package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Chuẩn hoá, kiểm tra hồ sơ và tìm không dấu")
class KiemTraHoSoTest {

  @Test
  @DisplayName("Họ tên: chỉ chữ cái, không số, không ký tự lạ; tự viết hoa chữ đầu")
  void hoTen() {
    assertEquals("Nguyễn Văn An", KiemTraHoSo.chuanHoaHoTen("  nguyễn   VĂN an "));
    assertNull(KiemTraHoSo.kiemTraHoTen("Nguyễn Văn An"));
    assertNull(KiemTraHoSo.kiemTraHoTen("Lê Thị Thu-Hà"));
    assertNull(KiemTraHoSo.kiemTraHoTen("Đặng"));
    assertNotNull(KiemTraHoSo.kiemTraHoTen("Nguyễn Văn 2"));
    assertNotNull(KiemTraHoSo.kiemTraHoTen("An@123"));
    assertNotNull(KiemTraHoSo.kiemTraHoTen("An_Bình"));
    assertNotNull(KiemTraHoSo.kiemTraHoTen("   "));
    assertNotNull(KiemTraHoSo.kiemTraHoTen("A"));
    assertNotNull(KiemTraHoSo.kiemTraHoTen("A".repeat(101)));
  }

  @Test
  @DisplayName("Số điện thoại: chuẩn hoá +84, khoảng trắng, chấm, gạch")
  void soDienThoai() {
    assertEquals("0902100001", KiemTraHoSo.chuanHoaSdt("+84 902-100.001"));
    assertEquals("0902100001", KiemTraHoSo.chuanHoaSdt("84902100001"));
    assertNull(KiemTraHoSo.kiemTraSdt("(0902) 100 001"));
    assertNotNull(KiemTraHoSo.kiemTraSdt("0402100001"));
    assertNotNull(KiemTraHoSo.kiemTraSdt("090210000"));
    assertNotNull(KiemTraHoSo.kiemTraSdt("abc"));
  }

  @Test
  @DisplayName("Email và địa chỉ không bắt buộc nhưng phải hợp lệ nếu có")
  void emailDiaChi() {
    assertNull(KiemTraHoSo.kiemTraEmail(""));
    assertNull(KiemTraHoSo.kiemTraEmail("an.nv@dtaq.com.vn"));
    assertNotNull(KiemTraHoSo.kiemTraEmail("an@dtaq"));
    assertNotNull(KiemTraHoSo.kiemTraEmail("an nv@dtaq.vn"));
    assertNull(KiemTraHoSo.kiemTraDiaChi(null));
    assertNotNull(KiemTraHoSo.kiemTraDiaChi("x".repeat(256)));
  }

  @Test
  @DisplayName("Tìm không dấu theo tên, theo một phần số điện thoại từ 3 chữ số")
  void khop() {
    assertTrue(KiemTraHoSo.khop("nguyen van", "0902100001", "Nguyễn Văn An"));
    assertTrue(KiemTraHoSo.khop("ĐẶNG", "0902100001", "Đặng Hải"));
    assertTrue(KiemTraHoSo.khop("dang", "0902100001", "Đặng Hải"));
    assertTrue(KiemTraHoSo.khop("100 001", "0902100001", "An"));
    assertTrue(KiemTraHoSo.khop("", "0902100001", "An"));
    assertFalse(KiemTraHoSo.khop("01", "0902100001", "An"));
    assertFalse(KiemTraHoSo.khop("binh", "0902100001", "Nguyễn Văn An"));
  }
}
