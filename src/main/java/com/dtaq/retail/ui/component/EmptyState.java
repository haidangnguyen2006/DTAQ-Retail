package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Khối hiển thị trạng thái rỗng (Empty State): icon, tiêu đề, gợi ý và (tuỳ chọn) một nút hành động.
 */
public class EmptyState extends JPanel {

  public EmptyState(String iconName, String titleText, String hintText) {
    this(iconName, titleText, hintText, null);
  }

  public EmptyState(String iconName, String titleText, String hintText, DtaqButton action) {
    super(new MigLayout("fill, insets 28 32 28 32", "[center]", "[]12[]4[]"));
    setOpaque(false);

    add(Tile.icon(iconName, 23, DtaqColors.TEXT_SUBTLE, DtaqColors.BG_APP, 46, Tile.OVAL), "center, wrap");

    JLabel title = new JLabel(titleText);
    title.setFont(DtaqFonts.EMPTY_TITLE);
    title.setForeground(DtaqColors.TEXT);
    add(title, "center, wrap");

    if (hintText != null && !hintText.trim().isEmpty()) {
      JLabel hint = new JLabel(hintText);
      hint.setFont(DtaqFonts.CAPTION);
      hint.setForeground(DtaqColors.TEXT_MUTED);
      add(hint, "center, wrap");
    }

    if (action != null) {
      add(action, "center, gaptop 12");
    }
  }
}
