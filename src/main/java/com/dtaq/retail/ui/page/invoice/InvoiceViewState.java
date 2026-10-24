package com.dtaq.retail.ui.page.invoice;

/**
 * Các trạng thái hiển thị của màn hình Lập hóa đơn bán lẻ (theo prototype Figma, thêm trạng thái xử lý).
 */
public enum InvoiceViewState {
  /** Chưa chọn khách hàng (Figma frame 1:1054). */
  NO_CUSTOMER,

  /** Đã chọn khách hàng, giỏ còn trống (Figma frame 2003:231). */
  CUSTOMER_SELECTED,

  /** Hiển thị danh sách kết quả tra cứu sản phẩm (Figma frame 1:878). */
  PRODUCT_RESULTS,

  /** Đã có sản phẩm trong giỏ, sẵn sàng thanh toán (Figma frame 1:716). */
  READY_TO_PAY,

  /** Đã gửi yêu cầu thanh toán, đang chờ kết quả từ controller; khóa toàn bộ thao tác. */
  PROCESSING,

  /** Đã thanh toán thành công (Figma frame 1:523). */
  PAID
}
