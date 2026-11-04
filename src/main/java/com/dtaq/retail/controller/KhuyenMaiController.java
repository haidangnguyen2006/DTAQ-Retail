package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.MaGenerator;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
  private final DataSource dataSource;

  /**
   * Khởi tạo bộ điều khiển khuyến mãi.
   *
   * @param khuyenMaiDao lớp truy cập dữ liệu chương trình khuyến mãi; không được {@code null}
   * @param dongSanPhamDao lớp truy cập dữ liệu dòng sản phẩm; không được {@code null}
   * @param dataSource nguồn kết nối CSDL phục vụ sinh mã định danh tự động; không được {@code null}
   */
  public KhuyenMaiController(
      KhuyenMaiDao khuyenMaiDao,
      DongSanPhamDao dongSanPhamDao,
      DataSource dataSource) {
    this.khuyenMaiDao = Objects.requireNonNull(khuyenMaiDao, "khuyenMaiDao");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
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

    yeuCauQuanLy();

    // 1. Validate thông tin nhập liệu (Luồng phụ 9.a)
    String loiNhapLieu = validateThongTin(tenKM, tyLeGiam, ngayBatDau, ngayKetThuc, maDongSP);
    if (loiNhapLieu != null) {
      return KetQua.loi(loiNhapLieu);
    }

    String tenChuanHoa = tenKM.trim();
    String moTaChuanHoa = (moTa != null && !moTa.isBlank()) ? moTa.trim() : null;
    String maDongChuanHoa = maDongSP.trim();

    // 2. Kiểm tra dòng sản phẩm tồn tại (Điều kiện trước UC012)
    Optional<DongSanPham> optDongSP;
    try {
      optDongSP = dongSanPhamDao.findById(maDongChuanHoa);
    } catch (DataAccessException e) {
      log.error("Lỗi kiểm tra dòng sản phẩm: {}", maDongChuanHoa, e);
      return KetQua.loi("Lỗi hệ thống khi kiểm tra dòng sản phẩm: " + e.getMessage());
    }

    if (optDongSP.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm áp dụng không tồn tại trong hệ thống");
    }

    // 3. Kiểm tra xung đột thời gian áp dụng (Luồng phụ 10.b & Quyết định D11)
    if (dangApDung) {
      try {
        if (khuyenMaiDao.existsChongLan(maDongChuanHoa, ngayBatDau, ngayKetThuc, null)) {
          return KetQua.loi("Dòng sản phẩm đã có chương trình khuyến mãi khác đang áp dụng trong cùng khoảng thời gian");
        }
      } catch (DataAccessException e) {
        log.error("Lỗi kiểm tra chồng lấn khuyến mãi: {}", maDongChuanHoa, e);
        return KetQua.loi("Không thể kiểm tra xung đột thời gian khuyến mãi: " + e.getMessage());
      }
    }

    // 4. Sinh mã khuyến mãi tự động (KM-YYYY-NNNNNN theo Quyết định C-12 / D7)
    String maKM;
    try (Connection conn = dataSource.getConnection()) {
      maKM = MaGenerator.next(conn, "KhuyenMai", "maKM", "KM");
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi sinh mã khuyến mãi mới", e);
      return KetQua.loi("Không thể khởi tạo mã khuyến mãi mới: " + e.getMessage());
    }

    // 5. Kiểm tra trùng mã (Luồng phụ 10.a)
    try {
      if (khuyenMaiDao.existsTrungMa(maKM)) {
        return KetQua.loi("Mã chương trình khuyến mãi đã tồn tại: " + maKM);
      }
    } catch (DataAccessException e) {
      log.warn("Lỗi kiểm tra trùng mã khuyến mãi: {}", e.getMessage());
    }

    // 6. Lưu khuyến mãi vào CSDL
    KhuyenMai khuyenMaiMoi = new KhuyenMai(
        maKM,
        tenChuanHoa,
        ngayBatDau,
        ngayKetThuc,
        tyLeGiam,
        moTaChuanHoa,
        dangApDung,
        optDongSP.get()
    );

    try {
      khuyenMaiDao.insert(khuyenMaiMoi);
      log.info("Thêm mới khuyến mãi thành công: mã {}, tên {}", maKM, tenChuanHoa);
      return KetQua.thanhCong(khuyenMaiMoi, "Thêm chương trình khuyến mãi thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi lưu chương trình khuyến mãi mới", e);
      return KetQua.loi("Thêm khuyến mãi thất bại: " + e.getMessage());
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

    String loiNhapLieu = validateThongTin(tenKM, tyLeGiam, ngayBatDau, ngayKetThuc, maDongSP);
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
      if (khuyenMaiDao.existsChongLan(km.dongSanPham().maDongSP(), km.ngayBatDau(), km.ngayKetThuc(), maChuanHoa)) {
        return KetQua.loi("Không thể kích hoạt do xung đột thời gian với chương trình khuyến mãi khác đang áp dụng");
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

  private String validateThongTin(
      String tenKM,
      BigDecimal tyLeGiam,
      LocalDate ngayBatDau,
      LocalDate ngayKetThuc,
      String maDongSP) {

    if (tenKM == null || tenKM.isBlank()) {
      return "Tên chương trình khuyến mãi không được để trống";
    }

    if (tyLeGiam == null) {
      return "Tỷ lệ giảm giá không được để trống";
    }

    if (tyLeGiam.compareTo(BigDecimal.ZERO) < 0 || tyLeGiam.compareTo(new BigDecimal("100")) > 0) {
      return "Tỷ lệ giảm giá phải nằm trong khoảng từ 0% đến 100%";
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

    if (maDongSP == null || maDongSP.isBlank()) {
      return "Dòng sản phẩm áp dụng không được để trống";
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
