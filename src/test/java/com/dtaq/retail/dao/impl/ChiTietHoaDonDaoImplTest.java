package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link ChiTietHoaDonDaoImpl} trên cơ sở dữ liệu phát triển. */
class ChiTietHoaDonDaoImplTest {

  private static final String HOA_DON_CO_SAN = "HD-2026-000001";
  private static final String HOA_DON_NHIEU_DONG = "HD-2026-000021";
  private static final String HOA_DON_KHONG_TON_TAI = "HD-9999-999999";
  private static final String HOA_DON_TAM = "HD-2026-999999";
  private static final String MA_KM_MAU = "KM-2026-000001";

  private static final String DEM_SO_DONG =
      "SELECT COUNT(*) FROM \"ChiTietHoaDon\" WHERE \"maHD\" = ?";

  private static final String CHEN_HOA_DON_TAM =
      "INSERT INTO \"HoaDon\" (\"maHD\", \"ngayLap\", \"nhanVienLap\", \"khachHang\","
          + " \"trangThaiThanhToan\") VALUES (?, ?, ?, ?, ?)";

  private static final String TIM_MAY_CHUA_BAN =
      "SELECT sp.\"soSerial\", sp.\"maNCC\", sp.\"maDongSP\", sp.\"giaNhap\","
          + " sp.\"ngayNhapKhoBanDau\", sp.\"trangThai\", sp.\"soLanDoiTra\", sp.\"soLanSua\","
          + " ncc.\"tenNCC\","
          + " dsp.\"tenDongSP\", dsp.\"maDanhMuc\", dsp.\"donViTinh\", dsp.\"soLuongTon\","
          + " dsp.\"thue\", dsp.\"donGiaBanTruocThue\", dsp.\"trangThaiKinhDoanh\","
          + " dsp.\"ngayNhapKho\", dsp.\"ngayCapNhapCuoi\","
          + " dm.\"tenDanhMuc\", dm.\"moTa\""
          + " FROM \"SanPham\" sp"
          + " JOIN \"NhaCungCap\" ncc ON ncc.\"maNCC\" = sp.\"maNCC\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = sp.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\""
          + " WHERE sp.\"trangThai\" = 'SAN_SANG_BAN'"
          + " AND NOT EXISTS ("
          + " SELECT 1 FROM \"ChiTietHoaDon\" ct WHERE ct.\"soSerial\" = sp.\"soSerial\")"
          + " ORDER BY sp.\"soSerial\""
          + " LIMIT 1";

  private ChiTietHoaDonDaoImpl dao;

  @BeforeAll
  static void requireDatabase() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
  }

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new ChiTietHoaDonDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("findByHoaDon nạp đủ chi tiết và các thực thể liên quan của hóa đơn có sẵn")
  void findByHoaDonNapDayDuChiTiet() throws SQLException {
    List<ChiTietHoaDon> chiTiet = dao.findByHoaDon(HOA_DON_CO_SAN);
    Assumptions.assumeTrue(demSoDong(HOA_DON_CO_SAN) > 0, "seed V4 chưa được áp dụng");

    assertEquals(demSoDong(HOA_DON_CO_SAN), chiTiet.size(),
        "số dòng chi tiết phải khớp bảng");

    for (ChiTietHoaDon dong : chiTiet) {
      assertNotNull(dong.hoaDon(), "hóa đơn phải được nạp");
      assertEquals(HOA_DON_CO_SAN, dong.hoaDon().maHD());
      assertEquals(1, dong.soLuong());
      assertNotNull(dong.sanPham(), "sản phẩm phải được nạp");
      assertFalse(dong.sanPham().soSerial().isBlank(), "số serial không được rỗng");
      assertNotNull(dong.sanPham().dongSanPham(), "dòng sản phẩm phải được nạp");
      assertNotNull(dong.sanPham().dongSanPham().danhMucSanPham(),
          "danh mục sản phẩm phải được nạp");
      assertNotNull(dong.sanPham().nhaCungCap(), "nhà cung cấp phải được nạp");
      assertNotNull(dong.hoaDon().nhanVienLap(), "nhân viên lập phải được nạp");
      assertNull(dong.hoaDon().nhanVienLap().matKhau(), "không nạp mật khẩu nhân viên");
      assertNotNull(dong.hoaDon().khachHang(), "khách hàng phải được nạp");

      kiemTraThangBang(dong.donGiaBanTaiLucBan(), "donGiaBanTaiLucBan");
      kiemTraThangBang(dong.thueSuatTaiLucBan(), "thueSuatTaiLucBan");
      kiemTraThangBang(dong.tyLeGiamTaiLucBan(), "tyLeGiamTaiLucBan");
      kiemTraThangBang(dong.tienKhuyenMaiThucTe(), "tienKhuyenMaiThucTe");
      kiemTraThangBang(dong.tienTruocKhuyenMai(), "tienTruocKhuyenMai");
      kiemTraThangBang(dong.tienSauKhuyenMai(), "tienSauKhuyenMai");
      kiemTraThangBang(dong.tienThue(), "tienThue");
      kiemTraThangBang(dong.sanPham().giaNhap(), "giaNhap");
      kiemTraThangBang(dong.sanPham().dongSanPham().thue(), "thue");
      kiemTraThangBang(dong.sanPham().dongSanPham().donGiaBanTruocThue(),
          "donGiaBanTruocThue");
    }
  }

  @Test
  @DisplayName("findByHoaDon sắp xếp chi tiết theo số serial tăng dần")
  void findByHoaDonSapXepTheoSerial() {
    List<ChiTietHoaDon> chiTiet = dao.findByHoaDon(HOA_DON_NHIEU_DONG);
    Assumptions.assumeTrue(chiTiet.size() >= 2, "seed V4 chưa có hóa đơn nhiều dòng");

    for (int i = 1; i < chiTiet.size(); i++) {
      String truoc = chiTiet.get(i - 1).sanPham().soSerial();
      String sau = chiTiet.get(i).sanPham().soSerial();
      assertTrue(truoc.compareTo(sau) <= 0,
          "chi tiết phải sắp xếp theo số serial tăng dần: " + truoc + " > " + sau);
    }
  }

  @Test
  @DisplayName("findByHoaDon trả về danh sách rỗng khi hóa đơn không tồn tại")
  void findByHoaDonKhongTonTaiTraVeRong() {
    assertTrue(dao.findByHoaDon(HOA_DON_KHONG_TON_TAI).isEmpty(),
        "hóa đơn không tồn tại phải trả về danh sách rỗng");
  }

  @Test
  @DisplayName("insert ghi được một dòng chi tiết trong giao dịch của bên gọi")
  void insertGhiDuocDongChiTiet() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      ketNoi.setAutoCommit(false);
      SanPham sanPham = timMayChuaBan(ketNoi);
      Assumptions.assumeTrue(sanPham != null, "không còn máy SAN_SANG_BAN chưa bán");

      HoaDon hoaDon = new HoaDon(
          HOA_DON_TAM,
          LocalDate.of(2026, 12, 31),
          null,
          null,
          true);
      chenHoaDonTam(ketNoi);

      ChiTietHoaDon chiTiet = new ChiTietHoaDon(
          hoaDon,
          sanPham,
          1,
          new BigDecimal("100"),
          new BigDecimal("10"),
          null,
          null,
          BigDecimal.ZERO,
          new BigDecimal("100"),
          new BigDecimal("100"),
          new BigDecimal("10"));

      dao.insert(ketNoi, chiTiet);

      assertEquals(1, demSoDong(ketNoi, HOA_DON_TAM),
          "phải ghi đúng một dòng chi tiết cho hóa đơn tạm");
      assertFalse(ketNoi.getAutoCommit(), "DAO không được đổi chế độ cam kết");
    } finally {
      try {
        ketNoi.rollback();
        ketNoi.setAutoCommit(true);
      } finally {
        ketNoi.close();
      }
    }
  }

  @Test
  @DisplayName("insert ghi được mã khuyến mãi và tỷ lệ giảm tại lúc bán")
  void insertGhiDuocKhuyenMai() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      ketNoi.setAutoCommit(false);
      SanPham sanPham = timMayChuaBan(ketNoi);
      Assumptions.assumeTrue(sanPham != null, "không còn máy SAN_SANG_BAN chưa bán");
      Assumptions.assumeTrue(
          tonTaiKhuyenMai(ketNoi, MA_KM_MAU), "seed khuyến mãi chưa được áp dụng");

      HoaDon hoaDon = new HoaDon(
          HOA_DON_TAM,
          LocalDate.of(2026, 12, 31),
          null,
          null,
          true);
      chenHoaDonTam(ketNoi);

      KhuyenMai khuyenMai = new KhuyenMai(
          MA_KM_MAU,
          "Khuyến mãi Tivi Samsung",
          LocalDate.of(2026, 1, 1),
          LocalDate.of(2026, 1, 31),
          new BigDecimal("10"),
          null,
          true,
          sanPham.dongSanPham());
      ChiTietHoaDon chiTiet = new ChiTietHoaDon(
          hoaDon,
          sanPham,
          1,
          new BigDecimal("100"),
          new BigDecimal("10"),
          khuyenMai,
          new BigDecimal("10"),
          new BigDecimal("10"),
          new BigDecimal("100"),
          new BigDecimal("90"),
          new BigDecimal("9"));

      dao.insert(ketNoi, chiTiet);

      try (PreparedStatement statement = ketNoi.prepareStatement(
          "SELECT \"maKM\", \"tyLeGiamTaiLucBan\" FROM \"ChiTietHoaDon\" WHERE \"maHD\" = ?")) {
        statement.setString(1, HOA_DON_TAM);
        try (ResultSet resultSet = statement.executeQuery()) {
          assertTrue(resultSet.next(), "phải có dòng chi tiết vừa ghi");
          assertEquals(MA_KM_MAU, resultSet.getString("maKM"));
          assertEquals(
              0,
              new BigDecimal("10").compareTo(resultSet.getBigDecimal("tyLeGiamTaiLucBan")),
              "tỷ lệ giảm tại lúc bán phải được lưu");
        }
      }
    } finally {
      try {
        ketNoi.rollback();
        ketNoi.setAutoCommit(true);
      } finally {
        ketNoi.close();
      }
    }
  }

  @Test
  @DisplayName("insert từ chối dòng chi tiết thiếu dòng sản phẩm")
  void insertTuChoiChiTietThieuDongSanPham() throws SQLException {
    HoaDon hoaDon = new HoaDon(
        HOA_DON_TAM,
        LocalDate.of(2026, 12, 31),
        new NhanVien("NVBANHANG-2026-001", "Đăng bán hàng", "0900000002", null,
            LocalDate.of(2026, 1, 1), "NVBH2026001", "khong-dung-toi",
            LoaiQuyen.NHAN_VIEN_BAN_HANG, true),
        new KhachHang("KH-2026-000001", "Nguyễn Văn An", "0902100001", null),
        true);
    SanPham sanPham = new SanPham(
        "SERIAL-KIEM-THU",
        new NhaCungCap("NCC-2026-000001", "Nhà cung cấp kiểm thử"),
        null,
        new BigDecimal("100"),
        LocalDate.of(2026, 12, 31),
        TrangThaiSanPham.SAN_SANG_BAN,
        0,
        0);
    ChiTietHoaDon chiTiet = new ChiTietHoaDon(
        hoaDon,
        sanPham,
        1,
        new BigDecimal("100"),
        new BigDecimal("10"),
        null,
        null,
        BigDecimal.ZERO,
        new BigDecimal("100"),
        new BigDecimal("100"),
        new BigDecimal("10"));

    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      assertThrows(IllegalArgumentException.class, () -> dao.insert(ketNoi, chiTiet));
    } finally {
      ketNoi.close();
    }
  }

  @Test
  @DisplayName("insert từ chối dòng chi tiết có số lượng khác 1")
  void insertTuChoiSoLuongKhacMot() throws SQLException {
    HoaDon hoaDon = new HoaDon(
        HOA_DON_TAM,
        LocalDate.of(2026, 12, 31),
        new NhanVien("NVBANHANG-2026-001", "Đăng bán hàng", "0900000002", null,
            LocalDate.of(2026, 1, 1), "NVBH2026001", "khong-dung-toi",
            LoaiQuyen.NHAN_VIEN_BAN_HANG, true),
        new KhachHang("KH-2026-000001", "Nguyễn Văn An", "0902100001", null),
        true);
    SanPham sanPham = new SanPham(
        "SERIAL-KIEM-THU",
        new NhaCungCap("NCC-2026-000001", "Nhà cung cấp kiểm thử"),
        new DongSanPham("DSP-2026-000001", "Dòng kiểm thử",
            new DanhMucSanPham("DM-2026-000001", "Danh mục kiểm thử", null),
            "Chiếc", 1, BigDecimal.TEN, new BigDecimal("100"), true, null, null),
        new BigDecimal("100"),
        LocalDate.of(2026, 12, 31),
        TrangThaiSanPham.SAN_SANG_BAN,
        0,
        0);
    ChiTietHoaDon chiTiet = new ChiTietHoaDon(
        hoaDon,
        sanPham,
        2,
        new BigDecimal("100"),
        new BigDecimal("10"),
        null,
        null,
        BigDecimal.ZERO,
        new BigDecimal("200"),
        new BigDecimal("200"),
        new BigDecimal("20"));

    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      assertThrows(IllegalArgumentException.class, () -> dao.insert(ketNoi, chiTiet));
    } finally {
      ketNoi.close();
    }
  }

  private void chenHoaDonTam(Connection ketNoi) throws SQLException {
    try (PreparedStatement statement = ketNoi.prepareStatement(CHEN_HOA_DON_TAM)) {
      statement.setString(1, HOA_DON_TAM);
      statement.setObject(2, LocalDate.of(2026, 12, 31));
      statement.setString(3, "NVBANHANG-2026-001");
      statement.setString(4, "KH-2026-000001");
      statement.setBoolean(5, true);
      statement.executeUpdate();
    }
  }

  private SanPham timMayChuaBan(Connection ketNoi) throws SQLException {
    try (PreparedStatement statement = ketNoi.prepareStatement(TIM_MAY_CHUA_BAN);
        ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        return null;
      }
      DanhMucSanPham danhMuc = new DanhMucSanPham(
          resultSet.getString(11),
          resultSet.getString(19),
          resultSet.getString(20));
      DongSanPham dongSanPham = new DongSanPham(
          resultSet.getString(3),
          resultSet.getString(10),
          danhMuc,
          resultSet.getString(12),
          resultSet.getInt(13),
          resultSet.getBigDecimal(14),
          resultSet.getBigDecimal(15),
          resultSet.getBoolean(16),
          resultSet.getObject(17, LocalDate.class),
          resultSet.getObject(18, LocalDate.class));
      NhaCungCap nhaCungCap = new NhaCungCap(
          resultSet.getString(2),
          resultSet.getString(9));
      return new SanPham(
          resultSet.getString(1),
          nhaCungCap,
          dongSanPham,
          resultSet.getBigDecimal(4),
          resultSet.getObject(5, LocalDate.class),
          TrangThaiSanPham.valueOf(resultSet.getString(6)),
          resultSet.getInt(7),
          resultSet.getInt(8));
    }
  }

  private boolean tonTaiKhuyenMai(Connection ketNoi, String maKM) throws SQLException {
    try (PreparedStatement statement =
        ketNoi.prepareStatement("SELECT 1 FROM \"KhuyenMai\" WHERE \"maKM\" = ?")) {
      statement.setString(1, maKM);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private int demSoDong(String maHD) throws SQLException {
    try (Connection ketNoi = ConnectDB.getDataSource().getConnection()) {
      return demSoDong(ketNoi, maHD);
    }
  }

  private int demSoDong(Connection ketNoi, String maHD) throws SQLException {
    try (PreparedStatement statement = ketNoi.prepareStatement(DEM_SO_DONG)) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private void kiemTraThangBang(BigDecimal giaTri, String tenCot) {
    if (giaTri != null) {
      assertEquals(0, giaTri.scale(), tenCot + " phải là số nguyên VND (thang 0)");
    }
  }
}
