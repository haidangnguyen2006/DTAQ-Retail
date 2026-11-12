package com.dtaq.retail.controller;

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
}
