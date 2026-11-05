package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Một máy cụ thể trong kho, ánh xạ bảng "SanPham". */
public class SanPham {

  private String soSerial;
  private NhaCungCap nhaCungCap;
  private DongSanPham dongSanPham;
  private BigDecimal giaNhap;
  private LocalDate ngayNhapKhoBanDau;
  private TrangThaiSanPham trangThai;
  private int soLanDoiTra;
  private int soLanSua;

  /** Tạo đối tượng rỗng. */
  public SanPham() {
  }

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp các trường, không kiểm tra ràng buộc, để giữ tương thích với
   * các nơi dựng đối tượng tạm hoặc đọc dữ liệu cũ (cho phép {@code null}).
   */
  public SanPham(
      String soSerial,
      NhaCungCap nhaCungCap,
      DongSanPham dongSanPham,
      BigDecimal giaNhap,
      LocalDate ngayNhapKhoBanDau,
      TrangThaiSanPham trangThai,
      int soLanDoiTra,
      int soLanSua) {
    this.soSerial = soSerial;
    this.nhaCungCap = nhaCungCap;
    this.dongSanPham = dongSanPham;
    this.giaNhap = giaNhap;
    this.ngayNhapKhoBanDau = ngayNhapKhoBanDau;
    this.trangThai = trangThai;
    this.soLanDoiTra = soLanDoiTra;
    this.soLanSua = soLanSua;
  }

  public String soSerial() {
    return soSerial;
  }

  public String getSoSerial() {
    return soSerial;
  }

  public NhaCungCap nhaCungCap() {
    return nhaCungCap;
  }

  public NhaCungCap getNhaCungCap() {
    return nhaCungCap;
  }

  public void setNhaCungCap(NhaCungCap nhaCungCap) {
    this.nhaCungCap = RangBuoc.batBuoc(nhaCungCap, "nhaCungCap");
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

  public BigDecimal giaNhap() {
    return giaNhap;
  }

  public BigDecimal getGiaNhap() {
    return giaNhap;
  }

  public void setGiaNhap(BigDecimal giaNhap) {
    this.giaNhap = RangBuoc.tienKhongAm(giaNhap, "giaNhap");
  }

  public LocalDate ngayNhapKhoBanDau() {
    return ngayNhapKhoBanDau;
  }

  public LocalDate getNgayNhapKhoBanDau() {
    return ngayNhapKhoBanDau;
  }

  public void setNgayNhapKhoBanDau(LocalDate ngayNhapKhoBanDau) {
    this.ngayNhapKhoBanDau = RangBuoc.batBuoc(ngayNhapKhoBanDau, "ngayNhapKhoBanDau");
  }

  public TrangThaiSanPham trangThai() {
    return trangThai;
  }

  public TrangThaiSanPham getTrangThai() {
    return trangThai;
  }

  public void setTrangThai(TrangThaiSanPham trangThai) {
    this.trangThai = RangBuoc.batBuoc(trangThai, "trangThai");
  }

  public int soLanDoiTra() {
    return soLanDoiTra;
  }

  public int getSoLanDoiTra() {
    return soLanDoiTra;
  }

  public void setSoLanDoiTra(int soLanDoiTra) {
    this.soLanDoiTra = RangBuoc.khongAm(soLanDoiTra, "soLanDoiTra");
  }

  public int soLanSua() {
    return soLanSua;
  }

  public int getSoLanSua() {
    return soLanSua;
  }

  public void setSoLanSua(int soLanSua) {
    this.soLanSua = RangBuoc.khongAm(soLanSua, "soLanSua");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SanPham khac)) {
      return false;
    }
    return soLanDoiTra == khac.soLanDoiTra
        && soLanSua == khac.soLanSua
        && Objects.equals(soSerial, khac.soSerial)
        && Objects.equals(nhaCungCap, khac.nhaCungCap)
        && Objects.equals(dongSanPham, khac.dongSanPham)
        && Objects.equals(giaNhap, khac.giaNhap)
        && Objects.equals(ngayNhapKhoBanDau, khac.ngayNhapKhoBanDau)
        && trangThai == khac.trangThai;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        soSerial,
        nhaCungCap,
        dongSanPham,
        giaNhap,
        ngayNhapKhoBanDau,
        trangThai,
        soLanDoiTra,
        soLanSua);
  }

  @Override
  public String toString() {
    return "SanPham["
        + "soSerial=" + soSerial
        + ", nhaCungCap=" + nhaCungCap
        + ", dongSanPham=" + dongSanPham
        + ", giaNhap=" + giaNhap
        + ", ngayNhapKhoBanDau=" + ngayNhapKhoBanDau
        + ", trangThai=" + trangThai
        + ", soLanDoiTra=" + soLanDoiTra
        + ", soLanSua=" + soLanSua
        + "]";
  }
}
