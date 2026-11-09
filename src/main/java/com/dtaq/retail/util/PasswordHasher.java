package com.dtaq.retail.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/** BCrypt based password hashing helper. */
public final class PasswordHasher {

  private static final int COST = 10;

  /** BCrypt chỉ nhận tối đa 72 byte UTF-8; thư viện ném lỗi với mật khẩu dài hơn. */
  public static final int DO_DAI_TOI_DA_BYTE = 72;

  private PasswordHasher() {
  }

  /** Hashes a raw password with BCrypt cost 10 and returns the encoded hash. */
  public static String hash(char[] rawPassword) {
    if (rawPassword == null) {
      throw new IllegalArgumentException("Mật khẩu không được để trống");
    }
    return BCrypt.withDefaults().hashToString(COST, rawPassword);
  }

  /** Verifies a raw password against an encoded BCrypt hash; returns false for null or blank input. */
  public static boolean verify(char[] rawPassword, String encodedHash) {
    if (rawPassword == null || encodedHash == null || encodedHash.isBlank()) {
      return false;
    }
    if (quaDai(rawPassword)) {
      return false; // không hash nào lưu được từ mật khẩu này, nên chắc chắn sai
    }
    return BCrypt.verifyer().verify(rawPassword, encodedHash).verified;
  }

  /** Returns true when the password exceeds BCrypt's {@value #DO_DAI_TOI_DA_BYTE}-byte limit. */
  public static boolean quaDai(char[] rawPassword) {
    return rawPassword != null
        && StandardCharsets.UTF_8.encode(CharBuffer.wrap(rawPassword)).remaining() > DO_DAI_TOI_DA_BYTE;
  }
}
