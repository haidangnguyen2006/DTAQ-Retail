package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.controller.ThongKeDoanhThuController.BaoCao;
import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.ThongKeDoanhThuDao;
import com.dtaq.retail.dao.ThongKeDoanhThuDao.DongDoanhThu;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.DateRange;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Thống kê doanh thu (UC017)")
class ThongKeDoanhThuControllerTest {

  private static final LocalDate NGAY = LocalDate.of(2026, 3, 10);
  private static final DateRange KY = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

  private List<DongDoanhThu> ban = List.of();
  private List<DongDoanhThu> tra = List.of();
  private int soHoaDon;
  private ThongKeDoanhThuController controller;

  @BeforeEach
  void setUp() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    ThongKeDoanhThuDao dao = new ThongKeDoanhThuDao() {
      @Override
      public List<DongDoanhThu> doanhThu(LocalDate tu, LocalDate den) {
        return ban;
      }

      @Override
      public List<DongDoanhThu> dieuChinh(LocalDate tu, LocalDate den) {
        return tra;
      }
    };
    // Controller chỉ dùng findDaThanhToanTrongKy để đếm số hóa đơn.
    HoaDonDao hoaDonDao = (HoaDonDao) Proxy.newProxyInstance(HoaDonDao.class.getClassLoader(),
        new Class<?>[] {HoaDonDao.class}, (proxy, method, args) -> {
          if (method.getName().equals("findDaThanhToanTrongKy")) {
            return Collections.nCopies(soHoaDon, null);
          }
          throw new UnsupportedOperationException(method.getName());
        });
    controller = new ThongKeDoanhThuController(dao, hoaDonDao);
  }

  @AfterEach
  void tearDown() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("1 hóa đơn 2 máy + 1 phiếu trả hoàn tất: 3 chỉ tiêu tách riêng, đã trừ hàng trả")
  void haiMayMotPhieuTra() {
    ban = List.of(
        dong("DSP-1", "Tủ lạnh", null, "20000000", "2000000"),
        dong("DSP-2", "Máy giặt", null, "10000000", "800000"));
    tra = List.of(dong("DSP-2", "Máy giặt", "PT-1", "-10000000", "-800000"));
    soHoaDon = 1;

    BaoCao baoCao = controller.xem(KY, 0);

    assertEquals(20_000_000L, baoCao.kpi().tongTruocThue());
    assertEquals(2_000_000L, baoCao.kpi().tongThue());
    assertEquals(22_000_000L, baoCao.kpi().tongDoanhThu());
    assertEquals(1, baoCao.kpi().soHoaDon());
    assertEquals(1, baoCao.kpi().soPhieuDoiTra());
    assertEquals(3, baoCao.dongExcel().size(), "Excel lấy đúng các dòng đang hiển thị");
    double tongCot = baoCao.doanhThuTheoThoiGian().stream().mapToDouble(p -> p.valueMillions()).sum();
    assertEquals(22.0, tongCot, 1e-9, "biểu đồ cột tính theo triệu đồng");
  }

  @Test
  @DisplayName("Biểu đồ tròn: tên dòng sản phẩm, bỏ dòng không còn doanh thu, mỗi phần một màu")
  void bieuDoTron() {
    ban = List.of(
        dong("DSP-1", "Tủ lạnh", null, "20000000", "0"),
        dong("DSP-2", "Máy giặt", null, "10000000", "0"),
        dong("DSP-3", "Máy lọc", null, "5000000", "0"));
    tra = List.of(dong("DSP-2", "Máy giặt", "PT-1", "-10000000", "0"));

    var slices = controller.xem(KY, 0).topDongSanPham();

    assertEquals(List.of("Tủ lạnh", "Máy lọc"), slices.stream().map(s -> s.name()).toList());
    assertTrue(!slices.get(0).color().equals(slices.get(1).color()));
  }

  @Test
  @DisplayName("Kỳ không có phát sinh: chỉ tiêu bằng 0 và không có dòng Excel")
  void kyRong() {
    BaoCao baoCao = controller.xem(KY, 2);
    assertEquals(0L, baoCao.kpi().tongDoanhThu());
    assertTrue(baoCao.dongExcel().isEmpty());
    assertTrue(baoCao.topDongSanPham().isEmpty());
  }

  @Test
  @DisplayName("Chỉ Quản lý được xem")
  void chiQuanLy() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    assertThrows(SecurityException.class, () -> controller.xem(KY, 0));
  }

  private static DongDoanhThu dong(String ma, String ten, String phieu, String truocThue, String thue) {
    return new DongDoanhThu(NGAY, ma, ten, phieu, new BigDecimal(truocThue), new BigDecimal(thue));
  }

  private static NhanVien nhanVien(LoaiQuyen quyen) {
    return new NhanVien("NV", "Nhân viên", "0900000000", null, LocalDate.of(2026, 1, 1), "nv", "hash",
        quyen, true);
  }
}
