package com.dtaq.retail.ui.component.table;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqStyles;
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
  public static Color rowBackground(JTable table, int row, boolean selected) {
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
      setFont(DtaqFonts.CODE);
      setForeground(DtaqColors.PRIMARY_STRONG);
      return this;
    }
  }

  /**
   * Renderer hiển thị ô trạng thái bằng {@link StatusPill}. Chuỗi trong model giữ nguyên (sắp xếp,
   * so sánh không đổi); {@code mapper} chỉ quyết định phần chữ và loại pill khi vẽ.
   */
  public static class PillRenderer implements TableCellRenderer {

    /** Phần chữ hiển thị và loại pill tương ứng với một giá trị ô. */
    public record Pill(String text, StatusPill.Type type) {
    }

    private final java.util.function.Function<String, Pill> mapper;
    private final JPanel panel = new JPanel(new MigLayout("insets 0, fill", "[left]", "[center]"));
    private final StatusPill pill = StatusPill.neutral("");

    public PillRenderer(java.util.function.Function<String, Pill> mapper) {
      this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
      panel.setOpaque(true);
      panel.add(pill);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      String raw = value == null ? "" : value.toString();
      Pill p = mapper.apply(raw);
      boolean coPill = p != null && !p.text().isBlank();
      pill.setVisible(coPill);
      if (coPill) {
        pill.setText(p.text());
        pill.setType(p.type());
      }
      panel.setToolTipText(coPill ? p.text() : null);
      panel.setBackground(rowBackground(table, row, isSelected));
      int pad = cellPad(table);
      panel.setBorder(BorderFactory.createEmptyBorder(0, pad, 0, pad));
      return panel;
    }
  }

  /** Renderer cho số thường (số lượng, số ngày): căn phải. */
  public static class NumberRenderer extends TextRenderer {
    public NumberRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      // Chữ số đều cột để các số cùng hàng đơn vị thẳng nhau.
      setFont(DtaqFonts.NUMERIC_13);
      return this;
    }
  }

  /** Renderer cho số tiền: căn phải, định dạng bằng {@link MoneyFormat}. */
  public static class MoneyRenderer extends TextRenderer {
    public MoneyRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setFont(DtaqFonts.MONEY_TABLE);
      return this;
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
    private final java.util.function.IntFunction<List<RowAction>> actionProvider;
    private final JPanel renderPanel;
    private final JPanel editPanel;
    private JTable editingTable;
    private int editingRow = -1;

    public ActionCell(List<RowAction> actions) {
      this(row -> actions);
    }

    public ActionCell(java.util.function.IntFunction<List<RowAction>> actionProvider) {
      this.actionProvider = actionProvider == null ? row -> List.of() : actionProvider;
      this.renderPanel = buildPanel(false, -1);
      this.editPanel = buildPanel(true, -1);
    }

    private JPanel buildPanel(boolean live, int rowIndex) {
      JPanel panel = new JPanel(new MigLayout("insets 0 12 0 12, gapx 6, fill", "[]", "[center]"));
      panel.setOpaque(true);
      int modelRow = rowIndex;
      for (RowAction action : actionProvider.apply(modelRow)) {
        DtaqButton button = switch (action.style()) {
          case PRIMARY -> DtaqButton.secondarySmall(action.label());
          case DANGER -> DtaqButton.secondarySmall(action.label());
          case SECONDARY -> DtaqButton.secondarySmall(action.label());
        };
        // Trong bảng dùng nút nhẹ (viền/ghost) để các hành động không lấn át dữ liệu.
        button.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
            switch (action.style()) {
              case PRIMARY -> "background: " + DtaqStyles.hex(DtaqColors.SURFACE)
                  + "; foreground: " + DtaqStyles.hex(DtaqColors.PRIMARY_STRONG)
                  + "; borderColor: " + DtaqStyles.hex(DtaqColors.PRIMARY_BORDER_SOFT)
                  + "; hoverBackground: " + DtaqStyles.hex(DtaqColors.PRIMARY_SOFT)
                  + "; borderWidth: 1";
              case DANGER -> "background: " + DtaqStyles.hex(DtaqColors.SURFACE)
                  + "; foreground: " + DtaqStyles.hex(DtaqColors.DANGER)
                  + "; borderColor: " + DtaqStyles.hex(DtaqColors.DANGER_SOFT_HOVER)
                  + "; hoverBackground: " + DtaqStyles.hex(DtaqColors.DANGER_SOFT)
                  + "; borderWidth: 1";
              case SECONDARY -> "background: " + DtaqStyles.hex(DtaqColors.SURFACE)
                  + "; foreground: " + DtaqStyles.hex(DtaqColors.TEXT_MUTED)
                  + "; borderColor: " + DtaqStyles.hex(DtaqColors.BORDER)
                  + "; hoverBackground: " + DtaqStyles.hex(DtaqColors.DISABLED_BG)
                  + "; borderWidth: 1";
            } + DtaqStyles.SMALL_BUTTON_EXTRA);
        if (live) {
          button.addActionListener(e -> fire(action));
        }
        panel.add(button);
      }
      return panel;
    }

    /** Độ rộng đủ hiển thị trọn các nút của ô. */
    public int doRongToiThieu() {
      return renderPanel.getPreferredSize().width;
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
      int modelRow = table != null ? table.convertRowIndexToModel(row) : row;
      JPanel panel = buildPanel(false, modelRow);
      panel.setBackground(rowBackground(table, row, isSelected));
      return panel;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
      editingTable = table;
      editingRow = row;
      int modelRow = table != null ? table.convertRowIndexToModel(row) : row;
      JPanel panel = buildPanel(true, modelRow);
      panel.setBackground(rowBackground(table, row, isSelected));
      return panel;
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }
  }
}
