# Foody REST Backend — classroom development

Backend authority for tables, prices, order status, simulated checkout and permissions.
It uses SQLite with BEGIN IMMEDIATE transactions and PBKDF2-hashed passwords.

## Windows local setup

```powershell
cd C:\GitHub\Smart-Restaurant-Ordering\backend
py -3 -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000
```

Check `http://127.0.0.1:8000/v1/health` from the Windows machine.
AVD connects to `http://10.0.2.2:8000/`. Android uses this build-configured URL.
For LDPlayer, use the host's reachable LAN IP in the debug Android Gradle configuration.

## Demo accounts

- staff@foody.shop (Staff)
- kitchen@foody.shop (Kitchen)
- owner@foody.shop (Owner)
- password `foody123` for local demo only, configurable with FOODY_DEMO_PASSWORD **before the first DB initialization**.

No signup API exists. Use HTTPS, secret management, stronger auth and rate limiting before deployment outside a trusted classroom environment.

## Test

```powershell
.venv\Scripts\python.exe -m pytest -q
```

Android and backend run on separate runtimes; the server must be launched separately.
