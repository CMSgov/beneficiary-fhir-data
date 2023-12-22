package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/** Tests the {@link RdaPipelineTestUtils}. */
public class RdaPipelineTestUtils {
  /**
   * Verifies the expected value of a {@link Counter} with a meaningful message in case of a
   * mismatch.
   *
   * @param expected expected reading
   * @param meterName name for inclusion in the message
   * @param meter {@link Counter} to get value from
   */
  public static void assertMeterReading(long expected, String meterName, Counter meter) {
    assertEquals(expected, meter.count(), "Meter " + meterName);
  }

  /**
   * Verifies the expected value of a {@link DistributionSummary} with a meaningful message in case
   * of a mismatch.
   *
   * @param expected expected reading
   * @param histogramName name for inclusion in the message
   * @param histogram {@link DistributionSummary} to get value from
   */
  public static void assertHistogramReading(
      long expected, String histogramName, DistributionSummary histogram) {
    double total = histogram.totalAmount();
    assertEquals(expected, (long) total, "Histogram " + histogramName);
  }

  /**
   * Verifies the expected value of a {@link AtomicLong} with a meaningful message in case of a
   * mismatch.
   *
   * @param expected expected reading
   * @param gaugeName name for inclusion in the message
   * @param gauge {@link AtomicLong} to get value from
   */
  public static void assertGaugeReading(long expected, String gaugeName, AtomicLong gauge) {
    assertEquals(expected, gauge.get(), "Gauge " + gaugeName);
  }

  /**
   * Verifies the expected number of values have been written to a {@link Timer} with a meaningful
   * message in case of a mismatch.
   *
   * @param expected expected number of values added to the timer
   * @param timerName name for inclusion in the message
   * @param timer {@link Timer} to get count from
   */
  public static void assertTimerCount(long expected, String timerName, Timer timer) {
    assertEquals(expected, timer.count(), "Gauge " + timerName);
  }

  /**
   * Creates a containerized postgres DB that is destroyed when the test ends plus a {@link
   * PipelineApplicationState} and {@link TransactionManager} using that db, passes them to the
   * provided lambda function, then closes them and destroys the database.
   *
   * @param clock used for the app state
   * @param test lambda to receive the appState and perform some testing
   * @throws Exception pass through from test
   */
  public static void runTestWithTemporaryDb(Clock clock, DatabaseConsumer test) throws Exception {

    final MetricRegistry appMetrics = new MetricRegistry();
    final HikariDataSource dataSource =
        DatabaseTestUtils.createPooledTestContainerDataSourceForTestContainerWithPostgres(
            10, appMetrics);
    try (PipelineApplicationState appState =
            new PipelineApplicationState(
                new SimpleMeterRegistry(),
                appMetrics,
                dataSource,
                RDA_PERSISTENCE_UNIT_NAME,
                clock);
        TransactionManager transactionManager =
            new TransactionManager(appState.getEntityManagerFactory())) {
      test.accept(appState, transactionManager);
    }
  }

  /**
   * Looks for a record in the MbiCache table using the given EntityManager.
   *
   * @param transactionManager used to perform the query
   * @param mbi mbi string to look for
   * @return null if not cached otherwise the Mbi record from database
   */
  public static Mbi lookupCachedMbi(TransactionManager transactionManager, String mbi) {
    return transactionManager.executeFunction(
        entityManager -> {
          final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
          final CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
          final Root<Mbi> root = criteria.from(Mbi.class);
          criteria.select(root).where(builder.equal(root.get(Mbi.Fields.mbi), mbi));
          final var records = entityManager.createQuery(criteria).getResultList();
          return records.isEmpty() ? null : records.get(0);
        });
  }

  /** An interface for a test database. */
  @FunctionalInterface
  public interface DatabaseConsumer {
    /**
     * Accepts parameters for the consumer.
     *
     * @param appState the app state
     * @param transactionManager the entity manager
     * @throws Exception any exception setting up the consumer
     */
    void accept(PipelineApplicationState appState, TransactionManager transactionManager)
        throws Exception;
  }
}
