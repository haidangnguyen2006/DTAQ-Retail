package com.dtaq.retail.config;

import java.util.Objects;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Runs Flyway schema migrations against the application's shared {@link DataSource}.
 *
 * <p>This class never creates its own connection pool or reads database credentials. The caller
 * supplies the {@link DataSource} owned by {@link ConnectDB}, so migrations and application queries
 * use the same pool. Migrations are loaded from {@code classpath:db/migration}; no migration scripts
 * are bundled yet, so a fresh database remains empty until a migration is separately approved.
 */
public final class DbMigration {

  /** Classpath location scanned by Flyway for versioned migration scripts. */
  public static final String MIGRATION_LOCATION = "classpath:db/migration";

  private DbMigration() {
  }

  /**
   * Applies all pending migrations using the supplied data source.
   *
   * @param dataSource shared application data source; must not be {@code null}
   * @throws IllegalStateException if Flyway cannot configure or apply migrations
   */
  public static void migrate(DataSource dataSource) {
    try {
      configuration(dataSource).load().migrate();
    } catch (RuntimeException e) {
      throw new IllegalStateException("Database migration failed", e);
    }
  }

  /**
   * Builds the Flyway configuration without connecting to the database.
   *
   * @param dataSource shared application data source
   * @return Flyway configuration bound to the supplied data source and migration location
   */
  static FluentConfiguration configuration(DataSource dataSource) {
    Objects.requireNonNull(dataSource, "dataSource");
    return Flyway.configure()
        .dataSource(dataSource)
        .locations(MIGRATION_LOCATION);
  }
}
