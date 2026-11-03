package com.dtaq.retail.ui.page.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dtaq.retail.controller.TraCuuSanPhamController;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.shell.PageId;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử khởi tạo màn hình tra cứu sản phẩm và tồn kho. */
class ProductLookupPageTest {

  @Test
  @DisplayName("Trang ProductLookupPage phải có id, tiêu đề và section đúng")
  void pageProperties_ok() {
    TraCuuSanPhamController controller = new TraCuuSanPhamController(new FakeSanPhamDao(), new FakeDongSanPhamDao());
    ProductLookupPage page = new ProductLookupPage(controller);

    assertEquals(PageId.PRODUCT_LOOKUP, page.id());
    assertEquals("Tra cứu sản phẩm", page.title());
    assertEquals("Bán hàng", page.section());
    assertFalse(page.scrollable());
    assertNotNull(page.view());
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

  private static final class FakeDongSanPhamDao implements DongSanPhamDao {
    @Override
    public void insert(Connection ketNoi, DongSanPham dongSanPham) {}

    @Override
    public Optional<DongSanPham> findById(String maDongSP) {
      return Optional.empty();
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
  }
}
