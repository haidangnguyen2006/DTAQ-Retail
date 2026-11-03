package com.dtaq.retail.ui.component;

import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Component;
import java.awt.Frame;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại chọn sản phẩm đang sẵn sàng để bán trong một dòng sản phẩm.
 */
public final class SanPhamPicker {

  private final SanPhamDao sanPhamDao;

  public SanPhamPicker(SanPhamDao sanPhamDao) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
  }

  public Optional<SanPham> chon(Component parent, String maDongSP) {
    Frame frame = parent instanceof Frame f ? f : ConfirmDialog.frameOf(parent);
    PickerDialog dialog = new PickerDialog(frame, sanPhamDao, maDongSP);
    dialog.setVisible(true);
    return Optional.ofNullable(dialog.ketQua);
  }

  private static final class PickerDialog extends ModalDialog {
    private final SanPhamDao sanPhamDao;
    private final DefaultTableModel tableModel = DataTable.readOnlyModel(
        new String[] {"Số serial", "Tình trạng", "Ngày nhập kho"}, 0);
    private final DataTable dataTable = new DataTable(tableModel, 38);
    private final JLabel summaryLabel = new JLabel();
    private final DtaqButton chonButton = DtaqButton.primary("Chọn máy");
    private SanPham ketQua;

    PickerDialog(Frame parent, SanPhamDao sanPhamDao, String maDongSP) {
      super(parent, "Chọn máy bán", "Chỉ hiển thị máy đang sẵn sàng để bán.", 520);
      this.sanPhamDao = sanPhamDao;

      summaryLabel.setFont(DtaqFonts.BODY_13);
      summaryLabel.setForeground(DtaqColors.TEXT_MUTED);
      summaryLabel.setText("Đang tải danh sách máy...");
      getBody().add(summaryLabel, "growx, wrap, gapbottom 8");
      getBody().add(dataTable, "growx, push, h 220!");

      dataTable.setColumnWidths(180, 180, 120);
      dataTable.setOnRowDoubleClick(row -> xacNhan(row));

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

      SwingUtilities.invokeLater(() -> taiDanhSach(maDongSP));
    }

    private void taiDanhSach(String maDongSP) {
      tableModel.setRowCount(0);
      if (maDongSP == null || maDongSP.isBlank()) {
        summaryLabel.setText("Chưa chọn dòng sản phẩm để lọc");
        return;
      }
      var danhSach = sanPhamDao.findBanDuoc(maDongSP);
      if (danhSach.isEmpty()) {
        summaryLabel.setText("Không có máy nào đang sẵn sàng để bán");
        return;
      }
      summaryLabel.setText("Tìm thấy " + danhSach.size() + " máy sẵn sàng");
      for (SanPham sp : danhSach) {
        tableModel.addRow(new Object[] {
            sp.soSerial(),
            sp.trangThai() == null ? "" : sp.trangThai().name(),
            sp.ngayNhapKhoBanDau() == null ? "" : sp.ngayNhapKhoBanDau().toString()
        });
      }
      if (!danhSach.isEmpty()) {
        chonButton.setEnabled(true);
        dataTable.getTable().setRowSelectionInterval(0, 0);
      }
    }

    private void xacNhan(int row) {
      if (row < 0) {
        return;
      }
      var selected = sanPhamDao.findBanDuoc(dataTable.getTable().getValueAt(0, 0).toString());
      if (selected.isEmpty()) {
        return;
      }
      ketQua = selected.get(0);
      dispose();
    }

    private void xacNhan(Object rowIndex) {
      if (!(rowIndex instanceof Integer index)) {
        return;
      }
      var rows = sanPhamDao.findByTrangThai(TrangThaiSanPham.SAN_SANG_BAN);
      if (index >= 0 && index < rows.size()) {
        ketQua = rows.get(index);
        dispose();
      }
    }
  }
}
