package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Phiếu trả hàng, ánh xạ bảng "PhieuTra".
 *
 * <p>Lớp giữ nguyên các accessor kiểu record ({@code maPhieuTra()}, {@code hoaDon()}...) để không
 * phá vỡ các call site hiện có, đồng thời bổ sung cặp getter/setter theo kiểu JavaBean. Setter chỉ
 * kiểm tra các ràng buộc thuần trong bộ nhớ qua {@link RangBuoc}; phân quyền, khóa ngoại và tính
 * duy nhất do controller, DAO và cơ sở dữ liệu đảm nhiệm.
 */
public class PhieuTra {

  private String maPhieuTra;
  private NhanVien nhanVienLap;
  private HoaDon hoaDon;
  private LocalDate ngayTao;
  private LocalDate ngayHoanTat;
  private TrangThaiDuyet trangThaiDuyet;
  private TrangThaiXuLyPhieu trangThaiXuLyPhieu;
  private NhanVien nhanVienDuyet;
  private LocalDate ngayDuyet;
  private String lyDoDuyet;
  private BigDecimal tienKhachDua;

  public PhieuTra() {}

  /**
   * Khởi tạo đầy đủ theo đúng thứ tự khai báo field.
   *
   * <p>Constructor chỉ gán giá trị, <b>không</b> gọi setter và <b>không</b> kiểm tra ràng buộc, để
   * các luồng đọc/đối chiếu dữ liệu (kể cả bản ghi chưa hoàn chỉnh) vẫn dựng được đối tượng.
   */
  public PhieuTra(
      String maPhieuTra,
      NhanVien nhanVienLap,
      HoaDon hoaDon,
      LocalDate ngayTao,
      LocalDate ngayHoanTat,
      TrangThaiDuyet trangThaiDuyet,
      TrangThaiXuLyPhieu trangThaiXuLyPhieu,
      NhanVien nhanVienDuyet,
      LocalDate ngayDuyet,
      String lyDoDuyet,
      BigDecimal tienKhachDua) {
    this.maPhieuTra = maPhieuTra;
    this.nhanVienLap = nhanVienLap;
    this.hoaDon = hoaDon;
    this.ngayTao = ngayTao;
    this.ngayHoanTat = ngayHoanTat;
    this.trangThaiDuyet = trangThaiDuyet;
    this.trangThaiXuLyPhieu = trangThaiXuLyPhieu;
    this.nhanVienDuyet = nhanVienDuyet;
    this.ngayDuyet = ngayDuyet;
    this.lyDoDuyet = lyDoDuyet;
    this.tienKhachDua = tienKhachDua;
  }

  public String maPhieuTra() {
    return maPhieuTra;
  }

  /** Chỉ có getter: mã phiếu là định danh bất biến, do tầng ứng dụng sinh khi lưu thành công. */
  public String getMaPhieuTra() {
    return maPhieuTra;
  }

  public NhanVien nhanVienLap() {
    return nhanVienLap;
  }

  public NhanVien getNhanVienLap() {
    return nhanVienLap;
  }

  public void setNhanVienLap(NhanVien nhanVienLap) {
    this.nhanVienLap = RangBuoc.batBuoc(nhanVienLap, "nhanVienLap");
  }

  public HoaDon hoaDon() {
    return hoaDon;
  }

  public HoaDon getHoaDon() {
    return hoaDon;
  }

  public void setHoaDon(HoaDon hoaDon) {
    this.hoaDon = RangBuoc.batBuoc(hoaDon, "hoaDon");
  }

  public LocalDate ngayTao() {
    return ngayTao;
  }

  public LocalDate getNgayTao() {
    return ngayTao;
  }

  public void setNgayTao(LocalDate ngayTao) {
    this.ngayTao = RangBuoc.batBuoc(ngayTao, "ngayTao");
  }

  public LocalDate ngayHoanTat() {
    return ngayHoanTat;
  }

  public LocalDate getNgayHoanTat() {
    return ngayHoanTat;
  }

  /**
   * Gán ngày hoàn tất.
   *
   * @throws IllegalArgumentException nếu {@code ngayHoanTat} trước {@code ngayTao}, hoặc phiếu đang
   *     ở trạng thái {@code HOAN_TAT} mà giá trị mới là {@code null}
   */
  public void setNgayHoanTat(LocalDate ngayHoanTat) {
    RangBuoc.khongTruoc(ngayTao, ngayHoanTat, "ngayHoanTat không được trước ngayTao");
    if (trangThaiXuLyPhieu == TrangThaiXuLyPhieu.HOAN_TAT && ngayHoanTat == null) {
      throw new IllegalArgumentException("Phiếu HOAN_TAT phải có ngayHoanTat");
    }
    this.ngayHoanTat = ngayHoanTat;
  }

  public TrangThaiDuyet trangThaiDuyet() {
    return trangThaiDuyet;
  }

  public TrangThaiDuyet getTrangThaiDuyet() {
    return trangThaiDuyet;
  }

  /**
   * Gán trạng thái duyệt kèm kiểm tra RB55.
   *
   * <p>Hãy gọi {@code setNhanVienDuyet}/{@code setNgayDuyet}/{@code setLyDoDuyet} trước
   * {@code setTrangThaiDuyet}, vì setter này đọc lại các field đó để kiểm tra.
   *
   * @throws NullPointerException nếu {@code trangThaiDuyet} là {@code null}
   * @throws IllegalArgumentException nếu trạng thái là {@code DA_DUYET}/{@code TU_CHOI} mà thiếu
   *     người duyệt hoặc ngày duyệt (RB55), hoặc {@code TU_CHOI} mà thiếu lý do duyệt
   */
  public void setTrangThaiDuyet(TrangThaiDuyet trangThaiDuyet) {
    RangBuoc.batBuoc(trangThaiDuyet, "trangThaiDuyet");
    boolean daQuyetDinh =
        trangThaiDuyet == TrangThaiDuyet.DA_DUYET || trangThaiDuyet == TrangThaiDuyet.TU_CHOI;
    if (daQuyetDinh && (nhanVienDuyet == null || ngayDuyet == null)) {
      throw new IllegalArgumentException(
          "Trạng thái " + trangThaiDuyet + " phải có nhanVienDuyet và ngayDuyet");
    }
    if (trangThaiDuyet == TrangThaiDuyet.TU_CHOI
        && (lyDoDuyet == null || lyDoDuyet.isBlank())) {
      throw new IllegalArgumentException("Phiếu bị từ chối phải có lyDoDuyet");
    }
    this.trangThaiDuyet = trangThaiDuyet;
  }

  public TrangThaiXuLyPhieu trangThaiXuLyPhieu() {
    return trangThaiXuLyPhieu;
  }

  public TrangThaiXuLyPhieu getTrangThaiXuLyPhieu() {
    return trangThaiXuLyPhieu;
  }

  /**
   * Gán trạng thái xử lý của phiếu.
   *
   * @throws NullPointerException nếu {@code trangThaiXuLyPhieu} là {@code null}
   * @throws IllegalArgumentException nếu trạng thái là {@code HOAN_TAT} mà chưa có
   *     {@code ngayHoanTat}
   */
  public void setTrangThaiXuLyPhieu(TrangThaiXuLyPhieu trangThaiXuLyPhieu) {
    RangBuoc.batBuoc(trangThaiXuLyPhieu, "trangThaiXuLyPhieu");
    if (trangThaiXuLyPhieu == TrangThaiXuLyPhieu.HOAN_TAT && ngayHoanTat == null) {
      throw new IllegalArgumentException("Phiếu HOAN_TAT phải có ngayHoanTat");
    }
    this.trangThaiXuLyPhieu = trangThaiXuLyPhieu;
  }

  public NhanVien nhanVienDuyet() {
    return nhanVienDuyet;
  }

  public NhanVien getNhanVienDuyet() {
    return nhanVienDuyet;
  }

  /** Cho phép {@code null}: phiếu chưa cần duyệt thì chưa có người duyệt. */
  public void setNhanVienDuyet(NhanVien nhanVienDuyet) {
    this.nhanVienDuyet = nhanVienDuyet;
  }

  public LocalDate ngayDuyet() {
    return ngayDuyet;
  }

  public LocalDate getNgayDuyet() {
    return ngayDuyet;
  }

  /**
   * Gán ngày duyệt.
   *
   * @throws IllegalArgumentException nếu {@code ngayDuyet} trước {@code ngayTao} hoặc sau
   *     {@code ngayHoanTat}
   */
  public void setNgayDuyet(LocalDate ngayDuyet) {
    RangBuoc.khongTruoc(ngayTao, ngayDuyet, "ngayDuyet không được trước ngayTao");
    RangBuoc.khongTruoc(ngayDuyet, ngayHoanTat, "ngayDuyet không được sau ngayHoanTat");
    this.ngayDuyet = ngayDuyet;
  }

  public String lyDoDuyet() {
    return lyDoDuyet;
  }

  public String getLyDoDuyet() {
    return lyDoDuyet;
  }

  /** Chuỗi tùy chọn: {@code null} và chuỗi rỗng đều được chuẩn hóa thành {@code null}. */
  public void setLyDoDuyet(String lyDoDuyet) {
    this.lyDoDuyet = RangBuoc.chuoiTuyChon(lyDoDuyet);
  }

  public BigDecimal tienKhachDua() {
    return tienKhachDua;
  }

  public BigDecimal getTienKhachDua() {
    return tienKhachDua;
  }

  /**
   * Gán số tiền khách đưa.
   *
   * @throws NullPointerException nếu {@code tienKhachDua} là {@code null}
   * @throws IllegalArgumentException nếu {@code tienKhachDua} âm
   */
  public void setTienKhachDua(BigDecimal tienKhachDua) {
    this.tienKhachDua = RangBuoc.tienKhongAm(tienKhachDua, "tienKhachDua");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PhieuTra khac)) {
      return false;
    }
    return Objects.equals(maPhieuTra, khac.maPhieuTra)
        && Objects.equals(nhanVienLap, khac.nhanVienLap)
        && Objects.equals(hoaDon, khac.hoaDon)
        && Objects.equals(ngayTao, khac.ngayTao)
        && Objects.equals(ngayHoanTat, khac.ngayHoanTat)
        && trangThaiDuyet == khac.trangThaiDuyet
        && trangThaiXuLyPhieu == khac.trangThaiXuLyPhieu
        && Objects.equals(nhanVienDuyet, khac.nhanVienDuyet)
        && Objects.equals(ngayDuyet, khac.ngayDuyet)
        && Objects.equals(lyDoDuyet, khac.lyDoDuyet)
        && Objects.equals(tienKhachDua, khac.tienKhachDua);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maPhieuTra,
        nhanVienLap,
        hoaDon,
        ngayTao,
        ngayHoanTat,
        trangThaiDuyet,
        trangThaiXuLyPhieu,
        nhanVienDuyet,
        ngayDuyet,
        lyDoDuyet,
        tienKhachDua);
  }

  @Override
  public String toString() {
    return "PhieuTra[maPhieuTra="
        + maPhieuTra
        + ", nhanVienLap="
        + nhanVienLap
        + ", hoaDon="
        + hoaDon
        + ", ngayTao="
        + ngayTao
        + ", ngayHoanTat="
        + ngayHoanTat
        + ", trangThaiDuyet="
        + trangThaiDuyet
        + ", trangThaiXuLyPhieu="
        + trangThaiXuLyPhieu
        + ", nhanVienDuyet="
        + nhanVienDuyet
        + ", ngayDuyet="
        + ngayDuyet
        + ", lyDoDuyet="
        + lyDoDuyet
        + ", tienKhachDua="
        + tienKhachDua
        + "]";
  }
}
