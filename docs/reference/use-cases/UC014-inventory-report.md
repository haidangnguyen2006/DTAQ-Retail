---
title: "UC014 – Báo cáo sản phẩm tồn kho lâu ngày"
uc_id: "UC014"
catalog_title: "Báo cáo sản phẩm tồn kho hằng ngày"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC014 – Báo cáo sản phẩm tồn kho lâu ngày"
page: "Chưa xác minh"
---

# UC014 – Báo cáo sản phẩm tồn kho lâu ngày

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC014 – Báo cáo sản phẩm tồn kho lâu ngày”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh. Định vị bằng Chương 3, mục IV, heading UC và nhãn bảng/sơ đồ tương ứng.

> **Phân biệt tên gọi:** Trong danh sách use case tại Chương 3, mục III, UC014 có tên “Báo cáo sản phẩm tồn kho hằng ngày”. Heading đặc tả tại Chương 3, mục IV là “UC014 – Báo cáo sản phẩm tồn kho lâu ngày”. DOCX dùng hai tên khác nhau; tài liệu này giữ nguyên cả hai, không chuẩn hóa và không tự chọn một tên thay thế. Xung đột tên gọi được ghi nhận tại [C-01 — Tiêu đề UC014](../requirements/scope-and-requirements.md#c-01--tiêu-đề-uc014).

## Cảnh báo chưa được giải quyết

> **C-01 — Tiêu đề UC014:** danh mục Ch.3 §III ghi “Báo cáo sản phẩm tồn kho **hằng ngày**”, còn tiêu đề đặc tả chi tiết Ch.3 §IV và FR014 ghi “**lâu ngày**”. Nguồn chưa thống nhất tên chuẩn; tài liệu này giữ nguyên cả hai và không tự đổi ID. Xem [C-01 — Tiêu đề UC014](../requirements/scope-and-requirements.md#c-01--tiêu-đề-uc014).
>
> **BR-11 / XBR-12 — ngưỡng “trên 60 ngày” là mốc tham chiếu, không phải mặc định:** [BR-11](../requirements/scope-and-requirements.md#5-quy-tắc-nghiệp-vụ-chính) và [XBR-12](../business-rules/rules-and-exceptions.md#2-quy-tắc-cắt-ngang-nhiều-tính-năng) ghi ngưỡng tham chiếu **trên 60 ngày**. FR014 nêu ngưỡng này với tính chất tham khảo. Đặc tả UC014 để Quản lý **tự nhập ngưỡng số ngày tồn kho** (“Nhập ngưỡng số ngày tồn kho”), và nguồn **không quy định giá trị mặc định**. Vì vậy đây là hai dữ kiện tương thích: 60 ngày là mốc tham khảo, **không phải** ngưỡng cố định đã chốt và **không phải** giá trị mặc định. Xem thêm [FR014](../requirements/scope-and-requirements.md#fr014--báo-cáo-hàng-tồn-kho-lâu-ngày).

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Tên trong danh sách use case | Báo cáo sản phẩm tồn kho hằng ngày (Chương 3, mục III) |
| Heading đặc tả | UC014 – Báo cáo sản phẩm tồn kho lâu ngày (Chương 3, mục IV) |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC014 – Báo cáo sản phẩm tồn kho lâu ngày”, trước tiểu mục “a. Sequence diagram” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | [Danh mục use case](catalog.md) — Chương 3, mục III (UC001–UC019) |
| Quy tắc / xung đột liên quan | [BR-11](../requirements/scope-and-requirements.md#5-quy-tắc-nghiệp-vụ-chính), [XBR-12](../business-rules/rules-and-exceptions.md#2-quy-tắc-cắt-ngang-nhiều-tính-năng), [C-01](../requirements/scope-and-requirements.md#c-01--tiêu-đề-uc014) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC014 |
| Tiêu đề bảng (nguyên văn) | Use case: Báo cáo sản phẩm tồn kho lâu ngày |
| Mục đích: | Xác định máy tồn kho lâu để hỗ trợ kế hoạch kinh doanh. |
| Mô tả: | Quản lý chọn ngưỡng lưu kho để xem máy tồn lâu theo ngày nhập và trạng thái hiện tại. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. Máy có thông tin ngày nhập kho. |
| Điều kiện sau: | Hiển thị báo cáo; dữ liệu tồn kho không thay đổi. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Báo cáo sản phẩm tồn kho lâu ngày”. |  |
|  | 2. Hiển thị bộ lọc báo cáo. |
| 3. Nhập ngưỡng số ngày tồn kho. |  |
| 4. Chọn xem báo cáo. |  |
|  | 5. Kiểm tra ngưỡng tồn kho. |
|  | 6. Xác định các máy còn tồn tại thời điểm báo cáo. |
|  | 7. Tính số ngày tồn từ ngày nhập kho của từng máy. |
|  | 8. Lọc các máy vượt ngưỡng được chọn. |
|  | 9. Hiển thị dòng sản phẩm, serial, ngày nhập, trạng thái và số ngày tồn. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 5.a.1. Phát hiện ngưỡng số ngày tồn kho không hợp lệ. |
|  | 5.a.2. Hiển thị thông báo yêu cầu điều chỉnh. |
|  | 5.a.3. Quay lại bước 3. |
|  | 8.a.1. Không có máy vượt ngưỡng tồn kho. |
|  | 8.a.2. Hiển thị báo cáo không có dữ liệu phù hợp. |
| 9.a.1. Chọn dòng sản phẩm cần xem chi tiết. |  |
|  | 9.a.2. Hiển thị các máy thuộc dòng sản phẩm được chọn. |

## Sơ đồ

Nguồn: Chương 3, mục IV, `UC014`, các tiểu mục “a. Sequence diagram” và “b. Activity diagram” nằm ngay sau bảng đặc tả trong DOCX.

### a. Sequence diagram

- Định vị nguồn: Chương 3, mục IV, `UC014`, tiểu mục “a. Sequence diagram”.
- Asset trong DOCX: `word/media/image33.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image33.png](../assets/diagrams/image33.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

### b. Activity diagram

- Định vị nguồn: Chương 3, mục IV, `UC014`, tiểu mục “b. Activity diagram”.
- Asset trong DOCX: `word/media/image31.png`, liệt kê tại [ui/screen-flow-and-screens.md §4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3); bản sao byte-identical: [diagrams/image31.png](../assets/diagrams/image31.png) (xem [manifest](../assets/manifest.md)).
- Hình nhúng trong DOCX — không vẽ lại, không mô tả nội dung hình trong tài liệu này.

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Các ràng buộc nghiệp vụ rút trực tiếp:
  - Chỉ Nhân viên quản lý đã đăng nhập đúng quyền mới được thực hiện chức năng.
  - Máy phải có thông tin ngày nhập kho để tính số ngày tồn.
  - Báo cáo chỉ hiển thị dữ liệu; dữ liệu tồn kho không được thay đổi sau khi xem báo cáo.
  - Ngưỡng số ngày tồn kho phải hợp lệ; nếu không hợp lệ, hệ thống yêu cầu điều chỉnh và quay lại bước nhập ngưỡng.
  - Báo cáo chỉ xét các máy còn tồn tại tại thời điểm báo cáo; số ngày tồn được tính từ ngày nhập kho của từng máy.
  - Kết quả phải hiển thị dòng sản phẩm, serial, ngày nhập, trạng thái và số ngày tồn.
  - Nếu không có máy vượt ngưỡng, hệ thống phải hiển thị báo cáo không có dữ liệu phù hợp.
  - Khi chọn một dòng sản phẩm, hệ thống phải hiển thị các máy thuộc dòng sản phẩm đó.
- Bảng gốc không có mục “Error Flows” riêng; các nhánh xử lý không hợp lệ và không có dữ liệu nằm trong Luồng sự kiện phụ.
- Trong phạm vi đặc tả UC014, nguồn không ấn định một giá trị ngưỡng mặc định hoặc giá trị ngưỡng cụ thể; ngưỡng do người dùng nhập. Nguồn cũng không quy định định dạng hiển thị của ngày nhập và số ngày tồn.
- Ngưỡng tham chiếu “trên 60 ngày” của [FR014](../requirements/scope-and-requirements.md#fr014--báo-cáo-hàng-tồn-kho-lâu-ngày), [BR-11](../requirements/scope-and-requirements.md#5-quy-tắc-nghiệp-vụ-chính) và [XBR-12](../business-rules/rules-and-exceptions.md#2-quy-tắc-cắt-ngang-nhiều-tính-năng) không phải giá trị mặc định; xem [Cảnh báo chưa được giải quyết](#cảnh-báo-chưa-được-giải-quyết).
- Hai sơ đồ chỉ tồn tại dưới dạng hình nhúng trong DOCX; tài liệu này không vẽ lại, không suy diễn nội dung sơ đồ.

Trước: [UC013 – Thêm nhân viên bán hàng](UC013-sales-staff-management.md) — Sau: [UC015 – Thống kê doanh số theo sản phẩm](UC015-sales-by-product.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
