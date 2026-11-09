package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.MaGenerator;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;

/**
 * Triển khai JDBC của {@link PhieuTraDao} cho nghiệp vụ lập/hoàn tất phiếu trả (UC006) và duyệt
 * yêu cầu trả hàng (UC018).
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

  private static final String SELECT_CHO_HOAN_TAT =
      "SELECT pt.\"maPhieuTra\" AS maPhieu, pt.\"hoaDon\" AS maHDGoc, kh.\"hoTen\" AS tenKhachHang,"
          + " kh.\"soDienThoai\" AS soDienThoai, pt.\"tienKhachDua\" AS hoanDuKien,"
          + " nv.\"hoTen\" AS tenNhanVien, pt.\"ngayTao\" AS ngayTao,"
          + " (SELECT string_agg(ct.\"lyDoTra\", '; ') FROM \"ChiTietPhieuTra\" ct"
          + "  WHERE ct.\"maPhieuTra\" = pt.\"maPhieuTra\") AS lyDo"
          + " FROM \"PhieuTra\" pt"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pt.\"hoaDon\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = pt.\"nhanVienLap\""
          + " WHERE pt.\"trangThaiDuyet\" = 'DA_DUYET'"
          + " AND pt.\"trangThaiXuLyPhieu\" = 'CHO_XU_LY'"
          + " ORDER BY pt.\"ngayTao\", pt.\"maPhieuTra\"";

  private static final String SELECT_BY_ID =
      "SELECT pt.\"maPhieuTra\" AS maPhieuTra, pt.\"ngayTao\" AS ngayTao,"
          + " pt.\"ngayHoanTat\" AS ngayHoanTat, pt.\"trangThaiDuyet\" AS trangThaiDuyet,"
          + " pt.\"trangThaiXuLyPhieu\" AS trangThaiXuLyPhieu, pt.\"lyDoDuyet\" AS lyDoDuyet,"
          + " pt.\"ngayDuyet\" AS ngayDuyet,"
          + " pt.\"tienKhachDua\" AS tienKhachDua,"
          + " lap.\"maNV\" AS lap_maNV, lap.\"hoTen\" AS lap_hoTen,"
          + " lap.\"soDienThoai\" AS lap_soDienThoai, lap.\"email\" AS lap_email,"
          + " lap.\"ngayVaoLam\" AS lap_ngayVaoLam,"
          + " lap.\"tenDangNhap\" AS lap_tenDangNhap, lap.\"loaiQuyen\" AS lap_loaiQuyen,"
          + " lap.\"trangThaiHoatDong\" AS lap_trangThaiHoatDong,"
          + " duyet.\"maNV\" AS duyet_maNV, duyet.\"hoTen\" AS duyet_hoTen,"
          + " duyet.\"soDienThoai\" AS duyet_soDienThoai, duyet.\"email\" AS duyet_email,"
          + " duyet.\"ngayVaoLam\" AS duyet_ngayVaoLam,"
          + " duyet.\"tenDangNhap\" AS duyet_tenDangNhap,"
          + " duyet.\"loaiQuyen\" AS duyet_loaiQuyen,"
          + " duyet.\"trangThaiHoatDong\" AS duyet_trangThaiHoatDong,"
          + " hd.\"maHD\" AS hd_maHD, hd.\"ngayLap\" AS hd_ngayLap,"
          + " hd.\"trangThaiThanhToan\" AS hd_trangThaiThanhToan,"
          + " hdnv.\"maNV\" AS hdnv_maNV, hdnv.\"hoTen\" AS hdnv_hoTen,"
          + " hdnv.\"soDienThoai\" AS hdnv_soDienThoai, hdnv.\"email\" AS hdnv_email,"
          + " hdnv.\"ngayVaoLam\" AS hdnv_ngayVaoLam,"
          + " hdnv.\"tenDangNhap\" AS hdnv_tenDangNhap,"
          + " hdnv.\"loaiQuyen\" AS hdnv_loaiQuyen,"
          + " hdnv.\"trangThaiHoatDong\" AS hdnv_trangThaiHoatDong,"
          + " kh.\"maKH\" AS kh_maKH, kh.\"hoTen\" AS kh_hoTen,"
          + " kh.\"soDienThoai\" AS kh_soDienThoai, kh.\"diaChi\" AS kh_diaChi"
          + " FROM \"PhieuTra\" pt"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pt.\"hoaDon\""
          + " JOIN \"NhanVien\" lap ON lap.\"maNV\" = pt.\"nhanVienLap\""
          + " JOIN \"NhanVien\" hdnv ON hdnv.\"maNV\" = hd.\"nhanVienLap\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " LEFT JOIN \"NhanVien\" duyet ON duyet.\"maNV\" = pt.\"nhanVienDuyet\""
          + " WHERE pt.\"maPhieuTra\" = ?";

  private static final String SELECT_CHI_TIET =
      "SELECT ct.\"soSerial\" AS soSerial, ct.\"tinhTrangHangTra\" AS tinhTrangHangTra,"
          + " ct.\"lyDoTra\" AS lyDoTra,"
          + " cthd.\"soLuong\" AS cthd_soLuong,"
          + " cthd.\"donGiaBanTaiLucBan\" AS cthd_donGia,"
          + " cthd.\"thueSuatTaiLucBan\" AS cthd_thueSuat,"
          + " cthd.\"maKM\" AS cthd_maKM,"
          + " cthd.\"tyLeGiamTaiLucBan\" AS cthd_tyLeGiam,"
          + " cthd.\"tienKhuyenMaiThucTe\" AS cthd_tienKhuyenMai,"
          + " cthd.\"tienTruocKhuyenMai\" AS cthd_tienTruoc,"
          + " cthd.\"tienSauKhuyenMai\" AS cthd_tienSau,"
          + " cthd.\"tienThue\" AS cthd_tienThue,"
          + " sp.\"soSerial\" AS sp_soSerial, sp.\"maNCC\" AS sp_maNCC,"
          + " sp.\"maDongSP\" AS sp_maDongSP, sp.\"giaNhap\" AS sp_giaNhap,"
          + " sp.\"ngayNhapKhoBanDau\" AS sp_ngayNhapKhoBanDau,"
          + " sp.\"trangThai\" AS sp_trangThai, sp.\"soLanDoiTra\" AS sp_soLanDoiTra,"
          + " sp.\"soLanSua\" AS sp_soLanSua,"
          + " ncc.\"maNCC\" AS ncc_maNCC, ncc.\"tenNCC\" AS ncc_tenNCC,"
          + " dsp.\"maDongSP\" AS dsp_maDongSP, dsp.\"tenDongSP\" AS dsp_tenDongSP,"
          + " dsp.\"maDanhMuc\" AS dsp_maDanhMuc, dsp.\"donViTinh\" AS dsp_donViTinh,"
          + " dsp.\"soLuongTon\" AS dsp_soLuongTon, dsp.\"thue\" AS dsp_thue,"
          + " dsp.\"donGiaBanTruocThue\" AS dsp_donGiaBanTruocThue,"
          + " dsp.\"trangThaiKinhDoanh\" AS dsp_trangThaiKinhDoanh,"
          + " dsp.\"ngayNhapKho\" AS dsp_ngayNhapKho,"
          + " dsp.\"ngayCapNhapCuoi\" AS dsp_ngayCapNhapCuoi,"
          + " dm.\"maDanhMuc\" AS dm_maDanhMuc, dm.\"tenDanhMuc\" AS dm_tenDanhMuc,"
          + " dm.\"moTa\" AS dm_moTa,"
          + " km.\"maKM\" AS km_maKM, km.\"tenKM\" AS km_tenKM,"
          + " km.\"ngayBatDau\" AS km_ngayBatDau, km.\"ngayKetThuc\" AS km_ngayKetThuc,"
          + " km.\"tyLeGiam\" AS km_tyLeGiam, km.\"moTa\" AS km_moTa,"
          + " km.\"dangApDung\" AS km_dangApDung"
          + " FROM \"ChiTietPhieuTra\" ct"
          + " JOIN \"ChiTietHoaDon\" cthd"
          + " ON cthd.\"maHD\" = ct.\"maHD\" AND cthd.\"soSerial\" = ct.\"soSerial\""
          + " JOIN \"SanPham\" sp"
          + " ON sp.\"soSerial\" = cthd.\"soSerial\" AND sp.\"maDongSP\" = cthd.\"maDongSP\""
          + " JOIN \"NhaCungCap\" ncc ON ncc.\"maNCC\" = sp.\"maNCC\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = sp.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\""
          + " LEFT JOIN \"KhuyenMai\" km ON km.\"maKM\" = cthd.\"maKM\""
          + " WHERE ct.\"maPhieuTra\" = ?"
          + " ORDER BY ct.\"soSerial\"";

  private static final String SELECT_SERIAL_DA_DOI_TRA =
      "SELECT ct.\"soSerial\" FROM \"ChiTietPhieuTra\" ct"
          + " JOIN \"PhieuTra\" pt ON pt.\"maPhieuTra\" = ct.\"maPhieuTra\""
          + " WHERE ct.\"maHD\" = ? AND ct.\"soSerial\" IN (%s)"
          + " AND pt.\"trangThaiDuyet\" <> 'TU_CHOI'"
          + " AND pt.\"trangThaiXuLyPhieu\" <> 'HUY'"
          + " UNION"
          + " SELECT cd.\"soSerial\" FROM \"ChiTietPhieuDoi\" cd"
          + " JOIN \"PhieuDoi\" pd ON pd.\"maPhieuDoi\" = cd.\"maPhieuDoi\""
          + " WHERE cd.\"maHD\" = ? AND cd.\"soSerial\" IN (%s)"
          + " AND pd.\"trangThaiDuyet\" <> 'TU_CHOI'"
          + " AND pd.\"trangThaiXuLyPhieu\" <> 'HUY'";

  private static final String SELECT_PHIEU_HIEU_LUC =
      "SELECT 1 WHERE EXISTS ("
          + " SELECT 1 FROM \"ChiTietPhieuTra\" ct"
          + " JOIN \"PhieuTra\" pt ON pt.\"maPhieuTra\" = ct.\"maPhieuTra\""
          + " WHERE ct.\"maHD\" = ? AND ct.\"soSerial\" = ?"
          + " AND pt.\"trangThaiDuyet\" <> 'TU_CHOI'"
          + " AND pt.\"trangThaiXuLyPhieu\" <> 'HUY')"
          + " OR EXISTS ("
          + " SELECT 1 FROM \"ChiTietPhieuDoi\" cd"
          + " JOIN \"PhieuDoi\" pd ON pd.\"maPhieuDoi\" = cd.\"maPhieuDoi\""
          + " WHERE cd.\"maHD\" = ? AND cd.\"soSerial\" = ?"
          + " AND pd.\"trangThaiDuyet\" <> 'TU_CHOI'"
          + " AND pd.\"trangThaiXuLyPhieu\" <> 'HUY')";

  private static final String UPDATE_QUYET_DINH =
      "UPDATE \"PhieuTra\" SET \"trangThaiDuyet\" = ?, \"nhanVienDuyet\" = ?, \"ngayDuyet\" = ?,"
          + " \"lyDoDuyet\" = ?"
          + " WHERE \"maPhieuTra\" = ? AND \"trangThaiDuyet\" = 'CHO_DUYET'";

  private static final String INSERT_PHIEU_TRA =
      "INSERT INTO \"PhieuTra\" (\"maPhieuTra\", \"nhanVienLap\", \"hoaDon\", \"ngayTao\","
          + " \"ngayHoanTat\", \"trangThaiDuyet\", \"trangThaiXuLyPhieu\", \"nhanVienDuyet\","
          + " \"ngayDuyet\", \"lyDoDuyet\", \"tienKhachDua\")"
          + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

  private static final String INSERT_CHI_TIET =
      "INSERT INTO \"ChiTietPhieuTra\" (\"maPhieuTra\", \"maHD\", \"soSerial\","
          + " \"tinhTrangHangTra\", \"lyDoTra\")"
          + " VALUES (?,?,?,?,?)";

  private static final String UPDATE_MAY_NHAN_LAI =
      "UPDATE \"SanPham\" SET \"trangThai\" = ?, \"soLanDoiTra\" = \"soLanDoiTra\" + 1"
          + " WHERE \"soSerial\" = ? AND \"trangThai\" = 'DA_BAN'";

  private static final String UPDATE_HOAN_TAT =
      "UPDATE \"PhieuTra\" SET \"trangThaiXuLyPhieu\" = 'HOAN_TAT', \"ngayHoanTat\" = ?"
          + " WHERE \"maPhieuTra\" = ? AND \"trangThaiXuLyPhieu\" = 'CHO_XU_LY'";

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
    return findDanhSach(SELECT_CHO_DUYET, "Không thể tải danh sách phiếu trả chờ duyệt");
  }

  @Override
  public List<PhieuDuyet> findChoHoanTat() {
    return findDanhSach(SELECT_CHO_HOAN_TAT, "Không thể tải danh sách phiếu trả chờ hoàn tất");
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

  @Override
  public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
    if (maHD == null || maHD.isBlank() || soSerials == null || soSerials.isEmpty()) {
      return Set.of();
    }
    List<String> serials = soSerials.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(serial -> !serial.isEmpty())
        .distinct()
        .toList();
    if (serials.isEmpty()) {
      return Set.of();
    }

    String danhSachThamSo = String.join(",", Collections.nCopies(serials.size(), "?"));
    String sql = String.format(SELECT_SERIAL_DA_DOI_TRA, danhSachThamSo, danhSachThamSo);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      String maHDChuanHoa = maHD.trim();
      int chiSo = 1;
      statement.setString(chiSo++, maHDChuanHoa);
      for (String serial : serials) {
        statement.setString(chiSo++, serial);
      }
      statement.setString(chiSo++, maHDChuanHoa);
      for (String serial : serials) {
        statement.setString(chiSo++, serial);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        Set<String> ketQua = new LinkedHashSet<>();
        while (resultSet.next()) {
          ketQua.add(resultSet.getString(1));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra máy đã đổi/trả của hóa đơn " + maHD, e);
    }
  }

  @Override
  public Optional<PhieuTra> findById(String maPhieuTra) {
    if (maPhieuTra == null || maPhieuTra.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maPhieuTra.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapPhieuTra(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu phiếu trả " + maPhieuTra, e);
    }
  }

  @Override
  public List<ChiTietPhieuTra> findChiTiet(String maPhieuTra) {
    Optional<PhieuTra> dauPhieu = findById(maPhieuTra);
    if (dauPhieu.isEmpty()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_CHI_TIET)) {
      statement.setString(1, dauPhieu.get().maPhieuTra());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ChiTietPhieuTra> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapChiTietPhieuTra(resultSet, dauPhieu.get()));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải chi tiết phiếu trả " + maPhieuTra, e);
    }
  }

  @Override
  public String taoPhieuTra(
      Connection ketNoi, PhieuTra phieuTra, List<ChiTietPhieuTra> chiTiet) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(phieuTra, "phieuTra");
    Objects.requireNonNull(chiTiet, "chiTiet");
    if (phieuTra.nhanVienLap() == null) {
      throw new IllegalArgumentException("Phiếu trả phải có nhân viên lập");
    }
    if (phieuTra.hoaDon() == null) {
      throw new IllegalArgumentException("Phiếu trả phải có hóa đơn gốc");
    }
    if (phieuTra.ngayTao() == null) {
      throw new IllegalArgumentException("Phiếu trả phải có ngày tạo");
    }
    if (phieuTra.trangThaiDuyet() == null || phieuTra.trangThaiXuLyPhieu() == null) {
      throw new IllegalArgumentException("Phiếu trả phải có trạng thái duyệt và trạng thái xử lý");
    }
    if (phieuTra.tienKhachDua() == null) {
      throw new IllegalArgumentException("Phiếu trả phải có số tiền hoàn cho khách");
    }
    String maHDGoc = phieuTra.hoaDon().maHD();

    try {
      for (ChiTietPhieuTra dong : chiTiet) {
        // Guard nằm trong cùng transaction với INSERT để thu hẹp khe TOCTOU giữa hai yêu cầu.
        kiemTraChuaCoPhieuHieuLuc(ketNoi, maHDGoc, serialCua(dong));
      }
      String maPhieuTra = MaGenerator.next(ketNoi, "PhieuTra", "maPhieuTra", "PT");
      try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_PHIEU_TRA)) {
        statement.setString(1, maPhieuTra);
        statement.setString(2, phieuTra.nhanVienLap().maNV());
        statement.setString(3, maHDGoc);
        statement.setDate(4, Date.valueOf(phieuTra.ngayTao()));
        ganNgay(statement, 5, phieuTra.ngayHoanTat());
        statement.setString(6, phieuTra.trangThaiDuyet().name());
        statement.setString(7, phieuTra.trangThaiXuLyPhieu().name());
        if (phieuTra.nhanVienDuyet() == null) {
          statement.setNull(8, Types.VARCHAR);
        } else {
          statement.setString(8, phieuTra.nhanVienDuyet().maNV());
        }
        ganNgay(statement, 9, phieuTra.ngayDuyet());
        statement.setString(10, phieuTra.lyDoDuyet());
        statement.setBigDecimal(11, phieuTra.tienKhachDua());
        if (statement.executeUpdate() != 1) {
          throw new DataAccessException("Không thể ghi đầu phiếu trả " + maPhieuTra);
        }
      }
      for (ChiTietPhieuTra dong : chiTiet) {
        ghiChiTiet(ketNoi, maPhieuTra, maHDGoc, dong);
      }
      return maPhieuTra;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể ghi phiếu trả cho hóa đơn " + maHDGoc, e);
    }
  }

  @Override
  public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuTra> chiTiet) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(chiTiet, "chiTiet");
    int tongSoDong = 0;
    try (PreparedStatement statement = ketNoi.prepareStatement(UPDATE_MAY_NHAN_LAI)) {
      for (ChiTietPhieuTra dong : chiTiet) {
        if (dong == null || dong.tinhTrangHangTra() == null) {
          throw new IllegalArgumentException("Dòng phiếu trả phải có tình trạng hàng trả");
        }
        String soSerial = serialCua(dong);
        String trangThaiMoi = switch (dong.tinhTrangHangTra()) {
          case NGUYEN_VEN -> TrangThaiSanPham.SAN_SANG_BAN.name();
          case HANG_LOI -> TrangThaiSanPham.HANG_LOI.name();
        };
        statement.setString(1, trangThaiMoi);
        statement.setString(2, soSerial);
        int soDong = statement.executeUpdate();
        if (soDong != 1) {
          throw new IllegalStateException(
              "Máy " + soSerial + " không còn ở trạng thái đã bán");
        }
        tongSoDong += soDong;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật trạng thái máy nhận lại", e);
    }
    return tongSoDong;
  }

  @Override
  public boolean capNhatHoanTat(
      Connection ketNoi, String maPhieuTra, LocalDate ngayHoanTat) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(ngayHoanTat, "ngayHoanTat");
    if (maPhieuTra == null || maPhieuTra.isBlank()) {
      return false;
    }
    try (PreparedStatement statement = ketNoi.prepareStatement(UPDATE_HOAN_TAT)) {
      statement.setDate(1, Date.valueOf(ngayHoanTat));
      statement.setString(2, maPhieuTra.trim());
      return statement.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể hoàn tất phiếu trả " + maPhieuTra, e);
    }
  }

  private List<PhieuDuyet> findDanhSach(String sql, String thongDiepLoi) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      List<PhieuDuyet> danhSach = new ArrayList<>();
      while (resultSet.next()) {
        danhSach.add(mapPhieuDuyet(resultSet));
      }
      return danhSach;
    } catch (SQLException e) {
      throw new DataAccessException(thongDiepLoi, e);
    }
  }

  private void ghiChiTiet(
      Connection ketNoi, String maPhieuTra, String maHDGoc, ChiTietPhieuTra dong)
      throws SQLException {
    if (dong == null) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu trả không được để trống");
    }
    if (dong.tinhTrangHangTra() == null) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu trả phải có tình trạng hàng trả");
    }
    if (dong.lyDoTra() == null || dong.lyDoTra().isBlank()) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu trả phải có lý do trả");
    }
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_CHI_TIET)) {
      statement.setString(1, maPhieuTra);
      // RB24: mọi dòng chi tiết thuộc đúng hóa đơn gốc của đầu phiếu, không tin dữ liệu dòng.
      statement.setString(2, maHDGoc);
      statement.setString(3, serialCua(dong));
      statement.setString(4, dong.tinhTrangHangTra().name());
      statement.setString(5, dong.lyDoTra().trim());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException(
            "Không thể ghi dòng chi tiết phiếu trả " + maPhieuTra);
      }
    }
  }

  private static String serialCua(ChiTietPhieuTra dong) {
    if (dong.chiTietHoaDon() == null || dong.chiTietHoaDon().sanPham() == null) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu trả phải có máy được trả");
    }
    String soSerial = dong.chiTietHoaDon().sanPham().soSerial();
    if (soSerial == null || soSerial.isBlank()) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu trả phải có số serial");
    }
    return soSerial;
  }

  /**
   * Bảo đảm máy chưa nằm trên phiếu đổi/trả còn hiệu lực trong cùng transaction.
   *
   * @param ketNoi kết nối của transaction đang mở
   * @param maHD mã hóa đơn gốc
   * @param soSerial serial máy cần kiểm
   * @throws IllegalStateException nếu đã có phiếu hiệu lực cho cặp (hóa đơn, serial)
   */
  private static void kiemTraChuaCoPhieuHieuLuc(
      Connection ketNoi, String maHD, String soSerial) throws SQLException {
    try (PreparedStatement statement = ketNoi.prepareStatement(SELECT_PHIEU_HIEU_LUC)) {
      statement.setString(1, maHD);
      statement.setString(2, soSerial);
      statement.setString(3, maHD);
      statement.setString(4, soSerial);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          throw new IllegalStateException(
              "Máy " + soSerial + " đã được đổi/trả trước đó");
        }
      }
    }
  }

  private static void ganNgay(PreparedStatement statement, int chiSo, LocalDate ngay)
      throws SQLException {
    if (ngay == null) {
      statement.setNull(chiSo, Types.DATE);
    } else {
      statement.setDate(chiSo, Date.valueOf(ngay));
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

  private PhieuTra mapPhieuTra(ResultSet resultSet) throws SQLException {
    NhanVien nhanVienLap = mapNhanVien(resultSet, "lap_");
    NhanVien nhanVienDuyet = mapNhanVien(resultSet, "duyet_");
    HoaDon hoaDon = mapHoaDon(resultSet);
    Date ngayHoanTat = resultSet.getDate("ngayHoanTat");
    Date ngayDuyet = resultSet.getDate("ngayDuyet");
    return new PhieuTra(
        resultSet.getString("maPhieuTra"),
        nhanVienLap,
        hoaDon,
        resultSet.getObject("ngayTao", LocalDate.class),
        ngayHoanTat == null ? null : ngayHoanTat.toLocalDate(),
        TrangThaiDuyet.valueOf(resultSet.getString("trangThaiDuyet")),
        TrangThaiXuLyPhieu.valueOf(resultSet.getString("trangThaiXuLyPhieu")),
        nhanVienDuyet,
        ngayDuyet == null ? null : ngayDuyet.toLocalDate(),
        resultSet.getString("lyDoDuyet"),
        resultSet.getBigDecimal("tienKhachDua"));
  }

  private HoaDon mapHoaDon(ResultSet resultSet) throws SQLException {
    NhanVien nhanVien = mapNhanVien(resultSet, "hdnv_");
    KhachHang khachHang = new KhachHang(
        resultSet.getString("kh_maKH"),
        resultSet.getString("kh_hoTen"),
        resultSet.getString("kh_soDienThoai"),
        resultSet.getString("kh_diaChi"));
    return new HoaDon(
        resultSet.getString("hd_maHD"),
        resultSet.getObject("hd_ngayLap", LocalDate.class),
        nhanVien,
        khachHang,
        resultSet.getBoolean("hd_trangThaiThanhToan"));
  }

  private NhanVien mapNhanVien(ResultSet resultSet, String tienTo) throws SQLException {
    String maNV = resultSet.getString(tienTo + "maNV");
    if (maNV == null) {
      return null;
    }
    return new NhanVien(
        maNV,
        resultSet.getString(tienTo + "hoTen"),
        resultSet.getString(tienTo + "soDienThoai"),
        resultSet.getString(tienTo + "email"),
        resultSet.getObject(tienTo + "ngayVaoLam", LocalDate.class),
        resultSet.getString(tienTo + "tenDangNhap"),
        // Không nạp mật khẩu: nghiệp vụ đổi/trả không cần và không được đưa hash lên tầng trên.
        null,
        LoaiQuyen.valueOf(resultSet.getString(tienTo + "loaiQuyen")),
        resultSet.getBoolean(tienTo + "trangThaiHoatDong"));
  }

  private ChiTietPhieuTra mapChiTietPhieuTra(ResultSet resultSet, PhieuTra phieuTra)
      throws SQLException {
    SanPham sanPham = mapSanPham(resultSet, "");
    ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon(
        phieuTra.hoaDon(),
        sanPham,
        resultSet.getInt("cthd_soLuong"),
        resultSet.getBigDecimal("cthd_donGia"),
        resultSet.getBigDecimal("cthd_thueSuat"),
        mapKhuyenMai(resultSet, sanPham.dongSanPham()),
        resultSet.getBigDecimal("cthd_tyLeGiam"),
        resultSet.getBigDecimal("cthd_tienKhuyenMai"),
        resultSet.getBigDecimal("cthd_tienTruoc"),
        resultSet.getBigDecimal("cthd_tienSau"),
        resultSet.getBigDecimal("cthd_tienThue"));
    return new ChiTietPhieuTra(
        phieuTra,
        chiTietHoaDon,
        TinhTrangHangTra.valueOf(resultSet.getString("tinhTrangHangTra")),
        resultSet.getString("lyDoTra"));
  }

  private SanPham mapSanPham(ResultSet resultSet, String tienTo) throws SQLException {
    NhaCungCap nhaCungCap = new NhaCungCap(
        resultSet.getString(tienTo + "ncc_maNCC"),
        resultSet.getString(tienTo + "ncc_tenNCC"));
    DanhMucSanPham danhMuc = new DanhMucSanPham(
        resultSet.getString(tienTo + "dm_maDanhMuc"),
        resultSet.getString(tienTo + "dm_tenDanhMuc"),
        resultSet.getString(tienTo + "dm_moTa"));
    DongSanPham dongSanPham = new DongSanPham(
        resultSet.getString(tienTo + "dsp_maDongSP"),
        resultSet.getString(tienTo + "dsp_tenDongSP"),
        danhMuc,
        resultSet.getString(tienTo + "dsp_donViTinh"),
        resultSet.getInt(tienTo + "dsp_soLuongTon"),
        resultSet.getBigDecimal(tienTo + "dsp_thue"),
        resultSet.getBigDecimal(tienTo + "dsp_donGiaBanTruocThue"),
        resultSet.getBoolean(tienTo + "dsp_trangThaiKinhDoanh"),
        resultSet.getObject(tienTo + "dsp_ngayNhapKho", LocalDate.class),
        resultSet.getObject(tienTo + "dsp_ngayCapNhapCuoi", LocalDate.class));
    return new SanPham(
        resultSet.getString(tienTo + "sp_soSerial"),
        nhaCungCap,
        dongSanPham,
        resultSet.getBigDecimal(tienTo + "sp_giaNhap"),
        resultSet.getObject(tienTo + "sp_ngayNhapKhoBanDau", LocalDate.class),
        TrangThaiSanPham.valueOf(resultSet.getString(tienTo + "sp_trangThai")),
        resultSet.getInt(tienTo + "sp_soLanDoiTra"),
        resultSet.getInt(tienTo + "sp_soLanSua"));
  }

  private KhuyenMai mapKhuyenMai(ResultSet resultSet, DongSanPham dongSanPham)
      throws SQLException {
    String maKM = resultSet.getString("cthd_maKM");
    if (maKM == null) {
      return null;
    }
    return new KhuyenMai(
        resultSet.getString("km_maKM"),
        resultSet.getString("km_tenKM"),
        resultSet.getObject("km_ngayBatDau", LocalDate.class),
        resultSet.getObject("km_ngayKetThuc", LocalDate.class),
        resultSet.getBigDecimal("km_tyLeGiam"),
        resultSet.getString("km_moTa"),
        resultSet.getBoolean("km_dangApDung"),
        dongSanPham);
  }
}
