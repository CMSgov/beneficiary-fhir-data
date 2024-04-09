package gov.cms.bfd.server.war;

import static gov.cms.bfd.DatabaseTestUtils.DEFAULT_IT_DATABASE;
import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_PASSWORD;
import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_USERNAME;
import static io.restassured.RestAssured.certificate;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.DatabaseTestUtils;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import jakarta.annotation.Nullable;
import jakarta.annotation.concurrent.GuardedBy;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * A base test to inherit from if a test class requires a server to be spun up for the test. Will
 * start a server prior to the test (if needed) with the datasource specified by the system
 * properties: 'its.db.url', 'its.db.username', 'its.db.password'. Only local databases are allowed,
 * since the database is truncated after each test.
 */
public class ServerRequiredTest {

  /** The database url used to set up the tests. */
  private static final String DB_URL = System.getProperty("its.db.url", DEFAULT_IT_DATABASE);

  /** The database connection to use for the test. */
  @GuardedBy("class synchronized")
  private static DataSource dataSource;

  /** The base url to use when hitting the container server endpoint. */
  @GuardedBy("class synchronized")
  protected static String baseServerUrl;

  /** The request spec with the auth certificate to use when connecting to the test server. */
  @GuardedBy("class synchronized")
  protected static RequestSpecification requestAuth;

  /** The Server test utils instance, for convenience and brevity. */
  protected ServerTestUtils testUtils = ServerTestUtils.get();

  /** Sets up the test server (and required datasource) if the server is not already running. */
  @BeforeAll
  protected static synchronized void setup() throws IOException {
    if (!ServerExecutor.isRunning()) {
      assertTrue(
          ServerTestUtils.isValidServerDatabase(DB_URL),
          "'its.db.url' was set to an illegal db value; should be a local database (container or otherwise).");
      // Initialize/get the database/datasource, so we can just pass a connection string to the
      // server
      dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
      String resolvedDbUrl;
      String dbUsername;
      String dbPassword;
      // Grab the previously set-up local database url to pass to the test server
      if (dataSource instanceof PGSimpleDataSource && DB_URL.endsWith("tc")) {
        resolvedDbUrl = ((PGSimpleDataSource) dataSource).getUrl();
        dbUsername = TEST_CONTAINER_DATABASE_USERNAME;
        dbPassword = TEST_CONTAINER_DATABASE_PASSWORD;
      } else {
        // If we support other datasources in the future, cast and pull the actual URL from them
        // like above
        throw new IllegalStateException("Unable to setup test server with requested datasource.");
      }

      boolean startedServer = ServerExecutor.startServer(resolvedDbUrl, dbUsername, dbPassword);
      assertTrue(startedServer, "Could not startup server for tests.");
      baseServerUrl = "https://localhost:" + ServerExecutor.getServerPort();
      setRequestAuth();
      // Setup a shutdown hook to shut down the server when we are finished with all tests
      Runtime.getRuntime().addShutdownHook(new Thread(ServerExecutor::stopServer));
    }
  }

  /** Sets the request auth (security certs) used in calls to the local server. */
  private static void setRequestAuth() {
    // Get the certs for the test
    String trustStorePath = "src/test/resources/certs/test-truststore.jks";
    String keyStorePath = "src/test/resources/certs/test-keystore.p12";
    String testPassword = "changeit";
    String keystoreType = "pkcs12";
    // Set up the cert for the calls
    AuthenticationScheme testCertificate =
        certificate(
            trustStorePath,
            testPassword,
            keyStorePath,
            testPassword,
            CertificateAuthSettings.certAuthSettings()
                .keyStoreType(keystoreType)
                .trustStoreType(keystoreType)
                .allowAllHostnames());
    requestAuth =
        new RequestSpecBuilder().setBaseUri(baseServerUrl).setAuth(testCertificate).build();
  }

  /**
   * Cleans the database after each test by truncating all non-RDA data. (RDA is skipped since RDA
   * tests currently manage their own data cleanup).
   */
  @AfterEach
  public void cleanDatabaseServerAfterEachTestCase() {
    final DataSource dataSource = getDataSource();
    if (dataSource != null && ServerTestUtils.isValidServerDatabase(DB_URL)) {
      ServerTestUtils.get().truncateNonRdaTablesInDataSource(dataSource);
    }
  }

  /**
   * Gets the current {@link DataSource} (if any).
   *
   * @return the data source or null if there is none
   */
  @Nullable
  private static synchronized DataSource getDataSource() {
    return dataSource;
  }
}
