package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.ThongKeSanPhamDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Thống kê doanh số theo sản phẩm (UC015)")
class ThongKeSanPhamControllerTest {

  private static final LocalDate TU = LocalDate.of(2026, 1, 1);
  private static final LocalDate DEN = LocalDate.of(2026, 1, 31);

  private int soLanGoiDao;

  private final ThongKeSanPhamController controller = new ThongKeSanPhamController(new ThongKeSanPhamDao() {
    @Override
    public List<DongThongKe> thongKe(LocalDate tuNgay, LocalDate denNgay, String maDongSP) {
      soLanGoiDao++;
      return List.of(
          new DongThongKe("DSP-1", "Tủ lạnh", "Điện lạnh", 3, 2, new BigDecimal("40000000"), 1,
              new BigDecimal("20000000")),
          new DongThongKe("DSP-2", "Máy giặt", "Giặt", 1, 1, new BigDecimal("10000000"), 0,
              new BigDecimal("10000000")));
    }

    @Override
    public List<GiaoDichDoiChieu> doiChieu(LocalDate tuNgay, LocalDate denNgay, String maDongSP) {
      soLanGoiDao++;
      return List.of();
    }
  });

  @AfterEach
  void tearDown() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Tổng cộng bằng tổng các dòng")
  void tongCong() {
    dangNhap(LoaiQuyen.NHAN_VIEN_QUAN_LY);
    var baoCao = controller.xemBaoCao(TU, DEN, null).duLieu();
    assertEquals(3, baoCao.tongSoLuongBan());
    assertEquals(1, baoCao.tongSoLuongTra());
    assertEquals(0, new BigDecimal("30000000").compareTo(baoCao.tongDoanhSo()));
  }

  @Test
  @DisplayName("Kỳ đảo ngược (5.a) và đối chiếu thiếu dòng sản phẩm bị từ chối, không truy vấn")
  void kyKhongHopLe() {
    dangNhap(LoaiQuyen.NHAN_VIEN_QUAN_LY);
    assertFalse(controller.xemBaoCao(DEN, TU, null).thanhCong());
    assertFalse(controller.doiChieu(TU, DEN, " ").thanhCong());
    assertTrue(controller.doiChieu(TU, DEN, "DSP-1").thanhCong());
    assertEquals(1, soLanGoiDao);
  }

  @Test
  @DisplayName("Chỉ Quản lý được xem và đối chiếu")
  void chiQuanLy() {
    dangNhap(LoaiQuyen.NHAN_VIEN_BAN_HANG);
    assertThrows(SecurityException.class, () -> controller.xemBaoCao(TU, DEN, null));
    assertThrows(SecurityException.class, () -> controller.doiChieu(TU, DEN, "DSP-1"));
  }

  private static void dangNhap(LoaiQuyen quyen) {
    CurrentSession.set(new NhanVien("NV", "Nhân viên", "0900000000", null, LocalDate.of(2026, 1, 1),
        "nv", "hash", quyen, true));
  }
}
