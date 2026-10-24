package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.miginfocom.swing.MigLayout;

/**
 * Vùng dữ liệu có đủ 4 trạng thái: đang tải, rỗng, lỗi và có dữ liệu.
 * Chỉ một trạng thái được hiển thị tại một thời điểm và kích thước theo trạng thái đang hiển thị.
 */
public class StatefulView extends JPanel {

  public enum State {
    LOADING,
    EMPTY,
    ERROR,
    CONTENT
  }

  private final JComponent content;
  private final String defaultEmptyIcon;
  private final String defaultEmptyTitle;
  private final String defaultEmptyHint;

  private State state;
  private Runnable onRetry;

  public StatefulView(JComponent content, String emptyIcon, String emptyTitle, String emptyHint) {
    super(new BorderLayout());
    this.content = content;
    this.defaultEmptyIcon = emptyIcon;
    this.defaultEmptyTitle = emptyTitle;
    this.defaultEmptyHint = emptyHint;
    setOpaque(false);
    showEmpty();
  }

  public State getState() {
    return state;
  }

  public void setOnRetry(Runnable onRetry) {
    this.onRetry = onRetry;
  }

  public void showLoading() {
    JPanel panel = new JPanel(new MigLayout("fill, insets 36 32 36 32", "[center]", "[]12[]"));
    panel.setOpaque(false);
    JLabel label = new JLabel("Đang tải dữ liệu…");
    label.setFont(DtaqFonts.BODY_13);
    label.setForeground(DtaqColors.TEXT_MUTED);
    panel.add(label, "center, wrap");
    JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);
    bar.setPreferredSize(new Dimension(180, 4));
    panel.add(bar, "center, w 180!, h 4!");
    show(State.LOADING, panel);
  }

  public void showEmpty() {
    showEmpty(defaultEmptyIcon, defaultEmptyTitle, defaultEmptyHint);
  }

  public void showEmpty(String iconName, String title, String hint) {
    show(State.EMPTY, new EmptyState(iconName, title, hint));
  }

  public void showError(String message) {
    DtaqButton retry = null;
    if (onRetry != null) {
      retry = DtaqButton.secondary("Thử lại");
      retry.addActionListener(e -> onRetry.run());
    }
    String text = (message == null || message.isBlank()) ? "Không thể tải dữ liệu" : message;
    show(State.ERROR, new EmptyState("alert-circle", "Có lỗi xảy ra", text, retry));
  }

  public void showContent() {
    show(State.CONTENT, content);
  }

  private void show(State newState, JComponent component) {
    state = newState;
    removeAll();
    add(component, BorderLayout.CENTER);
    revalidate();
    repaint();
  }
}
