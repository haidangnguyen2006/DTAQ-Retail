package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Dimension;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;

/**
 * Thẻ hiển thị chỉ số quan trọng (KPI Card) theo chuẩn Figma.
 */
public class KpiCard extends Card {

  private final JLabel labelTitle = new JLabel();
  private final JLabel valueLabel = new JLabel();
  private final JLabel captionLabel = new JLabel();

  public KpiCard(String labelText, String initialValue, String captionText) {
    super(DtaqSpacing.CARD_PADDING_KPI, 10, DtaqSpacing.RADIUS_L);
    setLayout(new MigLayout("fill, insets " + DtaqSpacing.CARD_PADDING_KPI, "[grow]", "[]8[]8[]"));
    setPreferredSize(new Dimension(getPreferredSize().width, 134));

    labelTitle.setText(labelText);
    labelTitle.setFont(DtaqFonts.BODY_13);
    labelTitle.setForeground(DtaqColors.TEXT_MUTED);
    add(labelTitle, "wrap");

    valueLabel.setText(initialValue != null ? initialValue : "—");
    valueLabel.setFont(DtaqFonts.KPI_VALUE);
    valueLabel.setForeground(DtaqColors.TEXT);
    add(valueLabel, "wrap");

    captionLabel.setText(captionText != null ? captionText : "");
    captionLabel.setFont(DtaqFonts.CAPTION);
    if (captionText != null && captionText.startsWith("↑")) {
      captionLabel.setForeground(DtaqColors.PRIMARY);
    } else {
      captionLabel.setForeground(DtaqColors.TEXT_MUTED);
    }
    add(captionLabel, "wrap");
  }

  public void setValue(String value) {
    valueLabel.setText(value);
  }

  public void setCaption(String caption) {
    captionLabel.setText(caption);
    if (caption != null && caption.startsWith("↑")) {
      captionLabel.setForeground(DtaqColors.PRIMARY);
    } else {
      captionLabel.setForeground(DtaqColors.TEXT_MUTED);
    }
  }
}
