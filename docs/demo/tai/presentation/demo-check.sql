-- DTAQ - SELECT DOI SOAT DEMO PHAN TAI
-- Ngay soan: 2026-11-06 | Code doi chieu: ea45f69 | PostgreSQL
-- Di kem demo-script.md: canh A-H.
-- CACH CHAY: mo Query Tool/SQL Editor ket noi cung DB demo voi app.
-- Boi den tron 1 PHAN (comment + SQL den dau ;) roi Execute Selection.
-- Moi phan tu chay doc lap: da lap day du CTE, khong can setup/view/phan truoc.
-- Khong chay toan file thay demo: phai thao tac UI xen giua cac lan SELECT.
-- Toan bo chi doc. Khong tao/xoa/sua du lieu hay reset fixture.
-- Literal mac dinh theo demo 06/11/2026: SAM-TV-0071, DSP-2026-000101,
-- danh muc/KM DEMO Tai 0611 nhu ten tieng Viet trong cac cau SQL.
-- Ky doanh thu 01/06/2026-30/09/2026; ky rong 01/01/2000-31/01/2000.
-- Neu doi fixture/ngay/ky tren UI, doi literal o cac phan lien quan cho khop.
-- Expected la dieu kien kiem tra, chua phai ket qua DB live da xac nhan.
-- Du lieu seed co the da thay doi. Khong doc mat khau bang script nay.

-- ============================================================================
-- 00 | DB DICH VA NGAY
-- CHAY LUC: Truoc demo
-- CAN THAY: DB dung voi app; ngay DB dung ngay tham chieu.
-- ============================================================================
SELECT current_database() AS "databaseDangDung", CURRENT_DATE AS "ngayDB", current_setting('TimeZone') AS "muiGioDB";

-- ============================================================================
-- A1 | TAI KHOAN DEMO VA VAI TRO
-- CHAY LUC: Truoc A2 / D3
-- CAN THAY: Du 2 tai khoan, trangThaiHoatDong=true; khong xac minh mat khau.
-- ============================================================================
SELECT "tenDangNhap", "hoTen", "loaiQuyen", "trangThaiHoatDong" FROM "NhanVien" WHERE "tenDangNhap" IN ('NVBH2026001','NVQL2026001') ORDER BY "tenDangNhap";

-- ============================================================================
-- B1 | DANH MUC MOI VA CHONG TRUNG
-- CHAY LUC: Truoc B2, sau B4/B6
-- CAN THAY: Truoc 0 dong; sau tao 1 dong; thu trung van 1 dong.
-- ============================================================================
SELECT "maDanhMuc", "tenDanhMuc", "moTa"
FROM "DanhMucSanPham"
WHERE lower(btrim("tenDanhMuc")) =
      lower(btrim('DEMO Tài 0611 - Phụ kiện thử nghiệm'));

-- ============================================================================
-- B2 | TIVI DANG CO DONG THAM CHIEU
-- CHAY LUC: Truoc va sau B8
-- CAN THAY: Tivi van ton tai, soDongDangThamChieu > 0; xoa bi chan.
-- ============================================================================
SELECT dm."maDanhMuc", dm."tenDanhMuc",
       COUNT(dsp."maDongSP") AS "soDongDangThamChieu"
FROM "DanhMucSanPham" dm
LEFT JOIN "DongSanPham" dsp ON dsp."maDanhMuc" = dm."maDanhMuc"
WHERE dm."maDanhMuc" = 'DM-2026-000001'
GROUP BY dm."maDanhMuc", dm."tenDanhMuc";

-- ============================================================================
-- B3 | SAU XOA DANH MUC DEMO
-- CHAY LUC: Sau B9/B10
-- CAN THAY: 0 dong; phai da thay 1 dong truoc xoa o B1.
-- LUU Y: Tim ten fixture da chot, khong can tu dien ma sinh. Doi literal neu da doi ten.
-- ============================================================================
SELECT "maDanhMuc", "tenDanhMuc", "moTa"
FROM "DanhMucSanPham"
WHERE lower(btrim("tenDanhMuc")) =
      lower(btrim('DEMO Tài 0611 - Phụ kiện thử nghiệm'));

-- ============================================================================
-- C1 | HO SO LOI VA TRANG THAI MAY
-- CHAY LUC: Truoc C3, sau C5/C9
-- CAN THAY: SAN_SANG_BAN -> CHO_XU_LY/HANG_LOI -> DA_GUI_BAO_HANH/DANG_BAO_HANH.
-- ============================================================================
SELECT s."soSerial", s."maDongSP", s."trangThai" AS "trangThaiMay",
       l."maLoi", l."tinhTrangXuLy", l."moTaLoi",
       l."ngayGhiNhan", l."ghiChuXuLy",
       l."maPhieuTra", l."maPhieuDoi"
FROM "SanPham" s
LEFT JOIN "SanPhamLoi" l ON l."soSerial" = s."soSerial"
WHERE s."soSerial" = 'SAM-TV-0071'
ORDER BY l."ngayGhiNhan", l."maLoi";

-- ============================================================================
-- C2 | COUNTER DONG SAN PHAM
-- CHAY LUC: Truoc C3, sau C5/C7/C9
-- CAN THAY: counterKhop=true; ghi loi giam 1; thu trung/gui bao hanh khong giam them.
-- LUU Y: Ghi so truoc demo de so; khong thao tac dong thoi cung dong.
-- ============================================================================
SELECT d."maDongSP", d."tenDongSP", d."soLuongTon",
       COUNT(s."soSerial") FILTER
         (WHERE s."trangThai" = 'SAN_SANG_BAN') AS "demMayBanDuoc",
       d."soLuongTon" = COUNT(s."soSerial") FILTER
         (WHERE s."trangThai" = 'SAN_SANG_BAN') AS "counterKhop"
FROM "DongSanPham" d
LEFT JOIN "SanPham" s ON s."maDongSP" = d."maDongSP"
WHERE d."maDongSP" =
      (SELECT "maDongSP" FROM "SanPham" WHERE "soSerial" = 'SAM-TV-0071')
GROUP BY d."maDongSP", d."tenDongSP", d."soLuongTon";

-- ============================================================================
-- C3 | SO HO SO LOI
-- CHAY LUC: Truoc C3, sau C5/C7
-- CAN THAY: Ghi loi tang 1; thu trung khong tang them. So voi baseline truoc demo.
-- ============================================================================
SELECT COUNT(*) AS "soHoSoCuaSerial"
FROM "SanPhamLoi" WHERE "soSerial" = 'SAM-TV-0071';

-- ============================================================================
-- E1 | KM VA HIEU LUC
-- CHAY LUC: Truoc E2, sau E4/E6/E7
-- CAN THAY: Sau tao 10%, co=true; sau dung co=false; hieu luc con phu thuoc ngay DB.
-- ============================================================================
SELECT d."maDongSP", d."tenDongSP", k."maKM", k."tenKM",
       k."tyLeGiam", k."ngayBatDau", k."ngayKetThuc", k."dangApDung",
       (k."dangApDung" AND CURRENT_DATE BETWEEN k."ngayBatDau"
          AND k."ngayKetThuc") AS "hieuLucHomNay"
FROM "DongSanPham" d
LEFT JOIN "KhuyenMai" k ON k."maDongSP" = d."maDongSP"
WHERE d."maDongSP" = 'DSP-2026-000101'
ORDER BY k."ngayBatDau", k."maKM";

-- ============================================================================
-- E2 | KM BAT GIAO KHOANG DEMO
-- CHAY LUC: Truoc E2 va sau E4
-- CAN THAY: Truoc tao 0 dong; sau tao thay KM chinh.
-- ============================================================================
SELECT "maKM", "tenKM", "ngayBatDau", "ngayKetThuc"
FROM "KhuyenMai"
WHERE "maDongSP" = 'DSP-2026-000101'
  AND "dangApDung" = true
  AND "ngayBatDau" <= DATE '2026-11-13'
  AND "ngayKetThuc" >= DATE '2026-11-06';

-- ============================================================================
-- E3 | THU CHONG LAN KHONG TAO BAN GHI
-- CHAY LUC: Sau E6
-- CAN THAY: 0 dong neu ten thu chua ton tai truoc demo.
-- ============================================================================
SELECT "maKM", "tenKM", "dangApDung"
FROM "KhuyenMai"
WHERE "maDongSP" = 'DSP-2026-000101'
  AND "tenKM" = 'DEMO Tài 0611 - Kiểm tra chồng lấn';

-- ============================================================================
-- F1 | TON LAU CHUNG - CHI TIET
-- CHAY LUC: Sau F2
-- CAN THAY: Cac serial/trang thai/tuoi ton khop UI; nguong >=60, khong filter phu.
-- ============================================================================
WITH p AS (
  SELECT 60::int AS nguong_ngay, false AS chi_san_sang,
         NULL::text AS ma_dong, ''::text AS tu_khoa,
         NULL::text AS nganh_hang
), tap AS (
  SELECT s."soSerial", d."maDongSP", d."tenDongSP", dm."tenDanhMuc",
         s."ngayNhapKhoBanDau", s."trangThai",
         CURRENT_DATE - s."ngayNhapKhoBanDau" AS "soNgayTon"
  FROM "SanPham" s
  JOIN "DongSanPham" d ON d."maDongSP" = s."maDongSP"
  JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = d."maDanhMuc"
  CROSS JOIN p
  WHERE s."trangThai" IN ('SAN_SANG_BAN','HANG_LOI','DANG_BAO_HANH')
    AND (NOT p.chi_san_sang OR s."trangThai" = 'SAN_SANG_BAN')
    AND CURRENT_DATE - s."ngayNhapKhoBanDau" >= p.nguong_ngay
    AND (p.ma_dong IS NULL OR d."maDongSP" = p.ma_dong)
    AND (p.nganh_hang IS NULL OR dm."tenDanhMuc" = p.nganh_hang)
    AND (p.tu_khoa = ''
      OR strpos(lower(d."maDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(d."tenDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(s."soSerial"), lower(btrim(p.tu_khoa))) > 0)
)
SELECT * FROM tap
ORDER BY "tenDongSP", "ngayNhapKhoBanDau", "soSerial";

-- ============================================================================
-- F2 | TON LAU CHUNG - 3 KPI
-- CHAY LUC: Sau F2/F3
-- CAN THAY: 3 cot khop 3 card UI.
-- ============================================================================
WITH p AS (
  SELECT 60::int AS nguong_ngay, false AS chi_san_sang,
         NULL::text AS ma_dong, ''::text AS tu_khoa,
         NULL::text AS nganh_hang
), tap AS (
  SELECT s."soSerial", d."maDongSP", d."tenDongSP", dm."tenDanhMuc",
         s."ngayNhapKhoBanDau", s."trangThai",
         CURRENT_DATE - s."ngayNhapKhoBanDau" AS "soNgayTon"
  FROM "SanPham" s
  JOIN "DongSanPham" d ON d."maDongSP" = s."maDongSP"
  JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = d."maDanhMuc"
  CROSS JOIN p
  WHERE s."trangThai" IN ('SAN_SANG_BAN','HANG_LOI','DANG_BAO_HANH')
    AND (NOT p.chi_san_sang OR s."trangThai" = 'SAN_SANG_BAN')
    AND CURRENT_DATE - s."ngayNhapKhoBanDau" >= p.nguong_ngay
    AND (p.ma_dong IS NULL OR d."maDongSP" = p.ma_dong)
    AND (p.nganh_hang IS NULL OR dm."tenDanhMuc" = p.nganh_hang)
    AND (p.tu_khoa = ''
      OR strpos(lower(d."maDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(d."tenDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(s."soSerial"), lower(btrim(p.tu_khoa))) > 0)
)
SELECT COUNT(*) AS "tongMayTonLau", COUNT(DISTINCT "maDongSP") AS "soDongBiAnhHuong", COALESCE(MAX("soNgayTon"),0) AS "tonLauNhat" FROM tap;

-- ============================================================================
-- F3 | CHI SAN SANG BAN - CHI TIET
-- CHAY LUC: Sau F4
-- CAN THAY: Chi SAN_SANG_BAN; loai may demo dang bao hanh.
-- ============================================================================
WITH p AS (
  SELECT 60::int AS nguong_ngay, true AS chi_san_sang,
         NULL::text AS ma_dong, ''::text AS tu_khoa,
         NULL::text AS nganh_hang
), tap AS (
  SELECT s."soSerial", d."maDongSP", d."tenDongSP", dm."tenDanhMuc",
         s."ngayNhapKhoBanDau", s."trangThai",
         CURRENT_DATE - s."ngayNhapKhoBanDau" AS "soNgayTon"
  FROM "SanPham" s
  JOIN "DongSanPham" d ON d."maDongSP" = s."maDongSP"
  JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = d."maDanhMuc"
  CROSS JOIN p
  WHERE s."trangThai" IN ('SAN_SANG_BAN','HANG_LOI','DANG_BAO_HANH')
    AND (NOT p.chi_san_sang OR s."trangThai" = 'SAN_SANG_BAN')
    AND CURRENT_DATE - s."ngayNhapKhoBanDau" >= p.nguong_ngay
    AND (p.ma_dong IS NULL OR d."maDongSP" = p.ma_dong)
    AND (p.nganh_hang IS NULL OR dm."tenDanhMuc" = p.nganh_hang)
    AND (p.tu_khoa = ''
      OR strpos(lower(d."maDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(d."tenDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(s."soSerial"), lower(btrim(p.tu_khoa))) > 0)
)
SELECT * FROM tap
ORDER BY "tenDongSP", "ngayNhapKhoBanDau", "soSerial";

-- ============================================================================
-- F4 | CHI SAN SANG BAN - 3 KPI
-- CHAY LUC: Sau F4
-- CAN THAY: KPI cho tap chi san sang ban.
-- ============================================================================
WITH p AS (
  SELECT 60::int AS nguong_ngay, true AS chi_san_sang,
         NULL::text AS ma_dong, ''::text AS tu_khoa,
         NULL::text AS nganh_hang
), tap AS (
  SELECT s."soSerial", d."maDongSP", d."tenDongSP", dm."tenDanhMuc",
         s."ngayNhapKhoBanDau", s."trangThai",
         CURRENT_DATE - s."ngayNhapKhoBanDau" AS "soNgayTon"
  FROM "SanPham" s
  JOIN "DongSanPham" d ON d."maDongSP" = s."maDongSP"
  JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = d."maDanhMuc"
  CROSS JOIN p
  WHERE s."trangThai" IN ('SAN_SANG_BAN','HANG_LOI','DANG_BAO_HANH')
    AND (NOT p.chi_san_sang OR s."trangThai" = 'SAN_SANG_BAN')
    AND CURRENT_DATE - s."ngayNhapKhoBanDau" >= p.nguong_ngay
    AND (p.ma_dong IS NULL OR d."maDongSP" = p.ma_dong)
    AND (p.nganh_hang IS NULL OR dm."tenDanhMuc" = p.nganh_hang)
    AND (p.tu_khoa = ''
      OR strpos(lower(d."maDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(d."tenDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(s."soSerial"), lower(btrim(p.tu_khoa))) > 0)
)
SELECT COUNT(*) AS "tongMayTonLau", COUNT(DISTINCT "maDongSP") AS "soDongBiAnhHuong", COALESCE(MAX("soNgayTon"),0) AS "tonLauNhat" FROM tap;

-- ============================================================================
-- F5 | DRILL-DOWN DSP-2026-000001 - CHI TIET/EXCEL
-- CHAY LUC: F5: bo tick ready, tu khoa trong, nganh hang Tat ca, double-click dong DSP-2026-000001; sau F7
-- CAN THAY: Tung serial/cot khop UI va Excel.
-- LUU Y: Neu drill-down dong khac, doi ma_dong ca F5 va F6. Filter phai giong UI.
-- ============================================================================
WITH p AS (
  SELECT 60::int AS nguong_ngay, false AS chi_san_sang,
         'DSP-2026-000001'::text AS ma_dong, ''::text AS tu_khoa,
         NULL::text AS nganh_hang
), tap AS (
  SELECT s."soSerial", d."maDongSP", d."tenDongSP", dm."tenDanhMuc",
         s."ngayNhapKhoBanDau", s."trangThai",
         CURRENT_DATE - s."ngayNhapKhoBanDau" AS "soNgayTon"
  FROM "SanPham" s
  JOIN "DongSanPham" d ON d."maDongSP" = s."maDongSP"
  JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = d."maDanhMuc"
  CROSS JOIN p
  WHERE s."trangThai" IN ('SAN_SANG_BAN','HANG_LOI','DANG_BAO_HANH')
    AND (NOT p.chi_san_sang OR s."trangThai" = 'SAN_SANG_BAN')
    AND CURRENT_DATE - s."ngayNhapKhoBanDau" >= p.nguong_ngay
    AND (p.ma_dong IS NULL OR d."maDongSP" = p.ma_dong)
    AND (p.nganh_hang IS NULL OR dm."tenDanhMuc" = p.nganh_hang)
    AND (p.tu_khoa = ''
      OR strpos(lower(d."maDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(d."tenDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(s."soSerial"), lower(btrim(p.tu_khoa))) > 0)
)
SELECT * FROM tap
ORDER BY "tenDongSP", "ngayNhapKhoBanDau", "soSerial";

-- ============================================================================
-- F6 | DRILL-DOWN DSP-2026-000001 - 3 KPI
-- CHAY LUC: Sau F5
-- CAN THAY: 3 KPI cho tap drill-down.
-- ============================================================================
WITH p AS (
  SELECT 60::int AS nguong_ngay, false AS chi_san_sang,
         'DSP-2026-000001'::text AS ma_dong, ''::text AS tu_khoa,
         NULL::text AS nganh_hang
), tap AS (
  SELECT s."soSerial", d."maDongSP", d."tenDongSP", dm."tenDanhMuc",
         s."ngayNhapKhoBanDau", s."trangThai",
         CURRENT_DATE - s."ngayNhapKhoBanDau" AS "soNgayTon"
  FROM "SanPham" s
  JOIN "DongSanPham" d ON d."maDongSP" = s."maDongSP"
  JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = d."maDanhMuc"
  CROSS JOIN p
  WHERE s."trangThai" IN ('SAN_SANG_BAN','HANG_LOI','DANG_BAO_HANH')
    AND (NOT p.chi_san_sang OR s."trangThai" = 'SAN_SANG_BAN')
    AND CURRENT_DATE - s."ngayNhapKhoBanDau" >= p.nguong_ngay
    AND (p.ma_dong IS NULL OR d."maDongSP" = p.ma_dong)
    AND (p.nganh_hang IS NULL OR dm."tenDanhMuc" = p.nganh_hang)
    AND (p.tu_khoa = ''
      OR strpos(lower(d."maDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(d."tenDongSP"), lower(btrim(p.tu_khoa))) > 0
      OR strpos(lower(s."soSerial"), lower(btrim(p.tu_khoa))) > 0)
)
SELECT COUNT(*) AS "tongMayTonLau", COUNT(DISTINCT "maDongSP") AS "soDongBiAnhHuong", COALESCE(MAX("soNgayTon"),0) AS "tonLauNhat" FROM tap;

-- ============================================================================
-- G1 | DOANH THU - 4 KPI VA SO PHIEU
-- CHAY LUC: Sau G3/G4 va moi lan doi nhom G5
-- CAN THAY: 3 so tien/so HD/so phieu khop UI; doi nhom khong doi KPI.
-- ============================================================================
WITH p AS (
  SELECT DATE '2026-06-01' AS tu, DATE '2026-09-30' AS den
), hd AS (
  SELECT h."maHD"
  FROM "HoaDon" h CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true
    AND h."ngayLap" BETWEEN p.tu AND p.den
), ban AS (
  SELECT COALESCE(SUM(c."tienSauKhuyenMai"),0) AS hang,
         COALESCE(SUM(c."tienThue"),0) AS thue
  FROM hd JOIN "ChiTietHoaDon" c ON c."maHD" = hd."maHD"
), tra AS (
  SELECT t."maPhieuTra", c."tienSauKhuyenMai", c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD"
                           AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
), doi AS (
  SELECT DISTINCT d."maPhieuDoi"
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD"
                           AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
), tien_tra AS (
  SELECT COALESCE(SUM("tienSauKhuyenMai"),0) AS hang,
         COALESCE(SUM("tienThue"),0) AS thue FROM tra
)
SELECT ban.hang - tien_tra.hang AS "tienHangTruocThue",
       ban.thue - tien_tra.thue AS "tienThue",
       ban.hang + ban.thue - tien_tra.hang - tien_tra.thue AS "tongDoanhThu",
       (SELECT COUNT(*) FROM hd) AS "soHoaDonThanhCong",
       (SELECT COUNT(DISTINCT "maPhieuTra") FROM tra)
         + (SELECT COUNT(*) FROM doi) AS "soPhieuDoiTra"
FROM ban CROSS JOIN tien_tra;

-- ============================================================================
-- G2 | DOANH THU - CHI TIET NGUON/EXCEL
-- CHAY LUC: Sau G7
-- CAN THAY: 5 cot dau khop gia tri dong Excel, khong nham thu tu; 2 cot cuoi giai thich nguon.
-- ============================================================================
WITH p AS (
  SELECT DATE '2026-06-01' AS tu, DATE '2026-09-30' AS den
), tap AS (
  SELECT h."ngayLap" AS ngay, c."maDongSP",
         'BAN'::text AS nguon, NULL::text AS ma_phieu,
         SUM(c."tienSauKhuyenMai") AS hang, SUM(c."tienThue") AS thue
  FROM "HoaDon" h
  JOIN "ChiTietHoaDon" c ON c."maHD" = h."maHD"
  CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true AND h."ngayLap" BETWEEN p.tu AND p.den
  GROUP BY h."ngayLap", c."maDongSP"
  UNION ALL
  SELECT t."ngayHoanTat", c."maDongSP", 'TRA', t."maPhieuTra",
         -c."tienSauKhuyenMai", -c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
  UNION ALL
  SELECT d."ngayHoanTat", c."maDongSP", 'DOI', d."maPhieuDoi", 0, 0
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
)
SELECT ngay, "maDongSP", hang AS "tienHang", thue AS "tienThue",
       hang + thue AS "tongThanhToan", nguon, ma_phieu
FROM tap ORDER BY ngay, "maDongSP", nguon, ma_phieu;

-- ============================================================================
-- G3 | BIEU DO THEO NGAY
-- CHAY LUC: G5: chon NGAY tren UI
-- CAN THAY: So tien VND/trieu VND khop cot; tuan bat dau thu Hai.
-- ============================================================================
WITH p AS (
  SELECT DATE '2026-06-01' AS tu, DATE '2026-09-30' AS den
), tap AS (
  SELECT h."ngayLap" AS ngay, c."maDongSP",
         'BAN'::text AS nguon, NULL::text AS ma_phieu,
         SUM(c."tienSauKhuyenMai") AS hang, SUM(c."tienThue") AS thue
  FROM "HoaDon" h
  JOIN "ChiTietHoaDon" c ON c."maHD" = h."maHD"
  CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true AND h."ngayLap" BETWEEN p.tu AND p.den
  GROUP BY h."ngayLap", c."maDongSP"
  UNION ALL
  SELECT t."ngayHoanTat", c."maDongSP", 'TRA', t."maPhieuTra",
         -c."tienSauKhuyenMai", -c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
  UNION ALL
  SELECT d."ngayHoanTat", c."maDongSP", 'DOI', d."maPhieuDoi", 0, 0
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
)
SELECT date_trunc('day', lich.ngay)::date AS "mocKy",
       SUM(COALESCE(t.tien,0)) AS "doanhThuVND",
       SUM(COALESCE(t.tien,0)) / 1000000.0 AS "doanhThuTrieuVND"
FROM p
CROSS JOIN LATERAL generate_series(p.tu::timestamp, p.den::timestamp,
                                  INTERVAL '1 day') AS lich(ngay)
LEFT JOIN (SELECT ngay, SUM(hang + thue) AS tien FROM tap GROUP BY ngay) t
  ON t.ngay = lich.ngay::date
GROUP BY date_trunc('day', lich.ngay)::date
ORDER BY "mocKy";

-- ============================================================================
-- G4 | BIEU DO THEO TUAN
-- CHAY LUC: G5: chon TUAN tren UI
-- CAN THAY: So tien VND/trieu VND khop cot; tuan bat dau thu Hai.
-- ============================================================================
WITH p AS (
  SELECT DATE '2026-06-01' AS tu, DATE '2026-09-30' AS den
), tap AS (
  SELECT h."ngayLap" AS ngay, c."maDongSP",
         'BAN'::text AS nguon, NULL::text AS ma_phieu,
         SUM(c."tienSauKhuyenMai") AS hang, SUM(c."tienThue") AS thue
  FROM "HoaDon" h
  JOIN "ChiTietHoaDon" c ON c."maHD" = h."maHD"
  CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true AND h."ngayLap" BETWEEN p.tu AND p.den
  GROUP BY h."ngayLap", c."maDongSP"
  UNION ALL
  SELECT t."ngayHoanTat", c."maDongSP", 'TRA', t."maPhieuTra",
         -c."tienSauKhuyenMai", -c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
  UNION ALL
  SELECT d."ngayHoanTat", c."maDongSP", 'DOI', d."maPhieuDoi", 0, 0
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
)
SELECT date_trunc('week', lich.ngay)::date AS "mocKy",
       SUM(COALESCE(t.tien,0)) AS "doanhThuVND",
       SUM(COALESCE(t.tien,0)) / 1000000.0 AS "doanhThuTrieuVND"
FROM p
CROSS JOIN LATERAL generate_series(p.tu::timestamp, p.den::timestamp,
                                  INTERVAL '1 day') AS lich(ngay)
LEFT JOIN (SELECT ngay, SUM(hang + thue) AS tien FROM tap GROUP BY ngay) t
  ON t.ngay = lich.ngay::date
GROUP BY date_trunc('week', lich.ngay)::date
ORDER BY "mocKy";

-- ============================================================================
-- G5 | BIEU DO THEO THANG
-- CHAY LUC: G5: chon THANG tren UI
-- CAN THAY: So tien VND/trieu VND khop cot; tuan bat dau thu Hai.
-- ============================================================================
WITH p AS (
  SELECT DATE '2026-06-01' AS tu, DATE '2026-09-30' AS den
), tap AS (
  SELECT h."ngayLap" AS ngay, c."maDongSP",
         'BAN'::text AS nguon, NULL::text AS ma_phieu,
         SUM(c."tienSauKhuyenMai") AS hang, SUM(c."tienThue") AS thue
  FROM "HoaDon" h
  JOIN "ChiTietHoaDon" c ON c."maHD" = h."maHD"
  CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true AND h."ngayLap" BETWEEN p.tu AND p.den
  GROUP BY h."ngayLap", c."maDongSP"
  UNION ALL
  SELECT t."ngayHoanTat", c."maDongSP", 'TRA', t."maPhieuTra",
         -c."tienSauKhuyenMai", -c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
  UNION ALL
  SELECT d."ngayHoanTat", c."maDongSP", 'DOI', d."maPhieuDoi", 0, 0
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
)
SELECT date_trunc('month', lich.ngay)::date AS "mocKy",
       SUM(COALESCE(t.tien,0)) AS "doanhThuVND",
       SUM(COALESCE(t.tien,0)) / 1000000.0 AS "doanhThuTrieuVND"
FROM p
CROSS JOIN LATERAL generate_series(p.tu::timestamp, p.den::timestamp,
                                  INTERVAL '1 day') AS lich(ngay)
LEFT JOIN (SELECT ngay, SUM(hang + thue) AS tien FROM tap GROUP BY ngay) t
  ON t.ngay = lich.ngay::date
GROUP BY date_trunc('month', lich.ngay)::date
ORDER BY "mocKy";

-- ============================================================================
-- G6 | TOP 5 DONG SAN PHAM DUONG
-- CHAY LUC: Sau G6
-- CAN THAY: Ten/gia tri khop donut; dong am/0 bi loai; dong hang thu 5 co the khac thu tu.
-- ============================================================================
WITH p AS (
  SELECT DATE '2026-06-01' AS tu, DATE '2026-09-30' AS den
), tap AS (
  SELECT h."ngayLap" AS ngay, c."maDongSP",
         'BAN'::text AS nguon, NULL::text AS ma_phieu,
         SUM(c."tienSauKhuyenMai") AS hang, SUM(c."tienThue") AS thue
  FROM "HoaDon" h
  JOIN "ChiTietHoaDon" c ON c."maHD" = h."maHD"
  CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true AND h."ngayLap" BETWEEN p.tu AND p.den
  GROUP BY h."ngayLap", c."maDongSP"
  UNION ALL
  SELECT t."ngayHoanTat", c."maDongSP", 'TRA', t."maPhieuTra",
         -c."tienSauKhuyenMai", -c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
  UNION ALL
  SELECT d."ngayHoanTat", c."maDongSP", 'DOI', d."maPhieuDoi", 0, 0
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
)
SELECT t."maDongSP", d."tenDongSP", SUM(t.hang + t.thue) AS "giaTriDuongVND"
FROM tap t JOIN "DongSanPham" d ON d."maDongSP" = t."maDongSP"
GROUP BY t."maDongSP", d."tenDongSP"
HAVING SUM(t.hang + t.thue) > 0
ORDER BY "giaTriDuongVND" DESC
LIMIT 5;

-- ============================================================================
-- G7 | KY RONG - KPI
-- CHAY LUC: Preflight va sau G8 tren UI
-- CAN THAY: Cac so=0; chay them G8 de khang dinh khong phat sinh.
-- ============================================================================
WITH p AS (
  SELECT DATE '2000-01-01' AS tu, DATE '2000-01-31' AS den
), hd AS (
  SELECT h."maHD"
  FROM "HoaDon" h CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true
    AND h."ngayLap" BETWEEN p.tu AND p.den
), ban AS (
  SELECT COALESCE(SUM(c."tienSauKhuyenMai"),0) AS hang,
         COALESCE(SUM(c."tienThue"),0) AS thue
  FROM hd JOIN "ChiTietHoaDon" c ON c."maHD" = hd."maHD"
), tra AS (
  SELECT t."maPhieuTra", c."tienSauKhuyenMai", c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD"
                           AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
), doi AS (
  SELECT DISTINCT d."maPhieuDoi"
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD"
                           AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
), tien_tra AS (
  SELECT COALESCE(SUM("tienSauKhuyenMai"),0) AS hang,
         COALESCE(SUM("tienThue"),0) AS thue FROM tra
)
SELECT ban.hang - tien_tra.hang AS "tienHangTruocThue",
       ban.thue - tien_tra.thue AS "tienThue",
       ban.hang + ban.thue - tien_tra.hang - tien_tra.thue AS "tongDoanhThu",
       (SELECT COUNT(*) FROM hd) AS "soHoaDonThanhCong",
       (SELECT COUNT(DISTINCT "maPhieuTra") FROM tra)
         + (SELECT COUNT(*) FROM doi) AS "soPhieuDoiTra"
FROM ban CROSS JOIN tien_tra;

-- ============================================================================
-- G8 | KY RONG - CHI TIET
-- CHAY LUC: Preflight va sau G8 tren UI
-- CAN THAY: 0 dong; neu co dong tien 0 thi van co phat sinh.
-- ============================================================================
WITH p AS (
  SELECT DATE '2000-01-01' AS tu, DATE '2000-01-31' AS den
), tap AS (
  SELECT h."ngayLap" AS ngay, c."maDongSP",
         'BAN'::text AS nguon, NULL::text AS ma_phieu,
         SUM(c."tienSauKhuyenMai") AS hang, SUM(c."tienThue") AS thue
  FROM "HoaDon" h
  JOIN "ChiTietHoaDon" c ON c."maHD" = h."maHD"
  CROSS JOIN p
  WHERE h."trangThaiThanhToan" = true AND h."ngayLap" BETWEEN p.tu AND p.den
  GROUP BY h."ngayLap", c."maDongSP"
  UNION ALL
  SELECT t."ngayHoanTat", c."maDongSP", 'TRA', t."maPhieuTra",
         -c."tienSauKhuyenMai", -c."tienThue"
  FROM "PhieuTra" t
  JOIN "ChiTietPhieuTra" x ON x."maPhieuTra" = t."maPhieuTra"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE t."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND t."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND t."ngayHoanTat" BETWEEN p.tu AND p.den
  UNION ALL
  SELECT d."ngayHoanTat", c."maDongSP", 'DOI', d."maPhieuDoi", 0, 0
  FROM "PhieuDoi" d
  JOIN "ChiTietPhieuDoi" x ON x."maPhieuDoi" = d."maPhieuDoi"
  JOIN "ChiTietHoaDon" c ON c."maHD" = x."maHD" AND c."soSerial" = x."soSerial"
  CROSS JOIN p
  WHERE d."trangThaiXuLyPhieu" = 'HOAN_TAT'
    AND d."trangThaiDuyet" IN ('KHONG_CAN_DUYET','DA_DUYET')
    AND d."ngayHoanTat" BETWEEN p.tu AND p.den
)
SELECT ngay, "maDongSP", hang AS "tienHang", thue AS "tienThue",
       hang + thue AS "tongThanhToan", nguon, ma_phieu
FROM tap ORDER BY ngay, "maDongSP", nguon, ma_phieu;
