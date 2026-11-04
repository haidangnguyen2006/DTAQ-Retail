package com.dtaq.retail.controller;

import com.dtaq.retail.dao.KhachHangDao;
import com.dtaq.retail.dao.KhachHangGiaoDichDao;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietHoaDonView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietPhieuView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.HoaDonTomTat;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.LoaiPhieu;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.PhieuTomTat;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tra cứu khách hàng và giao dịch (UC005). Chỉ đọc: không phương thức nào ghi dữ liệu.
 *
 * <p>Mọi phương thức ném {@link SecurityException} nếu phiên hiện tại không phải nhân viên bán hàng
 * và {@link DataAccessException} nếu không đọc được dữ liệu.
 */
public final class TraCuuKhachHangController {

  private static final String LOI_QUYEN = "Chức năng này chỉ dành cho nhân viên bán hàng";

  private final KhachHangDao khachHangDao;
  private final KhachHangGiaoDichDao giaoDichDao;

  /**
   * Khởi tạo bộ điều khiển tra cứu.
   *
   * @param khachHangDao lớp truy cập khách hàng; không được {@code null}
   * @param giaoDichDao lớp truy vấn giao dịch của khách hàng; không được {@code null}
   */
  public TraCuuKhachHangController(KhachHangDao khachHangDao, KhachHangGiaoDichDao giaoDichDao) {
    this.khachHangDao = Objects.requireNonNull(khachHangDao, "khachHangDao");
    this.giaoDichDao = Objects.requireNonNull(giaoDichDao, "giaoDichDao");
  }

  /**
   * Tìm khách hàng theo số điện thoại; khoảng trắng trong số nhập vào được bỏ qua.
   *
   * @param soDienThoai số điện thoại do người dùng nhập
   * @return khách hàng tương ứng, hoặc rỗng nếu không có hoặc số nhập vào để trống (luồng 5.a)
   */
  public Optional<KhachHang> timKhach(String soDienThoai) {
    yeuCauNhanVienBanHang();
    String sdt = soDienThoai == null ? "" : soDienThoai.replaceAll("\\s", "");
    if (sdt.isEmpty()) {
      return Optional.empty();
    }
    return khachHangDao.findBySdt(sdt);
  }

  /**
   * Lịch sử mua hàng của khách, mới nhất trước.
   *
   * @param maKH mã khách hàng
   * @return danh sách hóa đơn, có thể rỗng
   */
  public List<HoaDonTomTat> lichSuHoaDon(String maKH) {
    yeuCauNhanVienBanHang();
    return giaoDichDao.hoaDonCuaKhach(maKH);
  }

  /**
   * Chi tiết hóa đơn gốc theo giá trị đã chốt lúc bán.
   *
   * @param maHD mã hóa đơn
   * @return các dòng chi tiết, có thể rỗng
   */
  public List<ChiTietHoaDonView> chiTietHoaDon(String maHD) {
    yeuCauNhanVienBanHang();
    return giaoDichDao.chiTietHoaDon(maHD);
  }

  /**
   * Phiếu đổi và phiếu trả lập trên các hóa đơn của khách (luồng 9.a); rỗng ứng với luồng 9.b.
   *
   * @param maKH mã khách hàng
   * @return danh sách phiếu, có thể rỗng
   */
  public List<PhieuTomTat> phieuDoiTra(String maKH) {
    yeuCauNhanVienBanHang();
    return giaoDichDao.phieuCuaKhach(maKH);
  }

  /**
   * Nội dung của một phiếu đổi hoặc phiếu trả (luồng 9.a.4).
   *
   * @param loai loại phiếu
   * @param maPhieu mã phiếu
   * @return các dòng hàng của phiếu, có thể rỗng
   */
  public List<ChiTietPhieuView> chiTietPhieu(LoaiPhieu loai, String maPhieu) {
    yeuCauNhanVienBanHang();
    return giaoDichDao.chiTietPhieu(loai, maPhieu);
  }

  private static void yeuCauNhanVienBanHang() {
    boolean duocPhep = CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_BAN_HANG)
        .isPresent();
    if (!duocPhep) {
      throw new SecurityException(LOI_QUYEN);
    }
  }
}
