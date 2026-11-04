package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.LoaiQuyen;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GiaThueControllerTest {

  @Test
  @DisplayName("Giá mới bằng 0 phải bị từ chối")
  void capNhat_giaBangKhongBiChapNhan() {
    FakeDongSanPhamDao fakeDao = new FakeDongSanPhamDao();
    GiaThueController controller = new GiaThueController(fakeDao);

    var ketQua = controller.capNhat(
        "DSP-2026-000001",
        BigDecimal.ZERO,
        BigDecimal.valueOf(10),
        LoaiQuyen.NHAN_VIEN_QUAN_LY);

    assertFalse(ketQua.thanhCong());
  }

  private static final class FakeDongSanPhamDao implements DongSanPhamDao {
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
    public void updateGiaThue(String maDongSP, java.math.BigDecimal giaBanTruocThue, java.math.BigDecimal thue) {}
  }
}
