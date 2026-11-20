package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.SanPhamController;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.component.Pagination;
import com.dtaq.retail.ui.component.PagedLoader;
import com.dtaq.retail.ui.format.NhanHienThi;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DatePicker;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
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

  private final JComboBox<NhaCungCap> supplierCombo = new JComboBox<>();
  private final JComboBox<DongSanPham> productLineCombo = new JComboBox<>();
  private final FormField serialField = FormField.text("Số serial", "Nhập số serial", "package");
  private final FormField supplierField = new FormField("Nhà cung cấp", supplierCombo);
  private final FormField productLineField = new FormField("Dòng sản phẩm", productLineCombo);
  private final FormField priceField = FormField.money("Giá nhập", "0");
  private final DatePicker dateField = new DatePicker("Ngày nhập kho");
  private final JComboBox<TrangThaiSanPham> statusBox = new JComboBox<>(TrangThaiSanPham.values());
  private final DtaqButton importButton = DtaqButton.secondary("Import Excel", "upload");
  private final DtaqButton saveButton = DtaqButton.primary("Lưu máy", "save");
  private SanPham sanPhamDangSua;

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Số serial", "Dòng sản phẩm", "Nhà cung cấp", "Tình trạng", "Giá nhập", "Thao tác"}, 5);
  private final DataTable dataTable = new DataTable(tableModel, 36);
  private final StatefulView tableState;
  private final List<SanPham> cachedProducts = new ArrayList<>();
  private final FormField searchField =
      FormField.search("Tìm máy", "Nhập số serial, tên dòng sản phẩm hoặc nhà cung cấp");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm", "search");
  private static final int KICH_THUOC_TRANG = 20;
  private final Pagination pagination = new Pagination();
  private final PagedLoader<SanPham> trang =
      new PagedLoader<>(pagination, KICH_THUOC_TRANG, "máy", this::veBang);
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

    NhanHienThi.datNhan(supplierCombo, NhaCungCap::tenNCC, "Chọn nhà cung cấp");
    NhanHienThi.datNhan(productLineCombo, DongSanPham::tenDongSP, "Chọn dòng sản phẩm");
    NhanHienThi.datNhan(statusBox, NhanHienThi::trangThaiSanPham, "");

    PageHeader header = new PageHeader(
        "Quản lý sản phẩm",
        "Thêm máy mới vào kho và đồng bộ số lượng tồn theo từng dòng sản phẩm.");
    importButton.addActionListener(e -> importExcel());
    header.setTrailing(importButton, saveButton);
    view.add(header, "growx, wrap");
    taiDuLieuThamChieu();

    FormField statusField = new FormField("Trạng thái", statusBox);
    statusField.setRequired(true);

    // Lưới 3 cột bằng nhau; FormField đã chừa dòng thông báo nên khoảng cách hàng chỉ cần nhỏ.
    JPanel form = new JPanel(new MigLayout("fillx, insets 0, gapy 8", "[0:0,grow,fill]16[0:0,grow,fill]16[0:0,grow,fill]", "[]"));
    form.setOpaque(false);
    form.add(serialField);
    form.add(supplierField);
    form.add(productLineField, "wrap");
    form.add(priceField);
    form.add(dateField.field());
    form.add(statusField);

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
    dataTable.setColumnWidths(160, 260, 180, 150, 130, 160);
    tableState = new StatefulView(dataTable, "package", "Chưa có máy", "Bấm lưu để thêm máy mới vào kho.");
    tableState.showEmpty("search", "Chưa tra cứu", "Nhập từ khóa rồi nhấn Tìm kiếm.");

    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[bottom]"));
    filterRow.setOpaque(false);
    searchField.getTextField().addActionListener(e -> timKiem());
    searchButton.addActionListener(e -> timKiem());
    filterRow.add(searchField, "growx");
    // Căn nút với ô nhập: FormField chừa 20px (khoảng cách + dòng thông báo) dưới ô nhập.
    filterRow.add(searchButton, "gapbottom " + DtaqSpacing.FIELD_MESSAGE_H);

    Card tableCard = Card.standard();
    tableCard.add(filterRow, "growx, wrap");
    tableCard.setBody(tableState);
    tableCard.add(pagination, "growx");
    view.add(tableCard, "grow, push");
  }

  /** Tìm theo từ khóa đang nhập (rỗng thì duyệt toàn bộ) và nạp trang đầu. */
  private void timKiem() {
    daTimKiem = true;
    String tuKhoa = searchField.getText();
    trang.datNguon((boQua, gioiHan) -> sanPhamDao.timTrang(tuKhoa, boQua, gioiHan));
  }

  /** Dữ liệu vừa thay đổi: nạp lại trang đang xem. Bảng chưa từng nạp thì giữ nguyên. */
  private void taiDanhSach() {
    if (daTimKiem) {
      trang.lamMoiDongBo();
    }
  }

  /** Vẽ một trang; chỉ số dòng của bảng trùng với {@link #cachedProducts}. */
  private void veBang(List<SanPham> dong) {
    cachedProducts.clear();
    cachedProducts.addAll(dong);
    tableModel.setRowCount(0);
    for (SanPham sanPham : cachedProducts) {
      tableModel.addRow(new Object[] {
          sanPham.soSerial(),
          sanPham.dongSanPham().tenDongSP(),
          sanPham.nhaCungCap().tenNCC(),
          NhanHienThi.trangThaiSanPham(sanPham.trangThai()),
          sanPham.giaNhap()
      });
    }
    if (cachedProducts.isEmpty()) {
      tableState.showEmpty("search", "Không tìm thấy máy", "Thử từ khóa khác.");
    } else {
      tableState.showContent();
    }
  }

  /** Lần đầu mở trang: nạp sẵn trang đầu ở luồng nền; các lần sau giữ nguyên dữ liệu đã nạp. */
  @Override
  public void onShow() {
    trang.chayNen(tableState::showLoading,
        loi -> tableState.showError("Không thể tải danh sách máy. Vui lòng thử lại."));
    if (!daTimKiem) {
      timKiem();
    }
  }

  private void taiDuLieuThamChieu() {
    if (nhaCungCapDao != null) {
      supplierCombo.removeAllItems();
      supplierCombo.addItem(null);
      nhaCungCapDao.findAll().stream()
          .sorted(java.util.Comparator.comparing(NhaCungCap::tenNCC))
          .forEach(supplierCombo::addItem);
    }
    if (dongSanPhamDao != null) {
      productLineCombo.removeAllItems();
      productLineCombo.addItem(null);
      dongSanPhamDao.findAll().stream()
          .sorted(java.util.Comparator.comparing(DongSanPham::tenDongSP))
          .forEach(productLineCombo::addItem);
    }
  }

  /** Mã của lựa chọn hiện tại; rỗng nếu chưa chọn. */
  private static <T> String maCua(JComboBox<T> combo, java.util.function.Function<T, String> ma) {
    @SuppressWarnings("unchecked")
    T chon = (T) combo.getSelectedItem();
    return chon == null ? "" : ma.apply(chon);
  }

  private static <T> void chonTheoMa(
      JComboBox<T> combo, java.util.function.Function<T, String> ma, String maCanChon) {
    for (int i = 0; i < combo.getItemCount(); i++) {
      T item = combo.getItemAt(i);
      if (item != null && ma.apply(item).equals(maCanChon)) {
        combo.setSelectedIndex(i);
        return;
      }
    }
    combo.setSelectedItem(null);
  }

  private void chonSua(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= cachedProducts.size()) {
      return;
    }
    SanPham sp = cachedProducts.get(rowIndex);
    sanPhamDangSua = sp;
    serialField.setText(sp.soSerial());
    chonTheoMa(supplierCombo, NhaCungCap::maNCC, sp.nhaCungCap().maNCC());
    chonTheoMa(productLineCombo, DongSanPham::maDongSP, sp.dongSanPham().maDongSP());
    priceField.setText(sp.giaNhap().toPlainString());
    dateField.setDate(sp.ngayNhapKhoBanDau() == null ? LocalDate.now() : sp.ngayNhapKhoBanDau());
    statusBox.setSelectedItem(sp.trangThai());
    // Trạng thái máy đã có chỉ đổi qua bán hàng, đổi/trả, xử lý sản phẩm lỗi.
    statusBox.setEnabled(false);
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
    supplierCombo.setSelectedItem(null);
    productLineCombo.setSelectedItem(null);
    priceField.setText("");
    dateField.setDate(LocalDate.now());
    statusBox.setSelectedItem(TrangThaiSanPham.SAN_SANG_BAN);
    statusBox.setEnabled(true);
    serialField.getTextField().setEditable(true);
    saveButton.setText("Lưu máy");
    serialField.setError(null);
    dataTable.repaint();
  }

  private void importExcel() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Chọn file Excel nhập sản phẩm");
    chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
    chooser.setAcceptAllFileFilterUsed(false);
    if (chooser.showOpenDialog(view) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = chooser.getSelectedFile();
    if (file == null || !file.exists()) {
      JOptionPane.showMessageDialog(view, "Vui lòng chọn file Excel hợp lệ.", "Import sản phẩm", JOptionPane.ERROR_MESSAGE);
      return;
    }

    importButton.setBusy(true);
    new SwingWorker<SanPhamController.KetQua<SanPhamController.NhapKetQua>, Void>() {
      @Override
      protected SanPhamController.KetQua<SanPhamController.NhapKetQua> doInBackground() {
        return controller.nhapDanhSach(file);
      }

      @Override
      protected void done() {
        importButton.setBusy(false);
        try {
          SanPhamController.KetQua<SanPhamController.NhapKetQua> ketQua = get();
          if (ketQua.thanhCong()) {
            JOptionPane.showMessageDialog(
                view,
                ketQua.thongBao(),
                "Import sản phẩm",
                JOptionPane.INFORMATION_MESSAGE);
            resetForm();
            daTimKiem = true;
            taiDanhSach();
          } else {
            JOptionPane.showMessageDialog(
                view,
                ketQua.thongBao(),
                "Import sản phẩm",
                JOptionPane.ERROR_MESSAGE);
          }
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
              view,
              "Không thể import file Excel. Vui lòng kiểm tra cấu trúc dữ liệu.",
              "Import sản phẩm",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  private void luuMay() {
    if (dateField.hasInvalidText()) {
      dateField.setError(DatePicker.LOI_DINH_DANG);
      dateField.textField().requestFocusInWindow();
      return;
    }
    LocalDate ngayNhap = dateField.getDate();

    SanPhamController.KetQua<SanPham> ketQua;
    if (sanPhamDangSua != null) {
      ketQua = controller.capNhat(
          sanPhamDangSua.soSerial(),
          maCua(supplierCombo, NhaCungCap::maNCC),
          maCua(productLineCombo, DongSanPham::maDongSP),
          BigDecimal.valueOf(priceField.getMoney()),
          ngayNhap,
          (TrangThaiSanPham) statusBox.getSelectedItem());
    } else {
      ketQua = controller.themMay(
          serialField.getText(),
          maCua(supplierCombo, NhaCungCap::maNCC),
          maCua(productLineCombo, DongSanPham::maDongSP),
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
