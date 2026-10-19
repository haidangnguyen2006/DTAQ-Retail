package com.dtaq.retail.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Static reader for database configuration resolved from system properties, environment variables, then defaults. */
public final class DbConfig {

  private static final Logger log = LoggerFactory.getLogger(DbConfig.class);

  private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/dtaq_retail";
  private static final String DEFAULT_USER = "postgres";
  private static final String DEFAULT_PASSWORD = "";
  private static final int DEFAULT_POOL_SIZE = 5;

  private static Path propertiesPath = Paths.get("db.properties");
  private static volatile Properties fileProps;

  private DbConfig() {
  }

  public static String jdbcUrl() {
    return resolve("DTAQ_DB_URL", DEFAULT_URL);
  }

  public static String username() {
    return resolve("DTAQ_DB_USER", DEFAULT_USER);
  }

  public static String password() {
    String value = resolve("DTAQ_DB_PASSWORD", DEFAULT_PASSWORD);
    if (value.isBlank()) {
      log.warn("Database password (DTAQ_DB_PASSWORD) is not set; connecting without a password");
    }
    return value;
  }

  public static int maximumPoolSize() {
    String value = resolve("DTAQ_DB_POOL_SIZE", null);
    if (value == null || value.isBlank()) {
      return DEFAULT_POOL_SIZE;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      log.warn("Invalid DTAQ_DB_POOL_SIZE value; using default {}", DEFAULT_POOL_SIZE);
      return DEFAULT_POOL_SIZE;
    }
  }

  private static String resolve(String key, String defaultValue) {
    String value = System.getProperty(key);
    if (value == null) {
      value = System.getenv(key);
    }
    if (value == null) {
      value = fileProperties().getProperty(key);
    }
    return value == null ? defaultValue : value;
  }

  private static Properties fileProperties() {
    Properties current = fileProps;
    if (current == null) {
      synchronized (DbConfig.class) {
        current = fileProps;
        if (current == null) {
          current = load(propertiesPath);
          fileProps = current;
        }
      }
    }
    return current;
  }

  private static Properties load(Path path) {
    Properties properties = new Properties();
    if (!Files.isReadable(path)) {
      log.info("No database configuration file found at {}", path.toAbsolutePath());
      return properties;
    }
    try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      properties.load(reader);
      log.info("Loaded database configuration from {}", path.toAbsolutePath());
    } catch (IOException e) {
      log.warn("Failed to load database configuration from {}: {}", path.toAbsolutePath(), e.getMessage());
    }
    return properties;
  }

  static synchronized void setPropertiesFileForTest(Path path) {
    propertiesPath = (path == null ? Paths.get("db.properties") : path);
    fileProps = null;
  }
}
