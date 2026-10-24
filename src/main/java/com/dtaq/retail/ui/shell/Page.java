package com.dtaq.retail.ui.shell;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;

/**
 * Giao diện đại diện cho một trang nội dung được nạp vào vùng CardLayout của AppShell.
 */
public interface Page {

  PageId id();

  String title();

  String section();

  JComponent view();

  /**
   * Trang có thanh cuộn dọc riêng hay không. Trang dạng bảng (bảng tự giãn hết chiều cao khung
   * và tự cuộn bên trong) trả về {@code false}.
   */
  default boolean scrollable() {
    return true;
  }

  default void onShow() {}

  default void onHide() {}

  /**
   * Đăng ký phím tắt riêng của trang. Shell gọi khi trang được hiển thị và gỡ khi chuyển trang,
   * nên trang chỉ cần {@code put} vào hai bảng ánh xạ được truyền vào.
   */
  default void installShortcuts(InputMap inputMap, ActionMap actionMap) {}
}
