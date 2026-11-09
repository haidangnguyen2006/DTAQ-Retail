# W1 — Tuấn Anh: UC007, `KhachHangPicker` và khung UC013

> **Branch:** `feature/anh-w1` · **Wave:** W1 (30/10–03/11)
> **Căn cứ:** [Phân công §3.3, §6](work-assignment-4-members.md) · [guide-tuan-anh.md §4, §5.1, §5.2](guide-tuan-anh.md) · [UC007](../use-cases/UC007-customer-management.md) · [UC013](../use-cases/UC013-sales-staff-management.md)

## 1. Kết quả

| Hạng mục W1 | Trạng thái |
|---|---|
| UC007: nối `CustomerListPage` + `AddCustomerDialog` với dữ liệu thật | Xong, **chờ đăng ký ở `AppNavigator`** (§2) |
| Cập nhật khách hàng (module mở rộng của UC007) | Xong |
| `KhachHangPicker` cho UC003 | Xong, sẵn sàng để Đăng dùng (§3) |
| Khung UC013: `NhanVienController` + `EmployeePage` hiển thị danh sách | Xong, **chờ đăng ký ở `AppNavigator`** (§2) |
| `mvn test` | Xanh: 242 test, 0 lỗi, 0 bỏ qua (DB phát triển có seed V2 + V4) |

UC013 mới ở mức khung theo lộ trình: thêm nhân viên, khóa/mở tài khoản và lọc trạng thái thuộc W2. Nút "Thêm nhân viên" đang bị vô hiệu hóa.

## 2. Việc cần Đăng làm: đăng ký trang trong `AppNavigator`

`AppNavigator` thuộc quyền sửa của Đăng nên chưa bị đụng đến. **Trước khi áp dụng các dòng dưới đây, ứng dụng vẫn hiện `CustomerListPage` chưa có dữ liệu và `PlaceholderPage` cho mục "Quản lý nhân viên".** Constructor không tham số của `CustomerListPage` được giữ lại để build không gãy.

Nhánh nhân viên bán hàng, thay dòng `new CustomerListPage()`:

```java
DataSource dataSource = ConnectDB.getDataSource();
CustomerListPage customers = new CustomerListPage(
    new KhachHangController(new KhachHangDaoImpl(dataSource), dataSource));
```

Nhánh quản lý:

```java
EmployeePage employees = new EmployeePage(new NhanVienController(nhanVienDao));
shell.registerPage(employees);
// thêm employees.id() vào Set.of(...) của registerPlaceholders
```

Sau khi đăng ký và kiểm tra xong, có thể xóa constructor không tham số của `CustomerListPage`.

## 3. Dùng `KhachHangPicker` ở UC003

```java
Optional<KhachHang> khach = new KhachHangPicker(khachHangController).chon(parent, sdt);
```

- Gọi trên EDT; method chờ tới khi người dùng chọn hoặc hủy. `parent` là `Frame` hoặc component bất kỳ trong cửa sổ.
- `sdt` không rỗng thì tự tra cứu ngay khi mở.
- Có nút "Tạo nhanh": mở `AddCustomerDialog` điền sẵn SĐT; lưu xong hoặc chọn dùng hồ sơ trùng thì trả về khách đó.
- Trả về rỗng khi người dùng hủy.
- Chỉ phiên **nhân viên bán hàng** dùng được, đúng tác nhân của UC003.

## 4. File thay đổi

| File | Loại | Nội dung |
|---|---|---|
| `controller/KhachHangController.java` | Mới | `danhSach`, `timTheoSdt`, `themKhach`, `capNhatKhach`, `kiemTraSdt`; kiểm tra quyền và validate |
| `controller/NhanVienController.java` | Mới | `danhSach`; chỉ Quản lý |
| `ui/page/customer/KhachHangPicker.java` | Mới | Hộp thoại chọn khách cho UC003 |
| `ui/page/employee/EmployeePage.java` | Mới | Khung UC013, bảng không có cột mật khẩu |
| `ui/page/customer/CustomerListPage.java` | Sửa | Thêm constructor nhận controller; tải dữ liệu bằng `SwingWorker`. Giao diện và API `setOn…` giữ nguyên |
| `ui/page/customer/AddCustomerDialog.java` | Sửa | Thêm chế độ cập nhật (`forEdit`), `save(...)`, hiển thị hồ sơ trùng và lỗi lưu |
| `test/.../controller/KhachHangControllerTest.java` | Mới | 14 test |
| `test/.../controller/NhanVienControllerTest.java` | Mới | 2 test |

Không sửa `AppNavigator`, `PageId`, `AppShellFrame`, `pom.xml`, migration hay DAO của thành viên khác.

## 5. Đối chiếu UC007

| Bước | Hành vi | Vị trí |
|---|---|---|
| 7 / 7.a | Thiếu họ tên hoặc SĐT sai → báo lỗi đúng ô, giữ dữ liệu đã nhập | `themKhach`, `AddCustomerDialog.submit` |
| 8 / 8.a | Trùng SĐT → không tạo mới, hiện hồ sơ để đối chiếu + nút "Dùng hồ sơ này" | `KetQuaLuuKhach.trung`, `showExistingCustomer` |
| 9–10 | Cấp mã `KH-YYYY-NNNNNN` và ghi trong cùng một transaction; lỗi thì rollback | `ghiKhachMoi` |
| 10.a | Lỗi lưu → thông báo thất bại, giữ dữ liệu để thử lại | `showFormError` |
| 11 | Thông báo thành công, tải lại bảng | `Toast` trong `CustomerListPage` |

Cập nhật khách: mã không đổi; không cho đổi sang SĐT đã thuộc khách khác. Khi đó báo lỗi ngay tại ô số điện thoại.

## 6. Kiểm thử

| Nội dung | Cách kiểm tra |
|---|---|
| Validate, trùng SĐT, cập nhật, tìm kiếm, phân trang, lỗi kết nối, phân quyền | `KhachHangControllerTest`, `NhanVienControllerTest` với fake DAO |
| Thêm mới trên DB: mã đúng dạng, dữ liệu chuẩn hóa | `themKhachTrenCsdl` (xóa dữ liệu thử sau test) |
| Lỗi sau khi ghi → rollback, không còn hồ sơ (NFR009) | `themKhachLoiGiuaChungQuayLui` |
| Luồng giao diện trên DB phát triển: phân trang, tìm kiếm, lỗi nhập, trùng SĐT + dùng hồ sơ cũ, thêm mới, cập nhật, picker (tìm thấy / hủy / tạo nhanh), `EmployeePage` đúng và sai quyền | Kịch bản tự động trên cửa sổ thật, có đối chiếu ảnh chụp; dữ liệu thử đã xóa |

Kịch bản giao diện là chương trình chạy thử riêng, không nằm trong repo: nó tự đăng ký các trang vào `AppShellFrame`, chưa chạy qua `AppNavigator` vì chưa có các dòng ở §2. `git diff --check` sạch.

## 7. Điểm cần Đăng xem

1. **Định dạng SĐT lệch giữa code cũ và guide.** `AddCustomerDialog` cũ chỉ nhận đúng 10 chữ số, guide §3 quy định 10–11 chữ số bắt đầu bằng 0 (`0\d{9,10}`). Đã theo guide và đặt quy tắc một chỗ ở `KhachHangController.kiemTraSdt`; hộp thoại gọi lại hàm này, placeholder đổi thành "Nhập số điện thoại". Tài liệu nguồn (RB09) chỉ ghi "đúng định dạng hệ thống chấp nhận". Cần xác nhận có giữ 11 chữ số hay không.
2. **Cột "Ngày tạo" luôn hiện "—".** Cột này có sẵn trên giao diện nhưng bảng `KhachHang` không có dữ liệu tương ứng. Không thêm cột vì migration thuộc Đăng và đặc tả không yêu cầu. Cần quyết định bỏ cột trên giao diện hay bổ sung schema.
3. **Chống trùng SĐT chỉ ở tầng ứng dụng.** Schema không có UNIQUE trên `soDienThoai`, nên hai máy lưu cùng SĐT đúng cùng lúc vẫn có thể tạo hai hồ sơ. Muốn chặn tuyệt đối cần một migration, cũng thuộc Đăng.
4. **Phân quyền `KhachHangController`: chỉ nhân viên bán hàng**, theo tác nhân UC007 và menu hiện tại; Quản lý bị từ chối. Nếu sau này Quản lý cần xem khách hàng thì phải nới điều kiện này.
5. **Tìm kiếm và phân trang xử lý trong bộ nhớ.** `danhSach` lọc trên `findAll()` vì hợp đồng `KhachHangDao` chưa có truy vấn phân trang. Đủ với dữ liệu hiện tại (60 khách); lượng khách lớn hơn thì nên chuyển xuống DAO.
6. **Tên kiểu kết quả khác guide.** Guide ghi `KetQuaThemKhach`; code dùng `KetQuaLuuKhach` vì cả thêm và cập nhật cùng dùng kiểu này.
7. **`EmployeePage` hiện cả tài khoản Quản lý**, có cột "Quyền", để W2 xử lý được quy tắc không tự khóa chính mình. Cần xác nhận khi hoàn tất UC013 vì đặc tả chỉ nói nhân viên bán hàng.
8. **Guide §5.2 ghi `CurrentSession.maNV()`, nhưng lớp này chưa có method đó** (hiện có `get()`/`require()`). W2 sẽ dùng `CurrentSession.require().maNV()`, không cần sửa `session/*`.
9. **Chưa đối chiếu Figma cho `KhachHangPicker` và `EmployeePage`.** Hai màn hình này dựng theo UI kit và bố cục của `CustomerListPage`.
10. **Lệch bố cục có sẵn ở hàng tìm kiếm của `CustomerListPage`:** nút "Tìm kiếm" và nhãn đếm không thẳng hàng với ô nhập. Lỗi này có từ trước, không thuộc phạm vi W1 nên chưa sửa.

## 8. Việc tiếp theo (W2, theo guide)

Hoàn tất UC013: `themNhanVien`, `khoaTaiKhoan`, lọc trạng thái, form trong `EmployeePage`, test khóa tài khoản → đăng nhập `LOCKED`. Làm UC005: `KhachHangGiaoDichDao`, `TraCuuKhachHangController`, `CustomerLookupPage`.
