-- DTAQ Retail - V1 initial schema (PostgreSQL)
-- Source design: DOCX Chuong 4, muc I.3 (14 thuc the) va muc II.3 (RB01-RB66).
-- Approved V1 amendment (2026-10-19): ChiTietHoaDon references the sold SanPham
-- by serial and persists sale-time price/tax/promotion snapshots; SanPhamLoi
-- references a serial and optional return/exchange header origins. These fields
-- are not claimed to have been present in the original DOCX diagram.
-- Identifiers are supplied by application code; the schema has no database-generated IDs.
-- This file creates no seed data, no compatibility table and no baseline.

CREATE TABLE "NhanVien" (
  "maNV" VARCHAR NOT NULL,
  "hoTen" VARCHAR NOT NULL,
  "soDienThoai" VARCHAR NOT NULL,
  "email" VARCHAR,
  "ngayVaoLam" DATE NOT NULL,
  "tenDangNhap" VARCHAR NOT NULL,
  "matKhau" VARCHAR NOT NULL,
  "loaiQuyen" VARCHAR NOT NULL,
  "trangThaiHoatDong" BOOLEAN NOT NULL,
  CONSTRAINT pk_nhanvien PRIMARY KEY ("maNV"),
  CONSTRAINT uq_nhanvien_tendangnhap UNIQUE ("tenDangNhap"),
  CONSTRAINT ck_nhanvien_hoten CHECK (btrim("hoTen") <> ''),
  CONSTRAINT ck_nhanvien_sodienthoai CHECK (btrim("soDienThoai") <> ''),
  CONSTRAINT ck_nhanvien_tendangnhap CHECK (btrim("tenDangNhap") <> ''),
  CONSTRAINT ck_nhanvien_matkhau CHECK (btrim("matKhau") <> ''),
  CONSTRAINT ck_nhanvien_loaiquyen CHECK (
    "loaiQuyen" IN ('NHAN_VIEN_QUAN_LY', 'NHAN_VIEN_BAN_HANG')
  )
);

CREATE TABLE "KhachHang" (
  "maKH" VARCHAR NOT NULL,
  "hoTen" VARCHAR NOT NULL,
  "soDienThoai" VARCHAR NOT NULL,
  "diaChi" VARCHAR,
  CONSTRAINT pk_khachhang PRIMARY KEY ("maKH"),
  CONSTRAINT ck_khachhang_hoten CHECK (btrim("hoTen") <> ''),
  CONSTRAINT ck_khachhang_sodienthoai CHECK (btrim("soDienThoai") <> '')
);

CREATE TABLE "DanhMucSanPham" (
  "maDanhMuc" VARCHAR NOT NULL,
  "tenDanhMuc" VARCHAR NOT NULL,
  "moTa" VARCHAR,
  CONSTRAINT pk_danhmucsanpham PRIMARY KEY ("maDanhMuc"),
  CONSTRAINT ck_danhmucsanpham_ten CHECK (btrim("tenDanhMuc") <> '')
);

CREATE UNIQUE INDEX uq_danhmucsanpham_ten_chuan_hoa
  ON "DanhMucSanPham" (lower(btrim("tenDanhMuc")));

CREATE TABLE "NhaCungCap" (
  "maNCC" VARCHAR NOT NULL,
  "tenNCC" VARCHAR NOT NULL,
  CONSTRAINT pk_nhacungcap PRIMARY KEY ("maNCC"),
  CONSTRAINT ck_nhacungcap_ten CHECK (btrim("tenNCC") <> '')
);

CREATE TABLE "DongSanPham" (
  "maDongSP" VARCHAR NOT NULL,
  "tenDongSP" VARCHAR NOT NULL,
  "maDanhMuc" VARCHAR NOT NULL,
  "donViTinh" VARCHAR NOT NULL,
  "soLuongTon" INTEGER NOT NULL,
  "thue" NUMERIC NOT NULL,
  "donGiaBanTruocThue" NUMERIC NOT NULL,
  "trangThaiKinhDoanh" BOOLEAN NOT NULL,
  "ngayNhapKho" DATE,
  "ngayCapNhapCuoi" DATE,
  CONSTRAINT pk_dongsanpham PRIMARY KEY ("maDongSP"),
  CONSTRAINT fk_dongsanpham_danhmuc FOREIGN KEY ("maDanhMuc")
    REFERENCES "DanhMucSanPham" ("maDanhMuc") ON DELETE RESTRICT,
  CONSTRAINT ck_dongsanpham_ten CHECK (btrim("tenDongSP") <> ''),
  CONSTRAINT ck_dongsanpham_donvitinh CHECK (btrim("donViTinh") <> ''),
  CONSTRAINT ck_dongsanpham_soluongton CHECK ("soLuongTon" >= 0),
  CONSTRAINT ck_dongsanpham_thue CHECK ("thue" >= 0 AND "thue" <= 100),
  CONSTRAINT ck_dongsanpham_dongia CHECK ("donGiaBanTruocThue" >= 0),
  CONSTRAINT ck_dongsanpham_ngaycapnhat CHECK (
    "ngayCapNhapCuoi" IS NULL
    OR "ngayNhapKho" IS NULL
    OR "ngayCapNhapCuoi" >= "ngayNhapKho"
  )
);

CREATE TABLE "SanPham" (
  "soSerial" VARCHAR NOT NULL,
  "maNCC" VARCHAR NOT NULL,
  "maDongSP" VARCHAR NOT NULL,
  "giaNhap" NUMERIC NOT NULL,
  "ngayNhapKhoBanDau" DATE NOT NULL,
  "trangThai" VARCHAR NOT NULL,
  "soLanDoiTra" INTEGER NOT NULL DEFAULT 0,
  "soLanSua" INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT pk_sanpham PRIMARY KEY ("soSerial"),
  CONSTRAINT uq_sanpham_serial_dong UNIQUE ("soSerial", "maDongSP"),
  CONSTRAINT fk_sanpham_nhacungcap FOREIGN KEY ("maNCC")
    REFERENCES "NhaCungCap" ("maNCC") ON DELETE RESTRICT,
  CONSTRAINT fk_sanpham_dongsanpham FOREIGN KEY ("maDongSP")
    REFERENCES "DongSanPham" ("maDongSP") ON DELETE RESTRICT,
  CONSTRAINT ck_sanpham_serial CHECK (btrim("soSerial") <> ''),
  CONSTRAINT ck_sanpham_gianhap CHECK ("giaNhap" >= 0),
  CONSTRAINT ck_sanpham_trangthai CHECK (
    "trangThai" IN (
      'SAN_SANG_BAN',
      'DA_BAN',
      'HANG_LOI',
      'DANG_BAO_HANH',
      'DA_TRA_NHA_CUNG_CAP',
      'DA_THANH_LY'
    )
  ),
  CONSTRAINT ck_sanpham_solandaitra CHECK ("soLanDoiTra" >= 0),
  CONSTRAINT ck_sanpham_solansua CHECK ("soLanSua" >= 0)
);

CREATE TABLE "KhuyenMai" (
  "maKM" VARCHAR NOT NULL,
  "tenKM" VARCHAR NOT NULL,
  "ngayBatDau" DATE NOT NULL,
  "ngayKetThuc" DATE NOT NULL,
  "tyLeGiam" NUMERIC NOT NULL,
  "moTa" VARCHAR,
  "dangApDung" BOOLEAN NOT NULL,
  "maDongSP" VARCHAR NOT NULL,
  CONSTRAINT pk_khuyenmai PRIMARY KEY ("maKM"),
  CONSTRAINT fk_khuyenmai_dongsanpham FOREIGN KEY ("maDongSP")
    REFERENCES "DongSanPham" ("maDongSP") ON DELETE RESTRICT,
  CONSTRAINT ck_khuyenmai_ten CHECK (btrim("tenKM") <> ''),
  CONSTRAINT ck_khuyenmai_ngay CHECK ("ngayBatDau" <= "ngayKetThuc"),
  CONSTRAINT ck_khuyenmai_tylegiam CHECK ("tyLeGiam" >= 0 AND "tyLeGiam" <= 100)
);

CREATE TABLE "HoaDon" (
  "maHD" VARCHAR NOT NULL,
  "ngayLap" DATE NOT NULL,
  "nhanVienLap" VARCHAR NOT NULL,
  "khachHang" VARCHAR NOT NULL,
  "trangThaiThanhToan" BOOLEAN NOT NULL,
  CONSTRAINT pk_hoadon PRIMARY KEY ("maHD"),
  CONSTRAINT fk_hoadon_nhanvien FOREIGN KEY ("nhanVienLap")
    REFERENCES "NhanVien" ("maNV") ON DELETE RESTRICT,
  CONSTRAINT fk_hoadon_khachhang FOREIGN KEY ("khachHang")
    REFERENCES "KhachHang" ("maKH") ON DELETE RESTRICT
);

CREATE TABLE "ChiTietHoaDon" (
  "maHD" VARCHAR NOT NULL,
  "soSerial" VARCHAR NOT NULL,
  "maDongSP" VARCHAR NOT NULL,
  "soLuong" INTEGER NOT NULL,
  "donGiaBanTaiLucBan" NUMERIC NOT NULL,
  "thueSuatTaiLucBan" NUMERIC NOT NULL,
  "maKM" VARCHAR,
  "tyLeGiamTaiLucBan" NUMERIC,
  "tienKhuyenMaiThucTe" NUMERIC NOT NULL,
  "tienTruocKhuyenMai" NUMERIC NOT NULL,
  "tienSauKhuyenMai" NUMERIC NOT NULL,
  "tienThue" NUMERIC NOT NULL,
  CONSTRAINT pk_chitiethoadon PRIMARY KEY ("maHD", "soSerial"),
  CONSTRAINT fk_chitiethoadon_hoadon FOREIGN KEY ("maHD")
    REFERENCES "HoaDon" ("maHD") ON DELETE RESTRICT,
  CONSTRAINT fk_chitiethoadon_sanpham FOREIGN KEY ("soSerial", "maDongSP")
    REFERENCES "SanPham" ("soSerial", "maDongSP") ON DELETE RESTRICT,
  CONSTRAINT fk_chitiethoadon_khuyenmai FOREIGN KEY ("maKM")
    REFERENCES "KhuyenMai" ("maKM") ON DELETE RESTRICT,
  CONSTRAINT ck_chitiethoadon_soluong CHECK ("soLuong" = 1),
  CONSTRAINT ck_chitiethoadon_dongia CHECK ("donGiaBanTaiLucBan" >= 0),
  CONSTRAINT ck_chitiethoadon_thue CHECK (
    "thueSuatTaiLucBan" >= 0 AND "thueSuatTaiLucBan" <= 100
  ),
  CONSTRAINT ck_chitiethoadon_tylegiam CHECK (
    "tyLeGiamTaiLucBan" IS NULL
    OR ("tyLeGiamTaiLucBan" >= 0 AND "tyLeGiamTaiLucBan" <= 100)
  ),
  CONSTRAINT ck_chitiethoadon_tienkhuyenmai CHECK ("tienKhuyenMaiThucTe" >= 0),
  CONSTRAINT ck_chitiethoadon_tientruoc CHECK ("tienTruocKhuyenMai" >= 0),
  CONSTRAINT ck_chitiethoadon_tiensau CHECK (
    "tienSauKhuyenMai" >= 0
    AND "tienSauKhuyenMai" <= "tienTruocKhuyenMai"
  ),
  CONSTRAINT ck_chitiethoadon_tienthue CHECK ("tienThue" >= 0)
);

CREATE TABLE "PhieuTra" (
  "maPhieuTra" VARCHAR NOT NULL,
  "nhanVienLap" VARCHAR NOT NULL,
  "hoaDon" VARCHAR NOT NULL,
  "ngayTao" DATE NOT NULL,
  "ngayHoanTat" DATE,
  "trangThaiDuyet" VARCHAR NOT NULL,
  "trangThaiXuLyPhieu" VARCHAR NOT NULL,
  "nhanVienDuyet" VARCHAR,
  "ngayDuyet" DATE,
  "lyDoDuyet" VARCHAR,
  "tienKhachDua" NUMERIC NOT NULL,
  CONSTRAINT pk_phieutra PRIMARY KEY ("maPhieuTra"),
  CONSTRAINT fk_phieutra_nhanvienlap FOREIGN KEY ("nhanVienLap")
    REFERENCES "NhanVien" ("maNV") ON DELETE RESTRICT,
  CONSTRAINT fk_phieutra_hoadon FOREIGN KEY ("hoaDon")
    REFERENCES "HoaDon" ("maHD") ON DELETE RESTRICT,
  CONSTRAINT fk_phieutra_nhanvienduyet FOREIGN KEY ("nhanVienDuyet")
    REFERENCES "NhanVien" ("maNV") ON DELETE RESTRICT,
  CONSTRAINT ck_phieutra_trangthaiduyet CHECK (
    "trangThaiDuyet" IN ('KHONG_CAN_DUYET', 'CHO_DUYET', 'DA_DUYET', 'TU_CHOI')
  ),
  CONSTRAINT ck_phieutra_trangthaixuly CHECK (
    "trangThaiXuLyPhieu" IN ('NHAP', 'CHO_XU_LY', 'HOAN_TAT', 'HUY')
  ),
  CONSTRAINT ck_phieutra_ngayhoantat CHECK (
    "ngayHoanTat" IS NULL OR "ngayHoanTat" >= "ngayTao"
  ),
  CONSTRAINT ck_phieutra_ngayduyet CHECK (
    "ngayDuyet" IS NULL OR "ngayDuyet" >= "ngayTao"
  ),
  CONSTRAINT ck_phieutra_ngayduyet_truoc_hoantat CHECK (
    "ngayDuyet" IS NULL
    OR "ngayHoanTat" IS NULL
    OR "ngayDuyet" <= "ngayHoanTat"
  ),
  CONSTRAINT ck_phieutra_hoantat_ngay CHECK (
    "trangThaiXuLyPhieu" <> 'HOAN_TAT' OR "ngayHoanTat" IS NOT NULL
  ),
  CONSTRAINT ck_phieutra_duyet_thongtin CHECK (
    "trangThaiDuyet" NOT IN ('DA_DUYET', 'TU_CHOI')
    OR ("nhanVienDuyet" IS NOT NULL AND "ngayDuyet" IS NOT NULL)
  ),
  CONSTRAINT ck_phieutra_tuchoi_lydo CHECK (
    "trangThaiDuyet" <> 'TU_CHOI'
    OR ("lyDoDuyet" IS NOT NULL AND btrim("lyDoDuyet") <> '')
  ),
  CONSTRAINT ck_phieutra_tienkhachdua CHECK ("tienKhachDua" >= 0)
);

CREATE TABLE "ChiTietPhieuTra" (
  "maPhieuTra" VARCHAR NOT NULL,
  "maHD" VARCHAR NOT NULL,
  "soSerial" VARCHAR NOT NULL,
  "tinhTrangHangTra" VARCHAR NOT NULL,
  "lyDoTra" VARCHAR NOT NULL,
  CONSTRAINT pk_chitietphieutra PRIMARY KEY ("maPhieuTra", "maHD", "soSerial"),
  CONSTRAINT fk_chitietphieutra_phieutra FOREIGN KEY ("maPhieuTra")
    REFERENCES "PhieuTra" ("maPhieuTra") ON DELETE RESTRICT,
  CONSTRAINT fk_chitietphieutra_chitiethoadon FOREIGN KEY ("maHD", "soSerial")
    REFERENCES "ChiTietHoaDon" ("maHD", "soSerial") ON DELETE RESTRICT,
  CONSTRAINT ck_chitietphieutra_tinhtrang CHECK (
    "tinhTrangHangTra" IN ('NGUYEN_VEN', 'HANG_LOI')
  ),
  CONSTRAINT ck_chitietphieutra_lydo CHECK (btrim("lyDoTra") <> '')
);

CREATE TABLE "PhieuDoi" (
  "maPhieuDoi" VARCHAR NOT NULL,
  "nhanVienLap" VARCHAR NOT NULL,
  "hoaDon" VARCHAR NOT NULL,
  "ngayHoanTat" DATE,
  "ngayTao" DATE NOT NULL,
  "trangThaiDuyet" VARCHAR NOT NULL,
  "trangThaiXuLyPhieu" VARCHAR NOT NULL,
  "tienKhachDuaThem" NUMERIC NOT NULL,
  "nhanVienDuyet" VARCHAR,
  "ngayDuyet" DATE,
  "lyDoDuyet" VARCHAR,
  CONSTRAINT pk_phieudoi PRIMARY KEY ("maPhieuDoi"),
  CONSTRAINT fk_phieudoi_nhanvienlap FOREIGN KEY ("nhanVienLap")
    REFERENCES "NhanVien" ("maNV") ON DELETE RESTRICT,
  CONSTRAINT fk_phieudoi_hoadon FOREIGN KEY ("hoaDon")
    REFERENCES "HoaDon" ("maHD") ON DELETE RESTRICT,
  CONSTRAINT fk_phieudoi_nhanvienduyet FOREIGN KEY ("nhanVienDuyet")
    REFERENCES "NhanVien" ("maNV") ON DELETE RESTRICT,
  CONSTRAINT ck_phieudoi_trangthaiduyet CHECK (
    "trangThaiDuyet" IN ('KHONG_CAN_DUYET', 'CHO_DUYET', 'DA_DUYET', 'TU_CHOI')
  ),
  CONSTRAINT ck_phieudoi_trangthaixuly CHECK (
    "trangThaiXuLyPhieu" IN ('NHAP', 'CHO_XU_LY', 'HOAN_TAT', 'HUY')
  ),
  CONSTRAINT ck_phieudoi_ngayhoantat CHECK (
    "ngayHoanTat" IS NULL OR "ngayHoanTat" >= "ngayTao"
  ),
  CONSTRAINT ck_phieudoi_ngayduyet CHECK (
    "ngayDuyet" IS NULL OR "ngayDuyet" >= "ngayTao"
  ),
  CONSTRAINT ck_phieudoi_ngayduyet_truoc_hoantat CHECK (
    "ngayDuyet" IS NULL
    OR "ngayHoanTat" IS NULL
    OR "ngayDuyet" <= "ngayHoanTat"
  ),
  CONSTRAINT ck_phieudoi_hoantat_ngay CHECK (
    "trangThaiXuLyPhieu" <> 'HOAN_TAT' OR "ngayHoanTat" IS NOT NULL
  ),
  CONSTRAINT ck_phieudoi_duyet_thongtin CHECK (
    "trangThaiDuyet" NOT IN ('DA_DUYET', 'TU_CHOI')
    OR ("nhanVienDuyet" IS NOT NULL AND "ngayDuyet" IS NOT NULL)
  ),
  CONSTRAINT ck_phieudoi_tuchoi_lydo CHECK (
    "trangThaiDuyet" <> 'TU_CHOI'
    OR ("lyDoDuyet" IS NOT NULL AND btrim("lyDoDuyet") <> '')
  ),
  CONSTRAINT ck_phieudoi_tienkhachduathem CHECK ("tienKhachDuaThem" >= 0)
);

CREATE TABLE "ChiTietPhieuDoi" (
  "maPhieuDoi" VARCHAR NOT NULL,
  "maHD" VARCHAR NOT NULL,
  "soSerial" VARCHAR NOT NULL,
  "lyDoDoi" VARCHAR NOT NULL,
  "chiTietSanPhamDoi" VARCHAR NOT NULL,
  CONSTRAINT pk_chitietphieudoi PRIMARY KEY ("maPhieuDoi", "maHD", "soSerial"),
  CONSTRAINT uq_chitietphieudoi_maythaythe UNIQUE ("maPhieuDoi", "chiTietSanPhamDoi"),
  CONSTRAINT fk_chitietphieudoi_phieudoi FOREIGN KEY ("maPhieuDoi")
    REFERENCES "PhieuDoi" ("maPhieuDoi") ON DELETE RESTRICT,
  CONSTRAINT fk_chitietphieudoi_chitiethoadon FOREIGN KEY ("maHD", "soSerial")
    REFERENCES "ChiTietHoaDon" ("maHD", "soSerial") ON DELETE RESTRICT,
  CONSTRAINT fk_chitietphieudoi_sanpham FOREIGN KEY ("chiTietSanPhamDoi")
    REFERENCES "SanPham" ("soSerial") ON DELETE RESTRICT,
  CONSTRAINT ck_chitietphieudoi_lydo CHECK (btrim("lyDoDoi") <> ''),
  CONSTRAINT ck_chitietphieudoi_khacmay CHECK ("chiTietSanPhamDoi" <> "soSerial")
);

CREATE TABLE "SanPhamLoi" (
  "maLoi" VARCHAR NOT NULL,
  "soSerial" VARCHAR NOT NULL,
  "maPhieuTra" VARCHAR,
  "maPhieuDoi" VARCHAR,
  "ghiChuXuLy" VARCHAR,
  "moTaLoi" VARCHAR NOT NULL,
  "ngayGhiNhan" DATE NOT NULL,
  "tinhTrangXuLy" VARCHAR NOT NULL,
  "ngayCapNhap" DATE,
  CONSTRAINT pk_sanphamloi PRIMARY KEY ("maLoi"),
  CONSTRAINT fk_sanphamloi_sanpham FOREIGN KEY ("soSerial")
    REFERENCES "SanPham" ("soSerial") ON DELETE RESTRICT,
  CONSTRAINT fk_sanphamloi_phieutra FOREIGN KEY ("maPhieuTra")
    REFERENCES "PhieuTra" ("maPhieuTra") ON DELETE RESTRICT,
  CONSTRAINT fk_sanphamloi_phieudoi FOREIGN KEY ("maPhieuDoi")
    REFERENCES "PhieuDoi" ("maPhieuDoi") ON DELETE RESTRICT,
  CONSTRAINT ck_sanphamloi_mota CHECK (btrim("moTaLoi") <> ''),
  CONSTRAINT ck_sanphamloi_trangthai CHECK (
    "tinhTrangXuLy" IN ('CHO_XU_LY', 'DA_GUI_BAO_HANH', 'DA_TRA_NHA_CUNG_CAP', 'THANH_LY')
  ),
  CONSTRAINT ck_sanphamloi_ngaycapnhat CHECK (
    "ngayCapNhap" IS NULL OR "ngayCapNhap" >= "ngayGhiNhan"
  )
);

-- Approved source-link integrity: when a defect record points to a return or
-- exchange header, the corresponding detail must contain the defective serial.
-- A NULL origin is allowed; no exclusivity rule is imposed on the two origins.
CREATE OR REPLACE FUNCTION fn_sanphamloi_nguon_hop_le()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW."maPhieuTra" IS NOT NULL AND NOT EXISTS (
    SELECT 1
    FROM "ChiTietPhieuTra" ct
    WHERE ct."maPhieuTra" = NEW."maPhieuTra"
      AND ct."soSerial" = NEW."soSerial"
  ) THEN
    RAISE EXCEPTION
      'SanPhamLoi %: soSerial % khong co trong ChiTietPhieuTra cua phieu tra %',
      NEW."maLoi", NEW."soSerial", NEW."maPhieuTra";
  END IF;

  IF NEW."maPhieuDoi" IS NOT NULL AND NOT EXISTS (
    SELECT 1
    FROM "ChiTietPhieuDoi" ct
    WHERE ct."maPhieuDoi" = NEW."maPhieuDoi"
      AND (
        ct."soSerial" = NEW."soSerial"
        OR ct."chiTietSanPhamDoi" = NEW."soSerial"
      )
  ) THEN
    RAISE EXCEPTION
      'SanPhamLoi %: soSerial % khong co trong ChiTietPhieuDoi cua phieu doi %',
      NEW."maLoi", NEW."soSerial", NEW."maPhieuDoi";
  END IF;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sanphamloi_nguon_hop_le
BEFORE INSERT OR UPDATE OF "soSerial", "maPhieuTra", "maPhieuDoi"
ON "SanPhamLoi"
FOR EACH ROW
EXECUTE FUNCTION fn_sanphamloi_nguon_hop_le();
