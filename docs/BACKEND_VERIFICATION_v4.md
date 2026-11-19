# Foody Backend Verification

Local FastAPI TestClient + pytest: 10 tests passed.
- Role permissions and password validation
- Six tables, duplicate-open guard
- End-to-end three roles, checkout only after SERVED
- Server price source and history snapshots
- Sold-out/unavailable protection and soft menu delete
- Idempotent order submission
- Concurrent table open
- Role-targeted events
- Token revocation
- Concurrent duplicate order submission

Actual Windows FastAPI server:
- Location: C:\GitHub\Smart-Restaurant-Ordering\backend
- Health: http://127.0.0.1:8000/v1/health -> ok=true
- Live script: backend/smoke_live.py
- Result: Staff -> Kitchen -> Served -> Checkout -> Owner
- Actual total: 138 THB, PASS.
- Endpoint bind after security adjustment: 127.0.0.1:8000.

Run test again (with backend isolated fixture):
cd C:\GitHub\Smart-Restaurant-Ordering\backend
python -m pytest -q

Run smoke against running server:
python smoke_live.py
The live smoke test uses Table 6 and a real simulated sale; it skips if Table 6 is currently occupied.
