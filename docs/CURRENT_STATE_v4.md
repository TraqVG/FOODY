# Foody — Current Implementation v4 (2026-11-13)
Status is based on code inspection and verified HTTP tests. Android build status is recorded separately.

## Implemented in source
- Kotlin + Jetpack Compose + Material 3; one APK, three roles.
- MVVM: FoodyStore (AndroidViewModel) -> FoodyRepository -> Retrofit/FastAPI; Room snapshot cache.
- FastAPI + SQLite server with normalized users, tokens, tables, dining sessions, foods, orders, order lines, payments, events.
- Server validates role and food availability; server calculates prices, saves immutable item/price snapshots.
- Order lifecycle: PENDING -> READY -> SERVED -> CHECKED_OUT.
- 6 tables, menu catalog, guest count, staff cart/add-on orders, kitchen queue, owner reports and menu CRUD (soft delete).
- Notification event feed; Android NotificationManager channel and Android 13 permission; foreground 4-second polling.
- WorkManager 15-minute periodic fallback (not instant push).
- Room stores most recently authenticated ServerSnapshot for read-only offline cache.
- Backend TestClient 10 tests passed on development runtime.
- Windows running backend /v1/health responded ok.
- Live Windows HTTP three-role workflow passed: Staff -> Kitchen -> Served -> Checkout -> Owner; amount 138 THB.

## Still unverified or pending
- Native Android Gradle Build / APK / AVD UI verification depends on Android SDK installation.
- Android notification appearance / POST_NOTIFICATIONS permission not verified on Emulator.
- Two separate AVDs and LDPlayer login/use simultaneously have not been tested in the native Android UI.
- Physical device and real payment are out of scope.
- Network security: current localhost development uses HTTP and demo credentials; for production require HTTPS, strong password provisioning, secure token storage, rate limits, backups and hardened deployment.
- Backend must be started separately after Windows restarts.

## Paths
- Android: C:\GitHub\Smart-Restaurant-Ordering\android
- Server: C:\GitHub\Smart-Restaurant-Ordering\backend
- UI reference: C:\GitHub\Smart-Restaurant-Ordering\prototype
- Former local-only FoodyStore retained in docs/FoodyStore_Legacy_LocalOnly.kt.txt

## Actual database responsibility
- Server SQLite is the authoritative DB for shared business data.
- Android Room is only cached display data; offline edits are not queued or transmitted until online.
- The HTML prototype remains separate; it does not use the FastAPI backend.
