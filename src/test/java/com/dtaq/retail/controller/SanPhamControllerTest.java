package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.impl.DongSanPhamImpl;
import com.dtaq.retail.dao.impl.NhaCungCapImpl;
import com.dtaq.retail.dao.impl.SanPhamImpl;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.util.ExcelTemplate;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử UC008: thêm máy mới và đồng bộ tồn kho. */
class SanPhamControllerTest {

  private final List<String> serialsCanXoa = new ArrayList<>();
  private final List<String> dongCanReset = new ArrayList<>();
  private final List<Integer> tonCu = new ArrayList<>();

  @AfterEach
  void tearDown() {
    if (!ConnectDB.isAvailable()) {
      return;
    }

    for (int i = 0; i < serialsCanXoa.size(); i++) {
      String serial = serialsCanXoa.get(i);
      String maDong = dongCanReset.get(i);
      int soLuongTon = tonCu.get(i);
      try (Connection connection = ConnectDB.getDataSource().getConnection();
          PreparedStatement deleteSanPham = connection.prepareStatement(
              "DELETE FROM \"SanPham\" WHERE \"soSerial\" = ?")) {
        deleteSanPham.setString(1, serial);
        deleteSanPham.executeUpdate();

        try (PreparedStatement updateTon = connection.prepareStatement(
            "UPDATE \"DongSanPham\" SET \"soLuongTon\" = ? WHERE \"maDongSP\" = ?")) {
          updateTon.setInt(1, soLuongTon);
          updateTon.setString(2, maDong);
          updateTon.executeUpdate();
        }
      } catch (Exception ignored) {
        // Dọn dẹp dữ liệu test theo cách an toàn.
      }
    }

    serialsCanXoa.clear();
    dongCanReset.clear();
    tonCu.clear();
  }

  @Test
  @DisplayName("Thêm máy mới thành công và tăng tồn kho của dòng sản phẩm")
  void themMay_thanhCong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(ConnectDB.getDataSource());
    SanPhamController controller = new SanPhamController(
        sanPhamDao, dongSanPhamDao, nhaCungCapDao, ConnectDB.getDataSource());

    DongSanPham dong = dongSanPhamDao.findAll().stream()
        .filter(DongSanPham::trangThaiKinhDoanh)
        .findFirst()
        .orElseThrow();
    NhaCungCap nhaCungCap = nhaCungCapDao.findAll().stream().findFirst().orElseThrow();

    String serial = "UC008-" + System.nanoTime();
    int tonBanDau = dongSanPhamDao.findById(dong.maDongSP()).orElseThrow().soLuongTon();

    SanPhamController.KetQua<SanPham> ketQua = controller.themMay(
        serial,
        nhaCungCap.maNCC(),
        dong.maDongSP(),
        new BigDecimal("12500000"),
        LocalDate.now(),
        TrangThaiSanPham.SAN_SANG_BAN);

    assertTrue(ketQua.thanhCong(), ketQua.thongBao());
    assertNotNull(ketQua.duLieu());
    assertEquals(serial, ketQua.duLieu().soSerial());

    DongSanPham sauKhiThem = dongSanPhamDao.findById(dong.maDongSP()).orElseThrow();
    assertEquals(tonBanDau + 1, sauKhiThem.soLuongTon());

    serialsCanXoa.add(serial);
    dongCanReset.add(dong.maDongSP());
    tonCu.add(tonBanDau);
  }

  @Test
  @DisplayName("Serial trùng phải bị từ chối và không tăng tồn kho")
  void themMay_loiSerialDaTonTai() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(ConnectDB.getDataSource());
    SanPhamController controller = new SanPhamController(
        sanPhamDao, dongSanPhamDao, nhaCungCapDao, ConnectDB.getDataSource());

    DongSanPham dong = dongSanPhamDao.findAll().stream()
        .filter(DongSanPham::trangThaiKinhDoanh)
        .findFirst()
        .orElseThrow();
    NhaCungCap nhaCungCap = nhaCungCapDao.findAll().stream().findFirst().orElseThrow();
    String serial = "UC008-DUPLICATE-" + System.nanoTime();

    SanPhamController.KetQua<SanPham> ketQua1 = controller.themMay(
        serial,
        nhaCungCap.maNCC(),
        dong.maDongSP(),
        new BigDecimal("15000000"),
        LocalDate.now(),
        TrangThaiSanPham.SAN_SANG_BAN);
    assertTrue(ketQua1.thanhCong(), ketQua1.thongBao());

    SanPhamController.KetQua<SanPham> ketQua2 = controller.themMay(
        serial,
        nhaCungCap.maNCC(),
        dong.maDongSP(),
        new BigDecimal("16000000"),
        LocalDate.now(),
        TrangThaiSanPham.SAN_SANG_BAN);

    assertFalse(ketQua2.thanhCong());
    assertTrue(ketQua2.thongBao().toLowerCase().contains("tồn tại")
        || ketQua2.thongBao().toLowerCase().contains("serial"));

    serialsCanXoa.add(serial);
    dongCanReset.add(dong.maDongSP());
    tonCu.add(dongSanPhamDao.findById(dong.maDongSP()).orElseThrow().soLuongTon() - 1);
  }

  @Test
  @DisplayName("Cập nhật máy hiện có phải giữ serial cố định và thay đổi thông tin hợp lệ")
  void capNhatMay_thanhCong() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(ConnectDB.getDataSource());
    SanPhamController controller = new SanPhamController(
        sanPhamDao, dongSanPhamDao, nhaCungCapDao, ConnectDB.getDataSource());

    // Tự tạo máy riêng cho test: không sửa máy có sẵn trong seed vì máy đầu tiên theo serial
    // (ELE-DR-0033) đã bán — ChiTietHoaDon tham chiếu (soSerial, maDongSP) nên đổi dòng sẽ vi phạm
    // khóa ngoại fk_chitiethoadon_sanpham — và để không ghi đè dữ liệu thật.
    List<DongSanPham> dongDangKinhDoanh = dongSanPhamDao.findAll().stream()
        .filter(DongSanPham::trangThaiKinhDoanh)
        .toList();
    Assumptions.assumeTrue(dongDangKinhDoanh.size() >= 2, "Cần ít nhất 2 dòng sản phẩm đang kinh doanh");
    DongSanPham dongGoc = dongDangKinhDoanh.get(0);
    DongSanPham dongMoi = dongDangKinhDoanh.get(1);
    NhaCungCap nhaCungCap = nhaCungCapDao.findAll().stream().findFirst().orElseThrow();
    int tonGocBanDau = dongSanPhamDao.findById(dongGoc.maDongSP()).orElseThrow().soLuongTon();
    int tonMoiBanDau = dongSanPhamDao.findById(dongMoi.maDongSP()).orElseThrow().soLuongTon();

    String serial = "UC008-SUA-" + System.nanoTime();
    SanPhamController.KetQua<SanPham> taoMoi = controller.themMay(
        serial,
        nhaCungCap.maNCC(),
        dongGoc.maDongSP(),
        new BigDecimal("15000000"),
        LocalDate.now(),
        TrangThaiSanPham.SAN_SANG_BAN);
    assertTrue(taoMoi.thanhCong(), taoMoi.thongBao());
    // Dọn sau test: xóa máy và trả tồn của cả hai dòng về như ban đầu.
    serialsCanXoa.add(serial);
    dongCanReset.add(dongMoi.maDongSP());
    tonCu.add(tonMoiBanDau);
    serialsCanXoa.add(serial);
    dongCanReset.add(dongGoc.maDongSP());
    tonCu.add(tonGocBanDau);
    SanPham sanPham = taoMoi.duLieu();

    BigDecimal giaMoi = new BigDecimal("18000000");
    LocalDate ngayMoi = LocalDate.now().minusDays(2);
    SanPhamController.KetQua<SanPham> ketQua = controller.capNhat(
        sanPham.soSerial(),
        nhaCungCap.maNCC(),
        dongMoi.maDongSP(),
        giaMoi,
        ngayMoi,
        sanPham.trangThai());

    assertTrue(ketQua.thanhCong(), ketQua.thongBao());
    assertEquals(sanPham.soSerial(), ketQua.duLieu().soSerial());
    assertEquals(dongMoi.maDongSP(), ketQua.duLieu().dongSanPham().maDongSP());
    assertEquals(giaMoi, ketQua.duLieu().giaNhap());
    assertEquals(ngayMoi, ketQua.duLieu().ngayNhapKhoBanDau());
    // Chuyển dòng thì tồn kho dịch theo: dòng cũ trở về như trước, dòng mới tăng 1.
    assertEquals(tonGocBanDau, dongSanPhamDao.findById(dongGoc.maDongSP()).orElseThrow().soLuongTon());
    assertEquals(tonMoiBanDau + 1, dongSanPhamDao.findById(dongMoi.maDongSP()).orElseThrow().soLuongTon());
  }

  @Test
  @DisplayName("Ngày nhập ở tương lai và mã dòng không tồn tại phải bị chặn")
  void themMay_loiValidateNhapLieu() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(ConnectDB.getDataSource());
    SanPhamController controller = new SanPhamController(
        sanPhamDao, dongSanPhamDao, nhaCungCapDao, ConnectDB.getDataSource());

    NhaCungCap nhaCungCap = nhaCungCapDao.findAll().stream().findFirst().orElseThrow();

    SanPhamController.KetQua<SanPham> dateSai = controller.themMay(
        "UC008-FUTURE-" + System.nanoTime(),
        nhaCungCap.maNCC(),
        "DSP-KHONG-TON-TAI",
        new BigDecimal("20000000"),
        LocalDate.now().plusDays(2),
        TrangThaiSanPham.SAN_SANG_BAN);
    assertFalse(dateSai.thanhCong());

    SanPhamController.KetQua<SanPham> dongSai = controller.themMay(
        "UC008-NO-DONG-" + System.nanoTime(),
        nhaCungCap.maNCC(),
        "DSP-KHONG-TON-TAI",
        new BigDecimal("20000000"),
        LocalDate.now(),
        TrangThaiSanPham.SAN_SANG_BAN);
    assertFalse(dongSai.thanhCong());
  }

  @Test
  @DisplayName("Thêm máy HANG_LOI không làm tăng số lượng tồn")
  void themMayHangLoiKhongTangTon() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(ConnectDB.getDataSource());
    SanPhamController controller = new SanPhamController(
        sanPhamDao, dongSanPhamDao, nhaCungCapDao, ConnectDB.getDataSource());

    DongSanPham dong = dongSanPhamDao.findAll().stream()
        .filter(DongSanPham::trangThaiKinhDoanh)
        .findFirst()
        .orElseThrow();
    NhaCungCap nhaCungCap = nhaCungCapDao.findAll().stream().findFirst().orElseThrow();

    String serial = "UC008-HANGLOI-" + System.nanoTime();
    int tonBanDau = dongSanPhamDao.findById(dong.maDongSP()).orElseThrow().soLuongTon();

    try {
      SanPhamController.KetQua<SanPham> ketQua = controller.themMay(
          serial,
          nhaCungCap.maNCC(),
          dong.maDongSP(),
          new BigDecimal("1000000"),
          LocalDate.now(),
          TrangThaiSanPham.HANG_LOI);

      assertTrue(ketQua.thanhCong(), ketQua.thongBao());
      assertNotNull(ketQua.duLieu());
      assertEquals(serial, ketQua.duLieu().soSerial());

      DongSanPham sauKhiThem = dongSanPhamDao.findById(dong.maDongSP()).orElseThrow();
      assertEquals(tonBanDau, sauKhiThem.soLuongTon(),
          "Máy HANG_LOI không được tính vào số lượng tồn (RB37)");
    } finally {
      serialsCanXoa.add(serial);
      dongCanReset.add(dong.maDongSP());
      tonCu.add(tonBanDau);
    }
  }

  @Test
  @DisplayName("Nhập Excel sản phẩm theo file mẫu phải thành công")
  void nhapDanhSach_thanhCong() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");

    SanPhamDao sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(ConnectDB.getDataSource());
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(ConnectDB.getDataSource());
    SanPhamController controller = new SanPhamController(
        sanPhamDao, dongSanPhamDao, nhaCungCapDao, ConnectDB.getDataSource());

    DongSanPham dong = dongSanPhamDao.findAll().stream()
        .filter(DongSanPham::trangThaiKinhDoanh)
        .findFirst()
        .orElseThrow();
    NhaCungCap nhaCungCap = nhaCungCapDao.findAll().stream().findFirst().orElseThrow();
    File file = Files.createTempFile("san-pham-import", ".xlsx").toFile();
    String serial = "UC008-IMPORT-" + System.nanoTime();

    ExcelTemplate.xuat(file, SanPhamController.specNhapSanPham(), List.<Object[]>of(new Object[] {
        serial,
        nhaCungCap.maNCC(),
        dong.maDongSP(),
        new BigDecimal("12000000"),
        LocalDate.now(),
        "SAN_SANG_BAN"
    }));

    SanPhamController.KetQua<SanPhamController.NhapKetQua> ketQua = controller.nhapDanhSach(file);
    assertTrue(ketQua.thanhCong(), ketQua.thongBao());
    assertEquals(1, ketQua.duLieu().soDong());
    file.delete();

    serialsCanXoa.add(serial);
    dongCanReset.add(dong.maDongSP());
    tonCu.add(dongSanPhamDao.findById(dong.maDongSP()).orElseThrow().soLuongTon() - 1);
  }
}
