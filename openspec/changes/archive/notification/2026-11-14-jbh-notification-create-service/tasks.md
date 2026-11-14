# Tasks: jbh-notification-create-service (JBH-74)

No test blueprint group: the change adds no unit tests (D5, Test-First off).
Copy source: `../jbh-personal-finance/` (read only). Skip `target/` and `.DS_Store`.
Paths below are relative to this repo root.

## 1. Root project
Depends on: none

- [x] 1.1 Create `pom.xml`: `com.jbh:jbh-notification-parent:1.0.0-SNAPSHOT`, packaging `pom`,
  modules contracts, infra, app, Quarkus BOM, properties and plugin management (D1),
  `dependencyManagement` for commons-lib, contracts, infra, `distributionManagement` and
  profile `github` (D7).
- [x] 1.2 Copy `pmd-ruleset.xml` unchanged from finance.
- [x] 1.3 Create `.gitignore` (`target/`, `.env`, `.DS_Store`, `.idea/`).

## 2. Contracts module
Depends on: 1

- [x] 2.1 Copy `jbh-notification/jbh-notification-contracts/` to `jbh-notification-contracts/`
  (main, `module-info.java`, 10 tests, `README.md`). Java files unchanged.
- [x] 2.2 Change the `pom.xml` parent to `jbh-notification-parent`. Keep the artifact id.
- [x] 2.3 Fix paths and names in the contracts `README.md` that point to finance.

## 3. Infra module and preferences removal
Depends on: 1

- [x] 3.1 Copy `jbh-notification/jbh-notification-infra/` to `jbh-notification-infra/`, except
  `UserPreferencesPort`, `DirectPreferencesAdapter`, `DirectPreferencesAdapterTest`,
  `FakePreferencesLookupPort`.
- [x] 3.2 Infra `pom.xml`: parent `jbh-notification-parent`; remove `jbh-preferences-contracts`;
  `jbh-commons-lib` without `${project.version}` (managed version).
- [x] 3.3 `NotificationService`: remove `UserPreferencesPort` (import, field, constructor
  parameter) and `resolveUserLanguage`. Pass `DEFAULT_LANGUAGE` to `withLocaleIfAbsent` (D6).
  Remove unused imports. Keep each method under the complexity limits.
- [x] 3.4 Check: no `com.jbh.preferences` or `jbh-preferences` string in any `*.java` or `pom.xml`.
- [x] 3.5 Check (D12): `db/notificationdb-master.xml`, `db/changelog/001_create_notifications_schema.sql`
  and `application.properties` datasource, Liquibase and Hibernate keys equal finance's
  (use `diff`). Same paths, ids, authors.

## 4. App module
Depends on: 1

- [x] 4.1 Create `jbh-notification-app/pom.xml` (D3, D9): parent, quarkus plugin with
  `build`, `generate-code`, `generate-code-tests`, failsafe, extensions, dependency on
  `jbh-notification-infra`, test dependencies, `native` profile as finance.
- [x] 4.2 Create `BootstrapNotificationApp` and `module-info.java` (module `jbh.notification.app`).
- [x] 4.3 Copy `ConsulServiceRegistration` and `MetricsTagsConfig` to
  `com.jbh.notification.app.config`. Change package and the three defaults (name, port, tags).
  Confirm the service id includes the service name.
- [x] 4.4 Create `src/main/resources/application.properties` (D9).
- [x] 4.5 Copy the three tests to `src/test/java/com/jbh/notification/app/config/`. Change
  package and literals only (D5). Do not change a rule or an asserted key.
- [x] 4.6 Create `Dockerfile` (`EXPOSE 7778`) and copy `.dockerignore`.

## 5. CI
Depends on: 1

- [x] 5.1 Copy `.github/maven-settings.xml` from finance.
- [x] 5.2 Create `.github/workflows/publish.yml` (D8).
- [x] 5.3 Create `.github/workflows/image.yml` (D8).

## 6. Docs
Depends on: 4, 5

- [x] 6.1 Create `README.md`: purpose, modules, port 7778, env vars, build, run, image, contracts use.
- [x] 6.2 Write `CLAUDE.md`: keep the `## Pipeline config (kkpa-orchestrate-change)` block
  unchanged. Add conventions, build and run commands, and the "System map sync" rule (D10).

## 7. Verification (leader runs; the worker does not)
Depends on: 2, 3, 4, 5, 6

- [x] 7.1 Build and test: `mvn -B -Pgithub clean verify` (needs `jbh-commons-lib` in `~/.m2` or a read token).
- [x] 7.2 Database tests: `mvn -B -pl jbh-notification-infra -Djbh.test.excludedGroups=none test` (Docker needed).
- [x] 7.3 PMD: `mvn -q pmd:check`.
- [x] 7.4 Package: `mvn -B -Pgithub -pl jbh-notification-app -am -DskipTests package`, then check
  `jbh-notification-app/target/quarkus-app/quarkus-run.jar`.
- [x] 7.5 Run the checks of the design test contract (rows "Preferences removed", "Changelog identical").

Size estimate: 22 new or changed files, ~820 changed lines (tests and docs included); plus 61 files copied unchanged (about 3,000 lines moved without edit)
