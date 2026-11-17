package com.dtaq.retail.ui.page.invoice;

import com.dtaq.retail.ui.format.MoneyFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Gợi ý nhanh số tiền khách đưa khi lập hóa đơn bán lẻ (UC003).
 *
 * <p>Lớp thuần Java, không phụ thuộc Swing: chỉ tính các số tiền tròn (bội số 1.000 VNĐ) đủ trả
 * hóa đơn để thu ngân bấm chọn thay vì gõ tay. Trang chỉ hiển thị kết quả, không tính lại hay làm
 * tròn tổng thanh toán của hóa đơn.
 */
final class CashSuggestions {

  /** Số gợi ý tối đa hiển thị trên màn hình. */
  static final int MAX_SUGGESTIONS = 6;

  /** Các mệnh giá tiền mặt đang lưu hành, xếp giảm dần. */
  private static final long[] NOTES = {
      500_000L, 200_000L, 100_000L, 50_000L, 20_000L, 10_000L, 5_000L, 2_000L, 1_000L};

  private CashSuggestions() {}

  /**
   * Danh sách số tiền khách có thể đưa, xếp tăng dần và không trùng lặp.
   *
   * <p>Mỗi gợi ý là bội số của một mệnh giá và đủ trả hóa đơn; gợi ý chỉ được giữ khi bỏ đi bất kỳ
   * tờ tiền nào trong cách phân rã cũng không còn đủ tiền, tức là khách không thể đưa ít hơn mà
   * vẫn đủ.
   *
   * @param total tổng thanh toán của hóa đơn (VNĐ)
   * @return tối đa {@value #MAX_SUGGESTIONS} gợi ý; danh sách rỗng nếu tổng không dương
   */
  static List<Long> forTotal(long total) {
    if (total <= 0) {
      return List.of();
    }
    TreeSet<Long> candidates = new TreeSet<>();
    for (long note : NOTES) {
      candidates.add(ceilToMultiple(total, note));
    }
    List<Long> suggestions = new ArrayList<>();
    for (long amount : candidates) {
      if (suggestions.size() >= MAX_SUGGESTIONS) {
        break;
      }
      if (isSuggestion(amount, total)) {
        suggestions.add(amount);
      }
    }
    return List.copyOf(suggestions);
  }

  /**
   * Phân rã số tiền thành các tờ tiền, mệnh giá lớn trước.
   *
   * @param amount số tiền cần phân rã (VNĐ)
   * @return các mệnh giá xếp giảm dần; rỗng nếu số tiền không dương hoặc không trả được bằng các
   *     mệnh giá hiện có (ví dụ không phải bội số của 1.000)
   */
  static List<Long> breakdown(long amount) {
    if (amount <= 0) {
      return List.of();
    }
    List<Long> notes = new ArrayList<>();
    long remainder = amount;
    for (long note : NOTES) {
      while (remainder >= note) {
        notes.add(note);
        remainder -= note;
      }
    }
    return remainder == 0 ? List.copyOf(notes) : List.of();
  }

  /**
   * Mô tả cách phân rã số tiền theo từng mệnh giá, ví dụ {@code "20.000 + 10.000 + 2.000 × 3"}.
   *
   * @param amount số tiền cần mô tả (VNĐ)
   * @return chuỗi mô tả; chuỗi rỗng nếu số tiền không dương
   */
  static String describe(long amount) {
    List<Long> notes = breakdown(amount);
    if (notes.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    int i = 0;
    while (i < notes.size()) {
      long note = notes.get(i);
      int count = 0;
      while (i < notes.size() && notes.get(i) == note) {
        count++;
        i++;
      }
      if (text.length() > 0) {
        text.append(" + ");
      }
      text.append(MoneyFormat.format(note));
      if (count > 1) {
        text.append(" × ").append(count);
      }
    }
    return text.toString();
  }

  /** Một số tiền có phải gợi ý hợp lệ cho tổng thanh toán hay không. */
  private static boolean isSuggestion(long amount, long total) {
    if (amount < total) {
      return false;
    }
    List<Long> notes = breakdown(amount);
    if (notes.isEmpty()) {
      return false;
    }
    for (long note : notes) {
      if (amount - note >= total) {
        return false;
      }
    }
    return true;
  }

  /** Làm tròn {@code value} lên bội số gần nhất của {@code step}. */
  private static long ceilToMultiple(long value, long step) {
    long remainder = value % step;
    return remainder == 0 ? value : value + (step - remainder);
  }
}
