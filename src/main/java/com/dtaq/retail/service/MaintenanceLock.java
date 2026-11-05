package com.dtaq.retail.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Khóa dùng chung cho các thao tác nặng như sao lưu và phục hồi cơ sở dữ liệu.
 *
 * <p>Chỉ một thao tác được chạy tại một thời điểm. Khóa là công bằng để các luồng chờ được phục vụ
 * theo thứ tự yêu cầu.
 */
public final class MaintenanceLock {

  private static final ReentrantLock LOCK = new ReentrantLock(true);

  private MaintenanceLock() {
  }

  /**
   * Thử giữ khóa trong khoảng thời gian chờ tối đa.
   *
   * @param timeout thời gian chờ tối đa
   * @return {@code true} nếu giữ được khóa, {@code false} nếu hết thời gian chờ
   * @throws InterruptedException nếu luồng bị ngắt khi đang chờ
   */
  public static boolean tryAcquire(Duration timeout) throws InterruptedException {
    Objects.requireNonNull(timeout, "timeout");
    long millis = Math.max(0L, timeout.toMillis());
    return LOCK.tryLock(millis, TimeUnit.MILLISECONDS);
  }

  /** Nhả khóa nếu luồng hiện tại đang giữ khóa; gọi khi chưa giữ khóa thì không làm gì. */
  public static void release() {
    if (LOCK.isHeldByCurrentThread()) {
      LOCK.unlock();
    }
  }

  /** Cho biết hiện có luồng nào đang giữ khóa hay không. */
  public static boolean isBusy() {
    return LOCK.isLocked();
  }
}
