package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.exception.ExcelExportException;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử bộ xuất báo cáo Excel: ghi tệp rồi đọc lại bằng chính POI. */
@DisplayName("ExcelExporter - xuất báo cáo .xlsx")
class ExcelExporterTest {

  private static final List<String> TIEU_DE_COT =
      List.of("Dòng SP", "Số lượng", "Doanh số", "Ngày nhập");

  @TempDir Path thuMucTam;

  @Test
  @DisplayName("Xuất tệp rồi đọc lại đúng tiêu đề, header, số và ngày")
  void xuatTepDocLaiDuoc() throws Exception {
    File tep = thuMucTam.resolve("bao-cao.xlsx").toFile();
    List<Object[]> dongDuLieu =
        List.of(
            new Object[] {
              "Tủ lạnh", 2L, new BigDecimal("1250000"), LocalDate.of(2026, 1, 5)
            },
            new Object[] {"Máy giặt", null, BigDecimal.ZERO, null});

    ExcelExporter.export(tep, "Doanh thu tháng 01/2026", TIEU_DE_COT, dongDuLieu);

    assertTrue(tep.exists(), "tệp xuất phải tồn tại");
    assertTrue(tep.length() > 0, "tệp xuất phải có nội dung");

    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(tep.toPath()))) {
      assertEquals(1, workbook.getNumberOfSheets());
      Sheet sheet = workbook.getSheetAt(0);
      assertEquals("Doanh thu tháng 012026", sheet.getSheetName(), "tên sheet đã bỏ ký tự cấm");
      assertTrue(sheet.getSheetName().length() <= 31, "tên sheet không quá 31 ký tự");

      Row dongTieuDe = sheet.getRow(0);
      assertEquals("Doanh thu tháng 01/2026", dongTieuDe.getCell(0).getStringCellValue());
      assertEquals(1, sheet.getNumMergedRegions(), "chỉ có một vùng gộp cho dòng tiêu đề");
      CellRangeAddress vungGop = sheet.getMergedRegion(0);
      assertEquals(0, vungGop.getFirstRow());
      assertEquals(0, vungGop.getFirstColumn());
      assertEquals(0, vungGop.getLastRow());
      assertEquals(TIEU_DE_COT.size() - 1, vungGop.getLastColumn());

      Row dongHeader = sheet.getRow(1);
      for (int cot = 0; cot < TIEU_DE_COT.size(); cot++) {
        assertEquals(TIEU_DE_COT.get(cot), dongHeader.getCell(cot).getStringCellValue());
      }

      Row dongSo = sheet.getRow(2);
      assertEquals("Tủ lạnh", dongSo.getCell(0).getStringCellValue());

      Cell soLuong = dongSo.getCell(1);
      assertEquals(CellType.NUMERIC, soLuong.getCellType());
      assertEquals(2, soLuong.getNumericCellValue());

      Cell doanhSo = dongSo.getCell(2);
      assertEquals(CellType.NUMERIC, doanhSo.getCellType());
      assertEquals(1250000, doanhSo.getNumericCellValue());
      assertEquals("#,##0", doanhSo.getCellStyle().getDataFormatString());

      Cell ngayNhap = dongSo.getCell(3);
      assertTrue(DateUtil.isCellDateFormatted(ngayNhap), "ô ngày phải là ô kiểu ngày");
      assertEquals("dd/MM/yyyy", ngayNhap.getCellStyle().getDataFormatString());
      assertEquals(LocalDate.of(2026, 1, 5), ngayNhap.getLocalDateTimeCellValue().toLocalDate());

      Row dongTrong = sheet.getRow(3);
      assertEquals(CellType.BLANK, dongTrong.getCell(1).getCellType(), "giá trị null là ô trống");
      assertEquals(3, sheet.getLastRowNum(), "tiêu đề + header + 2 dòng dữ liệu");
    }
  }

  @Test
  @DisplayName("Tiêu đề dài và có ký tự cấm được rút gọn thành tên sheet hợp lệ")
  void lamTronTenSheet() throws Exception {
    File tep = thuMucTam.resolve("bao-cao-dai.xlsx").toFile();
    String tieuDeDai = "Báo cáo [doanh thu] tháng 01/2026: chi tiết";

    ExcelExporter.export(tep, tieuDeDai, List.of("Cột"), List.<Object[]>of(new Object[] {"x"}));

    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(tep.toPath()))) {
      String tenSheet = workbook.getSheetAt(0).getSheetName();
      assertTrue(tenSheet.length() <= 31, "tên sheet phải không quá 31 ký tự");
      assertFalse(tenSheet.matches(".*[\\[\\]:*?/\\\\].*"), "tên sheet còn ký tự cấm: " + tenSheet);
    }
  }

  @Test
  @DisplayName("Cột tiền 10 chữ số đủ rộng cho dạng nhóm hàng nghìn")
  void doRongCotDuChoTienNhomNghin() throws Exception {
    File tep = thuMucTam.resolve("bao-cao-tien-lon.xlsx").toFile();

    ExcelExporter.export(
        tep,
        "Doanh thu",
        List.of("Dòng SP", "Doanh số"),
        List.<Object[]>of(new Object[] {"Tủ lạnh", new BigDecimal("1000000000")}));

    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(tep.toPath()))) {
      Sheet sheet = workbook.getSheetAt(0);
      int cotTien = 1;
      int rong = sheet.getColumnWidth(cotTien) / 256;
      assertTrue(
          rong >= 13,
          "độ rộng cột tiền phải tính theo dạng nhóm hàng nghìn (1.000.000.000): " + rong);
    }
  }

  @Test
  @DisplayName("Tham số không hợp lệ bị từ chối bằng IllegalArgumentException")
  void tuChoiThamSoKhongHopLe() {
    File tep = thuMucTam.resolve("bao-cao.xlsx").toFile();

    assertThrows(
        IllegalArgumentException.class,
        () -> ExcelExporter.export(null, "Tiêu đề", TIEU_DE_COT, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExcelExporter.export(tep, "Tiêu đề", List.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ExcelExporter.export(
                tep, "Tiêu đề", TIEU_DE_COT, List.<Object[]>of(new Object[] {"Tủ lạnh", 2L})));
  }

  @Test
  @DisplayName("Lỗi ghi tệp được bọc thành ExcelExportException")
  void loiGhiTepThiNemExcelExportException() {
    File tep = thuMucTam.resolve("khong-ton-tai/tep.xlsx").toFile();

    ExcelExportException loi =
        assertThrows(
            ExcelExportException.class,
            () ->
                ExcelExporter.export(
                    tep,
                    "Doanh thu",
                    TIEU_DE_COT,
                    List.<Object[]>of(
                        new Object[] {
                          "Tủ lạnh", 1L, BigDecimal.ONE, LocalDate.of(2026, 1, 5)
                        })));

    assertEquals("Không thể ghi tệp Excel: tep.xlsx", loi.getMessage());
    assertTrue(loi.getCause() instanceof java.io.IOException, "phải giữ nguyên nguyên nhân gốc");
  }
}
