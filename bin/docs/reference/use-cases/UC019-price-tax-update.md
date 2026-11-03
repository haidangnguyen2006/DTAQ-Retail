---
title: "UC019 – Cập nhật giá bán và thuế suất"
uc_id: "UC019"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC019 – Cập nhật giá bán và thuế suất"
page: "Chưa xác minh"
---

# UC019 – Cập nhật giá bán và thuế suất

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC019 – Cập nhật giá bán và thuế suất”, bảng use case đặt ngay sau heading.
>
> Số trang chưa được xác minh. Định vị bằng Chương 3, mục IV, heading UC và bảng đặc tả tương ứng.

## Cảnh báo chưa được giải quyết

> **C-08 — Ai cập nhật giá bán và thuế suất?** UC019 quy định tác nhân là **Nhân viên quản lý** (“Quản lý cập nhật giá và thuế cho giao dịch mới”), trong khi Ch.1 §II.1.b và FR006 nói Nhân viên bán hàng cập nhật theo thông tin đã được Quản lý phê duyệt. Nguồn chưa thống nhất vai trò; tài liệu này giữ nguyên tác nhân của đặc tả và không tự chọn. Xem [C-08 — Ai cập nhật giá bán và thuế suất?](../requirements/scope-and-requirements.md#c-08--ai-cập-nhật-giá-bán-và-thuế-suất).

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC019 – Cập nhật giá bán và thuế suất |
| Bảng đặc tả | Bảng use case duy nhất đặt ngay sau heading “UC019 – Cập nhật giá bán và thuế suất”, trước heading “CHƯƠNG 4: THIẾT KẾ VÀ HIỆN THỰC” |
| Sơ đồ | Không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh sách use case | [Danh mục use case](catalog.md) — Chương 3, mục III (UC001–UC019) |
| Quy tắc / xung đột liên quan | [C-08](../requirements/scope-and-requirements.md#c-08--ai-cập-nhật-giá-bán-và-thuế-suất) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC019 |
| Tiêu đề bảng (nguyên văn) | Use case: Cập nhật giá bán và thuế suất |
| Mục đích: | Thiết lập giá bán trước thuế và thuế suất của dòng sản phẩm. |
| Mô tả: | Quản lý cập nhật giá và thuế cho giao dịch mới; giao dịch cũ giữ nguyên thông tin đã chốt. |
| Tác nhân: | Nhân viên quản lý. |
| Điều kiện trước: | Quản lý đã đăng nhập đúng quyền. Dòng sản phẩm cần cập nhật đã tồn tại. |
| Điều kiện sau: | Giá và thuế hiện hành được cập nhật; giá và thuế đã chốt trên hóa đơn cũ không thay đổi. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Cập nhật giá bán và thuế suất”. |  |
|  | 2. Hiển thị danh sách dòng sản phẩm. |
| 3. Chọn dòng sản phẩm cần cập nhật. |  |
|  | 4. Hiển thị giá trước thuế và thuế suất hiện tại. |
| 5. Nhập giá bán trước thuế mới. |  |
| 6. Nhập thuế suất mới. |  |
| 7. Chọn lưu cập nhật. |  |
|  | 8. Kiểm tra tính hợp lệ của giá và thuế suất. |
|  | 9. Hiển thị thông tin thay đổi để xác nhận. |
| 10. Xác nhận cập nhật. |  |
|  | 11. Lưu giá và thuế suất mới vào dòng sản phẩm. |
|  | 12. Hiển thị thông tin sau cập nhật. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý | Hệ thống |
| --- | --- |
|  | 8.a.1. Phát hiện giá bán hoặc thuế suất không hợp lệ. |
|  | 8.a.2. Hiển thị lỗi nhập liệu. |
|  | 8.a.3. Quay lại bước 5. |
| 10.a.1. Chọn không xác nhận cập nhật. |  |
|  | 10.a.2. Giữ nguyên giá và thuế suất hiện hành. |
|  | 11.a.1. Phát hiện lỗi khi lưu giá và thuế suất. |
|  | 11.a.2. Giữ nguyên dữ liệu trước cập nhật. |
|  | 11.a.3. Thông báo cập nhật thất bại. |

## Ràng buộc và ghi chú biên tập

- Bảng đặc tả gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của hai luồng sự kiện.
- Các ràng buộc nghiệp vụ rút trực tiếp:
  - Chỉ Nhân viên quản lý đã đăng nhập đúng quyền mới được cập nhật giá bán và thuế suất.
  - Dòng sản phẩm cần cập nhật phải đã tồn tại.
  - Giá bán trước thuế và thuế suất mới phải được kiểm tra tính hợp lệ trước khi lưu.
  - Hệ thống phải hiển thị thông tin thay đổi để người dùng xác nhận trước khi lưu.
  - Nếu người dùng không xác nhận, giá và thuế suất hiện hành phải được giữ nguyên.
  - Nếu lưu thất bại, dữ liệu trước cập nhật phải được giữ nguyên và hệ thống phải thông báo cập nhật thất bại.
  - Giá và thuế suất mới chỉ áp dụng cho giao dịch mới; giá và thuế đã chốt trên hóa đơn cũ không được thay đổi.
  - Sau khi cập nhật thành công, hệ thống phải hiển thị thông tin sau cập nhật.
- Bảng gốc không có mục “Error Flows” riêng; các nhánh dữ liệu không hợp lệ, không xác nhận và lỗi lưu nằm trong Luồng sự kiện phụ.
- Trong phạm vi đặc tả UC019, nguồn không nêu tiêu chí định lượng cụ thể cho tính hợp lệ của giá bán và thuế suất (ví dụ: giá phải lớn hơn 0, thuế suất nằm trong miền nào) và không quy định thời điểm hiệu lực chi tiết của giá mới ngoài nguyên tắc “dùng cho giao dịch mới”.
- Đặc tả gốc không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động cho UC019; tài liệu này không vẽ sơ đồ thay thế.

Trước: [UC018 – Phê duyệt yêu cầu đổi trả](UC018-return-approval.md) — Sau: — (hết danh sách use case trong phạm vi tài liệu này) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
