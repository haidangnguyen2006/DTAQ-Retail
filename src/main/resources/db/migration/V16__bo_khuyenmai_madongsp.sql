-- V15 đã chuyển liên kết dòng sản phẩm sang ChiTietKhuyenMai.
ALTER TABLE "KhuyenMai"
  DROP CONSTRAINT fk_khuyenmai_dongsanpham,
  DROP COLUMN "maDongSP";
