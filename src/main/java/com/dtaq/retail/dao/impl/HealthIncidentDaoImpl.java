package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthIncident.Severity;
import com.dtaq.retail.entity.HealthIncident.TrangThai;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link HealthIncidentDao} trên bảng {@code "SuCoSucKhoe"}. */
public class HealthIncidentDaoImpl implements HealthIncidentDao {

  private static final String SELECT_COT =
      "SELECT \"id\", \"severity\", \"detail\", \"trangThai\", \"backupUsed\","
          + " \"detectedAt\", \"resolvedAt\", \"resolvedBy\""
          + " FROM \"SuCoSucKhoe\"";

  private static final String SELECT_ALL =
      SELECT_COT + " ORDER BY \"detectedAt\" DESC, \"id\" DESC";

  private static final String SELECT_MOI_NHAT_DANG_CHO =
      SELECT_COT
          + " WHERE \"trangThai\" = 'PENDING'"
          + " ORDER BY \"detectedAt\" DESC LIMIT 1";

  private static final String SELECT_BY_ID = SELECT_COT + " WHERE \"id\" = ?";

  private static final String INSERT_SU_CO =
      "INSERT INTO \"SuCoSucKhoe\" (\"severity\", \"detail\", \"trangThai\", \"backupUsed\","
          + " \"detectedAt\", \"resolvedAt\", \"resolvedBy\") VALUES (?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_KET_THUC =
      "UPDATE \"SuCoSucKhoe\" SET \"trangThai\" = ?, \"resolvedAt\" = ?, \"resolvedBy\" = ?,"
          + " \"backupUsed\" = ? WHERE \"id\" = ?";

  private final Supplier<DataSource> dataSourceSupplier;

  /**
   * Khởi tạo DAO với một nguồn dữ liệu cố định.
   *
   * <p>Nguồn dữ liệu cố định chỉ dùng được khi bể kết nối không bị dựng lại. Ứng dụng thật nên
   * dùng {@link #HealthIncidentDaoImpl(Supplier)} để DAO sống sót qua lần
   * {@link com.dtaq.retail.config.ConnectDB#reset()} của dịch vụ phục hồi.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public HealthIncidentDaoImpl(DataSource dataSource) {
    this(() -> dataSource);
    Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Khởi tạo DAO lấy nguồn dữ liệu qua một nhà cung cấp.
   *
   * <p>Mỗi thao tác đều gọi lại nhà cung cấp nên DAO luôn dùng bể kết nối hiện tại, kể cả sau khi
   * dịch vụ phục hồi đã đóng bể cũ và tạo bể mới.
   *
   * @param dataSourceSupplier nhà cung cấp nguồn dữ liệu; không được {@code null}
   */
  public HealthIncidentDaoImpl(Supplier<DataSource> dataSourceSupplier) {
    this.dataSourceSupplier = Objects.requireNonNull(dataSourceSupplier, "dataSourceSupplier");
  }

  @Override
  public long insert(HealthIncident incident) {
    Objects.requireNonNull(incident, "incident");
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement =
            connection.prepareStatement(INSERT_SU_CO, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, incident.severity().name());
      statement.setString(2, incident.detail());
      statement.setString(3, incident.trangThai().name());
      statement.setString(4, incident.backupUsed());
      statement.setTimestamp(5, Timestamp.from(incident.detectedAt()));
      datThoiDiem(statement, 6, incident.resolvedAt());
      statement.setString(7, incident.resolvedBy());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu sự cố sức khỏe");
      }
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (!keys.next()) {
          throw new DataAccessException("Không thể lấy mã sự cố sức khỏe vừa lưu");
        }
        return keys.getLong(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu sự cố sức khỏe", e);
    }
  }

  @Override
  public Optional<HealthIncident> findById(long id) {
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setLong(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapSuCo(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu sự cố sức khỏe " + id, e);
    }
  }

  @Override
  public Optional<HealthIncident> findMoiNhatDangCho() {
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement =
            connection.prepareStatement(SELECT_MOI_NHAT_DANG_CHO);
        ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        return Optional.empty();
      }
      return Optional.of(mapSuCo(resultSet));
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu sự cố sức khỏe đang chờ", e);
    }
  }

  @Override
  public List<HealthIncident> findAll() {
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<HealthIncident> ketQua = new ArrayList<>();
      while (resultSet.next()) {
        ketQua.add(mapSuCo(resultSet));
      }
      return ketQua;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách sự cố sức khỏe", e);
    }
  }

  @Override
  public void capNhatKetThuc(
      long id,
      TrangThai trangThai,
      Instant resolvedAt,
      String resolvedBy,
      String backupUsed) {
    Objects.requireNonNull(trangThai, "trangThai");
    try (Connection connection = dataSourceSupplier.get().getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_KET_THUC)) {
      statement.setString(1, trangThai.name());
      datThoiDiem(statement, 2, resolvedAt);
      statement.setString(3, resolvedBy);
      statement.setString(4, backupUsed);
      statement.setLong(5, id);
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể kết thúc sự cố sức khỏe " + id);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kết thúc sự cố sức khỏe " + id, e);
    }
  }

  private static void datThoiDiem(PreparedStatement statement, int chiSo, Instant thoiDiem)
      throws SQLException {
    if (thoiDiem == null) {
      statement.setNull(chiSo, Types.TIMESTAMP);
    } else {
      statement.setTimestamp(chiSo, Timestamp.from(thoiDiem));
    }
  }

  private static HealthIncident mapSuCo(ResultSet resultSet) throws SQLException {
    long id = resultSet.getLong("id");
    Long idHoacNull = resultSet.wasNull() ? null : id;
    Timestamp detectedAt = resultSet.getTimestamp("detectedAt");
    Instant thoiDiemPhatHien = detectedAt == null ? null : detectedAt.toInstant();
    Timestamp resolvedAt = resultSet.getTimestamp("resolvedAt");
    Instant thoiDiemKetThuc = resultSet.wasNull() ? null : resolvedAt.toInstant();
    return new HealthIncident(
        idHoacNull,
        Severity.valueOf(resultSet.getString("severity")),
        resultSet.getString("detail"),
        TrangThai.valueOf(resultSet.getString("trangThai")),
        resultSet.getString("backupUsed"),
        thoiDiemPhatHien,
        thoiDiemKetThuc,
        resultSet.getString("resolvedBy"));
  }
}
