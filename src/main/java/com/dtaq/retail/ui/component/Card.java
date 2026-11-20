package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thẻ chứa nội dung bo góc, viền mảnh và bóng đổ mềm (tự vẽ trong {@link #paintComponent}).
 * Dùng {@link #section} để có header chuẩn và {@link #setBody} để đổi phần thân mà không mất header.
 */
public class Card extends JPanel {

  private JComponent body;

  @Override
  public java.awt.Dimension getMinimumSize() {
    return new java.awt.Dimension(0, super.getMinimumSize().height);
  }

  public Card(int padding, int gap) {
    this(padding, gap, DtaqSpacing.RADIUS_L);
  }

  /** Chiều cao bóng đổ dưới thẻ (px logic); phần này nằm trong viền nên không lấn sang thẻ bên cạnh. */
  private static final int SHADOW = 3;
  private static final java.awt.Color SHADOW_TINT = new java.awt.Color(0x0B2447);

  private final int cornerRadius;

  public Card(int padding, int gap, int cornerRadius) {
    // gapy thay cho "[]gap[]": hàng thứ hai rỗng không còn chừa khoảng cách thừa ở đáy thẻ một hàng.
    super(new MigLayout("fillx, insets " + padding + ", gapy " + gap + ", hidemode 3", "[0:pref,grow]", "[]"));
    this.cornerRadius = cornerRadius;
    // Thẻ tự vẽ nền bo góc, viền mảnh và bóng mềm nhiều lớp để tách khỏi nền trang mà không nặng nề.
    setOpaque(false);
    setBackground(DtaqColors.SURFACE);
    setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1 + SHADOW, 1));
  }

  @Override
  protected void paintComponent(java.awt.Graphics g) {
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);
    float arc = com.formdev.flatlaf.util.UIScale.scale(cornerRadius * 2f);
    float shadow = com.formdev.flatlaf.util.UIScale.scale((float) SHADOW);
    float w = getWidth();
    float h = getHeight() - shadow;
    // Bóng: lớp rộng mờ dần + lớp sát mép đậm hơn chút, lệch xuống dưới.
    g2.setColor(DtaqColors.alpha(SHADOW_TINT, 0.035f));
    g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, shadow, w, h, arc, arc));
    g2.setColor(DtaqColors.alpha(SHADOW_TINT, 0.05f));
    g2.fill(new java.awt.geom.RoundRectangle2D.Float(0.5f, shadow * 0.6f, w - 1, h, arc, arc));
    g2.setColor(getBackground());
    g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, arc, arc));
    g2.setColor(DtaqColors.BORDER);
    g2.setStroke(new java.awt.BasicStroke(1f));
    g2.draw(new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc - 1, arc - 1));
    g2.dispose();
  }

  public static Card standard() {
    return new Card(DtaqSpacing.CARD_PADDING, DtaqSpacing.CARD_GAP);
  }

  public static Card compact() {
    return new Card(DtaqSpacing.CARD_PADDING_COMPACT, DtaqSpacing.CARD_GAP_COMPACT);
  }

  /** Thẻ có header gồm tiêu đề và một dòng chữ phụ bên phải (có thể rỗng). */
  public static Card titled(String titleText, String trailingText) {
    JLabel trailing = null;
    if (trailingText != null && !trailingText.trim().isEmpty()) {
      trailing = new JLabel(trailingText);
      trailing.setFont(DtaqFonts.CAPTION);
      trailing.setForeground(DtaqColors.TEXT_MUTED);
    }
    return section(titleText, null, trailing);
  }

  /** Thẻ có header: tiêu đề, mô tả ngắn (tuỳ chọn) và thành phần bên phải (tuỳ chọn). */
  public static Card section(String titleText, String subtitleText, JComponent trailing) {
    Card card = standard();
    card.add(buildHeader(titleText, subtitleText, trailing), "growx, wrap");
    return card;
  }

  /** Header chuẩn của một thẻ/khối nội dung. */
  public static JPanel buildHeader(String titleText, String subtitleText, JComponent trailing) {
    // Phần bên phải được co (chữ rút gọn "…") trước khi đẩy tiêu đề hay tràn khỏi thẻ.
    JPanel header = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[0:pref]", "[]"));
    header.setOpaque(false);

    JPanel titleBlock = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    titleBlock.setOpaque(false);
    JLabel title = new JLabel(titleText);
    title.setFont(DtaqFonts.CARD_TITLE);
    title.setForeground(DtaqColors.TEXT);
    titleBlock.add(title, "wrap");
    if (subtitleText != null && !subtitleText.trim().isEmpty()) {
      JLabel subtitle = new JLabel(subtitleText);
      subtitle.setFont(DtaqFonts.BODY_13);
      subtitle.setForeground(DtaqColors.TEXT_MUTED);
      titleBlock.add(subtitle);
    }
    header.add(titleBlock, "align left, aligny center");
    if (trailing != null) {
      header.add(trailing, "align right, aligny center, wmin 0");
    }
    return header;
  }

  /** Thay phần thân của thẻ (thành phần được thêm sau header). */
  public void setBody(JComponent newBody) {
    if (body != null) {
      remove(body);
    }
    body = newBody;
    add(newBody, "grow, push, wrap");
    revalidate();
    repaint();
  }
}
