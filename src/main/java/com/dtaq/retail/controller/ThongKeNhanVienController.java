package com.dtaq.retail.controller;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.ThongKeNhanVienDao;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DieuChinhDoiChieu;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DongThongKeNhanVien;
import com.dtaq.retail.dao.ThongKeNhanVienDao.HoaDonDoiChieu;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.exception.ExcelExportException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.util.ExcelExporter;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thống kê doanh số theo nhân viên (UC016). Chỉ đọc dữ liệu giao dịch; chỉ Nhân viên quản lý được
 * sử dụng. Các method truy vấn ném {@link SecurityException} khi sai quyền và {@link
 * DataAccessException} khi không đọc được dữ liệu.
 */
public final class ThongKeNhanVienController {

  /** Tiêu đề cột của bảng thống kê và tệp Excel, theo đúng thứ tự trên màn hình. */
  public static final List<String> TIEU_DE_COT = List.of(
      "Mã nhân viên", "Họ và tên", "Số hóa đơn", "Doanh số trước thuế (VND)",
      "Điều chỉnh đổi/trả (VND)", "Sau điều chỉnh (VND)");

  private static final Logger log = LoggerFactory.getLogger(ThongKeNhanVienController.class);

  private static final String LOI_QUYEN = "Chức năng này chỉ dành cho nhân viên quản lý";
  private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final ThongKeNhanVienDao thongKeDao;
  private final NhanVienDao nhanVienDao;

  /**
   * Khởi tạo bộ điều khiển thống kê.
   *
   * @param thongKeDao lớp truy vấn thống kê; không được {@code null}
   * @param nhanVienDao lớp truy cập nhân viên, dùng cho bộ lọc phạm vi nhân viên; không được {@code null}
   */
  public ThongKeNhanVienController(ThongKeNhanVienDao thongKeDao, NhanVienDao nhanVienDao) {
    this.thongKeDao = Objects.requireNonNull(thongKeDao, "thongKeDao");
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
  }

  /**
   * Danh sách nhân viên cho bộ lọc phạm vi (bước 3), gồm cả tài khoản đã khóa để xem lịch sử.
   *
   * @return danh sách nhân viên theo mã
   */
  public List<NhanVien> danhSachNhanVien() {
    yeuCauQuanLy();
    return nhanVienDao.findAll();
  }

  /**
   * Thống kê theo nhân viên trong kỳ (bước 5–10). Kỳ đã được {@link DateRange} kiểm tra hợp lệ
   * (luồng 5.a); danh sách rỗng ứng với luồng 6.a.
   *
   * @param ky kỳ thống kê; không được {@code null}
   * @param maNV mã nhân viên cần xem; {@code null} hoặc rỗng để xem tất cả
   * @return báo cáo kèm tổng cộng; không bao giờ {@code null}
   */
  public BaoCao thongKe(DateRange ky, String maNV) {
    yeuCauQuanLy();
    Objects.requireNonNull(ky, "ky");
    String loc = maNV == null || maNV.isBlank() ? null : maNV;
    List<DongThongKeNhanVien> dong = thongKeDao.thongKe(ky.tuNgay(), ky.denNgay(), loc);
    return new BaoCao(ky, dong);
  }

  /**
   * Hóa đơn và điều chỉnh đổi/trả dùng để tính doanh số của một nhân viên (luồng 10.a).
   *
   * @param ky kỳ thống kê; không được {@code null}
   * @param maNV mã nhân viên cần đối chiếu; không được {@code null}
   * @return dữ liệu đối chiếu; không bao giờ {@code null}
   */
  public DoiChieu doiChieu(DateRange ky, String maNV) {
    yeuCauQuanLy();
    Objects.requireNonNull(ky, "ky");
    Objects.requireNonNull(maNV, "maNV");
    return new DoiChieu(
        thongKeDao.hoaDonTrongKy(ky.tuNgay(), ky.denNgay(), maNV),
        thongKeDao.dieuChinhTrongKy(ky.tuNgay(), ky.denNgay(), maNV));
  }

  /**
   * Xuất đúng báo cáo đang hiển thị ra Excel, kèm dòng tổng cộng. Lỗi ghi tệp chỉ được báo lại,
   * báo cáo trên màn hình không bị ảnh hưởng.
   *
   * @param tep tệp đích
   * @param baoCao báo cáo đang hiển thị
   * @return thông báo lỗi tiếng Việt, hoặc {@code null} nếu xuất thành công
   */
  public String xuatExcel(File tep, BaoCao baoCao) {
    yeuCauQuanLy();
    if (tep == null || baoCao == null) {
      return "Chưa có tệp hoặc báo cáo để xuất";
    }
    List<Object[]> rows = new ArrayList<>();
    for (DongThongKeNhanVien d : baoCao.dong()) {
      rows.add(new Object[] {d.maNV(), d.hoTen(), d.soHoaDon(), d.doanhSo(), d.dieuChinh(), d.sauDieuChinh()});
    }
    rows.add(new Object[] {"Tổng cộng", null, baoCao.tongSoHoaDon(), baoCao.tongDoanhSo(),
        baoCao.tongDieuChinh(), baoCao.tongSauDieuChinh()});
    String tieuDe = "Thống kê doanh số theo nhân viên từ " + NGAY.format(baoCao.ky().tuNgay())
        + " đến " + NGAY.format(baoCao.ky().denNgay());
    try {
      ExcelExporter.export(tep, tieuDe, TIEU_DE_COT, rows);
      return null;
    } catch (ExcelExportException | IllegalArgumentException e) {
      log.warn("Không thể xuất báo cáo doanh số theo nhân viên (exception={})", e.getClass().getName());
      return "Không thể ghi tệp Excel. Kiểm tra tệp có đang mở hoặc thư mục có quyền ghi không";
    }
  }

  private static void yeuCauQuanLy() {
    boolean duocPhep = CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .isPresent();
    if (!duocPhep) {
      throw new SecurityException(LOI_QUYEN);
    }
  }

  /**
   * Báo cáo đã tổng hợp. Tổng cộng là tổng các giá trị dòng đã làm tròn sẵn, không làm tròn lại.
   *
   * @param ky kỳ thống kê
   * @param dong các dòng theo nhân viên
   */
  public record BaoCao(DateRange ky, List<DongThongKeNhanVien> dong) {

    public BaoCao {
      dong = List.copyOf(dong);
    }

    public int tongSoHoaDon() {
      return dong.stream().mapToInt(DongThongKeNhanVien::soHoaDon).sum();
    }

    public BigDecimal tongDoanhSo() {
      return dong.stream().map(DongThongKeNhanVien::doanhSo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal tongDieuChinh() {
      return dong.stream().map(DongThongKeNhanVien::dieuChinh).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal tongSauDieuChinh() {
      return tongDoanhSo().subtract(tongDieuChinh());
    }
  }

  /**
   * Dữ liệu đối chiếu của một nhân viên.
   *
   * @param hoaDons hóa đơn đã thanh toán trong kỳ
   * @param dieuChinhs phiếu trả hoàn tất trong kỳ trên hóa đơn của nhân viên
   */
  public record DoiChieu(List<HoaDonDoiChieu> hoaDons, List<DieuChinhDoiChieu> dieuChinhs) {
  }
}
