package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.SanPham;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Xử lý nghiệp vụ tra cứu sản phẩm và tồn kho theo các tiêu chí UC004.
 */
public final class TraCuuSanPhamController {

  public enum TieuChi {
    SERIAL,
    DONG_SAN_PHAM,
    DANH_MUC
  }

  public record KetQuaDong(
      String maDongSP,
      String tenDongSP,
      String tenDanhMuc,
      int soLuongTon,
      List<SanPham> danhSachSanPham) {
  }

  private final SanPhamDao sanPhamDao;
  private final DongSanPhamDao dongSanPhamDao;

  public TraCuuSanPhamController(SanPhamDao sanPhamDao, DongSanPhamDao dongSanPhamDao) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
  }

  public List<KetQuaDong> timKiem(TieuChi tieuChi) {
    return timKiem(tieuChi, "");
  }

  public List<KetQuaDong> timKiem(String tuKhoa, TieuChi tieuChi) {
    return timKiem(tieuChi, tuKhoa);
  }

  public List<KetQuaDong> timKiem(TieuChi tieuChi, String tuKhoa) {
    if (tieuChi == null) {
      return List.of();
    }
    String keyword = tuKhoa == null ? "" : tuKhoa.trim();
    if (keyword.isBlank()) {
      return List.of();
    }

    return switch (tieuChi) {
      case SERIAL -> timTheoSerial(keyword);
      case DONG_SAN_PHAM -> timTheoDongSanPham(keyword);
      case DANH_MUC -> timTheoDanhMuc(keyword);
    };
  }

  private List<KetQuaDong> timTheoSerial(String keyword) {
    Optional<SanPham> sanPham = sanPhamDao.findBySerial(keyword);
    if (sanPham.isEmpty()) {
      return List.of();
    }
    DongSanPham dong = sanPham.get().dongSanPham();
    return List.of(toKetQuaDong(dong, sanPhamDao.findByDong(dong.maDongSP())));
  }

  private List<KetQuaDong> timTheoDongSanPham(String keyword) {
    List<DongSanPham> danhSachDong = dongSanPhamDao.search(keyword);
    List<KetQuaDong> ketQua = new ArrayList<>();
    for (DongSanPham dong : danhSachDong) {
      List<SanPham> may = sanPhamDao.findByDong(dong.maDongSP());
      ketQua.add(toKetQuaDong(dong, may));
    }
    return ketQua;
  }

  private List<KetQuaDong> timTheoDanhMuc(String keyword) {
    String search = keyword == null ? "" : keyword.trim();
    if (search.isBlank()) {
      return List.of();
    }
    String lower = search.toLowerCase();
    List<DongSanPham> danhSachDong = dongSanPhamDao.findAllWithDanhMuc().stream()
        .filter(dsp -> dsp.danhMucSanPham() != null)
        .filter(dsp -> {
          String maDanhMuc = dsp.danhMucSanPham().maDanhMuc();
          String tenDanhMuc = dsp.danhMucSanPham().tenDanhMuc();
          if (maDanhMuc != null && maDanhMuc.equalsIgnoreCase(search)) {
            return true;
          }
          if (tenDanhMuc != null && tenDanhMuc.equalsIgnoreCase(search)) {
            return true;
          }
          if (maDanhMuc != null && maDanhMuc.toLowerCase().contains(lower)) {
            return true;
          }
          return tenDanhMuc != null && tenDanhMuc.toLowerCase().contains(lower);
        })
        .toList();
    List<KetQuaDong> ketQua = new ArrayList<>();
    for (DongSanPham dong : danhSachDong) {
      ketQua.add(toKetQuaDong(dong, sanPhamDao.findByDong(dong.maDongSP())));
    }
    return ketQua;
  }

  private KetQuaDong toKetQuaDong(DongSanPham dongSanPham, List<SanPham> sanPhamList) {
    return new KetQuaDong(
        dongSanPham.maDongSP(),
        dongSanPham.tenDongSP(),
        dongSanPham.danhMucSanPham() == null ? "" : dongSanPham.danhMucSanPham().tenDanhMuc(),
        dongSanPham.soLuongTon(),
        sanPhamList == null ? List.of() : sanPhamList);
  }
}
