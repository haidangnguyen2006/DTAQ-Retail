package com.dtaq.retail.service;

import com.dtaq.retail.config.HealthConfig;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lập lịch kiểm tra sức khỏe dữ liệu và chuyển kết quả cho {@link RecoveryCoordinator}.
 *
 * <p>Chu kỳ chạy trên một luồng nền daemon duy nhất nên không giữ ứng dụng sống sau khi cửa sổ
 * chính đóng. Mỗi lần kiểm tra giành {@link MaintenanceLock} như các thao tác sao lưu/phục hồi
 * khác; khi hệ thống đang bận, chu kỳ hiện tại được bỏ qua thay vì xếp hàng.
 *
 * <p>Lớp không phụ thuộc Swing và không bao giờ ném ngoại lệ ra ngoài {@link #kiemTraNgay()}.
 */
public class HealthMonitor implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(HealthMonitor.class);

  /** Thời gian chờ tối đa để giành khóa bảo trì cho một chu kỳ. */
  private static final Duration CHO_KHOA = Duration.ofSeconds(1);

  /** Thời gian chờ tối đa khi dừng luồng lập lịch, tính bằng giây. */
  private static final long CHO_DUNG_GIAY = 5L;

  /** Tên luồng nền, dùng cho nhật ký và chẩn đoán. */
  private static final String TEN_LUONG = "dtaq-health-monitor";

  private final DataHealthChecker checker;
  private final RecoveryCoordinator coordinator;

  private ScheduledExecutorService executor;
  private boolean daDong;

  /**
   * Khởi tạo bộ giám sát.
   *
   * @param checker bộ kiểm tra dữ liệu; không được {@code null}
   * @param coordinator bộ áp chính sách phục hồi; không được {@code null}
   */
  public HealthMonitor(DataHealthChecker checker, RecoveryCoordinator coordinator) {
    this.checker = Objects.requireNonNull(checker, "checker");
    this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
  }

  /**
   * Chạy một chu kỳ kiểm tra ngay trên luồng gọi.
   *
   * <p>Không làm gì khi giám sát bị tắt bằng {@code DTAQ_HEALTH_ENABLED=false}, kể cả khi phương
   * thức được gọi trực tiếp lúc khởi động. Bỏ qua khi {@link MaintenanceLock} đang bận hoặc không
   * giành được khóa trong một giây. Khóa luôn được nhả ở khối {@code finally}; ngoại lệ chỉ được
   * ghi cảnh báo.
   */
  public void kiemTraNgay() {
    if (!HealthConfig.batTat()) {
      log.debug("Bỏ qua kiểm tra sức khỏe vì giám sát đang tắt theo cấu hình");
      return;
    }
    if (MaintenanceLock.isBusy()) {
      log.info("Bỏ qua kiểm tra sức khỏe vì hệ thống đang bận");
      return;
    }
    try {
      if (!MaintenanceLock.tryAcquire(CHO_KHOA)) {
        log.info("Bỏ qua kiểm tra sức khỏe vì không giành được khóa bảo trì");
        return;
      }
      coordinator.xuLy(checker.kiemTra());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      log.warn("Kiểm tra sức khỏe thất bại: {}", e.getMessage(), e);
    } finally {
      MaintenanceLock.release();
    }
  }

  /**
   * Bắt đầu lập lịch kiểm tra định kỳ.
   *
   * <p>Không làm gì khi giám sát bị tắt trong cấu hình hoặc khi đã chạy. Gọi nhiều lần là an toàn.
   */
  public synchronized void start() {
    if (daDong) {
      log.warn("Bộ giám sát sức khỏe đã dừng; không khởi động lại");
      return;
    }
    if (executor != null) {
      return;
    }
    if (!HealthConfig.batTat()) {
      log.info("Giám sát sức khỏe đang tắt theo cấu hình");
      return;
    }
    int chuKyPhut = HealthConfig.chuKyPhut();
    executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread luong = new Thread(runnable, TEN_LUONG);
      luong.setDaemon(true);
      return luong;
    });
    executor.scheduleAtFixedRate(this::kiemTraNgay, chuKyPhut, chuKyPhut, TimeUnit.MINUTES);
    log.info("Đã bật giám sát sức khỏe dữ liệu, chu kỳ {} phút", chuKyPhut);
  }

  /**
   * Dừng luồng lập lịch và chờ tối đa năm giây.
   *
   * <p>Phương thức idempotent và không bao giờ ném ngoại lệ.
   */
  @Override
  public synchronized void close() {
    if (daDong) {
      return;
    }
    daDong = true;
    if (executor == null) {
      return;
    }
    try {
      executor.shutdownNow();
      if (!executor.awaitTermination(CHO_DUNG_GIAY, TimeUnit.SECONDS)) {
        log.warn("Luồng giám sát sức khỏe chưa dừng sau {} giây", CHO_DUNG_GIAY);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      log.warn("Không dừng sạch được luồng giám sát sức khỏe: {}", e.getMessage());
    }
  }

  /**
   * Đổi chu kỳ phút sang mili giây.
   *
   * @param minutes số phút
   * @return số mili giây tương ứng
   */
  static long chuKyMillis(int minutes) {
    return TimeUnit.MINUTES.toMillis(minutes);
  }
}
