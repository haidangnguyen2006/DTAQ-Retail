package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import org.junit.jupiter.api.Test;

class ResponsiveTest {
  @Test
  void sizeBoundaries() {
    assertEquals(Responsive.Size.S, Responsive.Size.of(959));
    assertEquals(Responsive.Size.M, Responsive.Size.of(960));
    assertEquals(Responsive.Size.M, Responsive.Size.of(1279));
    assertEquals(Responsive.Size.L, Responsive.Size.of(1280));
    assertEquals(Responsive.Size.L, Responsive.Size.of(1599));
    assertEquals(Responsive.Size.XL, Responsive.Size.of(1600));
  }

  @Test
  void gridReflowsWithoutReplacingControls() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JPanel grid = new JPanel(new MigLayout());
      JButton first = new JButton("A");
      grid.add(first);
      grid.add(new JButton("B"));
      Responsive.grid(grid, 240, 2);
      resize(grid, 400, 200);
      assertTrue(grid.getComponent(1).getY() > first.getY());
      resize(grid, 800, 200);
      assertEquals(first.getY(), grid.getComponent(1).getY());
      assertSame(first, grid.getComponent(0));
    });
  }

  @Test
  void tableScrollsRatherThanCrushingColumns() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      DataTable data = new DataTable(DataTable.readOnlyModel(new String[] {"Mã", "Tên"}));
      data.setColumnWidths(200, 300);
      var viewport = data.getScrollPane().getViewport();
      viewport.setSize(300, 150);
      viewport.dispatchEvent(new ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED));
      assertEquals(JTable.AUTO_RESIZE_OFF, data.getTable().getAutoResizeMode());
      viewport.setSize(800, 150);
      viewport.dispatchEvent(new ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED));
      assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, data.getTable().getAutoResizeMode());
      assertTrue(data.getTable().isFocusable());
    });
  }

  @Test
  void inputsKeepReadableHeightAndAccessibleLabels() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FormField field = FormField.text("Số serial", "Nhập serial", "hash");
      assertEquals(DtaqSpacing.CONTROL_H, field.getInputComponent().getMinimumSize().height);
      assertEquals("Số serial", field.getInputComponent().getAccessibleContext().getAccessibleName());
    });
  }

  @Test
  void referencedIconsArePackaged() {
    for (String name : new String[] {"calendar", "hash", "truck", "panel-left"}) {
      assertNotNull(DtaqIcons.class.getResource("icons/" + name + ".svg"), name);
    }
    assertNotNull(com.dtaq.retail.ui.theme.DtaqTheme.logo(34), "dtaq-logo.png");
    assertNotNull(com.dtaq.retail.ui.theme.DtaqTheme.logoMark(34), "dtaq-mark.png");
    assertEquals(6, com.dtaq.retail.ui.theme.DtaqTheme.windowIcons().size());
  }

  private static void resize(JPanel panel, int width, int height) {
    panel.setSize(new Dimension(width, height));
    panel.dispatchEvent(new ComponentEvent(panel, ComponentEvent.COMPONENT_RESIZED));
    panel.doLayout();
  }
}
