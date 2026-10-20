# AGENTS.md

# DTAQ — Project Instructions for AI Coding Agents
## link figma ( Dùng figma MCP : https://www.figma.com/design/kgJEXLuNJP1tvkMljWQ6Hl/DTAQ-%E2%80%A2-Prototype-qu%E1%BA%A3n-l%C3%BD-si%C3%AAu-th%E1%BB%8B-%C4%91i%E1%BB%87n-m%C3%A1y?node-id=3-375&t=UJZGeNUlAvcRxEWY-1 )
## 1. Project Overview

DTAQ is a desktop application for managing an electronics supermarket.

The application scope described in the project analysis and design report includes:

- Staff and customer management.
- Product, product-line and category management.
- Product and inventory lookup.
- Retail invoice creation and payment.
- Product returns and refunds.
- Defective-product management.
- Reports and related management functions.
- Backup and recovery requirements.

The approved project documentation and the existing repository are the primary references for all implementation decisions.

Do not assume that a feature is in scope merely because it is common in retail software.

## 2. Source of Truth and Documentation

The main analysis and design document is:

`v12_Nhom04_9_REPORT_ApplicationDevelopment.docx`

Locate the actual file path in the repository before reading it. Do not assume that the document is stored in a particular directory.

When working on a feature:

1. Identify its Use Case ID and exact title.
2. Read the corresponding Use Case specification.
3. Inspect related business rules, entities, class diagrams, database definitions and UI references.
4. Inspect the existing implementation before proposing changes.
5. Cross-check relevant details across these sources.
6. Report missing or conflicting information instead of inventing an answer.

If a Markdown documentation index exists, use it to locate the relevant sections. The original DOCX remains the reference for verifying extracted content.

The derived reference index is `docs/reference/README.md` (requirements, use cases, domain/entities, database/integrity, cross-feature rules, UI/screens, asset manifest). It is navigation aid only; the DOCX remains the source of truth, and current-code observations do not override documented requirements.

### Handling conflicting sources

Use the following process:

- Distinguish documented requirements from implemented behavior.
- Do not silently rewrite a documented requirement to match the current code.
- Do not assume that an old implementation is automatically correct.
- If the specification, database design, Figma prototype and source code disagree, explain the conflict and ask for clarification when it affects the requested feature.
- Do not claim to have inspected a document, diagram, Figma frame or source file unless it was actually accessible and inspected.

## 3. Scope Control

Implement only the feature explicitly requested by the user.

For collaborative development:

- Do not implement the entire application in a single task.
- Prefer one feature or one clearly bounded Use Case per task.
- Avoid unrelated refactoring and formatting changes.
- Do not overwrite work from other contributors.
- Do not change shared interfaces, database schemas, dependencies or architecture without establishing that the change is necessary.
- Ask before expanding the approved scope.

The project report excludes the following capabilities from its stated scope:

- E-commerce and online ordering.
- Online payments through cards, QR codes or digital wallets.
- Customer debt management.
- Installment sales.
- Multi-branch management.
- Deep supplier-management and human-resources functions.

Do not implement these capabilities unless the project requirements are explicitly revised.

## 4. Architecture and Existing Code

The project documentation identifies these architectural groups:

- `gui`: graphical user interface.
- `entity`: domain entities and data models.
- `dao`: data-access responsibilities.
- `connect`: database connection responsibilities.

These names describe the documented design. They do not guarantee that every package currently exists or that the source code follows the design exactly.

Before implementing a feature:

1. Inspect the existing package structure and representative classes.
2. Identify the actual path from the UI to data persistence.
3. Reuse established project conventions when they are consistent with the approved requirements.
4. Avoid introducing new layers, frameworks or architectural patterns without justification.
5. If the documented architecture and the implementation differ materially, report the difference before performing a broad refactor.

Keep responsibilities separated according to the architecture actually approved for the project.

- GUI code handles presentation and user interaction.
- Entity classes represent the relevant domain data.
- DAO-related code handles data access according to existing conventions.
- Database connection code follows the existing connection-management approach.
- Business rules must not be duplicated across multiple UI event handlers.

Do not place SQL queries directly in GUI event handlers when the project architecture provides a DAO layer.

## 5. Business Rules

The following rules are documented in the project report and must be checked whenever they apply to the requested feature.

### 5.1 Product and serial-number management

- The source contains a per-machine serial/status design and quantity-based stock wording in scope text; these descriptions conflict (C-07).
- `SAN_SANG_BAN` is a source status; its unit-level applicability (per machine or per quantity) is unresolved (C-07). Preserve the documented status term.
- Do not validate or implement a chosen stock granularity (per-machine or quantity-based) until C-07 is resolved.

**Unresolved (C-07):** the source is not consistent on whether stock is tracked per serial or per quantity (scope/FR text uses quantity, while UC descriptions and the design use per-serial `SanPham`). Do not assume either model; confirm before implementing, per `docs/reference/requirements/scope-and-requirements.md` → C-07.

Do not invent additional statuses or lifecycle transitions without consulting the specification.

### 5.2 Retail invoices and payment

- The documented payment scope is cash.
- An invoice is considered complete only after full payment.
- Invoice totals must use a single, consistent rounding rule; the concrete method, unit and order are unresolved (C-11).
- Historical transaction amounts must preserve the price, tax and promotion values applicable at the time of the transaction.

**Unresolved (C-11):** the source requires consistency but does not define the concrete rounding rule, unit or order of rounding. Do not invent one; confirm before implementing (see `docs/reference/requirements/scope-and-requirements.md` → C-11).

**Unresolved (C-04):** promotion scope is contradictory — Chapter 1 excludes a complete promotion subsystem, while UC012 and the UC003 flow describe promotion behaviour. Do not implement promotion behaviour without confirmation (see [`docs/reference/requirements/scope-and-requirements.md` → C-04](docs/reference/requirements/scope-and-requirements.md#c-04--khuyến-mãi-vừa-ngoài-phạm-vi-vừa-có-use-case); and do not recalculate historical transactions using current promotions).

Do not recalculate historical transactions using current product prices, tax settings or promotions.

### 5.3 Returns and refunds

- Apply the documented return and refund workflow.
- Manager approval is required when the refund amount is greater than or equal to 3,000,000 VND.
- Preserve the distinction between a return request, its approval and the resulting transaction.
- Do not bypass the approval requirement in either the GUI or the data-access workflow.

Consult the full Use Case specification for other eligibility conditions, statuses and exceptional flows.

### 5.4 Customer management

- Customer lookup may use a phone number.
- Purchase history must follow the documented relationship between customers and transactions.
- Do not expose or modify unrelated customer data when implementing a narrowly scoped feature.

### 5.5 Identifiers

The report describes identifiers using a prefix, year and sequence convention, with examples such as:

- `NVQUANLY-YYYY-NNNNNN`
- `NVBANHANG-YYYY-NNNNNN`

Treat these as documented examples, not as a reason to invent formats for every other entity.

- Identifiers are generated by the **application layer** when a record is successfully saved.
- Do not rely on a PostgreSQL sequence, identity column or default to generate an identifier; V1 contains no database generator.
- Use only the documented identifier formats and retain prefix/year/sequence conventions where the source defines them.
- Identifiers are immutable.
- Identifiers must not be reused.
- Before generating an identifier, inspect the existing implementation and the relevant entity/database specification.
- The DOCX also contains manual identifier-entry steps in some use cases. Those steps remain source evidence and are not rewritten; the source disagreement is retained historically, while the current implementation decision is application-generated IDs.

### 5.6 Currency and calculations

- Use Vietnamese currency conventions consistently.
- Apply one consistent rounding rule across UI, storage and reports; the concrete rule is unresolved (C-11).
- Invoice totals must be consistent with the sum of rounded detail amounts.
- Use the existing project approach for monetary values and database precision.
- Do not introduce inconsistent rounding behavior between the UI and persistence layer.

**No-invention guard (C-11):** do not choose the rounding method, unit or order on your own; confirm it before implementing.

## 6. UI and Figma

Use the available Figma theme and prototype screens as design references when accessible.

For each requested screen:

1. Inspect the corresponding Figma frame or a representative screen.
2. Reuse established colors, typography, spacing, component styles and interaction patterns.
3. Cross-check screen behavior against the relevant Use Case.
4. Follow the existing UI implementation conventions.
5. Avoid redesigning unrelated screens.

If no prototype exists for a requested screen, use the established design system and the closest relevant screen as references.

Do not claim that Figma was inspected if its content was unavailable through the configured tools. If important visual or interaction requirements cannot be verified, ask for the relevant frame, screenshot or clarification.

## 7. Collaborative Git Workflow

Before making changes:

1. Run `git status --short --branch`.
2. Inspect the current branch and working-tree changes.
3. Review the relevant files and recent changes when needed.
4. Identify files that may belong to another contributor's unfinished work.

During implementation:

- Modify only files required by the approved feature.
- Preserve unrelated changes.
- Do not use destructive Git commands to clean up the working tree.
- Do not reset, checkout over, revert or overwrite another contributor's work.
- Do not change branches or perform history rewriting without explicit authorization.

After implementation:

- Review `git diff --check`.
- Review `git diff` and the final changed-file list.
- Report build and test results accurately.
- Do not commit, push, merge or open a pull request unless explicitly requested.

## 8. Implementation Workflow

For every feature, follow these stages.

### Stage A — Understand

- Identify the exact requested feature and its Use Case.
- Read the relevant specification and business rules.
- Inspect existing code and related UI/data-access classes.
- Identify dependencies and potential shared-code impacts.

### Stage B — Plan

Before broad or risky edits, provide a concise implementation plan containing:

- Files to create or modify.
- Responsibilities of each change.
- Relevant business rules.
- Validation and error-handling behavior.
- Tests to add or update.
- Risks, assumptions and unresolved questions.

Do not begin a large implementation while important business requirements remain ambiguous.

### Stage C — Implement

- Keep changes limited to the approved feature.
- Follow existing naming, formatting and package conventions.
- Reuse existing utilities and components where appropriate.
- Avoid unnecessary dependencies.
- Preserve compatibility with other features and contributors' work.

### Stage D — Verify

- Run the relevant build and tests using the project's existing tooling.
- Check validation, exceptional flows and business-rule enforcement.
- Review the diff for unrelated modifications.
- Report any tests that could not be run and explain why.

### Stage E — Report

Summarize:

- What was implemented.
- Which files changed.
- Which Use Case and business rules were addressed.
- Which tests/build checks were executed and their results.
- Remaining issues or decisions requiring approval.

## 9. Java and Swing Guidelines

Apply these guidelines when the relevant code uses Java Swing.

- Follow the Java version and dependency versions declared in the repository's build configuration.
- Keep Swing UI updates on the Event Dispatch Thread (EDT).
- Avoid long-running database operations on the EDT.
- Use the project's existing database connection and resource-management conventions.
- Use prepared statements for SQL parameters where JDBC is used.
- Close database resources safely.
- Validate user input before persisting data.
- Provide understandable error messages without exposing credentials or internal secrets.
- Keep business validation consistent across entry points; do not rely solely on UI validation.

Do not assume a particular Java version, database engine, dependency or build command until verified in the repository.

## 10. Security and Data Integrity

- Never hard-code credentials, passwords, API keys or tokens.
- Never print secrets in logs or error messages.
- Use the existing configuration and secret-management conventions.
- Validate data at the appropriate application and persistence boundaries.
- Preserve referential integrity and transaction consistency.
- Avoid partial invoice, payment, return or refund updates.
- Use database transactions when a business operation requires multiple changes to succeed or fail together.
- Do not weaken authentication, authorization or approval requirements to make a feature easier to implement.

## 11. Testing

For a feature, consider tests for:

- Normal success cases.
- Invalid or missing input.
- Boundary conditions.
- Business-rule violations.
- Authorization and approval requirements where applicable.
- Database constraints and transaction consistency.
- Regression risks to related Use Cases.

Use the test framework already present in the repository. Do not introduce a new testing framework without a clear need and approval.

## 12. Documentation Maintenance

When a change affects documented behavior:

- Identify which requirements or design documents may need updating.
- Do not silently modify the original analysis and design report.
- Keep derived Markdown documentation traceable to the source.
- Mark proposed changes as proposals until approved.
- Avoid duplicating large sections of the same specification across multiple files.

## 13. Communication

Communicate in Vietnamese unless the user requests another language.

Explain important technical decisions clearly, especially when they affect business rules, architecture, shared files or data integrity.

If a requirement is unclear, ask a focused question rather than inventing a business decision.

Prefer a small, reviewable and verifiable change over a large, speculative implementation.
