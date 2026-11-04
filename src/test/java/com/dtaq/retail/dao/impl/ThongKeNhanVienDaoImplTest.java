package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DieuChinhDoiChieu;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DongThongKeNhanVien;
import com.dtaq.retail.dao.ThongKeNhanVienDao.HoaDonDoiChieu;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link ThongKeNhanVienDaoImpl} trên dữ liệu mẫu V4. */
class ThongKeNhanVienDaoImplTest {

  private static final String NV_HAI_HOA_DON = "NVBANHANG-2025-001";
  private static final String NV_HOA_DON_THANG_6 = "NVBANHANG-2026-002";
  private static final LocalDate QUY_1_TU = LocalDate.of(2026, 1, 1);
  private static final LocalDate QUY_1_DEN = LocalDate.of(2026, 3, 31);

  private ThongKeNhanVienDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new ThongKeNhanVienDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Nhân viên có 2 hóa đơn và 1 phiếu trả hoàn tất: số liệu khớp với danh sách đối chiếu")
  void haiHoaDonMotPhieuTra() {
    DongThongKeNhanVien dong = timDong(dao.thongKe(QUY_1_TU, QUY_1_DEN, null), NV_HAI_HOA_DON);
    List<HoaDonDoiChieu> hoaDons = dao.hoaDonTrongKy(QUY_1_TU, QUY_1_DEN, NV_HAI_HOA_DON);
    List<DieuChinhDoiChieu> dieuChinhs = dao.dieuChinhTrongKy(QUY_1_TU, QUY_1_DEN, NV_HAI_HOA_DON);
    Assumptions.assumeTrue(hoaDons.size() == 2, "seed V4 chưa được áp dụng");

    assertEquals(2, dong.soHoaDon());
    assertEquals(1, dieuChinhs.size(), "phiếu trả chờ xử lý không được tính");
    assertEquals(0, tong(hoaDons.stream().map(HoaDonDoiChieu::tienTruocThue).toList()).compareTo(dong.doanhSo()));
    assertEquals(0, tong(dieuChinhs.stream().map(DieuChinhDoiChieu::giaTriTru).toList()).compareTo(dong.dieuChinh()));
    assertEquals(0, dong.doanhSo().subtract(dong.dieuChinh()).compareTo(dong.sauDieuChinh()));
  }

  @Test
  @DisplayName("Mọi dòng thống kê khớp với dữ liệu đối chiếu của chính nhân viên đó")
  void moiDongKhopDoiChieu() {
    List<DongThongKeNhanVien> dongs = dao.thongKe(QUY_1_TU, LocalDate.of(2026, 12, 31), null);
    Assumptions.assumeFalse(dongs.isEmpty(), "seed V4 chưa được áp dụng");
    for (DongThongKeNhanVien dong : dongs) {
      List<HoaDonDoiChieu> hoaDons = dao.hoaDonTrongKy(QUY_1_TU, LocalDate.of(2026, 12, 31), dong.maNV());
      List<DieuChinhDoiChieu> dieuChinhs = dao.dieuChinhTrongKy(QUY_1_TU, LocalDate.of(2026, 12, 31), dong.maNV());
      assertEquals(hoaDons.size(), dong.soHoaDon(), dong.maNV());
      assertEquals(0, tong(hoaDons.stream().map(HoaDonDoiChieu::tienTruocThue).toList()).compareTo(dong.doanhSo()), dong.maNV());
      assertEquals(0, tong(dieuChinhs.stream().map(DieuChinhDoiChieu::giaTriTru).toList()).compareTo(dong.dieuChinh()), dong.maNV());
    }
  }

  @Test
  @DisplayName("Phiếu trả hoàn tất trong kỳ trên hóa đơn ngoài kỳ: quy về nhân viên lập hóa đơn gốc")
  void dieuChinhQuyVeNhanVienLapHoaDonGoc() {
    LocalDate tu = LocalDate.of(2026, 7, 1);
    LocalDate den = LocalDate.of(2026, 7, 31);
    List<DieuChinhDoiChieu> dieuChinhs = dao.dieuChinhTrongKy(tu, den, NV_HOA_DON_THANG_6);
    Assumptions.assumeTrue(dieuChinhs.size() == 2, "seed V4 chưa được áp dụng");

    DongThongKeNhanVien dong = timDong(dao.thongKe(tu, den, null), NV_HOA_DON_THANG_6);
    assertEquals(0, dong.soHoaDon(), "hóa đơn gốc lập tháng 6, không thuộc kỳ");
    assertTrue(dong.dieuChinh().signum() > 0);
    assertTrue(dong.sauDieuChinh().signum() < 0, "chỉ có hàng trả thì doanh số sau điều chỉnh âm");
    assertTrue(dieuChinhs.stream().allMatch(dc -> dc.maHD().equals("HD-2026-000024")));
  }

  @Test
  @DisplayName("Phiếu chờ xử lý hoặc đã hủy không làm giảm doanh số")
  void loaiPhieuChuaHoanTat() {
    List<DongThongKeNhanVien> thang3 = dao.thongKe(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);
    Assumptions.assumeFalse(thang3.isEmpty(), "seed V4 chưa được áp dụng");
    assertTrue(thang3.stream().allMatch(d -> d.dieuChinh().signum() == 0));
  }

  @Test
  @DisplayName("Lọc một nhân viên chỉ trả về nhân viên đó; kỳ không có phát sinh trả về rỗng")
  void locNhanVienVaKyRong() {
    List<DongThongKeNhanVien> dongs = dao.thongKe(QUY_1_TU, QUY_1_DEN, NV_HAI_HOA_DON);
    Assumptions.assumeFalse(dongs.isEmpty(), "seed V4 chưa được áp dụng");
    assertTrue(dongs.stream().allMatch(d -> d.maNV().equals(NV_HAI_HOA_DON)));
    assertEquals(1, dongs.size());

    assertTrue(dao.thongKe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31), null).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> dao.thongKe(QUY_1_DEN, QUY_1_TU, null));
  }

  @Test
  @DisplayName("Nhân viên đã khóa vẫn hiện lịch sử bán hàng")
  void nhanVienDaKhoaVanHien() {
    NhanVienDaoImpl nhanVienDao = new NhanVienDaoImpl(ConnectDB.getDataSource());
    Assumptions.assumeTrue(nhanVienDao.findByMa(NV_HAI_HOA_DON).isPresent(), "seed V4 chưa được áp dụng");
    try {
      nhanVienDao.updateTrangThai(NV_HAI_HOA_DON, false);
      DongThongKeNhanVien dong = timDong(dao.thongKe(QUY_1_TU, QUY_1_DEN, null), NV_HAI_HOA_DON);
      assertEquals(false, dong.dangHoatDong());
      assertEquals(2, dong.soHoaDon());
    } finally {
      nhanVienDao.updateTrangThai(NV_HAI_HOA_DON, true);
    }
  }

  private static DongThongKeNhanVien timDong(List<DongThongKeNhanVien> dongs, String maNV) {
    Optional<DongThongKeNhanVien> dong = dongs.stream().filter(d -> d.maNV().equals(maNV)).findFirst();
    Assumptions.assumeTrue(dong.isPresent(), "seed V4 chưa được áp dụng");
    return dong.get();
  }

  private static BigDecimal tong(List<BigDecimal> giaTri) {
    return giaTri.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
