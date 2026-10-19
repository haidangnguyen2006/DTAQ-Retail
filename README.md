# DTAQ-Retail

Ứng dụng desktop DTAQ Retail.

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
