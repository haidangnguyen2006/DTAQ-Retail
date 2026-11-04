package com.dtaq.retail.controller;

import com.dtaq.retail.dao.BaoCaoTonKhoDao;
import com.dtaq.retail.dao.BaoCaoTonKhoDao.DongTonLau;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.exception.ExcelExportException;
import com.dtaq.retail.util.ExcelExporter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Điều phối nghiệp vụ UC014: truy vấn, tổng hợp và xuất báo cáo tồn kho lâu ngày. */
public final class BaoCaoTonKhoController {

  private static final Logger log = LoggerFactory.getLogger(BaoCaoTonKhoController.class);
  private static final String TIEU_DE_BAO_CAO = "Báo cáo sản phẩm tồn kho lâu ngày";
  private static final List<String> TIEU_DE_COT = List.of(
      "Mã dòng sản phẩm", "Tên dòng sản phẩm", "Danh mục", "Số serial",
      "Ngày nhập kho", "Trạng thái", "Số ngày tồn");

  private final BaoCaoTonKhoDao baoCaoTonKhoDao;

  public BaoCaoTonKhoController(BaoCaoTonKhoDao baoCaoTonKhoDao) {
    this.baoCaoTonKhoDao = Objects.requireNonNull(baoCaoTonKhoDao, "baoCaoTonKhoDao");
  }

  /** Truy vấn báo cáo và tổng hợp các KPI cho danh sách máy trả về. */
  public KetQua<BaoCao> xemBaoCao(int nguongNgay, boolean chiTinhSanSangBan) {
    if (nguongNgay <= 0) {
      return KetQua.loi("Ngưỡng ngày tồn kho phải là số nguyên dương");
    }
    try {
      List<DongTonLau> chiTiet = baoCaoTonKhoDao.findTonLau(nguongNgay, chiTinhSanSangBan);
      BaoCao baoCao = new BaoCao(
          chiTiet,
          chiTiet.size(),
          (int) chiTiet.stream().map(DongTonLau::maDongSP).distinct().count(),
          chiTiet.stream().mapToLong(DongTonLau::soNgayTon).max().orElse(0));
      return KetQua.thanhCong(baoCao, "Tạo báo cáo tồn kho lâu ngày thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi truy vấn báo cáo tồn kho lâu ngày", e);
      return KetQua.loi("Không thể tải báo cáo tồn kho lâu ngày: " + e.getMessage());
    }
  }

  /** Xuất đúng danh sách đang hiển thị; thao tác này không thay đổi dữ liệu báo cáo. */
  public KetQua<Void> xuatExcel(File tep, List<DongTonLau> danhSachHienThi) {
    if (tep == null) {
      return KetQua.loi("Tệp xuất Excel không được để trống");
    }
    if (danhSachHienThi == null) {
      return KetQua.loi("Danh sách báo cáo không được để trống");
    }

    List<Object[]> rows = new ArrayList<>(danhSachHienThi.size());
    for (DongTonLau dong : danhSachHienThi) {
      if (dong == null) {
        return KetQua.loi("Dữ liệu báo cáo không hợp lệ");
      }
      rows.add(new Object[] {
          dong.maDongSP(), dong.tenDongSP(), dong.tenDanhMuc(), dong.soSerial(),
          dong.ngayNhapKhoBanDau(), dong.trangThai().name(), dong.soNgayTon()
      });
    }

    try {
      ExcelExporter.export(tep, TIEU_DE_BAO_CAO, TIEU_DE_COT, rows);
      return KetQua.thanhCong(null, "Xuất báo cáo Excel thành công");
    } catch (ExcelExportException | IllegalArgumentException e) {
      log.error("Lỗi khi xuất báo cáo tồn kho lâu ngày ra Excel", e);
      return KetQua.loi("Không thể xuất tệp Excel: " + e.getMessage());
    }
  }

  /** Kết quả tổng hợp; danh sách chi tiết được giữ bất biến với phía giao diện. */
  public record BaoCao(
      List<DongTonLau> chiTiet,
      int tongSoMay,
      int soDongSanPham,
      long soNgayTonLonNhat) {
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
