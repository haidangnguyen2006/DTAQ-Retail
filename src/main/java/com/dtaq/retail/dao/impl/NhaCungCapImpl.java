package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.entity.NhaCungCap;
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

/** Triển khai JDBC của {@link NhaCungCapDao} trên bảng {@code "NhaCungCap"}. */
public final class NhaCungCapImpl implements NhaCungCapDao {

  private static final String SELECT_BY_ID =
      "SELECT \"maNCC\", \"tenNCC\" FROM \"NhaCungCap\" WHERE \"maNCC\" = ?";

  private static final String SELECT_ALL =
      "SELECT \"maNCC\", \"tenNCC\" FROM \"NhaCungCap\" ORDER BY \"maNCC\"";

  private static final String INSERT_NHA_CUNG_CAP =
      "INSERT INTO \"NhaCungCap\" (\"maNCC\", \"tenNCC\") VALUES (?, ?)";

  private static final String UPDATE_TEN_NCC =
      "UPDATE \"NhaCungCap\" SET \"tenNCC\" = ? WHERE \"maNCC\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public NhaCungCapImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(Connection ketNoi, NhaCungCap nhaCungCap) {
    if (nhaCungCap == null || nhaCungCap.maNCC() == null || nhaCungCap.maNCC().isBlank()) {
      throw new IllegalArgumentException("Nhà cung cấp không hợp lệ");
    }
    Objects.requireNonNull(ketNoi, "ketNoi");
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_NHA_CUNG_CAP)) {
      statement.setString(1, nhaCungCap.maNCC());
      statement.setString(2, nhaCungCap.tenNCC());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu nhà cung cấp " + nhaCungCap.maNCC());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu nhà cung cấp " + nhaCungCap.maNCC(), e);
    }
  }

  @Override
  public Optional<NhaCungCap> findById(String maNCC) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maNCC);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapNhaCungCap(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu nhà cung cấp", e);
    }
  }

  @Override
  public List<NhaCungCap> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<NhaCungCap> ketQua = new ArrayList<>();
      while (resultSet.next()) {
        ketQua.add(mapNhaCungCap(resultSet));
      }
      return ketQua;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu danh sách nhà cung cấp", e);
    }
  }

  @Override
  public void updateTenNCC(String maNCC, String tenNCC) {
    Objects.requireNonNull(maNCC, "maNCC");
    Objects.requireNonNull(tenNCC, "tenNCC");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_TEN_NCC)) {
      statement.setString(1, tenNCC);
      statement.setString(2, maNCC);
      int affected = statement.executeUpdate();
      if (affected != 1) {
        throw new DataAccessException("Không thể cập nhật tên nhà cung cấp " + maNCC);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật tên nhà cung cấp " + maNCC, e);
    }
  }

  private NhaCungCap mapNhaCungCap(ResultSet resultSet) throws SQLException {
    return new NhaCungCap(
        resultSet.getString("maNCC"),
        resultSet.getString("tenNCC"));
  }
}
