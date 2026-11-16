package com.dtaq.retail.dao;

import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Truy vấn và ghi sản phẩm theo số serial. */
public interface SanPhamDao {

  /**
   * Ghi một máy mới vào bảng {@code "SanPham"}.
   *
   * <p>Bên gọi sở hữu kết nối và giao dịch: kết nối được truyền vào không bị đóng ở đây và chế độ
   * tự động xác nhận (auto-commit) của nó không bị thay đổi. Điều này cho phép insert máy, cập nhật
   * số lượng tồn và các bước liên quan nằm cùng một giao dịch bên gọi.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param sanPham máy cần ghi; số serial phải có sẵn và không được để trống
   * @throws IllegalArgumentException nếu sản phẩm {@code null} hoặc số serial rỗng
   */
  void insert(Connection ketNoi, SanPham sanPham);

  /**
   * Cập nhật thông tin một máy đang có trong kho.
   *
   * <p>Serial là khóa định danh, nên không thay đổi trong phương thức này. Bên gọi sở hữu kết nối và
   * giao dịch: kết nối được truyền vào không bị đóng ở đây và chế độ tự động xác nhận (auto-commit)
   * của nó không bị thay đổi.
   */
  default void update(Connection ketNoi, SanPham sanPham) {
    throw new UnsupportedOperationException("Cập nhật sản phẩm chưa được hỗ trợ bởi DAO hiện tại");
  }

  /**
   * Tìm sản phẩm theo số serial.
   *
   * @param soSerial số serial cần tra cứu
   * @return sản phẩm tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<SanPham> findById(String soSerial);

  /**
   * Liệt kê các sản phẩm thuộc một dòng sản phẩm.
   *
   * @param maDongSP mã dòng sản phẩm
   * @return danh sách sản phẩm thuộc dòng, có thể rỗng
   */
  List<SanPham> findByDongSanPham(String maDongSP);

  /**
   * Liệt kê các sản phẩm theo trạng thái hiện tại.
   *
   * @param trangThai trạng thái cần lọc
   * @return danh sách sản phẩm có trạng thái đó, có thể rỗng
   */
  List<SanPham> findByTrangThai(TrangThaiSanPham trangThai);

  /**
   * Liệt kê toàn bộ sản phẩm trong hệ thống.
   */
  List<SanPham> findAll();

  /**
   * Tìm máy theo số serial, tương đương với {@link #findById(String)}.
   */
  default Optional<SanPham> findBySerial(String soSerial) {
    return findById(soSerial);
  }

  /**
   * Tìm máy theo mã dòng sản phẩm, tương đương với {@link #findByDongSanPham(String)}.
   */
  default List<SanPham> findByDong(String maDongSP) {
    return findByDongSanPham(maDongSP);
  }

  /**
   * Tìm máy theo một phần số serial, mã dòng sản phẩm hoặc mã nhà cung cấp, sắp theo serial.
   *
   * @param tuKhoa từ khóa, không phân biệt hoa thường
   * @param gioiHan số máy tối đa trả về
   * @return các máy khớp; rỗng nếu không có
   */
  default List<SanPham> timKiem(String tuKhoa, int gioiHan) {
    String khoa = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase();
    return findAll().stream()
        .filter(sp -> sp.soSerial().toLowerCase().contains(khoa)
            || sp.dongSanPham().maDongSP().toLowerCase().contains(khoa)
            || sp.nhaCungCap().maNCC().toLowerCase().contains(khoa))
        .limit(gioiHan)
        .toList();
  }

  /**
   * Một trang máy khớp từ khóa (số serial, mã/tên dòng sản phẩm, mã/tên nhà cung cấp), sắp theo serial.
   *
   * @param tuKhoa từ khóa; rỗng thì lấy tất cả
   * @param boQua số dòng bỏ qua
   * @param gioiHan số dòng của trang
   */
  default Trang<SanPham> timTrang(String tuKhoa, int boQua, int gioiHan) {
    String khoa = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase();
    return Trang.cat(findAll().stream()
        .filter(sp -> sp.soSerial().toLowerCase().contains(khoa)
            || sp.dongSanPham().maDongSP().toLowerCase().contains(khoa)
            || sp.nhaCungCap().maNCC().toLowerCase().contains(khoa))
        .toList(), boQua, gioiHan);
  }

  /**
   * Đọc máy của nhiều dòng sản phẩm trong một lần, gom theo mã dòng.
   *
   * @param maDongSP các mã dòng cần đọc
   * @return máy theo từng mã dòng; dòng không có máy thì không có khóa tương ứng
   */
  default java.util.Map<String, List<SanPham>> findByDongIn(java.util.Collection<String> maDongSP) {
    java.util.Map<String, List<SanPham>> ketQua = new java.util.LinkedHashMap<>();
    for (String ma : maDongSP) {
      ketQua.put(ma, findByDongSanPham(ma));
    }
    return ketQua;
  }

  /**
   * Tìm máy theo danh mục sản phẩm.
   */
  default List<SanPham> findByDanhMuc(String maDanhMuc) {
    return findAll().stream()
        .filter(sp -> sp.dongSanPham().danhMucSanPham() != null
            && maDanhMuc.equals(sp.dongSanPham().danhMucSanPham().maDanhMuc()))
        .toList();
  }

  /**
   * Lấy danh sách máy đang sẵn sàng để bán theo dòng sản phẩm.
   */
  default List<SanPham> findBanDuoc(String maDongSP) {
    return findByDongSanPham(maDongSP).stream()
        .filter(sp -> sp.trangThai() == TrangThaiSanPham.SAN_SANG_BAN)
        .toList();
  }

  /**
   * Kiểm tra xem serial đã tồn tại trong kho hay chưa.
   */
  default boolean existsSerial(String soSerial) {
    return findById(soSerial).isPresent();
  }

  /**
   * Cập nhật trạng thái của một máy cụ thể.
   *
   * @param soSerial số serial máy cần cập nhật
   * @param trangThai trạng thái mới
   */
  void updateTrangThai(String soSerial, TrangThaiSanPham trangThai);

  /**
   * Đánh dấu một máy đã bán bằng câu lệnh có điều kiện, dùng trong giao dịch thanh toán.
   *
   * <p>Chỉ chuyển sang {@code DA_BAN} khi máy đang ở trạng thái {@code SAN_SANG_BAN}; nhờ vậy hai giao
   * dịch không thể cùng bán một máy (RB40). Bên gọi sở hữu kết nối và giao dịch: kết nối được truyền
   * vào không bị đóng ở đây và chế độ tự động xác nhận (auto-commit) của nó không bị thay đổi.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param soSerial số serial của máy cần đánh dấu
   * @return {@code true} nếu máy được chuyển sang {@code DA_BAN}, {@code false} nếu máy không còn
   *     sẵn sàng bán hoặc không tồn tại
   */
  default boolean danhDauDaBan(java.sql.Connection ketNoi, String soSerial) {
    throw new UnsupportedOperationException("Chưa hỗ trợ đánh dấu bán theo giao dịch");
  }

  /**
   * Chọn và khóa máy sẵn sàng bán của một dòng theo FIFO (nhập kho lâu nhất trước, rồi theo serial).
   *
   * <p>Chạy trong giao dịch của bên gọi; máy đang bị giao dịch khác khóa được bỏ qua nên hai quầy
   * bán cùng dòng không chọn trùng máy (RB40).
   *
   * @param ketNoi kết nối đang mở giao dịch của bên gọi
   * @param maDongSP mã dòng sản phẩm đang kinh doanh
   * @param soLuong số máy cần chọn
   * @return tối đa {@code soLuong} máy; ít hơn nếu không đủ máy
   */
  default List<SanPham> chonMayDeBan(Connection ketNoi, String maDongSP, int soLuong) {
    throw new UnsupportedOperationException("Chưa hỗ trợ chọn máy trong giao dịch");
  }
}
