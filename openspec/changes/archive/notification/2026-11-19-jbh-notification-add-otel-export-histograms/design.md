# Design: jbh-notification-add-otel-export-histograms

Source ticket: JBH-82 (part of JBH-55, section 6).

## Context

Quarkus app, Micrometer (Prometheus registry) and `quarkus-opentelemetry` are already in
`jbh-notification-app/pom.xml`. No dependency is added. The collector is Alloy. In Docker the
endpoint is `http://jbh-alloy:4317`. In the IDE it is `http://localhost:14317` (Alloy dev
port; the local LGTM keeps 4317). `jbh-deploy/compose.local.yaml` already sets
`OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SDK_DISABLED=false` and `JBH_ENV=local`.

## Responsibilities

- `MetricsTagsConfig.java` (existing, gets a second job): Micrometer meter filters for this
  app (common tags + HTTP server latency buckets). Changes when: metric policy changes.
- `application.properties` (app module, existing): app config. Changes when: config or env
  vars change.
- `README.md` / `CLAUDE.md` (existing): docs, new "Observability" section.
- New classes: none.

## Decisions

### D1. Properties in the OBSERVABILITY block
Final state of the block (only the changed lines; the rest stays):

```properties
# MDC entries such as traceId and spanId become top-level JSON fields.
quarkus.log.console.json.mdc.flat-fields=true
# OTLP log exporter is built in every profile; D8 decides at run time if it sends.
quarkus.otel.logs.enabled=true
# Export switch (see D8): the block of jbh.otel.* lines and the two quarkus.otel lines
# that read them. OTEL_SDK_DISABLED stops only the OTLP export. Tracing stays on.
# One OTLP gRPC endpoint for all profiles: Alloy in Docker, Alloy dev port in the IDE.
quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}
quarkus.otel.resource.attributes=service.namespace=jbh,deployment.environment.name=${jbh.metrics.env}
jbh.metrics.env=${JBH_ENV:prod}
%dev.jbh.metrics.env=dev
%test.jbh.metrics.env=test
```

- Removed: `%dev.quarkus.otel.logs.enabled`, `%prod.quarkus.otel.exporter.otlp.endpoint`,
  `%dev.quarkus.otel.exporter.otlp.endpoint`, and the old `quarkus.otel.sdk.disabled` and
  `%dev.quarkus.otel.sdk.disabled` lines. The switch lines are in D8.
- `quarkus.otel.logs.enabled` is a build-time Quarkus property, default `false`. It only
  builds the log exporter. Whether logs are sent is decided at run time by D8
  (`quarkus.otel.logs.handler.enabled`). Export is OFF by default, also in prod, until
  JBH-84.
- `%dev` and `%test` overrides of `jbh.metrics.env` stay, so dev and test never mix with
  prod. In dev, `JBH_ENV` is ignored by `jbh.metrics.env` (the `%dev` line wins). This is the
  same behavior as today.

### D2. One source for the environment name
`deployment.environment.name` reads `${jbh.metrics.env}`. So the metric tag `env` and the
resource attribute always match, and `JBH_ENV` is read in one place only.
`quarkus.otel.resource.attributes` is a comma-separated `key=value` list and can be
overridden at runtime.

### D3. No new env var for service.version
The plan says "service.version from an env var if the image sets one". The image sets none
(checked `Dockerfile`, `.github/workflows` and the repo). Quarkus OTel already sets
`service.name` and `service.version` from `quarkus.application.name` and
`quarkus.application.version`. Decision: add nothing. This is a planned deviation from the
plan text, because the condition is false.

### D4. Latency buckets as a second MeterFilter
`MetricsTagsConfig` gets a second `@Produces @Singleton` method `httpServerLatency()`:

- It overrides `MeterFilter.configure(Meter.Id id, DistributionStatisticConfig config)`.
- If `id.getName()` equals `http.server.requests` (exact match, not prefix), it returns
  `DistributionStatisticConfig.builder().serviceLevelObjectives(<nanos as double>...).build()
  .merge(config)`. Otherwise it returns `config` unchanged.
- Bucket list (11): 5 ms, 10 ms, 25 ms, 50 ms, 100 ms, 250 ms, 500 ms, 1 s, 2.5 s, 5 s,
  10 s. Values are nanoseconds for timers, built with `Duration.ofMillis(x).toNanos()` and
  kept in one `private static final` constant array (named, no literals in `if`).
- The constructor and `commonTags()` do not change, so `MetricsTagsConfigTest` stays valid.
- Rejected: a new class (user decision, SRP: the class is "Micrometer meter filters for
  this app"). Rejected: `quarkus.micrometer.binder.http-server...` properties (Quarkus has no
  property for SLOs). Rejected: `publishPercentileHistogram` (many more series).
- Series cost: 11 buckets + `+Inf` per tag set of `http.server.requests`. Other meters are
  untouched.
- The Javadoc of the class changes to: Micrometer meter filters for this app.

### D5. Incoming trace context
Quarkus uses `quarkus.otel.propagators=tracecontext,baggage` (W3C) by default. Search of all
`application*.properties` (app and infra modules), pom files and Java code found no
`quarkus.otel.propagators`, no sampler override that drops parents (sampler is
`parentbased_traceidratio`, which honors the caller decision), no
`quarkus.otel.traces.suppress-application-uris`, and no `quarkus.otel.instrument.*` switch.
The sampler is now set by D8: with export ON it is `parentbased_traceidratio` (honors the
caller decision, same as before); with export OFF it is `always_off`. `always_off` still
joins the caller trace id into the MDC and passes it on (see D8). Result: nothing turns
joining off. A call from `jbh-iam` through the gateway joins the caller trace in both modes
(export off: only in logs and in the outgoing `traceparent`, with sampled=0).

### D6. Sensitive data in telemetry (findings)
Checked: all log statements in app and infra (`Logger`), MDC use, `@WithSpan`, span
attribute calls, and OTel JDBC/mailer instrumentation.

- No `@WithSpan`, no `Span.current()`, no `setAttribute`, no MDC writes in the code. So
  custom spans carry nothing. Auto spans are HTTP server spans (method, route, status,
  `url.path`, `url.query`). The only query is `type=EMAIL`. The path has no email.
- No `quarkus-opentelemetry` JDBC or mailer instrumentation is configured, so no SQL text and
  no SMTP data in spans.
- Logs: they hold notification ids (UUID), `recipientId` (UUID, not an email), notification
  status, enum names, locale, team id, and exception class names. `MailerConfigLogger` masks
  the password (`****` + length) and prints only "set"/"not set" for user and from address.
  The Consul code logs the registration payload at DEBUG (service data, no user data).
- Residual risks, no code fix planned:
  1. `NotificationGlobalExceptionHandler` and `NotificationRestAdapterV1` log or return
     `exception.getMessage()` of validation and business errors. Today these messages hold
     field names, not values. Recorded for the reviewer.
  2. The `real-email` dev profile sets `quarkus.mailer.mock=true`. The Quarkus mock mailer
     logs recipient and body at INFO. This runs only on a developer machine, and logs now go
     to the local stack. Accepted.
- Tracing is now on in every profile, so span attributes exist even with export off. The
  findings above hold for them.
- Result: nothing leaks today. No fix task. The new "Observability" docs repeat the rule so
  future code keeps it.

### D7. Existing test that guards the old values
`jbh-notification-app/src/test/java/com/jbh/notification/app/config/ObservabilityConfigPropertiesTest.java`
reads `application.properties` and fails after D1. It must be updated (same methods,
renamed where the old name would lie; no new test methods):

| Test (today) | Today asserts | New expected value |
|---|---|---|
| `shouldReadEndpointFromEnvWhenConfigured` | `%prod.` endpoint starts with `${OTEL_EXPORTER_OTLP_ENDPOINT:` | `quarkus.otel.exporter.otlp.endpoint` equals `${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}` |
| `shouldLeaveBaseEndpointUnset` | base endpoint is null | `%prod.` and `%dev.` endpoint keys are null (one line only) |
| `shouldExportByDefaultInDevToLocalStack` | `%dev.sdk.disabled` is `${OTEL_SDK_DISABLED:false}` and `%dev.` endpoint is `http://localhost:4317` | `%dev.quarkus.otel.sdk.disabled` is null; `%dev.jbh.otel.export-off` is `${OTEL_SDK_DISABLED:false}`; the `%dev.` endpoint assertion is removed (covered above) |
| `shouldDisableOtelSdkByDefaultWhenNoEnvVar` | `quarkus.otel.sdk.disabled` is `${OTEL_SDK_DISABLED:true}` | `quarkus.otel.sdk.disabled` is `false`; `jbh.otel.export-off` is `${OTEL_SDK_DISABLED:true}`; rename to say export is off by default |
| `shouldSampleAllByDefaultWhenNoSamplerEnvVar` | `quarkus.otel.traces.sampler.arg` is `${OTEL_TRACES_SAMPLER_ARG:1.0}` | unchanged, plus asserts `quarkus.otel.traces.sampler` is `${jbh.otel.sampler.${jbh.otel.export-off}:always_off}` and `jbh.otel.sampler.true` / `.false` are `always_off` / `${OTEL_TRACES_SAMPLER:parentbased_traceidratio}` |
| `shouldExportLogsToOtelOnlyInDev` | `%dev.` logs enabled `true`, base null | `quarkus.otel.logs.enabled` equals `true`; `%dev.quarkus.otel.logs.enabled` is null |
| `shouldSetEnvPerProfileWhenTagging` | base `prod`, `%dev.` `dev`, `%test.` `test` | base `${JBH_ENV:prod}`, `%dev.` `dev`, `%test.` `test` |

The log keys (`jbh.otel.logs-handler.true` is `false`, `.false` is `true`,
`quarkus.otel.logs.handler.enabled` is `${jbh.otel.logs-handler.${jbh.otel.export-off}:false}`)
are asserted inside `shouldExportLogsToOtelOnlyInDev` (renamed, since logs are now on in
every profile). No new test method.
All other tests in the class stay unchanged and must still pass (sampler arg, datasource metrics `true`, JSON only in prod, log
levels, dev console format with traceId and spanId, no `quarkus.observability.enabled`).
`MetricsTagsConfigTest` is untouched and must still pass.

### D8. Run-time export switch
Goal (user decision, JBH-55 revision 2026-11-15): `OTEL_SDK_DISABLED` stops only the OTLP
export. Tracing stays on in every profile, also in prod, so `traceId` appears in logs and
`traceparent` is read and passed on with export off. Default is export OFF. JBH-84 turns prod
export on. Section 5 of JBH-55 (`jbh-personal-finance`) reuses this design as is.

Final lines (config only, no Java):

```properties
quarkus.otel.sdk.disabled=false
jbh.otel.export-off=${OTEL_SDK_DISABLED:true}
%dev.jbh.otel.export-off=${OTEL_SDK_DISABLED:false}
jbh.otel.sampler.true=always_off
jbh.otel.sampler.false=${OTEL_TRACES_SAMPLER:parentbased_traceidratio}
jbh.otel.logs-handler.true=false
jbh.otel.logs-handler.false=true
quarkus.otel.traces.sampler=${jbh.otel.sampler.${jbh.otel.export-off}:always_off}
quarkus.otel.logs.handler.enabled=${jbh.otel.logs-handler.${jbh.otel.export-off}:false}
```

Keep `quarkus.otel.traces.sampler.arg=${OTEL_TRACES_SAMPLER_ARG:1.0}`,
`quarkus.otel.logs.enabled=true` (build time) and the one endpoint line. A short plain-English
comment block above the lines says: what the switch does, why it is a lookup (the exporter
properties are build time), and that a bad value falls back to off.

How it works:
- `jbh.otel.export-off` is `true` or `false`. It is the only line that reads
  `OTEL_SDK_DISABLED`. Default `true`; the `%dev` line makes the default `false`. `%test` has
  no override, so export is off in tests while the SDK is on.
- Quarkus resolves composed expressions `${a.${b}}` inner first, and `${x:default}` gives a
  default. Run-time properties resolve at run time. So the key
  `jbh.otel.sampler.<true|false>` is chosen at run time.
- OFF: sampler `always_off`, so no span is sampled and nothing is exported. The logs
  handler is off, so no log is exported. The exporter objects exist but stay idle.
- ON: sampler is `OTEL_TRACES_SAMPLER` (default `parentbased_traceidratio`), logs handler is
  on. Same as today's dev export.
- Quarkus puts `traceId` and `spanId` in the MDC for every span, sampled or not, so with
  `always_off` the logs still carry the (incoming) trace id.
- Bad value (for example `1` or `TRUE`): the lookup key does not exist, so the defaults
  `always_off` and `false` apply. Export is off and the service still starts. Values are
  case-sensitive `true` and `false`.

Known limit (important for section 5): with export off, the outgoing `traceparent` has
sampled=0. A downstream service with a parent-based sampler then drops its spans for that
request. `jbh-notification` calls no other service over HTTP (only SMTP and Consul
registration), so there is no effect here. For `jbh-personal-finance` (calls `jbh-iam`
through the gateway) it matters only in a mixed setup: finance export off and iam export on.
In prod all services switch together (JBH-84), so no effect there.

Rejected options:
- `quarkus.otel.sdk.disabled` as the switch: it turns ALL OTel off (run time), so no trace
  ids in logs and no `traceparent` handling. This was the old design.
- `quarkus.otel.exporter.otlp.enabled`, `quarkus.otel.traces.exporter`,
  `quarkus.otel.logs.exporter`: build time. A prod image could not change them with an env
  var.
- Empty endpoint as the switch: `OTelExporterRecorder` skips the OTLP processor and log
  exporter only when the SDK is disabled or the resolved endpoint is empty. An empty or unset
  endpoint falls back to `http://localhost:4317/`, so it cannot switch export off.
- A Java CDI `Sampler` bean (it replaces the sampler): same result, more code, and the logs
  handler would still need its own negation logic.
- Sampler only (the Quarkus docs hint: "to enable or disable the exporter at runtime, use
  the sampler"): not enough alone, because the log handler would still export. D8 sets both.

New config names: the `jbh.otel.*` keys are ours, not Quarkus keys.

## Test contract

- Unit tests: none new (user excluded unit tests for the whole JBH-55 plan). No test
  blueprint group.
- Integration tests: none new. Existing suites that must still pass:
  `mvn -q verify` for the app module (`ObservabilityConfigPropertiesTest`,
  `MetricsTagsConfigTest`, `ConsulServiceRegistrationTest`) and the infra and contracts
  tests. The database tests (Docker) do not read the changed properties.
- Expected values of the properties are in D1 and D7.
- User check after the build (manual, not automated), with the service running and one
  request sent:
  `curl -s localhost:7778/q/metrics | grep 'http_server_requests_seconds_bucket' | grep -o 'le="[^"]*"' | sort -u`
  must show 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0 and +Inf.
  `curl -s localhost:7778/q/metrics | grep -vc '^#'` gives the series count to compare before
  and after the change.

## Database

No database change. No table, column, index, constraint, view or migration.

## Concurrency

none. The new `MeterFilter` is a stateless singleton with an immutable constant. Property
changes have no runtime state. No shared data, no check-then-act, no async code.

## Not included

- `../jbh-deploy/README.md` (system map): prod env vars are JBH-84, deploy docs are JBH-55
  section 8. Not edited here.
- Turning export on in prod (JBH-84).
- Any change in `jbh-deploy`, Alloy, or other services.
- A Java `Sampler` bean (rejected, D8).
- Percentile histograms, or buckets for meters other than `http.server.requests`.
- Unit tests for the new filter (excluded by the user).

## Risks

- The `jbh.otel.*` lookup keys are new config names. A typo in a key silently falls back to
  export off, not to an error. The properties test guards the exact keys and values.
- `${jbh.metrics.env}` inside `quarkus.otel.resource.attributes` is a config expression. If
  it did not expand, the attribute would hold the raw text. The Quarkus config supports
  nested expressions, and the user check is: start with `OTEL_SDK_DISABLED=false` and look
  at the `deployment.environment.name` resource attribute in Tempo or Loki.
- Moving the dev endpoint from 4317 to 14317 changes the default for developers running
  `quarkus:dev` without Alloy. The shared LGTM stack still works if they set
  `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317`. README documents this.
