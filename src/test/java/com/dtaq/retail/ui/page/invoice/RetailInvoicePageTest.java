package com.dtaq.retail.ui.page.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.controller.BanHangController;
import com.dtaq.retail.controller.KhachHangController;
import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.dao.KhachHangDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.impl.HoaDonTamFileDaoImpl;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.page.customer.KhachHangPicker;
import com.dtaq.retail.ui.shell.PageId;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử khởi tạo màn hình lập hóa đơn bán lẻ và việc nối bộ điều khiển bán hàng. */
class RetailInvoicePageTest {

  @Test
  @DisplayName("Trang RetailInvoicePage phải có id, tiêu đề, section và trạng thái ban đầu đúng")
  void pageProperties_ok() {
    RetailInvoicePage page = new RetailInvoicePage(nhanVienGia());

    assertEquals(PageId.RETAIL_INVOICE, page.id());
    assertEquals("Lập hóa đơn bán lẻ", page.title());
    assertEquals("Bán hàng", page.section());
    assertFalse(page.scrollable());
    assertNotNull(page.view());
    assertEquals(InvoiceViewState.NO_CUSTOMER, page.getViewState());
  }

  @Test
  @DisplayName("Trang RetailInvoicePage nối được bộ điều khiển bán hàng và bộ chọn khách hàng")
  void pageNoiControllerDungKhongNem() {
    BanHangController banHang = new BanHangController(new FakeSanPhamDao(), new FakeHoaDonTamDao());
    KhachHangPicker picker =
        new KhachHangPicker(new KhachHangController(new FakeKhachHangDao(), dataSourceGia()));

    RetailInvoicePage page = new RetailInvoicePage(nhanVienGia(), banHang, picker);

    assertEquals(PageId.RETAIL_INVOICE, page.id());
    assertEquals(InvoiceViewState.NO_CUSTOMER, page.getViewState());
  }

  @Test
  @DisplayName("Lưu hóa đơn tạm xong phải đậu bản tạm và trả trang về hóa đơn mới")
  void luuBanTamXongTrangVeHoaDonMoi(@TempDir Path thuMuc) throws Exception {
    DongSanPham dong = new DongSanPham(
        "DSP-01", "Máy giặt", new DanhMucSanPham("DM-01", "Gia dụng", "Đồ gia dụng"),
        "Cái", 1, new BigDecimal("10"), new BigDecimal("9800000"), true,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
    SanPham may = new SanPham(
        "SGMW-2026-000001", new NhaCungCap("NCC-01", "Nhà cung cấp A"), dong,
        new BigDecimal("8000000"), LocalDate.of(2026, 1, 1), TrangThaiSanPham.SAN_SANG_BAN, 0, 0);
    BanHangController controller = new BanHangController(
        new FakeSanPhamDao(List.of(may)),
        new HoaDonTamFileDaoImpl(thuMuc, Clock.fixed(
            Instant.parse("2026-11-03T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))));
    KhachHangPicker picker =
        new KhachHangPicker(new KhachHangController(new FakeKhachHangDao(), dataSourceGia()));
    NhanVien nhanVien = nhanVienGia();

    AtomicReference<RetailInvoicePage> pageRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      RetailInvoicePage page = new RetailInvoicePage(nhanVien, controller, picker);
      pageRef.set(page);
      controller.themDong("DSP-01", 1);
      page.setCustomer(new RetailInvoicePage.CustomerInfo(
          "KH-2026-000003", "Trần Thị B", "0901234567", "12 Lê Lợi"));
      page.setCart(
          List.of(new RetailInvoicePage.CartItem(
              "DSP-01", "Máy giặt", 1, 9800000, "10%", 10780000, 1)),
          new InvoiceTotals(9800000, 980000, 10780000));
      truongForm(page, "phoneLookupField").setText("0901234567");
      truongForm(page, "productSearchField").setText("SGMW");
      truongForm(page, "cashField").setText("10780000");
    });

    RetailInvoicePage page = pageRef.get();
    assertEquals(InvoiceViewState.READY_TO_PAY, page.getViewState());

    SwingUtilities.invokeAndWait(() -> truongNut(page, "saveDraftBtn").doClick());

    assertEquals(InvoiceViewState.NO_CUSTOMER, page.getViewState());
    assertEquals("", truongForm(page, "phoneLookupField").getText());
    assertEquals("", truongForm(page, "productSearchField").getText());
    assertEquals("", truongForm(page, "cashField").getText());
    assertNull(controller.maTamHienTai());
    assertTrue(controller.hoaDonHienTai().dongHang().isEmpty());
    try (Stream<Path> tep = Files.list(thuMuc)) {
      assertEquals(
          List.of("HDT-2026-000001.properties"),
          tep.map(tepCon -> tepCon.getFileName().toString()).sorted().toList());
    }
  }

  @Test
  @DisplayName("Giỏ hàng hiển thị đúng dòng sản phẩm, số lượng và thành tiền do controller tính")
  void gioHienThiDongVaSoLuong() {
    DongSanPham dong = new DongSanPham(
        "DSP-2026-000001", "Máy giặt", new DanhMucSanPham("DM-01", "Gia dụng", "Đồ gia dụng"),
        "Cái", 5, new BigDecimal("10"), new BigDecimal("9800000"), true,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
    List<SanPham> danhSachMay = new java.util.ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      danhSachMay.add(new SanPham(
          String.format("SGMW-2026-%06d", i), new NhaCungCap("NCC-01", "Nhà cung cấp A"), dong,
          new BigDecimal("8000000"), LocalDate.of(2026, 1, 1),
          TrangThaiSanPham.SAN_SANG_BAN, 0, 0));
    }
    BanHangController controller = new BanHangController(
        new FakeSanPhamDao(danhSachMay), new FakeHoaDonTamDao());
    KhachHangPicker picker =
        new KhachHangPicker(new KhachHangController(new FakeKhachHangDao(), dataSourceGia()));

    RetailInvoicePage page = new RetailInvoicePage(nhanVienGia(), controller, picker);
    page.setCart(
        List.of(new RetailInvoicePage.CartItem(
            "DSP-2026-000001", "Máy giặt", 3, 9800000, "10%", 32340000, 5)),
        new InvoiceTotals(29400000, 2940000, 32340000));

    DefaultTableModel model = (DefaultTableModel) truong(page, "cartTableModel");

    assertEquals(1, model.getRowCount());
    assertEquals("DSP-2026-000001", model.getValueAt(0, 0));
    assertEquals(3, model.getValueAt(0, 2));
    assertEquals(32340000L, model.getValueAt(0, 5));
  }

  private static FormField truongForm(RetailInvoicePage page, String tenTruong) {
    return (FormField) truong(page, tenTruong);
  }

  private static JButton truongNut(RetailInvoicePage page, String tenTruong) {
    return (JButton) truong(page, tenTruong);
  }

  private static Object truong(RetailInvoicePage page, String tenTruong) {
    try {
      Field field = RetailInvoicePage.class.getDeclaredField(tenTruong);
      field.setAccessible(true);
      return field.get(page);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Không truy cập được trường " + tenTruong, e);
    }
  }

  private static NhanVien nhanVienGia() {
    return new NhanVien(
        "NVBANHANG-2026-001",
        "Nguyễn Văn A",
        "0901234567",
        "a@dtaq.vn",
        LocalDate.of(2026, 1, 1),
        "nva",
        "matkhau",
        LoaiQuyen.NHAN_VIEN_BAN_HANG,
        true);
  }

  private static DataSource dataSourceGia() {
    return new DataSource() {
      @Override
      public Connection getConnection() {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public Connection getConnection(String username, String password) {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public void setLogWriter(PrintWriter out) {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public void setLoginTimeout(int seconds) {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public int getLoginTimeout() {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public Logger getParentLogger() {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public <T> T unwrap(Class<T> iface) {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        throw new UnsupportedOperationException("Không dùng kết nối trong kiểm thử giao diện");
      }
    };
  }

  private static final class FakeSanPhamDao implements SanPhamDao {

    private final List<SanPham> danhSach;

    FakeSanPhamDao() {
      this(List.of());
    }

    FakeSanPhamDao(List<SanPham> danhSach) {
      this.danhSach = List.copyOf(danhSach);
    }

    @Override
    public void insert(Connection ketNoi, SanPham sanPham) {}

    @Override
    public Optional<SanPham> findById(String soSerial) {
      return danhSach.stream()
          .filter(sanPham -> sanPham.soSerial().equals(soSerial))
          .findFirst();
    }

    @Override
    public List<SanPham> findByDongSanPham(String maDongSP) {
      return danhSach.stream()
          .filter(sanPham -> sanPham.dongSanPham().maDongSP().equals(maDongSP))
          .toList();
    }

    @Override
    public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
      return danhSach.stream()
          .filter(sanPham -> sanPham.trangThai() == trangThai)
          .toList();
    }

    @Override
    public List<SanPham> findAll() {
      return danhSach;
    }

    @Override
    public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {}
  }

  private static final class FakeKhachHangDao implements KhachHangDao {
    @Override
    public Optional<KhachHang> findBySdt(String soDienThoai) {
      return Optional.empty();
    }

    @Override
    public List<KhachHang> findAll() {
      return List.of();
    }

    @Override
    public void insert(KhachHang khachHang) {}

    @Override
    public void insert(Connection ketNoi, KhachHang khachHang) {}

    @Override
    public void update(KhachHang khachHang) {}
  }

  private static final class FakeHoaDonTamDao implements HoaDonTamDao {
    @Override
    public HoaDonTam luu(HoaDonTam banTam) {
      return banTam;
    }

    @Override
    public List<HoaDonTam> findByNhanVien(String maNV) {
      return List.of();
    }

    @Override
    public Optional<HoaDonTam> findById(String maTam) {
      return Optional.empty();
    }

    @Override
    public void xoa(String maTam) {}
  }
}
