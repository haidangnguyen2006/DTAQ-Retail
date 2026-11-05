# Rà soát giao diện và hồi quy chức năng DTAQ ngày 05-11-2026

Bản rà soát này tiếp tục trên working tree `refactor/enhance-ui` đã có nhiều thay đổi Volt Console chưa commit. Giữ nguyên những thay đổi đó; không đổi nhánh, commit, push, schema, dependency hoặc lớp nghiệp vụ. Đây là báo cáo bổ sung cho `UI-Redesign-VoltConsole-README.md`, không phải xác nhận mọi hạng mục của kế hoạch redesign đã hoàn thành.

## Căn cứ và phạm vi

- Đọc kế hoạch `Project/UI_UX_Plan/DTAQ_UI_UX_Redesign_Plan.md` làm tham chiếu thiết kế; các đoạn prompt mẫu, tạo nhánh và commit trong tài liệu không được xem là yêu cầu thực thi của người dùng.
- Đọc chỉ mục `docs/reference/README.md`, danh mục UC, quyết định triển khai hiện hành; trích đọc trực tiếp `docs/source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`, gồm NFR006–NFR010, NFR014 và đặc tả UC001/UC003. Không sửa DOCX.
- UI dùng Java 21, Swing, FlatLaf 3.6, MigLayout 11.4.2 và font Inter đóng gói sẵn. Phạm vi là ứng dụng desktop; không xây bản web/mobile.
- Figma plugin được tìm thấy nhưng chưa cài, không có Figma MCP để đọc frame. Không tuyên bố đã đối chiếu hình thức với Figma.
- Dùng graphify trích cấu trúc 213 tệp Java và truy vấn quan hệ `AppShellFrame`, `ModalDialog`, `RetailInvoicePage`; đối chiếu kết quả với mã nguồn. Graph/cache giữ ở `/tmp/dtaq-graph`, không thêm vào mã ứng dụng. Không thực hiện suy diễn ngữ nghĩa toàn bộ tài liệu bằng graphify.
- Áp dụng ui-ux-pro-max cho kiểm tra overflow/breakpoint; kết quả tìm kiếm thiên về web nên chỉ áp dụng nguyên tắc kiểm tra, không chuyển các breakpoint mobile sang Swing. Impeccable dùng chế độ Operate, giữ nhận diện Volt Console; launcher không thực thi được nên đọc ngữ cảnh và hướng dẫn trực tiếp. Ponytail giới hạn sửa ở thành phần có lỗi thực tế, không thêm thư viện.

## Vấn đề đã sửa

| Vấn đề quan sát được | Cách sửa |
|---|---|
| Nút thanh toán nằm dưới vùng nhìn ở 1280×680 | Đặt khối thanh toán trước phần thông tin khách, thu gọn khoảng cách; tách nhãn tổng tiền và số tiền để số dài vẫn đầy đủ. Giữ callback, điều kiện bật/tắt và F8. |
| Cột hóa đơn và hàng tìm khách không đủ chỗ khi tăng scale | Giữ cột thanh toán bằng đơn vị pixel logic của Swing; hàng tìm khách xuống dòng khi hẹp. |
| Ẩn panel thương hiệu làm form có thể rơi vào cột rộng 0 | Gắn cell tường minh cho panel/form trong cả đăng nhập và quên mật khẩu; form co được và cuộn dọc khi thấp. |
| Kích thước cửa sổ lớn hơn vùng màn hình ở DPI cao | Kẹp kích thước và minimum theo vùng làm việc của graphics configuration hiện tại, trừ taskbar/insets. Khung chính có kích thước khởi tạo trước maximize. |
| KPI/biểu đồ/bộ lọc giữ bố cục rộng | Thêm Responsive S/M/L/XL, lưới co theo bề rộng và scale thực của FlatLaf; áp dụng các hàng KPI/trang chủ/báo cáo. Legend donut xuống dưới khi hẹp. Bộ lọc tồn kho xuống hai hàng. |
| Minimum width của card/header đẩy nút ra ngoài cửa sổ khi scale | Cho phép co chiều rộng, giữ minimum height; header tự đưa nhóm thao tác xuống hàng. |
| Trang tự giãn có nội dung vượt chiều cao màn hình | AppShell thêm cuộn dọc dự phòng khi minimum height vượt viewport; khi đủ chỗ vẫn giãn theo viewport. Không đổi giá trị `Page.scrollable()`. |
| Bảng ép hẹp cột dù có scrollbar | Tự tắt auto-resize khi viewport nhỏ hơn tổng chiều rộng cột được cấu hình; bảng cuộn ngang bên trong. Bật truy cập bảng bằng bàn phím. |
| Một số ô nhập bị ép thấp, nhãn chưa liên kết accessibility | Giữ minimum height của control; thêm labelFor và accessible name. |
| Icon calendar/hash/truck hiển thị ô đỏ | Bổ sung ba SVG theo hệ nét hiện có, thêm kiểm tra tài nguyên đóng gói. |
| Dialog dài hoặc có bốn nút dễ vượt khung thấp | Nhóm nút tự xuống hàng; chiều cao body trừ không gian thực của header/footer; body cuộn; tính translucency theo màn hình của dialog và giữ nền đặc dự phòng. Kiểm tra Esc đóng. |
| Animation biểu đồ 280–320 ms vượt khuyến nghị kế hoạch | Rút về 180 ms. Giữ animation sidebar 160 ms, toast 180/140 ms và khả năng tắt bằng `flatlaf.animation=false`. |
| Topbar dài chiếm vùng breadcrumb | Giảm thông tin phụ ở vùng hẹp; giữ tooltip, accessible name và focus bàn phím cho nút menu/đăng xuất. |

## Tệp thuộc đợt rà soát này

- `ui/component/Responsive.java` (mới); `Card`, `PageHeader`, `DataTable`, `FormField`, `ModalDialog`, `BarChartPanel`, `DonutChartPanel`.
- `ui/LoginFrame.java`, `ui/ForgotPasswordFrame.java`, `ui/shell/AppShellFrame.java`, `ui/shell/Topbar.java`.
- `ui/page/home/{SalesHomePage,ManagerHomePage}.java`.
- `ui/page/report/{RevenueStatisticsPage,SalesByProductPage,SalesByEmployeePage,LongStockReportPage}.java`.
- `ui/page/invoice/RetailInvoicePage.java`.
- Ba icon `calendar.svg`, `hash.svg`, `truck.svg`.
- `src/test/java/com/dtaq/retail/ui/component/ResponsiveTest.java` và `src/test/java/com/dtaq/retail/ui/UiLayoutSmokeTest.java` (mới).

Các đường dẫn `ui/…` ở trên thuộc `src/main/java/com/dtaq/retail/`; các icon thuộc `src/main/resources/com/dtaq/retail/ui/theme/icons/`. Các tệp theme/sidebar/toast/chứng từ đã được sửa trước phiên này được giữ lại; không gán toàn bộ diff của chúng cho đợt rà soát này.

## Kiểm thử và bằng chứng

### Hồi quy tự động

- Baseline hiện tại: **605 tests, 0 failures, 0 errors, 0 skipped**.
- Bổ sung 9 tests: 5 kiểm tra breakpoint/lưới/bảng/ô nhập/icon; 4 kiểm tra auth hẹp, thanh toán/số tiền lớn, tất cả trang theo vai trò, dialog thấp và Esc.
- Kết quả cuối: **614 tests, 0 failures, 0 errors, 0 skipped**; xem bảng xác nhận bên dưới.
- Các log ERROR/WARN trong ca kiểm thử lỗi giả lập không tự đồng nghĩa build thất bại; kết luận dựa trên Surefire XML và mã thoát Maven.

| Nhóm luồng | Bằng chứng hồi quy có sẵn |
|---|---|
| UC001 Đăng nhập, đặt lại mật khẩu | AuthControllerTest (8), PasswordResetControllerTest (12), cùng smoke form auth/step 2. |
| UC003 Lập hóa đơn bán lẻ | BanHangControllerTest, RetailInvoicePageTest, BanHangThanhToanDbTest (2); smoke số tiền 990.000.000 VND và nút thanh toán ở 1280×680. |
| UC006 Xử lý đổi trả sản phẩm, UC018 Phê duyệt yêu cầu đổi trả | DoiTraControllerTest (29), PheDuyetDoiTraControllerTest (7), PhieuTraHoanTatDbTest (21). |
| UC019 Cập nhật giá bán và thuế suất | GiaThueControllerTest (2), kiểm thử trang giá/thuế; không sửa phân quyền hay snapshot giá/thuế. |
| Khách hàng, sản phẩm/serial, danh mục, khuyến mãi, nhân viên, báo cáo | Bộ kiểm thử controller, DAO, entity, UI hiện có trong toàn bộ Maven suite. |
| Sao lưu/phục hồi FR015/FR016 | Bộ kiểm thử BackupService/RestoreService/BackupController và các ca lịch/sức khỏe hiện có. Không phục hồi dữ liệu thật để thử giao diện. |

Các quy tắc tiền mặt, VND HALF_UP, snapshot lịch sử, ngưỡng phê duyệt hoàn tiền, phân quyền giá/thuế và mô hình tồn kho per-serial được bảo vệ bởi bộ kiểm thử nghiệp vụ hiện có; đợt này không sửa các lớp thực thi các quy tắc đó. Không suy luận từ việc render trang rằng toàn bộ nghiệp vụ đã được kiểm tra tay từ đầu đến cuối.

### Ma trận hiển thị

Dùng JFrame/JDialog thật dưới Xvfb, render bằng Swing; không dùng ảnh mockup.

- 13 trang quản lý + 10 trang bán hàng × 5 kích thước logic: **1024×600, 1280×680, 1366×728, 1536×824, 1920×1040**.
- Kiểm tra render và control bị cắt, minimum height có thể truy cập bằng cuộn, tiêu đề không bị ép chiều cao; chụp gallery. Mỗi cấu hình scale có 115 ảnh trang và ảnh auth, dialog, hóa đơn có dữ liệu.
- Kiểm tra native transform 1×/2×. Trên Linux/JDK đang dùng, `sun.java2d.uiScale=1.25/1.5` thực tế trả transform 1×; vì vậy chạy bổ sung `flatlaf.uiScale=1.25/1.5` và xác nhận user scale tương ứng. Đây là mô phỏng scale control, không phải bằng chứng chạy Windows fractional DPI thật.
- Xem trực tiếp ảnh đại diện: hóa đơn rỗng/có tiền lớn, sản phẩm có dữ liệu, báo cáo tồn kho, thống kê, sao lưu, auth 640/900/1100 và dialog 640×520; không tuyên bố đã xem tay từng pixel của toàn bộ ảnh.
- Gallery và bản JAR nằm trong `target/` (build output, không commit). Ảnh trước sửa ở `/tmp/dtaq-before` chỉ tồn tại trong môi trường kiểm tra.

### Lệnh tái kiểm tra

```bash
# Build + toàn bộ tests, gồm chế độ tắt animation.
xvfb-run -a -s '-screen 0 3840x2160x24' mvn -q -B \
  -Dflatlaf.animation=false -Ddtaq.uiSnapshots=target/ui-review/final package

# Xuất gallery có animation mặc định; không thêm Maven plugin vào pom.xml.
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/dtaq-cp
xvfb-run -a -s '-screen 0 3840x2160x24' java -Dflatlaf.uiScale=1.5 \
  -cp "target/test-classes:target/classes:$(cat /tmp/dtaq-cp)" \
  com.dtaq.retail.ui.UiLayoutSmokeTest target/ui-review/user-scale-1.5
```

Smoke các trang thật cần CSDL đã cấu hình; bộ test layout bỏ qua rõ ràng nếu không có display/CSDL. Khi chạy phiên xác nhận cuối, không được bỏ qua test nào. Gallery chỉ đọc dữ liệu và dùng phiên nhân viên thử cho bố cục; không thanh toán, gửi OTP, lưu cấu hình hay phục hồi dữ liệu thật.

## Giới hạn xác nhận

- Chưa chạy trên Windows thật, màn hình cảm ứng, nhiều màn hình có DPI khác nhau hoặc macOS; không thể bảo đảm mọi loại máy từ thử nghiệm Linux/Xvfb.
- Chưa đối chiếu frame Figma qua MCP. Bộ màu Volt Console và thiết kế hiện có được giữ làm chuẩn trực quan.
- Chưa kiểm tra tay in bằng máy in thật, gửi/nhận OTP qua SMTP thật, Excel trong Microsoft Excel thật, sao lưu/phục hồi phá hủy dữ liệu thật hoặc tình huống mạng/CSDL chậm. Các luồng này có kiểm thử tự động theo phạm vi suite hiện có.
- Chưa thiết kế lại toàn bộ trang/dialog theo mọi archetype của plan và chưa thêm hiệu ứng chuyển trang CardLayout. Hoạt ảnh tập trung ở control/biểu đồ hiện có để giữ vòng đời trang và phản hồi nhanh.

## Kết quả cuối

| Hạng mục | Kết quả |
|---|---|
| Build cuối: Maven package, animation tắt | Thành công, mã thoát 0; tạo `target/dtaq-retail-1.0.0-SNAPSHOT.jar`. |
| Surefire toàn bộ suite | **614 tests, 0 failures, 0 errors, 0 skipped**. |
| Gallery: 23 trang × 5 kích thước × 4 cấu hình scale | **460 lần render trang**; bốn lượt gallery đều mã thoát 0. Có thêm ảnh auth, hóa đơn dữ liệu thử và dialog cho mỗi lượt. |
| Native scale được đo | 1×, 2×; riêng 125/150% dùng FlatLaf user-scale được đo là 1.25/1.5. |
| Khởi chạy entry point `Main.main` từ classpath bản fat JAR cuối | Thành công ở màn 1280×720 (login 1100×720) và 1920×1080 @200% (login 960×540 logic); cửa sổ nằm trong màn hình. |
| Cấu hình khởi chạy thử | Tắt lịch sao lưu và tự động phục hồi bằng cờ JVM để tránh chạy tác vụ dữ liệu thật; không sửa tệp cấu hình. Flyway xác nhận schema v14 đã cập nhật, không chạy migration mới. |
| `git diff --check` | Thành công. Không thêm dependency, sửa schema, controller, DAO, entity hoặc dịch vụ. |
| Git | Giữ nhánh hiện tại, chưa commit/push. |

Ảnh minh họa cuối:

- [Hóa đơn 1280×680, số tiền lớn](../../../target/ui-review/final/invoice-populated-1280.png).
- [Quên mật khẩu bước 2 ở cửa sổ 640×520](../../../target/ui-review/final/ForgotPasswordFrame-step2-640.png).
- [Báo cáo tồn kho 1024×600, mô phỏng control 150%](../../../target/ui-review/user-scale-1.5/NHAN_VIEN_QUAN_LY-LONG_STOCK_REPORT-1024.png).
- [Khởi chạy JAR ở native scale 200%](../../../target/ui-review/startup-200.png).

Ảnh trong `target/` được tái tạo bằng lệnh trên và có thể bị xóa khi `mvn clean`. Kết quả này xác nhận các cấu hình đã chạy; việc nghiệm thu Windows thật và các thiết bị ngoại vi còn nằm trong giới hạn nêu trên.

## Bổ sung theo yêu cầu: sidebar xuống dòng và kéo chỉnh độ rộng

Tên mục menu dài dùng HTML table của Swing để tự xuống dòng, tăng chiều cao hàng và giữ tooltip/accessible name. Không dùng baseline một dòng cho nút HTML nhiều dòng để tránh lỗi FlatLaf/MigLayout khi co cửa sổ. Cạnh phải sidebar có tay nắm kéo 5 px, giới hạn độ rộng mở rộng 220–420 px; khi kéo giữ ít nhất khoảng 640 px cho nội dung nếu kích thước cửa sổ cho phép. Độ rộng được nhớ trong phiên khi thu gọn/mở lại; không lưu cấu hình ra đĩa và không đổi thứ tự menu.

Tệp: `NavItem`, `Sidebar`, `AppShellFrame`; thêm `NavItemWrapTest`. Smoke test đợi layout/validate hoàn tất sau chuỗi resize rồi đo, giữ nguyên các assertion chống cắt nội dung. Kiểm tra bằng gallery thật dưới Xvfb; chưa thao tác kéo bằng chuột thật trên Wayland.
