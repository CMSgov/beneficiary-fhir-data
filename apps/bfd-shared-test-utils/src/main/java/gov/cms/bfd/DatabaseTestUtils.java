package gov.cms.bfd;

import static java.util.Collections.singletonMap;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.hsqldb.jdbc.JDBCDataSource;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl.AclFormatException;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/** Provides utilities for managing the database in integration tests. */
public final class DatabaseTestUtils {
  /** Logger for writing information out. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestUtils.class);

  /**
   * This fake JDBC URL prefix is used for custom database setups only used in integration tests,
   * e.g. {@link #initUnpooledDataSourceForHsqlEmbeddedWithServer(String)}.
   */
  public static final String JDBC_URL_PREFIX_BLUEBUTTON_TEST = "jdbc:bfd-test:";

  /**
   * We need to inform Flyway of all of our schemas in order for {@link Flyway#clean()} to work
   * properly.
   *
   * <p>PUBLIC for hsql and public for postgres since they differ in caps sensitivity.
   */
  public static final List<String> FLYWAY_CLEAN_SCHEMAS = List.of("public", "PUBLIC", "rda", "RDA");

  /** The username used for HSQL locally. */
  static final String HSQL_SERVER_USERNAME = "test";
  /** The password used for HSQL locally. */
  static final String HSQL_SERVER_PASSWORD = "test";

  /** The username used for test container database username. */
  static final String TEST_CONTAINER_DATABASE_USERNAME = "bfd";

  /** The password used for test container database password. */
  static final String TEST_CONTAINER_DATABASE_PASSWORD = "bfdtest";

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
  public static DataSource initUnpooledDataSource() {
    /*
     * This is pretty hacky, but when this class is being used as part of the BFD Server tests, we
     * have to check for the DB connection properties that the BFD Server may have written out when
     * it was launched for the ITs run. If we DON'T use those properties, we'll end up connected to
     * a different database than the one that the application server instance being tested is using,
     * which is definitely not going to do what we wanted.
     */
    Optional<Properties> bfdServerTestDatabaseProperties = readTestDatabaseProperties();

    String url, username, password;
    url = System.getProperty("its.db.url", "");
    String usernameDefault = null;
    String passwordDefault = null;

    if (url != "" && url.endsWith("tc")) {
      // Build the actual DB connection properties to use.
      username = System.getProperty("its.db.username", usernameDefault);
      if (username != null && username.trim().isEmpty())
        username = TEST_CONTAINER_DATABASE_USERNAME;
      password = System.getProperty("its.db.password", passwordDefault);
      if (password != null && password.trim().isEmpty())
        password = TEST_CONTAINER_DATABASE_PASSWORD;
    } else if (bfdServerTestDatabaseProperties.isPresent()) {
      url = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.url");
      username = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.username");
      password = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.password");
    } else {
      /*
       * Build default DB connection properties that use HSQL, just as they're configured in the
       * parent POM.
       */
      String urlDefault = String.format("%shsqldb:mem", JDBC_URL_PREFIX_BLUEBUTTON_TEST);

      // Build the actual DB connection properties to use.
      url = System.getProperty("its.db.url", urlDefault);

      username = System.getProperty("its.db.username", usernameDefault);
      if (username != null && username.trim().isEmpty()) username = usernameDefault;
      password = System.getProperty("its.db.password", passwordDefault);
      if (password != null && password.trim().isEmpty()) password = passwordDefault;
    }

    return initUnpooledDataSource(url, username, password);
  }

  /**
   * Creates an unmigrated HSQL data source for testing specific migration logic.
   *
   * @return an unmigrated hsql data source
   */
  public static DataSource getUnpooledUnmigratedHsqlDataSource() {
    int hsqldbPort = startLocalHsqlServerAndReturnPort();

    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl(String.format("jdbc:hsqldb:hsql://localhost:%d/test-embedded", hsqldbPort));
    dataSource.setUser(HSQL_SERVER_USERNAME);
    dataSource.setPassword(HSQL_SERVER_PASSWORD);

    return dataSource;
  }

  /**
   * Starts a local hsql server after setting various properties.
   *
   * @return the port the hsql server was started on
   */
  private static int startLocalHsqlServerAndReturnPort() {
    /*
     * Select a random local port to run the HSQL DB server on, so that one test run doesn't
     * conflict with another.
     */
    int hsqldbPort = findFreePort();

    HsqlProperties hsqlProperties = new HsqlProperties();
    hsqlProperties.setProperty(
        "server.database.0",
        String.format(
            "mem:test-embedded;user=%s;password=%s", HSQL_SERVER_USERNAME, HSQL_SERVER_PASSWORD));
    hsqlProperties.setProperty("server.dbname.0", "test-embedded");
    hsqlProperties.setProperty("server.address", "127.0.0.1");
    hsqlProperties.setProperty("server.port", "" + hsqldbPort);
    hsqlProperties.setProperty("hsqldb.tx", "mvcc");
    org.hsqldb.server.Server server = new org.hsqldb.server.Server();

    try {
      server.setProperties(hsqlProperties);
    } catch (IOException | AclFormatException e) {
      throw new RuntimeException(e);
    }

    server.setLogWriter(new PrintWriter(new LoggerWriter(LOGGER, "HSQL Log: ")));
    server.setErrWriter(new PrintWriter(new LoggerWriter(LOGGER, "HSQL Error Log: ")));
    server.start();
    return hsqldbPort;
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
    if (url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST + "hsqldb:mem")) {
      dataSource = initUnpooledDataSourceForHsqlEmbeddedWithServer(url);
    } else if (url.startsWith("jdbc:hsqldb:hsql://localhost")) {
      dataSource = initUnpooledDataSourceForHsqlServer(url, username, password);
    } else if (url.startsWith("jdbc:postgresql:")) {
      dataSource = initUnpooledDataSourceForPostgresql(url, username, password);
    } else if (url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST + "tc")) {
      dataSource = initUnpooledDataSourceForTestContainerWithPostgres(username, password);
    } else {
      throw new RuntimeException("Unsupported test DB URL: " + url);
    }

    return dataSource;
  }

  /**
   * Creates an embedded HSQL DB that is also accessible on a local port (via {@link
   * org.hsqldb.server.Server}).
   *
   * @param url the JDBC URL that the application was configured to use
   * @return a HSQL {@link DataSource} for the test DB
   */
  private static DataSource initUnpooledDataSourceForHsqlEmbeddedWithServer(String url) {
    if (!url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST + "hsqldb:mem")) {
      throw new IllegalArgumentException();
    }

    int hsqldbPort = startLocalHsqlServerAndReturnPort();

    // Create the DataSource to connect to that shiny new DB.
    return initUnpooledDataSourceForHsqlServer(
        String.format("jdbc:hsqldb:hsql://localhost:%d/test-embedded", hsqldbPort),
        HSQL_SERVER_USERNAME,
        HSQL_SERVER_PASSWORD);
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
   * Initiates a HSQL {@link DataSource} for the test DB.
   *
   * @param url the JDBC URL that the application was configured to use
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a HSQL {@link DataSource} for the test DB
   */
  private static DataSource initUnpooledDataSourceForHsqlServer(
      String url, String username, String password) {
    if (!url.startsWith("jdbc:hsqldb:hsql://localhost")) {
      throw new IllegalArgumentException();
    }

    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl(url);
    if (username != null) dataSource.setUser(username);
    if (password != null) dataSource.setPassword(password);

    boolean migrationSuccess = DatabaseTestSchemaManager.createOrUpdateSchema(dataSource);
    if (!migrationSuccess) {
      throw new RuntimeException("Schema migration failed during test setup");
    }

    return dataSource;
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

    if (container == null || !container.isRunning()) {
      String testContainerDatabaseImage = System.getProperty("its.testcontainer.db.image", "");
      container =
          new PostgreSQLContainer(testContainerDatabaseImage)
              .withDatabaseName("fhirdb")
              .withUsername(username)
              .withPassword(password)
              .withTmpFs(singletonMap("/var/lib/postgresql/data", "rw"));

      container.start();
    }

    JdbcDatabaseContainer<?> jdbcContainer = (JdbcDatabaseContainer<?>) container;
    DataSource dataSource =
        initUnpooledDataSourceForPostgresql(
            jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword());

    boolean migrationSuccess = DatabaseTestSchemaManager.createOrUpdateSchema(dataSource);
    if (!migrationSuccess) {
      throw new RuntimeException("Schema migration failed during test setup");
    }

    return dataSource;
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
   * Drops all schemas in the {@link #getUnpooledDataSource()} database. Please note that this
   * operation appears to not be transactional for HSQL DB: unless <strong>all</strong> connections
   * are closed and re-opened, they may end up with an inconsistent view of the database after this
   * operation.
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
    if (!url.contains("localhost") && !url.contains("127.0.0.1") && !url.contains("hsqldb:mem")) {
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

  /** Cleans the database to prepare for the next test. */
  public void cleanDataSource() {
    String url;
    try (Connection connection = getUnpooledDataSource().getConnection(); ) {
      url = connection.getMetaData().getURL();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (!url.contains("localhost") && !url.contains("127.0.0.1") && !url.contains("hsqldb:mem")) {
      throw new RuntimeException("Our builds can only be run against local test DBs.");
    }
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

  /**
   * Creates a HSQL database for use in testing.
   *
   * @param url the JDBC URL that the application was configured to use
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   * @return the hikari data source
   */
  public static DataSource createTestDatabase(
      String url, String connectionsMaxText, MetricRegistry metricRegistry) {
    return createTestDatabase(url, null, null, null, connectionsMaxText, metricRegistry);
  }

  /**
   * Some of the DBs we support using in local development and testing require special handling.
   * This method takes care of that.
   *
   * @param url the JDBC URL that the application was configured to use
   * @param urlKey the test properties url key, may be null if not updating a server property file
   * @param usernameKey the test properties username key, may be null if not updating a server
   *     property file
   * @param passwordKey the test properties database password, may be null if not updating a server
   *     property file
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   * @return the hikari data source
   */
  public static DataSource createTestDatabase(
      String url,
      String urlKey,
      String usernameKey,
      String passwordKey,
      String connectionsMaxText,
      MetricRegistry metricRegistry) {
    /*
     * Note: Eventually, we may add support for other test DB types, but
     * right now only in-memory HSQL DBs are supported.
     */
    if (url.endsWith(":hsqldb:mem")) {
      return createTestDatabaseForHsql(
          connectionsMaxText, metricRegistry, urlKey, usernameKey, passwordKey);
    } else {
      throw new RuntimeException("Unsupported test URL: " + url);
    }
  }

  /**
   * Handles {@link #createTestDatabase} for HSQL. We need to special-case the HSQL DBs that are
   * supported by our tests, so that they get handled correctly. Specifically, we need to ensure
   * that the HSQL Server is started up, so that our test code can access the DB directly. In
   * addition, we may need to ensure that connection details to that HSQL server get written out
   * somewhere that the test code can find it.
   *
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   * @param urlKey the url key of the key/value pair used when writing to the test properties file,
   *     may be null if no test properties file is required
   * @param usernameKey the username key of the key/value pair used when writing to the test
   *     properties file, may be null if no test properties file is required
   * @param passwordKey the password key of the key/value pair used when writing to the test
   *     properties file, may be null if no test properties file is required
   * @return the data source for the newly set up database
   */
  private static DataSource createTestDatabaseForHsql(
      String connectionsMaxText,
      MetricRegistry metricRegistry,
      String urlKey,
      String usernameKey,
      String passwordKey) {

    boolean writeDbPropsFile = usernameKey != null && passwordKey != null && urlKey != null;
    Path testDbPropsPath = DatabaseTestUtils.findWarServerTestDatabaseProperties();
    boolean hasTestPropsPath = Files.isReadable(testDbPropsPath);
    if (writeDbPropsFile && hasTestPropsPath) {
      /*
       * Grab the path for the DB server properties, and remove it if found, as it'll be from an older
       * run. We'll (re-)create it later in this method.
       */
      try {
        Files.delete(testDbPropsPath);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);
    boolean migrationSuccess = DatabaseTestSchemaManager.createOrUpdateSchema(dataSource);
    if (!migrationSuccess) {
      throw new RuntimeException("Schema migration failed during test setup");
    }

    // Create the DataSource to connect to that shiny new DB.
    HikariDataSource dataSourcePool = new HikariDataSource();
    dataSourcePool.setDataSource(dataSource);
    configureDataSource(dataSourcePool, connectionsMaxText, metricRegistry);

    if (writeDbPropsFile) {
      /*
       * Write out the DB properties for <code>ServerTestUtils</code> to use.
       * This is primarily used in the BFD server IT tests.
       */
      Properties testDbProps = new Properties();
      testDbProps.setProperty(urlKey, dataSourceComponents.getUrl());
      testDbProps.setProperty(usernameKey, dataSourceComponents.getUsername());
      testDbProps.setProperty(passwordKey, dataSourceComponents.getPassword());
      try {
        testDbProps.store(new FileWriter(testDbPropsPath.toFile()), null);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return dataSourcePool;
  }

  /**
   * Configures a data source.
   *
   * @param poolingDataSource the {@link HikariDataSource} to be configured, which must already have
   *     its basic connection properties (URL, username, password) configured
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   */
  public static void configureDataSource(
      HikariDataSource poolingDataSource,
      String connectionsMaxText,
      MetricRegistry metricRegistry) {
    int connectionsMax;
    try {
      connectionsMax = Integer.parseInt(connectionsMaxText);
    } catch (NumberFormatException e) {
      connectionsMax = -1;
    }
    if (connectionsMax < 1) {
      // Assign a reasonable default value, if none was specified.
      connectionsMax = Runtime.getRuntime().availableProcessors() * 5;
    }

    poolingDataSource.setMaximumPoolSize(connectionsMax);

    /*
     * FIXME Temporary workaround for CBBI-357: send Postgres' query planner a
     * strongly worded letter instructing it to avoid sequential scans whenever
     * possible.
     */
    if (poolingDataSource.getJdbcUrl() != null
        && poolingDataSource.getJdbcUrl().contains("postgre"))
      poolingDataSource.setConnectionInitSql(
          "set application_name = 'bfd-server'; set enable_seqscan = false;");

    poolingDataSource.setRegisterMbeans(true);
    poolingDataSource.setMetricRegistry(metricRegistry);

    /*
     * FIXME Temporary setting for BB-1233 to find the source of any possible leaks
     * (see: https://github.com/brettwooldridge/HikariCP/issues/1111)
     */
    poolingDataSource.setLeakDetectionThreshold(60 * 1000);
  }
}
