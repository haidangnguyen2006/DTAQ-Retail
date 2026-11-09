-- DTAQ Retail - V12: bỏ ràng buộc UNIQUE("filePath") trên lịch sử sao lưu.
-- Lý do: một tệp backup có thể có bản ghi sao lưu (AUTO/MANUAL/SAFETY) và nhiều bản ghi
-- phục hồi (MANUAL_RESTORE/AUTO_RESTORE); lịch sử là log, không phải định danh duy nhất.
-- Không sửa V11 vì đã áp dụng (checksum Flyway).
ALTER TABLE "LichSuSaoLuu" DROP CONSTRAINT IF EXISTS uq_lichsusaoluu_filepath;

-- Đồng bộ từ vựng sự cố sức khỏe với đặc tả FR015/FR016 (HealthMonitor sẽ dùng sau).
ALTER TABLE "SuCoSucKhoe" ALTER COLUMN "severity" TYPE VARCHAR(12);
ALTER TABLE "SuCoSucKhoe" ALTER COLUMN "trangThai" TYPE VARCHAR(20);
ALTER TABLE "SuCoSucKhoe" DROP CONSTRAINT IF EXISTS ck_sucosuckhoe_severity;
ALTER TABLE "SuCoSucKhoe" ADD CONSTRAINT ck_sucosuckhoe_severity
  CHECK ("severity" IN ('CRITICAL', 'SUSPICIOUS'));
ALTER TABLE "SuCoSucKhoe" DROP CONSTRAINT IF EXISTS ck_sucosuckhoe_trangthai;
ALTER TABLE "SuCoSucKhoe" ADD CONSTRAINT ck_sucosuckhoe_trangthai
  CHECK ("trangThai" IN ('PENDING', 'RESOLVED_RESTORED', 'AUTO_RESTORED', 'DISMISSED', 'FAILED'));
