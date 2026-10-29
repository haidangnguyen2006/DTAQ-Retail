package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link SanPhamDao} trên bảng {@code "SanPham"}. */
public final class SanPhamImpl implements SanPhamDao {

  private static final String SELECT_CO_JOIN =
      "SELECT sp." + "\"soSerial\" AS sp_soSerial,"
          + " sp." + "\"maNCC\" AS sp_maNCC,"
          + " sp." + "\"maDongSP\" AS sp_maDongSP,"
          + " sp." + "\"giaNhap\" AS sp_giaNhap,"
          + " sp." + "\"ngayNhapKhoBanDau\" AS sp_ngayNhapKhoBanDau,"
          + " sp." + "\"trangThai\" AS sp_trangThai,"
          + " sp." + "\"soLanDoiTra\" AS sp_soLanDoiTra,"
          + " sp." + "\"soLanSua\" AS sp_soLanSua,"
          + " ncc." + "\"maNCC\" AS ncc_maNCC,"
          + " ncc." + "\"tenNCC\" AS ncc_tenNCC,"
          + " dsp." + "\"maDongSP\" AS dsp_maDongSP,"
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
          + " FROM \"SanPham\" sp"
          + " JOIN \"NhaCungCap\" ncc ON ncc.\"maNCC\" = sp.\"maNCC\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = sp.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\"";

  private static final String SELECT_BY_ID = SELECT_CO_JOIN + " WHERE sp.\"soSerial\" = ?";

  private static final String SELECT_BY_DONG_SAN_PHAM =
      SELECT_CO_JOIN + " WHERE sp.\"maDongSP\" = ? ORDER BY sp.\"soSerial\"";

  private static final String SELECT_BY_TRANG_THAI =
      SELECT_CO_JOIN + " WHERE sp.\"trangThai\" = ? ORDER BY sp.\"soSerial\"";

  private static final String INSERT_SAN_PHAM =
      "INSERT INTO \"SanPham\""
          + " (\"soSerial\", \"maNCC\", \"maDongSP\", \"giaNhap\", \"ngayNhapKhoBanDau\","
          + " \"trangThai\", \"soLanDoiTra\", \"soLanSua\")"
          + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_TRANG_THAI =
      "UPDATE \"SanPham\" SET \"trangThai\" = ? WHERE \"soSerial\" = ?";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public SanPhamImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(Connection ketNoi, SanPham sanPham) {
    if (sanPham == null || sanPham.soSerial() == null || sanPham.soSerial().isBlank()) {
      throw new IllegalArgumentException("Sản phẩm không hợp lệ");
    }
    Objects.requireNonNull(ketNoi, "ketNoi");
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_SAN_PHAM)) {
      statement.setString(1, sanPham.soSerial());
      statement.setString(2, sanPham.nhaCungCap().maNCC());
      statement.setString(3, sanPham.dongSanPham().maDongSP());
      statement.setBigDecimal(4, sanPham.giaNhap());
      statement.setObject(5, sanPham.ngayNhapKhoBanDau());
      statement.setString(6, sanPham.trangThai().name());
      statement.setInt(7, sanPham.soLanDoiTra());
      statement.setInt(8, sanPham.soLanSua());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu sản phẩm " + sanPham.soSerial());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể lưu sản phẩm " + sanPham.soSerial(), e);
    }
  }

  @Override
  public Optional<SanPham> findById(String soSerial) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, soSerial);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapSanPham(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu sản phẩm", e);
    }
  }

  @Override
  public List<SanPham> findByDongSanPham(String maDongSP) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_DONG_SAN_PHAM)) {
      statement.setString(1, maDongSP);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<SanPham> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapSanPham(resultSet));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu sản phẩm theo dòng sản phẩm", e);
    }
  }

  @Override
  public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_TRANG_THAI)) {
      statement.setString(1, trangThai.name());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<SanPham> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapSanPham(resultSet));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu sản phẩm theo trạng thái", e);
    }
  }

  @Override
  public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {
    Objects.requireNonNull(soSerial, "soSerial");
    Objects.requireNonNull(trangThai, "trangThai");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_TRANG_THAI)) {
      statement.setString(1, trangThai.name());
      statement.setString(2, soSerial);
      int affected = statement.executeUpdate();
      if (affected != 1) {
        throw new DataAccessException("Không thể cập nhật trạng thái sản phẩm " + soSerial);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật trạng thái sản phẩm " + soSerial, e);
    }
  }

  private SanPham mapSanPham(ResultSet resultSet) throws SQLException {
    DanhMucSanPham danhMucSanPham = new DanhMucSanPham(
        resultSet.getString("dm_maDanhMuc"),
        resultSet.getString("dm_tenDanhMuc"),
        resultSet.getString("dm_moTa"));

    DongSanPham dongSanPham = new DongSanPham(
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

    NhaCungCap nhaCungCap = new NhaCungCap(
        resultSet.getString("ncc_maNCC"),
        resultSet.getString("ncc_tenNCC"));

    return new SanPham(
        resultSet.getString("sp_soSerial"),
        nhaCungCap,
        dongSanPham,
        resultSet.getBigDecimal("sp_giaNhap"),
        resultSet.getObject("sp_ngayNhapKhoBanDau", LocalDate.class),
        TrangThaiSanPham.valueOf(resultSet.getString("sp_trangThai")),
        resultSet.getInt("sp_soLanDoiTra"),
        resultSet.getInt("sp_soLanSua"));
  }
}
