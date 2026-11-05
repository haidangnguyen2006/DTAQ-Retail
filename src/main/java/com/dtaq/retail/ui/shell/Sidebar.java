package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Tile;
import com.dtaq.retail.ui.format.PersonNames;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.ui.theme.DtaqTheme;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh điều hướng dọc bên trái (nền tối), gom mục theo nhóm và có chế độ thu gọn thành dải icon.
 * Thứ tự mục trong mỗi nhóm giữ theo {@link NavigationModel}; nhóm chỉ là cách trình bày.
 */
public class Sidebar extends JPanel {

  private static final String OTHER = "KHÁC";
  /** Thứ tự các nhóm và nhóm của từng mục (theo id trong {@link NavigationModel}). */
  private static final List<String> GROUP_ORDER = List.of(
      "TỔNG QUAN", "BÁN HÀNG", "BÁO CÁO", "PHÊ DUYỆT", "KHÁCH HÀNG", "HÀNG HÓA", "NHÂN SỰ", "HỆ THỐNG");
  private static final Map<String, String> GROUP_OF = Map.ofEntries(
      Map.entry("home", "TỔNG QUAN"),
      Map.entry("invoice", "BÁN HÀNG"), Map.entry("returns", "BÁN HÀNG"),
      Map.entry("revenue", "BÁO CÁO"), Map.entry("sales_by_product", "BÁO CÁO"),
      Map.entry("sales_by_employee", "BÁO CÁO"), Map.entry("long_stock", "BÁO CÁO"),
      Map.entry("return_approvals", "PHÊ DUYỆT"),
      Map.entry("customers", "KHÁCH HÀNG"), Map.entry("customer_lookup", "KHÁCH HÀNG"),
      Map.entry("product_lookup", "HÀNG HÓA"), Map.entry("products", "HÀNG HÓA"),
      Map.entry("product_lines", "HÀNG HÓA"), Map.entry("categories", "HÀNG HÓA"),
      Map.entry("defective_products", "HÀNG HÓA"), Map.entry("price_tax", "HÀNG HÓA"),
      Map.entry("promotions", "HÀNG HÓA"),
      Map.entry("employees", "NHÂN SỰ"),
      Map.entry("backup", "HỆ THỐNG"), Map.entry("restore", "HỆ THỐNG"));

  private final NhanVien nhanVien;
  private final Map<PageId, NavItem> navItemMap = new HashMap<>();
  private final List<NavItem> navItems = new ArrayList<>();
  private final List<JLabel> groupLabels = new ArrayList<>();
  private final List<JPanel> groupDividers = new ArrayList<>();
  private static final int LOGO_H = 34;
  private final JLabel logoLabel = new JLabel();
  private final JPanel brandText = new JPanel(new MigLayout("insets 0, gapy 0", "[]", "[]"));
  private Consumer<PageId> onNavigateListener;
  private boolean compact;
  private int expandedWidth = DtaqSpacing.SIDEBAR_W;

  public Sidebar(NhanVien nhanVien) {
    super(new MigLayout("fill, insets 0, hidemode 3", "[grow,fill]", "[]0[grow,fill]"));
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    setBackground(DtaqColors.SIDEBAR_BG);
    applyWidth();

    add(buildBrandBlock(), "growx, h " + DtaqSpacing.TOPBAR_H + "!, wrap");
    add(buildNavScroll(), "grow");
  }

  public void selectPage(PageId pageId) {
    for (Map.Entry<PageId, NavItem> entry : navItemMap.entrySet()) {
      entry.getValue().setActive(entry.getKey() == pageId);
    }
  }

  public void setOnNavigate(Consumer<PageId> listener) {
    this.onNavigateListener = listener;
  }

  public boolean isCompact() {
    return compact;
  }

  /** Thu gọn thành dải icon hoặc mở rộng đầy đủ (tức thì, không hoạt ảnh bề rộng để tránh giật layout). */
  public void setCompact(boolean compact) {
    this.compact = compact;
    applyWidth();
    brandText.setVisible(!compact);
    logoLabel.setIcon(compact ? DtaqTheme.logoMark(LOGO_H) : DtaqTheme.logo(LOGO_H));
    groupLabels.forEach(l -> l.setVisible(!compact));
    groupDividers.forEach(d -> d.setVisible(compact));
    navItems.forEach(i -> i.setCompact(compact));
    revalidate();
    repaint();
  }

  /** Độ rộng mở rộng được giữ lại khi thu gọn rồi mở lại. */
  public void setExpandedWidth(int width) {
    expandedWidth = Math.max(220, Math.min(420, width));
    applyWidth();
    revalidate();
    if (getParent() != null) getParent().revalidate();
  }

  private void applyWidth() {
    int w = compact ? DtaqSpacing.SIDEBAR_RAIL_W : expandedWidth;
    setPreferredSize(new Dimension(w, 0));
    setMinimumSize(new Dimension(w, 0));
  }

  private JPanel buildBrandBlock() {
    JPanel panel = new JPanel(new MigLayout("fill, insets 0 16 0 12, hidemode 3", "[]10[grow]", "[center]"));
    panel.setOpaque(false);
    panel.setPreferredSize(new Dimension(0, DtaqSpacing.TOPBAR_H));
    panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DtaqColors.SIDEBAR_DIVIDER));

    logoLabel.setIcon(DtaqTheme.logo(LOGO_H));
    logoLabel.setToolTipText("DTAQ Điện máy");
    panel.add(logoLabel, "align left");

    brandText.setOpaque(false);
    panel.add(brandText, "growx");
    return panel;
  }

  private JScrollPane buildNavScroll() {
    JPanel navList = new JPanel(new MigLayout("fillx, insets 8 12 8 12, gapy 2, hidemode 3", "[grow,fill]", "[]"));
    navList.setOpaque(false);

    Map<String, List<NavigationModel.NavEntry>> groups = new LinkedHashMap<>();
    GROUP_ORDER.forEach(g -> groups.put(g, new ArrayList<>()));
    groups.put(OTHER, new ArrayList<>());
    for (NavigationModel.NavEntry entry : NavigationModel.getEntriesFor(nhanVien.loaiQuyen())) {
      groups.get(GROUP_OF.getOrDefault(entry.id(), OTHER)).add(entry);
    }

    boolean first = true;
    for (Map.Entry<String, List<NavigationModel.NavEntry>> group : groups.entrySet()) {
      if (group.getValue().isEmpty()) {
        continue;
      }
      JLabel label = new JLabel(group.getKey());
      label.setFont(DtaqFonts.OVERLINE);
      label.setForeground(DtaqColors.SIDEBAR_TEXT_MUTED);
      groupLabels.add(label);
      navList.add(label, "gaptop " + (first ? 6 : 16) + ", gapbottom 4, gapleft 6, wrap");

      JPanel divider = new JPanel();
      divider.setBackground(DtaqColors.SIDEBAR_DIVIDER);
      divider.setPreferredSize(new Dimension(32, 1));
      divider.setVisible(false);
      groupDividers.add(divider);
      navList.add(divider, "gaptop " + (first ? 0 : 8) + ", gapbottom 4, align center, w 32!, h 1!, wrap");
      first = false;

      for (NavigationModel.NavEntry entry : group.getValue()) {
        NavItem item = new NavItem(entry.iconName(), entry.label());
        item.setOnClick(() -> {
          if (onNavigateListener != null) {
            onNavigateListener.accept(entry.pageId());
          }
        });
        navItemMap.put(entry.pageId(), item);
        navItems.add(item);
        navList.add(item, "growx, wrap");
      }
    }

    // Danh sách bám bề rộng khung cuộn để nhãn dài tự xuống dòng.
    JPanel tracked = new com.dtaq.retail.ui.component.ViewportPanel(navList);
    tracked.setOpaque(false);
    JScrollPane scroll = new JScrollPane(tracked,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setBorder(null);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getVerticalScrollBar().putClientProperty(
        com.formdev.flatlaf.FlatClientProperties.STYLE, "thumb: #323B4B; hoverThumbColor: #465062");
    return scroll;
  }
}
