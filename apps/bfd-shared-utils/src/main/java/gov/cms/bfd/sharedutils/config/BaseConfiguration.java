package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.database.AwsWrapperDataSourceFactory;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions.AuthenticationType;
import gov.cms.bfd.sharedutils.database.DatabaseOptions.DataSourceType;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.RdsHikariDataSourceFactory;
import java.net.URI;
import java.util.concurrent.TimeUnit;
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
   * DatabaseOptions#dataSourceType} value.
   */
  public static final String SSM_PATH_DATABASE_DATA_SOURCE_TYPE = "db/data_source_type";

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
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#maximumPoolSize} value.
   */
  public static final String SSM_PATH_DB_HIKARI_MAX_POOL_SIZE = "db/hikari/max_pool_size";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#minimumIdleConnections}
   * value.
   */
  public static final String SSM_PATH_DB_HIKARI_MIN_IDLE_CONNECTIONS =
      "db/hikari/min_idle_connections";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#idleTimeoutMs} value.
   */
  public static final String SSM_PATH_DB_HIKARI_IDLE_TIMEOUT_MS = "db/hikari/idle_timeout_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link
   * DatabaseOptions.HikariOptions#initializationFailTimeoutMs} value.
   */
  public static final String SSM_PATH_DB_HIKARI_INIT_FAIL_TIMEOUT_MS =
      "db/hikari/init_fail_timeout_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#connectionTimeoutMs} value.
   */
  public static final String SSM_PATH_DB_HIKARI_CONNECTION_TIMEOUT_MS =
      "db/hikari/connection_timeout_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#keepaliveTimeMs} value.
   */
  public static final String SSM_PATH_DB_HIKARI_KEEPALIVE_TIMEOUT_MS =
      "db/hikari/keepalive_timeout_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#validationTimeoutMs} value.
   */
  public static final String SSM_PATH_DB_HIKARI_VALIDATION_TIMEOUT_MS =
      "db/hikari/validation_timeout_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.HikariOptions} {@link DatabaseOptions.HikariOptions#maxConnectionLifetimeMs}
   * value.
   */
  public static final String SSM_PATH_DB_HIKARI_MAX_CONNECTION_LIFETIME_MS =
      "db/hikari/max_connection_lifetime_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.AwsJdbcWrapperOptions} {@link
   * DatabaseOptions.AwsJdbcWrapperOptions#useCustomPreset} value.
   */
  public static final String SSM_PATH_DB_WRAPPER_USE_CUSTOM_PRESET = "db/wrapper/use_custom_preset";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.AwsJdbcWrapperOptions} {@link
   * DatabaseOptions.AwsJdbcWrapperOptions#basePresetCode} value.
   */
  public static final String SSM_PATH_DB_WRAPPER_BASE_PRESET = "db/wrapper/base_preset";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.AwsJdbcWrapperOptions} {@link DatabaseOptions.AwsJdbcWrapperOptions#plugins}
   * value.
   */
  public static final String SSM_PATH_DB_WRAPPER_PLUGINS_CSV = "db/wrapper/plugins_csv";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.AwsJdbcWrapperOptions} {@link
   * DatabaseOptions.AwsJdbcWrapperOptions#hostSelectorStrategy} value.
   */
  public static final String SSM_PATH_DB_WRAPPER_HOST_SELECTOR_STRATEGY =
      "db/wrapper/host_selector_strategy";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.AwsJdbcWrapperOptions} {@link
   * DatabaseOptions.AwsJdbcWrapperOptions#clusterTopologyRefreshRateMs} value.
   */
  public static final String SSM_PATH_DB_WRAPPER_CLUSTER_TOPOLOGY_REFRESH_RATE_MS =
      "db/wrapper/cluster_topology_refresh_rate_ms";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * DatabaseOptions.AwsJdbcWrapperOptions} {@link
   * DatabaseOptions.AwsJdbcWrapperOptions#instanceStateMonitorRefreshRateMs} value.
   */
  public static final String SSM_PATH_DB_WRAPPER_INSTANCE_STATE_MONITOR_REFRESH_RATE_MS =
      "db/wrapper/instance_state_monitor_refresh_rate_ms";

  /** Name of setting containing alternative endpoint URL for AWS services. */
  public static final String ENV_VAR_AWS_ENDPOINT = "AWS_ENDPOINT";

  /** Name of setting containing region name for AWS services. */
  public static final String ENV_VAR_AWS_REGION = "AWS_REGION";

  /** Name of setting containing access key for AWS services. */
  public static final String ENV_VAR_AWS_ACCESS_KEY = "AWS_ACCESS_KEY";

  /** Name of setting containing secret key for AWS services. */
  public static final String ENV_VAR_AWS_SECRET_KEY = "AWS_SECRET_KEY";

  /**
   * Creates appropriate {@link DataSourceFactory} based on provided {@link DatabaseOptions} and
   * {@link AwsClientConfig} if the data source should use RDS authentication.
   *
   * @param databaseOptions used to configure the resulting {@link DataSourceFactory}
   * @param awsClientConfig used to configure authentication if the {@link
   *     DatabaseOptions#authenticationType} is {@link AuthenticationType#RDS}
   * @return factory for creating data sources
   */
  protected DataSourceFactory createDataSourceFactory(
      DatabaseOptions databaseOptions, AwsClientConfig awsClientConfig) {
    // TODO: The AWS JDBC Wrapper has the capability to use RDS authentication. When RDS auth
    // support is added, this should be updated such that the wrapper could also be used for it
    Preconditions.checkArgument(
        databaseOptions.getDataSourceType() != DataSourceType.AWS_WRAPPER
            || databaseOptions.getAuthenticationType() != AuthenticationType.RDS,
        "RDS authentication is unsupported when using the AWS JDBC Wrapper");
    if (databaseOptions.getAuthenticationType() == DatabaseOptions.AuthenticationType.RDS) {
      return RdsHikariDataSourceFactory.builder()
          .awsClientConfig(awsClientConfig)
          .databaseOptions(databaseOptions)
          .build();
    } else {
      if (databaseOptions.getDataSourceType() == DataSourceType.AWS_WRAPPER) {
        return new AwsWrapperDataSourceFactory(databaseOptions, awsClientConfig);
      }

      // dataSourceType should never be null (if it is, it defaults to HIKARI), but this covers all
      // cases exhaustively. If additional cases are introduced, HIKARI should be handled
      // explicitly.
      return new HikariDataSourceFactory(databaseOptions);
    }
  }

  /**
   * Creates a {@link HikariDataSource} based on provided {@link DatabaseOptions} and {@link
   * AwsClientConfig} if the data source should use RDS authentication.
   *
   * @param databaseOptions used to configure the resulting {@link HikariDataSourceFactory}
   * @param awsClientConfig used to configure authentication if the {@link
   *     DatabaseOptions#authenticationType} is {@link AuthenticationType#RDS}
   * @return a {@link HikariDataSourceFactory} that creates {@link HikariDataSource}s
   */
  protected HikariDataSourceFactory createHikariDataSourceFactory(
      DatabaseOptions databaseOptions, AwsClientConfig awsClientConfig) {
    // FIXME: This method provided as an escape hatch for Migrator and Pipeline as of
    // introducing the AWS JDBC Wrapper and commonizing configuration classes across all
    // applications in BFD-3508. This method should be removed in favor of refactoring the
    // Migrator and Pipeline to support the AwsWrapperDataSource properly.
    if (databaseOptions.getAuthenticationType() == DatabaseOptions.AuthenticationType.RDS) {
      return RdsHikariDataSourceFactory.builder()
          .awsClientConfig(awsClientConfig)
          .databaseOptions(databaseOptions)
          .build();
    } else {
      return new HikariDataSourceFactory(databaseOptions);
    }
  }

  /**
   * Reads the database options from a {@link ConfigLoader}.
   *
   * @param config used to read and parse configuration values
   * @return the database options
   */
  protected static DatabaseOptions loadDatabaseOptions(ConfigLoader config) {
    final var databaseAuthType =
        config
            .enumOption(SSM_PATH_DATABASE_AUTH_TYPE, DatabaseOptions.AuthenticationType.class)
            .orElse(DatabaseOptions.AuthenticationType.JDBC);
    final var databaseDataSourceType =
        config
            .enumOption(SSM_PATH_DATABASE_DATA_SOURCE_TYPE, DataSourceType.class)
            .orElse(DataSourceType.HIKARI);
    final var databaseUrl = config.stringValue(SSM_PATH_DATABASE_URL);
    final var databaseUsername = config.stringValue(SSM_PATH_DATABASE_USERNAME);
    final var databasePassword = config.stringValue(SSM_PATH_DATABASE_PASSWORD);

    // Get Hikari configuration; all default values (except for max pool size and min idle
    // connections) are taken directly from Hikari defaults
    final var hikariMaxPoolSize =
        config.positiveIntValue(
            SSM_PATH_DB_HIKARI_MAX_POOL_SIZE, Runtime.getRuntime().availableProcessors());
    final var hikariMinIdleConnections =
        config.positiveIntValue(
            SSM_PATH_DB_HIKARI_MIN_IDLE_CONNECTIONS, Runtime.getRuntime().availableProcessors());
    final var hikariIdleTimeoutMs =
        config.positiveLongValue(SSM_PATH_DB_HIKARI_IDLE_TIMEOUT_MS, TimeUnit.MINUTES.toMillis(10));
    // Can be negative to specify that initial connection attempts are bypassed and the pool should
    // be immediately started
    final var hikariInitFailTimeoutMs =
        config.longValue(SSM_PATH_DB_HIKARI_INIT_FAIL_TIMEOUT_MS, 1);
    final var hikariConnectionTimeoutMs =
        config.positiveLongValue(
            SSM_PATH_DB_HIKARI_CONNECTION_TIMEOUT_MS, TimeUnit.SECONDS.toMillis(30));
    final var hikariKeepaliveTimeoutMs =
        config.positiveLongValue(SSM_PATH_DB_HIKARI_KEEPALIVE_TIMEOUT_MS, 0);
    final var hikariValidationTimeoutMs =
        config.positiveLongValue(
            SSM_PATH_DB_HIKARI_VALIDATION_TIMEOUT_MS, TimeUnit.SECONDS.toMillis(5));
    final var hikariMaxConnectionLifetimeMs =
        config.positiveLongValue(
            SSM_PATH_DB_HIKARI_MAX_CONNECTION_LIFETIME_MS, TimeUnit.MINUTES.toMillis(30));
    final var hikariOptions =
        DatabaseOptions.HikariOptions.builder()
            .maximumPoolSize(hikariMaxPoolSize)
            .minimumIdleConnections(hikariMinIdleConnections)
            .idleTimeoutMs(hikariIdleTimeoutMs)
            .initializationFailTimeoutMs(hikariInitFailTimeoutMs)
            .connectionTimeoutMs(hikariConnectionTimeoutMs)
            .keepaliveTimeMs(hikariKeepaliveTimeoutMs)
            .validationTimeoutMs(hikariValidationTimeoutMs)
            .maxConnectionLifetimeMs(hikariMaxConnectionLifetimeMs)
            .build();

    final var dbOptionsBuilder =
        DatabaseOptions.builder()
            .authenticationType(databaseAuthType)
            .dataSourceType(databaseDataSourceType)
            .databaseUrl(databaseUrl)
            .databaseUsername(databaseUsername)
            .databasePassword(databasePassword)
            .hikariOptions(hikariOptions);

    // Get AWS Wrapper configuration
    if (databaseDataSourceType == DataSourceType.AWS_WRAPPER) {
      final var wrapperUseCustomPreset =
          config.booleanValue(SSM_PATH_DB_WRAPPER_USE_CUSTOM_PRESET, false);
      final var wrapperBasePresetCode = config.stringValue(SSM_PATH_DB_WRAPPER_BASE_PRESET, "E");
      // Default plugin list is the same as the default list of plugins when left unspecified
      final var wrapperPluginsCsv =
          config.stringValue(
              SSM_PATH_DB_WRAPPER_PLUGINS_CSV, "auroraConnectionTracker,failover,efm2");
      final var wrapperHostSelectorStrategy =
          config.stringValue(SSM_PATH_DB_WRAPPER_HOST_SELECTOR_STRATEGY, "roundRobin");
      final var wrapperClusterTopologyRefreshRateMs =
          config.longValue(SSM_PATH_DB_WRAPPER_CLUSTER_TOPOLOGY_REFRESH_RATE_MS, 30000L);
      final var wrapperInstanceStateMonitorRefreshRateMs =
          config.longValue(SSM_PATH_DB_WRAPPER_INSTANCE_STATE_MONITOR_REFRESH_RATE_MS, 5000L);
      final var wrapperOptions =
          DatabaseOptions.AwsJdbcWrapperOptions.builder()
              .useCustomPreset(wrapperUseCustomPreset)
              .basePresetCode(wrapperBasePresetCode)
              .pluginsCsv(wrapperPluginsCsv)
              .hostSelectorStrategy(wrapperHostSelectorStrategy)
              .clusterTopologyRefreshRateMs(wrapperClusterTopologyRefreshRateMs)
              .instanceStateMonitorRefreshRateMs(wrapperInstanceStateMonitorRefreshRateMs)
              .build();

      dbOptionsBuilder.awsJdbcWrapperOptions(wrapperOptions);
    }

    return dbOptionsBuilder.build();
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
