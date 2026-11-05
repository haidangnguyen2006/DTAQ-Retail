# Báo cáo rà soát tổng thể và sửa lỗi — Tuấn Anh

> **Ngày:** 05/11/2026 · **Nhánh làm việc:** `feature/anh-final-review` (tách từ `develop` @ `9ad6474`, **chưa commit**)
> **Phạm vi:** toàn bộ UC001–UC019 và các chức năng nền (sao lưu/phục hồi, giám sát sức khỏe dữ liệu)
> **Nguyên tắc:** chỉ sửa lỗi, **không thay đổi nghiệp vụ**. Những điểm cần quyết định nghiệp vụ được ghi ở §5, không tự xử lý.

## 1. Kết quả

| Hạng mục | Kết quả |
|---|---|
| `mvn clean test` | **603 test, 0 lỗi, 0 bỏ qua** (PostgreSQL 18, Flyway V1 → V14) |
| Kiểm thử giao diện thật (Xvfb + xdotool) | 11 trang Quản lý + 10 trang Bán hàng mở được, **0 `ERROR`/`SEVERE`** trong log |
| Luồng nghiệp vụ chạy thật từ đầu đến cuối | Đăng nhập/đăng xuất → lập hóa đơn → lập phiếu trả → Quản lý duyệt → hoàn tất phiếu → phục hồi dữ liệu → đăng nhập lại — **đều đúng**, đối chiếu từng bước với CSDL |
| Lỗi đã sửa | **13 lỗi** (§3) + 18 test mới / cập nhật |
| Đồng bộ `develop` | Đã gộp 3 commit mới (`42ce3de`, `0ca4634`, `9ad6474`); giải xung đột UC017 theo bản của Đăng (§3.1) |

## 2. Quá trình rà soát

1. **Chạy toàn bộ test trên `develop`** làm mốc ban đầu (613 test xanh trước khi gộp; 603 sau khi `develop` xóa bộ `ui/nav` cũ).
2. **Đọc từng UC theo đặc tả** (luồng chính, luồng phụ) và đối chiếu controller/DAO/trang: UC001–UC003, UC006, UC012, UC014–UC019 đọc kỹ; các UC còn lại kiểm tra qua test có sẵn và chạy giao diện.
3. **Quét lỗi theo mẫu trên toàn mã nguồn:** kết nối không đóng (không có — mọi `getConnection()` đều trong try-with-resources), so sánh chuỗi bằng `==`, `catch` rỗng, trang tạo không truyền controller, `TODO`/dữ liệu cứng, class chưa có test.
4. **Đối chiếu số liệu báo cáo với SQL tính tay độc lập** (UC015, UC017): viết test so DAO với truy vấn độc lập trên dữ liệu mẫu.
5. **Chạy ứng dụng thật** trên màn hình ảo, đăng nhập bằng tài khoản demo, bấm qua mọi trang và thực hiện giao dịch thật; chụp màn hình từng bước, kiểm log và CSDL sau mỗi thao tác.
6. **Với mỗi lỗi:** tìm nguyên nhân gốc → sửa ở chỗ dùng chung → viết test; với lỗi quan trọng, **chạy test trên mã cũ để chứng minh test bắt được lỗi**, rồi chạy trên mã mới.
7. **Dọn dữ liệu thử:** hóa đơn/phiếu tạo khi chạy giao diện được xóa bằng chính chức năng **Phục hồi dữ liệu** của ứng dụng (về bản sao lưu 18:32, trước khi kiểm thử) — đồng thời kiểm chứng lỗi §3.2.

## 3. Lỗi đã sửa

### 3.1. 🔴 UC017 — Trang thống kê doanh thu chưa nối dữ liệu và còn lỗi hiển thị

- **Hiện tượng:** `AppNavigator` tạo `new RevenueStatisticsPage()` không có controller → nút chọn kỳ bị tắt, không bao giờ tải báo cáo, không xuất được Excel. Controller/DAO của Tài đã viết xong nhưng không được gọi.
- **Gộp với `develop`:** Đăng đã nối controller ở `42ce3de` (tự tải 30 ngày gần nhất khi mở trang). Bản này được **giữ làm gốc**; bổ sung các lỗi còn lại:
  - **Kết quả cũ ghi đè kết quả mới** khi đổi kỳ/Ngày-Tuần-Tháng liên tục → thêm bộ đếm yêu cầu, bỏ qua kết quả lỗi thời.
  - **Chú thích KPI kẹt "Đang tải…"** sau khi tải xong (`KpiCard.setLoading` ghi đè chú thích) → khôi phục chú thích trong `renderSummary`.
  - **6.a "không có phát sinh"**: biểu đồ hiện rõ "Không có phát sinh trong kỳ"; nút Xuất Excel chỉ bật khi có dữ liệu.
  - **Biểu đồ tròn**: hiện **mã** dòng sản phẩm và cả 5 phần **cùng một màu** → hiện tên dòng, 5 màu `CHART_1..5`.
- **Kiểm chứng:** số liệu năm 2026 trên màn hình (trước thuế 682.100.000; tổng 744.176.000) khớp SQL tính tay. Test mới: `ThongKeDoanhThuControllerTest` (4), `ThongKeDoanhThuDaoImplTest` (3 — chỉ trừ phiếu `HOAN_TAT`, không trừ phiếu từ chối/chờ xử lý).

### 3.2. 🔴 Sau khi phục hồi dữ liệu, mọi thao tác CSDL hỏng cho tới khi mở lại ứng dụng

- **Nguyên nhân:** `RestoreService` đóng bể kết nối và tạo bể mới (`ConnectDB.reset()`), nhưng `AppNavigator`, mọi DAO và trang vẫn giữ `DataSource` của bể cũ đã đóng → kể cả đăng xuất rồi đăng nhập lại cũng báo "Không thể đăng nhập lúc này". Xảy ra cả khi Quản lý phục hồi thủ công lẫn khi `HealthMonitor` tự phục hồi.
- **Sửa tại gốc:** `ConnectDB.getDataSource()` trả một `DataSource` ủy quyền, mỗi lời gọi lấy bể **hiện tại** → mọi đối tượng đã tạo trước đó tự dùng bể mới.
- **Kiểm chứng:** test `ConnectDBTest`; chạy thật: phục hồi trên trang Phục hồi → đăng xuất → đăng nhập lại **thành công, không cần khởi động lại**.

### 3.3. 🔴 UC015 — Doanh số bị cộng 2 lần khi một máy có nhiều phiếu trả

- **Nguyên nhân:** SQL `LEFT JOIN ChiTietPhieuTra` trên từng dòng chi tiết hóa đơn. Guard của `PhieuTraDaoImpl` cho phép lập phiếu mới sau phiếu `HUY`/`TU_CHOI` → một máy có ≥ 2 dòng phiếu trả → dòng bán bị nhân bản.
- **Sửa:** tách tổng bán và tổng trả thành hai CTE rồi ghép theo dòng sản phẩm (cách UC016 đang dùng); đầu ra và cách tính giữ nguyên.
- **Kiểm chứng:** `ThongKeSanPhamDaoImplTest.phieuHuyKhongNhanDoiDoanhSo` — **trên mã cũ thất bại** (bán 1 máy, đếm thành 2), trên mã mới đạt.

### 3.4. 🟠 UC015 — Biểu đồ "Doanh thu theo dòng sản phẩm" hiển thị số liệu giả

- **Hiện tượng:** `buildPeriodRevenueChart` lấy doanh số chia 4 rồi gán cho "tháng 1–3 … tháng 10–12", bất kể kỳ đang xem (ví dụ xem 08/08–05/11 vẫn hiện 4 quý bằng nhau).
- **Sửa:** biểu đồ hiện **doanh số sau điều chỉnh thật** của 6 dòng dẫn đầu trong kỳ (đúng tiêu đề thẻ); nhãn trục rút gọn để không chồng chữ.
- **Kèm theo:** biểu đồ tròn "đã bán & tồn kho" hiện số lượng máy thành "**1 VNĐ**" và tâm ghi "Tổng doanh thu" → thêm `DonutChartPanel.setSoLuong(...)`, hiện "1 máy" / "Tổng số máy"; sửa chính tả "giữ" → "giữa".

### 3.5. 🟠 UC015 — Thiếu luồng 9.a và 6.a

- **9.a "Chọn sản phẩm cần đối chiếu → hiển thị giao dịch dùng để tổng hợp"** chưa có → thêm `ThongKeSanPhamDao.doiChieu`, `ThongKeSanPhamController.doiChieu` (kiểm quyền Quản lý), nhấp đúp một dòng để mở hộp thoại đối chiếu (cùng mẫu UC016).
- **6.a "Không có phát sinh"** không bao giờ hiện vì DAO luôn trả mọi dòng sản phẩm (kể cả dòng bằng 0) → trang coi là "không phát sinh" khi không dòng nào có bán hoặc trả.
- **Kiểm chứng:** `doiChieuKhopTongHop` — với mọi dòng sản phẩm cả năm, số giao dịch bán/trả và tổng giá trị đối chiếu **khớp tuyệt đối** số tổng hợp; `ThongKeSanPhamControllerTest` (3). Chạy thật: đối chiếu "Máy lọc nước 7 lõi" ra đúng `HD-2026-200006` / `SER-2026-0302` / 9.600.000.

### 3.6. 🟠 Lịch sử sao lưu/phục hồi không bao giờ được ghi trên Ubuntu

- **Nguyên nhân:** cột `LichSuSaoLuu.phienBanPg` là `VARCHAR(50)`, nhưng `pg_dump --version` trên Ubuntu là `pg_dump (PostgreSQL) 18.6 (Ubuntu 18.6-0ubuntu0.26.04.1)` (**56 ký tự**) → mọi lần ghi lịch sử lỗi `22001`. Tệp `.dump` vẫn được tạo nên phục hồi vẫn chạy, nhưng bảng lịch sử chỉ có dòng do test tạo.
- **Sửa:** migration mới `V14__lichsu_sao_luu_mo_rong_phien_ban_pg.sql` đổi cột sang `TEXT` (chỉ đổi kiểu cột).
- **Kiểm chứng:** test DAO dùng chuỗi phiên bản thật thay cho `"16.13"` giả — **thiếu V14 thì lỗi** `value too long for type character varying(50)`, có V14 thì đạt.

### 3.7. 🟠 UC006 — Phiếu cần duyệt vẫn hỏi "Xác nhận đã hoàn tiền"

- **Hiện tượng:** lập phiếu trả 10.560.000 (≥ 3.000.000, phải chờ Quản lý duyệt) nhưng hộp xác nhận ghi "Hoàn trả khách … Xác nhận **đã hoàn tiền** và lập phiếu?" → nhân viên có thể đưa tiền trước khi được duyệt, trái XBR-07 (duyệt chỉ ghi nhận quyết định; tiền hoàn khi hoàn tất sau duyệt).
- **Sửa:** khi phiếu cần duyệt, tiêu đề "Xác nhận gửi phê duyệt" và nội dung nêu rõ "**Chưa hoàn tiền** cho khách cho đến khi phiếu được duyệt và hoàn tất". Phiếu không cần duyệt giữ nguyên câu cũ. Cập nhật `ReturnsPageTest` (test cũ đang khẳng định hành vi sai).

### 3.8. 🟡 UC019 — Lưu giá/thuế thành công nhưng không có thông báo (bước 12)

Form xóa trắng mà không báo gì → thêm `Toast` "Cập nhật giá bán và thuế suất thành công"; bảng tải lại hiển thị giá/thuế mới.

### 3.9. 🟡 Nút "Từ chối" bị cắt trong bảng phê duyệt

Bảng tự co mọi cột theo chiều rộng còn lại, kể cả cột thao tác → sửa chung trong `DataTable.setActionColumn`: cột thao tác có độ rộng tối thiểu bằng đúng các nút (áp dụng cho mọi trang có cột thao tác).

### 3.10. 🟡 Lỗi `SEVERE` mỗi lần mở hộp thoại phê duyệt/từ chối

`FormField` áp style ô nhập (`arc: 12`) cho mọi thành phần, kể cả `JScrollPane` của ô ý kiến → FlatLaf ghi `UnknownStyleException: unknown style 'arc'`. Sửa: không áp style này cho `JScrollPane` (đã có viền bo góc riêng).

### 3.11. 🟡 Hộp chọn khách hàng — nút "Tìm" bị đẩy ra ngoài

Địa chỉ dài kéo rộng thân hộp thoại → nút "Tìm" và một phần địa chỉ bị cắt. Sửa: cho nhãn co lại (`wmin 0`), địa chỉ dài hiện "…" và tooltip đầy đủ.

### 3.12. 🟡 Biểu tượng `save` và `refresh-cw` không tồn tại

Các nút "Lưu", "Lưu máy", "Lưu cập nhật", "Làm mới" (5 trang) hiện **ô vuông đỏ**. Thêm 2 tệp SVG cùng bộ Lucide; bổ sung vào `DtaqIconsTest`.

### 3.13. Đã kiểm và **không** phải lỗi

| Nghi vấn | Kết luận |
|---|---|
| UC018 từ chối không chuyển phiếu sang `HUY` | Đúng XBR-07; guard đã loại cả `TU_CHOI` lẫn `HUY` nên máy vẫn lập được phiếu mới |
| `HealthMonitor` tự phục hồi có thể ghi đè dữ liệu thật | Chỉ khi **mọi** bảng cốt lõi rỗng, mất bảng hoặc CSDL không tồn tại (`3D`) — đủ thận trọng |
| UC003 bán trùng máy khi hai người thanh toán cùng lúc | `danhDauDaBan` có điều kiện trạng thái (CAS) trong cùng transaction, có rollback |
| UC012 chồng lấn khuyến mãi | Khớp quyết định D11 |
| UC014 | Đủ 5.a, 8.a, 9.a; khớp quyết định "loại 3 trạng thái đã rời kho" |

## 4. Kiểm thử chạy thật (tài khoản demo `NVQL2026001` / `NVBH2026001`)

| Bước | Kết quả | Đối chiếu CSDL |
|---|---|---|
| UC001 sai mật khẩu / đúng mật khẩu | "Tên đăng nhập hoặc mật khẩu không đúng", ô mật khẩu xóa / vào trang chủ đúng vai trò | — |
| UC002 Hủy / Đăng xuất | Giữ phiên / về màn hình đăng nhập | — |
| UC003 bán 1 máy lọc nước (9.600.000 + 10% thuế, khách đưa 11.000.000) | Hóa đơn 10.560.000, thối 440.000 | Serial `DA_BAN`, tồn 2 → 1 (RB37) |
| UC006 lập phiếu trả 10.560.000 | Báo cần Quản lý duyệt | `CHO_DUYET / CHO_XU_LY` |
| UC018 phê duyệt kèm ý kiến | Thành công | `DA_DUYET`, người duyệt, ý kiến; vẫn `CHO_XU_LY` |
| UC006 hoàn tất phiếu đã duyệt | Thành công | `HOAN_TAT`, serial về `SAN_SANG_BAN`, `soLanDoiTra = 1`, tồn 1 → 2 |
| UC015 / UC017 | Số liệu khớp SQL tính tay; đối chiếu 9.a đúng | — |
| Phục hồi dữ liệu (FR016) → đăng nhập lại | Thành công, tự tạo bản sao lưu an toàn trước | Dữ liệu thử đã bị xóa |

## 5. Còn tồn tại — cần nhóm quyết định (không tự sửa vì là nghiệp vụ)

| # | Vấn đề | Ghi chú |
|---|---|---|
| 1 | **Trang chủ Bán hàng** (3 KPI, hóa đơn gần đây) và **Trang chủ Quản lý** (nhật ký hoạt động, cảnh báo) **không có mã nạp dữ liệu** — luôn hiện "—"/trống | Cần chốt: "hóa đơn hôm nay" của cả cửa hàng hay của nhân viên đang đăng nhập, tính tiền mặt có gồm hoàn trả không… |
| 2 | `SanPhamController`, `DanhMucController`, `SanPhamLoiController` chưa kiểm quyền ở controller (XBR-13) | Đúng trạng thái "C-03: UC008/010/011 vẫn mở" |
| 3 | Mã hóa đơn mới nối tiếp dải seed V13 (`HD-2026-200009`…) thay vì `HD-2026-0000xx` | Đúng định dạng, không trùng. Sửa V13 lúc này sẽ gây lỗi checksum cho CSDL của mọi người |
| 4 | Phục hồi một bản sao lưu cũ hơn V14 đưa schema về V13 cho tới lần khởi động sau (Flyway áp lại V14) | Lịch sử của đúng lần phục hồi đó không ghi được (§3.6) |
| 5 | Trang Đổi/trả: lăn chuột khi con trỏ nằm trên ô nhập/bảng không cuộn trang | Giao diện, không ảnh hưởng nghiệp vụ |
| 6 | Nhiều test DB ghi dữ liệu thật vào CSDL phát triển (ví dụ `LichSuSaoLuu` có dòng "TEST") | Vệ sinh test |

## 6. File thay đổi

| File | Nội dung |
|---|---|
| `config/ConnectDB.java` | `DataSource` ủy quyền theo bể hiện tại (§3.2) |
| `controller/ThongKeDoanhThuController.java` | Biểu đồ tròn: tên dòng, 5 màu (§3.1) |
| `ui/page/report/RevenueStatisticsPage.java` | Chặn kết quả cũ, chú thích KPI, 6.a, nút Excel (§3.1) |
| `dao/ThongKeSanPhamDao.java`, `dao/impl/ThongKeSanPhamDaoImpl.java` | SQL tách CTE (§3.3); `doiChieu` (§3.5) |
| `controller/ThongKeSanPhamController.java` | `doiChieu` (§3.5) |
| `ui/page/report/SalesByProductPage.java` | Biểu đồ thật, 6.a, 9.a, nhãn, chính tả (§3.4, §3.5) |
| `ui/component/DonutChartPanel.java` | Chế độ số lượng `setSoLuong` (§3.4) |
| `db/migration/V14__lichsu_sao_luu_mo_rong_phien_ban_pg.sql` | **Mới** (§3.6) |
| `ui/page/returns/ReturnsPage.java` | Câu xác nhận phiếu cần duyệt (§3.7) |
| `ui/page/product/PriceTaxPage.java` | Thông báo thành công (§3.8) |
| `ui/component/DataTable.java`, `ui/component/table/TableRenderers.java` | Độ rộng tối thiểu cột thao tác (§3.9) |
| `ui/component/FormField.java` | Không áp style ô nhập cho `JScrollPane` (§3.10) |
| `ui/page/customer/KhachHangPicker.java` | Bố cục + tooltip (§3.11) |
| `icons/save.svg`, `icons/refresh-cw.svg` | **Mới** (§3.12) |
| Test mới | `ConnectDBTest`, `ThongKeDoanhThuControllerTest`, `ThongKeDoanhThuDaoImplTest`, `ThongKeSanPhamControllerTest`, `ThongKeSanPhamDaoImplTest` |
| Test cập nhật | `LichSuSaoLuuDaoImplTest`, `ReturnsPageTest`, `DtaqIconsTest` |

## 7. Lưu ý khi tiếp nhận

- Thay đổi **chưa được commit** (theo yêu cầu). Nhánh `feature/anh-final-review` đang ở đúng `develop` mới nhất cộng các thay đổi trên.
- Lần chạy ứng dụng đầu tiên sau khi gộp sẽ áp **migration V14** (an toàn, chỉ đổi kiểu một cột).
- Thư mục `backups/` có bản sao lưu an toàn `dtaq_2026-11-05_185019.dump` do ứng dụng tự tạo trước khi phục hồi; bản này chứa dữ liệu thử nên có thể xóa.
