package com.dtaq.retail.ui.format;

import java.util.Locale;

/**
 * Xử lý tên người dùng để hiển thị ngắn gọn và chữ cái avatar theo quy chuẩn DTAQ.
 */
public final class PersonNames {

  private PersonNames() {
  }

  /**
   * Lấy chữ viết tắt đại diện cho avatar (2 chữ cái đầu của 2 từ cuối họ tên).
   * Ví dụ: "Lê Thu Hà" -> "TH", "Trần Mai Linh" -> "ML", "Hà" -> "H".
   */
  public static String initials(String hoTen) {
    if (hoTen == null || hoTen.trim().isEmpty()) {
      return "NV";
    }
    String[] parts = hoTen.trim().split("\\s+");
    if (parts.length == 1) {
      return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
    }
    String last1 = parts[parts.length - 2];
    String last2 = parts[parts.length - 1];
    String c1 = last1.isEmpty() ? "" : last1.substring(0, 1);
    String c2 = last2.isEmpty() ? "" : last2.substring(0, 1);
    return (c1 + c2).toUpperCase(Locale.ROOT);
  }

  /**
   * Lấy tên gọi thân thiện cho lời chào (2 từ cuối của họ tên).
   * Ví dụ: "Lê Thu Hà" -> "Thu Hà", "Trần Mai Linh" -> "Mai Linh", "Hà" -> "Hà".
   */
  public static String shortName(String hoTen) {
    if (hoTen == null || hoTen.trim().isEmpty()) {
      return "bạn";
    }
    String[] parts = hoTen.trim().split("\\s+");
    if (parts.length <= 2) {
      return hoTen.trim();
    }
    return parts[parts.length - 2] + " " + parts[parts.length - 1];
  }
}
