package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Models the configuration options for the application. */
public class AppConfiguration extends BaseAppConfiguration {
  public static final String ENV_VAR_KEY_SQS_QUEUE_NAME = "DB_MIGRATOR_SQS_QUEUE";
  public static final String ENV_VAR_KEY_SQS_ENDPOINT = "DB_MIGRATOR_SQS_ENDPOINT";
  public static final String ENV_VAR_KEY_SQS_REGION = "DB_MIGRATOR_SQS_REGION";
  public static final String ENV_VAR_KEY_SQS_ACCESS_KEY = "DB_MIGRATOR_SQS_ACCESS_KEY";
  public static final String ENV_VAR_KEY_SQS_SECRET_KEY = "DB_MIGRATOR_SQS_ACCESS_KEY";

  /**
   * Controls where flyway looks for migration scripts. If not set (null or empty string) flyway
   * will use it's default location {@code src/main/resources/db/migration}. This is primarily for
   * the integration tests, so we can run test migrations under an arbitrary directory full of
   * scripts.
   */
  @Getter private final String flywayScriptLocationOverride;

  @Nullable @Getter private final SqsClient sqsClient;

  @Getter private final String sqsQueueName;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param flywayScriptLocationOverride if non-empty, will override the default location that
   *     flyway looks for migration scripts
   * @param sqsQueueName
   */
  private AppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      String flywayScriptLocationOverride,
      @Nullable SqsClient sqsClient,
      String sqsQueueName) {
    super(metricOptions, databaseOptions);
    this.flywayScriptLocationOverride = flywayScriptLocationOverride;
    this.sqsClient = sqsClient;
    this.sqsQueueName = sqsQueueName;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(super.toString());
    builder.append(", flywayScriptLocationOverride=");
    builder.append(flywayScriptLocationOverride);
    return builder.toString();
  }

  static SqsClient createSqsClient(ConfigLoader configLoader) {
    final var clientBuilder = SqsClient.builder();
    // either region or endpoint can be set on ssmClient but not both
    if (configLoader.stringOption(ENV_VAR_KEY_SQS_ENDPOINT).isPresent()) {
      // region is required when defining endpoint
      clientBuilder
          .region(configLoader.parsedValue(ENV_VAR_KEY_SQS_REGION, Region.class, Region::of))
          .endpointOverride(URI.create(configLoader.stringValue(ENV_VAR_KEY_SQS_ENDPOINT)));
    } else if (configLoader.stringOption(ENV_VAR_KEY_SQS_REGION).isPresent()) {
      clientBuilder.region(
          configLoader.parsedValue(ENV_VAR_KEY_SQS_REGION, Region.class, Region::of));
    }
    if (configLoader.stringOption(ENV_VAR_KEY_SQS_ACCESS_KEY).isPresent()) {
      clientBuilder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(
                  configLoader.stringValue(ENV_VAR_KEY_SQS_ACCESS_KEY),
                  configLoader.stringValue(ENV_VAR_KEY_SQS_SECRET_KEY))));
    }
    return clientBuilder.build();
  }

  /**
   * Read configuration variables from a layered {@link ConfigLoader} and build an {@link
   * AppConfiguration} instance from them.
   *
   * @param getenv function used to access environment variables (provided explicitly for testing)
   * @return instance representing the configuration provided to this application via the
   *     environment variables
   * @throws ConfigException if the configuration passed to the application is invalid
   */
  public static AppConfiguration loadConfig(Function<String, String> getenv) {
    final var configLoader = LayeredConfiguration.createConfigLoader(Map.of(), getenv);

    MetricOptions metricOptions = loadMetricOptions(configLoader);
    DatabaseOptions databaseOptions = loadDatabaseOptions(configLoader);

    String flywayScriptLocation =
        configLoader.stringOptionEmptyOK(ENV_VAR_FLYWAY_SCRIPT_LOCATION).orElse("");

    final String sqsQueueName = configLoader.stringValue(ENV_VAR_KEY_SQS_QUEUE_NAME, "");
    final SqsClient sqsClient = sqsQueueName.isEmpty() ? null : createSqsClient(configLoader);

    return new AppConfiguration(
        metricOptions, databaseOptions, flywayScriptLocation, sqsClient, sqsQueueName);
  }
}
