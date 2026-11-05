package com.dtaq.retail.notify;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hiển thị thông báo không chặn cho người dùng qua khay hệ thống.
 *
 * <p>Thông báo luôn được đẩy sang luồng giao diện bằng {@link SwingUtilities#invokeLater}. Khi môi
 * trường không hỗ trợ khay hệ thống (một số bản Linux, chế độ headless), dịch vụ ghi log thay vì
 * ném ngoại lệ: thông báo chỉ là phụ trợ, không được làm hỏng nghiệp vụ đang chạy.
 *
 * <p>Biểu tượng khay được tạo một lần và dùng lại cho mọi lần thông báo, sau đó được gỡ khi JVM
 * tắt. Nội dung log không bao giờ chứa mật khẩu hay thông tin xác thực.
 */
public final class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  /** Cạnh của biểu tượng khay, tính bằng pixel. */
  private static final int KICH_THUOC_BIEU_TUONG = 16;

  /** Màu nền biểu tượng, cùng tông với nhận diện của ứng dụng. */
  private static final Color MAU_BIEU_TUONG = new Color(0x1D, 0x4E, 0xD8);

  private static TrayIcon bieuTuong;
  private static boolean daThemVaoKhay;

  private NotificationService() {
  }

  /**
   * Hiển thị một thông báo hệ thống mà không chặn luồng gọi.
   *
   * <p>Phương thức không bao giờ ném ngoại lệ: mọi lỗi của khay hệ thống chỉ được ghi log.
   *
   * @param tieuDe tiêu đề ngắn của thông báo
   * @param noiDung nội dung thông báo; không nên chứa mật khẩu hay thông tin bí mật
   */
  public static void thongBao(String tieuDe, String noiDung) {
    String tieuDeAnToan = tieuDe == null ? "" : tieuDe;
    String noiDungAnToan = noiDung == null ? "" : noiDung;
    try {
      SwingUtilities.invokeLater(() -> hienThi(tieuDeAnToan, noiDungAnToan));
    } catch (RuntimeException e) {
      log.info("Không gửi được thông báo '{}' sang luồng giao diện: {}", tieuDeAnToan,
          e.getMessage());
    }
  }

  private static void hienThi(String tieuDe, String noiDung) {
    if (!SystemTray.isSupported()) {
      log.info("Thông báo: {} — {}", tieuDe, noiDung);
      return;
    }

    TrayIcon icon = bieuTuongKhay();
    try {
      if (!daThemVaoKhay) {
        SystemTray.getSystemTray().add(icon);
        daThemVaoKhay = true;
      }
      icon.setToolTip(tieuDe);
      icon.displayMessage(tieuDe, noiDung, TrayIcon.MessageType.INFO);
    } catch (AWTException | RuntimeException e) {
      log.warn("Không hiển thị được thông báo qua khay hệ thống: {}", e.getMessage());
      log.info("Thông báo: {} — {}", tieuDe, noiDung);
    }
  }

  private static synchronized TrayIcon bieuTuongKhay() {
    if (bieuTuong == null) {
      bieuTuong = new TrayIcon(taoBieuTuong(), "DTAQ");
      bieuTuong.setImageAutoSize(true);
      Runtime.getRuntime().addShutdownHook(
          new Thread(NotificationService::goBieuTuong, "dtaq-notify-shutdown"));
    }
    return bieuTuong;
  }

  private static synchronized void goBieuTuong() {
    if (bieuTuong == null || !daThemVaoKhay || !SystemTray.isSupported()) {
      return;
    }
    try {
      SystemTray.getSystemTray().remove(bieuTuong);
      daThemVaoKhay = false;
    } catch (RuntimeException e) {
      log.debug("Không gỡ được biểu tượng thông báo khi tắt: {}", e.getMessage());
    }
  }

  private static BufferedImage taoBieuTuong() {
    BufferedImage anh = new BufferedImage(KICH_THUOC_BIEU_TUONG, KICH_THUOC_BIEU_TUONG,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D hoa = anh.createGraphics();
    try {
      hoa.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      hoa.setColor(MAU_BIEU_TUONG);
      hoa.fillRoundRect(1, 1, KICH_THUOC_BIEU_TUONG - 2, KICH_THUOC_BIEU_TUONG - 2, 6, 6);
      hoa.setColor(Color.WHITE);
      hoa.fillOval(4, 4, KICH_THUOC_BIEU_TUONG - 8, KICH_THUOC_BIEU_TUONG - 8);
    } finally {
      hoa.dispose();
    }
    return anh;
  }
}
