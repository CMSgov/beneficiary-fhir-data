package gov.cms.bfd.sharedutils.config;

import static gov.cms.bfd.sharedutils.config.LayeredConfiguration.ENV_VAR_PREFIX;
import static gov.cms.bfd.sharedutils.config.LayeredConfiguration.PROPERTY_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.AbstractLocalStackTest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
public class LayeredConfigurationIT extends AbstractLocalStackTest {
  /** Shared client used by all tests. */
  private SsmClient ssmClient;

  /** Shared temp file used by tests to start properties. */
  private File propertiesFile;

  /**
   * Creates an ssm client and a temp properties file.
   *
   * @throws IOException pass through
   */
  @BeforeEach
  void setUp() throws IOException {
    ssmClient =
        SsmClient.builder()
            .region(Region.of(localstack.getRegion()))
            .endpointOverride(localstack.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .build();
    propertiesFile = File.createTempFile("test", ".properties");
  }

  /** closes ssm client and deletes temp properties file. */
  @AfterEach
  void tearDown() {
    ssmClient.close();
    if (propertiesFile.exists()) {
      propertiesFile.delete();
    }
  }

  /**
   * Calls {@link LayeredConfiguration#createConfigLoader} with all settings and verifies that the
   * layers yield the expected values, including higher priority layers overriding lower priority
   * ones.
   *
   * @throws IOException pass through
   */
  @Test
  void shouldLayerConfigurationSources() throws IOException {
    // define everything we will need to include in our config
    final var baseName = LayeredConfigurationIT.class.getSimpleName();
    final var ssmBasePath = "/" + baseName + "/shouldLayerConfigurationSources/";
    final var ssmCommonPath = ssmBasePath + "common/";
    final var ssmSpecificPath = ssmBasePath + "specific/";
    final var ssmParentPath = ssmBasePath + "parent/";
    final var ssmChildName = "child";
    final var ssmChildPath = ssmParentPath + ssmChildName + "/";
    final var nameA = baseName + "a";
    final var nameB = baseName + "b";
    final var nameC = baseName + "c";
    final var nameD = baseName + "d";
    final var nameE = baseName + "e";
    final var nameF = baseName + "f";
    final var nameG = baseName + "g";
    final var nameH = baseName + "h";
    final var nameZ = baseName + "z";

    final var configSettings =
        LayeredConfigurationSettings.builder()
            .propertiesFile(propertiesFile.getPath())
            .ssmHierarchies(List.of(ssmCommonPath, ssmSpecificPath, ssmParentPath))
            .build();
    final var configSettingsJson = new ObjectMapper().writeValueAsString(configSettings);

    // a comes from system property
    // b comes from env variable
    // c comes from property file
    // d comes from ssm specific
    // e comes from ssm common
    // f comes from defaults
    // g comes from parent
    // child.g comes from child
    // z is undefined
    final var systemProperties =
        ImmutableMap.<String, String>builder().put(nameA, "a-system-property").build();
    final var envVars =
        ImmutableMap.<String, String>builder()
            .put(BaseAppConfiguration.ENV_VAR_AWS_REGION, localstack.getRegion())
            .put(LayeredConfiguration.SSM_PATH_CONFIG_SETTINGS_JSON, configSettingsJson)
            .put(BaseAppConfiguration.ENV_VAR_AWS_ENDPOINT, localstack.getEndpoint().toString())
            .put(BaseAppConfiguration.ENV_VAR_AWS_ACCESS_KEY, localstack.getAccessKey())
            .put(BaseAppConfiguration.ENV_VAR_AWS_SECRET_KEY, localstack.getSecretKey())
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
            .put(nameH, "h-ssm-specific-parameter")
            .build();
    final var ssmCommonParameters =
        ImmutableMap.<String, String>builder()
            .put(nameA, "a-ssm-common-parameter")
            .put(nameB, "b-ssm-common-parameter")
            .put(nameC, "c-ssm-common-parameter")
            .put(nameD, "d-ssm-common-parameter")
            .put(nameE, "e-ssm-common-parameter")
            .build();
    final var ssmParentParameters =
        ImmutableMap.<String, String>builder().put(nameG, "g-ssm-parent-parameter").build();
    final var ssmChildParameters =
        ImmutableMap.<String, String>builder().put(nameG, "g-ssm-child-parameter").build();
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
      for (Map.Entry<String, String> entry : ssmCommonParameters.entrySet()) {
        addParameterToSsm(ssmCommonPath + entry.getKey(), entry.getValue());
      }
      for (Map.Entry<String, String> entry : ssmSpecificParameters.entrySet()) {
        addParameterToSsm(ssmSpecificPath + entry.getKey(), entry.getValue());
      }
      for (Map.Entry<String, String> entry : ssmParentParameters.entrySet()) {
        addParameterToSsm(ssmParentPath + entry.getKey(), entry.getValue());
      }
      for (Map.Entry<String, String> entry : ssmChildParameters.entrySet()) {
        addParameterToSsm(ssmChildPath + entry.getKey(), entry.getValue());
      }

      // set up all of our file properties
      var properties = new Properties();
      for (Map.Entry<String, String> entry : fileProperties.entrySet()) {
        properties.setProperty(entry.getKey(), entry.getValue());
      }
      writePropertiesFile(properties);

      // set up all of our system properties
      for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
        System.setProperty(entry.getKey(), entry.getValue());
      }

      final var config =
          LayeredConfiguration.createConfigLoader(
              defaultValues, ConfigLoaderSource.fromMap(envVars));
      assertEquals("a-system-property", config.stringValue(nameA));
      assertEquals("b-env-var", config.stringValue(nameB));
      assertEquals("c-file-property", config.stringValue(nameC));
      assertEquals("d-ssm-specific-parameter", config.stringValue(nameD));
      assertEquals("e-ssm-common-parameter", config.stringValue(nameE));
      assertEquals("f-default", config.stringValue(nameF));
      assertEquals("g-ssm-parent-parameter", config.stringValue(nameG));
      assertEquals("g-ssm-child-parameter", config.stringValue(ssmChildName + "/" + nameG));
      assertEquals("h-ssm-specific-parameter", config.stringValue(nameH));
      assertEquals(Optional.empty(), config.stringOption(nameZ));

    } finally {
      // system properties are a shared resource so remove our additions
      for (String propertyName : systemProperties.keySet()) {
        System.clearProperty(propertyName);
      }
    }
  }

  /**
   * Calls {@link LayeredConfiguration#createConfigLoader} with various settings and verifies that
   * the layers in the resulting {@link ConfigLoader} match expectations.
   *
   * @throws IOException pass through
   */
  @Test
  void shouldBuildConfigLoaderBasedOnEnvVars() throws IOException {
    // set up properties file
    Properties expectedProperties = new Properties();
    expectedProperties.setProperty("p1", "1");
    writePropertiesFile(expectedProperties);

    // set up the values to pull as a hierarchy
    final var baseName = LayeredConfigurationIT.class.getSimpleName();
    final var ssmBasePath = "/" + baseName + "/shouldBuildConfigLoaderBasedOnEnvVars/";
    final var hierarchyPath = ssmBasePath + "root/";
    final var hierarchiesMap = new HashMap<String, String>();
    hierarchiesMap.put("new", "data");
    hierarchiesMap.put("x/hover", "board");
    addParameterToSsm(hierarchyPath + "new", "data");
    addParameterToSsm(hierarchyPath + "x/hover", "board");

    // set up default values we'll pass to createConfigLoader()
    final var defaultValues = Map.of("a", "A", "b", "B");

    // Set up map to hold our simulated environment variables
    final var envVars = new HashMap<String, String>();
    envVars.put(BaseAppConfiguration.ENV_VAR_AWS_REGION, localstack.getRegion());
    envVars.put(BaseAppConfiguration.ENV_VAR_AWS_ENDPOINT, localstack.getEndpoint().toString());
    envVars.put(BaseAppConfiguration.ENV_VAR_AWS_ACCESS_KEY, localstack.getAccessKey());
    envVars.put(BaseAppConfiguration.ENV_VAR_AWS_SECRET_KEY, localstack.getSecretKey());

    // the source we'll pass as our environment variables
    final var getenv = ConfigLoaderSource.fromMap(envVars);

    //
    // Set up complete now we'll create some loaders and verify they have the expected layers.
    //

    // no layered config variables so only defaults, env and properties should be used
    var expectedLoader =
        ConfigLoader.builder()
            .addMap(defaultValues)
            .add(getenv)
            .alsoWithSsmToEnvVarMapping(ENV_VAR_PREFIX)
            .addSystemProperties()
            .alsoWithSsmToPropertyMapping(PROPERTY_NAME_PREFIX)
            .build();
    var actualLoader = LayeredConfiguration.createConfigLoader(defaultValues, getenv);
    assertEquals(expectedLoader, actualLoader);

    // using json settings we should have all layers added
    final var configSettings =
        LayeredConfigurationSettings.builder()
            .propertiesFile(propertiesFile.getPath())
            .ssmHierarchies(List.of(hierarchyPath))
            .build();
    final var configSettingsJson = new ObjectMapper().writeValueAsString(configSettings);
    envVars.put(LayeredConfiguration.SSM_PATH_CONFIG_SETTINGS_JSON, configSettingsJson);
    expectedLoader =
        ConfigLoader.builder()
            .addMap(defaultValues)
            .addMap(hierarchiesMap)
            .addProperties(expectedProperties)
            .alsoWithSsmToPropertyMapping(PROPERTY_NAME_PREFIX)
            .add(getenv)
            .alsoWithSsmToEnvVarMapping(ENV_VAR_PREFIX)
            .addSystemProperties()
            .alsoWithSsmToPropertyMapping(PROPERTY_NAME_PREFIX)
            .build();
    actualLoader = LayeredConfiguration.createConfigLoader(defaultValues, getenv);
    assertEquals(expectedLoader, actualLoader);
  }

  /**
   * Convenience method to push a parameter to SSM.
   *
   * @param name parameter name
   * @param value parameter value
   */
  private void addParameterToSsm(String name, String value) {
    ssmClient.putParameter(
        PutParameterRequest.builder().name(name).value(value).type(ParameterType.STRING).build());
  }

  /**
   * Convenience method to write a set of java properties to our temp file.
   *
   * @param properties properties to write to the file
   * @throws IOException pass through
   */
  private void writePropertiesFile(Properties properties) throws IOException {
    try (OutputStream out = new FileOutputStream(propertiesFile)) {
      properties.store(out, "testing");
    }
  }
}
