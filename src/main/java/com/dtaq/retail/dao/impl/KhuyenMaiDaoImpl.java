package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
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
 * Triển khai JDBC của {@link KhuyenMaiDao} thao tác trên bảng {@code "KhuyenMai"}.
 */
public class KhuyenMaiDaoImpl implements KhuyenMaiDao {

  private static final String SELECT_BASE =
      "SELECT km.\"maKM\", km.\"tenKM\", km.\"ngayBatDau\", km.\"ngayKetThuc\", km.\"tyLeGiam\","
          + " km.\"moTa\", km.\"dangApDung\", km.\"maDongSP\","
          + " dsp.\"tenDongSP\", dsp.\"donViTinh\", dsp.\"soLuongTon\", dsp.\"thue\","
          + " dsp.\"donGiaBanTruocThue\", dsp.\"trangThaiKinhDoanh\", dsp.\"ngayNhapKho\","
          + " dsp.\"ngayCapNhapCuoi\", dsp.\"maDanhMuc\""
          + " FROM \"KhuyenMai\" km"
          + " JOIN \"DongSanPham\" dsp ON km.\"maDongSP\" = dsp.\"maDongSP\"";

  private static final String SELECT_ALL =
      SELECT_BASE + " ORDER BY km.\"ngayBatDau\" DESC, km.\"maKM\" DESC";

  private static final String SELECT_BY_ID =
      SELECT_BASE + " WHERE km.\"maKM\" = ?";

  private static final String SELECT_BY_DONG_SP =
      SELECT_BASE + " WHERE km.\"maDongSP\" = ? ORDER BY km.\"ngayBatDau\" DESC, km.\"maKM\" DESC";

  private static final String SELECT_HIEU_LUC =
      SELECT_BASE
          + " WHERE km.\"maDongSP\" = ?"
          + " AND km.\"dangApDung\" = true"
          + " AND ? >= km.\"ngayBatDau\" AND ? <= km.\"ngayKetThuc\""
          + " ORDER BY km.\"tyLeGiam\" DESC, km.\"ngayBatDau\" DESC"
          + " LIMIT 1";

  private static final String INSERT =
      "INSERT INTO \"KhuyenMai\" (\"maKM\", \"tenKM\", \"ngayBatDau\", \"ngayKetThuc\", \"tyLeGiam\","
          + " \"moTa\", \"dangApDung\", \"maDongSP\")"
          + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE =
      "UPDATE \"KhuyenMai\" SET \"tenKM\" = ?, \"ngayBatDau\" = ?, \"ngayKetThuc\" = ?,"
          + " \"tyLeGiam\" = ?, \"moTa\" = ?, \"dangApDung\" = ?, \"maDongSP\" = ?"
          + " WHERE \"maKM\" = ?";

  private static final String UPDATE_TRANG_THAI_AP_DUNG =
      "UPDATE \"KhuyenMai\" SET \"dangApDung\" = ? WHERE \"maKM\" = ?";

  private static final String EXISTS_TRUNG_MA =
      "SELECT EXISTS(SELECT 1 FROM \"KhuyenMai\" WHERE \"maKM\" = ?)";

  private static final String EXISTS_CHONG_LAN =
      "SELECT EXISTS("
          + " SELECT 1 FROM \"KhuyenMai\""
          + " WHERE \"maDongSP\" = ?"
          + " AND \"dangApDung\" = true"
          + " AND NOT (\"ngayKetThuc\" < ? OR \"ngayBatDau\" > ?)"
          + " AND (? IS NULL OR \"maKM\" <> ?)"
          + ")";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung.
   *
   * @param dataSource nguồn dữ liệu kết nối CSDL; không được {@code null}
   */
  public KhuyenMaiDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public Optional<KhuyenMai> findHieuLuc(String maDongSP, LocalDate ngay) {
    if (maDongSP == null || maDongSP.isBlank() || ngay == null) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_HIEU_LUC)) {
      statement.setString(1, maDongSP.trim());
      Date sqlDate = Date.valueOf(ngay);
      statement.setDate(2, sqlDate);
      statement.setDate(3, sqlDate);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapKhuyenMai(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu khuyến mãi hiệu lực cho dòng sản phẩm: " + maDongSP, e);
    }
  }

  @Override
  public Optional<KhuyenMai> findById(String maKM) {
    if (maKM == null || maKM.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maKM.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapKhuyenMai(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu khuyến mãi theo mã: " + maKM, e);
    }
  }

  @Override
  public List<KhuyenMai> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<KhuyenMai> list = new ArrayList<>();
      while (resultSet.next()) {
        list.add(mapKhuyenMai(resultSet));
      }
      return list;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách chương trình khuyến mãi", e);
    }
  }

  @Override
  public List<KhuyenMai> findByDongSanPham(String maDongSP) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_DONG_SP)) {
      statement.setString(1, maDongSP.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<KhuyenMai> list = new ArrayList<>();
        while (resultSet.next()) {
          list.add(mapKhuyenMai(resultSet));
        }
        return list;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách khuyến mãi của dòng sản phẩm: " + maDongSP, e);
    }
  }

  @Override
  public void insert(KhuyenMai km) {
    validateKhuyenMai(km);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT)) {
      statement.setString(1, km.maKM().trim());
      statement.setString(2, km.tenKM().trim());
      statement.setDate(3, Date.valueOf(km.ngayBatDau()));
      statement.setDate(4, Date.valueOf(km.ngayKetThuc()));
      statement.setBigDecimal(5, km.tyLeGiam());
      statement.setString(6, km.moTa());
      statement.setBoolean(7, km.dangApDung());
      statement.setString(8, km.dongSanPham().maDongSP().trim());
      statement.executeUpdate();
    } catch (SQLException e) {
      handleSqlException("Không thể thêm mới chương trình khuyến mãi", e);
    }
  }

  @Override
  public void update(KhuyenMai km) {
    validateKhuyenMai(km);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE)) {
      statement.setString(1, km.tenKM().trim());
      statement.setDate(2, Date.valueOf(km.ngayBatDau()));
      statement.setDate(3, Date.valueOf(km.ngayKetThuc()));
      statement.setBigDecimal(4, km.tyLeGiam());
      statement.setString(5, km.moTa());
      statement.setBoolean(6, km.dangApDung());
      statement.setString(7, km.dongSanPham().maDongSP().trim());
      statement.setString(8, km.maKM().trim());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new DataAccessException("Không tìm thấy chương trình khuyến mãi để cập nhật: " + km.maKM());
      }
    } catch (SQLException e) {
      handleSqlException("Không thể cập nhật chương trình khuyến mãi", e);
    }
  }

  @Override
  public void updateTrangThaiApDung(String maKM, boolean dangApDung) {
    if (maKM == null || maKM.isBlank()) {
      throw new IllegalArgumentException("Mã khuyến mãi không được để trống");
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_TRANG_THAI_AP_DUNG)) {
      statement.setBoolean(1, dangApDung);
      statement.setString(2, maKM.trim());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new DataAccessException("Không tìm thấy chương trình khuyến mãi để đổi trạng thái: " + maKM);
      }
    } catch (SQLException e) {
      handleSqlException("Không thể cập nhật trạng thái áp dụng khuyến mãi", e);
    }
  }

  @Override
  public boolean existsTrungMa(String maKM) {
    if (maKM == null || maKM.isBlank()) {
      return false;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_TRUNG_MA)) {
      statement.setString(1, maKM.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() && resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra trùng mã khuyến mãi", e);
    }
  }

  @Override
  public boolean existsChongLan(String maDongSP, LocalDate ngayBatDau, LocalDate ngayKetThuc, String excludeMaKM) {
    if (maDongSP == null || maDongSP.isBlank() || ngayBatDau == null || ngayKetThuc == null) {
      return false;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_CHONG_LAN)) {
      statement.setString(1, maDongSP.trim());
      statement.setDate(2, Date.valueOf(ngayBatDau));
      statement.setDate(3, Date.valueOf(ngayKetThuc));
      if (excludeMaKM != null && !excludeMaKM.isBlank()) {
        statement.setString(4, excludeMaKM.trim());
        statement.setString(5, excludeMaKM.trim());
      } else {
        statement.setNull(4, java.sql.Types.VARCHAR);
        statement.setNull(5, java.sql.Types.VARCHAR);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() && resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra chồng lấn thời gian khuyến mãi", e);
    }
  }

  private void validateKhuyenMai(KhuyenMai km) {
    Objects.requireNonNull(km, "khuyenMai");
    if (km.maKM() == null || km.maKM().isBlank()) {
      throw new IllegalArgumentException("Mã khuyến mãi không được để trống");
    }
    if (km.tenKM() == null || km.tenKM().isBlank()) {
      throw new IllegalArgumentException("Tên khuyến mãi không được để trống");
    }
    if (km.ngayBatDau() == null || km.ngayKetThuc() == null) {
      throw new IllegalArgumentException("Thời gian khuyến mãi không được để trống");
    }
    if (km.ngayBatDau().isAfter(km.ngayKetThuc())) {
      throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
    }
    if (km.tyLeGiam() == null) {
      throw new IllegalArgumentException("Tỷ lệ giảm giá không được để trống");
    }
    if (km.tyLeGiam().compareTo(BigDecimal.ZERO) < 0 || km.tyLeGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException("Tỷ lệ giảm giá phải từ 0% đến 100%");
    }
    if (km.dongSanPham() == null || km.dongSanPham().maDongSP() == null || km.dongSanPham().maDongSP().isBlank()) {
      throw new IllegalArgumentException("Chương trình khuyến mãi phải gán cho một dòng sản phẩm cụ thể");
    }
  }

  private KhuyenMai mapKhuyenMai(ResultSet rs) throws SQLException {
    DongSanPham dsp = new DongSanPham(
        rs.getString("maDongSP"),
        rs.getString("tenDongSP"),
        null,
        rs.getString("donViTinh"),
        rs.getInt("soLuongTon"),
        rs.getBigDecimal("thue"),
        rs.getBigDecimal("donGiaBanTruocThue"),
        rs.getBoolean("trangThaiKinhDoanh"),
        rs.getObject("ngayNhapKho", LocalDate.class),
        rs.getObject("ngayCapNhapCuoi", LocalDate.class));

    return new KhuyenMai(
        rs.getString("maKM"),
        rs.getString("tenKM"),
        rs.getObject("ngayBatDau", LocalDate.class),
        rs.getObject("ngayKetThuc", LocalDate.class),
        rs.getBigDecimal("tyLeGiam"),
        rs.getString("moTa"),
        rs.getBoolean("dangApDung"),
        dsp);
  }

  private void handleSqlException(String defaultMessage, SQLException e) {
    if ("23505".equals(e.getSQLState())) {
      throw new DataAccessException("Mã khuyến mãi đã tồn tại trong hệ thống", e);
    }
    if ("23503".equals(e.getSQLState())) {
      throw new DataAccessException("Dòng sản phẩm được gán không tồn tại trong hệ thống", e);
    }
    if ("23514".equals(e.getSQLState())) {
      throw new DataAccessException("Thông tin khuyến mãi vi phạm ràng buộc dữ liệu (tỷ lệ hoặc khoảng ngày không hợp lệ)", e);
    }
    throw new DataAccessException(defaultMessage, e);
  }
}
