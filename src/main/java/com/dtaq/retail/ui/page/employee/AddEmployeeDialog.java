package com.dtaq.retail.ui.page.employee;

import com.dtaq.retail.controller.KiemTraHoSo;
import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.controller.NhanVienController.KetQuaLuuNhanVien;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.DatePicker;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.format.PhoneFormat;
import java.awt.Frame;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hộp thoại Thêm nhân viên bán hàng (UC013), dùng lại để cập nhật hồ sơ nhân viên đã có. Kiểm tra
 * tại chỗ (ngay khi rời ô) chỉ để phản hồi sớm; controller là nơi kiểm tra và quyết định cuối cùng.
 * Lỗi lưu giữ nguyên dữ liệu đã nhập để thử lại.
 */
final class AddEmployeeDialog extends ModalDialog {

  private static final Logger log = LoggerFactory.getLogger(AddEmployeeDialog.class);

  private static final String LOI_LUU = "Không thể lưu nhân viên. Vui lòng thử lại";

  private final NhanVienController controller;
  private final Consumer<NhanVien> onSaved;
  /** {@code null} khi thêm mới; hồ sơ đang sửa khi cập nhật. */
  private final NhanVien dangSua;
  private final FormField hoTenField = FormField.text("Họ và tên", "Ví dụ: Trần Thị Mai", "user-round");
  private final FormField sdtField = FormField.text("Số điện thoại", "Ví dụ: 0901 234 567", "phone");
  private final FormField emailField = FormField.text("Email", "ten@congty.vn", null);
  private final DatePicker ngayVaoLam = new DatePicker("Ngày vào làm");
  private final FormField tenDangNhapField =
      FormField.text("Tên đăng nhập", "Tự sinh theo mã khi lưu (ví dụ NVBH2026006)", "user-round");
  private final FormField matKhauField = FormField.password("Mật khẩu khởi tạo",
      "Tối thiểu " + NhanVienController.DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự");
  private final InfoCallout formError = new InfoCallout(InfoCallout.Kind.DANGER, "");
  private final DtaqButton luuButton;
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");
  private final Map<String, FormField> truongTheoTen;

  private boolean busy;

  AddEmployeeDialog(Frame parent, NhanVienController controller, Consumer<NhanVien> onSaved) {
    this(parent, controller, onSaved, null);
  }

  /** Hộp thoại cập nhật hồ sơ; tên đăng nhập chỉ hiển thị, mật khẩu không đổi ở đây. */
  static AddEmployeeDialog forEdit(
      Frame parent, NhanVienController controller, NhanVien nhanVien, Consumer<NhanVien> onSaved) {
    return new AddEmployeeDialog(parent, controller, onSaved, Objects.requireNonNull(nhanVien, "nhanVien"));
  }

  private AddEmployeeDialog(
      Frame parent, NhanVienController controller, Consumer<NhanVien> onSaved, NhanVien dangSua) {
    super(parent,
        dangSua == null ? "Thêm nhân viên bán hàng" : "Cập nhật nhân viên",
        dangSua == null ? "Tạo hồ sơ và tài khoản đăng nhập cho nhân viên mới."
            : dangSua.maNV() + " • " + dangSua.moTaQuyen() + " • "
                + (dangSua.trangThaiHoatDong() ? "Đang hoạt động" : "Đã khóa"),
        640);
    this.controller = Objects.requireNonNull(controller, "controller");
    this.onSaved = Objects.requireNonNull(onSaved, "onSaved");
    this.dangSua = dangSua;
    luuButton = DtaqButton.primary(dangSua == null ? "Lưu nhân viên" : "Lưu thay đổi");
    truongTheoTen = Map.of(
        NhanVienController.TRUONG_HO_TEN, hoTenField,
        NhanVienController.TRUONG_SDT, sdtField,
        NhanVienController.TRUONG_EMAIL, emailField,
        NhanVienController.TRUONG_NGAY_VAO_LAM, ngayVaoLam.field(),
        NhanVienController.TRUONG_TEN_DANG_NHAP, tenDangNhapField,
        NhanVienController.TRUONG_MAT_KHAU, matKhauField);

    hoTenField.setRequired(true);
    sdtField.setRequired(true);
    emailField.setRequired(true);
    ngayVaoLam.field().setRequired(true);
    matKhauField.setRequired(true);
    // Tên đăng nhập do hệ thống sinh từ mã nhân viên, không nhập tay.
    tenDangNhapField.getTextField().setEditable(false);
    formError.setVisible(false);
    installInlineValidation();

    getBody().setLayout(new MigLayout("fillx, insets 0, hidemode 3", "[grow,sg c]16[grow,sg c]", "[]"));
    getBody().add(hoTenField, "span 2, growx, wrap, gapbottom 16");
    getBody().add(sdtField, "growx, gapbottom 16");
    getBody().add(emailField, "growx, wrap, gapbottom 16");
    getBody().add(ngayVaoLam.field(), "growx, gapbottom 16");
    getBody().add(tenDangNhapField, "growx, wrap, gapbottom 16");
    getBody().add(matKhauField, "span 2, growx, wrap, gapbottom 20");
    getBody().add(formError, "span 2, growx, wrap, gapbottom 16");

    if (dangSua == null) {
      ngayVaoLam.setDate(LocalDate.now());
      getBody().add(new InfoCallout("Các trường có dấu * là bắt buộc. Mã nhân viên và tên đăng nhập được tạo tự động khi lưu; "
          + "tài khoản có quyền nhân viên bán hàng và ở trạng thái hoạt động."), "span 2, growx, wrap");
    } else {
      hoTenField.setText(dangSua.hoTen());
      sdtField.setText(PhoneFormat.format(dangSua.soDienThoai()));
      emailField.setText(dangSua.email());
      ngayVaoLam.setDate(dangSua.ngayVaoLam());
      tenDangNhapField.setText(dangSua.tenDangNhap());
      matKhauField.setVisible(false);
    }

    huyButton.addActionListener(e -> requestClose());
    luuButton.addActionListener(e -> submit());
    setPrimaryAction(luuButton);
    setActions(huyButton, luuButton);
  }

  /** Kiểm tra từng ô ngay khi rời ô; họ tên được chuẩn hoá và gợi ý tên đăng nhập. */
  private void installInlineValidation() {
    onBlur(hoTenField.getTextField(), () -> {
      if (hoTenField.getText().isBlank()) {
        return;
      }
      hoTenField.setText(KiemTraHoSo.chuanHoaHoTen(hoTenField.getText()));
      hoTenField.setError(KiemTraHoSo.kiemTraHoTen(hoTenField.getText()));
    });
    onBlur(sdtField.getTextField(), () -> {
      if (!sdtField.getText().isBlank()) {
        String loi = KiemTraHoSo.kiemTraSdt(sdtField.getText());
        sdtField.setError(loi);
        if (loi == null) {
          sdtField.setText(PhoneFormat.format(KiemTraHoSo.chuanHoaSdt(sdtField.getText())));
        }
      }
    });
    onBlur(emailField.getTextField(), () -> emailField.setError(emailField.getText().isBlank()
        ? "Email không được để trống" : KiemTraHoSo.kiemTraEmail(emailField.getText())));
    onBlur(ngayVaoLam.textField(), () -> {
      if (!ngayVaoLam.textField().getText().isBlank()) {
        LocalDate ngay = ngayVaoLam.getDate();
        ngayVaoLam.field().setError(ngay == null ? "Ngày không hợp lệ, nhập theo dạng dd/MM/yyyy"
            : NhanVienController.kiemTraNgayVaoLam(ngay));
      }
    });
  }

  private static void onBlur(JComponent component, Runnable action) {
    component.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        action.run();
      }
    });
  }

  private void submit() {
    if (busy) {
      return;
    }
    truongTheoTen.values().forEach(field -> field.setError(null));
    formError.setVisible(false);

    LocalDate ngay = ngayVaoLam.getDate();
    if (ngay == null) {
      ngayVaoLam.field().setError("Ngày vào làm phải theo dạng dd/MM/yyyy");
      return;
    }

    String hoTen = hoTenField.getText();
    String sdt = sdtField.getText();
    String email = emailField.getText();
    if (dangSua != null) {
      String maNV = dangSua.maNV();
      chay(() -> controller.capNhatNhanVien(maNV, hoTen, sdt, email, ngay));
      return;
    }
    char[] matKhau = ((JPasswordField) matKhauField.getInputComponent()).getPassword();
    chay(() -> controller.themNhanVien(hoTen, sdt, email, ngay, matKhau));
  }

  private void chay(Supplier<KetQuaLuuNhanVien> luu) {
    setBusy(true);
    new SwingWorker<KetQuaLuuNhanVien, Void>() {
      @Override
      protected KetQuaLuuNhanVien doInBackground() {
        return luu.get();
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
