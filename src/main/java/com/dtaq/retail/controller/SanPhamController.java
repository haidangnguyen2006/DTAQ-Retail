package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.ExcelImporter;
import com.dtaq.retail.util.ExcelTemplate;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý nghiệp vụ thêm máy mới vào kho và đồng bộ số lượng tồn theo UC008.
 */
public final class SanPhamController {

  private static final Logger log = LoggerFactory.getLogger(SanPhamController.class);

  private final SanPhamDao sanPhamDao;
  private final DongSanPhamDao dongSanPhamDao;
  private final NhaCungCapDao nhaCungCapDao;
  private final DataSource dataSource;

  public SanPhamController(
      SanPhamDao sanPhamDao,
      DongSanPhamDao dongSanPhamDao,
      NhaCungCapDao nhaCungCapDao,
      DataSource dataSource) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
    this.nhaCungCapDao = Objects.requireNonNull(nhaCungCapDao, "nhaCungCapDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  public KetQua<SanPham> themMay(
      String soSerial,
      String maNCC,
      String maDongSP,
      BigDecimal giaNhap,
      LocalDate ngayNhapKhoBanDau,
      TrangThaiSanPham trangThai) {

    if (soSerial == null || soSerial.isBlank()) {
      return KetQua.loi("Số serial không được để trống");
    }
    if (giaNhap == null || giaNhap.compareTo(BigDecimal.ZERO) < 0) {
      return KetQua.loi("Giá nhập không hợp lệ");
    }
    if (ngayNhapKhoBanDau != null && ngayNhapKhoBanDau.isAfter(LocalDate.now())) {
      return KetQua.loi("Ngày nhập kho ban đầu không được ở tương lai");
    }
    if (trangThai == null) {
      return KetQua.loi("Trạng thái sản phẩm không được để trống");
    }

    String serialChuanHoa = soSerial.trim();
    String maNccChuanHoa = maNCC == null ? "" : maNCC.trim();
    String maDongChuanHoa = maDongSP == null ? "" : maDongSP.trim();

    Optional<SanPham> tonTai = sanPhamDao.findById(serialChuanHoa);
    if (tonTai.isPresent()) {
      return KetQua.loi("Số serial đã tồn tại trong hệ thống");
    }

    Optional<NhaCungCap> nhaCungCap = nhaCungCapDao.findById(maNccChuanHoa);
    if (nhaCungCap.isEmpty()) {
      return KetQua.loi("Nhà cung cấp không tồn tại");
    }

    Optional<DongSanPham> dongSanPham = dongSanPhamDao.findById(maDongChuanHoa);
    if (dongSanPham.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm không tồn tại");
    }
    if (!dongSanPham.get().trangThaiKinhDoanh()) {
      return KetQua.loi("Dòng sản phẩm đang ngừng kinh doanh");
    }

    try (Connection ketNoi = dataSource.getConnection()) {
      boolean oldAutoCommit = ketNoi.getAutoCommit();
      ketNoi.setAutoCommit(false);
      try {
        SanPham sanPham = new SanPham(
            serialChuanHoa,
            nhaCungCap.get(),
            dongSanPham.get(),
            giaNhap,
            ngayNhapKhoBanDau == null ? LocalDate.now() : ngayNhapKhoBanDau,
            trangThai,
            0,
            0);
        sanPhamDao.insert(ketNoi, sanPham);
        capNhatSoLuongTon(ketNoi, maDongChuanHoa);
        ketNoi.commit();
        return KetQua.thanhCong(sanPham, "Thêm máy mới thành công");
      } catch (Exception e) {
        ketNoi.rollback();
        throw e;
      } finally {
        ketNoi.setAutoCommit(oldAutoCommit);
      }
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi lưu máy mới: {}", serialChuanHoa, e);
      return KetQua.loi(e.getMessage() == null ? "Không thể lưu máy mới" : e.getMessage());
    }
  }

  public KetQua<SanPham> capNhat(
      String soSerial,
      String maNCC,
      String maDongSP,
      BigDecimal giaNhap,
      LocalDate ngayNhapKhoBanDau,
      TrangThaiSanPham trangThai) {

    if (soSerial == null || soSerial.isBlank()) {
      return KetQua.loi("Số serial không được để trống");
    }
    if (giaNhap == null || giaNhap.compareTo(BigDecimal.ZERO) < 0) {
      return KetQua.loi("Giá nhập không hợp lệ");
    }
    if (ngayNhapKhoBanDau != null && ngayNhapKhoBanDau.isAfter(LocalDate.now())) {
      return KetQua.loi("Ngày nhập kho ban đầu không được ở tương lai");
    }
    if (trangThai == null) {
      return KetQua.loi("Trạng thái sản phẩm không được để trống");
    }

    String serialChuanHoa = soSerial.trim();
    String maNccChuanHoa = maNCC == null ? "" : maNCC.trim();
    String maDongChuanHoa = maDongSP == null ? "" : maDongSP.trim();

    Optional<SanPham> sanPhamCu = sanPhamDao.findById(serialChuanHoa);
    if (sanPhamCu.isEmpty()) {
      return KetQua.loi("Không tìm thấy sản phẩm để cập nhật");
    }

    Optional<NhaCungCap> nhaCungCap = nhaCungCapDao.findById(maNccChuanHoa);
    if (nhaCungCap.isEmpty()) {
      return KetQua.loi("Nhà cung cấp không tồn tại");
    }

    Optional<DongSanPham> dongSanPham = dongSanPhamDao.findById(maDongChuanHoa);
    if (dongSanPham.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm không tồn tại");
    }
    if (!dongSanPham.get().trangThaiKinhDoanh()) {
      return KetQua.loi("Dòng sản phẩm đang ngừng kinh doanh");
    }

    String maDongCu = sanPhamCu.get().dongSanPham().maDongSP();
    SanPham sanPhamCapNhat = new SanPham(
        serialChuanHoa,
        nhaCungCap.get(),
        dongSanPham.get(),
        giaNhap,
        ngayNhapKhoBanDau == null ? sanPhamCu.get().ngayNhapKhoBanDau() : ngayNhapKhoBanDau,
        trangThai,
        sanPhamCu.get().soLanDoiTra(),
        sanPhamCu.get().soLanSua());

    try (Connection ketNoi = dataSource.getConnection()) {
      boolean oldAutoCommit = ketNoi.getAutoCommit();
      ketNoi.setAutoCommit(false);
      try {
        sanPhamDao.update(ketNoi, sanPhamCapNhat);
        capNhatSoLuongTon(ketNoi, maDongCu);
        if (!maDongCu.equals(maDongChuanHoa)) {
          capNhatSoLuongTon(ketNoi, maDongChuanHoa);
        }
        ketNoi.commit();
        return KetQua.thanhCong(sanPhamCapNhat, "Cập nhật thông tin máy thành công");
      } catch (Exception e) {
        ketNoi.rollback();
        throw e;
      } finally {
        ketNoi.setAutoCommit(oldAutoCommit);
      }
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi cập nhật máy: {}", serialChuanHoa, e);
      return KetQua.loi(e.getMessage() == null ? "Không thể cập nhật máy" : e.getMessage());
    }
  }

  public static ExcelImporter.ImportSpec specNhapSanPham() {
    return new ExcelImporter.ImportSpec(
        ExcelTemplate.TEN_SHEET_DU_LIEU,
        List.of(
            new ExcelImporter.CotNhap("Số serial", ExcelImporter.KieuDuLieu.CHUOI, true, List.of()),
            new ExcelImporter.CotNhap("Mã nhà cung cấp", ExcelImporter.KieuDuLieu.CHUOI, true, List.of()),
            new ExcelImporter.CotNhap("Mã dòng sản phẩm", ExcelImporter.KieuDuLieu.CHUOI, true, List.of()),
            new ExcelImporter.CotNhap("Giá nhập", ExcelImporter.KieuDuLieu.SO_THAP_PHAN, true, List.of()),
            new ExcelImporter.CotNhap("Ngày nhập kho ban đầu", ExcelImporter.KieuDuLieu.NGAY, true, List.of()),
            new ExcelImporter.CotNhap("Trạng thái", ExcelImporter.KieuDuLieu.ENUM, false,
                List.of(TrangThaiSanPham.SAN_SANG_BAN.name()))),
        5000);
  }

  public KetQua<NhapKetQua> nhapDanhSach(File tep) {
    if (tep == null || !tep.isFile()) {
      return KetQua.loi("Tệp nhập không tồn tại");
    }
    ExcelImporter.ImportResult ketQuaDoc = ExcelImporter.doc(tep, specNhapSanPham());
    if (!ketQuaDoc.hopLe()) {
      String thongBao = ketQuaDoc.loi().stream()
          .limit(3)
          .map(loi -> loi.thongBao())
          .reduce((a, b) -> a + "; " + b)
          .orElse("Tệp Excel không hợp lệ");
      return KetQua.loi(thongBao);
    }

    Set<String> serialDaXem = new LinkedHashSet<>();
    List<String> maDongCapNhat = new ArrayList<>();
    try (Connection ketNoi = dataSource.getConnection()) {
      boolean oldAutoCommit = ketNoi.getAutoCommit();
      ketNoi.setAutoCommit(false);
      try {
        for (ExcelImporter.ImportRow dong : ketQuaDoc.dong()) {
          String serial = String.valueOf(dong.giaTri().get("Số serial")).trim();
          if (!serialDaXem.add(serial)) {
            throw new DataAccessException("Dòng excel trùng số serial: " + serial);
          }

          String maNcc = String.valueOf(dong.giaTri().get("Mã nhà cung cấp")).trim();
          String maDong = String.valueOf(dong.giaTri().get("Mã dòng sản phẩm")).trim();
          BigDecimal giaNhap = (BigDecimal) dong.giaTri().get("Giá nhập");
          LocalDate ngayNhap = (LocalDate) dong.giaTri().get("Ngày nhập kho ban đầu");
          String trangThaiText = String.valueOf(dong.giaTri().getOrDefault("Trạng thái", TrangThaiSanPham.SAN_SANG_BAN.name()));
          TrangThaiSanPham trangThai = trangThaiText == null || trangThaiText.isBlank()
              ? TrangThaiSanPham.SAN_SANG_BAN
              : TrangThaiSanPham.valueOf(trangThaiText.trim());

          Optional<NhaCungCap> nhaCungCap = nhaCungCapDao.findById(maNcc);
          if (nhaCungCap.isEmpty()) {
            throw new DataAccessException("Mã nhà cung cấp không tồn tại: " + maNcc);
          }
          Optional<DongSanPham> dongSanPham = dongSanPhamDao.findById(maDong);
          if (dongSanPham.isEmpty()) {
            throw new DataAccessException("Mã dòng sản phẩm không tồn tại: " + maDong);
          }
          if (!dongSanPham.get().trangThaiKinhDoanh()) {
            throw new DataAccessException("Dòng sản phẩm đang ngừng kinh doanh: " + maDong);
          }
          if (sanPhamDao.findById(serial).isPresent()) {
            throw new DataAccessException("Số serial đã tồn tại: " + serial);
          }
          SanPham sanPham = new SanPham(
              serial,
              nhaCungCap.get(),
              dongSanPham.get(),
              giaNhap,
              ngayNhap,
              trangThai,
              0,
              0);
          sanPhamDao.insert(ketNoi, sanPham);
          if (!maDongCapNhat.contains(maDong)) {
            maDongCapNhat.add(maDong);
          }
        }
        for (String maDong : maDongCapNhat) {
          capNhatSoLuongTon(ketNoi, maDong);
        }
        ketNoi.commit();
        return KetQua.thanhCong(new NhapKetQua(ketQuaDoc.dong().size()),
            "Nhập " + ketQuaDoc.dong().size() + " dòng sản phẩm thành công");
      } catch (Exception e) {
        ketNoi.rollback();
        throw e;
      } finally {
        ketNoi.setAutoCommit(oldAutoCommit);
      }
    } catch (Exception e) {
      log.error("Lỗi khi nhập danh sách sản phẩm từ Excel", e);
      return KetQua.loi(e.getMessage() == null ? "Không thể nhập danh sách sản phẩm" : e.getMessage());
    }
  }

  private void capNhatSoLuongTon(Connection ketNoi, String maDongSP) throws SQLException {
    try (PreparedStatement ps = ketNoi.prepareStatement(
        "UPDATE \"DongSanPham\" d SET \"soLuongTon\" = ("
            + "SELECT count(*) FROM \"SanPham\" s"
            + " WHERE s.\"maDongSP\" = d.\"maDongSP\""
            + " AND s.\"trangThai\" = 'SAN_SANG_BAN')"
            + " WHERE d.\"maDongSP\" = ?")) {
      ps.setString(1, maDongSP);
      ps.executeUpdate();
    }
  }

  public record NhapKetQua(int soDong) {}

  public record KetQua<T>(boolean thanhCong, String thongBao, T duLieu) {
    public static <T> KetQua<T> thanhCong(T duLieu, String thongBao) {
      return new KetQua<>(true, thongBao, duLieu);
    }

    public static <T> KetQua<T> loi(String thongBao) {
      return new KetQua<>(false, thongBao, null);
    }
  }
}
