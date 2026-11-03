---
title: "UC001 – Đăng nhập"
uc_id: "UC001"
source: "../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx"
source_chapter: "Chương 3: PHÂN TÍCH"
source_section: "IV. Đặc tả các yêu cầu chức năng (functional)"
source_heading: "UC001 – Đăng nhập"
page: "Chưa xác minh"
---

# UC001 – Đăng nhập

> **Nguồn:** [Báo cáo phân tích và thiết kế — v12_Nhom04_9_REPORT_ApplicationDevelopment.docx](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx), Chương 3, mục IV, heading “UC001 – Đăng nhập”.
>
> Số trang chưa được xác minh; định vị bằng chương, heading UC và bảng/sơ đồ tương ứng.

## Truy vết nguồn

| Hạng mục | Giá trị |
| --- | --- |
| Tài liệu nguồn | [`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx) |
| Chương | Chương 3: PHÂN TÍCH |
| Mục | IV. Đặc tả các yêu cầu chức năng (functional) |
| Heading use case | UC001 – Đăng nhập |
| Bảng đặc tả | Bảng use case đặt ngay sau heading “UC001 – Đăng nhập” |
| Sơ đồ | Không có tiểu mục sơ đồ trong đặc tả gốc |
| Số trang | Chưa xác minh |
| Danh sách use case | Chương 3, mục III — UC001–UC019; xem [catalog.md](catalog.md) |
| Yêu cầu chức năng liên quan | FR001 — **ứng viên — chưa xác nhận** ([truy vết §3](../traceability.md#3-truy-vết-yêu-cầu-chức-năng)) |

## Thông tin chung

| Trường | Nội dung |
| --- | --- |
| Mã use case | UC001 |
| Tiêu đề bảng (nguyên văn) | Use case: UC001 – Đăng nhập |
| Mục đích: | Xác thực nhân viên và xác định quyền truy cập hệ thống. |
| Mô tả: | Nhân viên sử dụng tài khoản được cấp để đăng nhập. Hệ thống kiểm tra thông tin đăng nhập, trạng thái tài khoản và cấp quyền sử dụng tương ứng. |
| Tác nhân: | Nhân viên quản lý; Nhân viên bán hàng. |
| Điều kiện trước: | Nhân viên đã được cấp tài khoản. Ứng dụng chưa có phiên đăng nhập của nhân viên. |
| Điều kiện sau: | Thành công: Thiết lập phiên làm việc và hiển thị màn hình chính theo quyền nhân viên. Thất bại: Không thiết lập phiên làm việc; giữ màn hình đăng nhập. |

## Luồng sự kiện chính (Basic flows)

| Nhân viên quản lý / Nhân viên bán hàng | Hệ thống |
| --- | --- |
| 1. Mở ứng dụng. |  |
|  | 2. Hiển thị màn hình đăng nhập. |
| 3. Nhập tên đăng nhập. |  |
| 4. Nhập mật khẩu. |  |
| 5. Chọn nút “Đăng nhập”. |  |
|  | 6. Kiểm tra các trường thông tin bắt buộc. |
|  | 7. Xác thực tên đăng nhập và mật khẩu với thông tin của NhanVien. |
|  | 8. Kiểm tra trạng thái tài khoản qua trangThaiHoatDong. |
|  | 9. Xác định quyền sử dụng từ loaiQuyen của nhân viên. |
|  | 10. Thiết lập phiên làm việc cho nhân viên. |
|  | 11. Hiển thị màn hình chính với các chức năng tương ứng với quyền của nhân viên. |

## Luồng sự kiện phụ (Alternative Flows):

| Nhân viên quản lý / Nhân viên bán hàng | Hệ thống |
| --- | --- |
|  | 6.a.1. Phát hiện tên đăng nhập hoặc mật khẩu chưa được nhập. |
|  | 6.a.2. Hiển thị thông báo “Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu”. |
| 6.a.3. Bổ sung thông tin còn thiếu. |  |
| 6.a.4. Chọn nút “Đăng nhập”. |  |
|  | 6.a.5. Tiếp tục bước 6. |
|  | 7.a.1. Phát hiện tên đăng nhập không tồn tại hoặc mật khẩu không đúng. |
|  | 7.a.2. Hiển thị thông báo “Tên đăng nhập hoặc mật khẩu không đúng”. |
| 7.a.3. Nhập lại thông tin đăng nhập. |  |
| 7.a.4. Chọn nút “Đăng nhập”. |  |
|  | 7.a.5. Tiếp tục bước 6. |
|  | 7.b.1. Phát hiện không thể truy cập dữ liệu tài khoản. |
|  | 7.b.2. Hiển thị thông báo “Không thể đăng nhập lúc này. Vui lòng thử lại sau”. |
|  | 7.b.3. Giữ màn hình đăng nhập và kết thúc use case thất bại. |
|  | 8.a.1. Phát hiện tài khoản có trạng thái không hoạt động. |
|  | 8.a.2. Hiển thị thông báo “Tài khoản đã bị khóa. Vui lòng liên hệ nhân viên quản lý”. |
|  | 8.a.3. Giữ màn hình đăng nhập và kết thúc use case thất bại. |

## Ghi chú biên tập

- Bản trích giữ nguyên nội dung bảng đặc tả của DOCX. Bảng gốc không có trường “Ràng buộc” riêng; các ràng buộc nằm trong điều kiện trước, điều kiện sau và các bước của luồng sự kiện.
- Đặc tả gốc không có tiểu mục sơ đồ tuần tự hoặc sơ đồ hoạt động cho UC001.

Trước: — (use case đầu tiên trong phạm vi tài liệu này) — Sau: [UC002 – Đăng xuất](UC002-logout.md) — Nguồn: [DOCX](../../source/v12_Nhom04_9_REPORT_ApplicationDevelopment.docx)
