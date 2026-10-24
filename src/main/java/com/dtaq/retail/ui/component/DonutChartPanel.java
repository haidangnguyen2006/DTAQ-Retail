package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Biểu đồ tròn Donut tự vẽ bằng Java2D (DonutChartPanel) kèm chú giải tỷ trọng các dòng sản phẩm.
 */
public class DonutChartPanel extends JPanel {

  public record DonutSlice(String name, long amount, Color color) {}

  private final List<DonutSlice> slices = new ArrayList<>();
  private long totalAmount = 0;
  private final JPanel legendPanel = new JPanel(new MigLayout("fillx, insets 0", "[10!]10[grow]20[]20[]", "[]8[]"));

  public DonutChartPanel() {
    super(new MigLayout("fill, insets 0", "[240!]24[grow]", "[center]"));
    setOpaque(false);

    JPanel chartCanvas = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawDonut((Graphics2D) g);
      }
    };
    chartCanvas.setOpaque(false);
    chartCanvas.setPreferredSize(new Dimension(240, 240));

    legendPanel.setOpaque(false);

    add(chartCanvas, "align center");
    add(legendPanel, "growx, aligny center");
  }

  public void setData(List<DonutSlice> newSlices) {
    slices.clear();
    totalAmount = 0;
    if (newSlices != null) {
      slices.addAll(newSlices);
      for (DonutSlice s : slices) {
        totalAmount += s.amount();
      }
    }
    updateLegend();
    repaint();
  }

  private void updateLegend() {
    legendPanel.removeAll();
    for (DonutSlice s : slices) {
      double percent = (totalAmount > 0) ? (s.amount() * 100.0 / totalAmount) : 0;

      // Color swatch
      JPanel swatch = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
          Graphics2D g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g2.setColor(s.color());
          g2.fill(new RoundRectangle2D.Float(0, 2, 10, 10, 2, 2));
          g2.dispose();
        }
      };
      swatch.setOpaque(false);
      swatch.setPreferredSize(new Dimension(10, 14));
      legendPanel.add(swatch);

      // Name
      JLabel nameLbl = new JLabel(s.name());
      nameLbl.setFont(DtaqFonts.BODY_14);
      nameLbl.setForeground(DtaqColors.TEXT);
      legendPanel.add(nameLbl);

      // Amount
      JLabel amtLbl = new JLabel(MoneyFormat.formatVnd(s.amount()));
      amtLbl.setFont(DtaqFonts.BUTTON);
      amtLbl.setForeground(DtaqColors.TEXT);
      legendPanel.add(amtLbl, "align right");

      // Percent
      JLabel pctLbl = new JLabel(String.format("%.1f%%", percent));
      pctLbl.setFont(DtaqFonts.BODY_13);
      pctLbl.setForeground(DtaqColors.TEXT_MUTED);
      legendPanel.add(pctLbl, "align right, wrap");
    }
    legendPanel.revalidate();
    legendPanel.repaint();
  }

  private void drawDonut(Graphics2D g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    int size = 220;
    int x = (240 - size) / 2;
    int y = (240 - size) / 2;

    if (totalAmount <= 0) {
      g2.setColor(DtaqColors.DISABLED_BG);
      g2.fillOval(x, y, size, size);
      int innerSize = (int) (size * 0.72);
      int innerX = x + (size - innerSize) / 2;
      int innerY = y + (size - innerSize) / 2;
      g2.setColor(DtaqColors.SURFACE);
      g2.fillOval(innerX, innerY, innerSize, innerSize);
      g2.dispose();
      return;
    }

    double startAngle = 90.0; // Start at 12 o'clock
    for (DonutSlice slice : slices) {
      double extent = -((slice.amount() * 360.0) / totalAmount);
      g2.setColor(slice.color());
      g2.fill(new Arc2D.Double(x, y, size, size, startAngle, extent, Arc2D.PIE));
      startAngle += extent;
    }

    // Cut out inner hole
    int innerSize = (int) (size * 0.72);
    int innerX = x + (size - innerSize) / 2;
    int innerY = y + (size - innerSize) / 2;
    g2.setColor(DtaqColors.SURFACE);
    g2.fill(new Ellipse2D.Double(innerX, innerY, innerSize, innerSize));

    // Center text
    String totalStr = (totalAmount >= 1_000_000)
        ? (totalAmount / 1_000_000) + "M"
        : MoneyFormat.format(totalAmount);
    g2.setFont(DtaqFonts.DONUT_CENTER);
    g2.setColor(DtaqColors.TEXT);
    int textW = g2.getFontMetrics().stringWidth(totalStr);
    g2.drawString(totalStr, x + (size - textW) / 2, y + size / 2);

    g2.setFont(DtaqFonts.CAPTION);
    g2.setColor(DtaqColors.TEXT_MUTED);
    String subStr = "Tổng doanh thu";
    int subW = g2.getFontMetrics().stringWidth(subStr);
    g2.drawString(subStr, x + (size - subW) / 2, y + size / 2 + 18);

    g2.dispose();
  }
}
