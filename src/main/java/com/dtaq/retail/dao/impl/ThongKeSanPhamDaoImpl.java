package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.ThongKeSanPhamDao;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/** Triển khai JDBC của báo cáo doanh số theo sản phẩm trên PostgreSQL. */
public final class ThongKeSanPhamDaoImpl implements ThongKeSanPhamDao {

  private final DataSource dataSource;

  public ThongKeSanPhamDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public List<DongThongKe> thongKe(LocalDate tuNgay, LocalDate denNgay, String maDongSP) {
    Objects.requireNonNull(tuNgay, "tuNgay");
    Objects.requireNonNull(denNgay, "denNgay");
    if (tuNgay.isAfter(denNgay)) {
      throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
    }

    StringBuilder sql = new StringBuilder(
        "SELECT dsp.\"maDongSP\" AS maDongSP, dsp.\"tenDongSP\" AS tenDongSP, "
            + "dm.\"tenDanhMuc\" AS tenDanhMuc, "
            + "dsp.\"soLuongTon\" AS soLuongTon, "
            + "COALESCE(SUM(CASE WHEN hd.\"maHD\" IS NOT NULL AND hd.\"trangThaiThanhToan\" = TRUE "
            + "THEN ct.\"soLuong\" ELSE 0 END), 0) AS soLuongBan, "
            + "COALESCE(SUM(CASE WHEN hd.\"maHD\" IS NOT NULL AND hd.\"trangThaiThanhToan\" = TRUE "
            + "THEN ct.\"tienSauKhuyenMai\" ELSE 0 END), 0) AS tienTruocThue, "
            + "COALESCE(COUNT(DISTINCT CASE WHEN pt.\"maPhieuTra\" IS NOT NULL "
            + "AND pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' "
            + "AND pt.\"ngayHoanTat\" BETWEEN ? AND ? THEN ct.\"soSerial\" END), 0) AS soLuongTra, "
            + "COALESCE(SUM(CASE WHEN pt.\"maPhieuTra\" IS NOT NULL "
            + "AND pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' "
            + "AND pt.\"ngayHoanTat\" BETWEEN ? AND ? THEN ct.\"tienSauKhuyenMai\" ELSE 0 END), 0) "
            + "AS tienTra, "
            + "COALESCE(SUM(CASE WHEN hd.\"maHD\" IS NOT NULL AND hd.\"trangThaiThanhToan\" = TRUE "
            + "THEN ct.\"tienSauKhuyenMai\" ELSE 0 END), 0) "
            + "- COALESCE(SUM(CASE WHEN pt.\"maPhieuTra\" IS NOT NULL "
            + "AND pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' "
            + "AND pt.\"ngayHoanTat\" BETWEEN ? AND ? THEN ct.\"tienSauKhuyenMai\" ELSE 0 END), 0) "
            + "AS doanhSoSauDieuChinh "
            + "FROM \"DongSanPham\" dsp "
            + "LEFT JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\" "
            + "LEFT JOIN \"ChiTietHoaDon\" ct ON ct.\"maDongSP\" = dsp.\"maDongSP\" "
            + "LEFT JOIN \"HoaDon\" hd ON hd.\"maHD\" = ct.\"maHD\" "
            + "AND hd.\"trangThaiThanhToan\" = TRUE "
            + "AND hd.\"ngayLap\" >= ? AND hd.\"ngayLap\" <= ? "
            + "LEFT JOIN \"ChiTietPhieuTra\" cpt ON cpt.\"maHD\" = ct.\"maHD\" "
            + "AND cpt.\"soSerial\" = ct.\"soSerial\" "
            + "LEFT JOIN \"PhieuTra\" pt ON pt.\"maPhieuTra\" = cpt.\"maPhieuTra\" "
            + "AND pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' "
            + "AND pt.\"ngayHoanTat\" >= ? AND pt.\"ngayHoanTat\" <= ? ");

    List<Object> params = new ArrayList<>();
    params.add(tuNgay);
    params.add(denNgay);
    params.add(tuNgay);
    params.add(denNgay);
    params.add(tuNgay);
    params.add(denNgay);
    params.add(tuNgay);
    params.add(denNgay);
    params.add(tuNgay);
    params.add(denNgay);

    if (maDongSP != null && !maDongSP.isBlank()) {
      sql.append(" WHERE dsp.\"maDongSP\" = ? ");
      params.add(maDongSP.trim());
    }

    sql.append(" GROUP BY dsp.\"maDongSP\", dsp.\"tenDongSP\", dm.\"tenDanhMuc\" "
        + "ORDER BY doanhSoSauDieuChinh DESC, dsp.\"maDongSP\" ");

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        List<DongThongKe> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new DongThongKe(
              resultSet.getString("maDongSP"),
              resultSet.getString("tenDongSP"),
              resultSet.getString("tenDanhMuc"),
              resultSet.getInt("soLuongTon"),
              resultSet.getLong("soLuongBan"),
              resultSet.getBigDecimal("tienTruocThue"),
              resultSet.getLong("soLuongTra"),
              resultSet.getBigDecimal("doanhSoSauDieuChinh")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tính doanh số theo sản phẩm", e);
    }
  }
}
