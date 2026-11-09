package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DongSanPhamControllerTest {

  @Test
  @DisplayName("Chuẩn hóa đơn vị tính chỉ chấp nhận Chiếc hoặc Cái")
  void normalizeDonViTinh_chuanHoaDungQuyTac() {
    assertEquals("Chiếc", DongSanPhamController.normalizeDonViTinh("chiếc"));
    assertEquals("Chiếc", DongSanPhamController.normalizeDonViTinh("CHIẾC"));
    assertEquals("Cái", DongSanPhamController.normalizeDonViTinh("cái"));
    assertEquals("Cái", DongSanPhamController.normalizeDonViTinh("CAI"));
    assertNull(DongSanPhamController.normalizeDonViTinh("bộ"));
    assertNull(DongSanPhamController.normalizeDonViTinh("hộp"));
  }
}
