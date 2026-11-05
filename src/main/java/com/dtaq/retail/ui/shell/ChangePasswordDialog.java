package com.dtaq.retail.ui.shell;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.util.PasswordHasher;
import java.awt.Frame;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;

/**
 * Hộp thoại Đổi mật khẩu dành cho người dùng hiện tại đang đăng nhập.
 */
public final class ChangePasswordDialog extends ModalDialog {

  private static final int DO_DAI_TOI_THIEU = 8;

  private final NhanVienDao nhanVienDao;
  private final NhanVien nhanVienHienTai;

  private final FormField matKhauCuField = FormField.password("Mật khẩu hiện tại", "Nhập mật khẩu đang dùng");
  private final FormField matKhauMoiField = FormField.password("Mật khẩu mới", "Tối thiểu " + DO_DAI_TOI_THIEU + " ký tự");
  private final FormField xacNhanField = FormField.password("Xác nhận mật khẩu mới", "Nhập lại mật khẩu mới");

  private final InfoCallout formError = new InfoCallout(InfoCallout.Kind.DANGER, "");
  private final DtaqButton luuButton = DtaqButton.primary("Đổi mật khẩu", "lock-keyhole");
  private final DtaqButton huyButton = DtaqButton.secondary("Hủy");

  private boolean busy;

  public ChangePasswordDialog(Frame parent, NhanVienDao nhanVienDao, NhanVien nhanVienHienTai) {
    super(parent, "Đổi mật khẩu", "Cập nhật mật khẩu đăng nhập cho tài khoản của bạn.", 440);
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
    this.nhanVienHienTai = Objects.requireNonNull(nhanVienHienTai, "nhanVienHienTai");

    formError.setVisible(false);
    getBody().add(formError, "growx, gapbottom 12, wrap");
    getBody().add(matKhauCuField, "growx, wrap");
    getBody().add(matKhauMoiField, "growx, wrap");
    getBody().add(xacNhanField, "growx, wrap");

    luuButton.addActionListener(e -> xuLyDoiMatKhau());
    huyButton.addActionListener(e -> requestClose());

    setActions(huyButton, luuButton);
    setPrimaryAction(luuButton);
  }

  private void xuLyDoiMatKhau() {
    if (busy) {
      return;
    }

    formError.setVisible(false);
    matKhauCuField.setError(null);
    matKhauMoiField.setError(null);
    xacNhanField.setError(null);

    char[] mkCu = getPasswordChars(matKhauCuField);
    char[] mkMoi = getPasswordChars(matKhauMoiField);
    char[] xacNhan = getPasswordChars(xacNhanField);

    boolean coLoi = false;

    if (mkCu == null || mkCu.length == 0) {
      matKhauCuField.setError("Vui lòng nhập mật khẩu hiện tại");
      coLoi = true;
    }

    if (mkMoi == null || mkMoi.length == 0) {
      matKhauMoiField.setError("Vui lòng nhập mật khẩu mới");
      coLoi = true;
    } else if (mkMoi.length < DO_DAI_TOI_THIEU) {
      matKhauMoiField.setError("Mật khẩu mới phải có ít nhất " + DO_DAI_TOI_THIEU + " ký tự");
      coLoi = true;
    } else if (PasswordHasher.quaDai(mkMoi)) {
      matKhauMoiField.setError("Mật khẩu mới quá dài (vượt quá 72 byte)");
      coLoi = true;
    } else if (String.valueOf(mkMoi).equalsIgnoreCase(nhanVienHienTai.tenDangNhap())) {
      matKhauMoiField.setError("Mật khẩu không được trùng tên đăng nhập");
      coLoi = true;
    }

    if (xacNhan == null || xacNhan.length == 0) {
      xacNhanField.setError("Vui lòng xác nhận mật khẩu mới");
      coLoi = true;
    } else if (mkMoi != null && !Arrays.equals(mkMoi, xacNhan)) {
      xacNhanField.setError("Mật khẩu xác nhận không khớp");
      coLoi = true;
    }

    if (coLoi) {
      clearPasswords(mkCu, mkMoi, xacNhan);
      return;
    }

    setBusy(true);

    new SwingWorker<String, Void>() {
      @Override
      protected String doInBackground() {
        try {
          var opt = nhanVienDao.findByMa(nhanVienHienTai.maNV());
          if (opt.isEmpty()) {
            return "Không tìm thấy thông tin tài khoản";
          }
          NhanVien dbNv = opt.get();
          if (!PasswordHasher.verify(mkCu, dbNv.matKhau())) {
            return "Mật khẩu hiện tại không đúng";
          }
          if (Arrays.equals(mkCu, mkMoi)) {
            return "Mật khẩu mới không được trùng mật khẩu hiện tại";
          }
          String hashMoi = PasswordHasher.hash(mkMoi);
          nhanVienDao.updateMatKhau(dbNv.maNV(), hashMoi);
          return null; // Thành công
        } catch (Exception ex) {
          return "Lỗi cập nhật mật khẩu: " + ex.getMessage();
        } finally {
          clearPasswords(mkCu, mkMoi, xacNhan);
        }
      }

      @Override
      protected void done() {
        setBusy(false);
        try {
          String err = get();
          if (err != null) {
            if ("Mật khẩu hiện tại không đúng".equals(err)) {
              matKhauCuField.setError(err);
            } else if (err.contains("trùng")) {
              matKhauMoiField.setError(err);
            } else {
              formError.setText(err);
              formError.setVisible(true);
            }
          } else {
            Toast.success(getOwner(), "Đổi mật khẩu thành công");
            requestClose();
          }
        } catch (Exception ex) {
          formError.setText("Không thể đổi mật khẩu. Vui lòng thử lại");
          formError.setVisible(true);
        }
      }
    }.execute();
  }

  private void setBusy(boolean busy) {
    this.busy = busy;
    luuButton.setEnabled(!busy);
    huyButton.setEnabled(!busy);
    matKhauCuField.getInputComponent().setEnabled(!busy);
    matKhauMoiField.getInputComponent().setEnabled(!busy);
    xacNhanField.getInputComponent().setEnabled(!busy);
  }

  private char[] getPasswordChars(FormField field) {
    if (field.getInputComponent() instanceof JPasswordField pf) {
      return pf.getPassword();
    }
    return null;
  }

  private void clearPasswords(char[]... arrays) {
    for (char[] arr : arrays) {
      if (arr != null) {
        Arrays.fill(arr, '\0');
      }
    }
  }
}
