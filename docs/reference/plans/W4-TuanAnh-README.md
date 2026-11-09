# W4 — Tuấn Anh: Rà soát UC001/UC002, kiểm thử, review chéo

> **Branch:** `feature/anh-w4` (tách từ `develop` @ `6eb3bea`) · **Wave:** W4 (12/11–14/11)
> **Căn cứ:** [Phân công §6](work-assignment-4-members.md) · [guide-tuan-anh.md §4](guide-tuan-anh.md) · [UC001](../use-cases/UC001-login.md) · [UC002](../use-cases/UC002-logout.md) · XBR-13, EX-01, EX-02

## 1. Kết quả

| Hạng mục W4 | Trạng thái |
|---|---|
| `mvn clean test` | **Xanh: 613 test, 0 lỗi, 0 bỏ qua** (PostgreSQL 18, Flyway V1→V13). Trước khi sửa: `develop` không chạy được migration (§2.1). |
| Rà soát UC001 Đăng nhập | Xong — sửa 2 lỗi (§2.2, §2.3) |
| Rà soát UC002 Đăng xuất | Xong — đạt, không cần sửa |
| Review chéo W3 | Xong UC006, UC015, UC016; UC017 (Tài) **chưa có code** để review |

## 2. Lỗi đã sửa trên nhánh này

### 2.1. 🔴 Trùng migration V7 — ứng dụng trên `develop` không khởi động được

- `develop` có hai tệp version 7: `V7__phieu_doi_chenh_lech.sql` (Đăng) và `V7__seed_sales_trend_demo.sql` (Quý). Flyway báo `Found more than one migration with version 7`; `Main` dừng khởi động (11 test DB lỗi).
- **Sửa:** đổi tệp của Quý thành `V13__seed_sales_trend_demo.sql` (seed độc lập, chạy sau chuỗi V7–V12 của Đăng).
- **Sửa thêm dữ liệu seed:** máy đã bán vẫn để `SAN_SANG_BAN` → tồn kho dư 8 máy (RB37) và UC003 có thể bán lại serial đã bán. Đổi theo quy ước V4: đã bán `DA_BAN`, trả lỗi `HANG_LOI`, trả nguyên vẹn `SAN_SANG_BAN`; bỏ câu `SELECT` tóm tắt cuối tệp.
- Test UC016 `dieuChinhQuyVeNhanVienLapHoaDonGoc` đổi kỳ thành 05/07–31/07 để không dính hóa đơn `HD-2026-200001` (04/07) của seed mới; ý nghĩa test giữ nguyên.

> ⚠️ **Ai đã chạy app với `V7__seed_sales_trend_demo` cũ (Quý):** CSDL phát triển ghi V7 khác tệp hiện tại → Flyway báo lỗi checksum. Xóa và tạo lại CSDL `dtaq_retail` rồi chạy lại app (seed dùng `ON CONFLICT DO NOTHING` nên không tự sửa được trạng thái serial đã chèn).

### 2.2. 🟠 UC001 7.a — mật khẩu dài hơn 72 byte báo sai thông báo

- BCrypt từ chối mật khẩu > 72 byte UTF-8 (≈ 24 ký tự có dấu) bằng `IllegalArgumentException`. Khi đăng nhập, lỗi này rơi vào nhánh "Không thể đăng nhập lúc này" (7.b) thay vì "Tên đăng nhập hoặc mật khẩu không đúng" (7.a). Ở UC013 và quên mật khẩu, tạo mật khẩu dài cũng ném lỗi.
- **Sửa tại gốc** `PasswordHasher`: thêm `quaDai(char[])`; `verify` trả `false` khi quá dài. `NhanVienController` báo "Mật khẩu quá dài" ở trường mật khẩu; `PasswordResetController` trả `WEAK_PASSWORD`.
- Test: `PasswordHasherTest` (2), `AuthControllerTest.passwordOverBcryptLimitIsInvalidCredentials`, `NhanVienControllerTest` (thêm 1 assert).

### 2.3. 🟠 UC001 7.b — CSDL không truy cập được thì ứng dụng thoát im lặng

- Chạy thử với CSDL không tồn tại: chờ ~32 giây rồi thoát mã 0, **không hiện cửa sổ nào**.
- **Sửa** `Main`: hiện hộp thoại "Không thể đăng nhập lúc này. Vui lòng thử lại sau." kèm gợi ý kiểm tra PostgreSQL/`db.properties`, rồi thoát mã 1. Đã chạy lại (headless): log lỗi và thoát mã 1; hộp thoại chưa kiểm bằng mắt.

### 2.4. 🟠 Trang UC015 và UC016 chưa được đăng ký — menu chỉ hiện trang tạm

- Commit W3 của Quý chỉ thêm `import`; trang UC016 chờ Đăng đăng ký (README W3 §2) nhưng chưa làm.
- **Sửa** `AppNavigator.managerPages`: tạo `SalesByProductPage` và `SalesByEmployeePage`, xếp sau "Quản lý nhân viên" đúng thứ tự menu. Quản lý giờ có 13 trang thật, 0 trang tạm. Cập nhật `AppNavigatorPagesTest`.

### 2.5. Dọn tệp rác commit nhầm

Xóa `CheckReport.java`, `CheckReport.class`, `cp.txt` (classpath Windows) ở thư mục gốc.

## 3. Rà soát UC001/UC002

| Bước đặc tả | Hành vi | Vị trí | Kết quả |
|---|---|---|---|
| UC001 6 / 6.a | Trống (kể cả toàn khoảng trắng) → "Vui lòng nhập đầy đủ…", không truy vấn | `AuthController.login` | ✅ |
| 7 / 7.a | Sai tên hoặc mật khẩu → cùng một thông báo, không lộ tài khoản có tồn tại | `AuthController`, `LoginFrame` | ✅ (sau §2.2) |
| 7.b | Lỗi truy cập dữ liệu → "Không thể đăng nhập lúc này…" | `DATA_ERROR`, `Main` | ✅ (sau §2.3) |
| 8 / 8.a | Khóa → "Tài khoản đã bị khóa…"; chỉ kiểm khóa **sau** khi mật khẩu đúng | `AuthController` | ✅ |
| 9–11 | Trang theo `loaiQuyen`, trang mặc định theo vai trò | `AppNavigator.showMain` | ✅ |
| Mật khẩu | BCrypt cost 10, không lưu/ghi log mật khẩu thô; ô mật khẩu xóa sau khi đăng nhập | `PasswordHasher`, `LoginFrame` | ✅ |
| UC002 1–2 | Nút "Đăng xuất" → hộp xác nhận | `Topbar` | ✅ |
| 3 / 3.a | Xác nhận → `CurrentSession.clear()` → đóng shell (kèm dialog con) → màn hình đăng nhập; Hủy → giữ phiên | `AppNavigator.logout` | ✅ |

**Ghi nhận, không sửa (ngoài đặc tả):** không giới hạn số lần đăng nhập sai; tài khoản bị khóa giữa phiên vẫn dùng tiếp đến khi đăng xuất; tên không tồn tại phản hồi nhanh hơn (không chạy BCrypt). `ui/MainFrame.java` không còn ai dùng (đã thay bằng `AppShellFrame`) — có thể xóa.

## 4. Việc cần thành viên khác xử lý

| # | Ai | Vấn đề | Đề xuất |
|---|---|---|---|
| 1 | **Đăng** | 🔴 Sau khi phục hồi (trang Phục hồi hoặc `HealthMonitor` tự phục hồi), `ConnectDB.reset()` đóng bể kết nối cũ nhưng `AppNavigator` và mọi DAO/trang vẫn giữ `uiDataSource` cũ → **mọi thao tác CSDL lỗi, kể cả đăng xuất rồi đăng nhập lại**, cho tới khi mở lại app. | Cho `ConnectDB.getDataSource()` trả một `DataSource` ủy quyền luôn lấy bể hiện tại, hoặc bắt buộc khởi động lại app sau phục hồi. |
| 2 | **Quý** | 🟠 UC015 `ThongKeSanPhamDaoImpl`: `LEFT JOIN ChiTietPhieuTra` trên từng dòng chi tiết hóa đơn → nếu một máy có ≥ 2 dòng phiếu trả (ví dụ phiếu cũ `HUY`, phiếu mới `HOAN_TAT`) thì dòng bán bị nhân đôi, **doanh số cộng 2 lần**. Dữ liệu mẫu hiện chưa có trường hợp này. | Tách tổng bán và tổng trả thành hai CTE rồi `JOIN` theo `maDongSP` (cách UC016 đang làm). |
| 3 | **Quý** | CSDL phát triển đã chạy V7 seed cũ | Tạo lại CSDL (xem cảnh báo §2.1). |
| 4 | **Tài** | UC017 chưa có code (`RevenueStatisticsPage` còn là giao diện cũ, nút Excel "đang được hoàn thiện"). | Hoàn tất W3 trước khi Đăng tích hợp. |
| 5 | Nhóm | `SanPhamController`, `DanhMucController`, `SanPhamLoiController` chưa kiểm quyền ở controller (XBR-13) — đúng trạng thái "UC008/010/011 vẫn mở" của C-03. | Chốt C-03 rồi bổ sung. |

## 5. File thay đổi

| File | Nội dung |
|---|---|
| `db/migration/V7__seed_sales_trend_demo.sql` → `V13__…` | Đổi version, sửa trạng thái serial, bỏ `SELECT` |
| `util/PasswordHasher.java` | `DO_DAI_TOI_DA_BYTE`, `quaDai`, `verify` không ném lỗi |
| `controller/NhanVienController.java`, `controller/PasswordResetController.java` | Chặn mật khẩu quá dài |
| `Main.java` | Hộp thoại lỗi khởi động (UC001 7.b) |
| `ui/AppNavigator.java` | Đăng ký trang UC015, UC016; bỏ import thừa |
| Test: `AuthControllerTest`, `PasswordHasherTest`, `NhanVienControllerTest`, `ThongKeNhanVienDaoImplTest`, `AppNavigatorPagesTest` | Như trên |
| `CheckReport.java`, `CheckReport.class`, `cp.txt` | Xóa |
