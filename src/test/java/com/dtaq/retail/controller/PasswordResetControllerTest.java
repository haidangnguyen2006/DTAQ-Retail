package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.MailDeliveryException;
import com.dtaq.retail.util.Mailer;
import com.dtaq.retail.util.PasswordHasher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests for {@link PasswordResetController}. */
class PasswordResetControllerTest {

  private static final String USERNAME = "nguyenhaidang";
  private static final String MA_NV = "NVQUANLY-2026-000001";
  private static final String EMAIL = "nguyenhaidangnt2006@gmail.com";
  private static final Instant NOW = Instant.parse("2026-10-20T00:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void unknownUserSendsNothingAndStaysSilent() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.empty());
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();

    new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED).guiMa(USERNAME);

    assertEquals(0, mailer.daGui.size());
    assertTrue(resetDao.tickets.isEmpty());
  }

  @Test
  void accountWithoutEmailSendsNothing() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee("   ", true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();

    new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED).guiMa(USERNAME);

    assertEquals(0, mailer.daGui.size());
    assertTrue(resetDao.tickets.isEmpty());
  }

  @Test
  void lockedAccountSendsNothing() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, false)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();

    new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED).guiMa(USERNAME);

    assertEquals(0, mailer.daGui.size());
    assertTrue(resetDao.tickets.isEmpty());
  }

  @Test
  void validAccountSendsSixDigitCodeAndStoresOnlyHash() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();

    new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED).guiMa(USERNAME);

    assertEquals(1, mailer.daGui.size());
    FakeMailer.DaGui daGui = mailer.daGui.get(0);
    assertEquals(EMAIL, daGui.toEmail());
    assertTrue(daGui.code().matches("\\d{6}"));

    PasswordResetDao.Ticket ticket = resetDao.tickets.get(MA_NV);
    assertTrue(ticket.codeHash().matches("[0-9a-f]{64}"));
    assertNotEquals(daGui.code(), ticket.codeHash());
    assertEquals(0, ticket.soLanThu());
  }

  @Test
  void correctCodeResetsPasswordAndDeletesTicket() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);
    String code = mailer.daGui.get(0).code();
    String matKhauMoi = "MatKhauMoi2026";

    PasswordResetController.KetQua ketQua =
        controller.datLaiMatKhau(USERNAME, code, matKhauMoi);

    assertTrue(ketQua.thanhCong());
    assertEquals(PasswordResetController.KetQuaDatLai.SUCCESS, ketQua.trangThai());
    assertTrue(resetDao.hashMoi.startsWith("$2a$10$"));
    assertTrue(PasswordHasher.verify(matKhauMoi.toCharArray(), resetDao.hashMoi));
    assertFalse(resetDao.tickets.containsKey(MA_NV));
  }

  @Test
  void expiredCodeRejected() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();

    new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED).guiMa(USERNAME);
    String code = mailer.daGui.get(0).code();

    Clock sau6Phut = Clock.offset(FIXED, Duration.ofMinutes(6));
    PasswordResetController.KetQua ketQua =
        new PasswordResetController(nhanVienDao, resetDao, mailer, sau6Phut)
            .datLaiMatKhau(USERNAME, code, "MatKhauMoi2026");

    assertEquals(PasswordResetController.KetQuaDatLai.CODE_EXPIRED, ketQua.trangThai());
    assertFalse(resetDao.tickets.containsKey(MA_NV));
  }

  @Test
  void wrongCodeIncrementsAttempts() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);
    String code = mailer.daGui.get(0).code();
    String maSai = code.equals("000000") ? "111111" : "000000";

    PasswordResetController.KetQua ketQua =
        controller.datLaiMatKhau(USERNAME, maSai, "MatKhauMoi2026");

    assertEquals(PasswordResetController.KetQuaDatLai.INVALID_CODE, ketQua.trangThai());
    assertEquals(1, resetDao.tickets.get(MA_NV).soLanThu());
  }

  @Test
  void fifthWrongAttemptInvalidatesCode() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);
    String code = mailer.daGui.get(0).code();
    String maSai = code.equals("000000") ? "111111" : "000000";

    PasswordResetController.KetQua ketQua = null;
    for (int lan = 1; lan <= 5; lan++) {
      ketQua = controller.datLaiMatKhau(USERNAME, maSai, "MatKhauMoi2026");
    }

    assertEquals(
        PasswordResetController.KetQuaDatLai.TOO_MANY_ATTEMPTS, ketQua.trangThai());
    assertFalse(resetDao.tickets.containsKey(MA_NV));
  }

  @Test
  void usedCodeCannotBeReused() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);
    String code = mailer.daGui.get(0).code();
    assertTrue(controller.datLaiMatKhau(USERNAME, code, "MatKhauMoi2026").thanhCong());

    PasswordResetController.KetQua ketQua =
        controller.datLaiMatKhau(USERNAME, code, "MatKhauKhac2026");

    assertEquals(PasswordResetController.KetQuaDatLai.INVALID_CODE, ketQua.trangThai());
  }

  @Test
  void resendWithinCooldownDoesNotSendAgain() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);
    controller.guiMa(USERNAME);

    assertEquals(1, mailer.daGui.size());
  }

  @Test
  void weakPasswordRejected() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);
    String code = mailer.daGui.get(0).code();

    assertEquals(
        PasswordResetController.KetQuaDatLai.WEAK_PASSWORD,
        controller.datLaiMatKhau(USERNAME, code, "1234567").trangThai());
    assertEquals(
        PasswordResetController.KetQuaDatLai.WEAK_PASSWORD,
        controller.datLaiMatKhau(USERNAME, code, USERNAME).trangThai());
    assertTrue(resetDao.tickets.containsKey(MA_NV));
  }

  @Test
  void mailFailureKeepsSilentAndRemovesTicket() {
    FakeNhanVienDao nhanVienDao = new FakeNhanVienDao(Optional.of(employee(EMAIL, true)));
    FakePasswordResetDao resetDao = new FakePasswordResetDao();
    FakeMailer mailer = new FakeMailer();
    mailer.loi = new MailDeliveryException("Không thể gửi email xác nhận");
    PasswordResetController controller =
        new PasswordResetController(nhanVienDao, resetDao, mailer, FIXED);

    controller.guiMa(USERNAME);

    assertTrue(resetDao.findTicket(MA_NV).isEmpty());
  }

  private static NhanVien employee(String email, boolean active) {
    return new NhanVien(
        MA_NV,
        "Nguyễn Hải Đăng",
        "0901234567",
        email,
        LocalDate.of(2026, 1, 5),
        USERNAME,
        PasswordHasher.hash("12345678".toCharArray()),
        LoaiQuyen.NHAN_VIEN_QUAN_LY,
        active);
  }

  private static final class FakeNhanVienDao implements NhanVienDao {

    private final Optional<NhanVien> ketQua;
    private int soLanGoi;

    private FakeNhanVienDao(Optional<NhanVien> ketQua) {
      this.ketQua = ketQua;
    }

    @Override
    public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
      soLanGoi++;
      return ketQua;
    }

    @Override
    public void updateMatKhau(String maNV, String matKhauHash) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử đặt lại mật khẩu");
    }
  }

  private static final class FakePasswordResetDao implements PasswordResetDao {

    private final Map<String, Ticket> tickets = new HashMap<>();
    private String hashMoi;

    @Override
    public void saveTicket(Ticket ticket) {
      tickets.put(ticket.maNV(), ticket);
    }

    @Override
    public Optional<Ticket> findTicket(String maNV) {
      return Optional.ofNullable(tickets.get(maNV));
    }

    @Override
    public void updateAttempts(String maNV, int soLanThu) {
      Ticket ticket = tickets.get(maNV);
      if (ticket == null) {
        return;
      }
      tickets.put(maNV, new Ticket(
          ticket.maNV(),
          ticket.codeHash(),
          ticket.createdAt(),
          ticket.expiresAt(),
          soLanThu,
          ticket.lastSentAt()));
    }

    @Override
    public void deleteTicket(String maNV) {
      tickets.remove(maNV);
    }

    @Override
    public void consume(String maNV, String newPasswordHash) {
      hashMoi = newPasswordHash;
      tickets.remove(maNV);
    }
  }

  private static final class FakeMailer implements Mailer {

    private final List<DaGui> daGui = new ArrayList<>();
    private RuntimeException loi;

    @Override
    public void sendPasswordResetCode(String toEmail, String code) {
      if (loi != null) {
        throw loi;
      }
      daGui.add(new DaGui(toEmail, code));
    }

    private record DaGui(String toEmail, String code) {}
  }
}
