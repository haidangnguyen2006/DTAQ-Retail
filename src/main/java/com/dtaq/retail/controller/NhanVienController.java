package com.dtaq.retail.controller;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.MaGenerator;
import com.dtaq.retail.util.PasswordHasher;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Xử lý nghiệp vụ quản lý nhân viên (UC013); chỉ Nhân viên quản lý được sử dụng. */
public final class NhanVienController {

  /** Tên trường họ tên, dùng để giao diện hiển thị lỗi đúng ô nhập. */
  public static final String TRUONG_HO_TEN = "hoTen";

  /** Tên trường số điện thoại. */
  public static final String TRUONG_SDT = "soDienThoai";

  /** Tên trường email. */
  public static final String TRUONG_EMAIL = "email";

  /** Tên trường ngày vào làm. */
  public static final String TRUONG_NGAY_VAO_LAM = "ngayVaoLam";

  /** Tên trường tên đăng nhập. */
  public static final String TRUONG_TEN_DANG_NHAP = "tenDangNhap";

  /** Tên trường mật khẩu. */
  public static final String TRUONG_MAT_KHAU = "matKhau";

  /** Độ dài mật khẩu tối thiểu, cùng quy tắc với chức năng đặt lại mật khẩu (UC002). */
  public static final int DO_DAI_MAT_KHAU_TOI_THIEU = 8;

  private static final Logger log = LoggerFactory.getLogger(NhanVienController.class);

  private static final String TIEN_TO_MA = "NVBANHANG";
  private static final String EMAIL_HOP_LE = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";
  private static final String SQLSTATE_TRUNG_KHOA = "23505";
  private static final String RANG_BUOC_TEN_DANG_NHAP = "uq_nhanvien_tendangnhap";
  private static final String LOI_QUYEN = "Chức năng này chỉ dành cho nhân viên quản lý";
  private static final String LOI_TRUNG_TEN_DANG_NHAP = "Tên đăng nhập đã tồn tại";
  private static final String LOI_TRUNG_MA = "Mã nhân viên vừa cấp đã tồn tại. Vui lòng lưu lại";
  private static final String LOI_LUU = "Không thể lưu nhân viên. Vui lòng thử lại";
  private static final String LOI_DOI_TRANG_THAI = "Không thể cập nhật trạng thái tài khoản. Vui lòng thử lại";
  private static final String LOI_TU_KHOA = "Không thể tự khóa tài khoản đang đăng nhập";

  private final NhanVienDao nhanVienDao;
  private final DataSource dataSource;

  /**
   * Khởi tạo bộ điều khiển nhân viên với nguồn dữ liệu dùng chung của ứng dụng.
   *
   * <p>Giữ lại cho chỗ khởi tạo hiện có trong {@code AppNavigator}; nên chuyển sang
   * {@link #NhanVienController(NhanVienDao, DataSource)} để truyền nguồn dữ liệu tường minh.
   *
   * @param nhanVienDao lớp truy cập nhân viên; không được {@code null}
   */
  public NhanVienController(NhanVienDao nhanVienDao) {
    this(nhanVienDao, ConnectDB.getDataSource());
  }

  /**
   * Khởi tạo bộ điều khiển nhân viên.
   *
   * @param nhanVienDao lớp truy cập nhân viên; không được {@code null}
   * @param dataSource nguồn kết nối để mở giao dịch cấp mã và ghi; không được {@code null}
   */
  public NhanVienController(NhanVienDao nhanVienDao, DataSource dataSource) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Liệt kê toàn bộ nhân viên, gồm cả tài khoản đã khóa.
   *
   * @return danh sách nhân viên, có thể rỗng
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   * @throws DataAccessException nếu không đọc được dữ liệu
   */
  public List<NhanVien> danhSach() {
    return danhSach(null);
  }

  /**
   * Liệt kê nhân viên theo trạng thái tài khoản.
   *
   * @param dangHoatDong {@code true} chỉ lấy tài khoản đang hoạt động, {@code false} chỉ lấy tài
   *     khoản đã khóa, {@code null} lấy tất cả
   * @return danh sách nhân viên, có thể rỗng
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   * @throws DataAccessException nếu không đọc được dữ liệu
   */
  public List<NhanVien> danhSach(Boolean dangHoatDong) {
    yeuCauQuanLy();
    List<NhanVien> tatCa = nhanVienDao.findAll();
    if (dangHoatDong == null) {
      return tatCa;
    }
    return tatCa.stream().filter(nv -> nv.trangThaiHoatDong() == dangHoatDong).toList();
  }

  /**
   * Thêm nhân viên bán hàng mới cùng tài khoản đang hoạt động (UC013). Mã nhân viên được cấp và
   * ghi trong cùng một giao dịch; mật khẩu chỉ được lưu dưới dạng băm. Mảng mật khẩu được xóa
   * trắng sau khi dùng.
   *
   * @param hoTen họ tên nhân viên
   * @param soDienThoai số điện thoại liên hệ
   * @param email email liên hệ; có thể để trống
   * @param ngayVaoLam ngày vào làm
   * @param tenDangNhap tên đăng nhập; không được trùng
   * @param matKhau mật khẩu khởi tạo dạng thô
   * @return kết quả lưu; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   */
  public KetQuaLuuNhanVien themNhanVien(String hoTen, String soDienThoai, String email,
      LocalDate ngayVaoLam, String tenDangNhap, char[] matKhau) {
    try {
      yeuCauQuanLy();
      Optional<KetQuaLuuNhanVien> loi =
          kiemTraDuLieu(hoTen, soDienThoai, email, ngayVaoLam, tenDangNhap, matKhau);
      if (loi.isPresent()) {
        return loi.get();
      }
      String ten = tenDangNhap.trim();
      if (nhanVienDao.existsTenDangNhap(ten)) {
        return KetQuaLuuNhanVien.loi(TRUONG_TEN_DANG_NHAP, LOI_TRUNG_TEN_DANG_NHAP);
      }
      String matKhauBam = PasswordHasher.hash(matKhau);
      try (Connection ketNoi = dataSource.getConnection()) {
        return KetQuaLuuNhanVien.thanhCong(ghiNhanVienMoi(ketNoi,
            hoTen, soDienThoai, email, ngayVaoLam, ten, matKhauBam));
      }
    } catch (DataAccessException e) {
      if (laTrungKhoa(e)) {
        return chuyenLoiTrungKhoa(e);
      }
      log.warn("Không thể lưu nhân viên mới (exception={})", e.getClass().getName());
      return KetQuaLuuNhanVien.loi(null, LOI_LUU);
    } catch (SQLException e) {
      log.warn("Không thể lưu nhân viên mới (exception={})", e.getClass().getName());
      return KetQuaLuuNhanVien.loi(null, LOI_LUU);
    } finally {
      if (matKhau != null) {
        Arrays.fill(matKhau, '\0');
      }
    }
  }

  /**
   * Khóa tài khoản khi nhân viên nghỉ việc. Hồ sơ không bị xóa để giữ lịch sử giao dịch; quản lý
   * không thể tự khóa tài khoản đang đăng nhập.
   *
   * @param maNV mã nhân viên cần khóa
   * @return kết quả cập nhật; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   */
  public KetQuaLuuNhanVien khoaTaiKhoan(String maNV) {
    NhanVien hienTai = yeuCauQuanLy();
    if (hienTai.maNV().equals(maNV)) {
      return KetQuaLuuNhanVien.loi(null, LOI_TU_KHOA);
    }
    return doiTrangThai(maNV, false);
  }

  /**
   * Mở lại tài khoản đã khóa.
   *
   * @param maNV mã nhân viên cần mở khóa
   * @return kết quả cập nhật; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   */
  public KetQuaLuuNhanVien moKhoaTaiKhoan(String maNV) {
    yeuCauQuanLy();
    return doiTrangThai(maNV, true);
  }

  private KetQuaLuuNhanVien doiTrangThai(String maNV, boolean trangThaiHoatDong) {
    try {
      nhanVienDao.updateTrangThai(maNV, trangThaiHoatDong);
      return KetQuaLuuNhanVien.thanhCong(nhanVienDao.findByMa(maNV).orElse(null));
    } catch (DataAccessException e) {
      log.warn("Không thể cập nhật trạng thái nhân viên (exception={})", e.getClass().getName());
      return KetQuaLuuNhanVien.loi(null, LOI_DOI_TRANG_THAI);
    }
  }

  private NhanVien ghiNhanVienMoi(Connection ketNoi, String hoTen, String soDienThoai, String email,
      LocalDate ngayVaoLam, String tenDangNhap, String matKhauBam) throws SQLException {
    ketNoi.setAutoCommit(false);
    try {
      String maNV = MaGenerator.next(ketNoi, "NhanVien", "maNV", TIEN_TO_MA);
      NhanVien nhanVien = new NhanVien(maNV, hoTen.trim(), chuanHoaSdt(soDienThoai),
          email == null || email.isBlank() ? null : email.trim(), ngayVaoLam, tenDangNhap,
          matKhauBam, LoaiQuyen.NHAN_VIEN_BAN_HANG, true);
      nhanVienDao.insert(ketNoi, nhanVien);
      ketNoi.commit();
      return nhanVien;
    } catch (SQLException | RuntimeException e) {
      ketNoi.rollback();
      throw e;
    } finally {
      ketNoi.setAutoCommit(true);
    }
  }

  private static Optional<KetQuaLuuNhanVien> kiemTraDuLieu(String hoTen, String soDienThoai,
      String email, LocalDate ngayVaoLam, String tenDangNhap, char[] matKhau) {
    if (hoTen == null || hoTen.isBlank()) {
      return Optional.of(KetQuaLuuNhanVien.loi(TRUONG_HO_TEN, "Họ tên không được để trống"));
    }
    String loiSdt = KhachHangController.kiemTraSdt(soDienThoai);
    if (loiSdt != null) {
      return Optional.of(KetQuaLuuNhanVien.loi(TRUONG_SDT, loiSdt));
    }
    if (email != null && !email.isBlank() && !email.trim().matches(EMAIL_HOP_LE)) {
      return Optional.of(KetQuaLuuNhanVien.loi(TRUONG_EMAIL, "Email không đúng định dạng"));
    }
    if (ngayVaoLam == null) {
      return Optional.of(KetQuaLuuNhanVien.loi(TRUONG_NGAY_VAO_LAM, "Ngày vào làm không được để trống"));
    }
    if (tenDangNhap == null || tenDangNhap.isBlank()) {
      return Optional.of(KetQuaLuuNhanVien.loi(TRUONG_TEN_DANG_NHAP, "Tên đăng nhập không được để trống"));
    }
    if (matKhau == null || matKhau.length < DO_DAI_MAT_KHAU_TOI_THIEU) {
      return Optional.of(KetQuaLuuNhanVien.loi(TRUONG_MAT_KHAU,
          "Mật khẩu phải có ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự"));
    }
    return Optional.empty();
  }

  /** Lỗi trùng khóa khi hai lần lưu chạy cùng lúc, lọt qua bước kiểm tra trước khi ghi. */
  private static boolean laTrungKhoa(DataAccessException e) {
    return e.getCause() instanceof SQLException sql && SQLSTATE_TRUNG_KHOA.equals(sql.getSQLState());
  }

  private static KetQuaLuuNhanVien chuyenLoiTrungKhoa(DataAccessException e) {
    String chiTiet = e.getCause().getMessage();
    if (chiTiet != null && chiTiet.contains(RANG_BUOC_TEN_DANG_NHAP)) {
      return KetQuaLuuNhanVien.loi(TRUONG_TEN_DANG_NHAP, LOI_TRUNG_TEN_DANG_NHAP);
    }
    return KetQuaLuuNhanVien.loi(null, LOI_TRUNG_MA);
  }

  private static String chuanHoaSdt(String soDienThoai) {
    return soDienThoai == null ? "" : soDienThoai.replaceAll("\\s", "");
  }

  private static NhanVien yeuCauQuanLy() {
    return CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElseThrow(() -> new SecurityException(LOI_QUYEN));
  }

  /**
   * Kết quả lưu nhân viên hoặc đổi trạng thái tài khoản.
   *
   * @param thanhCong lần lưu có thành công hay không
   * @param nhanVien hồ sơ sau khi lưu; {@code null} khi lỗi
   * @param truongLoi trường bị lỗi; {@code null} nếu lỗi không gắn với trường nào
   * @param thongBao thông báo lỗi tiếng Việt; {@code null} nếu thành công
   */
  public record KetQuaLuuNhanVien(
      boolean thanhCong, NhanVien nhanVien, String truongLoi, String thongBao) {

    static KetQuaLuuNhanVien thanhCong(NhanVien nhanVien) {
      return new KetQuaLuuNhanVien(true, nhanVien, null, null);
    }

    static KetQuaLuuNhanVien loi(String truongLoi, String thongBao) {
      return new KetQuaLuuNhanVien(false, null, truongLoi, thongBao);
    }
  }
}
