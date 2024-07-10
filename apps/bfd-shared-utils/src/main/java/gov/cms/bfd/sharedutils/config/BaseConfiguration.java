package gov.cms.bfd.sharedutils.config;

import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions.AuthenticationType;
import gov.cms.bfd.sharedutils.database.DefaultHikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.RdsDataSourceFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Base configuration class modeling the configuration options for BFD applications. Should be
 * extended directly by applications (Server) using Spring framework's @Configuration. Other
 * applications (Migrator, Pipeline, etc.) should instead use {@link BaseAppConfiguration}.
 */
public abstract class BaseConfiguration {

  /**
   * The path of the SSM parameter that should be used to provide the {@link DatabaseOptions} {@link
   * DatabaseOptions#authenticationType} value.
   */
  public static final String SSM_PATH_DATABASE_AUTH_TYPE = "db/auth_type";

  /**
   * The path of the SSM parameter that should be used to provide the {@link DatabaseOptions} {@link
   * DatabaseOptions#databaseUrl} value.
   */
  public static final String SSM_PATH_DATABASE_URL = "db/url";

  /**
   * The path of the SSM parameter that should be used to provide the {@link DatabaseOptions} {@link
   * DatabaseOptions#databaseUsername} value.
   */
  public static final String SSM_PATH_DATABASE_USERNAME = "db/username";

  /**
   * The path of the SSM parameter that should be used to provide the {@link DatabaseOptions} {@link
   * DatabaseOptions#databasePassword} value.
   */
  public static final String SSM_PATH_DATABASE_PASSWORD = "db/password";

  /**
   * The path of the SSM parameter that should be used to provide the {@link DatabaseOptions} {@link
   * DatabaseOptions#maxPoolSize} value.
   */
  public static final String SSM_PATH_DATABASE_MAX_POOL_SIZE = "db/max_connections";

  /**
   * The path of the SSM parameter that should be used to provide the {@link MetricOptions} {@link
   * MetricOptions#getNewRelicMetricKey()} value.
   */
  public static final String SSM_PATH_NEW_RELIC_METRIC_KEY = "new_relic/metrics/license_key";

  /**
   * The path of the SSM parameter that should be used to provide the {@link MetricOptions} {@link
   * MetricOptions#getNewRelicAppName()} value.
   */
  public static final String SSM_PATH_NEW_RELIC_APP_NAME = "new_relic/app_name";

  /**
   * The path of the SSM parameter that should be used to provide the {@link MetricOptions} {@link
   * MetricOptions#getNewRelicMetricHost()} value.
   */
  public static final String SSM_PATH_NEW_RELIC_METRIC_HOST = "new_relic/metrics/host";

  /**
   * The path of the SSM parameter that should be used to provide the {@link MetricOptions} {@link
   * MetricOptions#getNewRelicMetricPath()} value.
   */
  public static final String SSM_PATH_NEW_RELIC_METRIC_PATH = "new_relic/metrics/path";

  /**
   * The path of the SSM parameter that should be used to provide the {@link MetricOptions} {@link
   * MetricOptions#getNewRelicMetricPeriod()} value.
   */
  public static final String SSM_PATH_NEW_RELIC_METRIC_PERIOD = "new_relic/metrics/period";

  /** Name of setting containing alternative endpoint URL for AWS services. */
  public static final String ENV_VAR_AWS_ENDPOINT = "AWS_ENDPOINT";

  /** Name of setting containing region name for AWS services. */
  public static final String ENV_VAR_AWS_REGION = "AWS_REGION";

  /** Name of setting containing access key for AWS services. */
  public static final String ENV_VAR_AWS_ACCESS_KEY = "AWS_ACCESS_KEY";

  /** Name of setting containing secret key for AWS services. */
  public static final String ENV_VAR_AWS_SECRET_KEY = "AWS_SECRET_KEY";

  /**
   * Creates appropriate {@link HikariDataSourceFactory} based on provided {@link DatabaseOptions}
   * and {@link AwsClientConfig} if the data source should use RDS authentication.
   *
   * @param databaseOptions used to configure the resulting {@link DataSourceFactory}
   * @param awsClientConfig used to configure authentication if the {@link
   *     DatabaseOptions#authenticationType} is {@link AuthenticationType#RDS}
   * @return factory for creating data sources
   */
  public HikariDataSourceFactory createDataSourceFactory(
      DatabaseOptions databaseOptions, AwsClientConfig awsClientConfig) {
    if (databaseOptions.getAuthenticationType() == DatabaseOptions.AuthenticationType.RDS) {
      return RdsDataSourceFactory.builder()
          .awsClientConfig(awsClientConfig)
          .databaseOptions(databaseOptions)
          .build();
    } else {
      return new DefaultHikariDataSourceFactory(databaseOptions);
    }
  }

  /**
   * Reads metric options from a {@link ConfigLoader}.
   *
   * @param config used to read and parse configuration values
   * @return the metric options
   */
  protected static MetricOptions loadMetricOptions(ConfigLoader config) {
    Optional<String> newRelicMetricKey = config.stringOptionEmptyOK(SSM_PATH_NEW_RELIC_METRIC_KEY);
    Optional<String> newRelicAppName = config.stringOptionEmptyOK(SSM_PATH_NEW_RELIC_APP_NAME);
    Optional<String> newRelicMetricHost =
        config.stringOptionEmptyOK(SSM_PATH_NEW_RELIC_METRIC_HOST);
    Optional<String> newRelicMetricPath =
        config.stringOptionEmptyOK(SSM_PATH_NEW_RELIC_METRIC_PATH);
    Optional<Integer> newRelicMetricPeriod = config.intOption(SSM_PATH_NEW_RELIC_METRIC_PERIOD);

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
            .enumOption(SSM_PATH_DATABASE_AUTH_TYPE, DatabaseOptions.AuthenticationType.class)
            .orElse(DatabaseOptions.AuthenticationType.JDBC);
    String databaseUrl = config.stringValue(SSM_PATH_DATABASE_URL);
    String databaseUsername = config.stringValue(SSM_PATH_DATABASE_USERNAME);
    String databasePassword = config.stringValue(SSM_PATH_DATABASE_PASSWORD);
    Optional<Integer> databaseMaxPoolSize =
        config.positiveIntOption(SSM_PATH_DATABASE_MAX_POOL_SIZE);

    return DatabaseOptions.builder()
        .authenticationType(databaseAuthType)
        .databaseUrl(databaseUrl)
        .databaseUsername(databaseUsername)
        .databasePassword(databasePassword)
        .maxPoolSize(databaseMaxPoolSize.orElse(Runtime.getRuntime().availableProcessors()))
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
        .region(config.parsedOption(ENV_VAR_AWS_REGION, Region.class, Region::of).orElse(null))
        .endpointOverride(
            config.parsedOption(ENV_VAR_AWS_ENDPOINT, URI.class, URI::create).orElse(null))
        .accessKey(config.stringValue(ENV_VAR_AWS_ACCESS_KEY, null))
        .secretKey(config.stringValue(ENV_VAR_AWS_SECRET_KEY, null))
        .build();
  }

  /**
   * Creates a {@link SqsClient} instance using settings from the provided {@link AwsClientConfig}.
   *
   * @param awsClientConfig used to configure AWS services
   * @return the {@link SqsClient}
   */
  public static SqsClient createSqsClient(AwsClientConfig awsClientConfig) {
    final var clientBuilder = SqsClient.builder();
    awsClientConfig.configureAwsService(clientBuilder);
    return clientBuilder.build();
  }
}
