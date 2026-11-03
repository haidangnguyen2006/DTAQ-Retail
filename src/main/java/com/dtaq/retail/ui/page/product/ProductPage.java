package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.SanPhamController;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình quản lý máy trong kho theo UC008.
 */
public final class ProductPage implements Page {

  private final SanPhamController controller;
  private final SanPhamDao sanPhamDao;
  private final JPanel view;

  private final FormField serialField = FormField.text("Số serial", "Nhập số serial", "package");
  private final FormField supplierField = FormField.text("Mã NCC", "Nhập mã nhà cung cấp", "truck");
  private final FormField productLineField = FormField.text("Mã dòng SP", "Nhập mã dòng sản phẩm", "shopping-cart");
  private final FormField priceField = FormField.money("Giá nhập", "0");
  private final FormField dateField = FormField.text("Ngày nhập kho", LocalDate.now().toString(), "calendar");
  private final JComboBox<TrangThaiSanPham> statusBox = new JComboBox<>(TrangThaiSanPham.values());
  private final DtaqButton saveButton = DtaqButton.primary("Lưu máy", "save");

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Số serial", "Dòng SP", "NCC", "Tình trạng", "Giá nhập"}, 0);
  private final DataTable dataTable = new DataTable(tableModel, 36);
  private final StatefulView tableState;
  private final List<SanPham> cachedProducts = new ArrayList<>();

  public ProductPage(SanPhamController controller, SanPhamDao sanPhamDao) {
    this.controller = Objects.requireNonNull(controller, "controller");
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Quản lý sản phẩm",
        "Thêm máy mới vào kho và đồng bộ số lượng tồn theo từng dòng sản phẩm.");
    header.setTrailing(saveButton);
    view.add(header, "growx, wrap");

    FormField statusField = new FormField("Trạng thái", statusBox);
    statusField.setRequired(true);

    JPanel form = new JPanel(new MigLayout("fillx, insets 0", "[grow][grow]", "[]16[]16[]"));
    form.setOpaque(false);
    form.add(serialField, "growx");
    form.add(supplierField, "growx, wrap");
    form.add(productLineField, "growx");
    form.add(priceField, "growx, wrap");
    form.add(dateField, "growx");
    form.add(statusField, "growx");

    Card formCard = Card.standard();
    formCard.setBody(form);
    view.add(formCard, "growx, wrap");

    saveButton.addActionListener(e -> luuMay());
    dataTable.setCodeColumn(0);
    dataTable.setMoneyColumn(4);
    dataTable.setColumnWidths(160, 180, 120, 140, 130);
    tableState = new StatefulView(dataTable, "package", "Chưa có máy", "Bấm lưu để thêm máy mới vào kho.");

    Card tableCard = Card.standard();
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");

    taiDanhSach();
  }

  private void taiDanhSach() {
    cachedProducts.clear();
    cachedProducts.addAll(sanPhamDao.findAll());
    tableModel.setRowCount(0);
    for (SanPham sanPham : cachedProducts) {
      tableModel.addRow(new Object[] {
          sanPham.soSerial(),
          sanPham.dongSanPham().maDongSP(),
          sanPham.nhaCungCap().maNCC(),
          sanPham.trangThai().name(),
          sanPham.giaNhap()
      });
    }
    if (cachedProducts.isEmpty()) {
      tableState.showEmpty();
    } else {
      tableState.showContent();
    }
  }

  private void luuMay() {
    LocalDate ngayNhap = null;
    if (dateField.getText() != null && !dateField.getText().isBlank()) {
      ngayNhap = LocalDate.parse(dateField.getText().trim());
    }
    SanPhamController.KetQua<SanPham> ketQua = controller.themMay(
        serialField.getText(),
        supplierField.getText(),
        productLineField.getText(),
        BigDecimal.valueOf(priceField.getMoney()),
        ngayNhap,
        (TrangThaiSanPham) statusBox.getSelectedItem());
    if (ketQua.thanhCong()) {
      serialField.setText("");
      supplierField.setText("");
      productLineField.setText("");
      priceField.setText("");
      dateField.setText(LocalDate.now().toString());
      taiDanhSach();
      return;
    }
    // giữ nguyên dữ liệu đã nhập khi lỗi, như quy định của UI
    serialField.setError(ketQua.thongBao());
  }

  @Override
  public PageId id() {
    return PageId.PRODUCTS;
  }

  @Override
  public String title() {
    return "Quản lý sản phẩm";
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
