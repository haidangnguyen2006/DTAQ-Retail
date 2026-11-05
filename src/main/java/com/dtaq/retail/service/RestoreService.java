package com.dtaq.retail.service;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.dao.impl.LichSuSaoLuuDaoImpl;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.util.PgCommandExecutor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phục hồi toàn bộ cơ sở dữ liệu từ một bản sao lưu đã được kiểm tra.
 *
 * <p>Trình tự bắt buộc: xác nhận hai bước, giành {@link MaintenanceLock}, kiểm tra tệp, tạo bản sao
 * lưu an toàn, ngắt các kết nối khác, đóng bể kết nối, chạy {@code pg_restore --single-transaction},
 * mở lại bể kết nối, kiểm tra đủ bảng cốt lõi rồi ghi lịch sử. Mọi thao tác nặng chạy ngoài luồng
 * giao diện; lớp này không phụ thuộc Swing.
 *
 * <p>Phục hồi là thao tác phá hủy: dữ liệu sau mốc sao lưu sẽ mất. Vì vậy dịch vụ không bao giờ tự
 * chạy khi thiếu xác nhận của người dùng.
 *
 * <p>Các thao tác trên bể kết nối dùng chung đi qua bốn móc {@code protected} để kiểm thử thay được
 * mà không mở kết nối thật; hành vi mặc định giữ nguyên như {@link ConnectDB}.
 */
public class RestoreService {

  private static final Logger log = LoggerFactory.getLogger(RestoreService.class);

  /** Thời gian chờ tối đa để giành khóa bảo trì. */
  private static final Duration CHO_KHOA = Duration.ofSeconds(5);

  /** Độ dài tối đa của thông điệp lấy từ đầu ra tiến trình hoặc ngoại lệ. */
  private static final int GIOI_HAN_THONG_BAO = 500;

  /** Các bảng cốt lõi bắt buộc phải có sau khi phục hồi. */
  private static final List<String> BANG_COT_LOI = List.of(
      "NhanVien",
      "KhachHang",
      "DanhMucSanPham",
      "NhaCungCap",
      "DongSanPham",
      "SanPham",
      "KhuyenMai",
      "HoaDon",
      "ChiTietHoaDon",
      "PhieuTra",
      "ChiTietPhieuTra",
      "PhieuDoi",
      "ChiTietPhieuDoi",
      "SanPhamLoi");

  private static final String SQL_KIEM_TRA_BANG =
      "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public'"
          + " AND table_name = ?";

  private final BackupStore store;
  private final PgCommandExecutor pg;
  private final LichSuSaoLuuDao dao;
  private final BackupService backupService;

  /**
   * Khởi tạo dịch vụ phục hồi.
   *
   * <p>Tham số {@code dataSource} chỉ còn được kiểm tra khác {@code null} để giữ tương thích chữ
   * ký: mọi truy cập cơ sở dữ liệu sau phục hồi đều đi qua {@link #nguonDuLieuMoi()} hoặc DAO lịch
   * sử do bên gọi cung cấp, vì nguồn được tiêm vào đây có thể trỏ tới bể kết nối đã bị
   * {@link ConnectDB#reset()} đóng lại.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   * @param store kho tệp sao lưu; không được {@code null}
   * @param pg bộ gọi công cụ PostgreSQL; không được {@code null}
   * @param dao truy cập lịch sử sao lưu; không được {@code null}
   * @param backupService dịch vụ kiểm tra và tạo bản sao lưu an toàn; không được {@code null}
   */
  public RestoreService(DataSource dataSource, BackupStore store, PgCommandExecutor pg,
      LichSuSaoLuuDao dao, BackupService backupService) {
    Objects.requireNonNull(dataSource, "dataSource");
    this.store = Objects.requireNonNull(store, "store");
    this.pg = Objects.requireNonNull(pg, "pg");
    this.dao = Objects.requireNonNull(dao, "dao");
    this.backupService = Objects.requireNonNull(backupService, "backupService");
  }

  /**
   * Dựng dịch vụ theo cấu hình ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   * @param backupService dịch vụ sao lưu đã cấu hình; không được {@code null}
   * @return dịch vụ dùng chung kho tệp, công cụ PostgreSQL và DAO lịch sử của dịch vụ sao lưu
   */
  public static RestoreService macDinh(DataSource dataSource, BackupService backupService) {
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(backupService, "backupService");
    return new RestoreService(dataSource, backupService.store(), backupService.pg(),
        backupService.dao(), backupService);
  }

  /**
   * Ngắt các kết nối khác rồi đóng bể kết nối dùng chung trước khi ghi đè cơ sở dữ liệu.
   *
   * <p>Móc dành cho lớp con trong kiểm thử; mặc định ủy quyền cho {@link ConnectDB}.
   */
  protected void dongKetNoi() {
    ConnectDB.terminateOtherConnections();
    ConnectDB.close();
  }

  /** Mở lại bể kết nối dùng chung sau khi phục hồi xong; mặc định ủy quyền cho {@link ConnectDB}. */
  protected void moLaiKetNoi() {
    ConnectDB.reset();
  }

  /**
   * Kiểm tra cơ sở dữ liệu đã sẵn sàng sau khi mở lại bể kết nối hay chưa.
   *
   * @return {@code true} khi mở được kết nối; mặc định ủy quyền cho {@link ConnectDB}
   */
  protected boolean khaDung() {
    return ConnectDB.isAvailable();
  }

  /**
   * Nguồn dữ liệu hiện tại dùng cho kiểm tra sau phục hồi và ghi lịch sử.
   *
   * <p>Không dùng nguồn dữ liệu được tiêm vào constructor vì nó trỏ tới bể kết nối đã đóng.
   *
   * @return nguồn dữ liệu hiện tại; mặc định ủy quyền cho {@link ConnectDB}
   */
  protected DataSource nguonDuLieuMoi() {
    return ConnectDB.getDataSource();
  }

  /**
   * Phục hồi toàn bộ cơ sở dữ liệu từ bản sao lưu đã chọn.
   *
   * <p>Trước khi ghi, dịch vụ luôn tạo một bản sao lưu {@code SAFETY} để có thể quay lại trạng thái
   * hiện tại. Bản sao lưu an toàn thất bại chỉ được ghi cảnh báo vì cơ sở dữ liệu có thể đang hỏng.
   *
   * @param banGhi bản sao lưu nguồn; không được {@code null}
   * @param nguoiThucHien người thực hiện, dùng cho lịch sử
   * @param daXacNhan {@code true} khi người dùng đã xác nhận hai bước
   * @return bản ghi lịch sử {@code MANUAL_RESTORE} thành công vừa tạo
   * @throws BackupException khi thiếu xác nhận, hệ thống đang bận, tệp không hợp lệ, phục hồi lỗi,
   *     không kết nối lại được cơ sở dữ liệu hoặc dữ liệu phục hồi thiếu bảng cốt lõi
   */
  public BackupRecord phucHoi(BackupRecord banGhi, String nguoiThucHien, boolean daXacNhan) {
    return phucHoi(banGhi, nguoiThucHien, daXacNhan, LoaiSaoLuu.MANUAL_RESTORE);
  }

  /**
   * Phục hồi toàn bộ cơ sở dữ liệu và ghi lịch sử theo loại thao tác được chọn.
   *
   * <p>Dùng cho cả phục hồi thủ công ({@code MANUAL_RESTORE}) và tự động ({@code AUTO_RESTORE}).
   * Trong suốt thời gian ghi đè, {@link AppState} được đặt thành {@link AppState.State#MAINTENANCE}
   * để các tác vụ nền khác không chạy; trạng thái trước đó được khôi phục ở khối {@code finally}.
   *
   * @param banGhi bản sao lưu nguồn; không được {@code null}
   * @param nguoiThucHien người thực hiện, dùng cho lịch sử
   * @param daXacNhan {@code true} khi đã xác nhận hai bước hoặc hệ thống tự quyết định phục hồi
   * @param loai loại thao tác ghi vào lịch sử; {@code null} được hiểu là
   *     {@link LoaiSaoLuu#MANUAL_RESTORE}
   * @return bản ghi lịch sử thành công vừa tạo
   * @throws BackupException khi thiếu xác nhận, hệ thống đang bận, tệp không hợp lệ, phục hồi lỗi,
   *     không kết nối lại được cơ sở dữ liệu hoặc dữ liệu phục hồi thiếu bảng cốt lõi
   */
  public BackupRecord phucHoi(BackupRecord banGhi, String nguoiThucHien, boolean daXacNhan,
      LoaiSaoLuu loai) {
    if (!daXacNhan) {
      throw new BackupException("Cần xác nhận 2 bước trước khi phục hồi dữ liệu");
    }
    Objects.requireNonNull(banGhi, "banGhi");
    LoaiSaoLuu loaiGhi = loai == null ? LoaiSaoLuu.MANUAL_RESTORE : loai;
    if (!giuKhoa()) {
      throw new BackupException("Hệ thống đang bận, vui lòng thử lại sau");
    }
    AppState.State trangThaiTruoc = AppState.get();
    try {
      log.info("Bắt đầu phục hồi cơ sở dữ liệu từ {} (kho sao lưu {})", banGhi.fileName(),
          store.thuMuc().toAbsolutePath());
      backupService.kiemTra(banGhi);
      AppState.set(AppState.State.MAINTENANCE);
      saoLuuAnToan(nguoiThucHien);

      try {
        dongKetNoi();
        PgCommandExecutor.KetQua ketQua = pg.restore(banGhi.filePath());
        if (!ketQua.thanhCong() && laLoiKhongTonTai(ketQua)) {
          log.warn("Cơ sở dữ liệu đích không tồn tại; tạo lại rồi phục hồi lần hai");
          PgCommandExecutor.KetQua taoCsdl = pg.createDatabase();
          if (!taoCsdl.thanhCong()) {
            log.warn("Không tạo được cơ sở dữ liệu đích: {}", rutGon(taoCsdl.output()));
          }
          ketQua = pg.restore(banGhi.filePath());
        }
        if (!ketQua.thanhCong()) {
          throw new BackupException("Phục hồi thất bại: " + moTaKetQua(ketQua));
        }
      } finally {
        moLaiKetNoi();
      }

      if (!khaDung()) {
        throw new BackupException(
            "Phục hồi xong nhưng không kết nối lại được cơ sở dữ liệu");
      }
      kiemTraSauPhucHoi();

      BackupRecord ketQua = ghiLichSuThanhCong(banGhi, nguoiThucHien, loaiGhi);
      log.info("Đã phục hồi cơ sở dữ liệu từ {}", banGhi.fileName());
      return ketQua;
    } catch (RuntimeException e) {
      ghiLichSuThatBai(banGhi, nguoiThucHien, moTaNgoaiLe(e), loaiGhi);
      throw e;
    } finally {
      khoiPhucTrangThai(trangThaiTruoc);
      MaintenanceLock.release();
    }
  }

  /** Khôi phục trạng thái ứng dụng trước khi phục hồi; bảo trì không bao giờ là trạng thái cuối. */
  private static void khoiPhucTrangThai(AppState.State trangThaiTruoc) {
    AppState.set(trangThaiTruoc == AppState.State.MAINTENANCE
        ? AppState.State.NORMAL
        : trangThaiTruoc);
  }

  private boolean giuKhoa() {
    try {
      return MaintenanceLock.tryAcquire(CHO_KHOA);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BackupException("Đã bị ngắt khi chờ khóa phục hồi dữ liệu", e);
    }
  }

  private void saoLuuAnToan(String nguoiThucHien) {
    try {
      BackupRecord anToan = backupService.saoLuu(LoaiSaoLuu.SAFETY, nguoiThucHien);
      log.info("Đã tạo bản sao lưu an toàn {}", anToan.fileName());
    } catch (RuntimeException e) {
      log.warn("Không tạo được bản sao lưu an toàn trước khi phục hồi: {}", e.getMessage());
    }
  }

  private void kiemTraSauPhucHoi() {
    List<String> thieu = new ArrayList<>();
    try (Connection connection = nguonDuLieuMoi().getConnection();
        PreparedStatement statement = connection.prepareStatement(SQL_KIEM_TRA_BANG)) {
      for (String bang : BANG_COT_LOI) {
        statement.setString(1, bang);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            thieu.add(bang);
          }
        }
      }
    } catch (SQLException e) {
      throw new BackupException(
          "Không kiểm tra được dữ liệu sau phục hồi: " + e.getMessage(), e);
    }
    if (!thieu.isEmpty()) {
      throw new BackupException("Dữ liệu phục hồi thiếu bảng: " + String.join(", ", thieu));
    }
  }

  private BackupRecord ghiLichSuThanhCong(BackupRecord banGhi, String nguoiThucHien,
      LoaiSaoLuu loai) {
    BackupRecord ketQua = new BackupRecord(
        null,
        banGhi.filePath(),
        banGhi.fileSize(),
        banGhi.sha256(),
        loai,
        TrangThaiSaoLuu.SUCCESS,
        "Phục hồi từ " + banGhi.fileName(),
        Instant.now(),
        nguoiThucHien,
        banGhi.phienBanPg(),
        banGhi.tenCsdl());
    try {
      // Luôn dựng DAO từ bể kết nối hiện tại: bể đã được reset sau phục hồi, còn DAO được tiêm có
      // thể là bản giả trong kiểm thử.
      new LichSuSaoLuuDaoImpl(nguonDuLieuMoi()).insert(ketQua);
    } catch (RuntimeException e) {
      log.warn("Không ghi được lịch sử phục hồi {}: {}", banGhi.fileName(), e.getMessage());
    }
    return ketQua;
  }

  private void ghiLichSuThatBai(BackupRecord banGhi, String nguoiThucHien, String thongBao,
      LoaiSaoLuu loai) {
    BackupRecord thatBai = new BackupRecord(
        null,
        banGhi.filePath(),
        banGhi.fileSize(),
        banGhi.sha256(),
        loai,
        TrangThaiSaoLuu.FAILED,
        thongBao,
        Instant.now(),
        nguoiThucHien,
        banGhi.phienBanPg(),
        banGhi.tenCsdl());
    try {
      dao.insert(thatBai);
      return;
    } catch (RuntimeException e) {
      log.warn("Không ghi được lịch sử phục hồi thất bại bằng DAO hiện tại: {}", e.getMessage());
    }
    try {
      moLaiKetNoi();
      // Luôn dựng DAO từ nguồn dữ liệu hiện tại: nguồn được tiêm vào constructor có thể đã bị
      // ConnectDB.reset() đóng lại sau phục hồi.
      new LichSuSaoLuuDaoImpl(nguonDuLieuMoi()).insert(thatBai);
    } catch (RuntimeException e) {
      log.warn("Không ghi được lịch sử phục hồi thất bại: {}", e.getMessage());
    }
  }

  private static boolean laLoiKhongTonTai(PgCommandExecutor.KetQua ketQua) {
    String output = ketQua.output();
    if (output == null) {
      return false;
    }
    String chuan = output.toLowerCase(Locale.ROOT);
    return chuan.contains("does not exist") || chuan.contains("không tồn tại");
  }

  private static String moTaKetQua(PgCommandExecutor.KetQua ketQua) {
    if (ketQua.timeout()) {
      return "quá thời gian chờ pg_restore";
    }
    String output = rutGon(ketQua.output());
    return output.isBlank()
        ? "pg_restore kết thúc với mã thoát " + ketQua.exitCode()
        : output;
  }

  private static String moTaNgoaiLe(RuntimeException e) {
    String thongBao = e.getMessage();
    if (thongBao == null || thongBao.isBlank()) {
      thongBao = e.getClass().getSimpleName();
    }
    return rutGon(thongBao);
  }

  private static String rutGon(String output) {
    if (output == null) {
      return "";
    }
    String chuan = output.strip();
    return chuan.length() <= GIOI_HAN_THONG_BAO
        ? chuan
        : chuan.substring(0, GIOI_HAN_THONG_BAO);
  }
}
