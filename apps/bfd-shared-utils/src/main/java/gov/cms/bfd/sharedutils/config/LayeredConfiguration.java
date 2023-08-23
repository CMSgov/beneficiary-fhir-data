package gov.cms.bfd.sharedutils.config;

import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_ACCESS_KEY;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_ENDPOINT;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_REGION;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_SECRET_KEY;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * Utility class to allow creation of {@link ConfigLoader} instances in multiple applications using
 * a common algorithm.
 */
public final class LayeredConfiguration {

  /** Prevents instance creation. */
  private LayeredConfiguration() {}

  /**
   * The name of the environment variable that should be used to provide a path for looking up
   * configuration variables in AWS SSM parameter store.
   */
  public static final String ENV_VAR_KEY_SSM_PARAMETER_PATH = "SSM_PARAMETER_PATH";

  /**
   * The name of a java properties file that should be used to provide a source of configuration
   * variables.
   */
  public static final String ENV_VAR_KEY_PROPERTIES_FILE = "PROPERTIES_FILE";

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided function is used to look up environment variables so that these can
   * be simulated in tests without having to fork a process.
   *
   * <p>Config values will be loaded from these sources. Sources are checked in order with first
   * matching value used.
   *
   * <ol>
   *   <li>System properties.
   *   <li>Environment variables (using provided function).
   *   <li>If {@link #ENV_VAR_KEY_PROPERTIES_FILE} is defined use properties in that file.
   *   <li>If {@link #ENV_VAR_KEY_SSM_PARAMETER_PATH} is defined use parameters at that path.
   *   <li>default values from map
   * </ol>
   *
   * @param defaultValues possibly map containing default values for variables
   * @param getenv function used to access environment variables (provided explicitly for testing)
   * @return appropriately configured {@link ConfigLoader}
   */
  public static ConfigLoader createConfigLoader(
      Map<String, String> defaultValues, Function<String, String> getenv) {
    final var baseConfig = ConfigLoader.builder().addSingle(getenv).build();
    final var configBuilder = ConfigLoader.builder();

    // the defaults are our last resort
    configBuilder.addMap(defaultValues);

    // load parameters from AWS SSM if configured
    final var ssmPath = baseConfig.stringValue(ENV_VAR_KEY_SSM_PARAMETER_PATH, "");
    if (ssmPath.length() > 0) {
      final AwsClientConfig ssmConfig = loadSsmConfig(baseConfig);
      if (ssmConfig.isCredentialCheckUseful()) {
        ensureAwsCredentialsConfiguredCorrectly();
      }
      final var ssmClient = SsmClient.builder();
      ssmConfig.configureAwsService(ssmClient);
      final var parameterStore =
          new AwsParameterStoreClient(
              ssmClient.build(), AwsParameterStoreClient.DEFAULT_BATCH_SIZE);
      for (String singlePath : ssmPath.split(",")) {
        final var parametersMap = parameterStore.loadParametersAtPath(singlePath);
        configBuilder.addMap(parametersMap);
      }
    }

    // load properties from file if configured
    final var propertiesFile = baseConfig.stringValue(ENV_VAR_KEY_PROPERTIES_FILE, "");
    if (propertiesFile.length() > 0) {
      try {
        final var file = new File(propertiesFile);
        configBuilder.addPropertiesFile(file);
      } catch (IOException ex) {
        throw new ConfigException(ENV_VAR_KEY_PROPERTIES_FILE, "error parsing file", ex);
      }
    }

    configBuilder.addSingle(getenv);
    configBuilder.addSystemProperties();
    return configBuilder.build();
  }

  /**
   * Just for convenience: makes sure DefaultCredentialsProvider has whatever it needs before we try
   * to use any AWS resources.
   */
  public static void ensureAwsCredentialsConfiguredCorrectly() {
    /*
     * Just for convenience: make sure DefaultCredentialsProvider
     * has whatever it needs.
     */
    try {
      DefaultCredentialsProvider awsCredentialsProvider =
          DefaultCredentialsProvider.builder().build();
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
   * Loads {@link AwsClientConfig} for use in configuring SSM clients. These settings are generally
   * only changed from defaults during localstack based tests.
   *
   * @param config used to load configuration values
   * @return the aws client settings
   */
  private static AwsClientConfig loadSsmConfig(ConfigLoader config) {
    return AwsClientConfig.awsBuilder()
        .region(config.parsedOption(ENV_VAR_KEY_AWS_REGION, Region.class, Region::of).orElse(null))
        .endpointOverride(
            config.parsedOption(ENV_VAR_KEY_AWS_ENDPOINT, URI.class, URI::create).orElse(null))
        .accessKey(config.stringValue(ENV_VAR_KEY_AWS_ACCESS_KEY, null))
        .secretKey(config.stringValue(ENV_VAR_KEY_AWS_SECRET_KEY, null))
        .build();
  }
}
