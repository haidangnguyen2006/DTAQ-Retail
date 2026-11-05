package com.dtaq.retail.ui.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Kiểm thử định dạng kích thước tệp của {@link FileSizeFormat}. */
class FileSizeFormatTest {

  @Test
  void zeroAndNegativeBecomeZeroBytes() {
    assertEquals("0 B", FileSizeFormat.format(0));
    assertEquals("0 B", FileSizeFormat.format(-1024));
  }

  @Test
  void bytesBelowOneKilobyteHaveNoDecimals() {
    assertEquals("1023 B", FileSizeFormat.format(1023));
  }

  @Test
  void kilobytesUseOneDecimal() {
    assertEquals("1,0 KB", FileSizeFormat.format(1024));
    assertEquals("1,5 KB", FileSizeFormat.format(1536));
  }

  @Test
  void megabytesUseOneDecimal() {
    assertEquals("1,0 MB", FileSizeFormat.format(1048576L));
  }

  @Test
  void gigabytesUseOneDecimal() {
    assertEquals("1,0 GB", FileSizeFormat.format(1073741824L));
  }
}
