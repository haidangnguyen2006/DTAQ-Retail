package com.dtaq.retail.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.page.invoice.InvoiceTotals;
import com.dtaq.retail.ui.page.invoice.RetailInvoicePage;
import com.dtaq.retail.ui.shell.AppShellFrame;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.theme.DtaqTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Chạy dưới Xvfb; chỉ đọc dữ liệu, không thanh toán/gửi email/phục hồi thật.
 * main xuất gallery: java ... com.dtaq.retail.ui.UiLayoutSmokeTest target/ui-review
 */
class UiLayoutSmokeTest {
  private static final int[][] SIZES = {
      {1024, 600}, {1280, 680}, {1366, 728}, {1536, 824}, {1920, 1040}};

  @BeforeAll
  static void installTheme() throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Cần display hoặc xvfb-run");
    SwingUtilities.invokeAndWait(DtaqTheme::install);
  }

  @AfterEach
  void cleanup() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      for (Window window : Window.getWindows()) window.dispose();
      CurrentSession.clear();
    });
  }

  @Test
  void authenticationFitsNarrowWindowsAndResetStepTwoRemainsReachable() throws Exception {
    NhanVienDao staff = stub(NhanVienDao.class);
    PasswordResetDao reset = stub(PasswordResetDao.class);
    AppNavigator navigator = new AppNavigator(staff, reset, (to, code) -> {}, stub(javax.sql.DataSource.class));
    LoginFrame[] login = new LoginFrame[1];
    ForgotPasswordFrame[] forgot = new ForgotPasswordFrame[1];
    SwingUtilities.invokeAndWait(() -> {
      login[0] = new LoginFrame(new AuthController(staff), navigator);
      forgot[0] = new ForgotPasswordFrame(new PasswordResetController(staff, reset,
          (to, code) -> {}, Clock.systemUTC()), navigator);
      login[0].setVisible(true);
      forgot[0].setVisible(true);
    });
    for (JFrame frame : new JFrame[] {login[0], forgot[0]}) {
      for (int width : new int[] {640, 900, 1100}) {
        SwingUtilities.invokeAndWait(() -> frame.setSize(width, 520));
        settle();
        SwingUtilities.invokeAndWait(() -> {
          JComponent account = named(frame, "taiKhoan");
          assertNotNull(account);
          assertTrue(account.getVisibleRect().width > 200, "Form phải nằm ở cột có chiều rộng");
          snapshot(frame, frame.getClass().getSimpleName() + "-" + width);
        });
      }
    }
    SwingUtilities.invokeAndWait(() -> {
      try {
        var step = ForgotPasswordFrame.class.getDeclaredMethod("showStep", int.class);
        step.setAccessible(true);
        step.invoke(forgot[0], 2);
        forgot[0].setSize(640, 520);
      } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    });
    settle();
    SwingUtilities.invokeAndWait(() -> {
      JComponent password = named(forgot[0], "matKhauMoi");
      assertNotNull(password);
      password.scrollRectToVisible(new Rectangle(password.getSize()));
      assertTrue(password.getVisibleRect().height >= 30);
      snapshot(forgot[0], "ForgotPasswordFrame-step2-640");
    });
  }

  @Test
  void paymentAndLargeAmountsStayVisibleAtReferenceSize() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      var page = new RetailInvoicePage(employee(LoaiQuyen.NHAN_VIEN_BAN_HANG));
      page.setCustomer(new RetailInvoicePage.CustomerInfo("KH-TEST", "Nguyễn Minh Anh", "0900000000", "TP.HCM"));
      page.setCart(List.of(new RetailInvoicePage.CartItem("DSP-TEST", "Tủ lạnh kiểm thử", 1,
          900000000L, "10%", 990000000L, 1)), new InvoiceTotals(900000000L, 90000000L, 990000000L));
      AppShellFrame shell = new AppShellFrame(employee(LoaiQuyen.NHAN_VIEN_BAN_HANG), () -> {});
      shell.registerPage(page);
      shell.setExtendedState(JFrame.NORMAL);
      shell.setSize(1280, 680);
      shell.setVisible(true);
      shell.showPage(page.id());
    });
    settle();
    SwingUtilities.invokeAndWait(() -> {
      JFrame frame = (JFrame) java.util.Arrays.stream(Window.getWindows())
          .filter(w -> w instanceof AppShellFrame && w.isShowing()).findFirst().orElseThrow();
      JButton pay = button(frame, "Thanh toán bằng tiền mặt");
      assertNotNull(pay);
      assertTrue(pay.getVisibleRect().height >= pay.getHeight() - 4, "Nút thanh toán bị che");
      JLabel amount = label(frame, "990.000.000");
      assertNotNull(amount);
      assertTrue(amount.getWidth() >= amount.getPreferredSize().width, "Số tiền bị cắt");
      snapshot(frame, "invoice-populated-1280");
    });
  }

  @Test
  void everyRegisteredPageRendersAtFiveWindowSizes() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cần CSDL kiểm thử để dựng các trang thật");
    for (LoaiQuyen role : LoaiQuyen.values()) {
      NhanVien employee = employee(role);
      AppShellFrame[] frame = new AppShellFrame[1];
      java.util.ArrayList<Page> pages = new java.util.ArrayList<>();
      SwingUtilities.invokeAndWait(() -> {
        CurrentSession.set(employee);
        frame[0] = new AppShellFrame(employee, () -> {});
        pages.addAll(role == LoaiQuyen.NHAN_VIEN_QUAN_LY
            ? AppNavigator.managerPages(employee, ConnectDB.getDataSource(), frame[0]::showPage)
            : AppNavigator.salesPages(employee, ConnectDB.getDataSource(), frame[0]::showPage));
        pages.forEach(frame[0]::registerPage);
        frame[0].setExtendedState(JFrame.NORMAL);
        frame[0].setVisible(true);
      });
      for (int[] size : SIZES) {
        SwingUtilities.invokeAndWait(() -> frame[0].setSize(size[0], size[1]));
        for (Page page : pages) {
          SwingUtilities.invokeAndWait(() -> frame[0].showPage(page.id()));
          settle();
          SwingUtilities.invokeAndWait(() -> {
            assertTrue(page.view().getWidth() > 0, page.id().name());
            assertTrue(page.view().getHeight() >= page.view().getMinimumSize().height,
                "Nội dung tối thiểu phải truy cập được bằng cuộn: " + page.id());
            snapshot(frame[0], role + "-" + page.id() + "-" + size[0]);
            checkButtons(page.view());
          });
        }
      }
      SwingUtilities.invokeAndWait(() -> {
        pages.forEach(Page::onHide);
        frame[0].dispose();
        CurrentSession.clear();
      });
    }
  }

  @Test
  void dialogKeepsActionsVisibleOnShortWindowsAndEscapeCloses() throws Exception {
    ModalDialog[] dialog = new ModalDialog[1];
    SwingUtilities.invokeAndWait(() -> {
      JFrame owner = new JFrame();
      owner.setSize(640, 520);
      owner.setVisible(true);
      dialog[0] = new ModalDialog(owner, "Kiểm thử hộp thoại", "Biểu mẫu dài trên màn hình thấp", 620);
      for (int i = 0; i < 12; i++) {
        dialog[0].getBody().add(FormField.text("Trường " + i, "Nội dung", null), "growx, wrap");
      }
      dialog[0].setActions(DtaqButton.secondary("Chọn bản khác"),
          DtaqButton.secondary("Bỏ qua — dữ liệu hợp lệ"), DtaqButton.secondary("Để sau"),
          DtaqButton.primary("Phục hồi bản này"));
      SwingUtilities.invokeLater(() -> dialog[0].setVisible(true));
    });
    settle();
    SwingUtilities.invokeAndWait(() -> {
      JButton restore = button(dialog[0], "Phục hồi bản này");
      assertNotNull(restore);
      assertTrue(restore.getVisibleRect().height >= restore.getHeight() - 4);
      checkButtons(dialog[0]);
      assertTrue(dialog[0].getBody().getHeight() > 0);
      snapshot(dialog[0], "dialog-640");
      var escape = dialog[0].getRootPane().getActionForKeyStroke(KeyStroke.getKeyStroke("ESCAPE"));
      assertNotNull(escape);
      escape.actionPerformed(new java.awt.event.ActionEvent(dialog[0], 0, "escape"));
      assertFalse(dialog[0].isShowing());
    });
  }

  private static void checkButtons(Component component) {
    if (!component.isShowing()) return;
    if (component instanceof PageHeader header) {
      assertTrue(header.getHeight() >= header.getMinimumSize().height, "Tiêu đề bị ép chiều cao");
    }
    if (component instanceof JButton b && b.getText() != null && !b.getText().isBlank()) {
      if (SwingUtilities.getAncestorOfClass(JViewport.class, b) == null) {
        assertTrue(b.getHeight() > 0 && b.getVisibleRect().height >= b.getHeight()
            - 2 * com.formdev.flatlaf.util.UIScale.scale(2), "Nút bị che dọc: " + b.getText() + ancestors(b));
      }
      if (b.getVisibleRect().height > 4) assertTrue(b.getVisibleRect().width >= b.getWidth() - 2 * com.formdev.flatlaf.util.UIScale.scale(2),
          "Nút bị cắt ngang: " + b.getText() + " " + b.getSize() + " / " + b.getVisibleRect() + ancestors(b));
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) checkButtons(child);
    }
  }

  private static String ancestors(Component child) {
    StringBuilder result = new StringBuilder();
    for (Container p = child.getParent(); p != null; p = p.getParent()) {
      result.append(" / ").append(p.getClass().getSimpleName()).append(p.getBounds());
    }
    return result.toString();
  }

  private static void settle() throws Exception {
    // Dồn các sự kiện resize/revalidate vào EDT trước khi đo; không thao tác chuột/bàn phím.
    for (int i = 0; i < 8; i++) SwingUtilities.invokeAndWait(() -> {
      for (Window window : Window.getWindows()) {
        if (window.isShowing()) window.validate();
      }
    });
  }

  private static NhanVien employee(LoaiQuyen role) {
    return new NhanVien("NV-UI-TEST", "Nguyễn Minh Anh", "0900000000", "ui@example.test",
        LocalDate.of(2026, 1, 1), "ui-test", "", role, true);
  }

  @SuppressWarnings("unchecked")
  private static <T> T stub(Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
      throw new AssertionError("Không được gọi " + m.getName() + " trong kiểm thử bố cục xác thực");
    });
  }

  private static JComponent named(Container root, String name) {
    return find(root, c -> name.equals(c.getName()));
  }

  private static JButton button(Container root, String text) {
    return (JButton) find(root, c -> c instanceof JButton b && text.equals(b.getText()));
  }

  private static JLabel label(Container root, String text) {
    return (JLabel) find(root, c -> c instanceof JLabel l && text.equals(l.getText()));
  }

  private static JComponent find(Container root, java.util.function.Predicate<JComponent> match) {
    for (Component child : root.getComponents()) {
      if (child instanceof JComponent c && match.test(c)) return c;
      if (child instanceof Container nested) {
        JComponent found = find(nested, match);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static void snapshot(Window frame, String name) {
    String dir = System.getProperty("dtaq.uiSnapshots");
    if (dir == null) return;
    try {
      Files.createDirectories(Path.of(dir));
      BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();
      frame.paint(graphics);
      graphics.dispose();
      ImageIO.write(image, "png", Path.of(dir, name + ".png").toFile());
    } catch (Exception e) { throw new AssertionError(e); }
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("dtaq.uiSnapshots", args.length == 0 ? "target/ui-review" : args[0]);
    installTheme();
    System.out.println("System DPI transform: " + GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform()
        + "; FlatLaf user scale: " + com.formdev.flatlaf.util.UIScale.getUserScaleFactor());
    UiLayoutSmokeTest gallery = new UiLayoutSmokeTest();
    try {
      gallery.authenticationFitsNarrowWindowsAndResetStepTwoRemainsReachable();
      gallery.cleanup();
      gallery.dialogKeepsActionsVisibleOnShortWindowsAndEscapeCloses();
      gallery.cleanup();
      gallery.paymentAndLargeAmountsStayVisibleAtReferenceSize();
      gallery.cleanup();
      gallery.everyRegisteredPageRendersAtFiveWindowSizes();
    } finally {
      gallery.cleanup();
      ConnectDB.close();
    }
  }
}
