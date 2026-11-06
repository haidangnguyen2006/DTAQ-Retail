package com.dtaq.retail.ui.page.employee;

import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.controller.NhanVienController.KetQuaLuuNhanVien;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.Pagination;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình Quản lý nhân viên (UC013): tìm nhân viên theo tên/mã/SĐT/email, lọc theo trạng thái, xem
 * theo trang, thêm nhân viên bán hàng, sửa hồ sơ và khóa/mở khóa tài khoản. Mật khẩu không bao giờ
 * được đưa lên bảng.
 */
public final class EmployeePage implements Page {

  private static final Logger log = LoggerFactory.getLogger(EmployeePage.class);

  private static final int PAGE_SIZE = 10;
  private static final String LOAD_ERROR = "Không thể tải danh sách nhân viên. Vui lòng thử lại";
  private static final String UPDATE_ERROR = "Không thể cập nhật trạng thái tài khoản. Vui lòng thử lại";
  private static final String[] BO_LOC = {"Tất cả", "Đang hoạt động", "Đã khóa"};

  private final NhanVienController controller;
  private final JPanel view;
  private final DtaqButton addEmployeeBtn = DtaqButton.primary("Thêm nhân viên", "plus");
  private final FormField searchField = FormField.search(null,
      "Họ tên (có dấu hoặc không), mã, tên đăng nhập, email hoặc số điện thoại");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private final SegmentedControl filter = new SegmentedControl(BO_LOC, 0);
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã nhân viên", "Họ và tên", "Số điện thoại", "Email", "Ngày vào làm",
      "Tên đăng nhập", "Quyền", "Trạng thái", "Thao tác"}, 8);
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;
  private final Pagination pagination = new Pagination();
  /** Toàn bộ kết quả lọc; bảng chỉ hiện trang {@link #currentPage}. */
  private final List<NhanVien> ketQua = new ArrayList<>();
  private final List<NhanVien> rows = new ArrayList<>();

  private int currentPage = 1;
  private int loadRequest;
  /** Bảng chỉ nạp dữ liệu sau lần tìm kiếm đầu tiên của người dùng. */
  private boolean daTimKiem;

  /**
   * Khởi tạo trang quản lý nhân viên.
   *
   * @param controller bộ điều khiển nhân viên; không được {@code null}
   */
  public EmployeePage(NhanVienController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader("Quản lý nhân viên", null);
    
    addEmployeeBtn.addActionListener(e -> openAddDialog());
    header.setTrailing(addEmployeeBtn);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(150, 170, 115, 160, 100, 95, 140, 135, 165);
    dataTable.setActionColumn(8,
        RowAction.secondary("Sửa", this::openEditDialog),
        RowAction.secondary("Khóa / Mở", this::confirmToggle));
    dataTable.setOnRowDoubleClick(this::openEditDialog);
    tableState = new StatefulView(dataTable, "users", "Chưa có nhân viên",
        "Bấm \"Thêm nhân viên\" để tạo tài khoản đầu tiên.");
    tableState.setOnRetry(this::loadEmployees);
    tableState.showEmpty("search", "Chưa tra cứu", "Nhập từ khóa rồi nhấn Tìm kiếm.");
    filter.setOnSelectionChange(index -> loadEmployees());
    pagination.setOnPageChangeListener(page -> {
      currentPage = page;
      renderPage();
    });

    Card tableCard = Card.standard();
    tableCard.add(buildFilterRow(), "growx, wrap");
    tableCard.setBody(tableState);
    tableCard.add(pagination, "growx");
    view.add(tableCard, "grow, push");
  }

  private JPanel buildFilterRow() {
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]16[]", "[]"));
    filterRow.setOpaque(false);
    searchField.getTextField().setToolTipText("Gõ từ khóa rồi nhấn Enter hoặc bấm Tìm kiếm • Esc để xóa");
    searchField.getTextField().addActionListener(e -> timKiem());
    searchButton.addActionListener(e -> timKiem());
    searchField.getTextField().registerKeyboardAction(e -> {
      searchField.setText("");
      loadEmployees();
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
    filterRow.add(searchField.getInputComponent(), "growx, aligny center");
    filterRow.add(searchButton, "aligny center");
    filterRow.add(filter, "aligny center");
    return filterRow;
  }

  /** Bộ lọc đang chọn: {@code null} là tất cả. */
  private Boolean selectedStatus() {
    return switch (filter.getSelectedIndex()) {
      case 1 -> Boolean.TRUE;
      case 2 -> Boolean.FALSE;
      default -> null;
    };
  }

  private void loadEmployees() {
    loadEmployees(false);
  }

  /** @param giuTrang giữ trang đang xem (sau khi sửa/khóa), thay vì về trang 1 (khi đổi bộ lọc). */
  private void loadEmployees(boolean giuTrang) {
    if (!daTimKiem) {
      return;
    }
    int request = ++loadRequest;
    Boolean status = selectedStatus();
    String tuKhoa = searchField.getText();
    tableState.showLoading();
    new SwingWorker<List<NhanVien>, Void>() {
      @Override
      protected List<NhanVien> doInBackground() {
        return controller.tim(tuKhoa, status);
      }

      @Override
      protected void done() {
        if (request != loadRequest) {
          return;
        }
        try {
          ketQua.clear();
          ketQua.addAll(get());
          if (!giuTrang) {
            currentPage = 1;
          }
          renderPage();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          tableState.showError(LOAD_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể tải danh sách nhân viên (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          tableState.showError(cause instanceof SecurityException ? cause.getMessage() : LOAD_ERROR);
        }
      }
    }.execute();
  }

  private void renderPage() {
    rows.clear();
    tableModel.setRowCount(0);
    int tong = ketQua.size();
    if (tong == 0) {
      pagination.setPageData(1, 1, "Không có nhân viên nào");
      if (searchField.getText().isBlank() && selectedStatus() != null) {
        tableState.showEmpty("users",
            "Không có tài khoản nào ở trạng thái \"" + BO_LOC[filter.getSelectedIndex()] + "\"", null);
      } else if (searchField.getText().isBlank()) {
        tableState.showEmpty();
      } else {
        tableState.showEmpty("search", "Không tìm thấy nhân viên",
            "Không có nhân viên khớp \"" + searchField.getText().trim() + "\" trong bộ lọc hiện tại.");
      }
      return;
    }
    int soTrang = Pagination.totalPages(tong, PAGE_SIZE);
    currentPage = Math.max(1, Math.min(currentPage, soTrang));
    int tu = (currentPage - 1) * PAGE_SIZE;
    int den = Math.min(tu + PAGE_SIZE, tong);
    rows.addAll(ketQua.subList(tu, den));
    for (NhanVien nv : rows) {
      tableModel.addRow(new Object[] {
          nv.maNV(),
          nv.hoTen(),
          PhoneFormat.format(nv.soDienThoai()),
          nv.email() != null ? nv.email() : "—",
          DateFormats.formatDate(nv.ngayVaoLam()),
          nv.tenDangNhap(),
          nv.moTaQuyen(),
          nv.trangThaiHoatDong() ? "● Hoạt động" : "○ Đã khóa",
          ""
      });
    }
    pagination.setPageData(currentPage, soTrang,
        "Hiển thị " + (tu + 1) + "–" + den + " trên " + tong + " nhân viên");
    tableState.showContent();
  }

  private void openAddDialog() {
    new AddEmployeeDialog(ConfirmDialog.frameOf(view), controller, saved -> {
      Toast.success(view, "Đã thêm nhân viên " + saved.maNV() + " • tài khoản " + saved.tenDangNhap());
      loadEmployees();
    }).setVisible(true);
  }

  private void openEditDialog(int modelRow) {
    if (modelRow < 0 || modelRow >= rows.size()) {
      return;
    }
    AddEmployeeDialog.forEdit(ConfirmDialog.frameOf(view), controller, rows.get(modelRow), saved -> {
      Toast.success(view, "Đã cập nhật hồ sơ " + saved.maNV());
      loadEmployees(true);
    }).setVisible(true);
  }

  private void confirmToggle(int modelRow) {
    if (modelRow < 0 || modelRow >= rows.size()) {
      return;
    }
    NhanVien nv = rows.get(modelRow);
    boolean khoa = nv.trangThaiHoatDong();
    String hanhDong = khoa ? "Khóa" : "Mở khóa";
    String noiDung = khoa
        ? "Khóa tài khoản " + nv.tenDangNhap() + " của " + nv.hoTen()
            + "? Nhân viên sẽ không đăng nhập được; hồ sơ và lịch sử giao dịch vẫn được giữ."
        : "Mở khóa tài khoản " + nv.tenDangNhap() + " của " + nv.hoTen() + "?";
    ConfirmDialog.confirm(view, hanhDong + " tài khoản", noiDung, hanhDong, () -> toggle(
        khoa ? () -> controller.khoaTaiKhoan(nv.maNV()) : () -> controller.moKhoaTaiKhoan(nv.maNV()),
        khoa ? "Đã khóa tài khoản " + nv.tenDangNhap() : "Đã mở khóa tài khoản " + nv.tenDangNhap()));
  }

  private void toggle(Supplier<KetQuaLuuNhanVien> action, String successMessage) {
    new SwingWorker<KetQuaLuuNhanVien, Void>() {
      @Override
      protected KetQuaLuuNhanVien doInBackground() {
        return action.get();
      }

      @Override
      protected void done() {
        try {
          KetQuaLuuNhanVien ketQuaLuu = get();
          if (ketQuaLuu.thanhCong()) {
            Toast.success(view, successMessage);
            loadEmployees(true);
          } else {
            Toast.error(view, ketQuaLuu.thongBao());
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, UPDATE_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể cập nhật trạng thái nhân viên (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          Toast.error(view, cause instanceof SecurityException ? cause.getMessage() : UPDATE_ERROR);
        }
      }
    }.execute();
  }

  @Override
  public void onShow() {
    // Không tự tải: bảng chỉ nạp khi người dùng tìm kiếm.
  }

  private void timKiem() {
    if (searchField.getText().isBlank()) {
      return;
    }
    daTimKiem = true;
    loadEmployees();
  }

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusEmployeeSearch");
    actionMap.put("focusEmployeeSearch", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        searchField.getTextField().requestFocusInWindow();
      }
    });
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "openAddEmployee");
    actionMap.put("openAddEmployee", new AbstractAction() {
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
    return PageId.EMPLOYEES;
  }

  @Override
  public String title() {
    return "Quản lý nhân viên";
  }

  @Override
  public String section() {
    return "Quản lý";
  }

  @Override
  public JComponent view() {
    return view;
  }
}
