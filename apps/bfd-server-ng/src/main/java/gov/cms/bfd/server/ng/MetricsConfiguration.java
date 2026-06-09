package gov.cms.bfd.server.ng;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics;
import java.time.Duration;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/** Configuration for Micrometer metrics integration. */
@Configuration
@AllArgsConstructor
public class MetricsConfiguration {

  private final gov.cms.bfd.server.ng.Configuration configuration;

  /** List of metric names that are allowed to be published to Cloudwatch by Micrometer. */
  public static final Set<String> MICROMETER_ALLOWED_METRIC_PREFIX_NAMES =
      Set.of("application.", "jvm.threads.virtual.", "executor.");

  /**
   * Configures Micrometer to support @MeterTag annotations with SPEL expressions.
   *
   * <p>IMPORTANT: @Timed annotations only work on public methods for classes that are registered
   * beans. Attempting to use this for private methods or non-bean classes will not work since it
   * relies on Spring AOP's proxies.
   *
   * @param registry meter registry
   * @return timed aspect
   */
  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    var timedAspect = new TimedAspect(registry);
    ValueResolver valueResolver = Object::toString;
    var valueExpressionResolver = new SpelValueExpressionResolver();
    timedAspect.setMeterTagAnnotationHandler(
        new MeterTagAnnotationHandler(aClass -> valueResolver, aClass -> valueExpressionResolver));
    return timedAspect;
  }

  /**
   * Configures Micrometer to collect virtual thread metrics.
   *
   * @param registry meter registry
   * @return virtual thread metrics
   */
  @Bean
  public VirtualThreadMetrics virtualThreadMetrics(MeterRegistry registry) {
    var metrics = new VirtualThreadMetrics();
    metrics.bindTo(registry);
    return metrics;
  }

  /**
   * Configures Micrometer CloudWatch metric export properties. Currently, does not override
   * application.properties.
   *
   * @return CloudWatch configuration
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "management.metrics.export.cloudwatch",
      name = "enabled",
      havingValue = "true")
  public CloudWatchConfig cloudWatchConfig() {
    return new CloudWatchConfig() {

      @Override
      public @Nullable String get(String key) {
        return null;
      }

      @Override
      public String namespace() {
        return String.format("bfd-%s/server-ng", configuration.getEnv());
      }

      @Override
      public Duration step() {
        return Duration.ofMinutes(1);
      }
    };
  }

  /**
   * Configures the asynchronous CloudWatch client used for publishing metrics.
   *
   * @return CloudWatch async client
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "management.metrics.export.cloudwatch",
      name = "enabled",
      havingValue = "true")
  public CloudWatchAsyncClient cloudWatchAsyncClient() {
    return CloudWatchAsyncClient.create();
  }

  /**
   * Creates the CloudWatch meter registry for publishing Micrometer metrics.
   *
   * @param cloudWatchConfig CloudWatch configuration
   * @param cloudWatchAsyncClient CloudWatch async client
   * @return CloudWatch meter registry
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "management.metrics.export.cloudwatch",
      name = "enabled",
      havingValue = "true")
  public CloudWatchMeterRegistry cloudWatchMeterRegistry(
      CloudWatchConfig cloudWatchConfig, CloudWatchAsyncClient cloudWatchAsyncClient) {
    final var micrometerClock = io.micrometer.core.instrument.Clock.SYSTEM;
    var registry =
        new CloudWatchMeterRegistry(cloudWatchConfig, micrometerClock, cloudWatchAsyncClient);

    registry
        .config()
        .meterFilter(
            MeterFilter.denyUnless(
                id ->
                    MICROMETER_ALLOWED_METRIC_PREFIX_NAMES.stream()
                        .anyMatch(id.getName()::startsWith)))
        .meterFilter(MeterFilter.denyNameStartsWith("application.started"))
        .meterFilter(MeterFilter.denyNameStartsWith("application.ready"))
        .meterFilter(MeterFilter.ignoreTags("class", "method", "exception", "uri"));

    return registry;
  }
}
