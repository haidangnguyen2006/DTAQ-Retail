package com.dtaq.retail.ui;

import com.dtaq.retail.controller.AuthController;
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
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
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

  private final AuthController controller;
  private final AppNavigator navigator;

  private final JTextField taiKhoanField = new JTextField();
  private final JPasswordField matKhauField = new JPasswordField();
  private final JLabel loiLabel = new JLabel("");
  private final DtaqButton dangNhapButton = DtaqButton.primary("Đăng nhập");
  private final JButton quenMatKhauButton = new JButton("Không thể đăng nhập? Quên mật khẩu!");

  public LoginFrame(AuthController controller, AppNavigator navigator) {
    super(AppInfo.APP_NAME);
    this.controller = Objects.requireNonNull(controller, "controller");
    this.navigator = Objects.requireNonNull(navigator, "navigator");

    setDefaultCloseOperation(EXIT_ON_CLOSE);
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
    JPanel container = new JPanel(new MigLayout("fill, insets 48", "[center]", "[center]"));
    container.setBackground(DtaqColors.SURFACE);

    JPanel form = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]28[]20[]12[]16[]20[]"));
    form.setOpaque(false);
    form.setPreferredSize(new Dimension(408, 0));

    // 1. Eyebrow & Title
    JPanel greeting = new JPanel(new MigLayout("insets 0", "[]", "[]12[]"));
    greeting.setOpaque(false);

    JLabel subtitle = new JLabel("CHÀO MỪNG TRỞ LẠI");
    subtitle.setFont(DtaqFonts.EYEBROW);
    subtitle.setForeground(DtaqColors.PRIMARY);
    greeting.add(subtitle, "wrap");

    JLabel title = new JLabel("Đăng nhập DTAQ");
    title.setFont(DtaqFonts.DISPLAY);
    title.setForeground(DtaqColors.TEXT);
    greeting.add(title, "wrap");
    form.add(greeting, "growx, wrap");

    // 2. Tài khoản
    JPanel taiKhoanBlock = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]8[]"));
    taiKhoanBlock.setOpaque(false);
    JLabel taiKhoanLabel = new JLabel("Tài khoản");
    taiKhoanLabel.setFont(DtaqFonts.LABEL);
    taiKhoanLabel.setForeground(DtaqColors.TEXT);
    taiKhoanBlock.add(taiKhoanLabel, "wrap");

    taiKhoanField.setName("taiKhoan");
    taiKhoanField.setFont(DtaqFonts.BODY_14);
    taiKhoanField.setPreferredSize(new Dimension(0, DtaqSpacing.CONTROL_H));
    taiKhoanField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tài khoản");
    taiKhoanField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon("user-round", 18));
    taiKhoanField.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    taiKhoanField.addActionListener(event -> submit());
    taiKhoanBlock.add(taiKhoanField, "growx");
    form.add(taiKhoanBlock, "growx, wrap");

    // 3. Mật khẩu
    JPanel matKhauBlock = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]8[]"));
    matKhauBlock.setOpaque(false);
    JLabel matKhauLabel = new JLabel("Mật khẩu");
    matKhauLabel.setFont(DtaqFonts.LABEL);
    matKhauLabel.setForeground(DtaqColors.TEXT);
    matKhauBlock.add(matKhauLabel, "wrap");

    matKhauField.setName("matKhau");
    matKhauField.setFont(DtaqFonts.BODY_14);
    matKhauField.setPreferredSize(new Dimension(0, DtaqSpacing.CONTROL_H));
    matKhauField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mật khẩu");
    matKhauField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon("lock-keyhole", 18));
    matKhauField.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    matKhauField.putClientProperty(FlatClientProperties.STYLE_CLASS, "showRevealButton");
    matKhauField.addActionListener(event -> submit());
    matKhauBlock.add(matKhauField, "growx");
    form.add(matKhauBlock, "growx, wrap");

    // 4. Lỗi
    loiLabel.setFont(DtaqFonts.CAPTION);
    loiLabel.setForeground(DtaqColors.DANGER);
    form.add(loiLabel, "growx, wrap");

    // 5. Nút đăng nhập
    dangNhapButton.setPreferredSize(new Dimension(408, DtaqSpacing.BUTTON_H));
    dangNhapButton.addActionListener(event -> submit());
    form.add(dangNhapButton, "growx, h 42!, wrap");

    // 6. Divider & Link quên mật khẩu
    JPanel supportBlock = new JPanel(new MigLayout("fillx, insets 0", "[center]", "[]12[]"));
    supportBlock.setOpaque(false);

    JSeparator sep = new JSeparator();
    sep.setForeground(DtaqColors.BORDER);
    supportBlock.add(sep, "growx, wrap");

    quenMatKhauButton.setFont(DtaqFonts.CAPTION);
    quenMatKhauButton.setBorderPainted(false);
    quenMatKhauButton.setContentAreaFilled(false);
    quenMatKhauButton.setFocusPainted(false);
    quenMatKhauButton.setForeground(DtaqColors.TEXT_MUTED);
    quenMatKhauButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    quenMatKhauButton.addActionListener(event -> navigator.showForgotPassword());
    supportBlock.add(quenMatKhauButton, "center");

    form.add(supportBlock, "growx");

    container.add(form, "center");
    return container;
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
