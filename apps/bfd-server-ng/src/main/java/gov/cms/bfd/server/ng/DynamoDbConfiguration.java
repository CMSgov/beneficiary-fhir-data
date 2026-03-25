package gov.cms.bfd.server.ng;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfiguration {

  private static final String BFD_ENV_LOCAL = "local";

  @Bean
  public DynamoDbClient dynamoDbClient(
      AwsCredentialsProvider credentialsProvider,
      AwsRegionProvider regionProvider,
      gov.cms.bfd.server.ng.Configuration config) {
    if (config.getEnv().equals(BFD_ENV_LOCAL)) {
      return DynamoDbClient.builder()
          .endpointOverride(URI.create("http://localhost:8000"))
          .region(Region.US_EAST_1)
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create("dummy-key", "dummy-secret")))
          .build();
    }
    return DynamoDbClient.builder()
        .region(regionProvider.getRegion())
        .credentialsProvider(credentialsProvider)
        .build();
  }
}
