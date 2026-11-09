package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.util.ExcelImporter.CotNhap;
import com.dtaq.retail.util.ExcelImporter.ImportError;
import com.dtaq.retail.util.ExcelImporter.ImportResult;
import com.dtaq.retail.util.ExcelImporter.ImportRow;
import com.dtaq.retail.util.ExcelImporter.ImportSpec;
import com.dtaq.retail.util.ExcelImporter.KieuDuLieu;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử bộ đọc Excel dùng chung: tạo workbook bằng POI rồi đọc lại qua {@link ExcelImporter}. */
@DisplayName("ExcelImporter - đọc và kiểm tra tệp .xlsx")
class ExcelImporterTest {

  private static final String[] TIEU_DE =
      new String[] {"Mã", "Số lượng", "Giá", "Ngày", "Kích hoạt", "Trạng thái"};

  @TempDir Path thuMucTam;

  @Test
  @DisplayName("Đọc đúng chuỗi, số nguyên, số thập phân, ngày, đúng/sai và enum")
  void docDungMoiKieuDuLieu() throws Exception {
    File tep =
        taoTep(
            "hop-le.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {
              "SP01", 10, new BigDecimal("1250000.5"), LocalDate.of(2026, 1, 5), true, "Mới"
            },
            new Object[] {"SP02", "20", "99.5", "2026-02-28", "Không", "Cũ"},
            new Object[] {"SP03", 30, null, "29/02/2024", "1", null});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertTrue(ketQua.hopLe(), "không được có lỗi: " + ketQua.loi());
    assertEquals(3, ketQua.dong().size());
    assertEquals(3, ketQua.tongSoDong());

    ImportRow dong1 = ketQua.dong().get(0);
    assertEquals(1, dong1.soDongExcel());
    assertEquals("SP01", dong1.giaTri().get("Mã"));
    assertEquals(10L, dong1.giaTri().get("Số lượng"));
    assertEquals(new BigDecimal("1250000.5"), dong1.giaTri().get("Giá"));
    assertEquals(LocalDate.of(2026, 1, 5), dong1.giaTri().get("Ngày"));
    assertEquals(Boolean.TRUE, dong1.giaTri().get("Kích hoạt"));
    assertEquals("Mới", dong1.giaTri().get("Trạng thái"));

    ImportRow dong2 = ketQua.dong().get(1);
    assertEquals(20L, dong2.giaTri().get("Số lượng"));
    assertEquals(LocalDate.of(2026, 2, 28), dong2.giaTri().get("Ngày"));
    assertEquals(Boolean.FALSE, dong2.giaTri().get("Kích hoạt"));

    ImportRow dong3 = ketQua.dong().get(2);
    assertEquals(LocalDate.of(2024, 2, 29), dong3.giaTri().get("Ngày"));
    assertEquals(Boolean.TRUE, dong3.giaTri().get("Kích hoạt"));
    assertNull(dong3.giaTri().get("Giá"));
    assertNull(dong3.giaTri().get("Trạng thái"));
  }

  @Test
  @DisplayName("Thiếu cột tiêu đề báo lỗi ở dòng 0 và không trả dòng dữ liệu")
  void thieuCotTieuDe() throws Exception {
    File tep =
        taoTep(
            "thieu-cot.xlsx",
            "DuLieu",
            new String[] {"Mã", "Số lượng"},
            new Object[] {"SP01", 10});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertTrue(ketQua.dong().isEmpty());
    assertEquals(4, ketQua.loi().size(), "thiếu 4 cột so với mô tả");
    ImportError loiDau = ketQua.loi().get(0);
    assertEquals(0, loiDau.soDongExcel(), "lỗi tiêu đề luôn ở dòng 0");
    assertEquals("Giá", loiDau.cot());
    assertEquals("THIEU_COT", loiDau.maLoi());
    assertTrue(loiDau.thongBao().contains("Thiếu cột"));
  }

  @Test
  @DisplayName("Sai kiểu số nguyên báo SAI_KIEU")
  void saiKieuSoNguyen() throws Exception {
    File tep =
        taoTep(
            "sai-so-nguyen.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"SP01", "abc", null, null, null, null});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertTrue(ketQua.dong().isEmpty());
    ImportError loi = ketQua.loi().get(0);
    assertEquals(1, loi.soDongExcel());
    assertEquals("Số lượng", loi.cot());
    assertEquals("SAI_KIEU", loi.maLoi());
  }

  @Test
  @DisplayName("Ngày không tồn tại hoặc sai định dạng bị từ chối")
  void saiNgay() throws Exception {
    File tep =
        taoTep(
            "sai-ngay.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"SP01", 1, null, "31/02/2026", null, null});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertEquals("Ngày", ketQua.loi().get(0).cot());
    assertEquals("SAI_KIEU", ketQua.loi().get(0).maLoi());
  }

  @Test
  @DisplayName("Giá trị đúng/sai không hợp lệ bị từ chối")
  void saiLogic() throws Exception {
    File tep =
        taoTep(
            "sai-logic.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"SP01", 1, null, null, "Có lẽ", null});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertEquals("Kích hoạt", ketQua.loi().get(0).cot());
    assertEquals("SAI_KIEU", ketQua.loi().get(0).maLoi());
  }

  @Test
  @DisplayName("Giá trị enum ngoài danh sách cho phép báo SAI_GIA_TRI")
  void saiEnum() throws Exception {
    File tep =
        taoTep(
            "sai-enum.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"SP01", 1, null, null, null, "Khác"});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertEquals("Trạng thái", ketQua.loi().get(0).cot());
    assertEquals("SAI_GIA_TRI", ketQua.loi().get(0).maLoi());
    assertTrue(ketQua.loi().get(0).thongBao().contains("danh sách cho phép"));
  }

  @Test
  @DisplayName("Ô bắt buộc để trống báo THIEU_GIA_TRI")
  void thieuGiaTriBatBuoc() throws Exception {
    File tep =
        taoTep(
            "thieu-gia-tri.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"", 5, null, null, null, null});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    ImportError loi = ketQua.loi().get(0);
    assertEquals(1, loi.soDongExcel());
    assertEquals("Mã", loi.cot());
    assertEquals("THIEU_GIA_TRI", loi.maLoi());
  }

  @Test
  @DisplayName("Dòng trống hoàn toàn bị bỏ qua và không tính vào tổng số dòng")
  void boQuaDongTrong() throws Exception {
    File tep =
        taoTep(
            "dong-trong.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"SP01", 1, null, null, null, null},
            new Object[] {null, null, null, null, null, null},
            new Object[] {"SP02", 2, null, null, null, null});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertTrue(ketQua.hopLe());
    assertEquals(2, ketQua.dong().size());
    assertEquals(2, ketQua.tongSoDong());
    assertEquals(3, ketQua.dong().get(1).soDongExcel(), "dòng Excel thật của bản ghi thứ hai");
  }

  @Test
  @DisplayName("Cột thừa ngoài mô tả bị bỏ qua")
  void boQuaCotThua() throws Exception {
    File tep =
        taoTep(
            "cot-thua.xlsx",
            "DuLieu",
            new String[] {"Mã", "Số lượng", "Giá", "Ngày", "Kích hoạt", "Trạng thái", "Ghi chú"},
            new Object[] {"SP01", 1, null, null, null, null, "không dùng"});

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertTrue(ketQua.hopLe());
    assertEquals(1, ketQua.dong().size());
    assertEquals(6, ketQua.dong().get(0).giaTri().size(), "chỉ giữ các cột trong mô tả");
    assertFalse(ketQua.dong().get(0).giaTri().containsKey("Ghi chú"));
  }

  @Test
  @DisplayName("Tệp rỗng, sai phần mở rộng, quá dung lượng và thiếu sheet đều trả lỗi cấp tệp")
  void loiCapTep() throws Exception {
    File tepRong = thuMucTam.resolve("rong.xlsx").toFile();
    Files.createFile(tepRong.toPath());
    ImportResult rong = ExcelImporter.doc(tepRong, specMacDinh());
    assertEquals("TEP_RONG", rong.loi().get(0).maLoi());
    assertEquals(0, rong.loi().get(0).soDongExcel());

    File tepXls = taoTep("du-lieu.xls", "DuLieu", TIEU_DE);
    ImportResult saiDuoi = ExcelImporter.doc(tepXls, specMacDinh());
    assertEquals("TEP_KHONG_HOP_LE", saiDuoi.loi().get(0).maLoi());

    File tepLon = thuMucTam.resolve("qua-lon.xlsx").toFile();
    try (RandomAccessFile raf = new RandomAccessFile(tepLon, "rw")) {
      raf.setLength(10L * 1024 * 1024 + 1);
    }
    ImportResult quaLon = ExcelImporter.doc(tepLon, specMacDinh());
    assertEquals("TEP_QUA_LON", quaLon.loi().get(0).maLoi());

    File tepThieuSheet = taoTep("sai-sheet.xlsx", "Khac", TIEU_DE);
    ImportResult saiSheet = ExcelImporter.doc(tepThieuSheet, specMacDinh());
    assertEquals("SHEET_KHONG_TON_TAI", saiSheet.loi().get(0).maLoi());
  }

  @Test
  @DisplayName("Sheet không có dòng tiêu đề báo THIEU_TIEU_DE")
  void thieuDongTieuDe() throws Exception {
    File tep = thuMucTam.resolve("khong-tieu-de.xlsx").toFile();
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        OutputStream out = Files.newOutputStream(tep.toPath())) {
      workbook.createSheet("DuLieu");
      workbook.write(out);
    }

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertEquals("THIEU_TIEU_DE", ketQua.loi().get(0).maLoi());
    assertEquals(0, ketQua.loi().get(0).soDongExcel());
  }

  @Test
  @DisplayName("Vượt giới hạn dòng của mô tả báo QUA_NHIEU_DONG và dừng đọc")
  void vuotGioiHanDong() throws Exception {
    ImportSpec spec =
        new ImportSpec("DuLieu", specMacDinh().cot(), 2);
    File tep =
        taoTep(
            "qua-dong.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"SP01", 1, null, null, null, null},
            new Object[] {"SP02", 2, null, null, null, null},
            new Object[] {"SP03", 3, null, null, null, null});

    ImportResult ketQua = ExcelImporter.doc(tep, spec);

    assertFalse(ketQua.hopLe());
    assertEquals(2, ketQua.dong().size());
    assertEquals(3, ketQua.tongSoDong());
    assertTrue(
        ketQua.loi().stream().anyMatch(loi -> loi.maLoi().equals("QUA_NHIEU_DONG")),
        "phải có lỗi QUA_NHIEU_DONG");
  }

  @Test
  @DisplayName("Chỉ thu thập tối đa 200 lỗi")
  void gioiHanSoLoi() throws Exception {
    Object[][] dong = new Object[250][];
    for (int i = 0; i < dong.length; i++) {
      dong[i] = new Object[] {"SP" + i, "x", null, null, null, null};
    }
    File tep = taoTep("nhieu-loi.xlsx", "DuLieu", TIEU_DE, dong);

    ImportResult ketQua = ExcelImporter.doc(tep, specMacDinh());

    assertFalse(ketQua.hopLe());
    assertEquals(200, ketQua.loi().size());
    assertEquals(250, ketQua.tongSoDong());
    assertTrue(ketQua.dong().isEmpty());
  }

  @Test
  @DisplayName("Xuất báo cáo lỗi ra .xlsx với cột Dòng/Cột/Mã lỗi/Thông báo")
  void xuatBaoCaoLoi() throws Exception {
    File tepLoi =
        taoTep(
            "co-loi.xlsx",
            "DuLieu",
            TIEU_DE,
            new Object[] {"", "abc", null, null, null, null});
    ImportResult ketQua = ExcelImporter.doc(tepLoi, specMacDinh());
    assertEquals(2, ketQua.loi().size(), "thiếu mã và sai số lượng");

    File tepBaoCao = thuMucTam.resolve("bao-cao-loi.xlsx").toFile();
    ExcelImporter.xuatBaoCaoLoi(tepBaoCao, ketQua);

    assertTrue(tepBaoCao.exists());
    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(tepBaoCao.toPath()))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(1);
      assertEquals("Dòng", header.getCell(0).getStringCellValue());
      assertEquals("Cột", header.getCell(1).getStringCellValue());
      assertEquals("Mã lỗi", header.getCell(2).getStringCellValue());
      assertEquals("Thông báo", header.getCell(3).getStringCellValue());

      Row dongDau = sheet.getRow(2);
      assertEquals(2, dongDau.getCell(0).getNumericCellValue(), "hiển thị số dòng 1-based");
      assertEquals("Mã", dongDau.getCell(1).getStringCellValue());
      assertEquals("THIEU_GIA_TRI", dongDau.getCell(2).getStringCellValue());
      assertFalse(dongDau.getCell(3).getStringCellValue().isBlank());
    }
  }

  @Test
  @DisplayName("Tham số null hoặc mô tả rỗng bị từ chối bằng IllegalArgumentException")
  void tuChoiThamSoKhongHopLe() {
    File tep = thuMucTam.resolve("x.xlsx").toFile();
    assertThrows(IllegalArgumentException.class, () -> ExcelImporter.doc(null, specMacDinh()));
    assertThrows(IllegalArgumentException.class, () -> ExcelImporter.doc(tep, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExcelImporter.doc(tep, new ImportSpec("DuLieu", List.of(), 0)));
  }

  private static ImportSpec specMacDinh() {
    return new ImportSpec(
        "DuLieu",
        List.of(
            new CotNhap("Mã", KieuDuLieu.CHUOI, true, List.of()),
            new CotNhap("Số lượng", KieuDuLieu.SO_NGUYEN, true, List.of()),
            new CotNhap("Giá", KieuDuLieu.SO_THAP_PHAN, false, List.of()),
            new CotNhap("Ngày", KieuDuLieu.NGAY, false, List.of()),
            new CotNhap("Kích hoạt", KieuDuLieu.LOGIC, false, List.of()),
            new CotNhap("Trạng thái", KieuDuLieu.ENUM, false, List.of("Mới", "Cũ"))),
        5_000);
  }

  private File taoTep(String ten, String tenSheet, String[] tieuDe, Object[]... dong)
      throws IOException {
    File tep = thuMucTam.resolve(ten).toFile();
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        OutputStream out = Files.newOutputStream(tep.toPath())) {
      Sheet sheet = workbook.createSheet(tenSheet);
      Row header = sheet.createRow(0);
      for (int cot = 0; cot < tieuDe.length; cot++) {
        header.createCell(cot).setCellValue(tieuDe[cot]);
      }
      for (int chiSo = 0; chiSo < dong.length; chiSo++) {
        Row row = sheet.createRow(chiSo + 1);
        Object[] giaTri = dong[chiSo];
        for (int cot = 0; cot < giaTri.length; cot++) {
          ghiGiaTri(row.createCell(cot), giaTri[cot], workbook);
        }
      }
      workbook.write(out);
    }
    return tep;
  }

  private static void ghiGiaTri(Cell o, Object giaTri, Workbook workbook) {
    if (giaTri == null) {
      return;
    }
    if (giaTri instanceof LocalDate ngay) {
      o.setCellValue(LocalDateTime.of(ngay, LocalTime.MIDNIGHT));
      CellStyle kieuNgay = workbook.createCellStyle();
      kieuNgay.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy"));
      o.setCellStyle(kieuNgay);
    } else if (giaTri instanceof Number so) {
      o.setCellValue(so.doubleValue());
    } else if (giaTri instanceof Boolean co) {
      o.setCellValue(co);
    } else {
      o.setCellValue(String.valueOf(giaTri));
    }
  }
}
