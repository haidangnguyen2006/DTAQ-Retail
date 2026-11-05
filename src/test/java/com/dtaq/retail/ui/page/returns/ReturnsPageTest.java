package com.dtaq.retail.ui.page.returns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ReturnConfig;
import com.dtaq.retail.controller.DoiTraController;
import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.PhieuDoiDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.shell.PageId;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử màn hình xử lý đổi trả (UC006) với bộ điều khiển thật và DAO giả thủ công.
 *
 * <p>Không mở hộp thoại modal thật và không kết nối cơ sở dữ liệu; mọi trạng thái giao diện được
 * kiểm tra qua model, nhãn và trạng thái của {@link StatefulView}.
 */
class ReturnsPageTest {

  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Clock DONG_HO =
      Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), MUI_GIO);

  private static final String MA_HD = "HD-2026-000001";
  private static final String SERIAL = "SAM-TV-0041";
  /** Máy thay thế rẻ hơn máy trả (2.200.000 < 3.500.000). */
  private static final String SERIAL_MAY_RE = "SAM-TV-0050";
  /** Máy thay thế đắt hơn máy trả (5.500.000 > 3.500.000). */
  private static final String SERIAL_MAY_DAT = "SAM-TV-0051";
  private static final LocalDate NGAY_LAP = LocalDate.of(2026, 10, 20);

  private static final NhanVien BAN_HANG =
      nhanVien("NVBANHANG-2026-001", LoaiQuyen.NHAN_VIEN_BAN_HANG);
  private static final KhachHang KHACH_HANG =
      new KhachHang("KH-2026-000001", "Nguyễn Văn An", "0902100001", "Quận 1");

  private FakePhieuTraDao phieuTraDao;
  private FakePhieuDoiDao phieuDoiDao;
  private FakeSanPhamLoiDao sanPhamLoiDao;
  private FakeHoaDonDao hoaDonDao;
  private FakeChiTietHoaDonDao chiTietHoaDonDao;
  private FakeSanPhamDao sanPhamDao;
  private FakeDongSanPhamDao dongSanPhamDao;

  private ReturnsPage page;

  @BeforeEach
  void setUp() throws Exception {
    CurrentSession.set(BAN_HANG);

    phieuTraDao = new FakePhieuTraDao();
    phieuDoiDao = new FakePhieuDoiDao();
    sanPhamLoiDao = new FakeSanPhamLoiDao();
    hoaDonDao = new FakeHoaDonDao();
    chiTietHoaDonDao = new FakeChiTietHoaDonDao();
    sanPhamDao = new FakeSanPhamDao();
    dongSanPhamDao = new FakeDongSanPhamDao();

    DongSanPham dong = new DongSanPham(
        "DSP-2026-000001", "Tivi Samsung Crystal UHD 55 inch", null, "Chiếc", 3,
        BigDecimal.TEN, new BigDecimal("10000000"), true, NGAY_LAP.minusDays(30), NGAY_LAP);
    dongSanPhamDao.them(dong);

    SanPham may = new SanPham(
        SERIAL, null, dong, BigDecimal.ZERO, NGAY_LAP, TrangThaiSanPham.DA_BAN, 0, 0);
    sanPhamDao.them(may);

    DongSanPham dongRe = new DongSanPham(
        "DSP-2026-000002", "Tivi giá thấp", null, "Chiếc", 2,
        BigDecimal.TEN, new BigDecimal("2000000"), true, NGAY_LAP.minusDays(10), NGAY_LAP);
    dongSanPhamDao.them(dongRe);
    sanPhamDao.them(new SanPham(
        SERIAL_MAY_RE, null, dongRe, BigDecimal.ZERO, NGAY_LAP,
        TrangThaiSanPham.SAN_SANG_BAN, 0, 0));

    DongSanPham dongDat = new DongSanPham(
        "DSP-2026-000003", "Tivi cao cấp", null, "Chiếc", 2,
        BigDecimal.TEN, new BigDecimal("5000000"), true, NGAY_LAP.minusDays(10), NGAY_LAP);
    dongSanPhamDao.them(dongDat);
    sanPhamDao.them(new SanPham(
        SERIAL_MAY_DAT, null, dongDat, BigDecimal.ZERO, NGAY_LAP,
        TrangThaiSanPham.SAN_SANG_BAN, 0, 0));

    HoaDon hoaDon = new HoaDon(MA_HD, NGAY_LAP, BAN_HANG, KHACH_HANG, true);
    hoaDonDao.them(hoaDon);
    chiTietHoaDonDao.ds.add(new ChiTietHoaDon(
        hoaDon,
        may,
        1,
        new BigDecimal("10000000"),
        BigDecimal.TEN,
        null,
        null,
        BigDecimal.ZERO,
        new BigDecimal("10000000"),
        new BigDecimal("3000000"),
        new BigDecimal("500000")));

    page = taoPage();
  }

  @AfterEach
  void tearDown() {
    CurrentSession.clear();
    System.clearProperty(ReturnConfig.KEY_SO_NGAY_DOI_TRA);
    ReturnConfig.setPropertiesFileForTest(null);
  }

  @Test
  @DisplayName("Trang có đúng định danh và hiển thị trạng thái rỗng khi chưa tra cứu")
  void hienThiTrangThaiRongKhiChuaTraCuu() throws Exception {
    assertEquals(PageId.RETURNS, page.id());
    assertEquals("Xử lý đổi trả sản phẩm", page.title());
    assertEquals("Bán hàng", page.section());
    assertTrue(page.scrollable());
    assertNotNull(page.view());

    assertEquals(StatefulView.State.EMPTY, mayState(page).getState());
    assertEquals(0, mayModel(page).getRowCount());

    SwingUtilities.invokeAndWait(page::onShow);
    choDieuKien(() -> phieuState(page).getState() == StatefulView.State.EMPTY);
    assertEquals(StatefulView.State.EMPTY, phieuState(page).getState());
  }

  @Test
  @DisplayName("Sau khi tra cứu, trang hiển thị khách hàng, số điện thoại và ngày lập")
  void hienThiSoDienThoaiVaNgayLapSauTraCuu() throws Exception {
    SwingUtilities.invokeAndWait(() -> page.traCuuHoaDon(MA_HD));
    choDieuKien(() -> mayState(page).getState() == StatefulView.State.CONTENT);

    assertEquals("Nguyễn Văn An", nhan(page, "tenKhachHangLabel").getText());
    assertEquals("0902100001", nhan(page, "soDienThoaiLabel").getText());
    assertEquals("20/10/2026", nhan(page, "ngayLapLabel").getText());
    assertEquals(1, mayModel(page).getRowCount());
    assertEquals(SERIAL, mayModel(page).getValueAt(0, 0));
    assertEquals("Tivi Samsung Crystal UHD 55 inch", mayModel(page).getValueAt(0, 1));
    assertEquals(3500000L, mayModel(page).getValueAt(0, 2));
  }

  @Test
  @DisplayName("Hoàn tiền từ 3.000.000 VND trở lên hiện cảnh báo cần Quản lý phê duyệt")
  void hienCanhBaoNguongKhiHoanTienLon() throws Exception {
    SwingUtilities.invokeAndWait(() -> page.traCuuHoaDon(MA_HD));
    choDieuKien(() -> mayState(page).getState() == StatefulView.State.CONTENT);

    SwingUtilities.invokeAndWait(() -> lyDo(page).setText("Khách đổi ý, máy nguyên vẹn"));
    SwingUtilities.invokeAndWait(() -> page.chonMay(0));
    choDieuKien(() -> canhBaoNguong(page).isVisible());

    assertTrue(canhBaoNguong(page).isVisible());
    assertFalse(loiTinh(page).isVisible());
    assertEquals("3.500.000 VNĐ", nhan(page, "soTienHoanLabel").getText());
  }

  @Test
  @DisplayName("Tra cứu thất bại hiện lỗi và không có máy nào để chọn")
  void hienLoiKhiTraCuuThatBai() throws Exception {
    SwingUtilities.invokeAndWait(() -> page.traCuuHoaDon("HD-2026-999999"));
    choDieuKien(() -> loiTraCuu(page).isVisible());

    assertTrue(loiTraCuu(page).isVisible());
    assertTrue(noiDung(loiTraCuu(page)).contains("Không tìm thấy hóa đơn"));
    assertEquals(StatefulView.State.EMPTY, mayState(page).getState());
    assertEquals(0, mayModel(page).getRowCount());
  }

  @Test
  @DisplayName("Ô nhập serial máy thay thế ẩn khi trả hàng và hiện khi chọn đổi máy")
  void anOFNhapSerialMayThayTheKhiChonTraHang() throws Exception {
    JPanel serialPanel = (JPanel) truong(page, "serialPanel");
    assertFalse(serialPanel.isVisible());

    SegmentedControl hinhThuc = (SegmentedControl) truong(page, "hinhThuc");
    SwingUtilities.invokeAndWait(() -> hinhThuc.select(1));
    assertTrue(serialPanel.isVisible(), "Chọn Đổi máy phải hiện ô serial máy thay thế");

    SwingUtilities.invokeAndWait(() -> hinhThuc.select(0));
    assertFalse(serialPanel.isVisible(), "Chọn Trả hàng phải ẩn ô serial máy thay thế");
  }

  @Test
  @DisplayName("Chọn Đổi máy mà thiếu serial thì bị chặn và không ghi phiếu")
  void doiMayThieuSerialBiChan() throws Exception {
    SwingUtilities.invokeAndWait(() -> page.traCuuHoaDon(MA_HD));
    choDieuKien(() -> mayState(page).getState() == StatefulView.State.CONTENT);
    SwingUtilities.invokeAndWait(() -> lyDo(page).setText("Khách đổi sang mẫu khác"));
    SwingUtilities.invokeAndWait(() -> page.chonMay(0));
    choDieuKien(() -> hoanTatButton(page).isEnabled());

    SegmentedControl hinhThuc = (SegmentedControl) truong(page, "hinhThuc");
    SwingUtilities.invokeAndWait(() -> hinhThuc.select(1));

    FormField serialField = serialField(page);
    JLabel loiSerial = (JLabel) truong(serialField, "messageLabel");
    assertTrue(loiSerial.isVisible(), "Ô serial phải hiện lỗi");
    assertEquals("Vui lòng nhập serial máy thay thế", loiSerial.getText());
    assertFalse(hoanTatButton(page).isEnabled(), "Nút hoàn tất phải bị khoá");
    assertFalse(((JPanel) truong(page, "rowMayMoi")).isVisible(),
        "Không được hiện dòng Giá trị máy mới khi thiếu serial");

    SwingUtilities.invokeAndWait(() -> hoanTatButton(page).doClick());
    assertEquals(0, phieuTraDao.soLanTaoPhieu,
        "Không được gọi ghi phiếu khi yêu cầu đổi máy thiếu serial");
  }

  @Test
  @DisplayName("Ghi phiếu thất bại làm mới khối tính tiền và hiện callout lỗi")
  void ghiThatBaiLamMoiKhoiTinhTien() throws Exception {
    SwingUtilities.invokeAndWait(() -> page.traCuuHoaDon(MA_HD));
    choDieuKien(() -> mayState(page).getState() == StatefulView.State.CONTENT);
    SwingUtilities.invokeAndWait(() -> lyDo(page).setText("Khách đổi ý, máy nguyên vẹn"));
    SwingUtilities.invokeAndWait(() -> page.chonMay(0));
    choDieuKien(() -> canhBaoNguong(page).isVisible());

    phieuTraDao.loiKhiTaoPhieu =
        new IllegalStateException("Máy " + SERIAL + " đã được đổi/trả trước đó");
    DoiTraController.YeuCauDoiTra yeuCau = new DoiTraController.YeuCauDoiTra(
        MA_HD, SERIAL, TinhTrangHangTra.NGUYEN_VEN, "Khách đổi ý, máy nguyên vẹn", null);
    SwingUtilities.invokeAndWait(() -> page.guiPhieu(yeuCau));
    choDieuKien(() -> loiTinh(page).isVisible());

    assertTrue(loiTinh(page).isVisible());
    assertTrue(noiDung(loiTinh(page)).contains("đã được đổi/trả trước đó"));
    assertEquals("—", nhan(page, "soTienHoanLabel").getText(),
        "Số liệu cũ phải được làm mới");
    assertFalse(canhBaoNguong(page).isVisible(), "Cảnh báo ngưỡng cũ phải được ẩn");
    assertTrue(hoanTatButton(page).isEnabled(), "Nút phải được bật lại sau khi báo lỗi");
    assertEquals(1, phieuTraDao.soLanTaoPhieu);
  }

  @Test
  @DisplayName("Chế độ đổi máy không hiện dòng Số tiền hoàn")
  void doiMayKhongHienSoTienHoan() throws Exception {
    chuanBiDoiMay(SERIAL_MAY_RE);
    choDieuKien(() -> hoanTatButton(page).isEnabled());

    JPanel rowSoTienHoan = (JPanel) truong(page, "rowSoTienHoan");
    JPanel rowChenhLech = (JPanel) truong(page, "rowChenhLech");
    assertFalse(rowSoTienHoan.isVisible(), "Đổi hàng không hiển thị Số tiền hoàn");
    assertTrue(rowChenhLech.isVisible());
    assertEquals("Chênh lệch (không hoàn)",
        ((JLabel) rowChenhLech.getComponent(0)).getText());
    assertEquals("-1.300.000 VNĐ", nhan(page, "chenhLechLabel").getText(),
        "Chênh lệch âm phải giữ đúng dấu, không chuyển thành tiền hoàn");
    assertFalse(canhBaoNguong(page).isVisible(),
        "Ngưỡng 3.000.000 chỉ áp cho phiếu trả");
    assertTrue(hoanTatButton(page).isEnabled());
  }

  @Test
  @DisplayName("Đổi sang máy đắt hơn bị chặn, không ghi phiếu")
  void doiMayGiaTriCaoHonBiChan() throws Exception {
    chuanBiDoiMay(SERIAL_MAY_DAT);
    choDieuKien(() -> loiTinh(page).isVisible());

    assertTrue(noiDung(loiTinh(page)).contains("ngang bằng hoặc thấp hơn"));
    assertFalse(hoanTatButton(page).isEnabled(), "Nút hoàn tất phải bị khoá");
    assertFalse(((JPanel) truong(page, "rowSoTienHoan")).isVisible());

    SwingUtilities.invokeAndWait(() -> hoanTatButton(page).doClick());
    assertEquals(0, phieuDoiDao.soLanTaoPhieuDoi,
        "Không được gọi ghi phiếu đổi khi máy đắt hơn");
  }

  @Test
  @DisplayName("Xác nhận đổi hàng nêu rõ không hoàn tiền")
  void xacNhanDoiHangKhongHoanTien() {
    DoiTraController.KetQuaTinh doiMay = new DoiTraController.KetQuaTinh(
        true, "Đủ điều kiện đổi máy", new BigDecimal("3500000"), new BigDecimal("2200000"),
        new BigDecimal("-1300000"), BigDecimal.ZERO, false);

    assertEquals("Xác nhận đổi hàng", ReturnsPage.tieuDeXacNhan(true, doiMay));
    String noiDungDoi = ReturnsPage.noiDungXacNhan(true, doiMay);
    assertTrue(noiDungDoi.contains("không hoàn"));
    assertTrue(noiDungDoi.contains("không được hoàn lại"));
    assertFalse(noiDungDoi.contains("thu thêm"),
        "D17 không còn nhánh thu thêm tiền");

    DoiTraController.KetQuaTinh traHang = new DoiTraController.KetQuaTinh(
        true, "", new BigDecimal("3500000"), BigDecimal.ZERO, new BigDecimal("-3500000"),
        new BigDecimal("3500000"), true);
    assertEquals("Xác nhận hoàn tiền", ReturnsPage.tieuDeXacNhan(false, traHang));
  }

  @Test
  @DisplayName("Danh sách phiếu chờ hoàn tất hiển thị và sắp theo ngày tạo")
  void hienDanhSachPhieuChoHoanTat() throws Exception {
    phieuDoiDao.choHoanTat.add(phieuDuyet(
        PhieuDuyet.LoaiPhieu.PHIEU_DOI, "PD-2026-000009", LocalDate.of(2026, 3, 12),
        new BigDecimal("2000000")));
    phieuTraDao.choHoanTat.add(phieuDuyet(
        PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000010", LocalDate.of(2026, 3, 13),
        new BigDecimal("3500000")));

    SwingUtilities.invokeAndWait(page::onShow);
    choDieuKien(() -> phieuState(page).getState() == StatefulView.State.CONTENT);

    DefaultTableModel model = phieuModel(page);
    assertEquals(2, model.getRowCount());
    assertEquals("PD-2026-000009", model.getValueAt(0, 0));
    assertEquals(2000000L, model.getValueAt(0, 4));
    assertEquals("PT-2026-000010", model.getValueAt(1, 0));
    assertEquals("13/03/2026", model.getValueAt(1, 5));
  }

  private ReturnsPage taoPage() throws Exception {
    DoiTraController controller = new DoiTraController(
        phieuTraDao,
        phieuDoiDao,
        sanPhamLoiDao,
        hoaDonDao,
        chiTietHoaDonDao,
        sanPhamDao,
        dongSanPhamDao,
        new FakeDataSource(),
        60,
        DONG_HO);
    AtomicReference<ReturnsPage> ref = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> ref.set(new ReturnsPage(controller)));
    return ref.get();
  }

  private static PhieuDuyet phieuDuyet(
      PhieuDuyet.LoaiPhieu loai, String maPhieu, LocalDate ngayTao, BigDecimal hoanDuKien) {
    return new PhieuDuyet(loai, maPhieu, MA_HD, "Nguyễn Văn An", "0902100001",
        hoanDuKien, "Trần Văn Duy", "Khách đổi ý", ngayTao);
  }

  private static NhanVien nhanVien(String maNV, LoaiQuyen loaiQuyen) {
    return new NhanVien(maNV, "Nhân viên " + maNV, "0900000001", maNV + "@dtaq.example",
        LocalDate.of(2026, 1, 1), maNV, "matkhau", loaiQuyen, true);
  }

  private static DefaultTableModel mayModel(ReturnsPage page) {
    return (DefaultTableModel) truong(page, "mayModel");
  }

  private static StatefulView mayState(ReturnsPage page) {
    return (StatefulView) truong(page, "mayState");
  }

  private static DefaultTableModel phieuModel(ReturnsPage page) {
    return (DefaultTableModel) truong(page, "phieuModel");
  }

  private static StatefulView phieuState(ReturnsPage page) {
    return (StatefulView) truong(page, "phieuState");
  }

  private static JLabel nhan(ReturnsPage page, String tenTruong) {
    return (JLabel) truong(page, tenTruong);
  }

  private static FormField lyDo(ReturnsPage page) {
    return (FormField) truong(page, "lyDoField");
  }

  private static FormField serialField(ReturnsPage page) {
    return (FormField) truong(page, "serialThayTheField");
  }

  private static DtaqButton hoanTatButton(ReturnsPage page) {
    return (DtaqButton) truong(page, "hoanTatButton");
  }

  private void chuanBiDoiMay(String serialMoi) throws Exception {
    SwingUtilities.invokeAndWait(() -> page.traCuuHoaDon(MA_HD));
    choDieuKien(() -> mayState(page).getState() == StatefulView.State.CONTENT);
    SwingUtilities.invokeAndWait(() -> lyDo(page).setText("Khách đổi sang mẫu khác"));
    SwingUtilities.invokeAndWait(() -> page.chonMay(0));
    choDieuKien(() -> hoanTatButton(page).isEnabled());
    SegmentedControl hinhThuc = (SegmentedControl) truong(page, "hinhThuc");
    SwingUtilities.invokeAndWait(() -> hinhThuc.select(1));
    SwingUtilities.invokeAndWait(() -> serialField(page).setText(serialMoi));
  }

  private static InfoCallout canhBaoNguong(ReturnsPage page) {
    return (InfoCallout) truong(page, "canhBaoNguong");
  }

  private static InfoCallout loiTinh(ReturnsPage page) {
    return (InfoCallout) truong(page, "loiTinh");
  }

  private static InfoCallout loiTraCuu(ReturnsPage page) {
    return (InfoCallout) truong(page, "loiTraCuu");
  }

  private static String noiDung(InfoCallout callout) {
    try {
      Field field = InfoCallout.class.getDeclaredField("textLabel");
      field.setAccessible(true);
      return ((JLabel) field.get(callout)).getText();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Không đọc được nội dung InfoCallout", e);
    }
  }

  private static Object truong(Object page, String tenTruong) {
    try {
      Field field = page.getClass().getDeclaredField(tenTruong);
      field.setAccessible(true);
      return field.get(page);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Không truy cập được trường " + tenTruong, e);
    }
  }

  private static void choDieuKien(Supplier<Boolean> dieuKien) throws Exception {
    long han = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    AtomicReference<Boolean> dat = new AtomicReference<>(false);
    while (System.nanoTime() < han) {
      SwingUtilities.invokeAndWait(() -> dat.set(dieuKien.get()));
      if (Boolean.TRUE.equals(dat.get())) {
        return;
      }
      LockSupport.parkNanos(2_000_000L);
    }
    SwingUtilities.invokeAndWait(() -> dat.set(dieuKien.get()));
    assertTrue(Boolean.TRUE.equals(dat.get()), "Hết thời gian chờ trạng thái giao diện");
  }

  /** DAO hóa đơn giả trả về hóa đơn đã nạp. */
  private static final class FakeHoaDonDao implements HoaDonDao {

    private final Map<String, HoaDon> theoMa = new HashMap<>();

    private void them(HoaDon hoaDon) {
      theoMa.put(hoaDon.maHD(), hoaDon);
    }

    @Override
    public Optional<HoaDon> findById(String maHD) {
      return Optional.ofNullable(theoMa.get(maHD));
    }

    @Override
    public void insert(Connection ketNoi, HoaDon hoaDon) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<HoaDon> findDaThanhToanTrongKy(LocalDate tuNgay, LocalDate denNgay) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** DAO chi tiết hóa đơn giả trả về danh sách đã nạp. */
  private static final class FakeChiTietHoaDonDao implements ChiTietHoaDonDao {

    private final List<ChiTietHoaDon> ds = new ArrayList<>();

    @Override
    public List<ChiTietHoaDon> findByHoaDon(String maHD) {
      return ds.stream().filter(dong -> maHD.equals(dong.hoaDon().maHD())).toList();
    }

    @Override
    public void insert(Connection ketNoi, ChiTietHoaDon chiTiet) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** DAO sản phẩm giả trả về máy đã nạp. */
  private static final class FakeSanPhamDao implements SanPhamDao {

    private final Map<String, SanPham> theoSerial = new HashMap<>();

    private void them(SanPham sanPham) {
      theoSerial.put(sanPham.soSerial(), sanPham);
    }

    @Override
    public Optional<SanPham> findById(String soSerial) {
      return Optional.ofNullable(theoSerial.get(soSerial));
    }

    @Override
    public void insert(Connection ketNoi, SanPham sanPham) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<SanPham> findByDongSanPham(String maDongSP) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<SanPham> findAll() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** DAO dòng sản phẩm giả trả về dòng đã nạp. */
  private static final class FakeDongSanPhamDao implements DongSanPhamDao {

    private final Map<String, DongSanPham> theoMa = new HashMap<>();

    private void them(DongSanPham dongSanPham) {
      theoMa.put(dongSanPham.maDongSP(), dongSanPham);
    }

    @Override
    public Optional<DongSanPham> findById(String maDongSP) {
      return Optional.ofNullable(theoMa.get(maDongSP));
    }

    @Override
    public void insert(Connection ketNoi, DongSanPham dongSanPham) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<DongSanPham> findAll() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<DongSanPham> findByDanhMuc(String maDanhMuc) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public void updateSoLuongTon(String maDongSP, int soLuongTon) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** DAO phiếu trả giả chỉ hỗ trợ đọc danh sách chờ hoàn tất. */
  private static final class FakePhieuTraDao implements PhieuTraDao {

    private final List<PhieuDuyet> choHoanTat = new ArrayList<>();
    private RuntimeException loiKhiTaoPhieu;
    private int soLanTaoPhieu;

    @Override
    public List<PhieuDuyet> findChoHoanTat() {
      return choHoanTat;
    }

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
      return Set.of();
    }

    @Override
    public Optional<PhieuTra> findById(String maPhieuTra) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<ChiTietPhieuTra> findChiTiet(String maPhieuTra) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public String taoPhieuTra(
        Connection ketNoi, PhieuTra phieuTra, List<ChiTietPhieuTra> chiTiet) {
      soLanTaoPhieu++;
      if (loiKhiTaoPhieu != null) {
        throw loiKhiTaoPhieu;
      }
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuTra> chiTiet) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public boolean capNhatHoanTat(
        Connection ketNoi, String maPhieuTra, LocalDate ngayHoanTat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** DAO phiếu đổi giả chỉ hỗ trợ đọc danh sách chờ hoàn tất. */
  private static final class FakePhieuDoiDao implements PhieuDoiDao {

    private final List<PhieuDuyet> choHoanTat = new ArrayList<>();
    private int soLanTaoPhieuDoi;

    @Override
    public List<PhieuDuyet> findChoHoanTat() {
      return choHoanTat;
    }

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
      return Set.of();
    }

    @Override
    public Optional<PhieuDoi> findById(String maPhieuDoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<ChiTietPhieuDoi> findChiTiet(String maPhieuDoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public String taoPhieuDoi(
        Connection ketNoi, PhieuDoi phieuDoi, List<ChiTietPhieuDoi> chiTiet) {
      soLanTaoPhieuDoi++;
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public int danhDauMayThayThe(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public boolean capNhatHoanTat(
        Connection ketNoi, String maPhieuDoi, LocalDate ngayHoanTat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** DAO sản phẩm lỗi giả: màn hình đổi/trả chỉ đọc nên mọi thao tác ghi đều bị chặn. */
  private static final class FakeSanPhamLoiDao implements SanPhamLoiDao {

    @Override
    public void insert(Connection ketNoi, SanPhamLoi sanPhamLoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public void insert(SanPhamLoi sanPhamLoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public Optional<SanPhamLoi> findById(String maLoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public Optional<SanPhamLoi> findDangXuLy(String soSerial) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<SanPhamLoi> findAll() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public List<SanPhamLoi> findByTinhTrang(TrangThaiXuLyLoi tinhTrangXuLy) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public void updateTinhTrang(
        Connection ketNoi,
        String maLoi,
        TrangThaiXuLyLoi tinhTrangMoi,
        String ghiChuXuLy,
        LocalDate ngayCapNhat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }

    @Override
    public void updateTinhTrang(
        String maLoi,
        TrangThaiXuLyLoi tinhTrangMoi,
        String ghiChuXuLy,
        LocalDate ngayCapNhat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử giao diện");
    }
  }

  /** Nguồn dữ liệu giả cấp Connection tối thiểu cho transaction của controller. */
  private static final class FakeDataSource implements DataSource {

    private final Connection ketNoi = (Connection) Proxy.newProxyInstance(
        FakeDataSource.class.getClassLoader(),
        new Class<?>[] {Connection.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getAutoCommit" -> true;
          case "setAutoCommit", "commit", "rollback", "close" -> null;
          case "isClosed" -> false;
          case "toString" -> "fake-connection";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> null;
        });

    @Override
    public Connection getConnection() {
      return ketNoi;
    }

    @Override
    public Connection getConnection(String username, String password) {
      return ketNoi;
    }

    @Override
    public PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public void setLoginTimeout(int seconds) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException("Không hỗ trợ unwrap");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
