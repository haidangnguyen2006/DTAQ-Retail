package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử cấu trúc tham số của {@link PgCommandExecutor} mà không chạy tiến trình thật. */
class PgCommandExecutorTest {

  private static final String MAT_KHAU = "mat-khau-bi-mat-123";

  private final PgCommandExecutor executor = new PgCommandExecutor(
      Path.of("pg_dump"),
      Path.of("pg_restore"),
      Path.of("createdb"),
      "localhost",
      5432,
      "dtaq_retail",
      "postgres",
      MAT_KHAU);

  @Test
  @DisplayName("Lệnh sao lưu có định dạng nén, bỏ chủ sở hữu/quyền và trỏ đúng tệp đích")
  void lenhDumpDayDuThamSo() {
    Path dich = Path.of("/tmp/dtaq-test.dump");

    List<String> lenh = executor.lenhDump(dich);

    assertEquals("pg_dump", lenh.get(0));
    assertTrue(lenh.contains("-Fc"), "phải dùng định dạng nén -Fc");
    assertTrue(lenh.contains("--no-owner"), "phải bỏ thông tin chủ sở hữu");
    assertTrue(lenh.contains("--no-privileges"), "phải bỏ thông tin quyền");
    assertTrue(lenh.contains("-f"), "phải chỉ định tệp đích bằng -f");
    assertEquals(dich.toString(), lenh.get(lenh.indexOf("-f") + 1));
    assertTrue(lenh.contains("-d"), "phải chỉ định cơ sở dữ liệu");
    assertEquals("dtaq_retail", lenh.get(lenh.indexOf("-d") + 1));
    assertEquals("5432", lenh.get(lenh.indexOf("-p") + 1));
    assertEquals("postgres", lenh.get(lenh.indexOf("-U") + 1));
    assertEquals("localhost", lenh.get(lenh.indexOf("-h") + 1));
  }

  @Test
  @DisplayName("Lệnh phục hồi làm sạch, chạy trong một giao dịch và không lộ mật khẩu")
  void lenhRestoreDayDuThamSo() {
    Path nguon = Path.of("/tmp/dtaq-test.dump");

    List<String> lenh = executor.lenhRestore(nguon);

    assertEquals("pg_restore", lenh.get(0));
    assertTrue(lenh.contains("--clean"), "phải xóa đối tượng cũ");
    assertTrue(lenh.contains("--if-exists"), "phải dùng --if-exists cùng --clean");
    assertTrue(lenh.contains("--single-transaction"), "phải chạy trong một giao dịch");
    assertEquals(nguon.toString(), lenh.get(lenh.size() - 1));
    assertFalse(lenh.stream().anyMatch(thamSo -> thamSo.contains(MAT_KHAU)),
        "mật khẩu không được xuất hiện trong argv");
  }

  @Test
  @DisplayName("Mật khẩu không bao giờ nằm trong danh sách tham số của pg_dump")
  void lenhDumpKhongLoMatKhau() {
    List<String> lenh = executor.lenhDump(Path.of("/tmp/dtaq-test.dump"));

    assertFalse(lenh.contains(MAT_KHAU));
    assertFalse(lenh.contains("PGPASSWORD"));
    assertFalse(lenh.stream().anyMatch(thamSo -> thamSo.contains(MAT_KHAU)));
  }

  @Test
  @DisplayName("Tên cơ sở dữ liệu được công bố qua getter")
  void tenCoSoDuLieu() {
    assertEquals("dtaq_retail", executor.database());
  }
}
