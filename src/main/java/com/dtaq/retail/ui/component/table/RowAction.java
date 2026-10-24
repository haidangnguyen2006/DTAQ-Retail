package com.dtaq.retail.ui.component.table;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Một nút hành động trong cột thao tác của bảng.
 *
 * @param label nhãn nút
 * @param style kiểu hiển thị
 * @param onClick xử lý khi bấm; nhận chỉ số dòng theo <b>model</b> (đã quy đổi khi bảng có sắp xếp)
 */
public record RowAction(String label, Style style, IntConsumer onClick) {

  public enum Style {
    PRIMARY,
    SECONDARY,
    DANGER
  }

  public RowAction {
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(style, "style");
    Objects.requireNonNull(onClick, "onClick");
  }

  public static RowAction primary(String label, IntConsumer onClick) {
    return new RowAction(label, Style.PRIMARY, onClick);
  }

  public static RowAction secondary(String label, IntConsumer onClick) {
    return new RowAction(label, Style.SECONDARY, onClick);
  }

  public static RowAction danger(String label, IntConsumer onClick) {
    return new RowAction(label, Style.DANGER, onClick);
  }
}
