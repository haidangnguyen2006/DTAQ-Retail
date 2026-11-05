package com.dtaq.retail.service;

import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.util.BackupManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý các tệp sao lưu trên đĩa: quét, ghi metadata, dọn bản cũ và sao chép dự phòng.
 *
 * <p>Mỗi bản sao lưu là một cặp tệp cùng thư mục: {@code dtaq_<timestamp>.dump} và
 * {@code dtaq_<timestamp>.meta.json}. Thư mục là nguồn sự thật; bảng lịch sử trong cơ sở dữ liệu
 * chỉ là cache hiển thị. Mọi lỗi đọc/ghi lẻ được ghi cảnh báo thay vì làm hỏng thao tác đang chạy,
 * trừ {@link #ghiMeta(BackupRecord)} và {@link #taoThuMuc()} vốn được bên gọi xử lý.
 */
public class BackupStore {

  private static final Logger log = LoggerFactory.getLogger(BackupStore.class);

  /** Tiền tố tên tệp sao lưu. */
  private static final String TIEN_TO = "dtaq_";

  /** Thông điệp của bản ghi không có metadata đi kèm. */
  static final String THONG_BAO_THIEU_META = "Thiếu metadata";

  private final Path thuMuc;
  private final Path thuMucPhu;

  /**
   * Khởi tạo kho sao lưu.
   *
   * @param thuMuc thư mục chính chứa bản sao lưu; không được {@code null}
   * @param thuMucPhu thư mục phụ để sao chép thêm, có thể {@code null}
   */
  public BackupStore(Path thuMuc, Path thuMucPhu) {
    this.thuMuc = Objects.requireNonNull(thuMuc, "thuMuc");
    this.thuMucPhu = thuMucPhu;
  }

  /** Thư mục chính chứa bản sao lưu. */
  public Path thuMuc() {
    return thuMuc;
  }

  /**
   * Tạo thư mục chính nếu chưa có.
   *
   * @throws IOException khi không tạo được thư mục
   */
  public void taoThuMuc() throws IOException {
    Files.createDirectories(thuMuc);
  }

  /**
   * Đường dẫn tuyệt đối của tệp dump chính thức.
   *
   * @param timestamp dấu thời gian đã định dạng, ví dụ {@code 2026-11-05_000000}
   * @return {@code <thư mục>/dtaq_<timestamp>.dump}
   */
  public Path duongDanChinhThuc(String timestamp) {
    Objects.requireNonNull(timestamp, "timestamp");
    return thuMuc.resolve(TIEN_TO + timestamp + BackupManifest.DUMP_SUFFIX).toAbsolutePath();
  }

  /**
   * Đường dẫn tuyệt đối của tệp dump tạm, dùng trong lúc {@code pg_dump} đang ghi.
   *
   * @param timestamp dấu thời gian đã định dạng, ví dụ {@code 2026-11-05_000000}
   * @return {@code <thư mục>/dtaq_<timestamp>.dump.tmp}
   */
  public Path duongDanTam(String timestamp) {
    Objects.requireNonNull(timestamp, "timestamp");
    return thuMuc.resolve(TIEN_TO + timestamp + BackupManifest.DUMP_SUFFIX + ".tmp")
        .toAbsolutePath();
  }

  /**
   * Ghi metadata {@code *.meta.json} cho bản ghi.
   *
   * <p>Tệp metadata nằm cạnh tệp dump và chỉ lưu tên tệp, không lưu thư mục.
   *
   * @param banGhi bản ghi cần ghi; {@code filePath} phải trỏ tới tệp dump
   * @throws IOException khi không ghi được metadata
   */
  public void ghiMeta(BackupRecord banGhi) throws IOException {
    Objects.requireNonNull(banGhi, "banGhi");
    BackupManifest.write(banGhi.filePath(), banGhi);
  }

  /**
   * Liệt kê toàn bộ bản sao lưu trong thư mục chính, mới nhất trước.
   *
   * <p>Thứ tự là {@code createdAt} giảm dần, sau đó tên tệp giảm dần. Tệp {@code .dump} không có
   * metadata hoặc metadata hỏng được trả về với trạng thái {@link TrangThaiSaoLuu#FAILED} thay vì
   * bị bỏ qua, để người dùng vẫn thấy tệp trên đĩa. Tệp {@code .tmp} đang ghi dở bị bỏ qua. Một
   * metadata hỏng không làm hỏng cả danh sách: lỗi được ghi cảnh báo và bản ghi tương ứng bị bỏ qua
   * khi không còn tệp dump.
   *
   * @return danh sách bản ghi, có thể rỗng
   */
  public List<BackupRecord> danhSach() {
    try {
      Files.createDirectories(thuMuc);
    } catch (IOException e) {
      log.warn("Không tạo được thư mục sao lưu {}: {}", thuMuc.toAbsolutePath(), e.getMessage());
      return List.of();
    }

    List<Path> tepMeta = new ArrayList<>();
    List<Path> tepDump = new ArrayList<>();
    try (Stream<Path> luong = Files.list(thuMuc)) {
      for (Path tep : luong.toList()) {
        String ten = tep.getFileName().toString();
        if (ten.endsWith(BackupManifest.META_SUFFIX)) {
          tepMeta.add(tep);
        } else if (ten.endsWith(BackupManifest.DUMP_SUFFIX)) {
          tepDump.add(tep);
        }
      }
    } catch (IOException e) {
      log.warn("Không đọc được thư mục sao lưu {}: {}", thuMuc.toAbsolutePath(), e.getMessage());
      return List.of();
    }

    List<BackupRecord> ketQua = new ArrayList<>(tepMeta.size() + tepDump.size());
    Set<Path> dumpDaCoMeta = new HashSet<>();
    for (Path meta : tepMeta) {
      try {
        BackupRecord banGhi = BackupManifest.read(meta);
        ketQua.add(banGhi);
        dumpDaCoMeta.add(banGhi.filePath().toAbsolutePath().normalize());
      } catch (IOException | RuntimeException e) {
        log.warn("Bỏ qua metadata không đọc được {}: {}", meta.getFileName(), e.getMessage());
        Path dump = BackupManifest.dumpPathFor(meta);
        if (Files.isRegularFile(dump)) {
          ketQua.add(banGhiThieuMeta(dump, "Metadata không hợp lệ: " + e.getMessage()));
          dumpDaCoMeta.add(dump.toAbsolutePath().normalize());
        }
      }
    }
    for (Path dump : tepDump) {
      if (!dumpDaCoMeta.contains(dump.toAbsolutePath().normalize())) {
        ketQua.add(banGhiThieuMeta(dump, THONG_BAO_THIEU_META));
      }
    }

    ketQua.sort(Comparator.comparing(BackupRecord::createdAt).reversed()
        .thenComparing(BackupRecord::fileName, Comparator.<String>reverseOrder()));
    return ketQua;
  }

  /**
   * Lấy bản sao lưu thành công gần nhất.
   *
   * @return bản ghi {@code SUCCESS} mới nhất, hoặc rỗng nếu chưa có
   */
  public Optional<BackupRecord> moiNhatThanhCong() {
    return moiNhatThanhCong(danhSach());
  }

  /**
   * Dọn các bản sao lưu cũ hơn số ngày giữ, luôn giữ bản thành công mới nhất.
   *
   * <p>Mỗi bản bị xóa gồm cả tệp {@code .dump} và {@code .meta.json}. Lỗi xóa được ghi cảnh báo;
   * phương thức không bao giờ ném ngoại lệ để không làm hỏng thao tác sao lưu vừa thành công.
   *
   * @param soNgayGiu số ngày giữ bản sao lưu
   * @param now mốc thời gian hiện tại dùng để tính ngưỡng
   */
  public void donDep(int soNgayGiu, Instant now) {
    Objects.requireNonNull(now, "now");
    Instant nguong = now.minus(soNgayGiu, ChronoUnit.DAYS);
    List<BackupRecord> tatCa = danhSach();
    Optional<BackupRecord> moiNhat = moiNhatThanhCong(tatCa);
    Set<Path> duocGiu = moiNhat
        .map(banGhi -> Set.of(banGhi.filePath().toAbsolutePath().normalize()))
        .orElseGet(Set::of);

    for (BackupRecord banGhi : tatCa) {
      if (!banGhi.createdAt().isBefore(nguong)) {
        continue;
      }
      if (duocGiu.contains(banGhi.filePath().toAbsolutePath().normalize())) {
        log.info("Giữ bản sao lưu thành công mới nhất {} dù đã quá hạn", banGhi.fileName());
        continue;
      }
      xoaBanGhi(banGhi);
    }
  }

  /**
   * Sao chép bản sao lưu sang thư mục phụ nếu có cấu hình.
   *
   * <p>Lỗi sao chép chỉ được ghi cảnh báo: bản chính đã hợp lệ nên không cần làm hỏng thao tác.
   *
   * @param banGhi bản ghi cần sao chép
   */
  public void saoChepPhu(BackupRecord banGhi) {
    Objects.requireNonNull(banGhi, "banGhi");
    if (thuMucPhu == null || thuMucPhu.toString().isBlank()) {
      return;
    }
    Path dump = banGhi.filePath();
    Path meta = BackupManifest.metaPathFor(dump);
    try {
      Files.createDirectories(thuMucPhu);
      Files.copy(dump, thuMucPhu.resolve(banGhi.fileName()),
          StandardCopyOption.REPLACE_EXISTING);
      Files.copy(meta, thuMucPhu.resolve(meta.getFileName()),
          StandardCopyOption.REPLACE_EXISTING);
      log.info("Đã sao chép bản sao lưu {} sang {}", banGhi.fileName(),
          thuMucPhu.toAbsolutePath());
    } catch (IOException e) {
      log.warn("Không sao chép được bản sao lưu {} sang {}: {}", banGhi.fileName(),
          thuMucPhu.toAbsolutePath(), e.getMessage());
    }
  }

  /**
   * Xóa cả tệp dump và metadata của một bản ghi; tệp không tồn tại được bỏ qua.
   *
   * @param banGhi bản ghi cần xóa
   */
  public void xoaBanGhi(BackupRecord banGhi) {
    Objects.requireNonNull(banGhi, "banGhi");
    xoaTep(banGhi.filePath());
    xoaTep(BackupManifest.metaPathFor(banGhi.filePath()));
  }

  private static Optional<BackupRecord> moiNhatThanhCong(List<BackupRecord> danhSach) {
    return danhSach.stream().filter(BackupRecord::thanhCong).findFirst();
  }

  private static BackupRecord banGhiThieuMeta(Path dump, String thongBao) {
    long kichThuoc = 0L;
    Instant thoiDiem = Instant.now();
    try {
      kichThuoc = Files.size(dump);
      thoiDiem = Files.getLastModifiedTime(dump).toInstant();
    } catch (IOException e) {
      log.warn("Không đọc được thông tin tệp {}: {}", dump.getFileName(), e.getMessage());
    }
    // Không có metadata nên không biết loại thao tác; dùng MANUAL làm giá trị trung tính.
    return new BackupRecord(null, dump.toAbsolutePath(), kichThuoc, null, LoaiSaoLuu.MANUAL,
        TrangThaiSaoLuu.FAILED, thongBao, thoiDiem, null, null, null);
  }

  private static void xoaTep(Path tep) {
    try {
      if (Files.deleteIfExists(tep)) {
        log.info("Đã xóa tệp sao lưu {}", tep.toAbsolutePath());
      }
    } catch (IOException e) {
      log.warn("Không xóa được tệp sao lưu {}: {}", tep.toAbsolutePath(), e.getMessage());
    }
  }
}
