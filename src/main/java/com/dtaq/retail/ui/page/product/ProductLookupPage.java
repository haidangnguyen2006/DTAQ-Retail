package com.dtaq.retail.ui.page.product;

import com.dtaq.retail.controller.TraCuuSanPhamController;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình tra cứu sản phẩm và tồn kho theo UC004.
 */
public final class ProductLookupPage implements Page {

  private final TraCuuSanPhamController controller;
  private final JPanel view;

  private final FormField keywordField = FormField.search("Tìm kiếm", "Nhập serial, mã hoặc tên dòng");
  private final SegmentedControl criteria = new SegmentedControl(
      new String[] {"Serial", "Dòng SP", "Danh mục"}, 0);
  private final StatusPill countPill = StatusPill.neutral("0 kết quả");
  private final DefaultTableModel resultModel = DataTable.readOnlyModel(
      new String[] {"Mã dòng", "Tên dòng", "Danh mục", "Tồn kho", "Máy sẵn bán"}, 0);
  private final DataTable resultTable = new DataTable(resultModel, 36);
  private final DefaultTableModel detailModel = DataTable.readOnlyModel(
      new String[] {"Số serial", "Tình trạng", "Giá nhập", "Ngày nhập"}, 0);
  private final DataTable detailTable = new DataTable(detailModel, 36);
  private final StatefulView tableState;
  private final StatefulView detailState;

  private final List<TraCuuSanPhamController.KetQuaDong> cachedResults = new ArrayList<>();

  public ProductLookupPage(TraCuuSanPhamController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Tra cứu sản phẩm",
        "Tìm nhanh theo serial, dòng sản phẩm hoặc danh mục và xem máy khả dụng để bán.");
    view.add(header, "growx, wrap");

    resultTable.setCodeColumn(0);
    resultTable.setColumnWidths(140, 220, 170, 90, 110);
    resultTable.setOnRowDoubleClick(this::renderDetailForRow);

    detailTable.setCodeColumn(0);
    detailTable.setColumnWidths(180, 150, 150, 160);

    tableState = new StatefulView(resultTable, "search", "Chưa có dữ liệu", "Nhập từ khóa để tra cứu sản phẩm.");
    detailState = new StatefulView(detailTable, "package", "Chưa có máy", "Chọn một dòng sản phẩm để xem chi tiết.");

    Card card = Card.standard();
    card.add(buildFilterRow(), "growx, wrap");
    card.setBody(tableState);

    view.add(card, "grow, push, wrap");
    view.add(detailState, "grow, push");

    criteria.setOnSelectionChange(index -> refreshResults());
    keywordField.getTextField().addActionListener(e -> refreshResults());
    keywordField.setShortcut("F2");
    refreshResults();
  }

  private JPanel buildFilterRow() {
    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[][grow]12[]push[]", "[]"));
    row.setOpaque(false);
    row.add(criteria, "growx");
    row.add(keywordField, "growx");
    row.add(countPill, "align right");
    return row;
  }

  private void refreshResults() {
    String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
    var chi = switch (criteria.getSelectedIndex()) {
      case 1 -> TraCuuSanPhamController.TieuChi.DONG_SAN_PHAM;
      case 2 -> TraCuuSanPhamController.TieuChi.DANH_MUC;
      default -> TraCuuSanPhamController.TieuChi.SERIAL;
    };

    cachedResults.clear();
    cachedResults.addAll(controller.timKiem(chi, keyword));
    resultModel.setRowCount(0);
    if (cachedResults.isEmpty()) {
      countPill.setText("0 kết quả");
      tableState.showEmpty("search", "Không tìm thấy kết quả", "Thử từ khóa hoặc tiêu chí khác.");
      detailState.showEmpty("package", "Chưa có máy", "Chọn một dòng sản phẩm để xem chi tiết.");
      return;
    }

    countPill.setText(cachedResults.size() + " kết quả");
    for (var ketQua : cachedResults) {
      resultModel.addRow(new Object[] {
          ketQua.maDongSP(),
          ketQua.tenDongSP(),
          ketQua.tenDanhMuc(),
          ketQua.soLuongTon(),
          ketQua.danhSachSanPham().stream().filter(sp -> sp.trangThai() == com.dtaq.retail.entity.TrangThaiSanPham.SAN_SANG_BAN).count()
      });
    }
    tableState.showContent();
    renderDetailForRow(0);
  }

  private void renderDetailForRow(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= cachedResults.size()) {
      detailState.showEmpty();
      return;
    }
    var selected = cachedResults.get(rowIndex);
    detailModel.setRowCount(0);
    for (var sp : selected.danhSachSanPham()) {
      detailModel.addRow(new Object[] {
          sp.soSerial(),
          sp.trangThai().name(),
          sp.giaNhap(),
          sp.ngayNhapKhoBanDau()
      });
    }
    if (selected.danhSachSanPham().isEmpty()) {
      detailState.showEmpty("package", "Không có máy nào", "Dòng sản phẩm này chưa có máy trong kho.");
    } else {
      detailState.showContent();
    }
  }

  @Override
  public PageId id() {
    return PageId.PRODUCT_LOOKUP;
  }

  @Override
  public String title() {
    return "Tra cứu sản phẩm";
  }

  @Override
  public String section() {
    return "Bán hàng";
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
