package com.dtaq.retail.ui.shell;

import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.EmptyState;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Trang tạm (Placeholder Page) dùng làm khung hiển thị trước khi gắn chức năng chi tiết.
 */
public class PlaceholderPage implements Page {

  private final PageId id;
  private final String title;
  private final String section;
  private final String iconName;
  private final JPanel view;

  public PlaceholderPage(PageId id, String title, String section, String iconName) {
    this.id = id;
    this.title = title;
    this.section = section;
    this.iconName = iconName;

    this.view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"
    ));
    this.view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(title, "Trang chức năng nội bộ siêu thị điện máy DTAQ");
    this.view.add(header, "growx, wrap");

    Card contentCard = Card.standard();
    EmptyState emptyState = new EmptyState(
        iconName,
        "Đang phát triển giao diện",
        "Chức năng " + title + " đang được hoàn thiện theo kịch bản Use Case tương ứng."
    );
    contentCard.add(emptyState, "center");
    this.view.add(contentCard, "grow, push");
  }

  @Override
  public PageId id() {
    return id;
  }

  @Override
  public String title() {
    return title;
  }

  @Override
  public String section() {
    return section;
  }

  @Override
  public JComponent view() {
    return view;
  }
}
