-- DTAQ Retail - V7 bổ sung cột chênh lệch cho phiếu đổi (PostgreSQL)
-- ============================================================
-- MỤC ĐÍCH: lưu chênh lệch CÓ DẤU giữa giá trị máy mới và giá trị máy trả
-- trên cùng một phiếu đổi (UC006 bước 8.a.4-8.a.5 và 12.a).
--
-- QUY ƯỚC DẤU:
--   "tienChenhLech" = giá trị máy mới - giá trị máy trả.
--   * Âm   : cửa hàng phải hoàn lại tiền cho khách.
--   * Dương: khách phải trả thêm, cùng nghĩa với "tienKhachDuaThem" >= 0.
--   * Bằng 0: đổi ngang, không thu không hoàn.
--
-- VÌ SAO TẠO MIGRATION MỚI THAY VÌ SỬA V1: V1-V6 đã được áp dụng; sửa nội
-- dung sẽ làm sai lệch checksum của Flyway. Cột có DEFAULT 0 để các phiếu
-- đổi cũ (seed V4) giữ nguyên nghĩa "không chênh lệch".
-- ============================================================

ALTER TABLE "PhieuDoi"
  ADD COLUMN "tienChenhLech" NUMERIC NOT NULL DEFAULT 0;

COMMENT ON COLUMN "PhieuDoi"."tienChenhLech" IS
  'Chênh lệch có dấu = giá trị máy mới - giá trị máy trả. '
  'Âm: cửa hàng hoàn tiền cho khách. Dương: thu thêm của khách '
  '(cùng chiều với "tienKhachDuaThem").';
