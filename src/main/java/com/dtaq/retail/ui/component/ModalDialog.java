package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại modal chuẩn (Modal Dialog) hiển thị trên lớp nền mờ (overlay) phủ cửa sổ chính.
 * Esc luôn đóng được (kể cả khi focus trong ô nhập); bấm ra ngoài chỉ đóng khi bật
 * {@link #setCloseOnOverlayClick(boolean)} để không mất dữ liệu đang nhập.
 */
public class ModalDialog extends JDialog {

  private static final int MIN_BODY_HEIGHT = 240;
  private static final int BODY_VERTICAL_RESERVE = 280;

  private final JPanel bodyPanel;
  private final JPanel footerPanel;
  private boolean closeOnOverlayClick;

  public ModalDialog(Frame parent, String titleText, String subtitleText, int width) {
    super(parent, true);
    setUndecorated(true);
    setBackground(new Color(0, 0, 0, 0));

    JPanel overlayPanel = new JPanel(new MigLayout("fill, insets 0", "[center]", "[center]")) {
      @Override
      protected void paintComponent(Graphics g) {
        g.setColor(DtaqColors.OVERLAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
      }
    };
    overlayPanel.setOpaque(false);
    overlayPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (closeOnOverlayClick && e.getSource() == overlayPanel) {
          requestClose();
        }
      }
    });

    JPanel contentPanel = new JPanel(new MigLayout("fillx, insets 28", "[grow]", "[]18[]22[]")) {
      @Override
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = width;
        return d;
      }
    };
    contentPanel.setOpaque(true);
    contentPanel.setBackground(DtaqColors.SURFACE);
    contentPanel.putClientProperty(FlatClientProperties.STYLE, "arc: " + DtaqSpacing.RADIUS_DIALOG * 2);
    contentPanel.setBorder(new FlatLineBorder(
        new Insets(1, 1, 1, 1), DtaqColors.BORDER, 1f, DtaqSpacing.RADIUS_DIALOG * 2));
    contentPanel.addMouseListener(new MouseAdapter() { });

    contentPanel.add(buildHeader(titleText, subtitleText), "growx, wrap");

    bodyPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]"));
    bodyPanel.setOpaque(false);
    JScrollPane bodyScroll = new JScrollPane(new ViewportPanel(bodyPanel) {
      { setOpaque(false); }
    });
    bodyScroll.setBorder(null);
    bodyScroll.setOpaque(false);
    bodyScroll.getViewport().setOpaque(false);
    bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    int maxBodyHeight = parent != null
        ? Math.max(MIN_BODY_HEIGHT, parent.getHeight() - BODY_VERTICAL_RESERVE) : 480;
    contentPanel.add(bodyScroll, "growx, hmax " + maxBodyHeight + ", wrap");

    footerPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]8[]", "[]"));
    footerPanel.setOpaque(false);
    contentPanel.add(footerPanel, "growx");

    overlayPanel.add(contentPanel, "center");
    setContentPane(overlayPanel);

    getRootPane().registerKeyboardAction(
        e -> requestClose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);

    if (parent != null) {
      setBounds(parent.getBounds());
    } else {
      pack();
      setLocationRelativeTo(null);
    }
  }

  private JPanel buildHeader(String titleText, String subtitleText) {
    JPanel header = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
    header.setOpaque(false);

    JPanel titleBlock = new JPanel(new MigLayout("insets 0", "[]", "[]6[]"));
    titleBlock.setOpaque(false);

    JLabel titleLabel = new JLabel(titleText);
    titleLabel.setFont(DtaqFonts.DIALOG_TITLE);
    titleLabel.setForeground(DtaqColors.TEXT);
    titleBlock.add(titleLabel, "wrap");

    if (subtitleText != null && !subtitleText.trim().isEmpty()) {
      JLabel subLabel = new JLabel(subtitleText);
      subLabel.setFont(DtaqFonts.BODY_13);
      subLabel.setForeground(DtaqColors.TEXT_MUTED);
      titleBlock.add(subLabel, "wrap");
    }
    header.add(titleBlock, "align left");

    JButton closeBtn = new JButton(DtaqIcons.icon("x", 20, DtaqColors.TEXT_MUTED));
    closeBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    closeBtn.setFocusable(false);
    closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    closeBtn.setToolTipText("Đóng (Esc)");
    closeBtn.addActionListener(e -> requestClose());
    header.add(closeBtn, "align right, top");
    return header;
  }

  /** Yêu cầu đóng hộp thoại (nút ×, phím Esc, bấm nền). Lớp con ghi đè để chặn khi đang xử lý. */
  protected void requestClose() {
    dispose();
  }

  public void setCloseOnOverlayClick(boolean closeOnOverlayClick) {
    this.closeOnOverlayClick = closeOnOverlayClick;
  }

  public JPanel getBody() {
    return bodyPanel;
  }

  public void setActions(JComponent... actions) {
    footerPanel.removeAll();
    footerPanel.add(new JSeparator(), "growx, span, wrap, gapbottom 16");
    footerPanel.add(new JLabel(), "growx");
    for (JComponent action : actions) {
      footerPanel.add(action, "align right");
    }
    footerPanel.revalidate();
    footerPanel.repaint();
  }

  /** Nút mặc định: được kích hoạt khi nhấn Enter. */
  public void setPrimaryAction(JButton button) {
    getRootPane().setDefaultButton(button);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      Container owner = getOwner();
      if (owner != null) {
        setBounds(owner.getBounds());
      }
      SwingUtilities.invokeLater(this::focusFirstInput);
    }
    super.setVisible(visible);
  }

  private void focusFirstInput() {
    Component target = findFirstInput(bodyPanel);
    if (target != null) {
      target.requestFocusInWindow();
    }
  }

  private static Component findFirstInput(Container container) {
    for (Component child : container.getComponents()) {
      if ((child instanceof JTextComponent text && text.isEditable() && text.isEnabled())
          || (child instanceof JComboBox<?> combo && combo.isEnabled())) {
        return child;
      }
      if (child instanceof Container nested) {
        Component found = findFirstInput(nested);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
