package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * A base test to inherit from if a test class requires a server to be spun up for the test. Will
 * start a server prior to the test (if needed) with a containerized postgres database by default.
 */
public class ServerBaseTest {

  /** Sets up test resources. */
  @BeforeAll
  static void setup() throws IOException {
    /*DataSource dataSource = DatabaseTestUtils.initUnpooledDataSource();
    String dbUrl = "";
    if (dataSource instanceof PGSimpleDataSource) {
      dbUrl = ((PGSimpleDataSource)dataSource).getUrl();
    }*/

    boolean startedServer = ServerExecutor.startServer();
    assertTrue(startedServer, "Could not startup server for tests.");
  }

  /** Releases test resources. */
  @AfterAll
  static void tearDown() {
    ServerExecutor.stopServer();
  }
}
