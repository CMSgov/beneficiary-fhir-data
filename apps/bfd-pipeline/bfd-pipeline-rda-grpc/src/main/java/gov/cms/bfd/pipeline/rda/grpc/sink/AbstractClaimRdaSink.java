package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.time.Clock;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RdaSink implementation that writes TClaim objects to the database in batches.
 *
 * @param <TClaim> type of entity objects written to the database
 */
abstract class AbstractClaimRdaSink<TClaim> implements RdaSink<RdaChange<TClaim>> {
  protected final EntityManager entityManager;
  protected final Metrics metrics;
  protected final Clock clock;
  protected final Logger logger;

  protected AbstractClaimRdaSink(PipelineApplicationState appState) {
    entityManager = appState.getEntityManagerFactory().createEntityManager();
    metrics = new Metrics(getClass(), appState.getMetrics());
    clock = appState.getClock();
    logger = LoggerFactory.getLogger(getClass());
  }

  @Override
  public void close() throws Exception {
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
    }
  }

  public int writeBatch(Collection<RdaChange<TClaim>> claims) throws ProcessingException {
    final long maxSeq = maxSequenceInBatch(claims);
    try {
      metrics.calls.mark();
      try {
        updateLatencyMetric(claims);
        persistBatch(claims);
        metrics.objectsPersisted.mark(claims.size());
        metrics.setLatestSequenceNumber(maxSeq);
        logger.info(
            "writeBatch succeeded using persist: size={} maxSeq={} ", claims.size(), maxSeq);
      } catch (Throwable error) {
        if (isDuplicateKeyException(error)) {
          logger.info(
              "writeBatch switching to merge due to duplicate key exception: size={} maxSeq={}",
              claims.size(),
              maxSeq);
          mergeBatch(claims);
          metrics.objectsMerged.mark(claims.size());
          metrics.setLatestSequenceNumber(maxSeq);
          logger.info(
              "writeBatch succeeded using merge: size={} maxSeq={} ", claims.size(), maxSeq);
        } else {
          throw error;
        }
      }
    } catch (Exception error) {
      logger.error(
          "writeBatch failed: size={} maxSeq={} error={}",
          claims.size(),
          maxSeq,
          error.getMessage(),
          error);
      metrics.failures.mark();
      throw new ProcessingException(error, 0);
    }
    metrics.successes.mark();
    metrics.objectsWritten.mark(claims.size());
    return claims.size();
  }

  @VisibleForTesting
  Metrics getMetrics() {
    return metrics;
  }

  private void persistBatch(Iterable<RdaChange<TClaim>> changes) {
    boolean commit = false;
    try {
      entityManager.getTransaction().begin();
      for (RdaChange<TClaim> change : changes) {
        switch (change.getType()) {
          case INSERT:
            entityManager.persist(change.getClaim());
            break;
          case UPDATE:
            entityManager.merge(change.getClaim());
            break;
          case DELETE:
            // TODO: [DCGEO-131] accept DELETE changes from RDA API
            throw new IllegalArgumentException(
                "RDA API DELETE changes are not currently supported");
        }
      }
      commit = true;
    } finally {
      if (commit) {
        entityManager.getTransaction().commit();
      } else {
        entityManager.getTransaction().rollback();
      }
      entityManager.clear();
    }
  }

  private void mergeBatch(Iterable<RdaChange<TClaim>> changes) {
    boolean commit = false;
    try {
      entityManager.getTransaction().begin();
      for (RdaChange<TClaim> change : changes) {
        if (change.getType() != RdaChange.Type.DELETE) {
          entityManager.merge(change.getClaim());
        } else {
          // TODO: [DCGEO-131] accept DELETE changes from RDA API
          throw new IllegalArgumentException("RDA API DELETE changes are not currently supported");
        }
      }
      commit = true;
    } finally {
      if (commit) {
        entityManager.getTransaction().commit();
      } else {
        entityManager.getTransaction().rollback();
      }
      entityManager.clear();
    }
  }

  private long maxSequenceInBatch(Collection<RdaChange<TClaim>> claims) {
    OptionalLong value = claims.stream().mapToLong(RdaChange::getSequenceNumber).max();
    if (!value.isPresent()) {
      // This should never happen! But if it does, we'll shout about it rather than throw an
      // exception
      logger.warn("processed an empty batch!");
    }
    return value.orElse(0L);
  }

  /**
   * Used by sub-classes to read the highest known sequenceNumber for claims in the database.
   *
   * @param query JPAQL query string
   * @return result of the query or empty if no records matched
   */
  protected Optional<Long> readMaxExistingSequenceNumber(String query) throws ProcessingException {
    try {
      logger.info("running query to find max sequence number");
      Long sequenceNumber = entityManager.createQuery(query, Long.class).getSingleResult();
      final Optional<Long> answer = Optional.ofNullable(sequenceNumber);
      logger.info(
          "max sequence number result is {}", answer.map(n -> Long.toString(n)).orElse("none"));
      return answer;
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }

  private void updateLatencyMetric(Collection<RdaChange<TClaim>> claims) {
    for (RdaChange<TClaim> claim : claims) {
      final long nowMillis = clock.millis();
      final long changeMillis = claim.getTimestamp().toEpochMilli();
      final long age = Math.max(0L, nowMillis - changeMillis);
      metrics.changeAgeMillis.update(age);
    }
  }

  @VisibleForTesting
  static boolean isDuplicateKeyException(Throwable error) {
    while (error != null) {
      if (error instanceof EntityExistsException) {
        return true;
      }
      final String errorMessage = Strings.nullToEmpty(error.getMessage()).toLowerCase();
      if (errorMessage.contains("already exists") || errorMessage.contains("duplicate key")) {
        return true;
      }
      error = error.getCause() == error ? null : error.getCause();
    }
    return false;
  }

  /**
   * Metrics are tested in unit tests so they need to be easily accessible from tests. Also this
   * class is used to write both MCS and FISS claims so the metric names need to include a claim
   * type to distinguish them.
   */
  @Getter
  @VisibleForTesting
  static class Metrics {
    /** Number of times the sink has been called to store objects. */
    private final Meter calls;
    /** Number of calls that successfully stored the objects. */
    private final Meter successes;
    /** Number of calls that failed to store the objects. */
    private final Meter failures;
    /** Number of objects written. */
    private final Meter objectsWritten;
    /** Number of objects stored using <code>persist()</code>. */
    private final Meter objectsPersisted;
    /** Number of objects stored using <code>merge()</code> */
    private final Meter objectsMerged;
    /** Milliseconds between change timestamp and current time. */
    private final Histogram changeAgeMillis;
    /** Latest sequnce number from writing a batch. * */
    private final Gauge<?> latestSequenceNumber;
    /** The value returned by the latestSequenceNumber gauge. * */
    private final AtomicLong latestSequenceNumberValue = new AtomicLong(0L);

    private Metrics(Class<?> klass, MetricRegistry appMetrics) {
      final String base = klass.getSimpleName();
      calls = appMetrics.meter(MetricRegistry.name(base, "calls"));
      successes = appMetrics.meter(MetricRegistry.name(base, "successes"));
      failures = appMetrics.meter(MetricRegistry.name(base, "failures"));
      objectsWritten = appMetrics.meter(MetricRegistry.name(base, "writes", "total"));
      objectsPersisted = appMetrics.meter(MetricRegistry.name(base, "writes", "persisted"));
      objectsMerged = appMetrics.meter(MetricRegistry.name(base, "writes", "merged"));
      changeAgeMillis =
          appMetrics.histogram(MetricRegistry.name(base, "change", "latency", "millis"));
      latestSequenceNumber =
          appMetrics.gauge(
              MetricRegistry.name(base, "lastSeq"), () -> latestSequenceNumberValue::longValue);
    }

    private void setLatestSequenceNumber(long value) {
      latestSequenceNumberValue.set(value);
    }
  }
}
