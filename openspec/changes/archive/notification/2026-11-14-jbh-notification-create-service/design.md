# Design: create the jbh-notification service

Source ticket: JBH-74

## Context

The code to move is in `../jbh-personal-finance/jbh-notification/` (contracts, infra). The
app is modelled on `../jbh-personal-finance/jbh-z-assembly/`. This repo has no code yet.
`jbh-commons-lib` is built in its own repo (JBH-74 section 1).

## Decisions

**D1 Root POM.** `com.jbh:jbh-notification-parent:1.0.0-SNAPSHOT`, packaging `pom`, no parent.
Not named `jbh-notification`: finance still installs `com.jbh:jbh-notification` in `~/.m2`
until section 5. From finance's root `pom.xml` it copies only: the Quarkus BOM import,
the properties (Java 21, `quarkus.version` 3.33.4.1, plugin versions, `lombok.version`),
`pluginManagement` for compiler, surefire, failsafe, jandex, quarkus and PMD, and the PMD
plugin config with `pmd-ruleset.xml`. The enforcer config and the `quarkus-arc` and
`jbh-commons-lib` dependencies come from finance's `jbh-notification/pom.xml`.
Rejected: keep finance as parent (not standalone); copy SpotBugs (no ruleset files, not in
`Complexity-Check`); keep the `MODULE_NAME=module-a` quarkus plugin settings (dead config, dropped).
`dependencyManagement` also holds the versions of `jbh-commons-lib`, `jbh-notification-contracts`
and `jbh-notification-infra` (all `1.0.0-SNAPSHOT`).

**D2 Modules.** `jbh-notification-contracts`, `jbh-notification-infra`, `jbh-notification-app`.
Contract coordinates stay `com.jbh:jbh-notification-contracts:1.0.0-SNAPSHOT`.
Java packages stay `com.jbh.notification.*`. App packages: `com.jbh.notification.app`
and `com.jbh.notification.app.config`.

**D3 Observability.** As in finance: OpenTelemetry (SDK off unless `OTEL_SDK_DISABLED=false`),
JSON logs in prod only, Micrometer Prometheus at `/q/metrics`, `MetricsTagsConfig` with tags
`service` and `env`, SmallRye health at `/q/health`. `quarkus.application.name=jbh-notification`.
The app pom has the finance extensions: `quarkus-smallrye-health`,
`quarkus-micrometer-registry-prometheus`, `quarkus-logging-json`, `quarkus-opentelemetry`.

**D4 SOLID.** SRP only. No new interface, factory or abstract class.

**D5 Tests.** No new unit tests. Existing tests move with their code:
all contracts tests; infra tests except `DirectPreferencesAdapterTest` and
`FakePreferencesLookupPort` (deleted); the Testcontainers migration test with its resource
files; from `jbh-z-assembly`: `ConsulServiceRegistrationTest`, `MetricsTagsConfigTest`,
`ObservabilityConfigPropertiesTest`. The three app tests change only the package name and
the finance literals (`jbh-personal-finance` -> `jbh-notification`, `7777` -> `7778`,
tag `personal-finance` -> `notification`). Asserted keys and rules stay the same.

**D6 Language.** In `NotificationService.sendNotification`, the call
`resolveUserLanguage(notification.recipientId())` becomes the constant `DEFAULT_LANGUAGE`
(`"es"`), passed to `EmailMetadata.withLocaleIfAbsent`. A request `locale` wins. Delete
`resolveUserLanguage`, `UserPreferencesPort`, `DirectPreferencesAdapter`, the constructor
parameter and field `userPreferencesPort`, and the `jbh-preferences-contracts` dependency.
`NotificationService` keeps three constructor parameters. The code has no
`com.jbh.preferences` import. The `quarkus.index-dependency.preferences-*` lines are not copied.

**D7 jbh-commons-lib.** Normal dependency `com.jbh:jbh-commons-lib:1.0.0-SNAPSHOT`
(the infra pom used `${project.version}`; it now uses the managed version). Profile `github`
adds repository `github-packages-read`: `https://maven.pkg.github.com/kkpa-jbh/jbh-commons-lib`
with snapshots on. `distributionManagement`, id `github`:
`https://maven.pkg.github.com/kkpa-jbh/jbh-notification`.

**D8 CI.** `.github/maven-settings.xml` (copy of finance; servers `github` and
`github-packages-read`). `.github/workflows/publish.yml`: on push to `main` for paths
`jbh-notification-contracts/**` and `pom.xml`, plus `workflow_dispatch`. It runs Maven with
`-s .github/maven-settings.xml -Pgithub -pl jbh-notification-contracts -am -DskipTests` and the
goal `deploy`, with `GITHUB_TOKEN` and `PACKAGES_READ_TOKEN`. `-am` also publishes the parent POM.
`.github/workflows/image.yml`: `package` for `jbh-notification-app` with `-am`, builds
`jbh-notification-app/Dockerfile`, pushes `ghcr.io/kkpa-jbh/jbh-notification:<git-sha>` and
`:latest`, then deletes old image versions (keep 2), as finance does. This repo has no Maven
wrapper, so workflows call `mvn` (preinstalled on `ubuntu-latest`). No `ci.yml`.

**D9 App module.** `jbh-notification-app` copies from `jbh-z-assembly`:
`ConsulServiceRegistration` (defaults: name `jbh-notification`, port `7778`, tags
`quarkus,jbh,notification`), `MetricsTagsConfig`, a bootstrap class
`com.jbh.notification.app.BootstrapNotificationApp` (`@QuarkusMain`, log text "JBH Notification"),
`module-info.java` (module `jbh.notification.app`, same `requires`, `opens` for
`com.jbh.notification.app` and `.config`), `Dockerfile` (`EXPOSE 7778`, comments name
`jbh-notification` and `-pl jbh-notification-app`) and `.dockerignore`.
`application.properties` keys: `quarkus.http.port=7778`, `quarkus.http.host=0.0.0.0`,
`consul.*` as finance (`consul.service.name=jbh-notification`,
`consul.health-check.path=/q/health`), the observability block, log levels, `quarkus.arc.*`,
`quarkus.index-dependency.notification-contracts.*`, OpenAPI title `JBH Notification API`.
Dropped finance-only keys: uploads, products and preferences index-dependencies, and the
default (unnamed) datasource block. Infra defines only the named datasource `notifications`.
The app depends on `jbh-notification-infra` (which brings the contracts).

**D10 Docs.** `README.md`, full `CLAUDE.md`, `.gitignore` (`target/`, `.env`, `.DS_Store`,
`.idea/`). In `CLAUDE.md`, the block `## Pipeline config (kkpa-orchestrate-change)` stays
unchanged. New: conventions, build and run commands, and a "System map sync" rule (change a
port, route, dependency or contract here -> update `../jbh-deploy/README.md`).

**D11 Database gate: not touched (leader).** No DB object is created or altered. The
`notifications` datasource keeps its keys: `DATABASE_URL` default
`jdbc:postgresql://localhost:5432/jbh_finance`, `?currentSchema=notifications`,
`quarkus.liquibase.notifications.liquibase-schema-name=notifications`,
`migrate-at-start=true`, `schema-management.strategy=none`.

**D12 Liquibase history (leader).** The resources keep their exact classpath paths and content:
`db/notificationdb-master.xml` and `db/changelog/001_create_notifications_schema.sql` (changeset
ids and authors unchanged). Liquibase keys history on path, id and author. A new path would
re-run the changeset and fail, because the table exists. Check: `diff` of both files against
finance shows no difference, and the infra jar holds them at the same paths.

**D13 Transition (leader).** Until section 5, finance and this service may both start with the
same changelog on the same schema. See Concurrency.

## Test contract

No new unit tests (D5). Expected results:

| Check | Input | Expected |
|---|---|---|
| Language, request locale | `sendNotification` with metadata `locale="en"`, `recipientId` set | saved metadata `locale` = `en` |
| Language, no locale | metadata without `locale`; `recipientId` set or null | saved metadata `locale` = `es` |
| Subject (template) | locale `es`, template `TEAM_INVITATION` | subject from `email_es.properties` (unchanged) |
| Preferences removed | search `*.java` and `pom.xml` for `com.jbh.preferences` and `jbh-preferences` | no match |
| Constructor | `new NotificationService(repo, sender, templates)` | compiles; 3 parameters |
| Changelog identical | `diff` of both Liquibase files with finance | no difference |
| Migration test (moved) | empty PostgreSQL container, changelog run | schema `notifications` and its table exist (as the test asserts today) |
| Moved tests | `mvn test` | all pass; `DirectPreferencesAdapterTest` is gone |
| App tests | `ConsulServiceRegistrationTest`, `MetricsTagsConfigTest`, `ObservabilityConfigPropertiesTest` | pass with the new package and values |
| Package | `mvn -Pgithub -pl jbh-notification-app -am -DskipTests package` | `jbh-notification-app/target/quarkus-app/quarkus-run.jar` exists |
| Start | run the fast-jar with a reachable DB | `GET :7778/q/health` is `UP`; `/q/metrics` has tag `service="jbh-notification"` |
| PMD | `mvn -q pmd:check` | no violation |

The two language rows have no automated test (the system plan excludes unit tests). The
reviewer checks them in the diff; the user can check them with a manual call.

## Responsibilities

- `BootstrapNotificationApp` (new): starts Quarkus and waits for exit.
- `ConsulServiceRegistration` (copied): registers and deregisters the service in Consul.
- `MetricsTagsConfig` (copied): adds the tags `service` and `env` to every meter.
- `NotificationService` (existing, changed): loses one job (language lookup). The default
  language is a constant.
- All other moved classes: unchanged.

## Concurrency

- **Startup migration, finance and notification on the same schema (D13).** Task: group 3
  (infra, `migrate-at-start`) and group 4 (app). Risk: the risk exists only in the window
  between this service starting and the deploy of JBH-74 section 5. In that window finance
  runs without its notification module, so without the changelog. Liquibase's lock table
  `DATABASECHANGELOGLOCK` in schema `notifications` serializes two apps that start together.
  The second app waits, sees the changeset already ran (same path, id, author, checksum)
  and skips it. An edit to the changelog in only one repo during that window would stop one
  app at startup. No data loss. Decision (user): deferred. Section 5 closes the window. No
  guard and no note in this change.
- **Two instances of this service.** Task: group 3 and group 4. Risk: two instances run
  Liquibase at the same start. The Liquibase lock covers it. No extra guard. No in-JVM lock
  is added.
- **Status update after save** (`NotificationService`, existing): save, then update status by
  id. Not changed by this change. Risk: none new.
- **Consul registration.** The two services use distinct names and ports. The worker confirms
  that the service id in the copied class contains the service name. No other risk.
- **Email delivery during the transition.** No risk. The gateway routes
  `/jbh-api/notifications/**` to exactly one service at a time: finance before section 6,
  jbh-notification after. So one request cannot send twice.

## Risks and trade-offs

- A user who chose `en` in preferences now gets `es` unless the caller sends `locale`.
  JBH-74 accepts this (section 4 makes iam send the locale).
- The PMD ruleset path `/pmd-ruleset.xml` in the plugin config is copied from finance. The
  worker checks that `mvn -q pmd:check` finds it from the repo root.
- Surefire `useModulePath=true` in the parent, as in finance. The infra pom sets it to `false`.
- `jbh-commons-lib` does not exist in its repo yet (section 1). A local build needs it in `~/.m2`.
- Contracts inherit `quarkus-arc` and `jbh-commons-lib` from the parent, as in finance today.
  Not changed here.

## Files not copied

`Notification.mk` and the old `README.md` of finance's `jbh-notification/` (the new
`README.md` replaces them), `target/` and `.DS_Store`.
