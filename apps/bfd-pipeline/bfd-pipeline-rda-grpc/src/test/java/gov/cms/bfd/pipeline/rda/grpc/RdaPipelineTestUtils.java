package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class RdaPipelineTestUtils {
  public static void assertMeterReading(long expected, String meterName, Meter meter) {
    assertEquals(expected, meter.getCount(), "Meter " + meterName);
  }

  public static void assertGaugeReading(long expected, String gaugeName, Gauge<?> gauge) {
    assertEquals(Long.valueOf(expected), gauge.getValue(), "Gauge " + gaugeName);
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

  /**
   * Looks for a record in the MbiCache table using the given EntityManager.
   *
   * @param entityManager used to perform the query
   * @param mbi mbi string to look for
   * @return null if not cached otherwise the Mbi record from database
   */
  public static Mbi lookupCachedMbi(EntityManager entityManager, String mbi) {
    entityManager.getTransaction().begin();
    final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    final CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
    final Root<Mbi> root = criteria.from(Mbi.class);
    criteria.select(root).where(builder.equal(root.get(Mbi.Fields.mbi), mbi));
    final var records = entityManager.createQuery(criteria).getResultList();
    entityManager.getTransaction().commit();
    return records.isEmpty() ? null : records.get(0);
  }

  @FunctionalInterface
  public interface DatabaseConsumer {
    void accept(PipelineApplicationState appState, EntityManager entityManager) throws Exception;
  }
}
