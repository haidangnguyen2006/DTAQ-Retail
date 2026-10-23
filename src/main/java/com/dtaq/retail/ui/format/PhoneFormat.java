package com.dtaq.retail.ui.format;

/**
 * Định dạng số điện thoại hiển thị thân thiện (4-3-3: 0909 123 456).
 * Không thay đổi chuỗi lưu trữ trong CSDL.
 */
public final class PhoneFormat {

  private PhoneFormat() {
  }

  public static String format(String rawPhone) {
    if (rawPhone == null) {
      return "";
    }
    String digits = rawPhone.replaceAll("\\D", "");
    if (digits.length() == 10) {
      return digits.substring(0, 4) + " " + digits.substring(4, 7) + " " + digits.substring(7);
    }
    return rawPhone;
  }
}
