package com.dtaq.retail.ui.component;

import java.util.regex.Pattern;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** Bộ lọc ký tự cho ô số nguyên và giờ chạy. */
public final class InputFilters {
  private static final Pattern TIEN_TO_GIO = Pattern.compile("\\d{0,2}|\\d{1,2}:\\d{0,2}");
  private static final Pattern HAI_CHU_SO = Pattern.compile("\\d{2}");
  private static final Pattern BON_CHU_SO = Pattern.compile("\\d{4}");

  private InputFilters() {}

  /** Chỉ cho nhập chữ số, tối đa số ký tự được chỉ định. */
  public static void chiChoSo(JTextField o, int doDaiToiDa) {
    ganBoLoc(o, (hienTai, viTri, doDai, chen) -> {
      String ketQua = thayDoan(hienTai, viTri, doDai, chen);
      return laSoHopLe(ketQua, doDaiToiDa) ? ketQua : null;
    });
  }

  /** Cho nhập giờ dạng HH:mm, tự thêm dấu hai chấm khi gõ liền tay. */
  public static void gioPhut(JTextField o) {
    ganBoLoc(o, InputFilters::noiDungGioPhutSauKhiGo);
  }

  static boolean laSoHopLe(String s, int doDaiToiDa) {
    return s.length() <= doDaiToiDa && s.chars().allMatch(c -> c >= '0' && c <= '9');
  }

  static String noiDungGioPhutSauKhiGo(String hienTai, int viTri, int doDai, String chen) {
    String doanChen = chen == null ? "" : chen.strip();
    String ketQua = thayDoan(hienTai, viTri, doDai, doanChen);
    if (doanChen.isEmpty() || TIEN_TO_GIO.matcher(ketQua).matches()) {
      return ketQua;
    }
    if (doDai == 0 && viTri == hienTai.length() && HAI_CHU_SO.matcher(hienTai).matches()
        && doanChen.length() == 1 && laSoHopLe(doanChen, 1)) {
      return hienTai + ":" + doanChen;
    }
    if (doanChen.length() > 1 && BON_CHU_SO.matcher(ketQua).matches()) {
      return ketQua.substring(0, 2) + ":" + ketQua.substring(2);
    }
    return null;
  }

  private static String thayDoan(String hienTai, int viTri, int doDai, String chen) {
    return hienTai.substring(0, viTri) + chen + hienTai.substring(viTri + doDai);
  }

  private static void ganBoLoc(JTextField o, NoiDungMoi boLoc) {
    ((AbstractDocument) o.getDocument()).setDocumentFilter(new DocumentFilter() {
      @Override
      public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
          throws BadLocationException {
        replace(fb, offset, 0, string, attr);
      }

      @Override
      public void replace(FilterBypass fb, int offset, int length, String chen, AttributeSet attrs)
          throws BadLocationException {
        String hienTai = fb.getDocument().getText(0, fb.getDocument().getLength());
        String doanChen = chen == null ? "" : chen.strip();
        String moi = boLoc.tinh(hienTai, offset, length, doanChen);
        if (moi == null) {
          LookAndFeel laf = UIManager.getLookAndFeel();
          if (laf != null) {
            laf.provideErrorFeedback(o);
          }
          return;
        }

        if (moi.equals(thayDoan(hienTai, offset, length, doanChen))) {
          fb.replace(offset, length, doanChen, attrs);
        } else if (offset == hienTai.length() && moi.startsWith(hienTai)) {
          fb.replace(offset, 0, moi.substring(offset), attrs);
        } else {
          fb.replace(0, hienTai.length(), moi, attrs);
        }
      }
    });
  }

  @FunctionalInterface
  private interface NoiDungMoi {
    String tinh(String hienTai, int viTri, int doDai, String chen);
  }
}
