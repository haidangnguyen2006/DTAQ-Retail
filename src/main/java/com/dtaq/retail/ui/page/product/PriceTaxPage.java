package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.GiaThueController;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.math.BigDecimal;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình cập nhật giá bán và thuế suất cho dòng sản phẩm theo UC019.
 */
public final class PriceTaxPage implements Page {

  private final GiaThueController controller;
  private final DongSanPhamDao dongSanPhamDao;
  private final JPanel view;

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã dòng", "Tên dòng", "Giá hiện tại", "Thuế hiện tại", "Trạng thái"}, 0);
  private final DataTable table = new DataTable(tableModel, 36);
  private final StatefulView statefulView;
  private final JComboBox<String> productBox = new JComboBox<>();
  private final FormField productField = new FormField("Dòng sản phẩm", productBox);
  private final FormField priceField = FormField.money("Giá bán mới", "0");
  private final FormField taxField = FormField.money("Thuế suất mới %", "0");
  private final DtaqButton saveButton = DtaqButton.primary("Lưu cập nhật", "save");

  public PriceTaxPage(GiaThueController controller, DongSanPhamDao dongSanPhamDao) {
    this.controller = Objects.requireNonNull(controller, "controller");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Cập nhật giá bán và thuế suất",
        "Chỉ quản lý được phép cập nhật giá và thuế cho giao dịch mới. Hóa đơn cũ giữ nguyên chốt cũ.");
    header.setTrailing(saveButton);
    view.add(header, "growx, wrap");

    JPanel form = new JPanel(new MigLayout("fillx, insets 0", "[grow][grow]", "[]16[]"));
    form.setOpaque(false);
    productField.setRequired(true);
    form.add(productField, "growx");
    form.add(priceField, "growx, wrap");
    form.add(taxField, "growx");

    Card formCard = Card.standard();
    formCard.setBody(form);
    view.add(formCard, "growx, wrap");

    saveButton.addActionListener(e -> luu());
    table.setCodeColumn(0);
    table.setMoneyColumn(2);
    table.setColumnWidths(160, 200, 140, 120, 140);
    statefulView = new StatefulView(table, "receipt-text", "Chưa có dữ liệu", "Chọn dòng sản phẩm để cập nhật giá và thuế.");

    Card listCard = Card.standard();
    listCard.setBody(statefulView);
    view.add(listCard, "grow, push");

    taiDanhSach();
  }

  private void taiDanhSach() {
    productBox.removeAllItems();
    tableModel.setRowCount(0);
    for (DongSanPham dong : dongSanPhamDao.findAll()) {
      productBox.addItem(dong.maDongSP());
      tableModel.addRow(new Object[] {
          dong.maDongSP(),
          dong.tenDongSP(),
          dong.donGiaBanTruocThue(),
          dong.thue(),
          dong.trangThaiKinhDoanh() ? "Đang kinh doanh" : "Ngừng kinh doanh"
      });
    }
    if (tableModel.getRowCount() == 0) {
      statefulView.showEmpty();
    } else {
      statefulView.showContent();
    }
  }

  private void luu() {
    clearErrors();

    String maDongSP = (String) productBox.getSelectedItem();
    if (maDongSP == null || maDongSP.isBlank()) {
      productField.setError("Vui lòng chọn dòng sản phẩm");
      return;
    }

    if (priceField.getText() == null || priceField.getText().isBlank()) {
      priceField.setError("Vui lòng nhập giá bán mới");
      return;
    }
    if (priceField.getMoney() <= 0) {
      priceField.setError("Giá bán mới phải lớn hơn 0");
      return;
    }

    if (taxField.getText() == null || taxField.getText().isBlank()) {
      taxField.setError("Vui lòng nhập thuế suất mới");
      return;
    }
    if (taxField.getMoney() < 0 || taxField.getMoney() > 100) {
      taxField.setError("Thuế suất mới phải nằm trong khoảng 0% đến 100%");
      return;
    }

    ConfirmDialog.confirm(
        view,
        "Xác nhận cập nhật giá và thuế",
        "Cập nhật dòng " + maDongSP + " thành giá " + priceField.getText()
            + " và thuế " + taxField.getText() + "%?",
        "Cập nhật",
        () -> thucHienCapNhat(maDongSP));
  }

  private void thucHienCapNhat(String maDongSP) {
    try {
      var ketQua = controller.capNhat(
          maDongSP,
          BigDecimal.valueOf(priceField.getMoney()),
          BigDecimal.valueOf(taxField.getMoney()));
      if (ketQua.thanhCong()) {
        Toast.success(view, ketQua.thongBao()); // bước 12; bảng tải lại hiển thị giá/thuế mới
        clearErrors();
        productBox.setSelectedIndex(-1);
        priceField.setText("");
        taxField.setText("");
        taiDanhSach();
        return;
      }
      if (ketQua.thongBao().toLowerCase().contains("giá bán")) {
        priceField.setError(ketQua.thongBao());
        return;
      }
      if (ketQua.thongBao().toLowerCase().contains("thuế")) {
        taxField.setError(ketQua.thongBao());
        return;
      }
      productField.setError(ketQua.thongBao());
    } catch (SecurityException e) {
      productField.setError(e.getMessage());
    }
  }

  private void clearErrors() {
    productField.setError(null);
    priceField.setError(null);
    taxField.setError(null);
  }

  @Override
  public PageId id() {
    return PageId.PRICE_TAX;
  }

  @Override
  public String title() {
    return "Cập nhật giá bán và thuế suất";
  }

  @Override
  public String section() {
    return "Quản lý";
  }

  @Override
  public JComponent view() {
    return view;
  }

  @Override
  public boolean scrollable() {
    return false;
  }
}
