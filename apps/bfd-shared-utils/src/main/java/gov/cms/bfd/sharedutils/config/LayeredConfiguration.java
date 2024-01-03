package gov.cms.bfd.sharedutils.config;

import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_AWS_ACCESS_KEY;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_AWS_ENDPOINT;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_AWS_REGION;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_AWS_SECRET_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * Utility class to allow creation of {@link ConfigLoader} instances in multiple applications using
 * a common algorithm.
 */
@AllArgsConstructor
public final class LayeredConfiguration {
  /** Alternative name prefix used for java property lookup. */
  public static final String PROPERTY_NAME_PREFIX = "bfd.";

  /** Alternative name prefix used for environment variable lookup. */
  public static final String ENV_VAR_PREFIX = "BFD_";

  /**
   * The name of the environment variable that can be used to provide a JSON string defining a
   * {@link LayeredConfigurationSettings} object.
   */
  public static final String SSM_PATH_CONFIG_SETTINGS_JSON = "CONFIG_SETTINGS_JSON";

  /** Source of basic configuration data (usually environment variables). */
  private final ConfigLoader baseConfig;

  /** Settings that control which sources to include when building a {@link ConfigLoader}. */
  private final LayeredConfigurationSettings settings;

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information based on our settings and base config.
   *
   * <p>Config values will be loaded from the following sources. Sources are checked in order with
   * first matching value used.
   *
   * <ol>
   *   <li>System properties.
   *   <li>Values from our base config object.
   *   <li>If {@link LayeredConfigurationSettings#propertiesFile} is defined use properties in that
   *       file.
   *   <li>If {@link LayeredConfigurationSettings#ssmHierarchies} is defined use parameters at any
   *       level of the hierarchy rooted at each of those paths.
   *   <li>default values from provided {@link Map}.
   * </ol>
   *
   * @param defaultValues map containing default values for variables
   * @return appropriately configured {@link ConfigLoader}
   */
  public ConfigLoader createConfigLoader(Map<String, String> defaultValues) {
    final var configBuilder = ConfigLoader.builder();

    // the defaults are our last resort
    configBuilder.addMap(defaultValues);

    // load parameters from hierarchical AWS SSM paths if configured
    if (settings.hasSsmHierarchies()) {
      final var awsConfig = loadAwsClientConfig();
      if (awsConfig.isCredentialCheckUseful()) {
        ensureAwsCredentialsConfiguredCorrectly();
      }

      final var parameterStore = createAwsParameterStoreClient(awsConfig);
      addSsmParametersToBuilder(settings.getSsmHierarchies(), parameterStore, configBuilder);
    }

    // load properties from properties file if configured
    if (settings.hasPropertiesFile()) {
      addPropertiesFileToBuilder(settings.getPropertiesFile(), configBuilder);
    }

    // load environment variables with or without SSM parameter name mapping
    configBuilder.add(baseConfig.getSource()).alsoWithSsmToEnvVarMapping(ENV_VAR_PREFIX);

    configBuilder.addSystemProperties().alsoWithSsmToPropertyMapping(PROPERTY_NAME_PREFIX);

    return configBuilder.build();
  }

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided {@link ConfigLoaderSource} is used to look up environment variables
   * so that these can be simulated in tests without having to fork a process.
   *
   * <p>Internally this creates a {@link LayeredConfigurationSettings} by parsing the JSON value in
   * {@link #SSM_PATH_CONFIG_SETTINGS_JSON} and then creates an instance of this class and returns a
   * value using its {@link #createConfigLoader} method.
   *
   * @param defaultValues map containing default values for variables
   * @param getenv source of environment variables (provided explicitly for testing)
   * @return appropriately configured {@link ConfigLoader}
   */
  public static ConfigLoader createConfigLoader(
      Map<String, String> defaultValues, ConfigLoaderSource getenv) {
    final var baseConfig = ConfigLoader.builder().add(getenv).build();
    final var settings = loadLayeredConfigurationSettings(baseConfig);
    final var layeredConfig = new LayeredConfiguration(baseConfig, settings);
    return layeredConfig.createConfigLoader(defaultValues);
  }

  /**
   * Just for convenience: makes sure DefaultCredentialsProvider has whatever it needs before we try
   * to use any AWS resources.
   */
  public static void ensureAwsCredentialsConfiguredCorrectly() {
    try (var awsCredentialsProvider = DefaultCredentialsProvider.builder().build()) {
      awsCredentialsProvider.resolveCredentials();
    } catch (SdkClientException e) {
      /*
       * The credentials provider should throw this if it can't find what
       * it needs.
       */
      throw new AppConfigurationException(
          String.format(
              "Missing configuration for AWS credentials (for %s).",
              DefaultCredentialsProvider.class.getName()),
          e);
    }
  }

  /**
   * Reads parameters from SSM using the provided client and adds them to the provided {@link
   * ConfigLoader.Builder}.
   *
   * @param ssmPaths paths to read parameters from
   * @param parameterStore used to read parameters from ssm
   * @param configBuilder builder to add parameters to
   */
  private static void addSsmParametersToBuilder(
      List<String> ssmPaths,
      AwsParameterStoreClient parameterStore,
      ConfigLoader.Builder configBuilder) {
    for (String ssmPath : ssmPaths) {
      final var parametersMap = parameterStore.loadParametersAtPath(ssmPath, true);
      configBuilder.addMap(parametersMap);
    }
  }

  /**
   * Reads properties from a file and adds them to the provided {@link ConfigLoader.Builder}.
   *
   * @param propertiesFile path to a properties file
   * @param configBuilder builder to add parameters to
   */
  private static void addPropertiesFileToBuilder(
      String propertiesFile, ConfigLoader.Builder configBuilder) {
    try {
      final var file = new File(propertiesFile);
      configBuilder.addPropertiesFile(file).alsoWithSsmToPropertyMapping(PROPERTY_NAME_PREFIX);
    } catch (IOException ex) {
      throw new ConfigException("propertiesFile", "error parsing file", ex);
    }
  }

  /**
   * Loads {@link AwsClientConfig} for use in configuring SSM clients. These settings are generally
   * only changed from defaults during localstack based tests.
   *
   * @return the aws client settings
   */
  private AwsClientConfig loadAwsClientConfig() {
    return AwsClientConfig.awsBuilder()
        .region(baseConfig.parsedOption(ENV_VAR_AWS_REGION, Region.class, Region::of).orElse(null))
        .endpointOverride(
            baseConfig.parsedOption(ENV_VAR_AWS_ENDPOINT, URI.class, URI::create).orElse(null))
        .accessKey(baseConfig.stringValue(ENV_VAR_AWS_ACCESS_KEY, null))
        .secretKey(baseConfig.stringValue(ENV_VAR_AWS_SECRET_KEY, null))
        .build();
  }

  /**
   * Constructs an {@link AwsParameterStoreClient} configured using the given client settings.
   *
   * @param awsConfig configuration settings to communicate with AWS SSM
   * @return the created object
   */
  private AwsParameterStoreClient createAwsParameterStoreClient(AwsClientConfig awsConfig) {
    final var ssmClient = SsmClient.builder();
    awsConfig.configureAwsService(ssmClient);
    return new AwsParameterStoreClient(
        ssmClient.build(), AwsParameterStoreClient.DEFAULT_BATCH_SIZE);
  }

  /**
   * Uses the provided {@link ConfigLoader} to create a {@link LayeredConfigurationSettings} object.
   * The settings are provided in a JSON string contained in {@link #SSM_PATH_CONFIG_SETTINGS_JSON}.
   * Defaults are used if no settings are defined.
   *
   * @param config used to read settings
   * @return the created settings
   */
  @VisibleForTesting
  static LayeredConfigurationSettings loadLayeredConfigurationSettings(ConfigLoader config) {
    final var configJson = config.stringValue(SSM_PATH_CONFIG_SETTINGS_JSON, "");
    final LayeredConfigurationSettings settings;
    if (configJson.length() > 0) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        settings = mapper.readValue(configJson, LayeredConfigurationSettings.class);
      } catch (JsonProcessingException e) {
        throw new ConfigException(SSM_PATH_CONFIG_SETTINGS_JSON, "error parsing settings JSON", e);
      }
    } else {
      settings = LayeredConfigurationSettings.builder().build();
    }
    return settings;
  }

  /**
   * Split the given CSV string to create a list. All empty strings are removed from the list before
   * it is returned.
   *
   * @param rawPathString CSV string
   * @return list of non-empty values
   */
  @VisibleForTesting
  static List<String> splitPathCsv(String rawPathString) {
    return Arrays.stream(rawPathString.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
