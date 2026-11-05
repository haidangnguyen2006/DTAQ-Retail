package com.dtaq.retail.ui.page.returns;

import com.dtaq.retail.controller.DoiTraController;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.component.table.TableRenderers;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình tiếp nhận đổi/trả sản phẩm (PageId.RETURNS, UC006).
 *
 * <p>Trang chỉ hiển thị dữ liệu do {@link DoiTraController} cung cấp và chuyển thao tác của nhân
 * viên xuống bộ điều khiển: tra cứu hóa đơn gốc, chọn máy, tính trước tiền hoàn/chênh lệch, ghi
 * phiếu và hoàn tất phiếu. Mọi kiểm tra nghiệp vụ, ngưỡng phê duyệt và giao dịch nằm ở controller;
 * giao diện không chứa SQL và không làm tròn tiền (chỉ dùng {@link MoneyFormat} để hiển thị).
 */
public final class ReturnsPage implements Page {

  private static final Logger LOG = LoggerFactory.getLogger(ReturnsPage.class);

  private static final String LOI_QUYEN = "Chỉ Nhân viên bán hàng được xử lý đổi trả";
  private static final String LOI_TRA_CUU = "Không thể tra cứu hóa đơn gốc";
  private static final String LOI_TINH_PHIEU = "Không thể tính tiền đổi/trả";
  private static final String LOI_GHI_PHIEU = "Không thể ghi phiếu đổi/trả";
  private static final String LOI_HOAN_TAT = "Không thể hoàn tất phiếu";
  private static final String LOI_TAI_DANH_SACH = "Không thể tải danh sách phiếu chờ hoàn tất";
  private static final String CANH_BAO_NGUONG =
      "Số tiền hoàn từ 3.000.000 VND trở lên: phiếu sẽ được lưu ở trạng thái chờ Quản lý phê duyệt.";
  private static final String LOI_THIEU_SERIAL_MAY_THAY_THE =
      "Vui lòng nhập serial máy thay thế";

  private final DoiTraController controller;
  private final JPanel view;

  private final FormField maHoaDonField =
      FormField.text("Mã hóa đơn", "Ví dụ HD-2026-000001", "search");
  private final DtaqButton traCuuButton = DtaqButton.secondary("Tra cứu", "search");
  private final JPanel thongTinHoaDon = new JPanel(
      new MigLayout("fillx, insets 0, gapx 24", "[][grow][][grow][][grow]", "[]"));
  private final JLabel tenKhachHangLabel = giaTriNhan();
  private final JLabel soDienThoaiLabel = giaTriNhan();
  private final JLabel ngayLapLabel = giaTriNhan();
  private final InfoCallout loiTraCuu = new InfoCallout(InfoCallout.Kind.DANGER, "");

  private final DefaultTableModel mayModel = DataTable.readOnlyModel(
      new String[] {"Số serial", "Dòng sản phẩm", "Giá trị thanh toán", "Trạng thái", ""}, 4);
  private final DataTable mayTable = new DataTable(mayModel, 50);
  private final StatefulView mayState;
  private final List<DoiTraController.MayDaBan> mayCache = new ArrayList<>();

  private final FormField tinhTrangField =
      FormField.combo("Tình trạng máy", new String[] {"Nguyên vẹn", "Hàng lỗi"});
  private final FormField lyDoField =
      FormField.text("Lý do trả/đổi", "Ví dụ khách đổi sang mẫu khác", null);
  private final SegmentedControl hinhThuc =
      new SegmentedControl(new String[] {"Trả hàng", "Đổi máy"}, 0);
  private final FormField serialThayTheField =
      FormField.text("Serial máy thay thế", "Nhập serial máy khách nhận", "search");
  private final JPanel serialPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]"));

  private final JPanel rowMayMoi;
  private final JPanel rowChenhLech;
  private final JPanel rowSoTienHoan;
  private final JLabel giaTriMayTraLabel = giaTriTien();
  private final JLabel giaTriMayMoiLabel = giaTriTien();
  private final JLabel chenhLechLabel = giaTriTien();
  private final JLabel soTienHoanLabel = giaTriTien();
  private final InfoCallout canhBaoNguong = new InfoCallout(InfoCallout.Kind.WARNING, CANH_BAO_NGUONG);
  private final InfoCallout loiTinh = new InfoCallout(InfoCallout.Kind.DANGER, "");
  private final DtaqButton hoanTatButton = DtaqButton.primary("Hoàn tất phiếu", "circle-check");

  private final DefaultTableModel phieuModel = DataTable.readOnlyModel(
      new String[] {"Mã phiếu", "Hóa đơn gốc", "Khách hàng", "Số điện thoại", "Số tiền hoàn",
          "Ngày lập", ""},
      6);
  private final DataTable phieuTable = new DataTable(phieuModel, 50);
  private final StatefulView phieuState;
  private final List<PhieuDuyet> phieuCache = new ArrayList<>();

  private final Timer henXemTruoc = new Timer(350, e -> capNhatXemTruoc());
  private DoiTraController.KetQuaTraCuu hoaDonHienTai;
  private DoiTraController.MayDaBan mayDuocChon;
  private int lanTraCuu;
  private int lanXemTruoc;
  private int lanTaiDanhSach;

  /**
   * Khởi tạo trang xử lý đổi trả.
   *
   * @param controller bộ điều khiển đổi/trả; không được {@code null}
   */
  public ReturnsPage(DoiTraController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[]"));
    view.setBackground(DtaqColors.BG_APP);

    mayState = new StatefulView(
        mayTable, "package-search", "Chưa có máy nào để đổi/trả",
        "Tra cứu hóa đơn gốc để bắt đầu.");
    mayState.setOnRetry(this::traCuuHoaDon);
    phieuState = new StatefulView(
        phieuTable, "circle-check", "Không có phiếu nào chờ hoàn tất",
        "Các phiếu đã được Quản lý duyệt sẽ hiển thị tại đây.");
    phieuState.setOnRetry(this::taiDanhSachChoHoanTat);

    rowMayMoi = dongTien("Giá trị máy mới", giaTriMayMoiLabel, false);
    rowChenhLech = dongTien("Chênh lệch", chenhLechLabel, false);
    rowSoTienHoan = dongTien("Số tiền hoàn", soTienHoanLabel, true);
    rowMayMoi.setVisible(false);
    rowChenhLech.setVisible(false);

    view.add(new PageHeader(
        "Xử lý đổi trả sản phẩm",
        "Tiếp nhận máy khách trả lại hoặc đổi sang máy khác. Phiếu hoàn tiền từ 3.000.000 VND "
            + "trở lên cần Quản lý phê duyệt."), "growx, wrap");

    view.add(buildHoaDonCard(), "growx, wrap");
    view.add(buildMayCard(), "growx, wrap");
    view.add(buildThongTinCard(), "growx, wrap");
    view.add(buildTinhTienCard(), "growx, wrap");
    view.add(buildPhieuCard(), "growx, wrap");

    henXemTruoc.setRepeats(false);
    caiDatSuKien();
    refreshView();
  }

  @Override
  public PageId id() {
    return PageId.RETURNS;
  }

  @Override
  public String title() {
    return "Xử lý đổi trả sản phẩm";
  }

  @Override
  public String section() {
    return "Bán hàng";
  }

  @Override
  public JComponent view() {
    return view;
  }

  @Override
  public boolean scrollable() {
    return true;
  }

  @Override
  public void onShow() {
    taiDanhSachChoHoanTat();
  }

  /**
   * Tra cứu hóa đơn đang nhập trên form.
   *
   * @throws SecurityException không ném trực tiếp; lỗi quyền được hiển thị trên giao diện
   */
  public void traCuuHoaDon() {
    traCuuHoaDon(maHoaDonField.getText());
  }

  /**
   * Tra cứu hóa đơn theo mã và hiển thị danh sách máy đã bán.
   *
   * @param maHD mã hóa đơn gốc; rỗng sẽ hiện lỗi nhập liệu
   */
  public void traCuuHoaDon(String maHD) {
    String ma = maHD == null ? "" : maHD.trim();
    maHoaDonField.setText(ma);
    maHoaDonField.setError(null);
    loiTraCuu.setVisible(false);
    int lan = ++lanTraCuu;
    mayState.showLoading();

    new SwingWorker<DoiTraController.KetQuaTraCuu, Void>() {
      @Override
      protected DoiTraController.KetQuaTraCuu doInBackground() {
        return controller.traCuuHoaDon(ma);
      }

      @Override
      protected void done() {
        if (lan != lanTraCuu) {
          return;
        }
        try {
          hienThiTraCuu(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          hienThiLoiTraCuu(LOI_TRA_CUU);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            hienThiLoiTraCuu(LOI_QUYEN);
          } else {
            LOG.error("Không thể tra cứu hóa đơn đổi/trả", e.getCause());
            hienThiLoiTraCuu(LOI_TRA_CUU);
          }
        } catch (RuntimeException e) {
          LOG.error("Không thể tra cứu hóa đơn đổi/trả", e);
          hienThiLoiTraCuu(LOI_TRA_CUU);
        }
      }
    }.execute();
  }

  /**
   * Chọn một máy trong bảng "Máy đã bán" và tính trước tiền hoàn/chênh lệch.
   *
   * @param modelRow chỉ số dòng theo model của bảng máy đã bán
   */
  public void chonMay(int modelRow) {
    if (modelRow < 0 || modelRow >= mayCache.size()) {
      return;
    }
    DoiTraController.MayDaBan may = mayCache.get(modelRow);
    if (may.daDoiTra()) {
      Toast.error(view, "Máy " + may.soSerial() + " đã được đổi/trả trước đó");
      return;
    }
    mayDuocChon = may;
    capNhatXemTruoc();
  }

  /** Tính lại phần xem trước tiền hoàn/chênh lệch ở luồng nền. */
  public void capNhatXemTruoc() {
    DoiTraController.YeuCauDoiTra yeuCau = yeuCauHienTai();
    if (yeuCau == null) {
      xoaKetQuaTinh();
      return;
    }
    int lan = ++lanXemTruoc;
    new SwingWorker<DoiTraController.KetQuaTinh, Void>() {
      @Override
      protected DoiTraController.KetQuaTinh doInBackground() {
        return controller.tinhPhieu(yeuCau);
      }

      @Override
      protected void done() {
        if (lan != lanXemTruoc) {
          return;
        }
        try {
          hienThiKetQuaTinh(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            Toast.error(view, LOI_QUYEN);
          } else {
            LOG.error("Không thể tính tiền đổi/trả", e.getCause());
            Toast.error(view, LOI_TINH_PHIEU);
          }
        } catch (RuntimeException e) {
          LOG.error("Không thể tính tiền đổi/trả", e);
          Toast.error(view, LOI_TINH_PHIEU);
        }
      }
    }.execute();
  }

  /** Tải danh sách phiếu đã duyệt, đang chờ hoàn tất. */
  public void taiDanhSachChoHoanTat() {
    int lan = ++lanTaiDanhSach;
    phieuState.showLoading();
    new SwingWorker<List<PhieuDuyet>, Void>() {
      @Override
      protected List<PhieuDuyet> doInBackground() {
        return controller.danhSachChoHoanTat();
      }

      @Override
      protected void done() {
        if (lan != lanTaiDanhSach) {
          return;
        }
        try {
          hienThiDanhSachChoHoanTat(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          phieuState.showError(LOI_TAI_DANH_SACH);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            phieuState.showError(LOI_QUYEN);
          } else {
            LOG.error("Không thể tải danh sách phiếu chờ hoàn tất", e.getCause());
            phieuState.showError(LOI_TAI_DANH_SACH);
          }
        } catch (RuntimeException e) {
          LOG.error("Không thể tải danh sách phiếu chờ hoàn tất", e);
          phieuState.showError(LOI_TAI_DANH_SACH);
        }
      }
    }.execute();
  }

  private Card buildHoaDonCard() {
    Card card = Card.section("Hóa đơn gốc",
        "Tra cứu hóa đơn đã thanh toán để đối chiếu máy khách mang đến.", null);

    JPanel body = new JPanel(new MigLayout("fillx, insets 0, gapy 12, hidemode 3", "[grow][]", "[]"));
    body.setOpaque(false);

    maHoaDonField.setRequired(true);
    JPanel dongTraCuu = new JPanel(new MigLayout("fillx, insets 0, gapx 10", "[grow][]", "[bottom]"));
    dongTraCuu.setOpaque(false);
    dongTraCuu.add(maHoaDonField, "growx");
    traCuuButton.addActionListener(e -> traCuuHoaDon());
    dongTraCuu.add(traCuuButton, "aligny bottom");
    body.add(dongTraCuu, "span 2, growx, wrap");

    thongTinHoaDon.setOpaque(false);
    thongTinHoaDon.add(oThongTin("Khách hàng", tenKhachHangLabel));
    thongTinHoaDon.add(oThongTin("Số điện thoại", soDienThoaiLabel));
    thongTinHoaDon.add(oThongTin("Ngày lập", ngayLapLabel));
    thongTinHoaDon.setVisible(false);
    body.add(thongTinHoaDon, "span 2, growx, wrap");

    loiTraCuu.setVisible(false);
    body.add(loiTraCuu, "span 2, growx, wrap");

    card.add(body, "growx");
    return card;
  }

  private Card buildMayCard() {
    Card card = Card.section("Máy đã bán",
        "Chọn máy khách muốn trả hoặc đổi. Máy đã đổi/trả trước đó không chọn được.", null);
    mayTable.setCodeColumn(0);
    mayTable.setMoneyColumn(2);
    mayTable.setCustomRenderer(3, new TrangThaiMayRenderer());
    mayTable.setColumnWidths(150, 260, 150, 140, 130);
    mayTable.setActionColumn(4, RowAction.secondary("Chọn máy", this::chonMay));
    card.add(mayState, "growx");
    return card;
  }

  private Card buildThongTinCard() {
    Card card = Card.section("Thông tin đổi/trả",
        "Nhập tình trạng máy, lý do và chọn hình thức xử lý.", null);

    JPanel form = new JPanel(new MigLayout(
        "fillx, insets 0, hidemode 3", "[grow,fill]20[grow,fill]", "[]12[]12[]12[]"));
    form.setOpaque(false);

    tinhTrangField.setRequired(true);
    lyDoField.setRequired(true);
    form.add(tinhTrangField, "growx");
    form.add(lyDoField, "growx, wrap");

    JPanel dongHinhThuc = new JPanel(new MigLayout("insets 0, gapx 12", "[]", "[center]"));
    dongHinhThuc.setOpaque(false);
    JLabel nhanHinhThuc = new JLabel("Hình thức xử lý");
    nhanHinhThuc.setFont(DtaqFonts.LABEL);
    nhanHinhThuc.setForeground(DtaqColors.TEXT);
    dongHinhThuc.add(nhanHinhThuc);
    dongHinhThuc.add(hinhThuc);
    form.add(dongHinhThuc, "span 2, wrap");

    serialPanel.setOpaque(false);
    serialPanel.setVisible(false);
    serialPanel.add(serialThayTheField, "growx");
    form.add(serialPanel, "span 2, growx, wrap");

    card.add(form, "growx");
    return card;
  }

  private Card buildTinhTienCard() {
    Card card = Card.section("Tính tiền",
        "Giá trị máy trả lấy theo hóa đơn gốc; máy mới tính theo giá bán hiện hành.", null);

    JPanel tien = new JPanel(new MigLayout("fillx, insets 0, gapy 8, hidemode 3", "[grow]", "[]"));
    tien.setOpaque(false);

    tien.add(dongTien("Giá trị máy trả", giaTriMayTraLabel, false), "growx, wrap");
    tien.add(rowMayMoi, "growx, wrap");
    tien.add(rowChenhLech, "growx, wrap");
    tien.add(new JSeparator(), "growx, wrap, gaptop 2, gapbottom 2");
    tien.add(rowSoTienHoan, "growx, wrap");

    canhBaoNguong.setVisible(false);
    tien.add(canhBaoNguong, "growx, wrap");
    loiTinh.setVisible(false);
    tien.add(loiTinh, "growx, wrap");

    hoanTatButton.setEnabled(false);
    hoanTatButton.addActionListener(e -> xacNhanHoanTat());
    JPanel dongNut = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    dongNut.setOpaque(false);
    dongNut.add(hoanTatButton, "align right");
    tien.add(dongNut, "growx, gaptop 4");

    card.add(tien, "growx");
    return card;
  }

  private Card buildPhieuCard() {
    Card card = Card.section("Phiếu chờ hoàn tất",
        "Các phiếu đã được Quản lý duyệt và đang chờ nhân viên hoàn tất.", null);
    phieuTable.setCodeColumn(0);
    phieuTable.setCodeColumn(1);
    phieuTable.setMoneyColumn(4);
    phieuTable.setColumnWidths(130, 140, 190, 130, 140, 110, 130);
    phieuTable.setActionColumn(6, RowAction.primary("Hoàn tất", this::hoanTatTuDanhSach));
    card.add(phieuState, "growx");
    return card;
  }

  private void caiDatSuKien() {
    maHoaDonField.getTextField().addActionListener(e -> traCuuHoaDon());

    ((JComboBox<?>) tinhTrangField.getInputComponent())
        .addActionListener(e -> capNhatXemTruoc());

    hinhThuc.setOnSelectionChange(index -> {
      boolean doiMay = index == 1;
      serialPanel.setVisible(doiMay);
      if (!doiMay) {
        serialThayTheField.setError(null);
      }
      view.revalidate();
      view.repaint();
      capNhatXemTruoc();
    });

    lyDoField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        henXemTruoc.restart();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        henXemTruoc.restart();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        henXemTruoc.restart();
      }
    });
    lyDoField.getTextField().addActionListener(e -> capNhatXemTruoc());

    serialThayTheField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        henXemTruoc.restart();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        henXemTruoc.restart();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        henXemTruoc.restart();
      }
    });
    serialThayTheField.getTextField().addActionListener(e -> capNhatXemTruoc());
  }

  private void hienThiTraCuu(DoiTraController.KetQuaTraCuu ketQua) {
    if (!ketQua.thanhCong()) {
      hienThiLoiTraCuu(ketQua.thongBao());
      if (ketQua.maHD() == null || ketQua.maHD().isBlank()) {
        maHoaDonField.setError(ketQua.thongBao());
      }
      return;
    }
    hoaDonHienTai = ketQua;
    mayDuocChon = null;
    maHoaDonField.setError(null);
    loiTraCuu.setVisible(false);

    tenKhachHangLabel.setText(anToan(ketQua.tenKhachHang()));
    soDienThoaiLabel.setText(anToan(ketQua.soDienThoai()));
    ngayLapLabel.setText(DateFormats.formatDate(ketQua.ngayLap()));
    thongTinHoaDon.setVisible(true);

    mayCache.clear();
    mayModel.setRowCount(0);
    for (DoiTraController.MayDaBan may : ketQua.mayDaBan()) {
      mayCache.add(may);
      mayModel.addRow(new Object[] {
          anToan(may.soSerial()),
          anToan(may.tenDongSP()),
          may.giaTriThanhToan() == null ? 0L : may.giaTriThanhToan().longValueExact(),
          new TrangThaiMay(may.daDoiTra(), moTaTrangThai(may.trangThai())),
          ""});
    }
    if (mayCache.isEmpty()) {
      mayState.showEmpty("package-search", "Hóa đơn không có máy nào để đổi/trả",
          "Hóa đơn gốc chưa có dòng chi tiết nào.");
    } else {
      mayState.showContent();
    }
    xoaKetQuaTinh();
    refreshView();
  }

  private void hienThiLoiTraCuu(String thongBao) {
    hoaDonHienTai = null;
    mayDuocChon = null;
    mayCache.clear();
    mayModel.setRowCount(0);
    mayState.showEmpty();
    thongTinHoaDon.setVisible(false);
    loiTraCuu.setText(thongBao);
    loiTraCuu.setVisible(true);
    xoaKetQuaTinh();
    refreshView();
  }

  private void hienThiKetQuaTinh(DoiTraController.KetQuaTinh ketQua) {
    if (!ketQua.hopLe()) {
      xoaKetQuaTinh();
      loiTinh.setText(ketQua.thongBao());
      loiTinh.setVisible(true);
      hoanTatButton.setEnabled(false);
      refreshView();
      return;
    }
    boolean doiMay = hinhThuc.getSelectedIndex() == 1;
    giaTriMayTraLabel.setText(MoneyFormat.formatVnd(ketQua.giaTriMayTra()));
    rowMayMoi.setVisible(doiMay);
    rowChenhLech.setVisible(doiMay);
    rowSoTienHoan.setVisible(!doiMay);
    datNhanDongTien(rowChenhLech, doiMay ? "Chênh lệch (không hoàn)" : "Chênh lệch");
    giaTriMayMoiLabel.setText(MoneyFormat.formatVnd(ketQua.giaTriMayMoi()));
    if (doiMay) {
      chenhLechLabel.setText(
          ketQua.chenhLech() == null || ketQua.chenhLech().signum() == 0
              ? "Không chênh lệch"
              : MoneyFormat.formatVnd(ketQua.chenhLech()));
      // D17: ngưỡng 3.000.000 chỉ áp cho phiếu trả, không áp cho đổi hàng.
      canhBaoNguong.setVisible(false);
    } else {
      chenhLechLabel.setText(MoneyFormat.formatVnd(ketQua.chenhLech()));
      canhBaoNguong.setVisible(ketQua.canDuyet());
    }
    soTienHoanLabel.setText(MoneyFormat.formatVnd(ketQua.soTienHoan()));
    loiTinh.setVisible(false);
    hoanTatButton.setEnabled(true);
    refreshView();
  }

  private void xoaKetQuaTinh() {
    giaTriMayTraLabel.setText("—");
    giaTriMayMoiLabel.setText("—");
    chenhLechLabel.setText("—");
    soTienHoanLabel.setText("—");
    rowMayMoi.setVisible(false);
    rowChenhLech.setVisible(false);
    boolean doiMay = hinhThuc.getSelectedIndex() == 1;
    rowSoTienHoan.setVisible(!doiMay);
    datNhanDongTien(rowChenhLech, doiMay ? "Chênh lệch (không hoàn)" : "Chênh lệch");
    canhBaoNguong.setVisible(false);
    loiTinh.setVisible(false);
    hoanTatButton.setEnabled(false);
    refreshView();
  }

  private void xacNhanHoanTat() {
    DoiTraController.YeuCauDoiTra yeuCau = yeuCauHienTai();
    if (yeuCau == null) {
      return;
    }
    hoanTatButton.setBusy(true);
    new SwingWorker<DoiTraController.KetQuaTinh, Void>() {
      @Override
      protected DoiTraController.KetQuaTinh doInBackground() {
        return controller.tinhPhieu(yeuCau);
      }

      @Override
      protected void done() {
        try {
          DoiTraController.KetQuaTinh ketQua = get();
          hoanTatButton.setBusy(false);
          hienThiKetQuaTinh(ketQua);
          if (!ketQua.hopLe()) {
            return;
          }
          ConfirmDialog.confirm(
              view,
              tieuDeXacNhan(hinhThuc.getSelectedIndex() == 1, ketQua),
              noiDungXacNhan(hinhThuc.getSelectedIndex() == 1, ketQua),
              "Xác nhận",
              () -> guiPhieu(yeuCau));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          hoanTatButton.setBusy(false);
          Toast.error(view, LOI_TINH_PHIEU);
        } catch (ExecutionException e) {
          hoanTatButton.setBusy(false);
          if (e.getCause() instanceof SecurityException) {
            Toast.error(view, LOI_QUYEN);
          } else {
            LOG.error("Không thể tính tiền đổi/trả", e.getCause());
            Toast.error(view, LOI_TINH_PHIEU);
          }
        } catch (RuntimeException e) {
          hoanTatButton.setBusy(false);
          LOG.error("Không thể tính tiền đổi/trả", e);
          Toast.error(view, LOI_TINH_PHIEU);
        }
      }
    }.execute();
  }

  /**
   * Ghi phiếu với đúng yêu cầu đã được chốt ở lần tính lại cuối cùng.
   *
   * <p>Dữ liệu hiển thị trên chứng từ được chụp lại trước khi chạy nền; {@code done()} không đọc
   * lại các trường đang thay đổi trên form.
   *
   * @param yeuCau yêu cầu đổi/trả đã chốt; không được {@code null}
   */
  void guiPhieu(DoiTraController.YeuCauDoiTra yeuCau) {
    String maHD = yeuCau.maHD();
    String soSerial = yeuCau.soSerial();
    TinhTrangHangTra tinhTrang = yeuCau.tinhTrang();
    boolean doiMay =
        yeuCau.soSerialMayThayThe() != null && !yeuCau.soSerialMayThayThe().isBlank();
    DoiTraController.MayDaBan may = mayDuocChon;
    DoiTraController.KetQuaTraCuu hoaDon = hoaDonHienTai;
    String tenDongSP = may == null ? null : may.tenDongSP();
    String tenKhachHang = hoaDon == null ? null : hoaDon.tenKhachHang();
    String soDienThoai = hoaDon == null ? null : hoaDon.soDienThoai();

    hoanTatButton.setBusy(true);
    new SwingWorker<DoiTraController.KetQuaGhi, Void>() {
      @Override
      protected DoiTraController.KetQuaGhi doInBackground() {
        return controller.ghiPhieu(yeuCau);
      }

      @Override
      protected void done() {
        hoanTatButton.setBusy(false);
        try {
          DoiTraController.KetQuaGhi ketQua = get();
          if (!ketQua.thanhCong()) {
            // Số liệu cũ không còn đúng: xoá khối tính tiền và hiện lỗi nghiệp vụ.
            xoaKetQuaTinh();
            loiTinh.setText(ketQua.thongBao());
            loiTinh.setVisible(true);
            hoanTatButton.setEnabled(true);
            refreshView();
            Toast.error(view, ketQua.thongBao());
            return;
          }
          if (ketQua.choDuyet()) {
            String thongBao = ketQua.thongBao() + " · Số tiền hoàn: "
                + MoneyFormat.formatVnd(ketQua.soTienHoan());
            Toast.success(view, thongBao);
            xoaForm();
            taiDanhSachChoHoanTat();
            return;
          }
          Toast.success(view, ketQua.thongBao());
          moChungTu(
              ketQua, maHD, tenKhachHang, soDienThoai, soSerial, tenDongSP, tinhTrang, doiMay);
          xoaForm();
          taiDanhSachChoHoanTat();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, LOI_GHI_PHIEU);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            Toast.error(view, LOI_QUYEN);
          } else if (e.getCause() instanceof RuntimeException runtime) {
            LOG.error("Không thể ghi phiếu đổi/trả", runtime);
            Toast.error(view, runtime.getMessage() == null || runtime.getMessage().isBlank()
                ? LOI_GHI_PHIEU : runtime.getMessage());
          } else {
            LOG.error("Không thể ghi phiếu đổi/trả", e.getCause());
            Toast.error(view, LOI_GHI_PHIEU);
          }
        } catch (RuntimeException e) {
          LOG.error("Không thể ghi phiếu đổi/trả", e);
          Toast.error(view, LOI_GHI_PHIEU);
        }
      }
    }.execute();
  }

  private void hoanTatTuDanhSach(int modelRow) {
    if (modelRow < 0 || modelRow >= phieuCache.size()) {
      return;
    }
    PhieuDuyet phieu = phieuCache.get(modelRow);
    ConfirmDialog.confirm(
        view,
        "Xác nhận hoàn tất phiếu",
        "Xác nhận đã hoàn tiền cho khách và hoàn tất phiếu " + phieu.maPhieu() + "?",
        "Xác nhận",
        () -> guiHoanTat(phieu));
  }

  private void guiHoanTat(PhieuDuyet phieu) {
    new SwingWorker<DoiTraController.KetQuaGhi, Void>() {
      @Override
      protected DoiTraController.KetQuaGhi doInBackground() {
        return controller.hoanTatPhieu(phieu.maPhieu());
      }

      @Override
      protected void done() {
        try {
          DoiTraController.KetQuaGhi ketQua = get();
          if (!ketQua.thanhCong()) {
            Toast.error(view, ketQua.thongBao());
            taiDanhSachChoHoanTat();
            return;
          }
          Toast.success(view, ketQua.thongBao());
          moChungTu(ketQua, phieu.maHDGoc(), phieu.tenKhachHang(), phieu.soDienThoai(),
              null, null, null, phieu.loai() == PhieuDuyet.LoaiPhieu.PHIEU_DOI);
          taiDanhSachChoHoanTat();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Toast.error(view, LOI_HOAN_TAT);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            Toast.error(view, LOI_QUYEN);
          } else if (e.getCause() instanceof RuntimeException runtime) {
            LOG.error("Không thể hoàn tất phiếu {}", phieu.maPhieu(), runtime);
            Toast.error(view, runtime.getMessage() == null || runtime.getMessage().isBlank()
                ? LOI_HOAN_TAT : runtime.getMessage());
          } else {
            LOG.error("Không thể hoàn tất phiếu {}", phieu.maPhieu(), e.getCause());
            Toast.error(view, LOI_HOAN_TAT);
          }
          taiDanhSachChoHoanTat();
        } catch (RuntimeException e) {
          LOG.error("Không thể hoàn tất phiếu {}", phieu.maPhieu(), e);
          Toast.error(view, LOI_HOAN_TAT);
          taiDanhSachChoHoanTat();
        }
      }
    }.execute();
  }

  private void hienThiDanhSachChoHoanTat(List<PhieuDuyet> danhSach) {
    phieuCache.clear();
    phieuModel.setRowCount(0);
    if (danhSach == null || danhSach.isEmpty()) {
      phieuState.showEmpty();
      return;
    }
    phieuCache.addAll(danhSach);
    for (PhieuDuyet phieu : danhSach) {
      phieuModel.addRow(new Object[] {
          anToan(phieu.maPhieu()),
          anToan(phieu.maHDGoc()),
          anToan(phieu.tenKhachHang()),
          anToan(phieu.soDienThoai()),
          phieu.hoanDuKien() == null ? 0L : phieu.hoanDuKien().longValueExact(),
          DateFormats.formatDate(phieu.ngayTao()),
          ""});
    }
    phieuState.showContent();
  }

  private void xoaForm() {
    lanTraCuu++;
    lanXemTruoc++;
    hoaDonHienTai = null;
    mayDuocChon = null;
    maHoaDonField.setText("");
    maHoaDonField.setError(null);
    lyDoField.setText("");
    serialThayTheField.setText("");
    serialThayTheField.setError(null);
    ((JComboBox<?>) tinhTrangField.getInputComponent()).setSelectedIndex(0);
    hinhThuc.select(0);
    serialPanel.setVisible(false);
    thongTinHoaDon.setVisible(false);
    loiTraCuu.setVisible(false);
    mayCache.clear();
    mayModel.setRowCount(0);
    mayState.showEmpty();
    xoaKetQuaTinh();
  }

  private DoiTraController.YeuCauDoiTra yeuCauHienTai() {
    if (hoaDonHienTai == null || mayDuocChon == null) {
      return null;
    }
    int chiSoTinhTrang = ((JComboBox<?>) tinhTrangField.getInputComponent()).getSelectedIndex();
    TinhTrangHangTra tinhTrang =
        chiSoTinhTrang == 1 ? TinhTrangHangTra.HANG_LOI : TinhTrangHangTra.NGUYEN_VEN;
    String serialMayThayThe = null;
    if (hinhThuc.getSelectedIndex() == 1) {
      serialMayThayThe = serialThayTheField.getText().trim();
      if (serialMayThayThe.isEmpty()) {
        // Không bao giờ để yêu cầu "Đổi máy" rơi về nhánh trả hàng khi thiếu serial.
        serialThayTheField.setError(LOI_THIEU_SERIAL_MAY_THAY_THE);
        return null;
      }
      serialThayTheField.setError(null);
    } else {
      serialThayTheField.setError(null);
    }
    return new DoiTraController.YeuCauDoiTra(
        hoaDonHienTai.maHD(),
        mayDuocChon.soSerial(),
        tinhTrang,
        lyDoField.getText().trim(),
        serialMayThayThe);
  }

  private void moChungTu(
      DoiTraController.KetQuaGhi ketQua,
      String maHD,
      String tenKhachHang,
      String soDienThoai,
      String soSerial,
      String tenDongSP,
      TinhTrangHangTra tinhTrang,
      boolean doiMay) {
    new ReturnReceiptDialog(
        ConfirmDialog.frameOf(view),
        ketQua,
        maHD,
        tenKhachHang,
        soDienThoai,
        soSerial,
        tenDongSP,
        tinhTrang,
        doiMay).setVisible(true);
  }

  static String tieuDeXacNhan(boolean doiMay, DoiTraController.KetQuaTinh ketQua) {
    if (doiMay) {
      return "Xác nhận đổi hàng";
    }
    if (ketQua.canDuyet()) {
      return "Xác nhận gửi phê duyệt";
    }
    if (ketQua.soTienHoan() != null && ketQua.soTienHoan().signum() > 0) {
      return "Xác nhận hoàn tiền";
    }
    if (ketQua.chenhLech() != null && ketQua.chenhLech().signum() > 0) {
      return "Xác nhận thu thêm tiền";
    }
    return "Xác nhận đổi ngang giá";
  }

  static String noiDungXacNhan(boolean doiMay, DoiTraController.KetQuaTinh ketQua) {
    if (doiMay) {
      if (ketQua.chenhLech() != null && ketQua.chenhLech().signum() < 0) {
        return "Đổi sang máy có giá trị thấp hơn; đổi hàng không hoàn tiền, phần chênh lệch "
            + MoneyFormat.formatVnd(ketQua.chenhLech()) + " sẽ không được hoàn lại. "
            + "Xác nhận lập phiếu đổi?";
      }
      return "Đổi ngang giá, không hoàn tiền và không thu thêm. Xác nhận lập phiếu đổi?";
    }
    if (ketQua.canDuyet()) {
      // XBR-07: phiếu chờ duyệt chưa được hoàn tiền; tiền chỉ hoàn khi hoàn tất sau khi được duyệt.
      return "Số tiền hoàn " + MoneyFormat.formatVnd(ketQua.soTienHoan())
          + " cần Quản lý phê duyệt. Chưa hoàn tiền cho khách cho đến khi phiếu được duyệt"
          + " và hoàn tất. Xác nhận lập phiếu?";
    }
    if (ketQua.soTienHoan() != null && ketQua.soTienHoan().signum() > 0) {
      return "Hoàn trả khách " + MoneyFormat.formatVnd(ketQua.soTienHoan())
          + ". Xác nhận đã hoàn tiền và lập phiếu?";
    }
    if (ketQua.chenhLech() != null && ketQua.chenhLech().signum() > 0) {
      return "Khách cần trả thêm " + MoneyFormat.formatVnd(ketQua.chenhLech())
          + ". Xác nhận đã thu tiền và lập phiếu đổi?";
    }
    return "Đổi ngang giá, không phát sinh thu hoặc hoàn tiền. Xác nhận lập phiếu đổi?";
  }

  private static void datNhanDongTien(JPanel row, String nhan) {
    if (row.getComponentCount() > 0 && row.getComponent(0) instanceof JLabel label) {
      label.setText(nhan);
    }
  }

  private static JPanel dongTien(String nhan, JLabel giaTri, boolean manh) {
    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    row.setOpaque(false);
    JLabel label = new JLabel(nhan);
    label.setFont(manh ? DtaqFonts.LABEL : DtaqFonts.BODY_13);
    label.setForeground(manh ? DtaqColors.TEXT : DtaqColors.TEXT_MUTED);
    row.add(label);
    if (manh) {
      giaTri.setFont(DtaqFonts.SECTION_TITLE_STRONG);
      giaTri.setForeground(DtaqColors.PRIMARY_STRONG);
    }
    row.add(giaTri, "align right");
    return row;
  }

  private static JPanel oThongTin(String nhan, JLabel giaTri) {
    JPanel o = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    o.setOpaque(false);
    JLabel label = new JLabel(nhan);
    label.setFont(DtaqFonts.CAPTION);
    label.setForeground(DtaqColors.TEXT_MUTED);
    o.add(label, "wrap");
    o.add(giaTri);
    return o;
  }

  private static JLabel giaTriNhan() {
    JLabel label = new JLabel("");
    label.setFont(DtaqFonts.BODY_13_MEDIUM);
    label.setForeground(DtaqColors.TEXT);
    return label;
  }

  private static JLabel giaTriTien() {
    JLabel label = new JLabel("—");
    label.setFont(DtaqFonts.BODY_13_MEDIUM);
    label.setForeground(DtaqColors.TEXT);
    return label;
  }

  private static String moTaTrangThai(TrangThaiSanPham trangThai) {
    if (trangThai == null) {
      return "—";
    }
    return switch (trangThai) {
      case DA_BAN -> "Đã bán";
      case SAN_SANG_BAN -> "Sẵn sàng bán";
      case HANG_LOI -> "Hàng lỗi";
      case DANG_BAO_HANH -> "Đang bảo hành";
      case DA_TRA_NHA_CUNG_CAP -> "Đã trả nhà cung cấp";
      case DA_THANH_LY -> "Đã thanh lý";
    };
  }

  private static String anToan(String value) {
    return value == null ? "" : value;
  }

  private void refreshView() {
    view.revalidate();
    view.repaint();
  }

  /** Trạng thái hiển thị của một máy trong bảng "Máy đã bán". */
  private record TrangThaiMay(boolean daDoiTra, String moTa) {
  }

  /** Hiển thị cột trạng thái máy: máy đã đổi/trả dùng StatusPill cảnh báo. */
  private static final class TrangThaiMayRenderer extends TableRenderers.TextRenderer {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (value instanceof TrangThaiMay trangThai && trangThai.daDoiTra()) {
        int pad = TableRenderers.cellPad(table);
        JPanel panel = new JPanel(
            new MigLayout("insets 0 " + pad + " 0 " + pad, "[]", "[center]"));
        panel.setOpaque(true);
        panel.setBackground(getBackground());
        panel.add(StatusPill.warning("Đã đổi/trả"));
        return panel;
      }
      setText(value instanceof TrangThaiMay trangThai ? trangThai.moTa()
          : (value == null ? "" : value.toString()));
      return this;
    }
  }
}
