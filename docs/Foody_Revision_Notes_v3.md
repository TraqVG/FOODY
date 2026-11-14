# Foody — Document Revision v3.0

ตรวจเทียบเอกสาร Foody v2 กับซอร์ส Android ใน C:\GitHub\Smart-Restaurant-Ordering\android ณ 13 พ.ย. 2569

## จุดที่แก้

- เปลี่ยนข้อมูลตัวอย่าง: 5 โต๊ะ → 6 โต๊ะ, เมนู 10–15 → 8 รายการเริ่มต้น
- ระบุว่า Kotlin/Compose/Room เป็นโค้ดที่มีอยู่ แต่ยังไม่มีผล Gradle Build และ AVD Screenshot จริง
- ไม่กล่าวว่าใช้ FastAPI, Retrofit, Server Auth, JWT, WorkManager, Notification, SQL tables แยกกันแล้ว
- สถานะในซอร์สปัจจุบัน: PENDING → READY → SERVED → CHECKED_OUT ไม่ใช่ Pending → Accepted → Preparing → Ready → Completed
- Kitchen ไม่มีปุ่มรับ/เริ่มทำ มีเพียงปุ่ม เสร็จแล้ว
- Owner มีรายงานเป็นหน้าแรกและจัดการเมนู; Staff ไม่มี Dashboard
- ระบุ Room ที่มีจริง: foody_state(id, json) และ Logical JSON arrays
- ระบุแยก Business Rule เป้าหมายและสถานะ Implement รายข้อ (BR01–BR17)
- ระบุแยก Functional Requirements และช่องว่าง (RO01–RO15)
- ระบุความต่างของ Payment/Dining Session (ยังไม่มี); ตรรกะ Checkout ที่ยอมรับ READY เป็นข้อบกพร่องต้องแก้
- ระบุว่า Owner ลบเมนูได้ซึ่งต่างจาก BR16 (เสนอ soft delete)
- ระบุว่าไม่มี Sync ข้ามเครื่องและบัญชี/Password เป็น Demo เท่านั้น

## ไฟล์
- Foody_Project_Spec_v3_Current.docx — รายละเอียดโครงงาน + FR + BR + สถานะตรงโค้ด
- Foody_Architecture_v3_Current.html — แผนภาพ Architecture มีโค้ด/แผน
- Foody_Use_Case_v3_Current.html — Use Cases ตามบทบาท
- Foody_ER_Diagram_v3_Current.html — Logical JSON Data Model + Physical Room

## หมายเหตุ
เอกสารฉบับนี้สะท้อนงานล่าสุด ไม่ได้ยืนยันว่าแอป Compile หรือทำงานจริงบน Android Emulator
