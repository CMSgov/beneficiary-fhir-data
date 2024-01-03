package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.AbstractLocalStackTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

/** Integration test for {@link AwsParameterStoreClient}. */
public class AwsParameterStoreClientIT extends AbstractLocalStackTest {
  /** Shared client used by all tests. */
  private SsmClient ssmClient;

  /** Create a {@link SsmClient} before each test. */
  @BeforeEach
  void setUp() {
    ssmClient =
        SsmClient.builder()
            .region(Region.of(localstack.getRegion()))
            .endpointOverride(localstack.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .build();
  }

  /** Close the {@link SsmClient} after each test. */
  @AfterEach
  void tearDown() {
    ssmClient.close();
  }

  /** Upload some variables and verify that we can retrieve them. */
  @Test
  public void testGetParametersAtPath() {
    // These are the parameters we hope to retrieve.
    final var expectedParameters = new HashMap<String, String>();
    final String basePath = "/flat";
    addValue(expectedParameters, basePath + "/port", "18", "port");
    addValue(expectedParameters, basePath + "/mascot", "alpaca", "mascot");

    // This should be ignored because nested parameters are ignored when retrieving parameters.
    ssmClient.putParameter(
        PutParameterRequest.builder()
            .name(basePath + "/nested/ignoremeplease")
            .value("You were supposed to ignore me!")
            .type(ParameterType.STRING)
            .build());

    // We'll verify the default batch size works
    final var batchSize = AwsParameterStoreClient.DEFAULT_BATCH_SIZE;

    // Insert extra parameters so that we can test batching .
    for (int i = 1; i < 2 * batchSize; ++i) {
      String paramName = "extra." + i;
      String paramPath = basePath + "/" + paramName;
      String paramValue = "value-" + i;
      addValue(expectedParameters, paramPath, paramValue, paramName);
    }

    final var client = new AwsParameterStoreClient(ssmClient, batchSize);
    var actualParameters = client.loadParametersAtPath(basePath, false);
    assertEquals(expectedParameters, actualParameters);
  }

  /** Upload some variables in a hierarchy and verify that we can retrieve them. */
  @Test
  public void testGetParametersInHierarchy() {
    // These are the parameters we hope to retrieve.
    final var expectedParameters = new HashMap<String, String>();
    final String basePath = "/tree";
    addValue(expectedParameters, basePath + "/mascot", "alpaca", "mascot");

    for (var folderName : List.of("alpha", "bravo", "charlie")) {
      final var folderPath = basePath + "/" + folderName;

      // put a value into the folder
      addValue(expectedParameters, folderPath + "/port", "443", folderName + "/" + "port");

      // put values into sub-folders within the folder
      for (var subFolderName : List.of("delta", "echo", "foxtrot")) {
        final var subFolderPath = folderPath + "/" + subFolderName;

        for (int i = 1; i < 5; ++i) {
          String paramName = "extra_" + i;
          String paramPath = subFolderPath + "/" + paramName;
          String paramValue = "value-" + i;
          String mapKey = folderName + "/" + subFolderName + "/" + paramName;
          addValue(expectedParameters, paramPath, paramValue, mapKey);
        }
      }
    }

    // We'll verify that a smaller batch size works
    final var batchSize = 3;
    final var client = new AwsParameterStoreClient(ssmClient, batchSize);
    var actualParameters = client.loadParametersAtPath(basePath, true);
    assertEquals(expectedParameters, actualParameters);
  }

  /**
   * Upload a parameter value and add it to the map of expected values.
   *
   * @param expectedParameters map of expected values
   * @param paramPath ssm path for parameter
   * @param paramValue parameter value
   * @param mapKey key to use in map of expected values
   */
  private void addValue(
      Map<String, String> expectedParameters, String paramPath, String paramValue, String mapKey) {
    ssmClient.putParameter(
        PutParameterRequest.builder()
            .name(paramPath)
            .value(paramValue)
            .type(ParameterType.STRING)
            .build());
    expectedParameters.put(mapKey, paramValue);
  }
}
