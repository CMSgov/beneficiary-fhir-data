package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@AllArgsConstructor
public class RdsDataSourceFactory implements DataSourceFactory {
  private final RdsClientConfig rdsClientConfig;
  private final HikariDataSourceFactory realDataSourceFactory;
  private final RdsClient rdsClient;

  public RdsDataSourceFactory(RdsClientConfig rdsClientConfig, DatabaseOptions databaseOptions) {
    this.rdsClientConfig = rdsClientConfig;
    realDataSourceFactory = new HikariDataSourceFactory(databaseOptions, RdsHikariDataSource::new);
    rdsClient = createRdsClient(rdsClientConfig);
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
  class RdsHikariDataSource extends HikariDataSource {
    @Override
    public String getPassword() {
      return generateToken();
    }
  }
}
