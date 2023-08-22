package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

/**
 * Implementation of {@link DataSourceFactory} that creates instances of {@link HikariDataSource}
 * objects that use temporary auth tokens requested from RDS instead of a fixed password. {@see
 * https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html}.
 */
public class RdsDataSourceFactory implements DataSourceFactory {
  /**
   * A short delay between token requests so that constructing a new thread pool doesn't need to
   * trigger hundreds of token requests at the same time. According to the AWS documentation a token
   * is valid for 15 minutes but we can fetch a new one more frequently just to be safe.
   */
  private static final long DEFAULT_TOKEN_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

  /** Used to get the current time. */
  private final Clock clock;

  /** Minimum time between token requests. */
  private final long tokenTtlMillis;

  /** Used to communicate with RDS services to obtain tokens. */
  private final RdsClient rdsClient;

  /** IAM user id used in the token requests and to authenticate to the database. */
  private final String databaseUser;

  /** Database instance/host id. */
  private final String databaseHost;

  /** Database port. */
  private final int databasePort;

  /** Used to create configured instances of {@link RdsHikariDataSource}. */
  private final HikariDataSourceFactory realDataSourceFactory;

  /**
   * Initializes an instance. The {@link DatabaseOptions#authenticationType} must be set to {@link
   * DatabaseOptions.AuthenticationType#RDS}.
   *
   * @param clock optional, used to get current time
   * @param tokenTtlMillis optional, minimum millis between token requests
   * @param rdsClient optional, used to call the RDS API
   * @param awsClientConfig optional, used to create an {@link RdsClient} if needed
   * @param databaseOptions used to configure a {@link HikariDataSource}
   */
  @Builder
  private RdsDataSourceFactory(
      @Nullable Clock clock,
      @Nullable Long tokenTtlMillis,
      @Nullable RdsClient rdsClient,
      @Nullable AwsClientConfig awsClientConfig,
      @Nonnull DatabaseOptions databaseOptions) {
    this.clock = clock == null ? Clock.systemUTC() : clock;
    this.tokenTtlMillis = tokenTtlMillis == null ? DEFAULT_TOKEN_TTL_MILLIS : tokenTtlMillis;
    if (rdsClient != null) {
      this.rdsClient = rdsClient;
    } else if (awsClientConfig != null) {
      this.rdsClient = createRdsClient(awsClientConfig);
    } else {
      throw new IllegalArgumentException("either rdsClient or awsClientConfig must be provided");
    }
    if (databaseOptions.getAuthenticationType() != DatabaseOptions.AuthenticationType.RDS) {
      throw reportInvalidDatabaseOptions();
    }
    databaseUser = databaseOptions.getDatabaseUsername();
    databaseHost =
        databaseOptions
            .getDatabaseHost()
            .orElseThrow(RdsDataSourceFactory::reportInvalidDatabaseOptions);
    databasePort =
        databaseOptions
            .getDatabasePort()
            .orElseThrow(RdsDataSourceFactory::reportInvalidDatabaseOptions);
    realDataSourceFactory = new HikariDataSourceFactory(databaseOptions, RdsHikariDataSource::new);
  }

  /**
   * Constructs a {@link RdsHikariDataSource} instance.
   *
   * @return the data source
   */
  @Override
  public HikariDataSource createDataSource() {
    return realDataSourceFactory.createDataSource();
  }

  /**
   * Calls the RDS API to obtain a temporary token for use as a password.
   *
   * @return the token
   */
  String generateToken() {
    var tokenRequest =
        GenerateAuthenticationTokenRequest.builder()
            .username(databaseUser)
            .hostname(databaseHost)
            .port(databasePort)
            .build();

    return rdsClient.utilities().generateAuthenticationToken(tokenRequest);
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

  /**
   * An {@link HikariDataSource} subclass that looks up its password by calling the RDS API instead
   * of always returning a fixed password. Tokens are cached briefly to avoid making a large number
   * of simultaneous calls when the application is first started.
   */
  @NoArgsConstructor
  public class RdsHikariDataSource extends HikariDataSource {
    /**
     * Because we are subclassing {@link HikariDataSource} its safest not to use a synchronized
     * method to synchronize token updates. Instead we synchronize on this private object.
     */
    private final Object tokenLock = new Object();

    /** Time in millis at which we consider our token to have expired. */
    @GuardedBy("tokenLock")
    @Getter
    private long expires;

    /** Most recently generated token. */
    @GuardedBy("tokenLock")
    @Getter
    private String token;

    /**
     * Overrides default method to return a token obtained from RDS.
     *
     * @return a token from RDS
     */
    @Override
    public String getPassword() {
      synchronized (tokenLock) {
        long now = clock.millis();
        if (token == null || now > expires) {
          token = generateToken();
          expires = now + tokenTtlMillis;
        }
        return token;
      }
    }
  }
}
