# Foody — Current Documentation v3.0
Checked against Android code on 2026-11-13.

## Where the full files are
Updated DOCX and HTML diagrams are delivered in ChatGPT as:
- Foody_Project_Spec_v3_Current.docx
- Foody_Architecture_v3_Current.html
- Foody_Use_Case_v3_Current.html
- Foody_ER_Diagram_v3_Current.html
- Foody_Documents_v3_Current.zip

## Current code
C:\GitHub\Smart-Restaurant-Ordering\android
C:\GitHub\Smart-Restaurant-Ordering\prototype

- Native Android: Kotlin + Jetpack Compose + Material 3 + AndroidViewModel + Room.
- Data: one local Room entity SavedState / foody_state(id, json).
- 6 tables; 8 seeded food items; one restaurant; 3 demo login accounts.
- Staff: tables -> guest count -> menu -> cart -> send -> mark served -> simulated checkout; history.
- Kitchen: PENDING queue ordered by time; single Done button -> READY; no price.
- Owner: reports first; menu add, edit, delete, availability toggle.
- Statuses IN CODE: PENDING -> READY -> SERVED -> CHECKED_OUT.
- No native backend sync, FastAPI, Retrofit, server auth, notification delivery, WorkManager, explicit dining sessions or payments.
- Native Gradle Build, AVD run, and actual AVD screenshots NOT yet verified.

## Important gaps
- BR12: canCheckout() currently allows READY or SERVED, but target requires all orders served.
- BR16: Delete is hard delete on Food menu; add soft-delete or hide when referenced.
- No concurrency / idempotency / cancellation / audit trail / multiple emulator sync.
- Role guard is inside ViewModel; not a server authorization system.
- Staff history and Owner sales report are data from a single local Room instance.
- Existing HTML screenshots are Browser Preview, not native AVD screenshots.

## Source of truth
Use document v3 for describing the implementation.
Use the planned-target sections (FR/BR/Architecture) for remaining assignment scope.
Do not report planned components as completed until tested on the Android emulator.
