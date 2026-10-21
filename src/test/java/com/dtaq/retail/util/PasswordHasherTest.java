package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for {@link PasswordHasher}. */
class PasswordHasherTest {

  @Test
  void hashThenVerifySucceeds() {
    String h = PasswordHasher.hash("12345678".toCharArray());
    assertTrue(PasswordHasher.verify("12345678".toCharArray(), h));
  }

  @Test
  void wrongPasswordFails() {
    String h = PasswordHasher.hash("12345678".toCharArray());
    assertFalse(PasswordHasher.verify("12345679".toCharArray(), h));
  }

  @Test
  void hashIsSalted() {
    String first = PasswordHasher.hash("12345678".toCharArray());
    String second = PasswordHasher.hash("12345678".toCharArray());
    assertNotEquals(first, second);
    assertTrue(PasswordHasher.verify("12345678".toCharArray(), first));
    assertTrue(PasswordHasher.verify("12345678".toCharArray(), second));
  }

  @Test
  void hashUsesCost10() {
    String h = PasswordHasher.hash("12345678".toCharArray());
    assertTrue(h.startsWith("$2a$10$"));
  }

  @Test
  void emptyPasswordStillHashes() {
    String h = PasswordHasher.hash(new char[0]);
    assertFalse(h.isBlank());
    assertTrue(PasswordHasher.verify(new char[0], h));
  }

  @Test
  void nullPasswordRejected() {
    assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
  }
}
