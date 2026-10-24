package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thông báo chỉ dẫn (Callout) với 3 mức: thông tin, cảnh báo, lỗi.
 */
public class InfoCallout extends JPanel {

  public enum Kind {
    INFO("info", DtaqColors.PRIMARY_SOFT, DtaqColors.PRIMARY, DtaqColors.PRIMARY_STRONG),
    WARNING("alert-circle", DtaqColors.WARNING_SOFT, DtaqColors.WARNING, DtaqColors.WARNING),
    DANGER("alert-circle", DtaqColors.DANGER_SOFT, DtaqColors.DANGER, DtaqColors.DANGER);

    private final String icon;
    private final Color background;
    private final Color iconColor;
    private final Color textColor;

    Kind(String icon, Color background, Color iconColor, Color textColor) {
      this.icon = icon;
      this.background = background;
      this.iconColor = iconColor;
      this.textColor = textColor;
    }
  }

  private final JLabel textLabel = new JLabel();

  public InfoCallout(String text) {
    this(Kind.INFO, text);
  }

  public InfoCallout(Kind kind, String text) {
    super(new MigLayout("fillx, insets 12 14 12 14", "[]10[grow]", "[]"));
    setOpaque(true);
    setBackground(kind.background);
    putClientProperty(FlatClientProperties.STYLE, "arc: 16");

    add(new JLabel(DtaqIcons.icon(kind.icon, 17, kind.iconColor)), "top");

    textLabel.setFont(DtaqFonts.CAPTION);
    textLabel.setForeground(kind.textColor);
    setText(text);
    add(textLabel, "growx");
  }

  public void setText(String text) {
    textLabel.setText("<html>" + (text == null ? "" : text) + "</html>");
  }
}
