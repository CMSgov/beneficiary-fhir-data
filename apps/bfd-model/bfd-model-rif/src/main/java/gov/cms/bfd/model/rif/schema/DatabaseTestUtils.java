package gov.cms.bfd.model.rif.schema;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileReader;
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
import java.util.Arrays;
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

/**
 * Provides utilities for managing the database in integration tests.
 *
 * <p>Note: This is placed in <code>src/main/java</code> (rather than <code>src/test/java</code>)
 * for convenience: test dependencies aren't transitive, which tends to eff things up.
 */
public final class DatabaseTestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestUtils.class);

  /**
   * This fake JDBC URL prefix is used for custom database setups only used in integration tests,
   * e.g. {@link #initUnpooledDataSourceForHsqlEmbeddedWithServer(String)}.
   */
  public static final String JDBC_URL_PREFIX_BLUEBUTTON_TEST = "jdbc:bfd-test:";

  /**
   * We need to inform Flyway of all of our schemas in order for {@link Flyway#clean()} to work
   * properly.
   */
  public static final ImmutableList<String> FLYWAY_CLEAN_SCHEMAS =
      ImmutableList.of("public", "PUBLIC", "pre_adj", "rda");

  private static final String HSQL_SERVER_USERNAME = "test";
  private static final String HSQL_SERVER_PASSWORD = "test";

  /** The singleton {@link DatabaseTestUtils} instance to use everywhere. */
  private static DatabaseTestUtils SINGLETON;

  /**
   * The {@link DataSource} for the database to test against, as created by {@link
   * #initUnpooledDataSource()}.
   */
  private final DataSource unpooledDataSource;

  /**
   * Constructs a new {@link DatabaseTestUtils} instance. Marked <code>private</code>; use {@link
   * #get()}, instead.
   */
  private DatabaseTestUtils() {
    this.unpooledDataSource = initUnpooledDataSource();
  }

  /** @return the singleton {@link DatabaseTestUtils} instance to use everywhere */
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
   * @return the {@link DataSource} for the database to test against (as specified by the <code>
   *     its.db.*</code> system properties, see {@link #initUnpooledDataSource() for details})
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
    if (bfdServerTestDatabaseProperties.isPresent()) {
      url = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.url");
      username = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.username");
      password = bfdServerTestDatabaseProperties.get().getProperty("bfdServer.db.password");
    } else {
      /*
       * Build default DB connection properties that use HSQL, just as they're configured in the
       * parent POM.
       */
      String urlDefault = String.format("%shsqldb:mem", JDBC_URL_PREFIX_BLUEBUTTON_TEST);
      String usernameDefault = null;
      String passwordDefault = null;

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
   * @return the {@link Properties} file that contains the test DB connection properties (as created
   *     by <code>gov.cms.bfd.server.war.SpringConfiguration#findTestDatabaseProperties()</code>, or
   *     {@link Optional#empty()} if it's not present (indicating that just a regular DB connection
   *     is being used)
   */
  private static Optional<Properties> readTestDatabaseProperties() {
    Path testDatabasePropertiesPath = findTestDatabaseProperties();
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
   * @return the {@link Path} to the {@link Properties} file in <code>
   *     bfd-server-war/target/server-work</code> that the test DB connection properties will be
   *     written out to
   */
  public static Path findTestDatabaseProperties() {
    Path serverRunDir = Paths.get("target", "server-work");
    if (!Files.isDirectory(serverRunDir))
      serverRunDir = Paths.get("bfd-server-war", "target", "server-work");

    Path testDbPropertiesPath = serverRunDir.resolve("server-test-db.properties");
    return testDbPropertiesPath;
  }

  /**
   * @param url the JDBC URL for the test database to connect to
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a {@link DataSource} for the test DB, which will <strong>not</strong> be cleaned or
   *     schema-fied first
   */
  private static DataSource initUnpooledDataSource(String url, String username, String password) {
    DataSource dataSource;
    if (url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST + "hsqldb:mem")) {
      dataSource = initUnpooledDataSourceForHsqlEmbeddedWithServer(url);
    } else if (url.startsWith("jdbc:hsqldb:hsql://localhost")) {
      dataSource = initUnpooledDataSourceForHsqlServer(url, username, password);
    } else if (url.startsWith("jdbc:postgresql:")) {
      dataSource = initUnpooledDataSourceForPostgresql(url, username, password);
    } else {
      throw new BadCodeMonkeyException("Unsupported test DB URL: " + url);
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
      throw new BadCodeMonkeyException(e);
    }

    server.setLogWriter(new PrintWriter(new LoggerWriter(LOGGER, "HSQL Log: ")));
    server.setErrWriter(new PrintWriter(new LoggerWriter(LOGGER, "HSQL Error Log: ")));
    server.start();

    // Create the DataSource to connect to that shiny new DB.
    DataSource dataSource =
        initUnpooledDataSourceForHsqlServer(
            String.format("jdbc:hsqldb:hsql://localhost:%d/test-embedded", hsqldbPort),
            HSQL_SERVER_USERNAME,
            HSQL_SERVER_PASSWORD);
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

    return dataSource;
  }

  /**
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
    return dataSource;
  }

  /**
   * @return the cached and shared unpooled {@link DataSource} for the database to test against (as
   *     specified by the <code>its.db.*</code> system properties, see {@link
   *     #initUnpooledDataSource() for details})
   */
  public DataSource getUnpooledDataSource() {
    return unpooledDataSource;
  }

  /** Creates/updates the application schema into the {@link #getUnpooledDataSource()} database */
  public void createOrUpdateSchemaForDataSource() {
    DatabaseSchemaManager.createOrUpdateSchema(unpooledDataSource);
  }

  /**
   * Drops all schemas in the {@link #getUnpooledDataSource()} database. Please note that this
   * operation appears to not be transactional for HSQL DB: unless <strong>all</strong> connections
   * are closed and re-opened, they may end up with an inconsistent view of the database after this
   * operation.
   */
  public void dropSchemaForDataSource() {
    // Try to prevent career-limiting moves.
    String url;
    try (Connection connection = getUnpooledDataSource().getConnection(); ) {
      url = connection.getMetaData().getURL();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (!url.contains("localhost") && !url.contains("127.0.0.1") && !url.contains("hsqldb:mem")) {
      throw new BadCodeMonkeyException("Our builds can only be run against local test DBs.");
    }

    // Clean the DB so that it's fresh and ready for a new test case.
    Flyway flyway =
        Flyway.configure()
            .dataSource(unpooledDataSource)
            .schemas(FLYWAY_CLEAN_SCHEMAS.toArray(new String[0]))
            .connectRetries(5)
            .load();
    LOGGER.warn("Cleaning schemas: {}", Arrays.asList(flyway.getConfiguration().getSchemas()));
    flyway.clean();
  }

  /**
   * Represents the components required to construct a {@link DataSource} for our test DBs.
   *
   * <p>This is wildly insufficient for more complicated {@link DataSource}s; we're leaning heavily
   * on the very constrained set of simple {@link DataSource}s that are supported for our tests.
   */
  public static final class DataSourceComponents {
    private final String url;
    private final String username;
    private final String password;

    /**
     * Constructs a {@link DataSourceComponents} instance for the specified test {@link DataSource}
     * (does not support more complicated {@link DataSource}s, as discussed in the class' JavaDoc)
     *
     * @param dataSource the data source
     */
    public DataSourceComponents(DataSource dataSource) {
      if (dataSource instanceof JDBCDataSource) {
        JDBCDataSource hsqlDataSource = (JDBCDataSource) dataSource;
        this.url = hsqlDataSource.getUrl();
        this.username = hsqlDataSource.getUser();
        /*
         * HSQL's implementation doesn't expose the DataSource's password, which is dumb. Because
         * I'm lazy, I just hardcode it here. If you need this to NOT be hardcoded, simplest fix
         * would be to write a helper method that pulls the field's value via reflection.
         */
        this.password = HSQL_SERVER_PASSWORD; // no getter available; hardcoded
      } else if (dataSource instanceof PGSimpleDataSource) {
        PGSimpleDataSource pgDataSource = (PGSimpleDataSource) dataSource;
        this.url = pgDataSource.getUrl();
        this.username = pgDataSource.getUser();
        this.password = pgDataSource.getPassword();
      } else {
        throw new BadCodeMonkeyException();
      }
    }

    /** @return the JDBC URL that should be used to connect to the test DB */
    public String getUrl() {
      return url;
    }

    /** @return the username that should be used to connect to the test DB */
    public String getUsername() {
      return username;
    }

    /** @return the password that should be used to connect to the test DB */
    public String getPassword() {
      return password;
    }
  }

  /** Sends output to a specified {@link Logger}. */
  private static final class LoggerWriter extends Writer {
    private final Logger logger;
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

    /** @see java.io.Writer#write(char[], int, int) */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      String message = new String(cbuf, off, len);
      if (message.trim().isEmpty()) return;

      logger.debug(messagePrefix + message);
    }

    /** @see java.io.Writer#flush() */
    @Override
    public void flush() throws IOException {
      // Nothing to do.
    }

    /** @see java.io.Writer#close() */
    @Override
    public void close() throws IOException {
      // Nothing to do.
    }
  }
}
