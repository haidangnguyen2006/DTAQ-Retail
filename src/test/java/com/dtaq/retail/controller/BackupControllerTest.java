package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.BackupStore;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử chặn quyền và danh sách sao lưu của {@link BackupController}.
 *
 * <p>Không bài kiểm thử nào mở kết nối cơ sở dữ liệu hay chạy {@code pg_dump}/{@code pg_restore}:
 * kho sao lưu là thư mục tạm rỗng, công cụ PostgreSQL là bản giả không chạy tiến trình, và DAO
 * lịch sử lưu trong bộ nhớ.
 */
class BackupControllerTest {

  private static final String MA_QUAN_LY = "NVQUANLY-TEST-001";
  private static final String MA_BAN_HANG = "NVBANHANG-TEST-001";

  @TempDir
  Path tempDir;

  @AfterEach
  void dongPhien() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("nhân viên bán hàng bị chặn khi xem danh sách sao lưu")
  void nhanVienBanHangBiChan() {
    BackupController controller = taoController();
    CurrentSession.set(nhanVien(MA_BAN_HANG, LoaiQuyen.NHAN_VIEN_BAN_HANG));

    assertThrows(SecurityException.class, controller::danhSach);
  }

  @Test
  @DisplayName("chưa đăng nhập cũng bị chặn khi xem danh sách sao lưu")
  void chuaDangNhapBiChan() {
    BackupController controller = taoController();

    assertThrows(SecurityException.class, controller::danhSach);
  }

  @Test
  @DisplayName("quản lý xem được danh sách rỗng của kho sao lưu mới")
  void quanLyXemDuocDanhSachRong() {
    BackupController controller = taoController();
    CurrentSession.set(nhanVien(MA_QUAN_LY, LoaiQuyen.NHAN_VIEN_QUAN_LY));

    List<BackupRecord> danhSach = controller.danhSach();

    assertNotNull(danhSach);
    assertTrue(danhSach.isEmpty(), "kho sao lưu tạm phải rỗng");
  }

  private BackupController taoController() {
    BackupStore store = new BackupStore(tempDir.resolve("backups"), null);
    PgCommandExecutor pg = new PgCommandExecutor(
        Path.of("pg_dump"),
        Path.of("pg_restore"),
        Path.of("createdb"),
        "localhost",
        5432,
        "test",
        "test",
        "");
    LichSuSaoLuuDao dao = new DaoGia();
    BackupService backupService = new BackupService(new NguonDuLieuGia(), store, pg, dao);
    RestoreService restoreService =
        new RestoreService(new NguonDuLieuGia(), store, pg, dao, backupService);
    return new BackupController(backupService, restoreService);
  }

  private static NhanVien nhanVien(String maNV, LoaiQuyen loaiQuyen) {
    return new NhanVien(
        maNV,
        "Nguyễn Văn Kiểm Thử",
        "0900000000",
        "kiemthu@dtaq.local",
        LocalDate.of(2026, 1, 1),
        "kiemthu.test",
        "matkhau-test",
        loaiQuyen,
        true);
  }

  /** DAO lịch sử giả lưu trong bộ nhớ, không cần cơ sở dữ liệu thật. */
  private static final class DaoGia implements LichSuSaoLuuDao {

    private final List<BackupRecord> daGhi = new ArrayList<>();

    @Override
    public void insert(BackupRecord banGhi) {
      daGhi.add(banGhi);
    }

    @Override
    public List<BackupRecord> findAll() {
      return List.copyOf(daGhi);
    }

    @Override
    public Optional<BackupRecord> findMoiNhatThanhCong() {
      return daGhi.stream()
          .filter(BackupRecord::thanhCong)
          .max(Comparator.comparing(BackupRecord::createdAt));
    }

    @Override
    public boolean existsByFilePath(String filePath) {
      return daGhi.stream().anyMatch(banGhi -> banGhi.filePath().toString().equals(filePath));
    }
  }

  /** Nguồn dữ liệu giả: bài kiểm thử không mở kết nối qua nguồn này. */
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
