package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Preconditions;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.RdsDataSourceFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;
import lombok.Getter;
import software.amazon.awssdk.regions.Region;

/**
 * Models the common configuration options for BFD applications, should be extended by a specific
 * application.
 */
public abstract class BaseAppConfiguration {

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#authenticationType} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_AUTH_TYPE = "DATABASE_AUTH_TYPE";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getDatabaseUrl()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_URL = "DATABASE_URL";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getDatabaseUsername()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_USERNAME = "DATABASE_USERNAME";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getDatabasePassword()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_PASSWORD = "DATABASE_PASSWORD";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getMaxPoolSize()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE = "DATABASE_MAX_POOL_SIZE";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricKey()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_KEY = "NEW_RELIC_METRIC_KEY";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicAppName()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_APP_NAME = "NEW_RELIC_APP_NAME";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricHost()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_HOST = "NEW_RELIC_METRIC_HOST";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricPath()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_PATH = "NEW_RELIC_METRIC_PATH";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricPeriod()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_PERIOD = "NEW_RELIC_METRIC_PERIOD";

  /**
   * The name of the environment variable that should be used to provide the location of the flyway
   * scripts.
   */
  public static final String ENV_VAR_FLYWAY_SCRIPT_LOCATION = "FLYWAY_SCRIPT_LOCATION";

  /** Name of setting containing alternative endpoint URL for AWS services. */
  public static final String ENV_VAR_KEY_AWS_ENDPOINT = "AWS_ENDPOINT";
  /** Name of setting containing region name for AWS services. */
  public static final String ENV_VAR_KEY_AWS_REGION = "AWS_REGION";
  /** Name of setting containing access key for AWS services. */
  public static final String ENV_VAR_KEY_AWS_ACCESS_KEY = "AWS_ACCESS_KEY";
  /** Name of setting containing secret key for AWS services. */
  public static final String ENV_VAR_KEY_AWS_SECRET_KEY = "AWS_SECRET_KEY";

  /** Object for capturing the metrics data. */
  @Getter private final MetricOptions metricOptions;
  /** Holds the configured options for the database connection. */
  @Getter private final DatabaseOptions databaseOptions;
  /** Common configuration settings for all AWS clients. * */
  @Getter private final AwsClientConfig awsClientConfig;

  /**
   * Initializes an instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()} flyway looks for
   *     migration scripts
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
   * Creates appropriate {@link DataSourceFactory} based on on our {@link DatabaseOptions}.
   *
   * @return factory for creating data sources
   */
  public DataSourceFactory createDataSourceFactory() {
    if (databaseOptions.getAuthenticationType() == DatabaseOptions.AuthenticationType.RDS) {
      return RdsDataSourceFactory.builder()
                .awsClientConfig(awsClientConfig)
                .databaseOptions(databaseOptions)
                .build();
    } else {
      return new HikariDataSourceFactory(databaseOptions);
    }
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

  /**
   * Reads metric options from a {@link ConfigLoader}.
   *
   * @param config used to read and parse configuration values
   * @return the metric options
   */
  protected static MetricOptions loadMetricOptions(ConfigLoader config) {
    Optional<String> newRelicMetricKey = config.stringOptionEmptyOK(ENV_VAR_NEW_RELIC_METRIC_KEY);
    Optional<String> newRelicAppName = config.stringOptionEmptyOK(ENV_VAR_NEW_RELIC_APP_NAME);
    Optional<String> newRelicMetricHost = config.stringOptionEmptyOK(ENV_VAR_NEW_RELIC_METRIC_HOST);
    Optional<String> newRelicMetricPath = config.stringOptionEmptyOK(ENV_VAR_NEW_RELIC_METRIC_PATH);
    Optional<Integer> newRelicMetricPeriod = config.intOption(ENV_VAR_NEW_RELIC_METRIC_PERIOD);

    Optional<String> hostname;
    try {
      hostname = Optional.of(InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      hostname = Optional.empty();
    }

    return new MetricOptions(
        newRelicMetricKey,
        newRelicAppName,
        newRelicMetricHost,
        newRelicMetricPath,
        newRelicMetricPeriod,
        hostname);
  }

  /**
   * Reads the database options from a {@link ConfigLoader}.
   *
   * @param config used to read and parse configuration values
   * @return the database options
   */
  protected static DatabaseOptions loadDatabaseOptions(ConfigLoader config) {
    DatabaseOptions.AuthenticationType databaseAuthType =
        config
            .enumOption(ENV_VAR_KEY_DATABASE_AUTH_TYPE, DatabaseOptions.AuthenticationType.class)
            .orElse(DatabaseOptions.AuthenticationType.JDBC);
    String databaseUrl = config.stringValue(ENV_VAR_KEY_DATABASE_URL);
    String databaseUsername = config.stringValue(ENV_VAR_KEY_DATABASE_USERNAME);
    String databasePassword = config.stringValue(ENV_VAR_KEY_DATABASE_PASSWORD);
    Optional<Integer> databaseMaxPoolSize =
        config.positiveIntOption(ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE);

    return DatabaseOptions.builder()
        .authenticationType(databaseAuthType)
        .databaseUrl(databaseUrl)
        .databaseUsername(databaseUsername)
        .databasePassword(databasePassword)
        .maxPoolSize(databaseMaxPoolSize.orElse(1))
        .build();
  }

  /**
   * Loads {@link AwsClientConfig} for use in configuring AWS clients. These settings are generally
   * only changed from defaults during localstack based tests.
   *
   * @param config used to load configuration values
   * @return the aws client settings
   */
  protected static AwsClientConfig loadAwsClientConfig(ConfigLoader config) {
    return AwsClientConfig.awsBuilder()
        .region(config.parsedOption(ENV_VAR_KEY_AWS_REGION, Region.class, Region::of).orElse(null))
        .endpointOverride(
            config.parsedOption(ENV_VAR_KEY_AWS_ENDPOINT, URI.class, URI::create).orElse(null))
        .accessKey(config.stringValue(ENV_VAR_KEY_AWS_ACCESS_KEY, null))
        .secretKey(config.stringValue(ENV_VAR_KEY_AWS_SECRET_KEY, null))
        .build();
  }
}
