package com.dtaq.retail.controller;

import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.MaGenerator;
import com.dtaq.retail.util.TienUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
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
import javax.sql.DataSource;

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
 * ChiTietHoaDon} (mỗi dòng {@code soLuong = 1}) sẽ được ghi khi thanh toán. Khuyến mãi đang hiệu lực
 * của từng dòng sản phẩm được áp tại thời điểm tính tiền và được chốt lại trong 8 cột snapshot của
 * chi tiết hóa đơn khi thanh toán (RB46).
 *
 * <p>Bộ điều khiển chỉ giữ trạng thái của một hóa đơn đang lập trong bộ nhớ; mọi thao tác đọc/ghi
 * kho, khuyến mãi, hóa đơn và hóa đơn tạm đều đi qua lớp DAO được truyền vào. Việc ghi hóa đơn, ghi
 * chi tiết và đánh dấu máy đã bán nằm trong một giao dịch duy nhất trên kết nối do bộ điều khiển mở.
 */
public final class BanHangController {

  private static final BigDecimal KHONG = BigDecimal.ZERO.setScale(0);
  private static final BigDecimal TRAM = BigDecimal.valueOf(100);
  private static final int SO_KET_QUA_TOI_DA = 20;

  private final SanPhamDao sanPhamDao;
  private final HoaDonTamDao hoaDonTamDao;
  private final KhuyenMaiDao khuyenMaiDao;
  private final HoaDonDao hoaDonDao;
  private final ChiTietHoaDonDao chiTietHoaDonDao;
  private final DataSource dataSource;
  private final Clock clock;
  private final LinkedHashMap<String, Integer> soLuongTheoDong = new LinkedHashMap<>();
  private String maTamHienTai;

  /**
   * Khởi tạo bộ điều khiển chỉ dùng cho lập hóa đơn và hóa đơn tạm.
   *
   * <p>Các phụ thuộc phục vụ khuyến mãi và thanh toán được để {@code null}; gọi
   * {@link #thanhToan(NhanVien, KhachHang, long)} trên bộ điều khiển này sẽ báo
   * {@link IllegalStateException}.
   *
   * @param sanPhamDao lớp truy cập sản phẩm; không được {@code null}
   * @param hoaDonTamDao lớp lưu hóa đơn tạm; không được {@code null}
   */
  public BanHangController(SanPhamDao sanPhamDao, HoaDonTamDao hoaDonTamDao) {
    this(sanPhamDao, null, null, null, hoaDonTamDao, null, Clock.systemDefaultZone());
  }

  /**
   * Khởi tạo đầy đủ bộ điều khiển bán hàng cho luồng thanh toán.
   *
   * <p>Các DAO và nguồn dữ liệu có thể {@code null} khi chỉ cần lập hóa đơn trong bộ nhớ; khi đó
   * khuyến mãi không được áp và {@link #thanhToan(NhanVien, KhachHang, long)} báo
   * {@link IllegalStateException}.
   *
   * @param sanPhamDao lớp truy cập sản phẩm; không được {@code null}
   * @param khuyenMaiDao lớp tra cứu khuyến mãi; có thể {@code null}
   * @param hoaDonDao lớp ghi hóa đơn; có thể {@code null}
   * @param chiTietHoaDonDao lớp ghi chi tiết hóa đơn; có thể {@code null}
   * @param hoaDonTamDao lớp lưu hóa đơn tạm; không được {@code null}
   * @param dataSource nguồn dữ liệu dùng để mở giao dịch thanh toán; có thể {@code null}
   * @param clock đồng hồ xác định ngày áp khuyến mãi và ngày lập hóa đơn; {@code null} thì dùng
   *     đồng hồ hệ thống
   */
  public BanHangController(
      SanPhamDao sanPhamDao,
      KhuyenMaiDao khuyenMaiDao,
      HoaDonDao hoaDonDao,
      ChiTietHoaDonDao chiTietHoaDonDao,
      HoaDonTamDao hoaDonTamDao,
      DataSource dataSource,
      Clock clock) {
    this.sanPhamDao = Objects.requireNonNull(sanPhamDao, "sanPhamDao");
    this.hoaDonTamDao = Objects.requireNonNull(hoaDonTamDao, "hoaDonTamDao");
    this.khuyenMaiDao = khuyenMaiDao;
    this.hoaDonDao = hoaDonDao;
    this.chiTietHoaDonDao = chiTietHoaDonDao;
    this.dataSource = dataSource;
    this.clock = clock;
  }

  /**
   * Tính tiền của một dòng hóa đơn theo đơn vị máy, sau đó nhân với số lượng.
   *
   * <p>Đơn giá, tiền giảm giá và tiền thuế được làm tròn <b>một lần cho một máy</b> rồi mới nhân
   * với số lượng; không làm tròn lại tổng của dòng. Thuế được tính trên đơn giá <b>sau khuyến mãi</b>.
   * Nhờ vậy tổng của dòng bằng đúng tổng của {@code soLuong} dòng chi tiết hóa đơn (mỗi dòng một máy)
   * sẽ ghi khi thanh toán.
   *
   * @param donGiaTruocThue đơn giá bán trước thuế của một máy; không được {@code null}
   * @param thueSuat thuế suất tính theo phần trăm; không được {@code null}
   * @param soLuong số lượng máy của dòng; phải lớn hơn {@code 0}
   * @param tyLeGiam tỷ lệ giảm giá theo phần trăm; {@code null} được coi là {@code 0}
   * @return tiền của dòng đã làm tròn
   * @throws IllegalArgumentException nếu tham số bắt buộc {@code null} hoặc số lượng không hợp lệ
   */
  public static DongTinh tinhDong(
      BigDecimal donGiaTruocThue, BigDecimal thueSuat, int soLuong, BigDecimal tyLeGiam) {
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
    BigDecimal tiLeGiam = tyLeGiam == null ? BigDecimal.ZERO : tyLeGiam;
    BigDecimal motMay = TienUtil.lamTron(donGiaTruocThue);
    BigDecimal giamMotMay = TienUtil.lamTron(motMay.multiply(tiLeGiam).divide(TRAM));
    BigDecimal sauGiamMotMay = motMay.subtract(giamMotMay);
    BigDecimal thueMotMay = TienUtil.lamTron(sauGiamMotMay.multiply(thueSuat).divide(TRAM));
    return new DongTinh(
        motMay.multiply(soMay),
        giamMotMay.multiply(soMay),
        sauGiamMotMay.multiply(soMay),
        thueMotMay.multiply(soMay));
  }

  /**
   * Tính tiền của một dòng hóa đơn không có khuyến mãi.
   *
   * @param donGiaTruocThue đơn giá bán trước thuế của một máy; không được {@code null}
   * @param thueSuat thuế suất tính theo phần trăm; không được {@code null}
   * @param soLuong số lượng máy của dòng; phải lớn hơn {@code 0}
   * @return tiền của dòng đã làm tròn, tiền khuyến mãi bằng {@code 0}
   * @throws IllegalArgumentException nếu tham số {@code null} hoặc số lượng không hợp lệ
   */
  public static DongTinh tinhDong(BigDecimal donGiaTruocThue, BigDecimal thueSuat, int soLuong) {
    return tinhDong(donGiaTruocThue, thueSuat, soLuong, BigDecimal.ZERO);
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
   * làm tròn) nhân với số lượng. Khuyến mãi đang hiệu lực của từng dòng sản phẩm được tra theo ngày
   * hiện tại của {@code clock}. Tổng tiền là tổng các giá trị đã làm tròn ở cấp dòng; không làm tròn
   * lại tổng (C-11).
   *
   * @return hóa đơn hiện tại; hóa đơn rỗng nếu giỏ hàng trống
   * @throws DataAccessException nếu truy vấn khuyến mãi thất bại
   */
  public HoaDonTinh hoaDonHienTai() {
    return tinhHoaDon(docMay());
  }

  private HoaDonTinh tinhHoaDon(List<SanPham> may) {
    if (soLuongTheoDong.isEmpty()) {
      return HoaDonTinh.rong();
    }
    Map<String, DongBan> dongSanPham = docDongSanPham(may);
    List<DongGio> dongHang = new ArrayList<>(soLuongTheoDong.size());
    BigDecimal tongTruocThue = KHONG;
    BigDecimal tongKhuyenMai = KHONG;
    BigDecimal tongThue = KHONG;
    for (Map.Entry<String, Integer> entry : soLuongTheoDong.entrySet()) {
      DongBan dong = dongSanPham.get(entry.getKey());
      if (dong == null) {
        continue;
      }
      int soLuong = entry.getValue();
      KhuyenMai khuyenMai = timKhuyenMai(dong.maDongSP());
      BigDecimal tyLeGiam = khuyenMai == null ? null : khuyenMai.tyLeGiam();
      DongTinh tien =
          tinhDong(dong.donGiaTruocThue(), dong.thueSuat(), soLuong, tyLeGiam);
      dongHang.add(new DongGio(
          dong.maDongSP(),
          dong.tenDongSP(),
          soLuong,
          dong.tonKho(),
          dong.donGiaTruocThue(),
          dong.thueSuat(),
          tien.tienSauKhuyenMai().add(tien.tienThue()),
          khuyenMai,
          tyLeGiam,
          tien.tienKhuyenMai()));
      tongTruocThue = tongTruocThue.add(tien.tienSauKhuyenMai());
      tongKhuyenMai = tongKhuyenMai.add(tien.tienKhuyenMai());
      tongThue = tongThue.add(tien.tienThue());
    }
    if (dongHang.isEmpty()) {
      return HoaDonTinh.rong();
    }
    return new HoaDonTinh(
        List.copyOf(dongHang), tongTruocThue, tongKhuyenMai, tongThue,
        tongTruocThue.add(tongThue));
  }

  /**
   * Tra chương trình khuyến mãi đang hiệu lực của một dòng sản phẩm.
   *
   * <p>Nếu chưa cấu hình {@link KhuyenMaiDao} thì coi như dòng không có khuyến mãi. Lỗi truy cập dữ
   * liệu được truyền nguyên vẹn cho bên gọi, không bị nuốt.
   *
   * @param maDongSP mã dòng sản phẩm cần tra
   * @return chương trình khuyến mãi hiện hành, hoặc {@code null} nếu không có
   * @throws DataAccessException nếu truy vấn khuyến mãi thất bại
   */
  private KhuyenMai timKhuyenMai(String maDongSP) {
    if (khuyenMaiDao == null || maDongSP == null) {
      return null;
    }
    return khuyenMaiDao.findHieuLuc(maDongSP, LocalDate.now(dongHo())).orElse(null);
  }

  private Clock dongHo() {
    return clock == null ? Clock.systemDefaultZone() : clock;
  }

  /**
   * Phân bổ máy cụ thể cho từng dòng giỏ hàng theo nguyên tắc nhập kho lâu nhất trước (FIFO).
   *
   * <p>Máy được chọn trong số máy {@link TrangThaiSanPham#SAN_SANG_BAN} của đúng dòng sản phẩm và
   * thuộc dòng còn kinh doanh, sắp xếp theo {@code ngayNhapKhoBanDau} tăng dần (ngày trống xếp cuối),
   * cùng ngày thì theo số serial tăng dần. Nếu không đủ máy cho một dòng thì mã dòng đó được báo lại
   * trong {@link KetQuaPhanBo#dongThieu()}.
   *
   * @return danh sách máy được chọn và các dòng thiếu máy; không bao giờ {@code null}
   */
  public KetQuaPhanBo phanBoMay() {
    return phanBoMay(docMay());
  }

  private KetQuaPhanBo phanBoMay(List<SanPham> may) {
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

  /**
   * Thanh toán hóa đơn đang lập bằng tiền mặt trong một giao dịch duy nhất.
   *
   * <p>Luồng xử lý: kiểm tra giỏ hàng, khách hàng và số tiền khách đưa; phân bổ máy theo FIFO; mở
   * giao dịch trên nguồn dữ liệu đã cấu hình; sinh mã hóa đơn, ghi hóa đơn, ghi từng dòng chi tiết
   * (mỗi máy một dòng, {@code soLuong = 1}) kèm 8 cột snapshot giá/thuế/khuyến mãi tại thời điểm bán,
   * rồi đánh dấu máy đã bán bằng câu lệnh có điều kiện (chống bán trùng, RB40). Toàn bộ được cam kết
   * hoặc quay lui cùng nhau; không cập nhật tay {@code DongSanPham.soLuongTon} vì trigger cơ sở dữ
   * liệu tự tính lại.
   *
   * <p>Sau khi cam kết thành công, hóa đơn tạm đang gắn (nếu có) bị xóa và giỏ hàng được làm rỗng.
   * Nếu thất bại, giỏ hàng được giữ nguyên để người dùng thử lại.
   *
   * @param nhanVien nhân viên lập hóa đơn
   * @param khachHang khách hàng của hóa đơn; bắt buộc
   * @param tienKhachDua số tiền mặt khách đưa, đơn vị VND
   * @return kết quả thanh toán; không bao giờ {@code null}
   * @throws IllegalStateException nếu bộ điều khiển chưa được cấu hình đủ DAO và nguồn dữ liệu
   * @throws DataAccessException nếu ghi cơ sở dữ liệu thất bại
   */
  public KetQuaThanhToan thanhToan(NhanVien nhanVien, KhachHang khachHang, long tienKhachDua) {
    if (khuyenMaiDao == null || hoaDonDao == null || chiTietHoaDonDao == null
        || dataSource == null) {
      throw new IllegalStateException("Chưa cấu hình thanh toán");
    }
    if (soLuongTheoDong.isEmpty()) {
      return thatBai("Hóa đơn chưa có sản phẩm");
    }
    if (khachHang == null) {
      return thatBai("Vui lòng chọn khách hàng");
    }
    List<SanPham> may = docMay();
    HoaDonTinh hoaDon = tinhHoaDon(may);
    if (hoaDon.dongHang().isEmpty()) {
      return thatBai("Hóa đơn chưa có sản phẩm");
    }
    long tongThanhToan = hoaDon.tongThanhToan().longValue();
    if (tienKhachDua < tongThanhToan) {
      return thatBai("Số tiền khách đưa chưa đủ");
    }
    KetQuaPhanBo phanBo = phanBoMay(may);
    if (!phanBo.dongThieu().isEmpty()) {
      return thatBai(
          "Dòng sản phẩm " + phanBo.dongThieu().getFirst() + " không đủ máy để bán");
    }
    Map<String, SanPham> mayTheoSerial = new HashMap<>();
    for (SanPham sanPham : may) {
      if (sanPham != null && sanPham.soSerial() != null) {
        mayTheoSerial.put(sanPham.soSerial(), sanPham);
      }
    }
    Map<String, DongGio> dongTheoMa = new HashMap<>();
    for (DongGio dong : hoaDon.dongHang()) {
      dongTheoMa.put(dong.maDongSP(), dong);
    }
    List<ChiTietHoaDon> daGhi = new ArrayList<>();
    String maHD;
    try {
      maHD = ghiHoaDon(nhanVien, khachHang, phanBo, mayTheoSerial, dongTheoMa, daGhi);
    } catch (MayVuaBanException e) {
      return thatBai(e.getMessage());
    }
    long tongTruocThue = 0;
    long tongGiamGia = 0;
    long tongThue = 0;
    for (ChiTietHoaDon chiTiet : daGhi) {
      tongTruocThue += chiTiet.tienSauKhuyenMai().longValue();
      tongGiamGia += chiTiet.tienKhuyenMaiThucTe().longValue();
      tongThue += chiTiet.tienThue().longValue();
    }
    long tongPhaiTra = tongTruocThue + tongThue;
    String maTam = maTamHienTai;
    if (maTam != null) {
      hoaDonTamDao.xoa(maTam);
    }
    xoaGio();
    return new KetQuaThanhToan(
        true,
        "Thanh toán thành công",
        maHD,
        tongTruocThue,
        tongGiamGia,
        tongThue,
        tongPhaiTra,
        tienKhachDua,
        tienKhachDua - tongPhaiTra,
        List.copyOf(daGhi));
  }

  private String ghiHoaDon(
      NhanVien nhanVien,
      KhachHang khachHang,
      KetQuaPhanBo phanBo,
      Map<String, SanPham> mayTheoSerial,
      Map<String, DongGio> dongTheoMa,
      List<ChiTietHoaDon> daGhi) {
    try (Connection ketNoi = dataSource.getConnection()) {
      boolean autoCommitCu = ketNoi.getAutoCommit();
      try {
        ketNoi.setAutoCommit(false);
        String maHD = MaGenerator.next(ketNoi, "HoaDon", "maHD", "HD");
        HoaDon hoaDon = new HoaDon(
            maHD, LocalDate.now(dongHo()), nhanVien, khachHang, true);
        hoaDonDao.insert(ketNoi, hoaDon);
        for (MayDuocChon chon : phanBo.mayDuocChon()) {
          SanPham sanPham = mayTheoSerial.get(chon.soSerial());
          DongGio dong = dongTheoMa.get(chon.maDongSP());
          if (sanPham == null || dong == null) {
            throw new IllegalStateException(
                "Máy " + chon.soSerial() + " không còn trong kho");
          }
          DongTinh tien =
              tinhDong(dong.donGiaTruocThue(), dong.thueSuat(), 1, dong.tyLeGiam());
          ChiTietHoaDon chiTiet = new ChiTietHoaDon(
              hoaDon,
              sanPham,
              1,
              TienUtil.lamTron(dong.donGiaTruocThue()),
              dong.thueSuat(),
              dong.khuyenMai(),
              dong.tyLeGiam(),
              tien.tienKhuyenMai(),
              tien.tienTruocKhuyenMai(),
              tien.tienSauKhuyenMai(),
              tien.tienThue());
          chiTietHoaDonDao.insert(ketNoi, chiTiet);
          if (!sanPhamDao.danhDauDaBan(ketNoi, chon.soSerial())) {
            throw new MayVuaBanException(
                "Máy " + chon.soSerial() + " vừa được bán, vui lòng chọn lại");
          }
          daGhi.add(chiTiet);
        }
        ketNoi.commit();
        return maHD;
      } catch (RuntimeException | SQLException e) {
        try {
          ketNoi.rollback();
        } catch (SQLException loiQuayLui) {
          e.addSuppressed(loiQuayLui);
        }
        if (e instanceof RuntimeException loi) {
          throw loi;
        }
        throw new DataAccessException("Không thể thanh toán hóa đơn", e);
      } finally {
        try {
          ketNoi.setAutoCommit(autoCommitCu);
        } catch (SQLException ignored) {
          // Kết nối được đóng ngay sau đây; không còn thao tác nào phụ thuộc chế độ cam kết.
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException("Không thể thanh toán hóa đơn", e);
    }
  }

  private static KetQuaThanhToan thatBai(String thongBao) {
    return new KetQuaThanhToan(
        false, thongBao, null, 0, 0, 0, 0, 0, 0, List.of());
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
   * @param khuyenMai chương trình khuyến mãi đang áp cho dòng; có thể {@code null}
   * @param tyLeGiam tỷ lệ giảm giá theo phần trăm đã áp; có thể {@code null}
   * @param tienKhuyenMai tiền được giảm giá của cả dòng
   */
  public record DongGio(
      String maDongSP, String tenDongSP, int soLuong, int tonKho,
      BigDecimal donGiaTruocThue, BigDecimal thueSuat,
      BigDecimal thanhTienSauThue, KhuyenMai khuyenMai, BigDecimal tyLeGiam,
      BigDecimal tienKhuyenMai) {
  }

  /**
   * Hóa đơn đang lập cùng các tổng tiền.
   *
   * @param dongHang danh sách dòng hàng
   * @param tongTruocThue tổng tiền trước thuế sau khuyến mãi
   * @param tongKhuyenMai tổng tiền được giảm giá
   * @param tongThue tổng tiền thuế
   * @param tongThanhToan tổng tiền khách phải trả
   */
  public record HoaDonTinh(
      List<DongGio> dongHang, BigDecimal tongTruocThue,
      BigDecimal tongKhuyenMai, BigDecimal tongThue, BigDecimal tongThanhToan) {

    /**
     * Hóa đơn rỗng với mọi tổng tiền bằng {@code 0}.
     *
     * @return hóa đơn không có dòng hàng
     */
    public static HoaDonTinh rong() {
      return new HoaDonTinh(List.of(), KHONG, KHONG, KHONG, KHONG);
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

  /**
   * Kết quả thanh toán hóa đơn.
   *
   * @param thanhCong {@code true} nếu hóa đơn đã được ghi và cam kết
   * @param thongBao thông báo tiếng Việt cho người dùng
   * @param maHD mã hóa đơn vừa tạo; {@code null} khi thanh toán thất bại
   * @param tongTruocThue tổng tiền trước thuế sau khuyến mãi của các dòng đã ghi
   * @param tongGiamGia tổng tiền được giảm giá của các dòng đã ghi
   * @param tongThue tổng tiền thuế của các dòng đã ghi
   * @param tongThanhToan tổng tiền khách phải trả
   * @param tienKhachDua số tiền khách đưa
   * @param tienTraLai số tiền thối lại khách
   * @param chiTiet các dòng chi tiết đã ghi
   */
  public record KetQuaThanhToan(
      boolean thanhCong, String thongBao, String maHD,
      long tongTruocThue, long tongGiamGia, long tongThue, long tongThanhToan,
      long tienKhachDua, long tienTraLai, List<ChiTietHoaDon> chiTiet) {
  }

  /** Máy đã bị giao dịch khác bán trước khi kịp đánh dấu; giao dịch hiện tại phải quay lui. */
  private static final class MayVuaBanException extends IllegalStateException {

    MayVuaBanException(String thongBao) {
      super(thongBao);
    }
  }
}
