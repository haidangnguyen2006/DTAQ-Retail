package com.dtaq.retail.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Truy vấn chỉ đọc cho báo cáo doanh thu UC017. */
public interface ThongKeDoanhThuDao {
  record DongDoanhThu(LocalDate ngay, String maDongSP, String tenDongSP,
      String maPhieu, BigDecimal truocThue, BigDecimal thue) {}

  List<DongDoanhThu> doanhThu(LocalDate tuNgay, LocalDate denNgay);
  List<DongDoanhThu> dieuChinh(LocalDate tuNgay, LocalDate denNgay);
}
