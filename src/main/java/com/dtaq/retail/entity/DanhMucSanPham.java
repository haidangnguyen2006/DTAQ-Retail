package com.dtaq.retail.entity;

/** Danh mục sản phẩm, ánh xạ bảng "DanhMucSanPham". */
public record DanhMucSanPham(
    String maDanhMuc,
    String tenDanhMuc,
    String moTa) {
}
