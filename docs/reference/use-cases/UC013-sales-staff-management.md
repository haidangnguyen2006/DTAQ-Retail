---
title: "UC013 – Thêm nhân viên bán hàng"
uc_id: "UC013"
catalog_title: "Quản lý nhân viên bán hàng"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC013 – Thêm nhân viên bán hàng"
page: "Chưa xác minh"
---

# UC013 – Thêm nhân viên bán hàng

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC013 – Thêm nhân viên bán hàng”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng đặc tả tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC013 có tên “Quản lý nhân viên bán hàng”. Heading đặc tả tại Chương 3, mục IV là “UC013 – Thêm nhân viên bán hàng”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý nhân viên bán hàng (Chương 3, mục III) |
| Heading đặc tả | UC013 – Thêm nhân viên bán hàng (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC013 – Thêm nhân viên bán hàng”, trước heading UC014 |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Cảnh báo chưa được giải quyết

> **C-12 — Mã định danh nhập tay hay tự sinh:** Bước 3 của luồng chính yêu cầu “Nhập mã, họ tên và thông tin liên hệ.”, trong khi Quy ước chung Ch.4 §I.3, `RB02`, `RB03` và XBR-14 quy định mã định danh **tự sinh** dạng `TIỀN_TỐ-YYYY-NNNNNN`. Hai nguồn chưa thống nhất; tài liệu này giữ nguyên bước nguồn và **không chọn** cơ chế cấp mã. Xem [C-12 — Nhập mã định danh so với mã tự sinh bất biến](../requirements/scope-and-requirements.md#c-12--nhập-mã-định-danh-so-với-mã-tự-sinh-bất-biến).

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm nhân viên bán hàng |
| Mục đích: | Tạo hồ sơ và tài khoản cho nhân viên bán hàng mới. |
| Mô tả: | Quản lý nhập thông tin cá nhân và tài khoản để tạo nhân viên có quyền bán hàng. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. |
| Điều kiện sau: | Thành công: Tạo nhân viên và tài khoản hoạt động với quyền dành cho nhân viên bán hàng. Thất bại: Không tạo hồ sơ hoặc tài khoản mới. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm nhân viên bán hàng”. |  |
|  | 2. Hiển thị biểu mẫu thêm nhân viên. |
| 3. Nhập mã, họ tên và thông tin liên hệ. |  |
| 4. Nhập ngày vào làm. |  |
| 5. Nhập tên đăng nhập. |  |
| 6. Nhập mật khẩu khởi tạo. |  |
| 7. Chọn lưu. |  |
|  | 8. Kiểm tra tính đầy đủ và hợp lệ của dữ liệu. |
|  | 9. Kiểm tra tính duy nhất của mã nhân viên và tên đăng nhập. |
|  | 10. Lưu nhân viên với quyền nhân viên bán hàng. |
|  | 11. Thiết lập tài khoản ở trạng thái hoạt động. |
|  | 12. Hiển thị thông báo thêm nhân viên thành công. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 8.a.1. Phát hiện thiếu thông tin bắt buộc hoặc dữ liệu không hợp lệ. |
|  | 8.a.2. Hiển thị lỗi nhập liệu. |
| 8.a.3. Điều chỉnh thông tin nhân viên. |  |
| 8.a.4. Chọn lưu. |  |
|  | 8.a.5. Tiếp tục bước 8. |
|  | 9.a.1. Phát hiện mã nhân viên đã tồn tại. |
|  | 9.a.2. Hiển thị thông báo trùng mã nhân viên. |
|  | 9.a.3. Quay lại bước 3. |
|  | 9.b.1. Phát hiện tên đăng nhập đã tồn tại. |
|  | 9.b.2. Hiển thị thông báo trùng tên đăng nhập. |
|  | 9.b.3. Quay lại bước 5. |
|  | 10.a.1. Phát hiện lỗi khi lưu nhân viên. |
|  | 10.a.2. Hủy các thay đổi của lần thêm hiện tại. |
|  | 10.a.3. Thông báo thêm nhân viên thất bại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Đã đối chiếu toàn bộ 36 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 12 bước luồng chính và 14 bước luồng phụ.
- Không có sơ đồ riêng cho UC013 trong DOCX.

Trước: [UC012 – Thêm khuyến mãi](UC012-promotion-management.md) — Sau: [UC014 – Báo cáo sản phẩm tồn kho lâu ngày](UC014-inventory-report.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
