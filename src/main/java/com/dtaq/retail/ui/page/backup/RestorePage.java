package com.dtaq.retail.ui.page.backup;

import com.dtaq.retail.controller.BackupController;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.notify.NotificationService;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.FileSizeFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trang "Phục hồi dữ liệu" (PageId.RESTORE, FR016).
 *
 * <p>Quản lý chọn đúng một bản sao lưu, kiểm tra tính toàn vẹn của tệp rồi mở hộp thoại xác nhận hai
 * bước trước khi phục hồi toàn bộ cơ sở dữ liệu. Phục hồi là thao tác phá hủy: dữ liệu sau mốc sao
 * lưu sẽ mất. Mọi thao tác nặng chạy trong {@link SwingWorker}; không có SQL/JDBC ở đây.
 */
public final class RestorePage implements Page {

  private static final Logger log = LoggerFactory.getLogger(RestorePage.class);

  private static final String LOAD_ERROR = "Không thể tải danh sách bản sao lưu. Vui lòng thử lại";
  private static final String CHECK_ERROR = "Không thể kiểm tra tệp sao lưu. Vui lòng thử lại";
  private static final String RESTORE_ERROR = "Không thể phục hồi dữ liệu. Vui lòng thử lại";
  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

  private final BackupController controller;
  private final JPanel view;
  private final DtaqButton checkBtn = DtaqButton.secondary("Kiểm tra tệp", "check");
  private final DtaqButton restoreBtn = DtaqButton.dangerSoft("Phục hồi");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Tệp", "Thời điểm", "Kích thước", "Kiểm tra", "Trạng thái"});
  private final DataTable dataTable = new DataTable(tableModel, 48);
  private final StatefulView tableState;
  private final List<BackupRecord> rows = new ArrayList<>();

  private int loadRequest;
  private boolean busy;

  /**
   * Khởi tạo trang phục hồi dữ liệu.
   *
   * @param controller bộ điều phối sao lưu/phục hồi; không được {@code null}
   */
  public RestorePage(BackupController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][][grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        "Phục hồi dữ liệu",
        "Chọn một bản sao lưu đã kiểm tra để phục hồi toàn bộ cơ sở dữ liệu theo FR016.");
    checkBtn.setEnabled(false);
    restoreBtn.setEnabled(false);
    checkBtn.addActionListener(e -> kiemTraTep());
    restoreBtn.addActionListener(e -> batDauPhucHoi());
    header.setTrailing(checkBtn, restoreBtn);
    view.add(header, "growx, wrap");

    view.add(new InfoCallout(InfoCallout.Kind.WARNING,
        "Phục hồi sẽ thay thế toàn bộ dữ liệu hiện tại bằng dữ liệu trong bản sao lưu. "
            + "Dữ liệu sau mốc backup sẽ bị mất."), "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(260, 170, 110, 130, 120);
    dataTable.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    dataTable.getTable().getSelectionModel().addListSelectionListener(e -> updateActions());

    tableState = new StatefulView(
        dataTable,
        "clock",
        "Chưa có bản sao lưu",
        "Các bản sao lưu dùng để phục hồi sẽ hiển thị tại đây.");
    tableState.setOnRetry(this::taiDanhSach);

    Card listCard = Card.section(
        "Bản sao lưu khả dụng", "Chọn một dòng để kiểm tra hoặc phục hồi.", null);
    listCard.setBody(tableState);
    view.add(listCard, "grow, push");
  }

  /**
   * Tải danh sách bản sao lưu ở luồng nền; chỉ cập nhật giao diện trong {@code done()}.
   */
  public void taiDanhSach() {
    int request = ++loadRequest;
    busy = true;
    updateActions();
    tableState.showLoading();

    new SwingWorker<List<BackupRecord>, Void>() {
      @Override
      protected List<BackupRecord> doInBackground() {
        return controller.danhSach();
      }

      @Override
      protected void done() {
        if (request != loadRequest) {
          return;
        }
        try {
          renderDanhSach(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          tableState.showError(LOAD_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể tải danh sách bản sao lưu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          tableState.showError(cause instanceof SecurityException ? cause.getMessage() : LOAD_ERROR);
        } finally {
          busy = false;
          updateActions();
        }
      }
    }.execute();
  }

  private void renderDanhSach(List<BackupRecord> danhSach) {
    rows.clear();
    tableModel.setRowCount(0);

    if (danhSach == null || danhSach.isEmpty()) {
      updateActions();
      tableState.showEmpty();
      return;
    }

    rows.addAll(danhSach);
    for (BackupRecord banGhi : danhSach) {
      tableModel.addRow(new Object[] {
          banGhi.fileName(),
          thoiDiem(banGhi.createdAt()),
          FileSizeFormat.format(banGhi.fileSize()),
          "—",
          tenTrangThai(banGhi.trangThai())
      });
    }
    updateActions();
    tableState.showContent();
  }

  private void kiemTraTep() {
    int modelRow = selectedRow();
    BackupRecord banGhi = selectedRecord();
    if (modelRow < 0 || banGhi == null) {
      return;
    }

    busy = true;
    updateActions();
    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() {
        controller.kiemTra(banGhi);
        return null;
      }

      @Override
      protected void done() {
        busy = false;
        try {
          get();
          capNhatKetQuaKiemTra(modelRow, "Hợp lệ");
          Toast.success(view, "Tệp hợp lệ");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, CHECK_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể kiểm tra tệp sao lưu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          capNhatKetQuaKiemTra(modelRow, "Không hợp lệ");
          Toast.error(view, thongBaoLoi(cause, CHECK_ERROR));
        }
        updateActions();
      }
    }.execute();
  }

  private void batDauPhucHoi() {
    BackupRecord banGhi = selectedRecord();
    if (banGhi == null) {
      return;
    }
    new RestoreConfirmDialog(
        ConfirmDialog.frameOf(view), banGhi, () -> thucHienPhucHoi(banGhi)).setVisible(true);
  }

  private void thucHienPhucHoi(BackupRecord banGhi) {
    busy = true;
    updateActions();
    ProgressDialog progress = new ProgressDialog(
        ConfirmDialog.frameOf(view), "Đang phục hồi dữ liệu");
    progress.setStage("Đang phục hồi dữ liệu từ " + banGhi.fileName() + "...");

    new SwingWorker<BackupRecord, Void>() {
      @Override
      protected BackupRecord doInBackground() {
        return controller.phucHoi(banGhi, true);
      }

      @Override
      protected void done() {
        progress.finish();
        busy = false;
        try {
          get();
          Toast.success(view, "Đã phục hồi từ " + banGhi.fileName());
          NotificationService.thongBao(
              "Phục hồi dữ liệu", "Đã phục hồi từ " + banGhi.fileName());
          taiDanhSach();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, RESTORE_ERROR);
          NotificationService.thongBao("Phục hồi thất bại", RESTORE_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể phục hồi dữ liệu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          String thongBao = thongBaoLoi(cause, RESTORE_ERROR);
          Toast.error(view, thongBao);
          NotificationService.thongBao("Phục hồi thất bại", thongBao);
        }
        updateActions();
      }
    }.execute();

    progress.setVisible(true);
  }

  private void capNhatKetQuaKiemTra(int modelRow, String ketQua) {
    if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
      tableModel.setValueAt(ketQua, modelRow, 3);
    }
  }

  private int selectedRow() {
    int viewRow = dataTable.getTable().getSelectedRow();
    if (viewRow < 0) {
      return -1;
    }
    int modelRow = dataTable.getTable().convertRowIndexToModel(viewRow);
    return modelRow < rows.size() ? modelRow : -1;
  }

  private BackupRecord selectedRecord() {
    int modelRow = selectedRow();
    return modelRow < 0 ? null : rows.get(modelRow);
  }

  private void updateActions() {
    boolean chonDuoc = !busy && selectedRow() >= 0;
    checkBtn.setEnabled(chonDuoc);
    restoreBtn.setEnabled(chonDuoc);
  }

  private static String thongBaoLoi(Throwable cause, String macDinh) {
    if (cause instanceof SecurityException || cause instanceof BackupException) {
      String message = cause.getMessage();
      return message == null || message.isBlank() ? macDinh : message;
    }
    return macDinh;
  }

  private static String thoiDiem(Instant instant) {
    if (instant == null) {
      return "";
    }
    return DateFormats.formatDateTime(LocalDateTime.ofInstant(instant, MUI_GIO));
  }

  private static String tenTrangThai(BackupRecord.TrangThaiSaoLuu trangThai) {
    if (trangThai == null) {
      return "—";
    }
    return switch (trangThai) {
      case SUCCESS -> "Thành công";
      case FAILED -> "Thất bại";
    };
  }

  @Override
  public void onShow() {
    taiDanhSach();
  }

  @Override
  public boolean scrollable() {
    return false;
  }

  @Override
  public PageId id() {
    return PageId.RESTORE;
  }

  @Override
  public String title() {
    return "Phục hồi dữ liệu";
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
