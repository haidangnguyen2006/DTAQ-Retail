package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Triển khai JDBC của {@link SanPhamLoiDao} thao tác trên bảng {@code "SanPhamLoi"}.
 */
public class SanPhamLoiDaoImpl implements SanPhamLoiDao {

  private static final String SELECT_BASE =
      "SELECT spl.\"maLoi\", spl.\"soSerial\", spl.\"maPhieuTra\", spl.\"maPhieuDoi\","
          + " spl.\"ghiChuXuLy\", spl.\"moTaLoi\", spl.\"ngayGhiNhan\", spl.\"tinhTrangXuLy\", spl.\"ngayCapNhap\","
          + " sp.\"giaNhap\", sp.\"ngayNhapKhoBanDau\", sp.\"trangThai\", sp.\"soLanDoiTra\", sp.\"soLanSua\","
          + " sp.\"maNCC\", ncc.\"tenNCC\","
          + " sp.\"maDongSP\", dsp.\"tenDongSP\", dsp.\"donViTinh\", dsp.\"soLuongTon\","
          + " dsp.\"thue\", dsp.\"donGiaBanTruocThue\", dsp.\"trangThaiKinhDoanh\","
          + " dsp.\"ngayNhapKho\", dsp.\"ngayCapNhapCuoi\", dsp.\"maDanhMuc\""
          + " FROM \"SanPhamLoi\" spl"
          + " JOIN \"SanPham\" sp ON spl.\"soSerial\" = sp.\"soSerial\""
          + " JOIN \"DongSanPham\" dsp ON sp.\"maDongSP\" = dsp.\"maDongSP\""
          + " JOIN \"NhaCungCap\" ncc ON sp.\"maNCC\" = ncc.\"maNCC\"";

  private static final String SELECT_ALL =
      SELECT_BASE + " ORDER BY spl.\"ngayGhiNhan\" DESC, spl.\"maLoi\" DESC";

  private static final String SELECT_BY_ID =
      SELECT_BASE + " WHERE spl.\"maLoi\" = ?";

  private static final String SELECT_DANG_XU_LY =
      SELECT_BASE + " WHERE spl.\"soSerial\" = ? AND spl.\"tinhTrangXuLy\" = 'CHO_XU_LY' LIMIT 1";

  private static final String SELECT_BY_TINH_TRANG =
      SELECT_BASE + " WHERE spl.\"tinhTrangXuLy\" = ? ORDER BY spl.\"ngayGhiNhan\" DESC, spl.\"maLoi\" DESC";

  private static final String INSERT =
      "INSERT INTO \"SanPhamLoi\" (\"maLoi\", \"soSerial\", \"maPhieuTra\", \"maPhieuDoi\","
          + " \"ghiChuXuLy\", \"moTaLoi\", \"ngayGhiNhan\", \"tinhTrangXuLy\", \"ngayCapNhap\")"
          + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_TINH_TRANG =
      "UPDATE \"SanPhamLoi\" SET \"tinhTrangXuLy\" = ?, \"ghiChuXuLy\" = ?, \"ngayCapNhap\" = ?"
          + " WHERE \"maLoi\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung.
   *
   * @param dataSource nguồn dữ liệu kết nối CSDL; không được {@code null}
   */
  public SanPhamLoiDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(Connection ketNoi, SanPhamLoi sanPhamLoi) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    validateForInsert(sanPhamLoi);

    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT)) {
      bindInsertStatement(statement, sanPhamLoi);
      statement.executeUpdate();
    } catch (SQLException e) {
      handleSqlException("Không thể ghi nhận sản phẩm lỗi", e);
    }
  }

  @Override
  public void insert(SanPhamLoi sanPhamLoi) {
    try (Connection connection = dataSource.getConnection()) {
      insert(connection, sanPhamLoi);
    } catch (SQLException e) {
      throw new DataAccessException("Không thể mở kết nối ghi nhận sản phẩm lỗi", e);
    }
  }

  @Override
  public Optional<SanPhamLoi> findById(String maLoi) {
    if (maLoi == null || maLoi.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maLoi.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapSanPhamLoi(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu sản phẩm lỗi theo mã: " + maLoi, e);
    }
  }

  @Override
  public Optional<SanPhamLoi> findDangXuLy(String soSerial) {
    if (soSerial == null || soSerial.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_DANG_XU_LY)) {
      statement.setString(1, soSerial.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapSanPhamLoi(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra hồ sơ lỗi đang xử lý của máy: " + soSerial, e);
    }
  }

  @Override
  public List<SanPhamLoi> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<SanPhamLoi> list = new ArrayList<>();
      while (resultSet.next()) {
        list.add(mapSanPhamLoi(resultSet));
      }
      return list;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách sản phẩm lỗi", e);
    }
  }

  @Override
  public List<SanPhamLoi> findByTinhTrang(TrangThaiXuLyLoi tinhTrangXuLy) {
    Objects.requireNonNull(tinhTrangXuLy, "tinhTrangXuLy");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_TINH_TRANG)) {
      statement.setString(1, tinhTrangXuLy.name());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<SanPhamLoi> list = new ArrayList<>();
        while (resultSet.next()) {
          list.add(mapSanPhamLoi(resultSet));
        }
        return list;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lọc danh sách sản phẩm lỗi theo trạng thái", e);
    }
  }

  @Override
  public void updateTinhTrang(
      Connection ketNoi,
      String maLoi,
      TrangThaiXuLyLoi tinhTrangMoi,
      String ghiChuXuLy,
      LocalDate ngayCapNhat) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(tinhTrangMoi, "tinhTrangMoi");
    Objects.requireNonNull(ngayCapNhat, "ngayCapNhat");
    if (maLoi == null || maLoi.isBlank()) {
      throw new IllegalArgumentException("Mã hồ sơ lỗi không được để trống");
    }

    try (PreparedStatement statement = ketNoi.prepareStatement(UPDATE_TINH_TRANG)) {
      statement.setString(1, tinhTrangMoi.name());
      statement.setString(2, ghiChuXuLy);
      statement.setDate(3, Date.valueOf(ngayCapNhat));
      statement.setString(4, maLoi.trim());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new DataAccessException("Không tìm thấy hồ sơ lỗi để cập nhật: " + maLoi);
      }
    } catch (SQLException e) {
      handleSqlException("Không thể cập nhật trạng thái hồ sơ sản phẩm lỗi", e);
    }
  }

  @Override
  public void updateTinhTrang(
      String maLoi,
      TrangThaiXuLyLoi tinhTrangMoi,
      String ghiChuXuLy,
      LocalDate ngayCapNhat) {
    try (Connection connection = dataSource.getConnection()) {
      updateTinhTrang(connection, maLoi, tinhTrangMoi, ghiChuXuLy, ngayCapNhat);
    } catch (SQLException e) {
      throw new DataAccessException("Không thể mở kết nối cập nhật sản phẩm lỗi", e);
    }
  }

  private void validateForInsert(SanPhamLoi spl) {
    Objects.requireNonNull(spl, "sanPhamLoi");
    if (spl.maLoi() == null || spl.maLoi().isBlank()) {
      throw new IllegalArgumentException("Mã lỗi không được để trống");
    }
    if (spl.sanPham() == null || spl.sanPham().soSerial() == null || spl.sanPham().soSerial().isBlank()) {
      throw new IllegalArgumentException("Máy phát sinh lỗi phải xác định được số serial");
    }
    if (spl.moTaLoi() == null || spl.moTaLoi().isBlank()) {
      throw new IllegalArgumentException("Mô tả lỗi không được để trống");
    }
    if (spl.ngayGhiNhan() == null) {
      throw new IllegalArgumentException("Ngày ghi nhận lỗi không được để trống");
    }
    if (spl.tinhTrangXuLy() == null) {
      throw new IllegalArgumentException("Tình trạng xử lý lỗi không được để trống");
    }
  }

  private void bindInsertStatement(PreparedStatement ps, SanPhamLoi spl) throws SQLException {
    ps.setString(1, spl.maLoi().trim());
    ps.setString(2, spl.sanPham().soSerial().trim());
    ps.setString(3, (spl.phieuTra() != null) ? spl.phieuTra().maPhieuTra() : null);
    ps.setString(4, (spl.phieuDoi() != null) ? spl.phieuDoi().maPhieuDoi() : null);
    ps.setString(5, spl.ghiChuXuLy());
    ps.setString(6, spl.moTaLoi().trim());
    ps.setDate(7, Date.valueOf(spl.ngayGhiNhan()));
    ps.setString(8, spl.tinhTrangXuLy().name());
    if (spl.ngayCapNhap() != null) {
      ps.setDate(9, Date.valueOf(spl.ngayCapNhap()));
    } else {
      ps.setNull(9, java.sql.Types.DATE);
    }
  }

  private SanPhamLoi mapSanPhamLoi(ResultSet rs) throws SQLException {
    NhaCungCap ncc = new NhaCungCap(
        rs.getString("maNCC"),
        rs.getString("tenNCC"));

    DongSanPham dsp = new DongSanPham(
        rs.getString("maDongSP"),
        rs.getString("tenDongSP"),
        null, // DanhMucSanPham tra cứu riêng nếu cần
        rs.getString("donViTinh"),
        rs.getInt("soLuongTon"),
        rs.getBigDecimal("thue"),
        rs.getBigDecimal("donGiaBanTruocThue"),
        rs.getBoolean("trangThaiKinhDoanh"),
        rs.getObject("ngayNhapKho", LocalDate.class),
        rs.getObject("ngayCapNhapCuoi", LocalDate.class));

    SanPham sp = new SanPham(
        rs.getString("soSerial"),
        ncc,
        dsp,
        rs.getBigDecimal("giaNhap"),
        rs.getObject("ngayNhapKhoBanDau", LocalDate.class),
        TrangThaiSanPham.valueOf(rs.getString("trangThai")),
        rs.getInt("soLanDoiTra"),
        rs.getInt("soLanSua"));

    String maPhieuTra = rs.getString("maPhieuTra");
    PhieuTra phieuTra = (maPhieuTra != null) ? new PhieuTra(
        maPhieuTra, null, null, null, null, null, null, null, null, null, null) : null;

    String maPhieuDoi = rs.getString("maPhieuDoi");
    PhieuDoi phieuDoi = (maPhieuDoi != null) ? new PhieuDoi(
        maPhieuDoi, null, null, null, null, null, null, null, null, null, null) : null;

    return new SanPhamLoi(
        rs.getString("maLoi"),
        sp,
        phieuTra,
        phieuDoi,
        rs.getString("ghiChuXuLy"),
        rs.getString("moTaLoi"),
        rs.getObject("ngayGhiNhan", LocalDate.class),
        TrangThaiXuLyLoi.valueOf(rs.getString("tinhTrangXuLy")),
        rs.getObject("ngayCapNhap", LocalDate.class));
  }

  private void handleSqlException(String defaultMessage, SQLException e) {
    if ("23505".equals(e.getSQLState())) {
      throw new DataAccessException("Mã hồ sơ lỗi đã tồn tại trong hệ thống", e);
    }
    if ("23503".equals(e.getSQLState())) {
      throw new DataAccessException("Số serial hoặc chứng từ đổi/trả không tồn tại", e);
    }
    // Lỗi từ trigger CSDL fn_sanphamloi_nguon_hop_le
    if ("P0001".equals(e.getSQLState())) {
      throw new DataAccessException("Chứng từ đổi/trả không chứa số serial của máy lỗi này", e);
    }
    throw new DataAccessException(defaultMessage, e);
  }
}
