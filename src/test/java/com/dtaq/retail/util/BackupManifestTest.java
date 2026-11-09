package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.entity.BackupRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử ghi/đọc metadata và tính SHA-256 của {@link BackupManifest}. */
class BackupManifestTest {

  @TempDir
  Path tempDir;

  @Test
  void roundTripPreservesAllFields() throws IOException {
    Path dump = tempDir.resolve("dtaq_2026-11-05_000000.dump");
    Files.writeString(dump, "nội dung bản sao lưu");
    String sha = BackupManifest.sha256(dump);
    Instant createdAt = Instant.parse("2026-11-05T00:00:00Z");
    String thongBao = "Ghi chú có \"nháy\", dấu \\ và\nxuống dòng\ttab";
    BackupRecord goc = new BackupRecord(7L, dump, Files.size(dump), sha,
        BackupRecord.LoaiSaoLuu.MANUAL, BackupRecord.TrangThaiSaoLuu.SUCCESS, thongBao,
        createdAt, "quanly01", "pg_dump (PostgreSQL) 16.13", "dtaq_retail");

    BackupManifest.write(dump, goc);

    Path meta = BackupManifest.metaPathFor(dump);
    assertTrue(Files.exists(meta));
    String json = Files.readString(meta);
    assertTrue(json.contains("\"filePath\": \"" + dump.getFileName() + "\""));
    assertFalse(json.contains(tempDir.toString()), "Metadata chỉ được lưu tên tệp");

    BackupRecord doc = BackupManifest.read(meta);
    assertNull(doc.id());
    assertEquals(dump.toAbsolutePath(), doc.filePath().toAbsolutePath());
    assertEquals(goc.fileSize(), doc.fileSize());
    assertEquals(sha, doc.sha256());
    assertEquals(BackupRecord.LoaiSaoLuu.MANUAL, doc.loai());
    assertEquals(BackupRecord.TrangThaiSaoLuu.SUCCESS, doc.trangThai());
    assertEquals(thongBao, doc.thongBao());
    assertEquals(createdAt, doc.createdAt());
    assertEquals("quanly01", doc.createdBy());
    assertEquals("pg_dump (PostgreSQL) 16.13", doc.phienBanPg());
    assertEquals("dtaq_retail", doc.tenCsdl());
  }

  @Test
  void roundTripKeepsNullOptionalFields() throws IOException {
    Path dump = tempDir.resolve("dtaq_that_bai.dump");
    Files.writeString(dump, "");
    BackupRecord goc = new BackupRecord(null, dump, 0L, null,
        BackupRecord.LoaiSaoLuu.AUTO, BackupRecord.TrangThaiSaoLuu.FAILED, null,
        Instant.parse("2026-11-05T01:02:03Z"), null, null, null);

    BackupManifest.write(dump, goc);
    BackupRecord doc = BackupManifest.read(BackupManifest.metaPathFor(dump));

    assertNull(doc.sha256());
    assertNull(doc.thongBao());
    assertNull(doc.createdBy());
    assertNull(doc.phienBanPg());
    assertNull(doc.tenCsdl());
    assertEquals(BackupRecord.TrangThaiSaoLuu.FAILED, doc.trangThai());
  }

  @Test
  void sha256OfAbcMatchesKnownDigest() throws IOException {
    Path file = tempDir.resolve("abc.txt");
    Files.writeString(file, "abc");
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        BackupManifest.sha256(file));
  }

  @Test
  void malformedJsonThrowsIOException() throws IOException {
    Path khongPhaiJson = tempDir.resolve("khong-phai.meta.json");
    Files.writeString(khongPhaiJson, "không phải JSON");
    IOException loi = assertThrows(IOException.class, () -> BackupManifest.read(khongPhaiJson));
    assertTrue(loi.getMessage().startsWith("Metadata không hợp lệ: "));

    Path thieuKhoa = tempDir.resolve("thieu-khoa.meta.json");
    Files.writeString(thieuKhoa, "{\"filePath\": \"a.dump\"}");
    assertThrows(IOException.class, () -> BackupManifest.read(thieuKhoa));
  }

  @Test
  void invalidEnumAndInstantThrowIOException() throws IOException {
    Path saiLoai = tempDir.resolve("sai-loai.meta.json");
    Files.writeString(saiLoai, metadata("KHONG_CO"));
    assertThrows(IOException.class, () -> BackupManifest.read(saiLoai));

    Path saiThoiDiem = tempDir.resolve("sai-thoi-diem.meta.json");
    Files.writeString(saiThoiDiem, metadata("MANUAL").replace(
        "2026-11-05T00:00:00Z", "hôm qua"));
    assertThrows(IOException.class, () -> BackupManifest.read(saiThoiDiem));
  }

  @Test
  void metaAndDumpPathsRoundTrip() {
    Path dump = Path.of("dtaq_2026-11-05_000000.dump");
    Path meta = BackupManifest.metaPathFor(dump);
    assertEquals(Path.of("dtaq_2026-11-05_000000.meta.json"), meta);
    assertEquals(dump, BackupManifest.dumpPathFor(meta));
    assertEquals(Path.of("khac.meta.json"), BackupManifest.metaPathFor(Path.of("khac")));
    assertEquals(Path.of("khac.txt.dump"), BackupManifest.dumpPathFor(Path.of("khac.txt")));
  }

  private static String metadata(String loai) {
    return String.join("\n",
        "{",
        "  \"filePath\": \"a.dump\",",
        "  \"fileSize\": 12,",
        "  \"sha256\": null,",
        "  \"loai\": \"" + loai + "\",",
        "  \"trangThai\": \"SUCCESS\",",
        "  \"thongBao\": null,",
        "  \"createdAt\": \"2026-11-05T00:00:00Z\",",
        "  \"createdBy\": null,",
        "  \"phienBanPg\": null,",
        "  \"tenCsdl\": null",
        "}",
        "");
  }
}
