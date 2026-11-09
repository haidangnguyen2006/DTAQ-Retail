package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.util.MaGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link HoaDonDaoImpl} trên cơ sở dữ liệu phát triển. */
class HoaDonDaoImplTest {

  private static final String MA_HD_MAU = "HD-2026-000001";
  private static final String MA_HD_CHUA_THANH_TOAN = "HD-2026-000004";
  private static final String MA_NV_MAU = "NVBANHANG-2025-001";
  private static final String MA_NV_THEM = "NVBANHANG-2026-001";
  private static final String MA_KH_MAU = "KH-2026-000001";

  private HoaDonDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new HoaDonDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Tìm hóa đơn theo mã trả về đầy đủ nhân viên lập và khách hàng")
  void findByIdReturnsHoaDonWithRelations() {
    Optional<HoaDon> found = dao.findById(MA_HD_MAU);
    Assumptions.assumeTrue(found.isPresent(), "seed V4 chưa được áp dụng");

    HoaDon hoaDon = found.orElseThrow();
    assertEquals(MA_HD_MAU, hoaDon.maHD());
    assertEquals(LocalDate.of(2026, 1, 5), hoaDon.ngayLap());
    assertTrue(hoaDon.trangThaiThanhToan());
    assertEquals(MA_NV_MAU, hoaDon.nhanVienLap().maNV());
    assertNull(hoaDon.nhanVienLap().matKhau(), "hóa đơn không được nạp mật khẩu nhân viên");
    assertFalse(hoaDon.nhanVienLap().hoTen().isBlank(), "họ tên nhân viên phải có");
    assertNotNull(hoaDon.nhanVienLap().loaiQuyen(), "loại quyền nhân viên phải được ánh xạ");
    assertEquals(MA_KH_MAU, hoaDon.khachHang().maKH());
    assertFalse(hoaDon.khachHang().soDienThoai().isBlank(), "SĐT khách hàng phải có");
  }

  @Test
  @DisplayName("Tìm hóa đơn với mã không tồn tại trả về rỗng")
  void findByIdUnknownIsEmpty() {
    assertTrue(dao.findById("HD-9999-999999").isEmpty());
  }

  @Test
  @DisplayName("Liệt kê hóa đơn đã thanh toán trong tháng 01/2026 đúng thứ tự")
  void findDaThanhToanTrongKyReturnsSortedPaidInvoices() {
    LocalDate tuNgay = LocalDate.of(2026, 1, 1);
    LocalDate denNgay = LocalDate.of(2026, 1, 31);

    List<HoaDon> ketQua = dao.findDaThanhToanTrongKy(tuNgay, denNgay);

    Assumptions.assumeFalse(ketQua.isEmpty(), "seed V4 chưa được áp dụng");
    for (HoaDon hoaDon : ketQua) {
      assertTrue(hoaDon.trangThaiThanhToan(), "chỉ lấy hóa đơn đã thanh toán");
      assertFalse(hoaDon.ngayLap().isBefore(tuNgay), "ngày lập không được trước kỳ");
      assertFalse(hoaDon.ngayLap().isAfter(denNgay), "ngày lập không được sau kỳ");
    }
    for (int i = 1; i < ketQua.size(); i++) {
      HoaDon truoc = ketQua.get(i - 1);
      HoaDon sau = ketQua.get(i);
      int theoNgay = truoc.ngayLap().compareTo(sau.ngayLap());
      assertTrue(theoNgay < 0 || (theoNgay == 0
              && truoc.maHD().compareTo(sau.maHD()) <= 0),
          "kết quả phải sắp xếp theo ngày lập rồi tới mã hóa đơn");
    }
  }

  @Test
  @DisplayName("Chỉ liệt kê hóa đơn đã thanh toán trong kỳ")
  void findDaThanhToanTrongKyChiLayHoaDonDaThanhToan() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      Assumptions.assumeTrue(demHoaDon(ketNoi, MA_HD_MAU) > 0, "seed V4 chưa được áp dụng");
      Assumptions.assumeTrue(
          datTrangThaiThanhToan(ketNoi, MA_HD_CHUA_THANH_TOAN, false) == 1,
          "seed thiếu " + MA_HD_CHUA_THANH_TOAN);

      List<HoaDon> ketQua =
          dao.findDaThanhToanTrongKy(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

      assertFalse(
          ketQua.stream().anyMatch(hoaDon -> MA_HD_CHUA_THANH_TOAN.equals(hoaDon.maHD())),
          "hóa đơn chưa thanh toán không được xuất hiện");
      assertTrue(
          ketQua.stream().anyMatch(hoaDon -> MA_HD_MAU.equals(hoaDon.maHD())),
          "hóa đơn đã thanh toán vẫn phải xuất hiện");
    } finally {
      try {
        datTrangThaiThanhToan(ketNoi, MA_HD_CHUA_THANH_TOAN, true);
      } finally {
        ketNoi.close();
      }
    }
  }

  @Test
  @DisplayName("Khoảng ngày không có hóa đơn đã thanh toán trả về danh sách rỗng")
  void findDaThanhToanTrongKyOutsideSeedIsEmpty() {
    List<HoaDon> ketQua = dao.findDaThanhToanTrongKy(
        LocalDate.of(1999, 1, 1), LocalDate.of(1999, 12, 31));
    assertTrue(ketQua.isEmpty());
  }

  @Test
  @DisplayName("Khoảng ngày đảo ngược bị từ chối")
  void findDaThanhToanTrongKyRejectsReversedRange() {
    assertThrows(IllegalArgumentException.class, () -> dao.findDaThanhToanTrongKy(
        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
  }

  @Test
  @DisplayName("Ngày null bị từ chối")
  void findDaThanhToanTrongKyRejectsNullDays() {
    assertThrows(IllegalArgumentException.class, () -> dao.findDaThanhToanTrongKy(
        null, LocalDate.of(2026, 1, 31)));
    assertThrows(IllegalArgumentException.class, () -> dao.findDaThanhToanTrongKy(
        LocalDate.of(2026, 1, 1), null));
  }

  @Test
  @DisplayName("Ghi hóa đơn trong giao dịch của bên gọi rồi quay lui")
  void insertWritesHoaDonInsideCallerTransaction() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      ketNoi.setAutoCommit(false);
      String maHD = MaGenerator.next(ketNoi, "HoaDon", "maHD", "HD");
      NhanVien nhanVien = docNhanVien(ketNoi, MA_NV_THEM);
      KhachHang khachHang = docKhachHang(ketNoi, MA_KH_MAU);
      HoaDon hoaDon = new HoaDon(maHD, LocalDate.of(2026, 12, 31), nhanVien, khachHang, true);

      dao.insert(ketNoi, hoaDon);

      assertFalse(ketNoi.getAutoCommit(), "DAO không được đổi chế độ cam kết");
      assertEquals(1, demHoaDon(ketNoi, maHD), "hóa đơn phải tồn tại trong giao dịch hiện tại");
      assertTrue(
          maHD.matches("^HD-" + LocalDate.now().getYear() + "-\\d{6}$"),
          "mã hóa đơn sai định dạng: " + maHD);
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
  @DisplayName("insert từ chối hóa đơn thiếu khách hàng")
  void insertTuChoiHoaDonThieuKhachHang() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      NhanVien nhanVien = docNhanVien(ketNoi, MA_NV_THEM);
      HoaDon hoaDon =
          new HoaDon("HD-2026-999998", LocalDate.of(2026, 12, 31), nhanVien, null, true);

      assertThrows(IllegalArgumentException.class, () -> dao.insert(ketNoi, hoaDon));
      assertEquals(0, demHoaDon(ketNoi, "HD-2026-999998"), "không được ghi hóa đơn không hợp lệ");
    } finally {
      ketNoi.close();
    }
  }

  private NhanVien docNhanVien(Connection ketNoi, String maNV) throws SQLException {
    String sql = "SELECT \"maNV\", \"hoTen\", \"soDienThoai\", \"email\", \"ngayVaoLam\","
        + " \"tenDangNhap\", \"matKhau\", \"loaiQuyen\", \"trangThaiHoatDong\""
        + " FROM \"NhanVien\" WHERE \"maNV\" = ?";
    try (PreparedStatement statement = ketNoi.prepareStatement(sql)) {
      statement.setString(1, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        Assumptions.assumeTrue(resultSet.next(), "seed nhân viên chưa được áp dụng");
        return new NhanVien(
            resultSet.getString("maNV"),
            resultSet.getString("hoTen"),
            resultSet.getString("soDienThoai"),
            resultSet.getString("email"),
            resultSet.getObject("ngayVaoLam", LocalDate.class),
            resultSet.getString("tenDangNhap"),
            resultSet.getString("matKhau"),
            LoaiQuyen.valueOf(resultSet.getString("loaiQuyen")),
            resultSet.getBoolean("trangThaiHoatDong"));
      }
    }
  }

  private KhachHang docKhachHang(Connection ketNoi, String maKH) throws SQLException {
    String sql = "SELECT \"maKH\", \"hoTen\", \"soDienThoai\", \"diaChi\""
        + " FROM \"KhachHang\" WHERE \"maKH\" = ?";
    try (PreparedStatement statement = ketNoi.prepareStatement(sql)) {
      statement.setString(1, maKH);
      try (ResultSet resultSet = statement.executeQuery()) {
        Assumptions.assumeTrue(resultSet.next(), "seed khách hàng chưa được áp dụng");
        return new KhachHang(
            resultSet.getString("maKH"),
            resultSet.getString("hoTen"),
            resultSet.getString("soDienThoai"),
            resultSet.getString("diaChi"));
      }
    }
  }

  private int demHoaDon(Connection ketNoi, String maHD) throws SQLException {
    try (PreparedStatement statement =
        ketNoi.prepareStatement("SELECT COUNT(*) FROM \"HoaDon\" WHERE \"maHD\" = ?")) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  /**
   * Đặt cờ thanh toán và xác nhận ngay (kết nối ở chế độ auto-commit) để connection khác đọc thấy.
   *
   * @return số dòng đã cập nhật, dùng để test tự kiểm chứng UPDATE có hiệu lực
   */
  private int datTrangThaiThanhToan(Connection ketNoi, String maHD, boolean daThanhToan)
      throws SQLException {
    try (PreparedStatement statement =
        ketNoi.prepareStatement(
            "UPDATE \"HoaDon\" SET \"trangThaiThanhToan\" = ? WHERE \"maHD\" = ?")) {
      statement.setBoolean(1, daThanhToan);
      statement.setString(2, maHD);
      return statement.executeUpdate();
    }
  }
}
