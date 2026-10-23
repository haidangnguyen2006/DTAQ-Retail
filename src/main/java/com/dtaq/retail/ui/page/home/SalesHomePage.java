package com.dtaq.retail.ui.page.home;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.EmptyState;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.format.PersonNames;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Dimension;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Trang chủ dành cho Nhân viên bán hàng (SalesHomePage) theo Figma frame 1:1380.
 */
public class SalesHomePage implements Page {

  public record RecentInvoiceRow(String maHD, String khachHang, long tongThanhToan, String trangThai) {}

  public record SalesHomeData(
      String greetingName,
      String invoicesCount,
      String cashCollected,
      String customersServed,
      List<RecentInvoiceRow> recentInvoices
  ) {}

  private final NhanVien nhanVien;
  private final JPanel view;
  private final PageHeader header;
  private final KpiCard kpiInvoices;
  private final KpiCard kpiCash;
  private final KpiCard kpiCustomers;
  private final Card tableCard;
  private final DataTable dataTable;
  private final DefaultTableModel tableModel;

  public SalesHomePage(NhanVien nhanVien) {
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[grow]"
    ));
    view.setBackground(DtaqColors.BG_APP);

    // 1. Header
    String greeting = getGreetingTime() + ", " + PersonNames.shortName(nhanVien.hoTen());
    header = new PageHeader(greeting, "Bắt đầu một phiên bán hàng hiệu quả.");
    view.add(header, "growx, wrap");

    // 2. 3 KPI Cards
    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]20[grow, fill]20[grow, fill]", "[]"));
    kpiRow.setOpaque(false);

    kpiInvoices = new KpiCard("Hóa đơn trong ca", "—", "Hóa đơn đã thanh toán đủ");
    kpiCash = new KpiCard("Tiền mặt đã thu", "—", "Bao gồm tiền thuế");
    kpiCustomers = new KpiCard("Khách hàng đã phục vụ", "—", "Khách hàng phục vụ trong ca");

    kpiRow.add(kpiInvoices);
    kpiRow.add(kpiCash);
    kpiRow.add(kpiCustomers);
    view.add(kpiRow, "growx, wrap");

    // 3. Recent Invoices Card
    tableCard = Card.titled("Hóa đơn gần đây trong ca", "Hôm nay");

    String[] columns = {"Hóa đơn", "Khách hàng", "Tổng thanh toán", "Trạng thái"};
    tableModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    dataTable = new DataTable(tableModel, 48);
    dataTable.setCodeColumn(0);
    dataTable.setMoneyColumn(2);
    dataTable.setColumnWidths(135, 640, 140, 120);

    // Initially show empty state
    showEmptyRecent();
    view.add(tableCard, "grow, push");
  }

  public void render(SalesHomeData data) {
    if (data == null) {
      showEmptyRecent();
      return;
    }
    kpiInvoices.setValue(data.invoicesCount());
    kpiCash.setValue(data.cashCollected());
    kpiCustomers.setValue(data.customersServed());

    tableModel.setRowCount(0);
    if (data.recentInvoices() != null && !data.recentInvoices().isEmpty()) {
      tableCard.removeAll();
      tableCard.add(Card.titled("Hóa đơn gần đây trong ca", "Hôm nay"), "growx, wrap");
      for (RecentInvoiceRow row : data.recentInvoices()) {
        tableModel.addRow(new Object[]{row.maHD(), row.khachHang(), row.tongThanhToan(), row.trangThai()});
      }
      tableCard.add(dataTable, "grow, push");
    } else {
      showEmptyRecent();
    }
    view.revalidate();
    view.repaint();
  }

  private void showEmptyRecent() {
    tableCard.removeAll();
    JPanel headerBlock = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    headerBlock.setOpaque(false);
    headerBlock.add(new javax.swing.JLabel("Hóa đơn gần đây trong ca"), "align left");
    headerBlock.add(new javax.swing.JLabel("Hôm nay"), "align right");
    tableCard.add(headerBlock, "growx, wrap");

    EmptyState empty = new EmptyState("receipt-text", "Chưa có hóa đơn trong ca", "Các hóa đơn vừa lập sẽ hiển thị tại đây.");
    tableCard.add(empty, "center");
  }

  private String getGreetingTime() {
    int hour = LocalTime.now().getHour();
    if (hour >= 5 && hour < 11) {
      return "Chào buổi sáng";
    } else if (hour >= 11 && hour < 13) {
      return "Chào buổi trưa";
    } else if (hour >= 13 && hour < 18) {
      return "Chào buổi chiều";
    } else {
      return "Chào buổi tối";
    }
  }

  @Override
  public PageId id() {
    return PageId.SALES_HOME;
  }

  @Override
  public String title() {
    return "Trang chủ";
  }

  @Override
  public String section() {
    return "Bán hàng";
  }

  @Override
  public JComponent view() {
    return view;
  }
}
