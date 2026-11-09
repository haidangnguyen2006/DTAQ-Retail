package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Nghiệp vụ khuyến mãi (UC012)")
class KhuyenMaiControllerTest {

  private static final LocalDate BAT_DAU = LocalDate.of(2026, 11, 10);
  private static final LocalDate KET_THUC = LocalDate.of(2026, 11, 20);
  private static final String MA_DONG = "DSP-2026-000001";

  private final FakeKhuyenMaiDao khuyenMaiDao = new FakeKhuyenMaiDao();
  private final FakeDongSanPhamDao dongSanPhamDao = new FakeDongSanPhamDao();
  private KhuyenMaiController controller;

  @BeforeEach
  void setUp() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    controller = new KhuyenMaiController(khuyenMaiDao, dongSanPhamDao, dataSourceSinhMa(0));
  }

  @AfterEach
  void dongPhien() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Nhân viên bán hàng không được thêm mới chương trình khuyến mãi")
  void khongPhaiQuanLyThiChanThemMoi() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));

    assertThrows(SecurityException.class, () -> them(BigDecimal.TEN));
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Nhân viên bán hàng vẫn tra cứu được khuyến mãi hiệu lực cho UC003")
  void nhanVienBanHangVanTraCuuHieuLuc() {
    KhuyenMai km = khuyenMai("KM-2026-000012", true, BAT_DAU, KET_THUC);
    khuyenMaiDao.hieuLuc = Optional.of(km);
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));

    assertEquals(Optional.of(km), controller.timKhuyenMaiHieuLuc(MA_DONG, BAT_DAU));
  }

  @Test
  @DisplayName("Thêm thành công, tự sinh mã và chuẩn hoá dữ liệu")
  void themMoi_thanhCong() {
    var ketQua = controller.themMoi("  Black Friday  ", "  Giảm giá  ",
        new BigDecimal("15"), BAT_DAU, KET_THUC, " " + MA_DONG + " ", true);

    assertTrue(ketQua.thanhCong());
    assertNotNull(ketQua.duLieu());
    assertTrue(ketQua.duLieu().maKM().matches("^KM-\\d{4}-\\d{6}$"));
    assertEquals("Black Friday", ketQua.duLieu().tenKM());
    assertEquals("Giảm giá", ketQua.duLieu().moTa());
    assertEquals(1, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Từ chối tỷ lệ giảm ngoài khoảng 0 đến 100")
  void themMoi_tyLeNgoaiKhoang() {
    assertFalse(them(new BigDecimal("-0.01")).thanhCong());
    assertFalse(them(new BigDecimal("100.01")).thanhCong());
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Từ chối ngày kết thúc trước ngày bắt đầu")
  void themMoi_ngayKetThucTruocNgayBatDau() {
    var ketQua = controller.themMoi("KM test", null, BigDecimal.TEN,
        KET_THUC, BAT_DAU, MA_DONG, false);
    assertFalse(ketQua.thanhCong());
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Từ chối khi dòng sản phẩm không tồn tại")
  void themMoi_dongSanPhamKhongTonTai() {
    dongSanPhamDao.tonTai = false;
    assertFalse(them(BigDecimal.TEN).thanhCong());
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Từ chối mã khuyến mãi bị trùng")
  void themMoi_maTrung() {
    controller = new KhuyenMaiController(khuyenMaiDao, dongSanPhamDao, dataSourceSinhMa(1));
    khuyenMaiDao.maTrung = true;
    var ketQua = them(BigDecimal.TEN);
    assertFalse(ketQua.thanhCong());
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Từ chối khoảng thời gian chồng lấn khi cả hai chương trình đang áp dụng")
  void themMoi_chongLan() {
    khuyenMaiDao.chongLan = true;
    assertFalse(them(BigDecimal.TEN).thanhCong());
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  @Test
  @DisplayName("Cho phép khoảng ngày trùng khi chương trình khác đã dừng")
  void themMoi_chuongTrinhKhacDaDung() {
    khuyenMaiDao.ds.add(khuyenMai("KM-2026-000020", false, BAT_DAU, KET_THUC));
    var ketQua = controller.themMoi("Khuyến mãi mới", null, BigDecimal.TEN,
        BAT_DAU, KET_THUC, MA_DONG, true);
    assertTrue(ketQua.thanhCong());
    assertEquals(1, khuyenMaiDao.soLanKiemTraChongLan);
  }

  @Test
  @DisplayName("Bật và dừng chỉ cập nhật cờ áp dụng")
  void batDungApDung() {
    KhuyenMai km = khuyenMai("KM-2026-000010", false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(10));
    khuyenMaiDao.ds.add(km);

    assertTrue(controller.batDauApDung(km.maKM()).thanhCong());
    assertEquals(Boolean.TRUE, khuyenMaiDao.trangThaiCapNhat);
    assertTrue(controller.dungApDung(km.maKM()).thanhCong());
    assertEquals(Boolean.FALSE, khuyenMaiDao.trangThaiCapNhat);
  }

  @Test
  @DisplayName("Tra cứu khuyến mãi hiệu lực tại cả hai biên ngày")
  void timKhuyenMaiHieuLuc_bienNgay() {
    KhuyenMai km = khuyenMai("KM-2026-000011", true, BAT_DAU, KET_THUC);
    khuyenMaiDao.hieuLuc = Optional.of(km);

    assertEquals(khuyenMaiDao.hieuLuc, controller.timKhuyenMaiHieuLuc(MA_DONG, BAT_DAU));
    assertEquals(khuyenMaiDao.hieuLuc, controller.timKhuyenMaiHieuLuc(MA_DONG, KET_THUC));
    assertEquals(2, khuyenMaiDao.soLanTimHieuLuc);
  }

  private KhuyenMaiController.KetQua<KhuyenMai> them(BigDecimal tyLe) {
    return controller.themMoi("Khuyến mãi test", null, tyLe, BAT_DAU, KET_THUC, MA_DONG, true);
  }

  private static NhanVien nhanVien(LoaiQuyen loaiQuyen) {
    return new NhanVien(
        "NVQUANLY-TEST-001",
        "Nguyễn Quản Lý",
        "0900000001",
        "quanly.test@dtaq.local",
        LocalDate.of(2026, 1, 1),
        "quanly.test",
        "matkhau-test",
        loaiQuyen,
        true);
  }

  private static KhuyenMai khuyenMai(String ma, boolean dangApDung, LocalDate tuNgay, LocalDate denNgay) {
    return new KhuyenMai(ma, "Khuyến mãi", tuNgay, denNgay, BigDecimal.TEN, null, dangApDung, dongSanPham());
  }

  private static DongSanPham dongSanPham() {
    return new DongSanPham(MA_DONG, "Tủ lạnh", new DanhMucSanPham("DM", "Điện lạnh", null),
        "cái", 0, BigDecimal.ZERO, BigDecimal.ZERO, true, null, null);
  }

  /** JDBC proxy nhỏ chỉ phục vụ MAX mã trong MaGenerator; không phụ thuộc PostgreSQL. */
  private static DataSource dataSourceSinhMa(long maxMa) {
    ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
        new Class<?>[] {ResultSet.class}, (proxy, method, args) -> switch (method.getName()) {
          case "next" -> true;
          case "getLong" -> maxMa;
          case "close" -> null;
          default -> defaultValue(method.getReturnType());
        });
    PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
        PreparedStatement.class.getClassLoader(), new Class<?>[] {PreparedStatement.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "executeQuery" -> resultSet;
          case "close", "setInt", "setString" -> null;
          default -> defaultValue(method.getReturnType());
        });
    Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
        new Class<?>[] {Connection.class}, (proxy, method, args) -> switch (method.getName()) {
          case "prepareStatement" -> statement;
          case "close" -> null;
          default -> defaultValue(method.getReturnType());
        });
    return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
        new Class<?>[] {DataSource.class}, (proxy, method, args) ->
            method.getName().equals("getConnection") ? connection : defaultValue(method.getReturnType()));
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == double.class) return 0D;
    if (type == float.class) return 0F;
    if (type == short.class) return (short) 0;
    if (type == byte.class) return (byte) 0;
    if (type == char.class) return '\0';
    return null;
  }

  private static final class FakeDongSanPhamDao implements DongSanPhamDao {
    boolean tonTai = true;

    @Override public Optional<DongSanPham> findById(String maDongSP) {
      return tonTai ? Optional.of(dongSanPham()) : Optional.empty();
    }
    @Override public void insert(java.sql.Connection ketNoi, DongSanPham dongSanPham) {}
    @Override public List<DongSanPham> findAll() { return List.of(); }
    @Override public List<DongSanPham> findByDanhMuc(String maDanhMuc) { return List.of(); }
    @Override public void updateSoLuongTon(String maDongSP, int soLuongTon) {}
    @Override public void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh) {}
  }

  private static final class FakeKhuyenMaiDao implements KhuyenMaiDao {
    final List<KhuyenMai> ds = new ArrayList<>();
    Optional<KhuyenMai> hieuLuc = Optional.empty();
    boolean maTrung;
    boolean chongLan;
    int soLanInsert;
    int soLanKiemTraChongLan;
    int soLanTimHieuLuc;
    Boolean trangThaiCapNhat;

    @Override public Optional<KhuyenMai> findHieuLuc(String maDongSP, LocalDate ngay) {
      soLanTimHieuLuc++;
      return hieuLuc.filter(km -> !ngay.isBefore(km.ngayBatDau()) && !ngay.isAfter(km.ngayKetThuc()));
    }
    @Override public Optional<KhuyenMai> findById(String maKM) {
      return ds.stream().filter(km -> km.maKM().equals(maKM)).findFirst();
    }
    @Override public List<KhuyenMai> findAll() { return List.copyOf(ds); }
    @Override public List<KhuyenMai> findByDongSanPham(String maDongSP) { return List.of(); }
    @Override public void insert(KhuyenMai khuyenMai) { soLanInsert++; ds.add(khuyenMai); }
    @Override public void update(KhuyenMai khuyenMai) {}
    @Override public void updateTrangThaiApDung(String maKM, boolean dangApDung) {
      trangThaiCapNhat = dangApDung;
      ds.replaceAll(km -> km.maKM().equals(maKM)
          ? new KhuyenMai(km.maKM(), km.tenKM(), km.ngayBatDau(), km.ngayKetThuc(),
              km.tyLeGiam(), km.moTa(), dangApDung, km.dongSanPham())
          : km);
    }
    @Override public boolean existsTrungMa(String maKM) { return maTrung; }
    @Override public boolean existsChongLan(String maDongSP, LocalDate ngayBatDau,
        LocalDate ngayKetThuc, String excludeMaKM) {
      soLanKiemTraChongLan++;
      return chongLan;
    }
  }
}
