"""Integration, RBAC, price snapshot, duplicate, and multi-client tests."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from main import create_app


@pytest.fixture
def context(tmp_path: Path):
    app = create_app(str(tmp_path / "test.sqlite3"))
    client = TestClient(app)
    def auth(role):
        accounts = {
            "STAFF": "staff@foody.shop",
            "KITCHEN": "kitchen@foody.shop",
            "OWNER": "owner@foody.shop",
        }
        res = client.post("/v1/auth/login", json={"email": accounts[role], "password": "foody123"})
        assert res.status_code == 200, res.text
        return {"Authorization": "Bearer " + res.json()["token"]}
    return client, {role: auth(role) for role in ("STAFF", "KITCHEN", "OWNER")}


def api(client, method, url, headers=None, json=None):
    return client.request(method, url, headers=headers, json=json)


def open_and_order(c, users, table=1, key="unique-request-key-100", qty=1):
    staff = users["STAFF"]
    opened = c.post(f"/v1/tables/{table}/open", headers=staff, json={"guests": 2})
    assert opened.status_code == 201, opened.text
    order = c.post("/v1/orders", headers=staff, json={
        "tableId": table, "clientKey": key,
        "lines": [{"foodId": 1, "qty": qty, "note": "ไม่เผ็ด"}],
    })
    assert order.status_code == 201, order.text
    return order.json()["orderId"]


def test_login_requires_credentials_and_role_authorization(context):
    c, u = context
    assert c.get("/v1/snapshot").status_code == 401
    assert c.post("/v1/auth/login", json={"email": "staff@foody.shop", "password": "wrong"}).status_code == 401
    assert c.post("/v1/tables/1/open", headers=u["OWNER"], json={"guests": 1}).status_code == 403
    assert c.post("/v1/foods", headers=u["KITCHEN"], json={"name": "ทดสอบ", "price": 1, "category": "อื่น"}).status_code == 403
    assert c.get("/v1/snapshot", headers=u["STAFF"]).status_code == 200


def test_tables_six_and_no_double_open(context):
    c, u = context
    assert len(c.get("/v1/snapshot", headers=u["STAFF"]).json()["tables"]) == 6
    assert c.post("/v1/tables/1/open", headers=u["STAFF"], json={"guests": 0}).status_code == 422
    assert c.post("/v1/tables/1/open", headers=u["STAFF"], json={"guests": 4}).status_code == 201
    assert c.post("/v1/tables/1/open", headers=u["STAFF"], json={"guests": 4}).status_code == 409


def test_end_to_end_multirole_data_sync_and_checkout(context):
    c,u=context
    oid = open_and_order(c,u,table=2,qty=2)
    kitchen = c.get("/v1/snapshot",headers=u["KITCHEN"]).json()
    assert any(order["id"]==oid and order["tableId"]==2 for order in kitchen["orders"])
    assert all(l["price"] == 0 for o in kitchen["orders"] for l in o["lines"])
    assert c.post("/v1/tables/2/checkout",headers=u["STAFF"],json={"method":"CASH_SIMULATED"}).status_code == 409
    assert c.post(f"/v1/orders/{oid}/ready",headers=u["STAFF"]).status_code == 403
    assert c.post(f"/v1/orders/{oid}/ready",headers=u["KITCHEN"]).status_code == 200
    assert c.post(f"/v1/orders/{oid}/ready",headers=u["KITCHEN"]).status_code == 409
    assert c.post("/v1/tables/2/checkout",headers=u["STAFF"],json={"method":"CASH_SIMULATED"}).status_code == 409
    assert c.post(f"/v1/orders/{oid}/served",headers=u["STAFF"]).status_code == 200
    payment=c.post("/v1/tables/2/checkout",headers=u["STAFF"],json={"method":"CASH_SIMULATED"})
    assert payment.status_code == 200, payment.text
    assert payment.json()["amount"] == 138
    assert c.post("/v1/tables/2/checkout",headers=u["STAFF"],json={"method":"CASH_SIMULATED"}).status_code == 409
    owner=c.get("/v1/snapshot",headers=u["OWNER"]).json()
    assert owner["sales"][0]["amount"]==138
    assert next(t for t in owner["tables"] if t["id"]==2)["guests"]==0
    assert next(o for o in owner["orders"] if o["id"]==oid)["status"]=="CHECKED_OUT"


def test_server_price_and_order_snapshot_preserved(context):
    c,u=context
    oid=open_and_order(c,u,table=3)
    assert c.put("/v1/foods/1",headers=u["OWNER"],json={"name":"กะเพราใหม่","price":999,"category":"อาหารจานเดียว"}).status_code==200
    old_order=next(o for o in c.get("/v1/snapshot",headers=u["STAFF"]).json()["orders"] if o["id"]==oid)
    assert old_order["lines"][0]["price"]==69
    assert old_order["lines"][0]["name"]=="ข้าวกะเพราหมูสับ"
    c.post(f"/v1/orders/{oid}/ready",headers=u["KITCHEN"])
    c.post(f"/v1/orders/{oid}/served",headers=u["STAFF"])
    paid=c.post("/v1/tables/3/checkout",headers=u["STAFF"],json={"method":"CASH_SIMULATED"})
    assert paid.json()["amount"]==69


def test_unavailable_food_and_deleted_food_not_orderable(context):
    c,u=context
    c.post("/v1/tables/4/open",headers=u["STAFF"],json={"guests":1})
    assert c.patch("/v1/foods/1/availability",headers=u["OWNER"],json={"available":False}).status_code==200
    blocked=c.post("/v1/orders",headers=u["STAFF"],json={"tableId":4,"clientKey":"unavailable-111","lines":[{"foodId":1,"qty":1}]})
    assert blocked.status_code==409
    assert c.delete("/v1/foods/2",headers=u["OWNER"]).status_code==200
    assert not any(x["id"]==2 for x in c.get("/v1/snapshot",headers=u["OWNER"]).json()["foods"])
    blocked=c.post("/v1/orders",headers=u["STAFF"],json={"tableId":4,"clientKey":"deleted-menu-111","lines":[{"foodId":2,"qty":1}]})
    assert blocked.status_code==409


def test_idempotency_prevents_double_order_on_retry(context):
    c,u=context
    oid=open_and_order(c,u,table=5,key="client-unique-key-555")
    repeated=c.post("/v1/orders",headers=u["STAFF"],json={"tableId":5,"clientKey":"client-unique-key-555","lines":[{"foodId":1,"qty":1}]})
    assert repeated.status_code==201
    assert repeated.json()=={"orderId":oid,"duplicate":True}
    assert len([o for o in c.get("/v1/snapshot",headers=u["STAFF"]).json()["orders"] if o["tableId"]==5])==1


def test_concurrent_clients_cannot_open_same_table_twice(context):
    c,u=context
    def try_open(_):
        with TestClient(c.app) as client:
            return client.post("/v1/tables/6/open",headers=u["STAFF"],json={"guests":2}).status_code
    with ThreadPoolExecutor(max_workers=2) as pool:
        codes=list(pool.map(try_open,range(2)))
    assert sorted(codes)==[201,409]


def test_events_reach_intended_roles_only(context):
    c,u=context
    oid=open_and_order(c,u,table=1,key="events-client-key1")
    kitchen=c.get("/v1/events",headers=u["KITCHEN"],params={"after":0}).json()["events"]
    assert any(e["kind"]=="ORDER_NEW" and e["orderId"]==oid for e in kitchen)
    assert not c.get("/v1/events",headers=u["OWNER"]).json()["events"]
    c.post(f"/v1/orders/{oid}/ready",headers=u["KITCHEN"])
    events=c.get("/v1/events",headers=u["STAFF"],params={"after":0}).json()["events"]
    assert any(e["kind"]=="ORDER_READY" and e["orderId"]==oid for e in events)
    assert not any(e["kind"]=="ORDER_NEW" for e in events)


def test_logout_invalidates_token(context):
    c,u=context
    assert c.post("/v1/auth/logout",headers=u["STAFF"]).status_code==200
    assert c.get("/v1/snapshot",headers=u["STAFF"]).status_code==401


def test_double_submit_concurrent_idempotent(context):
    c,u=context
    c.post("/v1/tables/1/open",headers=u["STAFF"],json={"guests":2})
    payload={"tableId":1,"clientKey":"same-key-across-threads","lines":[{"foodId":1,"qty":1}]}
    def issue(_):
        with TestClient(c.app) as client:
            return client.post("/v1/orders",headers=u["STAFF"],json=payload).json()
    with ThreadPoolExecutor(max_workers=2) as pool:
        results=list(pool.map(issue,range(2)))
    assert results[0]["orderId"]==results[1]["orderId"]
    assert {x["duplicate"] for x in results}=={True,False}
