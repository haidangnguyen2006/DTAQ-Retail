package com.dtaq.retail.dao.impl;

import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.exception.DataAccessException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Triển khai {@link HoaDonTamDao} trên tệp thuộc tính cục bộ, mỗi hóa đơn tạm là một tệp
 * {@code <maTam>.properties} trong thư mục cấu hình.
 */
public final class HoaDonTamFileDaoImpl implements HoaDonTamDao {

  private static final Logger log = LoggerFactory.getLogger(HoaDonTamFileDaoImpl.class);

  private static final String DUOI_TEP = ".properties";
  private static final String TIEN_TO = "HDT";

  private final Path thuMuc;
  private final Clock dongHo;

  /**
   * Khởi tạo DAO với thư mục lưu trữ và đồng hồ chỉ định.
   *
   * @param thuMuc thư mục lưu hóa đơn tạm; không được {@code null}
   * @param dongHo đồng hồ dùng để sinh mã và ghi thời điểm lưu; không được {@code null}
   */
  public HoaDonTamFileDaoImpl(Path thuMuc, Clock dongHo) {
    this.thuMuc = Objects.requireNonNull(thuMuc, "thuMuc");
    this.dongHo = Objects.requireNonNull(dongHo, "dongHo");
  }

  /**
   * Thư mục lưu hóa đơn tạm mặc định.
   *
   * <p>Thứ tự ưu tiên: thuộc tính hệ thống {@code dtaq.draft.dir}, biến môi trường
   * {@code DTAQ_DRAFT_DIR}, sau cùng là {@code hoa-don-tam} trong thư mục làm việc.
   *
   * @return đường dẫn thư mục lưu hóa đơn tạm
   */
  public static Path macDinh() {
    String giaTri = System.getProperty("dtaq.draft.dir");
    if (giaTri == null || giaTri.isBlank()) {
      giaTri = System.getenv("DTAQ_DRAFT_DIR");
    }
    if (giaTri == null || giaTri.isBlank()) {
      return Path.of("hoa-don-tam");
    }
    return Path.of(giaTri.trim());
  }

  @Override
  public HoaDonTam luu(HoaDonTam banTam) {
    Objects.requireNonNull(banTam, "banTam");
    if (banTam.maNV() == null || banTam.maNV().isBlank()) {
      throw new IllegalArgumentException("Mã nhân viên không được để trống");
    }
    if (banTam.dongHang() == null) {
      throw new IllegalArgumentException("Danh sách dòng hàng không được để trống");
    }
    for (DongTam dong : banTam.dongHang()) {
      if (dong == null || dong.maDongSP() == null || dong.maDongSP().isBlank()) {
        throw new IllegalArgumentException("Hóa đơn tạm thiếu mã dòng sản phẩm");
      }
    }
    String maTam = (banTam.maTam() == null || banTam.maTam().isBlank())
        ? sinhMa()
        : banTam.maTam();
    HoaDonTam daLuu = new HoaDonTam(
        maTam,
        banTam.maNV(),
        banTam.tenNhanVien(),
        banTam.maKH(),
        banTam.tenKhachHang(),
        banTam.sdtKhachHang(),
        LocalDateTime.now(dongHo).withNano(0),
        List.copyOf(banTam.dongHang()));
    ghi(daLuu);
    return daLuu;
  }

  @Override
  public List<HoaDonTam> findByNhanVien(String maNV) {
    if (maNV == null || !Files.isDirectory(thuMuc)) {
      return List.of();
    }
    List<HoaDonTam> ketQua = new ArrayList<>();
    try (Stream<Path> luong = Files.list(thuMuc)) {
      for (Path tep : luong.filter(HoaDonTamFileDaoImpl::laTepHoaDon).toList()) {
        try {
          HoaDonTam hoaDon = doc(tep);
          if (maNV.equals(hoaDon.maNV())) {
            ketQua.add(hoaDon);
          }
        } catch (IOException | RuntimeException e) {
          log.warn("Bỏ qua tệp hóa đơn tạm không đọc được {}: {}",
              tep.getFileName(), e.getMessage());
        }
      }
    } catch (IOException e) {
      throw new DataAccessException("Không thể đọc thư mục hóa đơn tạm", e);
    }
    ketQua.sort(Comparator
        .comparing(HoaDonTam::luuLuc)
        .thenComparing(HoaDonTam::maTam)
        .reversed());
    return List.copyOf(ketQua);
  }

  @Override
  public Optional<HoaDonTam> findById(String maTam) {
    if (maTam == null || maTam.isBlank()) {
      return Optional.empty();
    }
    Path tep = thuMuc.resolve(maTam + DUOI_TEP);
    if (!Files.exists(tep)) {
      return Optional.empty();
    }
    try {
      return Optional.of(doc(tep));
    } catch (IOException | RuntimeException e) {
      throw new DataAccessException("Không thể đọc hóa đơn tạm " + maTam, e);
    }
  }

  @Override
  public void xoa(String maTam) {
    if (maTam == null || maTam.isBlank()) {
      return;
    }
    try {
      Files.deleteIfExists(thuMuc.resolve(maTam + DUOI_TEP));
    } catch (IOException e) {
      throw new DataAccessException("Không thể xóa hóa đơn tạm " + maTam, e);
    }
  }

  /**
   * Sinh mã hóa đơn tạm theo quy ước {@code HDT-YYYY-NNNNNN}.
   *
   * <p>Mã được sinh ở tầng ứng dụng bằng cách quét các tệp hiện có của năm hiện tại và
   * lấy số thứ tự lớn nhất cộng một; không dùng sequence của cơ sở dữ liệu.
   */
  private String sinhMa() {
    int nam = LocalDateTime.now(dongHo).getYear();
    String tienTo = TIEN_TO + "-" + nam + "-";
    long soLonNhat = 0;
    if (Files.isDirectory(thuMuc)) {
      try (Stream<Path> luong = Files.list(thuMuc)) {
        for (Path tep : luong.filter(HoaDonTamFileDaoImpl::laTepHoaDon).toList()) {
          String ten = tep.getFileName().toString();
          if (!ten.startsWith(tienTo)) {
            continue;
          }
          String phanSo = ten.substring(tienTo.length(), ten.length() - DUOI_TEP.length());
          try {
            soLonNhat = Math.max(soLonNhat, Long.parseLong(phanSo));
          } catch (NumberFormatException ignored) {
            // Tên tệp không theo quy ước số thứ tự; bỏ qua khi tính mã kế tiếp.
          }
        }
      } catch (IOException e) {
        throw new DataAccessException("Không thể đọc thư mục hóa đơn tạm", e);
      }
    }
    return String.format("%s-%d-%06d", TIEN_TO, nam, soLonNhat + 1);
  }

  private void ghi(HoaDonTam hoaDon) {
    Path tepDich = thuMuc.resolve(hoaDon.maTam() + DUOI_TEP);
    try {
      Files.createDirectories(thuMuc);
      Properties thuocTinh = new Properties();
      dat(thuocTinh, "maTam", hoaDon.maTam());
      dat(thuocTinh, "maNV", hoaDon.maNV());
      dat(thuocTinh, "tenNhanVien", hoaDon.tenNhanVien());
      dat(thuocTinh, "maKH", hoaDon.maKH());
      dat(thuocTinh, "tenKhachHang", hoaDon.tenKhachHang());
      dat(thuocTinh, "sdtKhachHang", hoaDon.sdtKhachHang());
      dat(thuocTinh, "luuLuc", hoaDon.luuLuc().toString());
      dat(thuocTinh, "soDong", Integer.toString(hoaDon.dongHang().size()));
      int chiSo = 0;
      for (DongTam dong : hoaDon.dongHang()) {
        String goc = "dong." + chiSo + ".";
        dat(thuocTinh, goc + "maDongSP", dong.maDongSP());
        dat(thuocTinh, goc + "tenDongSP", dong.tenDongSP());
        dat(thuocTinh, goc + "soLuong", Integer.toString(dong.soLuong()));
        dat(thuocTinh, goc + "donGiaTruocThue", chuoiSo(dong.donGiaTruocThue()));
        dat(thuocTinh, goc + "thueSuat", chuoiSo(dong.thueSuat()));
        chiSo++;
      }
      Path tepTam = Files.createTempFile(thuMuc, hoaDon.maTam(), ".tmp");
      try (BufferedWriter writer = Files.newBufferedWriter(tepTam, StandardCharsets.UTF_8)) {
        thuocTinh.store(writer, null);
      }
      Files.move(tepTam, tepDich,
          StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new DataAccessException("Không thể lưu hóa đơn tạm " + hoaDon.maTam(), e);
    }
  }

  private static HoaDonTam doc(Path tep) throws IOException {
    Properties thuocTinh = new Properties();
    try (Reader reader = Files.newBufferedReader(tep, StandardCharsets.UTF_8)) {
      thuocTinh.load(reader);
    }
    String soDong = thuocTinh.getProperty("soDong", "0");
    int soLuongDong = Integer.parseInt(soDong.trim());
    List<DongTam> dongHang = new ArrayList<>(soLuongDong);
    for (int chiSo = 0; chiSo < soLuongDong; chiSo++) {
      String goc = "dong." + chiSo + ".";
      String maDongSP = thuocTinh.getProperty(goc + "maDongSP");
      if (maDongSP == null || maDongSP.isBlank()) {
        throw new IllegalStateException("Hóa đơn tạm thiếu mã dòng sản phẩm: " + tep);
      }
      dongHang.add(new DongTam(
          maDongSP,
          thuocTinh.getProperty(goc + "tenDongSP"),
          Integer.parseInt(thuocTinh.getProperty(goc + "soLuong", "0").trim()),
          so(thuocTinh.getProperty(goc + "donGiaTruocThue")),
          so(thuocTinh.getProperty(goc + "thueSuat"))));
    }
    String luuLuc = thuocTinh.getProperty("luuLuc");
    if (luuLuc == null || luuLuc.isBlank()) {
      throw new IllegalStateException("Tệp hóa đơn tạm thiếu thời điểm lưu: " + tep);
    }
    return new HoaDonTam(
        thuocTinh.getProperty("maTam"),
        thuocTinh.getProperty("maNV"),
        thuocTinh.getProperty("tenNhanVien"),
        thuocTinh.getProperty("maKH"),
        thuocTinh.getProperty("tenKhachHang"),
        thuocTinh.getProperty("sdtKhachHang"),
        LocalDateTime.parse(luuLuc.trim()),
        List.copyOf(dongHang));
  }

  private static boolean laTepHoaDon(Path tep) {
    return Files.isRegularFile(tep)
        && tep.getFileName().toString().endsWith(DUOI_TEP);
  }

  private static void dat(Properties thuocTinh, String khoa, String giaTri) {
    if (giaTri != null) {
      thuocTinh.setProperty(khoa, giaTri);
    }
  }

  private static String chuoiSo(BigDecimal giaTri) {
    return giaTri == null ? null : giaTri.toPlainString();
  }

  private static BigDecimal so(String giaTri) {
    return giaTri == null ? null : new BigDecimal(giaTri);
  }
}
