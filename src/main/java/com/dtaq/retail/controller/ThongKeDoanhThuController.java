package com.dtaq.retail.controller;

import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.ThongKeDoanhThuDao;
import com.dtaq.retail.dao.ThongKeDoanhThuDao.DongDoanhThu;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.component.BarChartPanel;
import com.dtaq.retail.ui.component.DonutChartPanel;
import com.dtaq.retail.ui.page.report.RevenueStatisticsPage.RevenueSummary;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.util.ExcelExporter;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Điều phối thống kê doanh thu UC017 từ snapshot hóa đơn và phiếu trả. */
public final class ThongKeDoanhThuController {
  private static final List<String> COT_EXCEL = List.of("Ngày", "Dòng sản phẩm", "Tiền hàng trước thuế (VND)", "Thuế (VND)", "Tổng thanh toán (VND)");
  private final ThongKeDoanhThuDao doanhThuDao;
  private final HoaDonDao hoaDonDao;

  public ThongKeDoanhThuController(ThongKeDoanhThuDao doanhThuDao, HoaDonDao hoaDonDao) {
    this.doanhThuDao = Objects.requireNonNull(doanhThuDao, "doanhThuDao");
    this.hoaDonDao = Objects.requireNonNull(hoaDonDao, "hoaDonDao");
  }

  public BaoCao xem(DateRange ky, int nhomThoiGian) {
    yeuCauQuanLy();
    Objects.requireNonNull(ky, "ky");
    if (nhomThoiGian < 0 || nhomThoiGian > 2) throw new IllegalArgumentException("Nhóm thời gian không hợp lệ");
    List<DongDoanhThu> ban = doanhThuDao.doanhThu(ky.tuNgay(), ky.denNgay());
    List<DongDoanhThu> tra = doanhThuDao.dieuChinh(ky.tuNgay(), ky.denNgay());
    List<DongDoanhThu> tatCa = new ArrayList<>(ban);
    tatCa.addAll(tra);
    BigDecimal truocThue = tong(tatCa, false);
    BigDecimal thue = tong(tatCa, true);
    Set<String> maPhieu = tra.stream().map(DongDoanhThu::maPhieu).collect(Collectors.toSet());
    List<BarChartPanel.BarPoint> cot = taoBieuDo(ky, nhomThoiGian, tatCa);
    Map<String, BigDecimal> theoDong = tatCa.stream().collect(Collectors.groupingBy(
        DongDoanhThu::maDongSP, LinkedHashMap::new,
        Collectors.mapping(r -> r.truocThue().add(r.thue()),
            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    List<DonutChartPanel.DonutSlice> donut = theoDong.entrySet().stream()
        .filter(e -> e.getValue().signum() > 0)
        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
        .limit(5)
        .map(e -> new DonutChartPanel.DonutSlice(e.getKey(), e.getValue().longValue(), DtaqColors.PRIMARY))
        .toList();
    long soHoaDon = hoaDonDao.findDaThanhToanTrongKy(ky.tuNgay(), ky.denNgay()).size();
    RevenueSummary kpi = new RevenueSummary(truocThue.add(thue).longValue(), truocThue.longValue(), thue.longValue(),
        Math.toIntExact(soHoaDon), maPhieu.size(), null, null);
    List<DongDoanhThu> dongExcel = List.copyOf(tatCa);
    return new BaoCao(kpi, cot, donut, dongExcel);
  }

  private static BigDecimal tong(List<DongDoanhThu> rows, boolean laThue) {
    return rows.stream().map(r -> laThue ? r.thue() : r.truocThue()).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static List<BarChartPanel.BarPoint> taoBieuDo(DateRange ky, int nhom, List<DongDoanhThu> rows) {
    Map<LocalDate, BigDecimal> theoNgay = rows.stream().collect(Collectors.groupingBy(
        DongDoanhThu::ngay, Collectors.mapping(r -> r.truocThue().add(r.thue()),
            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    Map<LocalDate, BigDecimal> gop = new LinkedHashMap<>();
    for (LocalDate d = ky.tuNgay(); !d.isAfter(ky.denNgay()); d = d.plusDays(1)) {
      LocalDate moc = switch (nhom) {
        case 1 -> d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        case 2 -> d.withDayOfMonth(1);
        default -> d;
      };
      gop.merge(moc, theoNgay.getOrDefault(d, BigDecimal.ZERO), BigDecimal::add);
    }
    return gop.entrySet().stream().map(e -> new BarChartPanel.BarPoint(
        nhom == 2 ? String.format("%02d/%d", e.getKey().getMonthValue(), e.getKey().getYear())
            : nhom == 1 ? e.getKey().toString() : String.format("%02d/%02d", e.getKey().getDayOfMonth(), e.getKey().getMonthValue()),
        e.getValue().movePointLeft(6).doubleValue())).toList();
  }

  public void xuatExcel(File tep, BaoCao baoCao) {
    yeuCauQuanLy();
    if (tep == null || baoCao == null) throw new IllegalArgumentException("Dữ liệu xuất Excel không hợp lệ");
    List<Object[]> rows = baoCao.dongExcel().stream().map(r -> new Object[] {r.ngay(), r.maDongSP(), r.truocThue(), r.thue(), r.truocThue().add(r.thue())}).toList();
    ExcelExporter.export(tep, "Thống kê doanh thu", COT_EXCEL, rows);
  }

  private static void yeuCauQuanLy() {
    boolean laQuanLy = CurrentSession.get().map(n -> n.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY).orElse(false);
    if (!laQuanLy) throw new SecurityException("Chỉ Quản lý được xem thống kê doanh thu");
  }

  public record BaoCao(RevenueSummary kpi, List<BarChartPanel.BarPoint> doanhThuTheoThoiGian,
      List<DonutChartPanel.DonutSlice> topDongSanPham, List<DongDoanhThu> dongExcel) {
    public BaoCao { doanhThuTheoThoiGian = List.copyOf(doanhThuTheoThoiGian); topDongSanPham = List.copyOf(topDongSanPham); dongExcel = List.copyOf(dongExcel); }
  }
}
