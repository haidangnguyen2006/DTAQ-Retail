package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.Trang;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử các truy vấn phân trang (LIMIT/OFFSET kèm tổng số) trên cơ sở dữ liệu phát triển. */
class PhanTrangDaoDbTest {

  private DataSource dataSource;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dataSource = ConnectDB.getDataSource();
  }

  @Test
  @DisplayName("Máy: tổng số đúng, hai trang liên tiếp không trùng, từ khóa lọc tại CSDL")
  void sanPham() throws SQLException {
    SanPhamImpl dao = new SanPhamImpl(dataSource);
    Trang<SanPham> trang1 = dao.timTrang("", 0, 5);
    Trang<SanPham> trang2 = dao.timTrang("", 5, 5);

    assertEquals(dem("SanPham"), trang1.tongSo());
    assertTrue(trang1.dong().size() <= 5);
    Set<String> serial = new HashSet<>();
    trang1.dong().forEach(sp -> serial.add(sp.soSerial()));
    assertTrue(trang2.dong().stream().noneMatch(sp -> serial.contains(sp.soSerial())));

    Assumptions.assumeFalse(trang1.dong().isEmpty());
    String tuKhoa = trang1.dong().getFirst().soSerial();
    Trang<SanPham> loc = dao.timTrang(tuKhoa, 0, 5);
    assertTrue(loc.tongSo() >= 1 && loc.tongSo() < Math.max(2, trang1.tongSo()));
    assertTrue(dao.timTrang("khong-co-may-nao-nhu-the", 0, 5).dong().isEmpty());
  }

  @Test
  @DisplayName("Dòng sản phẩm, hồ sơ lỗi, khách hàng: tổng số của trang khớp số dòng của bảng")
  void cacBangConLai() throws SQLException {
    Trang<DongSanPham> dong = new DongSanPhamImpl(dataSource).timTrang("", 0, 3);
    assertEquals(dem("DongSanPham"), dong.tongSo());
    assertTrue(dong.dong().size() <= 3);

    SanPhamLoiDaoImpl loiDao = new SanPhamLoiDaoImpl(dataSource);
    Trang<SanPhamLoi> loi = loiDao.timTrang(null, "", 0, 3);
    assertEquals(dem("SanPhamLoi"), loi.tongSo());
    assertTrue(loiDao.timTrang(TrangThaiXuLyLoi.CHO_XU_LY, "", 0, 50).dong().stream()
        .allMatch(spl -> spl.tinhTrangXuLy() == TrangThaiXuLyLoi.CHO_XU_LY));

    Trang<KhachHang> khach = new KhachHangDaoImpl(dataSource).findTrang(0, 3);
    assertEquals(dem("KhachHang"), khach.tongSo());
    assertTrue(khach.dong().size() <= 3);
  }

  private int dem(String bang) throws SQLException {
    try (Connection ketNoi = dataSource.getConnection();
        Statement lenh = ketNoi.createStatement();
        ResultSet ketQua = lenh.executeQuery("SELECT count(*) FROM \"" + bang + "\"")) {
      ketQua.next();
      return ketQua.getInt(1);
    }
  }
}
