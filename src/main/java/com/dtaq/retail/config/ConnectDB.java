package com.dtaq.retail.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lazily creates and owns a single shared HikariCP connection pool for the application. */
public final class ConnectDB {

  private static final Logger log = LoggerFactory.getLogger(ConnectDB.class);
  private static final String POOL_NAME = "dtaq-retail-pool";

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
}
