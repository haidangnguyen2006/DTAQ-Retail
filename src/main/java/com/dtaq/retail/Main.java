package com.dtaq.retail;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.controller.MainController;
import com.dtaq.retail.dao.impl.ProductDaoImpl;
import com.dtaq.retail.ui.MainFrame;
import com.dtaq.retail.util.AppInfo;
import com.formdev.flatlaf.FlatLightLaf;
import javax.sql.DataSource;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Application entry point. */
public final class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private Main() {
  }

  public static void main(String[] args) {
    FlatLightLaf.setup();
    log.info("Starting {} version {}", AppInfo.APP_NAME, AppInfo.version());

    DataSource dataSource = ConnectDB.getDataSource();
    Runtime.getRuntime().addShutdownHook(new Thread(ConnectDB::close, "dtaq-retail-shutdown"));

    SwingUtilities.invokeLater(() -> {
      MainFrame frame = new MainFrame();
      new MainController(frame, new ProductDaoImpl(dataSource)).refresh();
      frame.setVisible(true);
    });
  }
}
