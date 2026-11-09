package com.dtaq.retail.exception;

/** Unchecked exception thrown when an Excel report file cannot be written. */
public class ExcelExportException extends RuntimeException {

  public ExcelExportException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExcelExportException(String message) {
    super(message);
  }
}
