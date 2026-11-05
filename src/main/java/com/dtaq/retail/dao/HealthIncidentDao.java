package com.dtaq.retail.dao;

import com.dtaq.retail.entity.HealthIncident;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn và ghi sự cố sức khỏe dữ liệu trên bảng {@code "SuCoSucKhoe"}.
 *
 * <p>Bảng là cache hiển thị/audit cho HealthMonitor; lỗi truy cập dữ liệu không được làm hỏng
 * chu kỳ kiểm tra sức khỏe, bên gọi chỉ ghi log cảnh báo.
 */
public interface HealthIncidentDao {

  /**
   * Ghi một sự cố mới bằng kết nối riêng (tự động xác nhận).
   *
   * <p>Cột {@code "id"} do cơ sở dữ liệu sinh nên giá trị {@code id} trong sự cố bị bỏ qua.
   *
   * @param incident sự cố cần lưu; không được {@code null}
   * @return khóa {@code id} do cơ sở dữ liệu sinh
   * @throws IllegalArgumentException nếu sự cố {@code null}
   * @throws com.dtaq.retail.exception.DataAccessException nếu không ghi được
   */
  long insert(HealthIncident incident);

  /**
   * Tìm một sự cố theo khóa kỹ thuật.
   *
   * @param id khóa kỹ thuật cần tra cứu
   * @return sự cố tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<HealthIncident> findById(long id);

  /**
   * Lấy sự cố {@code PENDING} mới nhất theo {@code detectedAt}.
   *
   * @return sự cố đang chờ xử lý mới nhất, hoặc rỗng nếu không còn sự cố nào chờ
   */
  Optional<HealthIncident> findMoiNhatDangCho();

  /**
   * Liệt kê toàn bộ sự cố, mới nhất trước.
   *
   * @return danh sách sự cố sắp theo {@code detectedAt} giảm dần, có thể rỗng
   */
  List<HealthIncident> findAll();

  /**
   * Kết thúc một sự cố bằng cách cập nhật trạng thái và thông tin xử lý.
   *
   * @param id khóa kỹ thuật của sự cố cần kết thúc
   * @param trangThai trạng thái kết thúc; không được {@code null}
   * @param resolvedAt thời điểm kết thúc; không được sớm hơn {@code detectedAt}
   * @param resolvedBy người hoặc tiến trình kết thúc, ví dụ {@code SYSTEM}
   * @param backupUsed đường dẫn tệp sao lưu đã dùng, có thể {@code null}
   * @throws com.dtaq.retail.exception.DataAccessException nếu không cập nhật đúng một dòng
   */
  void capNhatKetThuc(
      long id,
      HealthIncident.TrangThai trangThai,
      Instant resolvedAt,
      String resolvedBy,
      String backupUsed);
}
