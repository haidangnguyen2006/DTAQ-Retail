package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử chọn đúng máy theo dòng đang chọn trong {@link SanPhamPicker}. */
class SanPhamPickerTest {

  private static final String MA_DONG = "DSP-TEST-000001";

  private FakeSanPhamDao sanPhamDao;
  private SanPhamPicker picker;
  private Method mayDuocChon;

  @BeforeEach
  void setUp() throws Exception {
    sanPhamDao = new FakeSanPhamDao();
    picker = new SanPhamPicker(sanPhamDao);
    mayDuocChon = SanPhamPicker.class.getDeclaredMethod("mayDuocChon", int.class);
    mayDuocChon.setAccessible(true);
  }

  @Test
  @DisplayName("Chọn dòng thứ hai trả về đúng serial của máy thứ hai")
  void chonDongThuHaiTraVeMayThuHai() throws Exception {
    picker.taiDanhSach(MA_DONG);

    SanPham chon = (SanPham) mayDuocChon.invoke(picker, 1);

    assertEquals("SERI-TEST-000002", chon.soSerial());
  }

  @Test
  @DisplayName("Chọn dòng đầu trả về đúng serial của máy đầu")
  void chonDongDauTraVeMayDau() throws Exception {
    picker.taiDanhSach(MA_DONG);

    SanPham chon = (SanPham) mayDuocChon.invoke(picker, 0);

    assertEquals("SERI-TEST-000001", chon.soSerial());
  }

  @Test
  @DisplayName("Chỉ số ngoài phạm vi trả về null và không ném lỗi")
  void chiSoNgoaiPhamViTraVeNull() throws Exception {
    picker.taiDanhSach(MA_DONG);

    assertNull(mayDuocChon.invoke(picker, -1));
    assertNull(mayDuocChon.invoke(picker, 99));
  }

  @Test
  @DisplayName("Không có máy bán được thì không mở hộp thoại và trả về rỗng")
  void khongCoMayBanDuocThiTraVeRong() {
    sanPhamDao.banDuoc = false;

    Optional<SanPham> ketQua = picker.chon(null, MA_DONG);

    assertTrue(ketQua.isEmpty());
  }

  private static final class FakeSanPhamDao implements SanPhamDao {
    private boolean banDuoc = true;

    private static SanPham may(String serial) {
      DongSanPham dong = new DongSanPham(
          MA_DONG,
          "Laptop Dell",
          new DanhMucSanPham("DM-TEST-000001", "Điện tử", "Thiết bị điện tử"),
          "Chiếc",
          2,
          BigDecimal.valueOf(10),
          BigDecimal.valueOf(1_000_000),
          true,
          LocalDate.of(2026, 1, 1),
          LocalDate.of(2026, 1, 1));
      return new SanPham(
          serial,
          new NhaCungCap("NCC-TEST-000001", "Nhà cung cấp thử nghiệm"),
          dong,
          BigDecimal.valueOf(800_000),
          LocalDate.of(2026, 1, 5),
          TrangThaiSanPham.SAN_SANG_BAN,
          0,
          0);
    }

    @Override
    public void insert(Connection ketNoi, SanPham sanPham) {
    }

    @Override
    public Optional<SanPham> findById(String soSerial) {
      return List.of(may("SERI-TEST-000001"), may("SERI-TEST-000002")).stream()
          .filter(sp -> sp.soSerial().equals(soSerial))
          .findFirst();
    }

    @Override
    public List<SanPham> findByDongSanPham(String maDongSP) {
      if (!banDuoc || !MA_DONG.equals(maDongSP)) {
        return List.of();
      }
      return List.of(may("SERI-TEST-000001"), may("SERI-TEST-000002"));
    }

    @Override
    public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
      return findByDongSanPham(MA_DONG);
    }

    @Override
    public List<SanPham> findAll() {
      return findByDongSanPham(MA_DONG);
    }

    @Override
    public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {
    }
  }
}
