package com.dtaq.retail.ui.nav;

import com.dtaq.retail.entity.NhanVien;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Nội dung mục "Trang chủ": lời chào theo giờ, vai trò và mã nhân viên, ngày hiện tại và thẻ hướng
 * dẫn bắt đầu.
 *
 * <p>Theo ràng buộc C-11 (quy tắc làm tròn chưa chốt), panel này KHÔNG hiển thị bất kỳ số liệu
 * nghiệp vụ nào (doanh thu, tồn kho, tiền...); các thẻ số liệu chỉ bổ sung khi tính năng tương ứng
 * hoàn thành.
 */
public final class HomePanel extends JPanel {

  /** Màu xám dùng cho nhãn phụ. */
  private static final Color GREY = new Color(0x6B7280);

  /** Màu viền của thẻ thông tin. */
  private static final Color CARD_BORDER = new Color(0xE5E7EB);

  /** Cỡ chữ lớn cho lời chào. */
  private static final float GREETING_FONT_SIZE = 22f;

  /**
   * Khởi tạo trang chủ cho một nhân viên.
   *
   * @param nhanVien nhân viên đang đăng nhập; không được {@code null}
   */
  public HomePanel(NhanVien nhanVien) {
    super(new MigLayout("fill, wrap 1, insets 40", "[grow]", "[]"));
    Objects.requireNonNull(nhanVien, "nhanVien");
    setName("homePanel");

    JLabel greetingLabel = new JLabel(greeting(nhanVien));
    greetingLabel.setName("greeting");
    greetingLabel.setFont(greetingLabel.getFont().deriveFont(Font.BOLD, GREETING_FONT_SIZE));
    JLabel identityLabel = new JLabel(nhanVien.moTaQuyen() + " · " + nhanVien.maNV());
    identityLabel.setName("identity");
    JLabel dateLabel = new JLabel(LocalDate.now().toString());
    dateLabel.setName("date");
    dateLabel.setForeground(GREY);

    add(greetingLabel);
    add(identityLabel);
    add(dateLabel, "gapy 8");
    add(infoCard(), "growx, gapy 24");
  }

  /** Thẻ viền 1px chứa hướng dẫn bắt đầu; không có số liệu nghiệp vụ (C-11). */
  private JPanel infoCard() {
    JPanel card = new JPanel(
        new MigLayout("fillx, wrap 1, insets 20", "[grow]", "[]"));
    card.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

    JLabel startLabel =
        new JLabel("Chọn một mục trong “KHÔNG GIAN LÀM VIỆC” ở thanh bên để bắt đầu.");
    JLabel metricLabel = new JLabel(
        "Các thẻ số liệu sẽ được bổ sung khi tính năng tương ứng hoàn thành.");
    metricLabel.setForeground(GREY);

    card.add(startLabel, "wrap");
    card.add(metricLabel);
    return card;
  }

  /** Lời chào theo giờ hiện tại + tên (phần sau khoảng trắng cuối của họ tên, không có thì dùng nguyên). */
  private static String greeting(NhanVien nhanVien) {
    int hour = LocalTime.now().getHour();
    String prefix = hour < 12 ? "Chào buổi sáng, " : hour < 18 ? "Chào buổi chiều, " : "Chào buổi tối, ";
    return prefix + lastName(nhanVien.hoTen());
  }

  /** Phần tên cuối: chữ sau khoảng trắng cuối của họ tên đã trim, không có khoảng trắng thì trả nguyên chuỗi. */
  private static String lastName(String hoTen) {
    String trimmed = hoTen.trim();
    int lastSpace = trimmed.lastIndexOf(' ');
    return lastSpace >= 0 ? trimmed.substring(lastSpace + 1) : trimmed;
  }
}
