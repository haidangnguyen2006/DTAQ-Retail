package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Hóa đơn bán lẻ, ánh xạ bảng "HoaDon". */
public class HoaDon {

  private static final String CHI_TIET_TRONG =
      "Danh sách chi tiết không được để trống";

  private String maHD;
  private LocalDate ngayLap;
  private NhanVien nhanVienLap;
  private KhachHang khachHang;
  private boolean trangThaiThanhToan;

  /** Tạo hóa đơn rỗng. */
  public HoaDon() {}

  /** Tạo hóa đơn đầy đủ; không kiểm tra ràng buộc. */
  public HoaDon(
      String maHD,
      LocalDate ngayLap,
      NhanVien nhanVienLap,
      KhachHang khachHang,
      boolean trangThaiThanhToan) {
    this.maHD = maHD;
    this.ngayLap = ngayLap;
    this.nhanVienLap = nhanVienLap;
    this.khachHang = khachHang;
    this.trangThaiThanhToan = trangThaiThanhToan;
  }

  /** Mã hóa đơn, chỉ đọc sau khi tạo. */
  public String maHD() {
    return maHD;
  }

  /** Mã hóa đơn, chỉ đọc sau khi tạo. */
  public String getMaHD() {
    return maHD;
  }

  public LocalDate ngayLap() {
    return ngayLap;
  }

  public LocalDate getNgayLap() {
    return ngayLap;
  }

  /**
   * Gán ngày lập hóa đơn.
   *
   * @throws NullPointerException nếu {@code ngayLap} là {@code null}
   */
  public void setNgayLap(LocalDate ngayLap) {
    this.ngayLap = RangBuoc.batBuoc(ngayLap, "ngayLap");
  }

  public NhanVien nhanVienLap() {
    return nhanVienLap;
  }

  public NhanVien getNhanVienLap() {
    return nhanVienLap;
  }

  /**
   * Gán nhân viên lập hóa đơn.
   *
   * @throws NullPointerException nếu {@code nhanVienLap} là {@code null}
   */
  public void setNhanVienLap(NhanVien nhanVienLap) {
    this.nhanVienLap = RangBuoc.batBuoc(nhanVienLap, "nhanVienLap");
  }

  public KhachHang khachHang() {
    return khachHang;
  }

  public KhachHang getKhachHang() {
    return khachHang;
  }

  /**
   * Gán khách hàng của hóa đơn.
   *
   * @throws NullPointerException nếu {@code khachHang} là {@code null}
   */
  public void setKhachHang(KhachHang khachHang) {
    this.khachHang = RangBuoc.batBuoc(khachHang, "khachHang");
  }

  public boolean trangThaiThanhToan() {
    return trangThaiThanhToan;
  }

  public boolean isTrangThaiThanhToan() {
    return trangThaiThanhToan;
  }

  /** Gán trực tiếp trạng thái thanh toán; quy tắc không đảo cờ thuộc controller/DAO. */
  public void setTrangThaiThanhToan(boolean trangThaiThanhToan) {
    this.trangThaiThanhToan = trangThaiThanhToan;
  }

  /** Tổng tiền trước thuế, cộng các giá trị "tienSauKhuyenMai" đã lưu. */
  public BigDecimal tongTienTruocThue(List<ChiTietHoaDon> chiTiet) {
    kiemTraChiTiet(chiTiet);
    return chiTiet.stream()
        .map(ChiTietHoaDon::tienSauKhuyenMai)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Tổng tiền thuế, cộng các giá trị "tienThue" đã lưu. */
  public BigDecimal tongTienThue(List<ChiTietHoaDon> chiTiet) {
    kiemTraChiTiet(chiTiet);
    return chiTiet.stream()
        .map(ChiTietHoaDon::tienThue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Tổng tiền thanh toán, bằng tổng tiền trước thuế cộng tổng tiền thuế. */
  public BigDecimal tongTienThanhToan(List<ChiTietHoaDon> chiTiet) {
    return tongTienTruocThue(chiTiet).add(tongTienThue(chiTiet));
  }

  private static void kiemTraChiTiet(List<ChiTietHoaDon> chiTiet) {
    if (chiTiet == null) {
      throw new IllegalArgumentException(CHI_TIET_TRONG);
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
    HoaDon khac = (HoaDon) o;
    return trangThaiThanhToan == khac.trangThaiThanhToan
        && Objects.equals(maHD, khac.maHD)
        && Objects.equals(ngayLap, khac.ngayLap)
        && Objects.equals(nhanVienLap, khac.nhanVienLap)
        && Objects.equals(khachHang, khac.khachHang);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maHD, ngayLap, nhanVienLap, khachHang, trangThaiThanhToan);
  }

  /** Mô tả hóa đơn theo khóa ngoại, không duyệt danh sách chi tiết. */
  @Override
  public String toString() {
    return "HoaDon[maHD=" + maHD
        + ", ngayLap=" + ngayLap
        + ", nhanVienLap=" + (nhanVienLap == null ? null : nhanVienLap.maNV())
        + ", khachHang=" + (khachHang == null ? null : khachHang.maKH())
        + ", trangThaiThanhToan=" + trangThaiThanhToan
        + "]";
  }
}
