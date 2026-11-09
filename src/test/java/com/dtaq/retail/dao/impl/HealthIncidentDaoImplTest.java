package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.DbMigration;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.entity.HealthIncident.Severity;
import com.dtaq.retail.entity.HealthIncident.TrangThai;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp {@link HealthIncidentDaoImpl} trên cơ sở dữ liệu phát triển.
 *
 * <p>Mỗi lần chạy chèn một sự cố với {@code detail} duy nhất (có UUID) nên bài kiểm thử lặp lại
 * được. Bản ghi kiểm thử được xóa lại trong {@code finally} để không để rác trong bảng.
 */
class HealthIncidentDaoImplTest {

  private static final String XOA_SU_CO = "DELETE FROM \"SuCoSucKhoe\" WHERE \"id\" = ?";

  private static HealthIncidentDaoImpl dao;
  private static DataSource dataSource;

  @BeforeAll
  static void requireDatabase() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DbMigration.migrate(ConnectDB.getDataSource());
    dataSource = ConnectDB.getDataSource();
    dao = new HealthIncidentDaoImpl(dataSource);
  }

  @Test
  @DisplayName("insert tạo PENDING, findMoiNhatDangCho và findById đọc lại đúng")
  void insertRoiTimLaiDuoc() {
    Instant detectedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    HealthIncident suCo = suCoMoi(Severity.CRITICAL, detectedAt);
    long id = dao.insert(suCo);
    try {
      assertTrue(id > 0, "id phải do cơ sở dữ liệu sinh");

      HealthIncident moiNhat = dao.findMoiNhatDangCho()
          .orElseThrow(() -> new AssertionError("phải có sự cố PENDING vừa chèn"));
      assertEquals(id, moiNhat.id().longValue(), "phải trả sự cố PENDING mới nhất");
      assertEquals(Severity.CRITICAL, moiNhat.severity());
      assertEquals(TrangThai.PENDING, moiNhat.trangThai());
      assertTrue(moiNhat.dangCho(), "sự cố PENDING phải đang chờ xử lý");
      assertNull(moiNhat.resolvedAt(), "sự cố PENDING chưa có thời điểm kết thúc");
      assertNull(moiNhat.resolvedBy(), "sự cố PENDING chưa có người xử lý");

      HealthIncident docLai = dao.findById(id)
          .orElseThrow(() -> new AssertionError("findById phải trả sự cố " + id));
      assertEquals(suCo.detail(), docLai.detail());
      assertEquals(detectedAt, docLai.detectedAt(), "detectedAt phải đọc lại khớp");
      assertNull(docLai.backupUsed(), "sự cố mới chưa dùng bản sao lưu nào");
    } finally {
      xoa(id);
    }
  }

  @Test
  @DisplayName("capNhatKetThuc đổi trạng thái, ghi resolvedAt/resolvedBy/backupUsed")
  void capNhatKetThucGhiNhanXuLy() {
    Instant detectedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    HealthIncident suCo = suCoMoi(Severity.SUSPICIOUS, detectedAt);
    long id = dao.insert(suCo);
    try {
      Instant resolvedAt = detectedAt.plusSeconds(60);
      dao.capNhatKetThuc(
          id, TrangThai.DISMISSED, resolvedAt, "quanly01", "backups/dtaq_test.dump");

      HealthIncident sau = dao.findById(id)
          .orElseThrow(() -> new AssertionError("findById phải trả sự cố " + id));
      assertEquals(TrangThai.DISMISSED, sau.trangThai());
      assertEquals(resolvedAt, sau.resolvedAt(), "resolvedAt phải được ghi lại");
      assertEquals("quanly01", sau.resolvedBy());
      assertEquals("backups/dtaq_test.dump", sau.backupUsed());
      assertFalse(sau.dangCho(), "sự cố đã kết thúc không còn chờ xử lý");

      assertTrue(dao.findAll().stream().anyMatch(item -> item.id().equals(id)),
          "findAll phải chứa sự cố vừa chèn");
      assertFalse(dao.findMoiNhatDangCho().map(item -> item.id().equals(id)).orElse(false),
          "sự cố đã DISMISSED không được coi là đang chờ");
    } finally {
      xoa(id);
    }
  }

  @Test
  @DisplayName("findById trả rỗng với khóa không tồn tại")
  void findByIdTraRongKhiKhongTonTai() {
    assertTrue(dao.findById(-1L).isEmpty(), "khóa -1 không được tồn tại");
  }

  @Test
  @DisplayName("Constructor Supplier lấy nguồn dữ liệu hiện tại cho mỗi thao tác")
  void daoTheoSupplierDocGhiDuoc() {
    HealthIncidentDaoImpl daoTheoSupplier = new HealthIncidentDaoImpl(ConnectDB::getDataSource);
    Instant detectedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    long id = daoTheoSupplier.insert(suCoMoi(Severity.CRITICAL, detectedAt));
    try {
      assertTrue(id > 0, "id phải do cơ sở dữ liệu sinh");
      assertTrue(daoTheoSupplier.findById(id).isPresent(),
          "phải đọc lại được sự cố vừa chèn qua nguồn dữ liệu hiện tại");
    } finally {
      xoa(id);
    }
  }

  private static HealthIncident suCoMoi(Severity severity, Instant detectedAt) {
    return new HealthIncident(
        null,
        severity,
        "Sự cố kiểm thử DAO " + UUID.randomUUID(),
        TrangThai.PENDING,
        null,
        detectedAt,
        null,
        null);
  }

  private static void xoa(long id) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(XOA_SU_CO)) {
      statement.setLong(1, id);
      statement.executeUpdate();
    } catch (Exception e) {
      System.err.println("Không dọn được sự cố kiểm thử " + id + ": " + e.getMessage());
    }
  }
}
