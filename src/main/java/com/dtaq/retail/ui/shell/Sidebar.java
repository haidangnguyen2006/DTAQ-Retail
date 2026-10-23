package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.AppInfo;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
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
    super(new MigLayout("fill, insets 24 16 22 16", "[grow]", "[]28[]10[]push[]16[]"));
    Objects.requireNonNull(nhanVien, "nhanVien");

    setBackground(DtaqColors.SURFACE);
    setPreferredSize(new Dimension(DtaqSpacing.SIDEBAR_W, 0));
    setMinimumSize(new Dimension(DtaqSpacing.SIDEBAR_W, 0));
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DtaqColors.BORDER));

    // 1. Logo Brand
    add(buildBrandBlock(), "growx, wrap");

    // 2. Overline
    JLabel overline = new JLabel("KHÔNG GIAN LÀM VIỆC");
    overline.setFont(DtaqFonts.OVERLINE);
    overline.setForeground(DtaqColors.TEXT_SUBTLE);
    add(overline, "growx, wrap");

    // 3. Navigation items
    JPanel navList = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]4[]"));
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

    // 4. Info Card
    add(buildStationCard(nhanVien), "growx, wrap");

    // 5. Version
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

    JPanel logo = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.PRIMARY);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_CARD_SM, DtaqSpacing.RADIUS_CARD_SM));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    logo.setOpaque(false);
    logo.setPreferredSize(new Dimension(36, 36));
    logo.setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
    logo.add(new JLabel(DtaqIcons.icon("zap", 22, Color.WHITE)));

    panel.add(logo, "align left");

    JPanel textBlock = new JPanel(new MigLayout("insets 0", "[]", "[]0[]"));
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

  private JPanel buildStationCard(NhanVien nhanVien) {
    JPanel card = new JPanel(new MigLayout("fillx, insets 14", "[grow]", "[]4[]8[]")) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.BG_APP);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_CARD_SM, DtaqSpacing.RADIUS_CARD_SM));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    card.setOpaque(false);

    JLabel title = new JLabel("DTAQ • Siêu thị điện máy");
    title.setFont(DtaqFonts.TABLE_HEADER);
    title.setForeground(DtaqColors.TEXT);
    card.add(title, "wrap");

    JLabel spaceRole = new JLabel(nhanVien.moTaQuyen());
    spaceRole.setFont(DtaqFonts.CAPTION);
    spaceRole.setForeground(DtaqColors.TEXT_MUTED);
    card.add(spaceRole, "wrap");

    JPanel statusRow = new JPanel(new MigLayout("insets 0", "[]6[]", "[center]"));
    statusRow.setOpaque(false);

    JPanel dot = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.PRIMARY);
        g2.fillOval(0, 0, 6, 6);
        g2.dispose();
      }
    };
    dot.setOpaque(false);
    dot.setPreferredSize(new Dimension(6, 6));
    statusRow.add(dot);

    JLabel ready = new JLabel("Hệ thống sẵn sàng");
    ready.setFont(DtaqFonts.HINT);
    ready.setForeground(DtaqColors.PRIMARY);
    statusRow.add(ready);

    card.add(statusRow);
    return card;
  }
}
