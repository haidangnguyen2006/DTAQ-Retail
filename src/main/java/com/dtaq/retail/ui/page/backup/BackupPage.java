package com.dtaq.retail.ui.page.backup;

import com.dtaq.retail.config.BackupConfig;
import com.dtaq.retail.controller.BackupController;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.notify.NotificationService;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.InputFilters;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.FileSizeFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
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
 * Trang "Sao lưu dữ liệu" (PageId.BACKUP, FR015).
 *
 * <p>Trang hiển thị cấu hình sao lưu, cho phép Quản lý chỉnh giờ chạy và chu kỳ tự động, tạo ngay
 * một bản sao lưu toàn bộ cơ sở dữ liệu và theo dõi lịch sử các bản sao lưu trên đĩa. Mọi thao tác
 * nặng chạy trong {@link SwingWorker}; giao diện chỉ được cập nhật trong {@code done()}. Không có
 * SQL/JDBC ở đây.
 */
public final class BackupPage implements Page {

  private static final Logger log = LoggerFactory.getLogger(BackupPage.class);

  private static final String LOAD_ERROR = "Không thể tải danh sách bản sao lưu. Vui lòng thử lại";
  private static final String BACKUP_ERROR = "Không thể tạo bản sao lưu. Vui lòng thử lại";
  private static final String SAVE_CONFIG_ERROR = "Không thể lưu cấu hình. Vui lòng thử lại";
  private static final String SAVE_CONFIG_DONE = "Đã lưu cấu hình sao lưu";
  private static final String SAI_DINH_DANG_GIO = "Giờ phải có dạng HH:mm";
  private static final String SAI_CHU_KY = "Chu kỳ phải từ 1 đến 168 giờ";
  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter DINH_DANG_GIO =
      DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

  private final BackupController controller;
  private final JPanel view;
  private final DtaqButton backupBtn = DtaqButton.primary("Sao lưu ngay", "download");
  private final DtaqButton saveConfigBtn = DtaqButton.secondary("Lưu cấu hình", "check");
  private final StatusPill countPill = StatusPill.neutral("0 bản sao lưu");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Tệp", "Thời điểm", "Kích thước", "Loại", "Trạng thái"});
  private final DataTable dataTable = new DataTable(tableModel, 48);
  private final StatefulView tableState;

  private FormField runTimeField;
  private FormField intervalField;

  private int loadRequest;

  /**
   * Khởi tạo trang sao lưu dữ liệu.
   *
   * @param controller bộ điều phối sao lưu/phục hồi; không được {@code null}
   */
  public BackupPage(BackupController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][][grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        "Sao lưu dữ liệu",
        "Tạo bản sao lưu toàn bộ cơ sở dữ liệu theo FR015 và theo dõi các bản sao lưu hiện có.");
    backupBtn.addActionListener(e -> saoLuuNgay());
    saveConfigBtn.addActionListener(e -> luuCauHinh());
    header.setTrailing(backupBtn);
    view.add(header, "growx, wrap");

    view.add(buildConfigCard(), "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(260, 170, 110, 130, 120);
    tableState = new StatefulView(
        dataTable,
        "download",
        "Chưa có bản sao lưu",
        "Bấm “Sao lưu ngay” để tạo bản sao lưu đầu tiên.");
    tableState.setOnRetry(this::taiDanhSach);

    Card historyCard = Card.section(
        "Lịch sử sao lưu", "Các tệp sao lưu trên đĩa, mới nhất trước.", countPill);
    historyCard.setBody(tableState);
    view.add(historyCard, "grow, push");
  }

  private Card buildConfigCard() {
    FormField folderField = readOnlyField(
        "Thư mục sao lưu", BackupConfig.thuMucBackup().toAbsolutePath().toString());
    FormField keepDaysField = readOnlyField(
        "Số ngày giữ bản sao lưu", BackupConfig.soNgayGiu() + " ngày");

    runTimeField = FormField.text("Giờ chạy tự động", "HH:mm", "clock");
    intervalField = FormField.text("Chu kỳ (giờ)", "1–168", null);
    InputFilters.gioPhut(runTimeField.getTextField());
    InputFilters.chiChoSo(intervalField.getTextField(), 3);
    napLaiCauHinh();

    JPanel body = new JPanel(new MigLayout(
        "fillx, insets 0, gapy " + DtaqSpacing.FORM_FIELD_GAP, "[grow][grow]", "[]"));
    body.setOpaque(false);
    body.add(folderField, "growx, span 2, wrap");
    body.add(keepDaysField, "growx");
    body.add(runTimeField, "growx, wrap");
    body.add(intervalField, "growx, span 2, wrap");

    if (BackupConfig.biGhiDeBoiMoiTruong()) {
      body.add(new InfoCallout(InfoCallout.Kind.WARNING,
          "Giờ chạy/chu kỳ đang bị ghi đè bởi biến môi trường hoặc -D; thay đổi trên giao diện "
              + "sẽ được lưu nhưng không có hiệu lực."),
          "growx, span 2, wrap");
    }

    body.add(saveConfigBtn, "align right, span 2, wrap");
    body.add(new InfoCallout(InfoCallout.Kind.INFO,
        "Bản sao lưu chứa toàn bộ cơ sở dữ liệu, bao gồm mã băm mật khẩu BCrypt của tài khoản. "
            + "Không chia sẻ tệp sao lưu cho người khác hoặc ra ngoài hệ thống."),
        "growx, span 2");

    Card card = Card.section(
        "Cấu hình sao lưu",
        "Giờ chạy và chu kỳ có thể sửa rồi bấm “Lưu cấu hình”; các khóa khác đọc từ tệp cấu hình.",
        null);
    card.setBody(body);
    return card;
  }

  /** Nạp lại giờ chạy và chu kỳ đang có hiệu lực vào hai ô nhập. */
  private void napLaiCauHinh() {
    runTimeField.setText(DINH_DANG_GIO.format(BackupConfig.gioChay()));
    intervalField.setText(Integer.toString(BackupConfig.chuKyGio()));
  }

  /** Giá trị cấu hình có hiệu lực sau khi lưu, đọc ở luồng nền để không chạm tệp trên EDT. */
  private record CauHinhMoi(LocalTime gio, int chuKy) {
  }

  /** Lưu giờ chạy và chu kỳ ở luồng nền; không ghi tệp trên EDT. */
  private void luuCauHinh() {
    runTimeField.setError(null);
    intervalField.setError(null);

    LocalTime gioChay;
    try {
      gioChay = LocalTime.parse(runTimeField.getText().trim(), DINH_DANG_GIO);
    } catch (DateTimeParseException e) {
      runTimeField.setError(SAI_DINH_DANG_GIO);
      return;
    }

    int chuKy;
    try {
      chuKy = Integer.parseInt(intervalField.getText().trim());
    } catch (NumberFormatException e) {
      intervalField.setError(SAI_CHU_KY);
      return;
    }
    if (chuKy < 1 || chuKy > 168) {
      intervalField.setError(SAI_CHU_KY);
      return;
    }

    saveConfigBtn.setBusy(true);
    new SwingWorker<CauHinhMoi, Void>() {
      @Override
      protected CauHinhMoi doInBackground() {
        controller.luuCauHinh(gioChay, chuKy);
        return new CauHinhMoi(BackupConfig.gioChay(), BackupConfig.chuKyGio());
      }

      @Override
      protected void done() {
        saveConfigBtn.setBusy(false);
        try {
          CauHinhMoi moi = get();
          runTimeField.setText(DINH_DANG_GIO.format(moi.gio()));
          intervalField.setText(Integer.toString(moi.chuKy()));
          Toast.success(view, SAVE_CONFIG_DONE);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, SAVE_CONFIG_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể lưu cấu hình sao lưu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          Toast.error(view, thongBaoLuu(cause));
        }
      }
    }.execute();
  }

  private static String thongBaoLuu(Throwable cause) {
    if (cause instanceof SecurityException || cause instanceof IllegalArgumentException) {
      String message = cause.getMessage();
      if (message != null && !message.isBlank()) {
        return message;
      }
    }
    return SAVE_CONFIG_ERROR;
  }

  private static FormField readOnlyField(String label, String value) {
    FormField field = FormField.text(label, null, null);
    field.setText(value);
    field.getTextField().setEditable(false);
    field.getTextField().setEnabled(false);
    field.getTextField().setFocusable(false);
    field.getTextField().setToolTipText(value);
    return field;
  }

  /**
   * Tải danh sách bản sao lưu ở luồng nền; chỉ cập nhật giao diện trong {@code done()}.
   */
  public void taiDanhSach() {
    int request = ++loadRequest;
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
        }
      }
    }.execute();
  }

  private void renderDanhSach(List<BackupRecord> danhSach) {
    tableModel.setRowCount(0);
    int soLuong = danhSach == null ? 0 : danhSach.size();
    countPill.setText(soLuong + " bản sao lưu");

    if (soLuong == 0) {
      tableState.showEmpty();
      return;
    }

    for (BackupRecord banGhi : danhSach) {
      tableModel.addRow(new Object[] {
          banGhi.fileName(),
          thoiDiem(banGhi.createdAt()),
          FileSizeFormat.format(banGhi.fileSize()),
          tenLoai(banGhi.loai()),
          tenTrangThai(banGhi.trangThai())
      });
    }
    tableState.showContent();
  }

  private void saoLuuNgay() {
    backupBtn.setEnabled(false);
    ProgressDialog progress = new ProgressDialog(
        ConfirmDialog.frameOf(view), "Đang sao lưu dữ liệu");
    progress.setStage("Đang tạo bản sao lưu...");

    new SwingWorker<BackupRecord, Void>() {
      @Override
      protected BackupRecord doInBackground() {
        return controller.saoLuuNgay();
      }

      @Override
      protected void done() {
        progress.finish();
        backupBtn.setEnabled(true);
        try {
          BackupRecord banGhi = get();
          Toast.success(view, "Đã tạo bản sao lưu");
          NotificationService.thongBao(
              "Sao lưu dữ liệu", "Đã tạo bản sao lưu " + banGhi.fileName());
          taiDanhSach();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, BACKUP_ERROR);
          NotificationService.thongBao("Sao lưu thất bại", BACKUP_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể tạo bản sao lưu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          String thongBao = thongBaoLoi(cause, BACKUP_ERROR);
          Toast.error(view, thongBao);
          NotificationService.thongBao("Sao lưu thất bại", thongBao);
        }
      }
    }.execute();

    progress.setVisible(true);
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

  private static String tenLoai(BackupRecord.LoaiSaoLuu loai) {
    if (loai == null) {
      return "—";
    }
    return switch (loai) {
      case AUTO -> "Tự động";
      case MANUAL -> "Thủ công";
      case SAFETY -> "An toàn";
      case AUTO_RESTORE -> "Tự phục hồi";
      case MANUAL_RESTORE -> "Phục hồi";
    };
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
    return PageId.BACKUP;
  }

  @Override
  public String title() {
    return "Sao lưu dữ liệu";
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
