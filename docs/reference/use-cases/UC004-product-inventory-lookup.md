---
title: "UC004 – Tra cứu sản phẩm và tồn kho"
uc_id: "UC004"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC004 – Tra cứu sản phẩm và tồn kho"
page: "Chưa xác minh"
---

# UC004 – Tra cứu sản phẩm và tồn kho

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC004 – Tra cứu sản phẩm và tồn kho”.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC004 – Tra cứu sản phẩm và tồn kho |
| Bảng đặc tả | Bảng use case đặt ngay sau heading “UC004 – Tra cứu sản phẩm và tồn kho” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | Chương 3, mục III — UC001–UC019; xem [catalog.md](catalog.md) |
| Yêu cầu chức năng liên quan | FR006 — **ứng viên — chưa xác nhận** ([truy vết §3](../traceability.md#3-truy-vết-yêu-cầu-chức-năng)) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC004 |
| Tiêu đề bảng (nguyên văn) | Use case: Tra cứu sản phẩm và tồn kho |
| Mục đích: | Tìm thông tin sản phẩm và xác định máy có thể bán. |
| Mô tả: | Nhân viên tra cứu theo dòng sản phẩm, danh mục hoặc serial để xem thông tin và tồn kho. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. |
| Điều kiện sau: | Hiển thị kết quả tra cứu; dữ liệu sản phẩm và tồn kho không thay đổi. |

## Luồng sự kiện chính (Basic flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Tra cứu sản phẩm và tồn kho”. |  |
|  | 2. Hiển thị màn hình tra cứu. |
| 3. Nhập điều kiện tìm kiếm theo dòng sản phẩm, danh mục hoặc serial. |  |
| 4. Chọn tìm kiếm |  |
|  | 5. Tìm dữ liệu phù hợp. |
|  | 6. Hiển thị danh sách kết quả. |
| 7. Chọn sản phẩm cần xem |  |
|  | 8. Hiển thị thông tin dòng sản phẩm và các máy liên quan. |
| 9. Chọn kiểm tra máy có thể bán. |  |
|  | 10. Hiển thị danh sách máy sẵn sàng bán. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 5.a.1. Không tìm thấy dữ liệu phù hợp. |
|  | 5.a.2. Hiển thị thông báo không có kết quả. |
| 5.a.3. Thay đổi điều kiện tra cứu. |  |
|  | 5.a.4. Tiếp tục bước 4. |
|  | 10.a.1. Không có máy sẵn sàng bán. |
|  | 10.a.2. Hiển thị thông báo không có máy đủ điều kiện bán. |

## Sơ đồ

Hai tiểu mục sau nằm ngay sau bảng đặc tả trong DOCX. Bản sao hình nằm trong [assets/diagrams](../assets/diagrams/) theo bảng ánh xạ tại [Phân luồng màn hình và thiết kế màn hình — mục 4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3). Tài liệu này không diễn giải nội dung hình.

### a. Sequence diagram

![UC004 — a. Sequence diagram, bản sao từ word/media/image24.png](../assets/diagrams/image24.png)

- Bản sao: [`docs/reference/assets/diagrams/image24.png`](../assets/diagrams/image24.png)

### b. Activity diagram

![UC004 — b. Activity diagram, bản sao từ word/media/image28.png](../assets/diagrams/image28.png)

- Bản sao: [`docs/reference/assets/diagrams/image28.png`](../assets/diagrams/image28.png)

## Ghi chú biên tập

- Bản trích giữ nguyên nội dung bảng đặc tả của DOCX.
- Bảng gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của luồng sự kiện.
- Hai sơ đồ nằm dưới dạng hình nhúng trong DOCX nguồn và bản sao byte-identical đã được liên kết trong tài liệu này tại `docs/reference/assets/diagrams/`; tài liệu này không vẽ lại và không diễn giải nội dung trực quan của sơ đồ.
- **Mâu thuẫn cục bộ liên quan:** mức hạt tồn kho (số lượng so với serial từng máy) — xem [C-07](../requirements/scope-and-requirements.md#c-07--mô-hình-tồn-kho-serial-hay-số-lượng). Tài liệu giữ nguyên câu chữ DOCX, không tự chọn bên.

Trước: [UC003 – Lập hóa đơn bán lẻ](UC003-retail-invoice.md) — Sau: [UC005 – Tra cứu khách hàng và giao dịch](UC005-customer-transaction-lookup.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
