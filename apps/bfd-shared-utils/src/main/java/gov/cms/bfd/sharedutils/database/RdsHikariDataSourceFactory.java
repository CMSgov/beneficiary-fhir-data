package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;

/**
 * Implementation of {@link HikariDataSourceFactory} that creates instances of {@link
 * HikariDataSource} objects that use temporary auth tokens requested from RDS instead of a fixed
 * password. {@see
 * https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html}.
 */
public class RdsHikariDataSourceFactory extends HikariDataSourceFactory {
  /**
   * A short delay between token requests so that constructing a new thread pool doesn't need to
   * trigger hundreds of token requests at the same time. According to the AWS documentation a token
   * is valid for 15 minutes but we can fetch a new one more frequently just to be safe.
   */
  static final long DEFAULT_TOKEN_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

  /** Configuration settings for {@link RdsClient}. */
  @Getter private final AwsClientConfig awsClientConfig;

  /** Used to configure {@link RdsHikariDataSource} instances. */
  @Getter(AccessLevel.PACKAGE)
  @VisibleForTesting
  private final RdsHikariDataSource.Config dataSourceConfig;

  /**
   * Initializes an instance. The {@link DatabaseOptions#authenticationType} must be set to {@link
   * DatabaseOptions.AuthenticationType#RDS}.
   *
   * @param clock optional, used to get current time
   * @param tokenTtlMillis optional, minimum millis between token requests
   * @param awsClientConfig used to config {@link RdsClient} instances
   * @param databaseOptions used to configure {@link RdsHikariDataSource} instances
   */
  @Builder
  private RdsHikariDataSourceFactory(
      @Nullable Clock clock,
      @Nullable Long tokenTtlMillis,
      @Nonnull AwsClientConfig awsClientConfig,
      @Nonnull DatabaseOptions databaseOptions) {
    super(databaseOptions);
    var configBuilder = RdsHikariDataSource.Config.builder();
    configBuilder.clock(clock == null ? Clock.systemUTC() : clock);
    configBuilder.tokenTtlMillis(
        tokenTtlMillis == null ? DEFAULT_TOKEN_TTL_MILLIS : tokenTtlMillis);
    if (databaseOptions.getAuthenticationType() != DatabaseOptions.AuthenticationType.RDS) {
      throw reportInvalidDatabaseOptions();
    }
    configBuilder.databaseUser(databaseOptions.getDatabaseUsername());
    configBuilder.databaseHost(
        databaseOptions
            .getDatabaseHost()
            .orElseThrow(RdsHikariDataSourceFactory::reportInvalidDatabaseOptions));
    configBuilder.databasePort(
        databaseOptions
            .getDatabasePort()
            .orElseThrow(RdsHikariDataSourceFactory::reportInvalidDatabaseOptions));
    this.awsClientConfig = awsClientConfig;
    dataSourceConfig = configBuilder.build();
  }

  @Override
  public RdsHikariDataSource createDataSource(
      @Nullable Properties properties, MetricRegistry metricRegistry) {
    final var rdsClient = getRdsClient();
    // the dataSource will take care of closing this when its close method is called
    final var dataSource = createRdsHikariDataSource(rdsClient);
    configureDataSource(dataSource, properties, metricRegistry);
    return dataSource;
  }

  /**
   * Creates a {@link RdsClient}.
   *
   * @return the RDS client
   */
  private RdsClient getRdsClient() {
    final var rdsClientBuilder = createRdsClientBuilder();
    awsClientConfig.configureAwsService(rdsClientBuilder);
    rdsClientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
    return rdsClientBuilder.build();
  }

  /**
   * Creates a {@link RdsClientBuilder}.
   *
   * @return the builder
   */
  @VisibleForTesting
  RdsClientBuilder createRdsClientBuilder() {
    return RdsClient.builder();
  }

  /**
   * Creates a {@link RdsHikariDataSource} using the provided {@link RdsClient}.
   *
   * @param rdsClient used to call RDS API
   * @return the data source
   */
  @VisibleForTesting
  RdsHikariDataSource createRdsHikariDataSource(RdsClient rdsClient) {
    return new RdsHikariDataSource(dataSourceConfig, rdsClient);
  }

  /**
   * Creates an exception with appropriate message to indicate our {@link DatabaseOptions} was
   * invalid. Defined as a method since we need to generate a message in multiple places and to make
   * it easy to use with {@link Optional#orElse}.
   *
   * @return the exception
   */
  static IllegalArgumentException reportInvalidDatabaseOptions() {
    return new IllegalArgumentException(
        "RDS Authentication must be enabled and a valid JDBC URL must include host and port");
  }
}
