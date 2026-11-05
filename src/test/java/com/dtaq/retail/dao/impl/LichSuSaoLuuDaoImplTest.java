package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.DbMigration;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp {@link LichSuSaoLuuDaoImpl} trên cơ sở dữ liệu phát triển.
 *
 * <p>Mỗi lần chạy ghi thêm một bản ghi với {@code filePath} duy nhất (có UUID) nên bài kiểm thử
 * lặp lại được mà không đụng dữ liệu cũ. Dữ liệu kiểm thử không được dọn lại.
 */
class LichSuSaoLuuDaoImplTest {

  private static final String SHA256_ABC =
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

  /** Thời điểm cố định cho bản ghi kiểm thử, cắt về mili giây để đọc lại khớp tuyệt đối. */
  private static final Instant THOI_DIEM = Instant.now().truncatedTo(ChronoUnit.MILLIS);

  private static LichSuSaoLuuDaoImpl dao;
  private static BackupRecord banGhi;

  @BeforeAll
  static void requireDatabase() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DbMigration.migrate(ConnectDB.getDataSource());
    dao = new LichSuSaoLuuDaoImpl(ConnectDB.getDataSource());
    banGhi = new BackupRecord(
        null,
        Path.of("backups", "dtaq_test_" + UUID.randomUUID() + ".dump"),
        1_048_576L,
        SHA256_ABC,
        LoaiSaoLuu.MANUAL,
        TrangThaiSaoLuu.SUCCESS,
        "Bản ghi kiểm thử DAO",
        THOI_DIEM,
        "TEST",
        "16.13",
        "dtaq_db");
    dao.insert(banGhi);
  }

  @Test
  @DisplayName("insert ghi đủ trường và findAll đọc lại đúng, mới nhất trước")
  void insertRoiFindAllDocLaiDuTruong() {
    List<BackupRecord> danhSach = dao.findAll();

    BackupRecord docLai = danhSach.stream()
        .filter(item -> banGhi.filePath().equals(item.filePath()))
        .findFirst()
        .orElseThrow(() -> new AssertionError(
            "findAll phải chứa bản ghi vừa chèn " + banGhi.filePath()));

    assertNotNull(docLai.id(), "id phải do cơ sở dữ liệu sinh");
    assertEquals(banGhi.fileName(), docLai.fileName());
    assertEquals(banGhi.fileSize(), docLai.fileSize());
    assertEquals(SHA256_ABC, docLai.sha256());
    assertEquals(LoaiSaoLuu.MANUAL, docLai.loai());
    assertEquals(TrangThaiSaoLuu.SUCCESS, docLai.trangThai());
    assertTrue(docLai.thanhCong(), "bản ghi SUCCESS phải được coi là thành công");
    assertEquals("Bản ghi kiểm thử DAO", docLai.thongBao());
    assertEquals(THOI_DIEM, docLai.createdAt());
    assertEquals("TEST", docLai.createdBy());
    assertEquals("16.13", docLai.phienBanPg());
    assertEquals("dtaq_db", docLai.tenCsdl());

    for (int i = 1; i < danhSach.size(); i++) {
      assertFalse(danhSach.get(i - 1).createdAt().isBefore(danhSach.get(i).createdAt()),
          "findAll phải sắp xếp theo createdAt giảm dần");
    }
  }

  @Test
  @DisplayName("existsByFilePath đúng với đường dẫn đã chèn và đường dẫn lạ")
  void existsByFilePathPhanBietTonTai() {
    assertTrue(dao.existsByFilePath(banGhi.filePath().toString()),
        "đường dẫn vừa chèn phải tồn tại");
    assertFalse(dao.existsByFilePath("backups/khong_ton_tai_" + UUID.randomUUID() + ".dump"),
        "đường dẫn lạ không được coi là tồn tại");
  }

  @Test
  @DisplayName("Constructor Supplier lấy nguồn dữ liệu hiện tại cho mỗi thao tác")
  void constructorSupplierHoatDong() {
    LichSuSaoLuuDaoImpl daoTheoSupplier = new LichSuSaoLuuDaoImpl(ConnectDB::getDataSource);

    assertFalse(daoTheoSupplier.existsByFilePath(
            "backups/khong_ton_tai_" + UUID.randomUUID() + ".dump"),
        "đường dẫn lạ không được coi là tồn tại qua nguồn dữ liệu theo Supplier");
    assertTrue(daoTheoSupplier.existsByFilePath(banGhi.filePath().toString()),
        "bản ghi đã chèn phải được đọc thấy qua nguồn dữ liệu theo Supplier");
  }

  @Test
  @DisplayName("findMoiNhatThanhCong trả bản SUCCESS mới nhất")
  void findMoiNhatThanhCongTraBanMoiNhat() {
    Optional<BackupRecord> moiNhat = dao.findMoiNhatThanhCong();

    assertTrue(moiNhat.isPresent(), "phải có ít nhất bản ghi SUCCESS vừa chèn");
    assertTrue(moiNhat.orElseThrow().thanhCong(), "kết quả phải là bản ghi SUCCESS");

    Instant createdAtLonNhat = dao.findAll().stream()
        .filter(BackupRecord::thanhCong)
        .map(BackupRecord::createdAt)
        .max(Instant::compareTo)
        .orElseThrow();
    assertEquals(createdAtLonNhat, moiNhat.orElseThrow().createdAt(),
        "phải trả bản SUCCESS có createdAt lớn nhất");
    assertFalse(moiNhat.orElseThrow().createdAt().isBefore(banGhi.createdAt()),
        "bản trả về không được cũ hơn bản ghi kiểm thử");
  }
}
