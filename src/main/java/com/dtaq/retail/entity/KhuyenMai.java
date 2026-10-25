package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Chương trình khuyến mãi, ánh xạ bảng "KhuyenMai". */
public record KhuyenMai(
    String maKM,
    String tenKM,
    LocalDate ngayBatDau,
    LocalDate ngayKetThuc,
    BigDecimal tyLeGiam,
    String moTa,
    boolean dangApDung,
    DongSanPham dongSanPham) {
}
