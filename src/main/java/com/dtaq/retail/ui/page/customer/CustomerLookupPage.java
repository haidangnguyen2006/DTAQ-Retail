package com.dtaq.retail.ui.page.customer;

import com.dtaq.retail.controller.TraCuuKhachHangController;
import com.dtaq.retail.controller.TraCuuKhachHangController.TongQuanKhach;
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
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.Responsive;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.Pagination;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.TienUtil;
import java.time.LocalDate;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
  private static final int MAX_MATCHES = 50;
  private static final int MATCH_PAGE_SIZE = 5;

  private final TraCuuKhachHangController controller;
  private final JPanel view;
  private final FormField sdtField = FormField.search(null,
      "Số điện thoại, họ tên (có dấu hoặc không) hoặc mã khách hàng");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private final StatefulView resultState;

  /** Danh sách chọn khi từ khóa khớp nhiều khách (ví dụ tìm theo tên). */
  private final DefaultTableModel matchModel = DataTable.readOnlyModel(new String[] {
      "Mã khách hàng", "Họ và tên", "Số điện thoại", "Địa chỉ"});
  private final DataTable matchTable = new DataTable(matchModel);
  private final Pagination matchPagination = new Pagination();
  private final Card matchCard = Card.standard();
  private final List<KhachHang> matches = new ArrayList<>();
  private int currentMatchPage = 1;

  private final KpiCard kpiChiTieu = new KpiCard("Tổng chi tiêu", "—", "");
  private final KpiCard kpiHoaDon = new KpiCard("Số hóa đơn", "—", "");
  private final KpiCard kpiGanNhat = new KpiCard("Lần mua gần nhất", "—", "");
  private final KpiCard kpiPhieu = new KpiCard("Phiếu đổi/trả", "—", "");

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
        "fillx, hidemode 3, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    view.setBackground(DtaqColors.BG_APP);

    view.add(new PageHeader("Tra cứu khách hàng và giao dịch", null), "growx, wrap");
    view.add(buildSearchCard(), "growx, wrap");

    matchTable.setCodeColumn(0);
    matchTable.setVisibleRows(2, 6);
    matchTable.getTable().getSelectionModel().addListSelectionListener(e -> {
      int viewRow = matchTable.getTable().getSelectedRow();
      if (!e.getValueIsAdjusting() && viewRow >= 0) {
        int modelRow = matchTable.getTable().convertRowIndexToModel(viewRow);
        int actualIndex = (currentMatchPage - 1) * MATCH_PAGE_SIZE + modelRow;
        if (actualIndex >= 0 && actualIndex < matches.size()) {
          loadKhach(matches.get(actualIndex));
        }
      }
    });
    matchPagination.setOnPageChangeListener(this::renderMatchPage);
    matchCard.add(matchTable, "growx, wrap");
    matchCard.add(matchPagination, "growx");
    matchCard.setVisible(false);
    view.add(matchCard, "growx, wrap");

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
        "Nhập số điện thoại, họ tên hoặc mã khách hàng rồi nhấn Enter hoặc Tìm kiếm.");
    resultState.setOnRetry(this::search);
    view.add(resultState, "growx");
  }

  private Card buildSearchCard() {
    Card card = Card.standard();
    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[]"));
    row.setOpaque(false);
    sdtField.getTextField().addActionListener(e -> search());
    searchButton.addActionListener(e -> search());
    sdtField.getTextField().setToolTipText("Ví dụ: 0902 100 001 • nguyen van an • mã khách hàng (Nhấn Enter hoặc bấm Tìm kiếm)");
    row.add(sdtField.getInputComponent(), "growx, aligny center");
    row.add(searchButton, "aligny center");
    card.add(row, "growx");
    return card;
  }

  private JPanel buildResultPanel() {
    JPanel panel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    panel.setOpaque(false);

    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0, gapx 16",
        "[grow,fill][grow,fill][grow,fill][grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    Responsive.grid(kpiRow, 200, 4);
    kpiRow.add(kpiChiTieu);
    kpiRow.add(kpiHoaDon);
    kpiRow.add(kpiGanNhat);
    kpiRow.add(kpiPhieu);
    panel.add(kpiRow, "growx, wrap");

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
    String tuKhoa = sdtField.getText().trim();
    if (tuKhoa.isEmpty()) {
      showMatches(List.of());
      resultState.showEmpty("search", "Vui lòng nhập thông tin",
          "Nhập số điện thoại, họ tên hoặc mã khách hàng để tra cứu giao dịch.");
      return;
    }
    sdtField.setError(null);
    int request = ++searchRequest;
    resultState.showLoading();
    run(() -> controller.timGanDung(tuKhoa, MAX_MATCHES), ketQua -> {
      if (request != searchRequest) {
        return;
      }
      showMatches(ketQua.size() > 1 ? ketQua : List.of());
      if (ketQua.isEmpty()) {
        resultState.showEmpty("user-round", "Không tìm thấy khách hàng",
            "Không có hồ sơ khớp \"" + tuKhoa + "\". Kiểm tra lại hoặc tạo khách mới ở màn hình Quản lý khách hàng.");
      } else if (ketQua.size() == 1) {
        loadKhach(ketQua.get(0));
      } else {
        resultState.showEmpty("users", ketQua.size() + " khách hàng khớp",
            "Chọn một khách trong danh sách phía trên để xem giao dịch.");
      }
    }, message -> {
      if (request == searchRequest) {
        resultState.showError(message);
      }
    });
  }

  private void showMatches(List<KhachHang> ds) {
    matches.clear();
    matches.addAll(ds);
    if (ds.size() > 1) {
      renderMatchPage(1);
    } else {
      matchCard.setVisible(false);
    }
    view.revalidate();
  }

  private void renderMatchPage(int page) {
    currentMatchPage = page;
    matchModel.setRowCount(0);
    int total = matches.size();
    int from = (page - 1) * MATCH_PAGE_SIZE;
    int to = Math.min(from + MATCH_PAGE_SIZE, total);
    for (int i = from; i < to; i++) {
      KhachHang kh = matches.get(i);
      matchModel.addRow(new Object[] {
          kh.maKH(), kh.hoTen(), PhoneFormat.format(kh.soDienThoai()), kh.diaChi() == null ? "" : kh.diaChi()});
    }
    matchPagination.setPageData(page, Pagination.totalPages(total, MATCH_PAGE_SIZE),
        "Hiển thị " + (from + 1) + "–" + to + " trên " + total + " khách hàng");
    matchCard.setVisible(total > 1);
    view.revalidate();
  }

  /** Bước 6–8: tải lịch sử hóa đơn và phiếu đổi/trả của một khách đã xác định. */
  private void loadKhach(KhachHang khach) {
    int request = ++searchRequest;
    resultState.showLoading();
    run(() -> new KetQuaTraCuu(khach, controller.lichSuHoaDon(khach.maKH()), controller.phieuDoiTra(khach.maKH())),
        ketQua -> {
          if (request == searchRequest) {
            render(ketQua);
          }
        }, message -> {
          if (request == searchRequest) {
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

    renderTongQuan(TraCuuKhachHangController.tongQuan(hoaDons, phieus, LocalDate.now()));
    resultState.showContent();
    // Mở sẵn hóa đơn mới nhất để người dùng thấy ngay chi tiết, không phải bấm thêm.
    if (!hoaDons.isEmpty()) {
      hoaDonTable.getTable().setRowSelectionInterval(0, 0);
    }
  }

  private void renderTongQuan(TongQuanKhach tq) {
    kpiChiTieu.setValue(TienUtil.dinhDang(tq.tongChiTieu()));
    kpiChiTieu.setCaption(tq.soMay() + " sản phẩm đã mua");
    kpiHoaDon.setValue(String.valueOf(tq.soHoaDon()));
    kpiHoaDon.setCaption(tq.soHoaDon() == 0 ? "Chưa có hóa đơn đã thanh toán"
        : "TB " + TienUtil.dinhDang(tq.trungBinhHoaDon()) + " / hóa đơn");
    if (tq.lanMuaGanNhat() == null) {
      kpiGanNhat.setValue("Chưa mua");
      kpiGanNhat.setCaption("Khách mới, chưa có giao dịch");
    } else {
      long ngay = tq.soNgayTuLanCuoi();
      kpiGanNhat.setValue(ngay <= 0 ? "Hôm nay" : ngay + " ngày trước");
      kpiGanNhat.setCaption(DateFormats.formatDate(tq.lanMuaGanNhat()) + " • khách từ "
          + DateFormats.formatDate(tq.lanMuaDauTien()));
    }
    kpiPhieu.setValue(String.valueOf(tq.soPhieu()));
    if (tq.soPhieuDangMo() > 0) {
      kpiPhieu.setTrend(KpiCard.Trend.DOWN, tq.soPhieuDangMo() + " phiếu đang chờ xử lý");
    } else {
      kpiPhieu.setCaption(tq.soPhieu() == 0 ? "Chưa đổi/trả lần nào" : "Đã xử lý xong");
    }
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
