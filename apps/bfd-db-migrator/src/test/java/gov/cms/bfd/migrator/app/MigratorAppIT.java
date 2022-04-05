package gov.cms.bfd.migrator.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.migrator.util.DatabaseTestUtils;
import gov.cms.bfd.migrator.util.DatabaseTestUtils.DataSourceComponents;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the migrator app under various conditions to ensure it works correctly. */
public final class MigratorAppIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigratorApp.class);

  // Enum for determining which flyway script directory to run a test against
  private enum TestDirectory {
    REAL(""),
    BAD_SQL("/bad-sql"),
    DUPLICATE_VERSION("/duplicate-version");

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

  /** Cleans up the database after each test. */
  @BeforeEach
  public void teardown() {
    DatabaseTestUtils.get().dropSchemaForDataSource();
  }

  /**
   * Test when the migration app runs and there are no errors with the scripts, the exit code is 0.
   *
   * @throws IOException if there is an issue starting the app
   */
  @Test
  public void testMigrationRunWhenNoErrorsAndAllFilesRunExpectExitCodeZeroAndSchemaMigrationsRun()
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
      Awaitility.await()
          .atMost(new Duration(60, TimeUnit.SECONDS))
          .until(() -> !appProcess.isAlive());

      // Verify results
      assertEquals(0, appProcess.exitValue());

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
  public void testMigrationRunWhenConfigErrorExpectExitCodeOne() throws IOException {
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
      Awaitility.await()
          .atMost(new Duration(60, TimeUnit.SECONDS))
          .until(() -> !appProcess.isAlive());

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
  public void testMigrationRunWhenSchemaFileErrorExpectFailedExitCodeAndMigrationsNotRun()
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
      Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> !appProcess.isAlive());

      // Verify results

      // Ensure the script directory was found (which would cause a false positive failure)
      assertFalse(
          hasLogLine(appRunConsumer, "Skipping filesystem location"),
          "Could not find path to test files");

      assertEquals(2, appProcess.exitValue());

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
  public void
      testMigrationRunWhenSchemaFileDuplicateVersionExpectFailedExitCodeAndMigrationsNotRun()
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
      Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> !appProcess.isAlive());

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
   * Gets the number of migration scripts by checking the directory they are located in.
   *
   * @return the number migration scripts
   */
  public int getNumMigrationScripts() {
    String migrationPath = "src/main/resources/db/migration";

    long fileCount = 0;
    try (Stream<Path> files = Files.list(Paths.get(migrationPath))) {
      fileCount = files.count();
    } catch (IOException io) {
      fail("Could not find file path for migration scripts at: " + migrationPath);
    }

    return (int) fileCount;
  }

  /**
   * Creates a ProcessBuilder for the migrator tests to be run with.
   *
   * @param testDirectory the directory the flyway scripts to run for this test are located
   * @return ProcessBuilder ready with common values set
   */
  private static ProcessBuilder createAppProcessBuilder(TestDirectory testDirectory) {
    String[] command = createCommandForMigratorApp();
    ProcessBuilder appRunBuilder = new ProcessBuilder(command);
    appRunBuilder.redirectErrorStream(true);

    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, dataSourceComponents.getUrl());
    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, dataSourceComponents.getUsername());
    appRunBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, "1");
    Path testFilePath =
        Path.of(".", "src", "test", "resources", "db", "migration", "error-scenarios");
    String testFileDir = testFilePath.toAbsolutePath().toString();
    // If real we'll use the default flyway path, else use the test path
    if (testDirectory != TestDirectory.REAL) {
      appRunBuilder
          .environment()
          .put(
              AppConfiguration.ENV_VAR_FLYWAY_SCRIPT_LOCATION,
              "filesystem:" + testFileDir + testDirectory.getPath());
    }

    return appRunBuilder;
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
