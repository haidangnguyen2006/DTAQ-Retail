-- DTAQ Retail - V5 guard soLuongTon (PostgreSQL)
-- ============================================================
-- MỤC ĐÍCH: tính lại bộ đếm dẫn xuất "DongSanPham"."soLuongTon" theo RB37 —
-- mỗi dòng sản phẩm phải có soLuongTon bằng đúng số máy (bản ghi "SanPham")
-- của dòng đó đang ở trạng thái 'SAN_SANG_BAN'.
--
-- VÌ SAO TẠO MIGRATION MỚI THAY VÌ SỬA V4:
--   1. V1–V4 đã được áp dụng; sửa nội dung sẽ làm sai lệch checksum của Flyway.
--   2. SeedMigrationContractTest cấm câu lệnh UPDATE bên trong V4.
--   Do đó việc đồng bộ bộ đếm được tách thành migration riêng, không đụng V1–V4.
--
-- TÍNH IDEMPOTENT: câu lệnh UPDATE chỉ gán lại giá trị đếm được từ dữ liệu hiện có,
-- chạy nhiều lần vẫn cho cùng kết quả và không tạo thêm bản ghi.
-- ============================================================

UPDATE "DongSanPham" d
SET "soLuongTon" = (
  SELECT count(*)
  FROM "SanPham" s
  WHERE s."maDongSP" = d."maDongSP"
    AND s."trangThai" = 'SAN_SANG_BAN'
);

-- Self-check: sau khi đồng bộ, không còn dòng nào lệch bất biến RB37.
DO $$
DECLARE
  r RECORD;
BEGIN
  -- RB37: soLuongTon cua dong phai bang so may dang SAN_SANG_BAN cua dong do.
  FOR r IN
    SELECT d."maDongSP"
    FROM "DongSanPham" d
    WHERE d."soLuongTon" <> (
      SELECT count(*) FROM "SanPham" s
      WHERE s."maDongSP" = d."maDongSP" AND s."trangThai" = 'SAN_SANG_BAN'
    )
  LOOP
    RAISE EXCEPTION 'RB37: soLuongTon cua dong % khong bang so may SAN_SANG_BAN', r."maDongSP";
  END LOOP;
END $$;
