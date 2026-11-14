# jbh-notification

Notification service of the JBH system. It sends notifications (email today) for the other
services. Internal only: Caddy blocks `/jbh-api/notifications/**` in production, and `jbh-iam`
calls it through the gateway. See `README.md` for endpoint, env vars and run commands.

## Layout

Single-module repo with three Maven modules:

- `jbh-notification-contracts`: published library (GitHub Packages). Framework-agnostic.
- `jbh-notification-infra`: service code (REST, use cases, persistence, mailer, templates).
- `jbh-notification-app`: runnable Quarkus app. Port 7778, Consul name `jbh-notification`.

Stack: Java 21, Quarkus, PostgreSQL (database `jbh_finance`, schema `notifications`, shared
with `jbh-personal-finance`), Liquibase.

## Conventions

- Hexagonal architecture. Request and Response objects live in infra. DTOs are internal.
  Application code never depends on infrastructure.
- The standards live in `../jbh-personal-finance/.claude/standards/` (`code-quality.md`,
  `naming.md`) and `../jbh-personal-finance/.claude/architecture/hexagonal-layers.md`.
  Follow them. PMD runs with `pmd-ruleset.xml` and one violation fails the build.
- Support 2 or more app instances. Protect shared data in PostgreSQL, never with in-JVM locks.
- Write docs and comments in plain English. No HTML comments in markup.

## Rules

- Builds, tests and PMD are allowed, with quiet output (`mvn -q verify 2>&1 | tail -40`).
  Never start the service (`quarkus:dev`). Ask first before the database tests: they start Docker.
- Never commit `.env`.
- Liquibase: never rename or move a changelog file or changeset. History is keyed on file
  path, id and author.

## Build commands

`jbh-commons-lib` must be in `~/.m2` first.

- `mvn clean verify`: build, tests and PMD (add `-Pgithub` in CI).
- `mvn -q pmd:check`: PMD only.
- `mvn -pl jbh-notification-infra -Djbh.test.excludedGroups=none test`: database tests (Docker).
- `mvn -pl jbh-notification-app -am quarkus:dev`: run locally.

## System map sync (`../jbh-deploy/README.md`)

This repo is one part of the JBH system. The map of how the services talk to each other lives
in `../jbh-deploy/README.md` (the `jbh-deploy` repo, a sibling of this one).

**Rule:** when a change here affects the port, the Consul name, the `/jbh-api/notifications`
path, the contracts, the env vars, or the build order, update `../jbh-deploy/README.md` in
the same task. Change only the rows or lines that are affected. Then commit it in the
`jbh-deploy` repo (a separate commit there; never one commit across repos).

## Pipeline config (kkpa-orchestrate-change)

- **Modules-Root:** `..` (single-module repo: the module is this repo, `jbh-notification`.
  The Maven modules `jbh-notification-contracts`, `jbh-notification-infra` and
  `jbh-notification-app` are layers of that one module.)
- **Doc-Pack:** `CLAUDE.md`, `../jbh-personal-finance/.claude/standards/code-quality.md`,
  `../jbh-personal-finance/.claude/standards/naming.md`,
  `../jbh-personal-finance/.claude/architecture/hexagonal-layers.md`
- **Complexity-Check:** `mvn -q pmd:check` (uses `pmd-ruleset.xml`, copied from `jbh-personal-finance`)
- **Design-Gates:** `database`
- **Git-Strategy:** `auto`
