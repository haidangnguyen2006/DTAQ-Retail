---
title: "UC012 – Thêm khuyến mãi"
uc_id: "UC012"
catalog_title: "Quản lý khuyến mãi"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC012 – Thêm khuyến mãi"
page: "Chưa xác minh"
---

# UC012 – Thêm khuyến mãi

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC012 – Thêm khuyến mãi”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng đặc tả tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC012 có tên “Quản lý khuyến mãi”. Heading đặc tả tại Chương 3, mục IV là “UC012 – Thêm khuyến mãi”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý khuyến mãi (Chương 3, mục III) |
| Heading đặc tả | UC012 – Thêm khuyến mãi (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC012 – Thêm khuyến mãi”, trước heading UC013 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Cảnh báo nguồn và quyết định implementation

> **C-04 — Phạm vi khuyến mãi chưa thống nhất:** Ch.1 §II.6 ghi hệ thống không xây dựng phân hệ quản lý chương trình khuyến mãi hoàn chỉnh và Ch.1 §II.8 loại trừ quản lý chương trình khuyến mãi chuyên sâu; trong khi Ch.3 §III có UC012 – Quản lý khuyến mãi, Ch.3 §IV đặc tả “Thêm khuyến mãi”, luồng UC003 kiểm tra và áp dụng khuyến mãi, và danh mục nêu thực thể `KhuyenMai`. Tài liệu này giữ nguyên cả hai phía và **không quyết định** phạm vi khuyến mãi. Xem [C-04 — Khuyến mãi vừa ngoài phạm vi vừa có use case](../requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case).

> **C-12 — Mã định danh (đã giải quyết cho implementation hiện tại):** Bước 3 của luồng chính giữ nguyên văn “Nhập mã, tên và mô tả chương trình.” như bằng chứng nguồn. Quyết định implementation (đã duyệt 2026-10-20): mã định danh do **ứng dụng sinh** khi lưu mới thành công, giữ đúng định dạng `TIỀN_TỐ-YYYY-NNNNNN`; V1/PostgreSQL không dùng sequence, identity hay default. Xung đột với quy ước mã tự sinh tại Ch.4 §I.3, `RB02`, `RB03`, XBR-14 **vẫn tồn tại trong nguồn** và không bị sửa. Xem [C-12 — Nhập mã định danh so với mã tự sinh bất biến](../requirements/scope-and-requirements.md#c-12--nhập-mã-định-danh-so-với-mã-tự-sinh-bất-biến).

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm khuyến mãi |
| Mục đích: | Tạo chương trình ưu đãi cho dòng sản phẩm. |
| Mô tả: | Quản lý nhập nội dung, thời gian, loại giảm, giá trị giảm và dòng sản phẩm áp dụng. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. Dòng sản phẩm áp dụng đã tồn tại. |
| Điều kiện sau: | Thành công: Tạo khuyến mãi và liên kết dòng sản phẩm. Chương trình chỉ được áp dụng khi đang dùng và còn hiệu lực. Thất bại: Không tạo chương trình mới. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm khuyến mãi”. |  |
|  | 2. Hiển thị biểu mẫu thêm chương trình. |
| 3. Nhập mã, tên và mô tả chương trình. |  |
| 4. Chọn thời gian bắt đầu và kết thúc. |  |
| 5. Chọn loại giảm và nhập giá trị giảm. |  |
| 6. Chọn dòng sản phẩm áp dụng. |  |
| 7. Chọn trạng thái sử dụng ban đầu. |  |
| 8. Chọn lưu. |  |
|  | 9. Kiểm tra tính hợp lệ của thông tin chương trình. |
|  | 10. Kiểm tra mã chương trình và xung đột thời gian áp dụng. |
|  | 11. Lưu khuyến mãi và liên kết với dòng sản phẩm. |
|  | 12. Hiển thị thông báo thêm khuyến mãi thành công. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 9.a.1. Phát hiện thiếu thông tin, thời gian hoặc giá trị giảm không hợp lệ. |
|  | 9.a.2. Hiển thị lỗi nhập liệu. |
| 9.a.3. Điều chỉnh thông tin chương trình. |  |
| 9.a.4. Chọn lưu. |  |
|  | 9.a.5. Tiếp tục bước 9. |
|  | 10.a.1. Phát hiện mã khuyến mãi đã tồn tại. |
|  | 10.a.2. Hiển thị thông báo trùng mã. |
|  | 10.a.3. Quay lại bước 3. |
|  | 10.b.1. Phát hiện dòng sản phẩm có chương trình khác được áp dụng trong cùng thời gian. |
|  | 10.b.2. Hiển thị thông báo xung đột khuyến mãi. |
| 10.b.3. Điều chỉnh thời gian hoặc dòng sản phẩm áp dụng. |  |
| 10.b.4. Chọn lưu. |  |
|  | 10.b.5. Tiếp tục bước 9. |
|  | 11.a.1. Phát hiện lỗi khi lưu chương trình. |
|  | 11.a.2. Thông báo thêm khuyến mãi thất bại. |
|  | 11.a.3. Giữ thông tin đã nhập để quản lý thử lại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Đã đối chiếu toàn bộ 38 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 12 bước luồng chính và 16 bước luồng phụ.
- Không có sơ đồ riêng cho UC012 trong DOCX.

Trước: [UC011 – Thêm sản phẩm lỗi](UC011-defective-product-management.md) — Sau: [UC013 – Thêm nhân viên bán hàng](UC013-sales-staff-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
