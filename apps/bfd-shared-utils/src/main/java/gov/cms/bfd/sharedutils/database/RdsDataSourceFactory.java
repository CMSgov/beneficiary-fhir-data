package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;

/**
 * Implementation of {@link DataSourceFactory} that creates instances of {@link HikariDataSource}
 * objects that use temporary auth tokens requested from RDS instead of a fixed password. {@see
 * https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html}.
 */
public class RdsDataSourceFactory extends HikariDataSourceFactory {
  /**
   * A short delay between token requests so that constructing a new thread pool doesn't need to
   * trigger hundreds of token requests at the same time. According to the AWS documentation a token
   * is valid for 15 minutes but we can fetch a new one more frequently just to be safe.
   */
  private static final long DEFAULT_TOKEN_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

  private final AwsClientConfig awsClientConfig;

  /** Used to configure {@link RdsHikariDataSource} instances. */
  private final RdsHikariDataSource.Config dataSourceConfig;

  /**
   * Initializes an instance. The {@link DatabaseOptions#authenticationType} must be set to {@link
   * DatabaseOptions.AuthenticationType#RDS}.
   *
   * @param clock optional, used to get current time
   * @param tokenTtlMillis optional, minimum millis between token requests
   * @param awsClientConfig optional, used to create an {@link RdsClient} if needed
   * @param databaseOptions used to configure a {@link HikariDataSource}
   */
  @Builder
  private RdsDataSourceFactory(
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
            .orElseThrow(RdsDataSourceFactory::reportInvalidDatabaseOptions));
    configBuilder.databasePort(
        databaseOptions
            .getDatabasePort()
            .orElseThrow(RdsDataSourceFactory::reportInvalidDatabaseOptions));
    this.awsClientConfig = awsClientConfig;
    dataSourceConfig = configBuilder.build();
  }

  /**
   * Constructs a {@link RdsHikariDataSource} instance.
   *
   * @return the data source
   */
  @Override
  public HikariDataSource createDataSource() {
    // the dataSource will take care of closing this when its close method is called
    RdsClient rdsClient = createRdsClient(awsClientConfig);
    HikariDataSource dataSource = new RdsHikariDataSource(dataSourceConfig, rdsClient);
    configureDataSource(dataSource);
    return dataSource;
  }

  /**
   * Creates a {@link RdsClient} configured with the provided {@link AwsClientConfig}.
   *
   * @param awsClientConfig used to configure the client
   * @return the client
   */
  private static RdsClient createRdsClient(AwsClientConfig awsClientConfig) {
    RdsClientBuilder builder = RdsClient.builder();
    awsClientConfig.configureAwsService(builder);
    builder.credentialsProvider(DefaultCredentialsProvider.create());
    return builder.build();
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
