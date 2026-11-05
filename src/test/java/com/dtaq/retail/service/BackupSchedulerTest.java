package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử quyết định chạy bù và cổng trạng thái của {@link BackupScheduler}.
 *
 * <p>Chỉ kiểm tra hàm thuần {@code canChayBu} và phương thức {@code chay} với dịch vụ sao lưu giả;
 * không khởi động luồng lập lịch và không kết nối cơ sở dữ liệu.
 */
class BackupSchedulerTest {

  private static final Instant BAY_GIO = Instant.parse("2026-11-05T10:00:00Z");

  private static final String SHA256_GIA =
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

  @TempDir
  Path tempDir;

  @AfterEach
  void datLaiTrangThai() {
    MaintenanceLock.release();
    AppState.set(AppState.State.NORMAL);
  }

  @Test
  @DisplayName("Chưa có bản sao lưu nào thì phải chạy bù")
  void chuaCoBanSaoLuuThiChayBu() {
    assertTrue(BackupScheduler.canChayBu(Optional.empty(), BAY_GIO, 24));
  }

  @Test
  @DisplayName("Bản gần nhất cũ hơn 25 giờ so với chu kỳ 24 giờ thì chạy bù")
  void banCuHonChuKyThiChayBu() {
    Instant lanCuoi = BAY_GIO.minus(Duration.ofHours(25));
    assertTrue(BackupScheduler.canChayBu(Optional.of(lanCuoi), BAY_GIO, 24));
  }

  @Test
  @DisplayName("Bản gần nhất mới hơn 23 giờ so với chu kỳ 24 giờ thì không chạy bù")
  void banMoiHonChuKyThiKhongChayBu() {
    Instant lanCuoi = BAY_GIO.minus(Duration.ofHours(23));
    assertFalse(BackupScheduler.canChayBu(Optional.of(lanCuoi), BAY_GIO, 24));
  }

  @Test
  @DisplayName("Chu kỳ 0 giờ thì bản cũ hơn hiện tại là chạy bù")
  void chuKyKhongGioVaBanCuHonThiChayBu() {
    Instant lanCuoi = BAY_GIO.minus(Duration.ofSeconds(1));
    assertTrue(BackupScheduler.canChayBu(Optional.of(lanCuoi), BAY_GIO, 0));
  }

  @Test
  @DisplayName("Chu kỳ 0 giờ nhưng bản gần nhất trùng hiện tại thì không chạy bù")
  void chuKyKhongGioVaBanTrungHienTaiThiKhongChayBu() {
    assertFalse(BackupScheduler.canChayBu(Optional.of(BAY_GIO), BAY_GIO, 0));
  }

  @Test
  @DisplayName("Bản gần nhất vừa đúng 24 giờ thì chưa chạy bù")
  void banDungBangChuKyThiKhongChayBu() {
    Instant lanCuoi = BAY_GIO.minus(Duration.ofHours(24));
    assertFalse(BackupScheduler.canChayBu(Optional.of(lanCuoi), BAY_GIO, 24));
  }

  @Test
  @DisplayName("Trạng thái SUSPICIOUS thì không gọi sao lưu")
  void trangThaiSuspiciousThiKhongSaoLuu() {
    BackupServiceGia service = new BackupServiceGia();
    BackupScheduler scheduler = new BackupScheduler(service, "SYSTEM");
    AppState.set(AppState.State.SUSPICIOUS);

    scheduler.chay("AUTO");

    assertFalse(service.saoLuuDuocGoi, "dữ liệu bất thường thì không được sao lưu tự động");
    assertFalse(MaintenanceLock.isBusy(), "không được giữ khóa khi bỏ qua");
  }

  @Test
  @DisplayName("Trạng thái NORMAL thì gọi sao lưu")
  void trangThaiNormalThiSaoLuu() {
    BackupServiceGia service = new BackupServiceGia();
    BackupScheduler scheduler = new BackupScheduler(service, "SYSTEM");
    AppState.set(AppState.State.NORMAL);

    scheduler.chay("AUTO");

    assertTrue(service.saoLuuDuocGoi, "trạng thái bình thường thì phải chạy sao lưu");
    assertEquals(LoaiSaoLuu.AUTO, service.loaiNhanDuoc);
  }

  /** Dịch vụ sao lưu giả: chỉ ghi nhận lời gọi, không chạy {@code pg_dump}. */
  private final class BackupServiceGia extends BackupService {

    private boolean saoLuuDuocGoi;
    private LoaiSaoLuu loaiNhanDuoc;

    BackupServiceGia() {
      super(new NguonDuLieuGia(), new BackupStore(tempDir.resolve("backups"), null),
          new PgCommandExecutor(Path.of("pg_dump"), Path.of("pg_restore"),
              Path.of("createdb"), "localhost", 5432, "test", "test", ""),
          new LichSuGia());
    }

    @Override
    public BackupRecord saoLuu(LoaiSaoLuu loai, String nguoiThucHien) {
      saoLuuDuocGoi = true;
      loaiNhanDuoc = loai;
      return new BackupRecord(null, tempDir.resolve("dtaq_test.dump"), 1L, SHA256_GIA, loai,
          TrangThaiSaoLuu.SUCCESS, "Sao lưu giả lập", Instant.now(), nguoiThucHien, "16.13",
          "test");
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

  /** Nguồn dữ liệu giả: các bài kiểm thử không mở kết nối qua nguồn này. */
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
