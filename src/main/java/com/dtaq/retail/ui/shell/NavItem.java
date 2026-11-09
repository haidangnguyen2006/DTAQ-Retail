package com.dtaq.retail.ui.shell;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.ui.theme.DtaqStyles;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.Animator;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * Mục điều hướng trên thanh Sidebar (nền tối). Là nút thật nên chọn được bằng Tab và kích hoạt bằng Enter/Space.
 * Có chế độ thu gọn (chỉ icon) và thanh chỉ báo mục đang chọn trượt vào mượt.
 */
public class NavItem extends JButton {

  private static final int ICON_SIZE = 18;
  private static final int ACCENT_WIDTH = 3;
  private static final int ANIM_MS = 160;

  private final String iconName;
  private final String labelText;
  private boolean active;
  private boolean compact;
  /** Độ hiện của thanh chỉ báo, 0..1; chạy hoạt ảnh khi đổi trạng thái chọn. */
  private float accentProgress;
  private Animator animator;

  public NavItem(String iconName, String labelText) {
    super(labelText);
    this.iconName = iconName;
    this.labelText = labelText;
    setHorizontalAlignment(SwingConstants.LEFT);
    setIconTextGap(12);
    setFocusPainted(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setToolTipText(labelText);

    getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dtaq.activate");
    getActionMap().put("dtaq.activate", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doClick();
      }
    });

    updateState();
    addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        wrapLabel();
      }
    });
    getAccessibleContext().setAccessibleName(labelText);
  }

  private void wrapLabel() {
    if (compact || getWidth() <= 0) return;
    java.awt.Insets insets = getInsets();
    int width = Math.max(40, getWidth() - insets.left - insets.right
        - (getIcon() == null ? 0 : getIcon().getIconWidth() + getIconTextGap()));
    String escaped = labelText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    String html = "<html><table width='" + width
        + "' cellpadding='0' cellspacing='0'><tr><td>" + escaped + "</td></tr></table></html>";
    if (!html.equals(getText())) {
      super.setText(html);
      revalidate();
    }
  }

  public void setActive(boolean active) {
    if (this.active == active) {
      return;
    }
    this.active = active;
    updateState();
    animateAccent(active ? 1f : 0f);
  }

  public boolean isActive() {
    return active;
  }

  /** Thu gọn thành dải icon (tooltip giữ nhãn) hoặc mở lại đầy đủ. */
  public void setCompact(boolean compact) {
    this.compact = compact;
    super.setText(compact ? "" : labelText);
    setHorizontalAlignment(compact ? SwingConstants.CENTER : SwingConstants.LEFT);
    setIconTextGap(compact ? 0 : 12);
    setToolTipText(labelText);
    getAccessibleContext().setAccessibleName(labelText);
    updateState();
    revalidate();
  }

  public void setOnClick(Runnable onClick) {
    addActionListener(e -> onClick.run());
  }

  private void animateAccent(float target) {
    if (animator != null) {
      animator.cancel();
    }
    float from = accentProgress;
    if (!Animator.useAnimation() || !isShowing()) {
      accentProgress = target;
      repaint();
      return;
    }
    animator = new Animator(ANIM_MS, fraction -> {
      accentProgress = from + (target - from) * fraction;
      repaint();
    });
    animator.setInterpolator(com.formdev.flatlaf.util.CubicBezierEasing.EASE_OUT);
    animator.start();
  }

  private void updateState() {
    setFont(active ? DtaqFonts.BODY_13_MEDIUM : DtaqFonts.BODY_13);
    setIcon(DtaqIcons.icon(iconName, ICON_SIZE,
        active ? DtaqColors.SIDEBAR_ACCENT : DtaqColors.SIDEBAR_TEXT_MUTED));
    String style = DtaqStyles.navItem(active);
    if (compact) {
      style += "; margin: 0,0,0,0";
    }
    putClientProperty(FlatClientProperties.STYLE, style);
    wrapLabel();
  }

  /** Cho phép co hẹp hơn nhãn: nhãn dài xuống dòng và tooltip giữ đủ chữ. */
  @Override
  public Dimension getMinimumSize() {
    return new Dimension(DtaqSpacing.SIDEBAR_RAIL_W - 24, super.getMinimumSize().height);
  }

  /** Nhãn nhiều dòng căn giữa theo hàng, không dùng baseline một dòng của FlatLaf. */
  @Override
  public int getBaseline(int width, int height) {
    return -1;
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    d.height = Math.max(d.height, DtaqSpacing.NAV_ITEM_H);
    return d;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (accentProgress > 0.01f) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g2.setColor(DtaqColors.SIDEBAR_ACCENT);
      float barHeight = (getHeight() - 16) * accentProgress;
      g2.fill(new RoundRectangle2D.Float(0, (getHeight() - barHeight) / 2f, ACCENT_WIDTH, barHeight,
          ACCENT_WIDTH, ACCENT_WIDTH));
      g2.dispose();
    }
  }
}
