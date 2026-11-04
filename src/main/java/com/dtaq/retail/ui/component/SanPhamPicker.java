package com.dtaq.retail.ui.component;

import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại chọn sản phẩm đang sẵn sàng để bán trong một dòng sản phẩm.
 */
public final class SanPhamPicker {

  private final SanPhamDao sanPhamDao;
  private final List<SanPham> danhSach = new ArrayList<>();

  public SanPhamPicker(SanPhamDao sanPhamDao) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
  }

  /**
   * Mở hộp thoại chọn máy đang sẵn sàng bán của một dòng sản phẩm.
   *
   * @param parent thành phần neo hộp thoại
   * @param maDongSP mã dòng sản phẩm cần lọc
   * @return máy được chọn, hoặc rỗng nếu không có máy bán được hoặc người dùng hủy
   */
  public Optional<SanPham> chon(Component parent, String maDongSP) {
    taiDanhSach(maDongSP);
    if (danhSach.isEmpty()) {
      return Optional.empty();
    }
    Frame frame = parent instanceof Frame f ? f : ConfirmDialog.frameOf(parent);
    PickerDialog dialog = new PickerDialog(frame, this);
    dialog.setVisible(true);
    return Optional.ofNullable(dialog.ketQua);
  }

  /** Nạp danh sách máy sẵn sàng bán của dòng sản phẩm; tách riêng để kiểm thử. */
  void taiDanhSach(String maDongSP) {
    danhSach.clear();
    if (maDongSP == null || maDongSP.isBlank()) {
      return;
    }
    danhSach.addAll(sanPhamDao.findBanDuoc(maDongSP));
  }

  /** Bản sao danh sách máy đã nạp. */
  List<SanPham> danhSach() {
    return List.copyOf(danhSach);
  }

  /** Trả về máy tại chỉ số model, hoặc {@code null} nếu ngoài phạm vi. */
  private SanPham mayDuocChon(int modelRow) {
    if (modelRow < 0 || modelRow >= danhSach.size()) {
      return null;
    }
    return danhSach.get(modelRow);
  }

  private static final class PickerDialog extends ModalDialog {
    private final SanPhamPicker picker;
    private final DefaultTableModel tableModel = DataTable.readOnlyModel(
        new String[] {"Số serial", "Tình trạng", "Ngày nhập kho"}, 0);
    private final DataTable dataTable = new DataTable(tableModel, 38);
    private final JLabel summaryLabel = new JLabel();
    private final DtaqButton chonButton = DtaqButton.primary("Chọn máy");
    private SanPham ketQua;

    PickerDialog(Frame parent, SanPhamPicker picker) {
      super(parent, "Chọn máy bán", "Chỉ hiển thị máy đang sẵn sàng để bán.", 520);
      this.picker = picker;

      summaryLabel.setFont(DtaqFonts.BODY_13);
      summaryLabel.setForeground(DtaqColors.TEXT_MUTED);
      getBody().add(summaryLabel, "growx, wrap, gapbottom 8");
      getBody().add(dataTable, "growx, push, h 220!");

      dataTable.setColumnWidths(180, 180, 120);
      dataTable.setOnRowDoubleClick(this::xacNhan);

      DtaqButton huyButton = DtaqButton.secondary("Hủy");
      huyButton.addActionListener(e -> requestClose());
      chonButton.setEnabled(false);
      chonButton.addActionListener(e -> {
        int row = dataTable.getTable().getSelectedRow();
        if (row >= 0) {
          xacNhan(dataTable.getTable().convertRowIndexToModel(row));
        }
      });
      setActions(huyButton, chonButton);

      taiDanhSach();
    }

    private void taiDanhSach() {
      tableModel.setRowCount(0);
      List<SanPham> rows = picker.danhSach();
      if (rows.isEmpty()) {
        summaryLabel.setText("Không có máy nào đang sẵn sàng để bán");
        return;
      }
      summaryLabel.setText("Tìm thấy " + rows.size() + " máy sẵn sàng");
      for (SanPham sp : rows) {
        tableModel.addRow(new Object[] {
            sp.soSerial(),
            sp.trangThai() == null ? "" : sp.trangThai().name(),
            sp.ngayNhapKhoBanDau() == null ? "" : sp.ngayNhapKhoBanDau().toString()
        });
      }
      chonButton.setEnabled(true);
      dataTable.getTable().setRowSelectionInterval(0, 0);
    }

    private void xacNhan(int modelRow) {
      SanPham duocChon = picker.mayDuocChon(modelRow);
      if (duocChon == null) {
        return;
      }
      ketQua = duocChon;
      dispose();
    }
  }
}
