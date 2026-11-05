package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Phiếu đổi hàng, ánh xạ bảng "PhieuDoi".
 *
 * <p>{@code tienKhachDuaThem} là phần khách trả thêm (luôn {@code >= 0}), còn
 * {@code tienChenhLech} là hiệu có dấu giữa giá trị máy mới và giá trị máy trả: âm nghĩa là cửa
 * hàng phải hoàn tiền, dương nghĩa là khách phải trả thêm, bằng 0 là đổi ngang.
 *
 * <p>Lớp giữ nguyên các accessor kiểu record ({@code maPhieuDoi()}, {@code hoaDon()}...) để không
 * phá vỡ các call site hiện có, đồng thời bổ sung cặp getter/setter theo kiểu JavaBean. Setter chỉ
 * kiểm tra các ràng buộc thuần trong bộ nhớ qua {@link RangBuoc}; phân quyền, khóa ngoại và tính
 * duy nhất do controller, DAO và cơ sở dữ liệu đảm nhiệm.
 */
public class PhieuDoi {

  private String maPhieuDoi;
  private NhanVien nhanVienLap;
  private HoaDon hoaDon;
  private LocalDate ngayHoanTat;
  private LocalDate ngayTao;
  private TrangThaiDuyet trangThaiDuyet;
  private TrangThaiXuLyPhieu trangThaiXuLyPhieu;
  private BigDecimal tienKhachDuaThem;
  private BigDecimal tienChenhLech;
  private NhanVien nhanVienDuyet;
  private LocalDate ngayDuyet;
  private String lyDoDuyet;

  public PhieuDoi() {}

  /**
   * Khởi tạo đầy đủ theo đúng thứ tự khai báo field.
   *
   * <p>Constructor chỉ gán giá trị, <b>không</b> gọi setter và <b>không</b> kiểm tra ràng buộc, để
   * các luồng đọc/đối chiếu dữ liệu (kể cả bản ghi chưa hoàn chỉnh) vẫn dựng được đối tượng.
   */
  public PhieuDoi(
      String maPhieuDoi,
      NhanVien nhanVienLap,
      HoaDon hoaDon,
      LocalDate ngayHoanTat,
      LocalDate ngayTao,
      TrangThaiDuyet trangThaiDuyet,
      TrangThaiXuLyPhieu trangThaiXuLyPhieu,
      BigDecimal tienKhachDuaThem,
      BigDecimal tienChenhLech,
      NhanVien nhanVienDuyet,
      LocalDate ngayDuyet,
      String lyDoDuyet) {
    this.maPhieuDoi = maPhieuDoi;
    this.nhanVienLap = nhanVienLap;
    this.hoaDon = hoaDon;
    this.ngayHoanTat = ngayHoanTat;
    this.ngayTao = ngayTao;
    this.trangThaiDuyet = trangThaiDuyet;
    this.trangThaiXuLyPhieu = trangThaiXuLyPhieu;
    this.tienKhachDuaThem = tienKhachDuaThem;
    this.tienChenhLech = tienChenhLech;
    this.nhanVienDuyet = nhanVienDuyet;
    this.ngayDuyet = ngayDuyet;
    this.lyDoDuyet = lyDoDuyet;
  }

  public String maPhieuDoi() {
    return maPhieuDoi;
  }

  /** Chỉ có getter: mã phiếu là định danh bất biến, do tầng ứng dụng sinh khi lưu thành công. */
  public String getMaPhieuDoi() {
    return maPhieuDoi;
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

  public LocalDate ngayTao() {
    return ngayTao;
  }

  public LocalDate getNgayTao() {
    return ngayTao;
  }

  public void setNgayTao(LocalDate ngayTao) {
    this.ngayTao = RangBuoc.batBuoc(ngayTao, "ngayTao");
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

  public BigDecimal tienKhachDuaThem() {
    return tienKhachDuaThem;
  }

  public BigDecimal getTienKhachDuaThem() {
    return tienKhachDuaThem;
  }

  /**
   * Gán số tiền khách đưa thêm.
   *
   * <p>Tài liệu DOCX chỉ yêu cầu {@code >= 0}; việc buộc phải bằng 0 khi đổi ngang do ràng buộc cơ
   * sở dữ liệu {@code ck_phieudoi_khong_thu_them} đảm nhiệm.
   *
   * @throws NullPointerException nếu {@code tienKhachDuaThem} là {@code null}
   * @throws IllegalArgumentException nếu {@code tienKhachDuaThem} âm
   */
  public void setTienKhachDuaThem(BigDecimal tienKhachDuaThem) {
    this.tienKhachDuaThem = RangBuoc.tienKhongAm(tienKhachDuaThem, "tienKhachDuaThem");
  }

  public BigDecimal tienChenhLech() {
    return tienChenhLech;
  }

  public BigDecimal getTienChenhLech() {
    return tienChenhLech;
  }

  /**
   * Gán tiền chênh lệch; cho phép {@code null} và chỉ nhận giá trị không dương.
   *
   * <p><b>Xung đột nguồn:</b> tài liệu DOCX mô tả {@code tienChenhLech} là hiệu có dấu (có thể
   * dương khi khách trả thêm), trong khi schema V7/V10 đã duyệt quy ước trường này là số tiền cửa
   * hàng hoàn lại nên không được dương — phần chênh khách trả thêm đã có
   * {@code tienKhachDuaThem}. Bản triển khai theo schema đã duyệt.
   *
   * @throws IllegalArgumentException nếu {@code tienChenhLech} dương
   */
  public void setTienChenhLech(BigDecimal tienChenhLech) {
    if (tienChenhLech != null && tienChenhLech.signum() > 0) {
      throw new IllegalArgumentException("tienChenhLech không được dương");
    }
    this.tienChenhLech = tienChenhLech;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PhieuDoi khac)) {
      return false;
    }
    return Objects.equals(maPhieuDoi, khac.maPhieuDoi)
        && Objects.equals(nhanVienLap, khac.nhanVienLap)
        && Objects.equals(hoaDon, khac.hoaDon)
        && Objects.equals(ngayHoanTat, khac.ngayHoanTat)
        && Objects.equals(ngayTao, khac.ngayTao)
        && trangThaiDuyet == khac.trangThaiDuyet
        && trangThaiXuLyPhieu == khac.trangThaiXuLyPhieu
        && Objects.equals(tienKhachDuaThem, khac.tienKhachDuaThem)
        && Objects.equals(tienChenhLech, khac.tienChenhLech)
        && Objects.equals(nhanVienDuyet, khac.nhanVienDuyet)
        && Objects.equals(ngayDuyet, khac.ngayDuyet)
        && Objects.equals(lyDoDuyet, khac.lyDoDuyet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maPhieuDoi,
        nhanVienLap,
        hoaDon,
        ngayHoanTat,
        ngayTao,
        trangThaiDuyet,
        trangThaiXuLyPhieu,
        tienKhachDuaThem,
        tienChenhLech,
        nhanVienDuyet,
        ngayDuyet,
        lyDoDuyet);
  }

  @Override
  public String toString() {
    return "PhieuDoi[maPhieuDoi="
        + maPhieuDoi
        + ", nhanVienLap="
        + nhanVienLap
        + ", hoaDon="
        + hoaDon
        + ", ngayHoanTat="
        + ngayHoanTat
        + ", ngayTao="
        + ngayTao
        + ", trangThaiDuyet="
        + trangThaiDuyet
        + ", trangThaiXuLyPhieu="
        + trangThaiXuLyPhieu
        + ", tienKhachDuaThem="
        + tienKhachDuaThem
        + ", tienChenhLech="
        + tienChenhLech
        + ", nhanVienDuyet="
        + nhanVienDuyet
        + ", ngayDuyet="
        + ngayDuyet
        + ", lyDoDuyet="
        + lyDoDuyet
        + "]";
  }
}
