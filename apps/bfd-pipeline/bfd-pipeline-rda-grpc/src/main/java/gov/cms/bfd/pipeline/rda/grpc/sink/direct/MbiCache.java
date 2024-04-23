package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.LongStream;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides a mechanism for reliably producing Mbi objects from an MBI string. Two implementations
 * are provided. One simply computes hash value and returns an object with no connection to a
 * database. This is suitable for use in testing. The other looks up values in a database table and
 * inserts a new record when none is found. This is used in AWS envs to ensure that all of the claim
 * objects contain a valid foreign key value referencing the proper MBI record.
 *
 * <p>Values that have been looked up previously are kept in an in-memory LRU cache to avoid
 * excessive lookups in case we encounter the same MBI frequently during a session.
 */
@Slf4j
public abstract class MbiCache {
  /**
   * Increment of time used by {@link DatabaseBacked#waitForRetry} to compute exponential backoff
   * delay when retrying insert of record into MBI cache table. The value is somewhat arbitrary but
   * should be long enough to be meaningful but short enough to not impose excessive wait times.
   */
  private static final int RETRY_INTERVAL_MILLIS = 100;

  /** Used to track metrics for dashboards. */
  @Getter(AccessLevel.PACKAGE)
  protected final Metrics metrics;

  /** In-memory cache used to prevent re-calculation of recently used MBI hashes. */
  protected final Cache<String, Mbi> cache;

  /**
   * Constructs a new instance that computes all hash values on demand.
   *
   * @param metrics {@link Metrics} to use for reporting metrics
   * @param cache {@link Cache} used to manage recent values in memory
   */
  protected MbiCache(Metrics metrics, Cache<String, Mbi> cache) {
    this.metrics = metrics;
    this.cache = cache;
  }

  /**
   * Returns an Mbi object containing an appropriate hash value for the given MBI string.
   *
   * @param mbi the MBI to be hashed
   * @return an {@link Mbi} object with correct hash value
   */
  public final Mbi lookupMbi(String mbi) {
    try {
      metrics.addLookup();
      return cache.get(mbi, () -> computeMbi(mbi));
    } catch (ExecutionException ex) {
      final Throwable cause = ex.getCause();
      log.warn("caught exception while saving generated hash: message={}", cause.getMessage());
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new RuntimeException(cause);
      }
    }
  }

  /**
   * Creates a new instance connected to the specified database. Equivalent to calling {@link
   * #databaseCache} with appropriate parameters. Shares the same in memory cache as this instance
   * but does its database lookup using the provided {@link TransactionManager}.
   *
   * <p>Lifespan of the transactionManager is controlled by the caller. This object never closes the
   * transactionManager.
   *
   * @param transactionManager {@link TransactionManager} used to query and create records
   * @return an instance using a database backed cache as well as in memory cache
   */
  public abstract MbiCache withDatabaseLookup(TransactionManager transactionManager);

  /**
   * Returns an Mbi object containing an appropriate hash value for the given MBI string.
   *
   * @param mbi the MBI to be hashed
   * @return an {@link Mbi} object with correct hash value
   */
  protected abstract Mbi computeMbi(String mbi);

  /**
   * Produces a simple instance that computes the hash value when needed and is not connected to any
   * database. The Mbi objects returned from this must be manually merged into the database before
   * they can be used with a persistent claim object.
   *
   * @param config provides the information needed to configure the hash algorithm and cache
   * @return an MbiCache instance with no database connection and no metrics reporting
   */
  public static MbiCache computedCache(IdHasher.Config config) {
    return computedCache(config, new MetricRegistry());
  }

  /**
   * Produces a simple instance that computes the hash value when needed and is not connected to any
   * database. The Mbi objects returned from this must be manually merged into the database before
   * they can be used with a persistent claim object.
   *
   * @param config provides the information needed to configure the hash algorithm and cache
   * @param appMetrics {@link MetricRegistry} to use for reporting metrics
   * @return an MbiCache instance with no database connection
   */
  public static MbiCache computedCache(IdHasher.Config config, MetricRegistry appMetrics) {
    IdHasher hasher = new IdHasher(config);
    Cache<String, Mbi> cache =
        CacheBuilder.newBuilder().maximumSize(hasher.getConfig().getCacheSize()).build();
    Metrics metrics = new Metrics(appMetrics);
    return new Computed(metrics, cache, hasher);
  }

  /**
   * Produces an instance that ensures that all returned Mbi instances refer to a table that exists
   * in the database. The Mbi objects returned from this have their primary key (mbiId) already set
   * and can be used with a persistent claim object. Without requiring a merge.
   *
   * <p>Lifespan of the transactionManager is controlled by the caller. This object never closes the
   * transactionManager.
   *
   * @param hasher {@link IdHasher} used to compute hash values for raw MBI strings.
   * @param appMetrics {@link MetricRegistry} to use for reporting metrics
   * @param transactionManager {@link TransactionManager} used to query and create records
   * @return an instance with a corresponding record in the database
   */
  public static MbiCache databaseCache(
      IdHasher hasher, MetricRegistry appMetrics, TransactionManager transactionManager) {
    Cache<String, Mbi> cache =
        CacheBuilder.newBuilder().maximumSize(hasher.getConfig().getCacheSize()).build();
    Metrics metrics = new Metrics(appMetrics);
    return new DatabaseBacked(metrics, cache, hasher, new Random(), transactionManager);
  }

  /** Concrete class that computes new hashes on demand. */
  static class Computed extends MbiCache {
    /** Used to compute hash values for raw MBI strings. */
    protected final IdHasher hasher;

    /**
     * Creates a new instance with the specified parameters.
     *
     * @param metrics {@link Metrics} to use for reporting metrics
     * @param cache {@link Cache} used to manage recent values in memory
     * @param hasher {@link IdHasher} used to compute hash values for raw MBI strings.
     */
    Computed(Metrics metrics, Cache<String, Mbi> cache, IdHasher hasher) {
      super(metrics, cache);
      this.hasher = hasher;
    }

    @Override
    public MbiCache withDatabaseLookup(TransactionManager transactionManager) {
      return new DatabaseBacked(metrics, cache, hasher, new Random(), transactionManager);
    }

    @Override
    protected Mbi computeMbi(String mbi) {
      metrics.addMiss();
      metrics.addRetries(0);
      return new Mbi(mbi, hasher.computeIdentifierHash(mbi));
    }
  }

  /**
   * {@link MbiCache} implementation that maintains a table in the database containing previously
   * computed MBI/hash values. Requests to compute an MBI hash first check the database for an
   * existing record and return the value from the record if one is found. Otherwise computes the
   * value and writes it to the database. Multiple threads could encounter conflicts if they attempt
   * to write a record at the same time so retry logic is used in case of an error while writing.
   */
  @VisibleForTesting
  @Slf4j
  static class DatabaseBacked extends MbiCache {
    /** Used to compute hash values for raw MBI strings. */
    protected final IdHasher hasher;

    /** The {@link TransactionManager} used to execute transactions. */
    private final TransactionManager transactionManager;

    /** Creates random values. */
    private final Random random;

    /**
     * Creates a new instance with the specified parameters. The caller is responsible for closing
     * the {@link TransactionManager} when it is no longer needed.
     *
     * @param metrics {@link Metrics} to use for reporting metrics
     * @param cache {@link Cache} used to manage recent values in memory
     * @param hasher {@link IdHasher} used to compute hash values for raw MBI strings.
     * @param random {@link Random} used to generate random wait times during retries
     * @param transactionManager {@link TransactionManager} used to query and create records
     */
    DatabaseBacked(
        Metrics metrics,
        Cache<String, Mbi> cache,
        IdHasher hasher,
        Random random,
        TransactionManager transactionManager) {
      super(metrics, cache);
      this.hasher = hasher;
      this.random = random;
      this.transactionManager = transactionManager;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Look up or write cached MBI database record. In case of database exception re-attempt up
     * to 5 times with a short random interval. Exception is most likely caused by two threads
     * attempting to write the same MBI so the first retry should resolve the issue. If all retries
     * fail we simply return the computed hash value and let the caller try to write the record.
     *
     * @param mbi MBI to be hashed
     * @return MBI hash value
     */
    @VisibleForTesting
    @Override
    protected Mbi computeMbi(String mbi) {
      ReadResult result = new ReadResult(null, false);
      int retryNumber = 0;
      while (result.isEmpty() && retryNumber <= 5) {
        if (retryNumber >= 1) {
          waitForRetry(retryNumber);
        }
        try {
          result = readOrInsertIfMissing(mbi);
        } catch (PersistenceException ex) {
          final Throwable rootCause = Throwables.getRootCause(ex);
          log.debug(
              "caught exception while caching MBI: retry={} class={} message={} causeClass={} causeMessage={}",
              retryNumber,
              ex.getClass().getSimpleName(),
              ex.getMessage(),
              rootCause.getClass().getSimpleName(),
              rootCause.getMessage());
          retryNumber += 1;
        }
      }

      if (result.isInserted()) {
        metrics.addMiss();
      }
      metrics.addRetries(retryNumber);

      if (result.isEmpty()) {
        log.warn("unable to cache MBI after multiple tries, returning computed value");
        return new Mbi(mbi, hasher.computeIdentifierHash(mbi));
      }
      return result.getRecord();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new instance using the specified {@link TransactionManager} but sharing all
     * other resources with this instance.
     *
     * @param transactionManager {@link TransactionManager} used to query and create records
     * @return the cache
     */
    @Override
    public MbiCache withDatabaseLookup(TransactionManager transactionManager) {
      return new DatabaseBacked(metrics, cache, hasher, random, transactionManager);
    }

    /**
     * Look up the value in the database and return its hash if a record is found. If no record is
     * found insert one. Any PersistenceException will be passed through to the caller.
     *
     * @param mbi MBI to look up in the database
     * @return {@link ReadResult} containing the Mbi that is known to exist in the database and a
     *     flag to indicate if the record was inserted by this call
     */
    @VisibleForTesting
    ReadResult readOrInsertIfMissing(String mbi) {
      return transactionManager.executeFunction(
          entityManager -> {
            final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
            final Root<Mbi> root = criteria.from(Mbi.class);
            criteria.select(root).where(builder.equal(root.get(Mbi.Fields.mbi), mbi));
            boolean inserted = false;
            final var records = entityManager.createQuery(criteria).getResultList();
            var record = records.isEmpty() ? null : records.get(0);
            if (record == null) {
              record = entityManager.merge(new Mbi(mbi, hasher.computeIdentifierHash(mbi)));
              inserted = true;
            }
            return new ReadResult(record, inserted);
          });
    }

    /**
     * Wait a random backoff time. Later retries wait for a longer period of time.
     *
     * @param retryNumber identifies which retry attempt we are waiting for
     */
    private void waitForRetry(int retryNumber) {
      var delay = retryNumber * (RETRY_INTERVAL_MILLIS + random.nextInt(RETRY_INTERVAL_MILLIS));
      try {
        log.info("waiting for retry: retryNumber={} delay={}", retryNumber, delay);
        Thread.sleep(delay);
      } catch (InterruptedException ex) {
        // allow the Interrupted exception to flow through to terminate processing
        throw new RuntimeException(ex);
      }
    }
  }

  /** Used to return two output values from {@link DatabaseBacked#readOrInsertIfMissing}. */
  @VisibleForTesting
  @Data
  static class ReadResult {
    /** The record from the database or null if we don't have a record. */
    private final Mbi record;

    /** Flag indicating if the record is one that we have inserted during the call. */
    private final boolean inserted;

    /**
     * Used to test whether or we have a record.
     *
     * @return true if record is not null
     */
    boolean isEmpty() {
      return record == null;
    }
  }

  /** Metrics are tested in unit tests so they need to be easily accessible from tests. */
  @VisibleForTesting
  static class Metrics {
    /** Tracks number of calls to {@link MbiCache#lookupMbi(String)}. */
    private final Meter lookups;

    /**
     * Tracks number of calls to {@link MbiCache#lookupMbi(String)} in which MBI was not present in
     * the cache.
     */
    private final Meter misses;

    /** Tracks number of times database read/write had to be reattempted to arrive at a result. */
    private final Histogram retries;

    /**
     * Creates the metrics.
     *
     * @param appMetrics {@link MetricRegistry} to hold the metrics
     */
    Metrics(MetricRegistry appMetrics) {
      lookups = appMetrics.meter(MetricRegistry.name(MbiCache.class.getSimpleName(), "lookups"));
      misses = appMetrics.meter(MetricRegistry.name(MbiCache.class.getSimpleName(), "misses"));
      retries =
          appMetrics.histogram(MetricRegistry.name(MbiCache.class.getSimpleName(), "retries"));
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
      retries.update(count);
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
      return LongStream.of(retries.getSnapshot().getValues()).sum();
    }
  }
}
