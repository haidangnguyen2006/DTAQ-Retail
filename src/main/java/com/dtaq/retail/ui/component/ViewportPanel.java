package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Khung chứa nội dung đặt trong {@code JScrollPane}: luôn bám chiều rộng khung nhìn (không cuộn ngang)
 * và giãn hết chiều cao khi nội dung thấp hơn khung nhìn.
 */
public class ViewportPanel extends JPanel implements Scrollable {

  public ViewportPanel(JComponent content) {
    super(new BorderLayout());
    setBackground(DtaqColors.BG_APP);
    add(content, BorderLayout.CENTER);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 24;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return Math.max(24, visibleRect.height - 48);
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    Container parent = getParent();
    return parent instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
  }
}
