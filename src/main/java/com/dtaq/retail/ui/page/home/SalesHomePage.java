package com.dtaq.retail.ui.page.home;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.format.PersonNames;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Trang chủ dành cho Nhân viên bán hàng (SalesHomePage, Figma frame 1:1380).
 *
 * <p>Chỉ hiển thị số liệu do controller cung cấp qua {@link #render}; khi chưa có dữ liệu các chỉ số
 * hiện "—" và danh sách hóa đơn ở trạng thái rỗng.
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
  private final KpiCard kpiInvoices =
      new KpiCard("Hóa đơn hôm nay", "—", "Hóa đơn đã thanh toán đủ");
  private final KpiCard kpiCash =
      new KpiCard("Tiền mặt đã thu hôm nay", "—", "Bao gồm tiền thuế");
  private final KpiCard kpiCustomers =
      new KpiCard("Khách đã phục vụ hôm nay", "—", "Khách hàng có hóa đơn hôm nay");
  private final DefaultTableModel tableModel =
      DataTable.readOnlyModel(new String[] {"Hóa đơn", "Khách hàng", "Tổng thanh toán", "Trạng thái"});
  private final DataTable dataTable = new DataTable(tableModel, 48);
  private final StatefulView tableState;

  private Consumer<PageId> onNavigateListener;

  public SalesHomePage(NhanVien nhanVien) {
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader(
        getGreetingTime() + ", " + PersonNames.shortName(nhanVien.hoTen()),
        "Bắt đầu một phiên bán hàng hiệu quả.");
    DtaqButton customersBtn = DtaqButton.secondary("Quản lý khách hàng", "users");
    customersBtn.addActionListener(e -> navigate(PageId.CUSTOMERS));
    DtaqButton invoiceBtn = DtaqButton.primary("Lập hóa đơn", "receipt-text");
    invoiceBtn.addActionListener(e -> navigate(PageId.RETAIL_INVOICE));
    header.setTrailing(customersBtn, invoiceBtn);
    view.add(header, "growx, wrap");

    JPanel kpiRow = new JPanel(new MigLayout(
        "fillx, insets 0", "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]" + DtaqSpacing.KPI_GAP + "[grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    kpiRow.add(kpiInvoices);
    kpiRow.add(kpiCash);
    kpiRow.add(kpiCustomers);
    view.add(kpiRow, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setMoneyColumn(2);
    dataTable.setColumnWidths(140, 520, 160, 140);
    tableState = new StatefulView(
        dataTable, "receipt-text", "Chưa có dữ liệu hóa đơn", "Các hóa đơn vừa lập sẽ hiển thị tại đây.");
    Card tableCard = Card.titled("Hóa đơn gần đây", "Hôm nay");
    tableCard.setBody(tableState);
    view.add(tableCard, "growx");
  }

  /** Hiển thị dữ liệu trang chủ; {@code null} đưa trang về trạng thái rỗng. */
  public void render(SalesHomeData data) {
    tableModel.setRowCount(0);
    if (data == null) {
      kpiInvoices.setValue(null);
      kpiCash.setValue(null);
      kpiCustomers.setValue(null);
      tableState.showEmpty();
      return;
    }
    kpiInvoices.setValue(data.invoicesCount());
    kpiCash.setValue(data.cashCollected());
    kpiCustomers.setValue(data.customersServed());

    if (data.recentInvoices() == null || data.recentInvoices().isEmpty()) {
      tableState.showEmpty("receipt-text", "Chưa có hóa đơn hôm nay", "Các hóa đơn vừa lập sẽ hiển thị tại đây.");
      return;
    }
    for (RecentInvoiceRow row : data.recentInvoices()) {
      tableModel.addRow(new Object[] {row.maHD(), row.khachHang(), row.tongThanhToan(), row.trangThai()});
    }
    tableState.showContent();
  }

  public void showLoading() {
    kpiInvoices.setLoading();
    kpiCash.setLoading();
    kpiCustomers.setLoading();
    tableState.showLoading();
  }

  public void showError(String message) {
    tableState.showError(message);
  }

  /** Nhận yêu cầu chuyển trang từ các nút tắt ở tiêu đề. */
  public void setOnNavigate(Consumer<PageId> listener) {
    this.onNavigateListener = listener;
  }

  private void navigate(PageId target) {
    if (onNavigateListener != null) {
      onNavigateListener.accept(target);
    }
  }

  private static String getGreetingTime() {
    int hour = LocalTime.now().getHour();
    if (hour >= 5 && hour < 11) {
      return "Chào buổi sáng";
    } else if (hour >= 11 && hour < 13) {
      return "Chào buổi trưa";
    } else if (hour >= 13 && hour < 18) {
      return "Chào buổi chiều";
    }
    return "Chào buổi tối";
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
