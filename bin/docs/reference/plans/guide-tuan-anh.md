# Hướng dẫn code — Tuấn Anh

> **Dùng cùng:** [Phân công v3](work-assignment-4-members.md) · [UC005](../use-cases/UC005-customer-transaction-lookup.md) · [UC007](../use-cases/UC007-customer-management.md) · [UC013](../use-cases/UC013-sales-staff-management.md) · [UC016](../use-cases/UC016-sales-by-employee.md)
> **UC của bạn:** UC005, UC007, UC013, UC016 (+ `KhachHangPicker`, rà soát UC001/UC002).
> **Cập nhật theo mã nguồn 2026-10-25:** entity + `NhanVienDao` + `CustomerListPage`/`AddCustomerDialog` + UI kit đã có; **thiếu DAO/controller và dữ liệu thật**.

## 0. Hiện trạng mã nguồn (đọc trước)

| Đã có | Ở đâu | Bạn dùng thế nào |
|---|---|---|
| Entity `KhachHang`, `NhanVien`, `LoaiQuyen` | `entity/` | Không tạo lại |
| `NhanVienDao` (`findByTenDangNhap`, `updateMatKhau`) + impl | `dao/` | **Mở rộng** thêm method, không viết lại |
| `PasswordHasher` (BCrypt) | `util/` | Bắt buộc dùng cho mật khẩu |
| `MaGenerator.next(c, bang, cot, tienTo)` | `util/` | Cấp mã trong transaction hiện tại |
| `CustomerListPage`, `AddCustomerDialog` | `ui/page/customer/` | **Đã có khung UI + dữ liệu cứng** → thay bằng DAO thật |
| UI kit | `ui/component/` (`DataTable`, `PageHeader`, `FormField`, `KpiCard`, `StatusPill`, `EmptyState`, `Card`, `ModalDialog`, `SegmentedControl`) | Tái sử dụng |
| Định dạng | `ui/format/` (`MoneyFormat`, `DateFormats`, `PhoneFormat`, `PersonNames`) | Chỉ hiển thị |
| `AppNavigator` đã đăng ký `CustomerListPage` | `ui/AppNavigator.java` | Bạn **sửa constructor** trang, gửi Đăng 1 dòng đăng ký mới |

**Chưa có (việc của bạn):** `KhachHangDao` + impl; `KhachHangGiaoDichDao`; `ThongKeNhanVienDao`; các controller; `EmployeePage`, `CustomerLookupPage`, `SalesByEmployeePage`, `KhachHangPicker`; nối DAO cho `CustomerListPage`/`AddCustomerDialog`.

## 1. Môi trường

```bash
cp db.properties.example db.properties   # điền user/password, KHÔNG commit
mvn -q clean compile exec:java -Dexec.mainClass=com.dtaq.retail.Main
mvn -q test
```

Java **21**, Swing + FlatLaf + MigLayout, HikariCP, PostgreSQL JDBC, SLF4J, BCrypt, JUnit 5.
Tài khoản seed: `NVQL2026001` / `NVBH2026001`, mật khẩu demo `12345678` (xem `V2__seed_demo_accounts.sql`).

## 2. Kiến trúc và điểm nối UI

```
entity/…            record (ĐÃ CÓ)
dao/…Dao            interface + Javadoc
dao/impl/…DaoImpl   JDBC + DataSource
controller/…        nghiệp vụ, validate, quyền; KHÔNG import javax.swing
ui/page/…Page       implements Page; gọi controller; không SQL
```

```java
public final class CustomerLookupPage implements Page {
  private final TraCuuKhachHangController controller;
  private final JPanel root = new JPanel(new MigLayout("fill, wrap 1, insets 0"));

  public CustomerLookupPage(TraCuuKhachHangController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");
    // PageHeader + FormField.text(SĐT) + JTabbedPane(Hóa đơn | Phiếu đổi/trả)
  }

  @Override public PageId id() { return PageId.CUSTOMER_LOOKUP; }
  @Override public String title() { return "Tra cứu khách hàng"; }
  @Override public String section() { return "Bán hàng"; }
  @Override public JComponent view() { return root; }
}
```

`PageId` đã có: `CUSTOMERS`, `CUSTOMER_LOOKUP`, `EMPLOYEES`, `SALES_BY_EMPLOYEE`. `AppNavigator` là **file của Đăng** → gửi 1 dòng:

```java
shell.registerPage(new CustomerLookupPage(new TraCuuKhachHangController(khachHangDao, giaoDichDao)));
```

**Sửa trang đã có** (`CustomerListPage`): giữ nguyên UI, thay danh sách cứng bằng `controller.danhSach()` trong `SwingWorker`, giữ `DataTable`/`StatusPill`.

## 3. Kỹ thuật bắt buộc

| Kỹ thuật | Dùng ở | Ghi nhớ |
|---|---|---|
| `PreparedStatement` + try-with-resources | mọi DAO | Quote `"KhachHang"`, `"NhanVien"` |
| `DataSource` | constructor DAO | `ConnectDB.getDataSource()` |
| Transaction | UC013 (tạo NV + mã), UC007 (cấp mã + insert) | commit/rollback + `finally setAutoCommit(true)` |
| `MaGenerator.next(c, "KhachHang", "maKH", "KH")` | UC007 | Static, dùng `Connection` của transaction |
| `MaGenerator.next(c, "NhanVien", "maNV", "NVBANHANG")` | UC013 | Tiền tố theo vai trò |
| `PhoneFormat.format` | UC007/UC013 (hiển thị) | Chỉ định dạng hiển thị; chuẩn hoá dữ liệu bằng `replaceAll("\\s", "")` |
| `PasswordHasher.hash(char[])` / `verify` | UC013 | Lưu hash BCrypt; **không log** mật khẩu |
| `BigDecimal` + `TienUtil` | UC016 | Không `double` |
| `SwingWorker` | mọi panel | DB ngoài EDT |
| `DataTable` + `DefaultTableModel` | UC005, UC013, UC016 | `isCellEditable` → `false` |
| `JTabbedPane` | UC005 | Tab "Hóa đơn" / "Phiếu đổi-trả" |
| JUnit 5 + `Assumptions` | test DAO | `assumeTrue(ConnectDB.isAvailable())` |

**Controller chuẩn (validate + cấp mã):**

```java
public final class KhachHangController {
  private final KhachHangDao khachHangDao;
  private final DataSource dataSource;

  public KetQuaThemKhach themKhach(String hoTen, String soDienThoai, String diaChi) {
    if (hoTen == null || hoTen.isBlank()) {
      return KetQuaThemKhach.loi("Họ tên không được để trống");
    }
    String sdt = soDienThoai == null ? "" : soDienThoai.replaceAll("\\s", "");
    if (!sdt.matches("0\\d{9,10}")) {                       // 10–11 số, bắt đầu bằng 0
      return KetQuaThemKhach.loi("Số điện thoại không hợp lệ");
    }
    Optional<KhachHang> trung = khachHangDao.findBySdt(sdt);
    if (trung.isPresent()) {
      return KetQuaThemKhach.trung(trung.get());          // 8.a: dùng hồ sơ hiện có
    }
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      String maKH = MaGenerator.next(c, "KhachHang", "maKH", "KH");
      khachHangDao.insert(c, new KhachHang(maKH, hoTen.trim(), sdt,
          diaChi == null ? null : diaChi.trim()));
      c.commit();
      return KetQuaThemKhach.thanhCong(/* … */);
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu khách hàng", e);
    }
  }
}
```

> Gợi ý: cho `KhachHangDao.insert(Connection, KhachHang)` nhận connection để mã + insert nằm **cùng transaction**. Method cũ 1 tham số có thể giữ cho tiện.

**Ghi chú schema**
- `"KhachHang"` **không có UNIQUE `soDienThoai`** → chống trùng trong ứng dụng.
- `"NhanVien"` có UNIQUE `tenDangNhap` → kiểm trước + bắt `SQLState 23505`.
- Mã: `KH-YYYY-NNNNNN`, `NVBANHANG-YYYY-NNNNNN` (D7) — không cho nhập tay.

## 4. Lộ trình của bạn

| Wave | Việc | Kết quả |
|---|---|---|
| **W0** | `KhachHangDao` + impl; mở rộng `NhanVienDao` (`findAll`, `findByMa`, `insert`, `update`, `updateTrangThai`, `existsTenDangNhap`); test | `mvn test` xanh |
| **W1** | UC007 (nối `CustomerListPage` + `AddCustomerDialog`) + `KhachHangPicker`; khung UC013 | Thêm khách + chống trùng SĐT |
| **W2** | UC013 hoàn tất; UC005 | Khóa tài khoản → không đăng nhập; lịch sử mua đúng |
| **W3** | UC016 + Excel | Số HĐ/doanh số khớp tay; Excel đúng cột |
| **W4** | Rà soát UC001/UC002; test; review chéo | Phiên/khóa đúng |

## 5. Từng UC — các bước code

### 5.1. UC007 — Thêm/quản lý khách hàng (W1) — **nối trang đã có**

1. `KhachHangDao`: `findBySdt`, `findAll`, `insert`, `insert(Connection, …)`, `update`.
2. `KhachHangController` như §3; kiểm trùng SĐT trước khi insert.
3. `CustomerListPage`: thay dữ liệu cứng bằng `SwingWorker` gọi `controller.danhSach()`; nút "Thêm khách" mở `AddCustomerDialog` (đã có) → gọi `themKhach`; cập nhật bảng sau khi lưu.
4. `AddCustomerDialog`: hiển thị lỗi từ controller, **giữ dữ liệu** khi lỗi; khi trùng SĐT hiện hồ sơ cũ + nút "Dùng hồ sơ này".
5. `KhachHangPicker`: `Optional<KhachHang> chon(Component parent, String sdt)` — `ModalDialog` + ô tìm + nút "Tạo nhanh". **Đăng dùng ở UC003** → chốt W1.
6. Test: thêm mới; trùng SĐT trả hồ sơ cũ; SĐT sai; cập nhật địa chỉ.

**Xong khi:** không tạo được 2 khách cùng SĐT; mã KH đúng dạng.

### 5.2. UC013 — Thêm/quản lý nhân viên bán hàng (W1–W2) — **trang mới**

1. Mở rộng `NhanVienDao`: `findAll`, `findByMa`, `insert(Connection, NhanVien)`, `update`, `updateTrangThai`, `existsTenDangNhap`, `existsMaNV`.
2. `NhanVienController.themNhanVien(...)`:
   - validate họ tên/SĐT/tên đăng nhập/mật khẩu ≥ 8;
   - kiểm trùng mã + tên đăng nhập;
   - `PasswordHasher.hash(matKhauTho.toCharArray())`;
   - `loaiQuyen = NHAN_VIEN_BAN_HANG`, `trangThaiHoatDong = true`;
   - mã `NVBANHANG-YYYY-NNNNNN` qua `MaGenerator` trong transaction.
3. `khoaTaiKhoan(maNV)`: `updateTrangThai(maNV, false)`; chặn tự khóa (`CurrentSession.maNV()`).
4. `EmployeePage`: `DataTable` (ẩn cột mật khẩu) + form + nút "Khóa/Mở khóa" (chỉ Quản lý).
5. Đối chiếu UC001: tài khoản khóa → `AuthController` trả `LOCKED`; NV đã lập HĐ không xóa được (FK RESTRICT).
6. Test: trùng tên đăng nhập; DB lưu hash (`PasswordHasher.verify` đúng); khóa → login `LOCKED`.

**Xong khi:** không có mật khẩu thô trong DB/log; khóa không xóa lịch sử.

### 5.3. UC005 — Tra cứu khách hàng và giao dịch (W2) — **trang mới**

1. `KhachHangGiaoDichDao` (**read-only, của bạn**):
   - `List<HoaDonTomTat> hoaDonCuaKhach(String maKH)`;
   - `List<ChiTietHoaDonView> chiTietHoaDon(String maHD)`;
   - `List<PhieuTomTat> phieuCuaKhach(String maKH)` (join `PhieuTra`/`PhieuDoi` qua `hoaDon`).
2. **Không sửa** `HoaDonDao`/`ChiTietHoaDonDao` của Đăng.
3. Controller: chuẩn hoá SĐT (`PhoneFormat`), trả khách + danh sách; rỗng → "Không tìm thấy khách hàng".
4. `CustomerLookupPage`: ô SĐT + thông tin khách + `JTabbedPane` (Hóa đơn | Phiếu đổi/trả) + bảng chi tiết khi chọn HĐ.
5. Test: khách có 2 HĐ + 1 phiếu trả; khách không giao dịch; SĐT không tồn tại.

**Xong khi:** 3 tầng (khách → HĐ → chi tiết) mở được; không ghi dữ liệu.

### 5.4. UC016 — Thống kê doanh số theo nhân viên (W3) — **trang mới**

1. `ThongKeNhanVienDao`: `List<DongThongKeNhanVien> thongKe(LocalDate tu, LocalDate den, String maNV)`.
2. Doanh số = `SUM(ChiTietHoaDon.tienSauKhuyenMai)` của HĐ `trangThaiThanhToan = true` trong kỳ, nhóm theo `HoaDon.nhanVienLap`; số HĐ = `COUNT(DISTINCT maHD)`.
3. Điều chỉnh: phiếu `HOAN_TAT` trong kỳ → trừ giá trị hàng trả (snapshot HĐ gốc), quy về **nhân viên lập HĐ gốc**.
4. Dùng `DateRange` (Đăng) + `KyBaoCaoFilter`.
5. `SalesByEmployeePage`: bảng (Nhân viên, Số HĐ, Doanh số trước thuế, Điều chỉnh, Sau điều chỉnh) + nút "Xuất Excel"; double-click → danh sách HĐ + điều chỉnh.
6. Excel: `ExcelExporter.export(...)`; lỗi ghi file → giữ bảng.
7. Test: 1 NV có 2 HĐ + 1 phiếu trả hoàn tất; kỳ rỗng; NV nghỉ vẫn hiện lịch sử.

## 6. Lỗi thường gặp

| Lỗi | Hậu quả | Cách tránh |
|---|---|---|
| Lưu mật khẩu thô | Lộ thông tin | `PasswordHasher.hash` |
| Log mật khẩu/OTP | Lộ secret | Chỉ log loại exception |
| Xóa nhân viên | Mất lịch sử | Chỉ đổi `trangThaiHoatDong` |
| Tự khóa mình | Mất quyền truy cập | So `CurrentSession.maNV()` |
| Trùng SĐT | 2 hồ sơ | `findBySdt` trước insert |
| Gọi DAO trên EDT | Treo UI | `SwingWorker` |
| Join giá hiện tại khi thống kê | Sai lịch sử | Chỉ dùng snapshot |
| Tự sửa `AppNavigator`/`PageId` | Xung đột | Gửi 1 dòng cho Đăng |

## 7. Checklist

- [ ] Chỉ sửa file thuộc sở hữu (không đụng `HoaDonDao`, `DongSanPhamDao`, `KhuyenMaiDao`).
- [ ] Controller không import `javax.swing`; page không SQL.
- [ ] Validate ở controller; thông báo tiếng Việt; giữ dữ liệu khi lỗi.
- [ ] Không log mật khẩu; DB không có mật khẩu thô.
- [ ] Có test (kể cả ca khóa tài khoản → `LOCKED`).
- [ ] `mvn test` xanh; gửi Đăng 1 dòng `registerPage`.
- [ ] Báo lại: file thay đổi — UC — test — điểm còn mở.
