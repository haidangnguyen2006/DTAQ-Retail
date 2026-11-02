package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.controller.KhachHangController;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Component;
import java.awt.Frame;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chọn khách hàng theo số điện thoại, hoặc tạo nhanh khi chưa có hồ sơ. Dùng ở màn hình lập hóa đơn
 * (UC003).
 */
public final class KhachHangPicker {

  private final KhachHangController controller;

  /**
   * Khởi tạo bộ chọn khách hàng.
   *
   * @param controller bộ điều khiển khách hàng; không được {@code null}
   */
  public KhachHangPicker(KhachHangController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");
  }

  /**
   * Mở hộp thoại chọn khách hàng và chờ người dùng quyết định; phải gọi trên EDT.
   *
   * @param parent thành phần giao diện đang gọi
   * @param sdt số điện thoại tra cứu sẵn khi mở; rỗng hoặc {@code null} nếu chưa có
   * @return khách hàng được chọn, hoặc rỗng nếu người dùng hủy
   */
  public Optional<KhachHang> chon(Component parent, String sdt) {
    Frame frame = parent instanceof Frame f ? f : ConfirmDialog.frameOf(parent);
    PickerDialog dialog = new PickerDialog(frame, controller, sdt);
    dialog.setVisible(true);
    return Optional.ofNullable(dialog.ketQua);
  }

  private static final class PickerDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(PickerDialog.class);

    private static final String LOI_TRA_CUU = "Không thể tra cứu khách hàng. Vui lòng thử lại";

    private final KhachHangController controller;
    private final FormField sdtField = FormField.search("Số điện thoại", "Nhập số điện thoại khách hàng");
    private final DtaqButton timButton = DtaqButton.secondary("Tìm");
    private final DtaqButton taoNhanhButton = DtaqButton.secondary("Tạo nhanh");
    private final DtaqButton chonButton = DtaqButton.primary("Chọn khách hàng");
    private final JLabel tenLabel = new JLabel();
    private final JLabel chiTietLabel = new JLabel();
    private final StatefulView ketQuaView;

    private KhachHang timThay;
    private KhachHang ketQua;

    PickerDialog(Frame parent, KhachHangController controller, String sdt) {
      super(parent, "Chọn khách hàng", "Tìm theo số điện thoại hoặc tạo hồ sơ mới.", 520);
      this.controller = controller;

      JPanel timRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]8[]", "[bottom]"));
      timRow.setOpaque(false);
      timRow.add(sdtField, "growx");
      timRow.add(timButton);
      sdtField.getTextField().addActionListener(e -> tim());
      timButton.addActionListener(e -> tim());

      JPanel hoSo = new JPanel(new MigLayout("fillx, insets 8 0 8 0", "[grow]", "[]4[]"));
      hoSo.setOpaque(false);
      tenLabel.setFont(DtaqFonts.BODY_14);
      tenLabel.setForeground(DtaqColors.TEXT);
      chiTietLabel.setFont(DtaqFonts.BODY_13);
      chiTietLabel.setForeground(DtaqColors.TEXT_MUTED);
      hoSo.add(tenLabel, "growx, wrap");
      hoSo.add(chiTietLabel, "growx");

      ketQuaView = new StatefulView(hoSo, "search", "Chưa tra cứu", "Nhập số điện thoại để tìm khách hàng.");
      getBody().add(timRow, "growx, wrap, gapbottom 12");
      getBody().add(ketQuaView, "growx");

      DtaqButton huyButton = DtaqButton.secondary("Hủy");
      huyButton.addActionListener(e -> requestClose());
      taoNhanhButton.addActionListener(e -> taoNhanh());
      chonButton.addActionListener(e -> xacNhan(timThay));
      chonButton.setEnabled(false);
      setActions(huyButton, taoNhanhButton, chonButton);

      if (sdt != null && !sdt.isBlank()) {
        sdtField.setText(sdt.trim());
        SwingUtilities.invokeLater(this::tim);
      }
    }

    private void tim() {
      String sdt = sdtField.getText();
      if (sdt.isBlank()) {
        sdtField.setError("Vui lòng nhập số điện thoại");
        return;
      }
      sdtField.setError(null);
      capNhatKetQua(null);
      ketQuaView.showLoading();
      timButton.setEnabled(false);
      new SwingWorker<Optional<KhachHang>, Void>() {
        @Override
        protected Optional<KhachHang> doInBackground() {
          return controller.timTheoSdt(sdt);
        }

        @Override
        protected void done() {
          timButton.setEnabled(true);
          try {
            Optional<KhachHang> khachHang = get();
            capNhatKetQua(khachHang.orElse(null));
            if (khachHang.isEmpty()) {
              ketQuaView.showEmpty("user-round", "Không tìm thấy khách hàng",
                  "Chọn \"Tạo nhanh\" để tạo hồ sơ với số điện thoại này.");
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ketQuaView.showError(LOI_TRA_CUU);
          } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.warn("Không thể tra cứu khách hàng (exception={})",
                cause == null ? "none" : cause.getClass().getName());
            ketQuaView.showError(cause instanceof SecurityException ? cause.getMessage() : LOI_TRA_CUU);
          }
        }
      }.execute();
    }

    private void capNhatKetQua(KhachHang khachHang) {
      timThay = khachHang;
      chonButton.setEnabled(khachHang != null);
      if (khachHang != null) {
        tenLabel.setText(khachHang.hoTen());
        String sdt = khachHang.maKH() + " • " + PhoneFormat.format(khachHang.soDienThoai());
        chiTietLabel.setText(khachHang.diaChi() == null ? sdt : sdt + " • " + khachHang.diaChi());
        ketQuaView.showContent();
        setPrimaryAction(chonButton);
      }
    }

    private void taoNhanh() {
      AddCustomerDialog dialog = new AddCustomerDialog((Frame) getOwner());
      dialog.setSoDienThoai(sdtField.getText().trim());
      dialog.setOnSave(form -> dialog.save(
          () -> controller.themKhach(form.hoTen(), form.soDienThoai(), form.diaChi()),
          this::xacNhan,
          this::xacNhan));
      dialog.setVisible(true);
    }

    private void xacNhan(KhachHang khachHang) {
      ketQua = khachHang;
      dispose();
    }
  }
}
