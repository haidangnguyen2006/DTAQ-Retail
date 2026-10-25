package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Hộp thoại xác nhận / thông báo theo phong cách {@link ModalDialog}, thay cho {@code JOptionPane}.
 */
public final class ConfirmDialog extends ModalDialog {

  private ConfirmDialog(
      Frame parent, String title, String message, String confirmText,
      boolean showCancel, Runnable onConfirm) {
    super(parent, title, null, 440);

    JTextArea text = new JTextArea(message);
    text.setEditable(false);
    text.setFocusable(false);
    text.setOpaque(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setFont(DtaqFonts.BODY_14);
    text.setForeground(DtaqColors.TEXT);
    text.setBorder(null);
    getBody().add(text, "growx");

    DtaqButton confirm = DtaqButton.primary(confirmText);
    confirm.addActionListener(e -> {
      dispose();
      if (onConfirm != null) {
        onConfirm.run();
      }
    });
    setPrimaryAction(confirm);

    if (showCancel) {
      DtaqButton cancel = DtaqButton.secondary("Hủy");
      cancel.addActionListener(e -> dispose());
      setActions(cancel, confirm);
    } else {
      setActions(confirm);
    }
  }

  /** Hỏi xác nhận; {@code onConfirm} chỉ chạy khi người dùng bấm nút xác nhận. */
  public static void confirm(
      Component anchor, String title, String message, String confirmText, Runnable onConfirm) {
    new ConfirmDialog(frameOf(anchor), title, message, confirmText, true, onConfirm).setVisible(true);
  }

  /** Thông báo một nút "Đóng"; {@code onClose} chạy sau khi hộp thoại đóng. */
  public static void info(Component anchor, String title, String message, Runnable onClose) {
    new ConfirmDialog(frameOf(anchor), title, message, "Đóng", false, onClose).setVisible(true);
  }

  /** Tìm cửa sổ {@link Frame} gốc chứa {@code anchor}. */
  public static Frame frameOf(Component anchor) {
    Window window = anchor == null ? null : SwingUtilities.getWindowAncestor(anchor);
    while (window != null && !(window instanceof Frame)) {
      window = window.getOwner();
    }
    return (Frame) window;
  }
}
