# Build and Device Verification — Current Evidence

Date: 2026-11-13 (Thai local date shifted overnight during work).

## Verified
- Windows Python / FastAPI server installed and running on http://127.0.0.1:8000/v1/health.
- Health returns {"ok":true,"service":"Foody"}.
- Live Windows client logged in Staff, Kitchen, Owner and completed full order -> kitchen -> served -> checkout -> owner sales (138 THB).
- Local backend pytest run: 10/10 passed.
- Android sources exist, Retrofit+Repository+Room Cache+Notification+WorkManager integrated, test source exists.
- Gradle wrapper exists in android/gradlew.bat and gradle/wrapper/gradle-wrapper.jar.

## Not verified
- Android Studio executable not present when checked.
- Android SDK under %LOCALAPPDATA%\Android\Sdk not present when checked.
- winget install Google.AndroidStudio 2026.2.2.8 remained at downloading installer; no completion.
- Gradle 8.13 distribution download remained at downloading URL; no build result.
- Both stalled download processes were stopped deliberately.
- No APK, emulator screenshot, native UI test, or native notification delivery proof yet.
- Do not mark Kotlin compilation or multi-AVD UI tests as passed.

## When internet and Android Studio SDK are available
1. Install Android Studio (Google.AndroidStudio) and SDK Platform 36.
2. Install Android Emulator and create Android 14 API 34 Pixel AVD.
3. Open android/ in Android Studio; set Gradle JDK to the Embedded JDK.
4. Sync Gradle, build Debug APK; inspect exact compiler errors and fix if any.
5. Ensure Windows backend health is up on localhost:8000.
6. Launch AVD and test Staff, Kitchen, Owner as defined in TEST_PLAN_v4.md.
7. Allow Android 13+ POST_NOTIFICATIONS, verify notifications appear.
8. Test two devices against same Backend, then document actual screenshots/results.

## Security
- Demo accounts are seeded; fixed passwords and HTTP are for local classroom use only.
- Backend currently bound to 127.0.0.1, not LAN (safer for classroom development).
- LDPlayer may require changing host binding and API endpoint; use trusted LAN only.
