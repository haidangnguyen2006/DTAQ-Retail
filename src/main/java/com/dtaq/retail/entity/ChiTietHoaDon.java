package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.util.Objects;

/** Dòng chi tiết hóa đơn, ánh xạ bảng "ChiTietHoaDon". */
public class ChiTietHoaDon {

  private HoaDon hoaDon;
  private SanPham sanPham;
  private int soLuong;
  private BigDecimal donGiaBanTaiLucBan;
  private BigDecimal thueSuatTaiLucBan;
  private KhuyenMai khuyenMai;
  private BigDecimal tyLeGiamTaiLucBan;
  private BigDecimal tienKhuyenMaiThucTe;
  private BigDecimal tienTruocKhuyenMai;
  private BigDecimal tienSauKhuyenMai;
  private BigDecimal tienThue;

  /** Tạo dòng chi tiết rỗng. */
  public ChiTietHoaDon() {}

  /** Tạo dòng chi tiết đầy đủ; không kiểm tra ràng buộc. */
  public ChiTietHoaDon(
      HoaDon hoaDon,
      SanPham sanPham,
      int soLuong,
      BigDecimal donGiaBanTaiLucBan,
      BigDecimal thueSuatTaiLucBan,
      KhuyenMai khuyenMai,
      BigDecimal tyLeGiamTaiLucBan,
      BigDecimal tienKhuyenMaiThucTe,
      BigDecimal tienTruocKhuyenMai,
      BigDecimal tienSauKhuyenMai,
      BigDecimal tienThue) {
    this.hoaDon = hoaDon;
    this.sanPham = sanPham;
    this.soLuong = soLuong;
    this.donGiaBanTaiLucBan = donGiaBanTaiLucBan;
    this.thueSuatTaiLucBan = thueSuatTaiLucBan;
    this.khuyenMai = khuyenMai;
    this.tyLeGiamTaiLucBan = tyLeGiamTaiLucBan;
    this.tienKhuyenMaiThucTe = tienKhuyenMaiThucTe;
    this.tienTruocKhuyenMai = tienTruocKhuyenMai;
    this.tienSauKhuyenMai = tienSauKhuyenMai;
    this.tienThue = tienThue;
  }

  public HoaDon hoaDon() {
    return hoaDon;
  }

  public HoaDon getHoaDon() {
    return hoaDon;
  }

  /**
   * Gán hóa đơn chứa dòng chi tiết.
   *
   * @throws NullPointerException nếu {@code hoaDon} là {@code null}
   */
  public void setHoaDon(HoaDon hoaDon) {
    this.hoaDon = RangBuoc.batBuoc(hoaDon, "hoaDon");
  }

  public SanPham sanPham() {
    return sanPham;
  }

  public SanPham getSanPham() {
    return sanPham;
  }

  /**
   * Gán máy được bán trong dòng chi tiết.
   *
   * @throws NullPointerException nếu {@code sanPham} là {@code null}
   */
  public void setSanPham(SanPham sanPham) {
    this.sanPham = RangBuoc.batBuoc(sanPham, "sanPham");
  }

  public int soLuong() {
    return soLuong;
  }

  public int getSoLuong() {
    return soLuong;
  }

  /**
   * Gán số lượng của dòng chi tiết.
   *
   * @throws IllegalArgumentException nếu {@code soLuong} khác {@code 1} (RB12/D15)
   */
  public void setSoLuong(int soLuong) {
    if (soLuong != 1) {
      throw new IllegalArgumentException("Số lượng mỗi dòng chi tiết phải bằng 1");
    }
    this.soLuong = soLuong;
  }

  public BigDecimal donGiaBanTaiLucBan() {
    return donGiaBanTaiLucBan;
  }

  public BigDecimal getDonGiaBanTaiLucBan() {
    return donGiaBanTaiLucBan;
  }

  /**
   * Gán đơn giá bán tại thời điểm bán.
   *
   * @throws NullPointerException nếu {@code donGiaBanTaiLucBan} là {@code null}
   * @throws IllegalArgumentException nếu {@code donGiaBanTaiLucBan} âm
   */
  public void setDonGiaBanTaiLucBan(BigDecimal donGiaBanTaiLucBan) {
    this.donGiaBanTaiLucBan =
        RangBuoc.tienKhongAm(donGiaBanTaiLucBan, "donGiaBanTaiLucBan");
  }

  public BigDecimal thueSuatTaiLucBan() {
    return thueSuatTaiLucBan;
  }

  public BigDecimal getThueSuatTaiLucBan() {
    return thueSuatTaiLucBan;
  }

  /**
   * Gán thuế suất tại thời điểm bán.
   *
   * @throws NullPointerException nếu {@code thueSuatTaiLucBan} là {@code null}
   * @throws IllegalArgumentException nếu ngoài khoảng {@code 0..100}
   */
  public void setThueSuatTaiLucBan(BigDecimal thueSuatTaiLucBan) {
    this.thueSuatTaiLucBan =
        RangBuoc.phanTram(thueSuatTaiLucBan, "thueSuatTaiLucBan");
  }

  public KhuyenMai khuyenMai() {
    return khuyenMai;
  }

  public KhuyenMai getKhuyenMai() {
    return khuyenMai;
  }

  /** Gán khuyến mãi áp dụng tại thời điểm bán; cho phép {@code null}. */
  public void setKhuyenMai(KhuyenMai khuyenMai) {
    this.khuyenMai = khuyenMai;
  }

  public BigDecimal tyLeGiamTaiLucBan() {
    return tyLeGiamTaiLucBan;
  }

  public BigDecimal getTyLeGiamTaiLucBan() {
    return tyLeGiamTaiLucBan;
  }

  /**
   * Gán tỷ lệ giảm tại thời điểm bán; cho phép {@code null}.
   *
   * @throws IllegalArgumentException nếu khác {@code null} và ngoài khoảng {@code 0..100}
   */
  public void setTyLeGiamTaiLucBan(BigDecimal tyLeGiamTaiLucBan) {
    this.tyLeGiamTaiLucBan =
        RangBuoc.phanTramTuyChon(tyLeGiamTaiLucBan, "tyLeGiamTaiLucBan");
  }

  public BigDecimal tienKhuyenMaiThucTe() {
    return tienKhuyenMaiThucTe;
  }

  public BigDecimal getTienKhuyenMaiThucTe() {
    return tienKhuyenMaiThucTe;
  }

  /**
   * Gán tiền khuyến mãi thực tế.
   *
   * @throws NullPointerException nếu {@code tienKhuyenMaiThucTe} là {@code null}
   * @throws IllegalArgumentException nếu {@code tienKhuyenMaiThucTe} âm
   */
  public void setTienKhuyenMaiThucTe(BigDecimal tienKhuyenMaiThucTe) {
    this.tienKhuyenMaiThucTe =
        RangBuoc.tienKhongAm(tienKhuyenMaiThucTe, "tienKhuyenMaiThucTe");
  }

  public BigDecimal tienTruocKhuyenMai() {
    return tienTruocKhuyenMai;
  }

  public BigDecimal getTienTruocKhuyenMai() {
    return tienTruocKhuyenMai;
  }

  /**
   * Gán tiền trước khuyến mãi.
   *
   * @throws NullPointerException nếu {@code tienTruocKhuyenMai} là {@code null}
   * @throws IllegalArgumentException nếu {@code tienTruocKhuyenMai} âm
   */
  public void setTienTruocKhuyenMai(BigDecimal tienTruocKhuyenMai) {
    this.tienTruocKhuyenMai =
        RangBuoc.tienKhongAm(tienTruocKhuyenMai, "tienTruocKhuyenMai");
  }

  public BigDecimal tienSauKhuyenMai() {
    return tienSauKhuyenMai;
  }

  public BigDecimal getTienSauKhuyenMai() {
    return tienSauKhuyenMai;
  }

  /**
   * Gán tiền sau khuyến mãi, không được lớn hơn tiền trước khuyến mãi.
   *
   * @throws NullPointerException nếu {@code tienSauKhuyenMai} là {@code null}
   * @throws IllegalArgumentException nếu {@code tienSauKhuyenMai} âm hoặc lớn hơn
   *     {@code tienTruocKhuyenMai}
   */
  public void setTienSauKhuyenMai(BigDecimal tienSauKhuyenMai) {
    this.tienSauKhuyenMai = RangBuoc.tienKhongAm(tienSauKhuyenMai, "tienSauKhuyenMai");
    if (tienTruocKhuyenMai != null
        && this.tienSauKhuyenMai.compareTo(tienTruocKhuyenMai) > 0) {
      throw new IllegalArgumentException(
          "tienSauKhuyenMai không được lớn hơn tienTruocKhuyenMai");
    }
  }

  public BigDecimal tienThue() {
    return tienThue;
  }

  public BigDecimal getTienThue() {
    return tienThue;
  }

  /**
   * Gán tiền thuế của dòng chi tiết.
   *
   * @throws NullPointerException nếu {@code tienThue} là {@code null}
   * @throws IllegalArgumentException nếu {@code tienThue} âm
   */
  public void setTienThue(BigDecimal tienThue) {
    this.tienThue = RangBuoc.tienKhongAm(tienThue, "tienThue");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChiTietHoaDon khac = (ChiTietHoaDon) o;
    return soLuong == khac.soLuong
        && Objects.equals(hoaDon, khac.hoaDon)
        && Objects.equals(sanPham, khac.sanPham)
        && Objects.equals(donGiaBanTaiLucBan, khac.donGiaBanTaiLucBan)
        && Objects.equals(thueSuatTaiLucBan, khac.thueSuatTaiLucBan)
        && Objects.equals(khuyenMai, khac.khuyenMai)
        && Objects.equals(tyLeGiamTaiLucBan, khac.tyLeGiamTaiLucBan)
        && Objects.equals(tienKhuyenMaiThucTe, khac.tienKhuyenMaiThucTe)
        && Objects.equals(tienTruocKhuyenMai, khac.tienTruocKhuyenMai)
        && Objects.equals(tienSauKhuyenMai, khac.tienSauKhuyenMai)
        && Objects.equals(tienThue, khac.tienThue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        hoaDon,
        sanPham,
        soLuong,
        donGiaBanTaiLucBan,
        thueSuatTaiLucBan,
        khuyenMai,
        tyLeGiamTaiLucBan,
        tienKhuyenMaiThucTe,
        tienTruocKhuyenMai,
        tienSauKhuyenMai,
        tienThue);
  }

  /** Mô tả dòng chi tiết, không in lại hóa đơn cha. */
  @Override
  public String toString() {
    return "ChiTietHoaDon[maHD=" + (hoaDon == null ? null : hoaDon.maHD())
        + ", soSerial=" + (sanPham == null ? null : sanPham.soSerial())
        + ", soLuong=" + soLuong
        + ", tienSauKhuyenMai=" + tienSauKhuyenMai
        + "]";
  }
}
