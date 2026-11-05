package com.dtaq.retail.entity;

import java.util.Objects;

/**
 * Dòng chi tiết phiếu đổi, ánh xạ bảng "ChiTietPhieuDoi".
 *
 * <p>{@code tinhTrangHangTra} phản ánh tình trạng máy khách trả lại (UC006 bước 6): máy nguyên vẹn
 * được đưa lại về kho bán, máy lỗi chuyển sang nhóm cần xử lý (bước 15.a).
 */
public class ChiTietPhieuDoi {

  private PhieuDoi phieuDoi;
  private ChiTietHoaDon chiTietHoaDon;
  private String lyDoDoi;
  private TinhTrangHangTra tinhTrangHangTra;
  private SanPham chiTietSanPhamDoi;

  /** Tạo dòng chi tiết phiếu đổi rỗng. */
  public ChiTietPhieuDoi() {}

  /** Tạo dòng chi tiết phiếu đổi đầy đủ; không kiểm tra ràng buộc. */
  public ChiTietPhieuDoi(
      PhieuDoi phieuDoi,
      ChiTietHoaDon chiTietHoaDon,
      String lyDoDoi,
      TinhTrangHangTra tinhTrangHangTra,
      SanPham chiTietSanPhamDoi) {
    this.phieuDoi = phieuDoi;
    this.chiTietHoaDon = chiTietHoaDon;
    this.lyDoDoi = lyDoDoi;
    this.tinhTrangHangTra = tinhTrangHangTra;
    this.chiTietSanPhamDoi = chiTietSanPhamDoi;
  }

  public PhieuDoi phieuDoi() {
    return phieuDoi;
  }

  public PhieuDoi getPhieuDoi() {
    return phieuDoi;
  }

  /**
   * Gán phiếu đổi chứa dòng chi tiết.
   *
   * @throws NullPointerException nếu {@code phieuDoi} là {@code null}
   */
  public void setPhieuDoi(PhieuDoi phieuDoi) {
    this.phieuDoi = RangBuoc.batBuoc(phieuDoi, "phieuDoi");
  }

  public ChiTietHoaDon chiTietHoaDon() {
    return chiTietHoaDon;
  }

  public ChiTietHoaDon getChiTietHoaDon() {
    return chiTietHoaDon;
  }

  /**
   * Gán dòng chi tiết hóa đơn được đổi.
   *
   * @throws NullPointerException nếu {@code chiTietHoaDon} là {@code null}
   */
  public void setChiTietHoaDon(ChiTietHoaDon chiTietHoaDon) {
    this.chiTietHoaDon = RangBuoc.batBuoc(chiTietHoaDon, "chiTietHoaDon");
  }

  public String lyDoDoi() {
    return lyDoDoi;
  }

  public String getLyDoDoi() {
    return lyDoDoi;
  }

  /**
   * Gán lý do đổi hàng, không được để trống.
   *
   * @throws NullPointerException nếu {@code lyDoDoi} là {@code null}
   * @throws IllegalArgumentException nếu sau khi cắt khoảng trắng chỉ còn rỗng
   */
  public void setLyDoDoi(String lyDoDoi) {
    this.lyDoDoi = RangBuoc.chuoiBatBuoc(lyDoDoi, "lyDoDoi");
  }

  public TinhTrangHangTra tinhTrangHangTra() {
    return tinhTrangHangTra;
  }

  public TinhTrangHangTra getTinhTrangHangTra() {
    return tinhTrangHangTra;
  }

  /**
   * Gán tình trạng máy khách trả lại.
   *
   * @throws NullPointerException nếu {@code tinhTrangHangTra} là {@code null}
   */
  public void setTinhTrangHangTra(TinhTrangHangTra tinhTrangHangTra) {
    this.tinhTrangHangTra = RangBuoc.batBuoc(tinhTrangHangTra, "tinhTrangHangTra");
  }

  public SanPham chiTietSanPhamDoi() {
    return chiTietSanPhamDoi;
  }

  public SanPham getChiTietSanPhamDoi() {
    return chiTietSanPhamDoi;
  }

  /**
   * Gán máy đổi cho khách; máy đổi không được trùng máy trả (RB25).
   *
   * @throws NullPointerException nếu {@code chiTietSanPhamDoi} là {@code null}
   * @throws IllegalArgumentException nếu serial máy đổi trùng serial máy trả
   */
  public void setChiTietSanPhamDoi(SanPham chiTietSanPhamDoi) {
    this.chiTietSanPhamDoi = RangBuoc.batBuoc(chiTietSanPhamDoi, "chiTietSanPhamDoi");
    if (chiTietHoaDon != null
        && chiTietHoaDon.sanPham() != null
        && this.chiTietSanPhamDoi.soSerial() != null
        && this.chiTietSanPhamDoi.soSerial().equals(chiTietHoaDon.sanPham().soSerial())) {
      throw new IllegalArgumentException("Máy đổi không được trùng máy trả");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChiTietPhieuDoi khac = (ChiTietPhieuDoi) o;
    return Objects.equals(phieuDoi, khac.phieuDoi)
        && Objects.equals(chiTietHoaDon, khac.chiTietHoaDon)
        && Objects.equals(lyDoDoi, khac.lyDoDoi)
        && Objects.equals(tinhTrangHangTra, khac.tinhTrangHangTra)
        && Objects.equals(chiTietSanPhamDoi, khac.chiTietSanPhamDoi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phieuDoi, chiTietHoaDon, lyDoDoi, tinhTrangHangTra, chiTietSanPhamDoi);
  }

  @Override
  public String toString() {
    return "ChiTietPhieuDoi[phieuDoi=" + phieuDoi
        + ", chiTietHoaDon=" + chiTietHoaDon
        + ", lyDoDoi=" + lyDoDoi
        + ", tinhTrangHangTra=" + tinhTrangHangTra
        + ", chiTietSanPhamDoi=" + chiTietSanPhamDoi
        + "]";
  }
}
