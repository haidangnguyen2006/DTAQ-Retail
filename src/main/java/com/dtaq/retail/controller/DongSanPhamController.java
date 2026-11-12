package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.ExcelImporter;
import com.dtaq.retail.util.ExcelTemplate;
import com.dtaq.retail.util.MaGenerator;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nghiệp vụ quản lý dòng sản phẩm theo UC009.
 */
public final class DongSanPhamController {

  private static final Logger log = LoggerFactory.getLogger(DongSanPhamController.class);

  private final DongSanPhamDao dongSanPhamDao;
  private final DanhMucSanPhamDao danhMucSanPhamDao;
  private final DataSource dataSource;

  public DongSanPhamController(
      DongSanPhamDao dongSanPhamDao,
      DanhMucSanPhamDao danhMucSanPhamDao,
      DataSource dataSource) {
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
    this.danhMucSanPhamDao = Objects.requireNonNull(danhMucSanPhamDao, "danhMucSanPhamDao");
    this.dataSource = dataSource;
  }

  public List<DongSanPham> layDanhSach() {
    return dongSanPhamDao.findAll();
  }

  public Optional<DongSanPham> timTheoMa(String maDongSP) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return Optional.empty();
    }
    return dongSanPhamDao.findById(maDongSP.trim());
  }

  public KetQua<DongSanPham> themMoi(
      String tenDongSP,
      String maDanhMuc,
      String donViTinh,
      BigDecimal donGiaBanTruocThue,
      BigDecimal thue,
      boolean trangThaiKinhDoanh) {
    String ten = normalizeText(tenDongSP);
    String dv = normalizeDonViTinh(donViTinh);
    if (ten.isEmpty()) {
      return KetQua.loi("Tên dòng sản phẩm không được để trống");
    }
    if (dv == null || dv.isEmpty()) {
      return KetQua.loi("Đơn vị tính không hợp lệ. Chỉ chấp nhận Chiếc hoặc Cái");
    }
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return KetQua.loi("Danh mục sản phẩm không được để trống");
    }
    Optional<DanhMucSanPham> danhMuc = danhMucSanPhamDao.findById(maDanhMuc.trim());
    if (danhMuc.isEmpty()) {
      return KetQua.loi("Danh mục sản phẩm không tồn tại");
    }
    if (donGiaBanTruocThue == null || donGiaBanTruocThue.compareTo(BigDecimal.ZERO) < 0) {
      return KetQua.loi("Giá bán trước thuế không hợp lệ");
    }
    if (thue == null || thue.compareTo(BigDecimal.ZERO) < 0 || thue.compareTo(BigDecimal.valueOf(100)) > 0) {
      return KetQua.loi("Thuế suất không hợp lệ, phải nằm trong khoảng 0% đến 100%");
    }

    try {
      String maDong = sinhMaDongMoi();
      DongSanPham dongSanPham = new DongSanPham(
          maDong,
          ten,
          danhMuc.get(),
          dv,
          0,
          thue,
          donGiaBanTruocThue,
          trangThaiKinhDoanh,
          LocalDate.now(),
          LocalDate.now());

      if (dataSource != null) {
        try (Connection connection = dataSource.getConnection()) {
          connection.setAutoCommit(false);
          try {
            dongSanPhamDao.insert(connection, dongSanPham);
            connection.commit();
          } catch (Exception ex) {
            connection.rollback();
            throw ex;
          } finally {
            connection.setAutoCommit(true);
          }
        }
      } else {
        dongSanPhamDao.insert(null, dongSanPham);
      }
      return KetQua.thanhCong(dongSanPham, "Thêm dòng sản phẩm thành công");
    } catch (Exception e) {
      log.warn("Không thể thêm dòng sản phẩm mới: {}", e.getMessage(), e);
      return KetQua.loi(e.getMessage() == null ? "Không thể thêm dòng sản phẩm" : e.getMessage());
    }
  }

  public KetQua<DongSanPham> themMoi(
      String tenDongSP,
      String maDanhMuc,
      String donViTinh,
      BigDecimal donGiaBanTruocThue,
      BigDecimal thue) {
    return themMoi(tenDongSP, maDanhMuc, donViTinh, donGiaBanTruocThue, thue, true);
  }

  public KetQua<DongSanPham> themDong(
      String tenDongSP,
      String maDanhMuc,
      String donViTinh,
      BigDecimal donGiaBanTruocThue,
      BigDecimal thue,
      boolean trangThaiKinhDoanh) {
    return themMoi(tenDongSP, maDanhMuc, donViTinh, donGiaBanTruocThue, thue, trangThaiKinhDoanh);
  }

  public KetQua<DongSanPham> capNhat(
      String maDongSP,
      String tenDongSP,
      String maDanhMuc,
      String donViTinh,
      BigDecimal donGiaBanTruocThue,
      BigDecimal thue,
      boolean trangThaiKinhDoanh) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return KetQua.loi("Mã dòng sản phẩm không được để trống");
    }
    Optional<DongSanPham> cu = dongSanPhamDao.findById(maDongSP.trim());
    if (cu.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm không tồn tại");
    }
    DongSanPham dongCu = cu.get();
    String ten = normalizeText(tenDongSP);
    String dv = normalizeDonViTinh(donViTinh);
    if (ten.isEmpty()) {
      return KetQua.loi("Tên dòng sản phẩm không được để trống");
    }
    if (dv == null || dv.isEmpty()) {
      return KetQua.loi("Đơn vị tính không hợp lệ. Chỉ chấp nhận Chiếc hoặc Cái");
    }
    if (donGiaBanTruocThue == null || donGiaBanTruocThue.compareTo(BigDecimal.ZERO) < 0) {
      return KetQua.loi("Giá bán trước thuế không hợp lệ");
    }
    if (thue == null || thue.compareTo(BigDecimal.ZERO) < 0 || thue.compareTo(BigDecimal.valueOf(100)) > 0) {
      return KetQua.loi("Thuế suất không hợp lệ, phải nằm trong khoảng 0% đến 100%");
    }
    Optional<DanhMucSanPham> danhMuc = danhMucSanPhamDao.findById(maDanhMuc == null ? dongCu.danhMucSanPham().maDanhMuc() : maDanhMuc.trim());
    if (danhMuc.isEmpty()) {
      return KetQua.loi("Danh mục sản phẩm không tồn tại");
    }

    DongSanPham capNhat = new DongSanPham(
        maDongSP.trim(),
        ten,
        danhMuc.get(),
        dv,
        dongCu.soLuongTon(),
        thue,
        donGiaBanTruocThue,
        trangThaiKinhDoanh,
        dongCu.ngayNhapKho(),
        LocalDate.now());
    try {
      dongSanPhamDao.update(capNhat);
      return KetQua.thanhCong(capNhat, "Cập nhật dòng sản phẩm thành công");
    } catch (Exception e) {
      log.warn("Không thể cập nhật dòng sản phẩm {}: {}", maDongSP, e.getMessage(), e);
      return KetQua.loi(e.getMessage() == null ? "Không thể cập nhật dòng sản phẩm" : e.getMessage());
    }
  }

  /**
   * Ngừng kinh doanh một dòng sản phẩm (UC009).
   *
   * <p>Chỉ cập nhật trạng thái kinh doanh; không thay đổi giá bán, thuế suất hay số lượng tồn.
   *
   * @param maDongSP mã dòng sản phẩm cần ngừng kinh doanh
   * @return kết quả kèm dòng sản phẩm sau khi cập nhật
   */
  public KetQua<DongSanPham> ngungKinhDoanh(String maDongSP) {
    return datTrangThaiKinhDoanh(maDongSP, false);
  }

  /**
   * Cho một dòng sản phẩm đã ngừng kinh doanh trở lại kinh doanh (UC009).
   *
   * <p>Chỉ cập nhật trạng thái kinh doanh; không thay đổi giá bán, thuế suất hay số lượng tồn.
   *
   * @param maDongSP mã dòng sản phẩm cần kinh doanh lại
   * @return kết quả kèm dòng sản phẩm sau khi cập nhật
   */
  public KetQua<DongSanPham> kinhDoanhLai(String maDongSP) {
    return datTrangThaiKinhDoanh(maDongSP, true);
  }

  private KetQua<DongSanPham> datTrangThaiKinhDoanh(String maDongSP, boolean trangThai) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return KetQua.loi("Mã dòng sản phẩm không được để trống");
    }
    Optional<DongSanPham> dong = dongSanPhamDao.findById(maDongSP.trim());
    if (dong.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm không tồn tại");
    }
    DongSanPham hienTai = dong.get();
    if (hienTai.trangThaiKinhDoanh() == trangThai) {
      return KetQua.thanhCong(
          hienTai,
          trangThai ? "Dòng sản phẩm đang kinh doanh" : "Dòng sản phẩm đã ngừng kinh doanh");
    }
    try {
      dongSanPhamDao.updateTrangThaiKinhDoanh(hienTai.maDongSP(), trangThai);
    } catch (DataAccessException e) {
      log.warn("Không thể cập nhật trạng thái kinh doanh của dòng sản phẩm {}: {}",
          hienTai.maDongSP(), e.getMessage(), e);
      return KetQua.loi("Không thể cập nhật trạng thái kinh doanh của dòng sản phẩm");
    }
    DongSanPham capNhat = new DongSanPham(
        hienTai.maDongSP(),
        hienTai.tenDongSP(),
        hienTai.danhMucSanPham(),
        hienTai.donViTinh(),
        hienTai.soLuongTon(),
        hienTai.thue(),
        hienTai.donGiaBanTruocThue(),
        trangThai,
        hienTai.ngayNhapKho(),
        hienTai.ngayCapNhapCuoi());
    return KetQua.thanhCong(
        capNhat,
        trangThai ? "Kinh doanh lại dòng sản phẩm thành công" : "Ngừng kinh doanh dòng sản phẩm thành công");
  }

  public static ExcelImporter.ImportSpec specNhapDongSanPham() {
    return new ExcelImporter.ImportSpec(
        ExcelTemplate.TEN_SHEET_DU_LIEU,
        List.of(
            new ExcelImporter.CotNhap("Tên dòng sản phẩm", ExcelImporter.KieuDuLieu.CHUOI, true, List.of()),
            new ExcelImporter.CotNhap("Mã danh mục", ExcelImporter.KieuDuLieu.CHUOI, true, List.of()),
            new ExcelImporter.CotNhap("Đơn vị tính", ExcelImporter.KieuDuLieu.ENUM, true, List.of("Chiếc", "Cái")),
            new ExcelImporter.CotNhap("Giá bán trước thuế", ExcelImporter.KieuDuLieu.SO_THAP_PHAN, true, List.of()),
            new ExcelImporter.CotNhap("Thuế suất (%)", ExcelImporter.KieuDuLieu.SO_THAP_PHAN, true, List.of()),
            new ExcelImporter.CotNhap("Đang kinh doanh", ExcelImporter.KieuDuLieu.LOGIC, false, List.of())),
        5000);
  }

  public KetQua<NhapKetQua> nhapDanhSach(File tep) {
    if (tep == null || !tep.isFile()) {
      return KetQua.loi("Tệp nhập không tồn tại");
    }
    ExcelImporter.ImportResult ketQuaDoc = ExcelImporter.doc(tep, specNhapDongSanPham());
    if (!ketQuaDoc.hopLe()) {
      String thongBao = ketQuaDoc.loi().stream()
          .limit(3)
          .map(loi -> loi.thongBao())
          .reduce((a, b) -> a + "; " + b)
          .orElse("Tệp Excel không hợp lệ");
      return KetQua.loi(thongBao);
    }

    Set<String> tenDaXem = new HashSet<>();
    try (Connection ketNoi = dataSource.getConnection()) {
      boolean oldAutoCommit = ketNoi.getAutoCommit();
      ketNoi.setAutoCommit(false);
      try {
        for (ExcelImporter.ImportRow dong : ketQuaDoc.dong()) {
          String ten = String.valueOf(dong.giaTri().get("Tên dòng sản phẩm")).trim();
          String tenChuanHoa = normalizeText(ten).toLowerCase(Locale.ROOT);
          if (!tenDaXem.add(tenChuanHoa)) {
            throw new DataAccessException("Tên dòng sản phẩm trùng trong tệp: " + ten);
          }
          String maDanhMuc = String.valueOf(dong.giaTri().get("Mã danh mục")).trim();
          String donViTinh = String.valueOf(dong.giaTri().get("Đơn vị tính")).trim();
          BigDecimal giaBan = (BigDecimal) dong.giaTri().get("Giá bán trước thuế");
          BigDecimal thue = (BigDecimal) dong.giaTri().get("Thuế suất (%)");
          boolean kinhDoanh = Boolean.TRUE.equals(dong.giaTri().getOrDefault("Đang kinh doanh", true));

          Optional<DanhMucSanPham> danhMuc = danhMucSanPhamDao.findById(maDanhMuc);
          if (danhMuc.isEmpty()) {
            throw new DataAccessException("Mã danh mục không tồn tại: " + maDanhMuc);
          }
          String dv = normalizeDonViTinh(donViTinh);
          if (dv == null) {
            throw new DataAccessException("Đơn vị tính không hợp lệ. Chỉ chấp nhận Chiếc hoặc Cái");
          }
          if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) < 0) {
            throw new DataAccessException("Giá bán trước thuế không hợp lệ");
          }
          if (thue == null || thue.compareTo(BigDecimal.ZERO) < 0 || thue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DataAccessException("Thuế suất không hợp lệ, phải nằm trong khoảng 0% đến 100%");
          }

          String maDong = MaGenerator.next(ketNoi, "DongSanPham", "maDongSP", "DSP");
          DongSanPham dongSanPham = new DongSanPham(
              maDong,
              ten,
              danhMuc.get(),
              dv,
              0,
              thue,
              giaBan,
              kinhDoanh,
              LocalDate.now(),
              LocalDate.now());
          dongSanPhamDao.insert(ketNoi, dongSanPham);
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
      log.warn("Không thể nhập danh sách dòng sản phẩm từ Excel: {}", e.getMessage(), e);
      return KetQua.loi(e.getMessage() == null ? "Không thể nhập danh sách dòng sản phẩm" : e.getMessage());
    }
  }

  private String sinhMaDongMoi() {
    if (dataSource != null) {
      try (Connection connection = dataSource.getConnection()) {
        return MaGenerator.next(connection, "DongSanPham", "maDongSP", "DSP");
      } catch (SQLException e) {
        throw new IllegalStateException("Không thể sinh mã dòng sản phẩm", e);
      }
    }

    int year = LocalDate.now().getYear();
    long next = 1L;
    while (true) {
      String ma = String.format("DSP-%d-%06d", year, next);
      if (dongSanPhamDao.findById(ma).isEmpty()) {
        return ma;
      }
      next++;
    }
  }

  public record NhapKetQua(int soDong) {}

  public static String normalizeDonViTinh(String value) {
    if (value == null) {
      return null;
    }
    String normalized = stripAccents(value.trim());
    if (normalized.isEmpty()) {
      return "";
    }
    String lowered = normalized.toLowerCase(Locale.ROOT);
    if ("chiec".equals(lowered)) {
      return "Chiếc";
    }
    if ("cai".equals(lowered)) {
      return "Cái";
    }
    return null;
  }

  private static String stripAccents(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.replaceAll("\\p{M}", "");
  }

  private static String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  public record KetQua<T>(boolean thanhCong, String thongBao, T duLieu) {
    public static <T> KetQua<T> thanhCong(T duLieu, String thongBao) {
      return new KetQua<>(true, thongBao, duLieu);
    }

    public static <T> KetQua<T> loi(String thongBao) {
      return new KetQua<>(false, thongBao, null);
    }
  }
}
