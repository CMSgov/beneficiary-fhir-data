package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import javax.annotation.concurrent.GuardedBy;

@AllArgsConstructor
public class RdsDataSourceFactory implements DataSourceFactory {
  private static final long TokenTTLMillis = 5_000;
  private final Clock clock;
  private final RdsClientConfig rdsClientConfig;
  private final HikariDataSourceFactory realDataSourceFactory;
  private final RdsClient rdsClient;

  public RdsDataSourceFactory(
      Clock clock, RdsClientConfig rdsClientConfig, DatabaseOptions databaseOptions) {
    this.clock = clock;
    this.rdsClientConfig = rdsClientConfig;
    realDataSourceFactory = new HikariDataSourceFactory(databaseOptions, RdsHikariDataSource::new);
    rdsClient = createRdsClient(rdsClientConfig);
  }

  public RdsDataSourceFactory(RdsClientConfig rdsClientConfig, DatabaseOptions databaseOptions) {
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
            .username(rdsClientConfig.getUsername())
            .port(rdsClientConfig.getDbPort())
            .hostname(rdsClientConfig.getDbInstanceId())
            .build();

    return rdsClient.utilities().generateAuthenticationToken(tokenRequest);
  }

  static RdsClient createRdsClient(RdsClientConfig rdsClientConfig) {
    RdsClientBuilder builder = RdsClient.builder();
    rdsClientConfig.configureRdsService(builder);
    builder.credentialsProvider(ProfileCredentialsProvider.create());
    return builder.build();
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
