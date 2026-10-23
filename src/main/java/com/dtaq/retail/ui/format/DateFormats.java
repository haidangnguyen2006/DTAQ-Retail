package com.dtaq.retail.ui.format;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Định dạng ngày giờ chuẩn hiển thị trên toàn bộ giao diện DTAQ.
 */
public final class DateFormats {

  public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");
  public static final DateTimeFormatter DATE_TIME_BULLET = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm");
  public static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
  public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private DateFormats() {
  }

  public static String formatDate(LocalDate date) {
    return (date != null) ? DATE.format(date) : "";
  }

  public static String formatDateTime(LocalDateTime dateTime) {
    return (dateTime != null) ? DATE_TIME.format(dateTime) : "";
  }

  public static String formatDateTimeBullet(LocalDateTime dateTime) {
    return (dateTime != null) ? DATE_TIME_BULLET.format(dateTime) : "";
  }

  public static String formatDayMonth(LocalDate date) {
    return (date != null) ? DAY_MONTH.format(date) : "";
  }

  public static String formatTime(LocalDateTime dateTime) {
    return (dateTime != null) ? TIME.format(dateTime) : "";
  }
}
