package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp {@link SanPhamLoiDaoImpl} trên cơ sở dữ liệu PostgreSQL.
 */
class SanPhamLoiDaoImplTest {

  private static final String MA_LOI_SEED = "SPL-2026-000001";
  private static final String SERIAL_SEED = "SAM-TV-0001";
  private static final String PHIEU_TRA_SEED = "PT-2026-000001";
  private static final String PHIEU_DOI_SEED = "PD-2026-000001";
  private static final String SERIAL_DOI_SEED = "PAN-AC-0013";

  private static final String MA_LOI_TEST = "SPL-TEST-999";
  private static final String SERIAL_TEST = "SAM-TV-0002"; // có trong seed V4

  private SanPhamLoiDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "CSDL không khả dụng");
    dao = new SanPhamLoiDaoImpl(ConnectDB.getDataSource());
    cleanUpTestData();
  }

  @AfterEach
  void tearDown() {
    if (ConnectDB.isAvailable()) {
      cleanUpTestData();
    }
  }

  private void cleanUpTestData() {
    try (Connection conn = ConnectDB.getDataSource().getConnection();
        var ps = conn.prepareStatement("DELETE FROM \"SanPhamLoi\" WHERE \"maLoi\" = ?")) {
      ps.setString(1, MA_LOI_TEST);
      ps.executeUpdate();
    } catch (Exception ignored) {
    }
  }

  @Test
  @DisplayName("findAll trả về danh sách sản phẩm lỗi từ seed V4")
  void findAllReturnsDefectiveProducts() {
    List<SanPhamLoi> list = dao.findAll();
    assertNotNull(list);
    assertFalse(list.isEmpty(), "Danh sách sản phẩm lỗi từ seed V4 không được rỗng");
    assertTrue(list.stream().anyMatch(spl -> MA_LOI_SEED.equals(spl.maLoi())),
        "Phải chứa hồ sơ lỗi SPL-2026-000001");
  }

  @Test
  @DisplayName("findById nạp đầy đủ thông tin máy, nhà cung cấp, dòng sản phẩm")
  void findByIdReturnsFullDetails() {
    Optional<SanPhamLoi> opt = dao.findById(MA_LOI_SEED);
    assertTrue(opt.isPresent(), "SPL-2026-000001 phải tồn tại");

    SanPhamLoi spl = opt.get();
    assertEquals(MA_LOI_SEED, spl.maLoi());
    assertNotNull(spl.sanPham(), "Phải nạp thông tin SanPham");
    assertEquals(SERIAL_SEED, spl.sanPham().soSerial());
    assertNotNull(spl.sanPham().dongSanPham(), "Phải nạp thông tin DongSanPham");
    assertNotNull(spl.sanPham().nhaCungCap(), "Phải nạp thông tin NhaCungCap");
    assertEquals(PHIEU_TRA_SEED, spl.phieuTra().maPhieuTra());
    assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, spl.tinhTrangXuLy());
  }

  @Test
  @DisplayName("findById trả về rỗng khi mã không tồn tại hoặc null")
  void findByIdReturnsEmptyWhenNotFound() {
    assertTrue(dao.findById("SPL-KHONG-CO").isEmpty());
    assertTrue(dao.findById(null).isEmpty());
    assertTrue(dao.findById("   ").isEmpty());
  }

  @Test
  @DisplayName("findDangXuLy tìm đúng máy có hồ sơ đang ở trạng thái CHO_XU_LY")
  void findDangXuLyReturnsPendingRecord() {
    Optional<SanPhamLoi> opt = dao.findDangXuLy(SERIAL_SEED);
    assertTrue(opt.isPresent(), "SAM-TV-0001 đang ở trạng thái CHO_XU_LY");
    assertEquals(MA_LOI_SEED, opt.get().maLoi());
    assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, opt.get().tinhTrangXuLy());
  }

  @Test
  @DisplayName("findByTinhTrang lọc chính xác theo từng trạng thái xử lý lỗi")
  void findByTinhTrangFiltersCorrectly() {
    List<SanPhamLoi> choXuLy = dao.findByTinhTrang(TrangThaiXuLyLoi.CHO_XU_LY);
    assertFalse(choXuLy.isEmpty());
    for (SanPhamLoi spl : choXuLy) {
      assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, spl.tinhTrangXuLy());
    }

    List<SanPhamLoi> thanhLy = dao.findByTinhTrang(TrangThaiXuLyLoi.THANH_LY);
    assertFalse(thanhLy.isEmpty());
    for (SanPhamLoi spl : thanhLy) {
      assertEquals(TrangThaiXuLyLoi.THANH_LY, spl.tinhTrangXuLy());
    }
  }

  @Test
  @DisplayName("insert thêm thành công sản phẩm lỗi nội bộ không kèm chứng từ đổi trả")
  void insertInternalDefectSuccess() {
    SanPham sp = new SanPham(SERIAL_TEST, null, null, null, null, null, 0, 0);
    SanPhamLoi spl = new SanPhamLoi(
        MA_LOI_TEST,
        sp,
        null, // không qua phiếu trả
        null, // không qua phiếu đổi
        "Lỗi phát hiện tại kho",
        "Màn hình bị sọc khi bóc hộp",
        LocalDate.of(2026, 3, 1),
        TrangThaiXuLyLoi.CHO_XU_LY,
        null);

    dao.insert(spl);

    Optional<SanPhamLoi> found = dao.findById(MA_LOI_TEST);
    assertTrue(found.isPresent());
    assertEquals(SERIAL_TEST, found.get().sanPham().soSerial());
    assertEquals("Màn hình bị sọc khi bóc hộp", found.get().moTaLoi());
    assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, found.get().tinhTrangXuLy());
  }

  @Test
  @DisplayName("insert ném lỗi khi liên kết chứng từ đổi trả không chứa serial máy đó (trigger)")
  void insertWithInvalidOriginThrowsException() {
    // SERIAL_TEST (SAM-TV-0002) nhưng gán nhầm vào PHIEU_DOI_SEED (PD-2026-000001 của PAN-AC-0013)
    SanPham sp = new SanPham(SERIAL_TEST, null, null, null, null, null, 0, 0);
    PhieuDoi pd = new PhieuDoi(PHIEU_DOI_SEED, null, null, null, null, null, null, null, null, null, null);
    SanPhamLoi spl = new SanPhamLoi(
        MA_LOI_TEST,
        sp,
        null,
        pd,
        null,
        "Lỗi gán sai chứng từ",
        LocalDate.of(2026, 3, 1),
        TrangThaiXuLyLoi.CHO_XU_LY,
        null);

    assertThrows(DataAccessException.class, () -> dao.insert(spl),
        "Trigger fn_sanphamloi_nguon_hop_le phải chặn chứng từ không chứa serial");
  }

  @Test
  @DisplayName("updateTinhTrang cập nhật đúng trạng thái, ghi chú và ngày cập nhật")
  void updateTinhTrangSuccess() {
    SanPham sp = new SanPham(SERIAL_TEST, null, null, null, null, null, 0, 0);
    SanPhamLoi spl = new SanPhamLoi(
        MA_LOI_TEST,
        sp,
        null,
        null,
        "Ghi chú cũ",
        "Mô tả lỗi",
        LocalDate.of(2026, 3, 1),
        TrangThaiXuLyLoi.CHO_XU_LY,
        null);
    dao.insert(spl);

    LocalDate ngayCapNhat = LocalDate.of(2026, 3, 5);
    dao.updateTinhTrang(MA_LOI_TEST, TrangThaiXuLyLoi.DA_GUI_BAO_HANH, "Đã gửi trung tâm bảo hành", ngayCapNhat);

    Optional<SanPhamLoi> updated = dao.findById(MA_LOI_TEST);
    assertTrue(updated.isPresent());
    assertEquals(TrangThaiXuLyLoi.DA_GUI_BAO_HANH, updated.get().tinhTrangXuLy());
    assertEquals("Đã gửi trung tâm bảo hành", updated.get().ghiChuXuLy());
    assertEquals(ngayCapNhat, updated.get().ngayCapNhap());
  }

  @Test
  @DisplayName("insert và updateTinhTrang với Connection hỗ trợ transaction")
  void insertAndUpdateWithConnectionTransaction() throws Exception {
    try (Connection conn = ConnectDB.getDataSource().getConnection()) {
      conn.setAutoCommit(false);
      try {
        SanPham sp = new SanPham(SERIAL_TEST, null, null, null, null, null, 0, 0);
        SanPhamLoi spl = new SanPhamLoi(
            MA_LOI_TEST,
            sp,
            null,
            null,
            null,
            "Lỗi trong transaction",
            LocalDate.of(2026, 3, 1),
            TrangThaiXuLyLoi.CHO_XU_LY,
            null);

        dao.insert(conn, spl);
        dao.updateTinhTrang(conn, MA_LOI_TEST, TrangThaiXuLyLoi.DA_TRA_NHA_CUNG_CAP, "Trả lại nhà cung cấp", LocalDate.of(2026, 3, 2));

        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }

    Optional<SanPhamLoi> found = dao.findById(MA_LOI_TEST);
    assertTrue(found.isPresent());
    assertEquals(TrangThaiXuLyLoi.DA_TRA_NHA_CUNG_CAP, found.get().tinhTrangXuLy());
  }
}
