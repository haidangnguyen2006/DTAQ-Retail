---
title: "UC016 – Thống kê doanh số theo nhân viên"
uc_id: "UC016"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC016 – Thống kê doanh số theo nhân viên"
page: "Chưa xác minh"
---

# UC016 – Thống kê doanh số theo nhân viên

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC016 – Thống kê doanh số theo nhân viên”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh. Định vị bằng Chương 3, mục IV, heading UC và nhãn bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC016 – Thống kê doanh số theo nhân viên |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC016 – Thống kê doanh số theo nhân viên”, trước tiểu mục “a. Sequence diagram” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | [Danh mục use case](catalog.md) — Chương 3, mục III (UC001–UC019) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC016 |
| Tiêu đề bảng (nguyên văn) | Use case: Thống kê doanh số theo nhân viên |
| Mục đích: | Theo dõi kết quả bán hàng của từng nhân viên. |
| Mô tả: | Quản lý tổng hợp số lượng hóa đơn và doanh số trước thuế theo người lập, có điều chỉnh đổi/trả. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. |
| Điều kiện sau: | Hiển thị kết quả theo nhân viên; dữ liệu giao dịch không thay đổi. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thống kê doanh số theo nhân viên”. |  |
|  | 2. Hiển thị bộ lọc thống kê. |
| 3. Chọn khoảng thời gian và phạm vi nhân viên. |  |
| 4. Chọn thống kê. |  |
|  | 5. Kiểm tra điều kiện thống kê. |
|  | 6. Lấy các hóa đơn đã thanh toán trong phạm vi lọc. |
|  | 7. Nhóm hóa đơn theo nhân viên lập. |
|  | 8. Tổng hợp số lượng hóa đơn và tiền hàng trước thuế. |
|  | 9. Điều chỉnh doanh số theo giao dịch đổi/trả liên quan. |
|  | 10. Hiển thị kết quả theo nhân viên. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 5.a.1. Phát hiện khoảng thời gian không hợp lệ. |
|  | 5.a.2. Hiển thị yêu cầu điều chỉnh thời gian. |
|  | 5.a.3. Quay lại bước 3. |
|  | 6.a.1. Không có hóa đơn phù hợp. |
|  | 6.a.2. Hiển thị kết quả không có phát sinh. |
| 10.a.1. Chọn nhân viên cần đối chiếu. |  |
|  | 10.a.2. Hiển thị các hóa đơn và điều chỉnh đổi/trả dùng để tính doanh số. |

## Sơ đồ

Nguồn: Chương 3, mục IV, `UC016`, các tiểu mục “a. Sequence diagram” và “b. Activity diagram” nằm ngay sau bảng đặc tả trong DOCX.

### a. Sequence diagram

- Định vị nguồn: Chương 3, mục IV, `UC016`, tiểu mục “a. Sequence diagram”.
- Asset trong DOCX: `word/media/image35.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image35.png](../assets/diagrams/image35.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

### b. Activity diagram

- Định vị nguồn: Chương 3, mục IV, `UC016`, tiểu mục “b. Activity diagram”.
- Asset trong DOCX: `word/media/image37.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image37.png](../assets/diagrams/image37.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Các ràng buộc nghiệp vụ rút trực tiếp:
  - Chỉ Nhân viên quản lý đã đăng nhập đúng quyền mới được thực hiện chức năng.
  - Chỉ các hóa đơn đã thanh toán trong phạm vi lọc mới được dùng để thống kê.
  - Hóa đơn phải được nhóm theo nhân viên lập.
  - Số liệu phải tổng hợp số lượng hóa đơn và tiền hàng trước thuế.
  - Doanh số phải được điều chỉnh theo các giao dịch đổi/trả liên quan.
  - Khoảng thời gian phải hợp lệ; nếu không hợp lệ, hệ thống yêu cầu điều chỉnh thời gian và quay lại bước chọn thời gian.
  - Nếu không có hóa đơn phù hợp, hệ thống phải hiển thị kết quả không có phát sinh.
  - Khi chọn một nhân viên để đối chiếu, hệ thống phải hiển thị các hóa đơn và điều chỉnh đổi/trả dùng để tính doanh số.
  - Kết quả thống kê chỉ để xem; dữ liệu giao dịch không được thay đổi.
- Bảng gốc không có mục “Error Flows” riêng; các nhánh xử lý không hợp lệ và không có dữ liệu nằm trong Luồng sự kiện phụ.
- Trong phạm vi đặc tả UC016, nguồn không quy định chi tiết cách xác định “phạm vi nhân viên” hoặc công thức xử lý từng phần khi điều chỉnh doanh số theo giao dịch đổi/trả.
- Hai sơ đồ chỉ tồn tại dưới dạng hình nhúng trong DOCX; tài liệu này không vẽ lại, không suy diễn nội dung sơ đồ.

Trước: [UC015 – Thống kê doanh số theo sản phẩm](UC015-sales-by-product.md) — Sau: [UC017 – Thống kê doanh thu](UC017-revenue-statistics.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
