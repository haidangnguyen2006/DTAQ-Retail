package com.dtaq.retail.ui.shell;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.awt.event.ComponentEvent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class NavItemWrapTest {
  @Test
  void longLabelWrapsAndCompactKeepsAccessibleName() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      String text = "Thống kê doanh số theo nhân viên";
      NavItem item = new NavItem("users", text);
      item.setSize(210, 38);
      item.dispatchEvent(new ComponentEvent(item, ComponentEvent.COMPONENT_RESIZED));
      assertTrue(item.getText().startsWith("<html>"));
      int narrowHeight = item.getPreferredSize().height;
      item.setSize(400, narrowHeight);
      item.dispatchEvent(new ComponentEvent(item, ComponentEvent.COMPONENT_RESIZED));
      assertTrue(narrowHeight > item.getPreferredSize().height);
      item.setCompact(true);
      assertEquals("", item.getText());
      assertEquals(text, item.getAccessibleContext().getAccessibleName());
      item.setCompact(false);
      assertTrue(item.getText().contains(text));
    });
  }
}
