package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử thứ tự ưu tiên, giá trị mặc định và kiểm tra hợp lệ của {@link HealthConfig}. */
class HealthConfigTest {

  private static final List<String> KHOA = List.of(
      HealthConfig.KEY_BAT_TAT,
      HealthConfig.KEY_CHU_KY_PHUT,
      HealthConfig.KEY_TEP_TRANG_THAI,
      HealthConfig.KEY_NGUONG_GIAM,
      HealthConfig.KEY_BASELINE_TOI_THIEU,
      HealthConfig.KEY_SO_CHU_KY,
      HealthConfig.KEY_BANG_COT_LOI,
      HealthConfig.KEY_AUTO_RESTORE_BAT_TAT,
      HealthConfig.KEY_AUTO_RESTORE_TOI_DA,
      HealthConfig.KEY_SO_BAN_DU_PHONG,
      HealthConfig.KEY_NHAC_LAI_PHUT,
      HealthConfig.KEY_TIMEOUT_GIAY);

  private static final List<String> BANG_COT_LOI_MAC_DINH = List.of(
      "NhanVien", "KhachHang", "DanhMucSanPham", "NhaCungCap", "DongSanPham", "SanPham",
      "KhuyenMai", "HoaDon", "ChiTietHoaDon", "PhieuTra", "ChiTietPhieuTra", "PhieuDoi",
      "ChiTietPhieuDoi", "SanPhamLoi");

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    xoaSystemProperty();
  }

  @AfterEach
  void tearDown() {
    xoaSystemProperty();
    HealthConfig.setPropertiesFileForTest(null);
  }

  @Test
  void defaultsWhenNothingConfigured() {
    HealthConfig.setPropertiesFileForTest(tempDir.resolve("absent.properties"));
    boQuaKhiCoBienMoiTruong();

    assertTrue(HealthConfig.batTat());
    assertEquals(5, HealthConfig.chuKyPhut());
    assertEquals(Path.of("./backups/health_state.json"), HealthConfig.tepTrangThai());
    assertEquals(0.5, HealthConfig.nguongGiam());
    assertEquals(50, HealthConfig.baselineToiThieu());
    assertEquals(2, HealthConfig.soChuKyXacNhan());
    assertEquals(BANG_COT_LOI_MAC_DINH, HealthConfig.bangCotLoi());
    assertTrue(HealthConfig.autoRestoreBatTat());
    assertEquals(3, HealthConfig.autoRestoreToiDaMoiGio());
    assertEquals(3, HealthConfig.soBanDuPhongToiDa());
    assertEquals(30, HealthConfig.nhacLaiPhut());
    assertEquals(10, HealthConfig.timeoutGiay());
  }

  @Test
  void fileValuesOverrideDefaults() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health.properties", String.join("\n",
        "DTAQ_HEALTH_ENABLED=false",
        "DTAQ_HEALTH_INTERVAL_MINUTES=15",
        "DTAQ_HEALTH_STATE_FILE=/var/lib/dtaq/health_state.json",
        "DTAQ_HEALTH_DROP_THRESHOLD=0.25",
        "DTAQ_HEALTH_DROP_MIN_BASELINE=100",
        "DTAQ_HEALTH_SUSPICIOUS_CYCLES=4",
        "DTAQ_HEALTH_CORE_TABLES=HoaDon, SanPham",
        "DTAQ_HEALTH_AUTO_RESTORE_ENABLED=false",
        "DTAQ_HEALTH_AUTO_RESTORE_MAX_PER_HOUR=1",
        "DTAQ_HEALTH_FALLBACK_MAX_BACKUPS=5",
        "DTAQ_HEALTH_REMIND_MINUTES=10",
        "DTAQ_HEALTH_TIMEOUT_SECONDS=20",
        "")));
    boQuaKhiCoBienMoiTruong();

    assertFalse(HealthConfig.batTat());
    assertEquals(15, HealthConfig.chuKyPhut());
    assertEquals(Path.of("/var/lib/dtaq/health_state.json"), HealthConfig.tepTrangThai());
    assertEquals(0.25, HealthConfig.nguongGiam());
    assertEquals(100, HealthConfig.baselineToiThieu());
    assertEquals(4, HealthConfig.soChuKyXacNhan());
    assertEquals(List.of("HoaDon", "SanPham"), HealthConfig.bangCotLoi());
    assertFalse(HealthConfig.autoRestoreBatTat());
    assertEquals(1, HealthConfig.autoRestoreToiDaMoiGio());
    assertEquals(5, HealthConfig.soBanDuPhongToiDa());
    assertEquals(10, HealthConfig.nhacLaiPhut());
    assertEquals(20, HealthConfig.timeoutGiay());
  }

  @Test
  void systemPropertyOverridesFile() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health-sys.properties", String.join("\n",
        "DTAQ_HEALTH_INTERVAL_MINUTES=7",
        "DTAQ_HEALTH_DROP_THRESHOLD=0.3",
        "")));
    boQuaKhiCoBienMoiTruong();
    System.setProperty(HealthConfig.KEY_CHU_KY_PHUT, "9");
    System.setProperty(HealthConfig.KEY_NGUONG_GIAM, "0.9");

    assertEquals(9, HealthConfig.chuKyPhut());
    assertEquals(0.9, HealthConfig.nguongGiam());
  }

  @Test
  void invalidNumbersAndBooleansFallBackToDefaults() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health-bad.properties", String.join("\n",
        "DTAQ_HEALTH_ENABLED=maybe",
        "DTAQ_HEALTH_INTERVAL_MINUTES=0",
        "DTAQ_HEALTH_DROP_THRESHOLD=1.5",
        "DTAQ_HEALTH_DROP_MIN_BASELINE=abc",
        "DTAQ_HEALTH_SUSPICIOUS_CYCLES=-2",
        "DTAQ_HEALTH_AUTO_RESTORE_ENABLED=khong",
        "DTAQ_HEALTH_AUTO_RESTORE_MAX_PER_HOUR=0",
        "DTAQ_HEALTH_FALLBACK_MAX_BACKUPS=-1",
        "DTAQ_HEALTH_REMIND_MINUTES=abc",
        "DTAQ_HEALTH_TIMEOUT_SECONDS=0",
        "")));
    boQuaKhiCoBienMoiTruong();

    assertTrue(HealthConfig.batTat());
    assertEquals(5, HealthConfig.chuKyPhut());
    assertEquals(0.5, HealthConfig.nguongGiam());
    assertEquals(50, HealthConfig.baselineToiThieu());
    assertEquals(2, HealthConfig.soChuKyXacNhan());
    assertTrue(HealthConfig.autoRestoreBatTat());
    assertEquals(3, HealthConfig.autoRestoreToiDaMoiGio());
    assertEquals(3, HealthConfig.soBanDuPhongToiDa());
    assertEquals(30, HealthConfig.nhacLaiPhut());
    assertEquals(10, HealthConfig.timeoutGiay());
  }

  @Test
  void thresholdOutsideOpenUnitIntervalFallsBack() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health-threshold.properties", String.join("\n",
        "DTAQ_HEALTH_DROP_THRESHOLD=0",
        "")));
    boQuaKhiCoBienMoiTruong();
    assertEquals(0.5, HealthConfig.nguongGiam());

    HealthConfig.setPropertiesFileForTest(writeProperties("health-threshold-1.properties", String.join("\n",
        "DTAQ_HEALTH_DROP_THRESHOLD=1",
        "")));
    assertEquals(0.5, HealthConfig.nguongGiam());

    HealthConfig.setPropertiesFileForTest(writeProperties("health-threshold-nan.properties", String.join("\n",
        "DTAQ_HEALTH_DROP_THRESHOLD=NaN",
        "")));
    assertEquals(0.5, HealthConfig.nguongGiam());
  }

  @Test
  void invalidTableNamesAreDropped() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health-tables.properties", String.join("\n",
        "DTAQ_HEALTH_CORE_TABLES=1bad,has-dash,ok_underscore,Good1,Bang2,,Good1",
        "")));
    boQuaKhiCoBienMoiTruong();

    assertEquals(List.of("Good1", "Bang2"), HealthConfig.bangCotLoi());
  }

  @Test
  void allInvalidTableNamesFallBackToDefaultList() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health-tables-bad.properties", String.join("\n",
        "DTAQ_HEALTH_CORE_TABLES=1bad,has-dash,,",
        "")));
    boQuaKhiCoBienMoiTruong();

    assertEquals(BANG_COT_LOI_MAC_DINH, HealthConfig.bangCotLoi());
  }

  @Test
  void blankValuesUseDefaults() throws IOException {
    HealthConfig.setPropertiesFileForTest(writeProperties("health-blank.properties", String.join("\n",
        "DTAQ_HEALTH_INTERVAL_MINUTES=   ",
        "DTAQ_HEALTH_CORE_TABLES=",
        "DTAQ_HEALTH_STATE_FILE=  ",
        "")));
    boQuaKhiCoBienMoiTruong();

    assertEquals(5, HealthConfig.chuKyPhut());
    assertEquals(BANG_COT_LOI_MAC_DINH, HealthConfig.bangCotLoi());
    assertEquals(Path.of("./backups/health_state.json"), HealthConfig.tepTrangThai());
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

  private Path writeProperties(String fileName, String content) throws IOException {
    Path file = tempDir.resolve(fileName);
    Files.writeString(file, content);
    return file;
  }
}
