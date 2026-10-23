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
  private final JPanel trailingPanel = new JPanel(new MigLayout("insets 0", "[]", "[]"));

  public PageHeader(String titleText, String subtitleText) {
    super(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    setOpaque(false);

    JPanel titleBlock = new JPanel(new MigLayout("insets 0", "[]", "[]8[]"));
    titleBlock.setOpaque(false);

    titleLabel.setText(titleText);
    titleLabel.setFont(DtaqFonts.PAGE_TITLE);
    titleLabel.setForeground(DtaqColors.TEXT);
    titleBlock.add(titleLabel, "wrap");

    if (subtitleText != null && !subtitleText.trim().isEmpty()) {
      subtitleLabel.setText(subtitleText);
      subtitleLabel.setFont(DtaqFonts.BODY_14);
      subtitleLabel.setForeground(DtaqColors.TEXT_MUTED);
      titleBlock.add(subtitleLabel, "wrap");
    }

    add(titleBlock, "align left, aligny center");

    trailingPanel.setOpaque(false);
    add(trailingPanel, "align right, aligny center");
  }

  public void setTrailing(JComponent trailing) {
    trailingPanel.removeAll();
    if (trailing != null) {
      trailingPanel.add(trailing);
    }
    trailingPanel.revalidate();
    trailingPanel.repaint();
  }

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  public void setSubtitle(String subtitle) {
    subtitleLabel.setText(subtitle);
  }
}
