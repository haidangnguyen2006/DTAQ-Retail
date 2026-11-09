package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
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
 * Kiểm thử tích hợp {@link KhuyenMaiDaoImpl} trên cơ sở dữ liệu PostgreSQL.
 */
class KhuyenMaiDaoImplTest {

  private static final String MA_KM_SEED_ACTIVE = "KM-2026-000003"; // đang áp dụng (true), 2026-10-01 -> 2026-11-30, DSP-2026-000003
  private static final String DONG_SP_SEED = "DSP-2026-000003";

  private static final String MA_KM_TEST = "KM-TEST-999";
  private static final String DONG_SP_TEST = "DSP-2026-000001";

  private KhuyenMaiDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "CSDL không khả dụng");
    dao = new KhuyenMaiDaoImpl(ConnectDB.getDataSource());
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
        var ps = conn.prepareStatement("DELETE FROM \"KhuyenMai\" WHERE \"maKM\" = ?")) {
      ps.setString(1, MA_KM_TEST);
      ps.executeUpdate();
    } catch (Exception ignored) {
    }
  }

  @Test
  @DisplayName("findAll trả về danh sách khuyến mãi từ seed V4")
  void findAllReturnsPromotions() {
    List<KhuyenMai> list = dao.findAll();
    assertNotNull(list);
    assertFalse(list.isEmpty(), "Danh sách khuyến mãi seed V4 không được rỗng");
    assertTrue(list.stream().anyMatch(km -> MA_KM_SEED_ACTIVE.equals(km.maKM())),
        "Phải chứa khuyến mãi KM-2026-000003");
  }

  @Test
  @DisplayName("findById nạp đầy đủ thông tin dòng sản phẩm liên kết")
  void findByIdReturnsFullPromotionDetails() {
    Optional<KhuyenMai> opt = dao.findById(MA_KM_SEED_ACTIVE);
    assertTrue(opt.isPresent());

    KhuyenMai km = opt.get();
    assertEquals(MA_KM_SEED_ACTIVE, km.maKM());
    assertNotNull(km.dongSanPham(), "Phải nạp DongSanPham");
    assertEquals(DONG_SP_SEED, km.dongSanPham().maDongSP());
    assertTrue(km.dangApDung());
    assertEquals(0, new BigDecimal("15").compareTo(km.tyLeGiam()));
  }

  @Test
  @DisplayName("findHieuLuc tìm đúng khuyến mãi có hiệu lực tại ngày xác định (UC003)")
  void findHieuLucReturnsValidPromotion() {
    // KM-2026-000003 có hiệu lực từ 2026-10-01 đến 2026-11-30 và dangApDung = true
    LocalDate ngayTrongKhoang = LocalDate.of(2026, 10, 15);
    Optional<KhuyenMai> opt = dao.findHieuLuc(DONG_SP_SEED, ngayTrongKhoang);

    assertTrue(opt.isPresent(), "Phải tìm thấy khuyến mãi trong khoảng hiệu lực");
    assertEquals(MA_KM_SEED_ACTIVE, opt.get().maKM());

    // Ngày trước thời gian hiệu lực
    LocalDate ngayTruoc = LocalDate.of(2026, 9, 30);
    assertTrue(dao.findHieuLuc(DONG_SP_SEED, ngayTruoc).isEmpty(), "Trước ngày bắt đầu phải không thấy");

    // Ngày sau thời gian hiệu lực
    LocalDate ngaySau = LocalDate.of(2026, 12, 1);
    assertTrue(dao.findHieuLuc(DONG_SP_SEED, ngaySau).isEmpty(), "Sau ngày kết thúc phải không thấy");
  }

  @Test
  @DisplayName("findByDongSanPham lấy đúng danh sách khuyến mãi của dòng SP đó")
  void findByDongSanPhamReturnsCorrectList() {
    List<KhuyenMai> list = dao.findByDongSanPham(DONG_SP_SEED);
    assertFalse(list.isEmpty());
    for (KhuyenMai km : list) {
      assertEquals(DONG_SP_SEED, km.dongSanPham().maDongSP());
    }
  }

  @Test
  @DisplayName("insert thêm mới chương trình khuyến mãi thành công")
  void insertPromotionSuccess() {
    DongSanPham dsp = new DongSanPham(
        DONG_SP_TEST, null, null, null, 0, null, null, true, null, null);
    KhuyenMai km = new KhuyenMai(
        MA_KM_TEST,
        "Khuyến mãi Test JUnit",
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 15),
        new BigDecimal("10.50"),
        "Giảm giá 10.5%",
        false,
        dsp);

    dao.insert(km);

    Optional<KhuyenMai> found = dao.findById(MA_KM_TEST);
    assertTrue(found.isPresent());
    assertEquals("Khuyến mãi Test JUnit", found.get().tenKM());
    assertEquals(0, new BigDecimal("10.50").compareTo(found.get().tyLeGiam()));
    assertFalse(found.get().dangApDung());
  }

  @Test
  @DisplayName("insert ném lỗi khi ngày bắt đầu sau ngày kết thúc hoặc tỷ lệ giảm âm")
  void insertInvalidValidationThrowsException() {
    DongSanPham dsp = new DongSanPham(
        DONG_SP_TEST, null, null, null, 0, null, null, true, null, null);

    // Ngày không hợp lệ
    KhuyenMai kmNgaySai = new KhuyenMai(
        MA_KM_TEST, "Tên", LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1),
        BigDecimal.TEN, "Mô tả", false, dsp);
    assertThrows(IllegalArgumentException.class, () -> dao.insert(kmNgaySai));

    // Tỷ lệ vượt quá 100
    KhuyenMai kmTyLeSai = new KhuyenMai(
        MA_KM_TEST, "Tên", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10),
        new BigDecimal("150"), "Mô tả", false, dsp);
    assertThrows(IllegalArgumentException.class, () -> dao.insert(kmTyLeSai));
  }

  @Test
  @DisplayName("update cập nhật thành công thông tin khuyến mãi")
  void updatePromotionSuccess() {
    DongSanPham dsp = new DongSanPham(
        DONG_SP_TEST, null, null, null, 0, null, null, true, null, null);
    KhuyenMai km = new KhuyenMai(
        MA_KM_TEST, "Tên cũ", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10),
        BigDecimal.TEN, "Mô tả cũ", false, dsp);
    dao.insert(km);

    KhuyenMai kmMoi = new KhuyenMai(
        MA_KM_TEST, "Tên mới cập nhật", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20),
        new BigDecimal("25.00"), "Mô tả mới", false, dsp);
    dao.update(kmMoi);

    Optional<KhuyenMai> found = dao.findById(MA_KM_TEST);
    assertTrue(found.isPresent());
    assertEquals("Tên mới cập nhật", found.get().tenKM());
    assertEquals(LocalDate.of(2026, 11, 20), found.get().ngayKetThuc());
    assertEquals(0, new BigDecimal("25.00").compareTo(found.get().tyLeGiam()));
  }

  @Test
  @DisplayName("updateTrangThaiApDung bật và tắt cờ dangApDung thành công")
  void updateTrangThaiApDungSuccess() {
    DongSanPham dsp = new DongSanPham(
        DONG_SP_TEST, null, null, null, 0, null, null, true, null, null);
    KhuyenMai km = new KhuyenMai(
        MA_KM_TEST, "Tên", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10),
        BigDecimal.TEN, "Mô tả", false, dsp);
    dao.insert(km);

    dao.updateTrangThaiApDung(MA_KM_TEST, true);
    assertTrue(dao.findById(MA_KM_TEST).orElseThrow().dangApDung());

    dao.updateTrangThaiApDung(MA_KM_TEST, false);
    assertFalse(dao.findById(MA_KM_TEST).orElseThrow().dangApDung());
  }

  @Test
  @DisplayName("existsChongLan phát hiện chính xác khi khoảng ngày giao nhau trên chương trình đang áp dụng")
  void existsChongLanDetectsOverlap() {
    // KM-2026-000003: DONG_SP_SEED, 2026-10-01 -> 2026-11-30, dangApDung = true
    // Khoảng ngày giao nhau (2026-10-15 -> 2026-10-25)
    boolean chongLan = dao.existsChongLan(
        DONG_SP_SEED,
        LocalDate.of(2026, 10, 15),
        LocalDate.of(2026, 10, 25),
        null);
    assertTrue(chongLan, "Khoảng ngày nằm lọt trong KM đang chạy phải báo chồng lấn");

    // Loại trừ chính nó (khi sửa)
    boolean khongChongLanChinhNo = dao.existsChongLan(
        DONG_SP_SEED,
        LocalDate.of(2026, 10, 15),
        LocalDate.of(2026, 10, 25),
        MA_KM_SEED_ACTIVE);
    assertFalse(khongChongLanChinhNo, "Loại trừ chính mã đang sửa thì không báo chồng lấn");

    // Khoảng ngày không giao nhau (2026-12-01 -> 2026-12-31)
    boolean ngoaiKhoang = dao.existsChongLan(
        DONG_SP_SEED,
        LocalDate.of(2026, 12, 1),
        LocalDate.of(2026, 12, 31),
        null);
    assertFalse(ngoaiKhoang, "Khoảng ngày sau khi hết hạn không bị chồng lấn");
  }
}
