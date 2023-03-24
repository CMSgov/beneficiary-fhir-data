package gov.cms.bfd.pipeline.ccw.rif.load;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import gov.cms.bfd.model.rif.IdHash;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Wraps an {@link IdHasher} and provides support for caching recently computed hashes in memory as
 * well as permanently caching them in a database.
 */
public class DatabaseIdHasher {
  /** Query used to find existing hash record. */
  @VisibleForTesting
  static final String QueryString = "select h.hash from IdHash h where h.id = :id";

  /** Query parameter name for identifier. */
  @VisibleForTesting static final String IdParamName = "id";

  /**
   * Maximum number of retry attempts the cache will make when trying to a record to the database if
   * constraint violations are detected. Should not be necessary since the cache will prevent
   * multiple threads in this processing trying to insert the same hash simultaneously but added in
   * case some other process is doing so.
   */
  private static final int MaxRetries = 2;

  /** Used to create {@link EntityManager}s. */
  private final EntityManagerFactory entityManagerFactory;
  /** Used to compute hash values. */
  private final IdHasher idHasher;
  /** Used to track metrics for dashboards. */
  @Getter(AccessLevel.PACKAGE)
  private final Metrics metrics;
  /**
   * Used to hold recently used hash values in memory as well as to manage when to insert a record.
   */
  private final Cache<String, String> cache;

  /**
   * Creates an instance with the given maximum size and using the provided {@link IdHasher} to
   * compute values that are not present in the database.
   *
   * @param appMetrics {@link MetricRegistry} to use for reporting metrics
   * @param entityManagerFactory used to create {@link EntityManager}s
   * @param idHasher {@link IdHasher} used when computing is necessary
   * @param maxCacheSize maximum number of hashes to cache in memory cache
   */
  public DatabaseIdHasher(
      MetricRegistry appMetrics,
      EntityManagerFactory entityManagerFactory,
      IdHasher idHasher,
      int maxCacheSize) {
    this.entityManagerFactory = entityManagerFactory;
    this.idHasher = idHasher;
    metrics = new Metrics(appMetrics);
    cache = CacheBuilder.newBuilder().maximumSize(maxCacheSize).build();
  }

  /**
   * Computes a one-way cryptographic hash of the specified ID value. If the hash has previously
   * been computed and is still in the cache it will be returned immediately. Otherwise it will
   * retrieved from the database. If it exists in the database that value will be returned.
   * Otherwise the hash will be be computed and added to the cache and written to the database in
   * addition to returning it.
   *
   * <p>If multiple threads call this with the same key the first will perform the computation and
   * database update and others will wait for that operation to complete then they will return the
   * value from the first thread.
   *
   * <p>The database operations take place within the provided {@link EntityManager}s current
   * transaction.
   *
   * @param identifier any ID to be hashed
   * @return a one-way cryptographic hash of the specified ID value, exactly 64 characters long
   */
  public String computeIdentifierHash(String identifier) {
    try {
      metrics.addLookup();
      return cache.get(identifier, () -> getOrInsertIdentifierHash(identifier));
    } catch (UncheckedExecutionException | ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new RuntimeException(cause);
      }
    }
  }

  /**
   * Query the database for a match and return it if found. Otherwise compute the value and insert
   * it into the database then return it. Retries the insert if any constraint violations are
   * detected.
   *
   * @param identifier any ID to be hashed
   * @return a one-way cryptographic hash of the specified ID value, exactly 64 characters long
   */
  private String getOrInsertIdentifierHash(String identifier) {
    try (var transactionManager = new TransactionManager(entityManagerFactory)) {
      TransactionManager.RetryResult<ReadResult> result =
          transactionManager.executeFunctionWithRetries(
              MaxRetries,
              TransactionManager::isConstraintViolation,
              entityManager -> getOrInsertIdentifierHash(entityManager, identifier));
      if (result.getNumRetries() > 0) {
        metrics.addRetries(result.getNumRetries());
      }
      if (result.getValue().isInserted()) {
        metrics.addMiss();
      }
      return result.getValue().getHash();
    }
  }

  /**
   * Query the database for a match and return it if found. Otherwise compute the value and write it
   * to the database then return it.
   *
   * @param entityManager the {@link EntityManager} used for database query and insert
   * @param identifier any ID to be hashed
   * @return a one-way cryptographic hash of the specified ID value, exactly 64 characters long
   */
  private ReadResult getOrInsertIdentifierHash(EntityManager entityManager, String identifier) {
    List<String> records =
        entityManager
            .createQuery(QueryString, String.class)
            .setParameter(IdParamName, identifier)
            .setMaxResults(1)
            .getResultList();
    if (records.size() > 0) {
      return new ReadResult(records.get(0), false);
    }

    final String hash = idHasher.computeIdentifierHash(identifier);
    entityManager.persist(new IdHash(identifier, hash));
    return new ReadResult(hash, true);
  }

  /** Used to return two output values from {@link #getOrInsertIdentifierHash}. */
  @VisibleForTesting
  @AllArgsConstructor
  @Getter
  static class ReadResult {
    /** The hash value. */
    private final String hash;
    /** Flag indicating if the record is one that we have inserted during the call. */
    private final boolean inserted;
  }

  /** Metrics are tested in unit tests so they need to be easily accessible from tests. */
  @VisibleForTesting
  static class Metrics {
    /** Tracks number of calls to {@link DatabaseIdHasher#computeIdentifierHash}. */
    private final Meter lookups;
    /**
     * Tracks number of calls to {@link DatabaseIdHasher#computeIdentifierHash} in which identifier
     * was not present in the cache.
     */
    private final Meter misses;
    /** Tracks number of times database read/write had to be reattempted to arrive at a result. */
    private final Meter retries;

    /**
     * Creates the metrics.
     *
     * @param appMetrics {@link MetricRegistry} to hold the metrics
     */
    Metrics(MetricRegistry appMetrics) {
      lookups =
          appMetrics.meter(MetricRegistry.name(DatabaseIdHasher.class.getSimpleName(), "lookups"));
      misses =
          appMetrics.meter(MetricRegistry.name(DatabaseIdHasher.class.getSimpleName(), "misses"));
      retries =
          appMetrics.meter(MetricRegistry.name(DatabaseIdHasher.class.getSimpleName(), "retries"));
    }

    /** Increment number of lookups metric. */
    void addLookup() {
      lookups.mark();
    }

    /** Increment number of misses metric. */
    void addMiss() {
      misses.mark();
    }

    /**
     * Add number of retries value to retries metric.
     *
     * @param count the number of retries
     */
    void addRetries(int count) {
      retries.mark(count);
    }

    /**
     * Get current lookups metric value.
     *
     * @return current lookups metric value.
     */
    long getLookups() {
      return lookups.getCount();
    }

    /**
     * Get current misses metric value.
     *
     * @return current misses metric value.
     */
    long getMisses() {
      return misses.getCount();
    }

    /**
     * Get total number of retries.
     *
     * @return total number of retries.
     */
    long getTotalRetries() {
      return retries.getCount();
    }
  }
}
