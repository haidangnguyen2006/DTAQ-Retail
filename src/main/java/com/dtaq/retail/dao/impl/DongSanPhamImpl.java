package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link DongSanPhamDao} trên bảng {@code "DongSanPham"}. */
public final class DongSanPhamImpl implements DongSanPhamDao {

  private static final String SELECT_CO_JOIN =
      "SELECT dsp." + "\"maDongSP\" AS dsp_maDongSP,"
          + " dsp." + "\"tenDongSP\" AS dsp_tenDongSP,"
          + " dsp." + "\"maDanhMuc\" AS dsp_maDanhMuc,"
          + " dsp." + "\"donViTinh\" AS dsp_donViTinh,"
          + " dsp." + "\"soLuongTon\" AS dsp_soLuongTon,"
          + " dsp." + "\"thue\" AS dsp_thue,"
          + " dsp." + "\"donGiaBanTruocThue\" AS dsp_donGiaBanTruocThue,"
          + " dsp." + "\"trangThaiKinhDoanh\" AS dsp_trangThaiKinhDoanh,"
          + " dsp." + "\"ngayNhapKho\" AS dsp_ngayNhapKho,"
          + " dsp." + "\"ngayCapNhapCuoi\" AS dsp_ngayCapNhapCuoi,"
          + " dm." + "\"maDanhMuc\" AS dm_maDanhMuc,"
          + " dm." + "\"tenDanhMuc\" AS dm_tenDanhMuc,"
          + " dm." + "\"moTa\" AS dm_moTa"
          + " FROM \"DongSanPham\" dsp"
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\"";

  private static final String SELECT_BY_ID = SELECT_CO_JOIN + " WHERE dsp.\"maDongSP\" = ?";

  private static final String SELECT_ALL = SELECT_CO_JOIN + " ORDER BY dsp.\"maDongSP\"";

  private static final String SELECT_BY_DANH_MUC =
      SELECT_CO_JOIN + " WHERE dsp.\"maDanhMuc\" = ? ORDER BY dsp.\"tenDongSP\"";

  private static final String INSERT_DONG_SAN_PHAM =
      "INSERT INTO \"DongSanPham\""
          + " (\"maDongSP\", \"tenDongSP\", \"maDanhMuc\", \"donViTinh\", \"soLuongTon\","
          + " \"thue\", \"donGiaBanTruocThue\", \"trangThaiKinhDoanh\", \"ngayNhapKho\","
          + " \"ngayCapNhapCuoi\")"
          + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_SO_LUONG_TON =
      "UPDATE \"DongSanPham\" SET \"soLuongTon\" = ? WHERE \"maDongSP\" = ?";

  private static final String UPDATE_TRANG_THAI_KINH_DOANH =
      "UPDATE \"DongSanPham\" SET \"trangThaiKinhDoanh\" = ? WHERE \"maDongSP\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public DongSanPhamImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(Connection ketNoi, DongSanPham dongSanPham) {
    if (dongSanPham == null || dongSanPham.maDongSP() == null || dongSanPham.maDongSP().isBlank()) {
      throw new IllegalArgumentException("Dòng sản phẩm không hợp lệ");
    }
    Objects.requireNonNull(ketNoi, "ketNoi");
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_DONG_SAN_PHAM)) {
      statement.setString(1, dongSanPham.maDongSP());
      statement.setString(2, dongSanPham.tenDongSP());
      statement.setString(3, dongSanPham.danhMucSanPham().maDanhMuc());
      statement.setString(4, dongSanPham.donViTinh());
      statement.setInt(5, dongSanPham.soLuongTon());
      statement.setBigDecimal(6, dongSanPham.thue());
      statement.setBigDecimal(7, dongSanPham.donGiaBanTruocThue());
      statement.setBoolean(8, dongSanPham.trangThaiKinhDoanh());
      if (dongSanPham.ngayNhapKho() == null) {
        statement.setNull(9, Types.DATE);
      } else {
        statement.setObject(9, dongSanPham.ngayNhapKho());
      }
      if (dongSanPham.ngayCapNhapCuoi() == null) {
        statement.setNull(10, Types.DATE);
      } else {
        statement.setObject(10, dongSanPham.ngayCapNhapCuoi());
      }
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu dòng sản phẩm " + dongSanPham.maDongSP());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu dòng sản phẩm " + dongSanPham.maDongSP(), e);
    }
  }

  @Override
  public Optional<DongSanPham> findById(String maDongSP) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maDongSP);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapDongSanPham(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu dòng sản phẩm", e);
    }
  }

  @Override
  public List<DongSanPham> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      List<DongSanPham> ketQua = new ArrayList<>();
      while (resultSet.next()) {
        ketQua.add(mapDongSanPham(resultSet));
      }
      return ketQua;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu danh sách dòng sản phẩm", e);
    }
  }

  @Override
  public List<DongSanPham> findByDanhMuc(String maDanhMuc) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_DANH_MUC)) {
      statement.setString(1, maDanhMuc);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<DongSanPham> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapDongSanPham(resultSet));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu dòng sản phẩm theo danh mục", e);
    }
  }

  @Override
  public void updateSoLuongTon(String maDongSP, int soLuongTon) {
    Objects.requireNonNull(maDongSP, "maDongSP");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SO_LUONG_TON)) {
      statement.setInt(1, soLuongTon);
      statement.setString(2, maDongSP);
      int affected = statement.executeUpdate();
      if (affected != 1) {
        throw new DataAccessException("Không thể cập nhật số lượng tồn của dòng sản phẩm " + maDongSP);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật số lượng tồn của dòng sản phẩm " + maDongSP, e);
    }
  }

  @Override
  public void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh) {
    Objects.requireNonNull(maDongSP, "maDongSP");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_TRANG_THAI_KINH_DOANH)) {
      statement.setBoolean(1, trangThaiKinhDoanh);
      statement.setString(2, maDongSP);
      int affected = statement.executeUpdate();
      if (affected != 1) {
        throw new DataAccessException(
            "Không thể cập nhật trạng thái kinh doanh của dòng sản phẩm " + maDongSP);
      }
    } catch (SQLException e) {
      throw new DataAccessException(
          "Không thể cập nhật trạng thái kinh doanh của dòng sản phẩm " + maDongSP, e);
    }
  }

  private DongSanPham mapDongSanPham(ResultSet resultSet) throws SQLException {
    DanhMucSanPham danhMucSanPham = new DanhMucSanPham(
        resultSet.getString("dm_maDanhMuc"),
        resultSet.getString("dm_tenDanhMuc"),
        resultSet.getString("dm_moTa"));

    return new DongSanPham(
        resultSet.getString("dsp_maDongSP"),
        resultSet.getString("dsp_tenDongSP"),
        danhMucSanPham,
        resultSet.getString("dsp_donViTinh"),
        resultSet.getInt("dsp_soLuongTon"),
        resultSet.getBigDecimal("dsp_thue"),
        resultSet.getBigDecimal("dsp_donGiaBanTruocThue"),
        resultSet.getBoolean("dsp_trangThaiKinhDoanh"),
        resultSet.getObject("dsp_ngayNhapKho", LocalDate.class),
        resultSet.getObject("dsp_ngayCapNhapCuoi", LocalDate.class));
  }
}
