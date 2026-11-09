# Báo cáo thiết kế lại giao diện "Volt Console" (light mode)

> **Nhánh:** `refactor/enhance-ui` (nền là `develop` đã gộp PR #35 bugsmell) · **Ngày:** 2026-11-05
> **Căn cứ:** `UI_UX_Plan/DTAQ_UI_UX_Redesign_Plan.md` · **Phạm vi:** chỉ giao diện, không đổi nghiệp vụ, chữ hiển thị, phím tắt hay API.
> **Chưa commit** — toàn bộ thay đổi đang ở working tree để bạn xem trước.

## 1. Kết quả kiểm tra
| Hạng mục | Kết quả |
|---|---|
| `mvn test` trước khi sửa (baseline) | 603 test, 0 lỗi |
| `mvn test` sau khi sửa | **605 test, 0 lỗi** (thêm 2 test `DtaqSpacingTest`) |
| Test chức năng bị sửa | Không. Chỉ cập nhật giá trị kỳ vọng của `DtaqColorsTest`, `DtaqStylesTest` (test ghim mã màu cũ) |
| Chụp ảnh thử (headless) | Sidebar, rail, KPI, bảng, pill, callout, nút, ô tìm kiếm: hiển thị đúng |
| **Chưa kiểm** | Chưa mở app thật trên Windows, chưa thử các mức 125/150/200%, chưa thử cửa sổ 1024×600 bằng mắt. Cần bạn chạy thử |

## 1b. Kiểm tra lần 2 (`mvn clean package` + quét giao diện thật)
- `mvn clean package`: **BUILD SUCCESS, 610 test, 0 lỗi** (jar `target/dtaq-retail-1.0.0-SNAPSHOT.jar`).
- Quét runtime: dựng **toàn bộ 23 trang thật** (13 Quản lý + 10 Bán hàng, dữ liệu PostgreSQL thật) trong khung chính ở 4 kích thước 1024×600, 1280×680, 1366×728, 1920×1040; bắt mọi ngoại lệ trên luồng giao diện, tràn ngang và chữ/nút bị cắt. Kết quả cuối: **0 lỗi**.

| Lỗi phát hiện | Nguyên nhân | Đã sửa |
|---|---|---|
| App không mở được màn đăng nhập (`Illegal constraint: 'wmin 0'`) | Cú pháp cột MigLayout sai trong `LoginFrame`, `ForgotPasswordFrame` | Trả về `[40%,fill][60%,fill]` |
| Nút bị ép thấp 27px (cần 36px), chữ bị cắt ở cửa sổ 600px: Sản phẩm lỗi, Dòng sản phẩm, Sao lưu ("Lưu cấu hình" bị cắt ở mọi kích thước) | Bố cục được phép co nút xuống dưới chiều cao chuẩn | `DtaqButton.getMinimumSize()` giữ chiều cao chuẩn (sửa một chỗ cho mọi nút) |
| Bộ lọc "Tất cả / Đang hoạt động / Đã khóa" (Nhân viên) bị cắt 25px/28px | Như trên, ở nút của `SegmentedControl` | Thêm `getMinimumSize()` tương tự |
| Khối logo sidebar bị ép thấp, lệch với topbar | Hàng đầu sidebar bị co | Cố định cao 56px, căn giữa dọc |
| Nhãn menu dài bị cắt cụt (không có "…") | Danh sách menu rộng hơn khung cuộn | Danh sách bám bề rộng khung + `NavItem` cho co; nhãn hiện "…", tooltip đủ chữ |
| Biểu đồ doanh thu theo ngày: nhãn trục X đè nhau, chuỗi "0 tr" lặp dày | Mỗi cột đều vẽ nhãn | Nhãn trục X cách quãng theo bề rộng; bỏ nhãn giá trị của cột 0 (tooltip vẫn đủ) |

Bổ sung: khối "Tổng thanh toán" ở màn Lập hóa đơn đặt trong ô nền Volt nhạt cho nổi bật (không đổi trường/luồng; test của trang xanh).

Chưa xác định: trong ảnh chụp màn Thống kê doanh thu, icon của nút "Xuất Excel" không hiện dù nút đang bật; thử riêng nút này thì icon vẫn vẽ đúng, nên có thể chỉ là lỗi của cách chụp ảnh. Cần nhìn lại trên app thật.

## 2. Đã làm
**Token & LAF** (`ui/theme`): đổi toàn bộ giá trị màu/chữ/khoảng cách/bo góc sang hệ Volt (xanh `#2457F5`, nền `#F4F6FA`, ink `#151A24`), giữ nguyên tên hằng nên toàn app đổi diện mạo cùng lúc. Thêm token mới (sidebar tối, accent hổ phách, thang biểu đồ, `alpha()`, `arc()`, `normalizeRowHeight()`). Viết lại `DtaqLightLaf.properties` (focus ring 2px, tab gạch chân, thanh cuộn mảnh, nút hiện mật khẩu). Thêm 3 icon (`folder`, `badge-percent`, `panel-left`) vì code đã tham chiếu `folder`/`badge-percent` mà chưa có tệp. Logo dùng ảnh thương hiệu chính thức: `theme/dtaq-logo.png` (logo đầy đủ, 720×240) và `theme/dtaq-mark.png` (phần màn hình + giỏ hàng, 256×256, bo góc), nạp qua `DtaqTheme.logo()/logoMark()` kèm bản 2x cho màn HiDPI; thanh menu mở rộng hiện logo đầy đủ, thu gọn hiện biểu tượng vuông, màn đăng nhập hiện logo cao 88px, biểu tượng cửa sổ/taskbar lấy từ biểu tượng vuông.

**Component:**
- `StatusPill`: chấm tròn + màu theo nghĩa (thành công là xanh lá, tách khỏi màu chính).
- `DataTable`/`TableRenderers`: chuẩn hóa chiều cao hàng về 34/40/52, header nền chìm, mã dùng chữ `CODE` màu Volt, tiền semibold căn phải, nút hành động trong bảng dạng viền nhẹ (không còn nút đặc).
- `InfoCallout` (vạch nhấn 3px), `KpiCard` (tự hạ cỡ khi số dài, xu hướng tăng màu xanh lá), `ModalDialog` (rộng tự co theo cửa sổ, **dự phòng nền đục** khi máy không hỗ trợ cửa sổ trong suốt, tránh lỗi mở dialog trên RDP/VM).
- `Toast` nền tối; `BarChartPanel`, `DonutChartPanel` đổi bảng màu, bỏ kích thước cứng 800×250, thêm khe giữa lát.

**Shell:** Sidebar nền tối chia nhóm (TỔNG QUAN, BÁN HÀNG, BÁO CÁO…), thẻ phiên, cuộn được trên màn thấp; tự thu thành dải icon khi cửa sổ < 1280 và có nút thu gọn ở Topbar (bấm tay thì giữ lựa chọn). Topbar 56px có breadcrumb dạng chevron, ngày kèm icon, đường phân cách. Nội dung trang được căn giữa và giới hạn rộng tối đa 1680. Cửa sổ chính tối thiểu 1024×600. Có biểu tượng cửa sổ.

**Đăng nhập / Quên mật khẩu:** panel thương hiệu vẽ lưới chấm, đường "mạch điện", vầng sáng (cache ảnh, không vẽ lại mỗi lần); cửa sổ co theo vùng màn hình khả dụng (tối đa 92%); dưới 900px tự ẩn panel thương hiệu.

**Hoạt ảnh nhẹ (≤ 320ms, tự tắt khi FlatLaf tắt animation):** thanh chỉ báo mục menu trượt vào/ra, Toast trượt lên khi hiện và xuống khi ẩn, cột biểu đồ mọc dần từ đáy, vòng donut quét dần theo chiều kim đồng hồ.

**Khác:** hộp chứng từ (hóa đơn/phiếu) tự giảm chiều cao theo màn hình; bật `-Ddtaq.devtools=true` để dùng FlatInspector (`Ctrl+Shift+Alt+X`) và UIDefaults inspector (`Ctrl+Shift+Alt+Y`).

## 3. Chưa làm / khác với plan (nói rõ)
- **Trang không được thiết kế lại theo từng archetype** (bố cục 2 cột/KPI tự đổi theo lớp S/M/L/XL, form 2 cột, số bước 1–2 ở Quên mật khẩu, tile số ①②③ ở hóa đơn, khối "tổng tiền" viền Volt). Các trang hiện hưởng token mới (màu, chữ, bo góc, bảng, nút, pill) nhưng giữ nguyên bố cục MigLayout cũ. Nếu muốn đầy đủ như plan §9–§10, cần một đợt nữa làm từng trang.
- Chưa tạo `Responsive` helper và `UiGallery` (plan §6.3, §15.3) vì chưa có trang nào dùng đến; responsive hiện dựa vào shell (tự thu sidebar, giới hạn rộng), co giãn của MigLayout và bảng có thanh cuộn ngang.
- Chưa thêm bóng đổ mức 3 cho dialog, chưa làm `Card.setAccent`/`setHeaderDivider`, chưa làm chip phím tắt mới cho `DtaqButton` (giữ kiểu viền cũ).
- Chuyển trang chưa có hiệu ứng mờ dần (CardLayout không hỗ trợ nhẹ nhàng; làm sẽ rủi ro cho `onShow/onHide`).
- Không nạp skill `impeccable`/`ui-ux-pro-max`/`graphify`: plan đã chứa sẵn tiêu chí của chúng (tương phản, phân cấp chữ, trạng thái rỗng/lỗi, không cắt chữ), nên đối chiếu thủ công bằng ảnh chụp và bộ quét tự động để tiết kiệm token/thời gian; graphify không cần vì phạm vi chỉ là gói `ui`.

## 4. Việc bạn cần làm
1. Chạy app, xem thử cả hai vai trò (Quản lý, Bán hàng) ở 1280×720 và 1920×1080, thử phóng 125–150%.
2. Soát `git diff` rồi mới commit; còn thư mục `target/` và tệp scratch nằm ngoài repo.
3. Nếu hài lòng nền tảng này, giao đợt tiếp theo: bố cục từng trang theo plan §10 (ưu tiên màn Lập hóa đơn).

## 5. Tệp chính đã đổi
`ui/theme/*` (+ `DtaqLightLaf.properties`, 4 SVG), `ui/component/{StatusPill, DataTable, InfoCallout, KpiCard, Toast, ModalDialog, BarChartPanel, DonutChartPanel}`, `ui/component/table/TableRenderers`, `ui/shell/{AppShellFrame, Sidebar, NavItem, Topbar}`, `ui/{AuthBrandPanel, LoginFrame, ForgotPasswordFrame}`, hai hộp thoại chứng từ, `DtaqColorsTest`, `DtaqStylesTest`, `DtaqSpacingTest` (mới).
