# Foody Native Android — v4

Open this directory in Android Studio, not the prototype folder.
Requires Android SDK Platform 36; recommended testing AVD Android 14 API 34.
Gradle wrapper: gradlew.bat. Build: .\gradlew.bat :app:assembleDebug

## Project components
- MainActivity.kt: Compose navigation and notification permission.
- FoodyStore.kt: AndroidViewModel, business actions, UI state, foreground refresh (4 sec).
- FoodyRepository.kt: Retrofit + Room cache, token session, background sync scheduling.
- FoodyNetwork.kt: FastAPI Retrofit API contract.
- FoodySyncWorker.kt: periodic WorkManager fallback (minimum 15 min).
- FoodyNotifier.kt: native notification channel.
- FoodyRules.kt: shared checkout validation; unit tests at app/src/test.

## Backend
Windows: cd C:\GitHub\Smart-Restaurant-Ordering\backend
python -m uvicorn main:app --host 127.0.0.1 --port 8000
Health: http://127.0.0.1:8000/v1/health

Debug API URL set in app/build.gradle.kts: http://10.0.2.2:8000/
Android Emulator connects to the host loopback using 10.0.2.2.
LDPlayer often needs host LAN IP and firewall configuration; change BASE_URL before build.

## Demo login (server-seeded)
staff@foody.shop, kitchen@foody.shop, owner@foody.shop
Password: foody123.

## Verified
- Backend Python integration tests: 10 passed (local test runtime).
- Windows backend HTTP health: ok.
- Windows HTTP cross-role smoke: passed, correct cash total 138.

## Unverified
Android Gradle build, actual Android notification, UI flow on AVD, native multi-emulator testing.
Room caches snapshots but does not support offline order submission. WorkManager is not realtime.
For public deployment migrate debug HTTP to HTTPS and replace demo credentials.
