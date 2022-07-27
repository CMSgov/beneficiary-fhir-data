package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class RdaPipelineTestUtils {
  /**
   * Verifies the expected value of a {@link Meter} with a meaningful message in case of a mismatch.
   *
   * @param expected expected reading
   * @param meterName name for inclusion in the message
   * @param meter {@link Meter} to get value from
   */
  public static void assertMeterReading(long expected, String meterName, Meter meter) {
    assertEquals(expected, meter.getCount(), "Meter " + meterName);
  }

  /**
   * Verifies the expected value of a {@link Histogram} with a meaningful message in case of a
   * mismatch.
   *
   * @param expected expected reading
   * @param histogramName name for inclusion in the message
   * @param histogram {@link Histogram} to get value from
   */
  public static void assertHistogramReading(
      long expected, String histogramName, Histogram histogram) {
    long total = Arrays.stream(histogram.getSnapshot().getValues()).sum();
    assertEquals(expected, total, "Histogram " + histogramName);
  }

  /**
   * Verifies the expected value of a {@link Gauge} with a meaningful message in case of a mismatch.
   *
   * @param expected expected reading
   * @param meterName name for inclusion in the message
   * @param histogram {@link Gauge} to get value from
   */
  public static void assertGaugeReading(long expected, String gaugeName, Gauge<?> gauge) {
    assertEquals(expected, gauge.getValue(), "Gauge " + gaugeName);
  }

  /**
   * Verifies the expected number of values have been written to a {@link Timer} with a meaningful
   * message in case of a mismatch.
   *
   * @param expected expected number of values added to the timer
   * @param meterName name for inclusion in the message
   * @param histogram {@link Timer} to get count from
   */
  public static void assertTimerCount(long expected, String timerName, Timer timer) {
    assertEquals(expected, timer.getCount(), "Gauge " + timerName);
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
