package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.util.function.Consumer;

/**
 * Hộp thoại Thêm khách hàng mới (UC007, Figma frame 1:176).
 * Kiểm tra nhanh tại chỗ để phản hồi sớm; controller vẫn là nơi kiểm tra và quyết định cuối cùng.
 */
public class AddCustomerDialog extends ModalDialog {

  public record NewCustomerForm(String hoTen, String soDienThoai, String diaChi) {}

  private static final int PHONE_DIGITS = 10;

  private final FormField hoTenField = FormField.text("Họ và tên", "Nhập họ tên đầy đủ của khách hàng", null);
  private final FormField sdtField = FormField.text("Số điện thoại", "Nhập số điện thoại (10 chữ số)", "phone");
  private final FormField diaChiField = FormField.text("Địa chỉ", "Nhập địa chỉ liên hệ", null);
  private final DtaqButton luuButton = DtaqButton.primary("Lưu khách hàng");
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");

  private Consumer<NewCustomerForm> onSaveListener;
  private boolean busy;

  public AddCustomerDialog(Frame parent) {
    super(parent, "Thêm khách hàng", "Tạo hồ sơ để sử dụng khi lập hóa đơn.", 600);

    hoTenField.setRequired(true);
    sdtField.setRequired(true);
    sdtField.setHint("Số điện thoại dùng để tra cứu; không trùng hồ sơ hiện có.");

    getBody().add(hoTenField, "growx, wrap, gapbottom 16");
    getBody().add(sdtField, "growx, wrap, gapbottom 16");
    getBody().add(diaChiField, "growx, wrap, gapbottom 20");
    getBody().add(
        new InfoCallout("Các trường có dấu * là bắt buộc. Mã khách hàng sẽ được tạo tự động khi lưu."),
        "growx, wrap");

    hoTenField.getTextField().addActionListener(e -> sdtField.getTextField().requestFocusInWindow());
    sdtField.getTextField().addActionListener(e -> diaChiField.getTextField().requestFocusInWindow());
    diaChiField.getTextField().addActionListener(e -> submit());

    huyButton.addActionListener(e -> requestClose());
    luuButton.addActionListener(e -> submit());
    setPrimaryAction(luuButton);
    setActions(huyButton, luuButton);
  }

  private void submit() {
    if (busy) {
      return;
    }
    String hoTen = hoTenField.getText().trim();
    String sdt = sdtField.getText().trim();
    hoTenField.setError(hoTen.isEmpty() ? "Vui lòng nhập họ và tên" : null);
    sdtField.setError(validatePhone(sdt));
    if (hoTen.isEmpty() || validatePhone(sdt) != null) {
      return;
    }
    if (onSaveListener != null) {
      onSaveListener.accept(new NewCustomerForm(hoTen, sdt, diaChiField.getText().trim()));
    }
  }

  private static String validatePhone(String sdt) {
    if (sdt.isEmpty()) {
      return "Vui lòng nhập số điện thoại";
    }
    if (!sdt.matches("\\d{" + PHONE_DIGITS + "}")) {
      return "Số điện thoại phải gồm " + PHONE_DIGITS + " chữ số";
    }
    return null;
  }

  public void setOnSave(Consumer<NewCustomerForm> listener) {
    this.onSaveListener = listener;
  }

  public void clearForm() {
    hoTenField.setText("");
    sdtField.setText("");
    diaChiField.setText("");
    hoTenField.setError(null);
    sdtField.setError(null);
    diaChiField.setError(null);
    setBusy(false);
  }

  public void setFieldError(String fieldName, String error) {
    if ("hoTen".equalsIgnoreCase(fieldName)) {
      hoTenField.setError(error);
    } else if ("soDienThoai".equalsIgnoreCase(fieldName)) {
      sdtField.setError(error);
    } else if ("diaChi".equalsIgnoreCase(fieldName)) {
      diaChiField.setError(error);
    }
  }

  /** Khóa form và hiện "Đang xử lý…" trong lúc lưu. */
  public void setBusy(boolean busy) {
    this.busy = busy;
    luuButton.setBusy(busy);
    huyButton.setEnabled(!busy);
  }

  /** Lưu thành công: đóng hộp thoại. */
  public void closeWithSuccess() {
    setBusy(false);
    dispose();
  }

  @Override
  protected void requestClose() {
    if (!busy) {
      dispose();
    }
  }
}
