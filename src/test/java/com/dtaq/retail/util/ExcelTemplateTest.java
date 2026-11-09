package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.exception.ExcelExportException;
import com.dtaq.retail.util.ExcelImporter.CotNhap;
import com.dtaq.retail.util.ExcelImporter.ImportResult;
import com.dtaq.retail.util.ExcelImporter.ImportSpec;
import com.dtaq.retail.util.ExcelImporter.KieuDuLieu;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm thử bộ sinh tệp Excel mẫu và vòng đọc lại bằng {@link ExcelImporter}. */
@DisplayName("ExcelTemplate - sinh tệp mẫu .xlsx")
class ExcelTemplateTest {

  @TempDir Path thuMucTam;

  @Test
  @DisplayName("Tạo sheet DuLieu có tiêu đề in đậm, cố định dòng và định dạng ngày/số")
  void taoSheetDuLieuDungDinhDang() throws Exception {
    File tep = thuMucTam.resolve("mau.xlsx").toFile();
    ExcelTemplate.xuat(tep, specMau(), viDuMau());

    assertTrue(tep.exists());
    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(tep.toPath()))) {
      assertEquals(2, workbook.getNumberOfSheets());
      Sheet duLieu = workbook.getSheet("DuLieu");
      assertTrue(duLieu != null, "phải có sheet DuLieu");

      Row header = duLieu.getRow(0);
      for (int cot = 0; cot < specMau().cot().size(); cot++) {
        assertEquals(specMau().cot().get(cot).tieuDe(), header.getCell(cot).getStringCellValue());
        Font font =
            workbook.getFontAt(header.getCell(cot).getCellStyle().getFontIndex());
        assertTrue(font.getBold(), "tiêu đề cột phải in đậm");
      }
      assertTrue(duLieu.getPaneInformation() != null, "phải có khung cố định");
      assertTrue(duLieu.getPaneInformation().isFreezePane(), "dòng tiêu đề phải được cố định");
      assertEquals(1, duLieu.getPaneInformation().getHorizontalSplitPosition());

      Row dong = duLieu.getRow(1);
      Cell ngay = dong.getCell(3);
      assertTrue(DateUtil.isCellDateFormatted(ngay), "ô ngày phải là ô ngày");
      assertEquals("dd/MM/yyyy", ngay.getCellStyle().getDataFormatString());
      assertEquals(LocalDate.of(2026, 1, 5), ngay.getLocalDateTimeCellValue().toLocalDate());

      Cell soNguyen = dong.getCell(1);
      assertEquals(CellType.NUMERIC, soNguyen.getCellType());
      assertEquals("#,##0", soNguyen.getCellStyle().getDataFormatString());

      Cell soThapPhan = dong.getCell(2);
      assertEquals(CellType.NUMERIC, soThapPhan.getCellType());
      assertEquals("#,##0.00", soThapPhan.getCellStyle().getDataFormatString());

      assertEquals("Có", dong.getCell(4).getStringCellValue(), "đúng/sai ghi dạng Có/Không");
    }
  }

  @Test
  @DisplayName("Sheet HuongDan mô tả đủ cột, kiểu, bắt buộc và giá trị cho phép")
  void taoSheetHuongDan() throws Exception {
    File tep = thuMucTam.resolve("mau-huong-dan.xlsx").toFile();
    ExcelTemplate.xuat(tep, specMau(), viDuMau());

    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(tep.toPath()))) {
      Sheet huongDan = workbook.getSheet("HuongDan");
      assertTrue(huongDan != null, "phải có sheet HuongDan");
      assertEquals(specMau().cot().size() + 1, huongDan.getLastRowNum() + 1);

      assertEquals("Cột", huongDan.getRow(0).getCell(0).getStringCellValue());
      assertEquals("Kiểu dữ liệu", huongDan.getRow(0).getCell(1).getStringCellValue());
      assertEquals("Bắt buộc", huongDan.getRow(0).getCell(2).getStringCellValue());
      assertEquals("Giá trị cho phép", huongDan.getRow(0).getCell(3).getStringCellValue());

      Row dongEnum = huongDan.getRow(specMau().cot().size());
      assertEquals("Trạng thái", dongEnum.getCell(0).getStringCellValue());
      assertEquals("ENUM", dongEnum.getCell(1).getStringCellValue());
      assertEquals("Không", dongEnum.getCell(2).getStringCellValue());
      assertEquals("Mới, Cũ", dongEnum.getCell(3).getStringCellValue());
      assertFalse(dongEnum.getCell(4).getStringCellValue().isBlank());
    }
  }

  @Test
  @DisplayName("Tệp mẫu đọc lại được bằng ExcelImporter với cùng mô tả")
  void docLaiTepMau() throws Exception {
    File tep = thuMucTam.resolve("mau-doc-lai.xlsx").toFile();
    ExcelTemplate.xuat(tep, specMau(), viDuMau());

    ImportResult ketQua = ExcelImporter.doc(tep, specMau());

    assertTrue(ketQua.hopLe(), "tệp mẫu phải hợp lệ: " + ketQua.loi());
    assertEquals(1, ketQua.dong().size());
    assertEquals("SP01", ketQua.dong().get(0).giaTri().get("Mã"));
    assertEquals(10L, ketQua.dong().get(0).giaTri().get("Số lượng"));
    assertEquals(
        0,
        new BigDecimal("1250000.5")
            .compareTo((BigDecimal) ketQua.dong().get(0).giaTri().get("Giá")));
    assertEquals(LocalDate.of(2026, 1, 5), ketQua.dong().get(0).giaTri().get("Ngày"));
    assertEquals(Boolean.TRUE, ketQua.dong().get(0).giaTri().get("Kích hoạt"));
    assertEquals("Mới", ketQua.dong().get(0).giaTri().get("Trạng thái"));
  }

  @Test
  @DisplayName("Không có dòng mẫu thì tệp chỉ gồm tiêu đề và đọc lại không lỗi")
  void khongCoDongMau() throws Exception {
    File tep = thuMucTam.resolve("mau-rong.xlsx").toFile();
    ExcelTemplate.xuat(tep, specMau(), null);

    ImportResult ketQua = ExcelImporter.doc(tep, specMau());

    assertTrue(ketQua.hopLe());
    assertTrue(ketQua.dong().isEmpty());
    assertEquals(0, ketQua.tongSoDong());
  }

  @Test
  @DisplayName("Tham số không hợp lệ và lỗi ghi tệp được báo đúng loại ngoại lệ")
  void tuChoiThamSoKhongHopLe() {
    File tep = thuMucTam.resolve("mau.xlsx").toFile();
    assertThrows(IllegalArgumentException.class, () -> ExcelTemplate.xuat(null, specMau(), null));
    assertThrows(IllegalArgumentException.class, () -> ExcelTemplate.xuat(tep, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExcelTemplate.xuat(tep, new ImportSpec("DuLieu", List.of(), 0), null));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExcelTemplate.xuat(tep, specMau(), List.<Object[]>of(new Object[] {"thiếu"})));
    assertThrows(
        ExcelExportException.class,
        () ->
            ExcelTemplate.xuat(
                thuMucTam.resolve("khong-ton-tai/mau.xlsx").toFile(), specMau(), null));
  }

  private static ImportSpec specMau() {
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

  private static List<Object[]> viDuMau() {
    return List.<Object[]>of(
        new Object[] {
          "SP01",
          10L,
          new BigDecimal("1250000.50"),
          LocalDate.of(2026, 1, 5),
          true,
          "Mới"
        });
  }
}
