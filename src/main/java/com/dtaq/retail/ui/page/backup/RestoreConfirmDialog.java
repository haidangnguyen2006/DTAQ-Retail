package com.dtaq.retail.ui.page.backup;

import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.FileSizeFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.CardLayout;
import java.awt.Frame;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại xác nhận hai bước trước khi phục hồi toàn bộ cơ sở dữ liệu (FR016).
 *
 * <p>Bước 1 tóm tắt tệp sao lưu được chọn và cảnh báo chung. Bước 2 nhấn mạnh dữ liệu sau mốc sao
 * lưu sẽ mất; nút "Phục hồi" chỉ bật khi người dùng tự đánh dấu ô xác nhận. Callback chỉ chạy khi
 * người dùng bấm "Phục hồi"; đóng hộp thoại bằng Esc hoặc nút × sẽ hủy thao tác.
 */
public final class RestoreConfirmDialog extends ModalDialog {

  private static final String BUOC_1 = "buoc-1";
  private static final String BUOC_2 = "buoc-2";
  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

  private final CardLayout stepLayout = new CardLayout();
  private final JPanel stepPanel = new JPanel(stepLayout);
  private final JCheckBox hieuMatDuLieu =
      new JCheckBox("Tôi hiểu dữ liệu sau mốc backup sẽ bị mất");
  private final DtaqButton restoreBtn = DtaqButton.dangerSoft("Phục hồi");
  private final Runnable onConfirm;

  /**
   * Khởi tạo hộp thoại xác nhận phục hồi.
   *
   * @param parent cửa sổ cha; có thể {@code null}
   * @param banGhi bản sao lưu được chọn; không được {@code null}
   * @param onConfirm hành động chạy sau khi người dùng xác nhận đủ hai bước
   */
  public RestoreConfirmDialog(Frame parent, BackupRecord banGhi, Runnable onConfirm) {
    super(parent, "Xác nhận phục hồi dữ liệu",
        "Xác nhận hai bước trước khi phục hồi toàn bộ cơ sở dữ liệu.", 560);
    Objects.requireNonNull(banGhi, "banGhi");
    this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");

    stepPanel.setOpaque(false);
    stepPanel.add(buildStep1(banGhi), BUOC_1);
    stepPanel.add(buildStep2(), BUOC_2);
    getBody().add(stepPanel, "growx");

    restoreBtn.setEnabled(false);
    restoreBtn.addActionListener(e -> {
      dispose();
      onConfirm.run();
    });
    hieuMatDuLieu.addActionListener(e -> restoreBtn.setEnabled(hieuMatDuLieu.isSelected()));

    showStep1();
  }

  private JPanel buildStep1(BackupRecord banGhi) {
    JPanel panel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]16[]"));
    panel.setOpaque(false);
    panel.add(buildSummary(banGhi), "growx, wrap");
    panel.add(new InfoCallout(InfoCallout.Kind.WARNING,
        "Trước khi phục hồi, hệ thống tự tạo một bản sao lưu an toàn của dữ liệu hiện tại. "
            + "Chỉ tiếp tục khi bạn đã chọn đúng tệp sao lưu cần dùng."), "growx");
    return panel;
  }

  private static JPanel buildSummary(BackupRecord banGhi) {
    JPanel summary = new JPanel(new MigLayout("fillx, insets 0", "[130!][grow]", "[]8[]8[]"));
    summary.setOpaque(false);
    addSummaryRow(summary, "Tệp", banGhi.fileName());
    addSummaryRow(summary, "Thời điểm", thoiDiem(banGhi.createdAt()));
    addSummaryRow(summary, "Kích thước", FileSizeFormat.format(banGhi.fileSize()));
    return summary;
  }

  private static void addSummaryRow(JPanel panel, String nhan, String giaTri) {
    JLabel label = new JLabel(nhan);
    label.setFont(DtaqFonts.LABEL);
    label.setForeground(DtaqColors.TEXT_MUTED);
    JLabel value = new JLabel(giaTri == null ? "" : giaTri);
    value.setFont(DtaqFonts.BODY_14);
    value.setForeground(DtaqColors.TEXT);
    value.setToolTipText(giaTri);
    panel.add(label, "aligny top");
    panel.add(value, "growx, wrap");
  }

  private JPanel buildStep2() {
    JPanel panel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]16[]"));
    panel.setOpaque(false);
    panel.add(new InfoCallout(InfoCallout.Kind.DANGER,
        "Toàn bộ dữ liệu hiện tại sẽ bị thay thế bằng dữ liệu trong bản sao lưu. Hóa đơn, phiếu "
            + "trả, phiếu đổi và sản phẩm được tạo sau mốc backup sẽ bị mất và không thể khôi "
            + "phục, trừ khi bạn phục hồi lại từ bản sao lưu an toàn."), "growx, wrap");

    hieuMatDuLieu.setFont(DtaqFonts.BODY_14);
    hieuMatDuLieu.setForeground(DtaqColors.TEXT);
    hieuMatDuLieu.setOpaque(false);
    hieuMatDuLieu.setFocusPainted(false);
    panel.add(hieuMatDuLieu, "growx");
    return panel;
  }

  private void showStep1() {
    stepLayout.show(stepPanel, BUOC_1);
    DtaqButton cancel = DtaqButton.secondary("Hủy");
    cancel.addActionListener(e -> dispose());
    DtaqButton next = DtaqButton.primary("Tiếp tục");
    next.addActionListener(e -> showStep2());
    setActions(cancel, next);
    setPrimaryAction(next);
  }

  private void showStep2() {
    stepLayout.show(stepPanel, BUOC_2);
    DtaqButton back = DtaqButton.secondary("Quay lại");
    back.addActionListener(e -> showStep1());
    restoreBtn.setEnabled(hieuMatDuLieu.isSelected());
    setActions(back, restoreBtn);
    setPrimaryAction(restoreBtn);
  }

  private static String thoiDiem(Instant instant) {
    if (instant == null) {
      return "";
    }
    return DateFormats.formatDateTime(LocalDateTime.ofInstant(instant, MUI_GIO));
  }
}
