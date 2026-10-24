package com.dtaq.retail.ui.page.invoice;

/**
 * Tổng tiền của hóa đơn đang lập, do controller tính (UI chỉ hiển thị, không tính thuế hay làm tròn).
 *
 * @param tongTruocThue tổng tiền hàng trước thuế (VNĐ)
 * @param tongThue tổng tiền thuế (VNĐ)
 * @param tongThanhToan tổng thanh toán (VNĐ)
 */
public record InvoiceTotals(long tongTruocThue, long tongThue, long tongThanhToan) {

  /** Hóa đơn chưa có sản phẩm. */
  public static final InvoiceTotals EMPTY = new InvoiceTotals(0, 0, 0);
}
