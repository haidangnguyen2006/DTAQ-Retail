package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Bể kết nối dùng chung ConnectDB")
class ConnectDBTest {

  @Test
  @DisplayName("DataSource lấy trước khi reset (phục hồi dữ liệu) vẫn mở được kết nối sau reset")
  void dataSourceCuVanDungDuocSauReset() throws Exception {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    DataSource truocReset = ConnectDB.getDataSource();

    ConnectDB.reset();

    assertSame(truocReset, ConnectDB.getDataSource());
    try (Connection connection = truocReset.getConnection()) {
      assertTrue(connection.isValid(5));
    }
  }
}
