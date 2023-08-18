package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.net.URI;
import java.time.Duration;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClientBuilder;

public class RdsClientConfig extends AwsClientConfig {
  @Getter private final String dbInstanceId;
  @Getter private final int dbPort;
  @Getter private final String username;

  @Builder(builderClassName = "RdsBuilder", builderMethodName = "rdsBuilder")
  public RdsClientConfig(
      @Nullable Region region,
      @Nullable URI endpointOverride,
      @Nullable String accessKey,
      @Nullable String secretKey,
      String dbInstanceId,
      int dbPort,
      String username) {
    super(region, endpointOverride, accessKey, secretKey);
    this.dbInstanceId = dbInstanceId;
    this.dbPort = dbPort;
    this.username = username;
  }

  public void configureRdsService(RdsClientBuilder builder) {
    region.ifPresent(builder::region);
    endpointOverride.ifPresent(builder::endpointOverride);
    if (endpointOverride.isPresent()) {
      // AWS services can be slow under colima in some environments.
      // This makes the client more tolerant of delays.
      builder.overrideConfiguration(
          configBuilder ->
              configBuilder
                  .retryPolicy(b -> b.numRetries(10))
                  .apiCallAttemptTimeout(Duration.ofSeconds(10)));
    }
    if (accessKey.isPresent() && secretKey.isPresent()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
    }
  }
}
