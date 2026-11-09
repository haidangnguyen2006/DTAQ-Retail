package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InputFiltersTest {

  @Test
  @DisplayName("Ô số nguyên chặn chữ, dấu thập phân và quá giới hạn độ dài")
  void locSoNguyen() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JTextField o = new JTextField();
      InputFilters.chiChoSo(o, 3);
      for (String noiDung : new String[] {"a", "1.5", "1234"}) {
        o.setText(noiDung);
        assertEquals("", o.getText());
      }

      o.setText(" 168 ");
      assertEquals("168", o.getText());
      o.setText("1680");
      assertEquals("168", o.getText());
      o.setText("");
      assertEquals("", o.getText());
    });
  }

  @Test
  @DisplayName("Ô giờ chèn dấu hai chấm khi gõ hoặc dán bốn chữ số")
  void locGioPhut() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JTextField o = new JTextField();
      InputFilters.gioPhut(o);
      o.setText("ab");
      assertEquals("", o.getText());

      Document document = o.getDocument();
      try {
        for (char kyTu : "0830".toCharArray()) {
          document.insertString(document.getLength(), String.valueOf(kyTu), null);
        }
      } catch (BadLocationException e) {
        throw new AssertionError(e);
      }
      assertEquals("08:30", o.getText());
      o.setText(" 2315 ");
      assertEquals("23:15", o.getText());
      o.setText("08:300");
      assertEquals("23:15", o.getText());
    });
  }
}
