package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Kiểm tĩnh (không cần CSDL) hợp đồng cấu trúc của migration {@code V4__seed_full_data.sql}.
 *
 * <p>Bài test chỉ đọc tệp SQL trên classpath và xác nhận: đúng 14 khối INSERT cho 14 bảng nghiệp
 * vụ, số dòng trong khoảng 10..100, định danh đúng format tài liệu, serial duy nhất, không có lệnh
 * phá dữ liệu/upsert, các cột enum chỉ dùng giá trị của V1, và các bất biến an toàn dữ liệu (SĐT,
 * tenDangNhap, không tham chiếu khuyến mãi).
 */
class SeedMigrationContractTest {

  private static final String SQL_RESOURCE = "/db/migration/V4__seed_full_data.sql";

  private static final List<String> EXPECTED_TABLES = List.of(
      "NhanVien",
      "KhachHang",
      "DanhMucSanPham",
      "NhaCungCap",
      "DongSanPham",
      "SanPham",
      "KhuyenMai",
      "HoaDon",
      "ChiTietHoaDon",
      "PhieuTra",
      "ChiTietPhieuTra",
      "PhieuDoi",
      "ChiTietPhieuDoi",
      "SanPhamLoi");

  /** Bảng không có định danh riêng ở cột đầu: chỉ cần giá trị không rỗng (và serial duy nhất). */
  private static final Set<String> TABLES_WITHOUT_OWN_ID = Set.of(
      "SanPham", "ChiTietHoaDon", "ChiTietPhieuTra", "ChiTietPhieuDoi");

  private static final Map<String, String> ID_PATTERN_BY_TABLE = new LinkedHashMap<>();

  static {
    ID_PATTERN_BY_TABLE.put("NhanVien", "^NV(QUANLY|BANHANG)-\\d{4}-\\d{3}$");
    ID_PATTERN_BY_TABLE.put("KhachHang", "^KH-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("DanhMucSanPham", "^DM-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("NhaCungCap", "^NCC-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("DongSanPham", "^DSP-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("KhuyenMai", "^KM-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("HoaDon", "^HD-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("PhieuTra", "^PT-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("PhieuDoi", "^PD-\\d{4}-\\d{6}$");
    ID_PATTERN_BY_TABLE.put("SanPhamLoi", "^SPL-\\d{4}-\\d{6}$");
  }

  private static final Set<String> ALLOWED_TRANG_THAI_SAN_PHAM = Set.of(
      "SAN_SANG_BAN", "DA_BAN", "HANG_LOI", "DANG_BAO_HANH", "DA_TRA_NHA_CUNG_CAP", "DA_THANH_LY");

  private static final Set<String> ALLOWED_LOAI_QUYEN = Set.of(
      "NHAN_VIEN_QUAN_LY", "NHAN_VIEN_BAN_HANG");

  private static final Set<String> ALLOWED_TRANG_THAI_DUYET = Set.of(
      "KHONG_CAN_DUYET", "CHO_DUYET", "DA_DUYET", "TU_CHOI");

  private static final Set<String> ALLOWED_TRANG_THAI_XU_LY_PHIEU = Set.of(
      "NHAP", "CHO_XU_LY", "HOAN_TAT", "HUY");

  private static final Set<String> ALLOWED_TINH_TRANG_HANG_TRA = Set.of(
      "NGUYEN_VEN", "HANG_LOI");

  private static final Set<String> ALLOWED_TINH_TRANG_XU_LY = Set.of(
      "CHO_XU_LY", "DA_GUI_BAO_HANH", "DA_TRA_NHA_CUNG_CAP", "THANH_LY");

  /** Hai tài khoản đã seed ở V2 — V4 không được dùng lại. */
  private static final Set<String> V2_USERNAMES = Set.of("NVQL2026001", "NVBH2026001");

  private static String rawSql;
  private static String strippedSql;
  private static List<InsertBlock> blocks;

  @BeforeAll
  static void loadSql() {
    rawSql = readSqlFromClasspath();
    strippedSql = stripFullLineComments(rawSql);
    blocks = parseInsertBlocks(rawSql);
  }

  @Test
  void hasExactlyTheFourteenDocumentedInsertBlocks() {
    List<String> tables = blocks.stream().map(InsertBlock::table).toList();

    assertEquals(14, tables.size(),
        () -> "V4 phải có đúng 14 khối INSERT INTO, nhưng thấy " + tables.size() + ": " + tables);
    assertEquals(tables.size(), new LinkedHashSet<>(tables).size(),
        () -> "Mỗi bảng chỉ được có một khối INSERT, nhưng có bảng lặp: " + tables);
    assertEquals(new LinkedHashSet<>(EXPECTED_TABLES), new LinkedHashSet<>(tables),
        () -> "Tập bảng seed phải đúng 14 bảng nghiệp vụ đã duyệt, nhưng thấy: " + tables);

    assertFalse(tables.contains("DatLaiMatKhau"),
        "V4 không được seed bảng OTP tạm DatLaiMatKhau");
    assertFalse(rawSql.contains("INSERT INTO \"DatLaiMatKhau\""),
        "V4 không được chứa câu lệnh INSERT cho bảng DatLaiMatKhau");
  }

  @Test
  void everyTableHasBetweenTenAndHundredRows() {
    for (InsertBlock block : blocks) {
      int count = block.rows().size();
      assertTrue(count >= 10 && count <= 100,
          () -> "Bảng " + block.table() + " có " + count + " dòng, phải nằm trong khoảng 10..100");
    }
  }

  @Test
  void rowCountsMatchDocumentedPlan() {
    assertEquals(100, block("SanPham").rows().size(), "SanPham phải có đúng 100 máy (serial)");
    assertEquals(10, block("NhanVien").rows().size(), "NhanVien phải có đúng 10 dòng bổ sung");
    assertEquals(60, block("ChiTietHoaDon").rows().size(), "ChiTietHoaDon phải có đúng 60 dòng");
  }

  @Test
  void firstColumnMatchesDocumentedIdPattern() {
    for (Map.Entry<String, String> entry : ID_PATTERN_BY_TABLE.entrySet()) {
      String table = entry.getKey();
      Pattern pattern = Pattern.compile(entry.getValue());
      List<String> ids = firstColumnValues(table);

      assertFalse(ids.isEmpty(), () -> "Bảng " + table + " không có dòng dữ liệu để kiểm định danh");
      for (int i = 0; i < ids.size(); i++) {
        String value = ids.get(i);
        int rowNumber = i + 1;
        assertTrue(pattern.matcher(value).matches(),
            () -> "Định danh cột đầu của " + table + " dòng " + rowNumber + " = '" + value
                + "' không khớp mẫu tài liệu " + entry.getValue());
      }
    }
  }

  @Test
  void sanPhamSerialsAreNonBlankAndUnique() {
    List<String> serials = firstColumnValues("SanPham");

    assertFalse(serials.isEmpty(), "Khối SanPham không có serial nào");
    for (String serial : serials) {
      assertFalse(serial.isBlank(), "Serial của SanPham không được để trống");
    }

    Set<String> duplicated = duplicates(serials);
    assertTrue(duplicated.isEmpty(),
        () -> "Serial SanPham phải duy nhất, nhưng bị trùng: " + duplicated);
  }

  @Test
  void firstColumnNonBlankForTablesWithoutOwnId() {
    for (String table : TABLES_WITHOUT_OWN_ID) {
      List<String> values = firstColumnValues(table);
      assertFalse(values.isEmpty(), () -> "Bảng " + table + " không có dòng dữ liệu");
      for (int i = 0; i < values.size(); i++) {
        String value = values.get(i);
        int rowNumber = i + 1;
        assertFalse(value.isBlank(),
            () -> "Cột đầu của " + table + " dòng " + rowNumber + " không được để trống");
      }
    }
  }

  @Test
  void noDestructiveOrUpsertKeywords() {
    List<String> forbiddenPatterns = List.of(
        "\\bDELETE\\b", "\\bTRUNCATE\\b", "\\bDROP\\b", "\\bUPDATE\\b", "\\bON\\s+CONFLICT\\b");

    for (String regex : forbiddenPatterns) {
      Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(strippedSql);
      if (matcher.find()) {
        fail("V4 không được chứa từ khóa phá dữ liệu/upsert " + regex
            + " nhưng tìm thấy: '" + matcher.group() + "'");
      }
    }
  }

  @Test
  void enumColumnsOnlyUseDocumentedValues() {
    assertSubsetOf("loaiQuyen", enumValuesOf("loaiQuyen"), ALLOWED_LOAI_QUYEN);
    assertSubsetOf("trangThai (SanPham)", enumValuesOf("trangThai"), ALLOWED_TRANG_THAI_SAN_PHAM);
    assertSubsetOf("trangThaiDuyet", enumValuesOf("trangThaiDuyet"), ALLOWED_TRANG_THAI_DUYET);
    assertSubsetOf("trangThaiXuLyPhieu", enumValuesOf("trangThaiXuLyPhieu"), ALLOWED_TRANG_THAI_XU_LY_PHIEU);
    assertSubsetOf("tinhTrangHangTra", enumValuesOf("tinhTrangHangTra"), ALLOWED_TINH_TRANG_HANG_TRA);
    assertSubsetOf("tinhTrangXuLy", enumValuesOf("tinhTrangXuLy"), ALLOWED_TINH_TRANG_XU_LY);
  }

  @Test
  void noEnumLikeTokenOutsideAllowedValues() {
    Matcher matcher = Pattern.compile("'([A-Z][A-Z_]+)'").matcher(strippedSql);
    Set<String> found = new TreeSet<>();
    while (matcher.find()) {
      found.add(matcher.group(1));
    }

    Set<String> allowed = new TreeSet<>();
    allowed.addAll(ALLOWED_TRANG_THAI_SAN_PHAM);
    allowed.addAll(ALLOWED_LOAI_QUYEN);
    allowed.addAll(ALLOWED_TRANG_THAI_DUYET);
    allowed.addAll(ALLOWED_TRANG_THAI_XU_LY_PHIEU);
    allowed.addAll(ALLOWED_TINH_TRANG_HANG_TRA);
    allowed.addAll(ALLOWED_TINH_TRANG_XU_LY);

    assertSubsetOf("token enum trong toàn tệp", found, allowed);
  }

  @Test
  void customerAndEmployeePhoneNumbersAreUnique() {
    Matcher matcher = Pattern.compile("'09\\d{8}'").matcher(strippedSql);
    List<String> phones = new ArrayList<>();
    while (matcher.find()) {
      phones.add(matcher.group());
    }

    assertFalse(phones.isEmpty(), "Không tìm thấy số điện thoại 09xxxxxxxx nào trong V4");
    Set<String> duplicated = duplicates(phones);
    assertTrue(duplicated.isEmpty(),
        () -> "SĐT khách hàng/nhân viên phải duy nhất, nhưng bị trùng: " + duplicated);
  }

  @Test
  void usernamesAreUniqueAndDoNotReuseV2Accounts() {
    List<String> usernames = columnValues("tenDangNhap");

    assertFalse(usernames.isEmpty(), "Không trích được tenDangNhap nào từ V4");
    Set<String> duplicated = duplicates(usernames);
    assertTrue(duplicated.isEmpty(),
        () -> "tenDangNhap phải duy nhất, nhưng bị trùng: " + duplicated);

    Set<String> reusedV2Accounts = new TreeSet<>(usernames);
    reusedV2Accounts.retainAll(V2_USERNAMES);
    assertTrue(reusedV2Accounts.isEmpty(),
        () -> "V4 không được dùng lại tài khoản V2: " + reusedV2Accounts);
  }

  @Test
  void chiTietHoaDonDoesNotReferenceAnyPromotion() {
    InsertBlock block = block("ChiTietHoaDon");

    assertFalse(block.rawText().contains("'KM-"),
        "ChiTietHoaDon không được tham chiếu khuyến mãi (maKM phải là NULL)");

    List<String> maKmValues = columnValues(block, "maKM");
    assertFalse(maKmValues.isEmpty(), "Không trích được giá trị cột maKM của ChiTietHoaDon");
    for (String value : maKmValues) {
      assertTrue("NULL".equalsIgnoreCase(value),
          () -> "maKM của ChiTietHoaDon phải là NULL nhưng thấy: " + value);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private static String readSqlFromClasspath() {
    try (InputStream in = SeedMigrationContractTest.class.getResourceAsStream(SQL_RESOURCE)) {
      assertNotNull(in, "Không tìm thấy tệp migration V4 trên classpath: " + SQL_RESOURCE);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Không đọc được " + SQL_RESOURCE, e);
    }
  }

  private static String stripFullLineComments(String sql) {
    return String.join("\n", sql.lines()
        .filter(line -> !line.strip().startsWith("--"))
        .toList());
  }

  private static List<InsertBlock> parseInsertBlocks(String sql) {
    String[] parts = sql.split(Pattern.quote("INSERT INTO \""));
    List<InsertBlock> parsed = new ArrayList<>();
    for (int i = 1; i < parts.length; i++) {
      String part = parts[i];
      String table = part.substring(0, part.indexOf('"'));
      List<String> columns = parseColumns(part);
      parsed.add(new InsertBlock(table, columns, parseRows(part), part));
    }
    return parsed;
  }

  private static List<String> parseColumns(String part) {
    int open = part.indexOf('(', part.indexOf('"'));
    int close = part.indexOf(')', open);
    return splitTopLevel(part.substring(open + 1, close)).stream()
        .map(token -> token.strip().replace("\"", ""))
        .toList();
  }

  private static List<List<String>> parseRows(String part) {
    List<List<String>> rows = new ArrayList<>();
    boolean afterValues = false;
    for (String line : part.lines().toList()) {
      String trimmed = line.strip();
      if (!afterValues) {
        afterValues = trimmed.startsWith("VALUES");
        continue;
      }
      if (trimmed.startsWith("(")) {
        rows.add(splitTopLevel(trimmed.substring(trimmed.indexOf('(') + 1, trimmed.lastIndexOf(')'))));
      }
      if (trimmed.endsWith(";")) {
        break;
      }
    }
    return rows;
  }

  /** Tách theo dấu phẩy ở mức cao nhất, bỏ qua dấu phẩy nằm trong chuỗi nháy đơn. */
  private static List<String> splitTopLevel(String body) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuote = false;
    for (int i = 0; i < body.length(); i++) {
      char c = body.charAt(i);
      if (c == '\'') {
        inQuote = !inQuote;
        current.append(c);
      } else if (c == ',' && !inQuote) {
        tokens.add(current.toString().strip());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    tokens.add(current.toString().strip());
    return tokens;
  }

  private static InsertBlock block(String table) {
    return blocks.stream()
        .filter(block -> block.table().equals(table))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Không tìm thấy khối INSERT cho bảng " + table));
  }

  private static List<String> firstColumnValues(String table) {
    List<String> values = new ArrayList<>();
    for (List<String> row : block(table).rows()) {
      assertFalse(row.isEmpty(), () -> "Bảng " + table + " có dòng không đọc được giá trị");
      values.add(unquote(row.get(0)));
    }
    return values;
  }

  /** Toàn bộ giá trị (kể cả trùng) của một cột trong mọi khối INSERT có cột đó. */
  private static List<String> columnValues(String column) {
    List<String> values = new ArrayList<>();
    for (InsertBlock block : blocks) {
      int index = block.columns().indexOf(column);
      if (index < 0) {
        continue;
      }
      for (List<String> row : block.rows()) {
        if (index < row.size()) {
          values.add(unquote(row.get(index)));
        }
      }
    }
    return values;
  }

  /** Giá trị của một cột trong phạm vi một khối INSERT duy nhất. */
  private static List<String> columnValues(InsertBlock block, String column) {
    int index = block.columns().indexOf(column);
    if (index < 0) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (List<String> row : block.rows()) {
      if (index < row.size()) {
        values.add(unquote(row.get(index)));
      }
    }
    return values;
  }

  private static Set<String> enumValuesOf(String column) {
    Set<String> values = new TreeSet<>();
    for (String value : columnValues(column)) {
      if (!"NULL".equalsIgnoreCase(value)) {
        values.add(value);
      }
    }
    assertFalse(values.isEmpty(),
        () -> "Không trích được giá trị enum nào cho cột " + column + " (kiểm lại tên cột)");
    return values;
  }

  private static void assertSubsetOf(String label, Set<String> actual, Set<String> allowed) {
    Set<String> invalid = new TreeSet<>(actual);
    invalid.removeAll(allowed);
    assertTrue(invalid.isEmpty(),
        () -> "Giá trị " + label + " không thuộc enum của V1: " + invalid
            + " (cho phép: " + new TreeSet<>(allowed) + ")");
  }

  private static Set<String> duplicates(List<String> values) {
    Set<String> seen = new LinkedHashSet<>();
    Set<String> duplicated = new TreeSet<>();
    for (String value : values) {
      if (!seen.add(value)) {
        duplicated.add(value);
      }
    }
    return duplicated;
  }

  private static String unquote(String token) {
    String value = token.strip();
    if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  private record InsertBlock(String table, List<String> columns, List<List<String>> rows, String rawText) {
  }
}
