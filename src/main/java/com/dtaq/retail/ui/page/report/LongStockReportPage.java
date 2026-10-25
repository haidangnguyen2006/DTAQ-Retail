package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
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
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Báo cáo tồn kho lâu ngày (UC014, LongStockReportPage, Figma frame 1:2).
 *
 * <p>Ngưỡng số ngày tồn do Quản lý tự nhập (đặc tả UC014 không quy định giá trị mặc định). Trang chỉ hiển
 * thị kết quả và tổng hợp do controller cung cấp qua {@link #renderRows} và {@link #renderSummary}.
 */
public class LongStockReportPage implements Page {

  private static final String ALL_CATEGORIES = "Tất cả ngành hàng";
  private static final String NOT_CONNECTED_TIP = "Chức năng đang được kết nối dữ liệu";
  private static final String EMPTY_TITLE = "Chưa có báo cáo";
  private static final String EMPTY_HINT = "Nhập ngưỡng số ngày tồn kho rồi bấm Áp dụng.";

  public record LongStockRow(
      String maSP,
      String tenSP,
      String ngayNhap,
      int soNgayTon,
      int slTon,
      long giaNhap,
      long giaTriTon
  ) {}

  /** Tổng hợp của báo cáo do controller tính. */
  public record LongStockSummary(
      int soSanPham,
      int tongDonVi,
      long tongGiaTri,
      int soNgayTonLonNhat,
      String maSPTonLauNhat,
      String ngayNhapTonLauNhat
  ) {}

  /** Điều kiện lọc báo cáo; {@code category} là {@code null} khi chọn tất cả ngành hàng. */
  public record LongStockFilter(String keyword, String category, int soNgayToiThieu) {}

  private final JPanel view;
  private final KpiCard kpiProductCount = new KpiCard("Sản phẩm vượt ngưỡng", "—", "Chưa có dữ liệu");
  private final KpiCard kpiTotalValue = new KpiCard("Giá trị tồn theo giá nhập", "—", "VNĐ · Không dùng giá bán lẻ");
  private final KpiCard kpiMaxDays = new KpiCard("Thời gian tồn lâu nhất", "—", "Chưa có dữ liệu");
  private final FormField searchField = FormField.search("Tìm sản phẩm", "Mã hoặc tên sản phẩm");
  private final JComboBox<String> categoryCombo = new JComboBox<>(new String[] {ALL_CATEGORIES});
  private final FormField categoryField = new FormField("Ngành hàng", categoryCombo);
  private final FormField thresholdField = FormField.text("Ngưỡng số ngày tồn", "Nhập số ngày", null);
  private final DtaqButton applyBtn = DtaqButton.primary("Áp dụng");
  private final StatusPill resultCountPill = StatusPill.neutral("0 kết quả");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã", "Tên sản phẩm", "Ngày nhập", "Số ngày tồn ↓", "SL tồn", "Giá nhập / ĐV", "Giá trị tồn (VNĐ)"});
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;

  private LongStockSummary summary;
  private Consumer<LongStockFilter> onApplyFilterListener;

  public LongStockReportPage() {
    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        "Báo cáo tồn kho lâu ngày",
        "Các sản phẩm tồn kho vượt ngưỡng số ngày đã chọn · Tính đến "
            + DateFormats.formatDate(LocalDate.now()) + ".");
    DtaqButton exportBtn = DtaqButton.secondary("Xuất báo cáo", "download");
    exportBtn.setEnabled(false);
    exportBtn.setToolTipText("Chức năng xuất báo cáo Excel đang được hoàn thiện");
    header.setTrailing(exportBtn);
    view.add(header, "growx, wrap");

    JPanel kpiRow = new JPanel(new MigLayout(
        "fillx, insets 0", "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    kpiRow.add(kpiProductCount);
    kpiRow.add(kpiTotalValue);
    kpiRow.add(kpiMaxDays);
    view.add(kpiRow, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setNumberColumn(3);
    dataTable.setNumberColumn(4);
    dataTable.setMoneyColumn(5);
    dataTable.setMoneyColumn(6);
    dataTable.setColumnWidths(110, 360, 130, 130, 80, 140, 160);
    tableState = new StatefulView(dataTable, "package-search", EMPTY_TITLE, EMPTY_HINT);

    Card contentCard = Card.standard();
    contentCard.add(buildFilterRow(), "growx, wrap");
    contentCard.setBody(tableState);
    contentCard.add(buildTableFooter(), "growx");
    view.add(contentCard, "grow, push");

    refreshControls();
  }

  private JPanel buildFilterRow() {
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[280!]16[180!]16[160!]16[]push[]", "[bottom]"));
    filterRow.setOpaque(false);
    filterRow.add(searchField, "growx");
    filterRow.add(categoryField, "growx");
    thresholdField.setRequired(true);
    thresholdField.getTextField().addActionListener(e -> applyFilter());
    filterRow.add(thresholdField, "growx");
    applyBtn.addActionListener(e -> applyFilter());
    filterRow.add(applyBtn);
    filterRow.add(resultCountPill, "align right, aligny center");
    return filterRow;
  }

  private JPanel buildTableFooter() {
    JPanel footer = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    footer.setOpaque(false);
    footer.add(hint("Sắp xếp theo số ngày tồn giảm dần"), "align left");
    footer.add(hint("Giá trị tồn = Số lượng tồn × Giá nhập"), "align right");
    return footer;
  }

  private static JLabel hint(String text) {
    JLabel label = new JLabel(text);
    label.setFont(DtaqFonts.CAPTION);
    label.setForeground(DtaqColors.TEXT_MUTED);
    return label;
  }

  // ---------------------------------------------------------------- public API

  /** Nạp danh sách ngành hàng (ngoài mục "Tất cả ngành hàng") cho bộ lọc. */
  public void setCategories(List<String> categories) {
    Object selected = categoryCombo.getSelectedItem();
    categoryCombo.removeAllItems();
    categoryCombo.addItem(ALL_CATEGORIES);
    if (categories != null) {
      categories.forEach(categoryCombo::addItem);
    }
    if (selected != null) {
      categoryCombo.setSelectedItem(selected);
    }
  }

  /** Hiển thị tổng hợp (3 chỉ số đầu trang và dòng tổng cộng cuối bảng). */
  public void renderSummary(LongStockSummary newSummary) {
    this.summary = newSummary;
    if (newSummary == null) {
      kpiProductCount.setValue(null);
      kpiProductCount.setCaption("Chưa có dữ liệu");
      kpiTotalValue.setValue(null);
      kpiMaxDays.setValue(null);
      kpiMaxDays.setCaption("Chưa có dữ liệu");
      return;
    }
    kpiProductCount.setValue(String.valueOf(newSummary.soSanPham()));
    kpiProductCount.setCaption(newSummary.tongDonVi() + " đơn vị còn tồn trong kho");
    kpiTotalValue.setValue(MoneyFormat.format(newSummary.tongGiaTri()));
    kpiMaxDays.setValue(newSummary.soNgayTonLonNhat() + " ngày");
    kpiMaxDays.setCaption(newSummary.maSPTonLauNhat() + " · Nhập ngày " + newSummary.ngayNhapTonLauNhat());
  }

  /** Hiển thị kết quả báo cáo; danh sách rỗng báo "không có dữ liệu phù hợp" (UC014). */
  public void renderRows(List<LongStockRow> rows) {
    tableModel.setRowCount(0);
    if (rows == null || rows.isEmpty()) {
      resultCountPill.setText("0 kết quả");
      tableState.showEmpty("package-search", "Không có sản phẩm vượt ngưỡng",
          "Không có dữ liệu phù hợp với ngưỡng và bộ lọc đã chọn.");
      return;
    }
    resultCountPill.setText(rows.size() + " kết quả");
    for (LongStockRow r : rows) {
      tableModel.addRow(new Object[] {
          r.maSP(), r.tenSP(), r.ngayNhap(), r.soNgayTon(), r.slTon(), r.giaNhap(), r.giaTriTon()});
    }
    if (summary != null) {
      tableModel.addRow(new Object[] {
          "Tổng cộng", summary.soSanPham() + " sản phẩm", "—", "—",
          summary.tongDonVi(), "—", summary.tongGiaTri()});
    }
    tableState.showContent();
  }

  public void showLoading() {
    kpiProductCount.setLoading();
    kpiTotalValue.setLoading();
    kpiMaxDays.setLoading();
    tableState.showLoading();
  }

  public void showError(String message) {
    tableState.showError(message);
  }

  public void setOnApplyFilter(Consumer<LongStockFilter> listener) {
    this.onApplyFilterListener = listener;
    refreshControls();
  }

  // ---------------------------------------------------------------- hành vi

  private void refreshControls() {
    boolean connected = onApplyFilterListener != null;
    applyBtn.setEnabled(connected);
    applyBtn.setToolTipText(connected ? null : NOT_CONNECTED_TIP);
  }

  private void applyFilter() {
    if (onApplyFilterListener == null) {
      return;
    }
    int threshold = parseThreshold(thresholdField.getText());
    if (threshold <= 0) {
      thresholdField.setError("Ngưỡng số ngày tồn kho phải là số nguyên dương");
      return;
    }
    thresholdField.setError(null);
    String category = categoryCombo.getSelectedIndex() <= 0 ? null : (String) categoryCombo.getSelectedItem();
    onApplyFilterListener.accept(new LongStockFilter(searchField.getText().trim(), category, threshold));
  }

  /** Số nguyên dương người dùng nhập, hoặc 0 nếu rỗng/không hợp lệ. */
  static int parseThreshold(String text) {
    String trimmed = text == null ? "" : text.trim();
    if (!trimmed.matches("\\d{1,6}")) {
      return 0;
    }
    return Integer.parseInt(trimmed);
  }

  // ---------------------------------------------------------------- Page

  /** Bảng tự giãn hết chiều cao khung và tự cuộn bên trong. */
  @Override
  public boolean scrollable() {
    return false;
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
