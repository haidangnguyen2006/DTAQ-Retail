---
title: "UC005 – Tra cứu khách hàng và giao dịch"
uc_id: "UC005"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC005 – Tra cứu khách hàng và giao dịch"
page: "Chưa xác minh"
---

# UC005 – Tra cứu khách hàng và giao dịch

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC005 – Tra cứu khách hàng và giao dịch”.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC005 – Tra cứu khách hàng và giao dịch |
| Bảng đặc tả | Bảng use case đặt ngay sau heading “UC005 – Tra cứu khách hàng và giao dịch” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | Chương 3, mục III — UC001–UC019; xem [catalog.md](catalog.md) |
| Yêu cầu chức năng liên quan | FR004 — **ứng viên — chưa xác nhận** ([truy vết §3](../traceability.md#3-truy-vết-yêu-cầu-chức-năng)) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC005 |
| Tiêu đề bảng (nguyên văn) | Use case: Tra cứu khách hàng và giao dịch |
| Mục đích: | Tra cứu khách hàng và chứng từ giao dịch liên quan. |
| Mô tả: | Nhân viên tìm khách hàng để xem lịch sử mua hàng, hóa đơn và phiếu đổi/trả. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. |
| Điều kiện sau: | Hiển thị thông tin khách hàng và giao dịch; dữ liệu không thay đổi. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Tra cứu khách hàng và giao dịch”. |  |
|  | 2. Hiển thị màn hình tra cứu. |
| 3. Nhập số điện thoại hoặc thông tin nhận diện khách hàng. |  |
| 4. Chọn tìm kiếm. |  |
|  | 5. Tìm khách hàng phù hợp. |
|  | 6. Hiển thị kết quả tra cứu. |
| 7. Chọn khách hàng cần xem. |  |
|  | 8. Hiển thị thông tin khách hàng và lịch sử mua hàng. |
| 9. Chọn hóa đơn cần xem. |  |
|  | 10. Hiển thị chi tiết hóa đơn gốc. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 5.a.1. Không tìm thấy khách hàng phù hợp. |
|  | 5.a.2. Hiển thị thông báo không có kết quả. |
| 5.a.3. Điều chỉnh thông tin tìm kiếm. |  |
|  | 5.a.4. Tiếp tục bước 4. |
| 9.a.1. Chọn xem phiếu đổi/trả liên quan. |  |
|  | 9.a.2. Hiển thị danh sách phiếu đổi và phiếu trả. |
| 9.a.3. Chọn một phiếu. |  |
|  | 9.a.4. Hiển thị nội dung và trạng thái xử lý của phiếu. |
|  | 9.b.1. Không có phiếu đổi/trả liên quan. |
|  | 9.b.2. Hiển thị danh sách phiếu trống. |

## Sơ đồ

Hai tiểu mục sau nằm ngay sau bảng đặc tả trong DOCX. Bản sao hình nằm trong [assets/diagrams](../assets/diagrams/) theo bảng ánh xạ tại [Phân luồng màn hình và thiết kế màn hình — mục 4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3). Tài liệu này không diễn giải nội dung hình.

### a. Sequence diagram

![UC005 — a. Sequence diagram, bản sao từ word/media/image25.png](../assets/diagrams/image25.png)

- Bản sao: [`docs/reference/assets/diagrams/image25.png`](../assets/diagrams/image25.png)

### b. Activity diagram

![UC005 — b. Activity diagram, bản sao từ word/media/image36.png](../assets/diagrams/image36.png)

- Bản sao: [`docs/reference/assets/diagrams/image36.png`](../assets/diagrams/image36.png)

## Ghi chú biên tập

- Bản trích giữ nguyên nội dung bảng đặc tả của DOCX.
- Bảng gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của luồng sự kiện.
- Hai sơ đồ nằm dưới dạng hình nhúng trong DOCX nguồn và bản sao byte-identical đã được liên kết trong tài liệu này tại `docs/reference/assets/diagrams/`; tài liệu này không vẽ lại và không diễn giải nội dung trực quan của sơ đồ.

Trước: [UC004 – Tra cứu sản phẩm và tồn kho](UC004-product-inventory-lookup.md) — Sau: [UC006 – Xử lý đổi trả sản phẩm](UC006-return-exchange.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
