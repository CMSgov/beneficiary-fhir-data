package gov.cms.bfd.pipeline.app;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
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
   * The name of the environment variable that should be used to indicate whether or not to
   * configure the CCW RIF data load job. Defaults to true to run the job unless disabled.
   */
  public static final String ENV_VAR_KEY_CCW_RIF_JOB_ENABLED = "CCW_RIF_JOB_ENABLED";

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
   * configure the RDA GRPC data load job. Defaults to false to not run the job unless enabled.
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
   * The name of the environment variable that specifies which type of RDA API server to connect to.
   * {@link GrpcRdaSource.Config#getServerType()}
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_SERVER_TYPE = "RDA_GRPC_SERVER_TYPE";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_TYPE}. */
  public static final GrpcRdaSource.Config.ServerType DEFAULT_RDA_GRPC_SERVER_TYPE =
      GrpcRdaSource.Config.ServerType.Remote;

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
   * The name of the environment variable that specifies the name of an in-process mock RDA API
   * server. This name is used when instantiating the server as well as when connecting to it.
   * {@link GrpcRdaSource.Config#getInProcessServerName()}
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_NAME =
      "RDA_GRPC_INPROC_SERVER_NAME";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_NAME} */
  public static final String DEFAULT_RDA_GRPC_INPROC_SERVER_NAME = "MockRdaServer";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link GrpcRdaSource.Config#getMaxIdle()} ()} value. This variable value
   * should be in seconds.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_MAX_IDLE_SECONDS = "RDA_GRPC_MAX_IDLE_SECONDS";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS}. */
  public static final int DEFAULT_RDA_GRPC_MAX_IDLE_SECONDS = Integer.MAX_VALUE;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link GrpcRdaSource.Config#getStartingFissSeqNum()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_STARTING_FISS_SEQ_NUM =
      "RDA_JOB_STARTING_FISS_SEQ_NUM";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link GrpcRdaSource.Config#getStartingMcsSeqNum()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_STARTING_MCS_SEQ_NUM =
      "RDA_JOB_STARTING_MCS_SEQ_NUM";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * gov.cms.bfd.pipeline.rda.grpc.RdaServerJob.Config.ServerMode} value for the in-process RDA API
   * server.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_MODE =
      "RDA_GRPC_INPROC_SERVER_MODE";

  /**
   * The name of the environment variable that should be used to provide the run interval in seconds
   * for the in-process RDA API server job.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS =
      "RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS";

  /**
   * The name of the environment variable that should be used to provide the random number generator
   * for the PRNG used by the the in-process RDA API server job's random mode.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_RANDOM_SEED =
      "RDA_GRPC_INPROC_SERVER_RANDOM_SEED";

  /**
   * The name of the environment variable that should be used to provide the maximum number of
   * random claims to be returned to clients by the in-process RDA API server job's random mode.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS =
      "RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS";

  /**
   * The name of the environment variable that should be used to provide the name of the S3 region
   * containing the bucket used to serve claims by the in-process RDA API server job's random mode.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_REGION =
      "RDA_GRPC_INPROC_SERVER_S3_REGION";

  /**
   * The name of the environment variable that should be used to provide the name of the S3 bucket
   * used to serve claims by the in-process RDA API server job's random mode.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_BUCKET =
      "RDA_GRPC_INPROC_SERVER_S3_BUCKET";

  /**
   * The name of the environment variable that should be used to provide a directory path to add as
   * a prefix when looking for files within the S3 bucket. This is optional and defaults to objects
   * being accessed at the bucket's root.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY =
      "RDA_GRPC_INPROC_SERVER_S3_DIRECTORY";

  private final MetricOptions metricOptions;
  private final DatabaseOptions databaseOptions;
  // this can be null if the RDA job is not configured, Optional is not Serializable
  @Nullable private final CcwRifLoadOptions ccwRifLoadOptions;
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
  private AppConfiguration(
      MetricOptions metricOptions,
      DatabaseOptions databaseOptions,
      @Nullable CcwRifLoadOptions ccwRifLoadOptions,
      @Nullable RdaLoadOptions rdaLoadOptions) {
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
  public Optional<CcwRifLoadOptions> getCcwRifLoadOptions() {
    return Optional.ofNullable(ccwRifLoadOptions);
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
    LoadAppOptions loadOptions =
        new LoadAppOptions(
            new IdHasher.Config(hicnHashIterations, hicnHashPepper),
            loaderThreads,
            idempotencyRequired);

    CcwRifLoadOptions ccwRifLoadOptions =
        readCcwRifLoadOptionsFromEnvironmentVariables(loadOptions);

    RdaLoadOptions rdaLoadOptions =
        readRdaLoadOptionsFromEnvironmentVariables(loadOptions.getIdHasherConfig());
    return new AppConfiguration(metricOptions, databaseOptions, ccwRifLoadOptions, rdaLoadOptions);
  }

  @Nullable
  static CcwRifLoadOptions readCcwRifLoadOptionsFromEnvironmentVariables(
      LoadAppOptions loadOptions) {
    final boolean enabled = readEnvBooleanOptional(ENV_VAR_KEY_CCW_RIF_JOB_ENABLED).orElse(true);
    if (!enabled) {
      return null;
    }

    final String s3BucketName = readEnvStringRequired(ENV_VAR_KEY_BUCKET);
    final Optional<String> rifFilterText = readEnvStringOptional(ENV_VAR_KEY_ALLOWED_RIF_TYPE);
    final Optional<RifFileType> allowedRifFileType;
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
    ExtractionOptions extractionOptions = new ExtractionOptions(s3BucketName, allowedRifFileType);
    CcwRifLoadOptions ccwRifLoadOptions = new CcwRifLoadOptions(extractionOptions, loadOptions);
    return ccwRifLoadOptions;
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
    final AbstractRdaLoadJob.Config.ConfigBuilder jobConfig =
        AbstractRdaLoadJob.Config.builder()
            .runInterval(
                Duration.ofSeconds(
                    readEnvParsedOptional(ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS, Integer::parseInt)
                        .orElse(DEFAULT_RDA_JOB_INTERVAL_SECONDS)))
            .batchSize(
                readEnvParsedOptional(ENV_VAR_KEY_RDA_JOB_BATCH_SIZE, Integer::parseInt)
                    .orElse(DEFAULT_RDA_JOB_BATCH_SIZE));
    readEnvParsedOptional(ENV_VAR_KEY_RDA_JOB_STARTING_FISS_SEQ_NUM, Long::parseLong)
        .ifPresent(jobConfig::startingFissSeqNum);
    readEnvParsedOptional(ENV_VAR_KEY_RDA_JOB_STARTING_MCS_SEQ_NUM, Long::parseLong)
        .ifPresent(jobConfig::startingMcsSeqNum);
    final GrpcRdaSource.Config grpcConfig =
        GrpcRdaSource.Config.builder()
            .serverType(
                readEnvParsedOptional(
                        ENV_VAR_KEY_RDA_GRPC_SERVER_TYPE, GrpcRdaSource.Config.ServerType::valueOf)
                    .orElse(DEFAULT_RDA_GRPC_SERVER_TYPE))
            .host(
                readEnvNonEmptyStringOptional(ENV_VAR_KEY_RDA_GRPC_HOST)
                    .orElse(DEFAULT_RDA_GRPC_HOST))
            .port(
                readEnvParsedOptional(ENV_VAR_KEY_RDA_GRPC_PORT, Integer::parseInt)
                    .orElse(DEFAULT_RDA_GRPC_PORT))
            .inProcessServerName(
                readEnvNonEmptyStringOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_NAME)
                    .orElse(DEFAULT_RDA_GRPC_INPROC_SERVER_NAME))
            .maxIdle(
                Duration.ofSeconds(
                    readEnvParsedOptional(ENV_VAR_KEY_RDA_GRPC_MAX_IDLE_SECONDS, Integer::parseInt)
                        .orElse(DEFAULT_RDA_GRPC_MAX_IDLE_SECONDS)))
            .build();
    final RdaServerJob.Config.ConfigBuilder mockServerConfig = RdaServerJob.Config.builder();
    mockServerConfig.serverMode(
        readEnvParsedOptional(
                ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_MODE, RdaServerJob.Config.ServerMode::valueOf)
            .orElse(RdaServerJob.Config.ServerMode.Random));
    mockServerConfig.serverName(grpcConfig.getInProcessServerName());
    readEnvParsedOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS, Long::parseLong)
        .map(Duration::ofSeconds)
        .ifPresent(mockServerConfig::runInterval);
    readEnvParsedOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_RANDOM_SEED, Long::parseLong)
        .ifPresent(mockServerConfig::randomSeed);
    readEnvParsedOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS, Integer::parseInt)
        .ifPresent(mockServerConfig::randomMaxClaims);
    readEnvParsedOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_REGION, Regions::fromName)
        .ifPresent(mockServerConfig::s3Region);
    readEnvStringOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_BUCKET)
        .ifPresent(mockServerConfig::s3Bucket);
    readEnvStringOptional(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY)
        .ifPresent(mockServerConfig::s3Directory);
    return new RdaLoadOptions(
        jobConfig.build(), grpcConfig, mockServerConfig.build(), idHasherConfig);
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
   * @return the value of the specified environment variable, or {@link Optional#empty()} if it is
   *     not set or contains an empty value
   */
  static Optional<String> readEnvNonEmptyStringOptional(String environmentVariableName) {
    return readEnvStringOptional(environmentVariableName)
        .map(String::trim)
        .filter(s -> !s.isEmpty());
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
              "Invalid value for configuration environment variable '%s': '%s' (%s)",
              environmentVariableName, environmentVariableValueText.get(), e.getMessage()),
          e);
    }
  }

  /**
   * @param environmentVariableName the name of the environment variable to get the value of
   * @param parser the function used to convert the name into a parsed value
   * @return the value of the specified environment variable converted to a parsed value
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     value cannot be parsed.
   */
  static <T> Optional<T> readEnvParsedOptional(
      String environmentVariableName, Function<String, T> parser) {
    Optional<String> environmentVariableValueText =
        readEnvNonEmptyStringOptional(environmentVariableName);

    try {
      return environmentVariableValueText.map(parser);
    } catch (RuntimeException e) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s' (%s)",
              environmentVariableName, environmentVariableValueText.get(), e.getMessage()),
          e);
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
              "Invalid value for configuration environment variable '%s': '%s' (%s)",
              environmentVariableName, environmentVariableValueText.get(), e.getMessage()),
          e);
    }
  }
}
