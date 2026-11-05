package com.dtaq.retail.ui.page.report;

import com.dtaq.retail.controller.ThongKeNhanVienController;
import com.dtaq.retail.controller.ThongKeNhanVienController.BaoCao;
import com.dtaq.retail.controller.ThongKeNhanVienController.DoiChieu;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DieuChinhDoiChieu;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DongThongKeNhanVien;
import com.dtaq.retail.dao.ThongKeNhanVienDao.HoaDonDoiChieu;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.KpiCard;
import com.dtaq.retail.ui.component.KyBaoCaoFilter;
import com.dtaq.retail.ui.component.ModalDialog;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.DateRange;
import com.dtaq.retail.util.TienUtil;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
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
  private final DtaqButton exportBtn = DtaqButton.secondary("Xuất Excel", "download");
  private final KpiCard kpiHoaDon = new KpiCard("Số hóa đơn", "—", "Hóa đơn đã thanh toán trong kỳ");
  private final KpiCard kpiDoanhSo = new KpiCard("Doanh số trước thuế", "—", "Trước điều chỉnh đổi/trả");
  private final KpiCard kpiDieuChinh = new KpiCard("Điều chỉnh đổi/trả", "—", "Hàng trả hoàn tất trong kỳ");
  private final KpiCard kpiSauDieuChinh = new KpiCard("Sau điều chỉnh", "—", "Doanh số thực tế của kỳ");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã nhân viên", "Họ và tên", "Số hóa đơn", "Doanh số trước thuế (VND)",
      "Điều chỉnh đổi/trả (VND)", "Sau điều chỉnh (VND)", "Thao tác"}, 6);
  private final DataTable dataTable = new DataTable(tableModel, 52);
  private final StatefulView tableState;

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
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[]" + DtaqSpacing.SECTION_GAP + "[]"
            + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    PageHeader header = new PageHeader("Thống kê doanh số theo nhân viên",
        "Số hóa đơn và doanh số trước thuế theo người lập, đã điều chỉnh theo hàng trả.");
    exportBtn.setEnabled(false);
    exportBtn.addActionListener(e -> exportReport());
    header.setTrailing(exportBtn);
    view.add(header, "growx, wrap");

    Card filterCard = Card.standard();
    filterCard.add(nhanVienField, "w 360!, wrap");
    filterCard.add(kyFilter, "growx");
    kyFilter.setOnApply(this::loadReport);
    view.add(filterCard, "growx, wrap");

    JPanel kpiRow = new JPanel(new MigLayout("fillx, insets 0, gapx 16", "[grow,fill][grow,fill][grow,fill][grow,fill]", "[]"));
    kpiRow.setOpaque(false);
    kpiRow.add(kpiHoaDon);
    kpiRow.add(kpiDoanhSo);
    kpiRow.add(kpiDieuChinh);
    kpiRow.add(kpiSauDieuChinh);
    view.add(kpiRow, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setNumberColumn(2);
    dataTable.setMoneyColumn(3);
    dataTable.setMoneyColumn(4);
    dataTable.setMoneyColumn(5);
    dataTable.setColumnWidths(170, 220, 110, 200, 200, 200, 120);
    dataTable.setActionColumn(6, RowAction.secondary("Đối chiếu", this::openDoiChieu));
    dataTable.setOnRowDoubleClick(this::openDoiChieu);
    tableState = new StatefulView(dataTable, "chart-no-axes-combined", "Chưa có báo cáo",
        "Chọn kỳ và phạm vi nhân viên rồi bấm Áp dụng.");
    tableState.setOnRetry(() -> kyFilter.apply());

    Card tableCard = Card.standard();
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
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
    exportBtn.setEnabled(false);
    run(() -> controller.thongKe(ky, maNV), ketQua -> {
      if (request == loadRequest) {
        render(ketQua);
      }
    }, message -> {
      if (request == loadRequest) {
        baoCao = null;
        resetKpi();
        tableState.showError(message);
      }
    });
  }

  private void render(BaoCao ketQua) {
    baoCao = ketQua;
    tableModel.setRowCount(0);
    if (ketQua.dong().isEmpty()) {
      resetKpi();
      tableState.showEmpty("chart-no-axes-combined", "Không có phát sinh",
          "Không có hóa đơn đã thanh toán hoặc hàng trả hoàn tất trong kỳ đã chọn.");
      return;
    }
    for (DongThongKeNhanVien d : ketQua.dong()) {
      tableModel.addRow(new Object[] {
          d.maNV(), d.dangHoatDong() ? d.hoTen() : d.hoTen() + " (đã khóa)", d.soHoaDon(),
          d.doanhSo(), d.dieuChinh(), d.sauDieuChinh(), ""
      });
    }
    kpiHoaDon.setValue(String.valueOf(ketQua.tongSoHoaDon()));
    kpiDoanhSo.setValue(TienUtil.dinhDang(ketQua.tongDoanhSo()));
    kpiDieuChinh.setValue(TienUtil.dinhDang(ketQua.tongDieuChinh()));
    kpiSauDieuChinh.setValue(TienUtil.dinhDang(ketQua.tongSauDieuChinh()));
    exportBtn.setEnabled(true);
    tableState.showContent();
  }

  private void resetKpi() {
    kpiHoaDon.setValue("0");
    kpiDoanhSo.setValue(TienUtil.dinhDang(0));
    kpiDieuChinh.setValue(TienUtil.dinhDang(0));
    kpiSauDieuChinh.setValue(TienUtil.dinhDang(0));
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
