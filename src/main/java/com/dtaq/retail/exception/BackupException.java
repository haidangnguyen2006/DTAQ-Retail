package com.dtaq.retail.exception;

/** Lỗi khi sao lưu, kiểm tra hoặc phục hồi cơ sở dữ liệu. */
public class BackupException extends RuntimeException {

  public BackupException(String message) {
    super(message);
  }

  public BackupException(String message, Throwable cause) {
    super(message, cause);
  }
}
