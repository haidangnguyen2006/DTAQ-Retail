package com.dtaq.retail.service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Trạng thái sức khỏe hiện tại của ứng dụng, dùng chung giữa luồng giám sát và các tác vụ nền.
 *
 * <p>{@link RecoveryCoordinator} cập nhật trạng thái sau mỗi chu kỳ kiểm tra; {@link
 * BackupScheduler} đọc trạng thái để không chạy sao lưu tự động khi dữ liệu chưa ở mức bình thường.
 * Giá trị được giữ trong {@link AtomicReference} nên đọc/ghi an toàn giữa các luồng mà không cần
 * khóa.
 */
public final class AppState {

  /** Bốn mức sức khỏe mà phần còn lại của ứng dụng cần phân biệt. */
  public enum State {
    /** Dữ liệu bình thường; cho phép các tác vụ nền tự động chạy. */
    NORMAL,
    /** Có dấu hiệu bất thường hoặc tự động phục hồi thất bại; cần Quản lý can thiệp. */
    SUSPICIOUS,
    /** Đang sao lưu hoặc phục hồi; tạm dừng các tác vụ nền khác. */
    MAINTENANCE,
    /** Không kết nối được cơ sở dữ liệu nên chưa thể đánh giá. */
    UNAVAILABLE
  }

  private static final AtomicReference<State> TRANG_THAI = new AtomicReference<>(State.NORMAL);

  private AppState() {
  }

  /**
   * Trạng thái hiện tại.
   *
   * @return trạng thái đang được lưu, mặc định {@link State#NORMAL}
   */
  public static State get() {
    return TRANG_THAI.get();
  }

  /**
   * Đặt trạng thái hiện tại.
   *
   * @param state trạng thái mới; không được {@code null}
   */
  public static void set(State state) {
    TRANG_THAI.set(Objects.requireNonNull(state, "state"));
  }

  /**
   * Cho biết ứng dụng có đang ở trạng thái bình thường hay không.
   *
   * @return {@code true} khi trạng thái hiện tại là {@link State#NORMAL}
   */
  public static boolean isNormal() {
    return TRANG_THAI.get() == State.NORMAL;
  }
}
