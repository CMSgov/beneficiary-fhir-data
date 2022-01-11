package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.time.Clock;
import javax.persistence.PersistenceException;
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

  @Test
  public void createsNewRecordWhenNoneExists() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final MbiCache mbiCache = MbiCache.databaseCache(entityManager, normalHasher);
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getMbiHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getMbiHash());

          Mbi databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(entityManager, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash1, databaseMbiEntity.getMbiHash());

          databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(entityManager, mbi2);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash2, databaseMbiEntity.getMbiHash());
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
          entityManager.persist(new Mbi(null, mbi1, fakeHash1));
          entityManager.getTransaction().commit();

          // verify our fake is used instead of a computed correct one
          final MbiCache mbiCache = MbiCache.databaseCache(entityManager, normalHasher);
          assertEquals(fakeHash1, mbiCache.lookupMbi(mbi1).getMbiHash());
          assertEquals(fakeHash1, mbiCache.lookupMbi(mbi1).getMbiHash());

          // verify database still contains our fake hash
          Mbi databaseMbiEntity = RdaPipelineTestUtils.lookupCachedMbi(entityManager, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(fakeHash1, databaseMbiEntity.getMbiHash());
        });
  }

  @Test
  public void usesInMemoryCacheToAvoidExtraQueries() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final MbiCache.DatabaseLookupFunction lookupFunction =
              spy(new MbiCache.DatabaseLookupFunction(entityManager, normalHasher));
          final MbiCache mbiCache = new MbiCache(lookupFunction, normalHasher);

          // mix of calls in various order with repeats for the same mbi
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getMbiHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getMbiHash());
          assertEquals(hash3, mbiCache.lookupMbi(mbi3).getMbiHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getMbiHash());
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getMbiHash());
          assertEquals(hash3, mbiCache.lookupMbi(mbi3).getMbiHash());
          assertEquals(hash2, mbiCache.lookupMbi(mbi2).getMbiHash());

          // loading the fourth mbi overflows the cache so the oldest (mbi1) is recomputed
          assertEquals(hash4, mbiCache.lookupMbi(mbi4).getMbiHash());
          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getMbiHash());

          // every mbi except mbi1 should have been looked up in the database exactly once
          verify(lookupFunction, times(2)).lookupMbi(mbi1);
          verify(lookupFunction, times(1)).lookupMbi(mbi2);
          verify(lookupFunction, times(1)).lookupMbi(mbi3);
          verify(lookupFunction, times(1)).lookupMbi(mbi4);
        });
  }

  @Test
  public void retryFiveTimes() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final PersistenceException error = new PersistenceException("oops");
          final MbiCache.DatabaseLookupFunction lookupFunction =
              spy(new MbiCache.DatabaseLookupFunction(entityManager, normalHasher));
          final MbiCache mbiCache = new MbiCache(lookupFunction, normalHasher);
          doThrow(error, error, error, error, error)
              .doReturn(new Mbi(1L, mbi1, hash1))
              .when(lookupFunction)
              .readOrInsertIfMissing(mbi1);

          assertEquals(hash1, mbiCache.lookupMbi(mbi1).getMbiHash());
          verify(lookupFunction, times(1)).lookupMbi(mbi1);
          verify(lookupFunction, times(6)).readOrInsertIfMissing(mbi1);
        });
  }
}
