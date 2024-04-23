package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration test for the {@link MbiCache}. */
public class MbiCacheIT {
  /** Configuration for the object used for hashing values. */
  private final IdHasher.Config hashConfig =
      IdHasher.Config.builder()
          .hashIterations(1)
          .hashPepperString("just-a-test")
          .cacheSize(3)
          .build();

  /** Used for hashing in tests. */
  private final IdHasher normalHasher = new IdHasher(hashConfig);

  /** Test mbi 1. */
  private final String mbi1 = "1";

  /** Test mbi hash 1. */
  private final String hash1 = normalHasher.computeIdentifierHash(mbi1);

  /** Test mbi 2. */
  private final String mbi2 = "2";

  /** Test mbi hash 2. */
  private final String hash2 = normalHasher.computeIdentifierHash(mbi2);

  /** Test mbi 3. */
  private final String mbi3 = "3";

  /** Test mbi hash 3. */
  private final String hash3 = normalHasher.computeIdentifierHash(mbi3);

  /** Test mbi 4. */
  private final String mbi4 = "4";

  /** Test mbi hash 4. */
  private final String hash4 = normalHasher.computeIdentifierHash(mbi4);

  /** The test metric object. */
  private MetricRegistry appMetrics;

  /** Sets the test parameters up each test. */
  @BeforeEach
  void setUp() {
    appMetrics = new MetricRegistry();
  }

  /** Verifies that the computed cache updates the metrics when lookups are made against it. */
  @Test
  public void computedCacheUpdatesMetrics() {
    MbiCache mbiCache = MbiCache.computedCache(hashConfig);
    assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
    assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());

    assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
    assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());

    assertEquals(4, mbiCache.getMetrics().getLookups());
    assertEquals(2, mbiCache.getMetrics().getMisses());
    assertEquals(0, mbiCache.getMetrics().getTotalRetries());
  }

  /**
   * Verifies that when the cache gets multiple requests for records that are not in the cache, they
   * is added to the cache and proper metrics are recorded for the cache misses.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void createsNewRecordWhenNoneExists() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final MbiCache mbiCache =
              MbiCache.databaseCache(normalHasher, appMetrics, transactionManager);
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());

          Mbi databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(transactionManager, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash1, databaseMbiEntity.getHash());

          databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(transactionManager, mbi2);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash2, databaseMbiEntity.getHash());

          assertEquals(2, mbiCache.getMetrics().getLookups());
          assertEquals(2, mbiCache.getMetrics().getMisses());
          assertEquals(0, mbiCache.getMetrics().getTotalRetries());
        });
  }

  /**
   * Verifies that when the cache is hit, it uses the saved hash value instead of looking
   * up/computing a new one.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void usesExistingRecordWhenOneExists() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final String fakeHash1 = "not-a-real-hash-but-loads-from-db";

          // preload our fake hash code
          transactionManager.executeProcedure(
              entityManager -> entityManager.persist(new Mbi(mbi1, fakeHash1)));

          // verify our fake is used instead of a computed correct one
          final MbiCache mbiCache =
              MbiCache.databaseCache(normalHasher, appMetrics, transactionManager);
          assertEquals(fakeHash1, mbiCache.lookupMbi(mbi1).getHash());
          assertEquals(fakeHash1, mbiCache.lookupMbi(mbi1).getHash());

          // verify database still contains our fake hash
          Mbi databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(transactionManager, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(fakeHash1, databaseMbiEntity.getHash());
        });
  }

  /**
   * Verifies that when the same mbi is called multiple times, the cache is used on repeats to avoid
   * unneeded queries.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void usesInMemoryCacheToAvoidExtraQueries() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final MbiCache mbiCache =
              spy(MbiCache.databaseCache(normalHasher, appMetrics, transactionManager));

          // mix of calls in various order with repeats for the same mbi
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());
          assertEquals(hash3, mbiCache.lookupMbi(mbi3).getHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
          assertEquals(hash3, mbiCache.lookupMbi(mbi3).getHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());

          // loading the fourth mbi overflows the cache so the oldest (mbi1) is recomputed
          assertEquals(hash4, mbiCache.lookupMbi(mbi4).getHash());
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());

          assertEquals(9, mbiCache.getMetrics().getLookups());
          assertEquals(4, mbiCache.getMetrics().getMisses());
          assertEquals(0, mbiCache.getMetrics().getTotalRetries());

          // every mbi except mbi1 should have been looked up in the database exactly once
          verify(mbiCache, times(2)).computeMbi(mbi1);
          verify(mbiCache, times(1)).computeMbi(mbi2);
          verify(mbiCache, times(1)).computeMbi(mbi3);
          verify(mbiCache, times(1)).computeMbi(mbi4);
        });
  }

  /**
   * Verifies that if an error is thrown while attempting to get a value from the cache, it will
   * retry a set number of times before skipping the cache and looking up the value in the database.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void retryFiveTimes() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        Clock.systemUTC(),
        (appState, transactionManager) -> {
          final PersistenceException error = new PersistenceException("oops");
          final MbiCache.DatabaseBacked mbiCache =
              spy(
                  (MbiCache.DatabaseBacked)
                      MbiCache.databaseCache(normalHasher, appMetrics, transactionManager));
          doThrow(error, error, error, error, error)
              .doReturn(new MbiCache.ReadResult(new Mbi(1L, mbi1, hash1), true))
              .when(mbiCache)
              .readOrInsertIfMissing(eq(mbi1));

          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
          verify(mbiCache, times(1)).lookupMbi(mbi1);
          verify(mbiCache, times(6)).readOrInsertIfMissing(eq(mbi1));
        });
  }
}
