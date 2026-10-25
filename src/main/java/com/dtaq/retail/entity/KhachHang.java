package com.dtaq.retail.entity;

/** Khách hàng của siêu thị, ánh xạ bảng "KhachHang". */
public record KhachHang(
    String maKH,
    String hoTen,
    String soDienThoai,
    String diaChi) {
}
