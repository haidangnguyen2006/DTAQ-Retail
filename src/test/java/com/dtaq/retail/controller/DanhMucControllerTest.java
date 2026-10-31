package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.impl.DanhMucSanPhamDaoImpl;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử đơn vị và tích hợp cho bộ điều khiển danh mục sản phẩm.
 */
class DanhMucControllerTest {

  private static final String TEN_TEST = "Danh Mục Test Controller JUnit";
  private static final String MO_TA_TEST = "Mô tả thử nghiệm controller";

  private DanhMucSanPhamDao dao;
  private DanhMucController controller;
  private final List<String> maCanXoa = new ArrayList<>();

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");
    dao = new DanhMucSanPhamDaoImpl(ConnectDB.getDataSource());
    controller = new DanhMucController(dao, ConnectDB.getDataSource());
  }

  @AfterEach
  void tearDown() {
    if (ConnectDB.isAvailable()) {
      for (String ma : maCanXoa) {
        try {
          dao.delete(ma);
        } catch (Exception ignored) {
          // Bỏ qua dọn dẹp nếu bản ghi đã xóa
        }
      }
      maCanXoa.clear();
    }
  }

  @Test
  @DisplayName("Lấy danh sách danh mục thành công")
  void layDanhSach_thanhCong() {
    List<DanhMucSanPham> ds = controller.layDanhSach();
    assertNotNull(ds);
    assertFalse(ds.isEmpty());
  }

  @Test
  @DisplayName("Tìm kiếm theo mã rỗng trả về rỗng")
  void timTheoMa_maRong() {
    assertTrue(controller.timTheoMa(null).isEmpty());
    assertTrue(controller.timTheoMa("   ").isEmpty());
  }

  @Test
  @DisplayName("Thêm mới danh mục thành công với mã định danh tự sinh")
  void themMoi_thanhCong() {
    DanhMucController.KetQua<DanhMucSanPham> ketQua = controller.themMoi(TEN_TEST, MO_TA_TEST);
    assertTrue(ketQua.thanhCong());
    assertNotNull(ketQua.duLieu());

    DanhMucSanPham dm = ketQua.duLieu();
    maCanXoa.add(dm.maDanhMuc());

    assertNotNull(dm.maDanhMuc());
    assertTrue(dm.maDanhMuc().matches("^DM-\\d{4}-\\d{6}$"));
    assertEquals(TEN_TEST, dm.tenDanhMuc());
    assertEquals(MO_TA_TEST, dm.moTa());

    Optional<DanhMucSanPham> timThay = controller.timTheoMa(dm.maDanhMuc());
    assertTrue(timThay.isPresent());
    assertEquals(TEN_TEST, timThay.get().tenDanhMuc());
  }

  @Test
  @DisplayName("Từ chối thêm mới khi tên danh mục để trống")
  void themMoi_loiTenRong() {
    DanhMucController.KetQua<DanhMucSanPham> ketQua = controller.themMoi("   ", MO_TA_TEST);
    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().toLowerCase().contains("trống"));
  }

  @Test
  @DisplayName("Từ chối thêm mới khi tên danh mục đã tồn tại")
  void themMoi_loiTrungTen() {
    DanhMucController.KetQua<DanhMucSanPham> lan1 = controller.themMoi(TEN_TEST, MO_TA_TEST);
    assertTrue(lan1.thanhCong());
    maCanXoa.add(lan1.duLieu().maDanhMuc());

    DanhMucController.KetQua<DanhMucSanPham> lan2 = controller.themMoi(TEN_TEST.toUpperCase(), "Mô tả khác");
    assertFalse(lan2.thanhCong());
    assertTrue(lan2.thongBao().toLowerCase().contains("tồn tại") || lan2.thongBao().toLowerCase().contains("trùng"));
  }

  @Test
  @DisplayName("Cập nhật thông tin danh mục thành công")
  void capNhat_thanhCong() {
    DanhMucController.KetQua<DanhMucSanPham> taoMoi = controller.themMoi(TEN_TEST, MO_TA_TEST);
    assertTrue(taoMoi.thanhCong());
    DanhMucSanPham dm = taoMoi.duLieu();
    maCanXoa.add(dm.maDanhMuc());

    String tenMoi = TEN_TEST + " Đã Sửa";
    String moTaMoi = "Mô tả mới sau cập nhật";
    DanhMucController.KetQua<DanhMucSanPham> capNhat = controller.capNhat(dm.maDanhMuc(), tenMoi, moTaMoi);
    assertTrue(capNhat.thanhCong());

    Optional<DanhMucSanPham> sauCapNhat = controller.timTheoMa(dm.maDanhMuc());
    assertTrue(sauCapNhat.isPresent());
    assertEquals(tenMoi, sauCapNhat.get().tenDanhMuc());
    assertEquals(moTaMoi, sauCapNhat.get().moTa());
  }

  @Test
  @DisplayName("Từ chối cập nhật khi mã hoặc tên để trống")
  void capNhat_loiRong() {
    DanhMucController.KetQua<DanhMucSanPham> loiMa = controller.capNhat("  ", TEN_TEST, MO_TA_TEST);
    assertFalse(loiMa.thanhCong());

    DanhMucController.KetQua<DanhMucSanPham> loiTen = controller.capNhat("DM-2026-000001", "  ", MO_TA_TEST);
    assertFalse(loiTen.thanhCong());
  }

  @Test
  @DisplayName("Chặn xóa danh mục khi có dòng sản phẩm tham chiếu")
  void xoa_biChanKhiCoDongSanPham() {
    // DM-2026-000001 là danh mục hạt giống đã có dòng sản phẩm tham chiếu
    DanhMucController.KetQua<Void> ketQua = controller.xoa("DM-2026-000001");
    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("dòng sản phẩm tham chiếu"));
  }

  @Test
  @DisplayName("Xóa thành công danh mục khi không có dòng sản phẩm tham chiếu")
  void xoa_thanhCongKhiKhongCoDongSanPham() {
    DanhMucController.KetQua<DanhMucSanPham> taoMoi = controller.themMoi(TEN_TEST, MO_TA_TEST);
    assertTrue(taoMoi.thanhCong());
    String ma = taoMoi.duLieu().maDanhMuc();

    DanhMucController.KetQua<Void> ketQuaXoa = controller.xoa(ma);
    assertTrue(ketQuaXoa.thanhCong());
    assertTrue(controller.timTheoMa(ma).isEmpty());
  }

  @Test
  @DisplayName("Kiểm thử logic với lớp truy cập dữ liệu giả lập")
  void kiemThuLogic_giaLapDao() {
    FakeDanhMucDao fakeDao = new FakeDanhMucDao();
    // Tạo controller với dao giả lập để kiểm tra logic nhánh không phụ thuộc CSDL
    fakeDao.coDongThamChieu = true;
    DanhMucController fakeController = new DanhMucController(fakeDao, ConnectDB.getDataSource());

    DanhMucController.KetQua<Void> kqXoa = fakeController.xoa("DM-FAKE-01");
    assertFalse(kqXoa.thanhCong());
    assertEquals("Danh mục đang có dòng sản phẩm tham chiếu, không thể xóa", kqXoa.thongBao());
    assertEquals(0, fakeDao.soLanGoiDelete);
  }

  private static final class FakeDanhMucDao implements DanhMucSanPhamDao {
    boolean coDongThamChieu = false;
    int soLanGoiDelete = 0;

    @Override
    public List<DanhMucSanPham> findAll() {
      return List.of();
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
    public void delete(String maDanhMuc) {
      soLanGoiDelete++;
    }

    @Override
    public boolean existsDongByDanhMuc(String maDanhMuc) {
      return coDongThamChieu;
    }

    @Override
    public boolean existsByTen(String tenDanhMuc, String excludeMaDanhMuc) {
      return false;
    }
  }
}
