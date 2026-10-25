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
    SUCCESS("circle-check", DtaqColors.PRIMARY_SOFT_2),
    ERROR("alert-circle", DtaqColors.DANGER_SOFT),
    INFO("info", DtaqColors.PRIMARY_SOFT_2);

    private final String icon;
    private final Color iconColor;

    Kind(String icon, Color iconColor) {
      this.icon = icon;
      this.iconColor = iconColor;
    }
  }

  private static final int MARGIN = 24;
  private static final int DURATION_MS = 3000;

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
    toast.setBackground(DtaqColors.TEXT);
    toast.putClientProperty(FlatClientProperties.STYLE, "arc: 20");
    toast.add(new JLabel(DtaqIcons.icon(kind.icon, 18, kind.iconColor)));
    JLabel label = new JLabel(message);
    label.setFont(DtaqFonts.BODY_13_MEDIUM);
    label.setForeground(Color.WHITE);
    toast.add(label);

    Dimension size = toast.getPreferredSize();
    toast.setBounds(layered.getWidth() - size.width - MARGIN, layered.getHeight() - size.height - MARGIN,
        size.width, size.height);
    layered.add(toast, JLayeredPane.POPUP_LAYER);
    layered.repaint();

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
    layered.remove(toast);
    layered.revalidate();
    layered.repaint();
  }
}
