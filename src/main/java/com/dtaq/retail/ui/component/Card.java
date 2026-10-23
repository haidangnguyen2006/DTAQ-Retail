package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thẻ chứa nội dung bo tròn góc (Card / RoundedPanel) với viền thanh mảnh theo Figma.
 */
public class Card extends JPanel {

  private final int cornerRadius;

  public Card(int padding, int gap) {
    this(padding, gap, DtaqSpacing.RADIUS_L);
  }

  public Card(int padding, int gap, int cornerRadius) {
    super(new MigLayout("fillx, insets " + padding, "[grow]", "[]" + gap + "[]"));
    this.cornerRadius = cornerRadius;
    setOpaque(false);
  }

  public static Card standard() {
    return new Card(DtaqSpacing.CARD_PADDING, DtaqSpacing.CARD_GAP);
  }

  public static Card compact() {
    return new Card(DtaqSpacing.CARD_PADDING_COMPACT, DtaqSpacing.CARD_GAP_COMPACT);
  }

  public static Card titled(String titleText, String trailingText) {
    Card card = standard();
    JPanel header = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    header.setOpaque(false);

    JLabel title = new JLabel(titleText);
    title.setFont(DtaqFonts.CARD_TITLE);
    title.setForeground(DtaqColors.TEXT);
    header.add(title, "align left");

    if (trailingText != null && !trailingText.trim().isEmpty()) {
      JLabel trailing = new JLabel(trailingText);
      trailing.setFont(DtaqFonts.CAPTION);
      trailing.setForeground(DtaqColors.TEXT_MUTED);
      header.add(trailing, "align right");
    }

    card.add(header, "growx, wrap");
    return card;
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth() - 1;
    int h = getHeight() - 1;

    // Fill background
    g2.setColor(DtaqColors.SURFACE);
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

    // Draw border
    g2.setColor(DtaqColors.BORDER);
    g2.draw(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

    g2.dispose();
    super.paintComponent(g);
  }
}
