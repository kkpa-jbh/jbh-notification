## MODIFIED Requirements

### Requirement: Observability
The service SHALL export metrics, JSON logs in prod, and traces and logs over OTLP when
export is on (`OTEL_SDK_DISABLED=false`; default off). `OTEL_SDK_DISABLED` stops only the OTLP
export: tracing stays on, under the application name `jbh-notification`. HTTP server latency SHALL be
published as a histogram with fixed buckets. Telemetry SHALL NOT carry recipient emails,
email bodies, invitation tokens or Gmail credentials.

#### Scenario: Metrics tags
- **GIVEN** the service runs
- **WHEN** `GET /q/metrics` is called
- **THEN** every meter has the tags `service="jbh-notification"` and `env`

#### Scenario: Env tag from JBH_ENV
- **GIVEN** `JBH_ENV` is `local` and the profile is `prod`
- **WHEN** `GET /q/metrics` is called
- **THEN** every meter has the tag `env="local"`
- **AND** with `JBH_ENV` not set, the tag is `env="prod"`

#### Scenario: HTTP latency buckets
- **GIVEN** the service handled at least one HTTP request
- **WHEN** `GET /q/metrics` is called
- **THEN** `http_server_requests_seconds_bucket` has the `le` values 0.005, 0.01, 0.025,
  0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0 and 10.0, plus `+Inf`

#### Scenario: Other meters keep default buckets
- **GIVEN** the service runs
- **WHEN** `GET /q/metrics` is called
- **THEN** no meter other than `http.server.requests` gets these buckets from the service

#### Scenario: Prod logs
- **GIVEN** the `prod` profile is active
- **WHEN** the service logs a line
- **THEN** the line is JSON on the console

#### Scenario: Export off by default
- **GIVEN** `OTEL_SDK_DISABLED` is not set and the profile is not `dev`
- **WHEN** the service handles a request
- **THEN** no trace or log is sent over OTLP and no exporter error is logged
- **AND** tracing stays on: log lines carry `traceId` and `spanId`
- **AND** an incoming `traceparent` is read and passed on (with sampled=0)

#### Scenario: Export on
- **GIVEN** `OTEL_SDK_DISABLED` is `false`
- **WHEN** the service handles a request
- **THEN** traces and logs are sent over OTLP
- **AND** the sampler is `OTEL_TRACES_SAMPLER` (default `parentbased_traceidratio`)

#### Scenario: Bad switch value
- **GIVEN** `OTEL_SDK_DISABLED` is `1` or `TRUE` (not exactly `true` or `false`)
- **WHEN** the service starts
- **THEN** it starts and export is off

#### Scenario: One endpoint for every profile
- **GIVEN** export is on (`OTEL_SDK_DISABLED` is `false`)
- **WHEN** the service starts in any profile
- **THEN** it exports over OTLP to `OTEL_EXPORTER_OTLP_ENDPOINT`
- **AND** with the variable not set, the endpoint is `http://localhost:14317`

#### Scenario: Logs exported in every profile
- **GIVEN** export is on
- **WHEN** the service logs a line, in any profile
- **THEN** the line is exported over OTLP and linked to its trace

#### Scenario: Resource attributes
- **GIVEN** export is on and `JBH_ENV` is `local`
- **WHEN** the service exports telemetry
- **THEN** the resource has `service.namespace=jbh` and `deployment.environment.name=local`
- **AND** it has `service.name=jbh-notification`

#### Scenario: Incoming trace is joined
- **GIVEN** export is on
- **AND** a request arrives with a valid W3C `traceparent` header (for example from `jbh-iam`
  through the gateway)
- **WHEN** the service handles the request
- **THEN** its server span has the trace id of the caller and the caller span as parent

#### Scenario: No sensitive data in telemetry
- **GIVEN** a notification is sent to an email recipient with a body and an invitation token
- **WHEN** logs and spans are exported
- **THEN** no log line or span attribute contains the recipient email, the email body, the
  invitation token or a Gmail credential
