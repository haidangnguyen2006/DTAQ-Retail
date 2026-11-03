package com.dtaq.retail.ui.page.employee;

import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.controller.NhanVienController.KetQuaLuuNhanVien;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.format.DateFormats;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hộp thoại Thêm nhân viên bán hàng (UC013). Kiểm tra tại chỗ chỉ để phản hồi sớm; controller là
 * nơi kiểm tra và quyết định cuối cùng. Lỗi lưu giữ nguyên dữ liệu đã nhập để thử lại.
 */
final class AddEmployeeDialog extends ModalDialog {

  private static final Logger log = LoggerFactory.getLogger(AddEmployeeDialog.class);

  private static final String LOI_LUU = "Không thể lưu nhân viên. Vui lòng thử lại";

  /** Đọc ngày nhập tay theo kiểu chặt: ngày không có thật như 31/02 bị từ chối thay vì tự lùi về 28/02. */
  private static final DateTimeFormatter NGAY_NHAP =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

  private final NhanVienController controller;
  private final Consumer<NhanVien> onSaved;
  private final FormField hoTenField = FormField.text("Họ và tên", "Nhập họ tên đầy đủ", null);
  private final FormField sdtField = FormField.text("Số điện thoại", "Nhập số điện thoại", "phone");
  private final FormField emailField = FormField.text("Email", "Nhập email liên hệ", null);
  private final FormField ngayVaoLamField = FormField.text("Ngày vào làm", "dd/MM/yyyy", "calendar-days");
  private final FormField tenDangNhapField = FormField.text("Tên đăng nhập", "Nhập tên đăng nhập", "user-round");
  private final FormField matKhauField = FormField.password("Mật khẩu khởi tạo",
      "Tối thiểu " + NhanVienController.DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự");
  private final InfoCallout formError = new InfoCallout(InfoCallout.Kind.DANGER, "");
  private final DtaqButton luuButton = DtaqButton.primary("Lưu nhân viên");
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");
  private final Map<String, FormField> truongTheoTen;

  private boolean busy;

  AddEmployeeDialog(Frame parent, NhanVienController controller, Consumer<NhanVien> onSaved) {
    super(parent, "Thêm nhân viên bán hàng", "Tạo hồ sơ và tài khoản đăng nhập cho nhân viên mới.", 640);
    this.controller = Objects.requireNonNull(controller, "controller");
    this.onSaved = Objects.requireNonNull(onSaved, "onSaved");
    truongTheoTen = Map.of(
        NhanVienController.TRUONG_HO_TEN, hoTenField,
        NhanVienController.TRUONG_SDT, sdtField,
        NhanVienController.TRUONG_EMAIL, emailField,
        NhanVienController.TRUONG_NGAY_VAO_LAM, ngayVaoLamField,
        NhanVienController.TRUONG_TEN_DANG_NHAP, tenDangNhapField,
        NhanVienController.TRUONG_MAT_KHAU, matKhauField);

    hoTenField.setRequired(true);
    sdtField.setRequired(true);
    ngayVaoLamField.setRequired(true);
    tenDangNhapField.setRequired(true);
    matKhauField.setRequired(true);
    ngayVaoLamField.setText(DateFormats.formatDate(LocalDate.now()));
    formError.setVisible(false);

    getBody().setLayout(new MigLayout("fillx, insets 0, hidemode 3", "[grow]16[grow]", "[]"));
    getBody().add(hoTenField, "span 2, growx, wrap, gapbottom 16");
    getBody().add(sdtField, "growx, gapbottom 16");
    getBody().add(emailField, "growx, wrap, gapbottom 16");
    getBody().add(ngayVaoLamField, "growx, gapbottom 16");
    getBody().add(tenDangNhapField, "growx, wrap, gapbottom 16");
    getBody().add(matKhauField, "span 2, growx, wrap, gapbottom 20");
    getBody().add(formError, "span 2, growx, wrap, gapbottom 16");
    getBody().add(new InfoCallout("Các trường có dấu * là bắt buộc. Mã nhân viên được tạo tự động khi lưu; "
        + "tài khoản có quyền nhân viên bán hàng và ở trạng thái hoạt động."), "span 2, growx, wrap");

    huyButton.addActionListener(e -> requestClose());
    luuButton.addActionListener(e -> submit());
    setPrimaryAction(luuButton);
    setActions(huyButton, luuButton);
  }

  private void submit() {
    if (busy) {
      return;
    }
    truongTheoTen.values().forEach(field -> field.setError(null));
    formError.setVisible(false);

    LocalDate ngayVaoLam;
    try {
      ngayVaoLam = LocalDate.parse(ngayVaoLamField.getText().trim(), NGAY_NHAP);
    } catch (DateTimeParseException e) {
      ngayVaoLamField.setError("Ngày vào làm phải theo dạng dd/MM/yyyy");
      return;
    }

    String hoTen = hoTenField.getText();
    String sdt = sdtField.getText();
    String email = emailField.getText();
    String tenDangNhap = tenDangNhapField.getText();
    char[] matKhau = ((JPasswordField) matKhauField.getInputComponent()).getPassword();

    setBusy(true);
    new SwingWorker<KetQuaLuuNhanVien, Void>() {
      @Override
      protected KetQuaLuuNhanVien doInBackground() {
        return controller.themNhanVien(hoTen, sdt, email, ngayVaoLam, tenDangNhap, matKhau);
      }

      @Override
      protected void done() {
        setBusy(false);
        try {
          showResult(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          showFormError(LOI_LUU);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof SecurityException) {
            showFormError(cause.getMessage());
          } else {
            log.warn("Lưu nhân viên thất bại do lỗi không mong đợi (exception={})",
                cause == null ? "none" : cause.getClass().getName());
            showFormError(LOI_LUU);
          }
        }
      }
    }.execute();
  }

  private void showResult(KetQuaLuuNhanVien ketQua) {
    if (ketQua.thanhCong()) {
      dispose();
      onSaved.accept(ketQua.nhanVien());
      return;
    }
    FormField field = ketQua.truongLoi() == null ? null : truongTheoTen.get(ketQua.truongLoi());
    if (field != null) {
      field.setError(ketQua.thongBao());
    } else {
      showFormError(ketQua.thongBao());
    }
  }

  private void showFormError(String message) {
    formError.setText(message);
    formError.setVisible(true);
    getBody().revalidate();
    getBody().repaint();
  }

  private void setBusy(boolean busy) {
    this.busy = busy;
    luuButton.setBusy(busy);
    huyButton.setEnabled(!busy);
  }

  @Override
  protected void requestClose() {
    if (!busy) {
      dispose();
    }
  }
}
