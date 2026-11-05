package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthIncident.TrangThai;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử chu kỳ và việc bỏ qua khi hệ thống bận của {@link HealthMonitor}.
 *
 * <p>Không bài kiểm thử nào khởi động luồng lập lịch thật hay kết nối cơ sở dữ liệu; bộ kiểm tra và
 * bộ điều phối đều là bản giả đếm số lần gọi.
 */
class HealthMonitorTest {

  @TempDir
  Path tempDir;

  private DataHealthCheckerGia checker;
  private CoordinatorGia coordinator;
  private HealthMonitor monitor;

  @BeforeEach
  void setUp() {
    HealthStateStore store = new HealthStateStore(tempDir.resolve("health_state.json"));
    BackupStore kho = new BackupStore(tempDir.resolve("backups"), null);
    PgCommandExecutor pg = new PgCommandExecutor(Path.of("pg_dump"), Path.of("pg_restore"),
        Path.of("createdb"), "localhost", 5432, "test", "test", "");
    LichSuGia lichSu = new LichSuGia();
    BackupService backupService = new BackupService(new NguonDuLieuGia(), kho, pg, lichSu);
    RestoreService restoreService = new RestoreService(new NguonDuLieuGia(), kho, pg, lichSu,
        backupService);

    checker = new DataHealthCheckerGia(store);
    coordinator = new CoordinatorGia(store, new IncidentDaoGia(), backupService, restoreService);
    monitor = new HealthMonitor(checker, coordinator);
  }

  @AfterEach
  void tearDown() {
    monitor.close();
    MaintenanceLock.release();
    AppState.set(AppState.State.NORMAL);
    System.clearProperty(HealthConfig.KEY_BAT_TAT);
    HealthConfig.setPropertiesFileForTest(null);
  }

  @Test
  @DisplayName("Đổi chu kỳ phút sang mili giây")
  void doiChuKyPhutSangMiliGiay() {
    assertEquals(300_000L, HealthMonitor.chuKyMillis(5));
    assertEquals(60_000L, HealthMonitor.chuKyMillis(1));
    assertEquals(0L, HealthMonitor.chuKyMillis(0));
  }

  @Test
  @DisplayName("Bỏ qua chu kỳ khi khóa bảo trì đang bận")
  void boQuaKhiKhoaBaoTriDangBan() throws InterruptedException {
    assertTrue(MaintenanceLock.tryAcquire(Duration.ofSeconds(1)));
    try {
      monitor.kiemTraNgay();
    } finally {
      MaintenanceLock.release();
    }

    assertEquals(0, coordinator.soLan, "không được xử lý báo cáo khi hệ thống đang bận");
    assertEquals(0, checker.soLan, "không được kiểm tra dữ liệu khi hệ thống đang bận");
    assertFalse(MaintenanceLock.isBusy(), "không được giữ khóa sau chu kỳ");
  }

  @Test
  @DisplayName("Chạy đúng một chu kỳ khi khóa bảo trì rảnh")
  void chayMotChuKyKhiKhoaRanh() {
    monitor.kiemTraNgay();

    assertEquals(1, checker.soLan);
    assertEquals(1, coordinator.soLan);
    assertFalse(MaintenanceLock.isBusy(), "phải nhả khóa sau chu kỳ");
  }

  @Test
  @DisplayName("DTAQ_HEALTH_ENABLED=false chặn cả kiểm tra ngay lúc khởi động")
  void tatGiamSatChanKiemTraNgay() throws IOException {
    Path properties = tempDir.resolve("db.properties");
    Files.writeString(properties, HealthConfig.KEY_BAT_TAT + "=false\n");
    HealthConfig.setPropertiesFileForTest(properties);

    monitor.kiemTraNgay();

    assertEquals(0, checker.soLan, "không được kiểm tra dữ liệu khi giám sát bị tắt");
    assertEquals(0, coordinator.soLan, "không được xử lý báo cáo khi giám sát bị tắt");
    assertFalse(MaintenanceLock.isBusy(), "không được giữ khóa khi bị tắt");
  }

  @Test
  @DisplayName("Đóng nhiều lần không ném ngoại lệ")
  void dongNhieuLanKhongNem() {
    monitor.close();
    monitor.close();
    assertTrue(true);
  }

  /** Bộ kiểm tra giả: trả báo cáo HEALTHY và đếm số lần gọi. */
  private static final class DataHealthCheckerGia extends DataHealthChecker {

    private int soLan;

    DataHealthCheckerGia(HealthStateStore store) {
      super(new NguonDuLieuGia(), store);
    }

    @Override
    public HealthReport kiemTra() {
      soLan++;
      return new HealthReport(HealthStatus.HEALTHY, Map.of(), Map.of(),
          Map.of(), List.of(), "giả lập", null, Instant.now());
    }
  }

  /** Bộ điều phối giả: chỉ đếm số lần được gọi. */
  private static final class CoordinatorGia extends RecoveryCoordinator {

    private int soLan;

    CoordinatorGia(HealthStateStore store, HealthIncidentDao dao, BackupService backupService,
        RestoreService restoreService) {
      super(store, dao, backupService, restoreService);
    }

    @Override
    public void xuLy(HealthReport report) {
      soLan++;
    }
  }

  /** DAO sự cố không dùng trong các bài kiểm thử này. */
  private static final class IncidentDaoGia implements HealthIncidentDao {

    @Override
    public long insert(HealthIncident incident) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public Optional<HealthIncident> findById(long id) {
      return Optional.empty();
    }

    @Override
    public Optional<HealthIncident> findMoiNhatDangCho() {
      return Optional.empty();
    }

    @Override
    public List<HealthIncident> findAll() {
      return List.of();
    }

    @Override
    public void capNhatKetThuc(long id, TrangThai trangThai, Instant resolvedAt,
        String resolvedBy, String backupUsed) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }
  }

  /** DAO lịch sử không dùng trong các bài kiểm thử này. */
  private static final class LichSuGia implements LichSuSaoLuuDao {

    @Override
    public void insert(BackupRecord banGhi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public List<BackupRecord> findAll() {
      return List.of();
    }

    @Override
    public Optional<BackupRecord> findMoiNhatThanhCong() {
      return Optional.empty();
    }

    @Override
    public boolean existsByFilePath(String filePath) {
      return false;
    }
  }

  /** Nguồn dữ liệu giả: không bài kiểm thử nào mở kết nối qua nguồn này. */
  private static final class NguonDuLieuGia implements DataSource {

    @Override
    public Connection getConnection() {
      throw new UnsupportedOperationException("Không dùng cơ sở dữ liệu trong kiểm thử");
    }

    @Override
    public Connection getConnection(String username, String password) {
      throw new UnsupportedOperationException("Không dùng cơ sở dữ liệu trong kiểm thử");
    }

    @Override
    public PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public void setLoginTimeout(int seconds) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException("Không dùng trong kiểm thử");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException("Không dùng trong kiểm thử");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
