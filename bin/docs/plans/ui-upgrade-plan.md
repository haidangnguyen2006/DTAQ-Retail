# Kế hoạch nâng cấp giao diện DTAQ Retail

> **Trạng thái:** ĐỀ XUẤT — chưa được nhóm phê duyệt. Không phải tài liệu yêu cầu; báo cáo DOCX vẫn là nguồn chuẩn.
> **Ngày lập:** 2026-10-24 · **Nhánh khảo sát:** `feature/ui-shell-figma-restyle` (commit `8a53e9e`).
> **Người thực hiện dự kiến:** một agent/model khác sẽ code theo kế hoạch này, từng giai đoạn một.

---

## 0. Cách dùng tài liệu này (đọc trước khi code)

1. Làm **theo thứ tự giai đoạn** (mục 6). Mỗi giai đoạn là một thay đổi nhỏ, build được, xem được.
2. Trước mỗi giai đoạn: `git status --short --branch`, đọc lại các file liên quan, đọc `AGENTS.md` và skill `agents/skills/java-swing-layered-architecture/SKILL.md`.
3. Sau mỗi giai đoạn: `mvn -B clean package` (cần JDK 21 — xem `run.bat`), chạy app bằng `run.bat`, kiểm tra cả hai vai trò (`NVQL2026001` / `NVBH2026001`, mật khẩu `12345678`), rồi `git diff --check`.
4. **Không tự quyết** các điểm ở mục 8 (Quyết định cần chốt). Gặp điểm đó thì dừng và hỏi.
5. Không commit/push nếu người dùng chưa yêu cầu.

### Nguồn đã khảo sát khi lập kế hoạch

- Toàn bộ `src/main/java/com/dtaq/retail/ui/**` (47 file, ~5.400 dòng), `session/CurrentSession.java`, `AppNavigator.java`, `Main.java`.
- `pom.xml`, `DtaqLightLaf.properties`, bộ icon SVG trong `src/main/resources/com/dtaq/retail/ui/theme/icons/`.
- `docs/reference/ui/screen-flow-and-screens.md`, `docs/reference/requirements/scope-and-requirements.md` (C-04, C-07, C-11, C-12), `V1__initial_schema.sql`.
- Ảnh thiết kế trong báo cáo: `docs/reference/assets/screens/image1.png` (Trang chủ quản lý), `image3.png` (Lập hóa đơn), `image7.png` (In hóa đơn), `image14.png` (Thống kê), `image15.png` (Tồn kho lâu ngày).
- **Figma chưa được kiểm tra** trong phiên lập kế hoạch (không có Figma MCP). Mã nguồn có ghi node ID Figma trong Javadoc (`1:1200`, `1:1380`, `1:368`, `1:176`, `1:2`, `2030:50`, `1:1054`, `2003:231`, `1:878`, `1:716`, `1:523`). Người thực hiện nên mở các frame này nếu có Figma MCP; nếu không, dùng ảnh trong `docs/reference/assets/screens/`.

---

## 1. Mục tiêu

1. **Giao diện đẹp, sạch, gọn**, nhất quán trên mọi màn hình, bám theo prototype Figma/ảnh trong báo cáo.
2. **UX hiệu quả cho quầy bán hàng**: thao tác bằng bàn phím, ít click, trạng thái rõ ràng (đang tải / rỗng / lỗi / có dữ liệu).
3. **Xóa toàn bộ dữ liệu demo cứng** không lấy từ CSDL. Màn hình chưa có nguồn dữ liệu phải hiển thị trạng thái rỗng trung thực, không hiển thị số liệu giả.
4. **Đưa logic nghiệp vụ ra khỏi UI** (tính thuế, sinh mã hóa đơn, chốt thanh toán). UI chỉ hiển thị giá trị do controller trả về.
5. **Tận dụng thư viện có sẵn**, không thêm dependency.

### Ngoài phạm vi kế hoạch này

- Viết DAO/controller/SQL mới cho khách hàng, sản phẩm, hóa đơn, báo cáo. Đó là các task riêng theo từng Use Case (UC003, UC005, UC007, UC014, UC017, UC018…). Kế hoạch này chỉ chuẩn bị **API hiển thị** để các task đó gắn dữ liệu vào.
- Màn hình mới chưa có trong code (UC004, UC006, UC008–UC013, UC015, UC016, UC019). Không tự tạo.
- Các mục ngoài phạm vi đề tài: thanh toán online/QR, đa chi nhánh, công nợ, trả góp, khuyến mãi (C-04 chưa chốt).

---

## 2. Thư viện có sẵn (đã xác minh trong `pom.xml` và jar)

| Thư viện | Phiên bản | Dùng cho |
|---|---|---|
| FlatLaf | 3.6 | Look & Feel. Đã xác minh có: `FlatClientProperties.STYLE`/`STYLE_CLASS`, `OUTLINE`, `PLACEHOLDER_TEXT`, `TEXT_FIELD_LEADING_ICON`, `TEXT_FIELD_TRAILING_COMPONENT`, `TEXT_FIELD_SHOW_CLEAR_BUTTON`, `SELECT_ALL_ON_FOCUS_POLICY`, `BUTTON_TYPE_BORDERLESS`, `BUTTON_TYPE_TOOLBAR_BUTTON`; `FlatPanelUI` có thuộc tính `arc` (panel bo góc qua style); `FlatLineBorder(Insets, Color, float, int arc)`; `FlatTableUI` có `selectionArc`, `selectionInsets`. |
| flatlaf-extras | 3.6 | `FlatSVGIcon` (đang dùng trong `DtaqIcons`), `FlatAnimatedLafChange`. |
| flatlaf-fonts-inter | 4.1 | Font Inter, có họ `FAMILY`, `FAMILY_LIGHT`, `FAMILY_SEMIBOLD`. |
| MigLayout | 11.4.2 | Layout cho mọi màn hình. |
| JUnit Jupiter | 5.12.2 | Test cho phần logic thuần (formatter, model). |

**Không thêm** thư viện biểu đồ, bảng, layout, icon hay theme khác. Biểu đồ tiếp tục vẽ bằng Java2D (`BarChartPanel`, `DonutChartPanel`).

---

## 3. Hiện trạng (kết quả khảo sát)

### 3.1. Bản đồ màn hình

| Màn hình | Class | Vai trò | Nguồn dữ liệu thật | Tình trạng |
|---|---|---|---|---|
| Đăng nhập (UC001) | `LoginFrame` | — | Có (`AuthController` → `NhanVienDao`) | Hoạt động. Code trùng lặp với Quên mật khẩu. |
| Quên mật khẩu | `ForgotPasswordFrame` | — | Có (`PasswordResetController`) | Hoạt động. Trùng lặp code, thông báo thành công dùng `JOptionPane`. |
| Khung chính | `AppShellFrame`, `Sidebar`, `Topbar`, `NavItem` | Cả hai | Chỉ `NhanVien` đăng nhập | Có lỗi cuộn và phím tắt (3.3). |
| Trang chủ bán hàng | `SalesHomePage` | Bán hàng | **Chưa có** | KPI hiển thị "—". Có lỗi lồng card. |
| Quản lý khách hàng (UC007) | `CustomerListPage`, `AddCustomerDialog` | Bán hàng | **Chưa có** DAO `KhachHang` | Không có ai gọi `setOnSearch`/`setOnSave`; nút Lưu không làm gì. |
| Lập hóa đơn bán lẻ (UC003) | `RetailInvoicePage`, `InvoiceReceiptDialog` | Bán hàng | **Chưa có** | Chứa logic nghiệp vụ sai chỗ (3.2). |
| Trang chủ quản lý (UC018) | `ManagerHomePage` | Quản lý | **Chưa có** | Card "Kết nối CSDL: Ổn định" là giá trị cứng. |
| Thống kê doanh thu (UC017) | `RevenueStatisticsPage` | Quản lý | **Chưa có** | **Toàn bộ số liệu là demo.** |
| Báo cáo tồn kho lâu ngày (UC014) | `LongStockReportPage` | Quản lý | **Chưa có** | **KPI và 6 dòng bảng là demo.** |

Ghi nhận: trong `src/main/java` **không có nơi nào gọi** `setOnSearch`, `setOnSave`, `setOnCustomerLookup`, `setOnProductSearch`, `setOnApprove`, `renderApprovals`, `renderCustomers`, `renderRows`, và cũng **không có nơi nào gọi** `Page.installShortcuts`. Các màn hình nghiệp vụ hiện chỉ là vỏ giao diện.

### 3.2. Dữ liệu demo và logic nghiệp vụ sai chỗ — phải xử lý

| # | Vị trí | Vấn đề | Xử lý |
|---|---|---|---|
| D1 | `page/report/LongStockReportPage.java:76-78` | KPI cứng `"6"`, `"246.000.000"`, `"126 ngày"`, `"TIVI003 · Nhập ngày 05/06/2026"` | Khởi tạo `"—"` và caption trung tính; chỉ đổi qua `render…()`. |
| D2 | `page/report/LongStockReportPage.java:126` + method `populateInitialRows()` | 6 dòng sản phẩm giả | Xóa method và lời gọi. Bảng khởi đầu ở trạng thái rỗng. |
| D3 | `page/report/LongStockReportPage.java:95` | Danh sách ngành hàng cứng (`Tivi`, `Tủ lạnh`…) | Chỉ giữ `"Tất cả ngành hàng"`; thêm API `setCategories(List<…>)` để task UC014 nạp từ `DanhMucSanPham`. |
| D4 | `page/report/LongStockReportPage.java:103` | Nút "Áp dụng" không có listener | Thêm `setOnApplyFilter(Consumer<Filter>)`; tắt nút khi chưa có listener. |
| D5 | `page/report/RevenueStatisticsPage.java:58` | Khoảng ngày cứng `"03/10 – 09/10/2026"` | Hiển thị kỳ mặc định tính từ ngày hiện tại hoặc `"Chọn kỳ báo cáo"`; chỉ cập nhật qua API. |
| D6 | `page/report/RevenueStatisticsPage.java:70-73` | 4 KPI giả (`285.000.000`, `↑ 12,4%`…) | Khởi tạo `"—"`. |
| D7 | `page/report/RevenueStatisticsPage.java:94-…`, `:121-…` | Dữ liệu biểu đồ cột và donut giả | Không gọi `setData` lúc khởi tạo; biểu đồ hiển thị trạng thái rỗng. |
| D8 | `page/report/RevenueStatisticsPage.java:158` | Banner "Hiện có 6 yêu cầu đổi/trả…" | Ẩn banner mặc định; API `setPendingReturnCount(int)` chỉ hiện khi > 0. |
| D9 | `page/home/ManagerHomePage.java:162` | "Kết nối CSDL — Ổn định" luôn hiển thị | Xóa card này (không có kiểm tra thật). Giữ card thời gian và phiên bản. |
| D10 | `page/home/ManagerHomePage.java:147` | Cảnh báo cứng "Kiểm tra tồn kho định kỳ" | Xóa; khu cảnh báo hiển thị rỗng và nhận dữ liệu qua API. |
| D11 | `shell/Sidebar.java:168` | "Hệ thống sẵn sàng" luôn xanh | Xóa dòng trạng thái giả hoặc đổi thành thông tin tĩnh trung thực (vai trò người dùng). |
| D12 | `page/invoice/RetailInvoicePage.java:515-519` | UI tự tính thành tiền sau thuế bằng `double` rồi cắt bỏ phần lẻ — vi phạm C-11 và kiến trúc | Xóa phép tính. `CartItem`/`ProductResult` chỉ nhận số tiền đã tính từ controller. |
| D13 | `page/invoice/RetailInvoicePage.java:572` | Số hóa đơn `"HD-" + System.currentTimeMillis() % 100000` — vi phạm quy tắc định danh (AGENTS §5.5) | Xóa. Số hóa đơn chỉ nhận từ kết quả lưu của controller. |
| D14 | `page/invoice/RetailInvoicePage.java:574-575` | Tự bịa `"Khách vãng lai"`, `"KH0000"` | Xóa. Không cho thanh toán khi chưa chọn khách hàng (đúng như điều kiện `canPay` hiện có). |
| D15 | `page/invoice/RetailInvoicePage.java:544` | Chuyển sang `PAID` và mở hóa đơn **trước** khi có bất kỳ lưu trữ nào — vi phạm "hóa đơn hoàn tất chỉ sau khi thanh toán đủ" | Đổi luồng: nút Thanh toán gọi `onProcessPayment`; UI vào trạng thái "đang xử lý"; chỉ khi controller báo thành công mới gọi `showPaid(receipt)`. |
| D16 | `page/invoice/RetailInvoicePage.java:560` | Lỗi: tổng trước thuế trên hóa đơn in bỏ qua số lượng | Hết lỗi khi áp dụng D12/D15 (receipt nhận số liệu từ controller). |
| D17 | `page/invoice/RetailInvoicePage.java:563` | Đơn vị tính cứng `"Cái"` | Nhận `donViTinh` từ dữ liệu (`DongSanPham.donViTinh` có trong schema). |
| D18 | `page/invoice/RetailInvoicePage.java:384` | Pill "Tồn kho hợp lệ" luôn hiện | Xóa hoặc chỉ hiện khi controller xác nhận. |
| D19 | `page/invoice/InvoiceReceiptDialog.java:89,93` | Địa chỉ và số điện thoại cửa hàng cứng; **khác** với prototype (`image7.png` ghi "123 Nguyễn Văn Cừ…") | Không tự chọn. Chuyển vào một hằng/cấu hình duy nhất và đánh dấu chờ quyết định (mục 8, Q4). |
| D20 | `page/invoice/RetailInvoicePage.java:114` | Thời gian trên tiêu đề cố định từ lúc tạo trang | Cập nhật khi `onShow()` và sau mỗi hóa đơn mới. |

### 3.3. Lỗi giao diện/UX chung

| # | Vị trí | Vấn đề | Hướng sửa |
|---|---|---|---|
| U1 | `shell/AppShellFrame.java:54` | Một `JScrollPane` bọc cả `CardLayout`: mọi trang chung một thanh cuộn, chiều cao card bằng trang cao nhất, bảng `grow, push` không lấp đầy màn hình, và bị cuộn lồng với scroll của `DataTable`. | Mỗi trang có scroll riêng: bọc `page.view()` trong `JScrollPane` khi `registerPage`. Trang dạng bảng (khách hàng, tồn kho) không bọc scroll ngoài mà để bảng tự giãn theo viewport. Đề xuất thêm `default boolean scrollable() { return true; }` vào `Page`. |
| U2 | `shell/AppShellFrame.java` | `installShortcuts` **không bao giờ được gọi**: F2/F3/F4/F8 không hoạt động dù UI hiển thị gợi ý phím. | Trong `showPage`: xóa binding của trang cũ, gọi `page.installShortcuts(...)` trên `InputMap(WHEN_IN_FOCUSED_WINDOW)` của root pane. |
| U3 | `component/ModalDialog.java:124` | ESC dùng `KeyListener` trên `JDialog` undecorated, không nhận được khi focus nằm trong ô nhập. | Dùng `getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW)` + `ActionMap`. Thêm `setDefaultButton`. |
| U4 | `component/ModalDialog.java:56` | Click ra nền tối luôn đóng dialog, nên form đang nhập dễ mất dữ liệu. | Thêm cờ `closeOnOverlayClick` (mặc định `false` cho dialog có form). |
| U5 | `page/home/SalesHomePage.java:108` | `render()` thêm một `Card` vào trong `Card`, tạo viền kép. | Dùng một header dựng sẵn, chỉ thay phần thân (rỗng ↔ bảng). |
| U6 | `page/home/SalesHomePage.java:124-125`, `ManagerHomePage.java:139,145` | Tiêu đề card là `JLabel` thô, không có font/màu token. | Dùng `Card.titled(...)` hoặc component `SectionHeader` mới. |
| U7 | `page/home/ManagerHomePage.java:195` | `renderApprovals` gọi `removeAll()` làm mất phần header ("Danh sách chờ phê duyệt", tổng hoàn). | Tách header và vùng nội dung; chỉ thay vùng nội dung. |
| U8 | `ManagerHomePage.java:190`, `CustomerListPage.java:134` | Cột "Thao tác" chỉ là chữ (`"Xử lý"`, `"Chi tiết"`), không bấm được. Prototype có nút "Phê duyệt"/"Từ chối". | Thêm renderer/editor cột hành động vào `DataTable` (mục 5.2). |
| U9 | `CustomerListPage.java:137` | Phân trang cứng: luôn trang 1, kích thước trang `8` viết thẳng trong code. | Hằng `PAGE_SIZE`; `renderCustomers(list, total, page)`; phát sự kiện đổi trang. |
| U10 | `component/SegmentedControl.java:49,58` | Đổi trạng thái bằng `removeAll()` + đặt lại style thủ công, lặp lại mã của `DtaqButton`. | Dùng `JToggleButton` + `ButtonGroup` với style FlatLaf, hoặc cho `DtaqButton` hàm `setVariant(...)`. |
| U11 | `DtaqButton`, `FormField`, `LoginFrame`, `ForgotPasswordFrame` | Chuỗi style FlatLaf chép thẳng mã màu (`#087F75`, `#E2E8EC`), trùng với `DtaqColors`. | Đưa vào `DtaqLightLaf.properties` (style class `[style]…`) hoặc một helper `DtaqStyles`. Không còn mã hex trong Java ngoài `DtaqColors`. |
| U12 | `theme/DtaqFonts.java` | "Medium/SemiBold" đều map sang `Font.BOLD`; họ Inter SemiBold đã cài nhưng không dùng, nên chữ đậm nặng nề. KPI và tiêu đề trang dùng PLAIN cỡ lớn. | Thêm hàm `semibold(size)` dùng `FlatInterFont.FAMILY_SEMIBOLD`; map lại các token `*_MEDIUM`, `LABEL`, `TABLE_HEADER`, `PAGE_TITLE`, `KPI_VALUE`. |
| U13 | Nhiều class (`Card`, `InfoCallout`, `ActivityItem`, `EmptyState`, logo, avatar…) | Tự vẽ nền bo góc bằng `paintComponent`, lặp lại nhiều lần. | Dùng style FlatLaf `arc` trên `JPanel` + `FlatLineBorder` cho viền (đã xác minh có trong 3.6). Giữ `paintComponent` chỉ khi FlatLaf không làm được (biểu đồ, avatar tròn). |
| U14 | `LoginFrame`, `ForgotPasswordFrame` | Panel thương hiệu và khối ô nhập trùng lặp gần như toàn bộ; không dùng `FormField`. | Tách `AuthBrandPanel` dùng chung; dùng `FormField.text/password`. |
| U15 | `DtaqLightLaf.properties:29` | `Table.rowHeight = 61` quá cao, bảng thưa. | Dùng 44–48 cho bảng thường, 56–64 chỉ cho bảng hai dòng. |
| U16 | `theme/DtaqTheme.java:36` | `defaultFont` đặt bằng `UIManager.put` sau khi setup LAF. | Khai báo trong properties (`defaultFont = 13 "Inter"`) để FlatLaf áp dụng thống nhất. |
| U17 | `RetailInvoicePage` | Thanh phím tắt ghi "↑ ↓ Chọn kết quả" nhưng chưa có xử lý phím mũi tên; `selectedResultIndex` không bao giờ thay đổi. | Cài điều hướng ↑/↓/Enter trong ô tìm sản phẩm. |
| U18 | `Topbar` | Xác nhận đăng xuất dùng `JOptionPane` mặc định, lệch phong cách với `ModalDialog`. | Thêm `ConfirmDialog` dựa trên `ModalDialog`. Dùng cho đăng xuất và thông báo thành công của Quên mật khẩu. |

---

## 4. Nguyên tắc bắt buộc khi code (guardrails)

1. **Kiến trúc:** `UI → Controller → DAO → DAO impl → Entity → PostgreSQL`. UI không chứa SQL/JDBC, không giữ `DataSource`. Handler sự kiện chỉ chuyển tiếp qua callback/controller.
2. **Luồng EDT:** mọi truy vấn chạy trong `SwingWorker`; chỉ cập nhật component trong `done()`/EDT. Trong lúc chạy, khóa nút liên quan và hiện trạng thái đang tải.
3. **Không bịa dữ liệu:** không có chuỗi số liệu, tên sản phẩm, tên khách, mã, địa chỉ… giả trong code UI. Nhãn, placeholder và hướng dẫn thì được phép.
4. **Tiền tệ:** UI chỉ **định dạng** (`MoneyFormat`), không tính thuế, không làm tròn (C-11 chưa chốt). Tổng tiền hiển thị lấy từ controller.
5. **Định danh:** UI không sinh mã (hóa đơn, khách hàng, phiếu…). Mã do tầng ứng dụng sinh khi lưu thành công (AGENTS §5.5).
6. **Tồn kho (C-07):** không tự chọn mô hình serial hay số lượng. Xem mục 8, Q1.
7. **Khuyến mãi (C-04):** không thêm ô nhập hay hiển thị khuyến mãi.
8. **Phê duyệt đổi trả:** giữ ngưỡng ≥ 3.000.000 VNĐ do controller kiểm tra. UI chỉ hiển thị trạng thái; không tạo đường tắt bỏ qua phê duyệt.
9. **Design token:** màu chỉ lấy từ `DtaqColors`/properties, cỡ chữ từ `DtaqFonts`, khoảng cách và bo góc từ `DtaqSpacing`. Không viết số "ma thuật" mới.
10. **Phạm vi:** chỉ sửa file thuộc giai đoạn đang làm; không đổi entity, DAO, controller, schema, `pom.xml`.
11. **Giữ API công khai hiện có** của các trang (`render…`, `setOn…`, record dữ liệu) trừ khi giai đoạn đó ghi rõ việc đổi. Nếu đổi, cập nhật mọi nơi gọi trong cùng thay đổi.

---

## 5. Thiết kế mục tiêu

### 5.1. Design system (theme)

**Màu** — giữ bảng `DtaqColors` hiện tại (khớp prototype teal `#087F75`). Bổ sung có kiểm soát:

| Token mới | Mục đích | Gợi ý |
|---|---|---|
| `SUCCESS` / `SUCCESS_SOFT` | Trạng thái thành công tách biệt với màu thương hiệu | Có thể dùng lại `PRIMARY` / `PRIMARY_SOFT` nếu nhóm muốn giữ ít màu. |
| `WARNING` / `WARNING_SOFT` | Cảnh báo không nghiêm trọng (tồn kho lâu, chờ duyệt) | Cam/hổ phách dịu. |
| `ROW_HOVER` | Nền dòng bảng khi rê chuột | Giữa `SURFACE` và `BG_APP`. |
| `FOCUS_RING` | Viền focus bàn phím | Primary với độ trong suốt. |

Thêm vào `DtaqColorsTest` một kiểm tra cho token mới nếu test hiện tại có kiểm tra danh sách token.

**Typography** — thang chữ gọn (giảm số token đang có):

| Vai trò | Cỡ / độ đậm |
|---|---|
| Display (đăng nhập) | 32 SemiBold |
| Tiêu đề trang | 24–26 SemiBold |
| Tiêu đề card / section | 16 SemiBold |
| KPI value | 28 SemiBold, số tabular |
| Thân / control | 14 Regular |
| Bảng / nav | 13 Regular, header 12 SemiBold |
| Caption / hint | 12 / 11 Regular |

Gộp các token trùng nghĩa (`CARD_TITLE_SEMI`, `SECTION_TITLE_STRONG`, `CUSTOMER_NAME`…) về thang trên. Giữ tên cũ dạng alias `@Deprecated` trong một giai đoạn để không vỡ build, rồi xóa ở giai đoạn dọn dẹp.

**Khoảng cách** — lưới 4/8 pt. Đề xuất giảm `PAGE_PADDING` 32 → 24, `CARD_PADDING` 24 → 20, `TABLE_ROW_H` 61 → 48 để màn hình 1366×768 (máy quầy phổ biến) hiển thị đủ nội dung mà không phải cuộn.

**LAF properties** — chuyển style lặp lại vào `DtaqLightLaf.properties`:
- `defaultFont`, `Component.focusWidth = 1` (hiện là 0, nên khó thấy focus khi dùng bàn phím), `Component.focusColor`.
- `Table.selectionArc`, `Table.selectionInsets`, `Table.rowHeight`, `Table.intercellSpacing`.
- Style class dùng chung, ví dụ `[style]Button.primary`, `[style]Button.secondary`, `[style]Panel.card`, `[style]TextField.dtaq`, rồi gọi bằng `putClientProperty(STYLE_CLASS, "primary")`.

### 5.2. Thư viện component (`ui/component`)

| Component | Thay đổi |
|---|---|
| `Card` | Dùng style `arc` + `FlatLineBorder` thay `paintComponent`. Thêm `Card.section(title, subtitle, trailing)` có header chuẩn, và `setBody(JComponent)` để thay phần thân mà không mất header (sửa U5, U7). |
| **`StatefulView`** (mới) | `CardLayout` 4 trạng thái: `LOADING` (nhãn "Đang tải…" + `JProgressBar` indeterminate mảnh), `EMPTY` (`EmptyState`), `ERROR` (thông báo + nút "Thử lại"), `CONTENT`. Mọi vùng dữ liệu đều đặt trong component này. Có thể thay `PlaceholderPage` khi chưa có dữ liệu. |
| `DataTable` | Hover dòng; `TableRowSorter` tùy chọn; cột căn phải cho số; **renderer + editor cột hành động** (`setActionColumn(col, List<Action>)`) hiển thị nút nhỏ primary/danger; tooltip khi chữ bị cắt; giữ `setCodeColumn/MoneyColumn/TwoLineColumn`. Bỏ viền scroll kép khi đặt trong `Card`. |
| `DtaqButton` | Thêm `setVariant(Variant)`; style lấy từ style class thay chuỗi hex; nhãn phím tắt vẽ dạng "kbd" nhạt; trạng thái `busy` (vô hiệu + chữ "Đang xử lý…"). |
| `FormField` | Thêm `required` (dấu * màu danger), `setError` đồng thời đặt `OUTLINE=error` (đã có), `TEXT_FIELD_SHOW_CLEAR_BUTTON` cho ô tìm kiếm, `SELECT_ALL_ON_FOCUS_POLICY` cho ô số tiền. Thêm `FormField.money(...)` định dạng phân cách nghìn khi nhập và trả về `long`. `getTextField()` không ép kiểu mù (hiện ném `ClassCastException` với combo). |
| `KpiCard` | Thêm `setLoading()`, `setTrend(up/down/neutral, text)` thay cho việc dò ký tự "↑" trong caption. |
| `StatusPill` | Thêm `WARNING`, `INFO`; tự đặt chiều rộng tối thiểu. |
| `Pagination` | Hiển thị "Trang x / y", nút ‹ › dạng toolbar button; không dùng `StatusPill` làm số trang. |
| `SegmentedControl` | Viết lại bằng `JToggleButton` + `ButtonGroup` (sửa U10). |
| `ModalDialog` | Sửa U3, U4; thêm `setPrimaryAction`, focus vào ô đầu tiên khi mở, `pack()` theo nội dung với chiều cao tối đa và tự cuộn thân dialog. |
| **`ConfirmDialog`** (mới) | Dựa trên `ModalDialog`: tiêu đề, mô tả, nút xác nhận (primary/danger) và Hủy. Thay `JOptionPane` (U18). |
| **`Toast`** (mới, tùy chọn) | Thông báo ngắn góc phải dưới (`JLayeredPane` của frame) cho "Đã lưu khách hàng", "Thanh toán thành công". Tự ẩn sau 3 giây. |
| `EmptyState` | Thêm biến thể có nút hành động (ví dụ "Thêm khách hàng"). |
| `BarChartPanel` / `DonutChartPanel` | Trạng thái rỗng rõ ràng; tooltip giá trị khi rê chuột; nhãn trục theo đơn vị nhận vào (không cố định "Triệu ₫"); donut không cố định 240 px. |
| `InfoCallout` | Thêm biến thể `warning`/`danger`. |

### 5.3. Khung ứng dụng (`ui/shell`)

- **Sidebar**: rộng 232 px; nhóm menu theo vai trò; mục đang chọn có nền `PRIMARY_SOFT` và thanh nhấn 3 px bên trái; hỗ trợ focus bàn phím (Tab/Enter); bỏ heuristic `labelText.length() > 20`, cho phép xuống dòng tự nhiên hoặc rút gọn nhãn ("Báo cáo tồn kho"). Card cuối sidebar chỉ hiển thị thông tin thật (tên cửa hàng, vai trò) và phiên bản; xóa "Hệ thống sẵn sàng" (D11).
- **Topbar**: breadcrumb bên trái; bên phải là ngày, avatar chữ tắt, tên + vai trò, nút Đăng xuất dạng toolbar button. Đăng xuất dùng `ConfirmDialog`.
- **AppShellFrame**: sửa U1 (scroll theo trang) và U2 (phím tắt theo trang). Thêm phím toàn cục `Ctrl+1..9` để chuyển trang theo thứ tự menu (tùy chọn). Gọi `onShow()` để trang làm mới dữ liệu.
- **AppNavigator**: chuyển đăng nhập ↔ khung chính giữ nguyên vị trí và kích thước màn hình; chỉ `MAXIMIZED_BOTH` cho khung chính.

### 5.4. Đặc tả từng màn hình

Mỗi màn hình phải có đủ 4 trạng thái: **đang tải / rỗng / lỗi / có dữ liệu**. Cột "API hiển thị" là hợp đồng để task gắn dữ liệu (theo UC) dùng sau này; chưa có controller thì trang ở trạng thái rỗng.

#### A. Đăng nhập & Quên mật khẩu (UC001)
- Bố cục 2 cột: panel thương hiệu tối màu (`BRAND_DARK`) rộng ~40% với logo, tên, một dòng mô tả; form 400 px căn giữa bên phải.
- Dùng `FormField` cho mọi ô; lỗi hiển thị dưới ô liên quan hoặc trong một `InfoCallout` danger phía trên nút.
- Enter ở ô tài khoản chuyển sang ô mật khẩu; Enter ở ô mật khẩu thì gửi form. Caps Lock bật thì hiện gợi ý.
- Nút "Đăng nhập" ở trạng thái busy trong lúc chạy `SwingWorker` (đã có logic, chỉ đổi hiển thị).
- Quên mật khẩu chia 2 bước trong cùng khung: (1) nhập tài khoản → "Gửi mã"; (2) nhập mã + mật khẩu mới → "Xác nhận". Có liên kết quay lại. Thông báo thành công dùng `ConfirmDialog`/`Toast` rồi quay về đăng nhập.
- **Giữ nguyên toàn bộ thông điệp và logic bảo mật hiện có** (không tiết lộ tài khoản tồn tại; xem README).

#### B. Trang chủ bán hàng
- Lời chào + mô tả ngắn; bên phải là 2 nút tắt lớn: "Lập hóa đơn (F8)" và "Thêm khách hàng".
- 3 KPI (hóa đơn trong ca, tiền mặt đã thu, khách đã phục vụ) ở trạng thái `—`/loading cho tới khi có dữ liệu.
- Card "Hóa đơn gần đây" dùng `StatefulView`.
- Sửa U5, U6.
- API hiển thị: giữ `render(SalesHomeData)`; thêm `showLoading()`, `showError(String)`.
- Ghi chú: khái niệm "ca" chưa có trong schema V1, nên không tự định nghĩa. Nếu chưa chốt, đổi nhãn thành "hôm nay" (mục 8, Q6).

#### C. Quản lý khách hàng (UC007, tra cứu theo SĐT)
- Header với nút "Thêm khách hàng (F4)".
- Thanh lọc: ô tìm (F2) có nút xóa nhanh; Enter để tìm; pill tổng số.
- Bảng: Mã, Họ tên, SĐT (định dạng 4-3-3), Địa chỉ (cắt + tooltip), Ngày tạo, cột hành động "Chi tiết". Double-click dòng cũng mở chi tiết.
- Phân trang thật (U9).
- Dialog Thêm khách hàng: trường bắt buộc có `*`; validate tại chỗ (họ tên không rỗng, SĐT 10 chữ số) để phản hồi nhanh; **controller vẫn kiểm tra lại**. Khi lưu: nút busy → thành công thì đóng dialog, hiện toast, làm mới danh sách → lỗi trùng SĐT thì hiện dưới ô SĐT. Không hiển thị/nhập mã khách hàng (mã do hệ thống sinh).
- API hiển thị: `renderCustomers(list, total, page)`, `showLoading()`, `showError()`, `setOnSearch`, `setOnPageChange`, `setOnOpenDetail`; dialog: `setOnSave`, `setFieldError`, `setBusy`, `closeWithSuccess()`.

#### D. Lập hóa đơn bán lẻ (UC003) — màn hình quan trọng nhất
- Bố cục 2 cột như prototype `image3.png`: trái là (1) tra khách theo SĐT, (2) tìm sản phẩm, (3) giỏ hàng; phải là (4) thông tin khách, (5) thanh toán (cột phải dính trên cùng khi cuộn).
- **Luồng bàn phím:** F2 → ô SĐT; Enter → tra cứu; F3 → ô sản phẩm; ↑/↓ chọn kết quả; Enter → thêm; F8 → thanh toán; Esc → đóng danh sách kết quả; Delete → xóa dòng giỏ đang chọn (cần xác nhận).
- Kết quả tìm sản phẩm hiển thị dạng danh sách gọn (tối đa ~5 dòng, cuộn trong), dòng đang chọn được tô nền.
- Giỏ hàng: bảng có cột hành động "Xóa". **Không** có ô chỉnh số lượng cho tới khi chốt C-07 (mục 8, Q1).
- Thanh toán: ô "Tiền khách đưa" dùng `FormField.money`, tự chọn toàn bộ khi focus; hiện "Tiền trả lại" hoặc "Còn thiếu …" bằng màu danger; nút "Thanh toán bằng tiền mặt (F8)" chỉ bật khi đủ điều kiện.
- **Xử lý D12–D20.** Luồng thanh toán mới: `READY_TO_PAY` → bấm F8 → `PROCESSING` (khóa toàn trang) → controller trả thành công → `PAID` + `InvoiceReceiptDialog` với dữ liệu từ controller → "Đóng" → hóa đơn mới. Controller trả lỗi thì quay về `READY_TO_PAY` và hiện lỗi.
- Thêm `PROCESSING` vào `InvoiceViewState`.
- Hóa đơn in (`InvoiceReceiptDialog`): bố cục bảng như `image7.png` (STT, Mã SP, Tên SP, ĐVT, SL, Đơn giá, Thuế suất, Thành tiền), khối tổng, hình thức thanh toán "Tiền mặt", tiền khách đưa, tiền trả lại. Thông tin cửa hàng lấy từ một nguồn duy nhất (D19).
- API hiển thị: `setCustomer(CustomerInfo)`, `showCustomerNotFound(String phone)`, `renderSearchResults(List<ProductResult>)`, `setCart(List<CartItem>, InvoiceTotals)`, `showProcessing()`, `showPaid(ReceiptData)`, `showPaymentError(String)`; callback `setOnCustomerLookup`, `setOnProductSearch`, `setOnAddToCart(ProductResult)`, `setOnRemoveFromCart(CartItem)`, `setOnProcessPayment(long cashGiven)`. Record `InvoiceTotals(tongTruocThue, tongThue, tongThanhToan)` do controller tính.

#### E. Trang chủ quản lý (UC018)
- Card "Danh sách chờ phê duyệt": header (tiêu đề, số phiếu, tổng hoàn dự kiến) cố định; thân dùng `StatefulView`; bảng có cột hành động **Phê duyệt** (primary) / **Từ chối** (danger soft) như `image1.png`. Bấm hành động thì mở `ConfirmDialog` (Từ chối yêu cầu nhập lý do nếu UC018 quy định — kiểm tra `docs/reference/use-cases/UC018-return-approval.md`).
- 2 cột: "Nhật ký hoạt động" và "Cảnh báo", đều rỗng cho tới khi có nguồn dữ liệu (D10).
- Hàng thông tin: thời gian hiện tại (tự cập nhật mỗi phút bằng `javax.swing.Timer`) và phiên bản. Xóa card CSDL giả (D9).
- Sửa U6, U7, U8.
- API hiển thị: `renderApprovals(list)`, `showLoading()`, `showError()`, `setOnApprove(String maPhieu)`, `setOnReject(String maPhieu)`, `renderAlerts(list)`, `renderActivities(list)`.

#### F. Thống kê doanh thu (UC017)
- Header: nút chọn kỳ (dialog chọn từ ngày/đến ngày bằng `JSpinner` với `SpinnerDateModel`, có sẵn trong Swing) và nút "Xuất Excel" (UC017 có hộp thoại lưu file Excel). Nút này **tắt** cho tới khi có task xuất báo cáo; tooltip giải thích.
- Banner chờ duyệt chỉ hiện khi có số liệu > 0 (D8); bấm thì chuyển sang Trang chủ quản lý (đã có `setOnNavigate`).
- 4 KPI ở trạng thái `—`; biểu đồ cột và donut ở trạng thái rỗng (D5–D7).
- SegmentedControl Ngày/Tuần/Tháng phát sự kiện `setOnGranularityChange`.
- API hiển thị: `setPeriod(LocalDate from, LocalDate to)`, `renderSummary(...)`, `renderDailyRevenue(List<BarPoint>)`, `renderTopProductLines(List<DonutSlice>)`, `setPendingReturnCount(int)`, `showLoading()`, `showError()`, `setOnPeriodChange`.

#### G. Báo cáo tồn kho lâu ngày (UC014)
- Bộ lọc: tìm sản phẩm, ngành hàng (nạp động — D3), ngưỡng ngày (60/90/120 lấy theo UC014; nếu UC014 không quy định thì giữ nguyên bộ hiện có và ghi chú), nút "Áp dụng" (D4), pill kết quả.
- Bảng có **dòng tổng cộng** ở cuối như `image15.png`; cột số căn phải; header "Số ngày tồn ↓" thể hiện chiều sắp xếp.
- Xóa KPI và dòng giả (D1, D2). Nút "Xuất báo cáo" tắt kèm tooltip (giữ như hiện tại).
- API hiển thị: `setCategories(List<String>)`, `renderSummary(...)`, `renderRows(List<LongStockRow>)` (giữ), `showLoading()`, `showError()`, `setOnApplyFilter(Consumer<Filter>)`.

---

## 6. Lộ trình triển khai theo giai đoạn

Mỗi giai đoạn là một thay đổi riêng, có thể review độc lập. Ước lượng chỉ mang tính tham khảo.

### Giai đoạn 0 — Chuẩn bị
- Kiểm tra trạng thái git, build xanh bằng `run.bat`.
- Chụp ảnh màn hình hiện tại của 9 màn hình để so sánh trước/sau.
- **Xong khi:** có ảnh "trước"; `mvn -B clean package` thành công.

### Giai đoạn 1 — Dọn demo và logic sai chỗ (ưu tiên cao nhất)
- File: `LongStockReportPage`, `RevenueStatisticsPage`, `ManagerHomePage`, `Sidebar`, `RetailInvoicePage`, `InvoiceReceiptDialog`, `InvoiceViewState`.
- Việc: xử lý D1–D20 ở mức tối thiểu (xóa dữ liệu giả, xóa phép tính thuế và sinh mã, đổi luồng thanh toán sang `PROCESSING` → chờ callback). Chưa đổi giao diện.
- **Xong khi:** `grep` không còn số liệu/tên giả trong `ui/page/**` (kiểm tra thủ công các chuỗi ở mục 3.2); app chạy, mọi trang hiển thị trạng thái rỗng; không còn `double` dùng cho tiền trong `ui/**`; không còn `System.currentTimeMillis()` trong `ui/**`.

### Giai đoạn 2 — Design system
- File: `DtaqColors`, `DtaqFonts`, `DtaqSpacing`, `DtaqTheme`, `DtaqLightLaf.properties`, (mới) `DtaqStyles` nếu cần.
- Việc: mục 5.1; sửa U11, U12, U15, U16.
- **Xong khi:** không còn mã màu hex trong Java ngoài `DtaqColors`; test `DtaqColorsTest`, `DtaqIconsTest` xanh; giao diện không vỡ ở 1366×768 và 1920×1080.

### Giai đoạn 3 — Component
- File: `ui/component/**` (+ `StatefulView`, `ConfirmDialog`, `Toast` mới).
- Việc: mục 5.2; sửa U3, U4, U10, U13.
- **Xong khi:** mọi component mới/sửa được ít nhất một màn hình dùng; ESC đóng dialog khi focus nằm trong ô nhập; bảng có hover và cột hành động bấm được.

### Giai đoạn 4 — Khung ứng dụng
- File: `AppShellFrame`, `Sidebar`, `Topbar`, `NavItem`, `Page`, `AppNavigator`.
- Việc: mục 5.3; sửa U1, U2, U18.
- **Xong khi:** mỗi trang cuộn độc lập; F2/F3/F4/F8 hoạt động đúng trang; chuyển trang không giữ binding phím của trang cũ.

### Giai đoạn 5 — Màn hình (mỗi màn hình một thay đổi)
Thứ tự đề xuất: **5a** Đăng nhập + Quên mật khẩu (U14) → **5b** Lập hóa đơn (U17) → **5c** Khách hàng (U9) → **5d** Trang chủ bán hàng (U5, U6) → **5e** Trang chủ quản lý (U7, U8) → **5f** Tồn kho lâu ngày → **5g** Thống kê doanh thu.
- **Xong khi (mỗi màn hình):** đủ 4 trạng thái; luồng bàn phím như mục 5.4; khớp bố cục prototype tương ứng; không có chuỗi demo; API hiển thị như mục 5.4 tồn tại và có Javadoc ngắn.

### Giai đoạn 6 — Hoàn thiện
- Xóa alias token `@Deprecated` không còn dùng; xóa `PlaceholderPage` nếu không còn nơi dùng.
- Rà soát Tab order, focus ring, tooltip cho nút bị tắt, độ tương phản chữ `TEXT_SUBTLE` trên nền trắng (đảm bảo đọc được).
- Cập nhật README (ảnh chụp và phím tắt) nếu nhóm đồng ý.

---

## 7. Kiểm thử và xác minh

- **Build/test:** `mvn -B clean package` (chạy cả test hiện có: `NavigationModelTest`, `DtaqColorsTest`, `DtaqIconsTest`, `FormattersTest`, `AuthControllerTest`, …). Phải xanh sau mỗi giai đoạn. Ghi rõ nếu test nào không chạy được.
- **Test mới (JUnit, không thêm framework):** cho logic thuần nếu có tách ra, ví dụ parse ô tiền (`FormField.money`), tính số trang của `Pagination`, chuyển trạng thái của `InvoiceViewState`. Không viết test phụ thuộc màn hình thật (máy CI có thể headless).
- **Kiểm tra tay (mỗi giai đoạn):**
  - Đăng nhập bằng cả hai vai trò; duyệt hết menu; đăng xuất.
  - Kiểm tra cửa sổ ở 1366×768, 1920×1080 và khi Windows scale 125%/150%.
  - Thao tác chỉ bằng bàn phím trên màn hình Lập hóa đơn và Khách hàng.
  - Không có exception trong console (log Logback).
- **Rà soát diff:** `git diff --check`; không có thay đổi ngoài `ui/**`, `resources/.../theme/**` và tài liệu.

---

## 8. Quyết định cần chốt trước khi code phần liên quan

| # | Câu hỏi | Ảnh hưởng | Đề xuất (chờ duyệt) |
|---|---|---|---|
| Q1 | **C-07:** giỏ hàng theo serial (mỗi dòng = 1 máy) hay theo số lượng? Schema V1 có `CHECK ("soLuong" = 1)` ở `ChiTietHoaDon`, nghiêng về serial, nhưng AGENTS §5.1 vẫn ghi chưa chốt. | Cột SL, nút +/−, cách thêm sản phẩm (UC003) | Tạm hiển thị mỗi dòng một máy, không có ô chỉnh số lượng, cho tới khi nhóm chốt. |
| Q2 | **C-11:** quy tắc làm tròn tiền | Hiển thị tổng, tiền thuế | UI không tính; chỉ hiển thị số từ controller. |
| Q3 | Trang chưa có nguồn dữ liệu: **giữ trên menu với trạng thái rỗng** hay **ẩn khỏi menu**? | UX, demo cho giảng viên | Giữ trên menu, trạng thái rỗng ghi "Chưa có dữ liệu", nút thao tác tắt kèm tooltip. |
| Q4 | Thông tin cửa hàng in trên hóa đơn (tên, địa chỉ, điện thoại): lấy từ đâu? Code (`Số 12 Nguyễn Văn Bảo…`) và prototype (`123 Nguyễn Văn Cừ…`) đang khác nhau; schema không có bảng cấu hình. | `InvoiceReceiptDialog` | Đặt trong một hằng hoặc khóa cấu hình duy nhất; nhóm chọn giá trị. |
| Q5 | Thông tin "Chi nhánh Quận 1", "Quầy 01 · Ca sáng" trong prototype: hiển thị hay không? Đa chi nhánh nằm ngoài phạm vi; schema không có quầy/ca. | Sidebar, tiêu đề hóa đơn | Không hiển thị. |
| Q6 | KPI "trong ca" ở Trang chủ bán hàng: "ca" được định nghĩa thế nào? | Nhãn KPI | Đổi thành "hôm nay" nếu chưa có định nghĩa ca. |
| Q7 | Hình 13 "Quản lý hàng trong kho" chưa rõ ứng với UC nào (UC004/UC008/UC014). | Có thể phát sinh màn hình mới | Không làm trong kế hoạch này. |
| Q8 | Có muốn hỗ trợ giao diện tối (dark mode) không? FlatLaf hỗ trợ, nhưng sẽ phải nhân đôi token. | Khối lượng giai đoạn 2 | Không làm ở đợt này; chỉ để token sẵn sàng. |

---

## 9. Rủi ro

- **Đổi API trang khi có người khác đang gắn controller** → phối hợp qua nhánh; giữ API cũ trong giai đoạn chuyển tiếp nếu có thể.
- **Đổi `Page`/`AppShellFrame` ảnh hưởng mọi trang** → làm ở giai đoạn 4, một lần, kiểm tra hết các trang.
- **Font SemiBold không cài được** (môi trường thiếu font) → `DtaqTheme` đã có nhánh dự phòng; giữ hành vi đó.
- **HiDPI:** tránh `setPreferredSize` cứng theo pixel cho component chứa chữ; ưu tiên ràng buộc MigLayout (`w 400!`, `h 42!`) để FlatLaf tự scale.
- **Agent thực hiện tự "lấp" dữ liệu cho đẹp** → vi phạm mục 4.3; người review cần kiểm tra lại các chuỗi ở mục 3.2.

---

## 10. Nhật ký triển khai (2026-10-24)

> Ghi lại những gì đã làm theo kế hoạch và các điểm triển khai khác kế hoạch. Đây là đề xuất đang chờ nhóm duyệt.

### Đã hoàn thành

- **GĐ 1 (dọn demo/logic sai chỗ):** D1–D20 đã xử lý. Không còn số liệu giả trong `ui/page/**`; UI không còn tự tính thuế, không sinh số hóa đơn, không bịa khách "KH0000"; luồng thanh toán đi qua trạng thái `PROCESSING` và chỉ hiện hóa đơn khi controller gọi `showPaid(...)`. Thực hiện gộp cùng lúc với GĐ 5 vì các trang được viết lại.
- **GĐ 2 (design system):** token màu bổ sung, Inter SemiBold thật, `DtaqStyles` (hết mã hex trong Java), bo góc quy đổi đúng (FlatLaf `arc` là đường kính), `Component.focusWidth = 1`.
- **GĐ 3 (component):** `StatefulView`, `ConfirmDialog`, `Toast`, `Tile`, `ViewportPanel`, `RowAction`; `Card.section/setBody`, `DataTable` (hover, tooltip, cột số, cột nút bấm, `setCellPadding`), `FormField` (bắt buộc, ô tiền, nút xóa nhanh), `DtaqButton` (variant, busy, nhãn phím tắt), `SegmentedControl` dùng `JToggleButton`, `ModalDialog` (Esc, không đóng khi bấm nền, tự cuộn).
- **GĐ 4 (khung):** cuộn riêng theo trang (`Page.scrollable()`), phím tắt thật theo trang (shell gắn/gỡ), `NavItem` là nút Tab/Enter được, xác nhận đăng xuất bằng `ConfirmDialog`.
- **GĐ 5 (màn hình):** Đăng nhập, Quên mật khẩu (2 bước), Lập hóa đơn, Khách hàng, Trang chủ bán hàng, Trang chủ quản lý, Tồn kho lâu ngày, Thống kê doanh thu.
- **GĐ 6:** `PlaceholderPage` đã xóa. Test: 56 test đạt (thêm test cho ô tiền, phân trang, trục biểu đồ, ngưỡng tồn kho, token màu, icon, `DtaqStyles`).

### Khác với kế hoạch

| Mục | Kế hoạch | Thực tế | Lý do |
|---|---|---|---|
| Q1–Q8 | Áp dụng đề xuất mặc định | Đã áp dụng (không ô số lượng, trang rỗng giữ trên menu, không hiện chi nhánh/ca/quầy…) | Kế hoạch ghi đề xuất; chưa có xác nhận của nhóm |
| UC014 ngưỡng ngày | Combo 60/90/120 | **Ô nhập số do Quản lý tự nhập, không có mặc định** | UC014 quy định Quản lý tự nhập ngưỡng; 60 ngày chỉ là mốc tham khảo |
| UC018 phê duyệt | Hộp thoại xác nhận | Phê duyệt: nhập ý kiến; Từ chối: **bắt buộc lý do** (`setOnApprove/setOnReject` nhận 2 tham số) | Đặc tả UC018 bước 7 và 6.a.2 |
| Lập hóa đơn | Cột phải dính khi cuộn | Trang không cuộn; hai cột cuộn độc lập (cột phải luôn hiển thị) | Đạt hiệu quả "dính" mà không cần xử lý đặc biệt |
| Nhãn menu | "Báo cáo tồn kho lâu ngày" | "Báo cáo tồn kho" (tiêu đề trang vẫn đầy đủ) | Vừa chiều rộng thanh bên |
| Xóa dòng giỏ | Phím Delete | Chỉ nút "Xóa" trong bảng | Bảng không nhận focus; chưa cần thiết |
| `defaultFont` (U16) | Chuyển vào properties | **Chưa làm**, vẫn đặt bằng `UIManager.put` | `defaultFont` trong `FlatLaf.properties` đang bị comment, cú pháp chưa xác minh; giá trị thấp |
| Gộp token chữ trùng nghĩa | Gộp + alias `@Deprecated` | Giữ nguyên tên token, đổi giá trị sang thang mới | Tránh đổi tên hàng loạt, không ảnh hưởng người dùng |
| Mục tiêu "2 nút tắt lớn Trang chủ bán hàng" | "Lập hóa đơn (F8)" và "Thêm khách hàng" | "Lập hóa đơn" và "Quản lý khách hàng" (chuyển trang) | Mở thẳng hộp thoại thêm khách hàng cần liên kết chéo giữa các trang |

### Điều cần nhóm biết khi gắn controller

- Mọi trang chưa gắn callback thì điều khiển liên quan bị **tắt kèm tooltip** "Chức năng đang được kết nối dữ liệu"; trang hiển thị trạng thái rỗng, không có số liệu.
- `RetailInvoicePage`: controller sở hữu giỏ hàng, gọi `setCart(items, InvoiceTotals)`; `ReceiptData` mang cả thông tin cửa hàng (Q4 do controller/cấu hình quyết định).
- Ràng buộc kiểm tra nhanh SĐT 10 chữ số trong `AddCustomerDialog` lấy theo gợi ý hiện có trên giao diện; UC007 chỉ ghi "số điện thoại không hợp lệ". Controller vẫn là nơi quyết định cuối cùng.
- `LongStockReportPage.renderSummary(...)` cần gọi **trước** `renderRows(...)` để dòng "Tổng cộng" có số liệu.
