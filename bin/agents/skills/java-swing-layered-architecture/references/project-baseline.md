# Project Baseline — DTAQ Retail

Verified snapshot of the working tree on **2026-10-20**. Every fact below was read
from the repository files named here. This snapshot can go stale: re-inspect
`pom.xml` and the package map before relying on it, and re-check the live database
schema before writing new SQL.

## Artifact and toolchain

- Coordinates: `com.dtaq:dtaq-retail:1.0.0-SNAPSHOT`, packaging `jar`.
- Main class: `com.dtaq.retail.Main` (shaded runnable jar).
- Language level: Java 21; source and reporting encoding UTF-8.
- Build outputs a shaded jar at `target/dtaq-retail-1.0.0-SNAPSHOT.jar`.

## Pinned versions (`pom.xml`)

| Dependency | Version | Scope |
| :--- | :--- | :--- |
| FlatLaf | 3.6 | compile |
| MigLayout Swing | 11.4.2 | compile |
| MigLayout Core | 11.4.2 | compile |
| HikariCP | 6.3.0 | compile |
| SLF4J API | 2.0.17 | compile |
| PostgreSQL JDBC | 42.7.7 | runtime |
| Flyway Core | 13.10.2 | compile |
| Flyway Database PostgreSQL | 13.10.2 | compile |
| Logback Classic | 1.5.18 | runtime |
| Logback Core | 1.5.18 | runtime |
| JUnit Jupiter | 5.12.2 | test |

Plugins: maven-compiler-plugin 3.15.0 (`release` 21, UTF-8), maven-surefire-plugin
3.5.6 (`useModulePath=false`), maven-shade-plugin 3.6.2 (attaches the shaded jar in
the `package` phase, `ServicesResourceTransformer`, manifest main class).

## Package and file map (`src/main/java/com/dtaq/retail`)

| Package | File | Role |
| :--- | :--- | :--- |
| (root) | `Main` | Entry point; sets up FlatLaf, obtains the shared `DataSource`, registers a shutdown hook, runs Flyway fail-fast, then opens `MainFrame` on the EDT. |
| `config` | `DbConfig` | Static configuration reader with layered precedence. |
| `config` | `ConnectDB` | Owns the single shared HikariCP pool. |
| `config` | `DbMigration` | Thin Flyway runner bound to the shared `DataSource`; location `classpath:db/migration`. |
| `exception` | `DataAccessException` | Unchecked persistence failure. |
| `ui` | `MainFrame` | Generic welcome window (JFrame, MigLayout); no product status label/setter. |
| `util` | `AppInfo` | App name/version and window centering helpers. |

No `controller`, `dao`, `dao.impl` or `entity` package remains after the Product
demo cleanup. No business DAO/entity classes and no business screens are
implemented yet for the V1 tables.

Resources: `src/main/resources/logback.xml` (console appender, JSON-free
pattern, `com.dtaq.retail` at DEBUG, root at INFO) and
`src/main/resources/db/migration/V1__initial_schema.sql` (the only versioned
migration).

Tests: `src/test/java/com/dtaq/retail/config/DbConfigTest.java`,
`src/test/java/com/dtaq/retail/config/DbMigrationTest.java`.

## Connection lifecycle (`config/ConnectDB`)

- One lazily-created shared `HikariDataSource`; never created per DAO call.
- Pool name `dtaq-retail-pool`; `initializationFailTimeout(-1)` so obtaining the
  pool does not itself require a reachable database.
- Exposed via `getDataSource()`; also `isAvailable()` and `close()`.
- Registers a JVM shutdown hook that closes the pool.
- `Main` registers its own shutdown hook calling `ConnectDB::close` as well;
  `close()` checks for null/closed state, so repeated calls are safe.

## Configuration resolution (`config/DbConfig`)

- Precedence: system property > environment variable > `db.properties` > built-in default.
- `db.properties` is loaded lazily from the current working directory, never throws;
  missing file is logged at INFO, load failure at WARN.
- Keys: `DTAQ_DB_URL`, `DTAQ_DB_USER`, `DTAQ_DB_PASSWORD`, `DTAQ_DB_POOL_SIZE`.
- Defaults: URL `jdbc:postgresql://localhost:5432/dtaq_retail`, user `postgres`,
  empty password, pool size 5; unparsable pool size falls back to 5.
- Blank password logs a warning without printing the value.
- `setPropertiesFileForTest(Path)` is a package-private test seam.

## Flyway startup and schema state

- `Main` (`Main.java:21-43`) obtains the shared `DataSource`, registers the
  shutdown hook, calls `DbMigration.migrate(dataSource)` on the main thread, and
  only then submits `MainFrame` creation to `SwingUtilities.invokeLater`.
- On migration failure, `Main` logs the exception/cause **types only** (no message,
  URL or credentials), calls `ConnectDB.close()` and returns without opening the UI.
- `DbMigration` (`DbMigration.java:30-49`) configures
  `Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")`
  and calls `load().migrate()`; it does not create a second pool and does not read
  credentials itself. Runtime failures are wrapped in `IllegalStateException`.
- `V1__initial_schema.sql` creates the 14 documented domain tables, with the
  approved serial reference and sale-time snapshot amendments for
  `ChiTietHoaDon`/`SanPhamLoi`. It creates no `products` demo table, no seed data,
  no compatibility table and no baseline. Primary keys are application-supplied
  strings; the script defines no sequence, identity or default for IDs.
- **Runtime verification:** theo bằng chứng U1 đã ghi tại
  [`docs/reference/database/schema-and-integrity.md` §6.6](../../../../docs/reference/database/schema-and-integrity.md#66-kiểm-chứng-v1),
  V1/Flyway 13.10.2 đã chạy trên **PostgreSQL 16.13 disposable**: lần đầu áp dụng
  1 migration (`migrationsExecuted=1`), lần hai không còn migration chờ
  (`migrationsExecuted=0`); cluster disposable đã được dừng và xóa. Không dùng
  CSDL thật/existing, và phiên IDE chính này không trực tiếp chạy psql/Flyway.
- `DbMigrationTest` only inspects Flyway configuration with a proxy `DataSource`
  whose `getConnection()` throws; it opens no connection.
- Note: the `DbMigration` class Javadoc still contains a pre-V1 sentence saying no
  migration scripts are bundled. The resource listed above is authoritative.

## Build, run, and test commands

```bash
mvn clean package
java -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar
```

At this snapshot, `mvn -B -Dtest=DbConfigTest,DbMigrationTest test` passed 9/9
tests (8 configuration tests, 1 Flyway-configuration test) without connecting to
a database. Do not claim a command passed unless it was actually run.

## Observed conventions

- 2-space indentation; UTF-8; one public top-level type per file.
- `private static final Logger log = LoggerFactory.getLogger(X.class);`.
- One-line class Javadoc.
- No Service/DTO/Repository/Mapper/Factory/DI layer.
- README and UI strings are Vietnamese; code identifiers and comments are English.
- Tests use JUnit 5; `DbConfigTest` uses `@TempDir`/`Assumptions`, and
  `DbMigrationTest` uses a dynamic proxy `DataSource` to avoid a real connection.

## Known boundary deviation

`DbConfig.load` catches `IOException` and logs without rethrowing, which is an
intentional best-effort configuration load. No controller or DAO deviation is
current because those demo layers were removed.
