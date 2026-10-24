package com.dtaq.retail.ui.component.table;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import net.miginfocom.swing.MigLayout;

/**
 * Tập hợp các bộ vẽ ô (Table Cell Renderers) dùng chung cho các bảng DataTable trong DTAQ.
 */
public final class TableRenderers {

  /** Khóa client property của {@link JTable} lưu chỉ số dòng đang được rê chuột (Integer). */
  public static final String HOVER_ROW_KEY = "dtaq.hoverRow";

  /** Khóa client property của {@link JTable} lưu lề ngang của ô (Integer, mặc định 16). */
  public static final String CELL_PAD_KEY = "dtaq.cellPad";

  private static final int CELL_PAD = 16;

  /** Lề ngang của ô theo cấu hình của bảng. */
  public static int cellPad(JTable table) {
    return table.getClientProperty(CELL_PAD_KEY) instanceof Integer pad ? pad : CELL_PAD;
  }

  private TableRenderers() {
  }

  /** Màu nền của một dòng theo trạng thái chọn / rê chuột. */
  static Color rowBackground(JTable table, int row, boolean selected) {
    if (selected) {
      return DtaqColors.PRIMARY_SOFT;
    }
    Object hover = table.getClientProperty(HOVER_ROW_KEY);
    if (hover instanceof Integer hoverRow && hoverRow == row) {
      return DtaqColors.ROW_HOVER;
    }
    return DtaqColors.SURFACE;
  }

  /** Renderer văn bản mặc định: đệm trái/phải, nền theo chọn/hover. */
  public static class TextRenderer extends DefaultTableCellRenderer {
    public TextRenderer() {
      setBorder(BorderFactory.createEmptyBorder(0, CELL_PAD, 0, CELL_PAD));
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, format(value), isSelected, false, row, column);
      setFont(DtaqFonts.BODY_13);
      setForeground(DtaqColors.TEXT);
      setBackground(rowBackground(table, row, isSelected));
      int pad = cellPad(table);
      setBorder(BorderFactory.createEmptyBorder(0, pad, 0, pad));
      return this;
    }

    protected Object format(Object value) {
      return value;
    }
  }

  /** Renderer cho cột mã định danh, chữ đậm vừa. */
  public static class CodeRenderer extends TextRenderer {
    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setFont(DtaqFonts.BODY_13_MEDIUM);
      return this;
    }
  }

  /** Renderer cho số thường (số lượng, số ngày): căn phải. */
  public static class NumberRenderer extends TextRenderer {
    public NumberRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  /** Renderer cho số tiền: căn phải, định dạng bằng {@link MoneyFormat}. */
  public static class MoneyRenderer extends TextRenderer {
    public MoneyRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    protected Object format(Object value) {
      if (value instanceof Number n) {
        return MoneyFormat.format(n.longValue());
      }
      if (value instanceof BigDecimal bd) {
        return MoneyFormat.format(bd);
      }
      return value != null ? value.toString() : "";
    }
  }

  /** Renderer hiển thị 2 dòng trong 1 ô (ví dụ: Khách hàng & Số điện thoại). */
  public static class TwoLineRenderer extends JPanel implements TableCellRenderer {
    private final JLabel topLabel = new JLabel();
    private final JLabel bottomLabel = new JLabel();

    public TwoLineRenderer() {
      super(new MigLayout("insets 0 " + CELL_PAD + " 0 " + CELL_PAD + ", gapy 2, fill", "[grow]", "push[]2[]push"));
      setOpaque(true);
      topLabel.setFont(DtaqFonts.BODY_13);
      topLabel.setForeground(DtaqColors.TEXT);
      bottomLabel.setFont(DtaqFonts.CAPTION);
      bottomLabel.setForeground(DtaqColors.TEXT_MUTED);
      add(topLabel, "wrap");
      add(bottomLabel);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof String[] parts && parts.length >= 2) {
        topLabel.setText(parts[0]);
        bottomLabel.setText(parts[1]);
      } else {
        topLabel.setText(value != null ? value.toString() : "");
        bottomLabel.setText("");
      }
      setBackground(rowBackground(table, row, isSelected));
      return this;
    }
  }

  /** Renderer và editor cho cột thao tác: hiển thị các nút bấm được trong ô bảng. */
  public static class ActionCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
    private final List<RowAction> actions;
    private final JPanel renderPanel;
    private final JPanel editPanel;
    private JTable editingTable;
    private int editingRow = -1;

    public ActionCell(List<RowAction> actions) {
      this.actions = List.copyOf(actions);
      this.renderPanel = buildPanel(false);
      this.editPanel = buildPanel(true);
    }

    private JPanel buildPanel(boolean live) {
      JPanel panel = new JPanel(new MigLayout("insets 0 12 0 12, gapx 6, fill", "[]", "[center]"));
      panel.setOpaque(true);
      for (RowAction action : actions) {
        DtaqButton button = switch (action.style()) {
          case PRIMARY -> DtaqButton.primarySmall(action.label());
          case DANGER -> DtaqButton.dangerSmall(action.label());
          case SECONDARY -> DtaqButton.secondarySmall(action.label());
        };
        if (live) {
          button.addActionListener(e -> fire(action));
        }
        panel.add(button);
      }
      return panel;
    }

    private void fire(RowAction action) {
      JTable table = editingTable;
      int viewRow = editingRow;
      fireEditingStopped();
      if (table != null && viewRow >= 0 && viewRow < table.getRowCount()) {
        action.onClick().accept(table.convertRowIndexToModel(viewRow));
      }
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      renderPanel.setBackground(rowBackground(table, row, isSelected));
      return renderPanel;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
      editingTable = table;
      editingRow = row;
      editPanel.setBackground(rowBackground(table, row, isSelected));
      return editPanel;
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }
  }
}
