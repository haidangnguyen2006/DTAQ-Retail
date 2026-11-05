package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.DbMigration;
import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import com.dtaq.retail.service.HealthStateStore.HealthState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử {@link DataHealthChecker}: phân loại SQLState thuần và các chu kỳ kiểm tra thật.
 *
 * <p>Các bài cần cơ sở dữ liệu đều được bỏ qua qua {@link Assumptions} khi PostgreSQL không sẵn
 * sàng. Bài kiểm tra chỉ đọc dữ liệu, không chèn, sửa hay xóa dòng nào.
 */
class DataHealthCheckerTest {

  private static DataSource dataSource;

  @TempDir
  Path tempDir;

  @BeforeAll
  static void requireDatabase() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DbMigration.migrate(ConnectDB.getDataSource());
    dataSource = ConnectDB.getDataSource();
  }

  @AfterEach
  void tearDown() {
    HealthConfig.setPropertiesFileForTest(null);
  }

  @Test
  @DisplayName("3D000 là CRITICAL; 08/28 và mã lạ là UNAVAILABLE")
  void phanLoaiSqlStateTheoNhomLoi() {
    assertEquals(HealthStatus.CRITICAL, DataHealthChecker.phanLoaiSqlState("3D000"));
    assertEquals(HealthStatus.UNAVAILABLE, DataHealthChecker.phanLoaiSqlState("08006"));
    assertEquals(HealthStatus.UNAVAILABLE, DataHealthChecker.phanLoaiSqlState("28P01"));
    assertEquals(HealthStatus.UNAVAILABLE, DataHealthChecker.phanLoaiSqlState(null));
    assertEquals(HealthStatus.UNAVAILABLE, DataHealthChecker.phanLoaiSqlState("  "));
    assertEquals(HealthStatus.UNAVAILABLE, DataHealthChecker.phanLoaiSqlState("XX000"));
  }

  @Test
  @DisplayName("timSqlState đi theo nguyên nhân bọc ngoài")
  void timSqlStateDiTheoCause() {
    SQLException goc = new SQLException("mất kết nối", "08006");
    RuntimeException boc = new RuntimeException("lỗi bể kết nối", goc);

    assertEquals("08006", DataHealthChecker.timSqlState(boc));
    assertEquals("08006", DataHealthChecker.timSqlState(goc));
    assertNull(DataHealthChecker.timSqlState(new IllegalStateException("không có SQLState")));
    assertNull(DataHealthChecker.timSqlState(null));
  }

  @Test
  @DisplayName("timSqlState đi theo getNextException của SQLException")
  void timSqlStateDiTheoNextException() {
    SQLException dau = new SQLException("lỗi đầu tiên");
    SQLException tiep = new SQLException("sai mật khẩu", "28P01");
    dau.setNextException(tiep);

    assertEquals("28P01", DataHealthChecker.timSqlState(dau));
    assertEquals("28P01", DataHealthChecker.timSqlState(new RuntimeException(dau)));
  }

  @Test
  @DisplayName("HealthReport chuẩn hóa null, sao chép phòng thủ và nhận diện bất thường")
  void healthReportChuanHoaGiaTriNull() {
    Instant thoiDiem = Instant.parse("2026-11-05T00:00:00Z");
    HealthReport rong = new HealthReport(HealthStatus.HEALTHY, null, null, null, null,
        null, null, thoiDiem);

    assertTrue(rong.soDong().isEmpty());
    assertTrue(rong.baseline().isEmpty());
    assertTrue(rong.tyLeGiam().isEmpty());
    assertTrue(rong.bangThieu().isEmpty());
    assertFalse(rong.coBatThuong());
    assertThrows(UnsupportedOperationException.class, () -> rong.soDong().put("HoaDon", 1L));

    Map<String, Long> soDong = new LinkedHashMap<>();
    soDong.put("HoaDon", 3L);
    HealthReport baoDong = new HealthReport(HealthStatus.SUSPICIOUS, soDong, null, null, null,
        "giảm", null, thoiDiem);
    soDong.put("HoaDon", 99L);

    assertEquals(3L, baoDong.soDong().get("HoaDon"), "phải sao chép phòng thủ");
    assertTrue(baoDong.coBatThuong());
    assertTrue(new HealthReport(HealthStatus.CRITICAL, null, null, null, null, null, "3D000",
        thoiDiem).coBatThuong());
    assertFalse(new HealthReport(HealthStatus.UNAVAILABLE, null, null, null, null, null,
        "08006", thoiDiem).coBatThuong());
    assertThrows(NullPointerException.class,
        () -> new HealthReport(null, null, null, null, null, null, null, thoiDiem));
    assertThrows(NullPointerException.class,
        () -> new HealthReport(HealthStatus.HEALTHY, null, null, null, null, null, null, null));
  }

  @Test
  @DisplayName("Constructor Supplier lấy lại nguồn dữ liệu ở mỗi chu kỳ")
  void kiemTraQuaSupplierNguonDuLieu() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");

    HealthStateStore store = new HealthStateStore(tempDir.resolve("health_state.json"));
    AtomicInteger soLanGoi = new AtomicInteger();
    DataHealthChecker checker = new DataHealthChecker(() -> {
      soLanGoi.incrementAndGet();
      return dataSource;
    }, store);

    HealthReport dau = checker.kiemTra();
    assertEquals(1, soLanGoi.get(), "chu kỳ đầu phải gọi nhà cung cấp một lần");
    assertNotEquals(HealthStatus.UNAVAILABLE, dau.trangThai(),
        "kết nối được thì không thể UNAVAILABLE: " + dau.chiTiet());

    checker.kiemTra();
    assertEquals(2, soLanGoi.get(), "chu kỳ sau phải lấy lại nguồn dữ liệu hiện tại");
  }

  @Test
  @DisplayName("DB thật với baseline rỗng trả HEALTHY (hoặc CRITICAL nếu schema rỗng)")
  void kiemTraVoiBaselineRong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    Assumptions.assumeTrue(HealthConfig.bangCotLoi().size() == 14,
        "cấu hình bảng cốt lõi đã bị thay đổi");

    HealthStateStore store = new HealthStateStore(tempDir.resolve("health_state.json"));
    DataHealthChecker checker = new DataHealthChecker(dataSource, store);

    HealthReport report = checker.kiemTra();

    assertNotEquals(HealthStatus.UNAVAILABLE, report.trangThai(),
        "kết nối được thì không thể UNAVAILABLE: " + report.chiTiet());
    assertNull(report.sqlState(), "kiểm tra thành công không có SQLState");
    assertEquals(14, report.soDong().size());
    assertEquals(HealthConfig.bangCotLoi(), List.copyOf(report.soDong().keySet()));
    assertTrue(report.bangThieu().isEmpty());
    assertTrue(report.tyLeGiam().isEmpty(), "baseline rỗng thì không có suy giảm");
    assertTrue(report.baseline().isEmpty());
    assertNotNull(report.thoiDiem());
    if (report.trangThai() == HealthStatus.HEALTHY) {
      assertFalse(report.coBatThuong());
    }
  }

  @Test
  @DisplayName("DB thật báo CRITICAL và bangThieu khi cấu hình bảng không tồn tại")
  void kiemTraBaoThieuBangKhiCauHinhBangLa() throws IOException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    Assumptions.assumeTrue(System.getProperty(HealthConfig.KEY_BANG_COT_LOI) == null
            && System.getenv(HealthConfig.KEY_BANG_COT_LOI) == null,
        "biến cấu hình bảng cốt lõi đang được đặt");

    Path properties = tempDir.resolve("db.properties");
    Files.writeString(properties,
        HealthConfig.KEY_BANG_COT_LOI + "=HoaDon,KhongTonTai\n");
    HealthConfig.setPropertiesFileForTest(properties);

    HealthStateStore store = new HealthStateStore(tempDir.resolve("health_state.json"));
    DataHealthChecker checker = new DataHealthChecker(dataSource, store);

    HealthReport report = checker.kiemTra();

    assertEquals(HealthStatus.CRITICAL, report.trangThai());
    assertTrue(report.bangThieu().contains("KhongTonTai"), report.bangThieu().toString());
    assertFalse(report.bangThieu().contains("HoaDon"));
    assertTrue(report.chiTiet().contains("KhongTonTai"), report.chiTiet());
    assertNull(report.sqlState());
    assertTrue(report.coBatThuong());
  }

  @Test
  @DisplayName("DB thật trả SUSPICIOUS khi baseline cao hơn dữ liệu hiện tại")
  void kiemTraSuspiciousKhiBaselineCao() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    Assumptions.assumeTrue(coDuLieu(), "cần cơ sở dữ liệu có dữ liệu mẫu");

    Map<String, Long> baseline = new LinkedHashMap<>();
    baseline.put("NhanVien", 1_000_000L);
    HealthStateStore store = new HealthStateStore(tempDir.resolve("health_state.json"));
    store.ghi(new HealthState(baseline, Map.of(), null, null, List.of(), null, null));

    DataHealthChecker checker = new DataHealthChecker(dataSource, store);
    HealthReport report = checker.kiemTra();

    assertEquals(HealthStatus.SUSPICIOUS, report.trangThai(), report.chiTiet());
    assertTrue(report.tyLeGiam().containsKey("NhanVien"));
    assertTrue(report.tyLeGiam().get("NhanVien") >= HealthConfig.nguongGiam());
    assertTrue(report.chiTiet().contains("NhanVien"), report.chiTiet());
    assertEquals(baseline, report.baseline());
  }

  private static boolean coDuLieu() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      long tong = 0L;
      for (String bang : HealthConfig.bangCotLoi()) {
        try (ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM \"" + bang + '"')) {
          if (resultSet.next()) {
            tong += resultSet.getLong(1);
          }
        }
      }
      return tong > 0L;
    }
  }
}
