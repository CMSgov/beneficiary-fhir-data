package gov.cms.bfd.pipeline.app;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaVersion;
import gov.cms.bfd.pipeline.rda.grpc.source.StandardGrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.MetricOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import io.micrometer.cloudwatch.CloudWatchConfig;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Models the configuration options for the application.
 *
 * <p>Note that, in addition to the configuration specified here, the application must also be
 * provided with credentials that can be used to access the specified S3 bucket. For that, the
 * application supports all of the mechanisms that are supported by {@link
 * DefaultAWSCredentialsProviderChain}, which include environment variables, EC2 instance profiles,
 * etc.
 */
public final class AppConfiguration extends BaseAppConfiguration implements Serializable {
  private static final long serialVersionUID = -6845504165285244536L;

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
   * #getCcwRifLoadOptions()} {@link IdHasher.Config#getHashIterations()} value.
   */
  public static final String ENV_VAR_KEY_HICN_HASH_ITERATIONS = "HICN_HASH_ITERATIONS";

  /**
   * The name of the environment variable that should be used to provide a hex encoded
   * representation of the {@link #getCcwRifLoadOptions()} {@link IdHasher.Config#getHashPepper()}
   * ()} value.
   */
  public static final String ENV_VAR_KEY_HICN_HASH_PEPPER = "HICN_HASH_PEPPER";

  /**
   * The name of the environment variable that should be used to provide an integer size for the
   * in-memory cache of computed hicn/mbi hash values. Used to set the {@link
   * IdHasher.Config#getCacheSize()}.
   */
  private static final String ENV_VAR_KEY_HICN_HASH_CACHE_SIZE = "HICN_HASH_CACHE_SIZE";

  /** Default value for {@link IdHasher.Config#getCacheSize()}. */
  private static final int DEFAULT_HICN_HASH_CACHE_SIZE = 100;

  /**
   * The name of the environment variable that should be used to indicate whether or not to
   * configure the CCW RIF data load job. Defaults to true to run the job unless disabled.
   */
  public static final String ENV_VAR_KEY_CCW_RIF_JOB_ENABLED = "CCW_RIF_JOB_ENABLED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#getLoaderThreads()} value.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment as it
   * depends on number of cores, cpu speed, i/o throughput, and database performance.
   */
  public static final String ENV_VAR_KEY_LOADER_THREADS = "LOADER_THREADS";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#isIdempotencyRequired()} value.
   */
  public static final String ENV_VAR_KEY_IDEMPOTENCY_REQUIRED = "IDEMPOTENCY_REQUIRED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * LoadAppOptions#isFilteringNonNullAndNon2023Benes()} value, which is a bit complex; please see
   * its description for details.
   */
  public static final String ENV_VAR_KEY_RIF_FILTERING_NON_NULL_AND_NON_2023_BENES =
      "FILTERING_NON_NULL_AND_NON_2023_BENES";

  /**
   * The default value to use for the {@link #ENV_VAR_KEY_RIF_FILTERING_NON_NULL_AND_NON_2023_BENES}
   * configuration environment variable when it is not set.
   *
   * <p>Note: This filtering option (and implementation) is an inelegant workaround, which should be
   * removed as soon as is reasonable.
   */
  public static final boolean DEFAULT_RIF_FILTERING_NON_NULL_AND_NON_2023_BENES = true;

  /**
   * The name of the environment variable that should be used to provide the number of {@link
   * RifRecordEvent}s that will be included in each processing batch. Note that larger batch sizes
   * mean that more {@link RifRecordEvent}s will be held in memory simultaneously.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment. Generally
   * the performance boost from larger batch sizes drops off quickly.
   */
  public static final String ENV_VAR_KEY_RIF_JOB_BATCH_SIZE = "RIF_JOB_BATCH_SIZE";

  /**
   * The default number of {@link RifRecordEvent}s that will be included in each processing batch.
   */
  private static final int DEFAULT_RIF_JOB_BATCH_SIZE = 25;

  /**
   * The name of the environment variable that should be used to provide the work queue size for the
   * RIF loader's thread pool. This number is multiplied by the number of worker threads to obtain
   * the actual queue size. Lower sizes are more memory efficient but larger sizes could provide a
   * performance improvement in some circumstances.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment. Generally
   * smaller is better. The default value provides some slack for handling intermittent database
   * slow downs without wasting too much RAM with large numbers of objects waiting to be sent to the
   * database.
   */
  public static final String ENV_VAR_KEY_RIF_JOB_QUEUE_SIZE_MULTIPLE =
      "RIF_JOB_QUEUE_SIZE_MULTIPLE";

  /**
   * The default value for the {@link #ENV_VAR_KEY_RIF_JOB_QUEUE_SIZE_MULTIPLE} environment
   * variable.
   */
  public static final int DEFAULT_RIF_JOB_QUEUE_SIZE_MULTIPLE = 2;

  /**
   * The name of the environment variable that should be used to indicate whether or not to
   * configure the RDA GRPC data load job. Defaults to false to not run the job unless enabled.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_ENABLED = "RDA_JOB_ENABLED";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link AbstractRdaLoadJob.Config#getRunInterval()} value. This variable's
   * value should be the frequency at which this job runs in seconds.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS = "RDA_JOB_INTERVAL_SECONDS";

  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS}. */
  public static final int DEFAULT_RDA_JOB_INTERVAL_SECONDS = 300;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link AbstractRdaLoadJob.Config#getBatchSize()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_BATCH_SIZE = "RDA_JOB_BATCH_SIZE";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_BATCH_SIZE}. */
  public static final int DEFAULT_RDA_JOB_BATCH_SIZE = 1;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link AbstractRdaLoadJob.Config#getWriteThreads()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_WRITE_THREADS = "RDA_JOB_WRITE_THREADS";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_WRITE_THREADS}. */
  public static final int DEFAULT_RDA_JOB_WRITE_THREADS = 1;

  /**
   * The name of the environment variable that specifies which type of RDA API server to connect to.
   * {@link RdaSourceConfig#getServerType()}
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_SERVER_TYPE = "RDA_GRPC_SERVER_TYPE";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_SERVER_TYPE}. */
  public static final RdaSourceConfig.ServerType DEFAULT_RDA_GRPC_SERVER_TYPE =
      RdaSourceConfig.ServerType.Remote;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link RdaSourceConfig#getHost()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_HOST = "RDA_GRPC_HOST";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_HOST}. */
  public static final String DEFAULT_RDA_GRPC_HOST = "localhost";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link RdaSourceConfig#getPort()} value.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_PORT = "RDA_GRPC_PORT";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_PORT}. */
  public static final int DEFAULT_RDA_GRPC_PORT = 443;

  /**
   * The name of the environment variable that specifies the name of an in-process mock RDA API
   * server. This name is used when instantiating the server as well as when connecting to it.
   * {@link RdaSourceConfig#getInProcessServerName()}
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_NAME =
      "RDA_GRPC_INPROC_SERVER_NAME";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_NAME}. */
  public static final String DEFAULT_RDA_GRPC_INPROC_SERVER_NAME = "MockRdaServer";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link RdaSourceConfig#getMaxIdle()} value. This variable value should be
   * in seconds.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_MAX_IDLE_SECONDS = "RDA_GRPC_MAX_IDLE_SECONDS";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS}. */
  public static final int DEFAULT_RDA_GRPC_MAX_IDLE_SECONDS = Integer.MAX_VALUE;

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link StandardGrpcRdaSource}'s minIdleMillisBeforeConnectionDrop value.
   * This variable value should be in seconds.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP =
      "RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP";
  /**
   * The default value for {@link
   * AppConfiguration#ENV_VAR_KEY_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP}.
   */
  public static final int DEFAULT_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP =
      (int) Duration.ofMinutes(4).toSeconds();

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link RdaSourceConfig#getAuthenticationToken()} value.
   */
  public static final String ENV_VAR_KEY_RDA_GRPC_AUTH_TOKEN = "RDA_GRPC_AUTH_TOKEN";
  /** The default value for {@link AppConfiguration#ENV_VAR_KEY_RDA_GRPC_AUTH_TOKEN}. */
  public static final String DEFAULT_RDA_GRPC_AUTH_TOKEN = null;

  /**
   * The name of the environment variable that should be used to indicate how many RDA messages can
   * error without causing the job to stop processing prematurely.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_ERROR_LIMIT = "RDA_JOB_ERROR_LIMIT";

  /**
   * The name of the environment variable that should be used to indicate the maximum number of days
   * that processed records can remain in the {@link MessageError} table.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_ERROR_EXPIRE_DAYS = "RDA_JOB_ERROR_EXPIRE_DAYS";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link AbstractRdaLoadJob.Config#getStartingFissSeqNum()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_STARTING_FISS_SEQ_NUM =
      "RDA_JOB_STARTING_FISS_SEQ_NUM";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getRdaLoadOptions()} {@link AbstractRdaLoadJob.Config#getStartingMcsSeqNum()} ()} value.
   */
  public static final String ENV_VAR_KEY_RDA_JOB_STARTING_MCS_SEQ_NUM =
      "RDA_JOB_STARTING_MCS_SEQ_NUM";

  /**
   * The name of the boolean environment variable that should be used to determine if the {@link
   * gov.cms.bfd.pipeline.rda.grpc.source.DLQGrpcRdaSource} task should be run on subsequent job
   * runs.
   */
  public static final String ENV_VAR_KEY_PROCESS_DLQ = "RDA_JOB_PROCESS_DLQ";

  /**
   * The name of the string environment variable that can be set to override the RDA API Version
   * that the running job should be configured to ingest data for. The job will normally use the
   * default value hardcoded in the code, but this env variable can be used for special
   * circumstances.
   */
  public static final String ENV_VAR_KEY_RDA_VERSION = "RDA_JOB_RDA_VERSION";

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

  /**
   * Environment variable containing the namespace to use when sending Micrometer metrics to
   * CloudWatch. This is a required environment variable if {@link #ENV_VAR_KEY_CCW_RIF_JOB_ENABLED}
   * is set to true.
   */
  public static final String ENV_VAR_MICROMETER_CW_NAMESPACE = "MICROMETER_CW_NAMESPACE";

  /**
   * Environment variable containing the update interval to use when sending Micrometer metrics to
   * CloudWatch. The value must be in ISO-8601 format as parsed by {@link Duration#parse}. Default
   * value is PT1M (1 minute). More frequent updates provide higher resolution but can also increase
   * CW costs.
   */
  public static final String ENV_VAR_MICROMETER_CW_INTERVAL = "MICROMETER_CW_INTERVAL";

  /**
   * Environment variable indicating whether Micrometer metrics should be sent to CloudWatch.
   * Defaults to false.
   */
  public static final String ENV_VAR_MICROMETER_CW_ENABLED = "MICROMETER_CW_ENABLED";

  /**
   * Environment variable indicating whether Micrometer metrics should be sent to JMX. Defaults to
   * false. Can be used when testing the pipeline locally to monitor metrics as the pipeline runs.
   */
  public static final String ENV_VAR_MICROMETER_JMX_ENABLED = "MICROMETER_JMX_ENABLED";

  /**
   * List of metric names that are allowed to be published to Cloudwatch by Micrometer. Using an
   * allowed list avoids increasing AWS charges as new metrics are defined for use in NewRelic that
   * are not necessary in Cloudwatch. These need to be the base metric names, not one of the several
   * auto-generated aggregate metric names with suffixes like {@code .avg}.
   */
  public static Set<String> MICROMETER_CW_ALLOWED_METRIC_NAMES =
      Set.of("FissClaimRdaSink.change.latency.millis", "McsClaimRdaSink.change.latency.millis");

  /**
   * The CCW rif load options. This can be null if the CCW job is not configured, Optional is not
   * Serializable.
   */
  @Nullable private final CcwRifLoadOptions ccwRifLoadOptions;
  /**
   * The RDA rif load options. This can be null if the RDA job is not configured, Optional is not
   * Serializable.
   */
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
    super(metricOptions, databaseOptions);
    this.ccwRifLoadOptions = ccwRifLoadOptions;
    this.rdaLoadOptions = rdaLoadOptions;
  }

  /**
   * Gets the {@link #ccwRifLoadOptions}.
   *
   * @return the {@link CcwRifLoadOptions} that the application will use
   */
  public Optional<CcwRifLoadOptions> getCcwRifLoadOptions() {
    return Optional.ofNullable(ccwRifLoadOptions);
  }

  /**
   * Gets the {@link #rdaLoadOptions}.
   *
   * @return the {@link RdaLoadOptions} that the application will use
   */
  public Optional<RdaLoadOptions> getRdaLoadOptions() {
    return Optional.ofNullable(rdaLoadOptions);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(super.toString());
    builder.append("AppConfiguration [");
    builder.append("ccwRifLoadOptions=");
    builder.append(ccwRifLoadOptions);
    builder.append(", rdaLoadOptions=");
    builder.append(rdaLoadOptions);
    builder.append("]");
    return builder.toString();
  }

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided {@link ConfigLoader} is used to look up variables related to where to
   * find other sources of config variables.
   *
   * <p>Config values will be loaded from these sources. Sources are checked in order with first
   * matching value used.
   *
   * <ol>
   *   <li>System properties.
   *   <li>Environment variables.
   *   <li>If {@link #ENV_VAR_KEY_PROPERTIES_FILE} is defined use properties in that file.
   *   <li>If {@link #ENV_VAR_KEY_SSM_PARAMETER_PATH} is defined use parameters at that path.
   * </ol>
   *
   * @param baseConfig used to check for properties file and SSM path
   * @return appropriately configured {@link ConfigLoader}
   */
  static ConfigLoader createConfigLoader(ConfigLoader baseConfig) {
    final var configBuilder = ConfigLoader.builder();

    final var ssmPath = baseConfig.stringValue(ENV_VAR_KEY_SSM_PARAMETER_PATH, "");
    if (ssmPath.length() > 0) {
      ensureAwsCredentialsConfiguredCorrectly();
      final var ssmClient = AWSSimpleSystemsManagementClient.builder();
      baseConfig
          .parsedOption(ENV_VAR_KEY_SSM_REGION, Regions.class, Regions::fromName)
          .ifPresent(r -> ssmClient.setRegion(r.getName()));
      final var parameterStore = new AwsParameterStoreClient(ssmClient.build());
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

    configBuilder.addEnvironmentVariables();
    configBuilder.addSystemProperties();
    return configBuilder.build();
  }

  /**
   * Load configuration variables using the provided {@link ConfigLoader} instance and build an
   * {@link AppConfiguration} instance from them.
   *
   * <p>As a convenience, this method will also verify that AWS credentials were provided, such that
   * {@link DefaultAWSCredentialsProviderChain} can load them. If not, an {@link
   * AppConfigurationException} will be thrown.
   *
   * @param config used to load configuration values
   * @return the {@link AppConfiguration} instance
   * @throws ConfigException will be thrown if the configuration passed to the application are
   *     incomplete or incorrect.
   */
  static AppConfiguration loadConfig(ConfigLoader config) {
    int hicnHashIterations = config.positiveIntValue(ENV_VAR_KEY_HICN_HASH_ITERATIONS);
    byte[] hicnHashPepper = config.hexBytes(ENV_VAR_KEY_HICN_HASH_PEPPER);
    int hicnHashCacheSize =
        config.intOption(ENV_VAR_KEY_HICN_HASH_CACHE_SIZE).orElse(DEFAULT_HICN_HASH_CACHE_SIZE);

    int loaderThreads = config.positiveIntValue(ENV_VAR_KEY_LOADER_THREADS);
    boolean idempotencyRequired = config.booleanValue(ENV_VAR_KEY_IDEMPOTENCY_REQUIRED);
    boolean filteringNonNullAndNon2023Benes =
        config
            .booleanOption(ENV_VAR_KEY_RIF_FILTERING_NON_NULL_AND_NON_2023_BENES)
            .orElse(DEFAULT_RIF_FILTERING_NON_NULL_AND_NON_2023_BENES);
    int rifRecordBatchSize =
        config.intOption(ENV_VAR_KEY_RIF_JOB_BATCH_SIZE).orElse(DEFAULT_RIF_JOB_BATCH_SIZE);
    int rifTaskQueueSizeMultiple =
        config
            .intOption(ENV_VAR_KEY_RIF_JOB_QUEUE_SIZE_MULTIPLE)
            .orElse(DEFAULT_RIF_JOB_QUEUE_SIZE_MULTIPLE);

    MetricOptions metricOptions = loadMetricOptions(config);
    DatabaseOptions databaseOptions = loadDatabaseOptions(config, loaderThreads);

    LoadAppOptions loadOptions =
        new LoadAppOptions(
            IdHasher.Config.builder()
                .hashIterations(hicnHashIterations)
                .hashPepper(hicnHashPepper)
                .cacheSize(hicnHashCacheSize)
                .build(),
            loaderThreads,
            idempotencyRequired,
            filteringNonNullAndNon2023Benes,
            rifRecordBatchSize,
            rifTaskQueueSizeMultiple);

    CcwRifLoadOptions ccwRifLoadOptions = loadCcwRifLoadOptions(config, loadOptions);

    RdaLoadOptions rdaLoadOptions = loadRdaLoadOptions(config, loadOptions.getIdHasherConfig());
    return new AppConfiguration(metricOptions, databaseOptions, ccwRifLoadOptions, rdaLoadOptions);
  }

  /**
   * Reads database options from the {@link ConfigLoader}.
   *
   * @param config used to load configuration values
   * @param loaderThreads the number loader threads, to determine fallback value for database max
   *     pool size
   * @return the database options
   */
  private static DatabaseOptions loadDatabaseOptions(ConfigLoader config, int loaderThreads) {
    DatabaseOptions databaseOptions = loadDatabaseOptions(config);

    Optional<Integer> databaseMaxPoolSize = config.intOption(ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE);

    if (databaseMaxPoolSize.isPresent() && databaseMaxPoolSize.get() < 1) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, databaseMaxPoolSize));
    }

    /*
     * Note: For CcwRifLoadJob, databaseMaxPoolSize needs to be double the number of loader threads
     * when idempotent loads are being used. Apparently, the queries need a separate Connection?
     */
    if (databaseMaxPoolSize.isEmpty()) {
      databaseMaxPoolSize = Optional.of(loaderThreads * 2);
    }

    return new DatabaseOptions(
        databaseOptions.getDatabaseUrl(), databaseOptions.getDatabaseUsername(),
        databaseOptions.getDatabasePassword(), databaseMaxPoolSize.orElse(1));
  }

  /**
   * Reads the ccw rif load options from the {@link ConfigLoader}.
   *
   * @param config used to load configuration values
   * @param loadOptions the load options to use when creating the {link CcwRifLoadOptions}
   * @return the ccw rif load options
   */
  @Nullable
  static CcwRifLoadOptions loadCcwRifLoadOptions(ConfigLoader config, LoadAppOptions loadOptions) {
    final boolean enabled = config.booleanOption(ENV_VAR_KEY_CCW_RIF_JOB_ENABLED).orElse(true);
    if (!enabled) {
      return null;
    }

    final String s3BucketName = config.stringValue(ENV_VAR_KEY_BUCKET);
    final Optional<String> rifFilterText = config.stringOptionEmptyOK(ENV_VAR_KEY_ALLOWED_RIF_TYPE);
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

    ensureAwsCredentialsConfiguredCorrectly();
    ExtractionOptions extractionOptions = new ExtractionOptions(s3BucketName, allowedRifFileType);
    return new CcwRifLoadOptions(extractionOptions, loadOptions);
  }

  /*
   * Just for convenience: makes sure DefaultAWSCredentialsProviderChain has whatever it needs before
   * we try to use any AWS resources.
   */
  static void ensureAwsCredentialsConfiguredCorrectly() {
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

  /**
   * Loads the configuration settings related to the RDA gRPC API data load jobs. Ths job and most
   * of its settings are optional. Because the API may exist in some environments but not others a
   * separate environment variable indicates whether the settings should be loaded.
   *
   * @param config used to load configuration values
   * @param idHasherConfig the id hasher config
   * @return a valid RdaLoadOptions if job is configured, otherwise null
   */
  @Nullable
  static RdaLoadOptions loadRdaLoadOptions(ConfigLoader config, IdHasher.Config idHasherConfig) {
    final boolean enabled = config.booleanOption(ENV_VAR_KEY_RDA_JOB_ENABLED).orElse(false);
    if (!enabled) {
      return null;
    }
    final AbstractRdaLoadJob.Config.ConfigBuilder jobConfig =
        AbstractRdaLoadJob.Config.builder()
            .runInterval(
                Duration.ofSeconds(
                    config.intValue(
                        ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS, DEFAULT_RDA_JOB_INTERVAL_SECONDS)))
            .batchSize(config.intValue(ENV_VAR_KEY_RDA_JOB_BATCH_SIZE, DEFAULT_RDA_JOB_BATCH_SIZE))
            .writeThreads(
                config.intValue(ENV_VAR_KEY_RDA_JOB_WRITE_THREADS, DEFAULT_RDA_JOB_WRITE_THREADS));
    config
        .longOption(ENV_VAR_KEY_RDA_JOB_STARTING_FISS_SEQ_NUM)
        .ifPresent(jobConfig::startingFissSeqNum);
    config
        .longOption(ENV_VAR_KEY_RDA_JOB_STARTING_MCS_SEQ_NUM)
        .ifPresent(jobConfig::startingMcsSeqNum);
    config.booleanOption(ENV_VAR_KEY_PROCESS_DLQ).ifPresent(jobConfig::processDLQ);
    // Default to the hardcoded RDA version in RdaService, restricted to major version
    jobConfig.rdaVersion(
        RdaVersion.builder()
            .versionString(
                config
                    .stringOption(ENV_VAR_KEY_RDA_VERSION)
                    .orElse("^" + RdaService.RDA_PROTO_VERSION))
            .build());
    jobConfig.sinkTypePreference(AbstractRdaLoadJob.SinkTypePreference.NONE);
    final RdaSourceConfig grpcConfig =
        RdaSourceConfig.builder()
            .serverType(
                config
                    .enumOption(ENV_VAR_KEY_RDA_GRPC_SERVER_TYPE, RdaSourceConfig.ServerType.class)
                    .orElse(DEFAULT_RDA_GRPC_SERVER_TYPE))
            .host(config.stringValue(ENV_VAR_KEY_RDA_GRPC_HOST, DEFAULT_RDA_GRPC_HOST))
            .port(config.intValue(ENV_VAR_KEY_RDA_GRPC_PORT, DEFAULT_RDA_GRPC_PORT))
            .inProcessServerName(
                config.stringValue(
                    ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_NAME, DEFAULT_RDA_GRPC_INPROC_SERVER_NAME))
            .maxIdle(
                Duration.ofSeconds(
                    config.intValue(
                        ENV_VAR_KEY_RDA_GRPC_MAX_IDLE_SECONDS, DEFAULT_RDA_GRPC_MAX_IDLE_SECONDS)))
            .minIdleTimeBeforeConnectionDrop(
                Duration.ofSeconds(
                    config.intValue(
                        ENV_VAR_KEY_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP,
                        DEFAULT_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP)))
            .authenticationToken(
                config
                    .stringOptionEmptyOK(ENV_VAR_KEY_RDA_GRPC_AUTH_TOKEN)
                    .orElse(DEFAULT_RDA_GRPC_AUTH_TOKEN))
            .messageErrorExpirationDays(
                config.intOption(ENV_VAR_KEY_RDA_JOB_ERROR_EXPIRE_DAYS).orElse(null))
            .build();
    final RdaServerJob.Config.ConfigBuilder mockServerConfig = RdaServerJob.Config.builder();
    mockServerConfig.serverMode(
        config
            .enumOption(
                ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_MODE, RdaServerJob.Config.ServerMode.class)
            .orElse(RdaServerJob.Config.ServerMode.Random));
    mockServerConfig.serverName(grpcConfig.getInProcessServerName());
    config
        .longOption(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS)
        .map(Duration::ofSeconds)
        .ifPresent(mockServerConfig::runInterval);
    config
        .longOption(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_RANDOM_SEED)
        .ifPresent(mockServerConfig::randomSeed);
    config
        .intOption(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS)
        .ifPresent(mockServerConfig::randomMaxClaims);
    config
        .parsedOption(
            ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_REGION, Regions.class, Regions::fromName)
        .ifPresent(mockServerConfig::s3Region);
    config
        .stringOptionEmptyOK(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_BUCKET)
        .ifPresent(mockServerConfig::s3Bucket);
    config
        .stringOptionEmptyOK(ENV_VAR_KEY_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY)
        .ifPresent(mockServerConfig::s3Directory);
    final int errorLimit = config.intValue(ENV_VAR_KEY_RDA_JOB_ERROR_LIMIT, 0);

    return new RdaLoadOptions(
        jobConfig.build(), grpcConfig, mockServerConfig.build(), errorLimit, idHasherConfig);
  }

  /**
   * Checks configuration settings to determine if the feed of Micrometer metrics to JMX should be
   * enabled.
   *
   * @param config used to load configuration values
   * @return true if the feed should be configured
   */
  public static boolean isJmxMetricsEnabled(ConfigLoader config) {
    return config.booleanValue(ENV_VAR_MICROMETER_JMX_ENABLED, false);
  }

  /**
   * Creates an implementation of {@link CloudWatchConfig} that looks for environment variables to
   * find values for properties. Environment variable lookup is done using a {@link
   * MicrometerConfigHelper}.
   *
   * @param config used to load configuration values
   * @return an instance of {@link CloudWatchConfig}
   * @throws ConfigException thrown if any required properties are missing or if any environment
   *     variables have invalid values.
   */
  public static CloudWatchConfig loadCloudWatchRegistryConfig(ConfigLoader config) {
    final var micrometerConfigHelper = createMicrometerConfigHelper(config);
    final CloudWatchConfig cwConfig = micrometerConfigHelper::get;
    if (cwConfig.enabled()) {
      micrometerConfigHelper.throwIfConfigurationNotValid(cwConfig.validate());
    }
    return cwConfig;
  }

  /**
   * Creates an instance of {@link MicrometerConfigHelper} used to create a {@link CloudWatchConfig}
   * instance. Contains the property name to environment variable name mappings for supported {@link
   * CloudWatchConfig} properties as well as default values for some environment variables.
   *
   * @param config used to load configuration values
   * @return the instance
   */
  static MicrometerConfigHelper createMicrometerConfigHelper(ConfigLoader config) {
    return new MicrometerConfigHelper(
        List.of(
            new MicrometerConfigHelper.PropertyMapping(
                "cloudwatch.enabled", ENV_VAR_MICROMETER_CW_ENABLED, Optional.of("false")),
            new MicrometerConfigHelper.PropertyMapping(
                "cloudwatch.namespace", ENV_VAR_MICROMETER_CW_NAMESPACE, Optional.empty()),
            new MicrometerConfigHelper.PropertyMapping(
                "cloudwatch.step", ENV_VAR_MICROMETER_CW_INTERVAL, Optional.of("PT1M"))),
        varName -> config.stringValue(varName, null));
  }
}
