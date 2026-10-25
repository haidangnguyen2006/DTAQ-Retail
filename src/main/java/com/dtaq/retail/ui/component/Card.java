package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thẻ chứa nội dung bo góc, viền mảnh. Nền và viền do FlatLaf vẽ ({@code arc} + {@link FlatLineBorder}).
 * Dùng {@link #section} để có header chuẩn và {@link #setBody} để đổi phần thân mà không mất header.
 */
public class Card extends JPanel {

  private JComponent body;

  public Card(int padding, int gap) {
    this(padding, gap, DtaqSpacing.RADIUS_L);
  }

  public Card(int padding, int gap, int cornerRadius) {
    super(new MigLayout("fillx, insets " + padding + ", hidemode 3", "[grow]", "[]" + gap + "[]"));
    setOpaque(true);
    setBackground(DtaqColors.SURFACE);
    putClientProperty(FlatClientProperties.STYLE, "arc: " + cornerRadius * 2);
    setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), DtaqColors.BORDER, 1f, cornerRadius * 2));
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
    JPanel header = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
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
      header.add(trailing, "align right, aligny center");
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
