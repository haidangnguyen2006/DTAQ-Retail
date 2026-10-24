package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Báo cáo tồn kho lâu ngày (LongStockReportPage) theo Figma frame 1:2.
 */
public class LongStockReportPage implements Page {

  public record LongStockRow(
      String maSP,
      String tenSP,
      String ngayNhap,
      int soNgayTon,
      int slTon,
      long giaNhap,
      long giaTriTon
  ) {}

  private final JPanel view;
  private final KpiCard kpiProductCount;
  private final KpiCard kpiTotalValue;
  private final KpiCard kpiMaxDays;
  private final FormField searchField;
  private final JComboBox<String> categoryCombo;
  private final JComboBox<String> daysCombo;
  private final StatusPill resultCountPill;
  private final DataTable dataTable;
  private final DefaultTableModel tableModel;

  public LongStockReportPage() {
    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[grow]"
    ));
    view.setBackground(DtaqColors.BG_APP);

    // 1. Header
    String todayStr = DateFormats.formatDate(LocalDate.now());
    PageHeader header = new PageHeader(
        "Báo cáo tồn kho lâu ngày",
        "Các sản phẩm còn tồn trên 60 ngày · Tính đến " + todayStr + "."
    );
    DtaqButton exportBtn = DtaqButton.secondary("Xuất báo cáo", "download");
    exportBtn.setEnabled(false);
    exportBtn.setToolTipText("Chức năng xuất báo cáo Excel đang được hoàn thiện");
    header.setTrailing(exportBtn);
    view.add(header, "growx, wrap");

    // 2. 3 KPI Cards
    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]20[grow, fill]20[grow, fill]", "[]"));
    kpiRow.setOpaque(false);

    kpiProductCount = new KpiCard("Sản phẩm trên 60 ngày", "6", "48 đơn vị còn tồn trong kho");
    kpiTotalValue = new KpiCard("Giá trị tồn theo giá nhập", "246.000.000", "VNĐ · Không dùng giá bán lẻ");
    kpiMaxDays = new KpiCard("Thời gian tồn lâu nhất", "126 ngày", "TIVI003 · Nhập ngày 05/06/2026");

    kpiRow.add(kpiProductCount);
    kpiRow.add(kpiTotalValue);
    kpiRow.add(kpiMaxDays);
    view.add(kpiRow, "growx, wrap");

    // 3. Card Filter + Table
    Card contentCard = Card.standard();

    // Filters row
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[320!]16[180!]16[180!]16[]push[]", "[bottom]"));
    filterRow.setOpaque(false);

    searchField = FormField.text("Tìm sản phẩm", "Mã hoặc tên sản phẩm", "search");
    filterRow.add(searchField);

    categoryCombo = new JComboBox<>(new String[]{"Tất cả ngành hàng", "Tivi", "Tủ lạnh", "Máy giặt", "Máy lạnh", "Gia dụng"});
    FormField catField = new FormField("Ngành hàng", categoryCombo);
    filterRow.add(catField);

    daysCombo = new JComboBox<>(new String[]{"Trên 60 ngày", "Trên 90 ngày", "Trên 120 ngày"});
    FormField daysField = new FormField("Thời gian tồn", daysCombo);
    filterRow.add(daysField);

    DtaqButton applyBtn = DtaqButton.primary("Áp dụng");
    filterRow.add(applyBtn);

    resultCountPill = StatusPill.neutral("6 kết quả");
    filterRow.add(resultCountPill, "align right, aligny center");

    contentCard.add(filterRow, "growx, wrap, gapbottom 16");

    // Table
    String[] columns = {"Mã", "Tên sản phẩm", "Ngày nhập", "Số ngày tồn", "SL tồn", "Giá nhập / ĐV", "Giá trị tồn (VNĐ)"};
    tableModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int col) {
        return false;
      }
    };
    dataTable = new DataTable(tableModel, 60);
    dataTable.setCodeColumn(0);
    dataTable.setMoneyColumn(5);
    dataTable.setMoneyColumn(6);
    dataTable.setColumnWidths(90, 420, 100, 100, 60, 120, 140);

    // Populate initial sample rows according to Figma
    populateInitialRows();
    contentCard.add(dataTable, "grow, push, wrap, gapbottom 12");

    // Table Footer
    JPanel tableFooter = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    tableFooter.setOpaque(false);

    JLabel sortHint = new JLabel("Sắp xếp theo số ngày tồn giảm dần");
    sortHint.setFont(DtaqFonts.CAPTION);
    sortHint.setForeground(DtaqColors.TEXT_MUTED);
    tableFooter.add(sortHint, "align left");

    JLabel formulaHint = new JLabel("Giá trị tồn = Số lượng tồn × Giá nhập");
    formulaHint.setFont(DtaqFonts.CAPTION);
    formulaHint.setForeground(DtaqColors.TEXT_MUTED);
    tableFooter.add(formulaHint, "align right");

    contentCard.add(tableFooter, "growx");

    view.add(contentCard, "grow, push");
  }

  private void populateInitialRows() {
    List<LongStockRow> rows = List.of(
        new LongStockRow("TIVI003", "Smart Tivi Samsung 4K 65 inch Crystal UHD", "05/06/2026", 126, 8, 12_500_000, 100_000_000),
        new LongStockRow("TULANH02", "Tủ lạnh LG Inverter 394 lít Side-by-Side", "12/06/2026", 119, 6, 11_000_000, 66_000_000),
        new LongStockRow("MAYGIAT01", "Máy giặt Electrolux Inverter 10 kg cửa ngang", "25/06/2026", 106, 12, 3_000_000, 36_000_000),
        new LongStockRow("MAYLNH04", "Máy lạnh Daikin Inverter 1.5 HP Tiết kiệm điện", "10/07/2026", 91, 5, 5_200_000, 26_000_000),
        new LongStockRow("TIVI005", "Smart Tivi Sony 55 inch Google Tivi 4K HDR", "22/07/2026", 79, 10, 1_200_000, 12_000_000),
        new LongStockRow("QUAT01", "Quạt đứng Mitsubishi Electric có điều khiển", "08/08/2026", 62, 7, 850_000, 5_950_000)
    );

    for (LongStockRow r : rows) {
      tableModel.addRow(new Object[]{
          r.maSP(),
          r.tenSP(),
          r.ngayNhap(),
          r.soNgayTon() + " ngày",
          r.slTon(),
          r.giaNhap(),
          r.giaTriTon()
      });
    }
  }

  public void renderRows(List<LongStockRow> rows) {
    tableModel.setRowCount(0);
    if (rows == null || rows.isEmpty()) {
      resultCountPill.setText("0 kết quả");
      return;
    }
    resultCountPill.setText(rows.size() + " kết quả");
    for (LongStockRow r : rows) {
      tableModel.addRow(new Object[]{
          r.maSP(),
          r.tenSP(),
          r.ngayNhap(),
          r.soNgayTon() + " ngày",
          r.slTon(),
          r.giaNhap(),
          r.giaTriTon()
      });
    }
  }

  @Override
  public PageId id() {
    return PageId.LONG_STOCK_REPORT;
  }

  @Override
  public String title() {
    return "Báo cáo tồn kho lâu ngày";
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
