package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import lombok.Getter;
import software.amazon.awssdk.services.sqs.SqsClient;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

/** Models the configuration options for the application. */
public class AppConfiguration extends BaseAppConfiguration {
  /** Name of setting containing name of SQS queue to which progress messages can be sent. */
  public static final String ENV_VAR_KEY_SQS_QUEUE_NAME = "DB_MIGRATOR_SQS_QUEUE";

  /**
   * Controls where flyway looks for migration scripts. If not set (null or empty string) flyway
   * will use it's default location {@code src/main/resources/db/migration}. This is primarily for
   * the integration tests, so we can run test migrations under an arbitrary directory full of
   * scripts.
   */
  @Getter private final String flywayScriptLocationOverride;

  /**
   * Used to communicate with SQS for sending progress messages. Null if SQS progress tracking has
   * not been enabled.
   */
  @Nullable @Getter private final SqsClient sqsClient;

  /**
   * Name of SQS queue to send progress messages to. Empty string if SQS progress tracking has not
   * been enabled.
   */
  @Getter private final String sqsQueueName;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param awsClientConfig used to configure AWS services
   * @param flywayScriptLocationOverride if non-empty, will override the default location that
   *     flyway looks for migration scripts
   * @param sqsClient null or a valid {@link SqsClient}
   * @param sqsQueueName name of queue to post progress to (empty if none)
   */
  private AppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      AwsClientConfig awsClientConfig,
      String flywayScriptLocationOverride,
      @Nullable SqsClient sqsClient,
      String sqsQueueName) {
    super(metricOptions, databaseOptions, awsClientConfig);
    this.flywayScriptLocationOverride = flywayScriptLocationOverride;
    this.sqsClient = sqsClient;
    this.sqsQueueName = sqsQueueName;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(super.toString());
    builder.append(", flywayScriptLocationOverride=");
    builder.append(flywayScriptLocationOverride);
    builder.append(", sqsClient=");
    builder.append(sqsClient == null ? "disabled" : "enabled");
    builder.append(", sqsQueueName=");
    builder.append(sqsQueueName);
    return builder.toString();
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

    final AwsClientConfig awsClientConfig = loadAwsClientConfig(configLoader);
    final String sqsQueueName = configLoader.stringValue(ENV_VAR_KEY_SQS_QUEUE_NAME, "");
    final SqsClient sqsClient = sqsQueueName.isEmpty() ? null : createSqsClient(awsClientConfig);

    return new AppConfiguration(
        metricOptions,
        databaseOptions,
        awsClientConfig,
        flywayScriptLocation,
        sqsClient,
        sqsQueueName);
  }

  /**
   * Creates a {@link SqsClient} instance using settings from the provided {@link AwsClientConfig}.
   *
   * @param awsClientConfig used to configure AWS services
   * @return the {@link SqsClient}
   */
  static SqsClient createSqsClient(AwsClientConfig awsClientConfig) {
    final var clientBuilder = SqsClient.builder();
    awsClientConfig.configureAwsService(clientBuilder);
    return clientBuilder.build();
  }
}
