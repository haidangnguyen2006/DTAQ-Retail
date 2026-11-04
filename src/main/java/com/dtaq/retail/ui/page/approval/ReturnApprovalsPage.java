package com.dtaq.retail.ui.page.approval;

import com.dtaq.retail.controller.PheDuyetDoiTraController;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.page.home.ApprovalDecisionDialog;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.util.ArrayList;
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
 * Màn hình phê duyệt yêu cầu đổi/trả hàng (PageId.RETURN_APPROVALS, UC018).
 *
 * <p>Trang chỉ hiển thị danh sách phiếu đang chờ Quản lý phê duyệt do controller cung cấp và
 * chuyển quyết định duyệt/từ chối ra controller. Ngưỡng hoàn tiền và các kiểm tra nghiệp vụ nằm
 * ở controller, không nằm trong giao diện.
 */
public class ReturnApprovalsPage implements Page {

  private static final Logger LOG = LoggerFactory.getLogger(ReturnApprovalsPage.class);

  private static final String LOI_QUYEN = "Chỉ Quản lý được phê duyệt yêu cầu đổi trả";
  private static final String LOI_TAI_DANH_SACH = "Không thể tải danh sách phiếu chờ duyệt";
  private static final String LOI_GHI_QUYET_DINH = "Không thể ghi quyết định phê duyệt";

  private final PheDuyetDoiTraController controller;
  private final JPanel view;

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã phiếu", "Loại", "Mã HĐ gốc", "Khách hàng", "SĐT", "Hoàn dự kiến",
          "Nhân viên lập", "Lý do", "Ngày tạo", ""},
      9);
  private final DataTable dataTable = new DataTable(tableModel, 50);
  private final StatefulView tableState;
  private final List<PhieuDuyet> cachedList = new ArrayList<>();

  /**
   * Khởi tạo trang phê duyệt yêu cầu đổi trả.
   *
   * @param controller bộ điều khiển phê duyệt đổi trả; không được {@code null}
   */
  public ReturnApprovalsPage(PheDuyetDoiTraController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Phê duyệt yêu cầu đổi trả",
        "Kiểm tra phiếu đổi/trả vượt hạn mức trước khi nhân viên hoàn tất giao dịch.");
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setCodeColumn(2);
    dataTable.setMoneyColumn(5);
    dataTable.setColumnWidths(120, 90, 130, 170, 120, 140, 150, 240, 110, 180);
    dataTable.setActionColumn(9,
        RowAction.primary("Phê duyệt", row -> moQuyetDinh(true, row)),
        RowAction.danger("Từ chối", row -> moQuyetDinh(false, row)));

    tableState = new StatefulView(
        dataTable,
        "circle-check",
        "Không có yêu cầu chờ duyệt",
        "Các phiếu đổi/trả cần phê duyệt sẽ hiển thị tại đây.");
    tableState.setOnRetry(this::taiDanhSach);

    Card tableCard = Card.standard();
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
  }

  @Override
  public PageId id() {
    return PageId.RETURN_APPROVALS;
  }

  @Override
  public String title() {
    return "Phê duyệt yêu cầu đổi trả";
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

  @Override
  public void onShow() {
    taiDanhSach();
  }

  /**
   * Tải danh sách phiếu chờ duyệt ở luồng nền; chỉ cập nhật giao diện trong {@code done()}.
   */
  public void taiDanhSach() {
    tableState.showLoading();

    new SwingWorker<List<PhieuDuyet>, Void>() {
      @Override
      protected List<PhieuDuyet> doInBackground() {
        return controller.danhSachChoDuyet();
      }

      @Override
      protected void done() {
        try {
          renderDanhSach(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          tableState.showError(LOI_TAI_DANH_SACH);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            tableState.showError(LOI_QUYEN);
          } else {
            LOG.error("Không thể tải danh sách phiếu chờ duyệt", e.getCause());
            tableState.showError(LOI_TAI_DANH_SACH);
          }
        } catch (RuntimeException e) {
          LOG.error("Không thể tải danh sách phiếu chờ duyệt", e);
          tableState.showError(LOI_TAI_DANH_SACH);
        }
      }
    }.execute();
  }

  private void renderDanhSach(List<PhieuDuyet> danhSach) {
    cachedList.clear();
    tableModel.setRowCount(0);

    if (danhSach == null || danhSach.isEmpty()) {
      tableState.showEmpty();
      return;
    }

    cachedList.addAll(danhSach);
    for (PhieuDuyet phieu : danhSach) {
      tableModel.addRow(new Object[] {
          anToan(phieu.maPhieu()),
          tenLoai(phieu.loai()),
          anToan(phieu.maHDGoc()),
          anToan(phieu.tenKhachHang()),
          anToan(phieu.soDienThoai()),
          phieu.hoanDuKien() == null ? 0L : phieu.hoanDuKien().longValueExact(),
          anToan(phieu.tenNhanVienLap()),
          anToan(phieu.lyDo()),
          DateFormats.formatDate(phieu.ngayTao()),
          ""});
    }
    tableState.showContent();
  }

  private void moQuyetDinh(boolean duyet, int modelRow) {
    if (modelRow < 0 || modelRow >= cachedList.size()) {
      return;
    }
    String maPhieu = cachedList.get(modelRow).maPhieu();
    new ApprovalDecisionDialog(
        ConfirmDialog.frameOf(view), duyet, maPhieu,
        (ma, yKien) -> guiQuyetDinh(duyet, ma, yKien)).setVisible(true);
  }

  private void guiQuyetDinh(boolean duyet, String maPhieu, String yKien) {
    new SwingWorker<PheDuyetDoiTraController.KetQua, Void>() {
      @Override
      protected PheDuyetDoiTraController.KetQua doInBackground() {
        return controller.quyetDinh(maPhieu, duyet, yKien);
      }

      @Override
      protected void done() {
        try {
          PheDuyetDoiTraController.KetQua ketQua = get();
          if (ketQua.thanhCong()) {
            Toast.success(view, ketQua.thongBao());
          } else {
            Toast.error(view, ketQua.thongBao());
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, LOI_GHI_QUYET_DINH);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            Toast.error(view, LOI_QUYEN);
          } else {
            LOG.error("Không thể ghi quyết định phê duyệt", e.getCause());
            Toast.error(view, LOI_GHI_QUYET_DINH);
          }
        } catch (RuntimeException e) {
          LOG.error("Không thể ghi quyết định phê duyệt", e);
          Toast.error(view, LOI_GHI_QUYET_DINH);
        }
        taiDanhSach();
      }
    }.execute();
  }

  private static String tenLoai(PhieuDuyet.LoaiPhieu loai) {
    if (loai == null) {
      return "—";
    }
    return switch (loai) {
      case PHIEU_TRA -> "Trả hàng";
      case PHIEU_DOI -> "Đổi hàng";
    };
  }

  private static String anToan(String value) {
    return value == null ? "" : value;
  }
}
