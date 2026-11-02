package com.dtaq.retail.controller;

import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.MaGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý nghiệp vụ quản lý sản phẩm lỗi, tiếp nhận bảo hành và đồng bộ trạng thái kho.
 */
public final class SanPhamLoiController {

  private static final Logger log = LoggerFactory.getLogger(SanPhamLoiController.class);

  private final SanPhamLoiDao sanPhamLoiDao;
  private final SanPhamDao sanPhamDao;
  private final DataSource dataSource;

  /**
   * Khởi tạo bộ điều khiển sản phẩm lỗi.
   *
   * @param sanPhamLoiDao lớp truy cập hồ sơ sản phẩm lỗi; không được {@code null}
   * @param sanPhamDao lớp truy cập dữ liệu máy vật lý; không được {@code null}
   * @param dataSource nguồn dữ liệu kết nối phục vụ quản lý giao dịch và sinh mã; không được {@code null}
   */
  public SanPhamLoiController(
      SanPhamLoiDao sanPhamLoiDao,
      SanPhamDao sanPhamDao,
      DataSource dataSource) {
    this.sanPhamLoiDao = Objects.requireNonNull(sanPhamLoiDao, "sanPhamLoiDao");
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Lấy danh sách sản phẩm lỗi (toàn bộ hoặc lọc theo tình trạng xử lý).
   *
   * @param trangThaiLoc tình trạng cần lọc, hoặc {@code null} để lấy toàn bộ
   * @return danh sách sản phẩm lỗi
   */
  public List<SanPhamLoi> layDanhSach(TrangThaiXuLyLoi trangThaiLoc) {
    try {
      if (trangThaiLoc == null) {
        return sanPhamLoiDao.findAll();
      }
      return sanPhamLoiDao.findByTinhTrang(trangThaiLoc);
    } catch (DataAccessException e) {
      log.error("Lỗi khi tải danh sách sản phẩm lỗi", e);
      return Collections.emptyList();
    }
  }

  /**
   * Tìm hồ sơ lỗi theo mã định danh.
   *
   * @param maLoi mã hồ sơ lỗi
   * @return đối tượng hồ sơ nếu tìm thấy
   */
  public Optional<SanPhamLoi> timTheoMa(String maLoi) {
    if (maLoi == null || maLoi.isBlank()) {
      return Optional.empty();
    }
    try {
      return sanPhamLoiDao.findById(maLoi.trim());
    } catch (DataAccessException e) {
      log.error("Lỗi khi tìm hồ sơ lỗi: {}", maLoi, e);
      return Optional.empty();
    }
  }

  /**
   * Ghi nhận hồ sơ sản phẩm lỗi mới.
   *
   * @param soSerial số serial của máy; không được để trống
   * @param moTaLoi mô tả chi tiết lỗi; không được để trống
   * @param ngayPhatHien ngày phát hiện lỗi; nếu {@code null} lấy ngày hiện tại
   * @param ghiChuXuLy ghi chú xử lý ban đầu; có thể {@code null}
   * @param maPhieuTra mã phiếu trả liên quan nếu có
   * @param maPhieuDoi mã phiếu đổi liên quan nếu có
   * @return kết quả thao tác kèm hồ sơ vừa tạo nếu thành công
   */
  public KetQua<SanPhamLoi> ghiNhanLoi(
      String soSerial,
      String moTaLoi,
      LocalDate ngayPhatHien,
      String ghiChuXuLy,
      String maPhieuTra,
      String maPhieuDoi) {

    if (soSerial == null || soSerial.isBlank()) {
      return KetQua.loi("Số serial không được để trống");
    }
    if (moTaLoi == null || moTaLoi.isBlank()) {
      return KetQua.loi("Mô tả lỗi không được để trống");
    }

    LocalDate ngayGhiNhan = ngayPhatHien != null ? ngayPhatHien : LocalDate.now();
    if (ngayGhiNhan.isAfter(LocalDate.now())) {
      return KetQua.loi("Ngày phát hiện lỗi không được sau ngày hiện tại");
    }

    String serialChuanHoa = soSerial.trim();
    Optional<SanPham> sanPhamOpt;
    try {
      sanPhamOpt = sanPhamDao.findById(serialChuanHoa);
    } catch (DataAccessException e) {
      log.error("Lỗi khi tra cứu sản phẩm theo serial: {}", serialChuanHoa, e);
      return KetQua.loi("Không thể tra cứu sản phẩm trong hệ thống: " + e.getMessage());
    }

    if (sanPhamOpt.isEmpty()) {
      return KetQua.loi("Không tìm thấy sản phẩm có số serial " + serialChuanHoa + " trong hệ thống");
    }

    try {
      Optional<SanPhamLoi> dangXuLyOpt = sanPhamLoiDao.findDangXuLy(serialChuanHoa);
      if (dangXuLyOpt.isPresent()) {
        return KetQua.loi("Máy đã có hồ sơ lỗi đang trong quá trình xử lý (Mã: "
            + dangXuLyOpt.get().maLoi() + ")");
      }
    } catch (DataAccessException e) {
      log.error("Lỗi khi kiểm tra hồ sơ lỗi đang xử lý của máy: {}", serialChuanHoa, e);
      return KetQua.loi("Không thể kiểm tra tình trạng xử lý của máy: " + e.getMessage());
    }

    PhieuTra phieuTra = (maPhieuTra != null && !maPhieuTra.isBlank())
        ? new PhieuTra(maPhieuTra.trim(), null, null, null, null, null, null, null, null, null, null)
        : null;

    PhieuDoi phieuDoi = (maPhieuDoi != null && !maPhieuDoi.isBlank())
        ? new PhieuDoi(maPhieuDoi.trim(), null, null, null, null, null, null, null, null, null, null)
        : null;

    try (Connection conn = dataSource.getConnection()) {
      boolean oldAutoCommit = conn.getAutoCommit();
      conn.setAutoCommit(false);
      try {
        String maLoi = MaGenerator.next(conn, "SanPhamLoi", "maLoi", "SPL");
        SanPham sp = sanPhamOpt.get();
        SanPhamLoi spl = new SanPhamLoi(
            maLoi,
            sp,
            phieuTra,
            phieuDoi,
            ghiChuXuLy != null && !ghiChuXuLy.isBlank() ? ghiChuXuLy.trim() : null,
            moTaLoi.trim(),
            ngayGhiNhan,
            TrangThaiXuLyLoi.CHO_XU_LY,
            null);

        sanPhamLoiDao.insert(conn, spl);

        if (sp.trangThai() == TrangThaiSanPham.SAN_SANG_BAN) {
          capNhatTrangThaiSanPham(conn, serialChuanHoa, TrangThaiSanPham.HANG_LOI);
        }

        conn.commit();
        return KetQua.thanhCong(spl, "Ghi nhận sản phẩm lỗi thành công");
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(oldAutoCommit);
      }
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi lưu hồ sơ sản phẩm lỗi", e);
      if (laLoiTriggerChungTu(e)) {
        return KetQua.loi("Chứng từ liên kết không hợp lệ hoặc không chứa số serial của máy này");
      }
      return KetQua.loi(e.getMessage());
    }
  }

  /**
   * Chuyển trạng thái xử lý hồ sơ lỗi và đồng bộ trạng thái kho của máy vật lý trong một giao dịch.
   *
   * @param maLoi mã hồ sơ lỗi; không được rỗng
   * @param trangThaiMoi trạng thái xử lý mới; không được {@code null}
   * @param ghiChuXuLy nội dung ghi chú xử lý mới
   * @return kết quả thao tác
   */
  public KetQua<Void> chuyenTrangThai(
      String maLoi,
      TrangThaiXuLyLoi trangThaiMoi,
      String ghiChuXuLy) {

    if (maLoi == null || maLoi.isBlank()) {
      return KetQua.loi("Mã hồ sơ lỗi không được để trống");
    }
    if (trangThaiMoi == null) {
      return KetQua.loi("Trạng thái xử lý mới không được để trống");
    }

    String maChuanHoa = maLoi.trim();
    Optional<SanPhamLoi> splOpt;
    try {
      splOpt = sanPhamLoiDao.findById(maChuanHoa);
    } catch (DataAccessException e) {
      log.error("Lỗi khi tìm hồ sơ lỗi để cập nhật trạng thái: {}", maChuanHoa, e);
      return KetQua.loi("Không thể tra cứu hồ sơ lỗi: " + e.getMessage());
    }

    if (splOpt.isEmpty()) {
      return KetQua.loi("Không tìm thấy hồ sơ lỗi: " + maChuanHoa);
    }

    SanPhamLoi spl = splOpt.get();
    String soSerial = spl.sanPham().soSerial();

    TrangThaiSanPham trangThaiSpMoi = switch (trangThaiMoi) {
      case CHO_XU_LY -> TrangThaiSanPham.HANG_LOI;
      case DA_GUI_BAO_HANH -> TrangThaiSanPham.DANG_BAO_HANH;
      case DA_TRA_NHA_CUNG_CAP -> TrangThaiSanPham.DA_TRA_NHA_CUNG_CAP;
      case THANH_LY -> TrangThaiSanPham.DA_THANH_LY;
    };

    try (Connection conn = dataSource.getConnection()) {
      boolean oldAutoCommit = conn.getAutoCommit();
      conn.setAutoCommit(false);
      try {
        sanPhamLoiDao.updateTinhTrang(
            conn,
            maChuanHoa,
            trangThaiMoi,
            ghiChuXuLy != null && !ghiChuXuLy.isBlank() ? ghiChuXuLy.trim() : null,
            LocalDate.now());

        capNhatTrangThaiSanPham(conn, soSerial, trangThaiSpMoi);

        conn.commit();
        return KetQua.thanhCong(null, "Chuyển trạng thái xử lý lỗi thành công");
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(oldAutoCommit);
      }
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi cập nhật trạng thái hồ sơ lỗi", e);
      return KetQua.loi("Không thể cập nhật trạng thái hồ sơ lỗi: " + e.getMessage());
    }
  }

  private void capNhatTrangThaiSanPham(Connection conn, String soSerial, TrangThaiSanPham trangThai)
      throws SQLException {
    String sql = "UPDATE \"SanPham\" SET \"trangThai\" = ? WHERE \"soSerial\" = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, trangThai.name());
      ps.setString(2, soSerial);
      int updated = ps.executeUpdate();
      if (updated == 0) {
        throw new SQLException("Không tìm thấy sản phẩm có số serial: " + soSerial);
      }
    }
  }

  private boolean laLoiTriggerChungTu(Exception e) {
    Throwable cause = e;
    while (cause != null) {
      String msg = cause.getMessage();
      if (msg != null && (msg.contains("trg_sanphamloi_nguon_hop_le")
          || msg.contains("fn_sanphamloi_nguon_hop_le")
          || msg.contains("chứng từ")
          || msg.contains("nguon_hop_le"))) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  /**
   * Đóng gói kết quả trả về của các thao tác nghiệp vụ sản phẩm lỗi.
   *
   * @param <T> kiểu dữ liệu trả về kèm theo khi thao tác thành công
   * @param thanhCong trạng thái thành công hay thất bại
   * @param thongBao thông điệp kết quả
   * @param duLieu dữ liệu đối tượng trả về (có thể {@code null})
   */
  public record KetQua<T>(boolean thanhCong, String thongBao, T duLieu) {

    public static <T> KetQua<T> thanhCong(T duLieu, String thongBao) {
      return new KetQua<>(true, thongBao, duLieu);
    }

    public static <T> KetQua<T> loi(String thongBao) {
      return new KetQua<>(false, thongBao, null);
    }
  }
}
