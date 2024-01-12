package gov.cms.bfd;

import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Utility class containing code to assist in writing tests of SQS related functionality. */
public class SqsTestUtils {
  /**
   * Create a {@link SqsClient} configured for the SQS service in the provided {@link
   * LocalStackContainer}.
   *
   * @param localstack the container info
   * @return the client
   */
  public static SqsClient createSqsClientForLocalStack(LocalStackContainer localstack) {
    return SqsClient.builder()
        .region(Region.of(localstack.getRegion()))
        .endpointOverride(localstack.getEndpoint())
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
        .build();
  }
}
