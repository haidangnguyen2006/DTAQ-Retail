package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.entity.TrangThaiDuyet;
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
import javax.sql.DataSource;

/**
 * Triển khai JDBC của {@link PhieuTraDao} cho nghiệp vụ duyệt yêu cầu trả hàng (UC018).
 */
public class PhieuTraDaoImpl implements PhieuTraDao {

  private static final String SELECT_CHO_DUYET =
      "SELECT pt.\"maPhieuTra\" AS maPhieu, pt.\"hoaDon\" AS maHDGoc, kh.\"hoTen\" AS tenKhachHang,"
          + " kh.\"soDienThoai\" AS soDienThoai, pt.\"tienKhachDua\" AS hoanDuKien,"
          + " nv.\"hoTen\" AS tenNhanVien, pt.\"ngayTao\" AS ngayTao,"
          + " (SELECT string_agg(ct.\"lyDoTra\", '; ') FROM \"ChiTietPhieuTra\" ct"
          + "  WHERE ct.\"maPhieuTra\" = pt.\"maPhieuTra\") AS lyDo"
          + " FROM \"PhieuTra\" pt"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pt.\"hoaDon\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = pt.\"nhanVienLap\""
          + " WHERE pt.\"trangThaiDuyet\" = 'CHO_DUYET'"
          + " ORDER BY pt.\"ngayTao\", pt.\"maPhieuTra\"";

  private static final String UPDATE_QUYET_DINH =
      "UPDATE \"PhieuTra\" SET \"trangThaiDuyet\" = ?, \"nhanVienDuyet\" = ?, \"ngayDuyet\" = ?,"
          + " \"lyDoDuyet\" = ?"
          + " WHERE \"maPhieuTra\" = ? AND \"trangThaiDuyet\" = 'CHO_DUYET'";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung.
   *
   * @param dataSource nguồn dữ liệu kết nối CSDL; không được {@code null}
   */
  public PhieuTraDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public List<PhieuDuyet> findChoDuyet() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_CHO_DUYET);
        ResultSet resultSet = statement.executeQuery()) {
      List<PhieuDuyet> danhSach = new ArrayList<>();
      while (resultSet.next()) {
        danhSach.add(mapPhieuDuyet(resultSet));
      }
      return danhSach;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách phiếu trả chờ duyệt", e);
    }
  }

  @Override
  public boolean capNhatQuyetDinh(
      String maPhieu,
      TrangThaiDuyet quyetDinh,
      String maNhanVienDuyet,
      LocalDate ngayDuyet,
      String yKien) {
    Objects.requireNonNull(quyetDinh, "quyetDinh");
    Objects.requireNonNull(maNhanVienDuyet, "maNhanVienDuyet");
    Objects.requireNonNull(ngayDuyet, "ngayDuyet");
    if (maPhieu == null || maPhieu.isBlank()) {
      return false;
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_QUYET_DINH)) {
      statement.setString(1, quyetDinh.name());
      statement.setString(2, maNhanVienDuyet);
      statement.setDate(3, Date.valueOf(ngayDuyet));
      statement.setString(4, yKien);
      statement.setString(5, maPhieu.trim());
      return statement.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể ghi quyết định duyệt phiếu trả: " + maPhieu, e);
    }
  }

  private PhieuDuyet mapPhieuDuyet(ResultSet resultSet) throws SQLException {
    BigDecimal hoanDuKien = resultSet.getBigDecimal("hoanDuKien");
    Date ngayTao = resultSet.getDate("ngayTao");
    return new PhieuDuyet(
        PhieuDuyet.LoaiPhieu.PHIEU_TRA,
        resultSet.getString("maPhieu"),
        resultSet.getString("maHDGoc"),
        resultSet.getString("tenKhachHang"),
        resultSet.getString("soDienThoai"),
        hoanDuKien == null ? BigDecimal.ZERO : hoanDuKien,
        resultSet.getString("tenNhanVien"),
        resultSet.getString("lyDo"),
        ngayTao == null ? null : ngayTao.toLocalDate());
  }
}
