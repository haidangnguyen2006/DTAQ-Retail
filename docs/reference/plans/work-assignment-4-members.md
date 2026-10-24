# Phân công công việc 4 thành viên — DTAQ

> **Trạng thái:** kế hoạch đã được duyệt ngày **2026-10-24** (chủ dự án xác nhận các quyết định D1–D5).
> **Phạm vi tài liệu:** phân công code theo use case và module mở rộng cho 4 thành viên **Đăng, Quý, Tuấn Anh, Tài**.
> **Không thay thế** đặc tả use case: mọi luồng nghiệp vụ vẫn lấy `docs/reference/use-cases/UC0xx-*.md` làm canonical.
> **Quyết định nền:** [README §5.1](../README.md#51-quyết-định-implementation-đã-duyệt-2026-10-20-và-2026-10-24) · [C-04](../requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case) · [C-11](../requirements/scope-and-requirements.md#c-11--quy-tắc-làm-tròn-tiền-đã-chốt-2026-10-24).

## 1. Quyết định đã chốt (áp dụng cho cả nhóm)

| Mã | Quyết định | Nguồn |
|---|---|---|
| D1 | "In hóa đơn" = **giả lập hiển thị phiếu trên màn hình** (`HoaDonPreviewDialog`); không máy in, không PDF | [README §5.1](../README.md#51-quyết-định-implementation-đã-duyệt-2026-10-20-và-2026-10-24) |
| D2 | **Đăng** phụ trách toàn bộ UC thống kê: UC014, UC015, UC016, UC017 | README §5.1 |
| D3 | **Cả 4 báo cáo đều xuất Excel** từ đúng kết quả đang hiển thị (mở rộng đã duyệt so với FR011) | README §5.1 |
| D4 | Làm tròn **VND, 0 chữ số thập phân, `HALF_UP`**; một điểm làm tròn (`TienUtil`); hiển thị `1.250.000 VND` | [C-11](../requirements/scope-and-requirements.md#c-11--quy-tắc-làm-tròn-tiền-đã-chốt-2026-10-24) |
| D5 | **Cron 00:00 hằng ngày** + chạy bù khi khởi động; backup/restore giao **Đăng** | README §5.1, [C-05](../requirements/scope-and-requirements.md#c-05--thiếu-use-case-cho-sao-lưuphục-hồi) |
| D6 | **Khuyến mãi cơ bản thuộc phạm vi**: UC012 quản lý, UC003 bước 7–8 áp dụng trên từng dòng SP | [C-04](../requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case) |
| D7 | Mã định danh do **ứng dụng** sinh `TIỀN_TỐ-YYYY-NNNNNN`; không sequence/identity | [C-12](../requirements/scope-and-requirements.md#c-12--nhập-mã-định-danh-so-với-mã-tự-sinh-bất-biến) |

## 2. Tổng quan phân công

| Thành viên | Vai trò | Use case | Số UC | Ước lượng |
|---|---|---|---|---|
| **Đăng** | Lõi giao dịch + thống kê + hạ tầng/backup | UC003, UC006, UC014, UC015, UC016, UC017 + FR015/FR016 + hạ tầng + tích hợp | 6 UC + 2 FR | ~122h |
| **Quý** | Sản phẩm & kho | UC004, UC008, UC009, UC019 | 4 UC | ~43h |
| **Tuấn Anh** | Khách hàng & nhân sự | UC005, UC007, UC013 (+ rà soát UC001/UC002) | 3 UC | ~38h |
| **Tài** | Danh mục – chất lượng – duyệt – khuyến mãi | UC010, UC011, UC018, UC012 | 4 UC | ~42h |

**Phủ 19 UC:** 6 + 4 + 3 + 4 = 17, cộng UC001/UC002 (đã có code) = 19.

> ⚠️ **Cân tải:** Đăng chiếm ~50% khối lượng (2 UC lõi + 4 báo cáo + backup + hạ tầng). Nếu cần giảm tải, hai phương án đã nêu và **chưa áp dụng**: (P1) Tài viết `util/ExcelExporter` + 4 template cột; (P2) Tài nhận UI của UC015.

## 3. Chi tiết theo thành viên

Quy ước chung cho mọi UC: lớp `ui/*Panel` → `controller/*Controller` → `dao/*Dao` → `dao/impl/*DaoImpl` → `entity/*`; **không SQL trong UI**; dùng `PreparedStatement`; nghiệp vụ nhiều bước chạy trong **một transaction** (NFR009); kiểm tra quyền ở controller (NFR002); UI tiếng Việt, cập nhật EDT, DB ngoài EDT (NFR006/NFR007).

### 3.1. Đăng

#### UC003 — Lập hóa đơn bán lẻ ([đặc tả](../use-cases/UC003-retail-invoice.md); FR007)
- **Chính:** chọn khách → chọn dòng SP + serial → kiểm trạng thái kinh doanh/tồn → lấy đơn giá trước thuế + thuế suất → **áp khuyến mãi (bước 7–8)** → tính tiền/giảm/thuế/tổng → hóa đơn nháp → nhập tiền mặt → kiểm đủ tiền → lưu HĐ + chi tiết + snapshot → cập nhật máy/tồn → hiển thị HĐ đã thanh toán.
- **Module mở rộng:** hủy hóa đơn nháp (10.a); chặn thiếu tiền (13.a) và máy mất điều kiện bán (13.b); rollback khi lỗi lưu (14.a); **hóa đơn giả lập** (D1); snapshot `donGiaBanTaiLucBan`, `thueSuatTaiLucBan`, `maKM`, `tyLeGiamTaiLucBan`, `tienKhuyenMaiThucTe`, `tienTruocKhuyenMai`, `tienSauKhuyenMai`, `tienThue`; KM không hợp lệ → giữ giá hiện hành (7.a); khách hủy sau thanh toán → chỉ xử lý qua UC006 (FR009); nhập liệu nhanh bằng `KhachHangPicker` (Tuấn Anh) + `SanPhamPicker` (Quý).
- **File:** `entity/HoaDon`, `ChiTietHoaDon`; `dao/HoaDonDao`, `ChiTietHoaDonDao`; `controller/BanHangController`; `ui/LapHoaDonPanel`, `ui/HoaDonPreviewDialog`; `util/TienUtil`.
- **Verify:** 1 máy + nhiều máy; thiếu tiền; serial vừa bị bán; KM hết hạn; rollback giả lập; hóa đơn hiển thị đúng 3 chỉ tiêu.

#### UC006 — Xử lý đổi trả ([đặc tả](../use-cases/UC006-return-exchange.md); FR008)
- **Chính:** tra HĐ gốc → chọn máy trả → tình trạng + lý do → kiểm điều kiện + số lần đã đổi/trả → chọn trả/đổi → tính hoàn theo giá trị thanh toán thực tế trên HĐ gốc → kiểm hạn mức → xác nhận → lưu phiếu + chi tiết → cập nhật máy/tồn.
- **Module mở rộng:** nhánh `PhieuTra`/`ChiTietPhieuTra` và `PhieuDoi`/`ChiTietPhieuDoi`; chênh lệch thu thêm/hoàn/ngang giá (12.a, 12.b); **≥ 3.000.000đ → `CHO_DUYET`**, chưa hoàn tiền (10.a); chỉ hoàn tất khi phiếu đã duyệt (11.a, 13.a); hàng trả lỗi → `SanPhamLoi` (15.a, trigger `fn_sanphamloi_nguon_hop_le`); máy thay thế `SAN_SANG_BAN` → `DA_BAN` (8.a, 16.a); chặn đổi/trả vượt số lượng đã mua (7.a); bảo toàn HĐ gốc.
- **Lưu ý:** **không hard-code thời hạn đổi/trả** (nguồn không quy định số ngày — RB34); tham số hoá, mặc định không chặn.
- **File:** `entity/PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi` + DAO; `controller/DoiTraController`; `ui/XuLyDoiTraPanel`.
- **Verify:** 2.999.999đ (hoàn ngay) vs 3.000.000đ (chờ duyệt); đổi máy đắt/thấp/ngang; hàng lỗi; rollback.

#### UC018 — Phê duyệt yêu cầu đổi trả ([đặc tả](../use-cases/UC018-return-approval.md)) — *Tài triển khai, đặt cạnh luồng đổi/trả*
- **Chính:** danh sách `CHO_DUYET` → xem phiếu + HĐ gốc + máy → duyệt (ý kiến) / từ chối (bắt buộc lý do) → lưu `nhanVienDuyet`, `ngayDuyet`, `lyDoDuyet`.
- **Module mở rộng:** chống ghi đè (`UPDATE … WHERE trangThaiDuyet='CHO_DUYET'`); tải lại khi xung đột (9.a); danh sách trống (2.a); duyệt **không** tự hoàn tiền/không đổi tồn.
- **File:** Tài sở hữu `controller/PheDuyetDoiTraController` và `ui/PheDuyetDoiTraPanel`; Đăng cung cấp method duyệt trong `PhieuTraDao`/`PhieuDoiDao`.
- **Verify:** duyệt → UC006 hoàn tất được; từ chối thiếu lý do → chặn; 2 quản lý duyệt cùng phiếu → người sau bị từ chối.

#### UC014 — Báo cáo tồn kho lâu ngày ([đặc tả](../use-cases/UC014-inventory-report.md); FR014)
- **Chính:** nhập **ngưỡng ngày** (60 ngày chỉ là mốc tham chiếu) → xác định máy còn tồn → tính số ngày tồn từ `ngayNhapKhoBanDau` → lọc vượt ngưỡng → hiển thị dòng/serial/ngày nhập/trạng thái/số ngày tồn.
- **Module mở rộng:** ngưỡng không hợp lệ (5.a); không có dữ liệu (8.a); drill-down máy theo dòng (9.a); **xuất Excel** (D3); lọc theo trạng thái/dòng.
- **File:** `dao/BaoCaoTonKhoDao` (+impl), `controller/BaoCaoTonKhoController`, `ui/BaoCaoTonKhoPanel`.

#### UC015 — Thống kê doanh số theo sản phẩm ([đặc tả](../use-cases/UC015-sales-by-product.md); FR013)
- **Chính:** lọc kỳ + phạm vi SP → HĐ đã thanh toán + chi tiết → tổng hợp SL + tiền trước thuế theo dòng → điều chỉnh đổi/trả `HOAN_TAT`.
- **Module mở rộng:** `ngayBatDau > ngayKetThuc` (5.a); không có giao dịch (6.a); drill-down giao dịch (9.a); **xuất Excel**.
- **File:** `ui/ThongKeDoanhSoSanPhamPanel`, controller, dùng `ThongKeDao`.

#### UC016 — Thống kê doanh số theo nhân viên ([đặc tả](../use-cases/UC016-sales-by-employee.md); FR012)
- **Chính:** lọc kỳ + phạm vi NV → nhóm HĐ theo `nhanVienLap` → số HĐ + tiền trước thuế → điều chỉnh đổi/trả.
- **Module mở rộng:** kỳ không hợp lệ (5.a); không có HĐ (6.a); drill-down HĐ + điều chỉnh (10.a); **xuất Excel**.
- **File:** `ui/ThongKeDoanhSoNhanVienPanel`, controller.

#### UC017 — Thống kê doanh thu ([đặc tả](../use-cases/UC017-revenue-statistics.md); FR010, FR011)
- **Chính:** lọc ngày/tuần/tháng/năm/khoảng → chỉ HĐ đã thanh toán → 3 chỉ tiêu **riêng** (trước thuế, thuế, tổng) → điều chỉnh phiếu đổi/trả `HOAN_TAT` → báo cáo.
- **Module mở rộng:** kỳ không hợp lệ (5.a); không có phát sinh (6.a); **xuất Excel** + lỗi ghi file (10.a/10.b); loại phiếu `HUY`/`TU_CHOI`; tránh tính trùng tiền đổi/trả.
- **File:** `util/ExcelExporter`, `util/DateRange`, `ui/component/KyBaoCaoFilter`, `controller/ThongKeDoanhThuController`, `ui/ThongKeDoanhThuPanel`, `dao/ThongKeDao`.

#### Sao lưu / phục hồi (FR015, FR016 — [C-05](../requirements/scope-and-requirements.md#c-05--thiếu-use-case-cho-sao-lưuphục-hồi))
- `config/BackupScheduler` (daemon, 00:00 hằng ngày, chạy bù khi khởi động nếu > 24h), `service/BackupService`, `ui/SaoLuuPanel`, `ui/PhucHoiPanel`.
- Nội dung: **14 bảng**; JDBC `COPY (SELECT * FROM "X") TO STDOUT WITH (FORMAT csv, HEADER)` → thư mục/zip + `manifest.json` (thời điểm, số dòng, SHA-256); cấu hình `backup.dir`, `backup.keep.days` (mặc định 7).
- Restore: xác nhận 2 bước → tự backup hiện trạng → `TRUNCATE … CASCADE` + `COPY FROM STDIN` theo thứ tự FK trong **một transaction**; không chạm `flyway_schema_history`.
- Không log/không truyền mật khẩu ra command line; dùng `DbConfig`.

#### Hạ tầng dùng chung (làm ở W0)
`util/MaGenerator` (D7), `util/TienUtil` (D4), `util/ExcelExporter` + `util/DateRange`, `ui/nav/NavItem` (thêm mục còn thiếu), review `ui/nav/NavContentFactory`, tích hợp cuối.

### 3.2. Quý

#### UC004 — Tra cứu sản phẩm và tồn kho ([đặc tả](../use-cases/UC004-product-inventory-lookup.md))
- **Chính:** tìm theo dòng/danh mục/serial → kết quả → chi tiết dòng + máy → danh sách máy `SAN_SANG_BAN`.
- **Module mở rộng:** không có kết quả (5.a); không có máy bán được (10.a); đếm tồn theo trạng thái; **`ui/component/SanPhamPicker`** cho UC003.
- **File:** `dao/SanPhamDao` (read), `controller/TraCuuSanPhamController`, `ui/TraCuuSanPhamPanel`, `ui/component/SanPhamPicker`.

#### UC008 — Thêm/quản lý sản phẩm ([đặc tả](../use-cases/UC008-product-management.md); FR006)
- **Chính:** chọn dòng SP → serial → ngày nhập kho → tình trạng → lưu → cập nhật tồn.
- **Module mở rộng:** trùng serial (9.a); ngày nhập không hợp lệ (8.a); máy lỗi → không sẵn sàng bán + chuyển UC011 (10.a.3); rollback (10.b); cập nhật thông tin/đổi trạng thái máy (danh mục); chọn `NhaCungCap`; danh sách máy theo dòng + lọc trạng thái.
- **File:** `entity/SanPham`, `NhaCungCap`; `dao/SanPhamDao` (+write), `NhaCungCapDao`; `controller/SanPhamController`; `ui/QuanLySanPhamPanel`.

#### UC009 — Thêm/quản lý dòng sản phẩm ([đặc tả](../use-cases/UC009-product-line-management.md))
- **Chính:** tạo dòng (tên, danh mục, ĐVT, giá trước thuế, thuế suất) → tồn ban đầu **0**.
- **Module mở rộng:** trùng mã (9.a); giá/thuế không hợp lệ (8.a); cập nhật thông tin; **ngừng/kinh doanh lại** (`trangThaiKinhDoanh`, danh mục); chặn bán dòng đã ngừng ở UC003.
- **File:** `entity/DongSanPham`; `dao/DongSanPhamDao`; `controller/DongSanPhamController`; `ui/QuanLyDongSanPhamPanel`.
- **Phụ thuộc:** `DanhMucSanPhamDao.findAll()` (Tuấn Anh, W1).

#### UC019 — Cập nhật giá bán và thuế suất ([đặc tả](../use-cases/UC019-price-tax-update.md))
- **Chính:** chọn dòng → hiện giá/thuế → nhập mới → validate → **xác nhận** → lưu.
- **Module mở rộng:** thuế 0–100, giá ≥ 0 (8.a); hủy xác nhận (10.a); lỗi lưu → rollback (11.a); cập nhật `ngayCapNhapCuoi`; **không đụng snapshot HĐ cũ** (NFR008).
- **File:** `controller/GiaThueController`, `ui/CapNhatGiaThuePanel`, `DongSanPhamDao.updateGiaThue`.
- **Điểm chặn:** C-08 (ai cập nhật giá/thuế) chưa chốt — tạm theo tác nhân đặc tả (Quản lý).

### 3.3. Tuấn Anh

#### UC005 — Tra cứu khách hàng và giao dịch ([đặc tả](../use-cases/UC005-customer-transaction-lookup.md); FR004)
- **Chính:** tìm theo SĐT/thông tin → kết quả → thông tin khách + lịch sử mua → chi tiết HĐ gốc.
- **Module mở rộng:** danh sách phiếu đổi/trả + trạng thái (9.a); danh sách trống (5.a, 9.b); xuất lại chứng từ khi mất HĐ.
- **File:** `dao/KhachHangDao` (read), `dao/KhachHangGiaoDichDao` (read-only), `controller/TraCuuKhachHangController`, `ui/TraCuuKhachHangPanel`.

#### UC007 — Thêm/quản lý khách hàng ([đặc tả](../use-cases/UC007-customer-management.md); FR004)
- **Chính:** nhập họ tên/SĐT/địa chỉ → validate → kiểm trùng SĐT → cấp mã → lưu.
- **Module mở rộng:** trùng SĐT → hiện hồ sơ hiện có (8.a); lỗi lưu giữ dữ liệu (10.a); cập nhật khách hàng (danh mục); **`ui/component/KhachHangPicker`** cho UC003.
- **File:** `entity/KhachHang`; `dao/KhachHangDao`; `controller/KhachHangController`; `ui/QuanLyKhachHangPanel`, `ui/component/KhachHangPicker`.

#### UC013 — Thêm/quản lý nhân viên bán hàng ([đặc tả](../use-cases/UC013-sales-staff-management.md); FR003)
- **Chính:** hồ sơ + ngày vào làm + tên đăng nhập + mật khẩu khởi tạo → lưu NV quyền `NHAN_VIEN_BAN_HANG` + tài khoản hoạt động.
- **Module mở rộng:** trùng mã NV (9.a)/trùng tên đăng nhập (9.b); **khóa đăng nhập khi nghỉ việc** (`trangThaiHoatDong=false`, không xóa); cập nhật hồ sơ; lọc theo trạng thái; băm mật khẩu bằng `util/PasswordHasher`; không cho khóa chính mình.
- **File:** mở rộng `entity/NhanVien`, `dao/NhanVienDao` (`findAll`, `update`, `updateTrangThai`, `existsTenDangNhap`); `controller/NhanVienController`; `ui/QuanLyNhanVienPanel`.

#### Rà soát UC001/UC002
Kiểm tra `CurrentSession` clear khi đăng xuất (FR002), tài khoản bị khóa không đăng nhập được (NFR001), thông báo tiếng Việt (NFR006). Không mở rộng "Quên mật khẩu" (không có UC).

### 3.4. Tài

#### UC010 — Thêm/quản lý danh mục sản phẩm ([đặc tả](../use-cases/UC010-product-category-management.md); FR005)
- **Chính:** nhập mã/tên/mô tả → validate → kiểm trùng mã → lưu.
- **Module mở rộng:** trùng mã (8.a); thiếu mã/tên (7.a); cập nhật; **xóa phải kiểm tra dòng SP tham chiếu** (danh mục + `ON DELETE RESTRICT` + unique index tên chuẩn hoá); giữ dữ liệu khi lỗi (9.a).
- **File:** `entity/DanhMucSanPham`; `dao/DanhMucSanPhamDao`; `controller/DanhMucController`; `ui/QuanLyDanhMucPanel`.
- **Ưu tiên:** W1 (Quý cần `findAll()`).

#### UC011 — Thêm/quản lý sản phẩm lỗi ([đặc tả](../use-cases/UC011-defective-product-management.md))
- **Chính:** chọn máy theo serial → mô tả lỗi → ngày phát hiện → chứng từ đổi/trả (nếu có) → lưu `CHO_XU_LY`, máy không tính vào lượng bán được.
- **Module mở rộng:** chống trùng hồ sơ đang xử lý (9.a); chứng từ không khớp serial (8.b); **cập nhật tiến độ** `CHO_XU_LY → DA_GUI_BAO_HANH → DA_TRA_NHA_CUNG_CAP → THANH_LY` + `ngayCapNhap`, `ghiChuXuLy`; danh sách theo trạng thái; đồng bộ `SanPham.trangThai`.
- **File:** `entity/SanPhamLoi`; `dao/SanPhamLoiDao`; `controller/SanPhamLoiController`; `ui/QuanLySanPhamLoiPanel`.
- **Phụ thuộc:** Đăng (UC006) gọi `SanPhamLoiDao.insert` → giao interface ở W1.

#### UC018 — Phê duyệt yêu cầu đổi trả
Chi tiết phạm vi/module mở rộng/verify ở §3.1 (đặt cạnh luồng đổi/trả của Đăng). **Tài triển khai** `PheDuyetDoiTraController` + `PheDuyetDoiTraPanel`; dùng DAO của Đăng theo hợp đồng §4.

#### UC012 — Thêm/quản lý khuyến mãi ([đặc tả](../use-cases/UC012-promotion-management.md); D6)
- **Chính:** nhập tên/mô tả → thời gian bắt đầu–kết thúc → tỷ lệ giảm → chọn dòng SP → trạng thái áp dụng → kiểm mã + xung đột thời gian → lưu + liên kết dòng SP.
- **Module mở rộng:** trùng mã (10.a); **chồng lấn thời gian trên cùng dòng SP** (10.b); kích hoạt/dừng áp dụng (danh mục); danh sách + lọc theo dòng/trạng thái/hiệu lực; `findHieuLuc(maDongSP, ngay)` cho UC003.
- **File:** `entity/KhuyenMai`; `dao/KhuyenMaiDao`; `controller/KhuyenMaiController`; `ui/QuanLyKhuyenMaiPanel`.
- **Phụ thuộc:** Đăng dùng `findHieuLuc` ở W2 → chốt chữ ký ở W1.

## 4. Hợp đồng liên thành viên (chốt ở W1, không đổi sau W2)

| Interface | Chủ sở hữu | Method tối thiểu | Người dùng |
|---|---|---|---|
| `KhachHangDao` | Tuấn Anh | `findBySdt`, `insert`, `update`, `findAll` | Đăng (UC003), Tuấn Anh (UC005) |
| `NhanVienDao` | Tuấn Anh | `findAll`, `update`, `updateTrangThai`, `existsTenDangNhap` | Đăng (UC003), Tài (UC018) |
| `DanhMucSanPhamDao` | Tài | `findAll`, `insert`, `update`, `delete`, `existsDongByDanhMuc` | Quý (UC009) |
| `DongSanPhamDao` | Quý | `findAll`, `findById`, `insert`, `update`, `updateGiaThue`, `updateTrangThaiKinhDoanh` | Đăng (UC003), Đăng (UC014/015) |
| `SanPhamDao` | Quý | `findBySerial`, `findBanDuoc(maDongSP)`, `insert`, `updateTrangThai`, `existsSerial` | Đăng (UC003/UC014), Tài (UC011) |
| `SanPhamLoiDao` | Tài | `insert`, `findDangXuLy(soSerial)`, `updateTinhTrang` | Đăng (UC006) |
| `KhuyenMaiDao` | Tài | `findHieuLuc(maDongSP, ngay)`, `insert`, `update`, `existsTrungMa`, `existsChongLan` | Đăng (UC003) |
| `HoaDonDao`, `ChiTietHoaDonDao` | Đăng | CRUD + `findDaThanhToanTrongKy` | Đăng (báo cáo) |
| `PhieuTraDao`, `PhieuDoiDao` | Đăng | CRUD + `findChoDuyet`, `updateQuyetDuyet`, `findHoanTatTrongKy` | Tài (UC018), Đăng (báo cáo) |
| `util/MaGenerator`, `util/TienUtil` | Đăng | `next(prefix)`, `lamTron`, `dinhDang` | Cả nhóm |
| `util/ExcelExporter`, `util/DateRange`, `KyBaoCaoFilter` | Đăng | `export(...)`, `parse(ky)` | Đăng (4 báo cáo) |

> **Tài chưa sở hữu `ExcelExporter`** (thuộc Đăng) trừ khi áp dụng phương án P1.

## 5. Sở hữu file

**Dùng chung — chỉ Đăng sửa:** `pom.xml`, `src/main/resources/db/migration/*`, `config/*`, `session/*`, `ui/MainFrame.java`, `ui/AppNavigator.java`, `ui/nav/NavItem.java`, `util/MaGenerator`, `util/TienUtil`, `util/ExcelExporter`, `util/DateRange`, `ui/component/KyBaoCaoFilter`.

**`ui/nav/NavContentFactory.java`:** mỗi người chỉ thêm **một nhánh `case`** của mình (theo quy ước sẵn trong file), không sửa nhánh người khác; Đăng review cuối mỗi wave.

| Người | Entity / DAO / Controller / UI |
|---|---|
| Đăng | `HoaDon`, `ChiTietHoaDon`, `PhieuTra`, `ChiTietPhieuTra`, `PhieuDoi`, `ChiTietPhieuDoi` + DAO/impl; `BanHangController`, `DoiTraController`, 4 controller thống kê; `LapHoaDonPanel`, `HoaDonPreviewDialog`, `XuLyDoiTraPanel`, 4 panel thống kê, `SaoLuuPanel`, `PhucHoiPanel`; `ThongKeDao`, `BaoCaoTonKhoDao`; `BackupService`, `BackupScheduler` |
| Quý | `SanPham`, `NhaCungCap`, `DongSanPham` + DAO/impl; `TraCuuSanPhamController`, `SanPhamController`, `DongSanPhamController`, `GiaThueController`; 3 panel + `SanPhamPicker` |
| Tuấn Anh | `KhachHang`, mở rộng `NhanVien` + DAO/impl; `TraCuuKhachHangController`, `KhachHangController`, `NhanVienController`; 3 panel + `KhachHangPicker` |
| Tài | `DanhMucSanPham`, `SanPhamLoi`, `KhuyenMai` + DAO/impl; `DanhMucController`, `SanPhamLoiController`, `KhuyenMaiController`, `PheDuyetDoiTraController`; 4 panel gồm `PheDuyetDoiTraPanel` (dùng DAO của Đăng) |

## 6. Lộ trình

| Wave | Thời gian | Đăng | Quý | Tuấn Anh | Tài | Mốc |
|---|---|---|---|---|---|---|
| **W0** | 26/10–01/11 | `MaGenerator`, `TienUtil`, entity/DAO lõi HĐ, thêm `NavItem` còn thiếu | Entity/DAO `SanPham`, `DongSanPham` | Entity/DAO `KhachHang`, `NhanVien` mở rộng | Entity/DAO `DanhMucSanPham`, `KhuyenMai`, `SanPhamLoi` | Build xanh, chạy được với seed V4 |
| **W1** | 02–08/11 | Khung UC003 (chọn khách/máy + tính tiền) | UC004 + `SanPhamPicker`; UC008 | UC010 (**sớm nhất**), UC007 + `KhachHangPicker` | UC011; `KhuyenMaiDao.findHieuLuc` | Chốt hợp đồng §4 |
| **W2** | 09–15/11 | UC003 hoàn tất (gồm KM) + preview; UC017 + `ExcelExporter` | UC009; UC019 | UC013; UC005 | UC012 + panel; UC018 | Demo UC003 end-to-end |
| **W3** | 16–22/11 | UC006; UC014 | Rà soát + test | Hoàn thiện UC005 | UC018 hoàn tất; test UC012 | Demo đổi/trả + duyệt |
| **W4** | 23–29/11 | UC015, UC016; backup/restore; tích hợp; `mvn test` | Review chéo | Review chéo UC001/002 | Review chéo | Đủ 19 UC + 2 FR phụ trợ |

**Một UC chỉ được coi là xong khi:** panel mở được từ sidebar đúng quyền; có test; `mvn test` xanh; không sửa file người khác; `git diff` chỉ chứa file đã đăng ký.

## 7. Điểm chặn còn lại

| Điểm chặn | Ảnh hưởng | Xử lý |
|---|---|---|
| **C-07** mức hạt tồn kho | UC003/004/006/008/014 | Bám mô hình serial đã duyệt ở V1; không tự đổi sang đếm số lượng |
| **C-08** ai cập nhật giá/thuế | UC019 (Quý) | Tạm theo tác nhân đặc tả (Quản lý); chờ chốt |
| **C-02** tên "Thêm" vs "Quản lý" | UC007–UC013 | Giữ nhãn sidebar "Quản lý …"; chức năng cập nhật/xóa làm sau luồng "Thêm" |
| **Dependency Apache POI** | 4 báo cáo Excel | `pom.xml` chưa có `poi-ooxml`; cần duyệt. Nếu không duyệt → xuất CSV UTF-8 BOM |
| **Backup config** | FR015/FR016 | Cần chốt `backup.dir`, số ngày giữ (mặc định 7), có backup `NhanVien` (chứa hash mật khẩu) hay loại trừ |
| **Thời hạn đổi/trả** | UC006 | Không hard-code; tham số hoá, mặc định không chặn |

## 8. Definition of Done (chung)

1. Đúng UC + đúng module mở rộng ở §3; không mở rộng ngoài đặc tả.
2. Đúng kiến trúc `ui → controller → dao → dao/impl → entity`; không SQL trong UI.
3. Nghiệp vụ nhiều bước dùng transaction + rollback có test (NFR009).
4. Phân quyền ở controller, không chỉ ẩn nút (NFR002).
5. Tiền tệ qua `TienUtil` duy nhất (D4/C-11); không dùng `double`.
6. Mã do `MaGenerator` cấp khi lưu thành công (D7).
7. Test JUnit cho DAO/controller; `mvn test` xanh.
8. Không hard-code credential; mật khẩu qua `PasswordHasher`.
9. Báo cáo cuối UC: file thay đổi — UC/FR — test đã chạy — điểm còn mở.

## 9. Ước lượng chi tiết

| Thành viên | Hạng mục | Giờ |
|---|---|---|
| Đăng | UC003 26 · UC006 24 · UC014 10 · UC015 9 · UC016 9 · UC017 12 · Excel/filter/ThongKeDao 12 · Backup 12 · Hạ tầng + tích hợp 8 | ~122 |
| Quý | UC004 8 (+Picker 3) · UC008 12 · UC009 7 · UC019 6 · Entity/DAO 7 | ~43 |
| Tuấn Anh | UC005 10 · UC007 8 (+Picker 3) · UC013 12 · Entity/DAO 3 · Rà soát 2 | ~38 |
| Tài | UC010 5 · UC011 9 · UC012 12 · UC018 8 · Entity/DAO 8 | ~42 |

> Ước lượng thô để cân tải, không phải cam kết. Nếu áp dụng P1 (Tài viết `ExcelExporter`): Đăng −10h, Tài +10h.

## 10. Nhật ký thay đổi

| Ngày | Nội dung |
|---|---|
| 2026-10-24 | Lập kế hoạch v2.1: chốt D1–D7; C-04 chuyển sang trong phạm vi (Tài làm đầy đủ UC012); Đăng nhận 4 UC thống kê + backup; chốt làm tròn VND/0 chữ số/`HALF_UP`; lưu kế hoạch thành tài liệu |
