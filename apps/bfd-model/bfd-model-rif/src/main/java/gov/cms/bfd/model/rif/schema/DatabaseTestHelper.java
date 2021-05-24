package gov.cms.bfd.model.rif.schema;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.Arrays;
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
public final class DatabaseTestHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestHelper.class);

  /**
   * This fake JDBC URL prefix is used for custom database setups only used in integration tests,
   * e.g. {@link #createDataSourceForHsqlEmbeddedWithServer(String)}.
   */
  public static final String JDBC_URL_PREFIX_BLUEBUTTON_TEST = "jdbc:bfd-test:";

  /**
   * We need to inform Flyway of all of our schemas in order for {@link Flyway.clean()} to work
   * properly.
   */
  public static final ImmutableList<String> FLYWAY_CLEAN_SCHEMAS =
      ImmutableList.of("PUBLIC", "pre_adj");

  private static final String HSQL_SERVER_USERNAME = "test";
  private static final String HSQL_SERVER_PASSWORD = "test";

  /** @return the JDBC URL for the test DB to use */
  private static String getTestDatabaseUrl() {
    // Build a default DB URL that uses HSQL, just as it's configured in the parent POM.
    String urlDefault = String.format("%shsqldb:mem", JDBC_URL_PREFIX_BLUEBUTTON_TEST);

    // Pull the DB URL from the system properties.
    String url = System.getProperty("its.db.url", urlDefault);

    return url;
  }

  /** @return the username for the test DB to use */
  private static String getTestDatabaseUsername() {
    // Pull the DB URL from the system properties.
    String username = System.getProperty("its.db.username", null);
    if (username != null && username.trim().isEmpty()) username = null;
    return username;
  }

  /** @return the password for the test DB to use */
  private static String getTestDatabasePassword() {
    // Pull the DB URL from the system properties.
    String password = System.getProperty("its.db.password", null);
    if (password != null && password.trim().isEmpty()) password = null;
    return password;
  }

  /**
   * @return a {@link DataSource} for the test DB, which will <strong>not</strong> be cleaned or
   *     schema-fied first
   */
  public static DataSource getTestDatabase() {
    String url = getTestDatabaseUrl();
    String username = getTestDatabaseUsername();
    String password = getTestDatabasePassword();
    return getTestDatabase(url, username, password);
  }

  /**
   * @param url the JDBC URL for the test database to connect to
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a {@link DataSource} for the test DB, which will <strong>not</strong> be cleaned or
   *     schema-fied first
   */
  public static DataSource getTestDatabase(String url, String username, String password) {
    DataSource dataSource;
    if (url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST + "hsqldb:mem")) {
      dataSource = createDataSourceForHsqlEmbeddedWithServer(url);
    } else if (url.startsWith("jdbc:hsqldb:hsql://localhost")) {
      dataSource = createDataSourceForHsqlServer(url, username, password);
    } else if (url.startsWith("jdbc:postgresql:")) {
      dataSource = createDataSourceForPostgresql(url, username, password);
    } else {
      throw new BadCodeMonkeyException("Unsupported test DB URL: " + url);
    }

    return dataSource;
  }

  /** @return a {@link DataSource} for the test DB, which will be cleaned (i.e. wiped) first */
  public static DataSource getTestDatabaseAfterClean() {
    DataSource dataSource = getTestDatabase();

    // Try to prevent career-limiting moves.
    String url = getTestDatabaseUrl();
    if (!url.contains("localhost") && !url.contains("127.0.0.1") && !url.contains("hsqldb:mem")) {
      throw new BadCodeMonkeyException("Our builds can only be run against local test DBs.");
    }

    // Clean the DB so that it's fresh and ready for a new test case.
    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(FLYWAY_CLEAN_SCHEMAS.toArray(new String[0]))
            .connectRetries(5)
            .load();
    LOGGER.warn("Cleaning schemas: {}", Arrays.asList(flyway.getConfiguration().getSchemas()));
    flyway.clean();
    return dataSource;
  }

  /**
   * @return a {@link DataSource} for the test DB, which will be cleaned (i.e. wiped) and then have
   *     the BFD schema applied to it, first
   */
  public static DataSource getTestDatabaseAfterCleanAndSchema() {
    DataSource dataSource = getTestDatabaseAfterClean();

    // Schema-ify it so it's ready to go.
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);

    return dataSource;
  }

  /**
   * Creates an embedded HSQL DB that is also accessible on a local port (via {@link
   * org.hsqldb.server.Server}).
   *
   * @param url the JDBC URL that the application was configured to use
   * @return a HSQL {@link DataSource} for the test DB
   */
  private static DataSource createDataSourceForHsqlEmbeddedWithServer(String url) {
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
        createDataSourceForHsqlServer(
            String.format("jdbc:hsqldb:hsql://localhost:%d/test-embedded", hsqldbPort),
            HSQL_SERVER_USERNAME,
            HSQL_SERVER_PASSWORD);
    return dataSource;
  }

  /**
   * @param url the JDBC URL that the application was configured to use
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a HSQL {@link DataSource} for the test DB
   */
  private static DataSource createDataSourceForHsqlServer(
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
   * @param url the PostgreSQL JDBC URL to use
   * @param username the username for the test database to connect to
   * @param password the password for the test database to connect to
   * @return a PostgreSQL {@link DataSource} for the test DB
   */
  private static DataSource createDataSourceForPostgresql(
      String url, String username, String password) {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(url);
    if (username != null) dataSource.setUser(username);
    if (password != null) dataSource.setPassword(password);
    return dataSource;
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
