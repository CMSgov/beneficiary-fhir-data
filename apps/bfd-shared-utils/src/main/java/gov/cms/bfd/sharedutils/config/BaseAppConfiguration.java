package gov.cms.bfd.sharedutils.config;

import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Models the common configuration options for BFD applications, should be extended by a specific
 * application.
 *
 * <p>This is serializable in order to support IT tests which deserialize the application
 * configuration to ensure the values were set correctly. Classes that extend this should also be
 * serializable if similar testing is desired, but it is not required for production functionality.
 */
public abstract class BaseAppConfiguration implements Serializable {
  /** Serialization UID. */
  private static final long serialVersionUID = -6845504165285244533L;

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

  /** Object for capturing the metrics data. */
  private final MetricOptions metricOptions;
  /** Holds the configured options for the database connection. */
  private final DatabaseOptions databaseOptions;

  /**
   * Constructs a new {@link BaseAppConfiguration} instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()} flyway looks for
   *     migration scripts
   */
  protected BaseAppConfiguration(MetricOptions metricOptions, DatabaseOptions databaseOptions) {
    this.metricOptions = metricOptions;
    this.databaseOptions = databaseOptions;
  }

  /**
   * Gets the {@link #metricOptions}.
   *
   * @return the {@link MetricOptions} that the application will use
   */
  public MetricOptions getMetricOptions() {
    return metricOptions;
  }

  /**
   * Gets the {@link #databaseOptions}.
   *
   * @return the {@link DatabaseOptions} that the application will use
   */
  public DatabaseOptions getDatabaseOptions() {
    return databaseOptions;
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
}
