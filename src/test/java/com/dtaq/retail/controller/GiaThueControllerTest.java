package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GiaThueControllerTest {

  @BeforeEach
  void moPhienQuanLy() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
  }

  @AfterEach
  void dongPhien() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Giá mới bằng 0 phải bị từ chối")
  void capNhat_giaBangKhongBiChapNhan() {
    FakeDongSanPhamDao fakeDao = new FakeDongSanPhamDao();
    GiaThueController controller = new GiaThueController(fakeDao);

    var ketQua = controller.capNhat(
        "DSP-2026-000001",
        BigDecimal.ZERO,
        BigDecimal.valueOf(10));

    assertFalse(ketQua.thanhCong());
  }

  @Test
  @DisplayName("Không có phiên hoặc không phải quản lý thì bị chặn trước mọi kiểm tra")
  void khongPhaiQuanLyThiChan() {
    FakeDongSanPhamDao fakeDao = new FakeDongSanPhamDao();
    GiaThueController controller = new GiaThueController(fakeDao);

    CurrentSession.clear();
    assertThrows(SecurityException.class, () -> controller.capNhat(
        "DSP-2026-000001", BigDecimal.valueOf(15000000), BigDecimal.valueOf(10)));

    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    assertThrows(SecurityException.class, () -> controller.capNhatGiaThue(
        "DSP-2026-000001", BigDecimal.valueOf(15000000), BigDecimal.valueOf(10)));

    assertEquals(0, fakeDao.soLanCapNhat);
  }

  private static NhanVien nhanVien(LoaiQuyen loaiQuyen) {
    return new NhanVien(
        "NVQUANLY-TEST-001",
        "Nguyễn Quản Lý",
        "0900000001",
        "quanly.test@dtaq.local",
        LocalDate.of(2026, 1, 1),
        "quanly.test",
        "matkhau-test",
        loaiQuyen,
        true);
  }

  private static final class FakeDongSanPhamDao implements DongSanPhamDao {

    int soLanCapNhat;

    @Override
    public void insert(Connection ketNoi, DongSanPham dongSanPham) {}

    @Override
    public Optional<DongSanPham> findById(String maDongSP) {
      return Optional.of(new DongSanPham(
          "DSP-2026-000001",
          "Laptop",
          new DanhMucSanPham("DM-2026-000001", "Điện tử", "Thiết bị điện tử"),
          "Chiếc",
          10,
          BigDecimal.valueOf(10),
          BigDecimal.valueOf(15000000),
          true,
          LocalDate.now(),
          LocalDate.now()));
    }

    @Override
    public List<DongSanPham> findAll() {
      return List.of();
    }

    @Override
    public List<DongSanPham> findByDanhMuc(String maDanhMuc) {
      return List.of();
    }

    @Override
    public void updateSoLuongTon(String maDongSP, int soLuongTon) {}

    @Override
    public void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh) {}

    @Override
    public void updateGiaThue(String maDongSP, java.math.BigDecimal giaBanTruocThue, java.math.BigDecimal thue) {
      soLanCapNhat++;
    }
  }
}
