package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.util.BackupManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử quét, dọn và sao chép tệp sao lưu của {@link BackupStore}. */
class BackupStoreTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("danhSach trả bản mới nhất trước")
  void danhSachSapMoiNhatTruoc() throws IOException {
    BackupStore store = new BackupStore(tempDir, null);
    taoBanGhi(store, "cu", Instant.parse("2026-11-01T00:00:00Z"),
        TrangThaiSaoLuu.SUCCESS);
    taoBanGhi(store, "moi", Instant.parse("2026-11-04T00:00:00Z"),
        TrangThaiSaoLuu.SUCCESS);

    List<BackupRecord> danhSach = store.danhSach();

    assertEquals(2, danhSach.size());
    assertEquals("dtaq_moi.dump", danhSach.get(0).fileName());
    assertEquals("dtaq_cu.dump", danhSach.get(1).fileName());
  }

  @Test
  @DisplayName("Cùng thời điểm thì sắp tên tệp giảm dần")
  void danhSachCungThoiDiemSapTenGiamDan() throws IOException {
    BackupStore store = new BackupStore(tempDir, null);
    Instant thoiDiem = Instant.parse("2026-11-05T00:00:00Z");
    taoBanGhi(store, "a", thoiDiem, TrangThaiSaoLuu.SUCCESS);
    taoBanGhi(store, "b", thoiDiem, TrangThaiSaoLuu.SUCCESS);

    List<BackupRecord> danhSach = store.danhSach();

    assertEquals("dtaq_b.dump", danhSach.get(0).fileName());
    assertEquals("dtaq_a.dump", danhSach.get(1).fileName());
  }

  @Test
  @DisplayName("donDep xóa bản cũ hơn ngưỡng và giữ bản mới")
  void donDepXoaBanCuGiuBanMoi() throws IOException {
    BackupStore store = new BackupStore(tempDir, null);
    Instant now = Instant.parse("2026-11-05T00:00:00Z");
    BackupRecord cu = taoBanGhi(store, "cu", now.minusSeconds(8L * 24 * 3600),
        TrangThaiSaoLuu.SUCCESS);
    BackupRecord moi = taoBanGhi(store, "moi", now.minusSeconds(3600),
        TrangThaiSaoLuu.SUCCESS);

    store.donDep(7, now);

    assertFalse(Files.exists(cu.filePath()), "phải xóa tệp dump cũ");
    assertFalse(Files.exists(BackupManifest.metaPathFor(cu.filePath())),
        "phải xóa metadata cũ");
    assertTrue(Files.exists(moi.filePath()), "phải giữ bản mới");
    assertTrue(Files.exists(BackupManifest.metaPathFor(moi.filePath())),
        "phải giữ metadata bản mới");
  }

  @Test
  @DisplayName("donDep luôn giữ bản SUCCESS mới nhất dù đã quá hạn")
  void donDepGiuBanThanhCongMoiNhat() throws IOException {
    BackupStore store = new BackupStore(tempDir, null);
    Instant now = Instant.parse("2026-11-05T00:00:00Z");
    BackupRecord duyNhat = taoBanGhi(store, "duy-nhat", now.minusSeconds(30L * 24 * 3600),
        TrangThaiSaoLuu.SUCCESS);

    store.donDep(7, now);

    assertTrue(Files.exists(duyNhat.filePath()));
    assertTrue(Files.exists(BackupManifest.metaPathFor(duyNhat.filePath())));
  }

  @Test
  @DisplayName("Tệp dump không có metadata hiện ra với trạng thái FAILED")
  void dumpKhongMetaThanhFailed() throws IOException {
    BackupStore store = new BackupStore(tempDir, null);
    Path dump = tempDir.resolve("dtaq_mo_coi.dump");
    Files.writeString(dump, "noi-dung-khong-ro");
    Instant thoiDiem = Instant.parse("2026-11-04T10:00:00Z");
    Files.setLastModifiedTime(dump, FileTime.from(thoiDiem));

    List<BackupRecord> danhSach = store.danhSach();

    assertEquals(1, danhSach.size());
    BackupRecord banGhi = danhSach.get(0);
    assertEquals(TrangThaiSaoLuu.FAILED, banGhi.trangThai());
    assertEquals("Thiếu metadata", banGhi.thongBao());
    assertEquals(Files.size(dump), banGhi.fileSize());
    assertEquals(thoiDiem, banGhi.createdAt());
    assertNull(banGhi.sha256());
    assertEquals(dump.toAbsolutePath(), banGhi.filePath());
  }

  @Test
  @DisplayName("Tệp .tmp đang ghi dở bị bỏ qua")
  void boQuaTepTam() throws IOException {
    BackupStore store = new BackupStore(tempDir, null);
    taoBanGhi(store, "that", Instant.parse("2026-11-05T00:00:00Z"), TrangThaiSaoLuu.SUCCESS);
    Files.writeString(tempDir.resolve("dtaq_dang_ghi.dump.tmp"), "dang-ghi");
    Files.writeString(tempDir.resolve("dtaq_dang_ghi.meta.json.tmp"), "{}");

    List<BackupRecord> danhSach = store.danhSach();

    assertEquals(1, danhSach.size());
    assertEquals("dtaq_that.dump", danhSach.get(0).fileName());
  }

  @Test
  @DisplayName("saoChepPhu chép cả dump và metadata sang thư mục phụ")
  void saoChepPhuChepDuHaiTep() throws IOException {
    Path phu = tempDir.resolve("phu");
    BackupStore store = new BackupStore(tempDir.resolve("chinh"), phu);
    store.taoThuMuc();
    BackupRecord banGhi = taoBanGhi(store, "sao-chep", Instant.parse("2026-11-05T00:00:00Z"),
        TrangThaiSaoLuu.SUCCESS);

    store.saoChepPhu(banGhi);

    assertTrue(Files.exists(phu.resolve(banGhi.fileName())));
    assertTrue(Files.exists(phu.resolve(BackupManifest.metaPathFor(banGhi.filePath())
        .getFileName())));
  }

  private static BackupRecord taoBanGhi(BackupStore store, String ten, Instant createdAt,
      TrangThaiSaoLuu trangThai) throws IOException {
    store.taoThuMuc();
    Path dump = store.thuMuc().resolve("dtaq_" + ten + BackupManifest.DUMP_SUFFIX);
    Files.writeString(dump, "noi-dung-" + ten);
    BackupRecord banGhi = new BackupRecord(null, dump.toAbsolutePath(), Files.size(dump),
        BackupManifest.sha256(dump), LoaiSaoLuu.MANUAL, trangThai, "ghi chú", createdAt,
        "NV-1", "pg_dump (PostgreSQL) 16.13", "dtaq_retail");
    BackupManifest.write(dump, banGhi);
    return banGhi;
  }
}
