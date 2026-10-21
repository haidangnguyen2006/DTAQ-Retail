package com.dtaq.retail.ui;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.util.AppInfo;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/** Main application window for the signed-in employee. */
public class MainFrame extends JFrame {

  /**
   * Builds the main window for the signed-in employee.
   *
   * @param nhanVien the employee owning the current session
   * @param onLogout callback invoked after the session is cleared from this window
   */
  public MainFrame(NhanVien nhanVien, Runnable onLogout) {
    super(AppInfo.APP_NAME);
    if (nhanVien == null) {
      throw new IllegalArgumentException("nhanVien must not be null");
    }
    if (onLogout == null) {
      throw new IllegalArgumentException("onLogout must not be null");
    }

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new MigLayout("fill, insets 20", "[grow][]", "[][grow]"));

    JLabel userHeader = new JLabel(nhanVien.hoTen() + " · " + nhanVien.moTaQuyen());
    add(userHeader, "growx");

    JButton logoutButton = new JButton("Đăng xuất");
    logoutButton.addActionListener(event -> confirmLogout(onLogout));
    add(logoutButton, "wrap");

    JPanel center = new JPanel(new MigLayout("fill"));
    center.add(new JLabel("Chào mừng đến DTAQ Retail", SwingConstants.CENTER), "center, wrap");
    center.add(new JLabel(nhanVien.maNV(), SwingConstants.CENTER), "center");
    add(center, "grow, push");

    pack();
    setMinimumSize(new Dimension(800, 520));
    AppInfo.centerOnScreen(this);
  }

  private void confirmLogout(Runnable onLogout) {
    int choice = JOptionPane.showConfirmDialog(
        this,
        "Bạn có chắc muốn đăng xuất?",
        "Xác nhận đăng xuất",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE);
    if (choice == JOptionPane.YES_OPTION) {
      dispose();
      onLogout.run();
    }
  }
}
