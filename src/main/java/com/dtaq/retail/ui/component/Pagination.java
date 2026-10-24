package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh phân trang (Pagination) chuẩn theo thiết kế Figma.
 */
public class Pagination extends JPanel {

  private final JLabel infoLabel = new JLabel();
  private final DtaqButton prevButton = DtaqButton.secondary("Trước");
  private final StatusPill pagePill = StatusPill.success("1");
  private final DtaqButton nextButton = DtaqButton.secondary("Sau");

  private int currentPage = 1;
  private int totalPages = 1;
  private Consumer<Integer> onPageChangeListener;

  public Pagination() {
    super(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    setOpaque(false);

    infoLabel.setFont(DtaqFonts.CAPTION);
    infoLabel.setForeground(DtaqColors.TEXT_MUTED);
    add(infoLabel, "align left, aligny center");

    JPanel rightControls = new JPanel(new MigLayout("insets 0", "[]8[]8[]", "[]"));
    rightControls.setOpaque(false);

    prevButton.setPreferredSize(new Dimension(73, 42));
    nextButton.setPreferredSize(new Dimension(58, 42));

    prevButton.addActionListener(e -> {
      if (currentPage > 1) {
        setPage(currentPage - 1);
        if (onPageChangeListener != null) {
          onPageChangeListener.accept(currentPage);
        }
      }
    });

    nextButton.addActionListener(e -> {
      if (currentPage < totalPages) {
        setPage(currentPage + 1);
        if (onPageChangeListener != null) {
          onPageChangeListener.accept(currentPage);
        }
      }
    });

    rightControls.add(prevButton);
    rightControls.add(pagePill);
    rightControls.add(nextButton);

    add(rightControls, "align right, aligny center");
    updateControls();
  }

  public void setInfo(String text) {
    infoLabel.setText(text);
  }

  public void setPageData(int current, int total, String infoText) {
    this.currentPage = current;
    this.totalPages = Math.max(1, total);
    setInfo(infoText);
    pagePill.setText(String.valueOf(currentPage));
    updateControls();
  }

  public void setPage(int page) {
    this.currentPage = page;
    pagePill.setText(String.valueOf(page));
    updateControls();
  }

  public void setOnPageChangeListener(Consumer<Integer> listener) {
    this.onPageChangeListener = listener;
  }

  private void updateControls() {
    prevButton.setEnabled(currentPage > 1);
    nextButton.setEnabled(currentPage < totalPages);
  }
}
