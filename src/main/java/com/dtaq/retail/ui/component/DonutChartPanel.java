package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import net.miginfocom.swing.MigLayout;

/**
 * Biểu đồ tròn Donut tự vẽ bằng Java2D kèm chú giải tỷ trọng. Không có dữ liệu thì hiện vòng xám
 * và thông báo rỗng.
 */
public class DonutChartPanel extends JPanel {

  public record DonutSlice(String name, long amount, Color color) {}

  private static final Locale VI = Locale.of("vi", "VN");
  private static final double HOLE_RATIO = 0.72;
  private static final int CANVAS_SIZE = 240;
  private static final int CANVAS_PADDING = 10;

  private final List<DonutSlice> slices = new ArrayList<>();
  private long totalAmount;
  private String emptyText = "Chưa có dữ liệu";
  private final JPanel legendPanel = new JPanel(new MigLayout("fillx, insets 0", "[10!]10[grow]20[]20[]", "[]10[]"));
  private final Canvas canvas = new Canvas();

  public DonutChartPanel() {
    super(new MigLayout("fill, insets 0", "[" + CANVAS_SIZE + "!]32[grow]", "[center]"));
    setOpaque(false);
    legendPanel.setOpaque(false);
    add(canvas, "align center");
    add(legendPanel, "growx, aligny center");
  }

  public void setEmptyText(String emptyText) {
    this.emptyText = emptyText;
    canvas.repaint();
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
    canvas.repaint();
  }

  private void updateLegend() {
    legendPanel.removeAll();
    for (DonutSlice s : slices) {
      double percent = (totalAmount > 0) ? (s.amount() * 100.0 / totalAmount) : 0;

      Tile swatch = new Tile(s.color(), 3);
      swatch.setPreferredSize(new Dimension(10, 10));
      legendPanel.add(swatch);

      JLabel nameLbl = new JLabel(s.name());
      nameLbl.setFont(DtaqFonts.BODY_14);
      nameLbl.setForeground(DtaqColors.TEXT);
      legendPanel.add(nameLbl);

      JLabel amtLbl = new JLabel(MoneyFormat.formatVnd(s.amount()));
      amtLbl.setFont(DtaqFonts.BUTTON);
      amtLbl.setForeground(DtaqColors.TEXT);
      legendPanel.add(amtLbl, "align right");

      JLabel pctLbl = new JLabel(String.format(VI, "%.1f%%", percent));
      pctLbl.setFont(DtaqFonts.BODY_13);
      pctLbl.setForeground(DtaqColors.TEXT_MUTED);
      legendPanel.add(pctLbl, "align right, wrap");
    }
    legendPanel.revalidate();
    legendPanel.repaint();
  }

  /** Vùng vẽ vòng tròn; vòng luôn nằm giữa và vừa với cạnh nhỏ nhất. */
  private final class Canvas extends JPanel {
    Canvas() {
      setOpaque(false);
      setPreferredSize(new Dimension(CANVAS_SIZE, CANVAS_SIZE));
      ToolTipManager.sharedInstance().registerComponent(this);
    }

    private int ringSize() {
      return Math.max(80, Math.min(getWidth(), getHeight()) - 2 * CANVAS_PADDING);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
      if (totalAmount <= 0) {
        return null;
      }
      double cx = getWidth() / 2.0;
      double cy = getHeight() / 2.0;
      double dx = event.getX() - cx;
      double dy = cy - event.getY();
      double radius = ringSize() / 2.0;
      double dist = Math.hypot(dx, dy);
      if (dist > radius || dist < radius * HOLE_RATIO) {
        return null;
      }
      double angleFromTop = (90 - Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;
      double target = angleFromTop / 360.0 * totalAmount;
      double acc = 0;
      for (DonutSlice s : slices) {
        acc += s.amount();
        if (target <= acc) {
          return s.name() + ": " + MoneyFormat.formatVnd(s.amount());
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

      int size = ringSize();
      int x = (getWidth() - size) / 2;
      int y = (getHeight() - size) / 2;
      int innerSize = (int) (size * HOLE_RATIO);
      int innerX = x + (size - innerSize) / 2;
      int innerY = y + (size - innerSize) / 2;

      if (totalAmount <= 0) {
        g2.setColor(DtaqColors.DISABLED_BG);
        g2.fillOval(x, y, size, size);
        g2.setColor(DtaqColors.SURFACE);
        g2.fillOval(innerX, innerY, innerSize, innerSize);
        g2.setFont(DtaqFonts.CAPTION);
        g2.setColor(DtaqColors.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(emptyText, (getWidth() - fm.stringWidth(emptyText)) / 2, getHeight() / 2 + fm.getAscent() / 2);
        g2.dispose();
        return;
      }

      double startAngle = 90.0;
      for (DonutSlice slice : slices) {
        double extent = -((slice.amount() * 360.0) / totalAmount);
        g2.setColor(slice.color());
        g2.fill(new Arc2D.Double(x, y, size, size, startAngle, extent, Arc2D.PIE));
        startAngle += extent;
      }

      g2.setColor(DtaqColors.SURFACE);
      g2.fill(new Ellipse2D.Double(innerX, innerY, innerSize, innerSize));

      String totalStr = (totalAmount >= 1_000_000)
          ? (totalAmount / 1_000_000) + "M"
          : MoneyFormat.format(totalAmount);
      g2.setFont(DtaqFonts.DONUT_CENTER);
      g2.setColor(DtaqColors.TEXT);
      int cy = getHeight() / 2;
      g2.drawString(totalStr, (getWidth() - g2.getFontMetrics().stringWidth(totalStr)) / 2, cy);

      g2.setFont(DtaqFonts.CAPTION);
      g2.setColor(DtaqColors.TEXT_MUTED);
      String subStr = "Tổng doanh thu";
      g2.drawString(subStr, (getWidth() - g2.getFontMetrics().stringWidth(subStr)) / 2, cy + 18);

      g2.dispose();
    }
  }
}
