---
title: "UC018 – Phê duyệt yêu cầu đổi trả"
uc_id: "UC018"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC018 – Phê duyệt yêu cầu đổi trả"
page: "Chưa xác minh"
---

# UC018 – Phê duyệt yêu cầu đổi trả

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC018 – Phê duyệt yêu cầu đổi trả”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh. Định vị bằng Chương 3, mục IV, heading UC và bảng đặc tả tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC018 – Phê duyệt yêu cầu đổi trả |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC018 – Phê duyệt yêu cầu đổi trả”, trước heading UC019 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh sách use case | [Danh mục use case](catalog.md) — Chương 3, mục III (UC001–UC019) |
| Yêu cầu / quy tắc liên quan | [BR-07](../requirements/scope-and-requirements.md#5-quy-tắc-nghiệp-vụ-chính), [FR008](../requirements/scope-and-requirements.md#fr008--xử-lý-hoàn-trảđổi-sản-phẩm) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC018 |
| Tiêu đề bảng (nguyên văn) | Use case: Phê duyệt yêu cầu đổi trả |
| Mục đích: | Quyết định chấp nhận hoặc từ chối yêu cầu đổi/trả vượt hạn mức. |
| Mô tả: | Quản lý kiểm tra phiếu có tiền hoàn từ 3.000.000 đồng trở lên trước khi nhân viên hoàn tất giao dịch. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. Có phiếu đổi hoặc phiếu trả đang chờ duyệt. |
| Điều kiện sau: | Lưu quyết định, người duyệt, ngày duyệt và ý kiến. Việc duyệt chưa tự thực hiện hoàn tiền hoặc cập nhật tồn kho. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Phê duyệt yêu cầu đổi trả”. |  |
|  | 2. Hiển thị danh sách phiếu chờ duyệt. |
| 3. Chọn phiếu cần kiểm tra. |  |
|  | 4. Hiển thị phiếu, hóa đơn gốc và máy liên quan. |
| 5. Kiểm tra lý do, điều kiện xử lý và số tiền hoàn. |  |
| 6. Chọn phê duyệt phiếu. |  |
| 7. Nhập ý kiến phê duyệt. |  |
| 8. Xác nhận quyết định. |  |
|  | 9. Kiểm tra phiếu vẫn đang chờ duyệt. |
|  | 10. Lưu trạng thái đã duyệt cùng người duyệt, ngày duyệt và ý kiến. |
|  | 11. Hiển thị kết quả phê duyệt. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 2.a.1. Không có phiếu chờ duyệt. |
|  | 2.a.2. Hiển thị danh sách trống. |
| 6.a.1. Chọn từ chối yêu cầu. |  |
| 6.a.2. Nhập lý do từ chối. |  |
| 6.a.3. Xác nhận từ chối. |  |
|  | 6.a.4. Kiểm tra phiếu vẫn đang chờ duyệt. |
|  | 6.a.5. Lưu trạng thái từ chối cùng thông tin người duyệt. |
|  | 6.a.6. Hiển thị kết quả từ chối. |
|  | 9.a.1. Phát hiện phiếu đã được xử lý hoặc không còn hợp lệ. |
|  | 9.a.2. Từ chối ghi đè quyết định hiện tại. |
|  | 9.a.3. Tải lại thông tin phiếu. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Các ràng buộc nghiệp vụ rút trực tiếp:
  - Chỉ Nhân viên quản lý đã đăng nhập đúng quyền mới được phê duyệt.
  - Chỉ được xử lý phiếu đổi hoặc phiếu trả đang chờ duyệt.
  - Đối tượng kiểm tra là phiếu có tiền hoàn từ 3.000.000 đồng trở lên; quản lý phải kiểm tra lý do, điều kiện xử lý và số tiền hoàn trước khi quyết định.
  - Phê duyệt phải lưu trạng thái đã duyệt cùng người duyệt, ngày duyệt và ý kiến.
  - Từ chối phải nhập lý do từ chối và lưu trạng thái từ chối cùng thông tin người duyệt.
  - Trước khi ghi quyết định, hệ thống phải kiểm tra phiếu vẫn đang chờ duyệt; nếu phiếu đã được xử lý hoặc không còn hợp lệ, hệ thống từ chối ghi đè quyết định hiện tại và tải lại thông tin phiếu.
  - Việc phê duyệt chỉ lưu quyết định; chưa tự thực hiện hoàn tiền hoặc cập nhật tồn kho.
  - Không được ghi đè lên quyết định đã có của phiếu.
- Bảng gốc không có mục “Error Flows” riêng; các nhánh không có phiếu, từ chối và phiếu không còn hợp lệ nằm trong Luồng sự kiện phụ.
- Ngưỡng phê duyệt và thẩm quyền: giữ nguyên câu chữ nguồn “tiền hoàn từ 3.000.000 đồng trở lên” và tác nhân “Nhân viên quản lý”. Đối chiếu [BR-07](../requirements/scope-and-requirements.md#5-quy-tắc-nghiệp-vụ-chính) và [FR008](../requirements/scope-and-requirements.md#fr008--xử-lý-hoàn-trảđổi-sản-phẩm).
- Đặc tả gốc không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động cho UC018; tài liệu này không vẽ sơ đồ thay thế.

Trước: [UC017 – Thống kê doanh thu](UC017-revenue-statistics.md) — Sau: [UC019 – Cập nhật giá bán và thuế suất](UC019-price-tax-update.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
