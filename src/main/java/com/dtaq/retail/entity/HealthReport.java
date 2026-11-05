package com.dtaq.retail.entity;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kết quả một lần kiểm tra sức khỏe dữ liệu.
 *
 * <p>Báo cáo là ảnh chụp tại thời điểm {@code thoiDiem}: số dòng hiện tại của từng bảng cốt lõi,
 * baseline đã lưu, tỷ lệ suy giảm phát hiện được, danh sách bảng còn thiếu và SQLState nếu lần
 * kiểm tra thất bại. Các tập {@code null} được chuẩn hóa thành tập rỗng bất biến để nơi nhận không
 * phải kiểm tra riêng.
 */
public class HealthReport {

  /** Trạng thái tổng hợp; không được {@code null}. */
  private HealthStatus trangThai;

  /** Số dòng hiện tại theo tên bảng; bất biến sau khi chuẩn hóa. */
  private Map<String, Long> soDong;

  /** Số dòng baseline theo tên bảng; bất biến sau khi chuẩn hóa. */
  private Map<String, Long> baseline;

  /** Tỷ lệ suy giảm theo tên bảng, trong khoảng {@code (0, 1]}; bất biến sau khi chuẩn hóa. */
  private Map<String, Double> tyLeGiam;

  /** Các bảng cốt lõi không tồn tại trong schema {@code public}; bất biến sau khi chuẩn hóa. */
  private List<String> bangThieu;

  /** Mô tả ngắn gọn phục vụ nhật ký và thông báo. */
  private String chiTiet;

  /** Mã SQLState của lỗi gần nhất, {@code null} khi kiểm tra thành công. */
  private String sqlState;

  /** Thời điểm bắt đầu kiểm tra; không được {@code null}. */
  private Instant thoiDiem;

  /** Tạo báo cáo rỗng với các tập dữ liệu rỗng bất biến. */
  public HealthReport() {
    this.trangThai = null;
    this.soDong = Map.of();
    this.baseline = Map.of();
    this.tyLeGiam = Map.of();
    this.bangThieu = List.of();
    this.thoiDiem = null;
  }

  /**
   * Tạo báo cáo đầy đủ và chuẩn hóa các tập dữ liệu.
   *
   * @param trangThai trạng thái tổng hợp; không được {@code null}
   * @param soDong số dòng hiện tại theo tên bảng
   * @param baseline số dòng baseline theo tên bảng
   * @param tyLeGiam tỷ lệ suy giảm theo tên bảng, trong khoảng {@code (0, 1]}
   * @param bangThieu các bảng cốt lõi không tồn tại trong schema {@code public}
   * @param chiTiet mô tả ngắn gọn phục vụ nhật ký và thông báo
   * @param sqlState mã SQLState của lỗi gần nhất, {@code null} khi kiểm tra thành công
   * @param thoiDiem thời điểm bắt đầu kiểm tra; không được {@code null}
   * @throws NullPointerException nếu {@code trangThai} hoặc {@code thoiDiem} là {@code null}
   */
  public HealthReport(
      HealthStatus trangThai,
      Map<String, Long> soDong,
      Map<String, Long> baseline,
      Map<String, Double> tyLeGiam,
      List<String> bangThieu,
      String chiTiet,
      String sqlState,
      Instant thoiDiem) {
    Objects.requireNonNull(trangThai, "trangThai");
    Objects.requireNonNull(thoiDiem, "thoiDiem");
    this.trangThai = trangThai;
    this.soDong = chuanHoaMap(soDong);
    this.baseline = chuanHoaMap(baseline);
    this.tyLeGiam = chuanHoaMap(tyLeGiam);
    this.bangThieu = chuanHoaList(bangThieu);
    this.chiTiet = chiTiet;
    this.sqlState = sqlState;
    this.thoiDiem = thoiDiem;
  }

  /**
   * Sao chép phòng thủ một map thành map bất biến.
   *
   * @return map bất biến, rỗng khi {@code giaTri} là {@code null}
   */
  private static <V> Map<String, V> chuanHoaMap(Map<String, V> giaTri) {
    return giaTri == null
        ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(giaTri));
  }

  /**
   * Sao chép phòng thủ một danh sách thành danh sách bất biến.
   *
   * @return danh sách bất biến, rỗng khi {@code giaTri} là {@code null}
   */
  private static List<String> chuanHoaList(List<String> giaTri) {
    return giaTri == null ? List.of() : List.copyOf(giaTri);
  }

  /** @return trạng thái tổng hợp */
  public HealthStatus trangThai() {
    return trangThai;
  }

  /** @return trạng thái tổng hợp */
  public HealthStatus getTrangThai() {
    return trangThai;
  }

  /**
   * Đặt trạng thái tổng hợp.
   *
   * @throws NullPointerException nếu {@code trangThai} là {@code null}
   */
  public void setTrangThai(HealthStatus trangThai) {
    this.trangThai = RangBuoc.batBuoc(trangThai, "trangThai");
  }

  /** @return số dòng hiện tại theo tên bảng, luôn bất biến */
  public Map<String, Long> soDong() {
    return soDong;
  }

  /** @return số dòng hiện tại theo tên bảng, luôn bất biến */
  public Map<String, Long> getSoDong() {
    return soDong;
  }

  /** Đặt số dòng hiện tại theo tên bảng; luôn sao chép phòng thủ. */
  public void setSoDong(Map<String, Long> soDong) {
    this.soDong = chuanHoaMap(soDong);
  }

  /** @return số dòng baseline theo tên bảng, luôn bất biến */
  public Map<String, Long> baseline() {
    return baseline;
  }

  /** @return số dòng baseline theo tên bảng, luôn bất biến */
  public Map<String, Long> getBaseline() {
    return baseline;
  }

  /** Đặt số dòng baseline theo tên bảng; luôn sao chép phòng thủ. */
  public void setBaseline(Map<String, Long> baseline) {
    this.baseline = chuanHoaMap(baseline);
  }

  /** @return tỷ lệ suy giảm theo tên bảng, luôn bất biến */
  public Map<String, Double> tyLeGiam() {
    return tyLeGiam;
  }

  /** @return tỷ lệ suy giảm theo tên bảng, luôn bất biến */
  public Map<String, Double> getTyLeGiam() {
    return tyLeGiam;
  }

  /**
   * Đặt tỷ lệ suy giảm theo tên bảng; luôn sao chép phòng thủ.
   *
   * @throws IllegalArgumentException nếu có giá trị ngoài khoảng {@code (0, 1]}
   */
  public void setTyLeGiam(Map<String, Double> tyLeGiam) {
    Map<String, Double> daChuanHoa = chuanHoaMap(tyLeGiam);
    for (Double giaTri : daChuanHoa.values()) {
      if (giaTri == null || giaTri <= 0 || giaTri > 1) {
        throw new IllegalArgumentException("tyLeGiam phải trong khoảng (0,1]");
      }
    }
    this.tyLeGiam = daChuanHoa;
  }

  /** @return các bảng cốt lõi còn thiếu, luôn bất biến */
  public List<String> bangThieu() {
    return bangThieu;
  }

  /** @return các bảng cốt lõi còn thiếu, luôn bất biến */
  public List<String> getBangThieu() {
    return bangThieu;
  }

  /** Đặt danh sách bảng cốt lõi còn thiếu; luôn sao chép phòng thủ. */
  public void setBangThieu(List<String> bangThieu) {
    this.bangThieu = chuanHoaList(bangThieu);
  }

  /** @return mô tả ngắn gọn phục vụ nhật ký và thông báo */
  public String chiTiet() {
    return chiTiet;
  }

  /** @return mô tả ngắn gọn phục vụ nhật ký và thông báo */
  public String getChiTiet() {
    return chiTiet;
  }

  /** Đặt mô tả ngắn gọn phục vụ nhật ký và thông báo. */
  public void setChiTiet(String chiTiet) {
    this.chiTiet = RangBuoc.chuoiTuyChon(chiTiet);
  }

  /** @return mã SQLState của lỗi gần nhất, hoặc {@code null} */
  public String sqlState() {
    return sqlState;
  }

  /** @return mã SQLState của lỗi gần nhất, hoặc {@code null} */
  public String getSqlState() {
    return sqlState;
  }

  /** Đặt mã SQLState của lỗi gần nhất. */
  public void setSqlState(String sqlState) {
    this.sqlState = RangBuoc.chuoiTuyChon(sqlState);
  }

  /** @return thời điểm bắt đầu kiểm tra */
  public Instant thoiDiem() {
    return thoiDiem;
  }

  /** @return thời điểm bắt đầu kiểm tra */
  public Instant getThoiDiem() {
    return thoiDiem;
  }

  /**
   * Đặt thời điểm bắt đầu kiểm tra.
   *
   * @throws NullPointerException nếu {@code thoiDiem} là {@code null}
   */
  public void setThoiDiem(Instant thoiDiem) {
    this.thoiDiem = RangBuoc.batBuoc(thoiDiem, "thoiDiem");
  }

  /**
   * Cho biết báo cáo có dấu hiệu bất thường cần can thiệp hay không.
   *
   * @return {@code true} khi trạng thái là {@link HealthStatus#SUSPICIOUS} hoặc
   *     {@link HealthStatus#CRITICAL}
   */
  public boolean coBatThuong() {
    return trangThai == HealthStatus.SUSPICIOUS || trangThai == HealthStatus.CRITICAL;
  }

  /**
   * Cho biết báo cáo có dấu hiệu bất thường cần can thiệp hay không.
   *
   * @return {@code true} khi trạng thái là {@link HealthStatus#SUSPICIOUS} hoặc
   *     {@link HealthStatus#CRITICAL}
   */
  public boolean isCoBatThuong() {
    return coBatThuong();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HealthReport khac)) {
      return false;
    }
    return trangThai == khac.trangThai
        && Objects.equals(soDong, khac.soDong)
        && Objects.equals(baseline, khac.baseline)
        && Objects.equals(tyLeGiam, khac.tyLeGiam)
        && Objects.equals(bangThieu, khac.bangThieu)
        && Objects.equals(chiTiet, khac.chiTiet)
        && Objects.equals(sqlState, khac.sqlState)
        && Objects.equals(thoiDiem, khac.thoiDiem);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        trangThai, soDong, baseline, tyLeGiam, bangThieu, chiTiet, sqlState, thoiDiem);
  }

  @Override
  public String toString() {
    return "HealthReport[trangThai=" + trangThai
        + ", soDong=" + soDong
        + ", baseline=" + baseline
        + ", tyLeGiam=" + tyLeGiam
        + ", bangThieu=" + bangThieu
        + ", chiTiet=" + chiTiet
        + ", sqlState=" + sqlState
        + ", thoiDiem=" + thoiDiem
        + "]";
  }
}
