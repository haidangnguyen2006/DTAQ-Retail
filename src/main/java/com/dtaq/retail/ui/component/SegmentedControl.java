package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.ui.theme.DtaqStyles;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;

/**
 * Điều khiển nhóm chọn phân đoạn (Segmented Control) như "Ngày | Tuần | Tháng".
 */
public class SegmentedControl extends JPanel {

  private final List<JToggleButton> buttons = new ArrayList<>();
  private int selectedIndex;
  private Consumer<Integer> onSelectionChange;

  public SegmentedControl(String[] options, int defaultIndex) {
    super(new MigLayout("insets 0, gapx 6", "[]", "[]"));
    setOpaque(false);
    this.selectedIndex = defaultIndex;

    ButtonGroup group = new ButtonGroup();
    for (int i = 0; i < options.length; i++) {
      final int idx = i;
      JToggleButton btn = new JToggleButton(options[i]) {
        @Override
        public Dimension getPreferredSize() {
          Dimension d = super.getPreferredSize();
          d.height = Math.max(d.height, DtaqSpacing.BUTTON_H_SM);
          return d;
        }
      };
      btn.setFont(DtaqFonts.SMALL_BUTTON);
      btn.setFocusPainted(false);
      btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      btn.putClientProperty(FlatClientProperties.STYLE, DtaqStyles.TOGGLE_SEGMENT);
      btn.setSelected(i == defaultIndex);
      btn.addActionListener(e -> select(idx));
      group.add(btn);
      buttons.add(btn);
      add(btn);
    }
  }

  public void select(int index) {
    if (index < 0 || index >= buttons.size()) {
      return;
    }
    this.selectedIndex = index;
    buttons.get(index).setSelected(true);
    if (onSelectionChange != null) {
      onSelectionChange.accept(selectedIndex);
    }
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public void setOnSelectionChange(Consumer<Integer> listener) {
    this.onSelectionChange = listener;
  }
}
