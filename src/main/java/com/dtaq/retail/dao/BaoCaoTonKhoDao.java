package com.dtaq.retail.dao;

import com.dtaq.retail.entity.TrangThaiSanPham;
import java.time.LocalDate;
import java.util.List;

/** Truy vấn báo cáo máy tồn kho lâu ngày theo UC014. */
public interface BaoCaoTonKhoDao {

  /** Một máy đủ điều kiện xuất hiện trong báo cáo tồn kho lâu ngày. */
  record DongTonLau(
      String soSerial,
      String maDongSP,
      String tenDongSP,
      String tenDanhMuc,
      LocalDate ngayNhapKhoBanDau,
      TrangThaiSanPham trangThai,
      long soNgayTon) {}

  /**
   * Tìm các máy còn tồn có số ngày lưu kho đạt ngưỡng báo cáo.
   *
   * @param nguongNgay ngưỡng số ngày tồn kho, phải lớn hơn 0
   * @param chiTinhSanSangBan nếu đúng, chỉ lấy máy {@code SAN_SANG_BAN}
   * @return danh sách máy, sắp xếp theo dòng sản phẩm, ngày nhập ban đầu và serial
   * @throws IllegalArgumentException nếu ngưỡng không dương
   */
  List<DongTonLau> findTonLau(int nguongNgay, boolean chiTinhSanSangBan);
}
