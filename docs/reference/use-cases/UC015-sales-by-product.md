---
title: "UC015 – Thống kê doanh số theo sản phẩm"
uc_id: "UC015"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC015 – Thống kê doanh số theo sản phẩm"
page: "Chưa xác minh"
---

# UC015 – Thống kê doanh số theo sản phẩm

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC015 – Thống kê doanh số theo sản phẩm”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh. Định vị bằng Chương 3, mục IV, heading UC và nhãn bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC015 – Thống kê doanh số theo sản phẩm |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC015 – Thống kê doanh số theo sản phẩm”, trước tiểu mục “a. Sequence diagram” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | [Danh mục use case](catalog.md) — Chương 3, mục III (UC001–UC019) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC015 |
| Tiêu đề bảng (nguyên văn) | Use case: Thống kê doanh số theo sản phẩm |
| Mục đích: | Đánh giá số lượng bán và doanh số của từng dòng sản phẩm. |
| Mô tả: | Quản lý tổng hợp số lượng thực bán và doanh số trước thuế, có điều chỉnh đổi/trả. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. |
| Điều kiện sau: | Hiển thị kết quả thống kê; dữ liệu giao dịch không thay đổi. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thống kê doanh số theo sản phẩm”. |  |
|  | 2. Hiển thị bộ lọc thống kê. |
| 3. Chọn khoảng thời gian và phạm vi sản phẩm. |  |
| 4. Chọn thống kê. |  |
|  | 5. Kiểm tra điều kiện thống kê. |
|  | 6. Lấy hóa đơn đã thanh toán và chi tiết liên quan. |
|  | 7. Tổng hợp số lượng bán và tiền hàng trước thuế theo dòng sản phẩm. |
|  | 8. Điều chỉnh số lượng và doanh số theo các giao dịch đổi/trả đã hoàn tất. |
|  | 9. Hiển thị kết quả thống kê theo sản phẩm. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 5.a.1. Phát hiện ngày bắt đầu lớn hơn ngày kết thúc. |
|  | 5.a.2. Hiển thị thông báo khoảng thời gian không hợp lệ. |
|  | 5.a.3. Quay lại bước 3. |
|  | 6.a.1. Không có giao dịch phù hợp. |
|  | 6.a.2. Hiển thị kết quả không có phát sinh. |
| 9.a.1. Chọn sản phẩm cần đối chiếu. |  |
|  | 9.a.2. Hiển thị các giao dịch dùng để tổng hợp sản phẩm đó. |

## Sơ đồ

Nguồn: Chương 3, mục IV, `UC015`, các tiểu mục “a. Sequence diagram” và “b. Activity diagram” nằm ngay sau bảng đặc tả trong DOCX.

### a. Sequence diagram

- Định vị nguồn: Chương 3, mục IV, `UC015`, tiểu mục “a. Sequence diagram”.
- Asset trong DOCX: `word/media/image30.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image30.png](../assets/diagrams/image30.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

### b. Activity diagram

- Định vị nguồn: Chương 3, mục IV, `UC015`, tiểu mục “b. Activity diagram”.
- Asset trong DOCX: `word/media/image23.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image23.png](../assets/diagrams/image23.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Các ràng buộc nghiệp vụ rút trực tiếp:
  - Chỉ Nhân viên quản lý đã đăng nhập đúng quyền mới được thực hiện chức năng.
  - Chỉ hóa đơn đã thanh toán và các chi tiết liên quan mới được dùng để tổng hợp.
  - Số liệu phải tổng hợp theo dòng sản phẩm, gồm số lượng bán và tiền hàng trước thuế.
  - Số lượng và doanh số phải được điều chỉnh theo các giao dịch đổi/trả đã hoàn tất.
  - Ngày bắt đầu không được lớn hơn ngày kết thúc; nếu vi phạm, hệ thống thông báo khoảng thời gian không hợp lệ và quay lại bước chọn thời gian.
  - Nếu không có giao dịch phù hợp, hệ thống phải hiển thị kết quả không có phát sinh.
  - Khi chọn một sản phẩm để đối chiếu, hệ thống phải hiển thị các giao dịch dùng để tổng hợp sản phẩm đó.
  - Kết quả thống kê chỉ để xem; dữ liệu giao dịch không được thay đổi.
- Bảng gốc không có mục “Error Flows” riêng; các nhánh xử lý không hợp lệ và không có dữ liệu nằm trong Luồng sự kiện phụ.
- Trong phạm vi đặc tả UC015, nguồn không quy định chi tiết cách xác định “phạm vi sản phẩm” hoặc công thức xử lý từng phần khi điều chỉnh theo giao dịch đổi/trả.
- Hai sơ đồ chỉ tồn tại dưới dạng hình nhúng trong DOCX; tài liệu này không vẽ lại, không suy diễn nội dung sơ đồ.

Trước: [UC014 – Báo cáo sản phẩm tồn kho lâu ngày](UC014-inventory-report.md) — Sau: [UC016 – Thống kê doanh số theo nhân viên](UC016-sales-by-employee.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
