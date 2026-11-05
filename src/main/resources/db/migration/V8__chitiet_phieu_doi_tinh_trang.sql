-- DTAQ Retail - V8 bổ sung tình trạng hàng trả cho chi tiết phiếu đổi (PostgreSQL)
-- ============================================================
-- MỤC ĐÍCH: cho phép UC006 bước 6 (nhập tình trạng máy khách trả lại) và bước
-- 15.a (máy lỗi vào nhóm cần xử lý) áp dụng được cho cả phiếu ĐỔI, không chỉ
-- phiếu trả. "ChiTietPhieuDoi" trước đây thiếu cột này nên tầng ứng dụng không
-- thể quyết định máy nhận lại quay về "SAN_SANG_BAN" hay chuyển "HANG_LOI".
--
-- QUY ƯỚC GIÁ TRỊ:
--   * NGUYEN_VEN: máy còn nguyên vẹn, quay lại "SAN_SANG_BAN" sau khi hoàn tất.
--   * HANG_LOI  : máy lỗi, chuyển "HANG_LOI" và ghi nhận vào "SanPhamLoi".
--
-- VÌ SAO TẠO MIGRATION MỚI THAY VÌ SỬA V1: V1-V7 đã được áp dụng; sửa nội dung
-- sẽ làm sai lệch checksum của Flyway. Cột có DEFAULT 'NGUYEN_VEN' để dữ liệu
-- seed cũ (V4) và các dòng đã có giữ nguyên nghĩa "máy nguyên vẹn".
-- ============================================================

ALTER TABLE "ChiTietPhieuDoi"
  ADD COLUMN "tinhTrangHangTra" VARCHAR NOT NULL DEFAULT 'NGUYEN_VEN';

ALTER TABLE "ChiTietPhieuDoi"
  ADD CONSTRAINT ck_chitietphieudoi_tinhtrang
  CHECK ("tinhTrangHangTra" IN ('NGUYEN_VEN', 'HANG_LOI'));

COMMENT ON COLUMN "ChiTietPhieuDoi"."tinhTrangHangTra" IS
  'Tình trạng máy khách trả lại khi đổi máy (UC006 bước 6). '
  'NGUYEN_VEN: máy quay lại SAN_SANG_BAN. '
  'HANG_LOI: máy chuyển HANG_LOI và ghi nhận SanPhamLoi (bước 15.a). '
  'Giá trị mặc định NGUYEN_VEN áp cho dữ liệu seed cũ.';
