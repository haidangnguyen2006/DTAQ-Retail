package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.util.DateRange;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh lọc kỳ báo cáo dùng chung cho các trang thống kê: hai ô nhập "Từ ngày", "Đến ngày" và nút
 * "Áp dụng".
 *
 * <p>Đây là component giao diện thuần: không truy vấn cơ sở dữ liệu, không gọi DAO/controller và
 * không tự tải dữ liệu. Controller của trang đăng ký {@link #setOnApply(Consumer)} để nhận {@link
 * DateRange} đã kiểm tra hợp lệ, hoặc gọi {@link #getDateRange()} khi cần đọc kỳ hiện tại.
 *
 * <p>Kỳ mặc định là 7 ngày gần nhất, khớp cách trang thống kê doanh thu đang chọn kỳ.
 */
public class KyBaoCaoFilter extends JPanel {

  private static final String NHAN_TU_NGAY = "Từ ngày";
  private static final String NHAN_DEN_NGAY = "Đến ngày";
  private static final String GOI_Y_NGAY = "dd/MM/yyyy";
  private static final String BIEU_TUONG_NGAY = "calendar-days";
  private static final int SO_NGAY_MAC_DINH = 7;

  private final FormField tuNgayField =
      FormField.text(NHAN_TU_NGAY, GOI_Y_NGAY, BIEU_TUONG_NGAY);
  private final FormField denNgayField =
      FormField.text(NHAN_DEN_NGAY, GOI_Y_NGAY, BIEU_TUONG_NGAY);
  private final DtaqButton apDungButton = DtaqButton.primarySmall("Áp dụng");

  private Consumer<DateRange> onApply;

  /** Tạo bộ lọc với kỳ mặc định là 7 ngày gần nhất (tính cả hôm nay). */
  public KyBaoCaoFilter() {
    super(boCuc());
    setOpaque(false);
    dungGiaoDien();
    LocalDate denNgay = LocalDate.now();
    setDateRange(denNgay.minusDays(SO_NGAY_MAC_DINH - 1), denNgay);
  }

  /**
   * Tạo bộ lọc với kỳ cho sẵn.
   *
   * @param tuNgay ngày bắt đầu của kỳ; {@code null} để trống ô nhập
   * @param denNgay ngày kết thúc của kỳ; {@code null} để trống ô nhập
   */
  public KyBaoCaoFilter(LocalDate tuNgay, LocalDate denNgay) {
    super(boCuc());
    setOpaque(false);
    dungGiaoDien();
    setDateRange(tuNgay, denNgay);
  }

  private static MigLayout boCuc() {
    return new MigLayout("fillx, insets 0", "[grow,fill]16[grow,fill]16[push][]", "[bottom]");
  }

  private void dungGiaoDien() {
    add(tuNgayField, "growx");
    add(denNgayField, "growx");
    add(apDungButton, "skip 1");
    apDungButton.addActionListener(e -> apply());
  }

  /**
   * Đặt kỳ đang hiển thị và xoá lỗi của cả hai ô nhập.
   *
   * @param tuNgay ngày bắt đầu; {@code null} để trống ô nhập
   * @param denNgay ngày kết thúc; {@code null} để trống ô nhập
   */
  public void setDateRange(LocalDate tuNgay, LocalDate denNgay) {
    tuNgayField.setText(DateFormats.formatDate(tuNgay));
    denNgayField.setText(DateFormats.formatDate(denNgay));
    tuNgayField.setError(null);
    denNgayField.setError(null);
  }

  /**
   * Đọc kỳ hiện tại từ hai ô nhập.
   *
   * @return kỳ hợp lệ, hoặc {@link Optional#empty()} nếu nội dung đang trống hoặc sai định dạng
   */
  public Optional<DateRange> getDateRange() {
    try {
      return Optional.of(DateRange.parse(tuNgayField.getText(), denNgayField.getText()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /**
   * Đăng ký listener nhận kỳ mỗi khi người dùng áp dụng thành công.
   *
   * @param listener listener cần gọi; {@code null} để bỏ đăng ký
   */
  public void setOnApply(Consumer<DateRange> listener) {
    this.onApply = listener;
  }

  /**
   * Kiểm tra kỳ đang nhập và trao cho listener nếu hợp lệ.
   *
   * <p>Khi không hợp lệ, thông điệp lỗi được hiển thị ngay dưới ô nhập tương ứng và listener không
   * được gọi.
   *
   * @return {@code true} nếu kỳ hợp lệ và đã trao cho listener; {@code false} nếu ngược lại
   */
  public boolean apply() {
    tuNgayField.setError(null);
    denNgayField.setError(null);
    DateRange ky;
    try {
      ky = DateRange.parse(tuNgayField.getText(), denNgayField.getText());
    } catch (IllegalArgumentException e) {
      hienThiLoi(e.getMessage());
      return false;
    }
    if (onApply != null) {
      onApply.accept(ky);
    }
    return true;
  }

  /**
   * Hiển thị lỗi ở ô nhập tương ứng: lỗi thiếu ngày thuộc đúng ô của nó, lỗi định dạng thuộc ô có
   * giá trị không đọc được, lỗi thứ tự thuộc ô "Đến ngày" (khớp {@code PeriodDialog}).
   */
  private void hienThiLoi(String thongDiep) {
    if (thongDiep == null) {
      denNgayField.setError(null);
    } else if (thongDiep.startsWith(NHAN_TU_NGAY)) {
      tuNgayField.setError(thongDiep);
    } else if (thongDiep.startsWith(NHAN_DEN_NGAY)) {
      denNgayField.setError(thongDiep);
    } else if (!docDuoc(tuNgayField.getText())) {
      tuNgayField.setError(thongDiep);
    } else {
      denNgayField.setError(thongDiep);
    }
  }

  /** Kiểm tra một giá trị ngày đơn có đọc được theo định dạng {@code dd/MM/yyyy} hay không. */
  private static boolean docDuoc(String giaTri) {
    try {
      DateRange.parse(giaTri, giaTri);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    tuNgayField.getInputComponent().setEnabled(enabled);
    denNgayField.getInputComponent().setEnabled(enabled);
    apDungButton.setEnabled(enabled);
  }
}
