package com.dtaq.retail.ui.page.invoice;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.ViewportPanel;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Frame;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại hiển thị mẫu Hóa đơn bán lẻ sau khi thanh toán thành công (frame 1:523).
 * Mọi số liệu, số hóa đơn và thông tin cửa hàng đều do controller cung cấp qua {@link ReceiptData}.
 */
public class InvoiceReceiptDialog extends ModalDialog {

  public record ReceiptItem(int stt, String maSP, String tenSP, String dvt, int sl, long donGia, String thue, long thanhTien) {}

  /**
   * Dữ liệu hóa đơn đã lưu. Các trường thông tin cửa hàng có thể {@code null} (dòng tương ứng được ẩn).
   */
  public record ReceiptData(
      String tenCuaHang,
      String diaChiCuaHang,
      String dienThoaiCuaHang,
      String soHoaDon,
      LocalDateTime ngayLap,
      String tenKhachHang,
      String maKhachHang,
      String soDienThoai,
      String diaChi,
      String nhanVienBanHang,
      List<ReceiptItem> items,
      long tongTienHang,
      long tienThue,
      long tongThanhToan,
      long tienKhachDua,
      long tienTraLai
  ) {}

  private static final String[] ITEM_COLUMNS = {"STT", "Mã SP", "Tên sản phẩm", "ĐVT", "SL", "Đơn giá", "Thuế", "Thành tiền"};
  private static final int ITEM_ROW_HEIGHT = 30;

  private final JPanel receiptPaper;
  private final Runnable onCloseAndReset;
  private boolean closed;

  public InvoiceReceiptDialog(Frame parent, Runnable onCloseAndReset) {
    super(parent, "Thanh toán thành công", "Hóa đơn đã được lập và ghi nhận vào hệ thống.", 640);
    this.onCloseAndReset = onCloseAndReset;

    receiptPaper = new JPanel(new MigLayout("fillx, insets 20, hidemode 3", "[grow]", "[]"));
    receiptPaper.setBackground(DtaqColors.SURFACE);
    receiptPaper.setBorder(BorderFactory.createLineBorder(DtaqColors.BORDER, 1));

    JScrollPane scrollPane = new JScrollPane(new ViewportPanel(receiptPaper));
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize(new Dimension(576, 500));
    scrollPane.setBorder(null);
    scrollPane.getViewport().setBackground(DtaqColors.SURFACE);
    getBody().add(scrollPane, "growx");

    DtaqButton dongButton = DtaqButton.primary("Đóng hóa đơn");
    dongButton.addActionListener(e -> requestClose());
    setPrimaryAction(dongButton);
    setActions(dongButton);
  }

  /** Đóng hộp thoại (nút, Esc hoặc ×) và luôn báo trang để bắt đầu hóa đơn mới đúng một lần. */
  @Override
  protected void requestClose() {
    dispose();
    if (!closed) {
      closed = true;
      if (onCloseAndReset != null) {
        onCloseAndReset.run();
      }
    }
  }

  public void render(ReceiptData data) {
    receiptPaper.removeAll();
    if (data == null) {
      receiptPaper.revalidate();
      receiptPaper.repaint();
      return;
    }

    JPanel topHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    topHeader.setOpaque(false);

    JPanel storeInfo = new JPanel(new MigLayout("insets 0, gapy 2, hidemode 3", "[]", "[]"));
    storeInfo.setOpaque(false);
    addIfPresent(storeInfo, data.tenCuaHang(), DtaqFonts.TABLE_HEADER, DtaqColors.TEXT, true);
    addIfPresent(storeInfo, prefixed("Đ/c: ", data.diaChiCuaHang()), DtaqFonts.CAPTION, DtaqColors.TEXT_MUTED, true);
    addIfPresent(storeInfo, prefixed("ĐT: ", data.dienThoaiCuaHang()), DtaqFonts.CAPTION, DtaqColors.TEXT_MUTED, false);
    topHeader.add(storeInfo, "align left, aligny top");

    JPanel metaInfo = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    metaInfo.setOpaque(false);
    JLabel invNum = new JLabel("Số HĐ: " + data.soHoaDon());
    invNum.setFont(DtaqFonts.TABLE_HEADER);
    metaInfo.add(invNum, "align right, wrap");
    JLabel invDate = new JLabel("Ngày: " + DateFormats.formatDateTime(data.ngayLap()));
    invDate.setFont(DtaqFonts.CAPTION);
    invDate.setForeground(DtaqColors.TEXT_MUTED);
    metaInfo.add(invDate, "align right");
    topHeader.add(metaInfo, "align right, aligny top");
    receiptPaper.add(topHeader, "growx, wrap");

    JLabel title = new JLabel("HÓA ĐƠN BÁN LẺ", SwingConstants.CENTER);
    title.setFont(DtaqFonts.SECTION_TITLE_STRONG);
    receiptPaper.add(title, "growx, wrap, gaptop 14, gapbottom 10");

    JPanel custBlock = new JPanel(new MigLayout("fillx, insets 0, gapy 4", "[110!][grow]", "[]"));
    custBlock.setOpaque(false);
    addInfoRow(custBlock, "Khách hàng:", data.tenKhachHang() + " (" + data.maKhachHang() + ")");
    addInfoRow(custBlock, "Điện thoại:", data.soDienThoai());
    addInfoRow(custBlock, "Địa chỉ:", data.diaChi() != null && !data.diaChi().isBlank() ? data.diaChi() : "—");
    addInfoRow(custBlock, "Nhân viên:", data.nhanVienBanHang());
    receiptPaper.add(custBlock, "growx, wrap, gapbottom 12");

    receiptPaper.add(buildItemTable(data.items()), "growx, wrap, gapbottom 12");

    JPanel totals = new JPanel(new MigLayout("fillx, insets 0, gapy 5", "[grow][]", "[]"));
    totals.setOpaque(false);
    addTotalRow(totals, "Tổng tiền hàng trước thuế:", MoneyFormat.formatVnd(data.tongTienHang()), false);
    addTotalRow(totals, "Tổng tiền thuế VAT:", MoneyFormat.formatVnd(data.tienThue()), false);
    totals.add(new JSeparator(), "growx, span, wrap, gaptop 2, gapbottom 2");
    addTotalRow(totals, "TỔNG THANH TOÁN:", MoneyFormat.formatVnd(data.tongThanhToan()), true);
    addTotalRow(totals, "Hình thức thanh toán:", "Tiền mặt", false);
    addTotalRow(totals, "Tiền khách đưa:", MoneyFormat.formatVnd(data.tienKhachDua()), false);
    addTotalRow(totals, "Tiền trả lại khách:", MoneyFormat.formatVnd(data.tienTraLai()), false);
    receiptPaper.add(totals, "growx, wrap, gapbottom 14");

    JLabel thankYou = new JLabel("Cảm ơn quý khách đã mua hàng!", SwingConstants.CENTER);
    thankYou.setFont(DtaqFonts.CAPTION);
    thankYou.setForeground(DtaqColors.TEXT_MUTED);
    receiptPaper.add(thankYou, "growx");

    receiptPaper.revalidate();
    receiptPaper.repaint();
  }

  private static String prefixed(String prefix, String value) {
    return value == null || value.isBlank() ? null : prefix + value;
  }

  private static void addIfPresent(JPanel panel, String text, java.awt.Font font, java.awt.Color color, boolean wrap) {
    if (text == null || text.isBlank()) {
      return;
    }
    JLabel label = new JLabel(text);
    label.setFont(font);
    label.setForeground(color);
    panel.add(label, wrap ? "wrap" : "");
  }

  private static void addInfoRow(JPanel panel, String label, String value) {
    JLabel l = new JLabel(label);
    l.setFont(DtaqFonts.CAPTION);
    l.setForeground(DtaqColors.TEXT_MUTED);
    panel.add(l);
    JLabel v = new JLabel(value);
    v.setFont(DtaqFonts.BODY_13);
    panel.add(v, "wrap");
  }

  private static void addTotalRow(JPanel panel, String label, String value, boolean strong) {
    JLabel l = new JLabel(label);
    l.setFont(strong ? DtaqFonts.BUTTON : DtaqFonts.BODY_13);
    panel.add(l);
    JLabel v = new JLabel(value);
    v.setFont(strong ? DtaqFonts.BUTTON : DtaqFonts.BODY_13);
    v.setForeground(strong ? DtaqColors.PRIMARY_STRONG : DtaqColors.TEXT);
    panel.add(v, "align right, wrap");
  }

  private static JPanel buildItemTable(List<ReceiptItem> items) {
    DefaultTableModel model = new DefaultTableModel(ITEM_COLUMNS, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    for (ReceiptItem it : items) {
      model.addRow(new Object[] {
          it.stt(), it.maSP(), it.tenSP(), it.dvt(), it.sl(),
          MoneyFormat.format(it.donGia()), it.thue(), MoneyFormat.format(it.thanhTien())});
    }

    JTable table = new JTable(model);
    table.setRowHeight(ITEM_ROW_HEIGHT);
    table.setFont(DtaqFonts.CAPTION);
    table.setGridColor(DtaqColors.BORDER);
    table.setShowGrid(true);
    table.setFocusable(false);
    table.setRowSelectionAllowed(false);
    int[] widths = {36, 78, 170, 44, 34, 80, 48, 90};
    DefaultTableCellRenderer right = new DefaultTableCellRenderer();
    right.setHorizontalAlignment(SwingConstants.RIGHT);
    for (int i = 0; i < widths.length; i++) {
      table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
      if (i == 5 || i == 7 || i == 4) {
        table.getColumnModel().getColumn(i).setCellRenderer(right);
      }
    }
    table.getTableHeader().setFont(DtaqFonts.TABLE_HEADER);
    table.getTableHeader().setReorderingAllowed(false);
    table.getTableHeader().setPreferredSize(new Dimension(0, 30));

    JPanel holder = new JPanel(new MigLayout("fillx, insets 0, gap 0", "[grow]", "[]0[]"));
    holder.setOpaque(false);
    holder.add(table.getTableHeader(), "growx, wrap");
    holder.add(table, "growx");
    return holder;
  }
}
