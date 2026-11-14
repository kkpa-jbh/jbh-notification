# Proposal: create the jbh-notification service

Source ticket: JBH-74

## Why

Notification code lives inside the `jbh-personal-finance` monolith. JBH-74 splits the
system into services. This repo, `jbh-notification`, must build, run and ship on its own.
Other services (gateway client, iam) must read the contracts from this repo, not from finance.

## What changes

- New Maven multi-module project in this repo. Root: `com.jbh:jbh-notification-parent:1.0.0-SNAPSHOT`.
- Move `jbh-notification-contracts` and `jbh-notification-infra` here from
  `jbh-personal-finance/jbh-notification/`. Maven coordinates and Java packages stay.
- New runnable module `jbh-notification-app`: port 7778, Consul name `jbh-notification`,
  Dockerfile, observability (OpenTelemetry, JSON logs in prod, `/q/metrics`, `/q/health`).
- Remove the preferences call. A request `locale` wins; a missing locale becomes `es`.
  Delete `DirectPreferencesAdapter`, `UserPreferencesPort` and the `jbh-preferences-contracts`
  dependency.
- Depend on `com.jbh:jbh-commons-lib:1.0.0-SNAPSHOT` as an external package.
- Add CI: `.github/maven-settings.xml`, `publish.yml` (contracts), `image.yml` (container image).
- Add `README.md`, `CLAUDE.md` content and `.gitignore`.

## Capabilities

### New capabilities
- `notification-delivery`: how a notification is sent, and how its language is chosen.
- `notification-service-runtime`: how the service starts, registers and reports health and metrics.
- `notification-build-and-delivery`: how the contracts package and the image are built and published.

### Modified capabilities
None. The repo has no specs yet.

## Impact

- **Breaking:** no for callers. The HTTP API, the contracts and the database are unchanged.
  One behavior change: the language no longer comes from user preferences.
  A request without `locale` now always gets `es`, even if the user chose another language.
- **Services affected:** `jbh-personal-finance` still has its own copy until JBH-74 section 5.
  `jbh-gateway-client` and `jbh-iam` are changed later (sections 3 and 4).
- **Database:** no change. No table, column, index, constraint or migration is created or
  altered. The service reads and writes `jbh_finance.notifications.notifications`.
  Read paths touched: none changed. The only read path is the insert-then-update of
  status by notification id (existing).
- **Local installs:** finance keeps `com.jbh:jbh-notification` (pom) in `~/.m2`, so this repo uses
  the parent artifact `jbh-notification-parent`.

## Not included

- Gateway route to the new service (section 6).
- Gateway-client source change (section 3).
- iam locale (section 4).
- Removal of notification code from finance (section 5).
- Deploy and compose files (section 7).
- A `ci.yml` workflow.
- New unit tests. Existing tests move with their code.
