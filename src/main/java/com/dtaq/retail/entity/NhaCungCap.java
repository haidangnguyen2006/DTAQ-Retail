package com.dtaq.retail.entity;

import java.util.Objects;

/** Nhà cung cấp hàng hóa, ánh xạ bảng "NhaCungCap". */
public class NhaCungCap {

  private String maNCC;
  private String tenNCC;

  /** Tạo đối tượng rỗng. */
  public NhaCungCap() {}

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp, không kiểm tra ràng buộc.
   */
  public NhaCungCap(String maNCC, String tenNCC) {
    this.maNCC = maNCC;
    this.tenNCC = tenNCC;
  }

  /** Mã nhà cung cấp; không thể thay đổi sau khi tạo. */
  public String maNCC() {
    return maNCC;
  }

  /** Mã nhà cung cấp; không thể thay đổi sau khi tạo. */
  public String getMaNCC() {
    return maNCC;
  }

  public String tenNCC() {
    return tenNCC;
  }

  public String getTenNCC() {
    return tenNCC;
  }

  /** Gán tên nhà cung cấp; bắt buộc có giá trị. */
  public void setTenNCC(String tenNCC) {
    this.tenNCC = RangBuoc.chuoiBatBuoc(tenNCC, "tenNCC");
  }

  @Override
  public String toString() {
    return "NhaCungCap[maNCC=" + maNCC
        + ", tenNCC=" + tenNCC
        + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NhaCungCap khac)) {
      return false;
    }
    return Objects.equals(maNCC, khac.maNCC)
        && Objects.equals(tenNCC, khac.tenNCC);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maNCC, tenNCC);
  }
}
