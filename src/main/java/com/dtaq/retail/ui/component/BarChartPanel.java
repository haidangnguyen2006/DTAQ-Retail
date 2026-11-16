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
  private float reveal = 1f;
  private com.formdev.flatlaf.util.Animator revealAnimator;
  private String unitLabel = "Triệu ₫";
  private String valueSuffix = "tr";
  private String emptyText = "Chưa có dữ liệu thống kê";

  public BarChartPanel() {
    setOpaque(false);
    setPreferredSize(new Dimension(320, 260));
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
    startReveal();
  }

  /** Cột mọc dần từ đáy khi có dữ liệu mới (≈280ms); bỏ qua nếu hoạt ảnh bị tắt hoặc chưa hiển thị. */
  private void startReveal() {
    if (revealAnimator != null) {
      revealAnimator.cancel();
    }
    if (points.isEmpty() || !com.formdev.flatlaf.util.Animator.useAnimation() || !isShowing()) {
      reveal = 1f;
      repaint();
      return;
    }
    reveal = 0f;
    revealAnimator = new com.formdev.flatlaf.util.Animator(180, fraction -> {
      reveal = fraction;
      repaint();
    });
    revealAnimator.setInterpolator(com.formdev.flatlaf.util.CubicBezierEasing.EASE_OUT);
    revealAnimator.start();
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
    g2.setColor(DtaqColors.CHART_AXIS);
    g2.drawString(unitLabel, 16, 20);

    int chartHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
    int chartWidth = width - LEFT_MARGIN - RIGHT_MARGIN;

    // Trục tung + đường lưới
    for (int i = 0; i <= AXIS_STEPS; i++) {
      double stepVal = (axisMax / AXIS_STEPS) * i;
      int y = TOP_MARGIN + chartHeight - (int) ((stepVal / axisMax) * chartHeight);
      g2.setColor(DtaqColors.CHART_GRID);
      g2.drawLine(LEFT_MARGIN, y, LEFT_MARGIN + chartWidth, y);
      g2.setFont(DtaqFonts.HINT);
      g2.setColor(DtaqColors.CHART_AXIS);
      String label = formatValue(stepVal);
      g2.drawString(label, LEFT_MARGIN - g2.getFontMetrics().stringWidth(label) - 12, y + 4);
    }

    int n = points.size();
    int slotWidth = chartWidth / n;
    int barWidth = Math.min(56, (int) (slotWidth * 0.55));

    // Nhãn trục X cách quãng khi khe hẹp hơn nhãn dài nhất; tooltip vẫn đủ từng cột.
    int widest = 0;
    for (BarPoint p : points) {
      widest = Math.max(widest, g2.getFontMetrics(DtaqFonts.HINT).stringWidth(p.label()));
    }
    int labelStep = Math.max(1, (int) Math.ceil((widest + 8.0) / Math.max(1, slotWidth)));

    for (int i = 0; i < n; i++) {
      BarPoint pt = points.get(i);
      int slotCenterX = LEFT_MARGIN + i * slotWidth + slotWidth / 2;
      int barX = slotCenterX - barWidth / 2;
      int barH = (int) ((pt.valueMillions() / axisMax) * chartHeight * reveal);
      int barY = TOP_MARGIN + chartHeight - barH;

      int drawH = Math.max(barH, 2);
      // Ngày giá trị 0 không vẽ vạch ở chân (tránh hàng vạch nhiễu); vẫn giữ vùng để tooltip đúng cột.
      if (pt.valueMillions() > 0) {
        g2.setColor(DtaqColors.CHART_1);
        // Đầu cột bo 4px, chân cột vuông để cột "đứng" trên trục.
        g2.fill(new RoundRectangle2D.Float(barX, barY, barWidth, drawH, 8, 8));
        int flat = drawH / 2;
        g2.fillRect(barX, barY + drawH - flat, barWidth, flat);
      }
      barBounds.add(new Rectangle2D.Double(barX, barY, barWidth, drawH));

      // Cột 0 không ghi nhãn giá trị để biểu đồ nhiều ngày không bị rối.
      if (pt.valueMillions() > 0) {
        String valStr = formatValue(pt.valueMillions()) + " " + valueSuffix;
        g2.setFont(DtaqFonts.NUMERIC_12);
        g2.setColor(DtaqColors.alpha(DtaqColors.TEXT, reveal));
        g2.drawString(valStr, slotCenterX - g2.getFontMetrics().stringWidth(valStr) / 2, barY - 6);
      }

      g2.setFont(DtaqFonts.HINT);
      g2.setColor(DtaqColors.TEXT_MUTED);
      if (i % labelStep == 0) {
        g2.drawString(pt.label(), slotCenterX - g2.getFontMetrics().stringWidth(pt.label()) / 2, height - 12);
      }
    }

    g2.dispose();
  }
}
