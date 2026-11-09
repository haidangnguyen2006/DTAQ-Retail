package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.util.ExcelImporter;
import java.awt.Color;
import java.awt.Frame;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại xem trước kết quả nhập Excel dùng chung (UC nhập dữ liệu hàng loạt).
 *
 * <p>Hộp thoại chỉ hiển thị {@link ExcelImporter.ImportResult} do bên gọi cung cấp: phần tổng hợp
 * số dòng, số dòng hợp lệ và số lỗi; bảng xem trước tối đa {@code maxRows} dòng; bảng lỗi tối đa
 * 200 dòng. Hộp thoại không đọc tệp, không gọi cơ sở dữ liệu và không phụ thuộc Apache POI.
 *
 * <p>Việc nhập thật do bên gọi thực hiện qua {@link #setOnConfirm(Consumer)}; hộp thoại không tự
 * đóng sau khi xác nhận để bên gọi có thể hiển thị tiến trình hoặc lỗi lưu dữ liệu.
 */
public class ImportPreviewDialog extends ModalDialog {

  /** Số dòng xem trước mặc định khi tham số {@code maxRows} không dương. */
  private static final int SO_DONG_XEM_TRUOC_MAC_DINH = 50;

  /** Số lỗi tối đa hiển thị trong bảng lỗi. */
  private static final int SO_LOI_HIEN_THI_TOI_DA = 200;

  private static final String[] COT_LOI = {"Dòng", "Cột", "Mã lỗi", "Thông báo"};
  private static final int DO_RONG_COT_XEM_TRUOC = 150;

  private final ExcelImporter.ImportResult ketQua;
  private final boolean coTheNhap;
  private final boolean coLoi;

  private final DtaqButton nutNhap;
  private final DtaqButton nutChonTepKhac;
  private final DtaqButton nutXuatLoi;
  private final DtaqButton nutHuy;

  private Consumer<ExcelImporter.ImportResult> onConfirm;
  private Runnable onChooseAnother;
  private Consumer<ExcelImporter.ImportResult> onExportErrors;
  private boolean busy;

  /**
   * Khởi tạo hộp thoại xem trước.
   *
   * <p>Phải gọi trên EDT.
   *
   * @param parent cửa sổ cha; có thể {@code null}
   * @param title tiêu đề hộp thoại; rỗng thì dùng tiêu đề mặc định
   * @param result kết quả nhập cần hiển thị; không được {@code null}
   * @param previewHeaders tiêu đề các cột xem trước theo đúng thứ tự; rỗng thì lấy theo khóa của
   *     dòng hợp lệ đầu tiên
   * @param maxRows số dòng xem trước tối đa; không dương thì dùng mặc định 50
   */
  public ImportPreviewDialog(
      Frame parent,
      String title,
      ExcelImporter.ImportResult result,
      List<String> previewHeaders,
      int maxRows) {
    super(
        parent,
        title == null || title.isBlank() ? "Xem trước dữ liệu nhập" : title,
        "Kiểm tra dữ liệu trước khi nhập vào hệ thống.",
        900);
    this.ketQua = Objects.requireNonNull(result, "result");
    this.coLoi = !ketQua.loi().isEmpty();
    this.coTheNhap = ketQua.hopLe() && !ketQua.dong().isEmpty();

    nutChonTepKhac = DtaqButton.secondary("Chọn tệp khác", "folder");
    nutXuatLoi = DtaqButton.secondary("Xuất danh sách lỗi", "download");
    nutHuy = DtaqButton.secondary("Hủy");
    nutNhap = DtaqButton.primary("Nhập " + ketQua.dong().size() + " dòng", "check");

    taoThanhPhan(previewHeaders, maxRows);
    noiSuKien();

    nutNhap.setEnabled(coTheNhap);
    nutXuatLoi.setEnabled(coLoi);
    setPrimaryAction(nutNhap);
    setActions(nutChonTepKhac, nutXuatLoi, nutHuy, nutNhap);
  }

  private void taoThanhPhan(List<String> previewHeaders, int maxRows) {
    getBody().add(taoBangTongHop(), "growx, wrap, gapbottom 12");
    getBody().add(taoCallout(), "growx, wrap, gapbottom 12");

    List<String> tieuDeXemTruoc = chuanHoaTieuDe(previewHeaders);
    int gioiHanDong = maxRows > 0 ? maxRows : SO_DONG_XEM_TRUOC_MAC_DINH;
    int soDongHienThi = Math.min(gioiHanDong, ketQua.dong().size());
    String ghiChuXemTruoc =
        ketQua.dong().size() > soDongHienThi
            ? "Hiển thị " + soDongHienThi + "/" + ketQua.dong().size() + " dòng đầu tiên"
            : "Toàn bộ " + ketQua.dong().size() + " dòng hợp lệ";

    Card theXemTruoc = Card.section("Xem trước dữ liệu", ghiChuXemTruoc, null);
    if (tieuDeXemTruoc.isEmpty()) {
      theXemTruoc.add(taoNhanTrong("Không có cột để xem trước"), "growx, wrap");
    } else {
      theXemTruoc.add(taoBangXemTruoc(tieuDeXemTruoc, soDongHienThi), "growx, wrap");
    }
    getBody().add(theXemTruoc, "growx, wrap, gapbottom 12");

    if (coLoi) {
      int soLoiHienThi = Math.min(SO_LOI_HIEN_THI_TOI_DA, ketQua.loi().size());
      String ghiChuLoi =
          ketQua.loi().size() > soLoiHienThi
              ? "Hiển thị " + soLoiHienThi + "/" + ketQua.loi().size() + " lỗi đầu tiên"
              : "Tổng cộng " + ketQua.loi().size() + " lỗi, cần sửa tệp trước khi nhập";
      Card theLoi = Card.section("Lỗi phát hiện", ghiChuLoi, null);
      theLoi.add(taoBangLoi(soLoiHienThi), "growx, wrap");
      getBody().add(theLoi, "growx, wrap");
    }
  }

  private List<String> chuanHoaTieuDe(List<String> previewHeaders) {
    List<String> tieuDe =
        previewHeaders == null
            ? List.of()
            : previewHeaders.stream().filter(Objects::nonNull).map(String::trim).toList();
    if (!tieuDe.isEmpty()) {
      return tieuDe;
    }
    if (!ketQua.dong().isEmpty()) {
      return List.copyOf(ketQua.dong().get(0).giaTri().keySet());
    }
    return List.of();
  }

  private JPanel taoBangTongHop() {
    Card the = Card.compact();
    JPanel hang = new JPanel(new MigLayout("fillx, insets 0", "[grow][grow][grow]", "[]"));
    hang.setOpaque(false);
    hang.add(
        taoChiSo("Tổng số dòng", String.valueOf(ketQua.tongSoDong()), DtaqColors.TEXT), "growx");
    hang.add(
        taoChiSo(
            "Dòng hợp lệ", String.valueOf(ketQua.dong().size()), DtaqColors.SUCCESS),
        "growx");
    hang.add(
        taoChiSo(
            "Lỗi",
            String.valueOf(ketQua.loi().size()),
            coLoi ? DtaqColors.DANGER : DtaqColors.TEXT_MUTED),
        "growx");
    the.add(hang, "growx, wrap");
    return the;
  }

  private JPanel taoChiSo(String nhan, String giaTri, Color mau) {
    JPanel khoi = new JPanel(new MigLayout("insets 4 0", "[]", "[]2[]"));
    khoi.setOpaque(false);
    JLabel giaTriLabel = new JLabel(giaTri);
    giaTriLabel.setFont(DtaqFonts.KPI_VALUE);
    giaTriLabel.setForeground(mau);
    JLabel nhanLabel = new JLabel(nhan);
    nhanLabel.setFont(DtaqFonts.CAPTION);
    nhanLabel.setForeground(DtaqColors.TEXT_MUTED);
    khoi.add(giaTriLabel, "wrap");
    khoi.add(nhanLabel);
    return khoi;
  }

  private InfoCallout taoCallout() {
    if (coLoi) {
      return new InfoCallout(
          InfoCallout.Kind.DANGER,
          "Tệp có "
              + ketQua.loi().size()
              + " lỗi. Vui lòng sửa tệp và chọn lại; chỉ nhập được khi không còn lỗi.");
    }
    if (ketQua.dong().isEmpty()) {
      return new InfoCallout(
          InfoCallout.Kind.WARNING, "Tệp không có dòng dữ liệu nào để nhập.");
    }
    return new InfoCallout(
        InfoCallout.Kind.INFO,
        "Dữ liệu hợp lệ. Kiểm tra lại nội dung xem trước trước khi bấm nhập.");
  }

  private DataTable taoBangXemTruoc(List<String> tieuDe, int soDongHienThi) {
    DefaultTableModel model = DataTable.readOnlyModel(tieuDe.toArray(new String[0]));
    for (int i = 0; i < soDongHienThi; i++) {
      ExcelImporter.ImportRow dong = ketQua.dong().get(i);
      Object[] giaTri = new Object[tieuDe.size()];
      for (int cot = 0; cot < tieuDe.size(); cot++) {
        giaTri[cot] = hienThiGiaTri(dong.giaTri().get(tieuDe.get(cot)));
      }
      model.addRow(giaTri);
    }

    DataTable bang = new DataTable(model);
    int[] doRong = new int[tieuDe.size()];
    Arrays.fill(doRong, DO_RONG_COT_XEM_TRUOC);
    bang.setColumnWidths(doRong);
    bang.setVisibleRows(3, 8);
    return bang;
  }

  private DataTable taoBangLoi(int soLoiHienThi) {
    DefaultTableModel model = DataTable.readOnlyModel(COT_LOI);
    for (int i = 0; i < soLoiHienThi; i++) {
      ExcelImporter.ImportError loi = ketQua.loi().get(i);
      model.addRow(
          new Object[] {
            loi.soDongExcel() + 1,
            loi.cot() == null || loi.cot().isBlank() ? "—" : loi.cot(),
            loi.maLoi(),
            loi.thongBao()
          });
    }

    DataTable bang = new DataTable(model);
    bang.setNumberColumn(0);
    bang.setColumnWidths(70, 130, 130, 480);
    bang.setVisibleRows(3, 6);
    return bang;
  }

  private JLabel taoNhanTrong(String text) {
    JLabel nhan = new JLabel(text);
    nhan.setFont(DtaqFonts.BODY_13);
    nhan.setForeground(DtaqColors.TEXT_MUTED);
    return nhan;
  }

  private static String hienThiGiaTri(Object giaTri) {
    if (giaTri == null) {
      return "";
    }
    if (giaTri instanceof LocalDate ngay) {
      return DateFormats.formatDate(ngay);
    }
    if (giaTri instanceof Boolean dung) {
      return dung ? "Có" : "Không";
    }
    if (giaTri instanceof BigDecimal so) {
      return so.stripTrailingZeros().toPlainString();
    }
    return String.valueOf(giaTri);
  }

  private void noiSuKien() {
    nutChonTepKhac.addActionListener(
        e -> {
          if (busy) {
            return;
          }
          if (onChooseAnother != null) {
            onChooseAnother.run();
          }
          dispose();
        });
    nutXuatLoi.addActionListener(
        e -> {
          if (busy || onExportErrors == null) {
            return;
          }
          onExportErrors.accept(ketQua);
        });
    nutHuy.addActionListener(e -> requestClose());
    nutNhap.addActionListener(
        e -> {
          if (busy || !coTheNhap || onConfirm == null) {
            return;
          }
          onConfirm.accept(ketQua);
        });
  }

  /** Đặt xử lý khi người dùng xác nhận nhập; hộp thoại không tự đóng. */
  public void setOnConfirm(Consumer<ExcelImporter.ImportResult> listener) {
    this.onConfirm = listener;
  }

  /** Đặt xử lý khi người dùng muốn chọn tệp khác; hộp thoại đóng sau khi gọi xử lý. */
  public void setOnChooseAnother(Runnable listener) {
    this.onChooseAnother = listener;
  }

  /** Đặt xử lý khi người dùng yêu cầu xuất danh sách lỗi ra tệp. */
  public void setOnExportErrors(Consumer<ExcelImporter.ImportResult> listener) {
    this.onExportErrors = listener;
  }

  /**
   * Bật/tắt trạng thái đang xử lý. Khi bận, mọi nút bị vô hiệu hóa và không thể đóng bằng Esc hay
   * nút ×; khi xong, trạng thái nút được khôi phục theo tính hợp lệ của kết quả nhập.
   */
  public void setBusy(boolean busy) {
    this.busy = busy;
    nutChonTepKhac.setEnabled(!busy);
    nutXuatLoi.setEnabled(!busy && coLoi);
    nutHuy.setEnabled(!busy);
    nutNhap.setBusy(busy);
    if (!busy) {
      nutNhap.setEnabled(coTheNhap);
    }
  }

  @Override
  protected void requestClose() {
    if (busy) {
      return;
    }
    dispose();
  }
}
