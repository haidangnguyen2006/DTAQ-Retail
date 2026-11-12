package com.dtaq.retail.service;

import static com.dtaq.retail.util.ExcelImporter.KieuDuLieu.CHUOI;
import static com.dtaq.retail.util.ExcelImporter.KieuDuLieu.LOGIC;
import static com.dtaq.retail.util.ExcelImporter.KieuDuLieu.NGAY;
import static com.dtaq.retail.util.ExcelImporter.KieuDuLieu.SO_THAP_PHAN;

import com.dtaq.retail.util.ExcelImporter.CotNhap;
import com.dtaq.retail.util.ExcelImporter.ImportError;
import com.dtaq.retail.util.ExcelImporter.ImportResult;
import com.dtaq.retail.util.ExcelImporter.ImportRow;
import com.dtaq.retail.util.ExcelImporter.ImportSpec;
import com.dtaq.retail.util.ExcelImporter.KieuDuLieu;
import com.dtaq.retail.util.ExcelTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Quy tắc cột và gom nhóm của mẫu nhập khuyến mãi Wave 6. */
public final class KhuyenMaiExcel {
  public static final String MA_NHOM = "Mã nhóm";
  public static final String TEN = "Tên chương trình";
  public static final String MO_TA = "Mô tả";
  public static final String TY_LE = "Tỷ lệ giảm (%)";
  public static final String BAT_DAU = "Ngày bắt đầu";
  public static final String KET_THUC = "Ngày kết thúc";
  public static final String AP_DUNG = "Đang áp dụng";
  public static final String MA_DONG = "Mã dòng sản phẩm";

  public static final ImportSpec MAU = new ImportSpec(ExcelTemplate.TEN_SHEET_DU_LIEU, List.of(
      cot(MA_NHOM, CHUOI, true), cot(TEN, CHUOI, true), cot(MO_TA, CHUOI, false),
      cot(TY_LE, SO_THAP_PHAN, true), cot(BAT_DAU, NGAY, true), cot(KET_THUC, NGAY, true),
      cot(AP_DUNG, LOGIC, true), cot(MA_DONG, CHUOI, true)), 0);

  private KhuyenMaiExcel() {}

  private static CotNhap cot(String ten, KieuDuLieu kieu, boolean batBuoc) {
    return new CotNhap(ten, kieu, batBuoc, List.of());
  }

  public record ThongTin(String ten, String moTa, BigDecimal tyLe,
      LocalDate batDau, LocalDate ketThuc, boolean apDung) {}

  public record Nhom(int dongDau, ThongTin thongTin, Map<String, Integer> dongTheoMaSP) {}

  public static List<Nhom> gomNhom(ImportResult tep, List<ImportError> loi) {
    Map<String, Nhom> cacNhom = new LinkedHashMap<>();
    for (ImportRow dong : tep.dong()) {
      int soDong = dong.soDongExcel();
      var giaTri = dong.giaTri();
      String ten = (String) giaTri.get(TEN);
      BigDecimal tyLe = ((BigDecimal) giaTri.get(TY_LE)).stripTrailingZeros();
      LocalDate batDau = (LocalDate) giaTri.get(BAT_DAU);
      LocalDate ketThuc = (LocalDate) giaTri.get(KET_THUC);
      if (ten.length() > 200) {
        loi.add(loi(soDong, TEN, "Tên chương trình không được vượt quá 200 ký tự"));
      }
      if (tyLe.signum() < 0 || tyLe.compareTo(BigDecimal.valueOf(100)) > 0 || tyLe.scale() > 2) {
        loi.add(loi(soDong, TY_LE, "Tỷ lệ từ 0 đến 100, tối đa 2 chữ số thập phân"));
      }
      if (batDau.isAfter(ketThuc)) {
        loi.add(loi(soDong, KET_THUC, "Ngày kết thúc phải từ ngày bắt đầu trở đi"));
      }
      String maNhom = (String) giaTri.get(MA_NHOM);
      ThongTin thongTin = new ThongTin(ten, (String) giaTri.get(MO_TA), tyLe,
          batDau, ketThuc, (Boolean) giaTri.get(AP_DUNG));
      Nhom nhom = cacNhom.computeIfAbsent(maNhom,
          ignored -> new Nhom(soDong, thongTin, new LinkedHashMap<>()));
      ThongTin goc = nhom.thongTin();
      soSanh(loi, soDong, TEN, goc.ten(), thongTin.ten());
      soSanh(loi, soDong, MO_TA, goc.moTa(), thongTin.moTa());
      soSanh(loi, soDong, TY_LE, goc.tyLe(), thongTin.tyLe());
      soSanh(loi, soDong, BAT_DAU, goc.batDau(), thongTin.batDau());
      soSanh(loi, soDong, KET_THUC, goc.ketThuc(), thongTin.ketThuc());
      soSanh(loi, soDong, AP_DUNG, goc.apDung(), thongTin.apDung());
      String maSP = (String) giaTri.get(MA_DONG);
      if (nhom.dongTheoMaSP().putIfAbsent(maSP, soDong) != null) {
        loi.add(loi(soDong, MA_DONG, "Mã dòng sản phẩm bị lặp trong cùng nhóm"));
      }
    }
    return new ArrayList<>(cacNhom.values());
  }

  private static void soSanh(List<ImportError> loi, int dong, String cot, Object a, Object b) {
    if (!Objects.equals(a, b)) {
      loi.add(loi(dong, cot, "Giá trị phải giống nhau trong cùng mã nhóm"));
    }
  }

  public static ImportError loi(int dong, String cot, String thongBao) {
    return new ImportError(dong, cot, "KHUYEN_MAI", thongBao);
  }
}
