package gov.cms.bfd.migrator.app;

import static gov.cms.bfd.migrator.app.AppConfiguration.ENV_VAR_KEY_SQS_QUEUE_NAME;
import static gov.cms.bfd.migrator.app.MigratorApp.EXIT_CODE_BAD_CONFIG;
import static gov.cms.bfd.migrator.app.MigratorApp.EXIT_CODE_FAILED_HIBERNATE_VALIDATION;
import static gov.cms.bfd.migrator.app.MigratorApp.EXIT_CODE_FAILED_MIGRATION;
import static gov.cms.bfd.migrator.app.MigratorApp.EXIT_CODE_SUCCESS;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_ACCESS_KEY;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_ENDPOINT;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_REGION;
import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.AbstractLocalStackTest;
import gov.cms.bfd.DataSourceComponents;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.FileBasedAssertionHelper;
import gov.cms.bfd.migrator.app.SqsProgressReporter.SqsProgressMessage;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.sql.DataSource;
import lombok.Getter;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the migrator app under various conditions to ensure it works correctly. */
public final class MigratorAppIT extends AbstractLocalStackTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigratorAppIT.class);

  /** Name of SQS queue created in localstack to receive progress messages via SQS. */
  private static final String SQS_QUEUE_NAME = "migrator-progress.fifo";

  /**
   * Name of log file that will contain log output from the migrator. This has to match the value in
   * our {@code logback-test.xml} file.
   */
  private static final String LOG_FILE_PATH = "target/migratorOutput.log";

  /**
   * Controls access to the log file so that multiple tests do not try to write to it at the same
   * time. Package accessible so it can be used by other tests as well.
   */
  @VisibleForTesting
  static final FileBasedAssertionHelper LOG_FILE =
      new FileBasedAssertionHelper(Path.of(LOG_FILE_PATH));

  /** Used to communicate with the localstack SQS service. */
  private SqsDao sqsDao;

  /** Enum for determining which flyway script directory to run a test against. */
  @Getter
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
     * Initializes an instance.
     *
     * @param path the path to the test files for this selection
     */
    TestDirectory(String path) {
      this.path = path;
    }
  }

  /** Cleans up the database before each test. */
  @BeforeEach
  public void setup() {
    DatabaseTestUtils.get().dropSchemaForDataSource();
  }

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
    MigratorApp app = spy(new MigratorApp());
    ConfigLoader configLoader = createConfigLoader(TestDirectory.REAL);
    doReturn(configLoader).when(app).createConfigLoader();

    int numMigrations = getNumMigrationScripts();
    /* Normally the version would be the same as the number of files, but
    migration #12 is missing for some reason in the real scripts, so the version will be files +1 */
    int expectedVersion = numMigrations + 1;

    try {
      // Run the app and collect its output.
      final int exitCode = app.performMigrationsAndHandleExceptions();
      final String logOutput = LOG_FILE.readFileAsString();

      // Verify results
      assertEquals(
          EXIT_CODE_SUCCESS, exitCode, "Did not get expected exit code, \nOUTPUT:\n" + logOutput);

      // Test the migrations occurred by checking the log output
      boolean hasExpectedMigrationLine =
          logOutput.contains(
              String.format("Successfully applied %s migrations to schema", numMigrations));

      assertTrue(
          hasExpectedMigrationLine,
          "Did not find log entry for completing expected number of migrations ("
              + numMigrations
              + ") \nOUTPUT:\n"
              + logOutput);
      assertTrue(
          logOutput.contains(String.format("now at version v%s", expectedVersion)),
          "Did not find log entry for expected final version (v" + expectedVersion + ")");

      // verify that progress messages were passed to SQS
      final var progressMessages = readProgressMessagesFromSQSQueue();
      assertThat(progressMessages)
          .isSortedAccordingTo(Comparator.comparing(SqsProgressMessage::getMessageId));
      assertThat(progressMessages)
          .first()
          .matches(m -> m.getAppStage() == MigratorProgress.Stage.Started);
      assertThat(progressMessages.get(1))
          .matches(m -> m.getAppStage() == MigratorProgress.Stage.Connected);
      assertThat(progressMessages.subList(2, progressMessages.size() - 1))
          .allMatch(m -> m.getAppStage() == MigratorProgress.Stage.Migrating);
      assertThat(progressMessages)
          .last()
          .matches(m -> m.getAppStage() == MigratorProgress.Stage.Finished);

    } catch (ConditionTimeoutException e) {
      final String logOutput = LOG_FILE.readFileAsString();
      fail("Migration application threw exception, OUTPUT:\n" + logOutput, e);
    }
  }

  /**
   * Test when the migration app runs and the schema is not in the state the models imply, the exit
   * code is 3 (hibernate validation failure).
   */
  @Test
  void testMigrationRunWhenSchemaDoesntMatchTablesExpectValidationError() {
    // Setup
    MigratorApp app = spy(new MigratorApp());
    ConfigLoader configLoader = createConfigLoader(TestDirectory.VALIDATION_FAILURE);
    doReturn(configLoader).when(app).createConfigLoader();

    try {
      // Run the app and collect its output.
      final int exitCode = app.performMigrationsAndHandleExceptions();
      final String logOutput = LOG_FILE.readFileAsString();

      // Verify results
      assertEquals(
          EXIT_CODE_FAILED_HIBERNATE_VALIDATION,
          exitCode,
          "Did not get expected error code for validation failure., \nOUTPUT:\n" + logOutput);
    } catch (Exception e) {
      final String logOutput = LOG_FILE.readFileAsString();
      fail("Migration application threw exception, OUTPUT:\n" + logOutput, e);
    }
  }

  /** Test when the migration app runs and there is a configuration error, the exit code is 1. */
  @Test
  void testMigrationRunWhenConfigErrorExpectExitCodeOne() {
    // Setup
    // Sets up the app with an empty configuration.
    MigratorApp app = spy(new MigratorApp());
    ConfigLoader configLoader = ConfigLoader.builder().build();
    doReturn(configLoader).when(app).createConfigLoader();

    try {
      // Run the app and collect its output.
      final int exitCode = app.performMigrationsAndHandleExceptions();
      final String logOutput = LOG_FILE.readFileAsString();

      // Verify results
      assertEquals(EXIT_CODE_BAD_CONFIG, exitCode);
    } catch (Exception e) {
      final String logOutput = LOG_FILE.readFileAsString();
      fail("Migration application threw exception, OUTPUT:\n" + logOutput, e);
    }
  }

  /**
   * Test when the migration app runs and there are errors with the sql in a script, the exit code
   * is 2.
   */
  @Test
  void testMigrationRunWhenSchemaFileErrorExpectFailedExitCodeAndMigrationsNotRun() {
    // Setup
    MigratorApp app = spy(new MigratorApp());
    ConfigLoader configLoader = createConfigLoader(TestDirectory.BAD_SQL);
    doReturn(configLoader).when(app).createConfigLoader();

    try {
      // Run the app and collect its output.
      final int exitCode = app.performMigrationsAndHandleExceptions();
      final String logOutput = LOG_FILE.readFileAsString();

      // Verify results

      // Ensure the script directory was found (which would cause a false positive failure)
      assertFalse(
          logOutput.contains("Skipping filesystem location"), "Could not find path to test files");

      assertEquals(
          EXIT_CODE_FAILED_MIGRATION,
          exitCode,
          "Exited with the wrong exit code. OUTPUT:\n" + logOutput);

      // Test flyway threw an exception by checking the log output
      boolean hasExceptionLogLine = logOutput.contains("FlywayMigrateException");
      assertTrue(
          hasExceptionLogLine,
          "Did not have expected log line for migration exception; OUTPUT:\n" + logOutput);
    } catch (Exception e) {
      final String logOutput = LOG_FILE.readFileAsString();
      fail("Migration application threw exception, OUTPUT:\n" + logOutput, e);
    }
  }

  /**
   * Test when the migration app runs and there are two of the same file with the same version, the
   * exit code is 2.
   */
  @Test
  void testMigrationRunWhenSchemaFileDuplicateVersionExpectFailedExitCodeAndMigrationsNotRun() {
    // Setup
    MigratorApp app = spy(new MigratorApp());
    ConfigLoader configLoader = createConfigLoader(TestDirectory.DUPLICATE_VERSION);
    doReturn(configLoader).when(app).createConfigLoader();

    try {
      // Run the app and collect its output.
      final int exitCode = app.performMigrationsAndHandleExceptions();
      final String logOutput = LOG_FILE.readFileAsString();

      LOGGER.info(logOutput);

      // Verify results

      // Ensure the script directory was found (which would cause a false positive failure)
      assertFalse(
          logOutput.contains("Skipping filesystem location"), "Could not find path to test files");

      assertEquals(EXIT_CODE_FAILED_MIGRATION, exitCode);

      // Test flyway threw an exception by checking the log output
      assertTrue(
          logOutput.contains("FlywayException"),
          "Did not find expected flyway exception OUTPUT:\n" + logOutput);
      assertTrue(
          logOutput.contains("Found more than one migration with version"),
          "Did find duplicate version error OUTPUT:\n" + logOutput);
    } catch (Exception e) {
      final String logOutput = LOG_FILE.readFileAsString();
      fail("Migration application threw exception, OUTPUT:\n" + logOutput, e);
    }
  }

  /**
   * Gets the number of migration scripts by checking the directory they are located in and counting
   * the files.
   *
   * @return the number migration scripts
   * @throws IOException pass through
   */
  private int getNumMigrationScripts() throws IOException {
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
   * Creates the {@link ConfigLoader} used to configure the app for a test.
   *
   * @param testDirectory the directory the flyway scripts to run for this test are located
   * @return config with common values set
   */
  private ConfigLoader createConfigLoader(TestDirectory testDirectory) {
    // Clear the schema before this test
    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DatabaseTestUtils.get().dropSchemaForDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    ImmutableMap.Builder<String, String> environment = ImmutableMap.builder();
    environment.put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, dataSourceComponents.getUrl());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, dataSourceComponents.getUsername());
    environment.put(
        AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    environment.put(AppConfiguration.ENV_VAR_KEY_DATABASE_MAX_POOL_SIZE, "2");

    // add SQS related configuration settings
    environment.put(ENV_VAR_KEY_SQS_QUEUE_NAME, SQS_QUEUE_NAME);
    environment.put(ENV_VAR_KEY_AWS_ENDPOINT, localstack.getEndpoint().toString());
    environment.put(ENV_VAR_KEY_AWS_REGION, localstack.getRegion());
    environment.put(ENV_VAR_KEY_AWS_ACCESS_KEY, localstack.getAccessKey());
    environment.put(ENV_VAR_KEY_AWS_SECRET_KEY, localstack.getSecretKey());

    Path testFilePath =
        Path.of(".", "src", "test", "resources", "db", "migration-test", "error-scenarios");
    String testFileDir = testFilePath.toAbsolutePath().toString();
    // If real we'll use the default flyway path, else use the test path
    if (!testDirectory.equals(TestDirectory.REAL)) {
      environment.put(
          AppConfiguration.ENV_VAR_FLYWAY_SCRIPT_LOCATION,
          "filesystem:" + testFileDir + testDirectory.getPath());
    }

    return ConfigLoader.builder().addSingle(environment.build()::get).build();
  }

  /**
   * Read back all of the progress messages (JSON strings) from the SQS queue in localstack.
   *
   * @return the list
   */
  private List<SqsProgressMessage> readProgressMessagesFromSQSQueue() {
    final var queueUrl = sqsDao.lookupQueueUrl(SQS_QUEUE_NAME);
    var messages = new LinkedList<SqsProgressMessage>();
    sqsDao.processAllMessages(
        queueUrl,
        messageJson -> messages.add(SqsProgressReporter.convertJsonToMessage(messageJson)));
    return messages;
  }
}
