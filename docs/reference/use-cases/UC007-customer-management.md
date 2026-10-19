---
title: "UC007 – Thêm khách hàng"
uc_id: "UC007"
catalog_title: "Quản lý khách hàng"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC007 – Thêm khách hàng"
page: "Chưa xác minh"
---

# UC007 – Thêm khách hàng

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC007 – Thêm khách hàng”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng đặc tả tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC007 có tên “Quản lý khách hàng”. Heading đặc tả tại Chương 3, mục IV là “UC007 – Thêm khách hàng”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý khách hàng (Chương 3, mục III) |
| Heading đặc tả | UC007 – Thêm khách hàng (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC007 – Thêm khách hàng”, trước heading UC008 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm khách hàng |
| Mục đích: | Ghi nhận khách hàng mới phục vụ bán hàng và tra cứu giao dịch. |
| Mô tả: | Nhân viên nhập thông tin khách hàng để tạo hồ sơ khách hàng. |
| Tác nhân: | Nhân viên bán hàng |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. |
| Điều kiện sau: | Thành công: Tạo khách hàng mới với mã duy nhất. Thất bại: Không tạo hồ sơ khách hàng mới. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm khách hàng”. |  |
|  | 2. Hiển thị biểu mẫu thêm khách hàng. |
| 3. Nhập họ tên khách hàng. |  |
| 4. Nhập số điện thoại. |  |
| 5. Nhập địa chỉ. |  |
| 6. Chọn lưu. |  |
|  | 7. Kiểm tra tính đầy đủ và hợp lệ của thông tin. |
|  | 8. Kiểm tra khách hàng hiện có theo số điện thoại. |
|  | 9. Cấp mã khách hàng mới. |
|  | 10. Lưu thông tin khách hàng. |
|  | 11. Hiển thị thông báo thêm thành công. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 7.a.1. Phát hiện thiếu thông tin bắt buộc hoặc số điện thoại không hợp lệ. |
|  | 7.a.2. Hiển thị lỗi nhập liệu. |
| 7.a.3. Điều chỉnh thông tin khách hàng. |  |
| 7.a.4. Chọn lưu. |  |
|  | 7.a.5. Tiếp tục bước 7. |
|  | 8.a.1. Phát hiện số điện thoạ |
|  | 8.a.2. Hiển thị hồ sơ hiện có để đối chiếu. |
| 8.a.3. Chọn sử dụng hồ sơ hiện có sau khi đối chiếu. |  |
|  | 8.a.4. Kết thúc thao tác mà không tạo hồ sơ trùng. |
|  | 10.a.1. Phát hiện lỗi khi lưu khách hàng. |
|  | 10.a.2. Thông báo thêm khách hàng thất bại. |
|  | 10.a.3. Giữ thông tin đã nhập để nhân viên thử lại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Bước 8.a.1 trong DOCX kết thúc bằng chuỗi “Phát hiện số điện thoạ” (thiếu phần cuối). Bản trích giữ nguyên đúng chuỗi nguồn, không tự hoàn thiện hay suy đoán nội dung còn thiếu.
- Đã đối chiếu toàn bộ 33 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 11 bước luồng chính và 12 bước luồng phụ.
- Không có sơ đồ riêng cho UC007 trong DOCX.

Trước: [UC006 – Xử lý đổi trả sản phẩm](UC006-return-exchange.md) — Sau: [UC008 – Thêm sản phẩm](UC008-product-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
