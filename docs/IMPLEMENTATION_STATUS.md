# Foody reconstruction implementation status — 2026-11-14

This Git worktree was rebuilt using the original `prototype/index.html` (Foody v4) and accompanying Browser Preview screenshots, NOT by claiming to have obtained the source from offline Windows PC.

## Implemented in source
- Android Kotlin native Compose single-Activity role-based UI. No WebView.
- Staff: tables 6; guest count numeric keypad; menu grid with 8 original food images; cart, note, send; table detail, served, checkout; own order history.
- Kitchen: pending queue sorted by created time, item names with quantities, no prices, one Done button.
- Owner: initial sales dashboard (day/month/year + timeline), menu CRUD (soft delete), selling availability.
- Common: fixed-account login, settings/logout via top-right profile.
- API: Retrofit integration to existing FastAPI backend with server-owned prices, authorization, status changes and idempotency.
- Room: cached JSON snapshot for offline viewing, not offline order submission.
- Android notification: native system notification for server events while app is open (polling every 5 seconds, POST_NOTIFICATIONS permission).
- Backend FastAPI: previously tested code, 10 pytest tests passed in this reconstruction environment.

## Still not verified or not implemented
- Android Studio Gradle Sync / APK build / AVD screenshot verification NOT performed; Android SDK unavailable in current environment.
- Native UI not pixel-identical to HTML v4; designed to match structure, images, palette and workflow. Inspect under Pixel AVD.
- Browser preview screenshots are HTML, NOT actual Android screenshot.
- Background push notifications with app closed require FCM/other delivery. WorkManager is a dependency but not a real-time push solution.
- API authentication is a classroom demo on localhost; plaintext local debug HTTP is not acceptable for deployment.
- Multi-user Android/emulator E2E still requires manual verification; backend multi-account tests do not prove UI E2E.

## Source of truth
- `prototype/index.html` and `screenshots/` for appearance/UX reference.
- `android/` for current Android implementation.
- `backend/` for API and server-side behavior.
- `docs/Foody_Project_Spec_v3_Current.docx` is a historical spec; use this status when evaluating actual code coverage.
