package com.dtaq.retail.ui;

import com.dtaq.retail.util.AppInfo;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/** Main application window. */
public class MainFrame extends JFrame {

  public MainFrame() {
    super(AppInfo.APP_NAME);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));

    JLabel header = new JLabel(AppInfo.APP_NAME + " " + AppInfo.version());
    add(header, "wrap");

    JPanel center = new JPanel(new MigLayout("fill"));
    center.add(new JLabel("Chào mừng đến DTAQ Retail", SwingConstants.CENTER), "center");
    add(center, "grow, push");

    pack();
    setMinimumSize(new Dimension(800, 520));
    AppInfo.centerOnScreen(this);
  }
}
