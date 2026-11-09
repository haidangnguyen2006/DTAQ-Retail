package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.TrangChuDao;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link TrangChuDaoImpl} trên cơ sở dữ liệu phát triển. */
class TrangChuDaoImplTest {

  /** Ngày lập của HD-2026-000001 trong seed V4. */
  private static final LocalDate NGAY_SEED = LocalDate.of(2026, 1, 5);

  private TrangChuDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new TrangChuDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Tổng tiền KPI bằng tổng các hóa đơn đã thanh toán trong ngày")
  void tongQuanKhopTongHoaDon() {
    TrangChuDao.TongQuanBanHang tong = dao.tongQuanBanHang(NGAY_SEED, null);
    Assumptions.assumeTrue(tong.soHoaDon() > 0, "seed V4 chưa được áp dụng");

    List<TrangChuDao.HoaDonGanDay> hoaDon = dao.hoaDonGanDay(NGAY_SEED, null, 1000);
    long tongDaThanhToan = hoaDon.stream()
        .filter(TrangChuDao.HoaDonGanDay::daThanhToan)
        .mapToLong(TrangChuDao.HoaDonGanDay::tongThanhToan)
        .sum();
    assertEquals(tong.tongThu(), tongDaThanhToan);
    assertEquals(tong.soHoaDon(), hoaDon.stream().filter(TrangChuDao.HoaDonGanDay::daThanhToan).count());
    assertTrue(tong.tienThue() <= tong.tongThu());
    assertTrue(tong.soKhach() <= tong.soHoaDon());
  }

  @Test
  @DisplayName("Ngày không có giao dịch trả về số 0 và danh sách rỗng")
  void ngayTrongTraVeKhong() {
    LocalDate ngay = LocalDate.of(1999, 1, 1);
    assertEquals(new TrangChuDao.TongQuanBanHang(0, 0, 0, 0), dao.tongQuanBanHang(ngay, null));
    assertTrue(dao.hoaDonGanDay(ngay, "NV-KHONG-TON-TAI", 10).isEmpty());
    assertEquals(new TrangChuDao.DoiTraTrongNgay(0, 0, 0), dao.doiTraTrongNgay(ngay));
    assertTrue(dao.saoLuuTrongNgay(ngay).isEmpty());
  }

  @Test
  @DisplayName("Giới hạn số dòng phải dương")
  void gioiHanKhongDuong() {
    assertThrows(IllegalArgumentException.class, () -> dao.hoaDonGanDay(NGAY_SEED, null, 0));
  }
}
