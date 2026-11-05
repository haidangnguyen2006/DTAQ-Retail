package com.dtaq.retail.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đọc và ghi cấu hình sao lưu do Quản lý chỉnh sửa trên giao diện vào tệp {@code backup.properties}.
 *
 * <p>Tệp này chỉ chứa cấu hình vận hành (giờ chạy, chu kỳ), không chứa mật khẩu cơ sở dữ liệu hay
 * SMTP. Ghi tệp theo cách an toàn: ghi ra {@code <tệp>.tmp} rồi thay thế nguyên tử, giữ lại các khóa
 * khác đang có trong tệp. Sau khi ghi thành công, {@link BackupConfig#reload()} được gọi để lần đọc
 * kế tiếp nhận giá trị mới.
 */
public final class BackupSettingsStore {

  private static final Logger log = LoggerFactory.getLogger(BackupSettingsStore.class);

  private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");
  private static final String GHI_CHU =
      "DTAQ - cau hinh sao luu do Quan ly chinh sua tren giao dien";
  private static final int CHU_KY_NHO_NHAT = 1;
  private static final int CHU_KY_LON_NHAT = 168;

  private static Path filePath = Paths.get("backup.properties");

  private BackupSettingsStore() {
  }

  /**
   * Tệp cấu hình sao lưu đang được dùng.
   *
   * @return đường dẫn tệp, mặc định {@code backup.properties}
   */
  public static Path file() {
    return filePath;
  }

  /**
   * Lưu giờ chạy và chu kỳ sao lưu do Quản lý chọn trên giao diện.
   *
   * @param gioChay giờ chạy trong ngày, không được {@code null}
   * @param chuKyGio chu kỳ tính bằng giờ, từ {@code 1} đến {@code 168}
   * @throws IllegalArgumentException khi giờ trống hoặc chu kỳ ngoài khoảng cho phép
   * @throws UncheckedIOException khi không ghi được tệp cấu hình
   */
  public static synchronized void luu(LocalTime gioChay, int chuKyGio) {
    if (gioChay == null) {
      throw new IllegalArgumentException("Giờ chạy sao lưu không được để trống");
    }
    if (chuKyGio < CHU_KY_NHO_NHAT || chuKyGio > CHU_KY_LON_NHAT) {
      throw new IllegalArgumentException(
          "Chu kỳ sao lưu phải nằm trong khoảng " + CHU_KY_NHO_NHAT + " đến "
              + CHU_KY_LON_NHAT + " giờ");
    }

    Path dich = filePath;
    Path thuMuc = dich.toAbsolutePath().getParent();
    Path tam = tepTam(dich);
    Properties properties = doc(dich);
    String gio = gioChay.format(DINH_DANG_GIO);
    properties.setProperty(BackupConfig.KEY_GIO_CHAY, gio);
    properties.setProperty(BackupConfig.KEY_CHU_KY_GIO, Integer.toString(chuKyGio));

    try {
      if (thuMuc != null) {
        Files.createDirectories(thuMuc);
      }
      try (Writer writer = Files.newBufferedWriter(tam, StandardCharsets.UTF_8)) {
        properties.store(writer, GHI_CHU);
      }
      diChuyen(tam, dich);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Không ghi được cấu hình sao lưu vào " + dich.toAbsolutePath(), e);
    } finally {
      try {
        Files.deleteIfExists(tam);
      } catch (IOException e) {
        log.warn("Không xóa được tệp tạm {}: {}", tam.toAbsolutePath(), e.getMessage());
      }
    }

    BackupConfig.reload();
    log.info("Đã lưu cấu hình sao lưu: giờ chạy {}, chu kỳ {} giờ", gio, chuKyGio);
  }

  /**
   * Giờ chạy sao lưu đã lưu trong tệp cấu hình.
   *
   * @return giờ đã lưu, hoặc rỗng khi tệp thiếu/không hợp lệ
   */
  public static Optional<LocalTime> gioDaLuu() {
    String value = doc(filePath).getProperty(BackupConfig.KEY_GIO_CHAY);
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalTime.parse(value.trim(), DINH_DANG_GIO));
    } catch (DateTimeParseException e) {
      log.warn("{} = '{}' trong {} không đúng định dạng HH:mm",
          BackupConfig.KEY_GIO_CHAY, value, filePath.toAbsolutePath());
      return Optional.empty();
    }
  }

  /**
   * Chu kỳ sao lưu đã lưu trong tệp cấu hình.
   *
   * @return số giờ trong khoảng {@code 1..168}, hoặc rỗng khi tệp thiếu/không hợp lệ
   */
  public static Optional<Integer> chuKyDaLuu() {
    String value = doc(filePath).getProperty(BackupConfig.KEY_CHU_KY_GIO);
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      int soGio = Integer.parseInt(value.trim());
      if (soGio < CHU_KY_NHO_NHAT || soGio > CHU_KY_LON_NHAT) {
        log.warn("{} = {} trong {} ngoài khoảng {}-{}",
            BackupConfig.KEY_CHU_KY_GIO, soGio, filePath.toAbsolutePath(),
            CHU_KY_NHO_NHAT, CHU_KY_LON_NHAT);
        return Optional.empty();
      }
      return Optional.of(soGio);
    } catch (NumberFormatException e) {
      log.warn("{} = '{}' trong {} không phải số nguyên",
          BackupConfig.KEY_CHU_KY_GIO, value, filePath.toAbsolutePath());
      return Optional.empty();
    }
  }

  private static Path tepTam(Path dich) {
    Path ten = dich.getFileName();
    return ten == null ? Paths.get(dich + ".tmp") : dich.resolveSibling(ten + ".tmp");
  }

  private static void diChuyen(Path tam, Path dich) throws IOException {
    try {
      Files.move(tam, dich, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      log.debug("Hệ thống tệp không hỗ trợ di chuyển nguyên tử; ghi đè trực tiếp {}", dich);
      Files.move(tam, dich, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static Properties doc(Path path) {
    Properties properties = new Properties();
    if (!Files.isReadable(path)) {
      return properties;
    }
    try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException | IllegalArgumentException e) {
      log.warn("Không đọc được cấu hình sao lưu từ {}: {}",
          path.toAbsolutePath(), e.getMessage());
    }
    return properties;
  }

  /**
   * Đổi tệp cấu hình dùng cho kiểm thử; truyền {@code null} để quay lại {@code backup.properties}.
   *
   * @param path tệp cấu hình cần dùng, hoặc {@code null}
   */
  static synchronized void setFileForTest(Path path) {
    filePath = path == null ? Paths.get("backup.properties") : path;
  }
}
