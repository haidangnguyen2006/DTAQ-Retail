package com.dtaq.retail.dao;

import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Giao diện truy cập dữ liệu phiếu trả hàng trên bảng {@code "PhieuTra"}.
 *
 * <p>Giao diện phục vụ hai nghiệp vụ: lập phiếu trả và hoàn tất phiếu trả (UC006) cùng việc duyệt
 * yêu cầu trả hàng (UC018).
 *
 * <p>Các phương thức nhận tham số {@link Connection} dùng chính kết nối của bên gọi: lớp triển
 * khai không đóng kết nối, không đổi chế độ {@code auto-commit} và không tự gọi
 * {@code commit}/{@code rollback}. Bên gọi chịu trách nhiệm quản lý giao dịch để nhiều bảng được
 * ghi hoặc quay lui cùng nhau.
 */
public interface PhieuTraDao {

  /**
   * Lấy danh sách phiếu trả đang ở trạng thái {@code CHO_DUYET}.
   *
   * <p>Kết quả sắp xếp theo ngày tạo tăng dần, sau đó theo mã phiếu để thứ tự hiển thị ổn định.
   *
   * @return danh sách phiếu chờ duyệt, không bao giờ {@code null}, có thể rỗng
   */
  List<PhieuDuyet> findChoDuyet();

  /**
   * Ghi quyết định duyệt hoặc từ chối cho một phiếu trả.
   *
   * <p>Chỉ những dòng còn {@code "trangThaiDuyet" = 'CHO_DUYET'} mới được cập nhật, nhờ đó hai
   * người duyệt không thể ghi đè quyết định của nhau.
   *
   * @param maPhieu mã phiếu trả cần cập nhật
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
   * Tìm một phiếu trả theo mã.
   *
   * @param maPhieuTra mã phiếu trả; nếu {@code null} hoặc rỗng thì trả về {@link Optional#empty()}
   * @return phiếu trả kèm nhân viên lập, nhân viên duyệt và hóa đơn gốc; rỗng nếu không tồn tại
   */
  Optional<PhieuTra> findById(String maPhieuTra);

  /**
   * Lấy chi tiết của một phiếu trả.
   *
   * @param maPhieuTra mã phiếu trả; nếu phiếu không tồn tại thì trả về danh sách rỗng
   * @return các dòng chi tiết kèm dòng hóa đơn gốc tương ứng, không bao giờ {@code null}
   */
  List<ChiTietPhieuTra> findChiTiet(String maPhieuTra);

  /**
   * Lấy danh sách phiếu trả đã được duyệt và đang chờ hoàn tất.
   *
   * <p>Điều kiện: {@code "trangThaiDuyet" = 'DA_DUYET'} và
   * {@code "trangThaiXuLyPhieu" = 'CHO_XU_LY'}. Số tiền hoàn dự kiến lấy từ
   * {@code "tienKhachDua"} đã chốt khi lập phiếu.
   *
   * @return danh sách phiếu chờ hoàn tất, không bao giờ {@code null}, có thể rỗng
   */
  List<PhieuDuyet> findChoHoanTat();

  /**
   * Ghi đầu phiếu trả và toàn bộ dòng chi tiết trong giao dịch của bên gọi.
   *
   * <p>Mã phiếu được sinh bên trong giao dịch bằng quy ước {@code PT-YYYY-NNNNNN}; giá trị
   * {@code maPhieuTra} mà entity đầu vào có thể chứa bị bỏ qua. Cột {@code "maHD"} của từng dòng
   * chi tiết luôn lấy từ hóa đơn gốc của đầu phiếu ({@code phieuTra.hoaDon().maHD()}), không lấy
   * từ dữ liệu dòng, nhằm cưỡng chế RB24 (mọi dòng phải thuộc đúng hóa đơn gốc).
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param phieuTra đầu phiếu cần ghi; không được {@code null}
   * @param chiTiet các dòng chi tiết cần ghi; không được {@code null}, có thể rỗng
   * @return mã phiếu trả vừa sinh
   * @throws IllegalArgumentException nếu đầu phiếu hoặc dòng chi tiết thiếu dữ liệu bắt buộc
   * @throws IllegalStateException nếu một máy đã nằm trên phiếu đổi/trả còn hiệu lực; việc kiểm tra
   *     chạy trên chính {@code ketNoi} và trong cùng transaction với các lệnh INSERT
   */
  String taoPhieuTra(Connection ketNoi, PhieuTra phieuTra, List<ChiTietPhieuTra> chiTiet);

  /**
   * Cập nhật trạng thái các máy khách trả lại trong giao dịch của bên gọi.
   *
   * <p>Mỗi dòng chuyển máy từ {@code DA_BAN} sang {@code SAN_SANG_BAN} khi hàng còn nguyên vẹn,
   * hoặc sang {@code HANG_LOI} khi hàng lỗi; đồng thời tăng {@code "soLanDoiTra"} thêm một. Bộ đếm
   * {@code "DongSanPham"."soLuongTon"} do trigger V6 tự tính lại.
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param chiTiet các dòng chi tiết phiếu trả cần xử lý; không được {@code null}
   * @return tổng số máy đã cập nhật
   * @throws IllegalStateException nếu một serial không còn ở trạng thái {@code DA_BAN}
   */
  int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuTra> chiTiet);

  /**
   * Đánh dấu phiếu trả đã hoàn tất trong giao dịch của bên gọi.
   *
   * <p>Chỉ cập nhật khi phiếu còn {@code "trangThaiXuLyPhieu" = 'CHO_XU_LY'} (so sánh và ghi kiểu
   * CAS) để hai lần hoàn tất không thể ghi đè nhau.
   *
   * @param ketNoi kết nối do bên gọi quản lý; không bị đóng và không bị đổi {@code auto-commit}
   * @param maPhieuTra mã phiếu trả cần hoàn tất
   * @param ngayHoanTat ngày hoàn tất; không được {@code null} và không được trước ngày tạo
   * @return {@code true} nếu cập nhật đúng một dòng, {@code false} nếu phiếu không ở trạng thái chờ xử lý
   */
  boolean capNhatHoanTat(Connection ketNoi, String maPhieuTra, LocalDate ngayHoanTat);
}
