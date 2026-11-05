package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.controller.ThongKeNhanVienController;
import com.dtaq.retail.controller.ThongKeNhanVienController.BaoCao;
import com.dtaq.retail.controller.ThongKeNhanVienController.DoiChieu;
import com.dtaq.retail.controller.ThongKeNhanVienController.MucNhanXet;
import com.dtaq.retail.controller.ThongKeNhanVienController.NhanXet;
import com.dtaq.retail.controller.ThongKeNhanVienController.PhanTich;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DieuChinhDoiChieu;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DongThongKeNhanVien;
import com.dtaq.retail.dao.ThongKeNhanVienDao.HoaDonDoiChieu;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.BarChartPanel;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.KyBaoCaoFilter;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.Responsive;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.PersonNames;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.util.TienUtil;
import java.awt.Frame;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Màn hình Thống kê doanh số theo nhân viên (UC016): chọn kỳ và phạm vi nhân viên, xem số hóa đơn
 * và doanh số trước thuế đã điều chỉnh đổi/trả, đối chiếu từng nhân viên và xuất Excel. Chỉ đọc.
 */
public final class SalesByEmployeePage implements Page {

  private static final Logger log = LoggerFactory.getLogger(SalesByEmployeePage.class);

  private static final String LOAD_ERROR = "Không thể tải báo cáo. Vui lòng thử lại";
  private static final int SO_COT_BIEU_DO = 8;
  private static final LuaChon TAT_CA = new LuaChon(null, "Tất cả nhân viên");

  /** Một mục của bộ lọc phạm vi nhân viên; {@code maNV = null} là tất cả. */
  private record LuaChon(String maNV, String nhan) {
    @Override
    public String toString() {
      return nhan;
    }
  }

  private final ThongKeNhanVienController controller;
  private final JPanel view;
  private final KyBaoCaoFilter kyFilter = new KyBaoCaoFilter();
  private final FormField nhanVienField = FormField.combo("Phạm vi nhân viên", new LuaChon[] {TAT_CA});
  private final DtaqButton exportBtn = DtaqButton.secondary("Xuất Excel");
  private final KpiCard kpiSauDieuChinh = new KpiCard("Doanh số thực", "—", "Sau điều chỉnh đổi/trả");
  private final KpiCard kpiHoaDon = new KpiCard("Số hóa đơn", "—", "Hóa đơn đã thanh toán trong kỳ");
  private final KpiCard kpiTrungBinh = new KpiCard("TB mỗi hóa đơn", "—", "Doanh số thực / số hóa đơn");
  private final KpiCard kpiDieuChinh = new KpiCard("Hàng trả trừ doanh số", "—", "Phiếu trả hoàn tất trong kỳ");
  private final BarChartPanel chart = new BarChartPanel();
  private final JPanel insightList = new JPanel(new MigLayout("fillx, insets 0, wrap 1", "[grow,fill]", "[]8[]"));
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã nhân viên", "Họ và tên", "Số hóa đơn", "Doanh số trước thuế (VND)",
      "Điều chỉnh đổi/trả (VND)", "Sau điều chỉnh (VND)", "TB/hóa đơn (VND)", "Tỉ lệ trả",
      "Tỉ trọng", "Thao tác"}, 9);
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;
  /** Dòng đang hiển thị, sắp theo doanh số thực giảm dần; chỉ số dòng bảng tham chiếu danh sách này. */
  private final List<DongThongKeNhanVien> dongHienThi = new ArrayList<>();

  private BaoCao baoCao;
  private boolean daTaiNhanVien;
  private int loadRequest;

  /**
   * Khởi tạo trang thống kê doanh số theo nhân viên.
   *
   * @param controller bộ điều khiển thống kê; không được {@code null}
   */
  public SalesByEmployeePage(ThongKeNhanVienController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"
            + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader("Thống kê doanh số theo nhân viên", null);
    exportBtn.setEnabled(false);
    exportBtn.addActionListener(e -> exportReport());
    header.setTrailing(exportBtn);
    view.add(header, "growx, wrap");

    Card filterCard = Card.standard();
    // Hai cột cùng cấu trúc nhãn – ô – dòng thông báo nên căn đỉnh là thẳng hàng.
    JPanel phamViRow = new JPanel(new MigLayout("insets 0", "[320!]16[]", "[top]"));
    phamViRow.setOpaque(false);
    phamViRow.add(nhanVienField, "growx");
    phamViRow.add(buildPresetField());
    filterCard.add(phamViRow, "growx, wrap");
    filterCard.add(kyFilter, "growx");
    kyFilter.setOnApply(this::loadReport);
    view.add(filterCard, "growx, wrap");

    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0, gapx 16", "[grow,fill][grow,fill][grow,fill][grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    Responsive.grid(kpiRow, 220, 4);
    kpiRow.add(kpiSauDieuChinh);
    kpiRow.add(kpiHoaDon);
    kpiRow.add(kpiTrungBinh);
    kpiRow.add(kpiDieuChinh);
    view.add(kpiRow, "growx, wrap");

    chart.setUnit("Triệu ₫", "tr");
    chart.setEmptyText("Chưa có doanh số trong kỳ");
    Card chartCard = Card.section("Doanh số thực theo nhân viên", "Top " + SO_COT_BIEU_DO + ", triệu đồng", null);
    chartCard.add(chart, "grow, h 260!");
    insightList.setOpaque(false);
    Card insightCard = Card.section("Nhận xét nhanh", "Tự rút ra từ số liệu của kỳ", null);
    insightCard.add(insightList, "growx");
    JPanel phanTichRow = new JPanel(new MigLayout("fillx, insets 0, gapx 16", "[grow 60,fill][grow 40,fill]", "[top]"));
    phanTichRow.setOpaque(false);
    Responsive.grid(phanTichRow, 420, 2);
    phanTichRow.add(chartCard, "growy");
    phanTichRow.add(insightCard, "growy");
    view.add(phanTichRow, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setNumberColumn(2);
    dataTable.setMoneyColumn(3);
    dataTable.setMoneyColumn(4);
    dataTable.setMoneyColumn(5);
    dataTable.setMoneyColumn(6);
    dataTable.setNumberColumn(7);
    dataTable.setNumberColumn(8);
    dataTable.setColumnWidths(150, 170, 80, 150, 140, 150, 130, 80, 80, 110);
    dataTable.setActionColumn(9, RowAction.secondary("Đối chiếu", this::openDoiChieu));
    dataTable.setOnRowDoubleClick(this::openDoiChieu);
    dataTable.setVisibleRows(3, 12);
    tableState = new StatefulView(dataTable, "chart-no-axes-combined", "Chưa có báo cáo",
        "Chọn kỳ và phạm vi nhân viên rồi bấm Áp dụng.");
    tableState.setOnRetry(() -> kyFilter.apply());

    Card tableCard = Card.section("Chi tiết theo nhân viên",
        "Sắp theo doanh số thực giảm dần • nhấp đúp một dòng để đối chiếu hóa đơn", null);
    tableCard.add(tableState, "growx");
    view.add(tableCard, "growx");
  }

  /**
   * Nút chọn nhanh kỳ hay dùng, bọc trong {@link FormField} có nhãn "Chọn nhanh" để thẳng hàng với
   * ô phạm vi nhân viên bên cạnh; vẫn có thể nhập tay từ ngày – đến ngày.
   */
  private FormField buildPresetField() {
    JPanel row = new JPanel(new MigLayout("insets 0, gapx 8", "", "[]"));
    row.setOpaque(false);
    // Đặt sẵn style rỗng để FormField không gán style ô nhập văn bản cho JPanel.
    row.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "");
    LocalDate homNay = LocalDate.now();
    addPreset(row, "Hôm nay", homNay, homNay);
    addPreset(row, "7 ngày", homNay.minusDays(6), homNay);
    addPreset(row, "Tháng này", homNay.withDayOfMonth(1), homNay);
    LocalDate dauThangTruoc = homNay.minusMonths(1).withDayOfMonth(1);
    addPreset(row, "Tháng trước", dauThangTruoc, dauThangTruoc.withDayOfMonth(dauThangTruoc.lengthOfMonth()));
    addPreset(row, "Năm nay", homNay.withDayOfYear(1), homNay);
    return new FormField("Chọn nhanh", row);
  }

  private void addPreset(JPanel row, String nhan, LocalDate tu, LocalDate den) {
    DtaqButton nut = DtaqButton.secondary(nhan);
    nut.addActionListener(e -> {
      kyFilter.setDateRange(tu, den);
      kyFilter.apply();
    });
    row.add(nut, "h 36!");
  }

  // ---------------------------------------------------------------- tải dữ liệu

  private void loadNhanVien() {
    run(controller::danhSachNhanVien, nhanViens -> {
      List<LuaChon> luaChon = new ArrayList<>();
      luaChon.add(TAT_CA);
      for (NhanVien nv : nhanViens) {
        luaChon.add(new LuaChon(nv.maNV(), nv.maNV() + " • " + nv.hoTen()
            + (nv.trangThaiHoatDong() ? "" : " (đã khóa)")));
      }
      combo().setModel(new DefaultComboBoxModel<>(luaChon.toArray(new LuaChon[0])));
      daTaiNhanVien = true;
    }, message -> Toast.error(view, message));
  }

  private void loadReport(DateRange ky) {
    int request = ++loadRequest;
    String maNV = ((LuaChon) combo().getSelectedItem()).maNV();
    tableState.showLoading();
    for (KpiCard kpi : List.of(kpiSauDieuChinh, kpiHoaDon, kpiTrungBinh, kpiDieuChinh)) {
      kpi.setLoading();
    }
    exportBtn.setEnabled(false);
    run(() -> controller.phanTich(ky, maNV), ketQua -> {
      if (request == loadRequest) {
        render(ketQua);
      }
    }, message -> {
      if (request == loadRequest) {
        baoCao = null;
        resetKpi();
        renderInsights(List.of());
        chart.setData(List.of());
        tableState.showError(message);
      }
    });
  }

  private void render(PhanTich phanTich) {
    BaoCao ketQua = phanTich.hienTai();
    BaoCao truoc = phanTich.kyTruoc();
    baoCao = ketQua;
    tableModel.setRowCount(0);
    dongHienThi.clear();
    renderInsights(phanTich.nhanXet());
    if (ketQua.dong().isEmpty()) {
      resetKpi();
      chart.setData(List.of());
      tableState.showEmpty("chart-no-axes-combined", "Không có phát sinh",
          "Không có hóa đơn đã thanh toán hoặc hàng trả hoàn tất trong kỳ đã chọn.");
      return;
    }
    dongHienThi.addAll(ketQua.dong().stream()
        .sorted(Comparator.comparing(DongThongKeNhanVien::sauDieuChinh).reversed()).toList());
    BigDecimal tong = ketQua.tongSauDieuChinh();
    for (DongThongKeNhanVien d : dongHienThi) {
      tableModel.addRow(new Object[] {
          d.maNV(), d.dangHoatDong() ? d.hoTen() : d.hoTen() + " (đã khóa)", d.soHoaDon(),
          d.doanhSo(), d.dieuChinh(), d.sauDieuChinh(),
          ThongKeNhanVienController.trungBinhHoaDon(d.sauDieuChinh(), d.soHoaDon()),
          ThongKeNhanVienController.dinhDangPhanTram(ThongKeNhanVienController.phanTram(d.dieuChinh(), d.doanhSo())),
          ThongKeNhanVienController.dinhDangPhanTram(ThongKeNhanVienController.phanTram(d.sauDieuChinh(), tong)),
          ""
      });
    }
    chart.setData(dongHienThi.stream().limit(SO_COT_BIEU_DO)
        .map(d -> new BarChartPanel.BarPoint(PersonNames.shortName(d.hoTen()),
            d.sauDieuChinh().doubleValue() / 1_000_000d))
        .toList());

    kpiSauDieuChinh.setValue(TienUtil.dinhDang(tong));
    setTrend(kpiSauDieuChinh, phanTich.bienDong(tong, truoc.tongSauDieuChinh()),
        "Trước điều chỉnh " + TienUtil.dinhDang(ketQua.tongDoanhSo()));
    kpiHoaDon.setValue(String.valueOf(ketQua.tongSoHoaDon()));
    setTrend(kpiHoaDon, phanTich.bienDong(BigDecimal.valueOf(ketQua.tongSoHoaDon()),
        BigDecimal.valueOf(truoc.tongSoHoaDon())), "Kỳ trước " + truoc.tongSoHoaDon() + " hóa đơn");
    BigDecimal tb = ThongKeNhanVienController.trungBinhHoaDon(tong, ketQua.tongSoHoaDon());
    kpiTrungBinh.setValue(TienUtil.dinhDang(tb));
    setTrend(kpiTrungBinh, phanTich.bienDong(tb,
        ThongKeNhanVienController.trungBinhHoaDon(truoc.tongSauDieuChinh(), truoc.tongSoHoaDon())),
        "Doanh số thực / số hóa đơn");
    kpiDieuChinh.setValue(TienUtil.dinhDang(ketQua.tongDieuChinh()));
    kpiDieuChinh.setCaption("= " + ThongKeNhanVienController.dinhDangPhanTram(
        ThongKeNhanVienController.phanTram(ketQua.tongDieuChinh(), ketQua.tongDoanhSo()))
        + " doanh số trước điều chỉnh");
    exportBtn.setEnabled(true);
    tableState.showContent();
  }

  /** Chú thích KPI: mũi tên tăng/giảm so với kỳ trước, hoặc chú thích mặc định khi kỳ trước bằng 0. */
  private static void setTrend(KpiCard kpi, BigDecimal bienDong, String macDinh) {
    if (bienDong == null) {
      kpi.setCaption(macDinh + " • kỳ trước chưa có số liệu");
    } else if (bienDong.signum() == 0) {
      kpi.setTrend(KpiCard.Trend.NEUTRAL, "Không đổi so với kỳ trước");
    } else {
      kpi.setTrend(bienDong.signum() > 0 ? KpiCard.Trend.UP : KpiCard.Trend.DOWN,
          ThongKeNhanVienController.dinhDangPhanTram(bienDong.abs()) + " so với kỳ trước");
    }
  }

  private void renderInsights(List<NhanXet> nhanXet) {
    insightList.removeAll();
    if (nhanXet.isEmpty()) {
      JLabel trong = new JLabel("Chưa đủ số liệu để rút ra nhận xét cho kỳ này.");
      trong.setFont(DtaqFonts.BODY_13);
      trong.setForeground(DtaqColors.TEXT_MUTED);
      insightList.add(trong);
    }
    for (NhanXet nx : nhanXet) {
      insightList.add(new InfoCallout(
          nx.muc() == MucNhanXet.CANH_BAO ? InfoCallout.Kind.WARNING : InfoCallout.Kind.INFO,
          // Cố định bề rộng để nhãn HTML xuống dòng thay vì kéo giãn thẻ.
          "<div style='width:330px'>" + nx.noiDung().replace("&", "&amp;").replace("<", "&lt;") + "</div>"), "wmin 0");
    }
    insightList.revalidate();
    insightList.repaint();
  }

  private void resetKpi() {
    for (KpiCard kpi : List.of(kpiSauDieuChinh, kpiHoaDon, kpiTrungBinh, kpiDieuChinh)) {
      kpi.setValue("—");
      kpi.setCaption("Không có phát sinh trong kỳ");
    }
  }

  // ---------------------------------------------------------------- đối chiếu (10.a)

  private void openDoiChieu(int modelRow) {
    if (baoCao == null || modelRow < 0 || modelRow >= baoCao.dong().size()) {
      return;
    }
    DongThongKeNhanVien dong = baoCao.dong().get(modelRow);
    DateRange ky = baoCao.ky();
    run(() -> controller.doiChieu(ky, dong.maNV()),
        doiChieu -> new DoiChieuDialog(ConfirmDialog.frameOf(view), dong, ky, doiChieu).setVisible(true),
        message -> Toast.error(view, message));
  }

  /** Hộp thoại liệt kê hóa đơn và phiếu trả dùng để tính doanh số của một nhân viên. */
  private static final class DoiChieuDialog extends ModalDialog {

    DoiChieuDialog(Frame parent, DongThongKeNhanVien dong, DateRange ky, DoiChieu doiChieu) {
      super(parent, "Đối chiếu doanh số " + dong.maNV(), dong.hoTen() + " • "
          + DateFormats.formatDate(ky.tuNgay()) + " – " + DateFormats.formatDate(ky.denNgay()), 920);

      DefaultTableModel hoaDonModel = DataTable.readOnlyModel(new String[] {
          "Mã hóa đơn", "Ngày lập", "Khách hàng", "Số sản phẩm", "Tiền trước thuế (VND)"});
      for (HoaDonDoiChieu hd : doiChieu.hoaDons()) {
        hoaDonModel.addRow(new Object[] {
            hd.maHD(), DateFormats.formatDate(hd.ngayLap()), hd.tenKhachHang(), hd.soSanPham(), hd.tienTruocThue()});
      }
      DataTable hoaDonTable = new DataTable(hoaDonModel);
      hoaDonTable.setCodeColumn(0);
      hoaDonTable.setNumberColumn(3);
      hoaDonTable.setMoneyColumn(4);
      hoaDonTable.setVisibleRows(2, 6);

      DefaultTableModel dieuChinhModel = DataTable.readOnlyModel(new String[] {
          "Mã phiếu trả", "Hóa đơn gốc", "Ngày hoàn tất", "Số máy trả", "Giá trị trừ (VND)"});
      for (DieuChinhDoiChieu dc : doiChieu.dieuChinhs()) {
        dieuChinhModel.addRow(new Object[] {
            dc.maPhieuTra(), dc.maHD(), DateFormats.formatDate(dc.ngayHoanTat()), dc.soMay(), dc.giaTriTru()});
      }
      DataTable dieuChinhTable = new DataTable(dieuChinhModel);
      dieuChinhTable.setCodeColumn(0);
      dieuChinhTable.setNumberColumn(3);
      dieuChinhTable.setMoneyColumn(4);
      dieuChinhTable.setVisibleRows(2, 4);

      getBody().add(sectionTitle("Hóa đơn đã thanh toán (" + doiChieu.hoaDons().size() + ") • "
          + TienUtil.dinhDang(dong.doanhSo())), "growx, wrap, gapbottom 8");
      getBody().add(new StatefulView(hoaDonTable, "receipt-text", "Không có hóa đơn trong kỳ", null) {
        { if (!doiChieu.hoaDons().isEmpty()) showContent(); }
      }, "growx, wrap, gapbottom 16");
      getBody().add(sectionTitle("Điều chỉnh hàng trả (" + doiChieu.dieuChinhs().size() + ") • −"
          + TienUtil.dinhDang(dong.dieuChinh())), "growx, wrap, gapbottom 8");
      getBody().add(new StatefulView(dieuChinhTable, "receipt-text", "Không có hàng trả trong kỳ", null) {
        { if (!doiChieu.dieuChinhs().isEmpty()) showContent(); }
      }, "growx, wrap, gapbottom 16");
      getBody().add(sectionTitle("Sau điều chỉnh: " + TienUtil.dinhDang(dong.sauDieuChinh())), "growx");

      DtaqButton dong2 = DtaqButton.primary("Đóng");
      dong2.addActionListener(e -> dispose());
      setPrimaryAction(dong2);
      setActions(dong2);
    }

    private static JLabel sectionTitle(String text) {
      JLabel label = new JLabel(text);
      label.setFont(DtaqFonts.CARD_TITLE);
      label.setForeground(DtaqColors.TEXT);
      return label;
    }
  }

  // ---------------------------------------------------------------- xuất Excel

  private void exportReport() {
    BaoCao hienThi = baoCao;
    if (hienThi == null) {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Xuất báo cáo doanh số theo nhân viên");
    chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
    chooser.setSelectedFile(new File("doanh-so-theo-nhan-vien.xlsx"));
    if (chooser.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File selected = chooser.getSelectedFile();
    File target = selected.getName().toLowerCase().endsWith(".xlsx")
        ? selected : new File(selected.getParentFile(), selected.getName() + ".xlsx");
    exportBtn.setEnabled(false);
    run(() -> controller.xuatExcel(target, hienThi), loi -> {
      exportBtn.setEnabled(baoCao != null);
      if (loi == null) {
        Toast.success(view, "Đã xuất " + target.getName());
      } else {
        ConfirmDialog.info(view, "Xuất Excel thất bại", loi, null);
      }
    }, message -> {
      exportBtn.setEnabled(baoCao != null);
      ConfirmDialog.info(view, "Xuất Excel thất bại", message, null);
    });
  }

  // ---------------------------------------------------------------- tiện ích

  @SuppressWarnings("unchecked")
  private JComboBox<LuaChon> combo() {
    return (JComboBox<LuaChon>) nhanVienField.getInputComponent();
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
          log.warn("Thống kê doanh số theo nhân viên thất bại (exception={})",
              cause == null ? "none" : cause.getClass().getName());
          onError.accept(cause instanceof SecurityException ? cause.getMessage() : LOAD_ERROR);
        }
      }
    }.execute();
  }

  // ---------------------------------------------------------------- Page

  @Override
  public void onShow() {
    if (!daTaiNhanVien) {
      loadNhanVien();
    }
    kyFilter.apply();
  }

  @Override
  public boolean scrollable() {
    return false;
  }

  @Override
  public PageId id() {
    return PageId.SALES_BY_EMPLOYEE;
  }

  @Override
  public String title() {
    return "Thống kê doanh số theo nhân viên";
  }

  @Override
  public String section() {
    return "Quản lý";
  }

  @Override
  public JComponent view() {
    return view;
  }
}
