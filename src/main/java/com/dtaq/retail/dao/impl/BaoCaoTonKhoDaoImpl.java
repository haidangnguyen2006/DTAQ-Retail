package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.BaoCaoTonKhoDao;
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
import javax.sql.DataSource;

/** Triển khai JDBC của báo cáo tồn kho lâu ngày trên PostgreSQL. */
public final class BaoCaoTonKhoDaoImpl implements BaoCaoTonKhoDao {

  private static final String SELECT_TON_LAU =
      "SELECT sp.\"soSerial\", sp.\"maDongSP\", dsp.\"tenDongSP\", dm.\"tenDanhMuc\","
          + " sp.\"ngayNhapKhoBanDau\", sp.\"trangThai\","
          + " CURRENT_DATE - sp.\"ngayNhapKhoBanDau\" AS \"soNgayTon\""
          + " FROM \"SanPham\" sp"
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = sp.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\""
          + " WHERE sp.\"trangThai\" IN ('SAN_SANG_BAN', 'HANG_LOI', 'DANG_BAO_HANH')"
          + " AND (? = false OR sp.\"trangThai\" = 'SAN_SANG_BAN')"
          + " AND CURRENT_DATE - sp.\"ngayNhapKhoBanDau\" >= ?"
          + " ORDER BY dsp.\"tenDongSP\", sp.\"ngayNhapKhoBanDau\", sp.\"soSerial\"";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO dùng nguồn dữ liệu chung của ứng dụng.
   *
   * @param dataSource connection pool dùng chung; không được {@code null}
   */
  public BaoCaoTonKhoDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public List<DongTonLau> findTonLau(int nguongNgay, boolean chiTinhSanSangBan) {
    if (nguongNgay <= 0) {
      throw new IllegalArgumentException("Ngưỡng ngày tồn kho phải lớn hơn 0");
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_TON_LAU)) {
      statement.setBoolean(1, chiTinhSanSangBan);
      statement.setInt(2, nguongNgay);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<DongTonLau> result = new ArrayList<>();
        while (resultSet.next()) {
          result.add(new DongTonLau(
              resultSet.getString("soSerial"),
              resultSet.getString("maDongSP"),
              resultSet.getString("tenDongSP"),
              resultSet.getString("tenDanhMuc"),
              resultSet.getObject("ngayNhapKhoBanDau", LocalDate.class),
              TrangThaiSanPham.valueOf(resultSet.getString("trangThai")),
              resultSet.getLong("soNgayTon")));
        }
        return result;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải báo cáo máy tồn kho lâu ngày", e);
    }
  }
}
