package com.dtaq.retail.ui.page.invoice;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại hiển thị và xem trước mẫu Hóa đơn bán lẻ in sau khi thanh toán thành công (frame 1:523).
 */
public class InvoiceReceiptDialog extends ModalDialog {

  public record ReceiptItem(int stt, String maSP, String tenSP, String dvt, int sl, long donGia, String thue, long thanhTien) {}

  public record ReceiptData(
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

  private final JPanel receiptPaper;

  public InvoiceReceiptDialog(Frame parent, Runnable onCloseAndReset) {
    super(parent, "Thanh toán thành công", "Hóa đơn đã được lập và ghi nhận vào hệ thống.", 568);

    receiptPaper = new JPanel(new MigLayout("fillx, insets 20", "[grow]", "[]12[]12[]12[]12[]"));
    receiptPaper.setBackground(DtaqColors.SURFACE);
    receiptPaper.setBorder(BorderFactory.createLineBorder(DtaqColors.BORDER, 1));

    JScrollPane scrollPane = new JScrollPane(receiptPaper);
    scrollPane.setPreferredSize(new Dimension(504, 460));
    scrollPane.setBorder(null);
    scrollPane.getViewport().setBackground(DtaqColors.SURFACE);
    getBody().add(scrollPane, "growx, wrap, gapbottom 16");

    DtaqButton dongButton = DtaqButton.primary("Đóng hóa đơn");
    dongButton.addActionListener(e -> {
      dispose();
      if (onCloseAndReset != null) {
        onCloseAndReset.run();
      }
    });

    setActions(dongButton);
  }

  public void render(ReceiptData data) {
    receiptPaper.removeAll();
    if (data == null) {
      receiptPaper.revalidate();
      receiptPaper.repaint();
      return;
    }

    // Top Header: Store Info (Left) & Invoice Meta (Right)
    JPanel topHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    topHeader.setOpaque(false);

    JPanel storeInfo = new JPanel(new MigLayout("insets 0", "[]", "[]2[]2[]"));
    storeInfo.setOpaque(false);
    JLabel storeName = new JLabel("SIÊU THỊ ĐIỆN MÁY DTAQ");
    storeName.setFont(DtaqFonts.TABLE_HEADER);
    storeInfo.add(storeName, "wrap");
    JLabel storeAddr = new JLabel("Đ/c: Số 12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM");
    storeAddr.setFont(DtaqFonts.CAPTION);
    storeAddr.setForeground(DtaqColors.TEXT_MUTED);
    storeInfo.add(storeAddr, "wrap");
    JLabel storePhone = new JLabel("ĐT: 1900 1234");
    storePhone.setFont(DtaqFonts.CAPTION);
    storePhone.setForeground(DtaqColors.TEXT_MUTED);
    storeInfo.add(storePhone);
    topHeader.add(storeInfo, "align left");

    JPanel metaInfo = new JPanel(new MigLayout("insets 0", "[]", "[]2[]"));
    metaInfo.setOpaque(false);
    JLabel invNum = new JLabel("Số HĐ: " + data.soHoaDon());
    invNum.setFont(DtaqFonts.CAPTION);
    metaInfo.add(invNum, "align right, wrap");
    JLabel invDate = new JLabel("Ngày: " + DateFormats.formatDateTime(data.ngayLap()));
    invDate.setFont(DtaqFonts.CAPTION);
    invDate.setForeground(DtaqColors.TEXT_MUTED);
    metaInfo.add(invDate, "align right");
    topHeader.add(metaInfo, "align right");

    receiptPaper.add(topHeader, "growx, wrap");

    // Title
    JLabel title = new JLabel("HÓA ĐƠN BÁN LẺ", JLabel.CENTER);
    title.setFont(DtaqFonts.SECTION_TITLE_STRONG);
    receiptPaper.add(title, "growx, wrap, gapbottom 8");

    // Customer details
    JPanel custBlock = new JPanel(new MigLayout("fillx, insets 0", "[100!][grow]", "[]2[]2[]2[]"));
    custBlock.setOpaque(false);

    custBlock.add(new JLabel("Khách hàng:"), "align left");
    custBlock.add(new JLabel(data.tenKhachHang() + " (" + data.maKhachHang() + ")"), "wrap");

    custBlock.add(new JLabel("Điện thoại:"), "align left");
    custBlock.add(new JLabel(data.soDienThoai()), "wrap");

    custBlock.add(new JLabel("Địa chỉ:"), "align left");
    custBlock.add(new JLabel(data.diaChi() != null ? data.diaChi() : "—"), "wrap");

    custBlock.add(new JLabel("Nhân viên:"), "align left");
    custBlock.add(new JLabel(data.nhanVienBanHang()), "wrap");

    receiptPaper.add(custBlock, "growx, wrap, gapbottom 12");
    receiptPaper.add(new JSeparator(), "growx, wrap, gapbottom 8");

    // Products table summary
    for (ReceiptItem item : data.items()) {
      JPanel itemRow = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]2[]"));
      itemRow.setOpaque(false);

      JLabel name = new JLabel(item.stt() + ". " + item.tenSP() + " (" + item.maSP() + ")");
      name.setFont(DtaqFonts.BODY_13_MEDIUM);
      itemRow.add(name);

      JLabel total = new JLabel(MoneyFormat.formatVnd(item.thanhTien()));
      total.setFont(DtaqFonts.BODY_13_MEDIUM);
      itemRow.add(total, "align right, wrap");

      JLabel detail = new JLabel("SL: " + item.sl() + " " + item.dvt() + " × " + MoneyFormat.format(item.donGia()) + " (VAT " + item.thue() + ")");
      detail.setFont(DtaqFonts.CAPTION);
      detail.setForeground(DtaqColors.TEXT_MUTED);
      itemRow.add(detail, "span 2");

      receiptPaper.add(itemRow, "growx, wrap, gapbottom 6");
    }

    receiptPaper.add(new JSeparator(), "growx, wrap, gapbottom 8");

    // Totals block
    JPanel totals = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]4[]6[]8[]4[]"));
    totals.setOpaque(false);

    totals.add(new JLabel("Tổng tiền hàng trước thuế:"));
    totals.add(new JLabel(MoneyFormat.formatVnd(data.tongTienHang())), "align right, wrap");

    totals.add(new JLabel("Tổng tiền thuế VAT:"));
    totals.add(new JLabel(MoneyFormat.formatVnd(data.tienThue())), "align right, wrap");

    JLabel ttLabel = new JLabel("TỔNG THANH TOÁN:");
    ttLabel.setFont(DtaqFonts.BUTTON);
    totals.add(ttLabel);
    JLabel ttVal = new JLabel(MoneyFormat.formatVnd(data.tongThanhToan()));
    ttVal.setFont(DtaqFonts.BUTTON);
    ttVal.setForeground(DtaqColors.PRIMARY_STRONG);
    totals.add(ttVal, "align right, wrap");

    totals.add(new JLabel("Tiền khách đưa:"));
    totals.add(new JLabel(MoneyFormat.formatVnd(data.tienKhachDua())), "align right, wrap");

    totals.add(new JLabel("Tiền trả lại khách:"));
    totals.add(new JLabel(MoneyFormat.formatVnd(data.tienTraLai())), "align right, wrap");

    receiptPaper.add(totals, "growx, wrap, gapbottom 12");

    // Footer note
    JLabel thankYou = new JLabel("Cảm ơn quý khách đã mua hàng tại Siêu thị điện máy DTAQ!", JLabel.CENTER);
    thankYou.setFont(DtaqFonts.CAPTION);
    thankYou.setForeground(DtaqColors.TEXT_MUTED);
    receiptPaper.add(thankYou, "growx");

    receiptPaper.revalidate();
    receiptPaper.repaint();
  }
}
