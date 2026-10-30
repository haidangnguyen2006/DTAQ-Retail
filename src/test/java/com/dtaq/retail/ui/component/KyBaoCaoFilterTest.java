package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.util.DateRange;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

/**
 * Kiểm thử {@link KyBaoCaoFilter} ở mức dựng component headless (không mở cửa sổ): chỉ kiểm tra
 * logic chọn kỳ và trạng thái bật/tắt, không kiểm tra chi tiết đồ họa.
 */
@DisplayName("KyBaoCaoFilter - bộ lọc kỳ báo cáo")
class KyBaoCaoFilterTest {

  private static final String LOI_KY_DAO_NGUOC =
      "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu";

  @Test
  @DisplayName("Khởi tạo mặc định là 7 ngày gần nhất và không ném lỗi")
  void taoMacDinhKhongNem() {
    KyBaoCaoFilter boLoc =
        assertDoesNotThrow((ThrowingSupplier<KyBaoCaoFilter>) KyBaoCaoFilter::new);

    DateRange ky = boLoc.getDateRange().orElseThrow();
    LocalDate homNay = LocalDate.now();
    assertFalse(ky.denNgay().isBefore(homNay.minusDays(1)), "ngày kết thúc không được lùi quá 1 ngày");
    assertFalse(ky.denNgay().isAfter(homNay), "ngày kết thúc không được vượt hôm nay");
    assertEquals(ky.denNgay().minusDays(6), ky.tuNgay(), "kỳ mặc định gồm 7 ngày");
  }

  @Test
  @DisplayName("Khởi tạo với kỳ cho sẵn giữ đúng kỳ")
  void khoiTaoBangKyChoSan() {
    KyBaoCaoFilter boLoc =
        new KyBaoCaoFilter(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    assertEquals(
        Optional.of(new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))),
        boLoc.getDateRange());
  }

  @Test
  @DisplayName("setDateRange cập nhật kỳ đang hiển thị")
  void setDateRangeCapNhat() {
    KyBaoCaoFilter boLoc = new KyBaoCaoFilter();

    boLoc.setDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

    assertEquals(
        Optional.of(new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
        boLoc.getDateRange());
  }

  @Test
  @DisplayName("apply trao kỳ hợp lệ cho listener và xoá lỗi cũ")
  void applyTraoKyKhiHopLe() {
    KyBaoCaoFilter boLoc = new KyBaoCaoFilter();
    AtomicReference<DateRange> daNhan = new AtomicReference<>();
    boLoc.setOnApply(daNhan::set);
    List<JTextField> oNgay = cacOText(boLoc);
    assertEquals(2, oNgay.size(), "bộ lọc phải có đúng hai ô ngày (Từ ngày trước Đến ngày)");

    boLoc.setDateRange(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 1));
    assertFalse(boLoc.apply(), "kỳ đảo ngược phải bị từ chối");
    assertTrue(coNhanHienThi(boLoc, LOI_KY_DAO_NGUOC), "phải hiển thị lỗi kỳ đảo ngược");

    oNgay.get(0).setText("01/01/2026");
    oNgay.get(1).setText("31/01/2026");
    assertTrue(boLoc.apply(), "kỳ hợp lệ phải được chấp nhận");

    assertEquals(new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), daNhan.get());
    assertFalse(coNhanHienThi(boLoc, LOI_KY_DAO_NGUOC), "lỗi phải được xoá sau khi áp dụng thành công");
  }

  @Test
  @DisplayName("apply từ chối kỳ đảo ngược và không gọi listener")
  void applyTuChoiKyDaoNguoc() {
    KyBaoCaoFilter boLoc = new KyBaoCaoFilter();
    AtomicInteger soLanGoi = new AtomicInteger();
    boLoc.setOnApply(ky -> soLanGoi.incrementAndGet());

    boLoc.setDateRange(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 1));

    assertFalse(boLoc.apply());
    assertEquals(0, soLanGoi.get(), "listener không được gọi khi kỳ không hợp lệ");
  }

  @Test
  @DisplayName("Kỳ để trống trả về rỗng, apply thất bại và báo lỗi ở ô Từ ngày")
  void getDateRangeRongKhiNhapTrong() {
    KyBaoCaoFilter boLoc = new KyBaoCaoFilter();
    AtomicInteger soLanGoi = new AtomicInteger();
    boLoc.setOnApply(ky -> soLanGoi.incrementAndGet());

    boLoc.setDateRange(null, null);

    assertTrue(boLoc.getDateRange().isEmpty(), "kỳ trống phải trả về Optional rỗng");
    assertFalse(boLoc.apply(), "kỳ trống không được áp dụng");
    assertEquals(0, soLanGoi.get(), "listener không được gọi khi kỳ trống");
    assertTrue(
        coNhanHienThi(boLoc, "Từ ngày không được để trống"),
        "phải báo lỗi thiếu ngày bắt đầu");
  }

  @Test
  @DisplayName("setEnabled lan xuống hai ô nhập và nút Áp dụng")
  void tatThiVoHieuHoa() {
    KyBaoCaoFilter boLoc = new KyBaoCaoFilter();
    List<JTextField> oNgay = cacOText(boLoc);
    JButton nutApDung = timNut(boLoc, "Áp dụng");
    assertEquals(2, oNgay.size(), "bộ lọc phải có đúng hai ô ngày (Từ ngày trước Đến ngày)");
    assertNotNull(nutApDung, "phải có nút Áp dụng");

    boLoc.setEnabled(false);
    assertFalse(boLoc.isEnabled());
    assertTrue(oNgay.stream().noneMatch(Component::isEnabled), "hai ô nhập phải bị vô hiệu hoá");
    assertFalse(nutApDung.isEnabled(), "nút Áp dụng phải bị vô hiệu hoá");

    boLoc.setEnabled(true);
    assertTrue(boLoc.isEnabled());
    assertTrue(oNgay.stream().allMatch(Component::isEnabled), "hai ô nhập phải được bật lại");
    assertTrue(nutApDung.isEnabled(), "nút Áp dụng phải được bật lại");
  }

  @Test
  @DisplayName("apply báo lỗi đúng ô Từ ngày khi sai định dạng và xoá lỗi cũ của ô kia")
  void applyBaoLoiDungOoTuNgayKhiSaiDinhDang() {
    KyBaoCaoFilter boLoc = new KyBaoCaoFilter();
    List<JTextField> oNgay = cacOText(boLoc);
    assertEquals(2, oNgay.size(), "bộ lọc phải có đúng hai ô ngày (Từ ngày trước Đến ngày)");
    FormField truongTuNgay = truongCua(oNgay.get(0));
    FormField truongDenNgay = truongCua(oNgay.get(1));
    assertNotNull(truongTuNgay, "ô nhập phải nằm trong một FormField");
    assertNotNull(truongDenNgay, "ô nhập phải nằm trong một FormField");

    boLoc.setDateRange(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 1));
    assertFalse(boLoc.apply(), "kỳ đảo ngược phải bị từ chối");
    assertTrue(
        coNhanHienThi(truongDenNgay, LOI_KY_DAO_NGUOC), "lỗi thứ tự thuộc ô Đến ngày");

    String giaTriSai = "32/13/2026";
    oNgay.get(0).setText(giaTriSai);
    assertFalse(boLoc.apply(), "ngày không hợp lệ phải bị từ chối");

    String loiDinhDang = "Ngày không hợp lệ (định dạng dd/MM/yyyy): " + giaTriSai;
    assertTrue(
        coNhanHienThi(truongTuNgay, loiDinhDang),
        "lỗi định dạng phải hiển thị ở ô Từ ngày");
    assertFalse(
        coNhanHienThi(truongDenNgay, loiDinhDang),
        "ô Đến ngày không được báo lỗi định dạng của ô Từ ngày");
    assertFalse(
        coNhanHienThi(truongDenNgay, LOI_KY_DAO_NGUOC),
        "lỗi cũ phải được xoá trước khi kiểm tra lại");
  }

  /** Tìm trong cây component một nhãn đang hiển thị có đúng nội dung cần kiểm tra. */
  private static boolean coNhanHienThi(Component goc, String noiDung) {
    if (goc instanceof JLabel nhan && nhan.isVisible() && noiDung.equals(nhan.getText())) {
      return true;
    }
    if (goc instanceof Container chua) {
      for (Component con : chua.getComponents()) {
        if (coNhanHienThi(con, noiDung)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Liệt kê các ô văn bản theo thứ tự thêm component: Từ ngày trước, Đến ngày sau. */
  private static List<JTextField> cacOText(Component goc) {
    List<JTextField> ketQua = new ArrayList<>();
    thuThapOText(goc, ketQua);
    return ketQua;
  }

  private static void thuThapOText(Component goc, List<JTextField> ketQua) {
    if (goc instanceof JTextField o) {
      ketQua.add(o);
    }
    if (goc instanceof Container chua) {
      for (Component con : chua.getComponents()) {
        thuThapOText(con, ketQua);
      }
    }
  }

  /** Tìm nút có đúng nhãn trong cây component. */
  private static JButton timNut(Component goc, String nhan) {
    if (goc instanceof JButton nut && nhan.equals(nut.getText())) {
      return nut;
    }
    if (goc instanceof Container chua) {
      for (Component con : chua.getComponents()) {
        JButton timThay = timNut(con, nhan);
        if (timThay != null) {
          return timThay;
        }
      }
    }
    return null;
  }

  /** Đi ngược lên cây component để tìm FormField chứa ô nhập. */
  private static FormField truongCua(Component con) {
    for (Component hienTai = con.getParent(); hienTai != null; hienTai = hienTai.getParent()) {
      if (hienTai instanceof FormField truong) {
        return truong;
      }
    }
    return null;
  }
}
