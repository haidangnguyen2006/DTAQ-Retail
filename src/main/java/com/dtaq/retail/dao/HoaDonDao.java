package com.dtaq.retail.dao;

import com.dtaq.retail.entity.HoaDon;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Truy vấn và ghi hóa đơn bán lẻ. */
public interface HoaDonDao {

  /**
   * Ghi một hóa đơn mới vào bảng {@code "HoaDon"}.
   *
   * <p>Bên gọi sở hữu kết nối và giao dịch: kết nối được truyền vào không bị đóng ở đây và chế độ tự
   * động xác nhận (auto-commit) của nó không bị thay đổi. Nhờ vậy bước sinh mã, bước ghi hóa đơn và
   * bước ghi chi tiết nằm trong cùng một giao dịch của bên gọi.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param hoaDon hóa đơn cần ghi; mã hóa đơn phải có sẵn và không được để trống
   * @throws IllegalArgumentException nếu hóa đơn {@code null}, mã hóa đơn để trống, hoặc thiếu nhân
   *     viên lập / khách hàng
   */
  void insert(Connection ketNoi, HoaDon hoaDon);

  /**
   * Tìm hóa đơn theo mã, kèm nhân viên lập và khách hàng của hóa đơn.
   *
   * @param maHD mã hóa đơn cần tra cứu
   * @return hóa đơn tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<HoaDon> findById(String maHD);

  /**
   * Liệt kê các hóa đơn đã thanh toán trong một khoảng ngày lập (bao gồm hai đầu mút).
   *
   * <p>Kết quả sắp xếp theo ngày lập tăng dần, sau đó theo mã hóa đơn.
   *
   * @param tuNgay ngày lập đầu tiên được lấy; không được {@code null}
   * @param denNgay ngày lập cuối cùng được lấy; không được {@code null}
   * @return danh sách hóa đơn đã thanh toán trong khoảng ngày, có thể rỗng
   * @throws IllegalArgumentException nếu một trong hai ngày {@code null} hoặc
   *     {@code tuNgay} sau {@code denNgay}
   */
  List<HoaDon> findDaThanhToanTrongKy(LocalDate tuNgay, LocalDate denNgay);
}
