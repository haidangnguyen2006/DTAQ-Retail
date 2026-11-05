package com.dtaq.retail;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.DbMigration;
import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.dao.HealthIncidentDao;
import com.dtaq.retail.dao.impl.HealthIncidentDaoImpl;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.dao.impl.PasswordResetDaoImpl;
import com.dtaq.retail.service.BackupScheduler;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.DataHealthChecker;
import com.dtaq.retail.service.HealthMonitor;
import com.dtaq.retail.service.HealthStateStore;
import com.dtaq.retail.service.RecoveryCoordinator;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.ui.AppNavigator;
import com.dtaq.retail.util.AppInfo;
import com.dtaq.retail.util.Mailer;
import com.dtaq.retail.util.SmtpMailer;
import com.dtaq.retail.ui.theme.DtaqTheme;
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
    DtaqTheme.install();
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

    BackupScheduler backupScheduler = null;
    try {
      BackupService backupService = BackupService.macDinh(dataSource);
      RestoreService restoreService = RestoreService.macDinh(dataSource, backupService);
      BackupScheduler khoiTao = new BackupScheduler(backupService, "SYSTEM");
      backupScheduler = khoiTao;

      // Kiểm tra sức khỏe ngay và bật giám sát trước khi lịch sao lưu chạy bù, để AppState phản
      // ánh đúng tình trạng dữ liệu khi bộ lập lịch quyết định có chạy hay không.
      try {
        HealthStateStore healthStore = new HealthStateStore(HealthConfig.tepTrangThai());
        // DAO và bộ kiểm tra lấy nguồn dữ liệu qua ConnectDB ở mỗi lần dùng: nếu lần kiểm tra đầu
        // tiên tự động phục hồi, RestoreService sẽ reset bể kết nối và các đối tượng này vẫn sống.
        HealthIncidentDao incidentDao = new HealthIncidentDaoImpl(ConnectDB::getDataSource);
        DataHealthChecker checker = new DataHealthChecker(ConnectDB::getDataSource, healthStore);
        RecoveryCoordinator coordinator =
            new RecoveryCoordinator(healthStore, incidentDao, backupService, restoreService);
        HealthMonitor healthMonitor = new HealthMonitor(checker, coordinator);
        healthMonitor.kiemTraNgay();
        healthMonitor.start();
        Runtime.getRuntime()
            .addShutdownHook(new Thread(healthMonitor::close, "dtaq-health-shutdown"));
      } catch (RuntimeException e) {
        log.error("Không khởi động được giám sát sức khỏe: {}", e.getMessage());
      }

      khoiTao.start();
      Runtime.getRuntime()
          .addShutdownHook(new Thread(khoiTao::close, "dtaq-backup-shutdown"));
    } catch (RuntimeException e) {
      log.error("Không khởi động được lịch sao lưu: {}", e.getMessage());
    }

    // Lấy lại nguồn dữ liệu sau khối kiểm tra sức khỏe: một lần tự động phục hồi lúc khởi động đã
    // reset bể kết nối, nên biến dataSource ban đầu có thể trỏ tới bể đã đóng.
    DataSource uiDataSource = ConnectDB.getDataSource();
    // Bộ lập lịch có thể chưa khởi động được (null); khi đó trang Sao lưu vẫn hoạt động nhưng lưu
    // cấu hình không áp dụng tức thì.
    final BackupScheduler schedulerChoUi = backupScheduler;

    SwingUtilities.invokeLater(() -> {
      NhanVienDaoImpl nhanVienDao = new NhanVienDaoImpl(uiDataSource);
      PasswordResetDaoImpl passwordResetDao = new PasswordResetDaoImpl(uiDataSource);
      Mailer mailer = new SmtpMailer();
      AppNavigator navigator =
          new AppNavigator(nhanVienDao, passwordResetDao, mailer, uiDataSource, schedulerChoUi);
      navigator.showLogin();
    });
  }
}
