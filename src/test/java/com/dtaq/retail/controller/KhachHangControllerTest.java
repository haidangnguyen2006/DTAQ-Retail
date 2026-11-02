package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.KhachHangController.KetQuaLuuKhach;
import com.dtaq.retail.controller.KhachHangController.TrangKhachHang;
import com.dtaq.retail.controller.KhachHangController.TrangThaiLuu;
import com.dtaq.retail.dao.KhachHangDao;
import com.dtaq.retail.dao.impl.KhachHangDaoImpl;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
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

@DisplayName("Nghiệp vụ khách hàng (UC007)")
class KhachHangControllerTest {

  private static final KhachHang AN = new KhachHang("KH-2026-000001", "Nguyễn Văn An", "0902100001", "1 Lê Lợi");
  private static final KhachHang BINH = new KhachHang("KH-2026-000002", "Trần Thị Bình", "0902100002", null);

  private final FakeKhachHangDao dao = new FakeKhachHangDao();
  private final KhachHangController controller = new KhachHangController(dao, dataSourceLoi());

  @BeforeEach
  void dangNhapBanHang() {
    dangNhap(LoaiQuyen.NHAN_VIEN_BAN_HANG);
    dao.ds.add(AN);
    dao.ds.add(BINH);
  }

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Thiếu họ tên: báo lỗi đúng trường và không ghi")
  void themKhachThieuHoTen() {
    KetQuaLuuKhach ketQua = controller.themKhach("  ", "0902100009", null);
    assertEquals(TrangThaiLuu.LOI, ketQua.trangThai());
    assertEquals(KhachHangController.TRUONG_HO_TEN, ketQua.truongLoi());
    assertEquals(0, dao.soLanInsert);
  }

  @Test
  @DisplayName("SĐT sai định dạng: báo lỗi đúng trường và không ghi")
  void themKhachSaiSdt() {
    for (String sdt : List.of("", "123", "1902100009", "090210000a", "090210000912")) {
      KetQuaLuuKhach ketQua = controller.themKhach("Khách", sdt, null);
      assertEquals(TrangThaiLuu.LOI, ketQua.trangThai(), sdt);
      assertEquals(KhachHangController.TRUONG_SDT, ketQua.truongLoi(), sdt);
    }
    assertEquals(0, dao.soLanInsert);
  }

  @Test
  @DisplayName("Kiểm tra SĐT: 10–11 chữ số, bắt đầu bằng 0, bỏ qua khoảng trắng")
  void kiemTraSdtChapNhanDungDinhDang() {
    assertNull(KhachHangController.kiemTraSdt("0902100009"));
    assertNull(KhachHangController.kiemTraSdt("09021000091"));
    assertNull(KhachHangController.kiemTraSdt("0902 100 009"));
  }

  @Test
  @DisplayName("Trùng SĐT (kể cả có khoảng trắng): trả hồ sơ hiện có, không tạo mới")
  void themKhachTrungSdtTraHoSoCu() {
    KetQuaLuuKhach ketQua = controller.themKhach("Tên khác", "0902 100 001", "Địa chỉ khác");
    assertEquals(TrangThaiLuu.TRUNG_SDT, ketQua.trangThai());
    assertEquals(AN, ketQua.khachHang());
    assertEquals(0, dao.soLanInsert);
  }

  @Test
  @DisplayName("Lỗi kết nối khi lưu: báo thất bại, không ném lỗi ra giao diện")
  void themKhachLoiKetNoi() {
    KetQuaLuuKhach ketQua = controller.themKhach("Khách mới", "0902100009", null);
    assertEquals(TrangThaiLuu.LOI, ketQua.trangThai());
    assertNull(ketQua.truongLoi());
    assertFalse(ketQua.thongBao().isBlank());
  }

  @Test
  @DisplayName("Nhân viên quản lý hoặc chưa đăng nhập không được dùng chức năng")
  void chanSaiQuyen() {
    dangNhap(LoaiQuyen.NHAN_VIEN_QUAN_LY);
    assertThrows(SecurityException.class, () -> controller.themKhach("Khách", "0902100009", null));
    assertThrows(SecurityException.class, () -> controller.danhSach("", 1, 10));
    assertThrows(SecurityException.class, () -> controller.timTheoSdt("0902100001"));
    assertThrows(SecurityException.class,
        () -> controller.capNhatKhach(AN.maKH(), "Tên", "0902100001", null));

    CurrentSession.clear();
    assertThrows(SecurityException.class, () -> controller.danhSach("", 1, 10));
    assertEquals(0, dao.soLanInsert);
    assertEquals(0, dao.soLanUpdate);
  }

  @Test
  @DisplayName("Cập nhật địa chỉ giữ nguyên mã khách hàng")
  void capNhatDiaChi() {
    KetQuaLuuKhach ketQua = controller.capNhatKhach(AN.maKH(), " Nguyễn Văn An ", AN.soDienThoai(), " 9 Hai Bà Trưng ");
    assertEquals(TrangThaiLuu.THANH_CONG, ketQua.trangThai());
    KhachHang moi = dao.findBySdt(AN.soDienThoai()).orElseThrow();
    assertEquals(AN.maKH(), moi.maKH());
    assertEquals("Nguyễn Văn An", moi.hoTen());
    assertEquals("9 Hai Bà Trưng", moi.diaChi());
  }

  @Test
  @DisplayName("Cập nhật sang SĐT của khách khác bị chặn")
  void capNhatTrungSdtKhachKhac() {
    KetQuaLuuKhach ketQua = controller.capNhatKhach(BINH.maKH(), BINH.hoTen(), AN.soDienThoai(), null);
    assertEquals(TrangThaiLuu.LOI, ketQua.trangThai());
    assertEquals(KhachHangController.TRUONG_SDT, ketQua.truongLoi());
    assertEquals(0, dao.soLanUpdate);
  }

  @Test
  @DisplayName("Cập nhật thất bại khi ghi: báo lỗi chung")
  void capNhatLoiGhi() {
    dao.loiUpdate = true;
    KetQuaLuuKhach ketQua = controller.capNhatKhach(AN.maKH(), AN.hoTen(), AN.soDienThoai(), null);
    assertEquals(TrangThaiLuu.LOI, ketQua.trangThai());
    assertNull(ketQua.truongLoi());
  }

  @Test
  @DisplayName("Tìm theo SĐT bỏ qua khoảng trắng")
  void timTheoSdt() {
    assertEquals(Optional.of(AN), controller.timTheoSdt(" 0902 100 001 "));
    assertTrue(controller.timTheoSdt("0999999999").isEmpty());
  }

  @Test
  @DisplayName("Danh sách lọc theo họ tên không phân biệt hoa thường hoặc theo SĐT")
  void danhSachLocTheoTuKhoa() {
    assertEquals(2, controller.danhSach("", 1, 10).tongSo());
    assertEquals(List.of(BINH), controller.danhSach("trần thị", 1, 10).danhSach());
    assertEquals(List.of(AN), controller.danhSach("0902 100 001", 1, 10).danhSach());
    assertEquals(0, controller.danhSach("không có", 1, 10).tongSo());
  }

  @Test
  @DisplayName("Phân trang đúng và trang vượt quá được đưa về trang cuối")
  void danhSachPhanTrang() {
    TrangKhachHang trang2 = controller.danhSach("", 2, 1);
    assertEquals(List.of(BINH), trang2.danhSach());
    assertEquals(2, trang2.tongSo());
    assertEquals(2, trang2.trang());

    assertEquals(2, controller.danhSach("", 9, 1).trang());
    assertEquals(1, controller.danhSach("không có", 5, 10).trang());
    assertThrows(IllegalArgumentException.class, () -> controller.danhSach("", 1, 0));
  }

  @Test
  @DisplayName("Thêm mới trên CSDL: cấp mã đúng dạng và lưu chuẩn hoá; xóa dữ liệu thử sau test")
  void themKhachTrenCsdl() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DataSource dataSource = ConnectDB.getDataSource();
    KhachHangController thuc = new KhachHangController(new KhachHangDaoImpl(dataSource), dataSource);
    String sdt = "0988000201";
    try {
      KetQuaLuuKhach ketQua = thuc.themKhach(" Khách kiểm thử ", "0988 000 201", "  ");
      assertEquals(TrangThaiLuu.THANH_CONG, ketQua.trangThai());
      assertTrue(ketQua.khachHang().maKH().matches("^KH-\\d{4}-\\d{6}$"), ketQua.khachHang().maKH());

      KhachHang daLuu = thuc.timTheoSdt(sdt).orElseThrow();
      assertEquals("Khách kiểm thử", daLuu.hoTen());
      assertNull(daLuu.diaChi());

      assertEquals(TrangThaiLuu.TRUNG_SDT, thuc.themKhach("Khách khác", sdt, null).trangThai());
    } finally {
      xoaTheoSdt(dataSource, sdt);
    }
  }

  @Test
  @DisplayName("Lỗi sau khi ghi trong giao dịch: quay lui, không còn hồ sơ")
  void themKhachLoiGiuaChungQuayLui() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DataSource dataSource = ConnectDB.getDataSource();
    KhachHangDao that = new KhachHangDaoImpl(dataSource);
    String sdt = "0988000202";
    KhachHangDao ghiRoiLoi = new FakeKhachHangDao() {
      @Override
      public void insert(Connection ketNoi, KhachHang khachHang) {
        that.insert(ketNoi, khachHang);
        throw new DataAccessException("lỗi giả lập sau khi ghi");
      }
    };
    try {
      KetQuaLuuKhach ketQua =
          new KhachHangController(ghiRoiLoi, dataSource).themKhach("Khách kiểm thử", sdt, null);
      assertEquals(TrangThaiLuu.LOI, ketQua.trangThai());
      assertTrue(that.findBySdt(sdt).isEmpty(), "giao dịch phải được quay lui");
    } finally {
      xoaTheoSdt(dataSource, sdt);
    }
  }

  private static void xoaTheoSdt(DataSource dataSource, String sdt) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        PreparedStatement statement =
            ketNoi.prepareStatement("DELETE FROM \"KhachHang\" WHERE \"soDienThoai\" = ?")) {
      statement.setString(1, sdt);
      statement.executeUpdate();
    }
  }

  private static void dangNhap(LoaiQuyen loaiQuyen) {
    CurrentSession.set(new NhanVien("NV", "Nhân viên", "0900000000", null,
        LocalDate.of(2026, 1, 1), "nv", "hash", loaiQuyen, true));
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

  private static class FakeKhachHangDao implements KhachHangDao {

    final List<KhachHang> ds = new ArrayList<>();
    int soLanInsert;
    int soLanUpdate;
    boolean loiUpdate;

    @Override
    public Optional<KhachHang> findBySdt(String soDienThoai) {
      return ds.stream().filter(kh -> kh.soDienThoai().equals(soDienThoai)).findFirst();
    }

    @Override
    public List<KhachHang> findAll() {
      return List.copyOf(ds);
    }

    @Override
    public void insert(KhachHang khachHang) {
      soLanInsert++;
      ds.add(khachHang);
    }

    @Override
    public void insert(Connection ketNoi, KhachHang khachHang) {
      insert(khachHang);
    }

    @Override
    public void update(KhachHang khachHang) {
      if (loiUpdate) {
        throw new DataAccessException("lỗi giả lập");
      }
      soLanUpdate++;
      ds.replaceAll(kh -> kh.maKH().equals(khachHang.maKH()) ? khachHang : kh);
    }
  }
}
