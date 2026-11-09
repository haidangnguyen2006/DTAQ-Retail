-- DTAQ Retail - V15 chi tiet khuyen mai (PostgreSQL)
-- ============================================================
-- MỤC ĐÍCH: tách quan hệ "khuyến mãi áp dụng cho nhiều dòng sản phẩm" khỏi
-- bảng "KhuyenMai" bằng bảng chi tiết "ChiTietKhuyenMai" (khóa chính
-- "maKM","maDongSP"). Mỗi khuyến mãi có thể áp dụng cho một hoặc nhiều dòng
-- sản phẩm; mỗi dòng sản phẩm có thể thuộc nhiều khuyến mãi.
--
-- MIGRATION CHỈ THÊM (ADDITIVE): V15 KHÔNG xóa cột "KhuyenMai"."maDongSP"
-- và KHÔNG xóa khóa ngoại "fk_khuyenmai_dongsanpham". Cột cũ được giữ lại để
-- mã hiện tại (entity, DAO, controller, giao diện khuyến mãi) tiếp tục chạy;
-- dữ liệu cũ được sao chép sang bảng chi tiết. Việc xóa cột cũ chỉ được thực
-- hiện ở migration kế tiếp (V16) sau khi tầng ứng dụng đọc/ghi bảng chi tiết.
--
-- TÍNH AN TOÀN KHI CHẠY LẠI: Flyway chỉ áp dụng V15 một lần và chạy trong một
-- giao dịch, nên lỗi giữa chừng sẽ hoàn nguyên toàn bộ. Mỗi "KhuyenMai" chỉ có
-- một cột "maDongSP" nên câu lệnh sao chép dữ liệu không tạo bản ghi trùng.
-- ============================================================

-- 1. Bảng chi tiết khuyến mãi theo dòng sản phẩm.
CREATE TABLE "ChiTietKhuyenMai" (
  "maKM" VARCHAR NOT NULL,
  "maDongSP" VARCHAR NOT NULL,
  CONSTRAINT pk_chitietkhuyenmai PRIMARY KEY ("maKM", "maDongSP"),
  CONSTRAINT fk_chitietkhuyenmai_khuyenmai FOREIGN KEY ("maKM")
    REFERENCES "KhuyenMai" ("maKM") ON DELETE CASCADE,
  CONSTRAINT fk_chitietkhuyenmai_dongsanpham FOREIGN KEY ("maDongSP")
    REFERENCES "DongSanPham" ("maDongSP") ON DELETE RESTRICT
);

-- 2. Sao chép quan hệ khuyến mãi - dòng sản phẩm đang có ở V1 sang bảng chi tiết.
--    Cột "KhuyenMai"."maDongSP" vẫn được giữ nguyên (xem phần mục đích ở trên).
INSERT INTO "ChiTietKhuyenMai" ("maKM", "maDongSP")
SELECT "maKM", "maDongSP" FROM "KhuyenMai";

-- 3. Chỉ mục tra cứu khuyến mãi đang hiệu lực theo dòng sản phẩm
--    (truy vấn ngược chiều so với khóa chính).
CREATE INDEX idx_ctkm_dong_san_pham
  ON "ChiTietKhuyenMai" ("maDongSP", "maKM");
