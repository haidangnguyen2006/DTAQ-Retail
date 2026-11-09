package com.dtaq.retail.entity;

import java.util.Objects;

/** Dòng chi tiết phiếu trả, ánh xạ bảng "ChiTietPhieuTra". */
public class ChiTietPhieuTra {

  private PhieuTra phieuTra;
  private ChiTietHoaDon chiTietHoaDon;
  private TinhTrangHangTra tinhTrangHangTra;
  private String lyDoTra;

  /** Tạo dòng chi tiết phiếu trả rỗng. */
  public ChiTietPhieuTra() {}

  /** Tạo dòng chi tiết phiếu trả đầy đủ; không kiểm tra ràng buộc. */
  public ChiTietPhieuTra(
      PhieuTra phieuTra,
      ChiTietHoaDon chiTietHoaDon,
      TinhTrangHangTra tinhTrangHangTra,
      String lyDoTra) {
    this.phieuTra = phieuTra;
    this.chiTietHoaDon = chiTietHoaDon;
    this.tinhTrangHangTra = tinhTrangHangTra;
    this.lyDoTra = lyDoTra;
  }

  public PhieuTra phieuTra() {
    return phieuTra;
  }

  public PhieuTra getPhieuTra() {
    return phieuTra;
  }

  /**
   * Gán phiếu trả chứa dòng chi tiết.
   *
   * @throws NullPointerException nếu {@code phieuTra} là {@code null}
   */
  public void setPhieuTra(PhieuTra phieuTra) {
    this.phieuTra = RangBuoc.batBuoc(phieuTra, "phieuTra");
  }

  public ChiTietHoaDon chiTietHoaDon() {
    return chiTietHoaDon;
  }

  public ChiTietHoaDon getChiTietHoaDon() {
    return chiTietHoaDon;
  }

  /**
   * Gán dòng chi tiết hóa đơn được trả.
   *
   * @throws NullPointerException nếu {@code chiTietHoaDon} là {@code null}
   */
  public void setChiTietHoaDon(ChiTietHoaDon chiTietHoaDon) {
    this.chiTietHoaDon = RangBuoc.batBuoc(chiTietHoaDon, "chiTietHoaDon");
  }

  public TinhTrangHangTra tinhTrangHangTra() {
    return tinhTrangHangTra;
  }

  public TinhTrangHangTra getTinhTrangHangTra() {
    return tinhTrangHangTra;
  }

  /**
   * Gán tình trạng hàng trả.
   *
   * @throws NullPointerException nếu {@code tinhTrangHangTra} là {@code null}
   */
  public void setTinhTrangHangTra(TinhTrangHangTra tinhTrangHangTra) {
    this.tinhTrangHangTra = RangBuoc.batBuoc(tinhTrangHangTra, "tinhTrangHangTra");
  }

  public String lyDoTra() {
    return lyDoTra;
  }

  public String getLyDoTra() {
    return lyDoTra;
  }

  /**
   * Gán lý do trả hàng, không được để trống.
   *
   * @throws NullPointerException nếu {@code lyDoTra} là {@code null}
   * @throws IllegalArgumentException nếu sau khi cắt khoảng trắng chỉ còn rỗng
   */
  public void setLyDoTra(String lyDoTra) {
    this.lyDoTra = RangBuoc.chuoiBatBuoc(lyDoTra, "lyDoTra");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChiTietPhieuTra khac = (ChiTietPhieuTra) o;
    return Objects.equals(phieuTra, khac.phieuTra)
        && Objects.equals(chiTietHoaDon, khac.chiTietHoaDon)
        && Objects.equals(tinhTrangHangTra, khac.tinhTrangHangTra)
        && Objects.equals(lyDoTra, khac.lyDoTra);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phieuTra, chiTietHoaDon, tinhTrangHangTra, lyDoTra);
  }

  @Override
  public String toString() {
    return "ChiTietPhieuTra[phieuTra=" + phieuTra
        + ", chiTietHoaDon=" + chiTietHoaDon
        + ", tinhTrangHangTra=" + tinhTrangHangTra
        + ", lyDoTra=" + lyDoTra
        + "]";
  }
}
