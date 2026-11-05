package com.dtaq.retail.util;

import com.dtaq.retail.config.BackupConfig;
import com.dtaq.retail.config.DbConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gọi các công cụ dòng lệnh PostgreSQL ({@code pg_dump}, {@code pg_restore}, {@code createdb}).
 *
 * <p>Mật khẩu không bao giờ được đưa vào danh sách tham số; nó chỉ được truyền qua biến môi trường
 * {@code PGPASSWORD} của tiến trình con và không được ghi log.
 */
public class PgCommandExecutor {

  private static final Logger log = LoggerFactory.getLogger(PgCommandExecutor.class);

  private static final Duration TIMEOUT_SAO_LUU = Duration.ofMinutes(30);
  private static final Duration TIMEOUT_NGAN = Duration.ofMinutes(1);
  private static final int GIOI_HAN_KY_TU_DAU_RA = 64 * 1024;
  private static final int PORT_MAC_DINH = 5432;

  private final Path pgDump;
  private final Path pgRestore;
  private final Path createdb;
  private final String host;
  private final int port;
  private final String database;
  private final String user;
  private final String password;

  /** Kết quả chạy một tiến trình PostgreSQL. */
  public record KetQua(int exitCode, String output, boolean timeout) {

    /** {@code true} khi tiến trình kết thúc bình thường với mã thoát 0. */
    public boolean thanhCong() {
      return !timeout && exitCode == 0;
    }
  }

  /**
   * @param pgDump đường dẫn tới {@code pg_dump}
   * @param pgRestore đường dẫn tới {@code pg_restore}
   * @param createdb đường dẫn tới {@code createdb}
   * @param host máy chủ cơ sở dữ liệu
   * @param port cổng kết nối
   * @param database tên cơ sở dữ liệu
   * @param user tài khoản kết nối
   * @param password mật khẩu kết nối, có thể rỗng
   */
  public PgCommandExecutor(Path pgDump, Path pgRestore, Path createdb, String host, int port,
      String database, String user, String password) {
    this.pgDump = Objects.requireNonNull(pgDump, "pgDump");
    this.pgRestore = Objects.requireNonNull(pgRestore, "pgRestore");
    this.createdb = Objects.requireNonNull(createdb, "createdb");
    this.host = Objects.requireNonNull(host, "host");
    this.port = port;
    this.database = Objects.requireNonNull(database, "database");
    this.user = Objects.requireNonNull(user, "user");
    this.password = Objects.requireNonNull(password, "password");
  }

  /**
   * Dựng executor từ cấu hình kết nối cơ sở dữ liệu hiện có.
   *
   * <p>Đường dẫn công cụ lấy từ {@link BackupConfig#pgDump()}, {@link BackupConfig#pgRestore()} và
   * {@link BackupConfig#createdb()} để dùng chung chuỗi cấu hình system property → biến môi trường →
   * {@code db.properties} → mặc định, trong đó {@code DTAQ_PG_BIN_DIR} được ưu tiên.
   */
  public static PgCommandExecutor fromConfig() {
    String[] thongTin = parseJdbcUrl(DbConfig.jdbcUrl());
    return new PgCommandExecutor(
        BackupConfig.pgDump(),
        BackupConfig.pgRestore(),
        BackupConfig.createdb(),
        thongTin[0],
        Integer.parseInt(thongTin[1]),
        thongTin[2],
        DbConfig.username(),
        DbConfig.password());
  }

  /** Tên cơ sở dữ liệu đang được thao tác. */
  public String database() {
    return database;
  }

  /** Sao lưu toàn bộ cơ sở dữ liệu ra tệp định dạng nén của {@code pg_dump}. */
  public KetQua dump(Path output) {
    return chay(lenhDump(output), TIMEOUT_SAO_LUU);
  }

  /** Phục hồi cơ sở dữ liệu từ tệp sao lưu. */
  public KetQua restore(Path dump) {
    return chay(lenhRestore(dump), TIMEOUT_SAO_LUU);
  }

  /** Tạo cơ sở dữ liệu rỗng cùng tên cấu hình. */
  public KetQua createDatabase() {
    List<String> lenh = new ArrayList<>();
    lenh.add(createdb.toString());
    lenh.add("-h");
    lenh.add(host);
    lenh.add("-p");
    lenh.add(Integer.toString(port));
    lenh.add("-U");
    lenh.add(user);
    lenh.add(database);
    return chay(lenh, TIMEOUT_NGAN);
  }

  /** Liệt kê nội dung tệp sao lưu để kiểm tra tính hợp lệ. */
  public KetQua listContents(Path dump) {
    List<String> lenh = new ArrayList<>();
    lenh.add(pgRestore.toString());
    lenh.add("--list");
    lenh.add(Objects.requireNonNull(dump, "dump").toString());
    return chay(lenh, TIMEOUT_NGAN);
  }

  /** Đọc phiên bản của {@code pg_dump}. */
  public KetQua phienBan() {
    List<String> lenh = new ArrayList<>();
    lenh.add(pgDump.toString());
    lenh.add("--version");
    return chay(lenh, TIMEOUT_NGAN);
  }

  /** Danh sách tham số của lệnh sao lưu; tách riêng để kiểm thử không cần chạy tiến trình thật. */
  List<String> lenhDump(Path output) {
    List<String> lenh = new ArrayList<>();
    lenh.add(pgDump.toString());
    lenh.add("-Fc");
    lenh.add("--no-owner");
    lenh.add("--no-privileges");
    lenh.add("-h");
    lenh.add(host);
    lenh.add("-p");
    lenh.add(Integer.toString(port));
    lenh.add("-U");
    lenh.add(user);
    lenh.add("-d");
    lenh.add(database);
    lenh.add("-f");
    lenh.add(Objects.requireNonNull(output, "output").toString());
    return lenh;
  }

  /** Danh sách tham số của lệnh phục hồi; tách riêng để kiểm thử không cần chạy tiến trình thật. */
  List<String> lenhRestore(Path dump) {
    List<String> lenh = new ArrayList<>();
    lenh.add(pgRestore.toString());
    lenh.add("--clean");
    lenh.add("--if-exists");
    lenh.add("--no-owner");
    lenh.add("--no-privileges");
    lenh.add("--single-transaction");
    lenh.add("-h");
    lenh.add(host);
    lenh.add("-p");
    lenh.add(Integer.toString(port));
    lenh.add("-U");
    lenh.add(user);
    lenh.add("-d");
    lenh.add(database);
    lenh.add(Objects.requireNonNull(dump, "dump").toString());
    return lenh;
  }

  /**
   * Chạy một tiến trình, gộp luồng lỗi vào luồng ra và trả kết quả thay vì ném ngoại lệ.
   *
   * <p>Đầu ra được đọc trên luồng riêng để tiến trình không bị chặn khi bộ đệm đầy. Khi quá thời
   * gian chờ, tiến trình bị hủy cưỡng bức và kết quả có cờ {@code timeout}.
   */
  private KetQua chay(List<String> lenh, Duration timeout) {
    Objects.requireNonNull(lenh, "lenh");
    Objects.requireNonNull(timeout, "timeout");
    ProcessBuilder pb = new ProcessBuilder(lenh);
    pb.environment().put("PGPASSWORD", password);
    pb.redirectErrorStream(true);
    log.debug("Chạy lệnh PostgreSQL: {}", String.join(" ", lenh));

    Process tienTrinh;
    try {
      tienTrinh = pb.start();
    } catch (IOException e) {
      log.error("Không thể chạy {}: {}", lenh.get(0), e.getMessage());
      return new KetQua(-1,
          "Không thể chạy " + lenh.get(0) + ". Kiểm tra DTAQ_PG_BIN_DIR hoặc PATH: "
              + e.getMessage(),
          false);
    }

    StringBuilder dauRa = new StringBuilder();
    Thread luongDoc = new Thread(() -> docDauRa(tienTrinh, dauRa), "dtaq-pg-output-reader");
    luongDoc.setDaemon(true);
    luongDoc.start();

    boolean xong;
    try {
      xong = tienTrinh.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      tienTrinh.destroyForcibly();
      return new KetQua(-1, "Đã bị ngắt khi chờ " + lenh.get(0) + " kết thúc", false);
    }

    if (!xong) {
      tienTrinh.destroyForcibly();
      join(luongDoc, Duration.ofSeconds(5));
      log.warn("Lệnh {} vượt quá thời gian chờ {} phút", lenh.get(0), timeout.toMinutes());
      return new KetQua(-1, docChuoi(dauRa), true);
    }

    join(luongDoc, Duration.ofSeconds(10));
    int maThoat = tienTrinh.exitValue();
    if (maThoat != 0) {
      log.warn("Lệnh {} kết thúc với mã thoát {}", lenh.get(0), maThoat);
    }
    return new KetQua(maThoat, docChuoi(dauRa), false);
  }

  /** Đọc toàn bộ luồng ra của tiến trình, chỉ giữ tối đa {@link #GIOI_HAN_KY_TU_DAU_RA} ký tự. */
  private static void docDauRa(Process tienTrinh, StringBuilder dich) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(tienTrinh.getInputStream(), StandardCharsets.UTF_8))) {
      char[] boDem = new char[4096];
      int soKyTu;
      while ((soKyTu = reader.read(boDem)) >= 0) {
        synchronized (dich) {
          int conLai = GIOI_HAN_KY_TU_DAU_RA - dich.length();
          if (conLai > 0) {
            dich.append(boDem, 0, Math.min(soKyTu, conLai));
          }
        }
      }
    } catch (IOException e) {
      log.debug("Kết thúc đọc luồng tiến trình: {}", e.getMessage());
    }
  }

  private static String docChuoi(StringBuilder dauRa) {
    synchronized (dauRa) {
      return dauRa.toString();
    }
  }

  private static void join(Thread thread, Duration timeout) {
    try {
      thread.join(timeout.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Phân tích {@code jdbc:postgresql://host:port/db}; thiếu cổng thì dùng 5432.
   *
   * @return mảng {@code [host, port, database]}
   */
  static String[] parseJdbcUrl(String jdbcUrl) {
    Objects.requireNonNull(jdbcUrl, "jdbcUrl");
    String conLai = jdbcUrl.trim();
    int dauScheme = conLai.indexOf("://");
    if (dauScheme >= 0) {
      conLai = conLai.substring(dauScheme + 3);
    }
    int dauHoi = conLai.indexOf('?');
    if (dauHoi >= 0) {
      conLai = conLai.substring(0, dauHoi);
    }

    String phanHost;
    String tenCsdl = "postgres";
    int dauSlash = conLai.indexOf('/');
    if (dauSlash >= 0) {
      phanHost = conLai.substring(0, dauSlash);
      String phanCsdl = conLai.substring(dauSlash + 1);
      if (!phanCsdl.isBlank()) {
        tenCsdl = phanCsdl;
      }
    } else {
      phanHost = conLai;
    }

    int dauA = phanHost.lastIndexOf('@');
    if (dauA >= 0) {
      phanHost = phanHost.substring(dauA + 1);
    }

    String tenMay = "localhost";
    int cong = PORT_MAC_DINH;
    if (phanHost.startsWith("[")) {
      int dongNgoac = phanHost.indexOf(']');
      if (dongNgoac > 0) {
        tenMay = phanHost.substring(1, dongNgoac);
        String sauNgoac = phanHost.substring(dongNgoac + 1);
        if (sauNgoac.startsWith(":")) {
          cong = parsePort(sauNgoac.substring(1), cong);
        }
      }
    } else {
      int dauHaiCham = phanHost.lastIndexOf(':');
      if (dauHaiCham >= 0) {
        String phanMay = phanHost.substring(0, dauHaiCham);
        if (!phanMay.isBlank()) {
          tenMay = phanMay;
        }
        cong = parsePort(phanHost.substring(dauHaiCham + 1), cong);
      } else if (!phanHost.isBlank()) {
        tenMay = phanHost;
      }
    }
    return new String[] {tenMay, Integer.toString(cong), tenCsdl};
  }

  private static int parsePort(String giaTri, int macDinh) {
    try {
      int cong = Integer.parseInt(giaTri.trim());
      return cong > 0 ? cong : macDinh;
    } catch (NumberFormatException e) {
      log.warn("Cổng cơ sở dữ liệu không hợp lệ '{}'; dùng {}", giaTri, macDinh);
      return macDinh;
    }
  }
}
