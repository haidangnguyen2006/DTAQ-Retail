-- DTAQ Retail - V3 password reset tickets (PostgreSQL)
-- Bảng DatLaiMatKhau lưu mã băm của mã đặt lại mật khẩu theo từng nhân viên (khóa chính maNV).
-- Mã băm, thời điểm tạo và thời điểm hết hạn do tầng ứng dụng sinh ra; CSDL không tự sinh giá trị.

CREATE TABLE "DatLaiMatKhau" (
  "maNV" VARCHAR NOT NULL,
  "maBam" VARCHAR NOT NULL,
  "taoLuc" TIMESTAMPTZ NOT NULL,
  "hetHanLuc" TIMESTAMPTZ NOT NULL,
  "soLanThu" INTEGER NOT NULL,
  "lanGuiCuoi" TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_datlaimatkhau PRIMARY KEY ("maNV"),
  CONSTRAINT fk_datlaimatkhau_nhanvien FOREIGN KEY ("maNV")
    REFERENCES "NhanVien" ("maNV") ON DELETE RESTRICT,
  CONSTRAINT ck_datlaimatkhau_solan CHECK ("soLanThu" >= 0)
);
