package com.dtaq.retail.ui.nav;

import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Panel tạm cho các mục điều hướng chưa hiện thực UI: hiển thị tên tính năng, tham chiếu tài liệu
 * (Use Case / FR) và ghi chú điểm cắm {@link NavContentFactory}.
 *
 * <p>Không chứa logic nghiệp vụ; mục chỉ thấy trên sidebar theo vai trò qua
 * {@code NavItem.withRole(...)}.
 */
public final class PlaceholderPanel extends JPanel {

  /** Màu xám dùng cho nhãn phụ. */
  private static final Color GREY = new Color(0x6B7280);

  /** Mục điều hướng tương ứng; {@link #getName()} là {@code placeholderPanel.<mục>}. */
  private final NavItem item;

  /**
   * Khởi tạo panel tạm cho một mục điều hướng.
   *
   * @param item mục điều hướng; không được {@code null}
   */
  public PlaceholderPanel(NavItem item) {
    super(new MigLayout("fill, insets 32", "", "[]"));
    this.item = Objects.requireNonNull(item, "item");
    setName("placeholderPanel." + item.name());

    JLabel titleLabel = new JLabel(item.label() + " đang được phát triển");
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
    JLabel sourceLabel = new JLabel("Tham chiếu tài liệu: " + item.sourceRef());
    JLabel hookLabel = new JLabel(
        "Điểm cắm: com.dtaq.retail.ui.nav.NavContentFactory.create(...) – thay nhánh tương"
            + " ứng để gắn panel tính năng");
    hookLabel.setForeground(GREY);

    add(titleLabel, "alignx center, wrap");
    add(sourceLabel, "alignx center, wrap");
    add(hookLabel, "alignx center");
  }
}
