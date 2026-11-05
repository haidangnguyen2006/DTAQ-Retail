package com.dtaq.retail.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DtaqSpacingTest {

  @Test
  void chuanHoaChieuCaoHangBang() {
    assertEquals(DtaqSpacing.TABLE_ROW_H_COMPACT, DtaqSpacing.normalizeRowHeight(36));
    assertEquals(DtaqSpacing.TABLE_ROW_H, DtaqSpacing.normalizeRowHeight(48));
    assertEquals(DtaqSpacing.TABLE_ROW_H, DtaqSpacing.normalizeRowHeight(50));
    assertEquals(DtaqSpacing.TABLE_ROW_H_TWO_LINE, DtaqSpacing.normalizeRowHeight(52));
    assertEquals(DtaqSpacing.TABLE_ROW_H_TWO_LINE, DtaqSpacing.normalizeRowHeight(64));
  }

  @Test
  void arcLaGapDoiBanKinh() {
    assertEquals(16, DtaqSpacing.arc(8));
  }
}
