package com.jbh.notification.app.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.stream.LongStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Micrometer meter filters for this app: the common tags {@code service} and {@code env} on every
 * meter, and latency buckets for the HTTP server timer. Quarkus has no property for either, so
 * {@link MeterFilter} beans are the only way.
 */
@Singleton
public class MetricsTagsConfig {

  private static final String SERVICE_TAG = "service";
  private static final String ENV_TAG = "env";
  private static final String HTTP_SERVER_METER = "http.server.requests";

  /** Bucket bounds in milliseconds: 5 ms up to 10 s. */
  private static final long[] LATENCY_BUCKETS_MILLIS = {
    5, 10, 25, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000
  };

  private final String service;
  private final String env;

  public MetricsTagsConfig(
      @ConfigProperty(name = "quarkus.application.name") final String service,
      @ConfigProperty(name = "jbh.metrics.env") final String env) {
    this.service = service;
    this.env = env;
  }

  /**
   * Builds the filter that tags each meter with the service name and the environment.
   *
   * @return the common-tags filter
   */
  @Produces
  @Singleton
  public MeterFilter commonTags() {
    return MeterFilter.commonTags(Tags.of(SERVICE_TAG, service, ENV_TAG, env));
  }

  /**
   * Builds the filter that adds latency buckets to the HTTP server timer. Other meters keep their
   * configuration.
   *
   * @return the latency-buckets filter
   */
  @Produces
  @Singleton
  public MeterFilter httpServerLatency() {
    final DistributionStatisticConfig buckets =
        DistributionStatisticConfig.builder()
            .serviceLevelObjectives(latencyBucketsNanos())
            .build();
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(
          final Meter.Id id, final DistributionStatisticConfig config) {
        if (HTTP_SERVER_METER.equals(id.getName())) {
          return buckets.merge(config);
        }
        return config;
      }
    };
  }

  private static double[] latencyBucketsNanos() {
    return LongStream.of(LATENCY_BUCKETS_MILLIS)
        .mapToDouble(millis -> Duration.ofMillis(millis).toNanos())
        .toArray();
  }
}
