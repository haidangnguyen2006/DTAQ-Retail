package com.dtaq.retail.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Đọc và kiểm tra tệp Excel {@code .xlsx} theo một {@link ImportSpec} dùng chung cho các màn hình
 * nhập liệu.
 *
 * <p>Quy ước tệp: dòng 0 là tiêu đề cột (so khớp chính xác sau khi cắt khoảng trắng), dữ liệu bắt
 * đầu từ dòng 1, dòng trống hoàn toàn bị bỏ qua, cột thừa ngoài {@code spec.cot()} bị bỏ qua.
 *
 * <p>Giới hạn: chỉ nhận {@code .xlsx}, tối đa 10 MB, 50 cột và 5.000 dòng dữ liệu (hoặc
 * {@code spec.soDongToiDa()} nếu nhỏ hơn). Tối đa 200 lỗi được thu thập để tránh phình bộ nhớ.
 *
 * <p>Apache POI chỉ được dùng trong lớp này; các tầng khác chỉ làm việc với kiểu dữ liệu Java thuần.
 */
public final class ExcelImporter {

  /** Dung lượng tệp tối đa được chấp nhận (10 MB). */
  private static final long DUNG_LUONG_TOI_DA = 10L * 1024 * 1024;

  /** Số cột dữ liệu tối đa được chấp nhận. */
  private static final int SO_COT_TOI_DA = 50;

  /** Số dòng dữ liệu tối đa được chấp nhận. */
  private static final int SO_DONG_TOI_DA = 5_000;

  /** Số lỗi tối đa được thu thập. */
  private static final int SO_LOI_TOI_DA = 200;

  private static final int DO_DAI_GIA_TRI_TRONG_THONG_BAO = 50;

  private static final DateTimeFormatter NGAY_DD_MM_YYYY =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter NGAY_YYYY_MM_DD =
      DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

  private ExcelImporter() {
  }

  /** Kiểu dữ liệu của một cột nhập. */
  public enum KieuDuLieu {
    CHUOI,
    SO_NGUYEN,
    SO_THAP_PHAN,
    NGAY,
    LOGIC,
    ENUM
  }

  /**
   * Mô tả một cột cần nhập.
   *
   * @param tieuDe tiêu đề cột, phải khớp dòng 0 của tệp sau khi cắt khoảng trắng
   * @param kieu kiểu dữ liệu cần phân tích
   * @param batBuoc {@code true} nếu ô không được để trống
   * @param giaTriChoPhep danh sách giá trị hợp lệ khi {@code kieu} là {@link KieuDuLieu#ENUM}
   */
  public record CotNhap(
      String tieuDe, KieuDuLieu kieu, boolean batBuoc, List<String> giaTriChoPhep) {

    public CotNhap {
      giaTriChoPhep = giaTriChoPhep == null ? List.of() : List.copyOf(giaTriChoPhep);
    }
  }

  /**
   * Mô tả tệp cần nhập.
   *
   * @param tenSheet tên sheet chứa dữ liệu
   * @param cot danh sách cột theo đúng thứ tự mong đợi
   * @param soDongToiDa số dòng dữ liệu tối đa; không dương thì dùng giới hạn mặc định 5.000
   */
  public record ImportSpec(String tenSheet, List<CotNhap> cot, int soDongToiDa) {

    public ImportSpec {
      cot = cot == null ? List.of() : List.copyOf(cot);
    }
  }

  /**
   * Một dòng dữ liệu hợp lệ đã phân tích.
   *
   * @param soDongExcel chỉ số dòng trong tệp (bắt đầu từ 0, dòng tiêu đề là 0)
   * @param giaTri giá trị theo tiêu đề cột; ô trống có giá trị {@code null}
   */
  public record ImportRow(int soDongExcel, Map<String, Object> giaTri) {

    public ImportRow {
      giaTri =
          giaTri == null
              ? Map.of()
              : Collections.unmodifiableMap(new LinkedHashMap<>(giaTri));
    }
  }

  /**
   * Một lỗi phát hiện khi nhập.
   *
   * @param soDongExcel dòng gây lỗi; {@code 0} cho lỗi cấp tệp hoặc tiêu đề
   * @param cot tiêu đề cột gây lỗi; chuỗi rỗng cho lỗi cấp tệp
   * @param maLoi mã lỗi ổn định để tra cứu
   * @param thongBao thông báo tiếng Việt hiển thị cho người dùng
   */
  public record ImportError(int soDongExcel, String cot, String maLoi, String thongBao) {
  }

  /**
   * Kết quả nhập tệp.
   *
   * @param dong các dòng hợp lệ đã phân tích
   * @param loi các lỗi phát hiện (tối đa 200)
   * @param tongSoDong số dòng dữ liệu không trống đã gặp
   */
  public record ImportResult(List<ImportRow> dong, List<ImportError> loi, int tongSoDong) {

    public ImportResult {
      dong = dong == null ? List.of() : List.copyOf(dong);
      loi = loi == null ? List.of() : List.copyOf(loi);
    }

    /** {@code true} khi không có lỗi nào. */
    public boolean hopLe() {
      return loi.isEmpty();
    }
  }

  /**
   * Đọc tệp Excel và trả về kết quả đã kiểm tra. Không ném ngoại lệ cho lỗi nội dung tệp; mọi lỗi
   * được mô tả trong {@link ImportResult#loi()}.
   *
   * @param file tệp {@code .xlsx} cần đọc
   * @param spec mô tả cột và giới hạn dòng
   * @return kết quả nhập, không bao giờ {@code null}
   * @throws IllegalArgumentException nếu {@code file} hoặc {@code spec} không hợp lệ
   */
  public static ImportResult doc(File file, ImportSpec spec) {
    if (file == null) {
      throw new IllegalArgumentException("Tệp nhập không được để trống");
    }
    if (spec == null) {
      throw new IllegalArgumentException("Mô tả nhập không được để trống");
    }
    if (spec.cot().isEmpty()) {
      throw new IllegalArgumentException("Danh sách cột nhập không được để trống");
    }
    if (spec.tenSheet() == null || spec.tenSheet().isBlank()) {
      throw new IllegalArgumentException("Tên sheet nhập không được để trống");
    }

    List<ImportError> loi = new ArrayList<>();
    if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
      loi.add(loiCapTep("TEP_KHONG_HOP_LE", "Chỉ hỗ trợ tệp Excel định dạng .xlsx"));
      return new ImportResult(List.of(), loi, 0);
    }
    if (!file.isFile()) {
      loi.add(loiCapTep("TEP_KHONG_TON_TAI", "Tệp không tồn tại: " + file.getName()));
      return new ImportResult(List.of(), loi, 0);
    }
    if (file.length() == 0) {
      loi.add(loiCapTep("TEP_RONG", "Tệp Excel rỗng: " + file.getName()));
      return new ImportResult(List.of(), loi, 0);
    }
    if (file.length() > DUNG_LUONG_TOI_DA) {
      loi.add(loiCapTep("TEP_QUA_LON", "Tệp vượt quá dung lượng cho phép 10 MB"));
      return new ImportResult(List.of(), loi, 0);
    }

    try (InputStream in = Files.newInputStream(file.toPath());
        XSSFWorkbook workbook = new XSSFWorkbook(in)) {
      return docTuWorkbook(workbook, spec);
    } catch (IOException | RuntimeException e) {
      loi.add(loiCapTep("LOI_DOC_TEP", "Không thể đọc tệp Excel: " + file.getName()));
      return new ImportResult(List.of(), loi, 0);
    }
  }

  private static ImportResult docTuWorkbook(XSSFWorkbook workbook, ImportSpec spec) {
    List<ImportError> loi = new ArrayList<>();
    Sheet sheet = workbook.getSheet(spec.tenSheet());
    if (sheet == null) {
      loi.add(
          loiCapTep("SHEET_KHONG_TON_TAI", "Không tìm thấy sheet: " + spec.tenSheet()));
      return new ImportResult(List.of(), loi, 0);
    }

    Row dongTieuDe = sheet.getRow(0);
    if (dongTieuDe == null || dongTieuDe.getLastCellNum() <= 0) {
      loi.add(new ImportError(0, "", "THIEU_TIEU_DE", "Tệp không có dòng tiêu đề"));
      return new ImportResult(List.of(), loi, 0);
    }
    if (dongTieuDe.getLastCellNum() > SO_COT_TOI_DA) {
      loi.add(
          new ImportError(
              0, "", "QUA_NHIEU_COT", "Tệp có quá " + SO_COT_TOI_DA + " cột"));
      return new ImportResult(List.of(), loi, 0);
    }

    DataFormatter dinhDang = new DataFormatter(java.util.Locale.ROOT);
    FormulaEvaluator danhGia = workbook.getCreationHelper().createFormulaEvaluator();

    Map<String, Integer> viTriCot = new LinkedHashMap<>();
    for (int cot = 0; cot < dongTieuDe.getLastCellNum(); cot++) {
      String tieuDe = docChuoi(dongTieuDe.getCell(cot), dinhDang, danhGia);
      if (!tieuDe.isEmpty()) {
        viTriCot.putIfAbsent(tieuDe, cot);
      }
    }

    boolean thieuCot = false;
    List<Integer> cotTheoSpec = new ArrayList<>(spec.cot().size());
    for (CotNhap cot : spec.cot()) {
      Integer viTri = viTriCot.get(cot.tieuDe().trim());
      if (viTri == null) {
        thieuCot = true;
        loi.add(
            new ImportError(
                0, cot.tieuDe(), "THIEU_COT", "Thiếu cột bắt buộc: " + cot.tieuDe()));
        cotTheoSpec.add(-1);
      } else {
        cotTheoSpec.add(viTri);
      }
    }
    if (thieuCot) {
      return new ImportResult(List.of(), loi, 0);
    }

    int gioiHanDong = tinhGioiHanDong(spec.soDongToiDa());
    List<ImportRow> dongHopLe = new ArrayList<>();
    int tongSoDong = 0;
    boolean vuotGioiHan = false;

    for (int chiSoDong = 1; chiSoDong <= sheet.getLastRowNum(); chiSoDong++) {
      Row dong = sheet.getRow(chiSoDong);
      if (dong == null || laDongTrong(dong, cotTheoSpec, dinhDang, danhGia)) {
        continue;
      }
      tongSoDong++;
      if (tongSoDong > gioiHanDong) {
        vuotGioiHan = true;
        break;
      }

      Map<String, Object> giaTri = new LinkedHashMap<>();
      List<ImportError> loiCuaDong = new ArrayList<>();
      for (int i = 0; i < spec.cot().size(); i++) {
        CotNhap cot = spec.cot().get(i);
        Cell o = dong.getCell(cotTheoSpec.get(i));
        Object giaTriODaDoc =
            docGiaTri(o, cot, chiSoDong, dinhDang, danhGia, loiCuaDong);
        giaTri.put(cot.tieuDe(), giaTriODaDoc);
      }
      for (ImportError error : loiCuaDong) {
        themLoi(loi, error);
      }
      if (loiCuaDong.isEmpty()) {
        dongHopLe.add(new ImportRow(chiSoDong, giaTri));
      }
    }

    if (vuotGioiHan) {
      loi.add(
          new ImportError(
              0,
              "",
              "QUA_NHIEU_DONG",
              "Tệp vượt quá " + gioiHanDong + " dòng dữ liệu cho phép"));
    }

    return new ImportResult(dongHopLe, loi, tongSoDong);
  }

  private static Object docGiaTri(
      Cell o,
      CotNhap cot,
      int soDong,
      DataFormatter dinhDang,
      FormulaEvaluator danhGia,
      List<ImportError> loi) {
    String chuoi = docChuoi(o, dinhDang, danhGia);
    if (chuoi.isEmpty()) {
      if (cot.batBuoc()) {
        themLoi(
            loi,
            new ImportError(
                soDong,
                cot.tieuDe(),
                "THIEU_GIA_TRI",
                "Giá trị bắt buộc không được để trống"));
      }
      return null;
    }

    switch (cot.kieu()) {
      case CHUOI:
        return chuoi;
      case SO_NGUYEN:
        return docSoNguyen(o, chuoi, cot, soDong, loi);
      case SO_THAP_PHAN:
        return docSoThapPhan(o, chuoi, cot, soDong, loi);
      case NGAY:
        return docNgay(o, chuoi, cot, soDong, loi);
      case LOGIC:
        return docLogic(o, chuoi, cot, soDong, loi);
      case ENUM:
        return docEnum(chuoi, cot, soDong, loi);
      default:
        return chuoi;
    }
  }

  private static Long docSoNguyen(
      Cell o, String chuoi, CotNhap cot, int soDong, List<ImportError> loi) {
    Double so = docSo(o);
    if (so != null) {
      if (so != Math.rint(so) || so < Long.MIN_VALUE || so > Long.MAX_VALUE) {
        themLoi(
            loi,
            new ImportError(
                soDong, cot.tieuDe(), "SAI_KIEU", "Giá trị không phải số nguyên: " + rutGon(chuoi)));
        return null;
      }
      return so.longValue();
    }
    try {
      return Long.parseLong(chuoi);
    } catch (NumberFormatException e) {
      themLoi(
          loi,
          new ImportError(
              soDong, cot.tieuDe(), "SAI_KIEU", "Giá trị không phải số nguyên: " + rutGon(chuoi)));
      return null;
    }
  }

  private static BigDecimal docSoThapPhan(
      Cell o, String chuoi, CotNhap cot, int soDong, List<ImportError> loi) {
    Double so = docSo(o);
    if (so != null) {
      if (!Double.isFinite(so)) {
        themLoi(
            loi,
            new ImportError(
                soDong,
                cot.tieuDe(),
                "SAI_KIEU",
                "Giá trị không phải số thập phân: " + rutGon(chuoi)));
        return null;
      }
      return BigDecimal.valueOf(so);
    }
    try {
      return new BigDecimal(chuoi.replace(',', '.'));
    } catch (NumberFormatException e) {
      themLoi(
          loi,
          new ImportError(
              soDong,
              cot.tieuDe(),
              "SAI_KIEU",
              "Giá trị không phải số thập phân: " + rutGon(chuoi)));
      return null;
    }
  }

  /**
   * Đọc giá trị số từ ô kiểu số hoặc ô công thức trả về số; trả {@code null} nếu ô không phải số
   * hoặc đã được định dạng ngày (khi đó cột kiểu ngày sẽ xử lý).
   */
  private static Double docSo(Cell o) {
    if (o == null) {
      return null;
    }
    CellType kieu = o.getCellType();
    if (kieu != CellType.NUMERIC && kieu != CellType.FORMULA) {
      return null;
    }
    if (DateUtil.isCellDateFormatted(o)) {
      return null;
    }
    try {
      return o.getNumericCellValue();
    } catch (IllegalStateException e) {
      return null;
    }
  }

  private static LocalDate docNgay(
      Cell o, String chuoi, CotNhap cot, int soDong, List<ImportError> loi) {
    if (o != null
        && (o.getCellType() == CellType.NUMERIC || o.getCellType() == CellType.FORMULA)
        && DateUtil.isCellDateFormatted(o)) {
      try {
        return o.getLocalDateTimeCellValue().toLocalDate();
      } catch (IllegalStateException e) {
        // Rơi xuống nhánh đọc chuỗi bên dưới.
      }
    }
    try {
      return LocalDate.parse(chuoi, NGAY_DD_MM_YYYY);
    } catch (DateTimeParseException e) {
      // Thử định dạng ISO bên dưới.
    }
    try {
      return LocalDate.parse(chuoi, NGAY_YYYY_MM_DD);
    } catch (DateTimeParseException e) {
      themLoi(
          loi,
          new ImportError(
              soDong,
              cot.tieuDe(),
              "SAI_KIEU",
              "Ngày không hợp lệ (dd/MM/yyyy hoặc yyyy-MM-dd): " + rutGon(chuoi)));
      return null;
    }
  }

  private static Boolean docLogic(
      Cell o, String chuoi, CotNhap cot, int soDong, List<ImportError> loi) {
    if (o != null && o.getCellType() == CellType.BOOLEAN) {
      return o.getBooleanCellValue();
    }
    String chuanHoa = chuoi.trim().toLowerCase(java.util.Locale.ROOT);
    if (chuanHoa.equals("có") || chuanHoa.equals("co") || chuanHoa.equals("true")
        || chuanHoa.equals("1")) {
      return Boolean.TRUE;
    }
    if (chuanHoa.equals("không") || chuanHoa.equals("khong") || chuanHoa.equals("false")
        || chuanHoa.equals("0")) {
      return Boolean.FALSE;
    }
    themLoi(
        loi,
        new ImportError(
            soDong,
            cot.tieuDe(),
            "SAI_KIEU",
            "Giá trị đúng/sai không hợp lệ (Có/Không/TRUE/FALSE/1/0): " + rutGon(chuoi)));
    return null;
  }

  private static String docEnum(String chuoi, CotNhap cot, int soDong, List<ImportError> loi) {
    String giaTri = chuoi.trim();
    if (cot.giaTriChoPhep().contains(giaTri)) {
      return giaTri;
    }
    themLoi(
        loi,
        new ImportError(
            soDong,
            cot.tieuDe(),
            "SAI_GIA_TRI",
            "Giá trị không nằm trong danh sách cho phép: " + rutGon(giaTri)));
    return null;
  }

  private static String docChuoi(Cell o, DataFormatter dinhDang, FormulaEvaluator danhGia) {
    if (o == null) {
      return "";
    }
    return dinhDang.formatCellValue(o, danhGia).trim();
  }

  private static boolean laDongTrong(
      Row dong,
      List<Integer> cotTheoSpec,
      DataFormatter dinhDang,
      FormulaEvaluator danhGia) {
    for (int viTri : cotTheoSpec) {
      if (!docChuoi(dong.getCell(viTri), dinhDang, danhGia).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static void themLoi(List<ImportError> loi, ImportError error) {
    if (loi.size() < SO_LOI_TOI_DA) {
      loi.add(error);
    }
  }

  private static ImportError loiCapTep(String maLoi, String thongBao) {
    return new ImportError(0, "", maLoi, thongBao);
  }

  private static int tinhGioiHanDong(int soDongToiDa) {
    int gioiHan = soDongToiDa > 0 ? soDongToiDa : SO_DONG_TOI_DA;
    return Math.min(gioiHan, SO_DONG_TOI_DA);
  }

  private static String rutGon(String giaTri) {
    if (giaTri == null) {
      return "";
    }
    return giaTri.length() <= DO_DAI_GIA_TRI_TRONG_THONG_BAO
        ? giaTri
        : giaTri.substring(0, DO_DAI_GIA_TRI_TRONG_THONG_BAO) + "...";
  }

  /**
   * Xuất báo cáo lỗi nhập ra tệp Excel {@code .xlsx} với các cột Dòng/Cột/Mã lỗi/Thông báo.
   *
   * @param file tệp đích
   * @param result kết quả nhập cần báo cáo
   * @throws IllegalArgumentException nếu tham số không hợp lệ
   * @throws ExcelExportException nếu không ghi được tệp
   */
  public static void xuatBaoCaoLoi(File file, ImportResult result) {
    if (file == null) {
      throw new IllegalArgumentException("Tệp báo cáo lỗi không được để trống");
    }
    if (result == null) {
      throw new IllegalArgumentException("Kết quả nhập không được để trống");
    }

    List<String> tieuDe = List.of("Dòng", "Cột", "Mã lỗi", "Thông báo");
    List<Object[]> dong = new ArrayList<>();
    for (ImportError loi : result.loi()) {
      dong.add(
          new Object[] {
            loi.soDongExcel() + 1, loi.cot(), loi.maLoi(), loi.thongBao()
          });
    }
    ExcelExporter.export(file, "Báo cáo lỗi nhập Excel", tieuDe, dong);
  }
}
