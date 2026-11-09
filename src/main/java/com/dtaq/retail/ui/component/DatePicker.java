package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import net.miginfocom.swing.MigLayout;

/**
 * Ô ngày có lịch bật ra (bấm biểu tượng lịch hoặc Alt+↓), tự chèn dấu "/" khi gõ liền tay.
 * Ngày không có thật được báo lỗi khi rời ô. Tuần bắt đầu từ thứ Hai.
 */
public final class DatePicker {

  /** Thông báo chuẩn khi ô ngày có nội dung nhưng ngày không hợp lệ. */
  public static final String LOI_DINH_DANG =
      "Ngày không hợp lệ. Nhập theo dạng dd/MM/yyyy hoặc chọn trên lịch";

  private static final DateTimeFormatter NGAY_HIEN_THI = DateTimeFormatter.ofPattern("dd/MM/uuuu");
  private static final Pattern TIEN_TO_NGAY =
      Pattern.compile("\\d{0,2}|\\d{1,2}/\\d{0,2}|\\d{1,2}/\\d{1,2}/\\d{0,4}");
  private static final Pattern CAN_CHEN_GACH = Pattern.compile("\\d{2}|\\d{1,2}/\\d{2}");
  private static final Pattern TAM_CHU_SO = Pattern.compile("\\d{8}");
  private static final Pattern NGAY_ISO = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");

  /** Đọc chặt: ngày không có thật như 31/02 bị từ chối thay vì tự lùi về 28/02. */
  private static final DateTimeFormatter NGAY_NHAP =
      DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final String[] THU = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

  private final JTextField text = new JTextField();
  private final FormField field;
  private final String loiDinhDang;
  private final JButton moLichButton;
  private final JPopupMenu popup = new JPopupMenu();
  private final JLabel thangLabel = new JLabel("", SwingConstants.CENTER);
  private final JPanel luoiNgay = new JPanel(new GridLayout(0, 7, 2, 2));
  private YearMonth thangDangXem = YearMonth.now();

  public DatePicker(String label) {
    this(label, LOI_DINH_DANG);
  }

  /** Cho phép dùng thông báo ngắn hơn ở các ô ngày nằm cạnh nhau. */
  public DatePicker(String label, String loiDinhDang) {
    this.loiDinhDang = loiDinhDang;
    text.setFont(DtaqFonts.BODY_14);
    text.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/MM/yyyy");
    moLichButton = new JButton(DtaqIcons.icon("calendar-days", 18));
    moLichButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    moLichButton.setToolTipText("Chọn ngày trên lịch");
    moLichButton.setFocusable(false);
    moLichButton.addActionListener(e -> moLich());
    text.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, moLichButton);
    text.registerKeyboardAction(e -> moLich(),
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_FOCUSED);
    field = new FormField(label, text);
    ((AbstractDocument) text.getDocument()).setDocumentFilter(new MatNaNgay());
    text.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        field.setError(null);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        field.setError(null);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {}
    });
    text.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        if (!e.isTemporary() && !popup.isVisible()) {
          kiemTraDinhDang();
        }
      }
    });
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
    text.setText(ngay == null ? "" : NGAY_HIEN_THI.format(ngay));
  }

  public void setError(String error) {
    field.setError(error);
  }

  public String getText() {
    return text.getText().trim();
  }

  /** Cho biết ô ngày đang trống. */
  public boolean isBlank() {
    return getText().isEmpty();
  }

  /** Cho biết ô có nội dung nhưng không đọc được ngày hợp lệ. */
  public boolean hasInvalidText() {
    return !isBlank() && getDate() == null;
  }

  /** Báo lỗi định dạng nếu cần. Ô trống được kiểm tra theo quy tắc của từng màn hình. */
  public boolean kiemTraDinhDang() {
    if (hasInvalidText()) {
      setError(loiDinhDang);
      return false;
    }
    return true;
  }

  /** Bật hoặc tắt cả ô nhập và nút lịch. */
  public void setEnabled(boolean enabled) {
    text.setEnabled(enabled);
    moLichButton.setEnabled(enabled);
    if (!enabled) {
      popup.setVisible(false);
    }
  }

  /** Trả về nội dung sau thao tác, hoặc {@code null} nếu cần từ chối. */
  static String noiDungSauKhiGo(String hienTai, int viTri, int doDai, String chen) {
    String doanChen = chen == null ? "" : chen.strip();
    String ketQua = hienTai.substring(0, viTri) + doanChen + hienTai.substring(viTri + doDai);
    if (doanChen.isEmpty() || TIEN_TO_NGAY.matcher(ketQua).matches()) {
      return ketQua;
    }
    if (doDai == 0 && viTri == hienTai.length() && doanChen.length() == 1
        && doanChen.charAt(0) >= '0' && doanChen.charAt(0) <= '9'
        && CAN_CHEN_GACH.matcher(hienTai).matches()) {
      return hienTai + "/" + doanChen;
    }
    if (doanChen.length() > 1 && TAM_CHU_SO.matcher(ketQua).matches()) {
      return ketQua.substring(0, 2) + "/" + ketQua.substring(2, 4) + "/" + ketQua.substring(4);
    }
    Matcher iso = NGAY_ISO.matcher(ketQua);
    if (iso.matches()) {
      try {
        return NGAY_HIEN_THI.format(LocalDate.of(
            Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3))));
      } catch (DateTimeException e) {
        return null;
      }
    }
    return null;
  }

  private final class MatNaNgay extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
        throws BadLocationException {
      replace(fb, offset, 0, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String chen, AttributeSet attrs)
        throws BadLocationException {
      String hienTai = fb.getDocument().getText(0, fb.getDocument().getLength());
      String doanChen = chen == null ? "" : chen.strip();
      String moi = noiDungSauKhiGo(hienTai, offset, length, chen);
      if (moi == null) {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null) {
          laf.provideErrorFeedback(text);
        }
        return;
      }
      String duKien = hienTai.substring(0, offset) + doanChen + hienTai.substring(offset + length);
      if (moi.equals(duKien)) {
        fb.replace(offset, length, doanChen, attrs);
      } else if (offset == hienTai.length() && moi.equals(hienTai + "/" + doanChen)) {
        fb.replace(offset, 0, "/" + doanChen, attrs);
      } else {
        fb.replace(0, hienTai.length(), moi, attrs);
      }
    }
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
    if (!text.isEnabled()) {
      return;
    }
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
