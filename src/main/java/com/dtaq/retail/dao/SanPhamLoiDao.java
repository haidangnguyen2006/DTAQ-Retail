package com.dtaq.retail.dao;

import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Giao diện truy cập dữ liệu sản phẩm lỗi trên bảng {@code "SanPhamLoi"}.
 *
 * các trạng thái xử lý lỗi (chờ xử lý, gửi bảo hành, trả nhà cung cấp, thanh lý).
 */
public interface SanPhamLoiDao {

  /**
   * Thêm một bản ghi sản phẩm lỗi trong giao dịch do bên gọi quản lý.
   *
   * <p>Bên gọi sở hữu {@code ketNoi} và kiểm soát giao dịch (commit/rollback). Phương thức này
   * không tự đóng kết nối hay thay đổi chế độ auto-commit.
   *
   * @param ketNoi kết nối CSDL đang mở do bên gọi quản lý; không được {@code null}
   * @param sanPhamLoi bản ghi lỗi cần thêm; không được {@code null}
   * @throws IllegalArgumentException nếu tham số không hợp lệ
   */
  void insert(Connection ketNoi, SanPhamLoi sanPhamLoi);

  /**
   * Thêm một bản ghi sản phẩm lỗi sử dụng kết nối độc lập.
   *
   * @param sanPhamLoi bản ghi lỗi cần thêm; không được {@code null}
   * @throws IllegalArgumentException nếu tham số không hợp lệ
   */
  void insert(SanPhamLoi sanPhamLoi);

  /**
   * Tìm hồ sơ sản phẩm lỗi theo mã lỗi {@code maLoi}.
   *
   * @param maLoi mã hồ sơ lỗi (dạng {@code SPL-YYYY-NNNNNN})
   * @return bản ghi sản phẩm lỗi kèm máy liên quan, hoặc rỗng nếu không tìm thấy
   */
  Optional<SanPhamLoi> findById(String maLoi);

  /**
   * Tìm hồ sơ lỗi đang ở trạng thái {@link TrangThaiXuLyLoi#CHO_XU_LY} của một máy theo số serial.
   *
   * <p>Dùng để ngăn chặn việc tạo trùng lặp hồ sơ lỗi đang chờ xử lý cho cùng một máy (Rule 9.a / UC011).
   *
   * @param soSerial số serial máy cần kiểm tra
   * @return hồ sơ lỗi đang chờ xử lý nếu có, hoặc rỗng
   */
  Optional<SanPhamLoi> findDangXuLy(String soSerial);

  /**
   * Lấy toàn bộ danh sách sản phẩm lỗi trong hệ thống, sắp xếp theo ngày ghi nhận giảm dần.
   *
   * @return danh sách sản phẩm lỗi, không bao giờ {@code null}, có thể rỗng
   */
  List<SanPhamLoi> findAll();

  /**
   * Lấy danh sách sản phẩm lỗi theo trạng thái xử lý.
   *
   * @param tinhTrangXuLy trạng thái cần lọc; không được {@code null}
   * @return danh sách sản phẩm lỗi thỏa điều kiện, sắp xếp theo ngày ghi nhận giảm dần
   */
  List<SanPhamLoi> findByTinhTrang(TrangThaiXuLyLoi tinhTrangXuLy);

  /**
   * Cập nhật tình trạng xử lý và ghi chú xử lý của hồ sơ lỗi trong giao dịch do bên gọi quản lý.
   *
   * @param ketNoi kết nối CSDL đang mở do bên gọi quản lý; không được {@code null}
   * @param maLoi mã hồ sơ lỗi cần cập nhật; không được để trống
   * @param tinhTrangMoi trạng thái xử lý mới; không được {@code null}
   * @param ghiChuXuLy nội dung ghi chú xử lý (có thể {@code null})
   * @param ngayCapNhat ngày cập nhật (thường là ngày hiện tại); không được {@code null}
   */
  void updateTinhTrang(
      Connection ketNoi,
      String maLoi,
      TrangThaiXuLyLoi tinhTrangMoi,
      String ghiChuXuLy,
      LocalDate ngayCapNhat);

  /**
   * Cập nhật tình trạng xử lý và ghi chú xử lý của hồ sơ lỗi với kết nối độc lập.
   *
   * @param maLoi mã hồ sơ lỗi cần cập nhật; không được để trống
   * @param tinhTrangMoi trạng thái xử lý mới; không được {@code null}
   * @param ghiChuXuLy nội dung ghi chú xử lý (có thể {@code null})
   * @param ngayCapNhat ngày cập nhật; không được {@code null}
   */
  void updateTinhTrang(
      String maLoi,
      TrangThaiXuLyLoi tinhTrangMoi,
      String ghiChuXuLy,
      LocalDate ngayCapNhat);
}
