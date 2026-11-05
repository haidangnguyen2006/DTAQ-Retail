package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Tiêu đề trang chuẩn (PageHeader) gồm tên trang, mô tả ngữ cảnh và thành phần thao tác bên phải.
 */
public class PageHeader extends JPanel {

  private final JLabel titleLabel = new JLabel();
  private final JLabel subtitleLabel = new JLabel();
  private final JPanel trailingPanel = new JPanel(new MigLayout("insets 0, gapx 10", "[]", "[]"));

  @Override
  public java.awt.Dimension getMinimumSize() {
    return new java.awt.Dimension(0, super.getMinimumSize().height);
  }

  public PageHeader(String titleText, String subtitleText) {
    super(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    setOpaque(false);

    JPanel titleBlock = new JPanel(new MigLayout("fillx, insets 0", "[0:pref,grow]", "[]6[]"));
    titleBlock.setOpaque(false);

    titleLabel.setText(titleText);
    titleLabel.setFont(DtaqFonts.PAGE_TITLE);
    titleLabel.setForeground(DtaqColors.TEXT);
    titleBlock.add(titleLabel, "wrap");

    subtitleLabel.setFont(DtaqFonts.BODY_14);
    subtitleLabel.setForeground(DtaqColors.TEXT_MUTED);
    setSubtitle(subtitleText);
    titleBlock.add(subtitleLabel, "wrap");

    add(titleBlock, "growx, aligny center");

    trailingPanel.setOpaque(false);
    add(trailingPanel, "align right, aligny center");
    addComponentListener(new java.awt.event.ComponentAdapter() {
      private Boolean stacked;

      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        boolean narrow = getWidth() < titleBlock.getPreferredSize().width
            + trailingPanel.getPreferredSize().width + 24;
        if (stacked == null || stacked != narrow) {
          stacked = narrow;
          MigLayout layout = (MigLayout) getLayout();
          layout.setColumnConstraints(narrow ? "[0:pref,grow]" : "[0:pref,grow][]");
          layout.setComponentConstraints(titleBlock, "cell 0 0, growx");
          layout.setComponentConstraints(trailingPanel,
              narrow ? "cell 0 1, align left, gaptop 12" : "cell 1 0, align right");
          revalidate();
        }
      }
    });
  }

  /** Đặt các thành phần thao tác bên phải tiêu đề (thay thế phần đã có). */
  public void setTrailing(JComponent... trailing) {
    trailingPanel.removeAll();
    if (trailing != null) {
      for (JComponent component : trailing) {
        if (component != null) {
          trailingPanel.add(component);
        }
      }
    }
    trailingPanel.revalidate();
    trailingPanel.repaint();
  }

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  public void setSubtitle(String subtitle) {
    boolean has = subtitle != null && !subtitle.trim().isEmpty();
    subtitleLabel.setText(has ? subtitle : "");
    subtitleLabel.setVisible(has);
  }
}
