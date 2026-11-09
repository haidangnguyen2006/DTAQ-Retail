package com.dtaq.retail.ui.page.defective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.SanPhamLoiController;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.ui.shell.PageId;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử khởi tạo và cấu hình màn hình quản lý sản phẩm lỗi.
 */
class DefectiveProductsPageTest {

  @Test
  @DisplayName("Kiểm tra định danh và thuộc tính trang sản phẩm lỗi")
  void kiemTraThuocTinhTrang() {
    FakeSanPhamLoiDao fakeLoiDao = new FakeSanPhamLoiDao();
    FakeSanPhamDao fakeSpDao = new FakeSanPhamDao();
    SanPhamLoiController controller = new SanPhamLoiController(fakeLoiDao, fakeSpDao, ConnectDB.getDataSource());
    DefectiveProductsPage page = new DefectiveProductsPage(controller);

    assertEquals(PageId.DEFECTIVE_PRODUCTS, page.id());
    assertEquals("Quản lý sản phẩm lỗi", page.title());
    assertEquals("Quản lý", page.section());
    assertFalse(page.scrollable());
    assertNotNull(page.view());
  }

  private static final class FakeSanPhamLoiDao implements SanPhamLoiDao {
    @Override
    public void insert(Connection ketNoi, SanPhamLoi sanPhamLoi) {}

    @Override
    public void insert(SanPhamLoi sanPhamLoi) {}

    @Override
    public Optional<SanPhamLoi> findById(String maLoi) {
      return Optional.empty();
    }

    @Override
    public Optional<SanPhamLoi> findDangXuLy(String soSerial) {
      return Optional.empty();
    }

    @Override
    public List<SanPhamLoi> findAll() {
      return List.of();
    }

    @Override
    public List<SanPhamLoi> findByTinhTrang(TrangThaiXuLyLoi tinhTrangXuLy) {
      return List.of();
    }

    @Override
    public void updateTinhTrang(Connection ketNoi, String maLoi, TrangThaiXuLyLoi tinhTrangMoi, String ghiChuXuLy, LocalDate ngayCapNhat) {}

    @Override
    public void updateTinhTrang(String maLoi, TrangThaiXuLyLoi tinhTrangMoi, String ghiChuXuLy, LocalDate ngayCapNhat) {}
  }

  private static final class FakeSanPhamDao implements SanPhamDao {
    @Override
    public void insert(Connection ketNoi, SanPham sanPham) {}

    @Override
    public Optional<SanPham> findById(String soSerial) {
      return Optional.empty();
    }

    @Override
    public List<SanPham> findByDongSanPham(String maDongSP) {
      return List.of();
    }

    @Override
    public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
      return List.of();
    }

    @Override
    public List<SanPham> findAll() {
      return List.of();
    }

    @Override
    public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {}
  }
}
