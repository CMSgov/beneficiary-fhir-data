package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.Assert.assertEquals;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import javax.persistence.EntityManager;

public class RdaPipelineTestUtils {
  public static void assertMeterReading(long expected, String meterName, Meter meter) {
    assertEquals("Meter " + meterName, expected, meter.getCount());
  }

  public static void assertGaugeReading(long expected, String gaugeName, Gauge<Long> gauge) {
    assertEquals("Gauge " + gaugeName, Long.valueOf(expected), gauge.getValue());
  }

  /**
   * Creates a temporary in-memory HSQLDB that is destroyed when the test ends plus a
   * PipelineApplicationState and EntityManager using that db, passes them to the provided lambda
   * function, then closes them and destroys the database.
   *
   * @param testClass used to create a db name
   * @param clock used for the app state
   * @param test lambda to receive the appState and perform some testing
   */
  public static void runTestWithTemporaryDb(Class<?> testClass, Clock clock, DatabaseConsumer test)
      throws Exception {
    final String dbUrl = "jdbc:hsqldb:mem:" + testClass.getSimpleName();
    // the HSQLDB database will be destroyed when this connection is closed
    try (Connection dbLifetimeConnection =
        DriverManager.getConnection(dbUrl + ";shutdown=true", "", "")) {
      final DatabaseOptions dbOptions = new DatabaseOptions(dbUrl, "", "", 10);
      final MetricRegistry appMetrics = new MetricRegistry();
      final HikariDataSource dataSource =
          PipelineApplicationState.createPooledDataSource(dbOptions, appMetrics);
      DatabaseSchemaManager.createOrUpdateSchema(dataSource);
      try (PipelineApplicationState appState =
          new PipelineApplicationState(appMetrics, dataSource, RDA_PERSISTENCE_UNIT_NAME, clock)) {
        final EntityManager entityManager =
            appState.getEntityManagerFactory().createEntityManager();
        try {
          test.accept(appState, entityManager);
        } finally {
          entityManager.close();
        }
      }
    }
  }

  @FunctionalInterface
  public interface DatabaseConsumer {
    void accept(PipelineApplicationState appState, EntityManager entityManager) throws Exception;
  }
}
