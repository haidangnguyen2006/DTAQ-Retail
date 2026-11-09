package com.dtaq.retail.service;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.config.HealthConfig;
import com.dtaq.retail.entity.HealthReport;
import com.dtaq.retail.entity.HealthStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kiểm tra sức khỏe dữ liệu bằng cách đối chiếu các bảng cốt lõi với baseline đã lưu.
 *
 * <p>Mỗi lần gọi {@link #kiemTra()} mở một kết nối chỉ đọc, xác nhận đủ bảng trong schema {@code
 * public}, đếm số dòng của từng bảng rồi so với {@link HealthStateStore}. Thiếu bảng hoặc schema
 * rỗng là {@link HealthStatus#CRITICAL}; giảm mạnh so với baseline là
 * {@link HealthStatus#SUSPICIOUS}; còn lại là {@link HealthStatus#HEALTHY}.
 *
 * <p>Lỗi {@link SQLException} không được ném ra ngoài: mã SQLState được truy theo cả chuỗi nguyên
 * nhân và {@code getNextException()} để phân loại, vì HikariCP thường bọc lỗi gốc. Thông điệp trả
 * về chỉ là mô tả của trình điều khiển, không chứa mật khẩu.
 *
 * <p>Lớp chỉ đọc dữ liệu; không sửa bảng, không ghi lịch sử và không quyết định phục hồi.
 *
 * <p>Nguồn dữ liệu được lấy lại ở mỗi chu kỳ qua {@link java.util.function.Supplier} để bộ kiểm tra
 * vẫn dùng được sau khi {@link RestoreService} gọi {@link ConnectDB#reset()} và dựng bể kết nối mới.
 */
public class DataHealthChecker {

  private static final Logger log = LoggerFactory.getLogger(DataHealthChecker.class);

  private static final String SQL_KIEM_TRA_BANG =
      "SELECT 1 FROM information_schema.tables"
          + " WHERE table_schema = 'public' AND table_name = ?";

  private static final String SQL_DEM_BANG = "SELECT count(*) FROM \"";

  private final Supplier<DataSource> dataSourceSupplier;
  private final HealthStateStore stateStore;

  /**
   * Khởi tạo bộ kiểm tra dùng một nguồn dữ liệu cố định.
   *
   * <p>Nguồn dữ liệu được giữ cố định nên chỉ phù hợp khi vòng đời bể kết nối không đổi. Ứng dụng
   * thật nên dùng {@link #DataHealthChecker(Supplier, HealthStateStore)} để sống sót qua lần
   * {@link ConnectDB#reset()} của {@link RestoreService}.
   *
   * @param dataSource nguồn dữ liệu dùng chung; không được {@code null}
   * @param stateStore kho trạng thái chứa baseline; không được {@code null}
   */
  public DataHealthChecker(DataSource dataSource, HealthStateStore stateStore) {
    this(() -> dataSource, stateStore);
    Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Khởi tạo bộ kiểm tra lấy nguồn dữ liệu qua một nhà cung cấp.
   *
   * <p>Mỗi chu kỳ đều gọi lại nhà cung cấp, nên khi dịch vụ phục hồi gọi {@link ConnectDB#reset()}
   * để dựng bể kết nối mới thì chu kỳ kế tiếp vẫn dùng được bể hiện tại thay vì bể đã đóng.
   *
   * @param dataSourceSupplier nhà cung cấp nguồn dữ liệu; không được {@code null}
   * @param stateStore kho trạng thái chứa baseline; không được {@code null}
   */
  public DataHealthChecker(Supplier<DataSource> dataSourceSupplier, HealthStateStore stateStore) {
    this.dataSourceSupplier = Objects.requireNonNull(dataSourceSupplier, "dataSourceSupplier");
    this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
  }

  /**
   * Tạo bộ kiểm tra dùng cấu hình và bể kết nối mặc định của ứng dụng.
   *
   * @return bộ kiểm tra gắn với {@link ConnectDB} và tệp trạng thái trong {@link HealthConfig}
   */
  public static DataHealthChecker macDinh() {
    return new DataHealthChecker(ConnectDB::getDataSource,
        new HealthStateStore(HealthConfig.tepTrangThai()));
  }

  /**
   * Thực hiện một chu kỳ kiểm tra sức khỏe dữ liệu.
   *
   * <p>Phương thức không ném ngoại lệ: mọi {@link SQLException} được chuyển thành báo cáo
   * {@link HealthStatus#UNAVAILABLE} hoặc {@link HealthStatus#CRITICAL} tùy SQLState.
   *
   * @return báo cáo của chu kỳ, luôn khác {@code null}
   */
  public HealthReport kiemTra() {
    Instant thoiDiem = Instant.now();
    try (Connection connection = dataSourceSupplier.get().getConnection()) {
      connection.setAutoCommit(true);
      connection.setReadOnly(true);

      List<String> bangCotLoi = HealthConfig.bangCotLoi();
      int timeout = HealthConfig.timeoutGiay();

      List<String> bangThieu = new ArrayList<>();
      List<String> bangCoMat = new ArrayList<>();
      try (PreparedStatement statement = connection.prepareStatement(SQL_KIEM_TRA_BANG)) {
        statement.setQueryTimeout(timeout);
        for (String bang : bangCotLoi) {
          statement.setString(1, bang);
          try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
              bangCoMat.add(bang);
            } else {
              bangThieu.add(bang);
            }
          }
        }
      }

      if (!bangThieu.isEmpty()) {
        Map<String, Long> soDong = new LinkedHashMap<>();
        for (String bang : bangCoMat) {
          demBang(connection, bang, timeout).ifPresent(so -> soDong.put(bang, so));
        }
        String chiTiet = "Thiếu bảng cốt lõi: " + String.join(", ", bangThieu);
        log.warn("Phát hiện thiếu bảng cốt lõi: {}", bangThieu);
        return new HealthReport(HealthStatus.CRITICAL, soDong, stateStore.doc().baseline(),
            Map.of(), bangThieu, chiTiet, null, thoiDiem);
      }

      Map<String, Long> soDong = new LinkedHashMap<>();
      long tong = 0L;
      for (String bang : bangCotLoi) {
        long so = demBangBatBuoc(connection, bang, timeout);
        soDong.put(bang, so);
        tong += so;
      }

      Map<String, Long> baseline = stateStore.doc().baseline();
      if (tong == 0L) {
        String chiTiet = "Schema rỗng: tất cả bảng cốt lõi đều 0 dòng";
        log.warn(chiTiet);
        return new HealthReport(HealthStatus.CRITICAL, soDong, baseline, Map.of(), List.of(),
            chiTiet, null, thoiDiem);
      }

      double nguong = HealthConfig.nguongGiam();
      int baselineToiThieu = HealthConfig.baselineToiThieu();
      Map<String, Double> tyLeGiam = new LinkedHashMap<>();
      List<String> chiTiet = new ArrayList<>();
      for (String bang : bangCotLoi) {
        Long goc = baseline.get(bang);
        if (goc == null || goc < baselineToiThieu) {
          continue;
        }
        long hienTai = soDong.get(bang);
        if (HealthPolicy.nenSuspicious(goc, hienTai, nguong, baselineToiThieu)) {
          double tyLe = (double) (goc - hienTai) / goc;
          tyLeGiam.put(bang, tyLe);
          chiTiet.add(String.format(Locale.ROOT, "%s: %d → %d (giảm %.1f%%)",
              bang, goc, hienTai, tyLe * 100));
        }
      }

      if (!tyLeGiam.isEmpty()) {
        log.warn("Phát hiện suy giảm dữ liệu: {}", chiTiet);
        return new HealthReport(HealthStatus.SUSPICIOUS, soDong, baseline, tyLeGiam, List.of(),
            String.join("; ", chiTiet), null, thoiDiem);
      }
      return new HealthReport(HealthStatus.HEALTHY, soDong, baseline, Map.of(), List.of(),
          "Dữ liệu cốt lõi bình thường", null, thoiDiem);
    } catch (SQLException e) {
      String sqlState = timSqlState(e);
      HealthStatus trangThai = phanLoaiSqlState(sqlState);
      String chiTiet = thongDiepLoi(e);
      log.warn("Không kiểm tra được sức khỏe dữ liệu (SQLState {}): {}", sqlState, chiTiet);
      return new HealthReport(trangThai, Map.of(), Map.of(), Map.of(), List.of(), chiTiet,
          sqlState, thoiDiem);
    }
  }

  private static OptionalLong demBang(Connection connection, String bang, int timeout) {
    try {
      return OptionalLong.of(demBangBatBuoc(connection, bang, timeout));
    } catch (SQLException e) {
      log.warn("Không đếm được số dòng bảng {}: {}", bang, e.getMessage());
      return OptionalLong.empty();
    }
  }

  private static long demBangBatBuoc(Connection connection, String bang, int timeout)
      throws SQLException {
    String sql = SQL_DEM_BANG + bang + '"';
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(timeout);
      try (ResultSet resultSet = statement.executeQuery(sql)) {
        return resultSet.next() ? resultSet.getLong(1) : 0L;
      }
    }
  }

  private static String thongDiepLoi(SQLException e) {
    String thongDiep = e.getMessage();
    return thongDiep == null || thongDiep.isBlank() ? e.getClass().getSimpleName() : thongDiep;
  }

  /**
   * Tìm SQLState đầu tiên khác rỗng trong chuỗi nguyên nhân và {@code getNextException()}.
   *
   * <p>Duyệt theo chiều rộng từ ngoại lệ gốc, lần lượt qua {@code getCause()} và, với
   * {@link SQLException}, qua {@link SQLException#getNextException()}. Tập đã thăm dùng so sánh
   * danh tính nên chuỗi lỗi lặp vòng không làm treo phương thức.
   *
   * @param t ngoại lệ cần tra; có thể {@code null}
   * @return SQLState không rỗng, hoặc {@code null} khi không có
   */
  static String timSqlState(Throwable t) {
    if (t == null) {
      return null;
    }
    Set<Throwable> daGap = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<Throwable> hangDoi = new ArrayDeque<>();
    hangDoi.add(t);
    while (!hangDoi.isEmpty()) {
      Throwable hienTai = hangDoi.poll();
      if (hienTai == null || !daGap.add(hienTai)) {
        continue;
      }
      if (hienTai instanceof SQLException sqlException) {
        String state = sqlException.getSQLState();
        if (state != null && !state.isBlank()) {
          return state.trim();
        }
        if (sqlException.getNextException() != null) {
          hangDoi.add(sqlException.getNextException());
        }
      }
      if (hienTai.getCause() != null) {
        hangDoi.add(hienTai.getCause());
      }
    }
    return null;
  }

  /**
   * Phân loại SQLState thành trạng thái sức khỏe.
   *
   * <p>Nhóm {@code 3D} (schema không tồn tại) là lỗi dữ liệu nghiêm trọng; nhóm {@code 08} (lỗi
   * kết nối) và {@code 28} (sai quyền/mật khẩu) là không thể đánh giá. Mã thiếu hoặc không thuộc
   * hai nhóm trên cũng được coi là {@link HealthStatus#UNAVAILABLE} để không kích hoạt phục hồi
   * nhầm.
   *
   * @param state mã SQLState; có thể {@code null} hoặc rỗng
   * @return trạng thái tương ứng
   */
  static HealthStatus phanLoaiSqlState(String state) {
    if (state == null || state.isBlank()) {
      return HealthStatus.UNAVAILABLE;
    }
    String chuan = state.trim().toUpperCase(Locale.ROOT);
    if (chuan.startsWith("3D")) {
      return HealthStatus.CRITICAL;
    }
    if (chuan.startsWith("08") || chuan.startsWith("28")) {
      return HealthStatus.UNAVAILABLE;
    }
    return HealthStatus.UNAVAILABLE;
  }
}
