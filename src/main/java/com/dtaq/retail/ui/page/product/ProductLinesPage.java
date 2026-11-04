package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.DongSanPhamController;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
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
import java.util.List;
import java.util.Objects;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình quản lý dòng sản phẩm theo UC009.
 */
public final class ProductLinesPage implements Page {

  private final DongSanPhamController controller;
  private final DanhMucSanPhamDao danhMucSanPhamDao;
  private final JPanel view;
  
  private final FormField tenField = FormField.text("Tên dòng SP", "Nhập tên dòng sản phẩm", "shopping-cart");
  private final JComboBox<DanhMucSanPham> categoryBox = new JComboBox<>();
  private final FormField categoryField = new FormField("Danh mục", categoryBox);
  private final FormField unitField = FormField.text("Đơn vị tính", "Ví dụ: Chiếc / Cái", "package");
  private final FormField priceField = FormField.money("Giá bán trước thuế", "0");
  private final FormField taxField = FormField.money("Thuế suất %", "0");
  private final DtaqButton saveButton = DtaqButton.primary("Lưu", "save");

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã dòng", "Tên dòng", "Danh mục", "ĐVT", "Giá", "Thuế", "Kinh doanh"}, 0);
  private final DataTable table = new DataTable(tableModel, 36);
  private final StatefulView statefulView;

  public ProductLinesPage(DongSanPhamController controller, DanhMucSanPhamDao danhMucSanPhamDao) {
    this.controller = Objects.requireNonNull(controller, "controller");
    this.danhMucSanPhamDao = danhMucSanPhamDao;
    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Quản lý dòng sản phẩm",
        "Tạo mới và cập nhật thông tin dòng sản phẩm, giá bán trước thuế và thuế suất.");
    header.setTrailing(saveButton);
    view.add(header, "growx, wrap");

    categoryField.setRequired(true);
    categoryBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
          JList<?> list,
          Object value,
          int index,
          boolean isSelected,
          boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof DanhMucSanPham danhMuc) {
          setText(formatDanhMucDisplay(danhMuc));
        } else {
          setText("");
        }
        return this;
      }
    });

    JPanel form = new JPanel(new MigLayout("fillx, insets 0", "[grow][grow]", "[]16[]16[]"));
    form.setOpaque(false);
    form.add(tenField, "growx");
    form.add(categoryField, "growx, wrap");
    form.add(unitField, "growx");
    form.add(priceField, "growx, wrap");
    form.add(taxField, "growx");

    Card formCard = Card.standard();
    formCard.setBody(form);
    view.add(formCard, "growx, wrap");

    saveButton.addActionListener(e -> luu());
    table.setCodeColumn(0);
    table.setMoneyColumn(4);
    table.setColumnWidths(140, 220, 170, 90, 120, 90, 120);
    statefulView = new StatefulView(table, "shopping-cart", "Chưa có dòng sản phẩm", "Thêm dòng sản phẩm mới để bắt đầu.");

    Card listCard = Card.standard();
    listCard.setBody(statefulView);
    view.add(listCard, "grow, push");

    taiDanhSach();
  }

  public ProductLinesPage(DongSanPhamController controller) {
    this(controller, null);
  }

  static String formatDanhMucDisplay(DanhMucSanPham danhMuc) {
    if (danhMuc == null) {
      return "";
    }
    String ten = danhMuc.tenDanhMuc() == null ? "" : danhMuc.tenDanhMuc().trim();
    String moTa = danhMuc.moTa() == null ? "" : danhMuc.moTa().trim();
    StringBuilder text = new StringBuilder();
    if (danhMuc.maDanhMuc() != null && !danhMuc.maDanhMuc().isBlank()) {
      text.append(danhMuc.maDanhMuc().trim());
    }
    if (!ten.isEmpty()) {
      if (!text.isEmpty()) {
        text.append(" — ");
      }
      text.append(ten);
    }
    if (!moTa.isEmpty()) {
      if (!text.isEmpty()) {
        text.append(" — ");
      }
      text.append(moTa);
    }
    return text.toString();
  }

  private void taiDanhSach() {
    categoryBox.removeAllItems();
    List<DanhMucSanPham> danhMuc = danhMucSanPhamDao != null ? danhMucSanPhamDao.findAll() : List.of();
    for (DanhMucSanPham item : danhMuc) {
      categoryBox.addItem(item);
    }

    tableModel.setRowCount(0);
    for (DongSanPham dong : controller.layDanhSach()) {
      tableModel.addRow(new Object[] {
          dong.maDongSP(),
          dong.tenDongSP(),
          dong.danhMucSanPham().tenDanhMuc(),
          dong.donViTinh(),
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

    if (tenField.getText() == null || tenField.getText().isBlank()) {
      tenField.setError("Vui lòng nhập tên dòng sản phẩm");
      return;
    }

    Object selected = categoryBox.getSelectedItem();
    if (selected == null) {
      categoryField.setError("Vui lòng chọn danh mục sản phẩm");
      return;
    }

    String donViTinh = DongSanPhamController.normalizeDonViTinh(unitField.getText());
    if (donViTinh == null || donViTinh.isBlank()) {
      unitField.setError("Input chỉ chấp nhận Chiếc hoặc Cái");
      return;
    }
    unitField.setText(donViTinh);

    if (priceField.getText() == null || priceField.getText().isBlank()) {
      priceField.setError("Vui lòng nhập giá bán trước thuế");
      return;
    }
    if (priceField.getMoney() <= 0) {
      priceField.setError("Giá bán trước thuế phải lớn hơn 0");
      return;
    }

    if (taxField.getText() == null || taxField.getText().isBlank()) {
      taxField.setError("Vui lòng nhập thuế suất");
      return;
    }
    if (taxField.getMoney() < 0 || taxField.getMoney() > 100) {
      taxField.setError("Thuế suất phải nằm trong khoảng 0% đến 100%");
      return;
    }

    String maDanhMuc = ((DanhMucSanPham) selected).maDanhMuc();
    var ketQua = controller.themMoi(
        tenField.getText(),
        maDanhMuc,
        donViTinh,
        BigDecimal.valueOf(priceField.getMoney()),
        BigDecimal.valueOf(taxField.getMoney()),
        true);
    if (ketQua.thanhCong()) {
      clearErrors();
      tenField.setText("");
      unitField.setText("");
      priceField.setText("");
      taxField.setText("");
      categoryBox.setSelectedIndex(-1);
      taiDanhSach();
      return;
    }
    if (ketQua.thongBao().toLowerCase().contains("đơn vị tính")) {
      unitField.setError(ketQua.thongBao());
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
    tenField.setError(ketQua.thongBao());
  }

  private void clearErrors() {
    tenField.setError(null);
    categoryField.setError(null);
    unitField.setError(null);
    priceField.setError(null);
    taxField.setError(null);
  }

  @Override
  public PageId id() {
    return PageId.PRODUCT_LINES;
  }

  @Override
  public String title() {
    return "Quản lý dòng sản phẩm";
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
