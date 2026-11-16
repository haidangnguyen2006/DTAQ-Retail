package com.dtaq.retail.exception;

/** Unchecked exception thrown when a database or persistence operation fails. */
public class DataAccessException extends RuntimeException {

  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataAccessException(String message) {
    super(message);
  }

  /**
   * Chọn thông báo hiển thị cho người dùng: lỗi do ứng dụng tạo thì dùng thông điệp tiếng Việt của
   * nó, còn lỗi thô của JDBC (tên ràng buộc, câu SQL) được thay bằng câu mặc định.
   *
   * @param loi lỗi vừa bắt được
   * @param macDinh câu hiển thị khi lỗi không có thông điệp dành cho người dùng
   * @return thông báo an toàn để đưa lên giao diện
   */
  public static String thongBao(Throwable loi, String macDinh) {
    if (loi == null || loi instanceof java.sql.SQLException
        || loi.getMessage() == null || loi.getMessage().isBlank()) {
      return macDinh;
    }
    return loi.getMessage();
  }
}
