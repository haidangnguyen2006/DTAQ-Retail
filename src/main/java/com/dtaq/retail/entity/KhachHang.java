package com.dtaq.retail.entity;

import java.util.Objects;

/** Khách hàng của siêu thị, ánh xạ bảng "KhachHang". */
public class KhachHang {

  private String maKH;
  private String hoTen;
  private String soDienThoai;
  private String diaChi;

  /** Tạo đối tượng rỗng. */
  public KhachHang() {}

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp, không kiểm tra ràng buộc.
   */
  public KhachHang(String maKH, String hoTen, String soDienThoai, String diaChi) {
    this.maKH = maKH;
    this.hoTen = hoTen;
    this.soDienThoai = soDienThoai;
    this.diaChi = diaChi;
  }

  /** Mã khách hàng; không thể thay đổi sau khi tạo. */
  public String maKH() {
    return maKH;
  }

  /** Mã khách hàng; không thể thay đổi sau khi tạo. */
  public String getMaKH() {
    return maKH;
  }

  public String hoTen() {
    return hoTen;
  }

  public String getHoTen() {
    return hoTen;
  }

  /** Gán họ tên; bắt buộc có giá trị. */
  public void setHoTen(String hoTen) {
    this.hoTen = RangBuoc.chuoiBatBuoc(hoTen, "hoTen");
  }

  public String soDienThoai() {
    return soDienThoai;
  }

  public String getSoDienThoai() {
    return soDienThoai;
  }

  /** Gán số điện thoại; phải khớp {@link RangBuoc#MAU_SO_DIEN_THOAI}. */
  public void setSoDienThoai(String soDienThoai) {
    this.soDienThoai = RangBuoc.chuoiKhopMau(
        soDienThoai, RangBuoc.MAU_SO_DIEN_THOAI, "soDienThoai", "Số điện thoại không hợp lệ");
  }

  public String diaChi() {
    return diaChi;
  }

  public String getDiaChi() {
    return diaChi;
  }

  /** Gán địa chỉ; cho phép để trống (null). */
  public void setDiaChi(String diaChi) {
    this.diaChi = RangBuoc.chuoiTuyChon(diaChi);
  }

  @Override
  public String toString() {
    return "KhachHang[maKH=" + maKH
        + ", hoTen=" + hoTen
        + ", soDienThoai=" + soDienThoai
        + ", diaChi=" + diaChi
        + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof KhachHang khac)) {
      return false;
    }
    return Objects.equals(maKH, khac.maKH)
        && Objects.equals(hoTen, khac.hoTen)
        && Objects.equals(soDienThoai, khac.soDienThoai)
        && Objects.equals(diaChi, khac.diaChi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maKH, hoTen, soDienThoai, diaChi);
  }
}
