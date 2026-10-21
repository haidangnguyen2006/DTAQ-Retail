package com.dtaq.retail.ui;

import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.util.AppInfo;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cửa sổ đăng nhập của ứng dụng. */
public final class LoginFrame extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(LoginFrame.class);

  private static final Color BRAND_BACKGROUND = new Color(0x0F766E);
  private static final Color ERROR_FOREGROUND = new Color(0xDC2626);

  private static final String MISSING_FIELDS_MESSAGE =
      "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu";
  private static final String INVALID_CREDENTIALS_MESSAGE =
      "Tên đăng nhập hoặc mật khẩu không đúng";
  private static final String LOCKED_MESSAGE =
      "Tài khoản đã bị khóa. Vui lòng liên hệ nhân viên quản lý";
  private static final String DATA_ERROR_MESSAGE =
      "Không thể đăng nhập lúc này. Vui lòng thử lại sau";

  private final AuthController controller;
  private final AppNavigator navigator;

  private final JTextField taiKhoanField = new JTextField();
  private final JPasswordField matKhauField = new JPasswordField();
  private final JLabel loiLabel = new JLabel("");
  private final JButton dangNhapButton = new JButton("Đăng nhập");
  private final JButton quenMatKhauButton = new JButton("Không thể đăng nhập? Quên mật khẩu!");

  /**
   * Khởi tạo cửa sổ đăng nhập.
   *
   * @param controller bộ điều khiển đăng nhập; không được {@code null}
   * @param navigator bộ điều phối màn hình; không được {@code null}
   */
  public LoginFrame(AuthController controller, AppNavigator navigator) {
    super(AppInfo.APP_NAME);
    this.controller = Objects.requireNonNull(controller, "controller");
    this.navigator = Objects.requireNonNull(navigator, "navigator");

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setResizable(false);
    setLayout(new MigLayout("fill, insets 0", "[360!][grow]", "[grow]"));

    add(buildBrandPanel(), "grow");
    add(buildFormPanel(), "grow");

    pack();
    setMinimumSize(new Dimension(860, 520));
    AppInfo.centerOnScreen(this);
  }

  private JPanel buildBrandPanel() {
    JPanel panel = new JPanel(new MigLayout("fill, insets 40 32 40 32", "[grow]", "[]20[]push"));
    panel.setBackground(BRAND_BACKGROUND);

    JLabel name = new JLabel("DTAQ");
    name.setForeground(Color.WHITE);
    name.setFont(name.getFont().deriveFont(Font.BOLD, 40f));
    panel.add(name, "wrap");

    JLabel tagline = new JLabel("ĐIỆN MÁY");
    tagline.setForeground(Color.WHITE);
    tagline.setFont(tagline.getFont().deriveFont(Font.PLAIN, 18f));
    panel.add(tagline, "wrap");

    return panel;
  }

  private JPanel buildFormPanel() {
    JPanel panel = new JPanel(new MigLayout("fill, insets 48 56 48 56", "[grow, fill]", "[]"));

    JLabel subtitle = new JLabel("CHÀO MỪNG TRỞ LẠI");
    subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 12f));
    panel.add(subtitle, "wrap");

    JLabel title = new JLabel("Đăng nhập DTAQ");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
    panel.add(title, "wrap, gaptop 4, gapbottom 24");

    JLabel taiKhoanLabel = new JLabel("Tài khoản");
    panel.add(taiKhoanLabel, "wrap, gapbottom 4");
    taiKhoanField.setName("taiKhoan");
    taiKhoanField.addActionListener(event -> submit());
    panel.add(taiKhoanField, "growx, h 36!, wrap, gapbottom 16");

    JLabel matKhauLabel = new JLabel("Mật khẩu");
    panel.add(matKhauLabel, "wrap, gapbottom 4");
    matKhauField.setName("matKhau");
    matKhauField.addActionListener(event -> submit());
    panel.add(matKhauField, "growx, h 36!, wrap, gapbottom 8");

    loiLabel.setForeground(ERROR_FOREGROUND);
    panel.add(loiLabel, "growx, wrap, gapbottom 16");

    dangNhapButton.addActionListener(event -> submit());
    panel.add(dangNhapButton, "growx, h 40!, wrap");

    panel.add(buildForgotPasswordButton(), "growx, gaptop 8");

    return panel;
  }

  private JButton buildForgotPasswordButton() {
    quenMatKhauButton.setBorderPainted(false);
    quenMatKhauButton.setContentAreaFilled(false);
    quenMatKhauButton.setFocusPainted(false);
    quenMatKhauButton.setForeground(BRAND_BACKGROUND);
    quenMatKhauButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    quenMatKhauButton.addActionListener(event -> navigator.showForgotPassword());
    return quenMatKhauButton;
  }

  private void submit() {
    if (!dangNhapButton.isEnabled()) {
      return;
    }

    String tenDangNhap = taiKhoanField.getText();
    String matKhau = new String(matKhauField.getPassword());

    dangNhapButton.setEnabled(false);
    loiLabel.setText("");
    getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    SwingWorker<AuthController.LoginResult, Void> worker =
        new SwingWorker<>() {
          @Override
          protected AuthController.LoginResult doInBackground() {
            return controller.login(tenDangNhap, matKhau);
          }

          @Override
          protected void done() {
            try {
              AuthController.LoginResult result = get();
              if (result.thanhCong()) {
                matKhauField.setText("");
                navigator.showMain(result.nhanVien());
              } else {
                loiLabel.setText(messageFor(result.outcome()));
                if (result.outcome() == AuthController.Outcome.MISSING_FIELDS
                    || result.outcome() == AuthController.Outcome.INVALID_CREDENTIALS) {
                  matKhauField.setText("");
                  matKhauField.requestFocusInWindow();
                }
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              loiLabel.setText(DATA_ERROR_MESSAGE);
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              log.warn("Đăng nhập thất bại do lỗi không mong đợi (exception={})",
                  cause == null ? "none" : cause.getClass().getName());
              loiLabel.setText(DATA_ERROR_MESSAGE);
            } finally {
              dangNhapButton.setEnabled(true);
              getRootPane().setCursor(null);
            }
          }
        };
    worker.execute();
  }

  private String messageFor(AuthController.Outcome outcome) {
    return switch (outcome) {
      case MISSING_FIELDS -> MISSING_FIELDS_MESSAGE;
      case INVALID_CREDENTIALS -> INVALID_CREDENTIALS_MESSAGE;
      case LOCKED -> LOCKED_MESSAGE;
      case DATA_ERROR -> DATA_ERROR_MESSAGE;
      case SUCCESS -> "";
    };
  }
}
