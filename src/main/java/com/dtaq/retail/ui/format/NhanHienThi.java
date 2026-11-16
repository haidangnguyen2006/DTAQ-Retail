package com.dtaq.retail.ui.format;

import com.dtaq.retail.entity.TrangThaiSanPham;
import java.awt.Component;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * Nhãn tiếng Việt cho giá trị nội bộ khi đưa lên giao diện.
 *
 * <p>Tên enum và mã định danh là dữ liệu của hệ thống; người dùng chỉ thấy nhãn đọc được. Chỉ hiển thị
 * mã ở những cột mà bản thân nó là mã (mã hóa đơn, số serial...).
 */
public final class NhanHienThi {

  private NhanHienThi() {
  }

  /** Nhãn trạng thái của một máy. */
  public static String trangThaiSanPham(TrangThaiSanPham trangThai) {
    if (trangThai == null) {
      return "";
    }
    return switch (trangThai) {
      case SAN_SANG_BAN -> "Sẵn sàng bán";
      case DA_BAN -> "Đã bán";
      case HANG_LOI -> "Hàng lỗi";
      case DANG_BAO_HANH -> "Đang bảo hành";
      case DA_TRA_NHA_CUNG_CAP -> "Đã trả nhà cung cấp";
      case DA_THANH_LY -> "Đã thanh lý";
    };
  }

  /**
   * Cho ô chọn hiển thị nhãn thay vì {@code toString()} của giá trị.
   *
   * @param combo ô chọn cần đặt nhãn
   * @param nhan hàm lấy nhãn của một giá trị khác {@code null}
   * @param nhanRong nhãn của lựa chọn {@code null} (chưa chọn)
   */
  public static <T> void datNhan(JComboBox<T> combo, Function<T, String> nhan, String nhanRong) {
    combo.setRenderer(new DefaultListCellRenderer() {
      @Override
      @SuppressWarnings("unchecked")
      public Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        setText(value == null ? nhanRong : nhan.apply((T) value));
        return this;
      }
    });
  }
}
