package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.controller.KhachHangController;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.Pagination;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình Quản lý khách hàng (UC007, CustomerListPage, Figma frame 1:368).
 *
 * <p>Trang chỉ hiển thị danh sách do controller cung cấp qua {@link #renderCustomers}; tìm kiếm, đổi trang,
 * mở chi tiết và lưu khách hàng mới được chuyển ra qua các callback {@code setOn…}.
 */
public class CustomerListPage implements Page {

  /** Số khách hàng trên mỗi trang; controller dùng cùng giá trị khi truy vấn. */
  public static final int PAGE_SIZE = 10;

  private static final Logger log = LoggerFactory.getLogger(CustomerListPage.class);

  private static final String NOT_CONNECTED_TIP = "Chức năng đang được kết nối dữ liệu";
  private static final String LOAD_ERROR = "Không thể tải danh sách khách hàng. Vui lòng thử lại";

  public record CustomerRow(
      String maKH,
      String hoTen,
      String soDienThoai,
      String diaChi,
      String ngayTao
  ) {}

  private final JPanel view;
  private final FormField searchField = FormField.search("Tìm khách hàng", "Nhập họ tên hoặc số điện thoại");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private final DtaqButton addCustomerBtn = DtaqButton.primary("Thêm khách hàng", "plus");
  private final StatusPill countPill = StatusPill.neutral("0 khách hàng");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã khách hàng", "Họ và tên", "Số điện thoại", "Địa chỉ", "Ngày tạo", "Thao tác"}, 5);
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;
  private final Pagination pagination = new Pagination();
  private final List<CustomerRow> rows = new ArrayList<>();

  private AddCustomerDialog addCustomerDialog;
  private Consumer<String> onSearchListener;
  private Consumer<Integer> onPageChangeListener;
  private Consumer<CustomerRow> onOpenDetailListener;
  private Consumer<AddCustomerDialog.NewCustomerForm> onSaveCustomerListener;
  private String lastKeyword = "";
  private KhachHangController controller;
  private int currentPage = 1;
  private int loadRequest;

  public CustomerListPage() {
    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        "Quản lý khách hàng",
        "Tra cứu hồ sơ khách hàng để phục vụ và lập hóa đơn nhanh chóng.");
    addCustomerBtn.setShortcutKey("F4");
    addCustomerBtn.addActionListener(e -> openAddDialog());
    header.setTrailing(addCustomerBtn);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(130, 200, 140, 380, 110, 110);
    dataTable.setActionColumn(5, RowAction.secondary("Chi tiết", this::openDetail));
    dataTable.setOnRowDoubleClick(this::openDetail);
    tableState = new StatefulView(dataTable, "users", "Chưa có khách hàng", "Danh sách khách hàng sẽ hiển thị tại đây.");

    Card tableCard = Card.standard();
    tableCard.add(buildFilterRow(), "growx, wrap");
    tableCard.setBody(tableState);
    pagination.setInfo("Chưa có khách hàng nào");
    pagination.setOnPageChangeListener(page -> {
      if (onPageChangeListener != null) {
        onPageChangeListener.accept(page);
      }
    });
    tableCard.add(pagination, "growx");
    view.add(tableCard, "grow, push");

    refreshControls();
  }

  /**
   * Khởi tạo trang đã nối dữ liệu thật: danh sách, tìm kiếm, phân trang, thêm và cập nhật khách hàng
   * đều đi qua {@code controller}, chạy ngoài EDT.
   *
   * @param controller bộ điều khiển khách hàng; không được {@code null}
   */
  public CustomerListPage(KhachHangController controller) {
    this();
    this.controller = Objects.requireNonNull(controller, "controller");
    tableState.setOnRetry(() -> loadPage(currentPage));
    setOnSearch(keyword -> loadPage(1));
    setOnPageChange(this::loadPage);
    setOnOpenDetail(this::openEditDialog);
    setOnSaveCustomer(form -> addCustomerDialog.save(
        () -> controller.themKhach(form.hoTen(), form.soDienThoai(), form.diaChi()),
        saved -> {
          Toast.success(view, "Đã lưu khách hàng");
          loadPage(currentPage);
        },
        this::showExistingCustomer));
  }

  private void loadPage(int page) {
    int request = ++loadRequest;
    String keyword = lastKeyword;
    showLoading();
    new SwingWorker<KhachHangController.TrangKhachHang, Void>() {
      @Override
      protected KhachHangController.TrangKhachHang doInBackground() {
        return controller.danhSach(keyword, page, PAGE_SIZE);
      }

      @Override
      protected void done() {
        if (request != loadRequest) {
          return;
        }
        try {
          KhachHangController.TrangKhachHang trang = get();
          currentPage = trang.trang();
          renderCustomers(
              trang.danhSach().stream().map(CustomerListPage::toRow).toList(), trang.tongSo(), trang.trang());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          showError(LOAD_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể tải danh sách khách hàng (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          showError(cause instanceof SecurityException ? cause.getMessage() : LOAD_ERROR);
        }
      }
    }.execute();
  }

  /** Bảng chưa có cột ngày tạo trong CSDL nên để trống; {@link #renderCustomers} hiển thị "—". */
  private static CustomerRow toRow(KhachHang khachHang) {
    return new CustomerRow(
        khachHang.maKH(), khachHang.hoTen(), khachHang.soDienThoai(), khachHang.diaChi(), null);
  }

  private void openEditDialog(CustomerRow row) {
    AddCustomerDialog dialog = AddCustomerDialog.forEdit(
        ConfirmDialog.frameOf(view), row.maKH(), row.hoTen(), row.soDienThoai(), row.diaChi());
    dialog.setOnSave(form -> dialog.save(
        () -> controller.capNhatKhach(row.maKH(), form.hoTen(), form.soDienThoai(), form.diaChi()),
        saved -> {
          Toast.success(view, "Đã cập nhật khách hàng");
          loadPage(currentPage);
        },
        this::showExistingCustomer));
    dialog.setVisible(true);
  }

  /** Luồng 8.a: không tạo hồ sơ trùng, đưa hồ sơ hiện có lên bảng để nhân viên tiếp tục sử dụng. */
  private void showExistingCustomer(KhachHang khachHang) {
    searchField.setText(khachHang.soDienThoai());
    lastKeyword = khachHang.soDienThoai();
    Toast.show(view, "Đang dùng hồ sơ " + khachHang.maKH(), Toast.Kind.INFO);
    loadPage(1);
  }

  private JPanel buildFilterRow() {
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]push[]", "[bottom]"));
    filterRow.setOpaque(false);

    searchField.setShortcut("F2");
    searchField.getTextField().addActionListener(e -> triggerSearch());
    filterRow.add(searchField, "growx");

    searchButton.addActionListener(e -> triggerSearch());
    filterRow.add(searchButton);
    filterRow.add(countPill, "align right, aligny center");
    return filterRow;
  }

  // ---------------------------------------------------------------- public API

  /**
   * Hiển thị một trang khách hàng.
   *
   * @param list khách hàng của trang hiện tại
   * @param total tổng số khách hàng khớp điều kiện tìm kiếm
   * @param page số trang hiện tại (bắt đầu từ 1)
   */
  public void renderCustomers(List<CustomerRow> list, int total, int page) {
    rows.clear();
    tableModel.setRowCount(0);
    if (list == null || list.isEmpty()) {
      countPill.setText("0 khách hàng");
      pagination.setPageData(1, 1, "Chưa có khách hàng nào");
      if (lastKeyword.isEmpty()) {
        tableState.showEmpty();
      } else {
        tableState.showEmpty("search", "Không tìm thấy khách hàng",
            "Thử tìm bằng họ tên hoặc số điện thoại khác.");
      }
      return;
    }

    rows.addAll(list);
    for (CustomerRow c : list) {
      tableModel.addRow(new Object[] {
          c.maKH(),
          c.hoTen(),
          PhoneFormat.format(c.soDienThoai()),
          c.diaChi() != null ? c.diaChi() : "",
          c.ngayTao() != null ? c.ngayTao() : "—",
          ""
      });
    }
    int from = (page - 1) * PAGE_SIZE + 1;
    int to = from + list.size() - 1;
    countPill.setText(total + " khách hàng");
    pagination.setPageData(page, Pagination.totalPages(total, PAGE_SIZE),
        "Hiển thị " + from + "–" + to + " trên " + total + " khách hàng");
    tableState.showContent();
  }

  public void showLoading() {
    tableState.showLoading();
  }

  public void showError(String message) {
    tableState.showError(message);
  }

  public void setOnSearch(Consumer<String> listener) {
    this.onSearchListener = listener;
    refreshControls();
  }

  /** Nhận số trang mới (bắt đầu từ 1) khi người dùng bấm trước/sau. */
  public void setOnPageChange(Consumer<Integer> listener) {
    this.onPageChangeListener = listener;
  }

  public void setOnOpenDetail(Consumer<CustomerRow> listener) {
    this.onOpenDetailListener = listener;
  }

  /** Nhận dữ liệu form đã qua kiểm tra nhanh khi người dùng bấm "Lưu khách hàng". */
  public void setOnSaveCustomer(Consumer<AddCustomerDialog.NewCustomerForm> listener) {
    this.onSaveCustomerListener = listener;
    if (addCustomerDialog != null) {
      addCustomerDialog.setOnSave(listener);
    }
    refreshControls();
  }

  /** Hiển thị lỗi của controller dưới ô tương ứng ("hoTen", "soDienThoai", "diaChi") trong hộp thoại thêm. */
  public void showAddCustomerFieldError(String fieldName, String message) {
    if (addCustomerDialog != null) {
      addCustomerDialog.setBusy(false);
      addCustomerDialog.setFieldError(fieldName, message);
    }
  }

  public void setAddCustomerBusy(boolean busy) {
    if (addCustomerDialog != null) {
      addCustomerDialog.setBusy(busy);
    }
  }

  /** Lưu thành công: đóng hộp thoại và báo bằng thông báo ngắn. */
  public void closeAddCustomerDialog() {
    if (addCustomerDialog != null) {
      addCustomerDialog.closeWithSuccess();
    }
    Toast.success(view, "Đã lưu khách hàng");
  }

  public AddCustomerDialog getAddCustomerDialog() {
    return addCustomerDialog;
  }

  // ---------------------------------------------------------------- hành vi

  private void refreshControls() {
    boolean canSearch = onSearchListener != null;
    searchButton.setEnabled(canSearch);
    searchButton.setToolTipText(canSearch ? null : NOT_CONNECTED_TIP);
    searchField.getInputComponent().setEnabled(canSearch);

    boolean canAdd = onSaveCustomerListener != null;
    addCustomerBtn.setEnabled(canAdd);
    addCustomerBtn.setToolTipText(canAdd ? null : NOT_CONNECTED_TIP);
  }

  private void triggerSearch() {
    if (onSearchListener != null) {
      lastKeyword = searchField.getText().trim();
      onSearchListener.accept(lastKeyword);
    }
  }

  private void openDetail(int modelRow) {
    if (onOpenDetailListener != null && modelRow >= 0 && modelRow < rows.size()) {
      onOpenDetailListener.accept(rows.get(modelRow));
    }
  }

  private void openAddDialog() {
    if (onSaveCustomerListener == null) {
      return;
    }
    Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(view);
    if (addCustomerDialog == null || addCustomerDialog.getOwner() != topFrame) {
      addCustomerDialog = new AddCustomerDialog(topFrame);
      addCustomerDialog.setOnSave(onSaveCustomerListener);
    }
    addCustomerDialog.clearForm();
    addCustomerDialog.setVisible(true);
  }

  // ---------------------------------------------------------------- Page

  @Override
  public void onShow() {
    if (controller != null) {
      loadPage(currentPage);
    }
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

  /** Bảng tự giãn hết chiều cao khung và tự cuộn bên trong. */
  @Override
  public boolean scrollable() {
    return false;
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
