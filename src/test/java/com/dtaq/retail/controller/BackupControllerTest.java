package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.BackupConfig;
import com.dtaq.retail.config.BackupSettingsStore;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.service.AppState;
import com.dtaq.retail.service.BackupScheduler;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.BackupStore;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

  private Path tepCauHinh;

  @BeforeEach
  void chuanBiTepCauHinh() {
    tepCauHinh = tempDir.resolve("backup.properties");
    datTepCauHinh(tepCauHinh);
  }

  @AfterEach
  void dongPhien() {
    CurrentSession.clear();
    AppState.set(AppState.State.NORMAL);
    datTepCauHinh(null);
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

  @Test
  @DisplayName("quản lý lưu cấu hình thì ghi tệp và lập lại lịch ngay")
  void quanLyLuuCauHinh() throws IOException {
    BackupService backupService = taoBackupService();
    BackupSchedulerGia scheduler = new BackupSchedulerGia(backupService, "TEST");
    BackupController controller = taoController(backupService, scheduler);
    CurrentSession.set(nhanVien(MA_QUAN_LY, LoaiQuyen.NHAN_VIEN_QUAN_LY));

    controller.luuCauHinh(LocalTime.of(6, 30), 6);

    String noiDung = Files.readString(tepCauHinh, StandardCharsets.UTF_8);
    Properties daLuu = docTepCauHinh();
    assertTrue(noiDung.contains(BackupConfig.KEY_GIO_CHAY + "="),
        "tệp cấu hình phải có khóa giờ chạy, nội dung: " + noiDung);
    assertTrue(noiDung.contains(BackupConfig.KEY_CHU_KY_GIO + "="),
        "tệp cấu hình phải có khóa chu kỳ, nội dung: " + noiDung);
    assertEquals("06:30", daLuu.getProperty(BackupConfig.KEY_GIO_CHAY),
        "giờ chạy lưu trong tệp phải là 06:30");
    assertEquals("6", daLuu.getProperty(BackupConfig.KEY_CHU_KY_GIO),
        "chu kỳ lưu trong tệp phải là 6 giờ");
    assertEquals(1, scheduler.soLanLapLai(), "bộ lập lịch phải được lập lại đúng một lần");
  }

  @Test
  @DisplayName("nhân viên bán hàng không lưu được cấu hình và tệp không đổi")
  void nhanVienBanHangKhongLuuDuocCauHinh() {
    BackupController controller = taoController();
    CurrentSession.set(nhanVien(MA_BAN_HANG, LoaiQuyen.NHAN_VIEN_BAN_HANG));

    assertThrows(SecurityException.class,
        () -> controller.luuCauHinh(LocalTime.of(6, 30), 6));
    assertFalse(Files.exists(tepCauHinh), "không được tạo tệp cấu hình khi thiếu quyền");
  }

  @Test
  @DisplayName("chưa đăng nhập thì không lưu được cấu hình và tệp không đổi")
  void chuaDangNhapKhongLuuDuocCauHinh() {
    BackupController controller = taoController();

    assertThrows(SecurityException.class,
        () -> controller.luuCauHinh(LocalTime.of(6, 30), 6));
    assertFalse(Files.exists(tepCauHinh), "không được tạo tệp cấu hình khi chưa đăng nhập");
  }

  @Test
  @DisplayName("chu kỳ ngoài khoảng 1 đến 168 bị từ chối")
  void chuKyNgoaiKhoangBiTuChoi() {
    BackupController controller = taoController();
    CurrentSession.set(nhanVien(MA_QUAN_LY, LoaiQuyen.NHAN_VIEN_QUAN_LY));

    assertThrows(IllegalArgumentException.class,
        () -> controller.luuCauHinh(LocalTime.of(6, 30), 0));
    assertThrows(IllegalArgumentException.class,
        () -> controller.luuCauHinh(LocalTime.of(6, 30), 169));
    assertFalse(Files.exists(tepCauHinh), "cấu hình không hợp lệ không được ghi ra tệp");
  }

  private BackupController taoController() {
    DuLieuGia duLieu = taoDuLieuGia();
    return new BackupController(duLieu.backupService(), duLieu.restoreService());
  }

  private BackupController taoController(BackupService backupService, BackupScheduler scheduler) {
    RestoreService restoreService = new RestoreService(
        new NguonDuLieuGia(), taoKho(), taoPg(), new DaoGia(), backupService);
    return new BackupController(backupService, restoreService, scheduler);
  }

  private BackupService taoBackupService() {
    return new BackupService(new NguonDuLieuGia(), taoKho(), taoPg(), new DaoGia());
  }

  private DuLieuGia taoDuLieuGia() {
    BackupStore store = taoKho();
    PgCommandExecutor pg = taoPg();
    LichSuSaoLuuDao dao = new DaoGia();
    BackupService backupService = new BackupService(new NguonDuLieuGia(), store, pg, dao);
    RestoreService restoreService =
        new RestoreService(new NguonDuLieuGia(), store, pg, dao, backupService);
    return new DuLieuGia(backupService, restoreService);
  }

  private BackupStore taoKho() {
    return new BackupStore(tempDir.resolve("backups"), null);
  }

  private static PgCommandExecutor taoPg() {
    return new PgCommandExecutor(
        Path.of("pg_dump"),
        Path.of("pg_restore"),
        Path.of("createdb"),
        "localhost",
        5432,
        "test",
        "test",
        "");
  }

  /**
   * Đặt tệp cấu hình dùng cho kiểm thử.
   *
   * <p>Hai phương thức đặt tệp của {@link BackupSettingsStore} và {@link BackupConfig} là
   * package-private trong gói {@code config}, còn lớp kiểm thử này nằm ở gói {@code controller};
   * gọi qua reflection để không phải nới rộng phạm vi truy cập của mã sản phẩm.
   */
  private static void datTepCauHinh(Path path) {
    datTep(BackupSettingsStore.class, "setFileForTest", path);
    datTep(BackupConfig.class, "setBackupPropertiesFileForTest", path);
  }

  private static void datTep(Class<?> kieu, String tenPhuongThuc, Path path) {
    try {
      Method phuongThuc = kieu.getDeclaredMethod(tenPhuongThuc, Path.class);
      phuongThuc.setAccessible(true);
      phuongThuc.invoke(null, path);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Không đặt được tệp cấu hình kiểm thử: " + tenPhuongThuc, e);
    }
  }

  /** Đọc lại tệp cấu hình theo đúng cách {@link BackupConfig} đọc, kể cả phần thoát ký tự. */
  private Properties docTepCauHinh() throws IOException {
    Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(
        Files.newInputStream(tepCauHinh), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  /** Cặp dịch vụ sao lưu/phục hồi dùng chung một kho và một DAO giả. */
  private record DuLieuGia(BackupService backupService, RestoreService restoreService) {
  }

  /** Bộ lập lịch giả chỉ đếm số lần được yêu cầu lập lại, không tạo luồng. */
  private static final class BackupSchedulerGia extends BackupScheduler {

    private int soLanLapLai;

    private BackupSchedulerGia(BackupService service, String nguoiThucHien) {
      super(service, nguoiThucHien);
    }

    @Override
    public synchronized void lapLichLai() {
      soLanLapLai++;
    }

    private int soLanLapLai() {
      return soLanLapLai;
    }
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
