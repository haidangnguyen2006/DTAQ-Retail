package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.ui.component.ActivityItem;
import com.dtaq.retail.ui.component.BarChartPanel;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DonutChartPanel;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Thống kê doanh thu (RevenueStatisticsPage) theo Figma frame 2030:50.
 */
public class RevenueStatisticsPage implements Page {

  private final JPanel view;
  private final KpiCard kpiRevenue;
  private final KpiCard kpiPretax;
  private final KpiCard kpiTax;
  private final KpiCard kpiOrders;
  private final BarChartPanel barChart;
  private final DonutChartPanel donutChart;
  private final JPanel alertBanner;

  private Consumer<PageId> onNavigateListener;

  public RevenueStatisticsPage() {
    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"
    ));
    view.setBackground(DtaqColors.BG_APP);

    // 1. Header
    PageHeader header = new PageHeader("Thống kê doanh thu", "Theo dõi chỉ số kinh doanh và phân bổ ngành hàng toàn hệ thống.");
    DtaqButton dateBtn = DtaqButton.secondary("03/10 – 09/10/2026", "calendar-days");
    header.setTrailing(dateBtn);
    view.add(header, "growx, wrap");

    // 2. Alert Banner (Yêu cầu đổi/trả chờ)
    alertBanner = buildAlertBanner();
    view.add(alertBanner, "growx, wrap");

    // 3. 4 KPI Cards
    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]16[grow, fill]16[grow, fill]16[grow, fill]", "[]"));
    kpiRow.setOpaque(false);

    kpiRevenue = new KpiCard("Tổng doanh thu trong kỳ", "285.000.000", "↑ 12,4% so với kỳ trước");
    kpiPretax = new KpiCard("Tổng tiền hàng trước thuế", "259.000.000", "Đã trừ hoàn trả và đổi hàng");
    kpiTax = new KpiCard("Tổng tiền thuế phát sinh", "26.000.000", "Thuế VAT theo từng nhóm hàng");
    kpiOrders = new KpiCard("Hóa đơn bán lẻ thành công", "142", "6 phiếu đổi/trả trong kỳ");

    kpiRow.add(kpiRevenue);
    kpiRow.add(kpiPretax);
    kpiRow.add(kpiTax);
    kpiRow.add(kpiOrders);
    view.add(kpiRow, "growx, wrap");

    // 4. Bar Chart Card: Doanh thu theo ngày
    Card barCard = Card.standard();
    JPanel barHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    barHeader.setOpaque(false);
    JLabel barTitle = new JLabel("Doanh thu theo ngày");
    barTitle.setFont(DtaqFonts.SECTION_TITLE_STRONG);
    barHeader.add(barTitle, "align left");

    SegmentedControl segment = new SegmentedControl(new String[]{"Ngày", "Tuần", "Tháng"}, 0);
    barHeader.add(segment, "align right");
    barCard.add(barHeader, "growx, wrap, gapbottom 16");

    barChart = new BarChartPanel();
    barChart.setData(List.of(
        new BarChartPanel.BarPoint("03/10", 35.0),
        new BarChartPanel.BarPoint("04/10", 42.0),
        new BarChartPanel.BarPoint("05/10", 28.0),
        new BarChartPanel.BarPoint("06/10", 50.0),
        new BarChartPanel.BarPoint("07/10", 46.0),
        new BarChartPanel.BarPoint("08/10", 38.0),
        new BarChartPanel.BarPoint("09/10", 46.0)
    ));
    barCard.add(barChart, "growx");
    view.add(barCard, "growx, wrap");

    // 5. Donut Chart Card: Tỷ trọng top 5 dòng sản phẩm
    Card donutCard = Card.standard();
    JPanel donutHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    donutHeader.setOpaque(false);
    JLabel donutTitle = new JLabel("Tỷ trọng doanh thu top 5 dòng sản phẩm");
    donutTitle.setFont(DtaqFonts.SECTION_TITLE_STRONG);
    donutHeader.add(donutTitle, "align left");

    JLabel period = new JLabel("Kỳ 03/10 – 09/10/2026");
    period.setFont(DtaqFonts.BODY_13);
    period.setForeground(DtaqColors.TEXT_MUTED);
    donutHeader.add(period, "align right");
    donutCard.add(donutHeader, "growx, wrap, gapbottom 20");

    donutChart = new DonutChartPanel();
    donutChart.setData(List.of(
        new DonutChartPanel.DonutSlice("Tivi", 114_000_000, DtaqColors.CHART_1),
        new DonutChartPanel.DonutSlice("Tủ lạnh", 71_250_000, DtaqColors.CHART_2),
        new DonutChartPanel.DonutSlice("Máy giặt", 42_750_000, DtaqColors.CHART_3),
        new DonutChartPanel.DonutSlice("Máy lạnh", 34_200_000, DtaqColors.CHART_4),
        new DonutChartPanel.DonutSlice("Phụ kiện", 22_800_000, DtaqColors.CHART_5)
    ));
    donutCard.add(donutChart, "growx");
    view.add(donutCard, "growx");
  }

  private JPanel buildAlertBanner() {
    JPanel banner = new JPanel(new MigLayout("fillx, insets 12 16 12 16", "[]12[grow][]", "[center]")) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.DANGER_SOFT_2);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_M, DtaqSpacing.RADIUS_M));
        g2.setColor(DtaqColors.DANGER_SOFT);
        g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, DtaqSpacing.RADIUS_M, DtaqSpacing.RADIUS_M));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    banner.setOpaque(false);
    banner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    banner.add(new JLabel(DtaqIcons.icon("alert-circle", 20, DtaqColors.DANGER_STRONG)));

    JPanel textBlock = new JPanel(new MigLayout("insets 0", "[]", "[]2[]"));
    textBlock.setOpaque(false);
    JLabel title = new JLabel("Yêu cầu đổi/trả chờ phê duyệt");
    title.setFont(DtaqFonts.BUTTON);
    title.setForeground(DtaqColors.DANGER_STRONG);
    textBlock.add(title, "wrap");

    JLabel count = new JLabel("Hiện có 6 yêu cầu đổi/trả đang chờ quản lý xử lý.");
    count.setFont(DtaqFonts.CAPTION);
    count.setForeground(DtaqColors.DANGER);
    textBlock.add(count);
    banner.add(textBlock, "growx");

    JLabel link = new JLabel("Bấm vào để xem chi tiết →");
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

  public void setOnNavigate(Consumer<PageId> listener) {
    this.onNavigateListener = listener;
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
