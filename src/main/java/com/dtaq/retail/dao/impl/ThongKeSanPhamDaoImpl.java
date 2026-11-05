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

    // Bán và trả tổng hợp riêng rồi mới ghép theo dòng sản phẩm: một máy có nhiều dòng phiếu trả
    // (phiếu HUY rồi lập phiếu mới) không được nhân bản dòng bán.
    StringBuilder sql = new StringBuilder(
        "WITH ban AS ("
            + " SELECT ct.\"maDongSP\", SUM(ct.\"soLuong\") AS soLuong,"
            + " SUM(ct.\"tienSauKhuyenMai\") AS tien"
            + " FROM \"ChiTietHoaDon\" ct JOIN \"HoaDon\" hd ON hd.\"maHD\" = ct.\"maHD\""
            + " WHERE hd.\"trangThaiThanhToan\" = TRUE AND hd.\"ngayLap\" BETWEEN ? AND ?"
            + " GROUP BY ct.\"maDongSP\"),"
            + " tra AS ("
            + " SELECT ct.\"maDongSP\", COUNT(DISTINCT (ct.\"maHD\", ct.\"soSerial\")) AS soLuong,"
            + " SUM(ct.\"tienSauKhuyenMai\") AS tien"
            + " FROM \"ChiTietHoaDon\" ct"
            + " JOIN (SELECT DISTINCT cpt.\"maHD\", cpt.\"soSerial\""
            + "   FROM \"ChiTietPhieuTra\" cpt"
            + "   JOIN \"PhieuTra\" pt ON pt.\"maPhieuTra\" = cpt.\"maPhieuTra\""
            + "   WHERE pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' AND pt.\"ngayHoanTat\" BETWEEN ? AND ?) daTra"
            + " ON daTra.\"maHD\" = ct.\"maHD\" AND daTra.\"soSerial\" = ct.\"soSerial\""
            + " GROUP BY ct.\"maDongSP\")"
            + " SELECT dsp.\"maDongSP\" AS maDongSP, dsp.\"tenDongSP\" AS tenDongSP,"
            + " dm.\"tenDanhMuc\" AS tenDanhMuc, dsp.\"soLuongTon\" AS soLuongTon,"
            + " COALESCE(ban.soLuong, 0) AS soLuongBan,"
            + " COALESCE(ban.tien, 0) AS tienTruocThue,"
            + " COALESCE(tra.soLuong, 0) AS soLuongTra,"
            + " COALESCE(tra.tien, 0) AS tienTra,"
            + " COALESCE(ban.tien, 0) - COALESCE(tra.tien, 0) AS doanhSoSauDieuChinh"
            + " FROM \"DongSanPham\" dsp"
            + " LEFT JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\""
            + " LEFT JOIN ban ON ban.\"maDongSP\" = dsp.\"maDongSP\""
            + " LEFT JOIN tra ON tra.\"maDongSP\" = dsp.\"maDongSP\"");

    List<Object> params = new ArrayList<>();
    params.add(tuNgay);
    params.add(denNgay);
    params.add(tuNgay);
    params.add(denNgay);
    if (maDongSP != null && !maDongSP.isBlank()) {
      sql.append(" WHERE dsp.\"maDongSP\" = ? ");
      params.add(maDongSP.trim());
    }
    sql.append(" ORDER BY doanhSoSauDieuChinh DESC, dsp.\"maDongSP\" ");
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

  private static final String SQL_DOI_CHIEU =
      "SELECT FALSE AS laTra, hd.\"maHD\" AS maChungTu, hd.\"maHD\", hd.\"ngayLap\" AS ngay,"
          + " ct.\"soSerial\", ct.\"tienSauKhuyenMai\" AS giaTri"
          + " FROM \"ChiTietHoaDon\" ct JOIN \"HoaDon\" hd ON hd.\"maHD\" = ct.\"maHD\""
          + " WHERE hd.\"trangThaiThanhToan\" = TRUE AND hd.\"ngayLap\" BETWEEN ? AND ?"
          + " AND ct.\"maDongSP\" = ?"
          + " UNION ALL"
          + " SELECT TRUE, daTra.\"maPhieuTra\", ct.\"maHD\", daTra.\"ngayHoanTat\", ct.\"soSerial\","
          + " -ct.\"tienSauKhuyenMai\""
          + " FROM \"ChiTietHoaDon\" ct"
          + " JOIN (SELECT DISTINCT ON (cpt.\"maHD\", cpt.\"soSerial\") cpt.\"maHD\", cpt.\"soSerial\","
          + "   pt.\"maPhieuTra\", pt.\"ngayHoanTat\""
          + "   FROM \"ChiTietPhieuTra\" cpt"
          + "   JOIN \"PhieuTra\" pt ON pt.\"maPhieuTra\" = cpt.\"maPhieuTra\""
          + "   WHERE pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' AND pt.\"ngayHoanTat\" BETWEEN ? AND ?"
          + "   ORDER BY cpt.\"maHD\", cpt.\"soSerial\", pt.\"ngayHoanTat\", pt.\"maPhieuTra\") daTra"
          + " ON daTra.\"maHD\" = ct.\"maHD\" AND daTra.\"soSerial\" = ct.\"soSerial\""
          + " WHERE ct.\"maDongSP\" = ?"
          + " ORDER BY ngay, maChungTu";

  @Override
  public List<GiaoDichDoiChieu> doiChieu(LocalDate tuNgay, LocalDate denNgay, String maDongSP) {
    Objects.requireNonNull(tuNgay, "tuNgay");
    Objects.requireNonNull(denNgay, "denNgay");
    Objects.requireNonNull(maDongSP, "maDongSP");
    if (tuNgay.isAfter(denNgay)) {
      throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SQL_DOI_CHIEU)) {
      Object[] params = {tuNgay, denNgay, maDongSP.trim(), tuNgay, denNgay, maDongSP.trim()};
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        List<GiaoDichDoiChieu> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new GiaoDichDoiChieu(
              resultSet.getBoolean(1),
              resultSet.getString(2),
              resultSet.getString(3),
              resultSet.getObject(4, LocalDate.class),
              resultSet.getString(5),
              resultSet.getBigDecimal(6)));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải giao dịch đối chiếu của dòng " + maDongSP, e);
    }
  }
}
