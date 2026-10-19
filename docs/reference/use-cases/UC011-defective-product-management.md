---
title: "UC011 – Thêm sản phẩm lỗi"
uc_id: "UC011"
catalog_title: "Quản lý sản phẩm lỗi"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC011 – Thêm sản phẩm lỗi"
page: "Chưa xác minh"
---

# UC011 – Thêm sản phẩm lỗi

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC011 – Thêm sản phẩm lỗi”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng đặc tả tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC011 có tên “Quản lý sản phẩm lỗi”. Heading đặc tả tại Chương 3, mục IV là “UC011 – Thêm sản phẩm lỗi”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý sản phẩm lỗi (Chương 3, mục III) |
| Heading đặc tả | UC011 – Thêm sản phẩm lỗi (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC011 – Thêm sản phẩm lỗi”, trước heading UC012 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Cảnh báo chưa được giải quyết

> **C-03 — Tác nhân không thống nhất:** Danh mục Ch.3 §III ghi tác nhân “Quản lý”; đặc tả Ch.3 §IV ghi tác nhân “Nhân viên bán hàng.”; mô tả tác nhân Ch.3 §II giao “Quản lý sản phẩm, dòng sản phẩm, danh mục và sản phẩm lỗi” cho Nhân viên quản lý. Ba nguồn không thống nhất; tài liệu này giữ nguyên cả ba và **không chọn** tác nhân. Xem [C-03 — Tác nhân UC008–UC011 không thống nhất](../requirements/scope-and-requirements.md#c-03--tác-nhân-uc008uc011-không-thống-nhất).

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm sản phẩm lỗi |
| Mục đích: | Ghi nhận hồ sơ lỗi của một máy trong hệ thống. |
| Mô tả: | Nhân viên chọn máy, nhập thông tin lỗi và liên kết chứng từ đổi/trả nếu có. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. Máy đã tồn tại và được tiếp nhận hoặc kiểm tra thực tế. |
| Điều kiện sau: | Thành công: Tạo sản phẩm lỗi ở trạng thái chờ xử lý; máy không được tính vào lượng sẵn sàng bán. Thất bại: Không tạo hồ sơ lỗi mới. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm sản phẩm lỗi”. |  |
|  | 2. Hiển thị biểu mẫu ghi nhận lỗi. |
| 3. Chọn máy theo số serial. |  |
| 4. Nhập mô tả lỗi. |  |
| 5. Nhập ngày phát hiện lỗi. |  |
| 6. Chọn chứng từ đổi/trả liên quan nếu có. |  |
| 7. Chọn lưu. |  |
|  | 8. Kiểm tra thông tin lỗi và liên kết chứng từ. |
|  | 9. Kiểm tra hồ sơ lỗi đang xử lý của máy. |
|  | 10. Lưu sản phẩm lỗi ở trạng thái chờ xử lý. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 8.a.1. Phát hiện thiếu mô tả lỗi hoặc ngày phát hiện không hợp lệ. |
|  | 8.a.2. Hiển thị lỗi nhập liệu. |
| 8.a.3. Điều chỉnh thông tin lỗi. |  |
| 8.a.4. Chọn lưu. |  |
|  | 8.a.5. Tiếp tục bước 8. |
|  | 8.b.1. Phát hiện chứng từ không liên quan đến máy được chọn. |
|  | 8.b.2. Thông báo liên kết chứng từ không hợp lệ. |
|  | 8.b.3. Quay lại bước 6. |
|  | 9.a.1. Phát hiện máy đã có hồ sơ lỗi đang được xử lý. |
|  | 9.a.2. Hiển thị hồ sơ hiện có. |
|  | 9.a.3. Kết thúc thao tác mà không tạo hồ sơ lỗi trùng. |
|  | 10.a.1. Phát hiện lỗi khi lưu hồ sơ. |
|  | 10.a.2. Hủy các thay đổi của lần ghi nhận hiện tại. |
|  | 10.a.3. Thông báo ghi nhận lỗi thất bại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Đã đối chiếu toàn bộ 34 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 10 bước luồng chính và 14 bước luồng phụ.
- UC011 được [UC008 – Thêm sản phẩm](UC008-product-management.md) tham chiếu tại bước 10.a.3; tài liệu này không thêm ánh xạ yêu cầu chức năng nào.
- Không có sơ đồ riêng cho UC011 trong DOCX.

Trước: [UC010 – Thêm danh mục sản phẩm](UC010-product-category-management.md) — Sau: [UC012 – Thêm khuyến mãi](UC012-promotion-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
