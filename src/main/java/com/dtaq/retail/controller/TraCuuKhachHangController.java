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
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
   * Tìm khách theo họ tên (có dấu hay không dấu), mã khách hàng hoặc một phần số điện thoại. Khách
   * trùng khớp chính xác số điện thoại được đưa lên đầu.
   *
   * @param tuKhoa từ khoá do người dùng nhập
   * @param toiDa số kết quả tối đa
   * @return danh sách khách khớp, rỗng nếu từ khoá để trống
   */
  public List<KhachHang> timGanDung(String tuKhoa, int toiDa) {
    yeuCauNhanVienBanHang();
    if (tuKhoa == null || tuKhoa.isBlank()) {
      return List.of();
    }
    String sdt = KiemTraHoSo.chuanHoaSdt(tuKhoa);
    return khachHangDao.findAll().stream()
        .filter(kh -> KiemTraHoSo.khop(tuKhoa, kh.soDienThoai(), kh.hoTen(), kh.maKH()))
        .sorted(Comparator.comparing((KhachHang kh) -> !kh.soDienThoai().equals(sdt)))
        .limit(toiDa)
        .toList();
  }

  /**
   * Tóm tắt hành vi mua của khách từ lịch sử đã tải: chỉ hóa đơn đã thanh toán được tính chi tiêu.
   *
   * @param hoaDons lịch sử hóa đơn
   * @param phieus phiếu đổi/trả
   * @param homNay ngày tham chiếu để tính số ngày từ lần mua gần nhất
   * @return tóm tắt; không bao giờ {@code null}
   */
  public static TongQuanKhach tongQuan(
      List<HoaDonTomTat> hoaDons, List<PhieuTomTat> phieus, LocalDate homNay) {
    List<HoaDonTomTat> daTra = hoaDons.stream().filter(HoaDonTomTat::daThanhToan).toList();
    BigDecimal tong = daTra.stream().map(HoaDonTomTat::tongThanhToan)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal trungBinh = daTra.isEmpty() ? BigDecimal.ZERO
        : tong.divide(BigDecimal.valueOf(daTra.size()), 0, RoundingMode.HALF_UP);
    int soMay = daTra.stream().mapToInt(HoaDonTomTat::soSanPham).sum();
    LocalDate ganNhat = daTra.stream().map(HoaDonTomTat::ngayLap).max(LocalDate::compareTo).orElse(null);
    LocalDate dauTien = daTra.stream().map(HoaDonTomTat::ngayLap).min(LocalDate::compareTo).orElse(null);
    Long soNgay = ganNhat == null ? null : ChronoUnit.DAYS.between(ganNhat, homNay);
    long phieuMo = phieus.stream().filter(p -> p.trangThaiXuLy() == TrangThaiXuLyPhieu.NHAP
        || p.trangThaiXuLy() == TrangThaiXuLyPhieu.CHO_XU_LY).count();
    return new TongQuanKhach(daTra.size(), tong, trungBinh, soMay, ganNhat, dauTien, soNgay,
        phieus.size(), (int) phieuMo);
  }

  /**
   * Tóm tắt khách hàng cho màn hình tra cứu.
   *
   * @param soHoaDon số hóa đơn đã thanh toán
   * @param tongChiTieu tổng thanh toán (VND, đã làm tròn từng hóa đơn)
   * @param trungBinhHoaDon giá trị trung bình mỗi hóa đơn (VND, HALF_UP)
   * @param soMay tổng số máy đã mua
   * @param lanMuaGanNhat ngày mua gần nhất; {@code null} nếu chưa mua
   * @param lanMuaDauTien ngày mua đầu tiên; {@code null} nếu chưa mua
   * @param soNgayTuLanCuoi số ngày từ lần mua gần nhất; {@code null} nếu chưa mua
   * @param soPhieu số phiếu đổi/trả
   * @param soPhieuDangMo số phiếu nháp hoặc chờ xử lý
   */
  public record TongQuanKhach(int soHoaDon, BigDecimal tongChiTieu, BigDecimal trungBinhHoaDon,
      int soMay, LocalDate lanMuaGanNhat, LocalDate lanMuaDauTien, Long soNgayTuLanCuoi,
      int soPhieu, int soPhieuDangMo) {
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
