package com.dtaq.retail.exception;

/** Unchecked exception thrown when a database or persistence operation fails. */
public class DataAccessException extends RuntimeException {

  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataAccessException(String message) {
    super(message);
  }
}
