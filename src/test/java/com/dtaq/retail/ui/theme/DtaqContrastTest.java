package com.dtaq.retail.ui.theme;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Giữ chuẩn tương phản WCAG AA (>= 4,5:1) cho các cặp chữ/nền chính, để không ai làm tụt về sau. */
@DisplayName("Tương phản màu DTAQ Azure đạt WCAG AA")
class DtaqContrastTest {

  private static double lum(Color c) {
    double[] v = {c.getRed() / 255d, c.getGreen() / 255d, c.getBlue() / 255d};
    for (int i = 0; i < 3; i++) {
      v[i] = v[i] <= 0.04045 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
    }
    return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
  }

  private static double ratio(Color a, Color b) {
    double x = lum(a);
    double y = lum(b);
    return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
  }

  static Stream<Arguments> cacCap() {
    return Stream.of(
        Arguments.of("TEXT/SURFACE", DtaqColors.TEXT, DtaqColors.SURFACE),
        Arguments.of("TEXT_MUTED/SURFACE", DtaqColors.TEXT_MUTED, DtaqColors.SURFACE),
        Arguments.of("TEXT_MUTED/BG_APP", DtaqColors.TEXT_MUTED, DtaqColors.BG_APP),
        Arguments.of("TEXT_SUBTLE/SURFACE", DtaqColors.TEXT_SUBTLE, DtaqColors.SURFACE),
        Arguments.of("SURFACE/PRIMARY", DtaqColors.SURFACE, DtaqColors.PRIMARY),
        Arguments.of("SURFACE/PRIMARY_HOVER", DtaqColors.SURFACE, DtaqColors.PRIMARY_HOVER),
        Arguments.of("PRIMARY_STRONG/PRIMARY_SOFT", DtaqColors.PRIMARY_STRONG, DtaqColors.PRIMARY_SOFT),
        Arguments.of("SUCCESS/SUCCESS_SOFT", DtaqColors.SUCCESS, DtaqColors.SUCCESS_SOFT),
        Arguments.of("WARNING/WARNING_SOFT", DtaqColors.WARNING, DtaqColors.WARNING_SOFT),
        Arguments.of("DANGER_STRONG/DANGER_SOFT", DtaqColors.DANGER_STRONG, DtaqColors.DANGER_SOFT),
        Arguments.of("SURFACE/DANGER", DtaqColors.SURFACE, DtaqColors.DANGER),
        Arguments.of("SIDEBAR_TEXT/SIDEBAR_BG", DtaqColors.SIDEBAR_TEXT, DtaqColors.SIDEBAR_BG),
        Arguments.of("SIDEBAR_TEXT_MUTED/SIDEBAR_BG", DtaqColors.SIDEBAR_TEXT_MUTED, DtaqColors.SIDEBAR_BG),
        Arguments.of("SIDEBAR_ACTIVE_TEXT/SIDEBAR_ACTIVE_BG",
            DtaqColors.SIDEBAR_ACTIVE_TEXT, DtaqColors.SIDEBAR_ACTIVE_BG),
        Arguments.of("PILL_NEUTRAL_TEXT/DISABLED_BG", DtaqColors.PILL_NEUTRAL_TEXT, DtaqColors.DISABLED_BG));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cacCap")
  @DisplayName("Cặp chữ/nền đạt tối thiểu 4,5:1")
  void datChuanAA(String ten, Color chu, Color nen) {
    double r = ratio(chu, nen);
    assertTrue(r >= 4.5, ten + " chỉ đạt " + String.format("%.2f", r) + ":1");
  }
}
