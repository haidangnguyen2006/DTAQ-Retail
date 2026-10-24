package com.dtaq.retail.ui.page.home;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.ActivityItem;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.AppInfo;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Trang chủ Quản lý (ManagerHomePage, Figma frame 1:1200; UC018).
 *
 * <p>Chỉ hiển thị dữ liệu do controller cung cấp: danh sách phiếu chờ duyệt, nhật ký hoạt động và
 * cảnh báo. Quyết định phê duyệt/từ chối được chuyển ra qua {@link #setOnApprove}/{@link #setOnReject};
 * việc kiểm tra ngưỡng và ghi quyết định thuộc controller.
 */
public class ManagerHomePage implements Page {

  public record ApprovalRow(
      String maPhieu,
      String maHDGoc,
      String tenKH,
      String sdtKH,
      long hoanDuKien,
      String nhanVienLap,
      String lyDo
  ) {}

  /** Một dòng nhật ký hoạt động (tiêu đề và chi tiết như thời gian, kết quả). */
  public record ActivityRow(String tieuDe, String chiTiet) {}

  /** Một cảnh báo quan trọng. */
  public record AlertRow(String tieuDe, String chiTiet) {}

  private static final int CLOCK_REFRESH_MS = 30_000;

  private final NhanVien nhanVien;
  private final JPanel view;

  private final JLabel approvalSubtitle = new JLabel("Các yêu cầu đổi trả cần quản lý xác nhận");
  private final StatusPill totalRefundPill = StatusPill.neutral("Tổng hoàn dự kiến 0 VNĐ");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã phiếu", "Mã HĐ gốc", "Khách hàng & SĐT", "Hoàn dự kiến", "Nhân viên lập", "Lý do đổi/trả", "Thao tác"}, 6);
  private final DataTable dataTable = new DataTable(tableModel, 64);
  private final StatefulView approvalState;
  private final List<ApprovalRow> approvals = new ArrayList<>();

  private final JPanel activityList = new JPanel(new MigLayout("fillx, insets 0, gapy 14", "[grow]", "[]"));
  private final StatefulView activityState;
  private final JPanel alertList = new JPanel(new MigLayout("fillx, insets 0, gapy 14", "[grow]", "[]"));
  private final StatefulView alertState;

  private final JLabel clockLabel = new JLabel();
  private final Timer clockTimer;

  private BiConsumer<String, String> onApproveListener;
  private BiConsumer<String, String> onRejectListener;

  public ManagerHomePage(NhanVien nhanVien) {
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        "Trang chủ Quản lý", "Tổng hợp các yêu cầu phê duyệt và tình trạng vận hành hệ thống.");
    header.setTrailing(StatusPill.neutral(DateFormats.formatDate(LocalDate.now())));
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setCodeColumn(1);
    dataTable.setTwoLineColumn(2);
    dataTable.setMoneyColumn(3);
    dataTable.setColumnWidths(120, 120, 210, 130, 150, 220, 200);
    dataTable.setActionColumn(6,
        RowAction.primary("Phê duyệt", row -> decide(true, row)),
        RowAction.danger("Từ chối", row -> decide(false, row)));
    approvalState = new StatefulView(
        dataTable, "circle-check", "Chưa có yêu cầu chờ duyệt", "Các phiếu đổi/trả cần phê duyệt sẽ hiển thị tại đây.");
    view.add(buildApprovalCard(), "growx, wrap");

    activityState = new StatefulView(activityList, "clock", "Chưa có hoạt động", "Các sự kiện gần nhất sẽ hiển thị tại đây.");
    alertState = new StatefulView(alertList, "circle-check", "Chưa có cảnh báo", "Các cảnh báo quan trọng sẽ hiển thị tại đây.");
    activityList.setOpaque(false);
    alertList.setOpaque(false);

    JPanel twoColRow = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]16[grow,fill]", "[grow,fill]"));
    twoColRow.setOpaque(false);
    Card activityCard = Card.section("Nhật ký hoạt động", null, null);
    activityCard.setBody(activityState);
    Card alertsCard = Card.section("Cảnh báo quan trọng", null, null);
    alertsCard.setBody(alertState);
    twoColRow.add(activityCard);
    twoColRow.add(alertsCard);
    view.add(twoColRow, "growx, wrap");

    JPanel infoRow = new JPanel(new MigLayout("fillx, insets 0", "[grow,fill]16[grow,fill]", "[]"));
    infoRow.setOpaque(false);
    infoRow.add(buildClockCard());
    infoRow.add(buildVersionCard());
    view.add(infoRow, "growx");

    refreshClock();
    clockTimer = new Timer(CLOCK_REFRESH_MS, e -> refreshClock());
  }

  private Card buildApprovalCard() {
    Card card = Card.standard();

    JPanel header = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    header.setOpaque(false);
    JPanel titleBlock = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    titleBlock.setOpaque(false);
    JLabel title = new JLabel("Danh sách chờ phê duyệt");
    title.setFont(DtaqFonts.CARD_TITLE);
    title.setForeground(DtaqColors.TEXT);
    titleBlock.add(title, "wrap");
    approvalSubtitle.setFont(DtaqFonts.BODY_13);
    approvalSubtitle.setForeground(DtaqColors.TEXT_MUTED);
    titleBlock.add(approvalSubtitle);
    header.add(titleBlock, "align left");
    header.add(totalRefundPill, "align right");

    card.add(header, "growx, wrap");
    card.setBody(approvalState);
    return card;
  }

  private Card buildClockCard() {
    Card card = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 10, DtaqSpacing.RADIUS_L);
    JPanel row = new JPanel(new MigLayout("insets 0, gapx 12", "[][]", "[center]"));
    row.setOpaque(false);
    clockLabel.setFont(DtaqFonts.BODY_14);
    clockLabel.setForeground(DtaqColors.TEXT);
    row.add(new ActivityItem("clock", false, "Thời gian hiện tại", null));
    row.add(clockLabel);
    card.add(row, "growx");
    return card;
  }

  private Card buildVersionCard() {
    Card card = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 10, DtaqSpacing.RADIUS_L);
    card.add(new ActivityItem("package", false, "Phiên bản hệ thống", "DTAQ Retail · Version " + AppInfo.version()));
    return card;
  }

  private void refreshClock() {
    clockLabel.setText(DateFormats.formatDateTimeBullet(LocalDateTime.now()));
  }

  // ---------------------------------------------------------------- public API

  /** Hiển thị danh sách phiếu chờ duyệt; danh sách rỗng hoặc {@code null} hiện trạng thái rỗng. */
  public void renderApprovals(List<ApprovalRow> newApprovals) {
    approvals.clear();
    tableModel.setRowCount(0);
    if (newApprovals == null || newApprovals.isEmpty()) {
      approvalSubtitle.setText("Các yêu cầu đổi trả cần quản lý xác nhận");
      totalRefundPill.setText("Tổng hoàn dự kiến 0 VNĐ");
      approvalState.showEmpty("circle-check", "Không có yêu cầu chờ duyệt",
          "Tất cả các phiếu đổi/trả đã được xử lý hoàn tất.");
      return;
    }
    approvals.addAll(newApprovals);
    long total = 0;
    for (ApprovalRow r : newApprovals) {
      total += r.hoanDuKien();
      tableModel.addRow(new Object[] {
          r.maPhieu(), r.maHDGoc(), new String[] {r.tenKH(), r.sdtKH()},
          r.hoanDuKien(), r.nhanVienLap(), r.lyDo(), ""});
    }
    approvalSubtitle.setText(newApprovals.size() + " phiếu chưa được xử lý");
    totalRefundPill.setText("Tổng hoàn dự kiến " + MoneyFormat.formatVnd(total));
    approvalState.showContent();
  }

  public void showLoading() {
    approvalState.showLoading();
  }

  public void showError(String message) {
    approvalState.showError(message);
  }

  public void renderActivities(List<ActivityRow> rows) {
    activityList.removeAll();
    if (rows == null || rows.isEmpty()) {
      activityState.showEmpty();
      return;
    }
    for (ActivityRow row : rows) {
      activityList.add(new ActivityItem("clock", false, row.tieuDe(), row.chiTiet()), "growx, wrap");
    }
    activityState.showContent();
  }

  public void renderAlerts(List<AlertRow> rows) {
    alertList.removeAll();
    if (rows == null || rows.isEmpty()) {
      alertState.showEmpty("circle-check", "Không có cảnh báo", "Hiện không có cảnh báo quan trọng nào.");
      return;
    }
    for (AlertRow row : rows) {
      alertList.add(new ActivityItem("alert-circle", true, row.tieuDe(), row.chiTiet()), "growx, wrap");
    }
    alertState.showContent();
  }

  /** Nhận (mã phiếu, ý kiến phê duyệt) khi quản lý xác nhận phê duyệt. */
  public void setOnApprove(BiConsumer<String, String> listener) {
    this.onApproveListener = listener;
  }

  /** Nhận (mã phiếu, lý do từ chối) khi quản lý xác nhận từ chối; lý do luôn khác rỗng. */
  public void setOnReject(BiConsumer<String, String> listener) {
    this.onRejectListener = listener;
  }

  // ---------------------------------------------------------------- hành vi

  private void decide(boolean approve, int modelRow) {
    BiConsumer<String, String> listener = approve ? onApproveListener : onRejectListener;
    if (listener == null || modelRow < 0 || modelRow >= approvals.size()) {
      return;
    }
    Frame frame = ConfirmDialog.frameOf(view);
    new ApprovalDecisionDialog(frame, approve, approvals.get(modelRow).maPhieu(), listener).setVisible(true);
  }

  // ---------------------------------------------------------------- Page

  @Override
  public void onShow() {
    refreshClock();
    clockTimer.start();
  }

  @Override
  public void onHide() {
    clockTimer.stop();
  }

  @Override
  public PageId id() {
    return PageId.MANAGER_HOME;
  }

  @Override
  public String title() {
    return "Trang chủ Quản lý";
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
