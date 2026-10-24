package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.ui.theme.DtaqStyles;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import net.miginfocom.swing.MigLayout;

/**
 * Khối trường nhập liệu chuẩn (Nhãn [*] + Ô nhập + Gợi ý / Lỗi + Phím tắt).
 */
public class FormField extends JPanel {

  private static final int LEADING_ICON_PADDING = 10;

  private final JLabel labelComponent = new JLabel();
  private final JLabel requiredMark = new JLabel("*");
  private final JLabel shortcutLabel = new JLabel();
  private final JComponent inputComponent;
  private final JLabel messageLabel = new JLabel();

  public FormField(String labelText, JComponent input) {
    super(new MigLayout("fillx, insets 0", "[grow][]", "[]" + DtaqSpacing.FIELD_LABEL_GAP + "[]4[]"));
    setOpaque(false);
    this.inputComponent = input;

    labelComponent.setText(labelText);
    labelComponent.setFont(DtaqFonts.LABEL);
    labelComponent.setForeground(DtaqColors.TEXT);

    requiredMark.setFont(DtaqFonts.LABEL);
    requiredMark.setForeground(DtaqColors.DANGER);
    requiredMark.setVisible(false);

    shortcutLabel.setFont(DtaqFonts.HINT);
    shortcutLabel.setForeground(DtaqColors.TEXT_MUTED);

    messageLabel.setFont(DtaqFonts.CAPTION);
    messageLabel.setForeground(DtaqColors.TEXT_MUTED);
    messageLabel.setVisible(false);

    if (input.getClientProperty(FlatClientProperties.STYLE) == null) {
      input.putClientProperty(FlatClientProperties.STYLE, DtaqStyles.TEXT_FIELD);
    }
    input.setPreferredSize(new Dimension(input.getPreferredSize().width, DtaqSpacing.CONTROL_H));

    JPanel labelRow = new JPanel(new MigLayout("insets 0, gapx 3", "[][]", "[]"));
    labelRow.setOpaque(false);
    labelRow.add(labelComponent);
    labelRow.add(requiredMark);

    add(labelRow, "align left");
    add(shortcutLabel, "align right, wrap");
    add(inputComponent, "span 2, growx, wrap");
    add(messageLabel, "span 2, growx");
  }

  private static javax.swing.Icon leadingIcon(String iconName) {
    return DtaqIcons.paddedLeft(DtaqIcons.icon(iconName, 18), LEADING_ICON_PADDING);
  }

  public static FormField text(String label, String placeholder, String iconName) {
    JTextField field = new JTextField();
    field.setFont(DtaqFonts.BODY_14);
    field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    if (iconName != null) {
      field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, leadingIcon(iconName));
    }
    return new FormField(label, field);
  }

  /** Ô tìm kiếm: icon kính lúp và nút xóa nhanh khi có nội dung. */
  public static FormField search(String label, String placeholder) {
    FormField field = text(label, placeholder, "search");
    field.getTextField().putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    return field;
  }

  public static FormField password(String label, String placeholder) {
    JPasswordField field = new JPasswordField();
    field.setFont(DtaqFonts.BODY_14);
    field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, leadingIcon("lock-keyhole"));
    field.putClientProperty(FlatClientProperties.STYLE_CLASS, "showRevealButton");
    return new FormField(label, field);
  }

  public static <T> FormField combo(String label, T[] items) {
    JComboBox<T> combo = new JComboBox<>(items);
    combo.setFont(DtaqFonts.BODY_14);
    return new FormField(label, combo);
  }

  /**
   * Ô nhập số tiền (VNĐ): tự chèn dấu chấm phân cách hàng nghìn khi gõ, căn phải,
   * chọn toàn bộ khi nhận focus. Đọc giá trị bằng {@link #getMoney()}.
   */
  public static FormField money(String label, String placeholder) {
    JTextField field = new JTextField();
    field.setFont(DtaqFonts.BODY_14);
    field.setHorizontalAlignment(SwingConstants.RIGHT);
    field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    field.putClientProperty(
        FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY,
        FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY_ALWAYS);
    ((AbstractDocument) field.getDocument()).setDocumentFilter(new MoneyDocumentFilter());
    return new FormField(label, field);
  }

  public void setRequired(boolean required) {
    requiredMark.setVisible(required);
  }

  public void setShortcut(String shortcutText) {
    shortcutLabel.setText(shortcutText);
  }

  public void setHint(String hintText) {
    messageLabel.setText(hintText);
    messageLabel.setForeground(DtaqColors.TEXT_MUTED);
    messageLabel.setVisible(hintText != null && !hintText.trim().isEmpty());
    revalidate();
    repaint();
  }

  public void setError(String errorText) {
    messageLabel.setText(errorText);
    messageLabel.setForeground(DtaqColors.DANGER);
    messageLabel.setVisible(errorText != null && !errorText.trim().isEmpty());
    if (errorText != null && !errorText.trim().isEmpty()) {
      inputComponent.putClientProperty(FlatClientProperties.OUTLINE, "error");
    } else {
      inputComponent.putClientProperty(FlatClientProperties.OUTLINE, null);
    }
    revalidate();
    repaint();
  }

  public JComponent getInputComponent() {
    return inputComponent;
  }

  /** Ô văn bản bên trong; ném {@link IllegalStateException} nếu trường này không phải ô văn bản. */
  public JTextField getTextField() {
    if (inputComponent instanceof JTextField textField) {
      return textField;
    }
    throw new IllegalStateException("Trường này không phải ô nhập văn bản");
  }

  public String getText() {
    if (inputComponent instanceof JTextField tf) {
      return tf.getText();
    }
    return "";
  }

  public void setText(String text) {
    if (inputComponent instanceof JTextField tf) {
      tf.setText(text);
    }
  }

  /** Giá trị số tiền người dùng đã nhập vào ô {@link #money}; 0 nếu để trống. */
  public long getMoney() {
    return MoneyFormat.parseDigits(getText());
  }

  /** Bộ lọc tài liệu: chỉ giữ chữ số và định dạng lại với dấu chấm phân cách hàng nghìn. */
  private static final class MoneyDocumentFilter extends DocumentFilter {

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
        throws BadLocationException {
      replace(fb, offset, 0, string, attr);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
      replace(fb, offset, length, "", null);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
        throws BadLocationException {
      String current = fb.getDocument().getText(0, fb.getDocument().getLength());
      String next = current.substring(0, offset) + (text == null ? "" : text)
          + current.substring(offset + length);
      long value = MoneyFormat.parseDigits(next);
      boolean hasDigits = next.chars().anyMatch(Character::isDigit);
      String formatted = hasDigits ? MoneyFormat.format(value) : "";
      super.replace(fb, 0, fb.getDocument().getLength(), formatted, attrs);
    }
  }
}
