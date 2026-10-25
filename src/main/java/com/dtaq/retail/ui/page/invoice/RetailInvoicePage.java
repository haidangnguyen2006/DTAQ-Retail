package com.dtaq.retail.ui.page.invoice;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.ShortcutBar;
import com.dtaq.retail.ui.component.StatefulView;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.component.Tile;
import com.dtaq.retail.ui.component.Toast;
import com.dtaq.retail.ui.component.ViewportPanel;
import com.dtaq.retail.ui.component.table.RowAction;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Lập hóa đơn bán lẻ (UC003, RetailInvoicePage).
 *
 * <p>Trang chỉ hiển thị: tổng tiền, thuế, số hóa đơn và kết quả thanh toán do controller cung cấp
 * qua các hàm {@code set…}/{@code show…}; mọi thao tác của người dùng được chuyển ra qua các
 * callback {@code setOn…}. Chưa gắn callback thì điều khiển tương ứng bị tắt.
 */
public class RetailInvoicePage implements Page {

  public record CustomerInfo(String maKH, String hoTen, String sdt, String diaChi) {}

  public record ProductResult(String maSP, String tenSP, int tonKho, String thueSuat, long donGia) {}

  public record CartItem(String maSP, String tenSP, int soLuong, long donGiaTruocThue, String thueSuat, long thanhTienSauThue) {}

  private static final String NOT_CONNECTED_TIP = "Chức năng đang được kết nối dữ liệu";
  private static final String DRAFT_PILL_TEXT = "Hóa đơn mới · Bản nháp";
  private static final String PAY_TEXT = "Thanh toán bằng tiền mặt";
  private static final String PAID_TEXT = "Đã thu đủ tiền mặt";
  private static final int RIGHT_COLUMN_WIDTH = 380;
  private static final int WRAP_WIDTH = 300;
  private static final int RESULT_LIST_MAX_HEIGHT = 300;

  private final NhanVien nhanVien;
  private final JPanel view;
  private final PageHeader header;
  private final StatusPill invoiceStatusPill = StatusPill.neutral(DRAFT_PILL_TEXT);

  private final FormField phoneLookupField =
      FormField.text("Tìm khách hàng theo số điện thoại", "Nhập số điện thoại khách hàng", "phone");
  private final DtaqButton lookupCustomerBtn = DtaqButton.secondary("Tìm khách hàng");
  private final JPanel customerLookupStatusPanel = new JPanel(new MigLayout("insets 0, gapx 8", "[][]", "[center]"));

  private final FormField productSearchField = FormField.search("Tìm sản phẩm", "Nhập mã hoặc tên sản phẩm");
  private final JPanel productResultsPanel = new JPanel(new MigLayout("fillx, insets 0, hidemode 3", "[grow]", "[]8[]"));
  private final JPanel productRowsPanel = new JPanel(new MigLayout("fillx, insets 0, gapy 6", "[grow]", "[]"));

  private final JLabel cartSubtitleLabel = new JLabel("0 sản phẩm · 0 đơn vị");
  private final DefaultTableModel cartTableModel =
      DataTable.readOnlyModel(new String[] {"Mã", "Tên sản phẩm", "SL", "Đơn giá", "Thuế", "Thành tiền", ""}, 6);
  private final DataTable cartTable = new DataTable(cartTableModel, 52);
  private final StatefulView cartState;

  private final JPanel customerInfoContent = new JPanel(new MigLayout("fillx, insets 0, gapy 4", "[grow]", "[]"));

  private final JLabel subtotalLabel = new JLabel("0 VNĐ");
  private final JLabel taxLabel = new JLabel("0 VNĐ");
  private final JLabel totalAmountLabel = new JLabel("0");
  private final FormField cashField = FormField.money("Tiền khách đưa (VNĐ)", "0");
  private final JLabel changeLabel = new JLabel("0 VNĐ");
  private final DtaqButton payButton = DtaqButton.primary(PAY_TEXT);
  private final JLabel conditionLabel = new JLabel();

  private CustomerInfo currentCustomer;
  private String customerLookupMessage;
  private final List<CartItem> cartItems = new ArrayList<>();
  private InvoiceTotals totals = InvoiceTotals.EMPTY;
  private final List<ProductResult> searchResults = new ArrayList<>();
  private boolean resultsVisible;
  private int selectedResultIndex = -1;
  private boolean processing;
  private boolean paid;
  private String paymentError;
  private InvoiceViewState state = InvoiceViewState.NO_CUSTOMER;

  private Consumer<String> onCustomerLookup;
  private Consumer<String> onProductSearch;
  private Consumer<ProductResult> onAddToCart;
  private Consumer<CartItem> onRemoveFromCart;
  private LongConsumer onProcessPayment;
  private Runnable onNewInvoice;

  public RetailInvoicePage(NhanVien nhanVien) {
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    view = new JPanel(new MigLayout(
        "fill, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"));
    view.setBackground(DtaqColors.BG_APP);

    header = new PageHeader("Lập hóa đơn bán lẻ", headerMeta());
    header.setTrailing(invoiceStatusPill);
    view.add(header, "growx, wrap");

    cartState = new StatefulView(
        buildCartContent(), "shopping-cart", "Chưa có sản phẩm trong giỏ",
        "Nhấn F3 để tìm sản phẩm và bắt đầu lập hóa đơn.");

    JPanel body = new JPanel(new MigLayout(
        "fill, insets 0", "[grow,fill]20[" + RIGHT_COLUMN_WIDTH + "!,fill]", "[grow,fill]"));
    body.setOpaque(false);
    body.add(scroller(buildLeftColumn()));
    body.add(scroller(buildRightColumn()));
    view.add(body, "grow, push");

    refreshView();
  }

  // ---------------------------------------------------------------- build

  private static JScrollPane scroller(JComponent content) {
    JScrollPane scroll = new JScrollPane(new ViewportPanel(content));
    scroll.setBorder(null);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    return scroll;
  }

  private JPanel buildLeftColumn() {
    JPanel leftCol = new JPanel(new MigLayout("fillx, insets 0 0 0 6", "[grow]", "[]16[]16[]14[]"));
    leftCol.setOpaque(false);
    leftCol.add(buildCustomerLookupCard(), "growx, wrap");
    leftCol.add(buildProductSearchCard(), "growx, wrap");
    leftCol.add(buildCartCard(), "growx, wrap");
    leftCol.add(ShortcutBar.invoiceDefaults(), "growx");
    return leftCol;
  }

  private Card buildCustomerLookupCard() {
    Card card = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 12);
    JPanel searchRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[bottom]"));
    searchRow.setOpaque(false);

    phoneLookupField.setShortcut("F2");
    phoneLookupField.getTextField().addActionListener(e -> triggerCustomerLookup());
    searchRow.add(phoneLookupField, "growx");

    lookupCustomerBtn.addActionListener(e -> triggerCustomerLookup());
    searchRow.add(lookupCustomerBtn);
    card.add(searchRow, "growx, wrap");

    customerLookupStatusPanel.setOpaque(false);
    card.add(customerLookupStatusPanel, "growx");
    return card;
  }

  private Card buildProductSearchCard() {
    Card card = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 0);
    productSearchField.setShortcut("F3 · Enter để thêm");
    JTextField input = productSearchField.getTextField();
    input.addActionListener(e -> onProductEnter());
    bindKey(input, KeyEvent.VK_DOWN, "dtaq.nextResult", () -> moveSelection(1));
    bindKey(input, KeyEvent.VK_UP, "dtaq.prevResult", () -> moveSelection(-1));
    bindKey(input, KeyEvent.VK_ESCAPE, "dtaq.hideResults", this::hideResults);
    card.add(productSearchField, "growx, wrap");

    productResultsPanel.setOpaque(false);
    productResultsPanel.setVisible(false);
    productRowsPanel.setOpaque(false);
    card.add(productResultsPanel, "growx, gaptop 12");
    return card;
  }

  private Card buildCartCard() {
    Card card = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 14);
    cartSubtitleLabel.setFont(DtaqFonts.CAPTION);
    cartSubtitleLabel.setForeground(DtaqColors.TEXT_MUTED);
    card.add(Card.buildHeader("Giỏ hàng", null, cartSubtitleLabel), "growx, wrap");
    card.setBody(cartState);
    return card;
  }

  private JPanel buildCartContent() {
    cartTable.setCodeColumn(0);
    cartTable.setNumberColumn(2);
    cartTable.setMoneyColumn(3);
    cartTable.setMoneyColumn(5);
    cartTable.setCellPadding(10);
    cartTable.setColumnWidths(80, 210, 44, 100, 60, 104, 76);
    cartTable.setVisibleRows(2, 6);
    cartTable.setActionColumn(6, RowAction.danger("Xóa", this::removeCartRow));

    JPanel content = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]12[]"));
    content.setOpaque(false);
    content.add(cartTable, "growx, wrap");
    content.add(new InfoCallout(
        "Đơn giá và thuế suất được khóa theo sản phẩm. Không chỉnh giá hoặc áp dụng giảm giá tại quầy."), "growx");
    return content;
  }

  private JPanel buildRightColumn() {
    JPanel rightCol = new JPanel(new MigLayout("fillx, insets 0 6 0 0", "[grow]", "[]16[]"));
    rightCol.setOpaque(false);

    Card customerInfoCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 12);
    customerInfoCard.add(Card.buildHeader("Thông tin khách hàng", null, null), "growx, wrap");
    customerInfoContent.setOpaque(false);
    customerInfoCard.add(customerInfoContent, "growx");
    rightCol.add(customerInfoCard, "growx, wrap");

    rightCol.add(buildPaymentCard(), "growx");
    return rightCol;
  }

  private Card buildPaymentCard() {
    Card card = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 14);
    card.add(Card.buildHeader("Thanh toán", null, null), "growx, wrap");

    JPanel totalsPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]8[]10[]10[]12[]12[]10[]"));
    totalsPanel.setOpaque(false);

    addAmountRow(totalsPanel, "Tổng trước thuế:", subtotalLabel);
    addAmountRow(totalsPanel, "Tổng tiền thuế:", taxLabel);
    totalsPanel.add(new JSeparator(), "growx, span, wrap");

    JLabel totalTitle = new JLabel("Tổng thanh toán:");
    totalTitle.setFont(DtaqFonts.LABEL);
    totalsPanel.add(totalTitle);
    JPanel totalBig = new JPanel(new MigLayout("insets 0, gapx 4", "[][]", "[baseline]"));
    totalBig.setOpaque(false);
    totalAmountLabel.setFont(DtaqFonts.AMOUNT_XL);
    totalAmountLabel.setForeground(DtaqColors.PRIMARY_STRONG);
    totalBig.add(totalAmountLabel);
    JLabel vnd = new JLabel("VNĐ");
    vnd.setFont(DtaqFonts.CAPTION);
    vnd.setForeground(DtaqColors.TEXT_MUTED);
    totalBig.add(vnd);
    totalsPanel.add(totalBig, "align right, wrap");

    cashField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        onCashChanged();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        onCashChanged();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        onCashChanged();
      }
    });
    cashField.getTextField().addActionListener(e -> executePayment());
    totalsPanel.add(cashField, "growx, span, wrap");

    addAmountRow(totalsPanel, "Tiền trả lại khách:", changeLabel);

    payButton.setShortcutKey("F8");
    payButton.setEnabled(false);
    payButton.addActionListener(e -> executePayment());
    totalsPanel.add(payButton, "growx, span, h 44!, wrap");

    conditionLabel.setFont(DtaqFonts.CAPTION);
    totalsPanel.add(conditionLabel, "growx, span");

    card.add(totalsPanel, "growx");
    return card;
  }

  /** Chữ nhiều dòng trong cột phải: đặt độ rộng cố định để HTML tự xuống dòng thay vì bị cắt. */
  private static String wrap(String text) {
    String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    return "<html><table width='" + WRAP_WIDTH + "' cellpadding='0' cellspacing='0'><tr><td>" + safe + "</td></tr></table></html>";
  }

  private static void addAmountRow(JPanel panel, String text, JLabel value) {
    JLabel label = new JLabel(text);
    label.setFont(DtaqFonts.BODY_13);
    label.setForeground(DtaqColors.TEXT_MUTED);
    panel.add(label);
    value.setFont(DtaqFonts.AMOUNT_ROW);
    value.setForeground(DtaqColors.TEXT);
    panel.add(value, "align right, wrap");
  }

  private static void bindKey(JTextField field, int keyCode, String name, Runnable action) {
    field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), name);
    field.getActionMap().put(name, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        action.run();
      }
    });
  }

  // ---------------------------------------------------------------- public API (hiển thị)

  /** Hiển thị khách hàng đã chọn; {@code null} để bỏ chọn. */
  public void setCustomer(CustomerInfo customer) {
    this.currentCustomer = customer;
    this.customerLookupMessage = null;
    refreshView();
  }

  /** Báo không tìm thấy khách hàng theo số điện thoại đã nhập. */
  public void showCustomerNotFound(String phone) {
    this.currentCustomer = null;
    this.customerLookupMessage = "Không tìm thấy khách hàng có số điện thoại " + PhoneFormat.format(phone);
    refreshView();
  }

  /** Hiển thị kết quả tra cứu sản phẩm; {@code null} để ẩn danh sách, danh sách rỗng báo không có kết quả. */
  public void renderSearchResults(List<ProductResult> results) {
    searchResults.clear();
    if (results == null) {
      hideResults();
      return;
    }
    searchResults.addAll(results);
    selectedResultIndex = searchResults.isEmpty() ? -1 : 0;
    resultsVisible = true;
    renderResultRows();
  }

  /** Cập nhật giỏ hàng và tổng tiền (do controller tính). */
  public void setCart(List<CartItem> items, InvoiceTotals invoiceTotals) {
    cartItems.clear();
    if (items != null) {
      cartItems.addAll(items);
    }
    totals = invoiceTotals != null ? invoiceTotals : InvoiceTotals.EMPTY;
    paymentError = null;
    hideResults();
    refreshView();
  }

  /** Đang chờ controller xử lý thanh toán: khóa toàn bộ thao tác. */
  public void showProcessing() {
    processing = true;
    paymentError = null;
    refreshView();
  }

  /** Thanh toán thành công: hiển thị hóa đơn đã lưu; đóng hộp thoại sẽ bắt đầu hóa đơn mới. */
  public void showPaid(InvoiceReceiptDialog.ReceiptData receipt) {
    processing = false;
    paid = true;
    refreshView();
    Frame owner = ownerFrame();
    InvoiceReceiptDialog dialog = new InvoiceReceiptDialog(owner, this::resetForNewInvoice);
    dialog.render(receipt);
    dialog.setVisible(true);
  }

  /** Thanh toán thất bại: mở khóa và hiển thị lý do. */
  public void showPaymentError(String message) {
    processing = false;
    paymentError = (message == null || message.isBlank()) ? "Không thể hoàn tất thanh toán" : message;
    refreshView();
    Toast.error(view, paymentError);
  }

  /** Xóa toàn bộ dữ liệu hóa đơn đang hiển thị để lập hóa đơn mới. */
  public void resetForNewInvoice() {
    currentCustomer = null;
    customerLookupMessage = null;
    cartItems.clear();
    totals = InvoiceTotals.EMPTY;
    searchResults.clear();
    resultsVisible = false;
    selectedResultIndex = -1;
    processing = false;
    paid = false;
    paymentError = null;
    phoneLookupField.setText("");
    productSearchField.setText("");
    cashField.setText("");
    productResultsPanel.setVisible(false);
    header.setSubtitle(headerMeta());
    refreshView();
    if (onNewInvoice != null) {
      onNewInvoice.run();
    }
  }

  public InvoiceViewState getViewState() {
    return state;
  }

  public void setOnCustomerLookup(Consumer<String> listener) {
    this.onCustomerLookup = listener;
    refreshView();
  }

  public void setOnProductSearch(Consumer<String> listener) {
    this.onProductSearch = listener;
    refreshView();
  }

  public void setOnAddToCart(Consumer<ProductResult> listener) {
    this.onAddToCart = listener;
    refreshView();
  }

  public void setOnRemoveFromCart(Consumer<CartItem> listener) {
    this.onRemoveFromCart = listener;
  }

  /** Nhận số tiền khách đưa (VNĐ) khi người dùng bấm thanh toán. */
  public void setOnProcessPayment(LongConsumer listener) {
    this.onProcessPayment = listener;
    refreshView();
  }

  /** Gọi sau khi đóng hóa đơn đã thanh toán, để controller xóa giỏ hàng cũ. */
  public void setOnNewInvoice(Runnable listener) {
    this.onNewInvoice = listener;
  }

  // ---------------------------------------------------------------- hành vi

  private void triggerCustomerLookup() {
    if (onCustomerLookup != null && !isLocked()) {
      onCustomerLookup.accept(phoneLookupField.getText().trim());
    }
  }

  private void triggerProductSearch() {
    if (onProductSearch != null && !isLocked()) {
      onProductSearch.accept(productSearchField.getText().trim());
    }
  }

  private void onProductEnter() {
    if (resultsVisible && selectedResultIndex >= 0 && selectedResultIndex < searchResults.size()) {
      addResultToCart(searchResults.get(selectedResultIndex));
    } else {
      triggerProductSearch();
    }
  }

  private void moveSelection(int delta) {
    if (!resultsVisible || searchResults.isEmpty()) {
      return;
    }
    selectedResultIndex = Math.floorMod(selectedResultIndex + delta, searchResults.size());
    renderResultRows();
  }

  private void hideResults() {
    resultsVisible = false;
    selectedResultIndex = -1;
    productResultsPanel.setVisible(false);
    productResultsPanel.revalidate();
    updateStateFlag();
  }

  private void addResultToCart(ProductResult result) {
    if (onAddToCart != null && !isLocked()) {
      onAddToCart.accept(result);
    }
  }

  private void removeCartRow(int modelRow) {
    if (onRemoveFromCart != null && !isLocked() && modelRow >= 0 && modelRow < cartItems.size()) {
      onRemoveFromCart.accept(cartItems.get(modelRow));
    }
  }

  private void onCashChanged() {
    paymentError = null;
    updatePayment();
  }

  private void executePayment() {
    if (!payButton.isEnabled() || onProcessPayment == null) {
      return;
    }
    long cash = cashField.getMoney();
    showProcessing();
    onProcessPayment.accept(cash);
  }

  private boolean isLocked() {
    return processing || paid;
  }

  private Frame ownerFrame() {
    java.awt.Window window = SwingUtilities.getWindowAncestor(view);
    return window instanceof Frame frame ? frame : null;
  }

  private String headerMeta() {
    return "Nhân viên: " + nhanVien.hoTen() + " · " + DateFormats.formatDateTime(LocalDateTime.now());
  }

  // ---------------------------------------------------------------- render

  private void refreshView() {
    updateStateFlag();
    updateLookupStatus();
    updateCustomerInfo();
    updateCart();
    updatePayment();
    updateControls();
    updateStatusPill();
  }

  private void updateStateFlag() {
    if (paid) {
      state = InvoiceViewState.PAID;
    } else if (processing) {
      state = InvoiceViewState.PROCESSING;
    } else if (currentCustomer == null) {
      state = InvoiceViewState.NO_CUSTOMER;
    } else if (resultsVisible) {
      state = InvoiceViewState.PRODUCT_RESULTS;
    } else if (cartItems.isEmpty()) {
      state = InvoiceViewState.CUSTOMER_SELECTED;
    } else {
      state = InvoiceViewState.READY_TO_PAY;
    }
  }

  private void updateStatusPill() {
    switch (state) {
      case PAID -> {
        invoiceStatusPill.setType(StatusPill.Type.SUCCESS);
        invoiceStatusPill.setText("Đã thanh toán");
      }
      case PROCESSING -> {
        invoiceStatusPill.setType(StatusPill.Type.WARNING);
        invoiceStatusPill.setText("Đang xử lý thanh toán");
      }
      default -> {
        invoiceStatusPill.setType(StatusPill.Type.NEUTRAL);
        invoiceStatusPill.setText(DRAFT_PILL_TEXT);
      }
    }
  }

  private void updateControls() {
    boolean locked = isLocked();
    boolean canLookup = onCustomerLookup != null;
    lookupCustomerBtn.setEnabled(canLookup && !locked);
    lookupCustomerBtn.setToolTipText(canLookup ? null : NOT_CONNECTED_TIP);
    phoneLookupField.getInputComponent().setEnabled(canLookup && !locked);

    boolean canSearch = onProductSearch != null;
    productSearchField.getInputComponent().setEnabled(canSearch && !locked);
    productSearchField.getInputComponent().setToolTipText(canSearch ? null : NOT_CONNECTED_TIP);

    cashField.getInputComponent().setEnabled(!locked && !cartItems.isEmpty());
  }

  private void updateLookupStatus() {
    customerLookupStatusPanel.removeAll();
    if (currentCustomer != null) {
      customerLookupStatusPanel.add(new JLabel(DtaqIcons.icon("circle-check", 17, DtaqColors.PRIMARY)));
      JLabel success = new JLabel(currentCustomer.hoTen() + " · " + currentCustomer.maKH());
      success.setFont(DtaqFonts.BODY_13_MEDIUM);
      success.setForeground(DtaqColors.PRIMARY_STRONG);
      customerLookupStatusPanel.add(success);
    } else if (customerLookupMessage != null) {
      customerLookupStatusPanel.add(new JLabel(DtaqIcons.icon("alert-circle", 17, DtaqColors.DANGER)));
      JLabel error = new JLabel(customerLookupMessage);
      error.setFont(DtaqFonts.CAPTION);
      error.setForeground(DtaqColors.DANGER);
      customerLookupStatusPanel.add(error);
    } else {
      JLabel hint = new JLabel("Tra cứu khách hàng có sẵn hoặc thêm mới tại Quản lý khách hàng.");
      hint.setFont(DtaqFonts.CAPTION);
      hint.setForeground(DtaqColors.TEXT_MUTED);
      customerLookupStatusPanel.add(hint, "span 2");
    }
    customerLookupStatusPanel.revalidate();
    customerLookupStatusPanel.repaint();
  }

  private void updateCustomerInfo() {
    customerInfoContent.removeAll();
    if (currentCustomer == null) {
      JLabel notSelected = new JLabel("Chưa chọn khách hàng");
      notSelected.setFont(DtaqFonts.BUTTON);
      notSelected.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(notSelected, "wrap");

      JLabel hint = new JLabel(wrap("Tìm theo số điện thoại để tự động điền họ tên và địa chỉ vào hóa đơn."));
      hint.setFont(DtaqFonts.BODY_13);
      hint.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(hint, "growx");
    } else {
      JLabel badge = new JLabel(currentCustomer.maKH() + " · Khách hàng hiện có");
      badge.setFont(DtaqFonts.HINT);
      badge.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(badge, "wrap");

      JLabel name = new JLabel(currentCustomer.hoTen());
      name.setFont(DtaqFonts.CUSTOMER_NAME);
      name.setForeground(DtaqColors.TEXT);
      customerInfoContent.add(name, "wrap");

      JLabel phone = new JLabel(PhoneFormat.format(currentCustomer.sdt()));
      phone.setFont(DtaqFonts.BODY_14);
      customerInfoContent.add(phone, "wrap");

      String address = currentCustomer.diaChi() != null ? currentCustomer.diaChi() : "";
      JLabel addr = new JLabel(wrap(address));
      addr.setFont(DtaqFonts.BODY_13);
      addr.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(addr, "growx");
    }
    customerInfoContent.revalidate();
    customerInfoContent.repaint();
  }

  private void updateCart() {
    int totalUnits = cartItems.stream().mapToInt(CartItem::soLuong).sum();
    cartSubtitleLabel.setText(cartItems.size() + " sản phẩm · " + totalUnits + " đơn vị");
    cartTableModel.setRowCount(0);
    if (cartItems.isEmpty()) {
      cartState.showEmpty();
      return;
    }
    for (CartItem item : cartItems) {
      cartTableModel.addRow(new Object[] {
          item.maSP(), item.tenSP(), item.soLuong(), item.donGiaTruocThue(),
          item.thueSuat(), item.thanhTienSauThue(), ""});
    }
    cartState.showContent();
  }

  private void updatePayment() {
    long total = totals.tongThanhToan();
    subtotalLabel.setText(MoneyFormat.formatVnd(totals.tongTruocThue()));
    taxLabel.setText(MoneyFormat.formatVnd(totals.tongThue()));
    totalAmountLabel.setText(MoneyFormat.format(total));

    long cash = cashField.getMoney();
    changeLabel.setText(MoneyFormat.formatVnd(Math.max(0, cash - total)));

    boolean hasCart = !cartItems.isEmpty();
    boolean canPay = !isLocked() && currentCustomer != null && hasCart && total > 0
        && cash >= total && onProcessPayment != null;

    if (state == InvoiceViewState.PAID) {
      payButton.setBusy(false);
      payButton.setText(PAID_TEXT);
    } else if (state == InvoiceViewState.PROCESSING) {
      payButton.setBusy(true);
    } else {
      payButton.setBusy(false);
      payButton.setText(PAY_TEXT);
    }
    payButton.setEnabled(canPay);

    Color color = DtaqColors.TEXT_MUTED;
    String message;
    if (state == InvoiceViewState.PAID) {
      message = "Đã thu đủ 100% giá trị hóa đơn và hoàn tất giao dịch.";
    } else if (state == InvoiceViewState.PROCESSING) {
      message = "Đang xử lý thanh toán…";
    } else if (paymentError != null) {
      message = paymentError;
      color = DtaqColors.DANGER;
    } else if (currentCustomer == null) {
      message = "Chọn khách hàng và thêm sản phẩm để thanh toán.";
    } else if (!hasCart) {
      message = "Thêm sản phẩm vào giỏ hàng để tiếp tục.";
    } else if (cash < total) {
      message = "Thu đủ 100% giá trị hóa đơn (còn thiếu " + MoneyFormat.formatVnd(total - cash) + ").";
      color = DtaqColors.DANGER;
    } else if (onProcessPayment == null) {
      message = NOT_CONNECTED_TIP + ".";
    } else {
      message = "Đã nhận đủ tiền mặt. Nhấn F8 để hoàn tất thanh toán.";
      color = DtaqColors.PRIMARY_STRONG;
    }
    conditionLabel.setText(wrap(message));
    conditionLabel.setForeground(color);
  }

  private void renderResultRows() {
    productResultsPanel.removeAll();
    productRowsPanel.removeAll();

    JPanel headerRow = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    headerRow.setOpaque(false);
    JLabel count = new JLabel(searchResults.isEmpty()
        ? "Không tìm thấy sản phẩm phù hợp" : searchResults.size() + " kết quả phù hợp");
    count.setFont(DtaqFonts.CAPTION);
    count.setForeground(DtaqColors.TEXT_MUTED);
    headerRow.add(count, "align left");
    if (!searchResults.isEmpty()) {
      JLabel priceHint = new JLabel("Đơn giá trước thuế · VNĐ");
      priceHint.setFont(DtaqFonts.HINT);
      priceHint.setForeground(DtaqColors.TEXT_SUBTLE);
      headerRow.add(priceHint, "align right");
    }
    productResultsPanel.add(headerRow, "growx, wrap");

    for (int i = 0; i < searchResults.size(); i++) {
      productRowsPanel.add(buildResultRow(i), "growx, wrap");
    }
    if (!searchResults.isEmpty()) {
      JScrollPane rows = new JScrollPane(new ViewportPanel(productRowsPanel) {
        { setOpaque(false); }
      });
      rows.setBorder(null);
      rows.setOpaque(false);
      rows.getViewport().setOpaque(false);
      rows.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      productResultsPanel.add(rows, "growx, hmax " + RESULT_LIST_MAX_HEIGHT);
    }

    productResultsPanel.setVisible(true);
    productResultsPanel.revalidate();
    productResultsPanel.repaint();
    updateStateFlag();
  }

  private JPanel buildResultRow(int index) {
    ProductResult res = searchResults.get(index);
    boolean selected = index == selectedResultIndex;

    JPanel row = new JPanel(new MigLayout("fillx, insets 10 12 10 12", "[]12[grow][]12[]", "[center]"));
    row.setOpaque(true);
    row.setBackground(selected ? DtaqColors.PRIMARY_SOFT : DtaqColors.SURFACE);
    row.putClientProperty(FlatClientProperties.STYLE, "arc: 16");
    row.setBorder(new FlatLineBorder(
        new Insets(1, 1, 1, 1), selected ? DtaqColors.PRIMARY_BORDER_SOFT : DtaqColors.BORDER, 1f, 16));
    row.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        selectedResultIndex = index;
        renderResultRows();
      }
    });

    row.add(Tile.icon("package", 20, DtaqColors.TEXT_MUTED, DtaqColors.BG_APP, 40, DtaqSpacing.RADIUS_CARD_SM));

    JPanel info = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    info.setOpaque(false);
    JLabel name = new JLabel(res.tenSP());
    name.setFont(DtaqFonts.BODY_13_MEDIUM);
    info.add(name, "wrap");
    JLabel sub = new JLabel(res.maSP() + " · Tồn " + res.tonKho() + " · Thuế " + res.thueSuat());
    sub.setFont(DtaqFonts.HINT);
    sub.setForeground(DtaqColors.TEXT_MUTED);
    info.add(sub);
    row.add(info, "growx");

    JLabel price = new JLabel(MoneyFormat.format(res.donGia()));
    price.setFont(DtaqFonts.BUTTON);
    row.add(price, "align right");

    DtaqButton addBtn = selected ? DtaqButton.primarySmall("Thêm") : DtaqButton.secondarySmall("Thêm");
    if (selected) {
      addBtn.setShortcutKey("↵");
    }
    addBtn.setEnabled(onAddToCart != null);
    addBtn.setToolTipText(onAddToCart != null ? null : NOT_CONNECTED_TIP);
    addBtn.addActionListener(e -> addResultToCart(res));
    row.add(addBtn, "align right");
    return row;
  }

  // ---------------------------------------------------------------- Page

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    register(inputMap, actionMap, KeyEvent.VK_F2, "focusPhone",
        () -> phoneLookupField.getTextField().requestFocusInWindow());
    register(inputMap, actionMap, KeyEvent.VK_F3, "focusProduct",
        () -> productSearchField.getTextField().requestFocusInWindow());
    register(inputMap, actionMap, KeyEvent.VK_F8, "payInvoice", this::executePayment);
  }

  private static void register(InputMap inputMap, ActionMap actionMap, int keyCode, String name, Runnable action) {
    inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), name);
    actionMap.put(name, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        action.run();
      }
    });
  }

  @Override
  public void onShow() {
    header.setSubtitle(headerMeta());
  }

  @Override
  public boolean scrollable() {
    return false;
  }

  @Override
  public PageId id() {
    return PageId.RETAIL_INVOICE;
  }

  @Override
  public String title() {
    return "Lập hóa đơn bán lẻ";
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
