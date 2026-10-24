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

  private final JTable table;
  private final JScrollPane scrollPane;
  private final Set<Integer> rightAlignedColumns = new HashSet<>();
  private int minVisibleRows = DEFAULT_MIN_VISIBLE_ROWS;
  private int maxVisibleRows = DEFAULT_MAX_VISIBLE_ROWS;
  private int cellPadding = 16;

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
    table.setRowHeight(rowHeight);
    table.setShowVerticalLines(false);
    table.setShowHorizontalLines(true);
    table.setGridColor(DtaqColors.BORDER);
    table.setSelectionBackground(DtaqColors.PRIMARY_SOFT);
    table.setSelectionForeground(DtaqColors.TEXT);
    table.setFont(DtaqFonts.BODY_13);
    table.setIntercellSpacing(new Dimension(0, 1));
    table.setFillsViewportHeight(true);
    table.setFocusable(false);

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

  /** Lề ngang của ô và tiêu đề (mặc định 16); dùng giá trị nhỏ hơn cho bảng hẹp như giỏ hàng. */
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
      }
    }
  }

  public void setCodeColumn(int colIndex) {
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.CodeRenderer());
  }

  public void setMoneyColumn(int colIndex) {
    rightAlignedColumns.add(colIndex);
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.MoneyRenderer());
  }

  /** Cột số thường (số lượng, số ngày…): căn phải cả nội dung và tiêu đề. */
  public void setNumberColumn(int colIndex) {
    rightAlignedColumns.add(colIndex);
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.NumberRenderer());
  }

  public void setTwoLineColumn(int colIndex) {
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.TwoLineRenderer());
  }

  public void setCustomRenderer(int colIndex, TableCellRenderer renderer) {
    table.getColumnModel().getColumn(colIndex).setCellRenderer(renderer);
  }

  /**
   * Biến một cột thành cột thao tác với các nút bấm được. Cột phải nằm trong danh sách cột
   * "editable" khi tạo model bằng {@link #readOnlyModel}.
   */
  public void setActionColumn(int colIndex, RowAction... actions) {
    TableRenderers.ActionCell cell = new TableRenderers.ActionCell(List.of(actions));
    table.getColumnModel().getColumn(colIndex).setCellRenderer(cell);
    table.getColumnModel().getColumn(colIndex).setCellEditor(cell);
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
      setBackground(DtaqColors.BG_APP);
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
