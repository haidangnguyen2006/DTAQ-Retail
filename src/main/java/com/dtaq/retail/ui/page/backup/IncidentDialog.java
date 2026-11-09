package com.dtaq.retail.ui.page.backup;

import com.dtaq.retail.controller.IncidentController;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.HealthIncident;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.notify.NotificationService;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.FileSizeFormat;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Frame;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hộp thoại xử lý sự cố sức khỏe dữ liệu do HealthMonitor phát hiện.
 *
 * <p>Quản lý thấy sự cố {@code PENDING} ngay sau khi đăng nhập và có bốn lựa chọn: phục hồi từ bản
 * sao lưu hợp lệ gần nhất (qua xác nhận hai bước), chọn bản khác ở trang Phục hồi dữ liệu, bỏ qua
 * khi dữ liệu đã hợp lệ, hoặc để xử lý sau. Chỉ bản sao lưu {@code SUCCESS} mới được gợi ý.
 *
 * <p>Hộp thoại chỉ hiển thị và chuyển tiếp yêu cầu; mọi thao tác nặng chạy trong
 * {@link SwingWorker} và chỉ cập nhật giao diện trong {@code done()}. Không có SQL/JDBC ở đây.
 */
public final class IncidentDialog extends ModalDialog {

  private static final Logger log = LoggerFactory.getLogger(IncidentDialog.class);

  private static final String KHONG_CO_BAN_SAO_LUU = "Không có bản sao lưu hợp lệ";
  private static final String LOAD_ERROR = "Không thể tải danh sách bản sao lưu";
  private static final String RESTORE_ERROR = "Không thể phục hồi dữ liệu. Vui lòng thử lại";
  private static final String DISMISS_ERROR = "Không thể bỏ qua sự cố. Vui lòng thử lại";
  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

  private final Frame parent;
  private final IncidentController controller;
  private final HealthIncident incident;
  private final Consumer<PageId> onNavigate;

  private final JLabel backupFileValue = valueLabel("Đang tải…");
  private final JLabel backupTimeValue = valueLabel("Đang tải…");
  private final JLabel backupSizeValue = valueLabel("Đang tải…");

  private final DtaqButton restoreBtn = DtaqButton.dangerSoft("Phục hồi bản này");
  private final DtaqButton chonKhacBtn = DtaqButton.secondary("Chọn bản khác");
  private final DtaqButton boQuaBtn = DtaqButton.secondary("Bỏ qua — dữ liệu hợp lệ");
  private final DtaqButton deSauBtn = DtaqButton.secondary("Để sau");

  private BackupRecord banSaoLuu;
  private boolean daTai;

  /**
   * Khởi tạo hộp thoại sự cố dữ liệu.
   *
   * @param parent cửa sổ cha; có thể {@code null}
   * @param controller bộ điều phối sự cố; không được {@code null}
   * @param incident sự cố đang chờ xử lý; không được {@code null}
   * @param onNavigate callback điều hướng sang trang khác; không được {@code null}
   */
  public IncidentDialog(
      Frame parent,
      IncidentController controller,
      HealthIncident incident,
      Consumer<PageId> onNavigate) {
    super(parent, "Sự cố dữ liệu", phuDe(incident), 640);
    this.parent = parent;
    this.controller = Objects.requireNonNull(controller, "controller");
    this.incident = Objects.requireNonNull(incident, "incident");
    this.onNavigate = Objects.requireNonNull(onNavigate, "onNavigate");

    getBody().add(buildContent(), "growx");
    wireActions();
    capNhatActions();
    taiBanSaoLuu();
  }

  private JPanel buildContent() {
    JPanel content = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]14[]12[]"));
    content.setOpaque(false);
    content.add(new InfoCallout(mucDoCallout(), loiKhuyen()), "growx, wrap");
    content.add(buildMeta(), "growx, wrap");
    content.add(buildBackupBox(), "growx");
    return content;
  }

  private JPanel buildMeta() {
    JPanel meta = new JPanel(new MigLayout("fillx, insets 0", "[130!][grow]", "[]8[]8[]"));
    meta.setOpaque(false);
    addRow(meta, "Chi tiết", giaTriHoacGach(incident.detail()));
    addRow(meta, "Phát hiện lúc", thoiDiem(incident.detectedAt()));
    addRow(meta, "Trạng thái", tenTrangThai(incident.trangThai()));
    return meta;
  }

  private JPanel buildBackupBox() {
    JPanel box = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]10[]"));
    box.setOpaque(false);

    JLabel heading = new JLabel("Bản sao lưu gợi ý");
    heading.setFont(DtaqFonts.LABEL);
    heading.setForeground(DtaqColors.TEXT);
    box.add(heading, "growx, wrap");

    JPanel rows = new JPanel(new MigLayout("fillx, insets 0", "[130!][grow]", "[]8[]8[]"));
    rows.setOpaque(false);
    addRow(rows, "Tệp", backupFileValue);
    addRow(rows, "Thời điểm", backupTimeValue);
    addRow(rows, "Kích thước", backupSizeValue);
    box.add(rows, "growx");
    return box;
  }

  private void wireActions() {
    restoreBtn.addActionListener(e -> phucHoiBanNay());
    chonKhacBtn.addActionListener(e -> chonBanKhac());
    boQuaBtn.addActionListener(e -> boQuaSuCo());
    deSauBtn.addActionListener(e -> deSau());
  }

  /** Tải bản sao lưu mới nhất ở luồng nền; chỉ cập nhật nhãn và nút trong {@code done()}. */
  private void taiBanSaoLuu() {
    new SwingWorker<BackupRecord, Void>() {
      @Override
      protected BackupRecord doInBackground() {
        List<BackupRecord> danhSach = controller.danhSachBackup();
        if (danhSach == null) {
          return null;
        }
        for (BackupRecord banGhi : danhSach) {
          if (banGhi != null && banGhi.thanhCong()) {
            return banGhi;
          }
        }
        return null;
      }

      @Override
      protected void done() {
        try {
          hienThiBanSaoLuu(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          hienThiKhongCoBanSaoLuu(LOAD_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không tải được danh sách bản sao lưu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          hienThiKhongCoBanSaoLuu(LOAD_ERROR);
        } finally {
          daTai = true;
          capNhatActions();
        }
      }
    }.execute();
  }

  private void hienThiBanSaoLuu(BackupRecord banGhi) {
    banSaoLuu = banGhi;
    if (banGhi == null) {
      hienThiKhongCoBanSaoLuu(KHONG_CO_BAN_SAO_LUU);
      return;
    }
    backupFileValue.setText(banGhi.fileName());
    backupFileValue.setToolTipText(banGhi.filePath().toString());
    backupFileValue.setForeground(DtaqColors.TEXT);
    backupTimeValue.setText(thoiDiem(banGhi.createdAt()));
    backupTimeValue.setToolTipText(backupTimeValue.getText());
    backupSizeValue.setText(FileSizeFormat.format(banGhi.fileSize()));
  }

  private void hienThiKhongCoBanSaoLuu(String thongBao) {
    banSaoLuu = null;
    backupFileValue.setText(thongBao);
    backupFileValue.setToolTipText(null);
    backupFileValue.setForeground(DtaqColors.TEXT_MUTED);
    backupTimeValue.setText("—");
    backupTimeValue.setToolTipText(null);
    backupSizeValue.setText("—");
  }

  /** Chọn danh sách nút theo tình trạng bản sao lưu đã tải; luôn giữ đủ bốn lựa chọn. */
  private void capNhatActions() {
    boolean coBanSaoLuu = daTai && banSaoLuu != null;
    restoreBtn.setEnabled(coBanSaoLuu);
    restoreBtn.setToolTipText(coBanSaoLuu ? null : "Không có bản sao lưu hợp lệ để phục hồi");
    setActions(restoreBtn, chonKhacBtn, boQuaBtn, deSauBtn);
    setPrimaryAction(deSauBtn);
  }

  private void phucHoiBanNay() {
    BackupRecord chon = banSaoLuu;
    if (chon == null) {
      Toast.error(this, KHONG_CO_BAN_SAO_LUU);
      return;
    }
    dispose();
    new RestoreConfirmDialog(parent, chon, () -> chayPhucHoi(parent, chon)).setVisible(true);
  }

  private void chonBanKhac() {
    onNavigate.accept(PageId.RESTORE);
    dispose();
  }

  private void boQuaSuCo() {
    dispose();
    ConfirmDialog.confirm(parent, "Bỏ qua sự cố",
        "Baseline sẽ được cập nhật theo số liệu hiện tại và sao lưu tự động chạy lại. Tiếp tục?",
        "Bỏ qua", () -> chayBoQua(parent));
  }

  /**
   * Kiểm tra lại dữ liệu và cập nhật baseline ở luồng nền sau khi Quản lý xác nhận bỏ qua sự cố;
   * chỉ cập nhật giao diện trong {@code done()}.
   *
   * @param parent cửa sổ cha cho hộp thoại tiến trình và thông báo; có thể {@code null}
   */
  private void chayBoQua(Frame parent) {
    ProgressDialog progress = new ProgressDialog(parent, "Đang cập nhật baseline");
    progress.setStage("Đang kiểm tra lại dữ liệu và cập nhật baseline...");

    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() {
        controller.boQuaHopLe(incident);
        return null;
      }

      @Override
      protected void done() {
        progress.finish();
        try {
          get();
          Toast.success(parent, "Đã bỏ qua và cập nhật baseline");
          NotificationService.thongBao("Sự cố dữ liệu", "Đã bỏ qua và cập nhật baseline");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(parent, DISMISS_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không bỏ qua được sự cố (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          Toast.error(parent, thongBaoLoi(cause, DISMISS_ERROR));
        }
      }
    }.execute();

    progress.setVisible(true);
  }

  private void deSau() {
    controller.deSau(incident);
    dispose();
  }

  /**
   * Phục hồi dữ liệu từ bản sao lưu đã chọn ở luồng nền, hiển thị tiến trình và kết quả trên giao
   * diện. Chỉ gọi sau khi người dùng hoàn tất xác nhận hai bước.
   *
   * @param parent cửa sổ cha cho hộp thoại tiến trình và thông báo; có thể {@code null}
   * @param backup bản sao lưu nguồn; không được {@code null}
   */
  private void chayPhucHoi(Frame parent, BackupRecord backup) {
    ProgressDialog progress = new ProgressDialog(parent, "Đang phục hồi dữ liệu");
    progress.setStage("Đang phục hồi dữ liệu từ " + backup.fileName() + "...");

    new SwingWorker<BackupRecord, Void>() {
      @Override
      protected BackupRecord doInBackground() {
        return controller.phucHoi(incident, backup);
      }

      @Override
      protected void done() {
        progress.finish();
        try {
          get();
          Toast.success(parent, "Đã phục hồi từ " + backup.fileName());
          NotificationService.thongBao(
              "Phục hồi dữ liệu", "Đã phục hồi từ " + backup.fileName());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(parent, RESTORE_ERROR);
          NotificationService.thongBao("Phục hồi thất bại", RESTORE_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Không thể phục hồi dữ liệu (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          String thongBao = thongBaoLoi(cause, RESTORE_ERROR);
          Toast.error(parent, thongBao);
          NotificationService.thongBao("Phục hồi thất bại", thongBao);
        }
      }
    }.execute();

    progress.setVisible(true);
  }

  private InfoCallout.Kind mucDoCallout() {
    return incident.severity() == HealthIncident.Severity.CRITICAL
        ? InfoCallout.Kind.DANGER : InfoCallout.Kind.WARNING;
  }

  private String loiKhuyen() {
    if (incident.severity() == HealthIncident.Severity.CRITICAL) {
      return "Dữ liệu đang ở mức nghiêm trọng. Hãy phục hồi từ bản sao lưu hợp lệ gần nhất, hoặc "
          + "chọn bản khác ở trang Phục hồi dữ liệu trước khi tiếp tục bán hàng.";
    }
    return "Số liệu giảm bất thường. Hãy kiểm tra lại dữ liệu trước khi quyết định phục hồi hoặc "
        + "bỏ qua sự cố.";
  }

  private static String phuDe(HealthIncident incident) {
    if (incident != null && incident.severity() == HealthIncident.Severity.CRITICAL) {
      return "Dữ liệu nghiêm trọng — cần Quản lý xử lý";
    }
    return "Cần Quản lý xem xét";
  }

  private static JLabel valueLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(DtaqFonts.BODY_14);
    label.setForeground(DtaqColors.TEXT);
    return label;
  }

  private static void addRow(JPanel panel, String nhan, String giaTri) {
    JLabel value = valueLabel(giaTri);
    value.setToolTipText(giaTri);
    addRow(panel, nhan, value);
  }

  private static void addRow(JPanel panel, String nhan, JLabel value) {
    JLabel label = new JLabel(nhan);
    label.setFont(DtaqFonts.LABEL);
    label.setForeground(DtaqColors.TEXT_MUTED);
    panel.add(label, "aligny top");
    panel.add(value, "growx, wrap");
  }

  private static String giaTriHoacGach(String giaTri) {
    return giaTri == null || giaTri.isBlank() ? "—" : giaTri;
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

  private static String tenTrangThai(HealthIncident.TrangThai trangThai) {
    if (trangThai == null) {
      return "—";
    }
    return switch (trangThai) {
      case PENDING -> "Đang chờ xử lý";
      case RESOLVED_RESTORED -> "Đã phục hồi thủ công";
      case AUTO_RESTORED -> "Đã tự động phục hồi";
      case DISMISSED -> "Đã bỏ qua";
      case FAILED -> "Không thể phục hồi";
    };
  }
}
