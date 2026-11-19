# Foody

Native Android order management for a single table-service restaurant.

- Android Studio: android/ (Kotlin + Jetpack Compose + Room + Retrofit + WorkManager)
- REST Backend: backend/ (FastAPI + SQLite, shared authoritative data)
- UI Prototype: prototype/ (HTML reference only)
- Current status: docs/CURRENT_STATE_v4.md
- Test checklist: docs/TEST_PLAN_v4.md

## Run backend
Run in PowerShell:
cd C:\GitHub\Smart-Restaurant-Ordering\backend
python -m uvicorn main:app --host 127.0.0.1 --port 8000
Health: http://127.0.0.1:8000/v1/health

## Run Android
Open C:\GitHub\Smart-Restaurant-Ordering\android in Android Studio.
Install SDK Platform API 36 and Android 14 API 34 emulator. Sync, build, run on AVD.
AVD uses 10.0.2.2:8000 to access the Windows host.

Local demo accounts (no registration):
staff@foody.shop / kitchen@foody.shop / owner@foody.shop
Password: foody123

Development only: fixed demo credentials and HTTP are not suitable for public deployment.
