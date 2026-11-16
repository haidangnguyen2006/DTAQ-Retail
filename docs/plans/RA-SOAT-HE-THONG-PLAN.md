# DTAQ — Plan rà soát & sửa lỗi hệ thống (nghiệp vụ · UX/UI · truy vấn CSDL)

> **Ngày rà soát:** 2026-11-16 · **Nhánh lúc rà soát:** `refactor/bug-smell` (HEAD `d7eab22`)
> **Phương pháp:** đọc tĩnh mã nguồn + migration + tài liệu `docs/reference/`. **Chưa chạy ứng dụng, chưa chạy test, chưa đo `EXPLAIN`** — mọi số dòng bên dưới là tại HEAD nói trên; nếu lệch, tìm theo tên hàm/hằng số.
> **Đối tượng đọc:** AI agent / lập trình viên **không có ngữ cảnh** của phiên rà soát. Đọc hết §1–§3 trước khi sửa bất cứ thứ gì.

---

## 0. Trạng thái thực hiện (hoàn tất 2026-11-16)

Baseline trước khi sửa: 692 test pass, 0 fail, 0 skip (có PostgreSQL). Sau khi sửa: **707 pass** với `mvn -B clean package` (đúng lệnh CI cộng đóng gói).

| ID | Trạng thái | Ghi chú so với mô tả ở §4–§6 |
|---|---|---|
| NV-03 | Xong | Khóa `pg_advisory_xact_lock` trong `MaGenerator`; dòng sản phẩm và danh mục đều sinh mã trong cùng transaction với `INSERT` |
| NV-01 | Xong | Controller chặn + form khóa ô giá/thuế khi sửa nếu không phải Quản lý |
| NV-02 | Xong | Chỉ chặn ở `capNhat`. `themMay` **giữ nguyên** cho chọn trạng thái vì test hiện có khẳng định thêm máy `HANG_LOI` là hợp lệ |
| NV-05 | Xong | Chặn giá bán lẻ và giá nhập lẻ ở mọi lối vào, kể cả nhập Excel |
| NV-04 | Xong | Chặn mở lại hồ sơ đã kết thúc, chặn chuyển sang chính trạng thái hiện tại, chặn ghi lỗi cho máy đã thanh lý/trả NCC; "hồ sơ đang xử lý" (UC011 bước 9) gồm cả `DA_GUI_BAO_HANH`; khóa hồ sơ (`FOR UPDATE`) và kiểm lại trạng thái khi chuyển |
| NV-06, NV-09 | Xong | |
| NV-08 | Xong cho thêm khách | **Không áp cho `capNhatKhach`**: `KhachHangControllerTest` của nhóm yêu cầu cập nhật khách chạy được khi không lấy được kết nối riêng; đã thử và hoàn lại |
| NV-10 | Xong | Overload `thanhToan(…, tongDangHienThi)`; màn lập hóa đơn truyền tổng đang hiển thị và vẽ lại giỏ khi thất bại |
| UX-03 | Xong một phần | Thêm `DataAccessException.thongBao(loi, macDinh)`; áp cho `SanPham`/`DongSanPham`/`SanPhamLoi`/`DanhMuc` Controller và 6 chỗ ở `CategoriesPage`, `DefectiveProductsPage`, `PromotionsPage` (trước đó hiện cả tên lớp ngoại lệ). **Đính chính mức độ:** phần lớn `e.getMessage()` còn lại là thông điệp tiếng Việt do DAO bọc sẵn, không phải SQL thô; `KhuyenMaiController` và các controller báo cáo chưa đổi |
| UX-04, UX-06 | Xong | UX-06 giữ cụm "vừa được bán" vì test hiện có khẳng định cụm này |
| DB-01 | Xong | `V17__bo_sung_chi_muc.sql`; **đã được áp vào CSDL phát triển** khi chạy test |
| DB-02 | Xong | Tìm kiếm và giỏ hàng đọc theo dòng sản phẩm. **Không gộp truy vấn khuyến mãi:** mỗi dòng giỏ là một lần tra có chỉ mục (`idx_ctkm_dong_san_pham`), giỏ thường dưới 10 dòng; gộp lại phải viết lại logic ưu tiên của `SELECT_HIEU_LUC`, rủi ro lớn hơn lợi ích |
| DB-03 | Xong | Có test hai quầy thanh toán đồng thời (`haiQuayThanhToanDongThoiKhongTrungMay`) |
| DB-05 | Xong | `SanPhamDao.findByDongIn`; chưa thêm giới hạn số dòng trả về |
| DB-04 | **Bỏ** | `KiemTraHoSo.khop` so khớp **không dấu**; PostgreSQL chỉ làm được bằng extension `unaccent` (đổi hạ tầng). Giữ lọc trong RAM; bảng khách chỉ có 4 cột ngắn |
| NV-11 | **Chuyển sang §7** | `PhanTichTuanAnhTest` khẳng định "tổng chi tiêu" = tổng hóa đơn đã thanh toán; đổi nghĩa chỉ số là quyết định nghiệp vụ |
| NV-07 | Xong | `CurrentSession.yeuCauDangHoatDong()` ở mọi hàm ghi của `SanPham`/`DongSanPham`/`DanhMuc`/`SanPhamLoi` Controller; `thanhToan` yêu cầu Nhân viên bán hàng đang hoạt động. Không siết theo vai trò vì tác nhân UC008/UC010/UC011 chưa chốt (C-03) |
| DB-06 | Xong cho `ProductPage` | `SanPhamDao.timKiem` lọc + `LIMIT 200` tại CSDL, có gợi ý khi chạm giới hạn; các trang khác chưa đổi |
| UX-09 | Xong | |
| UX-01, UX-02 | **Bỏ** | Test của nhóm (`RetailInvoicePageTest`) khẳng định thao tác giỏ/lưu tạm chạy đồng bộ, và `BanHangController` không an toàn đa luồng. Sau DB-02/DB-05 mỗi thao tác chỉ còn 1–2 truy vấn có chỉ mục |
| UX-05 | **Chuyển sang §7** | UC003 và tài liệu phạm vi không nêu bước in hóa đơn |
| UX-07 | Xong (yêu cầu bổ sung 2026-11-16) | Mọi bảng danh sách có phân trang qua `ui/component/PagedLoader`. **Nạp dần tại CSDL** (`LIMIT/OFFSET` + `COUNT(*) OVER()`, mỗi trang một truy vấn, trang đã nạp giữ trong phiên): máy, dòng sản phẩm (2 màn), hồ sơ lỗi, khách hàng khi không có từ khóa. **Phân trang trên kết quả đã nạp một lần:** danh mục, khuyến mãi, nhân viên, phê duyệt, phiếu chờ hoàn tất, hóa đơn/phiếu của khách, tra cứu tồn kho, ba báo cáo, sao lưu, phục hồi. Trang đầu được nạp sẵn khi mở màn hình (`onShow`), tìm kiếm rỗng = duyệt toàn bộ. Không phân trang: giỏ hàng, bảng chi tiết của một chứng từ, hộp thoại, bảng tóm tắt ở trang chủ |
| Nhãn hiển thị | Xong (yêu cầu bổ sung 2026-11-16) | `ui/format/NhanHienThi`: trạng thái máy hiện tiếng Việt ở mọi bảng/ô chọn; ô chọn danh mục, dòng sản phẩm, nhà cung cấp chỉ hiện tên; cột "Dòng sản phẩm"/"Nhà cung cấp" của bảng máy hiện tên thay cho mã |
| Kiểm thử trên ứng dụng thật | Đã chạy 2026-11-16 | 27/27 bước pass: bán hàng (tìm dòng, giỏ, tổng lệch, sai vai trò, thanh toán qua giao diện, FIFO, `DA_BAN`, tồn kho), khóa ô giá/ô trạng thái, danh mục, vòng đời hồ sơ lỗi, hồ sơ lỗi của máy đã bán giữ `DA_BAN`, tổng chi tiêu ròng. Dữ liệu thử đã được dọn. Chưa thử tay: đổi/trả, nhập Excel (có test tự động) |
| Q-01…Q-09 | **Đã chốt 2026-11-16** | Xem §7; 4 mục đổi code (Q-03, Q-04, Q-06, Q-08), 5 mục ghi nhận quyết định |

---

## 1. Ngữ cảnh dự án (đọc trước)

| Mục | Giá trị |
|---|---|
| Sản phẩm | DTAQ — ứng dụng desktop quản lý bán hàng siêu thị điện máy (bán theo **từng máy / số serial**) |
| Công nghệ | Java 21, Swing + FlatLaf 3.6 + MigLayout, JDBC thuần + HikariCP, PostgreSQL, Flyway 13, JUnit 5, Apache POI |
| Build / test | `mvn -B clean test` (đúng lệnh CI ở `.github/workflows/build.yml`). Test cần DB **tự bỏ qua** khi không có PostgreSQL (`Assumptions.assumeTrue(ConnectDB.isAvailable())`) |
| Cấu hình DB | `db.properties` (không commit; mẫu ở `db.properties.example`) |
| Quy tắc bắt buộc cho agent | `AGENTS.md` ở gốc repo — **đọc toàn bộ**. Giao tiếp bằng tiếng Việt. Không commit/push/đổi nhánh khi chưa được yêu cầu |
| Tài liệu nghiệp vụ | `docs/reference/README.md` (mục lục) → `business-rules/rules-and-exceptions.md` (XBR-01…16), `database/schema-and-integrity.md` (RB01…RB66), `use-cases/UC0xx-*.md` |

### 1.1. Kiến trúc thực tế (giữ nguyên)

```
src/main/java/com/dtaq/retail/
  ui/page/<tính năng>/*Page.java   ← Swing, gọi controller (nên qua SwingWorker)
  ui/component, ui/shell, ui/theme ← design system nội bộ (DataTable, Pagination, Toast, ConfirmDialog, FormField, StatefulView…)
  controller/*Controller.java      ← nghiệp vụ + kiểm quyền + mở transaction (Connection) rồi truyền xuống DAO
  dao/*Dao.java, dao/impl/*Impl    ← SQL (PreparedStatement, hằng chuỗi SQL ở đầu lớp, tên bảng/cột bọc nháy kép)
  entity/                          ← record domain
  util/ (TienUtil, MaGenerator, ExcelImporter…), config/ (ConnectDB, DbMigration…), session/CurrentSession
src/main/resources/db/migration/V1…V16__*.sql   ← Flyway
```

Quy ước đang dùng — **phải theo đúng**:

- Trả kết quả nghiệp vụ bằng record `KetQua…(thanhCong, thongBao, …)`; lỗi quyền ném `SecurityException`; lỗi DB ném `DataAccessException`.
- Kiểm quyền bằng hàm private `yeuCauQuanLy()` / `yeuCauBanHang()` đọc `CurrentSession` (xem mẫu ở `GiaThueController`, `DoiTraController`).
- Transaction: controller lấy `Connection` từ `DataSource`, `setAutoCommit(false)`, gọi DAO nhận `Connection`, `commit`/`rollback` (mẫu: `BanHangController.ghiHoaDon`).
- Thêm hàm DAO mới: khai báo trong interface dưới dạng **`default` method** (như `SanPhamDao.findBySerial`) để các fake DAO trong test hiện có vẫn biên dịch được, rồi override trong `*Impl`.
- Tiền: chỉ `BigDecimal`, làm tròn duy nhất qua `TienUtil.lamTron` (VND, 0 chữ số thập phân, `HALF_UP`).
- Mã định danh: `MaGenerator.next(ketNoi, bảng, cột, tiềnTố)` → `TIỀN_TỐ-YYYY-NNNNNN`; không dùng sequence/identity.
- Tồn kho: `DongSanPham.soLuongTon` do **trigger `trg_sanpham_cap_nhat_ton` (V6)** tự tính lại mỗi khi `SanPham` đổi — không cập nhật tay.

### 1.2. Lược đồ (tóm tắt — KHÔNG thay đổi)

`NhanVien`, `KhachHang`, `DanhMucSanPham`, `NhaCungCap`, `DongSanPham`(giá, thuế, `soLuongTon`, `trangThaiKinhDoanh`), `SanPham`(PK `soSerial`, `trangThai`), `KhuyenMai` + `ChiTietKhuyenMai`, `HoaDon`(`ngayLap` kiểu DATE), `ChiTietHoaDon`(PK `maHD,soSerial`; 8 cột snapshot giá/thuế/KM), `PhieuTra`/`ChiTietPhieuTra`, `PhieuDoi`/`ChiTietPhieuDoi`, `SanPhamLoi`, `DatLaiMatKhau`, `LichSuSaoLuu`, `SuCoSucKhoe`.

Điểm quan trọng: ngoài PK/UNIQUE, **không có chỉ mục nào trên cột khóa ngoại hay cột lọc** (chỉ có `idx_ctkm_dong_san_pham`, 2 chỉ mục bảng sao lưu, 1 unique tên danh mục).

---

## 2. Ràng buộc cứng khi thực hiện plan

1. **Không đổi nghiệp vụ đã chốt** (D17 đổi hàng không hoàn/thu tiền; D24 nhân viên bán hàng được nhập Excel khuyến mãi; ngưỡng duyệt 3.000.000; bán theo serial; tiền mặt 100%…). Mọi mục cần đổi hành vi nằm ở **§7 — chỉ làm khi trưởng nhóm duyệt**.
2. **Không đổi thiết kế CSDL**: không thêm/xóa/sửa bảng, cột, kiểu, CHECK, FK, UNIQUE, trigger. Ngoại lệ duy nhất được phép trong plan: **một migration mới chỉ chứa `CREATE INDEX IF NOT EXISTS`** (§5, task DB-01) — chỉ mục không làm đổi mô hình dữ liệu. Không sửa migration V1–V16 đã phát hành.
3. **Không đổi hạ tầng codebase**: không thêm dependency vào `pom.xml`, không thêm tầng/pattern/framework mới (không ORM, không DI, không service layer mới), không đổi tên package/lớp, không đổi chữ ký hàm public đang có (thêm overload hoặc `default` method thay vì sửa).
4. **Mỗi task = một thay đổi nhỏ, tự kiểm chứng được**: sửa đúng các tệp liệt kê, thêm/cập nhật test cùng tệp test tương ứng trong `src/test/java/...`, chạy `mvn -B clean test`, không format lại code không liên quan.
5. Trước khi sửa một hàm dùng chung: `grep` mọi nơi gọi (kể cả test và fake DAO).
6. Baseline đã biết: `SanPhamControllerTest.capNhatMay_thanhCong` **đã fail trước** mọi thay đổi (lỗi dữ liệu). Chạy test trước khi bắt đầu để ghi lại baseline thực tế; không tính lỗi baseline vào thay đổi của mình, không "sửa" bằng cách xóa test.
7. Khi tài liệu và code mâu thuẫn → dừng, báo lại, không tự chọn (AGENTS.md §2).

---

## 3. Tổng hợp phát hiện

Mức độ: **P0** sai nghiệp vụ/toàn vẹn dữ liệu · **P1** hiệu năng hoặc rủi ro cao · **P2** trải nghiệm · **Q** cần quyết định.

| ID | Mức | Loại | Tóm tắt | Vị trí |
|---|---|---|---|---|
| NV-01 | P0 | Nghiệp vụ | Nhân viên bán hàng sửa được giá bán & thuế suất qua màn "Quản lý dòng sản phẩm" (vi phạm C-08/XBR-02) | `DongSanPhamController.capNhat` :144, `ProductLinesPage` :50-51 |
| NV-02 | P0 | Nghiệp vụ | Sửa tự do trạng thái máy (vd `DA_BAN`→`SAN_SANG_BAN`) ở màn "Quản lý sản phẩm" → bán lại máy đã bán, xuất kho không hóa đơn | `SanPhamController.capNhat` :124-174, `ProductPage` :56 |
| NV-03 | P0 | Toàn vẹn | Sinh mã `MAX+1` không khóa → 2 quầy thanh toán cùng lúc trùng `maHD`, một bên lỗi "Không thể ghi hóa đơn" | `MaGenerator.next` :76; `DongSanPhamController.sinhMaDongMoi` :348 (sinh mã ngoài transaction) |
| NV-04 | P0 | Nghiệp vụ | Hồ sơ lỗi: chuyển trạng thái không có máy trạng thái (THANH_LY→CHO_XU_LY được), ghi đè mù trạng thái máy, cho ghi lỗi máy đã thanh lý, cho 2 hồ sơ mở cùng máy | `SanPhamLoiController` :99-190, :199-260 |
| NV-05 | P0 | Dữ liệu | Giá có phần thập phân (nhập qua Excel) làm sập tra cứu/giỏ hàng (`longValueExact`) và bị nhân 10/100 khi mở form sửa (ô tiền bỏ dấu chấm) | `DongSanPhamController.nhapDanhSach`; `RetailInvoicePage` :234,:259; `ProductLinesPage` :282 |
| NV-06 | P1 | Nghiệp vụ | Đổi hàng: không kiểm máy thay thế thuộc dòng **đang kinh doanh** (RB35) | `DoiTraController.tinhPhieu` :264 |
| NV-07 | P1 | Phân quyền | 6 controller không kiểm phiên/quyền tại tầng nghiệp vụ (XBR-13) | `BanHang`, `SanPham`, `DongSanPham`, `DanhMuc`, `SanPhamLoi`, `TraCuuSanPham` Controller |
| NV-08 | P1 | Toàn vẹn | Tạo khách trùng SĐT khi 2 quầy thêm cùng lúc (kiểm-rồi-ghi, không khóa; DB không UNIQUE) | `KhachHangController.themKhach` :110 |
| NV-09 | P1 | Toàn vẹn | Hai phiếu trả chờ duyệt cho cùng một máy khi lập đồng thời (guard đọc trước không khóa) | `PhieuTraDaoImpl` :358, `PhieuDoiDaoImpl` :386 |
| NV-10 | P1 | Nghiệp vụ | Giá/KM đổi giữa lúc hiển thị và lúc bấm thanh toán → thu số tiền khác số trên màn hình, không cảnh báo | `BanHangController.thanhToan` :421-427 |
| NV-11 | P2 | Số liệu | "Tổng chi tiêu" của khách không trừ hàng đã trả | `TraCuuKhachHangController.tongQuan` :92 |
| DB-01 | P1 | CSDL | Thiếu chỉ mục cho FK/cột lọc; trigger tồn kho quét toàn bảng `SanPham` cho **mỗi dòng** thay đổi (nhập Excel 5.000 máy = 5.000 lần quét) | migrations |
| DB-02 | P1 | Truy vấn | Màn bán hàng nạp **toàn bộ** `SanPham` (JOIN 3 bảng) cho mỗi lần gõ tìm, thêm dòng, đổi số lượng, bỏ dòng, tính tiền; kèm N+1 truy vấn khuyến mãi | `BanHangController.docMay` :673, `timKhuyenMai` :336 |
| DB-03 | P1 | Truy vấn | Phân bổ FIFO ngoài transaction → va chạm "máy vừa được bán" khi 2 quầy bán cùng dòng | `BanHangController.phanBoMay` :361 |
| DB-04 | P1 | Truy vấn | Danh sách/tra cứu khách: nạp toàn bộ `KhachHang` rồi lọc + phân trang trong RAM | `KhachHangController` :66, `TraCuuKhachHangController` :77 |
| DB-05 | P1 | Truy vấn | Tra cứu tồn kho: N+1 (`findByDong` cho từng dòng), tìm theo danh mục nạp toàn bộ dòng sản phẩm | `TraCuuSanPhamController` :69,:76,:107 |
| DB-06 | P2 | Truy vấn | Màn quản lý sản phẩm / nhân viên / khuyến mãi / giá-thuế nạp toàn bảng, không phân trang ở DB | `ProductPage` :162, `NhanVienController` :108, `PriceTaxPage` :106,:119 |
| UX-01 | P1 | UX | Thao tác giỏ hàng, lưu/mở hóa đơn tạm chạy DB trên EDT → đơ giao diện | `RetailInvoicePage` :202-204, :296, :318 |
| UX-02 | P1 | UX | `ProductLookupPage`, `PriceTaxPage` không dùng `SwingWorker` (DB trên EDT) | 2 tệp trên |
| UX-03 | P1 | UX | Lộ thông báo lỗi kỹ thuật (SQL/exception) ra Toast và ô lỗi | xem §6 UX-03 |
| UX-04 | P1 | UX | Trang chủ Quản lý chỉ hiện phiếu chờ duyệt **lập hôm nay** — phiếu tồn từ hôm trước bị ẩn khỏi cảnh báo | `AppNavigator` :365 |
| UX-05 | P2 | UX | Phiếu hóa đơn sau thanh toán chỉ có nút "Đóng hóa đơn": không in/lưu, không mở lại được | `InvoiceReceiptDialog` :78 |
| UX-06 | P2 | UX | Thông báo "Máy … vừa được bán, vui lòng chọn lại" — thu ngân không chọn máy (hệ thống tự phân bổ) | `BanHangController` :518 |
| UX-07 | P2 | UX | Bảng lớn không phân trang (chỉ 3 trang dùng `Pagination`) | ProductPage, PromotionsPage, DefectiveProductsPage, ProductLinesPage |
| UX-08 | P2 | UX | Ô trạng thái máy cho chọn cả 6 trạng thái (gốc của NV-02) | `ProductPage` :56 |
| UX-09 | P2 | UX | Cảnh báo "đã điều chỉnh/bỏ dòng" khi mở hóa đơn tạm hiển thị bằng Toast lỗi và chỉ có mã dòng | `RetailInvoicePage` :325 |
| Q-01…Q-07 | Q | Quyết định | Xem §7 | — |

**Đã kiểm tra và đúng — không sửa:** snapshot giá/thuế/KM trên `ChiTietHoaDon`; làm tròn theo từng máy rồi cộng; thanh toán trong một transaction; `danhDauDaBan` có điều kiện chống bán trùng; duyệt/hoàn tất phiếu dùng `UPDATE … WHERE trạng thái cũ` (chống xử lý lặp); báo cáo doanh thu dùng giá trước thuế và chỉ lấy phiếu `HOAN_TAT`; kiểm chồng lấn khuyến mãi khi tạo và khi kích hoạt lại; nhập Excel khuyến mãi bởi nhân viên bán hàng (D24 — đã duyệt); đổi hàng không hoàn tiền (D17).

---

## 4. Giai đoạn 1 — Lỗi nghiệp vụ & toàn vẹn (P0 → P1)

Thứ tự thực hiện: NV-03 → NV-01 → NV-02 → NV-05 → NV-04 → NV-06 → NV-08 → NV-09 → NV-10 → NV-07 → NV-11.

### NV-03 — Chống trùng mã khi sinh đồng thời

**Vấn đề.** `MaGenerator.next` đọc `MAX(...)+1` rồi bên gọi mới `INSERT`. Ở mức cô lập mặc định (READ COMMITTED) hai transaction cùng đọc ra một số → transaction thứ hai vi phạm PK và rollback cả hóa đơn. Không có `advisory lock`/`FOR UPDATE` ở bất kỳ đâu trong repo (đã grep). Ảnh hưởng mọi bảng dùng `MaGenerator`: `HoaDon`, `PhieuTra`, `PhieuDoi`, `SanPhamLoi`, `KhachHang`, `KhuyenMai`, `DongSanPham`…

**Sửa (không đổi schema, không đổi chữ ký).**
1. Trong `util/MaGenerator.next(...)`, **trước** câu `SELECT MAX`, chạy trên cùng `ketNoi`:
   `SELECT pg_advisory_xact_lock(hashtext(?))` với tham số là tên bảng (`bang`). Khóa tự nhả khi transaction commit/rollback.
2. Khóa chỉ có tác dụng khi bên gọi **đang trong transaction** (`autoCommit=false`). Rà mọi nơi gọi `MaGenerator.next` (`grep -rn "MaGenerator.next"`), sửa nơi gọi ngoài transaction:
   - `DongSanPhamController.sinhMaDongMoi()` (:348) mở kết nối riêng, autocommit → **chuyển việc sinh mã vào trong khối transaction của `themMoi`** (ngay trước `dongSanPhamDao.insert(connection, …)`), giữ nguyên nhánh fallback `dataSource == null` dùng cho test.
   - Các nơi khác: xác nhận đã `setAutoCommit(false)` trước khi gọi.
3. Kiểm `MaGeneratorTest` (và test dùng `Connection` giả): nếu test mock `prepareStatement`, cập nhật để chấp nhận thêm câu lệnh khóa.

**Nghiệm thu.** Test DB mới (tự bỏ qua khi không có DB): 2 luồng, mỗi luồng mở transaction, gọi `MaGenerator.next` cho cùng bảng, chèn bản ghi, commit → hai mã **khác nhau**, không ngoại lệ. Toàn bộ test cũ vẫn như baseline.

### NV-01 — Chỉ Quản lý được đổi giá bán / thuế suất

**Vấn đề.** Quyết định C-08 (2026-10-25): chỉ Quản lý đổi `donGiaBanTruocThue`/`thue` (UC019, `GiaThueController` đã kiểm quyền đúng). Nhưng trang "Quản lý dòng sản phẩm" có trong menu **nhân viên bán hàng** (`NavigationModel`), form có ô giá/thuế sửa được, và `DongSanPhamController.capNhat` ghi thẳng cả hai giá trị, không kiểm quyền. Quyết định UC009 (2026-11-04) chỉ cho nhân viên bán hàng *tạo, ngừng, kinh doanh lại* dòng — không gồm sửa giá.

**Sửa.**
1. `DongSanPhamController.capNhat(...)`: nếu người dùng hiện tại **không phải** `NHAN_VIEN_QUAN_LY` và (`donGiaBanTruocThue` hoặc `thue` khác giá trị của `dongCu`, so bằng `compareTo`) → trả `KetQua.loi("Chỉ Quản lý được thay đổi giá bán và thuế suất")`. Các trường khác vẫn cập nhật bình thường. Đọc quyền qua `CurrentSession.get()` theo mẫu `GiaThueController.yeuCauQuanLy` (nhưng trả `KetQua.loi`, không ném, để form hiển thị được).
2. `ProductLinesPage`: khi đang **sửa** một dòng và người dùng không phải Quản lý → `priceField`/`taxField` chỉ đọc (dùng API sẵn có của `FormField`, vd `getInputComponent().setEnabled(false)`), kèm gợi ý "Giá và thuế do Quản lý cập nhật tại mục Cập nhật giá bán và thuế suất". Khi **thêm mới**: giữ nguyên hành vi hiện tại (xem Q-01).
3. Không đụng `GiaThueController`, `PriceTaxPage`.

**Nghiệm thu.** `DongSanPhamControllerTest`: (a) phiên bán hàng đổi giá → lỗi, DB/DAO không bị gọi `update`; (b) phiên bán hàng đổi tên, giữ nguyên giá/thuế → thành công; (c) phiên Quản lý đổi giá → thành công. `ProductLinesPageTest` hiện có vẫn pass (cập nhật nếu test đặt phiên).

### NV-02 — Khóa việc sửa tay trạng thái máy

**Vấn đề.** `ProductPage` có `JComboBox<TrangThaiSanPham>` đủ 6 giá trị; `SanPhamController.capNhat` ghi nguyên trạng thái người dùng chọn. Hệ quả: đưa máy `DA_BAN` về `SAN_SANG_BAN` (bán lần hai một máy đã thuộc về khách, tồn kho ảo), hoặc `SAN_SANG_BAN`→`DA_BAN` không có hóa đơn (thất thoát không dấu vết), bỏ qua toàn bộ luồng bán/đổi-trả/sản phẩm lỗi (XBR-04, RB38–RB39).

**Sửa.**
1. `SanPhamController.capNhat`: trạng thái là dữ liệu do **luồng nghiệp vụ** quản lý. Quy tắc: nếu `trangThai` truyền vào khác trạng thái hiện tại của máy → `KetQua.loi("Trạng thái máy chỉ thay đổi qua bán hàng, đổi/trả hoặc xử lý sản phẩm lỗi")`. Ngoài ra, nếu máy hiện **không** ở `SAN_SANG_BAN` thì không cho đổi `maDongSP` (máy đã có lịch sử; FK ghép từ `ChiTietHoaDon` cũng sẽ chặn nhưng bằng lỗi SQL thô).
2. `ProductPage`: khi sửa máy → `statusBox` chỉ đọc; khi thêm máy → cố định `SAN_SANG_BAN` (đúng với spec nhập Excel hiện tại chỉ cho `SAN_SANG_BAN`).
3. `SanPhamController.themMoi`: nếu đang cho chọn trạng thái khác `SAN_SANG_BAN` khi thêm → chặn tương tự (kiểm tra code quanh :105 trước khi sửa).

**Nghiệm thu.** `SanPhamControllerTest`: đổi trạng thái qua `capNhat` → lỗi; đổi `giaNhap`/`maNCC` giữ nguyên trạng thái → thành công. `ProductPageTest` cập nhật tương ứng.

### NV-05 — Giá bán phải là số nguyên VND

**Vấn đề.** Quy tắc C-11: tiền VND 0 chữ số thập phân. `DongSanPhamController.themMoi/capNhat/nhapDanhSach` và `GiaThueController.capNhat` chỉ kiểm `>= 0`/`> 0`. Cột Excel "Giá bán trước thuế" kiểu `SO_THAP_PHAN` nên nhận `1999000.5`. Khi đó: `RetailInvoicePage` gọi `donGiaTruocThue().longValueExact()` (:234, :259) → `ArithmeticException` → **toàn bộ kết quả tìm kiếm ở quầy báo "Không thể tra cứu sản phẩm"**; `ProductLinesPage` :282 đặt `toPlainString()` vào ô tiền (bộ lọc chỉ giữ chữ số) → `1999000.5` thành `19990005`, lưu lại là sai giá gấp 10.

**Sửa.**
1. Thêm kiểm tra dùng chung trong `DongSanPhamController` (hàm private static) và gọi ở `themMoi`, `capNhat`, `nhapDanhSach`; lặp lại ở `GiaThueController.capNhat`: `gia.stripTrailingZeros().scale() > 0` → lỗi "Giá bán trước thuế phải là số nguyên (VND)". Áp dụng tương tự cho `giaNhap` trong `SanPhamController` (thêm/sửa/nhập Excel).
2. Phòng thủ ở UI (không thay cho bước 1): `RetailInvoicePage` dùng `TienUtil.lamTron(x).longValueExact()` tại :234, :259; `ProductLinesPage` :282 dùng `TienUtil.lamTron(...).toPlainString()`.
3. Không sửa dữ liệu sẵn có bằng migration. Nếu nghi DB đã có giá lẻ, báo lại kèm câu kiểm tra: `SELECT "maDongSP","donGiaBanTruocThue" FROM "DongSanPham" WHERE "donGiaBanTruocThue" <> trunc("donGiaBanTruocThue");`

**Nghiệm thu.** Test controller: giá `1000.5` bị từ chối ở cả 4 lối vào; `1000.00` được chấp nhận. Test nhập Excel có một dòng giá lẻ → cả tệp bị từ chối, không ghi dòng nào.

### NV-04 — Máy trạng thái cho hồ sơ sản phẩm lỗi

**Vấn đề** (`SanPhamLoiController`):
- `chuyenTrangThai` (:199) nhận mọi `trangThaiMoi`, kể cả lùi từ trạng thái kết thúc (`THANH_LY`/`DA_TRA_NHA_CUNG_CAP` → `CHO_XU_LY`) hoặc chuyển sang chính trạng thái hiện tại, rồi **ghi đè vô điều kiện** `SanPham.trangThai` (:262). Máy đã thanh lý có thể "sống lại" thành `HANG_LOI`.
- `ghiNhanLoi` (:99) cho ghi lỗi với máy `DA_THANH_LY`/`DA_TRA_NHA_CUNG_CAP`; chỉ chặn trùng khi hồ sơ cũ đang `CHO_XU_LY` (máy có hồ sơ `DA_GUI_BAO_HANH` vẫn mở thêm hồ sơ thứ hai).
- Lộ `e.getMessage()` thô (xem UX-03).

**Sửa (giữ nguyên enum & CHECK của DB).**
1. Trong `chuyenTrangThai`, trước khi mở transaction, kiểm chuyển tiếp hợp lệ theo bảng dưới; sai → `KetQua.loi("Không thể chuyển hồ sơ từ <cũ> sang <mới>")`:

   | Từ | Được sang |
   |---|---|
   | `CHO_XU_LY` | `DA_GUI_BAO_HANH`, `DA_TRA_NHA_CUNG_CAP`, `THANH_LY` |
   | `DA_GUI_BAO_HANH` | `CHO_XU_LY` (bảo hành trả về, chưa xử lý xong), `DA_TRA_NHA_CUNG_CAP`, `THANH_LY` |
   | `DA_TRA_NHA_CUNG_CAP`, `THANH_LY` | (kết thúc — không chuyển tiếp) |

   Bảng này là **đề xuất tối thiểu** suy ra từ ý nghĩa trạng thái; UC011 (`docs/reference/use-cases/UC011-defective-product-management.md`) là nguồn quyết định — đọc trước, nếu UC011 quy định khác thì theo UC011 và ghi chú lại.
2. Cập nhật có điều kiện để chống hai người cùng xử lý: thêm vào `SanPhamLoiDao` một `default` method `updateTinhTrang(ketNoi, maLoi, trangThaiCu, trangThaiMoi, ghiChu, ngay)` trả `boolean`, SQL thêm `AND "tinhTrangXuLy" = ?`; `0` dòng → rollback và báo "Hồ sơ đã được người khác cập nhật".
3. `ghiNhanLoi`: từ chối khi máy đang `DA_THANH_LY` hoặc `DA_TRA_NHA_CUNG_CAP`; kiểm trùng theo "hồ sơ chưa kết thúc" (`CHO_XU_LY` **hoặc** `DA_GUI_BAO_HANH`) — thêm `default` method DAO hoặc mở rộng SQL của `findDangXuLy` (kiểm mọi nơi gọi trước khi đổi ngữ nghĩa).
4. Máy `DA_BAN` (máy của khách): xem Q-06 — **chưa đổi hành vi**.

**Nghiệm thu.** `SanPhamLoiControllerTest`: mỗi chuyển tiếp hợp lệ/không hợp lệ một ca; ghi lỗi máy đã thanh lý bị từ chối; hồ sơ thứ hai khi hồ sơ đầu `DA_GUI_BAO_HANH` bị từ chối.

### NV-06 — Máy thay thế phải thuộc dòng đang kinh doanh (RB35)

`DoiTraController.tinhPhieu` (:264) chỉ kiểm `SAN_SANG_BAN`. Sau khi lấy `dongMoi` (qua `dongSanPhamDayDu`), thêm: `!dongMoi.trangThaiKinhDoanh()` → `tinhKhongHopLe("Máy thay thế thuộc dòng sản phẩm đã ngừng kinh doanh")`. Thêm 1 ca vào `DoiTraControllerTest`.

### NV-08 — Không tạo khách trùng số điện thoại

`KhachHangController.themKhach` (:110) kiểm `findBySdt` **ngoài** transaction rồi mới ghi. Sửa trong `ghiKhachMoi(ketNoi, …)`: sau `setAutoCommit(false)`, chạy `SELECT pg_advisory_xact_lock(hashtext(?))` với khóa `"KH:" + sdt`, **kiểm lại** trùng SĐT trên chính `ketNoi` (thêm `default` method DAO `findBySdt(Connection, String)`), nếu trùng → rollback và trả `KetQuaLuuKhach.trung(...)`. Áp dụng cùng cách cho `capNhatKhach` khi đổi SĐT. Không thêm UNIQUE vào DB.

### NV-09 — Không lập hai phiếu cho cùng một máy

`PhieuTraDaoImpl.taoPhieuTra` (:358) và `PhieuDoiDaoImpl.taoPhieuDoi` (:386) kiểm "chưa có phiếu hiệu lực" bằng `SELECT` thường; hai transaction đồng thời cùng qua. Với phiếu ≥ 3.000.000 (chờ duyệt, chưa đụng `SanPham`) sẽ tồn tại 2 phiếu `CHO_DUYET` cho một máy. Sửa: ngay đầu hai hàm, khóa dòng chi tiết hóa đơn gốc trên `ketNoi`:
`SELECT 1 FROM "ChiTietHoaDon" WHERE "maHD" = ? AND "soSerial" = ? FOR UPDATE` (mỗi dòng chi tiết phiếu một lần), rồi mới chạy guard hiện có. Không đổi guard, không đổi chữ ký.

### NV-10 — Tổng tiền thay đổi giữa hiển thị và thanh toán

`thanhToan` tính lại giá/KM tại thời điểm bấm; nếu Quản lý vừa đổi giá hoặc KM vừa hết hạn, tiền thu khác tiền trên màn hình mà thu ngân không biết (chỉ so `tienKhachDua >= tổng mới`).
Sửa: thêm **overload** `thanhToan(nhanVien, khachHang, tienKhachDua, long tongDangHienThi)`; overload cũ giữ nguyên hành vi (gọi overload mới với giá trị "không kiểm"). Trong overload mới, nếu tổng tính lại ≠ `tongDangHienThi` → `thatBai("Giá hoặc khuyến mãi vừa thay đổi. Tổng mới: <TienUtil.dinhDang>. Vui lòng kiểm tra lại trước khi thanh toán")`, **không** ghi gì. `RetailInvoicePage.xuLyThanhToan` truyền `totals` đang hiển thị và, khi thất bại vì lý do này, gọi lại `hoaDonHienTai()` để vẽ lại giỏ.

### NV-07 — Kiểm phiên/quyền tại tầng nghiệp vụ (XBR-13)

Sáu controller không kiểm gì: `BanHangController`, `SanPhamController`, `DongSanPhamController`, `DanhMucController`, `SanPhamLoiController`, `TraCuuSanPhamController`. Tác nhân UC008/UC010/UC011 **chưa chốt** (C-03) nên không siết theo vai trò ở đây. Sửa tối thiểu: ở các hàm **ghi** của 6 controller này, yêu cầu đã đăng nhập và tài khoản đang hoạt động (`CurrentSession.get()` có giá trị và `trangThaiHoatDong()`), nếu không → `SecurityException("Cần đăng nhập để thực hiện thao tác")`. Riêng `BanHangController.thanhToan`: yêu cầu `NHAN_VIEN_BAN_HANG` (khớp `NavigationModel` và `DoiTraController`) và `nhanVien` truyền vào trùng `maNV` với phiên.
Lưu ý: các test controller hiện có có thể chưa đặt `CurrentSession` → thêm `CurrentSession.set(...)` trong `@BeforeEach` và `CurrentSession.clear()` trong `@AfterEach` (xem mẫu ở `DoiTraControllerTest`).

### NV-11 — Tổng chi tiêu của khách phải trừ hàng đã trả

`TraCuuKhachHangController.tongQuan` (:92) cộng mọi hóa đơn đã thanh toán. Sửa trong `KhachHangGiaoDichDaoImpl`: thêm một truy vấn tổng giá trị (`tienSauKhuyenMai + tienThue`) của các chi tiết hóa đơn thuộc phiếu trả `HOAN_TAT` của khách (JOIN `PhieuTra`→`ChiTietPhieuTra`→`ChiTietHoaDon`→`HoaDon` theo `hd."khachHang" = ?`), trả về qua `default` method DAO; `tongQuan` nhận thêm tham số qua **overload** và hiển thị "Tổng chi tiêu (đã trừ trả hàng)". Phiếu đổi không trừ (D17: không hoàn tiền).

---

## 5. Giai đoạn 2 — Tối ưu truy vấn CSDL

### DB-01 — Migration chỉ mục `V17__bo_sung_chi_muc.sql`

Tạo tệp mới `src/main/resources/db/migration/V17__bo_sung_chi_muc.sql` (kiểm tra số phiên bản lớn nhất hiện có trước; nếu đã có V17 thì dùng số kế tiếp). **Chỉ** chứa `CREATE INDEX IF NOT EXISTS`, mỗi chỉ mục kèm chú thích tiếng Việt nêu truy vấn được phục vụ, theo văn phong các migration hiện có:

```sql
-- Trigger tồn kho (V6), phân bổ FIFO khi bán, tra cứu máy theo dòng
CREATE INDEX IF NOT EXISTS idx_sanpham_dong_trangthai_ngaynhap
  ON "SanPham" ("maDongSP", "trangThai", "ngayNhapKhoBanDau", "soSerial");
-- Báo cáo tồn kho lâu ngày, đếm máy tồn lâu ở trang chủ
CREATE INDEX IF NOT EXISTS idx_sanpham_trangthai_ngaynhap
  ON "SanPham" ("trangThai", "ngayNhapKhoBanDau");
-- Thống kê theo kỳ, trang chủ theo ngày/nhân viên
CREATE INDEX IF NOT EXISTS idx_hoadon_ngaylap_nhanvien
  ON "HoaDon" ("ngayLap", "nhanVienLap");
-- Lịch sử mua của khách
CREATE INDEX IF NOT EXISTS idx_hoadon_khachhang_ngaylap
  ON "HoaDon" ("khachHang", "ngayLap" DESC);
-- Thống kê doanh số theo dòng sản phẩm
CREATE INDEX IF NOT EXISTS idx_chitiethoadon_dong ON "ChiTietHoaDon" ("maDongSP");
-- Truy lịch sử bán theo serial; hỗ trợ FK tới SanPham
CREATE INDEX IF NOT EXISTS idx_chitiethoadon_serial ON "ChiTietHoaDon" ("soSerial");
-- Phiếu theo hóa đơn gốc / theo khách
CREATE INDEX IF NOT EXISTS idx_phieutra_hoadon ON "PhieuTra" ("hoaDon");
CREATE INDEX IF NOT EXISTS idx_phieudoi_hoadon ON "PhieuDoi" ("hoaDon");
-- Điều chỉnh doanh thu theo ngày hoàn tất
CREATE INDEX IF NOT EXISTS idx_phieutra_ngayhoantat ON "PhieuTra" ("ngayHoanTat")
  WHERE "trangThaiXuLyPhieu" = 'HOAN_TAT';
CREATE INDEX IF NOT EXISTS idx_phieudoi_ngayhoantat ON "PhieuDoi" ("ngayHoanTat")
  WHERE "trangThaiXuLyPhieu" = 'HOAN_TAT';
-- Danh sách chờ duyệt / chờ hoàn tất
CREATE INDEX IF NOT EXISTS idx_phieutra_dangmo ON "PhieuTra" ("trangThaiDuyet", "ngayTao")
  WHERE "trangThaiXuLyPhieu" = 'CHO_XU_LY';
CREATE INDEX IF NOT EXISTS idx_phieudoi_dangmo ON "PhieuDoi" ("trangThaiDuyet", "ngayTao")
  WHERE "trangThaiXuLyPhieu" = 'CHO_XU_LY';
-- Guard "máy đã đổi/trả", JOIN chi tiết phiếu ↔ chi tiết hóa đơn
CREATE INDEX IF NOT EXISTS idx_chitietphieutra_cthd ON "ChiTietPhieuTra" ("maHD", "soSerial");
CREATE INDEX IF NOT EXISTS idx_chitietphieudoi_cthd ON "ChiTietPhieuDoi" ("maHD", "soSerial");
-- Tra khách theo số điện thoại (không UNIQUE: giữ nguyên thiết kế)
CREATE INDEX IF NOT EXISTS idx_khachhang_sodienthoai ON "KhachHang" ("soDienThoai");
-- Hồ sơ lỗi theo máy / theo trạng thái
CREATE INDEX IF NOT EXISTS idx_sanphamloi_serial_tinhtrang ON "SanPhamLoi" ("soSerial", "tinhTrangXuLy");
-- Dòng sản phẩm theo danh mục
CREATE INDEX IF NOT EXISTS idx_dongsanpham_danhmuc ON "DongSanPham" ("maDanhMuc");
```

Việc phải làm kèm:
- `grep -rn "V16\|migration" src/test/java/com/dtaq/retail/config` — các test `DbMigrationTest`, `SeedMigrationContractTest`, `ChiTietKhuyenMaiMigrationTest` có thể khẳng định danh sách/số lượng migration; cập nhật đúng chỗ cần.
- Cập nhật `docs/reference/database/schema-and-integrity.md` §6.1: thêm một ghi chú ngắn "V17 bổ sung chỉ mục, không đổi bảng/cột/ràng buộc".
- Nếu có PostgreSQL: chạy `EXPLAIN (ANALYZE, BUFFERS)` cho 3 truy vấn — đếm tồn trong trigger (`SELECT count(*) FROM "SanPham" WHERE "maDongSP"=… AND "trangThai"='SAN_SANG_BAN'`), `SELECT_DA_THANH_TOAN_TRONG_KY`, `SELECT_HOA_DON_CUA_KHACH` — ghi kết quả trước/sau vào báo cáo task. Không có DB → ghi rõ "chưa đo".
- **Không** thêm `pg_trgm`/extension (cần quyền superuser, đổi hạ tầng). Tìm kiếm `ILIKE '%…%'` chấp nhận quét tuần tự có `LIMIT`.

### DB-02 — Màn bán hàng: truy vấn theo dòng sản phẩm thay vì nạp toàn bộ máy

**Vấn đề.** `BanHangController.docMay()` (:673) = `sanPhamDao.findAll()` — toàn bộ `SanPham` JOIN `NhaCungCap`, `DongSanPham`, `DanhMucSanPham`, kể cả máy đã bán nhiều năm. Được gọi trong `timDongSanPham` (mỗi lần gõ, debounce 300 ms), `themDong`, `datSoLuong`, `boDong`→`hoaDonHienTai`, `luuBanTam`, `khoiPhucBanTam`, `thanhToan`. Thêm `timKhuyenMai` chạy 1 truy vấn cho **mỗi dòng giỏ** mỗi lần tính. Chi phí tăng theo tổng số máy từng nhập.

**Sửa.** `DongSanPham.soLuongTon` đã luôn đúng nhờ trigger V6 → không cần đếm lại từ `SanPham`.
1. `DongSanPhamDao` thêm 2 `default` method (override trong `DongSanPhamImpl`):
   - `List<DongSanPham> timDangBan(String tuKhoa, int gioiHan)` —
     `… FROM "DongSanPham" WHERE "trangThaiKinhDoanh" = TRUE AND "soLuongTon" > 0 AND ("maDongSP" ILIKE ? OR "tenDongSP" ILIKE ?) ORDER BY "maDongSP" LIMIT ?` (tham số `%tuKhoa%`, nhớ escape `%`/`_`/`\` trong từ khóa).
   - `List<DongSanPham> findByMaIn(Collection<String> maDongSP)` — `WHERE "maDongSP" = ANY (?)` (dùng `connection.createArrayOf("varchar", …)`).
   Cài đặt `default` trong interface = lọc từ `findAll()` để fake DAO cũ vẫn chạy.
2. `KhuyenMaiDao` thêm `default Map<String, KhuyenMai> findHieuLucTheoDong(Collection<String> maDongSP, LocalDate ngay)` — một truy vấn cho cả giỏ, giữ đúng thứ tự ưu tiên hiện có của `SELECT_HIEU_LUC` (`tyLeGiam DESC, ngayBatDau DESC, maKM ASC`), lấy bản đầu tiên cho mỗi dòng. `default` = lặp `findHieuLuc` hiện có.
3. `BanHangController`: thêm phụ thuộc `DongSanPhamDao` qua **constructor mới** (giữ 2 constructor cũ; khi `dongSanPhamDao == null` thì chạy đường cũ `docMay()` để `BanHangControllerTest` không vỡ). Khi có `dongSanPhamDao`:
   - `timDongSanPham` → `timDangBan(tuKhoa, 20)`.
   - `themDong`/`datSoLuong`/`tinhHoaDon`/`luuBanTam`/`khoiPhucBanTam` → `findByMaIn(các mã trong giỏ ∪ mã đang thao tác)`; `tonKho` lấy từ `soLuongTon`.
   - `tinhHoaDon` → `findHieuLucTheoDong` một lần.
4. `AppNavigator.salesPages` truyền `dongSanPhamDao` (đã có sẵn biến) vào constructor mới.
5. **Không** đổi công thức `tinhDong`, không đổi record trả về.

**Nghiệm thu.** `BanHangControllerTest` cũ pass nguyên trạng (đường cũ). Thêm test cho đường mới với fake `DongSanPhamDao` đếm số lần gọi: thêm 1 dòng vào giỏ 3 dòng → **0** lần `SanPhamDao.findAll()`, ≤ 1 lần `findByMaIn`, ≤ 1 lần truy vấn khuyến mãi. Tổng tiền của cùng một giỏ ở hai đường bằng nhau.

### DB-03 — Phân bổ FIFO trong transaction thanh toán

**Vấn đề.** `phanBoMay` chọn serial từ snapshot đọc trước transaction; hai quầy bán cùng dòng sẽ chọn **cùng** máy cũ nhất → quầy sau rollback với "Máy … vừa được bán".

**Sửa.** `SanPhamDao` thêm `default List<SanPham> chonMayDeBan(Connection ketNoi, String maDongSP, int soLuong)`; `SanPhamImpl` cài bằng (`SELECT_CO_JOIN` hiện có +)
`WHERE sp."maDongSP" = ? AND sp."trangThai" = 'SAN_SANG_BAN' AND dsp."trangThaiKinhDoanh" = TRUE ORDER BY sp."ngayNhapKhoBanDau", sp."soSerial" LIMIT ? FOR UPDATE OF sp SKIP LOCKED`.
Trong `ghiHoaDon` (đường mới của DB-02): với mỗi dòng giỏ gọi `chonMayDeBan` **bên trong** transaction; số máy trả về < số lượng → rollback, `thatBai("Dòng sản phẩm <mã> không đủ máy để bán")`. Giữ `danhDauDaBan` có điều kiện làm chốt chặn cuối. Thứ tự FIFO (ngày nhập tăng dần, rồi serial) **giữ đúng** như `phanBoMay` hiện tại. `phanBoMay()` public giữ nguyên cho nơi khác/test.

**Nghiệm thu.** Test DB (tự bỏ qua khi không có DB): 2 luồng cùng thanh toán 1 máy của một dòng có ≥ 2 máy → cả hai thành công với 2 serial khác nhau; dòng chỉ còn 1 máy → đúng một bên thành công, bên kia nhận "không đủ máy", không có dữ liệu ghi dở.

### DB-04 — Khách hàng: tìm kiếm & phân trang tại DB

`KhachHangController` (:66) và `TraCuuKhachHangController` (:77) gọi `khachHangDao.findAll()` rồi lọc bằng `KiemTraHoSo.khop`. Thêm vào `KhachHangDao` hai `default` method: `timKiem(String tuKhoa, int gioiHan, int boQua)` (`WHERE "soDienThoai" ILIKE ? OR "hoTen" ILIKE ? OR "maKH" ILIKE ? ORDER BY "maKH" LIMIT ? OFFSET ?`) và `demTimKiem(String tuKhoa)`. Hai controller chuyển sang dùng; giữ nguyên record `TrangKhachHang` và thứ tự "khớp đúng SĐT lên đầu" (ORDER BY `("soDienThoai" = ?) DESC, "maKH"`).
**Chú ý tương đương hành vi:** đọc `KiemTraHoSo.khop` trước — nếu nó so khớp không dấu/chuẩn hóa mà `ILIKE` không tái hiện được, **giữ lọc trong RAM cho phần tên** và chỉ đẩy xuống DB nhánh tìm theo SĐT/mã; ghi rõ lựa chọn trong báo cáo. Không được làm đổi kết quả tìm kiếm mà test hiện có đang khẳng định.

### DB-05 — Tra cứu sản phẩm & tồn kho: bỏ N+1

`TraCuuSanPhamController`: mỗi dòng sản phẩm khớp → 1 truy vấn `findByDong` (JOIN 3 bảng); tìm theo danh mục nạp toàn bộ dòng sản phẩm. Sửa: thêm `SanPhamDao.findByDongIn(Collection<String> maDongSP)` (`default` = lặp `findByDongSanPham`; `SanPhamImpl` dùng `= ANY (?)`), gom theo `maDongSP` trong controller. Giới hạn số dòng sản phẩm trả về (vd 50) và báo "Còn kết quả khác, hãy thu hẹp từ khóa" khi chạm giới hạn — chỉ thêm thông báo, không đổi cấu trúc `KetQuaDong`.

### DB-06 — Các danh sách quản trị nạp toàn bảng (P2)

`ProductPage` :162 (`sanPhamDao.findAll()` — toàn bộ máy), `NhanVienController` :108, `PriceTaxPage` :106/:119 (`dongSanPhamDao.findAll()` **hai lần**, trên EDT), `PromotionsPage`, `DefectiveProductsPage`. Thứ tự ưu tiên: `ProductPage` (bảng lớn nhất) → dùng `Pagination` sẵn có + truy vấn `LIMIT/OFFSET` + bộ lọc theo trạng thái/dòng tại DB (mẫu tham khảo: `CustomerListPage` sau DB-04). `PriceTaxPage`: nạp một lần và tái sử dụng kết quả. Các bảng còn lại (vài trăm dòng) chỉ cần phân trang phía client bằng `Pagination`.

---

## 6. Giai đoạn 3 — UX/UI cho quầy bán hàng điện máy

Nguyên tắc: dùng component có sẵn trong `ui/component` (`StatefulView`, `Toast`, `ConfirmDialog`, `Pagination`, `DtaqButton.setBusy`, `ShortcutBar`), token trong `ui/theme`; không thêm thư viện; không redesign màn không liên quan. Có sẵn `UiLayoutSmokeTest` và `ThemeContrastTest` — phải pass.

### UX-01 — Không chạy DB trên EDT ở màn lập hóa đơn
`RetailInvoicePage` :202-204 (`themDong`, `datSoLuong`, `boDong`), :296 (`luuBanTam`), :310-318 (`danhSachBanTam`, `khoiPhucBanTam`) gọi controller trực tiếp trong listener. Bọc bằng `SwingWorker` theo đúng mẫu `timDong` (:217) / `xuLyThanhToan` (:789): khóa thao tác giỏ trong lúc chờ (dùng cờ `processing` + `refreshView()` sẵn có), cập nhật UI trong `done()`. `BanHangController` giữ trạng thái giỏ **không thread-safe** → bảo đảm tuần tự: không cho phát sinh thao tác giỏ thứ hai khi thao tác trước chưa xong (khóa nút/ô số lượng), không dùng nhiều worker song song trên giỏ. Sau DB-02 mỗi thao tác chỉ còn 1–2 truy vấn nhỏ nên độ trễ không đáng kể.

### UX-02 — `ProductLookupPage`, `PriceTaxPage`
Cả hai có 0 `SwingWorker`. Chuyển lời gọi controller/DAO sang `SwingWorker`, hiện trạng thái tải/rỗng/lỗi bằng `StatefulView` như các trang khác (mẫu: `LongStockReportPage`).

### UX-03 — Thông báo lỗi thân thiện, không lộ chi tiết kỹ thuật
Hiện chuỗi `e.getMessage()` (có thể là lỗi SQL/tên ràng buộc tiếng Anh) được đưa thẳng ra người dùng tại: `SanPhamLoiController` :125,:140,:187,:217,:257; `DongSanPhamController` (khối `catch` của `themMoi`/`capNhat`); `SanPhamController` (khối `catch` của `capNhat`); `GiaThueController` (khối `catch`); `KhuyenMaiController` (các `"… : " + e.getMessage()`); `CategoriesPage` :201,:361; `DefectiveProductsPage` :217,:373; `PromotionsPage` :206,:424; `PriceTaxPage` :201.
Sửa: giữ `log.warn/error(…, e)`; thông báo cho người dùng là câu tiếng Việt cố định theo tình huống ("Không thể lưu dòng sản phẩm. Vui lòng thử lại."). Chỉ chuyển tiếp `getMessage()` khi ngoại lệ là lỗi nghiệp vụ do chính ứng dụng tạo (`IllegalArgumentException`/`IllegalStateException`/`SecurityException` với thông điệp tiếng Việt). Trường hợp xóa danh mục đang được dòng sản phẩm tham chiếu (FK RESTRICT) → "Danh mục đang có dòng sản phẩm, không thể xóa".

### UX-04 — Trang chủ Quản lý: hiện mọi phiếu đang chờ duyệt
`AppNavigator.taiTrangChuQuanLy` :365 lọc `homNay.equals(p.ngayTao())` → phiếu lập hôm qua chưa duyệt biến mất khỏi bảng duyệt nhanh và cảnh báo, trong khi khách đang chờ hoàn tiền. Bỏ bộ lọc ngày; sắp xếp cũ nhất lên đầu (đã có `THU_TU_HIEN_THI`); sửa câu cảnh báo ở `canhBaoHomNay` thành "N phiếu đổi/trả đang chờ duyệt (cũ nhất: dd/MM)". Cập nhật test liên quan trong `src/test/java/com/dtaq/retail/ui/`.

### UX-05 — In / lưu phiếu hóa đơn (kiểm phạm vi trước)
`InvoiceReceiptDialog` chỉ có "Đóng hóa đơn". Với hàng điện máy, phiếu có **số serial** là căn cứ bảo hành/đổi trả (XBR-15). Trước khi làm: đọc `UC003-retail-invoice.md` xem có bước in/giao hóa đơn — nếu có thì thực hiện, nếu không thì chuyển thành câu hỏi cho trưởng nhóm. Cách làm tối thiểu, không thêm dependency: nút "In hóa đơn" dùng `java.awt.print.PrinterJob` in chính panel nội dung của dialog (`Printable` vẽ component, co theo khổ giấy); lỗi/huỷ in không ảnh hưởng hóa đơn đã lưu. Mở lại hóa đơn cũ: tái sử dụng dialog này từ lịch sử hóa đơn ở `CustomerLookupPage` (dữ liệu đã có qua `KhachHangGiaoDichDao.chiTietHoaDon`).

### UX-06 — Va chạm bán trùng
Sau DB-03 lỗi này gần như không còn. Phần còn lại: đổi thông điệp :518 thành "Tồn kho vừa thay đổi, vui lòng bấm Thanh toán lại" (thu ngân không chọn máy). Giỏ hàng vẫn giữ nguyên như hiện tại.

### UX-07 / UX-08 / UX-09
- **UX-07:** thêm `Pagination` cho `ProductPage`, `PromotionsPage`, `DefectiveProductsPage`, `ProductLinesPage` (đi cùng DB-06).
- **UX-08:** thực hiện cùng NV-02 (ô trạng thái chỉ đọc + chú thích cách đổi trạng thái đúng luồng).
- **UX-09:** `RetailInvoicePage` :325 — dùng `Toast.Kind.INFO` (`Toast.Kind` chỉ có `SUCCESS`/`ERROR`/`INFO`; không thêm kiểu mới), nội dung nêu **tên dòng** và lý do ("hết hàng"/"giảm còn N máy") thay vì chỉ mã.

### Kiểm thử thủ công bắt buộc cho giai đoạn 3 (ghi kết quả vào báo cáo)
Đăng nhập tài khoản bán hàng và quản lý demo (lấy từ `V2__seed_demo_accounts.sql`/README): lập hóa đơn 2 dòng × 2 máy → thanh toán → kiểm phiếu; lưu tạm → mở lại; trả hàng < 3.000.000 và ≥ 3.000.000 → duyệt → hoàn tất; đổi hàng; thử sửa giá bằng tài khoản bán hàng (phải bị chặn); thử đổi trạng thái máy (phải bị chặn). Kiểm ở cửa sổ nhỏ nhất mà `UiLayoutSmokeTest` đang kiểm.

---

## 7. Quyết định đã chốt cho các mục còn mở (2026-11-16)

Nhóm ủy quyền người rà soát quyết định. Nguyên tắc chọn: sửa đúng triệu chứng ảnh hưởng người dùng, **không đổi thiết kế CSDL**, không thêm tính năng ngoài phạm vi. Các quyết định được ghi vào tệp UC tương ứng với mã `D29`–`D37`.

| ID | Mã | Quyết định | Thực hiện |
|---|---|---|---|
| Q-01 | D29 | Giá/thuế ban đầu nhập khi tạo dòng (đúng quyết định UC009); sau đó chỉ Quản lý đổi được | Không đổi code (NV-01 đã chặn sửa) |
| Q-02 | D30 | Máy thay thế truy vết qua phiếu đổi; không đổi/trả tiếp; không đổi CSDL | Không đổi code |
| Q-03 | D31 | Giá trị máy thay thế gồm khuyến mãi đang hiệu lực (XBR-03) | `DoiTraController` nhận `KhuyenMaiDao`; có test |
| Q-04 | D32 | Phiếu bị từ chối giữ nguyên trạng thái xử lý nhưng không đếm là phiếu đang mở | `TraCuuKhachHangController.tongQuan`; có test |
| Q-05 | D33 | Giữ hành vi mở hóa đơn tạm là xóa khỏi danh sách tạm | Không đổi code |
| Q-06 | D34 | Hồ sơ lỗi của máy `DA_BAN` không đổi trạng thái máy | `SanPhamLoiController.chuyenTrangThai`; đã thử trên ứng dụng |
| Q-07 | D35 | Vòng đời hồ sơ lỗi như NV-04. "Sửa xong → nhập lại kho" và `soLanSua` **để lại**: cần thêm giá trị vào CHECK của CSDL | Chưa làm — việc của đợt tính năng riêng |
| Q-08 | D36 | "Tổng chi tiêu" là số ròng sau khi trừ tiền đã hoàn | `KhachHangGiaoDichDao.tongHoanTra` + overload `tongQuan`; có test |
| Q-09 | D37 | Không in hóa đơn trong phạm vi hiện tại | Không đổi code |

**Việc còn lại sau đợt này (không thuộc bug-smell):** (1) trạng thái "đã sửa xong" cho hồ sơ lỗi và tăng `soLanSua` — cần migration đổi CHECK; (2) in hóa đơn nếu nhóm mở rộng phạm vi UC003.

---

## 8. Quy trình thực hiện & báo cáo cho từng task

1. `git status --short --branch` → ghi lại; không đụng thay đổi của người khác (hiện có 2 tệp đã staged: `ULTRA_FABLE_UI_UX_UPGRADE.md`, `anh-w6-plan.md`).
2. Chạy `mvn -B clean test` **trước khi sửa** → lưu danh sách test fail làm baseline.
3. Làm đúng **một** ID (hoặc cặp đã ghi "làm cùng"), theo thứ tự: Giai đoạn 1 → DB-01 → DB-02 → DB-03 → DB-04/05 → UX-01… Phụ thuộc: DB-03 cần DB-02; UX-06 cần DB-03; UX-08 đi cùng NV-02; UX-07 đi cùng DB-06.
4. Viết test trước hoặc cùng lúc; test cần DB phải tự bỏ qua bằng `Assumptions` như các test `*DbTest` hiện có và **dọn dữ liệu đã tạo** trong `@AfterEach`.
5. `mvn -B clean test` → so với baseline; `git diff --check`; xem lại `git diff` không có thay đổi ngoài phạm vi.
6. Báo cáo theo mẫu (tiếng Việt):

```
ID: NV-0x — <tên>
Tệp đã sửa: …
Hành vi trước / sau: …
Test thêm/sửa: … (kết quả: pass / skip vì không có DB)
Kết quả mvn test: X pass, Y fail (baseline: Z fail — liệt kê)
Chưa kiểm chứng được: …
Câu hỏi/mâu thuẫn phát hiện thêm: …
```

7. Không commit/push/mở PR trừ khi được yêu cầu.

## 9. Tiêu chí hoàn thành toàn plan

- Mọi mục P0/P1 ở §3 có test tự động chứng minh (hoặc ghi rõ lý do chỉ kiểm thủ công).
- Không tệp migration V1–V16 nào bị sửa; migration mới chỉ có `CREATE INDEX`.
- `pom.xml` không đổi dependency.
- Số test fail không tăng so với baseline.
- Màn lập hóa đơn không còn lời gọi `SanPhamDao.findAll()` trên đường chạy thật (kiểm bằng test đếm lời gọi ở DB-02).
- Các mục §7 vẫn nguyên trạng, kèm câu trả lời của trưởng nhóm nếu đã có.
