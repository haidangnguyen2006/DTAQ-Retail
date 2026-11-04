package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.entity.TrangThaiDuyet;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp {@link PhieuTraDaoImpl} trên cơ sở dữ liệu PostgreSQL (dữ liệu seed V4).
 */
class PhieuTraDaoImplTest {

  private static final String PHIEU_CHO_DUYET_1 = "PT-2026-000010";
  private static final String PHIEU_CHO_DUYET_2 = "PT-2026-000011";
  private static final String PHIEU_TU_CHOI = "PT-2026-000012";
  private static final String MA_HD_GOC_1 = "HD-2026-000010";
  private static final String MA_NV_DUYET = "NVQUANLY-2026-001";
  private static final LocalDate NGAY_DUYET = LocalDate.of(2026, 11, 4);

  private PhieuTraDaoImpl dao;
  private TrangThaiDuyetGoc goc;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new PhieuTraDaoImpl(ConnectDB.getDataSource());
    goc = docTrangThaiGoc(PHIEU_CHO_DUYET_1);
  }

  @AfterEach
  void tearDown() {
    if (goc != null && ConnectDB.isAvailable()) {
      khoiPhucTrangThai(PHIEU_CHO_DUYET_1, goc);
    }
  }

  @Test
  @DisplayName("findChoDuyet chỉ trả về đúng hai phiếu trả đang chờ duyệt trong seed")
  void findChoDuyetTraVeHaiPhieuSeed() {
    List<PhieuDuyet> danhSach = dao.findChoDuyet();

    assertNotNull(danhSach);
    assertEquals(List.of(PHIEU_CHO_DUYET_1, PHIEU_CHO_DUYET_2),
        danhSach.stream().map(PhieuDuyet::maPhieu).toList(),
        "Chỉ PT-2026-000010 và PT-2026-000011 đang chờ duyệt, theo thứ tự ngày tạo");

    for (PhieuDuyet phieu : danhSach) {
      assertEquals(PhieuDuyet.LoaiPhieu.PHIEU_TRA, phieu.loai());
      assertFalse(PHIEU_TU_CHOI.equals(phieu.maPhieu()),
          "Phiếu đã bị từ chối không được xuất hiện trong danh sách chờ duyệt");
    }

    PhieuDuyet dauTien = danhSach.get(0);
    assertEquals(MA_HD_GOC_1, dauTien.maHDGoc());
    assertEquals(0, new BigDecimal("12096000").compareTo(dauTien.hoanDuKien()));
    assertEquals(LocalDate.of(2026, 3, 13), dauTien.ngayTao());
    assertNotNull(dauTien.tenKhachHang());
    assertFalse(dauTien.tenKhachHang().isBlank(), "Phải nạp họ tên khách hàng");
    assertNotNull(dauTien.soDienThoai());
    assertFalse(dauTien.soDienThoai().isBlank(), "Phải nạp số điện thoại khách hàng");
    assertNotNull(dauTien.tenNhanVienLap());
    assertFalse(dauTien.tenNhanVienLap().isBlank(), "Phải nạp họ tên nhân viên lập phiếu");
    assertNotNull(dauTien.lyDo(), "Phải gộp lý do trả từ chi tiết phiếu");
    assertTrue(dauTien.lyDo().contains("không phù hợp nhu cầu"),
        "Lý do trả phải chứa nội dung do nhân viên ghi nhận");
  }

  @Test
  @DisplayName("capNhatQuyetDinh chỉ ghi được khi phiếu vẫn còn chờ duyệt")
  void capNhatQuyetDinhChiGhiKhiConChoDuyet() {
    assertEquals("CHO_DUYET", goc.trangThaiDuyet(), "Điều kiện đầu vào của bài kiểm thử");

    boolean lanDau = dao.capNhatQuyetDinh(
        PHIEU_CHO_DUYET_1, TrangThaiDuyet.DA_DUYET, MA_NV_DUYET, NGAY_DUYET, "Ý kiến kiểm tra");
    assertTrue(lanDau, "Lần duyệt đầu tiên phải cập nhật đúng một dòng");

    TrangThaiDuyetGoc sauDuyet = docTrangThaiGoc(PHIEU_CHO_DUYET_1);
    assertEquals("DA_DUYET", sauDuyet.trangThaiDuyet());
    assertEquals(MA_NV_DUYET, sauDuyet.nhanVienDuyet());
    assertEquals(NGAY_DUYET, sauDuyet.ngayDuyet());
    assertEquals("Ý kiến kiểm tra", sauDuyet.lyDoDuyet());

    boolean lanHai = dao.capNhatQuyetDinh(
        PHIEU_CHO_DUYET_1, TrangThaiDuyet.TU_CHOI, MA_NV_DUYET, NGAY_DUYET, "Ý kiến ghi đè");
    assertFalse(lanHai, "Phiếu đã xử lý thì không được cập nhật lần nữa");

    TrangThaiDuyetGoc sauLanHai = docTrangThaiGoc(PHIEU_CHO_DUYET_1);
    assertEquals("DA_DUYET", sauLanHai.trangThaiDuyet(), "Quyết định đầu tiên phải được giữ nguyên");
    assertEquals("Ý kiến kiểm tra", sauLanHai.lyDoDuyet(), "Ý kiến cũ không được bị ghi đè");
  }

  private static TrangThaiDuyetGoc docTrangThaiGoc(String maPhieu) {
    String sql = "SELECT \"trangThaiDuyet\", \"nhanVienDuyet\", \"ngayDuyet\", \"lyDoDuyet\""
        + " FROM \"PhieuTra\" WHERE \"maPhieuTra\" = ?";
    try (Connection conn = ConnectDB.getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, maPhieu);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Không tìm thấy phiếu trả " + maPhieu);
        Date ngayDuyet = rs.getDate("ngayDuyet");
        return new TrangThaiDuyetGoc(
            rs.getString("trangThaiDuyet"),
            rs.getString("nhanVienDuyet"),
            ngayDuyet == null ? null : ngayDuyet.toLocalDate(),
            rs.getString("lyDoDuyet"));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Không đọc được trạng thái phiếu trả " + maPhieu, e);
    }
  }

  private static void khoiPhucTrangThai(String maPhieu, TrangThaiDuyetGoc goc) {
    String sql = "UPDATE \"PhieuTra\" SET \"trangThaiDuyet\" = ?, \"nhanVienDuyet\" = ?,"
        + " \"ngayDuyet\" = ?, \"lyDoDuyet\" = ? WHERE \"maPhieuTra\" = ?";
    try (Connection conn = ConnectDB.getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, goc.trangThaiDuyet());
      if (goc.nhanVienDuyet() == null) {
        ps.setNull(2, Types.VARCHAR);
      } else {
        ps.setString(2, goc.nhanVienDuyet());
      }
      if (goc.ngayDuyet() == null) {
        ps.setNull(3, Types.DATE);
      } else {
        ps.setDate(3, Date.valueOf(goc.ngayDuyet()));
      }
      if (goc.lyDoDuyet() == null) {
        ps.setNull(4, Types.VARCHAR);
      } else {
        ps.setString(4, goc.lyDoDuyet());
      }
      ps.setString(5, maPhieu);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Không khôi phục được trạng thái phiếu trả " + maPhieu, e);
    }
  }

  /** Giá trị duyệt ban đầu của một phiếu, dùng để khôi phục sau khi kiểm thử. */
  private record TrangThaiDuyetGoc(
      String trangThaiDuyet, String nhanVienDuyet, LocalDate ngayDuyet, String lyDoDuyet) {
  }
}
