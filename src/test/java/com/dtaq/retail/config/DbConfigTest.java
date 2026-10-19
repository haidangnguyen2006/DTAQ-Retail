package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link DbConfig} resolution order and default values. */
class DbConfigTest {

  @TempDir
  Path tempDir;

  @AfterEach
  void tearDown() {
    System.clearProperty("DTAQ_DB_USER");
    DbConfig.setPropertiesFileForTest(null);
  }

  @Test
  void jdbcUrlDefaultsToPostgresPrefix() {
    assertTrue(DbConfig.jdbcUrl().startsWith("jdbc:postgresql://"));
  }

  @Test
  void maximumPoolSizeIsPositive() {
    assertTrue(DbConfig.maximumPoolSize() >= 1);
  }

  @Test
  void missingPropertiesFileFallsBackToDefaults() {
    DbConfig.setPropertiesFileForTest(tempDir.resolve("absent.properties"));
    Assumptions.assumeTrue(System.getenv("DTAQ_DB_USER") == null);
    System.clearProperty("DTAQ_DB_USER");
    assertTrue(DbConfig.jdbcUrl().startsWith("jdbc:postgresql://"));
    assertEquals("postgres", DbConfig.username());
  }

  @Test
  void fileValueOverridesDefault() throws IOException {
    DbConfig.setPropertiesFileForTest(writeProperties("db-user.properties", "DTAQ_DB_USER=fileuser"));
    Assumptions.assumeTrue(System.getenv("DTAQ_DB_USER") == null);
    System.clearProperty("DTAQ_DB_USER");
    assertEquals("fileuser", DbConfig.username());
  }

  @Test
  void systemPropertyOverridesFile() throws IOException {
    DbConfig.setPropertiesFileForTest(writeProperties("db-user-sys.properties", "DTAQ_DB_USER=fileuser"));
    System.setProperty("DTAQ_DB_USER", "sysuser");
    assertEquals("sysuser", DbConfig.username());
  }

  @Test
  void poolSizeIsReadFromFile() throws IOException {
    DbConfig.setPropertiesFileForTest(writeProperties("db-pool.properties", "DTAQ_DB_POOL_SIZE=7"));
    Assumptions.assumeTrue(System.getenv("DTAQ_DB_POOL_SIZE") == null);
    System.clearProperty("DTAQ_DB_POOL_SIZE");
    assertEquals(7, DbConfig.maximumPoolSize());
  }

  @Test
  void unparsablePoolSizeFallsBackToDefault() throws IOException {
    DbConfig.setPropertiesFileForTest(writeProperties("db-pool-bad.properties", "DTAQ_DB_POOL_SIZE=abc"));
    Assumptions.assumeTrue(System.getenv("DTAQ_DB_POOL_SIZE") == null);
    System.clearProperty("DTAQ_DB_POOL_SIZE");
    assertEquals(5, DbConfig.maximumPoolSize());
  }

  @Test
  void unreadablePathDoesNotThrow() {
    DbConfig.setPropertiesFileForTest(tempDir);
    assertNotNull(DbConfig.jdbcUrl());
  }

  private Path writeProperties(String fileName, String content) throws IOException {
    Path file = tempDir.resolve(fileName);
    Files.writeString(file, content);
    return file;
  }
}
