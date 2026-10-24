package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dòng sản phẩm, ánh xạ bảng "DongSanPham". */
public record DongSanPham(
    String maDongSP,
    String tenDongSP,
    DanhMucSanPham danhMucSanPham,
    String donViTinh,
    int soLuongTon,
    BigDecimal thue,
    BigDecimal donGiaBanTruocThue,
    boolean trangThaiKinhDoanh,
    LocalDate ngayNhapKho,
    LocalDate ngayCapNhapCuoi) {
}
