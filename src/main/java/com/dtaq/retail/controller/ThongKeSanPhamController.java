package com.dtaq.retail.controller;

import com.dtaq.retail.dao.ThongKeSanPhamDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.util.ExcelExporter;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Điều phối nghiệp vụ UC015: doanh số theo sản phẩm và xuất Excel. */
public final class ThongKeSanPhamController {

  private static final Logger log = LoggerFactory.getLogger(ThongKeSanPhamController.class);
  private static final String LOI_QUYEN = "Chỉ Quản lý được xem thống kê doanh số theo sản phẩm";
  private static final String TIEU_DE_BAO_CAO = "Thống kê doanh số theo sản phẩm";

  private final ThongKeSanPhamDao thongKeSanPhamDao;

  public ThongKeSanPhamController(ThongKeSanPhamDao thongKeSanPhamDao) {
    this.thongKeSanPhamDao = Objects.requireNonNull(thongKeSanPhamDao, "thongKeSanPhamDao");
  }

  public KetQua<BaoCao> xemBaoCao(LocalDate tuNgay, LocalDate denNgay, String maDongSP) {
    yeuCauQuanLy();
    if (tuNgay == null || denNgay == null) {
      return KetQua.loi("Kỳ báo cáo không được để trống");
    }
    if (tuNgay.isAfter(denNgay)) {
      return KetQua.loi("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
    }
    try {
      List<ThongKeSanPhamDao.DongThongKe> chiTiet = thongKeSanPhamDao.thongKe(tuNgay, denNgay, maDongSP);
      BaoCao baoCao = new BaoCao(
          chiTiet,
          chiTiet.stream().mapToLong(ThongKeSanPhamDao.DongThongKe::soLuongBan).sum(),
          chiTiet.stream().map(ThongKeSanPhamDao.DongThongKe::doanhSoSauDieuChinh)
              .reduce(BigDecimal.ZERO, BigDecimal::add),
          chiTiet.stream().mapToLong(ThongKeSanPhamDao.DongThongKe::soLuongTra).sum());
      return KetQua.thanhCong(baoCao, "Tạo báo cáo doanh số theo sản phẩm thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi thống kê doanh số theo sản phẩm", e);
      return KetQua.loi("Không thể tải báo cáo doanh số theo sản phẩm: " + e.getMessage());
    }
  }

  /** UC015 9.a: các giao dịch đã dùng để tổng hợp một dòng sản phẩm trong kỳ. */
  public KetQua<List<ThongKeSanPhamDao.GiaoDichDoiChieu>> doiChieu(
      LocalDate tuNgay, LocalDate denNgay, String maDongSP) {
    yeuCauQuanLy();
    if (tuNgay == null || denNgay == null || tuNgay.isAfter(denNgay)) {
      return KetQua.loi("Kỳ báo cáo không hợp lệ");
    }
    if (maDongSP == null || maDongSP.isBlank()) {
      return KetQua.loi("Vui lòng chọn dòng sản phẩm cần đối chiếu");
    }
    try {
      return KetQua.thanhCong(thongKeSanPhamDao.doiChieu(tuNgay, denNgay, maDongSP), null);
    } catch (DataAccessException e) {
      log.error("Lỗi khi tải giao dịch đối chiếu của {}", maDongSP, e);
      return KetQua.loi("Không thể tải giao dịch đối chiếu: " + e.getMessage());
    }
  }

  public KetQua<Void> xuatExcel(File tep, List<ThongKeSanPhamDao.DongThongKe> danhSachHienThi) {
    yeuCauQuanLy();
    if (tep == null) {
      return KetQua.loi("Tệp xuất Excel không được để trống");
    }
    if (danhSachHienThi == null) {
      return KetQua.loi("Danh sách báo cáo không được để trống");
    }

    List<String> headers = List.of(
        "Mã dòng SP",
        "Tên dòng sản phẩm",
        "Danh mục",
        "SL bán",
        "Tiền trước thuế",
        "SL trả",
        "Doanh số sau điều chỉnh");
    List<Object[]> rows = new ArrayList<>(danhSachHienThi.size());
    for (ThongKeSanPhamDao.DongThongKe dong : danhSachHienThi) {
      if (dong == null) {
        return KetQua.loi("Dữ liệu báo cáo không hợp lệ");
      }
      rows.add(new Object[] {
          dong.maDongSP(),
          dong.tenDongSP(),
          dong.tenDanhMuc(),
          dong.soLuongBan(),
          dong.tienTruocThue(),
          dong.soLuongTra(),
          dong.doanhSoSauDieuChinh()
      });
    }

    try {
      ExcelExporter.export(tep, TIEU_DE_BAO_CAO, headers, rows);
      return KetQua.thanhCong(null, "Xuất báo cáo Excel thành công");
    } catch (RuntimeException e) {
      log.error("Lỗi khi xuất thống kê doanh số theo sản phẩm ra Excel", e);
      return KetQua.loi("Không thể xuất tệp Excel: " + e.getMessage());
    }
  }

  private static void yeuCauQuanLy() {
    boolean laQuanLy = CurrentSession.get()
        .map(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElse(false);
    if (!laQuanLy) {
      throw new SecurityException(LOI_QUYEN);
    }
  }

  public record BaoCao(
      List<ThongKeSanPhamDao.DongThongKe> chiTiet,
      long tongSoLuongBan,
      BigDecimal tongDoanhSo,
      long tongSoLuongTra) {
    public BaoCao {
      chiTiet = List.copyOf(chiTiet);
    }
  }

  public record KetQua<T>(boolean thanhCong, String thongBao, T duLieu) {
    public static <T> KetQua<T> thanhCong(T duLieu, String thongBao) {
      return new KetQua<>(true, thongBao, duLieu);
    }

    public static <T> KetQua<T> loi(String thongBao) {
      return new KetQua<>(false, thongBao, null);
    }
  }
}
