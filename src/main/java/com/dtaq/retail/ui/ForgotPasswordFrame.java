package com.dtaq.retail.ui;

import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.AppInfo;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cửa sổ quên mật khẩu: gửi mã xác nhận qua email rồi đặt mật khẩu mới theo chuẩn Figma.
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

  private final PasswordResetController controller;
  private final AppNavigator navigator;

  private final JTextField taiKhoanField = new JTextField();
  private final JTextField maXacNhanField = new JTextField();
  private final JPasswordField matKhauMoiField = new JPasswordField();
  private final JLabel trangThaiLabel = new JLabel("");
  private final DtaqButton guiMaButton = DtaqButton.secondary("Gửi mã xác nhận");
  private final DtaqButton xacNhanButton = DtaqButton.primary("Xác nhận");

  public ForgotPasswordFrame(PasswordResetController controller, AppNavigator navigator) {
    super("Quên mật khẩu - DTAQ");
    this.controller = Objects.requireNonNull(controller, "controller");
    this.navigator = Objects.requireNonNull(navigator, "navigator");

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(true);
    setLayout(new MigLayout("fill, insets 0", "[164!][grow]", "[grow]"));
    getContentPane().setBackground(DtaqColors.SURFACE);

    add(buildBrandPanel(), "grow");
    add(buildFormPanel(), "grow");

    setPreferredSize(new Dimension(1100, 720));
    setMinimumSize(new Dimension(860, 560));
    pack();
    AppInfo.centerOnScreen(this);
  }

  private JPanel buildBrandPanel() {
    JPanel panel = new JPanel(new MigLayout("fill, insets 0", "[center]", "[center]"));
    panel.setBackground(DtaqColors.BRAND_DARK);

    JPanel brandBlock = new JPanel(new MigLayout("insets 0", "[center]", "[]12[]2[]"));
    brandBlock.setOpaque(false);

    JPanel logo = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_CARD_SM, DtaqSpacing.RADIUS_CARD_SM));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    logo.setOpaque(false);
    logo.setPreferredSize(new Dimension(36, 36));
    logo.setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
    logo.add(new JLabel(DtaqIcons.icon("zap", 22, DtaqColors.PRIMARY)));
    brandBlock.add(logo, "center, wrap");

    JLabel brand = new JLabel("DTAQ");
    brand.setFont(DtaqFonts.BRAND);
    brand.setForeground(Color.WHITE);
    brandBlock.add(brand, "center, wrap");

    JLabel tagline = new JLabel("ĐIỆN MÁY");
    tagline.setFont(DtaqFonts.OVERLINE);
    tagline.setForeground(DtaqColors.BRAND_ON_DARK_MUTED);
    brandBlock.add(tagline, "center");

    panel.add(brandBlock, "center");
    return panel;
  }

  private JPanel buildFormPanel() {
    JPanel container = new JPanel(new MigLayout("fill, insets 40 48 40 48", "[center]", "[center]"));
    container.setBackground(DtaqColors.SURFACE);

    JPanel form = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]20[]8[]14[]14[]10[]18[]"));
    form.setOpaque(false);
    form.setPreferredSize(new Dimension(408, 0));

    // 1. Eyebrow & Title
    JPanel greeting = new JPanel(new MigLayout("insets 0", "[]", "[]8[]"));
    greeting.setOpaque(false);

    JLabel subtitle = new JLabel("QUÊN MẬT KHẨU");
    subtitle.setFont(DtaqFonts.EYEBROW);
    subtitle.setForeground(DtaqColors.PRIMARY);
    greeting.add(subtitle, "wrap");

    JLabel title = new JLabel("Cập nhật mật khẩu");
    title.setFont(DtaqFonts.DISPLAY);
    title.setForeground(DtaqColors.TEXT);
    greeting.add(title, "wrap");
    form.add(greeting, "growx, wrap");

    // 2. Tài khoản
    JPanel taiKhoanBlock = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]6[]"));
    taiKhoanBlock.setOpaque(false);
    JLabel taiKhoanLabel = new JLabel("Tài khoản");
    taiKhoanLabel.setFont(DtaqFonts.LABEL);
    taiKhoanLabel.setForeground(DtaqColors.TEXT);
    taiKhoanBlock.add(taiKhoanLabel, "wrap");

    taiKhoanField.setName("taiKhoan");
    taiKhoanField.setFont(DtaqFonts.BODY_14);
    taiKhoanField.setPreferredSize(new Dimension(0, DtaqSpacing.CONTROL_H));
    taiKhoanField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tài khoản đã đăng ký");
    taiKhoanField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon("user-round", 18));
    taiKhoanField.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    taiKhoanBlock.add(taiKhoanField, "growx");
    form.add(taiKhoanBlock, "growx, wrap");

    // 3. Nút Gửi mã xác nhận
    guiMaButton.addActionListener(event -> guiMa());
    form.add(guiMaButton, "growx, h 42!, wrap");

    // 4. Mã xác nhận
    JPanel maBlock = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]6[]"));
    maBlock.setOpaque(false);
    JLabel maLabel = new JLabel("Mã xác nhận");
    maLabel.setFont(DtaqFonts.LABEL);
    maLabel.setForeground(DtaqColors.TEXT);
    maBlock.add(maLabel, "wrap");

    maXacNhanField.setName("maXacNhan");
    maXacNhanField.setFont(DtaqFonts.BODY_14);
    maXacNhanField.setPreferredSize(new Dimension(0, DtaqSpacing.CONTROL_H));
    maXacNhanField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập 6 ký tự mã xác nhận");
    maXacNhanField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon("lock-keyhole", 18));
    maXacNhanField.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    maBlock.add(maXacNhanField, "growx");
    form.add(maBlock, "growx, wrap");

    // 5. Mật khẩu mới
    JPanel mkMoiBlock = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]6[]"));
    mkMoiBlock.setOpaque(false);
    JLabel mkMoiLabel = new JLabel("Mật khẩu mới");
    mkMoiLabel.setFont(DtaqFonts.LABEL);
    mkMoiLabel.setForeground(DtaqColors.TEXT);
    mkMoiBlock.add(mkMoiLabel, "wrap");

    matKhauMoiField.setName("matKhauMoi");
    matKhauMoiField.setFont(DtaqFonts.BODY_14);
    matKhauMoiField.setPreferredSize(new Dimension(0, DtaqSpacing.CONTROL_H));
    matKhauMoiField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mật khẩu mới");
    matKhauMoiField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon("lock-keyhole", 18));
    matKhauMoiField.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    matKhauMoiField.putClientProperty(FlatClientProperties.STYLE_CLASS, "showRevealButton");
    mkMoiBlock.add(matKhauMoiField, "growx");
    form.add(mkMoiBlock, "growx, wrap");

    // 6. Nhãn trạng thái
    trangThaiLabel.setFont(DtaqFonts.CAPTION);
    form.add(trangThaiLabel, "growx, wrap");

    // 7. Nút Xác nhận
    xacNhanButton.setPreferredSize(new Dimension(408, DtaqSpacing.BUTTON_H));
    xacNhanButton.addActionListener(event -> xacNhan());
    form.add(xacNhanButton, "growx, h 42!, wrap");

    // 8. Divider & Link quay lại đăng nhập
    JPanel supportBlock = new JPanel(new MigLayout("fillx, insets 0", "[center]", "[]12[]"));
    supportBlock.setOpaque(false);

    JSeparator sep = new JSeparator();
    sep.setForeground(DtaqColors.BORDER);
    supportBlock.add(sep, "growx, wrap");

    JButton backBtn = new JButton("Đổi tài khoản khác? Đăng nhập!");
    backBtn.setFont(DtaqFonts.CAPTION);
    backBtn.setBorderPainted(false);
    backBtn.setContentAreaFilled(false);
    backBtn.setFocusPainted(false);
    backBtn.setForeground(DtaqColors.TEXT_MUTED);
    backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    backBtn.addActionListener(event -> {
      dispose();
      navigator.showLogin();
    });
    supportBlock.add(backBtn, "center");

    form.add(supportBlock, "growx");

    container.add(form, "center");
    return container;
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
    trangThaiLabel.setForeground(DtaqColors.TEXT_MUTED);
    trangThaiLabel.setText(message);
  }

  private void showError(String message) {
    trangThaiLabel.setForeground(DtaqColors.DANGER);
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
