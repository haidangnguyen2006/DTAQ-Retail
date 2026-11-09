package com.dtaq.retail.ui.page.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.controller.PheDuyetDoiTraController;
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
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.shell.PageId;
import java.lang.reflect.Field;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử màn hình phê duyệt yêu cầu đổi trả với bộ điều khiển thật và DAO giả.
 */
class ReturnApprovalsPageTest {

  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Clock DONG_HO_CO_DINH =
      Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), MUI_GIO);

  private static final NhanVien QUAN_LY =
      nhanVien("NVQUANLY-2026-001", LoaiQuyen.NHAN_VIEN_QUAN_LY);
  private static final NhanVien BAN_HANG =
      nhanVien("NVBANHANG-2026-001", LoaiQuyen.NHAN_VIEN_BAN_HANG);

  private final FakePhieuTraDao phieuTraDao = new FakePhieuTraDao();
  private final FakePhieuDoiDao phieuDoiDao = new FakePhieuDoiDao();

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Trang có đúng định danh và hiển thị đủ 4 phiếu chờ duyệt")
  void hienThiDanhSachChoDuyet() throws Exception {
    CurrentSession.set(QUAN_LY);
    phieuTraDao.danhSach = new ArrayList<>(List.of(
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000010", LocalDate.of(2026, 3, 13),
            new BigDecimal("12096000")),
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000011", LocalDate.of(2026, 3, 21),
            new BigDecimal("5000000"))));
    phieuDoiDao.danhSach = new ArrayList<>(List.of(
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_DOI, "PD-2026-000008", LocalDate.of(2026, 6, 4),
            BigDecimal.ZERO),
        phieu(PhieuDuyet.LoaiPhieu.PHIEU_DOI, "PD-2026-000009", LocalDate.of(2026, 6, 12),
            BigDecimal.ZERO)));

    ReturnApprovalsPage page = new ReturnApprovalsPage(controller());

    assertEquals(PageId.RETURN_APPROVALS, page.id());
    assertEquals("Phê duyệt yêu cầu đổi trả", page.title());
    assertEquals("Quản lý", page.section());
    assertNotNull(page.view());

    SwingUtilities.invokeAndWait(page::onShow);
    choTrangThai(state(page), s -> s.getState() == StatefulView.State.CONTENT);

    DefaultTableModel model = model(page);
    assertEquals(4, model.getRowCount());

    int dongPhieuTra = timDong(model, "PT-2026-000010");
    assertTrue(dongPhieuTra >= 0, "phải có dòng phiếu trả PT-2026-000010");
    assertEquals("Trả hàng", model.getValueAt(dongPhieuTra, 1));
    assertEquals(12096000L, model.getValueAt(dongPhieuTra, 5));

    int dongPhieuDoi = timDong(model, "PD-2026-000008");
    assertTrue(dongPhieuDoi >= 0, "phải có dòng phiếu đổi PD-2026-000008");
    assertEquals("Đổi hàng", model.getValueAt(dongPhieuDoi, 1));
    assertEquals(0L, model.getValueAt(dongPhieuDoi, 5));
  }

  @Test
  @DisplayName("Không có phiếu chờ duyệt thì hiện trạng thái rỗng")
  void danhSachRongHienTrangThaiRong() throws Exception {
    CurrentSession.set(QUAN_LY);

    ReturnApprovalsPage page = new ReturnApprovalsPage(controller());

    SwingUtilities.invokeAndWait(page::onShow);
    choTrangThai(state(page), s -> s.getState() == StatefulView.State.EMPTY);

    assertEquals(0, model(page).getRowCount());
  }

  @Test
  @DisplayName("Nhân viên bán hàng thấy thông báo lỗi quyền, không có ngoại lệ thoát ra")
  void khongPhaiQuanLyHienLoi() throws Exception {
    CurrentSession.set(BAN_HANG);

    ReturnApprovalsPage page = new ReturnApprovalsPage(controller());

    SwingUtilities.invokeAndWait(page::onShow);
    choTrangThai(state(page), s -> s.getState() == StatefulView.State.ERROR);

    assertEquals(StatefulView.State.ERROR, state(page).getState());
    assertEquals(0, model(page).getRowCount());
  }

  private static int timDong(DefaultTableModel model, String maPhieu) {
    for (int row = 0; row < model.getRowCount(); row++) {
      if (maPhieu.equals(model.getValueAt(row, 0))) {
        return row;
      }
    }
    return -1;
  }

  private static void choTrangThai(StatefulView view, Predicate<StatefulView> dieuKien)
      throws Exception {
    long han = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    AtomicReference<Boolean> dat = new AtomicReference<>(false);
    while (System.nanoTime() < han) {
      SwingUtilities.invokeAndWait(() -> dat.set(dieuKien.test(view)));
      if (Boolean.TRUE.equals(dat.get())) {
        return;
      }
      LockSupport.parkNanos(2_000_000L);
    }
    SwingUtilities.invokeAndWait(() -> dat.set(dieuKien.test(view)));
    assertTrue(Boolean.TRUE.equals(dat.get()), "Hết thời gian chờ trạng thái giao diện");
  }

  private static DefaultTableModel model(ReturnApprovalsPage page) {
    return (DefaultTableModel) truong(page, "tableModel");
  }

  private static StatefulView state(ReturnApprovalsPage page) {
    return (StatefulView) truong(page, "tableState");
  }

  private static Object truong(ReturnApprovalsPage page, String tenTruong) {
    try {
      Field field = ReturnApprovalsPage.class.getDeclaredField(tenTruong);
      field.setAccessible(true);
      return field.get(page);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Không truy cập được trường " + tenTruong, e);
    }
  }

  private PheDuyetDoiTraController controller() {
    return new PheDuyetDoiTraController(phieuTraDao, phieuDoiDao, DONG_HO_CO_DINH);
  }

  private static PhieuDuyet phieu(
      PhieuDuyet.LoaiPhieu loai, String maPhieu, LocalDate ngayTao, BigDecimal hoanDuKien) {
    return new PhieuDuyet(loai, maPhieu, "HD-2026-000010", "Nguyễn Văn An", "0902100001",
        hoanDuKien, "Trần Văn Duy", "Khách yêu cầu trả máy", ngayTao);
  }

  private static NhanVien nhanVien(String maNV, LoaiQuyen loaiQuyen) {
    return new NhanVien(maNV, "Nhân viên " + maNV, "0900000001", maNV + "@dtaq.example",
        LocalDate.of(2026, 1, 1), maNV, "matkhau", loaiQuyen, true);
  }

  /** DAO phiếu trả giả trả về danh sách cấu hình sẵn. */
  private static final class FakePhieuTraDao implements PhieuTraDao {

    private List<PhieuDuyet> danhSach = new ArrayList<>();

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      return danhSach;
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      return true;
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

  /** DAO phiếu đổi giả trả về danh sách cấu hình sẵn. */
  private static final class FakePhieuDoiDao implements PhieuDoiDao {

    private List<PhieuDuyet> danhSach = new ArrayList<>();

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      return danhSach;
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      return true;
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
