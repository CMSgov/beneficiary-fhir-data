package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.MbiUtil;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MbiCache {
  private static final Logger LOGGER = LoggerFactory.getLogger(MbiCache.class);

  private final LookupFunction lookupFunction;
  private final Cache<String, Mbi> cache;
  private final IdHasher hasher;

  @VisibleForTesting
  MbiCache(LookupFunction lookupFunction, IdHasher hasher) {
    this.lookupFunction = lookupFunction;
    this.hasher = hasher;
    cache = CacheBuilder.newBuilder().maximumSize(hasher.getConfig().getCacheSize()).build();
  }

  /**
   * Produces a simple instance that computes the hash value when needed and is not connected to any
   * database. The Mbi objects returned from this must be manually merged into the database before
   * they can be used with a persistent claim object.
   *
   * @param config provides the information needed to configure the hash algorithm and cache
   * @return an MbiCache instance with no database connection
   */
  public static MbiCache computedCache(IdHasher.Config config) {
    var hasher = new IdHasher(config);
    var lookupFunction = new ComputedLookupFunction(hasher);
    return new MbiCache(lookupFunction, hasher);
  }

  /**
   * Produces an instance that ensures that all returned Mbi instances refer to a table that exists
   * in the database. The Mbi objects returned from this have their primary key (mbiId) already set
   * and can be used with a persistent claim object. Without requiring a merge.
   *
   * <p>Lifespan of the entityManager is controlled by the caller. This object never closes the
   * entityManager.
   *
   * @param entityManager used to query and create records
   * @param hasher the hasher
   * @return an MbiCache instance with a corresponding record in the database
   */
  public static MbiCache databaseCache(EntityManager entityManager, IdHasher hasher) {
    var lookupFunction = new DatabaseLookupFunction(entityManager, hasher);
    return new MbiCache(lookupFunction, hasher);
  }

  /**
   * Returns an Mbi object containing an appropriate hash value for the given MBI string.
   *
   * @param mbi the MBI to be hashed
   * @return an Mbi object with correct hash value
   */
  public Mbi lookupMbi(String mbi) {
    try {
      return cache.get(mbi, () -> lookupFunction.lookupMbi(mbi));
    } catch (ExecutionException ex) {
      final var hash = hasher.computeIdentifierHash(mbi);
      LOGGER.warn(
          "caught exception while saving generated hash: hash={} message={}",
          hash,
          ex.getCause().getMessage());
      return MbiUtil.newMbi(mbi, hasher.computeIdentifierHash(mbi));
    }
  }

  /**
   * Creates a new instance connected to the specified database. Equivalent to calling {@code
   * databaseCache()} with appropriate parameters.
   *
   * <p>Lifespan of the entityManager is controlled by the caller. This object never closes the
   * entityManager.
   *
   * @param entityManager used to query and create records
   * @return an MbiCache instance with a corresponding record in the database
   */
  public MbiCache withDatabaseLookup(EntityManager entityManager) {
    return databaseCache(entityManager, hasher);
  }

  /**
   * Implementations of this interface perform the computation and/or I/O necessary to produce an
   * Mbi object from an MBI string.
   */
  public interface LookupFunction {
    Mbi lookupMbi(String mbi);
  }

  private static class ComputedLookupFunction implements LookupFunction {
    private final IdHasher hasher;

    private ComputedLookupFunction(IdHasher hasher) {
      this.hasher = hasher;
    }

    @Override
    public Mbi lookupMbi(String mbi) {
      return MbiUtil.newMbi(mbi, hasher.computeIdentifierHash(mbi));
    }
  }

  /**
   * Maintains a cache of MBI/hash values in a separate table within the database. Requests to
   * compute an MBI hash first check the database for an existing record and return the value from
   * the record if one is found. Otherwise computes the value and writes it to the database.
   * Multiple threads could encounter conflicts if they attempt to write a record at the same time
   * so retry logic is used in case of an error while writing.
   *
   * <p>Values that have been looked up previously are kept in an in-memory cache to avoid excessive
   * lookups in case we encounter the same MBI frequently during a session.
   */
  @VisibleForTesting
  static class DatabaseLookupFunction implements LookupFunction {
    private final EntityManager entityManager;
    private final IdHasher hasher;
    private final Random random;

    @VisibleForTesting
    DatabaseLookupFunction(EntityManager entityManager, IdHasher hasher) {
      this.entityManager = entityManager;
      this.hasher = hasher;
      random = new Random();
    }

    /**
     * Look up or write cached MBI database record. In case of database exception re-attempt up to 5
     * times with a short random interval. Exception is most likely caused by two threads attempting
     * to write the same MBI so the first retry should resolve the issue. If all retries fail we
     * simply return the computed hash value and let the caller try to write the record.
     *
     * @param mbi MBI to be hashed
     * @return MBI hash value
     */
    @VisibleForTesting
    @Override
    public Mbi lookupMbi(String mbi) {
      int retryNumber = 0;
      while (retryNumber <= 5) {
        if (retryNumber >= 1) {
          waitForRetry(retryNumber);
        }
        try {
          return readOrInsertIfMissing(mbi);
        } catch (PersistenceException ex) {
          final Throwable rootCause = Throwables.getRootCause(ex);
          LOGGER.warn(
              "caught exception while caching MBI: retry={} class={} message={} causeClass={} causeMessage={}",
              retryNumber,
              ex.getClass().getSimpleName(),
              ex.getMessage(),
              rootCause.getClass().getSimpleName(),
              rootCause.getMessage());
          retryNumber += 1;
        }
      }
      LOGGER.warn("unable to cache MBI after multiple tries, returning computed value");
      return MbiUtil.newMbi(mbi, hasher.computeIdentifierHash(mbi));
    }

    /**
     * Look up the value in the database and return its hash if a record is found. If no record is
     * found insert one. Any PersistenceException will be passed through to the caller.
     *
     * @param mbi MBI to look up in the database
     * @return the Mbi that is known to exist in the entityManager
     */
    @VisibleForTesting
    Mbi readOrInsertIfMissing(String mbi) {
      entityManager.getTransaction().begin();
      boolean successful = false;
      try {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
        final Root<Mbi> root = criteria.from(Mbi.class);
        criteria.select(root).where(builder.equal(root.get(Mbi.Fields.mbi), mbi));
        final var records = entityManager.createQuery(criteria).getResultList();
        var record = records.isEmpty() ? null : records.get(0);
        if (record == null) {
          record = entityManager.merge(MbiUtil.newMbi(mbi, hasher.computeIdentifierHash(mbi)));
        }
        successful = true;
        return record;
      } finally {
        if (successful) {
          entityManager.getTransaction().commit();
        } else {
          entityManager.getTransaction().rollback();
        }
      }
    }

    /**
     * Wait a random backoff time. Later retries wait for a longer period of time.
     *
     * @param retryNumber identifies which retry attempt we are waiting for
     */
    private void waitForRetry(int retryNumber) {
      var delay = retryNumber * (50L + random.nextInt(50));
      try {
        LOGGER.warn("waiting for retry: retryNumber={} delay={}", retryNumber, delay);
        Thread.sleep(delay);
      } catch (InterruptedException ex) {
        // allow the Interrupted exception to flow through to terminate processing
        throw new RuntimeException(ex);
      }
    }
  }
}
