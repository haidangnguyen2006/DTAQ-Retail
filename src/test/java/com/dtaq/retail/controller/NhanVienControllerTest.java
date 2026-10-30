package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Nghiệp vụ nhân viên (khung UC013)")
class NhanVienControllerTest {

  private final FakeNhanVienDao dao = new FakeNhanVienDao();
  private final NhanVienController controller = new NhanVienController(dao);

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Quản lý xem được danh sách nhân viên")
  void quanLyXemDanhSach() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    assertEquals(dao.ds, controller.danhSach());
  }

  @Test
  @DisplayName("Nhân viên bán hàng hoặc chưa đăng nhập không xem được danh sách")
  void chanSaiQuyen() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    assertThrows(SecurityException.class, controller::danhSach);

    CurrentSession.clear();
    assertThrows(SecurityException.class, controller::danhSach);
    assertEquals(0, dao.soLanDoc);
  }

  private static NhanVien nhanVien(LoaiQuyen loaiQuyen) {
    return new NhanVien("NV", "Nhân viên", "0900000000", null,
        LocalDate.of(2026, 1, 1), "nv", "hash", loaiQuyen, true);
  }

  private static final class FakeNhanVienDao implements NhanVienDao {

    private final List<NhanVien> ds = List.of(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    private int soLanDoc;

    @Override
    public List<NhanVien> findAll() {
      soLanDoc++;
      return ds;
    }

    @Override
    public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public void updateMatKhau(String maNV, String matKhauHash) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public Optional<NhanVien> findByMa(String maNV) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public void insert(Connection ketNoi, NhanVien nhanVien) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public void update(NhanVien nhanVien) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public void updateTrangThai(String maNV, boolean trangThaiHoatDong) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public boolean existsTenDangNhap(String tenDangNhap) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public boolean existsMaNV(String maNV) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }
  }
}
