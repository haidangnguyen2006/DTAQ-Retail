package com.dtaq.retail.ui;

import com.dtaq.retail.controller.PasswordResetController;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cửa sổ quên mật khẩu: gửi mã xác nhận qua email rồi đặt mật khẩu mới. */
public final class ForgotPasswordFrame extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(ForgotPasswordFrame.class);

  private static final Color BRAND_BACKGROUND = new Color(0x0F766E);
  private static final Color ERROR_FOREGROUND = new Color(0xDC2626);

  private static final String SENT_MESSAGE =
      "Nếu thông tin hợp lệ, mã xác nhận đã được gửi đến email đã đăng ký.";
  private static final String MISSING_USERNAME_MESSAGE = "Vui lòng nhập tài khoản";
  private static final String SUCCESS_MESSAGE =
      "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.";
  private static final String INVALID_CODE_MESSAGE = "Mã xác nhận không đúng";
  private static final String CODE_EXPIRED_MESSAGE =
      "Mã xác nhận đã hết hạn. Vui lòng gửi lại mã.";
  private static final String TOO_MANY_ATTEMPTS_MESSAGE =
      "Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã.";
  private static final String WEAK_PASSWORD_MESSAGE =
      "Mật khẩu mới phải có ít nhất 8 ký tự và khác tên đăng nhập";
  private static final String DATA_ERROR_MESSAGE =
      "Không thể đặt lại mật khẩu lúc này. Vui lòng thử lại sau";

  private final PasswordResetController controller;
  private final AppNavigator navigator;

  private final JTextField taiKhoanField = new JTextField();
  private final JTextField maXacNhanField = new JTextField();
  private final JPasswordField matKhauMoiField = new JPasswordField();
  private final JLabel trangThaiLabel = new JLabel("");
  private final JButton guiMaButton = new JButton("Gửi mã xác nhận");
  private final JButton xacNhanButton = new JButton("Xác nhận");

  /**
   * Khởi tạo cửa sổ quên mật khẩu.
   *
   * @param controller bộ điều khiển đặt lại mật khẩu; không được {@code null}
   * @param navigator bộ điều phối màn hình; không được {@code null}
   */
  public ForgotPasswordFrame(PasswordResetController controller, AppNavigator navigator) {
    super("Quên mật khẩu - DTAQ");
    this.controller = Objects.requireNonNull(controller, "controller");
    this.navigator = Objects.requireNonNull(navigator, "navigator");

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(false);
    setLayout(new MigLayout("fill, insets 0", "[360!][grow]", "[grow]"));

    add(buildBrandPanel(), "grow");
    add(buildFormPanel(), "grow");

    pack();
    setMinimumSize(new Dimension(860, 560));
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
    JPanel panel = new JPanel(new MigLayout("fill, insets 40 56 40 56", "[grow, fill]", "[]"));

    JLabel subtitle = new JLabel("QUÊN MẬT KHẨU");
    subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 12f));
    panel.add(subtitle, "wrap");

    JLabel title = new JLabel("Cập nhật mật khẩu");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
    panel.add(title, "wrap, gaptop 4, gapbottom 20");

    panel.add(new JLabel("Tài khoản"), "wrap, gapbottom 4");
    taiKhoanField.setName("taiKhoan");
    panel.add(taiKhoanField, "growx, h 36!, wrap, gapbottom 8");

    guiMaButton.addActionListener(event -> guiMa());
    panel.add(guiMaButton, "growx, h 36!, wrap, gapbottom 16");

    panel.add(new JLabel("Mã xác nhận"), "wrap, gapbottom 4");
    maXacNhanField.setName("maXacNhan");
    panel.add(maXacNhanField, "growx, h 36!, wrap, gapbottom 12");

    panel.add(new JLabel("Mật khẩu mới"), "wrap, gapbottom 4");
    matKhauMoiField.setName("matKhauMoi");
    panel.add(matKhauMoiField, "growx, h 36!, wrap, gapbottom 12");

    panel.add(trangThaiLabel, "growx, wrap, gapbottom 12");

    xacNhanButton.addActionListener(event -> xacNhan());
    panel.add(xacNhanButton, "growx, h 40!, wrap");

    panel.add(buildBackToLoginButton(), "growx, gaptop 8");

    return panel;
  }

  private JButton buildBackToLoginButton() {
    JButton button = new JButton("Đổi tài khoản khác? Đăng nhập!");
    button.setBorderPainted(false);
    button.setContentAreaFilled(false);
    button.setFocusPainted(false);
    button.setForeground(BRAND_BACKGROUND);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    button.addActionListener(event -> {
      dispose();
      navigator.showLogin();
    });
    return button;
  }

  private void guiMa() {
    if (!guiMaButton.isEnabled()) {
      return;
    }
    String tenDangNhap = taiKhoanField.getText();
    if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
      showError(MISSING_USERNAME_MESSAGE);
      return;
    }

    trangThaiLabel.setText("");
    setBusy(true);

    SwingWorker<Void, Void> worker =
        new SwingWorker<>() {
          @Override
          protected Void doInBackground() {
            controller.guiMa(tenDangNhap);
            return null;
          }

          @Override
          protected void done() {
            try {
              get();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              log.warn("Gửi mã xác nhận thất bại do lỗi không mong đợi (exception={})",
                  cause == null ? "none" : cause.getClass().getName());
            } finally {
              setBusy(false);
              showInfo(SENT_MESSAGE);
            }
          }
        };
    worker.execute();
  }

  private void xacNhan() {
    if (!xacNhanButton.isEnabled()) {
      return;
    }
    String tenDangNhap = taiKhoanField.getText();
    if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
      showError(MISSING_USERNAME_MESSAGE);
      return;
    }
    String maXacNhan = maXacNhanField.getText();
    String matKhauMoi = new String(matKhauMoiField.getPassword());

    trangThaiLabel.setText("");
    setBusy(true);

    SwingWorker<PasswordResetController.KetQua, Void> worker =
        new SwingWorker<>() {
          @Override
          protected PasswordResetController.KetQua doInBackground() {
            return controller.datLaiMatKhau(tenDangNhap, maXacNhan, matKhauMoi);
          }

          @Override
          protected void done() {
            try {
              PasswordResetController.KetQua ketQua = get();
              if (ketQua.thanhCong()) {
                JOptionPane.showMessageDialog(
                    ForgotPasswordFrame.this,
                    SUCCESS_MESSAGE,
                    AppInfo.APP_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
                navigator.showLogin();
                return;
              }
              showError(messageFor(ketQua.trangThai()));
              matKhauMoiField.setText("");
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              showError(DATA_ERROR_MESSAGE);
              matKhauMoiField.setText("");
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              log.warn("Đặt lại mật khẩu thất bại do lỗi không mong đợi (exception={})",
                  cause == null ? "none" : cause.getClass().getName());
              showError(DATA_ERROR_MESSAGE);
              matKhauMoiField.setText("");
            } finally {
              setBusy(false);
            }
          }
        };
    worker.execute();
  }

  private void setBusy(boolean busy) {
    guiMaButton.setEnabled(!busy);
    xacNhanButton.setEnabled(!busy);
    getRootPane().setCursor(
        busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
  }

  private void showInfo(String message) {
    trangThaiLabel.setForeground(Color.DARK_GRAY);
    trangThaiLabel.setText(message);
  }

  private void showError(String message) {
    trangThaiLabel.setForeground(ERROR_FOREGROUND);
    trangThaiLabel.setText(message);
  }

  private String messageFor(PasswordResetController.KetQuaDatLai trangThai) {
    return switch (trangThai) {
      case SUCCESS -> "";
      case INVALID_CODE -> INVALID_CODE_MESSAGE;
      case CODE_EXPIRED -> CODE_EXPIRED_MESSAGE;
      case TOO_MANY_ATTEMPTS -> TOO_MANY_ATTEMPTS_MESSAGE;
      case WEAK_PASSWORD -> WEAK_PASSWORD_MESSAGE;
      case DATA_ERROR -> DATA_ERROR_MESSAGE;
    };
  }
}
