package gov.cms.bfd.sharedutils.config;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

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

    configBuilder.addSingle(defaultValues::get);

    final var ssmPath = baseConfig.stringValue(ENV_VAR_KEY_SSM_PARAMETER_PATH, "");
    if (ssmPath.length() > 0) {
      ensureAwsCredentialsConfiguredCorrectly();
      final var ssmClient = AWSSimpleSystemsManagementClient.builder();
      baseConfig
          .parsedOption(ENV_VAR_KEY_SSM_REGION, Regions.class, Regions::fromName)
          .ifPresent(r -> ssmClient.setRegion(r.getName()));
      final var parameterStore =
          new AwsParameterStoreClient(
              ssmClient.build(), AwsParameterStoreClient.DEFAULT_BATCH_SIZE);
      final var parametersMap = parameterStore.loadParametersAtPath(ssmPath);
      configBuilder.addMap(parametersMap);
    }

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
   * Just for convenience: makes sure DefaultAWSCredentialsProviderChain has whatever it needs
   * before we try to use any AWS resources.
   */
  public static void ensureAwsCredentialsConfiguredCorrectly() {
    try {
      DefaultAWSCredentialsProviderChain awsCredentialsProvider =
          new DefaultAWSCredentialsProviderChain();
      awsCredentialsProvider.getCredentials();
    } catch (AmazonClientException e) {
      /*
       * The credentials provider should throw this if it can't find what
       * it needs.
       */
      throw new AppConfigurationException(
          String.format(
              "Missing configuration for AWS credentials (for %s).",
              DefaultAWSCredentialsProviderChain.class.getName()),
          e);
    }
  }
}
