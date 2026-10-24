package com.dtaq.retail.ui.page.home;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.ActivityItem;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.EmptyState;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.AppInfo;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Trang chủ Quản lý (ManagerHomePage) theo Figma frame 1:1200.
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

  private final NhanVien nhanVien;
  private final JPanel view;
  private final Card approvalCard;
  private final DataTable dataTable;
  private final DefaultTableModel tableModel;
  private final JLabel totalRefundLabel = new JLabel("Tổng hoàn dự kiến 0 VNĐ");
  private final JPanel activityPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]12[]"));
  private final JPanel alertPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]12[]"));

  private Consumer<String> onApproveListener;
  private Consumer<String> onRejectListener;

  public ManagerHomePage(NhanVien nhanVien) {
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"
    ));
    view.setBackground(DtaqColors.BG_APP);

    // 1. Header
    PageHeader header = new PageHeader("Trang chủ Quản lý", "Tổng hợp các yêu cầu phê duyệt và tình trạng vận hành hệ thống.");
    DtaqButton dateButton = DtaqButton.secondary(DateFormats.formatDate(LocalDate.now()), "calendar-days");
    header.setTrailing(dateButton);
    view.add(header, "growx, wrap");

    // 2. Approval List Card
    approvalCard = new Card(DtaqSpacing.CARD_PADDING_KPI, 16);
    JPanel approvalHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    approvalHeader.setOpaque(false);

    JPanel titleBlock = new JPanel(new MigLayout("insets 0", "[]", "[]4[]"));
    titleBlock.setOpaque(false);
    JLabel title = new JLabel("Danh sách chờ phê duyệt");
    title.setFont(DtaqFonts.SECTION_TITLE_STRONG);
    title.setForeground(DtaqColors.TEXT);
    titleBlock.add(title, "wrap");

    JLabel subtitle = new JLabel("Các yêu cầu đổi trả cần quản lý xác nhận");
    subtitle.setFont(DtaqFonts.BODY_13);
    subtitle.setForeground(DtaqColors.TEXT_MUTED);
    titleBlock.add(subtitle);
    approvalHeader.add(titleBlock, "align left");

    JPanel refundBadge = new JPanel(new MigLayout("insets 8 12 8 12", "[]", "[]")) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.BG_APP);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_M, DtaqSpacing.RADIUS_M));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    refundBadge.setOpaque(false);
    totalRefundLabel.setFont(DtaqFonts.BODY_13_MEDIUM);
    totalRefundLabel.setForeground(DtaqColors.TEXT);
    refundBadge.add(totalRefundLabel);
    approvalHeader.add(refundBadge, "align right");

    approvalCard.add(approvalHeader, "growx, wrap");

    String[] columns = {"Mã phiếu", "Mã HĐ gốc", "Khách hàng & SĐT", "Hoàn dự kiến", "Nhân viên lập", "Lý do đổi/trả", "Thao tác"};
    tableModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    dataTable = new DataTable(tableModel, 64);
    dataTable.setCodeColumn(0);
    dataTable.setCodeColumn(1);
    dataTable.setTwoLineColumn(2);
    dataTable.setMoneyColumn(3);
    dataTable.setColumnWidths(120, 120, 220, 140, 160, 140, 140);

    // Initial Empty State
    showEmptyApprovals();
    view.add(approvalCard, "growx, wrap");

    // 3. Two columns row: Activity Log & Alerts
    JPanel twoColRow = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]16[grow, fill]", "[]"));
    twoColRow.setOpaque(false);

    Card activityCard = Card.standard();
    activityCard.add(new JLabel("Nhật ký hoạt động"), "wrap, gapbottom 12");
    activityPanel.setOpaque(false);
    activityPanel.add(new EmptyState("clock", "Chưa có hoạt động", "Các sự kiện gần nhất sẽ hiển thị tại đây."));
    activityCard.add(activityPanel, "growx");

    Card alertsCard = Card.standard();
    alertsCard.add(new JLabel("Cảnh báo quan trọng"), "wrap, gapbottom 12");
    alertPanel.setOpaque(false);
    alertPanel.add(new ActivityItem("alert-circle", true, "Kiểm tra tồn kho định kỳ", "Theo dõi các sản phẩm tồn lâu ngày để lên kế hoạch xử lý."));
    alertsCard.add(alertPanel, "growx");

    twoColRow.add(activityCard);
    twoColRow.add(alertsCard);
    view.add(twoColRow, "growx, wrap");

    // 4. System Info Row (3 cards)
    JPanel infoRow = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]16[grow, fill]16[grow, fill]", "[]"));
    infoRow.setOpaque(false);

    Card timeCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 10);
    timeCard.add(new ActivityItem("clock", false, "Thời gian hiện tại", DateFormats.formatDateTimeBullet(LocalDateTime.now())));

    Card dbCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 10);
    dbCard.add(new ActivityItem("circle-check", false, "Kết nối CSDL", "Ổn định"));

    Card verCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 10);
    verCard.add(new ActivityItem("package", false, "Phiên bản hệ thống", "DTAQ Retail · Version " + AppInfo.version()));

    infoRow.add(timeCard);
    infoRow.add(dbCard);
    infoRow.add(verCard);
    view.add(infoRow, "growx");
  }

  public void renderApprovals(List<ApprovalRow> approvals) {
    if (approvals == null || approvals.isEmpty()) {
      showEmptyApprovals();
      totalRefundLabel.setText("Tổng hoàn dự kiến 0 VNĐ");
      return;
    }
    long total = 0;
    tableModel.setRowCount(0);
    for (ApprovalRow r : approvals) {
      total += r.hoanDuKien();
      tableModel.addRow(new Object[]{
          r.maPhieu(),
          r.maHDGoc(),
          new String[]{r.tenKH(), r.sdtKH()},
          r.hoanDuKien(),
          r.nhanVienLap(),
          r.lyDo(),
          "Xử lý"
      });
    }
    totalRefundLabel.setText("Tổng hoàn dự kiến " + MoneyFormat.formatVnd(total));

    approvalCard.removeAll();
    // Re-add header + table
    approvalCard.add(dataTable, "grow, push");
    view.revalidate();
    view.repaint();
  }

  private void showEmptyApprovals() {
    approvalCard.removeAll();
    EmptyState empty = new EmptyState("circle-check", "Không có yêu cầu chờ duyệt", "Tất cả các phiếu đổi/trả đã được xử lý hoàn tất.");
    approvalCard.add(empty, "center");
  }

  public void setOnApprove(Consumer<String> listener) {
    this.onApproveListener = listener;
  }

  public void setOnReject(Consumer<String> listener) {
    this.onRejectListener = listener;
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
