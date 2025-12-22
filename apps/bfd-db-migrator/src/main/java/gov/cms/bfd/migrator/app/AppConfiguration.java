package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import jakarta.annotation.Nullable;
import lombok.Getter;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Models the configuration options for the application. */
public class AppConfiguration extends BaseAppConfiguration {
  /**
   * The name of the environment variable that should be used to provide the location of the flyway
   * scripts.
   */
  public static final String ENV_VAR_FLYWAY_SCRIPT_LOCATION = "FLYWAY_SCRIPT_LOCATION";

  /**
   * Path of the SSM parameter containing name of SQS queue to which progress messages can be sent.
   */
  public static final String SSM_PATH_SQS_QUEUE_NAME = "sqs/queue_name";

  /**
   * Controls where flyway looks for migration scripts. If not set (null or empty string) flyway
   * will use it's default location {@code src/main/resources/db/migration}. This is for the
   * integration tests, so we can run test migrations under an arbitrary directory full of scripts.
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
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param awsClientConfig used to configure AWS services
   * @param flywayScriptLocationOverride if non-empty, will override the default location that
   *     flyway looks for migration scripts
   * @param sqsClient null or a valid {@link SqsClient}
   * @param sqsQueueName name of queue to post progress to (empty if none)
   */
  private AppConfiguration(
      DatabaseOptions databaseOptions,
      AwsClientConfig awsClientConfig,
      String flywayScriptLocationOverride,
      @Nullable SqsClient sqsClient,
      String sqsQueueName) {
    super(databaseOptions, awsClientConfig);
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
   * @param configLoader used to access settings (provided explicitly for testing)
   * @return instance representing the configuration provided to this application via the {@link
   *     ConfigLoader}
   * @throws ConfigException if the configuration passed to the application is invalid
   */
  public static AppConfiguration loadConfig(ConfigLoader configLoader) {
    DatabaseOptions databaseOptions = loadDatabaseOptions(configLoader);

    String flywayScriptLocation =
        configLoader.stringOptionEmptyOK(ENV_VAR_FLYWAY_SCRIPT_LOCATION).orElse("");

    final AwsClientConfig awsClientConfig = loadAwsClientConfig(configLoader);
    final String sqsQueueName = configLoader.stringValue(SSM_PATH_SQS_QUEUE_NAME, "");
    final SqsClient sqsClient = sqsQueueName.isEmpty() ? null : createSqsClient(awsClientConfig);

    return new AppConfiguration(
        databaseOptions, awsClientConfig, flywayScriptLocation, sqsClient, sqsQueueName);
  }
}
