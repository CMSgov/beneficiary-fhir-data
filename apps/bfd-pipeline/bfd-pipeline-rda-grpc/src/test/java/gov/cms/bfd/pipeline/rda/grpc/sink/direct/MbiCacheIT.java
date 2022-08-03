package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.time.Clock;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MbiCacheIT {
  private final IdHasher.Config hashConfig =
      IdHasher.Config.builder()
          .hashIterations(1)
          .hashPepperString("just-a-test")
          .cacheSize(3)
          .build();
  private final IdHasher normalHasher = new IdHasher(hashConfig);

  private final String mbi1 = "1";
  private final String hash1 = normalHasher.computeIdentifierHash(mbi1);
  private final String mbi2 = "2";
  private final String hash2 = normalHasher.computeIdentifierHash(mbi2);
  private final String mbi3 = "3";
  private final String hash3 = normalHasher.computeIdentifierHash(mbi3);
  private final String mbi4 = "4";
  private final String hash4 = normalHasher.computeIdentifierHash(mbi4);

  private MetricRegistry appMetrics;

  @BeforeEach
  void setUp() {
    appMetrics = new MetricRegistry();
  }

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

  @Test
  public void createsNewRecordWhenNoneExists() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final MbiCache mbiCache = MbiCache.databaseCache(normalHasher, appMetrics, entityManager);
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getHash());

          Mbi databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(entityManager, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash1, databaseMbiEntity.getHash());

          databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(entityManager, mbi2);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash2, databaseMbiEntity.getHash());

          assertEquals(2, mbiCache.getMetrics().getLookups());
          assertEquals(2, mbiCache.getMetrics().getMisses());
          assertEquals(0, mbiCache.getMetrics().getTotalRetries());
        });
  }

  @Test
  public void usesExistingRecordWhenOneExists() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final String fakeHash1 = "not-a-real-hash-but-loads-from-db";

          // preload our fake hash code
          entityManager.getTransaction().begin();
          entityManager.persist(new Mbi(mbi1, fakeHash1));
          entityManager.getTransaction().commit();

          // verify our fake is used instead of a computed correct one
          final MbiCache mbiCache = MbiCache.databaseCache(normalHasher, appMetrics, entityManager);
          assertEquals(fakeHash1, mbiCache.lookupMbi(mbi1).getHash());
          assertEquals(fakeHash1, mbiCache.lookupMbi(mbi1).getHash());

          // verify database still contains our fake hash
          Mbi databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(entityManager, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(fakeHash1, databaseMbiEntity.getHash());
        });
  }

  @Test
  public void usesInMemoryCacheToAvoidExtraQueries() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final MbiCache.DatabaseBackedCache mbiCache =
              spy(
                  new MbiCache.DatabaseBackedCache(
                      normalHasher, new MbiCache.Metrics(appMetrics), entityManager));

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
          verify(mbiCache, times(2)).lookupMbiImpl(mbi1);
          verify(mbiCache, times(1)).lookupMbiImpl(mbi2);
          verify(mbiCache, times(1)).lookupMbiImpl(mbi3);
          verify(mbiCache, times(1)).lookupMbiImpl(mbi4);
        });
  }

  @Test
  public void retryFiveTimes() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final PersistenceException error = new PersistenceException("oops");
          final MbiCache.DatabaseBackedCache mbiCache =
              spy(
                  new MbiCache.DatabaseBackedCache(
                      normalHasher, new MbiCache.Metrics(appMetrics), entityManager));
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
