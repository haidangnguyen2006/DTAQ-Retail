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

  default void onShow() {}

  default void onHide() {}

  default void installShortcuts(InputMap inputMap, ActionMap actionMap) {}
}
