package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import gov.cms.bfd.model.rda.PreAdjMbi;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.time.Clock;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

public class DatabaseMbiHasherIT {
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
          final DatabaseMbiHasher databaseHasher = new DatabaseMbiHasher(hashConfig, entityManager);
          assertEquals(hash1, databaseHasher.computeIdentifierHash(mbi1));
          assertEquals(hash2, databaseHasher.computeIdentifierHash(mbi2));

          PreAdjMbi databaseMbiEntity = entityManager.find(PreAdjMbi.class, mbi1);
          assertNotNull(databaseMbiEntity);
          assertEquals(hash1, databaseMbiEntity.getMbiHash());

          databaseMbiEntity = entityManager.find(PreAdjMbi.class, mbi2);
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
          entityManager.persist(new PreAdjMbi(mbi1, fakeHash1));
          entityManager.getTransaction().commit();

          // verify our fake is used instead of a computed correct one
          final DatabaseMbiHasher hasher = new DatabaseMbiHasher(hashConfig, entityManager);
          assertEquals(fakeHash1, hasher.computeIdentifierHash(mbi1));
          assertEquals(fakeHash1, hasher.computeIdentifierHash(mbi1));

          // verify database still contains our fake hash
          PreAdjMbi databaseMbiEntity = entityManager.find(PreAdjMbi.class, mbi1);
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
          final DatabaseMbiHasher hasher = spy(new DatabaseMbiHasher(hashConfig, entityManager));

          // mix of calls in various order with repeats for the same mbi
          assertEquals(hash1, hasher.computeIdentifierHash(mbi1));
          assertEquals(hash2, hasher.computeIdentifierHash(mbi2));
          assertEquals(hash3, hasher.computeIdentifierHash(mbi3));
          assertEquals(hash2, hasher.computeIdentifierHash(mbi2));
          assertEquals(hash1, hasher.computeIdentifierHash(mbi1));
          assertEquals(hash3, hasher.computeIdentifierHash(mbi3));
          assertEquals(hash2, hasher.computeIdentifierHash(mbi2));

          // loading the fourth mbi overflows the cache so the oldest (mbi1) is recomputed
          assertEquals(hash4, hasher.computeIdentifierHash(mbi4));
          assertEquals(hash1, hasher.computeIdentifierHash(mbi1));

          // every mbi except mbi1 should have been looked up in the database exactly once
          verify(hasher, times(2)).lookupWithRetries(mbi1);
          verify(hasher, times(1)).lookupWithRetries(mbi2);
          verify(hasher, times(1)).lookupWithRetries(mbi3);
          verify(hasher, times(1)).lookupWithRetries(mbi4);
        });
  }

  @Test
  public void retryFiveTimes() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        FissClaimRdaSinkIT.class,
        Clock.systemUTC(),
        (appState, entityManager) -> {
          final PersistenceException error = new PersistenceException("oops");
          final DatabaseMbiHasher hasher = spy(new DatabaseMbiHasher(hashConfig, entityManager));
          doThrow(error, error, error, error, error)
              .doReturn(hash1)
              .when(hasher)
              .readOrInsertIfMissing(mbi1);

          assertEquals(hash1, hasher.computeIdentifierHash(mbi1));
          verify(hasher, times(1)).lookupWithRetries(mbi1);
          verify(hasher, times(6)).readOrInsertIfMissing(mbi1);
        });
  }
}
