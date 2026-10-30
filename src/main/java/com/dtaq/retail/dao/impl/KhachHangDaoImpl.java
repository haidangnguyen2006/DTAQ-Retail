package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.KhachHangDao;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link KhachHangDao} trên bảng {@code "KhachHang"}. */
public final class KhachHangDaoImpl implements KhachHangDao {

  private static final String SELECT_COT =
      "SELECT \"maKH\", \"hoTen\", \"soDienThoai\", \"diaChi\" FROM \"KhachHang\"";

  private static final String SELECT_BY_SDT =
      SELECT_COT + " WHERE \"soDienThoai\" = ? ORDER BY \"maKH\" LIMIT 1";

  private static final String SELECT_ALL = SELECT_COT + " ORDER BY \"maKH\"";

  private static final String INSERT_KHACH_HANG =
      "INSERT INTO \"KhachHang\" (\"maKH\", \"hoTen\", \"soDienThoai\", \"diaChi\")"
          + " VALUES (?, ?, ?, ?)";

  private static final String UPDATE_KHACH_HANG =
      "UPDATE \"KhachHang\" SET \"hoTen\" = ?, \"soDienThoai\" = ?, \"diaChi\" = ?"
          + " WHERE \"maKH\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public KhachHangDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public Optional<KhachHang> findBySdt(String soDienThoai) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_SDT)) {
      statement.setString(1, soDienThoai);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapKhachHang(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu khách hàng theo số điện thoại", e);
    }
  }

  @Override
  public List<KhachHang> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<KhachHang> ketQua = new ArrayList<>();
      while (resultSet.next()) {
        ketQua.add(mapKhachHang(resultSet));
      }
      return ketQua;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách khách hàng", e);
    }
  }

  @Override
  public void insert(KhachHang khachHang) {
    kiemTraMa(khachHang);
    try (Connection connection = dataSource.getConnection()) {
      ghiMoi(connection, khachHang);
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu khách hàng " + khachHang.maKH(), e);
    }
  }

  @Override
  public void insert(Connection ketNoi, KhachHang khachHang) {
    kiemTraMa(khachHang);
    Objects.requireNonNull(ketNoi, "ketNoi");
    // Không đóng kết nối và không đổi auto-commit: giao dịch do bên gọi quản lý.
    try {
      ghiMoi(ketNoi, khachHang);
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu khách hàng " + khachHang.maKH(), e);
    }
  }

  @Override
  public void update(KhachHang khachHang) {
    kiemTraMa(khachHang);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_KHACH_HANG)) {
      statement.setString(1, khachHang.hoTen());
      statement.setString(2, khachHang.soDienThoai());
      statement.setString(3, khachHang.diaChi());
      statement.setString(4, khachHang.maKH());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể cập nhật khách hàng " + khachHang.maKH());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật khách hàng " + khachHang.maKH(), e);
    }
  }

  private void ghiMoi(Connection ketNoi, KhachHang khachHang) throws SQLException {
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_KHACH_HANG)) {
      statement.setString(1, khachHang.maKH());
      statement.setString(2, khachHang.hoTen());
      statement.setString(3, khachHang.soDienThoai());
      statement.setString(4, khachHang.diaChi());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu khách hàng " + khachHang.maKH());
      }
    }
  }

  private static void kiemTraMa(KhachHang khachHang) {
    if (khachHang == null || khachHang.maKH() == null || khachHang.maKH().isBlank()) {
      throw new IllegalArgumentException("Khách hàng không hợp lệ");
    }
  }

  private KhachHang mapKhachHang(ResultSet resultSet) throws SQLException {
    return new KhachHang(
        resultSet.getString("maKH"),
        resultSet.getString("hoTen"),
        resultSet.getString("soDienThoai"),
        resultSet.getString("diaChi"));
  }
}
