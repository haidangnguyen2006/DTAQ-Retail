package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;

/**
 * Khối trường nhập liệu chuẩn (Nhãn + Ô nhập + Gợi ý / Lỗi + Phím tắt).
 */
public class FormField extends JPanel {

  private final JLabel labelComponent = new JLabel();
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

    shortcutLabel.setFont(DtaqFonts.HINT);
    shortcutLabel.setForeground(DtaqColors.TEXT_MUTED);

    messageLabel.setFont(DtaqFonts.CAPTION);
    messageLabel.setForeground(DtaqColors.TEXT_MUTED);
    messageLabel.setVisible(false);

    input.setPreferredSize(new Dimension(input.getPreferredSize().width, DtaqSpacing.CONTROL_H));

    add(labelComponent, "align left");
    add(shortcutLabel, "align right, wrap");
    add(inputComponent, "span 2, growx, wrap");
    add(messageLabel, "span 2, growx");
  }

  public static FormField text(String label, String placeholder, String iconName) {
    JTextField field = new JTextField();
    field.setFont(DtaqFonts.BODY_14);
    field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    if (iconName != null) {
      field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon(iconName, 18));
    }
    field.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    return new FormField(label, field);
  }

  public static FormField password(String label, String placeholder) {
    JPasswordField field = new JPasswordField();
    field.setFont(DtaqFonts.BODY_14);
    field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, DtaqIcons.icon("lock-keyhole", 18));
    field.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    field.putClientProperty(FlatClientProperties.STYLE_CLASS, "showRevealButton");
    return new FormField(label, field);
  }

  public static <T> FormField combo(String label, T[] items) {
    JComboBox<T> combo = new JComboBox<>(items);
    combo.setFont(DtaqFonts.BODY_14);
    combo.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; borderColor: #E2E8EC; focusedBorderColor: #087F75");
    return new FormField(label, combo);
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

  public JTextField getTextField() {
    return (JTextField) inputComponent;
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
}
