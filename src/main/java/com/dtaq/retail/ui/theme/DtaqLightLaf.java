package com.dtaq.retail.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Look and Feel tùy biến cho hệ thống DTAQ, kế thừa từ {@link FlatLightLaf}.
 * Tự động tải các thuộc tính từ {@code DtaqLightLaf.properties}.
 */
public class DtaqLightLaf extends FlatLightLaf {

  public static final String NAME = "DTAQ Light";

  /** Cài đặt Look and Feel DTAQ Light làm giao diện mặc định. */
  public static boolean setup() {
    return setup(new DtaqLightLaf());
  }

  @Override
  public String getName() {
    return NAME;
  }
}
