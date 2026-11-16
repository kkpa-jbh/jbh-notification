# Tasks: jbh-notification-add-otel-export-histograms

Test-First is off: the user excluded unit tests for the JBH-55 plan. No test blueprint group.

## 1. Latency histogram filter

Depends on: none. Files: `MetricsTagsConfig.java` only.

- [ ] 1.1 In `jbh-notification-app/src/main/java/com/jbh/notification/app/config/MetricsTagsConfig.java`,
  add `@Produces @Singleton MeterFilter httpServerLatency()` (design D4): exact match on
  `http.server.requests`, SLOs from one constant (5 ms, 10 ms, 25 ms, 50 ms, 100 ms, 250 ms,
  500 ms, 1 s, 2.5 s, 5 s, 10 s) in nanoseconds, `merge(config)`; other meters return
  `config` unchanged. Keep the constructor and `commonTags()` as they are. Follow PMD traps
  (final params, no literals in `if`, constants named).
- [ ] 1.2 Update the class Javadoc: "Micrometer meter filters for this app" (common tags and
  HTTP server latency buckets).

## 2. Properties

Depends on: none. Files: app `application.properties` only.

- [ ] 2.1 Apply D1 in the OBSERVABILITY block: `quarkus.otel.logs.enabled=true` without
  `%dev.`; one `quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}`
  line (remove the `%dev` and `%prod` endpoint lines); replace both `sdk.disabled` lines by
  the export switch of D8 (exact lines and values there, with the plain-English comment block
  above them); keep `quarkus.otel.traces.sampler.arg`.
- [ ] 2.2 `jbh.metrics.env=${JBH_ENV:prod}`, keep `%dev` and `%test` overrides; add
  `quarkus.otel.resource.attributes=service.namespace=jbh,deployment.environment.name=${jbh.metrics.env}`
  (D2). No `service.version` var (D3).
- [ ] 2.3 Fix the comment above `quarkus.log.console.json.mdc.flat-fields`: no `trackingId`
  (not used in this repo); say "MDC entries such as traceId and spanId".
- [ ] 2.4 Keep JSON console in prod and all other lines unchanged.

## 3. Keep the existing property test passing

Depends on: 2.

- [ ] 3.1 Update `jbh-notification-app/src/test/java/com/jbh/notification/app/config/ObservabilityConfigPropertiesTest.java`
  as in design D7 (7 methods, one per row of the table). Change no other test. Add no new test method. Rename a
  method only when its old name says something false.

## 4. Docs

Depends on: 2 (values must match the properties).

- [ ] 4.1 `README.md`: new "Observability" section: signals (metrics at `/q/metrics` with tags
  `service` and `env`; HTTP latency buckets; traces and logs over OTLP, off by default),
  env vars (`OTEL_SDK_DISABLED`, `OTEL_EXPORTER_OTLP_ENDPOINT` default
  `http://localhost:14317` for all profiles, `JBH_ENV` default `prod`, sampler vars), the
  rule "no recipient email, email body, invitation token or Gmail credential in logs or span
  attributes", and the local check (`curl` of the bucket `le` values and
  `curl -s localhost:7778/q/metrics | grep -vc '^#'` for the series count). Fix the env var
  table rows: `OTEL_SDK_DISABLED` now means "stop only the OTLP export; tracing stays on"
  (default `true`, `false` in dev; values are case-sensitive `true`/`false`, a bad value means
  off); `OTEL_EXPORTER_OTLP_ENDPOINT`; add `JBH_ENV`. Note the known limit of D8 (outgoing
  `traceparent` has sampled=0 with export off).
- [ ] 4.2 `CLAUDE.md`: short "Observability" section (3-5 lines): where the config is, the
  sensitive-data rule, pointer to README for the env vars and local check.

## Dependencies overview

1 and 2 run in parallel (disjoint files). 3 and 4 run in parallel after 2 (disjoint files).

Size estimate: 5 files, ~170 changed lines (tests and docs included)
