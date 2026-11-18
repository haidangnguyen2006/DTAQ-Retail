package com.dtaq.retail.ui.page.report;

import static org.junit.jupiter.api.Assertions.*;

import com.dtaq.retail.controller.ThongKeSanPhamController;
import com.dtaq.retail.dao.ThongKeSanPhamDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.util.DateRange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SalesByProductLoadingTest {
  private static final DateRange NEW_RANGE = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));

  @AfterEach
  void clearSession() {
    CurrentSession.clear();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void ketQuaCuKhongGhiDeKyMoi(boolean oldFails) throws Exception {
    var old = new Reply();
    var latest = new Reply();
    var page = page(old, latest);
    assertTrue(old.entered.await(5, TimeUnit.SECONDS));
    try {
      load(page);
      assertTrue(latest.entered.await(5, TimeUnit.SECONDS));
      latest.value.complete(List.of(row("NEW", 1, 0, 0)));
      awaitState(page, StatefulView.State.CONTENT);
      assertEquals(NEW_RANGE, edt(() -> field(page, "currentRange")));
      if (oldFails) old.value.completeExceptionally(new DataAccessException("Lỗi kỳ cũ giả lập"));
      else old.value.complete(List.of(row("OLD", 0, 1, 5)));
      assertTrue(old.returned.await(5, TimeUnit.SECONDS));
      // SwingWorker chuyển done() lên EDT theo đợt. Quan sát qua nhiều đợt dispatch.
      long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
      do {
        assertEquals("NEW", edt(() -> field(page, "selectedProductCode")));
        assertEquals(NEW_RANGE, edt(() -> field(page, "currentRange")));
        assertEquals(StatefulView.State.CONTENT, edt(() -> ((StatefulView) field(page, "tableState")).getState()));
        Thread.sleep(10);
      } while (System.nanoTime() < deadline);
    } finally {
      old.value.complete(List.of());
      latest.value.complete(List.of());
    }
  }

  @Test
  void loadingVaLoiXoaNhanXetCuVaKhoaExport() throws Exception {
    var first = new Reply();
    first.value.complete(List.of(row("A", 1, 0, 2)));
    var failed = new Reply();
    var recovery = new Reply();
    var page = page(first, failed, recovery);
    awaitState(page, StatefulView.State.CONTENT);
    try {
      load(page);
      assertEquals(StatefulView.State.LOADING, edt(() -> ((StatefulView) field(page, "tableState")).getState()));
      assertFalse(edt(() -> ((AbstractButton) field(page, "exportBtn")).isEnabled()));
      assertTrue(edt(() -> ((List<?>) field(page, "rows")).isEmpty()));
      assertEquals(List.of("Đang tổng hợp nhận xét…"), insights(page));
      failed.value.completeExceptionally(new DataAccessException("Lỗi DB giả lập"));
      awaitState(page, StatefulView.State.ERROR);
      assertFalse(edt(() -> ((AbstractButton) field(page, "exportBtn")).isEnabled()));
      assertEquals(List.of("Không thể tổng hợp nhận xét khi tải báo cáo thất bại."), insights(page));
      load(page);
      recovery.value.complete(List.of(row("RECOVERED", 1, 1, 0)));
      awaitState(page, StatefulView.State.CONTENT);
      assertTrue(edt(() -> ((AbstractButton) field(page, "exportBtn")).isEnabled()));
      assertEquals("RECOVERED", edt(() -> field(page, "selectedProductCode")));
      assertFalse(insights(page).stream().anyMatch(text -> text.contains("thất bại")));
    } finally {
      failed.value.complete(List.of());
      recovery.value.complete(List.of());
    }
  }

  @Test
  void khongPhatSinhVanHienThiNhomTonVaKhoaExport() throws Exception {
    var reply = new Reply();
    reply.value.complete(List.of(row("A", 0, 0, 3)));
    var page = page(reply);
    awaitState(page, StatefulView.State.EMPTY);
    // EMPTY ban đầu cũng tồn tại trước worker, đợi câu nhận xét từ kết quả mới.
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (insights(page).contains("Đang tổng hợp nhận xét…") && System.nanoTime() < deadline) Thread.sleep(10);
    assertEquals(StatefulView.State.EMPTY, edt(() -> ((StatefulView) field(page, "tableState")).getState()));
    assertFalse(edt(() -> ((AbstractButton) field(page, "exportBtn")).isEnabled()));
    assertTrue(insights(page).stream().anyMatch(text -> text.contains("1 dòng hiện còn tồn kho")));
  }

  private static SalesByProductPage page(Reply... replies) throws Exception {
    CurrentSession.set(new NhanVien("NV", "Quản lý", "0900000000", null, LocalDate.now(),
        "ql", "hash", LoaiQuyen.NHAN_VIEN_QUAN_LY, true));
    var queue = new LinkedBlockingQueue<Reply>(List.of(replies));
    var dao = new ThongKeSanPhamDao() {
      public List<DongThongKe> thongKe(LocalDate tu, LocalDate den, String ma) {
        Reply reply = queue.remove();
        reply.entered.countDown();
        try {
          return reply.value.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
          throw new DataAccessException("Lỗi DB giả lập", e);
        } finally {
          reply.returned.countDown();
        }
      }
      public List<GiaoDichDoiChieu> doiChieu(LocalDate tu, LocalDate den, String ma) { return List.of(); }
    };
    return edt(() -> new SalesByProductPage(new ThongKeSanPhamController(dao)));
  }

  private static void load(SalesByProductPage page) throws Exception {
    edt(() -> {
      var method = SalesByProductPage.class.getDeclaredMethod("loadReport", DateRange.class, String.class);
      method.setAccessible(true);
      method.invoke(page, NEW_RANGE, null);
      return null;
    });
  }

  private static void awaitState(SalesByProductPage page, StatefulView.State expected) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (edt(() -> ((StatefulView) field(page, "tableState")).getState()) != expected && System.nanoTime() < deadline) Thread.sleep(10);
    assertEquals(expected, edt(() -> ((StatefulView) field(page, "tableState")).getState()));
  }

  private static List<String> insights(SalesByProductPage page) throws Exception {
    return edt(() -> {
      var result = new java.util.ArrayList<String>();
      for (var callout : ((JPanel) field(page, "insightList")).getComponents()) {
        for (var child : ((JPanel) callout).getComponents()) {
          if (child instanceof JLabel label && label.getText() != null) {
            result.add(label.getText().replace("<html>", "").replace("</html>", ""));
          }
        }
      }
      return result;
    });
  }

  private static Object field(Object target, String name) throws Exception {
    var field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static <T> T edt(Callable<T> action) throws Exception {
    var task = new FutureTask<T>(action);
    SwingUtilities.invokeAndWait(task);
    return task.get(5, TimeUnit.SECONDS);
  }

  private static ThongKeSanPhamDao.DongThongKe row(String ma, long ban, long tra, int ton) {
    return new ThongKeSanPhamDao.DongThongKe(ma, ma, "DM", ton, ban, BigDecimal.TEN, tra, BigDecimal.ONE);
  }

  private static class Reply {
    final CompletableFuture<List<ThongKeSanPhamDao.DongThongKe>> value = new CompletableFuture<>();
    final CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch returned = new CountDownLatch(1);
  }
}
