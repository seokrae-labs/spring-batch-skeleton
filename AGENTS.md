# AGENTS.md

Project rules for coding agents. `README.md` explains the design behind them.
If a task needs one of these rules to change, ask the maintainer before changing code.

## Modules

Module names follow `batch-<role>-<name>`. The roles sort alphabetically in dependency order, bottom layer first.

| Module | Holds | May depend on |
|---|---|---|
| `batch-core` | job repository database, `JobService` | nothing in this project |
| `batch-db-<name>` | one business database: `<Name>DbConfig`, `<name>DataSource`, `<name>TransactionManager` | nothing in this project |
| `batch-job-<name>` | jobs of one group: `<Name>JobGroup`, `<Name>JobGroupBootstrap`, readers, processors, writers | `batch-core`, the `batch-db-*` modules it uses |
| `batch-launcher-<name>` | a way to start jobs: `<Name>LauncherApplication` | `batch-job-*` modules |

- Packages are `com.example.batch.<role>.<name>` (e.g. `com.example.batch.db.main`). Property files and keys are `<role>-<name>.properties` / `<role>-<name>.*` (`core.*` for `batch-core`).
- Job group modules never depend on each other.
- Spring Boot appears only in `batch-launcher-cli`.
- The Spring Boot version is set once in `gradle/libs.versions.toml`; shared dependencies are declared once in `buildSrc`. Module build files never contain versions.

## Adding a job

1. Create `XxxJobConfig` in the job group it belongs to.
2. Add it to that group's `*JobGroup` `@Import` list. Jobs are not discovered by component scanning.
3. If the job writes to a business database, follow "Restart safety" below.
4. Add a test that runs the job through the group's `*JobGroupBootstrap` and checks the rows it wrote.

A new business database gets its own `batch-db-*` module, imported by the `*JobGroup` of every group that uses it.

## Two databases

Every group that uses a business database has two `DataSource` beans (`jobMetaDataSource`, `mainDataSource`) and two transaction managers. Always inject them by name with `@Qualifier`.

A step that writes to the business database must set `.transactionManager(...)` to that database's manager. If it is left out, the step runs without a transaction and nothing reports it.

## Restart safety

Job metadata is committed to the job repository before the chunk's business data is committed. If the process dies between those commits and the reader saved its position, a restart skips that chunk.

Steps that write to a business database are therefore rerun from the start on restart:

- the reader uses `saveState(false)`;
- the job first removes what it is about to write, in a step with `allowStartIfComplete(true)` (see `clearTargetDateStep`), or the writer is an upsert.

`SampleJobTest` proves this behaviour. Keep an equivalent restart test for every such job.

## Tests

- `scripts/check-java.sh` rejects `@Autowired`, `@Inject` and `@Resource` in main code (use constructor injection) and test methods without `@DisplayName`.
- In `@SpringBatchTest` classes, do not write helper methods that return `JobExecution` or `StepExecution`. The test listeners mistake them for execution factories and call them with no arguments.

## Checks

Claude Code runs `scripts/check-java.sh` after each edit and `scripts/run-tests.sh` before finishing. Fix what they report; do not disable them.
