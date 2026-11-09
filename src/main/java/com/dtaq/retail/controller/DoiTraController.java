package com.dtaq.retail.controller;

import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.PhieuDoiDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.MaGenerator;
import com.dtaq.retail.util.TienUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
 * Xử lý nghiệp vụ đổi/trả sản phẩm (UC006): tra cứu hóa đơn gốc, kiểm điều kiện, tính tiền hoàn
 * hoặc chênh lệch, ghi phiếu và hoàn tất phiếu.
 *
 * <p>Chỉ Nhân viên bán hàng được dùng mọi thao tác của bộ điều khiển; quyền được kiểm tra ở từng
 * phương thức công khai bằng {@link CurrentSession}, không dựa vào việc ẩn nút trên giao diện.
 *
 * <p>Tiền được xử lý hoàn toàn bằng {@link BigDecimal}. Giá trị máy trả lấy từ snapshot của hóa đơn
 * gốc ({@code tienSauKhuyenMai + tienThue}) và <b>không làm tròn lại</b> (C-11); giá trị máy mới
 * được tính qua {@link BanHangController#tinhDong} — điểm làm tròn duy nhất theo quy ước dự án.
 *
 * <p>Mọi thao tác ghi nhiều bảng nằm trong một transaction do bộ điều khiển mở trên
 * {@link DataSource}; bộ đếm tồn {@code DongSanPham.soLuongTon} do trigger V6 tự tính lại, bộ điều
 * khiển không gọi {@code updateSoLuongTon}.
 */
public final class DoiTraController {

  private static final Logger log = LoggerFactory.getLogger(DoiTraController.class);

  private static final String LOI_QUYEN = "Chỉ Nhân viên bán hàng được xử lý đổi trả";
  private static final String LOI_HOA_DON_CHUA_THANH_TOAN =
      "Hóa đơn chưa thanh toán, không đủ căn cứ xử lý đổi/trả";
  private static final String LOI_PHIEU_CHUA_DUYET =
      "Phiếu chưa được Quản lý phê duyệt, không thể hoàn tất";
  private static final String LOI_PHIEU_DA_HOAN_TAT =
      "Phiếu đã được hoàn tất bởi người khác. Danh sách đã được tải lại.";
  private static final String TIEN_TO_PHIEU_TRA = "PT-";
  private static final String TIEN_TO_PHIEU_DOI = "PD-";

  /** Ngưỡng hoàn tiền phải có Quản lý phê duyệt (UC006 bước 10.a, BR-07). */
  private static final BigDecimal NGUONG_PHE_DUYET = new BigDecimal("3000000");

  private static final Comparator<PhieuDuyet> THU_TU_HIEN_THI =
      Comparator.comparing(PhieuDuyet::ngayTao, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(PhieuDuyet::maPhieu, Comparator.nullsLast(Comparator.naturalOrder()));

  private final PhieuTraDao phieuTraDao;
  private final PhieuDoiDao phieuDoiDao;
  private final SanPhamLoiDao sanPhamLoiDao;
  private final HoaDonDao hoaDonDao;
  private final ChiTietHoaDonDao chiTietHoaDonDao;
  private final SanPhamDao sanPhamDao;
  private final DongSanPhamDao dongSanPhamDao;
  private final DataSource dataSource;
  private final int soNgayDoiTraToiDa;
  private final Clock clock;

  /**
   * Khởi tạo bộ điều khiển đổi/trả.
   *
   * @param phieuTraDao lớp truy cập phiếu trả; không được {@code null}
   * @param phieuDoiDao lớp truy cập phiếu đổi; không được {@code null}
   * @param sanPhamLoiDao lớp ghi nhận sản phẩm lỗi; không được {@code null}
   * @param hoaDonDao lớp tra cứu hóa đơn; không được {@code null}
   * @param chiTietHoaDonDao lớp tra cứu chi tiết hóa đơn; không được {@code null}
   * @param sanPhamDao lớp tra cứu máy; không được {@code null}
   * @param dongSanPhamDao lớp tra cứu dòng sản phẩm; không được {@code null}
   * @param dataSource nguồn dữ liệu dùng mở transaction ghi phiếu; không được {@code null}
   * @param soNgayDoiTraToiDa số ngày còn được đổi/trả; {@code <= 0} nghĩa là không chặn
   * @param clock đồng hồ xác định ngày lập/hoàn tất phiếu; {@code null} thì dùng đồng hồ hệ thống
   */
  public DoiTraController(
      PhieuTraDao phieuTraDao,
      PhieuDoiDao phieuDoiDao,
      SanPhamLoiDao sanPhamLoiDao,
      HoaDonDao hoaDonDao,
      ChiTietHoaDonDao chiTietHoaDonDao,
      SanPhamDao sanPhamDao,
      DongSanPhamDao dongSanPhamDao,
      DataSource dataSource,
      int soNgayDoiTraToiDa,
      Clock clock) {
    this.phieuTraDao = Objects.requireNonNull(phieuTraDao, "phieuTraDao");
    this.phieuDoiDao = Objects.requireNonNull(phieuDoiDao, "phieuDoiDao");
    this.sanPhamLoiDao = Objects.requireNonNull(sanPhamLoiDao, "sanPhamLoiDao");
    this.hoaDonDao = Objects.requireNonNull(hoaDonDao, "hoaDonDao");
    this.chiTietHoaDonDao = Objects.requireNonNull(chiTietHoaDonDao, "chiTietHoaDonDao");
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    this.soNgayDoiTraToiDa = soNgayDoiTraToiDa;
    this.clock = clock == null ? Clock.systemDefaultZone() : clock;
  }

  /**
   * Tra cứu hóa đơn gốc và liệt kê các máy đã bán của hóa đơn đó.
   *
   * @param maHD mã hóa đơn gốc; có thể {@code null} hoặc rỗng
   * @return kết quả tra cứu kèm danh sách máy và cờ "đã đổi/trả" cho từng máy
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên bán hàng
   */
  public KetQuaTraCuu traCuuHoaDon(String maHD) {
    yeuCauBanHang();
    String maChuanHoa = chuanHoa(maHD);
    if (maChuanHoa.isEmpty()) {
      return new KetQuaTraCuu(false, "Vui lòng nhập mã hóa đơn gốc", null, null, null, null,
          List.of());
    }

    Optional<HoaDon> hoaDonOpt = hoaDonDao.findById(maChuanHoa);
    if (hoaDonOpt.isEmpty()) {
      return new KetQuaTraCuu(false, "Không tìm thấy hóa đơn " + maChuanHoa, maChuanHoa, null,
          null, null, List.of());
    }
    HoaDon hoaDon = hoaDonOpt.get();
    if (!hoaDon.trangThaiThanhToan()) {
      return new KetQuaTraCuu(false, LOI_HOA_DON_CHUA_THANH_TOAN, maChuanHoa, null, null, null,
          List.of());
    }

    List<ChiTietHoaDon> chiTiet = anToan(chiTietHoaDonDao.findByHoaDon(maChuanHoa));
    List<String> serials = chiTiet.stream()
        .map(DoiTraController::serialCua)
        .filter(Objects::nonNull)
        .toList();
    Set<String> daDoiTra = serialsDaDoiTra(maChuanHoa, serials);

    List<MayDaBan> mayDaBan = new ArrayList<>();
    for (ChiTietHoaDon dong : chiTiet) {
      String soSerial = serialCua(dong);
      if (soSerial == null) {
        continue;
      }
      String tenDong = dong.sanPham() != null && dong.sanPham().dongSanPham() != null
          ? dong.sanPham().dongSanPham().tenDongSP()
          : null;
      TrangThaiSanPham trangThai = dong.sanPham() == null ? null : dong.sanPham().trangThai();
      mayDaBan.add(new MayDaBan(
          soSerial, tenDong, giaTriDong(dong), daDoiTra.contains(soSerial), trangThai));
    }

    KhachHang khachHang = hoaDon.khachHang();
    return new KetQuaTraCuu(
        true,
        "Tìm thấy hóa đơn " + hoaDon.maHD(),
        hoaDon.maHD(),
        khachHang == null ? null : khachHang.hoTen(),
        khachHang == null ? null : khachHang.soDienThoai(),
        hoaDon.ngayLap(),
        List.copyOf(mayDaBan));
  }

  /**
   * Kiểm tra điều kiện và tính trước tiền hoàn/chênh lệch của một yêu cầu đổi/trả; không ghi dữ liệu.
   *
   * @param yeuCau yêu cầu đổi/trả; có thể {@code null}
   * @return kết quả tính; khi không hợp lệ thì {@link KetQuaTinh#hopLe()} là {@code false}
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên bán hàng
   */
  public KetQuaTinh tinhPhieu(YeuCauDoiTra yeuCau) {
    yeuCauBanHang();
    if (yeuCau == null) {
      return tinhKhongHopLe("Yêu cầu đổi/trả không hợp lệ");
    }
    String maHD = chuanHoa(yeuCau.maHD());
    if (maHD.isEmpty()) {
      return tinhKhongHopLe("Vui lòng nhập mã hóa đơn gốc");
    }
    String soSerial = chuanHoa(yeuCau.soSerial());
    if (soSerial.isEmpty()) {
      return tinhKhongHopLe("Vui lòng chọn máy cần đổi/trả");
    }
    if (yeuCau.tinhTrang() == null) {
      return tinhKhongHopLe("Vui lòng chọn tình trạng hàng trả");
    }
    if (chuanHoa(yeuCau.lyDo()).isEmpty()) {
      return tinhKhongHopLe("Vui lòng nhập lý do đổi/trả");
    }

    Optional<HoaDon> hoaDonOpt = hoaDonDao.findById(maHD);
    if (hoaDonOpt.isEmpty()) {
      return tinhKhongHopLe("Không tìm thấy hóa đơn " + maHD);
    }
    HoaDon hoaDon = hoaDonOpt.get();
    if (!hoaDon.trangThaiThanhToan()) {
      return tinhKhongHopLe(LOI_HOA_DON_CHUA_THANH_TOAN);
    }

    Optional<ChiTietHoaDon> dongOpt = timDongHoaDon(maHD, soSerial);
    if (dongOpt.isEmpty()) {
      return tinhKhongHopLe("Máy " + soSerial + " không thuộc hóa đơn " + maHD);
    }
    ChiTietHoaDon dong = dongOpt.get();
    if (serialDaDoiTra(maHD, soSerial)) {
      return tinhKhongHopLe("Máy " + soSerial + " đã được đổi/trả trước đó");
    }
    if (dong.sanPham() == null || dong.sanPham().trangThai() != TrangThaiSanPham.DA_BAN) {
      return tinhKhongHopLe("Máy " + soSerial + " không còn ở trạng thái đã bán");
    }
    if (quaThoiHan(hoaDon)) {
      return tinhKhongHopLe("Đã hết thời hạn đổi/trả (" + soNgayDoiTraToiDa + " ngày)");
    }

    BigDecimal giaTriMayTra = giaTriDong(dong);
    String soSerialMayThayThe = chuanHoa(yeuCau.soSerialMayThayThe());
    if (soSerialMayThayThe.isEmpty()) {
      BigDecimal chenhLech = giaTriMayTra.negate();
      return new KetQuaTinh(
          true,
          "Đủ điều kiện trả hàng",
          giaTriMayTra,
          BigDecimal.ZERO,
          chenhLech,
          giaTriMayTra,
          canDuyet(giaTriMayTra));
    }
    if (soSerialMayThayThe.equals(soSerial)) {
      return tinhKhongHopLe("Máy thay thế phải khác máy trả");
    }
    Optional<SanPham> mayMoiOpt = sanPhamDao.findBySerial(soSerialMayThayThe);
    if (mayMoiOpt.isEmpty()) {
      return tinhKhongHopLe("Không tìm thấy máy thay thế " + soSerialMayThayThe);
    }
    SanPham mayMoi = mayMoiOpt.get();
    if (mayMoi.trangThai() != TrangThaiSanPham.SAN_SANG_BAN) {
      return tinhKhongHopLe("Máy thay thế " + soSerialMayThayThe + " không sẵn sàng bán");
    }
    Optional<DongSanPham> dongMoiOpt = dongSanPhamDayDu(mayMoi);
    if (dongMoiOpt.isEmpty()) {
      return tinhKhongHopLe(
          "Không tìm thấy dòng sản phẩm của máy thay thế " + soSerialMayThayThe);
    }
    DongSanPham dongMoi = dongMoiOpt.get();
    BanHangController.DongTinh tinh =
        BanHangController.tinhDong(dongMoi.donGiaBanTruocThue(), dongMoi.thue(), 1, BigDecimal.ZERO);
    BigDecimal giaTriMayMoi = tinh.tienSauKhuyenMai().add(tinh.tienThue());
    if (giaTriMayMoi.compareTo(giaTriMayTra) > 0) {
      // D17 (2026-11-05): đổi hàng chỉ sang máy ngang bằng hoặc thấp hơn, không hoàn tiền.
      return tinhKhongHopLe(
          "Máy thay thế phải có giá trị ngang bằng hoặc thấp hơn máy trả; "
              + "đổi hàng không hoàn tiền");
    }
    BigDecimal chenhLech = giaTriMayMoi.subtract(giaTriMayTra);
    return new KetQuaTinh(
        true,
        "Đủ điều kiện đổi máy",
        giaTriMayTra,
        giaTriMayMoi,
        chenhLech,
        BigDecimal.ZERO,
        false);
  }

  /**
   * Ghi phiếu đổi/trả trong một transaction duy nhất.
   *
   * <p>Phiếu dưới ngưỡng phê duyệt được hoàn tất ngay: cập nhật trạng thái máy nhận lại (và máy
   * thay thế nếu là phiếu đổi), ghi nhận {@code SanPhamLoi} khi hàng lỗi. Phiếu trả từ 3.000.000
   * đồng tiền hoàn trở lên chỉ được lưu ở trạng thái chờ duyệt và <b>không</b> đụng tới máy (UC006
   * 10.a). Phiếu đổi luôn hoàn tất ngay và không cần duyệt, không hoàn/thu tiền (D17, 2026-11-05).
   *
   * @param yeuCau yêu cầu đổi/trả cần ghi
   * @return kết quả ghi kèm mã phiếu và số tiền; khi không hợp lệ thì {@code thanhCong = false}
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên bán hàng
   */
  public KetQuaGhi ghiPhieu(YeuCauDoiTra yeuCau) {
    yeuCauBanHang();
    KetQuaTinh tinh = tinhPhieu(yeuCau);
    if (!tinh.hopLe()) {
      return ghiKhongThanhCong(tinh.thongBao());
    }

    String maHD = chuanHoa(yeuCau.maHD());
    String soSerial = chuanHoa(yeuCau.soSerial());
    String lyDo = chuanHoa(yeuCau.lyDo());
    TinhTrangHangTra tinhTrang = yeuCau.tinhTrang();
    String soSerialMayThayThe = chuanHoa(yeuCau.soSerialMayThayThe());
    boolean doiMay = !soSerialMayThayThe.isEmpty();

    Optional<HoaDon> hoaDonOpt = hoaDonDao.findById(maHD);
    if (hoaDonOpt.isEmpty()) {
      return ghiKhongThanhCong("Không tìm thấy hóa đơn " + maHD);
    }
    HoaDon hoaDon = hoaDonOpt.get();
    Optional<ChiTietHoaDon> dongOpt = timDongHoaDon(maHD, soSerial);
    if (dongOpt.isEmpty()) {
      return ghiKhongThanhCong("Máy " + soSerial + " không thuộc hóa đơn " + maHD);
    }
    ChiTietHoaDon dong = dongOpt.get();
    SanPham mayThayThe = null;
    if (doiMay) {
      mayThayThe = sanPhamDao.findBySerial(soSerialMayThayThe).orElse(null);
      if (mayThayThe == null) {
        return ghiKhongThanhCong("Không tìm thấy máy thay thế " + soSerialMayThayThe);
      }
    }

    NhanVien nhanVien = CurrentSession.require();
    LocalDate homNay = LocalDate.now(clock);
    boolean canDuyet = tinh.canDuyet();
    TrangThaiDuyet trangThaiDuyet =
        canDuyet ? TrangThaiDuyet.CHO_DUYET : TrangThaiDuyet.KHONG_CAN_DUYET;
    TrangThaiXuLyPhieu trangThaiXuLy =
        canDuyet ? TrangThaiXuLyPhieu.CHO_XU_LY : TrangThaiXuLyPhieu.HOAN_TAT;
    LocalDate ngayHoanTat = canDuyet ? null : homNay;
    BigDecimal soTienHoan = tinh.soTienHoan();
    BigDecimal chenhLech = tinh.chenhLech();

    try (Connection ketNoi = dataSource.getConnection()) {
      boolean autoCommitCu = ketNoi.getAutoCommit();
      try {
        ketNoi.setAutoCommit(false);
        String maPhieu;
        if (doiMay) {
          // D17 (2026-11-05): đổi hàng không thu thêm và không phát sinh hoàn tiền.
          BigDecimal tienThuThem = BigDecimal.ZERO;
          PhieuDoi header = new PhieuDoi(
              null,
              nhanVien,
              hoaDon,
              ngayHoanTat,
              homNay,
              trangThaiDuyet,
              trangThaiXuLy,
              tienThuThem,
              chenhLech,
              null,
              null,
              null);
          ChiTietPhieuDoi chiTiet =
              new ChiTietPhieuDoi(header, dong, lyDo, tinhTrang, mayThayThe);
          maPhieu = phieuDoiDao.taoPhieuDoi(ketNoi, header, List.of(chiTiet));
          if (!canDuyet) {
            phieuDoiDao.danhDauMayNhanLai(ketNoi, List.of(chiTiet));
            phieuDoiDao.danhDauMayThayThe(ketNoi, List.of(chiTiet));
            if (tinhTrang == TinhTrangHangTra.HANG_LOI) {
              PhieuDoi daLuu = new PhieuDoi(
                  maPhieu,
                  nhanVien,
                  hoaDon,
                  ngayHoanTat,
                  homNay,
                  trangThaiDuyet,
                  trangThaiXuLy,
                  tienThuThem,
                  chenhLech,
                  null,
                  null,
                  null);
              ghiSanPhamLoi(ketNoi, dong, null, daLuu, lyDo, homNay);
            }
          }
        } else {
          PhieuTra header = new PhieuTra(
              null,
              nhanVien,
              hoaDon,
              homNay,
              ngayHoanTat,
              trangThaiDuyet,
              trangThaiXuLy,
              null,
              null,
              null,
              soTienHoan);
          ChiTietPhieuTra chiTiet =
              new ChiTietPhieuTra(header, dong, tinhTrang, lyDo);
          maPhieu = phieuTraDao.taoPhieuTra(ketNoi, header, List.of(chiTiet));
          if (!canDuyet) {
            phieuTraDao.danhDauMayNhanLai(ketNoi, List.of(chiTiet));
            if (tinhTrang == TinhTrangHangTra.HANG_LOI) {
              PhieuTra daLuu = new PhieuTra(
                  maPhieu,
                  nhanVien,
                  hoaDon,
                  homNay,
                  ngayHoanTat,
                  trangThaiDuyet,
                  trangThaiXuLy,
                  null,
                  null,
                  null,
                  soTienHoan);
              ghiSanPhamLoi(ketNoi, dong, daLuu, null, lyDo, homNay);
            }
          }
        }
        ketNoi.commit();
        String thongBao;
        if (doiMay) {
          thongBao = "Đã lập phiếu đổi " + maPhieu + ". Chênh lệch "
              + TienUtil.dinhDang(chenhLech) + " không hoàn lại.";
        } else if (canDuyet) {
          thongBao = "Đã lập phiếu " + maPhieu + " và chuyển Quản lý phê duyệt";
        } else {
          thongBao = "Đã hoàn tất phiếu " + maPhieu;
        }
        return new KetQuaGhi(true, thongBao, maPhieu, soTienHoan, chenhLech, canDuyet);
      } catch (IllegalStateException e) {
        // Guard của DAO (máy đã có phiếu / không còn trạng thái phù hợp) là lỗi nghiệp vụ:
        // quay lui transaction rồi trả kết quả thất bại cho giao diện thay vì ném ra ngoài.
        try {
          ketNoi.rollback();
        } catch (SQLException loiQuayLui) {
          e.addSuppressed(loiQuayLui);
        }
        String thongBao = e.getMessage() == null || e.getMessage().isBlank()
            ? "Máy đã được đổi/trả trước đó" : e.getMessage();
        return new KetQuaGhi(false, thongBao, null, null, null, false);
      } catch (RuntimeException | SQLException e) {
        try {
          ketNoi.rollback();
        } catch (SQLException loiQuayLui) {
          e.addSuppressed(loiQuayLui);
        }
        if (e instanceof RuntimeException loi) {
          throw loi;
        }
        throw new DataAccessException(
            "Không thể ghi phiếu đổi/trả cho hóa đơn " + maHD, e);
      } finally {
        try {
          ketNoi.setAutoCommit(autoCommitCu);
        } catch (SQLException ignored) {
          // Kết nối được đóng ngay sau đây; không còn thao tác nào phụ thuộc chế độ cam kết.
        }
      }
    } catch (SQLException e) {
      log.error("Không mở được kết nối để ghi phiếu đổi/trả cho hóa đơn {}", maHD, e);
      throw new DataAccessException("Không thể xử lý đổi/trả cho hóa đơn " + maHD, e);
    }
  }

  /**
   * Hoàn tất một phiếu đã được Quản lý phê duyệt.
   *
   * <p>Chỉ phiếu ở trạng thái {@code DA_DUYET} và {@code CHO_XU_LY} mới được hoàn tất. Toàn bộ việc
   * cập nhật máy, ghi nhận hàng lỗi và đổi trạng thái phiếu nằm trong một transaction; nếu phiếu vừa
   * bị người khác hoàn tất thì giao dịch bị quay lui (CAS ở tầng DAO).
   *
   * <p>D17 (2026-11-05): phiếu đổi ({@code PD-...}) không bao giờ hoàn tiền nên
   * {@link KetQuaGhi#soTienHoan()} luôn bằng {@code 0}; chênh lệch vẫn được trả về để hiển thị.
   *
   * @param maPhieu mã phiếu trả ({@code PT-...}) hoặc phiếu đổi ({@code PD-...})
   * @return kết quả hoàn tất kèm thông báo hiển thị cho người dùng
   * @throws IllegalStateException nếu {@code capNhatHoanTat} không cập nhật được dòng nào vì phiếu
   *     vừa được người khác hoàn tất (CAS thua); transaction đã được quay lui trước khi ném
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên bán hàng
   */
  public KetQuaGhi hoanTatPhieu(String maPhieu) {
    yeuCauBanHang();
    String ma = chuanHoa(maPhieu);
    String maHoa = ma.toUpperCase(Locale.ROOT);
    boolean laPhieuTra = maHoa.startsWith(TIEN_TO_PHIEU_TRA);
    boolean laPhieuDoi = maHoa.startsWith(TIEN_TO_PHIEU_DOI);
    if (!laPhieuTra && !laPhieuDoi) {
      return ghiKhongThanhCong("Mã phiếu không hợp lệ");
    }
    LocalDate homNay = LocalDate.now(clock);

    if (laPhieuTra) {
      Optional<PhieuTra> phieuOpt = phieuTraDao.findById(ma);
      if (phieuOpt.isEmpty()) {
        return ghiKhongThanhCong("Không tìm thấy phiếu " + ma);
      }
      PhieuTra phieu = phieuOpt.get();
      KetQuaGhi loi = kiemTraDieuKienHoanTat(
          phieu.trangThaiDuyet(), phieu.trangThaiXuLyPhieu());
      if (loi != null) {
        return loi;
      }
      List<ChiTietPhieuTra> chiTiet = anToan(phieuTraDao.findChiTiet(ma));
      BigDecimal soTienHoan =
          phieu.tienKhachDua() == null ? BigDecimal.ZERO : phieu.tienKhachDua();
      BigDecimal chenhLech = soTienHoan.negate();
      return ghiHoanTatPhieuTra(ma, phieu, chiTiet, soTienHoan, chenhLech, homNay);
    }

    Optional<PhieuDoi> phieuOpt = phieuDoiDao.findById(ma);
    if (phieuOpt.isEmpty()) {
      return ghiKhongThanhCong("Không tìm thấy phiếu " + ma);
    }
    PhieuDoi phieu = phieuOpt.get();
    KetQuaGhi loi = kiemTraDieuKienHoanTat(
        phieu.trangThaiDuyet(), phieu.trangThaiXuLyPhieu());
    if (loi != null) {
      return loi;
    }
    List<ChiTietPhieuDoi> chiTiet = anToan(phieuDoiDao.findChiTiet(ma));
    BigDecimal chenhLech = phieu.tienChenhLech() == null ? BigDecimal.ZERO : phieu.tienChenhLech();
    // D17 (2026-11-05): phiếu đổi không hoàn tiền; chênh lệch âm chỉ để hiển thị.
    BigDecimal soTienHoan = BigDecimal.ZERO;
    return ghiHoanTatPhieuDoi(ma, phieu, chiTiet, soTienHoan, chenhLech, homNay);
  }

  /**
   * Gộp danh sách phiếu trả và phiếu đổi đã được duyệt, đang chờ hoàn tất.
   *
   * <p>Kết quả sắp xếp theo ngày tạo tăng dần, cùng ngày thì theo mã phiếu.
   *
   * @return danh sách chỉ đọc, không bao giờ {@code null}, có thể rỗng
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên bán hàng
   */
  public List<PhieuDuyet> danhSachChoHoanTat() {
    yeuCauBanHang();
    List<PhieuDuyet> tatCa = new ArrayList<>();
    tatCa.addAll(anToan(phieuTraDao.findChoHoanTat()));
    tatCa.addAll(anToan(phieuDoiDao.findChoHoanTat()));
    tatCa.sort(THU_TU_HIEN_THI);
    return List.copyOf(tatCa);
  }

  private KetQuaGhi ghiHoanTatPhieuTra(
      String maPhieu,
      PhieuTra phieu,
      List<ChiTietPhieuTra> chiTiet,
      BigDecimal soTienHoan,
      BigDecimal chenhLech,
      LocalDate homNay) {
    try (Connection ketNoi = dataSource.getConnection()) {
      boolean autoCommitCu = ketNoi.getAutoCommit();
      try {
        ketNoi.setAutoCommit(false);
        if (!chiTiet.isEmpty()) {
          phieuTraDao.danhDauMayNhanLai(ketNoi, chiTiet);
          for (ChiTietPhieuTra dong : chiTiet) {
            if (dong.tinhTrangHangTra() == TinhTrangHangTra.HANG_LOI) {
              ghiSanPhamLoi(ketNoi, dong.chiTietHoaDon(), phieu, null, dong.lyDoTra(), homNay);
            }
          }
        }
        if (!phieuTraDao.capNhatHoanTat(ketNoi, maPhieu, homNay)) {
          throw new IllegalStateException(LOI_PHIEU_DA_HOAN_TAT);
        }
        ketNoi.commit();
        return new KetQuaGhi(
            true, "Đã hoàn tất phiếu " + maPhieu, maPhieu, soTienHoan, chenhLech, false);
      } catch (RuntimeException | SQLException e) {
        quayLui(ketNoi, e);
        if (e instanceof RuntimeException loi) {
          throw loi;
        }
        throw new DataAccessException("Không thể hoàn tất phiếu trả " + maPhieu, e);
      } finally {
        khoiPhucAutoCommit(ketNoi, autoCommitCu);
      }
    } catch (SQLException e) {
      log.error("Không mở được kết nối để hoàn tất phiếu trả {}", maPhieu, e);
      throw new DataAccessException("Không thể hoàn tất phiếu trả " + maPhieu, e);
    }
  }

  private KetQuaGhi ghiHoanTatPhieuDoi(
      String maPhieu,
      PhieuDoi phieu,
      List<ChiTietPhieuDoi> chiTiet,
      BigDecimal soTienHoan,
      BigDecimal chenhLech,
      LocalDate homNay) {
    try (Connection ketNoi = dataSource.getConnection()) {
      boolean autoCommitCu = ketNoi.getAutoCommit();
      try {
        ketNoi.setAutoCommit(false);
        if (!chiTiet.isEmpty()) {
          phieuDoiDao.danhDauMayNhanLai(ketNoi, chiTiet);
          phieuDoiDao.danhDauMayThayThe(ketNoi, chiTiet);
          for (ChiTietPhieuDoi dong : chiTiet) {
            if (dong.tinhTrangHangTra() == TinhTrangHangTra.HANG_LOI) {
              ghiSanPhamLoi(ketNoi, dong.chiTietHoaDon(), null, phieu, dong.lyDoDoi(), homNay);
            }
          }
        }
        if (!phieuDoiDao.capNhatHoanTat(ketNoi, maPhieu, homNay)) {
          throw new IllegalStateException(LOI_PHIEU_DA_HOAN_TAT);
        }
        ketNoi.commit();
        return new KetQuaGhi(
            true, "Đã hoàn tất phiếu " + maPhieu, maPhieu, soTienHoan, chenhLech, false);
      } catch (RuntimeException | SQLException e) {
        quayLui(ketNoi, e);
        if (e instanceof RuntimeException loi) {
          throw loi;
        }
        throw new DataAccessException("Không thể hoàn tất phiếu đổi " + maPhieu, e);
      } finally {
        khoiPhucAutoCommit(ketNoi, autoCommitCu);
      }
    } catch (SQLException e) {
      log.error("Không mở được kết nối để hoàn tất phiếu đổi {}", maPhieu, e);
      throw new DataAccessException("Không thể hoàn tất phiếu đổi " + maPhieu, e);
    }
  }

  private static KetQuaGhi kiemTraDieuKienHoanTat(
      TrangThaiDuyet trangThaiDuyet, TrangThaiXuLyPhieu trangThaiXuLy) {
    // Kiểm trạng thái xử lý trước để phiếu đã hoàn tất không bị báo nhầm là "chưa được duyệt".
    if (trangThaiXuLy != TrangThaiXuLyPhieu.CHO_XU_LY) {
      return ghiKhongThanhCong(LOI_PHIEU_DA_HOAN_TAT);
    }
    if (trangThaiDuyet != TrangThaiDuyet.DA_DUYET) {
      return ghiKhongThanhCong(LOI_PHIEU_CHUA_DUYET);
    }
    return null;
  }

  private void ghiSanPhamLoi(
      Connection ketNoi,
      ChiTietHoaDon dong,
      PhieuTra phieuTra,
      PhieuDoi phieuDoi,
      String lyDo,
      LocalDate ngayGhiNhan) {
    SanPham sanPham = dong.sanPham();
    String maLoi = MaGenerator.next(ketNoi, "SanPhamLoi", "maLoi", "SPL");
    SanPhamLoi sanPhamLoi = new SanPhamLoi(
        maLoi,
        sanPham,
        phieuTra,
        phieuDoi,
        null,
        lyDo,
        ngayGhiNhan,
        TrangThaiXuLyLoi.CHO_XU_LY,
        null);
    sanPhamLoiDao.insert(ketNoi, sanPhamLoi);
  }

  private Optional<ChiTietHoaDon> timDongHoaDon(String maHD, String soSerial) {
    return anToan(chiTietHoaDonDao.findByHoaDon(maHD)).stream()
        .filter(dong -> soSerial.equals(serialCua(dong)))
        .findFirst();
  }

  private boolean serialDaDoiTra(String maHD, String soSerial) {
    return serialsDaDoiTra(maHD, List.of(soSerial)).contains(soSerial);
  }

  private Set<String> serialsDaDoiTra(String maHD, Collection<String> serials) {
    if (serials == null || serials.isEmpty()) {
      return Set.of();
    }
    Set<String> ketQua = new HashSet<>();
    ketQua.addAll(anToan(phieuTraDao.findSerialsDaDoiTra(maHD, serials)));
    ketQua.addAll(anToan(phieuDoiDao.findSerialsDaDoiTra(maHD, serials)));
    return ketQua;
  }

  private Optional<DongSanPham> dongSanPhamDayDu(SanPham sanPham) {
    DongSanPham dong = sanPham.dongSanPham();
    if (dong == null) {
      return Optional.empty();
    }
    if (dong.donGiaBanTruocThue() != null && dong.thue() != null) {
      return Optional.of(dong);
    }
    // Entity máy có thể chỉ mang mã dòng; nạp bản đầy đủ để lấy giá bán và thuế hiện hành.
    return dongSanPhamDao.findById(dong.maDongSP());
  }

  private boolean quaThoiHan(HoaDon hoaDon) {
    if (soNgayDoiTraToiDa <= 0 || hoaDon.ngayLap() == null) {
      return false;
    }
    LocalDate hetHan = hoaDon.ngayLap().plusDays(soNgayDoiTraToiDa);
    return hetHan.isBefore(LocalDate.now(clock));
  }

  private static boolean canDuyet(BigDecimal soTienHoan) {
    return soTienHoan != null && soTienHoan.compareTo(NGUONG_PHE_DUYET) >= 0;
  }

  private static BigDecimal giaTriDong(ChiTietHoaDon dong) {
    BigDecimal sauKhuyenMai =
        dong.tienSauKhuyenMai() == null ? BigDecimal.ZERO : dong.tienSauKhuyenMai();
    BigDecimal thue = dong.tienThue() == null ? BigDecimal.ZERO : dong.tienThue();
    return sauKhuyenMai.add(thue);
  }

  private static String serialCua(ChiTietHoaDon dong) {
    if (dong == null || dong.sanPham() == null) {
      return null;
    }
    String soSerial = dong.sanPham().soSerial();
    return soSerial == null || soSerial.isBlank() ? null : soSerial;
  }

  private static KetQuaTinh tinhKhongHopLe(String thongBao) {
    return new KetQuaTinh(false, thongBao, null, null, null, null, false);
  }

  private static KetQuaGhi ghiKhongThanhCong(String thongBao) {
    return new KetQuaGhi(false, thongBao, null, null, null, false);
  }

  private static String chuanHoa(String giaTri) {
    return giaTri == null ? "" : giaTri.trim();
  }

  private static <T> List<T> anToan(Collection<T> danhSach) {
    return danhSach == null ? List.of() : new ArrayList<>(danhSach);
  }

  private static void quayLui(Connection ketNoi, Exception nguyenNhan) {
    try {
      ketNoi.rollback();
    } catch (SQLException loiQuayLui) {
      nguyenNhan.addSuppressed(loiQuayLui);
    }
  }

  private static void khoiPhucAutoCommit(Connection ketNoi, boolean autoCommitCu) {
    try {
      ketNoi.setAutoCommit(autoCommitCu);
    } catch (SQLException ignored) {
      // Kết nối được đóng ngay sau đây; không còn thao tác nào phụ thuộc chế độ cam kết.
    }
  }

  private static void yeuCauBanHang() {
    boolean laBanHang = CurrentSession.get()
        .map(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_BAN_HANG)
        .orElse(false);
    if (!laBanHang) {
      throw new SecurityException(LOI_QUYEN);
    }
  }

  /**
   * Một máy đã bán trong hóa đơn gốc, dùng cho màn hình tra cứu đổi/trả.
   *
   * @param soSerial số serial của máy
   * @param tenDongSP tên dòng sản phẩm của máy
   * @param giaTriThanhToan giá trị đã thanh toán của dòng ({@code tienSauKhuyenMai + tienThue})
   * @param daDoiTra {@code true} nếu máy đã nằm trên một phiếu trả/đổi trước đó
   * @param trangThai trạng thái hiện tại của máy
   */
  public record MayDaBan(
      String soSerial,
      String tenDongSP,
      BigDecimal giaTriThanhToan,
      boolean daDoiTra,
      TrangThaiSanPham trangThai) {
  }

  /**
   * Yêu cầu đổi/trả do nhân viên bán hàng nhập.
   *
   * @param maHD mã hóa đơn gốc
   * @param soSerial serial máy khách trả lại
   * @param tinhTrang tình trạng máy khách trả lại (bắt buộc)
   * @param lyDo lý do đổi/trả (bắt buộc)
   * @param soSerialMayThayThe serial máy thay thế; {@code null}/rỗng nghĩa là trả hàng
   */
  public record YeuCauDoiTra(
      String maHD,
      String soSerial,
      TinhTrangHangTra tinhTrang,
      String lyDo,
      String soSerialMayThayThe) {
  }

  /**
   * Kết quả tra cứu hóa đơn gốc.
   *
   * @param thanhCong {@code true} khi tìm thấy hóa đơn đã thanh toán
   * @param thongBao thông báo hiển thị cho người dùng
   * @param maHD mã hóa đơn
   * @param tenKhachHang họ tên khách hàng của hóa đơn
   * @param soDienThoai số điện thoại khách hàng
   * @param ngayLap ngày lập hóa đơn
   * @param mayDaBan danh sách máy đã bán kèm cờ đã đổi/trả; rỗng khi tra cứu thất bại
   */
  public record KetQuaTraCuu(
      boolean thanhCong,
      String thongBao,
      String maHD,
      String tenKhachHang,
      String soDienThoai,
      LocalDate ngayLap,
      List<MayDaBan> mayDaBan) {
  }

  /**
   * Kết quả tính trước tiền hoàn/chênh lệch.
   *
   * @param hopLe {@code true} khi yêu cầu đủ điều kiện đổi/trả
   * @param thongBao thông báo hiển thị cho người dùng
   * @param giaTriMayTra giá trị máy khách trả lại theo snapshot hóa đơn gốc
   * @param giaTriMayMoi giá trị máy thay thế; {@code 0} khi trả hàng
   * @param chenhLech hiệu có dấu giữa giá trị máy mới và máy trả
   * @param soTienHoan số tiền cửa hàng phải hoàn cho khách ({@code >= 0})
   * @param canDuyet {@code true} khi tiền hoàn đạt ngưỡng phải Quản lý phê duyệt
   */
  public record KetQuaTinh(
      boolean hopLe,
      String thongBao,
      BigDecimal giaTriMayTra,
      BigDecimal giaTriMayMoi,
      BigDecimal chenhLech,
      BigDecimal soTienHoan,
      boolean canDuyet) {
  }

  /**
   * Kết quả ghi phiếu hoặc hoàn tất phiếu.
   *
   * @param thanhCong {@code true} khi thao tác thành công
   * @param thongBao thông báo hiển thị cho người dùng
   * @param maPhieu mã phiếu vừa lập/hoàn tất
   * @param soTienHoan số tiền hoàn cho khách
   * @param chenhLech chênh lệch có dấu của phiếu đổi
   * @param choDuyet {@code true} khi phiếu đang chờ Quản lý phê duyệt
   */
  public record KetQuaGhi(
      boolean thanhCong,
      String thongBao,
      String maPhieu,
      BigDecimal soTienHoan,
      BigDecimal chenhLech,
      boolean choDuyet) {
  }
}
