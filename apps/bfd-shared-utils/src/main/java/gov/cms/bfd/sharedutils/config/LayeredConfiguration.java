package gov.cms.bfd.sharedutils.config;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
   * The name of the environment variable that should be used to provide the region used for looking
   * up configuration variables in AWS SSM parameter store.
   */
  public static final String ENV_VAR_KEY_SSM_REGION = "SSM_REGION";

  /**
   * The name of the environment variable that should be used to provide an override endpoint used
   * for looking up configuration variables in AWS SSM parameter store. Intended for use in tests.
   */
  public static final String ENV_VAR_KEY_SSM_ENDPOINT = "SSM_ENDPOINT";

  /**
   * The name of the environment variable that should be used to provide an access key used for
   * looking up configuration variables in AWS SSM parameter store. Intended for use in tests.
   */
  public static final String ENV_VAR_KEY_SSM_ACCESS_KEY = "SSM_ACCESS_KEY";

  /**
   * The name of the environment variable that should be used to provide a secret key used for
   * looking up configuration variables in AWS SSM parameter store. Intended for use in tests.
   */
  public static final String ENV_VAR_KEY_SSM_SECRET_KEY = "SSM_SECRET_KEY";

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
      if (baseConfig.stringOption(ENV_VAR_KEY_SSM_ACCESS_KEY).isEmpty()) {
        ensureAwsCredentialsConfiguredCorrectly();
      }
      final var ssmClient = SsmClient.builder();
      // either region or endpoint can be set on ssmClient but not both
      if (baseConfig.stringOption(ENV_VAR_KEY_SSM_ENDPOINT).isPresent()) {
        // region is required when defining endpoint
        ssmClient
            .region(baseConfig.parsedValue(ENV_VAR_KEY_SSM_REGION, Region.class, Region::of))
            .endpointOverride(URI.create(baseConfig.stringValue(ENV_VAR_KEY_SSM_ENDPOINT)));
      } else if (baseConfig.stringOption(ENV_VAR_KEY_SSM_REGION).isPresent()) {
        ssmClient.region(baseConfig.parsedValue(ENV_VAR_KEY_SSM_REGION, Region.class, Region::of));
      }
      if (baseConfig.stringOption(ENV_VAR_KEY_SSM_ACCESS_KEY).isPresent()) {
        ssmClient.credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    baseConfig.stringValue(ENV_VAR_KEY_SSM_ACCESS_KEY),
                    baseConfig.stringValue(ENV_VAR_KEY_SSM_SECRET_KEY))));
      }
      final var parameterStore =
          new AwsParameterStoreClient(
              ssmClient.build(), AwsParameterStoreClient.DEFAULT_BATCH_SIZE);
      final var parametersMap = parameterStore.loadParametersAtPath(ssmPath);
      configBuilder.addMap(parametersMap);
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
}
