package com.dtaq.retail.ui.page.home;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Hộp thoại quyết định phê duyệt/từ chối phiếu đổi trả (UC018): phê duyệt nhập ý kiến,
 * từ chối bắt buộc nhập lý do. Việc ghi quyết định do controller thực hiện.
 */
final class ApprovalDecisionDialog extends ModalDialog {

  private static final int TEXT_HEIGHT = 96;

  private final boolean approve;
  private final String maPhieu;
  private final BiConsumer<String, String> onConfirm;
  private final JTextArea textArea = new JTextArea();
  private final FormField field;

  ApprovalDecisionDialog(Frame parent, boolean approve, String maPhieu, BiConsumer<String, String> onConfirm) {
    super(parent,
        (approve ? "Phê duyệt phiếu " : "Từ chối phiếu ") + maPhieu,
        approve ? "Kiểm tra lý do, điều kiện xử lý và số tiền hoàn trước khi quyết định."
            : "Quyết định từ chối sẽ được lưu cùng thông tin người duyệt.",
        560);
    this.approve = approve;
    this.maPhieu = maPhieu;
    this.onConfirm = onConfirm;

    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setFont(DtaqFonts.BODY_14);
    textArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

    JScrollPane scroll = new JScrollPane(textArea);
    scroll.setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), DtaqColors.BORDER, 1f, 12));
    field = new FormField(approve ? "Ý kiến phê duyệt" : "Lý do từ chối", scroll);
    scroll.setPreferredSize(new Dimension(0, TEXT_HEIGHT));
    field.setRequired(!approve);
    getBody().add(field, "growx, wrap, gapbottom 16");
    getBody().add(new InfoCallout(
        approve ? "Phê duyệt chỉ lưu quyết định; chưa tự thực hiện hoàn tiền hoặc cập nhật tồn kho."
            : "Phiếu bị từ chối sẽ không được hoàn tất giao dịch."), "growx");

    DtaqButton cancel = DtaqButton.secondary("Hủy");
    cancel.addActionListener(e -> dispose());
    DtaqButton confirm = approve ? DtaqButton.primary("Phê duyệt") : DtaqButton.dangerSoft("Từ chối");
    confirm.addActionListener(e -> submit());
    setActions(cancel, confirm);
  }

  private void submit() {
    String text = textArea.getText().trim();
    if (!approve && text.isEmpty()) {
      field.setError("Vui lòng nhập lý do từ chối");
      return;
    }
    dispose();
    onConfirm.accept(maPhieu, text);
  }
}
