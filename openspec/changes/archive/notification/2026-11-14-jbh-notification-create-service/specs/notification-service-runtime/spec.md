## ADDED Requirements

### Requirement: Runnable service
The service SHALL run as its own application on port 7778, bound to all interfaces.

#### Scenario: Service starts
- **GIVEN** a reachable PostgreSQL database
- **WHEN** the `jbh-notification-app` application starts
- **THEN** it listens on port 7778 on `0.0.0.0`
- **AND** `GET /q/health` returns `UP`

### Requirement: Service discovery
The service SHALL register in Consul as `jbh-notification`, with health check path `/q/health`.

#### Scenario: Consul enabled
- **GIVEN** `CONSUL_ENABLED` is `true`
- **WHEN** the service starts
- **THEN** it registers with name `jbh-notification` and port 7778
- **AND** the health check calls `/q/health`

#### Scenario: Consul disabled
- **GIVEN** `CONSUL_ENABLED` is `false`
- **WHEN** the service starts
- **THEN** it does not call Consul

### Requirement: Observability
The service SHALL export metrics, JSON logs in prod, and traces when enabled, under the
application name `jbh-notification`.

#### Scenario: Metrics tags
- **GIVEN** the service runs
- **WHEN** `GET /q/metrics` is called
- **THEN** every meter has the tags `service="jbh-notification"` and `env`

#### Scenario: Prod logs
- **GIVEN** the `prod` profile is active
- **WHEN** the service logs a line
- **THEN** the line is JSON on the console

#### Scenario: Traces off by default
- **GIVEN** `OTEL_SDK_DISABLED` is not set and the profile is not `dev`
- **WHEN** the service handles a request
- **THEN** no trace is exported and no exporter error is logged

### Requirement: Container image
The repo SHALL provide a Dockerfile that runs the Quarkus fast-jar and exposes port 7778.

#### Scenario: Image content
- **GIVEN** `jbh-notification-app/target/quarkus-app/` exists
- **WHEN** the image is built from `jbh-notification-app`
- **THEN** the image runs `quarkus-run.jar` as a non-root user
- **AND** it exposes 7778
