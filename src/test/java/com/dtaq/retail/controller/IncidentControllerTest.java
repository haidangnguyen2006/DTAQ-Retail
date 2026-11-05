package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthIncident.Severity;
import com.dtaq.retail.entity.HealthIncident.TrangThai;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.service.AppState;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.BackupStore;
import com.dtaq.retail.service.DataHealthChecker;
import com.dtaq.retail.service.HealthStateStore;
import com.dtaq.retail.service.HealthStateStore.HealthState;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử chặn quyền và các lựa chọn xử lý sự cố của {@link IncidentController}.
 *
 * <p>Không bài kiểm thử nào mở kết nối cơ sở dữ liệu hay chạy {@code pg_dump}/{@code pg_restore}:
 * DAO sự cố lưu trong bộ nhớ, kho sao lưu là thư mục tạm rỗng, công cụ PostgreSQL là bản giả không
 * chạy tiến trình, và bộ kiểm tra sức khỏe trả báo cáo dựng sẵn.
 */
class IncidentControllerTest {

  private static final String MA_QUAN_LY = "NVQUANLY-TEST-001";
  private static final String MA_BAN_HANG = "NVBANHANG-TEST-001";

  @TempDir
  Path tempDir;

  private HealthStateStore stateStore;
  private IncidentDaoGia incidentDao;
  private DataHealthCheckerGia checker;

  @BeforeEach
  void setUp() {
    stateStore = new HealthStateStore(tempDir.resolve("health_state.json"));
    incidentDao = new IncidentDaoGia();
    checker = new DataHealthCheckerGia(stateStore);
  }

  @AfterEach
  void donDep() {
    CurrentSession.clear();
    AppState.set(AppState.State.NORMAL);
  }

  @Test
  @DisplayName("nhân viên bán hàng bị chặn khi xem sự cố đang chờ")
  void nhanVienBanHangBiChan() {
    IncidentController controller = taoController();
    CurrentSession.set(nhanVien(MA_BAN_HANG, LoaiQuyen.NHAN_VIEN_BAN_HANG));

    assertThrows(SecurityException.class, controller::suCoDangCho);
  }

  @Test
  @DisplayName("chưa đăng nhập cũng bị chặn khi xem sự cố đang chờ")
  void chuaDangNhapBiChan() {
    IncidentController controller = taoController();

    assertThrows(SecurityException.class, controller::suCoDangCho);
  }

  @Test
  @DisplayName("để sau giữ sự cố PENDING và ghi nhận thời điểm nhắc")
  void deSauGiuSuCoDangCho() {
    CurrentSession.set(nhanVien(MA_QUAN_LY, LoaiQuyen.NHAN_VIEN_QUAN_LY));
    HealthIncident suCo = suCo(1L, Severity.SUSPICIOUS, TrangThai.PENDING);
    incidentDao.them(suCo);
    IncidentController controller = taoController();

    controller.deSau(suCo);

    HealthIncident sau = incidentDao.findById(1L).orElseThrow();
    assertEquals(TrangThai.PENDING, sau.trangThai(), "sự cố phải vẫn chờ xử lý");
    assertNull(sau.resolvedAt());
    assertNotNull(stateStore.doc().lastNotifiedAt(), "phải ghi nhận thời điểm nhắc");
    assertEquals(AppState.State.NORMAL, AppState.get(), "để sau không đổi trạng thái ứng dụng");
  }

  @Test
  @DisplayName("bỏ qua khi dữ liệu hợp lệ cập nhật baseline và kết thúc sự cố")
  void boQuaHopLeCapNhatBaseline() {
    CurrentSession.set(nhanVien(MA_QUAN_LY, LoaiQuyen.NHAN_VIEN_QUAN_LY));
    HealthIncident suCo = suCo(1L, Severity.SUSPICIOUS, TrangThai.PENDING);
    incidentDao.them(suCo);
    Map<String, Long> soDong = new LinkedHashMap<>();
    soDong.put("NhanVien", 12L);
    soDong.put("HoaDon", 7L);
    checker.traVe(baoCaoGia(HealthStatus.HEALTHY, soDong));
    stateStore.ghi(HealthState.rong()
        .withBaseline(Map.of("NhanVien", 99L))
        .withOpenIncident(1L)
        .withStreak(Map.of("NhanVien", 2)));
    AppState.set(AppState.State.SUSPICIOUS);
    IncidentController controller = taoController();

    controller.boQuaHopLe(suCo);

    HealthState sau = stateStore.doc();
    assertEquals(soDong, sau.baseline(), "baseline phải lấy theo số dòng hiện tại");
    assertTrue(sau.suspiciousStreak().isEmpty(), "chuỗi bất thường phải được xóa");
    assertNull(sau.openIncidentId(), "sự cố đang mở phải được xóa");
    assertEquals("HEALTHY", sau.lastStatus());

    HealthIncident ketThuc = incidentDao.findById(1L).orElseThrow();
    assertEquals(TrangThai.DISMISSED, ketThuc.trangThai());
    assertEquals(MA_QUAN_LY, ketThuc.resolvedBy());
    assertNull(ketThuc.backupUsed());
    assertNotNull(ketThuc.resolvedAt());
    assertEquals(AppState.State.NORMAL, AppState.get());
  }

  @Test
  @DisplayName("bỏ qua khi dữ liệu nghiêm trọng bị từ chối và không đổi sự cố")
  void boQuaKhiDuLieuNghiemTrongBiTuChoi() {
    CurrentSession.set(nhanVien(MA_QUAN_LY, LoaiQuyen.NHAN_VIEN_QUAN_LY));
    HealthIncident suCo = suCo(1L, Severity.CRITICAL, TrangThai.PENDING);
    incidentDao.them(suCo);
    checker.traVe(baoCaoGia(HealthStatus.CRITICAL, Map.of()));
    IncidentController controller = taoController();

    BackupException loi =
        assertThrows(BackupException.class, () -> controller.boQuaHopLe(suCo));

    assertEquals("Không thể bỏ qua khi dữ liệu chưa sẵn sàng", loi.getMessage());
    HealthIncident sau = incidentDao.findById(1L).orElseThrow();
    assertEquals(TrangThai.PENDING, sau.trangThai(), "sự cố không được kết thúc");
    assertNull(sau.resolvedAt());
  }

  private IncidentController taoController() {
    BackupStore kho = new BackupStore(tempDir.resolve("backups"), null);
    PgCommandExecutor pg = new PgCommandExecutor(
        Path.of("pg_dump"),
        Path.of("pg_restore"),
        Path.of("createdb"),
        "localhost",
        5432,
        "test",
        "test",
        "");
    LichSuSaoLuuDao lichSu = new LichSuGia();
    BackupService backupService = new BackupService(new NguonDuLieuGia(), kho, pg, lichSu);
    RestoreService restoreService =
        new RestoreService(new NguonDuLieuGia(), kho, pg, lichSu, backupService);
    return new IncidentController(
        incidentDao, backupService, restoreService, checker, stateStore);
  }

  private static HealthReport baoCaoGia(HealthStatus trangThai, Map<String, Long> soDong) {
    return new HealthReport(trangThai, soDong, Map.of(), Map.of(), List.of(),
        "báo cáo giả lập", null, Instant.now());
  }

  private static HealthIncident suCo(long id, Severity severity, TrangThai trangThai) {
    return new HealthIncident(
        id, severity, "Sự cố giả lập", trangThai, null, Instant.now(), null, null);
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

  /** DAO sự cố giả lưu trong bộ nhớ, không cần cơ sở dữ liệu thật. */
  private static final class IncidentDaoGia implements HealthIncidentDao {

    private final List<HealthIncident> suCo = new ArrayList<>();

    void them(HealthIncident incident) {
      suCo.add(incident);
    }

    @Override
    public long insert(HealthIncident incident) {
      long id = suCo.stream()
          .map(HealthIncident::id)
          .filter(Objects::nonNull)
          .max(Comparator.naturalOrder())
          .orElse(0L) + 1;
      suCo.add(new HealthIncident(id, incident.severity(), incident.detail(),
          incident.trangThai(), incident.backupUsed(), incident.detectedAt(),
          incident.resolvedAt(), incident.resolvedBy()));
      return id;
    }

    @Override
    public Optional<HealthIncident> findById(long id) {
      return suCo.stream().filter(incident -> incident.id() != null && incident.id() == id)
          .findFirst();
    }

    @Override
    public Optional<HealthIncident> findMoiNhatDangCho() {
      return suCo.stream()
          .filter(HealthIncident::dangCho)
          .max(Comparator.comparing(HealthIncident::detectedAt));
    }

    @Override
    public List<HealthIncident> findAll() {
      return List.copyOf(suCo);
    }

    @Override
    public void capNhatKetThuc(long id, TrangThai trangThai, Instant resolvedAt,
        String resolvedBy, String backupUsed) {
      for (int i = 0; i < suCo.size(); i++) {
        HealthIncident cu = suCo.get(i);
        if (cu.id() != null && cu.id() == id) {
          suCo.set(i, new HealthIncident(cu.id(), cu.severity(), cu.detail(), trangThai,
              backupUsed, cu.detectedAt(), resolvedAt, resolvedBy));
          return;
        }
      }
      throw new IllegalStateException("Không tìm thấy sự cố " + id);
    }
  }

  /** Bộ kiểm tra giả: trả báo cáo dựng sẵn, không chạm cơ sở dữ liệu. */
  private static final class DataHealthCheckerGia extends DataHealthChecker {

    private HealthReport baoCao;

    DataHealthCheckerGia(HealthStateStore store) {
      super(new NguonDuLieuGia(), store);
      this.baoCao = baoCaoGia(HealthStatus.HEALTHY, Map.of());
    }

    void traVe(HealthReport report) {
      this.baoCao = report;
    }

    @Override
    public HealthReport kiemTra() {
      return baoCao;
    }
  }

  /** DAO lịch sử giả: phục hồi không được gọi trong các bài kiểm thử này. */
  private static final class LichSuGia implements LichSuSaoLuuDao {

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
