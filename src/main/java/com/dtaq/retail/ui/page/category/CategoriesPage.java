package com.dtaq.retail.ui.page.category;

import com.dtaq.retail.controller.DanhMucController;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.ConfirmDialog;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.PageHeader;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình quản lý danh mục sản phẩm.
 */
public class CategoriesPage implements Page {

  private final DanhMucController controller;
  private final JPanel view;

  private final FormField searchField = FormField.search("Tìm danh mục", "Nhập mã hoặc tên danh mục");
  private final DtaqButton searchButton = DtaqButton.secondary("Tìm kiếm");
  private final DtaqButton refreshButton = DtaqButton.secondary("Làm mới", "refresh-cw");
  private final DtaqButton addCategoryButton = DtaqButton.primary("Thêm danh mục", "plus");
  private final StatusPill countPill = StatusPill.neutral("0 danh mục");

  private final DefaultTableModel tableModel = DataTable.readOnlyModel(
      new String[] {"Mã danh mục", "Tên danh mục", "Mô tả", "Thao tác"}, 3);
  private final DataTable dataTable = new DataTable(tableModel, 50);
  private final StatefulView tableState;

  private final List<DanhMucSanPham> cachedList = new ArrayList<>();
  private String currentFilter = "";

  /**
   * Khởi tạo trang quản lý danh mục sản phẩm.
   *
   * @param controller bộ điều khiển xử lý nghiệp vụ danh mục; không được {@code null}
   */
  public CategoriesPage(DanhMucController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");

    this.view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING + ", gapy 16",
        "[grow]",
        "[][grow]"));

    PageHeader header = new PageHeader(
        "Quản lý danh mục sản phẩm",
        "Tạo danh mục và phân nhóm các dòng sản phẩm trong hệ thống.");

    addCategoryButton.setShortcutKey("F4");
    addCategoryButton.addActionListener(e -> openAddDialog());

    refreshButton.setShortcutKey("F5");
    refreshButton.addActionListener(e -> loadCategories());

    header.setTrailing(refreshButton, addCategoryButton);
    view.add(header, "growx, wrap");

    dataTable.setCodeColumn(0);
    dataTable.setColumnWidths(160, 260, 420, 160);
    dataTable.setActionColumn(3,
        RowAction.secondary("Sửa", this::openEditDialog),
        RowAction.danger("Xóa", this::confirmDelete));
    dataTable.setOnRowDoubleClick(this::openEditDialog);

    tableState = new StatefulView(dataTable, "folder", "Chưa có danh mục", "Bấm 'Thêm danh mục' để tạo mới.");
    tableState.setOnRetry(this::loadCategories);

    Card tableCard = Card.standard();
    tableCard.add(buildFilterRow(), "growx, wrap");
    tableCard.setBody(tableState);
    view.add(tableCard, "grow, push");
  }

  private JPanel buildFilterRow() {
    JPanel filterRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]push[]", "[bottom]"));
    filterRow.setOpaque(false);

    searchField.setShortcut("F2");
    searchField.getTextField().addActionListener(e -> applyFilter());
    filterRow.add(searchField, "growx");

    searchButton.addActionListener(e -> applyFilter());
    filterRow.add(searchButton);
    filterRow.add(countPill, "align right, aligny center");
    return filterRow;
  }

  @Override
  public PageId id() {
    return PageId.CATEGORIES;
  }

  @Override
  public String title() {
    return "Quản lý danh mục sản phẩm";
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
    loadCategories();
  }

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "themDanhMuc");
    actionMap.put("themDanhMuc", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openAddDialog();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoiDanhMuc");
    actionMap.put("lamMoiDanhMuc", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadCategories();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "timKiemDanhMuc");
    actionMap.put("timKiemDanhMuc", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        searchField.getTextField().requestFocusInWindow();
      }
    });
  }

  /**
   * Tải danh sách danh mục từ cơ sở dữ liệu qua tiến trình nền.
   */
  public void loadCategories() {
    tableState.showLoading();
    refreshButton.setEnabled(false);

    new SwingWorker<List<DanhMucSanPham>, Void>() {
      @Override
      protected List<DanhMucSanPham> doInBackground() {
        return controller.layDanhSach();
      }

      @Override
      protected void done() {
        refreshButton.setEnabled(true);
        try {
          List<DanhMucSanPham> list = get();
          cachedList.clear();
          if (list != null) {
            cachedList.addAll(list);
          }
          renderFilteredList();
        } catch (Exception e) {
          tableState.showError("Không thể tải danh mục: " + e.getMessage());
        }
      }
    }.execute();
  }

  private void applyFilter() {
    this.currentFilter = searchField.getText().trim().toLowerCase();
    renderFilteredList();
  }

  private void renderFilteredList() {
    tableModel.setRowCount(0);
    List<DanhMucSanPham> filtered = cachedList.stream()
        .filter(dm -> currentFilter.isEmpty()
            || dm.maDanhMuc().toLowerCase().contains(currentFilter)
            || dm.tenDanhMuc().toLowerCase().contains(currentFilter))
        .toList();

    if (filtered.isEmpty()) {
      countPill.setText("0 danh mục");
      if (currentFilter.isEmpty()) {
        tableState.showEmpty();
      } else {
        tableState.showEmpty("search", "Không tìm thấy danh mục", "Thử tìm kiếm với từ khóa khác.");
      }
      return;
    }

    countPill.setText(filtered.size() + " danh mục");
    for (DanhMucSanPham dm : filtered) {
      tableModel.addRow(new Object[] {
          dm.maDanhMuc(),
          dm.tenDanhMuc(),
          dm.moTa() != null ? dm.moTa() : "",
          ""
      });
    }
    tableState.showContent();
  }

  private void openAddDialog() {
    Frame parentFrame = ConfirmDialog.frameOf(view);
    CategoryDialog dialog = new CategoryDialog(parentFrame, null);

    dialog.setOnSave(formData -> {
      dialog.setBusy(true);
      new SwingWorker<DanhMucController.KetQua<DanhMucSanPham>, Void>() {
        @Override
        protected DanhMucController.KetQua<DanhMucSanPham> doInBackground() {
          return controller.themMoi(formData.tenDanhMuc(), formData.moTa());
        }

        @Override
        protected void done() {
          dialog.setBusy(false);
          try {
            DanhMucController.KetQua<DanhMucSanPham> result = get();
            if (result.thanhCong()) {
              dialog.dispose();
              Toast.show(view, result.thongBao(), Toast.Kind.SUCCESS);
              loadCategories();
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

  private void openEditDialog(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= tableModel.getRowCount()) {
      return;
    }
    String ma = (String) tableModel.getValueAt(rowIndex, 0);
    DanhMucSanPham dm = cachedList.stream()
        .filter(d -> d.maDanhMuc().equals(ma))
        .findFirst()
        .orElse(null);

    if (dm == null) {
      return;
    }

    Frame parentFrame = ConfirmDialog.frameOf(view);
    CategoryDialog dialog = new CategoryDialog(parentFrame, dm);

    dialog.setOnSave(formData -> {
      dialog.setBusy(true);
      new SwingWorker<DanhMucController.KetQua<DanhMucSanPham>, Void>() {
        @Override
        protected DanhMucController.KetQua<DanhMucSanPham> doInBackground() {
          return controller.capNhat(formData.maDanhMuc(), formData.tenDanhMuc(), formData.moTa());
        }

        @Override
        protected void done() {
          dialog.setBusy(false);
          try {
            DanhMucController.KetQua<DanhMucSanPham> result = get();
            if (result.thanhCong()) {
              dialog.dispose();
              Toast.show(view, result.thongBao(), Toast.Kind.SUCCESS);
              loadCategories();
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

  private void confirmDelete(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= tableModel.getRowCount()) {
      return;
    }
    String ma = (String) tableModel.getValueAt(rowIndex, 0);
    String ten = (String) tableModel.getValueAt(rowIndex, 1);

    ConfirmDialog.confirm(
        view,
        "Xác nhận xóa danh mục",
        "Bạn có chắc chắn muốn xóa danh mục '" + ten + "' (" + ma + ") không?\n"
            + "Lưu ý: Không thể xóa danh mục nếu đang có dòng sản phẩm tham chiếu.",
        "Xóa danh mục",
        () -> thucHienXoa(ma));
  }

  private void thucHienXoa(String maDanhMuc) {
    new SwingWorker<DanhMucController.KetQua<Void>, Void>() {
      @Override
      protected DanhMucController.KetQua<Void> doInBackground() {
        return controller.xoa(maDanhMuc);
      }

      @Override
      protected void done() {
        try {
          DanhMucController.KetQua<Void> result = get();
          if (result.thanhCong()) {
            Toast.show(view, result.thongBao(), Toast.Kind.SUCCESS);
            loadCategories();
          } else {
            Toast.show(view, result.thongBao(), Toast.Kind.ERROR);
          }
        } catch (Exception e) {
          Toast.show(view, "Lỗi khi xóa: " + e.getMessage(), Toast.Kind.ERROR);
        }
      }
    }.execute();
  }
}
