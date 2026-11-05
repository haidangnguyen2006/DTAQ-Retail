package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.ThongKeDoanhThuDao;
import com.dtaq.retail.dao.ThongKeDoanhThuDao.DongDoanhThu;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import javax.sql.DataSource;

/** JDBC implementation UC017. Return adjustments use the original invoice snapshots. */
public final class ThongKeDoanhThuDaoImpl implements ThongKeDoanhThuDao {
  private static final String SQL = "WITH giao_dich AS ("
      + " SELECT hd.\"ngayLap\" ngay, ct.\"maDongSP\" ma_dong, dsp.\"tenDongSP\" ten_dong,"
      + " ct.\"tienSauKhuyenMai\" truoc_thue, ct.\"tienThue\" thue"
      + " FROM \"HoaDon\" hd JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\"=hd.\"maHD\""
      + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\"=ct.\"maDongSP\""
      + " WHERE hd.\"trangThaiThanhToan\"=true AND hd.\"ngayLap\" BETWEEN ? AND ?"
      + " UNION ALL"
      + " SELECT pt.\"ngayHoanTat\", ct.\"maDongSP\", dsp.\"tenDongSP\","
      + " -ct.\"tienSauKhuyenMai\", -ct.\"tienThue\""
      + " FROM \"PhieuTra\" pt JOIN \"ChiTietPhieuTra\" cpt ON cpt.\"maPhieuTra\"=pt.\"maPhieuTra\""
      + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\"=cpt.\"maHD\" AND ct.\"soSerial\"=cpt.\"soSerial\""
      + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\"=ct.\"maDongSP\""
      + " WHERE pt.\"trangThaiXuLyPhieu\"='HOAN_TAT' AND pt.\"ngayHoanTat\" BETWEEN ? AND ?"
      + ") SELECT ngay, ma_dong, ten_dong, SUM(truoc_thue) truoc_thue, SUM(thue) thue"
      + " FROM giao_dich GROUP BY ngay, ma_dong, ten_dong ORDER BY ngay, ten_dong";
  private final DataSource dataSource;
  public ThongKeDoanhThuDaoImpl(DataSource dataSource) { this.dataSource=Objects.requireNonNull(dataSource); }

  @Override public List<DongDoanhThu> find(LocalDate tuNgay, LocalDate denNgay) {
    if (tuNgay == null || denNgay == null || denNgay.isBefore(tuNgay)) throw new IllegalArgumentException("Kỳ báo cáo không hợp lệ");
    try (Connection c=dataSource.getConnection(); PreparedStatement ps=c.prepareStatement(SQL)) {
      ps.setDate(1,Date.valueOf(tuNgay)); ps.setDate(2,Date.valueOf(denNgay));
      ps.setDate(3,Date.valueOf(tuNgay)); ps.setDate(4,Date.valueOf(denNgay));
      try (ResultSet rs=ps.executeQuery()) { List<DongDoanhThu> out=new ArrayList<>(); while(rs.next()) out.add(new DongDoanhThu(rs.getObject(1,LocalDate.class),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),rs.getBigDecimal(5))); return out; }
    } catch(SQLException e) { throw new DataAccessException("Không thể tải thống kê doanh thu",e); }
  }
}
