package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
  private List<ChiTietKhuyenMai> chiTiet = List.of();

  /** Tạo đối tượng rỗng. */
  public KhuyenMai() {
  }

  /**
   * Tạo đối tượng từ dữ liệu của một dòng sản phẩm, gồm cả snapshot hóa đơn cũ.
   * Dòng sản phẩm có thể là {@code null} khi dựng đối tượng tạm.
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
    this(maKM, tenKM, ngayBatDau, ngayKetThuc, tyLeGiam, moTa,
        dongSanPham == null ? List.of() : List.of(new ChiTietKhuyenMai(maKM, dongSanPham)),
        dangApDung);
  }

  /** Tạo chương trình với danh sách dòng sản phẩm áp dụng. */
  public KhuyenMai(
      String maKM,
      String tenKM,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      BigDecimal tyLeGiam,
      String moTa,
      List<ChiTietKhuyenMai> chiTiet,
      boolean dangApDung) {
    this.maKM = maKM;
    this.tenKM = tenKM;
    this.ngayBatDau = ngayBatDau;
    this.ngayKetThuc = ngayKetThuc;
    this.tyLeGiam = tyLeGiam;
    this.moTa = moTa;
    this.dangApDung = dangApDung;
    this.chiTiet = List.copyOf(chiTiet);
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

  public List<ChiTietKhuyenMai> chiTiet() {
    return chiTiet;
  }

  /** Trả dòng đầu tiên cho các điểm gọi còn dùng mô hình một dòng. */
  public DongSanPham dongSanPham() {
    return chiTiet.isEmpty() ? null : chiTiet.getFirst().dongSanPham();
  }

  public DongSanPham getDongSanPham() {
    return dongSanPham();
  }

  public void setDongSanPham(DongSanPham dongSanPham) {
    this.chiTiet = List.of(
        new ChiTietKhuyenMai(maKM, RangBuoc.batBuoc(dongSanPham, "dongSanPham")));
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
        && Objects.equals(chiTiet, khac.chiTiet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maKM, tenKM, ngayBatDau, ngayKetThuc, tyLeGiam, moTa, dangApDung, chiTiet);
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
        + ", chiTiet=" + chiTiet
        + "]";
  }
}
