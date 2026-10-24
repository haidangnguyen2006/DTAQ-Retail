package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;

/**
 * Thẻ hiển thị chỉ số quan trọng (KPI Card). Chưa có dữ liệu thì hiện "—".
 */
public class KpiCard extends Card {

  /** Xu hướng so với kỳ trước; quyết định màu và mũi tên của chú thích. */
  public enum Trend {
    UP("↑ ", DtaqColors.PRIMARY),
    DOWN("↓ ", DtaqColors.DANGER),
    NEUTRAL("", DtaqColors.TEXT_MUTED);

    private final String prefix;
    private final Color color;

    Trend(String prefix, Color color) {
      this.prefix = prefix;
      this.color = color;
    }
  }

  private static final String EMPTY_VALUE = "—";

  private final JLabel labelTitle = new JLabel();
  private final JLabel valueLabel = new JLabel();
  private final JLabel captionLabel = new JLabel();

  public KpiCard(String labelText, String initialValue, String captionText) {
    super(DtaqSpacing.CARD_PADDING_KPI, 10, DtaqSpacing.RADIUS_L);
    setLayout(new MigLayout("fillx, insets " + DtaqSpacing.CARD_PADDING_KPI, "[grow]", "[]6[]6[]"));
    setMinimumSize(new Dimension(0, 112));

    labelTitle.setText(labelText);
    labelTitle.setFont(DtaqFonts.BODY_13);
    labelTitle.setForeground(DtaqColors.TEXT_MUTED);
    add(labelTitle, "wrap");

    valueLabel.setText(initialValue != null ? initialValue : EMPTY_VALUE);
    valueLabel.setFont(DtaqFonts.KPI_VALUE);
    valueLabel.setForeground(DtaqColors.TEXT);
    add(valueLabel, "wrap");

    captionLabel.setFont(DtaqFonts.CAPTION);
    setCaption(captionText);
    add(captionLabel, "wrap");
  }

  public void setValue(String value) {
    valueLabel.setText(value == null || value.isBlank() ? EMPTY_VALUE : value);
  }

  public void setCaption(String caption) {
    captionLabel.setText(caption != null ? caption : "");
    captionLabel.setForeground(Trend.NEUTRAL.color);
  }

  public void setTrend(Trend trend, String text) {
    captionLabel.setText(trend.prefix + (text != null ? text : ""));
    captionLabel.setForeground(trend.color);
  }

  /** Trạng thái đang tải: giá trị "—" và bỏ chú thích. */
  public void setLoading() {
    valueLabel.setText(EMPTY_VALUE);
    captionLabel.setText("Đang tải…");
    captionLabel.setForeground(Trend.NEUTRAL.color);
  }
}
