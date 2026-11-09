package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp {@link DanhMucSanPhamDaoImpl} trên cơ sở dữ liệu PostgreSQL.
 */
class DanhMucSanPhamDaoImplTest {

  private static final String MA_DM_SEED = "DM-2026-000001";
  private static final String MA_DM_TEST = "DM-TEST-999";
  private static final String TEN_DM_TEST = "Danh Mục Thử Nghiệm JUnit";

  private DanhMucSanPhamDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "CSDL không khả dụng");
    dao = new DanhMucSanPhamDaoImpl(ConnectDB.getDataSource());
    cleanUpTestData();
  }

  @AfterEach
  void tearDown() {
    if (ConnectDB.isAvailable()) {
      cleanUpTestData();
    }
  }

  private void cleanUpTestData() {
    try {
      dao.delete(MA_DM_TEST);
    } catch (Exception ignored) {
      // Bỏ qua nếu chưa tồn tại
    }
  }

  @Test
  @DisplayName("findAll trả về danh sách danh mục có sẵn từ seed V4")
  void findAllReturnsCategories() {
    List<DanhMucSanPham> list = dao.findAll();
    assertNotNull(list);
    assertFalse(list.isEmpty(), "Danh sách danh mục từ seed V4 không được rỗng");
    assertTrue(list.stream().anyMatch(dm -> MA_DM_SEED.equals(dm.maDanhMuc())),
        "Phải chứa danh mục DM001 từ seed V4");
  }

  @Test
  @DisplayName("findById trả về đúng danh mục khi tồn tại mã")
  void findByIdReturnsCategoryWhenExists() {
    Optional<DanhMucSanPham> opt = dao.findById(MA_DM_SEED);
    assertTrue(opt.isPresent(), "DM001 phải tồn tại");
    assertEquals(MA_DM_SEED, opt.get().maDanhMuc());
    assertNotNull(opt.get().tenDanhMuc());
  }

  @Test
  @DisplayName("findById trả về rỗng khi mã không tồn tại hoặc để trống")
  void findByIdReturnsEmptyWhenNotExists() {
    assertTrue(dao.findById("DM-KHONG-TON-TAI").isEmpty());
    assertTrue(dao.findById(null).isEmpty());
    assertTrue(dao.findById("   ").isEmpty());
  }

  @Test
  @DisplayName("findByTen tìm chính xác không phân biệt chữ hoa thường")
  void findByTenCaseInsensitive() {
    Optional<DanhMucSanPham> optSeed = dao.findById(MA_DM_SEED);
    Assumptions.assumeTrue(optSeed.isPresent());

    String ten = optSeed.get().tenDanhMuc();
    Optional<DanhMucSanPham> foundUpper = dao.findByTen(ten.toUpperCase());
    Optional<DanhMucSanPham> foundLower = dao.findByTen(ten.toLowerCase());

    assertTrue(foundUpper.isPresent(), "Phải tìm thấy khi viết HOA");
    assertTrue(foundLower.isPresent(), "Phải tìm thấy khi viết thường");
    assertEquals(MA_DM_SEED, foundUpper.get().maDanhMuc());
    assertEquals(MA_DM_SEED, foundLower.get().maDanhMuc());
  }

  @Test
  @DisplayName("insert và delete thêm và xóa danh mục thành công")
  void insertAndDeleteSuccess() {
    DanhMucSanPham dm = new DanhMucSanPham(MA_DM_TEST, TEN_DM_TEST, "Mô tả kiểm thử");
    dao.insert(dm);

    Optional<DanhMucSanPham> found = dao.findById(MA_DM_TEST);
    assertTrue(found.isPresent());
    assertEquals(TEN_DM_TEST, found.get().tenDanhMuc());
    assertEquals("Mô tả kiểm thử", found.get().moTa());

    dao.delete(MA_DM_TEST);
    assertTrue(dao.findById(MA_DM_TEST).isEmpty());
  }

  @Test
  @DisplayName("insert ném lỗi DataAccessException khi trùng tên chuẩn hóa (23505)")
  void insertDuplicateNameThrowsException() {
    Optional<DanhMucSanPham> optSeed = dao.findById(MA_DM_SEED);
    Assumptions.assumeTrue(optSeed.isPresent());

    String tenTrang = "  " + optSeed.get().tenDanhMuc().toUpperCase() + "  ";
    DanhMucSanPham dmTrang = new DanhMucSanPham(MA_DM_TEST, tenTrang, "Thử trùng");

    assertThrows(DataAccessException.class, () -> dao.insert(dmTrang));
  }

  @Test
  @DisplayName("update cập nhật thành công tên và mô tả")
  void updateSuccess() {
    DanhMucSanPham dm = new DanhMucSanPham(MA_DM_TEST, TEN_DM_TEST, "Mô tả ban đầu");
    dao.insert(dm);

    DanhMucSanPham dmCapNhat = new DanhMucSanPham(MA_DM_TEST, TEN_DM_TEST + " Mới", "Mô tả mới");
    dao.update(dmCapNhat);

    Optional<DanhMucSanPham> found = dao.findById(MA_DM_TEST);
    assertTrue(found.isPresent());
    assertEquals(TEN_DM_TEST + " Mới", found.get().tenDanhMuc());
    assertEquals("Mô tả mới", found.get().moTa());
  }

  @Test
  @DisplayName("existsDongByDanhMuc trả về true với danh mục đang có dòng SP, và xóa ném lỗi 23503")
  void existsDongByDanhMucAndPreventDelete() {
    boolean coDong = dao.existsDongByDanhMuc(MA_DM_SEED);
    assertTrue(coDong, "DM001 phải có dòng sản phẩm liên kết trong seed V4");

    assertThrows(DataAccessException.class, () -> dao.delete(MA_DM_SEED),
        "Xóa danh mục đang có dòng sản phẩm phải bị chặn");
  }

  @Test
  @DisplayName("existsByTen kiểm tra trùng tên chuẩn xác có và không có excludeMaDanhMuc")
  void existsByTenWorksCorrectly() {
    Optional<DanhMucSanPham> optSeed = dao.findById(MA_DM_SEED);
    Assumptions.assumeTrue(optSeed.isPresent());

    String ten = optSeed.get().tenDanhMuc();
    assertTrue(dao.existsByTen(ten, null), "Tên đã có trong hệ thống phải trả về true");
    assertFalse(dao.existsByTen(ten, MA_DM_SEED), "Tên của chính mã đang xét thì loại trừ trả về false");
    assertFalse(dao.existsByTen("Tên Hoàn Toàn Chưa Tồn Tại 123", null));
  }
}
