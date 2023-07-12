package gov.cms.bfd.migrator.app;

import static gov.cms.bfd.migrator.app.AppConfiguration.ENV_VAR_KEY_SQS_ACCESS_KEY;
import static gov.cms.bfd.migrator.app.AppConfiguration.ENV_VAR_KEY_SQS_ENDPOINT;
import static gov.cms.bfd.migrator.app.AppConfiguration.ENV_VAR_KEY_SQS_QUEUE_NAME;
import static gov.cms.bfd.migrator.app.AppConfiguration.ENV_VAR_KEY_SQS_REGION;
import static gov.cms.bfd.migrator.app.AppConfiguration.ENV_VAR_KEY_SQS_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.DataSourceComponents;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.ProcessOutputConsumer;
import gov.cms.bfd.TestContainerConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.sql.DataSource;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Tests the migrator app under various conditions to ensure it works correctly. */
@Testcontainers
public final class MigratorAppIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigratorApp.class);

  /** Name of SQS queue created in localstack to receive progress messages via SQS. */
  private static final String SQS_QUEUE_NAME = "migrator-progress";

  /** Automatically creates and destroys a localstack SQS service container. */
  @Container
  LocalStackContainer localstack =
      new LocalStackContainer(TestContainerConstants.LocalStackImageName)
          .withServices(LocalStackContainer.Service.SQS);

  /** Used to communicate with the localstack SQS service. */
  private SqsDao sqsDao;

  /** Enum for determining which flyway script directory to run a test against. */
  private enum TestDirectory {
    /** Value that will not override the flyway script location, and use the real flyway scripts. */
    REAL(""),
    /** Value that will use the flyway path for testing bad sql. */
    BAD_SQL("/bad-sql"),
    /** Value that will use the flyway path for a duplicate version number. * */
    DUPLICATE_VERSION("/duplicate-version"),
    /** Value that will use the flyway path for a validation failure. * */
    VALIDATION_FAILURE("/validation-failure");

    /** The flyway path to use. */
    private final String path;

    /**
     * Instantiates a new Test directory.
     *
     * @param path the path to the test files for this selection
     */
    TestDirectory(String path) {
      this.path = path;
    }

    /**
     * Gets the path for this selection.
     *
     * @return the path override for the test files
     */
    public String getPath() {
      return path;
    }
  }

  /** Cleans up the database before each test. */
  @BeforeEach
  public void setup() {
    DatabaseTestUtils.get().dropSchemaForDataSource();
  }

  /** Cleans up the schema after the test. */
  @AfterAll
  public static void teardown() {
    DatabaseTestUtils.get().dropSchemaForDataSource();
  }

  /** Create our progress queue in the localstack SQS service before each test. */
  @BeforeEach
  void createQueue() {
    sqsDao = new SqsDao(SqsDaoIT.createSqsClientForLocalStack(localstack));
    sqsDao.createQueue(SQS_QUEUE_NAME);
  }

  /**
   * Test when the migration app runs and there are no errors with the scripts, the exit code is 0.
   *
   * @throws IOException if there is an issue starting the app
   */
  @Test
  void testMigrationRunWhenNoErrorsAndAllFilesRunExpectExitCodeZeroAndSchemaMigrationsRun()
      throws IOException {
    // Setup
    ProcessBuilder appRunBuilder = createAppProcessBuilder(TestDirectory.REAL);
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    int numMigrations = getNumMigrationScripts();
    /* Normally the version would be the same as the number of files, but
    migration #12 is missing for some reason in the real scripts, so the version will be files +1 */
    int expectedVersion = numMigrations + 1;

    // Await start/finish of application
    try {
      Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> !appProcess.isAlive());

      // Verify results
      assertEquals(
          0,
          appProcess.exitValue(),
          "Did not get expected exit code, \nSTDOUT:\n" + appRunConsumer.getStdoutContents());

      // Test the migrations occurred by checking the log output
      boolean hasExpectedMigrationLine =
          hasLogLine(
              appRunConsumer,
              String.format("Successfully applied %s migrations to schema", numMigrations));

      assertTrue(
          hasExpectedMigrationLine,
          "Did not find log entry for completing expected number of migrations ("
              + numMigrations
              + ") \nSTDOUT:\n"
              + appRunConsumer.getStdoutContents());
      assertTrue(
          hasLogLine(appRunConsumer, String.format("now at version v%s", expectedVersion)),
          "Did not find log entry for expected final version (v" + expectedVersion + ")");

      // verify that progress messages were passed to SQS
      final var progressMessages = readProgressMessagesFromSQSQueue();
      assertThat(progressMessages)
          .first()
          .asString()
          .contains(MigratorProgress.Stage.Started.name());
      assertThat(progressMessages)
          .hasAtLeastOneElementOfType(String.class)
          .asString()
          .contains(MigratorProgress.Stage.Connected.name());
      assertThat(progressMessages)
          .hasAtLeastOneElementOfType(String.class)
          .asString()
          .contains(MigratorProgress.Stage.Migrating.name());

    } catch (ConditionTimeoutException e) {
      throw new RuntimeException(
          "Migration application failed to start within timeout, STDOUT:\n"
              + appRunConsumer.getStdoutContents(),
          e);
    }
  }

  /**
   * Test when the migration app runs and the schema is not in the state the models imply, the exit
   * code is 3 (hibernate validation failure).
   *
   * @throws IOException if there is an issue starting the app
   */
  @Test
  void testMigrationRunWhenSchemaDoesntMatchTablesExpectValidationError() throws IOException {
    // Setup
    ProcessBuilder appRunBuilder = createAppProcessBuilder(TestDirectory.VALIDATION_FAILURE);
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Await start/finish of application
    try {
      Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> !appProcess.isAlive());

      // Verify results
      assertEquals(
          3,
          appProcess.exitValue(),
          "Did not get expected error code for validation failure., \nSTDOUT:\n"
              + appRunConsumer.getStdoutContents());
    } catch (ConditionTimeoutException e) {
      throw new RuntimeException(
          "Migration application failed to start within timeout, STDOUT:\n"
              + appRunConsumer.getStdoutContents(),
          e);
    }
  }

  /**
   * Test when the migration app runs and there is a configuration error, the exit code is 1.
   *
   * @throws IOException if there is an issue starting the app
   */
  @Test
  void testMigrationRunWhenConfigErrorExpectExitCodeOne() throws IOException {
    // Setup
    // Run the process without configuring the app
    ProcessBuilder appRunBuilder = new ProcessBuilder(createCommandForMigratorApp());
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Await start/finish of application
    try {
      Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> !appProcess.isAlive());

      // Verify results
      assertEquals(1, appProcess.exitValue());
    } catch (ConditionTimeoutException e) {
      throw new RuntimeException(
          "Migration application failed to start within timeout, STDOUT:\n"
              + appRunConsumer.getStdoutContents(),
          e);
    }
  }

  /**
   * Test when the migration app runs and there are errors with the sql in a script, the exit code
   * is 2.
   *
   * @throws IOException if there is an issue starting the app
   */
  @Test
  void testMigrationRunWhenSchemaFileErrorExpectFailedExitCodeAndMigrationsNotRun()
      throws IOException {
    // Setup
    ProcessBuilder appRunBuilder = createAppProcessBuilder(TestDirectory.BAD_SQL);
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Await start/finish of application
    try {
      Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> !appProcess.isAlive());

      // Verify results

      // Ensure the script directory was found (which would cause a false positive failure)
      assertFalse(
          hasLogLine(appRunConsumer, "Skipping filesystem location"),
          "Could not find path to test files");

      assertEquals(
          2,
          appProcess.exitValue(),
          "Exited with the wrong exit code. STDOUT:\n" + appRunConsumer.getStdoutContents());

      // Test flyway threw an exception by checking the log output
      boolean hasExceptionLogLine = hasLogLine(appRunConsumer, "FlywayMigrateException");
      assertTrue(
          hasExceptionLogLine,
          "Did not have expected log line for migration exception; STDOUT:\n"
              + appRunConsumer.getStdoutContents());
    } catch (ConditionTimeoutException e) {
      throw new RuntimeException(
          "Migration application failed to start within timeout, STDOUT:\n"
              + appRunConsumer.getStdoutContents(),
          e);
    }
  }

  /**
   * Test when the migration app runs and there are two of the same file with the same version, the
   * exit code is 2.
   *
   * @throws IOException if there is an issue starting the app
   */
  @Test
  void testMigrationRunWhenSchemaFileDuplicateVersionExpectFailedExitCodeAndMigrationsNotRun()
      throws IOException {
    // Setup
    ProcessBuilder appRunBuilder = createAppProcessBuilder(TestDirectory.DUPLICATE_VERSION);
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Await start/finish of application
    try {
      Awaitility.await().atMost(Durations.ONE_MINUTE).until(() -> !appProcess.isAlive());

      LOGGER.info(appRunConsumer.getStdoutContents());

      // Verify results

      // Ensure the script directory was found (which would cause a false positive failure)
      assertFalse(
          hasLogLine(appRunConsumer, "Skipping filesystem location"),
          "Could not find path to test files");

      assertEquals(2, appProcess.exitValue());

      // Test flyway threw an exception by checking the log output
      assertTrue(
          hasLogLine(appRunConsumer, "FlywayException"),
          "Did not find expected flyway exception STDOUT:\n" + appRunConsumer.getStdoutContents());
      assertTrue(
          hasLogLine(appRunConsumer, "Found more than one migration with version"),
          "Did find duplicate version error STDOUT:\n" + appRunConsumer.getStdoutContents());
    } catch (ConditionTimeoutException e) {
      throw new RuntimeException(
          "Migration application failed to start within timeout, STDOUT:\n"
              + appRunConsumer.getStdoutContents(),
          e);
    }
  }

  /**
   * Gets the number of migration scripts by checking the directory they are located in and counting
   * the files.
   *
   * @return the number migration scripts
   * @throws IOException pass through
   */
  public int getNumMigrationScripts() throws IOException {

    int MAX_SEARCH_DEPTH = 5;
    Path jarFilePath =
        Files.find(
                Path.of("target/db-migrator/"),
                MAX_SEARCH_DEPTH,
                (path, basicFileAttributes) ->
                    path.toFile().getName().matches("bfd-model-rif-.*.\\.jar"))
            .findFirst()
            .orElse(null);
    if (jarFilePath == null) {
      throw new IOException("Could not find jar file for testing num migrations");
    }

    JarFile migrationJar = new JarFile(jarFilePath.toFile());
    Enumeration<? extends JarEntry> enumeration = migrationJar.entries();

    int fileCount = 0;
    while (enumeration.hasMoreElements()) {
      ZipEntry entry = enumeration.nextElement();

      // Check for sql migration scripts
      if (entry.getName().startsWith("db/migration/V") && entry.getName().endsWith(".sql")) {
        fileCount++;
      }
    }
    return fileCount;
  }

  /**
   * Creates a ProcessBuilder for the migrator tests to be run with.
   *
   * @param testDirectory the directory the flyway scripts to run for this test are located
   * @return ProcessBuilder ready with common values set
   */
  private ProcessBuilder createAppProcessBuilder(TestDirectory testDirectory) {
    String[] command = createCommandForMigratorApp();
    ProcessBuilder appRunBuilder = new ProcessBuilder(command);
    appRunBuilder.redirectErrorStream(true);

    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    // Clear the schema before this test
    DatabaseTestUtils.get().dropSchemaForDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    Map<String, String> environment = appRunBuilder.environment();
    environment.put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, dataSourceComponents.getUrl());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, dataSourceComponents.getUsername());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    environment.put(AppConfiguration.ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, "2");

    // add SQS related configuration settings
    environment.put(ENV_VAR_KEY_SQS_QUEUE_NAME, SQS_QUEUE_NAME);
    environment.put(
        ENV_VAR_KEY_SQS_ENDPOINT,
        localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
    environment.put(ENV_VAR_KEY_SQS_REGION, localstack.getRegion());
    environment.put(ENV_VAR_KEY_SQS_ACCESS_KEY, localstack.getAccessKey());
    environment.put(ENV_VAR_KEY_SQS_SECRET_KEY, localstack.getSecretKey());

    Path testFilePath =
        Path.of(".", "src", "test", "resources", "db", "migration-test", "error-scenarios");
    String testFileDir = testFilePath.toAbsolutePath().toString();
    // If real we'll use the default flyway path, else use the test path
    if (testDirectory != TestDirectory.REAL) {
      environment.put(
          AppConfiguration.ENV_VAR_FLYWAY_SCRIPT_LOCATION,
          "filesystem:" + testFileDir + testDirectory.getPath());
    }

    return appRunBuilder;
  }

  /**
   * Read back all of the progress messages (JSON strings) from the SQS queue in localstack.
   *
   * @return the list
   */
  private List<String> readProgressMessagesFromSQSQueue() {
    final var queueUrl = sqsDao.lookupQueueUrl(SQS_QUEUE_NAME);
    var messages = new ArrayList<String>();
    for (Optional<String> message = sqsDao.nextMessage(queueUrl);
        message.isPresent();
        message = sqsDao.nextMessage(queueUrl)) {
      message.ifPresent(messages::add);
    }
    return messages;
  }

  /**
   * Create command for the migrator script to be run.
   *
   * @return the command array for the migrator app
   */
  private static String[] createCommandForMigratorApp() {
    try {
      Path assemblyDirectory =
          Files.list(Paths.get(".", "target", "db-migrator"))
              .filter(f -> f.getFileName().toString().startsWith("bfd-db-migrator-"))
              .findFirst()
              .orElse(Path.of(""));
      Path pipelineAppScript = assemblyDirectory.resolve("bfd-db-migrator.sh");
      return new String[] {pipelineAppScript.toAbsolutePath().toString()};
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks if the app has a line matching the input text (partially or completely).
   *
   * @param appRunConsumer the consumer that is ingesting the application output
   * @param logLine the log line to check for, including fragments
   * @return {@code true} if the output has the specified log fragment
   */
  private static boolean hasLogLine(ProcessOutputConsumer appRunConsumer, String logLine) {
    return appRunConsumer.matches(line -> line.contains(logLine));
  }
}
