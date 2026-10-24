package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh phân trang (Pagination): thông tin bên trái; "Trang x / y" cùng nút trước/sau bên phải.
 */
public class Pagination extends JPanel {

  private final JLabel infoLabel = new JLabel();
  private final JLabel pageLabel = new JLabel("Trang 1 / 1");
  private final DtaqButton prevButton = DtaqButton.secondarySmall("");
  private final DtaqButton nextButton = DtaqButton.secondarySmall("");

  private int currentPage = 1;
  private int totalPages = 1;
  private Consumer<Integer> onPageChangeListener;

  /** Tổng số trang cho {@code totalItems} phần tử, mỗi trang {@code pageSize}; tối thiểu 1 trang. */
  public static int totalPages(int totalItems, int pageSize) {
    if (pageSize <= 0 || totalItems <= 0) {
      return 1;
    }
    return (totalItems + pageSize - 1) / pageSize;
  }

  public Pagination() {
    super(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    setOpaque(false);

    infoLabel.setFont(DtaqFonts.CAPTION);
    infoLabel.setForeground(DtaqColors.TEXT_MUTED);
    add(infoLabel, "align left, aligny center");

    pageLabel.setFont(DtaqFonts.CAPTION);
    pageLabel.setForeground(DtaqColors.TEXT_MUTED);

    prevButton.setIcon(DtaqIcons.icon("chevron-left", 16, DtaqColors.TEXT_MUTED));
    prevButton.setToolTipText("Trang trước");
    nextButton.setIcon(DtaqIcons.icon("chevron-right", 16, DtaqColors.TEXT_MUTED));
    nextButton.setToolTipText("Trang sau");

    prevButton.addActionListener(e -> goTo(currentPage - 1));
    nextButton.addActionListener(e -> goTo(currentPage + 1));

    JPanel rightControls = new JPanel(new MigLayout("insets 0, gapx 10", "[][][]", "[]"));
    rightControls.setOpaque(false);
    rightControls.add(prevButton);
    rightControls.add(pageLabel);
    rightControls.add(nextButton);
    add(rightControls, "align right, aligny center");
    updateControls();
  }

  private void goTo(int page) {
    if (page < 1 || page > totalPages || page == currentPage) {
      return;
    }
    setPage(page);
    if (onPageChangeListener != null) {
      onPageChangeListener.accept(currentPage);
    }
  }

  public void setInfo(String text) {
    infoLabel.setText(text);
  }

  public void setPageData(int current, int total, String infoText) {
    this.totalPages = Math.max(1, total);
    this.currentPage = Math.min(Math.max(1, current), totalPages);
    setInfo(infoText);
    updateControls();
  }

  public void setPage(int page) {
    this.currentPage = Math.min(Math.max(1, page), totalPages);
    updateControls();
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public void setOnPageChangeListener(Consumer<Integer> listener) {
    this.onPageChangeListener = listener;
  }

  private void updateControls() {
    pageLabel.setText("Trang " + currentPage + " / " + totalPages);
    prevButton.setEnabled(currentPage > 1);
    nextButton.setEnabled(currentPage < totalPages);
  }
}
