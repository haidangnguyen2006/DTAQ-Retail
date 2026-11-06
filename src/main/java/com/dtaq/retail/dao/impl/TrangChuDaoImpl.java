package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.TrangChuDao;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link TrangChuDao}; mọi truy vấn chỉ đọc và gom số liệu tại CSDL. */
public final class TrangChuDaoImpl implements TrangChuDao {

  // Tổng thanh toán = tổng các dòng đã làm tròn (C-11), không làm tròn lại.
  private static final String TONG_QUAN_BAN_HANG =
      "SELECT COUNT(DISTINCT hd.\"maHD\"),"
          + " COALESCE(SUM(ct.\"tienSauKhuyenMai\" + ct.\"tienThue\"), 0),"
          + " COALESCE(SUM(ct.\"tienThue\"), 0),"
          + " COUNT(DISTINCT hd.\"khachHang\")"
          + " FROM \"HoaDon\" hd JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = hd.\"maHD\""
          + " WHERE hd.\"trangThaiThanhToan\" = TRUE AND hd.\"ngayLap\" = ?"
          + " AND (?::VARCHAR IS NULL OR hd.\"nhanVienLap\" = ?)";

  private static final String HOA_DON_GAN_DAY =
      "SELECT hd.\"maHD\", kh.\"hoTen\", hd.\"trangThaiThanhToan\","
          + " COALESCE(SUM(ct.\"tienSauKhuyenMai\" + ct.\"tienThue\"), 0)"
          + " FROM \"HoaDon\" hd JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " LEFT JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = hd.\"maHD\""
          + " WHERE hd.\"ngayLap\" = ? AND (?::VARCHAR IS NULL OR hd.\"nhanVienLap\" = ?)"
          + " GROUP BY hd.\"maHD\", kh.\"hoTen\", hd.\"trangThaiThanhToan\""
          + " ORDER BY hd.\"maHD\" DESC LIMIT ?";

  private static final String DOI_TRA_TRONG_NGAY =
      "SELECT COALESCE(SUM(CASE WHEN \"trangThaiXuLyPhieu\" = 'HOAN_TAT' AND \"ngayHoanTat\" = ?"
          + " THEN 1 ELSE 0 END), 0),"
          + " COALESCE(SUM(CASE WHEN \"trangThaiDuyet\" = 'DA_DUYET' AND \"ngayDuyet\" = ?"
          + " THEN 1 ELSE 0 END), 0),"
          + " COALESCE(SUM(CASE WHEN \"trangThaiDuyet\" = 'TU_CHOI' AND \"ngayDuyet\" = ?"
          + " THEN 1 ELSE 0 END), 0)"
          + " FROM (SELECT \"trangThaiXuLyPhieu\", \"ngayHoanTat\", \"trangThaiDuyet\", \"ngayDuyet\""
          + " FROM \"PhieuTra\" UNION ALL SELECT \"trangThaiXuLyPhieu\", \"ngayHoanTat\","
          + " \"trangThaiDuyet\", \"ngayDuyet\" FROM \"PhieuDoi\") p"
          + " WHERE p.\"ngayHoanTat\" = ? OR p.\"ngayDuyet\" = ?";

  private static final String SAO_LUU_TRONG_NGAY =
      "SELECT \"createdAt\", \"loai\", \"trangThai\" FROM \"LichSuSaoLuu\""
          + " WHERE \"createdAt\" >= ? AND \"createdAt\" < ? ORDER BY \"createdAt\" DESC";

  private static final String MAY_TON_LAU =
      "SELECT COUNT(*) FROM \"SanPham\""
          + " WHERE \"trangThai\" IN ('SAN_SANG_BAN', 'HANG_LOI', 'DANG_BAO_HANH')"
          + " AND ?::DATE - \"ngayNhapKhoBanDau\" >= ?";

  private final DataSource dataSource;

  public TrangChuDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public TongQuanBanHang tongQuanBanHang(LocalDate ngay, String maNV) {
    Objects.requireNonNull(ngay, "ngay");
    try (Connection c = dataSource.getConnection();
        PreparedStatement st = c.prepareStatement(TONG_QUAN_BAN_HANG)) {
      st.setObject(1, ngay);
      st.setString(2, maNV);
      st.setString(3, maNV);
      try (ResultSet rs = st.executeQuery()) {
        rs.next();
        return new TongQuanBanHang(
            rs.getLong(1), rs.getBigDecimal(2).longValueExact(),
            rs.getBigDecimal(3).longValueExact(), rs.getLong(4));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải tổng quan bán hàng", e);
    }
  }

  @Override
  public List<HoaDonGanDay> hoaDonGanDay(LocalDate ngay, String maNV, int gioiHan) {
    Objects.requireNonNull(ngay, "ngay");
    if (gioiHan <= 0) {
      throw new IllegalArgumentException("Giới hạn phải lớn hơn 0");
    }
    try (Connection c = dataSource.getConnection();
        PreparedStatement st = c.prepareStatement(HOA_DON_GAN_DAY)) {
      st.setObject(1, ngay);
      st.setString(2, maNV);
      st.setString(3, maNV);
      st.setInt(4, gioiHan);
      try (ResultSet rs = st.executeQuery()) {
        List<HoaDonGanDay> ketQua = new ArrayList<>();
        while (rs.next()) {
          ketQua.add(new HoaDonGanDay(
              rs.getString(1), rs.getString(2), rs.getBigDecimal(4).longValueExact(),
              rs.getBoolean(3)));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải hóa đơn trong ngày", e);
    }
  }

  @Override
  public DoiTraTrongNgay doiTraTrongNgay(LocalDate ngay) {
    Objects.requireNonNull(ngay, "ngay");
    try (Connection c = dataSource.getConnection();
        PreparedStatement st = c.prepareStatement(DOI_TRA_TRONG_NGAY)) {
      for (int i = 1; i <= 5; i++) {
        st.setObject(i, ngay);
      }
      try (ResultSet rs = st.executeQuery()) {
        rs.next();
        return new DoiTraTrongNgay(rs.getLong(1), rs.getLong(2), rs.getLong(3));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải phiếu đổi/trả trong ngày", e);
    }
  }

  @Override
  public List<SaoLuuTrongNgay> saoLuuTrongNgay(LocalDate ngay) {
    Objects.requireNonNull(ngay, "ngay");
    try (Connection c = dataSource.getConnection();
        PreparedStatement st = c.prepareStatement(SAO_LUU_TRONG_NGAY)) {
      st.setObject(1, ngay.atStartOfDay());
      st.setObject(2, ngay.plusDays(1).atStartOfDay());
      try (ResultSet rs = st.executeQuery()) {
        List<SaoLuuTrongNgay> ketQua = new ArrayList<>();
        while (rs.next()) {
          ketQua.add(new SaoLuuTrongNgay(
              rs.getObject(1, LocalDateTime.class), rs.getString(2),
              "SUCCESS".equals(rs.getString(3))));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải lịch sử sao lưu trong ngày", e);
    }
  }

  @Override
  public long soMayTonLau(LocalDate ngay, int nguongNgay) {
    Objects.requireNonNull(ngay, "ngay");
    try (Connection c = dataSource.getConnection();
        PreparedStatement st = c.prepareStatement(MAY_TON_LAU)) {
      st.setObject(1, ngay);
      st.setInt(2, nguongNgay);
      try (ResultSet rs = st.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể đếm máy tồn kho lâu ngày", e);
    }
  }
}
