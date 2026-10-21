package com.dtaq.retail.controller;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.Mailer;
import com.dtaq.retail.util.OtpCodes;
import com.dtaq.retail.util.PasswordHasher;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Xử lý nghiệp vụ quên mật khẩu bằng mã xác nhận gửi qua email. */
public final class PasswordResetController {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

  private static final int DO_DAI_MAT_KHAU_TOI_THIEU = 8;

  private final NhanVienDao nhanVienDao;
  private final PasswordResetDao passwordResetDao;
  private final Mailer mailer;
  private final Clock clock;

  /**
   * Khởi tạo bộ điều khiển đặt lại mật khẩu.
   *
   * @param nhanVienDao lớp truy cập tài khoản nhân viên; không được {@code null}
   * @param passwordResetDao lớp truy cập phiếu đặt lại mật khẩu; không được {@code null}
   * @param mailer bộ gửi email xác nhận; không được {@code null}
   * @param clock nguồn thời gian dùng để kiểm tra hiệu lực và thời gian chờ; không được {@code null}
   */
  public PasswordResetController(
      NhanVienDao nhanVienDao,
      PasswordResetDao passwordResetDao,
      Mailer mailer,
      Clock clock) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
    this.passwordResetDao = Objects.requireNonNull(passwordResetDao, "passwordResetDao");
    this.mailer = Objects.requireNonNull(mailer, "mailer");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Gửi mã xác nhận tới email đã đăng ký của tài khoản.
   *
   * <p>Không bao giờ ném ngoại lệ và không tiết lộ tài khoản hay email có tồn tại hay không.
   *
   * @param tenDangNhap tên đăng nhập do người dùng nhập
   */
  public void guiMa(String tenDangNhap) {
    try {
      if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
        return;
      }
      Optional<NhanVien> timThay = nhanVienDao.findByTenDangNhap(tenDangNhap.trim());
      if (timThay.isEmpty()) {
        return;
      }
      NhanVien nhanVien = timThay.get();
      if (!nhanVien.trangThaiHoatDong()) {
        return;
      }
      String email = nhanVien.email();
      if (email == null || email.isBlank()) {
        return;
      }

      Instant now = Instant.now(clock);
      Optional<PasswordResetDao.Ticket> phieuHienCo =
          passwordResetDao.findTicket(nhanVien.maNV());
      if (phieuHienCo.isPresent()) {
        Instant lanGuiCuoi = phieuHienCo.get().lastSentAt();
        if (lanGuiCuoi != null
            && lanGuiCuoi.plus(OtpCodes.THOI_GIAN_CHO_GUI_LAI).isAfter(now)) {
          return;
        }
      }

      String ma = OtpCodes.sinhMa();
      passwordResetDao.saveTicket(new PasswordResetDao.Ticket(
          nhanVien.maNV(),
          OtpCodes.bam(ma),
          now,
          now.plus(OtpCodes.THOI_HAN),
          0,
          now));

      try {
        mailer.sendPasswordResetCode(email, ma);
      } catch (RuntimeException e) {
        xoaPhieuQuietly(nhanVien.maNV());
        log.warn("Không gửi được email xác nhận (exception={})", e.getClass().getName());
      }
    } catch (RuntimeException e) {
      log.warn("Không thể xử lý yêu cầu đặt lại mật khẩu (exception={})",
          e.getClass().getName());
    }
  }

  /**
   * Đặt mật khẩu mới khi mã xác nhận hợp lệ.
   *
   * @param tenDangNhap tên đăng nhập do người dùng nhập
   * @param ma mã xác nhận do người dùng nhập
   * @param matKhauMoi mật khẩu mới dạng thô
   * @return kết quả đặt lại mật khẩu; không bao giờ {@code null}
   */
  public KetQua datLaiMatKhau(String tenDangNhap, String ma, String matKhauMoi) {
    try {
      if (tenDangNhap == null || tenDangNhap.trim().isEmpty()
          || ma == null || ma.trim().isEmpty()) {
        return new KetQua(KetQuaDatLai.INVALID_CODE);
      }
      String tenDangNhapDaNhap = tenDangNhap.trim();
      String maDaNhap = ma.trim();

      Optional<NhanVien> timThay = nhanVienDao.findByTenDangNhap(tenDangNhapDaNhap);
      if (timThay.isEmpty()) {
        return new KetQua(KetQuaDatLai.INVALID_CODE);
      }
      NhanVien nhanVien = timThay.get();

      Optional<PasswordResetDao.Ticket> timPhieu = passwordResetDao.findTicket(nhanVien.maNV());
      if (timPhieu.isEmpty()) {
        return new KetQua(KetQuaDatLai.INVALID_CODE);
      }
      PasswordResetDao.Ticket phieu = timPhieu.get();

      Instant now = Instant.now(clock);
      if (now.isAfter(phieu.expiresAt())) {
        passwordResetDao.deleteTicket(nhanVien.maNV());
        return new KetQua(KetQuaDatLai.CODE_EXPIRED);
      }

      if (phieu.soLanThu() >= OtpCodes.SO_LAN_THU_TOI_DA) {
        passwordResetDao.deleteTicket(nhanVien.maNV());
        return new KetQua(KetQuaDatLai.TOO_MANY_ATTEMPTS);
      }

      if (!OtpCodes.khop(maDaNhap, phieu.codeHash())) {
        int lanThu = phieu.soLanThu() + 1;
        passwordResetDao.updateAttempts(nhanVien.maNV(), lanThu);
        if (lanThu >= OtpCodes.SO_LAN_THU_TOI_DA) {
          passwordResetDao.deleteTicket(nhanVien.maNV());
          return new KetQua(KetQuaDatLai.TOO_MANY_ATTEMPTS);
        }
        return new KetQua(KetQuaDatLai.INVALID_CODE);
      }

      if (matKhauMoi == null
          || matKhauMoi.length() < DO_DAI_MAT_KHAU_TOI_THIEU
          || matKhauMoi.equalsIgnoreCase(nhanVien.tenDangNhap())) {
        return new KetQua(KetQuaDatLai.WEAK_PASSWORD);
      }

      passwordResetDao.consume(
          nhanVien.maNV(), PasswordHasher.hash(matKhauMoi.toCharArray()));
      return new KetQua(KetQuaDatLai.SUCCESS);
    } catch (DataAccessException e) {
      log.warn("Không thể đặt lại mật khẩu (exception={})", e.getClass().getName());
      return new KetQua(KetQuaDatLai.DATA_ERROR);
    }
  }

  private void xoaPhieuQuietly(String maNV) {
    try {
      passwordResetDao.deleteTicket(maNV);
    } catch (RuntimeException e) {
      log.warn("Không thể xóa phiếu đặt lại mật khẩu (exception={})", e.getClass().getName());
    }
  }

  /** Trạng thái của một lần đặt lại mật khẩu. */
  public enum KetQuaDatLai {
    SUCCESS,
    INVALID_CODE,
    CODE_EXPIRED,
    TOO_MANY_ATTEMPTS,
    WEAK_PASSWORD,
    DATA_ERROR
  }

  /** Kết quả đặt lại mật khẩu. */
  public record KetQua(KetQuaDatLai trangThai) {

    /** Returns true when the password was reset successfully. */
    public boolean thanhCong() {
      return trangThai == KetQuaDatLai.SUCCESS;
    }
  }
}
