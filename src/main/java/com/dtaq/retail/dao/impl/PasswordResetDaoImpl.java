package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link PasswordResetDao} trên bảng {@code "DatLaiMatKhau"}. */
public class PasswordResetDaoImpl implements PasswordResetDao {

  private static final String SAVE_TICKET =
      "INSERT INTO \"DatLaiMatKhau\""
          + " (\"maNV\", \"maBam\", \"taoLuc\", \"hetHanLuc\", \"soLanThu\", \"lanGuiCuoi\")"
          + " VALUES (?, ?, ?, ?, ?, ?)"
          + " ON CONFLICT (\"maNV\") DO UPDATE SET"
          + " \"maBam\" = EXCLUDED.\"maBam\","
          + " \"taoLuc\" = EXCLUDED.\"taoLuc\","
          + " \"hetHanLuc\" = EXCLUDED.\"hetHanLuc\","
          + " \"soLanThu\" = EXCLUDED.\"soLanThu\","
          + " \"lanGuiCuoi\" = EXCLUDED.\"lanGuiCuoi\"";

  private static final String SELECT_TICKET =
      "SELECT \"maNV\", \"maBam\", \"taoLuc\", \"hetHanLuc\", \"soLanThu\", \"lanGuiCuoi\""
          + " FROM \"DatLaiMatKhau\" WHERE \"maNV\" = ?";

  private static final String UPDATE_ATTEMPTS =
      "UPDATE \"DatLaiMatKhau\" SET \"soLanThu\" = ? WHERE \"maNV\" = ?";

  private static final String DELETE_TICKET =
      "DELETE FROM \"DatLaiMatKhau\" WHERE \"maNV\" = ?";

  private static final String UPDATE_MAT_KHAU =
      "UPDATE \"NhanVien\" SET \"matKhau\" = ? WHERE \"maNV\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public PasswordResetDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void saveTicket(Ticket ticket) {
    Objects.requireNonNull(ticket, "ticket");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SAVE_TICKET)) {
      statement.setString(1, ticket.maNV());
      statement.setString(2, ticket.codeHash());
      setInstant(statement, 3, ticket.createdAt());
      setInstant(statement, 4, ticket.expiresAt());
      statement.setInt(5, ticket.soLanThu());
      setInstant(statement, 6, ticket.lastSentAt());
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu phiếu đặt lại mật khẩu", e);
    }
  }

  @Override
  public Optional<Ticket> findTicket(String maNV) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_TICKET)) {
      statement.setString(1, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new Ticket(
            resultSet.getString("maNV"),
            resultSet.getString("maBam"),
            getInstant(resultSet, "taoLuc"),
            getInstant(resultSet, "hetHanLuc"),
            resultSet.getInt("soLanThu"),
            getInstant(resultSet, "lanGuiCuoi")));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu phiếu đặt lại mật khẩu", e);
    }
  }

  @Override
  public void updateAttempts(String maNV, int soLanThu) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_ATTEMPTS)) {
      statement.setInt(1, soLanThu);
      statement.setString(2, maNV);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật số lần thử đặt lại mật khẩu", e);
    }
  }

  @Override
  public void deleteTicket(String maNV) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_TICKET)) {
      statement.setString(1, maNV);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException("Không thể xóa phiếu đặt lại mật khẩu", e);
    }
  }

  @Override
  public void consume(String maNV, String newPasswordHash) {
    try (Connection connection = dataSource.getConnection()) {
      try {
        connection.setAutoCommit(false);
        updateMatKhau(connection, maNV, newPasswordHash);
        deleteTicket(connection, maNV);
        connection.commit();
      } catch (SQLException e) {
        rollbackQuietly(connection);
        throw new DataAccessException("Không thể hoàn tất đặt lại mật khẩu", e);
      } finally {
        restoreAutoCommit(connection);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể hoàn tất đặt lại mật khẩu", e);
    }
  }

  private void updateMatKhau(Connection connection, String maNV, String newPasswordHash)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_MAT_KHAU)) {
      statement.setString(1, newPasswordHash);
      statement.setString(2, maNV);
      if (statement.executeUpdate() != 1) {
        connection.rollback();
        throw new DataAccessException("Không thể hoàn tất đặt lại mật khẩu");
      }
    }
  }

  private void deleteTicket(Connection connection, String maNV) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(DELETE_TICKET)) {
      statement.setString(1, maNV);
      statement.executeUpdate();
    }
  }

  private void rollbackQuietly(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException ignored) {
      // Giữ nguyên lỗi gốc; không ghi log nội dung phiếu hay mã băm.
    }
  }

  private void restoreAutoCommit(Connection connection) {
    try {
      connection.setAutoCommit(true);
    } catch (SQLException ignored) {
      // Connection đã đóng hoặc không khôi phục được; pool sẽ tự loại bỏ connection này.
    }
  }

  private static void setInstant(PreparedStatement statement, int index, Instant instant)
      throws SQLException {
    statement.setObject(index, OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
  }

  private static Instant getInstant(ResultSet resultSet, String column) throws SQLException {
    return resultSet.getObject(column, OffsetDateTime.class).toInstant();
  }
}
