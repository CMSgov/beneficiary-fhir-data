package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.DataSourceComponents;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissPayer;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/** Tests the {@link RdaPipelineTestUtils}. */
public class RdaPipelineTestUtils {
  /**
   * List of all RDA tables that could be updated by a test. Used by {@link #truncateRdaTables} to
   * truncate all tables between tests.
   */
  private static final List<Class<?>> RDA_ENTITY_CLASSES =
      List.of(
          RdaFissRevenueLine.class,
          RdaFissPayer.class,
          RdaFissDiagnosisCode.class,
          RdaFissProcCode.class,
          RdaFissClaim.class,
          RdaMcsDetail.class,
          RdaMcsDiagnosisCode.class,
          RdaMcsClaim.class,
          RdaClaimMessageMetaData.class,
          RdaApiProgress.class,
          MessageError.class,
          Mbi.class);

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
   * Creates a {@link HikariDataSource}, {@link PipelineApplicationState} and {@link
   * TransactionManager} using the shared containerized postgres DB, passes them to the provided
   * lambda function, then truncates all of the RDA tables and closes the {@link HikariDataSource}.
   *
   * @param clock used for the app state
   * @param test lambda to receive the appState and perform some testing
   * @throws Exception pass through from test
   */
  public static void runTestWithTemporaryDb(Clock clock, DatabaseConsumer test) throws Exception {
    final var appMetrics = new MetricRegistry();
    final var rawDataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    final var dataSourceComponents = new DataSourceComponents(rawDataSource);
    final var dbOptions =
        DatabaseOptions.builder()
            .databaseUrl(dataSourceComponents.getUrl())
            .databaseUsername(dataSourceComponents.getUsername())
            .databasePassword(dataSourceComponents.getPassword())
            .maxPoolSize(10)
            .build();
    try (HikariDataSource dataSource = new HikariDataSourceFactory(dbOptions).createDataSource();
        PipelineApplicationState appState =
            new PipelineApplicationState(
                new SimpleMeterRegistry(),
                appMetrics,
                dataSource,
                RDA_PERSISTENCE_UNIT_NAME,
                clock);
        TransactionManager transactionManager =
            new TransactionManager(appState.getEntityManagerFactory())) {
      try {
        test.accept(appState, transactionManager);
      } finally {
        truncateRdaTables(transactionManager);
      }
    }
  }

  /**
   * Truncates all RDA related tables using the provided {@link TransactionManager} so that they
   * will be empty for the next test when it runs.
   *
   * @param transactionManager used to execute deletes.
   */
  private static void truncateRdaTables(TransactionManager transactionManager) {
    for (Class<?> entityClass : RDA_ENTITY_CLASSES) {
      final String deleteStatement = "delete from " + entityClass.getSimpleName() + " f";
      transactionManager.executeProcedure(em -> em.createQuery(deleteStatement).executeUpdate());
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
