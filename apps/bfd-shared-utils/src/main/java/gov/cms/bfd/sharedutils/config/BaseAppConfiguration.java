package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.RdsClientConfig;
import gov.cms.bfd.sharedutils.database.RdsDataSourceFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import software.amazon.awssdk.regions.Region;

/**
 * Models the common configuration options for BFD applications, should be extended by a specific
 * application.
 */
public abstract class BaseAppConfiguration {

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

  /** Name of setting containing alternative endpoint URL for RDS service. */
  public static final String ENV_VAR_KEY_RDS_ENDPOINT = "RDS_ENDPOINT";
  /** Name of setting containing region name for RDS service queue. */
  public static final String ENV_VAR_KEY_RDS_REGION = "RDS_REGION";
  /** Name of setting containing access key for RDS service. */
  public static final String ENV_VAR_KEY_RDS_ACCESS_KEY = "RDS_ACCESS_KEY";
  /** Name of setting containing secret key for RDS service. */
  public static final String ENV_VAR_KEY_RDS_SECRET_KEY = "RDS_SECRET_KEY";
  /** Name of setting containing username used to obtain an access token for RDS. */
  public static final String ENV_VAR_KEY_RDS_USERNAME = "RDS_USERNAME";

  /** Object for capturing the metrics data. */
  @Getter private final MetricOptions metricOptions;
  /** Holds the configured options for the database connection. */
  @Getter private final DatabaseOptions databaseOptions;

  @Nullable private final RdsClientConfig rdsClientConfig;

  /**
   * Initializes an instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()} flyway looks for
   *     migration scripts
   */
  protected BaseAppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      @Nullable RdsClientConfig rdsClientConfig) {
    this.metricOptions = metricOptions;
    this.databaseOptions = databaseOptions;
    this.rdsClientConfig = rdsClientConfig;
  }

  public DataSourceFactory createDataSourceFactory() {
    if (rdsClientConfig == null) {
      return new HikariDataSourceFactory(databaseOptions, HikariDataSource::new);
    } else {
      return new RdsDataSourceFactory(rdsClientConfig, databaseOptions);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(", databaseOptions=");
    builder.append(databaseOptions);
    builder.append(", metricsOptions=");
    builder.append(metricOptions);
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
    String databaseUrl = config.stringValue(ENV_VAR_KEY_DATABASE_URL);
    String databaseUsername = config.stringValue(ENV_VAR_KEY_DATABASE_USERNAME);
    String databasePassword = config.stringValue(ENV_VAR_KEY_DATABASE_PASSWORD);
    Optional<Integer> databaseMaxPoolSize =
        config.positiveIntOption(ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE);

    return new DatabaseOptions(
        databaseUrl, databaseUsername, databasePassword, databaseMaxPoolSize.orElse(1));
  }

  /**
   * Loads {@link RdsClientConfig} for use in configuring RDS clients. Other than {@link
   * #ENV_VAR_KEY_RDS_USERNAME} these settings are generally only changed from defaults during
   * localstack based tests.
   *
   * @param config used to load configuration values
   * @return the aws client settings
   */
  @Nullable
  protected static RdsClientConfig loadRdsClientConfig(ConfigLoader config) {
    RdsClientConfig rdsConfig = null;
    final String rdsUsername = config.stringValue(ENV_VAR_KEY_RDS_USERNAME, null);
    if (!Strings.isNullOrEmpty(rdsUsername)) {
      rdsConfig =
          RdsClientConfig.rdsBuilder()
              .region(
                  config
                      .parsedOption(ENV_VAR_KEY_RDS_REGION, Region.class, Region::of)
                      .orElse(null))
              .endpointOverride(
                  config
                      .parsedOption(ENV_VAR_KEY_RDS_ENDPOINT, URI.class, URI::create)
                      .orElse(null))
              .accessKey(config.stringValue(ENV_VAR_KEY_RDS_ACCESS_KEY, null))
              .secretKey(config.stringValue(ENV_VAR_KEY_RDS_SECRET_KEY, null))
              .username(rdsUsername)
              .build();
    }
    return rdsConfig;
  }
}
