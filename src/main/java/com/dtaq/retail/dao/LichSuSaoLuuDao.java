package com.dtaq.retail.dao;

import com.dtaq.retail.entity.BackupRecord;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn và ghi lịch sử sao lưu/phục hồi trên bảng {@code "LichSuSaoLuu"}.
 *
 * <p>Đây là cache hiển thị: nguồn sự thật là cặp tệp {@code *.dump} + {@code *.meta.json}.
 * Lỗi truy cập dữ liệu không được làm hỏng thao tác sao lưu đã thành công; bên gọi chỉ ghi log
 * cảnh báo.
 */
public interface LichSuSaoLuuDao {

  /**
   * Ghi một bản ghi lịch sử mới bằng kết nối riêng (tự động xác nhận).
   *
   * <p>Cột {@code "id"} do cơ sở dữ liệu sinh nên giá trị {@code id} trong bản ghi bị bỏ qua.
   *
   * @param banGhi bản ghi cần lưu; không được {@code null}
   * @throws IllegalArgumentException nếu bản ghi {@code null}
   * @throws com.dtaq.retail.exception.DataAccessException nếu không ghi được, ví dụ trùng
   *     {@code filePath}
   */
  void insert(BackupRecord banGhi);

  /**
   * Liệt kê toàn bộ lịch sử, mới nhất trước.
   *
   * @return danh sách bản ghi sắp theo {@code createdAt} giảm dần, có thể rỗng
   */
  List<BackupRecord> findAll();

  /**
   * Lấy bản ghi thành công gần nhất.
   *
   * @return bản ghi {@code SUCCESS} mới nhất, hoặc rỗng nếu chưa có lần sao lưu thành công nào
   */
  Optional<BackupRecord> findMoiNhatThanhCong();

  /**
   * Kiểm tra một đường dẫn tệp đã có trong lịch sử hay chưa.
   *
   * @param filePath đường dẫn tệp dump cần kiểm tra
   * @return {@code true} nếu đã tồn tại bản ghi cùng {@code filePath}
   */
  boolean existsByFilePath(String filePath);
}
