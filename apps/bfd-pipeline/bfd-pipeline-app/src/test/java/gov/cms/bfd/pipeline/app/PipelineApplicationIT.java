package gov.cms.bfd.pipeline.app;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils.DataSourceComponents;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rda.grpc.RdaFissClaimLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaMcsClaimLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.server.ExceptionMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomFissClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RandomMcsClaimSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import gov.cms.bfd.pipeline.sharedutils.s3.S3MinioConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.apache.commons.codec.binary.Hex;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for {@link PipelineApplication}.
 *
 * <p>These tests require the application pipeline assembly to be built and available. Accordingly,
 * they may not run correctly in Eclipse: if the assembly isn't built yet, they'll just fail, but if
 * an older assembly exists (because you haven't rebuilt it), it'll run using the old code, which
 * probably isn't what you want.
 */
public final class PipelineApplicationIT {
  /** The POSIX signal number for the <code>SIGTERM</code> signal. */
  private static final int SIGTERM = 15;

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineApplicationIT.class);
  /**
   * Verifies that {@link PipelineApplication} exits as expected when launched with no configuration
   * environment variables.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void missingConfig() throws IOException, InterruptedException {
    // Start the app with no config env vars.
    ProcessBuilder appRunBuilder = createCcwRifAppProcessBuilder(new Bucket("foo"));
    appRunBuilder.environment().clear();
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();
    String text = new String(appProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    LOGGER.info("Debug PipelineAppIT  Tests Checkpoint 1: " + text);
    // Read the app's output.
    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();
    text = new String(appProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    LOGGER.info("Debug PipelineAppIT  Tests Checkpoint 2: " + text);
    // Wait for it to exit with an error.
    appProcess.waitFor(1, TimeUnit.MINUTES);
    appRunConsumerThread.join();
    // Verify that the application exited as expected.
    Assert.assertEquals(PipelineApplication.EXIT_CODE_BAD_CONFIG, appProcess.exitValue());
  }

  /**
   * Verifies that {@link PipelineApplication} works as expected when asked to run against an S3
   * bucket that doesn't exist. This test case isn't so much needed to test that one specific
   * failure case, but to instead verify that the application logs and keeps running as expected
   * when a job fails.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void missingBucket() throws IOException, InterruptedException {
    Process appProcess = null;
    try {
      // Start the app.
      ProcessBuilder appRunBuilder = createCcwRifAppProcessBuilder(new Bucket("foo"));
      appRunBuilder.redirectErrorStream(true);
      appProcess = appRunBuilder.start();

      // Read the app's output.
      ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
      Thread appRunConsumerThread = new Thread(appRunConsumer);
      appRunConsumerThread.start();

      // Wait for it to start scanning.
      Awaitility.await()
          .atMost(Duration.ONE_MINUTE)
          .until(() -> hasCcwRifLoadJobFailed(appRunConsumer));

      // Stop the application.
      sendSigterm(appProcess);
      appProcess.waitFor(1, TimeUnit.MINUTES);
      appRunConsumerThread.join();
    } finally {
      if (appProcess != null) appProcess.destroyForcibly();
    }
  }

  /**
   * Verifies that {@link PipelineApplication} works as expected when no data is made available for
   * it to process. Basically, it should just sit there and wait for data, doing nothing.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void noRifData() throws IOException, InterruptedException {
    skipOnUnsupportedOs();

    AmazonS3 s3Client = SharedS3Utilities.createS3Client(SharedS3Utilities.REGION_DEFAULT);

    Bucket bucket = null;
    Process appProcess = null;
    try {
      // Create the (empty) bucket to run against.
      bucket = DataSetTestUtilities.createTestBucket(s3Client);

      // Start the app.
      ProcessBuilder appRunBuilder = createCcwRifAppProcessBuilder(bucket);
      appRunBuilder.redirectErrorStream(true);
      appProcess = appRunBuilder.start();

      // Read the app's output.
      ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
      Thread appRunConsumerThread = new Thread(appRunConsumer);
      appRunConsumerThread.start();

      // Wait for it to start scanning.
      try {
        Awaitility.await()
            .atMost(Duration.ONE_MINUTE)
            .until(() -> hasCcwRifLoadJobCompleted(appRunConsumer));
      } catch (ConditionTimeoutException e) {
        throw new RuntimeException(
            "Pipeline application failed to start scanning within timeout, STDOUT:\n"
                + appRunConsumer.getStdoutContents(),
            e);
      }

      // Stop the application.
      sendSigterm(appProcess);
      appProcess.waitFor(1, TimeUnit.MINUTES);
      appRunConsumerThread.join();

      // Verify that the application exited as expected.
      verifyExitValueMatchesSignal(SIGTERM, appProcess);
    } finally {
      if (appProcess != null) appProcess.destroyForcibly();
      if (bucket != null) s3Client.deleteBucket(bucket.getName());
    }
  }

  /**
   * Verifies that {@link PipelineApplication} works as expected against a small amount of data. We
   * trust that other tests elsewhere are covering the ETL results' correctness; here we're just
   * verifying the overall flow. Does it find the data set, process it, and then not find a data set
   * anymore?
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void smallAmountOfRifData() throws IOException, InterruptedException {
    skipOnUnsupportedOs();

    AmazonS3 s3Client = SharedS3Utilities.createS3Client(SharedS3Utilities.REGION_DEFAULT);
    Bucket bucket = null;
    Process appProcess = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with a
       * data set.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(0),
              StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(1),
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Start the app.
      ProcessBuilder appRunBuilder = createCcwRifAppProcessBuilder(bucket);
      appRunBuilder.redirectErrorStream(true);
      appProcess = appRunBuilder.start();
      appProcess.getOutputStream().close();

      // Read the app's output.
      ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
      Thread appRunConsumerThread = new Thread(appRunConsumer);
      appRunConsumerThread.start();

      // Wait for it to process a data set.
      Awaitility.await()
          .atMost(Duration.ONE_MINUTE)
          .until(() -> hasADataSetBeenProcessed(appRunConsumer));

      // Stop the application.
      sendSigterm(appProcess);
      appProcess.waitFor(1, TimeUnit.MINUTES);
      appRunConsumerThread.join();

      // Verify that the application exited as expected.
      verifyExitValueMatchesSignal(SIGTERM, appProcess);
    } finally {
      if (appProcess != null) appProcess.destroyForcibly();
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  @Test
  public void rdaPipeline() throws Exception {
    skipOnUnsupportedOs();

    final AtomicReference<Process> appProcess = new AtomicReference<>();
    try {
      RdaServer.LocalConfig.builder()
          .fissSourceFactory(ignored -> new RandomFissClaimSource(12345, 100).toClaimChanges())
          .mcsSourceFactory(ignored -> new RandomMcsClaimSource(12345, 100).toClaimChanges())
          .build()
          .runWithPortParam(
              port -> {
                // Start the app.
                ProcessBuilder appRunBuilder = createRdaAppProcessBuilder(port);
                appRunBuilder.redirectErrorStream(true);
                appProcess.set(appRunBuilder.start());

                // Read the app's output.
                ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess.get());
                Thread appRunConsumerThread = new Thread(appRunConsumer);
                appRunConsumerThread.start();

                // Wait for it to start scanning.
                try {
                  Awaitility.await()
                      .atMost(Duration.ONE_MINUTE)
                      .until(
                          () ->
                              hasRdaFissLoadJobCompleted(appRunConsumer)
                                  && hasRdaMcsLoadJobCompleted(appRunConsumer));
                } catch (ConditionTimeoutException e) {
                  throw new RuntimeException(
                      "Pipeline application failed to start scanning within timeout, STDOUT:\n"
                          + appRunConsumer.getStdoutContents(),
                      e);
                }

                // Stop the application.
                sendSigterm(appProcess.get());
                appProcess.get().waitFor(1, TimeUnit.MINUTES);
                appRunConsumerThread.join();

                // Verify that the application exited as expected.
                verifyExitValueMatchesSignal(SIGTERM, appProcess.get());
              });
    } finally {
      if (appProcess.get() != null) appProcess.get().destroyForcibly();
    }
  }

  @Test
  public void rdaPipelineServerFailure() throws Exception {
    skipOnUnsupportedOs();

    final AtomicReference<Process> appProcess = new AtomicReference<>();
    try {
      RdaServer.LocalConfig.builder()
          .fissSourceFactory(
              ignored ->
                  new ExceptionMessageSource<>(
                      new RandomFissClaimSource(12345, 100).toClaimChanges(), 25, IOException::new))
          .mcsSourceFactory(
              ignored ->
                  new ExceptionMessageSource<>(
                      new RandomMcsClaimSource(12345, 100).toClaimChanges(), 25, IOException::new))
          .build()
          .runWithPortParam(
              port -> {
                // Start the app.
                ProcessBuilder appRunBuilder = createRdaAppProcessBuilder(port);
                appRunBuilder.redirectErrorStream(true);
                appProcess.set(appRunBuilder.start());

                // Read the app's output.
                ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess.get());
                Thread appRunConsumerThread = new Thread(appRunConsumer);
                appRunConsumerThread.start();

                // Wait for it to start scanning.
                try {
                  Awaitility.await()
                      .atMost(Duration.ONE_MINUTE)
                      .until(
                          () ->
                              hasRdaFissLoadJobCompleted(appRunConsumer)
                                  && hasRdaMcsLoadJobCompleted(appRunConsumer));
                } catch (ConditionTimeoutException e) {
                  throw new RuntimeException(
                      "Pipeline application failed to start scanning within timeout, STDOUT:\n"
                          + appRunConsumer.getStdoutContents(),
                      e);
                }

                // Stop the application.
                sendSigterm(appProcess.get());
                appProcess.get().waitFor(1, TimeUnit.MINUTES);
                appRunConsumerThread.join();

                // Verify that the application exited as expected.
                verifyExitValueMatchesSignal(SIGTERM, appProcess.get());
              });
    } finally {
      if (appProcess.get() != null) appProcess.get().destroyForcibly();
    }
  }

  /**
   * Throws an {@link AssumptionViolatedException} if the OS doesn't support
   * <strong>graceful</strong> shutdowns via {@link Process#destroy()}.
   */
  private static void skipOnUnsupportedOs() {
    /*
     * The only OS I know for sure that handles this correctly is Linux,
     * because I've verified that there. However, the following project
     * seems to indicate that Linux really might be it:
     * https://github.com/zeroturnaround/zt-process-killer. Some further
     * research indicates that this could be supported on Windows for GUI
     * apps, but not console apps. If this lack of OS support ever proves to
     * be a problem, the best thing to do would be to enhance our
     * application such that it listens on a particular port for shutdown
     * requests, and handles them gracefully.
     */

    Assume.assumeTrue(
        "Unsupported OS for this test case.",
        Arrays.asList("Linux", "Mac OS X").contains(System.getProperty("os.name")));
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that data set scanning has
   *     started, <code>false</code> if not
   */
  private static boolean hasCcwRifLoadJobCompleted(ProcessOutputConsumer appRunConsumer) {
    return hasJobRecordMatching(
        appRunConsumer,
        PipelineJobRecordStore.LOG_MESSAGE_PREFIX_JOB_COMPLETED,
        CcwRifLoadJob.class);
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that data set scanning has
   *     started, <code>false</code> if not
   */
  private static boolean hasRdaFissLoadJobCompleted(ProcessOutputConsumer appRunConsumer) {
    return hasJobRecordMatching(
        appRunConsumer,
        PipelineJobRecordStore.LOG_MESSAGE_PREFIX_JOB_COMPLETED,
        RdaFissClaimLoadJob.class);
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that data set scanning has
   *     started, <code>false</code> if not
   */
  private static boolean hasRdaMcsLoadJobCompleted(ProcessOutputConsumer appRunConsumer) {
    return hasJobRecordMatching(
        appRunConsumer,
        PipelineJobRecordStore.LOG_MESSAGE_PREFIX_JOB_COMPLETED,
        RdaMcsClaimLoadJob.class);
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that the {@link CcwRifLoadJob}
   *     failed, <code>false</code> if not
   */
  private static boolean hasCcwRifLoadJobFailed(ProcessOutputConsumer appRunConsumer) {
    return hasJobRecordMatching(
        appRunConsumer, PipelineJobRecordStore.LOG_MESSAGE_PREFIX_JOB_FAILED, CcwRifLoadJob.class);
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that data set scanning has
   *     started, <code>false</code> if not
   */
  private static boolean hasRdaFissLoadJobFailed(ProcessOutputConsumer appRunConsumer) {
    return hasJobRecordMatching(
        appRunConsumer,
        PipelineJobRecordStore.LOG_MESSAGE_PREFIX_JOB_FAILED,
        RdaFissClaimLoadJob.class);
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that data set scanning has
   *     started, <code>false</code> if not
   */
  private static boolean hasRdaMcsLoadJobFailed(ProcessOutputConsumer appRunConsumer) {
    return hasJobRecordMatching(
        appRunConsumer,
        PipelineJobRecordStore.LOG_MESSAGE_PREFIX_JOB_FAILED,
        RdaMcsClaimLoadJob.class);
  }

  private static boolean hasJobRecordMatching(
      ProcessOutputConsumer appRunConsumer, String prefix, Class<?> klass) {
    return appRunConsumer.matches(
        line -> line.contains(prefix) && line.contains(klass.getSimpleName()));
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that a data set has been
   *     processed, <code>false</code> if not
   */
  private static boolean hasADataSetBeenProcessed(ProcessOutputConsumer appRunConsumer) {
    return appRunConsumer.matches(
        line -> line.contains(CcwRifLoadJob.LOG_MESSAGE_DATA_SET_COMPLETE));
  }

  /**
   * Sends a <code>SIGTERM</code> to the specified {@link Process}, causing it to exit, but giving
   * it a chance to do so gracefully.
   *
   * @param process the {@link Process} to signal
   */
  private static void sendSigterm(Process process) {
    /*
     * We have to use reflection and external commands here to work around
     * this ridiculous JDK bug:
     * https://bugs.openjdk.java.net/browse/JDK-5101298.
     */
    if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
      try {
        Field pidField = process.getClass().getDeclaredField("pid");
        pidField.setAccessible(true);

        int processPid = pidField.getInt(process);

        ProcessBuilder killBuilder =
            new ProcessBuilder("/bin/kill", "--signal", "TERM", "" + processPid);
        int killBuilderExitCode = killBuilder.start().waitFor();
        if (killBuilderExitCode != 0) process.destroy();
      } catch (NoSuchFieldException
          | SecurityException
          | IllegalArgumentException
          | IllegalAccessException
          | InterruptedException
          | IOException e) {
        process.destroy();
        throw new RuntimeException(e);
      }
    } else {
      /*
       * Not sure if this bug exists on Windows or not (may cause test
       * cases to fail, if it does, because we wouldn't be able to read
       * all of the processes' output after they're stopped). If it does,
       * we could follow up on the ideas here to add a similar
       * platform-specific workaround:
       * https://stackoverflow.com/questions/140111/sending-an-arbitrary-
       * signal-in-windows.
       */
      process.destroy();
    }
  }

  /**
   * Verifies that the specified {@link Process} has exited, due to the specified signal.
   *
   * @param signalNumber the POSIX signal number to check for
   * @param process the {@link Process} to check the {@link Process#exitValue()} of
   */
  private static void verifyExitValueMatchesSignal(int signalNumber, Process process) {
    /*
     * Per POSIX (by way of http://unix.stackexchange.com/a/99143),
     * applications that exit due to a signal should return an exit code
     * that is 128 + the signal number.
     */
    Assert.assertEquals(128 + signalNumber, process.exitValue());
  }

  /**
   * Creates a ProcessBuilder with the common settings used by CCW/RIF and RDA tests.
   *
   * @return ProcessBuilder ready for more env vars to be added
   */
  private static ProcessBuilder createAppProcessBuilder() {
    String[] command = createCommandForPipelineApp();
    ProcessBuilder appRunBuilder = new ProcessBuilder(command);
    appRunBuilder.redirectErrorStream(true);

    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_HICN_HASH_ITERATIONS,
            String.valueOf(CcwRifLoadTestUtils.HICN_HASH_ITERATIONS));
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_HICN_HASH_PEPPER,
            Hex.encodeHexString(CcwRifLoadTestUtils.HICN_HASH_PEPPER));
    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, dataSourceComponents.getUrl());
    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, dataSourceComponents.getUsername());
    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_LOADER_THREADS,
            String.valueOf(LoadAppOptions.DEFAULT_LOADER_THREADS));
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_IDEMPOTENCY_REQUIRED,
            String.valueOf(CcwRifLoadTestUtils.IDEMPOTENCY_REQUIRED));
    appRunBuilder.environment().put("JAVA_HOME", System.getenv("JAVA_HOME"));
    /*
     * Note: Not explicitly providing AWS credentials here, as the child
     * process will inherit any that are present in this build/test process.
     */
    return appRunBuilder;
  }

  /**
   * Creates a ProcessBuilder configured for an CCS/RIF pipeline test.
   *
   * @param bucket the S3 {@link Bucket} that the application will be configured to pull RIF data
   *     from
   * @return a {@link ProcessBuilder} that can be used to launch the application
   */
  private static ProcessBuilder createCcwRifAppProcessBuilder(Bucket bucket) {
    ProcessBuilder appRunBuilder = createAppProcessBuilder();

    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_BUCKET, bucket.getName());

    return appRunBuilder;
  }

  /**
   * Creates a ProcessBuilder configured for an RDA pipeline test.
   *
   * @param port the TCP/IP port that the RDA mock server is listening on
   * @return a {@link ProcessBuilder} that can be used to launch the application
   */
  private static ProcessBuilder createRdaAppProcessBuilder(int port) {
    ProcessBuilder appRunBuilder = createAppProcessBuilder();

    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_CCW_RIF_JOB_ENABLED, "false");
    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_RDA_JOB_ENABLED, "true");
    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_RDA_JOB_BATCH_SIZE, "10");
    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_RDA_GRPC_PORT, String.valueOf(port));

    return appRunBuilder;
  }

  /**
   * @return the command array for {@link ProcessBuilder#ProcessBuilder(String...)} that will launch
   *     the application via its <code>.x</code> assembly executable script
   */
  private static String[] createCommandForPipelineApp() {
    try {
      Path assemblyDirectory =
          Files.list(Paths.get(".", "target", "pipeline-app"))
              .filter(f -> f.getFileName().toString().startsWith("bfd-pipeline-app-"))
              .findFirst()
              .get();
      Path pipelineAppScript = assemblyDirectory.resolve("bfd-pipeline-app.sh");

      S3MinioConfig minioConfig = S3MinioConfig.Singleton();
      if (minioConfig.useMinio) {
        return new String[] {
          pipelineAppScript.toAbsolutePath().toString(),
          "-Ds3.local=true",
          String.format("-Ds3.localUser=%s", minioConfig.minioUserName),
          String.format("-Ds3.localPass=%s", minioConfig.minioPassword),
          String.format("-Ds3.localAddress=%s", minioConfig.minioEndpointAddress)
        };
      }
      return new String[] {pipelineAppScript.toAbsolutePath().toString()};
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Managing external processes is tricky: at the OS level, all processes' output is sent to a
   * buffer. If that buffer fills up (because you're not reading the output), the process will block
   * -- forever. To avoid that, it's best to always have a separate thread running that consumes a
   * process' output. This {@link ProcessOutputConsumer} is designed to allow for just that.
   */
  private static final class ProcessOutputConsumer implements Runnable {
    private final BufferedReader stdoutReader;
    private final List<String> stdoutContents;

    /**
     * Constructs a new {@link ProcessOutputConsumer} instance.
     *
     * @param the {@link ProcessOutputConsumer} whose output should be consumed
     */
    public ProcessOutputConsumer(Process process) {
      /*
       * Note: we're only grabbing STDOUT, because we're assuming that
       * STDERR has been piped to/merged with it. If that's not the case,
       * you'd need a separate thread consuming that stream, too.
       */

      InputStream stdout = process.getInputStream();
      this.stdoutReader = new BufferedReader(new InputStreamReader(stdout));
      this.stdoutContents = new ArrayList<>();
    }

    /** @see java.lang.Runnable#run() */
    @Override
    public void run() {
      /*
       * Note: This will naturally stop once the process exits (due to the
       * null check below).
       */

      try {
        String line;
        while ((line = stdoutReader.readLine()) != null) {
          addLine(line);
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new UncheckedIOException(e);
      }
    }

    /** @return a {@link String} that contains the <code>STDOUT</code> contents so far */
    public synchronized String getStdoutContents() {
      return String.join("\n", stdoutContents);
    }

    /**
     * Matches every line in the current <code>STDOUT</code> contents looking for one that matches
     * the given predicate. This has to be synchronized to avoid potential
     * ConcurrentModificationExceptions.
     *
     * @param predicate used to test each line of the output
     * @return true if any line matches the predicate
     */
    public synchronized boolean matches(Predicate<String> predicate) {
      return stdoutContents.stream().anyMatch(predicate);
    }

    /**
     * Used internally to add a line of output to the stdoutContents with proper synchronization.
     *
     * @param line text to add to the output
     */
    private synchronized void addLine(String line) {
      stdoutContents.add(line);
    }
  }
}
