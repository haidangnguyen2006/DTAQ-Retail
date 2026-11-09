package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.service.HealthStateStore.HealthState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử đọc/ghi tệp trạng thái sức khỏe của {@link HealthStateStore}. */
class HealthStateStoreTest {

  @TempDir
  Path tempDir;

  @Test
  void roundTripPreservesAllFields() {
    Path file = tempDir.resolve("health_state.json");
    HealthStateStore store = new HealthStateStore(file);
    assertEquals(file, store.file());

    Map<String, Long> baseline = new LinkedHashMap<>();
    baseline.put("NhanVien", 100L);
    baseline.put("HoaDon", 0L);
    Map<String, Integer> streak = new LinkedHashMap<>();
    streak.put("HoaDon", 2);
    List<Instant> attempts = List.of(
        Instant.parse("2026-11-05T01:00:00Z"),
        Instant.parse("2026-11-05T02:00:00Z"));
    HealthState goc = new HealthState(baseline, streak, 12L,
        Instant.parse("2026-11-05T03:00:00Z"), attempts, "SUSPICIOUS",
        Instant.parse("2026-11-05T03:05:00Z"));

    store.ghi(goc);
    HealthState doc = store.doc();

    assertEquals(baseline, doc.baseline());
    assertEquals(streak, doc.suspiciousStreak());
    assertEquals(12L, doc.openIncidentId());
    assertEquals(Instant.parse("2026-11-05T03:00:00Z"), doc.lastNotifiedAt());
    assertEquals(attempts, doc.autoRestoreAttempts());
    assertEquals("SUSPICIOUS", doc.lastStatus());
    assertNotNull(doc.updatedAt(), "ghi phải đóng dấu updatedAt");
    assertFalse(doc.updatedAt().isBefore(Instant.now().minusSeconds(60)));
  }

  @Test
  void ghiWritesFlatJsonExactly() throws IOException {
    Path file = tempDir.resolve("health_state.json");
    HealthStateStore store = new HealthStateStore(file);
    Map<String, Long> baseline = new LinkedHashMap<>();
    baseline.put("NhanVien", 10L);
    Map<String, Integer> streak = new LinkedHashMap<>();
    streak.put("NhanVien", 1);
    HealthState state = new HealthState(baseline, streak, 7L,
        Instant.parse("2026-11-05T01:00:00Z"),
        List.of(Instant.parse("2026-11-05T02:00:00Z")), "SUSPICIOUS", null);

    store.ghi(state);

    String json = Files.readString(file, StandardCharsets.UTF_8);
    String dau = "{\"baseline\":{\"NhanVien\":10},\"suspiciousStreak\":{\"NhanVien\":1},"
        + "\"openIncidentId\":7,\"lastNotifiedAt\":\"2026-11-05T01:00:00Z\","
        + "\"autoRestoreAttempts\":[\"2026-11-05T02:00:00Z\"],"
        + "\"lastStatus\":\"SUSPICIOUS\",\"updatedAt\":\"";
    assertTrue(json.startsWith(dau), json);
    assertTrue(json.endsWith("\"}"), json);
    assertFalse(json.contains("\n"), "JSON phải phẳng trên một dòng");
    assertFalse(Files.exists(tempDir.resolve("health_state.json.tmp")),
        "Không được để lại tệp tạm");
  }

  @Test
  void missingFileReturnsEmptyState() {
    HealthStateStore store = new HealthStateStore(tempDir.resolve("khong_ton_tai.json"));

    assertEquals(HealthState.rong(), store.doc());
  }

  @Test
  void malformedJsonReturnsEmptyState() throws IOException {
    Path file = tempDir.resolve("health_state.json");
    HealthStateStore store = new HealthStateStore(file);

    Files.writeString(file, "khong phai json", StandardCharsets.UTF_8);
    assertEquals(HealthState.rong(), store.doc());

    Files.writeString(file, "{\"baseline\":[1,2]}", StandardCharsets.UTF_8);
    assertEquals(HealthState.rong(), store.doc());

    Files.writeString(file, "{\"baseline\":{},\"la\":1}", StandardCharsets.UTF_8);
    assertEquals(HealthState.rong(), store.doc());

    Files.writeString(file, "{\"baseline\":{", StandardCharsets.UTF_8);
    assertEquals(HealthState.rong(), store.doc());
  }

  @Test
  void nullAndEmptyCollectionsBecomeEmpty() throws IOException {
    Path file = tempDir.resolve("health_state.json");
    Files.writeString(file, "{\"baseline\":null,\"suspiciousStreak\":null,"
        + "\"openIncidentId\":null,\"lastNotifiedAt\":null,"
        + "\"autoRestoreAttempts\":null,\"lastStatus\":null,\"updatedAt\":null}",
        StandardCharsets.UTF_8);

    HealthState doc = new HealthStateStore(file).doc();

    assertEquals(Map.of(), doc.baseline());
    assertEquals(Map.of(), doc.suspiciousStreak());
    assertNull(doc.openIncidentId());
    assertNull(doc.lastNotifiedAt());
    assertEquals(List.of(), doc.autoRestoreAttempts());
    assertNull(doc.lastStatus());
    assertNull(doc.updatedAt());
  }

  @Test
  void recordConstructorNormalizesNullCollections() {
    HealthState state = new HealthState(null, null, null, null, null, null, null);

    assertEquals(Map.of(), state.baseline());
    assertEquals(Map.of(), state.suspiciousStreak());
    assertEquals(List.of(), state.autoRestoreAttempts());
  }

  @Test
  void withMethodsReturnNewImmutableStates() {
    Instant thoiDiem = Instant.parse("2026-11-05T03:00:00Z");
    HealthState goc = new HealthState(Map.of("NhanVien", 100L), Map.of("NhanVien", 1), 5L,
        thoiDiem, List.of(thoiDiem), "SUSPICIOUS", thoiDiem);

    HealthState moi = goc
        .withBaseline(Map.of("HoaDon", 20L))
        .withStreak(Map.of("HoaDon", 2))
        .withOpenIncident(9L)
        .withLastNotifiedAt(thoiDiem.plusSeconds(60))
        .withAutoRestoreAttempts(List.of(thoiDiem.plusSeconds(120)))
        .withLastStatus("CRITICAL");

    assertEquals(Map.of("NhanVien", 100L), goc.baseline());
    assertEquals(Map.of("NhanVien", 1), goc.suspiciousStreak());
    assertEquals(5L, goc.openIncidentId());
    assertEquals(thoiDiem, goc.lastNotifiedAt());
    assertEquals(List.of(thoiDiem), goc.autoRestoreAttempts());
    assertEquals("SUSPICIOUS", goc.lastStatus());
    assertEquals(thoiDiem, goc.updatedAt());

    assertEquals(Map.of("HoaDon", 20L), moi.baseline());
    assertEquals(Map.of("HoaDon", 2), moi.suspiciousStreak());
    assertEquals(9L, moi.openIncidentId());
    assertEquals(thoiDiem.plusSeconds(60), moi.lastNotifiedAt());
    assertEquals(List.of(thoiDiem.plusSeconds(120)), moi.autoRestoreAttempts());
    assertEquals("CRITICAL", moi.lastStatus());
    assertEquals(thoiDiem, moi.updatedAt());

    assertThrows(UnsupportedOperationException.class, () -> moi.baseline().put("Khac", 1L));
    assertThrows(UnsupportedOperationException.class, () -> moi.autoRestoreAttempts().add(thoiDiem));
  }

  @Test
  void ghiCreatesParentDirectories() {
    Path file = tempDir.resolve("con/thu-muc/health_state.json");
    HealthStateStore store = new HealthStateStore(file);

    store.ghi(HealthState.rong().withLastStatus("HEALTHY"));

    assertTrue(Files.isRegularFile(file));
    assertEquals("HEALTHY", store.doc().lastStatus());
  }
}
