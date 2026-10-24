---
title: "UC003 – Lập hóa đơn bán lẻ"
uc_id: "UC003"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC003 – Lập hóa đơn bán lẻ"
page: "Chưa xác minh"
---

# UC003 – Lập hóa đơn bán lẻ

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC003 – Lập hóa đơn bán lẻ”.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC003 – Lập hóa đơn bán lẻ |
| Bảng đặc tả | Bảng use case đặt ngay sau heading “UC003 – Lập hóa đơn bán lẻ” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | Chương 3, mục III — UC001–UC019; xem [catalog.md](catalog.md) |
| Yêu cầu chức năng liên quan | FR007 — **ứng viên — chưa xác nhận** ([truy vết §3](../traceability.md#3-truy-vết-yêu-cầu-chức-năng)) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC003 |
| Tiêu đề bảng (nguyên văn) | Use case: Lập hóa đơn bán lẻ |
| Mục đích: | Ghi nhận giao dịch bán hàng và phát hành hóa đơn. |
| Mô tả: | Nhân viên chọn khách hàng và máy cần bán. Hệ thống tính tiền và hoàn tất hóa đơn sau khi nhận đủ tiền mặt. |
| Tác nhân: | Nhân viên bán hàng |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. Khách hàng và sản phẩm cần bán đã được ghi nhận. |
| Điều kiện sau: | Thành công: Lưu hóa đơn, chi tiết, giá, thuế và ưu đãi thực tế; cập nhật trạng thái máy và tồn kho. Thất bại: Không ghi nhận hóa đơn đã thanh toán; không thay đổi tồn kho. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Lập hóa đơn bán lẻ”. |  |
|  | Hiển thị màn hình lập hóa đơn. |
| 3. Chọn khách hàng mua hàng. |  |
| 4. Chọn dòng sản phẩm và số lượng |  |
|  | 5. Kiểm tra trạng thái kinh doanh và số lượng tồn. |
|  | 6. Lấy đơn giá trước thuế và thuế suất đang áp dụng. |
|  | 7. Kiểm tra chương trình khuyến mãi còn hiệu lực. |
|  | 8. Áp dụng mức khuyến mãi hợp lệ cho từng dòng sản phẩm. |
|  | 9. Tính tiền hàng, tiền giảm, tiền thuế và tổng tiền phải thanh toán. |
|  | 10. Hiển thị hóa đơn nháp. |
| 11. Nhập số tiền mặt khách thanh toán. |  |
| 12. Chọn hoàn tất thanh toán. |  |
|  | 13. Kiểm tra số tiền nhận và khả năng bán hiện tại của các máy. |
|  | 14. Lưu hóa đơn và chi tiết giao dịch. |
|  | 15. Cập nhật trạng thái máy và số lượng tồn kho. |
|  | 16. Hiển thị hóa đơn đã thanh toán. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 5.a.1. Phát hiện máy không sẵn sàng bán hoặc dòng sản phẩm đã ngừng kinh doanh hoặc số lượng tồn không đủ. |
|  | 5.a.2. Hiển thị mặt hàng không thể bán. |
| 5.a.3. Chọn lại máy hoặc loại mặt hàng không hợp lệ. |  |
|  | 5.a.4. Tiếp tục bước 5. |
|  | 7.a.1. Không tìm thấy khuyến mãi hợp lệ. |
|  | 7.a.2. Giữ nguyên giá bán hiện hành. |
|  | 7.a.3. Tiếp tục bước 9. |
| 10.a.1. Chọn hủy hóa đơn nháp. |  |
|  | 10.a.2. Hiển thị yêu cầu xác nhận hủy. |
| 10.a.3. Xác nhận hủy hóa đơn nháp. |  |
|  | 10.a.4. Hủy hóa đơn nháp mà không thay đổi tồn kho. |
|  | 13.a.1. Phát hiện số tiền nhận chưa đủ. |
|  | 13.a.2. Hiển thị thông báo chưa thanh toán đủ. |
|  | 13.a.3. Quay lại bước 11. |
|  | 13.b.1. Phát hiện máy không còn đủ điều kiện bán. |
|  | 13.b.2. Từ chối hoàn tất hóa đơn. |
|  | 13.b.3. Quay lại bước 4. |
|  | 14.a.1. Phát hiện lỗi khi lưu giao dịch. |
|  | 14.a.2. Khôi phục dữ liệu về trước khi hoàn tất giao dịch. |
|  | 14.a.3. Thông báo chưa thể hoàn tất hóa đơn. |

## Sơ đồ

Hai tiểu mục sau nằm ngay sau bảng đặc tả trong DOCX. Bản sao hình nằm trong [assets/diagrams](../assets/diagrams/) theo bảng ánh xạ tại [Phân luồng màn hình và thiết kế màn hình — mục 4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3). Tài liệu này không diễn giải nội dung hình.

### a. Sequence diagram

![UC003 — a. Sequence diagram, bản sao từ word/media/image22.png](../assets/diagrams/image22.png)

- Bản sao: [`docs/reference/assets/diagrams/image22.png`](../assets/diagrams/image22.png)

### b. Activity diagram

![UC003 — b. Activity diagram, bản sao từ word/media/image21.png](../assets/diagrams/image21.png)

- Bản sao: [`docs/reference/assets/diagrams/image21.png`](../assets/diagrams/image21.png)

## Ghi chú biên tập

- Bản trích giữ nguyên nội dung bảng đặc tả của DOCX, kể cả cách đánh số bước trong bản gốc: bước 1 rồi đến bước 3; dòng “Hiển thị màn hình lập hóa đơn.” không được đánh số 2.
- Bảng gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của luồng sự kiện.
- Hai sơ đồ nằm dưới dạng hình nhúng trong DOCX nguồn và bản sao byte-identical đã được liên kết trong tài liệu này tại `docs/reference/assets/diagrams/`; tài liệu này không vẽ lại và không diễn giải nội dung trực quan của sơ đồ.
- **Mâu thuẫn cục bộ liên quan:** mức hạt tồn kho (số lượng so với serial từng máy) — xem [C-07](../requirements/scope-and-requirements.md#c-07--mô-hình-tồn-kho-serial-hay-số-lượng); phạm vi khuyến mãi (áp dụng tại các bước 7–8) — **đã chốt 2026-10-24: thuộc phạm vi**, xem [C-04](../requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case). Tài liệu giữ nguyên câu chữ DOCX, không tự chọn bên.

Trước: [UC002 – Đăng xuất](UC002-logout.md) — Sau: [UC004 – Tra cứu sản phẩm và tồn kho](UC004-product-inventory-lookup.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
