# Hướng dẫn code — Quý

> **Dùng cùng:** [Phân công v3](work-assignment-4-members.md) · [UC004](../use-cases/UC004-product-inventory-lookup.md) · [UC008](../use-cases/UC008-product-management.md) · [UC009](../use-cases/UC009-product-line-management.md) · [UC019](../use-cases/UC019-price-tax-update.md) · [UC015](../use-cases/UC015-sales-by-product.md)
> **UC của bạn:** UC004, UC008, UC009, UC019, UC015 (+ `SanPhamPicker`).
> **Cập nhật theo mã nguồn 2026-10-25:** entity + `TienUtil` + `MaGenerator` + UI kit đã có; **thiếu DAO/controller và dữ liệu thật**.

## 0. Hiện trạng mã nguồn (đọc trước)

| Đã có | Ở đâu | Bạn dùng thế nào |
|---|---|---|
| Entity `SanPham`, `NhaCungCap`, `DongSanPham`, `TrangThaiSanPham` | `entity/` | **Không tạo lại**, chỉ đọc constructor để dùng |
| `TienUtil`, `MaGenerator` | `util/` | Bắt buộc dùng (D4, D7) |
| Khung shell + trang | `ui/shell/`, `ui/page/` | Mỗi UC = một class `implements Page`, đăng ký ở `AppNavigator` |
| UI kit | `ui/component/` (`DataTable`, `PageHeader`, `FormField`, `KpiCard`, `StatusPill`, `EmptyState`, `Card`, `ModalDialog`, `SegmentedControl`, `DtaqButton`), `ui/theme/` | Tái sử dụng, không tự vẽ lại |
| Định dạng hiển thị | `ui/format/` (`MoneyFormat`, `DateFormats`, `PhoneFormat`) | Chỉ hiển thị; **không** dùng để làm tròn |
| DAO mẫu để chép khuôn | `dao/NhanVienDao`, `dao/impl/NhanVienDaoImpl`, `dao/HoaDonDao` | Chép cấu trúc, không sửa |

**Chưa có (việc của bạn):** DAO/impl cho `SanPham`, `DongSanPham`, `NhaCungCap`; controller; 5 trang UI; `SanPhamPicker`; DAO thống kê.

## 1. Môi trường

```bash
cp db.properties.example db.properties   # điền user/password, KHÔNG commit
mvn -q clean compile exec:java -Dexec.mainClass=com.dtaq.retail.Main
mvn -q test
```

Java **21**, Swing + FlatLaf + MigLayout, HikariCP, PostgreSQL JDBC, SLF4J, JUnit 5. Không thêm dependency khi chưa xin phép.

## 2. Kiến trúc và điểm nối UI

```
entity/…            record (ĐÃ CÓ)
dao/…Dao            interface + Javadoc
dao/impl/…DaoImpl   JDBC + DataSource (chép khuôn NhanVienDaoImpl)
controller/…        nghiệp vụ, validate, transaction; KHÔNG import javax.swing
ui/page/…Page       implements Page; gọi controller; không SQL
```

**Trang mới trông như sau** (`ui/shell/Page`):

```java
package com.dtaq.retail.ui.page.product;

public final class ProductLookupPage implements Page {
  private final TraCuuSanPhamController controller;
  private final JPanel root = new JPanel(new MigLayout("fill, wrap 1, insets 0"));

  public ProductLookupPage(TraCuuSanPhamController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");
    root.setName("productLookupPage");
    // PageHeader + FormField + DataTable …
  }

  @Override public PageId id() { return PageId.PRODUCT_LOOKUP; }
  @Override public String title() { return "Tra cứu sản phẩm"; }
  @Override public String section() { return "Bán hàng"; }
  @Override public JComponent view() { return root; }
}
```

**Đăng ký trang:** `PageId` đã có sẵn mã cho mọi màn hình (`PRODUCT_LOOKUP`, `PRODUCTS`, `PRODUCT_LINES`, `PRICE_TAX`, `SALES_BY_PRODUCT`). `AppNavigator` là **file của Đăng** → bạn **gửi 1 dòng** để Đăng thêm:

```java
shell.registerPage(new ProductLookupPage(new TraCuuSanPhamController(sanPhamDao)));
```

> Không tự sửa `AppNavigator`, `PageId`, `AppShellFrame`. Trang chưa đăng ký sẽ hiện `PlaceholderPage`.

## 3. Kỹ thuật bắt buộc

| Kỹ thuật | Dùng ở | Ghi nhớ |
|---|---|---|
| `PreparedStatement` + try-with-resources | mọi DAO | Không nối chuỗi SQL; quote `"SanPham"` |
| `DataSource` (`ConnectDB.getDataSource()`) | constructor DAO | Không `DriverManager` |
| Transaction | UC008 (insert + cộng tồn), UC009, UC019 | `setAutoCommit(false)` → commit/rollback → `finally setAutoCommit(true)` |
| `BigDecimal` + `TienUtil` | UC019, UC015 | Không `double`; UI chỉ `MoneyFormat.format(...)` |
| `MaGenerator.next(c, "SanPham", "soSerial", "SP")` | UC009/UC019 nếu cần mã | **Static**, nhận `Connection` của transaction hiện tại |
| `SwingWorker` | mọi thao tác DB | DB ngoài EDT, UI trong `done()` |
| `DataTable` + `DefaultTableModel` | UC004, UC015 | `isCellEditable` → `false`; `setMoneyColumn`, `setCodeColumn` |
| `JFileChooser` + `ExcelExporter` | UC015 | Lỗi ghi file → giữ báo cáo |
| JUnit 5 + `Assumptions` | test DAO | `assumeTrue(ConnectDB.isAvailable())`, dùng seed V4 |

**DAO mẫu đúng entity hiện tại** (`SanPham` chứa object `NhaCungCap` + `DongSanPham`):

```java
public class SanPhamDaoImpl implements SanPhamDao {
  private static final String INSERT = "INSERT INTO \"SanPham\""
      + " (\"soSerial\", \"maNCC\", \"maDongSP\", \"giaNhap\","
      + " \"ngayNhapKhoBanDau\", \"trangThai\") VALUES (?, ?, ?, ?, ?, ?)";
  private static final String TANG_TON =
      "UPDATE \"DongSanPham\" SET \"soLuongTon\" = \"soLuongTon\" + 1 WHERE \"maDongSP\" = ?";

  private final DataSource dataSource;

  public SanPhamDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(SanPham sp) {
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      try (PreparedStatement ps = c.prepareStatement(INSERT);
          PreparedStatement psTon = c.prepareStatement(TANG_TON)) {
        ps.setString(1, sp.soSerial());
        ps.setString(2, sp.nhaCungCap().maNCC());      // entity chứa object, không phải mã
        ps.setString(3, sp.dongSanPham().maDongSP());
        ps.setBigDecimal(4, sp.giaNhap());
        ps.setObject(5, sp.ngayNhapKhoBanDau());
        ps.setString(6, sp.trangThai().name());
        ps.executeUpdate();

        psTon.setString(1, sp.dongSanPham().maDongSP());
        psTon.executeUpdate();
        c.commit();
      } catch (SQLException e) {
        c.rollback();
        throw new DataAccessException("Không thể thêm máy " + sp.soSerial(), e);
      } finally {
        c.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể thêm máy " + sp.soSerial(), e);
    }
  }
}
```

**Sinh mã trong transaction (UC009):**

```java
try (Connection c = dataSource.getConnection()) {
  c.setAutoCommit(false);
  String maDong = MaGenerator.next(c, "DongSanPham", "maDongSP", "DSP");
  // INSERT … rồi c.commit()
}
```

## 4. Lộ trình của bạn

| Wave | Việc | Kết quả |
|---|---|---|
| **W0** | DAO interface + impl cho `SanPham`, `DongSanPham`, `NhaCungCap`; test kết nối (entity đã có) | `mvn test` xanh; đọc được seed |
| **W1** | UC004 + `SanPhamPicker`; UC008 | Tra cứu 3 tiêu chí; thêm máy cộng tồn |
| **W2** | UC009; UC019 | Ngừng kinh doanh chặn bán; đổi giá không đổi HĐ cũ |
| **W3** | UC015 + Excel | Số liệu khớp tay; Excel đúng cột |
| **W4** | Test, rà soát, review chéo | Không TODO; `mvn test` xanh |

## 5. Từng UC — các bước code

### 5.1. UC004 — Tra cứu sản phẩm và tồn kho (W1) — **trang mới**

1. `SanPhamDao`: `findBySerial`, `findByDong`, `findByDanhMuc`, `findBanDuoc(maDongSP)`, `findAll`.
2. `DongSanPhamDao`: `search(tuKhoa)`, `findById`, `findAllWithDanhMuc`.
3. `TraCuuSanPhamController.timKiem(TieuChi)` → `List<KetQuaDong>`; tiêu chí rỗng → danh sách rỗng + thông điệp.
4. `ProductLookupPage`: `PageHeader("Tra cứu sản phẩm")` + `FormField.text` + `SegmentedControl` chọn tiêu chí + `DataTable` kết quả; chọn dòng → bảng máy bên dưới (`EmptyState` khi không có).
5. `ui/component/SanPhamPicker`: `Optional<SanPham> chon(Component parent, String maDongSP)` — `ModalDialog` + `DataTable` máy `SAN_SANG_BAN`, double-click trả kết quả. **Đăng dùng ở UC003** → chốt chữ ký W1.
6. Test: serial có trong seed → tìm thấy; serial sai → rỗng; dòng ngừng kinh doanh không có máy bán được.

**Xong khi:** mở từ menu; 3 tiêu chí chạy; không SQL trong page.

### 5.2. UC008 — Thêm/quản lý sản phẩm (W1) — **trang mới**

1. `insert` trong transaction (insert + cộng `soLuongTon`) như §3.
2. `existsSerial` báo trùng trước; vẫn bắt `SQLState 23505` để chống race.
3. `SanPhamController.themMay(...)`: serial không rỗng; `ngayNhapKhoBanDau` không ở tương lai; dòng SP tồn tại + đang kinh doanh; `NhaCungCap` tồn tại.
4. Chỉ dùng 6 trạng thái CHECK của V1: `SAN_SANG_BAN`, `DA_BAN`, `HANG_LOI`, `DANG_BAO_HANH`, `DA_TRA_NHA_CUNG_CAP`, `DA_THANH_LY`.
5. Máy lỗi khi tiếp nhận → lưu `HANG_LOI` + `JOptionPane` hỏi "Ghi nhận hồ sơ lỗi?" (chuyển UC011 của Tài — **không tự insert** `SanPhamLoi`).
6. `ProductPage`: form + `DataTable` máy theo dòng + nút "Lưu"; lỗi giữ dữ liệu đã nhập.
7. Test: thêm 2 máy → tồn +2; trùng serial → lỗi; rollback khi dòng SP không tồn tại.

### 5.3. UC009 — Thêm/quản lý dòng sản phẩm (W2) — **trang mới**

1. `DongSanPhamDao`: `insert` (tồn 0), `existsMaDong`, `update`, `updateTrangThaiKinhDoanh`.
2. Mã dòng do `MaGenerator.next(c, "DongSanPham", "maDongSP", "DSP")` cấp khi lưu — **bỏ ô nhập mã** (D7).
3. Validate: tên/ĐVT không rỗng; `0 ≤ thue ≤ 100`; `donGiaBanTruocThue ≥ 0`; `maDanhMuc` tồn tại (`DanhMucSanPhamDao.findAll` của Tài).
4. Ngừng kinh doanh = `trangThaiKinhDoanh = false`; **không xóa**.
5. `ProductLinesPage`: `DataTable` + form; cột trạng thái "Đang kinh doanh"/"Ngừng kinh doanh".
6. Test: tạo dòng tồn 0; trùng mã; thuế 101 → lỗi; ngừng → `findBanDuoc` rỗng.

### 5.4. UC019 — Cập nhật giá bán và thuế suất (W2) — **trang mới**

1. `DongSanPhamDao.updateGiaThue(maDongSP, gia, thue)` + `"ngayCapNhapCuoi" = CURRENT_DATE`.
2. `GiaThueController.capNhat(...)`: validate; trả `KetQua(giaCu, giaMoi)` để UI xác nhận 2 bước.
3. `PriceTaxPage`: `DataTable` dòng SP + panel phải hiện giá/thuế hiện tại (read-only) + ô nhập mới + `ModalDialog` xác nhận.
4. **Không** UPDATE `ChiTietHoaDon` ở bất kỳ đâu (NFR008).
5. Test: đổi giá → `findById` thấy mới; HĐ cũ giữ `donGiaBanTaiLucBan` (đọc bằng DAO của Đăng).

### 5.5. UC015 — Thống kê doanh số theo sản phẩm (W3) — **trang mới**

1. `ThongKeSanPhamDao` (read-only, **của bạn**): `List<DongThongKe> thongKe(LocalDate tu, LocalDate den, String maDongSP)`.
2. Doanh số: HĐ `trangThaiThanhToan = true` ⟕ `ChiTietHoaDon` → `SUM(soLuong)`, `SUM(tienSauKhuyenMai)` theo `maDongSP`; **chỉ dùng cột snapshot**.
3. Điều chỉnh: phiếu `trangThaiXuLyPhieu = 'HOAN_TAT'` → trừ số lượng/giá trị của serial trả, lấy từ `ChiTietHoaDon` gốc.
4. `DateRange` của Đăng để validate kỳ; `KyBaoCaoFilter` cho UI.
5. `SalesByProductPage`: bộ lọc + `DataTable` (Dòng SP, SL bán, Tiền trước thuế, SL trả, Doanh số sau điều chỉnh) + nút "Xuất Excel"; double-click → bảng giao dịch.
6. Excel: `ExcelExporter.export(...)`; lỗi ghi file → thông báo, **giữ bảng**.
7. Test: 1 HĐ 2 máy + 1 phiếu trả hoàn tất; kỳ rỗng → "không có phát sinh".

## 6. Lỗi thường gặp

| Lỗi | Hậu quả | Cách tránh |
|---|---|---|
| Gọi DAO trong `actionPerformed` | Treo UI | `SwingWorker` |
| Quên commit/rollback | Mất hoặc kẹt dữ liệu | transaction + `finally setAutoCommit(true)` |
| Dùng `driverManager`/tự tạo pool | Sai kiến trúc | `ConnectDB.getDataSource()` |
| Quên quote `"SanPham"` | `relation does not exist` | Quote mọi identifier |
| `double` cho tiền | Sai số | `BigDecimal` + `TienUtil` |
| Tròn ở UI | Lệch tổng | UI chỉ `MoneyFormat.format` |
| Tự sửa `AppNavigator`/`PageId` | Xung đột với Đăng | Gửi 1 dòng đăng ký |
| Xóa dòng SP | Vi phạm FK | Chỉ ngừng kinh doanh |

## 7. Checklist

- [ ] Chỉ tạo/sửa file thuộc sở hữu (không sửa entity đã có, không sửa `AppNavigator`).
- [ ] Page `implements Page` đúng `PageId` đã tồn tại; controller không import `javax.swing`.
- [ ] Validate ở controller; thông báo tiếng Việt; giữ dữ liệu khi lỗi.
- [ ] Tiền qua `TienUtil`; mã qua `MaGenerator` trong transaction.
- [ ] Có test DAO/controller; `mvn test` xanh.
- [ ] Gửi Đăng: 1 dòng import + 1 dòng `registerPage(...)`.
- [ ] Báo lại: file thay đổi — UC — test — điểm còn mở.
