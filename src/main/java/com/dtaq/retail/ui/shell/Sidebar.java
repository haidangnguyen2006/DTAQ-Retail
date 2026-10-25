package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Tile;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.AppInfo;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh điều hướng dọc bên trái ứng dụng (Sidebar) theo đặc tả Figma.
 */
public class Sidebar extends JPanel {

  private final Map<PageId, NavItem> navItemMap = new HashMap<>();
  private Consumer<PageId> onNavigateListener;

  public Sidebar(NhanVien nhanVien) {
    super(new MigLayout("fill, insets 20 16 16 16", "[grow]", "[]24[]8[]push[]12[]"));
    Objects.requireNonNull(nhanVien, "nhanVien");

    setBackground(DtaqColors.SURFACE);
    setPreferredSize(new Dimension(DtaqSpacing.SIDEBAR_W, 0));
    setMinimumSize(new Dimension(DtaqSpacing.SIDEBAR_W, 0));
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DtaqColors.BORDER));

    add(buildBrandBlock(), "growx, wrap");

    JLabel overline = new JLabel("KHÔNG GIAN LÀM VIỆC");
    overline.setFont(DtaqFonts.OVERLINE);
    overline.setForeground(DtaqColors.TEXT_SUBTLE);
    add(overline, "growx, wrap");

    JPanel navList = new JPanel(new MigLayout("fillx, insets 0, gapy 4", "[grow]", "[]"));
    navList.setOpaque(false);
    List<NavigationModel.NavEntry> entries = NavigationModel.getEntriesFor(nhanVien.loaiQuyen());
    for (NavigationModel.NavEntry entry : entries) {
      NavItem item = new NavItem(entry.iconName(), entry.label());
      item.setOnClick(() -> {
        if (onNavigateListener != null) {
          onNavigateListener.accept(entry.pageId());
        }
      });
      navItemMap.put(entry.pageId(), item);
      navList.add(item, "growx, wrap");
    }
    add(navList, "growx, wrap");

    add(buildSessionCard(nhanVien), "growx, wrap");

    JLabel versionLabel = new JLabel("Nội bộ DTAQ · Phiên bản " + AppInfo.version());
    versionLabel.setFont(DtaqFonts.MICRO);
    versionLabel.setForeground(DtaqColors.TEXT_SUBTLE);
    add(versionLabel, "center");
  }

  public void selectPage(PageId pageId) {
    for (Map.Entry<PageId, NavItem> entry : navItemMap.entrySet()) {
      entry.getValue().setActive(entry.getKey() == pageId);
    }
  }

  public void setOnNavigate(Consumer<PageId> listener) {
    this.onNavigateListener = listener;
  }

  private JPanel buildBrandBlock() {
    JPanel panel = new JPanel(new MigLayout("insets 0 4 0 0", "[]10[grow]", "[center]"));
    panel.setOpaque(false);

    panel.add(Tile.icon("zap", 22, DtaqColors.SURFACE, DtaqColors.PRIMARY, 36, DtaqSpacing.RADIUS_CARD_SM), "align left");

    JPanel textBlock = new JPanel(new MigLayout("insets 0, gapy 0", "[]", "[]0[]"));
    textBlock.setOpaque(false);

    JLabel brand = new JLabel("DTAQ");
    brand.setFont(DtaqFonts.BRAND);
    brand.setForeground(DtaqColors.TEXT);
    textBlock.add(brand, "wrap");

    JLabel sub = new JLabel("ĐIỆN MÁY");
    sub.setFont(DtaqFonts.OVERLINE);
    sub.setForeground(DtaqColors.TEXT_MUTED);
    textBlock.add(sub);

    panel.add(textBlock, "growx");
    return panel;
  }

  /** Thẻ thông tin phiên: tên hệ thống và vai trò của người đăng nhập (chỉ dữ liệu thật). */
  private JPanel buildSessionCard(NhanVien nhanVien) {
    Tile card = new Tile(DtaqColors.BG_APP, DtaqSpacing.RADIUS_CARD_SM);
    card.setLayout(new MigLayout("fillx, insets 12 14 12 14, gapy 3", "[grow]", "[]"));

    JLabel title = new JLabel("DTAQ • Siêu thị điện máy");
    title.setFont(DtaqFonts.TABLE_HEADER);
    title.setForeground(DtaqColors.TEXT);
    card.add(title, "wrap");

    JLabel role = new JLabel(DtaqIcons.icon("user-round", 14, DtaqColors.TEXT_MUTED));
    role.setText(nhanVien.moTaQuyen());
    role.setIconTextGap(6);
    role.setFont(DtaqFonts.CAPTION);
    role.setForeground(DtaqColors.TEXT_MUTED);
    card.add(role);
    return card;
  }
}
