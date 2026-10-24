package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * Biểu đồ cột tự vẽ bằng Java2D (BarChartPanel). Giá trị nhận vào theo đơn vị do trang quyết định
 * (mặc định: triệu đồng); không có dữ liệu thì hiện thông báo rỗng thay vì trục trống.
 */
public class BarChartPanel extends JPanel {

  public record BarPoint(String label, double valueMillions) {}

  private static final Locale VI = Locale.of("vi", "VN");
  private static final int AXIS_STEPS = 4;
  private static final int LEFT_MARGIN = 56;
  private static final int RIGHT_MARGIN = 24;
  private static final int TOP_MARGIN = 30;
  private static final int BOTTOM_MARGIN = 36;

  private final List<BarPoint> points = new ArrayList<>();
  private final List<Rectangle2D> barBounds = new ArrayList<>();
  private double axisMax = AXIS_STEPS;
  private String unitLabel = "Triệu ₫";
  private String valueSuffix = "tr";
  private String emptyText = "Chưa có dữ liệu thống kê";

  public BarChartPanel() {
    setOpaque(false);
    setPreferredSize(new Dimension(800, 250));
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  /** Đặt nhãn đơn vị của trục tung và hậu tố hiển thị trên đầu mỗi cột (ví dụ "Triệu ₫", "tr"). */
  public void setUnit(String unitLabel, String valueSuffix) {
    this.unitLabel = unitLabel;
    this.valueSuffix = valueSuffix;
    repaint();
  }

  public void setEmptyText(String emptyText) {
    this.emptyText = emptyText;
    repaint();
  }

  public void setData(List<BarPoint> newPoints) {
    points.clear();
    if (newPoints != null) {
      points.addAll(newPoints);
    }
    double maxVal = 0;
    for (BarPoint p : points) {
      maxVal = Math.max(maxVal, p.valueMillions());
    }
    axisMax = niceAxisMax(maxVal);
    repaint();
  }

  /** Giá trị lớn nhất của trục, làm tròn lên bậc "đẹp" (1, 2, 2,5, 5 × 10^n) chia đều cho 4 khoảng. */
  static double niceAxisMax(double maxValue) {
    if (maxValue <= 0) {
      return AXIS_STEPS;
    }
    double rawStep = maxValue / AXIS_STEPS;
    double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    double normalized = rawStep / magnitude;
    double step = (normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 2.5 ? 2.5 : normalized <= 5 ? 5 : 10)
        * magnitude;
    return step * AXIS_STEPS;
  }

  private static String formatValue(double v) {
    return v == Math.rint(v) ? String.valueOf((long) v) : String.format(VI, "%.1f", v);
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    for (int i = 0; i < barBounds.size(); i++) {
      if (barBounds.get(i).contains(event.getPoint())) {
        BarPoint p = points.get(i);
        return p.label() + ": " + formatValue(p.valueMillions()) + " " + valueSuffix;
      }
    }
    return null;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    barBounds.clear();

    int width = getWidth();
    int height = getHeight();

    if (points.isEmpty()) {
      g2.setFont(DtaqFonts.BODY_13);
      g2.setColor(DtaqColors.TEXT_MUTED);
      FontMetrics fm = g2.getFontMetrics();
      g2.drawString(emptyText, (width - fm.stringWidth(emptyText)) / 2, height / 2);
      g2.dispose();
      return;
    }

    g2.setFont(DtaqFonts.HINT);
    g2.setColor(DtaqColors.TEXT_SUBTLE);
    g2.drawString(unitLabel, 16, 20);

    int chartHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
    int chartWidth = width - LEFT_MARGIN - RIGHT_MARGIN;

    // Trục tung + đường lưới
    for (int i = 0; i <= AXIS_STEPS; i++) {
      double stepVal = (axisMax / AXIS_STEPS) * i;
      int y = TOP_MARGIN + chartHeight - (int) ((stepVal / axisMax) * chartHeight);
      g2.setColor(DtaqColors.BORDER);
      g2.drawLine(LEFT_MARGIN, y, LEFT_MARGIN + chartWidth, y);
      g2.setFont(DtaqFonts.HINT);
      g2.setColor(DtaqColors.TEXT_SUBTLE);
      String label = formatValue(stepVal);
      g2.drawString(label, LEFT_MARGIN - g2.getFontMetrics().stringWidth(label) - 12, y + 4);
    }

    int n = points.size();
    int slotWidth = chartWidth / n;
    int barWidth = Math.min(72, (int) (slotWidth * 0.55));

    for (int i = 0; i < n; i++) {
      BarPoint pt = points.get(i);
      int slotCenterX = LEFT_MARGIN + i * slotWidth + slotWidth / 2;
      int barX = slotCenterX - barWidth / 2;
      int barH = (int) ((pt.valueMillions() / axisMax) * chartHeight);
      int barY = TOP_MARGIN + chartHeight - barH;

      g2.setColor(DtaqColors.PRIMARY);
      g2.fill(new RoundRectangle2D.Float(barX, barY, barWidth, Math.max(barH, 2), 6, 6));
      barBounds.add(new Rectangle2D.Double(barX, barY, barWidth, Math.max(barH, 2)));

      String valStr = formatValue(pt.valueMillions()) + " " + valueSuffix;
      g2.setFont(DtaqFonts.CAPTION);
      g2.setColor(DtaqColors.PRIMARY);
      g2.drawString(valStr, slotCenterX - g2.getFontMetrics().stringWidth(valStr) / 2, barY - 6);

      g2.setFont(DtaqFonts.HINT);
      g2.setColor(DtaqColors.TEXT_MUTED);
      g2.drawString(pt.label(), slotCenterX - g2.getFontMetrics().stringWidth(pt.label()) / 2, height - 12);
    }

    g2.dispose();
  }
}
