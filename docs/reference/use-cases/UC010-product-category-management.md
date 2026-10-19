---
title: "UC010 – Thêm danh mục sản phẩm"
uc_id: "UC010"
catalog_title: "Quản lý danh mục sản phẩm"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC010 – Thêm danh mục sản phẩm"
page: "Chưa xác minh"
---

# UC010 – Thêm danh mục sản phẩm

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC010 – Thêm danh mục sản phẩm”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng đặc tả tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC010 có tên “Quản lý danh mục sản phẩm”. Heading đặc tả tại Chương 3, mục IV là “UC010 – Thêm danh mục sản phẩm”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý danh mục sản phẩm (Chương 3, mục III) |
| Heading đặc tả | UC010 – Thêm danh mục sản phẩm (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC010 – Thêm danh mục sản phẩm”, trước heading UC011 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Cảnh báo chưa được giải quyết

> **C-03 — Tác nhân không thống nhất:** Danh mục Ch.3 §III ghi tác nhân “Quản lý”; đặc tả Ch.3 §IV ghi tác nhân “Nhân viên bán hàng.”; mô tả tác nhân Ch.3 §II giao “Quản lý sản phẩm, dòng sản phẩm, danh mục và sản phẩm lỗi” cho Nhân viên quản lý. Ba nguồn không thống nhất; tài liệu này giữ nguyên cả ba và **không chọn** tác nhân. Xem [C-03 — Tác nhân UC008–UC011 không thống nhất](../requirements/scope-and-requirements.md#c-03--tác-nhân-uc008uc011-không-thống-nhất).

> **C-12 — Mã định danh nhập tay hay tự sinh:** Bước 3 của luồng chính yêu cầu “Nhập mã danh mục.”, trong khi Quy ước chung Ch.4 §I.3, `RB02`, `RB03` và XBR-14 quy định mã định danh **tự sinh** dạng `TIỀN_TỐ-YYYY-NNNNNN`. Hai nguồn chưa thống nhất; tài liệu này giữ nguyên bước nguồn và **không chọn** cơ chế cấp mã. Xem [C-12 — Nhập mã định danh so với mã tự sinh bất biến](../requirements/scope-and-requirements.md#c-12--nhập-mã-định-danh-so-với-mã-tự-sinh-bất-biến).

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm danh mục sản phẩm |
| Mục đích: | Tạo danh mục để phân nhóm các dòng sản phẩm. |
| Mô tả: | Nhân viên nhập mã, tên và mô tả để tạo danh mục sản phẩm mới. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. |
| Điều kiện sau: | Thành công: Danh mục mới được lưu. Thất bại: Không tạo danh mục mới. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm danh mục sản phẩm”. |  |
|  | 2. Hiển thị biểu mẫu thêm danh mục. |
| 3. Nhập mã danh mục. |  |
| 4. Nhập tên danh mục. |  |
| 5. Nhập mô tả danh mục. |  |
| 6. Chọn lưu. |  |
|  | 7. Kiểm tra tính đầy đủ và hợp lệ của thông tin. |
|  | 8. Kiểm tra tính duy nhất của mã danh mục. |
|  | 9. Lưu Danh mục sản phẩm mới. |
|  | 10. Hiển thị thông báo thêm danh mục thành công. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 7.a.1. Phát hiện thiếu mã hoặc tên danh mục. |
|  | 7.a.2. Hiển thị thông báo yêu cầu nhập đầy đủ thông tin. |
| 7.a.3. Bổ sung thông tin còn thiếu. |  |
| 7.a.4. Chọn lưu. |  |
|  | 7.a.5. Tiếp tục bước 7. |
|  | 8.a.1. Phát hiện mã danh mục đã tồn tại. |
|  | 8.a.2. Hiển thị thông báo trùng mã danh mục. |
|  | 8.a.3. Quay lại bước 3. |
|  | 9.a.1. Phát hiện lỗi khi lưu danh mục. |
|  | 9.a.2. Thông báo thêm danh mục thất bại. |
|  | 9.a.3. Giữ thông tin đã nhập để nhân viên thử lại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Đã đối chiếu toàn bộ 31 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 10 bước luồng chính và 11 bước luồng phụ.
- Không có sơ đồ riêng cho UC010 trong DOCX.

Trước: [UC009 – Thêm dòng sản phẩm](UC009-product-line-management.md) — Sau: [UC011 – Thêm sản phẩm lỗi](UC011-defective-product-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
