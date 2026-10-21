package com.dtaq.retail;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.DbMigration;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.dao.impl.PasswordResetDaoImpl;
import com.dtaq.retail.ui.AppNavigator;
import com.dtaq.retail.util.AppInfo;
import com.dtaq.retail.util.Mailer;
import com.dtaq.retail.util.SmtpMailer;
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

    try {
      DbMigration.migrate(dataSource);
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      String causeType = (cause == null) ? "none" : cause.getClass().getName();
      log.error("Database migration failed; application startup aborted (exception={}, cause={})",
          e.getClass().getName(), causeType);
      ConnectDB.close();
      return;
    }

    SwingUtilities.invokeLater(() -> {
      NhanVienDaoImpl nhanVienDao = new NhanVienDaoImpl(dataSource);
      PasswordResetDaoImpl passwordResetDao = new PasswordResetDaoImpl(dataSource);
      Mailer mailer = new SmtpMailer();
      AppNavigator navigator = new AppNavigator(nhanVienDao, passwordResetDao, mailer);
      navigator.showLogin();
    });
  }
}
