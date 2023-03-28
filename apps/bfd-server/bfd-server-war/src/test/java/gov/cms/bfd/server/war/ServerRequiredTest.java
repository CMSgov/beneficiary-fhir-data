package gov.cms.bfd.server.war;

import static gov.cms.bfd.DatabaseTestUtils.DEFAULT_IT_DATABASE;
import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_PASSWORD;
import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_USERNAME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.DatabaseTestUtils;
import java.io.IOException;
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

  /** The Data source. */
  private static DataSource dataSource;

  /** The database url used to set up the tests. */
  private static final String dbUrl = System.getProperty("its.db.url", DEFAULT_IT_DATABASE);

  /** Sets up test resources. */
  @BeforeAll
  static void setup() throws IOException {

    if (!ServerExecutor.isRunning()) {
      assertTrue(
          ServerTestUtils.isValidServerDatabase(dbUrl),
          "'its.db.url' was set to an illegal db value; should be a local database (container or otherwise) OR an in-memory hsql db.");
      if (dataSource == null) {
        dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
      }
      String resolvedDbUrl = dbUrl;
      String dbUsername = System.getProperty("its.db.username", null);
      String dbPassword = System.getProperty("its.db.password", null);
      // If we're using a test container, get the test container's 'real' database url to set up the
      // server with
      if (dataSource instanceof PGSimpleDataSource && dbUrl.endsWith("tc")) {
        resolvedDbUrl = ((PGSimpleDataSource) dataSource).getUrl();
        dbUsername = TEST_CONTAINER_DATABASE_USERNAME;
        dbPassword = TEST_CONTAINER_DATABASE_PASSWORD;
      }

      boolean startedServer = ServerExecutor.startServer(resolvedDbUrl, dbUsername, dbPassword);
      assertTrue(startedServer, "Could not startup server for tests.");
      // Shutdown the server when we are finished with all tests
      Runtime.getRuntime().addShutdownHook(new Thread(ServerExecutor::stopServer));
    }
  }

  /** Cleans the database after each test. */
  @AfterEach
  public void cleanDatabaseServerAfterEachTestCase() {
    if (dataSource != null && ServerTestUtils.isValidServerDatabase(dbUrl)) {
      ServerTestUtils.get().truncateTablesInDataSource(dataSource);
    }
  }
}
