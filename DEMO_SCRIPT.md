# DEMO SCRIPT — Pixel Chaser Ransomware Simulation
# Cơ sở An toàn Thông tin — Bài tập lớn

Thời gian: **8–10 phút**
Đối tượng: Giảng viên + Hội đồng chấm điểm
Cần mở sẵn: 2 terminal + 1 browser + 1 cửa sổ game

---

## Chuẩn bị trước buổi demo (trước khi vào lớp)

### Bước 0 — Reset trạng thái sạch

Chạy file này tại thư mục gốc dự án:

```
reset-demo.bat
```

Nhập `YES` → xóa `sessions.db` + `Lab_sandbox\working\*`. Baseline giữ nguyên.

Kiểm tra baseline còn nguyên:

```
dir "Lab_sandbox\baseline"
```

Kết quả phải có: `config.json`, `document.txt`, `image.png`, `sample.csv`

---

### Terminal 1 — Demo Controller (Attacker C2)

```
cd build
java -cp "Launcher.jar;sqlite-jdbc.jar" ransomware.controller.DemoController
```

Chờ đến khi thấy:

```
==============================================
 PIXEL CHASER DEMO CONTROLLER  [ATTACKER]
==============================================
Listening : http://127.0.0.1:5000
```

### Terminal 2 — Recovery Backend + Portal

```
cd build
java -cp "Helper.jar;sqlite-jdbc.jar" ransomware.web.RecoveryBackend
```

Chờ đến khi thấy:

```
RECOVERY BACKEND listening :8080
Portal dir: ...Web_server\Recovery_Portal
```

### Browser

Mở `http://127.0.0.1:8080/`

Kiểm tra:
- Hiển thị **"No Active Session Selected"**
- Badge góc trên phải: **CONTROLLER ONLINE** (màu xanh lá)

---

## Luồng trình bày

---

### [0:00–0:30] Giới thiệu tổng quan

> "Chúng em xin trình bày bài tập lớn mô phỏng chuỗi tấn công ransomware trong môi trường sandbox. Toàn bộ hệ thống chạy localhost, chỉ thao tác trên thư mục `Lab_sandbox`, không ảnh hưởng tới dữ liệu thật."

Mô tả kiến trúc 5 tầng:
1. **Lure** — Email, Landing Page, Installer
2. **Game** — Pixel Chaser (Java Swing)
3. **Simulation Core** — Sandbox, Session, CryptoAdapter
4. **Orchestration** — Demo Controller (:5000)
5. **Recovery** — Portal + Backend (:8080)

---

### [0:30–1:30] PHASE 1 — Lure Delivery (Mồi nhử)

**Action:** Mở File Explorer → vào `Web_server\Email_Template\preview.html`

> "Chuỗi tấn công bắt đầu bằng một email mời thử game. Nạn nhân click CTA → dẫn tới landing page của studio giả."

**Action:** Mở `Web_server\Landing_Page\index.html` → cuộn xuống nút Download

> "Nút Download dẫn tới Installer. Đây là điểm khởi phát của toàn bộ sự cố."

---

### [1:30–2:00] PHASE 2 — Installer với JDK 26 (Nạn nhân tự nguyện)

> "Installer được thiết kế giống một ứng dụng game thông thường. Nạn nhân **tự nguyện** chạy nó."

Chỉ vào màn hình chọn components trong Installer:

> "Chú ý: phần **Java 26 Runtime Environment** được đánh dấu là **(required)** và không thể bỏ chọn. Nạn nhân được giải thích rằng game cần Java 26 để chạy — điều này hoàn toàn hợp lý và họ không nghi ngờ."

> "Đây là kỹ thuật **social engineering**: nạn nhân **tự tay cài phần mềm cần thiết để mình bị tấn công**. Họ là đồng lõa vô tình của chính sự cố của mình."

Sau khi Installer hoàn thành — game đã cài vào `%PROGRAMFILES64%\Pixel Chaser`.

---

### [2:00–2:30] PHASE 3 — Chơi game

**Action:** Chạy `run-game.bat` hoặc double-click shortcut `Pixel Chaser` trên Desktop.

```
cd build
java -jar PixelChaser.jar
```

> "Game Pixel Chaser là một game arcade Java bình thường. Nạn nhân chơi mà không thấy bất kỳ dấu hiệu bất thường nào."

Show menu chính, thử chơi vài giây.

---

### [2:30–3:00] PHASE 4 — Trigger (Kích hoạt)

**Action:** Đóng cửa sổ game (click ✕ hoặc Alt+F4).

> "Khi game bị đóng — **bất kể bằng cách nào**: nut X, Alt+F4, hoặc menu Exit — sự kiện `GAME_CLOSED` được gửi tới Demo Controller. Controller khởi động ngay lập tức quy trình mã hoá."

Sau ~1–2 giây: **cửa sổ "Wana Decrypt0r 2.0"** xuất hiện trên màn hình (không fullscreen).

---

### [3:00–4:00] PHASE 5 — Controller Log + LockScreen

**Action:** Switch sang Terminal 1.

Show log đang in ra:

```
[GAME_CLOSED] instanceId = <uuid>
  [1] Session: RCV-20260923-XXXXXX
  [2] Workspace prepared
  [3] Encrypted: 4 files (failed: 0)
  [4] Private key stored in DB (3168 b64 chars) — NOT on victim disk
  [5] Status: PAYMENT_PENDING
  [UI] LockScreen displayed — Portal: http://127.0.0.1:8080/
  [6] Countdown started (2160s)
```

> "Controller vừa tạo session mới với RecoveryID duy nhất. Copy 4 file từ baseline vào workspace riêng của session, mã hoá bằng AES-256-GCM + RSA-4096-OAEP. **Private key được lưu trong attacker DB và bị xoá khỏi máy nạn nhân ngay lập tức.**"

**Action:** Switch sang cửa sổ "Wana Decrypt0r 2.0".

Chỉ ra các thành phần:
- **Recovery ID** màu vàng
- **Countdown timer** đếm ngược (LED digital)
- **Payment Status: PENDING**
- **Recovery Portal URL** và nút `Copy Link`
- Ô **Enter Private Key (Base64)**
- Nút **[Open Demo Folder]** / **[Check Payment]** / **[Decrypt]** / **[Close Window]** (đang bị mờ - khóa)
- Phần FAQ bên phải giải thích "What Happened?", "Can I Recover?", "How Do I Pay?"

> **Điểm nhấn tương tác:** Thử click nút **✕** ở góc trên cửa sổ LockScreen. Cửa sổ sẽ chặn tắt và hiện cảnh báo: *"Cannot close: System is locked by ransomware"*. Nạn nhân không thể tắt ứng dụng để trốn tránh nếu chưa có Private Key.

---

### [4:00–4:30] PHASE 6 — Xem file bị mã hoá

**Action:** Click **[Open Demo Folder]** trong LockScreen.

File Explorer mở thư mục `Lab_sandbox\working\RCV-...\`

> "4 file ban đầu bình thường bây giờ đã có đuôi `.locked`. Dữ liệu bên trong đã được mã hoá — không thể đọc được."

**Action:** Trên cửa sổ LockScreen, click nút **[Open Link]** (hoặc `[Check Payment]`) ở mục Recovery Portal để mở trang giải mã cá nhân của nạn nhân: `http://127.0.0.1:8080/?rid=...`

> "Cửa sổ nạn nhân không tự ý bung lên tràn lan mà nạn nhân chủ động mở qua link được cấp trên LockScreen."

---

### [4:30–5:30] PHASE 7 — Phân chia vai trò 2 bên (Victim vs Attacker C2)

> "Hệ thống phân định ranh giới tuyệt đối giữa 2 vai trò: Nạn nhân bị kiểm soát hoàn toàn, Kẻ tấn công nắm toàn quyền C2."

**Trình chiếu 2 cửa sổ / 2 máy:**

1. **Phía Nạn nhân (Victim — Máy ảo / VM)**:
   - Truy cập: `http://<attacker-ip>:8080/?rid=...` (Giao diện `index.html`)
   - **Đặc điểm bị động**:
     - Chỉ có **Step 1 — Connect Your Session** và **Step 2 — Session & Payment Status**.
     - Ở Step 2, trạng thái hiển thị dạng **Hộp thông báo thụ động (Read-only)**: `⏳ Awaiting Attacker Payment Confirmation ($300 USD)`. Nạn nhân **không có nút bấm**, không thể can thiệp hay tự xác nhận.
     - Hoàn toàn **không thấy** Live Sandbox Monitor, không thấy C2 Database, không thấy System Info.

2. **Phía Kẻ tấn công (Attacker C2 — Máy thật / Host)**:
   - Truy cập: `http://127.0.0.1:8080/c2.html`
   - **Đặc điểm kiểm soát C2 toàn diện**:
     - Có nút điều khiển **`[CONFIRM DEMO PAYMENT ($300) — RELEASE KEY]`**.
     - Theo dõi **Live Sandbox Directory Monitor**: Nhìn thấy từ xa các file của nạn nhân bị đổi thành `.locked`.
     - Soi **Attacker C2 Database Live Inspector (sessions.db)** với 3 tab: `sessions`, `recovery_keys`, `file_manifest`.

**Tại tab `recovery_keys` trên trang Attacker C2:**
- Hiển thị Recovery ID, Key ID, Status: `AVAILABLE`, Key Length: `3168 chars`.
- **Tính năng mới — 👁️ View Key**: Attacker click nút `👁️ View Key` → **Modal Overlay** hiện lên hiển thị toàn bộ chuỗi khóa bí mật RSA-4096 Base64 đầy đủ, chứng minh khóa được lưu trữ tập trung tại C2 server chứ không để ở máy nạn nhân.

---

### [5:30–6:30] PHASE 8 — Attacker duyệt tiền & Giải phóng khóa

**Action:** Trên màn hình Attacker C2 (`c2.html`), Attacker click nút:
`[CONFIRM DEMO PAYMENT ($300) — RELEASE KEY]`

Quan sát diễn biến thời gian thực:
- **Phía Attacker C2**: Nút chuyển sang `PAYMENT CONFIRMED — KEY RELEASED`. Database Inspector cập nhật Status `CONFIRMED`.
- **Phía Nạn nhân (Victim Portal)**: 
  - Hộp thông báo thụ động lập tức đổi sang màu xanh: `✔ Payment Verified by Attacker — Private Key Dispatched`.
  - Khung **🔑 PRIVATE KEY RELEASED** xuất hiện chứa khóa giải mã RSA-4096 gửi từ server về.
  - Trên cửa sổ **Wana Decrypt0r 2.0**: `Payment Status` tự động đổi sang `CONFIRMED`.

> "Nạn nhân hoàn toàn bị động. Chỉ khi kẻ tấn công duyệt thanh toán trên C2 Server, API mới mở cổng giải phóng Private Key về máy nạn nhân."

---

### [6:30–7:30] PHASE 9 — Giải mã (Victim nhập key)

**Action:** Trên Recovery Portal → click **📋 Copy Private Key**.

> "Victim copy private key từ portal."

**Action:** Switch sang cửa sổ LockScreen.

Sequence thao tác:
1. Click **[Paste]** (hoặc Ctrl+V) vào ô *Enter Private Key (Base64)*
2. Thấy chuỗi Base64 dài điền vào ô.
   - Thanh trạng thái hiển thị: *"Private Key entered. Click [Decrypt] to verify key and restore files."*
   - Cửa sổ vẫn tiếp tục khóa bảo vệ, nút **[Close Window]** vẫn ở trạng thái vô hiệu hóa (disabled) để đảm bảo nạn nhân phải bấm giải mã thành công mới được phép thoát.
3. Click nút đỏ **[Decrypt]**

> "Private key được truyền trực tiếp vào thuật toán giải mã. Hệ thống dùng đúng `CryptoUtils.decryptFile()` — cùng hàm của nhóm Crypto viết, không thêm gì."

Quan sát:
- Label trạng thái: *"Attempting decryption of Lab_sandbox files..."*
- Sau ~1–2 giây: dialog **✔ DECRYPTION SUCCESSFUL!** — `4 files restored`
- Countdown đổi sang **RECOVERED** (màu xanh)
- Payment Status đổi sang **COMPLETED**
- **Mở khóa đóng ứng dụng**: Lúc này `isDecrypted` đã đạt `true`, nút **[Close Window]** được kích hoạt (sáng lên), và nút **✕** trên thanh tiêu đề cho phép đóng cửa sổ an toàn.

**Action:** Click **[Open Demo Folder]** lần nữa. File Explorer hiển thị 4 file — **không còn đuôi `.locked`**.

**Action:** Switch sang Browser → **Live Sandbox Monitor**:
- Tất cả file badge **PLAINTEXT (NORMAL)** (icon 📄)

**Action:** Switch sang tab `file_manifest` trong DB Inspector:
- 4 dòng Status: `RESTORED`

**Action:** Switch sang tab `recovery_keys`:
- Status: `USED` — key đã bị đánh dấu dùng một lần

> "SHA-256 của file sau giải mã khớp với SHA-256 trước khi mã hoá — tính toàn vẹn được đảm bảo."

---

### [7:30–8:00] PHASE 10 — Baseline vs Workspace

**Action:** Switch sang terminal, chạy:

```
dir "Lab_sandbox\baseline"
dir "Lab_sandbox\working" /s /b
```

> "Điểm cốt lõi: **baseline hoàn toàn không bị chạm vào**. Chỉ có workspace của session bị mã hoá và giải mã. `SandboxGuard` reject toàn bộ path nằm ngoài `Lab_sandbox`. Không có file thật nào bị ảnh hưởng."

---

### [8:00–8:30] Tóm tắt kiến trúc + Q&A Prep

**Action:** Chỉ vào ARCHITECTURE.md hoặc show sơ đồ.

**Điểm mạnh kỹ thuật:**
- AES-256-GCM mã hoá từng file riêng biệt (per-file unique AES key)
- RSA-4096-OAEP bọc AES key (hybrid encryption đúng chuẩn)
- Private key **không bao giờ tồn tại trên máy nạn nhân** sau quá trình mã hoá
- SHA-256 integrity check sau khi giải mã
- `SandboxGuard` — sandbox isolation tuyệt đối

---

## Câu hỏi dự kiến

**Q: Tại sao dùng Java 26?**
> Nhóm Crypto thiết kế thuật toán trên Java 26. Tuy nhiên code tương thích ngược với JDK 21+. Installer của chúng em có section JRE là bắt buộc — đây chính là một phần của kỹ thuật luring.

**Q: Private key lưu như thế nào?**
> RSA-4096 private key → serialize PKCS#8 DER bytes → Base64 encode → lưu vào bảng `recovery_keys` trong `sessions.db`. File `_private.key` bị xóa ngay sau khi mã hoá xong.

**Q: Làm sao đảm bảo sandbox an toàn?**
> 3 lớp bảo vệ: (1) `SandboxGuard` reject mọi path ngoài `Lab_sandbox`; (2) Baseline immutable, không bao giờ bị sửa; (3) `Config.TARGET_DIR` hardcode vào sandbox root.

**Q: Có test tự động không?**
> Có. 4 test suites: `SandboxTest`, `CryptoAdapterTest`, `RecoverySystemTest`, `ControllerSmokeTest`. Tổng >30 test cases.

**Q: Crypto do ai viết?**
> `CryptoUtils.java`, `Config.java`, `Ransomware.java`, `Decryption.java` là code của nhóm Crypto — **không sửa**. Nhóm em xây `CryptoAdapter` wrap lại, tích hợp vào session lifecycle.

**Q: Nếu chạy trên máy thật thì sao?**
> Không ảnh hưởng. Paths hardcode vào `Lab_sandbox`, `SandboxGuard` reject mọi thao tác ngoài sandbox.

---

## Reset sau demo (nếu cần demo lần 2)

```
reset-demo.bat
```

Nhập `YES`. Sau đó chạy lại Terminal 1, Terminal 2, rồi game.

---

> **Note kỹ thuật:** `reset-demo.bat` dùng `%~dp0` — tự resolve path tuyệt đối từ vị trí file .bat, không hardcode, hoạt động đúng với đường dẫn có ký tự tiếng Việt.
