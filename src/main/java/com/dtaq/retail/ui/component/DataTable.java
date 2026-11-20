package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.component.table.TableRenderers;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Bảng dữ liệu chuẩn (DataTable): thanh cuộn, header gọn, hover dòng, tooltip khi chữ bị cắt,
 * cột số căn phải và cột thao tác có nút bấm được.
 */
public class DataTable extends JPanel {

  private static final int DEFAULT_MIN_VISIBLE_ROWS = 3;
  private static final int DEFAULT_MAX_VISIBLE_ROWS = 8;
  /** Số dòng tối đa được đo khi tính bề rộng cột (bảng đã phân trang nên đủ đại diện). */
  private static final int MAX_SAMPLE_ROWS = 60;
  /** Nội dung hẹp hơn mức này (ngày, đơn vị, trạng thái ngắn) được giữ trọn như cột cố định. */
  private static final int SHORT_CONTENT_MAX = 150;
  /** Bề rộng tối thiểu của cột chữ dài trước khi chữ bị rút gọn bằng "…". */
  private static final int FLEX_MIN = 140;
  /** Mức co thêm của cột chữ dài khi bảng chật, để tránh cuộn ngang. */
  private static final int FLEX_HARD_MIN = 84;
  /** Giới hạn bề rộng tối thiểu của cột cố định để một giá trị bất thường không chiếm cả bảng. */
  private static final int RIGID_MAX = 320;

  private final JTable table;
  private final JScrollPane scrollPane;
  private final Set<Integer> rightAlignedColumns = new HashSet<>();
  private int minVisibleRows = DEFAULT_MIN_VISIBLE_ROWS;
  private int maxVisibleRows = DEFAULT_MAX_VISIBLE_ROWS;
  private int cellPadding = 12;
  /** Cột (theo model) có nội dung cố định không được cắt: mã, số, tiền, trạng thái, thao tác. */
  private final Set<Integer> rigidColumns = new HashSet<>();
  /** Bề rộng trang yêu cầu cho từng cột (theo model), dùng làm tỉ lệ khi còn dư chỗ. */
  private final java.util.Map<Integer, Integer> requestedWidths = new java.util.HashMap<>();
  private final java.util.Map<Integer, Integer> comfortMins = new java.util.HashMap<>();
  private final java.util.Map<Integer, Integer> hardMins = new java.util.HashMap<>();
  private final Set<Integer> flexibleColumns = new HashSet<>();
  private int comfortWidth;
  private int hardWidth;
  /** Mức co đang áp dụng: 0 thoải mái, 1 co cột chữ, 2 cuộn ngang; -1 buộc tính lại. */
  private int lastTier = -1;
  private boolean fitScheduled;

  /** Tạo model chỉ đọc; các cột trong {@code editableColumns} (ví dụ cột thao tác) vẫn bấm được. */
  public static DefaultTableModel readOnlyModel(String[] columns, int... editableColumns) {
    Set<Integer> editable = new HashSet<>();
    for (int column : editableColumns) {
      editable.add(column);
    }
    return new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return editable.contains(column);
      }
    };
  }

  public DataTable(TableModel model) {
    this(model, DtaqSpacing.TABLE_ROW_H);
  }

  public DataTable(TableModel model, int rowHeight) {
    super(new BorderLayout());
    setOpaque(false);

    table = new JTable(model) {
      @Override
      public String getToolTipText(MouseEvent event) {
        Point p = event.getPoint();
        int row = rowAtPoint(p);
        int col = columnAtPoint(p);
        if (row < 0 || col < 0) {
          return null;
        }
        Component cell = prepareRenderer(getCellRenderer(row, col), row, col);
        if (cell instanceof JLabel label
            && label.getText() != null && !label.getText().isBlank()
            && label.getPreferredSize().width > getCellRect(row, col, false).width) {
          return label.getText();
        }
        return null;
      }
    };
    table.setRowHeight(DtaqSpacing.normalizeRowHeight(rowHeight));
    table.setShowVerticalLines(false);
    table.setShowHorizontalLines(true);
    table.setGridColor(DtaqColors.CHART_GRID);
    table.setSelectionBackground(DtaqColors.PRIMARY_SOFT);
    table.setSelectionForeground(DtaqColors.TEXT);
    table.setFont(DtaqFonts.BODY_13);
    table.setIntercellSpacing(new Dimension(0, 1));
    table.setFillsViewportHeight(true);
    table.setFocusable(true);

    TableRenderers.TextRenderer textRenderer = new TableRenderers.TextRenderer();
    table.setDefaultRenderer(Object.class, textRenderer);
    table.setDefaultRenderer(String.class, textRenderer);
    table.setDefaultRenderer(Number.class, new TableRenderers.NumberRenderer());

    JTableHeader header = table.getTableHeader();
    header.setReorderingAllowed(false);
    header.setDefaultRenderer(new HeaderRenderer());
    header.setPreferredSize(new Dimension(0, DtaqSpacing.TABLE_HEADER_H));

    installHover();
    model.addTableModelListener(this::onModelChanged);

    scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.getViewport().setBackground(DtaqColors.SURFACE);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        applyColumnLayout();
      }
    });
    updateViewportHeight();

    add(scrollPane, BorderLayout.CENTER);
  }

  private void installHover() {
    MouseAdapter hover = new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        setHoverRow(table.rowAtPoint(e.getPoint()));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        setHoverRow(-1);
      }
    };
    table.addMouseMotionListener(hover);
    table.addMouseListener(hover);
  }

  private void setHoverRow(int row) {
    Object current = table.getClientProperty(TableRenderers.HOVER_ROW_KEY);
    if (current instanceof Integer c && c == row) {
      return;
    }
    table.putClientProperty(TableRenderers.HOVER_ROW_KEY, row);
    table.repaint();
  }

  private void onModelChanged(TableModelEvent event) {
    updateViewportHeight();
    scheduleFit();
  }

  /** Gom nhiều thay đổi dữ liệu liên tiếp thành một lần đo cột. */
  private void scheduleFit() {
    if (fitScheduled) {
      return;
    }
    fitScheduled = true;
    javax.swing.SwingUtilities.invokeLater(() -> {
      fitScheduled = false;
      fitColumns();
    });
  }

  /**
   * Đo tiêu đề và nội dung để đặt bề rộng tối thiểu từng cột: tiêu đề không bao giờ bị cắt; cột cố định
   * (mã, số, tiền, ngày, trạng thái, thao tác) giữ trọn nội dung; cột chữ dài co dần (rút gọn "…" kèm
   * tooltip) trước khi bảng phải cuộn ngang. Chỗ dư được dồn cho cột chữ dài.
   */
  private void fitColumns() {
    TableColumnModel cm = table.getColumnModel();
    int n = cm.getColumnCount();
    int rows = Math.min(table.getRowCount(), MAX_SAMPLE_ROWS);
    comfortMins.clear();
    hardMins.clear();
    flexibleColumns.clear();
    comfortWidth = 0;
    hardWidth = 0;
    for (int c = 0; c < n; c++) {
      javax.swing.table.TableColumn column = cm.getColumn(c);
      int model = column.getModelIndex();
      requestedWidths.computeIfAbsent(model, k -> column.getPreferredWidth());
      int header = headerWidth(column, c);
      int content = -1;
      for (int r = 0; r < rows; r++) {
        Component cell = table.prepareRenderer(table.getCellRenderer(r, c), r, c);
        content = Math.max(content, cell.getPreferredSize().width + 2);
      }
      boolean rigid = rigidColumns.contains(model) || (content >= 0 && content <= SHORT_CONTENT_MAX);
      int comfort;
      int hard;
      if (rigid) {
        comfort = Math.max(header, Math.min(content, RIGID_MAX));
        hard = comfort;
      } else {
        int text = content < 0 ? FLEX_MIN : content;
        comfort = Math.max(header, Math.min(text, FLEX_MIN));
        hard = Math.max(header, Math.min(text, FLEX_HARD_MIN));
        flexibleColumns.add(model);
      }
      comfortMins.put(model, comfort);
      hardMins.put(model, hard);
      comfortWidth += comfort;
      hardWidth += hard;
    }
    lastTier = -1;
    applyColumnLayout();
  }

  /** Chọn mức co theo bề rộng vùng nhìn: thoải mái, co cột chữ, hoặc cuộn ngang. */
  private void applyColumnLayout() {
    int viewport = scrollPane.getViewport().getWidth();
    int tier = viewport <= 0 || viewport >= comfortWidth ? 0 : viewport >= hardWidth ? 1 : 2;
    if (tier == lastTier) {
      return;
    }
    lastTier = tier;
    boolean anyFlexible = !flexibleColumns.isEmpty();
    TableColumnModel cm = table.getColumnModel();
    for (int c = 0; c < cm.getColumnCount(); c++) {
      javax.swing.table.TableColumn column = cm.getColumn(c);
      int model = column.getModelIndex();
      Integer comfort = comfortMins.get(model);
      if (comfort == null) {
        continue;
      }
      // Đã phải cuộn ngang thì giữ bề rộng thoải mái cho dễ đọc thay vì vừa cuộn vừa rút gọn chữ.
      int min = tier == 1 ? hardMins.get(model) : comfort;
      int requested = requestedWidths.getOrDefault(model, column.getPreferredWidth());
      // Bỏ trần cũ trước: TableColumn kẹp minWidth theo maxWidth hiện tại.
      column.setMaxWidth(Integer.MAX_VALUE);
      column.setMinWidth(min);
      // Cột cố định chỉ rộng tới mức trang yêu cầu; phần dư thuộc về cột chữ dài.
      if (anyFlexible && !flexibleColumns.contains(model)) {
        column.setMaxWidth(Math.max(min, requested));
      }
      column.setPreferredWidth(tier == 2 ? min : Math.max(min, requested));
    }
    table.setAutoResizeMode(tier == 2 ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
  }

  private int headerWidth(javax.swing.table.TableColumn column, int viewIndex) {
    TableCellRenderer renderer = column.getHeaderRenderer() != null
        ? column.getHeaderRenderer() : table.getTableHeader().getDefaultRenderer();
    return renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, viewIndex)
        .getPreferredSize().width + 2;
  }

  /** Chiều cao vùng nhìn theo số dòng hiện có, giới hạn trong [min, max] dòng. */
  private void updateViewportHeight() {
    int rows = Math.max(minVisibleRows, Math.min(maxVisibleRows, table.getRowCount()));
    table.setPreferredScrollableViewportSize(new Dimension(0, rows * table.getRowHeight()));
    revalidate();
  }

  /** Giới hạn số dòng hiển thị trước khi bảng tự cuộn (khi không được giãn hết chiều cao). */
  public void setVisibleRows(int min, int max) {
    this.minVisibleRows = Math.max(1, min);
    this.maxVisibleRows = Math.max(this.minVisibleRows, max);
    updateViewportHeight();
  }

  /** Lề ngang của ô và tiêu đề (mặc định 12); dùng giá trị nhỏ hơn cho bảng hẹp như giỏ hàng. */
  public void setCellPadding(int padding) {
    this.cellPadding = padding;
    table.putClientProperty(TableRenderers.CELL_PAD_KEY, padding);
    table.getTableHeader().repaint();
    table.repaint();
  }

  public JTable getTable() {
    return table;
  }

  public JScrollPane getScrollPane() {
    return scrollPane;
  }

  public void setColumnWidths(int... widths) {
    TableColumnModel colModel = table.getColumnModel();
    for (int i = 0; i < Math.min(widths.length, colModel.getColumnCount()); i++) {
      int w = widths[i];
      if (w > 0) {
        colModel.getColumn(i).setPreferredWidth(w);
        requestedWidths.put(colModel.getColumn(i).getModelIndex(), w);
      }
    }
    fitColumns();
  }

  public void setCodeColumn(int colIndex) {
    rigidColumns.add(colIndex);
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.CodeRenderer());
    scheduleFit();
  }

  public void setMoneyColumn(int colIndex) {
    rightAlignedColumns.add(colIndex);
    rigidColumns.add(colIndex);
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.MoneyRenderer());
    scheduleFit();
  }

  /** Cột số thường (số lượng, số ngày…): căn phải cả nội dung và tiêu đề. */
  public void setNumberColumn(int colIndex) {
    rightAlignedColumns.add(colIndex);
    rigidColumns.add(colIndex);
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.NumberRenderer());
    scheduleFit();
  }

  public void setTwoLineColumn(int colIndex) {
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.TwoLineRenderer());
  }

  public void setCustomRenderer(int colIndex, TableCellRenderer renderer) {
    if (renderer instanceof TableRenderers.PillRenderer) {
      rigidColumns.add(colIndex);
    }
    table.getColumnModel().getColumn(colIndex).setCellRenderer(renderer);
    scheduleFit();
  }

  /**
   * Biến một cột thành cột thao tác với các nút bấm được. Cột phải nằm trong danh sách cột
   * "editable" khi tạo model bằng {@link #readOnlyModel}.
   */
  public void setActionColumn(int colIndex, RowAction... actions) {
    setActionColumn(colIndex, row -> List.of(actions));
  }

  /** Cho phép cột thao tác hiển thị các nút theo từng dòng. */
  public void setActionColumn(int colIndex, java.util.function.IntFunction<List<RowAction>> actionProvider) {
    TableRenderers.ActionCell cell = new TableRenderers.ActionCell(actionProvider);
    table.getColumnModel().getColumn(colIndex).setCellRenderer(cell);
    table.getColumnModel().getColumn(colIndex).setCellEditor(cell);
    // Bảng tự co cột theo chiều rộng còn lại; không cho cột thao tác co đến mức cắt mất nút.
    rigidColumns.add(colIndex);
    table.getColumnModel().getColumn(colIndex).setMinWidth(cell.doRongToiThieu());
    scheduleFit();
  }

  /** Cho phép bấm tiêu đề cột để sắp xếp. */
  public void setSortable(boolean sortable) {
    table.setAutoCreateRowSorter(sortable);
  }

  /** Gọi khi nhấp đúp một dòng; tham số là chỉ số dòng theo model. */
  public void setOnRowDoubleClick(IntConsumer listener) {
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && listener != null) {
          int viewRow = table.rowAtPoint(e.getPoint());
          if (viewRow >= 0) {
            listener.accept(table.convertRowIndexToModel(viewRow));
          }
        }
      }
    });
  }

  /** Renderer tiêu đề: nền xám nhạt, chữ nhỏ đậm vừa, căn phải cho cột số. */
  private final class HeaderRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(t, value, false, false, row, column);
      setFont(DtaqFonts.TABLE_HEADER);
      setForeground(DtaqColors.TEXT_MUTED);
      setBackground(DtaqColors.SURFACE_SUNKEN);
      setHorizontalAlignment(
          rightAlignedColumns.contains(t.convertColumnIndexToModel(column))
              ? SwingConstants.RIGHT : SwingConstants.LEFT);
      setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(0, 0, 1, 0, DtaqColors.BORDER),
          BorderFactory.createEmptyBorder(0, cellPadding, 0, cellPadding)));
      return this;
    }
  }
}
