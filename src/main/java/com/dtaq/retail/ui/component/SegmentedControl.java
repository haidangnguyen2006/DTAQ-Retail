package com.dtaq.retail.ui.component;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Điều khiển nhóm chọn phân đoạn (Segmented Control) như "Ngày | Tuần | Tháng".
 */
public class SegmentedControl extends JPanel {

  private final List<DtaqButton> buttons = new ArrayList<>();
  private int selectedIndex = 0;
  private Consumer<Integer> onSelectionChange;

  public SegmentedControl(String[] options, int defaultIndex) {
    super(new MigLayout("insets 0", "[]8[]", "[]"));
    setOpaque(false);
    this.selectedIndex = defaultIndex;

    for (int i = 0; i < options.length; i++) {
      final int idx = i;
      DtaqButton.Variant variant = (i == defaultIndex)
          ? DtaqButton.Variant.SEGMENTED_ACTIVE
          : DtaqButton.Variant.SEGMENTED_INACTIVE;

      DtaqButton btn = new DtaqButton(options[i], variant);
      btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 31));
      btn.addActionListener(e -> select(idx));
      buttons.add(btn);
      add(btn);
    }
  }

  public void select(int index) {
    if (index < 0 || index >= buttons.size()) {
      return;
    }
    this.selectedIndex = index;
    for (int i = 0; i < buttons.size(); i++) {
      DtaqButton b = buttons.get(i);
      DtaqButton.Variant v = (i == selectedIndex)
          ? DtaqButton.Variant.SEGMENTED_ACTIVE
          : DtaqButton.Variant.SEGMENTED_INACTIVE;
      // Re-initialize button style for the new variant
      reapplyVariant(b, v);
    }
    if (onSelectionChange != null) {
      onSelectionChange.accept(selectedIndex);
    }
  }

  private void reapplyVariant(DtaqButton b, DtaqButton.Variant v) {
    // Replace with a newly configured button or trigger style refresh
    b.removeAll();
    if (v == DtaqButton.Variant.SEGMENTED_ACTIVE) {
      b.setBackground(com.dtaq.retail.ui.theme.DtaqColors.PRIMARY_SOFT);
      b.setForeground(com.dtaq.retail.ui.theme.DtaqColors.PRIMARY_STRONG);
      b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
          "background: #E8F5F2; foreground: #075E57; borderWidth: 0; arc: 6");
    } else {
      b.setBackground(com.dtaq.retail.ui.theme.DtaqColors.SURFACE);
      b.setForeground(com.dtaq.retail.ui.theme.DtaqColors.TEXT_MUTED);
      b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
          "background: #FFFFFF; foreground: #667580; borderColor: #E2E8EC; borderWidth: 1; arc: 6");
    }
    b.repaint();
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public void setOnSelectionChange(Consumer<Integer> listener) {
    this.onSelectionChange = listener;
  }
}
