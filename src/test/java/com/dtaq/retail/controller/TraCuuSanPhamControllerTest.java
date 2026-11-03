package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.impl.DongSanPhamImpl;
import com.dtaq.retail.dao.impl.SanPhamImpl;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.SanPham;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử UC004: tra cứu sản phẩm và tồn kho theo các tiêu chí. */
class TraCuuSanPhamControllerTest {

  @Test
  @DisplayName("Tra cứu theo serial trả về đúng dòng và máy tương ứng")
  void timKiemTheoSerial_thanhCong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    TraCuuSanPhamController controller = new TraCuuSanPhamController(sanPhamDao, dongSanPhamDao);

    SanPham sanPham = sanPhamDao.findAll().stream().findFirst().orElseThrow();
    List<TraCuuSanPhamController.KetQuaDong> ketQua =
        controller.timKiem(TraCuuSanPhamController.TieuChi.SERIAL, sanPham.soSerial());

    assertFalse(ketQua.isEmpty());
    assertEqualsOrContains(sanPham.dongSanPham().maDongSP(), ketQua.getFirst().maDongSP());
  }

  @Test
  @DisplayName("Tra cứu theo tên dòng hoặc từ khóa hợp lệ phải trả về danh sách không rỗng")
  void timKiemTheoDong_thanhCong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    TraCuuSanPhamController controller = new TraCuuSanPhamController(sanPhamDao, dongSanPhamDao);

    DongSanPham dong = dongSanPhamDao.findAll().stream().findFirst().orElseThrow();
    List<TraCuuSanPhamController.KetQuaDong> ketQua =
        controller.timKiem(TraCuuSanPhamController.TieuChi.DONG_SAN_PHAM, dong.maDongSP());

    assertFalse(ketQua.isEmpty());
    assertNotNull(ketQua.getFirst().danhSachSanPham());
  }

  @Test
  @DisplayName("Tra cứu theo danh mục phải trả về danh sách kết quả có liên quan")
  void timKiemTheoDanhMuc_thanhCong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    TraCuuSanPhamController controller = new TraCuuSanPhamController(sanPhamDao, dongSanPhamDao);

    DongSanPham dong = dongSanPhamDao.findAll().stream().findFirst().orElseThrow();
    String danhMuc = dong.danhMucSanPham().tenDanhMuc();
    List<TraCuuSanPhamController.KetQuaDong> ketQua =
        controller.timKiem(TraCuuSanPhamController.TieuChi.DANH_MUC, danhMuc);

    assertFalse(ketQua.isEmpty());
    assertTrue(ketQua.stream().anyMatch(item -> item.maDongSP().equals(dong.maDongSP())));
  }

  @Test
  @DisplayName("Từ khóa không tồn tại phải trả về danh sách rỗng")
  void timKiemKhongTonTai_traVeRong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    TraCuuSanPhamController controller = new TraCuuSanPhamController(sanPhamDao, dongSanPhamDao);

    List<TraCuuSanPhamController.KetQuaDong> ketQua =
        controller.timKiem(TraCuuSanPhamController.TieuChi.SERIAL, "SERIAL-KHONG-TON-TAI-XYZ-123");

    assertTrue(ketQua.isEmpty());
  }

  private static void assertEqualsOrContains(String expected, String actual) {
    assertTrue(actual.contains(expected) || expected.contains(actual),
        "Mã mong đợi: " + expected + " nhưng nhận được: " + actual);
  }
}
