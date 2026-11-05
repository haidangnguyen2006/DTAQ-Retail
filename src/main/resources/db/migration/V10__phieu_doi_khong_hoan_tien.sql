-- DTAQ Retail - V10 đổi hàng không hoàn tiền (PostgreSQL)
-- ============================================================
-- QUYẾT ĐỊNH D17 (chủ dự án, 2026-11-05): đổi hàng chỉ sang máy có giá trị
-- ngang bằng hoặc thấp hơn. Không hoàn tiền, không thu thêm.
--
-- HỆ QUẢ TRÊN BẢNG "PhieuDoi":
--   * "tienChenhLech" = giá trị máy mới - giá trị máy trả, nên với D17 chỉ có
--     thể <= 0. Chênh lệch âm là phần khách mất, không được hoàn lại.
--   * "tienKhachDuaThem" luôn bằng 0 vì không phát sinh thu thêm.
--
-- DỮ LIỆU HIỆN TẠI: 10 phiếu đổi seed (V4) đều có "tienChenhLech" = 0 và
-- "tienKhachDuaThem" = 0 nên hai ràng buộc được thêm mà không cần sửa dữ liệu.
--
-- VÌ SAO MIGRATION MỚI: V1-V9 đã áp dụng; thêm CHECK bằng migration mới để
-- bảo toàn checksum Flyway.
-- ============================================================

ALTER TABLE "PhieuDoi"
  ADD CONSTRAINT ck_phieudoi_khong_hoan_tien CHECK ("tienChenhLech" <= 0),
  ADD CONSTRAINT ck_phieudoi_khong_thu_them CHECK ("tienKhachDuaThem" = 0);

COMMENT ON CONSTRAINT ck_phieudoi_khong_hoan_tien ON "PhieuDoi" IS
  'D17 (2026-11-05): đổi hàng chỉ sang máy ngang bằng hoặc thấp hơn nên chênh lệch '
  'không dương; chênh lệch âm là phần khách mất, cửa hàng không hoàn tiền.';

COMMENT ON CONSTRAINT ck_phieudoi_khong_thu_them ON "PhieuDoi" IS
  'D17 (2026-11-05): đổi hàng không phát sinh thu thêm nên "tienKhachDuaThem" luôn bằng 0.';
