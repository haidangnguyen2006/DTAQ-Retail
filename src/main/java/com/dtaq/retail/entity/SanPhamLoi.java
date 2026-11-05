package com.dtaq.retail.entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bản ghi sản phẩm lỗi, ánh xạ bảng "SanPhamLoi".
 *
 * <p>Lớp giữ nguyên các accessor kiểu record ({@code maLoi()}, {@code sanPham()}...) để không phá
 * vỡ các call site hiện có, đồng thời bổ sung cặp getter/setter theo kiểu JavaBean. Setter chỉ kiểm
 * tra các ràng buộc thuần trong bộ nhớ qua {@link RangBuoc}; phân quyền, khóa ngoại và tính duy
 * nhất do controller, DAO và cơ sở dữ liệu đảm nhiệm.
 */
public class SanPhamLoi {

  private String maLoi;
  private SanPham sanPham;
  private PhieuTra phieuTra;
  private PhieuDoi phieuDoi;
  private String ghiChuXuLy;
  private String moTaLoi;
  private LocalDate ngayGhiNhan;
  private TrangThaiXuLyLoi tinhTrangXuLy;
  private LocalDate ngayCapNhap;

  public SanPhamLoi() {}

  /**
   * Khởi tạo đầy đủ theo đúng thứ tự khai báo field.
   *
   * <p>Constructor chỉ gán giá trị, <b>không</b> gọi setter và <b>không</b> kiểm tra ràng buộc, để
   * các luồng đọc/đối chiếu dữ liệu (kể cả bản ghi chưa hoàn chỉnh) vẫn dựng được đối tượng.
   */
  public SanPhamLoi(
      String maLoi,
      SanPham sanPham,
      PhieuTra phieuTra,
      PhieuDoi phieuDoi,
      String ghiChuXuLy,
      String moTaLoi,
      LocalDate ngayGhiNhan,
      TrangThaiXuLyLoi tinhTrangXuLy,
      LocalDate ngayCapNhap) {
    this.maLoi = maLoi;
    this.sanPham = sanPham;
    this.phieuTra = phieuTra;
    this.phieuDoi = phieuDoi;
    this.ghiChuXuLy = ghiChuXuLy;
    this.moTaLoi = moTaLoi;
    this.ngayGhiNhan = ngayGhiNhan;
    this.tinhTrangXuLy = tinhTrangXuLy;
    this.ngayCapNhap = ngayCapNhap;
  }

  public String maLoi() {
    return maLoi;
  }

  /** Chỉ có getter: mã lỗi là định danh bất biến, do tầng ứng dụng sinh khi lưu thành công. */
  public String getMaLoi() {
    return maLoi;
  }

  public SanPham sanPham() {
    return sanPham;
  }

  public SanPham getSanPham() {
    return sanPham;
  }

  public void setSanPham(SanPham sanPham) {
    this.sanPham = RangBuoc.batBuoc(sanPham, "sanPham");
  }

  public PhieuTra phieuTra() {
    return phieuTra;
  }

  public PhieuTra getPhieuTra() {
    return phieuTra;
  }

  /** Cho phép {@code null}: lỗi ghi nhận trực tiếp không gắn với phiếu trả nào. */
  public void setPhieuTra(PhieuTra phieuTra) {
    this.phieuTra = phieuTra;
  }

  public PhieuDoi phieuDoi() {
    return phieuDoi;
  }

  public PhieuDoi getPhieuDoi() {
    return phieuDoi;
  }

  /** Cho phép {@code null}: lỗi ghi nhận trực tiếp không gắn với phiếu đổi nào. */
  public void setPhieuDoi(PhieuDoi phieuDoi) {
    this.phieuDoi = phieuDoi;
  }

  public String ghiChuXuLy() {
    return ghiChuXuLy;
  }

  public String getGhiChuXuLy() {
    return ghiChuXuLy;
  }

  /** Chuỗi tùy chọn: {@code null} và chuỗi rỗng đều được chuẩn hóa thành {@code null}. */
  public void setGhiChuXuLy(String ghiChuXuLy) {
    this.ghiChuXuLy = RangBuoc.chuoiTuyChon(ghiChuXuLy);
  }

  public String moTaLoi() {
    return moTaLoi;
  }

  public String getMoTaLoi() {
    return moTaLoi;
  }

  /**
   * Gán mô tả lỗi (bắt buộc).
   *
   * @throws NullPointerException nếu {@code moTaLoi} là {@code null}
   * @throws IllegalArgumentException nếu {@code moTaLoi} chỉ gồm khoảng trắng
   */
  public void setMoTaLoi(String moTaLoi) {
    this.moTaLoi = RangBuoc.chuoiBatBuoc(moTaLoi, "moTaLoi");
  }

  public LocalDate ngayGhiNhan() {
    return ngayGhiNhan;
  }

  public LocalDate getNgayGhiNhan() {
    return ngayGhiNhan;
  }

  public void setNgayGhiNhan(LocalDate ngayGhiNhan) {
    this.ngayGhiNhan = RangBuoc.batBuoc(ngayGhiNhan, "ngayGhiNhan");
  }

  public TrangThaiXuLyLoi tinhTrangXuLy() {
    return tinhTrangXuLy;
  }

  public TrangThaiXuLyLoi getTinhTrangXuLy() {
    return tinhTrangXuLy;
  }

  public void setTinhTrangXuLy(TrangThaiXuLyLoi tinhTrangXuLy) {
    this.tinhTrangXuLy = RangBuoc.batBuoc(tinhTrangXuLy, "tinhTrangXuLy");
  }

  public LocalDate ngayCapNhap() {
    return ngayCapNhap;
  }

  public LocalDate getNgayCapNhap() {
    return ngayCapNhap;
  }

  /**
   * Gán ngày cập nhật.
   *
   * @throws IllegalArgumentException nếu {@code ngayCapNhap} trước {@code ngayGhiNhan}
   */
  public void setNgayCapNhap(LocalDate ngayCapNhap) {
    RangBuoc.khongTruoc(ngayGhiNhan, ngayCapNhap, "ngayCapNhap không được trước ngayGhiNhan");
    this.ngayCapNhap = ngayCapNhap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SanPhamLoi khac)) {
      return false;
    }
    return Objects.equals(maLoi, khac.maLoi)
        && Objects.equals(sanPham, khac.sanPham)
        && Objects.equals(phieuTra, khac.phieuTra)
        && Objects.equals(phieuDoi, khac.phieuDoi)
        && Objects.equals(ghiChuXuLy, khac.ghiChuXuLy)
        && Objects.equals(moTaLoi, khac.moTaLoi)
        && Objects.equals(ngayGhiNhan, khac.ngayGhiNhan)
        && tinhTrangXuLy == khac.tinhTrangXuLy
        && Objects.equals(ngayCapNhap, khac.ngayCapNhap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maLoi,
        sanPham,
        phieuTra,
        phieuDoi,
        ghiChuXuLy,
        moTaLoi,
        ngayGhiNhan,
        tinhTrangXuLy,
        ngayCapNhap);
  }

  @Override
  public String toString() {
    return "SanPhamLoi[maLoi="
        + maLoi
        + ", sanPham="
        + sanPham
        + ", phieuTra="
        + phieuTra
        + ", phieuDoi="
        + phieuDoi
        + ", ghiChuXuLy="
        + ghiChuXuLy
        + ", moTaLoi="
        + moTaLoi
        + ", ngayGhiNhan="
        + ngayGhiNhan
        + ", tinhTrangXuLy="
        + tinhTrangXuLy
        + ", ngayCapNhap="
        + ngayCapNhap
        + "]";
  }
}
