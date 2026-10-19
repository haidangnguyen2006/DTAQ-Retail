# Review Checklist — DTAQ Retail

Use this when reviewing a diff or a file in this repository. Verify each item against
the actual change, not against intent.

## Architecture checklist

1. **UI contains no SQL or JDBC.** Check `ui` files for `java.sql.*`, `javax.sql.*`,
   `com.zaxxer.hikari.*`, SQL strings, or `DataSource` use.
2. **UI delegates to controllers.** Event handlers should forward to a controller and
   stay thin, not embed business logic.
3. **Controllers contain no Swing component types and no JDBC.** Check imports for
   `JFrame`, `JDialog`, `JPanel`, `JTable`, `JLabel`, and `java.sql`/`javax.sql`.
   (`MainController`'s use of `MainFrame` is a known deviation — flag, do not copy.)
4. **DAO contracts are separated from persistence where appropriate.** Interfaces
   return entities and collections, not JDBC types.
5. **DAO implementations reuse the shared pool and existing utilities.** No new
   `HikariDataSource`; injected `DataSource`; try-with-resources.
6. **Entities are independent of Swing and persistence.** Data-only immutable records.
7. **SQL matches the real schema.** Confirm table and column names against the live
   database, not only against existing SQL text.
8. **Existing libraries are reused.** FlatLaf, MigLayout, HikariCP, PostgreSQL JDBC,
   SLF4J/Logback, JUnit.
9. **No unnecessary dependencies or layers.** No new theme, layout manager, pool,
   driver, logging framework, test framework, ORM, or DI container without a
   concrete, stated reason.
10. **Swing threading and JDBC resources are correct.** Database work off the EDT;
    UI updates on the EDT; every JDBC resource closed via try-with-resources.
11. **Error handling and logging follow conventions.** `DataAccessException` wrapping;
    SLF4J logging; no silent catches; no duplicate per-layer logging; no secrets.
12. **Test and build results are reported accurately.** Never claim a command passed
    unless it was run.

## Findings format

Report each finding as:

```
Severity | file:line | Issue | Required change
```

- Severity: `Critical` (breaks correctness, leaks secrets, deadlocks UI), `Major`
  (layer violation, resource leak), `Minor` (convention drift), `Info`.
- `file:line` points to the exact location.
- Issue states what is wrong and which layer rule it violates.
- Required change is the smallest safe correction.

## Red flags

- `java.sql` / `javax.sql` imports outside `dao.impl` (and the config layer's pool).
- `new HikariDataSource(...)` anywhere except `ConnectDB`.
- Closing the shared `DataSource` outside the shutdown path.
- String-concatenated SQL.
- Hardcoded credentials or logged connection details.
- `catch (Exception e) {}` or catches that only `printStackTrace()`.
- `System.out.println` used as application logging.
- Database calls on the EDT.
- Swing imports in `dao`, `dao.impl`, or `entity`.
- New `Service`/`DTO`/`Repository`/`Mapper`/`Factory` packages added by default.
- Tables or columns referenced in SQL but absent from the live schema.

## Posture

- Prefer the smallest safe correction. Do not rewrite the whole application.
- Preserve public APIs and existing behavior unless a breaking change is requested.
- If existing code violates a rule, explain the violation and propose the minimal fix.
- Keep changes surgical and consistent with surrounding style (2-space indent, UTF-8,
  English identifiers).

## Reporting unrun commands

- Run only the commands you can actually run, and report their real output.
- If a command cannot run for environment reasons, say so explicitly.
- Never state that tests, builds, or validations passed when they were not executed.
