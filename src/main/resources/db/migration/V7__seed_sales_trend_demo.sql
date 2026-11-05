-- DTAQ Retail - V5 seed demo for time-based sales trend report
-- Mục đích: tạo dữ liệu mẫu theo thời gian để dễ quan sát doanh số theo dòng sản phẩm
-- trên màn hình thống kê doanh số (UC015). Script này có thể chạy lại nhiều lần mà không lỗi
-- do trùng khóa vì dùng ON CONFLICT DO NOTHING.

-- 1) Dòng sản phẩm demo phát sinh theo thời gian
INSERT INTO "DongSanPham" ("maDongSP", "tenDongSP", "maDanhMuc", "donViTinh", "soLuongTon", "thue", "donGiaBanTruocThue", "trangThaiKinhDoanh", "ngayNhapKho", "ngayCapNhapCuoi")
VALUES
  ('DSP-2026-000101', 'Tủ lạnh Mitsubishi 380L', 'DM-2026-000002', 'Chiếc', 6, 10, 18900000, TRUE, '2026-06-01', '2026-06-01'),
  ('DSP-2026-000102', 'Máy giặt cửa trên 8kg', 'DM-2026-000003', 'Chiếc', 5, 10, 13200000, TRUE, '2026-06-10', '2026-06-10'),
  ('DSP-2026-000103', 'Máy lọc nước 7 lõi', 'DM-2026-000005', 'Chiếc', 4, 10, 9600000, TRUE, '2026-06-18', '2026-06-18')
ON CONFLICT ("maDongSP") DO NOTHING;

-- 2) Số serial tương ứng với từng dòng sản phẩm
INSERT INTO "SanPham" ("soSerial", "maNCC", "maDongSP", "giaNhap", "ngayNhapKhoBanDau", "trangThai", "soLanDoiTra", "soLanSua")
VALUES
  ('SER-2026-0101', 'NCC-2026-000001', 'DSP-2026-000101', 15000000, '2026-06-01', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0102', 'NCC-2026-000001', 'DSP-2026-000101', 15000000, '2026-06-01', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0103', 'NCC-2026-000001', 'DSP-2026-000101', 15000000, '2026-06-01', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0104', 'NCC-2026-000001', 'DSP-2026-000101', 15000000, '2026-06-01', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0105', 'NCC-2026-000001', 'DSP-2026-000101', 15000000, '2026-06-01', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0106', 'NCC-2026-000001', 'DSP-2026-000101', 15000000, '2026-06-01', 'SAN_SANG_BAN', 0, 0),

  ('SER-2026-0201', 'NCC-2026-000001', 'DSP-2026-000102', 10500000, '2026-06-10', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0202', 'NCC-2026-000001', 'DSP-2026-000102', 10500000, '2026-06-10', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0203', 'NCC-2026-000001', 'DSP-2026-000102', 10500000, '2026-06-10', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0204', 'NCC-2026-000001', 'DSP-2026-000102', 10500000, '2026-06-10', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0205', 'NCC-2026-000001', 'DSP-2026-000102', 10500000, '2026-06-10', 'SAN_SANG_BAN', 0, 0),

  ('SER-2026-0301', 'NCC-2026-000001', 'DSP-2026-000103', 7600000, '2026-06-18', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0302', 'NCC-2026-000001', 'DSP-2026-000103', 7600000, '2026-06-18', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0303', 'NCC-2026-000001', 'DSP-2026-000103', 7600000, '2026-06-18', 'SAN_SANG_BAN', 0, 0),
  ('SER-2026-0304', 'NCC-2026-000001', 'DSP-2026-000103', 7600000, '2026-06-18', 'SAN_SANG_BAN', 0, 0)
ON CONFLICT ("soSerial") DO NOTHING;

-- 3) Hóa đơn theo thời gian: doanh số tăng dần qua các tháng cho từng dòng 
INSERT INTO "HoaDon" ("maHD", "ngayLap", "nhanVienLap", "khachHang", "trangThaiThanhToan")
VALUES
  ('HD-2026-200001', '2026-07-04', 'NVBANHANG-2026-002', 'KH-2026-000011', TRUE),
  ('HD-2026-200002', '2026-07-18', 'NVBANHANG-2026-003', 'KH-2026-000021', TRUE),
  ('HD-2026-200003', '2026-07-29', 'NVBANHANG-2026-004', 'KH-2026-000033', TRUE),
  ('HD-2026-200004', '2026-08-05', 'NVBANHANG-2026-005', 'KH-2026-000041', TRUE),
  ('HD-2026-200005', '2026-08-16', 'NVBANHANG-2026-002', 'KH-2026-000048', TRUE),
  ('HD-2026-200006', '2026-08-27', 'NVBANHANG-2026-003', 'KH-2026-000052', TRUE),
  ('HD-2026-200007', '2026-09-05', 'NVBANHANG-2026-004', 'KH-2026-000012', TRUE),
  ('HD-2026-200008', '2026-09-21', 'NVBANHANG-2026-005', 'KH-2026-000030', TRUE)
ON CONFLICT ("maHD") DO NOTHING;

INSERT INTO "ChiTietHoaDon" ("maHD", "soSerial", "maDongSP", "soLuong", "donGiaBanTaiLucBan", "thueSuatTaiLucBan", "maKM", "tyLeGiamTaiLucBan", "tienKhuyenMaiThucTe", "tienTruocKhuyenMai", "tienSauKhuyenMai", "tienThue")
VALUES
  ('HD-2026-200001', 'SER-2026-0101', 'DSP-2026-000101', 1, 18900000, 10, NULL, NULL, 0, 18900000, 18900000, 1890000),
  ('HD-2026-200002', 'SER-2026-0201', 'DSP-2026-000102', 1, 13200000, 10, NULL, NULL, 0, 13200000, 13200000, 1320000),
  ('HD-2026-200003', 'SER-2026-0301', 'DSP-2026-000103', 1, 9600000, 10, NULL, NULL, 0, 9600000, 9600000, 960000),
  ('HD-2026-200004', 'SER-2026-0102', 'DSP-2026-000101', 1, 18900000, 10, NULL, NULL, 0, 18900000, 18900000, 1890000),
  ('HD-2026-200005', 'SER-2026-0202', 'DSP-2026-000102', 1, 13200000, 10, NULL, NULL, 0, 13200000, 13200000, 1320000),
  ('HD-2026-200006', 'SER-2026-0302', 'DSP-2026-000103', 1, 9600000, 10, NULL, NULL, 0, 9600000, 9600000, 960000),
  ('HD-2026-200007', 'SER-2026-0103', 'DSP-2026-000101', 1, 18900000, 10, NULL, NULL, 0, 18900000, 18900000, 1890000),
  ('HD-2026-200008', 'SER-2026-0203', 'DSP-2026-000102', 1, 13200000, 10, NULL, NULL, 0, 13200000, 13200000, 1320000)
ON CONFLICT ("maHD", "soSerial") DO NOTHING;

-- 4) Một vài phiếu trả hoàn tất để thể hiện xu hướng sau điều chỉnh
INSERT INTO "PhieuTra" ("maPhieuTra", "nhanVienLap", "hoaDon", "ngayTao", "ngayHoanTat", "trangThaiDuyet", "trangThaiXuLyPhieu", "nhanVienDuyet", "ngayDuyet", "lyDoDuyet", "tienKhachDua")
VALUES
  ('PT-2026-200001', 'NVBANHANG-2026-002', 'HD-2026-200004', '2026-08-06', '2026-08-07', 'DA_DUYET', 'HOAN_TAT', 'NVQUANLY-2026-003', '2026-08-07', 'Hàng bị lỗi màn hình', 20890000),
  ('PT-2026-200002', 'NVBANHANG-2026-003', 'HD-2026-200007', '2026-09-06', '2026-09-07', 'DA_DUYET', 'HOAN_TAT', 'NVQUANLY-2026-003', '2026-09-07', 'Đổi trả do lỗi cài đặt', 20790000)
ON CONFLICT ("maPhieuTra") DO NOTHING;

INSERT INTO "ChiTietPhieuTra" ("maPhieuTra", "maHD", "soSerial", "tinhTrangHangTra", "lyDoTra")
VALUES
  ('PT-2026-200001', 'HD-2026-200004', 'SER-2026-0102', 'HANG_LOI', 'Màn hình hiển thị đốm sáng'),
  ('PT-2026-200002', 'HD-2026-200007', 'SER-2026-0103', 'NGUYEN_VEN', 'Khách đổi trả theo cam kết')
ON CONFLICT ("maPhieuTra", "maHD", "soSerial") DO NOTHING;

-- 5) Tóm tắt dữ liệu có thể kiểm tra ngay trong SQL
SELECT dsp."maDongSP",
       dsp."tenDongSP",
       COUNT(CASE WHEN hd."ngayLap" BETWEEN '2026-07-01' AND '2026-07-31' THEN 1 END) AS so_hoa_don_thang_7,
       COUNT(CASE WHEN hd."ngayLap" BETWEEN '2026-08-01' AND '2026-08-31' THEN 1 END) AS so_hoa_don_thang_8,
       COUNT(CASE WHEN hd."ngayLap" BETWEEN '2026-09-01' AND '2026-09-30' THEN 1 END) AS so_hoa_don_thang_9
FROM "DongSanPham" dsp
LEFT JOIN "ChiTietHoaDon" ct ON ct."maDongSP" = dsp."maDongSP"
LEFT JOIN "HoaDon" hd ON hd."maHD" = ct."maHD" AND hd."trangThaiThanhToan" = TRUE
WHERE dsp."maDongSP" IN ('DSP-2026-000101', 'DSP-2026-000102', 'DSP-2026-000103')
GROUP BY dsp."maDongSP", dsp."tenDongSP"
ORDER BY dsp."maDongSP";
