# DTAQ-Retail

Ứng dụng desktop DTAQ Retail.

## Tài liệu

Bộ tài liệu tham chiếu dẫn xuất từ báo cáo phân tích và thiết kế (phạm vi, yêu cầu, use case, mô hình lớp, CSDL, quy tắc nghiệp vụ, màn hình) nằm tại [`docs/reference/README.md`](docs/reference/README.md).

## Build

Yêu cầu: JDK 21 và Maven 3.9+.

```bash
mvn clean package
```

Kết quả tạo ra file jar: `target/dtaq-retail-1.0.0-SNAPSHOT.jar`.

## Run

```bash
java -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar
```

Cấu hình qua biến môi trường:

| Biến | Mặc định | Mô tả |
| --- | --- | --- |
| `DTAQ_DB_URL` | `jdbc:postgresql://localhost:5432/dtaq_retail` | URL kết nối PostgreSQL |
| `DTAQ_DB_USER` | `postgres` | Tên đăng nhập cơ sở dữ liệu |
| `DTAQ_DB_PASSWORD` | (không có) | Mật khẩu cơ sở dữ liệu |
| `DTAQ_DB_POOL_SIZE` | `5` | Số kết nối tối đa trong pool |

Ví dụ:

```bash
DTAQ_DB_URL=jdbc:postgresql://localhost:5432/dtaq_retail \
DTAQ_DB_USER=postgres \
DTAQ_DB_PASSWORD=secret \
DTAQ_DB_POOL_SIZE=5 \
java -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar
```

### Chạy trong IntelliJ IDEA

Dự án có sẵn hai cấu hình chạy trong thư mục `.run/` (chia sẻ được qua Git, không phụ thuộc `.idea/`):

| Cấu hình | Dùng khi |
| --- | --- |
| `DTAQ` | Mặc định — X11, Wayland có Xwayland trên Linux, hoặc macOS/Windows; dùng JDK nào cũng được |
| `DTAQ (Flatpak/Wayland)` | IDE cài bằng Flatpak, hoặc phiên Wayland không có `DISPLAY` |

Cả hai cấu hình đặt working directory là thư mục gốc của repo để đọc được `db.properties`.

Nếu bấm Run (hoặc chạy `java -jar` trong terminal của IDE) và gặp lỗi:

```text
Exception in thread "AWT-EventQueue-0" java.awt.HeadlessException:
No X11 DISPLAY variable was set,
or no headful library support was found,
but this program performed an operation which requires it,
    at javax.swing.JFrame.<init>(JFrame.java:224)
    at com.dtaq.retail.ui.MainFrame.<init>(MainFrame.java:17)
```

thì nguyên nhân là process không có `DISPLAY` (thường gặp với IDE Flatpak trên phiên GNOME Wayland). Kiểm tra bằng `echo $DISPLAY`: nếu rỗng thì đúng trường hợp này. Hai cách xử lý:

- Chọn cấu hình `DTAQ (Flatpak/Wayland)` trong IDE, hoặc
- Chạy bằng JDK đi kèm IDE (JetBrains Runtime) và bật toolkit Wayland:

```bash
/app/jbr/bin/java -Dawt.toolkit.name=WLToolkit -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar
```

Với JBR ở đường dẫn khác, dùng `JAVA_HOME` trỏ tới JDK đó:

```bash
"$JAVA_HOME/bin/java" -Dawt.toolkit.name=WLToolkit -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar
```

> **Lưu ý:** `WLToolkit` chỉ tồn tại trong JetBrains Runtime. Nếu dùng JDK chuẩn (Temurin, Zulu, Oracle...) trên Linux, hãy chạy từ terminal desktop bình thường (đã có `DISPLAY`); **không** thêm `-Dawt.toolkit.name=WLToolkit` vì JDK không có class này. JDK 21 chưa hỗ trợ AWT Wayland native, nên phiên Wayland thuần không có Xwayland chỉ chạy được bằng JBR.

> **Lưu ý:** terminal tích hợp của IDE Flatpak không có `mvn` và `java` trên `PATH` (`which java` không tìm thấy). Hãy build ở terminal ngoài sandbox (`mvn clean package`) rồi chạy jar bằng đường dẫn JDK đầy đủ như trên.

### Cấu hình cục bộ qua `db.properties`

Thay vì đặt biến môi trường mỗi lần chạy, bạn có thể tạo file cấu hình cục bộ:

```bash
cp db.properties.example db.properties
```

Sau đó mở `db.properties` và điền các giá trị phù hợp với máy của bạn.

- `db.properties` đã được thêm vào `.gitignore`, nên **mỗi thành viên giữ giá trị riêng cho máy mình**: file này không thể bị push lên và không gây xung đột (conflict) giữa các thành viên.
- Thứ tự ưu tiên khi đọc cấu hình: thuộc tính hệ thống (`-D`) > biến môi trường > `db.properties` > giá trị mặc định.
- File được đọc từ **thư mục làm việc hiện tại (current working directory)**, vì vậy hãy chạy lệnh từ thư mục gốc của repo; nếu chạy ở nơi khác, file sẽ không được tìm thấy và ứng dụng dùng giá trị mặc định.
- `DTAQ_DB_PASSWORD` trong file này là văn bản thuần (plaintext) trên đĩa, nhưng vì file không bao giờ được commit và cũng không được đóng gói vào fat jar, nên mật khẩu không bị lộ qua Git hay qua file jar phát hành.

Xem `db.properties.example` để biết danh sách đầy đủ các khóa cấu hình.

### Tài khoản demo (V2)

Hai tài khoản demo được tạo bởi migration [`V2__seed_demo_accounts.sql`](src/main/resources/db/migration/V2__seed_demo_accounts.sql):

| Tên đăng nhập | Vai trò | Mật khẩu |
| --- | --- | --- |
| `NVQL2026001` | Nhân viên quản lý | `12345678` |
| `NVBH2026001` | Nhân viên bán hàng | `12345678` |

> **Cảnh báo:** đây là thông tin đăng nhập **demo**, chỉ dùng cho môi trường phát triển và kiểm thử. Không dùng các tài khoản này cho dữ liệu thật và không sao chép mật khẩu demo sang môi trường triển khai.

## Quên mật khẩu (OTP qua Gmail SMTP)

Chức năng quên mật khẩu gửi một mã xác nhận (OTP) 6 chữ số tới email đã đăng ký của nhân viên, sau đó cho phép đặt mật khẩu mới. Cấu hình máy chủ gửi mail qua các biến sau:

| Biến | Mặc định | Mô tả |
| --- | --- | --- |
| `DTAQ_SMTP_HOST` | `smtp.gmail.com` | Máy chủ SMTP dùng để gửi mail |
| `DTAQ_SMTP_PORT` | `587` | Cổng SMTP (STARTTLS) |
| `DTAQ_SMTP_USER` | (không có) | Tài khoản SMTP, thường là địa chỉ Gmail |
| `DTAQ_SMTP_PASSWORD` | (không có) | App Password của Gmail, không phải mật khẩu đăng nhập |
| `DTAQ_SMTP_FROM` | giá trị của `DTAQ_SMTP_USER` | Địa chỉ người gửi hiển thị trong email |
| `DTAQ_SMTP_STARTTLS` | `true` | Bật STARTTLS (`true`/`false`, không phân biệt hoa thường) |
| `DTAQ_SMTP_TIMEOUT_MS` | `10000` | Thời gian chờ kết nối/đọc của SMTP, tính bằng mili giây |

Thứ tự ưu tiên khi đọc cấu hình: thuộc tính hệ thống (`-D`) > biến môi trường > `db.properties` > giá trị mặc định.

### Cấu hình Gmail

1. Bật **Xác minh 2 bước (2-Step Verification)** cho tài khoản Google dùng để gửi mail.
2. Tạo **App Password** tại trang bảo mật của tài khoản Google (Google Account → Security → App passwords); Google chỉ hiển thị mật khẩu ứng dụng một lần.
3. Điền `DTAQ_SMTP_USER` bằng địa chỉ Gmail và `DTAQ_SMTP_PASSWORD` bằng App Password vừa tạo (16 ký tự, có thể nhập liền không dấu cách).
4. Chạy lại ứng dụng và thử chức năng quên mật khẩu với một tài khoản có email đã đăng ký.

App Password **không** phải mật khẩu đăng nhập Gmail thông thường; nếu chưa bật xác minh 2 bước, Gmail sẽ từ chối đăng nhập SMTP.

> **Bảo mật:** không commit App Password hay bất kỳ thông tin đăng nhập thật nào vào repository. `db.properties` đã nằm trong `.gitignore`, chỉ dùng cho cấu hình cục bộ của từng máy; `db.properties.example` chỉ chứa khóa với giá trị rỗng.

### Chính sách mã xác nhận (OTP)

- Mã gồm đúng **6 chữ số**, sinh bằng nguồn ngẫu nhiên an toàn.
- Mã có hiệu lực **5 phút** kể từ lúc gửi.
- Nhập sai tối đa **5 lần**; quá số lần cho phép thì mã bị hủy và phải gửi lại.
- Mỗi mã chỉ dùng **một lần**: đặt lại mật khẩu thành công sẽ xóa phiếu đặt lại.
- Chỉ được gửi lại sau **60 giây** kể từ lần gửi trước.
- Hệ thống chỉ lưu **mã băm** của OTP, không lưu mã thô.
- Khi gửi mã hoặc đặt lại mật khẩu thất bại, giao diện luôn hiển thị thông điệp chung và **không tiết lộ tài khoản hay email có tồn tại hay không**.

## Flyway migrations

Ứng dụng dùng **Flyway OSS 13.10.2** để chạy migration PostgreSQL lúc khởi động:

- Flyway dùng lại `DataSource` PostgreSQL hiện có, với cấu hình từ các biến `DTAQ_DB_*` và `db.properties` theo cơ chế cấu hình hiện hành.
- Vị trí migration: `classpath:db/migration`.
- Migration hiện có:
  - [`V1__initial_schema.sql`](src/main/resources/db/migration/V1__initial_schema.sql) — tạo 14 bảng nghiệp vụ.
  - [`V2__seed_demo_accounts.sql`](src/main/resources/db/migration/V2__seed_demo_accounts.sql) — tạo hai tài khoản demo, xem [Tài khoản demo (V2)](#tài-khoản-demo-v2).
  - [`V3__password_reset.sql`](src/main/resources/db/migration/V3__password_reset.sql) — tạo bảng `DatLaiMatKhau` lưu mã băm của mã đặt lại mật khẩu theo từng nhân viên.
  - [`V4__seed_full_data.sql`](src/main/resources/db/migration/V4__seed_full_data.sql) — nạp dữ liệu demo đầy đủ cho 14 bảng nghiệp vụ (`NhanVien`, `KhachHang`, `DanhMucSanPham`, `NhaCungCap`, `DongSanPham`, `SanPham`, `KhuyenMai`, `HoaDon`, `ChiTietHoaDon`, `PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi`, `SanPhamLoi`); **không** seed bảng `DatLaiMatKhau`; dùng lại hash BCrypt demo của V2 (mật khẩu demo `12345678`) và chỉ chứa SĐT/email hư cấu; tự động áp dụng khi khởi động trên CSDL đã ở V1–V3; kết thúc bằng khối `DO $$` tự kiểm và hủy migration nếu vi phạm các quy tắc toàn vẹn RB23/RB24/RB26/RB30/RB37/RB38/RB54/RB56.
- `Main` lấy shared `DataSource` từ `ConnectDB`, chạy Flyway trên main thread **trước khi giao diện Swing được khởi tạo**, rồi mới mở `MainFrame` trên EDT.
- Nếu migration thất bại, ứng dụng ghi log chẩn đoán an toàn (không ghi credential/URL), đóng pool và dừng khởi động; giao diện không được mở.
- Không bật baseline tự động.

### V1 — initial schema

`V1__initial_schema.sql` tạo **14 bảng nghiệp vụ** theo thiết kế đã ghi trong báo cáo:

`NhanVien`, `KhachHang`, `DanhMucSanPham`, `NhaCungCap`, `DongSanPham`, `SanPham`, `KhuyenMai`, `HoaDon`, `ChiTietHoaDon`, `PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi`, `SanPhamLoi`.

Các điểm đã được phê duyệt cho V1:

- `ChiTietHoaDon` tham chiếu `SanPham` theo serial và lưu snapshot đơn giá, thuế suất và khuyến mãi tại thời điểm bán.
- `SanPhamLoi` tham chiếu serial và có thể tham chiếu phiếu trả/phiếu đổi nguồn.
- Khóa chính là chuỗi do **ứng dụng sinh**; V1 **không** tạo sequence, identity hay default cho ID.
- Không tạo bảng demo `products`, không có seed data và không có bảng tương thích.

> V1 chỉ dành cho CSDL PostgreSQL **mới/rỗng** và đã được kiểm chứng runtime trên PostgreSQL **16.13 disposable** (không phải CSDL thật): Flyway 13.10.2 lần đầu áp dụng 1 migration, lần hai không còn migration chờ. Bằng chứng đã ghi tại [`docs/reference/database/schema-and-integrity.md` §6.6](docs/reference/database/schema-and-integrity.md#66-kiểm-chứng-v1); CSDL disposable đã được dừng và xóa sau kiểm thử.
