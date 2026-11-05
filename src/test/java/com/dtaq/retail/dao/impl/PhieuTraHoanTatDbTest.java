package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp phần hoàn tất phiếu trả/đổi của UC006 trên PostgreSQL (dữ liệu seed V4).
 *
 * <p>Mọi bài kiểm thử ghi đều mở một giao dịch riêng trên kết nối của lớp kiểm thử và quay lui ở
 * {@link #hoanNguyen()}, nên dữ liệu seed không bị thay đổi (NFR009). Bài kiểm thử đọc chỉ dùng dữ
 * liệu seed đã cam kết.
 */
class PhieuTraHoanTatDbTest {

  private static final String HD_GOC = "HD-2026-000031";
  private static final String HD_CO_PHIEU_TRA = "HD-2026-000024";
  private static final String HD_CO_PHIEU_DOI = "HD-2026-000013";
  private static final String NV_LAP = "NVBANHANG-2026-001";
  private static final String NV_DUYET = "NVQUANLY-2026-001";

  /** Máy đang DA_BAN thuộc DSP-2026-000006, dùng cho nhánh hàng nguyên vẹn. */
  private static final String SERIAL_DA_BAN_NGUYEN_VEN = "LG-FR-0006";
  private static final String DONG_NGUYEN_VEN = "DSP-2026-000006";

  /** Máy đang DA_BAN thuộc DSP-2026-000010, dùng cho nhánh hàng lỗi. */
  private static final String SERIAL_DA_BAN_HANG_LOI = "TOS-WM-0010";
  private static final String DONG_HANG_LOI = "DSP-2026-000010";

  /** Máy đang SAN_SANG_BAN thuộc DSP-2026-000019, dùng cho máy thay thế. */
  private static final String SERIAL_THAY_THE = "KAN-WP-0019";
  private static final String DONG_THAY_THE = "DSP-2026-000019";

  /** Serial thuộc HD_GOC và chưa có phiếu hiệu lực, dùng cho ca ghi phiếu trả. */
  private static final String SERIAL_KHONG_DA_BAN = "SAM-TV-0041";

  /** Phiếu trả seed đã HOAN_TAT để kiểm tra điều kiện CAS. */
  private static final String PHIEU_DA_HOAN_TAT = "PT-2026-000001";

  /** Phiếu đổi seed hoàn tất, dùng để kiểm tra đọc đầu phiếu và chi tiết. */
  private static final String PHIEU_DOI_SEED = "PD-2026-000001";

  /** Serial thứ hai thuộc HD_GOC và chưa có phiếu hiệu lực, dùng cho ca TU_CHOI. */
  private static final String SERIAL_TU_CHOI = "SAM-TV-0042";

  private static final LocalDate HOM_NAY = LocalDate.now();

  private PhieuTraDaoImpl phieuTraDao;
  private PhieuDoiDaoImpl phieuDoiDao;
  private Connection ketNoi;
  private final List<String> phieuTraDaTao = new ArrayList<>();
  private final List<String> phieuDoiDaTao = new ArrayList<>();

  @BeforeEach
  void setUp() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    phieuTraDao = new PhieuTraDaoImpl(ConnectDB.getDataSource());
    phieuDoiDao = new PhieuDoiDaoImpl(ConnectDB.getDataSource());
    ketNoi = ConnectDB.getDataSource().getConnection();
    ketNoi.setAutoCommit(false);
  }

  @AfterEach
  void hoanNguyen() throws SQLException {
    if (ketNoi != null) {
      try {
        ketNoi.rollback();
      } finally {
        ketNoi.setAutoCommit(true);
        ketNoi.close();
      }
    }
    donDepDuLieuDaTao();
  }

  @Test
  @DisplayName("taoPhieuTra sinh mã PT-YYYY-NNNNNN và ghi chi tiết theo hóa đơn gốc của đầu phiếu")
  void taoPhieuTraTrongTxTaoMaVaChiTiet() throws SQLException {
    ChiTietPhieuTra dong = new ChiTietPhieuTra(
        null,
        chiTietHoaDon("HD-2026-000002", SERIAL_KHONG_DA_BAN),
        TinhTrangHangTra.NGUYEN_VEN,
        "Máy còn nguyên hộp");

    String maPhieuTra = phieuTraDao.taoPhieuTra(
        ketNoi, phieuTraChoXuLy(HD_GOC, HOM_NAY, new BigDecimal("12096000")), List.of(dong));

    assertNotNull(maPhieuTra);
    assertTrue(maPhieuTra.matches("^PT-\\d{4}-\\d{6}$"),
        "Mã phiếu trả phải theo quy ước PT-YYYY-NNNNNN, nhận được " + maPhieuTra);

    PhieuTraRow dauPhieu = docPhieuTra(maPhieuTra);
    assertNotNull(dauPhieu, "Đầu phiếu vừa ghi phải đọc lại được trong cùng giao dịch");
    assertEquals(HD_GOC, dauPhieu.maHD());
    assertEquals("CHO_DUYET", dauPhieu.trangThaiDuyet());
    assertEquals("CHO_XU_LY", dauPhieu.trangThaiXuLyPhieu());
    assertEquals(0, new BigDecimal("12096000").compareTo(dauPhieu.tienKhachDua()));

    List<ChiTietRow> chiTiet = docChiTietPhieuTra(maPhieuTra);
    assertEquals(1, chiTiet.size());
    assertEquals(HD_GOC, chiTiet.get(0).maHD(),
        "RB24: maHD của dòng phải lấy từ hóa đơn gốc của đầu phiếu, không lấy từ dữ liệu dòng");
    assertEquals(SERIAL_KHONG_DA_BAN, chiTiet.get(0).soSerial());
    assertEquals("NGUYEN_VEN", chiTiet.get(0).tinhTrangHangTra());
  }

  @Test
  @DisplayName("Máy nguyên vẹn trở lại SAN_SANG_BAN, soLanDoiTra tăng và tồn kho tăng do trigger V6")
  void danhDauMayNhanLaiNguyenVenQuayLaiSanSangBan() throws SQLException {
    MayRow truoc = docMay(SERIAL_DA_BAN_NGUYEN_VEN);
    assertEquals("DA_BAN", truoc.trangThai(), "Điều kiện đầu vào: máy phải đang DA_BAN");
    int soLanDoiTraTruoc = truoc.soLanDoiTra();
    int tonTruoc = docSoLuongTon(DONG_NGUYEN_VEN);

    int soDong = phieuTraDao.danhDauMayNhanLai(
        ketNoi,
        List.of(new ChiTietPhieuTra(
            null,
            chiTietHoaDon(HD_GOC, SERIAL_DA_BAN_NGUYEN_VEN),
            TinhTrangHangTra.NGUYEN_VEN,
            "Khách đổi ý, máy nguyên vẹn")));

    assertEquals(1, soDong);
    MayRow sau = docMay(SERIAL_DA_BAN_NGUYEN_VEN);
    assertEquals("SAN_SANG_BAN", sau.trangThai());
    assertEquals(soLanDoiTraTruoc + 1, sau.soLanDoiTra());
    assertEquals(tonTruoc + 1, docSoLuongTon(DONG_NGUYEN_VEN),
        "Trigger V6 phải cộng máy vừa trả về thành tồn sẵn sàng bán");
  }

  @Test
  @DisplayName("Máy lỗi chuyển sang HANG_LOI và không làm tăng tồn kho")
  void danhDauMayNhanLaiHangLoiKhongTangTon() throws SQLException {
    MayRow truoc = docMay(SERIAL_DA_BAN_HANG_LOI);
    assertEquals("DA_BAN", truoc.trangThai());
    int tonTruoc = docSoLuongTon(DONG_HANG_LOI);

    int soDong = phieuTraDao.danhDauMayNhanLai(
        ketNoi,
        List.of(new ChiTietPhieuTra(
            null,
            chiTietHoaDon(HD_GOC, SERIAL_DA_BAN_HANG_LOI),
            TinhTrangHangTra.HANG_LOI,
            "Máy không lên nguồn")));

    assertEquals(1, soDong);
    MayRow sau = docMay(SERIAL_DA_BAN_HANG_LOI);
    assertEquals("HANG_LOI", sau.trangThai());
    assertEquals(truoc.soLanDoiTra() + 1, sau.soLanDoiTra());
    assertEquals(tonTruoc, docSoLuongTon(DONG_HANG_LOI),
        "Hàng lỗi không được tính vào tồn sẵn sàng bán");
  }

  @Test
  @DisplayName("Máy thay thế chuyển từ SAN_SANG_BAN sang DA_BAN và giảm tồn kho")
  void danhDauMayThayTheChuyenDaBan() throws SQLException {
    MayRow truoc = docMay(SERIAL_THAY_THE);
    assertEquals("SAN_SANG_BAN", truoc.trangThai());
    int tonTruoc = docSoLuongTon(DONG_THAY_THE);

    int soDong = phieuDoiDao.danhDauMayThayThe(
        ketNoi,
        List.of(new ChiTietPhieuDoi(
            null,
            null,
            "Đổi sang máy mới cùng model",
            TinhTrangHangTra.NGUYEN_VEN,
            sanPham(SERIAL_THAY_THE))));

    assertEquals(1, soDong);
    assertEquals("DA_BAN", docMay(SERIAL_THAY_THE).trangThai());
    assertEquals(tonTruoc - 1, docSoLuongTon(DONG_THAY_THE),
        "Máy giao cho khách phải bị trừ khỏi tồn sẵn sàng bán");
  }

  @Test
  @DisplayName("Serial không còn DA_BAN thì ném lỗi và rollback xóa cả phiếu vừa ghi (NFR009)")
  void rollbackKhiMayKhongPhaiDaBan() throws SQLException {
    ChiTietPhieuTra dong = new ChiTietPhieuTra(
        null,
        chiTietHoaDon(HD_GOC, SERIAL_KHONG_DA_BAN),
        TinhTrangHangTra.NGUYEN_VEN,
        "Máy còn nguyên vẹn");
    datTrangThaiMay(ketNoi, SERIAL_KHONG_DA_BAN, "HANG_LOI");

    String maPhieuTra = phieuTraDao.taoPhieuTra(
        ketNoi, phieuTraChoXuLy(HD_GOC, HOM_NAY, new BigDecimal("1000000")), List.of(dong));
    assertNotNull(docPhieuTra(maPhieuTra), "Phiếu phải tồn tại trước khi gặp lỗi");

    IllegalStateException loi = assertThrows(
        IllegalStateException.class,
        () -> phieuTraDao.danhDauMayNhanLai(ketNoi, List.of(dong)));
    assertTrue(loi.getMessage().contains(SERIAL_KHONG_DA_BAN));

    ketNoi.rollback();

    assertNull(docPhieuTra(maPhieuTra),
        "Sau rollback không được còn dòng PhieuTra nào của giao dịch đã hỏng");
    assertEquals("DA_BAN", docMay(SERIAL_KHONG_DA_BAN).trangThai(),
        "Trạng thái máy phải giữ nguyên khi giao dịch quay lui");
  }

  @Test
  @DisplayName("capNhatHoanTat chỉ ghi khi phiếu còn CHO_XU_LY và không ghi đè lần hai")
  void capNhatHoanTatChiGhiKhiChoXuLy() throws SQLException {
    assertFalse(phieuTraDao.capNhatHoanTat(ketNoi, PHIEU_DA_HOAN_TAT, HOM_NAY),
        "Phiếu đã HOAN_TAT không được cập nhật lần nữa");

    String maPhieuTra = phieuTraDao.taoPhieuTra(
        ketNoi,
        phieuTraDaDuyet(HD_GOC, HOM_NAY, new BigDecimal("1000000")),
        List.of());

    assertTrue(phieuTraDao.capNhatHoanTat(ketNoi, maPhieuTra, HOM_NAY),
        "Phiếu DA_DUYET/CHO_XU_LY phải hoàn tất được");
    PhieuTraRow sau = docPhieuTra(maPhieuTra);
    assertEquals("HOAN_TAT", sau.trangThaiXuLyPhieu());
    assertEquals(HOM_NAY, sau.ngayHoanTat());

    assertFalse(phieuTraDao.capNhatHoanTat(ketNoi, maPhieuTra, HOM_NAY),
        "Lần hoàn tất thứ hai phải bị chặn (CAS)");
  }

  @Test
  @DisplayName("findSerialsDaDoiTra gộp chi tiết phiếu trả và phiếu đổi của đúng hóa đơn")
  void findSerialsDaDoiTraGopHaiBangChiTiet() throws SQLException {
    Set<String> cuaPhieuTra = phieuTraDao.findSerialsDaDoiTra(
        HD_CO_PHIEU_TRA, List.of("SNY-AU-0027", "SNY-AU-0028", "KHONG-CO-TRONG-HD"));
    assertEquals(Set.of("SNY-AU-0027", "SNY-AU-0028"), cuaPhieuTra);
    assertEquals(demSerialChiTiet(HD_CO_PHIEU_TRA), cuaPhieuTra.size());

    Set<String> cuaPhieuDoi = phieuDoiDao.findSerialsDaDoiTra(
        HD_CO_PHIEU_DOI, List.of("PAN-AC-0013"));
    assertEquals(Set.of("PAN-AC-0013"), cuaPhieuDoi,
        "Serial nằm trong ChiTietPhieuDoi cũng phải bị coi là đã đổi/trả");

    assertEquals(Set.of(), phieuTraDao.findSerialsDaDoiTra(HD_CO_PHIEU_TRA, null));
    assertEquals(Set.of(), phieuTraDao.findSerialsDaDoiTra(HD_CO_PHIEU_TRA, List.of()));
    assertEquals(Set.of(), phieuTraDao.findSerialsDaDoiTra(null, List.of("SNY-AU-0027")));
  }

  @Test
  @DisplayName("findById và findChiTiet dựng đủ đầu phiếu và dòng chi tiết từ seed")
  void findByIdVaFindChiTietDocDuocSeed() {
    PhieuTra dauPhieu = phieuTraDao.findById(PHIEU_DA_HOAN_TAT).orElseThrow();
    assertEquals(PHIEU_DA_HOAN_TAT, dauPhieu.maPhieuTra());
    assertEquals("HD-2026-000001", dauPhieu.hoaDon().maHD());
    assertEquals("NVBANHANG-2025-001", dauPhieu.nhanVienLap().maNV());
    assertEquals(TrangThaiDuyet.DA_DUYET, dauPhieu.trangThaiDuyet());
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, dauPhieu.trangThaiXuLyPhieu());
    assertEquals(0, new BigDecimal("36190000").compareTo(dauPhieu.tienKhachDua()));

    List<ChiTietPhieuTra> chiTiet = phieuTraDao.findChiTiet(PHIEU_DA_HOAN_TAT);
    assertEquals(1, chiTiet.size());
    ChiTietPhieuTra dong = chiTiet.get(0);
    assertEquals(PHIEU_DA_HOAN_TAT, dong.phieuTra().maPhieuTra());
    assertEquals("HD-2026-000001", dong.chiTietHoaDon().hoaDon().maHD());
    assertEquals("SAM-TV-0001", dong.chiTietHoaDon().sanPham().soSerial());
    assertEquals(TinhTrangHangTra.HANG_LOI, dong.tinhTrangHangTra());
    assertNotNull(dong.chiTietHoaDon().tienSauKhuyenMai());
    assertNotNull(dong.chiTietHoaDon().sanPham().dongSanPham().danhMucSanPham());

    assertTrue(phieuTraDao.findById("PT-2026-KHONG-CO").isEmpty());
    assertTrue(phieuTraDao.findChiTiet("PT-2026-KHONG-CO").isEmpty());
  }

  @Test
  @DisplayName("findChoHoanTat lọc đúng DA_DUYET + CHO_XU_LY và khớp truy vấn đối chứng")
  void findChoHoanTatKhopTruyVanDoiChung() throws SQLException {
    List<PhieuDuyet> danhSach = phieuTraDao.findChoHoanTat();

    assertEquals(demPhieuTraChoHoanTat(), danhSach.size(),
        "Số phiếu chờ hoàn tất phải khớp truy vấn đối chứng trên cùng điều kiện");
    for (PhieuDuyet phieu : danhSach) {
      assertEquals(PhieuDuyet.LoaiPhieu.PHIEU_TRA, phieu.loai());
      assertTrue(phieu.maPhieu().startsWith("PT-"));
      assertEquals(1, demPhieuTheoMa(phieu.maPhieu()));
    }
    assertTrue(danhSach.stream().noneMatch(phieu -> "PT-2026-000010".equals(phieu.maPhieu())),
        "Phiếu mới CHO_DUYET chưa được duyệt nên chưa được hoàn tất");
  }

  @Test
  @DisplayName("taoPhieuDoi sinh mã PD-YYYY-NNNNNN, lưu chênh lệch có dấu và tình trạng hàng trả")
  void taoPhieuDoiLuuTienChenhLechVaTinhTrang() throws SQLException {
    PhieuDoi phieuDoi = new PhieuDoi(
        null,
        nhanVien(NV_LAP),
        new HoaDon(HD_GOC, HOM_NAY, null, null, true),
        null,
        HOM_NAY,
        TrangThaiDuyet.CHO_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY,
        BigDecimal.ZERO,
        new BigDecimal("-500000"),
        null,
        null,
        null);
    ChiTietPhieuDoi dong = new ChiTietPhieuDoi(
        null,
        chiTietHoaDon("HD-2026-000002", SERIAL_KHONG_DA_BAN),
        "Đổi máy lỗi block",
        TinhTrangHangTra.HANG_LOI,
        sanPham("PAN-AC-0061"));

    String maPhieuDoi = phieuDoiDao.taoPhieuDoi(ketNoi, phieuDoi, List.of(dong));

    assertNotNull(maPhieuDoi);
    assertTrue(maPhieuDoi.matches("^PD-\\d{4}-\\d{6}$"),
        "Mã phiếu đổi phải theo quy ước PD-YYYY-NNNNNN, nhận được " + maPhieuDoi);
    PhieuDoiRow dauPhieu = docPhieuDoi(maPhieuDoi);
    assertNotNull(dauPhieu);
    assertEquals(HD_GOC, dauPhieu.maHD());
    assertEquals(0, new BigDecimal("-500000").compareTo(dauPhieu.tienChenhLech()),
        "Chênh lệch âm phải được lưu nguyên dấu");
    ChiTietPhieuDoiRow chiTiet = docChiTietPhieuDoi(maPhieuDoi);
    assertEquals(HD_GOC, chiTiet.maHD(),
        "RB24: maHD của dòng phải lấy từ hóa đơn gốc của đầu phiếu");
    assertEquals(SERIAL_KHONG_DA_BAN, chiTiet.soSerial());
    assertEquals("PAN-AC-0061", chiTiet.chiTietSanPhamDoi());
    assertEquals("HANG_LOI", chiTiet.tinhTrangHangTra(),
        "Tình trạng hàng trả của phiếu đổi phải được lưu đúng");
  }

  @Test
  @DisplayName("Phiếu đổi: máy nguyên vẹn quay lại SAN_SANG_BAN và tăng tồn kho")
  void danhDauMayNhanLaiPhieuDoiNguyenVenQuayLaiSanSangBan() throws SQLException {
    MayRow truoc = docMay(SERIAL_DA_BAN_NGUYEN_VEN);
    assertEquals("DA_BAN", truoc.trangThai(), "Điều kiện đầu vào: máy phải đang DA_BAN");
    int tonTruoc = docSoLuongTon(DONG_NGUYEN_VEN);

    int soDong = phieuDoiDao.danhDauMayNhanLai(
        ketNoi,
        List.of(new ChiTietPhieuDoi(
            null,
            chiTietHoaDon(HD_CO_PHIEU_DOI, SERIAL_DA_BAN_NGUYEN_VEN),
            "Khách đổi sang mẫu khác, máy nguyên vẹn",
            TinhTrangHangTra.NGUYEN_VEN,
            sanPham("PAN-AC-0061"))));

    assertEquals(1, soDong);
    MayRow sau = docMay(SERIAL_DA_BAN_NGUYEN_VEN);
    assertEquals("SAN_SANG_BAN", sau.trangThai());
    assertEquals(truoc.soLanDoiTra() + 1, sau.soLanDoiTra());
    assertEquals(tonTruoc + 1, docSoLuongTon(DONG_NGUYEN_VEN),
        "Máy nguyên vẹn của phiếu đổi phải quay lại tồn sẵn sàng bán");
  }

  @Test
  @DisplayName("Phiếu đổi: máy lỗi vào HANG_LOI và không làm tăng tồn kho")
  void danhDauMayNhanLaiPhieuDoiHangLoiVaoHangLoi() throws SQLException {
    MayRow truoc = docMay(SERIAL_DA_BAN_HANG_LOI);
    assertEquals("DA_BAN", truoc.trangThai());
    int tonTruoc = docSoLuongTon(DONG_HANG_LOI);

    int soDong = phieuDoiDao.danhDauMayNhanLai(
        ketNoi,
        List.of(new ChiTietPhieuDoi(
            null,
            chiTietHoaDon(HD_CO_PHIEU_DOI, SERIAL_DA_BAN_HANG_LOI),
            "Máy lỗi bo mạch, khách đổi máy khác",
            TinhTrangHangTra.HANG_LOI,
            sanPham("PAN-AC-0061"))));

    assertEquals(1, soDong);
    MayRow sau = docMay(SERIAL_DA_BAN_HANG_LOI);
    assertEquals("HANG_LOI", sau.trangThai());
    assertEquals(truoc.soLanDoiTra() + 1, sau.soLanDoiTra());
    assertEquals(tonTruoc, docSoLuongTon(DONG_HANG_LOI),
        "Hàng lỗi của phiếu đổi không được tính vào tồn sẵn sàng bán");
  }

  @Test
  @DisplayName("findById và findChiTiet dựng đủ phiếu đổi cùng máy thay thế từ seed")
  void findByIdVaFindChiTietPhieuDoiDocDuocSeed() {
    PhieuDoi phieuDoi = phieuDoiDao.findById(PHIEU_DOI_SEED).orElseThrow();
    assertEquals("HD-2026-000013", phieuDoi.hoaDon().maHD());
    assertEquals("NVBANHANG-2025-001", phieuDoi.nhanVienLap().maNV());
    assertEquals(TrangThaiDuyet.KHONG_CAN_DUYET, phieuDoi.trangThaiDuyet());
    assertEquals(0, BigDecimal.ZERO.compareTo(phieuDoi.tienChenhLech()));

    List<ChiTietPhieuDoi> chiTiet = phieuDoiDao.findChiTiet(PHIEU_DOI_SEED);
    assertEquals(1, chiTiet.size());
    ChiTietPhieuDoi dong = chiTiet.get(0);
    assertEquals(PHIEU_DOI_SEED, dong.phieuDoi().maPhieuDoi());
    assertEquals("PAN-AC-0013", dong.chiTietHoaDon().sanPham().soSerial());
    assertEquals("PAN-AC-0061", dong.chiTietSanPhamDoi().soSerial());
    assertEquals("DSP-2026-000013", dong.chiTietSanPhamDoi().dongSanPham().maDongSP());
    assertNotNull(dong.chiTietSanPhamDoi().dongSanPham().danhMucSanPham());
    assertEquals(TinhTrangHangTra.HANG_LOI, dong.tinhTrangHangTra(),
        "PD-2026-000001 có hồ sơ SanPhamLoi nên V9 đã backfill thành HANG_LOI");
    assertTrue(phieuDoiDao.findById("PD-2026-KHONG-CO").isEmpty());
    assertTrue(phieuDoiDao.findChiTiet("PD-2026-KHONG-CO").isEmpty());
  }

  @Test
  @DisplayName("V9 backfill dòng ChiTietPhieuDoi có hồ sơ SanPhamLoi thành HANG_LOI và bỏ DEFAULT")
  void backfillTinhTrangPhieuDoiTheoHoSoLoi() throws SQLException {
    int tongCoHoSoLoi = demTongChiTietPhieuDoiCoHoSoLoi();
    int soNguyenVenConLai = demChiTietPhieuDoiCoHoSoLoi("NGUYEN_VEN");
    int soHangLoi = demChiTietPhieuDoiCoHoSoLoi("HANG_LOI");

    assertTrue(tongCoHoSoLoi >= 5,
        "Seed phải có ít nhất 5 dòng ChiTietPhieuDoi gắn SanPhamLoi, nhận được " + tongCoHoSoLoi);
    assertEquals(0, soNguyenVenConLai,
        "V9 phải chuyển mọi dòng có SanPhamLoi cùng phiếu + serial sang HANG_LOI");
    assertEquals(tongCoHoSoLoi, soHangLoi,
        "Mọi dòng có hồ sơ lỗi phải mang HANG_LOI sau backfill");

    assertNull(docColumnDefault("ChiTietPhieuDoi", "tinhTrangHangTra"),
        "V9 phải bỏ DEFAULT để mọi INSERT chỉ định tình trạng tường minh");
  }

  @Test
  @DisplayName("Phiếu đổi không bao giờ có hoàn dự kiến dù chênh lệch âm (D17)")
  void findChoHoanTatPhieuDoiKhongBaoGioHoanTien() throws SQLException {
    String maPhieuDoi = taoPhieuDoiChoHoanTatCommitted();

    List<PhieuDuyet> danhSach = phieuDoiDao.findChoHoanTat();
    PhieuDuyet phieu = danhSach.stream()
        .filter(dto -> maPhieuDoi.equals(dto.maPhieu()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Không thấy phiếu đổi " + maPhieuDoi));

    assertEquals(PhieuDuyet.LoaiPhieu.PHIEU_DOI, phieu.loai());
    assertEquals(0, BigDecimal.ZERO.compareTo(phieu.hoanDuKien()),
        "Đổi hàng không hoàn tiền nên hoàn dự kiến phải bằng 0, không lấy trị tuyệt đối");
  }

  @Test
  @DisplayName("CHECK V10 từ chối phiếu đổi có chênh lệch dương")
  void phieuDoiChenhLechDuongBiTuChoi() throws SQLException {
    String maPhieuDoi = "PD-2026-910001";

    SQLException loi = assertThrows(
        SQLException.class,
        () -> insertPhieuDoiViPham(
            ketNoi, maPhieuDoi, new BigDecimal("1"), BigDecimal.ZERO));

    assertTrue(loi.getSQLState() != null && loi.getSQLState().startsWith("23"),
        "Phải vi phạm CHECK (SQLState 23xxx), nhận được " + loi.getSQLState());
    ketNoi.rollback();
    assertEquals(0, demPhieuDoi(maPhieuDoi), "Không được ghi dòng nào khi vi phạm CHECK");
  }

  @Test
  @DisplayName("CHECK V10 từ chối phiếu đổi có thu thêm tiền")
  void phieuDoiThuThemKhacKhongBiTuChoi() throws SQLException {
    String maPhieuDoi = "PD-2026-910002";

    SQLException loi = assertThrows(
        SQLException.class,
        () -> insertPhieuDoiViPham(
            ketNoi, maPhieuDoi, BigDecimal.ZERO, new BigDecimal("1")));

    assertTrue(loi.getSQLState() != null && loi.getSQLState().startsWith("23"),
        "Phải vi phạm CHECK (SQLState 23xxx), nhận được " + loi.getSQLState());
    ketNoi.rollback();
    assertEquals(0, demPhieuDoi(maPhieuDoi), "Không được ghi dòng nào khi vi phạm CHECK");
  }

  @Test
  @DisplayName("danhDauMayThayThe ném lỗi khi máy không còn SAN_SANG_BAN")
  void danhDauMayThayTheNemKhiMayKhongSanSangBan() throws SQLException {
    ChiTietPhieuDoi dong = new ChiTietPhieuDoi(
        null,
        null,
        "Đổi sang máy khác",
        TinhTrangHangTra.NGUYEN_VEN,
        sanPham(SERIAL_DA_BAN_NGUYEN_VEN));

    IllegalStateException loi = assertThrows(
        IllegalStateException.class,
        () -> phieuDoiDao.danhDauMayThayThe(ketNoi, List.of(dong)));

    assertTrue(loi.getMessage().contains(SERIAL_DA_BAN_NGUYEN_VEN));
    assertEquals("DA_BAN", docMay(SERIAL_DA_BAN_NGUYEN_VEN).trangThai(),
        "Máy không được đổi trạng thái khi câu lệnh không khớp điều kiện");
  }

  @Test
  @DisplayName("Serial của phiếu bị từ chối/huỷ không chặn yêu cầu đổi/trả mới")
  void serialCuaPhieuTuChoiDuocPhepYeuCauLai() throws SQLException {
    String maPhieuTra = taoPhieuTraTuChoiCommitted();

    assertNotNull(docPhieuTra(maPhieuTra), "Fixture phải được ghi và cam kết");
    Set<String> daDoiTra = phieuTraDao.findSerialsDaDoiTra(
        HD_GOC, List.of(SERIAL_TU_CHOI));

    assertFalse(daDoiTra.contains(SERIAL_TU_CHOI),
        "Phiếu TU_CHOI/HUY không được coi là đã đổi/trả");
    assertEquals(Set.of(), daDoiTra);
  }

  @Test
  @DisplayName("taoPhieuTra chặn khi máy đã có phiếu đổi/trả còn hiệu lực")
  void taoPhieuTraChanKhiMayDaCoPhieuHieuLuc() throws SQLException {
    ChiTietPhieuTra dong = new ChiTietPhieuTra(
        null,
        chiTietHoaDon("HD-2026-000001", "SAM-TV-0001"),
        TinhTrangHangTra.NGUYEN_VEN,
        "Máy còn nguyên vẹn");
    PhieuTra header = phieuTraChoXuLy("HD-2026-000001", HOM_NAY, new BigDecimal("1000000"));

    IllegalStateException loi = assertThrows(
        IllegalStateException.class,
        () -> phieuTraDao.taoPhieuTra(ketNoi, header, List.of(dong)));

    assertTrue(loi.getMessage().contains("SAM-TV-0001"));
    assertEquals(1, demPhieuTheoCap(ketNoi, "HD-2026-000001", "SAM-TV-0001"),
        "Guard phải chặn trước khi ghi bất kỳ dòng phiếu mới nào");
  }

  @Test
  @DisplayName("taoPhieuDoi chặn khi máy đã có phiếu đổi/trả còn hiệu lực")
  void taoPhieuDoiChanKhiMayDaCoPhieuHieuLuc() throws SQLException {
    PhieuDoi header = new PhieuDoi(
        null,
        nhanVien(NV_LAP),
        new HoaDon("HD-2026-000013", HOM_NAY, null, null, true),
        null,
        HOM_NAY,
        TrangThaiDuyet.CHO_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY,
        BigDecimal.ZERO,
        new BigDecimal("-500000"),
        null,
        null,
        null);
    ChiTietPhieuDoi dong = new ChiTietPhieuDoi(
        null,
        chiTietHoaDon("HD-2026-000013", "PAN-AC-0013"),
        "Đổi máy lỗi",
        TinhTrangHangTra.HANG_LOI,
        sanPham("PAN-AC-0061"));

    IllegalStateException loi = assertThrows(
        IllegalStateException.class,
        () -> phieuDoiDao.taoPhieuDoi(ketNoi, header, List.of(dong)));

    assertTrue(loi.getMessage().contains("PAN-AC-0013"));
    assertEquals(1, demPhieuTheoCap(ketNoi, "HD-2026-000013", "PAN-AC-0013"),
        "Guard phải chặn trước khi ghi bất kỳ dòng phiếu mới nào");
  }

  private static SanPham sanPham(String soSerial) {
    return new SanPham(soSerial, null, null, BigDecimal.ZERO, null, null, 0, 0);
  }

  private static ChiTietHoaDon chiTietHoaDon(String maHD, String soSerial) {
    HoaDon hoaDon = new HoaDon(maHD, HOM_NAY, null, null, true);
    return new ChiTietHoaDon(
        hoaDon,
        sanPham(soSerial),
        1,
        new BigDecimal("1000000"),
        BigDecimal.ZERO,
        null,
        null,
        BigDecimal.ZERO,
        new BigDecimal("1000000"),
        new BigDecimal("1000000"),
        BigDecimal.ZERO);
  }

  private static NhanVien nhanVien(String maNV) {
    return new NhanVien(
        maNV, "Nhân viên " + maNV, "0900000001", maNV + "@dtaq.example", HOM_NAY,
        maNV, null, LoaiQuyen.NHAN_VIEN_BAN_HANG, true);
  }

  private static PhieuTra phieuTraChoXuLy(String maHD, LocalDate ngayTao, BigDecimal tienHoan) {
    HoaDon hoaDon = new HoaDon(maHD, ngayTao, null, null, true);
    return new PhieuTra(
        null,
        nhanVien(NV_LAP),
        hoaDon,
        ngayTao,
        null,
        TrangThaiDuyet.CHO_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY,
        null,
        null,
        null,
        tienHoan);
  }

  private static PhieuTra phieuTraDaDuyet(String maHD, LocalDate ngayTao, BigDecimal tienHoan) {
    HoaDon hoaDon = new HoaDon(maHD, ngayTao, null, null, true);
    return new PhieuTra(
        null,
        nhanVien(NV_LAP),
        hoaDon,
        ngayTao,
        null,
        TrangThaiDuyet.DA_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY,
        nhanVien(NV_DUYET),
        ngayTao,
        "Đồng ý hoàn tiền",
        tienHoan);
  }

  private PhieuTraRow docPhieuTra(String maPhieuTra) throws SQLException {
    String sql = "SELECT \"hoaDon\", \"trangThaiDuyet\", \"trangThaiXuLyPhieu\", \"ngayHoanTat\","
        + " \"tienKhachDua\" FROM \"PhieuTra\" WHERE \"maPhieuTra\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maPhieuTra);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        LocalDate ngayHoanTat = rs.getObject("ngayHoanTat", LocalDate.class);
        return new PhieuTraRow(
            rs.getString("hoaDon"),
            rs.getString("trangThaiDuyet"),
            rs.getString("trangThaiXuLyPhieu"),
            ngayHoanTat,
            rs.getBigDecimal("tienKhachDua"));
      }
    }
  }

  private List<ChiTietRow> docChiTietPhieuTra(String maPhieuTra) throws SQLException {
    String sql = "SELECT \"maHD\", \"soSerial\", \"tinhTrangHangTra\", \"lyDoTra\""
        + " FROM \"ChiTietPhieuTra\" WHERE \"maPhieuTra\" = ? ORDER BY \"soSerial\"";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maPhieuTra);
      try (ResultSet rs = ps.executeQuery()) {
        List<ChiTietRow> ketQua = new ArrayList<>();
        while (rs.next()) {
          ketQua.add(new ChiTietRow(
              rs.getString("maHD"),
              rs.getString("soSerial"),
              rs.getString("tinhTrangHangTra"),
              rs.getString("lyDoTra")));
        }
        return ketQua;
      }
    }
  }

  private PhieuDoiRow docPhieuDoi(String maPhieuDoi) throws SQLException {
    String sql = "SELECT \"hoaDon\", \"tienKhachDuaThem\", \"tienChenhLech\""
        + " FROM \"PhieuDoi\" WHERE \"maPhieuDoi\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maPhieuDoi);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return new PhieuDoiRow(
            rs.getString("hoaDon"),
            rs.getBigDecimal("tienKhachDuaThem"),
            rs.getBigDecimal("tienChenhLech"));
      }
    }
  }

  private ChiTietPhieuDoiRow docChiTietPhieuDoi(String maPhieuDoi) throws SQLException {
    String sql = "SELECT \"maHD\", \"soSerial\", \"tinhTrangHangTra\", \"chiTietSanPhamDoi\""
        + " FROM \"ChiTietPhieuDoi\" WHERE \"maPhieuDoi\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maPhieuDoi);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Không tìm thấy dòng chi tiết phiếu đổi " + maPhieuDoi);
        return new ChiTietPhieuDoiRow(
            rs.getString("maHD"),
            rs.getString("soSerial"),
            rs.getString("tinhTrangHangTra"),
            rs.getString("chiTietSanPhamDoi"));
      }
    }
  }

  private MayRow docMay(String soSerial) throws SQLException {
    String sql = "SELECT \"trangThai\", \"soLanDoiTra\" FROM \"SanPham\" WHERE \"soSerial\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, soSerial);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Không tìm thấy máy " + soSerial);
        return new MayRow(rs.getString("trangThai"), rs.getInt("soLanDoiTra"));
      }
    }
  }

  private int docSoLuongTon(String maDongSP) throws SQLException {
    String sql = "SELECT \"soLuongTon\" FROM \"DongSanPham\" WHERE \"maDongSP\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maDongSP);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Không tìm thấy dòng sản phẩm " + maDongSP);
        return rs.getInt("soLuongTon");
      }
    }
  }

  private int demSerialChiTiet(String maHD) throws SQLException {
    String sql = "SELECT count(DISTINCT \"soSerial\") FROM ("
        + " SELECT \"soSerial\" FROM \"ChiTietPhieuTra\" WHERE \"maHD\" = ?"
        + " UNION"
        + " SELECT \"soSerial\" FROM \"ChiTietPhieuDoi\" WHERE \"maHD\" = ?"
        + ") AS tap";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maHD);
      ps.setString(2, maHD);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private int demPhieuTraChoHoanTat() throws SQLException {
    String sql = "SELECT count(*) FROM \"PhieuTra\""
        + " WHERE \"trangThaiDuyet\" = 'DA_DUYET' AND \"trangThaiXuLyPhieu\" = 'CHO_XU_LY'";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getInt(1);
    }
  }

  private int demPhieuTheoMa(String maPhieu) throws SQLException {
    String sql = "SELECT count(*) FROM \"PhieuTra\" WHERE \"maPhieuTra\" = ?"
        + " AND \"trangThaiDuyet\" = 'DA_DUYET' AND \"trangThaiXuLyPhieu\" = 'CHO_XU_LY'";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maPhieu);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private int demTongChiTietPhieuDoiCoHoSoLoi() throws SQLException {
    String sql = "SELECT count(*) FROM \"ChiTietPhieuDoi\" ct"
        + " WHERE EXISTS (SELECT 1 FROM \"SanPhamLoi\" spl"
        + "  WHERE spl.\"maPhieuDoi\" = ct.\"maPhieuDoi\""
        + "  AND spl.\"soSerial\" = ct.\"soSerial\")";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getInt(1);
    }
  }

  private int demChiTietPhieuDoiCoHoSoLoi(String tinhTrang) throws SQLException {
    String sql = "SELECT count(*) FROM \"ChiTietPhieuDoi\" ct"
        + " WHERE ct.\"tinhTrangHangTra\" = ?"
        + " AND EXISTS (SELECT 1 FROM \"SanPhamLoi\" spl"
        + "  WHERE spl.\"maPhieuDoi\" = ct.\"maPhieuDoi\""
        + "  AND spl.\"soSerial\" = ct.\"soSerial\")";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, tinhTrang);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private String docColumnDefault(String bang, String cot) throws SQLException {
    String sql = "SELECT column_default FROM information_schema.columns"
        + " WHERE table_name = ? AND column_name = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, bang);
      ps.setString(2, cot);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Không thấy cột " + bang + "." + cot);
        return rs.getString("column_default");
      }
    }
  }

  private int demPhieuTheoCap(Connection conn, String maHD, String soSerial) throws SQLException {
    String sql = "SELECT (SELECT count(*) FROM \"ChiTietPhieuTra\""
        + "  WHERE \"maHD\" = ? AND \"soSerial\" = ?)"
        + " + (SELECT count(*) FROM \"ChiTietPhieuDoi\""
        + "  WHERE \"maHD\" = ? AND \"soSerial\" = ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, maHD);
      ps.setString(2, soSerial);
      ps.setString(3, maHD);
      ps.setString(4, soSerial);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private static void datTrangThaiMay(Connection conn, String soSerial, String trangThai)
      throws SQLException {
    String sql = "UPDATE \"SanPham\" SET \"trangThai\" = ? WHERE \"soSerial\" = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, trangThai);
      ps.setString(2, soSerial);
      assertEquals(1, ps.executeUpdate(), "Phải cập nhật đúng một máy " + soSerial);
    }
  }

  /**
   * Ghi một phiếu đổi DA_DUYET/CHO_XU_LY có chênh lệch âm và cam kết ngay.
   *
   * <p>Dữ liệu được dọn ở {@link #hoanNguyen()} theo thứ tự FK.
   */
  private String taoPhieuDoiChoHoanTatCommitted() throws SQLException {
    String maPhieuDoi = "PD-2026-900001";
    try (Connection conn = ConnectDB.getDataSource().getConnection()) {
      conn.setAutoCommit(true);
      String sql = "INSERT INTO \"PhieuDoi\" (\"maPhieuDoi\", \"nhanVienLap\", \"hoaDon\","
          + " \"ngayHoanTat\", \"ngayTao\", \"trangThaiDuyet\", \"trangThaiXuLyPhieu\","
          + " \"tienKhachDuaThem\", \"tienChenhLech\", \"nhanVienDuyet\", \"ngayDuyet\","
          + " \"lyDoDuyet\") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, maPhieuDoi);
        ps.setString(2, NV_LAP);
        ps.setString(3, HD_GOC);
        ps.setNull(4, Types.DATE);
        ps.setObject(5, HOM_NAY);
        ps.setString(6, TrangThaiDuyet.DA_DUYET.name());
        ps.setString(7, TrangThaiXuLyPhieu.CHO_XU_LY.name());
        ps.setBigDecimal(8, BigDecimal.ZERO);
        ps.setBigDecimal(9, new BigDecimal("-2000000"));
        ps.setString(10, NV_DUYET);
        ps.setObject(11, HOM_NAY);
        ps.setString(12, "Đồng ý hoàn tiền");
        assertEquals(1, ps.executeUpdate());
      }
      phieuDoiDaTao.add(maPhieuDoi);

      String sqlChiTiet = "INSERT INTO \"ChiTietPhieuDoi\" (\"maPhieuDoi\", \"maHD\","
          + " \"soSerial\", \"lyDoDoi\", \"tinhTrangHangTra\", \"chiTietSanPhamDoi\")"
          + " VALUES (?,?,?,?,?,?)";
      try (PreparedStatement ps = conn.prepareStatement(sqlChiTiet)) {
        ps.setString(1, maPhieuDoi);
        ps.setString(2, HD_GOC);
        ps.setString(3, SERIAL_KHONG_DA_BAN);
        ps.setString(4, "Khách đổi máy");
        ps.setString(5, TinhTrangHangTra.NGUYEN_VEN.name());
        ps.setString(6, "PAN-AC-0061");
        assertEquals(1, ps.executeUpdate());
      }
    }
    return maPhieuDoi;
  }

  /** Ghi một phiếu trả TU_CHOI/HUY và cam kết ngay để kiểm tra bộ lọc hiệu lực. */
  private String taoPhieuTraTuChoiCommitted() throws SQLException {
    String maPhieuTra = "PT-2026-900001";
    try (Connection conn = ConnectDB.getDataSource().getConnection()) {
      conn.setAutoCommit(true);
      String sql = "INSERT INTO \"PhieuTra\" (\"maPhieuTra\", \"nhanVienLap\", \"hoaDon\","
          + " \"ngayTao\", \"ngayHoanTat\", \"trangThaiDuyet\", \"trangThaiXuLyPhieu\","
          + " \"nhanVienDuyet\", \"ngayDuyet\", \"lyDoDuyet\", \"tienKhachDua\")"
          + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, maPhieuTra);
        ps.setString(2, NV_LAP);
        ps.setString(3, HD_GOC);
        ps.setObject(4, HOM_NAY);
        ps.setNull(5, Types.DATE);
        ps.setString(6, TrangThaiDuyet.TU_CHOI.name());
        ps.setString(7, TrangThaiXuLyPhieu.HUY.name());
        ps.setString(8, NV_DUYET);
        ps.setObject(9, HOM_NAY);
        ps.setString(10, "Từ chối theo kiểm thử");
        ps.setBigDecimal(11, BigDecimal.ZERO);
        assertEquals(1, ps.executeUpdate());
      }
      phieuTraDaTao.add(maPhieuTra);

      String sqlChiTiet = "INSERT INTO \"ChiTietPhieuTra\" (\"maPhieuTra\", \"maHD\","
          + " \"soSerial\", \"tinhTrangHangTra\", \"lyDoTra\") VALUES (?,?,?,?,?)";
      try (PreparedStatement ps = conn.prepareStatement(sqlChiTiet)) {
        ps.setString(1, maPhieuTra);
        ps.setString(2, HD_GOC);
        ps.setString(3, SERIAL_TU_CHOI);
        ps.setString(4, TinhTrangHangTra.NGUYEN_VEN.name());
        ps.setString(5, "Lý do kiểm thử");
        assertEquals(1, ps.executeUpdate());
      }
    }
    return maPhieuTra;
  }

  private void donDepDuLieuDaTao() {
    if ((phieuTraDaTao.isEmpty() && phieuDoiDaTao.isEmpty()) || !ConnectDB.isAvailable()) {
      return;
    }
    try (Connection conn = ConnectDB.getDataSource().getConnection()) {
      conn.setAutoCommit(true);
      for (String maPhieuTra : phieuTraDaTao) {
        xoa(conn, "DELETE FROM \"SanPhamLoi\" WHERE \"maPhieuTra\" = ?", maPhieuTra);
        xoa(conn, "DELETE FROM \"ChiTietPhieuTra\" WHERE \"maPhieuTra\" = ?", maPhieuTra);
        xoa(conn, "DELETE FROM \"PhieuTra\" WHERE \"maPhieuTra\" = ?", maPhieuTra);
      }
      for (String maPhieuDoi : phieuDoiDaTao) {
        xoa(conn, "DELETE FROM \"SanPhamLoi\" WHERE \"maPhieuDoi\" = ?", maPhieuDoi);
        xoa(conn, "DELETE FROM \"ChiTietPhieuDoi\" WHERE \"maPhieuDoi\" = ?", maPhieuDoi);
        xoa(conn, "DELETE FROM \"PhieuDoi\" WHERE \"maPhieuDoi\" = ?", maPhieuDoi);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Không dọn được dữ liệu kiểm thử đã cam kết", e);
    }
  }

  private static void xoa(Connection conn, String sql, String maPhieu) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, maPhieu);
      ps.executeUpdate();
    }
  }

  /**
   * Thử ghi một đầu phiếu đổi chỉ có header để kiểm tra CHECK của V10.
   *
   * @param conn kết nối của transaction kiểm thử (sẽ bị abort khi vi phạm)
   * @param maPhieuDoi mã phiếu thử
   * @param tienChenhLech chênh lệch cần ghi
   * @param tienKhachDuaThem tiền khách đưa thêm cần ghi
   */
  private static void insertPhieuDoiViPham(
      Connection conn,
      String maPhieuDoi,
      BigDecimal tienChenhLech,
      BigDecimal tienKhachDuaThem) throws SQLException {
    String sql = "INSERT INTO \"PhieuDoi\" (\"maPhieuDoi\", \"nhanVienLap\", \"hoaDon\","
        + " \"ngayHoanTat\", \"ngayTao\", \"trangThaiDuyet\", \"trangThaiXuLyPhieu\","
        + " \"tienKhachDuaThem\", \"tienChenhLech\", \"nhanVienDuyet\", \"ngayDuyet\","
        + " \"lyDoDuyet\") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, maPhieuDoi);
      ps.setString(2, NV_LAP);
      ps.setString(3, HD_GOC);
      ps.setNull(4, Types.DATE);
      ps.setObject(5, HOM_NAY);
      ps.setString(6, TrangThaiDuyet.KHONG_CAN_DUYET.name());
      ps.setString(7, TrangThaiXuLyPhieu.CHO_XU_LY.name());
      ps.setBigDecimal(8, tienKhachDuaThem);
      ps.setBigDecimal(9, tienChenhLech);
      ps.setNull(10, Types.VARCHAR);
      ps.setNull(11, Types.DATE);
      ps.setNull(12, Types.VARCHAR);
      ps.executeUpdate();
    }
  }

  private int demPhieuDoi(String maPhieuDoi) throws SQLException {
    String sql = "SELECT count(*) FROM \"PhieuDoi\" WHERE \"maPhieuDoi\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maPhieuDoi);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  /** Giá trị đọc lại của một dòng "SanPham" trong giao dịch kiểm thử. */
  private record MayRow(String trangThai, int soLanDoiTra) {
  }

  /** Giá trị đọc lại của một đầu phiếu trả trong giao dịch kiểm thử. */
  private record PhieuTraRow(
      String maHD,
      String trangThaiDuyet,
      String trangThaiXuLyPhieu,
      LocalDate ngayHoanTat,
      BigDecimal tienKhachDua) {
  }

  /** Giá trị đọc lại của một dòng "ChiTietPhieuTra" trong giao dịch kiểm thử. */
  private record ChiTietRow(
      String maHD, String soSerial, String tinhTrangHangTra, String lyDoTra) {
  }

  /** Giá trị đọc lại của một đầu phiếu đổi trong giao dịch kiểm thử. */
  private record PhieuDoiRow(
      String maHD, BigDecimal tienKhachDuaThem, BigDecimal tienChenhLech) {
  }

  /** Giá trị đọc lại của một dòng "ChiTietPhieuDoi" trong giao dịch kiểm thử. */
  private record ChiTietPhieuDoiRow(
      String maHD, String soSerial, String tinhTrangHangTra, String chiTietSanPhamDoi) {
  }
}
