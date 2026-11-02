package com.dtaq.retail.ui.page.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.DanhMucController;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.ui.shell.PageId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử khởi tạo và cấu hình màn hình quản lý danh mục sản phẩm.
 */
class CategoriesPageTest {

  @Test
  @DisplayName("Kiểm tra định danh và thuộc tính trang danh mục")
  void kiemTraThuocTinhTrang() {
    FakeDao fakeDao = new FakeDao();
    DanhMucController controller = new DanhMucController(fakeDao, ConnectDB.getDataSource());
    CategoriesPage page = new CategoriesPage(controller);

    assertEquals(PageId.CATEGORIES, page.id());
    assertEquals("Quản lý danh mục sản phẩm", page.title());
    assertEquals("Quản lý", page.section());
    assertFalse(page.scrollable());
    assertNotNull(page.view());
  }

  private static final class FakeDao implements DanhMucSanPhamDao {
    @Override
    public List<DanhMucSanPham> findAll() {
      return List.of(new DanhMucSanPham("DM-TEST-001", "Điện thoại", "Mô tả test"));
    }

    @Override
    public Optional<DanhMucSanPham> findById(String maDanhMuc) {
      return Optional.empty();
    }

    @Override
    public Optional<DanhMucSanPham> findByTen(String tenDanhMuc) {
      return Optional.empty();
    }

    @Override
    public void insert(DanhMucSanPham danhMuc) {}

    @Override
    public void update(DanhMucSanPham danhMuc) {}

    @Override
    public void delete(String maDanhMuc) {}

    @Override
    public boolean existsDongByDanhMuc(String maDanhMuc) {
      return false;
    }

    @Override
    public boolean existsByTen(String tenDanhMuc, String excludeMaDanhMuc) {
      return false;
    }
  }
}
