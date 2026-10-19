---
title: "UC008 – Thêm sản phẩm"
uc_id: "UC008"
catalog_title: "Quản lý sản phẩm"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC008 – Thêm sản phẩm"
page: "Chưa xác minh"
---

# UC008 – Thêm sản phẩm

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC008 – Thêm sản phẩm”, bảng use case đặt ngay sau heading, tiếp theo là hai tiểu mục sơ đồ.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC008 có tên “Quản lý sản phẩm”. Heading đặc tả tại Chương 3, mục IV là “UC008 – Thêm sản phẩm”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Quản lý sản phẩm (Chương 3, mục III) |
| Heading đặc tả | UC008 – Thêm sản phẩm (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC008 – Thêm sản phẩm”, trước tiểu mục “a. Sequence diagram” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh mục use case | [catalog.md — Đăng ký use case](catalog.md#1-đăng-ký-use-case) |

## Cảnh báo chưa được giải quyết

> **C-03 — Tác nhân không thống nhất:** Danh mục Ch.3 §III ghi tác nhân “Quản lý”; đặc tả Ch.3 §IV ghi tác nhân “Nhân viên bán hàng.”; mô tả tác nhân Ch.3 §II giao “Quản lý sản phẩm, dòng sản phẩm, danh mục và sản phẩm lỗi” cho Nhân viên quản lý. Ba nguồn không thống nhất; tài liệu này giữ nguyên cả ba và **không chọn** tác nhân. Xem [C-03 — Tác nhân UC008–UC011 không thống nhất](../requirements/scope-and-requirements.md#c-03--tác-nhân-uc008uc011-không-thống-nhất).

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Use case | Thêm sản phẩm |
| Mục đích: | Ghi nhận một máy mới theo số serial vào hệ thống. |
| Mô tả: | Nhân viên chọn dòng sản phẩm và nhập thông tin từng máy thực tế được tiếp nhận vào kho. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. Dòng sản phẩm tương ứng đã tồn tại. Máy đã được kiểm tra thực tế khi tiếp nhận. |
| Điều kiện sau: | Thành công: Tạo sản phẩm với serial duy nhất và cập nhật tồn kho phù hợp. Thất bại: Không tạo máy mới hoặc thay đổi tồn kho. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Thêm sản phẩm”. |  |
|  | 2. Hiển thị biểu mẫu thêm máy. |
| 3. Chọn dòng sản phẩm. |  |
| 4. Nhập số serial của máy. |  |
| 5. Nhập ngày nhập kho. |  |
| 6. Ghi nhận tình trạng thực tế của máy. |  |
| 7. Chọn lưu. |  |
|  | 8. Kiểm tra thông tin máy và dòng sản phẩm được chọn. |
|  | 9. Kiểm tra tính duy nhất của số serial. |
|  | 10. Lưu sản phẩm gắn với dòng sản phẩm. |
|  | 11. Cập nhật số lượng tồn theo máy vừa tiếp nhận. |
|  | 12. Hiển thị thông báo thêm sản phẩm thành công. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 8.a.1. Phát hiện thiếu thông tin bắt buộc hoặc ngày nhập kho không hợp lệ. |
|  | 8.a.2. Hiển thị lỗi nhập liệu. |
| 8.a.3. Điều chỉnh thông tin máy. |  |
| 8.a.4. Chọn lưu. |  |
|  | 8.a.5. Tiếp tục bước 8. |
|  | 9.a.1. Phát hiện số serial đã tồn tại. |
|  | 9.a.2. Thông báo không thể tạo máy trùng serial. |
|  | 9.a.3. Quay lại bước 4. |
|  | 10.a.1. Xác định máy tiếp nhận có tình trạng lỗi. |
|  | 10.a.2. Lưu máy ở trạng thái không sẵn sàng bán. |
|  | 10.a.3. Chuyển sang [UC011](UC011-defective-product-management.md) để ghi nhận hồ sơ lỗi. |
|  | 10.b.1. Phát hiện lỗi khi lưu máy. |
|  | 10.b.2. Hủy các thay đổi của lần thêm hiện tại. |
|  | 10.b.3. Thông báo thêm sản phẩm thất bại. |

## Sơ đồ

Hai tiểu mục sau nằm ngay sau bảng đặc tả trong DOCX. Bản sao hình nằm trong [assets/diagrams](../assets/diagrams/) theo bảng ánh xạ tại [Phân luồng màn hình và thiết kế màn hình — mục 4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3). Tài liệu này không diễn giải nội dung hình.

### a. Sequence diagram

![UC008 — a. Sequence diagram, bản sao từ word/media/image34.png](../assets/diagrams/image34.png)

- Bản sao: [`docs/reference/assets/diagrams/image34.png`](../assets/diagrams/image34.png)
- Tệp hình trong DOCX: `word/media/image34.png`.

### b. Activity diagram

![UC008 — b. Activity diagram, bản sao từ word/media/image29.png](../assets/diagrams/image29.png)

- Bản sao: [`docs/reference/assets/diagrams/image29.png`](../assets/diagrams/image29.png)
- Tệp hình trong DOCX: `word/media/image29.png`.

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Bước 10.a.3 tham chiếu trực tiếp “UC011”; tài liệu này liên kết đến [UC011 – Thêm sản phẩm lỗi](UC011-defective-product-management.md) mà không thêm ánh xạ yêu cầu chức năng nào.
- Đã đối chiếu toàn bộ 36 dòng của bảng gốc: 1 dòng “Use case”, 5 trường thông tin chung, 4 dòng tiêu đề luồng, 12 bước luồng chính và 14 bước luồng phụ.
- Hai sơ đồ được DOCX nguồn nhúng dưới dạng hình; tài liệu này liên kết tới các bản sao byte-identical tại `docs/reference/assets/diagrams/` (`image34.png`, `image29.png`), không vẽ lại và không diễn giải trực quan nội dung sơ đồ.

Trước: [UC007 – Thêm khách hàng](UC007-customer-management.md) — Sau: [UC009 – Thêm dòng sản phẩm](UC009-product-line-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
