package gov.cms.bfd.sharedutils.config;

import java.util.Optional;

/**
 * Models the user-configurable options for sending telemetry to New Relic. TODO: BFD-1558 Move this
 * class into a common location to be used here and pipeline
 */
public final class MetricOptions {

  /** The secret key granting access to the New Relic Metric API. */
  private final String newRelicMetricKey;
  /** The name of the app with which metrics are tagged in New Relic. */
  private final String newRelicAppName;
  /** The host of the New Relic Metric API where telemetry will be sent. */
  private final String newRelicMetricHost;
  /** The relative path of the New Relic Metric API where telemetry will be sent. */
  private final String newRelicMetricPath;
  /** The interval between when each batch of metrics is sent to New Relic. */
  private final Integer newRelicMetricPeriod;
  /** The hostname that will send metrics to New Relic. */
  private final String hostname;

  /**
   * Constructs a new {@link MetricOptions} instance.
   *
   * @param newRelicMetricKey the value to use for {@link #getNewRelicMetricKey()}
   * @param newRelicAppName the value to use for {@link #getNewRelicAppName()}
   * @param newRelicMetricHost the value to use for {@link #getNewRelicMetricHost()}
   * @param newRelicMetricPath the value to use for {@link #getNewRelicMetricPath()}
   * @param newRelicMetricPeriod the value to use for {@link #getNewRelicMetricPeriod()}
   * @param hostname the value to use for {@link #getHostname()}
   */
  public MetricOptions(
      Optional<String> newRelicMetricKey,
      Optional<String> newRelicAppName,
      Optional<String> newRelicMetricHost,
      Optional<String> newRelicMetricPath,
      Optional<Integer> newRelicMetricPeriod,
      Optional<String> hostname) {
    this.newRelicMetricKey = newRelicMetricKey.orElse(null);
    this.newRelicAppName = newRelicAppName.orElse(null);
    this.newRelicMetricHost = newRelicMetricHost.orElse(null);
    this.newRelicMetricPath = newRelicMetricPath.orElse(null);
    this.newRelicMetricPeriod = newRelicMetricPeriod.orElse(null);
    this.hostname = hostname.orElse(null);
  }

  /**
   * Gets the {@link #hostname}.
   *
   * @return the hostname that will send metrics to New Relic
   */
  public Optional<String> getHostname() {
    return Optional.ofNullable(hostname);
  }

  /**
   * Gets the {@link #newRelicMetricPeriod}.
   *
   * @return the interval between when each batch of metrics is sent to New Relic
   */
  public Optional<Integer> getNewRelicMetricPeriod() {
    return Optional.ofNullable(newRelicMetricPeriod);
  }

  /**
   * Gets the {@link #newRelicMetricPath}.
   *
   * @return the relative path of the New Relic Metric API where telemetry will be sent
   */
  public Optional<String> getNewRelicMetricPath() {
    return Optional.ofNullable(newRelicMetricPath);
  }

  /**
   * Gets the {@link #newRelicMetricHost}.
   *
   * @return the host of the New Relic Metric API where telemetry will be sent
   */
  public Optional<String> getNewRelicMetricHost() {
    return Optional.ofNullable(newRelicMetricHost);
  }

  /**
   * Gets the {@link #newRelicAppName}.
   *
   * @return the name of the app with which metrics are tagged in New Relic
   */
  public Optional<String> getNewRelicAppName() {
    return Optional.ofNullable(newRelicAppName);
  }

  /**
   * Gets the {@link #newRelicMetricKey}.
   *
   * @return the secret key granting access to the New Relic Metric API
   */
  public Optional<String> getNewRelicMetricKey() {
    return Optional.ofNullable(newRelicMetricKey);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("MetricOptions [newRelicMetricHost=");
    builder.append(newRelicMetricHost);
    builder.append(", newRelicMetricPath=");
    builder.append(newRelicMetricPath);
    builder.append(", newRelicMetricPath=");
    builder.append(newRelicMetricPath);
    builder.append(", newRelicAppName=");
    builder.append(newRelicAppName);
    builder.append(", newRelicMetricPeriod=");
    builder.append(newRelicMetricPeriod);
    builder.append(", newRelicMetricKey=");
    builder.append("***");
    builder.append(", hostname=");
    builder.append(hostname);
    builder.append("]");
    return builder.toString();
  }
}
