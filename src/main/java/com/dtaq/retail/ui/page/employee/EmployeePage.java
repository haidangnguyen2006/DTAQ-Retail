package com.dtaq.retail.ui.page.employee;

import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình Quản lý nhân viên (UC013, khung): hiển thị danh sách nhân viên từ CSDL. Mật khẩu không
 * bao giờ được đưa lên bảng.
 */
public final class EmployeePage implements Page {

  private static final Logger log = LoggerFactory.getLogger(EmployeePage.class);

  private static final String NOT_CONNECTED_TIP = "Chức năng đang được kết nối dữ liệu";
  private static final String LOAD_ERROR = "Không thể tải danh sách nhân viên. Vui lòng thử lại";

  private final NhanVienController controller;
  private final JPanel view;
  private final DtaqButton addEmployeeBtn = DtaqButton.primary("Thêm nhân viên", "plus");
  private final StatusPill countPill = StatusPill.neutral("0 nhân viên");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã nhân viên", "Họ và tên", "Số điện thoại", "Email", "Ngày vào làm",
      "Tên đăng nhập", "Quyền", "Trạng thái"});
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;

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
    addEmployeeBtn.setEnabled(false);
    addEmployeeBtn.setToolTipText(NOT_CONNECTED_TIP);
    header.setTrailing(addEmployeeBtn);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(170, 190, 120, 220, 110, 130, 150, 120);
    tableState = new StatefulView(dataTable, "users", "Chưa có nhân viên", "Danh sách nhân viên sẽ hiển thị tại đây.");
    tableState.setOnRetry(this::loadEmployees);

    Card tableCard = Card.standard();
    JPanel countRow = new JPanel(new MigLayout("fillx, insets 0", "push[]", "[]"));
    countRow.setOpaque(false);
    countRow.add(countPill);
    tableCard.add(countRow, "growx, wrap");
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
  }

  private void loadEmployees() {
    int request = ++loadRequest;
    tableState.showLoading();
    new SwingWorker<List<NhanVien>, Void>() {
      @Override
      protected List<NhanVien> doInBackground() {
        return controller.danhSach();
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
    tableModel.setRowCount(0);
    countPill.setText(nhanViens.size() + " nhân viên");
    if (nhanViens.isEmpty()) {
      tableState.showEmpty();
      return;
    }
    for (NhanVien nv : nhanViens) {
      tableModel.addRow(new Object[] {
          nv.maNV(),
          nv.hoTen(),
          PhoneFormat.format(nv.soDienThoai()),
          nv.email() != null ? nv.email() : "",
          DateFormats.formatDate(nv.ngayVaoLam()),
          nv.tenDangNhap(),
          nv.moTaQuyen(),
          nv.trangThaiHoatDong() ? "Đang hoạt động" : "Đã khóa"
      });
    }
    tableState.showContent();
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
