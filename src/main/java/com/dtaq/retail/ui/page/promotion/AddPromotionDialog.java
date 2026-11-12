package com.dtaq.retail.ui.page.promotion;

import com.dtaq.retail.dao.KhuyenMaiDao.DongApDung;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import java.awt.Frame;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

/** Hộp thoại tạo chương trình khuyến mãi mới. */
public final class AddPromotionDialog extends ModalDialog {

  public record PromotionFormData(
      String ten,
      String moTa,
      BigDecimal tyLeGiam,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      String maDongSP,
      List<String> maDanhMuc,
      Integer soNgayTonToiThieu,
      boolean dangApDung) {}

  private final FormField tenField = FormField.text("Tên chương trình", "Nhập tên khuyến mãi", null);
  private final FormField moTaField = FormField.text("Mô tả", "Mô tả ưu đãi (không bắt buộc)", null);
  private final FormField tyLeField = FormField.text("Tỷ lệ giảm (%)", "Từ 0 đến 100", null);
  private final FormField ngayBatDauField = FormField.text("Ngày bắt đầu (YYYY-MM-DD)", "2026-11-15", "calendar");
  private final FormField ngayKetThucField = FormField.text("Ngày kết thúc (YYYY-MM-DD)", "2026-11-30", "calendar");
  private final JComboBox<DongSanPham> dongSanPhamCombo = new JComboBox<>();
  private final FormField dongSanPhamField = new FormField("Dòng sản phẩm áp dụng", dongSanPhamCombo);
  private final JList<DanhMucSanPham> danhMucList = new JList<>();
  private final FormField ngayTonField = FormField.text("Tồn từ N ngày", "Không bắt buộc", null);
  private final JLabel previewSummary = new JLabel();
  private final DefaultTableModel previewModel = DataTable.readOnlyModel(
      new String[] {"Mã dòng", "Tên dòng", "Danh mục", "Tồn", "Máy đủ điều kiện"});
  private final DtaqButton previewButton = DtaqButton.secondary("Xem trước");
  private final JCheckBox dangApDungCheck = new JCheckBox("Bật áp dụng ngay sau khi lưu");
  private final DtaqButton saveButton = DtaqButton.primary("Lưu khuyến mãi");
  private final DtaqButton cancelButton = DtaqButton.secondary("Hủy");

  private BiConsumer<PromotionFormData, Boolean> onSubmit;
  private boolean busy;

  public AddPromotionDialog(Frame parent, List<DongSanPham> dongSanPham, List<DanhMucSanPham> danhMuc) {
    super(parent, "Thêm khuyến mãi", "Chọn dòng sản phẩm hoặc lọc theo danh mục và tuổi tồn.", 740);
    dongSanPhamCombo.setRenderer(new DefaultListCellRenderer() {
      @Override
      public java.awt.Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        setText(value instanceof DongSanPham dsp
            ? dsp.tenDongSP() + " (" + dsp.maDongSP() + ")"
            : "Không chọn dòng cụ thể");
        return this;
      }
    });
    dongSanPhamCombo.addItem(null);
    dongSanPham.forEach(dongSanPhamCombo::addItem);
    dongSanPhamCombo.setSelectedIndex(0);
    danhMucList.setListData(danhMuc.toArray(DanhMucSanPham[]::new));
    danhMucList.setVisibleRowCount(3);
    danhMucList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public java.awt.Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        if (value instanceof DanhMucSanPham dm) setText(dm.tenDanhMuc());
        return this;
      }
    });
    dongSanPhamCombo.addActionListener(e -> {
      boolean chonDong = dongSanPhamCombo.getSelectedItem() != null;
      if (chonDong) danhMucList.clearSelection();
      danhMucList.setEnabled(!busy && !chonDong);
    });

    tenField.setRequired(true);
    tyLeField.setRequired(true);
    ngayBatDauField.setRequired(true);
    ngayKetThucField.setRequired(true);
    ngayBatDauField.setText(LocalDate.now().toString());
    ngayKetThucField.setText(LocalDate.now().plusDays(30).toString());

    getBody().add(tenField, "growx, wrap, gapbottom 12");
    getBody().add(moTaField, "growx, wrap, gapbottom 12");
    getBody().add(tyLeField, "growx, wrap, gapbottom 12");
    getBody().add(ngayBatDauField, "growx, wrap, gapbottom 12");
    getBody().add(ngayKetThucField, "growx, wrap, gapbottom 12");
    getBody().add(dongSanPhamField, "growx, wrap, gapbottom 12");
    getBody().add(new FormField("Danh mục (Ctrl/Shift để chọn nhiều)",
        new JScrollPane(danhMucList)), "growx, wrap, gapbottom 12");
    getBody().add(ngayTonField, "growx, wrap, gapbottom 12");
    getBody().add(dangApDungCheck, "growx, wrap, gapbottom 12");
    getBody().add(new InfoCallout("Mỗi dòng sản phẩm chỉ có một chương trình đang áp dụng trong cùng khoảng ngày."), "growx, wrap");
    getBody().add(previewSummary, "growx, wrap");
    getBody().add(new DataTable(previewModel, 36), "growx, h 112!, wrap");

    cancelButton.addActionListener(e -> requestClose());
    previewButton.addActionListener(e -> submit(false));
    saveButton.addActionListener(e -> submit(true));
    setPrimaryAction(saveButton);
    setActions(cancelButton, previewButton, saveButton);
  }

  public void setOnSubmit(BiConsumer<PromotionFormData, Boolean> listener) {
    onSubmit = listener;
  }

  public void showPreview(List<DongApDung> cacDong) {
    previewModel.setRowCount(0);
    previewSummary.setText(cacDong.size() + " dòng sản phẩm sẽ được thêm vào chương trình");
    for (DongApDung dongApDung : cacDong) {
      DongSanPham dong = dongApDung.dong();
      previewModel.addRow(new Object[] {dong.maDongSP(), dong.tenDongSP(),
          dong.danhMucSanPham() == null ? "" : dong.danhMucSanPham().tenDanhMuc(),
          dong.soLuongTon(), dongApDung.soMayDuDieuKien()});
    }
  }

  public void setBusy(boolean busy) {
    this.busy = busy;
    saveButton.setEnabled(!busy);
    previewButton.setEnabled(!busy);
    cancelButton.setEnabled(!busy);
    tenField.getTextField().setEnabled(!busy);
    moTaField.getTextField().setEnabled(!busy);
    tyLeField.getTextField().setEnabled(!busy);
    ngayBatDauField.getTextField().setEnabled(!busy);
    ngayKetThucField.getTextField().setEnabled(!busy);
    dongSanPhamCombo.setEnabled(!busy);
    danhMucList.setEnabled(!busy && dongSanPhamCombo.getSelectedItem() == null);
    ngayTonField.getTextField().setEnabled(!busy);
    dangApDungCheck.setEnabled(!busy);
  }

  public void setErrorMessage(String error) {
    tenField.setError(error);
  }

  @Override
  protected void requestClose() {
    if (!busy) super.requestClose();
  }

  private void submit(boolean save) {
    if (busy) return;
    setErrorMessage(null);
    ngayTonField.setError(null);
    Integer soNgayTon;
    try {
      String value = ngayTonField.getText().trim();
      soNgayTon = value.isEmpty() ? null : Integer.valueOf(value);
      if (soNgayTon != null && soNgayTon < 0) throw new NumberFormatException();
    } catch (NumberFormatException e) {
      ngayTonField.setError("Nhập số ngày tồn là số nguyên không âm");
      return;
    }
    DongSanPham dong = (DongSanPham) dongSanPhamCombo.getSelectedItem();
    String maDong = dong == null ? null : dong.maDongSP();
    List<String> maDanhMuc = danhMucList.getSelectedValuesList().stream()
        .map(DanhMucSanPham::maDanhMuc).toList();
    if (!save) {
      if (onSubmit != null) onSubmit.accept(new PromotionFormData(
          null, null, null, null, null, maDong, maDanhMuc, soNgayTon, false), false);
      return;
    }
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
    if (onSubmit != null) {
      onSubmit.accept(new PromotionFormData(ten, moTaField.getText().trim(), tyLe,
          ngayBatDau, ngayKetThuc, maDong, maDanhMuc, soNgayTon, dangApDungCheck.isSelected()), true);
    }
  }
}
