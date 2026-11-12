package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.ui.component.DatePicker;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại chọn kỳ báo cáo theo khoảng ngày (UC017: kỳ báo cáo có thể là một khoảng thời gian).
 * Kiểm tra định dạng và thứ tự ngày; việc kiểm tra kỳ báo cáo hợp lệ cuối cùng thuộc controller.
 */
final class PeriodDialog extends ModalDialog {

  private static final String LOI_NGAY_NGAN = "Ngày sai (dd/MM/yyyy)";

  private final DatePicker fromPicker = new DatePicker("Từ ngày", LOI_NGAY_NGAN);
  private final DatePicker toPicker = new DatePicker("Đến ngày", LOI_NGAY_NGAN);
  private final BiConsumer<LocalDate, LocalDate> onConfirm;

  PeriodDialog(Frame parent, LocalDate from, LocalDate to, BiConsumer<LocalDate, LocalDate> onConfirm) {
    super(parent, "Chọn kỳ báo cáo", "Chọn khoảng ngày cần thống kê.", 480);
    this.onConfirm = onConfirm;

    fromPicker.setDate(from);
    toPicker.setDate(to);

    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]16[grow,fill]", "[]"));
    row.setOpaque(false);
    row.add(fromPicker.field());
    row.add(toPicker.field());
    getBody().add(row, "growx");

    DtaqButton cancel = DtaqButton.secondary("Hủy");
    cancel.addActionListener(e -> dispose());
    DtaqButton apply = DtaqButton.primary("Áp dụng");
    apply.addActionListener(e -> submit());
    setPrimaryAction(apply);
    setActions(cancel, apply);
  }

  private void submit() {
    fromPicker.setError(null);
    toPicker.setError(null);
    LocalDate from = docNgay(fromPicker, "Chọn ngày bắt đầu");
    LocalDate to = docNgay(toPicker, "Chọn ngày kết thúc");
    if (from == null || to == null) {
      return;
    }
    if (from.isAfter(to)) {
      toPicker.setError("Trước ngày bắt đầu");
      return;
    }
    dispose();
    onConfirm.accept(from, to);
  }

  /** Đọc ngày và hiện lỗi ngay dưới ô nếu giá trị không hợp lệ. */
  private static LocalDate docNgay(DatePicker picker, String loiTrong) {
    if (picker.isBlank()) {
      picker.setError(loiTrong);
      return null;
    }
    return picker.kiemTraDinhDang() ? picker.getDate() : null;
  }
}
