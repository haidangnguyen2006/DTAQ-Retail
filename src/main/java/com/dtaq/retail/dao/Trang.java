package com.dtaq.retail.dao;

import java.util.List;

/**
 * Một trang kết quả của truy vấn có phân trang.
 *
 * @param dong các dòng của trang
 * @param tongSo tổng số dòng khớp điều kiện trên mọi trang
 * @param <T> kiểu của một dòng
 */
public record Trang<T>(List<T> dong, int tongSo) {

  /** Cắt một trang từ danh sách đã có sẵn trong bộ nhớ. */
  public static <T> Trang<T> cat(List<T> tatCa, int boQua, int gioiHan) {
    int tu = Math.min(Math.max(0, boQua), tatCa.size());
    int den = Math.min(tu + Math.max(0, gioiHan), tatCa.size());
    return new Trang<>(List.copyOf(tatCa.subList(tu, den)), tatCa.size());
  }
}
