package com.dtaq.retail.ui.page.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.dao.ThongKeSanPhamDao.DongThongKe;
import com.dtaq.retail.ui.component.PagedLoader;
import com.dtaq.retail.ui.component.DataTable;
import java.util.stream.IntStream;
import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SalesByProductPageTest {

  @Test
  void chonDongTrangHaiKhongResetBang() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        var page = new SalesByProductPage(null);
        var rowsField = SalesByProductPage.class.getDeclaredField("rows");
        rowsField.setAccessible(true);
        rowsField.set(page, IntStream.range(0, 25).mapToObj(i ->
            new DongThongKe("DSP-" + i, "Dòng " + i, "DM", 2, 1, BigDecimal.TEN, 0, BigDecimal.TEN)).toList());
        var render = SalesByProductPage.class.getDeclaredMethod("renderRows");
        render.setAccessible(true);
        render.invoke(page);
        var trangField = SalesByProductPage.class.getDeclaredField("trang");
        trangField.setAccessible(true);
        ((PagedLoader<?>) trangField.get(page)).chuyenTrang(2);
        var tableField = SalesByProductPage.class.getDeclaredField("dataTable");
        tableField.setAccessible(true);
        var table = ((DataTable) tableField.get(page)).getTable();
        assertEquals(5, table.getRowCount());
        Object maDong = table.getValueAt(1, 0);
        table.setRowSelectionInterval(1, 1);
        assertEquals(5, table.getRowCount());
        assertEquals(maDong, table.getValueAt(1, 0));
        var selectedField = SalesByProductPage.class.getDeclaredField("selectedProductCode");
        selectedField.setAccessible(true);
        assertEquals(maDong, selectedField.get(page));
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(e);
      }
    });
  }

  @Test
  void recommendationBadge_TheoDoiVaSoLuongBan() {
    DateRange range = new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));

    assertEquals(
        "🟢 Cần nhập thêm tháng này",
        SalesByProductPage.recommendationBadge(12, 120, 5, BigDecimal.valueOf(10_000_000), range));

    assertEquals(
        "🔵 Đủ hàng / Bỏ qua",
        SalesByProductPage.recommendationBadge(90, 90, 5, BigDecimal.valueOf(20_000_000), range));

    assertEquals(
        "🟡 Tồn kho cao / Giãn đơn",
        SalesByProductPage.recommendationBadge(150, 90, 5, BigDecimal.valueOf(25_000_000), range));

    assertEquals(
        "🔴 Xả kho / Ngừng nhập",
        SalesByProductPage.recommendationBadge(200, 0, 0, BigDecimal.ZERO, range));
  }
}
