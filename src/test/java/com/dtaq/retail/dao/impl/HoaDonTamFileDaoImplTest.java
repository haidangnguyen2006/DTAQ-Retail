package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.exception.DataAccessException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử {@link HoaDonTamFileDaoImpl} trên thư mục tạm của hệ thống tệp. */
class HoaDonTamFileDaoImplTest {

  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Clock DONG_HO =
      Clock.fixed(Instant.parse("2026-11-03T07:05:12Z"), MUI_GIO);
  private static final LocalDateTime LUC_LUU_DAU = LocalDateTime.parse("2026-11-03T14:05:12");

  private static final List<HoaDonTamDao.DongTam> HAI_DONG = List.of(
      new HoaDonTamDao.DongTam(
          "DSP-2026-000001", "Máy giặt Samsung Inverter 10kg", 3,
          new BigDecimal("9800000"), new BigDecimal("10")),
      new HoaDonTamDao.DongTam(
          "DSP-2026-000002", "Tủ lạnh", 1,
          new BigDecimal("5000000"), new BigDecimal("8")));

  @TempDir
  Path thuMuc;

  private HoaDonTamFileDaoImpl dao;

  @BeforeEach
  void setUp() {
    dao = new HoaDonTamFileDaoImpl(thuMuc, DONG_HO);
  }

  @Test
  @DisplayName("Lưu hóa đơn tạm sinh mã mới và đọc lại đúng dòng sản phẩm cùng số lượng")
  void luuSinhMaVaDocLai() {
    HoaDonTamDao.HoaDonTam daLuu = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));

    assertEquals("HDT-2026-000001", daLuu.maTam());
    assertEquals(LUC_LUU_DAU, daLuu.luuLuc());
    assertTrue(Files.exists(thuMuc.resolve("HDT-2026-000001.properties")),
        "tệp hóa đơn tạm phải được tạo trong thư mục đích");

    HoaDonTamDao.HoaDonTam docLai = dao.findById("HDT-2026-000001").orElseThrow();
    assertEquals(daLuu, docLai);
    assertEquals("Nguyễn Văn A", docLai.tenNhanVien());
    assertEquals("Trần Thị B", docLai.tenKhachHang());
    assertEquals("0901234567", docLai.sdtKhachHang());
    assertEquals(2, docLai.dongHang().size());
    assertEquals("DSP-2026-000001", docLai.dongHang().get(0).maDongSP());
    assertEquals("Máy giặt Samsung Inverter 10kg", docLai.dongHang().get(0).tenDongSP());
    assertEquals(3, docLai.dongHang().get(0).soLuong());
    assertEquals("DSP-2026-000002", docLai.dongHang().get(1).maDongSP());
    assertEquals(1, docLai.dongHang().get(1).soLuong());
    assertEquals(0, new BigDecimal("9800000").compareTo(
        docLai.dongHang().get(0).donGiaTruocThue()));
    assertEquals(new BigDecimal("9800000").scale(),
        docLai.dongHang().get(0).donGiaTruocThue().scale(),
        "độ chính xác của BigDecimal phải giữ nguyên sau khi đọc lại");
  }

  @Test
  @DisplayName("Lưu lần hai với cùng mã tạm ghi đè tệp cũ")
  void luuLanHaiGhiDeCungMa() {
    HoaDonTamDao.HoaDonTam lanDau = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));

    Clock dongHoSau = Clock.fixed(Instant.parse("2026-11-03T07:06:12Z"), MUI_GIO);
    HoaDonTamFileDaoImpl daoSau = new HoaDonTamFileDaoImpl(thuMuc, dongHoSau);
    HoaDonTamDao.HoaDonTam lanHai = daoSau.luu(banTam(lanDau.maTam(), "NV-2026-000002", HAI_DONG));

    assertEquals(lanDau.maTam(), lanHai.maTam());
    assertEquals(LocalDateTime.parse("2026-11-03T14:06:12"), lanHai.luuLuc());
    assertEquals(1, daoSau.findByNhanVien("NV-2026-000002").size());
    assertEquals(lanHai.luuLuc(), daoSau.findById(lanDau.maTam()).orElseThrow().luuLuc());
  }

  @Test
  @DisplayName("Liệt kê hóa đơn tạm của nhân viên theo thứ tự mới nhất trước")
  void findByNhanVienMoiNhatTruoc() {
    HoaDonTamDao.HoaDonTam cu = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));

    Clock dongHoMoi = Clock.fixed(Instant.parse("2026-11-03T07:10:00Z"), MUI_GIO);
    HoaDonTamFileDaoImpl daoMoi = new HoaDonTamFileDaoImpl(thuMuc, dongHoMoi);
    HoaDonTamDao.HoaDonTam moi = daoMoi.luu(banTam(null, "NV-2026-000002", HAI_DONG));
    daoMoi.luu(banTam(null, "NV-2026-000003", HAI_DONG));

    List<HoaDonTamDao.HoaDonTam> ketQua = daoMoi.findByNhanVien("NV-2026-000002");
    assertEquals(2, ketQua.size());
    assertEquals(moi.maTam(), ketQua.get(0).maTam());
    assertEquals(cu.maTam(), ketQua.get(1).maTam());
  }

  @Test
  @DisplayName("Xóa hóa đơn tạm gỡ bỏ tệp tương ứng")
  void xoaXoaTep() {
    HoaDonTamDao.HoaDonTam daLuu = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));
    Path tep = thuMuc.resolve(daLuu.maTam() + ".properties");
    assertTrue(Files.exists(tep));

    dao.xoa(daLuu.maTam());

    assertFalse(Files.exists(tep));
    assertTrue(dao.findById(daLuu.maTam()).isEmpty());
    dao.xoa("HDT-2026-999999");
  }

  @Test
  @DisplayName("Tệp hỏng bị bỏ qua khi liệt kê hóa đơn tạm")
  void tephongBiBoQua() throws IOException {
    HoaDonTamDao.HoaDonTam hopLe = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));
    Files.writeString(
        thuMuc.resolve("HDT-2026-000099.properties"),
        "maTam=HDT-2026-000099\nsoDong=khong-phai-so\n",
        StandardCharsets.UTF_8);

    List<HoaDonTamDao.HoaDonTam> ketQua = dao.findByNhanVien("NV-2026-000002");

    assertEquals(1, ketQua.size());
    assertEquals(hopLe.maTam(), ketQua.get(0).maTam());
  }

  @Test
  @DisplayName("Tệp hóa đơn tạm định dạng cũ (lưu theo số serial) bị bỏ qua khi liệt kê")
  void boQuaTepDinhDangCu() throws IOException {
    Files.writeString(
        thuMuc.resolve("HDT-2026-000099.properties"),
        "maTam=HDT-2026-000099\n"
            + "maNV=NV-2026-000002\n"
            + "tenNhanVien=Nguyễn Văn A\n"
            + "luuLuc=2026-11-03T14:05:12\n"
            + "soDong=1\n"
            + "dong.0.soSerial=SP-2026-000021\n"
            + "dong.0.tenDongSP=Máy giặt\n"
            + "dong.0.soLuong=1\n"
            + "dong.0.donGiaTruocThue=9800000\n"
            + "dong.0.thueSuat=10\n",
        StandardCharsets.UTF_8);

    List<HoaDonTamDao.HoaDonTam> ketQua = dao.findByNhanVien("NV-2026-000002");

    assertTrue(ketQua.isEmpty(), "tệp định dạng cũ phải bị bỏ qua, không làm hỏng danh sách");
    assertThrows(DataAccessException.class, () -> dao.findById("HDT-2026-000099"));
  }

  @Test
  @DisplayName("Lưu hóa đơn tạm thiếu mã nhân viên bị từ chối")
  void luuThieuMaNhanVienBiTuChoi() {
    assertThrows(IllegalArgumentException.class,
        () -> dao.luu(banTam(null, "   ", HAI_DONG)));
  }

  @Test
  @DisplayName("Lưu hóa đơn tạm thiếu mã dòng sản phẩm bị từ chối")
  void luuThieuMaDongBiTuChoi() {
    List<HoaDonTamDao.DongTam> thieuMaDong = List.of(
        new HoaDonTamDao.DongTam(
            "  ", "Máy giặt", 1, new BigDecimal("9800000"), new BigDecimal("10")));

    IllegalArgumentException loi = assertThrows(IllegalArgumentException.class,
        () -> dao.luu(banTam(null, "NV-2026-000002", thieuMaDong)));

    assertTrue(loi.getMessage().contains("mã dòng sản phẩm"));
  }

  @Test
  @DisplayName("Lưu hóa đơn tạm không có dòng hàng vẫn đọc lại được")
  void luuKhongCoDongHang() {
    HoaDonTamDao.HoaDonTam daLuu = dao.luu(banTam(null, "NV-2026-000002", List.of()));

    HoaDonTamDao.HoaDonTam docLai = dao.findById(daLuu.maTam()).orElseThrow();
    assertEquals(0, docLai.dongHang().size());
    assertEquals(daLuu, docLai);
  }

  @Test
  @DisplayName("Mã hóa đơn tạm tăng dần theo từng lần lưu")
  void maTangDan() {
    HoaDonTamDao.HoaDonTam thuNhat = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));
    HoaDonTamDao.HoaDonTam thuHai = dao.luu(banTam(null, "NV-2026-000002", HAI_DONG));

    assertEquals("HDT-2026-000001", thuNhat.maTam());
    assertEquals("HDT-2026-000002", thuHai.maTam());
  }

  private static HoaDonTamDao.HoaDonTam banTam(
      String maTam, String maNV, List<HoaDonTamDao.DongTam> dongHang) {
    return new HoaDonTamDao.HoaDonTam(
        maTam, maNV, "Nguyễn Văn A",
        "KH-2026-000009", "Trần Thị B", "0901234567",
        null, dongHang);
  }
}
