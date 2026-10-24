package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại chọn kỳ báo cáo theo khoảng ngày (UC017: kỳ báo cáo có thể là một khoảng thời gian).
 * Chỉ kiểm tra thứ tự ngày; việc kiểm tra kỳ báo cáo hợp lệ cuối cùng thuộc controller.
 */
final class PeriodDialog extends ModalDialog {

  private static final String DATE_PATTERN = "dd/MM/yyyy";

  private final JSpinner fromSpinner;
  private final JSpinner toSpinner;
  private final FormField fromField;
  private final FormField toField;
  private final BiConsumer<LocalDate, LocalDate> onConfirm;

  PeriodDialog(Frame parent, LocalDate from, LocalDate to, BiConsumer<LocalDate, LocalDate> onConfirm) {
    super(parent, "Chọn kỳ báo cáo", "Chọn khoảng ngày cần thống kê.", 480);
    this.onConfirm = onConfirm;

    fromSpinner = newDateSpinner(from);
    toSpinner = newDateSpinner(to);
    fromField = new FormField("Từ ngày", fromSpinner);
    toField = new FormField("Đến ngày", toSpinner);

    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]16[grow,fill]", "[]"));
    row.setOpaque(false);
    row.add(fromField);
    row.add(toField);
    getBody().add(row, "growx");

    DtaqButton cancel = DtaqButton.secondary("Hủy");
    cancel.addActionListener(e -> dispose());
    DtaqButton apply = DtaqButton.primary("Áp dụng");
    apply.addActionListener(e -> submit());
    setPrimaryAction(apply);
    setActions(cancel, apply);
  }

  private static JSpinner newDateSpinner(LocalDate value) {
    Date date = Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    JSpinner spinner = new JSpinner(new SpinnerDateModel(date, null, null, java.util.Calendar.DAY_OF_MONTH));
    spinner.setEditor(new JSpinner.DateEditor(spinner, DATE_PATTERN));
    return spinner;
  }

  private static LocalDate toLocalDate(JSpinner spinner) {
    return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private void submit() {
    LocalDate from = toLocalDate(fromSpinner);
    LocalDate to = toLocalDate(toSpinner);
    if (from.isAfter(to)) {
      toField.setError("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
      return;
    }
    dispose();
    onConfirm.accept(from, to);
  }
}
