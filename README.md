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

## Flyway migrations

Ứng dụng dùng **Flyway OSS 13.10.2** để chạy migration PostgreSQL lúc khởi động:

- Flyway dùng lại `DataSource` PostgreSQL hiện có, với cấu hình từ các biến `DTAQ_DB_*` và `db.properties` theo cơ chế cấu hình hiện hành.
- Vị trí migration: `classpath:db/migration`.
- Migration hiện có: [`src/main/resources/db/migration/V1__initial_schema.sql`](src/main/resources/db/migration/V1__initial_schema.sql).
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
