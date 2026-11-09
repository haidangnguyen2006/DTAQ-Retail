package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.MaGenerator;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý nghiệp vụ quản lý danh mục sản phẩm, tách biệt logic với giao diện và truy cập dữ liệu.
 */
public final class DanhMucController {

  private static final Logger log = LoggerFactory.getLogger(DanhMucController.class);

  private final DanhMucSanPhamDao danhMucDao;
  private final DataSource dataSource;

  /**
   * Khởi tạo bộ điều khiển danh mục sản phẩm.
   *
   * @param danhMucDao lớp truy cập dữ liệu danh mục sản phẩm; không được {@code null}
   * @param dataSource nguồn kết nối cơ sở dữ liệu để phục vụ cấp mã định danh; không được {@code null}
   */
  public DanhMucController(DanhMucSanPhamDao danhMucDao, DataSource dataSource) {
    this.danhMucDao = Objects.requireNonNull(danhMucDao, "danhMucDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Lấy toàn bộ danh sách danh mục sản phẩm.
   *
   * @return danh sách danh mục, không bao giờ {@code null}
   */
  public List<DanhMucSanPham> layDanhSach() {
    try {
      return danhMucDao.findAll();
    } catch (DataAccessException e) {
      log.error("Lỗi khi tải danh sách danh mục sản phẩm", e);
      return Collections.emptyList();
    }
  }

  /**
   * Tìm danh mục sản phẩm theo mã định danh.
   *
   * @param maDanhMuc mã danh mục cần tìm
   * @return đối tượng danh mục nếu tồn tại, ngược lại rỗng
   */
  public Optional<DanhMucSanPham> timTheoMa(String maDanhMuc) {
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return Optional.empty();
    }
    try {
      return danhMucDao.findById(maDanhMuc.trim());
    } catch (DataAccessException e) {
      log.error("Lỗi khi tìm danh mục theo mã: {}", maDanhMuc, e);
      return Optional.empty();
    }
  }

  /**
   * Thêm mới một danh mục sản phẩm.
   *
   * @param tenDanhMuc tên danh mục; không được rỗng
   * @param moTa mô tả danh mục; có thể rỗng
   * @return kết quả thao tác kèm danh mục vừa thêm nếu thành công
   */
  public KetQua<DanhMucSanPham> themMoi(String tenDanhMuc, String moTa) {
    if (tenDanhMuc == null || tenDanhMuc.isBlank()) {
      return KetQua.loi("Tên danh mục không được để trống");
    }

    String tenChuanHoa = tenDanhMuc.trim();
    String moTaChuanHoa = moTa != null && !moTa.isBlank() ? moTa.trim() : null;

    try {
      if (danhMucDao.existsByTen(tenChuanHoa, null)) {
        return KetQua.loi("Tên danh mục đã tồn tại trong hệ thống");
      }
    } catch (DataAccessException e) {
      log.warn("Lỗi khi kiểm tra trùng tên danh mục: {}", e.getMessage());
    }

    String maDanhMuc;
    try (Connection conn = dataSource.getConnection()) {
      maDanhMuc = MaGenerator.next(conn, "DanhMucSanPham", "maDanhMuc", "DM");
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi sinh mã danh mục mới", e);
      return KetQua.loi("Không thể khởi tạo mã danh mục mới: " + e.getMessage());
    }

    DanhMucSanPham danhMuc = new DanhMucSanPham(maDanhMuc, tenChuanHoa, moTaChuanHoa);
    try {
      danhMucDao.insert(danhMuc);
      return KetQua.thanhCong(danhMuc, "Thêm danh mục sản phẩm thành công");
    } catch (DataAccessException e) {
      log.warn("Lỗi khi lưu danh mục mới: {}", e.getMessage());
      if (laLoiTrungLap(e)) {
        return KetQua.loi("Mã hoặc tên danh mục đã tồn tại trong hệ thống");
      }
      return KetQua.loi(e.getMessage());
    }
  }

  /**
   * Cập nhật thông tin một danh mục sản phẩm.
   *
   * @param maDanhMuc mã danh mục cần cập nhật; không được rỗng
   * @param tenDanhMuc tên danh mục mới; không được rỗng
   * @param moTa mô tả mới; có thể rỗng
   * @return kết quả thao tác kèm danh mục sau cập nhật nếu thành công
   */
  public KetQua<DanhMucSanPham> capNhat(String maDanhMuc, String tenDanhMuc, String moTa) {
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return KetQua.loi("Mã danh mục không được để trống");
    }
    if (tenDanhMuc == null || tenDanhMuc.isBlank()) {
      return KetQua.loi("Tên danh mục không được để trống");
    }

    String maChuanHoa = maDanhMuc.trim();
    String tenChuanHoa = tenDanhMuc.trim();
    String moTaChuanHoa = moTa != null && !moTa.isBlank() ? moTa.trim() : null;

    try {
      if (danhMucDao.existsByTen(tenChuanHoa, maChuanHoa)) {
        return KetQua.loi("Tên danh mục đã trùng với một danh mục khác trong hệ thống");
      }
    } catch (DataAccessException e) {
      log.warn("Lỗi khi kiểm tra trùng tên danh mục: {}", e.getMessage());
    }

    DanhMucSanPham danhMuc = new DanhMucSanPham(maChuanHoa, tenChuanHoa, moTaChuanHoa);
    try {
      danhMucDao.update(danhMuc);
      return KetQua.thanhCong(danhMuc, "Cập nhật danh mục sản phẩm thành công");
    } catch (DataAccessException e) {
      log.warn("Lỗi khi cập nhật danh mục: {}", e.getMessage());
      if (laLoiTrungLap(e)) {
        return KetQua.loi("Tên danh mục đã trùng với một danh mục khác trong hệ thống");
      }
      return KetQua.loi(e.getMessage());
    }
  }

  /**
   * Xóa một danh mục sản phẩm khỏi hệ thống.
   *
   * @param maDanhMuc mã danh mục cần xóa; không được rỗng
   * @return kết quả thao tác xóa
   */
  public KetQua<Void> xoa(String maDanhMuc) {
    if (maDanhMuc == null || maDanhMuc.isBlank()) {
      return KetQua.loi("Mã danh mục không được để trống");
    }

    String maChuanHoa = maDanhMuc.trim();
    try {
      boolean coDongThamChieu = danhMucDao.existsDongByDanhMuc(maChuanHoa);
      if (coDongThamChieu) {
        return KetQua.loi("Danh mục đang có dòng sản phẩm tham chiếu, không thể xóa");
      }
      danhMucDao.delete(maChuanHoa);
      return KetQua.thanhCong(null, "Xóa danh mục sản phẩm thành công");
    } catch (DataAccessException e) {
      log.error("Lỗi khi xóa danh mục: {}", maChuanHoa, e);
      return KetQua.loi(e.getMessage());
    }
  }

  private boolean laLoiTrungLap(DataAccessException e) {
    Throwable cause = e.getCause();
    while (cause != null) {
      if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
        return true;
      }
      cause = cause.getCause();
    }
    String msg = e.getMessage();
    return msg != null && (msg.contains("23505") || msg.toLowerCase().contains("tồn tại") || msg.toLowerCase().contains("trùng"));
  }

  /**
   * Đóng gói kết quả trả về của các thao tác nghiệp vụ.
   *
   * @param <T> kiểu dữ liệu trả về kèm theo khi thao tác thành công
   * @param thanhCong trạng thái thành công hay thất bại
   * @param thongBao thông điệp kết quả
   * @param duLieu dữ liệu đối tượng trả về (có thể {@code null})
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
