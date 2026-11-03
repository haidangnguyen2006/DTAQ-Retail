-- DTAQ Retail - V6 trigger soLuongTon (PostgreSQL)
-- ============================================================
-- MỤC ĐÍCH: bảo đảm RB37 ở tầng cơ sở dữ liệu — "DongSanPham"."soLuongTon"
-- luôn bằng đúng số máy (bản ghi "SanPham") của dòng đó đang ở trạng thái
-- 'SAN_SANG_BAN', kể cả khi dữ liệu bị thay đổi ngoài ứng dụng.
--
-- VÌ SAO DÙNG TRIGGER: V5 chỉ đồng bộ bộ đếm một lần, còn SanPhamController
-- chỉ tính lại khi thêm máy qua giao diện. Các đường ghi khác — trả hàng
-- (UC006), đổi trạng thái máy (UC011), hoặc câu lệnh SQL thủ công — không đi
-- qua controller, nên bộ đếm sẽ lệch nếu chỉ dựa vào tầng ứng dụng.
--
-- KHÔNG ĐỆ QUY: hàm chỉ UPDATE bảng "DongSanPham", còn trigger chỉ gắn trên
-- bảng "SanPham", nên việc cập nhật bộ đếm không kích hoạt lại chính trigger.
--
-- TÍNH IDEMPOTENT: câu lệnh đồng bộ dữ liệu bên dưới và CREATE OR REPLACE
-- FUNCTION đều chạy lại nhiều lần vẫn cho cùng kết quả; CREATE TRIGGER chỉ
-- được áp dụng một lần theo phiên bản Flyway V6.
-- ============================================================

-- 1. Chuẩn hóa dữ liệu hiện có (giống V5) trước khi bật trigger.
UPDATE "DongSanPham" d
SET "soLuongTon" = (
  SELECT count(*)
  FROM "SanPham" s
  WHERE s."maDongSP" = d."maDongSP"
    AND s."trangThai" = 'SAN_SANG_BAN'
);

-- 2. Hàm tính lại bộ đếm cho dòng sản phẩm bị ảnh hưởng.
CREATE OR REPLACE FUNCTION fn_dong_san_pham_cap_nhat_ton()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    UPDATE "DongSanPham" d
    SET "soLuongTon" = (
      SELECT count(*) FROM "SanPham" s
      WHERE s."maDongSP" = d."maDongSP" AND s."trangThai" = 'SAN_SANG_BAN'
    )
    WHERE d."maDongSP" = OLD."maDongSP";
    RETURN OLD;
  END IF;

  UPDATE "DongSanPham" d
  SET "soLuongTon" = (
    SELECT count(*) FROM "SanPham" s
    WHERE s."maDongSP" = d."maDongSP" AND s."trangThai" = 'SAN_SANG_BAN'
  )
  WHERE d."maDongSP" = NEW."maDongSP";

  -- Máy được chuyển sang dòng sản phẩm khác: dòng cũ cũng phải được tính lại.
  IF TG_OP = 'UPDATE' AND NEW."maDongSP" IS DISTINCT FROM OLD."maDongSP" THEN
    UPDATE "DongSanPham" d
    SET "soLuongTon" = (
      SELECT count(*) FROM "SanPham" s
      WHERE s."maDongSP" = d."maDongSP" AND s."trangThai" = 'SAN_SANG_BAN'
    )
    WHERE d."maDongSP" = OLD."maDongSP";
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Gắn trigger cho mọi thao tác ghi trên bảng "SanPham".
CREATE TRIGGER trg_sanpham_cap_nhat_ton
AFTER INSERT OR UPDATE OR DELETE ON "SanPham"
FOR EACH ROW EXECUTE FUNCTION fn_dong_san_pham_cap_nhat_ton();
