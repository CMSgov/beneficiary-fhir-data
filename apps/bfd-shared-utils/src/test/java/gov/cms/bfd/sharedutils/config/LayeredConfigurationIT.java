package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.TestContainerConstants;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

/** Integration test for {@link AwsParameterStoreClient}. */
@Testcontainers
public class LayeredConfigurationIT {
  /** Automatically creates and destroys a localstack SSM service container. */
  @Container
  LocalStackContainer localstack =
      new LocalStackContainer(TestContainerConstants.LocalStackImageName)
          .withServices(LocalStackContainer.Service.SSM);

  /** Verifies that configuration sources are layered in the expected order. */
  @Test
  void shouldLayerConfigurationSources() throws IOException {
    // define everything we will need to include in our config
    final var ssmBasePath = "/test/";
    final var ssmCommonPath = ssmBasePath + "common/";
    final var ssmSpecificPath = ssmBasePath + "specific/";
    final var ssmRegion = localstack.getRegion();
    final var ssmEndpoint = localstack.getEndpointOverride(LocalStackContainer.Service.SSM);
    final var ssmAccessKey = localstack.getAccessKey();
    final var ssmSecretKey = localstack.getSecretKey();
    final var baseName = LayeredConfigurationIT.class.getSimpleName();
    final var propertiesFile = File.createTempFile(baseName, ".properties");
    final var nameA = baseName + "a";
    final var nameB = baseName + "b";
    final var nameC = baseName + "c";
    final var nameD = baseName + "d";
    final var nameE = baseName + "e";
    final var nameF = baseName + "f";
    final var nameZ = baseName + "z";

    // a comes from system property
    // b comes from env variable
    // c comes from property file
    // d comes from ssm specific
    // e comes from ssm common
    // f comes from defaults
    // z is undefined
    final var systemProperties =
        ImmutableMap.<String, String>builder().put(nameA, "a-system-property").build();
    final var envVars =
        ImmutableMap.<String, String>builder()
            .put(LayeredConfiguration.ENV_VAR_KEY_SSM_REGION, ssmRegion)
            .put(
                LayeredConfiguration.ENV_VAR_KEY_SSM_PARAMETER_PATH,
                ssmCommonPath + "," + ssmSpecificPath)
            .put(LayeredConfiguration.ENV_VAR_KEY_SSM_ENDPOINT, ssmEndpoint.toString())
            .put(LayeredConfiguration.ENV_VAR_KEY_SSM_ACCESS_KEY, ssmAccessKey)
            .put(LayeredConfiguration.ENV_VAR_KEY_SSM_SECRET_KEY, ssmSecretKey)
            .put(LayeredConfiguration.ENV_VAR_KEY_PROPERTIES_FILE, propertiesFile.getAbsolutePath())
            .put(nameA, "a-env-var")
            .put(nameB, "b-env-var")
            .build();
    final var fileProperties =
        ImmutableMap.<String, String>builder()
            .put(nameA, "a-file-property")
            .put(nameB, "b-file-property")
            .put(nameC, "c-file-property")
            .build();
    final var ssmSpecificParameters =
        ImmutableMap.<String, String>builder()
            .put(nameA, "a-ssm-specific-parameter")
            .put(nameB, "b-ssm-specific-parameter")
            .put(nameC, "c-ssm-specific-parameter")
            .put(nameD, "d-ssm-specific-parameter")
            .build();
    final var ssmCommonParameters =
        ImmutableMap.<String, String>builder()
            .put(nameA, "a-ssm-common-parameter")
            .put(nameB, "b-ssm-common-parameter")
            .put(nameC, "c-ssm-common-parameter")
            .put(nameD, "d-ssm-common-parameter")
            .put(nameE, "e-ssm-common-parameter")
            .build();
    final var defaultValues =
        ImmutableMap.<String, String>builder()
            .put(nameA, "a-default")
            .put(nameB, "b-default")
            .put(nameC, "c-default")
            .put(nameD, "d-default")
            .put(nameE, "e-default")
            .put(nameF, "f-default")
            .build();

    try {
      // set up all of our SSM parameters
      final var ssmClient =
          SsmClient.builder()
              .region(Region.of(ssmRegion))
              .endpointOverride(ssmEndpoint)
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create(ssmAccessKey, ssmSecretKey)))
              .build();
      for (Map.Entry<String, String> entry : ssmCommonParameters.entrySet()) {
        ssmClient.putParameter(
            PutParameterRequest.builder()
                .name(ssmCommonPath + entry.getKey())
                .value(entry.getValue())
                .type(ParameterType.STRING)
                .build());
      }
      for (Map.Entry<String, String> entry : ssmSpecificParameters.entrySet()) {
        ssmClient.putParameter(
            PutParameterRequest.builder()
                .name(ssmSpecificPath + entry.getKey())
                .value(entry.getValue())
                .type(ParameterType.STRING)
                .build());
      }

      // set up all of our file properties
      try (var out = new FileWriter(propertiesFile)) {
        var props = new Properties();
        for (Map.Entry<String, String> entry : fileProperties.entrySet()) {
          props.setProperty(entry.getKey(), entry.getValue());
        }
        props.store(out, null);
      }

      // set up all of our system properties
      for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
        System.setProperty(entry.getKey(), entry.getValue());
      }

      final var config = LayeredConfiguration.createConfigLoader(defaultValues, envVars::get);
      assertEquals("a-system-property", config.stringValue(nameA));
      assertEquals("b-env-var", config.stringValue(nameB));
      assertEquals("c-file-property", config.stringValue(nameC));
      assertEquals("d-ssm-specific-parameter", config.stringValue(nameD));
      assertEquals("e-ssm-common-parameter", config.stringValue(nameE));
      assertEquals("f-default", config.stringValue(nameF));
      assertEquals(Optional.empty(), config.stringOption(nameZ));

    } finally {
      // system properties are a shared resource so remove our additions
      for (String propertyName : systemProperties.keySet()) {
        System.clearProperty(propertyName);
      }
      if (propertiesFile.exists()) {
        propertiesFile.delete();
      }
    }
  }
}
