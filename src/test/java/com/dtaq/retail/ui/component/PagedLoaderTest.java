package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dtaq.retail.dao.Trang;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử nạp dần theo trang và bộ nhớ đệm theo phiên của {@link PagedLoader}. */
class PagedLoaderTest {

  private static final List<Integer> DU_LIEU = IntStream.rangeClosed(1, 45).boxed().toList();

  private final List<Integer> dangVe = new ArrayList<>();
  private final AtomicInteger soLanTruyVan = new AtomicInteger();
  private final Pagination pagination = new Pagination();
  private final PagedLoader<Integer> loader = new PagedLoader<>(pagination, 20, "dòng", dong -> {
    dangVe.clear();
    dangVe.addAll(dong);
  });

  private Trang<Integer> truyVan(int boQua, int gioiHan) {
    soLanTruyVan.incrementAndGet();
    return Trang.cat(DU_LIEU, boQua, gioiHan);
  }

  @Test
  @DisplayName("Chỉ nạp trang đang xem; trang đã nạp không truy vấn lại")
  void napDanVaGiuTrangDaNap() {
    loader.datNguon(this::truyVan);

    assertEquals(1, soLanTruyVan.get(), "mở bảng chỉ nạp trang đầu");
    assertEquals(IntStream.rangeClosed(1, 20).boxed().toList(), dangVe);
    assertEquals(45, loader.tongSo());

    loader.chuyenTrang(3);
    assertEquals(2, soLanTruyVan.get());
    assertEquals(List.of(41, 42, 43, 44, 45), dangVe);
    assertEquals(3, pagination.getCurrentPage());

    loader.chuyenTrang(1);
    loader.chuyenTrang(3);
    assertEquals(2, soLanTruyVan.get(), "quay lại trang đã nạp không truy vấn lại");
    assertEquals(Integer.valueOf(41), loader.dong(0));
    assertNull(loader.dong(5));
  }

  @Test
  @DisplayName("Tìm kiếm lại bỏ bộ đệm và về trang đầu; làm mới giữ nguyên trang đang xem")
  void timKiemLaiVaLamMoi() {
    loader.datNguon(this::truyVan);
    loader.chuyenTrang(2);

    loader.lamMoi();
    assertEquals(3, soLanTruyVan.get());
    assertEquals(2, pagination.getCurrentPage(), "làm mới giữ trang đang xem");

    loader.datNguon((boQua, gioiHan) -> Trang.cat(List.of(7, 8), boQua, gioiHan));
    assertEquals(1, pagination.getCurrentPage());
    assertEquals(List.of(7, 8), dangVe);
  }

  @Test
  @DisplayName("Trang đang xem hết dòng sau khi dữ liệu giảm thì lùi về trang còn dữ liệu")
  void trangTrongLuiVeTrangTruoc() {
    List<Integer> nguon = new ArrayList<>(DU_LIEU);
    loader.datNguon((boQua, gioiHan) -> Trang.cat(nguon, boQua, gioiHan));
    loader.chuyenTrang(3);

    nguon.subList(40, 45).clear();
    loader.lamMoi();

    assertEquals(2, pagination.getCurrentPage());
    assertEquals(IntStream.rangeClosed(21, 40).boxed().toList(), dangVe);
  }

  @Test
  @DisplayName("Danh sách có sẵn trong bộ nhớ được chia trang mà không cần nguồn truy vấn")
  void phanTrangDanhSachCoSan() {
    loader.datDanhSach(DU_LIEU);

    assertEquals(20, dangVe.size());
    loader.chuyenTrang(3);
    assertEquals(5, dangVe.size());

    loader.datDanhSach(List.of());
    assertEquals(List.of(), dangVe);
    assertEquals(0, loader.tongSo());
  }
}
