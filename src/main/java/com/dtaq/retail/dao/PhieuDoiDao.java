package com.dtaq.retail.dao;

import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.TrangThaiDuyet;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Giao diện truy cập dữ liệu phiếu đổi hàng trên bảng {@code "PhieuDoi"}.
 *
 * <p>Giao diện phục vụ hai nghiệp vụ: lập phiếu đổi và hoàn tất phiếu đổi (UC006) cùng việc duyệt
 * yêu cầu đổi hàng (UC018).
 *
 * <p>Các phương thức nhận tham số {@link Connection} dùng chính kết nối của bên gọi: lớp triển
 * khai không đóng kết nối, không đổi chế độ {@code auto-commit} và không tự gọi
 * {@code commit}/{@code rollback}. Bên gọi chịu trách nhiệm quản lý giao dịch để nhiều bảng được
 * ghi hoặc quay lui cùng nhau.
 */
public interface PhieuDoiDao {

  /**
   * Lấy danh sách phiếu đổi đang ở trạng thái {@code CHO_DUYET}.
   *
   * <p>Kết quả sắp xếp theo ngày tạo tăng dần, sau đó theo mã phiếu để thứ tự hiển thị ổn định.
   *
   * @return danh sách phiếu chờ duyệt, không bao giờ {@code null}, có thể rỗng
   */
  List<PhieuDuyet> findChoDuyet();

  /**
   * Ghi quyết định duyệt hoặc từ chối cho một phiếu đổi.
   *
   * <p>Chỉ những dòng còn {@code "trangThaiDuyet" = 'CHO_DUYET'} mới được cập nhật, nhờ đó hai
   * người duyệt không thể ghi đè quyết định của nhau.
   *
   * @param maPhieu mã phiếu đổi cần cập nhật
   * @param quyetDinh quyết định của Quản lý ({@link TrangThaiDuyet#DA_DUYET} hoặc
   *     {@link TrangThaiDuyet#TU_CHOI}); không được {@code null}
   * @param maNhanVienDuyet mã nhân viên Quản lý ra quyết định; không được {@code null}
   * @param ngayDuyet ngày ra quyết định; không được {@code null}
   * @param yKien ý kiến/lý do của Quản lý, được lưu nguyên văn; có thể {@code null}
   * @return {@code true} nếu cập nhật đúng một dòng, {@code false} nếu không có dòng nào bị ảnh hưởng
   */
  boolean capNhatQuyetDinh(
      String maPhieu,
      TrangThaiDuyet quyetDinh,
      String maNhanVienDuyet,
      LocalDate ngayDuyet,
      String yKien);

  /**
   * Tìm những serial trong một hóa đơn đã từng nằm trên phiếu trả hoặc phiếu đổi.
   *
   * <p>Dùng để chặn đổi/trả hai lần cho cùng một máy (UC006 luồng phụ 7.a). Truy vấn chỉ đọc, gộp
   * hai bảng {@code "ChiTietPhieuTra"} và {@code "ChiTietPhieuDoi"}.
   *
   * @param maHD mã hóa đơn gốc; nếu {@code null} hoặc rỗng thì trả về tập rỗng
   * @param soSerials các serial cần kiểm tra; nếu {@code null} hoặc rỗng thì trả về tập rỗng
   * @return tập <b>không thứ tự</b> các serial đã từng đổi/trả trên phiếu còn hiệu lực (bỏ qua
   *     phiếu bị từ chối hoặc đã huỷ), không bao giờ {@code null}
   */
  Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials);

  /**
   * Tìm một phiếu đổi theo mã.
   *
   * @param maPhieuDoi mã phiếu đổi; nếu {@code null} hoặc rỗng thì trả về {@link Optional#empty()}
   * @return phiếu đổi kèm nhân viên lập, nhân viên duyệt và hóa đơn gốc; rỗng nếu không tồn tại
   */
  Optional<PhieuDoi> findById(String maPhieuDoi);

  /**
   * Lấy chi tiết của một phiếu đổi.
   *
   * @param maPhieuDoi mã phiếu đổi; nếu phiếu không tồn tại thì trả về danh sách rỗng
   * @return các dòng chi tiết kèm dòng hóa đơn gốc và máy thay thế, không bao giờ {@code null}
   */
  List<ChiTietPhieuDoi> findChiTiet(String maPhieuDoi);

  /**
   * Lấy danh sách phiếu đổi đã được duyệt và đang chờ hoàn tất.
   *
   * <p>Điều kiện: {@code "trangThaiDuyet" = 'DA_DUYET'} và
   * {@code "trangThaiXuLyPhieu" = 'CHO_XU_LY'}. Số tiền hoàn dự kiến bằng {@code -tienChenhLech}
   * khi chênh lệch âm (cửa hàng phải hoàn tiền), ngược lại bằng {@code 0}.
   *
   * @return danh sách phiếu chờ hoàn tất, không bao giờ {@code null}, có thể rỗng
   */
  List<PhieuDuyet> findChoHoanTat();

  /**
   * Ghi đầu phiếu đổi và toàn bộ dòng chi tiết trong giao dịch của bên gọi.
   *
   * <p>Mã phiếu được sinh bên trong giao dịch bằng quy ước {@code PD-YYYY-NNNNNN}; giá trị
   * {@code maPhieuDoi} mà entity đầu vào có thể chứa bị bỏ qua. Cột {@code "maHD"} của từng dòng
   * chi tiết luôn lấy từ hóa đơn gốc của đầu phiếu ({@code phieuDoi.hoaDon().maHD()}), không lấy
   * từ dữ liệu dòng, nhằm cưỡng chế RB24.
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param phieuDoi đầu phiếu cần ghi; không được {@code null}
   * @param chiTiet các dòng chi tiết cần ghi; không được {@code null}, có thể rỗng
   * @return mã phiếu đổi vừa sinh
   * @throws IllegalArgumentException nếu đầu phiếu hoặc dòng chi tiết thiếu dữ liệu bắt buộc
   * @throws IllegalStateException nếu một máy đã nằm trên phiếu đổi/trả còn hiệu lực; việc kiểm tra
   *     chạy trên chính {@code ketNoi} và trong cùng transaction với các lệnh INSERT
   */
  String taoPhieuDoi(Connection ketNoi, PhieuDoi phieuDoi, List<ChiTietPhieuDoi> chiTiet);

  /**
   * Cập nhật trạng thái các máy khách trả lại trong giao dịch của bên gọi.
   *
   * <p>Mỗi dòng rẽ nhánh theo {@code tinhTrangHangTra}: máy còn nguyên vẹn chuyển
   * {@code DA_BAN} → {@code SAN_SANG_BAN}; máy lỗi chuyển {@code DA_BAN} → {@code HANG_LOI}
   * (UC006 bước 15.a). Đồng thời tăng {@code "soLanDoiTra"} thêm một. Bộ đếm
   * {@code "DongSanPham"."soLuongTon"} do trigger V6 tự tính lại.
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param chiTiet các dòng chi tiết phiếu đổi cần xử lý; không được {@code null} và mỗi dòng phải
   *     có {@code tinhTrangHangTra}
   * @return tổng số máy đã cập nhật
   * @throws IllegalArgumentException nếu một dòng thiếu tình trạng hàng trả
   * @throws IllegalStateException nếu một serial không còn ở trạng thái {@code DA_BAN}
   */
  int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet);

  /**
   * Đánh dấu các máy thay thế đã giao cho khách trong giao dịch của bên gọi.
   *
   * <p>Mỗi máy chuyển từ {@code SAN_SANG_BAN} sang {@code DA_BAN}; bộ đếm tồn do trigger V6 tự
   * tính lại.
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param chiTiet các dòng chi tiết phiếu đổi chứa máy thay thế; không được {@code null}
   * @return tổng số máy đã cập nhật
   * @throws IllegalStateException nếu một serial không còn ở trạng thái {@code SAN_SANG_BAN}
   */
  int danhDauMayThayThe(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet);

  /**
   * Đánh dấu phiếu đổi đã hoàn tất trong giao dịch của bên gọi.
   *
   * <p>Chỉ cập nhật khi phiếu còn {@code "trangThaiXuLyPhieu" = 'CHO_XU_LY'} (so sánh và ghi kiểu
   * CAS) để hai lần hoàn tất không thể ghi đè nhau.
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param maPhieuDoi mã phiếu đổi cần hoàn tất
   * @param ngayHoanTat ngày hoàn tất; không được {@code null} và không được trước ngày tạo
   * @return {@code true} nếu cập nhật đúng một dòng, {@code false} nếu phiếu không ở trạng thái chờ xử lý
   */
  boolean capNhatHoanTat(Connection ketNoi, String maPhieuDoi, LocalDate ngayHoanTat);
}
