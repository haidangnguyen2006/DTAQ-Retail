-- DTAQ Retail - V9 backfill tình trạng hàng trả của chi tiết phiếu đổi (PostgreSQL)
-- ============================================================
-- BỐI CẢNH: V8 thêm cột "tinhTrangHangTra" với DEFAULT 'NGUYEN_VEN' cho dữ liệu
-- cũ. Cách gán đó đúng với máy còn nguyên vẹn nhưng sai với các máy đã được
-- ghi nhận lỗi trong "SanPhamLoi".
--
-- BẰNG CHỨNG BACKFILL: một dòng "ChiTietPhieuDoi" bị coi là hàng lỗi khi tồn tại
-- hồ sơ "SanPhamLoi" gắn đúng "maPhieuDoi" và đúng "soSerial" của dòng đó
-- (máy lỗi / gửi bảo hành / trả nhà cung cấp). Dòng không có hồ sơ lỗi vẫn giữ
-- 'NGUYEN_VEN' — giả định tốt nhất vì không còn nguồn dữ liệu nào khác.
--
-- DROP DEFAULT: sau backfill, mọi INSERT phải chỉ định "tinhTrangHangTra" tường
-- minh (DAO đã làm vậy) để không âm thầm gán sai tình trạng cho giao dịch mới.
-- Migration mới thay vì sửa V8 để bảo toàn checksum Flyway đã áp.
-- ============================================================

UPDATE "ChiTietPhieuDoi" ct
SET "tinhTrangHangTra" = 'HANG_LOI'
WHERE ct."tinhTrangHangTra" = 'NGUYEN_VEN'
  AND EXISTS (
    SELECT 1
    FROM "SanPhamLoi" spl
    WHERE spl."maPhieuDoi" = ct."maPhieuDoi"
      AND spl."soSerial" = ct."soSerial"
  );

ALTER TABLE "ChiTietPhieuDoi"
  ALTER COLUMN "tinhTrangHangTra" DROP DEFAULT;

COMMENT ON COLUMN "ChiTietPhieuDoi"."tinhTrangHangTra" IS
  'Tình trạng máy khách trả lại khi đổi máy (UC006 bước 6). '
  'V8 gán mặc định NGUYEN_VEN cho dữ liệu cũ; V9 sửa các dòng có hồ sơ SanPhamLoi '
  'gắn đúng phiếu và serial (máy lỗi/bảo hành/trả nhà cung cấp) thành HANG_LOI. '
  'Không còn DEFAULT để mọi INSERT phải chỉ định cột tường minh.';
