package com.dtaq.retail.controller;

import com.dtaq.retail.config.BackupSettingsStore;
import com.dtaq.retail.entity.BackupRecord;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.service.BackupScheduler;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.session.CurrentSession;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Điều phối các thao tác sao lưu và phục hồi cơ sở dữ liệu cho giao diện quản lý (FR015, FR016).
 *
 * <p>Mọi phương thức đều chặn quyền trước khi chạm tới dịch vụ: chỉ Nhân viên quản lý mới được sao
 * lưu hoặc phục hồi dữ liệu (NFR003, XBR-13). Lớp này chỉ chuyển tiếp yêu cầu; việc kiểm tra tệp,
 * tạo bản sao lưu an toàn và ghi lịch sử thuộc về {@link BackupService} và {@link RestoreService}.
 * Không có SQL hay thao tác nặng trong lớp này, nên bên gọi vẫn phải chạy nó ngoài luồng giao diện.
 */
public final class BackupController {

  private static final String LOI_QUYEN =
      "Chức năng này chỉ dành cho nhân viên quản lý";

  private final BackupService backupService;
  private final RestoreService restoreService;
  private final BackupScheduler backupScheduler;

  /**
   * Khởi tạo bộ điều phối sao lưu/phục hồi không gắn với bộ lập lịch.
   *
   * <p>Dùng cho các ngữ cảnh chỉ cần sao lưu/phục hồi thủ công hoặc cho kiểm thử; cấu hình lưu từ
   * giao diện sẽ không được áp dụng tức thì vì không có {@link BackupScheduler}.
   *
   * @param backupService dịch vụ sao lưu; không được {@code null}
   * @param restoreService dịch vụ phục hồi; không được {@code null}
   */
  public BackupController(BackupService backupService, RestoreService restoreService) {
    this(backupService, restoreService, null);
  }

  /**
   * Khởi tạo bộ điều phối sao lưu/phục hồi, có thể gắn với bộ lập lịch tự động.
   *
   * @param backupService dịch vụ sao lưu; không được {@code null}
   * @param restoreService dịch vụ phục hồi; không được {@code null}
   * @param backupScheduler bộ lập lịch cần lập lại sau khi lưu cấu hình; có thể {@code null}
   */
  public BackupController(
      BackupService backupService, RestoreService restoreService, BackupScheduler backupScheduler) {
    this.backupService = Objects.requireNonNull(backupService, "backupService");
    this.restoreService = Objects.requireNonNull(restoreService, "restoreService");
    this.backupScheduler = backupScheduler;
  }

  /**
   * Liệt kê các bản sao lưu hiện có trên đĩa, mới nhất trước.
   *
   * @return danh sách bản ghi, có thể rỗng
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   */
  public List<BackupRecord> danhSach() {
    yeuCauQuanLy();
    return backupService.danhSach();
  }

  /**
   * Sao lưu ngay toàn bộ cơ sở dữ liệu theo yêu cầu của quản lý.
   *
   * @return bản ghi sao lưu {@code MANUAL} vừa tạo
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   * @throws com.dtaq.retail.exception.BackupException khi hệ thống đang bận hoặc sao lưu thất bại
   */
  public BackupRecord saoLuuNgay() {
    yeuCauQuanLy();
    return backupService.saoLuu(BackupRecord.LoaiSaoLuu.MANUAL, nhanVienHienTai().maNV());
  }

  /**
   * Kiểm tra một bản sao lưu còn nguyên vẹn và đọc được hay không.
   *
   * @param banGhi bản ghi cần kiểm tra; không được {@code null}
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   * @throws com.dtaq.retail.exception.BackupException khi tệp thiếu, sai kích thước, sai mã băm
   *     hoặc không hợp lệ
   */
  public void kiemTra(BackupRecord banGhi) {
    yeuCauQuanLy();
    backupService.kiemTra(banGhi);
  }

  /**
   * Phục hồi toàn bộ cơ sở dữ liệu từ một bản sao lưu đã chọn.
   *
   * <p>Phục hồi là thao tác phá hủy: dữ liệu sau mốc sao lưu sẽ mất. Vì vậy {@code daXacNhan}
   * phải là {@code true} sau khi người dùng hoàn tất xác nhận hai bước; dịch vụ sẽ ném lỗi nếu
   * thiếu xác nhận.
   *
   * @param banGhi bản sao lưu nguồn; không được {@code null}
   * @param daXacNhan {@code true} khi người dùng đã xác nhận hai bước
   * @return bản ghi lịch sử {@code MANUAL_RESTORE} thành công
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   * @throws com.dtaq.retail.exception.BackupException khi thiếu xác nhận, tệp không hợp lệ hoặc
   *     phục hồi thất bại
   */
  public BackupRecord phucHoi(BackupRecord banGhi, boolean daXacNhan) {
    yeuCauQuanLy();
    return restoreService.phucHoi(banGhi, nhanVienHienTai().maNV(), daXacNhan);
  }

  /**
   * Lưu giờ chạy và chu kỳ sao lưu tự động do Quản lý chọn trên giao diện.
   *
   * <p>Giá trị được ghi bền vào {@code backup.properties} (không chứa mật khẩu), sau đó bộ lập lịch
   * được lập lại ngay để áp dụng giờ và chu kỳ mới mà không cần khởi động lại ứng dụng. Nếu một
   * trong hai khóa {@code DTAQ_BACKUP_TIME}/{@code DTAQ_BACKUP_INTERVAL_HOURS} đang được đặt bằng
   * system property hoặc biến môi trường thì giá trị đó vẫn thắng tệp và giao diện phải cảnh báo;
   * phương thức này không kiểm tra điều đó và không ném lỗi vì lý do đó.
   *
   * <p>Việc ghi tệp là thao tác I/O nên bên gọi phải chạy nó ngoài luồng giao diện.
   *
   * @param gioChay giờ chạy trong ngày; không được {@code null}
   * @param chuKyGio chu kỳ tính bằng giờ, từ {@code 1} đến {@code 168}
   * @throws SecurityException khi phiên hiện tại không phải Nhân viên quản lý
   * @throws IllegalArgumentException khi giờ trống hoặc chu kỳ ngoài khoảng {@code 1..168}
   * @throws java.io.UncheckedIOException khi không ghi được tệp cấu hình
   */
  public void luuCauHinh(LocalTime gioChay, int chuKyGio) {
    yeuCauQuanLy();
    if (gioChay == null) {
      throw new IllegalArgumentException("Giờ chạy không được để trống");
    }
    if (chuKyGio < 1 || chuKyGio > 168) {
      throw new IllegalArgumentException("Chu kỳ phải từ 1 đến 168 giờ");
    }

    BackupSettingsStore.luu(gioChay, chuKyGio);
    if (backupScheduler != null) {
      backupScheduler.lapLichLai();
    }
  }

  /**
   * Bắt buộc phiên đăng nhập hiện tại là Nhân viên quản lý.
   *
   * @return nhân viên quản lý đang đăng nhập
   * @throws SecurityException khi chưa đăng nhập hoặc tài khoản không phải quản lý
   */
  private static NhanVien yeuCauQuanLy() {
    return CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElseThrow(() -> new SecurityException(LOI_QUYEN));
  }

  /**
   * Lấy nhân viên đang đăng nhập sau khi đã kiểm tra quyền quản lý.
   *
   * @return nhân viên quản lý đang đăng nhập
   * @throws SecurityException khi chưa đăng nhập hoặc tài khoản không phải quản lý
   */
  private static NhanVien nhanVienHienTai() {
    return yeuCauQuanLy();
  }
}
