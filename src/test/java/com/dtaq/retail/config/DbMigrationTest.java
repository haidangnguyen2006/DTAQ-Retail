package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

/** Configuration-only tests for {@link DbMigration}; no database connection is opened. */
class DbMigrationTest {

  @Test
  void configurationReusesProvidedDataSourceAndMigrationLocation() {
    DataSource dataSource = dataSourceThatRejectsConnections();

    FluentConfiguration configuration = DbMigration.configuration(dataSource);

    assertSame(dataSource, configuration.getDataSource());
    List<String> locations = Arrays.stream(configuration.getLocations())
        .map(Location::getDescriptor)
        .toList();
    assertEquals(List.of(DbMigration.MIGRATION_LOCATION), locations);
  }

  private DataSource dataSourceThatRejectsConnections() {
    return (DataSource) Proxy.newProxyInstance(
        DataSource.class.getClassLoader(),
        new Class<?>[] {DataSource.class},
        (proxy, method, args) -> {
          if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
              case "toString" -> "unconnected-test-data-source";
              case "hashCode" -> System.identityHashCode(proxy);
              case "equals" -> proxy == args[0];
              default -> null;
            };
          }
          throw new SQLException("Test data source does not permit connections");
        });
  }
}
