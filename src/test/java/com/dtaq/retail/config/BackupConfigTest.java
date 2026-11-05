package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử thứ tự ưu tiên và giá trị mặc định của {@link BackupConfig}. */
class BackupConfigTest {

  private static final List<String> KHOA = List.of(
      BackupConfig.KEY_THU_MUC,
      BackupConfig.KEY_SO_NGAY_GIU,
      BackupConfig.KEY_GIO_CHAY,
      BackupConfig.KEY_CHU_KY_GIO,
      BackupConfig.KEY_BAT_TAT,
      BackupConfig.KEY_THU_MUC_PHU,
      BackupConfig.KEY_PG_BIN_DIR,
      BackupConfig.KEY_PG_DUMP,
      BackupConfig.KEY_PG_RESTORE,
      BackupConfig.KEY_PG_CREATEDB);

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    xoaSystemProperty();
    BackupConfig.setBackupPropertiesFileForTest(tempDir.resolve("absent-backup.properties"));
  }

  @AfterEach
  void tearDown() {
    xoaSystemProperty();
    BackupConfig.setBackupPropertiesFileForTest(null);
    BackupConfig.setPropertiesFileForTest(null);
  }

  @Test
  void defaultsWhenNothingConfigured() {
    BackupConfig.setPropertiesFileForTest(tempDir.resolve("absent.properties"));
    boQuaKhiCoBienMoiTruong();
    assertEquals(Path.of("./backups"), BackupConfig.thuMucBackup());
    assertEquals(7, BackupConfig.soNgayGiu());
    assertEquals(LocalTime.of(0, 0), BackupConfig.gioChay());
    assertEquals(24, BackupConfig.chuKyGio());
    assertTrue(BackupConfig.batTat());
    assertTrue(BackupConfig.thuMucPhu().isEmpty());
    assertEquals(tenCongCu("pg_dump"), BackupConfig.pgDump().getFileName().toString());
    assertEquals(tenCongCu("pg_restore"), BackupConfig.pgRestore().getFileName().toString());
    assertEquals(tenCongCu("createdb"), BackupConfig.createdb().getFileName().toString());
  }

  @Test
  void fileValuesOverrideDefaults() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup.properties", String.join("\n",
        "DTAQ_BACKUP_DIR=/var/lib/dtaq/backups",
        "DTAQ_BACKUP_KEEP_DAYS=3",
        "DTAQ_BACKUP_TIME=02:30",
        "DTAQ_BACKUP_INTERVAL_HOURS=12",
        "DTAQ_BACKUP_ENABLED=false",
        "DTAQ_BACKUP_SECONDARY_DIR=/mnt/phu",
        "")));
    boQuaKhiCoBienMoiTruong();
    assertEquals(Path.of("/var/lib/dtaq/backups"), BackupConfig.thuMucBackup());
    assertEquals(3, BackupConfig.soNgayGiu());
    assertEquals(LocalTime.of(2, 30), BackupConfig.gioChay());
    assertEquals(12, BackupConfig.chuKyGio());
    assertFalse(BackupConfig.batTat());
    assertEquals(Path.of("/mnt/phu"), BackupConfig.thuMucPhu().orElseThrow());
  }

  @Test
  void systemPropertyOverridesFile() throws IOException {
    BackupConfig.setPropertiesFileForTest(
        writeProperties("backup-sys.properties", "DTAQ_BACKUP_KEEP_DAYS=3"));
    boQuaKhiCoBienMoiTruong();
    System.setProperty(BackupConfig.KEY_SO_NGAY_GIU, "9");
    assertEquals(9, BackupConfig.soNgayGiu());
  }

  @Test
  void invalidNumbersAndTimeFallBackToDefaults() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup-bad.properties", String.join("\n",
        "DTAQ_BACKUP_KEEP_DAYS=abc",
        "DTAQ_BACKUP_TIME=25:99",
        "DTAQ_BACKUP_INTERVAL_HOURS=0",
        "DTAQ_BACKUP_ENABLED=maybe",
        "")));
    boQuaKhiCoBienMoiTruong();
    assertEquals(7, BackupConfig.soNgayGiu());
    assertEquals(LocalTime.of(0, 0), BackupConfig.gioChay());
    assertEquals(24, BackupConfig.chuKyGio());
    assertTrue(BackupConfig.batTat());
  }

  @Test
  void binDirWinsOverConfiguredToolName() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup-bin.properties", String.join("\n",
        "DTAQ_PG_BIN_DIR=/opt/postgresql/bin",
        "DTAQ_PG_DUMP=custom_pg_dump",
        "")));
    boQuaKhiCoBienMoiTruong();
    Path pgDump = BackupConfig.pgDump();
    assertEquals(Path.of("/opt/postgresql/bin"), pgDump.getParent());
    assertEquals(tenCongCu("pg_dump"), pgDump.getFileName().toString());
  }

  @Test
  void configuredToolPathUsedWhenNoBinDir() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup-tool.properties",
        "DTAQ_PG_RESTORE=/usr/local/bin/my_pg_restore"));
    boQuaKhiCoBienMoiTruong();
    assertEquals(Path.of("/usr/local/bin/my_pg_restore"), BackupConfig.pgRestore());
  }

  @Test
  void backupFileFromUiOverridesDbProperties() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup-db.properties", String.join("\n",
        "DTAQ_BACKUP_TIME=23:00",
        "DTAQ_BACKUP_INTERVAL_HOURS=24",
        "")));
    BackupConfig.setBackupPropertiesFileForTest(writeProperties("backup-ui.properties", String.join("\n",
        "DTAQ_BACKUP_TIME=06:30",
        "DTAQ_BACKUP_INTERVAL_HOURS=6",
        "")));
    boQuaKhiCoBienMoiTruong();

    assertEquals(LocalTime.of(6, 30), BackupConfig.gioChay());
    assertEquals(6, BackupConfig.chuKyGio());
  }

  @Test
  void uiFileOnlyOverridesSchedulingKeys() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup-db-foreign.properties", String.join("\n",
        "DTAQ_BACKUP_DIR=db-dir",
        "DTAQ_BACKUP_TIME=23:00",
        "DTAQ_BACKUP_INTERVAL_HOURS=24",
        "")));
    BackupConfig.setBackupPropertiesFileForTest(
        writeProperties("backup-ui-foreign.properties", String.join("\n",
            "DTAQ_BACKUP_DIR=ui-dir",
            "DTAQ_BACKUP_TIME=06:30",
            "DTAQ_BACKUP_INTERVAL_HOURS=6",
            "")));
    boQuaKhiCoBienMoiTruong();

    assertEquals(Path.of("db-dir"), BackupConfig.thuMucBackup(),
        "khóa lạ trong backup.properties không được ghi đè db.properties");
    assertEquals(LocalTime.of(6, 30), BackupConfig.gioChay());
    assertEquals(6, BackupConfig.chuKyGio());
  }

  @Test
  void systemPropertyWinsOverBothFiles() throws IOException {
    BackupConfig.setPropertiesFileForTest(writeProperties("backup-db-sys.properties", String.join("\n",
        "DTAQ_BACKUP_TIME=23:00",
        "DTAQ_BACKUP_INTERVAL_HOURS=24",
        "")));
    BackupConfig.setBackupPropertiesFileForTest(writeProperties("backup-ui-sys.properties", String.join("\n",
        "DTAQ_BACKUP_TIME=06:30",
        "DTAQ_BACKUP_INTERVAL_HOURS=6",
        "")));
    boQuaKhiCoBienMoiTruong();
    System.setProperty(BackupConfig.KEY_GIO_CHAY, "08:15");

    assertEquals(LocalTime.of(8, 15), BackupConfig.gioChay());
  }

  @Test
  void reloadSeesUpdatedBackupFile() throws IOException {
    Path file = writeProperties("backup-ui-reload.properties", String.join("\n",
        "DTAQ_BACKUP_TIME=06:30",
        "DTAQ_BACKUP_INTERVAL_HOURS=6",
        ""));
    BackupConfig.setBackupPropertiesFileForTest(file);
    boQuaKhiCoBienMoiTruong();
    assertEquals(LocalTime.of(6, 30), BackupConfig.gioChay());
    assertEquals(6, BackupConfig.chuKyGio());

    Files.writeString(file, String.join("\n",
        "DTAQ_BACKUP_TIME=21:45",
        "DTAQ_BACKUP_INTERVAL_HOURS=12",
        ""));
    assertEquals(LocalTime.of(6, 30), BackupConfig.gioChay());

    BackupConfig.reload();

    assertEquals(LocalTime.of(21, 45), BackupConfig.gioChay());
    assertEquals(12, BackupConfig.chuKyGio());
  }

  @Test
  void biGhiDeBoiMoiTruongTheoSystemProperty() {
    boQuaKhiCoBienMoiTruong();
    assertFalse(BackupConfig.biGhiDeBoiMoiTruong());

    System.setProperty(BackupConfig.KEY_CHU_KY_GIO, "6");
    assertTrue(BackupConfig.biGhiDeBoiMoiTruong());
    System.clearProperty(BackupConfig.KEY_CHU_KY_GIO);
    assertFalse(BackupConfig.biGhiDeBoiMoiTruong());

    System.setProperty(BackupConfig.KEY_GIO_CHAY, "08:15");
    assertTrue(BackupConfig.biGhiDeBoiMoiTruong());
    System.clearProperty(BackupConfig.KEY_GIO_CHAY);
    assertFalse(BackupConfig.biGhiDeBoiMoiTruong());
  }

  private void boQuaKhiCoBienMoiTruong() {
    for (String khoa : KHOA) {
      Assumptions.assumeTrue(System.getenv(khoa) == null,
          "Bỏ qua vì biến môi trường " + khoa + " đang được đặt");
    }
  }

  private void xoaSystemProperty() {
    for (String khoa : KHOA) {
      System.clearProperty(khoa);
    }
  }

  private static String tenCongCu(String ten) {
    boolean windows = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
        .contains("win");
    return windows ? ten + ".exe" : ten;
  }

  private Path writeProperties(String fileName, String content) throws IOException {
    Path file = tempDir.resolve(fileName);
    Files.writeString(file, content);
    return file;
  }
}
