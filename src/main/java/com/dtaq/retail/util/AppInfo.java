package com.dtaq.retail.util;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

/** Static application metadata and window helpers. */
public final class AppInfo {

  public static final String APP_NAME = "DTAQ Retail";

  private AppInfo() {
  }

  public static String version() {
    String version = AppInfo.class.getPackage().getImplementationVersion();
    return version == null ? "dev" : version;
  }

  public static void centerOnScreen(Window w) {
    Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    int x = bounds.x + (bounds.width - w.getWidth()) / 2;
    int y = bounds.y + (bounds.height - w.getHeight()) / 2;
    w.setLocation(x, y);
  }
}
