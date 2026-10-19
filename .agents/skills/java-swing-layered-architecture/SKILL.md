---
name: Java Swing Layered Architecture
description: Guide implementation, refactoring, debugging, and review of the DTAQ Retail desktop app while preserving its UI to Controller to DAO to DAO implementation to Entity to PostgreSQL architecture and reusing the existing FlatLaf, MigLayout, HikariCP, PostgreSQL JDBC, SLF4J/Logback, and JUnit dependencies. Use this skill whenever a task touches DTAQ Retail Swing screens or dialogs, controllers, DAO interfaces or JDBC implementations, entities, database configuration or connections, SQL, the Maven build, or code or architecture review in this repository, even if the user does not mention layers, Swing, or JDBC.
---

# Java Swing Layered Architecture

## Purpose

This skill is a guardrail for the DTAQ Retail desktop application, not a redesign mandate. Keep the existing layered architecture intact and make the smallest change that satisfies the request. When rules conflict, apply this priority order:

1. Explicit user requirements.
2. Existing project behavior and conventions.
3. The mandatory layered architecture.
4. Dependency compatibility.
5. General best practices.

## When this applies

Use this skill when a task:

- Implements a new Swing screen, dialog, controller, DAO contract or JDBC implementation, entity, or database feature.
- Refactors or cleans up any code under `src/main/java/com/dtaq/retail` or `src/test`.
- Fixes a bug whose behavior crosses UI, controller, or persistence boundaries.
- Changes SQL, database configuration, connections, or the Maven build.
- Reviews code or architecture in this repository.

## The mandatory flow

```
UI -> Controller -> DAO (interface) -> DAO implementation -> Entity -> PostgreSQL
```

`Entity` is a data/domain representation, not an access layer. The arrow shows logical data flow through the layers, not that an entity calls the database: the DAO implementation performs JDBC and produces entities that flow back up to the UI.

## Layer boundaries

| Layer | May | Must not |
| :--- | :--- | :--- |
| UI | Build Swing components with FlatLaf and MigLayout; delegate to controllers; keep event handlers thin; update components on the EDT. | Contain SQL or JDBC; open or manage connections; perform blocking database work on the EDT. |
| Controller | Orchestrate use cases, validate input, call DAO contracts, return results or errors using the project's existing conventions. | Import concrete Swing component types (for example `JFrame`, `JDialog`, `JPanel`, `JTable`, `JLabel`); perform JDBC; depend on DAO implementation classes when the interface is enough. |
| DAO (interface) | Define contracts with domain-oriented names; return entities, `Optional` for single rows, and `List` for many rows. | Leak `java.sql` types, expose `ResultSet`/`Connection`, or contain SQL. |
| DAO implementation | Perform JDBC, `SQL`/`PreparedStatement`/`ResultSet` mapping; reuse the shared pool; use try-with-resources. | Introduce a second connection mechanism or pool; return JDBC types; leak persistence details upward. |
| Entity | Hold data only and reuse existing entities where possible. | Import Swing, JDBC, or SQL; perform lookups; contain behavior that belongs to another layer. |
| Database | Store PostgreSQL data; inspect the real schema before writing SQL; use parameterized SQL; preserve integrity; follow existing transaction conventions. | Invent tables or columns; bypass parameterization; break referential or data integrity. |

## Inspect before changing

Work in this order before editing:

1. Read `pom.xml` to confirm dependencies, versions, and the build.
2. Read the package map under `src/main/java/com/dtaq/retail` (see `references/project-baseline.md`).
3. Trace the existing `UI -> Controller -> DAO -> implementation -> entity -> DB` flow for the feature you touch.
4. Find reusable classes, methods, components, and constants before writing new ones.
5. Inspect schema evidence (`ProductDaoImpl` SQL and, if available, the live database).
6. State the plan for each affected file, including its layer responsibility.

## Dependency reuse

- Reuse FlatLaf (theme), MigLayout (layout), HikariCP (pool), PostgreSQL JDBC (driver), SLF4J/Logback (logging), and JUnit (tests).
- Do not add another theme, layout manager, pool, driver, logging framework, testing framework, ORM, or dependency-injection container without a concrete technical reason.
- Do not assume a dependency listed in `pom.xml` is used; confirm before relying on it.
- Do not upgrade versions or rewrite the build unless explicitly requested or strictly necessary, and explain the reason first.

## Database and connections

- Reuse the existing `DataSource` from `ConnectDB`; never create a `HikariDataSource` per DAO method or a second pool.
- Never close the shared `DataSource`.
- Never hardcode credentials; read them through `DbConfig`.
- Use `PreparedStatement` with bound parameters; never concatenate user input into SQL.
- Use try-with-resources for connections, statements, and result sets.
- When an operation must be atomic, borrow one `Connection`, `setAutoCommit(false)`, commit or rollback, restore auto-commit, and close only that connection.
- Wrap JDBC failures in the existing `DataAccessException`.
- Never invent tables or columns; verify against the live schema.

## UI and threading

- Apply FlatLaf consistently and use MigLayout when it fits the layout.
- Reuse existing components, dialogs, table models, renderers, and formatters.
- Event handlers should only forward to controllers.
- Run database work off the EDT (`SwingWorker` or the existing async abstraction) and update components on the EDT.
- Handle loading, empty, success, validation, and failure states.
- Do not introduce an additional UI framework.

## Logging and errors

- Log through SLF4J with the existing Logback configuration.
- Never use `System.out.println` for application logging.
- Never swallow exceptions; preserve diagnostic context.
- Show users a message without stack traces or credentials.
- Never log passwords, tokens, or connection details.
- Avoid logging the same exception at multiple layers.
- Follow existing exception types instead of inventing a global error architecture.

## Coding standards

- Target Java 21 and the existing packages.
- Apply SOLID where practical; keep classes and methods focused.
- Avoid duplication, magic values, giant UI classes, and monolithic controllers.
- Use constructor injection; avoid global mutable state and unnecessary statics.
- Preserve public APIs and behavior unless a breaking change is requested.
- Make no unrelated refactoring; comment non-obvious decisions.
- Never invent classes, methods, tables, columns, or libraries.
- Do not default to adding Service, DTO, Repository, Mapper, Factory, or DI layers.

## Required workflow

1. Inspect the relevant files and current behavior.
2. Trace the existing flow end to end.
3. Identify reusable code and dependencies.
4. Explain the intended changes and each affected file's responsibility.
5. Implement the smallest complete change.
6. Verify layer boundaries and resource handling.
7. Run the relevant `mvn validate`, `mvn test`, or `mvn package` when available.
8. Report actual results without claiming unexecuted tests passed.

## Architecture checklist

- [ ] UI contains no SQL or JDBC.
- [ ] UI delegates work to controllers.
- [ ] Controllers contain no Swing component types and no JDBC.
- [ ] DAO contracts are separated from persistence where appropriate.
- [ ] DAO implementations reuse the shared pool and existing utilities.
- [ ] Entities are independent of Swing and persistence.
- [ ] SQL matches the real schema.
- [ ] Existing libraries are reused.
- [ ] No unnecessary dependencies or layers were added.
- [ ] Swing threading and JDBC resources are handled correctly.
- [ ] Error handling and logging follow conventions.
- [ ] Test and build results are reported accurately.

If existing code violates a rule, explain it and propose the smallest safe correction; never rewrite the whole application.

## Review mode

When asked to review, report each finding as:

```
Severity | file:line | Issue | Required change
```

Prefer the smallest safe fix and cite the layer rule that the finding violates.

## References

- `references/project-baseline.md` — verified repo facts, versions, package map, and commands.
- `references/layer-rules.md` — expanded per-layer rules, patterns, anti-patterns, and transactions.
- `references/review-checklist.md` — review checklist and red flags.
