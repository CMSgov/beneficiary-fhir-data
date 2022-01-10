package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.cms.bfd.model.rda.PreAdjMbi;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.NotThreadSafe;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains a cache of MBI/hash values in a separate table within the database. Requests to compute
 * an MBI hash first check the database for an existing record and return the value from the record
 * if one is found. Otherwise computes the value and writes it to the database. Multiple threads
 * could encounter conflicts if they attempt to write a record at the same time so retry logic is
 * used in case of an error while writing.
 *
 * <p>Values that have been looked up previously are kept in an in-memory cache to avoid excessive
 * lookups in case we encounter the same MBI frequently during a session.
 */
@NotThreadSafe
public class DatabaseMbiHasher extends IdHasher implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMbiHasher.class);

  private final EntityManager entityManager;
  private final Cache<String, String> cache;
  private final Random random;

  public DatabaseMbiHasher(Config config, EntityManager entityManager) {
    super(config);
    this.entityManager = entityManager;
    cache = CacheBuilder.newBuilder().maximumSize(config.getCacheSize()).build();
    random = new Random();
  }

  @Override
  public String computeIdentifierHash(String identifier) {
    try {
      return cache.get(identifier, () -> lookupWithRetries(identifier));
    } catch (ExecutionException ex) {
      final var hash = super.computeIdentifierHash(identifier);
      LOGGER.warn(
          "caught exception while saving generated hash: hash={} message={}",
          hash,
          ex.getCause().getMessage());
      return super.computeIdentifierHash(identifier);
    }
  }

  @Override
  public void close() throws Exception {
    entityManager.close();
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
  String lookupWithRetries(String mbi) {
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
    return super.computeIdentifierHash(mbi);
  }

  /**
   * Look up the value in the database and return its hash if a record is found. If no record is
   * found insert one. Any PersistenceException will be passed through to the caller.
   *
   * @param mbi MBI to look up in the database
   * @return the MBI hash value
   */
  @VisibleForTesting
  String readOrInsertIfMissing(String mbi) {
    entityManager.getTransaction().begin();
    PreAdjMbi record = entityManager.find(PreAdjMbi.class, mbi);
    if (record == null) {
      record = new PreAdjMbi(mbi, super.computeIdentifierHash(mbi));
      entityManager.merge(record);
    }
    entityManager.getTransaction().commit();
    return record.getMbiHash();
  }

  /**
   * Wait a random backoff time. Later retries wait for a longer period of time.
   *
   * @param retryNumber identifies which retry attempt we are waiting for
   */
  @VisibleForTesting
  void waitForRetry(int retryNumber) {
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
