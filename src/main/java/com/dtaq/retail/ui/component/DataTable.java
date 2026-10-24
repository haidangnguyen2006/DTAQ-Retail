package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.component.table.TableRenderers;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Bảng dữ liệu chuẩn (DataTable) được đóng gói kèm thanh cuộn và định dạng dòng theo Figma.
 */
public class DataTable extends JPanel {

  private final JTable table;
  private final JScrollPane scrollPane;

  public DataTable(TableModel model) {
    this(model, DtaqSpacing.TABLE_ROW_H);
  }

  public DataTable(TableModel model, int rowHeight) {
    super(new BorderLayout());
    setOpaque(false);

    table = new JTable(model);
    table.setRowHeight(rowHeight);
    table.setShowVerticalLines(false);
    table.setShowHorizontalLines(true);
    table.setGridColor(DtaqColors.BORDER);
    table.setSelectionBackground(DtaqColors.PRIMARY_SOFT);
    table.setSelectionForeground(DtaqColors.TEXT);
    table.setFont(DtaqFonts.BODY_13);
    table.setIntercellSpacing(new Dimension(0, 1));
    table.setFillsViewportHeight(true);

    // Table Header
    table.getTableHeader().setPreferredSize(new Dimension(0, DtaqSpacing.TABLE_HEADER_H));
    table.getTableHeader().setBackground(DtaqColors.BG_APP);
    table.getTableHeader().setForeground(DtaqColors.TEXT_MUTED);
    table.getTableHeader().setFont(DtaqFonts.TABLE_HEADER);
    table.getTableHeader().setReorderingAllowed(false);
    table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DtaqColors.BORDER));

    // Default cell padding
    DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
    defaultRenderer.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
    table.setDefaultRenderer(Object.class, defaultRenderer);
    table.setDefaultRenderer(String.class, defaultRenderer);

    scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createLineBorder(DtaqColors.BORDER, 1, true));
    scrollPane.getViewport().setBackground(DtaqColors.SURFACE);

    add(scrollPane, BorderLayout.CENTER);
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
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.MoneyRenderer());
  }

  public void setTwoLineColumn(int colIndex) {
    table.getColumnModel().getColumn(colIndex).setCellRenderer(new TableRenderers.TwoLineRenderer());
  }

  public void setCustomRenderer(int colIndex, TableCellRenderer renderer) {
    table.getColumnModel().getColumn(colIndex).setCellRenderer(renderer);
  }
}
