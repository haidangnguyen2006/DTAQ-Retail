# Plugin Figma: nhập giao diện DTAQ kèm prototype

Plugin dựng trên Figma một trang mới gồm mọi màn hình của ứng dụng (ảnh chụp 2x từ app thật) và nối
sẵn luồng prototype:

- Đăng nhập → Trang chủ Quản lý; Đăng nhập ↔ Quên mật khẩu.
- Menu trái của từng màn hình → màn hình tương ứng (vai trò Quản lý và Nhân viên bán hàng).
- Nút "Thêm…/Ghi nhận…" → hộp thoại; nút Đóng/Hủy → quay lại màn hình.
- Ba điểm bắt đầu luồng: `1. Đăng nhập → Quản lý`, `2. Quản lý`, `3. Nhân viên bán hàng`.

## 1. Xuất ảnh màn hình

Cần PostgreSQL đang chạy theo `db.properties` và máy có màn hình (cửa sổ app sẽ tự mở rồi đóng).
Công cụ chỉ đọc dữ liệu, không lưu hay thanh toán gì.

```sh
tools/figma-plugin/export-screens.sh        # Linux / macOS
tools\figma-plugin\export-screens.bat       # Windows
```

Kết quả nằm trong `tools/figma-plugin/screens/` (các tệp `.png` và `screens.json`; thư mục này không
đưa lên git). Có thể truyền kích thước cửa sổ khác, ví dụ `export-screens.sh 1920 1080`.

## 2. Nạp plugin vào Figma

1. Mở **Figma Desktop** → *Plugins* → *Development* → *Import plugin from manifest…*
2. Chọn `tools/figma-plugin/manifest.json`.

## 3. Dựng thiết kế

1. Trong tệp Figma bất kỳ: *Plugins* → *Development* → **DTAQ – Nhập giao diện & prototype**.
2. Bấm "Bấm để chọn tệp…", chọn **tất cả** tệp trong `screens/` (Ctrl/Cmd + A), rồi **Dựng thiết kế**.
3. Plugin tạo trang `DTAQ – Giao diện (ngày)`, chia khu vực *Xác thực / Quản lý / Nhân viên bán hàng /
   Hộp thoại*. Bấm **Present** (▶) và chọn điểm bắt đầu luồng để chạy prototype.

Vùng bấm là các hình chữ nhật trong suốt tên `↪ …` trong từng frame; có thể sửa hoặc thêm tương tác
ngay trong tab *Prototype* của Figma.
