# jbh-notification

Notification service of the JBH system. It sends notifications for the other services.
Email is the only type today.

The service is internal. In production, Caddy blocks `/jbh-api/notifications/**` from the
internet. `jbh-iam` reaches it through the gateway.

## Modules

| Module | What it is |
|---|---|
| `jbh-notification-contracts` | Published library. Commands, template metadata, validation. No framework code. |
| `jbh-notification-infra` | Service code: REST adapter, use cases, persistence, mailer, templates, Liquibase. |
| `jbh-notification-app` | Runnable Quarkus app and `Dockerfile`. Port 7778. |

## Endpoint

```
POST /jbh-api/notifications/v1?type=EMAIL
```

The body is a `SendNotificationCommand` (see the contracts module).

## Runtime

| Item | Value |
|---|---|
| Port | `7778` |
| Consul name | `jbh-notification` |
| Health | `/q/health` |
| Metrics | `/q/metrics` |

### Environment variables

| Variable | Use | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/jbh_finance` |
| `DATABASE_USERNAME` | Database user | `postgres` |
| `DATABASE_PASSWORD` | Database password | none in prod (required); `postgres` in dev and test |
| `GMAIL_USERNAME` | SMTP user | `whoknows` |
| `GMAIL_APP_PASSWORD` | SMTP app password | none in prod (required) |
| `GMAIL_FROM` | Sender address | `noreply@jbh-finance.com` |
| `GMAIL_HOST`, `GMAIL_PORT` | SMTP server | `smtp.gmail.com`, `465` |
| `JBH_FRONTEND_URL` | Base URL for links in emails | `https://jbh.usemagus.cloud` in prod |
| `CONSUL_ENABLED` | Turn Consul registration on or off | `true` |
| `CONSUL_HOST`, `CONSUL_PORT` | Consul address | `localhost`, `8500` |
| `CONSUL_HEALTH_CHECK_HOST` | Host Consul uses for the health check | empty |
| `CONSUL_DISCOVERY_HOSTNAME` | Address the service registers | empty |
| `CONSUL_PREFER_IP` | Register the IP instead of the host name | `true` |
| `OTEL_SDK_DISABLED` | Turn OpenTelemetry off or on | `true` (`false` in dev) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP gRPC collector (prod) | `http://localhost:4317` |
| `OTEL_TRACES_SAMPLER`, `OTEL_TRACES_SAMPLER_ARG` | Trace sampling | `parentbased_traceidratio`, `1.0` |

Never commit a `.env` file.

## Database

- Database `jbh_finance`, schema `notifications`.
- The database is shared with `jbh-personal-finance`.
- Liquibase runs at start (`db/notificationdb-master.xml`).

## Language of the email

The `locale` in the request metadata wins. If it is missing, the default is `es`.
Templates and subjects exist for `en` and `es`.

## Build

`jbh-commons-lib` must be in `~/.m2` first. Build it from its own repo
(`mvn install` there).

```bash
mvn clean verify            # build, tests and PMD
mvn -q pmd:check            # PMD only
mvn -pl jbh-notification-infra -Djbh.test.excludedGroups=none test   # database tests (needs Docker)
```

## Run

```bash
mvn -pl jbh-notification-app -am quarkus:dev
```

The app needs PostgreSQL on `localhost:5432` and, by default, Consul on `localhost:8500`.
Set `CONSUL_ENABLED=false` to run without Consul. In dev the mailer sends real email
(`%dev.quarkus.mailer.mock=false`), so set `GMAIL_USERNAME` and `GMAIL_APP_PASSWORD`.

## Image

CI builds the Quarkus fast-jar, then the image from `jbh-notification-app/Dockerfile`:

```bash
mvn -B -U -s .github/maven-settings.xml -Pgithub -pl jbh-notification-app -am -DskipTests package
docker build -t ghcr.io/kkpa-jbh/jbh-notification jbh-notification-app
```

Image: `ghcr.io/kkpa-jbh/jbh-notification` (tags: git SHA and `latest`).

## Use the contracts in another service

Add the dependency:

```xml
<dependency>
  <groupId>com.jbh</groupId>
  <artifactId>jbh-notification-contracts</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Add the repository `https://maven.pkg.github.com/kkpa-jbh/jbh-notification`. Add a
`settings.xml` server for its id with a token that has `read:packages`. In CI this
token is the secret `PACKAGES_READ_TOKEN`.

## CI workflows

| Workflow | Trigger | Result |
|---|---|---|
| `publish` | Push to `main` that changes `jbh-notification-contracts/**` or `pom.xml`; manual | Publishes the contracts and the parent POM to GitHub Packages |
| `image` | Push to `main`; manual | Builds and pushes the image to GHCR, keeps the 2 newest |
