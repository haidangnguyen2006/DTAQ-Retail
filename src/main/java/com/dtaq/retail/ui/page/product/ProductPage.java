package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.SanPhamController;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DatePicker;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.table.RowAction;
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
  private final NhaCungCapDao nhaCungCapDao;
  private final DongSanPhamDao dongSanPhamDao;
  private final JPanel view;

  private final JComboBox<String> supplierCombo = new JComboBox<>();
  private final JComboBox<String> productLineCombo = new JComboBox<>();
  private final FormField serialField = FormField.text("Số serial", "Nhập số serial", "package");
  private final FormField supplierField = new FormField("Mã NCC", supplierCombo);
  private final FormField productLineField = new FormField("Mã dòng SP", productLineCombo);
  private final FormField priceField = FormField.money("Giá nhập", "0");
  private final DatePicker dateField = new DatePicker("Ngày nhập kho");
  private final JComboBox<TrangThaiSanPham> statusBox = new JComboBox<>(TrangThaiSanPham.values());
  private final DtaqButton saveButton = DtaqButton.primary("Lưu máy", "save");
  private SanPham sanPhamDangSua;

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Số serial", "Dòng SP", "NCC", "Tình trạng", "Giá nhập", "Thao tác"}, 5);
  private final DataTable dataTable = new DataTable(tableModel, 36);
  private final StatefulView tableState;
  private final List<SanPham> cachedProducts = new ArrayList<>();
  private final FormField searchField =
      FormField.search("Tìm máy", "Nhập số serial, mã dòng SP hoặc mã NCC");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private boolean daTimKiem;

  public ProductPage(SanPhamController controller, SanPhamDao sanPhamDao) {
    this(controller, sanPhamDao, null, null);
  }

  public ProductPage(
      SanPhamController controller,
      SanPhamDao sanPhamDao,
      NhaCungCapDao nhaCungCapDao,
      DongSanPhamDao dongSanPhamDao) {
    this.controller = Objects.requireNonNull(controller, "controller");
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.nhaCungCapDao = nhaCungCapDao;
    this.dongSanPhamDao = dongSanPhamDao;
    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    supplierCombo.setEditable(true);
    productLineCombo.setEditable(true);

    PageHeader header = new PageHeader(
        "Quản lý sản phẩm",
        "Thêm máy mới vào kho và đồng bộ số lượng tồn theo từng dòng sản phẩm.");
    header.setTrailing(saveButton);
    view.add(header, "growx, wrap");
    taiDuLieuThamChieu();

    FormField statusField = new FormField("Trạng thái", statusBox);
    statusField.setRequired(true);

    JPanel form = new JPanel(new MigLayout("fillx, insets 0", "[grow][grow]", "[]16[]16[]"));
    form.setOpaque(false);
    form.add(serialField, "growx");
    form.add(supplierField, "growx, wrap");
    form.add(productLineField, "growx");
    form.add(priceField, "growx, wrap");
    form.add(dateField.field(), "growx");
    form.add(statusField, "growx");

    Card formCard = Card.standard();
    formCard.setBody(form);
    view.add(formCard, "growx, wrap");

    saveButton.addActionListener(e -> luuMay());
    dataTable.setCodeColumn(0);
    dataTable.setMoneyColumn(4);
    dataTable.setActionColumn(5, row -> {
      if (sanPhamDangSua != null && row >= 0 && row < cachedProducts.size()
          && cachedProducts.get(row).soSerial().equals(sanPhamDangSua.soSerial())) {
        return List.of(
            RowAction.secondary("Sửa", this::chonSua),
            RowAction.danger("Hủy", this::huyChinhSua));
      }
      return List.of(RowAction.secondary("Sửa", this::chonSua));
    });
    dataTable.setOnRowDoubleClick(this::chonSua);
    dataTable.setColumnWidths(160, 180, 120, 140, 130, 160);
    tableState = new StatefulView(dataTable, "package", "Chưa có máy", "Bấm lưu để thêm máy mới vào kho.");
    tableState.showEmpty("search", "Chưa tra cứu", "Nhập từ khóa rồi nhấn Tìm kiếm.");

    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[bottom]"));
    filterRow.setOpaque(false);
    searchField.getTextField().addActionListener(e -> timKiem());
    searchButton.addActionListener(e -> timKiem());
    filterRow.add(searchField, "growx");
    filterRow.add(searchButton);

    Card tableCard = Card.standard();
    tableCard.add(filterRow, "growx, wrap");
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
  }

  private void timKiem() {
    if (searchField.getText().isBlank()) {
      return;
    }
    daTimKiem = true;
    taiDanhSach();
  }

  /** Bảng chỉ nạp sau khi người dùng tìm kiếm; lọc theo serial, mã dòng SP hoặc mã NCC. */
  private void taiDanhSach() {
    if (!daTimKiem) {
      return;
    }
    String tuKhoa = searchField.getText().trim().toLowerCase();
    cachedProducts.clear();
    sanPhamDao.findAll().stream()
        .filter(sp -> tuKhoa.isEmpty()
            || sp.soSerial().toLowerCase().contains(tuKhoa)
            || sp.dongSanPham().maDongSP().toLowerCase().contains(tuKhoa)
            || sp.nhaCungCap().maNCC().toLowerCase().contains(tuKhoa))
        .forEach(cachedProducts::add);
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
      tableState.showEmpty("search", "Không tìm thấy máy", "Thử từ khóa khác.");
    } else {
      tableState.showContent();
    }
  }

  private void taiDuLieuThamChieu() {
    if (nhaCungCapDao != null) {
      supplierCombo.removeAllItems();
      supplierCombo.addItem("");
      nhaCungCapDao.findAll().stream()
          .map(NhaCungCap::maNCC)
          .sorted()
          .forEach(supplierCombo::addItem);
    }
    if (dongSanPhamDao != null) {
      productLineCombo.removeAllItems();
      productLineCombo.addItem("");
      dongSanPhamDao.findAll().stream()
          .map(DongSanPham::maDongSP)
          .sorted()
          .forEach(productLineCombo::addItem);
    }
  }

  private String getComboText(JComboBox<String> comboBox) {
    Object value = comboBox.getSelectedItem();
    return value == null ? "" : String.valueOf(value).trim();
  }

  private void chonSua(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= cachedProducts.size()) {
      return;
    }
    SanPham sp = cachedProducts.get(rowIndex);
    sanPhamDangSua = sp;
    serialField.setText(sp.soSerial());
    supplierCombo.setSelectedItem(sp.nhaCungCap().maNCC());
    productLineCombo.setSelectedItem(sp.dongSanPham().maDongSP());
    priceField.setText(sp.giaNhap().toPlainString());
    dateField.setDate(sp.ngayNhapKhoBanDau() == null ? LocalDate.now() : sp.ngayNhapKhoBanDau());
    statusBox.setSelectedItem(sp.trangThai());
    serialField.getTextField().setEditable(false);
    saveButton.setText("Cập nhật");
    serialField.setError(null);
    dataTable.repaint();
  }

  private void huyChinhSua(int rowIndex) {
    resetForm();
  }

  private void resetForm() {
    sanPhamDangSua = null;
    serialField.setText("");
    supplierCombo.setSelectedItem("");
    productLineCombo.setSelectedItem("");
    priceField.setText("");
    dateField.setDate(LocalDate.now());
    statusBox.setSelectedItem(TrangThaiSanPham.SAN_SANG_BAN);
    serialField.getTextField().setEditable(true);
    saveButton.setText("Lưu máy");
    serialField.setError(null);
    dataTable.repaint();
  }

  private void luuMay() {
    LocalDate ngayNhap = dateField.getDate();

    SanPhamController.KetQua<SanPham> ketQua;
    if (sanPhamDangSua != null) {
      ketQua = controller.capNhat(
          sanPhamDangSua.soSerial(),
          getComboText(supplierCombo),
          getComboText(productLineCombo),
          BigDecimal.valueOf(priceField.getMoney()),
          ngayNhap,
          (TrangThaiSanPham) statusBox.getSelectedItem());
    } else {
      ketQua = controller.themMay(
          serialField.getText(),
          getComboText(supplierCombo),
          getComboText(productLineCombo),
          BigDecimal.valueOf(priceField.getMoney()),
          ngayNhap,
          (TrangThaiSanPham) statusBox.getSelectedItem());
    }

    if (ketQua.thanhCong()) {
      resetForm();
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
