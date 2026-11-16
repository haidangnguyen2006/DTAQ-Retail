package com.dtaq.retail.controller;

import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.DongSanPham;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.impl.DanhMucSanPhamDaoImpl;
import com.dtaq.retail.dao.impl.DongSanPhamImpl;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.util.ExcelTemplate;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DongSanPhamControllerTest {

  @org.junit.jupiter.api.BeforeEach
  void datPhienKiemThu() {
    com.dtaq.retail.session.CurrentSession.set(new com.dtaq.retail.entity.NhanVien(
        "NV-TEST-PHIEN", "Nguyễn Kiểm Thử", "0900000009", null,
        java.time.LocalDate.of(2026, 1, 1), "kiemthu.phien", "matkhau-test",
        com.dtaq.retail.entity.LoaiQuyen.NHAN_VIEN_BAN_HANG, true));
  }

  @org.junit.jupiter.api.AfterEach
  void xoaPhienKiemThu() {
    com.dtaq.retail.session.CurrentSession.clear();
  }

  @Test
  @DisplayName("Chuẩn hóa đơn vị tính chỉ chấp nhận Chiếc hoặc Cái")
  void normalizeDonViTinh_chuanHoaDungQuyTac() {
    assertEquals("Chiếc", DongSanPhamController.normalizeDonViTinh("chiếc"));
    assertEquals("Chiếc", DongSanPhamController.normalizeDonViTinh("CHIẾC"));
    assertEquals("Cái", DongSanPhamController.normalizeDonViTinh("cái"));
    assertEquals("Cái", DongSanPhamController.normalizeDonViTinh("CAI"));
    assertNull(DongSanPhamController.normalizeDonViTinh("bộ"));
    assertNull(DongSanPhamController.normalizeDonViTinh("hộp"));
  }

  @Test
  @DisplayName("Nhập Excel dòng sản phẩm phải ghi thành công với dữ liệu hợp lệ")
  void nhapDanhSach_thanhCong() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    DanhMucSanPhamDao danhMucDao = new DanhMucSanPhamDaoImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamController controller = new DongSanPhamController(dongSanPhamDao, danhMucDao, ConnectDB.getDataSource());

    DanhMucSanPham danhMuc = danhMucDao.findAll().stream().findFirst().orElseThrow();
    File file = Files.createTempFile("dsp-import", ".xlsx").toFile();
    String ten = "Dòng nhập Excel " + System.nanoTime();
    ExcelTemplate.xuat(file, DongSanPhamController.specNhapDongSanPham(), List.<Object[]>of(new Object[] {
        ten,
        danhMuc.maDanhMuc(),
        "Chiếc",
        new BigDecimal("4500000"),
        new BigDecimal("10"),
        true
    }));

    DongSanPhamController.KetQua<DongSanPhamController.NhapKetQua> ketQua = controller.nhapDanhSach(file);
    assertTrue(ketQua.thanhCong(), ketQua.thongBao());
    assertEquals(1, ketQua.duLieu().soDong());
    file.delete();
  }


  @Test
  @DisplayName("Chỉ Quản lý đổi được giá bán và thuế suất của dòng sản phẩm (C-08)")
  void capNhat_chiQuanLyDoiDuocGiaThue() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    DanhMucSanPhamDao danhMucDao = new DanhMucSanPhamDaoImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamController controller =
        new DongSanPhamController(dongSanPhamDao, danhMucDao, ConnectDB.getDataSource());
    String maDanhMuc = danhMucDao.findAll().stream().findFirst().orElseThrow().maDanhMuc();
    BigDecimal gia = new BigDecimal("4500000");
    BigDecimal thue = new BigDecimal("10");

    DongSanPhamController.KetQua<DongSanPham> taoMoi = controller.themMoi(
        "Dòng kiểm thử C08 " + System.nanoTime(), maDanhMuc, "Chiếc", gia, thue, false);
    assertTrue(taoMoi.thanhCong(), taoMoi.thongBao());
    String ma = taoMoi.duLieu().maDongSP();
    try {
      CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
      assertFalse(controller.capNhat(
          ma, "Dòng kiểm thử C08", maDanhMuc, "Chiếc", new BigDecimal("1000"), thue, false)
          .thanhCong(), "Nhân viên bán hàng không được đổi giá");
      assertFalse(controller.capNhat(
          ma, "Dòng kiểm thử C08", maDanhMuc, "Chiếc", gia, new BigDecimal("0"), false)
          .thanhCong(), "Nhân viên bán hàng không được đổi thuế");
      assertTrue(controller.capNhat(
          ma, "Dòng kiểm thử C08 đổi tên", maDanhMuc, "Chiếc", gia, thue, false).thanhCong());
      assertEquals(0, gia.compareTo(
          dongSanPhamDao.findById(ma).orElseThrow().donGiaBanTruocThue()));

      CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
      assertFalse(controller.capNhat(
          ma, "Dòng kiểm thử C08", maDanhMuc, "Chiếc", new BigDecimal("5000000.5"), thue, false)
          .thanhCong(), "Giá lẻ phải bị từ chối (C-11)");
      assertTrue(controller.capNhat(
          ma, "Dòng kiểm thử C08", maDanhMuc, "Chiếc", new BigDecimal("5000000"), thue, false)
          .thanhCong());
    } finally {
      CurrentSession.clear();
      try (java.sql.Connection ketNoi = ConnectDB.getDataSource().getConnection();
          java.sql.PreparedStatement xoa = ketNoi.prepareStatement(
              "DELETE FROM \"DongSanPham\" WHERE \"maDongSP\" = ?")) {
        xoa.setString(1, ma);
        xoa.executeUpdate();
      }
    }
  }

  private static NhanVien nhanVien(LoaiQuyen loaiQuyen) {
    return new NhanVien(
        "NV-TEST-C08", "Nguyễn Kiểm Thử", "0900000001", null, LocalDate.of(2026, 1, 1),
        "kiemthu.c08", "matkhau-test", loaiQuyen, true);
  }
}
