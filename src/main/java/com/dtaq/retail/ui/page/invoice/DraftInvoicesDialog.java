package com.dtaq.retail.ui.page.invoice;

import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.dao.HoaDonTamDao.HoaDonTam;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

/**
 * Hộp thoại chọn một hóa đơn tạm để mở lại (UC003).
 *
 * <p>Hộp thoại chỉ hiển thị danh sách do bộ điều khiển bán hàng cung cấp và trả về mã hóa đơn tạm
 * người dùng chọn; việc khôi phục giỏ hàng do bên gọi thực hiện. Xóa một bản tạm đi qua callback
 * {@code onXoa} để không gọi trực tiếp lớp DAO.
 */
public final class DraftInvoicesDialog extends ModalDialog {

  private static final String[] COT =
      {"Mã tạm", "Khách hàng", "SĐT", "SL", "Lưu lúc", ""};
  private static final int COT_THAO_TAC = 5;

  private final Frame parent;
  private final List<HoaDonTam> danhSach;
  private final Consumer<String> onXoa;
  private final DefaultTableModel tableModel;
  private final DataTable table;

  private String maDaChon;

  /**
   * Khởi tạo hộp thoại danh sách hóa đơn tạm.
   *
   * @param parent cửa sổ cha; có thể {@code null}
   * @param danhSach danh sách hóa đơn tạm cần hiển thị; không được {@code null}
   * @param onXoa xử lý xóa một hóa đơn tạm theo mã; không được {@code null}
   */
  public DraftInvoicesDialog(Frame parent, List<HoaDonTam> danhSach, Consumer<String> onXoa) {
    super(parent, "Hóa đơn tạm", "Chọn một bản tạm để tiếp tục lập hóa đơn", 720);
    this.parent = parent;
    this.danhSach = new ArrayList<>(Objects.requireNonNull(danhSach, "danhSach"));
    this.onXoa = Objects.requireNonNull(onXoa, "onXoa");

    DtaqButton moButton = DtaqButton.primary("Mở");
    DtaqButton dongButton = DtaqButton.secondary("Đóng");
    dongButton.addActionListener(e -> dispose());

    if (this.danhSach.isEmpty()) {
      tableModel = null;
      table = null;
      JLabel trong = new JLabel("Chưa có hóa đơn tạm nào");
      trong.setFont(DtaqFonts.BODY_14);
      trong.setForeground(DtaqColors.TEXT_MUTED);
      getBody().add(trong, "growx");
      moButton.setEnabled(false);
    } else {
      tableModel = DataTable.readOnlyModel(COT, COT_THAO_TAC);
      table = new DataTable(tableModel, 52);
      table.setCodeColumn(0);
      table.setNumberColumn(3);
      table.setColumnWidths(150, 220, 120, 80, 150, 90);
      table.setVisibleRows(3, 8);
      table.setActionColumn(COT_THAO_TAC, RowAction.danger("Xóa", this::xoaDong));
      table.setOnRowDoubleClick(this::chonDong);
      getBody().add(table, "growx");
      napDuLieu();
    }

    moButton.addActionListener(e -> moDongDangChon());
    setActions(moButton, dongButton);
    setPrimaryAction(moButton);
  }

  /**
   * Hiển thị hộp thoại và chờ người dùng chọn.
   *
   * <p>Phải gọi trên EDT.
   *
   * @return mã hóa đơn tạm được chọn, hoặc rỗng nếu người dùng đóng hộp thoại
   */
  public Optional<String> moBanTam() {
    setVisible(true);
    return Optional.ofNullable(maDaChon);
  }

  private void napDuLieu() {
    if (tableModel == null) {
      return;
    }
    tableModel.setRowCount(0);
    for (HoaDonTam banTam : danhSach) {
      String tenKhachHang =
          banTam.tenKhachHang() == null || banTam.tenKhachHang().isBlank()
              ? "Khách lẻ" : banTam.tenKhachHang();
      String sdt = banTam.sdtKhachHang() == null ? "" : banTam.sdtKhachHang();
      int soLuong = banTam.dongHang() == null ? 0
          : banTam.dongHang().stream()
              .mapToInt(HoaDonTamDao.DongTam::soLuong)
              .sum();
      tableModel.addRow(new Object[] {
          banTam.maTam(), tenKhachHang, sdt, soLuong,
          DateFormats.formatDateTime(banTam.luuLuc()), ""});
    }
  }

  private void moDongDangChon() {
    if (table == null) {
      return;
    }
    int dong = table.getTable().getSelectedRow();
    if (dong < 0) {
      return;
    }
    chonDong(table.getTable().convertRowIndexToModel(dong));
  }

  private void chonDong(int dongModel) {
    if (dongModel < 0 || dongModel >= danhSach.size()) {
      return;
    }
    maDaChon = danhSach.get(dongModel).maTam();
    dispose();
  }

  private void xoaDong(int dongModel) {
    if (dongModel < 0 || dongModel >= danhSach.size()) {
      return;
    }
    HoaDonTam banTam = danhSach.get(dongModel);
    String maTam = banTam.maTam();
    ConfirmDialog.confirm(parent, "Xóa hóa đơn tạm", "Xóa bản tạm " + maTam + "?", "Xóa", () -> {
      onXoa.accept(maTam);
      danhSach.remove(banTam);
      napDuLieu();
    });
  }
}
