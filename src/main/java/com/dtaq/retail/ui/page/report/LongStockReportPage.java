package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.controller.BaoCaoTonKhoController;
import com.dtaq.retail.dao.BaoCaoTonKhoDao.DongTonLau;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/** Màn hình báo cáo tồn kho lâu ngày theo UC014. */
public class LongStockReportPage implements Page {

  private static final String ALL_CATEGORIES = "Tất cả ngành hàng";
  private static final String NOT_CONNECTED_TIP = "Chức năng đang được kết nối dữ liệu";
  private static final String EMPTY_TITLE = "Chưa có báo cáo";
  private static final String EMPTY_HINT = "Nhập ngưỡng số ngày tồn kho rồi bấm Xem báo cáo.";

  private final JPanel view;
  private final BaoCaoTonKhoController controller;
  private final KpiCard kpiMachines = new KpiCard("Tổng số máy tồn lâu", "—", "Chưa có dữ liệu");
  private final KpiCard kpiProductLines = new KpiCard("Dòng sản phẩm ảnh hưởng", "—", "Chưa có dữ liệu");
  private final KpiCard kpiMaxDays = new KpiCard("Số ngày tồn lâu nhất", "—", "Chưa có dữ liệu");
  private final FormField searchField = FormField.search("Tìm sản phẩm", "Mã hoặc tên dòng sản phẩm");
  private final JComboBox<String> categoryCombo = new JComboBox<>(new String[] {ALL_CATEGORIES});
  private final FormField categoryField = new FormField("Ngành hàng", categoryCombo);
  private final FormField thresholdField = FormField.text("Ngưỡng số ngày tồn", "Nhập số ngày", null);
  private final JCheckBox readyOnly = new JCheckBox("Chỉ tính máy sẵn sàng bán");
  private final DtaqButton applyBtn = DtaqButton.primary("Xem báo cáo");
  private final DtaqButton exportBtn = DtaqButton.secondary("Xuất báo cáo", "download");
  private final StatusPill resultCountPill = StatusPill.neutral("0 máy");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã dòng SP", "Tên dòng sản phẩm", "Danh mục", "Số serial", "Ngày nhập kho", "Trạng thái", "Số ngày tồn"});
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;
  private List<DongTonLau> reportRows = List.of();
  private String selectedProductLine;

  /** Giữ constructor hiện tại để AppNavigator có thể nối controller trong thay đổi shell riêng. */
  public LongStockReportPage() {
    this(null);
  }

  public LongStockReportPage(BaoCaoTonKhoController controller) {
    this.controller = controller;
    view = new JPanel(new MigLayout("fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[0:pref,grow]", "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);
    thresholdField.setText("60");
    thresholdField.setRequired(true);
    readyOnly.setOpaque(false);

    PageHeader header = new PageHeader("Báo cáo tồn kho lâu ngày",
        "Các máy còn tồn đạt ngưỡng ngày đã chọn · Tính đến " + DateFormats.formatDate(LocalDate.now()) + ".");
    header.setTrailing(exportBtn);
    view.add(header, "growx, wrap");

    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0",
        "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    com.dtaq.retail.ui.component.Responsive.grid(kpiRow, 240, 3);
    kpiRow.add(kpiMachines);
    kpiRow.add(kpiProductLines);
    kpiRow.add(kpiMaxDays);
    view.add(kpiRow, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setNumberColumn(6);
    dataTable.setSortable(true);
    dataTable.setColumnWidths(130, 280, 160, 180, 140, 160, 110);
    dataTable.setOnRowDoubleClick(this::drillDown);
    tableState = new StatefulView(dataTable, "package-search", EMPTY_TITLE, EMPTY_HINT);

    Card contentCard = Card.standard();
    contentCard.add(buildFilterRow(), "growx, wrap");
    contentCard.setBody(tableState);
    contentCard.add(buildTableFooter(), "growx");
    view.add(contentCard, "grow, push");

    thresholdField.getTextField().addActionListener(e -> loadReport());
    applyBtn.addActionListener(e -> loadReport());
    searchField.getTextField().addActionListener(e -> renderRows());
    categoryCombo.addActionListener(e -> renderRows());
    exportBtn.addActionListener(e -> exportReport());
    exportBtn.setEnabled(false);
    refreshControls();
    tableState.showEmpty("package-search", EMPTY_TITLE, EMPTY_HINT);
  }

  private JPanel buildFilterRow() {
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]16[180!]16[180!]16[]16[]push[]", "[bottom]")) {
      @Override
      public java.awt.Dimension getMinimumSize() {
        return new java.awt.Dimension(0, getPreferredSize().height);
      }
    };
    filterRow.setOpaque(false);
    filterRow.add(searchField, "growx");
    filterRow.add(categoryField, "growx");
    filterRow.add(thresholdField, "growx");
    filterRow.add(readyOnly);
    filterRow.add(applyBtn);
    filterRow.add(resultCountPill, "align right, aligny center");
    com.dtaq.retail.ui.component.Responsive.onSizeChange(filterRow, size -> {
      MigLayout layout = (MigLayout) filterRow.getLayout();
      boolean narrow = size == com.dtaq.retail.ui.component.Responsive.Size.S;
      layout.setColumnConstraints(narrow ? "[0:pref,grow]16[180!]16[180!]"
          : "[grow]16[180!]16[180!]16[]16[]push[]");
      for (int i = 0; i < filterRow.getComponentCount(); i++) {
        layout.setComponentConstraints(filterRow.getComponent(i), "cell "
            + (narrow ? i % 3 : i) + " " + (narrow ? i / 3 : 0)
            + (i < 3 ? ", growx" : ", align right, aligny bottom"));
      }
    });
    return filterRow;
  }

  private JPanel buildTableFooter() {
    JPanel footer = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    footer.setOpaque(false);
    footer.add(hint("Chọn đúp một dòng sản phẩm để lọc máy thuộc dòng đó"), "align left");
    footer.add(hint("Báo cáo hiển thị từng máy theo serial"), "align right");
    return footer;
  }

  private static JLabel hint(String text) {
    JLabel label = new JLabel(text);
    label.setFont(DtaqFonts.CAPTION);
    label.setForeground(DtaqColors.TEXT_MUTED);
    return label;
  }

  private void refreshControls() {
    boolean connected = controller != null;
    applyBtn.setEnabled(connected);
    applyBtn.setToolTipText(connected ? null : NOT_CONNECTED_TIP);
  }

  private void loadReport() {
    if (controller == null) return;
    int days = parseThreshold(thresholdField.getText());
    if (days <= 0) {
      thresholdField.setError("Ngưỡng số ngày tồn kho phải là số nguyên dương");
      return;
    }
    thresholdField.setError(null);
    boolean onlyReady = readyOnly.isSelected();
    applyBtn.setEnabled(false);
    tableState.showLoading();
    new SwingWorker<BaoCaoTonKhoController.KetQua<BaoCaoTonKhoController.BaoCao>, Void>() {
      @Override protected BaoCaoTonKhoController.KetQua<BaoCaoTonKhoController.BaoCao> doInBackground() {
        return controller.xemBaoCao(days, onlyReady);
      }

      @Override protected void done() {
        applyBtn.setEnabled(true);
        try {
          var result = get();
          if (!result.thanhCong()) {
            tableState.showError(result.thongBao());
            return;
          }
          reportRows = result.duLieu().chiTiet();
          selectedProductLine = null;
          refreshCategories();
          renderRows();
        } catch (Exception e) {
          tableState.showError("Không thể tải báo cáo tồn kho lâu ngày.");
        }
      }
    }.execute();
  }

  private void refreshCategories() {
    Object selected = categoryCombo.getSelectedItem();
    categoryCombo.removeAllItems();
    categoryCombo.addItem(ALL_CATEGORIES);
    reportRows.stream().map(DongTonLau::tenDanhMuc).distinct().forEach(categoryCombo::addItem);
    if (selected != null && reportRows.stream().anyMatch(row -> selected.equals(row.tenDanhMuc()))) {
      categoryCombo.setSelectedItem(selected);
    } else {
      categoryCombo.setSelectedIndex(0);
    }
  }

  private List<DongTonLau> visibleRows() {
    String keyword = searchField.getText().trim().toLowerCase();
    Object category = categoryCombo.getSelectedItem();
    return reportRows.stream()
        .filter(row -> selectedProductLine == null || selectedProductLine.equals(row.maDongSP()))
        .filter(row -> category == null || ALL_CATEGORIES.equals(category) || category.equals(row.tenDanhMuc()))
        .filter(row -> keyword.isEmpty() || row.maDongSP().toLowerCase().contains(keyword)
            || row.tenDongSP().toLowerCase().contains(keyword) || row.soSerial().toLowerCase().contains(keyword))
        .toList();
  }

  private void renderRows() {
    List<DongTonLau> rows = visibleRows();
    tableModel.setRowCount(0);
    for (DongTonLau row : rows) {
      tableModel.addRow(new Object[] {row.maDongSP(), row.tenDongSP(), row.tenDanhMuc(), row.soSerial(),
          DateFormats.formatDate(row.ngayNhapKhoBanDau()), row.trangThai().name(), row.soNgayTon()});
    }
    resultCountPill.setText(rows.size() + " máy");
    kpiMachines.setValue(String.valueOf(rows.size()));
    kpiMachines.setCaption("Máy đang hiển thị");
    kpiProductLines.setValue(String.valueOf(rows.stream().map(DongTonLau::maDongSP).distinct().count()));
    long longest = rows.stream().mapToLong(DongTonLau::soNgayTon).max().orElse(0);
    kpiMaxDays.setValue(longest + " ngày");
    kpiMaxDays.setCaption("Trong phạm vi lọc hiện tại");
    exportBtn.setEnabled(!rows.isEmpty());
    if (rows.isEmpty()) {
      tableState.showEmpty("package-search", "Không có máy phù hợp", "Thử thay đổi ngưỡng hoặc bộ lọc.");
    } else {
      tableState.showContent();
    }
  }

  private void drillDown(int modelRow) {
    List<DongTonLau> rows = visibleRows();
    if (modelRow < 0 || modelRow >= rows.size()) return;
    String line = rows.get(modelRow).maDongSP();
    selectedProductLine = line.equals(selectedProductLine) ? null : line;
    renderRows();
  }

  private void exportReport() {
    if (controller == null) return;
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Xuất báo cáo tồn kho lâu ngày");
    chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
    if (chooser.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) return;
    File selected = chooser.getSelectedFile();
    File target = selected.getName().toLowerCase().endsWith(".xlsx")
        ? selected : new File(selected.getParentFile(), selected.getName() + ".xlsx");
    List<DongTonLau> rows = visibleRows();
    exportBtn.setEnabled(false);
    new SwingWorker<BaoCaoTonKhoController.KetQua<Void>, Void>() {
      @Override protected BaoCaoTonKhoController.KetQua<Void> doInBackground() {
        return controller.xuatExcel(target, rows);
      }

      @Override protected void done() {
        exportBtn.setEnabled(!visibleRows().isEmpty());
        try {
          var result = get();
          JOptionPane.showMessageDialog(view, result.thongBao(), "Xuất báo cáo",
              result.thanhCong() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(view, "Không thể xuất tệp Excel.", "Xuất báo cáo", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  /** Số nguyên dương người dùng nhập, hoặc 0 nếu rỗng/không hợp lệ. */
  static int parseThreshold(String text) {
    String value = text == null ? "" : text.trim();
    if (!value.matches("\\d{1,6}")) return 0;
    return Integer.parseInt(value);
  }

  @Override public boolean scrollable() { return false; }
  @Override public PageId id() { return PageId.LONG_STOCK_REPORT; }
  @Override public String title() { return "Báo cáo tồn kho lâu ngày"; }
  @Override public String section() { return "Quản lý"; }
  @Override public JComponent view() { return view; }
}
