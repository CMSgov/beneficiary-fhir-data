package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Preconditions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import lombok.Getter;

/**
 * Models the common configuration options for BFD applications, should be extended by a specific
 * application.
 */
public abstract class BaseAppConfiguration extends BaseConfiguration {

  /** Object for capturing the metrics data. */
  @Getter private final MetricOptions metricOptions;

  /** Holds the configured options for the database connection. */
  @Getter private final DatabaseOptions databaseOptions;

  /** Common configuration settings for all AWS clients. * */
  @Getter private final AwsClientConfig awsClientConfig;

  /**
   * Initializes an instance.
   *
   * @param metricOptions the value to use for {@link #metricOptions}
   * @param databaseOptions the value to use for {@link #databaseOptions} flyway looks for migration
   *     scripts
   * @param awsClientConfig common configuration settings for all AWS clients
   */
  protected BaseAppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      AwsClientConfig awsClientConfig) {
    this.metricOptions = Preconditions.checkNotNull(metricOptions);
    this.databaseOptions = Preconditions.checkNotNull(databaseOptions);
    this.awsClientConfig = Preconditions.checkNotNull(awsClientConfig);
  }

  /**
   * Creates appropriate {@link HikariDataSourceFactory} based on our {@link DatabaseOptions}.
   *
   * @return factory for creating data sources
   */
  public HikariDataSourceFactory createDataSourceFactory() {
    return createDataSourceFactory(databaseOptions, awsClientConfig);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(", databaseOptions=");
    builder.append(databaseOptions);
    builder.append(", metricsOptions=");
    builder.append(metricOptions);
    builder.append(", awsClientConfig=");
    builder.append(awsClientConfig);
    return builder.toString();
  }
}
