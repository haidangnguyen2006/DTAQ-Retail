package com.dtaq.retail.controller;

import com.dtaq.retail.dao.KhachHangDao;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.MaGenerator;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Xử lý nghiệp vụ thêm, cập nhật và tra cứu khách hàng (UC007). */
public final class KhachHangController {

  /** Tên trường họ tên, dùng để giao diện hiển thị lỗi đúng ô nhập. */
  public static final String TRUONG_HO_TEN = "hoTen";

  /** Tên trường số điện thoại, dùng để giao diện hiển thị lỗi đúng ô nhập. */
  public static final String TRUONG_SDT = "soDienThoai";

  private static final Logger log = LoggerFactory.getLogger(KhachHangController.class);

  private static final String SDT_HOP_LE = "0\\d{9,10}";
  private static final String TIEN_TO_MA = "KH";
  private static final String LOI_THIEU_HO_TEN = "Họ tên không được để trống";
  private static final String LOI_THIEU_SDT = "Số điện thoại không được để trống";
  private static final String LOI_SAI_SDT = "Số điện thoại phải gồm 10–11 chữ số và bắt đầu bằng 0";
  private static final String LOI_LUU = "Không thể lưu khách hàng. Vui lòng thử lại";
  private static final String LOI_QUYEN = "Chức năng này chỉ dành cho nhân viên bán hàng";

  private final KhachHangDao khachHangDao;
  private final DataSource dataSource;

  /**
   * Khởi tạo bộ điều khiển khách hàng.
   *
   * @param khachHangDao lớp truy cập khách hàng; không được {@code null}
   * @param dataSource nguồn kết nối để mở giao dịch cấp mã và ghi; không được {@code null}
   */
  public KhachHangController(KhachHangDao khachHangDao, DataSource dataSource) {
    this.khachHangDao = Objects.requireNonNull(khachHangDao, "khachHangDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Lấy một trang khách hàng khớp từ khóa, sắp xếp theo mã khách hàng.
   *
   * @param tuKhoa một phần họ tên (không phân biệt hoa thường) hoặc số điện thoại; rỗng để lấy tất cả
   * @param trang số trang cần lấy, bắt đầu từ 1; lớn hơn trang cuối thì trả về trang cuối
   * @param kichThuoc số khách hàng trên mỗi trang; phải lớn hơn 0
   * @return trang kết quả; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên bán hàng
   * @throws DataAccessException nếu không đọc được dữ liệu
   */
  public TrangKhachHang danhSach(String tuKhoa, int trang, int kichThuoc) {
    yeuCauNhanVienBanHang();
    if (kichThuoc <= 0) {
      throw new IllegalArgumentException("Kích thước trang phải lớn hơn 0");
    }
    String tuKhoaThuong = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);
    String tuKhoaSdt = chuanHoaSdt(tuKhoa);
    List<KhachHang> khop = khachHangDao.findAll().stream()
        .filter(kh -> tuKhoaThuong.isEmpty()
            || kh.hoTen().toLowerCase(Locale.ROOT).contains(tuKhoaThuong)
            || kh.soDienThoai().contains(tuKhoaSdt))
        .toList();

    int tongSo = khop.size();
    int trangCuoi = Math.max(1, (tongSo + kichThuoc - 1) / kichThuoc);
    int trangHienTai = Math.max(1, Math.min(trang, trangCuoi));
    int tu = (trangHienTai - 1) * kichThuoc;
    int den = Math.min(tu + kichThuoc, tongSo);
    return new TrangKhachHang(khop.subList(tu, den), tongSo, trangHienTai);
  }

  /**
   * Tìm khách hàng theo số điện thoại; khoảng trắng trong số nhập vào được bỏ qua.
   *
   * @param soDienThoai số điện thoại do người dùng nhập
   * @return khách hàng tương ứng, hoặc rỗng nếu không tồn tại
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên bán hàng
   * @throws DataAccessException nếu không đọc được dữ liệu
   */
  public Optional<KhachHang> timTheoSdt(String soDienThoai) {
    yeuCauNhanVienBanHang();
    return khachHangDao.findBySdt(chuanHoaSdt(soDienThoai));
  }

  /**
   * Thêm khách hàng mới. Nếu số điện thoại đã có hồ sơ thì không tạo mới mà trả về hồ sơ đó để đối
   * chiếu (luồng 8.a). Mã khách hàng được cấp và ghi trong cùng một giao dịch.
   *
   * @param hoTen họ tên khách hàng
   * @param soDienThoai số điện thoại khách hàng
   * @param diaChi địa chỉ khách hàng; có thể để trống
   * @return kết quả lưu; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên bán hàng
   */
  public KetQuaLuuKhach themKhach(String hoTen, String soDienThoai, String diaChi) {
    yeuCauNhanVienBanHang();
    Optional<KetQuaLuuKhach> loi = kiemTraDuLieu(hoTen, soDienThoai);
    if (loi.isPresent()) {
      return loi.get();
    }
    String sdt = chuanHoaSdt(soDienThoai);

    try {
      Optional<KhachHang> trung = khachHangDao.findBySdt(sdt);
      if (trung.isPresent()) {
        return KetQuaLuuKhach.trung(trung.get());
      }
      try (Connection ketNoi = dataSource.getConnection()) {
        return KetQuaLuuKhach.thanhCong(ghiKhachMoi(ketNoi, hoTen, sdt, diaChi));
      }
    } catch (SQLException | DataAccessException e) {
      log.warn("Không thể lưu khách hàng mới (exception={})", e.getClass().getName());
      return KetQuaLuuKhach.loi(null, LOI_LUU);
    }
  }

  /**
   * Cập nhật họ tên, số điện thoại và địa chỉ của khách hàng; mã khách hàng không đổi.
   *
   * @param maKH mã khách hàng cần cập nhật
   * @param hoTen họ tên mới
   * @param soDienThoai số điện thoại mới; không được trùng hồ sơ của khách hàng khác
   * @param diaChi địa chỉ mới; có thể để trống
   * @return kết quả lưu; không bao giờ {@code null}
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên bán hàng
   */
  public KetQuaLuuKhach capNhatKhach(
      String maKH, String hoTen, String soDienThoai, String diaChi) {
    yeuCauNhanVienBanHang();
    Optional<KetQuaLuuKhach> loi = kiemTraDuLieu(hoTen, soDienThoai);
    if (loi.isPresent()) {
      return loi.get();
    }
    String sdt = chuanHoaSdt(soDienThoai);

    try {
      Optional<KhachHang> cungSdt = khachHangDao.findBySdt(sdt);
      if (cungSdt.isPresent() && !cungSdt.get().maKH().equals(maKH)) {
        return KetQuaLuuKhach.loi(
            TRUONG_SDT, "Số điện thoại đã thuộc hồ sơ " + cungSdt.get().maKH());
      }
      KhachHang khachHang = new KhachHang(maKH, hoTen.trim(), sdt, chuanHoaDiaChi(diaChi));
      khachHangDao.update(khachHang);
      return KetQuaLuuKhach.thanhCong(khachHang);
    } catch (DataAccessException e) {
      log.warn("Không thể cập nhật khách hàng (exception={})", e.getClass().getName());
      return KetQuaLuuKhach.loi(null, LOI_LUU);
    }
  }

  /**
   * Kiểm tra định dạng số điện thoại khách hàng.
   *
   * @param soDienThoai số điện thoại do người dùng nhập
   * @return thông báo lỗi, hoặc {@code null} nếu hợp lệ
   */
  public static String kiemTraSdt(String soDienThoai) {
    String sdt = chuanHoaSdt(soDienThoai);
    if (sdt.isEmpty()) {
      return LOI_THIEU_SDT;
    }
    return sdt.matches(SDT_HOP_LE) ? null : LOI_SAI_SDT;
  }

  private KhachHang ghiKhachMoi(Connection ketNoi, String hoTen, String sdt, String diaChi)
      throws SQLException {
    ketNoi.setAutoCommit(false);
    try {
      String maKH = MaGenerator.next(ketNoi, "KhachHang", "maKH", TIEN_TO_MA);
      KhachHang khachHang = new KhachHang(maKH, hoTen.trim(), sdt, chuanHoaDiaChi(diaChi));
      khachHangDao.insert(ketNoi, khachHang);
      ketNoi.commit();
      return khachHang;
    } catch (SQLException | RuntimeException e) {
      ketNoi.rollback();
      throw e;
    } finally {
      ketNoi.setAutoCommit(true);
    }
  }

  private static Optional<KetQuaLuuKhach> kiemTraDuLieu(String hoTen, String soDienThoai) {
    if (hoTen == null || hoTen.isBlank()) {
      return Optional.of(KetQuaLuuKhach.loi(TRUONG_HO_TEN, LOI_THIEU_HO_TEN));
    }
    String loiSdt = kiemTraSdt(soDienThoai);
    return loiSdt == null
        ? Optional.empty()
        : Optional.of(KetQuaLuuKhach.loi(TRUONG_SDT, loiSdt));
  }

  private static String chuanHoaSdt(String soDienThoai) {
    return soDienThoai == null ? "" : soDienThoai.replaceAll("\\s", "");
  }

  private static String chuanHoaDiaChi(String diaChi) {
    return diaChi == null || diaChi.isBlank() ? null : diaChi.trim();
  }

  private static void yeuCauNhanVienBanHang() {
    boolean duocPhep = CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_BAN_HANG)
        .isPresent();
    if (!duocPhep) {
      throw new SecurityException(LOI_QUYEN);
    }
  }

  /** Một trang khách hàng kèm tổng số kết quả khớp và số trang thực tế. */
  public record TrangKhachHang(List<KhachHang> danhSach, int tongSo, int trang) {
  }

  /** Các kết cục của một lần lưu khách hàng. */
  public enum TrangThaiLuu {
    THANH_CONG,
    TRUNG_SDT,
    LOI
  }

  /**
   * Kết quả lưu khách hàng.
   *
   * @param trangThai kết cục của lần lưu
   * @param khachHang hồ sơ vừa lưu, hoặc hồ sơ hiện có khi trùng số điện thoại
   * @param truongLoi trường bị lỗi; {@code null} nếu lỗi không gắn với trường nào
   * @param thongBao thông báo lỗi tiếng Việt; {@code null} nếu không có lỗi
   */
  public record KetQuaLuuKhach(
      TrangThaiLuu trangThai, KhachHang khachHang, String truongLoi, String thongBao) {

    static KetQuaLuuKhach thanhCong(KhachHang khachHang) {
      return new KetQuaLuuKhach(TrangThaiLuu.THANH_CONG, khachHang, null, null);
    }

    static KetQuaLuuKhach trung(KhachHang khachHang) {
      return new KetQuaLuuKhach(TrangThaiLuu.TRUNG_SDT, khachHang, null, null);
    }

    static KetQuaLuuKhach loi(String truongLoi, String thongBao) {
      return new KetQuaLuuKhach(TrangThaiLuu.LOI, null, truongLoi, thongBao);
    }
  }
}
