## ADDED Requirements

### Requirement: Standalone build
The repo SHALL build without `jbh-personal-finance`. It SHALL read `jbh-commons-lib`
`1.0.0-SNAPSHOT` as an external package.

#### Scenario: Build in CI
- **GIVEN** the profile `github` and a token that can read `kkpa-jbh/jbh-commons-lib`
- **WHEN** Maven builds the root project
- **THEN** the modules contracts, infra and app build
- **AND** no artifact of `jbh-personal-finance` is needed

#### Scenario: Parent name does not clash
- **GIVEN** `~/.m2` holds `com.jbh:jbh-notification` from finance
- **WHEN** this repo is installed
- **THEN** it installs `com.jbh:jbh-notification-parent` and does not replace that artifact

### Requirement: Contracts publishing
The repo SHALL publish `com.jbh:jbh-notification-contracts:1.0.0-SNAPSHOT`, and its parent POM,
to `https://maven.pkg.github.com/kkpa-jbh/jbh-notification`.

#### Scenario: Contracts change on main
- **GIVEN** a push to `main` that changes `jbh-notification-contracts/**` or the root `pom.xml`
- **WHEN** the publish workflow runs
- **THEN** the contracts and the parent POM are published
- **AND** the coordinates of the contracts are unchanged

### Requirement: Image publishing
The repo SHALL push the service image to GHCR on every push to `main`.

#### Scenario: Image pushed
- **GIVEN** a push to `main`
- **WHEN** the image workflow runs
- **THEN** it pushes `ghcr.io/kkpa-jbh/jbh-notification:<git-sha>` and `ghcr.io/kkpa-jbh/jbh-notification:latest`
- **AND** it keeps only the 2 newest image versions

### Requirement: Repository documents
The repo SHALL have a `README.md`, a `CLAUDE.md` with a "System map sync" rule, and a `.gitignore`.

#### Scenario: System map sync
- **GIVEN** a change to the port, route, dependency or contract of this service
- **WHEN** the change is made
- **THEN** `CLAUDE.md` tells the author to update `../jbh-deploy/README.md`
