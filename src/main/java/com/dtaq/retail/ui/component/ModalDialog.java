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

  private final JPanel bodyPanel;
  private final JPanel footerPanel;
  private final JPanel contentPanel;
  private final JScrollPane bodyScroll;
  private boolean closeOnOverlayClick;

  public ModalDialog(Frame parent, String titleText, String subtitleText, int width) {
    super(parent, true);
    setUndecorated(true);
    // Máy không hỗ trợ cửa sổ trong suốt từng điểm ảnh (một số RDP/VM): dùng nền đặc để hộp thoại vẫn mở được.
    boolean translucent = getGraphicsConfiguration().getDevice()
        .isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
    if (translucent) {
      setBackground(new Color(0, 0, 0, 0));
    }

    JPanel overlayPanel = new JPanel(new MigLayout("fill, insets 0", "[center]", "[center]")) {
      @Override
      protected void paintComponent(Graphics g) {
        g.setColor(translucent ? DtaqColors.OVERLAY : new Color(0xC9CED8));
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

    contentPanel = new JPanel(new MigLayout("fillx, insets 24", "[grow]", "[]16[]20[]")) {
      @Override
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int available = parent != null
            ? parent.getWidth() - 2 * DtaqSpacing.DIALOG_MARGIN : width;
        d.width = Math.min(width, Math.max(320, available));
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
    bodyScroll = new JScrollPane(new ViewportPanel(bodyPanel) {
      { setOpaque(false); }
    });
    bodyScroll.setBorder(null);
    bodyScroll.setOpaque(false);
    bodyScroll.getViewport().setOpaque(false);
    bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    contentPanel.add(bodyScroll, "growx, hmin 0, hmax 480, wrap");

    footerPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]16[]"));
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
    addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        fitBody();
      }
    });
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
    closeBtn.getAccessibleContext().setAccessibleName("Đóng (Esc)");
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
    footerPanel.add(new JSeparator(), "growx, wrap");
    JPanel actionRow = new JPanel(new MigLayout("insets 0, gap 8, alignx right", "[]", "[]"));
    actionRow.setOpaque(false);
    for (JComponent action : actions) {
      actionRow.add(action);
    }
    actionRow.addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        int widest = 1;
        for (JComponent action : actions) {
          widest = Math.max(widest, action.getPreferredSize().width + 8);
        }
        int columns = Math.max(1, Math.min(actions.length, actionRow.getWidth() / widest));
        ((MigLayout) actionRow.getLayout()).setLayoutConstraints(
            "insets 0, gap 8, alignx right, wrap " + columns);
        actionRow.revalidate();
        fitBody();
      }
    });
    footerPanel.add(actionRow, "growx");
    footerPanel.revalidate();
    footerPanel.repaint();
  }

  private void fitBody() {
    int reserve = 2 * DtaqSpacing.DIALOG_MARGIN + 50 + 36
        + contentPanel.getComponent(0).getPreferredSize().height + footerPanel.getPreferredSize().height;
    ((MigLayout) contentPanel.getLayout()).setComponentConstraints(bodyScroll,
        "growx, hmin 0, hmax " + Math.max(0, getHeight() - reserve) + ", wrap");
    contentPanel.revalidate();
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
      } else {
        pack();
        Responsive.fitWindow(this, getSize(), new Dimension(0, 0));
      }
      fitBody();
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
