package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.controller.PheDuyetDoiTraController.KetQua;
import com.dtaq.retail.dao.PhieuDoiDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.session.CurrentSession;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Nghiệp vụ phê duyệt yêu cầu đổi trả (UC018)")
class PheDuyetDoiTraControllerTest {

  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Clock DONG_HO_CO_DINH =
      Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), MUI_GIO);
  private static final LocalDate NGAY_DUYET = LocalDate.of(2026, 11, 4);

  private static final NhanVien QUAN_LY =
      nhanVien("NVQUANLY-2026-001", LoaiQuyen.NHAN_VIEN_QUAN_LY);
  private static final NhanVien BAN_HANG =
      nhanVien("NVBANHANG-2026-001", LoaiQuyen.NHAN_VIEN_BAN_HANG);

  private final FakePhieuTraDao phieuTraDao = new FakePhieuTraDao();
  private final FakePhieuDoiDao phieuDoiDao = new FakePhieuDoiDao();

  private PheDuyetDoiTraController controller;

  @BeforeEach
  void setUp() {
    controller = new PheDuyetDoiTraController(phieuTraDao, phieuDoiDao, DONG_HO_CO_DINH);
  }

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Nhân viên bán hàng không được xem danh sách và không được ra quyết định")
  void khongPhaiQuanLyThiTuChoi() {
    CurrentSession.set(BAN_HANG);

    SecurityException loiDanhSach = assertThrows(SecurityException.class, controller::danhSachChoDuyet);
    assertEquals("Chỉ Quản lý được phê duyệt yêu cầu đổi trả", loiDanhSach.getMessage());
    SecurityException loiQuyetDinh =
        assertThrows(SecurityException.class, () -> controller.quyetDinh("PT-2026-000010", true, null));
    assertEquals("Chỉ Quản lý được phê duyệt yêu cầu đổi trả", loiQuyetDinh.getMessage());

    assertEquals(0, phieuTraDao.soLanTim);
    assertEquals(0, phieuDoiDao.soLanTim);
    assertEquals(0, phieuTraDao.soLanCapNhat);
    assertEquals(0, phieuDoiDao.soLanCapNhat);
  }

  @Test
  @DisplayName("Từ chối mà không nhập lý do thì bị chặn và không gọi DAO")
  void tuChoiThieuLyDoBiChan() {
    CurrentSession.set(QUAN_LY);

    KetQua ketQua = controller.quyetDinh("PT-2026-000010", false, "   ");

    assertFalse(ketQua.thanhCong());
    assertEquals("Vui lòng nhập lý do từ chối", ketQua.thongBao());
    assertEquals(0, phieuTraDao.soLanCapNhat);
    assertEquals(0, phieuDoiDao.soLanCapNhat);
  }

  @Test
  @DisplayName("Duyệt ghi đúng trạng thái, người duyệt, ngày duyệt và ý kiến đã cắt khoảng trắng")
  void pheDuyetGhiDungTrangThai() {
    CurrentSession.set(QUAN_LY);

    KetQua ketQua = controller.quyetDinh("PT-2026-000010", true, "  Đạt điều kiện  ");

    assertTrue(ketQua.thanhCong());
    assertEquals("Đã phê duyệt phiếu PT-2026-000010", ketQua.thongBao());
    assertEquals(1, phieuTraDao.soLanCapNhat);
    assertEquals(0, phieuDoiDao.soLanCapNhat);
    assertEquals("PT-2026-000010", phieuTraDao.maPhieuCuoi);
    assertEquals(TrangThaiDuyet.DA_DUYET, phieuTraDao.quyetDinhCuoi);
    assertEquals(QUAN_LY.maNV(), phieuTraDao.maNhanVienDuyetCuoi);
    assertEquals(NGAY_DUYET, phieuTraDao.ngayDuyetCuoi);
    assertEquals("Đạt điều kiện", phieuTraDao.yKienCuoi);
  }

  @Test
  @DisplayName("Từ chối ghi trạng thái TU_CHOI kèm lý do đã cắt khoảng trắng")
  void tuChoiGhiLyDo() {
    CurrentSession.set(QUAN_LY);

    KetQua ketQua = controller.quyetDinh("PD-2026-000008", false, "  Máy đã qua thời hạn  ");

    assertTrue(ketQua.thanhCong());
    assertEquals("Đã từ chối phiếu PD-2026-000008", ketQua.thongBao());
    assertEquals(0, phieuTraDao.soLanCapNhat);
    assertEquals(1, phieuDoiDao.soLanCapNhat);
    assertEquals(TrangThaiDuyet.TU_CHOI, phieuDoiDao.quyetDinhCuoi);
    assertEquals("Máy đã qua thời hạn", phieuDoiDao.yKienCuoi);
    assertEquals(QUAN_LY.maNV(), phieuDoiDao.maNhanVienDuyetCuoi);
    assertEquals(NGAY_DUYET, phieuDoiDao.ngayDuyetCuoi);
  }

  @Test
  @DisplayName("Quyết định lần hai trên phiếu đã xử lý báo cho người dùng tải lại danh sách")
  void quyetDinhLanHaiBaoDaXuLy() {
    CurrentSession.set(QUAN_LY);
    phieuTraDao.ketQuaCapNhat = false;

    KetQua ketQua = controller.quyetDinh("PT-2026-000011", true, null);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("đã được xử lý"), ketQua.thongBao());
    assertEquals(1, phieuTraDao.soLanCapNhat);
    assertNull(phieuTraDao.yKienCuoi, "Duyệt mà không nhập ý kiến thì lưu null");
  }

  @Test
  @DisplayName("Mã phiếu sai định dạng bị từ chối trước khi gọi DAO")
  void maPhieuKhongHopLe() {
    CurrentSession.set(QUAN_LY);

    KetQua ketQua = controller.quyetDinh("XX-1", true, null);

    assertFalse(ketQua.thanhCong());
    assertEquals("Mã phiếu không hợp lệ", ketQua.thongBao());
    assertEquals(0, phieuTraDao.soLanCapNhat);
    assertEquals(0, phieuDoiDao.soLanCapNhat);

    assertFalse(controller.quyetDinh("   ", true, null).thanhCong());
    assertFalse(controller.quyetDinh(null, true, null).thanhCong());
  }

  @Test
  @DisplayName("Danh sách gộp hai loại phiếu và sắp xếp tăng dần theo ngày tạo")
  void danhSachGopHaiLoaiVaSapXep() {
    CurrentSession.set(QUAN_LY);
    phieuTraDao.danhSach = new ArrayList<>(List.of(
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000011", LocalDate.of(2026, 3, 21)),
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000010", LocalDate.of(2026, 3, 13))));
    phieuDoiDao.danhSach = new ArrayList<>(List.of(
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_DOI, "PD-2026-000009", LocalDate.of(2026, 6, 12)),
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_DOI, "PD-2026-000008", LocalDate.of(2026, 6, 4))));

    List<PhieuDuyet> danhSach = controller.danhSachChoDuyet();

    assertEquals(List.of("PT-2026-000010", "PT-2026-000011", "PD-2026-000008", "PD-2026-000009"),
        danhSach.stream().map(PhieuDuyet::maPhieu).toList());
    assertEquals(1, phieuTraDao.soLanTim);
    assertEquals(1, phieuDoiDao.soLanTim);
    assertThrows(UnsupportedOperationException.class,
        () -> danhSach.add(phieu(PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000012", null)),
        "Danh sách trả về phải bất biến");
  }

  private static PhieuDuyet phieu(PhieuDuyet.LoaiPhieu loai, String maPhieu, LocalDate ngayTao) {
    return new PhieuDuyet(loai, maPhieu, "HD-2026-000010", "Nguyễn Văn An", "0902100001",
        loai == PhieuDuyet.LoaiPhieu.PHIEU_TRA ? new BigDecimal("12096000") : BigDecimal.ZERO,
        "Trần Văn Duy", "Khách yêu cầu trả máy", ngayTao);
  }

  private static NhanVien nhanVien(String maNV, LoaiQuyen loaiQuyen) {
    return new NhanVien(maNV, "Nhân viên " + maNV, "0900000001", maNV + "@dtaq.example",
        LocalDate.of(2026, 1, 1), maNV, "matkhau", loaiQuyen, true);
  }

  /** DAO phiếu trả giả ghi lại tham số của lần gọi gần nhất. */
  private static final class FakePhieuTraDao implements PhieuTraDao {

    private List<PhieuDuyet> danhSach = new ArrayList<>();
    private boolean ketQuaCapNhat = true;
    private int soLanTim;
    private int soLanCapNhat;
    private String maPhieuCuoi;
    private TrangThaiDuyet quyetDinhCuoi;
    private String maNhanVienDuyetCuoi;
    private LocalDate ngayDuyetCuoi;
    private String yKienCuoi;

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      soLanTim++;
      return danhSach;
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      soLanCapNhat++;
      maPhieuCuoi = maPhieu;
      quyetDinhCuoi = quyetDinh;
      maNhanVienDuyetCuoi = maNhanVienDuyet;
      ngayDuyetCuoi = ngayDuyet;
      yKienCuoi = yKien;
      return ketQuaCapNhat;
    }

    @Override
    public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
      return Set.of();
    }

    @Override
    public Optional<PhieuTra> findById(String maPhieuTra) {
      return Optional.empty();
    }

    @Override
    public List<ChiTietPhieuTra> findChiTiet(String maPhieuTra) {
      return List.of();
    }

    @Override
    public List<PhieuDuyet> findChoHoanTat() {
      return List.of();
    }

    @Override
    public String taoPhieuTra(
        Connection ketNoi, PhieuTra phieuTra, List<ChiTietPhieuTra> chiTiet) {
      return null;
    }

    @Override
    public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuTra> chiTiet) {
      return 0;
    }

    @Override
    public boolean capNhatHoanTat(
        Connection ketNoi, String maPhieuTra, LocalDate ngayHoanTat) {
      return false;
    }
  }

  /** DAO phiếu đổi giả ghi lại tham số của lần gọi gần nhất. */
  private static final class FakePhieuDoiDao implements PhieuDoiDao {

    private List<PhieuDuyet> danhSach = new ArrayList<>();
    private boolean ketQuaCapNhat = true;
    private int soLanTim;
    private int soLanCapNhat;
    private TrangThaiDuyet quyetDinhCuoi;
    private String maNhanVienDuyetCuoi;
    private LocalDate ngayDuyetCuoi;
    private String yKienCuoi;

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      soLanTim++;
      return danhSach;
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      soLanCapNhat++;
      quyetDinhCuoi = quyetDinh;
      maNhanVienDuyetCuoi = maNhanVienDuyet;
      ngayDuyetCuoi = ngayDuyet;
      yKienCuoi = yKien;
      return ketQuaCapNhat;
    }

    @Override
    public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
      return Set.of();
    }

    @Override
    public Optional<PhieuDoi> findById(String maPhieuDoi) {
      return Optional.empty();
    }

    @Override
    public List<ChiTietPhieuDoi> findChiTiet(String maPhieuDoi) {
      return List.of();
    }

    @Override
    public List<PhieuDuyet> findChoHoanTat() {
      return List.of();
    }

    @Override
    public String taoPhieuDoi(
        Connection ketNoi, PhieuDoi phieuDoi, List<ChiTietPhieuDoi> chiTiet) {
      return null;
    }

    @Override
    public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
      return 0;
    }

    @Override
    public int danhDauMayThayThe(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
      return 0;
    }

    @Override
    public boolean capNhatHoanTat(
        Connection ketNoi, String maPhieuDoi, LocalDate ngayHoanTat) {
      return false;
    }
  }
}
