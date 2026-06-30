package gov.cms.bfd;

import java.net.URI;
import java.net.URISyntaxException;
import org.ministack.testcontainers.MiniStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Utility class containing code to assist in writing tests of SQS related functionality. */
public class SqsTestUtils {
  /**
   * Create a {@link SqsClient} configured for the SQS service in the provided {@link
   * MiniStackContainer}.
   *
   * @param miniStack the container info
   * @return the client
   */
  public static SqsClient createSqsClientForMiniStack(MiniStackContainer miniStack) {
    try {
      return SqsClient.builder()
          .region(Region.of(miniStack.getRegion()))
          .endpointOverride(new URI(miniStack.getEndpoint()))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(miniStack.getAccessKey(), miniStack.getSecretKey())))
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
