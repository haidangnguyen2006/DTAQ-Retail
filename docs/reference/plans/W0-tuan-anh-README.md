# W0 — Tuấn Anh: DAO khách hàng và nhân viên

> **Branch:** `feature/anh-w0` · **Wave:** W0 (26/10–29/10)
> **Căn cứ:** [Phân công §4, §6](work-assignment-4-members.md) · [guide-tuan-anh.md §3, §4, §5.1, §5.2](guide-tuan-anh.md)
> **Phạm vi:** chỉ tầng DAO + impl + test. Không có controller, UI, migration hay thay đổi `pom.xml`.

## 1. Kết quả

| Hạng mục W0 | Trạng thái |
|---|---|
| `KhachHangDao` + `KhachHangDaoImpl` (mới) | Xong |
| Mở rộng `NhanVienDao` + `NhanVienDaoImpl` (giữ nguyên 2 method cũ) | Xong |
| Test tích hợp cho cả hai DAO | Xong |
| `mvn test` | Xanh: 170 test, 0 lỗi, 0 bỏ qua (chạy trên DB phát triển có seed V2 + V4) |

## 2. File thay đổi

| File | Loại | Nội dung |
|---|---|---|
| `src/main/java/com/dtaq/retail/dao/KhachHangDao.java` | Mới | Interface + Javadoc |
| `src/main/java/com/dtaq/retail/dao/impl/KhachHangDaoImpl.java` | Mới | JDBC trên `"KhachHang"` |
| `src/main/java/com/dtaq/retail/dao/NhanVienDao.java` | Sửa | Thêm 7 method |
| `src/main/java/com/dtaq/retail/dao/impl/NhanVienDaoImpl.java` | Sửa | Cài đặt 7 method mới; tách `SELECT_COT` dùng chung |
| `src/test/java/com/dtaq/retail/dao/impl/KhachHangDaoImplTest.java` | Mới | 8 test |
| `src/test/java/com/dtaq/retail/dao/impl/NhanVienDaoImplTest.java` | Sửa | Thêm 9 test (tổng 13) |
| `src/test/java/com/dtaq/retail/controller/AuthControllerTest.java` | Sửa | Chỉ thêm stub cho `FakeNhanVienDao` |
| `src/test/java/com/dtaq/retail/controller/PasswordResetControllerTest.java` | Sửa | Chỉ thêm stub cho `FakeNhanVienDao` |

Không đụng file thuộc sở hữu của Đăng trong `src/main` (`AppNavigator`, `PageId`, `MaGenerator`, `TienUtil`, `config/*`, `session/*`, migration, `pom.xml`).

## 3. Hợp đồng DAO (phân công §4)

### 3.1. `KhachHangDao` — Đăng dùng ở UC003

| Method | Hành vi |
|---|---|
| `Optional<KhachHang> findBySdt(String)` | So khớp chính xác; nếu dữ liệu cũ có nhiều hồ sơ cùng SĐT thì trả hồ sơ có mã nhỏ nhất (`ORDER BY "maKH" LIMIT 1`). Chuẩn hoá SĐT là việc của controller |
| `List<KhachHang> findAll()` | Sắp xếp theo `maKH` |
| `void insert(KhachHang)` | Kết nối riêng, auto-commit |
| `void insert(Connection, KhachHang)` | Dùng giao dịch của bên gọi: không đóng kết nối, không đổi auto-commit (cùng mẫu với `HoaDonDao.insert`). Để `MaGenerator.next(c, "KhachHang", "maKH", "KH")` và insert nằm chung một transaction |
| `void update(KhachHang)` | Cập nhật `hoTen`, `soDienThoai`, `diaChi` theo `maKH`; mã không đổi. Không có dòng nào bị ảnh hưởng → `DataAccessException` |

### 3.2. `NhanVienDao` — method mới

| Method | Hành vi |
|---|---|
| `List<NhanVien> findAll()` | Gồm cả tài khoản đã khóa; sắp xếp theo `maNV` |
| `Optional<NhanVien> findByMa(String)` | Tra cứu theo `maNV` |
| `void insert(Connection, NhanVien)` | Dùng giao dịch của bên gọi (UC013 + `MaGenerator` tiền tố `NVBANHANG`). `matKhau` phải là hash BCrypt; lỗi trùng `tenDangNhap` (UNIQUE) → `DataAccessException` |
| `void update(NhanVien)` | **Chỉ** cập nhật `hoTen`, `soDienThoai`, `email`, `ngayVaoLam`. Không đổi `tenDangNhap`, `matKhau` (dùng `updateMatKhau`), `loaiQuyen`, `trangThaiHoatDong` (dùng `updateTrangThai`) |
| `void updateTrangThai(String, boolean)` | Khóa/mở tài khoản; nhân viên không bao giờ bị xóa |
| `boolean existsTenDangNhap(String)` | `SELECT EXISTS(...)` |
| `boolean existsMaNV(String)` | `SELECT EXISTS(...)` |

Quy ước chung (theo `NhanVienDaoImpl`/`HoaDonDaoImpl` hiện có): SQL là hằng số, tên bảng/cột trong nháy kép, `PreparedStatement` + try-with-resources, nhận `DataSource` qua constructor, lỗi bọc trong `DataAccessException` với thông báo tiếng Việt, thông báo lỗi không chứa mật khẩu.

## 4. Kiểm thử

Mọi test DAO dùng `Assumptions.assumeTrue(ConnectDB.isAvailable())` như các test hiện có, và không để lại dữ liệu:

- Test `insert(Connection, …)`: chạy trong transaction rồi `rollback`.
- Test `update`/`updateTrangThai`: khôi phục giá trị gốc trong `finally`.
- `KhachHangDaoImplTest.insertWithOwnConnectionPersists`: dùng mã `KH-9999-999901` (năm 9999, không ảnh hưởng số thứ tự năm thật) và xóa trong `finally`.

| Lớp test | Ca kiểm thử |
|---|---|
| `KhachHangDaoImplTest` | Tìm theo SĐT có/không có; `findAll` có thứ tự; insert trong transaction + định dạng `KH-YYYY-NNNNNN` + không đổi auto-commit; insert bằng kết nối riêng; từ chối mã trống/`null`; cập nhật địa chỉ; cập nhật mã không tồn tại báo lỗi |
| `NhanVienDaoImplTest` (mới thêm) | `findAll` có thứ tự; `findByMa` có/không có; `existsTenDangNhap`/`existsMaNV`; insert trong transaction + định dạng `NVBANHANG-YYYY-NNNNNN`; trùng tên đăng nhập bị chặn; từ chối `null`; `update` chỉ đổi 4 trường hồ sơ; khóa/mở tài khoản; cập nhật mã không tồn tại báo lỗi |

Lệnh đã chạy: `mvn -q test` → toàn bộ 170 test đạt; `git diff --check` sạch.

## 5. Điểm cần Đăng xem

1. **Test của UC001/UC002:** `AuthControllerTest` và `PasswordResetControllerTest` có `FakeNhanVienDao implements NhanVienDao`, nên phải thêm stub (ném `UnsupportedOperationException`) cho 7 method mới để còn compile. Logic test không đổi.
2. **Mã seed 3 chữ số và mã sinh 6 chữ số:** seed V2/V4 dùng `NVBANHANG-2026-001`…`004`, còn `MaGenerator` sinh `NNNNNN`. `MaGenerator` lấy `MAX(CAST(phần số AS INTEGER))` nên vẫn sinh đúng số tiếp theo (`NVBANHANG-2026-000005`); test insert đã chạy qua. Hai độ dài cùng tồn tại trong dữ liệu; không sửa seed vì migration thuộc Đăng.
3. **`KhachHang` không có UNIQUE `soDienThoai`:** DAO không chặn trùng. Việc kiểm tra `findBySdt` trước khi insert thuộc `KhachHangController` (UC007, W1).
4. **Chưa có `registerPage`:** W0 không có trang mới nên không cần sửa `AppNavigator`.

## 6. Việc tiếp theo (W1, theo guide)

UC007: `KhachHangController`, nối `CustomerListPage`/`AddCustomerDialog` với DAO, `KhachHangPicker` cho UC003; dựng khung UC013.
