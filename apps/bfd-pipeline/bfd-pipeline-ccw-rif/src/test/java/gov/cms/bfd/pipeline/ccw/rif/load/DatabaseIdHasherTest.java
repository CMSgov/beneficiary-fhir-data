package gov.cms.bfd.pipeline.ccw.rif.load;

import static gov.cms.bfd.pipeline.ccw.rif.load.DatabaseIdHasher.IdParamName;
import static gov.cms.bfd.pipeline.ccw.rif.load.DatabaseIdHasher.QueryString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.model.rif.IdHash;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link DatabaseIdHasher}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DatabaseIdHasherTest {

  /** Mock entity manager. */
  @Mock private EntityManagerFactory entityManagerFactory;
  /** Mock entity manager. */
  @Mock private EntityManager entityManager;
  /** Mock query. */
  @Mock private TypedQuery<String> query;
  /** Mock transaction. */
  @Mock private EntityTransaction transaction;
  /** Uncached hasher used by cached hasher and to check hash values are correct. */
  private IdHasher idHasher;
  /** Hasher being tested. */
  private DatabaseIdHasher dbHasher;

  /** Basic wiring of mocks and creation of non-mocks. */
  @BeforeEach
  void setUp() {
    doReturn(entityManager).when(entityManagerFactory).createEntityManager();
    doReturn(transaction).when(entityManager).getTransaction();
    doReturn(query).when(entityManager).createQuery(QueryString, String.class);
    doReturn(query).when(query).setMaxResults(anyInt());
    doReturn(query).when(query).setParameter(eq(IdParamName), anyString());
    idHasher =
        new IdHasher(
            IdHasher.Config.builder().hashPepperString("pepper").hashIterations(1).build());
    dbHasher = new DatabaseIdHasher(new SimpleMeterRegistry(), entityManagerFactory, idHasher, 2);
  }

  /**
   * Verifies that first call computes a hash and writes record to database and that subsequent
   * calls just return the cached value.
   */
  @Test
  void shouldInsertRecordThenReturnFromCache() {
    final var identifier = "123456";
    final var expectedHash = idHasher.computeIdentifierHash(identifier);
    final var expectedRecord = new IdHash(identifier, expectedHash);

    // verifies no queries or persists have happened yet
    verify(query, times(0)).getResultList();
    verify(entityManager, times(0)).persist(any());

    // the first call will run the query and persist the record
    doReturn(List.of()).when(query).getResultList();
    assertEquals(expectedHash, dbHasher.computeIdentifierHash(identifier));
    verify(query, times(1)).setParameter(IdParamName, identifier);
    verify(query, times(1)).setMaxResults(1);
    verify(query, times(1)).getResultList();
    verify(entityManager, times(1)).persist(refEq(expectedRecord));

    // all other calls will return cached value and no queries will be performed
    assertEquals(expectedHash, dbHasher.computeIdentifierHash(identifier));
    assertEquals(expectedHash, dbHasher.computeIdentifierHash(identifier));
    assertEquals(expectedHash, dbHasher.computeIdentifierHash(identifier));
    verify(query, times(1)).setParameter(anyString(), any());
    verify(query, times(1)).setMaxResults(anyInt());
    verify(query, times(1)).getResultList();
    verify(entityManager, times(1)).persist(any());

    assertEquals(4L, dbHasher.getMetrics().getLookups());
    assertEquals(1L, dbHasher.getMetrics().getMisses());
    assertEquals(0L, dbHasher.getMetrics().getRetries());
  }

  /** Verifies that {@link RuntimeException}s are not wrapped but just passed through unchanged. */
  @Test
  void shouldRethrowUnwrappedRuntimeExceptions() {
    final var identifier = "123456";
    final var expectedException = new RuntimeException("oops");

    doThrow(expectedException).when(entityManager).persist(any());
    final var actualException =
        assertThrows(RuntimeException.class, () -> dbHasher.computeIdentifierHash(identifier));
    assertSame(expectedException, actualException);
  }

  /**
   * Verifies that a constraint violation exception triggers a retry. Just doing one retry to keep
   * test time reasonable.
   */
  @Test
  void shouldRetryOnConstraintViolationException() {
    final var identifier = "123456";
    final var expectedHash = idHasher.computeIdentifierHash(identifier);
    final var expectedRecord = new IdHash(identifier, expectedHash);

    final var constraintViolation =
        new ConstraintViolationException("oops", new SQLException("testing"), "id");

    // never find the record in the database using query
    doReturn(List.of()).when(query).getResultList();

    // first persist fails, second succeeds, so there will be one retry
    doThrow(new RuntimeException(constraintViolation))
        .doNothing()
        .when(entityManager)
        .persist(any());

    assertEquals(expectedHash, dbHasher.computeIdentifierHash(identifier));
    verify(query, times(2)).setParameter(IdParamName, identifier);
    verify(query, times(2)).setMaxResults(1);
    verify(query, times(2)).getResultList();
    verify(entityManager, times(2)).persist(refEq(expectedRecord));

    assertEquals(1L, dbHasher.getMetrics().getLookups());
    assertEquals(1L, dbHasher.getMetrics().getMisses());
    assertEquals(1L, dbHasher.getMetrics().getRetries());
  }
}
