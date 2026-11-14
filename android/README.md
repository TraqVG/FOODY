# Android native Foody

Android Studio native UI based on the v4 screenshot set in `../screenshots`. The implementation uses Kotlin, Compose, Material3, MVVM (FoodyViewModel), repository (FoodyRepository), Retrofit to FastAPI, and a Room snapshot cache. It does not wrap the HTML in WebView.

- Open this directory in Android Studio.
- Gradle 8.13 / AGP 8.11.1 / Kotlin 2.2.21 / JDK 17, compileSdk 36, Android Emulator API 34.
- Start `../backend` on host port 8000. The debug API base URL is `http://10.0.2.2:8000/`.
- The API is server-authoritative. Cart totals are previews only; backend validates price on submission and billing.
- **Not yet compile/AVD verified** in this environment; use Android Studio Gradle sync/build and compare native screenshots to `../screenshots`.
- Foreground-polling Android notifications are implemented but background push while closed is not.
