package com.dtaq.retail.ui.shell;

import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.EmptyState;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Trang tạm cho các mục menu chưa có giao diện thật.
 *
 * <p>Mỗi mục menu trong {@link NavigationModel} đều phải được đăng ký với shell, nếu không
 * {@code AppShellFrame.showPage} sẽ bỏ qua im lặng và mục menu trở thành "chết". Lớp này dựng một
 * khung trang tối giản theo design system (PageHeader + Card chứa EmptyState) để báo rằng chức năng
 * đang được hoàn thiện theo đặc tả Use Case tương ứng.</p>
 *
 * <p>{@link #view()} chỉ trả về panel thô, không tự bọc {@code ViewportPanel} hay
 * {@code JScrollPane}: {@link AppShellFrame} đảm nhiệm việc cuộn dựa trên {@link #scrollable()}.</p>
 */
public class PlaceholderPage implements Page {

  private final PageId id;
  private final String title;
  private final String section;
  private final String iconName;
  private final JPanel view;

  /**
   * Khởi tạo trang tạm.
   *
   * @param id định danh trang; không được {@code null}
   * @param title tiêu đề hiển thị trên header và breadcrumb; không được {@code null}
   * @param section nhóm menu chứa trang; không được {@code null}
   * @param iconName tên tệp icon SVG trong resources; không được {@code null}
   */
  public PlaceholderPage(PageId id, String title, String section, String iconName) {
    this.id = Objects.requireNonNull(id, "id");
    this.title = Objects.requireNonNull(title, "title");
    this.section = Objects.requireNonNull(section, "section");
    this.iconName = Objects.requireNonNull(iconName, "iconName");

    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        title, "Chức năng đang được hoàn thiện theo đặc tả Use Case.");
    view.add(header, "growx, wrap");

    Card card = Card.standard();
    card.setBody(new EmptyState(
        iconName,
        "Đang phát triển giao diện",
        "Chức năng “" + title + "” sẽ được hoàn thiện cùng Use Case tương ứng."));
    view.add(card, "grow, push");
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
