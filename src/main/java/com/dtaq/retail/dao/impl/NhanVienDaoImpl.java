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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link NhanVienDao} trên bảng {@code "NhanVien"}. */
public class NhanVienDaoImpl implements NhanVienDao {

  private static final String SELECT_COT =
      "SELECT \"maNV\", \"hoTen\", \"soDienThoai\", \"email\", \"ngayVaoLam\","
          + " \"tenDangNhap\", \"matKhau\", \"loaiQuyen\", \"trangThaiHoatDong\""
          + " FROM \"NhanVien\"";

  private static final String SELECT_BY_TEN_DANG_NHAP =
      SELECT_COT + " WHERE \"tenDangNhap\" = ?";

  private static final String SELECT_BY_MA = SELECT_COT + " WHERE \"maNV\" = ?";

  private static final String SELECT_ALL = SELECT_COT + " ORDER BY \"maNV\"";

  private static final String UPDATE_MAT_KHAU =
      "UPDATE \"NhanVien\" SET \"matKhau\" = ? WHERE \"maNV\" = ?";

  private static final String INSERT_NHAN_VIEN =
      "INSERT INTO \"NhanVien\" (\"maNV\", \"hoTen\", \"soDienThoai\", \"email\", \"ngayVaoLam\","
          + " \"tenDangNhap\", \"matKhau\", \"loaiQuyen\", \"trangThaiHoatDong\")"
          + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_NHAN_VIEN =
      "UPDATE \"NhanVien\" SET \"hoTen\" = ?, \"soDienThoai\" = ?, \"email\" = ?,"
          + " \"ngayVaoLam\" = ? WHERE \"maNV\" = ?";

  private static final String UPDATE_TRANG_THAI =
      "UPDATE \"NhanVien\" SET \"trangThaiHoatDong\" = ? WHERE \"maNV\" = ?";

  private static final String EXISTS_TEN_DANG_NHAP =
      "SELECT EXISTS (SELECT 1 FROM \"NhanVien\" WHERE \"tenDangNhap\" = ?)";

  private static final String EXISTS_MA_NV =
      "SELECT EXISTS (SELECT 1 FROM \"NhanVien\" WHERE \"maNV\" = ?)";

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

  @Override
  public List<NhanVien> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<NhanVien> ketQua = new ArrayList<>();
      while (resultSet.next()) {
        ketQua.add(mapNhanVien(resultSet));
      }
      return ketQua;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách nhân viên", e);
    }
  }

  @Override
  public Optional<NhanVien> findByMa(String maNV) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_MA)) {
      statement.setString(1, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapNhanVien(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu nhân viên", e);
    }
  }

  @Override
  public void insert(Connection ketNoi, NhanVien nhanVien) {
    kiemTraMa(nhanVien);
    Objects.requireNonNull(ketNoi, "ketNoi");
    // Không đóng kết nối và không đổi auto-commit: giao dịch do bên gọi quản lý.
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_NHAN_VIEN)) {
      statement.setString(1, nhanVien.maNV());
      statement.setString(2, nhanVien.hoTen());
      statement.setString(3, nhanVien.soDienThoai());
      statement.setString(4, nhanVien.email());
      statement.setObject(5, nhanVien.ngayVaoLam());
      statement.setString(6, nhanVien.tenDangNhap());
      statement.setString(7, nhanVien.matKhau());
      statement.setString(8, nhanVien.loaiQuyen() == null ? null : nhanVien.loaiQuyen().name());
      statement.setBoolean(9, nhanVien.trangThaiHoatDong());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu nhân viên " + nhanVien.maNV());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu nhân viên " + nhanVien.maNV(), e);
    }
  }

  @Override
  public void update(NhanVien nhanVien) {
    kiemTraMa(nhanVien);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_NHAN_VIEN)) {
      statement.setString(1, nhanVien.hoTen());
      statement.setString(2, nhanVien.soDienThoai());
      statement.setString(3, nhanVien.email());
      statement.setObject(4, nhanVien.ngayVaoLam());
      statement.setString(5, nhanVien.maNV());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể cập nhật nhân viên " + nhanVien.maNV());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật nhân viên " + nhanVien.maNV(), e);
    }
  }

  @Override
  public void updateTrangThai(String maNV, boolean trangThaiHoatDong) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_TRANG_THAI)) {
      statement.setBoolean(1, trangThaiHoatDong);
      statement.setString(2, maNV);
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể cập nhật trạng thái nhân viên");
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật trạng thái nhân viên", e);
    }
  }

  @Override
  public boolean existsTenDangNhap(String tenDangNhap) {
    return exists(EXISTS_TEN_DANG_NHAP, tenDangNhap, "Không thể kiểm tra tên đăng nhập");
  }

  @Override
  public boolean existsMaNV(String maNV) {
    return exists(EXISTS_MA_NV, maNV, "Không thể kiểm tra mã nhân viên");
  }

  private boolean exists(String sql, String giaTri, String thongBaoLoi) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, giaTri);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException(thongBaoLoi, e);
    }
  }

  private static void kiemTraMa(NhanVien nhanVien) {
    if (nhanVien == null || nhanVien.maNV() == null || nhanVien.maNV().isBlank()) {
      throw new IllegalArgumentException("Nhân viên không hợp lệ");
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
