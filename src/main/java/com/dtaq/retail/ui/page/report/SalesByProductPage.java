package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.controller.ThongKeSanPhamController;
import com.dtaq.retail.dao.ThongKeSanPhamDao;
import com.dtaq.retail.ui.component.BarChartPanel;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DonutChartPanel;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.KyBaoCaoFilter;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.util.TienUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/** Màn hình thống kê doanh số theo sản phẩm theo UC015. */
public final class SalesByProductPage implements Page {

  private static final String REPORT_TITLE = "Thống kê doanh số theo sản phẩm";
  private static final String EMPTY_TITLE = "Chưa có dữ liệu";
  private static final String EMPTY_HINT = "Chọn kỳ báo cáo để xem doanh số theo sản phẩm.";
  private static final String DEFAULT_PRODUCT_CODE = "DSP-2026-000001";
  private static final LocalDate DEFAULT_FROM_DATE = LocalDate.now().minusDays(89);
  private static final LocalDate DEFAULT_TO_DATE = LocalDate.now();

  private final JPanel view;
  private final ThongKeSanPhamController controller;
  private final KpiCard kpiUnits = new KpiCard("Tổng số lượng bán", "—", "Đã trừ hàng trả");
  private final KpiCard kpiRevenue = new KpiCard("Doanh số sau điều chỉnh", "—", "Tổng tiền trước thuế");
  private final KpiCard kpiReturned = new KpiCard("Tổng số lượng trả", "—", "Hàng hoàn tất");
  private final DtaqButton exportBtn = DtaqButton.secondary("Xuất Excel", "download");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã dòng SP", "Tên dòng sản phẩm", "Danh mục", "SL bán",
      "Tiền trước thuế", "SL trả", "Doanh số sau điều chỉnh", "Khuyến nghị nhập kho"});
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;
  private final KyBaoCaoFilter filter = new KyBaoCaoFilter();
  private final JLabel focusLabel = new JLabel();
  private final BarChartPanel salesChart = new BarChartPanel();
  private final DonutChartPanel revenueMixChart = new DonutChartPanel();
  private List<ThongKeSanPhamDao.DongThongKe> rows = List.of();
  private DateRange currentRange = new DateRange(DEFAULT_FROM_DATE, DEFAULT_TO_DATE);
  private String selectedProductCode = DEFAULT_PRODUCT_CODE;

  public SalesByProductPage(ThongKeSanPhamController controller) {
    this.controller = controller;
    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(REPORT_TITLE,
        "Doanh số các dòng sản phẩm trong kỳ được tổng hợp từ hóa đơn đã thanh toán và phiếu trả hoàn tất.");
    header.setTrailing(exportBtn);
    view.add(header, "growx, wrap");

    JPanel kpiRow = new JPanel(new MigLayout(
        "fillx, insets 0",
        "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]",
        "[]"));
    kpiRow.setOpaque(false);
    com.dtaq.retail.ui.component.Responsive.grid(kpiRow, 240, 3);
    kpiRow.add(kpiUnits);
    kpiRow.add(kpiRevenue);
    kpiRow.add(kpiReturned);
    view.add(kpiRow, "growx, wrap");

    view.add(buildAnalyticsPanel(), "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setNumberColumn(3);
    dataTable.setMoneyColumn(4);
    dataTable.setNumberColumn(5);
    dataTable.setMoneyColumn(6);
    dataTable.setOnRowDoubleClick(modelRow -> moDoiChieu((String) tableModel.getValueAt(modelRow, 0)));
    dataTable.getTable().setToolTipText("Nhấp đúp một dòng để xem các giao dịch dùng để tính");
    dataTable.setCustomRenderer(7, new RecommendationBadgeRenderer());
    dataTable.setSortable(true);
    dataTable.setColumnWidths(120, 240, 150, 90, 160, 90, 160, 220);
    dataTable.getTable().setRowSelectionAllowed(true);
    dataTable.getTable().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    dataTable.getTable().getSelectionModel().addListSelectionListener(event -> {
      if (event.getValueIsAdjusting() || dataTable.getTable().getSelectedRow() < 0) {
        return;
      }
      int viewRow = dataTable.getTable().getSelectedRow();
      int modelRow = dataTable.getTable().convertRowIndexToModel(viewRow);
      String maDongSP = String.valueOf(tableModel.getValueAt(modelRow, 0));
      if (selectedProductCode == null || !selectedProductCode.equals(maDongSP)) {
        selectedProductCode = maDongSP;
        renderRows();
      }
    });

    tableState = new StatefulView(dataTable, "chart-no-axes-combined", EMPTY_TITLE, EMPTY_HINT);
    Card contentCard = Card.standard();
    contentCard.add(buildFilterBar(), "growx, wrap");
    contentCard.setBody(tableState);
    view.add(contentCard, "grow, push");

    filter.setDateRange(DEFAULT_FROM_DATE, DEFAULT_TO_DATE);
    filter.setOnApply(range -> loadReport(range, null));
    exportBtn.addActionListener(e -> exportReport());
    loadReport(new DateRange(DEFAULT_FROM_DATE, DEFAULT_TO_DATE), null);
    tableState.showEmpty("chart-no-axes-combined", EMPTY_TITLE, EMPTY_HINT);
  }

  private JPanel buildFilterBar() {
    JPanel filterBar = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]", "[bottom]"));
    filterBar.setOpaque(false);
    filterBar.add(filter, "growx");
    return filterBar;
  }

  private JPanel buildAnalyticsPanel() {
    JPanel analyticsPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]16[grow,fill]", "[]"));
    analyticsPanel.setOpaque(false);
    com.dtaq.retail.ui.component.Responsive.grid(analyticsPanel, 500, 2);

    JLabel topLabel = new JLabel("Top 6 trong kỳ");
    topLabel.setForeground(DtaqColors.TEXT_MUTED);
    Card salesCard = Card.section("Doanh thu theo dòng sản phẩm", null, topLabel);
    salesCard.setBody(salesChart);
    analyticsPanel.add(salesCard, "growx");

    Card mixCard = Card.section("Tỉ lệ giữa số sản phẩm đã bán & tồn kho", null, focusLabel);
    revenueMixChart.setSoLuong("máy", "Tổng số máy");
    mixCard.setBody(revenueMixChart);
    analyticsPanel.add(mixCard, "growx");
    return analyticsPanel;
  }

  private void loadReport(DateRange ky, String maDongSP) {
    if (controller == null) {
      return;
    }
    this.currentRange = ky;
    if (maDongSP != null && !maDongSP.isBlank()) {
      this.selectedProductCode = maDongSP;
    }
    exportBtn.setEnabled(false);
    tableState.showLoading();
    new SwingWorker<ThongKeSanPhamController.KetQua<ThongKeSanPhamController.BaoCao>, Void>() {
      @Override
      protected ThongKeSanPhamController.KetQua<ThongKeSanPhamController.BaoCao> doInBackground() {
        return controller.xemBaoCao(ky.tuNgay(), ky.denNgay(), maDongSP);
      }

      @Override
      protected void done() {
        try {
          ThongKeSanPhamController.KetQua<ThongKeSanPhamController.BaoCao> result = get();
          if (!result.thanhCong()) {
            tableState.showError(result.thongBao());
            return;
          }
          rows = result.duLieu().chiTiet();
          renderRows();
        } catch (Exception e) {
          tableState.showError("Không thể tải báo cáo doanh số theo sản phẩm.");
        }
      }
    }.execute();
  }

  private void renderRows() {
    tableModel.setRowCount(0);
    // DAO liệt kê mọi dòng sản phẩm (kể cả dòng bằng 0), nên "không phát sinh" là không dòng nào có
    // bán hoặc trả trong kỳ (6.a).
    if (rows.stream().allMatch(r -> r.soLuongBan() == 0 && r.soLuongTra() == 0)) {
      kpiUnits.setValue("0");
      kpiRevenue.setValue(MoneyFormat.format(0L));
      kpiReturned.setValue("0");
      focusLabel.setText("Không có dữ liệu");
      salesChart.setData(List.of());
      revenueMixChart.setData(List.of());
      tableState.showEmpty("chart-no-axes-combined", "Không có phát sinh trong kỳ", "Không có hóa đơn hoặc hàng trả trong khoảng thời gian đã chọn.");
      exportBtn.setEnabled(false);
      return;
    }

    List<ThongKeSanPhamDao.DongThongKe> sortedRows = rows.stream()
        .sorted(Comparator.comparingLong((ThongKeSanPhamDao.DongThongKe row) -> row.doanhSoSauDieuChinh().longValue()).reversed())
        .toList();
    if (selectedProductCode == null || sortedRows.stream().noneMatch(row -> selectedProductCode.equals(row.maDongSP()))) {
      selectedProductCode = sortedRows.getFirst().maDongSP();
    }

    long tongSoLuongBan = 0L;
    long tongSoLuongTra = 0L;
    long tongDoanhSo = 0L;
    var focusRow = sortedRows.stream()
        .filter(row -> selectedProductCode.equals(row.maDongSP()))
        .findFirst()
        .orElse(sortedRows.getFirst());

    for (ThongKeSanPhamDao.DongThongKe row : sortedRows) {
      tableModel.addRow(new Object[] {
          row.maDongSP(),
          row.tenDongSP(),
          row.tenDanhMuc(),
          row.soLuongBan(),
          MoneyFormat.format(row.tienTruocThue()),
          row.soLuongTra(),
          MoneyFormat.format(row.doanhSoSauDieuChinh()),
          recommendationBadge(row.soLuongTon(), row.soLuongBan(), row.soLuongTra(), row.doanhSoSauDieuChinh(), currentRange)
      });
      tongSoLuongBan += row.soLuongBan();
      tongSoLuongTra += row.soLuongTra();
      tongDoanhSo += row.doanhSoSauDieuChinh().longValue();
    }

    kpiUnits.setValue(String.valueOf(tongSoLuongBan));
    kpiRevenue.setValue(MoneyFormat.format(tongDoanhSo));
    kpiReturned.setValue(String.valueOf(tongSoLuongTra));

    if (focusRow != null) {
      focusLabel.setText(focusRow.tenDongSP());
    } else {
      focusLabel.setText("Tất cả dòng SP");
    }

    salesChart.setUnit("Triệu ₫", "tr");
    salesChart.setData(buildPeriodRevenueChart(sortedRows));

    List<Color> palette = List.of(
        DtaqColors.CHART_1,
        DtaqColors.CHART_2,
        DtaqColors.CHART_3,
        DtaqColors.CHART_4,
        DtaqColors.CHART_5);
    List<DonutChartPanel.DonutSlice> slices = sortedRows.stream()
        .limit(5)
        .map(row -> new DonutChartPanel.DonutSlice(row.tenDongSP(), row.soLuongBan(), palette.get(Math.min(palette.size() - 1, sortedRows.indexOf(row)))))
        .toList();
    if (slices.isEmpty()) {
      revenueMixChart.setData(List.of());
    } else {
      ThongKeSanPhamDao.DongThongKe active = focusRow;
      long sold = active.soLuongBan();
      int stock = active.soLuongTon();
      revenueMixChart.setData(List.of(
          new DonutChartPanel.DonutSlice("Đã bán", sold, DtaqColors.CHART_1),
          new DonutChartPanel.DonutSlice("Tồn kho", Math.max(stock, 0), DtaqColors.CHART_2)));
    }

    exportBtn.setEnabled(true);
    tableState.showContent();
  }

  static String recommendationBadge(int soLuongTon, long soLuongBan, long soLuongTra, BigDecimal doanhSoSauDieuChinh, DateRange currentRange) {
    long days = Math.max(1L, ChronoUnit.DAYS.between(currentRange.tuNgay(), currentRange.denNgay()) + 1);
    double avgDailyQty = soLuongBan / (double) days;
    double doi = avgDailyQty > 0 ? (double) soLuongTon / avgDailyQty : 9999.0;
    if (doi < 30 && soLuongBan > 0) {
      return "🟢 Cần nhập thêm tháng này";
    }
    if (doi >= 30 && doi <= 120) {
      return "🔵 Đủ hàng / Bỏ qua";
    }
    if (doi > 120 && doi <= 180) {
      return "🟡 Tồn kho cao / Giãn đơn";
    }
    return "🔴 Xả kho / Ngừng nhập";
  }

  /** Doanh số sau điều chỉnh thật của các dòng dẫn đầu trong kỳ đang xem (đơn vị triệu đồng). */
  private static List<BarChartPanel.BarPoint> buildPeriodRevenueChart(
      List<ThongKeSanPhamDao.DongThongKe> source) {
    return source.stream()
        .filter(row -> row.doanhSoSauDieuChinh().signum() > 0)
        .sorted(Comparator.comparing(ThongKeSanPhamDao.DongThongKe::doanhSoSauDieuChinh).reversed())
        .limit(6)
        .map(row -> new BarChartPanel.BarPoint(nhanNgan(row.tenDongSP()),
            row.doanhSoSauDieuChinh().movePointLeft(6).doubleValue()))
        .toList();
  }

  /** Tên đầy đủ nằm trong bảng; trục biểu đồ chỉ đủ chỗ cho khoảng 14 ký tự mỗi cột. */
  private static String nhanNgan(String ten) {
    return ten == null || ten.length() <= 14 ? ten : ten.substring(0, 13) + "…";
  }

  private static final class RecommendationBadgeRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
      String raw = value == null ? "" : value.toString();
      setHorizontalAlignment(SwingConstants.LEFT);
      setToolTipText(raw);
      setText("<html><body style='white-space:nowrap'>" + escapeHtml(raw) + "</body></html>");
      return this;
    }

    private String escapeHtml(String raw) {
      return raw.replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;");
    }
  }

  /** UC015 9.a: hiển thị hóa đơn và phiếu trả đã dùng để tổng hợp dòng sản phẩm đang chọn. */
  private void moDoiChieu(String maDongSP) {
    DateRange ky = currentRange;
    ThongKeSanPhamDao.DongThongKe dong = rows.stream()
        .filter(r -> r.maDongSP().equals(maDongSP)).findFirst().orElse(null);
    if (controller == null || ky == null || dong == null) {
      return;
    }
    new SwingWorker<ThongKeSanPhamController.KetQua<List<ThongKeSanPhamDao.GiaoDichDoiChieu>>, Void>() {
      @Override
      protected ThongKeSanPhamController.KetQua<List<ThongKeSanPhamDao.GiaoDichDoiChieu>> doInBackground() {
        return controller.doiChieu(ky.tuNgay(), ky.denNgay(), maDongSP);
      }

      @Override
      protected void done() {
        try {
          var ketQua = get();
          if (!ketQua.thanhCong()) {
            JOptionPane.showMessageDialog(view, ketQua.thongBao(), "Đối chiếu", JOptionPane.ERROR_MESSAGE);
            return;
          }
          new DoiChieuDialog(ConfirmDialog.frameOf(view), dong, ky, ketQua.duLieu()).setVisible(true);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(view, "Không thể tải giao dịch đối chiếu.", "Đối chiếu",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  private static final class DoiChieuDialog extends ModalDialog {

    DoiChieuDialog(Frame parent, ThongKeSanPhamDao.DongThongKe dong, DateRange ky,
        List<ThongKeSanPhamDao.GiaoDichDoiChieu> giaoDichs) {
      super(parent, "Đối chiếu " + dong.maDongSP(), dong.tenDongSP() + " • "
          + DateFormats.formatDate(ky.tuNgay()) + " – " + DateFormats.formatDate(ky.denNgay()), 880);

      DefaultTableModel model = DataTable.readOnlyModel(new String[] {
          "Loại", "Chứng từ", "Hóa đơn", "Ngày", "Số serial", "Giá trị trước thuế (VND)"});
      for (ThongKeSanPhamDao.GiaoDichDoiChieu gd : giaoDichs) {
        model.addRow(new Object[] {gd.laTra() ? "Trả" : "Bán", gd.maChungTu(), gd.maHD(),
            DateFormats.formatDate(gd.ngay()), gd.soSerial(), gd.giaTri()});
      }
      DataTable table = new DataTable(model);
      table.setCodeColumn(1);
      table.setMoneyColumn(5);
      table.setVisibleRows(3, 10);

      JLabel tomTat = new JLabel("Bán " + dong.soLuongBan() + " • Trả " + dong.soLuongTra()
          + " • Sau điều chỉnh " + TienUtil.dinhDang(dong.doanhSoSauDieuChinh()));
      tomTat.setFont(DtaqFonts.CARD_TITLE);
      tomTat.setForeground(DtaqColors.TEXT);
      getBody().add(tomTat, "growx, wrap, gapbottom 8");
      getBody().add(new StatefulView(table, "receipt-text", "Không có giao dịch trong kỳ", null) {
        { if (!giaoDichs.isEmpty()) showContent(); }
      }, "growx");

      DtaqButton dongBtn = DtaqButton.primary("Đóng");
      dongBtn.addActionListener(e -> dispose());
      setPrimaryAction(dongBtn);
      setActions(dongBtn);
    }
  }

  private void exportReport() {
    if (rows.isEmpty()) {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Xuất báo cáo doanh số theo sản phẩm");
    chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
    if (chooser.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File selected = chooser.getSelectedFile();
    File target = selected.getName().toLowerCase().endsWith(".xlsx")
        ? selected : new File(selected.getParentFile(), selected.getName() + ".xlsx");
    exportBtn.setEnabled(false);
    new SwingWorker<ThongKeSanPhamController.KetQua<Void>, Void>() {
      @Override
      protected ThongKeSanPhamController.KetQua<Void> doInBackground() {
        return controller.xuatExcel(target, rows);
      }

      @Override
      protected void done() {
        exportBtn.setEnabled(true);
        try {
          ThongKeSanPhamController.KetQua<Void> result = get();
          JOptionPane.showOptionDialog(
              view,
              result.thongBao(),
              "Xuất báo cáo",
              JOptionPane.DEFAULT_OPTION,
              result.thanhCong() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE,
              null,
              new String[] {"OK"},
              "OK");
        } catch (Exception e) {
          JOptionPane.showOptionDialog(
              view,
              "Không thể xuất tệp Excel.",
              "Xuất báo cáo",
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.ERROR_MESSAGE,
              null,
              new String[] {"OK"},
              "OK");
        }
      }
    }.execute();
  }

  @Override
  public PageId id() {
    return PageId.SALES_BY_PRODUCT;
  }

  @Override
  public String title() {
    return REPORT_TITLE;
  }

  @Override
  public String section() {
    return "Quản lý";
  }

  @Override
  public JComponent view() {
    return view;
  }
}
