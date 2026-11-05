package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Dòng sản phẩm, ánh xạ bảng "DongSanPham". */
public class DongSanPham {

  private String maDongSP;
  private String tenDongSP;
  private DanhMucSanPham danhMucSanPham;
  private String donViTinh;
  private int soLuongTon;
  private BigDecimal thue;
  private BigDecimal donGiaBanTruocThue;
  private boolean trangThaiKinhDoanh;
  private LocalDate ngayNhapKho;
  private LocalDate ngayCapNhapCuoi;

  /** Tạo đối tượng rỗng. */
  public DongSanPham() {
  }

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp các trường, không kiểm tra ràng buộc, để giữ tương thích với
   * các nơi dựng đối tượng tạm hoặc đọc dữ liệu cũ (cho phép {@code null}).
   */
  public DongSanPham(
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
    this.maDongSP = maDongSP;
    this.tenDongSP = tenDongSP;
    this.danhMucSanPham = danhMucSanPham;
    this.donViTinh = donViTinh;
    this.soLuongTon = soLuongTon;
    this.thue = thue;
    this.donGiaBanTruocThue = donGiaBanTruocThue;
    this.trangThaiKinhDoanh = trangThaiKinhDoanh;
    this.ngayNhapKho = ngayNhapKho;
    this.ngayCapNhapCuoi = ngayCapNhapCuoi;
  }

  public String maDongSP() {
    return maDongSP;
  }

  public String getMaDongSP() {
    return maDongSP;
  }

  public String tenDongSP() {
    return tenDongSP;
  }

  public String getTenDongSP() {
    return tenDongSP;
  }

  public void setTenDongSP(String tenDongSP) {
    this.tenDongSP = RangBuoc.chuoiBatBuoc(tenDongSP, "tenDongSP");
  }

  public DanhMucSanPham danhMucSanPham() {
    return danhMucSanPham;
  }

  public DanhMucSanPham getDanhMucSanPham() {
    return danhMucSanPham;
  }

  public void setDanhMucSanPham(DanhMucSanPham danhMucSanPham) {
    this.danhMucSanPham = RangBuoc.batBuoc(danhMucSanPham, "danhMucSanPham");
  }

  public String donViTinh() {
    return donViTinh;
  }

  public String getDonViTinh() {
    return donViTinh;
  }

  public void setDonViTinh(String donViTinh) {
    this.donViTinh = RangBuoc.chuoiBatBuoc(donViTinh, "donViTinh");
  }

  public int soLuongTon() {
    return soLuongTon;
  }

  public int getSoLuongTon() {
    return soLuongTon;
  }

  public void setSoLuongTon(int soLuongTon) {
    this.soLuongTon = RangBuoc.khongAm(soLuongTon, "soLuongTon");
  }

  public BigDecimal thue() {
    return thue;
  }

  public BigDecimal getThue() {
    return thue;
  }

  public void setThue(BigDecimal thue) {
    this.thue = RangBuoc.phanTram(thue, "thue");
  }

  public BigDecimal donGiaBanTruocThue() {
    return donGiaBanTruocThue;
  }

  public BigDecimal getDonGiaBanTruocThue() {
    return donGiaBanTruocThue;
  }

  public void setDonGiaBanTruocThue(BigDecimal donGiaBanTruocThue) {
    this.donGiaBanTruocThue = RangBuoc.tienKhongAm(donGiaBanTruocThue, "donGiaBanTruocThue");
  }

  public boolean trangThaiKinhDoanh() {
    return trangThaiKinhDoanh;
  }

  public boolean getTrangThaiKinhDoanh() {
    return trangThaiKinhDoanh;
  }

  public boolean isTrangThaiKinhDoanh() {
    return trangThaiKinhDoanh;
  }

  public void setTrangThaiKinhDoanh(boolean trangThaiKinhDoanh) {
    this.trangThaiKinhDoanh = trangThaiKinhDoanh;
  }

  public LocalDate ngayNhapKho() {
    return ngayNhapKho;
  }

  public LocalDate getNgayNhapKho() {
    return ngayNhapKho;
  }

  /**
   * Gán ngày nhập kho.
   *
   * <p>Không kiểm tra "không ở tương lai" vì cần ngữ cảnh nghiệp vụ bên ngoài entity.
   */
  public void setNgayNhapKho(LocalDate ngayNhapKho) {
    this.ngayNhapKho = ngayNhapKho;
  }

  public LocalDate ngayCapNhapCuoi() {
    return ngayCapNhapCuoi;
  }

  public LocalDate getNgayCapNhapCuoi() {
    return ngayCapNhapCuoi;
  }

  /** Gán ngày cập nhật cuối và bảo đảm không trước {@code ngayNhapKho}. */
  public void setNgayCapNhapCuoi(LocalDate ngayCapNhapCuoi) {
    this.ngayCapNhapCuoi = ngayCapNhapCuoi;
    RangBuoc.khongTruoc(
        ngayNhapKho, ngayCapNhapCuoi, "ngayCapNhapCuoi không được trước ngayNhapKho");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DongSanPham khac)) {
      return false;
    }
    return soLuongTon == khac.soLuongTon
        && trangThaiKinhDoanh == khac.trangThaiKinhDoanh
        && Objects.equals(maDongSP, khac.maDongSP)
        && Objects.equals(tenDongSP, khac.tenDongSP)
        && Objects.equals(danhMucSanPham, khac.danhMucSanPham)
        && Objects.equals(donViTinh, khac.donViTinh)
        && Objects.equals(thue, khac.thue)
        && Objects.equals(donGiaBanTruocThue, khac.donGiaBanTruocThue)
        && Objects.equals(ngayNhapKho, khac.ngayNhapKho)
        && Objects.equals(ngayCapNhapCuoi, khac.ngayCapNhapCuoi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maDongSP,
        tenDongSP,
        danhMucSanPham,
        donViTinh,
        soLuongTon,
        thue,
        donGiaBanTruocThue,
        trangThaiKinhDoanh,
        ngayNhapKho,
        ngayCapNhapCuoi);
  }

  @Override
  public String toString() {
    return "DongSanPham["
        + "maDongSP=" + maDongSP
        + ", tenDongSP=" + tenDongSP
        + ", danhMucSanPham=" + danhMucSanPham
        + ", donViTinh=" + donViTinh
        + ", soLuongTon=" + soLuongTon
        + ", thue=" + thue
        + ", donGiaBanTruocThue=" + donGiaBanTruocThue
        + ", trangThaiKinhDoanh=" + trangThaiKinhDoanh
        + ", ngayNhapKho=" + ngayNhapKho
        + ", ngayCapNhapCuoi=" + ngayCapNhapCuoi
        + "]";
  }
}
