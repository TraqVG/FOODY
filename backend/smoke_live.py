"""Cross-role end-to-end HTTP check against the running Windows FastAPI server."""
import uuid
import httpx

BASE = "http://127.0.0.1:8000/v1"
with httpx.Client(base_url=BASE, timeout=8) as client:
    tokens = {}
    for name in ("staff", "kitchen", "owner"):
        response = client.post(
            "/auth/login",
            json={"email": f"{name}@foody.shop", "password": "foody123"},
        )
        response.raise_for_status()
        tokens[name] = {"Authorization": "Bearer " + response.json()["token"]}
    table = 6
    snap = client.get("/snapshot", headers=tokens["staff"])
    snap.raise_for_status()
    table_state = next(t for t in snap.json()["tables"] if t["id"] == table)
    if table_state["guests"] != 0:
        print("SKIPPED: table 6 occupied; backend health and login passed")
    else:
        opened = client.post(f"/tables/{table}/open", headers=tokens["staff"], json={"guests": 2})
        assert opened.status_code == 201, opened.text
        order = client.post("/orders", headers=tokens["staff"], json={
            "tableId": table, "clientKey": str(uuid.uuid4()),
            "lines": [{"foodId": 1, "qty": 2, "note": "ไม่เผ็ด"}],
        })
        assert order.status_code == 201, order.text
        oid = order.json()["orderId"]
        pending = client.get("/snapshot", headers=tokens["kitchen"]).json()
        assert any(o["id"] == oid for o in pending["orders"])
        assert client.post(f"/orders/{oid}/ready", headers=tokens["kitchen"]).status_code == 200
        assert client.post(f"/orders/{oid}/served", headers=tokens["staff"]).status_code == 200
        checkout = client.post(f"/tables/{table}/checkout", headers=tokens["staff"], json={"method": "CASH_SIMULATED"})
        assert checkout.status_code == 200, checkout.text
        assert checkout.json()["amount"] == 138
        report = client.get("/snapshot", headers=tokens["owner"]).json()
        assert any(s["tableId"] == table and s["amount"] == 138 for s in report["sales"])
        print("PASS: HTTP Staff -> Kitchen -> Served -> Checkout -> Owner, total=138")
print("LIVE WINDOWS BACKEND CHECK OK")
