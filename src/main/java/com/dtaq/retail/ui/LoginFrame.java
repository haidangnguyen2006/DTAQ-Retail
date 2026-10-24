package com.dtaq.retail.ui;

import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.util.AppInfo;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cửa sổ đăng nhập của ứng dụng DTAQ được thiết kế chuẩn theo Figma.
 */
public final class LoginFrame extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(LoginFrame.class);

  private static final String MISSING_FIELDS_MESSAGE =
      "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu";
  private static final String INVALID_CREDENTIALS_MESSAGE =
      "Tên đăng nhập hoặc mật khẩu không đúng";
  private static final String LOCKED_MESSAGE =
      "Tài khoản đã bị khóa. Vui lòng liên hệ nhân viên quản lý";
  private static final String DATA_ERROR_MESSAGE =
      "Không thể đăng nhập lúc này. Vui lòng thử lại sau";
  private static final String CAPS_LOCK_HINT = "Caps Lock đang bật";

  private static final int FORM_WIDTH = 400;

  private final AuthController controller;
  private final AppNavigator navigator;

  private final FormField taiKhoanField = FormField.text("Tài khoản", "Nhập tài khoản", "user-round");
  private final FormField matKhauField = FormField.password("Mật khẩu", "Nhập mật khẩu");
  private final JPasswordField matKhauInput = (JPasswordField) matKhauField.getInputComponent();
  private final InfoCallout loiCallout = new InfoCallout(InfoCallout.Kind.DANGER, "");
  private final DtaqButton dangNhapButton = DtaqButton.primary("Đăng nhập");
  private final DtaqButton quenMatKhauButton = DtaqButton.link("Không thể đăng nhập? Quên mật khẩu!");

  public LoginFrame(AuthController controller, AppNavigator navigator) {
    super(AppInfo.APP_NAME);
    this.controller = Objects.requireNonNull(controller, "controller");
    this.navigator = Objects.requireNonNull(navigator, "navigator");

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setResizable(true);
    setLayout(new MigLayout("fill, insets 0", "[40%,fill][60%,fill]", "[grow,fill]"));
    getContentPane().setBackground(DtaqColors.SURFACE);

    add(new AuthBrandPanel());
    add(buildFormPanel());

    setPreferredSize(new Dimension(1100, 720));
    setMinimumSize(new Dimension(860, 560));
    pack();
    AppInfo.centerOnScreen(this);
  }

  private JPanel buildFormPanel() {
    JPanel container = new JPanel(new MigLayout("fill, insets 48", "[center]", "[center]"));
    container.setBackground(DtaqColors.SURFACE);

    JPanel form = new JPanel(new MigLayout("fillx, insets 0, hidemode 3", "[grow]", "[]28[]20[]12[]16[]20[]"));
    form.setOpaque(false);

    JPanel greeting = new JPanel(new MigLayout("insets 0", "[]", "[]10[]"));
    greeting.setOpaque(false);
    JLabel eyebrow = new JLabel("CHÀO MỪNG TRỞ LẠI");
    eyebrow.setFont(DtaqFonts.EYEBROW);
    eyebrow.setForeground(DtaqColors.PRIMARY);
    greeting.add(eyebrow, "wrap");
    JLabel title = new JLabel("Đăng nhập DTAQ");
    title.setFont(DtaqFonts.DISPLAY);
    title.setForeground(DtaqColors.TEXT);
    greeting.add(title);
    form.add(greeting, "growx, wrap");

    taiKhoanField.getTextField().setName("taiKhoan");
    taiKhoanField.getTextField().addActionListener(event -> matKhauInput.requestFocusInWindow());
    form.add(taiKhoanField, "growx, wrap");

    matKhauInput.setName("matKhau");
    matKhauInput.addActionListener(event -> submit());
    matKhauInput.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        refreshCapsLockHint();
      }

      @Override
      public void keyReleased(KeyEvent e) {
        refreshCapsLockHint();
      }
    });
    matKhauInput.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        refreshCapsLockHint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        matKhauField.setHint(null);
      }
    });
    form.add(matKhauField, "growx, wrap");

    loiCallout.setVisible(false);
    form.add(loiCallout, "growx, wrap");

    dangNhapButton.addActionListener(event -> submit());
    form.add(dangNhapButton, "growx, h 42!, wrap");

    JPanel support = new JPanel(new MigLayout("fillx, insets 0", "[center]", "[]12[]"));
    support.setOpaque(false);
    support.add(new JSeparator(), "growx, wrap");
    quenMatKhauButton.setForeground(DtaqColors.TEXT_MUTED);
    quenMatKhauButton.addActionListener(event -> navigator.showForgotPassword());
    support.add(quenMatKhauButton, "center");
    form.add(support, "growx");

    container.add(form, "center, w " + FORM_WIDTH + "!");
    return container;
  }

  private void refreshCapsLockHint() {
    try {
      boolean on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
      matKhauField.setHint(on ? CAPS_LOCK_HINT : null);
    } catch (UnsupportedOperationException e) {
      matKhauField.setHint(null);
    }
  }

  private void showError(String message) {
    loiCallout.setText(message);
    loiCallout.setVisible(true);
    loiCallout.getParent().revalidate();
  }

  private void clearError() {
    loiCallout.setVisible(false);
    loiCallout.getParent().revalidate();
  }

  private void submit() {
    if (dangNhapButton.isBusy()) {
      return;
    }

    String tenDangNhap = taiKhoanField.getText();
    String matKhau = new String(matKhauInput.getPassword());

    dangNhapButton.setBusy(true);
    clearError();
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
                matKhauInput.setText("");
                navigator.showMain(result.nhanVien());
              } else {
                showError(messageFor(result.outcome()));
                if (result.outcome() == AuthController.Outcome.MISSING_FIELDS
                    || result.outcome() == AuthController.Outcome.INVALID_CREDENTIALS) {
                  matKhauInput.setText("");
                  matKhauInput.requestFocusInWindow();
                }
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              showError(DATA_ERROR_MESSAGE);
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              log.warn("Đăng nhập thất bại do lỗi không mong đợi (exception={})",
                  cause == null ? "none" : cause.getClass().getName());
              showError(DATA_ERROR_MESSAGE);
            } finally {
              dangNhapButton.setBusy(false);
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
