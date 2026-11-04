package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuDoiDao;
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
 * Triển khai JDBC của {@link PhieuDoiDao} cho nghiệp vụ duyệt yêu cầu đổi hàng (UC018).
 */
public class PhieuDoiDaoImpl implements PhieuDoiDao {

  private static final String SELECT_CHO_DUYET =
      "SELECT pd.\"maPhieuDoi\" AS maPhieu, pd.\"hoaDon\" AS maHDGoc, kh.\"hoTen\" AS tenKhachHang,"
          + " kh.\"soDienThoai\" AS soDienThoai,"
          + " nv.\"hoTen\" AS tenNhanVien, pd.\"ngayTao\" AS ngayTao,"
          + " (SELECT string_agg(ct.\"lyDoDoi\", '; ') FROM \"ChiTietPhieuDoi\" ct"
          + "  WHERE ct.\"maPhieuDoi\" = pd.\"maPhieuDoi\") AS lyDo"
          + " FROM \"PhieuDoi\" pd"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pd.\"hoaDon\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = pd.\"nhanVienLap\""
          + " WHERE pd.\"trangThaiDuyet\" = 'CHO_DUYET'"
          + " ORDER BY pd.\"ngayTao\", pd.\"maPhieuDoi\"";

  private static final String UPDATE_QUYET_DINH =
      "UPDATE \"PhieuDoi\" SET \"trangThaiDuyet\" = ?, \"nhanVienDuyet\" = ?, \"ngayDuyet\" = ?,"
          + " \"lyDoDuyet\" = ?"
          + " WHERE \"maPhieuDoi\" = ? AND \"trangThaiDuyet\" = 'CHO_DUYET'";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung.
   *
   * @param dataSource nguồn dữ liệu kết nối CSDL; không được {@code null}
   */
  public PhieuDoiDaoImpl(DataSource dataSource) {
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
      throw new DataAccessException("Không thể tải danh sách phiếu đổi chờ duyệt", e);
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
      throw new DataAccessException("Không thể ghi quyết định duyệt phiếu đổi: " + maPhieu, e);
    }
  }

  private PhieuDuyet mapPhieuDuyet(ResultSet resultSet) throws SQLException {
    Date ngayTao = resultSet.getDate("ngayTao");
    // Bảng "PhieuDoi" không lưu số tiền hoàn; theo quyết định đã duyệt, hoàn dự kiến của phiếu
    // đổi hàng luôn bằng 0.
    return new PhieuDuyet(
        PhieuDuyet.LoaiPhieu.PHIEU_DOI,
        resultSet.getString("maPhieu"),
        resultSet.getString("maHDGoc"),
        resultSet.getString("tenKhachHang"),
        resultSet.getString("soDienThoai"),
        BigDecimal.ZERO,
        resultSet.getString("tenNhanVien"),
        resultSet.getString("lyDo"),
        ngayTao == null ? null : ngayTao.toLocalDate());
  }
}
