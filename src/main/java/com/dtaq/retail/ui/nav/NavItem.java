package com.dtaq.retail.ui.nav;

import com.dtaq.retail.entity.LoaiQuyen;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Danh mục mục điều hướng trên thanh bên ("sidebar") của màn hình chính, theo vai trò nhân viên.
 *
 * <p>Thứ tự hằng là thứ tự hiển thị trên sidebar với mỗi vai trò (xem
 * {@code docs/reference/assets/screens/screen-flow.jpg}); {@link #name()} đồng thời được dùng
 * làm key của {@code CardLayout} trong khung nội dung, do đó không được đổi tên hằng.
 *
 * <p>Mỗi mục có tên hiển thị tiếng Việt ({@link #label()}), danh sách vai trò được phép xem
 * ({@code allowedRoles}, so sánh trực tiếp với {@link LoaiQuyen}) và tham chiếu tài liệu
 * {@link #sourceRef()} (mã Use Case / FR trong báo cáo phân tích – thiết kế). Các ghi chú xung đột
 * tài liệu (C-01, C-03, C-05) chỉ được ghi trong {@code sourceRef}, không ra quyết định nghiệp vụ
 * tại đây.
 */
public enum NavItem {
  TRANG_CHU(
      "Trang chủ",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY, LoaiQuyen.NHAN_VIEN_BAN_HANG),
      "UC001/UC002 – Đăng nhập/Đăng xuất"),
  QUAN_LY_NHAN_VIEN(
      "Quản lý nhân viên",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY),
      "UC013 – Quản lý nhân viên bán hàng"),
  THONG_KE_DOANH_THU(
      "Thống kê doanh thu",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY),
      "UC017 – Thống kê doanh thu"),
  THONG_KE_DOANH_SO_NV(
      "Thống kê doanh số theo nhân viên",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY),
      "UC016 – Thống kê doanh số theo nhân viên"),
  BAO_CAO_TON_KHO(
      "Báo cáo tồn kho",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY),
      "UC014 – Báo cáo sản phẩm tồn kho lâu ngày (C-01)"),
  SAO_LUU_DU_LIEU(
      "Sao lưu dữ liệu",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY),
      "FR015 – Sao lưu dữ liệu (C-05: chưa có UC)"),
  PHUC_HOI_DU_LIEU(
      "Phục hồi dữ liệu",
      Set.of(LoaiQuyen.NHAN_VIEN_QUAN_LY),
      "FR016 – Phục hồi dữ liệu (C-05: chưa có UC)"),
  QUAN_LY_KHACH_HANG(
      "Quản lý khách hàng",
      Set.of(LoaiQuyen.NHAN_VIEN_BAN_HANG),
      "UC007 – Quản lý khách hàng"),
  QUAN_LY_LOAI_SAN_PHAM(
      "Quản lý loại sản phẩm",
      Set.of(LoaiQuyen.NHAN_VIEN_BAN_HANG),
      "UC010 – Thêm danh mục sản phẩm (C-03)"),
  QUAN_LY_SAN_PHAM(
      "Quản lý sản phẩm",
      Set.of(LoaiQuyen.NHAN_VIEN_BAN_HANG),
      "UC008 – Thêm sản phẩm (C-03)"),
  LAP_HOA_DON_BAN_LE(
      "Lập hóa đơn bán lẻ",
      Set.of(LoaiQuyen.NHAN_VIEN_BAN_HANG),
      "UC003 – Lập hóa đơn bán lẻ"),
  XU_LY_DOI_TRA_HANG(
      "Xử lý đổi/trả hàng",
      Set.of(LoaiQuyen.NHAN_VIEN_BAN_HANG),
      "UC006 – Xử lý đổi trả sản phẩm");

  /** Tên hiển thị tiếng Việt trên thanh bên. */
  private final String label;

  /** Vai trò được phép thấy mục này (bộ bất biến). */
  private final Set<LoaiQuyen> allowedRoles;

  /** Tham chiếu tài liệu gốc (Use Case / FR) trong báo cáo phân tích – thiết kế. */
  private final String sourceRef;

  NavItem(String label, Set<LoaiQuyen> allowedRoles, String sourceRef) {
    this.label = Objects.requireNonNull(label, "label");
    this.allowedRoles = Objects.requireNonNull(allowedRoles, "allowedRoles");
    this.sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
  }

  /** Tên hiển thị tiếng Việt; các nhãn phải duy nhất giữa các mục. */
  public String label() {
    return label;
  }

  /** Tham chiếu tài liệu gốc (Use Case / FR) dùng cho nhãn phụ và PlaceholderPanel. */
  public String sourceRef() {
    return sourceRef;
  }

  /** Kiểm tra mục này có được phép hiển thị cho vai trò {@code loaiQuyen} hay không. */
  public boolean isAllowed(LoaiQuyen loaiQuyen) {
    return allowedRoles.contains(Objects.requireNonNull(loaiQuyen, "loaiQuyen"));
  }

  /**
   * Danh sách mục được phép hiển thị cho vai trò {@code loaiQuyen}, đúng thứ tự hằng (luôn bắt đầu
   * bằng {@link #TRANG_CHU}).
   */
  public static List<NavItem> withRole(LoaiQuyen loaiQuyen) {
    Objects.requireNonNull(loaiQuyen, "loaiQuyen");
    return Arrays.stream(values()).filter(item -> item.isAllowed(loaiQuyen)).toList();
  }
}
