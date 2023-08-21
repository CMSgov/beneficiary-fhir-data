package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.AbstractLocalStackTest;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

/** Integration test for {@link AwsParameterStoreClient}. */
public class AwsParameterStoreClientIT extends AbstractLocalStackTest {
  /** Upload some variables and verify that we can retrieve them. */
  @Test
  public void testGetParameters() {
    final var ssmClient =
        SsmClient.builder()
            .region(Region.of(localstack.getRegion()))
            .endpointOverride(localstack.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .build();

    // These are the parameters we hope to retrieve.
    final String basePath = "/test";
    ssmClient.putParameter(
        PutParameterRequest.builder()
            .name(basePath + "/port")
            .value("18")
            .type(ParameterType.STRING)
            .build());
    ssmClient.putParameter(
        PutParameterRequest.builder()
            .name(basePath + "/mascot")
            .value("alpaca")
            .type(ParameterType.STRING)
            .build());

    // This should be ignored because nested parameters are ignored when retrieving parameters.
    ssmClient.putParameter(
        PutParameterRequest.builder()
            .name(basePath + "/nested/ignoremeplease")
            .value("You were supposed to ignore me!")
            .type(ParameterType.STRING)
            .build());

    final var expectedParameters =
        ImmutableMap.<String, String>builder().put("port", "18").put("mascot", "alpaca");

    final var batchSize = AwsParameterStoreClient.DEFAULT_BATCH_SIZE;

    // Insert extra parameters so that we can test batching .
    for (int i = 1; i < 2 * batchSize; ++i) {
      String paramName = "extra." + i;
      String paramPath = basePath + "/" + paramName;
      String paramValue = "value-" + i;
      ssmClient.putParameter(
          PutParameterRequest.builder()
              .name(paramPath)
              .value(paramValue)
              .type(ParameterType.STRING)
              .build());
      expectedParameters.put(paramName, paramValue);
    }

    final var client = new AwsParameterStoreClient(ssmClient, batchSize);
    var actualParameters = client.loadParametersAtPath(basePath);
    assertEquals(expectedParameters.build(), actualParameters);
  }
}
