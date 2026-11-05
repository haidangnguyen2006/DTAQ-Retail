package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthIncident.Severity;
import com.dtaq.retail.entity.HealthIncident.TrangThai;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.service.HealthStateStore.HealthState;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử chính sách của {@link RecoveryCoordinator} với kho trạng thái thật và các dịch vụ giả.
 *
 * <p>Không bài kiểm thử nào chạm cơ sở dữ liệu hay chạy {@code pg_restore}: sự cố, bản sao lưu và
 * phục hồi đều là bản giả trong bộ nhớ. Cấu hình được đặt bằng system property để không phụ thuộc
 * {@code db.properties} của máy chạy.
 */
class RecoveryCoordinatorTest {

  private static final String SHA256_GIA =
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

  private static final List<String> KHOA_CAU_HINH = List.of(
      HealthConfig.KEY_BAT_TAT,
      HealthConfig.KEY_SO_CHU_KY,
      HealthConfig.KEY_AUTO_RESTORE_BAT_TAT,
      HealthConfig.KEY_AUTO_RESTORE_TOI_DA,
      HealthConfig.KEY_SO_BAN_DU_PHONG,
      HealthConfig.KEY_NHAC_LAI_PHUT);

  @TempDir
  Path tempDir;

  private HealthStateStore store;
  private IncidentDaoGia incidentDao;
  private BackupServiceGia backupService;
  private RestoreServiceGia restoreService;
  private RecoveryCoordinator coordinator;

  @BeforeEach
  void setUp() {
    System.setProperty(HealthConfig.KEY_BAT_TAT, "true");
    System.setProperty(HealthConfig.KEY_SO_CHU_KY, "2");
    System.setProperty(HealthConfig.KEY_AUTO_RESTORE_BAT_TAT, "true");
    System.setProperty(HealthConfig.KEY_AUTO_RESTORE_TOI_DA, "3");
    System.setProperty(HealthConfig.KEY_SO_BAN_DU_PHONG, "3");
    System.setProperty(HealthConfig.KEY_NHAC_LAI_PHUT, "30");

    store = new HealthStateStore(tempDir.resolve("health_state.json"));
    incidentDao = new IncidentDaoGia();
    BackupStore kho = new BackupStore(tempDir.resolve("backups"), null);
    PgCommandExecutor pg = new PgCommandExecutor(Path.of("pg_dump"), Path.of("pg_restore"),
        Path.of("createdb"), "localhost", 5432, "test", "test", "");
    backupService = new BackupServiceGia(kho, pg, new LichSuGia());
    restoreService = new RestoreServiceGia(kho, pg, new LichSuGia(), backupService);
    coordinator = new RecoveryCoordinator(store, incidentDao, backupService, restoreService);
  }

  @AfterEach
  void tearDown() {
    for (String khoa : KHOA_CAU_HINH) {
      System.clearProperty(khoa);
    }
    HealthConfig.setPropertiesFileForTest(null);
    MaintenanceLock.release();
    AppState.set(AppState.State.NORMAL);
  }

  @Test
  @DisplayName("HEALTHY cập nhật baseline, xóa streak và đóng sự cố đang mở")
  void healthyCapNhatBaselineVaDongSuCo() {
    store.ghi(new HealthState(Map.of("A", 100L), Map.of("A", 3), 7L, null, List.of(),
        "SUSPICIOUS", null));
    incidentDao.themSan(new HealthIncident(7L, Severity.SUSPICIOUS, "cũ", TrangThai.PENDING,
        null, Instant.now().minusSeconds(600), null, null));

    coordinator.xuLy(baoCaoo(HealthStatus.HEALTHY, Map.of("A", 100L, "B", 40L), Map.of()));

    HealthState sau = store.doc();
    assertEquals(Map.of("A", 100L, "B", 40L), sau.baseline());
    assertTrue(sau.suspiciousStreak().isEmpty());
    assertNull(sau.openIncidentId());
    assertEquals("HEALTHY", sau.lastStatus());
    HealthIncident suCo = incidentDao.findById(7L).orElseThrow();
    assertEquals(TrangThai.DISMISSED, suCo.trangThai());
    assertEquals("SYSTEM", suCo.resolvedBy());
    assertTrue(suCo.resolvedAt() != null);
    assertEquals(AppState.State.NORMAL, AppState.get());
  }

  @Test
  @DisplayName("SUSPICIOUS cần đủ số chu kỳ mới tạo sự cố PENDING")
  void suspiciousCanDuChuKyMoiTaoSuCo() {
    HealthReport baoCao = baoCaoo(HealthStatus.SUSPICIOUS, Map.of("A", 10L), Map.of("A", 0.9));

    coordinator.xuLy(baoCao);

    assertTrue(incidentDao.findAll().isEmpty(), "một chu kỳ chưa đủ để tạo sự cố");
    assertEquals(1, store.doc().suspiciousStreak().get("A"));
    assertNull(store.doc().openIncidentId());
    assertEquals(AppState.State.NORMAL, AppState.get(), "chưa đủ chu kỳ thì giữ nguyên trạng thái");

    coordinator.xuLy(baoCao);

    assertEquals(1, incidentDao.findAll().size());
    HealthIncident suCo = incidentDao.findAll().get(0);
    assertEquals(TrangThai.PENDING, suCo.trangThai());
    assertEquals(Severity.SUSPICIOUS, suCo.severity());
    assertEquals(2, store.doc().suspiciousStreak().get("A"));
    assertEquals(suCo.id(), store.doc().openIncidentId());
    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
    assertTrue(restoreService.daPhucHoi.isEmpty(), "SUSPICIOUS không bao giờ tự phục hồi");
  }

  @Test
  @DisplayName("SUSPICIOUS đặt lại streak của bảng không còn giảm")
  void suspiciousDatLaiStreakBangKhongGiam() {
    store.ghi(new HealthState(Map.of(), Map.of("A", 1, "B", 2), null, null, List.of(), null,
        null));

    coordinator.xuLy(baoCaoo(HealthStatus.SUSPICIOUS, Map.of("A", 10L, "B", 500L),
        Map.of("A", 0.9)));

    assertEquals(2, store.doc().suspiciousStreak().get("A"));
    assertEquals(0, store.doc().suspiciousStreak().get("B"));
  }

  @Test
  @DisplayName("CRITICAL tự động phục hồi thành công thì đóng sự cố AUTO_RESTORED")
  void criticalTuDongPhucHoiThanhCong() {
    BackupRecord moiNhat = banGhiSaoLuu("dtaq_moi_nhat.dump", Instant.now().minusSeconds(60));
    BackupRecord cuHon = banGhiSaoLuu("dtaq_cu_hon.dump", Instant.now().minusSeconds(3600));
    backupService.danhSach = List.of(moiNhat, cuHon);

    coordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertEquals(1, restoreService.daPhucHoi.size(), "chỉ cần một lần phục hồi thành công");
    assertEquals(moiNhat.fileName(), restoreService.daPhucHoi.get(0).fileName());
    assertEquals(LoaiSaoLuu.AUTO_RESTORE, restoreService.loai.get(0));
    assertEquals("SYSTEM", restoreService.nguoiThucHien.get(0));
    assertTrue(restoreService.daXacNhan.get(0));

    HealthIncident suCo = incidentDao.findAll().get(0);
    assertEquals(TrangThai.AUTO_RESTORED, suCo.trangThai());
    assertEquals(moiNhat.fileName(), suCo.backupUsed());
    assertEquals(AppState.State.NORMAL, AppState.get());

    HealthState sau = store.doc();
    assertEquals("HEALTHY", sau.lastStatus());
    assertNull(sau.openIncidentId());
    assertEquals(1, sau.autoRestoreAttempts().size());
  }

  @Test
  @DisplayName("CRITICAL thử lần lượt các bản dự phòng và dừng ở bản hợp lệ đầu tiên")
  void criticalBoQuaBanHongVaThuBanKeTiep() {
    BackupRecord hong = banGhiSaoLuu("dtaq_hong.dump", Instant.now().minusSeconds(60));
    BackupRecord tot = banGhiSaoLuu("dtaq_tot.dump", Instant.now().minusSeconds(120));
    backupService.danhSach = List.of(hong, tot);
    backupService.hong.add(hong.fileName());

    coordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertEquals(List.of(hong.fileName(), tot.fileName()), backupService.daKiemTra);
    assertEquals(1, restoreService.daPhucHoi.size());
    assertEquals(tot.fileName(), restoreService.daPhucHoi.get(0).fileName());
    assertEquals(TrangThai.AUTO_RESTORED, incidentDao.findAll().get(0).trangThai());
  }

  @Test
  @DisplayName("CRITICAL đã dùng hết hạn mức trong giờ thì không phục hồi và đánh dấu FAILED")
  void criticalVuotGioiHanThiFailed() {
    Instant now = Instant.now();
    store.ghi(new HealthState(Map.of(), Map.of(), null, null,
        List.of(now.minusSeconds(60), now.minusSeconds(120), now.minusSeconds(180)), "CRITICAL",
        null));
    backupService.danhSach = List.of(banGhiSaoLuu("dtaq_a.dump", now.minusSeconds(60)));

    coordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertTrue(restoreService.daPhucHoi.isEmpty(), "vượt hạn mức thì không được gọi phục hồi");
    assertEquals(TrangThai.FAILED, incidentDao.findAll().get(0).trangThai());
    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
    assertEquals("CRITICAL", store.doc().lastStatus());
    assertNull(store.doc().openIncidentId());
    assertEquals(3, store.doc().autoRestoreAttempts().size());
  }

  @Test
  @DisplayName("CRITICAL tắt tự động phục hồi thì chỉ cảnh báo và giữ sự cố PENDING")
  void criticalTatTuDongPhucHoiThiChiCanhBao() {
    System.setProperty(HealthConfig.KEY_AUTO_RESTORE_BAT_TAT, "false");
    backupService.danhSach = List.of(banGhiSaoLuu("dtaq_a.dump", Instant.now()));

    coordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertTrue(restoreService.daPhucHoi.isEmpty());
    assertEquals(TrangThai.PENDING, incidentDao.findAll().get(0).trangThai());
    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
    assertEquals("CRITICAL", store.doc().lastStatus());
    assertEquals(incidentDao.findAll().get(0).id(), store.doc().openIncidentId());
  }

  @Test
  @DisplayName("CRITICAL không có bản sao lưu hợp lệ thì đánh dấu FAILED")
  void criticalKhongCoBanDuPhongThiFailed() {
    backupService.danhSach = List.of();

    coordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertTrue(restoreService.daPhucHoi.isEmpty());
    assertEquals(TrangThai.FAILED, incidentDao.findAll().get(0).trangThai());
    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
  }

  @Test
  @DisplayName("CRITICAL phục hồi lỗi hết các bản thì đánh dấu FAILED")
  void criticalPhucHoiLoiHetThiFailed() {
    BackupRecord mot = banGhiSaoLuu("dtaq_mot.dump", Instant.now().minusSeconds(60));
    BackupRecord hai = banGhiSaoLuu("dtaq_hai.dump", Instant.now().minusSeconds(120));
    backupService.danhSach = List.of(mot, hai);
    restoreService.loi.add(mot.fileName());
    restoreService.loi.add(hai.fileName());

    coordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertEquals(2, restoreService.daPhucHoi.size());
    assertEquals(TrangThai.FAILED, incidentDao.findAll().get(0).trangThai());
    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
    assertEquals(2, store.doc().autoRestoreAttempts().size());
  }

  @Test
  @DisplayName("UNAVAILABLE chỉ ghi trạng thái phụ và không bao giờ phục hồi")
  void unavailableKhongPhucHoi() {
    coordinator.xuLy(baoCaoo(HealthStatus.UNAVAILABLE, Map.of(), Map.of()));

    assertTrue(restoreService.daPhucHoi.isEmpty());
    assertTrue(incidentDao.findAll().isEmpty());
    assertEquals(AppState.State.UNAVAILABLE, AppState.get());
    HealthState sau = store.doc();
    assertEquals("UNAVAILABLE", sau.lastStatus());
    assertTrue(sau.lastNotifiedAt() != null);
  }

  @Test
  @DisplayName("UNAVAILABLE chỉ nhắc lại sau khoảng nhắc cấu hình")
  void unavailableChiNhacLaiSauKhoangNhac() {
    Instant cu = Instant.now().minusSeconds(60);
    store.ghi(new HealthState(Map.of(), Map.of(), null, cu, List.of(), "UNAVAILABLE", null));

    coordinator.xuLy(baoCaoo(HealthStatus.UNAVAILABLE, Map.of(), Map.of()));

    assertEquals(cu, store.doc().lastNotifiedAt(), "chưa đủ 30 phút thì giữ mốc cũ");
  }

  @Test
  @DisplayName("CRITICAL vẫn tự động phục hồi khi không ghi được sự cố")
  void criticalVanPhucHoiKhiKhongGhiDuocSuCo() {
    BackupRecord banGhi = banGhiSaoLuu("dtaq_tot.dump", Instant.now().minusSeconds(60));
    backupService.danhSach = List.of(banGhi);
    RecoveryCoordinator loi =
        new RecoveryCoordinator(store, new IncidentDaoLoiGhi(), backupService, restoreService);

    loi.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertEquals(1, restoreService.daPhucHoi.size(),
        "phải thử phục hồi dù không ghi được sự cố");
    assertEquals(LoaiSaoLuu.AUTO_RESTORE, restoreService.loai.get(0));
    assertEquals(banGhi.fileName(), restoreService.daPhucHoi.get(0).fileName());
    assertEquals(AppState.State.NORMAL, AppState.get());
    assertEquals("HEALTHY", store.doc().lastStatus());
    assertEquals(1, store.doc().autoRestoreAttempts().size());
  }

  @Test
  @DisplayName("SUSPICIOUS vẫn cảnh báo khi không ghi được sự cố")
  void suspiciousVanCanhBaoKhiKhongGhiDuocSuCo() {
    RecoveryCoordinator loi =
        new RecoveryCoordinator(store, new IncidentDaoLoiGhi(), backupService, restoreService);
    HealthReport baoCao = baoCaoo(HealthStatus.SUSPICIOUS, Map.of("A", 10L), Map.of("A", 0.9));

    loi.xuLy(baoCao);
    loi.xuLy(baoCao);

    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
    assertNull(store.doc().openIncidentId(), "không có sự cố nào được ghi");
    assertTrue(restoreService.daPhucHoi.isEmpty(), "SUSPICIOUS không bao giờ tự phục hồi");
  }

  @Test
  @DisplayName("Giới hạn tần suất vẫn hiệu lực khi tệp trạng thái không ghi được")
  void gioiHanTanSuatDungBoNhoKhiKhongGhiDuocTep() {
    HealthStateStore khongGhi = new HealthStateStore(tempDir.resolve("khong_ghi.json")) {
      @Override
      public void ghi(HealthState state) {
        // Giả lập thư mục sao lưu không ghi được: chỉ bộ nhớ giữ được lượt thử.
      }
    };
    BackupRecord mot = banGhiSaoLuu("dtaq_mot.dump", Instant.now().minusSeconds(60));
    BackupRecord hai = banGhiSaoLuu("dtaq_hai.dump", Instant.now().minusSeconds(120));
    BackupRecord ba = banGhiSaoLuu("dtaq_ba.dump", Instant.now().minusSeconds(180));
    backupService.danhSach = List.of(mot, hai, ba);
    restoreService.loi.addAll(List.of(mot.fileName(), hai.fileName(), ba.fileName()));
    RecoveryCoordinator khongGhiCoordinator =
        new RecoveryCoordinator(khongGhi, incidentDao, backupService, restoreService);

    khongGhiCoordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));
    assertEquals(3, restoreService.daPhucHoi.size(), "phải thử đủ ba bản dự phòng");

    khongGhiCoordinator.xuLy(baoCaoo(HealthStatus.CRITICAL, Map.of(), Map.of()));

    assertEquals(3, restoreService.daPhucHoi.size(),
        "hết hạn mức trong bộ nhớ thì không được thử lại");
    assertEquals(AppState.State.SUSPICIOUS, AppState.get());
  }

  private HealthReport baoCaoo(HealthStatus trangThai, Map<String, Long> soDong,
      Map<String, Double> tyLeGiam) {
    return new HealthReport(trangThai, soDong, Map.of(), tyLeGiam, List.of(),
        "mô tả giả lập", null, Instant.now());
  }

  private BackupRecord banGhiSaoLuu(String ten, Instant thoiDiem) {
    return new BackupRecord(null, tempDir.resolve(ten), 1L, SHA256_GIA, LoaiSaoLuu.AUTO,
        TrangThaiSaoLuu.SUCCESS, "Sao lưu thành công", thoiDiem, "SYSTEM", "16.13", "test");
  }

  /** Sự cố giả lưu trong bộ nhớ, không cần cơ sở dữ liệu. */
  private static final class IncidentDaoGia implements HealthIncidentDao {
    private final Map<Long, HealthIncident> theoId = new LinkedHashMap<>();
    private long tiepTheo = 1L;

    void themSan(HealthIncident suCo) {
      theoId.put(suCo.id(), suCo);
      tiepTheo = Math.max(tiepTheo, suCo.id() + 1);
    }

    @Override
    public long insert(HealthIncident incident) {
      long id = tiepTheo++;
      theoId.put(id, new HealthIncident(id, incident.severity(), incident.detail(),
          incident.trangThai(), incident.backupUsed(), incident.detectedAt(),
          incident.resolvedAt(), incident.resolvedBy()));
      return id;
    }

    @Override
    public Optional<HealthIncident> findById(long id) {
      return Optional.ofNullable(theoId.get(id));
    }

    @Override
    public Optional<HealthIncident> findMoiNhatDangCho() {
      return theoId.values().stream()
          .filter(HealthIncident::dangCho)
          .reduce((truoc, sau) -> sau);
    }

    @Override
    public List<HealthIncident> findAll() {
      return List.copyOf(theoId.values());
    }

    @Override
    public void capNhatKetThuc(long id, TrangThai trangThai, Instant resolvedAt,
        String resolvedBy, String backupUsed) {
      HealthIncident cu = theoId.get(id);
      if (cu == null) {
        return;
      }
      theoId.put(id, new HealthIncident(id, cu.severity(), cu.detail(), trangThai, backupUsed,
          cu.detectedAt(), resolvedAt, resolvedBy));
    }
  }

  /** DAO sự cố luôn lỗi, mô phỏng bảng {@code "SuCoSucKhoe"} không truy cập được. */
  private static final class IncidentDaoLoiGhi implements HealthIncidentDao {

    @Override
    public long insert(HealthIncident incident) {
      throw new DataAccessException("Không truy cập được bảng SuCoSucKhoe");
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
      throw new DataAccessException("Không truy cập được bảng SuCoSucKhoe");
    }
  }

  /** Dịch vụ sao lưu giả: danh sách và kiểm tra do bài kiểm thử quyết định. */
  private static final class BackupServiceGia extends BackupService {

    private List<BackupRecord> danhSach = List.of();
    private final Set<String> hong = new HashSet<>();
    private final List<String> daKiemTra = new ArrayList<>();

    BackupServiceGia(BackupStore store, PgCommandExecutor pg, LichSuSaoLuuDao dao) {
      super(new NguonDuLieuGia(), store, pg, dao);
    }

    @Override
    public List<BackupRecord> danhSach() {
      return danhSach;
    }

    @Override
    public void kiemTra(BackupRecord banGhi) {
      daKiemTra.add(banGhi.fileName());
      if (hong.contains(banGhi.fileName())) {
        throw new BackupException("Bản sao lưu giả lập bị hỏng");
      }
    }
  }

  /** Dịch vụ phục hồi giả: ghi lại lời gọi, không chạy {@code pg_restore}. */
  private static final class RestoreServiceGia extends RestoreService {

    private final List<BackupRecord> daPhucHoi = new ArrayList<>();
    private final List<LoaiSaoLuu> loai = new ArrayList<>();
    private final List<String> nguoiThucHien = new ArrayList<>();
    private final List<Boolean> daXacNhan = new ArrayList<>();
    private final Set<String> loi = new HashSet<>();

    RestoreServiceGia(BackupStore store, PgCommandExecutor pg, LichSuSaoLuuDao dao,
        BackupService backupService) {
      super(new NguonDuLieuGia(), store, pg, dao, backupService);
    }

    @Override
    public BackupRecord phucHoi(BackupRecord banGhi, String nguoiThucHien, boolean daXacNhan,
        LoaiSaoLuu loaiSaoLuu) {
      daPhucHoi.add(banGhi);
      loai.add(loaiSaoLuu);
      this.nguoiThucHien.add(nguoiThucHien);
      this.daXacNhan.add(daXacNhan);
      if (loi.contains(banGhi.fileName())) {
        throw new BackupException("Phục hồi giả lập thất bại");
      }
      return new BackupRecord(null, banGhi.filePath(), banGhi.fileSize(), banGhi.sha256(),
          loaiSaoLuu, TrangThaiSaoLuu.SUCCESS, "Phục hồi từ " + banGhi.fileName(),
          Instant.now(), nguoiThucHien, banGhi.phienBanPg(), banGhi.tenCsdl());
    }
  }

  /** DAO lịch sử giả, không dùng trong các bài kiểm thử này. */
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
      return daGhi.stream().filter(BackupRecord::thanhCong).findFirst();
    }

    @Override
    public boolean existsByFilePath(String filePath) {
      return daGhi.stream().anyMatch(banGhi -> banGhi.filePath().toString().equals(filePath));
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

  @Test
  @DisplayName("Không ném ngoại lệ khi kho trạng thái lỗi giữa chu kỳ")
  void khongNemNgoaiLeKhiKhoLoi() {    HealthStateStore khoLoi =
        new HealthStateStore(tempDir.resolve("thu_muc_khong_ton_tai/x.json")) {
      @Override
      public HealthState doc() {
        throw new IllegalStateException("giả lập lỗi đọc");
      }
    };
    RecoveryCoordinator loi = new RecoveryCoordinator(khoLoi, incidentDao, backupService,
        restoreService);
    AppState.set(AppState.State.SUSPICIOUS);

    loi.xuLy(baoCaoo(HealthStatus.HEALTHY, Map.of("A", 1L), Map.of()));

    assertEquals(AppState.State.SUSPICIOUS, AppState.get(),
        "trạng thái phải giữ nguyên khi chu kỳ lỗi");
  }
}
