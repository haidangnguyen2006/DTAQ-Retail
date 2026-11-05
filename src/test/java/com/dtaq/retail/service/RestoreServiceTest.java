package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.DbMigration;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.dao.impl.LichSuSaoLuuDaoImpl;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử các nhánh bảo vệ và ghi lịch sử của {@link RestoreService}.
 *
 * <p>Không bài kiểm thử nào chạy {@code pg_restore} thật: công cụ PostgreSQL luôn là bản giả. Các
 * nhánh thất bại thay bốn móc kết nối bằng bản giả nên không đụng tới {@link ConnectDB}. Nhánh thành
 * công cần cơ sở dữ liệu thật để mở lại bể kết nối, kiểm tra bảng cốt lõi và ghi lịch sử nên được bỏ
 * qua khi không có PostgreSQL.
 */
class RestoreServiceTest {

  private static final String SHA256_GIA =
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

  @TempDir
  Path tempDir;

  private DaoGia dao;
  private PgGia pg;
  private BackupStore store;

  /** Các đường dẫn đã ghi vào bảng lịch sử để dọn lại sau mỗi bài kiểm thử. */
  private final List<String> duongDanLichSu = new ArrayList<>();

  @BeforeAll
  static void apDungMigration() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DbMigration.migrate(ConnectDB.getDataSource());
  }

  @AfterEach
  void donDep() {
    MaintenanceLock.release();
    AppState.set(AppState.State.NORMAL);
    for (String duongDan : duongDanLichSu) {
      try {
        xoaLichSu(duongDan);
      } catch (SQLException e) {
        // Dọn dẹp chỉ là hỗ trợ; không được che kết quả kiểm thử.
      }
    }
  }

  @Test
  @DisplayName("thiếu xác nhận 2 bước thì ném lỗi trước mọi thao tác")
  void thieuXacNhanThiNemTruocMoiThaoTac() {
    BackupServiceGia backupService = new BackupServiceGia(store(), pgGia(), daoGia());
    RestoreServiceGia service = taoService(backupService);

    BackupException loi = assertThrows(BackupException.class,
        () -> service.phucHoi(banGhiGia(), "NV-1", false));

    assertTrue(loi.getMessage().contains("xác nhận 2 bước"), loi.getMessage());
    assertFalse(backupService.kiemTraDuocGoi, "không được kiểm tra tệp khi thiếu xác nhận");
    assertFalse(backupService.saoLuuDuocGoi, "không được sao lưu an toàn khi thiếu xác nhận");
    assertEquals(0, pg.soLanRestore, "không được gọi pg_restore khi thiếu xác nhận");
    assertFalse(service.dongKetNoiDuocGoi, "không được đóng bể kết nối khi thiếu xác nhận");
    assertTrue(dao.daGhi.isEmpty(), "không được ghi lịch sử khi thiếu xác nhận");
    assertFalse(MaintenanceLock.isBusy(), "không được giữ khóa sau khi ném lỗi");
  }

  @Test
  @DisplayName("tệp sao lưu không tồn tại thì ném lỗi trước khi phục hồi")
  void kiemTraThatBaiThiNemTruocKhiPhucHoi() {
    BackupStore kho = store();
    PgGia pgGia = pgGia();
    // Dùng BackupService thật để kiểm tra sự tồn tại của tệp.
    BackupService backupService = new BackupService(new NguonDuLieuGia(), kho, pgGia, daoGia());
    RestoreServiceGia service = new RestoreServiceGia(kho, pgGia, daoGia(), backupService);
    BackupRecord banGhi = banGhiGia();

    BackupException loi = assertThrows(BackupException.class,
        () -> service.phucHoi(banGhi, "NV-1", true));

    assertTrue(loi.getMessage().contains("Không tìm thấy tệp sao lưu"), loi.getMessage());
    assertEquals(0, pgGia.soLanRestore, "không được gọi pg_restore khi tệp không hợp lệ");
    assertFalse(service.dongKetNoiDuocGoi, "không được đóng bể kết nối khi tệp không hợp lệ");
    assertEquals(1, dao.daGhi.size(), "phải ghi một bản lịch sử thất bại");
    assertEquals(LoaiSaoLuu.MANUAL_RESTORE, dao.daGhi.get(0).loai());
    assertEquals(TrangThaiSaoLuu.FAILED, dao.daGhi.get(0).trangThai());
    assertEquals("NV-1", dao.daGhi.get(0).createdBy());
    assertFalse(MaintenanceLock.isBusy(), "phải nhả khóa sau khi thất bại");
  }

  @Test
  @DisplayName("pg_restore lỗi thì ném BackupException, ghi FAILED và nhả khóa")
  void restoreLoiThiNemVaGhiFailed() {
    pg = pgGia();
    pg.datKetQuaRestore(new PgCommandExecutor.KetQua(1, "lỗi giả lập", false));
    BackupServiceGia backupService = new BackupServiceGia(store(), pg, daoGia());
    RestoreServiceGia service = taoService(backupService);

    BackupException loi = assertThrows(BackupException.class,
        () -> service.phucHoi(banGhiGia(), "NV-2", true));

    assertTrue(loi.getMessage().contains("lỗi giả lập"), loi.getMessage());
    assertTrue(backupService.saoLuuDuocGoi, "phải tạo bản sao lưu an toàn trước khi ghi");
    assertTrue(service.dongKetNoiDuocGoi, "phải đóng bể kết nối trước khi ghi");
    assertTrue(service.moLaiKetNoiDuocGoi, "phải mở lại bể kết nối sau khi ghi");
    assertEquals(1, pg.soLanRestore);
    assertEquals(1, dao.daGhi.size());
    assertEquals(LoaiSaoLuu.MANUAL_RESTORE, dao.daGhi.get(0).loai());
    assertEquals(TrangThaiSaoLuu.FAILED, dao.daGhi.get(0).trangThai());
    assertFalse(MaintenanceLock.isBusy(), "phải nhả khóa sau khi thất bại");
  }

  @Test
  @DisplayName("sao lưu an toàn lỗi không chặn phục hồi")
  void saoLuuAnToanLoiKhongChanPhucHoi() {
    pg = pgGia();
    pg.datKetQuaRestore(new PgCommandExecutor.KetQua(1, "pg_restore hỏng", false));
    BackupServiceGia backupService = new BackupServiceGia(store(), pg, daoGia());
    backupService.loiSaoLuu = new BackupException("ổ đĩa đầy");
    RestoreServiceGia service = taoService(backupService);

    BackupException loi = assertThrows(BackupException.class,
        () -> service.phucHoi(banGhiGia(), "NV-3", true));

    assertTrue(loi.getMessage().contains("pg_restore hỏng"), loi.getMessage());
    assertEquals(1, pg.soLanRestore, "vẫn phải thử phục hồi khi sao lưu an toàn lỗi");
    assertFalse(MaintenanceLock.isBusy(), "phải nhả khóa sau khi thất bại");
  }

  @Test
  @DisplayName("phục hồi thành công thì trả bản ghi MANUAL_RESTORE SUCCESS")
  void phucHoiThanhCongTraBanGhiSuccess() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    pg = pgGia();
    pg.datKetQuaRestore(new PgCommandExecutor.KetQua(0, "", false));
    BackupServiceGia backupService = new BackupServiceGia(store(), pg, daoGia());
    RestoreService service = new RestoreService(ConnectDB.getDataSource(), store(), pg, daoGia(),
        backupService);
    Path tep = tempDir.resolve("dtaq_" + UUID.randomUUID() + ".dump");
    duongDanLichSu.add(tep.toString());
    BackupRecord banGhi = banGhiNguon(tep, "Bản gốc");

    try {
      BackupRecord ketQua = service.phucHoi(banGhi, "NV-4", true);

      assertNotNull(ketQua);
      assertEquals(LoaiSaoLuu.MANUAL_RESTORE, ketQua.loai());
      assertEquals(TrangThaiSaoLuu.SUCCESS, ketQua.trangThai());
      assertTrue(ketQua.thongBao().contains(banGhi.fileName()), ketQua.thongBao());
      assertEquals("NV-4", ketQua.createdBy());
      assertEquals(banGhi.filePath(), ketQua.filePath());
      assertEquals(banGhi.sha256(), ketQua.sha256());
      assertEquals(banGhi.tenCsdl(), ketQua.tenCsdl());
      assertEquals(1, pg.soLanRestore);
      assertTrue(new LichSuSaoLuuDaoImpl(ConnectDB.getDataSource())
              .existsByFilePath(banGhi.filePath().toString()),
          "bản ghi SUCCESS phải được lưu vào lịch sử");
      assertFalse(MaintenanceLock.isBusy(), "phải nhả khóa sau khi thành công");
    } finally {
      xoaLichSu(tep.toString());
    }
  }

  @Test
  @DisplayName("phục hồi vẫn ghi lịch sử khi đã có bản ghi cùng filePath")
  void phucHoiGhiLichSuKhiDaCoBanGhiCungFilePath() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    Path tep = tempDir.resolve("dtaq_" + UUID.randomUUID() + ".dump");
    duongDanLichSu.add(tep.toString());
    BackupRecord banGhi = banGhiNguon(tep, "Bản gốc");
    // Bản ghi sao lưu nguồn đã chiếm filePath trước khi phục hồi.
    new LichSuSaoLuuDaoImpl(ConnectDB.getDataSource()).insert(banGhi);

    pg = pgGia();
    pg.datKetQuaRestore(new PgCommandExecutor.KetQua(0, "", false));
    BackupServiceGia backupService = new BackupServiceGia(store(), pg, daoGia());
    RestoreService service = new RestoreService(ConnectDB.getDataSource(), store(), pg, daoGia(),
        backupService);

    try {
      BackupRecord ketQua = service.phucHoi(banGhi, "NV-5", true);
      assertEquals(LoaiSaoLuu.MANUAL_RESTORE, ketQua.loai());
      // Phục hồi lặp lại cùng một tệp cũng phải ghi được thêm một bản ghi.
      service.phucHoi(banGhi, "NV-5", true);

      List<BackupRecord> tatCa = new LichSuSaoLuuDaoImpl(ConnectDB.getDataSource()).findAll();
      long soBanCungTep = tatCa.stream()
          .filter(item -> item.filePath().equals(tep))
          .count();
      long soBanPhucHoi = tatCa.stream()
          .filter(item -> item.filePath().equals(tep))
          .filter(item -> item.loai() == LoaiSaoLuu.MANUAL_RESTORE)
          .filter(BackupRecord::thanhCong)
          .count();
      assertEquals(3, soBanCungTep, "phải giữ bản ghi nguồn và hai bản ghi phục hồi");
      assertEquals(2, soBanPhucHoi, "phải ghi được nhiều bản MANUAL_RESTORE dù trùng filePath");
    } finally {
      xoaLichSu(tep.toString());
    }
  }

  @Test
  @DisplayName("overload ghi AUTO_RESTORE và khôi phục trạng thái ứng dụng sau khi lỗi")
  void overloadGhiAutoRestoreVaKhoiPhucTrangThai() {
    pg = pgGia();
    pg.datKetQuaRestore(new PgCommandExecutor.KetQua(1, "lỗi giả lập", false));
    BackupServiceGia backupService = new BackupServiceGia(store(), pg, daoGia());
    RestoreServiceGia service = taoService(backupService);
    AppState.set(AppState.State.SUSPICIOUS);

    BackupException loi = assertThrows(BackupException.class,
        () -> service.phucHoi(banGhiGia(), "SYSTEM", true, LoaiSaoLuu.AUTO_RESTORE));

    assertTrue(loi.getMessage().contains("lỗi giả lập"), loi.getMessage());
    assertEquals(1, dao.daGhi.size(), "phải ghi một bản lịch sử thất bại");
    assertEquals(LoaiSaoLuu.AUTO_RESTORE, dao.daGhi.get(0).loai());
    assertEquals(TrangThaiSaoLuu.FAILED, dao.daGhi.get(0).trangThai());
    assertEquals(AppState.State.MAINTENANCE, service.trangThaiKhiDongKetNoi,
        "phải chuyển sang MAINTENANCE trong lúc phục hồi");
    assertEquals(AppState.State.SUSPICIOUS, AppState.get(),
        "phải khôi phục trạng thái trước khi phục hồi");
    assertFalse(MaintenanceLock.isBusy(), "phải nhả khóa sau khi thất bại");
  }

  @Test
  @DisplayName("loai null được hiểu là MANUAL_RESTORE")
  void loaiNullThiDungManualRestore() {
    pg = pgGia();
    pg.datKetQuaRestore(new PgCommandExecutor.KetQua(1, "lỗi giả lập", false));
    BackupServiceGia backupService = new BackupServiceGia(store(), pg, daoGia());
    RestoreServiceGia service = taoService(backupService);

    assertThrows(BackupException.class,
        () -> service.phucHoi(banGhiGia(), "NV-6", true, null));

    assertEquals(1, dao.daGhi.size());
    assertEquals(LoaiSaoLuu.MANUAL_RESTORE, dao.daGhi.get(0).loai());
    assertEquals(AppState.State.NORMAL, AppState.get(),
        "trạng thái NORMAL phải được giữ nguyên");
  }

  private RestoreServiceGia taoService(BackupServiceGia backupService) {
    return new RestoreServiceGia(store(), pgGia(), daoGia(), backupService);
  }

  private BackupStore store() {
    if (store == null) {
      store = new BackupStore(tempDir.resolve("backups"), null);
    }
    return store;
  }

  private PgGia pgGia() {
    if (pg == null) {
      pg = new PgGia();
    }
    return pg;
  }

  private DaoGia daoGia() {
    if (dao == null) {
      dao = new DaoGia();
    }
    return dao;
  }

  private BackupRecord banGhiGia() {
    return new BackupRecord(null, tempDir.resolve("khong_ton_tai_" + UUID.randomUUID() + ".dump"),
        1024L, SHA256_GIA, LoaiSaoLuu.MANUAL, TrangThaiSaoLuu.SUCCESS, "Bản gốc", Instant.now(),
        "NV-0", "16.13", "dtaq_test");
  }

  private BackupRecord banGhiNguon(Path tep, String thongBao) {
    return new BackupRecord(null, tep, 1024L, SHA256_GIA, LoaiSaoLuu.MANUAL,
        TrangThaiSaoLuu.SUCCESS, thongBao, Instant.now(), "NV-0", "16.13", "dtaq_test");
  }

  private static void xoaLichSu(String filePath) throws SQLException {
    try (Connection connection = ConnectDB.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM \"LichSuSaoLuu\" WHERE \"filePath\" = ?")) {
      statement.setString(1, filePath);
      statement.executeUpdate();
    }
  }

  /**
   * Dịch vụ phục hồi giả: thay bốn móc kết nối bằng bản không chạm {@link ConnectDB}.
   *
   * <p>Nhờ vậy các nhánh thất bại chạy được cả khi không có PostgreSQL.
   */
  private static final class RestoreServiceGia extends RestoreService {

    private final DataSource nguonDuLieuGia = new NguonDuLieuGia();
    private boolean dongKetNoiDuocGoi;
    private boolean moLaiKetNoiDuocGoi;
    private AppState.State trangThaiKhiDongKetNoi;

    RestoreServiceGia(BackupStore store, PgCommandExecutor pg, LichSuSaoLuuDao dao,
        BackupService backupService) {
      super(new NguonDuLieuGia(), store, pg, dao, backupService);
    }

    @Override
    protected void dongKetNoi() {
      dongKetNoiDuocGoi = true;
      trangThaiKhiDongKetNoi = AppState.get();
    }

    @Override
    protected void moLaiKetNoi() {
      moLaiKetNoiDuocGoi = true;
    }

    @Override
    protected boolean khaDung() {
      return true;
    }

    @Override
    protected DataSource nguonDuLieuMoi() {
      return nguonDuLieuGia;
    }
  }

  /** Công cụ PostgreSQL giả: không chạy tiến trình nào, trả kết quả do bài kiểm thử đặt. */
  private static final class PgGia extends PgCommandExecutor {

    private PgCommandExecutor.KetQua ketQuaRestore = new PgCommandExecutor.KetQua(0, "", false);
    private int soLanRestore;
    private int soLanTaoCsdl;

    PgGia() {
      super(Path.of("pg_dump"), Path.of("pg_restore"), Path.of("createdb"),
          "localhost", 5432, "dtaq_test", "dtaq", "");
    }

    void datKetQuaRestore(PgCommandExecutor.KetQua ketQua) {
      this.ketQuaRestore = ketQua;
    }

    @Override
    public PgCommandExecutor.KetQua dump(Path output) {
      return new PgCommandExecutor.KetQua(0, "", false);
    }

    @Override
    public PgCommandExecutor.KetQua restore(Path dump) {
      soLanRestore++;
      return ketQuaRestore;
    }

    @Override
    public PgCommandExecutor.KetQua createDatabase() {
      soLanTaoCsdl++;
      return new PgCommandExecutor.KetQua(0, "", false);
    }

    @Override
    public PgCommandExecutor.KetQua listContents(Path dump) {
      return new PgCommandExecutor.KetQua(0, "; nội dung giả lập", false);
    }

    @Override
    public PgCommandExecutor.KetQua phienBan() {
      return new PgCommandExecutor.KetQua(0, "pg_dump (PostgreSQL) 16.13", false);
    }

    @Override
    public String database() {
      return "dtaq_test";
    }
  }

  /** Dịch vụ sao lưu giả: bỏ qua kiểm tra tệp và không chạy {@code pg_dump}. */
  private static final class BackupServiceGia extends BackupService {

    private boolean kiemTraDuocGoi;
    private boolean saoLuuDuocGoi;
    private RuntimeException loiSaoLuu;
    private final BackupRecord banSaoAnToan;

    BackupServiceGia(BackupStore store, PgCommandExecutor pg, LichSuSaoLuuDao dao) {
      super(new NguonDuLieuGia(), store, pg, dao);
      this.banSaoAnToan = new BackupRecord(null, Path.of("backups", "dtaq_safety.dump"), 1L,
          SHA256_GIA, LoaiSaoLuu.SAFETY, TrangThaiSaoLuu.SUCCESS, "Sao lưu an toàn", Instant.now(),
          "NV-0", "16.13", "dtaq_test");
    }

    @Override
    public void kiemTra(BackupRecord banGhi) {
      kiemTraDuocGoi = true;
    }

    @Override
    public BackupRecord saoLuu(LoaiSaoLuu loai, String nguoiThucHien) {
      saoLuuDuocGoi = true;
      if (loiSaoLuu != null) {
        throw loiSaoLuu;
      }
      return banSaoAnToan;
    }
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
