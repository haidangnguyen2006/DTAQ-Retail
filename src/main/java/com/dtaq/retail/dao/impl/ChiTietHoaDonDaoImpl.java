package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/** Triển khai JDBC của {@link ChiTietHoaDonDao} trên bảng {@code "ChiTietHoaDon"}. */
public final class ChiTietHoaDonDaoImpl implements ChiTietHoaDonDao {

  private static final String SELECT_BY_HOA_DON =
      "SELECT ct.\"maHD\" AS ct_maHD,"
          + " ct.\"soSerial\" AS ct_soSerial,"
          + " ct.\"maDongSP\" AS ct_maDongSP,"
          + " ct.\"soLuong\" AS ct_soLuong,"
          + " ct.\"donGiaBanTaiLucBan\" AS ct_donGiaBanTaiLucBan,"
          + " ct.\"thueSuatTaiLucBan\" AS ct_thueSuatTaiLucBan,"
          + " ct.\"maKM\" AS ct_maKM,"
          + " ct.\"tyLeGiamTaiLucBan\" AS ct_tyLeGiamTaiLucBan,"
          + " ct.\"tienKhuyenMaiThucTe\" AS ct_tienKhuyenMaiThucTe,"
          + " ct.\"tienTruocKhuyenMai\" AS ct_tienTruocKhuyenMai,"
          + " ct.\"tienSauKhuyenMai\" AS ct_tienSauKhuyenMai,"
          + " ct.\"tienThue\" AS ct_tienThue,"
          + " hd.\"maHD\" AS hd_maHD,"
          + " hd.\"ngayLap\" AS hd_ngayLap,"
          + " hd.\"trangThaiThanhToan\" AS hd_trangThaiThanhToan,"
          + " nv.\"maNV\" AS nv_maNV,"
          + " nv.\"hoTen\" AS nv_hoTen,"
          + " nv.\"soDienThoai\" AS nv_soDienThoai,"
          + " nv.\"email\" AS nv_email,"
          + " nv.\"ngayVaoLam\" AS nv_ngayVaoLam,"
          + " nv.\"tenDangNhap\" AS nv_tenDangNhap,"
          + " nv.\"matKhau\" AS nv_matKhau,"
          + " nv.\"loaiQuyen\" AS nv_loaiQuyen,"
          + " nv.\"trangThaiHoatDong\" AS nv_trangThaiHoatDong,"
          + " kh.\"maKH\" AS kh_maKH,"
          + " kh.\"hoTen\" AS kh_hoTen,"
          + " kh.\"soDienThoai\" AS kh_soDienThoai,"
          + " kh.\"diaChi\" AS kh_diaChi,"
          + " sp.\"soSerial\" AS sp_soSerial,"
          + " sp.\"maNCC\" AS sp_maNCC,"
          + " sp.\"maDongSP\" AS sp_maDongSP,"
          + " sp.\"giaNhap\" AS sp_giaNhap,"
          + " sp.\"ngayNhapKhoBanDau\" AS sp_ngayNhapKhoBanDau,"
          + " sp.\"trangThai\" AS sp_trangThai,"
          + " sp.\"soLanDoiTra\" AS sp_soLanDoiTra,"
          + " sp.\"soLanSua\" AS sp_soLanSua,"
          + " ncc.\"maNCC\" AS ncc_maNCC,"
          + " ncc.\"tenNCC\" AS ncc_tenNCC,"
          + " dsp.\"maDongSP\" AS dsp_maDongSP,"
          + " dsp.\"tenDongSP\" AS dsp_tenDongSP,"
          + " dsp.\"maDanhMuc\" AS dsp_maDanhMuc,"
          + " dsp.\"donViTinh\" AS dsp_donViTinh,"
          + " dsp.\"soLuongTon\" AS dsp_soLuongTon,"
          + " dsp.\"thue\" AS dsp_thue,"
          + " dsp.\"donGiaBanTruocThue\" AS dsp_donGiaBanTruocThue,"
          + " dsp.\"trangThaiKinhDoanh\" AS dsp_trangThaiKinhDoanh,"
          + " dsp.\"ngayNhapKho\" AS dsp_ngayNhapKho,"
          + " dsp.\"ngayCapNhapCuoi\" AS dsp_ngayCapNhapCuoi,"
          + " dm.\"maDanhMuc\" AS dm_maDanhMuc,"
          + " dm.\"tenDanhMuc\" AS dm_tenDanhMuc,"
          + " dm.\"moTa\" AS dm_moTa,"
          + " km.\"maKM\" AS km_maKM,"
          + " km.\"tenKM\" AS km_tenKM,"
          + " km.\"ngayBatDau\" AS km_ngayBatDau,"
          + " km.\"ngayKetThuc\" AS km_ngayKetThuc,"
          + " km.\"tyLeGiam\" AS km_tyLeGiam,"
          + " km.\"moTa\" AS km_moTa,"
          + " km.\"dangApDung\" AS km_dangApDung,"
          + " km.\"maDongSP\" AS km_maDongSP"
          + " FROM \"ChiTietHoaDon\" ct"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = ct.\"maHD\""
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = hd.\"nhanVienLap\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"SanPham\" sp"
          + " ON sp.\"soSerial\" = ct.\"soSerial\" AND sp.\"maDongSP\" = ct.\"maDongSP\""
          + " JOIN \"NhaCungCap\" ncc ON ncc.\"maNCC\" = sp.\"maNCC\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = sp.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\""
          + " LEFT JOIN \"KhuyenMai\" km ON km.\"maKM\" = ct.\"maKM\""
          + " WHERE ct.\"maHD\" = ?"
          + " ORDER BY ct.\"soSerial\"";

  private static final String INSERT_CHI_TIET =
      "INSERT INTO \"ChiTietHoaDon\" (\"maHD\", \"soSerial\", \"maDongSP\", \"soLuong\","
          + " \"donGiaBanTaiLucBan\", \"thueSuatTaiLucBan\", \"maKM\", \"tyLeGiamTaiLucBan\","
          + " \"tienKhuyenMaiThucTe\", \"tienTruocKhuyenMai\", \"tienSauKhuyenMai\", \"tienThue\")"
          + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   */
  public ChiTietHoaDonDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void insert(Connection ketNoi, ChiTietHoaDon chiTiet) {
    if (chiTiet == null) {
      throw new IllegalArgumentException("Chi tiết hóa đơn không được để trống");
    }
    if (chiTiet.soLuong() != 1) {
      throw new IllegalArgumentException("Số lượng mỗi dòng chi tiết phải bằng 1");
    }
    if (chiTiet.hoaDon() == null || chiTiet.sanPham() == null) {
      throw new IllegalArgumentException("Chi tiết hóa đơn phải có hóa đơn và sản phẩm");
    }
    Objects.requireNonNull(ketNoi, "ketNoi");
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_CHI_TIET)) {
      statement.setString(1, chiTiet.hoaDon().maHD());
      statement.setString(2, chiTiet.sanPham().soSerial());
      statement.setString(3, chiTiet.sanPham().dongSanPham().maDongSP());
      statement.setInt(4, chiTiet.soLuong());
      statement.setBigDecimal(5, chiTiet.donGiaBanTaiLucBan());
      statement.setBigDecimal(6, chiTiet.thueSuatTaiLucBan());
      if (chiTiet.khuyenMai() == null) {
        statement.setNull(7, Types.VARCHAR);
      } else {
        statement.setString(7, chiTiet.khuyenMai().maKM());
      }
      if (chiTiet.tyLeGiamTaiLucBan() == null) {
        statement.setNull(8, Types.NUMERIC);
      } else {
        statement.setBigDecimal(8, chiTiet.tyLeGiamTaiLucBan());
      }
      statement.setBigDecimal(9, chiTiet.tienKhuyenMaiThucTe());
      statement.setBigDecimal(10, chiTiet.tienTruocKhuyenMai());
      statement.setBigDecimal(11, chiTiet.tienSauKhuyenMai());
      statement.setBigDecimal(12, chiTiet.tienThue());
      int affected = statement.executeUpdate();
      if (affected != 1) {
        throw new DataAccessException("Không thể ghi dòng chi tiết hóa đơn");
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể ghi dòng chi tiết hóa đơn", e);
    }
  }

  @Override
  public List<ChiTietHoaDon> findByHoaDon(String maHD) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_HOA_DON)) {
      statement.setString(1, maHD);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ChiTietHoaDon> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapChiTietHoaDon(resultSet));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu chi tiết hóa đơn", e);
    }
  }

  private ChiTietHoaDon mapChiTietHoaDon(ResultSet resultSet) throws SQLException {
    NhanVien nhanVien = new NhanVien(
        resultSet.getString("nv_maNV"),
        resultSet.getString("nv_hoTen"),
        resultSet.getString("nv_soDienThoai"),
        resultSet.getString("nv_email"),
        resultSet.getObject("nv_ngayVaoLam", LocalDate.class),
        resultSet.getString("nv_tenDangNhap"),
        resultSet.getString("nv_matKhau"),
        LoaiQuyen.valueOf(resultSet.getString("nv_loaiQuyen")),
        resultSet.getBoolean("nv_trangThaiHoatDong"));

    KhachHang khachHang = new KhachHang(
        resultSet.getString("kh_maKH"),
        resultSet.getString("kh_hoTen"),
        resultSet.getString("kh_soDienThoai"),
        resultSet.getString("kh_diaChi"));

    HoaDon hoaDon = new HoaDon(
        resultSet.getString("hd_maHD"),
        resultSet.getObject("hd_ngayLap", LocalDate.class),
        nhanVien,
        khachHang,
        resultSet.getBoolean("hd_trangThaiThanhToan"));

    DanhMucSanPham danhMucSanPham = new DanhMucSanPham(
        resultSet.getString("dm_maDanhMuc"),
        resultSet.getString("dm_tenDanhMuc"),
        resultSet.getString("dm_moTa"));

    DongSanPham dongSanPham = new DongSanPham(
        resultSet.getString("dsp_maDongSP"),
        resultSet.getString("dsp_tenDongSP"),
        danhMucSanPham,
        resultSet.getString("dsp_donViTinh"),
        resultSet.getInt("dsp_soLuongTon"),
        resultSet.getBigDecimal("dsp_thue"),
        resultSet.getBigDecimal("dsp_donGiaBanTruocThue"),
        resultSet.getBoolean("dsp_trangThaiKinhDoanh"),
        resultSet.getObject("dsp_ngayNhapKho", LocalDate.class),
        resultSet.getObject("dsp_ngayCapNhapCuoi", LocalDate.class));

    NhaCungCap nhaCungCap = new NhaCungCap(
        resultSet.getString("ncc_maNCC"),
        resultSet.getString("ncc_tenNCC"));

    SanPham sanPham = new SanPham(
        resultSet.getString("sp_soSerial"),
        nhaCungCap,
        dongSanPham,
        resultSet.getBigDecimal("sp_giaNhap"),
        resultSet.getObject("sp_ngayNhapKhoBanDau", LocalDate.class),
        TrangThaiSanPham.valueOf(resultSet.getString("sp_trangThai")),
        resultSet.getInt("sp_soLanDoiTra"),
        resultSet.getInt("sp_soLanSua"));

    KhuyenMai khuyenMai = null;
    String maKM = resultSet.getString("ct_maKM");
    if (maKM != null) {
      khuyenMai = new KhuyenMai(
          resultSet.getString("km_maKM"),
          resultSet.getString("km_tenKM"),
          resultSet.getObject("km_ngayBatDau", LocalDate.class),
          resultSet.getObject("km_ngayKetThuc", LocalDate.class),
          resultSet.getBigDecimal("km_tyLeGiam"),
          resultSet.getString("km_moTa"),
          resultSet.getBoolean("km_dangApDung"),
          dongSanPham);
    }

    return new ChiTietHoaDon(
        hoaDon,
        sanPham,
        resultSet.getInt("ct_soLuong"),
        resultSet.getBigDecimal("ct_donGiaBanTaiLucBan"),
        resultSet.getBigDecimal("ct_thueSuatTaiLucBan"),
        khuyenMai,
        resultSet.getBigDecimal("ct_tyLeGiamTaiLucBan"),
        resultSet.getBigDecimal("ct_tienKhuyenMaiThucTe"),
        resultSet.getBigDecimal("ct_tienTruocKhuyenMai"),
        resultSet.getBigDecimal("ct_tienSauKhuyenMai"),
        resultSet.getBigDecimal("ct_tienThue"));
  }
}
