package gov.cms.bfd.server.war;

import java.io.Serializable;
import java.util.Optional;

/**
 * Models the configuration options for the application.
 *
 * <p>Note that, in addition to the configuration specified here, the application must also be
 * provided with credentials that can be used to access the specified S3 bucket. For that, the
 * application supports all of the mechanisms that are supported by {@link
 * DefaultAWSCredentialsProviderChain}, which include environment variables, EC2 instance profiles,
 * etc.
 */
public final class FhirAppConfiguration implements Serializable {
  private static final long serialVersionUID = -6845504165285244536L;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getLoadOptions()} {@link LoadAppOptions#isV2Enabled()} value.
   */
  public static final String ENV_VAR_KEY_V2_ENABLED = "V2_ENABLED";

  private final LoadFhirAppOptions loadOptions;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param extractionOptions the value to use for {@link #getExtractionOptions()}
   * @param loadOptions the value to use for {@link #getLoadOptions()}
   */
  public FhirAppConfiguration(LoadFhirAppOptions loadOptions) {

    this.loadOptions = loadOptions;
  }

  /** @return the {@link LoadAppOptions} that the application will use */
  public LoadFhirAppOptions getLoadOptions() {
    return loadOptions;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AppConfiguration [=");
    builder.append(", loadOptions=");
    builder.append(loadOptions);
    builder.append("]");
    return builder.toString();
  }

  /**
   * Per <code>/dev/design-decisions-readme.md</code>, this application accepts its configuration
   * via environment variables. Read those in, and build an {@link AppConfiguration} instance from
   * them.
   *
   * <p>As a convenience, this method will also verify that AWS credentials were provided, such that
   * {@link DefaultAWSCredentialsProviderChain} can load them. If not, an {@link
   * AppConfigurationException} will be thrown.
   *
   * @return the {@link AppConfiguration} instance represented by the configuration provided to this
   *     application via the environment variables
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     configuration passed to the application are incomplete or incorrect.
   */
  static FhirAppConfiguration readConfigFromEnvironmentVariables() {

    String v2EnabledText = System.getenv(ENV_VAR_KEY_V2_ENABLED);
    boolean v2Enabled = false;
    if (v2EnabledText != null && !v2EnabledText.isEmpty()) {
      v2Enabled = Boolean.parseBoolean(v2EnabledText);
    }

    return new FhirAppConfiguration(new LoadFhirAppOptions(v2Enabled));
  }

  /**
   * Design note: want better parsing than what {@link Boolean#parseBoolean(String)} provides.
   *
   * @param booleanText the text to try and parse a <code>boolean</code> from
   * @return the parsed <code>boolean</code>, or {@link Optional#empty()} if nothing valid could be
   *     parsed
   */
  static Optional<Boolean> parseBoolean(String booleanText) {
    if ("true".equalsIgnoreCase(booleanText)) return Optional.of(true);
    else if ("false".equalsIgnoreCase(booleanText)) return Optional.of(false);
    else return Optional.empty();
  }
}
