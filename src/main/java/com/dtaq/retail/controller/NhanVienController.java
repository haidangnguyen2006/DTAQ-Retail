package com.dtaq.retail.controller;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import java.util.List;
import java.util.Objects;

/** Xử lý nghiệp vụ quản lý nhân viên (UC013); chỉ Nhân viên quản lý được sử dụng. */
public final class NhanVienController {

  private static final String LOI_QUYEN = "Chức năng này chỉ dành cho nhân viên quản lý";

  private final NhanVienDao nhanVienDao;

  /**
   * Khởi tạo bộ điều khiển nhân viên.
   *
   * @param nhanVienDao lớp truy cập nhân viên; không được {@code null}
   */
  public NhanVienController(NhanVienDao nhanVienDao) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
  }

  /**
   * Liệt kê toàn bộ nhân viên, gồm cả tài khoản đã khóa.
   *
   * @return danh sách nhân viên, có thể rỗng
   * @throws SecurityException nếu phiên hiện tại không phải nhân viên quản lý
   * @throws DataAccessException nếu không đọc được dữ liệu
   */
  public List<NhanVien> danhSach() {
    yeuCauQuanLy();
    return nhanVienDao.findAll();
  }

  private static void yeuCauQuanLy() {
    boolean duocPhep = CurrentSession.get()
        .filter(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .isPresent();
    if (!duocPhep) {
      throw new SecurityException(LOI_QUYEN);
    }
  }
}
