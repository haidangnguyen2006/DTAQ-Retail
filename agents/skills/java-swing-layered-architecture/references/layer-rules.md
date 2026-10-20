# Layer Rules — DTAQ Retail

Expanded per-layer boundaries, the patterns this repository already uses, transaction
guidance, and anti-patterns. Read alongside `project-baseline.md` (verified facts) and
`review-checklist.md` (review mode).

## Mandatory flow

```
UI -> Controller -> DAO (interface) -> DAO implementation -> Entity -> PostgreSQL
```

`Entity` is a data/domain representation, not an access layer. The arrow is logical
data flow; entities do not call the database.

## UI layer (`ui` package)

- Allowed dependencies: `javax.swing.*`, `java.awt.*`, FlatLaf, MigLayout, controllers.
- Forbidden: `java.sql.*`, `javax.sql.*`, `com.zaxxer.hikari.*`, direct SQL, direct
  `DataSource` use, blocking database work on the EDT.
- Reuse existing components, dialogs, table models, renderers, and formatters.
- Event handlers should only forward to a controller; keep them thin.
- Update Swing components on the EDT.

## Controller layer (`controller` package)

- Allowed dependencies: DAO interfaces, entities, exception types, validation logic,
  `SwingWorker`/`SwingUtilities` at the boundary.
- Forbidden: concrete Swing component types (`JFrame`, `JDialog`, `JPanel`, `JTable`,
  `JLabel`, ...), `java.sql.*`, JDBC, DAO implementation classes when the interface
  suffices.
- Orchestrate use cases, validate input, call DAO contracts, and return results or
  errors using existing conventions.
- Known deviation: `MainController` takes a concrete `MainFrame`. Flag it, do not
  copy it, and do not refactor it unless the task covers it.

## DAO layer (`dao` package)

- Contracts only: domain-oriented method names (`findById`, `findAll`, `insert`).
- Return entities, `Optional` for a single row, `List` for many rows.
- Forbidden: `java.sql` types in signatures, exposing `Connection`/`ResultSet`, SQL.

## DAO implementation layer (`dao.impl` package)

- Perform JDBC: `PreparedStatement`, `ResultSet` mapping, generated keys.
- Constructor-inject `javax.sql.DataSource`; never create a pool here.
- Use try-with-resources for connections, statements, and result sets.
- Hold SQL in `private static final String` constants.
- Wrap `SQLException` in `DataAccessException`; never leak `SQLException` upward.
- No second connection mechanism or pool.

## Entity layer (`entity` package)

- Data only: immutable records such as `Product`.
- Forbidden: Swing, JDBC, SQL, lookups, or behavior owned by another layer.
- Reuse existing entities instead of duplicating shapes.

## Database layer (PostgreSQL)

- Inspect the real schema before writing SQL; the only in-repo evidence is the SQL in
  `ProductDaoImpl`, so the live database must be re-checked.
- Use parameterized SQL; never concatenate input.
- Preserve referential and data integrity.
- Follow existing transaction conventions.

## Patterns already used in this repository

- Constructor injection: `ProductDaoImpl(DataSource)` and `MainController(MainFrame, ProductDao)`.
- `Optional` for single rows; `List` for many rows.
- SQL as `private static final String` constants.
- `SwingWorker` with `doInBackground()` and `done()`; UI updates via `SwingUtilities.invokeLater`.
- try-with-resources on every JDBC resource.
- `DataAccessException` wrapping of `SQLException`.
- Immutable record entities.
- One-line class Javadoc and `private static final Logger`.

## Transaction guidance

For an atomic multi-statement operation:

1. Borrow one `Connection` from the shared `DataSource`.
2. `connection.setAutoCommit(false)`.
3. Execute the statements.
4. `commit()` on success; `rollback()` in the catch block.
5. Restore auto-commit (`setAutoCommit(true)`).
6. Close only that connection (try-with-resources).
7. Wrap JDBC failures in `DataAccessException`.

Do not hold a connection open across UI interactions or reuse a connection beyond the
operation.

## Schema-inspection commands

```bash
psql -c '\d products'
psql -c "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'products' ORDER BY ordinal_position;"
```

## Anti-patterns

- SQL or JDBC inside a UI class.
- JDBC or `DataSource` calls inside a controller.
- Swing types inside a DAO or entity.
- A `HikariDataSource` (or any pool) created per DAO method.
- String-concatenated SQL instead of `PreparedStatement` parameters.
- Silent catch blocks that drop exceptions.
- Logging the same exception at every layer.
- Inventing tables or columns not present in the live schema.
- Adding Service/DTO/Repository/Mapper/Factory/DI layers by default.
- Using `System.out.println` for application logging.
- Logging passwords, tokens, or connection details.
