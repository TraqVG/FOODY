# Foody — Restaurant Order Management

**Kotlin/Jetpack Compose Android app** สำหรับพนักงานรับออเดอร์ ร้านอาหารตามสั่ง 6 โต๊ะ เชื่อม **FastAPI + SQLite** และ Room สำหรับเก็บ Snapshot Cache โดยใช้ **UI Prototype v4 และ Screenshots ของเดิม** เป็น visual reference.

> **Status:** Source implementation + tested FastAPI. Android Gradle Sync, APK build and AVD screenshot have **not** been verified yet. โปรดอย่าอ้างว่า Android build ผ่านจนกว่าจะรันจริง.

## โครงสร้าง Repository

| Directory | Purpose |
|---|---|
| `android/` | Android Studio Project: Kotlin, Compose, Material 3, MVVM, Repository, Retrofit, Room, local Android notifications |
| `backend/` | FastAPI, SQLite, server-owned pricing / RBAC / dining sessions / idempotency / payments, pytest |
| `prototype/` | **Exact Foody v4 interactive HTML** and embedded food images; visual reference only |
| `screenshots/` | Browser Preview screenshots of all roles (NOT native Android screenshots) |
| `docs/` | Specification v3, Architecture, Use Case, ER/Data Model + review notes |

## Android UI flow (no irrelevant dashboards)

- **STAFF**: โต๊ะ 6 โต๊ะ → รายละเอียดโต๊ะว่าง → จำนวนลูกค้า (1–20, default 1) → เลือกเมนูพร้อมภาพและราคาใหญ่ → ตะกร้า/หมายเหตุ → ส่งออเดอร์เข้าครัว → ดูรายการในโต๊ะ → เสิร์ฟอาหารที่ครัวทำเสร็จ → เช็กบิลจำลอง; ประวัติออเดอร์ของบัญชีตนเอง.
- **KITCHEN**: คิวออเดอร์ค้างเรียงตามเวลาสั่ง, ชื่ออาหารขนาดใหญ่ **ไม่แสดงราคา**, ปุ่ม **เสร็จแล้ว** เพียงหนึ่งปุ่มต่อออเดอร์.
- **OWNER**: หน้าแรก **รายงาน** ยอดรายวัน/เดือน/ปี รายการขาย + จัดการเมนู (เพิ่ม/แก้ไข/หยุดขาย/ลบแบบ Soft Delete).
- **EVERY ROLE**: Login ของบัญชีร้านที่กำหนดไว้เท่านั้น, ไอคอนโปรไฟล์มุมขวาบน → ตั้งค่า / ออกจากระบบ. ไม่มีการสมัครสมาชิก.

## Run backend (Windows)

```powershell
cd backend
py -3 -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
.venv\Scripts\python.exe -m pytest -q
```

Local API: http://127.0.0.1:8000/v1/health

Demo accounts: `staff@foody.shop`, `kitchen@foody.shop`, `owner@foody.shop`. Password for each account `foody123` (**local demo only**). No registration endpoint.

## Run Android

1. Open `android/` folder in Android Studio (NOT `prototype/`).
2. Use **JDK 17**, Gradle 8.13, Android SDK Platform 36. Install Android Studio and let it sync the project.
3. Start backend on the host machine.
4. Create **Android 14 API 34** emulator in Android Studio and press Run.
5. The Android Emulator accesses host backend via `http://10.0.2.2:8000/`, configured in `android/app/build.gradle.kts`. This HTTP exemption is only present in the Android **debug** manifest.
6. Switch user through Settings → Logout → Login to see role-specific screens. To test simultaneous access, run multiple emulators logged into different accounts against the same backend.

**Android Gradle Wrapper:** distribution properties are included. If `android/gradle/wrapper/gradle-wrapper.jar` is missing, generate it from a locally installed Gradle 8.13 with `gradle wrapper --gradle-version 8.13` or create/import the project from Android Studio. Do not download a wrapper JAR from an unknown source.

## Technology

- Android: Kotlin, Jetpack Compose, Material 3, ViewModel / Repository, Retrofit + Gson, Coroutines, Room 2.8.4 cache, Android Notifications (foreground polling).
- Backend: Python FastAPI, SQLite, Pydantic, PBKDF2 password hashing, Bearer token, server RBAC, unique open session/table, server-snapshot order prices, transaction + idempotency.
- UI visual reference: `prototype/index.html` / `screenshots`. Native implementation may vary in pixel details until compared against Android Emulator screenshots.

## Still to complete before submission

- **Build native Android in Android Studio**, fix any Gradle/compile errors, run AVD and compare screenshots to v4.
- Validate multi-emulator end-to-end manual workflow. Backend pytest tests alone do not prove Android multi-user operation.
- Notification events are polled while app is in foreground. To receive messages while app is closed, add FCM/background delivery. WorkManager is declared but not relied upon for real-time delivery.
- Add robust retry/loading/error states and tests for Android repository/ViewModel/Compose, plus stronger production auth/HTTPS before deployment.
- Review course rubric for any mandatory details not covered by this reconstruction.
