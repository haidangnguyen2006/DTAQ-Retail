package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.Tile;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PersonNames;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh tiêu đề trên cùng (Topbar): breadcrumb, ngày, thông tin người dùng và nút đăng xuất.
 */
public class Topbar extends JPanel {

  private static final int AVATAR_SIZE = 32;

  private final JLabel sectionLabel = new JLabel();
  private final JLabel pageLabel = new JLabel();

  public Topbar(NhanVien nhanVien, Runnable onLogout) {
    super(new MigLayout("fill, insets 0 " + DtaqSpacing.PAGE_PADDING + " 0 " + DtaqSpacing.PAGE_PADDING,
        "[grow][]", "[center]"));
    Objects.requireNonNull(nhanVien, "nhanVien");

    setBackground(DtaqColors.SURFACE);
    setPreferredSize(new Dimension(0, DtaqSpacing.TOPBAR_H));
    setMinimumSize(new Dimension(0, DtaqSpacing.TOPBAR_H));
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DtaqColors.BORDER));

    add(buildBreadcrumb(), "align left, aligny center");
    add(buildUserArea(nhanVien, onLogout), "align right, aligny center");
  }

  private JPanel buildBreadcrumb() {
    JPanel breadcrumb = new JPanel(new MigLayout("insets 0, gapx 10", "[][][]", "[center]"));
    breadcrumb.setOpaque(false);

    sectionLabel.setFont(DtaqFonts.BODY_13);
    sectionLabel.setForeground(DtaqColors.TEXT_MUTED);

    JLabel slashLabel = new JLabel("/");
    slashLabel.setFont(DtaqFonts.BODY_13);
    slashLabel.setForeground(DtaqColors.TEXT_SUBTLE);

    pageLabel.setFont(DtaqFonts.BODY_13_MEDIUM);
    pageLabel.setForeground(DtaqColors.TEXT);

    breadcrumb.add(sectionLabel);
    breadcrumb.add(slashLabel);
    breadcrumb.add(pageLabel);
    return breadcrumb;
  }

  private JPanel buildUserArea(NhanVien nhanVien, Runnable onLogout) {
    JPanel area = new JPanel(new MigLayout("insets 0, gapx 12", "[][][][]", "[center]"));
    area.setOpaque(false);

    JLabel dateLabel = new JLabel(DateFormats.formatDate(LocalDate.now()));
    dateLabel.setFont(DtaqFonts.CAPTION);
    dateLabel.setForeground(DtaqColors.TEXT_MUTED);
    area.add(dateLabel, "gapright 6");

    Tile avatar = new Tile(DtaqColors.PRIMARY_SOFT, Tile.OVAL);
    avatar.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
    JLabel initials = new JLabel(PersonNames.initials(nhanVien.hoTen()));
    initials.setFont(DtaqFonts.PILL);
    initials.setForeground(DtaqColors.PRIMARY);
    avatar.add(initials);
    area.add(avatar);

    JPanel userBlock = new JPanel(new MigLayout("insets 0, gapy 1", "[]", "[]"));
    userBlock.setOpaque(false);
    JLabel nameLabel = new JLabel(nhanVien.hoTen());
    nameLabel.setFont(DtaqFonts.TABLE_HEADER);
    nameLabel.setForeground(DtaqColors.TEXT);
    userBlock.add(nameLabel, "wrap");
    JLabel roleLabel = new JLabel(nhanVien.moTaQuyen());
    roleLabel.setFont(DtaqFonts.HINT);
    roleLabel.setForeground(DtaqColors.TEXT_MUTED);
    userBlock.add(roleLabel);
    area.add(userBlock);

    JButton logoutBtn = new JButton("Đăng xuất", DtaqIcons.icon("log-out", 17, DtaqColors.TEXT_MUTED));
    logoutBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    logoutBtn.setFont(DtaqFonts.PILL);
    logoutBtn.setForeground(DtaqColors.TEXT);
    logoutBtn.setIconTextGap(7);
    logoutBtn.setFocusPainted(false);
    logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    logoutBtn.addActionListener(e -> ConfirmDialog.confirm(
        this, "Đăng xuất", "Bạn có chắc muốn đăng xuất?", "Đăng xuất",
        () -> {
          if (onLogout != null) {
            onLogout.run();
          }
        }));
    area.add(logoutBtn);
    return area;
  }

  public void setBreadcrumb(String section, String pageTitle) {
    sectionLabel.setText(section);
    pageLabel.setText(pageTitle);
  }
}
