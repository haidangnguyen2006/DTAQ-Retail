package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử đọc/ghi tệp cấu hình sao lưu do giao diện chỉnh sửa. */
class BackupSettingsStoreTest {

  @TempDir
  Path tempDir;

  @AfterEach
  void tearDown() {
    BackupSettingsStore.setFileForTest(null);
  }

  @Test
  void luuRoiDocLaiDuocGiaTri() {
    BackupSettingsStore.setFileForTest(tempDir.resolve("backup.properties"));

    BackupSettingsStore.luu(LocalTime.of(6, 30), 12);

    assertEquals(Optional.of(LocalTime.of(6, 30)), BackupSettingsStore.gioDaLuu());
    assertEquals(Optional.of(12), BackupSettingsStore.chuKyDaLuu());
  }

  @Test
  void tepLuuChuaCaHaiKhoaVaKhongDeLaiTepTam() throws IOException {
    Path file = tempDir.resolve("backup.properties");
    BackupSettingsStore.setFileForTest(file);

    BackupSettingsStore.luu(LocalTime.of(6, 30), 12);

    Properties properties = doc(file);
    assertEquals("06:30", properties.getProperty(BackupConfig.KEY_GIO_CHAY));
    assertEquals("12", properties.getProperty(BackupConfig.KEY_CHU_KY_GIO));
    assertFalse(Files.exists(tempDir.resolve("backup.properties.tmp")));
  }

  @Test
  void luuLaiGiuNguyenKhoaKhacTrongTep() throws IOException {
    Path file = tempDir.resolve("backup.properties");
    Files.writeString(file,
        "DTAQ_BACKUP_DIR=/mnt/sao-luu\nDTAQ_BACKUP_KEEP_DAYS=3\n",
        StandardCharsets.UTF_8);
    BackupSettingsStore.setFileForTest(file);

    BackupSettingsStore.luu(LocalTime.of(23, 5), 24);

    Properties properties = doc(file);
    assertEquals("/mnt/sao-luu", properties.getProperty(BackupConfig.KEY_THU_MUC));
    assertEquals("3", properties.getProperty(BackupConfig.KEY_SO_NGAY_GIU));
    assertEquals("23:05", properties.getProperty(BackupConfig.KEY_GIO_CHAY));
    assertEquals("24", properties.getProperty(BackupConfig.KEY_CHU_KY_GIO));
  }

  @Test
  void chuKyNgoaiKhoangBiTuChoi() {
    BackupSettingsStore.setFileForTest(tempDir.resolve("backup.properties"));

    assertThrows(IllegalArgumentException.class,
        () -> BackupSettingsStore.luu(LocalTime.of(6, 30), 0));
    assertThrows(IllegalArgumentException.class,
        () -> BackupSettingsStore.luu(LocalTime.of(6, 30), 169));
    assertFalse(Files.exists(tempDir.resolve("backup.properties")));
  }

  @Test
  void gioNullBiTuChoi() {
    BackupSettingsStore.setFileForTest(tempDir.resolve("backup.properties"));

    assertThrows(IllegalArgumentException.class,
        () -> BackupSettingsStore.luu(null, 12));
  }

  @Test
  void tepThieuTraVeRong() {
    BackupSettingsStore.setFileForTest(tempDir.resolve("khong-ton-tai.properties"));

    assertTrue(BackupSettingsStore.gioDaLuu().isEmpty());
    assertTrue(BackupSettingsStore.chuKyDaLuu().isEmpty());
  }

  @Test
  void tepCoGiaTriKhongHopLeTraVeRong() throws IOException {
    Path file = tempDir.resolve("backup-hong.properties");
    Files.writeString(file,
        "DTAQ_BACKUP_TIME=99:99\nDTAQ_BACKUP_INTERVAL_HOURS=abc\n",
        StandardCharsets.UTF_8);
    BackupSettingsStore.setFileForTest(file);

    assertTrue(BackupSettingsStore.gioDaLuu().isEmpty());
    assertTrue(BackupSettingsStore.chuKyDaLuu().isEmpty());
  }

  @Test
  void chuKyNgoaiKhoangTrongTepTraVeRong() throws IOException {
    Path file = tempDir.resolve("backup-chu-ky.properties");
    Files.writeString(file,
        "DTAQ_BACKUP_TIME=06:30\nDTAQ_BACKUP_INTERVAL_HOURS=0\n",
        StandardCharsets.UTF_8);
    BackupSettingsStore.setFileForTest(file);

    assertEquals(Optional.of(LocalTime.of(6, 30)), BackupSettingsStore.gioDaLuu());
    assertTrue(BackupSettingsStore.chuKyDaLuu().isEmpty());
  }

  @Test
  void tepHongCuPhapTraVeRong() throws IOException {
    Path file = tempDir.resolve("backup-hong-cu-phap.properties");
    Files.writeString(file, "DTAQ_BACKUP_TIME=\\uZZZZ\n", StandardCharsets.UTF_8);
    BackupSettingsStore.setFileForTest(file);

    assertTrue(BackupSettingsStore.gioDaLuu().isEmpty());
    assertTrue(BackupSettingsStore.chuKyDaLuu().isEmpty());
  }

  @Test
  void macDinhLaBackupProperties() {
    BackupSettingsStore.setFileForTest(null);

    assertEquals(Path.of("backup.properties"), BackupSettingsStore.file());
  }

  private static Properties doc(Path path) throws IOException {
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }
}
