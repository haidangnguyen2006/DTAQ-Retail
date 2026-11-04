package com.dtaq.retail.ui.page.promotion;

import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JList;

/** Hộp thoại tạo chương trình khuyến mãi mới. */
public final class AddPromotionDialog extends ModalDialog {

  public record PromotionFormData(
      String ten,
      String moTa,
      BigDecimal tyLeGiam,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      String maDongSP,
      boolean dangApDung) {}

  private final FormField tenField = FormField.text("Tên chương trình", "Nhập tên khuyến mãi", null);
  private final FormField moTaField = FormField.text("Mô tả", "Mô tả ưu đãi (không bắt buộc)", null);
  private final FormField tyLeField = FormField.text("Tỷ lệ giảm (%)", "Từ 0 đến 100", null);
  private final FormField ngayBatDauField = FormField.text("Ngày bắt đầu (YYYY-MM-DD)", "2026-11-15", "calendar");
  private final FormField ngayKetThucField = FormField.text("Ngày kết thúc (YYYY-MM-DD)", "2026-11-30", "calendar");
  private final JComboBox<DongSanPham> dongSanPhamCombo = new JComboBox<>();
  private final FormField dongSanPhamField = new FormField("Dòng sản phẩm áp dụng", dongSanPhamCombo);
  private final JCheckBox dangApDungCheck = new JCheckBox("Bật áp dụng ngay sau khi lưu");
  private final DtaqButton saveButton = DtaqButton.primary("Lưu khuyến mãi");
  private final DtaqButton cancelButton = DtaqButton.secondary("Hủy");

  private Consumer<PromotionFormData> onSaveListener;
  private boolean busy;

  public AddPromotionDialog(Frame parent, List<DongSanPham> dongSanPham) {
    super(parent, "Thêm khuyến mãi", "Tạo chương trình giảm giá cho một dòng sản phẩm.", 560);
    dongSanPhamCombo.setRenderer(new DefaultListCellRenderer() {
      @Override
      public java.awt.Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        setText(value instanceof DongSanPham dsp
            ? dsp.tenDongSP() + " (" + dsp.maDongSP() + ")"
            : "Chọn dòng sản phẩm");
        return this;
      }
    });
    if (dongSanPham != null) {
      dongSanPham.forEach(dongSanPhamCombo::addItem);
    }

    tenField.setRequired(true);
    tyLeField.setRequired(true);
    ngayBatDauField.setRequired(true);
    ngayKetThucField.setRequired(true);
    dongSanPhamField.setRequired(true);
    ngayBatDauField.setText(LocalDate.now().toString());
    ngayKetThucField.setText(LocalDate.now().plusDays(30).toString());

    getBody().add(tenField, "growx, wrap, gapbottom 12");
    getBody().add(moTaField, "growx, wrap, gapbottom 12");
    getBody().add(tyLeField, "growx, wrap, gapbottom 12");
    getBody().add(ngayBatDauField, "growx, wrap, gapbottom 12");
    getBody().add(ngayKetThucField, "growx, wrap, gapbottom 12");
    getBody().add(dongSanPhamField, "growx, wrap, gapbottom 12");
    getBody().add(dangApDungCheck, "growx, wrap, gapbottom 12");
    getBody().add(new InfoCallout("Mỗi dòng sản phẩm chỉ có một chương trình đang áp dụng trong cùng khoảng ngày."), "growx, wrap");

    cancelButton.addActionListener(e -> requestClose());
    saveButton.addActionListener(e -> submit());
    setPrimaryAction(saveButton);
    setActions(cancelButton, saveButton);
  }

  public void setOnSave(Consumer<PromotionFormData> listener) {
    onSaveListener = listener;
  }

  public void setBusy(boolean busy) {
    this.busy = busy;
    saveButton.setEnabled(!busy);
    cancelButton.setEnabled(!busy);
    tenField.getTextField().setEnabled(!busy);
    moTaField.getTextField().setEnabled(!busy);
    tyLeField.getTextField().setEnabled(!busy);
    ngayBatDauField.getTextField().setEnabled(!busy);
    ngayKetThucField.getTextField().setEnabled(!busy);
    dongSanPhamCombo.setEnabled(!busy);
    dangApDungCheck.setEnabled(!busy);
  }

  public void setErrorMessage(String error) {
    tenField.setError(error);
  }

  private void submit() {
    if (busy) return;
    String ten = tenField.getText().trim();
    if (ten.isEmpty()) {
      tenField.setError("Vui lòng nhập tên chương trình");
      tenField.getTextField().requestFocusInWindow();
      return;
    }
    tenField.setError(null);

    BigDecimal tyLe;
    try {
      tyLe = new BigDecimal(tyLeField.getText().trim().replace(',', '.'));
    } catch (NumberFormatException e) {
      tyLeField.setError("Tỷ lệ giảm phải là một số từ 0 đến 100");
      return;
    }
    LocalDate ngayBatDau;
    LocalDate ngayKetThuc;
    try {
      ngayBatDau = LocalDate.parse(ngayBatDauField.getText().trim());
      ngayKetThuc = LocalDate.parse(ngayKetThucField.getText().trim());
    } catch (DateTimeParseException e) {
      ngayBatDauField.setError("Ngày phải theo định dạng YYYY-MM-DD");
      return;
    }
    DongSanPham dong = (DongSanPham) dongSanPhamCombo.getSelectedItem();
    if (dong == null) {
      dongSanPhamField.setError("Vui lòng chọn dòng sản phẩm");
      return;
    }
    if (tyLe.compareTo(BigDecimal.ZERO) < 0 || tyLe.compareTo(new BigDecimal("100")) > 0) {
      tyLeField.setError("Tỷ lệ giảm phải nằm trong khoảng từ 0 đến 100");
      return;
    }
    tyLeField.setError(null);
    ngayBatDauField.setError(null);
    dongSanPhamField.setError(null);
    if (ngayBatDau.isAfter(ngayKetThuc)) {
      ngayKetThucField.setError("Ngày kết thúc không được trước ngày bắt đầu");
      return;
    }
    ngayKetThucField.setError(null);
    if (onSaveListener != null) {
      onSaveListener.accept(new PromotionFormData(ten, moTaField.getText().trim(), tyLe,
          ngayBatDau, ngayKetThuc, dong.maDongSP(), dangApDungCheck.isSelected()));
    }
  }
}
