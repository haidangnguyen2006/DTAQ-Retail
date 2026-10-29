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
}
