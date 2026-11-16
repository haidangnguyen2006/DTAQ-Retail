package com.dtaq.retail.dao.impl;

/** Dựng mẫu {@code ILIKE} từ từ khóa người dùng nhập. */
final class MauLike {

  private MauLike() {
  }

  /** Mẫu "chứa từ khóa"; ký tự đại diện trong từ khóa được thoát để so khớp theo nghĩa đen. */
  static String chua(String tuKhoa) {
    String khoa = tuKhoa == null ? "" : tuKhoa.trim();
    return "%" + khoa.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
