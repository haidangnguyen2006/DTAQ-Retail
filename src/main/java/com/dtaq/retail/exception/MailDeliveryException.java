package com.dtaq.retail.exception;

/** Unchecked exception thrown when an email cannot be delivered. */
public class MailDeliveryException extends RuntimeException {

  public MailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }

  public MailDeliveryException(String message) {
    super(message);
  }
}
