package com.dtaq.retail.controller;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá và kiểm tra dữ liệu hồ sơ người (khách hàng UC007, nhân viên UC013) cùng so khớp từ
 * khoá tìm kiếm không phân biệt dấu tiếng Việt. Không truy cập dữ liệu.
 */
public final class KiemTraHoSo {

  /** Di động 10 số (03/05/07/08/09) hoặc cố định 11 số (02x). */
  private static final Pattern SDT_HOP_LE = Pattern.compile("0[35789]\\d{8}|02\\d{9}");
  /** Các từ chỉ gồm chữ cái (mọi dấu tiếng Việt), nối bằng một khoảng trắng, dấu nháy, chấm hoặc gạch. */
  private static final Pattern HO_TEN_HOP_LE = Pattern.compile("\\p{L}+(?:(?:[ '\\-]|\\. ?)\\p{L}+)*\\.?");
  private static final Pattern EMAIL_HOP_LE =
      Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(?:\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}");
  private static final Pattern DAU = Pattern.compile("\\p{M}+");

  public static final int DO_DAI_HO_TEN_TOI_DA = 100;
  public static final int DO_DAI_DIA_CHI_TOI_DA = 255;

  private KiemTraHoSo() {
  }

  // ---------------------------------------------------------------- tìm kiếm

  /** Chữ thường, bỏ dấu tiếng Việt (kể cả đ), gộp khoảng trắng. {@code null} thành chuỗi rỗng. */
  public static String boDau(String text) {
    if (text == null) {
      return "";
    }
    String khongDau = DAU.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    return khongDau.replace('đ', 'd').replace('Đ', 'D')
        .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
  }

  /**
   * Từ khoá khớp một trong các trường: so khớp không dấu với chữ, và theo chữ số với số điện thoại
   * (gõ "0902 100" hay "100 001" đều ra). Từ khoá rỗng khớp tất cả.
   */
  public static boolean khop(String tuKhoa, String soDienThoai, String... truongChu) {
    String k = boDau(tuKhoa);
    if (k.isEmpty()) {
      return true;
    }
    for (String truong : truongChu) {
      if (boDau(truong).contains(k)) {
        return true;
      }
    }
    // Chỉ tìm theo số khi từ khoá toàn chữ số (từ 3 số), tránh "An 1" khớp mọi số có chữ số 1.
    String so = chuanHoaSdt(tuKhoa);
    return soDienThoai != null && so.matches("\\d{3,}") && soDienThoai.contains(so);
  }

  // ---------------------------------------------------------------- số điện thoại

  /** Bỏ khoảng trắng, chấm, gạch, ngoặc; đổi tiền tố +84/84 thành 0. */
  public static String chuanHoaSdt(String soDienThoai) {
    if (soDienThoai == null) {
      return "";
    }
    String so = soDienThoai.replaceAll("[\\s.\\-()]", "");
    if (so.startsWith("+84")) {
      so = "0" + so.substring(3);
    } else if (so.startsWith("84") && so.length() == 11) {
      so = "0" + so.substring(2);
    }
    return so;
  }

  /** Thông báo lỗi tiếng Việt, hoặc {@code null} nếu hợp lệ. */
  public static String kiemTraSdt(String soDienThoai) {
    String sdt = chuanHoaSdt(soDienThoai);
    if (sdt.isEmpty()) {
      return "Số điện thoại không được để trống";
    }
    if (!sdt.matches("\\d+")) {
      return "Số điện thoại chỉ gồm chữ số";
    }
    if (SDT_HOP_LE.matcher(sdt).matches()) {
      return null;
    }
    return sdt.startsWith("02")
        ? "Số cố định phải gồm 11 chữ số, bắt đầu bằng 02"
        : "Số di động phải gồm 10 chữ số, đầu số 03, 05, 07, 08 hoặc 09";
  }

  // ---------------------------------------------------------------- họ tên

  /** Gộp khoảng trắng và viết hoa chữ đầu mỗi từ: " nguyễn  văn an" thành "Nguyễn Văn An". */
  public static String chuanHoaHoTen(String hoTen) {
    if (hoTen == null || hoTen.isBlank()) {
      return "";
    }
    StringBuilder ketQua = new StringBuilder();
    for (String tu : hoTen.trim().split("\\s+")) {
      if (!ketQua.isEmpty()) {
        ketQua.append(' ');
      }
      ketQua.append(tu.substring(0, 1).toUpperCase(Locale.ROOT)).append(tu.substring(1).toLowerCase(Locale.ROOT));
    }
    return ketQua.toString();
  }

  /** Thông báo lỗi tiếng Việt, hoặc {@code null} nếu hợp lệ. */
  public static String kiemTraHoTen(String hoTen) {
    String ten = chuanHoaHoTen(hoTen);
    if (ten.isEmpty()) {
      return "Họ tên không được để trống";
    }
    if (ten.codePoints().anyMatch(Character::isDigit)) {
      return "Họ tên không được chứa chữ số";
    }
    if (!HO_TEN_HOP_LE.matcher(ten).matches()) {
      return "Họ tên chỉ gồm chữ cái và khoảng trắng";
    }
    if (ten.length() < 2) {
      return "Họ tên quá ngắn";
    }
    return ten.length() > DO_DAI_HO_TEN_TOI_DA
        ? "Họ tên tối đa " + DO_DAI_HO_TEN_TOI_DA + " ký tự" : null;
  }

  // ---------------------------------------------------------------- khác

  /** Email để trống là hợp lệ (không bắt buộc). */
  public static String kiemTraEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return EMAIL_HOP_LE.matcher(email.trim()).matches() ? null : "Email không đúng định dạng (ví dụ: ten@congty.vn)";
  }

  /** Địa chỉ để trống là hợp lệ (không bắt buộc). */
  public static String kiemTraDiaChi(String diaChi) {
    return diaChi != null && diaChi.trim().length() > DO_DAI_DIA_CHI_TOI_DA
        ? "Địa chỉ tối đa " + DO_DAI_DIA_CHI_TOI_DA + " ký tự" : null;
  }
}
