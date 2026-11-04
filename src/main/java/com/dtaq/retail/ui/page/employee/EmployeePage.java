package com.dtaq.retail.ui.page.employee;

import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.controller.NhanVienController.KetQuaLuuNhanVien;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình Quản lý nhân viên (UC013): danh sách nhân viên lọc theo trạng thái, thêm nhân viên bán
 * hàng và khóa/mở khóa tài khoản. Mật khẩu không bao giờ được đưa lên bảng.
 */
public final class EmployeePage implements Page {

  private static final Logger log = LoggerFactory.getLogger(EmployeePage.class);

  private static final String LOAD_ERROR = "Không thể tải danh sách nhân viên. Vui lòng thử lại";
  private static final String UPDATE_ERROR = "Không thể cập nhật trạng thái tài khoản. Vui lòng thử lại";
  private static final String[] BO_LOC = {"Tất cả", "Đang hoạt động", "Đã khóa"};

  private final NhanVienController controller;
  private final JPanel view;
  private final DtaqButton addEmployeeBtn = DtaqButton.primary("Thêm nhân viên", "plus");
  private final SegmentedControl filter = new SegmentedControl(BO_LOC, 0);
  private final StatusPill countPill = StatusPill.neutral("0 nhân viên");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã nhân viên", "Họ và tên", "Số điện thoại", "Email", "Ngày vào làm",
      "Tên đăng nhập", "Quyền", "Trạng thái", "Thao tác"}, 8);
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;
  private final List<NhanVien> rows = new ArrayList<>();

  private int loadRequest;

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

    PageHeader header = new PageHeader(
        "Quản lý nhân viên", "Theo dõi hồ sơ và trạng thái tài khoản của nhân viên.");
    addEmployeeBtn.addActionListener(e -> openAddDialog());
    header.setTrailing(addEmployeeBtn);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(170, 180, 120, 210, 110, 130, 140, 120, 130);
    dataTable.setActionColumn(8, RowAction.secondary("Khóa / Mở khóa", this::confirmToggle));
    tableState = new StatefulView(dataTable, "users", "Chưa có nhân viên", "Danh sách nhân viên sẽ hiển thị tại đây.");
    tableState.setOnRetry(this::loadEmployees);
    filter.setOnSelectionChange(index -> loadEmployees());

    Card tableCard = Card.standard();
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[]push[]", "[center]"));
    filterRow.setOpaque(false);
    filterRow.add(filter);
    filterRow.add(countPill);
    tableCard.add(filterRow, "growx, wrap");
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
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
    int request = ++loadRequest;
    Boolean status = selectedStatus();
    tableState.showLoading();
    new SwingWorker<List<NhanVien>, Void>() {
      @Override
      protected List<NhanVien> doInBackground() {
        return controller.danhSach(status);
      }

      @Override
      protected void done() {
        if (request != loadRequest) {
          return;
        }
        try {
          render(get());
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

  private void render(List<NhanVien> nhanViens) {
    rows.clear();
    tableModel.setRowCount(0);
    countPill.setText(nhanViens.size() + " nhân viên");
    if (nhanViens.isEmpty()) {
      tableState.showEmpty();
      return;
    }
    rows.addAll(nhanViens);
    for (NhanVien nv : nhanViens) {
      tableModel.addRow(new Object[] {
          nv.maNV(),
          nv.hoTen(),
          PhoneFormat.format(nv.soDienThoai()),
          nv.email() != null ? nv.email() : "",
          DateFormats.formatDate(nv.ngayVaoLam()),
          nv.tenDangNhap(),
          nv.moTaQuyen(),
          nv.trangThaiHoatDong() ? "Đang hoạt động" : "Đã khóa",
          ""
      });
    }
    tableState.showContent();
  }

  private void openAddDialog() {
    new AddEmployeeDialog(ConfirmDialog.frameOf(view), controller, saved -> {
      Toast.success(view, "Đã thêm nhân viên " + saved.maNV());
      loadEmployees();
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
          KetQuaLuuNhanVien ketQua = get();
          if (ketQua.thanhCong()) {
            Toast.success(view, successMessage);
            loadEmployees();
          } else {
            Toast.error(view, ketQua.thongBao());
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
    loadEmployees();
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
