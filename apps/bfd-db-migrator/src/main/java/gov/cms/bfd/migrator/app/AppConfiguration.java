package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.util.Optional;

/** Models the configuration options for the application. */
public class AppConfiguration extends BaseAppConfiguration {

  /**
   * Controls where flyway looks for migration scripts. If not set (null or empty string) flyway
   * will use it's default location <code>src/main/resources/db/migration</code>. This is primarily
   * for the integration tests, so we can run test migrations under an arbitrary directory full of
   * scripts.
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
    super(metricOptions, databaseOptions);
    this.flywayScriptLocationOverride = flywayScriptLocationOverride;
  }

  /**
   * Gets the flyway script location override.
   *
   * @return the flyway script location override
   */
  public String getFlywayScriptLocationOverride() {
    return flywayScriptLocationOverride;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(super.toString());
    builder.append(", flywayScriptLocationOverride=");
    builder.append(flywayScriptLocationOverride);
    return builder.toString();
  }

  /**
   * Per <code>/dev/design-decisions-readme.md</code>, this application accepts its configuration
   * via environment variables. Read those in, and build an {@link AppConfiguration} instance from
   * them.
   *
   * @return the {@link AppConfiguration} instance represented by the configuration provided to this
   *     application via the environment variables
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     configuration passed to the application are incomplete or incorrect.
   */
  public static AppConfiguration readConfigFromEnvironmentVariables() {
    final var configLoader = BaseAppConfiguration.envVarConfigLoader();

    MetricOptions metricOptions = readMetricOptionsFromEnvironmentVariables(configLoader);
    DatabaseOptions databaseOptions = readDatabaseOptionsFromEnvironmentVariables(configLoader);

    Optional<String> flywayScriptLocationOverride =
        readEnvStringOptional(configLoader, ENV_VAR_FLYWAY_SCRIPT_LOCATION);
    String flywayScriptLocation = flywayScriptLocationOverride.orElse("");

    return new AppConfiguration(metricOptions, databaseOptions, flywayScriptLocation);
  }
}
