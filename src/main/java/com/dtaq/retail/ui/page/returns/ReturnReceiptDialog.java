package com.dtaq.retail.ui.page.returns;

import com.dtaq.retail.controller.DoiTraController;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.ViewportPanel;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Frame;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Chứng từ đổi/trả hàng sau khi phiếu được hoàn tất hoặc gửi chờ Quản lý phê duyệt (UC006 bước 17).
 *
 * <p>Hộp thoại chỉ hiển thị dữ liệu do {@code ReturnsPage} và {@link DoiTraController.KetQuaGhi}
 * truyền vào; không truy vấn cơ sở dữ liệu và không tính toán nghiệp vụ.
 */
public final class ReturnReceiptDialog extends ModalDialog {

  private static final int RECEIPT_WIDTH = 520;

  /**
   * Khởi tạo chứng từ đổi/trả.
   *
   * @param parent cửa sổ cha; có thể {@code null}
   * @param ketQua kết quả ghi phiếu/hoàn tất phiếu; không được {@code null}
   * @param maHD mã hóa đơn gốc; có thể {@code null}
   * @param tenKhachHang họ tên khách hàng; có thể {@code null}
   * @param soDienThoai số điện thoại khách hàng; có thể {@code null}
   * @param soSerial serial máy khách trả lại; có thể {@code null}
   * @param tenDongSP tên dòng sản phẩm của máy trả; có thể {@code null}
   * @param tinhTrang tình trạng máy khách trả lại; có thể {@code null}
   * @param doiMay {@code true} nếu là phiếu đổi hàng (D17: không hoàn tiền)
   */
  public ReturnReceiptDialog(
      Frame parent,
      DoiTraController.KetQuaGhi ketQua,
      String maHD,
      String tenKhachHang,
      String soDienThoai,
      String soSerial,
      String tenDongSP,
      TinhTrangHangTra tinhTrang,
      boolean doiMay) {
    super(parent, "Chứng từ đổi/trả", "Phiếu đổi/trả đã được ghi nhận vào hệ thống.", 560);

    JPanel receiptPaper = new JPanel(new MigLayout(
        "fillx, insets 20, hidemode 3", "[grow]", "[]"));
    receiptPaper.setBackground(DtaqColors.SURFACE);
    receiptPaper.setBorder(BorderFactory.createLineBorder(DtaqColors.BORDER, 1));

    JScrollPane scrollPane = new JScrollPane(new ViewportPanel(receiptPaper));
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize(new Dimension(516, Math.min(500, java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height - 260)));
    scrollPane.setBorder(null);
    scrollPane.getViewport().setBackground(DtaqColors.SURFACE);
    getBody().add(scrollPane, "growx");

    JLabel title = new JLabel("PHIẾU ĐỔI/TRẢ HÀNG", SwingConstants.CENTER);
    title.setFont(DtaqFonts.SECTION_TITLE_STRONG);
    title.setForeground(DtaqColors.TEXT);
    receiptPaper.add(title, "growx, wrap, gapbottom 14");

    JPanel info = new JPanel(new MigLayout("fillx, insets 0, gapy 5", "[130!][grow]", "[]"));
    info.setOpaque(false);
    themDongThongTin(info, "Mã phiếu:", ketQua.maPhieu());
    themDongThongTin(info, "Hóa đơn gốc:", maHD);
    themDongThongTin(info, "Khách hàng:", tenKhachHang);
    themDongThongTin(info, "Số điện thoại:", soDienThoai);
    themDongThongTin(info, "Serial máy trả:", soSerial);
    themDongThongTin(info, "Dòng sản phẩm:", tenDongSP);
    themDongThongTin(info, "Tình trạng:", moTaTinhTrang(tinhTrang));
    receiptPaper.add(info, "growx, wrap, gapbottom 12");

    receiptPaper.add(new JSeparator(), "growx, wrap, gapbottom 10");

    JPanel totals = new JPanel(new MigLayout("fillx, insets 0, gapy 5", "[grow][]", "[]"));
    totals.setOpaque(false);
    BigDecimal chenhLech = ketQua.chenhLech() == null ? BigDecimal.ZERO : ketQua.chenhLech();
    BigDecimal soTienHoan = ketQua.soTienHoan() == null ? BigDecimal.ZERO : ketQua.soTienHoan();
    if (doiMay) {
      // D17: đổi hàng chỉ ngang bằng hoặc thấp hơn, không hoàn tiền, không thu thêm.
      String giaTriChenhLech = chenhLech.signum() == 0
          ? "Không chênh lệch"
          : MoneyFormat.formatVnd(chenhLech);
      themDongTien(totals, "Chênh lệch (không hoàn):", giaTriChenhLech, true);
      themDongTien(totals, "Hoàn tiền:", "Không hoàn tiền", false);
    } else {
      boolean hoanTrungChenhLech =
          chenhLech.signum() < 0 && soTienHoan.compareTo(chenhLech.negate()) == 0;
      if (chenhLech.signum() < 0) {
        themDongTien(totals, "Cửa hàng hoàn khách:",
            MoneyFormat.formatVnd(chenhLech.negate()), true);
      } else if (chenhLech.signum() > 0) {
        themDongTien(totals, "Khách trả thêm:", MoneyFormat.formatVnd(chenhLech), true);
      } else {
        themDongTien(totals, "Chênh lệch:", MoneyFormat.formatVnd(BigDecimal.ZERO), false);
      }
      if (!hoanTrungChenhLech && soTienHoan.signum() > 0) {
        themDongTien(totals, "Số tiền hoàn:", MoneyFormat.formatVnd(soTienHoan), true);
      }
    }
    receiptPaper.add(totals, "growx, wrap, gapbottom 12");

    JPanel trangThaiRow = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    trangThaiRow.setOpaque(false);
    JLabel nhanTrangThai = new JLabel("Trạng thái phiếu:");
    nhanTrangThai.setFont(DtaqFonts.BODY_13);
    nhanTrangThai.setForeground(DtaqColors.TEXT_MUTED);
    trangThaiRow.add(nhanTrangThai);
    trangThaiRow.add(ketQua.choDuyet()
        ? StatusPill.warning("Chờ Quản lý phê duyệt")
        : StatusPill.success("Đã hoàn tất"));
    receiptPaper.add(trangThaiRow, "growx, wrap, gaptop 4");

    DtaqButton dongButton = DtaqButton.primary("Đóng");
    dongButton.addActionListener(e -> requestClose());
    setPrimaryAction(dongButton);
    setActions(dongButton);

    receiptPaper.revalidate();
    receiptPaper.repaint();
    setMinimumSize(new Dimension(RECEIPT_WIDTH + 40, 420));
  }

  private static void themDongThongTin(JPanel panel, String nhan, String giaTri) {
    if (giaTri == null || giaTri.isBlank()) {
      return;
    }
    JLabel label = new JLabel(nhan);
    label.setFont(DtaqFonts.CAPTION);
    label.setForeground(DtaqColors.TEXT_MUTED);
    panel.add(label);
    JLabel value = new JLabel(giaTri);
    value.setFont(DtaqFonts.BODY_13);
    value.setForeground(DtaqColors.TEXT);
    panel.add(value, "wrap");
  }

  private static void themDongTien(JPanel panel, String nhan, String giaTri, boolean manh) {
    JLabel label = new JLabel(nhan);
    label.setFont(manh ? DtaqFonts.LABEL : DtaqFonts.BODY_13);
    label.setForeground(manh ? DtaqColors.TEXT : DtaqColors.TEXT_MUTED);
    panel.add(label);
    JLabel value = new JLabel(giaTri);
    // Số tiền dùng chữ số đều cột (cùng cỡ chữ cũ).
    value.setFont(manh ? DtaqFonts.numeric(true, 16f, 0f) : DtaqFonts.NUMERIC_13);
    value.setForeground(manh ? DtaqColors.PRIMARY_STRONG : DtaqColors.TEXT);
    panel.add(value, "align right, wrap");
  }

  private static String moTaTinhTrang(TinhTrangHangTra tinhTrang) {
    if (tinhTrang == null) {
      return null;
    }
    return tinhTrang == TinhTrangHangTra.NGUYEN_VEN ? "Nguyên vẹn" : "Hàng lỗi";
  }
}
