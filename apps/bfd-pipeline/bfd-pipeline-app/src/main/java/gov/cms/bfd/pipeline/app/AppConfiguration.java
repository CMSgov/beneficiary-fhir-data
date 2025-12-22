package gov.cms.bfd.pipeline.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaVersion;
import gov.cms.bfd.pipeline.rda.grpc.source.StandardGrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.npi_fda.NpiFdaLoadJobConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.BackfillConfigOptions;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.BaseAppConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Models the configuration options for the application.
 *
 * <p>Note that, in addition to the configuration specified here, the application must also be
 * provided with credentials that can be used to access the specified S3 bucket. For that, the
 * application supports all of the mechanisms that are supported by {@link
 * DefaultCredentialsProvider}, which include environment variables, EC2 instance profiles, etc.
 */
public final class AppConfiguration extends BaseAppConfiguration {

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * ExtractionOptions#getS3BucketName()} value.
   */
  public static final String SSM_PATH_BUCKET = "ccw/s3_bucket_name";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * ExtractionOptions#getDataSetFilter()} value: This environment variable specifies the {@link
   * RifFileType} that will be processed. Any {@link DataSetManifest}s that contain other {@link
   * RifFileType}s will be skipped entirely (even if they <em>also</em> contain the allowed {@link
   * RifFileType}. For example, specifying "BENEFICIARY" will configure the application to only
   * process data sets that <strong>only</strong> contain {@link RifFileType#BENEFICIARY}s.
   */
  public static final String SSM_PATH_ALLOWED_RIF_TYPE = "ccw/job/allowed_data_set_type";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link IdHasher.Config#getHashIterations()} value.
   */
  public static final String SSM_PATH_HICN_HASH_ITERATIONS = "hicn_hash/iterations";

  /**
   * The path of the SSM parameter that should be used to provide a hex encoded representation of
   * the {@link #getCcwRifLoadOptions()} {@link IdHasher.Config#getHashPepper()} ()} value.
   */
  public static final String SSM_PATH_HICN_HASH_PEPPER = "hicn_hash/pepper";

  /**
   * The path of the SSM parameter that should be used to provide an integer size for the in-memory
   * cache of computed hicn/mbi hash values. Used to set the {@link IdHasher.Config#getCacheSize()}.
   */
  private static final String SSM_PATH_HICN_HASH_CACHE_SIZE = "hicn_hash/cache_size";

  /**
   * The path of the SSM parameter that should be used to indicate whether or not to configure the
   * CCW RIF data load job. Defaults to true to run the job unless disabled.
   */
  public static final String SSM_PATH_CCW_RIF_JOB_ENABLED = "ccw/job/enabled";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link AbstractRdaLoadJob.Config#getRunInterval()} value. This variable's value should be the
   * frequency at which this job runs in seconds.
   */
  public static final String SSM_PATH_CCW_RIF_JOB_INTERVAL_SECONDS = "ccw/job/interval_seconds";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions.PerformanceSettings#getLoaderThreads()} value.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment as it
   * depends on number of cores, cpu speed, i/o throughput, and database performance.
   */
  public static final String SSM_PATH_LOADER_THREADS = "loader_thread_count";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions.PerformanceSettings#getLoaderThreads()} value
   * specific to processing claims data.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment as it
   * depends on number of cores, cpu speed, i/o throughput, and database performance.
   */
  public static final String SSM_PATH_LOADER_THREADS_CLAIMS = "ccw/job/claims/loader_thread_count";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * #getCcwRifLoadOptions()} {@link LoadAppOptions#isIdempotencyRequired()} value.
   */
  public static final String SSM_PATH_IDEMPOTENCY_REQUIRED = "ccw/idempotency_enabled";

  /**
   * The path of the SSM parameter that should be used to provide the number of {@link
   * RifRecordEvent}s that will be included in each processing batch. Note that larger batch sizes
   * mean that more {@link RifRecordEvent}s will be held in memory simultaneously.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment. Generally
   * the performance boost from larger batch sizes drops off quickly.
   */
  public static final String SSM_PATH_RIF_JOB_BATCH_SIZE = "ccw/job/batch_size";

  /**
   * The path of the SSM parameter that should be used to provide the number of {@link
   * RifRecordEvent}s that will be included in each processing batch specific to processing claims
   * data. Note that larger batch sizes mean that more {@link RifRecordEvent}s will be held in
   * memory simultaneously.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment. Generally
   * the performance boost from larger batch sizes drops off quickly.
   */
  public static final String SSM_PATH_RIF_JOB_BATCH_SIZE_CLAIMS = "ccw/job/claims/batch_size";

  /**
   * The path of the SSM parameter that should be used to provide the work queue size for the RIF
   * loader's thread pool. This number is multiplied by the number of worker threads to obtain the
   * actual queue size. Lower sizes are more memory efficient but larger sizes could provide a
   * performance improvement in some circumstances.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment. Generally
   * smaller is better. The default value provides some slack for handling intermittent database
   * slow downs without wasting too much RAM with large numbers of objects waiting to be sent to the
   * database.
   */
  public static final String SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE = "ccw/job/queue_size_multiple";

  /**
   * The path of the SSM parameter that should be used to provide the work queue size for the RIF
   * loader's thread pool when processing claims data. This number is multiplied by the number of
   * worker threads to obtain the actual queue size. Lower sizes are more memory efficient but
   * larger sizes could provide a performance improvement in some circumstances.
   *
   * <p>Benchmarking is necessary to determine an optimal value in any given environment. Generally
   * smaller is better. The default value provides some slack for handling intermittent database
   * slow downs without wasting too much RAM with large numbers of objects waiting to be sent to the
   * database.
   */
  public static final String SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE_CLAIMS =
      "ccw/job/claims/queue_size_multiple";

  /**
   * Path of the SSM parameter containing name of SQS queue to which progress messages can be sent.
   */
  public static final String CCW_JOB_SQS_STATUS_QUEUE_NAME = "ccw/job/sqs/status_queue_name";

  /**
   * The path of the SSM parameter that should be used to indicate whether or not to configure the
   * RDA GRPC data load job. Defaults to false to not run the job unless enabled.
   */
  public static final String SSM_PATH_RDA_JOB_ENABLED = "rda/job/enabled";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link AbstractRdaLoadJob.Config#getRunInterval()} value. This variable's value should be the
   * frequency at which this job runs in seconds.
   */
  public static final String SSM_PATH_RDA_JOB_INTERVAL_SECONDS = "rda/job/interval_seconds";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link AbstractRdaLoadJob.Config#getBatchSize()} value.
   */
  public static final String SSM_PATH_RDA_JOB_BATCH_SIZE = "rda/job/batch_size";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link AbstractRdaLoadJob.Config#getWriteThreads()} value.
   */
  public static final String SSM_PATH_RDA_JOB_WRITE_THREADS = "rda/job/write_thread_count";

  /**
   * The path of the SSM parameter that specifies which type of RDA API server to connect to. {@link
   * RdaSourceConfig#getServerType()}
   */
  public static final String SSM_PATH_RDA_GRPC_SERVER_TYPE = "rda/grpc/server_type";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link RdaSourceConfig#getHost()} ()} value.
   */
  public static final String SSM_PATH_RDA_GRPC_HOST = "rda/grpc/host";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link RdaSourceConfig#getPort()} value.
   */
  public static final String SSM_PATH_RDA_GRPC_PORT = "rda/grpc/port";

  /**
   * The path of the SSM parameter that specifies the name of an in-process mock RDA API server.
   * This name is used when instantiating the server as well as when connecting to it. {@link
   * RdaSourceConfig#getInProcessServerName()}
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_NAME =
      "rda/grpc/inprocess_server/name";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link RdaSourceConfig#getMaxIdle()} value. This variable value should be in seconds.
   */
  public static final String SSM_PATH_RDA_GRPC_MAX_IDLE_SECONDS = "rda/grpc/max_idle_seconds";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link StandardGrpcRdaSource}'s minIdleMillisBeforeConnectionDrop value. This variable value
   * should be in seconds.
   */
  public static final String SSM_PATH_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP =
      "rda/grpc/seconds_before_connection_drop";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link RdaSourceConfig#getAuthenticationToken()} value.
   */
  public static final String SSM_PATH_RDA_GRPC_AUTH_TOKEN = "rda/grpc/auth_token";

  /**
   * The path of the SSM parameter that should be used to provide the sequence number update
   * interval.
   */
  public static final String SSM_PATH_RDA_JOB_SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS =
      "rda/job/sequence_range_update_interval_seconds";

  /**
   * The path of the SSM parameter that should be used to indicate how many RDA messages can error
   * without causing the job to stop processing prematurely.
   */
  public static final String SSM_PATH_RDA_JOB_ERROR_LIMIT = "rda/job/error_limit";

  /**
   * The path of the SSM parameter that should be used to indicate the maximum number of days that
   * processed records can remain in the {@link MessageError} table.
   */
  public static final String SSM_PATH_RDA_JOB_ERROR_EXPIRE_DAYS = "rda/job/error_expire_days";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link AbstractRdaLoadJob.Config#getStartingFissSeqNum()} ()} value.
   */
  public static final String SSM_PATH_RDA_JOB_STARTING_FISS_SEQ_NUM =
      "rda/job/starting_fiss_seq_num";

  /**
   * The path of the SSM parameter that should be used to provide the {@link #getRdaLoadOptions()}
   * {@link AbstractRdaLoadJob.Config#getStartingMcsSeqNum()} ()} value.
   */
  public static final String SSM_PATH_RDA_JOB_STARTING_MCS_SEQ_NUM = "rda/job/starting_mcs_seq_num";

  /**
   * The path of the SSM parameter that should be used to determine if the {@link
   * gov.cms.bfd.pipeline.rda.grpc.source.DLQGrpcRdaSource} task should be run on subsequent job
   * runs.
   */
  public static final String SSM_PATH_PROCESS_DLQ = "rda/job/process_dlq";

  /**
   * The path of the SSM parameter that should be used to determine if the cleanup tasks that remove
   * old FISS and MCS claims should be run on subsequent job runs.
   */
  public static final String SSM_PATH_CLEANUP_ENABLED = "rda/cleanup/enabled";

  /** The maximum number of claims that will be removed during a cleanup task run. */
  public static final String SSM_PATH_CLEANUP_RUN_SIZE = "rda/cleanup/run_size";

  /** The number of claims to remove as part of a single transaction during a cleanup task run. */
  public static final String SSM_PATH_CLEANUP_TRANSACTION_SIZE = "rda/cleanup/transaction_size";

  /**
   * The path of the SSM parameter that can be set to override the RDA API Version that the running
   * job should be configured to ingest data for. The job will normally use the default value
   * hardcoded in the code, but this env variable can be used for special circumstances.
   */
  public static final String SSM_PATH_RDA_VERSION = "rda/job/rda_version";

  /**
   * The path of the SSM parameter that should be used to provide the {@link
   * gov.cms.bfd.pipeline.rda.grpc.RdaServerJob.Config.ServerMode} value for the in-process RDA API
   * server.
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_MODE =
      "rda/grpc/inprocess_server/mode";

  /**
   * The path of the SSM parameter that should be used to provide the run interval in seconds for
   * the in-process RDA API server job.
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS =
      "rda/grpc/inprocess_server/interval_seconds";

  /**
   * The path of the SSM parameter that should be used to provide the random number generator for
   * the PRNG used by the the in-process RDA API server job's random mode.
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_SEED =
      "rda/grpc/inprocess_server/random/seed";

  /**
   * The path of the SSM parameter that should be used to provide the maximum number of random
   * claims to be returned to clients by the in-process RDA API server job's random mode.
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS =
      "rda/grpc/inprocess_server/random/max_claims";

  /**
   * The path of the SSM parameter that should be used to provide the name of the S3 bucket used to
   * serve claims by the in-process RDA API server job's random mode.
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_BUCKET =
      "rda/grpc/inprocess_server/s3/bucket";

  /**
   * The path of the SSM parameter that should be used to provide a directory path to add as a
   * prefix when looking for files within the S3 bucket. This is optional and defaults to objects
   * being accessed at the bucket's root.
   */
  public static final String SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY =
      "rda/grpc/inprocess_server/s3/dir";

  /**
   * Path of the SSM parameter containing the namespace to use when sending Micrometer metrics to
   * CloudWatch. This is a required environment variable if {@link #SSM_PATH_CCW_RIF_JOB_ENABLED} is
   * set to true.
   */
  public static final String SSM_PATH_MICROMETER_CW_NAMESPACE = "micrometer_cw/namespace";

  /**
   * Path of the SSM parameter containing the update interval to use when sending Micrometer metrics
   * to CloudWatch. The value must be in ISO-8601 format as parsed by {@link Duration#parse}.
   * Default value is PT1M (1 minute). More frequent updates provide higher resolution but can also
   * increase CW costs.
   */
  public static final String SSM_PATH_MICROMETER_CW_INTERVAL = "micrometer_cw/interval";

  /**
   * Path of the SSM parameter indicating whether Micrometer metrics should be sent to CloudWatch.
   * Defaults to false.
   */
  public static final String SSM_PATH_MICROMETER_CW_ENABLED = "micrometer_cw/enabled";

  /**
   * Path of the SSM parameter indicating whether Micrometer metrics should be sent to JMX. Defaults
   * to false. Can be used when testing the pipeline locally to monitor metrics as the pipeline
   * runs.
   */
  public static final String SSM_PATH_MICROMETER_JMX_ENABLED = "micrometer_cw/jmx_enabled";

  /** Enabled flag for NpiFdaLoadJob. */
  public static final String SSM_PATH_NPI_FDA_LOAD_JOB = "rda/npi_fda_load_job/enabled";

  /** The number of records to save before commiting a transaction. */
  public static final String SSM_PATH_NPI_FDA_BATCH_SIZE = "rda/npi_fda_load_job/batch_size";

  /** The run interval for the NPI Load Job, in days. */
  public static final String SSM_PATH_NPI_FDA_RUN_INTERVAL = "rda/npi_fda_load_job/run_interval";

  /**
   * List of metric names that are allowed to be published to Cloudwatch by Micrometer. Using an
   * allowed list avoids increasing AWS charges as new metrics may not be necessary to send to
   * Cloudwatch. These need to be the base metric names, not one of the several auto-generated
   * aggregate metric names with suffixes like {@code .avg}.
   */
  public static final Set<String> MICROMETER_CW_ALLOWED_METRIC_NAMES =
      Set.of(
          "FissClaimRdaSink.change.latency.millis",
          "McsClaimRdaSink.change.latency.millis",
          "FissClaimRdaSink.lastSeq",
          "McsClaimRdaSink.lastSeq",
          "FissClaimRdaSink.maxSeq",
          "McsClaimRdaSink.maxSeq",
          CcwRifLoadJob.Metrics.DATASET_PROCESSING_ACTIVE_TIMER_NAME,
          CcwRifLoadJob.Metrics.DATASET_PROCESSING_TOTAL_TIMER_NAME,
          CcwRifLoadJob.Metrics.MANIFEST_PROCESSING_ACTIVE_TIMER_NAME,
          CcwRifLoadJob.Metrics.MANIFEST_PROCESSING_TOTAL_TIMER_NAME,
          DefaultDataSetMonitorListener.Metrics.RIF_FILE_PROCESSING_ACTIVE_TIMER_NAME,
          DefaultDataSetMonitorListener.Metrics.RIF_FILE_PROCESSING_TOTAL_TIMER_NAME);

  /** Config value for SAMHSA backfill enabled. */
  public static final String SSM_PATH_SAMHSA_BACKFILL_ENABLED = "rda/samhsa/backfill/enabled";

  /** Config value for SAMHSA backfill batch size. */
  public static final String SSM_PATH_SAMHSA_BACKFILL_BATCH_SIZE = "rda/samhsa/backfill/batch_size";

  /** Config value for SAMHSA backfill log reporting interval. */
  public static final String SSM_PATH_SAMHSA_BACKFILL_LOG_INTERVAL =
      "rda/samhsa/backfill/log_interval";

  /**
   * The CCW rif load options. This can be null if the CCW job is not configured, Optional is not
   * Serializable.
   */
  @Nullable private final CcwRifLoadOptions ccwRifLoadOptions;

  /** Configuration for Backfill Config. */
  @Nullable private final BackfillConfigOptions backfillConfigOptions;

  /**
   * The RDA rif load options. This can be null if the RDA job is not configured, Optional is not
   * Serializable.
   */
  @Nullable private final RdaLoadOptions rdaLoadOptions;

  /** The Options for NpiFdaLoadJob. */
  @Nullable private final NpiFdaLoadJobConfig npiFdaLoadConfig;

  /** All of the default configuration values. These will be used as the last layer in config. */
  private static final Map<String, String> DEFAULT_CONFIG_VALUES =
      ImmutableMap.<String, String>builder()
          .put(SSM_PATH_HICN_HASH_CACHE_SIZE, "100")
          .put(SSM_PATH_RIF_JOB_BATCH_SIZE, "25")
          .put(SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE, "2")
          .put(SSM_PATH_CCW_RIF_JOB_INTERVAL_SECONDS, "30")
          .put(SSM_PATH_RDA_JOB_INTERVAL_SECONDS, "300")
          .put(SSM_PATH_RDA_JOB_BATCH_SIZE, "1")
          .put(SSM_PATH_RDA_JOB_WRITE_THREADS, "1")
          .put(SSM_PATH_RDA_JOB_SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS, "300")
          .put(SSM_PATH_RDA_GRPC_SERVER_TYPE, RdaSourceConfig.ServerType.Remote.name())
          .put(SSM_PATH_RDA_GRPC_HOST, "localhost")
          .put(SSM_PATH_RDA_GRPC_PORT, "443")
          .put(SSM_PATH_RDA_GRPC_INPROC_SERVER_NAME, "MockRdaServer")
          .put(SSM_PATH_RDA_GRPC_MAX_IDLE_SECONDS, String.valueOf(Integer.MAX_VALUE))
          .put(SSM_PATH_NPI_FDA_LOAD_JOB, "false")
          .put(SSM_PATH_NPI_FDA_BATCH_SIZE, "100000")
          .put(SSM_PATH_NPI_FDA_RUN_INTERVAL, "30")
          .put(
              SSM_PATH_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP,
              String.valueOf(Duration.ofMinutes(4).toSeconds()))
          .put(SSM_PATH_SAMHSA_BACKFILL_ENABLED, "false")
          .put(SSM_PATH_SAMHSA_BACKFILL_BATCH_SIZE, String.valueOf(10000))
          .put(SSM_PATH_SAMHSA_BACKFILL_LOG_INTERVAL, String.valueOf(3600))
          .build();

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param databaseOptions the value to use for {@link #getDatabaseOptions()}
   * @param awsClientConfig used to configure AWS services
   * @param ccwRifLoadOptions the value to use for {@link #getCcwRifLoadOptions()}
   * @param rdaLoadOptions the value to use for {@link #getRdaLoadOptions()}
   * @param backFillConfigOptions the value to use fo {@link #getBackfillConfigOptions()}
   * @param npiFdaLoadConfig the value to use for {@link #getNpiFdaLoadConfigOptions}
   */
  private AppConfiguration(
      DatabaseOptions databaseOptions,
      AwsClientConfig awsClientConfig,
      @Nullable CcwRifLoadOptions ccwRifLoadOptions,
      @Nullable RdaLoadOptions rdaLoadOptions,
      @Nullable BackfillConfigOptions backFillConfigOptions,
      @Nullable NpiFdaLoadJobConfig npiFdaLoadConfig) {
    super(databaseOptions, awsClientConfig);
    this.ccwRifLoadOptions = ccwRifLoadOptions;
    this.rdaLoadOptions = rdaLoadOptions;
    this.backfillConfigOptions = backFillConfigOptions;
    this.npiFdaLoadConfig = npiFdaLoadConfig;
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

  /**
   * returns the {@link #backfillConfigOptions}.
   *
   * @return the {@link BackfillConfigOptions}
   */
  public Optional<BackfillConfigOptions> getBackfillConfigOptions() {
    return Optional.ofNullable(backfillConfigOptions);
  }

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
   * Get the config options for NpiFdaLoadJob.
   *
   * @return the config.
   */
  public Optional<NpiFdaLoadJobConfig> getNpiFdaLoadConfigOptions() {
    return Optional.ofNullable(npiFdaLoadConfig);
  }

  /**
   * Loads the config for NpiFdaLoadJob.
   *
   * @param config The ConfigLoader.
   * @param ccwPipelineEnabled True is the CCW pipeline is enabled. We only want to run in an RDA
   *     pipeline instance.
   * @return The NpiFdaLoadJob configuration.
   */
  public static NpiFdaLoadJobConfig loadNpiFdaLoadConfig(
      ConfigLoader config, boolean ccwPipelineEnabled) {
    boolean enabled = config.booleanOption(SSM_PATH_NPI_FDA_LOAD_JOB).orElse(false);
    if (!enabled || ccwPipelineEnabled) {
      return null;
    }
    int batchSize = config.intValue(SSM_PATH_NPI_FDA_BATCH_SIZE, 100000);
    int runInterval = config.intValue(SSM_PATH_NPI_FDA_RUN_INTERVAL, 30);
    return NpiFdaLoadJobConfig.builder()
        .enabled(enabled)
        .batchSize(batchSize)
        .runInterval(runInterval)
        .build();
  }

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided {@link ConfigLoaderSource} is used to look up environment variables
   * so that these can be simulated in tests without having to fork a process.
   *
   * <p>{@see LayeredConfiguration#createConfigLoader} for possible sources of configuration
   * variables.
   *
   * @param getenv {@link ConfigLoaderSource} used to access environment variables (provided
   *     explicitly for testing)
   * @return appropriately configured {@link ConfigLoader}
   */
  static ConfigLoader createConfigLoader(ConfigLoaderSource getenv) {
    return LayeredConfiguration.createConfigLoader(DEFAULT_CONFIG_VALUES, getenv);
  }

  /**
   * Build a {@link ConfigLoader} for use in tests that just uses default values and those in the
   * provided {@link Map}.
   *
   * @param getenv map containing environment variables
   * @return appropriately configured {@link ConfigLoader}
   */
  static ConfigLoader createConfigLoaderForTesting(Map<String, String> getenv) {
    return ConfigLoader.builder().addMap(DEFAULT_CONFIG_VALUES).addMap(getenv).build();
  }

  /**
   * Loads Backfill Configuration.
   *
   * @param config Config loader
   * @param ccwPipelineEnabled True if the CCW pipeline is enabled in this instance.
   * @return {@link BackfillConfigOptions}
   */
  public static BackfillConfigOptions loadBackfillConfigOptions(
      ConfigLoader config, boolean ccwPipelineEnabled) {
    boolean enabled = config.booleanOption(SSM_PATH_SAMHSA_BACKFILL_ENABLED).orElse(false);
    // We don't want to run if we're on a CCW Pipeline instance
    if (!enabled || ccwPipelineEnabled) {
      return null;
    }
    int batchSize = config.intValue(SSM_PATH_SAMHSA_BACKFILL_BATCH_SIZE, 10000);
    Long logInterval = config.longValue(SSM_PATH_SAMHSA_BACKFILL_LOG_INTERVAL, 3600);
    BackfillConfigOptions backfillConfigOptions =
        BackfillConfigOptions.builder()
            .enabled(enabled)
            .batchSize(batchSize)
            .logInterval(logInterval)
            .build();
    return backfillConfigOptions;
  }

  /**
   * Load configuration variables using the provided {@link ConfigLoader} instance and build an
   * {@link AppConfiguration} instance from them.
   *
   * <p>As a convenience, this method will also verify that AWS credentials were provided, such that
   * {@link DefaultCredentialsProvider} can load them. If not, an {@link AppConfigurationException}
   * will be thrown.
   *
   * @param config used to load configuration values
   * @return the {@link AppConfiguration} instance
   * @throws ConfigException will be thrown if the configuration passed to the application are
   *     incomplete or incorrect.
   */
  static AppConfiguration loadConfig(ConfigLoader config) {
    int hicnHashIterations = config.positiveIntValue(SSM_PATH_HICN_HASH_ITERATIONS);
    byte[] hicnHashPepper = config.hexBytes(SSM_PATH_HICN_HASH_PEPPER);
    int hicnHashCacheSize = config.intValue(SSM_PATH_HICN_HASH_CACHE_SIZE);
    final boolean idempotencyRequired = config.booleanValue(SSM_PATH_IDEMPOTENCY_REQUIRED);

    final var benePerformanceSettings = loadBeneficiaryPerformanceSettings(config);
    final var claimPerformanceSettings =
        loadClaimPerformanceSettings(config, benePerformanceSettings);
    final int maxLoaderThreads =
        Math.max(
            benePerformanceSettings.getLoaderThreads(),
            claimPerformanceSettings.getLoaderThreads());
    DatabaseOptions databaseOptions = loadDatabaseOptions(config, maxLoaderThreads);

    LoadAppOptions loadOptions =
        new LoadAppOptions(
            IdHasher.Config.builder()
                .hashIterations(hicnHashIterations)
                .hashPepper(hicnHashPepper)
                .cacheSize(hicnHashCacheSize)
                .build(),
            idempotencyRequired,
            benePerformanceSettings,
            claimPerformanceSettings);

    CcwRifLoadOptions ccwRifLoadOptions = loadCcwRifLoadOptions(config, loadOptions);
    RdaLoadOptions rdaLoadOptions = loadRdaLoadOptions(config, loadOptions.getIdHasherConfig());
    BackfillConfigOptions backfillConfigOptions =
        loadBackfillConfigOptions(config, ccwRifLoadOptions != null);
    AwsClientConfig awsClientConfig = BaseAppConfiguration.loadAwsClientConfig(config);
    NpiFdaLoadJobConfig npiFdaConfig = loadNpiFdaLoadConfig(config, ccwRifLoadOptions != null);
    return new AppConfiguration(
        databaseOptions,
        awsClientConfig,
        ccwRifLoadOptions,
        rdaLoadOptions,
        backfillConfigOptions,
        npiFdaConfig);
  }

  /**
   * Loads beneficiary specific {@link LoadAppOptions.PerformanceSettings}.
   *
   * @param config used to load configuration values
   * @return the loaded settings
   */
  static LoadAppOptions.PerformanceSettings loadBeneficiaryPerformanceSettings(
      ConfigLoader config) {
    return new LoadAppOptions.PerformanceSettings(
        config.positiveIntValue(SSM_PATH_LOADER_THREADS),
        config.positiveIntValue(SSM_PATH_RIF_JOB_BATCH_SIZE),
        config.positiveIntValue(SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE));
  }

  /**
   * Loads optional claim specific {@link LoadAppOptions.PerformanceSettings}. Uses the provided
   * beneficiary settings to obtain values if the claim specific settings are not present in the
   * {@link ConfigLoader}.
   *
   * @param config used to load configuration values
   * @param benePerformanceSettings used to get default values
   * @return the loaded settings
   */
  static LoadAppOptions.PerformanceSettings loadClaimPerformanceSettings(
      ConfigLoader config, LoadAppOptions.PerformanceSettings benePerformanceSettings) {
    return new LoadAppOptions.PerformanceSettings(
        config.positiveIntValue(
            SSM_PATH_LOADER_THREADS_CLAIMS, benePerformanceSettings.getLoaderThreads()),
        config.positiveIntValue(
            SSM_PATH_RIF_JOB_BATCH_SIZE_CLAIMS, benePerformanceSettings.getRecordBatchSize()),
        config.positiveIntValue(
            SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE_CLAIMS,
            benePerformanceSettings.getTaskQueueSizeMultiple()));
  }

  /**
   * Reads database options from the {@link ConfigLoader}.
   *
   * @param config used to load configuration values
   * @param loaderThreads the number loader threads, to determine fallback value for database max
   *     pool size
   * @return the database options
   */
  static DatabaseOptions loadDatabaseOptions(ConfigLoader config, int loaderThreads) {
    DatabaseOptions databaseOptions = loadDatabaseOptions(config);

    Optional<Integer> databaseMaxPoolSize = config.intOption(SSM_PATH_DB_HIKARI_MAX_POOL_SIZE);

    if (databaseMaxPoolSize.isPresent() && databaseMaxPoolSize.get() < 1) {
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s': '%s'",
              SSM_PATH_DB_HIKARI_MAX_POOL_SIZE, databaseMaxPoolSize));
    }

    /*
     * Note: For CcwRifLoadJob, databaseMaxPoolSize needs to be double the number of loader threads
     * when idempotent loads are being used. Apparently, the queries need a separate Connection?
     */
    if (databaseMaxPoolSize.isEmpty()) {
      databaseMaxPoolSize = Optional.of(loaderThreads * 2);
    }

    return databaseOptions.toBuilder()
        .hikariOptions(
            DatabaseOptions.HikariOptions.builder()
                .maximumPoolSize(databaseMaxPoolSize.orElse(1))
                .build())
        .build();
  }

  /**
   * Loads {@link S3ClientConfig} for use in configuring S3 clients. These settings are generally
   * only changed from defaults during localstack based tests.
   *
   * @param config used to load configuration values
   * @return the aws client settings
   */
  static S3ClientConfig loadS3ServiceConfig(ConfigLoader config) {
    return S3ClientConfig.s3Builder().awsClientConfig(loadAwsClientConfig(config)).build();
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
    final boolean enabled = config.booleanOption(SSM_PATH_CCW_RIF_JOB_ENABLED).orElse(true);
    if (!enabled) {
      return null;
    }

    final String s3BucketName = config.stringValue(SSM_PATH_BUCKET);
    final Optional<String> rifFilterText = config.stringOptionEmptyOK(SSM_PATH_ALLOWED_RIF_TYPE);
    final Optional<RifFileType> allowedRifFileType;
    if (rifFilterText.isPresent()) {
      try {
        allowedRifFileType = Optional.of(RifFileType.valueOf(rifFilterText.get()));
      } catch (IllegalArgumentException e) {
        throw new AppConfigurationException(
            String.format(
                "Invalid value for configuration environment variable '%s': '%s'",
                SSM_PATH_ALLOWED_RIF_TYPE, rifFilterText),
            e);
      }
    } else {
      allowedRifFileType = Optional.empty();
    }
    final S3ClientConfig s3ClientConfig = loadS3ServiceConfig(config);
    if (s3ClientConfig.getAwsClientConfig().isCredentialCheckUseful()) {
      LayeredConfiguration.ensureAwsCredentialsConfiguredCorrectly();
    }
    final ExtractionOptions extractionOptions =
        new ExtractionOptions(s3BucketName, allowedRifFileType, Optional.empty(), s3ClientConfig);
    final Optional<Duration> runInterval =
        config
            .positiveIntOptionZeroOK(SSM_PATH_CCW_RIF_JOB_INTERVAL_SECONDS)
            .map(Duration::ofSeconds);
    final Optional<String> sqsQueueName = config.stringOption(CCW_JOB_SQS_STATUS_QUEUE_NAME);
    return new CcwRifLoadOptions(extractionOptions, loadOptions, runInterval, sqsQueueName);
  }

  /**
   * Loads the common configuration settings used by various implementations of the {@link
   * AbstractRdaLoadJob} abstract class.
   *
   * @param config used to load configuration values
   * @return a valid AbstractRdaLoadJob.Config
   */
  @VisibleForTesting
  static AbstractRdaLoadJob.Config loadRdaLoadJobConfigOptions(ConfigLoader config) {
    final AbstractRdaLoadJob.Config.ConfigBuilder jobConfig =
        AbstractRdaLoadJob.Config.builder()
            .runInterval(Duration.ofSeconds(config.intValue(SSM_PATH_RDA_JOB_INTERVAL_SECONDS)))
            .batchSize(config.intValue(SSM_PATH_RDA_JOB_BATCH_SIZE))
            .writeThreads(config.intValue(SSM_PATH_RDA_JOB_WRITE_THREADS));
    config
        .longOption(SSM_PATH_RDA_JOB_STARTING_FISS_SEQ_NUM)
        .map(seq -> Math.max(1L, seq))
        .ifPresent(jobConfig::startingFissSeqNum);
    config
        .longOption(SSM_PATH_RDA_JOB_STARTING_MCS_SEQ_NUM)
        .map(seq -> Math.max(1L, seq))
        .ifPresent(jobConfig::startingMcsSeqNum);
    config.booleanOption(SSM_PATH_PROCESS_DLQ).ifPresent(jobConfig::processDLQ);
    config.booleanOption(SSM_PATH_CLEANUP_ENABLED).ifPresent(jobConfig::runCleanup);
    config.intOption(SSM_PATH_CLEANUP_RUN_SIZE).ifPresent(jobConfig::cleanupRunSize);
    config
        .intOption(SSM_PATH_CLEANUP_TRANSACTION_SIZE)
        .ifPresent(jobConfig::cleanupTransactionSize);
    // Default to the hardcoded RDA version in RdaService, restricted to major version
    jobConfig.rdaVersion(
        RdaVersion.builder()
            .versionString(
                config
                    .stringOption(SSM_PATH_RDA_VERSION)
                    .orElse("^" + RdaService.RDA_PROTO_VERSION))
            .build());
    jobConfig.sinkTypePreference(AbstractRdaLoadJob.SinkTypePreference.NONE);
    return jobConfig.build();
  }

  /**
   * Loads the common configuration settings used by various implementations of the {@link
   * RdaSource} interface.
   *
   * @param config used to load configuration values
   * @return a valid RdaSourceConfig
   */
  @VisibleForTesting
  static RdaSourceConfig loadRdaSourceConfig(ConfigLoader config) {
    return RdaSourceConfig.builder()
        .serverType(
            config.enumValue(SSM_PATH_RDA_GRPC_SERVER_TYPE, RdaSourceConfig.ServerType.class))
        .host(config.stringValue(SSM_PATH_RDA_GRPC_HOST))
        .port(config.intValue(SSM_PATH_RDA_GRPC_PORT))
        .inProcessServerName(config.stringValue(SSM_PATH_RDA_GRPC_INPROC_SERVER_NAME))
        .maxIdle(Duration.ofSeconds(config.intValue(SSM_PATH_RDA_GRPC_MAX_IDLE_SECONDS)))
        .minIdleTimeBeforeConnectionDrop(
            Duration.ofSeconds(config.intValue(SSM_PATH_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP)))
        .authenticationToken(config.stringOptionEmptyOK(SSM_PATH_RDA_GRPC_AUTH_TOKEN).orElse(null))
        .sequenceRangeUpdateIntervalSeconds(
            config.longValue(SSM_PATH_RDA_JOB_SEQUENCE_RANGE_UPDATE_INTERVAL_SECONDS))
        .messageErrorExpirationDays(
            config.intOption(SSM_PATH_RDA_JOB_ERROR_EXPIRE_DAYS).orElse(null))
        .build();
  }

  /**
   * Loads configuration settings used by {@link RdaServerJob}.
   *
   * @param config used to load configuration values
   * @param grpcConfig settings for communicating with RDA API
   * @param serverJobConfigBuilder used to construct the config settings
   * @return a valid RdaServerJob.Config
   */
  @VisibleForTesting
  static RdaServerJob.Config loadRdaServerJobConfig(
      ConfigLoader config,
      RdaSourceConfig grpcConfig,
      RdaServerJob.Config.ConfigBuilder serverJobConfigBuilder) {
    serverJobConfigBuilder.serverMode(
        config
            .enumOption(SSM_PATH_RDA_GRPC_INPROC_SERVER_MODE, RdaServerJob.Config.ServerMode.class)
            .orElse(RdaServerJob.Config.ServerMode.Random));
    serverJobConfigBuilder.serverName(grpcConfig.getInProcessServerName());
    S3ClientConfig s3ClientConfig = loadS3ServiceConfig(config);
    serverJobConfigBuilder.s3ClientConfig(s3ClientConfig);
    config
        .longOption(SSM_PATH_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS)
        .map(Duration::ofSeconds)
        .ifPresent(serverJobConfigBuilder::runInterval);
    config
        .longOption(SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_SEED)
        .ifPresent(serverJobConfigBuilder::randomSeed);
    config
        .intOption(SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS)
        .ifPresent(serverJobConfigBuilder::randomMaxClaims);
    config
        .stringOptionEmptyOK(SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_BUCKET)
        .ifPresent(serverJobConfigBuilder::s3Bucket);
    config
        .stringOptionEmptyOK(SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY)
        .ifPresent(serverJobConfigBuilder::s3Directory);
    return serverJobConfigBuilder.build();
  }

  /**
   * Loads the configuration settings related to the RDA gRPC API data load jobs. This job and most
   * of its settings are optional. Because the API may exist in some environments but not others a
   * separate environment variable indicates whether the settings should be loaded.
   *
   * @param config used to load configuration values
   * @param idHasherConfig the id hasher config
   * @return a valid RdaLoadOptions if job is configured, otherwise null
   */
  @Nullable
  static RdaLoadOptions loadRdaLoadOptions(ConfigLoader config, IdHasher.Config idHasherConfig) {
    final boolean enabled = config.booleanOption(SSM_PATH_RDA_JOB_ENABLED).orElse(false);
    if (!enabled) {
      return null;
    }

    final AbstractRdaLoadJob.Config jobConfig = loadRdaLoadJobConfigOptions(config);
    final RdaSourceConfig grpcConfig = loadRdaSourceConfig(config);
    final RdaServerJob.Config serverJobConfigBuilder =
        loadRdaServerJobConfig(config, grpcConfig, RdaServerJob.Config.builder());

    final int errorLimit = config.intValue(SSM_PATH_RDA_JOB_ERROR_LIMIT, 0);
    return new RdaLoadOptions(
        jobConfig, grpcConfig, serverJobConfigBuilder, errorLimit, idHasherConfig);
  }

  /**
   * Checks configuration settings to determine if the feed of Micrometer metrics to JMX should be
   * enabled.
   *
   * @param config used to load configuration values
   * @return true if the feed should be configured
   */
  public static boolean isJmxMetricsEnabled(ConfigLoader config) {
    return config.booleanValue(SSM_PATH_MICROMETER_JMX_ENABLED, false);
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
                "cloudwatch.enabled", SSM_PATH_MICROMETER_CW_ENABLED, Optional.of("false")),
            new MicrometerConfigHelper.PropertyMapping(
                "cloudwatch.namespace", SSM_PATH_MICROMETER_CW_NAMESPACE, Optional.empty()),
            new MicrometerConfigHelper.PropertyMapping(
                "cloudwatch.step", SSM_PATH_MICROMETER_CW_INTERVAL, Optional.of("PT1M"))),
        varName -> config.stringValue(varName, null));
  }
}
