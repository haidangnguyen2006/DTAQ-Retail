package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.util.function.Consumer;

/**
 * Hộp thoại Thêm khách hàng mới (AddCustomerDialog) theo Figma frame 1:176.
 */
public class AddCustomerDialog extends ModalDialog {

  public record NewCustomerForm(String hoTen, String soDienThoai, String diaChi) {}

  private final FormField hoTenField = FormField.text("Họ và tên *", "Nhập họ tên đầy đủ của khách hàng", null);
  private final FormField sdtField = FormField.text("Số điện thoại *", "Nhập số điện thoại (10 chữ số)", "phone");
  private final FormField diaChiField = FormField.text("Địa chỉ", "Nhập địa chỉ liên hệ", null);
  private final DtaqButton luuButton = DtaqButton.primary("Lưu khách hàng");
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");

  private Consumer<NewCustomerForm> onSaveListener;

  public AddCustomerDialog(Frame parent) {
    super(parent, "Thêm khách hàng", "Tạo hồ sơ để sử dụng khi lập hóa đơn.", 600);

    sdtField.setHint("Số điện thoại dùng để tra cứu; không trùng hồ sơ hiện có.");

    getBody().add(hoTenField, "growx, wrap, gapbottom 16");
    getBody().add(sdtField, "growx, wrap, gapbottom 16");
    getBody().add(diaChiField, "growx, wrap, gapbottom 20");

    InfoCallout callout = new InfoCallout("Các trường có dấu * là bắt buộc. Mã khách hàng sẽ được tạo tự động khi lưu.");
    getBody().add(callout, "growx, wrap");

    huyButton.addActionListener(e -> dispose());
    luuButton.addActionListener(e -> {
      if (onSaveListener != null) {
        onSaveListener.accept(new NewCustomerForm(
            hoTenField.getText().trim(),
            sdtField.getText().trim(),
            diaChiField.getText().trim()
        ));
      }
    });

    setActions(huyButton, luuButton);
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

  public void setBusy(boolean busy) {
    luuButton.setEnabled(!busy);
    huyButton.setEnabled(!busy);
  }
}
