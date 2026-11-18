package com.dtaq.retail.ui.page.promotion;

import static org.junit.jupiter.api.Assertions.*;

import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.ui.component.FormField;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class AddPromotionDialogTest {
  private static final DanhMucSanPham DM1 = new DanhMucSanPham("DM1", "Điện lạnh", null);
  private static final DanhMucSanPham DM2 = new DanhMucSanPham("DM2", "Gia dụng", null);
  private static final DongSanPham DONG = new DongSanPham("DSP1", "Tủ lạnh", DM1, "máy", 3,
      BigDecimal.TEN, BigDecimal.TEN, true, LocalDate.now(), LocalDate.now());

  @Test
  void xemTruocGuiDayDuHaiDanhMuc() throws Exception {
    dialog(d -> {
      var received = new AtomicReference<AddPromotionDialog.PromotionFormData>();
      d.setOnSubmit((form, save) -> { assertFalse(save); received.set(form); });
      ((JList<?>) field(d, "danhMucList")).setSelectedIndices(new int[] {0, 1});
      ((AbstractButton) field(d, "previewButton")).doClick();
      assertNotNull(received.get());
      assertNull(received.get().maDongSP());
      assertEquals(List.of("DM1", "DM2"), received.get().maDanhMuc());
    });
  }

  @Test
  void chonDongXoaKhoaDanhMucVaBoChonChoPhepChonLai() throws Exception {
    dialog(d -> {
      var categories = (JList<?>) field(d, "danhMucList");
      var lines = (JComboBox<?>) field(d, "dongSanPhamCombo");
      categories.setSelectedIndices(new int[] {0, 1});
      lines.setSelectedItem(DONG);
      assertTrue(categories.isSelectionEmpty());
      assertFalse(categories.isEnabled());
      var received = new AtomicReference<AddPromotionDialog.PromotionFormData>();
      d.setOnSubmit((form, save) -> received.set(form));
      ((AbstractButton) field(d, "previewButton")).doClick();
      assertEquals("DSP1", received.get().maDongSP());
      assertEquals(List.of(), received.get().maDanhMuc());
      d.setBusy(true);
      d.setBusy(false);
      assertFalse(categories.isEnabled());
      lines.setSelectedIndex(0);
      assertTrue(categories.isEnabled());
      assertTrue(categories.isSelectionEmpty());
    });
  }

  @Test
  void busyChanSubmitVaKhoiPhucChonNhieu() throws Exception {
    dialog(d -> {
      var received = new AtomicReference<AddPromotionDialog.PromotionFormData>();
      d.setOnSubmit((form, save) -> received.set(form));
      var categories = (JList<?>) field(d, "danhMucList");
      categories.setSelectedIndices(new int[] {0, 1});
      d.setBusy(true);
      assertFalse(categories.isEnabled());
      assertFalse(((JComboBox<?>) field(d, "dongSanPhamCombo")).isEnabled());
      ((AbstractButton) field(d, "previewButton")).doClick();
      ((AbstractButton) field(d, "saveButton")).doClick();
      assertNull(received.get());
      d.setBusy(false);
      assertTrue(categories.isEnabled());
      ((AbstractButton) field(d, "previewButton")).doClick();
      assertEquals(List.of("DM1", "DM2"), received.get().maDanhMuc());
    });
  }

  @Test
  void luuGuiFormHopLeVaTuChoiTuoiTonAm() throws Exception {
    dialog(d -> {
      var received = new AtomicReference<AddPromotionDialog.PromotionFormData>();
      d.setOnSubmit((form, save) -> { assertTrue(save); received.set(form); });
      ((JComboBox<?>) field(d, "dongSanPhamCombo")).setSelectedItem(DONG);
      ((FormField) field(d, "tenField")).setText("  Ưu đãi  ");
      ((FormField) field(d, "tyLeField")).setText("12,5");
      ((FormField) field(d, "ngayBatDauField")).setText("2026-11-18");
      ((FormField) field(d, "ngayKetThucField")).setText("2026-11-30");
      ((FormField) field(d, "ngayTonField")).setText("-1");
      ((AbstractButton) field(d, "saveButton")).doClick();
      assertNull(received.get());
      ((FormField) field(d, "ngayTonField")).setText("0");
      ((AbstractButton) field(d, "saveButton")).doClick();
      assertNotNull(received.get());
      assertEquals("Ưu đãi", received.get().ten());
      assertEquals(new BigDecimal("12.5"), received.get().tyLeGiam());
      assertEquals(0, received.get().soNgayTonToiThieu());
      assertEquals("DSP1", received.get().maDongSP());
      assertEquals(List.of(), received.get().maDanhMuc());
    });
  }

  private static void dialog(Consumer<AddPromotionDialog> action) throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Dialog cần môi trường desktop");
    var task = new FutureTask<Void>(() -> {
      var previous = UIManager.getLookAndFeel();
      AddPromotionDialog dialog = null;
      try {
        UIManager.setLookAndFeel(new FlatLightLaf());
        dialog = new AddPromotionDialog(null, List.of(DONG), List.of(DM1, DM2));
        action.accept(dialog);
      } finally {
        if (dialog != null) dialog.dispose();
        UIManager.setLookAndFeel(previous);
      }
      return null;
    });
    SwingUtilities.invokeAndWait(task);
    task.get(5, TimeUnit.SECONDS);
  }

  private static Object field(Object target, String name) {
    try {
      var field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
