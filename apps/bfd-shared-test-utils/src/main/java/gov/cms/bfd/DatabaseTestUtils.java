package gov.cms.bfd;

import static java.util.Collections.singletonMap;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/** Provides utilities for managing the database in integration tests. */
public final class DatabaseTestUtils {
  /** Logger for writing information out. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestUtils.class);

  /**
   * This fake JDBC URL prefix is used for custom database setups only used in integration tests.
   */
  public static final String JDBC_URL_PREFIX_BLUEBUTTON_TEST = "jdbc:bfd-test:";

  /**
   * We need to inform Flyway of all of our schemas in order for {@link Flyway#clean()} to work
   * properly.
   */
  public static final List<String> FLYWAY_CLEAN_SCHEMAS = List.of("public", "rda");

  /** The default database type to use for the integration tests when nothing is provided. */
  public static final String DEFAULT_IT_DATABASE = "jdbc:bfd-test:tc";

  /** The system property defined in pom.xml containing the image to use for test database. */
  public static final String TEST_CONTAINER_DATABASE_IMAGE_PROPERTY = "its.testcontainer.db.image";

  /** The default test container image to use when nothing is provided. */
  public static final String TEST_CONTAINER_DATABASE_IMAGE_DEFAULT = "postgres:14.9-alpine";

  /** The username used for test container database username. */
  public static final String TEST_CONTAINER_DATABASE_USERNAME = "bfd";

  /** The password used for test container database password. */
  public static final String TEST_CONTAINER_DATABASE_PASSWORD = "bfdtest";

  /** The singleton {@link DatabaseTestUtils} instance to use everywhere. */
  private static DatabaseTestUtils SINGLETON;

  /**
   * The {@link DataSource} for the database to test against, as created by {@link
   * #initUnpooledDataSource()}.
   */
  private final DataSource unpooledDataSource;

  /** Generic container for postgressql. */
  private static GenericContainer container = null;

  /**
   * Constructs a new {@link DatabaseTestUtils} instance. Marked <code>private</code>; use {@link
   * #get()}, instead.
   */
  private DatabaseTestUtils() {
    this.unpooledDataSource = initUnpooledDataSource();
  }

  /**
   * Get a singleton for {@link DatabaseTestUtils}.
   *
   * @return the singleton {@link DatabaseTestUtils} instance to use everywhere
   */
  public static synchronized DatabaseTestUtils get() {
    /*
     * Why are we using a singleton and caching all of these fields? Because creating some of the
     * fields here is expensive, so we don't want to have to re-create it for every test.
     */

    if (SINGLETON == null) {
      SINGLETON = new DatabaseTestUtils();
    }

    return SINGLETON;
  }

  /**
   * Returns a {@link DataSource} for the database to test against (as specified by the <code>
   * its.db.*</code> system properties, see {@link #initUnpooledDataSource() for details}).
   *
   * @return the datasource
   */
  private static DataSource initUnpooledDataSource() {
    /*
     * This is pretty hacky, but when this class is being used as part of the BFD Server tests, we
     * have to check for the DB connection properties that the BFD Server may have written out when
     * it was launched for the ITs run. If we DON'T use those properties, we'll end up connected to
     * a different database than the one that the application server instance being tested is using,
     * which is definitely not going to do what we wanted.
     */
    Optional<Properties> bfdServerTestDatabaseProperties = readTestDatabaseProperties();

    String url, username, password;
    url = System.getProperty("its.db.url", DEFAULT_IT_DATABASE);

    if (bfdServerTestDatabaseProperties.isPresent()) {
      LOGGER.info("Setting up data source using server properties ({})", url);
      url = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.url");
      username = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.username");
      password = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.password");
    } else {
      LOGGER.info("Setting up postgres test container data source");
      // Build the test container postgres db by default
      username = System.getProperty("its.db.username", TEST_CONTAINER_DATABASE_USERNAME);
      if (username == null || username.trim().isBlank()) {
        username = TEST_CONTAINER_DATABASE_USERNAME;
      }
      password = System.getProperty("its.db.password", TEST_CONTAINER_DATABASE_PASSWORD);
      if (password == null || password.trim().isBlank()) {
        password = TEST_CONTAINER_DATABASE_PASSWORD;
      }
    }

    return initUnpooledDataSource(url, username, password);
  }

  /**
   * Gets the {@link Properties} file that contains the test DB connection properties (as created by
   * <code>gov.cms.bfd.server.war.SpringConfiguration#findTestDatabaseProperties()</code>.
   *
   * @return the properties file or {@link Optional#empty()} if it's not present (indicating that
   *     just a regular DB connection is being used)
   */
  private static Optional<Properties> readTestDatabaseProperties() {
    Path testDatabasePropertiesPath = findWarServerTestDatabaseProperties();
    if (!Files.isRegularFile(testDatabasePropertiesPath)) return Optional.empty();

    try {
      Properties testDbProps = new Properties();
      testDbProps.load(new FileReader(testDatabasePropertiesPath.toFile()));
      return Optional.of(testDbProps);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Gets the {@link Path} to the {@link Properties} file in <code>
   *  bfd-server-war/target/server-work</code> that the test DB connection properties will be
   * written out to.
   *
   * @return the database properties path
   */
  public static Path findWarServerTestDatabaseProperties() {
    Path serverRunDir = Paths.get("target", "server-work");
    if (!Files.isDirectory(serverRunDir)) {
      serverRunDir = Paths.get("bfd-server-war", "target", "server-work");
    }

    return serverRunDir.resolve("server-test-db.properties");
  }

  /**
   * Initiates a {@link DataSource} for the test DB, which will <strong>not</strong> be cleaned or
   * schema-fied first.
   *
   * @param url the JDBC URL for the test database to connect to
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return the datasource
   */
  private static DataSource initUnpooledDataSource(String url, String username, String password) {
    DataSource dataSource;
    if (url.startsWith("jdbc:postgresql:")) {
      dataSource = initUnpooledDataSourceForPostgresql(url, username, password);
    } else if (url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST + "tc")) {
      dataSource = initUnpooledDataSourceForTestContainerWithPostgres(username, password);
    } else {
      throw new RuntimeException("Unsupported test DB URL: " + url);
    }

    return dataSource;
  }

  /**
   * Note: It's possible for this to result in race conditions, if the random port selected enters
   * use after this method returns and before whatever called this method gets a chance to grab it.
   * It's pretty unlikely, though, and there's not much we can do about it, either. So.
   *
   * @return a free local port number
   */
  private static int findFreePort() {
    try (ServerSocket socket = new ServerSocket(0, 50, Inet4Address.getByName("127.0.0.1"))) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Initiates a PostgreSQL {@link DataSource} for the test DB.
   *
   * @param url the PostgreSQL JDBC URL to use
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a PostgreSQL {@link DataSource} for the test DB
   */
  private static DataSource initUnpooledDataSourceForPostgresql(
      String url, String username, String password) {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(url);
    if (username != null) dataSource.setUser(username);
    if (password != null) dataSource.setPassword(password);

    // In order to store and retrieve JSON in postgresql without adding any additional maven
    // dependencies  we can set this property to allow String values to be transparently
    // converted to/from jsonb values.
    try {
      dataSource.setProperty("stringtype", "unspecified");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    boolean migrationSuccess = DatabaseTestSchemaManager.createOrUpdateSchema(dataSource);
    if (!migrationSuccess) {
      throw new RuntimeException("Schema migration failed during test setup");
    }

    return dataSource;
  }

  /**
   * Initiates a Test Container PostgreSQL {@link DataSource} for the test DB.
   *
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a PostgreSQL {@link DataSource} for the test DB
   */
  private static DataSource initUnpooledDataSourceForTestContainerWithPostgres(
      String username, String password) {

    String testContainerDatabaseImage =
        System.getProperty(
            TEST_CONTAINER_DATABASE_IMAGE_PROPERTY, TEST_CONTAINER_DATABASE_IMAGE_DEFAULT);
    LOGGER.debug("Starting container, using image {}", testContainerDatabaseImage);
    container =
        new PostgreSQLContainer(testContainerDatabaseImage)
            .withDatabaseName("fhirdb")
            .withUsername(username)
            .withPassword(password)
            .withTmpFs(singletonMap("/var/lib/postgresql/data", "rw"))
            .waitingFor(Wait.forListeningPort());

    container.start();

    LOGGER.debug("Container started, running migrations...");
    JdbcDatabaseContainer<?> jdbcContainer = (JdbcDatabaseContainer<?>) container;
    DataSource dataSource =
        initUnpooledDataSourceForPostgresql(
            jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword());
    LOGGER.debug("Ran migrations on container.");
    return dataSource;
  }

  /**
   * Initiates a pooled Test Container PostgreSQL {@link DataSource} for the test DB.
   *
   * @param maxPoolSize the max pool size
   * @param metrics the metrics
   * @return a PostgreSQL {@link DataSource} for the test DB
   */
  public static HikariDataSource createPooledTestContainerDataSourceForTestContainerWithPostgres(
      int maxPoolSize, MetricRegistry metrics) {

    String testContainerDatabaseImage =
        System.getProperty(
            TEST_CONTAINER_DATABASE_IMAGE_PROPERTY, TEST_CONTAINER_DATABASE_IMAGE_DEFAULT);
    LOGGER.debug("Starting container, using image {}", testContainerDatabaseImage);
    container =
        new PostgreSQLContainer(testContainerDatabaseImage)
            .withDatabaseName("fhirdb")
            .withUsername(DatabaseTestUtils.TEST_CONTAINER_DATABASE_USERNAME)
            .withPassword(DatabaseTestUtils.TEST_CONTAINER_DATABASE_PASSWORD)
            .withTmpFs(singletonMap("/var/lib/postgresql/data", "rw"))
            .waitingFor(Wait.forListeningPort());
    container.start();

    LOGGER.debug("Container started, running migrations...");
    JdbcDatabaseContainer<?> jdbcContainer = (JdbcDatabaseContainer<?>) container;
    HikariDataSource pooledDataSource = new HikariDataSource();
    Properties dataSourceProperties = new Properties();
    dataSourceProperties.setProperty("stringtype", "unspecified");
    pooledDataSource.setDataSourceProperties(dataSourceProperties);
    pooledDataSource.setJdbcUrl(jdbcContainer.getJdbcUrl());
    pooledDataSource.setUsername(jdbcContainer.getUsername());
    pooledDataSource.setPassword(jdbcContainer.getPassword());
    pooledDataSource.setMaximumPoolSize(maxPoolSize);
    pooledDataSource.setRegisterMbeans(true);
    pooledDataSource.setMetricRegistry(metrics);

    boolean migrationSuccess = DatabaseTestSchemaManager.createOrUpdateSchema(pooledDataSource);
    if (!migrationSuccess) {
      throw new RuntimeException("Schema migration failed during test setup");
    }

    LOGGER.debug("Ran migrations on container.");
    return pooledDataSource;
  }

  /**
   * Gets the cached and shared unpooled {@link DataSource} for the database to test against (as
   * specified by the <code>its.db.*</code> system properties, see {@link #initUnpooledDataSource()
   * for details}).
   *
   * @return the cached and shared unpooled {@link DataSource} for the database to test against (as
   *     specified by the <code>its.db.*</code> system properties, see {@link
   *     #initUnpooledDataSource() for details})
   */
  public DataSource getUnpooledDataSource() {
    return unpooledDataSource;
  }

  /**
   * Drops all schemas in the {@link #getUnpooledDataSource()} database.
   *
   * @return if the database was successfully cleaned
   */
  public boolean dropSchemaForDataSource() {
    // Try to prevent career-limiting moves.
    String url;
    try (Connection connection = getUnpooledDataSource().getConnection(); ) {
      url = connection.getMetaData().getURL();
    } catch (SQLException e) {
      LOGGER.error("SQL Exception when cleaning DB: ", e);
      return false;
    }
    if (!url.contains("localhost") && !url.contains("127.0.0.1")) {
      LOGGER.error("Our builds can only be run against local test DBs.");
      return false;
    }

    // Clean the DB so that it's fresh and ready for a new test case.
    Flyway flyway =
        Flyway.configure()
            .dataSource(unpooledDataSource)
            .schemas(FLYWAY_CLEAN_SCHEMAS.toArray(new String[0]))
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .connectRetries(2)
            .cleanDisabled(false)
            .placeholders(DatabaseTestSchemaManager.createScriptPlaceholdersMap(unpooledDataSource))
            .load();
    LOGGER.warn("Cleaning schemas: {}", Arrays.asList(flyway.getConfiguration().getSchemas()));
    flyway.clean();
    // Drop any created types since flyway's clean doesnt do this
    // Make sure we are not using test containers locally.
    if (!url.startsWith("jdbc:postgresql://localhost")) {
      try (Connection connection = getUnpooledDataSource().getConnection(); ) {
        try (Statement statement = connection.createStatement()) {
          statement.executeUpdate("DROP TYPE JSONB");
          statement.executeUpdate("DROP TYPE JSON");
        }
        connection.commit();
      } catch (SQLException e) {
        LOGGER.error("SQL Exception when cleaning DB: ", e);
        return false;
      }
    }
    return true;
  }

  /** Sends output to a specified {@link Logger}. */
  private static final class LoggerWriter extends Writer {
    /** The logger to use for this writer. */
    private final Logger logger;

    /** The message prefix to put before log messages written. */
    private final String messagePrefix;

    /**
     * Constructs a new {@link LoggerWriter} instance.
     *
     * @param logger the {@link Logger} to output to
     * @param messagePrefix the text to prefix every log message with
     */
    public LoggerWriter(Logger logger, String messagePrefix) {
      this.logger = logger;
      this.messagePrefix = messagePrefix;
    }

    /** {@inheritDoc} */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      String message = new String(cbuf, off, len);
      if (message.trim().isEmpty()) return;

      logger.debug(messagePrefix + message);
    }

    /** {@inheritDoc} */
    @Override
    public void flush() throws IOException {
      // Nothing to do.
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
      // Nothing to do.
    }
  }
}
