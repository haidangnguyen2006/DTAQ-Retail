package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatePickerTest {

  @Test
  @DisplayName("Chặn chữ, dấu gạch đầu, ngày ISO sai và nội dung vượt độ dài")
  void tuChoiNoiDungSaiMatNa() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DatePicker picker = new DatePicker("Ngày");
      for (String noiDung : new String[] {"abc", "12a", "/", "2026-02-30", "１２"}) {
        picker.textField().setText(noiDung);
        assertTrue(picker.isBlank());
      }

      picker.textField().setText("09/11/2026");
      picker.textField().selectAll();
      picker.textField().replaceSelection("12a");
      goTiep(picker, "1");
      assertEquals("09/11/2026", picker.getText());
    });
  }

  @Test
  @DisplayName("Gõ liên tục hoặc dán ngày compact và ISO đều thành dd/MM/yyyy")
  void nhapVaDanNgay() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DatePicker picker = new DatePicker("Ngày");
      goTiep(picker, "09112026");
      assertEquals("09/11/2026", picker.getText());
      assertEquals(LocalDate.of(2026, 11, 9), picker.getDate());

      picker.textField().setText(" 05012026 ");
      assertEquals("05/01/2026", picker.getText());
      picker.textField().setText("2024-2-29");
      assertEquals("29/02/2024", picker.getText());
      picker.textField().setText("1/2/2026");
      assertEquals(LocalDate.of(2026, 2, 1), picker.getDate());
      picker.setDate(LocalDate.of(2026, 1, 5));
      assertEquals("05/01/2026", picker.getText());
    });
  }

  @Test
  @DisplayName("Báo lỗi khi rời ô có ngày sai và xóa lỗi khi sửa")
  void baoLoiKhiRoiO() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DatePicker picker = new DatePicker("Ngày");
      picker.textField().setText("31/02/2026");
      assertNull(picker.getDate());
      assertTrue(picker.hasInvalidText());
      matFocus(picker, true);
      assertNull(picker.textField().getClientProperty(FlatClientProperties.OUTLINE));
      matFocus(picker, false);
      assertEquals("error", picker.textField().getClientProperty(FlatClientProperties.OUTLINE));
      assertFalse(picker.kiemTraDinhDang());

      picker.textField().select(2, 3);
      picker.textField().replaceSelection("");
      assertEquals("3102/2026", picker.getText());
      assertNull(picker.textField().getClientProperty(FlatClientProperties.OUTLINE));
      picker.setDate(null);
      assertTrue(picker.isBlank());
      assertFalse(picker.hasInvalidText());
      assertTrue(picker.kiemTraDinhDang());

      picker.setDate(LocalDate.of(2026, 1, 5));
      picker.setError("Lỗi nghiệp vụ");
      matFocus(picker, false);
      assertEquals("error", picker.textField().getClientProperty(FlatClientProperties.OUTLINE));
    });
  }

  @Test
  @DisplayName("Vô hiệu hóa cả ô nhập và nút lịch")
  void batTatOvaLich() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DatePicker picker = new DatePicker("Ngày");
      JButton lich = (JButton) picker.textField()
          .getClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT);
      picker.setEnabled(false);
      assertFalse(picker.textField().isEnabled());
      assertFalse(lich.isEnabled());
      picker.setEnabled(true);
      assertTrue(picker.textField().isEnabled());
      assertTrue(lich.isEnabled());
    });
  }

  private static void matFocus(DatePicker picker, boolean tamThoi) {
    FocusEvent event = new FocusEvent(picker.textField(), FocusEvent.FOCUS_LOST, tamThoi);
    for (var listener : picker.textField().getFocusListeners()) {
      listener.focusLost(event);
    }
  }

  private static void goTiep(DatePicker picker, String chuoi) {
    Document document = picker.textField().getDocument();
    try {
      for (char kyTu : chuoi.toCharArray()) {
        document.insertString(document.getLength(), String.valueOf(kyTu), null);
      }
    } catch (BadLocationException e) {
      throw new AssertionError(e);
    }
  }
}
