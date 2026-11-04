package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietHoaDonView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietPhieuView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.HoaDonTomTat;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.LoaiPhieu;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.PhieuTomTat;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link KhachHangGiaoDichDaoImpl} trên dữ liệu mẫu V4. */
class KhachHangGiaoDichDaoImplTest {

  private static final String KH_CO_PHIEU_TRA = "KH-2026-000001";
  private static final String HD_CO_PHIEU_TRA = "HD-2026-000001";
  private static final String PHIEU_TRA = "PT-2026-000001";
  private static final String KH_CO_PHIEU_DOI = "KH-2026-000013";
  private static final String PHIEU_DOI = "PD-2026-000001";

  private KhachHangGiaoDichDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new KhachHangGiaoDichDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Lịch sử hóa đơn: tổng tiền bằng tổng các dòng chi tiết đã chốt lúc bán")
  void hoaDonCuaKhachKhopChiTiet() {
    List<HoaDonTomTat> hoaDons = dao.hoaDonCuaKhach(KH_CO_PHIEU_TRA);
    Optional<HoaDonTomTat> hd = hoaDons.stream().filter(h -> h.maHD().equals(HD_CO_PHIEU_TRA)).findFirst();
    Assumptions.assumeTrue(hd.isPresent(), "seed V4 chưa được áp dụng");

    List<ChiTietHoaDonView> chiTiet = dao.chiTietHoaDon(HD_CO_PHIEU_TRA);
    assertEquals(chiTiet.size(), hd.get().soSanPham());
    BigDecimal tong = chiTiet.stream().map(ChiTietHoaDonView::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, tong.compareTo(hd.get().tongThanhToan()), "tổng hóa đơn phải bằng tổng chi tiết");
    assertEquals(0, hd.get().tienHang().add(hd.get().tienThue()).compareTo(hd.get().tongThanhToan()));
    for (ChiTietHoaDonView ct : chiTiet) {
      assertEquals(0, ct.tienSauKhuyenMai().add(ct.tienThue()).compareTo(ct.thanhTien()));
      assertTrue(ct.tenDongSP() != null && !ct.tenDongSP().isBlank());
    }
    for (int i = 1; i < hoaDons.size(); i++) {
      assertTrue(!hoaDons.get(i - 1).ngayLap().isBefore(hoaDons.get(i).ngayLap()), "mới nhất trước");
    }
  }

  @Test
  @DisplayName("Phiếu trả của khách và nội dung phiếu trả")
  void phieuTraVaNoiDung() {
    List<PhieuTomTat> phieus = dao.phieuCuaKhach(KH_CO_PHIEU_TRA);
    Optional<PhieuTomTat> pt = phieus.stream().filter(p -> p.maPhieu().equals(PHIEU_TRA)).findFirst();
    Assumptions.assumeTrue(pt.isPresent(), "seed V4 chưa được áp dụng");
    assertEquals(LoaiPhieu.TRA, pt.get().loai());
    assertEquals(HD_CO_PHIEU_TRA, pt.get().maHD());
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, pt.get().trangThaiXuLy());

    List<ChiTietPhieuView> chiTiet = dao.chiTietPhieu(LoaiPhieu.TRA, PHIEU_TRA);
    assertEquals(1, chiTiet.size());
    assertEquals(TinhTrangHangTra.HANG_LOI, chiTiet.get(0).tinhTrangHangTra());
    assertNull(chiTiet.get(0).serialThayThe());
    assertTrue(!chiTiet.get(0).lyDo().isBlank());
  }

  @Test
  @DisplayName("Phiếu đổi của khách và máy thay thế")
  void phieuDoiVaMayThayThe() {
    Optional<PhieuTomTat> pd = dao.phieuCuaKhach(KH_CO_PHIEU_DOI).stream()
        .filter(p -> p.maPhieu().equals(PHIEU_DOI)).findFirst();
    Assumptions.assumeTrue(pd.isPresent(), "seed V4 chưa được áp dụng");
    assertEquals(LoaiPhieu.DOI, pd.get().loai());

    List<ChiTietPhieuView> chiTiet = dao.chiTietPhieu(LoaiPhieu.DOI, PHIEU_DOI);
    assertEquals(1, chiTiet.size());
    assertNull(chiTiet.get(0).tinhTrangHangTra());
    assertTrue(chiTiet.get(0).serialThayThe() != null && !chiTiet.get(0).serialThayThe().equals(chiTiet.get(0).soSerial()));
  }

  @Test
  @DisplayName("Khách hoặc chứng từ không tồn tại: danh sách rỗng")
  void khongTonTaiTraVeRong() {
    assertTrue(dao.hoaDonCuaKhach("KH-9999-999999").isEmpty());
    assertTrue(dao.phieuCuaKhach("KH-9999-999999").isEmpty());
    assertTrue(dao.chiTietHoaDon("HD-9999-999999").isEmpty());
    assertTrue(dao.chiTietPhieu(LoaiPhieu.DOI, "PD-9999-999999").isEmpty());
  }
}
