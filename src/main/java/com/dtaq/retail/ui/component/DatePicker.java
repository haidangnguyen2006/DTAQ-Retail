package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Ô ngày có lịch bật ra (bấm biểu tượng lịch hoặc Alt+↓); vẫn gõ tay được theo dd/MM/yyyy. Tuần bắt
 * đầu từ thứ Hai theo thói quen Việt Nam.
 */
public final class DatePicker {

  /** Đọc chặt: ngày không có thật như 31/02 bị từ chối thay vì tự lùi về 28/02. */
  private static final DateTimeFormatter NGAY_NHAP =
      DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final String[] THU = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

  private final JTextField text = new JTextField();
  private final FormField field;
  private final JPopupMenu popup = new JPopupMenu();
  private final JLabel thangLabel = new JLabel("", SwingConstants.CENTER);
  private final JPanel luoiNgay = new JPanel(new GridLayout(0, 7, 2, 2));
  private YearMonth thangDangXem = YearMonth.now();

  public DatePicker(String label) {
    text.setFont(DtaqFonts.BODY_14);
    text.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/MM/yyyy");
    JButton moLich = new JButton(DtaqIcons.icon("calendar-days", 18));
    moLich.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    moLich.setToolTipText("Chọn ngày trên lịch");
    moLich.setFocusable(false);
    moLich.addActionListener(e -> moLich());
    text.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, moLich);
    text.registerKeyboardAction(e -> moLich(),
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_FOCUSED);
    field = new FormField(label, text);
    buildPopup();
  }

  public FormField field() {
    return field;
  }

  public JTextField textField() {
    return text;
  }

  /** Ngày đang nhập, hoặc {@code null} nếu trống hay sai định dạng. */
  public LocalDate getDate() {
    try {
      return LocalDate.parse(text.getText().trim(), NGAY_NHAP);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public void setDate(LocalDate ngay) {
    text.setText(ngay == null ? "" : DateFormats.formatDate(ngay));
  }

  public void setError(String error) {
    field.setError(error);
  }

  public String getText() {
    return text.getText().trim();
  }

  private void buildPopup() {
    JPanel panel = new JPanel(new MigLayout("insets 12, gap 6", "[]push[]push[]", "[]8[]8[]"));
    panel.setBackground(DtaqColors.SURFACE);
    thangLabel.setFont(DtaqFonts.CARD_TITLE);
    thangLabel.setForeground(DtaqColors.TEXT);
    panel.add(navButton("chevron-left", "Tháng trước", -1));
    panel.add(thangLabel, "growx");
    panel.add(navButton("chevron-right", "Tháng sau", 1), "wrap");
    luoiNgay.setOpaque(false);
    panel.add(luoiNgay, "span 3, growx, wrap");
    JButton homNay = new JButton("Hôm nay");
    homNay.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
    homNay.setForeground(DtaqColors.PRIMARY);
    homNay.addActionListener(e -> chon(LocalDate.now()));
    panel.add(homNay, "span 3, center");
    popup.setBorder(BorderFactory.createLineBorder(DtaqColors.BORDER));
    popup.add(panel);
  }

  private JButton navButton(String icon, String tip, int buoc) {
    JButton button = new JButton(DtaqIcons.icon(icon, 16));
    button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    button.setToolTipText(tip);
    button.addActionListener(e -> {
      thangDangXem = thangDangXem.plusMonths(buoc);
      veLich();
    });
    return button;
  }

  private void moLich() {
    LocalDate dangChon = getDate();
    thangDangXem = YearMonth.from(dangChon != null ? dangChon : LocalDate.now());
    veLich();
    popup.show(text, 0, text.getHeight() + 4);
  }

  private void veLich() {
    thangLabel.setText("Tháng " + thangDangXem.getMonthValue() + ", " + thangDangXem.getYear());
    luoiNgay.removeAll();
    for (String thu : THU) {
      JLabel nhan = new JLabel(thu, SwingConstants.CENTER);
      nhan.setFont(DtaqFonts.CAPTION);
      nhan.setForeground(DtaqColors.TEXT_SUBTLE);
      luoiNgay.add(nhan);
    }
    LocalDate dau = thangDangXem.atDay(1);
    for (int i = 0; i < dau.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue(); i++) {
      luoiNgay.add(new JLabel());
    }
    LocalDate dangChon = getDate();
    LocalDate homNay = LocalDate.now();
    for (int ngay = 1; ngay <= thangDangXem.lengthOfMonth(); ngay++) {
      LocalDate d = thangDangXem.atDay(ngay);
      JButton nut = new JButton(String.valueOf(ngay));
      nut.setFont(DtaqFonts.BODY_13);
      nut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      nut.setMargin(new java.awt.Insets(4, 6, 4, 6));
      if (d.equals(dangChon)) {
        nut.putClientProperty(FlatClientProperties.STYLE, "arc:999; background:#2457F5; foreground:#FFFFFF; borderWidth:0");
      } else {
        nut.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        nut.setForeground(d.equals(homNay) ? DtaqColors.PRIMARY : DtaqColors.TEXT);
      }
      nut.addActionListener(e -> chon(d));
      luoiNgay.add(nut);
    }
    luoiNgay.revalidate();
    luoiNgay.repaint();
    popup.pack();
  }

  private void chon(LocalDate ngay) {
    setDate(ngay);
    field.setError(null);
    popup.setVisible(false);
    text.requestFocusInWindow();
  }
}
