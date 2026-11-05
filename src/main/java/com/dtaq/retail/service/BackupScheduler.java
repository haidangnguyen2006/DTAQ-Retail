package com.dtaq.retail.service;

import com.dtaq.retail.config.BackupConfig;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.exception.BackupException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lập lịch sao lưu tự động theo chu kỳ cấu hình và chạy bù khi ứng dụng khởi động.
 *
 * <p>Lần chạy đầu tiên rơi vào giờ cấu hình {@code DTAQ_BACKUP_TIME} gần nhất chưa qua, sau đó lặp
 * lại mỗi {@code DTAQ_BACKUP_INTERVAL_HOURS} giờ (mặc định 24 giờ), theo múi giờ
 * {@code Asia/Ho_Chi_Minh}. Chính giá trị chu kỳ đó cũng là ngưỡng chạy bù: ngay sau khi khởi động,
 * nếu bản sao lưu thành công gần nhất đã cũ hơn chu kỳ thì chạy bù một lần. Công việc chạy trên một
 * luồng nền daemon duy nhất, không chặn luồng giao diện.
 *
 * <p>Lớp này không ném lỗi ra ngoài luồng lập lịch: lỗi sao lưu chỉ được ghi cảnh báo, còn lần chạy
 * kế tiếp vẫn được giữ. Cấu hình đổi trên giao diện được áp dụng bằng {@link #lapLichLai()}.
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
  private ScheduledFuture<?> nhiemVuDinhKy;

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
   * Bật lịch sao lưu: chạy bù ngay nếu cần, rồi lặp lại theo giờ và chu kỳ cấu hình.
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
    lapLich(moi);
  }

  /**
   * Lập lại lịch chạy theo cấu hình hiện tại, dùng khi Quản lý đổi giờ chạy hoặc chu kỳ.
   *
   * <p>Hủy lịch cũ (không ngắt lần chạy đang diễn ra) rồi đặt lịch mới trên cùng luồng nền. Không
   * làm gì khi sao lưu tự động chưa bật hoặc đã dừng. Phương thức không bao giờ ném ngoại lệ.
   */
  public synchronized void lapLichLai() {
    if (closed.get() || !started.get() || executor == null) {
      return;
    }
    try {
      lapLich(executor);
    } catch (RuntimeException e) {
      log.warn("Không lập lại được lịch sao lưu: {}", e.getMessage());
    }
  }

  /**
   * Đặt lịch chạy lặp lại: lần đầu vào giờ cấu hình kế tiếp, sau đó mỗi {@code chuKyGio} giờ.
   *
   * <p>Lịch cũ nếu có luôn bị hủy trước khi thay bằng lịch mới.
   *
   * @param executor luồng nền dùng để đặt lịch; không được {@code null}
   */
  private void lapLich(ScheduledExecutorService executor) {
    ZonedDateTime bayGio = ZonedDateTime.now(MUI_GIO);
    LocalTime gio = BackupConfig.gioChay();
    long delayMillis = delayMillis(gio, bayGio);
    int chuKyGio = Math.max(1, BackupConfig.chuKyGio());
    long periodMillis = chuKyMillis(chuKyGio);
    ZonedDateTime lanToi = bayGio.plus(Duration.ofMillis(delayMillis));

    synchronized (this) {
      if (nhiemVuDinhKy != null) {
        nhiemVuDinhKy.cancel(false);
      }
      nhiemVuDinhKy = executor.scheduleAtFixedRate(this::chayTheoLich, delayMillis, periodMillis,
          TimeUnit.MILLISECONDS);
    }
    log.info("Đã lập lịch sao lưu lúc {} mỗi {} giờ; lần chạy kế tiếp: {}", gio, chuKyGio, lanToi);
  }

  /**
   * Đặt lịch chạy lặp lại trên một luồng nền cho trước; dùng cho kiểm thử.
   *
   * @param executor luồng nền dùng để đặt lịch; không được {@code null}
   */
  void lapLichChoTest(ScheduledExecutorService executor) {
    lapLich(executor);
  }

  /**
   * Nhiệm vụ lặp lại hiện tại, hoặc {@code null} khi chưa lập lịch hoặc đã dừng.
   *
   * @return nhiệm vụ đang giữ lịch chạy theo định kỳ
   */
  synchronized ScheduledFuture<?> nhiemVuDinhKy() {
    return nhiemVuDinhKy;
  }

  /**
   * Đổi chu kỳ tính bằng giờ sang số mili giây, tối thiểu một giờ.
   *
   * @param chuKyGio chu kỳ mong đợi; giá trị không dương được nâng thành một giờ
   * @return độ dài chu kỳ tính bằng mili giây
   */
  static long chuKyMillis(int chuKyGio) {
    return TimeUnit.HOURS.toMillis(Math.max(1, chuKyGio));
  }

  /**
   * Khoảng thời gian từ {@code now} tới lần xuất hiện kế tiếp của {@code gio} trong ngày.
   *
   * <p>Nếu {@code now} đã đúng {@code gio} thì lần kế tiếp là ngày mai, tránh vòng lặp delay 0.
   *
   * @param gio giờ chạy trong ngày
   * @param now thời điểm hiện tại kèm múi giờ
   * @return số mili giây phải chờ, luôn dương
   */
  static long delayMillis(LocalTime gio, ZonedDateTime now) {
    Objects.requireNonNull(gio, "gio");
    Objects.requireNonNull(now, "now");
    ZonedDateTime lanToi = now.toLocalDate().atTime(gio).atZone(now.getZone());
    if (!lanToi.isAfter(now)) {
      lanToi = lanToi.plusDays(1);
    }
    return Duration.between(now, lanToi).toMillis();
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
      if (nhiemVuDinhKy != null) {
        nhiemVuDinhKy.cancel(false);
        nhiemVuDinhKy = null;
      }
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
