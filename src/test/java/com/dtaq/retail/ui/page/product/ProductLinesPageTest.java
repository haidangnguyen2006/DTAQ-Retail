package com.dtaq.retail.ui.page.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.controller.DongSanPhamController;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import java.awt.Component;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử nút ngừng / kinh doanh lại dòng sản phẩm trên màn hình UC009. */
class ProductLinesPageTest {

  private FakeDongSanPhamDao dongSanPhamDao;
  private ProductLinesPage page;

  @BeforeEach
  void setUp() throws Exception {
    CurrentSession.set(new NhanVien(
        "NVQUANLY-TEST-001",
        "Nguyễn Quản Lý",
        "0900000001",
        "quanly.test@dtaq.local",
        LocalDate.of(2026, 1, 1),
        "quanly.test",
        "matkhau-test",
        LoaiQuyen.NHAN_VIEN_QUAN_LY,
        true));
    dongSanPhamDao = new FakeDongSanPhamDao();
    DongSanPhamController controller = new DongSanPhamController(dongSanPhamDao, new FakeDanhMucDao(), null);
    onEdt(() -> page = new ProductLinesPage(controller, new FakeDanhMucDao()));
    onEdt(() -> {
      assertEquals(0, bangHienTai().getRowCount(), "bảng không được tự nạp trước khi tìm kiếm");
      page.timKiem();
    });
  }

  private javax.swing.table.DefaultTableModel bangHienTai() {
    try {
      var field = ProductLinesPage.class.getDeclaredField("tableModel");
      field.setAccessible(true);
      return (javax.swing.table.DefaultTableModel) field.get(page);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @AfterEach
  void tearDown() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Chưa chọn dòng thì cả hai nút đều bị tắt")
  void chuaChonDongThiNutBiTat() throws Exception {
    onEdt(() -> {
      assertFalse(page.nutNgung().isEnabled());
      assertFalse(page.nutKinhDoanhLai().isEnabled());
    });
  }

  @Test
  @DisplayName("Chọn dòng đang kinh doanh thì bật nút Ngừng kinh doanh")
  void chonDongDangKinhDoanhThiNutNgungBat() throws Exception {
    onEdt(() -> page.chonDong(0));
    onEdt(() -> {
      assertTrue(page.nutNgung().isEnabled());
      assertFalse(page.nutKinhDoanhLai().isEnabled());
    });
  }

  @Test
  @DisplayName("Chọn dòng đã ngừng thì bật nút Kinh doanh lại")
  void chonDongDaNgungThiNutKinhDoanhLaiBat() throws Exception {
    onEdt(() -> page.chonDong(1));
    onEdt(() -> {
      assertFalse(page.nutNgung().isEnabled());
      assertTrue(page.nutKinhDoanhLai().isEnabled());
    });
  }

  @Test
  @DisplayName("Áp dụng ngừng kinh doanh gọi đúng DAO và tải lại danh sách")
  void apDungNgungGoiDungDao() throws Exception {
    onEdt(() -> page.chonDong(0));
    int truoc = dongSanPhamDao.findAllCount.get();

    onEdt(() -> page.apDungTrangThai(false));
    awaitReload(truoc + 1);

    assertEquals("DSP-TEST-000001", dongSanPhamDao.lastMa.get());
    assertEquals(Boolean.FALSE, dongSanPhamDao.lastTrangThai.get());
  }

  @Test
  @DisplayName("Áp dụng kinh doanh lại gọi đúng DAO và tải lại danh sách")
  void apDungKinhDoanhLaiGoiDungDao() throws Exception {
    onEdt(() -> page.chonDong(1));
    int truoc = dongSanPhamDao.findAllCount.get();

    onEdt(() -> page.apDungTrangThai(true));
    awaitReload(truoc + 1);

    assertEquals("DSP-TEST-000002", dongSanPhamDao.lastMa.get());
    assertEquals(Boolean.TRUE, dongSanPhamDao.lastTrangThai.get());
  }

  @Test
  @DisplayName("Sau cập nhật trạng thái, chỉ một nút trạng thái được kích hoạt")
  void apDungTrangThai_toggleNutKinhDoanh() throws Exception {
    onEdt(() -> page.chonDong(0));
    int truoc = dongSanPhamDao.findAllCount.get();

    onEdt(() -> page.apDungTrangThai(false));
    awaitReload(truoc + 1);

    onEdt(() -> {
      assertFalse(page.nutNgung().isEnabled());
      assertTrue(page.nutKinhDoanhLai().isEnabled());
    });
  }

  @Test
  @DisplayName("Bấm sửa phải nạp dữ liệu dòng đang chọn lên form")
  void chonSua_napDuLieuLenForm() throws Exception {
    onEdt(() -> page.chonDong(0));
    onEdt(() -> {
      try {
        invokePrivate(page, "chonSua", 0);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    onEdt(() -> {
      try {
        assertEquals("Laptop Dell", getFieldText(page, "tenField"));
        assertEquals("DM-TEST-000001", ((DanhMucSanPham) getComboValue(page, "categoryBox")).maDanhMuc());
        assertEquals("Chiếc", getFieldText(page, "unitField"));
        assertEquals("1.000.000", getFieldText(page, "priceField"));
        assertEquals("10", getFieldText(page, "taxField"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  @DisplayName("Cột thao tác phải editable để nút Sửa/Hủy hoạt động")
  void cotThaoTac_phaiEditable() throws Exception {
    onEdt(() -> {
      try {
        var field = ProductLinesPage.class.getDeclaredField("tableModel");
        field.setAccessible(true);
        var tableModel = (javax.swing.table.DefaultTableModel) field.get(page);
        assertTrue(tableModel.isCellEditable(0, 7));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  @DisplayName("Bấm sửa phải hiện nút Hủy trong cột thao tác của dòng đang chỉnh")
  void chonSua_hienThiNutHuyTrongCotThaoTac() throws Exception {
    onEdt(() -> {
      try {
        invokePrivate(page, "chonSua", 0);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    onEdt(() -> {
      try {
        var labels = getActionLabels(page, 0, 7);
        assertTrue(labels.contains("Sửa"));
        assertTrue(labels.contains("Hủy"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void awaitReload(int expected) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 3_000L;
    while (dongSanPhamDao.findAllCount.get() < expected && System.currentTimeMillis() < deadline) {
      Thread.sleep(20L);
    }
    assertEquals(expected, dongSanPhamDao.findAllCount.get(), "Danh sách chưa được tải lại");
  }

  private static void onEdt(Runnable action) throws Exception {
    SwingUtilities.invokeAndWait(action);
  }

  private static String getFieldText(ProductLinesPage target, String fieldName) throws Exception {
    var field = ProductLinesPage.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    Object value = field.get(target);
    return value instanceof com.dtaq.retail.ui.component.FormField formField ? formField.getText() : String.valueOf(value);
  }

  private static List<String> getActionLabels(ProductLinesPage target, int row, int column) throws Exception {
    var tableField = ProductLinesPage.class.getDeclaredField("table");
    tableField.setAccessible(true);
    var dataTable = (com.dtaq.retail.ui.component.DataTable) tableField.get(target);
    var swingTable = dataTable.getTable();
    var renderer = swingTable.getColumnModel().getColumn(column).getCellRenderer();
    Component component = renderer.getTableCellRendererComponent(swingTable, null, false, false, row, column);
    List<String> labels = new ArrayList<>();
    collectButtonTexts(component, labels);
    return labels;
  }

  private static void collectButtonTexts(java.awt.Component component, List<String> labels) {
    if (component instanceof javax.swing.AbstractButton button) {
      labels.add(button.getText());
    }
    if (component instanceof java.awt.Container container) {
      for (java.awt.Component child : container.getComponents()) {
        collectButtonTexts(child, labels);
      }
    }
  }

  private static Object getComboValue(ProductLinesPage target, String fieldName) throws Exception {
    var field = ProductLinesPage.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return ((javax.swing.JComboBox<?>) field.get(target)).getSelectedItem();
  }

  private static void invokePrivate(ProductLinesPage target, String methodName, int rowIndex) throws Exception {
    var method = ProductLinesPage.class.getDeclaredMethod(methodName, int.class);
    method.setAccessible(true);
    method.invoke(target, rowIndex);
  }

  private static final class FakeDongSanPhamDao implements DongSanPhamDao {
    private final List<DongSanPham> rows = new ArrayList<>();
    private final AtomicInteger findAllCount = new AtomicInteger();
    private final AtomicReference<String> lastMa = new AtomicReference<>();
    private final AtomicReference<Boolean> lastTrangThai = new AtomicReference<>();

    FakeDongSanPhamDao() {
      rows.add(dong("DSP-TEST-000001", "Laptop Dell", true));
      rows.add(dong("DSP-TEST-000002", "Màn hình LG", false));
    }

    private static DongSanPham dong(String ma, String ten, boolean trangThai) {
      return new DongSanPham(
          ma,
          ten,
          new DanhMucSanPham("DM-TEST-000001", "Điện tử", "Thiết bị điện tử"),
          "Chiếc",
          10,
          BigDecimal.valueOf(10),
          BigDecimal.valueOf(1_000_000),
          trangThai,
          LocalDate.of(2026, 1, 1),
          LocalDate.of(2026, 1, 1));
    }

    @Override
    public void insert(Connection ketNoi, DongSanPham dongSanPham) {
    }

    @Override
    public Optional<DongSanPham> findById(String maDongSP) {
      return rows.stream().filter(d -> d.maDongSP().equals(maDongSP)).findFirst();
    }

    @Override
    public List<DongSanPham> findAll() {
      findAllCount.incrementAndGet();
      return List.copyOf(rows);
    }

    @Override
    public List<DongSanPham> findByDanhMuc(String maDanhMuc) {
      return List.copyOf(rows);
    }

    @Override
    public void updateSoLuongTon(String maDongSP, int soLuongTon) {
    }

    @Override
    public void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh) {
      lastMa.set(maDongSP);
      lastTrangThai.set(trangThaiKinhDoanh);
      for (int i = 0; i < rows.size(); i++) {
        DongSanPham cu = rows.get(i);
        if (cu.maDongSP().equals(maDongSP)) {
          rows.set(i, new DongSanPham(
              cu.maDongSP(), cu.tenDongSP(), cu.danhMucSanPham(), cu.donViTinh(), cu.soLuongTon(),
              cu.thue(), cu.donGiaBanTruocThue(), trangThaiKinhDoanh, cu.ngayNhapKho(), cu.ngayCapNhapCuoi()));
        }
      }
    }
  }

  private static final class FakeDanhMucDao implements DanhMucSanPhamDao {
    private final List<DanhMucSanPham> rows = List.of(
        new DanhMucSanPham("DM-TEST-000001", "Điện tử", "Thiết bị điện tử"));

    @Override
    public List<DanhMucSanPham> findAll() {
      return rows;
    }

    @Override
    public Optional<DanhMucSanPham> findById(String maDanhMuc) {
      return rows.stream().filter(d -> d.maDanhMuc().equals(maDanhMuc)).findFirst();
    }

    @Override
    public Optional<DanhMucSanPham> findByTen(String tenDanhMuc) {
      return rows.stream().filter(d -> d.tenDanhMuc().equals(tenDanhMuc)).findFirst();
    }

    @Override
    public void insert(DanhMucSanPham danhMuc) {
    }

    @Override
    public void update(DanhMucSanPham danhMuc) {
    }

    @Override
    public void delete(String maDanhMuc) {
    }

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
