package com.dtaq.retail.controller;

import com.dtaq.retail.dao.PhieuDoiDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.session.CurrentSession;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Xử lý nghiệp vụ duyệt yêu cầu trả hàng và đổi hàng (UC018); chỉ Nhân viên quản lý được dùng.
 *
 * <p>Bộ điều khiển chỉ đọc danh sách phiếu đang chờ duyệt từ hai DAO tương ứng và ghi quyết định
 * duyệt/từ chối vào đúng một dòng của phiếu được chọn. Mọi thay đổi tồn kho, hóa đơn hay sản phẩm
 * thuộc về nghiệp vụ hoàn tất phiếu, không nằm trong phạm vi phê duyệt này.
 */
public final class PheDuyetDoiTraController {

  private static final String LOI_QUYEN = "Chỉ Quản lý được phê duyệt yêu cầu đổi trả";
  private static final String LOI_MA_PHIEU = "Mã phiếu không hợp lệ";
  private static final String LOI_THIEU_LY_DO = "Vui lòng nhập lý do từ chối";
  private static final String TIEN_TO_PHIEU_TRA = "PT";
  private static final String TIEN_TO_PHIEU_DOI = "PD";

  private static final Comparator<PhieuDuyet> THU_TU_HIEN_THI =
      Comparator.comparing(PhieuDuyet::ngayTao, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(PhieuDuyet::maPhieu, Comparator.nullsLast(Comparator.naturalOrder()));

  private final PhieuTraDao phieuTraDao;
  private final PhieuDoiDao phieuDoiDao;
  private final Clock clock;

  /**
   * Khởi tạo bộ điều khiển phê duyệt đổi trả.
   *
   * @param phieuTraDao lớp truy cập phiếu trả; không được {@code null}
   * @param phieuDoiDao lớp truy cập phiếu đổi; không được {@code null}
   * @param clock đồng hồ dùng để lấy ngày duyệt; {@code null} thì dùng đồng hồ hệ thống
   */
  public PheDuyetDoiTraController(PhieuTraDao phieuTraDao, PhieuDoiDao phieuDoiDao, Clock clock) {
    this.phieuTraDao = Objects.requireNonNull(phieuTraDao, "phieuTraDao");
    this.phieuDoiDao = Objects.requireNonNull(phieuDoiDao, "phieuDoiDao");
    this.clock = clock == null ? Clock.systemDefaultZone() : clock;
  }

  /**
   * Lấy danh sách yêu cầu trả hàng và đổi hàng đang chờ Quản lý phê duyệt.
   *
   * <p>Hai nguồn được gộp lại và sắp xếp theo ngày tạo tăng dần, cùng ngày thì theo mã phiếu.
   *
   * @return danh sách chỉ đọc, không bao giờ {@code null}, có thể rỗng
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên quản lý
   */
  public List<PhieuDuyet> danhSachChoDuyet() {
    yeuCauQuanLy();
    List<PhieuDuyet> tatCa = new ArrayList<>();
    tatCa.addAll(anToan(phieuTraDao.findChoDuyet()));
    tatCa.addAll(anToan(phieuDoiDao.findChoDuyet()));
    tatCa.sort(THU_TU_HIEN_THI);
    return List.copyOf(tatCa);
  }

  /**
   * Ghi quyết định duyệt hoặc từ chối cho một phiếu trả/phiếu đổi.
   *
   * @param maPhieu mã phiếu cần xử lý ({@code PT-...} là phiếu trả, {@code PD-...} là phiếu đổi)
   * @param duyet {@code true} để duyệt, {@code false} để từ chối
   * @param yKien ý kiến của Quản lý; khi từ chối thì bắt buộc phải có lý do
   * @return kết quả xử lý kèm thông báo hiển thị cho người dùng
   * @throws SecurityException nếu phiên hiện tại không phải Nhân viên quản lý
   */
  public KetQua quyetDinh(String maPhieu, boolean duyet, String yKien) {
    yeuCauQuanLy();

    String maDaTrim = maPhieu == null ? "" : maPhieu.trim();
    String maChuanHoa = maDaTrim.toUpperCase(Locale.ROOT);
    if (maChuanHoa.isEmpty()
        || (!maChuanHoa.startsWith(TIEN_TO_PHIEU_TRA) && !maChuanHoa.startsWith(TIEN_TO_PHIEU_DOI))) {
      return new KetQua(false, LOI_MA_PHIEU);
    }

    String yKienDaTrim = yKien == null || yKien.isBlank() ? null : yKien.trim();
    if (!duyet && yKienDaTrim == null) {
      return new KetQua(false, LOI_THIEU_LY_DO);
    }

    TrangThaiDuyet trangThai = duyet ? TrangThaiDuyet.DA_DUYET : TrangThaiDuyet.TU_CHOI;
    boolean laPhieuTra = maChuanHoa.startsWith(TIEN_TO_PHIEU_TRA);
    boolean thanhCong = laPhieuTra
        ? phieuTraDao.capNhatQuyetDinh(
            maDaTrim, trangThai, CurrentSession.require().maNV(), LocalDate.now(clock), yKienDaTrim)
        : phieuDoiDao.capNhatQuyetDinh(
            maDaTrim, trangThai, CurrentSession.require().maNV(), LocalDate.now(clock), yKienDaTrim);

    if (!thanhCong) {
      return new KetQua(false,
          "Phiếu " + maPhieu + " đã được xử lý bởi người khác. Danh sách đã được tải lại.");
    }
    return duyet
        ? new KetQua(true, "Đã phê duyệt phiếu " + maPhieu)
        : new KetQua(true, "Đã từ chối phiếu " + maPhieu);
  }

  /** Kết quả một lần phê duyệt, dùng trực tiếp cho thông báo trên giao diện. */
  public record KetQua(boolean thanhCong, String thongBao) {
  }

  private static void yeuCauQuanLy() {
    boolean laQuanLy = CurrentSession.get()
        .map(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElse(false);
    if (!laQuanLy) {
      throw new SecurityException(LOI_QUYEN);
    }
  }

  private static Collection<PhieuDuyet> anToan(Collection<PhieuDuyet> danhSach) {
    return danhSach == null ? List.of() : danhSach;
  }
}
