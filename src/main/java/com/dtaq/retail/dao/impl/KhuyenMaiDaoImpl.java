package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.entity.ChiTietKhuyenMai;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Triển khai JDBC của {@link KhuyenMaiDao} trên bảng chương trình và bảng chi tiết.
 */
public class KhuyenMaiDaoImpl implements KhuyenMaiDao {

  private static final String SELECT_BASE =
      "SELECT km.\"maKM\", km.\"tenKM\", km.\"ngayBatDau\", km.\"ngayKetThuc\", km.\"tyLeGiam\","
          + " km.\"moTa\", km.\"dangApDung\", ct.\"maDongSP\","
          + " dsp.\"tenDongSP\", dsp.\"donViTinh\", dsp.\"soLuongTon\", dsp.\"thue\","
          + " dsp.\"donGiaBanTruocThue\", dsp.\"trangThaiKinhDoanh\", dsp.\"ngayNhapKho\","
          + " dsp.\"ngayCapNhapCuoi\", dsp.\"maDanhMuc\""
          + " FROM \"KhuyenMai\" km"
          + " JOIN \"ChiTietKhuyenMai\" ct ON ct.\"maKM\" = km.\"maKM\""
          + " JOIN \"DongSanPham\" dsp ON ct.\"maDongSP\" = dsp.\"maDongSP\"";

  private static final String SELECT_ALL =
      SELECT_BASE + " ORDER BY km.\"ngayBatDau\" DESC, km.\"maKM\" DESC";

  private static final String SELECT_BY_ID =
      SELECT_BASE + " WHERE km.\"maKM\" = ?";

  private static final String SELECT_BY_DONG_SP =
      SELECT_BASE
          + " WHERE km.\"maKM\" IN (SELECT \"maKM\" FROM \"ChiTietKhuyenMai\" WHERE \"maDongSP\" = ?)"
          + " ORDER BY km.\"ngayBatDau\" DESC, km.\"maKM\" DESC,"
          + " CASE WHEN ct.\"maDongSP\" = ? THEN 0 ELSE 1 END, ct.\"maDongSP\"";

  private static final String SELECT_HIEU_LUC =
      SELECT_BASE
          + " WHERE EXISTS (SELECT 1 FROM \"ChiTietKhuyenMai\" loc"
          + " WHERE loc.\"maKM\" = km.\"maKM\" AND loc.\"maDongSP\" = ?)"
          + " AND km.\"dangApDung\" = true"
          + " AND ? >= km.\"ngayBatDau\" AND ? <= km.\"ngayKetThuc\""
          + " ORDER BY km.\"tyLeGiam\" DESC, km.\"ngayBatDau\" DESC, km.\"maKM\" ASC,"
          + " CASE WHEN ct.\"maDongSP\" = ? THEN 0 ELSE 1 END, ct.\"maDongSP\"";

  private static final String INSERT =
      "INSERT INTO \"KhuyenMai\" (\"maKM\", \"tenKM\", \"ngayBatDau\", \"ngayKetThuc\","
          + " \"tyLeGiam\", \"moTa\", \"dangApDung\") VALUES (?, ?, ?, ?, ?, ?, ?)";

  private static final String INSERT_CHI_TIET =
      "INSERT INTO \"ChiTietKhuyenMai\" (\"maKM\", \"maDongSP\") VALUES (?, ?)";

  private static final String DELETE_CHI_TIET =
      "DELETE FROM \"ChiTietKhuyenMai\" WHERE \"maKM\" = ?";

  private static final String UPDATE =
      "UPDATE \"KhuyenMai\" SET \"tenKM\" = ?, \"ngayBatDau\" = ?, \"ngayKetThuc\" = ?,"
          + " \"tyLeGiam\" = ?, \"moTa\" = ?, \"dangApDung\" = ?"
          + " WHERE \"maKM\" = ?";

  private static final String UPDATE_TRANG_THAI_AP_DUNG =
      "UPDATE \"KhuyenMai\" SET \"dangApDung\" = ? WHERE \"maKM\" = ?";

  private static final String EXISTS_TRUNG_MA =
      "SELECT EXISTS(SELECT 1 FROM \"KhuyenMai\" WHERE \"maKM\" = ?)";

  private static final String EXISTS_CHONG_LAN =
      "SELECT EXISTS("
          + " SELECT 1 FROM \"KhuyenMai\" km"
          + " JOIN \"ChiTietKhuyenMai\" ct ON ct.\"maKM\" = km.\"maKM\""
          + " WHERE ct.\"maDongSP\" = ?"
          + " AND km.\"dangApDung\" = true"
          + " AND NOT (km.\"ngayKetThuc\" < ? OR km.\"ngayBatDau\" > ?)"
          + " AND (? IS NULL OR km.\"maKM\" <> ?)"
          + ")";

  private final DataSource dataSource;

  /**
   * Khởi tạo DAO với nguồn dữ liệu dùng chung.
   *
   * @param dataSource nguồn dữ liệu kết nối CSDL; không được {@code null}
   */
  public KhuyenMaiDaoImpl(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public Optional<KhuyenMai> findHieuLuc(String maDongSP, LocalDate ngay) {
    if (maDongSP == null || maDongSP.isBlank() || ngay == null) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_HIEU_LUC)) {
      statement.setString(1, maDongSP.trim());
      Date sqlDate = Date.valueOf(ngay);
      statement.setDate(2, sqlDate);
      statement.setDate(3, sqlDate);
      statement.setString(4, maDongSP.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        Map<String, KhuyenMaiAccumulator> rows = new LinkedHashMap<>();
        do {
          mapKhuyenMai(resultSet, rows);
        } while (resultSet.next());
        return Optional.of(rows.values().iterator().next().toEntity());
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu khuyến mãi hiệu lực cho dòng sản phẩm: " + maDongSP, e);
    }
  }

  @Override
  public Optional<KhuyenMai> findById(String maKM) {
    if (maKM == null || maKM.isBlank()) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setString(1, maKM.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        Map<String, KhuyenMaiAccumulator> rows = readKhuyenMai(resultSet);
        return rows.values().stream().findFirst().map(KhuyenMaiAccumulator::toEntity);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tra cứu khuyến mãi theo mã: " + maKM, e);
    }
  }

  @Override
  public List<KhuyenMai> findAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      return toEntities(readKhuyenMai(resultSet));
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách chương trình khuyến mãi", e);
    }
  }

  @Override
  public List<KhuyenMai> findByDongSanPham(String maDongSP) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_DONG_SP)) {
      statement.setString(1, maDongSP.trim());
      statement.setString(2, maDongSP.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        return toEntities(readKhuyenMai(resultSet));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tải danh sách khuyến mãi của dòng sản phẩm: " + maDongSP, e);
    }
  }

  @Override
  public List<DongApDung> findDongTheoDieuKien(
      String maDongSP, List<String> maDanhMuc, LocalDate ngayNhapMuonNhat) {
    String dieuKienNgay = ngayNhapMuonNhat == null ? "" : " AND sp.\"ngayNhapKhoBanDau\" <= ?";
    String sql = """
        SELECT dsp.*, dm."tenDanhMuc", dm."moTa", may."soMay"
        FROM "DongSanPham" dsp
        JOIN "DanhMucSanPham" dm ON dm."maDanhMuc" = dsp."maDanhMuc"
        CROSS JOIN LATERAL (
          SELECT count(*) AS "soMay" FROM "SanPham" sp
          WHERE sp."maDongSP" = dsp."maDongSP" AND sp."trangThai" = 'SAN_SANG_BAN'%s
        ) may
        WHERE dsp."trangThaiKinhDoanh" = true
        """.formatted(dieuKienNgay);
    if (maDongSP != null) {
      sql += " AND dsp.\"maDongSP\" = ?";
    } else if (!maDanhMuc.isEmpty()) {
      sql += " AND dsp.\"maDanhMuc\" IN ("
          + String.join(", ", Collections.nCopies(maDanhMuc.size(), "?")) + ")";
    }
    if (ngayNhapMuonNhat != null) {
      sql += " AND may.\"soMay\" > 0";
    }
    sql += " ORDER BY dsp.\"maDongSP\"";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement statement = conn.prepareStatement(sql)) {
      int index = 1;
      if (ngayNhapMuonNhat != null) {
        statement.setObject(index++, ngayNhapMuonNhat);
      }
      if (maDongSP != null) {
        statement.setString(index++, maDongSP);
      } else {
        for (String ma : maDanhMuc) {
          statement.setString(index++, ma);
        }
      }
      try (ResultSet rs = statement.executeQuery()) {
        List<DongApDung> ketQua = new ArrayList<>();
        while (rs.next()) {
          ketQua.add(new DongApDung(mapDongSanPham(rs, new DanhMucSanPham(
              rs.getString("maDanhMuc"), rs.getString("tenDanhMuc"), rs.getString("moTa"))),
              rs.getInt("soMay")));
        }
        return ketQua;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể tìm dòng sản phẩm theo điều kiện khuyến mãi", e);
    }
  }

  @Override
  public void insert(KhuyenMai km) {
    validateKhuyenMai(km);
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        insert(connection, km);
        connection.commit();
      } catch (SQLException | RuntimeException e) {
        try {
          connection.rollback();
        } catch (SQLException rollbackError) {
          e.addSuppressed(rollbackError);
        }
        if (e instanceof SQLException sqlException) {
          handleSqlException("Không thể thêm mới chương trình khuyến mãi", sqlException);
        }
        throw e;
      }
    } catch (SQLException e) {
      handleSqlException("Không thể thêm mới chương trình khuyến mãi", e);
    }
  }

  @Override
  public void insert(Connection ketNoi, KhuyenMai km) {
    validateKhuyenMai(km);
    Objects.requireNonNull(ketNoi, "ketNoi");
    try (PreparedStatement statement = ketNoi.prepareStatement(INSERT)) {
      statement.setString(1, km.maKM().trim());
      statement.setString(2, km.tenKM().trim());
      statement.setDate(3, Date.valueOf(km.ngayBatDau()));
      statement.setDate(4, Date.valueOf(km.ngayKetThuc()));
      statement.setBigDecimal(5, km.tyLeGiam());
      statement.setString(6, km.moTa());
      statement.setBoolean(7, km.dangApDung());
      if (statement.executeUpdate() != 1) {
        throw new DataAccessException("Không thể lưu chương trình khuyến mãi " + km.maKM());
      }
      try (PreparedStatement detail = ketNoi.prepareStatement(INSERT_CHI_TIET)) {
        for (ChiTietKhuyenMai row : km.chiTiet()) {
          detail.setString(1, km.maKM().trim());
          String maDongSP = row.dongSanPham().maDongSP().trim();
          detail.setString(2, maDongSP);
          try {
            detail.executeUpdate();
          } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) throw new LoiDongKhuyenMai(maDongSP, e);
            throw e;
          }
        }
      }
    } catch (SQLException e) {
      handleSqlException("Không thể thêm mới chương trình khuyến mãi", e);
    }
  }

  @Override
  public void update(KhuyenMai km) {
    validateKhuyenMai(km);
    String maKM = km.maKM().trim();
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
          statement.setString(1, km.tenKM().trim());
          statement.setDate(2, Date.valueOf(km.ngayBatDau()));
          statement.setDate(3, Date.valueOf(km.ngayKetThuc()));
          statement.setBigDecimal(4, km.tyLeGiam());
          statement.setString(5, km.moTa());
          statement.setBoolean(6, km.dangApDung());
          statement.setString(7, maKM);
          if (statement.executeUpdate() == 0) {
            throw new DataAccessException("Không tìm thấy chương trình khuyến mãi để cập nhật: " + maKM);
          }
        }
        try (PreparedStatement statement = connection.prepareStatement(DELETE_CHI_TIET)) {
          statement.setString(1, maKM);
          statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CHI_TIET)) {
          for (ChiTietKhuyenMai row : km.chiTiet()) {
            statement.setString(1, maKM);
            statement.setString(2, row.dongSanPham().maDongSP().trim());
            statement.executeUpdate();
          }
        }
        connection.commit();
      } catch (SQLException | RuntimeException e) {
        try {
          connection.rollback();
        } catch (SQLException rollbackError) {
          e.addSuppressed(rollbackError);
        }
        throw e;
      }
    } catch (SQLException e) {
      handleSqlException("Không thể cập nhật chương trình khuyến mãi", e);
    }
  }

  @Override
  public void updateTrangThaiApDung(String maKM, boolean dangApDung) {
    if (maKM == null || maKM.isBlank()) {
      throw new IllegalArgumentException("Mã khuyến mãi không được để trống");
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_TRANG_THAI_AP_DUNG)) {
      statement.setBoolean(1, dangApDung);
      statement.setString(2, maKM.trim());
      int affected = statement.executeUpdate();
      if (affected == 0) {
        throw new DataAccessException("Không tìm thấy chương trình khuyến mãi để đổi trạng thái: " + maKM);
      }
    } catch (SQLException e) {
      handleSqlException("Không thể cập nhật trạng thái áp dụng khuyến mãi", e);
    }
  }

  @Override
  public boolean existsTrungMa(String maKM) {
    if (maKM == null || maKM.isBlank()) {
      return false;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_TRUNG_MA)) {
      statement.setString(1, maKM.trim());
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() && resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra trùng mã khuyến mãi", e);
    }
  }

  @Override
  public boolean existsChongLan(String maDongSP, LocalDate ngayBatDau, LocalDate ngayKetThuc, String excludeMaKM) {
    if (maDongSP == null || maDongSP.isBlank() || ngayBatDau == null || ngayKetThuc == null) {
      return false;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(EXISTS_CHONG_LAN)) {
      statement.setString(1, maDongSP.trim());
      statement.setDate(2, Date.valueOf(ngayBatDau));
      statement.setDate(3, Date.valueOf(ngayKetThuc));
      if (excludeMaKM != null && !excludeMaKM.isBlank()) {
        statement.setString(4, excludeMaKM.trim());
        statement.setString(5, excludeMaKM.trim());
      } else {
        statement.setNull(4, java.sql.Types.VARCHAR);
        statement.setNull(5, java.sql.Types.VARCHAR);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() && resultSet.getBoolean(1);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể kiểm tra chồng lấn thời gian khuyến mãi", e);
    }
  }

  private void validateKhuyenMai(KhuyenMai km) {
    Objects.requireNonNull(km, "khuyenMai");
    if (km.maKM() == null || km.maKM().isBlank()) {
      throw new IllegalArgumentException("Mã khuyến mãi không được để trống");
    }
    if (km.tenKM() == null || km.tenKM().isBlank()) {
      throw new IllegalArgumentException("Tên khuyến mãi không được để trống");
    }
    if (km.ngayBatDau() == null || km.ngayKetThuc() == null) {
      throw new IllegalArgumentException("Thời gian khuyến mãi không được để trống");
    }
    if (km.ngayBatDau().isAfter(km.ngayKetThuc())) {
      throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
    }
    if (km.tyLeGiam() == null) {
      throw new IllegalArgumentException("Tỷ lệ giảm giá không được để trống");
    }
    if (km.tyLeGiam().compareTo(BigDecimal.ZERO) < 0 || km.tyLeGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException("Tỷ lệ giảm giá phải từ 0% đến 100%");
    }
    if (km.chiTiet().isEmpty()) {
      throw new IllegalArgumentException("Chương trình khuyến mãi phải có ít nhất một dòng sản phẩm");
    }
    for (ChiTietKhuyenMai row : km.chiTiet()) {
      if (row == null || row.dongSanPham() == null || row.dongSanPham().maDongSP() == null
          || row.dongSanPham().maDongSP().isBlank()) {
        throw new IllegalArgumentException("Dòng sản phẩm khuyến mãi không hợp lệ");
      }
      if (row.maKM() != null && !km.maKM().trim().equals(row.maKM().trim())) {
        throw new IllegalArgumentException("Mã khuyến mãi trong chi tiết không khớp với chương trình");
      }
    }
  }

  private Map<String, KhuyenMaiAccumulator> readKhuyenMai(ResultSet resultSet) throws SQLException {
    Map<String, KhuyenMaiAccumulator> rows = new LinkedHashMap<>();
    while (resultSet.next()) {
      mapKhuyenMai(resultSet, rows);
    }
    return rows;
  }

  private List<KhuyenMai> toEntities(Map<String, KhuyenMaiAccumulator> rows) {
    return rows.values().stream().map(KhuyenMaiAccumulator::toEntity).toList();
  }

  private DongSanPham mapDongSanPham(ResultSet rs, DanhMucSanPham danhMuc) throws SQLException {
    return new DongSanPham(
        rs.getString("maDongSP"),
        rs.getString("tenDongSP"),
        danhMuc,
        rs.getString("donViTinh"),
        rs.getInt("soLuongTon"),
        rs.getBigDecimal("thue"),
        rs.getBigDecimal("donGiaBanTruocThue"),
        rs.getBoolean("trangThaiKinhDoanh"),
        rs.getObject("ngayNhapKho", LocalDate.class),
        rs.getObject("ngayCapNhapCuoi", LocalDate.class));

  }

  private void mapKhuyenMai(ResultSet rs, Map<String, KhuyenMaiAccumulator> rows) throws SQLException {
    DongSanPham dsp = mapDongSanPham(rs, null);
    String maKM = rs.getString("maKM");
    KhuyenMaiAccumulator accumulator = rows.get(maKM);
    if (accumulator == null) {
      accumulator = new KhuyenMaiAccumulator(
          maKM,
          rs.getString("tenKM"),
          rs.getObject("ngayBatDau", LocalDate.class),
          rs.getObject("ngayKetThuc", LocalDate.class),
          rs.getBigDecimal("tyLeGiam"),
          rs.getString("moTa"),
          rs.getBoolean("dangApDung"));
      rows.put(maKM, accumulator);
    }
    accumulator.chiTiet.add(new ChiTietKhuyenMai(maKM, dsp));
  }

  private static final class KhuyenMaiAccumulator {
    private final String maKM;
    private final String tenKM;
    private final LocalDate ngayBatDau;
    private final LocalDate ngayKetThuc;
    private final BigDecimal tyLeGiam;
    private final String moTa;
    private final boolean dangApDung;
    private final List<ChiTietKhuyenMai> chiTiet = new ArrayList<>();

    KhuyenMaiAccumulator(String maKM, String tenKM, LocalDate ngayBatDau, LocalDate ngayKetThuc,
        BigDecimal tyLeGiam, String moTa, boolean dangApDung) {
      this.maKM = maKM;
      this.tenKM = tenKM;
      this.ngayBatDau = ngayBatDau;
      this.ngayKetThuc = ngayKetThuc;
      this.tyLeGiam = tyLeGiam;
      this.moTa = moTa;
      this.dangApDung = dangApDung;
    }

    KhuyenMai toEntity() {
      return new KhuyenMai(maKM, tenKM, ngayBatDau, ngayKetThuc, tyLeGiam, moTa, chiTiet, dangApDung);
    }
  }

  private void handleSqlException(String defaultMessage, SQLException e) {
    if ("23505".equals(e.getSQLState())) {
      throw new DataAccessException("Mã khuyến mãi đã tồn tại trong hệ thống", e);
    }
    if ("23503".equals(e.getSQLState())) {
      throw new DataAccessException("Dòng sản phẩm được gán không tồn tại trong hệ thống", e);
    }
    if ("23514".equals(e.getSQLState())) {
      throw new DataAccessException("Thông tin khuyến mãi vi phạm ràng buộc dữ liệu (tỷ lệ hoặc khoảng ngày không hợp lệ)", e);
    }
    throw new DataAccessException(defaultMessage, e);
  }
}
