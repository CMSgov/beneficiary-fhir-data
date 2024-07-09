package gov.cms.bfd.pipeline.app;

import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_CLEANUP_ENABLED;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_CLEANUP_RUN_SIZE;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_CLEANUP_TRANSACTION_SIZE;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_AUTH_TOKEN;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_HOST;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_MODE;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_NAME;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_SEED;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_BUCKET;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_MAX_IDLE_SECONDS;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_PORT;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_GRPC_SERVER_TYPE;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_JOB_BATCH_SIZE;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_JOB_ERROR_EXPIRE_DAYS;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_JOB_INTERVAL_SECONDS;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_JOB_STARTING_FISS_SEQ_NUM;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_JOB_STARTING_MCS_SEQ_NUM;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_JOB_WRITE_THREADS;
import static gov.cms.bfd.pipeline.app.AppConfiguration.SSM_PATH_RDA_VERSION;
import static gov.cms.bfd.pipeline.app.AppConfiguration.loadBeneficiaryPerformanceSettings;
import static gov.cms.bfd.pipeline.app.AppConfiguration.loadClaimPerformanceSettings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.AbstractRdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaService;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaVersion;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DefaultHikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.RdsDataSourceFactory;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.core.instrument.config.validate.ValidationException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

/** Unit tests for {@link AppConfiguration}. */
public class AppConfigurationTest {
  /**
   * Verifies that {@link AppConfiguration#loadConfig} works as expected when passed valid
   * configuration environment variables.
   *
   * @throws Exception (indicates a test error)
   */
  @Test
  public void normalUsage() throws Exception {
    final var envVars = new HashMap<String, String>();
    envVars.put(AppConfiguration.SSM_PATH_BUCKET, "foo");
    envVars.put(AppConfiguration.SSM_PATH_ALLOWED_RIF_TYPE, RifFileType.BENEFICIARY.name());
    envVars.put(
        AppConfiguration.SSM_PATH_HICN_HASH_ITERATIONS,
        String.valueOf(CcwRifLoadTestUtils.HICN_HASH_ITERATIONS));
    envVars.put(
        AppConfiguration.SSM_PATH_HICN_HASH_PEPPER,
        Hex.encodeHexString(CcwRifLoadTestUtils.HICN_HASH_PEPPER));
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_URL, "some_url");
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_USERNAME, "some_user");
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_PASSWORD, "some_password");
    envVars.put(AppConfiguration.SSM_PATH_LOADER_THREADS, "42");
    envVars.put(AppConfiguration.SSM_PATH_IDEMPOTENCY_REQUIRED, "true");
    envVars.put(AppConfiguration.ENV_VAR_AWS_ENDPOINT, "http://localhost:999999");
    envVars.put(AppConfiguration.ENV_VAR_AWS_ACCESS_KEY, "unreal-access-key");
    envVars.put(AppConfiguration.ENV_VAR_AWS_SECRET_KEY, "unreal-secret-key");
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(envVars);
    AppConfiguration testAppConfig = AppConfiguration.loadConfig(configLoader);

    assertNotNull(testAppConfig);
    assertEquals(
        envVars.get(AppConfiguration.SSM_PATH_BUCKET),
        testAppConfig.getCcwRifLoadOptions().get().getExtractionOptions().getS3BucketName());
    assertEquals(
        envVars.get(AppConfiguration.SSM_PATH_ALLOWED_RIF_TYPE),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getExtractionOptions()
            .getAllowedRifFileType()
            .get()
            .name());
    assertEquals(
        Integer.parseInt(envVars.get(AppConfiguration.SSM_PATH_HICN_HASH_ITERATIONS)),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getLoadOptions()
            .getIdHasherConfig()
            .getHashIterations());
    assertArrayEquals(
        Hex.decodeHex(envVars.get(AppConfiguration.SSM_PATH_HICN_HASH_PEPPER).toCharArray()),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getLoadOptions()
            .getIdHasherConfig()
            .getHashPepper());

    assertEquals(
        DatabaseOptions.AuthenticationType.JDBC,
        testAppConfig.getDatabaseOptions().getAuthenticationType());
    assertEquals(
        envVars.get(AppConfiguration.SSM_PATH_DATABASE_URL),
        testAppConfig.getDatabaseOptions().getDatabaseUrl());
    assertEquals(
        envVars.get(AppConfiguration.SSM_PATH_DATABASE_USERNAME),
        testAppConfig.getDatabaseOptions().getDatabaseUsername());
    assertEquals(
        envVars.get(AppConfiguration.SSM_PATH_DATABASE_PASSWORD),
        testAppConfig.getDatabaseOptions().getDatabasePassword());
    assertThat(testAppConfig.createDataSourceFactory())
        .isExactlyInstanceOf(DefaultHikariDataSourceFactory.class);

    assertEquals(
        Integer.parseInt(envVars.get(AppConfiguration.SSM_PATH_LOADER_THREADS)),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getLoadOptions()
            .getBeneficiaryPerformanceSettings()
            .getLoaderThreads());
    assertEquals(
        envVars.get(AppConfiguration.SSM_PATH_IDEMPOTENCY_REQUIRED),
        "" + testAppConfig.getCcwRifLoadOptions().get().getLoadOptions().isIdempotencyRequired());
    assertEquals(
        S3ClientConfig.s3Builder()
            .endpointOverride(URI.create(envVars.get(AppConfiguration.ENV_VAR_AWS_ENDPOINT)))
            .accessKey(envVars.get(AppConfiguration.ENV_VAR_AWS_ACCESS_KEY))
            .secretKey(envVars.get(AppConfiguration.ENV_VAR_AWS_SECRET_KEY))
            .build(),
        testAppConfig.getCcwRifLoadOptions().get().getExtractionOptions().getS3ClientConfig());
  }

  /** Verify that RDS authentication settings are loaded as expected. */
  @Test
  void testRdsDatabaseAuthenticationSettings() {
    final var envVars = new HashMap<String, String>();
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_AUTH_TYPE, "RDS");
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_URL, "jdbc:postgres://host:1234/fhirdb");
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_USERNAME, "some_user");
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_PASSWORD, "not-used");
    envVars.put(AppConfiguration.SSM_PATH_DATABASE_MAX_POOL_SIZE, "14");
    addRequiredSettingsForTest(envVars);

    final var configLoader = AppConfiguration.createConfigLoaderForTesting(envVars);
    AppConfiguration testAppConfig = AppConfiguration.loadConfig(configLoader);

    assertEquals(
        DatabaseOptions.builder()
            .authenticationType(DatabaseOptions.AuthenticationType.RDS)
            .databaseUrl(envVars.get(AppConfiguration.SSM_PATH_DATABASE_URL))
            .databaseUsername(envVars.get(AppConfiguration.SSM_PATH_DATABASE_USERNAME))
            .databasePassword(envVars.get(AppConfiguration.SSM_PATH_DATABASE_PASSWORD))
            .maxPoolSize(14)
            .build(),
        testAppConfig.getDatabaseOptions());
    assertThat(testAppConfig.createDataSourceFactory())
        .isExactlyInstanceOf(RdsDataSourceFactory.class);
  }

  /** Verify that AWS settings are loaded as expected. */
  @Test
  void testAwsSettings() {
    final var envVars = new HashMap<String, String>();
    envVars.put(AppConfiguration.ENV_VAR_AWS_REGION, "us-west-1");
    envVars.put(AppConfiguration.ENV_VAR_AWS_ENDPOINT, "http://localhost:999999");
    envVars.put(AppConfiguration.ENV_VAR_AWS_ACCESS_KEY, "unreal-access-key");
    envVars.put(AppConfiguration.ENV_VAR_AWS_SECRET_KEY, "unreal-secret-key");
    addRequiredSettingsForTest(envVars);

    final var configLoader = AppConfiguration.createConfigLoaderForTesting(envVars);
    AppConfiguration testAppConfig = AppConfiguration.loadConfig(configLoader);

    assertEquals(
        AwsClientConfig.awsBuilder()
            .endpointOverride(URI.create(envVars.get(AppConfiguration.ENV_VAR_AWS_ENDPOINT)))
            .accessKey(envVars.get(AppConfiguration.ENV_VAR_AWS_ACCESS_KEY))
            .secretKey(envVars.get(AppConfiguration.ENV_VAR_AWS_SECRET_KEY))
            .region(Region.US_WEST_1)
            .build(),
        testAppConfig.getAwsClientConfig());
  }

  /**
   * Verifies that {@link AppConfiguration#loadBeneficiaryPerformanceSettings} enforces field
   * requirements and parses settings correctly.
   */
  @Test
  void testBeneficiaryPerformanceSettings() {
    final var envVars = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(envVars);

    assertThrows(ConfigException.class, () -> loadBeneficiaryPerformanceSettings(configLoader));

    // verify values must be positive
    envVars.put(AppConfiguration.SSM_PATH_LOADER_THREADS, "0");
    assertThatThrownBy(() -> loadBeneficiaryPerformanceSettings(configLoader))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(AppConfiguration.SSM_PATH_LOADER_THREADS)
        .hasMessageContaining(ConfigLoader.NOT_POSITIVE_INTEGER);
    envVars.put(AppConfiguration.SSM_PATH_LOADER_THREADS, "10");

    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_BATCH_SIZE, "-1");
    assertThatThrownBy(() -> loadBeneficiaryPerformanceSettings(configLoader))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(AppConfiguration.SSM_PATH_RIF_JOB_BATCH_SIZE)
        .hasMessageContaining(ConfigLoader.NOT_POSITIVE_INTEGER);
    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_BATCH_SIZE, "11");

    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE, "0");
    assertThatThrownBy(() -> loadBeneficiaryPerformanceSettings(configLoader))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(AppConfiguration.SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE)
        .hasMessageContaining(ConfigLoader.NOT_POSITIVE_INTEGER);
    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE, "12");

    // verify values are parsed correctly when present
    assertEquals(
        new LoadAppOptions.PerformanceSettings(10, 11, 12),
        loadBeneficiaryPerformanceSettings(configLoader));
  }

  /**
   * Verifies that {@link AppConfiguration#loadClaimPerformanceSettings} uses defaults as necessary
   * and parses settings correctly.
   */
  @Test
  void testClaimPerformanceSettings() {
    final var envVars = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(envVars);

    // verify defaults are used as expected
    final var benePerformanceSettings = new LoadAppOptions.PerformanceSettings(1, 2, 3);
    assertEquals(
        benePerformanceSettings,
        loadClaimPerformanceSettings(configLoader, benePerformanceSettings));

    // verify values must be positive
    envVars.put(AppConfiguration.SSM_PATH_LOADER_THREADS_CLAIMS, "0");
    assertThatThrownBy(() -> loadClaimPerformanceSettings(configLoader, benePerformanceSettings))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(AppConfiguration.SSM_PATH_LOADER_THREADS_CLAIMS)
        .hasMessageContaining(ConfigLoader.NOT_POSITIVE_INTEGER);
    envVars.put(AppConfiguration.SSM_PATH_LOADER_THREADS_CLAIMS, "20");

    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_BATCH_SIZE_CLAIMS, "-1");
    assertThatThrownBy(() -> loadClaimPerformanceSettings(configLoader, benePerformanceSettings))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(AppConfiguration.SSM_PATH_RIF_JOB_BATCH_SIZE_CLAIMS)
        .hasMessageContaining(ConfigLoader.NOT_POSITIVE_INTEGER);
    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_BATCH_SIZE_CLAIMS, "21");

    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE_CLAIMS, "0");
    assertThatThrownBy(() -> loadClaimPerformanceSettings(configLoader, benePerformanceSettings))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(AppConfiguration.SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE_CLAIMS)
        .hasMessageContaining(ConfigLoader.NOT_POSITIVE_INTEGER);
    envVars.put(AppConfiguration.SSM_PATH_RIF_JOB_QUEUE_SIZE_MULTIPLE_CLAIMS, "22");

    // verify values are parsed correctly when present
    assertEquals(
        new LoadAppOptions.PerformanceSettings(20, 21, 22),
        loadClaimPerformanceSettings(configLoader, benePerformanceSettings));
  }

  /**
   * Verifies that {@link AppConfiguration#loadConfig} fails as expected when it's called in an
   * application that hasn't had any of the configuration environment variables set.
   */
  @Test
  public void noEnvVarsSpecified() {
    final var configLoader = ConfigLoader.builder().build();
    assertThrows(ConfigException.class, () -> AppConfiguration.loadConfig(configLoader));
  }

  /** Verifies that micrometer settings are correctly extracted using environment variable names. */
  @Test
  public void testCloudWatchMicrometerConfigSettings() {
    final var envVars = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(envVars);
    final var helper = AppConfiguration.createMicrometerConfigHelper(configLoader);
    final CloudWatchConfig config = helper::get;
    assertEquals("cloudwatch", config.prefix());

    // confirm the defaults work as expected
    assertFalse(config.enabled());
    assertEquals(Duration.ofMinutes(1), config.step());
    assertThrows(ValidationException.class, () -> config.namespace());

    // confirm explicit values are parsed correctly
    envVars.put(AppConfiguration.SSM_PATH_MICROMETER_CW_ENABLED, "true");
    envVars.put(AppConfiguration.SSM_PATH_MICROMETER_CW_INTERVAL, "PT28S");
    envVars.put(AppConfiguration.SSM_PATH_MICROMETER_CW_NAMESPACE, "my-namespace");
    assertTrue(config.enabled());
    assertEquals(Duration.ofSeconds(28), config.step());
    assertEquals("my-namespace", config.namespace());
  }

  /**
   * Verify that {@link AbstractRdaLoadJob.Config} settings are loaded correctly. Includes checks
   * for default values of optional fields as well as ensuring strings are parsed correctly and
   * starting sequence numbers are forced to be positive.
   */
  @Test
  public void testLoadRdaLoadJobConfigOptions() {
    var settingsMap = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(settingsMap);
    settingsMap.put(SSM_PATH_RDA_JOB_INTERVAL_SECONDS, "42");
    settingsMap.put(SSM_PATH_RDA_JOB_BATCH_SIZE, "5");
    settingsMap.put(SSM_PATH_RDA_JOB_WRITE_THREADS, "11");

    // verify minimal required options load as expected and check defaults
    AbstractRdaLoadJob.Config jobConfig =
        AppConfiguration.loadRdaLoadJobConfigOptions(configLoader);
    assertEquals(Duration.ofSeconds(42), jobConfig.getRunInterval());
    assertEquals(5, jobConfig.getBatchSize());
    assertEquals(11, jobConfig.getWriteThreads());
    assertEquals(Optional.empty(), jobConfig.getStartingFissSeqNum());
    assertEquals(Optional.empty(), jobConfig.getStartingFissSeqNum());
    assertEquals(false, jobConfig.shouldProcessDLQ());
    assertEquals(
        RdaVersion.builder().versionString("^" + RdaService.RDA_PROTO_VERSION).build(),
        jobConfig.getRdaVersion());
    assertEquals(AbstractRdaLoadJob.SinkTypePreference.NONE, jobConfig.getSinkTypePreference());

    // verify providing an explicit RDA version string loads that version
    settingsMap.put(SSM_PATH_RDA_VERSION, "^1.2.3");
    jobConfig = AppConfiguration.loadRdaLoadJobConfigOptions(configLoader);
    assertEquals(RdaVersion.builder().versionString("^1.2.3").build(), jobConfig.getRdaVersion());

    // verify setting the starting sequence numbers to zero/negative yields 1 as a setting
    settingsMap.put(SSM_PATH_RDA_JOB_STARTING_FISS_SEQ_NUM, "0");
    settingsMap.put(SSM_PATH_RDA_JOB_STARTING_MCS_SEQ_NUM, "-10");
    jobConfig = AppConfiguration.loadRdaLoadJobConfigOptions(configLoader);
    assertEquals(Optional.of(1L), jobConfig.getStartingFissSeqNum());
    assertEquals(Optional.of(1L), jobConfig.getStartingMcsSeqNum());

    // verify setting the starting sequence numbers to positive number uses that number
    settingsMap.put(SSM_PATH_RDA_JOB_STARTING_FISS_SEQ_NUM, "2");
    settingsMap.put(SSM_PATH_RDA_JOB_STARTING_MCS_SEQ_NUM, "10");
    jobConfig = AppConfiguration.loadRdaLoadJobConfigOptions(configLoader);
    assertEquals(Optional.of(2L), jobConfig.getStartingFissSeqNum());
    assertEquals(Optional.of(10L), jobConfig.getStartingMcsSeqNum());

    // verify claims cleanup default settings
    assertFalse(jobConfig.shouldRunCleanup());
    assertEquals(0, jobConfig.getCleanupRunSize());
    assertEquals(0, jobConfig.getCleanupTransactionSize());

    // verify claims cleanup settings when values are present
    settingsMap.put(SSM_PATH_CLEANUP_ENABLED, "true");
    settingsMap.put(SSM_PATH_CLEANUP_RUN_SIZE, "100000");
    settingsMap.put(SSM_PATH_CLEANUP_TRANSACTION_SIZE, "5000");
    jobConfig = AppConfiguration.loadRdaLoadJobConfigOptions(configLoader);
    assertTrue(jobConfig.shouldRunCleanup());
    assertEquals(100000, jobConfig.getCleanupRunSize());
    assertEquals(5000, jobConfig.getCleanupTransactionSize());
  }

  /**
   * Verify that {@link RdaSourceConfig} settings are loaded correctly. Includes checks for default
   * values of optional fields as well as ensuring strings are parsed correctly.
   */
  @Test
  public void testLoadRdaSourceConfig() {
    var settingsMap = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(settingsMap);
    settingsMap.put(SSM_PATH_RDA_GRPC_SERVER_TYPE, RdaSourceConfig.ServerType.InProcess.name());
    settingsMap.put(SSM_PATH_RDA_GRPC_HOST, "host.test.com");
    settingsMap.put(SSM_PATH_RDA_GRPC_PORT, "450");
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_NAME, "rda-test-server");
    settingsMap.put(SSM_PATH_RDA_GRPC_MAX_IDLE_SECONDS, "180");
    settingsMap.put(SSM_PATH_RDA_GRPC_SECONDS_BEFORE_CONNECTION_DROP, "150");

    // verify minimal required options load as expected and check defaults
    RdaSourceConfig sourceConfig = AppConfiguration.loadRdaSourceConfig(configLoader);
    assertEquals(RdaSourceConfig.ServerType.InProcess, sourceConfig.getServerType());
    assertEquals("host.test.com", sourceConfig.getHost());
    assertEquals(450, sourceConfig.getPort());
    assertEquals("rda-test-server", sourceConfig.getInProcessServerName());
    assertEquals(Duration.ofSeconds(180), sourceConfig.getMaxIdle());
    assertEquals(Duration.ofSeconds(150), sourceConfig.getMinIdleTimeBeforeConnectionDrop());
    assertNull(sourceConfig.getAuthenticationToken());
    assertNull(sourceConfig.getExpirationDate());
    assertEquals(Optional.empty(), sourceConfig.getMessageErrorExpirationDays());

    // verify empty string token is ignored properly
    settingsMap.put(SSM_PATH_RDA_GRPC_AUTH_TOKEN, "");
    sourceConfig = AppConfiguration.loadRdaSourceConfig(configLoader);
    assertNull(sourceConfig.getAuthenticationToken());

    // verify token and expiration parse correctly
    long expiresMillis = Instant.now().plus(20, ChronoUnit.DAYS).getEpochSecond();
    String rawToken = String.format("{\"exp\":%d}", expiresMillis);
    String expiration = Base64.getEncoder().encodeToString(rawToken.getBytes());
    String token = String.format("NotAReal.%s.Token", expiration);
    settingsMap.put(SSM_PATH_RDA_GRPC_AUTH_TOKEN, token);
    settingsMap.put(SSM_PATH_RDA_JOB_ERROR_EXPIRE_DAYS, "42");
    sourceConfig = AppConfiguration.loadRdaSourceConfig(configLoader);
    assertEquals(token, sourceConfig.getAuthenticationToken());
    assertEquals(expiresMillis, sourceConfig.getExpirationDate());
    assertEquals(Optional.of(42), sourceConfig.getMessageErrorExpirationDays());
  }

  /**
   * Verify that {@link RdaServerJob.Config} settings are loaded correctly. Includes checks for
   * default values of optional fields as well as ensuring strings are parsed correctly. Tests calls
   * to a builder rather than checking the resulting config object because {@link
   * RdaServerJob.Config} is more complex than simply a few primitive field values.
   */
  @Test
  public void testLoadRdaServerJobConfig() {
    var settingsMap = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoaderForTesting(settingsMap);

    // set up the one value used in the RdaSourceConfig
    RdaSourceConfig sourceConfig = mock(RdaSourceConfig.class);
    doReturn("server-name").when(sourceConfig).getInProcessServerName();

    // verify minimal required options load as expected and check defaults
    RdaServerJob.Config.ConfigBuilder configBuilder = mock(RdaServerJob.Config.ConfigBuilder.class);
    S3ClientConfig expectedS3ClientConfig = S3ClientConfig.s3Builder().build();
    AppConfiguration.loadRdaServerJobConfig(configLoader, sourceConfig, configBuilder);
    verify(configBuilder).serverMode(RdaServerJob.Config.ServerMode.Random);
    verify(configBuilder).serverName("server-name");
    verify(configBuilder).s3ClientConfig(expectedS3ClientConfig);
    verify(configBuilder).build();
    verifyNoMoreInteractions(configBuilder);

    // verify all options set
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_MODE, RdaServerJob.Config.ServerMode.S3.name());
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_INTERVAL_SECONDS, "360");
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_SEED, "42");
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS, "17");
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_BUCKET, "my-bucket");
    settingsMap.put(SSM_PATH_RDA_GRPC_INPROC_SERVER_S3_DIRECTORY, "/my-directory");
    settingsMap.put(AppConfiguration.ENV_VAR_AWS_ENDPOINT, "http://localhost:999999");
    settingsMap.put(AppConfiguration.ENV_VAR_AWS_ACCESS_KEY, "unreal-access-key");
    settingsMap.put(AppConfiguration.ENV_VAR_AWS_SECRET_KEY, "unreal-secret-key");
    expectedS3ClientConfig =
        S3ClientConfig.s3Builder()
            .endpointOverride(URI.create("http://localhost:999999"))
            .accessKey("unreal-access-key")
            .secretKey("unreal-secret-key")
            .build();
    configBuilder = mock(RdaServerJob.Config.ConfigBuilder.class);
    AppConfiguration.loadRdaServerJobConfig(configLoader, sourceConfig, configBuilder);
    verify(configBuilder).serverMode(RdaServerJob.Config.ServerMode.S3);
    verify(configBuilder).serverName("server-name");
    verify(configBuilder).runInterval(Duration.ofSeconds(360));
    verify(configBuilder).randomSeed(42L);
    verify(configBuilder).randomMaxClaims(17);
    verify(configBuilder).s3ClientConfig(expectedS3ClientConfig);
    verify(configBuilder).s3Bucket("my-bucket");
    verify(configBuilder).s3Directory("/my-directory");
    verify(configBuilder).build();
    verifyNoMoreInteractions(configBuilder);
  }

  /**
   * Adds the settings that must be defined in order for {@link
   * AppConfiguration#loadConfig(ConfigLoader)} to succeed.
   *
   * @param envVars map to add the settings to
   */
  private void addRequiredSettingsForTest(Map<String, String> envVars) {
    envVars.putIfAbsent(
        AppConfiguration.SSM_PATH_HICN_HASH_ITERATIONS,
        String.valueOf(CcwRifLoadTestUtils.HICN_HASH_ITERATIONS));
    envVars.putIfAbsent(
        AppConfiguration.SSM_PATH_HICN_HASH_PEPPER,
        Hex.encodeHexString(CcwRifLoadTestUtils.HICN_HASH_PEPPER));
    envVars.putIfAbsent(AppConfiguration.SSM_PATH_LOADER_THREADS, "42");
    envVars.putIfAbsent(AppConfiguration.SSM_PATH_IDEMPOTENCY_REQUIRED, "true");
    envVars.putIfAbsent(AppConfiguration.SSM_PATH_BUCKET, "foo");
    envVars.putIfAbsent(AppConfiguration.SSM_PATH_DATABASE_URL, "jdbc:postgres://host:1234/fhirdb");
    envVars.putIfAbsent(AppConfiguration.SSM_PATH_DATABASE_USERNAME, "some_user");
    envVars.putIfAbsent(AppConfiguration.SSM_PATH_DATABASE_PASSWORD, "not-used");

    // if this one is absent the tests will do AWS credential checks!
    envVars.putIfAbsent(AppConfiguration.ENV_VAR_AWS_ENDPOINT, "http://localhost:999999");
  }
}
