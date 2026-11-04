package com.dtaq.retail.ui.page.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dtaq.retail.controller.SanPhamController;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.shell.PageId;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử khởi tạo màn hình quản lý sản phẩm. */
class ProductPageTest {

  @Test
  @DisplayName("Trang ProductPage phải có id, tiêu đề và section đúng")
  void pageProperties_ok() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    SanPhamController controller = new SanPhamController(
        sanPhamDao,
        new FakeDongSanPhamDao(),
        new FakeNhaCungCapDao(),
        new FakeDataSource());

    ProductPage page = new ProductPage(controller, sanPhamDao);

    assertEquals(PageId.PRODUCTS, page.id());
    assertEquals("Quản lý sản phẩm", page.title());
    assertEquals("Quản lý", page.section());
    assertFalse(page.scrollable());
    assertNotNull(page.view());
  }

  @Test
  @DisplayName("Hiển thị danh mục dạng văn bản đọc được")
  void formatDanhMucDisplay_hienThiDocDuoc() {
    DanhMucSanPham danhMuc = new DanhMucSanPham("DM-2026-000001", "Điện tử gia dụng", "Máy giặt mini");

    assertEquals("DM-2026-000001 — Điện tử gia dụng — Máy giặt mini",
        ProductLinesPage.formatDanhMucDisplay(danhMuc));
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

  private static final class FakeNhaCungCapDao implements NhaCungCapDao {
    @Override
    public void insert(Connection ketNoi, NhaCungCap nhaCungCap) {}

    @Override
    public Optional<NhaCungCap> findById(String maNCC) {
      return Optional.empty();
    }

    @Override
    public List<NhaCungCap> findAll() {
      return List.of();
    }

    @Override
    public void updateTenNCC(String maNCC, String tenNCC) {}
  }

  private static final class FakeDataSource implements DataSource {
    @Override
    public Connection getConnection() {
      throw new UnsupportedOperationException("FakeDataSource không dùng cho test UI");
    }

    @Override
    public Connection getConnection(String username, String password) {
      throw new UnsupportedOperationException("FakeDataSource không dùng cho test UI");
    }

    @Override
    public java.io.PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
      return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
      return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
