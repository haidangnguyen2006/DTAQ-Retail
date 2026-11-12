package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.dao.KhuyenMaiDao.DongApDung;
import com.dtaq.retail.entity.ChiTietKhuyenMai;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.service.KhuyenMaiExcel;
import com.dtaq.retail.service.KhuyenMaiExcel.Nhom;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.ExcelImporter.ImportError;
import com.dtaq.retail.util.ExcelImporter.ImportResult;
import com.dtaq.retail.util.MaGenerator;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý logic nghiệp vụ quản lý chương trình khuyến mãi (UC012),
 * tra cứu khuyến mãi hiệu lực cho nghiệp vụ lập hóa đơn bán lẻ (UC003).
 */
public final class KhuyenMaiController {

  private static final Logger log = LoggerFactory.getLogger(KhuyenMaiController.class);
  private static final String LOI_QUYEN = "Chỉ Quản lý được quản lý chương trình khuyến mãi";

  private final KhuyenMaiDao khuyenMaiDao;
  private final DongSanPhamDao dongSanPhamDao;
  private final DanhMucSanPhamDao danhMucDao;
  private final DataSource dataSource;

  /**
   * Khởi tạo bộ điều khiển khuyến mãi.
   *
   * @param khuyenMaiDao lớp truy cập dữ liệu chương trình khuyến mãi; không được {@code null}
   * @param dongSanPhamDao lớp truy cập dữ liệu dòng sản phẩm; không được {@code null}
   * @param danhMucDao lớp truy cập dữ liệu danh mục; không được {@code null}
   * @param dataSource nguồn kết nối CSDL phục vụ sinh mã định danh tự động; không được {@code null}
   */
  public KhuyenMaiController(
      KhuyenMaiDao khuyenMaiDao,
      DongSanPhamDao dongSanPhamDao,
      DanhMucSanPhamDao danhMucDao,
      DataSource dataSource) {
    this.khuyenMaiDao = Objects.requireNonNull(khuyenMaiDao, "khuyenMaiDao");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
    this.danhMucDao = Objects.requireNonNull(danhMucDao, "danhMucDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Lấy toàn bộ danh sách chương trình khuyến mãi.
   *
   * @return danh sách khuyến mãi, không bao giờ {@code null}
   */
  public List<KhuyenMai> layDanhSach() {
    try {
      return khuyenMaiDao.findAll();
    } catch (DataAccessException e) {
      log.error("Lỗi khi tải danh sách chương trình khuyến mãi", e);
      return Collections.emptyList();
    }
  }

  /** Lấy các dòng sản phẩm để chọn bộ lọc và gắn khuyến mãi. */
  public List<DongSanPham> layDanhSachDongSanPham() {
    try {
      return dongSanPhamDao.findAll();
    } catch (DataAccessException e) {
      log.error("Lỗi khi tải danh sách dòng sản phẩm cho khuyến mãi", e);
      return Collections.emptyList();
    }
  }

  public List<DanhMucSanPham> layDanhSachDanhMuc() {
    yeuCauQuanLy();
    return danhMucDao.findAll();
  }

  /**
   * Lấy danh sách khuyến mãi của một dòng sản phẩm cụ thể.
   *
   * @param maDongSP mã dòng sản phẩm cần lọc
   * @return danh sách khuyến mãi tương ứng
   */
  public List<KhuyenMai> layDanhSachTheoDong(String maDongSP) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return khuyenMaiDao.findByDongSanPham(maDongSP.trim());
    } catch (DataAccessException e) {
      log.error("Lỗi khi tải danh sách khuyến mãi cho dòng sản phẩm: {}", maDongSP, e);
      return Collections.emptyList();
    }
  }

  /**
   * Tìm chương trình khuyến mãi theo mã định danh {@code maKM}.
   *
   * @param maKM mã khuyến mãi cần tìm
   * @return đối tượng khuyến mãi nếu có
   */
  public Optional<KhuyenMai> timTheoMa(String maKM) {
    if (maKM == null || maKM.isBlank()) {
      return Optional.empty();
    }
    try {
      return khuyenMaiDao.findById(maKM.trim());
    } catch (DataAccessException e) {
      log.error("Lỗi khi tìm khuyến mãi theo mã: {}", maKM, e);
      return Optional.empty();
    }
  }

  /**
   * Tra cứu chương trình khuyến mãi đang áp dụng và còn hiệu lực cho một dòng sản phẩm tại một ngày xác định.
   * Đây là phương thức cốt lõi được sử dụng bởi UC003 (bán hàng - Đăng).
   *
   * @param maDongSP mã dòng sản phẩm
   * @param ngay ngày kiểm tra (thường là ngày lập hóa đơn)
   * @return khuyến mãi hiệu lực nếu có
   */
  public Optional<KhuyenMai> timKhuyenMaiHieuLuc(String maDongSP, LocalDate ngay) {
    if (maDongSP == null || maDongSP.isBlank() || ngay == null) {
      return Optional.empty();
    }
    try {
      return khuyenMaiDao.findHieuLuc(maDongSP.trim(), ngay);
    } catch (DataAccessException e) {
      log.error("Lỗi khi tra cứu khuyến mãi hiệu lực cho dòng {} tại ngày {}", maDongSP, ngay, e);
      return Optional.empty();
    }
  }

  /**
   * Thêm mới một chương trình khuyến mãi vào hệ thống (UC012).
   *
   * @param tenKM tên chương trình khuyến mãi; không được để trống
   * @param moTa mô tả chương trình; có thể để trống
   * @param tyLeGiam tỷ lệ giảm giá từ 0% đến 100%
   * @param ngayBatDau ngày bắt đầu có hiệu lực
   * @param ngayKetThuc ngày kết thúc hiệu lực
   * @param maDongSP mã dòng sản phẩm áp dụng; phải tồn tại trong CSDL
   * @param dangApDung trạng thái sử dụng ban đầu (đang áp dụng hay tạm dừng)
   * @return kết quả thao tác kèm đối tượng khuyến mãi vừa thêm nếu thành công
   */
  public KetQua<KhuyenMai> themMoi(
      String tenKM,
      String moTa,
      BigDecimal tyLeGiam,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      String maDongSP,
      boolean dangApDung) {
    return themMoiTheoDieuKien(tenKM, moTa, tyLeGiam, ngayBatDau, ngayKetThuc,
        maDongSP, List.of(), null, dangApDung);
  }

  /** Tìm các dòng áp dụng cho phần xem trước. Lúc lưu hệ thống xét lại cùng điều kiện. */
  public KetQua<List<DongApDung>> xemTruocDongApDung(
      String maDongSP, List<String> maDanhMucs, Integer soNgayTonToiThieu) {
    yeuCauQuanLy();
    String maDong = maDongSP == null || maDongSP.isBlank() ? null : maDongSP.trim();
    List<String> danhMucs = maDanhMucs == null ? List.of() : maDanhMucs.stream()
        .filter(Objects::nonNull).map(String::trim).filter(ma -> !ma.isEmpty()).distinct().toList();
    if (maDong == null && danhMucs.isEmpty() && soNgayTonToiThieu == null) {
      return KetQua.loi("Chọn ít nhất một đối tượng hoặc điều kiện");
    }
    if (maDong != null && !danhMucs.isEmpty()) {
      return KetQua.loi("Không thể chọn đồng thời dòng sản phẩm và danh mục");
    }
    if (soNgayTonToiThieu != null && soNgayTonToiThieu < 0) {
      return KetQua.loi("Số ngày tồn tối thiểu không được âm");
    }

    try {
      if (maDong != null && dongSanPhamDao.findById(maDong).isEmpty()) {
        return KetQua.loi("Dòng sản phẩm áp dụng không tồn tại trong hệ thống");
      }
      for (String ma : danhMucs) {
        if (danhMucDao.findById(ma).isEmpty()) {
          return KetQua.loi("Danh mục áp dụng không tồn tại: " + ma);
        }
      }
      LocalDate ngayGioiHan = soNgayTonToiThieu == null
          ? null : LocalDate.now().minusDays(soNgayTonToiThieu);
      List<DongApDung> cacDong = khuyenMaiDao.findDongTheoDieuKien(maDong, danhMucs, ngayGioiHan);
      if (cacDong.isEmpty()) {
        return KetQua.loi("Không có dòng sản phẩm nào thỏa điều kiện");
      }
      return KetQua.thanhCong(cacDong, "Số dòng áp dụng: " + cacDong.size());
    } catch (DataAccessException e) {
      log.error("Lỗi khi tìm dòng áp dụng khuyến mãi", e);
      return KetQua.loi("Không thể tìm dòng sản phẩm theo điều kiện khuyến mãi");
    }
  }

  /** Tạo một chương trình khuyến mãi cho tất cả dòng thỏa điều kiện. */
  public KetQua<KhuyenMai> themMoiTheoDieuKien(
      String tenKM, String moTa, BigDecimal tyLeGiam, LocalDate ngayBatDau, LocalDate ngayKetThuc,
      String maDongSP, List<String> maDanhMucs, Integer soNgayTonToiThieu, boolean dangApDung) {
    return themMoiTheoDieuKien(tenKM, moTa, tyLeGiam, ngayBatDau, ngayKetThuc,
        maDongSP, maDanhMucs, soNgayTonToiThieu, dangApDung, null);
  }

  public KetQua<KhuyenMai> themMoiTheoDieuKien(
      String tenKM, String moTa, BigDecimal tyLeGiam, LocalDate ngayBatDau, LocalDate ngayKetThuc,
      String maDongSP, List<String> maDanhMucs, Integer soNgayTonToiThieu, boolean dangApDung,
      List<String> maDongDaXacNhan) {
    yeuCauQuanLy();
    String loiNhapLieu = validateThongTin(tenKM, tyLeGiam, ngayBatDau, ngayKetThuc);
    if (loiNhapLieu != null) {
      return KetQua.loi(loiNhapLieu);
    }
    var xemTruoc = xemTruocDongApDung(maDongSP, maDanhMucs, soNgayTonToiThieu);
    if (!xemTruoc.thanhCong()) {
      return KetQua.loi(xemTruoc.thongBao());
    }
    List<DongSanPham> cacDong = xemTruoc.duLieu().stream().map(DongApDung::dong).toList();
    if (maDongDaXacNhan != null
        && !maDongDaXacNhan.equals(cacDong.stream().map(DongSanPham::maDongSP).toList())) {
      return KetQua.loi("Danh sách dòng áp dụng đã thay đổi. Vui lòng xem trước lại.");
    }
    try {
      if (dangApDung) {
        for (DongSanPham dong : cacDong) {
          if (khuyenMaiDao.existsChongLan(dong.maDongSP(), ngayBatDau, ngayKetThuc, null)) {
            return KetQua.loi("Dòng sản phẩm đã có khuyến mãi trong cùng thời gian: " + dong.maDongSP());
          }
        }
      }
      try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        try {
          String maKM = MaGenerator.next(conn, "KhuyenMai", "maKM", "KM");
          List<ChiTietKhuyenMai> chiTiet = cacDong.stream()
              .map(dong -> new ChiTietKhuyenMai(maKM, dong)).toList();
          KhuyenMai km = new KhuyenMai(maKM, tenKM.trim(), ngayBatDau, ngayKetThuc, tyLeGiam,
              moTa == null || moTa.isBlank() ? null : moTa.trim(), chiTiet, dangApDung);
          khuyenMaiDao.insert(conn, km);
          conn.commit();
          return KetQua.thanhCong(km, "Đã thêm chương trình cho " + chiTiet.size() + " dòng sản phẩm");
        } catch (SQLException | RuntimeException e) {
          try {
            conn.rollback();
          } catch (SQLException rollbackError) {
            e.addSuppressed(rollbackError);
          }
          throw e;
        }
      }
    } catch (DataAccessException e) {
      log.error("Lỗi khi tạo chương trình khuyến mãi", e);
      return KetQua.loi(e.getMessage());
    } catch (SQLException e) {
      log.error("Lỗi khi tạo chương trình khuyến mãi", e);
      return KetQua.loi("Thêm khuyến mãi thất bại");
    }
  }

  /**
   * Cập nhật thông tin chương trình khuyến mãi.
   *
   * @param maKM mã khuyến mãi cần cập nhật; không được để trống
   * @param tenKM tên chương trình khuyến mãi
   * @param moTa mô tả chương trình
   * @param tyLeGiam tỷ lệ giảm giá
   * @param ngayBatDau ngày bắt đầu hiệu lực
   * @param ngayKetThuc ngày kết thúc hiệu lực
   * @param maDongSP mã dòng sản phẩm áp dụng
   * @param dangApDung trạng thái áp dụng
   * @return kết quả thao tác kèm khuyến mãi đã cập nhật
   */
  public KetQua<KhuyenMai> capNhat(
      String maKM,
      String tenKM,
      String moTa,
      BigDecimal tyLeGiam,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      String maDongSP,
      boolean dangApDung) {

    yeuCauQuanLy();
    if (maKM == null || maKM.isBlank()) {
      return KetQua.loi("Mã chương trình khuyến mãi không được để trống");
    }

    String maChuanHoa = maKM.trim();
    Optional<KhuyenMai> optHienTai;
    try {
      optHienTai = khuyenMaiDao.findById(maChuanHoa);
    } catch (DataAccessException e) {
      return KetQua.loi("Lỗi khi tra cứu khuyến mãi: " + e.getMessage());
    }

    if (optHienTai.isEmpty()) {
      return KetQua.loi("Không tìm thấy chương trình khuyến mãi mã: " + maChuanHoa);
    }

    if (optHienTai.get().chiTiet().size() > 1) {
      return KetQua.loi("Chức năng sửa hiện chỉ hỗ trợ chương trình có một dòng sản phẩm");
    }
    if (maDongSP == null || maDongSP.isBlank()) {
      return KetQua.loi("Dòng sản phẩm áp dụng không được để trống");
    }
    String loiNhapLieu = validateThongTin(tenKM, tyLeGiam, ngayBatDau, ngayKetThuc);
    if (loiNhapLieu != null) {
      return KetQua.loi(loiNhapLieu);
    }

    String tenChuanHoa = tenKM.trim();
    String moTaChuanHoa = (moTa != null && !moTa.isBlank()) ? moTa.trim() : null;
    String maDongChuanHoa = maDongSP.trim();

    Optional<DongSanPham> optDongSP;
    try {
      optDongSP = dongSanPhamDao.findById(maDongChuanHoa);
    } catch (DataAccessException e) {
      return KetQua.loi("Lỗi hệ thống khi kiểm tra dòng sản phẩm: " + e.getMessage());
    }

    if (optDongSP.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm áp dụng không tồn tại trong hệ thống");
    }

    if (dangApDung) {
      try {
        if (khuyenMaiDao.existsChongLan(maDongChuanHoa, ngayBatDau, ngayKetThuc, maChuanHoa)) {
          return KetQua.loi("Dòng sản phẩm đã có chương trình khuyến mãi khác đang áp dụng trong cùng khoảng thời gian");
        }
      } catch (DataAccessException e) {
        return KetQua.loi("Không thể kiểm tra xung đột thời gian khuyến mãi: " + e.getMessage());
      }
    }

    KhuyenMai khuyenMaiCapNhat = new KhuyenMai(
        maChuanHoa,
        tenChuanHoa,
        ngayBatDau,
        ngayKetThuc,
        tyLeGiam,
        moTaChuanHoa,
        dangApDung,
        optDongSP.get()
    );

    try {
      khuyenMaiDao.update(khuyenMaiCapNhat);
      log.info("Cập nhật khuyến mãi thành công: mã {}", maChuanHoa);
      return KetQua.thanhCong(khuyenMaiCapNhat, "Cập nhật chương trình khuyến mãi thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi cập nhật khuyến mãi: {}", maChuanHoa, e);
      return KetQua.loi("Cập nhật khuyến mãi thất bại: " + e.getMessage());
    }
  }

  /**
   * Bật trạng thái áp dụng cho một chương trình khuyến mãi.
   *
   * @param maKM mã khuyến mãi cần kích hoạt
   * @return kết quả thao tác
   */
  public KetQua<Void> batDauApDung(String maKM) {
    yeuCauQuanLy();
    if (maKM == null || maKM.isBlank()) {
      return KetQua.loi("Mã chương trình khuyến mãi không được để trống");
    }

    String maChuanHoa = maKM.trim();
    Optional<KhuyenMai> optKM;
    try {
      optKM = khuyenMaiDao.findById(maChuanHoa);
    } catch (DataAccessException e) {
      return KetQua.loi("Lỗi khi kiểm tra khuyến mãi: " + e.getMessage());
    }

    if (optKM.isEmpty()) {
      return KetQua.loi("Không tìm thấy chương trình khuyến mãi mã: " + maChuanHoa);
    }

    KhuyenMai km = optKM.get();
    if (km.dangApDung()) {
      return KetQua.thanhCong(null, "Chương trình khuyến mãi hiện đã ở trạng thái áp dụng");
    }

    // Không kích hoạt chương trình đã kết thúc trong quá khứ
    if (km.ngayKetThuc().isBefore(LocalDate.now())) {
      return KetQua.loi("Không thể kích hoạt chương trình khuyến mãi đã hết hạn");
    }

    // Kiểm tra chồng lấn thời gian với chương trình đang áp dụng khác
    try {
      for (ChiTietKhuyenMai chiTiet : km.chiTiet()) {
        String maDong = chiTiet.dongSanPham().maDongSP();
        if (khuyenMaiDao.existsChongLan(maDong, km.ngayBatDau(), km.ngayKetThuc(), maChuanHoa)) {
          return KetQua.loi("Không thể kích hoạt do xung đột thời gian trên dòng sản phẩm: " + maDong);
        }
      }
    } catch (DataAccessException e) {
      return KetQua.loi("Lỗi khi kiểm tra xung đột thời gian: " + e.getMessage());
    }

    try {
      khuyenMaiDao.updateTrangThaiApDung(maChuanHoa, true);
      log.info("Kích hoạt áp dụng khuyến mãi thành công: {}", maChuanHoa);
      return KetQua.thanhCong(null, "Kích hoạt áp dụng chương trình khuyến mãi thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi kích hoạt khuyến mãi: {}", maChuanHoa, e);
      return KetQua.loi("Kích hoạt khuyến mãi thất bại: " + e.getMessage());
    }
  }

  /**
   * Dừng áp dụng một chương trình khuyến mãi.
   *
   * @param maKM mã khuyến mãi cần dừng
   * @return kết quả thao tác
   */
  public KetQua<Void> dungApDung(String maKM) {
    yeuCauQuanLy();
    if (maKM == null || maKM.isBlank()) {
      return KetQua.loi("Mã chương trình khuyến mãi không được để trống");
    }

    String maChuanHoa = maKM.trim();
    Optional<KhuyenMai> optKM;
    try {
      optKM = khuyenMaiDao.findById(maChuanHoa);
    } catch (DataAccessException e) {
      return KetQua.loi("Lỗi khi kiểm tra khuyến mãi: " + e.getMessage());
    }

    if (optKM.isEmpty()) {
      return KetQua.loi("Không tìm thấy chương trình khuyến mãi mã: " + maChuanHoa);
    }

    if (!optKM.get().dangApDung()) {
      return KetQua.thanhCong(null, "Chương trình khuyến mãi hiện đã dừng áp dụng");
    }

    try {
      khuyenMaiDao.updateTrangThaiApDung(maChuanHoa, false);
      log.info("Dừng áp dụng khuyến mãi thành công: {}", maChuanHoa);
      return KetQua.thanhCong(null, "Dừng áp dụng chương trình khuyến mãi thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi dừng áp dụng khuyến mãi: {}", maChuanHoa, e);
      return KetQua.loi("Dừng áp dụng khuyến mãi thất bại: " + e.getMessage());
    }
  }

  /** Bắt buộc phiên đăng nhập hiện tại là Nhân viên quản lý (UC012). */
  private static void yeuCauQuanLy() {
    boolean laQuanLy = CurrentSession.get()
        .map(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElse(false);
    if (!laQuanLy) {
      throw new SecurityException(LOI_QUYEN);
    }
  }

  /** Kiểm tra tệp trước khi hiển thị hoặc xác nhận nhập. */
  public ImportResult xemTruocNhapExcel(ImportResult tep) {
    yeuCauQuyenNhap();
    return kiemTraTep(tep).ketQua();
  }

  /** Nhập tất cả nhóm trong một giao dịch sau khi kiểm tra lại tệp. */
  public KetQuaNhap nhapExcel(ImportResult tep) {
    yeuCauQuyenNhap();
    TepKhuyenMai daKiemTra = kiemTraTep(tep);
    if (!daKiemTra.ketQua().hopLe()) {
      return new KetQuaNhap(0, daKiemTra.ketQua());
    }
    Nhom nhomDangLuu = null;
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        for (Nhom nhom : daKiemTra.nhom()) {
          nhomDangLuu = nhom;
          String maKM = MaGenerator.next(conn, "KhuyenMai", "maKM", "KM");
          var tt = nhom.thongTin();
          List<ChiTietKhuyenMai> chiTiet = nhom.dongTheoMaSP().keySet().stream()
              .map(maDong -> new ChiTietKhuyenMai(maKM, daKiemTra.dongSanPham().get(maDong))).toList();
          khuyenMaiDao.insert(conn, new KhuyenMai(maKM, tt.ten(), tt.batDau(), tt.ketThuc(),
              tt.tyLe(), tt.moTa(), chiTiet, tt.apDung()));
        }
        nhomDangLuu = null;
        conn.commit();
        return new KetQuaNhap(daKiemTra.nhom().size(), tep);
      } catch (SQLException | RuntimeException e) {
        try {
          conn.rollback();
        } catch (SQLException rollbackError) {
          e.addSuppressed(rollbackError);
        }
        throw e;
      }
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi nhập chương trình khuyến mãi", e);
      int dong = nhomDangLuu == null ? 0 : nhomDangLuu.dongDau();
      String cot = nhomDangLuu == null ? "" : KhuyenMaiExcel.MA_NHOM;
      String thongBao = "Không thể lưu tệp khuyến mãi";
      if (e instanceof KhuyenMaiDao.LoiDongKhuyenMai loiDong && nhomDangLuu != null) {
        dong = nhomDangLuu.dongTheoMaSP().getOrDefault(loiDong.maDongSP(), dong);
        cot = KhuyenMaiExcel.MA_DONG;
        thongBao = loiDong.getMessage();
      }
      return new KetQuaNhap(0, new ImportResult(tep.dong(),
          List.of(KhuyenMaiExcel.loi(dong, cot, thongBao)), tep.tongSoDong()));
    }
  }

  private TepKhuyenMai kiemTraTep(ImportResult tep) {
    List<ImportError> loi = new ArrayList<>(tep.loi());
    if (!loi.isEmpty()) {
      return new TepKhuyenMai(tep, List.of(), Map.of());
    }
    if (tep.dong().isEmpty()) {
      loi.add(KhuyenMaiExcel.loi(0, "", "Tệp không có dòng dữ liệu"));
    }
    List<Nhom> nhom = KhuyenMaiExcel.gomNhom(tep, loi);
    Map<String, DongSanPham> dongSanPham = new HashMap<>();
    if (loi.isEmpty()) {
      kiemTraDongNhap(nhom, dongSanPham, loi);
    }
    return new TepKhuyenMai(new ImportResult(tep.dong(), loi, tep.tongSoDong()), nhom, dongSanPham);
  }

  private void kiemTraDongNhap(
      List<Nhom> cacNhom, Map<String, DongSanPham> dongSanPham, List<ImportError> loi) {
    Map<String, List<Nhom>> nhomApDungTheoDong = new HashMap<>();
    for (Nhom nhom : cacNhom) {
      var tt = nhom.thongTin();
      for (var chiTiet : nhom.dongTheoMaSP().entrySet()) {
        String maDong = chiTiet.getKey();
        int dongExcel = chiTiet.getValue();
        try {
          DongSanPham dong = dongSanPham.computeIfAbsent(maDong,
              ma -> dongSanPhamDao.findById(ma).orElse(null));
          if (dong == null) {
            loi.add(KhuyenMaiExcel.loi(dongExcel, KhuyenMaiExcel.MA_DONG,
                "Dòng sản phẩm không tồn tại: " + maDong));
            continue;
          }
          if (!tt.apDung()) continue;
          List<Nhom> nhomTruoc = nhomApDungTheoDong.computeIfAbsent(maDong,
              ma -> new ArrayList<>());
          boolean chongLanTrongTep = nhomTruoc.stream().anyMatch(truoc ->
              !truoc.thongTin().ketThuc().isBefore(tt.batDau())
                  && !truoc.thongTin().batDau().isAfter(tt.ketThuc()));
          nhomTruoc.add(nhom);
          if (chongLanTrongTep || khuyenMaiDao.existsChongLan(
              maDong, tt.batDau(), tt.ketThuc(), null)) {
            loi.add(KhuyenMaiExcel.loi(dongExcel, KhuyenMaiExcel.MA_DONG,
                "Dòng sản phẩm có khuyến mãi đang áp dụng trong cùng khoảng ngày: " + maDong));
          }
        } catch (DataAccessException e) {
          log.error("Lỗi kiểm tra dòng khuyến mãi trong tệp", e);
          loi.add(KhuyenMaiExcel.loi(dongExcel, KhuyenMaiExcel.MA_DONG,
              "Không thể kiểm tra dòng sản phẩm: " + maDong));
          return;
        }
      }
    }
  }

  private static void yeuCauQuyenNhap() {
    boolean duocNhap = CurrentSession.get().map(nv ->
        nv.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY
            || nv.loaiQuyen() == LoaiQuyen.NHAN_VIEN_BAN_HANG).orElse(false);
    if (!duocNhap) throw new SecurityException("Cần đăng nhập để nhập khuyến mãi");
  }

  public record KetQuaNhap(int soChuongTrinh, ImportResult ketQua) {}

  private record TepKhuyenMai(
      ImportResult ketQua, List<Nhom> nhom, Map<String, DongSanPham> dongSanPham) {}

  private String validateThongTin(
      String tenKM,
      BigDecimal tyLeGiam,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc) {

    if (tenKM == null || tenKM.isBlank()) {
      return "Tên chương trình khuyến mãi không được để trống";
    }

    if (tyLeGiam == null) {
      return "Tỷ lệ giảm giá không được để trống";
    }

    if (tyLeGiam.compareTo(BigDecimal.ZERO) < 0 || tyLeGiam.compareTo(new BigDecimal("100")) > 0) {
      return "Tỷ lệ giảm giá phải nằm trong khoảng từ 0% đến 100%";
    }
    if (tenKM.trim().length() > 200) {
      return "Tên chương trình khuyến mãi không được vượt quá 200 ký tự";
    }
    if (tyLeGiam.stripTrailingZeros().scale() > 2) {
      return "Tỷ lệ giảm giá chỉ được có tối đa 2 chữ số thập phân";
    }

    if (ngayBatDau == null) {
      return "Ngày bắt đầu không được để trống";
    }

    if (ngayKetThuc == null) {
      return "Ngày kết thúc không được để trống";
    }

    if (ngayBatDau.isAfter(ngayKetThuc)) {
      return "Ngày bắt đầu không được sau ngày kết thúc";
    }

    return null;
  }

  /**
   * Đóng gói kết quả trả về của các thao tác nghiệp vụ khuyến mãi.
   *
   * @param <T> kiểu dữ liệu đối tượng trả về
   * @param thanhCong trạng thái thành công
   * @param thongBao thông báo mô tả kết quả
   * @param duLieu dữ liệu đối tượng trả về khi thành công
   */
  public record KetQua<T>(boolean thanhCong, String thongBao, T duLieu) {

    public static <T> KetQua<T> thanhCong(T duLieu, String thongBao) {
      return new KetQua<>(true, thongBao, duLieu);
    }

    public static <T> KetQua<T> loi(String thongBao) {
      return new KetQua<>(false, thongBao, null);
    }
  }
}
