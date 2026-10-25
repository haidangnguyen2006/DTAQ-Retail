# Phân công công việc 4 thành viên — DTAQ (v3)

> **Trạng thái:** cập nhật **2026-10-25** theo yêu cầu của chủ dự án (phân lại UC thống kê, chuyển UC018 về Đăng).
> **Phạm vi tài liệu:** phân công theo use case + module mở rộng + hợp đồng giữa các thành viên.
> **Hướng dẫn code chi tiết:** [Quý](guide-quy.md) · [Tuấn Anh](guide-tuan-anh.md) · [Tài](guide-tai.md) (bắt buộc đọc trước khi code).
> **Canonical nghiệp vụ:** `docs/reference/use-cases/UC0xx-*.md`; quyết định nền: [README §5.1](../README.md#51-quyết-định-implementation-đã-duyệt-2026-10-20-và-2026-10-24).

## 1. Quyết định đã chốt

| Mã | Quyết định | Nguồn |
|---|---|---|
| D1 | "In hóa đơn" = **giả lập hiển thị phiếu trên màn hình** (`HoaDonPreviewDialog`), không máy in/PDF | [README §5.1](../README.md#51-quyết-định-implementation-đã-duyệt-2026-10-20-và-2026-10-24) |
| D2 | **3 UC thống kê chia đều** cho 3 thành viên: Quý → UC015, Tuấn Anh → UC016, Tài → UC017 | kế hoạch v3 |
| D3 | **Cả 4 báo cáo** (UC014–UC017) đều xuất Excel từ kết quả đang hiển thị | README §5.1 |
| D4 | Làm tròn **VND, 0 chữ số thập phân, `HALF_UP`**; một điểm tròn (`TienUtil`); hiển thị `1.250.000 VND` | [C-11](../requirements/scope-and-requirements.md#c-11--quy-tắc-làm-tròn-tiền-đã-chốt-2026-10-24) |
| D5 | Cron backup **00:00 hằng ngày** + chạy bù khi khởi động; backup/restore giao **Đăng** | [C-05](../requirements/scope-and-requirements.md#c-05--thiếu-use-case-cho-sao-lưuphục-hồi) |
| D6 | **Khuyến mãi cơ bản thuộc phạm vi**: UC012 quản lý, UC003 bước 7–8 áp dụng từng dòng SP | [C-04](../requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case) |
| D7 | Mã do **ứng dụng** sinh `TIỀN_TỐ-YYYY-NNNNNN`, không sequence/identity | [C-12](../requirements/scope-and-requirements.md#c-12--nhập-mã-định-danh-so-với-mã-tự-sinh-bất-biến) |

## 2. Tổng quan phân công

| Thành viên | Vai trò | Use case | Số UC | Ước lượng |
|---|---|---|---|---|
| **Đăng** | Lõi giao dịch + phê duyệt + sao lưu + hạ tầng dùng chung | UC003, UC006, UC018 + FR015/FR016 + hạ tầng | 3 UC + 2 FR | ~83h |
| **Quý** | Sản phẩm, kho & thống kê sản phẩm | UC004, UC008, UC009, UC019, **UC015** | 5 UC | ~53h |
| **Tuấn Anh** | Khách hàng, nhân sự & thống kê nhân viên | UC005, UC007, UC013, **UC016** | 4 UC | ~49h |
| **Tài** | Danh mục, hàng lỗi, khuyến mãi, tồn kho & doanh thu | UC010, UC011, UC012, UC014, **UC017** | 5 UC | ~56h |

**Phủ 19 UC:** 3 + 5 + 4 + 5 = 17, cộng UC001/UC002 (đã có code) = 19. **FR015/FR016** do Đăng.

> **Nguyên tắc cân tải:** Đăng làm **ít UC nhưng nặng nhất** (giao dịch nhiều bước + transaction + hạ tầng dùng chung). Ba bạn còn lại nhiều UC hơn, mỗi UC ít ràng buộc chéo hơn, có hướng dẫn code từng bước.

## 3. Phạm vi từng người (chi tiết code: xem guide)

### 3.1. Đăng — lõi giao dịch, phê duyệt, sao lưu, hạ tầng

| UC/FR | Nội dung chính | Module mở rộng |
|---|---|---|
| UC003 | Chọn khách + serial → áp khuyến mãi → tính tiền/thuế → nháp → thanh toán tiền mặt → lưu + snapshot + trừ tồn | Hủy nháp; chặn thiếu tiền/máy vừa bán; rollback; **hóa đơn giả lập**; snapshot 8 cột; KM hết hạn |
| UC006 | Đổi/trả theo HĐ gốc, tính hoàn theo giá trị thực tế, ngưỡng **3.000.000đ** | `PhieuTra`/`PhieuDoi`; chênh lệch; `CHO_DUYET`; mở lại sau duyệt; hàng lỗi → `SanPhamLoi`; máy thay thế |
| UC018 | Danh sách chờ duyệt → duyệt/từ chối + ý kiến/lý do | Chống ghi đè; không tự hoàn tiền/đổi tồn |
| FR015/FR016 | Sao lưu 14 bảng, cron 00:00 + chạy bù; phục hồi có xác nhận 2 bước | `manifest.json` + SHA-256; transaction; tự backup trước khi restore |

**Hạ tầng dùng chung:** `util/MaGenerator`, `util/TienUtil` **đã có** (W0-A); Đăng còn phải cung cấp `util/ExcelExporter`, `util/DateRange`, `ui/component/KyBaoCaoFilter` và giữ quyền sửa `ui/AppNavigator`.

### 3.2. Quý — xem [guide-quy.md](guide-quy.md)

| UC | Nội dung chính | Module mở rộng |
|---|---|---|
| UC004 | Tìm theo dòng/danh mục/serial → chi tiết + máy `SAN_SANG_BAN` | `SanPhamPicker` cho UC003; đếm tồn theo trạng thái |
| UC008 | Thêm máy theo serial + ngày nhập + tình trạng → cộng tồn | Trùng serial; máy lỗi → UC011; đổi trạng thái; nhà cung cấp |
| UC009 | Tạo dòng SP (danh mục, ĐVT, giá, thuế), tồn ban đầu 0 | Trùng mã; ngừng/kinh doanh lại; chặn bán dòng ngừng |
| UC019 | Cập nhật giá/thuế + xác nhận trước khi lưu | Validate 0–100; không đụng snapshot HĐ cũ |
| **UC015** | Thống kê SL bán + doanh số trước thuế theo dòng SP, điều chỉnh đổi/trả | Drill-down giao dịch; **xuất Excel** |

### 3.3. Tuấn Anh — xem [guide-tuan-anh.md](guide-tuan-anh.md)

| UC | Nội dung chính | Module mở rộng |
|---|---|---|
| UC005 | Tìm khách theo SĐT → lịch sử mua → chi tiết HĐ gốc | Danh sách phiếu đổi/trả + trạng thái; danh sách trống |
| UC007 | Thêm khách: họ tên/SĐT/địa chỉ → trùng SĐT thì dùng hồ sơ cũ | Cập nhật khách; `KhachHangPicker` cho UC003 |
| UC013 | Thêm NV + tài khoản (băm mật khẩu), khóa khi nghỉ việc | Trùng mã/tên đăng nhập; lọc trạng thái; không khóa chính mình |
| **UC016** | Thống kê số HĐ + doanh số trước thuế theo nhân viên, điều chỉnh đổi/trả | Drill-down HĐ; **xuất Excel** |

Kèm rà soát UC001/UC002 (phiên, tài khoản khóa, thông báo tiếng Việt).

### 3.4. Tài — xem [guide-tai.md](guide-tai.md)

| UC | Nội dung chính | Module mở rộng |
|---|---|---|
| UC010 | Thêm/sửa danh mục; xóa phải kiểm dòng SP tham chiếu | Unique tên chuẩn hoá; giữ dữ liệu khi lỗi |
| UC011 | Ghi nhận lỗi theo serial + chứng từ liên quan → `CHO_XU_LY` | Chống trùng hồ sơ; chuyển 4 trạng thái; đồng bộ `SanPham` |
| UC012 | Tạo khuyến mãi theo dòng SP (thời gian, % giảm, trạng thái) | Chống trùng mã; chống chồng lấn thời gian; `findHieuLuc` cho UC003 |
| UC014 | Báo cáo tồn kho lâu ngày theo ngưỡng ngày do người dùng nhập | Drill-down máy theo dòng; **xuất Excel** |
| **UC017** | Doanh thu 3 chỉ tiêu riêng + điều chỉnh đổi/trả | Xuất Excel; xử lý lỗi ghi file; loại phiếu hủy/từ chối |

## 4. Hợp đồng liên thành viên (chốt W1, không đổi sau W2)

| Interface | Chủ sở hữu | Method tối thiểu | Người dùng |
|---|---|---|---|
| `util/MaGenerator` | Đăng | `next(String prefix)` | Cả nhóm |
| `util/TienUtil` | Đăng | `lamTron(BigDecimal)`, `dinhDang(BigDecimal)` | Cả nhóm |
| `util/ExcelExporter` | Đăng | `export(File, String title, List<String> headers, List<Object[]> rows)` | Quý, Tuấn Anh, Tài |
| `util/DateRange` + `KyBaoCaoFilter` | Đăng | `parse(...)`, component lọc kỳ | Quý (UC015), Tuấn Anh (UC016), Tài (UC014/017) |
| `NhanVienDao` | Tuấn Anh | `findAll`, `update`, `updateTrangThai`, `existsTenDangNhap` | Đăng (UC003), Tuấn Anh |
| `KhachHangDao` | Tuấn Anh | `findBySdt`, `findAll`, `insert`, `update` | Đăng (UC003), Tuấn Anh |
| `DanhMucSanPhamDao` | Tài | `findAll`, `insert`, `update`, `delete`, `existsDongByDanhMuc` | Quý (UC009) |
| `DongSanPhamDao` | Quý | `findAll`, `findById`, `insert`, `update`, `updateGiaThue`, `updateTrangThaiKinhDoanh` | Đăng (UC003), Quý |
| `SanPhamDao` | Quý | `findBySerial`, `findBanDuoc(maDongSP)`, `insert`, `updateTrangThai`, `existsSerial` | Đăng (UC003), Đăng (UC006), Quý, Tài (UC011/UC014) |
| `KhuyenMaiDao` | Tài | `findHieuLuc(maDongSP, ngay)`, `insert`, `update`, `existsTrungMa`, `existsChongLan` | Đăng (UC003) |
| `SanPhamLoiDao` | Tài | `insert`, `findDangXuLy(soSerial)`, `updateTinhTrang` | Đăng (UC006), Tài |
| `HoaDonDao`, `ChiTietHoaDonDao` | Đăng | CRUD + truy vấn theo kỳ | Đăng |
| `PhieuTraDao`, `PhieuDoiDao` | Đăng | CRUD + `findChoDuyet`, `updateQuyetDuyet`, `findHoanTatTrongKy` | Đăng |

**DAO báo cáo (read-only, mỗi người tự tạo — không sửa DAO của Đăng):** Quý `ThongKeSanPhamDao`; Tuấn Anh `ThongKeNhanVienDao`; Tài `ThongKeDoanhThuDao`, `BaoCaoTonKhoDao`.

## 5. Sở hữu file

**Chỉ Đăng sửa:** `pom.xml`, `src/main/resources/db/migration/*`, `config/*`, `session/*`, `ui/MainFrame.java`, `ui/AppNavigator.java`, `ui/shell/*`, `ui/nav/*`, `util/TienUtil`, `util/MaGenerator`, `util/ExcelExporter`, `util/DateRange`, `ui/component/KyBaoCaoFilter`.

**Nguyên tắc đăng ký trang:** mỗi người tự tạo class `implements Page` (package `ui.page.<nhóm>`) dùng **`PageId` đã có**; **không sửa `AppNavigator`/`PageId`/`AppShellFrame`** — gửi Đăng **1 dòng** `registerPage(...)` để thay `PlaceholderPage`. Trang chưa đăng ký sẽ hiện `PlaceholderPage`.

**Hiện trạng UI (2026-10-25):** đã có khung + dữ liệu cứng cho `RetailInvoicePage` + `InvoiceReceiptDialog` (UC003), `CustomerListPage` + `AddCustomerDialog` (UC007), `LongStockReportPage` (UC014), `RevenueStatisticsPage` (UC017), `ManagerHomePage`/`SalesHomePage`; UI kit `ui/component/*` + theme dùng chung. **Không viết lại giao diện** — chỉ thay dữ liệu cứng bằng DAO/controller.

| Người | Entity / DAO / Controller / UI |
|---|---|
| Đăng | `HoaDon`, `ChiTietHoaDon`, `PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi` + DAO/impl; `BanHangController`, `DoiTraController`, `PheDuyetDoiTraController`; `RetailInvoicePage` (nối DAO), `InvoiceReceiptDialog`, `ReturnsPage`, `ReturnApprovalsPage`, `BackupPage`, `RestorePage`; `BackupService`, `BackupScheduler`; `ExcelExporter`, `DateRange`, `KyBaoCaoFilter` |
| Quý | `SanPhamDao`, `NhaCungCapDao`, `DongSanPhamDao` + impl; `TraCuuSanPhamController`, `SanPhamController`, `DongSanPhamController`, `GiaThueController`, `ThongKeSanPhamController`; `ProductLookupPage`, `ProductPage`, `ProductLinesPage`, `PriceTaxPage`, `SalesByProductPage`, `SanPhamPicker` |
| Tuấn Anh | `KhachHangDao` + impl, mở rộng `NhanVienDao`; `TraCuuKhachHangController`, `KhachHangController`, `NhanVienController`, `ThongKeNhanVienController`; `CustomerListPage` (nối DAO), `AddCustomerDialog`, `CustomerLookupPage`, `EmployeePage`, `SalesByEmployeePage`, `KhachHangPicker` |
| Tài | `DanhMucSanPhamDao`, `SanPhamLoiDao`, `KhuyenMaiDao` + impl; `DanhMucController`, `SanPhamLoiController`, `KhuyenMaiController`, `BaoCaoTonKhoController`, `ThongKeDoanhThuController`; `CategoriesPage`, `DefectiveProductsPage`, `PromotionsPage`, `LongStockReportPage` (nối DAO), `RevenueStatisticsPage` (nối DAO) |

## 6. Lộ trình

> **Trạng thái mã nguồn 2026-10-25:** 14 entity + `TienUtil` + `MaGenerator` + UI kit/theme + 4 trang UI có dữ liệu cứng đã có. Vì vậy W0 **không còn** việc tạo entity — chỉ còn DAO/impl + test và hạ tầng báo cáo.

| Wave | Thời gian | Đăng | Quý | Tuấn Anh | Tài |
|---|---|---|---|---|---|
| **W0** | 26/10–01/11 | `ExcelExporter`, `DateRange`, `KyBaoCaoFilter`; DAO lõi HĐ | DAO/impl `SanPham`, `DongSanPham`, `NhaCungCap` | DAO/impl `KhachHang`; mở rộng `NhanVienDao` | DAO/impl `DanhMucSanPham`, `SanPhamLoi`, `KhuyenMai` |
| **W1** | 02–08/11 | Khung UC003 (chọn khách/máy, tính tiền); `ExcelExporter` chạy được | UC004 + `SanPhamPicker`; UC008 | UC007 + `KhachHangPicker`; khung UC013 | UC010 (**sớm**, Quý cần); UC011 |
| **W2** | 09–15/11 | UC003 hoàn tất (KM + preview); UC018 | UC009; UC019 | UC013 hoàn tất; UC005 | UC012 + `findHieuLuc`; UC014 |
| **W3** | 16–22/11 | UC006; vào FR015/FR016 | **UC015** + Excel | **UC016** + Excel | **UC017** + Excel |
| **W4** | 23–29/11 | Backup/restore hoàn tất; tích hợp; `mvn test` | Rà soát, test | Rà soát UC001/002, test | Rà soát, test |

## 7. Điểm chặn còn lại

| Điểm chặn | Ảnh hưởng | Xử lý |
|---|---|---|
| **C-07** mức hạt tồn kho | UC003/004/006/008/011/014 | Bám mô hình serial đã duyệt ở V1; không tự đổi sang đếm số lượng |
| **C-08** ai cập nhật giá/thuế | UC019 (Quý) | Tạm theo tác nhân đặc tả (Quản lý); chờ chốt |
| **C-02** tên "Thêm"/"Quản lý" | UC007–UC013 | Sidebar giữ nhãn "Quản lý …"; làm luồng "Thêm" trước, cập nhật/xóa sau |
| **Dependency Apache POI** | 4 báo cáo Excel | `pom.xml` chưa có `poi-ooxml` — **Đăng xin duyệt**; nếu không duyệt → CSV UTF-8 BOM |
| **Cấu hình backup** | FR015/FR016 | Chốt `backup.dir`, số ngày giữ (mặc định 7), có backup bảng `NhanVien` hay không |
| **Thời hạn đổi/trả** | UC006 | Không hard-code; tham số hoá, mặc định không chặn |

## 8. Definition of Done

1. Đúng UC + module mở rộng ở §3, không mở rộng ngoài đặc tả.
2. Đúng kiến trúc `ui → controller → dao → dao/impl → entity`; **không SQL trong UI**.
3. Nghiệp vụ nhiều bước: transaction + rollback có test (NFR009).
4. Kiểm tra quyền ở controller, không chỉ ẩn nút (NFR002).
5. Tiền qua `TienUtil` duy nhất; không dùng `double` (D4/C-11).
6. Mã qua `MaGenerator` khi lưu thành công (D7).
7. Test JUnit 5 cho DAO/controller; `mvn test` xanh.
8. Không hard-code credential; mật khẩu qua `PasswordHasher`.
9. Báo cáo cuối UC: file thay đổi — UC/FR — test đã chạy — điểm còn mở.

## 9. Ước lượng

| Thành viên | Hạng mục | Giờ |
|---|---|---|
| Đăng | UC003 26 · UC006 24 · UC018 8 · Backup 12 · Hạ tầng 8 · Tích hợp 5 | ~83 |
| Quý | UC004 8 (+Picker 3) · UC008 12 · UC009 7 · UC019 6 · UC015 10 · DAO 7 | ~53 |
| Tuấn Anh | UC005 10 · UC007 8 (+Picker 3) · UC013 12 · UC016 10 · DAO 4 · Rà soát 2 | ~49 |
| Tài | UC010 5 · UC011 9 · UC012 12 · UC014 10 · UC017 12 · DAO 8 | ~56 |

## 10. Hướng dẫn code

| Thành viên | Tệp | Nội dung |
|---|---|---|
| Quý | [guide-quy.md](guide-quy.md) | Kỹ thuật nền, lộ trình W0–W4, từng bước code cho UC004/008/009/019/015 |
| Tuấn Anh | [guide-tuan-anh.md](guide-tuan-anh.md) | Kỹ thuật nền, lộ trình, từng bước code cho UC005/007/013/016 |
| Tài | [guide-tai.md](guide-tai.md) | Kỹ thuật nền, lộ trình, từng bước code cho UC010/011/012/014/017 |

## 11. Nhật ký thay đổi

| Ngày | Nội dung |
|---|---|
| 2026-10-24 | v2.1: chốt D1–D7; C-04 trong phạm vi; Đăng nhận 4 UC thống kê + backup |
| 2026-10-25 | **v3:** Đăng chỉ còn UC003, UC006, UC018 + FR015/016; 3 UC thống kê chia cho Quý (UC015), Tuấn Anh (UC016), Tài (UC017); thêm 3 tệp hướng dẫn code |
| 2026-10-25 | **v3.1:** đồng bộ với mã nguồn — entity/`TienUtil`/`MaGenerator`/UI kit đã có; W0 chỉ còn DAO + hạ tầng báo cáo; đăng ký trang qua `PageId` + `AppNavigator` thay cho `NavContentFactory` |
