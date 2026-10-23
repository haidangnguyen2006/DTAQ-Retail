# KẾ HOẠCH NÂNG CẤP SƯỜN GIAO DIỆN DTAQ THEO FIGMA (`ui_dtaq.fig`)

> **Loại tài liệu:** Kế hoạch triển khai (proposal) cho AI coding agent / lập trình viên tiếp theo.
> **Ngày lập:** 2026-10-23.
> **Nguồn đã thực sự đọc:** `ui_dtaq.fig` (giải mã trực tiếp file nhị phân, 1.870 node, 14 frame), toàn bộ `src/main/java/com/dtaq/retail/ui/*`, `Main.java`, `entity/*`, `session/CurrentSession.java`, `pom.xml`, `V1__initial_schema.sql` (các bảng liên quan), `docs/reference/ui/screen-flow-and-screens.md`, `docs/reference/use-cases/catalog.md`, `docs/reference/requirements/scope-and-requirements.md` (C-01…C-12), skill `agents/skills/java-swing-layered-architecture`.
> **Không truy cập được:** Figma MCP (không có trong phiên). Mọi số đo trong tài liệu này lấy từ file `.fig` đã giải mã — xem Phụ lục C để tái trích xuất.
> **Trạng thái:** CHƯA ĐƯỢC DUYỆT. Các mục đánh dấu **[QUYẾT ĐỊNH]** phải được nhóm xác nhận trước khi code phần liên quan.

---

## Mục lục

0. [Cách dùng tài liệu này (đọc trước)](#0-cách-dùng-tài-liệu-này-đọc-trước)
1. [Mục tiêu và phạm vi](#1-mục-tiêu-và-phạm-vi)
2. [Hiện trạng mã nguồn UI](#2-hiện-trạng-mã-nguồn-ui)
3. [Tổng quan file Figma](#3-tổng-quan-file-figma)
4. [Design tokens](#4-design-tokens)
5. [Thư viện component (spec chi tiết)](#5-thư-viện-component-spec-chi-tiết)
6. [App shell: Sidebar + Topbar + vùng nội dung](#6-app-shell-sidebar--topbar--vùng-nội-dung)
7. [Đặc tả từng màn hình](#7-đặc-tả-từng-màn-hình)
8. [Bản đồ điều hướng & phím tắt](#8-bản-đồ-điều-hướng--phím-tắt)
9. [Kiến trúc mã đề xuất (package, lớp, trách nhiệm)](#9-kiến-trúc-mã-đề-xuất)
10. [Lộ trình triển khai theo giai đoạn (có tiêu chí nghiệm thu)](#10-lộ-trình-triển-khai-theo-giai-đoạn)
11. [Xung đột Figma ↔ tài liệu ↔ schema ↔ code](#11-xung-đột-figma--tài-liệu--schema--code)
12. [Danh sách quyết định cần nhóm duyệt](#12-danh-sách-quyết-định-cần-nhóm-duyệt)
13. [Kiểm thử & checklist so khớp trực quan](#13-kiểm-thử--checklist-so-khớp-trực-quan)
14. [Rủi ro](#14-rủi-ro)
- [Phụ lục A — Bảng màu đầy đủ theo nơi dùng](#phụ-lục-a--bảng-màu-đầy-đủ-theo-nơi-dùng)
- [Phụ lục B — Danh sách icon](#phụ-lục-b--danh-sách-icon)
- [Phụ lục C — Cách giải mã lại file `.fig`](#phụ-lục-c--cách-giải-mã-lại-file-fig)

---

## 0. Cách dùng tài liệu này (đọc trước)

1. **Tuân thủ `AGENTS.md`**: giao tiếp tiếng Việt; chạy `git status --short --branch` trước khi sửa; không commit/push nếu chưa được yêu cầu; không đổi schema/dependency khi chưa duyệt.
2. **"Sườn giao diện" ≠ "nghiệp vụ"**. Kế hoạch này nâng cấp **khung** (theme, component, app shell, bố cục từng màn hình, trạng thái hiển thị, điều hướng, phím tắt). **Không** hiện thực DAO/SQL/nghiệp vụ của UC003–UC019 — mỗi UC đó là một task riêng (AGENTS.md §3). Màn hình nghiệp vụ trong giai đoạn này hiển thị **trạng thái rỗng (empty state)** và nhận dữ liệu qua interface view-model, chưa nối DB.
3. **Dữ liệu mẫu trong Figma (Nguyễn Văn A, 8.000.000, KH0001, HD-091026-013…) chỉ là minh họa.** Không hard-code vào mã chạy thật. Được phép dùng trong test hoặc lớp `*PreviewData` chỉ chạy ở chế độ demo **nếu nhóm đồng ý** ([QUYẾT ĐỊNH] D-09).
4. Làm **theo đúng thứ tự giai đoạn ở mục 10**; mỗi giai đoạn là một thay đổi nhỏ, build được, review được.
5. Khi số đo Figma và trải nghiệm Swing xung đột (ví dụ font metric khác), ưu tiên: (1) màu và phân cấp chữ đúng, (2) khoảng cách/padding đúng, (3) kích thước tuyệt đối gần đúng ±2px.
6. Mọi xung đột ở mục 11 **không được tự giải quyết** bằng cách chọn một bên — giữ hành vi hiện có/an toàn và báo lại.

---

## 1. Mục tiêu và phạm vi

### 1.1 Mục tiêu

- Thay giao diện tối giản hiện tại (`LoginFrame`, `ForgotPasswordFrame`, `MainFrame` chào mừng) bằng giao diện thống nhất theo prototype Figma "DTAQ • Prototype quản lý siêu thị điện máy".
- Xây **hệ thống design token + component tái sử dụng** để các task UC sau chỉ việc lắp ráp.
- Xây **app shell một cửa sổ** (sidebar theo vai trò + topbar + vùng nội dung đổi trang bằng `CardLayout`) thay cho việc mở/đóng nhiều `JFrame`.
- Dựng **khung tĩnh của 9 màn hình/hộp thoại** có trong Figma, với đầy đủ trạng thái hiển thị (rỗng, đang tải, lỗi, có dữ liệu).

### 1.2 Trong phạm vi

| # | Hạng mục | Ghi chú |
|---|---|---|
| S1 | Theme FlatLaf tùy biến + token màu/chữ/khoảng cách | Không thêm dependency nếu chưa duyệt (D-01, D-02) |
| S2 | Bộ icon (22 icon Lucide) | D-02 |
| S3 | Component dùng chung (button, input, card, table, pill, callout, dialog, KPI, chart, pagination, empty state…) | |
| S4 | App shell + điều hướng theo vai trò + đăng xuất có xác nhận (UC002) | Thay `MainFrame` |
| S5 | Restyle Đăng nhập (UC001) & Quên mật khẩu — **giữ nguyên controller/logic** | |
| S6 | Khung màn hình: Trang chủ NVBH, Trang chủ QL, Quản lý khách hàng + hộp thoại Thêm KH, Lập hóa đơn bán lẻ (5 trạng thái) + hộp thoại hóa đơn, Thống kê doanh thu, Báo cáo tồn kho lâu ngày | Chỉ UI + view-model interface |

### 1.3 Ngoài phạm vi (không làm trong kế hoạch này)

- DAO/SQL/controller nghiệp vụ cho khách hàng, hóa đơn, đổi trả, báo cáo, thống kê.
- Sinh mã định danh, tính tiền/thuế/làm tròn (C-11), khuyến mãi (C-04), tồn kho serial/số lượng (C-07).
- Xuất Excel/PDF, in hóa đơn thật (chỉ dựng panel xem trước nếu D-08 duyệt).
- Mọi thứ AGENTS.md §3 loại trừ (thanh toán online/QR, đa chi nhánh, công nợ, trả góp…).
- Đổi schema DB, thêm migration.

---

## 2. Hiện trạng mã nguồn UI

| Tệp | Hiện trạng | Hành động trong kế hoạch |
|---|---|---|
| `Main.java` | `FlatLightLaf.setup()`, migration, rồi `AppNavigator.showLogin()` trên EDT | Đổi sang cài theme DTAQ (`DtaqTheme.install()`) — 1 dòng |
| `ui/AppNavigator.java` | Điều phối 3 `JFrame`: Login, ForgotPassword, Main; `showMain` set `CurrentSession`; `logout()` | Giữ API công khai; `showMain` mở `AppShellFrame` thay `MainFrame` |
| `ui/LoginFrame.java` | MigLayout 2 cột `[360!][grow]`, brand `#0F766E`, lỗi `#DC2626`, `SwingWorker` gọi `AuthController.login` | Restyle theo Figma; **giữ nguyên** luồng `submit()`, thông báo lỗi, `setName(...)` của field |
| `ui/ForgotPasswordFrame.java` | Có nút "Gửi mã xác nhận" + 3 field + "Xác nhận" | Restyle; **giữ nút Gửi mã** (Figma thiếu — xem X-01) |
| `ui/MainFrame.java` | Cửa sổ chào mừng 800×520 + nút đăng xuất có `JOptionPane` xác nhận | Thay bằng `AppShellFrame`; giữ hộp thoại xác nhận đăng xuất (UC002) |
| `entity/NhanVien` | record; `moTaQuyen()` → "Quản lý"/"Nhân viên bán hàng" | Dùng cho topbar |
| `entity/LoaiQuyen` | `NHAN_VIEN_QUAN_LY`, `NHAN_VIEN_BAN_HANG` | Dùng chọn bộ menu sidebar |
| `session/CurrentSession` | static holder | Shell đọc qua tham số `NhanVien` (không đọc static trong component) |

**Stack đã xác minh trong `pom.xml`:** Java 21, FlatLaf 3.6, MigLayout 11.4.2, JUnit Jupiter 5.12.2. **Không có** `flatlaf-extras`, `flatlaf-fonts-inter`, thư viện SVG hay chart.

**Quy ước code phải giữ** (từ code hiện có): Javadoc một dòng tiếng Việt cho lớp; hằng `private static final`; `Objects.requireNonNull(x, "x")`; `SwingWorker` cho việc chạm DB; logger SLF4J `private static final Logger log`; field có `setName("...")` để test/tra cứu; MigLayout cho bố cục.

---

## 3. Tổng quan file Figma

- File: `ui_dtaq.fig` (zip: `canvas.fig` kiwi v106, `meta.json`, `thumbnail.png`, 4 ảnh PNG). Tên gốc trong meta: "Untitled (Copy)". Một trang "Page 1".
- Tất cả frame màn hình rộng **1440 px**, cao 960–1151 px, nền app `#F4F6F8`. Font duy nhất: **Inter** (Regular/Medium/Semi Bold/Bold/Italic).
- Có **51 tương tác prototype** (ON_CLICK → NAVIGATE, DISSOLVE 300ms) và **2 điểm bắt đầu flow**: `NVBH` và `QL` (cả hai bắt đầu từ frame "Đăng nhập").
- Auto-layout được dùng nhất quán (hướng H/V, gap, padding) → ánh xạ rất tự nhiên sang MigLayout.

### 3.1 Danh sách frame

| ID frame | Tên frame trong Figma | Kích thước | Vai trò | UC liên quan | Ánh xạ màn hình mã |
|---|---|---|---|---|---|
| `1:1524` | Đăng nhập (flow NVBH) | 1440×960 | — | UC001 | `LoginFrame` |
| `2003:21` | Đăng nhập (flow QL, tài khoản `linh.quanly`) | 1440×960 | — | UC001 | `LoginFrame` (cùng màn) |
| `2028:2` | Đổi mật khẩu | 1440×960 | — | (Quên mật khẩu, ngoài 19 UC — G) | `ForgotPasswordFrame` |
| `1:1380` | Trang chủ — Nhân viên bán hàng | 1440×960 | NVBH | (Hình 8 DOCX) | `SalesHomePage` |
| `1:1200` | Trang chủ — Quản lý | 1440×1088 | QL | UC018 (danh sách chờ duyệt) | `ManagerHomePage` |
| `2030:50` | Trang chủ — Thống kê (sidebar active "Thống kê doanh thu") | 1440×1151 | QL | UC017 (+UC015) | `RevenueStatisticsPage` |
| `1:2` | Báo cáo tồn kho lâu ngày — Quản lý | 1440×1019 | QL | UC014 | `LongStockReportPage` |
| `1:368` | Quản lý khách hàng | 1440×960 | NVBH | UC007/UC005 | `CustomerListPage` |
| `1:176` | Quản lý khách hàng — Thêm khách hàng (modal) | 1440×990 | NVBH | UC007 | `AddCustomerDialog` |
| `1:1054` | Lập hóa đơn — Chưa chọn khách hàng | 1440×960 | NVBH | UC003 | `RetailInvoicePage` state `NO_CUSTOMER` |
| `2003:231` | Lập hóa đơn — chọn khách hàng | 1440×960 | NVBH | UC003 | state `CUSTOMER_SELECTED` |
| `1:878` | Lập hóa đơn — Khách hàng và kết quả tìm tivi | 1440×1009 | NVBH | UC003/UC004 | state `PRODUCT_RESULTS` |
| `1:716` | Lập hóa đơn — Sẵn sàng thanh toán | 1440×960 | NVBH | UC003 | state `READY_TO_PAY` |
| `1:523` | Lập hóa đơn — Thanh toán thành công (modal ảnh hóa đơn) | 1440×960 | NVBH | UC003 (+Hình 9 "In hoá đơn") | state `PAID` + `InvoiceReceiptDialog` |
| `2021:7` | Group 2 (ghép ảnh hóa đơn, không phải màn hình) | 720×725 | — | — | Chỉ tham khảo bố cục hóa đơn in |

### 3.2 Ảnh nhúng

4 ảnh PNG đều là **mẫu "HÓA ĐƠN BÁN LẺ"** (1440×1450, 720×754, 360×363, 360×377) — dùng trong modal "Thanh toán thành công". Bố cục hóa đơn in (xem 7.11):

- Trái trên: "SIÊU THỊ ĐIỆN MÁY DTAQ" (đậm), địa chỉ cửa hàng 2 dòng, "ĐT: …". Phải trên: "Số hóa đơn: …", "Ngày lập: dd/MM/yyyy HH:mm".
- Tiêu đề giữa: "HÓA ĐƠN BÁN LẺ" (đậm, lớn).
- Khối thông tin: Họ tên khách hàng (kèm mã), Số điện thoại, Địa chỉ, Nhân viên bán hàng — mỗi dòng có gạch chân.
- Bảng có viền: STT | Mã SP | Tên sản phẩm | ĐVT | SL | Đơn giá (VNĐ) | Thuế suất | Thành tiền (VNĐ); header nền xám nhạt.
- Tổng: "Tổng tiền hàng trước thuế", "Tổng tiền thuế (VAT x%)", kẻ đôi, "TỔNG THANH TOÁN: … VNĐ" (đậm).
- "Hình thức thanh toán: Tiền mặt", "Tiền khách đưa", "Tiền trả lại khách".
- Chân: "Ghi chú: Cảm ơn quý khách đã mua hàng tại Siêu thị điện máy DTAQ!".

---

## 4. Design tokens

> Tạo **một nguồn duy nhất** cho token: lớp `ui/theme/DtaqColors`, `DtaqFonts`, `DtaqSpacing` (hằng Java) **và** file `DtaqLightLaf.properties` (cho FlatLaf). Không viết mã màu hex rải rác trong màn hình. Thay `BRAND_BACKGROUND = 0x0F766E` và `ERROR_FOREGROUND = 0xDC2626` hiện có bằng token.

### 4.1 Màu (trích chính xác từ Figma)

| Token | Hex | Dùng ở đâu trong Figma |
|---|---|---|
| `PRIMARY` | `#087F75` | Nút chính, logo nền, icon active, chỉ báo "Hệ thống sẵn sàng", cột biểu đồ, viền input focus |
| `PRIMARY_STRONG` (text on soft) | `#075E57` | Chữ menu active, chữ pill thành công, tổng thanh toán lớn, chữ callout |
| `PRIMARY_SOFT` | `#E8F5F2` | Nền menu active (QL), pill thành công, callout info, avatar, nền dòng kết quả được chọn, segmented active, ô icon nhật ký |
| `PRIMARY_SOFT_2` | `#D6EEEA` | Nền menu active (NVBH — xem X-08), chữ phím tắt trên nút chính |
| `PRIMARY_BORDER_SOFT` | `#B5DCD6` | Viền dòng kết quả tìm kiếm đang chọn |
| `BRAND_DARK` | `#163E43` | Panel thương hiệu bên trái màn đăng nhập |
| `BRAND_ON_DARK_MUTED` | `#BDDBD6` | Chữ "ĐIỆN MÁY" trên nền tối |
| `TEXT` | `#18252D` | Chữ chính, tiêu đề, nội dung ô bảng |
| `TEXT_MUTED` | `#667580` | Nhãn phụ, mô tả, header bảng, icon mặc định |
| `TEXT_SUBTLE` | `#93A0AA` | Placeholder, nhóm điều hướng, phiên bản, dấu "/", chữ nút disabled, trục biểu đồ |
| `BG_APP` | `#F4F6F8` | Nền cửa sổ, header bảng, pill trung tính, thẻ "Thông tin quầy" |
| `SURFACE` | `#FFFFFF` | Card, sidebar, topbar, input, dialog |
| `BORDER` | `#E2E8EC` | Mọi viền 1px: card, input, bảng, divider, nút phụ |
| `DISABLED_BG` | `#E5EBEF` | Nền nút disabled (Thanh toán, Trước/Sau) |
| `DANGER` | `#B42318` | Chữ nút "Từ chối", icon cảnh báo trang chủ QL |
| `DANGER_SOFT` | `#FEE4E2` | Nền nút "Từ chối" |
| `DANGER_STRONG` | `#7F0808` | Chữ & icon khối "Cảnh báo quan trọng" (Thống kê) |
| `DANGER_SOFT_2` | `#F5E8E8` | Nền ô icon cảnh báo |
| `OVERLAY` | `#152A3D` @ 40% | Lớp phủ sau modal |
| `SHADOW` | `#102635` @ 15% | Bóng dialog (blur 56, offset y 16) |
| `CHART_1..5` | `#087F75`, `#439D93`, `#79B8AE`, `#ABD1C9`, `#D4E7E2` | Donut + chú giải top 5 |
| `LINK_ACCENT` | `#00B3A4` | Chỉ 1 chỗ: link "Cần Đổi/Trả hàng ?" (đề xuất thay bằng `PRIMARY`, X-09) |

**Màu lỗi cho validation:** Figma **không** có trạng thái lỗi input. Đề xuất dùng `DANGER #B42318` cho chữ lỗi và viền input lỗi (thay `#DC2626` hiện có) — [QUYẾT ĐỊNH] D-10 (mặc định: dùng `DANGER`).

### 4.2 Typography

Font: **Inter** (hỗ trợ đầy đủ dấu tiếng Việt). [QUYẾT ĐỊNH] D-01 cách nạp font. Fallback: `Segoe UI` (Windows) → font mặc định FlatLaf.

| Token | Font / size | Letter-spacing | Dùng cho |
|---|---|---|---|
| `DISPLAY` | Bold 32 | −1 | Tiêu đề màn đăng nhập "Đăng nhập DTAQ", "Cập nhật mật khẩu" |
| `AMOUNT_XL` | Regular 32 | −1 | Số "Tổng thanh toán" (màu `PRIMARY_STRONG`) |
| `PAGE_TITLE` | Regular 28 | −0.7 | Tiêu đề trang |
| `KPI_VALUE` | Regular 29 | −0.8 | Giá trị thẻ chỉ số |
| `DIALOG_TITLE` | Regular 24 | −0.6 | Tiêu đề hộp thoại |
| `DONUT_CENTER` | Semi Bold 24 | 0 | "285M" giữa donut |
| `BRAND` | Regular 23 | +1 | Chữ "DTAQ" |
| `SECTION_TITLE_STRONG` | Semi Bold 18 | 0 | Tiêu đề card trang QL ("Danh sách chờ phê duyệt", "Nhật ký hoạt động", "Doanh thu theo ngày") |
| `CUSTOMER_NAME` | Semi Bold 18 | 0 | Họ tên khách trong card "Thông tin khách hàng" |
| `CARD_TITLE` | Regular 16 | 0 | Tiêu đề khối ("Giỏ hàng", "Thanh toán", "Thông tin khách hàng", "Hóa đơn gần đây trong ca") |
| `CARD_TITLE_SEMI` | Semi Bold 16 | 0 | Thẻ thông tin hệ thống ("Thời gian hiện tại", "Kết nối CSDL", "Phiên bản") |
| `EMPTY_TITLE` | Medium 15 | 0 | "Chưa có sản phẩm trong giỏ" |
| `BODY_14` | Regular 14 | 0 | Mô tả trang, giá trị input, placeholder |
| `BUTTON` | Semi Bold 14 | 0 | Nhãn nút cao 42 |
| `AMOUNT_ROW` | Medium 14 | 0 | Giá trị các dòng "Tổng trước thuế/Tổng tiền thuế" |
| `BODY_13` | Regular 13 | 0 | Nội dung ô bảng, menu, breadcrumb, nhãn KPI |
| `BODY_13_MEDIUM` | Medium 13 | 0 | Cột mã (cột đầu) trong bảng, breadcrumb trang hiện tại, tên sản phẩm kết quả |
| `LABEL` | Semi Bold 13 | 0 | Nhãn field ("Tài khoản", "Tìm khách hàng") |
| `TABLE_HEADER` | Semi Bold 12 | 0 | Tên cột |
| `CAPTION` | Regular 12 | 0 | Diễn giải KPI, hướng dẫn, ngày topbar |
| `PILL` | Medium 12 | 0 | Pill trạng thái |
| `SMALL_BUTTON` | Semi Bold 12 | 0 | Nút nhỏ cao 31 ("Phê duyệt", "Từ chối", segmented) |
| `EYEBROW` | Semi Bold 12 | +1.2 | "CHÀO MỪNG TRỞ LẠI" (màu `PRIMARY`) |
| `HINT` | Regular 11 | 0 | Phím tắt "F2", "F3 · Enter để thêm", điều kiện thanh toán, trục biểu đồ |
| `HINT_ON_PRIMARY` | Medium 11 | 0 | "F4", "F8", "↵" trên nút chính |
| `OVERLINE` | Semi Bold 10 | +1.2…1.3 | "KHÔNG GIAN LÀM VIỆC", "ĐIỆN MÁY" |
| `MICRO` | Regular 10 | 0 | "Nội bộ DTAQ · Phiên bản 1.0", vai trò dưới tên user |
| `ITALIC_HINT` | Italic 12 | 0 | "Bấm vào để xem chi tiết" |

Line-height: phần lớn 100%; đoạn nhiều dòng dùng 1.5–1.7 (Swing `JLabel` HTML hoặc `JTextArea` không viền nếu cần xuống dòng — ví dụ cột "Lý do đổi/trả", địa chỉ khách).

> Swing không có letter-spacing trên `JLabel` thường; dùng `font.deriveFont(Map.of(TextAttribute.TRACKING, value/size))` (tracking = px / fontSize). Chỉ áp cho các token có tracking ≠ 0.

### 4.3 Khoảng cách, bo góc, kích thước

| Token | Giá trị | Nơi dùng |
|---|---|---|
| `RADIUS_S` | 4 | Ô số lượng nhỏ trong giỏ |
| `RADIUS_M` | 6 | Button, input, menu item, callout, nút nhỏ |
| `RADIUS_TABLE` | 7 | Khung bảng |
| `RADIUS_CARD_SM` | 8 | Logo, thẻ "Thông tin quầy", ô icon 36×36 |
| `RADIUS_L` | 10 | Card nội dung, thẻ KPI |
| `RADIUS_DIALOG` | 14 | Hộp thoại |
| `RADIUS_PILL` | 100 (full) | Pill trạng thái, avatar 32 (r16), chỉ báo 6×6 |
| `CONTROL_H` | 44 | Input |
| `BUTTON_H` | 42 | Button chuẩn |
| `BUTTON_H_SM` | 31 | Nút nhỏ (Phê duyệt/Từ chối/segmented) |
| `PILL_H` | 25 | Pill (padding 5/9) |
| `TOPBAR_H` | 72 | Topbar |
| `SIDEBAR_W` | 224 | Sidebar |
| `NAV_ITEM_H` | 46 (64 khi nhãn 2 dòng) | Mục menu |
| `TABLE_HEADER_H` | 44 (27 ở bảng duyệt đổi trả) | Header bảng |
| `TABLE_ROW_H` | 61–62 (48 ở bảng hóa đơn gần đây; 84 khi tên SP 2 dòng) | Dòng bảng |
| `PAGE_PADDING` | 32 | Padding vùng nội dung |
| `SECTION_GAP` | 24 | Khoảng cách giữa các khối trang |
| `CARD_PADDING` | 24 (trang danh sách) / 22 (KPI, card QL) / 20 (card màn hóa đơn) | |
| `CARD_GAP` | 20 (giữa phần tử trong card), 16 (card QL) | |
| `FIELD_LABEL_GAP` | 8 | Nhãn → input |
| `FORM_FIELD_GAP` | 20 (dialog) / 28 (màn đăng nhập) | |
| `KPI_GAP` | 20 (3 thẻ) / 16 (4 thẻ) | |

---

## 5. Thư viện component (spec chi tiết)

Đặt trong `com.dtaq.retail.ui.component`. Mỗi component là lớp nhỏ, **không** biết DB/controller, chỉ nhận dữ liệu hiển thị và phát sự kiện (listener/`Runnable`/`Consumer`).

### 5.1 `DtaqButton` (factory: `primary`, `secondary`, `danger`, `small…`)

| Biến thể | Nền | Chữ | Viền | Kích thước |
|---|---|---|---|---|
| Primary | `#087F75` | `#FFFFFF` Semi Bold 14 | không (Figma có viền trắng 1px — bỏ qua) | cao 42, padding ngang 16, r6, gap icon 10 |
| Secondary | `#FFFFFF` | `#18252D` Semi Bold 14 | `#E2E8EC` 1px | cao 42, padding 16, r6 |
| Disabled (mọi loại) | `#E5EBEF` | `#93A0AA` | — | |
| Primary small | `#087F75` | trắng Semi Bold 12 | — | cao 31, padding 8/7, r6 |
| Danger-soft small | `#FEE4E2` | `#B42318` Semi Bold 12 | — | cao 31, padding 8/7, r6 |
| Segmented (active) | `#E8F5F2` | `#075E57` Semi Bold 12 | — | cao 31, padding 8/12, r6 |
| Segmented (inactive) | trắng | `#667580` Semi Bold 12 | `#E2E8EC` | như trên |
| Link | trong suốt | `#667580` Regular 12 (hoặc `PRIMARY`) | — | con trỏ tay |

- Hỗ trợ **icon trái** (17px) và **nhãn phím tắt phải** (`F4`, `F8`, `↵`: Medium 11, màu `#D6EEEA` trên primary; khi disabled màu `#93A0AA`).
- Hover/pressed không có trong Figma → dùng FlatLaf mặc định suy ra từ màu nền (`hoverBackground` = darken 5%, `pressedBackground` = darken 10%).
- Cài bằng `FlatClientProperties.STYLE` hoặc style class trong `.properties` (FlatLaf ≥ 2.x hỗ trợ `[style]Button.primary = …`; **kiểm chứng cú pháp với FlatLaf 3.6 trước khi dùng**).

### 5.2 `FormField` (nhãn + input + hint + phím tắt)

```
┌ Nhãn (Semi Bold 13 #18252D)        Phím tắt (Regular 11 #667580, căn phải) ┐
│ gap 8                                                                      │
│ [icon 18 #667580] gap 10  Giá trị / placeholder (Regular 14)               │ ← cao 44, r6, viền #E2E8EC 1px, padding ngang 12
│ gap 8 (tùy chọn)                                                           │
└ Hướng dẫn (Regular 12 #667580) / Lỗi (Regular 12 DANGER)                   ┘
```

- Trạng thái focus: viền `#087F75` **1.5px** (Figma). FlatLaf: `Component.focusedBorderColor=#087F75`, `Component.focusWidth=0`, `Component.innerFocusWidth=1` (≈1.5px nhìn thấy) — điều chỉnh để giống.
- Placeholder `#93A0AA`: `JTextField.placeholderText`.
- Icon trái: `JTextField.leadingIcon` (FlatLaf ≥ 2.0).
- Mật khẩu: `JPasswordField` + `PasswordField.showRevealButton=true` (tùy chọn).
- Dấu `*` bắt buộc là một phần của nhãn ("Họ và tên *").
- Biến thể **dropdown**: `JComboBox` cùng kích thước; Figma đặt icon `chevron-down` **bên trái** chữ (báo cáo tồn kho) — đề xuất dùng mũi tên mặc định bên phải của FlatLaf (X-10).
- API gợi ý: `FormField.text(label, placeholder, icon)`, `.password(...)`, `.combo(label, items)`, `setHint(String)`, `setError(String)`, `setShortcut(String)`, `getField()`.

### 5.3 `Card` (RoundedPanel)

- Nền trắng, viền `#E2E8EC` 1px, bo r10, padding theo tham số (20/22/24), layout dọc MigLayout `insets p, gap g`.
- Vẽ bằng `paintComponent` + `RoundRectangle2D` (antialias); `setOpaque(false)`.
- Biến thể `Card.titled(title, trailingText)` → hàng tiêu đề: trái `CARD_TITLE` (Regular 16), phải chữ phụ Regular 12 `#667580` (ví dụ "1 sản phẩm · 1 đơn vị", "Hôm nay").

### 5.4 `KpiCard`

- Card r10, padding 22, gap 12, cao 134. Dòng 1 nhãn Regular 13 `#667580`; dòng 2 giá trị `KPI_VALUE`; dòng 3 diễn giải Regular 12 `#667580` (hoặc `#087F75` khi là xu hướng tăng "↑ 12,4% so với kỳ trước").
- Trạng thái loading: giá trị "—"; lỗi: "Không tải được".

### 5.5 `StatusPill`

- Cao 25, padding 5/9, bo tròn hoàn toàn, chữ Medium 12.
- Tone `NEUTRAL`: nền `#F4F6F8`, chữ `#667580` (ví dụ "Bản nháp", "8 khách hàng", "6 kết quả").
- Tone `SUCCESS`: nền `#E8F5F2`, chữ `#075E57` ("Đã thanh toán", "Tồn kho sau bán: 11", trang hiện tại phân trang).
- (Đề xuất thêm) `DANGER`: nền `#FEE4E2`, chữ `#B42318` — chỉ dùng khi có trạng thái lỗi được đặc tả.

### 5.6 `InfoCallout`

- Nền `#E8F5F2`, r6, padding 14, gap 10; icon `info` 17 `#087F75`; chữ Regular 12 `#075E57`, cho phép xuống dòng.
- Dùng ở: dialog Thêm KH ("Các trường có dấu * là bắt buộc. Mã khách hàng sẽ được tạo tự động khi lưu."), giỏ hàng ("Đơn giá và thuế suất được khóa theo sản phẩm. Không chỉnh giá hoặc áp dụng giảm giá tại quầy.").

### 5.7 `DataTable` (JTable được style)

| Phần | Spec |
|---|---|
| Khung | viền `#E2E8EC` 1px, bo r7 (bọc `JScrollPane` trong panel bo góc) |
| Header | nền `#F4F6F8`, cao 44, chữ Semi Bold 12 `#667580`, padding trái/phải 16, gap cột 12; header 2 dòng được phép ("Đơn giá\ntrước thuế") |
| Dòng | nền trắng, **viền trên** 1px `#E2E8EC` (Figma: `borderTop` độc lập), cao 61–62, padding dọc 14 |
| Ô | Regular 13 `#18252D`; **cột đầu (mã) Medium 13**; số/tiền **căn phải** |
| Cột hành động | chữ "Chi tiết" (link) hoặc cặp nút nhỏ |
| Chọn dòng | Figma không có → dùng `PRIMARY_SOFT` làm nền chọn, chữ `TEXT` |
| Hàng tổng | cùng style dòng; đề xuất chữ Semi Bold để phân biệt (Figma dùng Medium ở cột đầu) |
| Không lưới dọc | `setShowVerticalLines(false)`, `setIntercellSpacing(new Dimension(12,0))` |

- Cung cấp renderer dùng chung: `TextCellRenderer`, `CodeCellRenderer` (Medium), `MoneyCellRenderer` (căn phải, `MoneyFormat`), `TwoLineCellRenderer` (tên + SĐT), `ActionCellRenderer/Editor` (nút trong ô).
- Chiều rộng cột cấu hình theo **tỉ lệ** lấy từ Figma (xem từng màn hình mục 7), cột "tên/địa chỉ" là cột co giãn.

### 5.8 `Pagination`

- Trái: "Hiển thị 1–8 trên 8 khách hàng" Regular 12 `#667580`.
- Phải (gap 8): nút secondary "Trước" (73×42), pill trang hiện tại (tone SUCCESS, "1"), nút "Sau" (58×42). Disabled khi không còn trang (Figma đang vẽ ở trạng thái disabled).

### 5.9 `EmptyState`

- Căn giữa, dọc gap 12: vòng tròn 46×46 nền `#F4F6F8` chứa icon 23 `#93A0AA`; tiêu đề Medium 15 `#18252D`; hướng dẫn Regular 12 `#667580`.
- Ví dụ: giỏ trống ("Chưa có sản phẩm trong giỏ" / "Nhấn F3 để tìm sản phẩm và bắt đầu lập hóa đơn.").

### 5.10 `ModalDialog`

- `JDialog` modal, không trang trí (`setUndecorated(true)`) đặt trên **glass pane overlay** `#152A3D` 40% của shell; hoặc `JLayeredPane` overlay trong cùng cửa sổ. Click overlay = Hủy (theo prototype), `Esc` = Hủy.
- Thân: nền trắng, r14, padding 32, gap 24, bóng `#102635` 15% blur 56 y+16 (vẽ bóng mềm thủ công hoặc bỏ nếu tốn công — ưu tiên thấp).
- Header: tiêu đề `DIALOG_TITLE` + mô tả Regular 13 `#667580` (gap 10), nút đóng `x` 20px góc phải.
- Footer: divider 1px `#E2E8EC`, rồi hàng nút căn phải gap 12 (Secondary "Hủy" + Primary hành động).
- Thêm KH: rộng 600; Hóa đơn thành công: rộng 568.

### 5.11 `PageHeader`

- Hàng ngang: trái khối (tiêu đề `PAGE_TITLE` + mô tả Regular 14 `#667580`, gap 8), phải phần tử trailing (nút hành động, pill trạng thái, hoặc nút ngày có icon `calendar-days`). Căn giữa theo chiều dọc.

### 5.12 `ShortcutBar`

- Hàng chữ Regular 11 `#667580` gap 20: "F2  Khách hàng", "F3  Sản phẩm", "F8  Thanh toán", "↑ ↓  Chọn kết quả".

### 5.13 `IconTile` + `ActivityItem`

- Ô 36×36 r8, nền `#E8F5F2` (icon `clock` `#087F75`) hoặc `#F5E8E8` (icon `alert-circle` `#B42318`). Bên phải: tiêu đề Regular 13 `#18252D`, phụ Regular 12 `#667580` (gap 2). Item cách nhau 12.

### 5.14 `BarChartPanel` và `DonutChartPanel` (Java2D tự vẽ, không thêm thư viện)

- **Bar chart** (khu vực 1108×250): nhãn đơn vị "Triệu ₫" góc trái trên (Regular 11 `#93A0AA`); trục Y 5 mốc (0/20/40/60/80) căn phải rộng 56; vùng cột rộng 1028 cao 190, mỗi ngày một ô 147 px, cột rộng 72 bo r6 màu `PRIMARY`, nhãn giá trị phía trên cột (Regular 12 `PRIMARY`, căn giữa); trục X nhãn ngày Regular 11 `#667580` căn giữa. Không có lưới ngang trong Figma.
- **Donut** 220×220, `innerRadius = 0.72`, bắt đầu từ −90° (12 giờ), theo chiều kim đồng hồ, 5 lát theo `CHART_1..5`; giữa: giá trị `DONUT_CENTER` + "Tổng doanh thu" Regular 12 `#667580`.
- **Legend** mỗi dòng: ô màu 10×10 r2, tên Regular 14 (co giãn), giá trị Semi Bold 14 căn phải rộng 80, % Regular 13 `#667580` căn phải rộng 60, khoảng cách dòng 12.
- Chart nhận `List<ChartPoint(label, value)>`; tự tính thang trục Y (làm tròn lên bội số đẹp). **Không** tự tính doanh thu.

### 5.15 Sidebar/Topbar → xem mục 6.

---

## 6. App shell: Sidebar + Topbar + vùng nội dung

### 6.1 Bố cục tổng

```
┌──────────── 1440 ─────────────────────────────────────────────────────────┐
│ Sidebar 224 │ Topbar 72 (trắng, viền dưới #E2E8EC)                        │
│ (trắng,     ├─────────────────────────────────────────────────────────────┤
│  viền phải  │ Nội dung trang (nền #F4F6F8, padding 32, gap dọc 24)        │
│  #E2E8EC)   │  → JScrollPane dọc, không cuộn ngang                        │
└─────────────┴─────────────────────────────────────────────────────────────┘
```

- `AppShellFrame extends JFrame`: `BorderLayout` — WEST `Sidebar`, CENTER panel dọc (NORTH `Topbar`, CENTER `JScrollPane(contentCards)` với `CardLayout`).
- Kích thước: preferred 1440×960, **minimum 1280×760**, mở ở trạng thái **maximized** (đề xuất; D-07). Nội dung co giãn ngang; card và bảng dùng `growx`.

### 6.2 Sidebar (`Sidebar`)

Auto-layout dọc, padding top 24 / ngang 16 / bottom 22, gap 28:

1. **Thương hiệu** (cao 42, lề trái 10): logo 36×36 nền `#087F75` r8 + icon `zap` trắng 22; bên phải (gap 10) "DTAQ" (`BRAND`, `#18252D`) trên "ĐIỆN MÁY" (`OVERLINE`, `#667580`).
2. **Nhóm điều hướng**: overline "KHÔNG GIAN LÀM VIỆC" (`#93A0AA`), gap 10, rồi các `NavItem`.
   - `NavItem`: cao 46, r6, padding 12, gap 10, icon 19 + nhãn Regular 13.
   - Mặc định: nền trong suốt, icon & chữ `#667580`.
   - Active (chuẩn hóa — X-08): nền `#E8F5F2`, icon `#087F75`, chữ `#075E57` **Semi Bold 13**.
   - Hover (Figma không có): nền `#F4F6F8`.
   - Nhãn dài tự xuống dòng (item cao 64 — "Báo cáo tồn kho lâu ngày").
3. **Khoảng linh hoạt** (push).
4. **Thẻ thông tin quầy**: nền `#F4F6F8` r8 padding 14 gap 10: dòng 1 Semi Bold 12 `#18252D` ("DTAQ • Chi nhánh Quận 1"), dòng 2 Regular 12 `#667580` (NVBH: "Quầy 01 · Ca sáng"; QL: "Không gian quản lý"), dòng 3 chấm tròn 6×6 `#087F75` + "Hệ thống sẵn sàng" Regular 11 `#087F75`. → **[QUYẾT ĐỊNH] D-05** (chi nhánh/quầy/ca không có trong schema & đa chi nhánh ngoài phạm vi).
5. **Phiên bản**: "Nội bộ DTAQ · Phiên bản {AppInfo.version()}" Regular 10 `#93A0AA`.

**Menu theo vai trò** (từ Figma):

| Vai trò (`LoaiQuyen`) | Mục | Icon | Trang đích |
|---|---|---|---|
| `NHAN_VIEN_BAN_HANG` | Trang chủ | `layout-dashboard` | `SalesHomePage` |
| | Quản lý khách hàng | `users` | `CustomerListPage` |
| | Lập hóa đơn bán lẻ | `receipt-text` | `RetailInvoicePage` |
| | ~~Thống kê~~ | (`receipt-text`) | **Không đưa vào** — mục thừa trong Figma, trỏ nhầm sang Lập hóa đơn (X-07) |
| `NHAN_VIEN_QUAN_LY` | Trang chủ | `layout-dashboard` | `ManagerHomePage` |
| | Thống kê doanh thu | `chart-no-axes-combined` | `RevenueStatisticsPage` |
| | Báo cáo tồn kho lâu ngày | `package-search` | `LongStockReportPage` |

> Menu là dữ liệu (`NavigationModel` → `List<NavEntry(id, label, icon, pageId)>` theo `LoaiQuyen`), **không** if/else trong component. Các UC khác (UC004, UC006, UC008–UC013, UC015, UC016, UC019) sẽ thêm mục khi có task riêng.

### 6.3 Topbar (`Topbar`)

Cao 72, nền trắng, viền dưới `#E2E8EC`, padding ngang 32, căn giữa dọc:

- **Trái — breadcrumb** (gap 10): khu vực (Regular 13 `#667580`: NVBH "Bán hàng", QL "Quản lý") · "/" (`#93A0AA`) · trang hiện tại (Medium 13 `#18252D`).
- **Phải — phiên người dùng** (gap 18): ngày `dd/MM/yyyy` (Regular 12 `#667580`) · avatar tròn 32 nền `#E8F5F2` chữ viết tắt Regular 12 `#087F75` · khối tên (Semi Bold 12 `#18252D`) trên vai trò (`moTaQuyen()`, Regular 10 `#667580`, gap 3) · nút "Đăng xuất" (icon `log-out` 17 `#667580` + Medium 12 `#18252D`, gap 7, không nền).
- Chữ viết tắt: **2 chữ đầu của 2 từ cuối họ tên** ("Lê Thu Hà" → "TH", "Trần Mai Linh" → "ML"). Figma có ngoại lệ "LTH" (X-11) — dùng quy tắc 2 chữ.
- Ngày cập nhật khi đổi ngày (Timer 60s) — chi tiết nhỏ, tùy chọn.
- Đăng xuất: giữ hộp thoại xác nhận hiện có ("Bạn có chắc muốn đăng xuất?") theo UC002, sau đó gọi `onLogout` → `AppNavigator.logout()`.

### 6.4 Chuyển trang

- `AppShellFrame.showPage(PageId)` đổi card, cập nhật nav active + breadcrumb, gọi `Page.onShow()` (để trang tự nạp dữ liệu bằng `SwingWorker` khi có controller).
- Interface `Page`: `PageId id()`, `String title()`, `JComponent view()`, `default void onShow() {}`, `default void onHide() {}`, `default void installShortcuts(InputMap, ActionMap) {}`.
- Hiệu ứng DISSOLVE 300ms của Figma: **bỏ qua** (không cần trong Swing).

---

## 7. Đặc tả từng màn hình

> Ký hiệu: `H g10 p0/16` = auto-layout ngang, gap 10, padding dọc 0 / ngang 16. Kích thước theo frame 1440. Các giá trị chữ trích nguyên văn Figma.

### 7.1 Đăng nhập (`LoginFrame`) — frame `1:1524` / `2003:21` — UC001

**Bố cục:** cửa sổ 1440×960 nền trắng, 2 cột:

- **Cột trái 163–164 px** nền `#163E43`: khối thương hiệu căn giữa dọc (logo 36×36 **nền trắng** r8 + icon `zap` `#087F75`; "DTAQ" trắng Regular 23; "ĐIỆN MÁY" `#BDDBD6` Semi Bold 10).
- **Cột phải** co giãn; form rộng **408** căn giữa cả 2 chiều, dọc gap 28:
  1. Lời chào (gap 12): "CHÀO MỪNG TRỞ LẠI" (`EYEBROW`, `#087F75`) / "Đăng nhập DTAQ" (`DISPLAY`).
  2. FormField "Tài khoản" — icon `user-round`, (Figma minh họa "NV0001"/"linh.quanly").
  3. FormField "Mật khẩu" — icon `lock-keyhole`, `JPasswordField`.
  4. **[Bổ sung, không có trong Figma]** nhãn lỗi Regular 12 `DANGER` (giữ `loiLabel` hiện có, ẩn khi rỗng).
  5. Nút Primary "Đăng nhập" full width 408×42.
  6. Hỗ trợ (padding top 8, gap 12): divider 1px `#E2E8EC`; link "Không thể đăng nhập? Quên mật khẩu!" Regular 12 `#667580` (đề xuất in đậm/màu `PRIMARY` ở phần "Quên mật khẩu!").

**Hành vi (giữ nguyên code hiện tại):** Enter ở 2 field → `submit()`; `SwingWorker` → `AuthController.login`; 4 thông báo lỗi giữ nguyên chuỗi; thành công → `navigator.showMain(nv)` — shell tự chọn trang chủ theo `LoaiQuyen` (Figma: NVBH → "Trang chủ — Nhân viên bán hàng", QL → "Trang chủ — Quản lý").

**Kích thước cửa sổ:** Figma 1440×960; đề xuất cửa sổ đăng nhập 1100×720 (`setResizable(true)`, form vẫn căn giữa) — D-07. Không để `setResizable(false)` với kích thước lớn.

### 7.2 Quên mật khẩu (`ForgotPasswordFrame`) — frame `2028:2` "Đổi mật khẩu"

Cùng khung 2 cột như 7.1. Form 408, gap 28:

1. "Cập nhật mật khẩu" (`DISPLAY`) — Figma bỏ eyebrow; code hiện có eyebrow "QUÊN MẬT KHẨU" → đề xuất giữ eyebrow cho nhất quán với đăng nhập (tùy chọn).
2. "Tài khoản" (icon `user-round`).
3. **"Gửi mã xác nhận"** — nút Secondary full width (**không có trong Figma** nhưng bắt buộc cho luồng OTP hiện tại — X-01). Đặt ngay dưới field Tài khoản.
4. "Mã xác nhận" (icon `lock-keyhole`; Figma ví dụ "HKD123").
5. "Mật khẩu mới" (icon `lock-keyhole`, password).
6. Nhãn trạng thái (info `#667580` / lỗi `DANGER`) — giữ `trangThaiLabel`.
7. Primary "Xác nhận" 408×42.
8. Divider + link "Đổi tài khoản khác? Đăng nhập!" → `navigator.showLogin()`.

Giữ nguyên toàn bộ chuỗi thông báo và `SwingWorker` hiện có.

### 7.3 Trang chủ — Nhân viên bán hàng (`SalesHomePage`) — frame `1:1380`

Breadcrumb: "Bán hàng / Trang chủ". Nội dung (gap 24):

1. **PageHeader** không có trailing: tiêu đề "Chào buổi sáng, {tên gọi}" (tên gọi = 2 từ cuối họ tên, ví dụ "Thu Hà"); mô tả "Quầy 01 đã sẵn sàng. Bắt đầu một phiên bán hàng hiệu quả." (phụ thuộc D-05). Lời chào theo giờ: [QUYẾT ĐỊNH] D-11 (đề xuất: 05:00–10:59 "buổi sáng", 11:00–12:59 "buổi trưa", 13:00–17:59 "buổi chiều", còn lại "buổi tối").
2. **3 KpiCard** (mỗi thẻ 371×134, gap 20):
   - "Hóa đơn trong ca" · "12" · "12 hóa đơn đã thanh toán đủ"
   - "Tiền mặt đã thu" · "68.200.000" · "Bao gồm 6.200.000 VNĐ tiền thuế"
   - "Khách hàng đã phục vụ" · "10" · "2 khách hàng mới trong ca"
   → Khái niệm "ca" không có trong schema (X-05). Skeleton: hiển thị "—" cho tới khi có UC/định nghĩa.
3. **Card "Hóa đơn gần đây trong ca"** (padding 24, gap 20), phải: "Hôm nay". DataTable cột: Hóa đơn (135, Medium) · Khách hàng (641, co giãn) · Tổng thanh toán (140, phải) · Trạng thái (120). Dòng cao 48. Trống → EmptyState "Chưa có hóa đơn trong hôm nay".

View-model: `SalesHomeData(String greetingName, KpiValue invoices, KpiValue cashCollected, KpiValue customersServed, List<RecentInvoiceRow> recent)`.

### 7.4 Trang chủ — Quản lý (`ManagerHomePage`) — frame `1:1200`

Breadcrumb: "Quản lý / Trang chủ Quản lý". Nội dung (gap 24):

1. **PageHeader**: "Trang chủ Quản lý"; trailing nút Secondary icon `calendar-days` + ngày "09/10/2026" (140×42) — hiện chỉ hiển thị ngày hôm nay (chọn ngày là tính năng chưa đặc tả).
2. **Card "Danh sách chờ phê duyệt đổi/trả"** (UC018) — padding 22, gap 16:
   - Header: trái "Danh sách chờ phê duyệt" (Semi Bold 18) / "6 phiếu chưa được xử lý" (Regular 13 `#667580`, gap 4); phải hộp nền `#F4F6F8` r6 padding 8/12: "Tổng hoàn dự kiến 26.200.000 VNĐ" (Semi Bold 13).
   - Bảng **kiểu nhẹ** (khác bảng chuẩn): header **không nền**, cao 27, viền dưới, chữ Semi Bold 12 `#667580`; dòng viền dưới, cao tự theo nội dung (61–92), padding dọc 14.
   - Cột: Mã phiếu (120) · Mã HD gốc (120) · Khách hàng & SĐT (220, 2 dòng: tên Regular 13 / SĐT Regular 12 `#667580`) · Hoàn dự kiến (140, **Semi Bold 13**) · Nhân viên lập (160) · Lý do đổi/trả (136, xuống dòng) · Thao tác (140: Primary small "Phê duyệt" + Danger-soft small "Từ chối", gap 8).
   - "Từ chối" **phải mở hộp thoại nhập lý do bắt buộc** (schema `ck_phieutra_tuchoi_lydo`/`ck_phieudoi_tuchoi_lydo`) — Figma thiếu (X-06). "Phê duyệt" mở hộp thoại xác nhận. Cả hai chỉ phát sự kiện `onApprove(id)`/`onReject(id)`; logic nằm ở task UC018.
3. **Hàng 2 card** (gap 16, mỗi card 568):
   - "Nhật ký hoạt động" (padding 20, gap 16): danh sách `ActivityItem` icon `clock` ("Đối soát cuối ca" · "08:30 • Đã khớp", "Sao lưu dữ liệu gần nhất" · "06:00 • Thành công"). → **Không có nguồn dữ liệu/UC** (X-04): skeleton hiển thị EmptyState "Chưa có hoạt động", ẩn toàn card nếu D-06 quyết định bỏ.
   - "Cảnh báo": `ActivityItem` tone danger icon `alert-circle` ("5 sản phẩm tồn trên 60 ngày" · "Cần rà soát tồn kho và khuyến mãi"; "2 phiếu đổi/trả cần xử lý trước 18:00" · "PDT000128 và PDT000129"). → hạn "18:00" không có trong đặc tả, "khuyến mãi" vướng C-04 (X-04).
4. **Hàng thông tin hệ thống** (3 card 373×87, gap 16, padding 20, gap 12): "Thời gian hiện tại" (icon `clock` + "09/10/2026 • 09:42"), "Kết nối CSDL" (chấm 8×8 `#087F75` + "Ổn định" — trạng thái kết nối lấy từ kết quả ping DataSource ở task sau; skeleton: "Đang kiểm tra…"), "Phiên bản" ("Nội bộ DTAQ · Version {AppInfo.version()}").

### 7.5 Thống kê doanh thu (`RevenueStatisticsPage`) — frame `2030:50` — UC017 (+UC015)

Breadcrumb: "Quản lý / Thống kê doanh thu". Nội dung (gap 24):

1. **PageHeader** "Thống kê doanh thu"; trailing nút Secondary `calendar-days` + "03/10 – 09/10/2026" (194×42) → mở bộ chọn kỳ (UC017 "Bộ lọc kỳ báo cáo"; component chọn ngày chưa có trong Figma → D-12).
2. **Card cảnh báo quan trọng** (padding 16, gap 8, có thể click): ô icon 36 nền `#F5E8E8` + `alert-circle` `#7F0808`; "Yêu cầu đổi/trả chờ" Semi Bold 14 `#7F0808`; "Số lượng: 6" Regular 13 `#7F0808`; dòng "Bấm vào để xem chi tiết" Italic 12 `#7F0808`. Click → chuyển `ManagerHomePage` (danh sách chờ duyệt). Ẩn khi số lượng = 0.
3. **4 KpiCard** (276×134, gap 16): "Tổng doanh thu trong kỳ" ("285.000.000", "↑ 12,4% so với kỳ trước" màu `PRIMARY`) · "Tổng tiền hàng trước thuế" ("Đã trừ hoàn trả và đổi hàng") · "Tổng tiền thuế phát sinh" ("Thuế VAT theo từng nhóm hàng") · "Hóa đơn bán lẻ thành công" ("6 phiếu đổi/trả trong kỳ").
4. **Card "Doanh thu theo ngày"** (cao ~345, padding 22, gap 20): tiêu đề Semi Bold 18 + segmented "Ngày | Tuần | Tháng" (gap 8) bên phải; `BarChartPanel` (5.14).
5. **Card "Tỷ trọng doanh thu top 5 dòng sản phẩm"** (padding 22, gap 20): tiêu đề Semi Bold 18 + phải "Kỳ 03/10 – 09/10/2026" Regular 13 `#667580`; nội dung ngang gap 24: `DonutChartPanel` 220 + legend (Tivi/Tủ lạnh/Máy giặt/Máy lạnh/Phụ kiện).

Ghi chú: UC017 hậu điều kiện có "hộp thoại lưu file Excel" nhưng Figma **không có nút xuất** ở màn này (trong khi màn tồn kho lại có) — X-12. Skeleton chừa chỗ trailing để thêm nút "Xuất báo cáo" khi task UC017 làm.

### 7.6 Báo cáo tồn kho lâu ngày (`LongStockReportPage`) — frame `1:2` — UC014

Breadcrumb: "Quản lý / Báo cáo tồn kho lâu ngày". Nội dung (gap 24):

1. **PageHeader**: "Báo cáo tồn kho lâu ngày" / "Các sản phẩm còn tồn trên 60 ngày · Tính đến 09/10/2026."; trailing Secondary `download` "Xuất báo cáo" (148×42) — xuất file cho UC014 chưa đặc tả (X-12) → disabled kèm tooltip trong skeleton.
2. **3 KpiCard** (371×134, gap 20): "Sản phẩm trên 60 ngày" ("6", "48 đơn vị còn tồn trong kho") · "Giá trị tồn theo giá nhập" ("246.000.000", "VNĐ · Không dùng giá bán lẻ") · "Thời gian tồn lâu nhất" ("126 ngày", "TIVI003 · Nhập ngày 05/06/2026").
3. **Card nội dung** (padding 24, gap 20):
   - Bộ lọc (H gap 16, căn đáy): FormField "Tìm sản phẩm" 340 (icon `search`, "Mã hoặc tên sản phẩm") · Combo "Ngành hàng" 195 ("Tất cả ngành hàng") · Combo "Thời gian tồn" 192 ("Trên 60 ngày") · Primary "Áp dụng" · đẩy phải pill NEUTRAL "6 kết quả".
   - DataTable cột: Mã (83, Medium) · Tên sản phẩm (454, co giãn) · Ngày nhập (92) · Số ngày tồn ↓ (90, phải) · SL tồn (48, phải) · Giá nhập / ĐV (108, phải) · Giá trị tồn (VNĐ) (125, phải). Hàng cuối "Tổng cộng".
   - Chân (H, 2 đầu): "Sắp xếp theo số ngày tồn giảm dần" · "Giá trị tồn = Số lượng tồn × Giá nhập" (Regular 12 `#667580`).
   - Các ngưỡng combo ("Trên 60 ngày"…) là tham số của UC014 ("chọn ngưỡng lưu kho") — giá trị danh sách cần xác nhận (D-12). Cột "SL tồn" vướng C-07.

### 7.7 Quản lý khách hàng (`CustomerListPage`) — frame `1:368` — UC007/UC005

Breadcrumb: "Bán hàng / Quản lý khách hàng". Nội dung (gap 24):

1. **PageHeader**: "Quản lý khách hàng" / "Tra cứu hồ sơ khách hàng để phục vụ và lập hóa đơn nhanh chóng."; trailing Primary icon `plus` "Thêm khách hàng" + phím tắt "F4" (204×42) → mở `AddCustomerDialog`.
2. **Card** (padding 24, gap 20):
   - Bộ lọc (H gap 16): FormField "Tìm khách hàng" (shortcut "F2", icon `search`, placeholder "Nhập họ tên hoặc số điện thoại", rộng ~570, co giãn) · Secondary "Tìm kiếm" (94×42) · đẩy phải pill NEUTRAL "{n} khách hàng". Enter trong ô tìm = bấm Tìm kiếm.
   - DataTable cột: Mã khách hàng (100, Medium) · Họ và tên (161) · Số điện thoại (124) · Địa chỉ (472, co giãn) · Ngày tạo (91) · Thao tác (64, link "Chi tiết"). 8 dòng/trang (Figma). → "Ngày tạo" **không có cột trong `KhachHang`** (X-03). "Chi tiết" mở màn tra cứu khách hàng & giao dịch (UC005 — chưa có thiết kế; skeleton: phát sự kiện `onOpenDetail(maKH)`, chưa điều hướng).
   - `Pagination` ("Hiển thị 1–8 trên 8 khách hàng").
3. **Trạng thái**: đang tải (overlay mờ + "Đang tải…"), rỗng ("Chưa có khách hàng" / "Không tìm thấy khách hàng phù hợp"), lỗi (callout danger + nút "Thử lại").

### 7.8 Hộp thoại Thêm khách hàng (`AddCustomerDialog`) — frame `1:176` — UC007

`ModalDialog` rộng 600 trên overlay:

- Header: "Thêm khách hàng" / "Tạo hồ sơ để sử dụng khi lập hóa đơn." + nút `x`.
- Form (gap 20):
  - "Họ và tên *" (Figma minh họa đang focus).
  - "Số điện thoại *" + hint "Số điện thoại dùng để tra cứu; không trùng hồ sơ hiện có."
  - "Địa chỉ *" → **schema `diaChi` cho phép NULL** (X-02). Không tự áp bắt buộc ở DB; validation do task UC007 quyết định.
- `InfoCallout`: "Các trường có dấu * là bắt buộc. Mã khách hàng sẽ được tạo tự động khi lưu." (phù hợp quyết định ID do ứng dụng sinh — AGENTS §5.5).
- Divider, footer phải: Secondary "Hủy" (60×42) · Primary "Lưu khách hàng" (141×42).
- Hành vi skeleton: `Esc`/click overlay/`x`/Hủy → đóng không lưu; "Lưu" → gọi `Consumer<NewCustomerForm>` truyền vào (task UC007 nối controller); hiển thị lỗi từng field qua `FormField.setError`. Nút Lưu disabled trong lúc `SwingWorker` chạy.

### 7.9 Lập hóa đơn bán lẻ (`RetailInvoicePage`) — 5 frame — UC003

Breadcrumb: "Bán hàng / Lập hóa đơn bán lẻ". Cấu trúc chung (gap 24):

1. **PageHeader**: "Lập hóa đơn bán lẻ" / "Quầy 01 · Nhân viên: {hoTen} · {dd/MM/yyyy, HH:mm}"; trailing `StatusPill`: NEUTRAL "{maHD} · Bản nháp" hoặc SUCCESS "{maHD} · Đã thanh toán". (Định dạng mã "HD-091026-013" không khớp quy ước — X-13; hiển thị mã do hệ thống cấp, không tự sinh theo Figma. Khi chưa có mã: "Hóa đơn mới · Bản nháp".)
2. **Thân 2 cột** (gap 24): trái **768** (co giãn) / phải **360** (cố định).

**Cột trái** (dọc gap 20):

- **Card Tra cứu khách hàng** (padding 20, gap 20): hàng (gap 12) FormField "Tìm khách hàng theo số điện thoại" (shortcut F2, icon `phone`, placeholder "Nhập số điện thoại khách hàng") + Secondary "Tìm khách hàng" (140×42). Dưới:
  - chưa chọn: hướng dẫn "Tra cứu khách hàng có sẵn hoặc thêm mới tại Quản lý khách hàng." (Regular 12 `#667580`);
  - đã chọn: icon `circle-check` 17 `#087F75` + "Nguyễn Văn A · KH0001" (Medium 13 `#075E57`) + "Thông tin đã được điền vào hóa đơn" (Regular 12 `#667580`), gap 8.
- **Card Tìm sản phẩm** (padding 20, gap 20): FormField "Tìm sản phẩm" (shortcut "F3 · Enter để thêm", icon `search`, placeholder "Nhập mã hoặc tên sản phẩm"). Khi có kết quả, thêm khối **Kết quả tra cứu** (gap 12):
  - hàng tiêu đề: "{n} kết quả phù hợp" (Regular 12) · phải "Đơn giá trước thuế · VNĐ" (Regular 11).
  - mỗi kết quả (cao 66, padding 12, gap 12, r6): icon sản phẩm 26 (`tv`) · tên (Medium 13) / "TIVI001 · Tồn 12 · Thuế 10%" (Regular 11 `#667580`) · đơn giá (Semi Bold 14) · nút "Thêm".
  - dòng **đang chọn**: nền `#E8F5F2`, viền `#B5DCD6`, nút Primary "Thêm" + "↵"; dòng khác: nền trắng, viền `#E2E8EC`, nút Secondary "Thêm". `↑/↓` đổi dòng chọn, Enter = thêm dòng chọn.
- **Card Giỏ hàng** (padding 20, gap 20): tiêu đề "Giỏ hàng" + phải "{x} sản phẩm · {y} đơn vị" (hoặc "0 sản phẩm").
  - DataTable cột: Mã (68, Medium) · Tên sản phẩm (297, co giãn, 2 dòng) · SL (30, phải) · "Đơn giá\ntrước thuế" (94, phải) · "Thuế\nsuất" (46, phải) · "Thành tiền\nsau thuế" (101, phải). Dòng cao 84.
  - Trạng thái **Sẵn sàng thanh toán**: ô SL là ô nhập nhỏ 28×22 viền `#93A0AA` r4 → **vướng C-07** (schema `ChiTietHoaDon.soLuong = 1`, mỗi dòng 1 serial). Skeleton: SL chỉ hiển thị, không cho sửa, cho tới khi C-07 được chốt (X-14).
  - Dưới bảng (H, 2 đầu): pill SUCCESS "Tồn kho: 12 · Số lượng hợp lệ" (nháp) / "Tồn kho sau bán: 11" (đã TT) · phải "Tiền thuế: 800.000 VNĐ" (Regular 12 `#667580`).
  - `InfoCallout` "Đơn giá và thuế suất được khóa theo sản phẩm. Không chỉnh giá hoặc áp dụng giảm giá tại quầy." (chỉ thấy ở frame Đã thanh toán; đề xuất hiện luôn khi giỏ có hàng).
  - Giỏ trống: chỉ header bảng + `EmptyState` (`shopping-cart`; "Chưa có sản phẩm trong giỏ"; hướng dẫn "Nhấn F3 để tìm sản phẩm và bắt đầu lập hóa đơn." hoặc, khi đang có kết quả tìm, "Chọn “Thêm” hoặc nhấn Enter để thêm sản phẩm đã chọn.").
- **ShortcutBar** (5.12).

**Cột phải** (dọc gap 20):

- **Card Thông tin khách hàng** (padding 20, gap 20): tiêu đề "Thông tin khách hàng".
  - chưa chọn: "Chưa chọn khách hàng" (Medium 14 `#667580`) / "Tìm theo số điện thoại để tự động điền họ tên và địa chỉ vào hóa đơn." (Regular 13 `#667580`), gap 15.
  - đã chọn (gap 14): "KH0001 · Khách hàng hiện có" (Regular 11 `#667580`) / họ tên (`CUSTOMER_NAME`) (gap 6); SĐT Regular 14; địa chỉ Regular 13 `#667580` (xuống dòng).
- **Card Thanh toán** (padding 20, gap 20): tiêu đề "Thanh toán".
  - Khối tổng (gap 16): "Tổng trước thuế" ↔ giá trị (Medium 14, phải); "Tổng tiền thuế" ↔ giá trị; divider; "Tổng thanh toán" (Semi Bold 13); số lớn `AMOUNT_XL` `#075E57` + "VNĐ" (Regular 12 `#667580`, gap 8, căn baseline).
  - FormField "Tiền khách đưa (VNĐ)" (không icon, căn trái giá trị).
  - "Tiền trả lại khách" ↔ giá trị Semi Bold 14 ("200.000 VNĐ").
  - Nút full width 320×42: "Thanh toán bằng tiền mặt" + "F8". Disabled (`#E5EBEF`/`#93A0AA`) khi chưa đủ điều kiện; Primary khi đủ; sau khi đã TT đổi nhãn "Đã thu đủ tiền mặt" và disabled.
  - Dòng điều kiện (Regular 11 `#667580`): "Chọn khách hàng và thêm sản phẩm để thanh toán." / "Thu đủ 100% giá trị hóa đơn. Số lượng bán không được vượt tồn kho." / "Đã thu đủ 100% giá trị hóa đơn và cập nhật tồn kho."

**Máy trạng thái UI** (`InvoiceViewState`):

| State | Frame | Khách | Kết quả tìm | Giỏ | Nút thanh toán | Pill |
|---|---|---|---|---|---|---|
| `NO_CUSTOMER` | `1:1054` | chưa chọn (ô SĐT focus) | ẩn | trống | disabled | Bản nháp |
| `CUSTOMER_SELECTED` | `2003:231` | đã chọn | ẩn | trống | disabled | Bản nháp |
| `PRODUCT_RESULTS` | `1:878` | đã chọn | hiện (ô SP focus) | trống/có | disabled nếu giỏ trống | Bản nháp |
| `READY_TO_PAY` | `1:716` | đã chọn | ẩn | có hàng | **enabled** khi tiền khách đưa ≥ tổng | Bản nháp |
| `PAID` | `1:523` | đã chọn | ẩn | có hàng (khóa) | disabled "Đã thu đủ tiền mặt" | Đã thanh toán + mở `InvoiceReceiptDialog` |

- Điều kiện enable nút (UI-only, **không phải nguồn sự thật**; controller UC003 phải kiểm tra lại): có khách, giỏ ≥ 1 dòng, tiền khách đưa ≥ tổng thanh toán.
- **Không tính tiền ở UI**: tổng, thuế, tiền thừa do controller UC003 trả về (C-11 làm tròn chưa chốt). Skeleton hiển thị "0".
- Không có khuyến mãi trong UI (C-04; Figma ghi rõ "không áp dụng giảm giá tại quầy").

### 7.10 (gộp trong 7.9) Các phím tắt của màn hóa đơn → mục 8.2.

### 7.11 Hộp thoại hóa đơn thành công (`InvoiceReceiptDialog`) — frame `1:523`

- `ModalDialog` rộng 568, padding 32, gap 24: vùng xem trước hóa đơn 504×508 (Figma là ảnh PNG; Swing: panel `InvoiceReceiptPanel` tự vẽ theo bố cục 3.2, cuộn dọc nếu dài); dưới phải link "Cần Đổi/Trả hàng ?" (Regular 12, `PRIMARY` — X-09) → điều hướng UC006 (chưa có màn → disabled/ẩn trong skeleton); nút Primary full width "Đóng" → đóng dialog và **reset trang về `NO_CUSTOMER`** với hóa đơn nháp mới (theo prototype).
- Thông tin cửa hàng (tên, địa chỉ, SĐT) trong mẫu hóa đơn: không có trong schema → D-08 (đặt trong file cấu hình, không hard-code).
- In thật (`java.awt.print`) và "ĐVT": ngoài phạm vi skeleton (D-08).

---

## 8. Bản đồ điều hướng & phím tắt

### 8.1 Điều hướng (từ 51 tương tác prototype, đã chuẩn hóa)

```
Đăng nhập ──(thành công, NVBH)──► Trang chủ NVBH
          ──(thành công, QL)────► Trang chủ QL
          ──"Quên mật khẩu!"────► Quên mật khẩu ──"Xác nhận" thành công / "Đăng nhập!"──► Đăng nhập

[Shell NVBH] Sidebar: Trang chủ ↔ Quản lý khách hàng ↔ Lập hóa đơn bán lẻ
  Quản lý KH ──"Thêm khách hàng"/F4──► modal Thêm KH ──Hủy/Lưu/overlay/Esc──► Quản lý KH
  Lập HĐ: NO_CUSTOMER ──Tìm KH──► CUSTOMER_SELECTED ──F3/gõ SP──► PRODUCT_RESULTS
          ──Thêm/Enter──► READY_TO_PAY ──Thanh toán/F8──► PAID + modal hóa đơn ──Đóng──► NO_CUSTOMER

[Shell QL] Sidebar: Trang chủ QL ↔ Thống kê doanh thu ↔ Báo cáo tồn kho lâu ngày
  Thống kê ──click "Yêu cầu đổi/trả chờ"──► Trang chủ QL (danh sách chờ duyệt)

Mọi trang ──"Đăng xuất"──► hộp xác nhận (UC002) ──Có──► Đăng nhập
```

Sai lệch prototype cần bỏ qua: mục "Thống kê" ở sidebar NVBH trỏ sang Lập hóa đơn (X-07); một số nút "Lập hóa đơn bán lẻ" trỏ sang frame "chọn khách hàng" thay vì "Chưa chọn" — trang luôn mở ở state hiện có (không reset nếu đang có hóa đơn nháp dở — đề xuất, D-13).

### 8.2 Phím tắt

| Phím | Phạm vi | Hành động |
|---|---|---|
| `Enter` | Đăng nhập/Quên MK | Submit (đã có) |
| `F2` | Quản lý KH | Focus ô "Tìm khách hàng" |
| `F4` | Quản lý KH | Mở hộp thoại Thêm khách hàng |
| `F2` | Lập HĐ | Focus ô SĐT khách hàng |
| `F3` | Lập HĐ | Focus ô Tìm sản phẩm |
| `↑` / `↓` | Lập HĐ (khi danh sách kết quả hiển thị) | Đổi dòng kết quả được chọn |
| `Enter` | Ô tìm sản phẩm | Thêm dòng kết quả đang chọn vào giỏ |
| `F8` | Lập HĐ | Thanh toán (chỉ khi nút enabled) |
| `Esc` | Mọi modal | Đóng / Hủy |

Cài bằng `InputMap(WHEN_IN_FOCUSED_WINDOW)` của **panel trang** (đăng ký trong `Page.installShortcuts`, gỡ khi `onHide`) để F2/F3 không xung đột giữa các trang.

---

## 9. Kiến trúc mã đề xuất

Giữ luồng bắt buộc `UI → Controller → DAO → DAO impl → Entity → PostgreSQL`. Kế hoạch này **chỉ** thêm lớp ở tầng UI; không thêm Service/DTO/Repository.

```
src/main/java/com/dtaq/retail/
├── Main.java                         (sửa 1 dòng: DtaqTheme.install())
├── ui/
│   ├── AppNavigator.java             (sửa: showMain → AppShellFrame; giữ API)
│   ├── LoginFrame.java               (restyle, giữ logic)
│   ├── ForgotPasswordFrame.java      (restyle, giữ logic)
│   ├── MainFrame.java                (XÓA sau khi AppShellFrame thay thế — hỏi trước, D-14)
│   ├── theme/
│   │   ├── DtaqTheme.java            install(): nạp font + DtaqLightLaf.setup() + UIManager defaults
│   │   ├── DtaqLightLaf.java         extends FlatLightLaf (getName "DTAQ Light")
│   │   ├── DtaqColors.java           hằng Color theo mục 4.1
│   │   ├── DtaqFonts.java            hàm font(token) theo mục 4.2 (có tracking)
│   │   ├── DtaqSpacing.java          hằng số mục 4.3
│   │   └── DtaqIcons.java            icon(name, size, color) – mục Phụ lục B
│   ├── format/
│   │   ├── MoneyFormat.java          "8.000.000" (grouping '.'), "… VNĐ"; chỉ định dạng, KHÔNG làm tròn
│   │   ├── DateFormats.java          dd/MM/yyyy; dd/MM/yyyy, HH:mm; dd/MM/yyyy • HH:mm; dd/MM
│   │   ├── PhoneFormat.java          hiển thị "0909 123 456" (4-3-3), không đổi dữ liệu lưu
│   │   └── PersonNames.java          initials(hoTen), shortName(hoTen)
│   ├── component/
│   │   ├── DtaqButton.java  FormField.java  Card.java  KpiCard.java  StatusPill.java
│   │   ├── InfoCallout.java  DataTable.java  Pagination.java  EmptyState.java
│   │   ├── ModalDialog.java  PageHeader.java  ShortcutBar.java  IconTile.java  ActivityItem.java
│   │   ├── SegmentedControl.java  BarChartPanel.java  DonutChartPanel.java
│   │   └── table/ TextCellRenderer, CodeCellRenderer, MoneyCellRenderer, TwoLineCellRenderer, ActionCell
│   ├── shell/
│   │   ├── AppShellFrame.java        JFrame: Sidebar + Topbar + CardLayout
│   │   ├── Sidebar.java  NavItem.java  Topbar.java
│   │   ├── NavigationModel.java      menu theo LoaiQuyen (thuần Java, test được)
│   │   ├── PageId.java               enum: SALES_HOME, MANAGER_HOME, CUSTOMERS, RETAIL_INVOICE, REVENUE_STATS, LONG_STOCK_REPORT
│   │   └── Page.java                 interface (mục 6.4)
│   └── page/
│       ├── home/      SalesHomePage, ManagerHomePage (+ record view-model)
│       ├── customer/  CustomerListPage, AddCustomerDialog, NewCustomerForm (record)
│       ├── invoice/   RetailInvoicePage, InvoiceViewState (enum), InvoiceReceiptDialog, InvoiceReceiptPanel
│       └── report/    RevenueStatisticsPage, LongStockReportPage
src/main/resources/com/dtaq/retail/ui/theme/
│   ├── DtaqLightLaf.properties       token FlatLaf (mục 9.2)
│   └── icons/*.svg                   (nếu D-02 chọn SVG)
src/test/java/com/dtaq/retail/ui/...  (mục 13)
```

### 9.1 Nguyên tắc

- **Component không gọi controller/DAO**; trang nhận controller qua constructor **khi task UC tương ứng được làm**. Trong skeleton, trang nhận `Supplier`/listener rỗng hoặc chỉ hiển thị empty state.
- View-model là `record` trong package trang (dữ liệu hiển thị đã định dạng hoặc đã tính sẵn từ controller) — **không** dùng entity ở chỗ cần chuỗi định dạng; không thêm tầng DTO chung.
- Mọi I/O trong `SwingWorker`; cập nhật UI trong `done()`.
- Không static mutable mới (ngoại lệ hiện có: `CurrentSession`).
- Mỗi lớp ≤ ~300 dòng; trang lớn (hóa đơn) tách panel con: `CustomerLookupPanel`, `ProductSearchPanel`, `CartPanel`, `CustomerInfoPanel`, `PaymentPanel`.

### 9.2 `DtaqLightLaf.properties` (khởi điểm — kiểm chứng từng key với FlatLaf 3.6)

```properties
@accentColor = #087F75
@background = #F4F6F8
@foreground = #18252D
@disabledForeground = #93A0AA
@selectionBackground = #E8F5F2
@selectionForeground = #18252D

Component.arc = 6
Button.arc = 6
TextComponent.arc = 6
Component.focusWidth = 0
Component.innerFocusWidth = 1
Component.borderColor = #E2E8EC
Component.focusedBorderColor = #087F75
Component.disabledBorderColor = #E2E8EC

Button.background = #FFFFFF
Button.foreground = #18252D
Button.borderColor = #E2E8EC
Button.default.background = #087F75
Button.default.foreground = #FFFFFF
Button.default.borderWidth = 0
Button.disabledBackground = #E5EBEF

TextField.placeholderForeground = #93A0AA
TextField.margin = 0,12,0,12
PasswordField.margin = 0,12,0,12

Table.rowHeight = 61
Table.showHorizontalLines = true
Table.showVerticalLines = false
Table.gridColor = #E2E8EC
Table.selectionBackground = #E8F5F2
Table.selectionForeground = #18252D
TableHeader.background = #F4F6F8
TableHeader.foreground = #667580
TableHeader.height = 44
TableHeader.separatorColor = #F4F6F8
TableHeader.bottomSeparatorColor = #E2E8EC

ScrollBar.width = 10
ScrollBar.thumbArc = 999
ScrollBar.thumbInsets = 2,2,2,2
ScrollPane.border = 0,0,0,0

Panel.background = #F4F6F8
OptionPane.background = #FFFFFF
```

### 9.3 Font

- Phương án A (khuyến nghị, cần duyệt D-01): thêm `com.formdev:flatlaf-fonts-inter` (cùng nhà phát triển FlatLaf; **kiểm tra version trên Maven Central trước khi thêm**), gọi `FlatInterFont.install()` rồi `FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY)` (kiểm chứng API) **trước** `setup()`.
- Phương án B (không thêm dependency): nhúng file Inter `.ttf` (OFL license) vào `resources/fonts/`, `Font.createFont` + `GraphicsEnvironment.registerFont`.
- Phương án C: không đổi font (FlatLaf mặc định: Segoe UI trên Windows) — chấp nhận lệch nhẹ.
- Sau khi nạp: `UIManager.put("defaultFont", inter 13)` vì phần lớn nội dung Figma là 13px.

### 9.4 HiDPI

- FlatLaf tự scale theo DPI Windows. Số px trong token là **px logic**; khi vẽ tay (Card, chart, pill) dùng `UIScale.scale(int)`.
- MigLayout: dùng đơn vị mặc định và kiểm tra ở 100%/125%/150% (mục 13.3).

---

## 10. Lộ trình triển khai theo giai đoạn

> Mỗi giai đoạn: build xanh (`mvn -q test`), chạy app được, diff gọn. Báo cáo kết quả build/test thật sau mỗi giai đoạn.

### Giai đoạn 0 — Chuẩn bị (không sửa code)

1. `git status --short --branch`; xác nhận không có thay đổi dở của người khác ở `ui/`.
2. Nhóm duyệt các quyết định mục 12 (tối thiểu D-01, D-02, D-05, D-07, D-09, D-14).
3. Đề xuất tạo nhánh riêng (ví dụ `feature/ui-shell`) — chỉ khi người dùng đồng ý.
- **Nghiệm thu:** danh sách quyết định có câu trả lời.

### Giai đoạn 1 — Theme & token

- Tạo `ui/theme/DtaqColors`, `DtaqFonts`, `DtaqSpacing`, `DtaqLightLaf` + `.properties`, `DtaqTheme.install()`.
- Sửa `Main`: `FlatLightLaf.setup()` → `DtaqTheme.install()`.
- Cài font theo D-01.
- **Nghiệm thu:** app chạy, màn đăng nhập hiện tại tự đổi sang màu accent `#087F75`, bo góc 6, font Inter (nếu duyệt); test `DtaqColorsTest` khớp hex mục 4.1.

### Giai đoạn 2 — Icon

- `DtaqIcons.icon(name, size, color)` cho 22 icon Phụ lục B theo D-02.
- **Nghiệm thu:** một màn demo nội bộ (không commit, hoặc test) vẽ đủ 22 icon ở 17/18/19/22 px; màu đổi được; sắc nét ở 150%.

### Giai đoạn 3 — Component

- Làm theo thứ tự: `DtaqButton` → `FormField` → `Card` → `StatusPill` → `InfoCallout` → `PageHeader` → `KpiCard` → `DataTable` + renderers → `EmptyState` → `Pagination` → `ModalDialog` → `ShortcutBar` → `IconTile/ActivityItem` → `SegmentedControl` → `BarChartPanel` → `DonutChartPanel`.
- `format/*` + unit test.
- **Nghiệm thu:** mỗi component khớp spec mục 5 (màu, kích thước, padding); test formatter xanh.

### Giai đoạn 4 — App shell

- `PageId`, `Page`, `NavigationModel`, `NavItem`, `Sidebar`, `Topbar`, `AppShellFrame`.
- `AppNavigator.showMain(nv)` mở `AppShellFrame(nv, pages, onLogout)`; trang mặc định theo vai trò.
- Đăng xuất: chuyển hộp xác nhận từ `MainFrame` sang `Topbar`/`AppShellFrame`.
- Tạm thời mỗi `Page` là `PlaceholderPage(title)`.
- `MainFrame`: xóa khi không còn tham chiếu (**hỏi trước** — D-14).
- **Nghiệm thu:** đăng nhập NVBH thấy 3 mục menu đúng, QL thấy 3 mục đúng; click chuyển trang, breadcrumb và active đổi đúng; đăng xuất có xác nhận rồi về màn đăng nhập; `NavigationModelTest` xanh.

### Giai đoạn 5 — Màn xác thực

- Restyle `LoginFrame`, `ForgotPasswordFrame` theo 7.1/7.2 bằng component mới; **không đổi** controller, chuỗi thông báo, tên field (`setName`).
- **Nghiệm thu:** `AuthControllerTest`, `PasswordResetControllerTest` vẫn xanh; thao tác đăng nhập đúng/sai/khóa, gửi mã, đặt lại MK hoạt động như trước; bố cục khớp Figma.

### Giai đoạn 6 — Khung màn hình nghiệp vụ (mỗi gạch đầu dòng = 1 PR/commit riêng)

1. `SalesHomePage` (7.3).
2. `ManagerHomePage` (7.4) — nút Phê duyệt/Từ chối chỉ phát sự kiện.
3. `CustomerListPage` + `AddCustomerDialog` (7.7, 7.8).
4. `RetailInvoicePage` + panel con + `InvoiceViewState` + `InvoiceReceiptDialog` (7.9, 7.11) — có hàm `render(InvoiceViewModel)` để chuyển giữa 5 state.
5. `RevenueStatisticsPage` (7.5).
6. `LongStockReportPage` (7.6).

Mỗi trang: empty/loading/error state; phím tắt mục 8.2; không SQL; không số liệu Figma hard-code (trừ D-09).
- **Nghiệm thu:** so khớp checklist 13.2 cho từng trang ở 1440×960; chuyển state hóa đơn bằng view-model giả trong test UI thủ công.

### Giai đoạn 7 — Rà soát & báo cáo

- `git diff --check`, `mvn -q test`, `mvn -q package`; chạy app ở 100%/125%/150%.
- Cập nhật `docs/reference/ui/screen-flow-and-screens.md` mục ảnh chụp mã nguồn (đánh dấu là quan sát code, không sửa nội dung DOCX) — chỉ khi được yêu cầu.
- Báo cáo theo AGENTS.md §8 Stage E.

### Sau kế hoạch này (task riêng, theo UC)

UC007 (nối `AddCustomerDialog` + `KhachHangDao`), UC005 (tra cứu/“Chi tiết”), UC003 (hóa đơn — cần chốt C-07, C-11, C-04, định dạng mã), UC018 (phê duyệt — ngưỡng 3.000.000 VNĐ), UC014, UC017/UC015.

---

## 11. Xung đột Figma ↔ tài liệu ↔ schema ↔ code

| ID | Mô tả | Nguồn A | Nguồn B | Cách xử lý trong skeleton |
|---|---|---|---|---|
| X-01 | Màn "Đổi mật khẩu" Figma không có nút gửi mã | Figma `2028:2` | Code `ForgotPasswordFrame` (OTP qua email) | Giữ nút "Gửi mã xác nhận" |
| X-02 | Địa chỉ khách hàng bắt buộc (`*`) | Figma `1:176` | Schema `KhachHang.diaChi` nullable | Không ép ở DB; UC007 quyết định |
| X-03 | Cột "Ngày tạo" khách hàng | Figma `1:368` | Schema `KhachHang` không có cột ngày tạo | Ẩn cột cho tới khi có quyết định (không đổi schema) |
| X-04 | Nhật ký hoạt động (đối soát cuối ca, sao lưu), cảnh báo hạn 18:00, "rà soát … khuyến mãi" | Figma `1:1200` | Không có UC/bảng; sao lưu = C-05; khuyến mãi = C-04 | Empty state hoặc ẩn (D-06) |
| X-05 | Khái niệm "ca", "quầy", "chi nhánh" | Figma (sidebar, trang chủ NVBH, hóa đơn) | Schema không có; đa chi nhánh ngoài phạm vi | D-05 |
| X-06 | Từ chối phiếu không có hộp nhập lý do | Figma `1:1200` | Schema bắt buộc lý do khi từ chối | Thiết kế thêm hộp thoại lý do theo `ModalDialog` |
| X-07 | Mục "Thống kê" trong sidebar NVBH (icon & đích sai) | Figma `1:1380` | Thống kê là chức năng của QL (UC015–UC017) | Không đưa vào menu NVBH |
| X-08 | Màu menu active khác nhau (`#E8F5F2` + chữ đậm vs `#D6EEEA` + chữ thường) | Figma QL vs NVBH | — | Chuẩn hóa `#E8F5F2` / `#075E57` Semi Bold |
| X-09 | Màu link `#00B3A4` lạc tông | Figma `1:523` | Token chính | Dùng `PRIMARY` |
| X-10 | Icon chevron của dropdown nằm bên trái | Figma `1:2` | Quy ước combo | Dùng mũi tên phải mặc định |
| X-11 | Viết tắt avatar "LTH" (3 chữ) | Figma `1:1200` | Các frame khác 2 chữ | Quy tắc 2 chữ |
| X-12 | Nút "Xuất báo cáo" ở tồn kho, không có ở thống kê doanh thu | Figma `1:2`/`2030:50` | UC017 mới có xuất Excel (FR011) | Tồn kho: disabled; Thống kê: chừa chỗ |
| X-13 | Định dạng mã: `KH0001`, `HD-091026-013`, `HD000125`, `PDT000124`, `NV0001` | Figma | Quy ước `TIỀN_TỐ-YYYY-NNNNNN` (C-12, AGENTS §5.5) | Chỉ hiển thị mã từ dữ liệu; không sinh mã ở UI |
| X-14 | SL trong giỏ sửa được / "SL tồn" theo số lượng | Figma `1:716`, `1:2` | `ChiTietHoaDon.soLuong = 1` (serial), C-07 | SL chỉ đọc; chờ C-07 |
| X-15 | Hóa đơn hiển thị giờ "09:42" | Figma | `HoaDon.ngayLap` kiểu `DATE` | Chỉ hiển thị ngày cho dữ liệu đã lưu; giờ hiện tại chỉ dùng cho bản nháp — báo nhóm |
| X-16 | Tên "Báo cáo tồn kho" vs "… lâu ngày" vs danh mục "hằng ngày" | Figma `1:1200` vs `1:2`; C-01 | | Dùng "Báo cáo tồn kho lâu ngày" (tiêu đề đặc tả) |
| X-17 | "Version 1.0" vs "Phiên bản 1.0" | Figma `1:1200` vs sidebar | | Dùng "Phiên bản" + `AppInfo.version()` |
| X-18 | Figma có 3 "trang chủ" (NVBH, QL, Thống kê) — DOCX có Hình 5/8/14 chưa rõ vai trò | Figma | `screen-flow-and-screens.md` §7.4 | Ánh xạ theo mục 3.1; báo nhóm xác nhận |
| X-19 | Footer frame Thêm KH "Dữ liệu minh họa · Không phải số liệu báo cáo thực tế" | Figma `1:176` | | Không đưa vào app |

---

## 12. Danh sách quyết định cần nhóm duyệt

| ID | Câu hỏi | Khuyến nghị | Chặn giai đoạn |
|---|---|---|---|
| D-01 | Nạp font Inter bằng cách nào? | A: thêm `flatlaf-fonts-inter` (cùng vendor FlatLaf) | 1 |
| D-02 | Nguồn icon? (a) thêm `flatlaf-extras` + SVG Lucide (ISC license) dùng `FlatSVGIcon`; (b) tự vẽ Java2D 22 icon, không dependency | (a) nếu chấp nhận dependency; (b) nếu không | 2 |
| D-03 | Có cho phép thêm bất kỳ dependency UI nào không? (chart/date-picker) | Không — chart tự vẽ; date-picker dùng 2 `JSpinner` ngày | 3 |
| D-04 | Giữ hiệu ứng chuyển trang? | Không | 4 |
| D-05 | Hiển thị "Chi nhánh/Quầy/Ca" thế nào khi schema không có? | Thay bằng "DTAQ • Siêu thị điện máy" + vai trò; bỏ "Quầy/Ca" | 4, 6 |
| D-06 | Card "Nhật ký hoạt động" ở Trang chủ QL: giữ (empty) hay ẩn? | Ẩn tới khi có UC | 6 |
| D-07 | Kích thước cửa sổ | Đăng nhập 1100×720 resizable; Shell mở maximized, min 1280×760 | 4, 5 |
| D-08 | Thông tin cửa hàng trên hóa đơn in + có làm in thật không? | Đọc từ file cấu hình (như `db.properties`); in thật để task UC003 | 6 |
| D-09 | Có chế độ demo dùng dữ liệu mẫu Figma? | Không trong code chính; chỉ trong test | 6 |
| D-10 | Màu lỗi validation | `#B42318` | 3 |
| D-11 | Mốc giờ lời chào | Như 7.3 | 6 |
| D-12 | Bộ chọn kỳ thống kê & ngưỡng tồn kho (60/90/120 ngày?) | Hỏi nhóm nghiệp vụ | 6 (5, 6) |
| D-13 | Khi bấm lại menu "Lập hóa đơn" lúc đang có nháp | Giữ nguyên nháp | 6 |
| D-14 | Xóa `MainFrame.java` sau khi thay | Xóa (có hỏi) | 4 |
| D-15 | Danh sách chờ duyệt ở Trang chủ QL lọc theo điều kiện nào (chỉ `CHO_DUYET`?) | `trangThaiDuyet = 'CHO_DUYET'` — xác nhận ở task UC018 | (UC018) |

---

## 13. Kiểm thử & checklist so khớp trực quan

### 13.1 Unit test (JUnit 5 hiện có; không thêm framework)

| Test | Kiểm tra |
|---|---|
| `MoneyFormatTest` | 0 → "0"; 8000000 → "8.000.000"; 246000000 → "246.000.000"; hậu tố "VNĐ"; số âm (nếu cho phép) |
| `DateFormatsTest` | `09/10/2026`, `09/10/2026, 09:42`, `09/10/2026 • 09:42`, `03/10` |
| `PhoneFormatTest` | "0909123456" → "0909 123 456"; chuỗi không hợp lệ trả nguyên |
| `PersonNamesTest` | "Lê Thu Hà" → "TH"/"Thu Hà"; "Trần Mai Linh" → "ML"; 1 từ; khoảng trắng thừa |
| `NavigationModelTest` | QL có đúng 3 mục theo thứ tự; NVBH có đúng 3 mục; không có "Thống kê" cho NVBH; trang mặc định theo vai trò |
| `GreetingTest` | biên 04:59/05:00/10:59/11:00/12:59/13:00/17:59/18:00 (sau khi D-11 duyệt) |
| `InvoiceViewStateTest` | chuyển trạng thái + điều kiện enable nút thanh toán (UI-only) |
| `DtaqColorsTest` | hex khớp mục 4.1 |
| Component (tùy chọn) | dựng trong `Assumptions.assumeFalse(GraphicsEnvironment.isHeadless())` — kiểm tra kích thước ưu tiên nút 42, input 44 |

### 13.2 Checklist trực quan (mỗi màn hình, chụp ở 1440×960, 100% scale, đặt cạnh frame Figma)

- [ ] Nền app `#F4F6F8`, sidebar 224 trắng có viền phải, topbar 72 có viền dưới.
- [ ] Tiêu đề trang 28 Regular; mô tả 14 `#667580`; khoảng cách khối 24; padding 32.
- [ ] Card trắng r10 viền `#E2E8EC`; padding đúng (20/22/24).
- [ ] Input cao 44, r6; focus viền `#087F75`; placeholder `#93A0AA`; icon trái 18.
- [ ] Nút 42, r6, Semi Bold 14; disabled `#E5EBEF`/`#93A0AA`; phím tắt hiện đúng.
- [ ] Bảng: header `#F4F6F8` 44, chữ 12 Semi Bold `#667580`; dòng 61–62; cột số căn phải; cột mã Medium.
- [ ] Pill 25 bo tròn đúng tone.
- [ ] Menu active đúng màu và đúng mục; breadcrumb đúng khu vực ("Bán hàng"/"Quản lý").
- [ ] Tiếng Việt có dấu hiển thị đầy đủ, không bị cắt chân chữ (ạ, ặ, ỵ) ở dòng cao 100%.
- [ ] Phím tắt hoạt động (8.2), không kích hoạt nhầm ở trang khác.
- [ ] Không có số liệu mẫu Figma trong bản chạy thật.

### 13.3 Kiểm tra môi trường

- Windows scale 100% / 125% / 150%; cửa sổ min 1280×760 không vỡ bố cục (bảng cuộn, cột co giãn).
- Không có thao tác DB trên EDT (đặt breakpoint/log tên thread trong `doInBackground`).

---

## 14. Rủi ro

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| Thêm dependency (font/icon) không được duyệt | Lệch font/icon | Có phương án B/C (9.3, D-02 b) |
| Cú pháp FlatLaf properties/style class khác version | Theme không áp | Kiểm từng key với tài liệu FlatLaf 3.6; viết test khởi tạo LAF |
| Vẽ bo góc/bóng tay gây giật khi resize | UX | Cache hình bóng; bỏ bóng dialog nếu cần |
| Phạm vi trượt sang nghiệp vụ (tính tiền, sinh mã) | Vi phạm AGENTS §3, C-07/C-11 | View-model nhận số đã tính; không hàm tính toán trong UI |
| Đụng code người khác ở `ui/` | Conflict | Giai đoạn 0 kiểm tra git; PR nhỏ theo giai đoạn |
| `JTable` khó đạt dòng cao + viền trên kiểu Figma | Lệch nhẹ | Renderer tự vẽ border; chấp nhận ±2px |

---

## Phụ lục A — Bảng màu đầy đủ theo nơi dùng

| Hex | Số lần/khu vực | Ghi chú |
|---|---|---|
| `#18252D` | chữ chính mọi nơi | `TEXT` |
| `#667580` | nhãn phụ, icon mặc định | `TEXT_MUTED` (cũng là màu viền frame Figma — không dùng làm viền UI) |
| `#93A0AA` | placeholder, chữ disabled, overline | `TEXT_SUBTLE` |
| `#087F75` | primary | |
| `#075E57` | chữ trên nền primary-soft | |
| `#E8F5F2` / `#D6EEEA` / `#B5DCD6` | primary-soft các mức | |
| `#F4F6F8` | nền app, header bảng | |
| `#FFFFFF` | surface | |
| `#E2E8EC` | viền | |
| `#E5EBEF` | nền disabled | |
| `#163E43` / `#BDDBD6` | panel thương hiệu đăng nhập | |
| `#B42318` / `#FEE4E2` | danger | |
| `#7F0808` / `#F5E8E8` | cảnh báo mạnh | |
| `#152A3D`@40% / `#102635`@15% | overlay / shadow | |
| `#439D93` `#79B8AE` `#ABD1C9` `#D4E7E2` | chart | |
| `#00B3A4` | link lạc tông (X-09) | |

## Phụ lục B — Danh sách icon

Tên icon trong Figma trùng tên bộ **Lucide** (stroke 2, viewBox 24, line cap/join round). Kích thước & màu mặc định theo Figma:

| Icon | Kích thước | Màu | Nơi dùng |
|---|---|---|---|
| `zap` | 22 | trắng (sidebar) / `#087F75` (đăng nhập) | Logo |
| `layout-dashboard` | 19 | muted / primary khi active | Menu Trang chủ |
| `chart-no-axes-combined` | 19 | | Menu Thống kê doanh thu |
| `package-search` | 19 | | Menu Báo cáo tồn kho |
| `users` | 19 | | Menu Quản lý khách hàng |
| `receipt-text` | 19 | | Menu Lập hóa đơn |
| `log-out` | 17 | `#667580` | Topbar |
| `download` | 17 | `#667580` | Xuất báo cáo |
| `search` | 18 | `#667580` | Ô tìm |
| `chevron-down` | 18 | `#667580` | Combo |
| `plus` | 17 | trắng | Thêm khách hàng |
| `x` | 20 | `#667580` | Đóng dialog |
| `info` | 17 | `#087F75` | Callout |
| `phone` | 18 | `#667580` | Ô SĐT |
| `circle-check` | 17 | `#087F75` | Khách đã chọn |
| `tv` | 26 | `#667580` | Kết quả sản phẩm (icon theo danh mục — đề xuất map danh mục → icon, mặc định `package`) |
| `shopping-cart` | 23 | `#93A0AA` | Giỏ trống |
| `calendar-days` | 17 | `#667580` | Nút ngày/kỳ |
| `clock` | 16–18 | `#087F75` | Nhật ký, thời gian |
| `alert-circle` (Lucide mới: `circle-alert`) | 18 | `#B42318` / `#7F0808` | Cảnh báo |
| `user-round` | 18 | `#667580` | Ô tài khoản |
| `lock-keyhole` | 18 | `#667580` | Ô mật khẩu / mã |

## Phụ lục C — Cách giải mã lại file `.fig`

Figma MCP không có trong phiên lập kế hoạch; dữ liệu được lấy bằng cách đọc trực tiếp `ui_dtaq.fig`:

1. `.fig` là file **zip**: `canvas.fig`, `meta.json`, `thumbnail.png`, `images/<sha1>` (PNG).
2. `canvas.fig`: 8 byte `fig-kiwi`, `uint32` version (106), rồi các chunk `uint32 LE length + data`. Chunk 0 = **schema kiwi** (deflate raw), chunk 1 = **dữ liệu** (deflate raw hoặc zstd).
3. Giải schema kiwi (định nghĩa: tên, kind ENUM/STRUCT/MESSAGE, danh sách field `name/type/isArray/value`), rồi giải message gốc `Message` → `nodeChanges[]` (mỗi node có `guid`, `parentIndex{guid, position}`, `type`, `name`, `size`, `transform`, `fillPaints`, `strokePaints`, `cornerRadius`, `stackMode/stackSpacing/stack*Padding`, `fontName`, `fontSize`, `textData.characters`, `prototypeInteractions`…).
4. Dựng cây bằng `parentIndex.guid`, sắp con theo `parentIndex.position` (so sánh chuỗi).

Script Node.js tối giản (Node ≥ 22 có `zlib.zstdDecompressSync`; chạy: `node decode.js canvas.fig out.json`):

```js
const fs=require('fs'),zlib=require('zlib');
const buf=fs.readFileSync(process.argv[2]);let off=12;const chunks=[];
while(off<buf.length){const n=buf.readUInt32LE(off);off+=4;chunks.push(buf.subarray(off,off+n));off+=n;}
const inflate=c=>(c[0]===0x28&&c[1]===0xb5)?zlib.zstdDecompressSync(c):zlib.inflateRawSync(c);
class R{constructor(b){this.b=b;this.i=0}byte(){return this.b[this.i++]}
 vu(){let r=0,s=0,c;do{c=this.b[this.i++];r|=(c&127)<<s;s+=7}while(c&128&&s<35);return r>>>0}
 vi(){const v=this.vu();return(v&1)?~(v>>>1):(v>>>1)}
 f(){if(this.b[this.i]===0){this.i++;return 0}let x=this.b[this.i]|(this.b[this.i+1]<<8)|(this.b[this.i+2]<<16)|(this.b[this.i+3]<<24);this.i+=4;
   x=(x<<23)|(x>>>9);const d=new DataView(new ArrayBuffer(4));d.setUint32(0,x>>>0,true);return d.getFloat32(0,true)}
 s(){const st=this.i;while(this.b[this.i]!==0)this.i++;const r=this.b.toString('utf8',st,this.i);this.i++;return r}
 vu64(){let r=0n,s=0n,c;do{c=this.b[this.i++];r|=BigInt(c&127)<<s;s+=7n}while(c&128);return Number(r)}}
const sr=new R(inflate(chunks[0]));const defs=[];const nd=sr.vu();
for(let k=0;k<nd;k++){const name=sr.s(),kind=sr.byte(),nf=sr.vu(),fields=[];
 for(let j=0;j<nf;j++)fields.push({name:sr.s(),type:sr.vi(),arr:!!sr.byte(),val:sr.vu()});defs.push({name,kind,fields})}
function dec(r,t){switch(t){case-1:return!!r.byte();case-2:return r.byte();case-3:return r.vi();case-4:return r.vu();
 case-5:return r.f();case-6:return r.s();case-7:case-8:return r.vu64()}
 const d=defs[t];if(d.kind===0){const v=r.vu();return(d.fields.find(x=>x.val===v)||{name:v}).name}
 if(d.kind===1){const o={};for(const fd of d.fields)o[fd.name]=field(r,fd);return o}
 const o={};for(;;){const id=r.vu();if(id===0)return o;const fd=d.fields.find(x=>x.val===id);o[fd.name]=field(r,fd)}}
function field(r,fd){if(!fd.arr)return dec(r,fd.type);const n=r.vu();
 if(fd.type===-2){r.i+=n;return{bytes:n}}return Array.from({length:n},()=>dec(r,fd.type))}
const msg=dec(new R(inflate(chunks[1])),defs.findIndex(d=>d.name==='Message'));
fs.writeFileSync(process.argv[3],JSON.stringify(msg));console.log('nodes',msg.nodeChanges.length);
```

Màu trong file ở dạng float 0–1 (`{r,g,b,a}`) → nhân 255 để ra hex. `prototypeInteractions[].actions[].transitionNodeID` cho biết frame đích của mỗi click.
