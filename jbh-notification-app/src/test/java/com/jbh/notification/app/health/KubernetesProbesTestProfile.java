package com.jbh.notification.app.health;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/**
 * Runs the app like the Kubernetes cluster does (JBH-86): Consul registration off
 * (the cluster sets CONSUL_ENABLED=false), a mock mailer and a throwaway PostgreSQL.
 */
public class KubernetesProbesTestProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "consul.enabled", "false",
        // No email leaves the test: the mock mailer only logs. The password is a placeholder, never a real one.
        "quarkus.mailer.mock", "true",
        "quarkus.mailer.password", "test-placeholder");
  }

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(AppPostgresResource.class));
  }
}
