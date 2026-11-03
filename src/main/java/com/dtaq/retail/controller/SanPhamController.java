package com.dtaq.retail.controller;

import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý nghiệp vụ thêm máy mới vào kho và đồng bộ số lượng tồn theo UC008.
 */
public final class SanPhamController {

  private static final Logger log = LoggerFactory.getLogger(SanPhamController.class);

  private final SanPhamDao sanPhamDao;
  private final DongSanPhamDao dongSanPhamDao;
  private final NhaCungCapDao nhaCungCapDao;
  private final DataSource dataSource;

  public SanPhamController(
      SanPhamDao sanPhamDao,
      DongSanPhamDao dongSanPhamDao,
      NhaCungCapDao nhaCungCapDao,
      DataSource dataSource) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.dongSanPhamDao = Objects.requireNonNull(dongSanPhamDao, "dongSanPhamDao");
    this.nhaCungCapDao = Objects.requireNonNull(nhaCungCapDao, "nhaCungCapDao");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  public KetQua<SanPham> themMay(
      String soSerial,
      String maNCC,
      String maDongSP,
      BigDecimal giaNhap,
      LocalDate ngayNhapKhoBanDau,
      TrangThaiSanPham trangThai) {

    if (soSerial == null || soSerial.isBlank()) {
      return KetQua.loi("Số serial không được để trống");
    }
    if (giaNhap == null || giaNhap.compareTo(BigDecimal.ZERO) < 0) {
      return KetQua.loi("Giá nhập không hợp lệ");
    }
    if (ngayNhapKhoBanDau != null && ngayNhapKhoBanDau.isAfter(LocalDate.now())) {
      return KetQua.loi("Ngày nhập kho ban đầu không được ở tương lai");
    }
    if (trangThai == null) {
      return KetQua.loi("Trạng thái sản phẩm không được để trống");
    }

    String serialChuanHoa = soSerial.trim();
    String maNccChuanHoa = maNCC == null ? "" : maNCC.trim();
    String maDongChuanHoa = maDongSP == null ? "" : maDongSP.trim();

    Optional<SanPham> tonTai = sanPhamDao.findById(serialChuanHoa);
    if (tonTai.isPresent()) {
      return KetQua.loi("Số serial đã tồn tại trong hệ thống");
    }

    Optional<NhaCungCap> nhaCungCap = nhaCungCapDao.findById(maNccChuanHoa);
    if (nhaCungCap.isEmpty()) {
      return KetQua.loi("Nhà cung cấp không tồn tại");
    }

    Optional<DongSanPham> dongSanPham = dongSanPhamDao.findById(maDongChuanHoa);
    if (dongSanPham.isEmpty()) {
      return KetQua.loi("Dòng sản phẩm không tồn tại");
    }
    if (!dongSanPham.get().trangThaiKinhDoanh()) {
      return KetQua.loi("Dòng sản phẩm đang ngừng kinh doanh");
    }

    try (Connection ketNoi = dataSource.getConnection()) {
      boolean oldAutoCommit = ketNoi.getAutoCommit();
      ketNoi.setAutoCommit(false);
      try {
        SanPham sanPham = new SanPham(
            serialChuanHoa,
            nhaCungCap.get(),
            dongSanPham.get(),
            giaNhap,
            ngayNhapKhoBanDau == null ? LocalDate.now() : ngayNhapKhoBanDau,
            trangThai,
            0,
            0);
        sanPhamDao.insert(ketNoi, sanPham);
        // RB37: soLuongTon là bộ đếm dẫn xuất từ SanPham.
        try (PreparedStatement ps = ketNoi.prepareStatement(
            "UPDATE \"DongSanPham\" d SET \"soLuongTon\" = ("
                + "SELECT count(*) FROM \"SanPham\" s"
                + " WHERE s.\"maDongSP\" = d.\"maDongSP\""
                + " AND s.\"trangThai\" = 'SAN_SANG_BAN')"
                + " WHERE d.\"maDongSP\" = ?")) {
          ps.setString(1, maDongChuanHoa);
          ps.executeUpdate();
        }
        ketNoi.commit();
        return KetQua.thanhCong(sanPham, "Thêm máy mới thành công");
      } catch (Exception e) {
        ketNoi.rollback();
        throw e;
      } finally {
        ketNoi.setAutoCommit(oldAutoCommit);
      }
    } catch (SQLException | DataAccessException e) {
      log.error("Lỗi khi lưu máy mới: {}", serialChuanHoa, e);
      return KetQua.loi(e.getMessage() == null ? "Không thể lưu máy mới" : e.getMessage());
    }
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
