# Hướng dẫn code — Tài

> **Dùng cùng:** [Phân công v3](work-assignment-4-members.md) · [UC010](../use-cases/UC010-product-category-management.md) · [UC011](../use-cases/UC011-defective-product-management.md) · [UC012](../use-cases/UC012-promotion-management.md) · [UC014](../use-cases/UC014-inventory-report.md) · [UC017](../use-cases/UC017-revenue-statistics.md)
> **UC của bạn:** UC010, UC011, UC012, UC014, UC017.
> **Cập nhật theo mã nguồn 2026-10-25:** entity + UI kit + 2 trang báo cáo đã có (dữ liệu cứng); **thiếu DAO/controller**.

## 0. Hiện trạng mã nguồn (đọc trước)

| Đã có | Ở đâu | Bạn dùng thế nào |
|---|---|---|
| Entity `DanhMucSanPham`, `KhuyenMai`, `SanPhamLoi`, `TrangThaiXuLyLoi` | `entity/` | Không tạo lại |
| `LongStockReportPage` (UC014), `RevenueStatisticsPage` (UC017) | `ui/page/report/` | **Có khung UI + dữ liệu cứng** → thay bằng DAO thật |
| `BarChartPanel`, `DonutChartPanel`, `KpiCard`, `DataTable` | `ui/component/` | Tái sử dụng cho báo cáo |
| UI kit + theme | `ui/component/`, `ui/theme/` | Không tự vẽ lại |
| `MoneyFormat`, `DateFormats` | `ui/format/` | Chỉ hiển thị |
| `TienUtil`, `MaGenerator` | `util/` | Bắt buộc dùng (D4, D7) |
| `AppNavigator` đăng ký sẵn 2 trang báo cáo | `ui/AppNavigator.java` | Trang đã có chỗ; chỉ sửa constructor |

**Chưa có (việc của bạn):** DAO/impl `DanhMucSanPham`, `SanPhamLoi`, `KhuyenMai`; `BaoCaoTonKhoDao`, `ThongKeDoanhThuDao`; controller; `CategoriesPage`, `DefectiveProductsPage`, `PromotionsPage`; nối DAO cho 2 trang báo cáo.

## 1. Môi trường

```bash
cp db.properties.example db.properties   # điền user/password, KHÔNG commit
mvn -q clean compile exec:java -Dexec.mainClass=com.dtaq.retail.Main
mvn -q test
```

Java **21**, Swing + FlatLaf + MigLayout, HikariCP, PostgreSQL JDBC, SLF4J, JUnit 5. Không thêm dependency khi chưa được duyệt (đặc biệt `poi-ooxml`).

## 2. Kiến trúc và điểm nối UI

```
entity/…            record (ĐÃ CÓ)
dao/…Dao            interface + Javadoc
dao/impl/…DaoImpl   JDBC + DataSource
controller/…        nghiệp vụ, validate, quyền; KHÔNG import javax.swing
ui/page/…Page       implements Page; gọi controller; không SQL
```

```java
public final class PromotionsPage implements Page {
  private final KhuyenMaiController controller;
  private final JPanel root = new JPanel(new MigLayout("fill, wrap 1, insets 0"));

  public PromotionsPage(KhuyenMaiController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");
    // PageHeader + DataTable + form + nút "Lưu"
  }

  @Override public PageId id() { return PageId.PROMOTIONS; }
  @Override public String title() { return "Quản lý khuyến mãi"; }
  @Override public String section() { return "Quản lý"; }
  @Override public JComponent view() { return root; }
}
```

`PageId` đã có: `CATEGORIES`, `DEFECTIVE_PRODUCTS`, `PROMOTIONS`, `LONG_STOCK_REPORT`, `REVENUE_STATS`. `AppNavigator` là **file của Đăng** → gửi 1 dòng:

```java
shell.registerPage(new PromotionsPage(new KhuyenMaiController(khuyenMaiDao, dongSanPhamDao)));
```

**Sửa trang báo cáo đã có:** giữ layout/Figma, bỏ `List.of(new LongStockRow(...))` và `barChart.setData(List.of(...))`, thay bằng controller thật trong `SwingWorker`, giữ `MoneyFormat` để hiển thị.

## 3. Kỹ thuật bắt buộc

| Kỹ thuật | Dùng ở | Ghi nhớ |
|---|---|---|
| `PreparedStatement` + try-with-resources | mọi DAO | Quote `"KhuyenMai"`, `"SanPhamLoi"` |
| `DataSource` | constructor DAO | `ConnectDB.getDataSource()` |
| Transaction | UC011 (đổi trạng thái + `SanPham`), UC012 | commit/rollback + `finally setAutoCommit(true)` |
| `MaGenerator.next(c, bang, cot, tienTo)` | UC010 `DM`, UC011 `SPL`, UC012 `KM` | Static, dùng `Connection` của transaction |
| `BigDecimal` + `TienUtil` | UC014, UC017 | Không `double`; đọc `getBigDecimal` |
| `ChronoUnit.DAYS` | UC014 | `DAYS.between(ngayNhapKhoBanDau, LocalDate.now())` |
| Overlap ngày | UC012 (chồng lấn) | `!(ketThuc < batDauMoi) && !(batDau > ketThucMoi)` |
| `BarChartPanel`/`DonutChartPanel` | UC017 | `setData(List<BarPoint>/List<DonutSlice>)` |
| `SwingWorker` | mọi thao tác DB | DB ngoài EDT |
| `SQLState 23505` | UC010, UC012 | Trùng mã/tên → thông báo tiếng Việt |
| JUnit 5 + `Assumptions` | test DAO | `assumeTrue(ConnectDB.isAvailable())` |

**DAO chuẩn + bắt unique:**

```java
@Override
public void insert(DanhMucSanPham dm) {
  try (Connection c = dataSource.getConnection();
      PreparedStatement ps = c.prepareStatement(INSERT)) {
    ps.setString(1, dm.maDanhMuc());
    ps.setString(2, dm.tenDanhMuc());
    ps.setString(3, dm.moTa());
    ps.executeUpdate();
  } catch (SQLException e) {
    throw new DataAccessException("Không thể lưu danh mục", e);
  }
}

// controller
try {
  danhMucDao.insert(new DanhMucSanPham(ma, ten, moTa));
} catch (DataAccessException e) {
  if (e.getCause() instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
    return KetQua.loi("Mã hoặc tên danh mục đã tồn tại");
  }
  throw e;
}
```

**Chồng lấn khuyến mãi (10.b):**

```sql
SELECT COUNT(*) FROM "KhuyenMai"
WHERE "maDongSP" = ? AND "dangApDung" = true
  AND NOT ("ngayKetThuc" < ? OR "ngayBatDau" > ?)
```

## 4. Lộ trình của bạn

| Wave | Việc | Kết quả |
|---|---|---|
| **W0** | DAO `DanhMucSanPham`, `SanPhamLoi`, `KhuyenMai` + impl; test | `mvn test` xanh |
| **W1** | UC010 (**sớm** — Quý cần `findAll`); UC011 | Xóa danh mục bị chặn; ghi nhận lỗi OK |
| **W2** | UC012 + `findHieuLuc` cho Đăng; UC014 (nối trang) | KM chống chồng lấn; báo cáo tồn đúng |
| **W3** | UC017 (nối trang + biểu đồ + Excel) | 3 chỉ tiêu khớp tay; Excel đúng cột |
| **W4** | Test, rà soát, review chéo | Không TODO; `mvn test` xanh |

> **Excel:** `poi-ooxml` **5.4.1 đã được duyệt** (2026-10-25, kèm `log4j-to-slf4j`); **Đăng** thêm vào `pom.xml`, bạn chỉ gọi `ExcelExporter` — **không tự sửa `pom.xml`**.

## 5. Từng UC — các bước code

### 5.1. UC010 — Thêm/quản lý danh mục sản phẩm (W1) — **trang mới**

1. `DanhMucSanPhamDao`: `findAll`, `insert`, `update`, `delete`, `existsDongByDanhMuc`.
2. `DanhMucController`: validate mã/tên; chống trùng (unique index `lower(btrim(ten))`); cấp mã `DM-YYYY-NNNNNN` qua `MaGenerator`.
3. `xoa(ma)`: `existsDongByDanhMuc` → lỗi "Danh mục còn dòng sản phẩm, không thể xóa".
4. `CategoriesPage`: `DataTable` + form + nút "Xóa" (có xác nhận).
5. Test: xóa bị chặn; trùng tên khác hoa/thường; sửa mô tả.

**Xong khi:** `findAll` dùng được cho Quý (UC009).

### 5.2. UC011 — Thêm/quản lý sản phẩm lỗi (W1) — **trang mới**

1. `SanPhamLoiDao`: `insert(Connection, …)`, `findDangXuLy(soSerial)`, `findAll`, `updateTinhTrang`.
2. `SanPhamLoiController.ghiNhan(...)`: validate mô tả/ngày; máy tồn tại (`SanPhamDao.findBySerial` của Quý); chặn nếu đã có hồ sơ đang xử lý (9.a); mã `SPL-YYYY-NNNNNN`.
3. Chứng từ: kiểm serial có trong `ChiTietPhieuTra`/`ChiTietPhieuDoi`; DB có trigger `trg_sanphamloi_nguon_hop_le` → bắt lỗi, dịch sang tiếng Việt.
4. Chuyển trạng thái trong **một transaction**: `tinhTrangXuLy` + `ngayCapNhap` và đồng bộ `SanPham.trangThai` (`HANG_LOI`/`DANG_BAO_HANH`/`DA_TRA_NHA_CUNG_CAP`/`DA_THANH_LY`).
5. `DefectiveProductsPage`: bảng + lọc trạng thái + form + nút "Chuyển trạng thái".
6. Test: 2 hồ sơ cùng serial; sai serial chứng từ; trạng thái `SanPham` đổi theo.

**Xong khi:** máy lỗi không nằm trong danh sách bán được; không tạo hồ sơ trùng.

### 5.3. UC012 — Thêm/quản lý khuyến mãi (W2) — **trang mới**

1. `KhuyenMaiDao`: `insert`, `update`, `findAll`, `existsTrungMa`, `existsChongLan`, `findHieuLuc(maDongSP, ngay)`.
2. `findHieuLuc`: `dangApDung = true AND ngay BETWEEN "ngayBatDau" AND "ngayKetThuc"` → `Optional<KhuyenMai>`; **chốt chữ ký với Đăng ở W1** (Đăng dùng ở UC003 bước 7–8).
3. `KhuyenMaiController.them(...)`: validate tên, `0 ≤ tyLeGiam ≤ 100`, `ngayBatDau ≤ ngayKetThuc`, dòng SP tồn tại; kiểm trùng mã + chồng lấn trước khi insert; mã `KM-YYYY-NNNNNN`.
4. `batDau/dungApDung(maKM)`: chỉ đổi cờ `dangApDung`; không sửa HĐ cũ.
5. `PromotionsPage`: `DataTable` + form + bộ lọc dòng/trạng thái; cột "Hiệu lực" tính từ ngày + cờ.
6. Test: trùng mã; chồng lấn cùng dòng; `findHieuLuc` đúng biên ngày; KM hết hạn không trả về.

**Xong khi:** Đăng lấy được `findHieuLuc` để áp ở UC003.

### 5.4. UC014 — Báo cáo tồn kho lâu ngày (W2) — **nối trang đã có**

1. `BaoCaoTonKhoDao` (read-only): `List<DongTonLau> tonLau(int nguongNgay)` từ `SanPham` + `DongSanPham`.
2. "Còn tồn" (đã chốt 2026-10-25): tính `SAN_SANG_BAN`, `HANG_LOI`, `DANG_BAO_HANH`; **loại** `DA_BAN`, `DA_TRA_NHA_CUNG_CAP`, `DA_THANH_LY` → `WHERE "trangThai" NOT IN ('DA_BAN','DA_TRA_NHA_CUNG_CAP','DA_THANH_LY')`.
3. `soNgayTon = ChronoUnit.DAYS.between(ngayNhapKhoBanDau, LocalDate.now())` trong Java; ngưỡng do người dùng nhập (60 chỉ là mốc tham chiếu).
4. Controller: validate `nguongNgay > 0`; rỗng → "Không có máy vượt ngưỡng".
5. `LongStockReportPage`: **bỏ `List.of(new LongStockRow(...))`**, gọi controller trong `SwingWorker`, giữ `DataTable` + `KpiCard` + cột sắp xếp theo số ngày; ô ngưỡng **điền sẵn 60** (chỉ là gợi ý); thêm checkbox **"Chỉ tính máy sẵn sàng bán"** (mặc định tắt, khi bật chỉ lấy `SAN_SANG_BAN`); double-click → máy của dòng đó.
6. Excel: `ExcelExporter.export(...)`; lỗi ghi file → giữ báo cáo.
7. Test: máy 61 ngày với ngưỡng 60 → hiện; máy `DA_BAN` → không; ngưỡng ≤ 0 → lỗi.

**Xong khi:** số ngày tồn đúng; Excel khớp bảng; không còn dữ liệu cứng.

### 5.5. UC017 — Thống kê doanh thu (W3) — **nối trang đã có**

1. `ThongKeDoanhThuDao` (read-only): `ChiTieu doanhThu(tu, den)` + `List<DongDieuChinh> dieuChinh(tu, den)`.
2. Chỉ HĐ `trangThaiThanhToan = true`; `SUM(tienSauKhuyenMai)` (trước thuế), `SUM(tienThue)`, `SUM(tienSauKhuyenMai + tienThue)`; **chỉ cột snapshot**.
3. Điều chỉnh: phiếu `HOAN_TAT` (bỏ `HUY`, `TU_CHOI`, `NHAP`, `CHO_XU_LY`) → trừ giá trị hàng trả lấy từ `ChiTietHoaDon` gốc; phiếu đổi chỉ trừ **chênh lệch đã hoàn**.
4. `DateRange` (Đăng) validate kỳ; `KyBaoCaoFilter` cho UI.
5. `RevenueStatisticsPage`: **bỏ dữ liệu cứng của `barChart.setData(List.of(...))` và `donutChart.setData(List.of(...))`**, thay bằng kết quả controller; giữ `KpiCard` 3 chỉ tiêu + `DataTable` + nút "Xuất Excel".
6. Excel: xuất **đúng kết quả đang hiển thị**; lỗi ghi file → thông báo + giữ báo cáo (10.b).
7. Test: 1 HĐ 2 máy + 1 phiếu trả hoàn tất; kỳ rỗng; phiếu `TU_CHOI` không bị trừ.

**Xong khi:** 3 chỉ tiêu tách riêng, khớp tính tay; biểu đồ đúng số; Excel khớp bảng.

## 6. Lỗi thường gặp

| Lỗi | Hậu quả | Cách tránh |
|---|---|---|
| Để DB ném FK/trigger ra UI | Thông báo khó hiểu | Kiểm trước + bắt `SQLState`/trigger và dịch |
| Xóa danh mục đang dùng | Mất liên kết | `existsDongByDanhMuc` trước |
| Chỉ kiểm chồng lấn khi `dangApDung` | Bỏ sót KM trùng thời gian | Kiểm theo khoảng ngày (10.b) |
| Hard-code 60 ngày | Sai FR014 | Ngưỡng do người dùng nhập |
| `SUM` → `double` | Sai số | `getBigDecimal` + `BigDecimal` |
| Trừ tiền trả 2 lần | Doanh thu sai | Chỉ trừ phần đã hoàn thực tế |
| Xuất Excel bằng query lại | Lệch bảng đang xem | Xuất từ model hiện tại |
| Tự thêm `poi-ooxml` | Xung đột build | Chờ Đăng duyệt |
| Tự sửa `AppNavigator` | Xung đột | Gửi 1 dòng cho Đăng |

## 7. Checklist

- [ ] Chỉ sửa file thuộc sở hữu (không đụng `SanPhamDao`, `DongSanPhamDao`, `HoaDonDao`).
- [ ] Controller không import `javax.swing`; page không SQL; không còn dữ liệu cứng.
- [ ] `23505`/FK/trigger đều dịch thành thông báo tiếng Việt.
- [ ] Tiền qua `BigDecimal` + `TienUtil`; báo cáo chỉ đọc.
- [ ] Có test: chồng lấn KM, xóa danh mục bị chặn, điều chỉnh doanh thu.
- [ ] `mvn test` xanh; gửi Đăng 1 dòng `registerPage`.
- [ ] Báo lại: file thay đổi — UC — test — điểm còn mở.
