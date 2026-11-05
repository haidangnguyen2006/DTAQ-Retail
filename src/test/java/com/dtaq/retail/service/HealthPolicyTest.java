package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Kiểm thử các biên của ngưỡng suy giảm, số chu kỳ và giới hạn tự động phục hồi. */
class HealthPolicyTest {

  private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

  @Test
  void dropExactlyAtThresholdIsSuspicious() {
    assertTrue(HealthPolicy.nenSuspicious(50, 25, 0.5, 50));
  }

  @Test
  void dropJustBelowThresholdIsNotSuspicious() {
    assertFalse(HealthPolicy.nenSuspicious(50, 26, 0.5, 50));
  }

  @Test
  void baselineBelowMinimumIsNotSuspicious() {
    assertFalse(HealthPolicy.nenSuspicious(49, 0, 0.5, 50));
  }

  @Test
  void zeroCurrentWithSufficientBaselineIsSuspicious() {
    assertTrue(HealthPolicy.nenSuspicious(100, 0, 0.5, 50));
  }

  @Test
  void streakNeedsEnoughCycles() {
    assertFalse(HealthPolicy.duChuKy(1, 2));
    assertTrue(HealthPolicy.duChuKy(2, 2));
    assertTrue(HealthPolicy.duChuKy(3, 2));
  }

  @Test
  void attemptsWithinOneHourAreCounted() {
    List<Instant> attempts = List.of(
        NOW.minusSeconds(10),
        NOW.minusSeconds(30 * 60),
        NOW.minusSeconds(2 * 60 * 60));

    assertEquals(2, HealthPolicy.locTrongGio(attempts, NOW).size());
  }

  @Test
  void boundaryOfExactlyOneHourIsKept() {
    List<Instant> attempts = List.of(NOW.minusSeconds(3600));

    assertEquals(1, HealthPolicy.locTrongGio(attempts, NOW).size());
  }

  @Test
  void allOldAttemptsAreFiltered() {
    List<Instant> attempts = List.of(
        NOW.minusSeconds(3601),
        NOW.minusSeconds(24 * 60 * 60));

    assertTrue(HealthPolicy.locTrongGio(attempts, NOW).isEmpty());
  }

  @Test
  void autoRestoreAllowedBelowLimit() {
    List<Instant> attempts = List.of(
        NOW.minusSeconds(10),
        NOW.minusSeconds(30 * 60));

    assertTrue(HealthPolicy.duocAutoRestore(attempts, NOW, 3));
  }

  @Test
  void autoRestoreBlockedAtLimit() {
    List<Instant> attempts = List.of(
        NOW.minusSeconds(10),
        NOW.minusSeconds(20 * 60),
        NOW.minusSeconds(40 * 60));

    assertFalse(HealthPolicy.duocAutoRestore(attempts, NOW, 3));
  }

  @Test
  void oldAttemptsDoNotCountTowardsLimit() {
    List<Instant> attempts = List.of(
        NOW.minusSeconds(10),
        NOW.minusSeconds(20 * 60),
        NOW.minusSeconds(2 * 60 * 60),
        NOW.minusSeconds(3 * 60 * 60));

    assertTrue(HealthPolicy.duocAutoRestore(attempts, NOW, 3));
  }

  @Test
  void emptyAttemptsAllowAutoRestore() {
    assertTrue(HealthPolicy.duocAutoRestore(List.of(), NOW, 1));
    assertTrue(HealthPolicy.duocAutoRestore(null, NOW, 1));
  }
}
