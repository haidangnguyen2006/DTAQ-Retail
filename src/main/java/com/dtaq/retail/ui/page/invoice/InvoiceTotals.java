package com.dtaq.retail.ui.page.invoice;

/**
 * Tổng tiền của hóa đơn đang lập, do controller tính (UI chỉ hiển thị, không tính thuế, giảm giá
 * hay làm tròn).
 *
 * @param tongTruocThue tổng tiền hàng trước thuế sau giảm giá (VNĐ)
 * @param tongThue tổng tiền thuế (VNĐ)
 * @param tongThanhToan tổng thanh toán (VNĐ)
 * @param tongGiamGia tổng tiền được giảm giá (VNĐ)
 */
public record InvoiceTotals(
    long tongTruocThue, long tongThue, long tongThanhToan, long tongGiamGia) {

  /** Hóa đơn chưa có sản phẩm. */
  public static final InvoiceTotals EMPTY = new InvoiceTotals(0, 0, 0, 0);

  /**
   * Tổng tiền của hóa đơn không có giảm giá.
   *
   * @param tongTruocThue tổng tiền hàng trước thuế (VNĐ)
   * @param tongThue tổng tiền thuế (VNĐ)
   * @param tongThanhToan tổng thanh toán (VNĐ)
   */
  public InvoiceTotals(long tongTruocThue, long tongThue, long tongThanhToan) {
    this(tongTruocThue, tongThue, tongThanhToan, 0);
  }
}
