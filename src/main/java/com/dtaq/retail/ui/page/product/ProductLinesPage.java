package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.DongSanPhamController;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
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
  private final DtaqButton ngungButton = DtaqButton.secondary("Ngừng kinh doanh");
  private final DtaqButton kinhDoanhLaiButton = DtaqButton.secondary("Kinh doanh lại");
  private DongSanPham dongDangSua;

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã dòng", "Tên dòng", "Danh mục", "ĐVT", "Giá", "Thuế", "Kinh doanh", "Thao tác"}, 7);
  private final DataTable table = new DataTable(tableModel, 36);
  private final StatefulView statefulView;
  private final List<DongSanPham> danhSachDong = new ArrayList<>();
  private final FormField searchField =
      FormField.search("Tìm dòng sản phẩm", "Nhập mã, tên dòng hoặc tên danh mục");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private boolean daTimKiem;
  private DongSanPham dongDangChon;

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
    header.setTrailing(ngungButton, kinhDoanhLaiButton, saveButton);
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
    ngungButton.setEnabled(false);
    kinhDoanhLaiButton.setEnabled(false);
    ngungButton.addActionListener(e -> xacNhanNgungKinhDoanh());
    kinhDoanhLaiButton.addActionListener(e -> xacNhanKinhDoanhLai());
    table.setOnRowDoubleClick(this::chonDong);
    table.getTable().getSelectionModel().addListSelectionListener(event -> {
      if (!event.getValueIsAdjusting()) {
        capNhatLuaChon();
      }
    });
    table.setActionColumn(7, row -> {
      if (dongDangSua != null && row >= 0 && row < danhSachDong.size()
          && danhSachDong.get(row).maDongSP().equals(dongDangSua.maDongSP())) {
        return List.of(
            RowAction.secondary("Sửa", this::chonSua),
            RowAction.danger("Hủy", this::huyChinhSua));
      }
      return List.of(RowAction.secondary("Sửa", this::chonSua));
    });
    table.setCodeColumn(0);
    table.setMoneyColumn(4);
    table.setColumnWidths(140, 220, 170, 90, 120, 90, 120, 150);
    statefulView = new StatefulView(table, "shopping-cart", "Chưa có dòng sản phẩm", "Thêm dòng sản phẩm mới để bắt đầu.");

    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[bottom]"));
    filterRow.setOpaque(false);
    searchField.getTextField().addActionListener(e -> {
      if (!searchField.getText().isBlank()) {
        timKiem();
      }
    });
    searchButton.addActionListener(e -> {
      if (!searchField.getText().isBlank()) {
        timKiem();
      }
    });
    filterRow.add(searchField, "growx");
    filterRow.add(searchButton);

    Card listCard = Card.standard();
    listCard.add(filterRow, "growx, wrap");
    listCard.setBody(statefulView);
    view.add(listCard, "grow, push");

    taiDanhSach();
  }

  /** Tìm theo từ khóa đang nhập; dùng chung cho nút Tìm kiếm và kiểm thử. */
  void timKiem() {
    daTimKiem = true;
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

    dongDangChon = null;
    danhSachDong.clear();
    tableModel.setRowCount(0);
    // Danh mục ở trên là lựa chọn của form; bảng chỉ nạp sau khi người dùng tìm kiếm.
    if (!daTimKiem) {
      capNhatLuaChon();
      statefulView.showEmpty("search", "Chưa tra cứu", "Nhập từ khóa rồi nhấn Tìm kiếm.");
      return;
    }
    String tuKhoa = searchField.getText().trim().toLowerCase();
    for (DongSanPham dong : controller.layDanhSach()) {
      if (!tuKhoa.isEmpty()
          && !dong.maDongSP().toLowerCase().contains(tuKhoa)
          && !dong.tenDongSP().toLowerCase().contains(tuKhoa)
          && !dong.danhMucSanPham().tenDanhMuc().toLowerCase().contains(tuKhoa)) {
        continue;
      }
      danhSachDong.add(dong);
      tableModel.addRow(new Object[] {
          dong.maDongSP(),
          dong.tenDongSP(),
          dong.danhMucSanPham().tenDanhMuc(),
          dong.donViTinh(),
          dong.donGiaBanTruocThue(),
          dong.thue(),
          dong.trangThaiKinhDoanh() ? "Đang kinh doanh" : "Ngừng kinh doanh",
          ""
      });
    }
    capNhatLuaChon();
    if (tableModel.getRowCount() == 0) {
      statefulView.showEmpty("search", "Không tìm thấy dòng sản phẩm", "Thử từ khóa khác.");
    } else {
      statefulView.showContent();
    }
  }

  /** Chọn dòng theo chỉ số model, dùng chung cho nhấp đúp và kiểm thử. */
  void chonDong(int modelRow) {
    int viewRow = table.getTable().convertRowIndexToView(modelRow);
    if (viewRow >= 0) {
      table.getTable().setRowSelectionInterval(viewRow, viewRow);
    }
    capNhatLuaChon();
  }

  private void capNhatLuaChon() {
    int viewRow = table.getTable().getSelectedRow();
    dongDangChon = null;
    if (viewRow >= 0) {
      int modelRow = table.getTable().convertRowIndexToModel(viewRow);
      if (modelRow >= 0 && modelRow < danhSachDong.size()) {
        dongDangChon = danhSachDong.get(modelRow);
      }
    }
    boolean dangKinhDoanh = dongDangChon != null && dongDangChon.trangThaiKinhDoanh();
    ngungButton.setEnabled(dongDangChon != null && dangKinhDoanh);
    kinhDoanhLaiButton.setEnabled(dongDangChon != null && !dangKinhDoanh);
  }

  private void chonSua(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= danhSachDong.size()) {
      return;
    }
    DongSanPham dong = danhSachDong.get(rowIndex);
    dongDangSua = dong;
    dongDangChon = dong;
    int viewRow = table.getTable().convertRowIndexToView(rowIndex);
    if (viewRow >= 0) {
      table.getTable().setRowSelectionInterval(viewRow, viewRow);
    }
    capNhatLuaChon();
    tenField.setText(dong.tenDongSP());
    chonDanhMuc(dong.danhMucSanPham() == null ? null : dong.danhMucSanPham().maDanhMuc());
    unitField.setText(dong.donViTinh());
    if (dong.donGiaBanTruocThue() != null) {
      priceField.setText(dong.donGiaBanTruocThue().toPlainString());
    } else {
      priceField.setText("0");
    }
    if (dong.thue() != null) {
      taxField.setText(dong.thue().toPlainString());
    } else {
      taxField.setText("0");
    }
    saveButton.setText("Cập nhật");
    clearErrors();
    table.repaint();
  }

  private void chonDanhMuc(String maDanhMuc) {
    categoryBox.setSelectedIndex(-1);
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return;
    }
    for (int i = 0; i < categoryBox.getItemCount(); i++) {
      DanhMucSanPham item = categoryBox.getItemAt(i);
      if (item != null && maDanhMuc.equals(item.maDanhMuc())) {
        categoryBox.setSelectedIndex(i);
        return;
      }
    }
  }

  private void huyChinhSua(int rowIndex) {
    resetForm();
  }

  private void resetForm() {
    dongDangSua = null;
    dongDangChon = null;
    tenField.setText("");
    unitField.setText("");
    priceField.setText("");
    taxField.setText("");
    categoryBox.setSelectedIndex(-1);
    saveButton.setText("Lưu");
    clearErrors();
    capNhatLuaChon();
    table.repaint();
  }

  private void xacNhanNgungKinhDoanh() {
    if (dongDangChon == null) {
      return;
    }
    String maDong = dongDangChon.maDongSP();
    ConfirmDialog.confirm(
        view,
        "Xác nhận ngừng kinh doanh",
        "Dòng " + maDong + " sẽ không còn được bán. Tiếp tục?",
        "Ngừng kinh doanh",
        () -> apDungTrangThai(false));
  }

  private void xacNhanKinhDoanhLai() {
    if (dongDangChon == null) {
      return;
    }
    String maDong = dongDangChon.maDongSP();
    ConfirmDialog.confirm(
        view,
        "Xác nhận kinh doanh lại",
        "Dòng " + maDong + " sẽ được bán trở lại. Tiếp tục?",
        "Kinh doanh lại",
        () -> apDungTrangThai(true));
  }

  /**
   * Áp dụng trạng thái kinh doanh cho dòng đang chọn; gọi controller ngoài EDT rồi tải lại danh sách.
   *
   * @param kinhDoanh {@code true} để kinh doanh lại, {@code false} để ngừng kinh doanh
   */
  void apDungTrangThai(boolean kinhDoanh) {
    if (dongDangChon == null) {
      return;
    }
    String maDong = dongDangChon.maDongSP();
    ngungButton.setEnabled(false);
    kinhDoanhLaiButton.setEnabled(false);
    new SwingWorker<DongSanPhamController.KetQua<DongSanPham>, Void>() {
      @Override
      protected DongSanPhamController.KetQua<DongSanPham> doInBackground() {
        return kinhDoanh ? controller.kinhDoanhLai(maDong) : controller.ngungKinhDoanh(maDong);
      }

      @Override
      protected void done() {
        try {
          DongSanPhamController.KetQua<DongSanPham> ketQua = get();
          if (ketQua.thanhCong()) {
            Toast.success(view, ketQua.thongBao());
            taiDanhSach();
            int modelRow = -1;
            for (int i = 0; i < danhSachDong.size(); i++) {
              if (danhSachDong.get(i).maDongSP().equals(maDong)) {
                modelRow = i;
                break;
              }
            }
            if (modelRow >= 0) {
              table.getTable().setRowSelectionInterval(modelRow, modelRow);
              capNhatLuaChon();
            } else {
              dongDangChon = null;
              capNhatLuaChon();
            }
          } else {
            Toast.error(view, ketQua.thongBao());
            capNhatLuaChon();
          }
        } catch (Exception ex) {
          Toast.error(view, "Không thể cập nhật trạng thái kinh doanh của dòng sản phẩm");
          capNhatLuaChon();
        }
      }
    }.execute();
  }

  DtaqButton nutNgung() {
    return ngungButton;
  }

  DtaqButton nutKinhDoanhLai() {
    return kinhDoanhLaiButton;
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
    DongSanPhamController.KetQua<DongSanPham> ketQua;
    if (dongDangSua != null) {
      ketQua = controller.capNhat(
          dongDangSua.maDongSP(),
          tenField.getText(),
          maDanhMuc,
          donViTinh,
          BigDecimal.valueOf(priceField.getMoney()),
          BigDecimal.valueOf(taxField.getMoney()),
          dongDangSua.trangThaiKinhDoanh());
    } else {
      ketQua = controller.themMoi(
          tenField.getText(),
          maDanhMuc,
          donViTinh,
          BigDecimal.valueOf(priceField.getMoney()),
          BigDecimal.valueOf(taxField.getMoney()),
          true);
    }
    if (ketQua.thanhCong()) {
      clearErrors();
      resetForm();
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
