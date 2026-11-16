package com.dtaq.retail.dao;

import com.dtaq.retail.entity.DongSanPham;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Truy vấn và cập nhật dòng sản phẩm. */
public interface DongSanPhamDao {

  /**
   * Ghi một dòng sản phẩm mới vào bảng {@code "DongSanPham"}.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param dongSanPham dòng sản phẩm cần ghi; mã dòng sản phẩm phải có sẵn
   * @throws IllegalArgumentException nếu dòng sản phẩm {@code null} hoặc mã rỗng
   */
  void insert(Connection ketNoi, DongSanPham dongSanPham);

  /**
   * Tìm dòng sản phẩm theo mã.
   *
   * @param maDongSP mã dòng sản phẩm
   * @return dòng sản phẩm tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<DongSanPham> findById(String maDongSP);

  /**
   * Liệt kê tất cả dòng sản phẩm.
   *
   * @return danh sách dòng sản phẩm, có thể rỗng
   */
  List<DongSanPham> findAll();

  /**
   * Liệt kê dòng sản phẩm trong một danh mục.
   *
   * @param maDanhMuc mã danh mục sản phẩm
   * @return danh sách dòng sản phẩm thuộc danh mục, có thể rỗng
   */
  List<DongSanPham> findByDanhMuc(String maDanhMuc);

  /**
   * Cập nhật toàn bộ thông tin của một dòng sản phẩm đã có.
   *
   * @param dongSanPham dòng sản phẩm mới sau khi cập nhật; không được {@code null}
   */
  default void update(DongSanPham dongSanPham) {
    throw new UnsupportedOperationException("Cập nhật dòng sản phẩm chưa được triển khai");
  }

  /**
   * Cập nhật số lượng tồn hiện tại của dòng sản phẩm.
   *
   * @param maDongSP mã dòng sản phẩm
   * @param soLuongTon số lượng tồn mới
   */
  void updateSoLuongTon(String maDongSP, int soLuongTon);

  /**
   * Cập nhật trạng thái kinh doanh của dòng sản phẩm.
   *
   * @param maDongSP mã dòng sản phẩm
   * @param trangThaiKinhDoanh trạng thái kinh doanh mới
   */
  void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh);

  /**
   * Cập nhật giá bán trước thuế và thuế suất của một dòng sản phẩm.
   *
   * @param maDongSP mã dòng sản phẩm cần cập nhật
   * @param giaBanTruocThue giá trị mới
   * @param thue thuế mới
   */
  default void updateGiaThue(String maDongSP, java.math.BigDecimal giaBanTruocThue, java.math.BigDecimal thue) {
    throw new UnsupportedOperationException("Cập nhật giá và thuế chưa được triển khai");
  }

  /**
   * Tìm kiếm dòng sản phẩm theo từ khóa.
   */
  default List<DongSanPham> search(String tuKhoa) {
    String keyword = tuKhoa == null ? "" : tuKhoa.trim();
    if (keyword.isEmpty()) {
      return findAll();
    }
    String lower = keyword.toLowerCase();
    return findAll().stream()
        .filter(dsp -> dsp.maDongSP().toLowerCase().contains(lower)
            || dsp.tenDongSP().toLowerCase().contains(lower)
            || dsp.danhMucSanPham().tenDanhMuc().toLowerCase().contains(lower))
        .toList();
  }

  /**
   * Lấy toàn bộ dòng sản phẩm kèm danh mục liên quan.
   */
  /**
   * Tìm dòng đang kinh doanh và còn máy bán được theo một phần mã hoặc tên, sắp theo mã dòng.
   *
   * <p>Dùng cho màn bán hàng: tồn kho lấy từ {@code soLuongTon} (trigger V6 giữ đúng), không đọc
   * bảng máy.
   *
   * @param tuKhoa một phần mã hoặc tên dòng, không phân biệt hoa thường
   * @param gioiHan số dòng tối đa trả về
   * @return các dòng khớp; rỗng nếu không có
   */
  default List<DongSanPham> timDangBan(String tuKhoa, int gioiHan) {
    String khoa = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase();
    return findAll().stream()
        .filter(dsp -> dsp.trangThaiKinhDoanh() && dsp.soLuongTon() > 0)
        .filter(dsp -> dsp.maDongSP().toLowerCase().contains(khoa)
            || dsp.tenDongSP().toLowerCase().contains(khoa))
        .sorted(java.util.Comparator.comparing(DongSanPham::maDongSP))
        .limit(gioiHan)
        .toList();
  }

  /**
   * Đọc nhiều dòng sản phẩm theo mã trong một lần.
   *
   * @param maDongSP các mã cần đọc
   * @return các dòng tìm thấy; mã không tồn tại bị bỏ qua
   */
  default List<DongSanPham> findByMaIn(java.util.Collection<String> maDongSP) {
    return maDongSP.stream().map(this::findById).flatMap(Optional::stream).toList();
  }

  /**
   * Một trang dòng sản phẩm khớp từ khóa (mã, tên dòng hoặc tên danh mục), sắp theo mã dòng.
   *
   * @param tuKhoa từ khóa; rỗng thì lấy tất cả
   * @param boQua số dòng bỏ qua
   * @param gioiHan số dòng của trang
   */
  default Trang<DongSanPham> timTrang(String tuKhoa, int boQua, int gioiHan) {
    return Trang.cat(search(tuKhoa), boQua, gioiHan);
  }

  default List<DongSanPham> findAllWithDanhMuc() {
    return findAll();
  }

  /**
   * Kiểm tra sự tồn tại của mã dòng sản phẩm.
   */
  default boolean existsMaDong(String maDongSP) {
    return findById(maDongSP).isPresent();
  }
}
