package com.jbh.notification.app.health;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a throwaway PostgreSQL and gives its URL and login to the notifications datasource. Same idea
 * as NotificationPostgresResource in jbh-notification-infra: a real URL, never an empty one, so Quarkus
 * cannot pick the URL of the properties file (a real database).
 */
public class AppPostgresResource implements QuarkusTestResourceLifecycleManager {

  /** Same major version as production (jbh-deploy compose.yaml). */
  private static final String IMAGE = "postgres:16-alpine";
  private static final String DATASOURCE_PREFIX = "quarkus.datasource.notifications.";

  private PostgreSQLContainer postgres;

  @Override
  public Map<String, String> start() {
    postgres = new PostgreSQLContainer(IMAGE).withInitScript("db/test-create-notification-app-schema.sql");
    postgres.start();
    return Map.of(
        DATASOURCE_PREFIX + "jdbc.url", postgres.getJdbcUrl() + "?currentSchema=notifications",
        DATASOURCE_PREFIX + "username", postgres.getUsername(),
        DATASOURCE_PREFIX + "password", postgres.getPassword());
  }

  @Override
  public void stop() {
    if (postgres != null) {
      postgres.stop();
    }
  }
}
