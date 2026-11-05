package com.dtaq.retail.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đọc cấu hình thời hạn đổi/trả từ system property, biến môi trường, tệp {@code db.properties} rồi
 * mới dùng giá trị mặc định (RB34/D11).
 *
 * <p>Khóa cấu hình là {@code DTAQ_RETURN_WINDOW_DAYS}. Giá trị trống, không phải số hoặc nhỏ hơn
 * hoặc bằng {@code 0} đều được hiểu là <b>không chặn</b> thời hạn và trả về {@code -1}.
 */
public final class ReturnConfig {

  private static final Logger log = LoggerFactory.getLogger(ReturnConfig.class);

  /** Khóa cấu hình số ngày còn được phép đổi/trả. */
  public static final String KEY_SO_NGAY_DOI_TRA = "DTAQ_RETURN_WINDOW_DAYS";

  private static final int KHONG_CHAN = -1;

  private static Path propertiesPath = Paths.get("db.properties");
  private static volatile Properties fileProps;

  private ReturnConfig() {
  }

  /**
   * Số ngày tối đa kể từ ngày lập hóa đơn mà khách còn được đổi/trả.
   *
   * @return số ngày dương, hoặc {@code -1} khi cấu hình trống/không hợp lệ/không dương (không chặn)
   */
  public static int soNgayDoiTra() {
    String value = resolve(KEY_SO_NGAY_DOI_TRA);
    if (value == null || value.isBlank()) {
      return KHONG_CHAN;
    }
    try {
      int soNgay = Integer.parseInt(value.trim());
      if (soNgay <= 0) {
        log.warn("{} = {} không dương; hiểu là không chặn thời hạn đổi/trả", KEY_SO_NGAY_DOI_TRA, soNgay);
        return KHONG_CHAN;
      }
      return soNgay;
    } catch (NumberFormatException e) {
      log.warn("{} = '{}' không phải số nguyên; hiểu là không chặn thời hạn đổi/trả",
          KEY_SO_NGAY_DOI_TRA, value);
      return KHONG_CHAN;
    }
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
      synchronized (ReturnConfig.class) {
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
      log.info("Không tìm thấy tệp cấu hình đổi/trả tại {}", path.toAbsolutePath());
      return properties;
    }
    try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      properties.load(reader);
      log.info("Đã nạp cấu hình đổi/trả từ {}", path.toAbsolutePath());
    } catch (IOException e) {
      log.warn("Không nạp được cấu hình đổi/trả từ {}: {}",
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
