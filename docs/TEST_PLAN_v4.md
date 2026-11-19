# Foody — Acceptance Test Matrix

## Prerequisites
1. Windows backend running: http://127.0.0.1:8000/v1/health -> ok true.
2. Android Studio installed, SDK Platform API 36, Android 14 AVD (API 34) created, Gradle Sync successful.
3. Build the Foody Android project and install on at least two emulators; both use BASE_URL http://10.0.2.2:8000/.
4. Test accounts (server-seeded): staff@foody.shop / kitchen@foody.shop / owner@foody.shop; demo password foody123.
5. Android 13+: allow notifications when prompted.

## Automated backend (Python)
Command: cd backend; python -m pytest -q
Tests cover login, role isolation, six tables, duplicate opens, duplicate order submission, concurrent clients, price snapshot, sold out, soft delete, queue status transition, checkout guards, reports, notifications events, token revocation.

## On AVD 1 (staff) / AVD 2 (kitchen)
- Login invalid -> error; no signup.
- Login staff -> landing screen Tables; 6 clickable cards; no dashboard.
- Select vacant table -> guest default 1 -> change count -> Open Table.
- Pick menu with photo, category, price -> cart qty / note -> send once -> table shows PENDING.
- AVD 2 kitchen -> same new order appears with table, dish names and quantities, no prices.
- Kitchen taps 'เสร็จแล้ว' once -> order READY; extra tap blocked.
- Staff receives Android notification in app foreground (permission required), table shows 'พร้อมเสิร์ฟ'.
- Staff tries checkout before served -> blocked by server.
- Staff marks served -> checkout with cash simulated -> table becomes vacant.
- Owner login on another emulator or reuse second -> report daily/monthly/yearly totals update.
- Owner pauses menu -> staff cannot order it; owner edits menu price -> historical order retains old price.
- Owner soft-deletes menu -> old order still retains item snapshot.
- Turn network off -> last cached snapshot visible as read-only; writes show connection error.
- App background >15 min -> WorkManager may check, not a guaranteed instant alert.
- Confirm AVD and LDPlayer use suitable different host addresses; LDPlayer must point to Windows LAN IP.

## Known test limitations
- Backend HTTP flow was tested on Windows; UI multi-emulator and APK build must not be declared passed until executed on AVD.
- No real payment, no real camera/QR, no cloud deployment.
