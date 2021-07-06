package gov.cms.bfd.pipeline.app;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
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
   * #getDatabaseOptions()} {@link DatabaseOptions#getMaxPoolSize()} value.
   */
  public static final String ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE = "DATABASE_MAX_POOL_SIZE";

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

  /**
   * The name of the environment variable that should be used to indicate whether or not to
   * configure the RDA GPC data load job. Defaults to false to mean not to load the job.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_ENABLED = "RDA_JOB_ENABLED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link RdaLoadJob.Config#getRunInterval()} value. This variable's value
   * should be the frequency at which this job runs in seconds.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS = "RDA_JOB_INTERVAL_SECONDS";

  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS}. */
  public static final int DEFAULT_RDA_JOB_INTERVAL_SECONDS = 300;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link RdaLoadJob.Config#getBatchSize()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_BATCH_SIZE = "RDA_JOB_BATCH_SIZE";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_BATCH_SIZE}. */
  public static final int DEFAULT_RDA_JOB_BATCH_SIZE = 1;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link GrpcRdaSource.Config#getHost()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_HOST = "RDA_GRPC_HOST";

  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_HOST}. */
  public static final String DEFAULT_RDA_GRPC_HOST = "localhost";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link GrpcRdaSource.Config#getPort()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_PORT = "RDA_GRPC_PORT";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_PORT}. */
  public static final int DEFAULT_RDA_GRPC_PORT = 443;
  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link GrpcRdaSource.Config#getMaxIdle()} ()} value. This variable value
   * should be in seconds.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_MAX_IDLE_SECONDS = "RDA_GRPC_MAX_IDLE_SECONDS";

  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS}. */
  public static final int DEFAULT_RDA_GRPC_MAX_IDLE_SECONDS = Integer.MAX_VALUE;

  private final MetricOptions metricOptions;
  private final DatabaseOptions databaseOptions;
  private final CcwRifLoadOptions ccwRifLoadOptions;
  // this can be null if the RDA job is not configured, Optional is not Serializable
  @Nullable private final RdaLoadOptions rdaLoadOptions;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param metricOptions the value to use for {@link #getMetricOptions()}
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param ccwRifLoadOptions the value to use for {@link #getCcwRifLoadOptions()}
   * @param rdaLoadOptions the value to use for {@link #getRdaLoadOptions()}
   */
  public AppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      CcwRifLoadOptions ccwRifLoadOptions,
      RdaLoadOptions rdaLoadOptions) {
    this.metricOptions = metricOptions;
    this.databaseOptions = databaseOptions;
    this.ccwRifLoadOptions = ccwRifLoadOptions;
    this.rdaLoadOptions = rdaLoadOptions;
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

  /** @return the {@link RdaLoadOptions} that the application will use */
  public Optional<RdaLoadOptions> getRdaLoadOptions() {
    return Optional.ofNullable(rdaLoadOptions);
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
    builder.append(", rdaLoadOptions=");
    builder.append(rdaLoadOptions);
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
    String s3BucketName = readEnvStringRequired(ENV_VAR_KEY_BUCKET);
    int hicnHashIterations = readEnvIntPositiveRequired(ENV_VAR_KEY_HICN_HASH_ITERATIONS);
    byte[] hicnHashPepper = readEnvBytesRequired(ENV_VAR_KEY_HICN_HASH_PEPPER);
    String databaseUrl = readEnvStringRequired(ENV_VAR_KEY_DATABASE_URL);
    String databaseUsername = readEnvStringRequired(ENV_VAR_KEY_DATABASE_USERNAME);
    String databasePassword = readEnvStringRequired(ENV_VAR_KEY_DATABASE_PASSWORD);
    int loaderThreads = readEnvIntPositiveRequired(ENV_VAR_KEY_LOADER_THREADS);
    boolean idempotencyRequired = readEnvBooleanRequired(ENV_VAR_KEY_IDEMPOTENCY_REQUIRED);
    Optional<String> newRelicMetricKey = readEnvStringOptional(ENV_VAR_NEW_RELIC_METRIC_KEY);
    Optional<String> newRelicAppName = readEnvStringOptional(ENV_VAR_NEW_RELIC_APP_NAME);
    Optional<String> newRelicMetricHost = readEnvStringOptional(ENV_VAR_NEW_RELIC_METRIC_HOST);
    Optional<String> newRelicMetricPath = readEnvStringOptional(ENV_VAR_NEW_RELIC_METRIC_PATH);
    Optional<Integer> newRelicMetricPeriod = readEnvIntOptional(ENV_VAR_NEW_RELIC_METRIC_PERIOD);

    /*
     * Note: For CcwRifLoadJob, databaseMaxPoolSize needs to be double the number of loader threads
     * when idempotent loads are being used. Apparently, the queries need a separate Connection?
     */
    Optional<Integer> databaseMaxPoolSize = readEnvIntOptional(ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE);
    if (databaseMaxPoolSize.isPresent() && databaseMaxPoolSize.get() < 1)
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, databaseMaxPoolSize));
    if (!databaseMaxPoolSize.isPresent()) databaseMaxPoolSize = Optional.of(loaderThreads * 2);

    Optional<String> rifFilterText = readEnvStringOptional(ENV_VAR_KEY_ALLOWED_RIF_TYPE);
    Optional<RifFileType> allowedRifFileType;
    if (rifFilterText.isPresent()) {
      try {
        allowedRifFileType = Optional.of(RifFileType.valueOf(rifFilterText.get()));
      } catch (IllegalArgumentException e) {
        throw new AppConfigurationException(
            String.format(
                "Invalid value for configuration environment variable '%s': '%s'",
                ENV_VAR_KEY_ALLOWED_RIF_TYPE, rifFilterText),
            e);
      }
    } else {
      allowedRifFileType = Optional.empty();
    }

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

    Optional<String> hostname;
    try {
      hostname = Optional.of(InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      hostname = Optional.empty();
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
        new DatabaseOptions(
            databaseUrl, databaseUsername, databasePassword, databaseMaxPoolSize.get());
    ExtractionOptions extractionOptions = new ExtractionOptions(s3BucketName, allowedRifFileType);
    LoadAppOptions loadOptions =
        new LoadAppOptions(
            new IdHasher.Config(hicnHashIterations, hicnHashPepper),
            loaderThreads,
            idempotencyRequired);
    CcwRifLoadOptions ccwRifLoadOptions = new CcwRifLoadOptions(extractionOptions, loadOptions);

    RdaLoadOptions rdaLoadOptions =
        readRdaLoadOptionsFromEnvironmentVariables(loadOptions.getIdHasherConfig());
    return new AppConfiguration(metricOptions, databaseOptions, ccwRifLoadOptions, rdaLoadOptions);
  }

  /**
   * Loads the configuration settings related to the RDA gRPC API data load jobs. Ths job and most
   * of its settings are optional. Because the API may exist in some environments but not others a
   * separate environment variable indicates whether or not the settings should be loaded.
   *
   * @return a valid RdaLoadOptions if job is configured, otherwise null
   */
  @Nullable
  static RdaLoadOptions readRdaLoadOptionsFromEnvironmentVariables(IdHasher.Config idHasherConfig) {
    final boolean enabled = readEnvBooleanOptional(ENV_VAR_KEY_RDA_JOB_ENABLED).orElse(false);
    if (!enabled) {
      return null;
    }
    final RdaLoadJob.Config jobConfig =
        new RdaLoadJob.Config(
            Duration.ofSeconds(
                readEnvIntOptional(ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS)
                    .orElse(DEFAULT_RDA_JOB_INTERVAL_SECONDS)),
            readEnvIntOptional(ENV_VAR_KEY_RDA_JOB_BATCH_SIZE).orElse(DEFAULT_RDA_JOB_BATCH_SIZE));
    final GrpcRdaSource.Config grpcConfig =
        new GrpcRdaSource.Config(
            readEnvStringOptional(ENV_VAR_KEY_RDA_GRPC_HOST).orElse(DEFAULT_RDA_GRPC_HOST),
            readEnvIntOptional(ENV_VAR_KEY_RDA_GRPC_PORT).orElse(DEFAULT_RDA_GRPC_PORT),
            Duration.ofSeconds(
                readEnvIntOptional(ENV_VAR_KEY_RDA_GRPC_MAX_IDLE_SECONDS)
                    .orElse(DEFAULT_RDA_GRPC_MAX_IDLE_SECONDS)));
    return new RdaLoadOptions(jobConfig, grpcConfig, idHasherConfig);
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   */
  static Optional<String> readEnvStringOptional(String environmentVariableName) {
    Optional<String> environmentVariableValue =
        Optional.ofNullable(System.getenv(environmentVariableName));
    return environmentVariableValue;
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value is missing.
   */
  static String readEnvStringRequired(String environmentVariableName) {
    Optional<String> environmentVariableValue =
        Optional.ofNullable(System.getenv(environmentVariableName));
    if (!environmentVariableValue.isPresent()) {
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              environmentVariableName));
    } else if (environmentVariableValue.get().isEmpty()) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              environmentVariableName, environmentVariableValue.get()));
    }

    return environmentVariableValue.get();
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  static Optional<Integer> readEnvIntOptional(String environmentVariableName) {
    Optional<String> environmentVariableValueText =
        Optional.ofNullable(System.getenv(environmentVariableName));
    if (!environmentVariableValueText.isPresent()) {
      return Optional.empty();
    }

    try {
      return Optional.of(Integer.valueOf(environmentVariableValueText.get()));
    } catch (NumberFormatException e) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              environmentVariableName, environmentVariableValueText.get()));
    }
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed or is not positive.
   */
  static int readEnvIntPositiveRequired(String environmentVariableName) {
    Optional<Integer> environmentVariableValue = readEnvIntOptional(environmentVariableName);
    if (!environmentVariableValue.isPresent()) {
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              environmentVariableName));
    } else if (environmentVariableValue.get() < 1) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              environmentVariableName, environmentVariableValue.get()));
    }

    return environmentVariableValue.get();
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  static Optional<Boolean> readEnvBooleanOptional(String environmentVariableName) {
    Optional<String> environmentVariableValueText =
        Optional.ofNullable(System.getenv(environmentVariableName));
    if (!environmentVariableValueText.isPresent()) {
      return Optional.empty();
    }

    if ("true".equalsIgnoreCase(environmentVariableValueText.get())) return Optional.of(true);
    else if ("false".equalsIgnoreCase(environmentVariableValueText.get()))
      return Optional.of(false);
    else
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              environmentVariableName, environmentVariableValueText.get()));
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  static boolean readEnvBooleanRequired(String environmentVariableName) {
    Optional<Boolean> environmentVariableValue = readEnvBooleanOptional(environmentVariableName);
    if (!environmentVariableValue.isPresent()) {
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              environmentVariableName));
    }

    return environmentVariableValue.get();
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @return the value of the specified environment variable
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  static byte[] readEnvBytesRequired(String environmentVariableName) {
    Optional<String> environmentVariableValueText =
        Optional.ofNullable(System.getenv(environmentVariableName));
    if (!environmentVariableValueText.isPresent()) {
      throw new AppConfigurationException(
          String.format(
              "Missing value for configuration environment variable '%s'.",
              environmentVariableName));
    }

    try {
      byte[] environmentVariableValue =
          Hex.decodeHex(environmentVariableValueText.get().toCharArray());
      return environmentVariableValue;
    } catch (DecoderException e) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              environmentVariableName, environmentVariableValueText.get()));
    }
  }
}
