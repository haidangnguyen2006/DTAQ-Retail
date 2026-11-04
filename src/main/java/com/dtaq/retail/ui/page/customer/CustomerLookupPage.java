package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.controller.TraCuuKhachHangController;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietHoaDonView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietPhieuView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.HoaDonTomTat;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.LoaiPhieu;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.PhieuTomTat;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình Tra cứu khách hàng và giao dịch (UC005): tìm khách theo số điện thoại, xem lịch sử hóa
 * đơn, chi tiết hóa đơn gốc và phiếu đổi/trả. Trang chỉ đọc, không có thao tác ghi.
 */
public final class CustomerLookupPage implements Page {

  private static final Logger log = LoggerFactory.getLogger(CustomerLookupPage.class);

  private static final String LOAD_ERROR = "Không thể tải dữ liệu tra cứu. Vui lòng thử lại";

  private final TraCuuKhachHangController controller;
  private final JPanel view;
  private final FormField sdtField = FormField.search("Số điện thoại", "Nhập số điện thoại khách hàng");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private final StatefulView resultState;

  private final JLabel tenKhachLabel = new JLabel();
  private final JLabel thongTinKhachLabel = new JLabel();
  private final JLabel diaChiLabel = new JLabel();
  private final JTabbedPane tabs = new JTabbedPane();

  private final DefaultTableModel hoaDonModel = DataTable.readOnlyModel(new String[] {
      "Mã hóa đơn", "Ngày lập", "Nhân viên lập", "Số sản phẩm",
      "Tiền hàng (VND)", "Thuế (VND)", "Tổng thanh toán (VND)", "Thanh toán"});
  private final DataTable hoaDonTable = new DataTable(hoaDonModel);
  private final StatefulView hoaDonState;
  private final DefaultTableModel chiTietHoaDonModel = DataTable.readOnlyModel(new String[] {
      "Số serial", "Mã dòng", "Tên sản phẩm", "Đơn giá (VND)", "Thuế suất",
      "Khuyến mãi (VND)", "Sau khuyến mãi (VND)", "Thuế (VND)", "Thành tiền (VND)"});
  private final DataTable chiTietHoaDonTable = new DataTable(chiTietHoaDonModel);
  private final StatefulView chiTietHoaDonState;
  private final JLabel chiTietHoaDonTitle = sectionTitle("Chi tiết hóa đơn gốc");

  private final DefaultTableModel phieuModel = DataTable.readOnlyModel(new String[] {
      "Loại phiếu", "Mã phiếu", "Hóa đơn gốc", "Ngày tạo", "Trạng thái duyệt",
      "Trạng thái xử lý", "Ngày hoàn tất"});
  private final DataTable phieuTable = new DataTable(phieuModel);
  private final StatefulView phieuState;
  private final DefaultTableModel chiTietPhieuModel = DataTable.readOnlyModel(new String[] {
      "Số serial", "Tên sản phẩm", "Lý do", "Tình trạng hàng trả", "Máy thay thế"});
  private final DataTable chiTietPhieuTable = new DataTable(chiTietPhieuModel);
  private final StatefulView chiTietPhieuState;
  private final JLabel chiTietPhieuTitle = sectionTitle("Nội dung phiếu");
  private final JLabel yKienDuyetLabel = new JLabel();

  private final List<HoaDonTomTat> hoaDons = new ArrayList<>();
  private final List<PhieuTomTat> phieus = new ArrayList<>();
  private int searchRequest;
  private int hoaDonRequest;
  private int phieuRequest;

  /**
   * Khởi tạo trang tra cứu khách hàng và giao dịch.
   *
   * @param controller bộ điều khiển tra cứu; không được {@code null}
   */
  public CustomerLookupPage(TraCuuKhachHangController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    view.setBackground(DtaqColors.BG_APP);

    view.add(new PageHeader("Tra cứu khách hàng và giao dịch",
        "Tìm khách theo số điện thoại để xem lịch sử mua hàng, hóa đơn và phiếu đổi/trả."), "growx, wrap");
    view.add(buildSearchCard(), "growx, wrap");

    hoaDonState = new StatefulView(hoaDonTable, "receipt-text", "Chưa có hóa đơn",
        "Khách hàng chưa có giao dịch mua hàng.");
    chiTietHoaDonState = new StatefulView(chiTietHoaDonTable, "receipt-text", "Chưa chọn hóa đơn",
        "Chọn một hóa đơn để xem chi tiết hóa đơn gốc.");
    phieuState = new StatefulView(phieuTable, "receipt-text", "Không có phiếu đổi/trả",
        "Khách hàng chưa có phiếu đổi hoặc phiếu trả nào.");
    chiTietPhieuState = new StatefulView(chiTietPhieuTable, "receipt-text", "Chưa chọn phiếu",
        "Chọn một phiếu để xem nội dung và trạng thái xử lý.");

    configureTables();
    resultState = new StatefulView(buildResultPanel(), "search", "Chưa tra cứu",
        "Nhập số điện thoại để xem khách hàng và giao dịch.");
    resultState.setOnRetry(this::search);
    view.add(resultState, "growx");
  }

  private Card buildSearchCard() {
    Card card = Card.standard();
    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[bottom]"));
    row.setOpaque(false);
    sdtField.setShortcut("F2");
    sdtField.getTextField().addActionListener(e -> search());
    searchButton.addActionListener(e -> search());
    row.add(sdtField, "growx");
    row.add(searchButton);
    card.add(row, "growx");
    return card;
  }

  private JPanel buildResultPanel() {
    JPanel panel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    panel.setOpaque(false);

    Card khachCard = Card.compact();
    tenKhachLabel.setFont(DtaqFonts.CUSTOMER_NAME);
    tenKhachLabel.setForeground(DtaqColors.TEXT);
    thongTinKhachLabel.setFont(DtaqFonts.BODY_13);
    thongTinKhachLabel.setForeground(DtaqColors.TEXT_MUTED);
    diaChiLabel.setFont(DtaqFonts.BODY_13);
    diaChiLabel.setForeground(DtaqColors.TEXT_MUTED);
    khachCard.add(tenKhachLabel, "growx, wrap");
    khachCard.add(thongTinKhachLabel, "growx, wrap");
    khachCard.add(diaChiLabel, "growx");
    panel.add(khachCard, "growx, wrap");

    JPanel hoaDonTab = new JPanel(new MigLayout("fillx, insets 12 0 0 0", "[grow]", "[]16[]8[]"));
    hoaDonTab.setOpaque(false);
    hoaDonTab.add(hoaDonState, "growx, wrap");
    hoaDonTab.add(chiTietHoaDonTitle, "growx, wrap");
    hoaDonTab.add(chiTietHoaDonState, "growx");

    JPanel phieuTab = new JPanel(new MigLayout("fillx, insets 12 0 0 0", "[grow]", "[]16[]8[]8[]"));
    phieuTab.setOpaque(false);
    yKienDuyetLabel.setFont(DtaqFonts.BODY_13);
    yKienDuyetLabel.setForeground(DtaqColors.TEXT_MUTED);
    phieuTab.add(phieuState, "growx, wrap");
    phieuTab.add(chiTietPhieuTitle, "growx, wrap");
    phieuTab.add(yKienDuyetLabel, "growx, wrap");
    phieuTab.add(chiTietPhieuState, "growx");

    tabs.addTab("Hóa đơn", hoaDonTab);
    tabs.addTab("Phiếu đổi/trả", phieuTab);
    Card tabCard = Card.standard();
    tabCard.add(tabs, "growx");
    panel.add(tabCard, "growx");
    return panel;
  }

  private void configureTables() {
    hoaDonTable.setCodeColumn(0);
    hoaDonTable.setNumberColumn(3);
    hoaDonTable.setMoneyColumn(4);
    hoaDonTable.setMoneyColumn(5);
    hoaDonTable.setMoneyColumn(6);
    hoaDonTable.setVisibleRows(3, 6);
    hoaDonTable.getTable().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        int viewRow = hoaDonTable.getTable().getSelectedRow();
        if (viewRow >= 0) {
          loadChiTietHoaDon(hoaDons.get(hoaDonTable.getTable().convertRowIndexToModel(viewRow)));
        }
      }
    });

    chiTietHoaDonTable.setCodeColumn(0);
    chiTietHoaDonTable.setMoneyColumn(3);
    chiTietHoaDonTable.setMoneyColumn(5);
    chiTietHoaDonTable.setMoneyColumn(6);
    chiTietHoaDonTable.setMoneyColumn(7);
    chiTietHoaDonTable.setMoneyColumn(8);
    chiTietHoaDonTable.setVisibleRows(2, 6);

    phieuTable.setCodeColumn(1);
    phieuTable.setVisibleRows(3, 6);
    phieuTable.getTable().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        int viewRow = phieuTable.getTable().getSelectedRow();
        if (viewRow >= 0) {
          loadChiTietPhieu(phieus.get(phieuTable.getTable().convertRowIndexToModel(viewRow)));
        }
      }
    });

    chiTietPhieuTable.setCodeColumn(0);
    chiTietPhieuTable.setVisibleRows(2, 6);
  }

  // ---------------------------------------------------------------- tra cứu

  /** Kết quả bước 5–8: khách hàng kèm lịch sử hóa đơn và phiếu đổi/trả. */
  private record KetQuaTraCuu(KhachHang khachHang, List<HoaDonTomTat> hoaDons, List<PhieuTomTat> phieus) {}

  private void search() {
    String sdt = sdtField.getText();
    if (sdt.isBlank()) {
      sdtField.setError("Vui lòng nhập số điện thoại");
      return;
    }
    sdtField.setError(null);
    int request = ++searchRequest;
    searchButton.setEnabled(false);
    resultState.showLoading();
    run(() -> {
      Optional<KhachHang> khach = controller.timKhach(sdt);
      if (khach.isEmpty()) {
        return null;
      }
      String maKH = khach.get().maKH();
      return new KetQuaTraCuu(khach.get(), controller.lichSuHoaDon(maKH), controller.phieuDoiTra(maKH));
    }, ketQua -> {
      if (request != searchRequest) {
        return;
      }
      searchButton.setEnabled(true);
      if (ketQua == null) {
        resultState.showEmpty("user-round", "Không tìm thấy khách hàng",
            "Kiểm tra lại số điện thoại rồi tìm kiếm lại.");
      } else {
        render(ketQua);
      }
    }, message -> {
      if (request == searchRequest) {
        searchButton.setEnabled(true);
        resultState.showError(message);
      }
    });
  }

  private void render(KetQuaTraCuu ketQua) {
    KhachHang kh = ketQua.khachHang();
    tenKhachLabel.setText(kh.hoTen());
    thongTinKhachLabel.setText(kh.maKH() + " • " + PhoneFormat.format(kh.soDienThoai()));
    diaChiLabel.setText(kh.diaChi() == null ? "Chưa có địa chỉ" : kh.diaChi());

    hoaDons.clear();
    hoaDons.addAll(ketQua.hoaDons());
    hoaDonModel.setRowCount(0);
    for (HoaDonTomTat hd : hoaDons) {
      hoaDonModel.addRow(new Object[] {
          hd.maHD(), DateFormats.formatDate(hd.ngayLap()), hd.tenNhanVienLap(), hd.soSanPham(),
          hd.tienHang(), hd.tienThue(), hd.tongThanhToan(), hd.daThanhToan() ? "Đã thanh toán" : "Chưa thanh toán"
      });
    }
    showOrEmpty(hoaDonState, hoaDons.isEmpty());
    tabs.setTitleAt(0, "Hóa đơn (" + hoaDons.size() + ")");
    resetChiTietHoaDon();

    phieus.clear();
    phieus.addAll(ketQua.phieus());
    phieuModel.setRowCount(0);
    for (PhieuTomTat p : phieus) {
      phieuModel.addRow(new Object[] {
          p.loai() == LoaiPhieu.TRA ? "Phiếu trả" : "Phiếu đổi", p.maPhieu(), p.maHD(),
          DateFormats.formatDate(p.ngayTao()), nhanTrangThaiDuyet(p.trangThaiDuyet()),
          nhanTrangThaiXuLy(p.trangThaiXuLy()), p.ngayHoanTat() == null ? "—" : DateFormats.formatDate(p.ngayHoanTat())
      });
    }
    showOrEmpty(phieuState, phieus.isEmpty());
    tabs.setTitleAt(1, "Phiếu đổi/trả (" + phieus.size() + ")");
    resetChiTietPhieu();

    resultState.showContent();
  }

  private void loadChiTietHoaDon(HoaDonTomTat hoaDon) {
    int request = ++hoaDonRequest;
    chiTietHoaDonTitle.setText("Chi tiết hóa đơn gốc " + hoaDon.maHD());
    chiTietHoaDonState.showLoading();
    run(() -> controller.chiTietHoaDon(hoaDon.maHD()), chiTiet -> {
      if (request != hoaDonRequest) {
        return;
      }
      chiTietHoaDonModel.setRowCount(0);
      for (ChiTietHoaDonView ct : chiTiet) {
        chiTietHoaDonModel.addRow(new Object[] {
            ct.soSerial(), ct.maDongSP(), ct.tenDongSP(), ct.donGia(), ct.thueSuat().stripTrailingZeros().toPlainString() + "%",
            ct.tienKhuyenMai(), ct.tienSauKhuyenMai(), ct.tienThue(), ct.thanhTien()
        });
      }
      if (chiTiet.isEmpty()) {
        chiTietHoaDonState.showEmpty("receipt-text", "Hóa đơn không có dòng chi tiết", null);
      } else {
        chiTietHoaDonState.showContent();
      }
    }, message -> {
      if (request == hoaDonRequest) {
        chiTietHoaDonState.showError(message);
      }
    });
  }

  private void loadChiTietPhieu(PhieuTomTat phieu) {
    int request = ++phieuRequest;
    chiTietPhieuTitle.setText((phieu.loai() == LoaiPhieu.TRA ? "Nội dung phiếu trả " : "Nội dung phiếu đổi ")
        + phieu.maPhieu() + " • " + nhanTrangThaiXuLy(phieu.trangThaiXuLy()));
    yKienDuyetLabel.setText(phieu.lyDoDuyet() == null || phieu.lyDoDuyet().isBlank()
        ? "Trạng thái duyệt: " + nhanTrangThaiDuyet(phieu.trangThaiDuyet())
        : "Trạng thái duyệt: " + nhanTrangThaiDuyet(phieu.trangThaiDuyet()) + " • Ý kiến: " + phieu.lyDoDuyet());
    yKienDuyetLabel.setVisible(true);
    chiTietPhieuState.showLoading();
    run(() -> controller.chiTietPhieu(phieu.loai(), phieu.maPhieu()), chiTiet -> {
      if (request != phieuRequest) {
        return;
      }
      chiTietPhieuModel.setRowCount(0);
      for (ChiTietPhieuView ct : chiTiet) {
        chiTietPhieuModel.addRow(new Object[] {
            ct.soSerial(), ct.tenDongSP(), ct.lyDo(),
            ct.tinhTrangHangTra() == null ? "—" : nhanTinhTrang(ct.tinhTrangHangTra()),
            ct.serialThayThe() == null ? "—" : ct.serialThayThe()
        });
      }
      if (chiTiet.isEmpty()) {
        chiTietPhieuState.showEmpty("receipt-text", "Phiếu không có dòng hàng", null);
      } else {
        chiTietPhieuState.showContent();
      }
    }, message -> {
      if (request == phieuRequest) {
        chiTietPhieuState.showError(message);
      }
    });
  }

  private void resetChiTietHoaDon() {
    ++hoaDonRequest;
    chiTietHoaDonModel.setRowCount(0);
    chiTietHoaDonTitle.setText("Chi tiết hóa đơn gốc");
    chiTietHoaDonState.showEmpty();
  }

  private void resetChiTietPhieu() {
    ++phieuRequest;
    chiTietPhieuModel.setRowCount(0);
    chiTietPhieuTitle.setText("Nội dung phiếu");
    yKienDuyetLabel.setVisible(false);
    chiTietPhieuState.showEmpty();
  }

  /** Chạy truy vấn ngoài EDT; kết quả và lỗi được trả về trên EDT. */
  private <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
    new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return task.call();
      }

      @Override
      protected void done() {
        try {
          onSuccess.accept(get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          onError.accept(LOAD_ERROR);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.warn("Tra cứu khách hàng thất bại (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          onError.accept(cause instanceof SecurityException ? cause.getMessage() : LOAD_ERROR);
        }
      }
    }.execute();
  }

  private static void showOrEmpty(StatefulView state, boolean empty) {
    if (empty) {
      state.showEmpty();
    } else {
      state.showContent();
    }
  }

  private static JLabel sectionTitle(String text) {
    JLabel label = new JLabel(text);
    label.setFont(DtaqFonts.CARD_TITLE);
    label.setForeground(DtaqColors.TEXT);
    return label;
  }

  private static String nhanTrangThaiDuyet(TrangThaiDuyet trangThai) {
    return switch (trangThai) {
      case KHONG_CAN_DUYET -> "Không cần duyệt";
      case CHO_DUYET -> "Chờ duyệt";
      case DA_DUYET -> "Đã duyệt";
      case TU_CHOI -> "Từ chối";
    };
  }

  private static String nhanTrangThaiXuLy(TrangThaiXuLyPhieu trangThai) {
    return switch (trangThai) {
      case NHAP -> "Nháp";
      case CHO_XU_LY -> "Chờ xử lý";
      case HOAN_TAT -> "Hoàn tất";
      case HUY -> "Đã hủy";
    };
  }

  private static String nhanTinhTrang(TinhTrangHangTra tinhTrang) {
    return tinhTrang == TinhTrangHangTra.HANG_LOI ? "Hàng lỗi" : "Nguyên vẹn";
  }

  // ---------------------------------------------------------------- Page

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusLookupPhone");
    actionMap.put("focusLookupPhone", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        sdtField.getTextField().requestFocusInWindow();
      }
    });
  }

  @Override
  public PageId id() {
    return PageId.CUSTOMER_LOOKUP;
  }

  @Override
  public String title() {
    return "Tra cứu khách hàng và giao dịch";
  }

  @Override
  public String section() {
    return "Bán hàng";
  }

  @Override
  public JComponent view() {
    return view;
  }
}
