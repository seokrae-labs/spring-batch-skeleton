# AGENTS.md

Guide for coding agents working in this repository. Read `README.md` for the design background.
If a task seems to require breaking a rule below, stop and ask the maintainer first.

## Where code goes

| You are adding | Put it in |
|---|---|
| Job, step, reader, processor, writer | the job group module it belongs to (`*-job-group`) |
| Connection settings for a business database | a `*-db-support` module, one per database |
| Something every job needs (job repository, launching) | `batch-support` |
| A way to start jobs (CLI, HTTP) | `admin-cli` or a new launcher module |

- Modules depend downward only: launcher → job group → `batch-support` / `*-db-support`.
- A job group never depends on another job group. Spring Boot is used by launcher modules only.
- `batch-support` knows nothing about business databases.
- Versions live in `gradle.properties` (Spring Boot) and `buildSrc` (shared dependencies). Do not write versions in module build files.
- Use `api` only for dependencies that callers must see; everything else is `implementation`.

## Registering jobs

- One `@Configuration` class per job, with the job name as a `public static final` constant.
- Add the class to the group's `*JobGroup` `@Import` list. Never rely on component scanning for jobs.
- The group's `*JobGroup` also imports the `*-db-support` configuration it uses.
- Declare required job parameters with `DefaultJobParametersValidator`.
- Job parameters carry per-run values only. Per-environment values go in the module's `.properties` file.

## Writing steps

- Inject beans through constructors. Field and setter injection are not allowed outside tests.
- Inject `DataSource` and `PlatformTransactionManager` with `@Qualifier` whenever more than one bean of that type exists, which is the case in every group that uses a business database.
- A step that writes to a business database must call `.transactionManager(...)` with that database's manager. Without it, Spring Batch silently uses a non-transactional one.
- `@StepScope` / `@JobScope` bean methods return the concrete class (e.g. `FlatFileItemReader<T>`), not the interface.
- Non-bean factory methods start with `build`.
- Keep conversion, filtering and validation in an `ItemProcessor` and test it on its own.
- Prefer Spring Batch's readers and writers over hand-written ones. Never collect a whole input into memory.
- SQL lives in a `*Sql` constants class and uses named parameters.

## Restart safety (important)

The job repository and the business database are different databases, so a chunk's metadata is committed before its business data. If the process dies between the two commits and the reader saved its position, a restart skips that chunk.

So every step that writes to a business database must be safe to rerun from the beginning:

- the reader uses `saveState(false)`;
- the job first clears what it is about to write, in a step with `allowStartIfComplete(true)`, or the writer is idempotent (upsert).

Ask before changing either of these.

## Ask, do not guess

Ask the maintainer when any of these is not given:

- chunk size;
- which exceptions to skip, the skip limit, and the retry policy;
- input file encoding and delimiter;
- cursor or paging reader for a database query.

Do not work around `JobInstanceAlreadyCompleteException` by changing parameters or deleting metadata. Report it instead. Never edit job repository tables by hand.

## Tests

- JUnit Jupiter and AssertJ. Every test method has a `@DisplayName` that states the expected behaviour.
- Cover failure paths and boundary values, not only the happy path.
- Job tests load the group's `*JobGroupBootstrap` with `@SpringJUnitConfig` and build parameters from `JobOperatorTestUtils.getUniqueJobParametersBuilder()`.
- Assert on the data the job wrote, not only on `BatchStatus.COMPLETED`.
- A step that must survive restarts gets a test that fails it on purpose and restarts it with the same parameters.
- In `@SpringBatchTest` classes, do not add helper methods that return `JobExecution` or `StepExecution`; the test listeners call such methods with no arguments.

## Checks

Claude Code runs `scripts/check-java.sh` after each file edit and `scripts/run-tests.sh` before finishing. When a check fails, fix the cause. Do not disable or bypass the checks.
