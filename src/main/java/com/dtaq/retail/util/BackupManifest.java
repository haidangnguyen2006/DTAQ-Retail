package com.dtaq.retail.util;

import com.dtaq.retail.entity.BackupRecord;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ghi và đọc tệp metadata {@code *.meta.json} đi kèm mỗi bản sao lưu, đồng thời tính SHA-256.
 *
 * <p>Dự án không dùng thư viện JSON nên tệp metadata là một đối tượng JSON phẳng với đúng mười
 * khóa: {@code filePath} (chỉ tên tệp), {@code fileSize}, {@code sha256}, {@code loai},
 * {@code trangThai}, {@code thongBao}, {@code createdAt}, {@code createdBy}, {@code phienBanPg},
 * {@code tenCsdl}. Tệp được ghi UTF-8 qua tệp tạm cùng thư mục rồi thay thế nguyên tử để không bao
 * giờ để lại metadata dở dang.
 */
public final class BackupManifest {

  /** Hậu tố tệp dump do {@code pg_dump -Fc} tạo ra. */
  public static final String DUMP_SUFFIX = ".dump";

  /** Hậu tố tệp metadata đi kèm. */
  public static final String META_SUFFIX = ".meta.json";

  private static final String TEN_TEP_TAM_SUFFIX = ".tmp";

  private static final Pattern SO = Pattern.compile("-?\\d+");

  private BackupManifest() {
  }

  /**
   * Ghi metadata cho một tệp dump.
   *
   * <p>Tệp đích là {@link #metaPathFor(Path)} của {@code dumpFile}. Trường {@code filePath} trong
   * JSON chỉ chứa tên tệp, không chứa thư mục.
   *
   * @param dumpFile tệp dump tương ứng
   * @param meta bản ghi cần ghi
   * @throws IOException khi không tạo được thư mục, không ghi được tệp tạm hoặc không thay thế được
   *     tệp đích
   */
  public static void write(Path dumpFile, BackupRecord meta) throws IOException {
    Objects.requireNonNull(dumpFile, "dumpFile");
    Objects.requireNonNull(meta, "meta");

    Path dich = metaPathFor(dumpFile);
    Path thuMuc = dich.toAbsolutePath().getParent();
    if (thuMuc != null) {
      Files.createDirectories(thuMuc);
    }
    Path tam = dich.resolveSibling(dich.getFileName() + TEN_TEP_TAM_SUFFIX);
    try {
      Files.writeString(tam, toJson(dumpFile, meta), StandardCharsets.UTF_8);
      try {
        Files.move(tam, dich, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tam, dich, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tam);
    }
  }

  /**
   * Đọc metadata từ tệp {@code *.meta.json}.
   *
   * <p>Đường dẫn tệp dump trong kết quả được ghép với thư mục chứa tệp metadata, vì JSON chỉ lưu
   * tên tệp.
   *
   * @param metaFile tệp metadata cần đọc
   * @return bản ghi tương ứng, {@code id} luôn là {@code null}
   * @throws IOException khi tệp không đọc được, không phải đối tượng JSON, thiếu khóa bắt buộc hoặc
   *     giá trị không hợp lệ
   */
  public static BackupRecord read(Path metaFile) throws IOException {
    Objects.requireNonNull(metaFile, "metaFile");
    String noiDung = Files.readString(metaFile, StandardCharsets.UTF_8).trim();
    if (!noiDung.startsWith("{") || !noiDung.endsWith("}")) {
      throw loi("không phải đối tượng JSON");
    }

    String tenTep = docChuoi(noiDung, "filePath", true);
    if (tenTep == null || tenTep.isBlank()) {
      throw loi("filePath rỗng");
    }
    long fileSize = docSo(noiDung, "fileSize");
    if (fileSize < 0) {
      throw loi("fileSize âm");
    }
    String sha256 = docChuoi(noiDung, "sha256", false);
    String loai = docChuoi(noiDung, "loai", true);
    String trangThai = docChuoi(noiDung, "trangThai", true);
    String thongBao = docChuoi(noiDung, "thongBao", false);
    String createdAt = docChuoi(noiDung, "createdAt", true);
    String createdBy = docChuoi(noiDung, "createdBy", false);
    String phienBanPg = docChuoi(noiDung, "phienBanPg", false);
    String tenCsdl = docChuoi(noiDung, "tenCsdl", false);

    BackupRecord.LoaiSaoLuu loaiSaoLuu;
    try {
      loaiSaoLuu = BackupRecord.LoaiSaoLuu.valueOf(loai);
    } catch (IllegalArgumentException e) {
      throw loi("loai không hợp lệ: " + loai);
    }
    BackupRecord.TrangThaiSaoLuu trangThaiSaoLuu;
    try {
      trangThaiSaoLuu = BackupRecord.TrangThaiSaoLuu.valueOf(trangThai);
    } catch (IllegalArgumentException e) {
      throw loi("trangThai không hợp lệ: " + trangThai);
    }
    Instant thoiDiem;
    try {
      thoiDiem = Instant.parse(createdAt);
    } catch (DateTimeParseException e) {
      throw loi("createdAt không hợp lệ: " + createdAt);
    }

    Path thuMuc = metaFile.toAbsolutePath().getParent();
    Path filePath = thuMuc == null ? Path.of(tenTep) : thuMuc.resolve(tenTep);
    return new BackupRecord(null, filePath, fileSize, sha256, loaiSaoLuu, trangThaiSaoLuu,
        thongBao, thoiDiem, createdBy, phienBanPg, tenCsdl);
  }

  /**
   * Tính SHA-256 của một tệp, đọc theo luồng để không nạp cả tệp vào bộ nhớ.
   *
   * @param file tệp cần băm
   * @return chuỗi hex thường 64 ký tự
   * @throws IOException khi không đọc được tệp
   */
  public static String sha256(Path file) throws IOException {
    Objects.requireNonNull(file, "file");
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM không hỗ trợ SHA-256", e);
    }
    byte[] boDem = new byte[8192];
    try (InputStream in = Files.newInputStream(file);
        DigestInputStream digestIn = new DigestInputStream(in, digest)) {
      while (digestIn.read(boDem) >= 0) {
        // Đọc hết luồng để DigestInputStream cập nhật toàn bộ nội dung.
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  /**
   * Đường dẫn metadata tương ứng với một tệp dump.
   *
   * @param dumpFile tệp dump, ví dụ {@code dtaq_2026-11-05_000000.dump}
   * @return {@code dtaq_2026-11-05_000000.meta.json}; tên khác {@code .dump} thì thêm hậu tố
   */
  public static Path metaPathFor(Path dumpFile) {
    Objects.requireNonNull(dumpFile, "dumpFile");
    String ten = tenTep(dumpFile);
    String tenMeta = ten.endsWith(DUMP_SUFFIX)
        ? ten.substring(0, ten.length() - DUMP_SUFFIX.length()) + META_SUFFIX
        : ten + META_SUFFIX;
    return cungThuMuc(dumpFile, tenMeta);
  }

  /**
   * Đường dẫn tệp dump tương ứng với một tệp metadata.
   *
   * @param metaFile tệp metadata, ví dụ {@code dtaq_2026-11-05_000000.meta.json}
   * @return {@code dtaq_2026-11-05_000000.dump}; tên khác {@code .meta.json} thì thêm hậu tố
   */
  public static Path dumpPathFor(Path metaFile) {
    Objects.requireNonNull(metaFile, "metaFile");
    String ten = tenTep(metaFile);
    String tenDump = ten.endsWith(META_SUFFIX)
        ? ten.substring(0, ten.length() - META_SUFFIX.length()) + DUMP_SUFFIX
        : ten + DUMP_SUFFIX;
    return cungThuMuc(metaFile, tenDump);
  }

  private static String toJson(Path dumpFile, BackupRecord meta) {
    StringBuilder json = new StringBuilder(512);
    json.append("{\n");
    json.append("  \"filePath\": ").append(chuoiJson(tenTep(dumpFile))).append(",\n");
    json.append("  \"fileSize\": ").append(meta.fileSize()).append(",\n");
    json.append("  \"sha256\": ").append(chuoiJsonHoacNull(meta.sha256())).append(",\n");
    json.append("  \"loai\": ").append(chuoiJson(meta.loai().name())).append(",\n");
    json.append("  \"trangThai\": ").append(chuoiJson(meta.trangThai().name())).append(",\n");
    json.append("  \"thongBao\": ").append(chuoiJsonHoacNull(meta.thongBao())).append(",\n");
    json.append("  \"createdAt\": ").append(chuoiJson(meta.createdAt().toString())).append(",\n");
    json.append("  \"createdBy\": ").append(chuoiJsonHoacNull(meta.createdBy())).append(",\n");
    json.append("  \"phienBanPg\": ").append(chuoiJsonHoacNull(meta.phienBanPg())).append(",\n");
    json.append("  \"tenCsdl\": ").append(chuoiJsonHoacNull(meta.tenCsdl())).append("\n");
    json.append("}\n");
    return json.toString();
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

  private static String docChuoi(String json, String khoa, boolean batBuoc) throws IOException {
    Matcher matcher = patternChuoi(khoa).matcher(json);
    if (!matcher.find()) {
      if (batBuoc) {
        throw loi("thiếu khóa " + khoa);
      }
      return null;
    }
    return giaiEscape(matcher.group(1), khoa);
  }

  private static long docSo(String json, String khoa) throws IOException {
    Matcher matcher = Pattern.compile("\"" + Pattern.quote(khoa) + "\"\\s*:\\s*(-?\\d+)")
        .matcher(json);
    if (!matcher.find()) {
      throw loi("thiếu khóa " + khoa);
    }
    String value = matcher.group(1);
    if (!SO.matcher(value).matches()) {
      throw loi(khoa + " không phải số: " + value);
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw loi(khoa + " vượt quá giới hạn: " + value);
    }
  }

  private static Pattern patternChuoi(String khoa) {
    return Pattern.compile("\"" + Pattern.quote(khoa) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
        Pattern.DOTALL);
  }

  private static String giaiEscape(String value, String khoa) throws IOException {
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch != '\\') {
        sb.append(ch);
        continue;
      }
      i++;
      if (i >= value.length()) {
        throw loi("escape ở cuối giá trị " + khoa);
      }
      char escape = value.charAt(i);
      switch (escape) {
        case '"' -> sb.append('"');
        case '\\' -> sb.append('\\');
        case '/' -> sb.append('/');
        case 'n' -> sb.append('\n');
        case 'r' -> sb.append('\r');
        case 't' -> sb.append('\t');
        case 'b' -> sb.append('\b');
        case 'f' -> sb.append('\f');
        case 'u' -> {
          if (i + 5 > value.length()) {
            throw loi("escape \\u thiếu chữ số trong " + khoa);
          }
          String hex = value.substring(i + 1, i + 5);
          try {
            sb.append((char) Integer.parseInt(hex, 16));
          } catch (NumberFormatException e) {
            throw loi("escape \\u không hợp lệ trong " + khoa + ": " + hex);
          }
          i += 4;
        }
        default -> throw loi("escape không hợp lệ '\\" + escape + "' trong " + khoa);
      }
    }
    return sb.toString();
  }

  private static String tenTep(Path path) {
    Path ten = path.getFileName();
    return ten == null ? path.toString() : ten.toString();
  }

  private static Path cungThuMuc(Path goc, String tenMoi) {
    Path thuMuc = goc.getParent();
    return thuMuc == null ? Path.of(tenMoi) : thuMuc.resolve(tenMoi);
  }

  private static IOException loi(String chiTiet) {
    return new IOException("Metadata không hợp lệ: " + chiTiet);
  }
}
