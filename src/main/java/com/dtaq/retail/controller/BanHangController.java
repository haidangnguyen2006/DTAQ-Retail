package com.dtaq.retail.controller;

import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.util.TienUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Xử lý nghiệp vụ lập hóa đơn bán hàng tại quầy (UC003, đợt W1).
 *
 * <p>Theo UC003 bước 4 ("Chọn dòng sản phẩm và số lượng"), hóa đơn bán lẻ được lập theo <b>dòng sản
 * phẩm và số lượng</b>: mỗi dòng giỏ hàng là một dòng sản phẩm kèm số lượng máy (tối thiểu 1, tối đa
 * bằng số máy đang bán được của dòng đó). Máy cụ thể (số serial) chỉ được chọn ở bước thanh toán bằng
 * {@link #phanBoMay()}, nên đợt W1 chưa cần biết serial nào sẽ bán.
 *
 * <p>Tiền được tính theo quy chuẩn C-11 (VND, 0 chữ số thập phân, {@code HALF_UP}) <b>trên từng
 * máy</b> rồi mới nhân với số lượng, nhờ vậy tổng của giỏ hàng luôn bằng tổng của N dòng {@code
 * ChiTietHoaDon} (mỗi dòng {@code soLuong = 1}) sẽ được ghi khi thanh toán. Đợt này chưa áp dụng
 * khuyến mãi.
 *
 * <p>Bộ điều khiển chỉ giữ trạng thái của một hóa đơn đang lập trong bộ nhớ; mọi thao tác đọc/ghi
 * kho và hóa đơn tạm đều đi qua lớp DAO được truyền vào.
 */
public final class BanHangController {

  private static final BigDecimal KHONG = BigDecimal.ZERO.setScale(0);
  private static final int SO_KET_QUA_TOI_DA = 20;

  private final SanPhamDao sanPhamDao;
  private final HoaDonTamDao hoaDonTamDao;
  private final LinkedHashMap<String, Integer> soLuongTheoDong = new LinkedHashMap<>();
  private String maTamHienTai;

  /**
   * Khởi tạo bộ điều khiển bán hàng.
   *
   * @param sanPhamDao lớp truy cập sản phẩm; không được {@code null}
   * @param hoaDonTamDao lớp lưu hóa đơn tạm; không được {@code null}
   */
  public BanHangController(SanPhamDao sanPhamDao, HoaDonTamDao hoaDonTamDao) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.hoaDonTamDao = Objects.requireNonNull(hoaDonTamDao, "hoaDonTamDao");
  }

  /**
   * Tính tiền của một dòng hóa đơn theo đơn vị máy, sau đó nhân với số lượng.
   *
   * <p>Đơn giá và tiền thuế được làm tròn <b>một lần cho một máy</b> rồi mới nhân với số lượng; không
   * làm tròn lại tổng của dòng. Nhờ vậy tổng của dòng bằng đúng tổng của {@code soLuong} dòng chi tiết
   * hóa đơn (mỗi dòng một máy) sẽ ghi khi thanh toán. Đợt W1 chưa có khuyến mãi nên tiền khuyến mãi
   * luôn bằng {@code 0}.
   *
   * @param donGiaTruocThue đơn giá bán trước thuế của một máy; không được {@code null}
   * @param thueSuat thuế suất tính theo phần trăm; không được {@code null}
   * @param soLuong số lượng máy của dòng; phải lớn hơn {@code 0}
   * @return tiền của dòng đã làm tròn
   * @throws IllegalArgumentException nếu tham số {@code null} hoặc số lượng không hợp lệ
   */
  public static DongTinh tinhDong(BigDecimal donGiaTruocThue, BigDecimal thueSuat, int soLuong) {
    if (donGiaTruocThue == null) {
      throw new IllegalArgumentException("Đơn giá trước thuế không được để trống");
    }
    if (thueSuat == null) {
      throw new IllegalArgumentException("Thuế suất không được để trống");
    }
    if (soLuong <= 0) {
      throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
    }
    BigDecimal soMay = BigDecimal.valueOf(soLuong);
    BigDecimal motMay = TienUtil.lamTron(donGiaTruocThue);
    BigDecimal thueMotMay =
        TienUtil.lamTron(motMay.multiply(thueSuat).divide(BigDecimal.valueOf(100)));
    BigDecimal tienSauKhuyenMai = motMay.multiply(soMay);
    return new DongTinh(
        tienSauKhuyenMai,
        TienUtil.lamTron(BigDecimal.ZERO),
        tienSauKhuyenMai,
        thueMotMay.multiply(soMay));
  }

  /**
   * Tìm dòng sản phẩm đang kinh doanh và còn máy bán được theo mã hoặc tên dòng.
   *
   * <p>Chỉ trả về dòng còn kinh doanh và có ít nhất một máy {@link TrangThaiSanPham#SAN_SANG_BAN};
   * tồn kho hiển thị là số máy bán được của dòng, đếm trên đúng một lần đọc dữ liệu để các dòng trong
   * kết quả nhất quán với nhau.
   *
   * @param tuKhoa một phần mã hoặc tên dòng sản phẩm, không phân biệt hoa thường; rỗng thì trả rỗng
   * @return tối đa 20 dòng, sắp xếp theo mã dòng sản phẩm; rỗng nếu không khớp
   */
  public List<DongBan> timDongSanPham(String tuKhoa) {
    if (tuKhoa == null || tuKhoa.isBlank()) {
      return List.of();
    }
    String khoa = tuKhoa.trim().toLowerCase(Locale.ROOT);
    return docDongSanPham(docMay()).values().stream()
        .filter(dong -> dong.tonKho() > 0)
        .filter(dong -> khopTuKhoa(dong, khoa))
        .sorted(Comparator.comparing(DongBan::maDongSP))
        .limit(SO_KET_QUA_TOI_DA)
        .toList();
  }

  /**
   * Thêm một dòng sản phẩm vào hóa đơn đang lập; nếu dòng đã có thì cộng dồn số lượng.
   *
   * <p>Số lượng sau khi cộng không được vượt quá số máy đang bán được của dòng.
   *
   * @param maDongSP mã dòng sản phẩm cần thêm
   * @param soLuong số lượng máy cần thêm; phải lớn hơn {@code 0}
   * @return kết quả thêm dòng kèm hóa đơn hiện tại; không bao giờ {@code null}
   * @throws IllegalArgumentException nếu {@code soLuong} không lớn hơn {@code 0}
   */
  public KetQuaDoiDong themDong(String maDongSP, int soLuong) {
    if (soLuong <= 0) {
      throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
    }
    Map<String, DongBan> dongSanPham = docDongSanPham(docMay());
    DongBan dong = maDongSP == null ? null : dongSanPham.get(maDongSP);
    if (dong == null) {
      return new KetQuaDoiDong(
          false, "Dòng sản phẩm " + maDongSP + " không còn kinh doanh", hoaDonHienTai());
    }
    if (dong.tonKho() == 0) {
      return new KetQuaDoiDong(
          false, "Dòng sản phẩm " + maDongSP + " đã hết hàng", hoaDonHienTai());
    }
    int hienTai = soLuongTheoDong.getOrDefault(maDongSP, 0);
    if (hienTai + soLuong > dong.tonKho()) {
      return new KetQuaDoiDong(
          false, "Dòng sản phẩm " + maDongSP + " chỉ còn " + dong.tonKho() + " máy",
          hoaDonHienTai());
    }
    soLuongTheoDong.put(maDongSP, hienTai + soLuong);
    return new KetQuaDoiDong(true, "Đã thêm dòng sản phẩm " + maDongSP, hoaDonHienTai());
  }

  /**
   * Đặt lại số lượng của một dòng đã có trong hóa đơn đang lập.
   *
   * @param maDongSP mã dòng sản phẩm cần đổi số lượng
   * @param soLuong số lượng mới; tối thiểu {@code 1} và không vượt quá tồn kho bán được
   * @return kết quả đổi số lượng kèm hóa đơn hiện tại; không bao giờ {@code null}
   * @throws IllegalArgumentException nếu dòng chưa có trong hóa đơn
   */
  public KetQuaDoiDong datSoLuong(String maDongSP, int soLuong) {
    if (maDongSP == null || !soLuongTheoDong.containsKey(maDongSP)) {
      throw new IllegalArgumentException("Dòng sản phẩm chưa có trong hóa đơn");
    }
    if (soLuong < 1) {
      return new KetQuaDoiDong(false, "Số lượng tối thiểu là 1", hoaDonHienTai());
    }
    Map<String, DongBan> dongSanPham = docDongSanPham(docMay());
    DongBan dong = dongSanPham.get(maDongSP);
    if (dong == null) {
      return new KetQuaDoiDong(
          false, "Dòng sản phẩm " + maDongSP + " không còn kinh doanh", hoaDonHienTai());
    }
    if (soLuong > dong.tonKho()) {
      return new KetQuaDoiDong(
          false, "Dòng sản phẩm " + maDongSP + " chỉ còn " + dong.tonKho() + " máy",
          hoaDonHienTai());
    }
    soLuongTheoDong.put(maDongSP, soLuong);
    return new KetQuaDoiDong(true, "Đã cập nhật số lượng dòng sản phẩm " + maDongSP,
        hoaDonHienTai());
  }

  /**
   * Bỏ một dòng khỏi hóa đơn đang lập; không báo lỗi nếu dòng không có trong hóa đơn.
   *
   * @param maDongSP mã dòng sản phẩm cần bỏ
   * @return hóa đơn hiện tại sau khi bỏ; không bao giờ {@code null}
   */
  public HoaDonTinh boDong(String maDongSP) {
    if (maDongSP != null) {
      soLuongTheoDong.remove(maDongSP);
    }
    return hoaDonHienTai();
  }

  /**
   * Tính hóa đơn đang lập từ giỏ hàng hiện tại.
   *
   * <p>Mỗi dòng giỏ hàng gồm dòng sản phẩm và số lượng; thành tiền của dòng là tiền của một máy (đã
   * làm tròn) nhân với số lượng. Tổng tiền là tổng các giá trị đã làm tròn ở cấp dòng; không làm tròn
   * lại tổng (C-11).
   *
   * @return hóa đơn hiện tại; hóa đơn rỗng nếu giỏ hàng trống
   */
  public HoaDonTinh hoaDonHienTai() {
    if (soLuongTheoDong.isEmpty()) {
      return HoaDonTinh.rong();
    }
    Map<String, DongBan> dongSanPham = docDongSanPham(docMay());
    List<DongGio> dongHang = new ArrayList<>(soLuongTheoDong.size());
    BigDecimal tongTruocThue = KHONG;
    BigDecimal tongThue = KHONG;
    for (Map.Entry<String, Integer> entry : soLuongTheoDong.entrySet()) {
      DongBan dong = dongSanPham.get(entry.getKey());
      if (dong == null) {
        continue;
      }
      int soLuong = entry.getValue();
      DongTinh tien = tinhDong(dong.donGiaTruocThue(), dong.thueSuat(), soLuong);
      dongHang.add(new DongGio(
          dong.maDongSP(),
          dong.tenDongSP(),
          soLuong,
          dong.tonKho(),
          dong.donGiaTruocThue(),
          dong.thueSuat(),
          tien.tienSauKhuyenMai().add(tien.tienThue())));
      tongTruocThue = tongTruocThue.add(tien.tienTruocKhuyenMai());
      tongThue = tongThue.add(tien.tienThue());
    }
    if (dongHang.isEmpty()) {
      return HoaDonTinh.rong();
    }
    return new HoaDonTinh(
        List.copyOf(dongHang), tongTruocThue, tongThue, tongTruocThue.add(tongThue));
  }

  /**
   * Phân bổ máy cụ thể cho từng dòng giỏ hàng theo nguyên tắc nhập kho lâu nhất trước (FIFO).
   *
   * <p>Máy được chọn trong số máy {@link TrangThaiSanPham#SAN_SANG_BAN} của đúng dòng sản phẩm và
   * thuộc dòng còn kinh doanh, sắp xếp theo {@code ngayNhapKhoBanDau} tăng dần (ngày trống xếp cuối),
   * cùng ngày thì theo số serial tăng dần. Nếu không đủ máy cho một dòng thì mã dòng đó được báo lại
   * trong {@link KetQuaPhanBo#dongThieu()}. Đợt W1 chưa dùng kết quả này để ghi hóa đơn; luồng thanh
   * toán W2 sẽ dùng.
   *
   * @return danh sách máy được chọn và các dòng thiếu máy; không bao giờ {@code null}
   */
  public KetQuaPhanBo phanBoMay() {
    List<SanPham> may = docMay();
    List<MayDuocChon> chon = new ArrayList<>();
    List<String> dongThieu = new ArrayList<>();
    Comparator<SanPham> thuTu = Comparator
        .comparing(SanPham::ngayNhapKhoBanDau, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(SanPham::soSerial, Comparator.nullsLast(Comparator.naturalOrder()));
    for (Map.Entry<String, Integer> entry : soLuongTheoDong.entrySet()) {
      String maDongSP = entry.getKey();
      int soLuong = entry.getValue();
      List<SanPham> khaDung = may.stream()
          .filter(Objects::nonNull)
          .filter(sanPham -> sanPham.dongSanPham() != null)
          .filter(sanPham -> maDongSP.equals(sanPham.dongSanPham().maDongSP()))
          .filter(sanPham -> sanPham.trangThai() == TrangThaiSanPham.SAN_SANG_BAN)
          .filter(sanPham -> sanPham.dongSanPham().trangThaiKinhDoanh())
          .sorted(thuTu)
          .toList();
      int lay = Math.min(soLuong, khaDung.size());
      for (int i = 0; i < lay; i++) {
        SanPham sanPham = khaDung.get(i);
        chon.add(new MayDuocChon(maDongSP, sanPham.soSerial(), sanPham.ngayNhapKhoBanDau()));
      }
      if (lay < soLuong) {
        dongThieu.add(maDongSP);
      }
    }
    return new KetQuaPhanBo(List.copyOf(chon), List.copyOf(dongThieu));
  }

  /** Xóa toàn bộ giỏ hàng và quên mã hóa đơn tạm đang gắn với hóa đơn hiện tại. */
  public void xoaGio() {
    soLuongTheoDong.clear();
    maTamHienTai = null;
  }

  /**
   * Mã hóa đơn tạm đang gắn với hóa đơn hiện tại.
   *
   * @return mã hóa đơn tạm, hoặc {@code null} nếu hóa đơn hiện tại chưa được lưu tạm
   */
  public String maTamHienTai() {
    return maTamHienTai;
  }

  /**
   * Lưu hóa đơn đang lập thành hóa đơn tạm.
   *
   * <p>Mỗi dòng của hóa đơn tạm là một dòng sản phẩm kèm số lượng; máy cụ thể được chọn lại ở bước
   * thanh toán. Nếu hóa đơn đang gắn với một mã tạm thì lần lưu này ghi đè đúng hóa đơn đó; ngược lại
   * lớp DAO cấp mã mới. Thời điểm lưu do lớp DAO ghi.
   *
   * @param maNV mã nhân viên đang lập hóa đơn
   * @param tenNhanVien tên nhân viên đang lập hóa đơn
   * @param khachHang khách hàng của hóa đơn; có thể {@code null}
   * @return hóa đơn tạm đã lưu; không bao giờ {@code null}
   */
  public HoaDonTamDao.HoaDonTam luuBanTam(
      String maNV, String tenNhanVien, KhachHang khachHang) {
    Map<String, DongBan> dongSanPham = docDongSanPham(docMay());
    List<HoaDonTamDao.DongTam> dongHang = new ArrayList<>(soLuongTheoDong.size());
    for (Map.Entry<String, Integer> entry : soLuongTheoDong.entrySet()) {
      DongBan dong = dongSanPham.get(entry.getKey());
      if (dong == null) {
        continue;
      }
      dongHang.add(new HoaDonTamDao.DongTam(
          dong.maDongSP(),
          dong.tenDongSP(),
          entry.getValue(),
          dong.donGiaTruocThue(),
          dong.thueSuat()));
    }
    HoaDonTamDao.HoaDonTam banTam = new HoaDonTamDao.HoaDonTam(
        maTamHienTai,
        maNV,
        tenNhanVien,
        khachHang == null ? null : khachHang.maKH(),
        khachHang == null ? null : khachHang.hoTen(),
        khachHang == null ? null : khachHang.soDienThoai(),
        null,
        List.copyOf(dongHang));
    HoaDonTamDao.HoaDonTam daLuu = hoaDonTamDao.luu(banTam);
    maTamHienTai = daLuu.maTam();
    return daLuu;
  }

  /**
   * Liệt kê hóa đơn tạm của một nhân viên, mới nhất trước.
   *
   * @param maNV mã nhân viên
   * @return danh sách hóa đơn tạm; rỗng nếu không có
   */
  public List<HoaDonTamDao.HoaDonTam> danhSachBanTam(String maNV) {
    return hoaDonTamDao.findByNhanVien(maNV);
  }

  /**
   * Khôi phục hóa đơn tạm vào hóa đơn đang lập.
   *
   * <p>Dòng sản phẩm được đọc lại từ kho nên đơn giá và thuế suất là giá trị hiện hành, không phải
   * giá trị đã lưu. Dòng không còn kinh doanh hoặc đã hết máy bị bỏ; dòng còn máy nhưng ít hơn số
   * lượng đã lưu thì được điều chỉnh về đúng tồn kho. Các dòng bị bỏ hoặc bị điều chỉnh được báo lại
   * trong {@link KetQuaKhoiPhuc#dongDaDieuChinh()}.
   *
   * <p>Khôi phục thành công là thao tác "mở" hóa đơn tạm: hóa đơn tạm được xóa khỏi danh sách hóa
   * đơn tạm (tệp của nó bị xóa) vì nội dung đã chuyển sang hóa đơn đang lập. Hóa đơn đang lập vẫn
   * giữ mã tạm đó, nên lần {@link #luuBanTam(String, String, KhachHang)} kế tiếp sẽ tạo lại hóa đơn
   * tạm dưới đúng mã cũ thay vì cấp mã mới. Nếu không tìm thấy hóa đơn tạm thì không có gì bị xóa.
   *
   * @param maTam mã hóa đơn tạm cần khôi phục
   * @return kết quả khôi phục; không bao giờ {@code null}
   * @throws IllegalArgumentException nếu không tìm thấy hóa đơn tạm
   */
  public KetQuaKhoiPhuc khoiPhucBanTam(String maTam) {
    Optional<HoaDonTamDao.HoaDonTam> timThay =
        maTam == null ? Optional.empty() : hoaDonTamDao.findById(maTam);
    if (timThay.isEmpty()) {
      throw new IllegalArgumentException("Không tìm thấy hóa đơn tạm " + maTam);
    }
    HoaDonTamDao.HoaDonTam banTam = timThay.get();
    soLuongTheoDong.clear();
    Map<String, DongBan> dongSanPham = docDongSanPham(docMay());
    List<String> dongDaDieuChinh = new ArrayList<>();
    List<HoaDonTamDao.DongTam> dongHang =
        banTam.dongHang() == null ? List.of() : banTam.dongHang();
    for (HoaDonTamDao.DongTam dong : dongHang) {
      DongBan thongTin = dong.maDongSP() == null ? null : dongSanPham.get(dong.maDongSP());
      if (thongTin == null || thongTin.tonKho() == 0) {
        dongDaDieuChinh.add(dong.maDongSP());
        continue;
      }
      int soLuong = Math.min(dong.soLuong(), thongTin.tonKho());
      if (soLuong != dong.soLuong()) {
        dongDaDieuChinh.add(dong.maDongSP());
      }
      soLuongTheoDong.put(dong.maDongSP(), soLuong);
    }
    maTamHienTai = maTam;
    hoaDonTamDao.xoa(maTam);
    return new KetQuaKhoiPhuc(banTam, List.copyOf(dongDaDieuChinh), hoaDonHienTai());
  }

  /**
   * Xóa một hóa đơn tạm theo mã.
   *
   * @param maTam mã hóa đơn tạm cần xóa
   */
  public void xoaBanTam(String maTam) {
    hoaDonTamDao.xoa(maTam);
  }

  private List<SanPham> docMay() {
    return sanPhamDao.findAll();
  }

  /**
   * Gom máy trong kho thành các dòng sản phẩm còn kinh doanh.
   *
   * <p>Tồn kho của dòng là số máy {@link TrangThaiSanPham#SAN_SANG_BAN} của dòng đó. Thứ tự dòng giữ
   * theo thứ tự xuất hiện đầu tiên trong dữ liệu kho.
   */
  private Map<String, DongBan> docDongSanPham(List<SanPham> may) {
    Map<String, Integer> tonTheoDong = new HashMap<>();
    for (SanPham sanPham : may) {
      if (sanPham == null || sanPham.dongSanPham() == null) {
        continue;
      }
      if (sanPham.trangThai() == TrangThaiSanPham.SAN_SANG_BAN) {
        tonTheoDong.merge(sanPham.dongSanPham().maDongSP(), 1, Integer::sum);
      }
    }
    Map<String, DongBan> ketQua = new LinkedHashMap<>();
    for (SanPham sanPham : may) {
      if (sanPham == null || sanPham.dongSanPham() == null) {
        continue;
      }
      DongSanPham dongSanPham = sanPham.dongSanPham();
      if (!dongSanPham.trangThaiKinhDoanh()) {
        continue;
      }
      ketQua.computeIfAbsent(dongSanPham.maDongSP(), ma -> new DongBan(
          dongSanPham.maDongSP(),
          dongSanPham.tenDongSP(),
          tonTheoDong.getOrDefault(dongSanPham.maDongSP(), 0),
          dongSanPham.donGiaBanTruocThue(),
          dongSanPham.thue()));
    }
    return ketQua;
  }

  private static boolean khopTuKhoa(DongBan dong, String tuKhoa) {
    boolean khopMa = dong.maDongSP() != null
        && dong.maDongSP().toLowerCase(Locale.ROOT).contains(tuKhoa);
    boolean khopTen = dong.tenDongSP() != null
        && dong.tenDongSP().toLowerCase(Locale.ROOT).contains(tuKhoa);
    return khopMa || khopTen;
  }

  /**
   * Một dòng sản phẩm đang kinh doanh, dùng cho danh sách chọn dòng sản phẩm.
   *
   * @param maDongSP mã dòng sản phẩm
   * @param tenDongSP tên dòng sản phẩm
   * @param tonKho số máy bán được còn lại của dòng sản phẩm
   * @param donGiaTruocThue đơn giá bán trước thuế của một máy
   * @param thueSuat thuế suất áp dụng
   */
  public record DongBan(
      String maDongSP, String tenDongSP, int tonKho,
      BigDecimal donGiaTruocThue, BigDecimal thueSuat) {
  }

  /**
   * Tiền của một dòng hóa đơn sau khi làm tròn.
   *
   * @param tienTruocKhuyenMai tiền trước khuyến mãi
   * @param tienKhuyenMai tiền được giảm giá
   * @param tienSauKhuyenMai tiền sau khuyến mãi
   * @param tienThue tiền thuế của dòng
   */
  public record DongTinh(
      BigDecimal tienTruocKhuyenMai, BigDecimal tienKhuyenMai,
      BigDecimal tienSauKhuyenMai, BigDecimal tienThue) {
  }

  /**
   * Một dòng hàng của hóa đơn đang lập.
   *
   * @param maDongSP mã dòng sản phẩm
   * @param tenDongSP tên dòng sản phẩm
   * @param soLuong số lượng máy của dòng
   * @param tonKho số máy bán được còn lại của dòng tại thời điểm tính
   * @param donGiaTruocThue đơn giá bán trước thuế của một máy
   * @param thueSuat thuế suất áp dụng
   * @param thanhTienSauThue thành tiền của dòng đã gồm thuế
   */
  public record DongGio(
      String maDongSP, String tenDongSP, int soLuong, int tonKho,
      BigDecimal donGiaTruocThue, BigDecimal thueSuat,
      BigDecimal thanhTienSauThue) {
  }

  /**
   * Hóa đơn đang lập cùng các tổng tiền.
   *
   * @param dongHang danh sách dòng hàng
   * @param tongTruocThue tổng tiền trước thuế
   * @param tongThue tổng tiền thuế
   * @param tongThanhToan tổng tiền khách phải trả
   */
  public record HoaDonTinh(
      List<DongGio> dongHang, BigDecimal tongTruocThue,
      BigDecimal tongThue, BigDecimal tongThanhToan) {

    /**
     * Hóa đơn rỗng với mọi tổng tiền bằng {@code 0}.
     *
     * @return hóa đơn không có dòng hàng
     */
    public static HoaDonTinh rong() {
      return new HoaDonTinh(List.of(), KHONG, KHONG, KHONG);
    }
  }

  /**
   * Kết quả một thao tác thay đổi dòng hàng của hóa đơn.
   *
   * @param thanhCong {@code true} nếu thao tác được áp dụng
   * @param thongBao thông báo tiếng Việt cho người dùng
   * @param hoaDon hóa đơn hiện tại sau thao tác
   */
  public record KetQuaDoiDong(boolean thanhCong, String thongBao, HoaDonTinh hoaDon) {
  }

  /**
   * Một máy được phân bổ cho một dòng của hóa đơn.
   *
   * @param maDongSP mã dòng sản phẩm của máy
   * @param soSerial số serial của máy
   * @param ngayNhapKhoBanDau ngày nhập kho ban đầu của máy; có thể {@code null}
   */
  public record MayDuocChon(String maDongSP, String soSerial, LocalDate ngayNhapKhoBanDau) {
  }

  /**
   * Kết quả phân bổ máy cho giỏ hàng.
   *
   * @param mayDuocChon danh sách máy được chọn, theo thứ tự dòng hàng của hóa đơn
   * @param dongThieu mã các dòng sản phẩm không đủ máy để phân bổ
   */
  public record KetQuaPhanBo(List<MayDuocChon> mayDuocChon, List<String> dongThieu) {
  }

  /**
   * Kết quả khôi phục hóa đơn tạm.
   *
   * @param banTam hóa đơn tạm vừa khôi phục
   * @param dongDaDieuChinh mã các dòng sản phẩm bị bỏ hoặc bị giảm số lượng do tồn kho thay đổi
   * @param hoaDon hóa đơn hiện tại sau khi khôi phục
   */
  public record KetQuaKhoiPhuc(
      HoaDonTamDao.HoaDonTam banTam, List<String> dongDaDieuChinh, HoaDonTinh hoaDon) {
  }
}
