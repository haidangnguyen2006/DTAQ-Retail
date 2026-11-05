package com.dtaq.retail.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lazily creates and owns a single shared HikariCP connection pool for the application. */
public final class ConnectDB {

  private static final Logger log = LoggerFactory.getLogger(ConnectDB.class);
  private static final String POOL_NAME = "dtaq-retail-pool";

  /** Ngắt mọi kết nối khác tới cơ sở dữ liệu hiện tại, phục vụ phục hồi dữ liệu. */
  private static final String SQL_NGAT_KET_NOI =
      "SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
          + "WHERE datname = current_database() AND pid <> pg_backend_pid()";

  private static HikariDataSource dataSource;
  private static boolean shutdownHookRegistered;

  private ConnectDB() {
  }

  private static synchronized HikariDataSource dataSource() {
    if (dataSource == null) {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(DbConfig.jdbcUrl());
      config.setUsername(DbConfig.username());
      config.setPassword(DbConfig.password());
      config.setMaximumPoolSize(DbConfig.maximumPoolSize());
      config.setPoolName(POOL_NAME);
      config.setInitializationFailTimeout(-1);
      dataSource = new HikariDataSource(config);
      if (!shutdownHookRegistered) {
        Runtime.getRuntime().addShutdownHook(new Thread(ConnectDB::close, "dtaq-retail-shutdown"));
        shutdownHookRegistered = true;
      }
    }
    return dataSource;
  }

  public static DataSource getDataSource() {
    return dataSource();
  }

  public static boolean isAvailable() {
    try {
      try (Connection connection = dataSource().getConnection()) {
        return connection != null && !connection.isClosed();
      }
    } catch (Exception e) {
      log.warn("Database is not available: {}", e.getMessage());
      return false;
    }
  }

  public static synchronized void close() {
    HikariDataSource current = dataSource;
    if (current != null && !current.isClosed()) {
      current.close();
    }
  }

  /**
   * Đóng và xóa bể kết nối hiện tại để lần gọi sau tạo lại từ cấu hình.
   *
   * <p>Dùng trước khi phục hồi cơ sở dữ liệu: bể cũ không còn được giữ lại và có thể được tạo mới
   * sau khi quá trình phục hồi hoàn tất. Khác với {@link #close()}, trường tĩnh được đặt về
   * {@code null} nên bể sẽ được dựng lại khi cần.
   */
  public static synchronized void reset() {
    HikariDataSource current = dataSource;
    dataSource = null;
    if (current != null && !current.isClosed()) {
      current.close();
    }
  }

  /**
   * Ngắt các kết nối khác đang mở tới cơ sở dữ liệu hiện tại.
   *
   * <p>Cần thiết khi phục hồi vì {@code pg_restore --clean} không thể xóa đối tượng đang bị phiên
   * khác sử dụng. Lỗi thiếu quyền hoặc phiên bản PostgreSQL không hỗ trợ chỉ được ghi cảnh báo.
   */
  public static void terminateOtherConnections() {
    try (Connection connection = dataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(SQL_NGAT_KET_NOI)) {
      statement.execute();
    } catch (Exception e) {
      log.warn("Không ngắt được các kết nối còn lại tới cơ sở dữ liệu: {}", e.getMessage());
    }
  }
}
