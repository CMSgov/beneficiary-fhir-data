package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.time.Clock;
import javax.annotation.concurrent.GuardedBy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@AllArgsConstructor
public class RdsDataSourceFactory implements DataSourceFactory {
  private static final long TokenTTLMillis = 5_000;
  private final Clock clock;
  private final String databaseUser;
  private final String databaseHost;
  private final int databasePort;
  private final HikariDataSourceFactory realDataSourceFactory;
  private final RdsClient rdsClient;

  public RdsDataSourceFactory(
      Clock clock, AwsClientConfig rdsClientConfig, DatabaseOptions databaseOptions) {
    this.clock = clock;
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
    rdsClient = createRdsClient(rdsClientConfig);
  }

  public RdsDataSourceFactory(AwsClientConfig rdsClientConfig, DatabaseOptions databaseOptions) {
    this(Clock.systemUTC(), rdsClientConfig, databaseOptions);
  }

  @Override
  public HikariDataSource createDataSource() {
    return realDataSourceFactory.createDataSource();
  }

  String generateToken() {
    var tokenRequest =
        GenerateAuthenticationTokenRequest.builder()
            .credentialsProvider(ProfileCredentialsProvider.create())
            .username(databaseUser)
            .hostname(databaseHost)
            .port(databasePort)
            .build();

    return rdsClient.utilities().generateAuthenticationToken(tokenRequest);
  }

  static RdsClient createRdsClient(AwsClientConfig awsClientConfig) {
    RdsClientBuilder builder = RdsClient.builder();
    awsClientConfig.configureAwsService(builder);
    builder.credentialsProvider(ProfileCredentialsProvider.create());
    return builder.build();
  }

  static IllegalArgumentException reportInvalidDatabaseOptions() {
    return new IllegalArgumentException(
        "RDS Authentication must be enabled and a valid JDBC URL must include host and port");
  }

  @NoArgsConstructor
  public class RdsHikariDataSource extends HikariDataSource {
    /**
     * Because we are subclassing {@link HikariDataSource} its safest not to use a synchronized
     * method to synchronized token updates. Instead we synchronize on this private object.
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
          expires = now + TokenTTLMillis;
        }
        return token;
      }
    }
  }
}
