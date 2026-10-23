package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Biểu đồ cột tự vẽ bằng Java2D (BarChartPanel) phục vụ thống kê doanh thu theo ngày/kỳ.
 */
public class BarChartPanel extends JPanel {

  public record BarPoint(String label, double valueMillions) {}

  private final List<BarPoint> points = new ArrayList<>();
  private double maxScale = 80.0;

  public BarChartPanel() {
    setOpaque(false);
    setPreferredSize(new Dimension(800, 250));
  }

  public void setData(List<BarPoint> newPoints) {
    points.clear();
    if (newPoints != null) {
      points.addAll(newPoints);
    }
    double maxVal = 10.0;
    for (BarPoint p : points) {
      if (p.valueMillions() > maxVal) {
        maxVal = p.valueMillions();
      }
    }
    // Round maxScale up to next multiple of 20
    maxScale = Math.ceil(maxVal / 20.0) * 20.0;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    int width = getWidth();
    int height = getHeight();

    // Unit label at top-left
    g2.setFont(DtaqFonts.HINT);
    g2.setColor(DtaqColors.TEXT_SUBTLE);
    g2.drawString("Triệu ₫", 16, 20);

    int leftMargin = 56;
    int bottomMargin = 36;
    int topMargin = 30;
    int chartHeight = height - topMargin - bottomMargin;
    int chartWidth = width - leftMargin - 24;

    // Y Axis labels (5 steps)
    int steps = 4;
    for (int i = 0; i <= steps; i++) {
      double stepVal = (maxScale / steps) * i;
      int y = topMargin + chartHeight - (int) ((stepVal / maxScale) * chartHeight);
      String label = String.valueOf((int) stepVal);
      int labelW = g2.getFontMetrics().stringWidth(label);
      g2.drawString(label, leftMargin - labelW - 12, y + 4);
    }

    if (points.isEmpty()) {
      g2.setFont(DtaqFonts.BODY_13);
      g2.setColor(DtaqColors.TEXT_MUTED);
      g2.drawString("Chưa có dữ liệu thống kê", leftMargin + 20, topMargin + chartHeight / 2);
      g2.dispose();
      return;
    }

    // Draw bars
    int n = points.size();
    int slotWidth = chartWidth / n;
    int barWidth = Math.min(72, (int) (slotWidth * 0.55));

    for (int i = 0; i < n; i++) {
      BarPoint pt = points.get(i);
      int slotCenterX = leftMargin + i * slotWidth + slotWidth / 2;
      int barX = slotCenterX - barWidth / 2;

      int barH = (int) ((pt.valueMillions() / maxScale) * chartHeight);
      int barY = topMargin + chartHeight - barH;

      // Bar
      g2.setColor(DtaqColors.PRIMARY);
      g2.fill(new RoundRectangle2D.Float(barX, barY, barWidth, barH, 6, 6));

      // Value label on top
      String valStr = ((int) pt.valueMillions()) + " tr";
      g2.setFont(DtaqFonts.CAPTION);
      int valW = g2.getFontMetrics().stringWidth(valStr);
      g2.drawString(valStr, slotCenterX - valW / 2, barY - 6);

      // X Axis label
      g2.setFont(DtaqFonts.HINT);
      g2.setColor(DtaqColors.TEXT_MUTED);
      int lblW = g2.getFontMetrics().stringWidth(pt.label());
      g2.drawString(pt.label(), slotCenterX - lblW / 2, height - 12);
    }

    g2.dispose();
  }
}
