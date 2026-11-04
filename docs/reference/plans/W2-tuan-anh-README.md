# W2 — Tuấn Anh: hoàn tất UC013 và UC005

> **Branch:** `feature/anh-w2` · **Wave:** W2 (04/11–07/11)
> **Căn cứ:** [Phân công §3.3, §6](work-assignment-4-members.md) · [guide-tuan-anh.md §5.2, §5.3](guide-tuan-anh.md) · [UC013](../use-cases/UC013-sales-staff-management.md) · [UC005](../use-cases/UC005-customer-transaction-lookup.md)

## 1. Kết quả

| Hạng mục W2 | Trạng thái |
|---|---|
| UC013: thêm nhân viên bán hàng + tài khoản, khóa/mở khóa, lọc trạng thái, chặn tự khóa | Xong, chạy được ngay (trang đã đăng ký từ W1) |
| UC005: tra cứu khách theo SĐT → lịch sử hóa đơn → chi tiết hóa đơn gốc → phiếu đổi/trả và nội dung phiếu | Xong, **chờ đăng ký `CustomerLookupPage`** (§2) |
| `mvn test` | Xanh: 327 test, 0 lỗi, 0 bỏ qua (DB phát triển, seed V2 + V4) |

## 2. Việc cần Đăng làm trong `AppNavigator`

**Bắt buộc:** đăng ký trang UC005 trong `salesPages(...)` và thêm vào `List.of(...)` trả về:

```java
CustomerLookupPage customerLookup = new CustomerLookupPage(new TraCuuKhachHangController(
    new KhachHangDaoImpl(dataSource), new KhachHangGiaoDichDaoImpl(dataSource)));
```

Chưa áp dụng thì mục "Tra cứu khách hàng và giao dịch" vẫn hiện `PlaceholderPage`.

**Nên đổi:** trong `managerPages(...)`, truyền `dataSource` cho `NhanVienController`:

```java
new EmployeePage(new NhanVienController(new NhanVienDaoImpl(dataSource), dataSource))
```

`NhanVienController` cần `DataSource` để mở transaction khi thêm nhân viên. Constructor một tham số đang được giữ lại để dòng hiện có trong `AppNavigator` vẫn biên dịch được; nó tự lấy `ConnectDB.getDataSource()`. Sau khi đổi dòng trên thì có thể xóa constructor đó.

## 3. File thay đổi

| File | Loại | Nội dung |
|---|---|---|
| `controller/NhanVienController.java` | Sửa | `themNhanVien`, `khoaTaiKhoan`, `moKhoaTaiKhoan`, `danhSach(Boolean)`; constructor nhận `DataSource` |
| `ui/page/employee/EmployeePage.java` | Sửa | Bật nút thêm, bộ lọc trạng thái, cột thao tác "Khóa / Mở khóa" có xác nhận |
| `ui/page/employee/AddEmployeeDialog.java` | Mới | Form thêm nhân viên bán hàng |
| `dao/KhachHangGiaoDichDao.java` | Mới | DAO chỉ đọc cho UC005 + các record hiển thị |
| `dao/impl/KhachHangGiaoDichDaoImpl.java` | Mới | JDBC, chỉ có câu `SELECT` |
| `controller/TraCuuKhachHangController.java` | Mới | Tra cứu khách, hóa đơn, phiếu; chỉ đọc |
| `ui/page/customer/CustomerLookupPage.java` | Mới | Màn hình UC005 |
| `test/.../controller/NhanVienControllerTest.java` | Sửa | 10 test (thay 2 test khung W1) |
| `test/.../controller/TraCuuKhachHangControllerTest.java` | Mới | 5 test |
| `test/.../dao/impl/KhachHangGiaoDichDaoImplTest.java` | Mới | 4 test trên dữ liệu mẫu |

Không sửa `AppNavigator`, `PageId`, `session/*`, `util/*`, migration, `pom.xml`, hay DAO của thành viên khác (`HoaDonDao`, `ChiTietHoaDonDao`, …).

## 4. UC013 — đối chiếu đặc tả và lý do thiết kế

| Bước UC013 | Hành vi | Vị trí |
|---|---|---|
| 3–6 | Form: họ tên, SĐT, email, ngày vào làm, tên đăng nhập, mật khẩu khởi tạo. Không có ô nhập mã | `AddEmployeeDialog` |
| 8 / 8.a | Thiếu hoặc sai dữ liệu → lỗi đúng ô, giữ nguyên dữ liệu đã nhập | `NhanVienController.kiemTraDuLieu` |
| 9.b | Tên đăng nhập đã tồn tại → lỗi tại ô tên đăng nhập | `existsTenDangNhap` + bắt `23505` |
| 9.a | Mã trùng (chỉ xảy ra khi hai lần lưu chạy cùng lúc) → báo lưu lại | bắt `23505` trên `pk_nhanvien` |
| 10–11 | Lưu với quyền `NHAN_VIEN_BAN_HANG`, `trangThaiHoatDong = true` | `ghiNhanVienMoi` |
| 10.a | Lỗi khi lưu → rollback, báo thất bại | `ghiNhanVienMoi`, test `trungTenDangNhapDoChayDongThoi` |

**Vì sao code như vậy:**

- **Mã cấp trong cùng transaction với insert.** `MaGenerator.next(c, "NhanVien", "maNV", "NVBANHANG")` dùng chung `Connection` với `insert(Connection, NhanVien)`, nên lỗi ở bất kỳ bước nào thì cả mã lẫn bản ghi đều bị rollback (D7, NFR009).
- **Kiểm tra trùng hai lớp.** Code gọi `existsTenDangNhap` trước để báo lỗi sớm, nhưng hai quản lý có thể lưu cùng lúc. Vì vậy lỗi `SQLState 23505` từ CSDL vẫn được bắt và phân biệt theo tên ràng buộc (`uq_nhanvien_tendangnhap` hoặc khóa chính), đúng như guide §3 yêu cầu.
- **Mật khẩu dạng `char[]`.** Mật khẩu lấy từ `JPasswordField.getPassword()`, băm bằng `PasswordHasher`, và mảng được xóa trắng trong `finally`. Mật khẩu thô không đi vào `String`, log hay CSDL; test kiểm tra DB chỉ chứa hash và `verify` đúng.
- **Độ dài mật khẩu ≥ 8**, cùng quy tắc với UC002 (`PasswordResetController`). Hằng số được công khai để form hiển thị đúng gợi ý.
- **Kiểm tra SĐT dùng lại `KhachHangController.kiemTraSdt`** để cả hệ thống chỉ có một quy tắc số điện thoại. Email không bắt buộc, nhưng nếu nhập thì phải đúng định dạng (`class-model-and-entities.md` §3.1, mục 1.4).
- **Khóa thay vì xóa** (XBR-09): `khoaTaiKhoan` chỉ đổi `trangThaiHoatDong`, hồ sơ và lịch sử giao dịch vẫn còn. `AuthController` đã trả `LOCKED` cho tài khoản khóa; test tích hợp đi đủ chuỗi thêm → đăng nhập được → khóa → `LOCKED` → mở khóa → đăng nhập lại được.
- **Không tự khóa chính mình:** so với `CurrentSession.require().maNV()` (guide ghi `CurrentSession.maNV()`, nhưng method đó không tồn tại).
- **Kiểm tra quyền ở controller** (NFR002, XBR-13): mọi method ném `SecurityException` nếu phiên không phải Quản lý, không chỉ dựa vào việc ẩn menu.
- **Một nút "Khóa / Mở khóa" cho mỗi dòng:** `RowAction` của `DataTable` có nhãn cố định cho cả cột. Hộp xác nhận nói rõ hành động thực sự (khóa hay mở khóa) theo trạng thái hiện tại của dòng.
- **Ngày vào làm đọc theo kiểu chặt** (`dd/MM/uuuu`, `ResolverStyle.STRICT`). `DateFormats.DATE` dùng chung sẽ tự đổi `31/02/2026` thành `28/02/2026` mà không báo lỗi. Lỗi này được phát hiện khi chạy thử giao diện; formatter chặt chỉ đặt trong dialog, không sửa `DateFormats`.
- **Lọc trạng thái ở controller** (`danhSach(Boolean)`), để trang chỉ hiển thị dữ liệu.

## 5. UC005 — đối chiếu đặc tả và lý do thiết kế

| Bước UC005 | Hành vi | Vị trí |
|---|---|---|
| 3–6 | Nhập SĐT (bỏ khoảng trắng) → tìm khách | `TraCuuKhachHangController.timKhach` |
| 5.a | Không có khách → "Không tìm thấy khách hàng", sửa SĐT và tìm lại | `CustomerLookupPage.search` |
| 7–8 | Thông tin khách + lịch sử hóa đơn (mới nhất trước) | `lichSuHoaDon` |
| 9–10 | Chọn hóa đơn → chi tiết hóa đơn gốc | `chiTietHoaDon` |
| 9.a | Tab "Phiếu đổi/trả": loại, mã, hóa đơn gốc, trạng thái duyệt, trạng thái xử lý | `phieuDoiTra` |
| 9.a.4 | Chọn phiếu → nội dung (serial, lý do, tình trạng hàng trả hoặc máy thay thế) + ý kiến duyệt | `chiTietPhieu` |
| 9.b | Không có phiếu → danh sách trống kèm thông báo | trạng thái rỗng của bảng phiếu |

**Vì sao code như vậy:**

- **DAO chỉ đọc riêng (`KhachHangGiaoDichDao`)**, theo guide §5.3, để không sửa `HoaDonDao`/`ChiTietHoaDonDao` của Đăng. Impl chỉ có câu `SELECT`; controller không có method ghi.
- **Record hiển thị nằm trong interface DAO** (`HoaDonTomTat`, `ChiTietHoaDonView`, `PhieuTomTat`, `ChiTietPhieuView`). Đây là dữ liệu tổng hợp cho màn hình, không phải thực thể nghiệp vụ, nên không thêm vào `entity/`.
- **Số tiền lấy từ snapshot trên `ChiTietHoaDon`** (`donGiaBanTaiLucBan`, `thueSuatTaiLucBan`, `tienSauKhuyenMai`, `tienThue`), không join giá hay thuế hiện tại của `DongSanPham` (RB46, C-11). Tổng hóa đơn là tổng các giá trị dòng đã làm tròn sẵn trong DB, không làm tròn lại; test khẳng định tổng hóa đơn bằng tổng chi tiết. Từ `DongSanPham` chỉ lấy **tên** sản phẩm để hiển thị.
- **Thêm `chiTietPhieu` ngoài 3 method guide liệt kê.** UC005 bước 9.a.4 yêu cầu "hiển thị nội dung và trạng thái xử lý của phiếu", mà 3 method trong guide chưa trả về các dòng hàng của phiếu.
- **Phiếu hiển thị theo khách** (guide §5.3: `phieuCuaKhach` + tab riêng), có cột "Hóa đơn gốc" để đối chiếu với tab hóa đơn.
- **Không hiển thị số tiền trên phiếu.** Schema không có cột "tổng tiền hoàn"; cách tính hoàn hoặc chênh lệch thuộc UC006 của Đăng, nên không tự suy ra để tránh hiển thị sai.
- **Chỉ nhân viên bán hàng** dùng được (tác nhân UC005), kiểm tra ở controller.
- **Truy vấn chạy ngoài EDT.** Mỗi vùng (tra cứu, chi tiết hóa đơn, chi tiết phiếu) có bộ đếm yêu cầu riêng, nên khi bấm nhanh liên tiếp thì kết quả cũ trả về muộn bị bỏ qua, không ghi đè kết quả mới.

## 6. Kiểm thử

| Nội dung | Test |
|---|---|
| Phân quyền, lọc trạng thái, validate từng trường, trùng tên đăng nhập, lỗi kết nối, xóa trắng mật khẩu, không tự khóa, khóa/mở khóa, lỗi cập nhật | `NhanVienControllerTest` (fake DAO) |
| Thêm trên DB: mã `NVBANHANG-YYYY-NNNNNN`, chỉ lưu hash, đăng nhập SUCCESS → khóa → `LOCKED` → mở → SUCCESS; dữ liệu thử bị xóa sau test | `themKhoaMoKhoaTrenCsdl` |
| Trùng tên đăng nhập lọt qua bước kiểm tra trước → CSDL chặn, rollback, báo đúng trường | `trungTenDangNhapDoChayDongThoi` |
| Khách có 2 hóa đơn + 1 phiếu trả; khách không giao dịch; SĐT không tồn tại hoặc trống; phân quyền | `TraCuuKhachHangControllerTest` |
| Dữ liệu thật: tổng hóa đơn = tổng chi tiết, phiếu trả + tình trạng hàng, phiếu đổi + máy thay thế, mã không tồn tại → rỗng | `KhachHangGiaoDichDaoImplTest` |

**Chạy thử giao diện trên cửa sổ thật** với DB phát triển, bằng một chương trình kiểm tra riêng không nằm trong repo; chương trình này tự đăng ký trang vào `AppShellFrame`. Có đối chiếu ảnh chụp, dữ liệu thử đã xóa.

- **UC005:** SĐT trống, không tìm thấy, khách có phiếu trả, khách có phiếu đổi, khách không có giao dịch, chi tiết reset khi tìm khách khác.
- **UC013:** lọc, form báo lỗi, ngày không hợp lệ, trùng tên đăng nhập, mật khẩu ngắn, thêm thành công, khóa → đăng nhập `LOCKED` → mở khóa, tự khóa bị chặn.

`git diff --check` sạch.

## 7. Điểm cần Đăng xem

1. **Đăng ký `CustomerLookupPage`** và đổi constructor `NhanVienController` (§2).
2. **Quy tắc mã và tên đăng nhập của nhân viên lệch giữa các tài liệu.** Code theo guide §3/§5.2 và D7: mã `NVBANHANG-YYYY-NNNNNN`, năm hiện tại, quản lý **tự nhập** tên đăng nhập (UC013 bước 5). Trong khi đó `class-model-and-entities.md` §3.1 ghi:
   - mã 3 chữ số với `YYYY` là **năm vào làm**;
   - số thứ tự **dùng chung** cho cả quản lý và bán hàng;
   - tên đăng nhập **tự sinh** dạng `NVBHYYYYNNN`.

   Cần chốt nguồn nào đúng; nếu theo đặc tả thực thể thì phải đổi cách gọi `MaGenerator` và bỏ ô tên đăng nhập.
3. **Thứ tự sắp xếp mã bị lẫn:** mã mới 6 chữ số (`NVBANHANG-2026-000006`) đứng trước mã mẫu 3 chữ số (`NVBANHANG-2026-001`) khi sắp theo chuỗi. Lỗi này sẽ hết khi dữ liệu mẫu và quy tắc mã được thống nhất (điểm 2).
4. **UC005 bước 3 ghi "số điện thoại hoặc thông tin nhận diện"**, guide chỉ yêu cầu SĐT. Hiện chỉ tìm theo SĐT; nếu cần thêm tiêu chí (họ tên, mã KH) thì phải bổ sung.
5. **Câu hỏi từ W1 vẫn mở:** độ dài SĐT 10–11 chữ số, và cột "Ngày tạo" của `CustomerListPage`.
6. **Chưa đối chiếu Figma** cho `CustomerLookupPage` và `AddEmployeeDialog`; hai màn hình dựng theo UI kit và bố cục các trang hiện có.

## 8. Việc tiếp theo (W3, theo guide)

UC016: `ThongKeNhanVienDao`, `ThongKeNhanVienController`, `SalesByEmployeePage` dùng `DateRange` + `KyBaoCaoFilter`, xuất Excel bằng `ExcelExporter`.
