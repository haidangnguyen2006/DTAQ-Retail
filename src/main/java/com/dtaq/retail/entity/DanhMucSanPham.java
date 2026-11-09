package com.dtaq.retail.entity;

import java.util.Objects;

/** Danh mục sản phẩm, ánh xạ bảng "DanhMucSanPham". */
public class DanhMucSanPham {

  private String maDanhMuc;
  private String tenDanhMuc;
  private String moTa;

  /** Tạo đối tượng rỗng. */
  public DanhMucSanPham() {}

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp, không kiểm tra ràng buộc.
   */
  public DanhMucSanPham(String maDanhMuc, String tenDanhMuc, String moTa) {
    this.maDanhMuc = maDanhMuc;
    this.tenDanhMuc = tenDanhMuc;
    this.moTa = moTa;
  }

  /** Mã danh mục; không thể thay đổi sau khi tạo. */
  public String maDanhMuc() {
    return maDanhMuc;
  }

  /** Mã danh mục; không thể thay đổi sau khi tạo. */
  public String getMaDanhMuc() {
    return maDanhMuc;
  }

  public String tenDanhMuc() {
    return tenDanhMuc;
  }

  public String getTenDanhMuc() {
    return tenDanhMuc;
  }

  /** Gán tên danh mục; bắt buộc có giá trị. */
  public void setTenDanhMuc(String tenDanhMuc) {
    this.tenDanhMuc = RangBuoc.chuoiBatBuoc(tenDanhMuc, "tenDanhMuc");
  }

  public String moTa() {
    return moTa;
  }

  public String getMoTa() {
    return moTa;
  }

  /** Gán mô tả; cho phép để trống (null). */
  public void setMoTa(String moTa) {
    this.moTa = RangBuoc.chuoiTuyChon(moTa);
  }

  @Override
  public String toString() {
    return "DanhMucSanPham[maDanhMuc=" + maDanhMuc
        + ", tenDanhMuc=" + tenDanhMuc
        + ", moTa=" + moTa
        + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DanhMucSanPham khac)) {
      return false;
    }
    return Objects.equals(maDanhMuc, khac.maDanhMuc)
        && Objects.equals(tenDanhMuc, khac.tenDanhMuc)
        && Objects.equals(moTa, khac.moTa);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maDanhMuc, tenDanhMuc, moTa);
  }
}
