package com.dtaq.retail.ui.page.defective;

import com.dtaq.retail.controller.SanPhamLoiController;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.SegmentedControl;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình quản lý sản phẩm lỗi và theo dõi tình trạng xử lý bảo hành, trả hãng.
 */
public class DefectiveProductsPage implements Page {

  private final SanPhamLoiController controller;
  private final JPanel view;

  private final FormField searchField = FormField.search("Tìm kiếm", "Nhập mã lỗi, số serial hoặc tên máy");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private final DtaqButton refreshButton = DtaqButton.secondary("Làm mới", "refresh-cw");
  private final DtaqButton addDefectButton = DtaqButton.primary("Ghi nhận máy lỗi", "plus");
  private final StatusPill countPill = StatusPill.neutral("0 máy lỗi");

  private final SegmentedControl statusFilter = new SegmentedControl(
      new String[] {"Tất cả", "Chờ xử lý", "Gửi bảo hành", "Trả NCC", "Thanh lý"}, 0);

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã lỗi", "Số Serial", "Dòng sản phẩm", "Ngày ghi nhận", "Tình trạng", "Mô tả lỗi", "Thao tác"}, 6);
  private final DataTable dataTable = new DataTable(tableModel, 50);
  private final StatefulView tableState;

  private final List<SanPhamLoi> cachedList = new ArrayList<>();
  private TrangThaiXuLyLoi currentStatusFilter = null;
  private String currentSearchKeyword = "";
  /** Bảng chỉ nạp dữ liệu sau lần tìm kiếm đầu tiên của người dùng. */
  private boolean daTimKiem;

  /**
   * Khởi tạo trang quản lý sản phẩm lỗi.
   *
   * @param controller bộ điều khiển xử lý nghiệp vụ sản phẩm lỗi; không được {@code null}
   */
  public DefectiveProductsPage(SanPhamLoiController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Quản lý sản phẩm lỗi",
        "Tiếp nhận máy lỗi, chuyển trạng thái bảo hành và đồng bộ tình trạng kho.");

    addDefectButton.setShortcutKey("F4");
    addDefectButton.addActionListener(e -> openAddDialog());

    refreshButton.setShortcutKey("F5");
    refreshButton.addActionListener(e -> loadDefects());

    header.setTrailing(refreshButton, addDefectButton);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setCodeColumn(1);
    dataTable.setColumnWidths(140, 140, 240, 110, 150, 280, 110);
    dataTable.setActionColumn(6, RowAction.primary("Xử lý", this::openStatusDialog));
    dataTable.setOnRowDoubleClick(this::openStatusDialog);

    tableState = new StatefulView(dataTable, "alert-circle", "Chưa có sản phẩm lỗi", "Bấm 'Ghi nhận máy lỗi' để tạo mới hồ sơ.");
    tableState.showEmpty("search", "Chưa tra cứu", "Nhập từ khóa rồi nhấn Tìm kiếm; để trống để xem tất cả.");
    tableState.setOnRetry(this::loadDefects);

    Card tableCard = Card.standard();
    tableCard.add(buildFilterRow(), "growx, wrap");
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
  }

  private JPanel buildFilterRow() {
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[][]16[grow]12[]push[]", "[bottom]"));
    filterRow.setOpaque(false);

    statusFilter.setOnSelectionChange(index -> {
      currentStatusFilter = switch (index) {
        case 1 -> TrangThaiXuLyLoi.CHO_XU_LY;
        case 2 -> TrangThaiXuLyLoi.DA_GUI_BAO_HANH;
        case 3 -> TrangThaiXuLyLoi.DA_TRA_NHA_CUNG_CAP;
        case 4 -> TrangThaiXuLyLoi.THANH_LY;
        default -> null;
      };
      loadDefects();
    });
    filterRow.add(statusFilter);

    searchField.setShortcut("F2");
    searchField.getTextField().addActionListener(e -> applySearchFilter());
    filterRow.add(searchField, "growx");

    searchButton.addActionListener(e -> applySearchFilter());
    filterRow.add(searchButton);
    filterRow.add(countPill, "align right, aligny center");
    return filterRow;
  }

  @Override
  public PageId id() {
    return PageId.DEFECTIVE_PRODUCTS;
  }

  @Override
  public String title() {
    return "Quản lý sản phẩm lỗi";
  }

  @Override
  public String section() {
    return "Quản lý";
  }

  @Override
  public JComponent view() {
    return view;
  }

  @Override
  public boolean scrollable() {
    return false;
  }

  @Override
  public void onShow() {
    // Không tự tải: bảng chỉ nạp khi người dùng tìm kiếm.
  }

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "themSanPhamLoi");
    actionMap.put("themSanPhamLoi", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openAddDialog();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoiSanPhamLoi");
    actionMap.put("lamMoiSanPhamLoi", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadDefects();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "timKiemSanPhamLoi");
    actionMap.put("timKiemSanPhamLoi", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        searchField.getTextField().requestFocusInWindow();
      }
    });
  }

  /**
   * Tải danh sách hồ sơ lỗi theo trạng thái lọc qua tiến trình nền.
   */
  public void loadDefects() {
    if (!daTimKiem) {
      return;
    }
    tableState.showLoading();
    refreshButton.setEnabled(false);

    new SwingWorker<List<SanPhamLoi>, Void>() {
      @Override
      protected List<SanPhamLoi> doInBackground() {
        return controller.layDanhSach(currentStatusFilter);
      }

      @Override
      protected void done() {
        refreshButton.setEnabled(true);
        try {
          List<SanPhamLoi> list = get();
          cachedList.clear();
          if (list != null) {
            cachedList.addAll(list);
          }
          renderFilteredList();
        } catch (Exception e) {
          tableState.showError("Không thể tải danh sách sản phẩm lỗi: " + e.getMessage());
        }
      }
    }.execute();
  }

  private void applySearchFilter() {
    this.currentSearchKeyword = searchField.getText().trim().toLowerCase();
    daTimKiem = true;
    loadDefects();
  }

  private void renderFilteredList() {
    tableModel.setRowCount(0);

    List<SanPhamLoi> filtered = cachedList.stream()
        .filter(spl -> {
          if (currentSearchKeyword.isEmpty()) {
            return true;
          }
          String ma = spl.maLoi().toLowerCase();
          String serial = spl.sanPham().soSerial().toLowerCase();
          String dongSp = (spl.sanPham().dongSanPham() != null)
              ? spl.sanPham().dongSanPham().tenDongSP().toLowerCase()
              : "";
          return ma.contains(currentSearchKeyword)
              || serial.contains(currentSearchKeyword)
              || dongSp.contains(currentSearchKeyword);
        })
        .toList();

    if (filtered.isEmpty()) {
      countPill.setText("0 máy lỗi");
      if (currentSearchKeyword.isEmpty()) {
        tableState.showEmpty();
      } else {
        tableState.showEmpty("search", "Không tìm thấy hồ sơ lỗi", "Thử tìm kiếm với số serial hoặc mã lỗi khác.");
      }
      return;
    }

    countPill.setText(filtered.size() + " máy lỗi");
    for (SanPhamLoi spl : filtered) {
      String tenDongSp = (spl.sanPham().dongSanPham() != null)
          ? spl.sanPham().dongSanPham().tenDongSP()
          : "—";

      tableModel.addRow(new Object[] {
          spl.maLoi(),
          spl.sanPham().soSerial(),
          tenDongSp,
          spl.ngayGhiNhan() != null ? spl.ngayGhiNhan().toString() : "—",
          dinhDangTrangThai(spl.tinhTrangXuLy()),
          spl.moTaLoi(),
          ""
      });
    }
    tableState.showContent();
  }

  private String dinhDangTrangThai(TrangThaiXuLyLoi tt) {
    if (tt == null) {
      return "—";
    }
    return switch (tt) {
      case CHO_XU_LY -> "Chờ xử lý";
      case DA_GUI_BAO_HANH -> "Đã gửi bảo hành";
      case DA_TRA_NHA_CUNG_CAP -> "Đã trả nhà cung cấp";
      case THANH_LY -> "Thanh lý";
    };
  }

  private void openAddDialog() {
    Frame parentFrame = ConfirmDialog.frameOf(view);
    AddDefectiveProductDialog dialog = new AddDefectiveProductDialog(parentFrame);

    dialog.setOnSave(formData -> {
      dialog.setBusy(true);
      new SwingWorker<SanPhamLoiController.KetQua<SanPhamLoi>, Void>() {
        @Override
        protected SanPhamLoiController.KetQua<SanPhamLoi> doInBackground() {
          String maPhieuTra = formData.laPhieuTra() ? formData.maChungTuLienKet() : null;
          String maPhieuDoi = !formData.laPhieuTra() ? formData.maChungTuLienKet() : null;

          return controller.ghiNhanLoi(
              formData.soSerial(),
              formData.moTaLoi(),
              formData.ngayPhatHien(),
              formData.ghiChuXuLy(),
              maPhieuTra,
              maPhieuDoi);
        }

        @Override
        protected void done() {
          dialog.setBusy(false);
          try {
            SanPhamLoiController.KetQua<SanPhamLoi> result = get();
            if (result.thanhCong()) {
              dialog.dispose();
              Toast.show(view, result.thongBao(), Toast.Kind.SUCCESS);
              loadDefects();
            } else {
              dialog.setErrorMessage(result.thongBao());
            }
          } catch (Exception e) {
            dialog.setErrorMessage("Lỗi xử lý: " + e.getMessage());
          }
        }
      }.execute();
    });

    dialog.setVisible(true);
  }

  private void openStatusDialog(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= tableModel.getRowCount()) {
      return;
    }
    String maLoi = (String) tableModel.getValueAt(rowIndex, 0);
    SanPhamLoi spl = cachedList.stream()
        .filter(s -> s.maLoi().equals(maLoi))
        .findFirst()
        .orElse(null);

    if (spl == null) {
      return;
    }

    Frame parentFrame = ConfirmDialog.frameOf(view);
    UpdateDefectiveStatusDialog dialog = new UpdateDefectiveStatusDialog(parentFrame, spl);

    dialog.setOnUpdate((trangThaiMoi, ghiChu) -> {
      dialog.setBusy(true);
      new SwingWorker<SanPhamLoiController.KetQua<Void>, Void>() {
        @Override
        protected SanPhamLoiController.KetQua<Void> doInBackground() {
          return controller.chuyenTrangThai(spl.maLoi(), trangThaiMoi, ghiChu);
        }

        @Override
        protected void done() {
          dialog.setBusy(false);
          try {
            SanPhamLoiController.KetQua<Void> result = get();
            if (result.thanhCong()) {
              dialog.dispose();
              Toast.show(view, result.thongBao(), Toast.Kind.SUCCESS);
              loadDefects();
            } else {
              Toast.show(view, result.thongBao(), Toast.Kind.ERROR);
            }
          } catch (Exception e) {
            Toast.show(view, "Lỗi khi cập nhật trạng thái: " + e.getMessage(), Toast.Kind.ERROR);
          }
        }
      }.execute();
    });

    dialog.setVisible(true);
  }
}
