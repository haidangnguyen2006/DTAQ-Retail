package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.service.KhuyenMaiExcel;
import com.dtaq.retail.util.ExcelImporter;
import com.dtaq.retail.util.ExcelImporter.ImportResult;
import com.dtaq.retail.util.ExcelImporter.ImportRow;
import java.io.File;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private int soLanCommit;
  private int soLanRollback;

  @BeforeEach
  void setUp() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    DanhMucSanPhamDao danhMucDao = (DanhMucSanPhamDao) Proxy.newProxyInstance(
        DanhMucSanPhamDao.class.getClassLoader(), new Class<?>[] {DanhMucSanPhamDao.class},
        (proxy, method, args) -> {
          if (method.getName().equals("findById")) {
            String ma = (String) args[0];
            return List.of("DM", "DM2").contains(ma)
                ? Optional.of(new DanhMucSanPham(ma, ma, null)) : Optional.empty();
          }
          throw new UnsupportedOperationException(method.getName());
        });
    controller = new KhuyenMaiController(khuyenMaiDao, dongSanPhamDao, danhMucDao, dataSourceSinhMa(0));
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
  void themMoiTheoHaiDanhMucLuuMotChuongTrinhVaHaiChiTiet() {
    khuyenMaiDao.dongApDung = List.of(dongSanPham(), dongSanPhamThuHai());
    var ketQua = themTheoHaiDanhMuc();

    assertTrue(ketQua.thanhCong());
    assertEquals(List.of("DM", "DM2"), khuyenMaiDao.danhMucDaLoc);
    assertEquals(LocalDate.now().minusDays(30), khuyenMaiDao.ngayGioiHan);
    assertEquals(1, khuyenMaiDao.soLanInsert);
    assertEquals(2, ketQua.duLieu().chiTiet().size());
  }

  @Test
  void xungDotDongThuHaiChanCaChuongTrinh() {
    khuyenMaiDao.dongApDung = List.of(dongSanPham(), dongSanPhamThuHai());
    khuyenMaiDao.dongChongLan = dongSanPhamThuHai().maDongSP();

    assertFalse(themTheoHaiDanhMuc().thanhCong());
    assertEquals(2, khuyenMaiDao.soLanKiemTraChongLan);
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  private KhuyenMaiController.KetQua<KhuyenMai> themTheoHaiDanhMuc() {
    return controller.themMoiTheoDieuKien("Khuyến mãi", null, BigDecimal.TEN, BAT_DAU,
        KET_THUC, null, List.of("DM", "DM2"), 30, true);
  }

  private static DongSanPham dongSanPhamThuHai() {
    return new DongSanPham("DSP-2026-000002", "Quạt", new DanhMucSanPham("DM2", "Gia dụng", null),
        "cái", 0, BigDecimal.ZERO, BigDecimal.ZERO, true, null, null);
  }

  @Test
  void mauExcelTrongRepoDocDuocVaGomNhieuDong() {
    var tep = ExcelImporter.doc(
        new File("docs/reference/templates/mau_nhap_khuyen_mai.xlsx"), KhuyenMaiExcel.MAU);
    var loi = new ArrayList<ExcelImporter.ImportError>();
    assertTrue(tep.hopLe(), () -> tep.loi().toString());
    var nhom = KhuyenMaiExcel.gomNhom(tep, loi);
    assertTrue(loi.isEmpty(), loi::toString);
    assertTrue(nhom.stream().anyMatch(n -> n.dongTheoMaSP().size() == 2));
  }

  @Test
  void banHangDuocNhapMotNhomHaiDongVaBatApDung() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    var result = controller.nhapExcel(tepNhap(
        dongNhap(1, "A", MA_DONG), dongNhap(2, "A", dongSanPhamThuHai().maDongSP())));
    assertTrue(result.ketQua().hopLe());
    assertEquals(1, result.soChuongTrinh());
    assertEquals(1, soLanCommit);
    assertEquals(2, khuyenMaiDao.ds.getFirst().chiTiet().size());
    assertTrue(khuyenMaiDao.ds.getFirst().dangApDung());
  }

  @Test
  void loiFkTrongNhomSauRollbackToanTepVaChiDungDongExcel() {
    String maDongLoi = dongSanPhamThuHai().maDongSP();
    khuyenMaiDao.dongLoiKhiGhi = maDongLoi;
    var result = controller.nhapExcel(tepNhap(
        dongNhap(1, "A", MA_DONG), dongNhap(2, "B", maDongLoi)));
    assertFalse(result.ketQua().hopLe());
    assertEquals(0, result.soChuongTrinh());
    assertEquals(1, soLanRollback);
    assertEquals(0, soLanCommit);
    assertEquals(2, result.ketQua().loi().getFirst().soDongExcel());
    assertEquals(KhuyenMaiExcel.MA_DONG, result.ketQua().loi().getFirst().cot());
  }

  @Test
  void chongLanTrongTepChanTruocKhiGhi() {
    var result = controller.nhapExcel(tepNhap(dongNhap(1, "A", MA_DONG), dongNhap(2, "B", MA_DONG)));
    assertFalse(result.ketQua().hopLe());
    assertEquals(2, result.ketQua().loi().getFirst().soDongExcel());
    assertEquals(0, khuyenMaiDao.soLanInsert);
  }

  private static ImportResult tepNhap(ImportRow... dong) {
    return new ImportResult(List.of(dong), List.of(), dong.length);
  }

  private static ImportRow dongNhap(int soDong, String nhom, String maDong) {
    return new ImportRow(soDong, Map.of(
        KhuyenMaiExcel.MA_NHOM, nhom, KhuyenMaiExcel.TEN, "Ưu đãi",
        KhuyenMaiExcel.TY_LE, BigDecimal.TEN, KhuyenMaiExcel.BAT_DAU, BAT_DAU,
        KhuyenMaiExcel.KET_THUC, KET_THUC, KhuyenMaiExcel.AP_DUNG, true,
        KhuyenMaiExcel.MA_DONG, maDong));
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
  private DataSource dataSourceSinhMa(long maxMa) {
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
          case "commit" -> { soLanCommit++; yield null; }
          case "rollback" -> { soLanRollback++; yield null; }
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
      if (!tonTai) return Optional.empty();
      if (MA_DONG.equals(maDongSP)) return Optional.of(dongSanPham());
      if (dongSanPhamThuHai().maDongSP().equals(maDongSP)) return Optional.of(dongSanPhamThuHai());
      return Optional.empty();
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
    String dongChongLan;
    String dongLoiKhiGhi;
    List<DongSanPham> dongApDung = List.of(dongSanPham());
    List<String> danhMucDaLoc;
    LocalDate ngayGioiHan;
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
    @Override public List<DongApDung> findDongTheoDieuKien(
        String maDongSP, List<String> maDanhMuc, LocalDate ngayNhapMuonNhat) {
      danhMucDaLoc = maDanhMuc;
      ngayGioiHan = ngayNhapMuonNhat;
      return dongApDung.stream().map(dong -> new DongApDung(dong, 1)).toList();
    }
    @Override public void insert(KhuyenMai khuyenMai) { soLanInsert++; ds.add(khuyenMai); }
    @Override public void insert(java.sql.Connection ketNoi, KhuyenMai khuyenMai) {
      if (maTrung) {
        throw new DataAccessException("Mã khuyến mãi đã tồn tại trong hệ thống");
      }
      for (var chiTiet : khuyenMai.chiTiet()) {
        String maDong = chiTiet.dongSanPham().maDongSP();
        if (maDong.equals(dongLoiKhiGhi)) throw new LoiDongKhuyenMai(maDong, null);
      }
      soLanInsert++;
      ds.add(khuyenMai);
    }
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
      return chongLan || maDongSP.equals(dongChongLan);
    }
  }
}
