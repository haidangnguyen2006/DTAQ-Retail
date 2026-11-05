package com.dtaq.retail.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đọc cấu hình sao lưu cơ sở dữ liệu theo thứ tự ưu tiên: system property, biến môi trường, tệp
 * {@code db.properties}, rồi giá trị mặc định.
 *
 * <p>Các khóa {@code DTAQ_BACKUP_*} điều khiển thư mục, lịch chạy và thời hạn giữ bản sao lưu;
 * các khóa {@code DTAQ_PG_*} chỉ đường dẫn tới công cụ dòng lệnh PostgreSQL. Giá trị số hoặc giờ
 * không hợp lệ được ghi cảnh báo và thay bằng mặc định, không làm ứng dụng dừng.
 */
public final class BackupConfig {

  private static final Logger log = LoggerFactory.getLogger(BackupConfig.class);

  /** Thư mục chứa các tệp {@code *.dump} và {@code *.meta.json}. */
  public static final String KEY_THU_MUC = "DTAQ_BACKUP_DIR";

  /** Số ngày giữ một bản sao lưu trước khi bị dọn. */
  public static final String KEY_SO_NGAY_GIU = "DTAQ_BACKUP_KEEP_DAYS";

  /** Giờ chạy sao lưu tự động trong ngày, định dạng {@code HH:mm}. */
  public static final String KEY_GIO_CHAY = "DTAQ_BACKUP_TIME";

  /** Chu kỳ chạy bù tính bằng giờ. */
  public static final String KEY_CHU_KY_GIO = "DTAQ_BACKUP_INTERVAL_HOURS";

  /** Bật/tắt sao lưu tự động. */
  public static final String KEY_BAT_TAT = "DTAQ_BACKUP_ENABLED";

  /** Thư mục phụ để sao chép thêm bản sao lưu; không bắt buộc. */
  public static final String KEY_THU_MUC_PHU = "DTAQ_BACKUP_SECONDARY_DIR";

  /** Thư mục chứa {@code pg_dump}, {@code pg_restore}, {@code createdb}; không bắt buộc. */
  public static final String KEY_PG_BIN_DIR = "DTAQ_PG_BIN_DIR";

  /** Tên hoặc đường dẫn công cụ {@code pg_dump}. */
  public static final String KEY_PG_DUMP = "DTAQ_PG_DUMP";

  /** Tên hoặc đường dẫn công cụ {@code pg_restore}. */
  public static final String KEY_PG_RESTORE = "DTAQ_PG_RESTORE";

  /** Tên hoặc đường dẫn công cụ {@code createdb}. */
  public static final String KEY_PG_CREATEDB = "DTAQ_PG_CREATEDB";

  private static final String MAC_DINH_THU_MUC = "./backups";
  private static final int MAC_DINH_SO_NGAY_GIU = 7;
  private static final String MAC_DINH_GIO_CHAY = "00:00";
  private static final int MAC_DINH_CHU_KY_GIO = 24;
  private static final boolean MAC_DINH_BAT_TAT = true;
  private static final String MAC_DINH_PG_DUMP = "pg_dump";
  private static final String MAC_DINH_PG_RESTORE = "pg_restore";
  private static final String MAC_DINH_PG_CREATEDB = "createdb";

  private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");

  private static Path propertiesPath = Paths.get("db.properties");
  private static volatile Properties fileProps;

  private BackupConfig() {
  }

  /**
   * Thư mục chứa bản sao lưu.
   *
   * @return đường dẫn cấu hình, mặc định {@code ./backups}
   */
  public static Path thuMucBackup() {
    String value = giaTriHoacMacDinh(KEY_THU_MUC, MAC_DINH_THU_MUC);
    try {
      return Path.of(value);
    } catch (InvalidPathException e) {
      log.warn("{} = '{}' không phải đường dẫn hợp lệ; dùng {}", KEY_THU_MUC, value, MAC_DINH_THU_MUC);
      return Path.of(MAC_DINH_THU_MUC);
    }
  }

  /**
   * Thư mục phụ để sao chép thêm bản sao lưu.
   *
   * @return đường dẫn cấu hình, hoặc rỗng khi không cấu hình
   */
  public static Optional<Path> thuMucPhu() {
    String value = resolve(KEY_THU_MUC_PHU);
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Path.of(value.trim()));
    } catch (InvalidPathException e) {
      log.warn("{} = '{}' không phải đường dẫn hợp lệ; bỏ qua thư mục phụ", KEY_THU_MUC_PHU, value);
      return Optional.empty();
    }
  }

  /**
   * Số ngày giữ một bản sao lưu.
   *
   * @return số ngày dương, mặc định {@code 7}
   */
  public static int soNgayGiu() {
    String value = resolve(KEY_SO_NGAY_GIU);
    if (value == null || value.isBlank()) {
      return MAC_DINH_SO_NGAY_GIU;
    }
    try {
      int soNgay = Integer.parseInt(value.trim());
      if (soNgay <= 0) {
        log.warn("{} = {} không dương; dùng {}", KEY_SO_NGAY_GIU, soNgay, MAC_DINH_SO_NGAY_GIU);
        return MAC_DINH_SO_NGAY_GIU;
      }
      return soNgay;
    } catch (NumberFormatException e) {
      log.warn("{} = '{}' không phải số nguyên; dùng {}", KEY_SO_NGAY_GIU, value, MAC_DINH_SO_NGAY_GIU);
      return MAC_DINH_SO_NGAY_GIU;
    }
  }

  /**
   * Giờ chạy sao lưu tự động trong ngày.
   *
   * @return giờ cấu hình, mặc định {@code 00:00}
   */
  public static LocalTime gioChay() {
    String value = resolve(KEY_GIO_CHAY);
    if (value == null || value.isBlank()) {
      return LocalTime.parse(MAC_DINH_GIO_CHAY, DINH_DANG_GIO);
    }
    try {
      return LocalTime.parse(value.trim(), DINH_DANG_GIO);
    } catch (DateTimeParseException e) {
      log.warn("{} = '{}' không đúng định dạng HH:mm; dùng {}", KEY_GIO_CHAY, value, MAC_DINH_GIO_CHAY);
      return LocalTime.parse(MAC_DINH_GIO_CHAY, DINH_DANG_GIO);
    }
  }

  /**
   * Chu kỳ chạy bù tính bằng giờ.
   *
   * @return số giờ dương, mặc định {@code 24}
   */
  public static int chuKyGio() {
    String value = resolve(KEY_CHU_KY_GIO);
    if (value == null || value.isBlank()) {
      return MAC_DINH_CHU_KY_GIO;
    }
    try {
      int soGio = Integer.parseInt(value.trim());
      if (soGio <= 0) {
        log.warn("{} = {} không dương; dùng {}", KEY_CHU_KY_GIO, soGio, MAC_DINH_CHU_KY_GIO);
        return MAC_DINH_CHU_KY_GIO;
      }
      return soGio;
    } catch (NumberFormatException e) {
      log.warn("{} = '{}' không phải số nguyên; dùng {}", KEY_CHU_KY_GIO, value, MAC_DINH_CHU_KY_GIO);
      return MAC_DINH_CHU_KY_GIO;
    }
  }

  /**
   * Cho biết sao lưu tự động có được bật hay không.
   *
   * @return {@code true}/{@code false} theo cấu hình; giá trị lạ được hiểu là {@code true}
   */
  public static boolean batTat() {
    String value = resolve(KEY_BAT_TAT);
    if (value == null || value.isBlank()) {
      return MAC_DINH_BAT_TAT;
    }
    String chuan = value.trim();
    if ("true".equalsIgnoreCase(chuan)) {
      return true;
    }
    if ("false".equalsIgnoreCase(chuan)) {
      return false;
    }
    log.warn("{} = '{}' không phải true/false; dùng {}", KEY_BAT_TAT, value, MAC_DINH_BAT_TAT);
    return MAC_DINH_BAT_TAT;
  }

  /**
   * Đường dẫn công cụ {@code pg_dump}.
   *
   * @return đường dẫn tuyệt đối trong {@code DTAQ_PG_BIN_DIR}, tên cấu hình, hoặc tên trên PATH
   */
  public static Path pgDump() {
    return congCu(KEY_PG_DUMP, MAC_DINH_PG_DUMP);
  }

  /**
   * Đường dẫn công cụ {@code pg_restore}.
   *
   * @return đường dẫn tuyệt đối trong {@code DTAQ_PG_BIN_DIR}, tên cấu hình, hoặc tên trên PATH
   */
  public static Path pgRestore() {
    return congCu(KEY_PG_RESTORE, MAC_DINH_PG_RESTORE);
  }

  /**
   * Đường dẫn công cụ {@code createdb}.
   *
   * @return đường dẫn tuyệt đối trong {@code DTAQ_PG_BIN_DIR}, tên cấu hình, hoặc tên trên PATH
   */
  public static Path createdb() {
    return congCu(KEY_PG_CREATEDB, MAC_DINH_PG_CREATEDB);
  }

  private static Path congCu(String khoa, String tenMacDinh) {
    String ten = laWindows() ? tenMacDinh + ".exe" : tenMacDinh;
    String thuMuc = resolve(KEY_PG_BIN_DIR);
    if (thuMuc != null && !thuMuc.isBlank()) {
      try {
        return Path.of(thuMuc.trim(), ten);
      } catch (InvalidPathException e) {
        log.warn("{} = '{}' không phải đường dẫn hợp lệ; dùng PATH", KEY_PG_BIN_DIR, thuMuc);
      }
    }
    String giaTri = resolve(khoa);
    if (giaTri != null && !giaTri.isBlank()) {
      try {
        return Path.of(giaTri.trim());
      } catch (InvalidPathException e) {
        log.warn("{} = '{}' không phải đường dẫn hợp lệ; dùng {}", khoa, giaTri, ten);
      }
    }
    return Path.of(ten);
  }

  private static boolean laWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }

  private static String giaTriHoacMacDinh(String key, String macDinh) {
    String value = resolve(key);
    return value == null || value.isBlank() ? macDinh : value.trim();
  }

  private static String resolve(String key) {
    String value = System.getProperty(key);
    if (value == null) {
      value = System.getenv(key);
    }
    if (value == null) {
      value = fileProperties().getProperty(key);
    }
    return value;
  }

  private static Properties fileProperties() {
    Properties current = fileProps;
    if (current == null) {
      synchronized (BackupConfig.class) {
        current = fileProps;
        if (current == null) {
          current = load(propertiesPath);
          fileProps = current;
        }
      }
    }
    return current;
  }

  private static Properties load(Path path) {
    Properties properties = new Properties();
    if (!Files.isReadable(path)) {
      log.info("Không tìm thấy tệp cấu hình sao lưu tại {}", path.toAbsolutePath());
      return properties;
    }
    try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      properties.load(reader);
      log.info("Đã nạp cấu hình sao lưu từ {}", path.toAbsolutePath());
    } catch (IOException e) {
      log.warn("Không nạp được cấu hình sao lưu từ {}: {}",
          path.toAbsolutePath(), e.getMessage());
    }
    return properties;
  }

  /**
   * Đổi tệp cấu hình dùng cho kiểm thử; truyền {@code null} để quay lại {@code db.properties}.
   *
   * @param path tệp cấu hình cần dùng, hoặc {@code null}
   */
  public static synchronized void setPropertiesFileForTest(Path path) {
    propertiesPath = path == null ? Paths.get("db.properties") : path;
    fileProps = null;
  }
}
