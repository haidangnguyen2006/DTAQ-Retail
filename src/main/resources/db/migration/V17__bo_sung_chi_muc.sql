-- V17: bổ sung chỉ mục cho khóa ngoại và cột lọc. KHÔNG đổi bảng, cột hay ràng buộc.
-- Lý do: V1 chỉ có chỉ mục của PK/UNIQUE nên các truy vấn theo dòng sản phẩm, ngày lập,
-- khách hàng, hóa đơn gốc đều quét toàn bảng; trigger tồn kho (V6) đếm lại máy của dòng
-- cho mỗi dòng "SanPham" thay đổi nên nhập Excel N máy là N lần quét toàn bảng.

-- Trigger tồn kho (V6), chọn máy theo FIFO khi bán, tra cứu máy theo dòng.
CREATE INDEX IF NOT EXISTS idx_sanpham_dong_trangthai_ngaynhap
  ON "SanPham" ("maDongSP", "trangThai", "ngayNhapKhoBanDau", "soSerial");

-- Báo cáo tồn kho lâu ngày, đếm máy tồn lâu ở trang chủ.
CREATE INDEX IF NOT EXISTS idx_sanpham_trangthai_ngaynhap
  ON "SanPham" ("trangThai", "ngayNhapKhoBanDau");

-- Thống kê theo kỳ, trang chủ theo ngày và nhân viên.
CREATE INDEX IF NOT EXISTS idx_hoadon_ngaylap_nhanvien
  ON "HoaDon" ("ngayLap", "nhanVienLap");

-- Lịch sử mua hàng của khách.
CREATE INDEX IF NOT EXISTS idx_hoadon_khachhang_ngaylap
  ON "HoaDon" ("khachHang", "ngayLap" DESC);

-- Thống kê doanh số theo dòng sản phẩm.
CREATE INDEX IF NOT EXISTS idx_chitiethoadon_dong
  ON "ChiTietHoaDon" ("maDongSP");

-- Truy lịch sử bán theo serial; hỗ trợ kiểm tra khóa ngoại tới "SanPham".
CREATE INDEX IF NOT EXISTS idx_chitiethoadon_serial
  ON "ChiTietHoaDon" ("soSerial");

-- Phiếu theo hóa đơn gốc (tra cứu giao dịch của khách).
CREATE INDEX IF NOT EXISTS idx_phieutra_hoadon ON "PhieuTra" ("hoaDon");
CREATE INDEX IF NOT EXISTS idx_phieudoi_hoadon ON "PhieuDoi" ("hoaDon");

-- Điều chỉnh doanh thu theo ngày hoàn tất.
CREATE INDEX IF NOT EXISTS idx_phieutra_ngayhoantat
  ON "PhieuTra" ("ngayHoanTat") WHERE "trangThaiXuLyPhieu" = 'HOAN_TAT';
CREATE INDEX IF NOT EXISTS idx_phieudoi_ngayhoantat
  ON "PhieuDoi" ("ngayHoanTat") WHERE "trangThaiXuLyPhieu" = 'HOAN_TAT';

-- Danh sách chờ duyệt và chờ hoàn tất.
CREATE INDEX IF NOT EXISTS idx_phieutra_dangmo
  ON "PhieuTra" ("trangThaiDuyet", "ngayTao") WHERE "trangThaiXuLyPhieu" = 'CHO_XU_LY';
CREATE INDEX IF NOT EXISTS idx_phieudoi_dangmo
  ON "PhieuDoi" ("trangThaiDuyet", "ngayTao") WHERE "trangThaiXuLyPhieu" = 'CHO_XU_LY';

-- Kiểm tra "máy đã đổi/trả", nối chi tiết phiếu với chi tiết hóa đơn.
CREATE INDEX IF NOT EXISTS idx_chitietphieutra_cthd
  ON "ChiTietPhieuTra" ("maHD", "soSerial");
CREATE INDEX IF NOT EXISTS idx_chitietphieudoi_cthd
  ON "ChiTietPhieuDoi" ("maHD", "soSerial");

-- Tra khách theo số điện thoại (không UNIQUE: giữ nguyên thiết kế).
CREATE INDEX IF NOT EXISTS idx_khachhang_sodienthoai
  ON "KhachHang" ("soDienThoai");

-- Hồ sơ lỗi theo máy và trạng thái xử lý.
CREATE INDEX IF NOT EXISTS idx_sanphamloi_serial_tinhtrang
  ON "SanPhamLoi" ("soSerial", "tinhTrangXuLy");

-- Dòng sản phẩm theo danh mục.
CREATE INDEX IF NOT EXISTS idx_dongsanpham_danhmuc
  ON "DongSanPham" ("maDanhMuc");
