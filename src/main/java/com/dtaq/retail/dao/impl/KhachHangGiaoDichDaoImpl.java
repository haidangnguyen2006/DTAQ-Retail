package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.KhachHangGiaoDichDao;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
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

/** Triển khai JDBC chỉ đọc của {@link KhachHangGiaoDichDao}. */
public final class KhachHangGiaoDichDaoImpl implements KhachHangGiaoDichDao {

  /** Tổng tiền cộng từ các dòng chi tiết đã làm tròn sẵn khi bán (C-11), không làm tròn lại. */
  private static final String SELECT_HOA_DON_CUA_KHACH =
      "SELECT hd.\"maHD\", hd.\"ngayLap\", hd.\"trangThaiThanhToan\", nv.\"hoTen\" AS \"tenNhanVien\","
          + " COUNT(ct.\"soSerial\") AS \"soSanPham\","
          + " COALESCE(SUM(ct.\"tienSauKhuyenMai\"), 0) AS \"tienHang\","
          + " COALESCE(SUM(ct.\"tienThue\"), 0) AS \"tienThue\""
          + " FROM \"HoaDon\" hd"
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = hd.\"nhanVienLap\""
          + " LEFT JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = hd.\"maHD\""
          + " WHERE hd.\"khachHang\" = ?"
          + " GROUP BY hd.\"maHD\", hd.\"ngayLap\", hd.\"trangThaiThanhToan\", nv.\"hoTen\""
          + " ORDER BY hd.\"ngayLap\" DESC, hd.\"maHD\" DESC";

  private static final String SELECT_CHI_TIET_HOA_DON =
      "SELECT ct.\"soSerial\", ct.\"maDongSP\", dsp.\"tenDongSP\", ct.\"donGiaBanTaiLucBan\","
          + " ct.\"thueSuatTaiLucBan\", ct.\"tienKhuyenMaiThucTe\", ct.\"tienSauKhuyenMai\", ct.\"tienThue\""
          + " FROM \"ChiTietHoaDon\" ct"
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = ct.\"maDongSP\""
          + " WHERE ct.\"maHD\" = ?"
          + " ORDER BY ct.\"soSerial\"";

  private static final String SELECT_PHIEU_CUA_KHACH =
      "SELECT 'TRA' AS \"loai\", pt.\"maPhieuTra\" AS \"maPhieu\", pt.\"hoaDon\", pt.\"ngayTao\","
          + " pt.\"ngayHoanTat\", pt.\"trangThaiDuyet\", pt.\"trangThaiXuLyPhieu\", pt.\"lyDoDuyet\""
          + " FROM \"PhieuTra\" pt JOIN \"HoaDon\" hd ON hd.\"maHD\" = pt.\"hoaDon\""
          + " WHERE hd.\"khachHang\" = ?"
          + " UNION ALL"
          + " SELECT 'DOI', pd.\"maPhieuDoi\", pd.\"hoaDon\", pd.\"ngayTao\","
          + " pd.\"ngayHoanTat\", pd.\"trangThaiDuyet\", pd.\"trangThaiXuLyPhieu\", pd.\"lyDoDuyet\""
          + " FROM \"PhieuDoi\" pd JOIN \"HoaDon\" hd ON hd.\"maHD\" = pd.\"hoaDon\""
          + " WHERE hd.\"khachHang\" = ?"
          + " ORDER BY \"ngayTao\" DESC, \"maPhieu\" DESC";

  private static final String SELECT_CHI_TIET_PHIEU_TRA =
      "SELECT c.\"soSerial\", dsp.\"tenDongSP\", c.\"lyDoTra\" AS \"lyDo\", c.\"tinhTrangHangTra\","
          + " NULL AS \"serialThayThe\""
          + " FROM \"ChiTietPhieuTra\" c"
          + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = c.\"maHD\" AND ct.\"soSerial\" = c.\"soSerial\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = ct.\"maDongSP\""
          + " WHERE c.\"maPhieuTra\" = ?"
          + " ORDER BY c.\"soSerial\"";

  private static final String SELECT_CHI_TIET_PHIEU_DOI =
      "SELECT c.\"soSerial\", dsp.\"tenDongSP\", c.\"lyDoDoi\" AS \"lyDo\", NULL AS \"tinhTrangHangTra\","
          + " c.\"chiTietSanPhamDoi\" AS \"serialThayThe\""
          + " FROM \"ChiTietPhieuDoi\" c"
          + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = c.\"maHD\" AND ct.\"soSerial\" = c.\"soSerial\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = ct.\"maDongSP\""
          + " WHERE c.\"maPhieuDoi\" = ?"
          + " ORDER BY c.\"soSerial\"";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public KhachHangGiaoDichDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public List<HoaDonTomTat> hoaDonCuaKhach(String maKH) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_HOA_DON_CUA_KHACH)) {
      statement.setString(1, maKH);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<HoaDonTomTat> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new HoaDonTomTat(
              resultSet.getString("maHD"),
              resultSet.getObject("ngayLap", LocalDate.class),
              resultSet.getString("tenNhanVien"),
              resultSet.getInt("soSanPham"),
              resultSet.getBigDecimal("tienHang"),
              resultSet.getBigDecimal("tienThue"),
              resultSet.getBigDecimal("tienHang").add(resultSet.getBigDecimal("tienThue")),
              resultSet.getBoolean("trangThaiThanhToan")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải lịch sử hóa đơn của khách hàng", e);
    }
  }

  @Override
  public List<ChiTietHoaDonView> chiTietHoaDon(String maHD) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_CHI_TIET_HOA_DON)) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ChiTietHoaDonView> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new ChiTietHoaDonView(
              resultSet.getString("soSerial"),
              resultSet.getString("maDongSP"),
              resultSet.getString("tenDongSP"),
              resultSet.getBigDecimal("donGiaBanTaiLucBan"),
              resultSet.getBigDecimal("thueSuatTaiLucBan"),
              resultSet.getBigDecimal("tienKhuyenMaiThucTe"),
              resultSet.getBigDecimal("tienSauKhuyenMai"),
              resultSet.getBigDecimal("tienThue"),
              resultSet.getBigDecimal("tienSauKhuyenMai").add(resultSet.getBigDecimal("tienThue"))));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải chi tiết hóa đơn", e);
    }
  }

  @Override
  public List<PhieuTomTat> phieuCuaKhach(String maKH) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_PHIEU_CUA_KHACH)) {
      statement.setString(1, maKH);
      statement.setString(2, maKH);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<PhieuTomTat> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(new PhieuTomTat(
              LoaiPhieu.valueOf(resultSet.getString("loai")),
              resultSet.getString("maPhieu"),
              resultSet.getString("hoaDon"),
              resultSet.getObject("ngayTao", LocalDate.class),
              resultSet.getObject("ngayHoanTat", LocalDate.class),
              TrangThaiDuyet.valueOf(resultSet.getString("trangThaiDuyet")),
              TrangThaiXuLyPhieu.valueOf(resultSet.getString("trangThaiXuLyPhieu")),
              resultSet.getString("lyDoDuyet")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải phiếu đổi/trả của khách hàng", e);
    }
  }

  @Override
  public List<ChiTietPhieuView> chiTietPhieu(LoaiPhieu loai, String maPhieu) {
    Objects.requireNonNull(loai, "loai");
    String sql = loai == LoaiPhieu.TRA ? SELECT_CHI_TIET_PHIEU_TRA : SELECT_CHI_TIET_PHIEU_DOI;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, maPhieu);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ChiTietPhieuView> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          String tinhTrang = resultSet.getString("tinhTrangHangTra");
          ketQua.add(new ChiTietPhieuView(
              resultSet.getString("soSerial"),
              resultSet.getString("tenDongSP"),
              resultSet.getString("lyDo"),
              tinhTrang == null ? null : TinhTrangHangTra.valueOf(tinhTrang),
              resultSet.getString("serialThayThe")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải chi tiết phiếu", e);
    }
  }
}
