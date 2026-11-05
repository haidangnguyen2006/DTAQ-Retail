package com.dtaq.retail.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đọc cấu hình giám sát sức khỏe dữ liệu theo thứ tự ưu tiên: system property, biến môi trường,
 * tệp {@code db.properties}, rồi giá trị mặc định.
 *
 * <p>Các khóa {@code DTAQ_HEALTH_*} điều khiển chu kỳ kiểm tra, ngưỡng phát hiện suy giảm, danh
 * sách bảng cốt lõi và chính sách tự động phục hồi. Giá trị số không dương, ngưỡng ngoài khoảng
 * {@code (0, 1)} hoặc tên bảng không hợp lệ đều được ghi cảnh báo và thay bằng mặc định, không làm
 * ứng dụng dừng.
 */
public final class HealthConfig {

  private static final Logger log = LoggerFactory.getLogger(HealthConfig.class);

  /** Bật/tắt giám sát sức khỏe dữ liệu. */
  public static final String KEY_BAT_TAT = "DTAQ_HEALTH_ENABLED";

  /** Chu kỳ kiểm tra tính bằng phút. */
  public static final String KEY_CHU_KY_PHUT = "DTAQ_HEALTH_INTERVAL_MINUTES";

  /** Tệp JSON lưu trạng thái sức khỏe ngoài cơ sở dữ liệu. */
  public static final String KEY_TEP_TRANG_THAI = "DTAQ_HEALTH_STATE_FILE";

  /** Tỷ lệ suy giảm tối thiểu để coi là bất thường, trong khoảng {@code (0, 1)}. */
  public static final String KEY_NGUONG_GIAM = "DTAQ_HEALTH_DROP_THRESHOLD";

  /** Số dòng baseline tối thiểu để bắt đầu so sánh suy giảm. */
  public static final String KEY_BASELINE_TOI_THIEU = "DTAQ_HEALTH_DROP_MIN_BASELINE";

  /** Số chu kỳ bất thường liên tiếp cần có trước khi tạo incident. */
  public static final String KEY_SO_CHU_KY = "DTAQ_HEALTH_SUSPICIOUS_CYCLES";

  /** Danh sách bảng cốt lõi, phân tách bằng dấu phẩy. */
  public static final String KEY_BANG_COT_LOI = "DTAQ_HEALTH_CORE_TABLES";

  /** Bật/tắt tự động phục hồi khi dữ liệu ở trạng thái nghiêm trọng. */
  public static final String KEY_AUTO_RESTORE_BAT_TAT = "DTAQ_HEALTH_AUTO_RESTORE_ENABLED";

  /** Số lần tự động phục hồi tối đa trong một giờ. */
  public static final String KEY_AUTO_RESTORE_TOI_DA = "DTAQ_HEALTH_AUTO_RESTORE_MAX_PER_HOUR";

  /** Số bản sao lưu dự phòng tối đa được thử khi phục hồi. */
  public static final String KEY_SO_BAN_DU_PHONG = "DTAQ_HEALTH_FALLBACK_MAX_BACKUPS";

  /** Khoảng cách tối thiểu giữa hai lần nhắc lại cảnh báo, tính bằng phút. */
  public static final String KEY_NHAC_LAI_PHUT = "DTAQ_HEALTH_REMIND_MINUTES";

  /** Thời gian chờ tối đa cho một truy vấn kiểm tra, tính bằng giây. */
  public static final String KEY_TIMEOUT_GIAY = "DTAQ_HEALTH_TIMEOUT_SECONDS";

  private static final String MAC_DINH_TEP_TRANG_THAI = "./backups/health_state.json";
  private static final int MAC_DINH_CHU_KY_PHUT = 5;
  private static final double MAC_DINH_NGUONG_GIAM = 0.5;
  private static final int MAC_DINH_BASELINE_TOI_THIEU = 50;
  private static final int MAC_DINH_SO_CHU_KY = 2;
  private static final int MAC_DINH_AUTO_RESTORE_TOI_DA = 3;
  private static final int MAC_DINH_SO_BAN_DU_PHONG = 3;
  private static final int MAC_DINH_NHAC_LAI_PHUT = 30;
  private static final int MAC_DINH_TIMEOUT_GIAY = 10;
  private static final boolean MAC_DINH_BAT_TAT = true;

  private static final String MAC_DINH_BANG_COT_LOI = String.join(",", List.of(
      "NhanVien", "KhachHang", "DanhMucSanPham", "NhaCungCap", "DongSanPham", "SanPham",
      "KhuyenMai", "HoaDon", "ChiTietHoaDon", "PhieuTra", "ChiTietPhieuTra", "PhieuDoi",
      "ChiTietPhieuDoi", "SanPhamLoi"));

  private static final Pattern TEN_BANG = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

  private static Path propertiesPath = Paths.get("db.properties");
  private static volatile Properties fileProps;

  private HealthConfig() {
  }

  /**
   * Cho biết giám sát sức khỏe có được bật hay không.
   *
   * @return {@code true}/{@code false} theo cấu hình; giá trị lạ được hiểu là {@code true}
   */
  public static boolean batTat() {
    return docBoolean(KEY_BAT_TAT);
  }

  /**
   * Chu kỳ kiểm tra sức khỏe.
   *
   * @return số phút dương, mặc định {@code 5}
   */
  public static int chuKyPhut() {
    return docSoDuong(KEY_CHU_KY_PHUT, MAC_DINH_CHU_KY_PHUT);
  }

  /**
   * Tệp JSON lưu trạng thái sức khỏe ngoài cơ sở dữ liệu.
   *
   * @return đường dẫn cấu hình, mặc định {@code ./backups/health_state.json}
   */
  public static Path tepTrangThai() {
    String value = giaTriHoacMacDinh(KEY_TEP_TRANG_THAI, MAC_DINH_TEP_TRANG_THAI);
    try {
      return Path.of(value);
    } catch (InvalidPathException e) {
      log.warn("{} = '{}' không phải đường dẫn hợp lệ; dùng {}",
          KEY_TEP_TRANG_THAI, value, MAC_DINH_TEP_TRANG_THAI);
      return Path.of(MAC_DINH_TEP_TRANG_THAI);
    }
  }

  /**
   * Tỷ lệ suy giảm tối thiểu để coi là bất thường.
   *
   * @return số thực trong khoảng {@code (0, 1)}, mặc định {@code 0.5}
   */
  public static double nguongGiam() {
    String value = resolve(KEY_NGUONG_GIAM);
    if (value == null || value.isBlank()) {
      return MAC_DINH_NGUONG_GIAM;
    }
    try {
      double nguong = Double.parseDouble(value.trim());
      if (Double.isFinite(nguong) && nguong > 0 && nguong < 1) {
        return nguong;
      }
      log.warn("{} = {} nằm ngoài khoảng (0, 1); dùng {}",
          KEY_NGUONG_GIAM, value, MAC_DINH_NGUONG_GIAM);
      return MAC_DINH_NGUONG_GIAM;
    } catch (NumberFormatException e) {
      log.warn("{} = '{}' không phải số thực; dùng {}",
          KEY_NGUONG_GIAM, value, MAC_DINH_NGUONG_GIAM);
      return MAC_DINH_NGUONG_GIAM;
    }
  }

  /**
   * Số dòng baseline tối thiểu để bắt đầu so sánh suy giảm.
   *
   * @return số dòng dương, mặc định {@code 50}
   */
  public static int baselineToiThieu() {
    return docSoDuong(KEY_BASELINE_TOI_THIEU, MAC_DINH_BASELINE_TOI_THIEU);
  }

  /**
   * Số chu kỳ bất thường liên tiếp cần có trước khi tạo incident.
   *
   * @return số chu kỳ dương, mặc định {@code 2}
   */
  public static int soChuKyXacNhan() {
    return docSoDuong(KEY_SO_CHU_KY, MAC_DINH_SO_CHU_KY);
  }

  /**
   * Danh sách bảng cốt lõi cần kiểm tra.
   *
   * <p>Tên bảng không khớp {@code [A-Za-z][A-Za-z0-9]*} bị loại bỏ. Danh sách rỗng sau khi lọc
   * được thay bằng danh sách mặc định mười bốn bảng.
   *
   * @return danh sách tên bảng không trùng lặp, giữ nguyên thứ tự cấu hình
   */
  public static List<String> bangCotLoi() {
    String value = resolve(KEY_BANG_COT_LOI);
    if (value == null || value.isBlank()) {
      return macDinhBangCotLoi();
    }
    Set<String> hopLe = new LinkedHashSet<>();
    for (String phan : value.split(",")) {
      String ten = phan.trim();
      if (ten.isEmpty()) {
        continue;
      }
      if (TEN_BANG.matcher(ten).matches()) {
        hopLe.add(ten);
      } else {
        log.warn("Bỏ qua tên bảng không hợp lệ trong {}: '{}'", KEY_BANG_COT_LOI, ten);
      }
    }
    if (hopLe.isEmpty()) {
      log.warn("{} không còn tên bảng hợp lệ; dùng danh sách mặc định", KEY_BANG_COT_LOI);
      return macDinhBangCotLoi();
    }
    return List.copyOf(hopLe);
  }

  /**
   * Cho biết tự động phục hồi có được bật hay không.
   *
   * @return {@code true}/{@code false} theo cấu hình; giá trị lạ được hiểu là {@code true}
   */
  public static boolean autoRestoreBatTat() {
    return docBoolean(KEY_AUTO_RESTORE_BAT_TAT);
  }

  /**
   * Số lần tự động phục hồi tối đa trong một giờ.
   *
   * @return số lần dương, mặc định {@code 3}
   */
  public static int autoRestoreToiDaMoiGio() {
    return docSoDuong(KEY_AUTO_RESTORE_TOI_DA, MAC_DINH_AUTO_RESTORE_TOI_DA);
  }

  /**
   * Số bản sao lưu dự phòng tối đa được thử khi phục hồi.
   *
   * @return số bản dương, mặc định {@code 3}
   */
  public static int soBanDuPhongToiDa() {
    return docSoDuong(KEY_SO_BAN_DU_PHONG, MAC_DINH_SO_BAN_DU_PHONG);
  }

  /**
   * Khoảng cách tối thiểu giữa hai lần nhắc lại cảnh báo.
   *
   * @return số phút dương, mặc định {@code 30}
   */
  public static int nhacLaiPhut() {
    return docSoDuong(KEY_NHAC_LAI_PHUT, MAC_DINH_NHAC_LAI_PHUT);
  }

  /**
   * Thời gian chờ tối đa cho một truy vấn kiểm tra.
   *
   * @return số giây dương, mặc định {@code 10}
   */
  public static int timeoutGiay() {
    return docSoDuong(KEY_TIMEOUT_GIAY, MAC_DINH_TIMEOUT_GIAY);
  }

  private static List<String> macDinhBangCotLoi() {
    return List.of(MAC_DINH_BANG_COT_LOI.split(","));
  }

  private static boolean docBoolean(String key) {
    String value = resolve(key);
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
    log.warn("{} = '{}' không phải true/false; dùng {}", key, value, MAC_DINH_BAT_TAT);
    return MAC_DINH_BAT_TAT;
  }

  private static int docSoDuong(String key, int macDinh) {
    String value = resolve(key);
    if (value == null || value.isBlank()) {
      return macDinh;
    }
    try {
      int so = Integer.parseInt(value.trim());
      if (so <= 0) {
        log.warn("{} = {} không dương; dùng {}", key, so, macDinh);
        return macDinh;
      }
      return so;
    } catch (NumberFormatException e) {
      log.warn("{} = '{}' không phải số nguyên; dùng {}", key, value, macDinh);
      return macDinh;
    }
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
      synchronized (HealthConfig.class) {
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
      log.info("Không tìm thấy tệp cấu hình sức khỏe tại {}", path.toAbsolutePath());
      return properties;
    }
    try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      properties.load(reader);
      log.info("Đã nạp cấu hình sức khỏe từ {}", path.toAbsolutePath());
    } catch (IOException e) {
      log.warn("Không nạp được cấu hình sức khỏe từ {}: {}",
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
