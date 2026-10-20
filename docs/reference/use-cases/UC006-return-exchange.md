---
title: "UC006 – Xử lý đổi trả sản phẩm"
uc_id: "UC006"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC006 – Xử lý đổi trả sản phẩm"
page: "Chưa xác minh"
---

# UC006 – Xử lý đổi trả sản phẩm

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC006 – Xử lý đổi trả sản phẩm”.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC006 – Xử lý đổi trả sản phẩm |
| Bảng đặc tả | Bảng use case đặt ngay sau heading “UC006 – Xử lý đổi trả sản phẩm” |
| Sơ đồ | Tiểu mục “a. Sequence diagram” và “b. Activity diagram” ngay sau bảng đặc tả |
| Số trang | Chưa xác minh |
| Danh sách use case | Chương 3, mục III — UC001–UC019; xem [catalog.md](catalog.md) |
| Yêu cầu chức năng liên quan | FR008 — **ứng viên — chưa xác nhận** ([truy vết §3](../traceability.md#3-truy-vết-yêu-cầu-chức-năng)) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC006 |
| Tiêu đề bảng (nguyên văn) | Use case: Xử lý đổi trả sản phẩm |
| Mục đích: | Xử lý trả hàng hoặc đổi sang máy khác. |
| Mô tả: | Nhân viên kiểm tra hóa đơn gốc, lập phiếu và xử lý tiền hoàn hoặc chênh lệch theo thẩm quyền. |
| Tác nhân: | Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã đăng nhập đúng quyền. Có hóa đơn đã thanh toán để đối chiếu máy khách mang đến. |
| Điều kiện sau: | Hoàn tất: Lưu phiếu, ghi nhận tiền và cập nhật máy, tồn kho. Chờ duyệt: Lưu phiếu chờ quản lý, chưa hoàn tiền và chưa hoàn tất. Hóa đơn gốc được bảo toàn. |

## Luồng sự kiện chính (Basic flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Xử lý đổi trả sản phẩm”. |  |
|  | 2. Hiển thị màn hình tiếp nhận đổi/trả. |
| 3. Tra cứu hóa đơn gốc. |  |
|  | 4. Hiển thị hóa đơn và các máy đã bán. |
| 5. Chọn máy khách yêu cầu trả. |  |
| 6. Nhập tình trạng máy và lý do trả. |  |
|  | 7. Kiểm tra điều kiện đổi/trả và số lượng đã xử lý trước đó. |
| 8. Chọn hình thức trả hàng và hoàn tiền. |  |
|  | 9. Tính tiền hoàn theo giá trị thanh toán thực tế trên hóa đơn gốc. |
|  | 10. Kiểm tra số tiền hoàn theo hạn mức phê duyệt. |
|  | 11. Hiển thị nội dung phiếu trả đủ điều kiện hoàn tất. |
| 12. Xác nhận đã hoàn tiền cho khách. |  |
|  | 13. Kiểm tra điều kiện hoàn tất phiếu. |
|  | 14. Lưu phiếu trả và chi tiết phiếu. |
|  | 15. Cập nhật máy nhận lại theo tình trạng thực tế. |
|  | 16. Cập nhật tồn kho tương ứng. |
|  | 17. Hiển thị chứng từ đã hoàn tất. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 4.a.1. Không tìm thấy hóa đơn đã thanh toán phù hợp. |
|  | 4.a.2. Thông báo không đủ căn cứ xử lý đổi/trả. |
|  | 4.a.3. Quay lại bước 3. |
|  | 7.a.1. Phát hiện máy không thuộc hóa đơn, hết thời hạn hoặc đã được đổi/trả hết số lượng. |
|  | 7.a.2. Hiển thị lý do không đủ điều kiện xử lý. |
|  | 7.a.3. Quay lại bước 5. |
| 8.a.1. Chọn hình thức đổi sản phẩm. |  |
| 8.a.2. Chọn máy thay thế theo serial. |  |
|  | 8.a.3. Kiểm tra máy thay thế có sẵn sàng bán. |
|  | 8.a.4. Tính chênh lệch giữa giá trị sau thuế của máy mới và giá trị thanh toán thực tế của máy trả. |
|  | 8.a.5. Hiển thị phiếu đổi và số tiền chênh lệch. |
|  | 8.a.6. Tiếp tục bước 10 với số tiền cần hoàn nếu có. |
|  | 8.b.1. Phát hiện máy thay thế không đủ điều kiện bán. |
|  | 8.b.2. Thông báo không thể chọn máy thay thế. |
|  | 8.b.3. Quay lại bước 8.a.2. |
|  | 10.a.1. Xác định số tiền hoàn từ 3.000.000 đồng trở lên. |
|  | 10.a.2. Lưu phiếu ở trạng thái chờ duyệt. |
|  | 10.a.3. Chuyển yêu cầu đến chức năng phê duyệt đổi/trả. |
|  | 10.a.4. Kết thúc lượt xử lý hiện tại mà chưa hoàn tiền. |
| 11.a.1. Mở lại phiếu đã được quản lý phê duyệt. |  |
|  | 11.a.2. Kiểm tra trạng thái phê duyệt và dữ liệu hiện tại. |
|  | 11.a.3. Tiếp tục bước 12. |
| 12.a.1. Xác nhận đã thu thêm tiền khi máy đổi có giá trị cao hơn. |  |
|  | 12.a.2. Tiếp tục bước 13. |
| 12.b.1. Xác nhận đổi ngang giá khi không phát sinh thu hoặc hoàn tiền. |  |
|  | 12.b.2. Tiếp tục bước 13. |
|  | 13.a.1. Phát hiện phiếu chưa được duyệt theo hạn mức hoặc đã bị từ chối. |
|  | 13.a.2. Từ chối hoàn tất phiếu. |
|  | 14.a.1. Xác định giao dịch đang xử lý là đổi sản phẩm. |
|  | 14.a.2. Lưu PhieuDoi và ChiTietPhieuDoi thay cho phiếu trả. |
|  | 14.a.3. Tiếp tục bước 15. |
|  | 15.a.1. Xác định máy nhận lại bị lỗi. |
|  | 15.a.2. Ghi nhận máy vào nhóm cần xử lý lỗi. |
|  | 15.a.3. Loại máy khỏi lượng hàng sẵn sàng bán. |
|  | 15.a.4. Tiếp tục bước 16. |
|  | 16.a.1. Xác định giao dịch có máy thay thế được giao cho khách. |
|  | 16.a.2. Cập nhật giảm tồn và trạng thái đã bán của máy thay thế. |
|  | 16.a.3. Tiếp tục bước 17. |

## Sơ đồ

Hai tiểu mục sau nằm ngay sau bảng đặc tả trong DOCX. Bản sao hình nằm trong [assets/diagrams](../assets/diagrams/) theo bảng ánh xạ tại [Phân luồng màn hình và thiết kế màn hình — mục 4](../ui/screen-flow-and-screens.md#4-tham-chiếu-diagram-use-case-chương-3). Tài liệu này không diễn giải nội dung hình.

### a. Sequence diagram

![UC006 — a. Sequence diagram, bản sao từ word/media/image26.png](../assets/diagrams/image26.png)

- Bản sao: [`docs/reference/assets/diagrams/image26.png`](../assets/diagrams/image26.png)

### b. Activity diagram

![UC006 — b. Activity diagram, bản sao từ word/media/image27.png](../assets/diagrams/image27.png)

- Bản sao: [`docs/reference/assets/diagrams/image27.png`](../assets/diagrams/image27.png)

## Ghi chú biên tập

- Bản trích giữ nguyên nội dung bảng đặc tả của DOCX, bao gồm ngưỡng phê duyệt “3.000.000 đồng” tại bước 10.a.1; đối chiếu quy tắc canonical [BR-07](../requirements/scope-and-requirements.md#5-quy-tắc-nghiệp-vụ-chính) và [FR008](../requirements/scope-and-requirements.md#fr008--xử-lý-hoàn-trảđổi-sản-phẩm).
- Bảng gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của luồng sự kiện.
- Hai sơ đồ nằm dưới dạng hình nhúng trong DOCX nguồn và bản sao byte-identical đã được liên kết trong tài liệu này tại `docs/reference/assets/diagrams/`; tài liệu này không vẽ lại và không diễn giải nội dung trực quan của sơ đồ.
- **Mâu thuẫn cục bộ liên quan:** mức hạt tồn kho (số lượng so với serial từng máy) — xem [C-07](../requirements/scope-and-requirements.md#c-07--mô-hình-tồn-kho-serial-hay-số-lượng). Tài liệu giữ nguyên câu chữ DOCX, không tự chọn bên.

Trước: [UC005 – Tra cứu khách hàng và giao dịch](UC005-customer-transaction-lookup.md) — Sau: [UC007 – Thêm khách hàng](UC007-customer-management.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
