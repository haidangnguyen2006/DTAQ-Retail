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
import com.dtaq.retail.util.TienUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
      "Điều chỉnh đổi/trả (VND)", "Sau điều chỉnh (VND)", "TB mỗi hóa đơn (VND)", "Tỉ lệ hàng trả",
      "Tỉ trọng doanh số");

  private static final Logger log = LoggerFactory.getLogger(ThongKeNhanVienController.class);

  private static final String LOI_QUYEN = "Chức năng này chỉ dành cho nhân viên quản lý";
  private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");

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
      rows.add(new Object[] {d.maNV(), d.hoTen(), d.soHoaDon(), d.doanhSo(), d.dieuChinh(), d.sauDieuChinh(),
          trungBinhHoaDon(d.sauDieuChinh(), d.soHoaDon()),
          dinhDangPhanTram(phanTram(d.dieuChinh(), d.doanhSo())),
          dinhDangPhanTram(phanTram(d.sauDieuChinh(), baoCao.tongSauDieuChinh()))});
    }
    rows.add(new Object[] {"Tổng cộng", null, baoCao.tongSoHoaDon(), baoCao.tongDoanhSo(),
        baoCao.tongDieuChinh(), baoCao.tongSauDieuChinh(),
        trungBinhHoaDon(baoCao.tongSauDieuChinh(), baoCao.tongSoHoaDon()),
        dinhDangPhanTram(phanTram(baoCao.tongDieuChinh(), baoCao.tongDoanhSo())), "100%"});
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
  // ---------------------------------------------------------------- phân tích (insight)

  /**
   * Thống kê kỳ đã chọn kèm kỳ liền trước cùng độ dài và các nhận xét rút ra từ số liệu.
   *
   * @param ky kỳ báo cáo
   * @param maNV mã nhân viên, hoặc {@code null}/rỗng để lấy tất cả
   * @return phân tích; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   * @throws DataAccessException nếu không đọc được dữ liệu
   */
  public PhanTich phanTich(DateRange ky, String maNV) {
    BaoCao hienTai = thongKe(ky, maNV);
    long soNgay = ChronoUnit.DAYS.between(ky.tuNgay(), ky.denNgay()) + 1;
    BaoCao kyTruoc = thongKe(
        new DateRange(ky.tuNgay().minusDays(soNgay), ky.tuNgay().minusDays(1)), maNV);
    return new PhanTich(hienTai, kyTruoc, nhanXet(hienTai, kyTruoc));
  }

  /** Doanh số thực (sau điều chỉnh) chia số hóa đơn, làm tròn VND HALF_UP; 0 nếu không có hóa đơn. */
  public static BigDecimal trungBinhHoaDon(BigDecimal doanhSo, int soHoaDon) {
    return soHoaDon == 0 ? BigDecimal.ZERO
        : doanhSo.divide(BigDecimal.valueOf(soHoaDon), 0, RoundingMode.HALF_UP);
  }

  /** Tỉ lệ phần trăm {@code tu / mau}, một chữ số thập phân; {@code null} nếu mẫu bằng 0. */
  public static BigDecimal phanTram(BigDecimal tu, BigDecimal mau) {
    return mau.signum() == 0 ? null
        : tu.multiply(BigDecimal.valueOf(100)).divide(mau, 1, RoundingMode.HALF_UP);
  }

  /** Định dạng phần trăm kiểu Việt Nam: "12,5%"; {@code null} thành "—". */
  public static String dinhDangPhanTram(BigDecimal phanTram) {
    return phanTram == null ? "—" : phanTram.stripTrailingZeros().toPlainString().replace('.', ',') + "%";
  }

  /**
   * Rút ra nhận xét có thể hành động từ số liệu: xu hướng so với kỳ trước, người dẫn đầu và mức phụ
   * thuộc, tỉ lệ hàng trả bất thường, giá trị trung bình hóa đơn và biến động từng người.
   */
  static List<NhanXet> nhanXet(BaoCao hienTai, BaoCao kyTruoc) {
    List<NhanXet> ketQua = new ArrayList<>();
    List<DongThongKeNhanVien> dong = hienTai.dong();
    if (dong.isEmpty()) {
      return ketQua;
    }
    BigDecimal tong = hienTai.tongSauDieuChinh();
    BigDecimal tongTruoc = kyTruoc.tongSauDieuChinh();
    String kyTruocText = NGAY_NGAN.format(kyTruoc.ky().tuNgay()) + "–" + NGAY_NGAN.format(kyTruoc.ky().denNgay());

    // 1. Xu hướng tổng.
    BigDecimal bienDong = phanTram(tong.subtract(tongTruoc), tongTruoc.abs());
    if (bienDong != null && bienDong.signum() == 0) {
      ketQua.add(new NhanXet(MucNhanXet.THONG_TIN,
          "Doanh số thực không đổi so với kỳ trước (" + kyTruocText + "): " + TienUtil.dinhDang(tong) + "."));
    } else if (bienDong != null) {
      boolean tang = bienDong.signum() > 0;
      ketQua.add(new NhanXet(tang ? MucNhanXet.TICH_CUC : MucNhanXet.CANH_BAO,
          "Doanh số thực " + (tang ? "tăng " : "giảm ") + dinhDangPhanTram(bienDong.abs())
              + " so với kỳ trước (" + kyTruocText + "): " + TienUtil.dinhDang(tongTruoc)
              + " → " + TienUtil.dinhDang(tong) + "."));
    } else if (tong.signum() > 0) {
      ketQua.add(new NhanXet(MucNhanXet.THONG_TIN,
          "Kỳ trước (" + kyTruocText + ") chưa có doanh số để so sánh."));
    }

    // 2. Người dẫn đầu và mức phụ thuộc.
    DongThongKeNhanVien dau = dong.stream()
        .max(Comparator.comparing(DongThongKeNhanVien::sauDieuChinh)).orElseThrow();
    BigDecimal tyTrongDau = phanTram(dau.sauDieuChinh(), tong);
    if (dong.size() > 1 && tyTrongDau != null && dau.sauDieuChinh().signum() > 0) {
      boolean phuThuoc = dong.size() >= 3 && tyTrongDau.compareTo(BigDecimal.valueOf(50)) > 0;
      ketQua.add(new NhanXet(phuThuoc ? MucNhanXet.CANH_BAO : MucNhanXet.TICH_CUC,
          dau.hoTen() + " dẫn đầu với " + TienUtil.dinhDang(dau.sauDieuChinh()) + ", chiếm "
              + dinhDangPhanTram(tyTrongDau) + " doanh số thực"
              + (phuThuoc ? " — doanh thu đang phụ thuộc nhiều vào một người." : ".")));
    }

    // 3. Tỉ lệ hàng trả bất thường: ít nhất 10% và gấp đôi mức chung.
    BigDecimal tyLeChung = phanTram(hienTai.tongDieuChinh(), hienTai.tongDoanhSo());
    for (DongThongKeNhanVien d : dong) {
      BigDecimal tyLe = phanTram(d.dieuChinh(), d.doanhSo());
      if (tyLe != null && tyLeChung != null && tyLe.compareTo(BigDecimal.TEN) >= 0
          && tyLe.compareTo(tyLeChung.multiply(BigDecimal.TWO)) >= 0) {
        ketQua.add(new NhanXet(MucNhanXet.CANH_BAO, d.hoTen() + " có tỉ lệ hàng trả "
            + dinhDangPhanTram(tyLe) + " (mức chung " + dinhDangPhanTram(tyLeChung)
            + ") — nên rà soát cách tư vấn và kiểm tra máy trước khi giao."));
      }
      if (d.soHoaDon() == 0 && d.dieuChinh().signum() > 0) {
        ketQua.add(new NhanXet(MucNhanXet.CANH_BAO, d.hoTen()
            + " không có hóa đơn mới trong kỳ nhưng phát sinh hàng trả "
            + TienUtil.dinhDang(d.dieuChinh()) + " từ hóa đơn cũ."));
      }
    }

    // 4. Giá trị trung bình mỗi hóa đơn cao nhất.
    int tongHoaDon = hienTai.tongSoHoaDon();
    if (dong.size() > 1 && tongHoaDon > 0) {
      DongThongKeNhanVien aov = dong.stream().filter(d -> d.soHoaDon() > 0)
          .max(Comparator.comparing(d -> trungBinhHoaDon(d.sauDieuChinh(), d.soHoaDon()))).orElseThrow();
      ketQua.add(new NhanXet(MucNhanXet.THONG_TIN, aov.hoTen() + " có giá trị trung bình mỗi hóa đơn cao nhất: "
          + TienUtil.dinhDang(trungBinhHoaDon(aov.sauDieuChinh(), aov.soHoaDon())) + " (mức chung "
          + TienUtil.dinhDang(trungBinhHoaDon(tong, tongHoaDon)) + ") — kinh nghiệm bán kèm, bán nâng cấp đáng chia sẻ."));
    }

    // 5. Biến động mạnh của từng người so với kỳ trước (giảm quá 30%).
    for (DongThongKeNhanVien d : dong) {
      kyTruoc.dong().stream().filter(t -> t.maNV().equals(d.maNV())).findFirst().ifPresent(t -> {
        BigDecimal bd = phanTram(d.sauDieuChinh().subtract(t.sauDieuChinh()), t.sauDieuChinh().abs());
        if (bd != null && bd.compareTo(BigDecimal.valueOf(-30)) < 0) {
          ketQua.add(new NhanXet(MucNhanXet.CANH_BAO, d.hoTen() + " giảm " + dinhDangPhanTram(bd.abs())
              + " doanh số so với kỳ trước — cần trao đổi để tìm nguyên nhân."));
        }
      });
    }
    return ketQua;
  }

  /** Mức độ của một nhận xét; giao diện quyết định màu. */
  public enum MucNhanXet {
    TICH_CUC,
    CANH_BAO,
    THONG_TIN
  }

  /** Một nhận xét rút ra từ số liệu. */
  public record NhanXet(MucNhanXet muc, String noiDung) {
  }

  /** Báo cáo kỳ hiện tại, kỳ liền trước cùng độ dài và các nhận xét. */
  public record PhanTich(BaoCao hienTai, BaoCao kyTruoc, List<NhanXet> nhanXet) {

    public PhanTich {
      nhanXet = List.copyOf(nhanXet);
    }

    /** Biến động phần trăm của chỉ số giữa hai kỳ; {@code null} nếu kỳ trước bằng 0. */
    public BigDecimal bienDong(BigDecimal hienTaiGiaTri, BigDecimal kyTruocGiaTri) {
      return phanTram(hienTaiGiaTri.subtract(kyTruocGiaTri), kyTruocGiaTri.abs());
    }
  }

  public record DoiChieu(List<HoaDonDoiChieu> hoaDons, List<DieuChinhDoiChieu> dieuChinhs) {
  }
}
