package gov.cms.bfd.sharedutils.config;

import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Function;

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
   * Creates a {@link ConfigLoader} that reads environment variables.
   *
   * @return the config
   */
  public static ConfigLoader envVarConfigLoader() {
    return new ConfigLoader(key -> Optional.ofNullable(System.getenv(key)).stream().toList());
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

  /** {@inheritDoc} */
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
   * Reads metric options from environment variables.
   *
   * @return the metric options read from the environment variables
   */
  protected static MetricOptions readMetricOptionsFromEnvironmentVariables(ConfigLoader config) {
    Optional<String> newRelicMetricKey =
        readEnvStringOptional(config, ENV_VAR_NEW_RELIC_METRIC_KEY);
    Optional<String> newRelicAppName = readEnvStringOptional(config, ENV_VAR_NEW_RELIC_APP_NAME);
    Optional<String> newRelicMetricHost =
        readEnvStringOptional(config, ENV_VAR_NEW_RELIC_METRIC_HOST);
    Optional<String> newRelicMetricPath =
        readEnvStringOptional(config, ENV_VAR_NEW_RELIC_METRIC_PATH);
    Optional<Integer> newRelicMetricPeriod =
        readEnvIntOptional(config, ENV_VAR_NEW_RELIC_METRIC_PERIOD);

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
   * Reads the database options from environment variables.
   *
   * @return the database options read from the environment variables
   */
  protected static DatabaseOptions readDatabaseOptionsFromEnvironmentVariables(
      ConfigLoader config) {
    String databaseUrl = readEnvStringRequired(config, ENV_VAR_KEY_DATABASE_URL);
    String databaseUsername = readEnvStringRequired(config, ENV_VAR_KEY_DATABASE_USERNAME);
    String databasePassword = readEnvStringRequired(config, ENV_VAR_KEY_DATABASE_PASSWORD);
    Optional<Integer> databaseMaxPoolSize =
        readEnvIntOptional(config, ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE);

    if (databaseMaxPoolSize.isPresent() && databaseMaxPoolSize.get() < 1) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, databaseMaxPoolSize));
    }

    return new DatabaseOptions(
        databaseUrl, databaseUsername, databasePassword, databaseMaxPoolSize.orElse(1));
  }

  /**
   * Reads an optional String environment variable.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set or is empty
   */
  protected static Optional<String> readEnvStringOptional(
      ConfigLoader config, String environmentVariableName) {
    return config.stringOptionEmptyOK(environmentVariableName);
  }

  /**
   * Reads an optional String environment value, skipping any that are null or whitespace.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set or contains an empty value
   */
  protected static Optional<String> readEnvNonEmptyStringOptional(
      ConfigLoader config, String environmentVariableName) {
    return config.stringOption(environmentVariableName);
  }

  /**
   * Reads a required String environment variable.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value is missing
   */
  protected static String readEnvStringRequired(
      ConfigLoader config, String environmentVariableName) {
    return config.stringValue(environmentVariableName);
  }

  /**
   * Reads an optional Integer environment variable.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed
   */
  protected static Optional<Integer> readEnvIntOptional(
      ConfigLoader config, String environmentVariableName) {
    return config.intOption(environmentVariableName);
  }

  /**
   * Reads an optional environment variable using a parser to determine the value.
   *
   * @param <T> the type parameter
   * @param environmentVariableName the name of the environment variable to get the value of
   * @param klass class of object returned by the parser
   * @param parser the function used to convert the name into a parsed value
   * @return the value of the specified environment variable converted to a parsed value
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  protected static <T> Optional<T> readEnvParsedOptional(
      ConfigLoader config,
      String environmentVariableName,
      Class<T> klass,
      Function<String, T> parser) {
    return config.parsedOption(environmentVariableName, klass, parser);
  }

  /**
   * Reads a required Integer environment variable that must be positive.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed or is not positive.
   */
  protected static int readEnvIntPositiveRequired(
      ConfigLoader config, String environmentVariableName) {
    int value = config.intValue(environmentVariableName);
    if (value < 1) {
      throw new ConfigException(environmentVariableName, "not a positive integer");
    }
    return value;
  }

  /**
   * Reads an optional Boolean environment variable.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  protected static Optional<Boolean> readEnvBooleanOptional(
      ConfigLoader config, String environmentVariableName) {
    return config.booleanOption(environmentVariableName);
  }

  /**
   * Reads a required Boolean environment variable.
   *
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  protected static boolean readEnvBooleanRequired(
      ConfigLoader config, String environmentVariableName) {
    return config.booleanValue(environmentVariableName);
  }
}
