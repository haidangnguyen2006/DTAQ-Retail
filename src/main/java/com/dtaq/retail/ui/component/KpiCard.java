package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
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
    UP("trending-up", DtaqColors.SUCCESS),
    DOWN("trending-down", DtaqColors.DANGER),
    NEUTRAL(null, DtaqColors.TEXT_MUTED);

    /** Tên icon Lucide (thay cho ký tự mũi tên); null nếu không có icon. */
    private final String iconName;
    private final Color color;

    Trend(String iconName, Color color) {
      this.iconName = iconName;
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
    labelTitle.setFont(DtaqFonts.LABEL);
    labelTitle.setForeground(DtaqColors.TEXT_MUTED);
    add(labelTitle, "wrap");

    valueLabel.setText(initialValue != null ? initialValue : EMPTY_VALUE);
    valueLabel.setFont(DtaqFonts.KPI_VALUE);
    valueLabel.setForeground(DtaqColors.TEXT);
    add(valueLabel, "wrap");

    captionLabel.setFont(DtaqFonts.CAPTION);
    setCaption(captionText);
    // Luôn giữ chỗ một dòng chú thích để thẻ không chú thích cao bằng thẻ bên cạnh.
    add(captionLabel, "wrap, hmin 16");
  }

  public void setValue(String value) {
    valueLabel.setText(value == null || value.isBlank() ? EMPTY_VALUE : value);
    // Giá trị dài thì hạ cỡ chữ theo bậc để không bị cắt số.
    int len = valueLabel.getText().length();
    valueLabel.setFont(len > 22 ? DtaqFonts.numeric(true, 18f, -0.2f)
        : len > 16 ? DtaqFonts.numeric(true, 22f, -0.4f) : DtaqFonts.KPI_VALUE);
  }

  public void setCaption(String caption) {
    // Chú thích rỗng vẫn giữ một dấu cách để dòng không co về 0.
    captionLabel.setText(caption == null || caption.isEmpty() ? " " : caption);
    captionLabel.setIcon(null);
    captionLabel.setForeground(Trend.NEUTRAL.color);
  }

  public void setTrend(Trend trend, String text) {
    captionLabel.setText(text != null ? text : "");
    captionLabel.setIcon(trend.iconName == null ? null : DtaqIcons.icon(trend.iconName, 14, trend.color));
    captionLabel.setIconTextGap(4);
    captionLabel.setForeground(trend.color);
  }

  /** Trạng thái đang tải: giá trị "—" và bỏ chú thích. */
  public void setLoading() {
    valueLabel.setText(EMPTY_VALUE);
    captionLabel.setText("Đang tải…");
    captionLabel.setIcon(null);
    captionLabel.setForeground(Trend.NEUTRAL.color);
  }
}
