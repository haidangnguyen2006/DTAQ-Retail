package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link HoaDonDao} trên bảng {@code "HoaDon"}. */
public final class HoaDonDaoImpl implements HoaDonDao {

  /**
   * Phần đọc dùng chung: hóa đơn kèm nhân viên lập và khách hàng. Mọi cột được đặt bí danh theo
   * tiền tố {@code hd_}, {@code nv_}, {@code kh_} để tránh trùng tên giữa ba bảng.
   */
  private static final String SELECT_CO_JOIN =
      "SELECT hd.\"maHD\" AS \"hd_maHD\","
          + " hd.\"ngayLap\" AS \"hd_ngayLap\","
          + " hd.\"trangThaiThanhToan\" AS \"hd_trangThaiThanhToan\","
          + " nv.\"maNV\" AS \"nv_maNV\","
          + " nv.\"hoTen\" AS \"nv_hoTen\","
          + " nv.\"soDienThoai\" AS \"nv_soDienThoai\","
          + " nv.\"email\" AS \"nv_email\","
          + " nv.\"ngayVaoLam\" AS \"nv_ngayVaoLam\","
          + " nv.\"tenDangNhap\" AS \"nv_tenDangNhap\","
          + " nv.\"matKhau\" AS \"nv_matKhau\","
          + " nv.\"loaiQuyen\" AS \"nv_loaiQuyen\","
          + " nv.\"trangThaiHoatDong\" AS \"nv_trangThaiHoatDong\","
          + " kh.\"maKH\" AS \"kh_maKH\","
          + " kh.\"hoTen\" AS \"kh_hoTen\","
          + " kh.\"soDienThoai\" AS \"kh_soDienThoai\","
          + " kh.\"diaChi\" AS \"kh_diaChi\""
          + " FROM \"HoaDon\" hd"
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = hd.\"nhanVienLap\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\"";

  private static final String SELECT_BY_ID = SELECT_CO_JOIN + " WHERE hd.\"maHD\" = ?";

  private static final String SELECT_DA_THANH_TOAN_TRONG_KY =
      SELECT_CO_JOIN
          + " WHERE hd.\"trangThaiThanhToan\" = TRUE"
          + " AND hd.\"ngayLap\" BETWEEN ? AND ?"
          + " ORDER BY hd.\"ngayLap\", hd.\"maHD\"";

  private static final String INSERT_HOA_DON =
      "INSERT INTO \"HoaDon\""
          + " (\"maHD\", \"ngayLap\", \"nhanVienLap\", \"khachHang\", \"trangThaiThanhToan\")"
          + " VALUES (?, ?, ?, ?, ?)";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public HoaDonDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(Connection ketNoi, HoaDon hoaDon) {
    if (hoaDon == null || hoaDon.maHD() == null || hoaDon.maHD().isBlank()) {
      throw new IllegalArgumentException("Hóa đơn không hợp lệ");
    }
    Objects.requireNonNull(ketNoi, "ketNoi");
    // Không đóng kết nối và không đổi auto-commit: giao dịch do bên gọi quản lý.
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_HOA_DON)) {
      statement.setString(1, hoaDon.maHD());
      statement.setObject(2, hoaDon.ngayLap());
      statement.setString(3, hoaDon.nhanVienLap().maNV());
      statement.setString(4, hoaDon.khachHang().maKH());
      statement.setBoolean(5, hoaDon.trangThaiThanhToan());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu hóa đơn " + hoaDon.maHD());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu hóa đơn " + hoaDon.maHD(), e);
    }
  }

  @Override
  public Optional<HoaDon> findById(String maHD) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapHoaDon(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu hóa đơn", e);
    }
  }

  @Override
  public List<HoaDon> findDaThanhToanTrongKy(LocalDate tuNgay, LocalDate denNgay) {
    if (tuNgay == null || denNgay == null || tuNgay.isAfter(denNgay)) {
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(SELECT_DA_THANH_TOAN_TRONG_KY)) {
      statement.setObject(1, tuNgay);
      statement.setObject(2, denNgay);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<HoaDon> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapHoaDon(resultSet));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu hóa đơn đã thanh toán trong kỳ", e);
    }
  }

  private HoaDon mapHoaDon(ResultSet resultSet) throws SQLException {
    NhanVien nhanVien = new NhanVien(
        resultSet.getString("nv_maNV"),
        resultSet.getString("nv_hoTen"),
        resultSet.getString("nv_soDienThoai"),
        resultSet.getString("nv_email"),
        resultSet.getObject("nv_ngayVaoLam", LocalDate.class),
        resultSet.getString("nv_tenDangNhap"),
        resultSet.getString("nv_matKhau"),
        LoaiQuyen.valueOf(resultSet.getString("nv_loaiQuyen")),
        resultSet.getBoolean("nv_trangThaiHoatDong"));
    KhachHang khachHang = new KhachHang(
        resultSet.getString("kh_maKH"),
        resultSet.getString("kh_hoTen"),
        resultSet.getString("kh_soDienThoai"),
        resultSet.getString("kh_diaChi"));
    return new HoaDon(
        resultSet.getString("hd_maHD"),
        resultSet.getObject("hd_ngayLap", LocalDate.class),
        nhanVien,
        khachHang,
        resultSet.getBoolean("hd_trangThaiThanhToan"));
  }
}
