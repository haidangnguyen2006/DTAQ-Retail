---
title: "UC002 – Đăng xuất"
uc_id: "UC002"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC002 – Đăng xuất"
page: "Chưa xác minh"
---

# UC002 – Đăng xuất

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC002 – Đăng xuất”.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC002 – Đăng xuất |
| Bảng đặc tả | Bảng use case đặt ngay sau heading “UC002 – Đăng xuất” |
| Sơ đồ | Không có tiểu mục sơ đồ trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh sách use case | Chương 3, mục III — UC001–UC019; xem [catalog.md](catalog.md) |
| Yêu cầu chức năng liên quan | FR002 — **ứng viên — chưa xác nhận** ([truy vết §3](../traceability.md#3-truy-vết-yêu-cầu-chức-năng)) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC002 |
| Tiêu đề bảng (nguyên văn) | Use case: Đăng xuất |
| Mục đích: | Kết thúc phiên làm việc của nhân viên. |
| Mô tả: | Nhân viên đăng xuất đẻ chấm dứt quyền truy cập bằng phiên hiện tại. |
| Tác nhân: | Nhân viên quản lý; Nhân viên bán hàng |
| Điều kiện trước: | Nhân viên đang đăng nhập. |
| Điều kiện sau: | Thành công: Phiên làm việc bị hủy; hiển thi màn hình đăng nhập. Hủy thao tác: Giữ nguyên phiên hiện tại. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý/Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Chọn chức năng “Đăng xuất” |  |
|  | 2. Hiển thị yêu cầu xác nhận đăng xuất |
| Xác nhận đăng xuất. |  |
|  | Hủy phiên làm việc hiện tại. |
|  | Hiển thị màn hình đăng nhập. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý/Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 3.a.1. Chọn không đăng xuất. |  |
|  | 3.a.2. Giữ phiên làm việc hiện tại. |
|  | 3.a.3. Trở về màn hình trước đó. |

## Ghi chú biên tập

- Bản trích giữ nguyên nội dung bảng đặc tả của DOCX, kể cả chính tả trong bản gốc: “đẻ chấm dứt” và “hiển thi màn hình đăng nhập”.
- Luồng sự kiện chính trong DOCX có bước 1–2, sau đó là các dòng **không được đánh số** (“Xác nhận đăng xuất.”, “Hủy phiên làm việc hiện tại.”, “Hiển thị màn hình đăng nhập.”); luồng sự kiện phụ bắt đầu bằng `3.a`. Đây là cách trình bày của nguồn (đã kiểm chứng trực tiếp từ XML của DOCX); tài liệu giữ nguyên, **không đánh số lại và không suy đoán các bước còn thiếu**.
- Bảng gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của luồng sự kiện.
- Đặc tả gốc không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động cho UC002.
- Ô tiêu đề bảng gốc ghi “Use case: Đăng xuất” (không kèm mã UC002).

Trước: [UC001 – Đăng nhập](UC001-login.md) — Sau: [UC003 – Lập hóa đơn bán lẻ](UC003-retail-invoice.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
