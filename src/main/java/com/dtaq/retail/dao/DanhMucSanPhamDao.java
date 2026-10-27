package com.dtaq.retail.dao;

import com.dtaq.retail.entity.DanhMucSanPham;
import java.util.List;
import java.util.Optional;

public interface DanhMucSanPhamDao {

  /**
   * Lấy toàn bộ danh sách danh mục sản phẩm, sắp xếp theo tên danh mục tăng dần.
   *
   * @return danh sách danh mục sản phẩm, không bao giờ {@code null}, có thể rỗng
   */
  List<DanhMucSanPham> findAll();

  /**
   * Tìm danh mục sản phẩm theo mã định danh.
   *
   * @param maDanhMuc mã danh mục cần tra cứu
   * @return danh mục sản phẩm tương ứng, hoặc rỗng nếu không tìm thấy
   */
  Optional<DanhMucSanPham> findById(String maDanhMuc);

  /**
   * Tìm danh mục sản phẩm theo tên đã chuẩn hóa (không phân biệt chữ hoa, chữ thường và khoảng trắng đầu cuối).
   *
   * @param tenDanhMuc tên danh mục cần tra cứu
   * @return danh mục sản phẩm tương ứng, hoặc rỗng nếu không tìm thấy
   */
  Optional<DanhMucSanPham> findByTen(String tenDanhMuc);

  /**
   * Thêm mới một danh mục sản phẩm vào bảng {@code "DanhMucSanPham"}.
   *
   * @param danhMuc danh mục sản phẩm cần thêm; mã và tên không được để trống
   * @throws IllegalArgumentException nếu {@code danhMuc} là {@code null} hoặc mã/tên để trống
   */
  void insert(DanhMucSanPham danhMuc);

  /**
   * Cập nhật thông tin tên và mô tả của danh mục sản phẩm.
   *
   * @param danhMuc danh mục sản phẩm chứa thông tin cập nhật
   * @throws IllegalArgumentException nếu {@code danhMuc} là {@code null} hoặc mã/tên để trống
   */
  void update(DanhMucSanPham danhMuc);

  /**
   * Xóa danh mục sản phẩm theo mã.
   *
   * @param maDanhMuc mã danh mục cần xóa
   * @throws IllegalArgumentException nếu {@code maDanhMuc} để trống
   */
  void delete(String maDanhMuc);

  /**
   * Kiểm tra xem danh mục có đang được dòng sản phẩm nào tham chiếu tới không.
   *
   * @param maDanhMuc mã danh mục cần kiểm tra
   * @return {@code true} nếu có ít nhất một dòng sản phẩm thuộc danh mục này, ngược lại {@code false}
   */
  boolean existsDongByDanhMuc(String maDanhMuc);

  /**
   * Kiểm tra xem tên danh mục đã tồn tại trong hệ thống chưa (theo chuẩn hóa {@code lower(btrim(ten))}).
   *
   * @param tenDanhMuc tên danh mục cần kiểm tra
   * @param excludeMaDanhMuc mã danh mục cần loại trừ (dùng khi cập nhật chính danh mục đó), có thể {@code null}
   * @return {@code true} nếu tên đã tồn tại, ngược lại {@code false}
   */
  boolean existsByTen(String tenDanhMuc, String excludeMaDanhMuc);
}
