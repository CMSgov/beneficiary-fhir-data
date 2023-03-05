package gov.cms.bfd.pipeline.ccw.rif.load;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import gov.cms.bfd.model.rif.IdHash;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.persistence.EntityManager;

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

  /** Used to compute hash values. */
  private final IdHasher hasher;
  /**
   * Used to hold recently used hash values in memory as well as to manage when to insert a record.
   */
  private final Cache<String, String> cache;

  /**
   * Creates an instance with the given maximum size and using the provided {@link IdHasher} to
   * compute values that are not present in the database.
   *
   * @param idHasher {@link IdHasher} used when computing is necessary
   * @param maxCacheSize maximum number of hashes to cache in memory cache
   */
  public DatabaseIdHasher(IdHasher idHasher, int maxCacheSize) {
    this.hasher = idHasher;
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
   * @param entityManager the {@link EntityManager} used for database query and insert
   * @param identifier any ID to be hashed
   * @return a one-way cryptographic hash of the specified ID value, exactly 64 characters long
   */
  public String computeIdentifierHash(EntityManager entityManager, String identifier) {
    try {
      return cache.get(identifier, () -> getOrInsertIdentifierHash(entityManager, identifier));
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
   * Query the database for a match and return it if found. Otherwise compute the value and write it
   * to the database then return it.
   *
   * @param entityManager the {@link EntityManager} used for database query and insert
   * @param identifier any ID to be hashed
   * @return a one-way cryptographic hash of the specified ID value, exactly 64 characters long
   */
  private String getOrInsertIdentifierHash(EntityManager entityManager, String identifier) {
    List<String> records =
        entityManager
            .createQuery(QueryString, String.class)
            .setParameter(IdParamName, identifier)
            .setMaxResults(1)
            .getResultList();
    if (records.size() > 0) {
      return records.get(0);
    }

    final String hash = hasher.computeIdentifierHash(identifier);
    entityManager.persist(new IdHash(identifier, hash));
    return hash;
  }
}
