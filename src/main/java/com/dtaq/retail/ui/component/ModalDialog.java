package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thoại modal chuẩn (Modal Dialog) hiển thị trên lớp nền mờ (overlay).
 */
public class ModalDialog extends JDialog {

  private final JPanel contentPanel;
  private final JPanel bodyPanel;
  private final JPanel footerPanel;

  public ModalDialog(Frame parent, String titleText, String subtitleText, int width) {
    super(parent, true);
    setUndecorated(true);
    setBackground(new Color(0, 0, 0, 0));

    // Outer root pane with semi-transparent overlay
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
        // Click outside content card closes dialog
        if (e.getSource() == overlayPanel) {
          dispose();
        }
      }
    });

    // Content card with rounded corners
    contentPanel = new JPanel(new MigLayout("fillx, insets 32", "[grow]", "[]20[]24[]")) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.SURFACE);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_DIALOG, DtaqSpacing.RADIUS_DIALOG));
        g2.setColor(DtaqColors.BORDER);
        g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, DtaqSpacing.RADIUS_DIALOG, DtaqSpacing.RADIUS_DIALOG));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    contentPanel.setOpaque(false);
    contentPanel.setPreferredSize(new Dimension(width, contentPanel.getPreferredSize().height));
    contentPanel.addMouseListener(new MouseAdapter() {}); // Block click from bubbling to overlay

    // Header
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
    closeBtn.setBorderPainted(false);
    closeBtn.setContentAreaFilled(false);
    closeBtn.setFocusPainted(false);
    closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    closeBtn.addActionListener(e -> dispose());
    header.add(closeBtn, "align right, top");

    contentPanel.add(header, "growx, wrap");

    // Body
    bodyPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]"));
    bodyPanel.setOpaque(false);
    contentPanel.add(bodyPanel, "growx, wrap");

    // Footer
    footerPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]8[]", "[]"));
    footerPanel.setOpaque(false);
    contentPanel.add(footerPanel, "growx");

    overlayPanel.add(contentPanel, "center");
    setContentPane(overlayPanel);

    // ESC to close
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          dispose();
        }
      }
    });

    if (parent != null) {
      setSize(parent.getWidth(), parent.getHeight());
      setLocation(parent.getLocation());
    } else {
      pack();
      setLocationRelativeTo(null);
    }
  }

  public JPanel getBody() {
    return bodyPanel;
  }

  public void setActions(JComponent... actions) {
    footerPanel.removeAll();
    footerPanel.add(new JSeparator(), "growx, span, wrap, gapbottom 16");
    footerPanel.add(new JLabel(), "growx"); // push to right
    for (JComponent action : actions) {
      footerPanel.add(action, "align right");
    }
    footerPanel.revalidate();
    footerPanel.repaint();
  }
}
