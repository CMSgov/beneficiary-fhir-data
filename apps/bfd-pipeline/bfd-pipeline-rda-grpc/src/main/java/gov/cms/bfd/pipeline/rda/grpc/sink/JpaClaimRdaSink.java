package gov.cms.bfd.pipeline.rda.grpc.sink;

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
public class JpaClaimRdaSink<TClaim> implements RdaSink<RdaChange<TClaim>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JpaClaimRdaSink.class);

  private final EntityManager entityManager;
  private final Metrics metrics;
  private final Clock clock;

  public JpaClaimRdaSink(String claimType, PipelineApplicationState appState, Clock clock) {
    entityManager = appState.getEntityManagerFactory().createEntityManager();
    metrics = new Metrics(appState.getMetrics(), claimType);
    this.clock = clock;
  }

  @Override
  public void close() throws Exception {
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
    }
  }

  public int writeBatch(Collection<RdaChange<TClaim>> claims) throws ProcessingException {
    try {
      metrics.calls.mark();
      try {
        updateLatencyMetric(claims);
        persistBatch(claims);
        metrics.objectsPersisted.mark(claims.size());
        LOGGER.info("wrote batch of {} claims using persist()", claims.size());
      } catch (Throwable error) {
        if (isDuplicateKeyException(error)) {
          LOGGER.info(
              "caught duplicate key exception: switching to merge for batch of {} claims",
              claims.size());
          mergeBatch(claims);
          metrics.objectsMerged.mark(claims.size());
          LOGGER.info("wrote batch of {} claims using merge()", claims.size());
        } else {
          throw error;
        }
      }
    } catch (Exception error) {
      LOGGER.error("writeBatch failure: error={}", error.getMessage(), error);
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

    private Metrics(MetricRegistry appMetrics, String claimType) {
      final String base = MetricRegistry.name(JpaClaimRdaSink.class.getSimpleName(), claimType);
      calls = appMetrics.meter(MetricRegistry.name(base, "calls"));
      successes = appMetrics.meter(MetricRegistry.name(base, "successes"));
      failures = appMetrics.meter(MetricRegistry.name(base, "failures"));
      objectsWritten = appMetrics.meter(MetricRegistry.name(base, "writes", "total"));
      objectsPersisted = appMetrics.meter(MetricRegistry.name(base, "writes", "persisted"));
      objectsMerged = appMetrics.meter(MetricRegistry.name(base, "writes", "merged"));
      changeAgeMillis =
          appMetrics.histogram(MetricRegistry.name(base, "change", "latency", "millis"));
    }
  }
}
