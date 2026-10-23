package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PersonNames;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh tiêu đề trên cùng (Topbar) theo chuẩn Figma.
 */
public class Topbar extends JPanel {

  private final JLabel sectionLabel = new JLabel();
  private final JLabel slashLabel = new JLabel("/");
  private final JLabel pageLabel = new JLabel();

  public Topbar(NhanVien nhanVien, Runnable onLogout) {
    super(new MigLayout("fill, insets 0 32 0 32", "[grow][]", "[center]"));
    Objects.requireNonNull(nhanVien, "nhanVien");

    setBackground(DtaqColors.SURFACE);
    setPreferredSize(new Dimension(0, DtaqSpacing.TOPBAR_H));
    setMinimumSize(new Dimension(0, DtaqSpacing.TOPBAR_H));
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DtaqColors.BORDER));

    // Breadcrumb (Left)
    JPanel breadcrumb = new JPanel(new MigLayout("insets 0", "[]10[]10[]", "[center]"));
    breadcrumb.setOpaque(false);

    sectionLabel.setFont(DtaqFonts.BODY_13);
    sectionLabel.setForeground(DtaqColors.TEXT_MUTED);

    slashLabel.setFont(DtaqFonts.BODY_13);
    slashLabel.setForeground(DtaqColors.TEXT_SUBTLE);

    pageLabel.setFont(DtaqFonts.BODY_13_MEDIUM);
    pageLabel.setForeground(DtaqColors.TEXT);

    breadcrumb.add(sectionLabel);
    breadcrumb.add(slashLabel);
    breadcrumb.add(pageLabel);

    add(breadcrumb, "align left, aligny center");

    // User session & logout (Right)
    JPanel rightPanel = new JPanel(new MigLayout("insets 0", "[]18[]12[]18[]", "[center]"));
    rightPanel.setOpaque(false);

    // Current date
    JLabel dateLabel = new JLabel(DateFormats.formatDate(LocalDate.now()));
    dateLabel.setFont(DtaqFonts.CAPTION);
    dateLabel.setForeground(DtaqColors.TEXT_MUTED);
    rightPanel.add(dateLabel);

    // Avatar 32x32 circle
    String initials = PersonNames.initials(nhanVien.hoTen());
    JPanel avatar = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.PRIMARY_SOFT);
        g2.fillOval(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
      }
    };
    avatar.setOpaque(false);
    avatar.setPreferredSize(new Dimension(32, 32));
    avatar.setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
    JLabel initialsLabel = new JLabel(initials);
    initialsLabel.setFont(DtaqFonts.CAPTION);
    initialsLabel.setForeground(DtaqColors.PRIMARY);
    avatar.add(initialsLabel);
    rightPanel.add(avatar);

    // Name + Role block
    JPanel userBlock = new JPanel(new MigLayout("insets 0", "[]", "[]2[]"));
    userBlock.setOpaque(false);

    JLabel nameLabel = new JLabel(nhanVien.hoTen());
    nameLabel.setFont(DtaqFonts.TABLE_HEADER);
    nameLabel.setForeground(DtaqColors.TEXT);
    userBlock.add(nameLabel, "wrap");

    JLabel roleLabel = new JLabel(nhanVien.moTaQuyen());
    roleLabel.setFont(DtaqFonts.MICRO);
    roleLabel.setForeground(DtaqColors.TEXT_MUTED);
    userBlock.add(roleLabel);
    rightPanel.add(userBlock);

    // Logout button
    JButton logoutBtn = new JButton("Đăng xuất", DtaqIcons.icon("log-out", 17, DtaqColors.TEXT_MUTED));
    logoutBtn.setFont(DtaqFonts.PILL);
    logoutBtn.setForeground(DtaqColors.TEXT);
    logoutBtn.setIconTextGap(7);
    logoutBtn.setBorderPainted(false);
    logoutBtn.setContentAreaFilled(false);
    logoutBtn.setFocusPainted(false);
    logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    logoutBtn.addActionListener(e -> {
      int choice = JOptionPane.showConfirmDialog(
          Topbar.this,
          "Bạn có chắc muốn đăng xuất?",
          "Xác nhận đăng xuất",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (choice == JOptionPane.YES_OPTION && onLogout != null) {
        onLogout.run();
      }
    });
    rightPanel.add(logoutBtn);

    add(rightPanel, "align right, aligny center");
  }

  public void setBreadcrumb(String section, String pageTitle) {
    sectionLabel.setText(section);
    pageLabel.setText(pageTitle);
  }
}
