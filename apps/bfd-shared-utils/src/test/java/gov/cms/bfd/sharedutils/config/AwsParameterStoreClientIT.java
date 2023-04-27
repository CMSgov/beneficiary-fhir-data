package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Integration test for {@link AwsParameterStoreClient}. */
@Testcontainers
public class AwsParameterStoreClientIT {
  /** The localstack image and version to use. */
  private static final DockerImageName localstackImage =
      DockerImageName.parse("localstack/localstack:2.0.2");

  /** Automatically creates and destroys a localstack SSM service container. */
  @Container
  public LocalStackContainer localstack =
      new LocalStackContainer(localstackImage).withServices(LocalStackContainer.Service.SSM);

  /** Upload some variables and verify that we can retrieve them. */
  @Test
  public void testGetParameters() {
    final var ssmClient =
        AWSSimpleSystemsManagementClient.builder()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                    localstack.getEndpointOverride(LocalStackContainer.Service.SSM).toString(),
                    localstack.getRegion()))
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())))
            .build();

    // These are the parameters we hope to retrieve.
    final String basePath = "/test";
    ssmClient.putParameter(
        new PutParameterRequest()
            .withName(basePath + "/port")
            .withValue("18")
            .withType(ParameterType.String));
    ssmClient.putParameter(
        new PutParameterRequest()
            .withName(basePath + "/mascot")
            .withValue("alpaca")
            .withType(ParameterType.String));

    // This should be ignored because nested parameters are ignored when retrieving parameters.
    ssmClient.putParameter(
        new PutParameterRequest()
            .withName(basePath + "/nested/ignoremeplease")
            .withValue("You were supposed to ignore me!")
            .withType(ParameterType.String));

    final var expectedParameters =
        ImmutableMap.<String, String>builder().put("port", "18").put("mascot", "alpaca");

    final var batchSize = 4;

    // Insert extra parameters so that we can test batching .
    for (int i = 1; i < 2 * batchSize; ++i) {
      String paramName = "extra." + i;
      String paramPath = basePath + "/" + paramName;
      String paramValue = "value-" + i;
      ssmClient.putParameter(
          new PutParameterRequest()
              .withName(paramPath)
              .withValue(paramValue)
              .withType(ParameterType.String));
      expectedParameters.put(paramName, paramValue);
    }

    final var client = new AwsParameterStoreClient(ssmClient, batchSize);
    var actualParameters = client.loadParametersAtPath(basePath);
    assertEquals(expectedParameters.build(), actualParameters);
  }
}
