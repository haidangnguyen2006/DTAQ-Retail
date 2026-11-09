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
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Sinh tệp Excel mẫu {@code .xlsx} cho một {@link ExcelImporter.ImportSpec} dùng chung cho các màn
 * hình nhập liệu.
 *
 * <p>Tệp mẫu gồm hai sheet:
 *
 * <ul>
 *   <li>{@code DuLieu}: dòng 0 là tiêu đề cột (in đậm, cố định khi cuộn), các dòng tiếp theo là dữ
 *       liệu mẫu. Cột ngày dùng định dạng {@code dd/MM/yyyy}, cột số dùng {@code #,##0} hoặc
 *       {@code #,##0.00}.
 *   <li>{@code HuongDan}: mô tả từng cột (kiểu dữ liệu, bắt buộc, giá trị cho phép).
 * </ul>
 *
 * <p>Dữ liệu mẫu do người gọi truyền vào; tệp sinh ra đọc lại được bằng chính {@link ExcelImporter}
 * với cùng {@code spec} (khi {@code spec.tenSheet()} là {@code "DuLieu"}).
 *
 * <p>Apache POI chỉ được dùng trong lớp này; các tầng khác chỉ làm việc với kiểu dữ liệu Java thuần.
 */
public final class ExcelTemplate {

  /** Tên sheet dữ liệu cố định của tệp mẫu. */
  public static final String TEN_SHEET_DU_LIEU = "DuLieu";

  /** Tên sheet hướng dẫn cố định của tệp mẫu. */
  public static final String TEN_SHEET_HUONG_DAN = "HuongDan";

  private static final String DINH_DANG_SO_NGUYEN = "#,##0";
  private static final String DINH_DANG_SO_THAP_PHAN = "#,##0.00";
  private static final String DINH_DANG_NGAY = "dd/MM/yyyy";
  private static final int RONG_COT_TOI_DA = 60;
  private static final int RONG_COT_TOI_THIEU = 12;

  private ExcelTemplate() {
  }

  /**
   * Ghi tệp Excel mẫu.
   *
   * @param file tệp đích {@code .xlsx}
   * @param spec mô tả các cột cần nhập
   * @param viDu các dòng dữ liệu mẫu; mỗi dòng phải có đúng {@code spec.cot().size()} phần tử.
   *     {@code null} được coi là danh sách rỗng
   * @throws IllegalArgumentException nếu tham số không hợp lệ
   * @throws ExcelExportException nếu không ghi được tệp
   */
  public static void xuat(File file, ExcelImporter.ImportSpec spec, List<Object[]> viDu) {
    if (file == null) {
      throw new IllegalArgumentException("Tệp mẫu không được để trống");
    }
    if (spec == null) {
      throw new IllegalArgumentException("Mô tả nhập không được để trống");
    }
    if (spec.cot().isEmpty()) {
      throw new IllegalArgumentException("Danh sách cột nhập không được để trống");
    }
    List<Object[]> dongViDu = viDu == null ? List.of() : viDu;
    for (Object[] dong : dongViDu) {
      if (dong == null || dong.length != spec.cot().size()) {
        throw new IllegalArgumentException(
            "Dòng mẫu phải có đúng " + spec.cot().size() + " cột");
      }
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      CellStyle kieuHeader = taoKieuHeader(workbook);
      CellStyle kieuSoNguyen = taoKieuSo(workbook, DINH_DANG_SO_NGUYEN);
      CellStyle kieuSoThapPhan = taoKieuSo(workbook, DINH_DANG_SO_THAP_PHAN);
      CellStyle kieuNgay = taoKieuSo(workbook, DINH_DANG_NGAY);

      XSSFSheet duLieu = workbook.createSheet(TEN_SHEET_DU_LIEU);
      taoSheetDuLieu(duLieu, spec, dongViDu, kieuHeader, kieuSoNguyen, kieuSoThapPhan, kieuNgay);

      XSSFSheet huongDan = workbook.createSheet(TEN_SHEET_HUONG_DAN);
      taoSheetHuongDan(huongDan, spec, kieuHeader);

      try (OutputStream out = Files.newOutputStream(file.toPath())) {
        workbook.write(out);
      }
    } catch (IOException e) {
      throw new ExcelExportException("Không thể ghi tệp Excel: " + file.getName(), e);
    }
  }

  private static void taoSheetDuLieu(
      XSSFSheet sheet,
      ExcelImporter.ImportSpec spec,
      List<Object[]> viDu,
      CellStyle kieuHeader,
      CellStyle kieuSoNguyen,
      CellStyle kieuSoThapPhan,
      CellStyle kieuNgay) {
    Row dongHeader = sheet.createRow(0);
    for (int cot = 0; cot < spec.cot().size(); cot++) {
      Cell o = dongHeader.createCell(cot);
      o.setCellValue(spec.cot().get(cot).tieuDe());
      o.setCellStyle(kieuHeader);
    }

    for (int chiSo = 0; chiSo < viDu.size(); chiSo++) {
      Row dong = sheet.createRow(chiSo + 1);
      Object[] giaTri = viDu.get(chiSo);
      for (int cot = 0; cot < spec.cot().size(); cot++) {
        ghiGiaTri(
            dong.createCell(cot),
            giaTri[cot],
            spec.cot().get(cot).kieu(),
            kieuSoNguyen,
            kieuSoThapPhan,
            kieuNgay);
      }
    }

    for (int cot = 0; cot < spec.cot().size(); cot++) {
      int daiNhat = spec.cot().get(cot).tieuDe().length();
      for (Object[] dong : viDu) {
        daiNhat = Math.max(daiNhat, chuoiHienThi(dong[cot]).length());
      }
      sheet.setColumnWidth(cot, doRongCot(daiNhat));
    }
    sheet.createFreezePane(0, 1);
  }

  private static void ghiGiaTri(
      Cell o,
      Object giaTri,
      ExcelImporter.KieuDuLieu kieu,
      CellStyle kieuSoNguyen,
      CellStyle kieuSoThapPhan,
      CellStyle kieuNgay) {
    if (giaTri == null) {
      o.setBlank();
      return;
    }
    switch (kieu) {
      case NGAY -> {
        if (giaTri instanceof LocalDate ngay) {
          o.setCellValue(LocalDateTime.of(ngay, LocalTime.MIDNIGHT));
          o.setCellStyle(kieuNgay);
        } else {
          o.setCellValue(String.valueOf(giaTri));
        }
      }
      case SO_NGUYEN -> {
        if (giaTri instanceof Number so) {
          o.setCellValue(so.doubleValue());
          o.setCellStyle(kieuSoNguyen);
        } else {
          o.setCellValue(String.valueOf(giaTri));
        }
      }
      case SO_THAP_PHAN -> {
        if (giaTri instanceof Number so) {
          o.setCellValue(so.doubleValue());
          o.setCellStyle(kieuSoThapPhan);
        } else {
          o.setCellValue(String.valueOf(giaTri));
        }
      }
      case LOGIC -> {
        if (giaTri instanceof Boolean co) {
          // Ghi dạng chữ để người dùng thấy đúng giá trị cần nhập.
          o.setCellValue(co ? "Có" : "Không");
        } else {
          o.setCellValue(String.valueOf(giaTri));
        }
      }
      case ENUM -> o.setCellValue(String.valueOf(giaTri));
      case CHUOI -> o.setCellValue(chuoiHienThi(giaTri));
      default -> o.setCellValue(String.valueOf(giaTri));
    }
  }

  private static void taoSheetHuongDan(
      XSSFSheet sheet, ExcelImporter.ImportSpec spec, CellStyle kieuHeader) {
    List<String> tieuDe =
        List.of("Cột", "Kiểu dữ liệu", "Bắt buộc", "Giá trị cho phép", "Mô tả");
    Row dongHeader = sheet.createRow(0);
    for (int cot = 0; cot < tieuDe.size(); cot++) {
      Cell o = dongHeader.createCell(cot);
      o.setCellValue(tieuDe.get(cot));
      o.setCellStyle(kieuHeader);
    }

    for (int chiSo = 0; chiSo < spec.cot().size(); chiSo++) {
      ExcelImporter.CotNhap cot = spec.cot().get(chiSo);
      Row dong = sheet.createRow(chiSo + 1);
      dong.createCell(0).setCellValue(cot.tieuDe());
      dong.createCell(1).setCellValue(cot.kieu().name());
      dong.createCell(2).setCellValue(cot.batBuoc() ? "Có" : "Không");
      dong.createCell(3)
          .setCellValue(
              cot.giaTriChoPhep().isEmpty() ? "" : String.join(", ", cot.giaTriChoPhep()));
      dong.createCell(4).setCellValue(moTa(cot.kieu()));
    }

    int[] doDai = {24, 16, 12, 32, 48};
    for (int cot = 0; cot < doDai.length; cot++) {
      sheet.setColumnWidth(cot, doRongCot(doDai[cot]));
    }
    sheet.createFreezePane(0, 1);
  }

  private static String moTa(ExcelImporter.KieuDuLieu kieu) {
    return switch (kieu) {
      case CHUOI -> "Chuỗi ký tự";
      case SO_NGUYEN -> "Số nguyên";
      case SO_THAP_PHAN -> "Số thập phân (dùng dấu chấm thập phân)";
      case NGAY -> "Ngày: dd/MM/yyyy hoặc yyyy-MM-dd";
      case LOGIC -> "Đúng/Sai: Có/Không, TRUE/FALSE hoặc 1/0";
      case ENUM -> "Chỉ nhận một trong các giá trị cho phép";
    };
  }

  private static String chuoiHienThi(Object giaTri) {
    if (giaTri == null) {
      return "";
    }
    if (giaTri instanceof LocalDate ngay) {
      return dinhDangNgay(ngay);
    }
    if (giaTri instanceof BigDecimal so) {
      return so.stripTrailingZeros().toPlainString();
    }
    return String.valueOf(giaTri);
  }

  private static int doRongCot(int doDai) {
    int rong = Math.max(RONG_COT_TOI_THIEU, Math.min(RONG_COT_TOI_DA, doDai + 2));
    return rong * 256;
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
    kieu.setAlignment(HorizontalAlignment.CENTER);
    kieu.setWrapText(true);
    return kieu;
  }

  private static CellStyle taoKieuSo(XSSFWorkbook workbook, String dinhDang) {
    CellStyle kieu = workbook.createCellStyle();
    kieu.setDataFormat(workbook.createDataFormat().getFormat(dinhDang));
    return kieu;
  }

  private static String dinhDangNgay(LocalDate ngay) {
    return String.format(Locale.ROOT, "%02d/%02d/%04d", ngay.getDayOfMonth(),
        ngay.getMonthValue(), ngay.getYear());
  }
}
