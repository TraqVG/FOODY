"""Foody classroom backend: one restaurant, server-owned prices, three roles.

Run: python -m uvicorn main:app --host 0.0.0.0 --port 8000
The bundled accounts and HTTP transport are for local classroom development ONLY.
"""
from __future__ import annotations

import contextlib
import hashlib
import hmac
import os
import secrets
import sqlite3
import time
from pathlib import Path
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException, status
from pydantic import BaseModel, ConfigDict, Field

MILLIS_IN_DAY = 24 * 60 * 60 * 1000


def now_ms() -> int:
    return int(time.time() * 1000)


def hashed_password(password: str, salt: bytes) -> bytes:
    return hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 250_000)


class Database:
    def __init__(self, db_path: str):
        self.path = db_path
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)

    @contextlib.contextmanager
    def connect(self):
        con = sqlite3.connect(self.path, timeout=15.0)
        con.row_factory = sqlite3.Row
        con.execute("PRAGMA foreign_keys=ON")
        con.execute("PRAGMA busy_timeout=15000")
        try:
            yield con
        finally:
            con.close()

    @contextlib.contextmanager
    def write(self):
        with self.connect() as con:
            con.execute("BEGIN IMMEDIATE")
            try:
                yield con
                con.commit()
            except Exception:
                con.rollback()
                raise

    def initialize(self):
        with self.write() as con:
            con.executescript("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY, email TEXT UNIQUE NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('STAFF','KITCHEN','OWNER')),
                salt BLOB NOT NULL, password_hash BLOB NOT NULL
            );
            CREATE TABLE IF NOT EXISTS tokens (
                token_hash TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id),
                expires_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS restaurant_tables (id INTEGER PRIMARY KEY);
            CREATE TABLE IF NOT EXISTS foods (
                id INTEGER PRIMARY KEY, name TEXT NOT NULL, price INTEGER NOT NULL CHECK(price>0),
                category TEXT NOT NULL, image INTEGER NOT NULL DEFAULT 8,
                available INTEGER NOT NULL DEFAULT 1, deleted INTEGER NOT NULL DEFAULT 0
            );
            CREATE TABLE IF NOT EXISTS dining_sessions (
                id INTEGER PRIMARY KEY, table_id INTEGER NOT NULL REFERENCES restaurant_tables(id),
                guests INTEGER NOT NULL CHECK(guests BETWEEN 1 AND 20),
                opened_by INTEGER NOT NULL REFERENCES users(id), opened_at INTEGER NOT NULL,
                closed_at INTEGER
            );
            CREATE UNIQUE INDEX IF NOT EXISTS one_active_session_per_table
                ON dining_sessions(table_id) WHERE closed_at IS NULL;
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY, session_id INTEGER NOT NULL REFERENCES dining_sessions(id),
                staff_id INTEGER NOT NULL REFERENCES users(id), created_at INTEGER NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('PENDING','READY','SERVED','CHECKED_OUT','CANCELLED')),
                client_key TEXT NOT NULL, UNIQUE(staff_id,client_key)
            );
            CREATE TABLE IF NOT EXISTS order_lines (
                id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL REFERENCES orders(id),
                food_id INTEGER NOT NULL REFERENCES foods(id),
                name_snapshot TEXT NOT NULL, price_snapshot INTEGER NOT NULL,
                qty INTEGER NOT NULL CHECK(qty BETWEEN 1 AND 99), note TEXT NOT NULL DEFAULT ''
            );
            CREATE TABLE IF NOT EXISTS payments (
                id INTEGER PRIMARY KEY, session_id INTEGER UNIQUE NOT NULL REFERENCES dining_sessions(id),
                amount INTEGER NOT NULL CHECK(amount>0),
                method TEXT NOT NULL CHECK(method='CASH_SIMULATED'),
                paid_at INTEGER NOT NULL, staff_id INTEGER NOT NULL REFERENCES users(id)
            );
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY, kind TEXT NOT NULL, target_role TEXT NOT NULL,
                target_email TEXT, table_id INTEGER NOT NULL, order_id INTEGER,
                created_at INTEGER NOT NULL
            );
            """)
            count = con.execute("SELECT COUNT(*) FROM users").fetchone()[0]
            if count == 0:
                demo_password = os.environ.get("FOODY_DEMO_PASSWORD", "foody123")
                for email, role in (
                    ("staff@foody.shop", "STAFF"),
                    ("kitchen@foody.shop", "KITCHEN"),
                    ("owner@foody.shop", "OWNER"),
                ):
                    salt = os.urandom(16)
                    con.execute(
                        "INSERT INTO users(email,role,salt,password_hash) VALUES(?,?,?,?)",
                        (email, role, salt, hashed_password(demo_password, salt)),
                    )
            for number in range(1, 7):
                con.execute("INSERT OR IGNORE INTO restaurant_tables(id) VALUES(?)", (number,))
            if con.execute("SELECT COUNT(*) FROM foods").fetchone()[0] == 0:
                foods = (
                    ("ข้าวกะเพราหมูสับ", 69, "อาหารจานเดียว", 1),
                    ("ข้าวผัดกุ้ง", 89, "อาหารจานเดียว", 2),
                    ("ผัดซีอิ๊วหมู", 79, "อาหารจานเดียว", 3),
                    ("ชาไทย", 45, "เครื่องดื่ม", 4),
                    ("ต้มยำกุ้ง", 120, "กับข้าว", 5),
                    ("ผัดไทยกุ้ง", 85, "อาหารจานเดียว", 6),
                    ("ข้าวเหนียวมะม่วง", 60, "ของหวาน", 7),
                    ("น้ำเปล่า", 20, "เครื่องดื่ม", 8),
                )
                con.executemany(
                    "INSERT INTO foods(name,price,category,image) VALUES(?,?,?,?)", foods
                )


class LoginRequest(BaseModel):
    email: str
    password: str


class OpenTableRequest(BaseModel):
    guests: int = Field(ge=1, le=20)


class OrderLineRequest(BaseModel):
    foodId: int
    qty: int = Field(ge=1, le=99)
    note: str = Field(default="", max_length=160)


class NewOrderRequest(BaseModel):
    tableId: int
    lines: list[OrderLineRequest] = Field(min_length=1, max_length=50)
    clientKey: str = Field(min_length=8, max_length=100)


class FoodRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    price: int = Field(gt=0, le=1000000)
    category: str = Field(min_length=1, max_length=50)


class AvailabilityRequest(BaseModel):
    available: bool


class CheckoutRequest(BaseModel):
    method: str = Field(default="CASH_SIMULATED")


class User:
    def __init__(self, row: sqlite3.Row):
        self.id = row["id"]
        self.email = row["email"]
        self.role = row["role"]


def row_dict(row: sqlite3.Row) -> dict:
    return dict(row)


def create_app(db_path: str | None = None) -> FastAPI:
    app = FastAPI(title="Foody Restaurant API", version="1.0.0")
    db = Database(db_path or os.environ.get("FOODY_DB", "foody.sqlite3"))
    db.initialize()
    app.state.database = db

    def current_user(authorization: Annotated[str | None, Header()] = None) -> User:
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(401, "Missing bearer token")
        token = authorization.removeprefix("Bearer ").strip()
        if not token:
            raise HTTPException(401, "Missing bearer token")
        with db.connect() as con:
            user = con.execute(
                """SELECT users.id,users.email,users.role FROM tokens
                   JOIN users ON tokens.user_id=users.id
                   WHERE token_hash=? AND expires_at>?""",
                (hashlib.sha256(token.encode()).hexdigest(), now_ms()),
            ).fetchone()
        if not user:
            raise HTTPException(401, "Invalid or expired session")
        return User(user)

    def require(role: str, user: User):
        if user.role != role:
            raise HTTPException(403, "Insufficient role permission")

    def event(con, kind: str, role: str, table: int, order: int | None = None, target: str | None = None):
        con.execute(
            "INSERT INTO events(kind,target_role,target_email,table_id,order_id,created_at) VALUES(?,?,?,?,?,?)",
            (kind, role, target, table, order, now_ms()),
        )

    def active_session(con, table_id: int):
        return con.execute(
            "SELECT * FROM dining_sessions WHERE table_id=? AND closed_at IS NULL", (table_id,)
        ).fetchone()

    @app.get("/v1/health")
    def health():
        return {"ok": True, "service": "Foody", "serverTime": now_ms()}

    @app.post("/v1/auth/login")
    def login(req: LoginRequest):
        with db.write() as con:
            user = con.execute("SELECT * FROM users WHERE email=?", (req.email.strip().lower(),)).fetchone()
            valid = bool(user and hmac.compare_digest(
                user["password_hash"], hashed_password(req.password, user["salt"])
            ))
            if not valid:
                raise HTTPException(401, "Incorrect login")
            token = secrets.token_urlsafe(32)
            con.execute(
                "INSERT INTO tokens(token_hash,user_id,expires_at) VALUES(?,?,?)",
                (hashlib.sha256(token.encode()).hexdigest(), user["id"], now_ms()+7*MILLIS_IN_DAY),
            )
        return {"token": token, "email": user["email"], "role": user["role"]}

    @app.post("/v1/auth/logout")
    def logout(user: User = Depends(current_user), authorization: str = Header()):
        token = authorization.removeprefix("Bearer ").strip()
        with db.write() as con:
            con.execute("DELETE FROM tokens WHERE token_hash=? AND user_id=?",
                        (hashlib.sha256(token.encode()).hexdigest(), user.id))
        return {"ok": True}

    @app.get("/v1/me")
    def me(user: User = Depends(current_user)):
        return {"email": user.email, "role": user.role}

    @app.get("/v1/snapshot")
    def snapshot(user: User = Depends(current_user)):
        with db.connect() as con:
            tables = [
                {"id": r["id"], "guests": r["guests"]}
                for r in con.execute("""SELECT t.id, COALESCE(s.guests,0) guests
                      FROM restaurant_tables t LEFT JOIN dining_sessions s
                      ON s.table_id=t.id AND s.closed_at IS NULL ORDER BY t.id""")
            ]
            food_rows = con.execute(
                "SELECT id,name,price,category,image,available FROM foods WHERE deleted=0 ORDER BY id"
            ).fetchall()
            foods = [
                {"id": f["id"], "name": f["name"],
                 "price": f["price"] if user.role != "KITCHEN" else 0,
                 "category": f["category"], "image": f["image"],
                 "available": bool(f["available"])} for f in food_rows
            ]
            where = "WHERE o.status IN ('PENDING','READY')" if user.role == "KITCHEN" else ""
            orders = []
            for order in con.execute(f"""SELECT o.id,s.table_id,u.email AS staff_email,
                      o.created_at,o.status FROM orders o
                      JOIN dining_sessions s ON s.id=o.session_id
                      JOIN users u ON u.id=o.staff_id {where} ORDER BY o.created_at DESC"""):
                lines = con.execute("""SELECT food_id,name_snapshot,price_snapshot,qty,note
                    FROM order_lines WHERE order_id=? ORDER BY id""", (order["id"],)).fetchall()
                orders.append({
                    "id": order["id"], "tableId": order["table_id"],
                    "staffEmail": order["staff_email"], "time": order["created_at"],
                    "status": order["status"],
                    "lines": [{"foodId": l["food_id"], "name": l["name_snapshot"],
                               "price": l["price_snapshot"] if user.role != "KITCHEN" else 0,
                               "qty": l["qty"], "note": l["note"]} for l in lines]
                })
            sales = []
            if user.role == "OWNER" or user.role == "STAFF":
                for payment in con.execute("""SELECT p.id,s.table_id,p.paid_at,p.amount,s.id session_id
                        FROM payments p JOIN dining_sessions s ON s.id=p.session_id
                        ORDER BY p.paid_at DESC"""):
                    order_ids = [r[0] for r in con.execute(
                        "SELECT id FROM orders WHERE session_id=? ORDER BY id", (payment["session_id"],))]
                    sales.append({"id": payment["id"],"tableId": payment["table_id"],
                                  "time": payment["paid_at"],"amount": payment["amount"],
                                  "orderIds": order_ids})
            cursor = con.execute("SELECT COALESCE(MAX(id),0) FROM events").fetchone()[0]
        return {"tables": tables, "foods": foods, "orders": orders,
                "sales": sales, "latestEventId": cursor, "serverTime": now_ms()}

    @app.post("/v1/tables/{table_id}/open", status_code=201)
    def open_table(table_id: int, req: OpenTableRequest, user: User = Depends(current_user)):
        require("STAFF", user)
        with db.write() as con:
            if not con.execute("SELECT 1 FROM restaurant_tables WHERE id=?", (table_id,)).fetchone():
                raise HTTPException(404, "Table does not exist")
            if active_session(con, table_id):
                raise HTTPException(409, "Table already occupied")
            cur = con.execute(
                "INSERT INTO dining_sessions(table_id,guests,opened_by,opened_at) VALUES(?,?,?,?)",
                (table_id, req.guests, user.id, now_ms()),
            )
        return {"sessionId": cur.lastrowid, "tableId": table_id, "guests": req.guests}

    @app.post("/v1/orders", status_code=201)
    def create_order(req: NewOrderRequest, user: User = Depends(current_user)):
        require("STAFF", user)
        if len({line.foodId for line in req.lines}) != len(req.lines):
            raise HTTPException(422, "Duplicate menu items in one request")
        with db.write() as con:
            previous = con.execute("SELECT id FROM orders WHERE staff_id=? AND client_key=?",
                                   (user.id, req.clientKey)).fetchone()
            if previous:
                return {"orderId": previous["id"], "duplicate": True}
            session = active_session(con, req.tableId)
            if not session:
                raise HTTPException(409, "Table is not open")
            snapshots = []
            for line in req.lines:
                food = con.execute("SELECT * FROM foods WHERE id=? AND deleted=0 AND available=1",
                                   (line.foodId,)).fetchone()
                if not food:
                    raise HTTPException(409, "Menu item unavailable")
                snapshots.append((line.foodId, food["name"], food["price"], line.qty, line.note))
            cur = con.execute("""INSERT INTO orders(session_id,staff_id,created_at,status,client_key)
                VALUES(?,?,?,'PENDING',?)""", (session["id"], user.id, now_ms(), req.clientKey))
            for food_id, name, price, qty, note in snapshots:
                con.execute("""INSERT INTO order_lines(order_id,food_id,name_snapshot,price_snapshot,qty,note)
                    VALUES(?,?,?,?,?,?)""", (cur.lastrowid, food_id, name, price, qty, note))
            event(con, "ORDER_NEW", "KITCHEN", req.tableId, cur.lastrowid)
        return {"orderId": cur.lastrowid, "duplicate": False}

    @app.post("/v1/orders/{order_id}/ready")
    def ready(order_id: int, user: User = Depends(current_user)):
        require("KITCHEN", user)
        with db.write() as con:
            order = con.execute("""SELECT o.status,s.table_id,u.email staff_email
                FROM orders o JOIN dining_sessions s ON s.id=o.session_id
                JOIN users u ON u.id=o.staff_id WHERE o.id=?""", (order_id,)).fetchone()
            if not order:
                raise HTTPException(404, "Order not found")
            if order["status"] != "PENDING":
                raise HTTPException(409, "Order no longer pending")
            con.execute("UPDATE orders SET status='READY' WHERE id=?", (order_id,))
            event(con, "ORDER_READY", "STAFF", order["table_id"], order_id, order["staff_email"])
        return {"status": "READY"}

    @app.post("/v1/orders/{order_id}/served")
    def served(order_id: int, user: User = Depends(current_user)):
        require("STAFF", user)
        with db.write() as con:
            order = con.execute("SELECT status FROM orders WHERE id=?", (order_id,)).fetchone()
            if not order:
                raise HTTPException(404, "Order not found")
            if order["status"] != "READY":
                raise HTTPException(409, "Must be READY before served")
            con.execute("UPDATE orders SET status='SERVED' WHERE id=?", (order_id,))
        return {"status": "SERVED"}

    @app.post("/v1/tables/{table_id}/checkout")
    def checkout(table_id: int, req: CheckoutRequest, user: User = Depends(current_user)):
        require("STAFF", user)
        if req.method != "CASH_SIMULATED":
            raise HTTPException(422, "Only simulated cash is supported")
        with db.write() as con:
            session = active_session(con, table_id)
            if not session:
                raise HTTPException(409, "Table not open")
            order_rows = con.execute("SELECT id,status FROM orders WHERE session_id=?",(session["id"],)).fetchall()
            if not order_rows or any(row["status"] != "SERVED" for row in order_rows):
                raise HTTPException(409, "All orders must be SERVED before checkout")
            amount = con.execute("""SELECT SUM(l.price_snapshot * l.qty) total
                FROM order_lines l JOIN orders o ON o.id=l.order_id WHERE o.session_id=?""",
                (session["id"],)).fetchone()["total"]
            at = now_ms()
            p = con.execute("""INSERT INTO payments(session_id,amount,method,paid_at,staff_id)
                VALUES(?,?,?,?,?)""", (session["id"],amount,req.method,at,user.id))
            con.execute("UPDATE orders SET status='CHECKED_OUT' WHERE session_id=?", (session["id"],))
            con.execute("UPDATE dining_sessions SET closed_at=? WHERE id=?", (at,session["id"]))
            event(con, "SALE_COMPLETE", "OWNER", table_id)
        return {"paymentId": p.lastrowid, "tableId": table_id, "amount": amount}

    @app.post("/v1/foods", status_code=201)
    def create_food(req: FoodRequest, user: User = Depends(current_user)):
        require("OWNER", user)
        with db.write() as con:
            cur = con.execute("INSERT INTO foods(name,price,category,image) VALUES(?,?,?,8)",
                              (req.name.strip(),req.price,req.category.strip()))
        return {"id": cur.lastrowid}

    @app.put("/v1/foods/{food_id}")
    def update_food(food_id: int, req: FoodRequest, user: User = Depends(current_user)):
        require("OWNER", user)
        with db.write() as con:
            cur=con.execute("UPDATE foods SET name=?,price=?,category=? WHERE id=? AND deleted=0",
                            (req.name.strip(),req.price,req.category.strip(),food_id))
            if cur.rowcount==0: raise HTTPException(404,"Menu item missing")
        return {"ok": True}

    @app.patch("/v1/foods/{food_id}/availability")
    def set_availability(food_id: int, req: AvailabilityRequest, user: User = Depends(current_user)):
        require("OWNER", user)
        with db.write() as con:
            cur=con.execute("UPDATE foods SET available=? WHERE id=? AND deleted=0",
                            (int(req.available),food_id))
            if cur.rowcount==0: raise HTTPException(404,"Menu item missing")
        return {"ok": True}

    @app.delete("/v1/foods/{food_id}")
    def delete_food(food_id: int, user: User = Depends(current_user)):
        require("OWNER", user)
        with db.write() as con:
            cur=con.execute("UPDATE foods SET deleted=1,available=0 WHERE id=? AND deleted=0",(food_id,))
            if cur.rowcount==0: raise HTTPException(404,"Menu item missing")
        return {"ok": True}

    @app.get("/v1/events")
    def events(after: int = 0, user: User = Depends(current_user)):
        if after < 0:
            raise HTTPException(422, "after must be nonnegative")
        with db.connect() as con:
            rows=con.execute("""SELECT id,kind,table_id,order_id,created_at
                FROM events WHERE id>? AND target_role=?
                AND (target_email IS NULL OR target_email=?)
                ORDER BY id LIMIT 100""",(after,user.role,user.email)).fetchall()
            latest=con.execute("SELECT COALESCE(MAX(id),0) FROM events").fetchone()[0]
        return {"events":[{"id":r["id"],"kind":r["kind"],"tableId":r["table_id"],
                           "orderId":r["order_id"],"time":r["created_at"]} for r in rows],
                "latestEventId":latest}

    return app


app = create_app()
