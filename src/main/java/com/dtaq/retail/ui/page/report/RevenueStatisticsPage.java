package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.ui.component.BarChartPanel;
import com.dtaq.retail.controller.ThongKeDoanhThuController;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DonutChartPanel;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.Tile;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.DateRange;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Thống kê doanh thu (UC017, RevenueStatisticsPage, Figma frame 2030:50).
 *
 * <p>Chỉ hiển thị số liệu do controller cung cấp qua {@link #renderSummary}, {@link #renderDailyRevenue},
 * {@link #renderTopProductLines}; chưa có dữ liệu thì các chỉ số hiện "—" và biểu đồ ở trạng thái rỗng.
 */
public class RevenueStatisticsPage implements Page {

  private static final String NOT_CONNECTED_TIP = "Chức năng đang được kết nối dữ liệu";
  private static final String PERIOD_PLACEHOLDER = "Chọn kỳ báo cáo";
  private static final String[] GRANULARITY_TITLES = {
      "Doanh thu theo ngày", "Doanh thu theo tuần", "Doanh thu theo tháng"};

  /** Chỉ tiêu tổng hợp của kỳ báo cáo do controller tính; {@code soVoiKyTruoc} có thể {@code null}. */
  public record RevenueSummary(
      long tongDoanhThu,
      long tongTruocThue,
      long tongThue,
      int soHoaDon,
      int soPhieuDoiTra,
      KpiCard.Trend xuHuong,
      String soVoiKyTruoc
  ) {}

  private final JPanel view;
  private final ThongKeDoanhThuController controller;
  private ThongKeDoanhThuController.BaoCao baoCao;
  private DateRange kyBaoCao = new DateRange(LocalDate.now().minusDays(29), LocalDate.now());
  private int nhomThoiGian;
  private final DtaqButton periodButton = DtaqButton.secondary(PERIOD_PLACEHOLDER, "calendar-days");
  private final DtaqButton exportButton = DtaqButton.secondary("Xuất Excel", "download");
  private final JPanel alertBanner;
  private final JLabel alertCountLabel = new JLabel();
  private final KpiCard kpiRevenue = new KpiCard("Tổng doanh thu trong kỳ", "—", null);
  private final KpiCard kpiPretax = new KpiCard("Tổng tiền hàng trước thuế", "—", "Đã trừ hoàn trả và đổi hàng");
  private final KpiCard kpiTax = new KpiCard("Tổng tiền thuế phát sinh", "—", "Thuế VAT theo từng nhóm hàng");
  private final KpiCard kpiOrders = new KpiCard("Hóa đơn bán lẻ thành công", "—", null);
  private final JLabel barTitle = new JLabel(GRANULARITY_TITLES[0]);
  private final BarChartPanel barChart = new BarChartPanel();
  private final JLabel donutPeriod = new JLabel();
  private final DonutChartPanel donutChart = new DonutChartPanel();

  private LocalDate periodFrom;
  private LocalDate periodTo;
  private Consumer<PageId> onNavigateListener;
  private BiConsumer<LocalDate, LocalDate> onPeriodChangeListener;
  private Consumer<Integer> onGranularityChangeListener;

  public RevenueStatisticsPage() {
    this(null);
  }

  public RevenueStatisticsPage(ThongKeDoanhThuController controller) {
    this.controller = controller;
    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"
            + DtaqSpacing.SECTION_GAP + "[]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        "Thống kê doanh thu", "Theo dõi chỉ số kinh doanh và phân bổ ngành hàng toàn hệ thống.");
    periodButton.setToolTipText(NOT_CONNECTED_TIP);
    periodButton.setEnabled(false);
    periodButton.addActionListener(e -> openPeriodDialog());
    exportButton.setEnabled(false);
    exportButton.setToolTipText(controller == null ? NOT_CONNECTED_TIP : null);
    exportButton.addActionListener(e -> exportReport());
    header.setTrailing(periodButton, exportButton);
    view.add(header, "growx, wrap");

    alertBanner = buildAlertBanner();
    alertBanner.setVisible(false);
    view.add(alertBanner, "growx, hidemode 3, wrap");

    JPanel kpiRow = new JPanel(new MigLayout(
        "fillx, insets 0",
        "[grow,fill]" + DtaqSpacing.KPI_GAP_COMPACT + "[grow,fill]" + DtaqSpacing.KPI_GAP_COMPACT
            + "[grow,fill]" + DtaqSpacing.KPI_GAP_COMPACT + "[grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    kpiRow.add(kpiRevenue);
    kpiRow.add(kpiPretax);
    kpiRow.add(kpiTax);
    kpiRow.add(kpiOrders);
    view.add(kpiRow, "growx, wrap");

    view.add(buildBarCard(), "growx, wrap");
    view.add(buildDonutCard(), "growx");

    if (controller != null) {
      setPeriod(kyBaoCao.tuNgay(), kyBaoCao.denNgay());
      setOnPeriodChange((from, to) -> {
        kyBaoCao = new DateRange(from, to);
        taiBaoCao();
      });
      setOnGranularityChange(index -> {
        nhomThoiGian = index;
        taiBaoCao();
      });
    }
  }

  /** Tải báo cáo cho kỳ và mức gộp thời gian đang chọn; không chặn EDT. */
  private void taiBaoCao() {
    DateRange ky = kyBaoCao;
    int nhom = nhomThoiGian;
    showLoading();
    exportButton.setEnabled(false);
    new SwingWorker<ThongKeDoanhThuController.BaoCao, Void>() {
      @Override
      protected ThongKeDoanhThuController.BaoCao doInBackground() {
        return controller.xem(ky, nhom);
      }

      @Override
      protected void done() {
        try {
          ThongKeDoanhThuController.BaoCao ketQua = get();
          baoCao = ketQua;
          renderSummary(ketQua.kpi());
          renderDailyRevenue(ketQua.doanhThuTheoThoiGian());
          renderTopProductLines(ketQua.topDongSanPham());
          exportButton.setEnabled(true);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          hienLoiTaiBaoCao();
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            showError("Chỉ Quản lý được xem thống kê doanh thu");
          } else {
            hienLoiTaiBaoCao();
          }
        } catch (SecurityException e) {
          showError("Chỉ Quản lý được xem thống kê doanh thu");
        }
      }
    }.execute();
  }

  /** Xóa số liệu cũ để không hiển thị nhầm báo cáo của kỳ trước khi tải thất bại. */
  private void hienLoiTaiBaoCao() {
    baoCao = null;
    renderSummary(null);
    renderDailyRevenue(null);
    renderTopProductLines(null);
    exportButton.setEnabled(false);
    showError("Không thể tải dữ liệu thống kê doanh thu.");
  }

  private void exportReport() {
    if (controller == null || baoCao == null) return;
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Xuất thống kê doanh thu");
    chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
    if (chooser.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) return;
    File selected = chooser.getSelectedFile();
    File target = selected.getName().toLowerCase().endsWith(".xlsx") ? selected
        : new File(selected.getParentFile(), selected.getName() + ".xlsx");
    exportButton.setEnabled(false);
    new SwingWorker<Void, Void>() {
      @Override protected Void doInBackground() { controller.xuatExcel(target, baoCao); return null; }
      @Override protected void done() {
        exportButton.setEnabled(baoCao != null);
        try { get(); JOptionPane.showMessageDialog(view,"Xuất báo cáo Excel thành công","Xuất báo cáo",JOptionPane.INFORMATION_MESSAGE); }
        catch(Exception e) { JOptionPane.showMessageDialog(view,"Không thể ghi tệp Excel. Báo cáo vẫn được giữ trên màn hình.","Xuất báo cáo",JOptionPane.ERROR_MESSAGE); }
      }
    }.execute();
  }

  private Card buildBarCard() {
    Card card = Card.standard();
    JPanel barHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    barHeader.setOpaque(false);
    barTitle.setFont(DtaqFonts.CARD_TITLE);
    barTitle.setForeground(DtaqColors.TEXT);
    barHeader.add(barTitle, "align left");

    SegmentedControl segment = new SegmentedControl(new String[] {"Ngày", "Tuần", "Tháng"}, 0);
    segment.setOnSelectionChange(index -> {
      barTitle.setText(GRANULARITY_TITLES[index]);
      if (onGranularityChangeListener != null) {
        onGranularityChangeListener.accept(index);
      }
    });
    barHeader.add(segment, "align right");
    card.add(barHeader, "growx, wrap");

    card.setBody(barChart);
    return card;
  }

  private Card buildDonutCard() {
    donutPeriod.setFont(DtaqFonts.BODY_13);
    donutPeriod.setForeground(DtaqColors.TEXT_MUTED);
    Card card = Card.section("Tỷ trọng doanh thu top 5 dòng sản phẩm", null, donutPeriod);
    card.setBody(donutChart);
    return card;
  }

  private JPanel buildAlertBanner() {
    JPanel banner = new JPanel(new MigLayout("fillx, insets 12 16 12 16", "[]12[grow][]", "[center]"));
    banner.setOpaque(true);
    banner.setBackground(DtaqColors.DANGER_SOFT_2);
    banner.putClientProperty(FlatClientProperties.STYLE, "arc: 16");
    banner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    banner.add(Tile.icon("alert-circle", 20, DtaqColors.DANGER_STRONG, DtaqColors.DANGER_SOFT, 36, DtaqSpacing.RADIUS_CARD_SM));

    JPanel textBlock = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    textBlock.setOpaque(false);
    JLabel title = new JLabel("Yêu cầu đổi/trả chờ phê duyệt");
    title.setFont(DtaqFonts.BUTTON);
    title.setForeground(DtaqColors.DANGER_STRONG);
    textBlock.add(title, "wrap");
    alertCountLabel.setFont(DtaqFonts.CAPTION);
    alertCountLabel.setForeground(DtaqColors.DANGER);
    textBlock.add(alertCountLabel);
    banner.add(textBlock, "growx");

    JLabel link = new JLabel("Bấm vào để xem chi tiết", DtaqIcons.icon("chevron-right", 14, DtaqColors.DANGER_STRONG),
        JLabel.LEFT);
    link.setHorizontalTextPosition(JLabel.LEFT);
    link.setFont(DtaqFonts.ITALIC_HINT);
    link.setForeground(DtaqColors.DANGER_STRONG);
    banner.add(link, "align right");

    banner.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (onNavigateListener != null) {
          onNavigateListener.accept(PageId.MANAGER_HOME);
        }
      }
    });
    return banner;
  }

  // ---------------------------------------------------------------- public API

  /** Đặt kỳ báo cáo đang hiển thị (nhãn nút chọn kỳ và tiêu đề biểu đồ tròn). */
  public void setPeriod(LocalDate from, LocalDate to) {
    this.periodFrom = from;
    this.periodTo = to;
    if (from == null || to == null) {
      periodButton.setText(PERIOD_PLACEHOLDER);
      donutPeriod.setText("");
      return;
    }
    String text = DateFormats.formatDayMonth(from) + " – " + DateFormats.formatDate(to);
    periodButton.setText(text);
    donutPeriod.setText("Kỳ " + text);
  }

  /** Hiển thị 4 chỉ tiêu tổng hợp; {@code null} đưa về trạng thái rỗng. */
  public void renderSummary(RevenueSummary summary) {
    if (summary == null) {
      kpiRevenue.setValue(null);
      kpiRevenue.setCaption("");
      kpiPretax.setValue(null);
      kpiTax.setValue(null);
      kpiOrders.setValue(null);
      kpiOrders.setCaption("");
      return;
    }
    kpiRevenue.setValue(MoneyFormat.format(summary.tongDoanhThu()));
    if (summary.soVoiKyTruoc() != null && summary.xuHuong() != null) {
      kpiRevenue.setTrend(summary.xuHuong(), summary.soVoiKyTruoc());
    } else {
      kpiRevenue.setCaption("");
    }
    kpiPretax.setValue(MoneyFormat.format(summary.tongTruocThue()));
    kpiTax.setValue(MoneyFormat.format(summary.tongThue()));
    kpiOrders.setValue(String.valueOf(summary.soHoaDon()));
    kpiOrders.setCaption(summary.soPhieuDoiTra() + " phiếu đổi/trả trong kỳ");
  }

  public void renderDailyRevenue(List<BarChartPanel.BarPoint> points) {
    barChart.setData(points);
  }

  public void renderTopProductLines(List<DonutChartPanel.DonutSlice> slices) {
    donutChart.setData(slices);
  }

  /** Số phiếu đổi/trả đang chờ duyệt; banner chỉ hiện khi lớn hơn 0. */
  public void setPendingReturnCount(int count) {
    alertBanner.setVisible(count > 0);
    alertCountLabel.setText("Hiện có " + count + " yêu cầu đổi/trả đang chờ quản lý xử lý.");
    view.revalidate();
  }

  public void showLoading() {
    kpiRevenue.setLoading();
    kpiPretax.setLoading();
    kpiTax.setLoading();
    kpiOrders.setLoading();
  }

  public void showError(String message) {
    ConfirmDialog.info(view, "Không thể tải thống kê",
        message == null || message.isBlank() ? "Không thể tải dữ liệu thống kê." : message, null);
  }

  public void setOnNavigate(Consumer<PageId> listener) {
    this.onNavigateListener = listener;
  }

  /** Nhận kỳ báo cáo mới (từ ngày, đến ngày) khi người dùng chọn xong; kích hoạt nút chọn kỳ. */
  public void setOnPeriodChange(BiConsumer<LocalDate, LocalDate> listener) {
    this.onPeriodChangeListener = listener;
    boolean connected = listener != null;
    periodButton.setEnabled(connected);
    periodButton.setToolTipText(connected ? null : NOT_CONNECTED_TIP);
  }

  /** Nhận chỉ số phân đoạn Ngày/Tuần/Tháng (0, 1, 2). */
  public void setOnGranularityChange(Consumer<Integer> listener) {
    this.onGranularityChangeListener = listener;
  }

  private void openPeriodDialog() {
    if (onPeriodChangeListener == null) {
      return;
    }
    LocalDate to = periodTo != null ? periodTo : LocalDate.now();
    LocalDate from = periodFrom != null ? periodFrom : to.minusDays(6);
    new PeriodDialog(ConfirmDialog.frameOf(view), from, to, (newFrom, newTo) -> {
      setPeriod(newFrom, newTo);
      onPeriodChangeListener.accept(newFrom, newTo);
    }).setVisible(true);
  }

  @Override
  public void onShow() {
    if (controller != null) {
      taiBaoCao();
    }
  }

  @Override
  public PageId id() {
    return PageId.REVENUE_STATS;
  }

  @Override
  public String title() {
    return "Thống kê doanh thu";
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
