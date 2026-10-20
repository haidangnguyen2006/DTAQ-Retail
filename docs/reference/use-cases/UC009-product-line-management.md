---
title: "UC009 – Thêm dòng sản phẩm"
uc_id: "UC009"
catalog_title: "Quản lý dòng sản phẩm"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC009 – Thêm dòng sản phẩm"
page: "Chưa xác minh"
---

# UC009 – Thêm dòng sản phẩm

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC009 – Thêm dòng sản phẩm”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng đặc tả tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC009 có tên “Quản lý dòng sản phẩm”. Heading đặc tả tại Chương 3, mục IV là “UC009 – Thêm dòng sản phẩm”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý dòng sản phẩm (Chương 3, mục III) |
| Heading đặc tả | UC009 – Thêm dòng sản phẩm (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC009 – Thêm dòng sản phẩm”, trước heading UC010 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Cảnh báo nguồn và quyết định implementation

> **C-03 — Tác nhân không thống nhất:** Danh mục Ch.3 §III ghi tác nhân “Quản lý”; đặc tả Ch.3 §IV ghi tác nhân “Nhân viên bán hàng.”; mô tả tác nhân Ch.3 §II giao “Quản lý sản phẩm, dòng sản phẩm, danh mục và sản phẩm lỗi” cho Nhân viên quản lý. Ba nguồn không thống nhất; tài liệu này giữ nguyên cả ba và **không chọn** tác nhân. Xem [C-03 — Tác nhân UC008–UC011 không thống nhất](../requirements/scope-and-requirements.md#c-03--tác-nhân-uc008uc011-không-thống-nhất).

> **C-12 — Mã định danh (đã giải quyết cho implementation hiện tại):** Bước 3 của luồng chính giữ nguyên văn “Nhập mã và tên dòng sản phẩm.” như bằng chứng nguồn. Quyết định implementation (đã duyệt 2026-10-20): mã định danh do **ứng dụng sinh** khi lưu mới thành công, giữ đúng định dạng `TIỀN_TỐ-YYYY-NNNNNN`; V1/PostgreSQL không dùng sequence, identity hay default. Xung đột với quy ước mã tự sinh tại Ch.4 §I.3, `RB02`, `RB03`, XBR-14 **vẫn tồn tại trong nguồn** và không bị sửa. Xem [C-12 — Nhập mã định danh so với mã tự sinh bất biến](../requirements/scope-and-requirements.md#c-12--nhập-mã-định-danh-so-với-mã-tự-sinh-bất-biến).

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm dòng sản phẩm |
| Mục đích: | Tạo thông tin chung cho các máy thuộc cùng dòng sản phẩm. |
| Mô tả: | Nhân viên nhập thông tin dòng sản phẩm và liên kết với danh mục tương ứng. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. Danh mục đã tồn tại. Giá và thuế khởi tạo đã được quản lý phê duyệt. |
| Điều kiện sau: | Thành công: Tạo dòng sản phẩm mới; chưa phát sinh máy hoặc tồn kho từ riêng thao tác này. Thất bại: Không tạo dòng sản phẩm mới. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm dòng sản phẩm”. |  |
|  | 2. Hiển thị biểu mẫu thêm dòng sản phẩm. |
| 3. Nhập mã và tên dòng sản phẩm. |  |
| 4. Chọn danh mục sản phẩm. |  |
| 5. Nhập đơn vị tính. |  |
| 6. Nhập giá bán trước thuế và thuế suất đã được phê duyệt. |  |
| 7. Chọn lưu. |  |
|  | 8. Kiểm tra tính đầy đủ và hợp lệ của dữ liệu. |
|  | 9. Kiểm tra tính duy nhất của mã dòng sản phẩm. |
|  | 10. Lưu dòng sản phẩm với số lượng tồn ban đầu bằng 0. |
|  | 11. Hiển thị thông báo thêm dòng sản phẩm thành công. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 8.a.1. Phát hiện thông tin bắt buộc bị thiếu hoặc giá, thuế không hợp lệ. |
|  | 8.a.2. Hiển thị lỗi nhập liệu. |
| 8.a.3. Điều chỉnh thông tin dòng sản phẩm. |  |
| 8.a.4. Chọn lưu. |  |
|  | 8.a.5. Tiếp tục bước 8. |
|  | 9.a.1. Phát hiện mã dòng sản phẩm đã tồn tại. |
|  | 9.a.2. Hiển thị thông báo trùng mã. |
|  | 9.a.3. Quay lại bước 3. |
|  | 10.a.1. Phát hiện lỗi khi lưu dòng sản phẩm. |
|  | 10.a.2. Thông báo thêm dòng sản phẩm thất bại. |
|  | 10.a.3. Giữ thông tin đã nhập để nhân viên thử lại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Đã đối chiếu toàn bộ 32 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 11 bước luồng chính và 11 bước luồng phụ.
- Không có sơ đồ riêng cho UC009 trong DOCX.

Trước: [UC008 – Thêm sản phẩm](UC008-product-management.md) — Sau: [UC010 – Thêm danh mục sản phẩm](UC010-product-category-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
