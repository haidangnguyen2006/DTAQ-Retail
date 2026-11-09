package com.dtaq.retail.ui.page.backup;

import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại tiến trình không xác định thời lượng, dùng để chặn thao tác khác trong khi sao lưu hoặc
 * phục hồi cơ sở dữ liệu đang chạy.
 *
 * <p>Hộp thoại không thể đóng bằng Esc, nút × hay bấm ra nền khi đang bận; chỉ {@link #finish()}
 * mới đóng được. Bên gọi phải gọi {@code finish()} trong {@code SwingWorker.done()}.
 */
public final class ProgressDialog extends ModalDialog {

  private static final int BAR_HEIGHT = 6;

  private final JLabel stageLabel = new JLabel();
  private final JProgressBar progressBar = new JProgressBar();
  private boolean busy = true;

  /**
   * Khởi tạo hộp thoại tiến trình.
   *
   * @param parent cửa sổ cha; có thể {@code null}
   * @param title tiêu đề hộp thoại
   */
  public ProgressDialog(Frame parent, String title) {
    super(parent, title, null, 480);
    setCloseOnOverlayClick(false);

    stageLabel.setFont(DtaqFonts.BODY_14);
    stageLabel.setForeground(DtaqColors.TEXT);
    getBody().add(stageLabel, "growx, wrap, gapbottom 12");

    progressBar.setIndeterminate(true);
    progressBar.setPreferredSize(new Dimension(0, BAR_HEIGHT));
    getBody().add(progressBar, "growx");

    DtaqButton cho = DtaqButton.secondary("Đang xử lý...");
    cho.setEnabled(false);
    setActions(cho);
  }

  /**
   * Đổi dòng mô tả giai đoạn đang chạy.
   *
   * @param text mô tả giai đoạn; {@code null} được hiểu là chuỗi rỗng
   */
  public void setStage(String text) {
    stageLabel.setText(text == null ? "" : text);
  }

  /** Kết thúc tiến trình và đóng hộp thoại; gọi trên luồng giao diện. */
  public void finish() {
    busy = false;
    dispose();
  }

  /** Chặn mọi cách đóng khi tiến trình còn đang chạy. */
  @Override
  protected void requestClose() {
    if (!busy) {
      super.requestClose();
    }
  }
}
