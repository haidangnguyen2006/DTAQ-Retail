package com.dtaq.retail.service;

import com.dtaq.retail.config.BackupConfig;
import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.LichSuSaoLuuDao;
import com.dtaq.retail.dao.impl.LichSuSaoLuuDaoImpl;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.BackupRecord.LoaiSaoLuu;
import com.dtaq.retail.entity.BackupRecord.TrangThaiSaoLuu;
import com.dtaq.retail.exception.BackupException;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.BackupManifest;
import com.dtaq.retail.util.PgCommandExecutor;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điều phối một lần sao lưu cơ sở dữ liệu: giữ khóa, gọi {@code pg_dump}, kiểm tra tệp, ghi metadata,
 * ghi lịch sử và dọn bản cũ.
 *
 * <p>Lịch sử trong cơ sở dữ liệu chỉ là cache: lỗi ghi lịch sử không được làm hỏng một bản sao lưu đã
 * thành công. Mọi thao tác nặng chạy ngoài luồng giao diện; lớp này không phụ thuộc Swing.
 */
public class BackupService {

  private static final Logger log = LoggerFactory.getLogger(BackupService.class);

  /** Thời gian chờ tối đa để giành khóa bảo trì. */
  private static final Duration CHO_KHOA = Duration.ofSeconds(5);

  /** Độ dài tối đa của thông điệp lỗi lấy từ đầu ra tiến trình. */
  private static final int GIOI_HAN_THONG_BAO = 500;

  private static final DateTimeFormatter DINH_DANG_THOI_GIAN =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  private final DataSource dataSource;
  private final BackupStore store;
  private final PgCommandExecutor pg;
  private final LichSuSaoLuuDao dao;

  /**
   * Khởi tạo dịch vụ sao lưu.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   * @param store kho tệp sao lưu; không được {@code null}
   * @param pg bộ gọi công cụ PostgreSQL; không được {@code null}
   * @param dao truy cập lịch sử sao lưu; không được {@code null}
   */
  public BackupService(DataSource dataSource, BackupStore store, PgCommandExecutor pg,
      LichSuSaoLuuDao dao) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    this.store = Objects.requireNonNull(store, "store");
    this.pg = Objects.requireNonNull(pg, "pg");
    this.dao = Objects.requireNonNull(dao, "dao");
  }

  /**
   * Dựng dịch vụ theo cấu hình ứng dụng.
   *
   * <p>DAO lịch sử lấy nguồn dữ liệu qua {@link ConnectDB} ở mỗi thao tác thay vì giữ nguồn được
   * truyền vào, để lịch sử sao lưu vẫn ghi được sau khi dịch vụ phục hồi gọi
   * {@link ConnectDB#reset()} và dựng bể kết nối mới.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   * @return dịch vụ dùng thư mục, công cụ PostgreSQL và DAO lịch sử theo cấu hình
   */
  public static BackupService macDinh(DataSource dataSource) {
    Objects.requireNonNull(dataSource, "dataSource");
    BackupStore store = new BackupStore(
        BackupConfig.thuMucBackup(),
        BackupConfig.thuMucPhu().orElse(null));
    return new BackupService(dataSource, store, PgCommandExecutor.fromConfig(),
        new LichSuSaoLuuDaoImpl(ConnectDB::getDataSource));
  }

  /** Kho tệp sao lưu dùng bởi dịch vụ. */
  public BackupStore store() {
    return store;
  }

  /** Bộ gọi công cụ PostgreSQL dùng bởi dịch vụ. */
  public PgCommandExecutor pg() {
    return pg;
  }

  /** Truy cập lịch sử sao lưu dùng bởi dịch vụ. */
  public LichSuSaoLuuDao dao() {
    return dao;
  }

  /**
   * Sao lưu toàn bộ cơ sở dữ liệu ra một cặp tệp {@code .dump} + {@code .meta.json}.
   *
   * <p>Khi {@code pg_dump} thất bại hoặc quá thời gian chờ, một bản ghi {@code FAILED} vẫn được ghi
   * vào metadata và lịch sử để người dùng thấy được lần thử không thành công.
   *
   * @param loai loại thao tác; không được {@code null}
   * @param nguoiThucHien người hoặc tiến trình thực hiện
   * @return bản ghi thành công vừa tạo
   * @throws BackupException khi hệ thống đang bận hoặc sao lưu thất bại
   */
  public BackupRecord saoLuu(LoaiSaoLuu loai, String nguoiThucHien) {
    Objects.requireNonNull(loai, "loai");
    if (!giuKhoa()) {
      throw new BackupException("Hệ thống đang bận, vui lòng thử lại sau");
    }
    try {
      return thucHienSaoLuu(loai, nguoiThucHien);
    } finally {
      MaintenanceLock.release();
    }
  }

  /**
   * Liệt kê các bản sao lưu trên đĩa, mới nhất trước.
   *
   * @return danh sách bản ghi, có thể rỗng
   */
  public List<BackupRecord> danhSach() {
    return store.danhSach();
  }

  /**
   * Kiểm tra một bản sao lưu còn nguyên vẹn và đọc được hay không.
   *
   * <p>Kiểm tra lần lượt: tệp tồn tại, kích thước khớp metadata, SHA-256 khớp metadata và
   * {@code pg_restore --list} đọc được nội dung.
   *
   * @param banGhi bản ghi cần kiểm tra; không được {@code null}
   * @throws BackupException nêu rõ lý do khi tệp thiếu, sai kích thước, sai mã băm hoặc không hợp lệ
   */
  public void kiemTra(BackupRecord banGhi) {
    Objects.requireNonNull(banGhi, "banGhi");
    Path tep = banGhi.filePath();
    if (!Files.isRegularFile(tep)) {
      throw new BackupException("Không tìm thấy tệp sao lưu: " + tep.toAbsolutePath());
    }
    try {
      long kichThuoc = Files.size(tep);
      if (kichThuoc != banGhi.fileSize()) {
        throw new BackupException("Kích thước tệp sao lưu không khớp: mong đợi "
            + banGhi.fileSize() + " byte, thực tế " + kichThuoc + " byte");
      }
      if (banGhi.sha256() == null || banGhi.sha256().isBlank()) {
        throw new BackupException("Bản ghi không có mã SHA-256 để kiểm tra");
      }
      String shaThucTe = BackupManifest.sha256(tep);
      if (!shaThucTe.equalsIgnoreCase(banGhi.sha256())) {
        throw new BackupException("Mã SHA-256 không khớp: tệp sao lưu đã bị thay đổi");
      }
      PgCommandExecutor.KetQua kiemTraNoiDung = pg.listContents(tep);
      if (!kiemTraNoiDung.thanhCong()) {
        throw new BackupException("Tệp sao lưu không hợp lệ: "
            + rutGon(kiemTraNoiDung.output()));
      }
    } catch (IOException e) {
      throw new BackupException("Không thể kiểm tra tệp sao lưu " + banGhi.fileName(), e);
    }
  }

  /**
   * Đồng bộ lịch sử trong cơ sở dữ liệu với các tệp đang có trên đĩa.
   *
   * <p>Bản ghi nào chưa có trong bảng {@code "LichSuSaoLuu"} thì được thêm. Lỗi cơ sở dữ liệu của
   * từng dòng chỉ được ghi cảnh báo để không chặn các dòng còn lại.
   */
  public void dongBoLichSu() {
    for (BackupRecord banGhi : store.danhSach()) {
      try {
        if (!dao.existsByFilePath(banGhi.filePath().toString())) {
          dao.insert(banGhi);
        }
      } catch (DataAccessException e) {
        log.warn("Không đồng bộ được lịch sử cho {}: {}", banGhi.fileName(), e.getMessage());
      }
    }
  }

  private boolean giuKhoa() {
    try {
      return MaintenanceLock.tryAcquire(CHO_KHOA);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BackupException("Đã bị ngắt khi chờ khóa sao lưu", e);
    }
  }

  private BackupRecord thucHienSaoLuu(LoaiSaoLuu loai, String nguoiThucHien) {
    String thoiDiem = DINH_DANG_THOI_GIAN.format(Instant.now());
    Path tam = store.duongDanTam(thoiDiem);
    Path chinhThuc = store.duongDanChinhThuc(thoiDiem);
    try {
      store.taoThuMuc();
    } catch (IOException e) {
      throw new BackupException(
          "Không tạo được thư mục sao lưu " + store.thuMuc().toAbsolutePath(), e);
    }

    PgCommandExecutor.KetQua ketQuaDump = pg.dump(tam);
    if (!ketQuaDump.thanhCong()) {
      xoaTam(tam);
      throw ghiThatBai(loai, nguoiThucHien, chinhThuc, ketQuaDump);
    }

    try {
      long kichThuoc = Files.size(tam);
      if (kichThuoc <= 0) {
        xoaTam(tam);
        throw new BackupException("Sao lưu thất bại: tệp dump rỗng");
      }
      PgCommandExecutor.KetQua kiemTraNoiDung = pg.listContents(tam);
      if (!kiemTraNoiDung.thanhCong()) {
        xoaTam(tam);
        throw new BackupException("Sao lưu thất bại: tệp dump không hợp lệ: "
            + rutGon(kiemTraNoiDung.output()));
      }
      String sha256 = BackupManifest.sha256(tam);
      chuyenThanhChinhThuc(tam, chinhThuc);

      BackupRecord banGhi = new BackupRecord(null, chinhThuc, kichThuoc, sha256, loai,
          TrangThaiSaoLuu.SUCCESS, "Sao lưu thành công", Instant.now(), nguoiThucHien,
          pg.phienBan().output().lines().findFirst().orElse(""), pg.database());
      try {
        store.ghiMeta(banGhi);
      } catch (IOException e) {
        throw new BackupException(
            "Không ghi được metadata cho " + chinhThuc.getFileName(), e);
      }

      ghiLichSu(banGhi);
      store.saoChepPhu(banGhi);
      store.donDep(BackupConfig.soNgayGiu(), Instant.now());
      log.info("Đã sao lưu cơ sở dữ liệu vào {}", chinhThuc.toAbsolutePath());
      return banGhi;
    } catch (IOException e) {
      xoaTam(tam);
      throw new BackupException("Sao lưu thất bại: " + e.getMessage(), e);
    }
  }

  private BackupException ghiThatBai(LoaiSaoLuu loai, String nguoiThucHien, Path chinhThuc,
      PgCommandExecutor.KetQua ketQua) {
    String thongBao = rutGon(ketQua.output());
    if (thongBao.isBlank()) {
      thongBao = ketQua.timeout()
          ? "Quá thời gian chờ pg_dump"
          : "pg_dump kết thúc với mã thoát " + ketQua.exitCode();
    }
    BackupRecord thatBai = new BackupRecord(null, chinhThuc, 0L, null, loai,
        TrangThaiSaoLuu.FAILED, thongBao, Instant.now(), nguoiThucHien,
        pg.phienBan().output(), pg.database());
    try {
      store.ghiMeta(thatBai);
    } catch (IOException e) {
      log.warn("Không ghi được metadata thất bại cho {}: {}", chinhThuc.getFileName(),
          e.getMessage());
    }
    ghiLichSu(thatBai);
    log.warn("Sao lưu thất bại: {}", thongBao);
    return new BackupException("Sao lưu thất bại: " + thongBao);
  }

  private void ghiLichSu(BackupRecord banGhi) {
    try {
      dao.insert(banGhi);
    } catch (DataAccessException e) {
      log.warn("Không ghi được lịch sử sao lưu {}: {}", banGhi.fileName(), e.getMessage());
    }
  }

  private static void chuyenThanhChinhThuc(Path tam, Path dich) throws IOException {
    try {
      Files.move(tam, dich, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(tam, dich, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void xoaTam(Path tam) {
    try {
      Files.deleteIfExists(tam);
    } catch (IOException e) {
      log.warn("Không xóa được tệp tạm {}: {}", tam.toAbsolutePath(), e.getMessage());
    }
  }

  private static String rutGon(String output) {
    if (output == null) {
      return "";
    }
    String chuan = output.strip();
    return chuan.length() <= GIOI_HAN_THONG_BAO
        ? chuan
        : chuan.substring(0, GIOI_HAN_THONG_BAO);
  }
}
