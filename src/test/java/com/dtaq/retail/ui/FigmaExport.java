package com.dtaq.retail.ui;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.dao.impl.PasswordResetDaoImpl;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.shell.AppShellFrame;
import com.dtaq.retail.ui.shell.NavItem;
import com.dtaq.retail.ui.shell.NavigationModel;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.theme.DtaqTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

/**
 * Xuất ảnh mọi màn hình (2x) và vùng bấm (menu, nút mở/đóng hộp thoại) ra {@code screens.json} để plugin
 * Figma trong {@code tools/figma-plugin} dựng frame và luồng prototype. Chỉ đọc dữ liệu, không lưu gì.
 *
 * <p>Chạy: {@code java -cp target/test-classes:target/dtaq-retail-1.0.0-SNAPSHOT.jar
 * com.dtaq.retail.ui.FigmaExport tools/figma-plugin/screens [rộng cao]} (cần CSDL và màn hình).
 */
public final class FigmaExport {

  private static final float SCALE = Float.parseFloat(System.getProperty("dtaq.figma.scale", "2"));
  /** Thời gian chờ trang tải dữ liệu nền trước khi chụp. */
  private static final long LOAD_WAIT_MS = 2200;
  private static final Map<String, String> DIALOG_BUTTONS = Map.of(
      "CUSTOMERS", "Thêm khách hàng",
      "EMPLOYEES", "Thêm nhân viên",
      "CATEGORIES", "Thêm danh mục",
      "DEFECTIVE_PRODUCTS", "Ghi nhận máy lỗi",
      "PROMOTIONS", "Thêm khuyến mãi");

  private record Hotspot(Rectangle box, String target, String label) {
  }

  private record Screen(String id, String title, String group, int width, int height, List<Hotspot> hotspots) {
  }

  private FigmaExport() {
  }

  public static void main(String[] args) throws Exception {
    Path dir = Path.of(args.length > 0 ? args[0] : "tools/figma-plugin/screens");
    int width = args.length > 2 ? Integer.parseInt(args[1]) : 1440;
    int height = args.length > 2 ? Integer.parseInt(args[2]) : 900;
    Files.createDirectories(dir);
    SwingUtilities.invokeAndWait(DtaqTheme::install);
    var ds = ConnectDB.getDataSource();
    var staff = new NhanVienDaoImpl(ds);
    var reset = new PasswordResetDaoImpl(ds);
    AppNavigator nav = new AppNavigator(staff, reset, (to, code) -> { }, ds);
    List<Screen> screens = new ArrayList<>();

    // --- Xác thực
    Window[] auth = new Window[1];
    SwingUtilities.invokeAndWait(() -> auth[0] = open(new LoginFrame(new AuthController(staff), nav), width, height));
    settle();
    List<Hotspot> loginSpots = new ArrayList<>();
    spot(loginSpots, auth[0], c -> text(c, "Đăng nhập"), "QL-MANAGER_HOME", "Đăng nhập");
    spot(loginSpots, auth[0], c -> text(c, "Không thể đăng nhập? Quên mật khẩu!"), "forgot", "Quên mật khẩu");
    screens.add(capture(auth[0], null, dir, "login", "Đăng nhập", "Xác thực", loginSpots));
    SwingUtilities.invokeAndWait(() -> {
      auth[0].dispose();
      auth[0] = open(new ForgotPasswordFrame(new PasswordResetController(
          staff, reset, (to, c) -> { }, Clock.systemDefaultZone()), nav), width, height);
    });
    settle();
    List<Hotspot> forgotSpots = new ArrayList<>();
    spot(forgotSpots, auth[0], c -> text(c, "Đổi tài khoản khác? Đăng nhập!"), "login", "Về đăng nhập");
    screens.add(capture(auth[0], null, dir, "forgot", "Quên mật khẩu", "Xác thực", forgotSpots));
    SwingUtilities.invokeAndWait(auth[0]::dispose);

    // --- Từng vai trò
    for (LoaiQuyen role : List.of(LoaiQuyen.NHAN_VIEN_QUAN_LY, LoaiQuyen.NHAN_VIEN_BAN_HANG)) {
      boolean manager = role == LoaiQuyen.NHAN_VIEN_QUAN_LY;
      String prefix = manager ? "QL-" : "BH-";
      String group = manager ? "Quản lý" : "Nhân viên bán hàng";
      NhanVien nv = staff.findByTenDangNhap(manager ? "NVQL2026001" : "NVBH2026001")
          .orElseGet(() -> staff.findAll().stream().filter(n -> n.loaiQuyen() == role).findFirst().orElseThrow());
      Map<String, String> pageByLabel = new java.util.HashMap<>();
      NavigationModel.getEntriesFor(role).forEach(e -> pageByLabel.put(e.label(), prefix + e.pageId().name()));

      AppShellFrame[] frame = new AppShellFrame[1];
      List<Page> pages = new ArrayList<>();
      SwingUtilities.invokeAndWait(() -> {
        CurrentSession.set(nv);
        frame[0] = new AppShellFrame(nv, () -> { });
        pages.addAll(manager
            ? AppNavigator.managerPages(nv, ds, frame[0]::showPage)
            : AppNavigator.salesPages(nv, ds, frame[0]::showPage));
        pages.forEach(frame[0]::registerPage);
        frame[0].setExtendedState(JFrame.NORMAL);
        open(frame[0], width, height);
      });
      for (Page page : pages) {
        String id = prefix + page.id().name();
        SwingUtilities.invokeAndWait(() -> frame[0].showPage(page.id()));
        Thread.sleep(LOAD_WAIT_MS);
        settle();
        List<Hotspot> spots = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> collect(frame[0].getRootPane(), c -> c instanceof NavItem, c -> {
          String target = pageByLabel.get(c.getAccessibleContext().getAccessibleName());
          if (target != null && !target.equals(id)) {
            add(spots, frame[0], c, target, c.getAccessibleContext().getAccessibleName());
          }
        }));
        String dialogText = DIALOG_BUTTONS.get(page.id().name());
        AbstractButton opener = dialogText == null ? null : find(frame[0], c -> text(c, dialogText));
        String dialogId = id + "-dialog";
        if (opener != null) {
          add(spots, frame[0], opener, dialogId, dialogText);
        }
        screens.add(capture(frame[0], null, dir, id, page.title(), group, spots));
        System.out.println("đã chụp " + id);

        if (opener != null) {
          SwingUtilities.invokeLater(opener::doClick);
          Thread.sleep(1500);
          settle();
          JDialog dialog = (JDialog) java.util.Arrays.stream(Window.getWindows())
              .filter(w -> w instanceof JDialog && w.isShowing()).findFirst().orElse(null);
          if (dialog != null) {
            List<Hotspot> dialogSpots = new ArrayList<>();
            spot(dialogSpots, dialog, c -> "Đóng (Esc)".equals(c.getToolTipText()), id, "Đóng");
            spot(dialogSpots, dialog, c -> text(c, "Hủy"), id, "Hủy");
            screens.add(capture(frame[0], dialog, dir, dialogId, dialogText, "Hộp thoại", dialogSpots));
            SwingUtilities.invokeAndWait(dialog::dispose);
            System.out.println("đã chụp " + dialogId);
          }
        }
      }
      SwingUtilities.invokeAndWait(() -> {
        pages.forEach(Page::onHide);
        frame[0].dispose();
        CurrentSession.clear();
      });
    }

    Files.writeString(dir.resolve("screens.json"), json(screens, width, height), StandardCharsets.UTF_8);
    System.out.println("Xong: " + screens.size() + " màn hình → " + dir.toAbsolutePath());
    ConnectDB.close();
    System.exit(0);
  }

  private static <W extends Window> W open(W window, int width, int height) {
    window.setSize(width, height);
    window.setLocation(0, 0);
    window.setVisible(true);
    return window;
  }

  private static boolean text(Component c, String text) {
    return c instanceof AbstractButton b && text.equals(b.getText()) && b.isShowing();
  }

  private static void spot(List<Hotspot> out, Window window, Predicate<JComponent> match, String target, String label)
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JComponent c = find(window, match);
      if (c != null) {
        add(out, window, c, target, label);
      }
    });
  }

  /** Vùng bấm theo toạ độ logic của root pane, chỉ phần đang nhìn thấy. */
  private static void add(List<Hotspot> out, Window window, JComponent c, String target, String label) {
    Rectangle visible = c.getVisibleRect();
    if (visible.isEmpty()) {
      return;
    }
    JRootPane root = ((RootPaneContainer) window).getRootPane();
    out.add(new Hotspot(SwingUtilities.convertRectangle(c, visible, root), target, label));
  }

  @SuppressWarnings("unchecked")
  private static <T extends JComponent> T find(Window window, Predicate<JComponent> match) {
    List<JComponent> found = new ArrayList<>();
    collect(((RootPaneContainer) window).getRootPane(), match, found::add);
    return found.isEmpty() ? null : (T) found.get(0);
  }

  private static void collect(Container root, Predicate<JComponent> match, java.util.function.Consumer<JComponent> sink) {
    for (Component child : root.getComponents()) {
      if (child instanceof JComponent c && c.isShowing() && match.test(c)) {
        sink.accept(c);
      }
      if (child instanceof Container nested) {
        collect(nested, match, sink);
      }
    }
  }

  /** Chụp root pane (không viền cửa sổ) ở tỉ lệ {@link #SCALE}; có hộp thoại thì vẽ chồng lên khung chính. */
  private static Screen capture(Window base, JDialog dialog, Path dir, String id, String title, String group,
      List<Hotspot> spots) throws Exception {
    Screen[] result = new Screen[1];
    SwingUtilities.invokeAndWait(() -> {
      JRootPane root = ((RootPaneContainer) base).getRootPane();
      BufferedImage image = new BufferedImage(Math.round(root.getWidth() * SCALE),
          Math.round(root.getHeight() * SCALE), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.scale(SCALE, SCALE);
      root.paint(g);
      if (dialog != null) {
        JRootPane top = dialog.getRootPane();
        Point a = root.getLocationOnScreen();
        Point b = top.getLocationOnScreen();
        Graphics2D d = (Graphics2D) g.create();
        d.translate(b.x - a.x, b.y - a.y);
        top.paint(d);
        d.dispose();
        // Toạ độ vùng bấm của hộp thoại tính theo root pane hộp thoại; dời về hệ toạ độ khung chính.
        spots.replaceAll(s -> new Hotspot(new Rectangle(s.box().x + b.x - a.x, s.box().y + b.y - a.y,
            s.box().width, s.box().height), s.target(), s.label()));
      }
      g.dispose();
      try {
        ImageIO.write(image, "png", dir.resolve(id + ".png").toFile());
      } catch (java.io.IOException e) {
        throw new java.io.UncheckedIOException(e);
      }
      result[0] = new Screen(id, title, group, root.getWidth(), root.getHeight(), List.copyOf(spots));
    });
    return result[0];
  }

  private static void settle() throws Exception {
    for (int i = 0; i < 6; i++) {
      SwingUtilities.invokeAndWait(() -> {
        for (Window w : Window.getWindows()) {
          if (w.isShowing()) {
            w.validate();
          }
        }
      });
    }
  }

  private static String json(List<Screen> screens, int width, int height) {
    StringBuilder sb = new StringBuilder("{\n  \"app\": \"DTAQ\",\n  \"scale\": ").append(SCALE)
        .append(",\n  \"window\": [").append(width).append(", ").append(height).append("],\n  \"screens\": [\n");
    for (int i = 0; i < screens.size(); i++) {
      Screen s = screens.get(i);
      sb.append("    {\"id\": ").append(q(s.id())).append(", \"file\": ").append(q(s.id() + ".png"))
          .append(", \"title\": ").append(q(s.title())).append(", \"group\": ").append(q(s.group()))
          .append(", \"width\": ").append(s.width()).append(", \"height\": ").append(s.height())
          .append(", \"hotspots\": [");
      for (int j = 0; j < s.hotspots().size(); j++) {
        Hotspot h = s.hotspots().get(j);
        sb.append(j == 0 ? "\n" : ",\n").append("      {\"x\": ").append(h.box().x).append(", \"y\": ")
            .append(h.box().y).append(", \"w\": ").append(h.box().width).append(", \"h\": ")
            .append(h.box().height).append(", \"target\": ").append(q(h.target())).append(", \"label\": ")
            .append(q(h.label())).append("}");
      }
      sb.append(s.hotspots().isEmpty() ? "]}" : "\n    ]}").append(i < screens.size() - 1 ? ",\n" : "\n");
    }
    return sb.append("  ]\n}\n").toString();
  }

  private static String q(String s) {
    StringBuilder sb = new StringBuilder("\"");
    for (char ch : s.toCharArray()) {
      switch (ch) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        default -> sb.append(ch < 0x20 ? String.format("\\u%04x", (int) ch) : String.valueOf(ch));
      }
    }
    return sb.append('"').toString();
  }
}
