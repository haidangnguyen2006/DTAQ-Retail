package com.dtaq.retail.controller;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.dao.impl.HealthIncidentDaoImpl;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.service.AppState;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.DataHealthChecker;
import com.dtaq.retail.service.HealthStateStore;
import com.dtaq.retail.service.HealthStateStore.HealthState;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.session.CurrentSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Điều phối việc xử lý sự cố sức khỏe dữ liệu do HealthMonitor phát hiện.
 *
 * <p>Khi HealthMonitor tạo một sự cố {@code PENDING}, Quản lý có bốn lựa chọn: phục hồi từ một bản
 * sao lưu, chọn bản khác ở trang Phục hồi, bỏ qua sự cố khi dữ liệu đã hợp lệ, hoặc để sau. Mọi
 * phương thức công khai đều chặn quyền trước khi chạm tới dịch vụ: chỉ Nhân viên quản lý mới được
 * xử lý sự cố (NFR003, XBR-13).
 *
 * <p>Lớp này chỉ chuyển tiếp yêu cầu. Việc kiểm tra tệp, tạo bản sao lưu an toàn và ghi đè cơ sở dữ
 * liệu thuộc về {@link RestoreService}; việc đọc/ghi trạng thái giám sát thuộc về
 * {@link HealthStateStore}. Không có SQL hay thao tác nặng trong lớp này, nên bên gọi vẫn phải chạy
 * nó ngoài luồng giao diện.
 */
public final class IncidentController {

  private static final String LOI_QUYEN =
      "Chức năng này chỉ dành cho nhân viên quản lý";

  private final HealthIncidentDao incidentDao;
  private final BackupService backupService;
  private final RestoreService restoreService;
  private final DataHealthChecker checker;
  private final HealthStateStore stateStore;

  /**
   * Khởi tạo bộ điều phối sự cố sức khỏe.
   *
   * @param incidentDao truy cập bảng {@code "SuCoSucKhoe"}; không được {@code null}
   * @param backupService dịch vụ sao lưu; không được {@code null}
   * @param restoreService dịch vụ phục hồi; không được {@code null}
   * @param checker bộ kiểm tra sức khỏe dữ liệu; không được {@code null}
   * @param stateStore kho trạng thái giám sát; không được {@code null}
   */
  public IncidentController(
      HealthIncidentDao incidentDao,
      BackupService backupService,
      RestoreService restoreService,
      DataHealthChecker checker,
      HealthStateStore stateStore) {
    this.incidentDao = Objects.requireNonNull(incidentDao, "incidentDao");
    this.backupService = Objects.requireNonNull(backupService, "backupService");
    this.restoreService = Objects.requireNonNull(restoreService, "restoreService");
    this.checker = Objects.requireNonNull(checker, "checker");
    this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
  }

  /**
   * Dựng bộ điều phối theo cấu hình và bể kết nối mặc định của ứng dụng.
   *
   * <p>DAO sự cố và bộ kiểm tra lấy nguồn dữ liệu qua {@link ConnectDB} ở mỗi thao tác để vẫn dùng
   * được sau khi {@link RestoreService} gọi {@link ConnectDB#reset()} và dựng bể kết nối mới.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   * @return bộ điều phối dùng tệp trạng thái, kho sao lưu và công cụ PostgreSQL theo cấu hình
   */
  public static IncidentController macDinh(DataSource dataSource) {
    HealthStateStore store = new HealthStateStore(HealthConfig.tepTrangThai());
    BackupService backup = BackupService.macDinh(dataSource);
    return new IncidentController(
        new HealthIncidentDaoImpl(ConnectDB::getDataSource),
        backup,
        RestoreService.macDinh(dataSource, backup),
        new DataHealthChecker(ConnectDB::getDataSource, store),
        store);
  }

  /**
   * Lấy sự cố {@code PENDING} mới nhất đang chờ Quản lý xử lý.
   *
   * @return sự cố đang chờ, hoặc rỗng khi không còn sự cố nào
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   */
  public Optional<HealthIncident> suCoDangCho() {
    yeuCauQuanLy();
    return incidentDao.findMoiNhatDangCho();
  }

  /**
   * Liệt kê các bản sao lưu hiện có trên đĩa, mới nhất trước.
   *
   * @return danh sách bản ghi, có thể rỗng
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   */
  public List<BackupRecord> danhSachBackup() {
    yeuCauQuanLy();
    return backupService.danhSach();
  }

  /**
   * Phục hồi cơ sở dữ liệu từ một bản sao lưu và kết thúc sự cố đang chờ.
   *
   * <p>Phục hồi là thao tác phá hủy: dữ liệu sau mốc sao lưu sẽ mất. Lời gọi này chỉ được thực hiện
   * sau khi người dùng hoàn tất xác nhận hai bước ở giao diện; dịch vụ phục hồi nhận
   * {@code daXacNhan = true} và tự tạo một bản sao lưu an toàn trước khi ghi đè.
   *
   * @param incident sự cố cần kết thúc; không được {@code null}
   * @param backup bản sao lưu nguồn; không được {@code null}
   * @return bản ghi lịch sử {@code MANUAL_RESTORE} thành công
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   * @throws com.dtaq.retail.exception.BackupException khi phục hồi thất bại
   */
  public BackupRecord phucHoi(HealthIncident incident, BackupRecord backup) {
    NhanVien nv = yeuCauQuanLy();
    Objects.requireNonNull(incident, "incident");
    Objects.requireNonNull(backup, "backup");
    BackupRecord ketQua = restoreService.phucHoi(
        backup, nv.maNV(), true, BackupRecord.LoaiSaoLuu.MANUAL_RESTORE);
    incidentDao.capNhatKetThuc(
        incident.id(),
        HealthIncident.TrangThai.RESOLVED_RESTORED,
        Instant.now(),
        nv.maNV(),
        backup.fileName());
    AppState.set(AppState.State.NORMAL);
    return ketQua;
  }

  /**
   * Bỏ qua sự cố khi Quản lý xác nhận dữ liệu hiện tại là hợp lệ.
   *
   * <p>Phương thức kiểm tra lại dữ liệu ngay tại thời điểm bấm nút. Nếu dữ liệu vẫn
   * {@link HealthStatus#CRITICAL} hoặc không đánh giá được ({@link HealthStatus#UNAVAILABLE}) thì
   * không được bỏ qua. Khi dữ liệu đã dùng được, baseline được cập nhật theo số dòng hiện tại, chuỗi
   * bất thường và sự cố đang mở bị xóa để HealthMonitor và sao lưu tự động chạy lại từ mốc mới.
   *
   * @param incident sự cố cần bỏ qua; không được {@code null}
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   * @throws BackupException khi dữ liệu chưa sẵn sàng nên không thể bỏ qua
   */
  public void boQuaHopLe(HealthIncident incident) {
    NhanVien nv = yeuCauQuanLy();
    Objects.requireNonNull(incident, "incident");
    HealthReport report = checker.kiemTra();
    if (report.trangThai() == HealthStatus.CRITICAL
        || report.trangThai() == HealthStatus.UNAVAILABLE) {
      throw new BackupException("Không thể bỏ qua khi dữ liệu chưa sẵn sàng");
    }
    HealthState state = stateStore.doc();
    stateStore.ghi(state
        .withBaseline(report.soDong())
        .withStreak(Map.of())
        .withOpenIncident(null)
        .withLastStatus("HEALTHY"));
    incidentDao.capNhatKetThuc(
        incident.id(),
        HealthIncident.TrangThai.DISMISSED,
        Instant.now(),
        nv.maNV(),
        null);
    AppState.set(AppState.State.NORMAL);
  }

  /**
   * Để Quản lý xử lý sự cố sau.
   *
   * <p>Chỉ ghi nhận thời điểm nhắc gần nhất để không hiện lại hộp thoại ngay; sự cố vẫn giữ trạng
   * thái {@code PENDING} cho tới khi được phục hồi hoặc bỏ qua.
   *
   * @param incident sự cố cần để sau; không được {@code null}
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   */
  public void deSau(HealthIncident incident) {
    yeuCauQuanLy();
    Objects.requireNonNull(incident, "incident");
    HealthState state = stateStore.doc();
    stateStore.ghi(state.withLastNotifiedAt(Instant.now()));
  }

  /**
   * Bắt buộc phiên đăng nhập hiện tại là Nhân viên quản lý.
   *
   * @return nhân viên quản lý đang đăng nhập
   * @throws SecurityException khi chưa đăng nhập hoặc tài khoản không phải quản lý
   */
  private static NhanVien yeuCauQuanLy() {
    return CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElseThrow(() -> new SecurityException(LOI_QUYEN));
  }
}
