package com.dtaq.retail.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;

/** Sinh và đối chiếu mã OTP dùng cho luồng đặt lại mật khẩu. */
public final class OtpCodes {

  /** Thời gian hiệu lực của một mã OTP. */
  public static final Duration THOI_HAN = Duration.ofMinutes(5);

  /** Số lần nhập sai tối đa trước khi mã bị hủy. */
  public static final int SO_LAN_THU_TOI_DA = 5;

  /** Khoảng thời gian tối thiểu giữa hai lần gửi mã. */
  public static final Duration THOI_GIAN_CHO_GUI_LAI = Duration.ofSeconds(60);

  private static final int SO_CHU_SO = 6;
  private static final int GIOI_HAN = 1_000_000;
  private static final SecureRandom RANDOM = new SecureRandom();

  private OtpCodes() {
  }

  /**
   * Sinh mã OTP gồm đúng 6 chữ số, có đệm số 0 ở đầu khi cần.
   *
   * @return mã OTP 6 chữ số
   */
  public static String sinhMa() {
    String digits = Integer.toString(RANDOM.nextInt(GIOI_HAN));
    return "0".repeat(SO_CHU_SO - digits.length()) + digits;
  }

  /**
   * Băm mã OTP bằng SHA-256 và trả về chuỗi hex chữ thường.
   *
   * @param ma mã OTP cần băm
   * @return chuỗi hex 64 ký tự
   */
  public static String bam(String ma) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(String.valueOf(ma).getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Không hỗ trợ thuật toán SHA-256", e);
    }
  }

  /**
   * So sánh mã người dùng nhập với mã đã băm theo cách không rò rỉ thời gian.
   *
   * @param ma mã OTP người dùng nhập
   * @param maBam mã OTP đã băm được lưu trữ
   * @return {@code true} khi khớp; {@code false} khi một trong hai rỗng
   */
  public static boolean khop(String ma, String maBam) {
    if (ma == null || ma.isBlank() || maBam == null || maBam.isBlank()) {
      return false;
    }
    byte[] maDaBam = bam(ma).getBytes(StandardCharsets.UTF_8);
    byte[] maLuuTru = maBam.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(maDaBam, maLuuTru);
  }
}
