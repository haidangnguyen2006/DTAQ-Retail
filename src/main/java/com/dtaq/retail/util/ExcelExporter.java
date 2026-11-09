package com.dtaq.retail.util;

import com.dtaq.retail.exception.ExcelExportException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Xuất báo cáo ra tệp Excel {@code .xlsx} theo một bố cục thống nhất cho mọi trang báo cáo.
 *
 * <p>Bố cục: dòng 0 là tiêu đề báo cáo (in đậm, gộp toàn bộ cột), dòng 1 là tiêu đề cột (in đậm, nền
 * xám, viền mảnh), dữ liệu bắt đầu từ dòng 2; hai dòng đầu được cố định khi cuộn. Số hiển thị theo
 * định dạng {@code #,##0}, ngày theo {@code dd/MM/yyyy}, giá trị {@code null} để trống.
 *
 * <p>Độ rộng cột được tính từ độ dài chuỗi dài nhất trong cột (không dùng {@code autoSizeColumn}) để
 * kết quả luôn tất định và an toàn khi chạy không có màn hình.
 *
 * <p>Apache POI chỉ được dùng trong lớp này; các tầng khác chỉ làm việc với kiểu dữ liệu Java thuần.
 */
public final class ExcelExporter {

  private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String DINH_DANG_SO = "#,##0";
  private static final int TEN_SHEET_TOI_DA = 31;
  private static final int RONG_COT_TOI_DA = 60;
  private static final int RONG_COT_TOI_THIEU = 12;
  private static final Pattern KY_TU_CAM = Pattern.compile("[\\[\\]:*?/\\\\]");

  private ExcelExporter() {
  }

  /**
   * Ghi danh sách dòng dữ liệu ra tệp Excel theo bố cục báo cáo chuẩn của dự án.
   *
   * @param file tệp đích, không được {@code null}
   * @param title tiêu đề báo cáo, không được {@code null} hoặc rỗng
   * @param headers tiêu đề các cột, không được {@code null} hoặc rỗng
   * @param rows dữ liệu từng dòng; mỗi dòng phải có đúng số phần tử bằng {@code headers};
   *     {@code null} được coi là danh sách rỗng
   * @throws IllegalArgumentException nếu tham số không hợp lệ hoặc một dòng sai số cột
   * @throws ExcelExportException nếu không ghi được tệp
   */
  public static void export(
      File file, String title, List<String> headers, List<Object[]> rows) {
    if (file == null) {
      throw new IllegalArgumentException("Tệp xuất không được để trống");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Tiêu đề báo cáo không được để trống");
    }
    if (headers == null || headers.isEmpty()) {
      throw new IllegalArgumentException("Tiêu đề cột không được để trống");
    }
    for (String header : headers) {
      if (header == null || header.isBlank()) {
        throw new IllegalArgumentException("Tiêu đề cột không được để trống");
      }
    }
    List<Object[]> duLieu = rows == null ? List.of() : rows;
    for (Object[] dong : duLieu) {
      if (dong == null || dong.length != headers.size()) {
        throw new IllegalArgumentException("Dòng dữ liệu phải có đúng " + headers.size() + " cột");
      }
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      XSSFSheet sheet = workbook.createSheet(tenSheet(title));
      CellStyle kieuTieuDe = taoKieuTieuDe(workbook);
      CellStyle kieuHeader = taoKieuHeader(workbook);
      CellStyle kieuSo = taoKieuSo(workbook);
      CellStyle kieuNgay = taoKieuNgay(workbook);

      Row dongTieuDe = sheet.createRow(0);
      Cell oTieuDe = dongTieuDe.createCell(0);
      oTieuDe.setCellValue(title);
      oTieuDe.setCellStyle(kieuTieuDe);
      if (headers.size() > 1) {
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.size() - 1));
      }

      Row dongHeader = sheet.createRow(1);
      for (int cot = 0; cot < headers.size(); cot++) {
        Cell oHeader = dongHeader.createCell(cot);
        oHeader.setCellValue(headers.get(cot));
        oHeader.setCellStyle(kieuHeader);
      }

      for (int chiSo = 0; chiSo < duLieu.size(); chiSo++) {
        ghiDong(sheet.createRow(chiSo + 2), duLieu.get(chiSo), kieuSo, kieuNgay);
      }

      for (int cot = 0; cot < headers.size(); cot++) {
        int daiNhat = headers.get(cot).length();
        for (Object[] dong : duLieu) {
          daiNhat = Math.max(daiNhat, chuoiDoDai(dong[cot]).length());
        }
        int rong = Math.max(RONG_COT_TOI_THIEU, Math.min(RONG_COT_TOI_DA, daiNhat + 2));
        sheet.setColumnWidth(cot, rong * 256);
      }

      sheet.createFreezePane(0, 2);

      try (OutputStream out = Files.newOutputStream(file.toPath())) {
        workbook.write(out);
      }
    } catch (IOException e) {
      throw new ExcelExportException("Không thể ghi tệp Excel: " + file.getName(), e);
    }
  }

  private static void ghiDong(
      Row dong, Object[] duLieu, CellStyle kieuSo, CellStyle kieuNgay) {
    for (int cot = 0; cot < duLieu.length; cot++) {
      Cell o = dong.createCell(cot);
      Object giaTri = duLieu[cot];
      if (giaTri == null) {
        o.setBlank();
      } else if (giaTri instanceof BigDecimal so) {
        o.setCellValue(so.doubleValue());
        o.setCellStyle(kieuSo);
      } else if (giaTri instanceof Long
          || giaTri instanceof Integer
          || giaTri instanceof Short) {
        o.setCellValue(((Number) giaTri).doubleValue());
        o.setCellStyle(kieuSo);
      } else if (giaTri instanceof LocalDate ngay) {
        o.setCellValue(LocalDateTime.of(ngay, LocalTime.MIDNIGHT));
        o.setCellStyle(kieuNgay);
      } else if (giaTri instanceof Boolean co) {
        o.setCellValue(co);
      } else {
        o.setCellValue(String.valueOf(giaTri));
      }
    }
  }

  private static String chuoiDoDai(Object giaTri) {
    if (giaTri == null) {
      return "";
    }
    if (giaTri instanceof LocalDate ngay) {
      return NGAY.format(ngay);
    }
    if (giaTri instanceof Number so) {
      // Đo theo dạng đã nhóm hàng nghìn (ô hiển thị định dạng #,##0), không theo chữ số thô.
      return String.format(Locale.ROOT, "%,d", so.longValue());
    }
    return String.valueOf(giaTri);
  }

  private static CellStyle taoKieuTieuDe(XSSFWorkbook workbook) {
    CellStyle kieu = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 14);
    kieu.setFont(font);
    kieu.setAlignment(HorizontalAlignment.CENTER);
    return kieu;
  }

  private static CellStyle taoKieuHeader(XSSFWorkbook workbook) {
    CellStyle kieu = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    kieu.setFont(font);
    kieu.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    kieu.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    kieu.setBorderTop(BorderStyle.THIN);
    kieu.setBorderRight(BorderStyle.THIN);
    kieu.setBorderBottom(BorderStyle.THIN);
    kieu.setBorderLeft(BorderStyle.THIN);
    kieu.setWrapText(true);
    return kieu;
  }

  private static CellStyle taoKieuSo(XSSFWorkbook workbook) {
    CellStyle kieu = workbook.createCellStyle();
    kieu.setDataFormat(workbook.createDataFormat().getFormat(DINH_DANG_SO));
    return kieu;
  }

  private static CellStyle taoKieuNgay(XSSFWorkbook workbook) {
    CellStyle kieu = workbook.createCellStyle();
    kieu.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy"));
    return kieu;
  }

  private static String tenSheet(String title) {
    String ten = KY_TU_CAM.matcher(title).replaceAll("").trim();
    if (ten.isEmpty()) {
      return "Bao cao";
    }
    return ten.length() > TEN_SHEET_TOI_DA ? ten.substring(0, TEN_SHEET_TOI_DA) : ten;
  }
}
