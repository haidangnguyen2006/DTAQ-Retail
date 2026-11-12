package com.dtaq.retail.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import java.awt.Font;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điều phối thiết lập giao diện và bộ font chuẩn cho toàn bộ ứng dụng DTAQ.
 */
public final class DtaqTheme {

  private static final Logger log = LoggerFactory.getLogger(DtaqTheme.class);

  private DtaqTheme() {
  }

  /**
   * Cài đặt bộ font Inter, thiết lập Look and Feel và cấu hình UIManager mặc định.
   */
  public static void install() {
    try {
      FlatInterFont.install();
      FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
      FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
      FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
    } catch (Exception e) {
      log.warn("Không thể cài đặt font Inter, chuyển sang font hệ thống mặc định", e);
    }

    DtaqLightLaf.setup();

    Font defaultFont = new Font(FlatInterFont.FAMILY, Font.PLAIN, 13);
    UIManager.put("defaultFont", defaultFont);

    // Công cụ soi giao diện, chỉ bật khi chạy với -Ddtaq.devtools=true.
    if (Boolean.getBoolean("dtaq.devtools")) {
      com.formdev.flatlaf.extras.FlatInspector.install("ctrl shift alt X");
      com.formdev.flatlaf.extras.FlatUIDefaultsInspector.install("ctrl shift alt Y");
    }
  }

  private static volatile Font numeralRegular;
  private static volatile Font numeralSemibold;
  private static volatile boolean numeralsLoaded;

  /**
   * Font số đều cột "DTAQ Numerals" (Inter 4.1 đã đóng băng tnum); trả null nếu không nạp được
   * để bên gọi rơi về Inter. An toàn khi gọi nhiều lần và từ nhiều luồng.
   * Không phụ thuộc {@link DtaqFonts} nên không tạo vòng khởi tạo.
   */
  public static Font numeralBase(boolean semibold) {
    if (!numeralsLoaded) {
      synchronized (DtaqTheme.class) {
        if (!numeralsLoaded) {
          numeralRegular = loadFont("fonts/DtaqNumerals-Regular.otf");
          numeralSemibold = loadFont("fonts/DtaqNumerals-SemiBold.otf");
          numeralsLoaded = true;
        }
      }
    }
    return semibold ? numeralSemibold : numeralRegular;
  }

  private static Font loadFont(String resource) {
    try (java.io.InputStream in = DtaqTheme.class.getResourceAsStream(resource)) {
      if (in == null) {
        log.warn("Không tìm thấy font {}", resource);
        return null;
      }
      // TRUETYPE_FONT cũng đọc được file .otf (CFF).
      Font font = Font.createFont(Font.TRUETYPE_FONT, in);
      java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
      return font;
    } catch (java.awt.FontFormatException | java.io.IOException e) {
      log.warn("Không nạp được font {}", resource, e);
      return null;
    }
  }

  /** Biểu tượng cửa sổ (phần màn hình + giỏ hàng của logo); rỗng nếu không nạp được để không cản việc mở cửa sổ. */
  public static java.util.List<java.awt.Image> windowIcons() {
    java.awt.image.BufferedImage mark = image("dtaq-mark.png");
    if (mark == null) {
      return java.util.List.of();
    }
    return java.util.stream.IntStream.of(16, 24, 32, 48, 64, 256)
        .mapToObj(s -> smooth(mark, s, s)).toList();
  }

  /** Logo DTAQ đầy đủ cao {@code height}px (kèm bản 2x cho màn HiDPI); null nếu không nạp được. */
  public static javax.swing.ImageIcon logo(int height) {
    return scaledIcon("dtaq-logo.png", height);
  }

  /** Riêng phần biểu tượng (vuông) của logo, dùng khi thanh menu thu gọn. */
  public static javax.swing.ImageIcon logoMark(int size) {
    return scaledIcon("dtaq-mark.png", size);
  }

  private static javax.swing.ImageIcon scaledIcon(String name, int height) {
    java.awt.image.BufferedImage src = image(name);
    if (src == null) {
      return null;
    }
    int width = Math.round(src.getWidth() * (float) height / src.getHeight());
    return new javax.swing.ImageIcon(new java.awt.image.BaseMultiResolutionImage(
        smooth(src, width, height), smooth(src, width * 2, height * 2)));
  }

  /** Thu nhỏ mượt; bọc qua ImageIcon để ảnh được nạp xong (có kích thước) trước khi dùng. */
  private static java.awt.Image smooth(java.awt.Image src, int width, int height) {
    return new javax.swing.ImageIcon(src.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)).getImage();
  }

  private static java.awt.image.BufferedImage image(String name) {
    try (java.io.InputStream in = DtaqTheme.class.getResourceAsStream(name)) {
      return in == null ? null : javax.imageio.ImageIO.read(in);
    } catch (java.io.IOException e) {
      log.debug("Không nạp được ảnh {}", name, e);
      return null;
    }
  }
}
