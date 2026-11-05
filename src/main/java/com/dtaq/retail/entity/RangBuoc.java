package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ràng buộc dữ liệu dùng chung cho các setter của entity.
 *
 * <p>Lớp chỉ chứa các kiểm tra thuần (không truy vấn cơ sở dữ liệu, không phụ thuộc phiên làm việc):
 * bắt buộc có giá trị, chuỗi không rỗng sau khi cắt khoảng trắng, số tiền không âm, tỷ lệ phần trăm
 * trong khoảng {@code 0..100} và thứ tự ngày trong cùng một đối tượng.
 *
 * <p>Các quy tắc cần ngữ cảnh bên ngoài (tồn tại khóa ngoại, duy nhất, phân quyền, làm tròn tiền tệ)
 * <b>không</b> thuộc lớp này; chúng vẫn do DAO, cơ sở dữ liệu, controller và {@code TienUtil} đảm
 * nhiệm.
 */
final class RangBuoc {

  /** Số điện thoại được hệ thống chấp nhận: bắt đầu bằng 0, tổng 10 hoặc 11 chữ số. */
  static final String MAU_SO_DIEN_THOAI = "0\\d{9,10}";

  /** Định dạng email tối thiểu đang dùng trong controller. */
  static final String MAU_EMAIL = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";

  /** Mã băm SHA-256 dạng hex thường, 64 ký tự. */
  static final String MAU_SHA256 = "[0-9a-f]{64}";

  private RangBuoc() {}

  /**
   * Bắt buộc có giá trị.
   *
   * @throws NullPointerException nếu {@code giaTri} là {@code null}
   */
  static <T> T batBuoc(T giaTri, String tenTruong) {
    if (giaTri == null) {
      throw new NullPointerException(tenTruong);
    }
    return giaTri;
  }

  /**
   * Chuỗi bắt buộc: cắt khoảng trắng hai đầu và từ chối chuỗi rỗng.
   *
   * @return giá trị đã được cắt khoảng trắng
   * @throws NullPointerException nếu {@code giaTri} là {@code null}
   * @throws IllegalArgumentException nếu sau khi cắt chỉ còn khoảng trắng
   */
  static String chuoiBatBuoc(String giaTri, String tenTruong) {
    batBuoc(giaTri, tenTruong);
    String daCat = giaTri.trim();
    if (daCat.isEmpty()) {
      throw new IllegalArgumentException(tenTruong + " không được để trống");
    }
    return daCat;
  }

  /**
   * Chuỗi tùy chọn: cắt khoảng trắng và chuyển chuỗi rỗng thành {@code null}.
   *
   * @return giá trị đã chuẩn hóa, hoặc {@code null}
   */
  static String chuoiTuyChon(String giaTri) {
    if (giaTri == null) {
      return null;
    }
    String daCat = giaTri.trim();
    return daCat.isEmpty() ? null : daCat;
  }

  /**
   * Chuỗi bắt buộc phải khớp một biểu thức chính quy.
   *
   * @throws IllegalArgumentException nếu không khớp {@code mau}
   */
  static String chuoiKhopMau(String giaTri, String mau, String tenTruong, String thongDiep) {
    String daCat = chuoiBatBuoc(giaTri, tenTruong);
    if (!daCat.matches(mau)) {
      throw new IllegalArgumentException(thongDiep);
    }
    return daCat;
  }

  /**
   * Chuỗi tùy chọn nhưng nếu có giá trị thì phải khớp biểu thức chính quy.
   *
   * @return giá trị đã chuẩn hóa, hoặc {@code null}
   */
  static String chuoiTuyChonKhopMau(
      String giaTri, String mau, String tenTruong, String thongDiep) {
    String daCat = chuoiTuyChon(giaTri);
    if (daCat == null) {
      return null;
    }
    if (!daCat.matches(mau)) {
      throw new IllegalArgumentException(thongDiep);
    }
    return daCat;
  }

  /**
   * Số tiền bắt buộc và không âm.
   *
   * <p>Không làm tròn tại đây: theo C-11, việc làm tròn chỉ diễn ra một lần trong {@code TienUtil}.
   *
   * @throws NullPointerException nếu {@code giaTri} là {@code null}
   * @throws IllegalArgumentException nếu {@code giaTri} âm
   */
  static BigDecimal tienKhongAm(BigDecimal giaTri, String tenTruong) {
    batBuoc(giaTri, tenTruong);
    if (giaTri.signum() < 0) {
      throw new IllegalArgumentException(tenTruong + " không được âm");
    }
    return giaTri;
  }

  /** Như {@link #tienKhongAm(BigDecimal, String)} nhưng cho phép {@code null}. */
  static BigDecimal tienTuyChonKhongAm(BigDecimal giaTri, String tenTruong) {
    if (giaTri == null) {
      return null;
    }
    return tienKhongAm(giaTri, tenTruong);
  }

  /**
   * Tỷ lệ phần trăm bắt buộc trong khoảng {@code 0..100}.
   *
   * @throws NullPointerException nếu {@code giaTri} là {@code null}
   * @throws IllegalArgumentException nếu ngoài khoảng {@code 0..100}
   */
  static BigDecimal phanTram(BigDecimal giaTri, String tenTruong) {
    batBuoc(giaTri, tenTruong);
    if (giaTri.signum() < 0 || giaTri.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException(tenTruong + " phải nằm trong khoảng 0..100");
    }
    return giaTri;
  }

  /** Như {@link #phanTram(BigDecimal, String)} nhưng cho phép {@code null}. */
  static BigDecimal phanTramTuyChon(BigDecimal giaTri, String tenTruong) {
    if (giaTri == null) {
      return null;
    }
    return phanTram(giaTri, tenTruong);
  }

  /**
   * Số đếm không âm.
   *
   * @throws IllegalArgumentException nếu {@code giaTri} âm
   */
  static int khongAm(int giaTri, String tenTruong) {
    if (giaTri < 0) {
      throw new IllegalArgumentException(tenTruong + " không được âm");
    }
    return giaTri;
  }

  /**
   * Bảo đảm {@code sau} không trước {@code truoc}; bỏ qua khi một trong hai là {@code null}.
   *
   * @throws IllegalArgumentException nếu {@code sau} trước {@code truoc}
   */
  static void khongTruoc(LocalDate truoc, LocalDate sau, String thongDiep) {
    if (truoc != null && sau != null && sau.isBefore(truoc)) {
      throw new IllegalArgumentException(thongDiep);
    }
  }
}
