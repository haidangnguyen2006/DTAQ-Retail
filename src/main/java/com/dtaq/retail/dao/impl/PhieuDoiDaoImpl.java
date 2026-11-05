package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.PhieuDoiDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuDoi;
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
 * Triển khai JDBC của {@link PhieuDoiDao} cho nghiệp vụ lập/hoàn tất phiếu đổi (UC006) và duyệt
 * yêu cầu đổi hàng (UC018).
 */
public class PhieuDoiDaoImpl implements PhieuDoiDao {

  private static final String SELECT_CHO_DUYET =
      "SELECT pd.\"maPhieuDoi\" AS maPhieu, pd.\"hoaDon\" AS maHDGoc, kh.\"hoTen\" AS tenKhachHang,"
          + " kh.\"soDienThoai\" AS soDienThoai, pd.\"tienChenhLech\" AS tienChenhLech,"
          + " nv.\"hoTen\" AS tenNhanVien, pd.\"ngayTao\" AS ngayTao,"
          + " (SELECT string_agg(ct.\"lyDoDoi\", '; ') FROM \"ChiTietPhieuDoi\" ct"
          + "  WHERE ct.\"maPhieuDoi\" = pd.\"maPhieuDoi\") AS lyDo"
          + " FROM \"PhieuDoi\" pd"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pd.\"hoaDon\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = pd.\"nhanVienLap\""
          + " WHERE pd.\"trangThaiDuyet\" = 'CHO_DUYET'"
          + " ORDER BY pd.\"ngayTao\", pd.\"maPhieuDoi\"";

  private static final String SELECT_CHO_HOAN_TAT =
      "SELECT pd.\"maPhieuDoi\" AS maPhieu, pd.\"hoaDon\" AS maHDGoc, kh.\"hoTen\" AS tenKhachHang,"
          + " kh.\"soDienThoai\" AS soDienThoai, pd.\"tienChenhLech\" AS tienChenhLech,"
          + " nv.\"hoTen\" AS tenNhanVien, pd.\"ngayTao\" AS ngayTao,"
          + " (SELECT string_agg(ct.\"lyDoDoi\", '; ') FROM \"ChiTietPhieuDoi\" ct"
          + "  WHERE ct.\"maPhieuDoi\" = pd.\"maPhieuDoi\") AS lyDo"
          + " FROM \"PhieuDoi\" pd"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pd.\"hoaDon\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " JOIN \"NhanVien\" nv ON nv.\"maNV\" = pd.\"nhanVienLap\""
          + " WHERE pd.\"trangThaiDuyet\" = 'DA_DUYET'"
          + " AND pd.\"trangThaiXuLyPhieu\" = 'CHO_XU_LY'"
          + " ORDER BY pd.\"ngayTao\", pd.\"maPhieuDoi\"";

  private static final String SELECT_BY_ID =
      "SELECT pd.\"maPhieuDoi\" AS maPhieuDoi, pd.\"ngayTao\" AS ngayTao,"
          + " pd.\"ngayHoanTat\" AS ngayHoanTat, pd.\"trangThaiDuyet\" AS trangThaiDuyet,"
          + " pd.\"trangThaiXuLyPhieu\" AS trangThaiXuLyPhieu, pd.\"lyDoDuyet\" AS lyDoDuyet,"
          + " pd.\"ngayDuyet\" AS ngayDuyet,"
          + " pd.\"tienKhachDuaThem\" AS tienKhachDuaThem,"
          + " pd.\"tienChenhLech\" AS tienChenhLech,"
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
          + " FROM \"PhieuDoi\" pd"
          + " JOIN \"HoaDon\" hd ON hd.\"maHD\" = pd.\"hoaDon\""
          + " JOIN \"NhanVien\" lap ON lap.\"maNV\" = pd.\"nhanVienLap\""
          + " JOIN \"NhanVien\" hdnv ON hdnv.\"maNV\" = hd.\"nhanVienLap\""
          + " JOIN \"KhachHang\" kh ON kh.\"maKH\" = hd.\"khachHang\""
          + " LEFT JOIN \"NhanVien\" duyet ON duyet.\"maNV\" = pd.\"nhanVienDuyet\""
          + " WHERE pd.\"maPhieuDoi\" = ?";

  private static final String SELECT_CHI_TIET =
      "SELECT ct.\"soSerial\" AS soSerial, ct.\"lyDoDoi\" AS lyDoDoi,"
          + " ct.\"tinhTrangHangTra\" AS tinhTrangHangTra,"
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
          + " km.\"dangApDung\" AS km_dangApDung,"
          + " doi.\"soSerial\" AS doi_sp_soSerial, doi.\"maNCC\" AS doi_sp_maNCC,"
          + " doi.\"maDongSP\" AS doi_sp_maDongSP, doi.\"giaNhap\" AS doi_sp_giaNhap,"
          + " doi.\"ngayNhapKhoBanDau\" AS doi_sp_ngayNhapKhoBanDau,"
          + " doi.\"trangThai\" AS doi_sp_trangThai,"
          + " doi.\"soLanDoiTra\" AS doi_sp_soLanDoiTra,"
          + " doi.\"soLanSua\" AS doi_sp_soLanSua,"
          + " doincc.\"maNCC\" AS doi_ncc_maNCC, doincc.\"tenNCC\" AS doi_ncc_tenNCC,"
          + " doidsp.\"maDongSP\" AS doi_dsp_maDongSP,"
          + " doidsp.\"tenDongSP\" AS doi_dsp_tenDongSP,"
          + " doidsp.\"maDanhMuc\" AS doi_dsp_maDanhMuc,"
          + " doidsp.\"donViTinh\" AS doi_dsp_donViTinh,"
          + " doidsp.\"soLuongTon\" AS doi_dsp_soLuongTon,"
          + " doidsp.\"thue\" AS doi_dsp_thue,"
          + " doidsp.\"donGiaBanTruocThue\" AS doi_dsp_donGiaBanTruocThue,"
          + " doidsp.\"trangThaiKinhDoanh\" AS doi_dsp_trangThaiKinhDoanh,"
          + " doidsp.\"ngayNhapKho\" AS doi_dsp_ngayNhapKho,"
          + " doidsp.\"ngayCapNhapCuoi\" AS doi_dsp_ngayCapNhapCuoi,"
          + " doidm.\"maDanhMuc\" AS doi_dm_maDanhMuc,"
          + " doidm.\"tenDanhMuc\" AS doi_dm_tenDanhMuc, doidm.\"moTa\" AS doi_dm_moTa"
          + " FROM \"ChiTietPhieuDoi\" ct"
          + " JOIN \"ChiTietHoaDon\" cthd"
          + " ON cthd.\"maHD\" = ct.\"maHD\" AND cthd.\"soSerial\" = ct.\"soSerial\""
          + " JOIN \"SanPham\" sp"
          + " ON sp.\"soSerial\" = cthd.\"soSerial\" AND sp.\"maDongSP\" = cthd.\"maDongSP\""
          + " JOIN \"NhaCungCap\" ncc ON ncc.\"maNCC\" = sp.\"maNCC\""
          + " JOIN \"DongSanPham\" dsp ON dsp.\"maDongSP\" = sp.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" dm ON dm.\"maDanhMuc\" = dsp.\"maDanhMuc\""
          + " LEFT JOIN \"KhuyenMai\" km ON km.\"maKM\" = cthd.\"maKM\""
          + " JOIN \"SanPham\" doi ON doi.\"soSerial\" = ct.\"chiTietSanPhamDoi\""
          + " JOIN \"NhaCungCap\" doincc ON doincc.\"maNCC\" = doi.\"maNCC\""
          + " JOIN \"DongSanPham\" doidsp ON doidsp.\"maDongSP\" = doi.\"maDongSP\""
          + " JOIN \"DanhMucSanPham\" doidm ON doidm.\"maDanhMuc\" = doidsp.\"maDanhMuc\""
          + " WHERE ct.\"maPhieuDoi\" = ?"
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
      "UPDATE \"PhieuDoi\" SET \"trangThaiDuyet\" = ?, \"nhanVienDuyet\" = ?, \"ngayDuyet\" = ?,"
          + " \"lyDoDuyet\" = ?"
          + " WHERE \"maPhieuDoi\" = ? AND \"trangThaiDuyet\" = 'CHO_DUYET'";

  private static final String INSERT_PHIEU_DOI =
      "INSERT INTO \"PhieuDoi\" (\"maPhieuDoi\", \"nhanVienLap\", \"hoaDon\", \"ngayHoanTat\","
          + " \"ngayTao\", \"trangThaiDuyet\", \"trangThaiXuLyPhieu\", \"tienKhachDuaThem\","
          + " \"tienChenhLech\", \"nhanVienDuyet\", \"ngayDuyet\", \"lyDoDuyet\")"
          + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

  private static final String INSERT_CHI_TIET =
      "INSERT INTO \"ChiTietPhieuDoi\" (\"maPhieuDoi\", \"maHD\", \"soSerial\", \"lyDoDoi\","
          + " \"tinhTrangHangTra\", \"chiTietSanPhamDoi\")"
          + " VALUES (?,?,?,?,?,?)";

  private static final String UPDATE_MAY_NHAN_LAI =
      "UPDATE \"SanPham\" SET \"trangThai\" = ?, \"soLanDoiTra\" = \"soLanDoiTra\" + 1"
          + " WHERE \"soSerial\" = ? AND \"trangThai\" = 'DA_BAN'";

  private static final String UPDATE_MAY_THAY_THE =
      "UPDATE \"SanPham\" SET \"trangThai\" = 'DA_BAN'"
          + " WHERE \"soSerial\" = ? AND \"trangThai\" = 'SAN_SANG_BAN'";

  private static final String UPDATE_HOAN_TAT =
      "UPDATE \"PhieuDoi\" SET \"trangThaiXuLyPhieu\" = 'HOAN_TAT', \"ngayHoanTat\" = ?"
          + " WHERE \"maPhieuDoi\" = ? AND \"trangThaiXuLyPhieu\" = 'CHO_XU_LY'";

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
    return findDanhSach(SELECT_CHO_DUYET, "Không thể tải danh sách phiếu đổi chờ duyệt");
  }

  @Override
  public List<PhieuDuyet> findChoHoanTat() {
    return findDanhSach(SELECT_CHO_HOAN_TAT, "Không thể tải danh sách phiếu đổi chờ hoàn tất");
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
  public Optional<PhieuDoi> findById(String maPhieuDoi) {
    if (maPhieuDoi == null || maPhieuDoi.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maPhieuDoi.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapPhieuDoi(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu phiếu đổi " + maPhieuDoi, e);
    }
  }

  @Override
  public List<ChiTietPhieuDoi> findChiTiet(String maPhieuDoi) {
    Optional<PhieuDoi> dauPhieu = findById(maPhieuDoi);
    if (dauPhieu.isEmpty()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_CHI_TIET)) {
      statement.setString(1, dauPhieu.get().maPhieuDoi());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ChiTietPhieuDoi> ketQua = new ArrayList<>();
        while (resultSet.next()) {
          ketQua.add(mapChiTietPhieuDoi(resultSet, dauPhieu.get()));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải chi tiết phiếu đổi " + maPhieuDoi, e);
    }
  }

  @Override
  public String taoPhieuDoi(
      Connection ketNoi, PhieuDoi phieuDoi, List<ChiTietPhieuDoi> chiTiet) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(phieuDoi, "phieuDoi");
    Objects.requireNonNull(chiTiet, "chiTiet");
    if (phieuDoi.nhanVienLap() == null) {
      throw new IllegalArgumentException("Phiếu đổi phải có nhân viên lập");
    }
    if (phieuDoi.hoaDon() == null) {
      throw new IllegalArgumentException("Phiếu đổi phải có hóa đơn gốc");
    }
    if (phieuDoi.ngayTao() == null) {
      throw new IllegalArgumentException("Phiếu đổi phải có ngày tạo");
    }
    if (phieuDoi.trangThaiDuyet() == null || phieuDoi.trangThaiXuLyPhieu() == null) {
      throw new IllegalArgumentException("Phiếu đổi phải có trạng thái duyệt và trạng thái xử lý");
    }
    if (phieuDoi.tienKhachDuaThem() == null) {
      throw new IllegalArgumentException("Phiếu đổi phải có số tiền khách đưa thêm");
    }
    String maHDGoc = phieuDoi.hoaDon().maHD();

    try {
      for (ChiTietPhieuDoi dong : chiTiet) {
        // Guard nằm trong cùng transaction với INSERT để thu hẹp khe TOCTOU giữa hai yêu cầu.
        kiemTraChuaCoPhieuHieuLuc(ketNoi, maHDGoc, serialMayTra(dong));
      }
      String maPhieuDoi = MaGenerator.next(ketNoi, "PhieuDoi", "maPhieuDoi", "PD");
      try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_PHIEU_DOI)) {
        statement.setString(1, maPhieuDoi);
        statement.setString(2, phieuDoi.nhanVienLap().maNV());
        statement.setString(3, maHDGoc);
        ganNgay(statement, 4, phieuDoi.ngayHoanTat());
        statement.setDate(5, Date.valueOf(phieuDoi.ngayTao()));
        statement.setString(6, phieuDoi.trangThaiDuyet().name());
        statement.setString(7, phieuDoi.trangThaiXuLyPhieu().name());
        statement.setBigDecimal(8, phieuDoi.tienKhachDuaThem());
        statement.setBigDecimal(9, Objects.requireNonNullElse(
            phieuDoi.tienChenhLech(), BigDecimal.ZERO));
        if (phieuDoi.nhanVienDuyet() == null) {
          statement.setNull(10, Types.VARCHAR);
        } else {
          statement.setString(10, phieuDoi.nhanVienDuyet().maNV());
        }
        ganNgay(statement, 11, phieuDoi.ngayDuyet());
        statement.setString(12, phieuDoi.lyDoDuyet());
        if (statement.executeUpdate() != 1) {
          throw new DataAccessException("Không thể ghi đầu phiếu đổi " + maPhieuDoi);
        }
      }
      for (ChiTietPhieuDoi dong : chiTiet) {
        ghiChiTiet(ketNoi, maPhieuDoi, maHDGoc, dong);
      }
      return maPhieuDoi;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể ghi phiếu đổi cho hóa đơn " + maHDGoc, e);
    }
  }

  @Override
  public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(chiTiet, "chiTiet");
    int tongSoDong = 0;
    try (PreparedStatement statement = ketNoi.prepareStatement(UPDATE_MAY_NHAN_LAI)) {
      for (ChiTietPhieuDoi dong : chiTiet) {
        String soSerial = serialMayTra(dong);
        if (dong.tinhTrangHangTra() == null) {
          throw new IllegalArgumentException(
              "Dòng chi tiết phiếu đổi phải có tình trạng hàng trả");
        }
        // UC006 bước 6 + 15.a: tình trạng máy khách trả quyết định máy quay lại kho bán hay
        // chuyển sang nhóm hàng lỗi cần xử lý.
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
  public int danhDauMayThayThe(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(chiTiet, "chiTiet");
    int tongSoDong = 0;
    try (PreparedStatement statement = ketNoi.prepareStatement(UPDATE_MAY_THAY_THE)) {
      for (ChiTietPhieuDoi dong : chiTiet) {
        if (dong == null || dong.chiTietSanPhamDoi() == null) {
          throw new IllegalArgumentException("Dòng phiếu đổi phải có máy thay thế");
        }
        String soSerial = dong.chiTietSanPhamDoi().soSerial();
        if (soSerial == null || soSerial.isBlank()) {
          throw new IllegalArgumentException("Máy thay thế phải có số serial");
        }
        statement.setString(1, soSerial);
        int soDong = statement.executeUpdate();
        if (soDong != 1) {
          throw new IllegalStateException(
              "Máy " + soSerial + " không còn ở trạng thái sẵn sàng bán");
        }
        tongSoDong += soDong;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể cập nhật trạng thái máy thay thế", e);
    }
    return tongSoDong;
  }

  @Override
  public boolean capNhatHoanTat(
      Connection ketNoi, String maPhieuDoi, LocalDate ngayHoanTat) {
    Objects.requireNonNull(ketNoi, "ketNoi");
    Objects.requireNonNull(ngayHoanTat, "ngayHoanTat");
    if (maPhieuDoi == null || maPhieuDoi.isBlank()) {
      return false;
    }
    try (PreparedStatement statement = ketNoi.prepareStatement(UPDATE_HOAN_TAT)) {
      statement.setDate(1, Date.valueOf(ngayHoanTat));
      statement.setString(2, maPhieuDoi.trim());
      return statement.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new DataAccessException("Không thể hoàn tất phiếu đổi " + maPhieuDoi, e);
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
      Connection ketNoi, String maPhieuDoi, String maHDGoc, ChiTietPhieuDoi dong)
      throws SQLException {
    if (dong == null) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu đổi không được để trống");
    }
    if (dong.lyDoDoi() == null || dong.lyDoDoi().isBlank()) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu đổi phải có lý do đổi");
    }
    if (dong.tinhTrangHangTra() == null) {
      throw new IllegalArgumentException(
          "Dòng chi tiết phiếu đổi phải có tình trạng hàng trả");
    }
    if (dong.chiTietSanPhamDoi() == null || dong.chiTietSanPhamDoi().soSerial() == null
        || dong.chiTietSanPhamDoi().soSerial().isBlank()) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu đổi phải có máy thay thế");
    }
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT_CHI_TIET)) {
      statement.setString(1, maPhieuDoi);
      // RB24: mọi dòng chi tiết thuộc đúng hóa đơn gốc của đầu phiếu, không tin dữ liệu dòng.
      statement.setString(2, maHDGoc);
      statement.setString(3, serialMayTra(dong));
      statement.setString(4, dong.lyDoDoi().trim());
      statement.setString(5, dong.tinhTrangHangTra().name());
      statement.setString(6, dong.chiTietSanPhamDoi().soSerial());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException(
            "Không thể ghi dòng chi tiết phiếu đổi " + maPhieuDoi);
      }
    }
  }

  private static String serialMayTra(ChiTietPhieuDoi dong) {
    if (dong == null || dong.chiTietHoaDon() == null
        || dong.chiTietHoaDon().sanPham() == null) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu đổi phải có máy được đổi");
    }
    String soSerial = dong.chiTietHoaDon().sanPham().soSerial();
    if (soSerial == null || soSerial.isBlank()) {
      throw new IllegalArgumentException("Dòng chi tiết phiếu đổi phải có số serial");
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
    // D17 (2026-11-05): đổi hàng không hoàn tiền nên hoàn dự kiến luôn bằng 0,
    // dù "tienChenhLech" có âm (phần khách mất khi đổi sang máy rẻ hơn).
    Date ngayTao = resultSet.getDate("ngayTao");
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

  private PhieuDoi mapPhieuDoi(ResultSet resultSet) throws SQLException {
    NhanVien nhanVienLap = mapNhanVien(resultSet, "lap_");
    NhanVien nhanVienDuyet = mapNhanVien(resultSet, "duyet_");
    HoaDon hoaDon = mapHoaDon(resultSet);
    Date ngayHoanTat = resultSet.getDate("ngayHoanTat");
    Date ngayDuyet = resultSet.getDate("ngayDuyet");
    return new PhieuDoi(
        resultSet.getString("maPhieuDoi"),
        nhanVienLap,
        hoaDon,
        ngayHoanTat == null ? null : ngayHoanTat.toLocalDate(),
        resultSet.getObject("ngayTao", LocalDate.class),
        TrangThaiDuyet.valueOf(resultSet.getString("trangThaiDuyet")),
        TrangThaiXuLyPhieu.valueOf(resultSet.getString("trangThaiXuLyPhieu")),
        resultSet.getBigDecimal("tienKhachDuaThem"),
        resultSet.getBigDecimal("tienChenhLech"),
        nhanVienDuyet,
        ngayDuyet == null ? null : ngayDuyet.toLocalDate(),
        resultSet.getString("lyDoDuyet"));
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

  private ChiTietPhieuDoi mapChiTietPhieuDoi(ResultSet resultSet, PhieuDoi phieuDoi)
      throws SQLException {
    SanPham sanPham = mapSanPham(resultSet, "");
    ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon(
        phieuDoi.hoaDon(),
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
    return new ChiTietPhieuDoi(
        phieuDoi,
        chiTietHoaDon,
        resultSet.getString("lyDoDoi"),
        TinhTrangHangTra.valueOf(resultSet.getString("tinhTrangHangTra")),
        mapSanPham(resultSet, "doi_"));
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
