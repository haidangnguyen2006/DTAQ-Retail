package com.dtaq.retail.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** BCrypt based password hashing helper. */
public final class PasswordHasher {

  private static final int COST = 10;

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
    return BCrypt.verifyer().verify(rawPassword, encodedHash).verified;
  }
}
