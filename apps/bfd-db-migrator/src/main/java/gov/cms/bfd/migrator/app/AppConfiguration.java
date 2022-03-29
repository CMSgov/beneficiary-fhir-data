package gov.cms.bfd.migrator.app;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Models the configuration options for the application. TODO: BFD-1558 Move this class into a
 * common location to be used here and pipeline, refactor getters
 */
public final class AppConfiguration {

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

  private final MetricOptions metricOptions;
  private final DatabaseOptions databaseOptions;
  /*
   * Controls where flyway looks for migration scripts. If not set (null or empty string) flyway will use it's default location
   * <code>src/main/resources/db/migration</code>. This is primarily for the integration tests, so we can run test migrations
   * under an arbitrary directory full of scripts.
   */
  private final String flywayScriptLocationOverride;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param flywayScriptLocationOverride if non-empty, will override the default location that
   *     flyway looks for migration scripts
   */
  private AppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      String flywayScriptLocationOverride) {
    this.metricOptions = metricOptions;
    this.databaseOptions = databaseOptions;
    this.flywayScriptLocationOverride = flywayScriptLocationOverride;
  }

  /** @return the {@link MetricOptions} that the application will use */
  public MetricOptions getMetricOptions() {
    return metricOptions;
  }

  /** @return the {@link DatabaseOptions} that the application will use */
  public DatabaseOptions getDatabaseOptions() {
    return databaseOptions;
  }

  /**
   * Gets the flyway script location override.
   *
   * @return the flyway script location override
   */
  public String getFlywayScriptLocationOverride() {
    return flywayScriptLocationOverride;
  }

  /** @see Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(", databaseOptions=");
    builder.append(databaseOptions);
    builder.append(", metricsOptions=");
    builder.append(metricOptions);
    builder.append(", flywayScriptLocationOverride=");
    builder.append(flywayScriptLocationOverride);
    return builder.toString();
  }

  /**
   * Per <code>/dev/design-decisions-readme.md</code>, this application accepts its configuration
   * via environment variables. Read those in, and build an {@link AppConfiguration} instance from
   * them.
   *
   * @param dbUrl the dbUrl to use, if null will use the environment variable
   * @param dbUser the dbUser to use, if null will use the environment variable
   * @param dbPass the dbPass to use, if null will use the environment variable
   * @return the {@link AppConfiguration} instance represented by the configuration provided to this
   *     application via the environment variables
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     configuration passed to the application are incomplete or incorrect.
   */
  static AppConfiguration readConfigFromEnvironmentVariables(
      String dbUrl, String dbUser, String dbPass) {
    String databaseUrl = dbUrl;
    if (databaseUrl == null) {
      databaseUrl = readEnvStringRequired(ENV_VAR_KEY_DATABASE_URL);
    }
    String databaseUsername = dbUser;
    if (databaseUsername == null) {
      databaseUsername = readEnvStringRequired(ENV_VAR_KEY_DATABASE_USERNAME);
    }
    String databasePassword = dbPass;
    if (databasePassword == null) {
      databasePassword = readEnvStringRequired(ENV_VAR_KEY_DATABASE_PASSWORD);
    }
    Optional<String> newRelicMetricKey = readEnvStringOptional(ENV_VAR_NEW_RELIC_METRIC_KEY);
    Optional<String> newRelicAppName = readEnvStringOptional(ENV_VAR_NEW_RELIC_APP_NAME);
    Optional<String> newRelicMetricHost = readEnvStringOptional(ENV_VAR_NEW_RELIC_METRIC_HOST);
    Optional<String> newRelicMetricPath = readEnvStringOptional(ENV_VAR_NEW_RELIC_METRIC_PATH);
    Optional<Integer> newRelicMetricPeriod = readEnvIntOptional(ENV_VAR_NEW_RELIC_METRIC_PERIOD);
    Optional<Integer> databaseMaxPoolSize = readEnvIntOptional(ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE);
    Optional<String> flywayScriptLocationOverride =
        readEnvStringOptional(ENV_VAR_FLYWAY_SCRIPT_LOCATION);
    if (databaseMaxPoolSize.isPresent() && databaseMaxPoolSize.get() < 1) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, databaseMaxPoolSize));
    }

    Optional<String> hostname;
    try {
      hostname = Optional.of(InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      hostname = Optional.empty();
    }

    String flywayScriptLocation = flywayScriptLocationOverride.orElse("");

    MetricOptions metricOptions =
        new MetricOptions(
            newRelicMetricKey,
            newRelicAppName,
            newRelicMetricHost,
            newRelicMetricPath,
            newRelicMetricPeriod,
            hostname);
    DatabaseOptions databaseOptions =
        new DatabaseOptions(
            databaseUrl, databaseUsername, databasePassword, databaseMaxPoolSize.orElse(1));

    return new AppConfiguration(metricOptions, databaseOptions, flywayScriptLocation);
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   */
  static Optional<String> readEnvStringOptional(String environmentVariableName) {
    Optional<String> environmentVariableValue =
        Optional.ofNullable(System.getenv(environmentVariableName));
    return environmentVariableValue;
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value is missing.
   */
  static String readEnvStringRequired(String environmentVariableName) {
    Optional<String> environmentVariableValue =
        Optional.ofNullable(System.getenv(environmentVariableName));
    if (!environmentVariableValue.isPresent()) {
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              environmentVariableName));
    } else if (environmentVariableValue.get().isEmpty()) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              environmentVariableName, environmentVariableValue.get()));
    }

    return environmentVariableValue.get();
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  static Optional<Integer> readEnvIntOptional(String environmentVariableName) {
    Optional<String> environmentVariableValueText =
        Optional.ofNullable(System.getenv(environmentVariableName));
    if (!environmentVariableValueText.isPresent()) {
      return Optional.empty();
    }

    try {
      return Optional.of(Integer.valueOf(environmentVariableValueText.get()));
    } catch (NumberFormatException e) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s' (%s)",
              environmentVariableName, environmentVariableValueText.get(), e.getMessage()),
          e);
    }
  }
}
