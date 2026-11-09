package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.ThongKeNhanVienDao;
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

/** Triển khai JDBC chỉ đọc của {@link ThongKeNhanVienDao}. */
public final class ThongKeNhanVienDaoImpl implements ThongKeNhanVienDao {

  /** Hóa đơn đã thanh toán lập trong kỳ, nhóm theo nhân viên lập. */
  private static final String CTE_BAN =
      "ban AS (SELECT hd.\"nhanVienLap\" AS \"maNV\", COUNT(DISTINCT hd.\"maHD\") AS \"soHoaDon\","
          + " SUM(ct.\"tienSauKhuyenMai\") AS \"doanhSo\""
          + " FROM \"HoaDon\" hd JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = hd.\"maHD\""
          + " WHERE hd.\"trangThaiThanhToan\" = TRUE AND hd.\"ngayLap\" BETWEEN ? AND ?"
          + " GROUP BY hd.\"nhanVienLap\")";

  /** Máy trên phiếu trả hoàn tất trong kỳ, định giá theo chi tiết hóa đơn gốc. */
  private static final String CTE_TRA =
      "tra AS (SELECT hd.\"nhanVienLap\" AS \"maNV\", SUM(ct.\"tienSauKhuyenMai\") AS \"dieuChinh\""
          + " FROM \"PhieuTra\" pt"
          + " JOIN \"ChiTietPhieuTra\" c ON c.\"maPhieuTra\" = pt.\"maPhieuTra\""
          + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = c.\"maHD\" AND ct.\"soSerial\" = c.\"soSerial\""
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = c.\"maHD\""
          + " WHERE pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' AND hd.\"trangThaiThanhToan\" = TRUE"
          + " AND pt.\"ngayHoanTat\" BETWEEN ? AND ?"
          + " GROUP BY hd.\"nhanVienLap\")";

  private static final String SELECT_THONG_KE =
      "WITH " + CTE_BAN + ", " + CTE_TRA
          + " SELECT nv.\"maNV\", nv.\"hoTen\", nv.\"trangThaiHoatDong\","
          + " COALESCE(b.\"soHoaDon\", 0) AS \"soHoaDon\", COALESCE(b.\"doanhSo\", 0) AS \"doanhSo\","
          + " COALESCE(t.\"dieuChinh\", 0) AS \"dieuChinh\""
          + " FROM \"NhanVien\" nv"
          + " LEFT JOIN ban b ON b.\"maNV\" = nv.\"maNV\""
          + " LEFT JOIN tra t ON t.\"maNV\" = nv.\"maNV\""
          + " WHERE (b.\"maNV\" IS NOT NULL OR t.\"maNV\" IS NOT NULL)"
          + " AND (CAST(? AS VARCHAR) IS NULL OR nv.\"maNV\" = ?)"
          + " ORDER BY nv.\"maNV\"";

  private static final String SELECT_HOA_DON =
      "SELECT hd.\"maHD\", hd.\"ngayLap\", kh.\"hoTen\" AS \"tenKhachHang\","
          + " COUNT(ct.\"soSerial\") AS \"soSanPham\", SUM(ct.\"tienSauKhuyenMai\") AS \"tienTruocThue\""
          + " FROM \"HoaDon\" hd"
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = hd.\"maHD\""
          + " WHERE hd.\"trangThaiThanhToan\" = TRUE AND hd.\"ngayLap\" BETWEEN ? AND ?"
          + " AND hd.\"nhanVienLap\" = ?"
          + " GROUP BY hd.\"maHD\", hd.\"ngayLap\", kh.\"hoTen\""
          + " ORDER BY hd.\"ngayLap\", hd.\"maHD\"";

  private static final String SELECT_DIEU_CHINH =
      "SELECT pt.\"maPhieuTra\", pt.\"hoaDon\", pt.\"ngayHoanTat\","
          + " COUNT(c.\"soSerial\") AS \"soMay\", SUM(ct.\"tienSauKhuyenMai\") AS \"giaTriTru\""
          + " FROM \"PhieuTra\" pt"
          + " JOIN \"ChiTietPhieuTra\" c ON c.\"maPhieuTra\" = pt.\"maPhieuTra\""
          + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = c.\"maHD\" AND ct.\"soSerial\" = c.\"soSerial\""
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = c.\"maHD\""
          + " WHERE pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' AND hd.\"trangThaiThanhToan\" = TRUE"
          + " AND pt.\"ngayHoanTat\" BETWEEN ? AND ? AND hd.\"nhanVienLap\" = ?"
          + " GROUP BY pt.\"maPhieuTra\", pt.\"hoaDon\", pt.\"ngayHoanTat\""
          + " ORDER BY pt.\"ngayHoanTat\", pt.\"maPhieuTra\"";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public ThongKeNhanVienDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public List<DongThongKeNhanVien> thongKe(LocalDate tuNgay, LocalDate denNgay, String maNV) {
    kiemTraKy(tuNgay, denNgay);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_THONG_KE)) {
      statement.setObject(1, tuNgay);
      statement.setObject(2, denNgay);
      statement.setObject(3, tuNgay);
      statement.setObject(4, denNgay);
      statement.setString(5, maNV);
      statement.setString(6, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<DongThongKeNhanVien> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new DongThongKeNhanVien(
              resultSet.getString("maNV"),
              resultSet.getString("hoTen"),
              resultSet.getBoolean("trangThaiHoatDong"),
              resultSet.getInt("soHoaDon"),
              resultSet.getBigDecimal("doanhSo"),
              resultSet.getBigDecimal("dieuChinh")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể thống kê doanh số theo nhân viên", e);
    }
  }

  @Override
  public List<HoaDonDoiChieu> hoaDonTrongKy(LocalDate tuNgay, LocalDate denNgay, String maNV) {
    kiemTraKy(tuNgay, denNgay);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_HOA_DON)) {
      statement.setObject(1, tuNgay);
      statement.setObject(2, denNgay);
      statement.setString(3, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<HoaDonDoiChieu> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new HoaDonDoiChieu(
              resultSet.getString("maHD"),
              resultSet.getObject("ngayLap", LocalDate.class),
              resultSet.getString("tenKhachHang"),
              resultSet.getInt("soSanPham"),
              resultSet.getBigDecimal("tienTruocThue")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải hóa đơn đối chiếu của nhân viên", e);
    }
  }

  @Override
  public List<DieuChinhDoiChieu> dieuChinhTrongKy(LocalDate tuNgay, LocalDate denNgay, String maNV) {
    kiemTraKy(tuNgay, denNgay);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_DIEU_CHINH)) {
      statement.setObject(1, tuNgay);
      statement.setObject(2, denNgay);
      statement.setString(3, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<DieuChinhDoiChieu> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new DieuChinhDoiChieu(
              resultSet.getString("maPhieuTra"),
              resultSet.getString("hoaDon"),
              resultSet.getObject("ngayHoanTat", LocalDate.class),
              resultSet.getInt("soMay"),
              resultSet.getBigDecimal("giaTriTru")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải điều chỉnh đổi/trả của nhân viên", e);
    }
  }

  private static void kiemTraKy(LocalDate tuNgay, LocalDate denNgay) {
    if (tuNgay == null || denNgay == null || tuNgay.isAfter(denNgay)) {
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
    }
  }
}
