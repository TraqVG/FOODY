FOODY — ภาพตรวจหน้าจอ Browser Preview

ภาพทั้งหมดจับจากไฟล์ Foody_Interactive_UI_v4.html โดยใช้ Chromium ที่ขนาดหน้าจอ 390 x 844 CSS pixels (pixel ratio 2)

ข้อจำกัดสำคัญ: ภาพเหล่านี้ไม่ใช่ภาพจาก Android Studio Emulator (AVD) และไม่ใช่การยืนยันว่า Android Kotlin/Compose Build หรือ Run ผ่าน
ไม่มีการเปิด Android Studio หรือจับหน้าจอจากเครื่อง Windows ของผู้ใช้โดยตรง

โฟลเดอร์:
00_Common: Login
01_Staff: โต๊ะ, เปิดโต๊ะ, เลือกเมนู, ตะกร้า, รายละเอียดโต๊ะ, เช็กบิล, ประวัติ, ตั้งค่า
02_Kitchen: คิวครัวและตั้งค่า
03_Owner: รายงาน, จัดการเมนู, เพิ่ม/แก้ไขเมนู, ตั้งค่า
04_Extra_HTML: หน้าเสริมที่มีเฉพาะ HTML ต้นแบบ (ยังไม่ตรงกับ Kotlin Native)

ภาพที่ต่อท้าย _scrolled เป็นส่วนเลื่อนลงของหน้าจอเดิม

ขั้นตอนที่ยังต้องทำสำหรับ Screenshot Android จริง:
1. เปิดโฟลเดอร์ C:\GitHub\Smart-Restaurant-Ordering\android ด้วย Android Studio
2. Gradle Sync และแก้ Build Error (ถ้ามี)
3. สร้าง Android Emulator (AVD) Android 14 และกด Run
4. ล็อกอินทั้ง 3 บทบาท และจับภาพบน AVD ทุกหน้า
5. แยก ZIP จาก Android Screenshots จริง
