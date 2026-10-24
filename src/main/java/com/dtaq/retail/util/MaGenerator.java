package com.dtaq.retail.util;

import com.dtaq.retail.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Sinh mã định danh theo quy ước {@code TIỀN_TỐ-YYYY-NNNNNN} (quyết định D7).
 *
 * <p>Mã được sinh ở tầng ứng dụng, không dùng PostgreSQL sequence hay cột identity. Số thứ tự của một
 * năm được xác định bằng {@code MAX(phần số của các mã cùng năm) + 1}, đọc trực tiếp trên
 * {@link Connection} do bên gọi truyền vào. Nhờ vậy bước đọc số thứ tự và bước ghi bản ghi mới nằm
 * trong cùng một giao dịch của bên gọi: nếu giao dịch quay lui thì mã vừa cấp cũng không tồn tại.
 *
 * <p>Tên bảng và tên cột không thể truyền bằng tham số {@link PreparedStatement} nên chúng được
 * kiểm tra theo danh sách trắng (chỉ chữ cái, chữ số, bắt đầu bằng chữ cái) rồi mới được bọc trong
 * dấu nháy kép. Việc bọc nháy kép là bắt buộc vì PostgreSQL hạ chữ hoa trong định danh không được
 * trích dẫn xuống chữ thường ({@code "HoaDon"} khác {@code HoaDon}).
 */
public final class MaGenerator {

  private static final Pattern BANG_COT = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
  private static final Pattern TIEN_TO = Pattern.compile("[A-Z][A-Z0-9]*");
  private static final int SO_CHU_SO = 6;

  private MaGenerator() {
  }

  /**
   * Sinh mã kế tiếp cho năm hiện tại.
   *
   * @param ketNoi kết nối của bên gọi (không bị đóng bởi lớp này)
   * @param bang tên bảng chứa cột mã
   * @param cotMa tên cột mã
   * @param tienTo tiền tố mã, chỉ gồm chữ in hoa và chữ số
   * @return mã mới dạng {@code TIỀN_TỐ-YYYY-NNNNNN}
   */
  public static String next(Connection ketNoi, String bang, String cotMa, String tienTo) {
    return next(ketNoi, bang, cotMa, tienTo, LocalDate.now().getYear());
  }

  /**
   * Sinh mã kế tiếp cho một năm xác định.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null} và không bị đóng ở đây
   * @param bang tên bảng chứa cột mã
   * @param cotMa tên cột mã
   * @param tienTo tiền tố mã, chỉ gồm chữ in hoa và chữ số
   * @param nam năm dùng để tạo phần giữa của mã
   * @return mã mới dạng {@code TIỀN_TỐ-YYYY-NNNNNN}
   * @throws IllegalArgumentException nếu tham số không hợp lệ
   * @throws DataAccessException nếu truy vấn cơ sở dữ liệu thất bại
   */
  public static String next(
      Connection ketNoi, String bang, String cotMa, String tienTo, int nam) {
    if (ketNoi == null) {
      throw new IllegalArgumentException("Kết nối không được để trống");
    }
    if (bang == null || bang.isBlank() || !BANG_COT.matcher(bang).matches()) {
      throw new IllegalArgumentException("Tên bảng không hợp lệ: " + bang);
    }
    if (cotMa == null || cotMa.isBlank() || !BANG_COT.matcher(cotMa).matches()) {
      throw new IllegalArgumentException("Tên cột không hợp lệ: " + cotMa);
    }
    if (tienTo == null || tienTo.isBlank() || !TIEN_TO.matcher(tienTo).matches()) {
      throw new IllegalArgumentException("Tiền tố mã không hợp lệ: " + tienTo);
    }

    String goc = tienTo + "-" + nam + "-";
    String cotDaTrich = '"' + cotMa + '"';
    String bangDaTrich = '"' + bang + '"';
    String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING("
        + cotDaTrich
        + " FROM ?) AS INTEGER)), 0) FROM "
        + bangDaTrich
        + " WHERE "
        + cotDaTrich
        + " LIKE ?";

    try (PreparedStatement statement = ketNoi.prepareStatement(sql)) {
      statement.setInt(1, goc.length() + 1);
      statement.setString(2, goc + "%");
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        long max = resultSet.getLong(1);
        long tiepTheo = max + 1;
        return String.format("%s%0" + SO_CHU_SO + "d", goc, tiepTheo);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể sinh mã cho bảng " + bang, e);
    }
  }
}
