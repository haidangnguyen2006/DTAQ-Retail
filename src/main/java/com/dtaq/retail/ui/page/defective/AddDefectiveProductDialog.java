package com.dtaq.retail.ui.page.defective;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

/**
 * Hộp thoại tiếp nhận và ghi nhận hồ sơ sản phẩm lỗi.
 */
public class AddDefectiveProductDialog extends ModalDialog {

  public record NewDefectiveForm(
      String soSerial,
      String moTaLoi,
      LocalDate ngayPhatHien,
      String ghiChuXuLy,
      String maChungTuLienKet,
      boolean laPhieuTra
  ) {}

  private final FormField serialField = FormField.text("Số Serial máy", "Nhập chính xác số serial của máy lỗi", "hash");
  private final FormField moTaField = FormField.text("Mô tả lỗi phát hiện", "Mô tả triệu chứng hư hỏng", null);
  private final FormField ngayPhatHienField = FormField.text("Ngày phát hiện (YYYY-MM-DD)", "Mặc định là ngày hôm nay", "calendar");
  private final FormField ghiChuField = FormField.text("Ghi chú xử lý ban đầu", "Thông tin tiếp nhận thêm", null);
  private final FormField chungTuField = FormField.text("Mã chứng từ đổi/trả (nếu có)", "VD: PT-2026-000001 hoặc PD-2026-000001", null);

  private final DtaqButton luuButton = DtaqButton.primary("Ghi nhận lỗi");
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");

  private Consumer<NewDefectiveForm> onSaveListener;
  private boolean busy;

  public AddDefectiveProductDialog(Frame parent) {
    super(
        parent,
        "Ghi nhận sản phẩm lỗi",
        "Tiếp nhận máy lỗi để chuyển sang quy trình kiểm định, bảo hành hoặc đổi trả.",
        580);

    serialField.setRequired(true);
    moTaField.setRequired(true);
    ngayPhatHienField.setText(LocalDate.now().toString());

    getBody().add(serialField, "growx, wrap, gapbottom 12");
    getBody().add(moTaField, "growx, wrap, gapbottom 12");
    getBody().add(ngayPhatHienField, "growx, wrap, gapbottom 12");
    getBody().add(ghiChuField, "growx, wrap, gapbottom 12");
    getBody().add(chungTuField, "growx, wrap, gapbottom 16");

    getBody().add(
        new InfoCallout("Mã lỗi dạng SPL-YYYY-NNNNNN sẽ được tự động cấp. Máy sẽ được chuyển sang trạng thái Hàng lỗi."),
        "growx, wrap");

    huyButton.addActionListener(e -> requestClose());
    luuButton.addActionListener(e -> submit());

    setPrimaryAction(luuButton);
    setActions(huyButton, luuButton);
  }

  public void setOnSave(Consumer<NewDefectiveForm> onSaveListener) {
    this.onSaveListener = onSaveListener;
  }

  public void setBusy(boolean busy) {
    this.busy = busy;
    luuButton.setEnabled(!busy);
    huyButton.setEnabled(!busy);
    serialField.getTextField().setEnabled(!busy);
    moTaField.getTextField().setEnabled(!busy);
  }

  public void setErrorMessage(String error) {
    serialField.setError(error);
  }

  private void submit() {
    if (busy) {
      return;
    }

    String serial = serialField.getText().trim();
    String moTa = moTaField.getText().trim();
    String ngayStr = ngayPhatHienField.getText().trim();
    String ghiChu = ghiChuField.getText().trim();
    String chungTu = chungTuField.getText().trim();

    serialField.setError(null);
    moTaField.setError(null);
    ngayPhatHienField.setError(null);

    boolean coLoi = false;
    if (serial.isEmpty()) {
      serialField.setError("Vui lòng nhập số serial của máy");
      coLoi = true;
    }
    if (moTa.isEmpty()) {
      moTaField.setError("Vui lòng nhập mô tả lỗi");
      coLoi = true;
    }

    LocalDate ngayPhatHien = LocalDate.now();
    if (!ngayStr.isEmpty()) {
      try {
        ngayPhatHien = LocalDate.parse(ngayStr);
        if (ngayPhatHien.isAfter(LocalDate.now())) {
          ngayPhatHienField.setError("Ngày phát hiện không được sau ngày hiện tại");
          coLoi = true;
        }
      } catch (DateTimeParseException e) {
        ngayPhatHienField.setError("Định dạng ngày không hợp lệ (YYYY-MM-DD)");
        coLoi = true;
      }
    }

    if (coLoi) {
      return;
    }

    boolean laPhieuTra = chungTu.toUpperCase().startsWith("PT");
    if (onSaveListener != null) {
      onSaveListener.accept(new NewDefectiveForm(
          serial,
          moTa,
          ngayPhatHien,
          ghiChu.isEmpty() ? null : ghiChu,
          chungTu.isEmpty() ? null : chungTu,
          laPhieuTra));
    }
  }
}
