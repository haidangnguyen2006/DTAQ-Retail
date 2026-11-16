package com.dtaq.retail.ui.component;

import com.dtaq.retail.dao.Trang;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Nạp dần dữ liệu cho một bảng có {@link Pagination}.
 *
 * <p>Chỉ trang đang xem được nạp; sang trang nào mới nạp trang đó. Trang đã nạp được giữ lại trong
 * phiên nên quay lại không truy vấn lại. Bộ nhớ đệm chỉ bị bỏ khi người dùng tìm kiếm ({@link
 * #datNguon}) hoặc dữ liệu vừa được thay đổi ({@link #lamMoi}).
 *
 * @param <T> kiểu của một dòng
 */
public final class PagedLoader<T> {

  /** Nguồn trả về một trang theo vị trí bắt đầu và số dòng. */
  @FunctionalInterface
  public interface Nguon<T> {
    Trang<T> tai(int boQua, int gioiHan);
  }

  private final Pagination pagination;
  private final int kichThuoc;
  private final String donVi;
  private final Consumer<List<T>> ve;
  private final Map<Integer, List<T>> daTai = new HashMap<>();
  private Nguon<T> nguon;
  private boolean nen;
  private Runnable khiDangTai = () -> { };
  private Consumer<Throwable> khiLoi = loi -> { };
  private List<T> dongHienTai = List.of();
  private int tongSo;
  private int trang = 1;
  private int phien;

  /**
   * @param pagination thanh phân trang của bảng
   * @param kichThuoc số dòng mỗi trang
   * @param donVi tên gọi của một dòng để ghép câu "Hiển thị 1–20 trên 135 máy"
   * @param ve hàm vẽ các dòng của trang hiện tại lên bảng (nhận danh sách rỗng khi không có dữ liệu)
   */
  public PagedLoader(Pagination pagination, int kichThuoc, String donVi, Consumer<List<T>> ve) {
    this.pagination = Objects.requireNonNull(pagination, "pagination");
    this.kichThuoc = kichThuoc;
    this.donVi = donVi;
    this.ve = Objects.requireNonNull(ve, "ve");
    pagination.setInfo("");
    pagination.setOnPageChangeListener(this::hien);
  }

  /** Nạp từng trang ở luồng nền thay vì trên luồng giao diện. */
  public PagedLoader<T> chayNen(Runnable khiDangTai, Consumer<Throwable> khiLoi) {
    this.nen = true;
    this.khiDangTai = Objects.requireNonNull(khiDangTai, "khiDangTai");
    this.khiLoi = Objects.requireNonNull(khiLoi, "khiLoi");
    return this;
  }

  /** Đặt nguồn mới (người dùng vừa tìm kiếm hoặc đổi bộ lọc) và nạp trang đầu. */
  public void datNguon(Nguon<T> nguonMoi) {
    this.nguon = Objects.requireNonNull(nguonMoi, "nguonMoi");
    boDem();
    hien(1);
  }

  /** Phân trang một danh sách đã nằm sẵn trong bộ nhớ; không truy vấn thêm khi chuyển trang. */
  public void datDanhSach(List<T> tatCa) {
    List<T> banSao = List.copyOf(tatCa);
    this.nguon = null;
    boDem();
    this.tongSo = banSao.size();
    for (int i = 1; i <= Pagination.totalPages(tongSo, kichThuoc); i++) {
      daTai.put(i, Trang.cat(banSao, (i - 1) * kichThuoc, kichThuoc).dong());
    }
    hien(1);
  }

  /** Dữ liệu vừa thay đổi: bỏ bộ nhớ đệm và nạp lại đúng trang đang xem. */
  public void lamMoi() {
    if (nguon == null) {
      return;
    }
    boDem();
    hien(trang);
  }

  /**
   * Như {@link #lamMoi} nhưng nạp ngay trên luồng gọi, để mã phía sau dùng được dữ liệu mới (chọn lại
   * dòng vừa sửa). Chỉ dùng sau một thao tác của người dùng: đây là một truy vấn một trang.
   */
  public void lamMoiDongBo() {
    boolean nenCu = nen;
    nen = false;
    try {
      lamMoi();
    } finally {
      nen = nenCu;
    }
  }

  /** Chuyển tới một trang; trang chưa nạp thì nạp, trang đã nạp thì dùng lại. */
  public void chuyenTrang(int trangCanHien) {
    hien(trangCanHien);
  }

  /** Đã có nguồn dữ liệu hay chưa (chưa thì bảng chưa từng được nạp). */
  public boolean daCoNguon() {
    return nguon != null;
  }

  /** Các dòng đang hiển thị; chỉ số trùng với chỉ số dòng của model bảng. */
  public List<T> dongHienTai() {
    return dongHienTai;
  }

  /** Dòng đang hiển thị tại chỉ số model, hoặc {@code null} nếu ngoài phạm vi. */
  public T dong(int chiSo) {
    return chiSo >= 0 && chiSo < dongHienTai.size() ? dongHienTai.get(chiSo) : null;
  }

  public int tongSo() {
    return tongSo;
  }

  private void boDem() {
    daTai.clear();
    phien++;
  }

  private void hien(int trangCanHien) {
    int so = Math.max(1, trangCanHien);
    List<T> coSan = daTai.get(so);
    if (coSan != null || nguon == null) {
      apDung(so, coSan == null ? List.of() : coSan);
      return;
    }
    int phienYeuCau = phien;
    Nguon<T> nguonYeuCau = nguon;
    if (!nen) {
      nhan(phienYeuCau, so, nguonYeuCau.tai((so - 1) * kichThuoc, kichThuoc));
      return;
    }
    khiDangTai.run();
    new SwingWorker<Trang<T>, Void>() {
      @Override
      protected Trang<T> doInBackground() {
        return nguonYeuCau.tai((so - 1) * kichThuoc, kichThuoc);
      }

      @Override
      protected void done() {
        try {
          nhan(phienYeuCau, so, get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          khiLoi.accept(e);
        } catch (ExecutionException e) {
          khiLoi.accept(e.getCause() == null ? e : e.getCause());
        }
      }
    }.execute();
  }

  private void nhan(int phienYeuCau, int so, Trang<T> ketQua) {
    if (phienYeuCau != phien) {
      return; // người dùng đã tìm kiếm lại trong lúc chờ
    }
    tongSo = ketQua.tongSo();
    int soTrang = Pagination.totalPages(tongSo, kichThuoc);
    if (ketQua.dong().isEmpty() && so > 1) {
      // Trang đang xem không còn dòng nào (vừa xóa dòng cuối, hoặc dữ liệu đã thay đổi): về trang gần nhất còn dữ liệu.
      hien(tongSo == 0 ? 1 : Math.min(so - 1, soTrang));
      return;
    }
    daTai.put(so, List.copyOf(ketQua.dong()));
    apDung(so, daTai.get(so));
  }

  private void apDung(int so, List<T> dong) {
    trang = so;
    dongHienTai = dong;
    int tu = (so - 1) * kichThuoc + 1;
    pagination.setPageData(so, Pagination.totalPages(tongSo, kichThuoc), dong.isEmpty()
        ? "Không có " + donVi + " nào"
        : "Hiển thị " + tu + "–" + (tu + dong.size() - 1) + " trên " + tongSo + " " + donVi);
    ve.accept(dong);
  }
}
