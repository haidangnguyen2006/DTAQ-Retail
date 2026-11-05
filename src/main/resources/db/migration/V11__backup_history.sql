-- DTAQ Retail - V11 lịch sử sao lưu/phục hồi và sự cố sức khỏe (PostgreSQL)
-- ============================================================
-- MỤC ĐÍCH
--   * "LichSuSaoLuu": lịch sử các lần sao lưu/phục hồi để giao diện hiển thị nhanh.
--   * "SuCoSucKhoe": sự cố sức khỏe hệ thống (dùng cho HealthMonitor ở giai đoạn sau).
--
-- ĐÂY LÀ CACHE HIỂN THỊ, KHÔNG PHẢI NGUỒN SỰ THẬT
--   * Nguồn sự thật của một bản sao lưu là cặp tệp "<ten>.dump" + "<ten>.meta.json" trong
--     thư mục backup; kích thước và SHA-256 trong tệp meta là chuẩn để kiểm tra toàn vẹn.
--   * Khi thư mục backup và bảng lệch nhau (ví dụ người dùng chép tay tệp vào thư mục),
--     ứng dụng quét lại tệp rồi đồng bộ vào bảng; không bao giờ xóa tệp dựa vào bảng.
--   * Hai bảng có thể bị xóa và dựng lại bất cứ lúc nào mà không mất bản sao lưu.
--
-- VÌ SAO TẠO MIGRATION MỚI
--   V1-V10 đã được áp dụng trên các cơ sở dữ liệu hiện có; thêm bảng bằng V11 để bảo toàn
--   checksum Flyway. Không sửa V1-V10.
-- ============================================================

CREATE TABLE "LichSuSaoLuu" (
  "id" BIGSERIAL NOT NULL,
  "filePath" TEXT NOT NULL,
  "fileSize" BIGINT,
  "sha256" VARCHAR(64),
  "loai" VARCHAR(20) NOT NULL,
  "trangThai" VARCHAR(10) NOT NULL,
  "thongBao" TEXT,
  "createdAt" TIMESTAMP NOT NULL DEFAULT now(),
  "createdBy" VARCHAR(50),
  "phienBanPg" VARCHAR(50),
  "tenCsdl" VARCHAR(100),
  CONSTRAINT pk_lichsusaoluu PRIMARY KEY ("id"),
  CONSTRAINT uq_lichsusaoluu_filepath UNIQUE ("filePath"),
  CONSTRAINT ck_lichsusaoluu_loai CHECK (
    "loai" IN ('AUTO', 'MANUAL', 'SAFETY', 'AUTO_RESTORE', 'MANUAL_RESTORE')
  ),
  CONSTRAINT ck_lichsusaoluu_trangthai CHECK ("trangThai" IN ('SUCCESS', 'FAILED')),
  CONSTRAINT ck_lichsusaoluu_filepath CHECK (btrim("filePath") <> ''),
  CONSTRAINT ck_lichsusaoluu_filesize CHECK ("fileSize" IS NULL OR "fileSize" >= 0),
  CONSTRAINT ck_lichsusaoluu_sha256 CHECK (
    "sha256" IS NULL OR "sha256" ~ '^[0-9a-f]{64}$'
  )
);

-- Truy vấn thường gặp: bản ghi mới nhất, bản SUCCESS mới nhất.
CREATE INDEX idx_lichsusaoluu_createdat ON "LichSuSaoLuu" ("createdAt" DESC);

COMMENT ON TABLE "LichSuSaoLuu" IS
  'Cache hiển thị lịch sử sao lưu/phục hồi. Nguồn sự thật là cặp tệp .dump + .meta.json '
  'trong thư mục backup; bảng này có thể dựng lại bằng cách quét tệp.';

CREATE TABLE "SuCoSucKhoe" (
  "id" BIGSERIAL NOT NULL,
  "severity" VARCHAR(10) NOT NULL,
  "detail" TEXT,
  "trangThai" VARCHAR(10) NOT NULL,
  "backupUsed" TEXT,
  "detectedAt" TIMESTAMP NOT NULL DEFAULT now(),
  "resolvedAt" TIMESTAMP,
  "resolvedBy" VARCHAR(50),
  CONSTRAINT pk_sucosuckhoe PRIMARY KEY ("id"),
  CONSTRAINT ck_sucosuckhoe_severity CHECK ("severity" IN ('INFO', 'WARN', 'ERROR')),
  CONSTRAINT ck_sucosuckhoe_trangthai CHECK ("trangThai" IN ('OPEN', 'RESOLVED')),
  CONSTRAINT ck_sucosuckhoe_thoidiem CHECK (
    "resolvedAt" IS NULL OR "resolvedAt" >= "detectedAt"
  )
);

CREATE INDEX idx_sucosuckhoe_detectedat ON "SuCoSucKhoe" ("detectedAt" DESC);

COMMENT ON TABLE "SuCoSucKhoe" IS
  'Cache hiển thị sự cố sức khỏe hệ thống. Nguồn sự thật là nhật ký ứng dụng; '
  'bảng này có thể dựng lại và không thay thế nhật ký.';
