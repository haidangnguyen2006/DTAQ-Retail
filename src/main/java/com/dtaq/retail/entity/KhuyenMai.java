package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Chương trình khuyến mãi, ánh xạ bảng "KhuyenMai". */
public class KhuyenMai {

  private String maKM;
  private String tenKM;
  private LocalDate ngayBatDau;
  private LocalDate ngayKetThuc;
  private BigDecimal tyLeGiam;
  private String moTa;
  private boolean dangApDung;
  private DongSanPham dongSanPham;

  /** Tạo đối tượng rỗng. */
  public KhuyenMai() {
  }

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp các trường, không kiểm tra ràng buộc, để giữ tương thích với
   * các nơi dựng đối tượng tạm hoặc đọc dữ liệu cũ (cho phép {@code null}).
   */
  public KhuyenMai(
      String maKM,
      String tenKM,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      BigDecimal tyLeGiam,
      String moTa,
      boolean dangApDung,
      DongSanPham dongSanPham) {
    this.maKM = maKM;
    this.tenKM = tenKM;
    this.ngayBatDau = ngayBatDau;
    this.ngayKetThuc = ngayKetThuc;
    this.tyLeGiam = tyLeGiam;
    this.moTa = moTa;
    this.dangApDung = dangApDung;
    this.dongSanPham = dongSanPham;
  }

  public String maKM() {
    return maKM;
  }

  public String getMaKM() {
    return maKM;
  }

  public String tenKM() {
    return tenKM;
  }

  public String getTenKM() {
    return tenKM;
  }

  public void setTenKM(String tenKM) {
    this.tenKM = RangBuoc.chuoiBatBuoc(tenKM, "tenKM");
  }

  public LocalDate ngayBatDau() {
    return ngayBatDau;
  }

  public LocalDate getNgayBatDau() {
    return ngayBatDau;
  }

  /** Gán ngày bắt đầu và bảo đảm không sau {@code ngayKetThuc}. */
  public void setNgayBatDau(LocalDate ngayBatDau) {
    this.ngayBatDau = RangBuoc.batBuoc(ngayBatDau, "ngayBatDau");
    RangBuoc.khongTruoc(ngayBatDau, ngayKetThuc, "ngayBatDau không được sau ngayKetThuc");
  }

  public LocalDate ngayKetThuc() {
    return ngayKetThuc;
  }

  public LocalDate getNgayKetThuc() {
    return ngayKetThuc;
  }

  /** Gán ngày kết thúc và bảo đảm không trước {@code ngayBatDau}. */
  public void setNgayKetThuc(LocalDate ngayKetThuc) {
    this.ngayKetThuc = RangBuoc.batBuoc(ngayKetThuc, "ngayKetThuc");
    RangBuoc.khongTruoc(ngayBatDau, ngayKetThuc, "ngayBatDau không được sau ngayKetThuc");
  }

  public BigDecimal tyLeGiam() {
    return tyLeGiam;
  }

  public BigDecimal getTyLeGiam() {
    return tyLeGiam;
  }

  public void setTyLeGiam(BigDecimal tyLeGiam) {
    this.tyLeGiam = RangBuoc.phanTram(tyLeGiam, "tyLeGiam");
  }

  public String moTa() {
    return moTa;
  }

  public String getMoTa() {
    return moTa;
  }

  public void setMoTa(String moTa) {
    this.moTa = RangBuoc.chuoiTuyChon(moTa);
  }

  public boolean dangApDung() {
    return dangApDung;
  }

  public boolean getDangApDung() {
    return dangApDung;
  }

  public boolean isDangApDung() {
    return dangApDung;
  }

  public void setDangApDung(boolean dangApDung) {
    this.dangApDung = dangApDung;
  }

  public DongSanPham dongSanPham() {
    return dongSanPham;
  }

  public DongSanPham getDongSanPham() {
    return dongSanPham;
  }

  public void setDongSanPham(DongSanPham dongSanPham) {
    this.dongSanPham = RangBuoc.batBuoc(dongSanPham, "dongSanPham");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof KhuyenMai khac)) {
      return false;
    }
    return dangApDung == khac.dangApDung
        && Objects.equals(maKM, khac.maKM)
        && Objects.equals(tenKM, khac.tenKM)
        && Objects.equals(ngayBatDau, khac.ngayBatDau)
        && Objects.equals(ngayKetThuc, khac.ngayKetThuc)
        && Objects.equals(tyLeGiam, khac.tyLeGiam)
        && Objects.equals(moTa, khac.moTa)
        && Objects.equals(dongSanPham, khac.dongSanPham);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maKM, tenKM, ngayBatDau, ngayKetThuc, tyLeGiam, moTa, dangApDung, dongSanPham);
  }

  @Override
  public String toString() {
    return "KhuyenMai["
        + "maKM=" + maKM
        + ", tenKM=" + tenKM
        + ", ngayBatDau=" + ngayBatDau
        + ", ngayKetThuc=" + ngayKetThuc
        + ", tyLeGiam=" + tyLeGiam
        + ", moTa=" + moTa
        + ", dangApDung=" + dangApDung
        + ", dongSanPham=" + dongSanPham
        + "]";
  }
}
