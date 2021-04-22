package gov.cms.bfd.pipeline.app;

import java.io.Serializable;

/** Models the user-configurable options for sending telemetry to New Relic. */
public final class MetricOptions implements Serializable {
  private final String newRelicMetricKey;
  private final String newRelicAppName;
  private final String newRelicMetricHost;
  private final String newRelicMetricPath;
  private final int newRelicMetricPeriod;
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
      String newRelicMetricKey,
      String newRelicAppName,
      String newRelicMetricHost,
      String newRelicMetricPath,
      int newRelicMetricPeriod,
      String hostname) {
    this.newRelicMetricKey = newRelicMetricKey;
    this.newRelicAppName = newRelicAppName;
    this.newRelicMetricHost = newRelicMetricHost;
    this.newRelicMetricPath = newRelicMetricPath;
    this.newRelicMetricPeriod = newRelicMetricPeriod;
    this.hostname = hostname;
  }

  /** @return the hostname that will send metrics to New Relic */
  public String getHostname() {
    return hostname;
  }

  /** @return the interval between when each batch of metrics is sent to New Relic */
  public int getNewRelicMetricPeriod() {
    return newRelicMetricPeriod;
  }

  /** @return the relative path of the New Relic Metric API where telemetry will be sent */
  public String getNewRelicMetricPath() {
    return newRelicMetricPath;
  }

  /** @return the host of the New Relic Metric API where telemetry will be sent */
  public String getNewRelicMetricHost() {
    return newRelicMetricHost;
  }

  /** @return the name of the app with which metrics are tagged in New Relic */
  public String getNewRelicAppName() {
    return newRelicAppName;
  }

  /** @return the secret key granting access to the New Relic Metric API */
  public String getNewRelicMetricKey() {
    return newRelicMetricKey;
  }

  /** @see java.lang.Object#toString() */
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
