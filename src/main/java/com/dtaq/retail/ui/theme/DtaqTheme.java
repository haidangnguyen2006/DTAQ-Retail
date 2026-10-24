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
  }
}
