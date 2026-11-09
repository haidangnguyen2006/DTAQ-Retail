package com.dtaq.retail.dao;

import com.dtaq.retail.entity.KhuyenMai;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Giao diện truy cập dữ liệu chương trình khuyến mãi trên bảng {@code "KhuyenMai"}.
 *
 * và cung cấp phương thức tra cứu khuyến mãi hiệu lực {@link #findHieuLuc(String, LocalDate)}
 * cho UC003 (bán hàng - Đăng).
 */
public interface KhuyenMaiDao {

  /**
   * Tìm chương trình khuyến mãi đang áp dụng và còn hiệu lực cho một dòng sản phẩm tại một ngày xác định.
   *
   * <p>Điều kiện thỏa mãn: {@code dangApDung = true} và {@code ngay BETWEEN ngayBatDau AND ngayKetThuc}.
   * Nếu có nhiều chương trình thỏa mãn, ưu tiên chương trình có tỷ lệ giảm cao nhất.
   *
   * @param maDongSP mã dòng sản phẩm cần tra cứu; không được để trống
   * @param ngay ngày kiểm tra hiệu lực (thường là ngày lập hóa đơn); không được {@code null}
   * @return chương trình khuyến mãi thỏa mãn nếu có, hoặc rỗng
   */
  Optional<KhuyenMai> findHieuLuc(String maDongSP, LocalDate ngay);

  /**
   * Tìm chương trình khuyến mãi theo mã định danh {@code maKM}.
   *
   * @param maKM mã khuyến mãi (dạng {@code KM-YYYY-NNNNNN})
   * @return thông tin chương trình khuyến mãi kèm dòng sản phẩm, hoặc rỗng nếu không tìm thấy
   */
  Optional<KhuyenMai> findById(String maKM);

  /**
   * Lấy toàn bộ danh sách chương trình khuyến mãi, sắp xếp theo ngày bắt đầu giảm dần.
   *
   * @return danh sách chương trình khuyến mãi, không bao giờ {@code null}, có thể rỗng
   */
  List<KhuyenMai> findAll();

  /**
   * Lấy danh sách chương trình khuyến mãi áp dụng cho một dòng sản phẩm cụ thể.
   *
   * @param maDongSP mã dòng sản phẩm cần lọc
   * @return danh sách khuyến mãi của dòng sản phẩm, sắp xếp theo ngày bắt đầu giảm dần
   */
  List<KhuyenMai> findByDongSanPham(String maDongSP);

  /**
   * Thêm mới một chương trình khuyến mãi vào bảng {@code "KhuyenMai"}.
   *
   * @param khuyenMai thông tin khuyến mãi cần thêm
   * @throws IllegalArgumentException nếu thông tin không hợp lệ
   */
  void insert(KhuyenMai khuyenMai);

  /**
   * Cập nhật thông tin của một chương trình khuyến mãi.
   *
   * @param khuyenMai thông tin khuyến mãi cần cập nhật
   * @throws IllegalArgumentException nếu thông tin không hợp lệ
   */
  void update(KhuyenMai khuyenMai);

  /**
   * Bật hoặc tắt trạng thái áp dụng của chương trình khuyến mãi (cập nhật cờ {@code dangApDung}).
   *
   * @param maKM mã khuyến mãi cần cập nhật; không được để trống
   * @param dangApDung {@code true} nếu áp dụng, {@code false} nếu dừng áp dụng
   */
  void updateTrangThaiApDung(String maKM, boolean dangApDung);

  /**
   * Kiểm tra xem mã khuyến mãi đã tồn tại trong hệ thống chưa.
   *
   * @param maKM mã khuyến mãi cần kiểm tra
   * @return {@code true} nếu mã đã tồn tại, ngược lại {@code false}
   */
  boolean existsTrungMa(String maKM);

  /**
   * Kiểm tra xem có chương trình khuyến mãi nào đang áp dụng ({@code dangApDung = true})
   * trên cùng dòng sản phẩm bị chồng lấn khoảng thời gian hay không (Quy tắc 10.b & Quyết định 2026-10-25).
   *
   * <p>Hai khoảng thời gian giao nhau khi: {@code NOT (ngayKetThucCu < ngayBatDauMoi OR ngayBatDauCu > ngayKetThucMoi)}.
   *
   * @param maDongSP mã dòng sản phẩm cần kiểm tra
   * @param ngayBatDau ngày bắt đầu mới
   * @param ngayKetThuc ngày kết thúc mới
   * @param excludeMaKM mã khuyến mãi cần loại trừ (khi cập nhật chính khuyến mãi đó), có thể {@code null}
   * @return {@code true} nếu bị chồng lấn thời gian với chương trình đang áp dụng khác, ngược lại {@code false}
   */
  boolean existsChongLan(String maDongSP, LocalDate ngayBatDau, LocalDate ngayKetThuc, String excludeMaKM);
}
