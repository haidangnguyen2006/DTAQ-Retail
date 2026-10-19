# Project Baseline — DTAQ Retail

Verified snapshot of the working tree on **2026-10-19**. Every fact below was read
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
| Logback Classic | 1.5.18 | runtime |
| Logback Core | 1.5.18 | runtime |
| JUnit Jupiter | 5.12.2 | test |

Plugins: maven-compiler-plugin 3.15.0 (`release` 21, UTF-8), maven-surefire-plugin
3.5.6 (`useModulePath=false`), maven-shade-plugin 3.6.2 (attaches the shaded jar in
the `package` phase, `ServicesResourceTransformer`, manifest main class).

## Package and file map (`src/main/java/com/dtaq/retail`)

| Package | File | Role |
| :--- | :--- | :--- |
| (root) | `Main` | Entry point; sets up FlatLaf, wires controller/DAO, registers shutdown hook. |
| `config` | `DbConfig` | Static configuration reader with layered precedence. |
| `config` | `ConnectDB` | Owns the single shared HikariCP pool. |
| `controller` | `MainController` | Loads products via the DAO and reflects status in the view. |
| `dao` | `ProductDao` | DAO contract for products. |
| `dao.impl` | `ProductDaoImpl` | Plain-JDBC implementation of `ProductDao`. |
| `entity` | `Product` | Immutable product record. |
| `exception` | `DataAccessException` | Unchecked persistence failure. |
| `ui` | `MainFrame` | Main window (JFrame, MigLayout). |
| `util` | `AppInfo` | App name/version and window centering helpers. |

Other resources: `src/main/resources/logback.xml` (console appender, JSON-free
pattern, `com.dtaq.retail` at DEBUG, root at INFO).

Tests: `src/test/java/com/dtaq/retail/config/DbConfigTest.java`,
`src/test/java/com/dtaq/retail/dao/ProductDaoImplTest.java`.

## Connection lifecycle (`config/ConnectDB`)

- One lazily-created shared `HikariDataSource`; never created per DAO call.
- Pool name `dtaq-retail-pool`; `initializationFailTimeout(-1)` so startup does not
  fail when the database is down.
- Exposed via `getDataSource()`; also `isAvailable()` and `close()`.
- Registers a JVM shutdown hook that closes the pool.
- `Main` registers its own shutdown hook calling `ConnectDB::close` as well.

## Configuration resolution (`config/DbConfig`)

- Precedence: system property > environment variable > `db.properties` > built-in default.
- `db.properties` is loaded lazily from the current working directory, never throws;
  missing file is logged at INFO, load failure at WARN.
- Keys: `DTAQ_DB_URL`, `DTAQ_DB_USER`, `DTAQ_DB_PASSWORD`, `DTAQ_DB_POOL_SIZE`.
- Defaults: URL `jdbc:postgresql://localhost:5432/dtaq_retail`, user `postgres`,
  empty password, pool size 5; unparsable pool size falls back to 5.
- Blank password logs a warning without printing the value.
- `setPropertiesFileForTest(Path)` is a package-private test seam.

## Persistence shapes

- `Product`: `record Product(long id, String name, BigDecimal price, int quantity)`;
  immutable, no JDBC or Swing.
- `DataAccessException`: unchecked, extends `RuntimeException`; constructors
  `(String)` and `(String, Throwable)`.
- `ProductDao`: `Optional<Product> findById(long)`, `List<Product> findAll()`,
  `long insert(Product)`; no `java.sql` types in the contract.
- `ProductDaoImpl`: constructor-injected `javax.sql.DataSource`; opens one
  `Connection` per operation; `PreparedStatement` + try-with-resources; maps
  `ResultSet` rows to `Product`; wraps `SQLException` in `DataAccessException`.
  SQL is held in `private static final String` constants.

## SQL evidence and schema warning

The table exists only as SQL text inside `ProductDaoImpl`; there is no migration or
DDL file in the repository.

```sql
SELECT id, name, price, quantity FROM products WHERE id = ?
SELECT id, name, price, quantity FROM products ORDER BY id
INSERT INTO products(name, price, quantity) VALUES (?,?,?)
```

Columns implied: `products(id, name, price, quantity)`. **Verify this against the
live database before writing new SQL**, for example:

```bash
# Connection flags must match your DbConfig settings (defaults shown).
psql -h localhost -U postgres -d dtaq_retail -c '\d products'
# or
psql -c "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'products' ORDER BY ordinal_position;"
```

## Build, run, and test commands

```bash
mvn validate
mvn test
mvn package
mvn -B clean package
java -jar target/dtaq-retail-1.0.0-SNAPSHOT.jar
```

Do not claim a command passed unless it was actually run.

## Observed conventions

- 2-space indentation; UTF-8; one public top-level type per file.
- `private static final Logger log = LoggerFactory.getLogger(X.class);`.
- One-line class Javadoc.
- SQL in `private static final String` constants.
- `Optional` for single rows; `List` for many rows.
- Entities as immutable records.
- No `org.postgresql.*` imports in main code; the driver is a runtime dependency reached through standard JDBC.
- No Service/DTO/Repository/Mapper/Factory/DI layer.
- README and UI strings are Vietnamese; code identifiers and comments are English.
- Tests use JUnit 5 with `@TempDir` and `Assumptions`; the DAO test self-skips when
  PostgreSQL is unreachable.

## Known boundary deviation

`MainController` depends on the concrete `MainFrame` type and uses `SwingWorker`
plus `SwingUtilities.invokeLater`, which is stricter than "controllers must not
depend on Swing components". Treat this as an existing exception to flag in review,
not as a pattern to copy, and do not refactor it unless the task explicitly covers
it. `DbConfig.load` also catches `IOException` and logs without rethrowing, which is
an intentional best-effort configuration load.
