---
title: "UC017 – Thống kê doanh thu"
uc_id: "UC017"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC017 – Thống kê doanh thu"
page: "Chưa xác minh"
---

# UC017 – Thống kê doanh thu

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC017 – Thống kê doanh thu”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh. Định vị bằng Chương 3, mục IV, heading UC và nhãn bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC017 – Thống kê doanh thu |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC017 – Thống kê doanh thu”, trước tiểu mục “a. Sequence diagram” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | [Danh mục use case](catalog.md) — Chương 3, mục III (UC001–UC019) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC017 |
| Tiêu đề bảng (nguyên văn) | Use case: Thống kê doanh thu |
| Mục đích: | Tổng hợp doanh thu nội bộ và hỗ trợ xuất báo cáo. |
| Mô tả: | Quản lý xem doanh thu trước thuế đã điều chỉnh đổi/trả; tiền thuế và tổng tiền thanh toán được thể hiện riêng. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. |
| Điều kiện sau: | Hiển thị báo cáo; tạo file Excel nếu xuất thành công; dữ liệu giao dịch không thay đổi. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thống kê doanh thu”. |  |
|  | 2. Hiển thị bộ lọc kỳ báo cáo. |
| 3. Chọn ngày, tuần, tháng, năm hoặc khoảng thời gian. |  |
| 4. Chọn thống kê. |  |
|  | 5. Kiểm tra kỳ báo cáo. |
|  | 6. Lấy các hóa đơn đã thanh toán hợp lệ trong kỳ. |
|  | 7. Tổng hợp giá trị hàng hóa trước thuế. |
|  | 8. Tổng hợp tiền thuế và tổng tiền thanh toán thành các chỉ tiêu riêng. |
|  | 9. Điều chỉnh các chỉ tiêu theo giao dịch đổi/trả đã hoàn tất trong kỳ. |
|  | 10. Hiển thị báo cáo doanh thu. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 5.a.1. Phát hiện kỳ báo cáo không hợp lệ. |
|  | 5.a.2. Hiển thị yêu cầu điều chỉnh. |
|  | 5.a.3. Quay lại bước 3. |
|  | 6.a.1. Không có giao dịch phù hợp. |
|  | 6.a.2. Hiển thị báo cáo không có phát sinh. |
| 10.a.1. Chọn xuất báo cáo Excel. |  |
|  | 10.a.2. Hiển thị hộp thoại lưu file. |
| 10.a.3. Chọn tên file và nơi lưu. |  |
|  | 10.a.5. Tạo file Excel từ kết quả thống kê đang hiển thị. |
|  | 10.a.6. Thông báo xuất báo cáo thành công. |
|  | 10.b.1. Phát hiện không thể ghi file báo cáo. |
|  | 10.b.2. Hiển thị thông báo xuất báo cáo thất bại. |
|  | 10.b.3. Giữ nguyên báo cáo trên màn hình. |

## Sơ đồ

Nguồn: Chương 3, mục IV, `UC017`, các tiểu mục “a. Sequence diagram” và “b. Activity diagram” nằm ngay sau bảng đặc tả trong DOCX.

### a. Sequence diagram

- Định vị nguồn: Chương 3, mục IV, `UC017`, tiểu mục “a. Sequence diagram”.
- Asset trong DOCX: `word/media/image38.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image38.png](../assets/diagrams/image38.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

### b. Activity diagram

- Định vị nguồn: Chương 3, mục IV, `UC017`, tiểu mục “b. Activity diagram”.
- Asset trong DOCX: `word/media/image10.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image10.png](../assets/diagrams/image10.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Các ràng buộc nghiệp vụ rút trực tiếp:
  - Chỉ Nhân viên quản lý đã đăng nhập đúng quyền mới được thực hiện chức năng.
  - Kỳ báo cáo có thể là ngày, tuần, tháng, năm hoặc một khoảng thời gian.
  - Kỳ báo cáo phải hợp lệ; nếu không hợp lệ, hệ thống yêu cầu điều chỉnh và quay lại bước chọn kỳ.
  - Chỉ các hóa đơn đã thanh toán hợp lệ trong kỳ mới được dùng để tổng hợp.
  - Giá trị hàng hóa trước thuế, tiền thuế và tổng tiền thanh toán phải là các chỉ tiêu riêng.
  - Các chỉ tiêu phải được điều chỉnh theo giao dịch đổi/trả đã hoàn tất trong kỳ.
  - Nếu không có giao dịch phù hợp, hệ thống phải hiển thị báo cáo không có phát sinh.
  - Khi xuất Excel, file phải được tạo từ kết quả thống kê đang hiển thị; nếu không thể ghi file, hệ thống thông báo thất bại và giữ nguyên báo cáo trên màn hình.
  - Sau khi xem hoặc xuất báo cáo, dữ liệu giao dịch không được thay đổi.
- Bảng gốc không có mục “Error Flows” riêng; các nhánh xử lý không hợp lệ, không có dữ liệu và lỗi ghi file nằm trong Luồng sự kiện phụ.
- Nguồn đánh số luồng phụ từ `10.a.3` sang `10.a.5`, thiếu `10.a.4`. Tài liệu này giữ nguyên đúng số bước của nguồn và không tự bổ sung bước còn thiếu.
- Nguồn không quy định định dạng cụ thể của file Excel hoặc chi tiết cách đặt tên file.
- Hai sơ đồ chỉ tồn tại dưới dạng hình nhúng trong DOCX; tài liệu này không vẽ lại, không suy diễn nội dung sơ đồ.

Trước: [UC016 – Thống kê doanh số theo nhân viên](UC016-sales-by-employee.md) — Sau: [UC018 – Phê duyệt yêu cầu đổi trả](UC018-return-approval.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
