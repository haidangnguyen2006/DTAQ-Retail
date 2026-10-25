package com.dtaq.retail.ui;

import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.util.AppInfo;
import java.awt.Cursor;
import java.awt.Dimension;
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
 * Cửa sổ quên mật khẩu theo 2 bước trong cùng một khung: (1) nhập tài khoản và gửi mã xác nhận qua
 * email, (2) nhập mã và mật khẩu mới.
 */
public final class ForgotPasswordFrame extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(ForgotPasswordFrame.class);

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

  private static final int FORM_WIDTH = 400;

  private final PasswordResetController controller;
  private final AppNavigator navigator;

  private final FormField taiKhoanField =
      FormField.text("Tài khoản", "Nhập tài khoản đã đăng ký", "user-round");
  private final FormField maXacNhanField =
      FormField.text("Mã xác nhận", "Nhập 6 ký tự mã xác nhận", "lock-keyhole");
  private final FormField matKhauMoiField = FormField.password("Mật khẩu mới", "Nhập mật khẩu mới");
  private final JPasswordField matKhauMoiInput = (JPasswordField) matKhauMoiField.getInputComponent();

  private final InfoCallout infoCallout = new InfoCallout(InfoCallout.Kind.INFO, "");
  private final InfoCallout errorCallout = new InfoCallout(InfoCallout.Kind.DANGER, "");

  private final DtaqButton guiMaButton = DtaqButton.primary("Gửi mã xác nhận");
  private final DtaqButton xacNhanButton = DtaqButton.primary("Xác nhận");
  private final DtaqButton guiLaiButton = DtaqButton.link("Gửi lại mã");
  private final DtaqButton doiTaiKhoanButton = DtaqButton.link("Đổi tài khoản");

  private final JPanel step1Panel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]"));
  private final JPanel step2Panel =
      new JPanel(new MigLayout("fillx, insets 0, hidemode 3", "[grow]", "[]14[]16[]"));

  public ForgotPasswordFrame(PasswordResetController controller, AppNavigator navigator) {
    super("Quên mật khẩu - DTAQ");
    this.controller = Objects.requireNonNull(controller, "controller");
    this.navigator = Objects.requireNonNull(navigator, "navigator");

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(true);
    setLayout(new MigLayout("fill, insets 0", "[40%,fill][60%,fill]", "[grow,fill]"));
    getContentPane().setBackground(DtaqColors.SURFACE);

    add(new AuthBrandPanel());
    add(buildFormPanel());

    showStep(1);

    setPreferredSize(new Dimension(1100, 720));
    setMinimumSize(new Dimension(860, 560));
    pack();
    AppInfo.centerOnScreen(this);
  }

  private JPanel buildFormPanel() {
    JPanel container = new JPanel(new MigLayout("fill, insets 40 48 40 48", "[center]", "[center]"));
    container.setBackground(DtaqColors.SURFACE);

    JPanel form = new JPanel(new MigLayout("fillx, insets 0, hidemode 3", "[grow]", "[]24[]16[]14[]16[]"));
    form.setOpaque(false);

    JPanel greeting = new JPanel(new MigLayout("insets 0", "[]", "[]10[]"));
    greeting.setOpaque(false);
    JLabel eyebrow = new JLabel("QUÊN MẬT KHẨU");
    eyebrow.setFont(DtaqFonts.EYEBROW);
    eyebrow.setForeground(DtaqColors.PRIMARY);
    greeting.add(eyebrow, "wrap");
    JLabel title = new JLabel("Cập nhật mật khẩu");
    title.setFont(DtaqFonts.DISPLAY);
    title.setForeground(DtaqColors.TEXT);
    greeting.add(title);
    form.add(greeting, "growx, wrap");

    taiKhoanField.getTextField().setName("taiKhoan");
    taiKhoanField.getTextField().addActionListener(event -> guiMa());
    form.add(taiKhoanField, "growx, wrap");

    infoCallout.setVisible(false);
    errorCallout.setVisible(false);
    form.add(infoCallout, "growx, wrap");
    form.add(errorCallout, "growx, wrap");

    // Bước 1: gửi mã
    guiMaButton.addActionListener(event -> guiMa());
    step1Panel.setOpaque(false);
    step1Panel.add(guiMaButton, "growx, h 42!");
    form.add(step1Panel, "growx, wrap");

    // Bước 2: nhập mã + mật khẩu mới
    maXacNhanField.getTextField().setName("maXacNhan");
    matKhauMoiInput.setName("matKhauMoi");
    matKhauMoiInput.addActionListener(event -> xacNhan());
    maXacNhanField.getTextField().addActionListener(event -> matKhauMoiInput.requestFocusInWindow());
    xacNhanButton.addActionListener(event -> xacNhan());
    guiLaiButton.setForeground(DtaqColors.PRIMARY);
    guiLaiButton.addActionListener(event -> guiMa());
    doiTaiKhoanButton.setForeground(DtaqColors.TEXT_MUTED);
    doiTaiKhoanButton.addActionListener(event -> showStep(1));

    step2Panel.setOpaque(false);
    step2Panel.add(maXacNhanField, "growx, wrap");
    step2Panel.add(matKhauMoiField, "growx, wrap");
    step2Panel.add(xacNhanButton, "growx, h 42!, wrap");
    JPanel resendRow = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
    resendRow.setOpaque(false);
    resendRow.add(guiLaiButton);
    resendRow.add(doiTaiKhoanButton);
    step2Panel.add(resendRow, "growx");
    form.add(step2Panel, "growx, wrap");

    JPanel support = new JPanel(new MigLayout("fillx, insets 0", "[center]", "[]12[]"));
    support.setOpaque(false);
    support.add(new JSeparator(), "growx, wrap");
    DtaqButton backBtn = DtaqButton.link("Đổi tài khoản khác? Đăng nhập!");
    backBtn.setForeground(DtaqColors.TEXT_MUTED);
    backBtn.addActionListener(event -> {
      dispose();
      navigator.showLogin();
    });
    support.add(backBtn, "center");
    form.add(support, "growx");

    container.add(form, "center, w " + FORM_WIDTH + "!");
    return container;
  }

  private void showStep(int step) {
    boolean second = step == 2;
    step1Panel.setVisible(!second);
    step2Panel.setVisible(second);
    taiKhoanField.getTextField().setEditable(!second);
    if (!second) {
      maXacNhanField.setText("");
      matKhauMoiInput.setText("");
      clearBanners();
      taiKhoanField.getTextField().requestFocusInWindow();
    } else {
      maXacNhanField.getTextField().requestFocusInWindow();
    }
    revalidate();
    repaint();
  }

  private void clearBanners() {
    infoCallout.setVisible(false);
    errorCallout.setVisible(false);
  }

  private void guiMa() {
    if (isBusy()) {
      return;
    }
    String tenDangNhap = taiKhoanField.getText();
    if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
      showError(MISSING_USERNAME_MESSAGE);
      return;
    }

    clearBanners();
    DtaqButton active = step2Panel.isVisible() ? guiLaiButton : guiMaButton;
    setBusy(true, active);

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
              setBusy(false, active);
              // Luôn hiện thông điệp chung và sang bước 2 để không tiết lộ tài khoản có tồn tại hay không
              showStep(2);
              showInfo(SENT_MESSAGE);
            }
          }
        };
    worker.execute();
  }

  private void xacNhan() {
    if (isBusy()) {
      return;
    }
    String tenDangNhap = taiKhoanField.getText();
    if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
      showError(MISSING_USERNAME_MESSAGE);
      return;
    }
    String maXacNhan = maXacNhanField.getText();
    String matKhauMoi = new String(matKhauMoiInput.getPassword());

    clearBanners();
    setBusy(true, xacNhanButton);

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
                setBusy(false, xacNhanButton);
                ConfirmDialog.info(
                    ForgotPasswordFrame.this, "Đặt lại mật khẩu", SUCCESS_MESSAGE, navigator::showLogin);
                return;
              }
              showError(messageFor(ketQua.trangThai()));
              matKhauMoiInput.setText("");
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              showError(DATA_ERROR_MESSAGE);
              matKhauMoiInput.setText("");
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              log.warn("Đặt lại mật khẩu thất bại do lỗi không mong đợi (exception={})",
                  cause == null ? "none" : cause.getClass().getName());
              showError(DATA_ERROR_MESSAGE);
              matKhauMoiInput.setText("");
            } finally {
              setBusy(false, xacNhanButton);
            }
          }
        };
    worker.execute();
  }

  private boolean isBusy() {
    return guiMaButton.isBusy() || xacNhanButton.isBusy() || !guiLaiButton.isEnabled();
  }

  /** Khóa mọi nút khi đang xử lý; nút đang thực hiện hiện "Đang xử lý…" (nút liên kết chỉ bị tắt). */
  private void setBusy(boolean busy, DtaqButton active) {
    if (active.getVariant() == DtaqButton.Variant.LINK) {
      active.setEnabled(!busy);
    } else {
      active.setBusy(busy);
    }
    for (DtaqButton other : new DtaqButton[] {guiMaButton, xacNhanButton}) {
      if (other != active) {
        other.setEnabled(!busy);
      }
    }
    getRootPane().setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
  }

  private void showInfo(String message) {
    errorCallout.setVisible(false);
    infoCallout.setText(message);
    infoCallout.setVisible(true);
    revalidate();
  }

  private void showError(String message) {
    infoCallout.setVisible(false);
    errorCallout.setText(message);
    errorCallout.setVisible(true);
    revalidate();
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
