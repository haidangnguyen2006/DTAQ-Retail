package com.dtaq.retail.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.util.BackupManifest;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử luồng sao lưu và kiểm tra tệp của {@link BackupService} với công cụ PostgreSQL giả. */
class BackupServiceTest {

  private static final String DUMP_THANH_CONG = """
      #!/bin/sh
      if [ "$1" = "--version" ]; then
        echo "pg_dump (PostgreSQL) 16.13"
        exit 0
      fi
      out=""
      while [ $# -gt 0 ]; do
        if [ "$1" = "-f" ]; then
          shift
          out="$1"
        fi
        shift
      done
      if [ -z "$out" ]; then
        echo "pg_dump: thiếu tham số -f" >&2
        exit 2
      fi
      printf 'DUMP-CONTENT' > "$out"
      exit 0
      """;

  private static final String DUMP_THAT_BAI = """
      #!/bin/sh
      if [ "$1" = "--version" ]; then
        echo "pg_dump (PostgreSQL) 16.13"
        exit 0
      fi
      echo "pg_dump: lỗi giả lập" >&2
      exit 1
      """;

  private static final String RESTORE_KIEM_TRA = """
      #!/bin/sh
      if [ "$1" = "--list" ]; then
        echo "; nội dung giả lập"
        exit 0
      fi
      exit 0
      """;

  @TempDir
  Path tempDir;

  private Path pgDump;
  private Path pgRestore;
  private BackupStore store;
  private DaoGia dao;
  private BackupService service;

  @BeforeEach
  void chuanBi() throws IOException {
    Assumptions.assumeFalse(
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"),
        "Kịch bản giả lập pg_dump cần shell POSIX");
    pgDump = tempDir.resolve("pg_dump");
    pgRestore = tempDir.resolve("pg_restore");
    taoScript(pgDump, DUMP_THANH_CONG);
    taoScript(pgRestore, RESTORE_KIEM_TRA);
    store = new BackupStore(tempDir.resolve("backups"), null);
    dao = new DaoGia();
    PgCommandExecutor pg = new PgCommandExecutor(pgDump, pgRestore,
        tempDir.resolve("createdb"), "localhost", 5432, "dtaq_retail", "dtaq", "mat-khau-gia");
    service = new BackupService(new NguonDuLieuGia(), store, pg, dao);
  }

  @Test
  @DisplayName("saoLuu tạo dump, metadata, SHA-256 và ghi lịch sử thành công")
  void saoLuuTaoTepVaGhiLichSu() throws IOException {
    BackupRecord banGhi = service.saoLuu(LoaiSaoLuu.MANUAL, "NV-1");

    assertTrue(banGhi.thanhCong());
    assertTrue(banGhi.filePath().isAbsolute(), "đường dẫn phải tuyệt đối");
    assertTrue(banGhi.fileName().startsWith("dtaq_"));
    assertTrue(banGhi.fileName().endsWith(".dump"));
    assertTrue(Files.exists(banGhi.filePath()));
    assertEquals(Files.size(banGhi.filePath()), banGhi.fileSize());
    assertEquals(BackupManifest.sha256(banGhi.filePath()), banGhi.sha256());
    assertTrue(Files.exists(BackupManifest.metaPathFor(banGhi.filePath())));
    assertEquals("pg_dump (PostgreSQL) 16.13", banGhi.phienBanPg());
    assertEquals("dtaq_retail", banGhi.tenCsdl());
    assertEquals("NV-1", banGhi.createdBy());

    assertEquals(1, dao.daGhi.size());
    assertEquals(TrangThaiSaoLuu.SUCCESS, dao.daGhi.get(0).trangThai());
    assertEquals(banGhi.filePath(), dao.daGhi.get(0).filePath());

    try (Stream<Path> luong = Files.list(store.thuMuc())) {
      assertTrue(luong.noneMatch(tep -> tep.getFileName().toString().endsWith(".tmp")),
          "không được để lại tệp .tmp");
    }
  }

  @Test
  @DisplayName("kiemTra phát hiện tệp bị thay đổi nội dung")
  void kiemTraPhatHienTepBiSua() throws IOException {
    BackupRecord banGhi = service.saoLuu(LoaiSaoLuu.MANUAL, "NV-1");
    Files.writeString(banGhi.filePath(), "TAMPERED-DAT", StandardCharsets.UTF_8);
    assertEquals(banGhi.fileSize(), Files.size(banGhi.filePath()),
        "kịch bản kiểm thử phải giữ nguyên kích thước để kiểm tra SHA-256");

    BackupException loi = assertThrows(BackupException.class, () -> service.kiemTra(banGhi));

    assertTrue(loi.getMessage().contains("SHA-256"), loi.getMessage());
  }

  @Test
  @DisplayName("pg_dump lỗi thì ghi metadata và lịch sử FAILED rồi ném BackupException")
  void saoLuuThatBaiGhiNhanFailed() throws IOException {
    taoScript(pgDump, DUMP_THAT_BAI);

    BackupException loi = assertThrows(BackupException.class,
        () -> service.saoLuu(LoaiSaoLuu.MANUAL, "NV-2"));

    assertTrue(loi.getMessage().startsWith("Sao lưu thất bại: "), loi.getMessage());
    assertTrue(loi.getMessage().contains("lỗi giả lập"), loi.getMessage());

    List<BackupRecord> danhSach = store.danhSach();
    assertEquals(1, danhSach.size());
    BackupRecord thatBai = danhSach.get(0);
    assertEquals(TrangThaiSaoLuu.FAILED, thatBai.trangThai());
    assertTrue(thatBai.thongBao().contains("lỗi giả lập"), thatBai.thongBao());
    assertEquals(0L, thatBai.fileSize());
    assertNull(thatBai.sha256());
    assertFalse(Files.exists(thatBai.filePath()), "không được để lại tệp dump thất bại");
    assertTrue(Files.exists(BackupManifest.metaPathFor(thatBai.filePath())));

    assertEquals(1, dao.daGhi.size());
    assertEquals(TrangThaiSaoLuu.FAILED, dao.daGhi.get(0).trangThai());

    try (Stream<Path> luong = Files.list(store.thuMuc())) {
      assertTrue(luong.noneMatch(tep -> tep.getFileName().toString().endsWith(".tmp")));
    }
  }

  @Test
  @DisplayName("dongBoLichSu chỉ thêm bản ghi còn thiếu")
  void dongBoLichSuBoSungBanThieu() throws IOException {
    service.saoLuu(LoaiSaoLuu.MANUAL, "NV-1");
    dao.daGhi.clear();

    service.dongBoLichSu();
    assertEquals(1, dao.daGhi.size());

    service.dongBoLichSu();
    assertEquals(1, dao.daGhi.size(), "không được thêm trùng bản ghi đã có");
  }

  private static void taoScript(Path script, String noiDung) throws IOException {
    Files.writeString(script, noiDung, StandardCharsets.UTF_8);
    Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
  }

  /** DAO lịch sử giả lưu trong bộ nhớ, không cần cơ sở dữ liệu thật. */
  private static final class DaoGia implements LichSuSaoLuuDao {

    private final List<BackupRecord> daGhi = new ArrayList<>();

    @Override
    public void insert(BackupRecord banGhi) {
      daGhi.add(banGhi);
    }

    @Override
    public List<BackupRecord> findAll() {
      return List.copyOf(daGhi);
    }

    @Override
    public Optional<BackupRecord> findMoiNhatThanhCong() {
      return daGhi.stream()
          .filter(BackupRecord::thanhCong)
          .max(Comparator.comparing(BackupRecord::createdAt));
    }

    @Override
    public boolean existsByFilePath(String filePath) {
      return daGhi.stream().anyMatch(banGhi -> banGhi.filePath().toString().equals(filePath));
    }
  }

  /** Nguồn dữ liệu giả: dịch vụ không mở kết nối nào vì DAO đã được thay bằng bản trong bộ nhớ. */
  private static final class NguonDuLieuGia implements DataSource {

    @Override
    public Connection getConnection() {
      throw new UnsupportedOperationException("Không dùng cơ sở dữ liệu trong kiểm thử");
    }

    @Override
    public Connection getConnection(String username, String password) {
      throw new UnsupportedOperationException("Không dùng cơ sở dữ liệu trong kiểm thử");
    }

    @Override
    public PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public void setLoginTimeout(int seconds) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException("Không dùng trong kiểm thử");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException("Không dùng trong kiểm thử");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
