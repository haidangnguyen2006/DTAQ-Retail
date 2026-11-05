package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.NhanVienController.KetQuaLuuNhanVien;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.PasswordHasher;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Nghiệp vụ nhân viên (UC013)")
class NhanVienControllerTest {

  private static final LocalDate NGAY = LocalDate.of(2026, 11, 4);
  private static final NhanVien QUAN_LY = nhanVien("NVQUANLY-2026-001", "ql", LoaiQuyen.NHAN_VIEN_QUAN_LY, true);
  private static final NhanVien BAN_HANG = nhanVien("NVBANHANG-2026-001", "bh", LoaiQuyen.NHAN_VIEN_BAN_HANG, true);
  private static final NhanVien DA_KHOA = nhanVien("NVBANHANG-2026-002", "khoa", LoaiQuyen.NHAN_VIEN_BAN_HANG, false);

  private final FakeNhanVienDao dao = new FakeNhanVienDao();
  private final NhanVienController controller = new NhanVienController(dao, dataSourceLoi());

  @BeforeEach
  void dangNhapQuanLy() {
    CurrentSession.set(QUAN_LY);
    dao.ds.addAll(List.of(QUAN_LY, BAN_HANG, DA_KHOA));
  }

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Quản lý xem danh sách và lọc theo trạng thái tài khoản")
  void danhSachLocTrangThai() {
    assertEquals(3, controller.danhSach().size());
    assertEquals(List.of(QUAN_LY, BAN_HANG), controller.danhSach(true));
    assertEquals(List.of(DA_KHOA), controller.danhSach(false));
  }

  @Test
  @DisplayName("Nhân viên bán hàng hoặc chưa đăng nhập không dùng được chức năng")
  void chanSaiQuyen() {
    CurrentSession.set(BAN_HANG);
    assertThrows(SecurityException.class, controller::danhSach);
    assertThrows(SecurityException.class, () -> them("Tên", "0901234567", null, NGAY, "moi", "12345678"));
    assertThrows(SecurityException.class, () -> controller.khoaTaiKhoan(DA_KHOA.maNV()));
    assertThrows(SecurityException.class, () -> controller.moKhoaTaiKhoan(DA_KHOA.maNV()));

    CurrentSession.clear();
    assertThrows(SecurityException.class, controller::danhSach);
    assertEquals(0, dao.soLanDoiTrangThai);
  }

  @Test
  @DisplayName("Dữ liệu thiếu hoặc sai: báo lỗi đúng trường, không ghi")
  void themNhanVienKiemTraDuLieu() {
    assertEquals(NhanVienController.TRUONG_HO_TEN,
        them(" ", "0901234567", null, NGAY, "moi", "12345678").truongLoi());
    assertEquals(NhanVienController.TRUONG_SDT,
        them("Tên", "123", null, NGAY, "moi", "12345678").truongLoi());
    assertEquals(NhanVienController.TRUONG_EMAIL,
        them("Tên", "0901234567", "khong-hop-le", NGAY, "moi", "12345678").truongLoi());
    assertEquals(NhanVienController.TRUONG_NGAY_VAO_LAM,
        them("Tên", "0901234567", null, null, "moi", "12345678").truongLoi());
    assertEquals(NhanVienController.TRUONG_TEN_DANG_NHAP,
        them("Tên", "0901234567", null, NGAY, "  ", "12345678").truongLoi());
    assertEquals(NhanVienController.TRUONG_MAT_KHAU,
        them("Tên", "0901234567", null, NGAY, "moi", "1234567").truongLoi());
    assertEquals(NhanVienController.TRUONG_MAT_KHAU,
        them("Tên", "0901234567", null, NGAY, "moi", "x".repeat(73)).truongLoi());
    assertEquals(0, dao.soLanInsert);
  }

  @Test
  @DisplayName("Tên đăng nhập đã tồn tại: báo lỗi trường tên đăng nhập (9.b)")
  void themNhanVienTrungTenDangNhap() {
    KetQuaLuuNhanVien ketQua = them("Tên", "0901234567", "a@b.vn", NGAY, " bh ", "12345678");
    assertFalse(ketQua.thanhCong());
    assertEquals(NhanVienController.TRUONG_TEN_DANG_NHAP, ketQua.truongLoi());
    assertEquals(0, dao.soLanInsert);
  }

  @Test
  @DisplayName("Lỗi kết nối khi lưu: báo thất bại chung, mảng mật khẩu bị xóa trắng")
  void themNhanVienLoiKetNoiVaXoaMatKhau() {
    char[] matKhau = "12345678".toCharArray();
    KetQuaLuuNhanVien ketQua = controller.themNhanVien("Tên", "0901234567", null, NGAY, "moi", matKhau);
    assertFalse(ketQua.thanhCong());
    assertNull(ketQua.truongLoi());
    assertEquals("\0".repeat(8), new String(matKhau));
  }

  @Test
  @DisplayName("Không tự khóa tài khoản đang đăng nhập")
  void khongTuKhoa() {
    KetQuaLuuNhanVien ketQua = controller.khoaTaiKhoan(QUAN_LY.maNV());
    assertFalse(ketQua.thanhCong());
    assertEquals(0, dao.soLanDoiTrangThai);
  }

  @Test
  @DisplayName("Khóa rồi mở khóa tài khoản nhân viên khác")
  void khoaVaMoKhoa() {
    assertTrue(controller.khoaTaiKhoan(BAN_HANG.maNV()).thanhCong());
    assertFalse(dao.findByMa(BAN_HANG.maNV()).orElseThrow().trangThaiHoatDong());
    assertTrue(controller.moKhoaTaiKhoan(BAN_HANG.maNV()).thanhCong());
    assertTrue(dao.findByMa(BAN_HANG.maNV()).orElseThrow().trangThaiHoatDong());
  }

  @Test
  @DisplayName("Lỗi khi cập nhật trạng thái: báo thất bại")
  void doiTrangThaiLoi() {
    dao.loiDoiTrangThai = true;
    assertFalse(controller.khoaTaiKhoan(BAN_HANG.maNV()).thanhCong());
  }

  @Test
  @DisplayName("Thêm trên CSDL: mã đúng dạng, chỉ lưu hash; khóa thì đăng nhập LOCKED, mở lại thì đăng nhập được")
  void themKhoaMoKhoaTrenCsdl() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DataSource dataSource = ConnectDB.getDataSource();
    NhanVienDaoImpl that = new NhanVienDaoImpl(dataSource);
    NhanVienController thuc = new NhanVienController(that, dataSource);
    AuthController auth = new AuthController(that);
    String tenDangNhap = "kiemthu_w2_uc013";
    String matKhau = "MatKhau2026";
    try {
      KetQuaLuuNhanVien ketQua = thuc.themNhanVien(" Nhân viên kiểm thử ", "0988 000 401",
          "kiemthu@dtaq.example", NGAY, tenDangNhap, matKhau.toCharArray());
      assertTrue(ketQua.thanhCong(), ketQua.thongBao());
      NhanVien moi = that.findByTenDangNhap(tenDangNhap).orElseThrow();
      assertTrue(moi.maNV().matches("^NVBANHANG-\\d{4}-\\d{6}$"), moi.maNV());
      assertEquals("Nhân viên kiểm thử", moi.hoTen());
      assertEquals("0988000401", moi.soDienThoai());
      assertEquals(LoaiQuyen.NHAN_VIEN_BAN_HANG, moi.loaiQuyen());
      assertTrue(moi.trangThaiHoatDong());
      assertFalse(moi.matKhau().contains(matKhau), "không được lưu mật khẩu thô");
      assertTrue(PasswordHasher.verify(matKhau.toCharArray(), moi.matKhau()));
      assertEquals(AuthController.Outcome.SUCCESS, auth.login(tenDangNhap, matKhau).outcome());

      assertTrue(thuc.khoaTaiKhoan(moi.maNV()).thanhCong());
      assertEquals(AuthController.Outcome.LOCKED, auth.login(tenDangNhap, matKhau).outcome());
      assertTrue(that.findByMa(moi.maNV()).isPresent(), "khóa không được xóa hồ sơ");

      assertTrue(thuc.moKhoaTaiKhoan(moi.maNV()).thanhCong());
      assertEquals(AuthController.Outcome.SUCCESS, auth.login(tenDangNhap, matKhau).outcome());
    } finally {
      xoaTheoTenDangNhap(dataSource, tenDangNhap);
    }
  }

  @Test
  @DisplayName("Trùng tên đăng nhập lọt qua bước kiểm tra trước: CSDL chặn, quay lui, báo đúng trường")
  void trungTenDangNhapDoChayDongThoi() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DataSource dataSource = ConnectDB.getDataSource();
    NhanVienDaoImpl that = new NhanVienDaoImpl(dataSource);
    Optional<NhanVien> daCo = that.findByTenDangNhap("NVBH2026001");
    Assumptions.assumeTrue(daCo.isPresent(), "seed V2 chưa được áp dụng");
    int truoc = that.findAll().size();
    NhanVienDao boQuaKiemTra = new NhanVienDaoImpl(dataSource) {
      @Override
      public boolean existsTenDangNhap(String tenDangNhap) {
        return false;
      }
    };

    KetQuaLuuNhanVien ketQua = new NhanVienController(boQuaKiemTra, dataSource).themNhanVien(
        "Trùng", "0988000402", null, NGAY, "NVBH2026001", "12345678".toCharArray());

    assertFalse(ketQua.thanhCong());
    assertEquals(NhanVienController.TRUONG_TEN_DANG_NHAP, ketQua.truongLoi());
    assertEquals(truoc, that.findAll().size(), "giao dịch phải được quay lui");
  }

  private KetQuaLuuNhanVien them(String hoTen, String sdt, String email, LocalDate ngay,
      String tenDangNhap, String matKhau) {
    return controller.themNhanVien(hoTen, sdt, email, ngay, tenDangNhap, matKhau.toCharArray());
  }

  private static void xoaTheoTenDangNhap(DataSource dataSource, String tenDangNhap) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement =
            ketNoi.prepareStatement("DELETE FROM \"NhanVien\" WHERE \"tenDangNhap\" = ?")) {
      statement.setString(1, tenDangNhap);
      statement.executeUpdate();
    }
  }

  private static NhanVien nhanVien(String maNV, String tenDangNhap, LoaiQuyen loaiQuyen, boolean hoatDong) {
    return new NhanVien(maNV, "Nhân viên " + tenDangNhap, "0900000000", null,
        LocalDate.of(2026, 1, 1), tenDangNhap, "hash", loaiQuyen, hoatDong);
  }

  /** Nguồn dữ liệu luôn lỗi kết nối, dùng cho các ca không cần CSDL thật. */
  private static DataSource dataSourceLoi() {
    return (DataSource) Proxy.newProxyInstance(
        DataSource.class.getClassLoader(),
        new Class<?>[] {DataSource.class},
        (proxy, method, args) -> {
          throw new SQLException("không có kết nối");
        });
  }

  private static final class FakeNhanVienDao implements NhanVienDao {

    private final List<NhanVien> ds = new ArrayList<>();
    private int soLanInsert;
    private int soLanDoiTrangThai;
    private boolean loiDoiTrangThai;

    @Override
    public List<NhanVien> findAll() {
      return List.copyOf(ds);
    }

    @Override
    public Optional<NhanVien> findByMa(String maNV) {
      return ds.stream().filter(nv -> nv.maNV().equals(maNV)).findFirst();
    }

    @Override
    public boolean existsTenDangNhap(String tenDangNhap) {
      return ds.stream().anyMatch(nv -> nv.tenDangNhap().equals(tenDangNhap));
    }

    @Override
    public boolean existsMaNV(String maNV) {
      return findByMa(maNV).isPresent();
    }

    @Override
    public void insert(Connection ketNoi, NhanVien nhanVien) {
      soLanInsert++;
      ds.add(nhanVien);
    }

    @Override
    public void updateTrangThai(String maNV, boolean trangThaiHoatDong) {
      if (loiDoiTrangThai) {
        throw new DataAccessException("lỗi giả lập");
      }
      soLanDoiTrangThai++;
      ds.replaceAll(nv -> nv.maNV().equals(maNV)
          ? new NhanVien(nv.maNV(), nv.hoTen(), nv.soDienThoai(), nv.email(), nv.ngayVaoLam(),
              nv.tenDangNhap(), nv.matKhau(), nv.loaiQuyen(), trangThaiHoatDong)
          : nv);
    }

    @Override
    public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public void updateMatKhau(String maNV, String matKhauHash) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }

    @Override
    public void update(NhanVien nhanVien) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử nhân viên");
    }
  }
}
