package gov.cms.bfd.sharedutils.config;

import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_ACCESS_KEY;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_ENDPOINT;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_REGION;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_SECRET_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
  /**
   * The name of the environment variable that can be used to provide a JSON string defining a
   * {@link LayeredConfigurationSettings} object.
   */
  public static final String ENV_VAR_KEY_SETTINGS_JSON = "LAYERED_CONFIG_SETTINGS_JSON";

  /**
   * The name of the environment variable that can be used to provide a path for looking up
   * configuration variables in AWS SSM parameter store. Intended to be used when {@link
   * #ENV_VAR_KEY_SETTINGS_JSON} is not being used.
   */
  public static final String ENV_VAR_KEY_SSM_PARAMETER_PATH = "SSM_PARAMETER_PATH";

  /**
   * The name of a java properties file that should be used to provide a source of configuration
   * variables.
   */
  public static final String ENV_VAR_KEY_PROPERTIES_FILE = "PROPERTIES_FILE";

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
   *   <li>If {@link LayeredConfigurationSettings#ssmPaths} is defined use parameters at each of
   *       those paths.
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

    if (settings.hasSsmPaths() || settings.hasSsmHierarchies()) {
      final var awsConfig = loadAwsClientConfig();
      if (awsConfig.isCredentialCheckUseful()) {
        ensureAwsCredentialsConfiguredCorrectly();
      }

      final var parameterStore = createAwsParameterStoreClient(awsConfig);

      // load parameters from flat AWS SSM paths if configured
      if (settings.hasSsmPaths()) {
        addSsmParametersToBuilder(settings.getSsmPaths(), false, parameterStore, configBuilder);
      }

      // load parameters from hierarchical AWS SSM paths if configured
      if (settings.hasSsmHierarchies()) {
        addSsmParametersToBuilder(
            settings.getSsmHierarchies(), true, parameterStore, configBuilder);
      }
    }

    // load properties from file if configured
    if (settings.hasPropertiesFile()) {
      addPropertiesFileToBuilder(settings.getPropertiesFile(), configBuilder);
    }

    configBuilder.addSingle(key -> baseConfig.stringValue(key, null));
    configBuilder.addSystemProperties();
    return configBuilder.build();
  }

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided function is used to look up environment variables so that these can
   * be simulated in tests without having to fork a process.
   *
   * <p>Creates a {@link LayeredConfigurationSettings} object either by parsing the JSON value in
   * {@link #ENV_VAR_KEY_SETTINGS_JSON} or the comma separated list of paths in {@link
   * #ENV_VAR_KEY_SSM_PARAMETER_PATH}. Then creates an instance of this class and returns a value
   * using its {@link #createConfigLoader} method.
   *
   * @param defaultValues possibly map containing default values for variables
   * @param getenv function used to access environment variables (provided explicitly for testing)
   * @return appropriately configured {@link ConfigLoader}
   */
  public static ConfigLoader createConfigLoader(
      Map<String, String> defaultValues, Function<String, String> getenv) {
    final var baseConfig = ConfigLoader.builder().addSingle(getenv).build();
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
   * @param recursive true means to read from entire hierarchy, false just from the root paths
   * @param parameterStore client to use
   * @param configBuilder builder to add parameters to
   */
  private static void addSsmParametersToBuilder(
      List<String> ssmPaths,
      boolean recursive,
      AwsParameterStoreClient parameterStore,
      ConfigLoader.Builder configBuilder) {
    for (String ssmPath : ssmPaths) {
      final var parametersMap = parameterStore.loadParametersAtPath(ssmPath, recursive);
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
      configBuilder.addPropertiesFile(file);
    } catch (IOException ex) {
      throw new ConfigException(ENV_VAR_KEY_PROPERTIES_FILE, "error parsing file", ex);
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
        .region(
            baseConfig.parsedOption(ENV_VAR_KEY_AWS_REGION, Region.class, Region::of).orElse(null))
        .endpointOverride(
            baseConfig.parsedOption(ENV_VAR_KEY_AWS_ENDPOINT, URI.class, URI::create).orElse(null))
        .accessKey(baseConfig.stringValue(ENV_VAR_KEY_AWS_ACCESS_KEY, null))
        .secretKey(baseConfig.stringValue(ENV_VAR_KEY_AWS_SECRET_KEY, null))
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
   * The settings are either provided in a JSON string contained in {@link
   * #ENV_VAR_KEY_SETTINGS_JSON} or produced using any SSM paths contained in {@link
   * #ENV_VAR_KEY_SSM_PARAMETER_PATH} and properties file path contained in {@link
   * #ENV_VAR_KEY_PROPERTIES_FILE}.
   *
   * @param config used to read settings
   * @return the created settings
   */
  @VisibleForTesting
  static LayeredConfigurationSettings loadLayeredConfigurationSettings(ConfigLoader config) {
    final var configJson = config.stringValue(ENV_VAR_KEY_SETTINGS_JSON, "");
    final LayeredConfigurationSettings settings;
    if (configJson.length() > 0) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        settings = mapper.readValue(configJson, LayeredConfigurationSettings.class);
      } catch (JsonProcessingException e) {
        throw new ConfigException(ENV_VAR_KEY_SETTINGS_JSON, "error parsing settings JSON", e);
      }
    } else {
      settings =
          LayeredConfigurationSettings.builder()
              .ssmPaths(splitPathCsv(config.stringValue(ENV_VAR_KEY_SSM_PARAMETER_PATH, "")))
              .propertiesFile(config.stringValue(ENV_VAR_KEY_PROPERTIES_FILE, ""))
              .build();
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
    return Arrays.stream(rawPathString.split(",")).filter(s -> !s.isEmpty()).toList();
  }
}
