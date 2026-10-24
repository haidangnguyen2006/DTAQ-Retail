package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh gợi ý phím tắt bên dưới màn hình lập hóa đơn.
 */
public class ShortcutBar extends JPanel {

  public record ShortcutItem(String key, String desc) {}

  public ShortcutBar(List<ShortcutItem> items) {
    super(new MigLayout("insets 0", "[]20[]", "[]"));
    setOpaque(false);

    for (ShortcutItem item : items) {
      JLabel lbl = new JLabel("<html><b>" + item.key() + "</b>  " + item.desc() + "</html>");
      lbl.setFont(DtaqFonts.HINT);
      lbl.setForeground(DtaqColors.TEXT_MUTED);
      add(lbl);
    }
  }

  public static ShortcutBar invoiceDefaults() {
    return new ShortcutBar(List.of(
        new ShortcutItem("F2", "Khách hàng"),
        new ShortcutItem("F3", "Sản phẩm"),
        new ShortcutItem("F8", "Thanh toán"),
        new ShortcutItem("↑ ↓", "Chọn kết quả")
    ));
  }
}
