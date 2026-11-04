package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.LoaiQuyen;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nghiệp vụ cập nhật giá bán và thuế suất theo UC019.
 */
public final class GiaThueController {

  private static final Logger log = LoggerFactory.getLogger(GiaThueController.class);

  private final DongSanPhamDao dongSanPhamDao;

  public GiaThueController(DongSanPhamDao dongSanPhamDao) {
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
  }

  public KetQua<CapNhatGiaThue> capNhat(
      String maDongSP,
      BigDecimal giaMoi,
      BigDecimal thueMoi,
      LoaiQuyen nguoiThucHien) {
    if (maDongSP == null || maDongSP.isBlank()) {
      return KetQua.loi("Mã dòng sản phẩm không được để trống");
    }
    if (nguoiThucHien != LoaiQuyen.NHAN_VIEN_QUAN_LY) {
      return KetQua.loi("Chỉ quản lý mới được cập nhật giá bán và thuế suất");
    }
    if (giaMoi == null || giaMoi.compareTo(BigDecimal.ZERO) <= 0) {
      return KetQua.loi("Giá bán trước thuế không hợp lệ");
    }
    if (thueMoi == null || thueMoi.compareTo(BigDecimal.ZERO) < 0 || thueMoi.compareTo(BigDecimal.valueOf(100)) > 0) {
      return KetQua.loi("Thuế suất không hợp lệ, phải nằm trong khoảng 0% đến 100%");
    }

    Optional<DongSanPham> hienTai = dongSanPhamDao.findById(maDongSP.trim());
    if (hienTai.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm không tồn tại");
    }

    DongSanPham dong = hienTai.get();
    BigDecimal giaCu = dong.donGiaBanTruocThue();
    BigDecimal thueCu = dong.thue();
    try {
      dongSanPhamDao.updateGiaThue(maDongSP.trim(), giaMoi, thueMoi);
      return KetQua.thanhCong(
          new CapNhatGiaThue(maDongSP.trim(), giaCu, giaMoi, thueCu, thueMoi),
          "Cập nhật giá bán và thuế suất thành công");
    } catch (Exception e) {
      log.warn("Không thể cập nhật giá và thuế cho {}: {}", maDongSP, e.getMessage(), e);
      return KetQua.loi(e.getMessage() == null ? "Không thể cập nhật giá và thuế" : e.getMessage());
    }
  }

  public KetQua<CapNhatGiaThue> capNhatGiaThue(
      String maDongSP,
      BigDecimal giaMoi,
      BigDecimal thueMoi,
      LoaiQuyen nguoiThucHien) {
    return capNhat(maDongSP, giaMoi, thueMoi, nguoiThucHien);
  }

  public record CapNhatGiaThue(
      String maDongSP,
      BigDecimal giaCu,
      BigDecimal giaMoi,
      BigDecimal thueCu,
      BigDecimal thueMoi) {
  }

  public record KetQua<T>(boolean thanhCong, String thongBao, T duLieu) {
    public static <T> KetQua<T> thanhCong(T duLieu, String thongBao) {
      return new KetQua<>(true, thongBao, duLieu);
    }

    public static <T> KetQua<T> loi(String thongBao) {
      return new KetQua<>(false, thongBao, null);
    }
  }
}
