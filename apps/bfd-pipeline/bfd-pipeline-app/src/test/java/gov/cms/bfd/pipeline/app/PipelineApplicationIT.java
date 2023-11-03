package gov.cms.bfd.pipeline.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.DataSourceComponents;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.FileBasedAssertionHelper;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.AbstractLocalStackS3Test;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaFissClaimLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaMcsClaimLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomClaimGeneratorConfig;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Integration tests for {@link PipelineApplication}.
 *
 * <p>These tests require the application pipeline assembly to be built and available. Accordingly,
 * they may not run correctly in Eclipse: if the assembly isn't built yet, they'll just fail, but if
 * an older assembly exists (because you haven't rebuilt it), it'll run using the old code, which
 * probably isn't what you want.
 */
public final class PipelineApplicationIT extends AbstractLocalStackS3Test {
  /**
   * Name of log file that will contain log output from the app. This has to match the value in our
   * {@code logback-test.xml} file.
   */
  private static final String LOG_FILE_PATH = "target/pipelineOutput.log";

  /**
   * Controls access to the log file so that multiple tests do not try to write to it at the same
   * time.
   */
  private static final FileBasedAssertionHelper LOG_FILE =
      new FileBasedAssertionHelper(Path.of(LOG_FILE_PATH));

  /**
   * Locks and truncates the log file so that each test case can read only its own messages from the
   * file when making assertions about log output.
   *
   * @throws Exception unable to lock or truncate the file
   */
  @BeforeEach
  public void lockLogFile() throws Exception {
    LOG_FILE.beginTest(true, 300);
  }

  /** Unlocks the log file. */
  @AfterEach
  public void releaseLogFile() {
    LOG_FILE.endTest();
  }

  /**
   * Verifies that {@link PipelineApplication} exits as expected when launched with no configuration
   * environment variables.
   */
  @Test
  public void missingConfig() {
    // Configure the app with no config env vars.
    ConfigLoader configLoader = ConfigLoader.builder().build();
    PipelineApplication app = createApplicationForTest(configLoader);

    // Run the app and verify it exited as expected
    final int exitCode = app.runPipelineAndHandleExceptions();
    assertEquals(PipelineApplication.EXIT_CODE_BAD_CONFIG, exitCode);
  }

  /**
   * Verifies that {@link PipelineApplication} works as expected when asked to run against an S3
   * bucket that doesn't exist. This test case isn't so much needed to test that one specific
   * failure case, but to instead verify that the application logs and keeps running as expected
   * when a job fails.
   */
  @Test
  public void missingBucket() {
    // Configure the app with the bad bucket name.
    ConfigLoader configLoader = createCcwRifJobConfig("foo");
    PipelineApplication app = createApplicationForTest(configLoader);

    // Run the app and collect its output.
    final int exitCode = app.runPipelineAndHandleExceptions();
    final List<String> logLines = LOG_FILE.readFileAsIndividualLines();

    // Verify the results match expectations
    assertEquals(PipelineApplication.EXIT_CODE_JOB_FAILED, exitCode);
    assertCcwRifLoadJobFailed(logLines);
  }

  /**
   * Verifies that {@link PipelineApplication} works as expected when no data is made available for
   * it to process. Basically, it should just sit there and wait for data, doing nothing.
   */
  @Test
  public void noRifData() {
    String bucket = null;
    try {
      // Create the (empty) bucket to run against.
      bucket = s3Dao.createTestBucket();

      // Configure the app with the temporary bucket name.
      ConfigLoader configLoader = createCcwRifJobConfig(bucket);
      PipelineApplication app = createApplicationForTest(configLoader);

      // Run the app and collect its output.
      final int exitCode = app.runPipelineAndHandleExceptions();
      final List<String> logLines = LOG_FILE.readFileAsIndividualLines();

      // Verify the results match expectations
      assertEquals(PipelineApplication.EXIT_CODE_SUCCESS, exitCode);
      assertCcwRifLoadJobCompleted(logLines);
    } finally {
      if (StringUtils.isNotBlank(bucket)) {
        s3Dao.deleteTestBucket(bucket);
      }
    }
  }

  /**
   * Verifies that {@link PipelineApplication} works as expected against a small amount of data. We
   * trust that other tests elsewhere are covering the ETL results' correctness; here we're just
   * verifying the overall flow. Does it find the data set, process it, and then not find a data set
   * anymore?
   */
  @Test
  public void smallAmountOfRifData() {
    String bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with a
       * data set.
       */
      bucket = s3Dao.createTestBucket();
      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              false,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      DataSetTestUtilities.putObject(s3Dao, bucket, manifest);
      DataSetTestUtilities.putObject(
          s3Dao,
          bucket,
          manifest,
          manifest.getEntries().get(0),
          StaticRifResource.SAMPLE_A_BENES.getResourceUrl());
      DataSetTestUtilities.putObject(
          s3Dao,
          bucket,
          manifest,
          manifest.getEntries().get(1),
          StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl());

      // Configure the app with the temporary bucket name.
      ConfigLoader configLoader = createCcwRifJobConfig(bucket);
      PipelineApplication app = createApplicationForTest(configLoader);

      // Run the app and collect its output.
      final int exitCode = app.runPipelineAndHandleExceptions();
      final List<String> logLines = LOG_FILE.readFileAsIndividualLines();

      // Verify the results match expectations
      assertEquals(PipelineApplication.EXIT_CODE_SUCCESS, exitCode);
      assertCcwRifLoadJobCompleted(logLines);
      assertADataSetBeenProcessed(logLines);

      // Verify all files were moved into the done "folder".
      final String keyPrefix =
          DataSetTestUtilities.keyPrefixForManifest(
                  CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS, manifest)
              + "/";
      final Set<String> completedFiles =
          s3Dao
              .listObjects(bucket, keyPrefix)
              .map(S3Dao.S3ObjectSummary::getKey)
              .map(key -> key.substring(keyPrefix.length()))
              .collect(ImmutableSet.toImmutableSet());
      assertEquals(Set.of("0_manifest.xml", "beneficiaries.rif", "carrier.rif"), completedFiles);
    } finally {
      if (StringUtils.isNotBlank(bucket)) {
        s3Dao.deleteTestBucket(bucket);
      }
    }
  }

  /**
   * Verifies the RDA pipeline can be configured, started, and shut down successfully.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void rdaPipeline() throws Exception {
    final var randomClaimConfig =
        RandomClaimGeneratorConfig.builder().seed(12345).maxToSend(30).build();
    final var serviceConfig =
        RdaMessageSourceFactory.Config.builder().randomClaimConfig(randomClaimConfig).build();
    RdaServer.LocalConfig.builder()
        .serviceConfig(serviceConfig)
        .build()
        .runWithPortParam(
            port -> {
              // Configure the app with the server port.
              ConfigLoader configLoader = createRdaJobConfig(port);
              PipelineApplication app = createApplicationForTest(configLoader);

              // Run the app and collect its output.
              final int exitCode = app.runPipelineAndHandleExceptions();
              final List<String> logLines = LOG_FILE.readFileAsIndividualLines();

              // Verify the results match expectations
              assertEquals(PipelineApplication.EXIT_CODE_SUCCESS, exitCode);
              assertRdaFissLoadJobCompleted(logLines);
              assertRdaMcsLoadJobCompleted(logLines);
              assertJobRecordMatching(
                  logLines,
                  line -> line.contains("processed 30 objects in"),
                  RdaFissClaimLoadJob.class,
                  "FISS job processed all claims");
              assertJobRecordMatching(
                  logLines,
                  line -> line.contains("processed 30 objects in"),
                  RdaMcsClaimLoadJob.class,
                  "MCS job processed all claims");
            });
  }

  /**
   * Verifies that when there is an exception while running the RDA jobs they complete normally
   * after logging their exception.
   *
   * @throws Exception indicates a test failure
   */
  @Test
  public void rdaPipelineServerFailure() throws Exception {
    final var randomClaimConfig =
        RandomClaimGeneratorConfig.builder().seed(12345).maxToSend(100).build();
    final var serviceConfig =
        RdaMessageSourceFactory.Config.builder()
            .randomClaimConfig(randomClaimConfig)
            .throwExceptionAfterCount(25)
            .build();
    RdaServer.LocalConfig.builder()
        .serviceConfig(serviceConfig)
        .build()
        .runWithPortParam(
            port -> {
              // Configure the app with the server port.
              ConfigLoader configLoader = createRdaJobConfig(port);
              PipelineApplication app = createApplicationForTest(configLoader);

              // Run the app and collect its output.
              final int exitCode = app.runPipelineAndHandleExceptions();
              final List<String> logLines = LOG_FILE.readFileAsIndividualLines();

              // Verify the results match expectations
              assertEquals(PipelineApplication.EXIT_CODE_SUCCESS, exitCode);
              assertRdaFissLoadJobCompleted(logLines);
              assertRdaMcsLoadJobCompleted(logLines);
              assertJobRecordMatching(
                  logLines,
                  line -> line.contains("StatusRuntimeException"),
                  RdaFissClaimLoadJob.class,
                  "FISS job terminated by grpc exception");
              assertJobRecordMatching(
                  logLines,
                  line -> line.contains("StatusRuntimeException"),
                  RdaMcsClaimLoadJob.class,
                  "MCS job terminated by grpc exception");
            });
  }

  /**
   * Verifies that when a failing smoke test causes the application to terminate with appropriate
   * exist code and log message.
   *
   * @throws Exception indicates a test failure
   */
  @Test
  public void smokeTestFailure() throws Exception {
    // Create a mock job that has a guaranteed smoke test failure.
    PipelineJob smokeTestFailureJob = mock(PipelineJob.class);
    doReturn(true).when(smokeTestFailureJob).isInterruptible();
    doReturn(false).when(smokeTestFailureJob).isSmokeTestSuccessful();

    String bucket = null;
    try {
      // Create the (empty) bucket to run against.
      bucket = s3Dao.createTestBucket();

      // Configure the app with the temporary bucket name.
      ConfigLoader configLoader = createCcwRifJobConfig(bucket);
      PipelineApplication app = createApplicationForTest(configLoader);

      // Override normal job creation to ensure our mock job is created instead of real one.
      doReturn(List.of(smokeTestFailureJob)).when(app).createAllJobs(any(), any(), any(), any());

      // Run the app and collect its output.
      final int exitCode = app.runPipelineAndHandleExceptions();
      final List<String> logLines = LOG_FILE.readFileAsIndividualLines();

      // Verify the results match expectations
      assertEquals(PipelineApplication.EXIT_CODE_SMOKE_TEST_FAILURE, exitCode);
      assertASmokeTestFailureWasLogged(logLines);
    } finally {
      if (StringUtils.isNotBlank(bucket)) {
        s3Dao.deleteTestBucket(bucket);
      }
    }
  }

  /**
   * Verifies that the CCW RIF load job has completed by checking the job records.
   *
   * @param logLines list containing every line in the log file
   */
  private static void assertCcwRifLoadJobCompleted(List<String> logLines) {
    assertJobRecordMatching(
        logLines,
        PipelineJobRunner.JobRunSummary::isSuccessString,
        CcwRifLoadJob.class,
        "CCW/RIF job completed");
  }

  /**
   * Verifies that the RDA Fiss load job has completed by checking the job records.
   *
   * @param logLines list containing every line in the log file
   */
  private static void assertRdaFissLoadJobCompleted(List<String> logLines) {
    assertJobRecordMatching(
        logLines,
        PipelineJobRunner.JobRunSummary::isSuccessString,
        RdaFissClaimLoadJob.class,
        "RDA/FISS job successful");
  }

  /**
   * Verifies that the RDA MCS load job has completed by checking the job records.
   *
   * @param logLines list containing every line in the log file
   */
  private static void assertRdaMcsLoadJobCompleted(List<String> logLines) {
    assertJobRecordMatching(
        logLines,
        PipelineJobRunner.JobRunSummary::isSuccessString,
        RdaMcsClaimLoadJob.class,
        "RDA/MCS job successful");
  }

  /**
   * Verifies that the CCW RIF load job has failed by checking the job records.
   *
   * @param logLines list containing every line in the log file
   */
  private static void assertCcwRifLoadJobFailed(List<String> logLines) {
    assertJobRecordMatching(
        logLines,
        PipelineJobRunner.JobRunSummary::isFailureString,
        CcwRifLoadJob.class,
        "CCW/RIF job failed");
  }

  /**
   * Verifies that a job has a job record matching a specified predicate.
   *
   * @param logLines list containing every line in the log file
   * @param matcher {@link Predicate} used to find a target string
   * @param klass used to verify a target string contains the class name
   * @param description describes the assertion when reporting a failure
   */
  private static void assertJobRecordMatching(
      List<String> logLines, Predicate<String> matcher, Class<?> klass, String description) {
    assertThat(logLines)
        .describedAs(description)
        .anyMatch(line -> matcher.test(line) && line.contains(klass.getSimpleName()));
  }

  /**
   * Verifies that a data set has been processed by the specified job by checking for a specific log
   * message.
   *
   * @param logLines list containing every line in the log file
   */
  private static void assertADataSetBeenProcessed(List<String> logLines) {
    assertThat(logLines)
        .describedAs("All data sets processed")
        .anyMatch(line -> line.contains(CcwRifLoadJob.LOG_MESSAGE_DATA_SET_COMPLETE));
  }

  /**
   * Verifies that a smoke test failure has been logged.
   *
   * @param logLines list containing every line in the log file
   */
  private static void assertASmokeTestFailureWasLogged(List<String> logLines) {
    assertJobRecordMatching(
        logLines,
        line -> line.contains("Pipeline terminating due to smoke test failure"),
        PipelineApplication.class,
        "Smoke test failure.");
  }

  /**
   * Adds environment variables for the common settings used by CCW/RIF and RDA tests.
   *
   * @param environment map to hold the variables
   */
  private void addCommonAppSettings(Map<String, String> environment) {
    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    environment.put(
        AppConfiguration.ENV_VAR_KEY_HICN_HASH_ITERATIONS,
        String.valueOf(CcwRifLoadTestUtils.HICN_HASH_ITERATIONS));
    environment.put(
        AppConfiguration.ENV_VAR_KEY_HICN_HASH_PEPPER,
        Hex.encodeHexString(CcwRifLoadTestUtils.HICN_HASH_PEPPER));
    environment.put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, dataSourceComponents.getUrl());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, dataSourceComponents.getUsername());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_LOADER_THREADS,
        String.valueOf(LoadAppOptions.DEFAULT_LOADER_THREADS));
    environment.put(
        AppConfiguration.ENV_VAR_KEY_IDEMPOTENCY_REQUIRED,
        String.valueOf(CcwRifLoadTestUtils.IDEMPOTENCY_REQUIRED));

    environment.put(AppConfiguration.ENV_VAR_KEY_AWS_ENDPOINT, localstack.getEndpoint().toString());
    environment.put(AppConfiguration.ENV_VAR_KEY_AWS_ACCESS_KEY, localstack.getAccessKey());
    environment.put(AppConfiguration.ENV_VAR_KEY_AWS_SECRET_KEY, localstack.getSecretKey());
  }

  /**
   * Creates the {@link ConfigLoader} used to configure the app for a CCW/RIF pipeline test.
   *
   * @param bucket the name of the S3 bucket that the application will be configured to pull RIF
   *     data from
   * @return config with common values set
   */
  private ConfigLoader createCcwRifJobConfig(String bucket) {
    final var environment = new HashMap<String, String>();
    addCommonAppSettings(environment);

    environment.put(AppConfiguration.ENV_VAR_KEY_RDA_JOB_ENABLED, "false");
    environment.put(AppConfiguration.ENV_VAR_KEY_CCW_RIF_JOB_ENABLED, "true");
    environment.put(AppConfiguration.ENV_VAR_KEY_BUCKET, bucket);
    environment.put(
        AppConfiguration.ENV_VAR_KEY_RIF_FILTERING_NON_NULL_AND_NON_2023_BENES,
        Boolean.FALSE.toString());

    // ensure the job runs only once so the app doesn't loop forever
    environment.put(AppConfiguration.ENV_VAR_KEY_CCW_RIF_JOB_INTERVAL_SECONDS, "0");

    return AppConfiguration.createConfigLoaderForTesting(environment);
  }

  /**
   * Creates the {@link ConfigLoader} used to configure the app for an RDA pipeline test.
   *
   * @param port the TCP/IP port that the RDA mock server is listening on
   * @return config with common values set
   */
  private ConfigLoader createRdaJobConfig(int port) {
    final var environment = new HashMap<String, String>();
    addCommonAppSettings(environment);

    environment.put(AppConfiguration.ENV_VAR_KEY_CCW_RIF_JOB_ENABLED, "false");
    environment.put(AppConfiguration.ENV_VAR_KEY_RDA_JOB_ENABLED, "true");
    environment.put(AppConfiguration.ENV_VAR_KEY_RDA_JOB_BATCH_SIZE, "10");
    environment.put(AppConfiguration.ENV_VAR_KEY_RDA_GRPC_PORT, String.valueOf(port));

    // ensure the job runs only once so the app doesn't loop forever
    environment.put(AppConfiguration.ENV_VAR_KEY_RDA_JOB_INTERVAL_SECONDS, "0");

    return AppConfiguration.createConfigLoaderForTesting(environment);
  }

  /**
   * Creates a Mockito spy of a {@link PipelineApplication} and configures it to use the provided
   * {@link ConfigLoader}.
   *
   * @param configLoader the desired configuration
   * @return the application object
   */
  private PipelineApplication createApplicationForTest(ConfigLoader configLoader) {
    // using a spy lets us override and verify method calls
    PipelineApplication app = spy(new PipelineApplication());

    // override the default app logic with our own config
    doReturn(configLoader).when(app).createConfigLoader();

    // we don't actually want to register a shutdown hook
    doNothing().when(app).registerShutdownHook(any(), any());
    return app;
  }
}
