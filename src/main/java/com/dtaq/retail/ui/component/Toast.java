package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.miginfocom.swing.MigLayout;

/**
 * Thông báo ngắn góc phải dưới cửa sổ, tự ẩn sau vài giây. Gọi trên EDT.
 */
public final class Toast {

  public enum Kind {
    SUCCESS("circle-check", new Color(0x5DD39E)),
    ERROR("alert-circle", new Color(0xFF8A85)),
    INFO("info", DtaqColors.SIDEBAR_ACCENT);

    private final String icon;
    private final Color iconColor;

    Kind(String icon, Color iconColor) {
      this.icon = icon;
      this.iconColor = iconColor;
    }
  }

  private static final int MARGIN = 24;
  private static final int DURATION_MS = 3000;
  private static final int SLIDE_PX = 18;
  private static final int ENTER_MS = 180;
  private static final int EXIT_MS = 140;

  /** Trượt thông báo theo trục dọc từ {@code from} tới {@code to}; nhẹ, bỏ qua nếu hoạt ảnh bị tắt. */
  private static void slide(JComponent toast, int from, int to, int ms, Runnable done) {
    if (!com.formdev.flatlaf.util.Animator.useAnimation()) {
      toast.setLocation(toast.getX(), to);
      if (done != null) {
        done.run();
      }
      return;
    }
    com.formdev.flatlaf.util.Animator animator = new com.formdev.flatlaf.util.Animator(ms, fraction -> {
      toast.setLocation(toast.getX(), from + Math.round((to - from) * fraction));
      if (toast.getParent() != null) {
        toast.getParent().repaint();
      }
    }, () -> {
      if (done != null) {
        done.run();
      }
    });
    animator.setInterpolator(com.formdev.flatlaf.util.CubicBezierEasing.EASE_OUT);
    animator.start();
  }

  private Toast() {
  }

  public static void show(Component anchor, String message, Kind kind) {
    JRootPane root = anchor == null ? null : SwingUtilities.getRootPane(anchor);
    if (root == null) {
      return;
    }
    JLayeredPane layered = root.getLayeredPane();

    JPanel toast = new JPanel(new MigLayout("insets 12 16 12 18, gapx 10", "[][]", "[center]"));
    toast.setOpaque(true);
    toast.setBackground(new Color(0x1F2633));
    toast.putClientProperty(FlatClientProperties.STYLE, "arc: 20");
    toast.add(new JLabel(DtaqIcons.icon(kind.icon, 18, kind.iconColor)));
    JLabel label = new JLabel(message);
    label.setFont(DtaqFonts.BODY_13_MEDIUM);
    label.setForeground(DtaqColors.SURFACE);
    toast.add(label);

    Dimension size = toast.getPreferredSize();
    int x = layered.getWidth() - size.width - MARGIN;
    int y = layered.getHeight() - size.height - MARGIN;
    toast.setBounds(x, y + SLIDE_PX, size.width, size.height);
    layered.add(toast, JLayeredPane.POPUP_LAYER);
    layered.repaint();
    slide(toast, y + SLIDE_PX, y, ENTER_MS, null);

    Timer timer = new Timer(DURATION_MS, e -> dismiss(layered, toast));
    timer.setRepeats(false);
    timer.start();
  }

  public static void success(Component anchor, String message) {
    show(anchor, message, Kind.SUCCESS);
  }

  public static void error(Component anchor, String message) {
    show(anchor, message, Kind.ERROR);
  }

  private static void dismiss(JLayeredPane layered, JComponent toast) {
    slide(toast, toast.getY(), toast.getY() + SLIDE_PX, EXIT_MS, () -> {
      layered.remove(toast);
      layered.revalidate();
      layered.repaint();
    });
  }
}
