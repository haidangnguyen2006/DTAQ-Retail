package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.controller.KhachHangController;
import com.dtaq.retail.controller.KhachHangController.KetQuaLuuKhach;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Frame;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hộp thoại Thêm khách hàng mới (UC007, Figma frame 1:176), dùng lại cho cập nhật hồ sơ đã có.
 * Kiểm tra nhanh tại chỗ để phản hồi sớm; controller vẫn là nơi kiểm tra và quyết định cuối cùng.
 */
public class AddCustomerDialog extends ModalDialog {

  public record NewCustomerForm(String hoTen, String soDienThoai, String diaChi) {}

  private static final Logger log = LoggerFactory.getLogger(AddCustomerDialog.class);

  private static final String LOI_LUU = "Không thể lưu khách hàng. Vui lòng thử lại";

  private final FormField hoTenField = FormField.text("Họ và tên", "Nhập họ tên đầy đủ của khách hàng", null);
  private final FormField sdtField = FormField.text("Số điện thoại", "Nhập số điện thoại", "phone");
  private final FormField diaChiField = FormField.text("Địa chỉ", "Nhập địa chỉ liên hệ", null);
  private final InfoCallout formError = new InfoCallout(InfoCallout.Kind.DANGER, "");
  private final JPanel existingPanel = new JPanel(new MigLayout("fillx, insets 0, hidemode 3", "[grow]", "[]10[]10[]"));
  private final DtaqButton luuButton;
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");

  private Consumer<NewCustomerForm> onSaveListener;
  private boolean busy;

  public AddCustomerDialog(Frame parent) {
    this(parent, "Thêm khách hàng", "Tạo hồ sơ để sử dụng khi lập hóa đơn.", "Lưu khách hàng",
        "Các trường có dấu * là bắt buộc. Mã khách hàng sẽ được tạo tự động khi lưu.");
  }

  private AddCustomerDialog(Frame parent, String title, String subtitle, String saveText, String note) {
    super(parent, title, subtitle, 600);
    luuButton = DtaqButton.primary(saveText);

    hoTenField.setRequired(true);
    sdtField.setRequired(true);
    sdtField.setHint("Số điện thoại dùng để tra cứu; không trùng hồ sơ hiện có.");

    existingPanel.setOpaque(false);
    existingPanel.setVisible(false);
    formError.setVisible(false);

    getBody().setLayout(new MigLayout("fillx, insets 0, hidemode 3", "[grow]", "[]"));
    getBody().add(hoTenField, "growx, wrap, gapbottom 16");
    getBody().add(sdtField, "growx, wrap, gapbottom 16");
    getBody().add(diaChiField, "growx, wrap, gapbottom 20");
    getBody().add(existingPanel, "growx, wrap, gapbottom 16");
    getBody().add(formError, "growx, wrap, gapbottom 16");
    getBody().add(new InfoCallout(note), "growx, wrap");

    hoTenField.getTextField().addActionListener(e -> sdtField.getTextField().requestFocusInWindow());
    sdtField.getTextField().addActionListener(e -> diaChiField.getTextField().requestFocusInWindow());
    diaChiField.getTextField().addActionListener(e -> submit());

    huyButton.addActionListener(e -> requestClose());
    luuButton.addActionListener(e -> submit());
    setPrimaryAction(luuButton);
    setActions(huyButton, luuButton);
  }

  /** Tạo hộp thoại cập nhật đã điền sẵn thông tin khách hàng; mã khách hàng chỉ hiển thị, không sửa được. */
  public static AddCustomerDialog forEdit(
      Frame parent, String maKH, String hoTen, String soDienThoai, String diaChi) {
    AddCustomerDialog dialog = new AddCustomerDialog(parent, "Cập nhật khách hàng",
        "Mã khách hàng: " + maKH, "Lưu thay đổi",
        "Các trường có dấu * là bắt buộc. Mã khách hàng không thay đổi.");
    dialog.hoTenField.setText(hoTen);
    dialog.sdtField.setText(soDienThoai);
    dialog.diaChiField.setText(diaChi);
    return dialog;
  }

  private void submit() {
    if (busy) {
      return;
    }
    hideMessages();
    String hoTen = hoTenField.getText().trim();
    String sdt = sdtField.getText().trim();
    String loiSdt = KhachHangController.kiemTraSdt(sdt);
    hoTenField.setError(hoTen.isEmpty() ? "Vui lòng nhập họ và tên" : null);
    sdtField.setError(loiSdt);
    if (hoTen.isEmpty() || loiSdt != null) {
      return;
    }
    if (onSaveListener != null) {
      onSaveListener.accept(new NewCustomerForm(hoTen, sdt, diaChiField.getText().trim()));
    }
  }

  public void setOnSave(Consumer<NewCustomerForm> listener) {
    this.onSaveListener = listener;
  }

  public void clearForm() {
    hoTenField.setText("");
    sdtField.setText("");
    diaChiField.setText("");
    hoTenField.setError(null);
    sdtField.setError(null);
    diaChiField.setError(null);
    hideMessages();
    setBusy(false);
  }

  /** Điền sẵn số điện thoại, dùng khi tạo nhanh từ một số vừa tra cứu không thấy. */
  public void setSoDienThoai(String soDienThoai) {
    sdtField.setText(soDienThoai);
  }

  public void setFieldError(String fieldName, String error) {
    if ("hoTen".equalsIgnoreCase(fieldName)) {
      hoTenField.setError(error);
    } else if ("soDienThoai".equalsIgnoreCase(fieldName)) {
      sdtField.setError(error);
    } else if ("diaChi".equalsIgnoreCase(fieldName)) {
      diaChiField.setError(error);
    }
  }

  /** Khóa form và hiện "Đang xử lý…" trong lúc lưu. */
  public void setBusy(boolean busy) {
    this.busy = busy;
    luuButton.setBusy(busy);
    huyButton.setEnabled(!busy);
  }

  /** Lưu thành công: đóng hộp thoại. */
  public void closeWithSuccess() {
    setBusy(false);
    dispose();
  }

  /**
   * Chạy thao tác lưu ngoài EDT và hiển thị kết quả. Lỗi giữ nguyên dữ liệu đã nhập để thử lại.
   *
   * @param luu thao tác lưu gọi controller
   * @param onSaved nhận hồ sơ sau khi lưu thành công; hộp thoại đã đóng
   * @param onUseExisting nhận hồ sơ hiện có khi người dùng chọn dùng lại; hộp thoại đã đóng
   */
  public void save(
      Supplier<KetQuaLuuKhach> luu, Consumer<KhachHang> onSaved, Consumer<KhachHang> onUseExisting) {
    setBusy(true);
    new SwingWorker<KetQuaLuuKhach, Void>() {
      @Override
      protected KetQuaLuuKhach doInBackground() {
        return luu.get();
      }

      @Override
      protected void done() {
        setBusy(false);
        try {
          showResult(get(), onSaved, onUseExisting);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          showFormError(LOI_LUU);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof SecurityException) {
            showFormError(cause.getMessage());
          } else {
            log.warn("Lưu khách hàng thất bại do lỗi không mong đợi (exception={})",
                cause == null ? "none" : cause.getClass().getName());
            showFormError(LOI_LUU);
          }
        }
      }
    }.execute();
  }

  private void showResult(
      KetQuaLuuKhach ketQua, Consumer<KhachHang> onSaved, Consumer<KhachHang> onUseExisting) {
    switch (ketQua.trangThai()) {
      case THANH_CONG -> {
        closeWithSuccess();
        onSaved.accept(ketQua.khachHang());
      }
      case TRUNG_SDT -> showExistingCustomer(ketQua.khachHang(), onUseExisting);
      case LOI -> {
        if (ketQua.truongLoi() != null) {
          setFieldError(ketQua.truongLoi(), ketQua.thongBao());
        } else {
          showFormError(ketQua.thongBao());
        }
      }
    }
  }

  /** Luồng 8.a: hiển thị hồ sơ trùng số điện thoại để đối chiếu và cho phép dùng lại. */
  private void showExistingCustomer(KhachHang khachHang, Consumer<KhachHang> onUseExisting) {
    existingPanel.removeAll();
    existingPanel.add(new InfoCallout(InfoCallout.Kind.WARNING,
        "Số điện thoại đã có hồ sơ khách hàng. Vui lòng đối chiếu trước khi dùng hồ sơ này."), "growx, wrap");
    existingPanel.add(existingLabel(khachHang.maKH() + " • " + khachHang.hoTen() + " • "
        + PhoneFormat.format(khachHang.soDienThoai())), "growx, wrap");
    if (khachHang.diaChi() != null) {
      existingPanel.add(existingLabel(khachHang.diaChi()), "growx, wrap");
    }
    DtaqButton dungHoSo = DtaqButton.secondary("Dùng hồ sơ này");
    dungHoSo.addActionListener(e -> {
      closeWithSuccess();
      onUseExisting.accept(khachHang);
    });
    existingPanel.add(dungHoSo, "align left");
    existingPanel.setVisible(true);
    getBody().revalidate();
    getBody().repaint();
  }

  private static JLabel existingLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(DtaqFonts.BODY_13);
    label.setForeground(DtaqColors.TEXT);
    return label;
  }

  private void showFormError(String message) {
    formError.setText(message);
    formError.setVisible(true);
    getBody().revalidate();
    getBody().repaint();
  }

  private void hideMessages() {
    existingPanel.setVisible(false);
    formError.setVisible(false);
  }

  @Override
  protected void requestClose() {
    if (!busy) {
      dispose();
    }
  }
}
