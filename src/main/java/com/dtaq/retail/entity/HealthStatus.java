package com.dtaq.retail.entity;

/**
 * Trạng thái sức khỏe dữ liệu của một chu kỳ kiểm tra.
 *
 * <p>Trạng thái do {@code DataHealthChecker} xác định từ số dòng của các bảng cốt lõi, baseline đã
 * lưu và SQLState khi kết nối cơ sở dữ liệu gặp lỗi.
 */
public enum HealthStatus {

  /** Đủ bảng cốt lõi, có dữ liệu và không có suy giảm đáng kể so với baseline. */
  HEALTHY,

  /** Đủ bảng và còn dữ liệu nhưng một số bảng giảm mạnh so với baseline; cần Quản lý xác nhận. */
  SUSPICIOUS,

  /** Thiếu bảng cốt lõi, schema rỗng hoặc cơ sở dữ liệu hỏng; cần phục hồi. */
  CRITICAL,

  /** Không kết nối được tới cơ sở dữ liệu hoặc bị từ chối quyền; chưa thể đánh giá dữ liệu. */
  UNAVAILABLE
}
