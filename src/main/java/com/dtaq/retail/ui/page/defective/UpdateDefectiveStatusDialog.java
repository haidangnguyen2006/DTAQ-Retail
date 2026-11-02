package com.dtaq.retail.ui.page.defective;

import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.util.function.BiConsumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại cập nhật tình trạng xử lý của hồ sơ sản phẩm lỗi.
 */
public class UpdateDefectiveStatusDialog extends ModalDialog {

  private final JComboBox<StatusItem> statusCombo = new JComboBox<>();
  private final FormField ghiChuField = FormField.text("Ghi chú xử lý mới", "Nội dung biên bản bàn giao hoặc kết quả xử lý", null);

  private final DtaqButton capNhatButton = DtaqButton.primary("Cập nhật trạng thái");
  private final DtaqButton huyButton = DtaqButton.secondary("Đóng");

  private BiConsumer<TrangThaiXuLyLoi, String> onUpdateListener;
  private boolean busy;

  public record StatusItem(TrangThaiXuLyLoi status, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  public UpdateDefectiveStatusDialog(Frame parent, SanPhamLoi spl) {
    super(
        parent,
        "Chuyển trạng thái xử lý lỗi",
        "Hồ sơ: " + spl.maLoi() + " — Máy: " + spl.sanPham().soSerial(),
        520);

    statusCombo.addItem(new StatusItem(TrangThaiXuLyLoi.CHO_XU_LY, "Chờ xử lý"));
    statusCombo.addItem(new StatusItem(TrangThaiXuLyLoi.DA_GUI_BAO_HANH, "Đã gửi bảo hành"));
    statusCombo.addItem(new StatusItem(TrangThaiXuLyLoi.DA_TRA_NHA_CUNG_CAP, "Đã trả nhà cung cấp"));
    statusCombo.addItem(new StatusItem(TrangThaiXuLyLoi.THANH_LY, "Thanh lý"));

    for (int i = 0; i < statusCombo.getItemCount(); i++) {
      if (statusCombo.getItemAt(i).status() == spl.tinhTrangXuLy()) {
        statusCombo.setSelectedIndex(i);
        break;
      }
    }

    JPanel comboPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]4[]"));
    comboPanel.setOpaque(false);
    JLabel lbl = new JLabel("Trạng thái xử lý mới *");
    comboPanel.add(lbl, "wrap");
    comboPanel.add(statusCombo, "growx");

    getBody().add(comboPanel, "growx, wrap, gapbottom 16");
    getBody().add(ghiChuField, "growx, wrap, gapbottom 16");

    huyButton.addActionListener(e -> requestClose());
    capNhatButton.addActionListener(e -> submit());

    setPrimaryAction(capNhatButton);
    setActions(huyButton, capNhatButton);
  }

  public void setOnUpdate(BiConsumer<TrangThaiXuLyLoi, String> onUpdateListener) {
    this.onUpdateListener = onUpdateListener;
  }

  public void setBusy(boolean busy) {
    this.busy = busy;
    capNhatButton.setEnabled(!busy);
    huyButton.setEnabled(!busy);
    statusCombo.setEnabled(!busy);
    ghiChuField.getTextField().setEnabled(!busy);
  }

  private void submit() {
    if (busy) {
      return;
    }
    StatusItem selected = (StatusItem) statusCombo.getSelectedItem();
    if (selected == null) {
      return;
    }
    String ghiChu = ghiChuField.getText().trim();
    if (onUpdateListener != null) {
      onUpdateListener.accept(selected.status(), ghiChu);
    }
  }
}
