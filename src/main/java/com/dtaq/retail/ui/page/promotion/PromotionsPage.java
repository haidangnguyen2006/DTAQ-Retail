package com.dtaq.retail.ui.page.promotion;

import com.dtaq.retail.controller.KhuyenMaiController;
import com.dtaq.retail.controller.KhuyenMaiController.KetQua;
import com.dtaq.retail.dao.KhuyenMaiDao.DongApDung;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.service.KhuyenMaiExcel;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.ImportPreviewDialog;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.util.ExcelImporter;
import com.dtaq.retail.util.ExcelImporter.ImportResult;
import com.dtaq.retail.util.ExcelTemplate;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/** Trang quản lý chương trình khuyến mãi (UC012). */
public final class PromotionsPage implements Page {

  private static final String ALL_LINES = "Tất cả dòng sản phẩm";
  private static final String[] STATUS_FILTERS = {"Tất cả", "Đang áp dụng", "Đã kết thúc"};

  private final KhuyenMaiController controller;
  private final JPanel view;
  private final JComboBox<DongSanPham> lineFilter = new JComboBox<>();
  private final JComboBox<String> statusFilter = new JComboBox<>(STATUS_FILTERS);
  private final DtaqButton refreshButton = DtaqButton.secondary("Làm mới", "refresh-cw");
  private final DtaqButton addButton = DtaqButton.primary("Thêm khuyến mãi", "plus");
  private final DtaqButton toggleButton = DtaqButton.secondary("Bật / Dừng áp dụng");
  private final DtaqButton templateButton = DtaqButton.secondary("Tải mẫu", "download");
  private final DtaqButton importButton = DtaqButton.secondary("Nhập Excel");
  private final StatusPill countPill = StatusPill.neutral("0 chương trình");
  private final DefaultTableModel tableModel = DataTable.readOnlyModel(new String[] {
      "Mã khuyến mãi", "Tên chương trình", "Số dòng áp dụng", "% Giảm",
      "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái hiệu lực"}, 6);
  private final DataTable dataTable = new DataTable(tableModel, 48);
  private final StatefulView tableState;
  private final List<KhuyenMai> promotions = new ArrayList<>();
  private final List<DongSanPham> productLines = new ArrayList<>();
  private List<DanhMucSanPham> danhMuc = List.of();
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm", "search");
  /** Chỉ Quản lý được tạo và bật/dừng chương trình khuyến mãi (UC012). */
  private final boolean laQuanLy;
  /** Bảng chỉ nạp khuyến mãi sau lần tìm kiếm đầu tiên của người dùng. */
  private boolean daTimKiem;

  public PromotionsPage(KhuyenMaiController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");
    this.laQuanLy = CurrentSession.get()
        .map(nhanVien -> nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        .orElse(false);
    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16", "[grow]", "[][grow]"));

    PageHeader header = new PageHeader(
        "Quản lý khuyến mãi", "Tạo chương trình ưu đãi theo dòng sản phẩm và thời gian áp dụng.");
    addButton.setShortcutKey("F4");
    addButton.addActionListener(e -> openAddDialog());
    refreshButton.setShortcutKey("F5");
    refreshButton.addActionListener(e -> loadData());
    toggleButton.addActionListener(e -> toggleSelectedPromotion());
    templateButton.addActionListener(e -> xuatTep(view,
        "mau_nhap_khuyen_mai.xlsx", file -> ExcelTemplate.xuat(file, KhuyenMaiExcel.MAU, List.of())));
    importButton.addActionListener(e -> chonTepNhap());
    header.setTrailing(refreshButton, templateButton, importButton, addButton);
    addButton.setVisible(laQuanLy);
    addButton.setEnabled(laQuanLy);
    toggleButton.setVisible(laQuanLy);
    toggleButton.setEnabled(laQuanLy);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(150, 240, 125, 80, 120, 120, 150);
    dataTable.setOnRowDoubleClick(row -> toggleSelectedPromotion());
    tableState = new StatefulView(dataTable, "badge-percent", "Chưa có khuyến mãi",
        "Thử bộ lọc khác hoặc nhập chương trình từ Excel.");
    tableState.setOnRetry(this::loadData);

    Card tableCard = Card.standard();
    tableCard.add(buildFilterRow(), "growx, wrap");
    tableCard.add(toggleButton, "right, wrap, gapbottom 8");
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
    configureLineFilter();
    lineFilter.addActionListener(e -> renderFilteredList());
    statusFilter.addActionListener(e -> renderFilteredList());
  }

  private JPanel buildFilterRow() {
    JPanel row = new JPanel(new MigLayout("fillx, insets 0", "[][grow]16[][grow]16[]push[]", "[bottom]"));
    row.setOpaque(false);
    row.add(new javax.swing.JLabel("Dòng sản phẩm"));
    row.add(lineFilter, "growx");
    row.add(new javax.swing.JLabel("Trạng thái"));
    row.add(statusFilter, "growx");
    searchButton.addActionListener(e -> {
      daTimKiem = true;
      loadData();
    });
    row.add(searchButton);
    row.add(countPill, "align right, aligny center");
    return row;
  }

  private void configureLineFilter() {
    lineFilter.setRenderer(new DefaultListCellRenderer() {
      @Override
      public java.awt.Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        setText(value instanceof DongSanPham dsp ? dsp.tenDongSP() : ALL_LINES);
        return this;
      }
    });
  }

  @Override public PageId id() { return PageId.PROMOTIONS; }
  @Override public String title() { return "Quản lý khuyến mãi"; }
  @Override public String section() { return "Quản lý"; }
  @Override public JComponent view() { return view; }
  @Override public boolean scrollable() { return false; }
  @Override public void onShow() { loadData(); }

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "themKhuyenMai");
    actionMap.put("themKhuyenMai", new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { openAddDialog(); }
    });
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "taiLaiKhuyenMai");
    actionMap.put("taiLaiKhuyenMai", new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { loadData(); }
    });
  }

  /** Tải danh sách khuyến mãi và dòng sản phẩm ngoài EDT. */
  public void loadData() {
    tableState.showLoading();
    refreshButton.setEnabled(false);
    new SwingWorker<PageData, Void>() {
      @Override protected PageData doInBackground() {
        // Trước lần tìm kiếm đầu tiên chỉ nạp dòng sản phẩm cho bộ lọc, không nạp bảng.
        return new PageData(
            daTimKiem ? controller.layDanhSach() : List.of(), controller.layDanhSachDongSanPham(),
            laQuanLy ? controller.layDanhSachDanhMuc() : List.of());
      }
      @Override protected void done() {
        refreshButton.setEnabled(true);
        try {
          PageData data = get();
          promotions.clear();
          promotions.addAll(data.promotions());
          productLines.clear();
          productLines.addAll(data.productLines());
          danhMuc = data.danhMuc();
          DongSanPham selected = (DongSanPham) lineFilter.getSelectedItem();
          lineFilter.removeAllItems();
          lineFilter.addItem(null);
          productLines.forEach(lineFilter::addItem);
          if (selected != null) {
            productLines.stream().filter(dsp -> dsp.maDongSP().equals(selected.maDongSP()))
                .findFirst().ifPresent(lineFilter::setSelectedItem);
          }
          addButton.setEnabled(laQuanLy && !productLines.isEmpty());
          renderFilteredList();
        } catch (Exception e) {
          tableState.showError("Không thể tải danh sách khuyến mãi: " + e.getMessage());
        }
      }
    }.execute();
  }

  private void renderFilteredList() {
    tableModel.setRowCount(0);
    if (!daTimKiem) {
      countPill.setText("0 chương trình");
      tableState.showEmpty("search", "Chưa tra cứu", "Chọn bộ lọc rồi bấm Tìm kiếm.");
      return;
    }
    DongSanPham selectedLine = (DongSanPham) lineFilter.getSelectedItem();
    String status = (String) statusFilter.getSelectedItem();
    LocalDate today = LocalDate.now();
    List<KhuyenMai> filtered = promotions.stream()
        .filter(km -> selectedLine == null || km.chiTiet().stream()
            .anyMatch(ct -> ct.dongSanPham().maDongSP().equals(selectedLine.maDongSP())))
        .filter(km -> switch (status == null ? STATUS_FILTERS[0] : status) {
          case "Đang áp dụng" -> dangHieuLuc(km, today);
          case "Đã kết thúc" -> km.ngayKetThuc().isBefore(today);
          default -> true;
        })
        .toList();
    if (filtered.isEmpty()) {
      countPill.setText("0 chương trình");
      tableState.showEmpty();
      return;
    }
    countPill.setText(filtered.size() + " chương trình");
    for (KhuyenMai km : filtered) {
      tableModel.addRow(new Object[] {
          km.maKM(), km.tenKM(), km.chiTiet().size(),
          km.tyLeGiam().stripTrailingZeros().toPlainString(), km.ngayBatDau(), km.ngayKetThuc(),
          trangThai(km, today)
      });
    }
    tableState.showContent();
  }

  private static boolean dangHieuLuc(KhuyenMai km, LocalDate today) {
    return km.dangApDung() && !today.isBefore(km.ngayBatDau()) && !today.isAfter(km.ngayKetThuc());
  }

  private static String trangThai(KhuyenMai km, LocalDate today) {
    if (dangHieuLuc(km, today)) return "Đang áp dụng";
    if (km.ngayKetThuc().isBefore(today)) return "Đã kết thúc";
    if (!km.dangApDung()) return "Đã dừng";
    return "Sắp áp dụng";
  }

  private void openAddDialog() {
    if (!laQuanLy) {
      Toast.show(view, "Chỉ Quản lý được tạo chương trình khuyến mãi", Toast.Kind.ERROR);
      return;
    }
    if (productLines.isEmpty()) {
      Toast.show(view, "Chưa có dòng sản phẩm để gắn khuyến mãi", Toast.Kind.ERROR);
      return;
    }
    Frame parent = ConfirmDialog.frameOf(view);
    AddPromotionDialog dialog = new AddPromotionDialog(parent, List.copyOf(productLines), danhMuc);
    dialog.setOnSubmit((form, luu) -> {
      dialog.setBusy(true);
      new SwingWorker<KetQua<List<DongApDung>>, Void>() {
        @Override protected KetQua<List<DongApDung>> doInBackground() {
          return controller.xemTruocDongApDung(
              form.maDongSP(), form.maDanhMuc(), form.soNgayTonToiThieu());
        }
        @Override protected void done() {
          dialog.setBusy(false);
          try {
            var result = get();
            if (!result.thanhCong()) {
              dialog.showPreview(List.of());
              dialog.setErrorMessage(result.thongBao());
              return;
            }
            dialog.showPreview(result.duLieu());
            if (luu && JOptionPane.showConfirmDialog(dialog,
                "Tạo 1 chương trình với " + result.duLieu().size() + " dòng sản phẩm?",
                "Xác nhận tạo khuyến mãi", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
              luuKhuyenMai(dialog, form, result.duLieu().stream()
                  .map(dong -> dong.dong().maDongSP()).toList());
            }
          } catch (Exception e) {
            dialog.setErrorMessage("Không thể xem trước dòng áp dụng");
          }
        }
      }.execute();
    });
    dialog.setVisible(true);
  }

  private void luuKhuyenMai(
      AddPromotionDialog dialog, AddPromotionDialog.PromotionFormData form, List<String> maDong) {
    dialog.setBusy(true);
    new SwingWorker<KetQua<KhuyenMai>, Void>() {
      @Override protected KetQua<KhuyenMai> doInBackground() {
        return controller.themMoiTheoDieuKien(form.ten(), form.moTa(), form.tyLeGiam(),
            form.ngayBatDau(), form.ngayKetThuc(), form.maDongSP(), form.maDanhMuc(),
            form.soNgayTonToiThieu(), form.dangApDung(), maDong);
      }
      @Override protected void done() {
        dialog.setBusy(false);
        try {
          var result = get();
          if (result.thanhCong()) {
            dialog.dispose();
            Toast.show(view, result.thongBao(), Toast.Kind.SUCCESS);
            daTimKiem = true;
            loadData();
          } else {
            dialog.setErrorMessage(result.thongBao());
          }
        } catch (Exception e) {
          dialog.setErrorMessage("Không thể lưu khuyến mãi");
        }
      }
    }.execute();
  }

  private void chonTepNhap() {
    JFileChooser chooser = chonTepExcel();
    if (chooser.showOpenDialog(view) != JFileChooser.APPROVE_OPTION) return;
    File tep = chooser.getSelectedFile();
    importButton.setEnabled(false);
    chayExcel(() -> controller.xemTruocNhapExcel(ExcelImporter.doc(tep, KhuyenMaiExcel.MAU)),
        this::moXemTruocNhap, () -> importButton.setEnabled(true));
  }

  private void moXemTruocNhap(ImportResult ketQua) {
    var cot = KhuyenMaiExcel.MAU.cot().stream().map(ExcelImporter.CotNhap::tieuDe).toList();
    ImportPreviewDialog dialog = new ImportPreviewDialog(
        ConfirmDialog.frameOf(view), "Nhập khuyến mãi từ Excel", ketQua, cot, 50);
    dialog.setOnChooseAnother(() -> SwingUtilities.invokeLater(this::chonTepNhap));
    dialog.setOnExportErrors(loi -> xuatTep(dialog,
        "loi_nhap_khuyen_mai.xlsx", file -> ExcelImporter.xuatBaoCaoLoi(file, loi)));
    dialog.setOnConfirm(tep -> {
      dialog.setBusy(true);
      chayExcel(() -> controller.nhapExcel(tep), daNhap -> {
        dialog.dispose();
        if (!daNhap.ketQua().hopLe()) {
          moXemTruocNhap(daNhap.ketQua());
        } else {
          Toast.show(view, "Đã nhập " + daNhap.soChuongTrinh() + " chương trình", Toast.Kind.SUCCESS);
          daTimKiem = true;
          loadData();
        }
      }, () -> dialog.setBusy(false));
    });
    dialog.setVisible(true);
  }

  private void xuatTep(Component parent, String tenMacDinh, Consumer<File> ghiTep) {
    JFileChooser chooser = chonTepExcel();
    chooser.setSelectedFile(new File(tenMacDinh));
    if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
    File daChon = chooser.getSelectedFile();
    File dich = daChon.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
        ? daChon : new File(daChon.getParentFile(), daChon.getName() + ".xlsx");
    if (dich.exists() && JOptionPane.showConfirmDialog(parent,
        "Ghi đè tệp " + dich.getName() + "?", "Xác nhận ghi đè",
        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
    chayExcel(() -> {
      ghiTep.accept(dich);
      return dich.getName();
    }, ten -> Toast.show(view, "Đã xuất " + ten, Toast.Kind.SUCCESS), () -> {});
  }

  private static JFileChooser chonTepExcel() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
    chooser.setAcceptAllFileFilterUsed(false);
    return chooser;
  }

  private <T> void chayExcel(Callable<T> thaoTac, Consumer<T> khiXong, Runnable khoiPhuc) {
    new SwingWorker<T, Void>() {
      @Override protected T doInBackground() throws Exception {
        return thaoTac.call();
      }
      @Override protected void done() {
        khoiPhuc.run();
        try {
          khiXong.accept(get());
        } catch (Exception e) {
          Toast.show(view, "Không thể xử lý tệp Excel", Toast.Kind.ERROR);
        }
      }
    }.execute();
  }

  private void toggleSelectedPromotion() {
    if (!laQuanLy) {
      Toast.show(view, "Chỉ Quản lý được bật/dừng chương trình khuyến mãi", Toast.Kind.ERROR);
      return;
    }
    int row = dataTable.getTable().getSelectedRow();
    if (row < 0) {
      Toast.show(view, "Vui lòng chọn chương trình khuyến mãi", Toast.Kind.ERROR);
      return;
    }
    String maKM = String.valueOf(tableModel.getValueAt(dataTable.getTable().convertRowIndexToModel(row), 0));
    KhuyenMai selected = promotions.stream().filter(km -> km.maKM().equals(maKM)).findFirst().orElse(null);
    if (selected == null) return;
    boolean bat = !selected.dangApDung();
    new SwingWorker<KetQua<Void>, Void>() {
      @Override protected KetQua<Void> doInBackground() {
        return bat ? controller.batDauApDung(maKM) : controller.dungApDung(maKM);
      }
      @Override protected void done() {
        try {
          KetQua<Void> result = get();
          Toast.show(view, result.thongBao(), result.thanhCong() ? Toast.Kind.SUCCESS : Toast.Kind.ERROR);
          if (result.thanhCong()) loadData();
        } catch (Exception e) {
          Toast.show(view, "Không thể cập nhật trạng thái: " + e.getMessage(), Toast.Kind.ERROR);
        }
      }
    }.execute();
  }

  private record PageData(
      List<KhuyenMai> promotions, List<DongSanPham> productLines, List<DanhMucSanPham> danhMuc) {}
}
