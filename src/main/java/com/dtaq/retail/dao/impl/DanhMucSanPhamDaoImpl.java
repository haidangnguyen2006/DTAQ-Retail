package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
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

/**
 * Triển khai JDBC của {@link DanhMucSanPhamDao} thao tác trên bảng {@code "DanhMucSanPham"}.
 */
public class DanhMucSanPhamDaoImpl implements DanhMucSanPhamDao {

  private static final String SELECT_ALL =
      "SELECT \"maDanhMuc\", \"tenDanhMuc\", \"moTa\" FROM \"DanhMucSanPham\" ORDER BY \"tenDanhMuc\" ASC";

  private static final String SELECT_BY_ID =
      "SELECT \"maDanhMuc\", \"tenDanhMuc\", \"moTa\" FROM \"DanhMucSanPham\" WHERE \"maDanhMuc\" = ?";

  private static final String SELECT_BY_TEN =
      "SELECT \"maDanhMuc\", \"tenDanhMuc\", \"moTa\" FROM \"DanhMucSanPham\" "
          + "WHERE lower(btrim(\"tenDanhMuc\")) = lower(btrim(?))";

  private static final String INSERT =
      "INSERT INTO \"DanhMucSanPham\" (\"maDanhMuc\", \"tenDanhMuc\", \"moTa\") VALUES (?, ?, ?)";

  private static final String UPDATE =
      "UPDATE \"DanhMucSanPham\" SET \"tenDanhMuc\" = ?, \"moTa\" = ? WHERE \"maDanhMuc\" = ?";

  private static final String DELETE =
      "DELETE FROM \"DanhMucSanPham\" WHERE \"maDanhMuc\" = ?";

  private static final String EXISTS_DONG_BY_DANH_MUC =
      "SELECT EXISTS(SELECT 1 FROM \"DongSanPham\" WHERE \"maDanhMuc\" = ?)";

  private static final String EXISTS_BY_TEN =
      "SELECT EXISTS(SELECT 1 FROM \"DanhMucSanPham\" "
          + "WHERE lower(btrim(\"tenDanhMuc\")) = lower(btrim(?)) "
          + "AND (? IS NULL OR \"maDanhMuc\" <> ?))";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung.
   *
   * @param dataSource nguồn dữ liệu kết nối CSDL; không được {@code null}
   */
  public DanhMucSanPhamDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public List<DanhMucSanPham> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<DanhMucSanPham> list = new ArrayList<>();
      while (resultSet.next()) {
        list.add(mapDanhMuc(resultSet));
      }
      return list;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách danh mục sản phẩm", e);
    }
  }

  @Override
  public Optional<DanhMucSanPham> findById(String maDanhMuc) {
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maDanhMuc);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapDanhMuc(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu danh mục sản phẩm theo mã", e);
    }
  }

  @Override
  public Optional<DanhMucSanPham> findByTen(String tenDanhMuc) {
    if (tenDanhMuc == null || tenDanhMuc.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_TEN)) {
      statement.setString(1, tenDanhMuc);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapDanhMuc(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu danh mục sản phẩm theo tên", e);
    }
  }

  @Override
  public void insert(DanhMucSanPham danhMuc) {
    Objects.requireNonNull(danhMuc, "danhMuc");
    if (danhMuc.maDanhMuc() == null || danhMuc.maDanhMuc().isBlank()) {
      throw new IllegalArgumentException("Mã danh mục không được để trống");
    }
    if (danhMuc.tenDanhMuc() == null || danhMuc.tenDanhMuc().isBlank()) {
      throw new IllegalArgumentException("Tên danh mục không được để trống");
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT)) {
      statement.setString(1, danhMuc.maDanhMuc().trim());
      statement.setString(2, danhMuc.tenDanhMuc().trim());
      statement.setString(3, danhMuc.moTa());
      statement.executeUpdate();
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        throw new DataAccessException("Mã hoặc tên danh mục đã tồn tại trong hệ thống", e);
      }
      throw new DataAccessException("Không thể thêm mới danh mục sản phẩm", e);
    }
  }

  @Override
  public void update(DanhMucSanPham danhMuc) {
    Objects.requireNonNull(danhMuc, "danhMuc");
    if (danhMuc.maDanhMuc() == null || danhMuc.maDanhMuc().isBlank()) {
      throw new IllegalArgumentException("Mã danh mục không được để trống");
    }
    if (danhMuc.tenDanhMuc() == null || danhMuc.tenDanhMuc().isBlank()) {
      throw new IllegalArgumentException("Tên danh mục không được để trống");
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE)) {
      statement.setString(1, danhMuc.tenDanhMuc().trim());
      statement.setString(2, danhMuc.moTa());
      statement.setString(3, danhMuc.maDanhMuc().trim());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new DataAccessException("Không tìm thấy danh mục để cập nhật: " + danhMuc.maDanhMuc());
      }
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        throw new DataAccessException("Tên danh mục đã trùng với một danh mục khác", e);
      }
      throw new DataAccessException("Không thể cập nhật danh mục sản phẩm", e);
    }
  }

  @Override
  public void delete(String maDanhMuc) {
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      throw new IllegalArgumentException("Mã danh mục không được để trống");
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE)) {
      statement.setString(1, maDanhMuc.trim());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new DataAccessException("Không tìm thấy danh mục để xóa: " + maDanhMuc);
      }
    } catch (SQLException e) {
      if ("23503".equals(e.getSQLState())) {
        throw new DataAccessException("Không thể xóa danh mục đang có dòng sản phẩm liên kết", e);
      }
      throw new DataAccessException("Không thể xóa danh mục sản phẩm", e);
    }
  }

  @Override
  public boolean existsDongByDanhMuc(String maDanhMuc) {
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return false;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_DONG_BY_DANH_MUC)) {
      statement.setString(1, maDanhMuc.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() && resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra dòng sản phẩm theo danh mục", e);
    }
  }

  @Override
  public boolean existsByTen(String tenDanhMuc, String excludeMaDanhMuc) {
    if (tenDanhMuc == null || tenDanhMuc.isBlank()) {
      return false;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_BY_TEN)) {
      statement.setString(1, tenDanhMuc.trim());
      if (excludeMaDanhMuc != null && !excludeMaDanhMuc.isBlank()) {
        statement.setString(2, excludeMaDanhMuc.trim());
        statement.setString(3, excludeMaDanhMuc.trim());
      } else {
        statement.setNull(2, java.sql.Types.VARCHAR);
        statement.setNull(3, java.sql.Types.VARCHAR);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() && resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra trùng tên danh mục", e);
    }
  }

  private DanhMucSanPham mapDanhMuc(ResultSet rs) throws SQLException {
    return new DanhMucSanPham(
        rs.getString("maDanhMuc"),
        rs.getString("tenDanhMuc"),
        rs.getString("moTa"));
  }
}
