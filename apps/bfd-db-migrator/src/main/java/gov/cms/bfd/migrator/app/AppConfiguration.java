package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.util.Map;

/** Models the configuration options for the application. */
public class AppConfiguration extends BaseAppConfiguration {

  /**
   * Controls where flyway looks for migration scripts. If not set (null or empty string) flyway
   * will use it's default location {@code src/main/resources/db/migration}. This is primarily for
   * the integration tests, so we can run test migrations under an arbitrary directory full of
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(super.toString());
    builder.append(", flywayScriptLocationOverride=");
    builder.append(flywayScriptLocationOverride);
    return builder.toString();
  }

  /**
   * Read configuration variables from a layered {@link ConfigLoader} and build an {@link
   * AppConfiguration} instance from them.
   *
   * @return instance representing the configuration provided to this application via the
   *     environment variables
   * @throws ConfigException if the configuration passed to the application is invalid
   */
  public static AppConfiguration loadConfig() {
    final var configLoader = LayeredConfiguration.createConfigLoader(Map.of(), System::getenv);

    MetricOptions metricOptions = loadMetricOptions(configLoader);
    DatabaseOptions databaseOptions = loadDatabaseOptions(configLoader);

    String flywayScriptLocation =
        configLoader.stringOptionEmptyOK(ENV_VAR_FLYWAY_SCRIPT_LOCATION).orElse("");

    return new AppConfiguration(metricOptions, databaseOptions, flywayScriptLocation);
  }
}
