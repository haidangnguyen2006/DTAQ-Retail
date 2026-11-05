package com.dtaq.retail.service;

import com.dtaq.retail.config.BackupConfig;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.exception.BackupException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lập lịch sao lưu tự động hằng ngày và chạy bù khi ứng dụng khởi động.
 *
 * <p>Mỗi ngày một lần vào giờ cấu hình {@code DTAQ_BACKUP_TIME} theo múi giờ
 * {@code Asia/Ho_Chi_Minh}. Ngay sau khi khởi động, nếu bản sao lưu thành công gần nhất đã cũ hơn
 * chu kỳ {@code DTAQ_BACKUP_INTERVAL_HOURS} thì chạy bù một lần. Công việc chạy trên một luồng nền
 * daemon duy nhất, không chặn luồng giao diện.
 *
 * <p>Lớp này không ném lỗi ra ngoài luồng lập lịch: lỗi sao lưu chỉ được ghi cảnh báo, còn lần chạy
 * kế tiếp vẫn được giữ.
 */
public class BackupScheduler implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

  /** Múi giờ dùng cho lịch chạy và cách tính lần chạy kế tiếp. */
  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

  /** Thời gian chờ tối đa khi dừng luồng lập lịch. */
  private static final long CHO_DUNG_GIAY = 5L;

  private final BackupService service;
  private final String nguoiThucHien;

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean dangChay = new AtomicBoolean(false);
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private ScheduledExecutorService executor;

  /**
   * Khởi tạo bộ lập lịch; chưa có luồng nào được tạo cho tới khi gọi {@link #start()}.
   *
   * @param service dịch vụ sao lưu dùng để chạy; không được {@code null}
   * @param nguoiThucHien tên người hoặc tiến trình ghi vào lịch sử; không được {@code null}
   */
  public BackupScheduler(BackupService service, String nguoiThucHien) {
    this.service = Objects.requireNonNull(service, "service");
    this.nguoiThucHien = Objects.requireNonNull(nguoiThucHien, "nguoiThucHien");
  }

  /**
   * Bật lịch sao lưu: chạy bù ngay nếu cần, rồi chạy hằng ngày vào giờ cấu hình.
   *
   * <p>Gọi nhiều lần chỉ có tác dụng ở lần đầu. Khi {@code DTAQ_BACKUP_ENABLED=false} thì chỉ ghi
   * thông tin và không tạo luồng nào.
   */
  public void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    if (!BackupConfig.batTat()) {
      log.info("Sao lưu tự động đang tắt ({} = false); không lập lịch",
          BackupConfig.KEY_BAT_TAT);
      return;
    }

    ScheduledExecutorService moi = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "dtaq-backup-scheduler");
      thread.setDaemon(true);
      return thread;
    });
    synchronized (this) {
      executor = moi;
    }

    moi.execute(this::chayBu);

    ZonedDateTime bayGio = ZonedDateTime.now(MUI_GIO);
    ZonedDateTime lanToi = bayGio.toLocalDate().atTime(BackupConfig.gioChay()).atZone(MUI_GIO);
    if (!lanToi.isAfter(bayGio)) {
      lanToi = lanToi.plusDays(1);
    }
    long delayMillis = Duration.between(bayGio, lanToi).toMillis();
    moi.scheduleAtFixedRate(this::chayTheoLich, delayMillis, TimeUnit.DAYS.toMillis(1),
        TimeUnit.MILLISECONDS);
    log.info("Đã lập lịch sao lưu tự động lúc {} mỗi ngày; lần chạy kế tiếp: {}",
        BackupConfig.gioChay(), lanToi);
  }

  /**
   * Quyết định có cần chạy bù hay không dựa trên lần sao lưu thành công gần nhất.
   *
   * <p>Chỉ chạy bù khi bản gần nhất đã cũ <em>hơn hẳn</em> chu kỳ (mốc cộng chu kỳ nằm trước thời
   * điểm hiện tại). Chu kỳ không dương được hiểu là "luôn cần bản mới", nhưng vẫn không chạy bù khi
   * bản gần nhất trùng đúng thời điểm hiện tại.
   *
   * @param lanCuoi thời điểm sao lưu thành công gần nhất; rỗng khi chưa từng sao lưu
   * @param now thời điểm hiện tại
   * @param chuKyGio chu kỳ mong đợi tính bằng giờ
   * @return {@code true} khi chưa có bản nào hoặc bản gần nhất đã đủ cũ
   */
  static boolean canChayBu(Optional<Instant> lanCuoi, Instant now, int chuKyGio) {
    Objects.requireNonNull(lanCuoi, "lanCuoi");
    Objects.requireNonNull(now, "now");
    if (lanCuoi.isEmpty()) {
      return true;
    }
    Instant moc = chuKyGio <= 0
        ? lanCuoi.get()
        : lanCuoi.get().plus(chuKyGio, ChronoUnit.HOURS);
    return moc.isBefore(now);
  }

  private void chayBu() {
    Optional<Instant> lanCuoi = service.danhSach().stream()
        .filter(BackupRecord::thanhCong)
        .map(BackupRecord::createdAt)
        .max(Comparator.naturalOrder());
    if (canChayBu(lanCuoi, Instant.now(), BackupConfig.chuKyGio())) {
      chay("AUTO");
    }
  }

  private void chayTheoLich() {
    chay("AUTO");
  }

  /**
   * Chạy một lần sao lưu tự động nếu trạng thái ứng dụng cho phép.
   *
   * <p>Bỏ qua khi {@link AppState} không phải {@link AppState.State#NORMAL}: dữ liệu đang bất
   * thường hoặc đang bảo trì thì một bản sao lưu mới không có giá trị và có thể cạnh tranh với
   * thao tác phục hồi. Áp dụng cho cả lần chạy bù lúc khởi động và lần chạy theo lịch.
   *
   * @param nguon nhãn nguồn chạy, dùng cho nhật ký
   */
  void chay(String nguon) {
    if (!AppState.isNormal()) {
      log.info("Bỏ qua sao lưu vì trạng thái {}", AppState.get());
      return;
    }
    if (!dangChay.compareAndSet(false, true)) {
      log.info("Bỏ qua lần sao lưu {} vì lần trước chưa kết thúc", nguon);
      return;
    }
    try {
      BackupRecord banGhi = service.saoLuu(BackupRecord.LoaiSaoLuu.AUTO, nguoiThucHien);
      log.info("Sao lưu tự động ({}) thành công: {}", nguon, banGhi.fileName());
    } catch (BackupException e) {
      log.warn("Sao lưu tự động ({}) thất bại: {}", nguon, e.getMessage());
    } catch (RuntimeException e) {
      log.warn("Sao lưu tự động ({}) gặp lỗi không mong đợi: {}", nguon, e.getMessage());
    } finally {
      dangChay.set(false);
    }
  }

  /**
   * Dừng luồng lập lịch và chờ tối đa 5 giây. Gọi nhiều lần không gây lỗi.
   *
   * <p>Phương thức không bao giờ ném ngoại lệ để dùng được làm shutdown hook.
   */
  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    ScheduledExecutorService hienTai;
    synchronized (this) {
      hienTai = executor;
    }
    if (hienTai == null) {
      return;
    }
    hienTai.shutdownNow();
    try {
      if (!hienTai.awaitTermination(CHO_DUNG_GIAY, TimeUnit.SECONDS)) {
        log.warn("Bộ lập lịch sao lưu chưa dừng hẳn sau {} giây", CHO_DUNG_GIAY);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      log.warn("Lỗi khi dừng bộ lập lịch sao lưu: {}", e.getMessage());
    }
  }
}
