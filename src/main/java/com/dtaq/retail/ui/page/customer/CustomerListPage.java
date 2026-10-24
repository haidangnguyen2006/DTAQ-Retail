package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.EmptyState;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.Pagination;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Quản lý khách hàng (CustomerListPage) theo Figma frame 1:368.
 */
public class CustomerListPage implements Page {

  public record CustomerRow(
      String maKH,
      String hoTen,
      String soDienThoai,
      String diaChi,
      String ngayTao
  ) {}

  private final JPanel view;
  private final FormField searchField;
  private final DtaqButton searchButton;
  private final StatusPill countPill;
  private final Card tableCard;
  private final DataTable dataTable;
  private final DefaultTableModel tableModel;
  private final Pagination pagination;

  private AddCustomerDialog addCustomerDialog;
  private Consumer<String> onSearchListener;
  private Consumer<CustomerRow> onOpenDetailListener;

  public CustomerListPage() {
    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"
    ));
    view.setBackground(DtaqColors.BG_APP);

    // 1. Header
    PageHeader header = new PageHeader(
        "Quản lý khách hàng",
        "Tra cứu hồ sơ khách hàng để phục vụ và lập hóa đơn nhanh chóng."
    );
    DtaqButton addCustomerBtn = DtaqButton.primary("Thêm khách hàng", "plus");
    addCustomerBtn.setShortcutKey("F4");
    addCustomerBtn.addActionListener(e -> openAddDialog());
    header.setTrailing(addCustomerBtn);
    view.add(header, "growx, wrap");

    // 2. Card contains Filter + Table + Pagination
    tableCard = Card.standard();

    // Filter row
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]push[]", "[bottom]"));
    filterRow.setOpaque(false);

    searchField = FormField.text("Tìm khách hàng", "Nhập họ tên hoặc số điện thoại", "search");
    searchField.setShortcut("F2");
    searchField.getTextField().addActionListener(e -> triggerSearch());
    filterRow.add(searchField, "growx");

    searchButton = DtaqButton.secondary("Tìm kiếm");
    searchButton.addActionListener(e -> triggerSearch());
    filterRow.add(searchButton);

    countPill = StatusPill.neutral("0 khách hàng");
    filterRow.add(countPill, "align right, aligny center");

    tableCard.add(filterRow, "growx, wrap, gapbottom 16");

    // Table
    String[] columns = {"Mã khách hàng", "Họ và tên", "Số điện thoại", "Địa chỉ", "Ngày tạo", "Thao tác"};
    tableModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    dataTable = new DataTable(tableModel, 60);
    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(120, 180, 140, 420, 100, 80);
    tableCard.add(dataTable, "grow, push, wrap, gapbottom 16");

    // Pagination
    pagination = new Pagination();
    pagination.setInfo("Hiển thị 0 trên 0 khách hàng");
    tableCard.add(pagination, "growx");

    view.add(tableCard, "grow, push");
  }

  public void renderCustomers(List<CustomerRow> list, int total) {
    tableModel.setRowCount(0);
    if (list == null || list.isEmpty()) {
      countPill.setText("0 khách hàng");
      pagination.setPageData(1, 1, "Chưa có khách hàng nào");
      return;
    }

    countPill.setText(total + " khách hàng");
    for (CustomerRow c : list) {
      tableModel.addRow(new Object[]{
          c.maKH(),
          c.hoTen(),
          PhoneFormat.format(c.soDienThoai()),
          c.diaChi() != null ? c.diaChi() : "",
          c.ngayTao() != null ? c.ngayTao() : "—",
          "Chi tiết"
      });
    }
    pagination.setPageData(1, (int) Math.ceil((double) total / 8), "Hiển thị 1–" + list.size() + " trên " + total + " khách hàng");
  }

  private void triggerSearch() {
    if (onSearchListener != null) {
      onSearchListener.accept(searchField.getText().trim());
    }
  }

  private void openAddDialog() {
    Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(view);
    if (addCustomerDialog == null || addCustomerDialog.getOwner() != topFrame) {
      addCustomerDialog = new AddCustomerDialog(topFrame);
    }
    addCustomerDialog.clearForm();
    addCustomerDialog.setVisible(true);
  }

  public AddCustomerDialog getAddCustomerDialog() {
    return addCustomerDialog;
  }

  public void setOnSearch(Consumer<String> listener) {
    this.onSearchListener = listener;
  }

  public void setOnOpenDetail(Consumer<CustomerRow> listener) {
    this.onOpenDetailListener = listener;
  }

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusSearch");
    actionMap.put("focusSearch", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        searchField.getTextField().requestFocusInWindow();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "openAddCustomer");
    actionMap.put("openAddCustomer", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openAddDialog();
      }
    });
  }

  @Override
  public PageId id() {
    return PageId.CUSTOMERS;
  }

  @Override
  public String title() {
    return "Quản lý khách hàng";
  }

  @Override
  public String section() {
    return "Bán hàng";
  }

  @Override
  public JComponent view() {
    return view;
  }
}
