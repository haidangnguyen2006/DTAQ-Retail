-- DTAQ Retail - V2 demo accounts (PostgreSQL)
-- Hai tài khoản dưới đây là tài khoản DEMO, được tạo theo yêu cầu rõ ràng của người dùng.
-- Mật khẩu demo là 12345678, được lưu dưới dạng băm BCrypt (cost 10), không lưu mật khẩu thô.

INSERT INTO "NhanVien" ("maNV","hoTen","soDienThoai","email","ngayVaoLam","tenDangNhap","matKhau","loaiQuyen","trangThaiHoatDong")
VALUES
  ('NVQUANLY-2026-001','Đăng quản lý','0900000001','nguyenhaidangnt2006@gmail.com','2026-01-01','NVQL2026001','$2a$10$P.Gg4UrBpaD9ljFjqi8hcuvGR6RU0EF2U0PLAwMMBBlRtTpyVrwZ6','NHAN_VIEN_QUAN_LY',TRUE),
  ('NVBANHANG-2026-001','Đăng bán hàng','0900000002','nguyenhaidangnt2006@gmail.com','2026-01-01','NVBH2026001','$2a$10$tV156spc6sgsy3ZafsIZI.nnNWx9XXSJuSUMqvQHLNYAYbSFQ7Ut.','NHAN_VIEN_BAN_HANG',TRUE);
