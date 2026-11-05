package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.exception.DataAccessException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link LichSuSaoLuuDao} trên bảng {@code "LichSuSaoLuu"}. */
public class LichSuSaoLuuDaoImpl implements LichSuSaoLuuDao {

  private static final String SELECT_COT =
      "SELECT \"id\", \"filePath\", \"fileSize\", \"sha256\", \"loai\", \"trangThai\","
          + " \"thongBao\", \"createdAt\", \"createdBy\", \"phienBanPg\", \"tenCsdl\""
          + " FROM \"LichSuSaoLuu\"";

  private static final String SELECT_ALL =
      SELECT_COT + " ORDER BY \"createdAt\" DESC, \"id\" DESC";

  private static final String SELECT_MOI_NHAT_THANH_CONG =
      SELECT_COT
          + " WHERE \"trangThai\" = 'SUCCESS'"
          + " ORDER BY \"createdAt\" DESC, \"id\" DESC LIMIT 1";

  private static final String INSERT_LICH_SU =
      "INSERT INTO \"LichSuSaoLuu\" (\"filePath\", \"fileSize\", \"sha256\", \"loai\","
          + " \"trangThai\", \"thongBao\", \"createdAt\", \"createdBy\", \"phienBanPg\","
          + " \"tenCsdl\") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String EXISTS_FILE_PATH =
      "SELECT EXISTS (SELECT 1 FROM \"LichSuSaoLuu\" WHERE \"filePath\" = ?)";

  private final Supplier<DataSource> dataSourceSupplier;

  /**
   * Khởi tạo DAO với một nguồn dữ liệu cố định.
   *
   * <p>Nguồn dữ liệu cố định chỉ dùng được khi bể kết nối không bị dựng lại. Ứng dụng thật nên
   * dùng {@link #LichSuSaoLuuDaoImpl(Supplier)} để DAO sống sót qua lần
   * {@link com.dtaq.retail.config.ConnectDB#reset()} của dịch vụ phục hồi.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public LichSuSaoLuuDaoImpl(DataSource dataSource) {
    this(() -> dataSource);
    Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Khởi tạo DAO lấy nguồn dữ liệu qua một nhà cung cấp.
   *
   * <p>Mỗi thao tác đều gọi lại nhà cung cấp nên DAO luôn dùng bể kết nối hiện tại, kể cả sau khi
   * {@link com.dtaq.retail.config.ConnectDB#reset()} đã đóng bể cũ và tạo bể mới. Nhờ vậy lịch sử
   * sao lưu vẫn ghi được sau một lần tự động phục hồi.
   *
   * @param dataSourceSupplier nhà cung cấp nguồn dữ liệu; không được {@code null}
   */
  public LichSuSaoLuuDaoImpl(Supplier<DataSource> dataSourceSupplier) {
    this.dataSourceSupplier = Objects.requireNonNull(dataSourceSupplier, "dataSourceSupplier");
  }

  @Override
  public void insert(BackupRecord banGhi) {
    Objects.requireNonNull(banGhi, "banGhi");
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT_LICH_SU)) {
      statement.setString(1, banGhi.filePath().toString());
      statement.setLong(2, banGhi.fileSize());
      statement.setString(3, banGhi.sha256());
      statement.setString(4, banGhi.loai().name());
      statement.setString(5, banGhi.trangThai().name());
      statement.setString(6, banGhi.thongBao());
      statement.setTimestamp(7, Timestamp.from(banGhi.createdAt()));
      statement.setString(8, banGhi.createdBy());
      statement.setString(9, banGhi.phienBanPg());
      statement.setString(10, banGhi.tenCsdl());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException(
            "Không thể lưu lịch sử sao lưu " + banGhi.fileName());
      }
    } catch (SQLException e) {
      throw new DataAccessException(
          "Không thể lưu lịch sử sao lưu " + banGhi.fileName(), e);
    }
  }

  @Override
  public List<BackupRecord> findAll() {
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<BackupRecord> ketQua = new ArrayList<>();
      while (resultSet.next()) {
        ketQua.add(mapBanGhi(resultSet));
      }
      return ketQua;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải lịch sử sao lưu", e);
    }
  }

  @Override
  public Optional<BackupRecord> findMoiNhatThanhCong() {
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement =
            connection.prepareStatement(SELECT_MOI_NHAT_THANH_CONG);
        ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        return Optional.empty();
      }
      return Optional.of(mapBanGhi(resultSet));
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu lần sao lưu thành công gần nhất", e);
    }
  }

  @Override
  public boolean existsByFilePath(String filePath) {
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_FILE_PATH)) {
      statement.setString(1, filePath);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra lịch sử sao lưu", e);
    }
  }

  private static BackupRecord mapBanGhi(ResultSet resultSet) throws SQLException {
    long id = resultSet.getLong("id");
    Long idHoacNull = resultSet.wasNull() ? null : id;
    Timestamp createdAt = resultSet.getTimestamp("createdAt");
    Instant thoiDiem = createdAt == null ? null : createdAt.toInstant();
    return new BackupRecord(
        idHoacNull,
        Path.of(resultSet.getString("filePath")),
        resultSet.getLong("fileSize"),
        resultSet.getString("sha256"),
        LoaiSaoLuu.valueOf(resultSet.getString("loai")),
        TrangThaiSaoLuu.valueOf(resultSet.getString("trangThai")),
        resultSet.getString("thongBao"),
        thoiDiem,
        resultSet.getString("createdBy"),
        resultSet.getString("phienBanPg"),
        resultSet.getString("tenCsdl"));
  }
}
