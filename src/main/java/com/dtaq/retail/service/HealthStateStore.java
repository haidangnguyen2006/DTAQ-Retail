package com.dtaq.retail.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đọc và ghi trạng thái giám sát sức khỏe vào tệp JSON phẳng ngoài cơ sở dữ liệu.
 *
 * <p>Tệp mặc định là {@code ./backups/health_state.json}. Nội dung gồm đúng bảy khóa: {@code
 * baseline}, {@code suspiciousStreak}, {@code openIncidentId}, {@code lastNotifiedAt},
 * {@code autoRestoreAttempts}, {@code lastStatus} và {@code updatedAt}. Dự án không dùng thư viện
 * JSON nên tệp được đọc bằng bộ phân tích tối giản, chặt chẽ; tệp thiếu hoặc hỏng luôn trả về
 * {@link HealthState#rong()} kèm cảnh báo thay vì ném ngoại lệ.
 *
 * <p>Tệp được ghi UTF-8 qua tệp tạm cùng thư mục rồi thay thế nguyên tử để không bao giờ để lại
 * trạng thái dở dang. {@code updatedAt} do kho quản lý: mỗi lần ghi đều đóng dấu thời điểm hiện tại.
 */
public class HealthStateStore {

  private static final Logger log = LoggerFactory.getLogger(HealthStateStore.class);

  private static final String TEN_TEP_TAM_SUFFIX = ".tmp";

  private final Path file;

  /**
   * Khởi tạo kho trạng thái.
   *
   * @param file tệp JSON trạng thái; không được {@code null}
   */
  public HealthStateStore(Path file) {
    this.file = Objects.requireNonNull(file, "file");
  }

  /** Tệp JSON trạng thái đang dùng. */
  public Path file() {
    return file;
  }

  /**
   * Đọc trạng thái đã lưu.
   *
   * <p>Tệp không tồn tại, không đọc được hoặc không đúng cấu trúc đều trả về
   * {@link HealthState#rong()} và ghi cảnh báo; phương thức không bao giờ ném ngoại lệ.
   *
   * @return trạng thái đã lưu, hoặc trạng thái rỗng
   */
  public HealthState doc() {
    if (!Files.isRegularFile(file)) {
      log.warn("Chưa có tệp trạng thái sức khỏe {}; dùng trạng thái rỗng",
          file.toAbsolutePath());
      return HealthState.rong();
    }
    try {
      String noiDung = Files.readString(file, StandardCharsets.UTF_8);
      return parse(noiDung);
    } catch (IOException | RuntimeException e) {
      log.warn("Không đọc được trạng thái sức khỏe {}: {}; dùng trạng thái rỗng",
          file.toAbsolutePath(), e.getMessage());
      return HealthState.rong();
    }
  }

  /**
   * Ghi trạng thái xuống tệp.
   *
   * <p>Trường {@code updatedAt} luôn được đóng dấu {@link Instant#now()} tại thời điểm ghi. Lỗi
   * ghi chỉ được ghi cảnh báo để chu kỳ giám sát không bị dừng.
   *
   * @param state trạng thái cần ghi; không được {@code null}
   */
  public void ghi(HealthState state) {
    Objects.requireNonNull(state, "state");
    Path ten = file.getFileName();
    if (ten == null) {
      log.warn("Đường dẫn trạng thái sức khỏe không có tên tệp: {}", file);
      return;
    }
    HealthState ban = new HealthState(state.baseline(), state.suspiciousStreak(),
        state.openIncidentId(), state.lastNotifiedAt(), state.autoRestoreAttempts(),
        state.lastStatus(), Instant.now());
    Path tam = file.resolveSibling(ten + TEN_TEP_TAM_SUFFIX);
    try {
      Path thuMuc = file.toAbsolutePath().getParent();
      if (thuMuc != null) {
        Files.createDirectories(thuMuc);
      }
      Files.writeString(tam, toJson(ban), StandardCharsets.UTF_8);
      try {
        Files.move(tam, file, StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tam, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      log.warn("Không ghi được trạng thái sức khỏe {}: {}", file.toAbsolutePath(), e.getMessage());
    } finally {
      try {
        Files.deleteIfExists(tam);
      } catch (IOException e) {
        log.warn("Không xóa được tệp tạm trạng thái sức khỏe {}: {}", tam, e.getMessage());
      }
    }
  }

  /**
   * Trạng thái giám sát sức khỏe được lưu giữa các chu kỳ.
   *
   * @param baseline số dòng baseline theo từng bảng
   * @param suspiciousStreak số chu kỳ bất thường liên tiếp theo từng bảng
   * @param openIncidentId mã incident đang mở, có thể {@code null}
   * @param lastNotifiedAt thời điểm gửi cảnh báo gần nhất, có thể {@code null}
   * @param autoRestoreAttempts các lần tự động phục hồi đã thử
   * @param lastStatus trạng thái của chu kỳ gần nhất, có thể {@code null}
   * @param updatedAt thời điểm ghi gần nhất, có thể {@code null} khi chưa ghi
   */
  public record HealthState(Map<String, Long> baseline, Map<String, Integer> suspiciousStreak,
      Long openIncidentId, Instant lastNotifiedAt, List<Instant> autoRestoreAttempts,
      String lastStatus, Instant updatedAt) {

    /** Chuẩn hóa giá trị {@code null} thành tập rỗng và sao chép phòng thủ. */
    public HealthState {
      baseline = baseline == null
          ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(baseline));
      suspiciousStreak = suspiciousStreak == null
          ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(suspiciousStreak));
      autoRestoreAttempts = autoRestoreAttempts == null ? List.of() : List.copyOf(autoRestoreAttempts);
    }

    /** Trạng thái rỗng, dùng khi chưa có tệp hoặc tệp không đọc được. */
    public static HealthState rong() {
      return new HealthState(Map.of(), Map.of(), null, null, List.of(), null, null);
    }

    /** Trả về bản sao với baseline mới. */
    public HealthState withBaseline(Map<String, Long> value) {
      return new HealthState(value, suspiciousStreak, openIncidentId, lastNotifiedAt,
          autoRestoreAttempts, lastStatus, updatedAt);
    }

    /** Trả về bản sao với chuỗi chu kỳ bất thường mới. */
    public HealthState withStreak(Map<String, Integer> value) {
      return new HealthState(baseline, value, openIncidentId, lastNotifiedAt,
          autoRestoreAttempts, lastStatus, updatedAt);
    }

    /** Trả về bản sao với incident đang mở mới. */
    public HealthState withOpenIncident(Long value) {
      return new HealthState(baseline, suspiciousStreak, value, lastNotifiedAt,
          autoRestoreAttempts, lastStatus, updatedAt);
    }

    /** Trả về bản sao với thời điểm cảnh báo gần nhất mới. */
    public HealthState withLastNotifiedAt(Instant value) {
      return new HealthState(baseline, suspiciousStreak, openIncidentId, value,
          autoRestoreAttempts, lastStatus, updatedAt);
    }

    /** Trả về bản sao với danh sách lần tự động phục hồi mới. */
    public HealthState withAutoRestoreAttempts(List<Instant> value) {
      return new HealthState(baseline, suspiciousStreak, openIncidentId, lastNotifiedAt,
          value, lastStatus, updatedAt);
    }

    /** Trả về bản sao với trạng thái chu kỳ gần nhất mới. */
    public HealthState withLastStatus(String value) {
      return new HealthState(baseline, suspiciousStreak, openIncidentId, lastNotifiedAt,
          autoRestoreAttempts, value, updatedAt);
    }
  }

  private static String toJson(HealthState state) {
    StringBuilder json = new StringBuilder(256);
    json.append("{\"baseline\":{");
    boolean dau = true;
    for (Map.Entry<String, Long> entry : state.baseline().entrySet()) {
      if (!dau) {
        json.append(',');
      }
      json.append(chuoiJson(entry.getKey())).append(':').append(entry.getValue());
      dau = false;
    }
    json.append("},\"suspiciousStreak\":{");
    dau = true;
    for (Map.Entry<String, Integer> entry : state.suspiciousStreak().entrySet()) {
      if (!dau) {
        json.append(',');
      }
      json.append(chuoiJson(entry.getKey())).append(':').append(entry.getValue());
      dau = false;
    }
    json.append("},\"openIncidentId\":")
        .append(state.openIncidentId() == null ? "null" : state.openIncidentId());
    json.append(",\"lastNotifiedAt\":").append(chuoiJsonHoacNull(state.lastNotifiedAt()));
    json.append(",\"autoRestoreAttempts\":[");
    dau = true;
    for (Instant thoiDiem : state.autoRestoreAttempts()) {
      if (!dau) {
        json.append(',');
      }
      json.append(chuoiJson(thoiDiem.toString()));
      dau = false;
    }
    json.append("],\"lastStatus\":").append(chuoiJsonHoacNull(state.lastStatus()));
    json.append(",\"updatedAt\":").append(chuoiJsonHoacNull(state.updatedAt()));
    json.append('}');
    return json.toString();
  }

  private static String chuoiJsonHoacNull(Instant thoiDiem) {
    return thoiDiem == null ? "null" : chuoiJson(thoiDiem.toString());
  }

  private static String chuoiJsonHoacNull(String value) {
    return value == null ? "null" : chuoiJson(value);
  }

  private static String chuoiJson(String value) {
    StringBuilder sb = new StringBuilder(value.length() + 16);
    sb.append('"');
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        default -> {
          if (ch < 0x20) {
            sb.append(String.format("\\u%04x", (int) ch));
          } else {
            sb.append(ch);
          }
        }
      }
    }
    sb.append('"');
    return sb.toString();
  }

  private static HealthState parse(String noiDung) {
    BoDocJson doc = new BoDocJson(noiDung);
    doc.boQuaKhoangTrang();
    doc.mongDoi('{');

    Map<String, Long> baseline = Map.of();
    Map<String, Integer> streak = Map.of();
    Long openIncidentId = null;
    Instant lastNotifiedAt = null;
    List<Instant> attempts = List.of();
    String lastStatus = null;
    Instant updatedAt = null;

    Set<String> daGap = new HashSet<>();
    doc.boQuaKhoangTrang();
    if (!doc.thu('}')) {
      while (true) {
        doc.boQuaKhoangTrang();
        String khoa = doc.docChuoi();
        if (!daGap.add(khoa)) {
          throw new JsonKhongHopLe("khóa trùng: " + khoa);
        }
        doc.boQuaKhoangTrang();
        doc.mongDoi(':');
        doc.boQuaKhoangTrang();
        switch (khoa) {
          case "baseline" -> baseline = doc.docMapLong();
          case "suspiciousStreak" -> streak = doc.docMapInt();
          case "openIncidentId" -> openIncidentId = doc.docLongHoacNull();
          case "lastNotifiedAt" -> lastNotifiedAt = doc.docInstantHoacNull();
          case "autoRestoreAttempts" -> attempts = doc.docMangInstant();
          case "lastStatus" -> lastStatus = doc.docChuoiHoacNull();
          case "updatedAt" -> updatedAt = doc.docInstantHoacNull();
          default -> throw new JsonKhongHopLe("khóa lạ: " + khoa);
        }
        doc.boQuaKhoangTrang();
        char ketThuc = doc.lay();
        if (ketThuc == '}') {
          break;
        }
        if (ketThuc != ',') {
          throw new JsonKhongHopLe("thiếu dấu phẩy hoặc đóng ngoặc");
        }
      }
    }
    doc.boQuaKhoangTrang();
    if (!doc.het()) {
      throw new JsonKhongHopLe("thừa dữ liệu sau đối tượng");
    }
    return new HealthState(baseline, streak, openIncidentId, lastNotifiedAt, attempts, lastStatus,
        updatedAt);
  }

  /** Bộ phân tích JSON tối giản, chỉ phục vụ đúng cấu trúc trạng thái sức khỏe. */
  private static final class BoDocJson {

    private final String json;
    private int viTri;

    BoDocJson(String json) {
      this.json = json;
    }

    void boQuaKhoangTrang() {
      while (viTri < json.length() && Character.isWhitespace(json.charAt(viTri))) {
        viTri++;
      }
    }

    boolean het() {
      return viTri >= json.length();
    }

    char xem() {
      if (het()) {
        throw new JsonKhongHopLe("hết dữ liệu");
      }
      return json.charAt(viTri);
    }

    char lay() {
      char ch = xem();
      viTri++;
      return ch;
    }

    void mongDoi(char kyTu) {
      char thucTe = lay();
      if (thucTe != kyTu) {
        throw new JsonKhongHopLe("mong đợi '" + kyTu + "' nhưng gặp '" + thucTe + "'");
      }
    }

    boolean thu(char kyTu) {
      if (!het() && json.charAt(viTri) == kyTu) {
        viTri++;
        return true;
      }
      return false;
    }

    String docChuoi() {
      boQuaKhoangTrang();
      mongDoi('"');
      StringBuilder sb = new StringBuilder();
      while (true) {
        char ch = lay();
        if (ch == '"') {
          return sb.toString();
        }
        if (ch == '\\') {
          sb.append(docEscape());
        } else {
          if (ch < 0x20) {
            throw new JsonKhongHopLe("ký tự điều khiển trong chuỗi");
          }
          sb.append(ch);
        }
      }
    }

    private char docEscape() {
      char escape = lay();
      return switch (escape) {
        case '"' -> '"';
        case '\\' -> '\\';
        case '/' -> '/';
        case 'n' -> '\n';
        case 'r' -> '\r';
        case 't' -> '\t';
        case 'b' -> '\b';
        case 'f' -> '\f';
        case 'u' -> docUnicode();
        default -> throw new JsonKhongHopLe("escape không hợp lệ '\\" + escape + "'");
      };
    }

    private char docUnicode() {
      if (viTri + 4 > json.length()) {
        throw new JsonKhongHopLe("escape \\u thiếu chữ số");
      }
      String hex = json.substring(viTri, viTri + 4);
      try {
        char ch = (char) Integer.parseInt(hex, 16);
        viTri += 4;
        return ch;
      } catch (NumberFormatException e) {
        throw new JsonKhongHopLe("escape \\u không hợp lệ: " + hex);
      }
    }

    long docLong() {
      int batDau = viTri;
      if (!het() && json.charAt(viTri) == '-') {
        viTri++;
      }
      int batDauChuSo = viTri;
      while (!het() && json.charAt(viTri) >= '0' && json.charAt(viTri) <= '9') {
        viTri++;
      }
      if (viTri == batDauChuSo) {
        throw new JsonKhongHopLe("mong đợi số nguyên");
      }
      String so = json.substring(batDau, viTri);
      if (so.length() > 1 && (so.charAt(0) == '0'
          || (so.charAt(0) == '-' && so.charAt(1) == '0' && so.length() > 2))) {
        throw new JsonKhongHopLe("số nguyên có số 0 ở đầu: " + so);
      }
      try {
        return Long.parseLong(so);
      } catch (NumberFormatException e) {
        throw new JsonKhongHopLe("số nguyên vượt quá giới hạn: " + so);
      }
    }

    Long docLongHoacNull() {
      if (thuNull()) {
        return null;
      }
      return docLong();
    }

    String docChuoiHoacNull() {
      if (thuNull()) {
        return null;
      }
      return docChuoi();
    }

    Instant docInstantHoacNull() {
      if (thuNull()) {
        return null;
      }
      return docInstant(docChuoi());
    }

    private boolean thuNull() {
      boQuaKhoangTrang();
      if (thu('n')) {
        mongDoiChuoi("ull");
        return true;
      }
      return false;
    }

    private void mongDoiChuoi(String conLai) {
      if (!json.startsWith(conLai, viTri)) {
        throw new JsonKhongHopLe("mong đợi null");
      }
      viTri += conLai.length();
    }

    Map<String, Long> docMapLong() {
      if (thuNull()) {
        return Map.of();
      }
      Map<String, Long> ketQua = new LinkedHashMap<>();
      docThanDoiTuong((khoa, doc) -> {
        if (ketQua.containsKey(khoa)) {
          throw new JsonKhongHopLe("khóa trùng: " + khoa);
        }
        ketQua.put(khoa, doc.docLong());
      });
      return ketQua;
    }

    Map<String, Integer> docMapInt() {
      if (thuNull()) {
        return Map.of();
      }
      Map<String, Integer> ketQua = new LinkedHashMap<>();
      docThanDoiTuong((khoa, doc) -> {
        if (ketQua.containsKey(khoa)) {
          throw new JsonKhongHopLe("khóa trùng: " + khoa);
        }
        long so = doc.docLong();
        if (so < Integer.MIN_VALUE || so > Integer.MAX_VALUE) {
          throw new JsonKhongHopLe("số nguyên vượt quá giới hạn: " + so);
        }
        ketQua.put(khoa, (int) so);
      });
      return ketQua;
    }

    private void docThanDoiTuong(XuLyKhoa xuLy) {
      boQuaKhoangTrang();
      mongDoi('{');
      boQuaKhoangTrang();
      if (thu('}')) {
        return;
      }
      while (true) {
        boQuaKhoangTrang();
        String khoa = docChuoi();
        boQuaKhoangTrang();
        mongDoi(':');
        boQuaKhoangTrang();
        xuLy.xuLy(khoa, this);
        boQuaKhoangTrang();
        char ketThuc = lay();
        if (ketThuc == '}') {
          return;
        }
        if (ketThuc != ',') {
          throw new JsonKhongHopLe("thiếu dấu phẩy hoặc đóng ngoặc");
        }
      }
    }

    List<Instant> docMangInstant() {
      if (thuNull()) {
        return List.of();
      }
      List<Instant> ketQua = new ArrayList<>();
      boQuaKhoangTrang();
      mongDoi('[');
      boQuaKhoangTrang();
      if (thu(']')) {
        return ketQua;
      }
      while (true) {
        boQuaKhoangTrang();
        ketQua.add(docInstant(docChuoi()));
        boQuaKhoangTrang();
        char ketThuc = lay();
        if (ketThuc == ']') {
          return ketQua;
        }
        if (ketThuc != ',') {
          throw new JsonKhongHopLe("thiếu dấu phẩy hoặc đóng ngoặc");
        }
      }
    }

    private static Instant docInstant(String value) {
      try {
        return Instant.parse(value);
      } catch (DateTimeParseException e) {
        throw new JsonKhongHopLe("thời điểm không hợp lệ: " + value);
      }
    }
  }

  @FunctionalInterface
  private interface XuLyKhoa {
    void xuLy(String khoa, BoDocJson doc);
  }

  /** Ngoại lệ nội bộ khi nội dung JSON không đúng cấu trúc mong đợi. */
  private static final class JsonKhongHopLe extends RuntimeException {

    JsonKhongHopLe(String thongDiep) {
      super(thongDiep);
    }
  }
}
