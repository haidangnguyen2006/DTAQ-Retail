package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link NhanVienDao} trên bảng {@code "NhanVien"}. */
public class NhanVienDaoImpl implements NhanVienDao {

  private static final String SELECT_BY_TEN_DANG_NHAP =
      "SELECT \"maNV\", \"hoTen\", \"soDienThoai\", \"email\", \"ngayVaoLam\","
          + " \"tenDangNhap\", \"matKhau\", \"loaiQuyen\", \"trangThaiHoatDong\""
          + " FROM \"NhanVien\" WHERE \"tenDangNhap\" = ?";

  private static final String UPDATE_MAT_KHAU =
      "UPDATE \"NhanVien\" SET \"matKhau\" = ? WHERE \"maNV\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public NhanVienDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_TEN_DANG_NHAP)) {
      statement.setString(1, tenDangNhap);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapNhanVien(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu tài khoản nhân viên", e);
    }
  }

  @Override
  public void updateMatKhau(String maNV, String matKhauHash) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_MAT_KHAU)) {
      statement.setString(1, matKhauHash);
      statement.setString(2, maNV);
      int affected = statement.executeUpdate();
      if (affected != 1) {
        throw new DataAccessException("Không thể cập nhật mật khẩu nhân viên");
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật mật khẩu nhân viên", e);
    }
  }

  private NhanVien mapNhanVien(ResultSet resultSet) throws SQLException {
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
