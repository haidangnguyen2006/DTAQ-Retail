package com.dtaq.retail.service;

import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthIncident.Severity;
import com.dtaq.retail.entity.HealthIncident.TrangThai;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.notify.NotificationService;
import com.dtaq.retail.service.HealthStateStore.HealthState;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Áp chính sách phục hồi sau mỗi chu kỳ kiểm tra sức khỏe dữ liệu.
 *
 * <p>Bốn trạng thái được xử lý khác nhau:
 *
 * <ul>
 *   <li>{@link HealthStatus#HEALTHY}: cập nhật baseline, xóa chuỗi bất thường và đóng sự cố còn mở.
 *   <li>{@link HealthStatus#SUSPICIOUS}: tăng chuỗi đếm theo bảng và chỉ tạo sự cố chờ Quản lý khi
 *       vượt đủ số chu kỳ xác nhận; không bao giờ tự phục hồi.
 *   <li>{@link HealthStatus#CRITICAL}: tạo sự cố, rồi thử tự động phục hồi từ các bản sao lưu
 *       hợp lệ mới nhất trong giới hạn tần suất.
 *   <li>{@link HealthStatus#UNAVAILABLE}: chỉ ghi trạng thái phụ và cảnh báo, không phục hồi.
 * </ul>
 *
 * <p>Mọi ngoại lệ đều bị chặn trong {@link #xuLy(HealthReport)}: một chu kỳ lỗi không được làm dừng
 * luồng giám sát. Lần thử tự động phục hồi được ghi xuống {@link HealthStateStore} <em>trước</em>
 * khi gọi phục hồi để giới hạn tần suất vẫn đúng kể cả khi tiến trình chết giữa chừng.
 *
 * <p>Giới hạn tần suất không chỉ dựa vào tệp trạng thái: các lần thử còn được giữ trong bộ nhớ của
 * tiến trình, khởi tạo từ tệp và hợp nhất với tệp ở mỗi chu kỳ. Nhờ vậy khi {@link
 * HealthStateStore#ghi} không ghi được (thư mục sao lưu bị xóa, ổ đĩa chỉ đọc...), giới hạn vẫn có
 * hiệu lực trong suốt vòng đời tiến trình.
 *
 * <p>Bảng {@code "SuCoSucKhoe"} nằm trong chính cơ sở dữ liệu đang được đánh giá nên có thể không
 * truy cập được đúng lúc dữ liệu hỏng. Việc ghi sự cố vì thế chỉ là nỗ lực tối đa: khi ghi thất bại,
 * chính sách phục hồi vẫn chạy tiếp và chỉ bỏ qua bước cập nhật sự cố.
 */
public class RecoveryCoordinator {

  private static final Logger log = LoggerFactory.getLogger(RecoveryCoordinator.class);

  /** Người hoặc tiến trình kết thúc sự cố do hệ thống tự xử lý. */
  private static final String NGUOI_HE_THONG = "SYSTEM";

  private final HealthStateStore store;
  private final HealthIncidentDao incidentDao;
  private final BackupService backupService;
  private final RestoreService restoreService;

  /** Các lần tự động phục hồi của tiến trình hiện tại, dùng khi tệp trạng thái không ghi được. */
  private final List<Instant> autoRestoreAttempts = new CopyOnWriteArrayList<>();

  /**
   * Khởi tạo bộ điều phối.
   *
   * @param store kho trạng thái ngoài cơ sở dữ liệu; không được {@code null}
   * @param incidentDao truy cập bảng sự cố; không được {@code null}
   * @param backupService dịch vụ liệt kê và kiểm tra bản sao lưu; không được {@code null}
   * @param restoreService dịch vụ phục hồi dữ liệu; không được {@code null}
   */
  public RecoveryCoordinator(HealthStateStore store, HealthIncidentDao incidentDao,
      BackupService backupService, RestoreService restoreService) {
    this.store = Objects.requireNonNull(store, "store");
    this.incidentDao = Objects.requireNonNull(incidentDao, "incidentDao");
    this.backupService = Objects.requireNonNull(backupService, "backupService");
    this.restoreService = Objects.requireNonNull(restoreService, "restoreService");
    try {
      autoRestoreAttempts.addAll(this.store.doc().autoRestoreAttempts());
    } catch (RuntimeException e) {
      log.warn("Không đọc được các lần tự động phục hồi đã lưu: {}", e.getMessage());
    }
  }

  /**
   * Xử lý một báo cáo sức khỏe theo chính sách đã duyệt.
   *
   * <p>Phương thức không bao giờ ném ngoại lệ; lỗi được ghi log và trạng thái ứng dụng giữ nguyên
   * giá trị đặt được gần nhất.
   *
   * @param report báo cáo của chu kỳ kiểm tra; không được {@code null}
   */
  public void xuLy(HealthReport report) {
    Objects.requireNonNull(report, "report");
    try {
      Instant now = Instant.now();
      HealthState state = store.doc();
      switch (report.trangThai()) {
        case HEALTHY -> xuLyHealthy(report, state, now);
        case SUSPICIOUS -> xuLySuspicious(report, state, now);
        case CRITICAL -> xuLyCritical(report, state, now);
        case UNAVAILABLE -> xuLyUnavailable(report, state, now);
      }
    } catch (RuntimeException e) {
      log.error("Lỗi khi xử lý báo cáo sức khỏe {}: {}", report.trangThai(), e.getMessage(), e);
    }
  }

  private void xuLyHealthy(HealthReport report, HealthState state, Instant now) {
    store.ghi(state.withBaseline(report.soDong()).withStreak(Map.of()).withOpenIncident(null)
        .withLastStatus("HEALTHY"));
    if (state.openIncidentId() != null) {
      ketThucIncident(state.openIncidentId(), TrangThai.DISMISSED, now, null);
    }
    AppState.set(AppState.State.NORMAL);
  }

  private void xuLySuspicious(HealthReport report, HealthState state, Instant now) {
    Map<String, Integer> streak = new LinkedHashMap<>(state.suspiciousStreak());
    for (String bang : report.tyLeGiam().keySet()) {
      streak.merge(bang, 1, Integer::sum);
    }
    for (String bang : report.soDong().keySet()) {
      if (!report.tyLeGiam().containsKey(bang)) {
        streak.put(bang, 0);
      }
    }

    int soChuKy = HealthConfig.soChuKyXacNhan();
    boolean duChuKy = streak.values().stream()
        .anyMatch(giaTri -> HealthPolicy.duChuKy(giaTri, soChuKy));
    if (!duChuKy) {
      store.ghi(state.withStreak(streak).withLastStatus("SUSPICIOUS"));
      return;
    }

    Long idSuCo = state.openIncidentId();
    if (idSuCo == null) {
      idSuCo = taoSuCo(Severity.SUSPICIOUS, report.chiTiet(), now);
    }
    AppState.set(AppState.State.SUSPICIOUS);

    Instant lanThongBao = state.lastNotifiedAt();
    if (denHanNhacLai(lanThongBao, now)) {
      NotificationService.thongBao("Dữ liệu bất thường", report.chiTiet());
      lanThongBao = now;
    }

    store.ghi(state.withStreak(streak).withOpenIncident(idSuCo)
        .withLastNotifiedAt(lanThongBao).withLastStatus("SUSPICIOUS"));
  }

  private void xuLyCritical(HealthReport report, HealthState state, Instant now) {
    Long idSuCo = state.openIncidentId();
    if (idSuCo == null) {
      idSuCo = taoSuCo(Severity.CRITICAL, report.chiTiet(), now);
    }

    if (!HealthConfig.autoRestoreBatTat()) {
      log.warn("Dữ liệu nghiêm trọng nhưng tự động phục hồi đang tắt: {}", report.chiTiet());
      NotificationService.thongBao("Dữ liệu nghiêm trọng", report.chiTiet());
      AppState.set(AppState.State.SUSPICIOUS);
      store.ghi(state.withOpenIncident(idSuCo).withLastStatus("CRITICAL"));
      return;
    }

    List<Instant> lanThu = lanThuDaBiet(state);
    if (!HealthPolicy.duocAutoRestore(lanThu, now, HealthConfig.autoRestoreToiDaMoiGio())) {
      log.warn("Đã vượt giới hạn tự động phục hồi trong một giờ");
      ketThucIncident(idSuCo, TrangThai.FAILED, now, null);
      NotificationService.thongBao("Dữ liệu nghiêm trọng", report.chiTiet());
      AppState.set(AppState.State.SUSPICIOUS);
      store.ghi(state.withOpenIncident(null).withLastStatus("CRITICAL"));
      return;
    }

    List<BackupRecord> ungVien = timBanDuPhong();
    if (ungVien.isEmpty()) {
      log.warn("Không có bản sao lưu hợp lệ nào để tự động phục hồi");
      ketThucIncident(idSuCo, TrangThai.FAILED, now, null);
      NotificationService.thongBao("Dữ liệu nghiêm trọng",
          "Không có bản sao lưu hợp lệ để tự động phục hồi. " + moTa(report));
      AppState.set(AppState.State.SUSPICIOUS);
      store.ghi(state.withOpenIncident(null).withLastStatus("CRITICAL"));
      return;
    }

    for (BackupRecord banGhi : ungVien) {
      // Mỗi lần thử dùng một mốc riêng, tăng dần, để danh sách gộp theo tập không nuốt mất lượt
      // thử: đồng hồ (nhất là trên Windows) có thể trả cùng một Instant cho hai lần gọi liên tiếp.
      Instant thoiDiemThu = Instant.now();
      Instant cuoi = lanThu.isEmpty() ? null : lanThu.get(lanThu.size() - 1);
      if (cuoi != null && !thoiDiemThu.isAfter(cuoi)) {
        thoiDiemThu = cuoi.plusNanos(1);
      }
      lanThu.add(thoiDiemThu);
      // Ghi vào bộ nhớ trước để giới hạn tần suất vẫn đúng khi tệp trạng thái không ghi được.
      autoRestoreAttempts.add(thoiDiemThu);
      // Ghi lượt thử trước khi phục hồi để giới hạn tần suất vẫn đúng nếu tiến trình chết
      // giữa chừng.
      store.ghi(state.withOpenIncident(idSuCo).withAutoRestoreAttempts(lanThu)
          .withLastStatus("CRITICAL"));
      try {
        restoreService.phucHoi(banGhi, NGUOI_HE_THONG, true,
            BackupRecord.LoaiSaoLuu.AUTO_RESTORE);
        ketThucIncident(idSuCo, TrangThai.AUTO_RESTORED, now, banGhi.fileName());
        if (idSuCo == null) {
          ghiSuCoDaPhucHoi(report.chiTiet(), banGhi.fileName(), now);
        }
        AppState.set(AppState.State.NORMAL);
        NotificationService.thongBao("Đã tự động phục hồi",
            "Đã tự động phục hồi từ " + banGhi.fileName());
        store.ghi(state.withOpenIncident(null).withAutoRestoreAttempts(lanThu)
            .withLastStatus("HEALTHY"));
        log.info("Đã tự động phục hồi cơ sở dữ liệu từ {}", banGhi.fileName());
        return;
      } catch (RuntimeException e) {
        log.warn("Tự động phục hồi từ {} thất bại: {}", banGhi.fileName(), e.getMessage());
      }
    }

    ketThucIncident(idSuCo, TrangThai.FAILED, now, null);
    NotificationService.thongBao("Dữ liệu nghiêm trọng",
        "Tự động phục hồi thất bại. " + moTa(report));
    AppState.set(AppState.State.SUSPICIOUS);
    store.ghi(state.withOpenIncident(null).withAutoRestoreAttempts(lanThu)
        .withLastStatus("CRITICAL"));
  }

  private void xuLyUnavailable(HealthReport report, HealthState state, Instant now) {
    Instant lanThongBao = state.lastNotifiedAt();
    if (denHanNhacLai(lanThongBao, now)) {
      NotificationService.thongBao("Không kết nối được cơ sở dữ liệu", report.chiTiet());
      lanThongBao = now;
    }
    store.ghi(state.withLastNotifiedAt(lanThongBao).withLastStatus("UNAVAILABLE"));
    AppState.set(AppState.State.UNAVAILABLE);
  }

  /**
   * Hợp nhất các lần thử đã lưu trong tệp với danh sách trong bộ nhớ của tiến trình.
   *
   * <p>Danh sách trong bộ nhớ là nguồn dự phòng khi {@link HealthStateStore#ghi} không ghi được:
   * giới hạn tần suất vẫn đúng trong vòng đời tiến trình. Danh sách trả về là bản sao có thể sửa
   * được để bổ sung lượt thử mới trước khi ghi xuống tệp.
   *
   * @param state trạng thái đã đọc ở đầu chu kỳ
   * @return danh sách hợp nhất, không trùng lặp
   */
  private List<Instant> lanThuDaBiet(HealthState state) {
    List<Instant> hopNhat = new ArrayList<>(state.autoRestoreAttempts());
    for (Instant thoiDiem : autoRestoreAttempts) {
      if (!hopNhat.contains(thoiDiem)) {
        hopNhat.add(thoiDiem);
      }
    }
    return hopNhat;
  }

  /**
   * Chèn một sự cố {@code PENDING} ở chế độ nỗ lực tối đa.
   *
   * <p>Bảng sự cố có thể không truy cập được đúng lúc dữ liệu hỏng (ví dụ SQLState {@code 3D000}).
   * Khi đó việc ghi sự cố chỉ là phụ và không được chặn chính sách phục hồi.
   *
   * @return mã sự cố vừa tạo, hoặc {@code null} khi không ghi được
   */
  private Long taoSuCo(Severity severity, String chiTiet, Instant now) {
    try {
      return incidentDao.insert(new HealthIncident(null, severity, chiTiet, TrangThai.PENDING,
          null, now, null, null));
    } catch (RuntimeException e) {
      log.warn("Không ghi được sự cố {} ({}): {}", severity, chiTiet, e.getMessage());
      return null;
    }
  }

  /**
   * Ghi lại sự cố {@code AUTO_RESTORED} sau khi tự động phục hồi thành công mà lúc phát hiện chưa
   * ghi được sự cố nào.
   *
   * <p>Chỉ là nỗ lực tối đa: bảng sự cố có thể vẫn chưa sẵn sàng và lỗi chỉ được ghi cảnh báo.
   */
  private void ghiSuCoDaPhucHoi(String chiTiet, String tepDung, Instant now) {
    try {
      incidentDao.insert(new HealthIncident(null, Severity.CRITICAL, chiTiet,
          TrangThai.AUTO_RESTORED, tepDung, now, now, NGUOI_HE_THONG));
      log.info("Đã ghi sự cố tự động phục hồi sau khi bảng sự cố sẵn sàng trở lại");
    } catch (RuntimeException e) {
      log.warn("Không ghi được sự cố AUTO_RESTORED sau phục hồi: {}", e.getMessage());
    }
  }

  /**
   * Lấy tối đa {@link HealthConfig#soBanDuPhongToiDa()} bản sao lưu thành công, hợp lệ, mới nhất.
   *
   * <p>Danh sách từ {@link BackupService#danhSach()} đã sắp mới nhất trước; bản không kiểm tra được
   * bị bỏ qua kèm cảnh báo thay vì làm hỏng cả chu kỳ.
   */
  private List<BackupRecord> timBanDuPhong() {
    int toiDa = HealthConfig.soBanDuPhongToiDa();
    List<BackupRecord> ungVien = new ArrayList<>(toiDa);
    for (BackupRecord banGhi : backupService.danhSach()) {
      if (!banGhi.thanhCong()) {
        continue;
      }
      try {
        backupService.kiemTra(banGhi);
      } catch (BackupException e) {
        log.warn("Bỏ qua bản sao lưu không hợp lệ {}: {}", banGhi.fileName(), e.getMessage());
        continue;
      }
      ungVien.add(banGhi);
      if (ungVien.size() >= toiDa) {
        break;
      }
    }
    return ungVien;
  }

  private void ketThucIncident(Long id, TrangThai trangThai, Instant now, String tepDung) {
    if (id == null) {
      return;
    }
    try {
      incidentDao.capNhatKetThuc(id, trangThai, now, NGUOI_HE_THONG, tepDung);
    } catch (RuntimeException e) {
      log.warn("Không cập nhật được sự cố sức khỏe {}: {}", id, e.getMessage());
    }
  }

  private static boolean denHanNhacLai(Instant lanTruoc, Instant now) {
    return lanTruoc == null
        || lanTruoc.plus(HealthConfig.nhacLaiPhut(), ChronoUnit.MINUTES).isBefore(now);
  }

  private static String moTa(HealthReport report) {
    return report.chiTiet() == null ? "" : report.chiTiet();
  }
}
