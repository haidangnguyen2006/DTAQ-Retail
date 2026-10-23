package com.dtaq.retail.ui.component.table;

import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Component;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import net.miginfocom.swing.MigLayout;

/**
 * Tập hợp các bộ vẽ ô (Table Cell Renderers) dùng chung cho các bảng DataTable trong DTAQ.
 */
public final class TableRenderers {

  private TableRenderers() {
  }

  /**
   * Renderer cho cột mã định danh (cột đầu), font Medium 13.
   */
  public static class CodeRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setFont(DtaqFonts.BODY_13_MEDIUM);
      setForeground(isSelected ? DtaqColors.TEXT : DtaqColors.TEXT);
      setBackground(isSelected ? DtaqColors.PRIMARY_SOFT : DtaqColors.SURFACE);
      setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
      return this;
    }
  }

  /**
   * Renderer cho số tiền, căn phải và định dạng bằng MoneyFormat.
   */
  public static class MoneyRenderer extends DefaultTableCellRenderer {
    public MoneyRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      String formatted;
      if (value instanceof Number n) {
        formatted = MoneyFormat.format(n.longValue());
      } else if (value instanceof BigDecimal bd) {
        formatted = MoneyFormat.format(bd);
      } else {
        formatted = (value != null) ? value.toString() : "0";
      }
      super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column);
      setFont(DtaqFonts.BODY_13);
      setForeground(isSelected ? DtaqColors.TEXT : DtaqColors.TEXT);
      setBackground(isSelected ? DtaqColors.PRIMARY_SOFT : DtaqColors.SURFACE);
      setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
      return this;
    }
  }

  /**
   * Renderer hiển thị 2 dòng trong 1 ô (ví dụ: Khách hàng & Số điện thoại).
   */
  public static class TwoLineRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
    private final JLabel topLabel = new JLabel();
    private final JLabel bottomLabel = new JLabel();

    public TwoLineRenderer() {
      super(new MigLayout("insets 4 16 4 16", "[]", "[]2[]"));
      setOpaque(true);
      topLabel.setFont(DtaqFonts.BODY_13);
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
      } else if (value != null) {
        topLabel.setText(value.toString());
        bottomLabel.setText("");
      }
      setBackground(isSelected ? DtaqColors.PRIMARY_SOFT : DtaqColors.SURFACE);
      return this;
    }
  }
}
