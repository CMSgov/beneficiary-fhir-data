package gov.cms.bfd.pipeline.app;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Models the configuration options for the application.
 *
 * <p>Note that, in addition to the configuration specified here, the application must also be
 * provided with credentials that can be used to access the specified S3 bucket. For that, the
 * application supports all of the mechanisms that are supported by {@link
 * DefaultAWSCredentialsProviderChain}, which include environment variables, EC2 instance profiles,
 * etc.
 */
public final class AppConfiguration implements Serializable {
  private static final long serialVersionUID = -6845504165285244536L;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * ExtractionOptions#getS3BucketName()} value.
   */
  public static final String ENV_VAR_KEY_BUCKET = "S3_BUCKET_NAME";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * ExtractionOptions#getDataSetFilter()} value: This environment variable specifies the {@link
   * RifFileType} that will be processed. Any {@link DataSetManifest}s that contain other {@link
   * RifFileType}s will be skipped entirely (even if they <em>also</em> contain the allowed {@link
   * RifFileType}. For example, specifying "BENEFICIARY" will configure the application to only
   * process data sets that <strong>only</strong> contain {@link RifFileType#BENEFICIARY}s.
   */
  public static final String ENV_VAR_KEY_ALLOWED_RIF_TYPE = "DATA_SET_TYPE_ALLOWED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#getHicnHashIterations()} value.
   */
  public static final String ENV_VAR_KEY_HICN_HASH_ITERATIONS = "HICN_HASH_ITERATIONS";

  /**
   * The name of the environment variable that should be used to provide a hex encoded
   * representation of the {@link #getCcwRifLoadOptions()} {@link
   * LoadAppOptions#getHicnHashPepper()} value.
   */
  public static final String ENV_VAR_KEY_HICN_HASH_PEPPER = "HICN_HASH_PEPPER";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getDatabaseUrl()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_URL = "DATABASE_URL";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getDatabaseUsername()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_USERNAME = "DATABASE_USERNAME";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getDatabaseOptions()} {@link DatabaseOptions#getDatabasePassword()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_PASSWORD = "DATABASE_PASSWORD";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#getLoaderThreads()} value.
   */
  public static final String ENV_VAR_KEY_LOADER_THREADS = "LOADER_THREADS";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#isIdempotencyRequired()} value.
   */
  public static final String ENV_VAR_KEY_IDEMPOTENCY_REQUIRED = "IDEMPOTENCY_REQUIRED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#isFixupsEnabled()} value.
   */
  public static final String ENV_VAR_KEY_FIXUPS_ENABLED = "FIXUPS_ENABLED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#getFixupThreads()} value.
   */
  public static final String ENV_VAR_KEY_FIXUP_THREADS = "FIXUP_THREADS";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricKey()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_KEY = "NEW_RELIC_METRIC_KEY";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicAppName()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_APP_NAME = "NEW_RELIC_APP_NAME";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricHost()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_HOST = "NEW_RELIC_METRIC_HOST";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricPath()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_PATH = "NEW_RELIC_METRIC_PATH";
  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getMetricOptions()} {@link MetricOptions#getNewRelicMetricPeriod()} value.
   */
  public static final String ENV_VAR_NEW_RELIC_METRIC_PERIOD = "NEW_RELIC_METRIC_PERIOD";

  private final MetricOptions metricOptions;
  private final DatabaseOptions databaseOptions;
  private final CcwRifLoadOptions ccwRifLoadOptions;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param ccwRifLoadOptions the value to use for {@link #getCcwRifLoadOptions()}
   */
  public AppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      CcwRifLoadOptions ccwRifLoadOptions) {
    this.metricOptions = metricOptions;
    this.databaseOptions = databaseOptions;
    this.ccwRifLoadOptions = ccwRifLoadOptions;
  }

  /** @return the {@link MetricOptions} that the application will use */
  public MetricOptions getMetricOptions() {
    return metricOptions;
  }

  /** @return the {@link DatabaseOptions} that the application will use */
  public DatabaseOptions getDatabaseOptions() {
    return databaseOptions;
  }

  /** @return the {@link CcwRifLoadOptions} that the application will use */
  public CcwRifLoadOptions getCcwRifLoadOptions() {
    return ccwRifLoadOptions;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AppConfiguration [metricOptions=");
    builder.append(metricOptions);
    builder.append(", databaseOptions=");
    builder.append(databaseOptions);
    builder.append(", ccwRifLoadOptions=");
    builder.append(ccwRifLoadOptions);
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
  static AppConfiguration readConfigFromEnvironmentVariables() {
    String s3BucketName = System.getenv(ENV_VAR_KEY_BUCKET);
    if (s3BucketName == null || s3BucketName.isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_BUCKET));

    String rifFilterText = System.getenv(ENV_VAR_KEY_ALLOWED_RIF_TYPE);
    RifFileType allowedRifFileType;
    if (rifFilterText != null && !rifFilterText.isEmpty()) {
      try {
        allowedRifFileType = RifFileType.valueOf(rifFilterText);
      } catch (IllegalArgumentException e) {
        throw new AppConfigurationException(
            String.format(
                "Invalid value for configuration environment variable '%s': '%s'",
                ENV_VAR_KEY_ALLOWED_RIF_TYPE, rifFilterText),
            e);
      }
    } else {
      allowedRifFileType = null;
    }

    String hicnHashIterationsText = System.getenv(ENV_VAR_KEY_HICN_HASH_ITERATIONS);
    if (hicnHashIterationsText == null || hicnHashIterationsText.isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_HICN_HASH_ITERATIONS));
    int hicnHashIterations;
    try {
      hicnHashIterations = Integer.parseInt(hicnHashIterationsText);
    } catch (NumberFormatException e) {
      hicnHashIterations = -1;
    }
    if (hicnHashIterations < 1)
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_HICN_HASH_ITERATIONS, hicnHashIterationsText));

    String hicnHashPepperText = System.getenv(ENV_VAR_KEY_HICN_HASH_PEPPER);
    if (hicnHashPepperText == null || hicnHashPepperText.isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_HICN_HASH_PEPPER));
    byte[] hicnHashPepper;
    try {
      hicnHashPepper = Hex.decodeHex(hicnHashPepperText.toCharArray());
    } catch (DecoderException e) {
      hicnHashPepper = new byte[] {};
    }
    if (hicnHashPepperText.length() < 1)
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_HICN_HASH_PEPPER, hicnHashPepperText));

    String databaseUrl = System.getenv(ENV_VAR_KEY_DATABASE_URL);
    if (databaseUrl == null || databaseUrl.isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_DATABASE_URL));

    String databaseUsername = System.getenv(ENV_VAR_KEY_DATABASE_USERNAME);
    if (databaseUsername == null)
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_DATABASE_USERNAME));

    String databasePassword = System.getenv(ENV_VAR_KEY_DATABASE_PASSWORD);
    if (databasePassword == null)
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_DATABASE_PASSWORD));

    String loaderThreadsText = System.getenv(ENV_VAR_KEY_LOADER_THREADS);
    if (loaderThreadsText == null || loaderThreadsText.isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_LOADER_THREADS));
    int loaderThreads;
    try {
      loaderThreads = Integer.parseInt(loaderThreadsText);
    } catch (NumberFormatException e) {
      loaderThreads = -1;
    }
    if (loaderThreads < 1)
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_LOADER_THREADS, loaderThreadsText));

    String idempotencyRequiredText = System.getenv(ENV_VAR_KEY_IDEMPOTENCY_REQUIRED);
    if (idempotencyRequiredText == null || idempotencyRequiredText.isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              ENV_VAR_KEY_IDEMPOTENCY_REQUIRED));
    Optional<Boolean> idempotencyRequired = parseBoolean(idempotencyRequiredText);
    if (!idempotencyRequired.isPresent())
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s'.",
              ENV_VAR_KEY_IDEMPOTENCY_REQUIRED));

    /*
     * Just for convenience: make sure DefaultAWSCredentialsProviderChain
     * has whatever it needs.
     */
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

    // New Relic Metrics
    String newRelicMetricKey = System.getenv(ENV_VAR_NEW_RELIC_METRIC_KEY);
    String newRelicAppName = System.getenv(ENV_VAR_NEW_RELIC_APP_NAME);
    String newRelicMetricHost = System.getenv(ENV_VAR_NEW_RELIC_METRIC_HOST);
    String newRelicMetricPath = System.getenv(ENV_VAR_NEW_RELIC_METRIC_PATH);
    String rawNewRelicMetricPeriod = System.getenv(ENV_VAR_NEW_RELIC_METRIC_PERIOD);
    int newRelicMetricPeriod;
    try {
      newRelicMetricPeriod = Integer.parseInt(rawNewRelicMetricPeriod);
    } catch (NumberFormatException ex) {
      newRelicMetricPeriod = 15;
    }

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "unknown";
    }

    MetricOptions metricOptions =
        new MetricOptions(
            newRelicMetricKey,
            newRelicAppName,
            newRelicMetricHost,
            newRelicMetricPath,
            newRelicMetricPeriod,
            hostname);
    DatabaseOptions databaseOptions =
        new DatabaseOptions(databaseUrl, databaseUsername, databasePassword);
    ExtractionOptions extractionOptions = new ExtractionOptions(s3BucketName, allowedRifFileType);
    LoadAppOptions loadOptions =
        new LoadAppOptions(
            hicnHashIterations,
            hicnHashPepper,
            loaderThreads,
            idempotencyRequired.get().booleanValue());
    CcwRifLoadOptions ccwRifLoadOptions = new CcwRifLoadOptions(extractionOptions, loadOptions);

    return new AppConfiguration(metricOptions, databaseOptions, ccwRifLoadOptions);
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
