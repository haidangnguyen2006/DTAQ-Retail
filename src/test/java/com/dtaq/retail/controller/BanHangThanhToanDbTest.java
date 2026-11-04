package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.dao.impl.ChiTietHoaDonDaoImpl;
import com.dtaq.retail.dao.impl.HoaDonDaoImpl;
import com.dtaq.retail.dao.impl.HoaDonTamFileDaoImpl;
import com.dtaq.retail.dao.impl.KhuyenMaiDaoImpl;
import com.dtaq.retail.dao.impl.SanPhamImpl;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.TienUtil;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp luồng thanh toán UC003 trên cơ sở dữ liệu phát triển.
 *
 * <p>Mỗi bài tự chọn một dòng sản phẩm còn kinh doanh có ít nhất hai máy {@code SAN_SANG_BAN}, ghi
 * hóa đơn thật rồi dọn sạch đúng những bản ghi do bài kiểm thử tạo ra. Không xóa dữ liệu mẫu.
 */
class BanHangThanhToanDbTest {

  private static final String SAN_SANG_BAN = "SAN_SANG_BAN";
  private static final String DA_BAN = "DA_BAN";

  private static DataSource dataSource;

  private String maHDDaTao;
  private final Map<String, String> trangThaiGocTheoSerial = new LinkedHashMap<>();

  @BeforeAll
  static void kiemTraCoSoDuLieu() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dataSource = ConnectDB.getDataSource();
  }

  @AfterEach
  void donDep() throws SQLException {
    if (!ConnectDB.isAvailable()) {
      return;
    }
    try (Connection ketNoi = dataSource.getConnection()) {
      // Trả từng máy về đúng trạng thái ghi nhận trước khi thanh toán; trigger sẽ tính lại tồn.
      try (PreparedStatement statement = ketNoi.prepareStatement(
          "UPDATE \"SanPham\" SET \"trangThai\" = ? WHERE \"soSerial\" = ?")) {
        for (Map.Entry<String, String> entry : trangThaiGocTheoSerial.entrySet()) {
          statement.setString(1, entry.getValue());
          statement.setString(2, entry.getKey());
          statement.addBatch();
        }
        if (!trangThaiGocTheoSerial.isEmpty()) {
          statement.executeBatch();
        }
      }
      if (maHDDaTao != null) {
        try (PreparedStatement statement = ketNoi.prepareStatement(
            "DELETE FROM \"ChiTietHoaDon\" WHERE \"maHD\" = ?")) {
          statement.setString(1, maHDDaTao);
          statement.executeUpdate();
        }
        try (PreparedStatement statement = ketNoi.prepareStatement(
            "DELETE FROM \"HoaDon\" WHERE \"maHD\" = ?")) {
          statement.setString(1, maHDDaTao);
          statement.executeUpdate();
        }
      }
    } finally {
      maHDDaTao = null;
      trangThaiGocTheoSerial.clear();
    }
  }

  @Test
  @DisplayName("Thanh toán: ghi hóa đơn, chi tiết snapshot và chuyển máy sang DA_BAN")
  void thanhToanGhiHoaDonVaChiTietSnapshot() throws SQLException {
    Clock dongHo = Clock.systemDefaultZone();
    DongMay dong = chonDongVaHaiMay();
    KhachHang khachHang = docKhachHangDauTien();
    NhanVien nhanVien = docNhanVienDauTien();
    int tonTruoc = docSoLuongTon(dong.maDongSP());
    KhuyenMaiDao khuyenMaiDao = new KhuyenMaiDaoImpl(dataSource);
    Optional<KhuyenMai> khuyenMai =
        khuyenMaiDao.findHieuLuc(dong.maDongSP(), LocalDate.now(dongHo));

    BanHangController controller = taoController(dongHo, new ChiTietHoaDonDaoImpl(dataSource));
    controller.themDong(dong.maDongSP(), 2);
    long tienKhachDua = controller.hoaDonHienTai().tongThanhToan().longValue() + 1000000;

    BanHangController.KetQuaThanhToan ketQua =
        controller.thanhToan(nhanVien, khachHang, tienKhachDua);
    // Gán ngay sau khi thanh toán để dọn được hóa đơn đã cam kết kể cả khi một khẳng định phía dưới lỗi.
    maHDDaTao = ketQua.maHD();

    assertTrue(ketQua.thanhCong(), ketQua.thongBao());
    assertNotNull(ketQua.maHD());
    assertTrue(ketQua.maHD().matches("^HD-\\d{4}-\\d{6}$"), "mã hóa đơn sai định dạng");
    maHDDaTao = ketQua.maHD();

    assertEquals(1, demHoaDon(ketQua.maHD()), "phải có đúng một hóa đơn mới");
    assertTrue(hoaDonDaThanhToan(ketQua.maHD()), "hóa đơn phải ở trạng thái đã thanh toán");
    assertEquals(khachHang.maKH(), maKhachHangCuaHoaDon(ketQua.maHD()));
    assertEquals(nhanVien.maNV(), maNhanVienCuaHoaDon(ketQua.maHD()));

    List<ChiTietHoaDon> chiTiet =
        new ChiTietHoaDonDaoImpl(dataSource).findByHoaDon(ketQua.maHD());
    assertEquals(2, chiTiet.size(), "hai máy phải sinh đúng hai dòng chi tiết");
    assertEquals(dong.serials().stream().sorted().toList(),
        chiTiet.stream().map(ct -> ct.sanPham().soSerial()).sorted().toList());
    for (ChiTietHoaDon ct : chiTiet) {
      assertEquals(1, ct.soLuong(), "mỗi dòng chi tiết là một máy");
      assertSnapshot(ct, dong, khuyenMai, dongHo);
    }

    long tongTruocThue = 0;
    long tongGiamGia = 0;
    long tongThue = 0;
    for (ChiTietHoaDon ct : chiTiet) {
      tongTruocThue += ct.tienSauKhuyenMai().longValue();
      tongGiamGia += ct.tienKhuyenMaiThucTe().longValue();
      tongThue += ct.tienThue().longValue();
    }
    assertEquals(tongTruocThue, ketQua.tongTruocThue());
    assertEquals(tongGiamGia, ketQua.tongGiamGia());
    assertEquals(tongThue, ketQua.tongThue());
    assertEquals(tongTruocThue + tongThue, ketQua.tongThanhToan());
    assertEquals(tienKhachDua, ketQua.tienKhachDua());
    assertEquals(tienKhachDua - ketQua.tongThanhToan(), ketQua.tienTraLai());

    for (String soSerial : dong.serials()) {
      assertEquals(DA_BAN, docTrangThai(soSerial), "máy đã bán phải chuyển sang DA_BAN");
    }
    assertEquals(tonTruoc - 2, docSoLuongTon(dong.maDongSP()),
        "trigger phải giảm tồn sẵn sàng bán đúng 2 máy");
    assertTrue(controller.hoaDonHienTai().dongHang().isEmpty(), "giỏ hàng phải được làm rỗng");
  }

  @Test
  @DisplayName("Lỗi khi ghi chi tiết phải quay lui toàn bộ giao dịch thanh toán")
  void loiGhiChiTietQuayLuiToanBo() throws SQLException {
    Clock dongHo = Clock.systemDefaultZone();
    DongMay dong = chonDongVaHaiMay();
    KhachHang khachHang = docKhachHangDauTien();
    NhanVien nhanVien = docNhanVienDauTien();
    int tonTruoc = docSoLuongTon(dong.maDongSP());
    int soHoaDonTruoc = demTatCaHoaDon();
    int soChiTietTruoc = demTatCaChiTiet();

    ChiTietHoaDonDao chiTietLoi = new FakeChiTietHoaDonDaoLoi(2);
    BanHangController controller = taoController(dongHo, chiTietLoi);
    controller.themDong(dong.maDongSP(), 2);

    assertThrows(DataAccessException.class,
        () -> controller.thanhToan(nhanVien, khachHang, 100000000L));

    assertEquals(soHoaDonTruoc, demTatCaHoaDon(), "hóa đơn phải được quay lui");
    assertEquals(soChiTietTruoc, demTatCaChiTiet(), "chi tiết phải được quay lui");
    for (String soSerial : dong.serials()) {
      assertEquals(SAN_SANG_BAN, docTrangThai(soSerial),
          "máy phải được khôi phục trạng thái sẵn sàng bán");
    }
    assertEquals(tonTruoc, docSoLuongTon(dong.maDongSP()), "tồn kho phải được khôi phục");
    BanHangController.HoaDonTinh conLai = controller.hoaDonHienTai();
    assertEquals(1, conLai.dongHang().size(), "giỏ hàng phải được giữ nguyên");
    assertEquals(2, conLai.dongHang().getFirst().soLuong());
  }

  private BanHangController taoController(Clock dongHo, ChiTietHoaDonDao chiTietHoaDonDao)
      throws SQLException {
    Path thuMucTam = Path.of("/tmp/opencode/dtaq-ban-hang-db-test");
    try {
      Files.createDirectories(thuMucTam);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Không tạo được thư mục hóa đơn tạm", e);
    }
    return new BanHangController(
        new SanPhamImpl(dataSource),
        new KhuyenMaiDaoImpl(dataSource),
        new HoaDonDaoImpl(dataSource),
        chiTietHoaDonDao,
        new HoaDonTamFileDaoImpl(thuMucTam, dongHo),
        dataSource,
        dongHo);
  }

  private void assertSnapshot(
      ChiTietHoaDon ct, DongMay dong, Optional<KhuyenMai> khuyenMai, Clock dongHo) {
    BigDecimal donGia = TienUtil.lamTron(dong.donGia());
    BigDecimal tyLeGiam = khuyenMai.map(KhuyenMai::tyLeGiam).orElse(null);
    BanHangController.DongTinh tien =
        BanHangController.tinhDong(dong.donGia(), dong.thue(), 1, tyLeGiam);

    assertTien(donGia, ct.donGiaBanTaiLucBan());
    assertTien(dong.thue(), ct.thueSuatTaiLucBan());
    assertEquals(
        khuyenMai.map(KhuyenMai::maKM).orElse(null),
        ct.khuyenMai() == null ? null : ct.khuyenMai().maKM());
    if (tyLeGiam == null) {
      assertNull(ct.tyLeGiamTaiLucBan());
    } else {
      assertTien(tyLeGiam, ct.tyLeGiamTaiLucBan());
    }
    assertTien(tien.tienKhuyenMai(), ct.tienKhuyenMaiThucTe());
    assertTien(tien.tienTruocKhuyenMai(), ct.tienTruocKhuyenMai());
    assertTien(tien.tienSauKhuyenMai(), ct.tienSauKhuyenMai());
    assertTien(tien.tienThue(), ct.tienThue());
  }

  private DongMay chonDongVaHaiMay() throws SQLException {
    try (Connection ketNoi = dataSource.getConnection()) {
      String maDongSP;
      try (PreparedStatement statement = ketNoi.prepareStatement(
          "SELECT sp.\"maDongSP\" FROM \"SanPham\" sp"
              + " JOIN \"DongSanPham\" d ON d.\"maDongSP\" = sp.\"maDongSP\""
              + " WHERE sp.\"trangThai\" = 'SAN_SANG_BAN' AND d.\"trangThaiKinhDoanh\""
              + " GROUP BY sp.\"maDongSP\" HAVING count(*) >= 2"
              + " ORDER BY sp.\"maDongSP\" LIMIT 1")) {
        try (ResultSet resultSet = statement.executeQuery()) {
          Assumptions.assumeTrue(
              resultSet.next(), "không có dòng sản phẩm nào đủ hai máy sẵn sàng bán");
          maDongSP = resultSet.getString(1);
        }
      }
      BigDecimal donGia;
      BigDecimal thue;
      try (PreparedStatement statement = ketNoi.prepareStatement(
          "SELECT \"donGiaBanTruocThue\", \"thue\" FROM \"DongSanPham\""
              + " WHERE \"maDongSP\" = ?")) {
        statement.setString(1, maDongSP);
        try (ResultSet resultSet = statement.executeQuery()) {
          resultSet.next();
          donGia = resultSet.getBigDecimal(1);
          thue = resultSet.getBigDecimal(2);
        }
      }
      List<String> serials = new ArrayList<>();
      try (PreparedStatement statement = ketNoi.prepareStatement(
          "SELECT \"soSerial\", \"trangThai\" FROM \"SanPham\""
              + " WHERE \"maDongSP\" = ? AND \"trangThai\" = 'SAN_SANG_BAN'"
              + " ORDER BY \"ngayNhapKhoBanDau\" ASC NULLS LAST, \"soSerial\" ASC LIMIT 2")) {
        statement.setString(1, maDongSP);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            String soSerial = resultSet.getString("soSerial");
            serials.add(soSerial);
            trangThaiGocTheoSerial.put(soSerial, resultSet.getString("trangThai"));
          }
        }
      }
      Assumptions.assumeTrue(serials.size() == 2, "không đủ hai máy sẵn sàng bán");
      return new DongMay(maDongSP, donGia, thue, List.copyOf(serials));
    }
  }

  private KhachHang docKhachHangDauTien() throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(
            "SELECT \"maKH\", \"hoTen\", \"soDienThoai\", \"diaChi\""
                + " FROM \"KhachHang\" ORDER BY \"maKH\" LIMIT 1");
        ResultSet resultSet = statement.executeQuery()) {
      Assumptions.assumeTrue(resultSet.next(), "chưa có khách hàng mẫu");
      return new KhachHang(
          resultSet.getString("maKH"),
          resultSet.getString("hoTen"),
          resultSet.getString("soDienThoai"),
          resultSet.getString("diaChi"));
    }
  }

  private NhanVien docNhanVienDauTien() throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(
            "SELECT \"maNV\", \"hoTen\", \"soDienThoai\", \"email\", \"ngayVaoLam\","
                + " \"tenDangNhap\", \"loaiQuyen\", \"trangThaiHoatDong\""
                + " FROM \"NhanVien\" ORDER BY \"maNV\" LIMIT 1");
        ResultSet resultSet = statement.executeQuery()) {
      Assumptions.assumeTrue(resultSet.next(), "chưa có nhân viên mẫu");
      return new NhanVien(
          resultSet.getString("maNV"),
          resultSet.getString("hoTen"),
          resultSet.getString("soDienThoai"),
          resultSet.getString("email"),
          resultSet.getObject("ngayVaoLam", LocalDate.class),
          resultSet.getString("tenDangNhap"),
          null,
          LoaiQuyen.valueOf(resultSet.getString("loaiQuyen")),
          resultSet.getBoolean("trangThaiHoatDong"));
    }
  }

  private int demHoaDon(String maHD) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(
            "SELECT count(*) FROM \"HoaDon\" WHERE \"maHD\" = ?")) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private boolean hoaDonDaThanhToan(String maHD) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(
            "SELECT \"trangThaiThanhToan\" FROM \"HoaDon\" WHERE \"maHD\" = ?")) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        Assumptions.assumeTrue(resultSet.next(), "không tìm thấy hóa đơn vừa tạo");
        return resultSet.getBoolean(1);
      }
    }
  }

  private String maKhachHangCuaHoaDon(String maHD) throws SQLException {
    return docChuoi("SELECT \"khachHang\" FROM \"HoaDon\" WHERE \"maHD\" = ?", maHD);
  }

  private String maNhanVienCuaHoaDon(String maHD) throws SQLException {
    return docChuoi("SELECT \"nhanVienLap\" FROM \"HoaDon\" WHERE \"maHD\" = ?", maHD);
  }

  private String docChuoi(String sql, String thamSo) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(sql)) {
      statement.setString(1, thamSo);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
      }
    }
  }

  private String docTrangThai(String soSerial) throws SQLException {
    return docChuoi("SELECT \"trangThai\" FROM \"SanPham\" WHERE \"soSerial\" = ?", soSerial);
  }

  private int docSoLuongTon(String maDongSP) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(
            "SELECT \"soLuongTon\" FROM \"DongSanPham\" WHERE \"maDongSP\" = ?")) {
      statement.setString(1, maDongSP);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int demTatCaHoaDon() throws SQLException {
    return dem("SELECT count(*) FROM \"HoaDon\"");
  }

  private int demTatCaChiTiet() throws SQLException {
    return dem("SELECT count(*) FROM \"ChiTietHoaDon\"");
  }

  private int dem(String sql) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement = ketNoi.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static void assertTien(BigDecimal mongDoi, BigDecimal thucTe) {
    assertNotNull(thucTe);
    assertEquals(0, mongDoi.compareTo(thucTe),
        "Mong đợi " + mongDoi + " nhưng nhận " + thucTe);
  }

  /** Dòng sản phẩm được chọn cùng hai máy và giá/thuế hiện hành. */
  private record DongMay(
      String maDongSP, BigDecimal donGia, BigDecimal thue, List<String> serials) {
  }

  /** DAO chi tiết hóa đơn giả lập lỗi ở lần ghi thứ N, dùng để kiểm tra quay lui. */
  private static final class FakeChiTietHoaDonDaoLoi implements ChiTietHoaDonDao {

    private final int loiTaiLanThu;
    private final List<ChiTietHoaDon> daGhi = new ArrayList<>();

    FakeChiTietHoaDonDaoLoi(int loiTaiLanThu) {
      this.loiTaiLanThu = loiTaiLanThu;
    }

    @Override
    public void insert(Connection ketNoi, ChiTietHoaDon chiTiet) {
      if (daGhi.size() + 1 == loiTaiLanThu) {
        throw new DataAccessException("Lỗi ghi chi tiết giả lập");
      }
      daGhi.add(chiTiet);
    }

    @Override
    public List<ChiTietHoaDon> findByHoaDon(String maHD) {
      return daGhi.stream()
          .filter(chiTiet -> chiTiet.hoaDon().maHD().equals(maHD))
          .toList();
    }
  }
}
