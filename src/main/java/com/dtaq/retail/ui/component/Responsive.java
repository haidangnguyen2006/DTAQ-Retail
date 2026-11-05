package com.dtaq.retail.ui.component;

import com.formdev.flatlaf.util.UIScale;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/** Co giãn theo kích thước logic của Swing, không ép DPI của hệ điều hành. */
public final class Responsive {
  public enum Size {
    S, M, L, XL;

    public static Size of(int width) {
      return width < 960 ? S : width < 1280 ? M : width < 1600 ? L : XL;
    }
  }

  private Responsive() {}

  public static void onSizeChange(JComponent host, Consumer<Size> listener) {
    host.addComponentListener(new ComponentAdapter() {
      private Size last;

      @Override
      public void componentResized(ComponentEvent e) {
        Size now = Size.of(UIScale.unscale(host.getWidth()));
        if (host.getWidth() > 0 && now != last) {
          last = now;
          listener.accept(now);
          host.revalidate();
          host.repaint();
        }
      }
    });
  }

  /** Lưới các ô cùng vai trò; giữ nguyên component, listener và thứ tự Tab. */
  public static void grid(JPanel panel, int minCellWidth, int maxColumns) {
    MigLayout layout = (MigLayout) panel.getLayout();
    panel.addComponentListener(new ComponentAdapter() {
      private int lastColumns;

      @Override
      public void componentResized(ComponentEvent e) {
        int available = UIScale.unscale(panel.getWidth());
        int columns = Math.max(1, Math.min(maxColumns, (available + 16) / (minCellWidth + 16)));
        if (columns == 3 && maxColumns == 4) {
          columns = 2;
        }
        if (columns != lastColumns) {
          lastColumns = columns;
          layout.setLayoutConstraints("fillx, insets 0, gap 16, wrap " + columns);
          layout.setColumnConstraints("[0:pref,grow,fill]".repeat(columns));
          panel.revalidate();
          panel.repaint();
        }
      }
    });
  }

  /** Kẹp cửa sổ vào vùng làm việc của màn hình hiện tại, kể cả khi scale 200%. */
  public static void fitWindow(Window window, Dimension preferred, Dimension minimum) {
    var config = window.getGraphicsConfiguration();
    Rectangle usable = new Rectangle(config.getBounds());
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
    usable.x += insets.left;
    usable.y += insets.top;
    usable.width -= insets.left + insets.right;
    usable.height -= insets.top + insets.bottom;
    window.setMinimumSize(new Dimension(Math.min(minimum.width, usable.width),
        Math.min(minimum.height, usable.height)));
    window.setSize(Math.min(preferred.width, usable.width), Math.min(preferred.height, usable.height));
    window.setLocation(usable.x + (usable.width - window.getWidth()) / 2,
        usable.y + (usable.height - window.getHeight()) / 2);
  }
}
