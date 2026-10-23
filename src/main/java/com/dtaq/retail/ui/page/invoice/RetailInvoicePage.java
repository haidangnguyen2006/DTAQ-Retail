package com.dtaq.retail.ui.page.invoice;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.Card;
import com.dtaq.retail.ui.component.DataTable;
import com.dtaq.retail.ui.component.DtaqButton;
import com.dtaq.retail.ui.component.EmptyState;
import com.dtaq.retail.ui.component.FormField;
import com.dtaq.retail.ui.component.InfoCallout;
import com.dtaq.retail.ui.component.PageHeader;
import com.dtaq.retail.ui.component.ShortcutBar;
import com.dtaq.retail.ui.component.StatusPill;
import com.dtaq.retail.ui.format.DateFormats;
import com.dtaq.retail.ui.format.MoneyFormat;
import com.dtaq.retail.ui.format.PhoneFormat;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Màn hình Lập hóa đơn bán lẻ (RetailInvoicePage) theo Figma 5 frame tương tác.
 */
public class RetailInvoicePage implements Page {

  public record CustomerInfo(String maKH, String hoTen, String sdt, String diaChi) {}

  public record ProductResult(String maSP, String tenSP, int tonKho, String thueSuat, long donGia) {}

  public record CartItem(String maSP, String tenSP, int soLuong, long donGiaTruocThue, String thueSuat, long thanhTienSauThue) {}

  private final NhanVien nhanVien;
  private final JPanel view;
  private final PageHeader header;
  private final StatusPill invoiceStatusPill;

  // Left Column Cards
  private final Card customerLookupCard;
  private final FormField phoneLookupField;
  private final DtaqButton lookupCustomerBtn;
  private final JPanel customerLookupStatusPanel;

  private final Card productSearchCard;
  private final FormField productSearchField;
  private final JPanel productResultsPanel;

  private final Card cartCard;
  private final JLabel cartSubtitleLabel;
  private final DataTable cartTable;
  private final DefaultTableModel cartTableModel;
  private final JPanel cartFooterPanel;

  // Right Column Cards
  private final Card customerInfoCard;
  private final JPanel customerInfoContent;

  private final Card paymentCard;
  private final JLabel subtotalLabel = new JLabel("0 VNĐ");
  private final JLabel taxLabel = new JLabel("0 VNĐ");
  private final JLabel totalAmountLabel = new JLabel("0");
  private final JTextField cashGivenField = new JTextField();
  private final JLabel changeLabel = new JLabel("0 VNĐ");
  private final DtaqButton payButton = DtaqButton.primary("Thanh toán bằng tiền mặt");
  private final JLabel conditionLabel = new JLabel("Chọn khách hàng và thêm sản phẩm để thanh toán.");

  private InvoiceViewState state = InvoiceViewState.NO_CUSTOMER;
  private CustomerInfo currentCustomer;
  private final List<CartItem> cartItems = new ArrayList<>();
  private final List<ProductResult> searchResults = new ArrayList<>();
  private int selectedResultIndex = -1;

  private InvoiceReceiptDialog receiptDialog;
  private Consumer<String> onCustomerLookup;
  private Consumer<String> onProductSearch;
  private Consumer<CartItem> onAddToCart;
  private Runnable onProcessPayment;

  public RetailInvoicePage(NhanVien nhanVien) {
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    view = new JPanel(new MigLayout(
        "fillx, insets " + DtaqSpacing.PAGE_PADDING,
        "[grow]",
        "[]" + DtaqSpacing.SECTION_GAP + "[grow]"
    ));
    view.setBackground(DtaqColors.BG_APP);

    // 1. Header
    String headerMeta = "Nhân viên: " + nhanVien.hoTen() + " · " + DateFormats.formatDateTime(LocalDateTime.now());
    header = new PageHeader("Lập hóa đơn bán lẻ", headerMeta);
    invoiceStatusPill = StatusPill.neutral("Hóa đơn mới · Bản nháp");
    header.setTrailing(invoiceStatusPill);
    view.add(header, "growx, wrap");

    // 2. Main 2-columns body
    JPanel body = new JPanel(new MigLayout("fill, insets 0", "[grow, fill]24[360!]", "[top]"));
    body.setOpaque(false);

    // === LEFT COLUMN ===
    JPanel leftCol = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]20[]20[]16[]"));
    leftCol.setOpaque(false);

    // Card 1: Tra cứu khách hàng
    customerLookupCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 12);
    JPanel custSearchRow = new JPanel(new MigLayout("fillx, insets 0", "[grow]12[]", "[bottom]"));
    custSearchRow.setOpaque(false);

    phoneLookupField = FormField.text("Tìm khách hàng theo số điện thoại", "Nhập số điện thoại khách hàng", "phone");
    phoneLookupField.setShortcut("F2");
    phoneLookupField.getTextField().addActionListener(e -> triggerCustomerLookup());
    custSearchRow.add(phoneLookupField, "growx");

    lookupCustomerBtn = DtaqButton.secondary("Tìm khách hàng");
    lookupCustomerBtn.addActionListener(e -> triggerCustomerLookup());
    custSearchRow.add(lookupCustomerBtn);
    customerLookupCard.add(custSearchRow, "growx, wrap");

    customerLookupStatusPanel = new JPanel(new MigLayout("insets 0", "[]8[]", "[center]"));
    customerLookupStatusPanel.setOpaque(false);
    customerLookupCard.add(customerLookupStatusPanel, "growx");
    leftCol.add(customerLookupCard, "growx, wrap");

    // Card 2: Tìm sản phẩm
    productSearchCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 12);
    productSearchField = FormField.text("Tìm sản phẩm", "Nhập mã hoặc tên sản phẩm", "search");
    productSearchField.setShortcut("F3 · Enter để thêm");
    productSearchField.getTextField().addActionListener(e -> triggerProductSearch());
    productSearchCard.add(productSearchField, "growx, wrap");

    productResultsPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]6[]"));
    productResultsPanel.setOpaque(false);
    productResultsPanel.setVisible(false);
    productSearchCard.add(productResultsPanel, "growx");
    leftCol.add(productSearchCard, "growx, wrap");

    // Card 3: Giỏ hàng
    cartCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 16);
    JPanel cartHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    cartHeader.setOpaque(false);
    JLabel cartTitle = new JLabel("Giỏ hàng");
    cartTitle.setFont(DtaqFonts.CARD_TITLE);
    cartTitle.setForeground(DtaqColors.TEXT);
    cartHeader.add(cartTitle, "align left");

    cartSubtitleLabel = new JLabel("0 sản phẩm · 0 đơn vị");
    cartSubtitleLabel.setFont(DtaqFonts.CAPTION);
    cartSubtitleLabel.setForeground(DtaqColors.TEXT_MUTED);
    cartHeader.add(cartSubtitleLabel, "align right");
    cartCard.add(cartHeader, "growx, wrap");

    String[] cartCols = {"Mã", "Tên sản phẩm", "SL", "Đơn giá trước thuế", "Thuế suất", "Thành tiền"};
    cartTableModel = new DefaultTableModel(cartCols, 0) {
      @Override
      public boolean isCellEditable(int row, int col) {
        return false;
      }
    };
    cartTable = new DataTable(cartTableModel, 72);
    cartTable.setCodeColumn(0);
    cartTable.setMoneyColumn(3);
    cartTable.setMoneyColumn(5);
    cartTable.setColumnWidths(70, 290, 40, 100, 60, 110);

    cartFooterPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]12[]"));
    cartFooterPanel.setOpaque(false);

    leftCol.add(cartCard, "growx, wrap");

    // Shortcut bar
    leftCol.add(ShortcutBar.invoiceDefaults(), "growx");

    body.add(leftCol, "grow");

    // === RIGHT COLUMN ===
    JPanel rightCol = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]20[]"));
    rightCol.setOpaque(false);

    // Card 1: Thông tin khách hàng
    customerInfoCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 14);
    JLabel custInfoTitle = new JLabel("Thông tin khách hàng");
    custInfoTitle.setFont(DtaqFonts.CARD_TITLE);
    customerInfoCard.add(custInfoTitle, "wrap, gapbottom 10");

    customerInfoContent = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]6[]6[]"));
    customerInfoContent.setOpaque(false);
    customerInfoCard.add(customerInfoContent, "growx");
    rightCol.add(customerInfoCard, "growx, wrap");

    // Card 2: Thanh toán
    paymentCard = new Card(DtaqSpacing.CARD_PADDING_COMPACT, 16);
    JLabel paymentTitle = new JLabel("Thanh toán");
    paymentTitle.setFont(DtaqFonts.CARD_TITLE);
    paymentCard.add(paymentTitle, "wrap, gapbottom 12");

    JPanel totals = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]8[]8[]12[]12[]12[]8[]12[]"));
    totals.setOpaque(false);

    totals.add(new JLabel("Tổng trước thuế:"));
    subtotalLabel.setFont(DtaqFonts.AMOUNT_ROW);
    totals.add(subtotalLabel, "align right, wrap");

    totals.add(new JLabel("Tổng tiền thuế:"));
    taxLabel.setFont(DtaqFonts.AMOUNT_ROW);
    totals.add(taxLabel, "align right, wrap");

    totals.add(new JSeparator(), "growx, span, wrap");

    JLabel totalTitle = new JLabel("Tổng thanh toán:");
    totalTitle.setFont(DtaqFonts.LABEL);
    totals.add(totalTitle);

    JPanel totalBigBlock = new JPanel(new MigLayout("insets 0", "[]4[]", "[baseline]"));
    totalBigBlock.setOpaque(false);
    totalAmountLabel.setFont(DtaqFonts.AMOUNT_XL);
    totalAmountLabel.setForeground(DtaqColors.PRIMARY_STRONG);
    totalBigBlock.add(totalAmountLabel);
    JLabel vndLbl = new JLabel("VNĐ");
    vndLbl.setFont(DtaqFonts.CAPTION);
    vndLbl.setForeground(DtaqColors.TEXT_MUTED);
    totalBigBlock.add(vndLbl);
    totals.add(totalBigBlock, "align right, wrap");

    // Tiền khách đưa
    FormField cashField = new FormField("Tiền khách đưa (VNĐ)", cashGivenField);
    cashGivenField.setFont(DtaqFonts.BODY_14);
    cashGivenField.addActionListener(e -> updatePaymentState());
    totals.add(cashField, "growx, span, wrap");

    totals.add(new JLabel("Tiền trả lại khách:"));
    changeLabel.setFont(DtaqFonts.BUTTON);
    totals.add(changeLabel, "align right, wrap");

    payButton.setShortcutKey("F8");
    payButton.setEnabled(false);
    payButton.addActionListener(e -> executePayment());
    totals.add(payButton, "growx, span, h 42!, wrap");

    conditionLabel.setFont(DtaqFonts.HINT);
    conditionLabel.setForeground(DtaqColors.TEXT_MUTED);
    totals.add(conditionLabel, "growx, span");

    paymentCard.add(totals, "growx");
    rightCol.add(paymentCard, "growx");

    body.add(rightCol, "top");
    view.add(body, "grow, push");

    // Apply initial state
    applyState(InvoiceViewState.NO_CUSTOMER);
  }

  public void applyState(InvoiceViewState newState) {
    this.state = newState;
    updateCustomerLookupView();
    updateCustomerInfoView();
    updateCartView();
    updatePaymentState();
  }

  private void updateCustomerLookupView() {
    customerLookupStatusPanel.removeAll();
    if (currentCustomer == null) {
      JLabel hint = new JLabel("Tra cứu khách hàng có sẵn hoặc thêm mới tại Quản lý khách hàng.");
      hint.setFont(DtaqFonts.CAPTION);
      hint.setForeground(DtaqColors.TEXT_MUTED);
      customerLookupStatusPanel.add(hint);
    } else {
      JLabel icon = new JLabel(DtaqIcons.icon("circle-check", 17, DtaqColors.PRIMARY));
      customerLookupStatusPanel.add(icon);

      JLabel success = new JLabel(currentCustomer.hoTen() + " · " + currentCustomer.maKH());
      success.setFont(DtaqFonts.BODY_13_MEDIUM);
      success.setForeground(DtaqColors.PRIMARY_STRONG);
      customerLookupStatusPanel.add(success);

      JLabel sub = new JLabel("— Thông tin đã được điền vào hóa đơn");
      sub.setFont(DtaqFonts.CAPTION);
      sub.setForeground(DtaqColors.TEXT_MUTED);
      customerLookupStatusPanel.add(sub);
    }
    customerLookupStatusPanel.revalidate();
    customerLookupStatusPanel.repaint();
  }

  private void updateCustomerInfoView() {
    customerInfoContent.removeAll();
    if (currentCustomer == null) {
      JLabel notSelected = new JLabel("Chưa chọn khách hàng");
      notSelected.setFont(DtaqFonts.BUTTON);
      notSelected.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(notSelected, "wrap, gapbottom 4");

      JLabel hint = new JLabel("<html>Tìm theo số điện thoại để tự động điền họ tên và địa chỉ vào hóa đơn.</html>");
      hint.setFont(DtaqFonts.BODY_13);
      hint.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(hint, "growx");
    } else {
      JLabel badge = new JLabel(currentCustomer.maKH() + " · Khách hàng hiện có");
      badge.setFont(DtaqFonts.HINT);
      badge.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(badge, "wrap, gapbottom 2");

      JLabel name = new JLabel(currentCustomer.hoTen());
      name.setFont(DtaqFonts.CUSTOMER_NAME);
      name.setForeground(DtaqColors.TEXT);
      customerInfoContent.add(name, "wrap, gapbottom 4");

      JLabel phone = new JLabel(PhoneFormat.format(currentCustomer.sdt()));
      phone.setFont(DtaqFonts.BODY_14);
      phone.setForeground(DtaqColors.TEXT);
      customerInfoContent.add(phone, "wrap, gapbottom 2");

      JLabel addr = new JLabel("<html>" + (currentCustomer.diaChi() != null ? currentCustomer.diaChi() : "") + "</html>");
      addr.setFont(DtaqFonts.BODY_13);
      addr.setForeground(DtaqColors.TEXT_MUTED);
      customerInfoContent.add(addr, "growx");
    }
    customerInfoContent.revalidate();
    customerInfoContent.repaint();
  }

  private void updateCartView() {
    cartCard.removeAll();
    JPanel cartHeader = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    cartHeader.setOpaque(false);
    JLabel cartTitle = new JLabel("Giỏ hàng");
    cartTitle.setFont(DtaqFonts.CARD_TITLE);
    cartHeader.add(cartTitle, "align left");

    int totalUnits = cartItems.stream().mapToInt(CartItem::soLuong).sum();
    cartSubtitleLabel.setText(cartItems.size() + " sản phẩm · " + totalUnits + " đơn vị");
    cartHeader.add(cartSubtitleLabel, "align right");
    cartCard.add(cartHeader, "growx, wrap");

    if (cartItems.isEmpty()) {
      EmptyState empty = new EmptyState(
          "shopping-cart",
          "Chưa có sản phẩm trong giỏ",
          "Nhấn F3 để tìm sản phẩm và bắt đầu lập hóa đơn."
      );
      cartCard.add(empty, "center");
    } else {
      cartTableModel.setRowCount(0);
      long totalTax = 0;
      for (CartItem item : cartItems) {
        cartTableModel.addRow(new Object[]{
            item.maSP(),
            item.tenSP(),
            item.soLuong(),
            item.donGiaTruocThue(),
            item.thueSuat(),
            item.thanhTienSauThue()
        });
      }
      cartCard.add(cartTable, "grow, push, wrap");

      JPanel footer = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
      footer.setOpaque(false);
      StatusPill validPill = StatusPill.success("Tồn kho hợp lệ");
      footer.add(validPill, "align left");

      cartCard.add(footer, "growx, wrap");
      InfoCallout callout = new InfoCallout("Đơn giá và thuế suất được khóa theo sản phẩm. Không chỉnh giá hoặc áp dụng giảm giá tại quầy.");
      cartCard.add(callout, "growx");
    }
    cartCard.revalidate();
    cartCard.repaint();
  }

  private void updatePaymentState() {
    long subtotal = 0;
    long total = 0;
    for (CartItem item : cartItems) {
      subtotal += (item.donGiaTruocThue() * item.soLuong());
      total += item.thanhTienSauThue();
    }
    long tax = total - subtotal;

    subtotalLabel.setText(MoneyFormat.formatVnd(subtotal));
    taxLabel.setText(MoneyFormat.formatVnd(tax));
    totalAmountLabel.setText(MoneyFormat.format(total));

    long cash = 0;
    try {
      String cashText = cashGivenField.getText().replaceAll("\\D", "");
      if (!cashText.isEmpty()) {
        cash = Long.parseLong(cashText);
      }
    } catch (NumberFormatException ignored) {}

    long change = Math.max(0, cash - total);
    changeLabel.setText(MoneyFormat.formatVnd(change));

    boolean canPay = (currentCustomer != null) && !cartItems.isEmpty() && (cash >= total && total > 0);
    payButton.setEnabled(canPay);

    if (state == InvoiceViewState.PAID) {
      payButton.setText("Đã thu đủ tiền mặt");
      payButton.setEnabled(false);
      invoiceStatusPill.setType(StatusPill.Type.SUCCESS);
      invoiceStatusPill.setText("Đã thanh toán");
      conditionLabel.setText("Đã thu đủ 100% giá trị hóa đơn và hoàn tất giao dịch.");
    } else if (currentCustomer == null) {
      conditionLabel.setText("Chọn khách hàng và thêm sản phẩm để thanh toán.");
    } else if (cartItems.isEmpty()) {
      conditionLabel.setText("Thêm sản phẩm vào giỏ hàng để tiếp tục.");
    } else if (cash < total) {
      conditionLabel.setText("Thu đủ 100% giá trị hóa đơn (còn thiếu " + MoneyFormat.formatVnd(total - cash) + ").");
    } else {
      conditionLabel.setText("Đã nhận đủ tiền mặt. Nhấn F8 để hoàn tất thanh toán.");
    }
  }

  public void setCustomer(CustomerInfo customer) {
    this.currentCustomer = customer;
    applyState(cartItems.isEmpty() ? InvoiceViewState.CUSTOMER_SELECTED : InvoiceViewState.READY_TO_PAY);
  }

  public void addCartItem(CartItem item) {
    this.cartItems.add(item);
    applyState(InvoiceViewState.READY_TO_PAY);
  }

  public void renderSearchResults(List<ProductResult> results) {
    this.searchResults.clear();
    productResultsPanel.removeAll();
    if (results == null || results.isEmpty()) {
      productResultsPanel.setVisible(false);
      return;
    }
    this.searchResults.addAll(results);
    productResultsPanel.setVisible(true);

    JPanel headerRow = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    headerRow.setOpaque(false);
    JLabel countLbl = new JLabel(results.size() + " kết quả phù hợp");
    countLbl.setFont(DtaqFonts.CAPTION);
    countLbl.setForeground(DtaqColors.TEXT_MUTED);
    headerRow.add(countLbl, "align left");

    JLabel priceHint = new JLabel("Đơn giá trước thuế · VNĐ");
    priceHint.setFont(DtaqFonts.HINT);
    priceHint.setForeground(DtaqColors.TEXT_SUBTLE);
    headerRow.add(priceHint, "align right");
    productResultsPanel.add(headerRow, "growx, wrap, gapbottom 8");

    for (int i = 0; i < results.size(); i++) {
      ProductResult res = results.get(i);
      final int idx = i;
      boolean isSelected = (idx == selectedResultIndex);

      JPanel itemRow = new JPanel(new MigLayout("fillx, insets 10 12 10 12", "[]12[grow][]12[]", "[center]"));
      itemRow.setBackground(isSelected ? DtaqColors.PRIMARY_SOFT : DtaqColors.SURFACE);
      itemRow.setBorder(BorderFactory.createLineBorder(isSelected ? DtaqColors.PRIMARY_BORDER_SOFT : DtaqColors.BORDER, 1));

      itemRow.add(new JLabel(DtaqIcons.icon("tv", 24)));

      JPanel info = new JPanel(new MigLayout("insets 0", "[]", "[]2[]"));
      info.setOpaque(false);
      JLabel name = new JLabel(res.tenSP());
      name.setFont(DtaqFonts.BODY_13_MEDIUM);
      info.add(name, "wrap");

      JLabel sub = new JLabel(res.maSP() + " · Tồn " + res.tonKho() + " · Thuế " + res.thueSuat());
      sub.setFont(DtaqFonts.HINT);
      sub.setForeground(DtaqColors.TEXT_MUTED);
      info.add(sub);
      itemRow.add(info, "growx");

      JLabel price = new JLabel(MoneyFormat.format(res.donGia()));
      price.setFont(DtaqFonts.BUTTON);
      itemRow.add(price, "align right");

      DtaqButton addBtn = isSelected ? DtaqButton.primary("Thêm") : DtaqButton.secondary("Thêm");
      if (isSelected) {
        addBtn.setShortcutKey("↵");
      }
      addBtn.addActionListener(e -> addResultToCart(res));
      itemRow.add(addBtn, "align right");

      productResultsPanel.add(itemRow, "growx, wrap, gapbottom 6");
    }

    productResultsPanel.revalidate();
    productResultsPanel.repaint();
  }

  private void addResultToCart(ProductResult res) {
    long donGia = res.donGia();
    double rate = 0.10;
    try {
      rate = Double.parseDouble(res.thueSuat().replace("%", "").trim()) / 100.0;
    } catch (Exception ignored) {}
    long thanhTien = (long) (donGia * (1.0 + rate));

    CartItem item = new CartItem(res.maSP(), res.tenSP(), 1, donGia, res.thueSuat(), thanhTien);
    addCartItem(item);
    productResultsPanel.setVisible(false);
  }

  private void triggerCustomerLookup() {
    String phone = phoneLookupField.getText().trim();
    if (onCustomerLookup != null) {
      onCustomerLookup.accept(phone);
    }
  }

  private void triggerProductSearch() {
    String query = productSearchField.getText().trim();
    if (onProductSearch != null) {
      onProductSearch.accept(query);
    }
  }

  private void executePayment() {
    if (!payButton.isEnabled()) {
      return;
    }
    applyState(InvoiceViewState.PAID);
    openReceiptDialog();
    if (onProcessPayment != null) {
      onProcessPayment.run();
    }
  }

  private void openReceiptDialog() {
    Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(view);
    receiptDialog = new InvoiceReceiptDialog(topFrame, this::resetForNewInvoice);

    long subtotal = 0;
    long total = 0;
    List<InvoiceReceiptDialog.ReceiptItem> items = new ArrayList<>();
    int stt = 1;
    for (CartItem ci : cartItems) {
      subtotal += ci.donGiaTruocThue();
      total += ci.thanhTienSauThue();
      items.add(new InvoiceReceiptDialog.ReceiptItem(
          stt++, ci.maSP(), ci.tenSP(), "Cái", ci.soLuong(), ci.donGiaTruocThue(), ci.thueSuat(), ci.thanhTienSauThue()
      ));
    }
    long cash = 0;
    try {
      cash = Long.parseLong(cashGivenField.getText().replaceAll("\\D", ""));
    } catch (Exception ignored) {}

    InvoiceReceiptDialog.ReceiptData rData = new InvoiceReceiptDialog.ReceiptData(
        "HD-" + System.currentTimeMillis() % 100000,
        LocalDateTime.now(),
        currentCustomer != null ? currentCustomer.hoTen() : "Khách vãng lai",
        currentCustomer != null ? currentCustomer.maKH() : "KH0000",
        currentCustomer != null ? currentCustomer.sdt() : "",
        currentCustomer != null ? currentCustomer.diaChi() : "",
        nhanVien.hoTen(),
        items,
        subtotal,
        total - subtotal,
        total,
        cash,
        Math.max(0, cash - total)
    );
    receiptDialog.render(rData);
    receiptDialog.setVisible(true);
  }

  public void resetForNewInvoice() {
    currentCustomer = null;
    cartItems.clear();
    searchResults.clear();
    phoneLookupField.setText("");
    productSearchField.setText("");
    cashGivenField.setText("");
    invoiceStatusPill.setType(StatusPill.Type.NEUTRAL);
    invoiceStatusPill.setText("Hóa đơn mới · Bản nháp");
    payButton.setText("Thanh toán bằng tiền mặt");
    applyState(InvoiceViewState.NO_CUSTOMER);
  }

  @Override
  public void installShortcuts(InputMap inputMap, ActionMap actionMap) {
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusPhone");
    actionMap.put("focusPhone", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        phoneLookupField.getTextField().requestFocusInWindow();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusProduct");
    actionMap.put("focusProduct", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        productSearchField.getTextField().requestFocusInWindow();
      }
    });

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "payInvoice");
    actionMap.put("payInvoice", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (payButton.isEnabled()) {
          executePayment();
        }
      }
    });
  }

  public void setOnCustomerLookup(Consumer<String> listener) {
    this.onCustomerLookup = listener;
  }

  public void setOnProductSearch(Consumer<String> listener) {
    this.onProductSearch = listener;
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
