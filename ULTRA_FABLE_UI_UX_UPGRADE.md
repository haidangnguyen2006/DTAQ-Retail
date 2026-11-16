# ULTRA FABLE UI/UX UPGRADE — Kế hoạch nâng cấp giao diện DTAQ

> **Loại tài liệu:** kế hoạch triển khai (chưa sửa mã Java nào).
> **Ngày lập:** 2026-11-11 · **Nhánh lúc khảo sát:** `feature/anh-w6` (commit `2008c76`).
> **Đối tượng đọc:** lập trình viên hoặc model AI **không có ngữ cảnh phiên làm việc này**. Mọi thứ cần biết đều nằm trong file; không cần hỏi lại người lập.
> **File này bị `.gitignore`** (không commit). Assets ở mục 5 thì **không** bị ignore — chúng cần được commit cùng mã.

---

## 0. Đọc trước khi làm

### 0.1 Mục tiêu một câu

Làm cho giao diện DTAQ hết vẻ "AI tạo ra": đưa màu về đúng **xanh dương của logo**, sửa các lỗi hiển thị gốc, và siết lại chi tiết (chữ số, viền, bóng, trạng thái) — **mà không đổi nội dung và bố cục**.

### 0.2 Ba luật cứng (vi phạm = làm sai)

| # | Luật | Nghĩa cụ thể |
|---|---|---|
| L1 | **Không đổi nội dung** | Không sửa, thêm, bớt chuỗi chữ hiển thị (nhãn, tiêu đề, placeholder, thông báo, tên cột, mục menu). Không đổi dữ liệu, không đổi phím tắt. |
| L2 | **Không đổi bố cục** | Không thêm/bớt/di chuyển thành phần. Không đổi số cột, thứ tự, vùng trang. Không đổi ràng buộc `MigLayout` về cấu trúc. Chỉ được chỉnh ràng buộc **căn hàng** ở đúng các chỗ mục 8.3 liệt kê. |
| L3 | **Không đụng tầng dưới UI** | Chỉ sửa trong `src/main/java/com/dtaq/retail/ui/**`, `src/main/resources/com/dtaq/retail/ui/theme/**` và test tương ứng trong `src/test/java/com/dtaq/retail/ui/**`. Không sửa `controller`, `dao`, `service`, `entity`, `config`, `util`, SQL, `pom.xml`. |

Được phép đổi: màu, font, cỡ chữ, bo góc, viền, bóng, icon, trạng thái hover/focus/disabled, hoạt ảnh trạng thái, hình trang trí (panel thương hiệu ở màn đăng nhập).

### 0.3 Quy tắc làm việc của repo (tóm tắt từ `AGENTS.md`)

- Trả lời và viết chú thích bằng **tiếng Việt**.
- Chạy `git status --short --branch` trước khi sửa. Không reset/checkout đè việc của người khác. Không commit/push khi chưa được yêu cầu.
- Cập nhật Swing trên EDT; không gọi CSDL trên EDT.
- Không thêm thư viện, không thêm framework test (đang dùng JUnit 5).
- `AGENTS.md` mục 6 bảo "tái dùng màu Figma". Kế hoạch này **cố ý lệch** khỏi bảng màu Figma cũ ("Volt Console", `#2457F5`) theo yêu cầu của nhóm: lấy màu chủ đạo theo logo. Bố cục Figma vẫn giữ nguyên. Kế hoạch cũ `UI_UPGRADE_PLAN.md` vẫn đúng về bố cục/đặc tả màn hình; phần **giá trị màu và font** của nó bị thay bằng mục 4 của file này.

### 0.4 Thứ tự thực hiện

Làm đúng thứ tự giai đoạn ở mục 9 (G0 → G5). Mỗi giai đoạn có cổng kiểm tra; không qua cổng thì không sang giai đoạn sau. Mỗi giai đoạn nên là một commit riêng.

---

## 1. Bối cảnh dự án

| Hạng mục | Giá trị đã kiểm chứng |
|---|---|
| Sản phẩm | DTAQ — phần mềm desktop quản lý bán hàng siêu thị điện máy |
| Người dùng | Nhân viên bán hàng (quầy, thao tác nhanh bằng bàn phím F2/F3/F8) và Quản lý (báo cáo, phê duyệt, sao lưu) |
| Công nghệ UI | **Java Swing**, Java 21 (`maven.compiler.release=21`), **FlatLaf 3.6** + `flatlaf-extras` (SVG icon), `flatlaf-fonts-inter` 4.1, **MigLayout 11.4.2** |
| Build | Maven: `mvn clean package` → `target/dtaq-retail-1.0.0-SNAPSHOT.jar` (jar đã gộp thư viện) |
| CSDL | PostgreSQL, cấu hình ở `db.properties` (không commit) |
| Điểm vào | `com.dtaq.retail.Main` → `DtaqTheme.install()` → `AppNavigator.showLogin()` |
| Quy mô UI | ~19.000 dòng trong `ui/`: 7 file theme, 30 component, 9 file shell, 40 trang/hộp thoại |
| Ngôn ngữ giao diện | Tiếng Việt có dấu |

### 1.1 Cấu trúc thư mục UI

```
src/main/java/com/dtaq/retail/ui/
├── AppNavigator.java          điều phối Login → ForgotPassword → AppShell; dựng danh sách trang theo vai trò
├── AuthBrandPanel.java        nửa trái màn đăng nhập/quên mật khẩu (trang trí + logo)
├── LoginFrame.java, ForgotPasswordFrame.java
├── theme/                     ← NGUỒN DUY NHẤT của màu, font, khoảng cách, style
│   ├── DtaqColors.java        hằng màu (tên giữ ổn định, chỉ đổi giá trị)
│   ├── DtaqFonts.java         hằng font (Inter)
│   ├── DtaqSpacing.java       bo góc, chiều cao, khoảng cách
│   ├── DtaqStyles.java        chuỗi style FlatLaf dựng từ DtaqColors
│   ├── DtaqIcons.java         nạp icon SVG Lucide + tô màu
│   ├── DtaqTheme.java         cài font + Look and Feel, nạp logo
│   └── DtaqLightLaf.java      lớp LaF, tự đọc DtaqLightLaf.properties
├── component/                 Card, DtaqButton, DataTable, FormField, KpiCard, StatusPill, InfoCallout,
│                              EmptyState, ModalDialog, Toast, PageHeader, Pagination, SegmentedControl,
│                              BarChartPanel, DonutChartPanel, DatePicker, StatefulView, Tile, ...
├── shell/                     AppShellFrame, Sidebar, NavItem, Topbar, NavigationModel, Page, PageId
└── page/<nhóm>/               các trang nghiệp vụ và hộp thoại
src/main/resources/com/dtaq/retail/ui/theme/
├── DtaqLightLaf.properties    giá trị mặc định UIManager cho FlatLaf
├── dtaq-logo.png (720×240), dtaq-mark.png (256×256)
├── icons/*.svg                Lucide, viewBox 24, stroke="#000000", stroke-width 2
└── fonts/                     (MỚI — xem mục 5)
```

### 1.2 Danh sách màn hình (không được thêm/bớt)

- **Xác thực:** Đăng nhập, Quên mật khẩu.
- **Quản lý (13 trang):** Trang chủ, Thống kê doanh thu, Báo cáo tồn kho lâu ngày, Quản lý dòng sản phẩm, Quản lý sản phẩm lỗi, Quản lý khuyến mãi, Quản lý nhân viên, Thống kê doanh số theo sản phẩm, Thống kê doanh số theo nhân viên, Phê duyệt yêu cầu đổi trả, Cập nhật giá bán và thuế suất, Sao lưu dữ liệu, Phục hồi dữ liệu.
- **Bán hàng (11 trang):** Trang chủ, Quản lý khách hàng, Lập hóa đơn bán lẻ, Tra cứu sản phẩm và tồn kho, Tra cứu khách hàng và giao dịch, Xử lý đổi/trả hàng, Quản lý sản phẩm, Quản lý dòng sản phẩm, Quản lý danh mục sản phẩm, Quản lý sản phẩm lỗi, Quản lý khuyến mãi.
- **Hộp thoại:** dựng trên `ModalDialog` / `ConfirmDialog`; còn 18 chỗ dùng `JOptionPane` gốc.

---

## 2. Chẩn đoán hiện trạng

Người lập đã dựng toàn bộ 25 màn hình ngoài màn hình (công cụ ở Phụ lục B) và đọc mã nguồn. Kết quả:

### 2.1 Lỗi gốc đã kiểm chứng bằng chương trình

| Mã | Lỗi | Bằng chứng | Hậu quả nhìn thấy |
|---|---|---|---|
| **B1** | `DtaqIcons.icon()` làm **mọi icon cùng tên + cùng cỡ dùng chung một màu**. Dòng `base.derive(size, size)` trả về **chính đối tượng trong cache** khi cỡ không đổi, rồi `setColorFilter` ghi đè màu cho tất cả nơi đang dùng. | Chạy thử: `base.derive(18,18) == base` → `true`; `DtaqIcons.icon("clock",18,RED) == DtaqIcons.icon("clock",18,BLUE)` → `true`. | Icon sidebar lúc đỏ lúc xanh ngẫu nhiên; mũi tên breadcrumb màu đỏ ở vai trò Quản lý; icon trong nút "Xuất Excel" biến mất (bị tô trắng). Đây là nguồn lớn nhất của cảm giác "lộn xộn". |
| **B2** | Chữ số Inter **không đều cột** (số 1 hẹp hơn số 0). Java2D không bật được tính năng OpenType `tnum`. | Đo ở 13px: `0`=8,20 · `1`=5,29 · `7`=7,36. | Cột tiền trong bảng không thẳng hàng theo hàng đơn vị; số KPI "nhảy" bề rộng khi cập nhật. |
| **B3** | `Topbar.java:203` đặt style `arc` cho `JPopupMenu` — khóa không tồn tại. | Log mỗi lần mở shell: `FlatStylingSupport$UnknownStyleException: unknown style 'arc'`. | Menu tài khoản không nhận style mong muốn; log bẩn. |
| **B4** | Nút đứng cạnh `FormField` bị **tụt xuống ~20px** so với ô nhập. Hàng dùng `"[bottom]"`, trong khi `FormField` luôn chừa 4px + dòng thông báo cao 16px dưới ô nhập. | Thấy ở Lập hóa đơn ("Tìm khách hàng"), Quản lý dòng sản phẩm ("Tìm kiếm"), Xử lý đổi trả ("Tra cứu"). | Nút và ô nhập lệch hàng. |
| **B5** | `KpiCard` không có chú thích thì **thấp hơn** thẻ bên cạnh. | Thống kê doanh thu: thẻ "Tổng doanh thu trong kỳ" lệch kích thước với 3 thẻ còn lại. | Hàng KPI so le. |

### 2.2 Vì sao trông "AI slop"

| Mã | Dấu hiệu | Vị trí |
|---|---|---|
| S1 | **Màu chủ đạo không phải màu thương hiệu.** UI dùng `#2457F5` (sắc độ 265°, ngả chàm). Logo dùng xanh azure `#0384FF` (255°) và xanh navy `#00183C` (257°). | `DtaqColors`, `DtaqLightLaf.properties` |
| S2 | Sidebar `#111826` gần như xám đen (độ bão hòa 0,03), không cùng họ với navy của logo (0,08). | `DtaqColors.SIDEBAR_*` |
| S3 | Logo có sẵn nền trắng bo tròn, đặt lên sidebar tối → trông như **miếng dán**. | `Sidebar.buildBrandBlock()` |
| S4 | Màn đăng nhập: lưới chấm + đường mạch điện + gradient tỏa — bộ ba trang trí kiểu AI điển hình. | `AuthBrandPanel.render()` |
| S5 | `InfoCallout` có **vạch màu dày 3px bên trái**. | `InfoCallout.paintComponent()` |
| S6 | Mọi thứ đều là thẻ trắng viền xám bo 12 giống hệt nhau trên nền xám; hộp thoại không có bóng nên không có chiều sâu. | `Card`, `ModalDialog` |
| S7 | Bo góc 8px cho mọi control + nhiều cỡ chữ gần nhau (10,5 / 11 / 12 / 12,5 / 13 / 14 / 15 / 16 / 17 / 18 / 20 / 22 / 26 / 28 / 32). | `DtaqSpacing`, `DtaqFonts` |
| S8 | Icon trùng nhau trong menu (2 mục dùng biểu đồ, 2 mục dùng `users`, 3 mục dùng `receipt-text`); icon sai nghĩa (giỏ hàng cho "dòng sản phẩm", `info` cho "danh mục"). | `NavigationModel` |
| S9 | Dùng ký tự/emoji thay icon: `"↑ "`, `"↓ "`, `"● Hoạt động"`, `"🟢 Cần nhập thêm…"`. Emoji hiển thị không ổn định trong Swing. | `KpiCard`, `EmployeePage`, `SalesByProductPage` |
| S10 | `StatusPill` trung tính (ví dụ ngày tháng) cũng có chấm tròn trạng thái → chấm mất nghĩa. | `StatusPill` |
| S11 | Mã màu viết cứng ngoài theme: 6 chỗ `new Color(0x…)` trong component, 3 chuỗi style có `#hex`. | xem mục 8.1 |
| S12 | Placeholder `#8792A3` trên nền trắng chỉ đạt tương phản ~3,2:1 (chuẩn tối thiểu 4,5:1). | `TEXT_SUBTLE` |
| S13 | Cột 0 của biểu đồ cột vẫn vẽ một vạch nhỏ → 28 vạch nhiễu ở chân biểu đồ. | `BarChartPanel` |

### 2.3 Điều đã loại trừ

- Ảnh chụp ở tỉ lệ 1,5× cho thấy chữ bị cắt đuôi; chụp lại ở 1× thì **không còn**. Đó là sai lệch của công cụ chụp, không phải lỗi ứng dụng. Vẫn nên xem thật ở Windows 125%/150% trong G5.
- Bảng "Lịch sử sao lưu" không giãn hết bề ngang trong ảnh chụp: chưa xác nhận được là lỗi thật. Kiểm tra bằng mắt ở G5; nếu thật thì ghi nhận, không tự sửa bố cục.

---

## 3. Định hướng thiết kế

**Tên hướng:** *DTAQ Azure*. Đây là **tinh chỉnh**, không phải thiết kế lại: giữ nguyên khung, thay "chất liệu".

**Loại bề mặt:** công cụ làm việc. Người dùng đến để hoàn thành tác vụ; giao diện phải quen tay, dày thông tin, nhất quán. Thương hiệu thể hiện ở độ chính xác của chi tiết, không ở trang trí.

**Bối cảnh sử dụng:** quầy bán hàng và văn phòng siêu thị, ánh sáng đèn trắng mạnh → **giao diện sáng**, sidebar navy đậm làm khung.

Thương hiệu chỉ được đóng góp đúng bốn thứ:

1. **Bảng màu** lấy từ logo: azure cho hành động, navy cho khung (mục 4.1).
2. **Chữ**: một họ Inter duy nhất; số liệu dùng bản số đều cột (mục 4.2).
3. **Mật độ**: giữ nguyên khoảng cách hiện tại (đổi sẽ lệch bố cục); chỉ siết bo góc (mục 4.3).
4. **Một nét riêng — "quỹ đạo DTAQ"**: hai cung elip mảnh lấy từ nét vòng cung trong logo, **chỉ xuất hiện ở panel thương hiệu của màn đăng nhập/quên mật khẩu** (mục 7.2). Không lặp lại ở nơi khác.

**Không làm:** gradient chữ, hiệu ứng kính mờ, vạch màu bên cạnh thẻ, bóng cứng lệch, hoạt ảnh trang trí, thêm thẻ mới, đổi hộp thoại thành trang.

---

## 4. Hệ thống thiết kế (giá trị chính xác)

### 4.1 Màu

**Cách tính:** lấy mẫu điểm ảnh logo → `#0384FF` (azure) và `#00183C` (navy). Dựng thang màu trong không gian OKLCH ở sắc độ 255,5°, rồi kiểm tra tương phản WCAG bằng chương trình. Mọi con số dưới đây là **đã tính**, không ước lượng.

#### Thang thương hiệu (tham chiếu)

| Bậc | Hex | Tương phản với trắng | Dùng cho |
|---|---|---|---|
| 50 | `#F2F7FE` | 1,08 | nền hover của hàng bảng |
| 100 | `#E4F0FF` | 1,15 | nền chọn, nền callout thông tin, avatar |
| 200 | `#C9E0FE` | 1,35 | nền chọn văn bản, nền nhấn |
| 300 | `#9DC7FF` | 1,74 | viền nhạt của nút/callout xanh |
| 400 | `#62A6FE` | 2,50 | vòng focus, chỉ báo đang chọn trên sidebar |
| 500 | `#1886FE` | 3,57 | **chỉ dùng cho hình** (cột biểu đồ, nét trang trí) |
| — | `#0384FF` | 3,65 | màu logo gốc — **không dùng cho chữ nhỏ hay nền nút** vì không đạt 4,5:1 |
| **600** | **`#006DD7`** | **5,04** | **màu hành động chính** (nền nút chính, liên kết, viền focus) |
| 650 | `#0062C2` | 6,0 | hover của nút chính |
| 700 | `#0058B0` | 6,95 | pressed, chữ xanh trên nền xanh nhạt |
| 950 | `#001D44` | 16,68 | nền sidebar, nền panel thương hiệu |

> **Lý do nút chính là `#006DD7` chứ không phải đúng màu logo `#0384FF`:** chữ trắng trên `#0384FF` chỉ đạt 3,65:1, dưới chuẩn 4,5:1. `#006DD7` cùng sắc độ, đậm hơn một bậc, đạt 5,04:1. Mắt vẫn nhận là "xanh DTAQ".

#### Bảng thay giá trị `DtaqColors` (tên hằng **giữ nguyên**)

| Hằng | Cũ | **Mới** | Ghi chú |
|---|---|---|---|
| `PRIMARY` | `#2457F5` | `#006DD7` | trắng trên nền này: 5,04:1 |
| `PRIMARY_HOVER` | `#1F4FD8` | `#0062C2` | |
| `PRIMARY_STRONG` | `#1A3FB0` | `#0058B0` | trên `PRIMARY_SOFT`: 6,02:1 |
| `PRIMARY_SOFT` | `#EEF3FF` | `#E4F0FF` | |
| `PRIMARY_SOFT_2` | `#DCE6FF` | `#C9E0FE` | |
| `PRIMARY_BORDER_SOFT` | `#BCCDFF` | `#9DC7FF` | |
| `BRAND_DARK` | `#111826` | `#001D44` | |
| `BRAND_ON_DARK_MUTED` | `#8B97AB` | `#88A7D0` | |
| `TEXT` | `#151A24` | `#111B28` | 17,34:1 |
| `TEXT_MUTED` | `#5B6577` | `#5A6472` | 6,00:1 (trên `BG_APP`: 5,53:1) |
| `TEXT_SUBTLE` | `#8792A3` | `#6B7583` | 4,67:1 — sửa S12 |
| `BG_APP` | `#F4F6FA` | `#F3F6FA` | |
| `SURFACE` | `#FFFFFF` | `#FFFFFF` | giữ |
| `SURFACE_SUNKEN` | `#FAFBFD` | `#F8FAFD` | |
| `BORDER` | `#DDE2EB` | `#DAE0E8` | |
| `BORDER_STRONG` | `#C5CDD9` | `#AFB8C4` | viền ô nhập rõ hơn |
| `BORDER_HOVER` | `#8792A3` | `#77818F` | |
| `DISABLED_BG` | `#ECEFF5` | `#EAEEF4` | |
| `DANGER`, `DANGER_SOFT`, `DANGER_STRONG`, `DANGER_SOFT_2`, `DANGER_SOFT_HOVER` | — | **giữ nguyên** | đã đạt: 5,50 / 6,63 / 4,81 |
| `SUCCESS`, `SUCCESS_SOFT`, `WARNING`, `WARNING_SOFT` | — | **giữ nguyên** | đã đạt: 5,27 / 4,66 / 4,98 / 4,56 |
| `ACCENT`, `ACCENT_SOFT`, `ACCENT_TEXT` | — | **giữ nguyên** | hổ phách đi cùng xanh; 6,85:1 |
| `ROW_HOVER` | `#F7F9FC` | `#F2F7FE` | |
| `FOCUS_RING` | `alpha(PRIMARY, .40)` | `alpha(BRAND, .45)` | |
| `OVERLAY` | `alpha(#0E121A, .45)` | `alpha(#07101C, .50)` | |
| `SHADOW` | `alpha(#0E121A, .12)` | `alpha(#07101C, .12)` | |
| `SIDEBAR_BG` | `#111826` | `#001D44` | |
| `SIDEBAR_HOVER` | `#1A2233` | `#002957` | |
| `SIDEBAR_ACTIVE_BG` | `#1E2A47` | `#003671` | |
| `SIDEBAR_TEXT` | `#C9D1DD` | `#C4D9F6` | ~12:1 trên nền sidebar |
| `SIDEBAR_TEXT_MUTED` | `#8B97AB` | `#88A7D0` | ~7:1 |
| `SIDEBAR_ACTIVE_TEXT` | `#FFFFFF` | `#FFFFFF` | giữ |
| `SIDEBAR_ACCENT` | `#8EA9FC` | `#62A6FE` | 6,67:1 |
| `SIDEBAR_DIVIDER` | `#1F2633` | `#112E52` | |
| `CHART_1` | `#2457F5` | `#1886FE` | |
| `CHART_2` | `#12A594` | `#EB6834` | |
| `CHART_3` | `#D98A0B` | `#1BAF7A` | |
| `CHART_4` | `#8E5CF6` | `#EDA100` | |
| `CHART_5` | `#E5484D` | `#6250D6` | |
| `CHART_6` | `#64748B` | `#77818F` | chỉ dành cho nhóm "Khác" |
| `CHART_GRID` | `#ECEFF5` | `#EAEEF4` | |
| `CHART_AXIS` | `#5B6577` | `#5A6472` | |

#### Hằng **thêm mới** vào `DtaqColors`

```java
// --- Thương hiệu (màu gốc của logo; chỉ dùng cho hình, không dùng cho chữ nhỏ/nền nút) ---
public static final Color BRAND = new Color(0x0384FF);
public static final Color BRAND_400 = new Color(0x62A6FE);

// --- Bổ sung để xóa mã màu viết cứng trong component ---
public static final Color PILL_NEUTRAL_TEXT = new Color(0x3E4957);      // 7,86:1 trên DISABLED_BG
public static final Color TOAST_BG = new Color(0x111B28);
public static final Color TOAST_SUCCESS = new Color(0x5DD39E);
public static final Color TOAST_ERROR = new Color(0xFF8A85);
public static final Color OVERLAY_OPAQUE = new Color(0xC7CED8);         // nền phủ khi máy không hỗ trợ trong suốt
public static final Color SIDEBAR_SCROLL_THUMB = new Color(0x1E4273);
public static final Color SIDEBAR_SCROLL_THUMB_HOVER = new Color(0x2F5C99);
public static final Color WARNING_BORDER = new Color(0xF0D9A0);
public static final Color DANGER_BORDER = new Color(0xF2B8B3);
```

#### Bảng màu biểu đồ

Thứ tự `CHART_1…CHART_5` = `#1886FE, #EB6834, #1BAF7A, #EDA100, #6250D6`. Đã chạy trình kiểm bảng màu phân loại: **đạt** dải độ sáng, sàn độ bão hòa, phân biệt cho người mù màu (ΔE xấu nhất 9,1 ≥ 8), phân biệt thị lực thường (22,9 ≥ 15). Cảnh báo duy nhất: `#1BAF7A` và `#EDA100` dưới 3:1 so với nền → **bắt buộc có nhãn chữ bên cạnh** (chú giải donut hiện đã có tên + số tiền + %: đủ điều kiện).

Quy tắc dùng:

- Màu gán theo **thứ tự cố định**, không xoay vòng. Mục thứ 6 trở đi gộp thành "Khác" dùng `CHART_6`.
- Biểu đồ một chuỗi (cột doanh thu theo ngày) chỉ dùng `CHART_1`.
- Màu trạng thái (`SUCCESS`/`WARNING`/`DANGER`) **không** dùng làm màu chuỗi.
- Chữ trên biểu đồ (giá trị, nhãn trục, chú giải) dùng `TEXT`/`TEXT_MUTED`, không dùng màu chuỗi.

### 4.2 Chữ

Giữ **Inter** (đã có sẵn, hỗ trợ tiếng Việt đầy đủ, hợp giao diện công cụ). Không thêm họ chữ thứ hai cho tiêu đề.

**Thêm "DTAQ Numerals"** — bản Inter 4.1 đã "đóng băng" chữ số đều cột (sửa B2). File đã nằm trong repo (mục 5). Mọi số tiền, số lượng, mã, ngày giờ trong bảng và KPI dùng font này. Cùng thông số dòng với Inter nên **không làm xê dịch bố cục**.

**Gom cỡ chữ** từ 15 bậc xuống 10 bậc: `11 · 12 · 13 · 14 · 16 · 18 · 22 · 24 · 28 · 32`.

| Hằng `DtaqFonts` | Cũ | **Mới** | Font |
|---|---|---|---|
| `DISPLAY` | semibold 28, tracking −0,6 | giữ | Inter |
| `AMOUNT_XL` | semibold 32, −0,8 | `numeric(true, 32f, -0.8f)` | **Numerals** |
| `PAGE_TITLE` | semibold 22, −0,4 | giữ | Inter |
| `KPI_VALUE` | semibold 26, −0,5 | `numeric(true, 24f, -0.4f)` | **Numerals** |
| `DIALOG_TITLE` | semibold 18, −0,2 | giữ | Inter |
| `DONUT_CENTER` | semibold 20, −0,3 | `numeric(true, 18f, -0.2f)` | **Numerals** |
| `BRAND` | semibold 17, +0,4 | semibold 16, 0 | Inter |
| `SECTION_TITLE_STRONG`, `CARD_TITLE`, `CARD_TITLE_SEMI` | semibold 15 | semibold 16 | Inter |
| `CUSTOMER_NAME` | semibold 16 | giữ | Inter |
| `EMPTY_TITLE` | semibold 14 | giữ | Inter |
| `BODY_14`, `BODY_13`, `BODY_13_MEDIUM`, `LABEL`, `BUTTON`, `SMALL_BUTTON`, `PILL`, `CAPTION`, `HINT`, `HINT_ON_PRIMARY`, `KBD`, `ITALIC_HINT` | — | giữ | Inter |
| `AMOUNT_ROW` | semibold 14 | `numeric(true, 14f, 0f)` | **Numerals** |
| `TABLE_HEADER` | semibold 12, +0,1 | semibold 12, 0 | Inter |
| `EYEBROW` | semibold 11, +0,6 | semibold 11, +0,4 | Inter |
| `OVERLINE` | semibold 11, +0,8 | semibold 11, +0,5 | Inter |
| `MICRO` | plain 10,5 | plain 11 | Inter |
| `MONEY_TABLE` | semibold 13 | `numeric(true, 13f, 0f)` | **Numerals** |
| `CODE` | semibold 12,5, +0,2 | `numeric(true, 12f, 0f)` | **Numerals** |
| `MONEY_INPUT` | semibold 18 | `numeric(true, 18f, 0f)` | **Numerals** |
| **`NUMERIC_13`** (mới) | — | `numeric(false, 13f, 0f)` | **Numerals** |
| **`NUMERIC_12`** (mới) | — | `numeric(false, 12f, 0f)` | **Numerals** |

`KpiCard.setValue()` đang tự hạ cỡ chữ khi số dài bằng `DtaqFonts.semibold(22f, -0.4f)` và `DIALOG_TITLE`: đổi hai chỗ đó thành `DtaqFonts.numeric(true, 22f, -0.4f)` và `DtaqFonts.numeric(true, 18f, -0.2f)`.

### 4.3 Bo góc, viền, bóng

| Hằng `DtaqSpacing` | Cũ | **Mới** |
|---|---|---|
| `RADIUS_S` | 6 | 4 |
| `RADIUS_M` | 8 | 6 |
| `RADIUS_TABLE`, `RADIUS_CARD_SM` | 10 | 8 |
| `RADIUS_L` | 12 | 10 |
| `RADIUS_DIALOG` | 16 | 14 |
| `RADIUS_PILL` | 100 | giữ |
| **`FIELD_MESSAGE_H`** (mới) | — | `20` (= 4px khoảng cách + 16px dòng thông báo của `FormField`) |
| Mọi hằng chiều cao/khoảng cách khác | — | **giữ nguyên** (đổi là lệch bố cục) |

- Control (nút, ô nhập, combo): bán kính **6** → trong chuỗi style FlatLaf là `arc: 12` (FlatLaf dùng đường kính). Nút nhỏ trong bảng: `arc: 8`.
- **Viền:** 1px. Thẻ dùng `BORDER`; ô nhập và nút phụ dùng `BORDER_STRONG`.
- **Bóng:** thẻ trên trang **không có bóng** (đã có viền). Bóng chỉ dành cho lớp nổi: hộp thoại, toast, popup. Bóng luôn có độ lệch dọc + độ nhòe, không dùng quầng màu lệch 0.
- **Vòng focus:** 2px, màu `#62A6FE`, hiện với mọi control khi dùng bàn phím. Không được tắt.

### 4.4 Chuyển động

Giữ các hoạt ảnh trạng thái đang có (chỉ báo sidebar 160ms, toast 180/140ms, cột mọc 280ms, donut 180ms). **Không thêm** hoạt ảnh mới. Mọi hoạt ảnh phải tiếp tục tôn trọng `Animator.useAnimation()`.

---

## 5. Assets đã tải về và đã kiểm chứng

Người lập **đã đặt sẵn** các file dưới đây vào repo (chưa commit, đang ở trạng thái untracked). Không cần tải lại. Nếu thiếu, tái tạo theo mục 5.3.

### 5.1 Icon Lucide bổ sung (29 file)

Thư mục: `src/main/resources/com/dtaq/retail/ui/theme/icons/` (tổng 63 file sau khi thêm).
Nguồn: `lucide-static@1.71.0` (giấy phép ISC). Đã chuẩn hóa giống icon cũ: một dòng, bỏ chú thích và thuộc tính `class`, `stroke="currentColor"` → `stroke="#000000"`. Đã nạp thử cả 63 file bằng `FlatSVGIcon`: **63/63 vẽ được**.

```
arrow-down  arrow-left-right  arrow-up  banknote  chart-column  circle-alert  circle-x
clipboard-check  corner-down-left  database-backup  eye  eye-off  file-spreadsheet  history
hourglass  id-card  inbox  layers  minus  package-x  pencil  printer  tags  trash-2
trending-down  trending-up  triangle-alert  upload  user-round-search
```

Kế hoạch này dùng chắc chắn: `chart-column, id-card, arrow-left-right, tags, layers, package-x, banknote, database-backup, history, clipboard-check, user-round-search, hourglass, trending-up, trending-down`. Số còn lại là dự phòng cùng bộ; **không tự ý gắn vào giao diện** nếu không có mục nào trong file này yêu cầu.

### 5.2 Font số đều cột

Thư mục: `src/main/resources/com/dtaq/retail/ui/theme/fonts/`

| File | Nội dung |
|---|---|
| `DtaqNumerals-Regular.otf` | Inter Regular 4.1 + `tnum` đóng băng, tên họ `DTAQ Numerals` |
| `DtaqNumerals-SemiBold.otf` | Inter SemiBold 4.1 + `tnum` đóng băng, tên họ `DTAQ Numerals SemiBold` |
| `OFL-Inter.txt` | Giấy phép SIL OFL 1.1 gốc của Inter (bắt buộc đi kèm) |
| `README.txt` | Ghi nguồn gốc và lý do |

Đã kiểm bằng Java: nạp được qua `Font.createFont`; ở 13px mọi chữ số rộng đúng 8,43 (Regular) / 8,41 (SemiBold); dấu `.` và `,` cũng là bản đều cột; hiển thị được `"1.250.000 VNĐ ₫ Đặng Hồng"`.

### 5.3 Cách tái tạo (chỉ khi mất file)

```bash
# Icon — với mỗi tên ở mục 5.1:
curl -sL -o <tên>.svg https://unpkg.com/lucide-static@1.71.0/icons/<tên>.svg
#   rồi: xóa <!-- ... -->, xóa class="...", đổi stroke="currentColor" thành stroke="#000000", gộp về một dòng.

# Font — cần Python + fonttools; nguồn là Inter-Regular.otf / Inter-SemiBold.otf trong
#   jar flatlaf-fonts-inter-4.1 (đường dẫn com/formdev/flatlaf/fonts/inter/).
#   Thuật toán: đọc các lookup thay thế đơn của feature GSUB 'tnum' → áp ánh xạ đó vào mọi bảng cmap
#   → đổi name ID 1/4/6 thành "DTAQ Numerals[ SemiBold]" → lưu.
```

---

## 6. Sửa nền tảng theme (giai đoạn G1)

### 6.1 `DtaqIcons.java` — sửa B1

Thay thân phương thức `icon(String, int, Color)`. Dùng **hàm dựng sao chép** `new FlatSVGIcon(base)` (đã kiểm: tạo đối tượng mới) và cache theo cả màu.

```java
private static final ConcurrentMap<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();

public static Icon icon(String name, int size, Color color) {
  Objects.requireNonNull(name, "name");
  String resourcePath = ICONS_PACKAGE + name + ".svg";
  String baseKey = resourcePath + "@" + size;
  FlatSVGIcon base = CACHE.computeIfAbsent(baseKey, k -> new FlatSVGIcon(resourcePath, size, size));
  if (color == null) {
    return base;
  }
  // Mỗi màu một bản riêng: derive(size, size) trả về chính bản trong cache nên không dùng được.
  return CACHE.computeIfAbsent(baseKey + "#" + Integer.toHexString(color.getRGB()), k -> {
    FlatSVGIcon tinted = new FlatSVGIcon(base);
    tinted.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
    return tinted;
  });
}
```

Giữ nguyên hai overload còn lại và `paddedLeft`.

**Test thêm vào `DtaqIconsTest`:**

```java
@Test
@DisplayName("Hai màu khác nhau của cùng một icon không dùng chung đối tượng")
void testTintKhongDungChung() {
  Icon red = DtaqIcons.icon("clock", 18, Color.RED);
  Icon blue = DtaqIcons.icon("clock", 18, Color.BLUE);
  assertNotSame(red, blue);
  assertSame(red, DtaqIcons.icon("clock", 18, Color.RED));
}
```

Đồng thời thêm 14 tên icon "dùng chắc chắn" ở mục 5.1 vào danh sách `ALL_ICONS` và sửa chữ "28 icon" trong `@DisplayName` thành số đúng.

### 6.2 `DtaqTheme.java` — nạp font số

Thêm (không đổi phần đang có):

```java
private static volatile java.awt.Font numeralRegular;
private static volatile java.awt.Font numeralSemibold;
private static volatile boolean numeralsLoaded;

/** Font số đều cột; trả null nếu không nạp được để bên gọi rơi về Inter. An toàn khi gọi nhiều lần. */
public static java.awt.Font numeralBase(boolean semibold) {
  if (!numeralsLoaded) {
    synchronized (DtaqTheme.class) {
      if (!numeralsLoaded) {
        numeralRegular = loadFont("fonts/DtaqNumerals-Regular.otf");
        numeralSemibold = loadFont("fonts/DtaqNumerals-SemiBold.otf");
        numeralsLoaded = true;
      }
    }
  }
  return semibold ? numeralSemibold : numeralRegular;
}

private static java.awt.Font loadFont(String resource) {
  try (java.io.InputStream in = DtaqTheme.class.getResourceAsStream(resource)) {
    if (in == null) {
      log.warn("Không tìm thấy font {}", resource);
      return null;
    }
    java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
    return font;
  } catch (java.awt.FontFormatException | java.io.IOException e) {
    log.warn("Không nạp được font {}", resource, e);
    return null;
  }
}
```

Lưu ý: `Font.TRUETYPE_FONT` là hằng đúng cho cả file `.otf`.

### 6.3 `DtaqFonts.java`

Thêm phương thức, rồi áp bảng ở mục 4.2:

```java
/** Font số đều cột (DTAQ Numerals); tự rơi về Inter nếu không nạp được file font. */
public static Font numeric(boolean semibold, float size, float trackingPx) {
  Font base = DtaqTheme.numeralBase(semibold);
  if (base == null) {
    return semibold ? semibold(size, trackingPx) : font(Font.PLAIN, size, trackingPx);
  }
  return withTracking(base.deriveFont(size), size, trackingPx);
}
```

Thứ tự khai báo: đặt `numeric(...)` **phía trên** các hằng `static final` dùng nó (cùng chỗ với `semibold`). Không tạo vòng phụ thuộc khởi tạo: `DtaqTheme.numeralBase` không đụng tới `DtaqFonts`.

### 6.4 `DtaqColors.java`

Áp đúng hai bảng ở mục 4.1. Sửa câu chú thích đầu lớp: bỏ chữ "Volt Console", ghi "DTAQ Azure — màu lấy từ logo". Sửa `INFO`, `INFO_SOFT`, `LINK_ACCENT`: giữ nguyên cách tham chiếu `PRIMARY`/`PRIMARY_SOFT`.

`BRAND` phải được khai báo **trước** `FOCUS_RING` (vì `FOCUS_RING = alpha(BRAND, 0.45f)`).

### 6.5 `DtaqSpacing.java`

Áp bảng mục 4.3. `arc()` và `normalizeRowHeight()` không đổi.

### 6.6 `DtaqStyles.java`

- Thay mọi `arc: 16` thành `arc: 12` (`TEXT_FIELD`, `BUTTON_PRIMARY`, `BUTTON_SECONDARY`, `BUTTON_DANGER`, `BUTTON_DANGER_SOLID`, `BUTTON_GHOST`, `TOGGLE_SEGMENT`, `navItem`).
- `SMALL_BUTTON_EXTRA`: `arc: 12` → `arc: 8`.
- `BUTTON_GHOST`: giữ nguyên chuỗi `background: #00000000` (đang chạy được); chỉ đổi `arc`.
- `navItem`: không đổi cấu trúc; màu tự đổi theo `DtaqColors`.

### 6.7 `DtaqLightLaf.properties` — thay toàn bộ nội dung

```properties
@accentColor = #006DD7
@background = #F3F6FA
@foreground = #111B28
@disabledForeground = #6B7583
@selectionBackground = #E4F0FF
@selectionForeground = #111B28
@selectionInactiveBackground = #EAEEF4

Component.arc = 12
Button.arc = 12
TextComponent.arc = 12
CheckBox.arc = 6
# Viền focus 2px để người dùng bàn phím nhìn thấy ô đang chọn
Component.focusWidth = 2
Component.innerFocusWidth = 0
Component.focusColor = #62A6FE
Component.borderColor = #AFB8C4
Component.focusedBorderColor = #006DD7
Component.disabledBorderColor = #DAE0E8

Button.background = #FFFFFF
Button.foreground = #111B28
Button.borderColor = #AFB8C4
Button.hoverBackground = #F3F6FA
Button.pressedBackground = #EAEEF4
Button.default.background = #006DD7
Button.default.foreground = #FFFFFF
Button.default.hoverBackground = #0062C2
Button.default.pressedBackground = #0058B0
Button.default.borderWidth = 0
Button.disabledBackground = #EAEEF4
Button.toolbar.hoverBackground = #EAEEF4
Button.toolbar.pressedBackground = #DAE0E8

TextField.placeholderForeground = #6B7583
TextField.margin = 0,12,0,12
TextField.selectionBackground = #C9E0FE
TextField.selectionForeground = #111B28
TextField.caretForeground = #006DD7
PasswordField.margin = 0,12,0,12
PasswordField.showRevealButton = true
PasswordField.selectionBackground = #C9E0FE
PasswordField.selectionForeground = #111B28
PasswordField.caretForeground = #006DD7
FormattedTextField.selectionBackground = #C9E0FE
FormattedTextField.selectionForeground = #111B28
FormattedTextField.caretForeground = #006DD7
TextArea.margin = 8,12,8,12
TextArea.selectionBackground = #C9E0FE
TextArea.selectionForeground = #111B28
TextArea.caretForeground = #006DD7
ComboBox.padding = 0,12,0,8
ComboBox.selectionBackground = #E4F0FF
ComboBox.selectionForeground = #111B28

Table.rowHeight = 40
Table.showHorizontalLines = true
Table.showVerticalLines = false
Table.intercellSpacing = 0,1
Table.gridColor = #EAEEF4
Table.selectionBackground = #E4F0FF
Table.selectionForeground = #111B28
Table.selectionInactiveBackground = #F3F6FA
Table.selectionInactiveForeground = #111B28
TableHeader.background = #F8FAFD
TableHeader.foreground = #5A6472
TableHeader.height = 36
TableHeader.separatorColor = #F8FAFD
TableHeader.bottomSeparatorColor = #DAE0E8

ScrollBar.width = 10
ScrollBar.thumbArc = 999
ScrollBar.thumbInsets = 2,2,2,2
ScrollBar.thumb = #C7CED8
ScrollBar.hoverThumbColor = #77818F
ScrollPane.smoothScrolling = true
ScrollPane.border = 0,0,0,0

TabbedPane.tabType = underlined
TabbedPane.tabHeight = 40
TabbedPane.underlineColor = #006DD7
TabbedPane.inactiveUnderlineColor = #62A6FE
TabbedPane.hoverColor = #F3F6FA
TabbedPane.focusColor = #E4F0FF
TabbedPane.selectedForeground = #111B28
TabbedPane.foreground = #5A6472
TabbedPane.contentAreaColor = #DAE0E8
TabbedPane.showTabSeparators = false

Popup.dropShadowPainted = true
PopupMenu.borderCornerRadius = 10
PopupMenu.borderColor = #DAE0E8
MenuItem.selectionBackground = #E4F0FF
MenuItem.selectionForeground = #111B28
ToolTip.background = #001D44
ToolTip.foreground = #FFFFFF
ToolTip.border = 6,10,6,10

ProgressBar.arc = 999
ProgressBar.foreground = #006DD7
ProgressBar.background = #EAEEF4
CheckBox.icon.selectedBackground = #006DD7
CheckBox.icon.checkmarkColor = #FFFFFF
Separator.foreground = #DAE0E8

Panel.background = #F3F6FA
OptionPane.background = #FFFFFF
```

Khóa nào FlatLaf không biết trong file `.properties` sẽ bị bỏ qua êm, không gây lỗi. (Khác với chuỗi `FlatClientProperties.STYLE` trong mã Java: khóa sai ở đó **sinh ngoại lệ trong log** — xem quy tắc Q3 mục 10.)

### 6.8 Cập nhật test theme (bắt buộc, nếu không build sẽ đỏ)

| Test | Việc cần làm |
|---|---|
| `DtaqColorsTest` | Thay toàn bộ giá trị mong đợi theo cột **Mới** của mục 4.1. Sửa `@DisplayName` có chữ "Figma" thành "đặc tả DTAQ Azure". Thêm khẳng định cho `BRAND = #0384FF`. |
| `DtaqStylesTest` | `hex(PRIMARY)` → `#006DD7`; `hex(FOCUS_RING)` → `#0384FF`; `BUTTON_PRIMARY` chứa `background: #006DD7`, `hoverBackground: #0062C2`, `arc: 12`. |
| `DtaqSpacingTest` | Không đổi (`arc(8) == 16` vẫn đúng). |
| `DtaqIconsTest` | Như mục 6.1. |

**Test tương phản mới** `DtaqContrastTest` (cùng gói `ui.theme`), để không ai làm tụt chuẩn về sau:

```java
private static double lum(Color c) {
  double[] v = {c.getRed() / 255d, c.getGreen() / 255d, c.getBlue() / 255d};
  for (int i = 0; i < 3; i++) v[i] = v[i] <= 0.04045 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
  return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
}
private static double ratio(Color a, Color b) {
  double x = lum(a), y = lum(b);
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
}
```

Khẳng định `>= 4.5` cho các cặp: `TEXT`/`SURFACE`, `TEXT_MUTED`/`SURFACE`, `TEXT_MUTED`/`BG_APP`, `TEXT_SUBTLE`/`SURFACE`, `SURFACE`/`PRIMARY`, `SURFACE`/`PRIMARY_HOVER`, `PRIMARY_STRONG`/`PRIMARY_SOFT`, `SUCCESS`/`SUCCESS_SOFT`, `WARNING`/`WARNING_SOFT`, `DANGER_STRONG`/`DANGER_SOFT`, `SURFACE`/`DANGER`, `SIDEBAR_TEXT`/`SIDEBAR_BG`, `SIDEBAR_TEXT_MUTED`/`SIDEBAR_BG`, `SIDEBAR_ACTIVE_TEXT`/`SIDEBAR_ACTIVE_BG`, `PILL_NEUTRAL_TEXT`/`DISABLED_BG`.

---

## 7. Sửa component, shell, màn xác thực (giai đoạn G2–G3)

Mỗi mục ghi: file → việc cần làm → điều **không** được đổi.

### 7.1 Component (`ui/component/`)

**`KpiCard.java`** — sửa B5, S9
- Luôn giữ một dòng chú thích: trong `setCaption`, khi `caption` rỗng/null thì đặt text là `" "` (một dấu cách) thay vì `""`. Thêm ràng buộc chiều cao tối thiểu cho `captionLabel` khi `add`: `"wrap, hmin 16"`.
- `Trend`: đổi trường `prefix` thành tên icon (`UP` → `"trending-up"`, `DOWN` → `"trending-down"`, `NEUTRAL` → `null`). Trong `setTrend`: `captionLabel.setText(text)`, `captionLabel.setIcon(iconName == null ? null : DtaqIcons.icon(iconName, 14, trend.color))`, `setIconTextGap(4)`. Trong `setCaption` và `setLoading`: `captionLabel.setIcon(null)`.
- Không đổi: chữ ký hàm dựng, các phương thức public, chuỗi `"Đang tải…"`, `"—"`.

**`StatusPill.java`** — sửa S10, S11
- `NEUTRAL` dùng `DtaqColors.PILL_NEUTRAL_TEXT` thay `new Color(0x465062)`.
- Kiểu `NEUTRAL` **không vẽ chấm** và không chừa chỗ cho chấm: viền trái `DOT_SPACE` chỉ áp khi `type != NEUTRAL`; trong `setType` phải đặt lại viền. Các kiểu còn lại giữ chấm.
- `getPreferredSize`: giữ công thức.

**`InfoCallout.java`** — sửa S5
- **Xóa hẳn** phần vẽ vạch 3px trong `paintComponent` (xóa cả override nếu không còn gì) và trường `accent`.
- Thêm viền 1px bo tròn cùng tông: `setBorder(new FlatLineBorder(new Insets(1,1,1,1), <màu viền>, 1f, 12))` với màu viền theo loại: `INFO` → `PRIMARY_BORDER_SOFT`, `WARNING` → `WARNING_BORDER`, `DANGER` → `DANGER_BORDER`. Thêm tham số màu viền vào enum `Kind`.
- Style `arc: 16` → `arc: 12`. Lề `insets 12 14 12 17` → `insets 12 14 12 14`.
- Không đổi: icon, nội dung, cách bọc HTML của `setText`.

**`ModalDialog.java`** — sửa S6, S11
- `new Color(0xC9CED8)` → `DtaqColors.OVERLAY_OPAQUE`.
- Thêm bóng cho khung hộp thoại. Trong `overlayPanel.paintComponent`, sau khi tô nền phủ và **trước** `super.paintComponent(g)`:

```java
if (contentPanel != null && contentPanel.getWidth() > 0) {
  Graphics2D g2 = (Graphics2D) g.create();
  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
  java.awt.Rectangle r = contentPanel.getBounds();
  int arc = DtaqSpacing.RADIUS_DIALOG * 2;
  int layers = 14;
  for (int i = layers; i >= 1; i--) {            // lớp ngoài cùng nhạt nhất
    g2.setColor(DtaqColors.alpha(DtaqColors.TOAST_BG, 0.012f * (layers - i + 1)));
    g2.fill(new java.awt.geom.RoundRectangle2D.Float(
        r.x - i, r.y - i + 10, r.width + 2 * i, r.height + 2 * i, arc + 2 * i, arc + 2 * i));
  }
  g2.dispose();
}
```

  `contentPanel` là trường `final` gán **sau** khi tạo `overlayPanel`, nên lớp vô danh không tham chiếu trực tiếp được lúc dựng. Cách xử lý: trong `paintComponent` lấy `getComponentCount() > 0 ? getComponent(0) : null` thay cho `contentPanel`.
- Bán kính tự đổi theo `RADIUS_DIALOG`.
- Không đổi: cấu trúc header/body/footer, phím Esc, `fitBody`, hành vi focus.

**`Toast.java`** — S11
- Ba `new Color(...)` → `TOAST_SUCCESS`, `TOAST_ERROR`, `TOAST_BG`. `arc: 20` → `arc: 16`.
- Không đổi vị trí, thời lượng, hoạt ảnh.

**`DtaqButton.java`** — S11
- Dòng 218 `new Color(keyColor.getRed(), …, 110)` → `DtaqColors.alpha(keyColor, 0.43f)`.
- `SEGMENTED_ACTIVE`: `arc: 6` → `arc: 12`.
- Trong các factory có icon cho nút phụ, icon giữ `TEXT_MUTED`. Sau khi sửa B1, icon nút "Xuất Excel" sẽ tự hiện lại — **không cần sửa gì thêm ở trang**.

**`DatePicker.java`** — S11
- Dòng 292: thay `#2457F5` bằng `DtaqStyles.hex(DtaqColors.PRIMARY)` và `#FFFFFF` bằng `DtaqStyles.hex(DtaqColors.SURFACE)`.
- `DatePickerTest` phải vẫn xanh.

**`DataTable.java` + `table/TableRenderers.java`** — B2
- `NumberRenderer`: thêm override `getTableCellRendererComponent` gọi `super` rồi `setFont(DtaqFonts.NUMERIC_13)`.
- `MoneyRenderer`, `CodeRenderer`: không sửa mã; chúng tự đổi vì `MONEY_TABLE` và `CODE` đã trỏ sang font số.
- `TwoLineRenderer`: không đổi.
- `ActionCell`: bỏ `+ "; arc: 12"` ở cuối chuỗi style (đã có trong `SMALL_BUTTON_EXTRA`, giờ là 8).
- Không đổi: chiều cao hàng, padding ô, cơ chế hover, sắp xếp, tooltip.

**`BarChartPanel.java`** — S13
- Trong vòng vẽ cột: **bỏ qua** việc vẽ hình cột khi giá trị của điểm bằng 0 (vẫn vẽ nhãn trục X). Đọc mã quanh dòng 170–195 để đặt điều kiện đúng chỗ; không đổi cách tính tỉ lệ.
- Nhãn giá trị trên đầu cột: `DtaqFonts.CAPTION` → `DtaqFonts.NUMERIC_12`. Nhãn trục Y: `DtaqFonts.HINT` giữ nguyên.
- Đầu cột bo 4px, chân cột vuông (nếu hiện đang bo cả bốn góc thì dùng clip để chân phẳng).

**`DonutChartPanel.java`**
- Nhãn số tiền và % trong chú giải: dùng `DtaqFonts.MONEY_TABLE` cho số tiền, `DtaqFonts.NUMERIC_13` cho %. Tên giữ `BODY_13`.
- Không đổi hình học, không đổi chữ ở tâm.

**`EmptyState.java`**
- Ô icon: nền `DtaqColors.DISABLED_BG`, icon `DtaqColors.TEXT_MUTED`, hình **vuông bo 10** (`DtaqSpacing.RADIUS_L`) thay vì hình tròn. Kích thước 46 giữ nguyên.
- Không đổi chữ, không đổi khoảng cách.

**`ActivityItem.java`, `Tile.java`, `Card.java`, `PageHeader.java`, `Pagination.java`, `FormField.java`, `SegmentedControl.java`, `StatefulView.java`, `ShortcutBar.java`, `ConfirmDialog.java`, `ImportPreviewDialog.java`, `SanPhamPicker.java`, `KyBaoCaoFilter.java`:** không sửa mã. Chúng đổi diện mạo qua token.

### 7.2 Màn xác thực

**`AuthBrandPanel.java`** — sửa S4. Thay toàn bộ thân `render(int w, int h)`; bỏ hằng `GRID`.

```java
private static BufferedImage render(int w, int h) {
  BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  Graphics2D g2 = img.createGraphics();
  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
  g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
  // "Quỹ đạo DTAQ": hai cung elip nghiêng lấy từ nét vòng cung của logo, đi sau logo.
  double cx = w / 2.0;
  double cy = h * 0.52;
  g2.rotate(Math.toRadians(-12), cx, cy);
  g2.setColor(DtaqColors.alpha(DtaqColors.BRAND, 0.55f));
  g2.setStroke(new java.awt.BasicStroke(2f));
  g2.draw(new java.awt.geom.Ellipse2D.Double(cx - w * 0.78, cy - h * 0.20, w * 1.56, h * 0.40));
  g2.setColor(DtaqColors.alpha(DtaqColors.BRAND_400, 0.22f));
  g2.setStroke(new java.awt.BasicStroke(1.25f));
  g2.draw(new java.awt.geom.Ellipse2D.Double(cx - w * 0.98, cy - h * 0.31, w * 1.96, h * 0.62));
  g2.dispose();
  return img;
}
```

Nền panel vẫn là `DtaqColors.BRAND_DARK` (giờ là navy logo). Không gradient, không lưới chấm, không đường mạch. Không đổi: logo 88px, dòng chữ "Quản lý siêu thị điện máy", cơ chế cache, `getMinimumSize`.

**`LoginFrame.java`, `ForgotPasswordFrame.java`**
- Nhãn `eyebrow` ("CHÀO MỪNG TRỞ LẠI" và tương đương): **giữ chữ** (luật L1). Chỉ đổi màu chữ sang `DtaqColors.TEXT_MUTED` nếu đang là `PRIMARY`.
- Không đổi gì khác. `UiLayoutSmokeTest.authenticationFitsNarrowWindows…` phải vẫn xanh.

### 7.3 Shell (`ui/shell/`)

**`Sidebar.java`** — sửa S2, S3, S11
- `buildBrandBlock()`: `panel.setOpaque(true); panel.setBackground(DtaqColors.SURFACE);` và viền dưới đổi sang `DtaqColors.BORDER`. Kết quả: ô logo trắng nối liền với topbar trắng thành một dải trên cùng; logo (vốn có nền trắng) nằm tự nhiên, hết hiệu ứng "miếng dán". **Vị trí và kích thước ô logo không đổi.**
- Style thanh cuộn: thay chuỗi `#323B4B`/`#465062` bằng `DtaqStyles.hex(DtaqColors.SIDEBAR_SCROLL_THUMB)` / `…_HOVER`.
- Không đổi: `GROUP_ORDER`, `GROUP_OF`, tên nhóm, thứ tự mục, logic thu gọn, bề rộng.

**`NavigationModel.java`** — sửa S8. Chỉ đổi **tham số `iconName`**; `id`, `label`, `pageId` và thứ tự **giữ nguyên tuyệt đối** (`NavigationModelTest` khẳng định nhãn và thứ tự).

| `id` | Icon cũ | **Icon mới** | Vai trò |
|---|---|---|---|
| `home` | `layout-dashboard` | giữ | cả hai |
| `revenue` | `chart-no-axes-combined` | giữ | QL |
| `long_stock` | `package-search` | `hourglass` | QL |
| `product_lines` | `shopping-cart` | `layers` | cả hai |
| `defective_products` | `alert-circle` | `package-x` | cả hai |
| `promotions` | `zap` | `badge-percent` (đã có sẵn) | cả hai |
| `employees` | `users` | giữ | QL |
| `sales_by_product` | `chart-no-axes-combined` | `chart-column` | QL |
| `sales_by_employee` | `users` | `id-card` | QL |
| `return_approvals` | `circle-check` | `clipboard-check` | QL |
| `price_tax` | `receipt-text` | `banknote` | QL |
| `backup` | `download` | `database-backup` | QL |
| `restore` | `clock` | `history` | QL |
| `customers` | `users` | giữ | BH |
| `invoice` | `receipt-text` | giữ | BH |
| `product_lookup` | `search` | `package-search` | BH |
| `customer_lookup` | `user-round` | `user-round-search` | BH |
| `returns` | `receipt-text` | `arrow-left-right` | BH |
| `products` | `package` | giữ | BH |
| `categories` | `info` | `tags` | BH |

`PlaceholderPage` nhận `entry.iconName()` nên tự đổi theo.

**`NavItem.java`**: không sửa mã. (Thanh chỉ báo 3px bên trái của mục đang chọn là chỉ báo điều hướng chuẩn, được giữ.) `NavItemWrapTest` phải vẫn xanh.

**`Topbar.java`** — sửa B3
- Dòng 203: thay cả chuỗi style của `popup` bằng `"borderColor: " + DtaqStyles.hex(DtaqColors.BORDER)`. Bo góc popup đã do `PopupMenu.borderCornerRadius = 10` trong `.properties` lo.
- Không đổi: breadcrumb, ngày, avatar, menu, ngưỡng ẩn hiện theo bề rộng.

**`AppShellFrame.java`, `ChangePasswordDialog.java`, `PlaceholderPage.java`, `Page.java`, `PageId.java`:** không sửa.

---

## 8. Quét các trang (giai đoạn G4)

### 8.1 Xóa mã màu viết cứng còn lại

Sau G2–G3, lệnh dưới đây phải **không còn kết quả** ngoài thư mục `theme/`:

```bash
cd src/main/java/com/dtaq/retail/ui
grep -rn 'new Color(' --include=*.java . | grep -v '^./theme/'
grep -rnE '"[^"]*#[0-9A-Fa-f]{6}' --include=*.java . | grep -v '^./theme/'
```

Ngoại lệ được phép: `new Color(0, 0, 0, 0)` (trong suốt hoàn toàn) trong `ModalDialog`.

### 8.2 Ký tự/emoji thay icon (S9) — xử lý ở tầng hiển thị, **không đổi chuỗi dữ liệu**

| Vị trí | Hiện tại | Cách làm |
|---|---|---|
| `EmployeePage.java:224` | ô bảng chứa `"● Hoạt động"` / `"○ Đã khóa"` | **Giữ chuỗi trong model.** Gắn renderer cho cột đó: bỏ 2 ký tự đầu khi vẽ, hiển thị bằng `StatusPill` (`success` cho "Hoạt động", `neutral` cho "Đã khóa") căn trái trong ô, nền ô theo `TableRenderers.rowBackground` (cần nâng phương thức đó lên `public`). Trước khi làm, đọc mã để chắc không có chỗ nào so sánh chuỗi ô này. |
| `SalesByProductPage.java:289–297` | hàm trả `"🟢 Cần nhập thêm tháng này"`, … | **Không sửa hàm trả chuỗi** — `SalesByProductPageTest` khẳng định đúng 4 chuỗi này. Sửa trong `RecommendationBadgeRenderer` (dòng ~110): khi vẽ, tách emoji đầu chuỗi và vẽ `StatusPill` với phần chữ còn lại: 🟢 → `success`, 🔵 → `info`, 🟡 → `warning`, 🔴 → `danger`. |
| `ShortcutBar.java:34` (`"↑ ↓"`), `RetailInvoicePage.java:1145` (`"↵"`), `RetailInvoicePage.java:436` (`"−"`), `SalesByEmployeePage.java:385` | ký tự mũi tên/toán học | **Giữ nguyên.** Đây là ký hiệu phím và dấu trừ, Inter có đủ glyph, và là nội dung chữ. |

### 8.3 Sửa lệch hàng nút cạnh ô nhập (B4)

Đây là **chỗ duy nhất** được chạm vào ràng buộc MigLayout của trang. Chỉ thêm khoảng cách đáy cho **nút**, không đổi cột/hàng.

Tìm:

```bash
grep -rn '"\[bottom\]"' src/main/java/com/dtaq/retail/ui --include=*.java
```

Đã biết: `CategoriesPage:103`, `KhachHangPicker:79`, `DefectiveProductsPage:107`, `RetailInvoicePage:357` (và ràng buộc ở dòng 374), `ProductPage:126`, `ProductLinesPage:134`, `PriceTaxPage:79`. Xem thêm `ReturnsPage` (hàng "Mã hóa đơn" + "Tra cứu") và `CustomerListPage`.

Với mỗi hàng, áp quy tắc:

- Nếu thành phần bên trái là `FormField` (có dòng thông báo cao 16px bên dưới) và bên phải là nút/điều khiển thường → thêm vào ràng buộc của **nút**: `gapbottom ` + `DtaqSpacing.FIELD_MESSAGE_H`.
- Nếu bên trái **không** phải `FormField` (ô nhập trần) → không sửa.
- `RetailInvoicePage:374` có hai nhánh (hẹp/rộng): chỉ thêm vào nhánh rộng (`"cell 1 0, aligny bottom"`).

Kiểm bằng mắt: mép trên và mép dưới của nút trùng với ô nhập.

### 8.4 `JOptionPane` (18 chỗ)

**Không thay** bằng `ModalDialog` (sẽ đổi hành vi). Chúng nhận màu mới qua `.properties`. Chỉ ghi lại danh sách vị trí vào báo cáo cuối để nhóm quyết định sau.

### 8.5 Font viết cứng ngoài theme

```bash
grep -rnE 'new Font\(|deriveFont' src/main/java/com/dtaq/retail/ui --include=*.java | grep -v '/theme/'
```

Có 4 chỗ. Với mỗi chỗ: nếu có hằng `DtaqFonts` tương đương thì thay; nếu là cỡ đặc biệt cho số tiền thì dùng `DtaqFonts.numeric(...)`. Không đổi cỡ chữ đang hiển thị quá ±1px.

### 8.6 Số liệu ngoài bảng

Tìm các `JLabel` hiển thị tiền/số lượng đang dùng `BODY_13`/`BODY_14`/`BUTTON` (ví dụ khối "Thanh toán" của `RetailInvoicePage`, `InvoiceReceiptDialog`, `ReturnReceiptDialog`, `InvoiceTotals`). Đổi **chỉ font** sang hằng số tương ứng cùng cỡ (`NUMERIC_13`, `AMOUNT_ROW`, `MONEY_TABLE`). Nhãn chữ ("Tổng trước thuế:") giữ Inter.

---

## 9. Lộ trình và cổng kiểm tra

| GĐ | Việc | File chính | Cổng qua |
|---|---|---|---|
| **G0** | Chuẩn bị: `git status`; `mvn -q -DskipTests package`; chụp ảnh **trước** bằng công cụ Phụ lục B vào thư mục tạm; xác nhận assets mục 5 có mặt (63 svg, 2 otf). | — | Build xanh; có 25 ảnh "trước". |
| **G1** | Nền tảng theme: mục 6.1 → 6.8. | `theme/*`, `DtaqLightLaf.properties`, 4 test theme + `DtaqContrastTest` | `mvn -q test -Dtest='Dtaq*Test'` xanh. Chụp lại: icon sidebar đồng màu; nút chính màu `#006DD7`; không còn màu chàm. |
| **G2** | Component: mục 7.1. | `component/*` | `mvn -q test -Dtest='com.dtaq.retail.ui.component.*Test'` xanh. Log không có `UnknownStyleException`. |
| **G3** | Shell + xác thực: mục 7.2, 7.3. | `shell/*`, `AuthBrandPanel`, `LoginFrame`, `ForgotPasswordFrame` | `NavigationModelTest`, `NavItemWrapTest`, `UiLayoutSmokeTest` xanh. Logo nằm trên nền trắng. |
| **G4** | Quét trang: mục 8. | `page/**` | Hai lệnh `grep` ở 8.1 rỗng (trừ ngoại lệ). Toàn bộ test `ui.page.*` xanh. |
| **G5** | Nghiệm thu: mục 10 + 11. | — | Tất cả ô trong danh sách mục 11 được tích. |

Lệnh dùng chung:

```bash
mvn -q -DskipTests package          # build
mvn test                            # toàn bộ test (một số test cần PostgreSQL đang chạy; thiếu thì tự bỏ qua)
mvn -q test -Dtest='Dtaq*Test'      # riêng test theme
```

> Người lập **chưa chạy** `mvn test` trong phiên khảo sát. Trước G1 hãy chạy một lần để biết test nào vốn đã đỏ/bị bỏ qua, tránh nhầm với lỗi do mình gây ra.

---

## 10. Quy tắc bắt buộc khi viết mã

| # | Quy tắc |
|---|---|
| Q1 | Màu chỉ lấy từ `DtaqColors`; font chỉ từ `DtaqFonts`; kích thước từ `DtaqSpacing`. Thiếu thì **thêm hằng vào theme**, không viết số tại chỗ. |
| Q2 | Tên hằng cũ **không đổi, không xóa** (hơn 40 file đang tham chiếu). Chỉ đổi giá trị và thêm hằng mới. |
| Q3 | Mọi khóa trong chuỗi `FlatClientProperties.STYLE` phải tồn tại ở FlatLaf 3.6 cho đúng loại component. Khóa sai không làm sập ứng dụng nhưng in `UnknownStyleException` ra log. Sau mỗi giai đoạn, chạy công cụ Phụ lục B và `grep UnknownStyleException` trên đầu ra: **phải bằng 0**. Không chắc một khóa → không dùng. |
| Q4 | Icon chỉ nạp qua `DtaqIcons.icon(...)`. Không tạo `FlatSVGIcon` trực tiếp trong trang. Không dùng emoji hay ký tự Unicode làm icon mới. |
| Q5 | Không thêm `JLabel`, nút, thẻ, cột, hàng. Không đổi chuỗi hiển thị. |
| Q6 | Số tiền vẫn định dạng qua `MoneyFormat`/`TienUtil` như cũ. Chỉ đổi font vẽ. |
| Q7 | Không tắt vòng focus. Mọi control mới chạm tới phải dùng được bằng Tab/Enter/Space như trước. |
| Q8 | Ý nghĩa không bao giờ chỉ truyền bằng màu: trạng thái luôn có chữ (và chấm/icon). |
| Q9 | Chú thích mã bằng tiếng Việt, giải thích **vì sao** khi không hiển nhiên. |
| Q10 | Mỗi giai đoạn: `git diff --stat` chỉ được chứa file trong phạm vi L3. Thấy file ngoài phạm vi → hoàn lại phần đó. |

---

## 11. Danh sách nghiệm thu

### 11.1 Tự động

- [ ] `mvn clean package` thành công.
- [ ] `mvn test`: không có test nào **mới** đỏ so với lần chạy ở G0.
- [ ] `DtaqContrastTest` xanh.
- [ ] Hai lệnh `grep` mục 8.1 không còn kết quả ngoài ngoại lệ.
- [ ] Đầu ra công cụ Phụ lục B không chứa `UnknownStyleException`.
- [ ] `git diff --stat` không có file ngoài `ui/**`, `resources/.../ui/theme/**`, `src/test/.../ui/**`.

### 11.2 Bằng mắt — so ảnh trước/sau từng màn (25 ảnh)

- [ ] **Nội dung:** mọi chuỗi chữ giống hệt ảnh trước.
- [ ] **Bố cục:** vị trí và kích thước các khối trùng ảnh trước (sai lệch cho phép: ≤2px do đổi font số; nút cạnh ô nhập dịch lên ~20px theo mục 8.3; thẻ KPI không chú thích cao bằng thẻ bên cạnh).
- [ ] **Màu:** không còn `#2457F5` hay xám đen `#111826`. Sidebar là navy `#001D44`. Nút chính `#006DD7`.
- [ ] **Icon:** mỗi mục menu một icon riêng; icon cùng nhóm cùng màu; mục đang chọn icon màu `#62A6FE`; icon nút "Xuất Excel" hiện rõ; mũi tên breadcrumb màu xám ở cả hai vai trò.
- [ ] **Logo:** nằm trên dải trắng liền với topbar, không còn viền "miếng dán" trên nền tối. Khi thu gọn sidebar, biểu tượng vuông hiển thị gọn trong ô trắng.
- [ ] **Số:** trong mọi bảng có cột tiền, các chữ số cùng hàng đơn vị thẳng cột dọc.
- [ ] **Đăng nhập:** nền navy, hai cung quỹ đạo đi sau logo; không còn chấm lưới, đường mạch, gradient.
- [ ] **Callout:** không còn vạch dày bên trái; có viền mảnh quanh.
- [ ] **Hộp thoại:** có bóng mềm lệch xuống dưới; bo 14.
- [ ] **Pill trung tính** (ngày tháng, "11 bản sao lưu"): không có chấm. Pill trạng thái: có chấm.
- [ ] **Biểu đồ cột:** ngày doanh thu 0 không có vạch ở chân.
- [ ] **Trạng thái:** rê chuột lên hàng bảng thấy nền xanh rất nhạt; Tab qua nút/ô nhập thấy vòng focus xanh 2px; nút vô hiệu vẫn đọc được chữ.

### 11.3 Môi trường

- [ ] Chạy thật `java -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar` ở cửa sổ 1440×900 và 1024×600.
- [ ] Windows tỉ lệ 100%, 125%, 150%: không có chữ bị cắt đuôi, icon không vỡ.
- [ ] Luồng bàn phím màn Lập hóa đơn (F2 → F3 → Enter → F8) hoạt động như cũ.
- [ ] Đăng nhập cả hai vai trò, mở lần lượt mọi mục menu.

---

## 12. Việc **không** làm trong kế hoạch này (cần nhóm duyệt riêng)

| Việc | Lý do để ngoài |
|---|---|
| Thay 18 `JOptionPane` bằng `ModalDialog` | Đổi hành vi hộp thoại. |
| Căn lại lưới hai thẻ đáy ở Trang chủ Quản lý (cột chia 584/616 không khớp hàng trên) | Là đổi bố cục. |
| Giảm khoảng trống dọc giữa các hàng `FormField` | Là đổi bố cục. |
| Bỏ ngày trùng lặp (topbar và pill ở tiêu đề trang cùng hiện ngày) | Là đổi nội dung. |
| Bỏ nhãn eyebrow "CHÀO MỪNG TRỞ LẠI" | Là đổi nội dung. |
| Đổi chữ ở tâm donut "32M" sang cách viết Việt ("32 tr") | Là đổi nội dung. |
| Giao diện tối | Ngoài phạm vi; bối cảnh dùng là quầy sáng. |
| Vẽ lại logo dạng SVG nền trong suốt | Cần file gốc từ người thiết kế logo. |

---

## 13. Rủi ro

| Rủi ro | Dấu hiệu | Xử lý |
|---|---|---|
| Test theme đỏ hàng loạt sau khi đổi màu | `DtaqColorsTest`, `DtaqStylesTest` fail | Đúng kỳ vọng — cập nhật theo mục 6.8 **trong cùng commit G1**. |
| Font số không nạp được trên máy nào đó | Log `Không nạp được font …` | Mã đã tự rơi về Inter; giao diện vẫn chạy, chỉ mất căn cột. Kiểm file có trong jar: `unzip -l target/*.jar \| grep DtaqNumerals`. |
| Đổi cỡ `CARD_TITLE` 15→16, `KPI_VALUE` 26→24 làm một nhãn dài bị cắt | Chữ có dấu "…" hoặc mất đuôi trong ảnh sau | Trả riêng hằng đó về cỡ cũ; ghi vào báo cáo. Không sửa bố cục để chữa. |
| Viền ô nhập đậm hơn làm bảng/trang "nặng" | Cảm quan khi xem ảnh sau | Lùi `BORDER_STRONG` về `#B9C1CC`; cập nhật test màu. |
| Ô logo trắng trên sidebar navy bị nhóm chê | Phản hồi | Phương án lùi: trả `buildBrandBlock` về trong suốt (giữ navy). Không phương án nào đổi bố cục. |
| Renderer mới ở `EmployeePage`/`SalesByProductPage` làm hỏng sắp xếp hoặc so sánh chuỗi | Test trang đỏ, sắp xếp sai | Model vẫn giữ chuỗi gốc nên sắp xếp không đổi; nếu vẫn lỗi thì bỏ renderer, giữ nguyên hiện trạng ô đó. |
| Nhánh `feature/anh-w6` đang có thay đổi chưa commit của người khác (`SanPhamControllerTest.java`, tài liệu W6) | `git status` | Không đụng các file đó. Tốt nhất tạo nhánh riêng cho việc UI từ commit sạch. |

---

## Phụ lục A — Nguồn và phương pháp

- **Skill Impeccable (v4.8.0):** phân loại bề mặt "Operate"; nguyên tắc tinh chỉnh giữ nguyên bản sắc/nội dung; danh sách thói quen cần tránh (vạch màu cạnh thẻ, gradient chữ, kính mờ trang trí, emoji thay icon, cùng một kiểu thẻ lặp lại, bóng lệch 0); yêu cầu tương phản ≥4,5:1 và tô màu cả "bề mặt mặc định" (vùng chọn chữ, con trỏ nhập, thanh cuộn, vòng focus).
- **Skill UI/UX Pro Max:** tra cứu cho loại sản phẩm "Inventory & Stock Management" → phong cách *Flat + Minimalism/Swiss*, kiểu bảng điều khiển *Data-Dense* (hàng bảng 36–40px, header 56px, sidebar ~240px — trùng với giá trị hiện có nên giữ); bảng màu gợi ý "xanh dữ liệu + điểm nhấn hổ phách" (khớp với `ACCENT` hiện có); cặp font "Minimal Swiss — Inter" cho dashboard doanh nghiệp. Bảng màu xanh lá mặc định của công cụ **bị loại** vì nhóm đã chốt màu chủ đạo theo logo.
- **Phương pháp biểu đồ:** màu phân loại gán cố định theo thứ tự, kiểm bằng trình kiểm bảng màu (mù màu + tương phản).
- **Màu:** lấy mẫu điểm ảnh `dtaq-logo.png`, dựng thang OKLCH, tính tương phản WCAG bằng chương trình.
- **Hiện trạng:** dựng 25 màn hình ngoài màn hình với dữ liệu thật của CSDL cục bộ và đọc mã nguồn theme, shell, component.

## Phụ lục B — Công cụ chụp màn hình ngoài màn hình (không commit)

Dựng mọi trang của cả hai vai trò mà **không mở cửa sổ**, xuất PNG 1440×900. Cần PostgreSQL chạy và `db.properties` hợp lệ (chỉ đọc dữ liệu).

Đặt file ở **ngoài repo**, ví dụ `/tmp/dtaq-shots/com/dtaq/retail/ui/Shots.java` (phải cùng tên gói để gọi được `AppNavigator.managerPages/salesPages`):

```java
package com.dtaq.retail.ui;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.dao.impl.PasswordResetDaoImpl;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.shell.AppShellFrame;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.theme.DtaqTheme;
import com.dtaq.retail.util.SmtpMailer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Shots {
  static final int W = 1440, H = 900;

  static void layoutAll(java.awt.Component c) {
    c.doLayout();
    if (c instanceof java.awt.Container k) {
      k.validate();
      for (java.awt.Component ch : k.getComponents()) layoutAll(ch);
    }
  }

  static void shot(JFrame f, String name) throws Exception {
    SwingUtilities.invokeAndWait(() -> { f.getRootPane().setSize(W, H); layoutAll(f.getRootPane()); });
    Thread.sleep(1800);                                   // chờ SwingWorker nạp dữ liệu
    SwingUtilities.invokeAndWait(() -> {
      layoutAll(f.getRootPane());
      layoutAll(f.getRootPane());
      BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      f.getRootPane().printAll(g);
      g.dispose();
      try { ImageIO.write(img, "png", new File(System.getProperty("out"), name + ".png")); }
      catch (Exception e) { throw new RuntimeException(e); }
    });
    System.out.println("shot " + name);
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] a) throws Exception {
    DtaqTheme.install();
    DataSource ds = ConnectDB.getDataSource();
    NhanVienDaoImpl dao = new NhanVienDaoImpl(ds);
    List<NhanVien> all = dao.findAll();
    JFrame[] lf = new JFrame[1];
    SwingUtilities.invokeAndWait(() -> {
      lf[0] = new LoginFrame(new AuthController(dao),
          new AppNavigator(dao, new PasswordResetDaoImpl(ds), new SmtpMailer(), ds));
      lf[0].addNotify();                                  // có peer nhưng không setVisible
    });
    shot(lf[0], "00-login");
    for (LoaiQuyen q : LoaiQuyen.values()) {
      NhanVien nv = all.stream().filter(n -> n.loaiQuyen() == q).findFirst().orElse(null);
      if (nv == null) continue;
      boolean ql = q == LoaiQuyen.NHAN_VIEN_QUAN_LY;
      AppShellFrame[] sh = new AppShellFrame[1];
      List<Page>[] pg = new List[1];
      SwingUtilities.invokeAndWait(() -> {
        CurrentSession.set(nv);
        sh[0] = new AppShellFrame(nv, () -> { });
        pg[0] = ql ? AppNavigator.managerPages(nv, ds, sh[0]::showPage, null)
                   : AppNavigator.salesPages(nv, ds, sh[0]::showPage);
        pg[0].forEach(sh[0]::registerPage);
        sh[0].addNotify();
      });
      int i = 0;
      for (Page p : pg[0]) {
        SwingUtilities.invokeAndWait(() -> sh[0].showPage(p.id()));
        shot(sh[0], (ql ? "m" : "s") + String.format("%02d-", i++) + p.id().name().toLowerCase());
      }
    }
    System.exit(0);
  }
}
```

Chạy từ thư mục gốc repo (sau `mvn -q -DskipTests package`):

```bash
CP="target/classes:target/dtaq-retail-1.0.0-SNAPSHOT.jar"       # Windows: đổi ':' thành ';'
javac -nowarn -d /tmp/dtaq-shots/out -cp "$CP" /tmp/dtaq-shots/com/dtaq/retail/ui/Shots.java
mkdir -p /tmp/dtaq-shots/before
java -Dsun.java2d.uiScale=1 -Dout=/tmp/dtaq-shots/before -cp "/tmp/dtaq-shots/out:$CP" \
     com.dtaq.retail.ui.Shots 2>&1 | tee /tmp/dtaq-shots/log.txt
grep -c UnknownStyleException /tmp/dtaq-shots/log.txt           # sau G3 phải là 0
```

Lưu ý:

- **Luôn chụp ở `uiScale=1`** và không nhân tỉ lệ khi vẽ. Chụp ở 1,5× cho ảnh có chữ bị cắt đuôi giả (đã gặp).
- Nếu máy có biến `JAVA_TOOL_OPTIONS=-Dsun.java2d.uiScale=2`, chạy với `JAVA_TOOL_OPTIONS=` ở đầu lệnh.
- Trang có `onShow` gọi CSDL sẽ hiện dữ liệu thật; ảnh trước/sau nên chụp trên cùng dữ liệu.
- Công cụ sẵn có trong repo: `UiLayoutSmokeTest.main` (xuất ảnh vào `target/ui-review` qua thuộc tính `dtaq.uiSnapshots`), nhưng nó **mở cửa sổ thật** trên màn hình.

## Phụ lục C — Bảng tra nhanh màu (dán vào công cụ chọn màu)

```
Hành động   #006DD7  hover #0062C2  pressed #0058B0
Nền nhạt    #F2F7FE  #E4F0FF  #C9E0FE  viền #9DC7FF  focus #62A6FE
Logo        azure #0384FF  navy #001D44
Chữ         #111B28  phụ #5A6472  mờ #6B7583
Nền         app #F3F6FA  thẻ #FFFFFF  chìm #F8FAFD  vô hiệu #EAEEF4
Viền        #DAE0E8  ô nhập #AFB8C4  hover #77818F
Sidebar     nền #001D44  hover #002957  chọn #003671  chữ #C4D9F6  mờ #88A7D0  vạch #112E52
Trạng thái  đạt #0F7B55/#E4F5EC  cảnh báo #B4540A/#FFF4DB  lỗi #C2342C/#FDECEB (đậm #9F2620)
Nhấn        hổ phách #F5A524/#FFF7E6 (chữ #7A4A00)
Biểu đồ     #1886FE  #EB6834  #1BAF7A  #EDA100  #6250D6  khác #77818F
```
