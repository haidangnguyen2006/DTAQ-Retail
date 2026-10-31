package com.dtaq.retail.ui.page.category;

import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.util.function.Consumer;

/**
 * Hộp thoại thêm mới hoặc cập nhật danh mục sản phẩm.
 */
public class CategoryDialog extends ModalDialog {

  public record CategoryFormData(String maDanhMuc, String tenDanhMuc, String moTa) {}

  private final FormField maField = FormField.text("Mã danh mục", "Mã tự sinh theo quy tắc hệ thống", null);
  private final FormField tenField = FormField.text("Tên danh mục", "Nhập tên danh mục sản phẩm", null);
  private final FormField moTaField = FormField.text("Mô tả", "Nhập thông tin mô tả chi tiết", null);

  private final DtaqButton luuButton = DtaqButton.primary("Lưu danh mục");
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");

  private final boolean cheDoCapNhat;
  private Consumer<CategoryFormData> onSaveListener;
  private boolean busy;

  /**
   * Khởi tạo hộp thoại ở chế độ thêm mới hoặc chỉnh sửa.
   *
   * @param parent cửa sổ chứa hộp thoại
   * @param danhMucHienTai thông tin danh mục nếu đang sửa, hoặc {@code null} nếu thêm mới
   */
  public CategoryDialog(Frame parent, DanhMucSanPham danhMucHienTai) {
    super(
        parent,
        danhMucHienTai == null ? "Thêm danh mục sản phẩm" : "Cập nhật danh mục sản phẩm",
        danhMucHienTai == null
            ? "Tạo danh mục mới để phân nhóm các dòng sản phẩm trong hệ thống."
            : "Chỉnh sửa tên và thông tin mô tả của danh mục.",
        520);

    this.cheDoCapNhat = danhMucHienTai != null;

    if (cheDoCapNhat) {
      maField.setText(danhMucHienTai.maDanhMuc());
      maField.getTextField().setEditable(false);
      maField.getTextField().setFocusable(false);
      getBody().add(maField, "growx, wrap, gapbottom 14");

      tenField.setText(danhMucHienTai.tenDanhMuc());
      if (danhMucHienTai.moTa() != null) {
        moTaField.setText(danhMucHienTai.moTa());
      }
    }

    tenField.setRequired(true);
    tenField.setHint("Tên danh mục không được để trống và không trùng lặp.");

    getBody().add(tenField, "growx, wrap, gapbottom 14");
    getBody().add(moTaField, "growx, wrap, gapbottom 18");

    if (!cheDoCapNhat) {
      getBody().add(
          new InfoCallout("Mã danh mục sẽ được hệ thống tự động sinh khi lưu thành công."),
          "growx, wrap");
    }

    tenField.getTextField().addActionListener(e -> moTaField.getTextField().requestFocusInWindow());
    moTaField.getTextField().addActionListener(e -> submit());

    huyButton.addActionListener(e -> requestClose());
    luuButton.addActionListener(e -> submit());

    setPrimaryAction(luuButton);
    setActions(huyButton, luuButton);
  }

  public void setOnSave(Consumer<CategoryFormData> onSaveListener) {
    this.onSaveListener = onSaveListener;
  }

  public void setBusy(boolean busy) {
    this.busy = busy;
    luuButton.setEnabled(!busy);
    huyButton.setEnabled(!busy);
    tenField.getTextField().setEnabled(!busy);
    moTaField.getTextField().setEnabled(!busy);
  }

  public void setErrorMessage(String error) {
    tenField.setError(error);
  }

  private void submit() {
    if (busy) {
      return;
    }
    String ten = tenField.getText().trim();
    if (ten.isEmpty()) {
      tenField.setError("Vui lòng nhập tên danh mục");
      tenField.getTextField().requestFocusInWindow();
      return;
    }
    tenField.setError(null);

    String ma = cheDoCapNhat ? maField.getText().trim() : null;
    String moTa = moTaField.getText().trim();

    if (onSaveListener != null) {
      onSaveListener.accept(new CategoryFormData(ma, ten, moTa));
    }
  }
}
