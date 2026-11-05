package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.ThongKeDoanhThuDao;
import com.dtaq.retail.dao.ThongKeDoanhThuDao.DongDoanhThu;
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
import javax.sql.DataSource;

/** JDBC implementation UC017. Return adjustments use the original invoice snapshots. */
public final class ThongKeDoanhThuDaoImpl implements ThongKeDoanhThuDao {
  private static final String SQL_DOANH_THU = "SELECT hd.\"ngayLap\", ct.\"maDongSP\", dsp.\"tenDongSP\","
      + " SUM(ct.\"tienSauKhuyenMai\"), SUM(ct.\"tienThue\")"
      + " FROM \"HoaDon\" hd JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\"=hd.\"maHD\""
      + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\"=ct.\"maDongSP\""
      + " WHERE hd.\"trangThaiThanhToan\"=true AND hd.\"ngayLap\" BETWEEN ? AND ?"
      + " GROUP BY hd.\"ngayLap\", ct.\"maDongSP\", dsp.\"tenDongSP\" ORDER BY 1,3";
  private static final String SQL_DIEU_CHINH = "SELECT pt.\"ngayHoanTat\", ct.\"maDongSP\", dsp.\"tenDongSP\","
      + " -SUM(ct.\"tienSauKhuyenMai\"), -SUM(ct.\"tienThue\")"
      + " FROM \"PhieuTra\" pt JOIN \"ChiTietPhieuTra\" cpt ON cpt.\"maPhieuTra\"=pt.\"maPhieuTra\""
      + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\"=cpt.\"maHD\" AND ct.\"soSerial\"=cpt.\"soSerial\""
      + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\"=ct.\"maDongSP\""
      + " WHERE pt.\"trangThaiXuLyPhieu\"='HOAN_TAT'"
      + " AND pt.\"trangThaiDuyet\" IN ('KHONG_CAN_DUYET','DA_DUYET')"
      + " AND pt.\"ngayHoanTat\" BETWEEN ? AND ?"
      + " GROUP BY pt.\"ngayHoanTat\", ct.\"maDongSP\", dsp.\"tenDongSP\" ORDER BY 1,3";
  private final DataSource dataSource;
  public ThongKeDoanhThuDaoImpl(DataSource dataSource) { this.dataSource=Objects.requireNonNull(dataSource); }

  @Override
  public List<DongDoanhThu> doanhThu(LocalDate tuNgay, LocalDate denNgay) {
    return truyVan(SQL_DOANH_THU, tuNgay, denNgay);
  }

  @Override
  public List<DongDoanhThu> dieuChinh(LocalDate tuNgay, LocalDate denNgay) {
    return truyVan(SQL_DIEU_CHINH, tuNgay, denNgay);
  }

  private List<DongDoanhThu> truyVan(String sql, LocalDate tuNgay, LocalDate denNgay) {
    if (tuNgay == null || denNgay == null || denNgay.isBefore(tuNgay)) {
      throw new IllegalArgumentException("Kỳ báo cáo không hợp lệ");
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setDate(1, Date.valueOf(tuNgay));
      statement.setDate(2, Date.valueOf(denNgay));
      try (ResultSet resultSet = statement.executeQuery()) {
        List<DongDoanhThu> result = new ArrayList<>();
        while (resultSet.next()) {
          result.add(new DongDoanhThu(
              resultSet.getObject(1, LocalDate.class),
              resultSet.getString(2),
              resultSet.getString(3),
              resultSet.getBigDecimal(4),
              resultSet.getBigDecimal(5)));
        }
        return result;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải thống kê doanh thu", e);
    }
  }
}
