# Proposal: jbh-notification-add-otel-export-histograms

Source ticket: JBH-82 - https://linear.app/kkpa/issue/JBH-82

## Why

Part of the JBH-55 plan (section 6). The service must be ready to send logs, traces and
metrics to the central collector (Alloy) in every profile, and it must publish latency
buckets for HTTP requests. Today:

- OTLP logs are on only in `dev`.
- The OTLP endpoint is set twice (`%dev` and `%prod`), with a different port than the shared
  dev default (`14317`).
- The metric `http.server.requests` has no histogram buckets, so p95 and p99 cannot be
  computed in Grafana.
- The docs have no Observability section.

## What changes

- `application.properties` (app module): `OTEL_SDK_DISABLED` stops only the OTLP export
  (tracing stays on in every profile, via a config lookup, no Java); OTLP logs on in every profile; one OTLP endpoint
  line for all profiles; `jbh.metrics.env` from `JBH_ENV`; resource attributes
  `service.namespace` and `deployment.environment.name`.
- `MetricsTagsConfig`: a second `MeterFilter` that sets 11 latency buckets (5 ms to 10 s) on
  `http.server.requests` only.
- `README.md` and `CLAUDE.md`: new "Observability" section (signals, env vars, local check).
- Spec delta: the `Observability` requirement of `notification-service-runtime` gets new
  scenarios.
- Check, no code change expected: an incoming W3C trace from `jbh-iam` through the gateway
  is joined, and no recipient email, email body, invitation token or Gmail credential
  reaches logs or span attributes. The result is in design.md.

## Impact

- Breaking: no. No API, contract or database change.
- Services affected: only `jbh-notification`. Prod export stays OFF by default
  (`OTEL_SDK_DISABLED` default `true`) until JBH-84 sets the prod env vars. Tracing and trace
  ids in logs are on in every profile.
- Database: none. Hot read paths: none. The metric series count of `/q/metrics` grows
  (11 extra buckets per `http.server.requests` tag set).
- New dependency: none.
- Existing test `ObservabilityConfigPropertiesTest` must change, because it guards the old
  property values.
